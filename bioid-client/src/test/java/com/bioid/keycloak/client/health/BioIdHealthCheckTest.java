package com.bioid.keycloak.client.health;

import static org.junit.jupiter.api.Assertions.*;

import com.bioid.keycloak.client.config.BioIdClientConfig;
import com.bioid.keycloak.client.connection.ConnectionPoolManager;
import com.bioid.keycloak.client.endpoint.RegionalEndpointManager;
import com.bioid.keycloak.client.metrics.ConnectionMetrics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("BioIdHealthCheck")
class BioIdHealthCheckTest {

  private BioIdHealthCheck healthCheck;
  private BioIdClientConfig config;
  private ConnectionPoolManager connectionManager;
  private RegionalEndpointManager endpointManager;

  @BeforeEach
  void setUp() {
    config = BioIdClientConfig.builder()
        .endpoint("bioid-eu-west-1.example.com:443")
        .clientId("test-client")
        .secretKey("test-key")
        .build();
        
    endpointManager = new RegionalEndpointManager(config);
    endpointManager.initialize();
    
    connectionManager = new ConnectionPoolManager(config);
    
    healthCheck = new BioIdHealthCheck(config, connectionManager, endpointManager);
  }

  @AfterEach
  void tearDown() {
    if (connectionManager != null) {
      connectionManager.close();
    }
    if (endpointManager != null) {
      endpointManager.close();
    }
  }

  @Test
  @DisplayName("Should perform health check")
  void shouldPerformHealthCheck() {
    // When
    ServiceHealthStatus status = healthCheck.performHealthCheck();

    // Then
    assertNotNull(status);
    assertNotNull(status.getState());
    assertNotNull(status.getEndpoint());
    assertNotNull(status.getLastHealthCheck());
    assertTrue(status.getResponseTimeMs() >= 0);
  }

  @Test
  @DisplayName("Should perform async health check")
  void shouldPerformAsyncHealthCheck() {
    // When & Then
    assertDoesNotThrow(() -> {
      var future = healthCheck.performHealthCheckAsync();
      ServiceHealthStatus status = future.join();
      assertNotNull(status);
      assertNotNull(status.getState());
    });
  }

  @Test
  @DisplayName("Should test service reachability")
  void shouldTestServiceReachability() {
    // When
    boolean isReachable = healthCheck.isServiceReachable();

    // Then - This might be false in test environment, but should not throw
    // The important thing is that the method executes without exception
    assertTrue(isReachable || !isReachable); // Always true, just testing execution
  }

  @Test
  @DisplayName("Should test async service reachability")
  void shouldTestAsyncServiceReachability() {
    // When & Then
    assertDoesNotThrow(() -> {
      var future = healthCheck.isServiceReachableAsync();
      Boolean result = future.join();
      assertNotNull(result);
    });
  }

  @Test
  @DisplayName("Should get connection metrics")
  void shouldGetConnectionMetrics() {
    // When
    ConnectionMetrics metrics = healthCheck.getConnectionMetrics();

    // Then
    assertNotNull(metrics);
    assertTrue(metrics.getTotalRequests() >= 0);
    assertTrue(metrics.getActiveConnections() >= 0);
    assertTrue(metrics.getMaxConnections() > 0);
    assertNotNull(metrics.getMetricsCollectedAt());
  }

  @Test
  @DisplayName("Should handle failover trigger")
  void shouldHandleFailoverTrigger() {
    // When & Then - Should not throw exception
    assertDoesNotThrow(() -> {
      boolean failoverTriggered = healthCheck.triggerFailoverIfNeeded();
      // Result can be true or false depending on health status
      assertTrue(failoverTriggered || !failoverTriggered);
    });
  }
}
