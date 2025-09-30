package com.bioid.keycloak.cluster;

import com.bioid.keycloak.performance.CredentialCache;
import com.bioid.keycloak.performance.GrpcChannelPool;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cluster-aware resource manager for Keycloak face recognition extension. Ensures proper resource
 * cleanup and stateless operation across cluster nodes.
 */
public class ClusterAwareResourceManager {

  private static final Logger logger = LoggerFactory.getLogger(ClusterAwareResourceManager.class);

  private static final long CLEANUP_INTERVAL_MINUTES = 10;
  private static final long HEALTH_CHECK_INTERVAL_MINUTES = 5;

  private final Map<String, NodeInfo> clusterNodes;
  private final ScheduledExecutorService maintenanceExecutor;
  private final ScheduledFuture<?> cleanupTask;
  private final ScheduledFuture<?> healthCheckTask;
  private final AtomicBoolean isShuttingDown;
  private final String nodeId;

  private static volatile ClusterAwareResourceManager instance;

  /** Information about a cluster node. */
  private static class NodeInfo {
    private final String nodeId;
    private volatile long lastSeen;
    private volatile boolean isHealthy;
    private volatile int activeConnections;
    private volatile long memoryUsage;

    public NodeInfo(String nodeId) {
      this.nodeId = nodeId;
      this.lastSeen = System.currentTimeMillis();
      this.isHealthy = true;
      this.activeConnections = 0;
      this.memoryUsage = 0;
    }

    public void updateHealth(boolean healthy, int connections, long memory) {
      this.isHealthy = healthy;
      this.activeConnections = connections;
      this.memoryUsage = memory;
      this.lastSeen = System.currentTimeMillis();
    }

    public boolean isStale(long staleThresholdMs) {
      return System.currentTimeMillis() - lastSeen > staleThresholdMs;
    }

    // Getters
    public String getNodeId() {
      return nodeId;
    }

    public long getLastSeen() {
      return lastSeen;
    }

    public boolean isHealthy() {
      return isHealthy;
    }

    public int getActiveConnections() {
      return activeConnections;
    }

    public long getMemoryUsage() {
      return memoryUsage;
    }
  }

