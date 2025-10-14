package com.bioid.keycloak.client.admin.model;

import java.time.Instant;
import java.util.List;

/**
 * Result of a bulk template deletion operation.
 */
public class BulkDeleteResult {
    private final int totalTemplates;
    private final int successfulDeletions;
    private final int failedDeletions;
    private final List<Long> deletedClassIds;
    private final List<BulkOperationError> errors;
    private final Instant startedAt;
    private final Instant completedAt;

    public BulkDeleteResult(int totalTemplates, int successfulDeletions, int failedDeletions,
                           List<Long> deletedClassIds, List<BulkOperationError> errors,
                           Instant startedAt, Instant completedAt) {
        this.totalTemplates = totalTemplates;
        this.successfulDeletions = successfulDeletions;
        this.failedDeletions = failedDeletions;
        this.deletedClassIds = deletedClassIds;
        this.errors = errors;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
    }

    public int getTotalTemplates() {
        return totalTemplates;
    }

    public int getSuccessfulDeletions() {
        return successfulDeletions;
    }

    public int getFailedDeletions() {
        return failedDeletions;
    }

    public List<Long> getDeletedClassIds() {
        return deletedClassIds;
    }

    public List<BulkOperationError> getErrors() {
        return errors;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public double getSuccessRate() {
        return totalTemplates > 0 ? (double) successfulDeletions / totalTemplates * 100.0 : 0.0;
    }

    public long getDurationMillis() {
        return completedAt.toEpochMilli() - startedAt.toEpochMilli();
    }
}