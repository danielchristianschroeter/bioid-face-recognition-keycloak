package com.bioid.keycloak.error;

/**
 * Status enumeration for bulk operations
 */
public enum BulkOperationStatus {
    PENDING("Operation is queued and waiting to start"),
    RUNNING("Operation is currently in progress"),
    COMPLETED("Operation completed successfully"),
    FAILED("Operation failed completely"),
    CANCELLED("Operation was cancelled by user"),
    PARTIALLY_COMPLETED("Operation completed with some failures");

    private final String description;

    BulkOperationStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == CANCELLED || this == PARTIALLY_COMPLETED;
    }

    public boolean isSuccessful() {
        return this == COMPLETED;
    }

    public boolean hasFailures() {
        return this == FAILED || this == PARTIALLY_COMPLETED;
    }
}