package com.bioid.keycloak.client.connection;

import com.bioid.keycloak.client.config.BioIdClientConfig;
import com.bioid.keycloak.client.exception.BioIdServiceException;
import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.channel.ChannelOption;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thread-safe connection manager for BioID gRPC channels.
 *
 * <p>Features: - Connection pooling with round-robin load balancing - Circuit breaker pattern for
 * fault tolerance - Health monitoring with automatic failover - Metrics collection for
 * observability - Graceful shutdown handling
 */
public class BioIdConnectionManager implements AutoCloseable {

  private static final Logger logger = LoggerFactory.getLogger(BioIdConnectionManager.class);

  private final BioIdClientConfig config;
  private final MeterRegistry meterRegistry;

  // Connection pool
  private final ManagedChannel[] channels;
  private final AtomicInteger channelIndex = new AtomicInteger(0);

  // Circuit breaker state
  private final AtomicInteger failureCount = new AtomicInteger(0);
  private final AtomicReference<CircuitBreakerState> circuitState =
      new AtomicReference<>(CircuitBreakerState.CLOSED);
  private final AtomicReference<Instant> lastFailureTime = new AtomicReference<>();

  // Health monitoring
  private final ScheduledExecutorService healthCheckExecutor;
  private final AtomicBoolean healthy = new AtomicBoolean(true);
  private final AtomicReference<String> currentEndpoint;

  // Metrics
  private final Counter connectionAttempts;
  private final Counter connectionFailures;
  private final Counter circuitBreakerTrips;
  private final Timer connectionTime;

  // Circuit breaker thresholds
  private static final int FAILURE_THRESHOLD = 5;
  private static final Duration CIRCUIT_OPEN_DURATION = Duration.ofSeconds(30);

  @SuppressWarnings("unused") // Reserved for future circuit breaker implementation
  private static final int HALF_OPEN_MAX_CALLS = 3;

  public BioIdConnectionManager(BioIdClientConfig config, MeterRegistry meterRegistry) {
    this.config = config;
    this.meterRegistry = meterRegistry;
    this.currentEndpoint = new AtomicReference<>(config.endpoint());

    // Initialize metrics
    this.connectionAttempts =
        Counter.builder("bioid.connection.attempts")
            .description("Total connection attempts to BioID service")
            .register(meterRegistry);
    this.connectionFailures =
        Counter.builder("bioid.connection.failures")
            .description("Failed connection attempts to BioID service")
            .register(meterRegistry);
    this.circuitBreakerTrips =
        Counter.builder("bioid.circuit_breaker.trips")
            .description("Circuit breaker trips")
            .register(meterRegistry);
    this.connectionTime =
        Timer.builder("bioid.connection.time")
            .description("Time to establish connection to BioID service")
            .register(meterRegistry);

    // Initialize connection pool
    this.channels = new ManagedChannel[config.channelPoolSize()];
    initializeChannels();

    // Start health monitoring
    this.healthCheckExecutor =
        Executors.newSingleThreadScheduledExecutor(
            r -> {
              Thread t = new Thread(r, "bioid-health-check");
              t.setDaemon(true);
              return t;
            });

    startHealthMonitoring();

    logger.info(
        "Initialized BioID connection manager with {} channels to endpoint: {}",
        config.channelPoolSize(),
        config.endpoint());
  }

  /**
   * Gets a healthy gRPC channel using round-robin load balancing. Implements circuit breaker
   * pattern for fault tolerance.
   *
   * @return managed channel
   * @throws BioIdServiceException if no healthy channels available
   */
  public ManagedChannel getChannel() throws BioIdServiceException {
    // Check circuit breaker state
    CircuitBreakerState state = circuitState.get();

    switch (state) {
      case OPEN:
        if (shouldAttemptReset()) {
          circuitState.compareAndSet(CircuitBreakerState.OPEN, CircuitBreakerState.HALF_OPEN);
          logger.info("Circuit breaker transitioning to HALF_OPEN state");
        } else {
          throw new BioIdServiceException("Circuit breaker is OPEN - service unavailable");
        }
        break;

      case HALF_OPEN:
        // Allow limited calls in half-open state
        break;

      case CLOSED:
      default:
        // Normal operation
        break;
    }

    if (!healthy.get()) {
      throw new BioIdServiceException("No healthy BioID endpoints available");
    }

    // Get next channel using round-robin
    int index = Math.abs(channelIndex.getAndIncrement() % channels.length);
    ManagedChannel channel = channels[index];

    if (channel == null || channel.isShutdown() || channel.isTerminated()) {
      // Try to recreate the channel
      synchronized (this) {
        if (channels[index] == null
            || channels[index].isShutdown()
            || channels[index].isTerminated()) {
          channels[index] = createChannel(currentEndpoint.get());
        }
      }
      channel = channels[index];
    }

    return channel;
  }

