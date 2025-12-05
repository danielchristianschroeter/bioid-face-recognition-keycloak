package com.bioid.keycloak.reconciliation;

/**
 * Status of repair operation.
 */
public enum RepairStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED,
    PARTIALLY_COMPLETED
}

