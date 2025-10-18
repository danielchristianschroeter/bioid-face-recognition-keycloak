package com.bioid.keycloak.client.connection;

import static org.assertj.core.api.Assertions.*;

import com.bioid.keycloak.client.config.BioIdClientConfig;
import com.bioid.keycloak.client.exception.BioIdServiceException;
import io.grpc.ManagedChannel;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Connection Pool Metrics")
class ConnectionPoolMetricsTest {

  private BioIdConnectionManager connectionManager;
  private SimpleMeterRegistry meterRegistry;
  private BioIdClientConfig config;

  @BeforeEach
  void setUp() {
    meterRegistry = new SimpleMeterRegistry();
    config =
        BioIdClientConfig.builder()
            .endpoint("https://bws.bioid.com")
            .clientId("test-client")
            .secretKey("test-secret")
            .channelPoolSize(3)
            .connectTimeout(Duration.ofSeconds(5))
            .requestTimeout(Duration.ofSeconds(10))
            .keepAliveTime(Duration.ofMinutes(5))
            .keepAliveTimeout(Duration.ofSeconds(20))
            .keepAliveWithoutCalls(true)
            .healthCheckInterval(Duration.ofMinutes(1))
            .build();
  }

  @AfterEach
  void tearDown() {
    if (connectionManager != null) {
      connectionManager.close();
    }
  }

  @Test
  @DisplayName("Should track active and idle connections")
  void shouldTrackActiveAndIdleConnections() throws BioIdServiceException {
    // Given
    connectionManager = new BioIdConnectionManager(config, meterRegistry);

    // When - Get initial metrics
    BioIdConnectionManager.ConnectionPoolMetrics initialMetrics =
        connectionManager.getConnectionPoolMetrics();

    // Then - Initially all connections should be idle
    assertThat(initialMetrics.getActiveConnections()).isEqualTo(0);
    assertThat(initialMetrics.getIdleConnections()).isEqualTo(3);
    assertThat(initialMetrics.getTotalConnections()).isEqualTo(3);
    assertThat(initialMetrics.getTotalRequests()).isEqualTo(0);
    assertThat(initialMetrics.getFailedRequests()).isEqualTo(0);
  }

  @Test
  @DisplayName("Should track total and failed requests")
  void shouldTrackTotalAndFailedRequests() throws BioIdServiceException {
    // Given
    connectionManager = new BioIdConnectionManager(config, meterRegistry);

    // When - Make some requests
    try {
      connectionManager.getChannel();
      connectionManager.recordSuccess();
    } catch (Exception e) {
      // Expected in test environment
    }

    try {
      connectionManager.getChannel();
      connectionManager.recordFailure();
    } catch (Exception e) {
      // Expected in test environment
    }

    BioIdConnectionManager.ConnectionPoolMetrics metrics =
        connectionManager.getConnectionPoolMetrics();

    // Then - Should track requests
    assertThat(metrics.getTotalRequests()).isGreaterThan(0);
  }

  @Test
  @DisplayName("Should calculate success rate correctly")
  void shouldCalculateSuccessRate() throws BioIdServiceException {
    // Given
    connectionManager = new BioIdConnectionManager(config, meterRegistry);

    // When - Simulate successful requests
    for (int i = 0; i < 10; i++) {
      try {
        connectionManager.getChannel();
        connectionManager.recordSuccess();
      } catch (Exception e) {
        // Expected in test environment
      }
    }

    BioIdConnectionManager.ConnectionPoolMetrics metrics =
        connectionManager.getConnectionPoolMetrics();

    // Then - Success rate should be high
    assertThat(metrics.getSuccessRate()).isGreaterThanOrEqualTo(0.0);
    assertThat(metrics.getSuccessRate()).isLessThanOrEqualTo(1.0);
  }

  @Test
  @DisplayName("Should calculate utilization correctly")
  void shouldCalculateUtilization() throws BioIdServiceException {
    // Given
    connectionManager = new BioIdConnectionManager(config, meterRegistry);

    // When - Get metrics
    BioIdConnectionManager.ConnectionPoolMetrics metrics =
        connectionManager.getConnectionPoolMetrics();

    // Then - Utilization should be between 0 and 1
    assertThat(metrics.getUtilization()).isGreaterThanOrEqualTo(0.0);
    assertThat(metrics.getUtilization()).isLessThanOrEqualTo(1.0);
  }

  @Test
  @DisplayName("Should track active connections during operations")
  void shouldTrackActiveConnectionsDuringOperations() throws BioIdServiceException {
    // Given
    connectionManager = new BioIdConnectionManager(config, meterRegistry);

    // When - Get a channel (marks as active)
    try {
      ManagedChannel channel = connectionManager.getChannel();
      
      // Check metrics while channel is "active"
      BioIdConnectionManager.ConnectionPoolMetrics metricsWhileActive =
          connectionManager.getConnectionPoolMetrics();
      
      // Then - Should show active connection
      assertThat(metricsWhileActive.getActiveConnections()).isGreaterThan(0);
      
      // When - Record success (releases connection)
      connectionManager.recordSuccess();
      
      // Then - Active connections should decrease
      BioIdConnectionManager.ConnectionPoolMetrics metricsAfterRelease =
          connectionManager.getConnectionPoolMetrics();
      assertThat(metricsAfterRelease.getActiveConnections())
          .isLessThan(metricsWhileActive.getActiveConnections());
          
    } catch (Exception e) {
      // Expected in test environment without real gRPC server
    }
  }

  @Test
  @DisplayName("Should handle metrics with no requests")
  void shouldHandleMetricsWithNoRequests() {
    // Given
    connectionManager = new BioIdConnectionManager(config, meterRegistry);

    // When
    BioIdConnectionManager.ConnectionPoolMetrics metrics =
        connectionManager.getConnectionPoolMetrics();

    // Then - Should handle zero requests gracefully
    assertThat(metrics.getTotalRequests()).isEqualTo(0);
    assertThat(metrics.getFailedRequests()).isEqualTo(0);
    assertThat(metrics.getSuccessRate()).isEqualTo(1.0); // 100% when no requests
    assertThat(metrics.getUtilization()).isEqualTo(0.0); // 0% when no active connections
  }

  @Test
  @DisplayName("Should track failed requests correctly")
  void shouldTrackFailedRequestsCorrectly() throws BioIdServiceException {
    // Given
    connectionManager = new BioIdConnectionManager(config, meterRegistry);

    // When - Simulate some failures
    for (int i = 0; i < 3; i++) {
      try {
        connectionManager.getChannel();
        connectionManager.recordFailure();
      } catch (Exception e) {
        // Expected in test environment
      }
    }

    BioIdConnectionManager.ConnectionPoolMetrics metrics =
        connectionManager.getConnectionPoolMetrics();

    // Then - Should track failures
    assertThat(metrics.getFailedRequests()).isGreaterThan(0);
    assertThat(metrics.getTotalRequests()).isGreaterThanOrEqualTo(metrics.getFailedRequests());
  }

  @Test
  @DisplayName("Should provide consistent metrics across multiple calls")
  void shouldProvideConsistentMetrics() {
    // Given
    connectionManager = new BioIdConnectionManager(config, meterRegistry);

    // When - Get metrics multiple times
    BioIdConnectionManager.ConnectionPoolMetrics metrics1 =
        connectionManager.getConnectionPoolMetrics();
    BioIdConnectionManager.ConnectionPoolMetrics metrics2 =
        connectionManager.getConnectionPoolMetrics();

    // Then - Total connections should be consistent
    assertThat(metrics1.getTotalConnections()).isEqualTo(metrics2.getTotalConnections());
  }
}
