package com.bioid.keycloak.health;

import com.bioid.keycloak.client.BioIdClient;
import com.bioid.keycloak.client.config.BioIdConfiguration;
import com.bioid.keycloak.metrics.FaceRecognitionMetrics;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.eclipse.microprofile.health.HealthCheckResponse;

/**
 * Health check for Face Recognition service components.
 *
 * <p>Monitors BioID service connectivity, gRPC channel health, and overall system readiness. Note:
 * This class is NOT a CDI bean - it's created manually to avoid dependency issues.
 */
public class FaceRecognitionHealthCheck {

  private final BioIdClient bioIdClient;
  private final BioIdConfiguration config;
  private final FaceRecognitionMetrics metrics;

  private static final String HEALTH_CHECK_NAME = "face-recognition";
  private static final Duration HEALTH_CHECK_TIMEOUT = Duration.ofSeconds(5);

  // Constructor - no longer a CDI bean
  public FaceRecognitionHealthCheck() {
    this.config = BioIdConfiguration.getInstance();
    this.bioIdClient = null; // No change here for the default constructor
    this.metrics = new FaceRecognitionMetrics();
  }

  // Change the type in the constructor signature
  public FaceRecognitionHealthCheck(
      BioIdClient bioIdClient, BioIdConfiguration config, FaceRecognitionMetrics metrics) {
    this.bioIdClient = bioIdClient;
    this.config = config;
    this.metrics = metrics;
  }

  /** MicroProfile HealthCheck interface method for compatibility with tests. */
  public HealthCheckResponse call() {
    HealthCheckResult result = performHealthCheck();

    // Convert our result to MicroProfile HealthCheckResponse
    if (result.isHealthy()) {
      return HealthCheckResponse.named(result.getName()).up().build();
    } else {
      return HealthCheckResponse.named(result.getName()).down().build();
    }
  }

  /**
   * Perform a health check and return the result. This replaces the MicroProfile HealthCheck
   * interface method.
   */
  public HealthCheckResult performHealthCheck() {

    try {
      // Record health check attempt
      metrics.incrementHealthCheck("bioid-service");

      Instant startTime = Instant.now();

      // Check BioID service connectivity
      boolean bioIdHealthy = checkBioIdService();

      Duration checkDuration = Duration.between(startTime, Instant.now());

      if (bioIdHealthy) {
        metrics.incrementHealthCheckSuccess("bioid-service");

        return new HealthCheckResult(
            HEALTH_CHECK_NAME,
            true,
            "UP",
            checkDuration.toMillis(),
            getCurrentEndpoint(),
            getConnectionPoolStatus(),
            null);
      } else {
        metrics.incrementHealthCheckFailure("bioid-service", "connectivity_failed");

        return new HealthCheckResult(
            HEALTH_CHECK_NAME,
            false,
            "DOWN",
            checkDuration.toMillis(),
            getCurrentEndpoint(),
            null,
            "BioID service connectivity check failed");
      }

    } catch (Exception e) {
      metrics.incrementHealthCheckFailure("bioid-service", "check_exception");

      return new HealthCheckResult(
          HEALTH_CHECK_NAME, false, "DOWN", -1, getCurrentEndpoint(), null, e.getMessage());
    }
  }

  /**
   * Check BioID service connectivity with timeout.
   *
   * @return true if service is reachable and responsive
   */
  private boolean checkBioIdService() {
    try {
      // If bioIdClient is not available, just check configuration
      if (bioIdClient == null) {
        return isConfigurationValid();
      }

      // Use CompletableFuture to implement timeout
      CompletableFuture<Boolean> healthCheckFuture =
          CompletableFuture.supplyAsync(
              () -> {
                try {
                  // Perform a lightweight health check operation
                  // This could be a ping or a simple service status check
                  return bioIdClient.isHealthy();
                } catch (Exception e) {
                  return false;
                }
              });

      // Wait for the result with timeout
      return healthCheckFuture.get(HEALTH_CHECK_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);

    } catch (Exception e) {
      return false;
    }
  }

  /**
   * Get current BioID endpoint information.
   *
   * @return endpoint information string
   */
  private String getCurrentEndpoint() {
    try {
      return config.getEndpoint();
    } catch (Exception e) {
      return "unknown";
    }
  }

