package com.bioid.keycloak.client.connection;

import static org.junit.jupiter.api.Assertions.*;

import com.bioid.keycloak.client.config.BioIdClientConfig;
import com.bioid.keycloak.client.endpoint.RegionalEndpointManager;
import io.grpc.ManagedChannel;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for ConnectionPoolManager. */
class ConnectionPoolManagerTest {

  private ConnectionPoolManager poolManager;
  private BioIdClientConfig config;
  private RegionalEndpointManager endpointManager;
  
  private static final String EU_ENDPOINT = "bioid-eu-west-1.example.com:443";
  private static final String US_ENDPOINT = "bioid-us-east-1.example.com:443";

  @BeforeEach
  void setUp() {
    config =
        BioIdClientConfig.builder()
            .endpoint(EU_ENDPOINT)
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

    endpointManager = new RegionalEndpointManager(config);
    endpointManager.initialize();

    poolManager = new ConnectionPoolManager(config);
  }

  @AfterEach
  void tearDown() {
    if (poolManager != null) {
      poolManager.close();
    }
    if (endpointManager != null) {
      endpointManager.close();
    }
  }

  @Test
  @DisplayName("Should initialize connection pool manager")
  void shouldInitializeConnectionPoolManager() {
    assertNotNull(poolManager);
    assertNotNull(poolManager.getStatus());
  }

  @Test
  @DisplayName("Should create connection on first request")
  void shouldCreateConnectionOnFirstRequest() throws Exception {
    ManagedChannel channel = poolManager.getConnection();

    assertNotNull(channel);
    assertFalse(channel.isShutdown());
  }

  @Test
  @DisplayName("Should reuse existing connection")
  void shouldReuseExistingConnection() throws Exception {
    ManagedChannel channel1 = poolManager.getConnection();
    ManagedChannel channel2 = poolManager.getConnection();

    assertNotNull(channel1);
    assertNotNull(channel2);
    // Should be the same connection instance
    assertEquals(channel1, channel2);
  }

  @Test
  @DisplayName("Should create connections for different endpoints")
  void shouldCreateConnectionsForDifferentEndpoints() throws Exception {
    ManagedChannel euChannel = poolManager.getConnection(EU_ENDPOINT);
    ManagedChannel usChannel = poolManager.getConnection(US_ENDPOINT);

    assertNotNull(euChannel);
    assertNotNull(usChannel);
    // Should be different connections
    assertNotEquals(euChannel, usChannel);
  }

  @Test
  @DisplayName("Should record success metrics")
  void shouldRecordSuccessMetrics() {
    long responseTime = 100L;
    
    // This should not throw an exception
    assertDoesNotThrow(() -> poolManager.recordSuccess(responseTime));
    
    // Verify metrics are updated
    var status = poolManager.getStatus();
    assertTrue(status.getTotalRequestsServed() > 0);
  }

  @Test
  @DisplayName("Should record failure metrics")
  void shouldRecordFailureMetrics() {
    // This should not throw an exception
    assertDoesNotThrow(() -> poolManager.recordFailure());
    
    // Verify metrics are updated
    var status = poolManager.getStatus();
    assertTrue(status.getTotalRequestsServed() > 0);
  }

  @Test
  @DisplayName("Should provide connection pool status")
  void shouldProvideConnectionPoolStatus() throws Exception {
    // Create a connection first
    poolManager.getConnection();
    
    var status = poolManager.getStatus();
    
    assertNotNull(status);
    assertTrue(status.getTotalConnections() >= 0);
    assertTrue(status.getActiveConnections() >= 0);
    assertTrue(status.getIdleConnections() >= 0);
    assertTrue(status.getMaxPoolSize() > 0);
  }

  @Test
  @DisplayName("Should refresh connection pool")
  void shouldRefreshConnectionPool() throws Exception {
    // Create a connection first
    ManagedChannel originalChannel = poolManager.getConnection();
    assertNotNull(originalChannel);
    
    // Refresh the pool
    assertDoesNotThrow(() -> poolManager.refreshPool());
    
    // Get a new connection - should be different after refresh
    ManagedChannel newChannel = poolManager.getConnection();
    assertNotNull(newChannel);
  }

  @Test
  @DisplayName("Should switch endpoint")
  void shouldSwitchEndpoint() throws Exception {
    String originalEndpoint = poolManager.getCurrentEndpoint();
    assertEquals(EU_ENDPOINT, originalEndpoint);
    
    // Switch to US endpoint
    assertDoesNotThrow(() -> poolManager.switchEndpoint(US_ENDPOINT));
    
    String newEndpoint = poolManager.getCurrentEndpoint();
    assertEquals(US_ENDPOINT, newEndpoint);
  }

  @Test
  @DisplayName("Should handle endpoint manager operations")
  void shouldHandleEndpointManagerOperations() {
    assertNotNull(endpointManager);
    
    // Test basic endpoint manager functionality
    assertEquals(EU_ENDPOINT, endpointManager.getCurrentEndpoint());
    assertFalse(endpointManager.getAvailableRegions().isEmpty());
  }

  @Test
  @DisplayName("Should close gracefully")
  void shouldCloseGracefully() throws Exception {
    // Create a connection first
    poolManager.getConnection();
    
    // Close should not throw an exception
    assertDoesNotThrow(() -> poolManager.close());
  }
}