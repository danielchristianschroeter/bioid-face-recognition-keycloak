package com.bioid.keycloak.error;

import java.time.Instant;
import java.util.List;
import java.util.ArrayList;

/**
 * Result container for bulk operations with detailed success/failure reporting
 */
public class BulkOperationResult<T> {
    
    private String operationId;
    private BulkOperationStatus status = BulkOperationStatus.PENDING;
    private int totalItems;
    private int processedItems;
    private int successfulItems;
    private int failedItems;
    private List<BulkOperationError> errors = new ArrayList<>();
    private List<T> results = new ArrayList<>();
    private Instant startedAt;
    private Instant completedAt;
    private String operationType;
    private String description;

    public BulkOperationResult() {
    }

    public BulkOperationResult(String operationId, String operationType) {
        this.operationId = operationId;
        this.operationType = operationType;
        this.startedAt = Instant.now();
    }

    // Getters and setters
    public String getOperationId() {
        return operationId;
    }

    public void setOperationId(String operationId) {
        this.operationId = operationId;
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

    public void setTotalItems(int totalItems) {
        this.totalItems = totalItems;
    }

    public int getProcessedItems() {
        return processedItems;
    }

    public void setProcessedItems(int processedItems) {
        this.processedItems = processedItems;
    }

    public void incrementProcessedItems() {
        this.processedItems++;
    }

    public int getSuccessfulItems() {
        return successfulItems;
    }

    public void setSuccessfulItems(int successfulItems) {
        this.successfulItems = successfulItems;
    }

    public void incrementSuccessfulItems() {
        this.successfulItems++;
    }

    public int getFailedItems() {
        return failedItems;
    }

    public void setFailedItems(int failedItems) {
        this.failedItems = failedItems;
    }

    public void incrementFailedItems() {
        this.failedItems++;
    }

    public List<BulkOperationError> getErrors() {
        return new ArrayList<>(errors);
    }

    public void setErrors(List<BulkOperationError> errors) {
        this.errors = new ArrayList<>(errors);
    }

    public void addError(BulkOperationError error) {
        this.errors.add(error);
    }

    public List<T> getResults() {
        return new ArrayList<>(results);
    }

    public void setResults(List<T> results) {
        this.results = new ArrayList<>(results);
    }

    public void addResult(T result) {
        this.results.add(result);
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    // Utility methods
    public double getSuccessRate() {
        if (totalItems == 0) {
            return 0.0;
        }
        return (double) successfulItems / totalItems;
    }

    public double getFailureRate() {
        if (totalItems == 0) {
            return 0.0;
        }
        return (double) failedItems / totalItems;
    }

    public boolean isCompleted() {
        return status == BulkOperationStatus.COMPLETED || 
               status == BulkOperationStatus.FAILED || 
               status == BulkOperationStatus.PARTIALLY_COMPLETED ||
               status == BulkOperationStatus.CANCELLED;
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public long getDurationMs() {
        if (startedAt == null) {
            return 0;
        }
        Instant endTime = completedAt != null ? completedAt : Instant.now();
        return endTime.toEpochMilli() - startedAt.toEpochMilli();
    }

    @Override
    public String toString() {
        return String.format("BulkOperationResult{operationId='%s', status=%s, totalItems=%d, " +
                "processedItems=%d, successfulItems=%d, failedItems=%d, operationType='%s', " +
                "duration=%dms}", 
                operationId, status, totalItems, processedItems, successfulItems, 
                failedItems, operationType, getDurationMs());
    }
}