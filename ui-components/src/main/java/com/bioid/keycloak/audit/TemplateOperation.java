package com.bioid.keycloak.audit;

/**
 * Enumeration of template operations for audit logging.
 */
public enum TemplateOperation {
    VIEW("Template viewed"),
    STATUS_CHECK("Template status checked"),
    THUMBNAIL_DOWNLOAD("Template thumbnail downloaded"),
    UPGRADE("Template upgraded"),
    DELETE("Template deleted"),
    TAG_UPDATE("Template tags updated"),
    HEALTH_CHECK("Template health checked"),
    METADATA_ACCESS("Template metadata accessed");

    private final String description;

    TemplateOperation(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Checks if this operation involves accessing sensitive data.
     */
    public boolean involvesSensitiveData() {
        return this == THUMBNAIL_DOWNLOAD ||
               this == METADATA_ACCESS;
    }

    /**
     * Checks if this operation modifies the template.
     */
    public boolean isModifyingOperation() {
        return this == UPGRADE ||
               this == DELETE ||
               this == TAG_UPDATE;
    }

    /**
     * Gets the risk level for this operation.
     */
    public RiskLevel getRiskLevel() {
        switch (this) {
            case DELETE:
                return RiskLevel.HIGH;
            case UPGRADE:
            case TAG_UPDATE:
            case THUMBNAIL_DOWNLOAD:
                return RiskLevel.MEDIUM;
            default:
                return RiskLevel.LOW;
        }
    }
}