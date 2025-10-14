package com.bioid.keycloak.client;

import com.bioid.keycloak.client.exception.BioIdException;
import com.bioid.keycloak.client.health.ServiceHealthStatus;
import com.bioid.keycloak.client.metrics.ConnectionMetrics;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Enhanced BioID client interface that extends the base BioIdClient with administrative
 * capabilities, bulk operations, health monitoring, and regional endpoint management.
 *
 * <p>This interface provides:
 * - Bulk template status queries and operations
 * - Service health monitoring and connection metrics
 * - Regional endpoint management with automatic failover
 * - Enhanced error handling and retry logic
 *
 * <p>All administrative methods are designed to be used by system administrators
 * and require appropriate permissions and authentication.
 */
public interface EnhancedBioIdClient extends BioIdClient {

  // ========== Bulk Template Operations ==========

  /**
   * Retrieves template status for multiple class IDs in a single batch operation.
   * This method is optimized for bulk queries and reduces the number of individual
   * gRPC calls required for large-scale template management.
   *
   * @param classIds list of class IDs to query template status for
   * @param includeThumbnails whether to include thumbnail data in the response
   * @return list of template status strings in the same order as the input class IDs
   * @throws BioIdException if the batch operation fails
   */
  List<String> getTemplateStatusBatch(
      List<Long> classIds, boolean includeThumbnails) throws BioIdException;

  /**
   * Asynchronously retrieves template status for multiple class IDs.
   *
   * @param classIds list of class IDs to query template status for
   * @param includeThumbnails whether to include thumbnail data in the response
   * @return CompletableFuture containing the list of template status strings
   */
  CompletableFuture<List<String>> getTemplateStatusBatchAsync(
      List<Long> classIds, boolean includeThumbnails);

  /**
   * Deletes multiple templates in a single batch operation.
   * This method provides efficient bulk deletion with detailed error reporting
   * for individual failures within the batch.
   *
   * @param classIds list of class IDs to delete templates for
   * @return bulk delete result containing success/failure information for each template
   * @throws BioIdException if the batch operation fails completely
   */
  BulkDeleteResult deleteTemplatesBatch(List<Long> classIds) throws BioIdException;

  /**
   * Asynchronously deletes multiple templates in a batch operation.
   *
   * @param classIds list of class IDs to delete templates for
   * @return CompletableFuture containing the bulk delete result
   */
  CompletableFuture<BulkDeleteResult> deleteTemplatesBatchAsync(List<Long> classIds);

  // ========== Health Monitoring and Metrics ==========

  /**
   * Retrieves the current health status of the BioID service.
   * This includes connectivity status, response times, and service availability.
   *
   * @return current service health status
   */
  ServiceHealthStatus getServiceHealth();

  /**
   * Retrieves detailed connection metrics for monitoring and diagnostics.
   * This includes request counts, error rates, latency statistics, and connection pool status.
   *
   * @return current connection metrics
   */
  ConnectionMetrics getConnectionMetrics();

  /**
   * Performs a health check by making a lightweight request to the BioID service.
   * This method can be used for monitoring and alerting purposes.
   *
   * @return true if the service is healthy and responsive, false otherwise
   */
  boolean performHealthCheck();

  /**
   * Asynchronously performs a health check.
   *
   * @return CompletableFuture containing the health check result
   */
  CompletableFuture<Boolean> performHealthCheckAsync();

  // ========== Regional Endpoint Management ==========

  /**
   * Switches the client to use a different regional endpoint.
   * This method enables geographic load balancing and disaster recovery scenarios.
   *
   * @param region the region identifier to switch to
   * @throws BioIdException if the region is not available or switching fails
   */
  void switchToRegion(String region) throws BioIdException;

  /**
   * Retrieves the list of available regional endpoints.
   * This can be used to implement intelligent routing based on latency or availability.
   *
   * @return list of available region identifiers
   */
  List<String> getAvailableRegions();

  /**
   * Gets the currently active regional endpoint.
   *
   * @return the current region identifier
   */
  String getCurrentRegion();

  /**
   * Automatically selects the best regional endpoint based on latency and availability.
   * This method performs latency tests to multiple regions and selects the optimal one.
   *
   * @return the selected region identifier
   * @throws BioIdException if no suitable region is available
   */
  String selectOptimalRegion() throws BioIdException;

  /**
   * Asynchronously selects the optimal regional endpoint.
   *
   * @return CompletableFuture containing the selected region identifier
   */
  CompletableFuture<String> selectOptimalRegionAsync();

  // ========== Connection Pool Management ==========

