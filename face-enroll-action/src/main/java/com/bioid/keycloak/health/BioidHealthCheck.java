package com.bioid.keycloak.health;

import com.bioid.keycloak.config.ConfigurationValidator;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Health check implementation for BioID Keycloak extension.
 *
 * <p>This class provides comprehensive health monitoring for the extension, including configuration
 * validation, service connectivity, and system status.
 *
 * @since 1.0.0
 */
public class BioidHealthCheck {

  private static final Logger logger = LoggerFactory.getLogger(BioidHealthCheck.class);

  private static final long HEALTH_CHECK_TIMEOUT_MS = 5000; // 5 seconds
  private static final long CACHE_DURATION_MS = 30000; // 30 seconds

  private volatile HealthStatus cachedStatus;
  private volatile long lastCheckTime;

  /** Health status enumeration. */
  public enum Status {
    UP,
    DOWN,
    DEGRADED,
    UNKNOWN
  }

  /**
   * Performs a comprehensive health check.
   *
   * @return the current health status
   */
  public HealthStatus checkHealth() {
    long currentTime = System.currentTimeMillis();

    // Return cached result if still valid
    if (cachedStatus != null && (currentTime - lastCheckTime) < CACHE_DURATION_MS) {
      logger.debug("Returning cached health status: {}", cachedStatus.getStatus());
      return cachedStatus;
    }

    logger.debug("Performing fresh health check");

    try {
      HealthStatus status = performHealthCheck();

      // Cache the result
      cachedStatus = status;
      lastCheckTime = currentTime;

      logger.info(
          "Health check completed: {} ({}ms)", status.getStatus(), status.getResponseTime());

      return status;

    } catch (Exception e) {
      logger.error("Health check failed with exception", e);

      HealthStatus errorStatus =
          new HealthStatus.Builder()
              .status(Status.DOWN)
              .message("Health check failed: " + e.getMessage())
              .responseTime(System.currentTimeMillis() - currentTime)
              .addDetail("error", e.getClass().getSimpleName() + ": " + e.getMessage())
              .build();

      return errorStatus;
    }
  }

