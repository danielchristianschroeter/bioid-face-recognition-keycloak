package com.bioid.keycloak.reconciliation;

/**
 * Types of repair actions.
 */
public enum RepairActionType {
    DELETE_ORPHANED_CREDENTIAL,
    DELETE_ORPHANED_TEMPLATE,
    UPDATE_METADATA,
    RECREATE_CREDENTIAL,
    RECREATE_TEMPLATE,
    RESOLVE_CONFLICT,
    MANUAL_INTERVENTION_REQUIRED
}

