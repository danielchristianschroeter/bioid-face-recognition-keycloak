package com.bioid.keycloak.client.connection;

import static org.junit.jupiter.api.Assertions.*;

import com.bioid.keycloak.client.config.BioIdClientConfig;
import com.bioid.keycloak.client.endpoint.RegionalEndpointManager;
import io.grpc.ManagedChannel;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for ConnectionPoolManager. */
class ConnectionPoolManagerTest {

  private ConnectionPoolManager poolManager;
  private BioIdClientConfig config;
  private RegionalEndpointManager endpointManager;

  @BeforeEach
  void setUp() {
    config =
        BioIdClientConfig.builder()
            .endpoint(RegionalEndpointManager.EU_ENDPOINT)
            .clientId("test-client")
            .secretKey("test-key")
            .channelPoolSize(3)
            .keepAliveTime(Duration.ofSeconds(30))
            .keepAliveTimeout(Duration.ofSeconds(30))
            .keepAliveWithoutCalls(true)
            .verificationTimeout(Duration.ofSeconds(4))
            .enrollmentTimeout(Duration.ofSeconds(7))
            .requestTimeout(Duration.ofSeconds(4))
            .connectTimeout(Duration.ofSeconds(10))
            .maxRetryAttempts(3)
            .retryBackoffMultiplier(2.0)
            .healthCheckInterval(Duration.ofSeconds(30))
            .tlsEnabled(true)
            .build();

    endpointManager = new RegionalEndpointManager(RegionalEndpointManager.Region.EU, false);

    poolManager = new ConnectionPoolManager(config, endpointManager);
  }

  @AfterEach
  void tearDown() {
    if (poolManager != null) {
      poolManager.shutdown();
    }
  }

  @Test
  @DisplayName("Should initialize connection pool manager")
  void shouldInitializeConnectionPoolManager() {
    assertNotNull(poolManager);
    assertFalse(poolManager.isShutdown());
    assertEquals(0, poolManager.getPoolCount()); // No pools created yet
  }

  @Test
  @DisplayName("Should create connection pool on first channel request")
  void shouldCreateConnectionPoolOnFirstChannelRequest() {
    ManagedChannel channel = poolManager.getChannel();

    assertNotNull(channel);
    assertFalse(channel.isShutdown());
    assertEquals(1, poolManager.getPoolCount());
  }

  @Test
  @DisplayName("Should reuse existing connection pool")
  void shouldReuseExistingConnectionPool() {
    ManagedChannel channel1 = poolManager.getChannel();
    ManagedChannel channel2 = poolManager.getChannel();

    assertNotNull(channel1);
    assertNotNull(channel2);
    assertEquals(1, poolManager.getPoolCount()); // Still only one pool
  }

  @Test
  @DisplayName("Should create separate pools for different endpoints")
  void shouldCreateSeparatePoolsForDifferentEndpoints() {
    ManagedChannel euChannel = poolManager.getChannel(RegionalEndpointManager.EU_ENDPOINT);
    ManagedChannel usChannel = poolManager.getChannel(RegionalEndpointManager.US_ENDPOINT);

    assertNotNull(euChannel);
    assertNotNull(usChannel);
    assertEquals(2, poolManager.getPoolCount());
  }

  @Test
  @DisplayName("Should get channel with failover")
  void shouldGetChannelWithFailover() {
    ManagedChannel channel = poolManager.getChannelWithFailover();

    assertNotNull(channel);
    assertFalse(channel.isShutdown());
  }

  @Test
  @DisplayName("Should report success to endpoint manager")
  void shouldReportSuccessToEndpointManager() {
    String endpoint = RegionalEndpointManager.EU_ENDPOINT;
    Duration latency = Duration.ofMillis(100);

    // This should not throw an exception
    assertDoesNotThrow(() -> poolManager.reportSuccess(endpoint, latency));

    // Verify endpoint manager received the success report
    Map<String, RegionalEndpointManager.EndpointHealth> healthStatus =
        endpointManager.getHealthStatus();
    RegionalEndpointManager.EndpointHealth health = healthStatus.get(endpoint);

    assertNotNull(health);
    assertTrue(health.isHealthy());
    assertEquals(0, health.getConsecutiveFailures());
  }

