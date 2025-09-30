package com.bioid.keycloak.client.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import com.bioid.keycloak.client.exception.BioIdAuthenticationException;
import com.bioid.keycloak.client.exception.BioIdNetworkException;
import com.bioid.keycloak.client.exception.BioIdServiceException;
import com.bioid.keycloak.client.exception.BioIdTemplateException;
import com.bioid.keycloak.client.exception.BioIdValidationException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("BioIdMetrics")
class BioIdMetricsTest {
  private MeterRegistry meterRegistry;
  private BioIdMetrics metrics;

  @BeforeEach
  void setUp() {
    meterRegistry = new SimpleMeterRegistry();
    metrics = new BioIdMetrics(meterRegistry);
  }

  @Test
  @DisplayName("Should record enrollment metrics")
  void shouldRecordEnrollmentMetrics() {
    // When
    metrics.incrementEnrollmentAttempts();
    metrics.incrementEnrollmentSuccesses();

    Timer.Sample sample = metrics.startEnrollmentTimer();
    // Simulate some work
    try {
      Thread.sleep(10);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    metrics.stopEnrollmentTimer(sample);

    // Then
    assertThat(meterRegistry.find("bioid.enrollment.attempts").counter().count()).isEqualTo(1.0);
    assertThat(meterRegistry.find("bioid.enrollment.successes").counter().count()).isEqualTo(1.0);
    assertThat(meterRegistry.find("bioid.enrollment.failures").counter().count()).isEqualTo(0.0);
    assertThat(meterRegistry.find("bioid.enrollment.duration").timer().count()).isEqualTo(1);
    assertThat(
            meterRegistry
                .find("bioid.enrollment.duration")
                .timer()
                .totalTime(java.util.concurrent.TimeUnit.MILLISECONDS))
        .isPositive();
  }

  @Test
  @DisplayName("Should record verification metrics")
  void shouldRecordVerificationMetrics() {
    // When
    metrics.incrementVerificationAttempts();
    metrics.incrementVerificationSuccesses();

    Timer.Sample sample = metrics.startVerificationTimer();
    // Simulate some work
    try {
      Thread.sleep(10);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    metrics.stopVerificationTimer(sample);

    // Then
    assertThat(meterRegistry.find("bioid.verification.attempts").counter().count()).isEqualTo(1.0);
    assertThat(meterRegistry.find("bioid.verification.successes").counter().count()).isEqualTo(1.0);
    assertThat(meterRegistry.find("bioid.verification.failures").counter().count()).isEqualTo(0.0);
    assertThat(meterRegistry.find("bioid.verification.duration").timer().count()).isEqualTo(1);
    assertThat(
            meterRegistry
                .find("bioid.verification.duration")
                .timer()
                .totalTime(java.util.concurrent.TimeUnit.MILLISECONDS))
        .isPositive();
  }

  @Test
  @DisplayName("Should record template metrics")
  void shouldRecordTemplateMetrics() {
    // When
    metrics.incrementTemplateStatusRequests();
    metrics.incrementTemplateDeletions();
    metrics.incrementTemplateTagUpdates();

    Timer.Sample statusSample = metrics.startTemplateStatusTimer();
    Timer.Sample deletionSample = metrics.startTemplateDeletionTimer();
    Timer.Sample tagsSample = metrics.startTemplateTagsTimer();

    // Simulate some work
    try {
      Thread.sleep(10);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    metrics.stopTemplateStatusTimer(statusSample);
    metrics.stopTemplateDeletionTimer(deletionSample);
    metrics.stopTemplateTagsTimer(tagsSample);

    // Then
    assertThat(meterRegistry.find("bioid.template.status.requests").counter().count())
        .isEqualTo(1.0);
    assertThat(meterRegistry.find("bioid.template.deletions").counter().count()).isEqualTo(1.0);
    assertThat(meterRegistry.find("bioid.template.tag.updates").counter().count()).isEqualTo(1.0);

    assertThat(meterRegistry.find("bioid.template.status.duration").timer().count()).isEqualTo(1);
    assertThat(meterRegistry.find("bioid.template.deletion.duration").timer().count()).isEqualTo(1);
    assertThat(meterRegistry.find("bioid.template.tags.duration").timer().count()).isEqualTo(1);
  }

  @Test
  @DisplayName("Should record error metrics")
  void shouldRecordErrorMetrics() {
    // When
    metrics.incrementValidationErrors();
    metrics.incrementAuthenticationErrors();
    metrics.incrementServiceErrors();
    metrics.incrementNetworkErrors();
    metrics.incrementTemplateErrors();

    // Then
    assertThat(meterRegistry.find("bioid.errors.validation").counter().count()).isEqualTo(1.0);
    assertThat(meterRegistry.find("bioid.errors.authentication").counter().count()).isEqualTo(1.0);
    assertThat(meterRegistry.find("bioid.errors.service").counter().count()).isEqualTo(1.0);
    assertThat(meterRegistry.find("bioid.errors.network").counter().count()).isEqualTo(1.0);
    assertThat(meterRegistry.find("bioid.errors.template").counter().count()).isEqualTo(1.0);
  }

  @Test
  @DisplayName("Should record errors based on exception type")
  void shouldRecordErrorsBasedOnExceptionType() {
    // When
    metrics.recordError(new BioIdValidationException("Validation error", "VALIDATION_ERROR"));
    metrics.recordError(new BioIdAuthenticationException("Authentication error", "AUTH_ERROR"));
    metrics.recordError(new BioIdServiceException("Service error", "SERVICE_ERROR"));
    metrics.recordError(new BioIdNetworkException("Network error", "NETWORK_ERROR", true));
    metrics.recordError(new BioIdTemplateException("Template error", "TEMPLATE_ERROR", false));
    metrics.recordError(new RuntimeException("Unknown error")); // Should increment service errors

    // Then
    assertThat(meterRegistry.find("bioid.errors.validation").counter().count()).isEqualTo(1.0);
    assertThat(meterRegistry.find("bioid.errors.authentication").counter().count()).isEqualTo(1.0);
    assertThat(meterRegistry.find("bioid.errors.service").counter().count())
        .isEqualTo(2.0); // 1 service + 1
    // unknown
    assertThat(meterRegistry.find("bioid.errors.network").counter().count()).isEqualTo(1.0);
    assertThat(meterRegistry.find("bioid.errors.template").counter().count()).isEqualTo(1.0);
  }
}
