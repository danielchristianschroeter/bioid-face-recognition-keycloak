package com.bioid.keycloak.client.connection;

import com.bioid.keycloak.client.config.BioIdClientConfig;
import com.bioid.keycloak.client.endpoint.RegionalEndpointManager;
import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages gRPC connection pools for different regional endpoints.
 *
 * <p>Features: - Separate connection pools per endpoint - Connection health monitoring - Automatic
 * connection recycling - Keep-alive configuration - Connection optimization for regional endpoints
 */
public class ConnectionPoolManager {

  private static final Logger logger = LoggerFactory.getLogger(ConnectionPoolManager.class);

  /** Connection pool for a specific endpoint. */
  public static class ConnectionPool {
    private final String endpoint;
    private final ManagedChannel[] channels;
    private final AtomicInteger roundRobinIndex;
    private final Instant createdAt;
    private final ReentrantLock lock = new ReentrantLock();
    private volatile boolean shutdown = false;

    public ConnectionPool(String endpoint, int poolSize, BioIdClientConfig config) {
      this.endpoint = endpoint;
      this.channels = new ManagedChannel[poolSize];
      this.roundRobinIndex = new AtomicInteger(0);
      this.createdAt = Instant.now();

      // Create channels
      for (int i = 0; i < poolSize; i++) {
        channels[i] = createChannel(endpoint, config);
      }

      logger.info("Created connection pool for {} with {} channels", endpoint, poolSize);
    }

    /** Gets the next available channel using round-robin. */
    public ManagedChannel getChannel() {
      if (shutdown) {
        throw new IllegalStateException("Connection pool is shutdown");
      }

      int index = Math.abs(roundRobinIndex.getAndIncrement() % channels.length);
      ManagedChannel channel = channels[index];

      // Check if channel is still usable
      if (channel.isShutdown() || channel.isTerminated()) {
        lock.lock();
        try {
          // Double-check after acquiring lock
          if (channels[index].isShutdown() || channels[index].isTerminated()) {
            logger.warn("Recreating shutdown channel {} for endpoint {}", index, endpoint);
            channels[index] = createChannel(endpoint, null); // Config not available here
          }
          return channels[index];
        } finally {
          lock.unlock();
        }
      }

      return channel;
    }

    /** Shuts down all channels in the pool. */
    public void shutdown() {
      lock.lock();
      try {
        if (shutdown) {
          return;
        }
        shutdown = true;

        logger.info("Shutting down connection pool for {}", endpoint);
        for (int i = 0; i < channels.length; i++) {
          if (channels[i] != null && !channels[i].isShutdown()) {
            channels[i].shutdown();
            try {
              if (!channels[i].awaitTermination(5, TimeUnit.SECONDS)) {
                channels[i].shutdownNow();
              }
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
              channels[i].shutdownNow();
            }
          }
        }
      } finally {
        lock.unlock();
      }
    }

    /** Gets the endpoint this pool serves. */
    public String getEndpoint() {
      return endpoint;
    }

    /** Gets the creation time of this pool. */
    public Instant getCreatedAt() {
      return createdAt;
    }

    /** Gets the number of channels in this pool. */
    public int getPoolSize() {
      return channels.length;
    }

    /** Checks if the pool is shutdown. */
    public boolean isShutdown() {
      return shutdown;
    }

    /** Creates a new gRPC channel for the given endpoint. */
    private ManagedChannel createChannel(String endpoint, BioIdClientConfig config) {
      // Extract host and port from endpoint
      String host;
      int port;
      boolean useTls;

      if (endpoint.startsWith("grpcs://")) {
        useTls = true;
        String hostPort = endpoint.substring(8); // Remove "grpcs://"
        int colonIndex = hostPort.lastIndexOf(':');
        if (colonIndex > 0) {
          host = hostPort.substring(0, colonIndex);
          port = Integer.parseInt(hostPort.substring(colonIndex + 1));
        } else {
          host = hostPort;
          port = 443; // Default HTTPS port
        }
      } else if (endpoint.startsWith("grpc://")) {
        useTls = false;
        String hostPort = endpoint.substring(7); // Remove "grpc://"
        int colonIndex = hostPort.lastIndexOf(':');
        if (colonIndex > 0) {
          host = hostPort.substring(0, colonIndex);
          port = Integer.parseInt(hostPort.substring(colonIndex + 1));
        } else {
          host = hostPort;
          port = 80; // Default HTTP port
        }
      } else {
        throw new IllegalArgumentException("Invalid endpoint format: " + endpoint);
      }

      NettyChannelBuilder builder = NettyChannelBuilder.forAddress(host, port);

      if (useTls) {
        builder.useTransportSecurity();
      } else {
        builder.usePlaintext();
      }

      // Apply configuration if available
      if (config != null) {
        builder
            .keepAliveTime(config.keepAliveTime().toSeconds(), TimeUnit.SECONDS)
            .keepAliveTimeout(config.keepAliveTimeout().toSeconds(), TimeUnit.SECONDS)
            .keepAliveWithoutCalls(config.keepAliveWithoutCalls())
            .maxInboundMessageSize(4 * 1024 * 1024) // 4MB max message size
            .userAgent("BioID-Keycloak-Extension/1.0");
      } else {
        // Default configuration
        builder
            .keepAliveTime(30, TimeUnit.SECONDS)
            .keepAliveTimeout(30, TimeUnit.SECONDS)
            .keepAliveWithoutCalls(true)
            .maxInboundMessageSize(4 * 1024 * 1024)
            .userAgent("BioID-Keycloak-Extension/1.0");
      }

      return builder.build();
    }
  }

