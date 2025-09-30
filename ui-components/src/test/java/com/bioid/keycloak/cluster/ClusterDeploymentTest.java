package com.bioid.keycloak.cluster;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests for cluster deployment functionality. */
public class ClusterDeploymentTest {

  private ClusterAwareResourceManager resourceManager;

  @BeforeEach
  void setUp() {
    resourceManager = ClusterAwareResourceManager.getInstance();
    resourceManager.registerNode();
  }

  @AfterEach
  void tearDown() {
    // Note: We don't shutdown the singleton in tests as it affects other tests
    // In a real deployment, shutdown would be handled by the container
  }

  @Test
  void testNodeRegistration() {
    String nodeId = resourceManager.getNodeId();
    assertNotNull(nodeId, "Node ID should not be null");
    assertFalse(nodeId.isEmpty(), "Node ID should not be empty");

    Map<String, ClusterAwareResourceManager.ClusterNodeStats> stats =
        resourceManager.getClusterStats();
    assertTrue(stats.containsKey(nodeId), "Cluster stats should contain this node");

    ClusterAwareResourceManager.ClusterNodeStats nodeStats = stats.get(nodeId);
    assertTrue(nodeStats.isHealthy(), "Node should be healthy");
    assertTrue(nodeStats.getMemoryUsage() > 0, "Memory usage should be positive");
  }

  @Test
  void testClusterHealthCheck() {
    // Single node should be healthy
    assertTrue(resourceManager.isClusterHealthy(), "Single node cluster should be healthy");

    // Update node health
    resourceManager.updateNodeHealth();

    // Should still be healthy
    assertTrue(
        resourceManager.isClusterHealthy(), "Cluster should remain healthy after health update");
  }

  @Test
  void testStatelessOperation() {
    // Test that stateless operation can be ensured without errors
    assertDoesNotThrow(
        () -> {
          resourceManager.ensureStatelessOperation();
        },
        "Ensuring stateless operation should not throw exceptions");

    // Verify node is still healthy after ensuring stateless operation
    Map<String, ClusterAwareResourceManager.ClusterNodeStats> stats =
        resourceManager.getClusterStats();
    String nodeId = resourceManager.getNodeId();
    ClusterAwareResourceManager.ClusterNodeStats nodeStats = stats.get(nodeId);

    assertNotNull(nodeStats, "Node stats should exist");
    assertTrue(nodeStats.isHealthy(), "Node should remain healthy");
  }

  @Test
  void testConcurrentOperations() throws InterruptedException {
    int threadCount = 5;
    int operationsPerThread = 10;
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch latch = new CountDownLatch(threadCount);
    AtomicInteger successCount = new AtomicInteger(0);

    for (int t = 0; t < threadCount; t++) {
      executor.submit(
          () -> {
            try {
              for (int i = 0; i < operationsPerThread; i++) {
                // Simulate concurrent cluster operations
                resourceManager.updateNodeHealth();
                resourceManager.ensureStatelessOperation();
                resourceManager.getClusterStats();
                resourceManager.isClusterHealthy();

                successCount.incrementAndGet();

                // Small delay to simulate real work
                Thread.sleep(10);
              }
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            } finally {
              latch.countDown();
            }
          });
    }

    assertTrue(latch.await(30, TimeUnit.SECONDS), "All threads should complete within 30 seconds");

    int expectedOperations = threadCount * operationsPerThread;
    assertEquals(expectedOperations, successCount.get(), "All operations should succeed");

    // Verify cluster is still healthy after concurrent operations
    assertTrue(
        resourceManager.isClusterHealthy(),
        "Cluster should be healthy after concurrent operations");

    executor.shutdown();
  }

  @Test
  void testMemoryUsageTracking() {
    Map<String, ClusterAwareResourceManager.ClusterNodeStats> stats =
        resourceManager.getClusterStats();
    String nodeId = resourceManager.getNodeId();
    ClusterAwareResourceManager.ClusterNodeStats nodeStats = stats.get(nodeId);

    assertNotNull(nodeStats, "Node stats should exist");
    assertTrue(nodeStats.getMemoryUsage() > 0, "Memory usage should be positive");

    String formattedMemory = nodeStats.getMemoryUsageFormatted();
    assertNotNull(formattedMemory, "Formatted memory should not be null");
    assertTrue(formattedMemory.contains("MB"), "Formatted memory should contain MB unit");
  }

