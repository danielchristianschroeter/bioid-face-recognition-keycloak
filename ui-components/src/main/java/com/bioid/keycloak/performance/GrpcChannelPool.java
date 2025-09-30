package com.bioid.keycloak.performance;

import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * gRPC channel pool for BioID service connections. Provides connection pooling with health
 * monitoring and automatic cleanup.
 */
public class GrpcChannelPool {

  private static final Logger logger = LoggerFactory.getLogger(GrpcChannelPool.class);

  private static final int DEFAULT_POOL_SIZE = 5;
  private static final int MAX_POOL_SIZE = 20;
  private static final long HEALTH_CHECK_INTERVAL_SECONDS = 30;
  private static final long IDLE_TIMEOUT_MINUTES = 10;
  private static final long CONNECTION_TIMEOUT_SECONDS = 10;

  private final Map<String, ChannelPoolEntry> channelPools;
  private final ScheduledExecutorService healthCheckExecutor;
  private final ScheduledFuture<?> healthCheckTask;
  private final ReentrantLock poolLock;

  private static volatile GrpcChannelPool instance;

  /** Pool entry containing channels for a specific endpoint. */
  private static class ChannelPoolEntry {
    private final List<PooledChannel> channels;
    private final AtomicInteger roundRobinIndex;
    private final String endpoint;
    private final int poolSize;
    private final ReentrantLock channelLock;

    public ChannelPoolEntry(String endpoint, int poolSize) {
      this.endpoint = endpoint;
      this.poolSize = poolSize;
      this.channels = new ArrayList<>(poolSize);
      this.roundRobinIndex = new AtomicInteger(0);
      this.channelLock = new ReentrantLock();

      // Initialize channels
      for (int i = 0; i < poolSize; i++) {
        channels.add(createPooledChannel());
      }
    }

    private PooledChannel createPooledChannel() {
      ManagedChannel channel =
          ManagedChannelBuilder.forTarget(endpoint)
              .keepAliveTime(30, TimeUnit.SECONDS)
              .keepAliveTimeout(5, TimeUnit.SECONDS)
              .keepAliveWithoutCalls(true)
              .maxInboundMessageSize(4 * 1024 * 1024) // 4MB
              .usePlaintext() // Use TLS in production
              .build();

      return new PooledChannel(channel, System.currentTimeMillis());
    }

    public PooledChannel getChannel() {
      channelLock.lock();
      try {
        // Round-robin selection
        int index = Math.abs(roundRobinIndex.getAndIncrement() % channels.size());
        PooledChannel pooledChannel = channels.get(index);

        // Check if channel is healthy
        if (isChannelHealthy(pooledChannel.getChannel())) {
          pooledChannel.updateLastUsed();
          return pooledChannel;
        }

        // Try to find a healthy channel
        for (PooledChannel channel : channels) {
          if (isChannelHealthy(channel.getChannel())) {
            channel.updateLastUsed();
            return channel;
          }
        }

        // All channels are unhealthy, create a new one
        logger.warn("All channels unhealthy for endpoint: " + endpoint + ", creating new channel");
        PooledChannel newChannel = createPooledChannel();
        channels.set(index, newChannel);
        return newChannel;

      } finally {
        channelLock.unlock();
      }
    }

    public void performHealthCheck() {
      channelLock.lock();
      try {
        long currentTime = System.currentTimeMillis();
        long idleThreshold = currentTime - (IDLE_TIMEOUT_MINUTES * 60 * 1000);

        for (int i = 0; i < channels.size(); i++) {
          PooledChannel pooledChannel = channels.get(i);
          ManagedChannel channel = pooledChannel.getChannel();

          // Check if channel is idle and should be recreated
          if (pooledChannel.getLastUsed() < idleThreshold) {
            logger.debug("Recreating idle channel for endpoint: " + endpoint);
            channel.shutdown();
            channels.set(i, createPooledChannel());
          } else if (!isChannelHealthy(channel)) {
            logger.warn("Unhealthy channel detected for endpoint: " + endpoint + ", recreating");
            channel.shutdown();
            channels.set(i, createPooledChannel());
          }
        }
      } finally {
        channelLock.unlock();
      }
    }

    public void shutdown() {
      channelLock.lock();
      try {
        for (PooledChannel pooledChannel : channels) {
          ManagedChannel channel = pooledChannel.getChannel();
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
        channels.clear();
      } finally {
        channelLock.unlock();
      }
    }

    private boolean isChannelHealthy(ManagedChannel channel) {
      ConnectivityState state = channel.getState(false);
      return state == ConnectivityState.READY || state == ConnectivityState.IDLE;
    }
  }

  /** Wrapper for ManagedChannel with usage tracking. */
  private static class PooledChannel {
    private final ManagedChannel channel;
    private volatile long lastUsed;

    public PooledChannel(ManagedChannel channel, long lastUsed) {
      this.channel = channel;
      this.lastUsed = lastUsed;
    }

    public ManagedChannel getChannel() {
      return channel;
    }

    public long getLastUsed() {
      return lastUsed;
    }

    public void updateLastUsed() {
      this.lastUsed = System.currentTimeMillis();
    }
  }