  /**
   * Get connection pool status information.
   *
   * @return connection pool status string
   */
  private String getConnectionPoolStatus() {
    try {
      if (bioIdClient != null) {
        // Get real connection pool metrics from the client
        Object poolMetrics = bioIdClient.getConnectionPoolMetrics();
        if (poolMetrics != null) {
          return "healthy";
        }
      }
      return "unknown";
    } catch (Exception e) {
      return "error: " + e.getMessage();
    }
  }

  /**
   * Detailed health check that includes additional system information. This method can be called
   * programmatically for more detailed diagnostics.
   *
   * @return detailed health check result
   */
  public DetailedHealthCheckResult getDetailedHealth() {
    DetailedHealthCheckResult.Builder builder = DetailedHealthCheckResult.builder();

    try {
      Instant startTime = Instant.now();

      // Check BioID service
      boolean bioIdHealthy = checkBioIdService();
      Duration bioIdCheckDuration = Duration.between(startTime, Instant.now());

      builder
          .bioIdServiceHealthy(bioIdHealthy)
          .bioIdCheckDuration(bioIdCheckDuration)
          .currentEndpoint(getCurrentEndpoint());

      // Check configuration
      boolean configValid = isConfigurationValid();
      builder.configurationValid(configValid);

      // Get connection pool metrics
      if (bioIdClient != null) {
        try {
          // Get real connection pool metrics from the client
          Object clientMetrics = bioIdClient.getConnectionPoolMetrics();
          if (clientMetrics != null) {
            // Use reflection to extract metrics from the client's ConnectionPoolMetrics object
            Class<?> metricsClass = clientMetrics.getClass();
            int active = (int) metricsClass.getMethod("getActiveConnections").invoke(clientMetrics);
            int idle = (int) metricsClass.getMethod("getIdleConnections").invoke(clientMetrics);
            int total = (int) metricsClass.getMethod("getTotalConnections").invoke(clientMetrics);
            long totalReqs = (long) metricsClass.getMethod("getTotalRequests").invoke(clientMetrics);
            long failedReqs = (long) metricsClass.getMethod("getFailedRequests").invoke(clientMetrics);
            
            ConnectionPoolMetrics poolMetrics = new ConnectionPoolMetrics(
                active, idle, total, totalReqs, failedReqs);
            builder.connectionPoolMetrics(poolMetrics);
          }
        } catch (Exception e) {
          builder.connectionPoolError(e.getMessage());
        }
      }

      // Overall health status
      boolean overallHealthy = bioIdHealthy && configValid;
      builder.overallHealthy(overallHealthy);

      return builder.build();

    } catch (Exception e) {
      return builder.overallHealthy(false).error(e.getMessage()).build();
    }
  }

  /**
   * Check if the current configuration is valid.
   *
   * @return true if configuration is valid
   */
  private boolean isConfigurationValid() {
    try {
      return config != null
          && config.getEndpoint() != null
          && !config.getEndpoint().isEmpty()
          && config.getClientId() != null
          && !config.getClientId().isEmpty();
    } catch (Exception e) {
      return false;
    }
  }

  /** Connection pool metrics data class. */
  public static class ConnectionPoolMetrics {
    private final int activeConnections;
    private final int idleConnections;
    private final int totalConnections;
    private final long totalRequests;
    private final long failedRequests;

    public ConnectionPoolMetrics(
        int activeConnections,
        int idleConnections,
        int totalConnections,
        long totalRequests,
        long failedRequests) {
      this.activeConnections = activeConnections;
      this.idleConnections = idleConnections;
      this.totalConnections = totalConnections;
      this.totalRequests = totalRequests;
      this.failedRequests = failedRequests;
    }

    // Getters
    public int getActiveConnections() {
      return activeConnections;
    }

    public int getIdleConnections() {
      return idleConnections;
    }

    public int getTotalConnections() {
      return totalConnections;
    }

    public long getTotalRequests() {
      return totalRequests;
    }

    public long getFailedRequests() {
      return failedRequests;
    }
  }

  /** Detailed health check result. */
  public static class DetailedHealthCheckResult {
    private final boolean overallHealthy;
    private final boolean bioIdServiceHealthy;
    private final Duration bioIdCheckDuration;
    private final String currentEndpoint;
    private final boolean configurationValid;
    private final ConnectionPoolMetrics connectionPoolMetrics;
    private final String connectionPoolError;
    private final String error;
    private final Instant checkTime;

