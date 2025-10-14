package com.bioid.keycloak.client.admin.model;

/**
 * Status of a bulk operation.
 */
public enum BulkOperationStatus {
    /**
     * Operation is queued but not yet started.
     */
    PENDING,
    
    /**
     * Operation is currently running.
     */
    RUNNING,
    
    /**
     * Operation completed successfully for all items.
     */
    COMPLETED,
    
    /**
     * Operation failed for all items.
     */
    FAILED,
    
    /**
     * Operation was cancelled by user or system.
     */
    CANCELLED,
    
    /**
     * Operation completed with some successes and some failures.
     */
    PARTIALLY_COMPLETED
}