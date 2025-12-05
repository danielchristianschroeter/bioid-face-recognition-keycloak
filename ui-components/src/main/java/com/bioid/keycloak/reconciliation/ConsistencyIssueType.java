package com.bioid.keycloak.reconciliation;

/**
 * Types of consistency issues that can be detected.
 */
public enum ConsistencyIssueType {
    ORPHANED_CREDENTIAL("Orphaned Credential",
        "Credential exists in Keycloak but no corresponding template in BioID"),
    ORPHANED_TEMPLATE("Orphaned Template",
        "Template exists in BioID but no corresponding credential in Keycloak"),
    METADATA_MISMATCH("Metadata Mismatch", "Metadata differs between Keycloak and BioID"),
    SYNC_CONFLICT("Sync Conflict", "Conflicting data that cannot be automatically resolved"),
    CORRUPTED_DATA("Corrupted Data", "Data corruption detected in either system"),
    INVALID_REFERENCE("Invalid Reference", "Invalid reference between credential and template");

    private final String displayName;
    private final String description;

    ConsistencyIssueType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}