  private final ConcurrentHashMap<String, ConnectionPool> pools = new ConcurrentHashMap<>();
  private final BioIdClientConfig config;
  private final RegionalEndpointManager endpointManager;
  private volatile boolean shutdown = false;

  public ConnectionPoolManager(BioIdClientConfig config, RegionalEndpointManager endpointManager) {
    this.config = config;
    this.endpointManager = endpointManager;

    logger.info("Connection pool manager initialized with pool size: {}", config.channelPoolSize());
  }

  /** Gets a channel for the primary endpoint. */
  public ManagedChannel getChannel() {
    String primaryEndpoint = endpointManager.getPrimaryEndpoint();
    return getChannel(primaryEndpoint);
  }

  /** Gets a channel for a specific endpoint. */
  public ManagedChannel getChannel(String endpoint) {
    if (shutdown) {
      throw new IllegalStateException("Connection pool manager is shutdown");
    }

    ConnectionPool pool =
        pools.computeIfAbsent(
            endpoint, ep -> new ConnectionPool(ep, config.channelPoolSize(), config));

    return pool.getChannel();
  }

  /** Gets a channel with automatic failover. */
  public ManagedChannel getChannelWithFailover() {
    if (shutdown) {
      throw new IllegalStateException("Connection pool manager is shutdown");
    }

    // Try endpoints in order of preference
    for (String endpoint : endpointManager.getOrderedEndpoints()) {
      try {
        ManagedChannel channel = getChannel(endpoint);
        if (channel != null && !channel.isShutdown() && !channel.isTerminated()) {
          return channel;
        }
      } catch (Exception e) {
        logger.warn("Failed to get channel for endpoint {}: {}", endpoint, e.getMessage());
        endpointManager.reportFailure(endpoint, e.getMessage());
      }
    }

    // Last resort: try primary endpoint even if unhealthy
    String primaryEndpoint = endpointManager.getPrimaryEndpoint();
    logger.warn("All endpoints failed, using primary endpoint {} as last resort", primaryEndpoint);
    return getChannel(primaryEndpoint);
  }

  /** Reports a successful operation for connection optimization. */
  public void reportSuccess(String endpoint, Duration latency) {
    endpointManager.reportSuccess(endpoint, latency);
  }

  /** Reports a failed operation for connection management. */
  public void reportFailure(String endpoint, String errorMessage) {
    endpointManager.reportFailure(endpoint, errorMessage);

    // Consider recreating the connection pool if too many failures
    ConnectionPool pool = pools.get(endpoint);
    if (pool != null) {
      // For now, just log. In a more sophisticated implementation,
      // we might recreate the pool after a certain number of failures
      logger.debug("Reported failure for endpoint {} with active pool", endpoint);
    }
  }

  /** Performs health checks on all connection pools. */
  public void performHealthCheck() {
    endpointManager.performHealthCheck();

    // Clean up any shutdown pools
    pools
        .entrySet()
        .removeIf(
            entry -> {
              ConnectionPool pool = entry.getValue();
              if (pool.isShutdown()) {
                logger.debug("Removing shutdown pool for endpoint {}", entry.getKey());
                return true;
              }
              return false;
            });
  }

  /** Gets the current number of connection pools. */
  public int getPoolCount() {
    return pools.size();
  }

  /** Gets information about all connection pools. */
  public java.util.Map<String, String> getPoolInfo() {
    java.util.Map<String, String> info = new java.util.HashMap<>();
    pools.forEach(
        (endpoint, pool) -> {
          info.put(
              endpoint,
              String.format(
                  "Pool size: %d, Created: %s, Shutdown: %s",
                  pool.getPoolSize(), pool.getCreatedAt(), pool.isShutdown()));
        });
    return info;
  }

  /** Shuts down all connection pools. */
  public void shutdown() {
    if (shutdown) {
      return;
    }

    logger.info("Shutting down connection pool manager with {} pools", pools.size());
    shutdown = true;

    // Shutdown all pools
    pools.values().parallelStream().forEach(ConnectionPool::shutdown);
    pools.clear();

    logger.info("Connection pool manager shutdown complete");
  }

  /** Checks if the manager is shutdown. */
  public boolean isShutdown() {
    return shutdown;
  }

  /** Gets the regional endpoint manager. */
  public RegionalEndpointManager getEndpointManager() {
    return endpointManager;
  }
}
