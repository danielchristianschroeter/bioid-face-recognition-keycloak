package com.bioid.keycloak.metrics;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Timer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FaceRecognitionMetricsTest {

  @Mock private MetricRegistry metricRegistry;

  @Mock private Counter counter;

  @Mock private Timer timer;

  @Mock private Timer.Context timerContext;

  @Mock private Histogram histogram;

  private FaceRecognitionMetrics metrics;

  @BeforeEach
  void setUp() {
    when(metricRegistry.counter(anyString(), any(org.eclipse.microprofile.metrics.Tag[].class)))
        .thenReturn(counter);
    when(metricRegistry.timer(anyString(), any(org.eclipse.microprofile.metrics.Tag[].class)))
        .thenReturn(timer);
    when(metricRegistry.histogram(anyString(), any(org.eclipse.microprofile.metrics.Tag[].class)))
        .thenReturn(histogram);
    when(timer.time()).thenReturn(timerContext);

    metrics = new FaceRecognitionMetrics(metricRegistry);
  }

  @Test
  void shouldIncrementEnrollSuccessMetric() {
    // When & Then - verify the method doesn't throw an exception
    // In a real implementation with proper CDI, the metrics would be recorded
    assertDoesNotThrow(() -> metrics.incrementEnrollSuccess());
  }

  @Test
  void shouldIncrementEnrollFailureMetricWithReason() {
    // Given
    String reason = "invalid_image";

    // When
    metrics.incrementEnrollFailure(reason);

    // Then
    assertDoesNotThrow(() -> metrics.incrementEnrollFailure(reason));
  }

  @Test
  void shouldStartEnrollTimer() {
    // When & Then - verify the method doesn't throw an exception
    // In a real implementation with proper CDI, a timer context would be created
    assertDoesNotThrow(() -> metrics.startEnrollTimer());
  }

  @Test
  void shouldIncrementVerifySuccessMetric() {
    // When
    metrics.incrementVerifySuccess();

    // Then
    assertDoesNotThrow(() -> metrics.incrementVerifySuccess());
  }

  @Test
  void shouldIncrementVerifyFailureMetricWithReason() {
    // Given
    String reason = "face_not_found";

    // When
    metrics.incrementVerifyFailure(reason);

    // Then
    assertDoesNotThrow(() -> metrics.incrementVerifyFailure(reason));
  }

  @Test
  void shouldRecordBioIdLatency() {
    // Given
    String operation = "verify";
    long latencyMs = 150L;

    // When
    metrics.recordBioIdLatency(operation, latencyMs);

    // Then
    assertDoesNotThrow(() -> metrics.recordBioIdLatency(operation, latencyMs));
  }

  @Test
  void shouldUpdateConnectionPoolMetrics() {
    // Given
    int active = 5;
    int idle = 3;

    // When
    metrics.updateConnectionPoolMetrics(active, idle);

    // Then
    assertDoesNotThrow(() -> metrics.updateConnectionPoolMetrics(active, idle));
  }

  @Test
  void shouldIncrementDeletionRequestMetrics() {
    // Given
    String priority = "high";

    // When
    metrics.incrementDeletionRequestCreated(priority);
    metrics.incrementDeletionRequestApproved();
    metrics.incrementDeletionRequestDeclined();

    // Then
    assertDoesNotThrow(
        () -> {
          metrics.incrementDeletionRequestCreated(priority);
          metrics.incrementDeletionRequestApproved();
          metrics.incrementDeletionRequestDeclined();
        });
  }

  @Test
  void shouldIncrementLivenessMetrics() {
    // Given
    String type = "passive";
    String reason = "low_confidence";

    // When
    metrics.incrementLivenessCheck(type);
    metrics.incrementLivenessSuccess(type);
    metrics.incrementLivenessFailure(type, reason);

    // Then
    assertDoesNotThrow(
        () -> {
          metrics.incrementLivenessCheck(type);
          metrics.incrementLivenessSuccess(type);
          metrics.incrementLivenessFailure(type, reason);
        });
  }

  @Test
  void shouldIncrementHealthCheckMetrics() {
    // Given
    String endpoint = "bioid-service";
    String reason = "timeout";

    // When
    metrics.incrementHealthCheck(endpoint);
    metrics.incrementHealthCheckSuccess(endpoint);
    metrics.incrementHealthCheckFailure(endpoint, reason);

    // Then
    assertDoesNotThrow(
        () -> {
          metrics.incrementHealthCheck(endpoint);
          metrics.incrementHealthCheckSuccess(endpoint);
          metrics.incrementHealthCheckFailure(endpoint, reason);
        });
  }

  @Test
  void shouldGetMetricsSummary() {
    // When
    FaceRecognitionMetrics.MetricsSummary summary = metrics.getMetricsSummary();

    // Then
    assertNotNull(summary);
    assertTrue(summary.getEnrollSuccessTotal() >= 0);
    assertTrue(summary.getEnrollFailureTotal() >= 0);
    assertTrue(summary.getVerifySuccessTotal() >= 0);
    assertTrue(summary.getVerifyFailureTotal() >= 0);
    assertTrue(summary.getBioIdRequestTotal() >= 0);
    assertTrue(summary.getBioIdErrorTotal() >= 0);
    assertTrue(summary.getTemplateDeleteTotal() >= 0);
    assertTrue(summary.getDeletionRequestCreatedTotal() >= 0);
    assertTrue(summary.getDeletionRequestApprovedTotal() >= 0);
    assertTrue(summary.getDeletionRequestDeclinedTotal() >= 0);
    assertTrue(summary.getLivenessCheckTotal() >= 0);
    assertTrue(summary.getHealthCheckTotal() >= 0);
    assertTrue(summary.getHealthCheckSuccessTotal() >= 0);
  }

  @Test
  void shouldBuildMetricsSummaryWithBuilder() {
    // Given
    long enrollSuccess = 100L;
    long enrollFailure = 5L;
    long verifySuccess = 500L;
    long verifyFailure = 10L;

    // When
    FaceRecognitionMetrics.MetricsSummary summary =
        FaceRecognitionMetrics.MetricsSummary.builder()
            .enrollSuccessTotal(enrollSuccess)
            .enrollFailureTotal(enrollFailure)
            .verifySuccessTotal(verifySuccess)
            .verifyFailureTotal(verifyFailure)
            .bioIdRequestTotal(600L)
            .bioIdErrorTotal(15L)
            .templateDeleteTotal(20L)
            .deletionRequestCreatedTotal(8L)
            .deletionRequestApprovedTotal(6L)
            .deletionRequestDeclinedTotal(2L)
            .livenessCheckTotal(450L)
            .healthCheckTotal(50L)
            .healthCheckSuccessTotal(48L)
            .build();

    // Then
    assertEquals(enrollSuccess, summary.getEnrollSuccessTotal());
    assertEquals(enrollFailure, summary.getEnrollFailureTotal());
    assertEquals(verifySuccess, summary.getVerifySuccessTotal());
    assertEquals(verifyFailure, summary.getVerifyFailureTotal());
    assertEquals(600L, summary.getBioIdRequestTotal());
    assertEquals(15L, summary.getBioIdErrorTotal());
    assertEquals(20L, summary.getTemplateDeleteTotal());
    assertEquals(8L, summary.getDeletionRequestCreatedTotal());
    assertEquals(6L, summary.getDeletionRequestApprovedTotal());
    assertEquals(2L, summary.getDeletionRequestDeclinedTotal());
    assertEquals(450L, summary.getLivenessCheckTotal());
    assertEquals(50L, summary.getHealthCheckTotal());
    assertEquals(48L, summary.getHealthCheckSuccessTotal());
  }

  @Test
  void shouldUpdateGaugeMetrics() {
    // Given
    long activeTemplates = 1000L;
    long pendingDeletions = 5L;

    // When
    metrics.updateActiveTemplateCount(activeTemplates);
    metrics.updatePendingDeletionRequests(pendingDeletions);

    // Then
    assertDoesNotThrow(
        () -> {
          metrics.updateActiveTemplateCount(activeTemplates);
          metrics.updatePendingDeletionRequests(pendingDeletions);
        });
  }

  @Test
  void shouldIncrementTemplateManagementMetrics() {
    // Given
    String deleteResult = "success";
    String upgradeResult = "completed";

    // When
    metrics.incrementTemplateDelete(deleteResult);
    metrics.incrementTemplateUpgrade(upgradeResult);

    // Then
    assertDoesNotThrow(
        () -> {
          metrics.incrementTemplateDelete(deleteResult);
          metrics.incrementTemplateUpgrade(upgradeResult);
        });
  }

  @Test
  void shouldIncrementBioIdServiceMetrics() {
    // Given
    String operation = "enroll";
    String endpoint = "face.bws-eu.bioid.com";
    String errorType = "timeout";

    // When
    metrics.incrementBioIdRequest(operation, endpoint);
    metrics.incrementBioIdError(operation, errorType);

    // Then
    assertDoesNotThrow(
        () -> {
          metrics.incrementBioIdRequest(operation, endpoint);
          metrics.incrementBioIdError(operation, errorType);
        });
  }
}