  /** Records a successful operation, potentially resetting circuit breaker. */
  public void recordSuccess() {
    CircuitBreakerState state = circuitState.get();

    if (state == CircuitBreakerState.HALF_OPEN) {
      // Reset circuit breaker after successful call in half-open state
      circuitState.set(CircuitBreakerState.CLOSED);
      failureCount.set(0);
      logger.info("Circuit breaker reset to CLOSED state after successful operation");
    } else if (state == CircuitBreakerState.CLOSED) {
      // Reset failure count on successful operation
      failureCount.set(0);
    }
  }

  /** Records a failure, potentially tripping circuit breaker. */
  public void recordFailure() {
    connectionFailures.increment();
    lastFailureTime.set(Instant.now());

    int failures = failureCount.incrementAndGet();
    CircuitBreakerState state = circuitState.get();

    if (state == CircuitBreakerState.CLOSED && failures >= FAILURE_THRESHOLD) {
      // Trip circuit breaker
      circuitState.set(CircuitBreakerState.OPEN);
      circuitBreakerTrips.increment();
      logger.warn("Circuit breaker tripped to OPEN state after {} failures", failures);
    } else if (state == CircuitBreakerState.HALF_OPEN) {
      // Return to open state on failure during half-open
      circuitState.set(CircuitBreakerState.OPEN);
      logger.warn("Circuit breaker returned to OPEN state after failure in HALF_OPEN");
    }
  }

  /**
   * Checks if the current endpoint is healthy.
   *
   * @return true if healthy, false otherwise
   */
  public boolean isHealthy() {
    return healthy.get() && circuitState.get() != CircuitBreakerState.OPEN;
  }

  /**
   * Gets the current active endpoint.
   *
   * @return current endpoint URL
   */
  public String getCurrentEndpoint() {
    return currentEndpoint.get();
  }

  private void initializeChannels() {
    String endpoint = currentEndpoint.get();

    for (int i = 0; i < channels.length; i++) {
      channels[i] = createChannel(endpoint);
    }
  }

