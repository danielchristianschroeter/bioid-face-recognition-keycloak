package com.bioid.keycloak.client.admin.model;

import java.time.Instant;

/**
 * Summary information for a bulk operation.
 */
public class BulkOperationSummary {
    private final String operationId;
    private final String operationType;
    private final BulkOperationStatus status;
    private final int totalItems;
    private final int processedItems;
    private final int successfulItems;
    private final int failedItems;
    private final Instant startedAt;
    private final Instant completedAt;
    private final String initiatedBy;

    public BulkOperationSummary(String operationId, String operationType, BulkOperationStatus status,
                              int totalItems, int processedItems, int successfulItems, int failedItems,
                              Instant startedAt, Instant completedAt, String initiatedBy) {
        this.operationId = operationId;
        this.operationType = operationType;
        this.status = status;
        this.totalItems = totalItems;
        this.processedItems = processedItems;
        this.successfulItems = successfulItems;
        this.failedItems = failedItems;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.initiatedBy = initiatedBy;
    }

    public String getOperationId() {
        return operationId;
    }

    public String getOperationType() {
        return operationType;
    }

    public BulkOperationStatus getStatus() {
        return status;
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

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public String getInitiatedBy() {
        return initiatedBy;
    }

    public double getSuccessRate() {
        return processedItems > 0 ? (double) successfulItems / processedItems * 100.0 : 0.0;
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
}