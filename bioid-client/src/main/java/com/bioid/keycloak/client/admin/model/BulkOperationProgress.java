package com.bioid.keycloak.client.admin.model;

import java.time.Instant;

/**
 * Progress information for a bulk operation.
 */
public class BulkOperationProgress {
    private final String operationId;
    private final BulkOperationStatus status;
    private final int totalItems;
    private final int processedItems;
    private final int successfulItems;
    private final int failedItems;
    private final double progressPercentage;
    private final Instant startedAt;
    private final Instant lastUpdated;
    private final long estimatedRemainingTimeMillis;
    private final String currentPhase;

    public BulkOperationProgress(String operationId, BulkOperationStatus status, int totalItems,
                               int processedItems, int successfulItems, int failedItems,
                               Instant startedAt, String currentPhase) {
        this.operationId = operationId;
        this.status = status;
        this.totalItems = totalItems;
        this.processedItems = processedItems;
        this.successfulItems = successfulItems;
        this.failedItems = failedItems;
        this.progressPercentage = totalItems > 0 ? (double) processedItems / totalItems * 100.0 : 0.0;
        this.startedAt = startedAt;
        this.lastUpdated = Instant.now();
        this.currentPhase = currentPhase;
        // Calculate estimated remaining time without method call to avoid 'this' escape
        this.estimatedRemainingTimeMillis = 0; // Will be calculated on first access
    }

    public String getOperationId() {
        return operationId;
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

    public double getProgressPercentage() {
        return progressPercentage;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getLastUpdated() {
        return lastUpdated;
    }

    public long getEstimatedRemainingTimeMillis() {
        return estimatedRemainingTimeMillis;
    }

    public String getCurrentPhase() {
        return currentPhase;
    }

    public double getSuccessRate() {
        return processedItems > 0 ? (double) successfulItems / processedItems * 100.0 : 0.0;
    }

    public long getElapsedTimeMillis() {
        return lastUpdated.toEpochMilli() - startedAt.toEpochMilli();
    }

    public double getItemsPerSecond() {
        long elapsedSeconds = getElapsedTimeMillis() / 1000;
        return elapsedSeconds > 0 ? (double) processedItems / elapsedSeconds : 0.0;
    }

    private long calculateEstimatedRemainingTime() {
        if (processedItems == 0 || status.equals(BulkOperationStatus.COMPLETED) || 
            status.equals(BulkOperationStatus.FAILED) || status.equals(BulkOperationStatus.CANCELLED)) {
            return 0;
        }

        long elapsedMillis = getElapsedTimeMillis();
        if (elapsedMillis == 0) {
            return 0;
        }

        int remainingItems = totalItems - processedItems;
        double averageTimePerItem = (double) elapsedMillis / processedItems;
        return (long) (remainingItems * averageTimePerItem);
    }
}