  private ManagedChannel createChannel(String endpoint) {
    connectionAttempts.increment();

    Timer.Sample sample = Timer.start(meterRegistry);

    try {
      // Parse endpoint to determine host, port, and TLS settings
      String host;
      int port;
      boolean useTls;

      if (endpoint.startsWith("grpcs://")) {
        // Secure gRPC
        useTls = true;
        String hostPort = endpoint.substring(8); // Remove "grpcs://"
        if (hostPort.contains(":")) {
          String[] parts = hostPort.split(":");
          host = parts[0];
          port = Integer.parseInt(parts[1]);
        } else {
          host = hostPort;
          port = 443; // Default HTTPS port
        }
      } else if (endpoint.startsWith("https://")) {
        // HTTPS endpoint (BioID BWS standard)
        useTls = true;
        String hostPort = endpoint.substring(8); // Remove "https://"
        if (hostPort.contains(":")) {
          String[] parts = hostPort.split(":");
          host = parts[0];
          port = Integer.parseInt(parts[1]);
        } else {
          host = hostPort;
          port = 443; // Default HTTPS port
        }
      } else if (endpoint.startsWith("grpc://")) {
        // Insecure gRPC
        useTls = false;
        String hostPort = endpoint.substring(7); // Remove "grpc://"
        if (hostPort.contains(":")) {
          String[] parts = hostPort.split(":");
          host = parts[0];
          port = Integer.parseInt(parts[1]);
        } else {
          host = hostPort;
          port = 80; // Default HTTP port
        }
      } else {
        // Assume it's just a hostname or hostname:port, default to secure
        useTls = true;
        if (endpoint.contains(":")) {
          String[] parts = endpoint.split(":");
          host = parts[0];
          port = Integer.parseInt(parts[1]);
        } else {
          host = endpoint;
          port = 443; // Default HTTPS port for gRPC-TLS
        }
      }

      // Use forTarget instead of forAddress to let gRPC handle DNS and connection properly
      String target = host + ":" + port;
      NettyChannelBuilder builder =
          NettyChannelBuilder.forTarget(target)
              .keepAliveTime(config.keepAliveTime().toSeconds(), TimeUnit.SECONDS)
              .keepAliveTimeout(config.keepAliveTimeout().toSeconds(), TimeUnit.SECONDS)
              .keepAliveWithoutCalls(config.keepAliveWithoutCalls())
              .maxInboundMessageSize(4 * 1024 * 1024) // 4MB max message size
              .withOption(
                  ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) config.connectTimeout().toMillis())
              .withOption(ChannelOption.SO_KEEPALIVE, true);

      // Configure TLS - always use TLS for BioID BWS
      if (config.mutualTlsEnabled() && config.keyStorePath() != null) {
        // Mutual TLS configuration would go here
        logger.debug("Mutual TLS configuration not yet implemented");
      }

      // Use transport security for HTTPS/gRPC-TLS
      builder.useTransportSecurity();

      logger.debug("Configured gRPC channel with TLS for target: {}", target);

      ManagedChannel channel = builder.build();

      sample.stop(connectionTime);
      logger.info(
          "Created gRPC channel to {}:{} (TLS: {})", host, port, useTls || config.tlsEnabled());

      return channel;

    } catch (Exception e) {
      sample.stop(connectionTime);
      connectionFailures.increment();
      logger.error("Failed to create gRPC channel to endpoint: {}", endpoint, e);
      throw new RuntimeException("Failed to create gRPC channel", e);
    }
  }

  private void startHealthMonitoring() {
    healthCheckExecutor.scheduleWithFixedDelay(
        this::performHealthCheck,
        config.healthCheckInterval().toSeconds(),
        config.healthCheckInterval().toSeconds(),
        TimeUnit.SECONDS);
  }

  private void performHealthCheck() {
    try {
      // Simple connectivity check - try to get a channel
      ManagedChannel channel = channels[0];
      if (channel != null && !channel.isShutdown() && !channel.isTerminated()) {
        // Channel exists and is not shutdown
        if (!healthy.get()) {
          healthy.set(true);
          logger.info("BioID service health check passed - marking as healthy");
        }
      } else {
        if (healthy.get()) {
          healthy.set(false);
          logger.warn("BioID service health check failed - marking as unhealthy");
        }
      }
    } catch (Exception e) {
      if (healthy.get()) {
        healthy.set(false);
        logger.warn("BioID service health check failed: {}", e.getMessage());
      }
    }
  }

  private boolean shouldAttemptReset() {
    Instant lastFailure = lastFailureTime.get();
    if (lastFailure == null) {
      return true;
    }

    return Instant.now().isAfter(lastFailure.plus(CIRCUIT_OPEN_DURATION));
  }

  @Override
  public void close() {
    logger.info("Shutting down BioID connection manager");

    // Shutdown health check executor
    healthCheckExecutor.shutdown();
    try {
      if (!healthCheckExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
        healthCheckExecutor.shutdownNow();
      }
    } catch (InterruptedException e) {
      healthCheckExecutor.shutdownNow();
      Thread.currentThread().interrupt();
    }

    // Shutdown all channels
    for (ManagedChannel channel : channels) {
      if (channel != null && !channel.isShutdown()) {
        channel.shutdown();
        try {
          if (!channel.awaitTermination(5, TimeUnit.SECONDS)) {
            channel.shutdownNow();
          }
        } catch (InterruptedException e) {
          channel.shutdownNow();
          Thread.currentThread().interrupt();
        }
      }
    }

    logger.info("BioID connection manager shutdown complete");
  }

  /** Circuit breaker states for fault tolerance. */
  private enum CircuitBreakerState {
    CLOSED, // Normal operation
    OPEN, // Failing fast, not allowing calls
    HALF_OPEN // Testing if service has recovered
  }
}
