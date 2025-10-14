package com.bioid.keycloak.client.connection;

import com.bioid.keycloak.client.EnhancedBioIdClient.ConnectionPoolConfig;
import com.bioid.keycloak.client.EnhancedBioIdClient.ConnectionPoolStatus;
import com.bioid.keycloak.client.config.BioIdClientConfig;
import com.bioid.keycloak.client.exception.BioIdException;
import com.bioid.keycloak.client.exception.BioIdServiceException;
import io.grpc.ManagedChannel;

import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages a pool of gRPC connections to BioID services with health monitoring,
 * automatic failover, and connection lifecycle management.
 *
 * <p>This class provides:
 * - Connection pooling with configurable pool size and timeouts
 * - Health monitoring with automatic connection replacement
 * - Regional endpoint switching with latency-based selection
 * - Connection metrics collection for monitoring
 * - Graceful connection cleanup and resource management
 */
public class ConnectionPoolManager implements AutoCloseable {

  private static final Logger logger = LoggerFactory.getLogger(ConnectionPoolManager.class);

  private final BioIdClientConfig config;
  private final ConcurrentHashMap<String, ManagedChannel> connectionPool;
  private final ConcurrentHashMap<String, ConnectionInfo> connectionMetrics;
  private final ScheduledExecutorService healthCheckExecutor;
  private final ScheduledExecutorService cleanupExecutor;
  
  private volatile ConnectionPoolConfig poolConfig;
  private volatile String currentEndpoint;
  private volatile boolean closed = false;
  
  // Metrics tracking
  private final AtomicLong totalRequestsServed = new AtomicLong(0);
  private final AtomicLong totalResponseTime = new AtomicLong(0);
  private final AtomicInteger activeConnections = new AtomicInteger(0);

  public ConnectionPoolManager(BioIdClientConfig config) {
    this.config = config;
    this.connectionPool = new ConcurrentHashMap<>();
    this.connectionMetrics = new ConcurrentHashMap<>();
    this.healthCheckExecutor = Executors.newScheduledThreadPool(2);
    this.cleanupExecutor = Executors.newScheduledThreadPool(1);
    this.currentEndpoint = config.endpoint();
    
    // Initialize with default pool configuration
    this.poolConfig = new ConnectionPoolConfig(
        10, // maxPoolSize
        2,  // minIdleConnections
        5000, // connectionTimeoutMs
        300000, // idleTimeoutMs (5 minutes)
        1800000 // maxLifetimeMs (30 minutes)
    );
    
    // Start background tasks
    startHealthChecking();
    startConnectionCleanup();
    
    logger.info("Connection pool manager initialized for endpoint: {}", currentEndpoint);
  }

  /**
   * Gets a connection from the pool, creating a new one if necessary.
   *
   * @return a managed gRPC channel
   * @throws BioIdException if unable to create or retrieve a connection
   */
  public ManagedChannel getConnection() throws BioIdException {
    return getConnection(currentEndpoint);
  }

  /**
   * Gets a connection for a specific endpoint.
   *
   * @param endpoint the endpoint to connect to
   * @return a managed gRPC channel
   * @throws BioIdException if unable to create or retrieve a connection
   */
  public ManagedChannel getConnection(String endpoint) throws BioIdException {
    if (closed) {
      throw new BioIdServiceException("Connection pool is closed");
    }

    ManagedChannel channel = connectionPool.get(endpoint);
    
    if (channel == null || channel.isShutdown() || channel.isTerminated()) {
      synchronized (this) {
        // Double-check locking pattern
        channel = connectionPool.get(endpoint);
        if (channel == null || channel.isShutdown() || channel.isTerminated()) {
          channel = createNewConnection(endpoint);
          connectionPool.put(endpoint, channel);
          activeConnections.incrementAndGet();
          
          logger.debug("Created new connection for endpoint: {}", endpoint);
        }
      }
    }
    
    // Update connection metrics
    ConnectionInfo info = connectionMetrics.computeIfAbsent(endpoint, 
        k -> new ConnectionInfo(endpoint, Instant.now()));
    info.recordAccess();
    
    return channel;
  }