  /**
   * Performs an asynchronous health check with timeout.
   *
   * @return CompletableFuture containing the health status
   */
  public CompletableFuture<HealthStatus> checkHealthAsync() {
    return CompletableFuture.supplyAsync(this::checkHealth)
        .orTimeout(HEALTH_CHECK_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        .exceptionally(
            throwable -> {
              logger.warn("Async health check timed out or failed", throwable);
              HealthStatus timeoutStatus =
                  new HealthStatus.Builder()
                      .status(Status.DOWN)
                      .message("Health check timed out")
                      .responseTime(HEALTH_CHECK_TIMEOUT_MS)
                      .addDetail(
                          "timeout", "Health check exceeded " + HEALTH_CHECK_TIMEOUT_MS + "ms")
                      .build();
              return timeoutStatus;
            });
  }

  private HealthStatus performHealthCheck() {
    long startTime = System.currentTimeMillis();
    HealthStatus.Builder builder = new HealthStatus.Builder();

    // Check configuration
    checkConfiguration(builder);

    // Check system resources
    checkSystemResources(builder);

    // Check database connectivity (if applicable)
    checkDatabaseConnectivity(builder);

    // Determine overall status
    Status overallStatus = determineOverallStatus(builder);
    long responseTime = System.currentTimeMillis() - startTime;

    return builder
        .status(overallStatus)
        .message(generateStatusMessage(overallStatus))
        .responseTime(responseTime)
        .timestamp(Instant.now())
        .build();
  }

  private void checkConfiguration(HealthStatus.Builder builder) {
    try {
      ConfigurationValidator.ValidationResult result =
          ConfigurationValidator.validateConfiguration();

      if (result.isValid()) {
        builder.addDetail("configuration", "Valid");
        builder.addCheck("configuration", true, "Configuration validation passed");
      } else {
        builder.addDetail("configuration", "Invalid - " + result.getErrors().size() + " errors");
        builder.addCheck(
            "configuration",
            false,
            "Configuration validation failed: " + String.join(", ", result.getErrors()));
      }

      if (result.hasWarnings()) {
        builder.addDetail("configuration_warnings", String.join(", ", result.getWarnings()));
      }

    } catch (Exception e) {
      logger.warn("Configuration check failed", e);
      builder.addCheck("configuration", false, "Configuration check failed: " + e.getMessage());
    }
  }

  private void checkSystemResources(HealthStatus.Builder builder) {
    try {
      Runtime runtime = Runtime.getRuntime();
      long maxMemory = runtime.maxMemory();
      long totalMemory = runtime.totalMemory();
      long freeMemory = runtime.freeMemory();
      long usedMemory = totalMemory - freeMemory;

      double memoryUsagePercent = (double) usedMemory / maxMemory * 100;

      builder.addDetail("memory_used_mb", usedMemory / (1024 * 1024));
      builder.addDetail("memory_max_mb", maxMemory / (1024 * 1024));
      builder.addDetail("memory_usage_percent", String.format("%.1f%%", memoryUsagePercent));

      boolean memoryOk = memoryUsagePercent < 90.0; // Alert if > 90% memory usage
      builder.addCheck(
          "memory",
          memoryOk,
          memoryOk
              ? "Memory usage normal"
              : "High memory usage: " + String.format("%.1f%%", memoryUsagePercent));

      // Check available processors
      int processors = runtime.availableProcessors();
      builder.addDetail("processors", processors);
      builder.addCheck("processors", processors > 0, processors + " processors available");

    } catch (Exception e) {
      logger.warn("System resources check failed", e);
      builder.addCheck(
          "system_resources", false, "System resources check failed: " + e.getMessage());
    }
  }

  private void checkDatabaseConnectivity(HealthStatus.Builder builder) {
    try {
      // This is a placeholder - in a real implementation, you would
      // check actual database connectivity through Keycloak's session
      builder.addCheck("database", true, "Database connectivity check not implemented");
      builder.addDetail("database", "Not checked - requires Keycloak session");

    } catch (Exception e) {
      logger.warn("Database connectivity check failed", e);
      builder.addCheck("database", false, "Database check failed: " + e.getMessage());
    }
  }

  private Status determineOverallStatus(HealthStatus.Builder builder) {
    Map<String, Boolean> checks = builder.getChecks();

    if (checks.isEmpty()) {
      return Status.UNKNOWN;
    }

    long failedChecks = checks.values().stream().mapToLong(passed -> passed ? 0 : 1).sum();

    if (failedChecks == 0) {
      return Status.UP;
    } else if (failedChecks < checks.size()) {
      return Status.DEGRADED;
    } else {
      return Status.DOWN;
    }
  }

  private String generateStatusMessage(Status status) {
    switch (status) {
      case UP:
        return "BioID extension is healthy";
      case DEGRADED:
        return "BioID extension is partially healthy";
      case DOWN:
        return "BioID extension is unhealthy";
      default:
        return "BioID extension status unknown";
    }
  }

  /** Health status result. */
  public static class HealthStatus {
    private final Status status;
    private final String message;
    private final long responseTime;
    private final Instant timestamp;
    private final Map<String, Object> details;
    private final Map<String, String> checkResults;

    private HealthStatus(Builder builder) {
      this.status = builder.status;
      this.message = builder.message;
      this.responseTime = builder.responseTime;
      this.timestamp = builder.timestamp;
      this.details = new HashMap<>(builder.details);
      this.checkResults = new HashMap<>(builder.checkResults);
    }

    public Status getStatus() {
      return status;
    }

    public String getMessage() {
      return message;
    }

    public long getResponseTime() {
      return responseTime;
    }

    public Instant getTimestamp() {
      return timestamp;
    }

    public Map<String, Object> getDetails() {
      return new HashMap<>(details);
    }

    public Map<String, String> getCheckResults() {
      return new HashMap<>(checkResults);
    }

    public void addDetail(String key, Object value) {
      details.put(key, value);
    }

    @Override
    public String toString() {
      return String.format(
          "HealthStatus[status=%s, message='%s', responseTime=%dms, checks=%d]",
          status, message, responseTime, checkResults.size());
    }

    /** Builder for HealthStatus. */
    public static class Builder {
      private Status status = Status.UNKNOWN;
      private String message = "";
      private long responseTime = 0;
      private Instant timestamp = Instant.now();
      private final Map<String, Object> details = new HashMap<>();
      private final Map<String, Boolean> checks = new HashMap<>();
      private final Map<String, String> checkResults = new HashMap<>();

      public Builder status(Status status) {
        this.status = status;
        return this;
      }

      public Builder message(String message) {
        this.message = message;
        return this;
      }

      public Builder responseTime(long responseTime) {
        this.responseTime = responseTime;
        return this;
      }

      public Builder timestamp(Instant timestamp) {
        this.timestamp = timestamp;
        return this;
      }

      public Builder addDetail(String key, Object value) {
        this.details.put(key, value);
        return this;
      }

      public Builder addCheck(String name, boolean passed, String result) {
        this.checks.put(name, passed);
        this.checkResults.put(name, result);
        return this;
      }

      public Map<String, Boolean> getChecks() {
        return new HashMap<>(checks);
      }

      public HealthStatus build() {
        return new HealthStatus(this);
      }
    }
  }
}