    private DetailedHealthCheckResult(Builder builder) {
      this.overallHealthy = builder.overallHealthy;
      this.bioIdServiceHealthy = builder.bioIdServiceHealthy;
      this.bioIdCheckDuration = builder.bioIdCheckDuration;
      this.currentEndpoint = builder.currentEndpoint;
      this.configurationValid = builder.configurationValid;
      this.connectionPoolMetrics = builder.connectionPoolMetrics;
      this.connectionPoolError = builder.connectionPoolError;
      this.error = builder.error;
      this.checkTime = Instant.now();
    }

    public static Builder builder() {
      return new Builder();
    }

    // Getters
    public boolean isOverallHealthy() {
      return overallHealthy;
    }

    public boolean isBioIdServiceHealthy() {
      return bioIdServiceHealthy;
    }

    public Duration getBioIdCheckDuration() {
      return bioIdCheckDuration;
    }

    public String getCurrentEndpoint() {
      return currentEndpoint;
    }

    public boolean isConfigurationValid() {
      return configurationValid;
    }

    public ConnectionPoolMetrics getConnectionPoolMetrics() {
      return connectionPoolMetrics;
    }

    public String getConnectionPoolError() {
      return connectionPoolError;
    }

    public String getError() {
      return error;
    }

    public Instant getCheckTime() {
      return checkTime;
    }

    public static class Builder {
      private boolean overallHealthy;
      private boolean bioIdServiceHealthy;
      private Duration bioIdCheckDuration;
      private String currentEndpoint;
      private boolean configurationValid;
      private ConnectionPoolMetrics connectionPoolMetrics;
      private String connectionPoolError;
      private String error;

      public Builder overallHealthy(boolean overallHealthy) {
        this.overallHealthy = overallHealthy;
        return this;
      }

      public Builder bioIdServiceHealthy(boolean bioIdServiceHealthy) {
        this.bioIdServiceHealthy = bioIdServiceHealthy;
        return this;
      }

      public Builder bioIdCheckDuration(Duration bioIdCheckDuration) {
        this.bioIdCheckDuration = bioIdCheckDuration;
        return this;
      }

      public Builder currentEndpoint(String currentEndpoint) {
        this.currentEndpoint = currentEndpoint;
        return this;
      }

      public Builder configurationValid(boolean configurationValid) {
        this.configurationValid = configurationValid;
        return this;
      }

      public Builder connectionPoolMetrics(ConnectionPoolMetrics connectionPoolMetrics) {
        this.connectionPoolMetrics = connectionPoolMetrics;
        return this;
      }

      public Builder connectionPoolError(String connectionPoolError) {
        this.connectionPoolError = connectionPoolError;
        return this;
      }

      public Builder error(String error) {
        this.error = error;
        return this;
      }

      public DetailedHealthCheckResult build() {
        return new DetailedHealthCheckResult(this);
      }
    }
  }

  /** Simple health check result class to replace MicroProfile HealthCheckResponse. */
  public static class HealthCheckResult {
    private final String name;
    private final boolean healthy;
    private final String status;
    private final long checkDurationMs;
    private final String endpoint;
    private final String connectionPoolStatus;
    private final String error;
    private final Instant checkTime;

    public HealthCheckResult(
        String name,
        boolean healthy,
        String status,
        long checkDurationMs,
        String endpoint,
        String connectionPoolStatus,
        String error) {
      this.name = name;
      this.healthy = healthy;
      this.status = status;
      this.checkDurationMs = checkDurationMs;
      this.endpoint = endpoint;
      this.connectionPoolStatus = connectionPoolStatus;
      this.error = error;
      this.checkTime = Instant.now();
    }

    // Getters
    public String getName() {
      return name;
    }

    public boolean isHealthy() {
      return healthy;
    }

    public String getStatus() {
      return status;
    }

    public long getCheckDurationMs() {
      return checkDurationMs;
    }

    public String getEndpoint() {
      return endpoint;
    }

    public String getConnectionPoolStatus() {
      return connectionPoolStatus;
    }

    public String getError() {
      return error;
    }

    public Instant getCheckTime() {
      return checkTime;
    }
  }
}
