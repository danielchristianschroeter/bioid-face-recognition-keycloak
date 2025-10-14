package com.bioid.keycloak.audit;

/**
 * Enumeration of administrative action types for audit logging.
 */
public enum AdminActionType {
    // Template Management Actions
    TEMPLATE_ACCESS("Template status accessed"),
    TEMPLATE_DELETE("Template deleted"),
    TEMPLATE_UPGRADE("Template upgraded"),
    TEMPLATE_BATCH_UPGRADE("Batch template upgrade"),
    TEMPLATE_BATCH_DELETE("Batch template deletion"),
    
    // User Enrollment Actions
    USER_ENROLLMENT_DELETE("User enrollment deleted"),
    ENROLLMENT_LINK_GENERATED("Enrollment link generated"),
    ENROLLMENT_STATUS_RESET("Enrollment status reset"),
    
    // Liveness Detection Actions
    LIVENESS_CONFIG_UPDATE("Liveness detection configuration updated"),
    LIVENESS_TEST_PERFORMED("Liveness detection test performed"),
    
    // Bulk Operations
    BULK_OPERATION("Bulk operation performed"),
    BULK_ENROLLMENT_LINKS("Bulk enrollment links generated"),
    BULK_TEMPLATE_TAGS("Bulk template tags updated"),
    
    // Compliance and Reporting
    COMPLIANCE_REPORT_GENERATED("Compliance report generated"),
    AUDIT_LOG_ACCESSED("Audit log accessed"),
    DATA_EXPORT_REQUESTED("Data export requested"),
    
    // Security Actions
    UNAUTHORIZED_ACCESS_ATTEMPT("Unauthorized access attempt"),
    PERMISSION_DENIED("Permission denied"),
    SESSION_SECURITY_VIOLATION("Session security violation"),
    
    // Configuration Actions
    ADMIN_CONFIG_UPDATE("Administrative configuration updated"),
    CONFIGURATION_UPDATE("Configuration updated"),
    CONFIGURATION_EXPORT("Configuration exported"),
    CONFIGURATION_IMPORT("Configuration imported"),
    RETENTION_POLICY_UPDATE("Audit retention policy updated"),
    SIEM_CONFIG_UPDATE("SIEM integration configuration updated"),
    
    // System Actions
    SERVICE_HEALTH_CHECK("Service health check performed"),
    CONNECTION_TEST("Connection test performed"),
    REGION_SWITCH("Regional endpoint switched");

    private final String description;

    AdminActionType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Checks if this action type is considered high-risk.
     */
    public boolean isHighRisk() {
        return this == TEMPLATE_DELETE ||
               this == USER_ENROLLMENT_DELETE ||
               this == TEMPLATE_BATCH_DELETE ||
               this == BULK_OPERATION ||
               this == ADMIN_CONFIG_UPDATE;
    }

    /**
     * Checks if this action type is compliance-relevant.
     */
    public boolean isComplianceRelevant() {
        return this == TEMPLATE_DELETE ||
               this == USER_ENROLLMENT_DELETE ||
               this == TEMPLATE_ACCESS ||
               this == COMPLIANCE_REPORT_GENERATED ||
               this == DATA_EXPORT_REQUESTED ||
               this == BULK_OPERATION;
    }

    /**
     * Checks if this action type is security-relevant.
     */
    public boolean isSecurityRelevant() {
        return this == UNAUTHORIZED_ACCESS_ATTEMPT ||
               this == PERMISSION_DENIED ||
               this == SESSION_SECURITY_VIOLATION ||
               this == LIVENESS_CONFIG_UPDATE ||
               this == ADMIN_CONFIG_UPDATE ||
               this == SIEM_CONFIG_UPDATE;
    }
}