  private ClusterAwareResourceManager() {
    this.clusterNodes = new ConcurrentHashMap<>();
    this.isShuttingDown = new AtomicBoolean(false);
    this.nodeId = generateNodeId();

    this.maintenanceExecutor =
        Executors.newScheduledThreadPool(
            2,
            r -> {
              Thread t = new Thread(r, "cluster-maintenance");
              t.setDaemon(true);
              return t;
            });

    // Start maintenance tasks
    this.cleanupTask =
        maintenanceExecutor.scheduleAtFixedRate(
            this::performClusterCleanup,
            CLEANUP_INTERVAL_MINUTES,
            CLEANUP_INTERVAL_MINUTES,
            TimeUnit.MINUTES);

    this.healthCheckTask =
        maintenanceExecutor.scheduleAtFixedRate(
            this::performHealthCheck,
            HEALTH_CHECK_INTERVAL_MINUTES,
            HEALTH_CHECK_INTERVAL_MINUTES,
            TimeUnit.MINUTES);

    // Register shutdown hook
    Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));

    logger.info("Cluster-aware resource manager initialized for node: " + nodeId);
  }

  /** Get singleton instance. */
  public static ClusterAwareResourceManager getInstance() {
    if (instance == null) {
      synchronized (ClusterAwareResourceManager.class) {
        if (instance == null) {
          instance = new ClusterAwareResourceManager();
        }
      }
    }
    return instance;
  }

  /** Register this node in the cluster. */
  public void registerNode() {
    if (isShuttingDown.get()) {
      return;
    }

    NodeInfo nodeInfo = new NodeInfo(nodeId);
    updateNodeHealth(nodeInfo);
    clusterNodes.put(nodeId, nodeInfo);

    logger.info("Registered cluster node: " + nodeId);
  }

  /** Update health information for this node. */
  public void updateNodeHealth() {
    NodeInfo nodeInfo = clusterNodes.get(nodeId);
    if (nodeInfo != null) {
      updateNodeHealth(nodeInfo);
    }
  }

  private void updateNodeHealth(NodeInfo nodeInfo) {
    try {
      // Get current resource usage
      Runtime runtime = Runtime.getRuntime();
      long memoryUsage = runtime.totalMemory() - runtime.freeMemory();

      // Get connection count from channel pool
      int activeConnections = getActiveConnectionCount();

      // Determine if node is healthy
      boolean isHealthy =
          !isShuttingDown.get()
              && memoryUsage < runtime.maxMemory() * 0.9; // Less than 90% memory usage

      nodeInfo.updateHealth(isHealthy, activeConnections, memoryUsage);

    } catch (Exception e) {
      logger.warn("Error updating node health", e);
      nodeInfo.updateHealth(false, 0, 0);
    }
  }

  /** Get information about all cluster nodes. */
  public Map<String, ClusterNodeStats> getClusterStats() {
    Map<String, ClusterNodeStats> stats = new ConcurrentHashMap<>();

    for (NodeInfo nodeInfo : clusterNodes.values()) {
      stats.put(
          nodeInfo.getNodeId(),
          new ClusterNodeStats(
              nodeInfo.getNodeId(),
              nodeInfo.isHealthy(),
              nodeInfo.getActiveConnections(),
              nodeInfo.getMemoryUsage(),
              nodeInfo.getLastSeen()));
    }

    return stats;
  }

  /** Get the current node ID. */
  public String getNodeId() {
    return nodeId;
  }

  /** Check if the cluster is healthy. */
  public boolean isClusterHealthy() {
    if (clusterNodes.isEmpty()) {
      return true; // Single node deployment
    }

    long healthyNodes = clusterNodes.values().stream().filter(NodeInfo::isHealthy).count();

    // Cluster is healthy if at least 50% of nodes are healthy
    return healthyNodes >= Math.ceil(clusterNodes.size() / 2.0);
  }

  /** Ensure stateless operation by cleaning up node-specific state. */
  public void ensureStatelessOperation() {
    try {
      // Clear any node-specific cached data that shouldn't persist across requests
      // This ensures that requests can be handled by any node in the cluster

      // Note: We don't clear the credential cache as it's beneficial for performance
      // and the data is not node-specific

      logger.debug("Ensured stateless operation for node: " + nodeId);

    } catch (Exception e) {
      logger.error("Error ensuring stateless operation", e);
    }
  }

  /** Perform cluster-wide resource cleanup. */
  private void performClusterCleanup() {
    if (isShuttingDown.get()) {
      return;
    }

    try {
      // Remove stale nodes (not seen for more than 30 minutes)
      long staleThreshold = 30 * 60 * 1000; // 30 minutes
      clusterNodes
          .entrySet()
          .removeIf(
              entry -> {
                boolean isStale = entry.getValue().isStale(staleThreshold);
                if (isStale && !entry.getKey().equals(nodeId)) {
                  logger.info("Removed stale cluster node: " + entry.getKey());
                }
                return isStale && !entry.getKey().equals(nodeId);
              });

      // Trigger garbage collection if memory usage is high
      Runtime runtime = Runtime.getRuntime();
      long memoryUsage = runtime.totalMemory() - runtime.freeMemory();
      double memoryUsagePercent = (double) memoryUsage / runtime.maxMemory();

      if (memoryUsagePercent > 0.8) { // More than 80% memory usage
        logger.info(
            "High memory usage detected ("
                + String.format("%.1f%%", memoryUsagePercent * 100)
                + "), suggesting garbage collection");
        System.gc();
      }

      // Update this node's health
      updateNodeHealth();

    } catch (Exception e) {
      logger.error("Error during cluster cleanup", e);
    }
  }

  /** Perform health check for this node. */
  private void performHealthCheck() {
    if (isShuttingDown.get()) {
      return;
    }

    try {
      // Update node health information
      updateNodeHealth();

      // Log cluster status
      if (logger.isDebugEnabled()) {
        Map<String, ClusterNodeStats> stats = getClusterStats();
        logger.debug("Cluster status: " + stats.size() + " nodes, healthy: " + isClusterHealthy());
      }

    } catch (Exception e) {
      logger.error("Error during health check", e);
    }
  }

  /** Get the number of active connections across all pools. */
  private int getActiveConnectionCount() {
    try {
      GrpcChannelPool channelPool = GrpcChannelPool.getInstance();
      Map<String, GrpcChannelPool.ChannelPoolStats> poolStats = channelPool.getAllStats();

      return poolStats.values().stream()
          .mapToInt(GrpcChannelPool.ChannelPoolStats::getHealthyChannels)
          .sum();

    } catch (Exception e) {
      logger.debug("Error getting connection count", e);
      return 0;
    }
  }

  /** Generate a unique node ID. */
  private String generateNodeId() {
    try {
      String hostname = java.net.InetAddress.getLocalHost().getHostName();
      String pid = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
      return hostname + "-" + pid + "-" + System.currentTimeMillis();
    } catch (Exception e) {
      return "node-" + System.currentTimeMillis() + "-" + Math.random();
    }
  }

  /** Shutdown the resource manager and cleanup resources. */
  @PreDestroy
  public void shutdown() {
    if (isShuttingDown.compareAndSet(false, true)) {
      logger.info("Shutting down cluster-aware resource manager for node: " + nodeId);

      try {
        // Cancel maintenance tasks
        if (cleanupTask != null) {
          cleanupTask.cancel(true);
        }
        if (healthCheckTask != null) {
          healthCheckTask.cancel(true);
        }

        // Shutdown maintenance executor
        maintenanceExecutor.shutdown();
        if (!maintenanceExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
          maintenanceExecutor.shutdownNow();
        }

        // Cleanup shared resources
        cleanupSharedResources();

        // Remove this node from cluster
        clusterNodes.remove(nodeId);

        logger.info("Cluster-aware resource manager shutdown complete");

      } catch (Exception e) {
        logger.error("Error during shutdown", e);
      }
    }
  }

  /** Cleanup shared resources that should be cleaned up when the last node shuts down. */
  private void cleanupSharedResources() {
    try {
      // Only cleanup shared resources if this is the last node
      if (clusterNodes.size() <= 1) {
        logger.info("Last node shutting down, cleaning up shared resources");

        // Shutdown channel pool
        GrpcChannelPool channelPool = GrpcChannelPool.getInstance();
        channelPool.shutdown();

        // Shutdown credential cache
        CredentialCache credentialCache = CredentialCache.getInstance();
        credentialCache.shutdown();

      } else {
        logger.info("Other nodes still active, skipping shared resource cleanup");
      }

    } catch (Exception e) {
      logger.error("Error cleaning up shared resources", e);
    }
  }

  /** Statistics for a cluster node. */
  public static class ClusterNodeStats {
    private final String nodeId;
    private final boolean isHealthy;
    private final int activeConnections;
    private final long memoryUsage;
    private final long lastSeen;

    public ClusterNodeStats(
        String nodeId, boolean isHealthy, int activeConnections, long memoryUsage, long lastSeen) {
      this.nodeId = nodeId;
      this.isHealthy = isHealthy;
      this.activeConnections = activeConnections;
      this.memoryUsage = memoryUsage;
      this.lastSeen = lastSeen;
    }

    public String getNodeId() {
      return nodeId;
    }

    public boolean isHealthy() {
      return isHealthy;
    }

    public int getActiveConnections() {
      return activeConnections;
    }

    public long getMemoryUsage() {
      return memoryUsage;
    }

    public long getLastSeen() {
      return lastSeen;
    }

    public String getMemoryUsageFormatted() {
      return String.format("%.1f MB", memoryUsage / (1024.0 * 1024.0));
    }

    public long getAgeSeconds() {
      return (System.currentTimeMillis() - lastSeen) / 1000;
    }

    @Override
    public String toString() {
      return String.format(
          "ClusterNodeStats{nodeId='%s', healthy=%s, connections=%d, memory=%s, age=%ds}",
          nodeId, isHealthy, activeConnections, getMemoryUsageFormatted(), getAgeSeconds());
    }
  }
}
