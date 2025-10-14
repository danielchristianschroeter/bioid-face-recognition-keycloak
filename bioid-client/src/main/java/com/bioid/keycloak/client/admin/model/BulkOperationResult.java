package com.bioid.keycloak.client.admin.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Result of a bulk operation with detailed success/failure reporting.
 * 
 * @param <T> the type of individual operation results
 */
public class BulkOperationResult<T> {
    private final String operationId;
    private BulkOperationStatus status;
    private final int totalItems;
    private int processedItems;
    private int successfulItems;
    private int failedItems;
    private final List<BulkOperationError> errors;
    private final List<T> results;
    private final Instant startedAt;
    private Instant completedAt;

    public BulkOperationResult(String operationId, int totalItems) {
        this.operationId = operationId;
        this.totalItems = totalItems;
        this.status = BulkOperationStatus.PENDING;
        this.processedItems = 0;
        this.successfulItems = 0;
        this.failedItems = 0;
        this.errors = new ArrayList<>();
        this.results = new ArrayList<>();
        this.startedAt = Instant.now();
    }

    public String getOperationId() {
        return operationId;
    }

    public BulkOperationStatus getStatus() {
        return status;
    }

    public void setStatus(BulkOperationStatus status) {
        this.status = status;
    }

    public int getTotalItems() {
        return totalItems;
    }

    public int getProcessedItems() {
        return processedItems;
    }

    public int getSuccessfulItems() {
        return successfulItems;
    }

    public int getFailedItems() {
        return failedItems;
    }

    public List<BulkOperationError> getErrors() {
        return errors;
    }

    public List<T> getResults() {
        return results;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public void addSuccess(T result) {
        this.results.add(result);
        this.successfulItems++;
        this.processedItems++;
    }

    public void addError(BulkOperationError error) {
        this.errors.add(error);
        this.failedItems++;
        this.processedItems++;
    }

    public double getSuccessRate() {
        return processedItems > 0 ? (double) successfulItems / processedItems * 100.0 : 0.0;
    }

    public double getProgressPercentage() {
        return totalItems > 0 ? (double) processedItems / totalItems * 100.0 : 0.0;
    }

    public long getDurationMillis() {
        if (completedAt == null) {
            return Instant.now().toEpochMilli() - startedAt.toEpochMilli();
        }
        return completedAt.toEpochMilli() - startedAt.toEpochMilli();
    }

    public boolean isCompleted() {
        return status == BulkOperationStatus.COMPLETED || 
               status == BulkOperationStatus.FAILED || 
               status == BulkOperationStatus.CANCELLED ||
               status == BulkOperationStatus.PARTIALLY_COMPLETED;
    }

    public void markCompleted() {
        this.completedAt = Instant.now();
        if (failedItems == 0) {
            this.status = BulkOperationStatus.COMPLETED;
        } else if (successfulItems == 0) {
            this.status = BulkOperationStatus.FAILED;
        } else {
            this.status = BulkOperationStatus.PARTIALLY_COMPLETED;
        }
    }

    public void markCancelled() {
        this.completedAt = Instant.now();
        this.status = BulkOperationStatus.CANCELLED;
    }

    public void markRunning() {
        this.status = BulkOperationStatus.RUNNING;
    }
}