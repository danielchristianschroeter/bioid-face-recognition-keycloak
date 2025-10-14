package com.bioid.keycloak.client.metrics;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * Comprehensive connection metrics for monitoring BioID client performance,
 * error rates, and connection pool utilization.
 *
 * <p>This class provides detailed metrics that can be used for:
 * - Performance monitoring and alerting
 * - Capacity planning and scaling decisions
 * - Troubleshooting connectivity issues
 * - Service level agreement (SLA) reporting
 */
public class ConnectionMetrics {

  private final long totalRequests;
  private final long successfulRequests;
  private final long failedRequests;
  private final double errorRate;
  private final Duration averageResponseTime;
  private final Duration p95ResponseTime;
  private final Duration p99ResponseTime;
  private final int activeConnections;
  private final int idleConnections;
  private final int maxConnections;
  private final long totalBytesTransferred;
  private final Instant metricsCollectedAt;
  private final Duration collectionPeriod;
  private final Map<String, Long> errorsByType;
  private final Map<String, Long> requestsByEndpoint;

  public ConnectionMetrics(long totalRequests, long successfulRequests, long failedRequests,
      double errorRate, Duration averageResponseTime, Duration p95ResponseTime,
      Duration p99ResponseTime, int activeConnections, int idleConnections, int maxConnections,
      long totalBytesTransferred, Instant metricsCollectedAt, Duration collectionPeriod,
      Map<String, Long> errorsByType, Map<String, Long> requestsByEndpoint) {
    this.totalRequests = totalRequests;
    this.successfulRequests = successfulRequests;
    this.failedRequests = failedRequests;
    this.errorRate = errorRate;
    this.averageResponseTime = averageResponseTime;
    this.p95ResponseTime = p95ResponseTime;
    this.p99ResponseTime = p99ResponseTime;
    this.activeConnections = activeConnections;
    this.idleConnections = idleConnections;
    this.maxConnections = maxConnections;
    this.totalBytesTransferred = totalBytesTransferred;
    this.metricsCollectedAt = metricsCollectedAt;
    this.collectionPeriod = collectionPeriod;
    this.errorsByType = errorsByType;
    this.requestsByEndpoint = requestsByEndpoint;
  }

  /**
   * Gets the total number of requests made during the collection period.
   *
   * @return total request count
   */
  public long getTotalRequests() {
    return totalRequests;
  }

  /**
   * Gets the number of successful requests during the collection period.
   *
   * @return successful request count
   */
  public long getSuccessfulRequests() {
    return successfulRequests;
  }

  /**
   * Gets the number of failed requests during the collection period.
   *
   * @return failed request count
   */
  public long getFailedRequests() {
    return failedRequests;
  }

  /**
   * Gets the error rate as a percentage (0.0 to 1.0).
   *
   * @return error rate percentage
   */
  public double getErrorRate() {
    return errorRate;
  }

  /**
   * Gets the average response time for all requests.
   *
   * @return average response time
   */
  public Duration getAverageResponseTime() {
    return averageResponseTime;
  }

  /**
   * Gets the 95th percentile response time.
   *
   * @return 95th percentile response time
   */
  public Duration getP95ResponseTime() {
    return p95ResponseTime;
  }

  /**
   * Gets the 99th percentile response time.
   *
   * @return 99th percentile response time
   */
  public Duration getP99ResponseTime() {
    return p99ResponseTime;
  }

  /**
   * Gets the current number of active connections.
   *
   * @return active connection count
   */
  public int getActiveConnections() {
    return activeConnections;
  }

  /**
   * Gets the current number of idle connections.
   *
   * @return idle connection count
   */
  public int getIdleConnections() {
    return idleConnections;
  }

  /**
   * Gets the maximum number of connections allowed in the pool.
   *
   * @return maximum connection count
   */
  public int getMaxConnections() {
    return maxConnections;
  }

  /**
   * Gets the total number of bytes transferred (sent + received).
   *
   * @return total bytes transferred
   */
  public long getTotalBytesTransferred() {
    return totalBytesTransferred;
  }

  /**
   * Gets the timestamp when these metrics were collected.
   *
   * @return metrics collection timestamp
   */
  public Instant getMetricsCollectedAt() {
    return metricsCollectedAt;
  }

  /**
   * Gets the time period over which these metrics were collected.
   *
   * @return collection period duration
   */
  public Duration getCollectionPeriod() {
    return collectionPeriod;
  }

  /**
   * Gets a breakdown of errors by error type.
   *
   * @return map of error types to their occurrence counts
   */
  public Map<String, Long> getErrorsByType() {
    return errorsByType;
  }

  /**
   * Gets a breakdown of requests by endpoint.
   *
   * @return map of endpoints to their request counts
   */
  public Map<String, Long> getRequestsByEndpoint() {
    return requestsByEndpoint;
  }

  /**
   * Calculates the connection pool utilization rate.
   *
   * @return utilization rate as a percentage (0.0 to 1.0)
   */
  public double getConnectionUtilization() {
    return maxConnections > 0 ? (double) activeConnections / maxConnections : 0.0;
  }