  /**
   * Records a successful request for metrics tracking.
   *
   * @param responseTimeMs the response time in milliseconds
   */
  public void recordSuccess(long responseTimeMs) {
    totalRequestsServed.incrementAndGet();
    totalResponseTime.addAndGet(responseTimeMs);
  }

  /**
   * Records a failed request for metrics tracking.
   */
  public void recordFailure() {
    totalRequestsServed.incrementAndGet();
  }

  /**
   * Gets the current connection pool status.
   *
   * @return connection pool status information
   */
  public ConnectionPoolStatus getStatus() {
    int totalConnections = connectionPool.size();
    int active = activeConnections.get();
    int idle = Math.max(0, totalConnections - active);
    long totalRequests = totalRequestsServed.get();
    double avgResponseTime = totalRequests > 0 ? 
        (double) totalResponseTime.get() / totalRequests : 0.0;

    return new ConnectionPoolStatus(
        totalConnections,
        active,
        idle,
        poolConfig.getMaxPoolSize(),
        totalRequests,
        avgResponseTime
    );
  }

  /**
   * Refreshes the connection pool by closing idle connections and creating new ones.
   */
  public void refreshPool() {
    logger.info("Refreshing connection pool");
    
    synchronized (this) {
      // Close all existing connections
      connectionPool.values().forEach(channel -> {
        if (!channel.isShutdown()) {
          channel.shutdown();
          try {
            channel.awaitTermination(5, TimeUnit.SECONDS);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            channel.shutdownNow();
          }
        }
      });
      
      connectionPool.clear();
      connectionMetrics.clear();
      activeConnections.set(0);
      
      logger.info("Connection pool refreshed");
    }
  }

  /**
   * Configures the connection pool with new settings.
   *
   * @param newConfig the new connection pool configuration
   * @throws BioIdException if the configuration is invalid
   */
  public void configurePool(ConnectionPoolConfig newConfig) throws BioIdException {
    if (newConfig.getMaxPoolSize() <= 0) {
      throw new BioIdException("Max pool size must be greater than 0");
    }
    if (newConfig.getMinIdleConnections() < 0) {
      throw new BioIdException("Min idle connections cannot be negative");
    }
    if (newConfig.getMinIdleConnections() > newConfig.getMaxPoolSize()) {
      throw new BioIdException("Min idle connections cannot exceed max pool size");
    }
    
    this.poolConfig = newConfig;
    logger.info("Connection pool reconfigured: {}", newConfig);
  }

  /**
   * Switches to a different endpoint.
   *
   * @param newEndpoint the new endpoint to use
   * @throws BioIdException if the endpoint switch fails
   */
  public void switchEndpoint(String newEndpoint) throws BioIdException {
    if (newEndpoint == null || newEndpoint.trim().isEmpty()) {
      throw new BioIdException("Endpoint cannot be null or empty");
    }
    
    String oldEndpoint = this.currentEndpoint;
    this.currentEndpoint = newEndpoint;
    
    logger.info("Switched endpoint from {} to {}", oldEndpoint, newEndpoint);
    
    // Optionally refresh the pool to use the new endpoint
    refreshPool();
  }

  /**
   * Gets the current endpoint.
   *
   * @return the current endpoint
   */
  public String getCurrentEndpoint() {
    return currentEndpoint;
  }

  private ManagedChannel createNewConnection(String endpoint) throws BioIdException {
    try {
      return NettyChannelBuilder.forTarget(endpoint)
          .keepAliveTime(30, TimeUnit.SECONDS)
          .keepAliveTimeout(5, TimeUnit.SECONDS)
          .keepAliveWithoutCalls(true)
          .maxInboundMessageSize(4 * 1024 * 1024) // 4MB
          .usePlaintext() // Use TLS in production
          .build();
          
    } catch (Exception e) {
      throw new BioIdServiceException("Failed to create connection to " + endpoint, e);
    }
  }