  @Test
  void testNodeStatsFormatting() {
    Map<String, ClusterAwareResourceManager.ClusterNodeStats> stats =
        resourceManager.getClusterStats();
    String nodeId = resourceManager.getNodeId();
    ClusterAwareResourceManager.ClusterNodeStats nodeStats = stats.get(nodeId);

    assertNotNull(nodeStats, "Node stats should exist");

    // Test toString method
    String statsString = nodeStats.toString();
    assertNotNull(statsString, "Stats string should not be null");
    assertTrue(statsString.contains(nodeId), "Stats string should contain node ID");
    assertTrue(statsString.contains("healthy=true"), "Stats string should show healthy status");

    // Test age calculation
    long age = nodeStats.getAgeSeconds();
    assertTrue(age >= 0, "Age should be non-negative");
    assertTrue(age < 60, "Age should be less than 60 seconds for a fresh node");
  }

  @Test
  void testResourceManagerSingleton() {
    ClusterAwareResourceManager instance1 = ClusterAwareResourceManager.getInstance();
    ClusterAwareResourceManager instance2 = ClusterAwareResourceManager.getInstance();

    assertSame(instance1, instance2, "Should return the same singleton instance");
    assertEquals(instance1.getNodeId(), instance2.getNodeId(), "Node IDs should be the same");
  }

  @Test
  void testClusterStatsCollection() {
    // Update health to ensure fresh stats
    resourceManager.updateNodeHealth();

    Map<String, ClusterAwareResourceManager.ClusterNodeStats> stats =
        resourceManager.getClusterStats();

    assertNotNull(stats, "Cluster stats should not be null");
    assertFalse(stats.isEmpty(), "Cluster stats should not be empty");

    String nodeId = resourceManager.getNodeId();
    assertTrue(stats.containsKey(nodeId), "Stats should contain current node");

    ClusterAwareResourceManager.ClusterNodeStats nodeStats = stats.get(nodeId);
    assertEquals(nodeId, nodeStats.getNodeId(), "Node ID should match");
    assertTrue(nodeStats.isHealthy(), "Node should be healthy");
    assertTrue(nodeStats.getActiveConnections() >= 0, "Active connections should be non-negative");
    assertTrue(nodeStats.getMemoryUsage() > 0, "Memory usage should be positive");
    assertTrue(nodeStats.getLastSeen() > 0, "Last seen timestamp should be positive");
  }

  @Test
  void testHealthStatusPersistence() throws InterruptedException {
    // Get initial health status
    Map<String, ClusterAwareResourceManager.ClusterNodeStats> initialStats =
        resourceManager.getClusterStats();
    String nodeId = resourceManager.getNodeId();
    ClusterAwareResourceManager.ClusterNodeStats initialNodeStats = initialStats.get(nodeId);

    assertTrue(initialNodeStats.isHealthy(), "Node should initially be healthy");

    // Wait a bit and update health
    Thread.sleep(100);
    resourceManager.updateNodeHealth();

    // Get updated stats
    Map<String, ClusterAwareResourceManager.ClusterNodeStats> updatedStats =
        resourceManager.getClusterStats();
    ClusterAwareResourceManager.ClusterNodeStats updatedNodeStats = updatedStats.get(nodeId);

    assertTrue(updatedNodeStats.isHealthy(), "Node should remain healthy");
    assertTrue(
        updatedNodeStats.getLastSeen() >= initialNodeStats.getLastSeen(),
        "Last seen timestamp should be updated");
  }

  @Test
  void testStatelessOperationIdempotency() {
    // Ensure stateless operation multiple times should not cause issues
    for (int i = 0; i < 5; i++) {
      assertDoesNotThrow(
          () -> {
            resourceManager.ensureStatelessOperation();
          },
          "Multiple calls to ensureStatelessOperation should not throw exceptions");
    }

    // Verify node is still healthy
    assertTrue(resourceManager.isClusterHealthy(), "Cluster should remain healthy");

    Map<String, ClusterAwareResourceManager.ClusterNodeStats> stats =
        resourceManager.getClusterStats();
    String nodeId = resourceManager.getNodeId();
    ClusterAwareResourceManager.ClusterNodeStats nodeStats = stats.get(nodeId);

    assertTrue(
        nodeStats.isHealthy(), "Node should remain healthy after multiple stateless operations");
  }
}
