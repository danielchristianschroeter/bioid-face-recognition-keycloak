package com.bioid.keycloak.audit;

/**
 * Enumeration of administrative action results for audit logging.
 */
public enum AdminActionResult {
    SUCCESS("Operation completed successfully"),
    FAILURE("Operation failed"),
    PARTIAL_SUCCESS("Operation completed with some failures"),
    CANCELLED("Operation was cancelled"),
    TIMEOUT("Operation timed out"),
    PERMISSION_DENIED("Operation denied due to insufficient permissions"),
    VALIDATION_ERROR("Operation failed due to validation errors"),
    SERVICE_UNAVAILABLE("Operation failed due to service unavailability");

    private final String description;

    AdminActionResult(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Checks if this result indicates a successful operation.
     */
    public boolean isSuccess() {
        return this == SUCCESS || this == PARTIAL_SUCCESS;
    }

    /**
     * Checks if this result indicates a failure that should be investigated.
     */
    public boolean requiresInvestigation() {
        return this == PERMISSION_DENIED || 
               this == VALIDATION_ERROR ||
               this == SERVICE_UNAVAILABLE;
    }

    /**
     * Checks if this result indicates a retryable failure.
     */
    public boolean isRetryable() {
        return this == TIMEOUT || this == SERVICE_UNAVAILABLE;
    }
}