package com.bioid.keycloak.metrics;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Face Recognition Metrics Collection Service.
 *
 * <p>Provides comprehensive metrics for enrollment, verification, and administrative operations to
 * support monitoring, alerting, and performance analysis.
 *
 * <p>Note: Metrics functionality is currently disabled to simplify CDI configuration. In
 * production, this should be integrated with Keycloak's metrics system.
 */
@ApplicationScoped
public class FaceRecognitionMetrics {

  // Default constructor for CDI
  public FaceRecognitionMetrics() {
    // Metrics disabled for now to avoid CDI complexity
  }

  // Constructor for testing
  public FaceRecognitionMetrics(Object metricRegistry) {
    // Metrics disabled for now to avoid CDI complexity
  }

  // Enrollment metrics - all no-op for now
  public void recordEnrollmentSuccess(String userId, String templateType, long durationMs) {
    // No-op
  }

  public void recordEnrollmentFailure(String userId, String errorCode, long durationMs) {
    // No-op
  }

  public void recordEnrollmentRetry(String userId, int retryCount) {
    // No-op
  }

  // Verification metrics - all no-op for now
  public void recordVerificationSuccess(
      String userId, double score, Double livenessScore, long durationMs) {
    // No-op
  }

  public void recordVerificationFailure(String userId, String errorCode, long durationMs) {
    // No-op
  }

  public void recordVerificationRetry(String userId, int retryCount) {
    // No-op
  }

  // Template management metrics - all no-op for now
  public void recordTemplateCreation(String userId, String templateType) {
    // No-op
  }

  public void recordTemplateDeletion(String userId, String reason) {
    // No-op
  }

  public void recordTemplateExpiration(String userId) {
    // No-op
  }

  // System health metrics - all no-op for now
  public void recordHealthCheckSuccess(long responseTimeMs) {
    // No-op
  }

  public void recordHealthCheckFailure(String errorCode, long responseTimeMs) {
    // No-op
  }

  public void recordGrpcConnectionStatus(boolean connected) {
    // No-op
  }

  // Administrative metrics - all no-op for now
  public void recordConfigurationUpdate(String section, String adminUserId) {
    // No-op
  }

  public void recordDeletionRequest(String userId, String priority) {
    // No-op
  }

  public void recordDeletionRequestProcessed(String userId, boolean approved) {
    // No-op
  }

  // Performance metrics - all no-op for now
  public void recordCacheHit(String cacheType) {
    // No-op
  }

  public void recordCacheMiss(String cacheType) {
    // No-op
  }

  public void recordDatabaseQuery(String queryType, long durationMs) {
    // No-op
  }

  // Liveness detection metrics - all no-op for now
  public void recordLivenessCheck(
      String userId, String livenessType, double score, boolean passed) {
    // No-op
  }

  public void recordLivenessFailure(String userId, String errorCode) {
    // No-op
  }

  // Error tracking - all no-op for now
  public void recordError(String errorType, String errorCode, String context) {
    // No-op
  }

  public void recordWarning(String warningType, String context) {
    // No-op
  }

  // Health check metrics - all no-op for now
  public void incrementHealthCheck(String checkType) {
    // No-op
  }

  public void incrementHealthCheckSuccess(String checkType) {
    // No-op
  }

  public void incrementHealthCheckFailure(String checkType, String errorCode) {
    // No-op
  }

  // Additional methods expected by tests - all no-op for now
  public void incrementEnrollSuccess() {
    // No-op
  }

  public void incrementEnrollFailure(String reason) {
    // No-op
  }

  public Object startEnrollTimer() {
    // No-op - return dummy object
    return new Object();
  }

  public void incrementVerifySuccess() {
    // No-op
  }

  public void incrementVerifyFailure(String reason) {
    // No-op
  }

  public void recordBioIdLatency(String operation, long latencyMs) {
    // No-op
  }

  public void updateConnectionPoolMetrics(int active, int idle) {
    // No-op
  }

  public void incrementDeletionRequestCreated(String priority) {
    // No-op
  }

  public void incrementDeletionRequestApproved() {
    // No-op
  }

  public void incrementDeletionRequestDeclined() {
    // No-op
  }

  public void incrementLivenessCheck(String type) {
    // No-op
  }

  public void incrementLivenessSuccess(String type) {
    // No-op
  }

  public void incrementLivenessFailure(String type, String reason) {
    // No-op
  }

  public void updateActiveTemplateCount(long count) {
    // No-op
  }

  public void updatePendingDeletionRequests(long count) {
    // No-op
  }

  public void incrementTemplateDelete(String result) {
    // No-op
  }

  public void incrementTemplateUpgrade(String result) {
    // No-op
  }

  public void incrementBioIdRequest(String operation, String endpoint) {
    // No-op
  }

  public void incrementBioIdError(String operation, String errorType) {
    // No-op
  }

  // Metrics summary - stub for now
  public MetricsSummary getMetricsSummary() {
    return new MetricsSummary(); // Return empty summary for now
  }

  /** Simple metrics summary class for development. */
  public static class MetricsSummary {
    public long totalEnrollments = 0;
    public long successfulEnrollments = 0;
    public long failedEnrollments = 0;
    public long totalVerifications = 0;
    public long successfulVerifications = 0;
    public long failedVerifications = 0;
    public long totalHealthChecks = 0;
    public long successfulHealthChecks = 0;
    public long failedHealthChecks = 0;
    public String status = "OK";
    public String lastUpdated = java.time.Instant.now().toString();