  /**
   * Calculates the requests per second rate.
   *
   * @return requests per second
   */
  public double getRequestsPerSecond() {
    long periodSeconds = collectionPeriod.getSeconds();
    return periodSeconds > 0 ? (double) totalRequests / periodSeconds : 0.0;
  }

  /**
   * Calculates the success rate as a percentage.
   *
   * @return success rate (0.0 to 1.0)
   */
  public double getSuccessRate() {
    return totalRequests > 0 ? (double) successfulRequests / totalRequests : 0.0;
  }

  /**
   * Calculates the average bytes per request.
   *
   * @return average bytes per request
   */
  public double getAverageBytesPerRequest() {
    return totalRequests > 0 ? (double) totalBytesTransferred / totalRequests : 0.0;
  }

  /**
   * Checks if the connection metrics indicate healthy performance.
   * This is based on error rate, response times, and connection utilization.
   *
   * @return true if metrics indicate healthy performance
   */
  public boolean isHealthy() {
    return errorRate < 0.05 && // Less than 5% error rate
           averageResponseTime.toMillis() < 1000 && // Less than 1 second average response
           getConnectionUtilization() < 0.8; // Less than 80% connection utilization
  }

  /**
   * Checks if the connection metrics indicate degraded performance.
   *
   * @return true if metrics indicate degraded performance
   */
  public boolean isDegraded() {
    return !isHealthy() && errorRate < 0.20; // Between 5% and 20% error rate
  }

  @Override
  public String toString() {
    return String.format(
        "ConnectionMetrics{totalRequests=%d, successRate=%.2f%%, errorRate=%.2f%%, " +
        "avgResponseTime=%dms, activeConnections=%d/%d, utilization=%.1f%%, " +
        "requestsPerSecond=%.1f, collectedAt=%s}",
        totalRequests, getSuccessRate() * 100, errorRate * 100,
        averageResponseTime.toMillis(), activeConnections, maxConnections,
        getConnectionUtilization() * 100, getRequestsPerSecond(), metricsCollectedAt);
  }

  /**
   * Builder class for creating ConnectionMetrics instances.
   */
  public static class Builder {
    private long totalRequests;
    private long successfulRequests;
    private long failedRequests;
    private double errorRate;
    private Duration averageResponseTime;
    private Duration p95ResponseTime;
    private Duration p99ResponseTime;
    private int activeConnections;
    private int idleConnections;
    private int maxConnections;
    private long totalBytesTransferred;
    private Instant metricsCollectedAt;
    private Duration collectionPeriod;
    private Map<String, Long> errorsByType;
    private Map<String, Long> requestsByEndpoint;

    public Builder setTotalRequests(long totalRequests) {
      this.totalRequests = totalRequests;
      return this;
    }

    public Builder setSuccessfulRequests(long successfulRequests) {
      this.successfulRequests = successfulRequests;
      return this;
    }

    public Builder setFailedRequests(long failedRequests) {
      this.failedRequests = failedRequests;
      return this;
    }

    public Builder setErrorRate(double errorRate) {
      this.errorRate = errorRate;
      return this;
    }

    public Builder setAverageResponseTime(Duration averageResponseTime) {
      this.averageResponseTime = averageResponseTime;
      return this;
    }

    public Builder setP95ResponseTime(Duration p95ResponseTime) {
      this.p95ResponseTime = p95ResponseTime;
      return this;
    }

    public Builder setP99ResponseTime(Duration p99ResponseTime) {
      this.p99ResponseTime = p99ResponseTime;
      return this;
    }

    public Builder setActiveConnections(int activeConnections) {
      this.activeConnections = activeConnections;
      return this;
    }

    public Builder setIdleConnections(int idleConnections) {
      this.idleConnections = idleConnections;
      return this;
    }

    public Builder setMaxConnections(int maxConnections) {
      this.maxConnections = maxConnections;
      return this;
    }

    public Builder setTotalBytesTransferred(long totalBytesTransferred) {
      this.totalBytesTransferred = totalBytesTransferred;
      return this;
    }

    public Builder setMetricsCollectedAt(Instant metricsCollectedAt) {
      this.metricsCollectedAt = metricsCollectedAt;
      return this;
    }

    public Builder setCollectionPeriod(Duration collectionPeriod) {
      this.collectionPeriod = collectionPeriod;
      return this;
    }

    public Builder setErrorsByType(Map<String, Long> errorsByType) {
      this.errorsByType = errorsByType;
      return this;
    }

    public Builder setRequestsByEndpoint(Map<String, Long> requestsByEndpoint) {
      this.requestsByEndpoint = requestsByEndpoint;
      return this;
    }

    public ConnectionMetrics build() {
      return new ConnectionMetrics(totalRequests, successfulRequests, failedRequests, errorRate,
          averageResponseTime, p95ResponseTime, p99ResponseTime, activeConnections,
          idleConnections, maxConnections, totalBytesTransferred, metricsCollectedAt,
          collectionPeriod, errorsByType, requestsByEndpoint);
    }
  }
}