  private void startHealthChecking() {
    healthCheckExecutor.scheduleWithFixedDelay(() -> {
      try {
        performHealthChecks();
      } catch (Exception e) {
        logger.warn("Health check failed", e);
      }
    }, 30, 30, TimeUnit.SECONDS);
  }

  private void startConnectionCleanup() {
    cleanupExecutor.scheduleWithFixedDelay(() -> {
      try {
        cleanupIdleConnections();
      } catch (Exception e) {
        logger.warn("Connection cleanup failed", e);
      }
    }, 60, 60, TimeUnit.SECONDS);
  }

  private void performHealthChecks() {
    Instant now = Instant.now();
    
    connectionMetrics.entrySet().removeIf(entry -> {
      ConnectionInfo info = entry.getValue();
      Duration idleDuration = Duration.between(info.getLastAccess(), now);
      
      if (idleDuration.toMillis() > poolConfig.getMaxLifetimeMs()) {
        String endpoint = entry.getKey();
        ManagedChannel channel = connectionPool.remove(endpoint);
        if (channel != null && !channel.isShutdown()) {
          channel.shutdown();
          activeConnections.decrementAndGet();
          logger.debug("Removed expired connection for endpoint: {}", endpoint);
        }
        return true;
      }
      return false;
    });
  }

  private void cleanupIdleConnections() {
    Instant now = Instant.now();
    
    connectionMetrics.entrySet().forEach(entry -> {
      ConnectionInfo info = entry.getValue();
      Duration idleDuration = Duration.between(info.getLastAccess(), now);
      
      if (idleDuration.toMillis() > poolConfig.getIdleTimeoutMs()) {
        String endpoint = entry.getKey();
        ManagedChannel channel = connectionPool.get(endpoint);
        if (channel != null && !channel.isShutdown()) {
          // Check if we have more than minimum idle connections
          int currentIdle = connectionPool.size() - activeConnections.get();
          if (currentIdle > poolConfig.getMinIdleConnections()) {
            connectionPool.remove(endpoint);
            channel.shutdown();
            activeConnections.decrementAndGet();
            logger.debug("Cleaned up idle connection for endpoint: {}", endpoint);
          }
        }
      }
    });
  }

  @Override
  public void close() {
    if (closed) {
      return;
    }
    
    closed = true;
    logger.info("Shutting down connection pool manager");
    
    // Shutdown executors
    healthCheckExecutor.shutdown();
    cleanupExecutor.shutdown();
    
    try {
      if (!healthCheckExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
        healthCheckExecutor.shutdownNow();
      }
      if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
        cleanupExecutor.shutdownNow();
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      healthCheckExecutor.shutdownNow();
      cleanupExecutor.shutdownNow();
    }
    
    // Close all connections
    connectionPool.values().forEach(channel -> {
      if (!channel.isShutdown()) {
        channel.shutdown();
        try {
          channel.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          channel.shutdownNow();
        }
      }
    });
    
    connectionPool.clear();
    connectionMetrics.clear();
    
    logger.info("Connection pool manager shutdown complete");
  }

  /**
   * Internal class to track connection information and metrics.
   */
  private static class ConnectionInfo {
    private final String endpoint;
    private final Instant createdAt;
    private volatile Instant lastAccess;
    private final AtomicLong accessCount = new AtomicLong(0);

    public ConnectionInfo(String endpoint, Instant createdAt) {
      this.endpoint = endpoint;
      this.createdAt = createdAt;
      this.lastAccess = createdAt;
    }

    public void recordAccess() {
      this.lastAccess = Instant.now();
      this.accessCount.incrementAndGet();
    }

    public String getEndpoint() { return endpoint; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastAccess() { return lastAccess; }
    public long getAccessCount() { return accessCount.get(); }
  }
}