  /**
   * Gets the current status of the connection pool.
   * This includes active connections, idle connections, and pool configuration.
   *
   * @return connection pool status information
   */
  ConnectionPoolStatus getConnectionPoolStatus();

  /**
   * Refreshes the connection pool by closing idle connections and establishing new ones.
   * This can be useful for recovering from network issues or configuration changes.
   */
  void refreshConnectionPool();

  /**
   * Configures the connection pool with new settings.
   * This allows runtime adjustment of pool size, timeouts, and other parameters.
   *
   * @param config new connection pool configuration
   * @throws BioIdException if the configuration is invalid or cannot be applied
   */
  void configureConnectionPool(ConnectionPoolConfig config) throws BioIdException;

  // ========== Data Transfer Objects ==========

  /**
   * Result of a bulk delete operation containing detailed success/failure information.
   */
  class BulkDeleteResult {
    private final int totalRequested;
    private final int successfulDeletes;
    private final int failedDeletes;
    private final List<BulkDeleteError> errors;

    public BulkDeleteResult(int totalRequested, int successfulDeletes, int failedDeletes,
        List<BulkDeleteError> errors) {
      this.totalRequested = totalRequested;
      this.successfulDeletes = successfulDeletes;
      this.failedDeletes = failedDeletes;
      this.errors = errors;
    }

    public int getTotalRequested() { return totalRequested; }
    public int getSuccessfulDeletes() { return successfulDeletes; }
    public int getFailedDeletes() { return failedDeletes; }
    public List<BulkDeleteError> getErrors() { return errors; }
    public boolean hasErrors() { return !errors.isEmpty(); }
    public double getSuccessRate() { 
      return totalRequested > 0 ? (double) successfulDeletes / totalRequested : 0.0; 
    }
  }

  /**
   * Information about a failed delete operation within a bulk delete.
   */
  class BulkDeleteError {
    private final long classId;
    private final String errorCode;
    private final String errorMessage;
    private final boolean retryable;

    public BulkDeleteError(long classId, String errorCode, String errorMessage, boolean retryable) {
      this.classId = classId;
      this.errorCode = errorCode;
      this.errorMessage = errorMessage;
      this.retryable = retryable;
    }

    public long getClassId() { return classId; }
    public String getErrorCode() { return errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public boolean isRetryable() { return retryable; }
  }

  /**
   * Connection pool status information.
   */
  class ConnectionPoolStatus {
    private final int totalConnections;
    private final int activeConnections;
    private final int idleConnections;
    private final int maxPoolSize;
    private final long totalRequestsServed;
    private final double averageRequestTime;

    public ConnectionPoolStatus(int totalConnections, int activeConnections, int idleConnections,
        int maxPoolSize, long totalRequestsServed, double averageRequestTime) {
      this.totalConnections = totalConnections;
      this.activeConnections = activeConnections;
      this.idleConnections = idleConnections;
      this.maxPoolSize = maxPoolSize;
      this.totalRequestsServed = totalRequestsServed;
      this.averageRequestTime = averageRequestTime;
    }

    public int getTotalConnections() { return totalConnections; }
    public int getActiveConnections() { return activeConnections; }
    public int getIdleConnections() { return idleConnections; }
    public int getMaxPoolSize() { return maxPoolSize; }
    public long getTotalRequestsServed() { return totalRequestsServed; }
    public double getAverageRequestTime() { return averageRequestTime; }
    public double getUtilizationRate() { 
      return maxPoolSize > 0 ? (double) activeConnections / maxPoolSize : 0.0; 
    }
  }

  /**
   * Connection pool configuration.
   */
  class ConnectionPoolConfig {
    private final int maxPoolSize;
    private final int minIdleConnections;
    private final long connectionTimeoutMs;
    private final long idleTimeoutMs;
    private final long maxLifetimeMs;

    public ConnectionPoolConfig(int maxPoolSize, int minIdleConnections, 
        long connectionTimeoutMs, long idleTimeoutMs, long maxLifetimeMs) {
      this.maxPoolSize = maxPoolSize;
      this.minIdleConnections = minIdleConnections;
      this.connectionTimeoutMs = connectionTimeoutMs;
      this.idleTimeoutMs = idleTimeoutMs;
      this.maxLifetimeMs = maxLifetimeMs;
    }

    public int getMaxPoolSize() { return maxPoolSize; }
    public int getMinIdleConnections() { return minIdleConnections; }
    public long getConnectionTimeoutMs() { return connectionTimeoutMs; }
    public long getIdleTimeoutMs() { return idleTimeoutMs; }
    public long getMaxLifetimeMs() { return maxLifetimeMs; }
  }
}