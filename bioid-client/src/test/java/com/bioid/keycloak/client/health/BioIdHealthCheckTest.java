package com.bioid.keycloak.client.health;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("BioIdHealthCheck")
class BioIdHealthCheckTest {

  @Test
  @DisplayName("Should report healthy status when initialized")
  void shouldReportHealthyStatusWhenInitialized() {
    // Given
    BioIdHealthCheck healthCheck = new BioIdHealthCheck();

    // When & Then
    assertThat(healthCheck.isHealthy()).isTrue();
    assertThat(healthCheck.getLastErrorMessage()).isEmpty();
  }

  @Test
  @DisplayName("Should report unhealthy status after marking unhealthy")
  void shouldReportUnhealthyStatusAfterMarkingUnhealthy() {
    // Given
    BioIdHealthCheck healthCheck = new BioIdHealthCheck();

    // When
    healthCheck.markUnhealthy("Service unavailable");

    // Then
    assertThat(healthCheck.isHealthy()).isFalse();
    assertThat(healthCheck.getLastErrorMessage()).isEqualTo("Service unavailable");
  }

  @Test
  @DisplayName("Should report healthy status after marking healthy")
  void shouldReportHealthyStatusAfterMarkingHealthy() {
    // Given
    BioIdHealthCheck healthCheck = new BioIdHealthCheck();
    healthCheck.markUnhealthy("Service unavailable");

    // When
    healthCheck.markHealthy();

    // Then
    assertThat(healthCheck.isHealthy()).isTrue();
    assertThat(healthCheck.getLastErrorMessage()).isEmpty();
  }

  @Test
  @DisplayName("Should report unhealthy status when last check exceeds timeout")
  void shouldReportUnhealthyStatusWhenLastCheckExceedsTimeout() throws Exception {
    // Given
    BioIdHealthCheck healthCheck = new BioIdHealthCheck(Duration.ofMillis(100));
    healthCheck.markHealthy();

    // When
    Thread.sleep(200); // Wait longer than the timeout

    // Then
    assertThat(healthCheck.isHealthy()).isFalse();
  }

  @Test
  @DisplayName("Should store and retrieve current endpoint")
  void shouldStoreAndRetrieveCurrentEndpoint() {
    // Given
    BioIdHealthCheck healthCheck = new BioIdHealthCheck();

    // When
    healthCheck.setCurrentEndpoint("localhost:9000");

    // Then
    assertThat(healthCheck.getCurrentEndpoint()).isEqualTo("localhost:9000");
  }

  @Test
  @DisplayName("Should update last successful check time when marked healthy")
  void shouldUpdateLastSuccessfulCheckTimeWhenMarkedHealthy() throws Exception {
    // Given
    BioIdHealthCheck healthCheck = new BioIdHealthCheck();
    Instant before = healthCheck.getLastSuccessfulCheck();

    // When
    Thread.sleep(100); // Ensure time passes
    healthCheck.markHealthy();

    // Then
    assertThat(healthCheck.getLastSuccessfulCheck()).isAfter(before);
  }
}
