package com.bioid.keycloak.client.connection;

import static org.assertj.core.api.Assertions.*;

import com.bioid.keycloak.client.config.BioIdClientConfig;
import com.bioid.keycloak.client.exception.BioIdServiceException;
import io.grpc.ManagedChannel;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for BioIdConnectionManager. Tests connection pooling, circuit breaker, and health
 * monitoring functionality.
 */
class BioIdConnectionManagerTest {

  private BioIdConnectionManager connectionManager;
  private SimpleMeterRegistry meterRegistry;
  private BioIdClientConfig config;

  @BeforeEach
  void setUp() {
    meterRegistry = new SimpleMeterRegistry();
    config =
        BioIdClientConfig.builder()
            .endpoint("localhost:9090")
            .clientId("test-client")
            .secretKey("test-secret-key-for-testing")
            .channelPoolSize(3)
            .connectTimeout(Duration.ofSeconds(1))
            .healthCheckInterval(Duration.ofSeconds(1))
            .maxRetryAttempts(2)
            .build();
  }

  @AfterEach
  void tearDown() {
    if (connectionManager != null) {
      connectionManager.close();
    }
  }

  @Test
  @DisplayName("Should initialize connection manager with correct configuration")
  void shouldInitializeConnectionManager() {
    // When
    connectionManager = new BioIdConnectionManager(config, meterRegistry);

    // Then
    assertThat(connectionManager.getCurrentEndpoint()).isEqualTo("localhost:9090");
    assertThat(connectionManager.isHealthy()).isTrue();

    // Verify metrics are registered
    assertThat(meterRegistry.getMeters()).isNotEmpty();
    assertThat(meterRegistry.find("bioid.connection.attempts").counter()).isNotNull();
    assertThat(meterRegistry.find("bioid.connection.failures").counter()).isNotNull();
    assertThat(meterRegistry.find("bioid.circuit_breaker.trips").counter()).isNotNull();
  }

  @Test
  @DisplayName("Should provide channels using round-robin load balancing")
  void shouldProvideChannelsWithRoundRobin() throws BioIdServiceException {
    // Given
    connectionManager = new BioIdConnectionManager(config, meterRegistry);

    // When - Get multiple channels
    ManagedChannel channel1 = connectionManager.getChannel();
    ManagedChannel channel2 = connectionManager.getChannel();
    ManagedChannel channel3 = connectionManager.getChannel();
    ManagedChannel channel4 = connectionManager.getChannel(); // Should wrap around

    // Then
    assertThat(channel1).isNotNull();
    assertThat(channel2).isNotNull();
    assertThat(channel3).isNotNull();
    assertThat(channel4).isNotNull();

    // Channels should be different (round-robin)
    assertThat(channel1).isNotSameAs(channel2);
    assertThat(channel2).isNotSameAs(channel3);

    // Fourth channel should be same as first (round-robin wrap)
    assertThat(channel4).isSameAs(channel1);
  }

  @Test
  @DisplayName("Should record success and reset failure count")
  void shouldRecordSuccessAndResetFailures() {
    // Given
    connectionManager = new BioIdConnectionManager(config, meterRegistry);

    // When - Record some failures then success
    connectionManager.recordFailure();
    connectionManager.recordFailure();
    connectionManager.recordSuccess();

    // Then - Should still be healthy (below failure threshold)
    assertThat(connectionManager.isHealthy()).isTrue();
  }

  @Test
  @DisplayName("Should trip circuit breaker after failure threshold")
  void shouldTripCircuitBreakerAfterFailures() {
    // Given
    connectionManager = new BioIdConnectionManager(config, meterRegistry);

    // When - Record failures beyond threshold (5 failures)
    for (int i = 0; i < 6; i++) {
      connectionManager.recordFailure();
    }

    // Then - Circuit breaker should be tripped
    assertThat(connectionManager.isHealthy()).isFalse();

    // Should throw exception when trying to get channel
    assertThatThrownBy(() -> connectionManager.getChannel())
        .isInstanceOf(BioIdServiceException.class)
        .hasMessageContaining("Circuit breaker is OPEN");

    // Verify circuit breaker trip metric
    assertThat(meterRegistry.find("bioid.circuit_breaker.trips").counter().count()).isEqualTo(1.0);
  }

  @Test
  @DisplayName("Should handle graceful shutdown")
  void shouldHandleGracefulShutdown() {
    // Given
    connectionManager = new BioIdConnectionManager(config, meterRegistry);

    // When
    assertThatCode(() -> connectionManager.close()).doesNotThrowAnyException();

    // Then - Should be able to call close multiple times
    assertThatCode(() -> connectionManager.close()).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("Should validate configuration parameters")
  void shouldValidateConfigurationParameters() {
    // When & Then - Should throw when creating invalid configs
    assertThatThrownBy(
            () ->
                BioIdClientConfig.builder().endpoint("").clientId("test").secretKey("test").build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Endpoint cannot be empty");

    assertThatThrownBy(
            () ->
                BioIdClientConfig.builder()
                    .endpoint("localhost:9090")
                    .clientId("")
                    .secretKey("test")
                    .build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Client ID cannot be empty");
  }

  @Test
  @DisplayName("Should handle connection failures gracefully")
  void shouldHandleConnectionFailuresGracefully() {
    // Given - Configuration with invalid endpoint
    BioIdClientConfig invalidEndpointConfig =
        BioIdClientConfig.builder()
            .endpoint("invalid-endpoint:9999")
            .clientId("test-client")
            .secretKey("test-secret-key-for-testing")
            .channelPoolSize(1)
            .connectTimeout(Duration.ofMillis(100))
            .build();

    // When & Then - Should not throw during initialization
    assertThatCode(
            () -> {
              connectionManager = new BioIdConnectionManager(invalidEndpointConfig, meterRegistry);
            })
        .doesNotThrowAnyException();

    // Connection failures should be recorded in metrics
    assertThat(meterRegistry.find("bioid.connection.attempts").counter().count()).isGreaterThan(0);
  }

  @Test
  @DisplayName("Should support fallback endpoints configuration")
  void shouldSupportFallbackEndpoints() {
    // Given
    BioIdClientConfig configWithFallbacks =
        BioIdClientConfig.builder()
            .endpoint("primary-endpoint:9090")
            .fallbackEndpoints(List.of("fallback1:9090", "fallback2:9090"))
            .clientId("test-client")
            .secretKey("test-secret-key-for-testing")
            .build();

    // When
    connectionManager = new BioIdConnectionManager(configWithFallbacks, meterRegistry);

    // Then
    assertThat(connectionManager.getCurrentEndpoint()).isEqualTo("primary-endpoint:9090");
    assertThat(connectionManager).isNotNull();
  }
}