  private GrpcChannelPool() {
    this.channelPools = new ConcurrentHashMap<>();
    this.poolLock = new ReentrantLock();
    this.healthCheckExecutor =
        Executors.newSingleThreadScheduledExecutor(
            r -> {
              Thread t = new Thread(r, "grpc-channel-health-check");
              t.setDaemon(true);
              return t;
            });

    // Start health check task
    this.healthCheckTask =
        healthCheckExecutor.scheduleAtFixedRate(
            this::performHealthChecks,
            HEALTH_CHECK_INTERVAL_SECONDS,
            HEALTH_CHECK_INTERVAL_SECONDS,
            TimeUnit.SECONDS);

    logger.info("gRPC channel pool initialized");
  }

  /** Get singleton instance of the channel pool. */
  public static GrpcChannelPool getInstance() {
    if (instance == null) {
      synchronized (GrpcChannelPool.class) {
        if (instance == null) {
          instance = new GrpcChannelPool();
        }
      }
    }
    return instance;
  }

  /** Get a channel for the specified endpoint. */
  public ManagedChannel getChannel(String endpoint) {
    return getChannel(endpoint, DEFAULT_POOL_SIZE);
  }

  /** Get a channel for the specified endpoint with custom pool size. */
  public ManagedChannel getChannel(String endpoint, int poolSize) {
    if (endpoint == null || endpoint.trim().isEmpty()) {
      throw new IllegalArgumentException("Endpoint cannot be null or empty");
    }

    int actualPoolSize = Math.min(Math.max(poolSize, 1), MAX_POOL_SIZE);

    ChannelPoolEntry poolEntry =
        channelPools.computeIfAbsent(endpoint, key -> new ChannelPoolEntry(key, actualPoolSize));

    return poolEntry.getChannel().getChannel();
  }

  /** Get channel statistics for monitoring. */
  public ChannelPoolStats getStats(String endpoint) {
    ChannelPoolEntry poolEntry = channelPools.get(endpoint);
    if (poolEntry == null) {
      return new ChannelPoolStats(endpoint, 0, 0, 0);
    }

    poolEntry.channelLock.lock();
    try {
      int totalChannels = poolEntry.channels.size();
      int healthyChannels = 0;
      int idleChannels = 0;
      long currentTime = System.currentTimeMillis();
      long idleThreshold = currentTime - (IDLE_TIMEOUT_MINUTES * 60 * 1000);

      for (PooledChannel pooledChannel : poolEntry.channels) {
        if (poolEntry.isChannelHealthy(pooledChannel.getChannel())) {
          healthyChannels++;
        }
        if (pooledChannel.getLastUsed() < idleThreshold) {
          idleChannels++;
        }
      }

      return new ChannelPoolStats(endpoint, totalChannels, healthyChannels, idleChannels);
    } finally {
      poolEntry.channelLock.unlock();
    }
  }

  /** Get statistics for all endpoints. */
  public Map<String, ChannelPoolStats> getAllStats() {
    Map<String, ChannelPoolStats> stats = new ConcurrentHashMap<>();
    for (String endpoint : channelPools.keySet()) {
      stats.put(endpoint, getStats(endpoint));
    }
    return stats;
  }

  /** Remove pool for specific endpoint. */
  public void removePool(String endpoint) {
    poolLock.lock();
    try {
      ChannelPoolEntry poolEntry = channelPools.remove(endpoint);
      if (poolEntry != null) {
        poolEntry.shutdown();
        logger.info("Removed channel pool for endpoint: " + endpoint);
      }
    } finally {
      poolLock.unlock();
    }
  }

  /** Shutdown all pools and cleanup resources. */
  public void shutdown() {
    logger.info("Shutting down gRPC channel pool");

    // Cancel health check task
    if (healthCheckTask != null) {
      healthCheckTask.cancel(true);
    }

    // Shutdown all pools
    poolLock.lock();
    try {
      for (ChannelPoolEntry poolEntry : channelPools.values()) {
        poolEntry.shutdown();
      }
      channelPools.clear();
    } finally {
      poolLock.unlock();
    }

    // Shutdown executor
    healthCheckExecutor.shutdown();
    try {
      if (!healthCheckExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
        healthCheckExecutor.shutdownNow();
      }
    } catch (InterruptedException e) {
      healthCheckExecutor.shutdownNow();
      Thread.currentThread().interrupt();
    }

    instance = null;
    logger.info("gRPC channel pool shutdown complete");
  }

  private void performHealthChecks() {
    try {
      for (ChannelPoolEntry poolEntry : channelPools.values()) {
        poolEntry.performHealthCheck();
      }
    } catch (Exception e) {
      logger.error("Error during health check", e);
    }
  }

  /** Statistics for a channel pool. */
  public static class ChannelPoolStats {
    private final String endpoint;
    private final int totalChannels;
    private final int healthyChannels;
    private final int idleChannels;

    public ChannelPoolStats(
        String endpoint, int totalChannels, int healthyChannels, int idleChannels) {
      this.endpoint = endpoint;
      this.totalChannels = totalChannels;
      this.healthyChannels = healthyChannels;
      this.idleChannels = idleChannels;
    }

    public String getEndpoint() {
      return endpoint;
    }

    public int getTotalChannels() {
      return totalChannels;
    }

    public int getHealthyChannels() {
      return healthyChannels;
    }

    public int getIdleChannels() {
      return idleChannels;
    }

    public int getUnhealthyChannels() {
      return totalChannels - healthyChannels;
    }

    @Override
    public String toString() {
      return String.format(
          "ChannelPoolStats{endpoint='%s', total=%d, healthy=%d, idle=%d}",
          endpoint, totalChannels, healthyChannels, idleChannels);
    }
  }
}
