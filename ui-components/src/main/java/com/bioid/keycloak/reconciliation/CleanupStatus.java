package com.bioid.keycloak.reconciliation;

/**
 * Status of cleanup operation.
 */
public enum CleanupStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED,
    PARTIALLY_COMPLETED
}

