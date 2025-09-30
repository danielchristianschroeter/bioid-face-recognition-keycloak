package com.bioid.keycloak;

import static org.junit.jupiter.api.Assertions.*;

import com.bioid.keycloak.admin.dto.DeletionRequestDto;
import com.bioid.keycloak.admin.dto.DeletionRequestPriority;
import com.bioid.keycloak.admin.dto.DeletionRequestStatus;
import com.bioid.keycloak.health.FaceRecognitionHealthCheck;
import com.bioid.keycloak.metrics.FaceRecognitionMetrics;
import org.junit.jupiter.api.Test;

/**
 * Simple compilation test to verify all classes can be instantiated and basic functionality works.
 * This test doesn't require full CDI or configuration setup.
 */
class SimpleCompilationTest {

  @Test
  void shouldCreateDeletionRequestDto() {
    // Given
    String id = "test-id";
    String userId = "user-123";
    String username = "testuser";
    String email = "test@example.com";
    String classId = "class-456";
    DeletionRequestStatus status = DeletionRequestStatus.PENDING;
    DeletionRequestPriority priority = DeletionRequestPriority.HIGH;
    String reason = "GDPR request";

    // When
    DeletionRequestDto dto = new DeletionRequestDto();
    dto.setId(id);
    dto.setUserId(userId);
    dto.setUsername(username);
    dto.setEmail(email);
    dto.setClassId(Long.parseLong(classId.replace("class-", "")));
    dto.setStatus(status);
    dto.setPriority(priority);
    dto.setReason(reason);
    dto.setTemplateExists(true);

    // Then
    assertEquals(id, dto.getId());
    assertEquals(userId, dto.getUserId());
    assertEquals(username, dto.getUsername());
    assertEquals(email, dto.getEmail());
    assertEquals(Long.valueOf(456L), dto.getClassId());
    assertEquals(status, dto.getStatus());
    assertEquals(priority, dto.getPriority());
    assertEquals(reason, dto.getReason());
    assertTrue(dto.getTemplateExists());
  }

  @Test
  void shouldCreateDeletionRequestStatus() {
    // Test enum values
    assertEquals("pending", DeletionRequestStatus.PENDING.getValue());
    assertEquals("in_progress", DeletionRequestStatus.IN_PROGRESS.getValue());
    assertEquals("approved", DeletionRequestStatus.APPROVED.getValue());
    assertEquals("completed", DeletionRequestStatus.COMPLETED.getValue());
    assertEquals("failed", DeletionRequestStatus.FAILED.getValue());
    assertEquals("declined", DeletionRequestStatus.DECLINED.getValue());
    assertEquals("cancelled", DeletionRequestStatus.CANCELLED.getValue());

    // Test fromValue method
    assertEquals(DeletionRequestStatus.PENDING, DeletionRequestStatus.fromValue("pending"));
    assertEquals(DeletionRequestStatus.COMPLETED, DeletionRequestStatus.fromValue("completed"));
  }

  @Test
  void shouldCreateDeletionRequestPriority() {
    // Test enum values
    assertEquals("urgent", DeletionRequestPriority.URGENT.getValue());
    assertEquals("high", DeletionRequestPriority.HIGH.getValue());
    assertEquals("normal", DeletionRequestPriority.NORMAL.getValue());
    assertEquals("low", DeletionRequestPriority.LOW.getValue());

    // Test fromValue method
    assertEquals(DeletionRequestPriority.URGENT, DeletionRequestPriority.fromValue("urgent"));
    assertEquals(DeletionRequestPriority.NORMAL, DeletionRequestPriority.fromValue("normal"));
  }

  @Test
  void shouldCreateMetricsInstance() {
    // When
    FaceRecognitionMetrics metrics = new FaceRecognitionMetrics();

    // Then
    assertNotNull(metrics);

    // Test that methods don't throw exceptions (even if they don't work without CDI)
    assertDoesNotThrow(
        () -> {
          FaceRecognitionMetrics.MetricsSummary summary = metrics.getMetricsSummary();
          assertNotNull(summary);
        });
  }

  @Test
  void shouldCreateHealthCheckInstance() {
    // When & Then - should handle missing configuration gracefully
    assertDoesNotThrow(
        () -> {
          try {
            FaceRecognitionHealthCheck healthCheck = new FaceRecognitionHealthCheck();
            assertNotNull(healthCheck);
          } catch (IllegalStateException e) {
            // Expected when configuration is missing - this is acceptable for compilation test
            assertTrue(e.getMessage().contains("Configuration validation failed"));
          }
        });
  }

  @Test
  void shouldCreateConnectionPoolMetrics() {
    // Given
    int active = 5;
    int idle = 3;
    int total = 8;
    long totalRequests = 1000L;
    long failedRequests = 5L;

    // When
    FaceRecognitionHealthCheck.ConnectionPoolMetrics metrics =
        new FaceRecognitionHealthCheck.ConnectionPoolMetrics(
            active, idle, total, totalRequests, failedRequests);

    // Then
    assertEquals(active, metrics.getActiveConnections());
    assertEquals(idle, metrics.getIdleConnections());
    assertEquals(total, metrics.getTotalConnections());
    assertEquals(totalRequests, metrics.getTotalRequests());
    assertEquals(failedRequests, metrics.getFailedRequests());
  }

  @Test
  void shouldBuildMetricsSummary() {
    // When
    FaceRecognitionMetrics.MetricsSummary summary =
        FaceRecognitionMetrics.MetricsSummary.builder()
            .enrollSuccessTotal(100L)
            .enrollFailureTotal(5L)
            .verifySuccessTotal(500L)
            .verifyFailureTotal(10L)
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
    assertEquals(100L, summary.getEnrollSuccessTotal());
    assertEquals(5L, summary.getEnrollFailureTotal());
    assertEquals(500L, summary.getVerifySuccessTotal());
    assertEquals(10L, summary.getVerifyFailureTotal());
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
  void shouldHandleInvalidEnumValues() {
    // Test invalid status
    assertThrows(IllegalArgumentException.class, () -> DeletionRequestStatus.fromValue("invalid"));

    // Test invalid priority
    assertThrows(
        IllegalArgumentException.class, () -> DeletionRequestPriority.fromValue("invalid"));
  }
}