    // Additional fields expected by tests
    private long enrollSuccessTotal = 0;
    private long enrollFailureTotal = 0;
    private long verifySuccessTotal = 0;
    private long verifyFailureTotal = 0;
    private long bioIdRequestTotal = 0;
    private long bioIdErrorTotal = 0;
    private long templateDeleteTotal = 0;
    private long deletionRequestCreatedTotal = 0;
    private long deletionRequestApprovedTotal = 0;
    private long deletionRequestDeclinedTotal = 0;
    private long livenessCheckTotal = 0;
    private long healthCheckTotal = 0;
    private long healthCheckSuccessTotal = 0;

    // Default constructor
    public MetricsSummary() {}

    // Constructor for builder
    private MetricsSummary(Builder builder) {
      this.enrollSuccessTotal = builder.enrollSuccessTotal;
      this.enrollFailureTotal = builder.enrollFailureTotal;
      this.verifySuccessTotal = builder.verifySuccessTotal;
      this.verifyFailureTotal = builder.verifyFailureTotal;
      this.bioIdRequestTotal = builder.bioIdRequestTotal;
      this.bioIdErrorTotal = builder.bioIdErrorTotal;
      this.templateDeleteTotal = builder.templateDeleteTotal;
      this.deletionRequestCreatedTotal = builder.deletionRequestCreatedTotal;
      this.deletionRequestApprovedTotal = builder.deletionRequestApprovedTotal;
      this.deletionRequestDeclinedTotal = builder.deletionRequestDeclinedTotal;
      this.livenessCheckTotal = builder.livenessCheckTotal;
      this.healthCheckTotal = builder.healthCheckTotal;
      this.healthCheckSuccessTotal = builder.healthCheckSuccessTotal;
    }

    // Getter methods expected by tests
    public long getEnrollSuccessTotal() {
      return enrollSuccessTotal;
    }

    public long getEnrollFailureTotal() {
      return enrollFailureTotal;
    }

    public long getVerifySuccessTotal() {
      return verifySuccessTotal;
    }

    public long getVerifyFailureTotal() {
      return verifyFailureTotal;
    }

    public long getBioIdRequestTotal() {
      return bioIdRequestTotal;
    }

    public long getBioIdErrorTotal() {
      return bioIdErrorTotal;
    }

    public long getTemplateDeleteTotal() {
      return templateDeleteTotal;
    }

    public long getDeletionRequestCreatedTotal() {
      return deletionRequestCreatedTotal;
    }

    public long getDeletionRequestApprovedTotal() {
      return deletionRequestApprovedTotal;
    }

    public long getDeletionRequestDeclinedTotal() {
      return deletionRequestDeclinedTotal;
    }

    public long getLivenessCheckTotal() {
      return livenessCheckTotal;
    }

    public long getHealthCheckTotal() {
      return healthCheckTotal;
    }

    public long getHealthCheckSuccessTotal() {
      return healthCheckSuccessTotal;
    }

    // Builder pattern
    public static Builder builder() {
      return new Builder();
    }

    public static class Builder {
      private long enrollSuccessTotal = 0;
      private long enrollFailureTotal = 0;
      private long verifySuccessTotal = 0;
      private long verifyFailureTotal = 0;
      private long bioIdRequestTotal = 0;
      private long bioIdErrorTotal = 0;
      private long templateDeleteTotal = 0;
      private long deletionRequestCreatedTotal = 0;
      private long deletionRequestApprovedTotal = 0;
      private long deletionRequestDeclinedTotal = 0;
      private long livenessCheckTotal = 0;
      private long healthCheckTotal = 0;
      private long healthCheckSuccessTotal = 0;

      public Builder enrollSuccessTotal(long enrollSuccessTotal) {
        this.enrollSuccessTotal = enrollSuccessTotal;
        return this;
      }

      public Builder enrollFailureTotal(long enrollFailureTotal) {
        this.enrollFailureTotal = enrollFailureTotal;
        return this;
      }

      public Builder verifySuccessTotal(long verifySuccessTotal) {
        this.verifySuccessTotal = verifySuccessTotal;
        return this;
      }

      public Builder verifyFailureTotal(long verifyFailureTotal) {
        this.verifyFailureTotal = verifyFailureTotal;
        return this;
      }

      public Builder bioIdRequestTotal(long bioIdRequestTotal) {
        this.bioIdRequestTotal = bioIdRequestTotal;
        return this;
      }

      public Builder bioIdErrorTotal(long bioIdErrorTotal) {
        this.bioIdErrorTotal = bioIdErrorTotal;
        return this;
      }

      public Builder templateDeleteTotal(long templateDeleteTotal) {
        this.templateDeleteTotal = templateDeleteTotal;
        return this;
      }

      public Builder deletionRequestCreatedTotal(long deletionRequestCreatedTotal) {
        this.deletionRequestCreatedTotal = deletionRequestCreatedTotal;
        return this;
      }

      public Builder deletionRequestApprovedTotal(long deletionRequestApprovedTotal) {
        this.deletionRequestApprovedTotal = deletionRequestApprovedTotal;
        return this;
      }

      public Builder deletionRequestDeclinedTotal(long deletionRequestDeclinedTotal) {
        this.deletionRequestDeclinedTotal = deletionRequestDeclinedTotal;
        return this;
      }

      public Builder livenessCheckTotal(long livenessCheckTotal) {
        this.livenessCheckTotal = livenessCheckTotal;
        return this;
      }

      public Builder healthCheckTotal(long healthCheckTotal) {
        this.healthCheckTotal = healthCheckTotal;
        return this;
      }

      public Builder healthCheckSuccessTotal(long healthCheckSuccessTotal) {
        this.healthCheckSuccessTotal = healthCheckSuccessTotal;
        return this;
      }

      public MetricsSummary build() {
        return new MetricsSummary(this);
      }
    }
  }
}