  @Test
  @DisplayName("Should report failure to endpoint manager")
  void shouldReportFailureToEndpointManager() {
    String endpoint = RegionalEndpointManager.EU_ENDPOINT;
    String errorMessage = "Connection failed";

    // This should not throw an exception
    assertDoesNotThrow(() -> poolManager.reportFailure(endpoint, errorMessage));

    // Verify endpoint manager received the failure report
    Map<String, RegionalEndpointManager.EndpointHealth> healthStatus =
        endpointManager.getHealthStatus();
    RegionalEndpointManager.EndpointHealth health = healthStatus.get(endpoint);

    assertNotNull(health);
    assertEquals(1, health.getConsecutiveFailures());
    assertEquals(errorMessage, health.getErrorMessage());
  }

  @Test
  @DisplayName("Should perform health check")
  void shouldPerformHealthCheck() {
    // Create a pool first
    poolManager.getChannel();

    // This should not throw an exception
    assertDoesNotThrow(() -> poolManager.performHealthCheck());
  }

  @Test
  @DisplayName("Should provide pool information")
  void shouldProvidePoolInformation() {
    // Create pools for different endpoints
    poolManager.getChannel(RegionalEndpointManager.EU_ENDPOINT);
    poolManager.getChannel(RegionalEndpointManager.US_ENDPOINT);

    Map<String, String> poolInfo = poolManager.getPoolInfo();

    assertEquals(2, poolInfo.size());
    assertTrue(poolInfo.containsKey(RegionalEndpointManager.EU_ENDPOINT));
    assertTrue(poolInfo.containsKey(RegionalEndpointManager.US_ENDPOINT));

    // Verify pool info contains expected information
    String euInfo = poolInfo.get(RegionalEndpointManager.EU_ENDPOINT);
    assertTrue(euInfo.contains("Pool size: 3"));
    assertTrue(euInfo.contains("Shutdown: false"));
  }

  @Test
  @DisplayName("Should shutdown all pools")
  void shouldShutdownAllPools() {
    // Create some pools
    poolManager.getChannel(RegionalEndpointManager.EU_ENDPOINT);
    poolManager.getChannel(RegionalEndpointManager.US_ENDPOINT);

    assertEquals(2, poolManager.getPoolCount());

    // Shutdown
    poolManager.shutdown();

    assertTrue(poolManager.isShutdown());
    assertEquals(0, poolManager.getPoolCount());
  }

  @Test
  @DisplayName("Should throw exception when getting channel after shutdown")
  void shouldThrowExceptionWhenGettingChannelAfterShutdown() {
    poolManager.shutdown();

    assertThrows(IllegalStateException.class, () -> poolManager.getChannel());
    assertThrows(IllegalStateException.class, () -> poolManager.getChannelWithFailover());
  }

  @Test
  @DisplayName("Should get endpoint manager")
  void shouldGetEndpointManager() {
    RegionalEndpointManager manager = poolManager.getEndpointManager();

    assertNotNull(manager);
    assertEquals(endpointManager, manager);
  }

  @Test
  @DisplayName("Connection pool should handle round-robin channel selection")
  void connectionPoolShouldHandleRoundRobinChannelSelection() {
    ConnectionPoolManager.ConnectionPool pool =
        new ConnectionPoolManager.ConnectionPool(RegionalEndpointManager.EU_ENDPOINT, 3, config);

    try {
      // Get multiple channels - should use round-robin
      ManagedChannel channel1 = pool.getChannel();
      ManagedChannel channel2 = pool.getChannel();
      ManagedChannel channel3 = pool.getChannel();
      ManagedChannel channel4 = pool.getChannel(); // Should wrap around

      assertNotNull(channel1);
      assertNotNull(channel2);
      assertNotNull(channel3);
      assertNotNull(channel4);

      assertEquals(3, pool.getPoolSize());
      assertEquals(RegionalEndpointManager.EU_ENDPOINT, pool.getEndpoint());
      assertFalse(pool.isShutdown());
    } finally {
      pool.shutdown();
    }
  }

  @Test
  @DisplayName("Connection pool should shutdown gracefully")
  void connectionPoolShouldShutdownGracefully() {
    ConnectionPoolManager.ConnectionPool pool =
        new ConnectionPoolManager.ConnectionPool(RegionalEndpointManager.EU_ENDPOINT, 2, config);

    assertFalse(pool.isShutdown());

    // Get a channel to ensure pool is active
    ManagedChannel channel = pool.getChannel();
    assertNotNull(channel);

    // Shutdown
    pool.shutdown();

    assertTrue(pool.isShutdown());

    // Should throw exception when trying to get channel after shutdown
    assertThrows(IllegalStateException.class, pool::getChannel);
  }
}
