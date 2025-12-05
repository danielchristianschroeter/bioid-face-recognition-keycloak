package com.bioid.keycloak.reconciliation;

/**
 * Types of synchronization actions.
 */
public enum SynchronizationActionType {
    UPDATE_METADATA,
    CREATE_CREDENTIAL,
    DELETE_CREDENTIAL,
    CREATE_TEMPLATE,
    DELETE_TEMPLATE,
    RESOLVE_CONFLICT
}

