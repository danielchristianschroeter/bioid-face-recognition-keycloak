package com.bioid.keycloak.audit;

/**
 * Enumeration of bulk operation types for audit logging.
 */
public enum BulkOperationType {
    ENROLLMENT_LINKS("Bulk enrollment link generation"),
    TEMPLATE_DELETION("Bulk template deletion"),
    TEMPLATE_UPGRADE("Bulk template upgrade"),
    TEMPLATE_TAG_UPDATE("Bulk template tag update"),
    USER_CLEANUP("Bulk user cleanup"),
    DATA_MIGRATION("Bulk data migration"),
    CONFIGURATION_UPDATE("Bulk configuration update");

    private final String description;

    BulkOperationType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Checks if this bulk operation type is considered high-risk.
     */
    public boolean isHighRisk() {
        return this == TEMPLATE_DELETION ||
               this == USER_CLEANUP ||
               this == DATA_MIGRATION;
    }

    /**
     * Gets the corresponding AdminActionType for this bulk operation.
     */
    public AdminActionType getAdminActionType() {
        switch (this) {
            case ENROLLMENT_LINKS:
                return AdminActionType.BULK_ENROLLMENT_LINKS;
            case TEMPLATE_DELETION:
                return AdminActionType.TEMPLATE_BATCH_DELETE;
            case TEMPLATE_UPGRADE:
                return AdminActionType.TEMPLATE_BATCH_UPGRADE;
            case TEMPLATE_TAG_UPDATE:
                return AdminActionType.BULK_TEMPLATE_TAGS;
            default:
                return AdminActionType.BULK_OPERATION;
        }
    }
}