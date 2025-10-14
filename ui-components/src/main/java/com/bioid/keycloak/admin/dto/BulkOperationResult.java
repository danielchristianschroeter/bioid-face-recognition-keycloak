package com.bioid.keycloak.admin.dto;

import java.time.Instant;
import java.util.List;

/**
 * DTO for bulk operation results and status.
 */
public class BulkOperationResult {
    private String operationId;
    private String status;
    private int totalItems;
    private int processedItems;
    private int successfulItems;
    private int failedItems;
    private List<BulkOperationError> errors;
    private Instant startedAt;
    private Instant completedAt;

    public BulkOperationResult() {}

    private BulkOperationResult(Builder builder) {
        this.operationId = builder.operationId;
        this.status = builder.status;
        this.totalItems = builder.totalItems;
        this.processedItems = builder.processedItems;
        this.successfulItems = builder.successfulItems;
        this.failedItems = builder.failedItems;
        this.errors = builder.errors;
        this.startedAt = builder.startedAt;
        this.completedAt = builder.completedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getOperationId() {
        return operationId;
    }

    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
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

    public int getSuccessfulItems() {
        return successfulItems;
    }

    public void setSuccessfulItems(int successfulItems) {
        this.successfulItems = successfulItems;
    }

    public int getFailedItems() {
        return failedItems;
    }

    public void setFailedItems(int failedItems) {
        this.failedItems = failedItems;
    }

    public List<BulkOperationError> getErrors() {
        return errors;
    }

    public void setErrors(List<BulkOperationError> errors) {
        this.errors = errors;
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

    public static class Builder {
        private String operationId;
        private String status;
        private int totalItems;
        private int processedItems;
        private int successfulItems;
        private int failedItems;
        private List<BulkOperationError> errors;
        private Instant startedAt;
        private Instant completedAt;

        public Builder operationId(String operationId) {
            this.operationId = operationId;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder totalItems(int totalItems) {
            this.totalItems = totalItems;
            return this;
        }

        public Builder processedItems(int processedItems) {
            this.processedItems = processedItems;
            return this;
        }

        public Builder successfulItems(int successfulItems) {
            this.successfulItems = successfulItems;
            return this;
        }

        public Builder failedItems(int failedItems) {
            this.failedItems = failedItems;
            return this;
        }

        public Builder errors(List<BulkOperationError> errors) {
            this.errors = errors;
            return this;
        }

        public Builder startedAt(Instant startedAt) {
            this.startedAt = startedAt;
            return this;
        }

        public Builder completedAt(Instant completedAt) {
            this.completedAt = completedAt;
            return this;
        }

        public BulkOperationResult build() {
            return new BulkOperationResult(this);
        }
    }
}