package com.bioid.keycloak.client.health;

import java.time.Instant;
import java.util.Map;

/**
 * Represents the health status of the BioID service including connectivity,
 * performance metrics, and service availability information.
 *
 * <p>This class provides comprehensive health information that can be used
 * for monitoring, alerting, and diagnostic purposes.
 */
public class ServiceHealthStatus {

  private final HealthState state;
  private final String endpoint;
  private final String region;
  private final long responseTimeMs;
  private final double errorRate;
  private final Instant lastHealthCheck;
  private final String version;
  private final Map<String, Object> additionalMetrics;
  private final String statusMessage;

  public ServiceHealthStatus(HealthState state, String endpoint, String region,
      long responseTimeMs, double errorRate, Instant lastHealthCheck,
      String version, Map<String, Object> additionalMetrics, String statusMessage) {
    this.state = state;
    this.endpoint = endpoint;
    this.region = region;
    this.responseTimeMs = responseTimeMs;
    this.errorRate = errorRate;
    this.lastHealthCheck = lastHealthCheck;
    this.version = version;
    this.additionalMetrics = additionalMetrics;
    this.statusMessage = statusMessage;
  }

  /**
   * Gets the overall health state of the service.
   *
   * @return the current health state
   */
  public HealthState getState() {
    return state;
  }

  /**
   * Gets the endpoint URL being monitored.
   *
   * @return the service endpoint
   */
  public String getEndpoint() {
    return endpoint;
  }

  /**
   * Gets the region identifier for the service endpoint.
   *
   * @return the region identifier
   */
  public String getRegion() {
    return region;
  }

  /**
   * Gets the last measured response time in milliseconds.
   *
   * @return response time in milliseconds
   */
  public long getResponseTimeMs() {
    return responseTimeMs;
  }

  /**
   * Gets the current error rate as a percentage (0.0 to 1.0).
   *
   * @return error rate percentage
   */
  public double getErrorRate() {
    return errorRate;
  }

  /**
   * Gets the timestamp of the last health check.
   *
   * @return timestamp of last health check
   */
  public Instant getLastHealthCheck() {
    return lastHealthCheck;
  }

  /**
   * Gets the service version if available.
   *
   * @return service version string, or null if not available
   */
  public String getVersion() {
    return version;
  }

  /**
   * Gets additional metrics and diagnostic information.
   *
   * @return map of additional metrics
   */
  public Map<String, Object> getAdditionalMetrics() {
    return additionalMetrics;
  }

  /**
   * Gets a human-readable status message describing the current state.
   *
   * @return status message
   */
  public String getStatusMessage() {
    return statusMessage;
  }

  /**
   * Checks if the service is considered healthy.
   *
   * @return true if the service is healthy, false otherwise
   */
  public boolean isHealthy() {
    return state == HealthState.HEALTHY;
  }

  /**
   * Checks if the service is degraded but still functional.
   *
   * @return true if the service is degraded, false otherwise
   */
  public boolean isDegraded() {
    return state == HealthState.DEGRADED;
  }

  /**
   * Checks if the service is unhealthy or unavailable.
   *
   * @return true if the service is unhealthy, false otherwise
   */
  public boolean isUnhealthy() {
    return state == HealthState.UNHEALTHY;
  }

  @Override
  public String toString() {
    return String.format(
        "ServiceHealthStatus{state=%s, endpoint='%s', region='%s', responseTime=%dms, " +
        "errorRate=%.2f%%, lastCheck=%s, message='%s'}",
        state, endpoint, region, responseTimeMs, errorRate * 100, lastHealthCheck, statusMessage);
  }

  /**
   * Enumeration of possible health states for the BioID service.
   */
  public enum HealthState {
    /**
     * Service is fully operational with normal response times and low error rates.
     */
    HEALTHY,

    /**
     * Service is operational but experiencing degraded performance or elevated error rates.
     * Some functionality may be impacted but core operations are still available.
     */
    DEGRADED,

    /**
     * Service is not operational or experiencing critical issues.
     * Most or all functionality is unavailable.
     */
    UNHEALTHY,

    /**
     * Health status cannot be determined due to connectivity issues or other problems.
     */
    UNKNOWN
  }

  /**
   * Builder class for creating ServiceHealthStatus instances.
   */
  public static class Builder {
    private HealthState state;
    private String endpoint;
    private String region;
    private long responseTimeMs;
    private double errorRate;
    private Instant lastHealthCheck;
    private String version;
    private Map<String, Object> additionalMetrics;
    private String statusMessage;

    public Builder setState(HealthState state) {
      this.state = state;
      return this;
    }

    public Builder setEndpoint(String endpoint) {
      this.endpoint = endpoint;
      return this;
    }

    public Builder setRegion(String region) {
      this.region = region;
      return this;
    }

    public Builder setResponseTimeMs(long responseTimeMs) {
      this.responseTimeMs = responseTimeMs;
      return this;
    }

    public Builder setErrorRate(double errorRate) {
      this.errorRate = errorRate;
      return this;
    }

    public Builder setLastHealthCheck(Instant lastHealthCheck) {
      this.lastHealthCheck = lastHealthCheck;
      return this;
    }

    public Builder setVersion(String version) {
      this.version = version;
      return this;
    }

    public Builder setAdditionalMetrics(Map<String, Object> additionalMetrics) {
      this.additionalMetrics = additionalMetrics;
      return this;
    }

    public Builder setStatusMessage(String statusMessage) {
      this.statusMessage = statusMessage;
      return this;
    }

    public ServiceHealthStatus build() {
      return new ServiceHealthStatus(state, endpoint, region, responseTimeMs, errorRate,
          lastHealthCheck, version, additionalMetrics, statusMessage);
    }
  }
}