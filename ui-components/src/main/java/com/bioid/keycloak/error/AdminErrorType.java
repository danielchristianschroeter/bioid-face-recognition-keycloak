package com.bioid.keycloak.error;

/**
 * Enumeration of administrative error types with specific classifications
 * for different categories of failures in the BioID administrative system.
 */
public enum AdminErrorType {
    // Template Management Errors
    TEMPLATE_NOT_FOUND("TEMPLATE_001", "Template does not exist for the specified class ID", true),
    TEMPLATE_CORRUPTED("TEMPLATE_002", "Template data is corrupted or invalid", false),
    TEMPLATE_UPGRADE_FAILED("TEMPLATE_003", "Template upgrade operation failed", true),
    TEMPLATE_SYNC_MISMATCH("TEMPLATE_004", "Template status mismatch between Keycloak and BioID", true),
    TEMPLATE_ACCESS_DENIED("TEMPLATE_005", "Access denied for template operation", false),
    TEMPLATE_QUOTA_EXCEEDED("TEMPLATE_006", "Template storage quota exceeded", false),
    
    // Liveness Detection Errors
    LIVENESS_DETECTION_FAILED("LIVENESS_001", "Liveness detection operation failed", true),
    INVALID_LIVENESS_MODE("LIVENESS_002", "Unsupported liveness detection mode", false),
    CHALLENGE_RESPONSE_FAILED("LIVENESS_003", "Challenge-response validation failed", false),
    INSUFFICIENT_IMAGES("LIVENESS_004", "Insufficient images for requested liveness mode", false),
    LIVENESS_THRESHOLD_INVALID("LIVENESS_005", "Invalid liveness threshold value", false),
    LIVENESS_SERVICE_UNAVAILABLE("LIVENESS_006", "Liveness detection service unavailable", true),
    
    // Bulk Operation Errors
    BULK_OPERATION_TIMEOUT("BULK_001", "Bulk operation exceeded maximum execution time", true),
    BULK_OPERATION_CANCELLED("BULK_002", "Bulk operation was cancelled by user", false),
    PARTIAL_BULK_FAILURE("BULK_003", "Bulk operation completed with some failures", false),
    CONCURRENT_OPERATION_LIMIT("BULK_004", "Maximum concurrent operations exceeded", true),
    BULK_OPERATION_TOO_LARGE("BULK_005", "Bulk operation size exceeds maximum limit", false),
    BULK_OPERATION_INVALID_INPUT("BULK_006", "Invalid input data for bulk operation", false),
    
    // Configuration Errors
    INVALID_CONFIGURATION("CONFIG_001", "Configuration contains invalid values", false),
    CONFIGURATION_CONFLICT("CONFIG_002", "Configuration conflicts with existing settings", false),
    PERMISSION_DENIED("CONFIG_003", "Insufficient permissions for requested operation", false),
    CONFIGURATION_NOT_FOUND("CONFIG_004", "Configuration not found", false),
    CONFIGURATION_LOCKED("CONFIG_005", "Configuration is locked and cannot be modified", false),
    
    // Service Errors
    SERVICE_UNAVAILABLE("SERVICE_001", "BioID service is temporarily unavailable", true),
    RATE_LIMIT_EXCEEDED("SERVICE_002", "API rate limit exceeded", true),
    QUOTA_EXCEEDED("SERVICE_003", "Service quota exceeded", false),
    REGION_UNAVAILABLE("SERVICE_004", "Requested region is not available", true),
    CONNECTION_TIMEOUT("SERVICE_005", "Connection to BioID service timed out", true),
    AUTHENTICATION_FAILED("SERVICE_006", "Authentication with BioID service failed", true),
    
    // Data Consistency Errors
    DATA_INCONSISTENCY("DATA_001", "Data inconsistency detected between systems", true),
    ORPHANED_CREDENTIAL("DATA_002", "Orphaned credential without corresponding template", true),
    ORPHANED_TEMPLATE("DATA_003", "Orphaned template without corresponding credential", true),
    SYNC_CONFLICT("DATA_004", "Synchronization conflict detected", true),
    DATA_CORRUPTION("DATA_005", "Data corruption detected", false),
    
    // Network and Infrastructure Errors
    NETWORK_ERROR("NETWORK_001", "Network communication error", true),
    CIRCUIT_BREAKER_OPEN("NETWORK_002", "Circuit breaker is open", true),
    LOAD_BALANCER_ERROR("NETWORK_003", "Load balancer error", true),
    DNS_RESOLUTION_FAILED("NETWORK_004", "DNS resolution failed", true),
    
    // Security Errors
    SECURITY_VIOLATION("SECURITY_001", "Security policy violation", false),
    CSRF_TOKEN_INVALID("SECURITY_002", "CSRF token validation failed", false),
    SESSION_EXPIRED("SECURITY_003", "Administrative session expired", false),
    AUDIT_LOG_FAILURE("SECURITY_004", "Failed to write audit log", true),
    
    // Unknown/Generic Errors
    UNKNOWN_ERROR("UNKNOWN_001", "An unknown error occurred", true),
    INTERNAL_ERROR("INTERNAL_001", "Internal system error", true);

    private final String code;
    private final String description;
    private final boolean retryable;

    AdminErrorType(String code, String description, boolean retryable) {
        this.code = code;
        this.description = description;
        this.retryable = retryable;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public boolean isRetryable() {
        return retryable;
    }

    /**
     * Get error type by code
     */
    public static AdminErrorType fromCode(String code) {
        for (AdminErrorType errorType : values()) {
            if (errorType.code.equals(code)) {
                return errorType;
            }
        }
        return UNKNOWN_ERROR;
    }

    /**
     * Check if error type is in a specific category
     */
    public boolean isTemplateError() {
        return code.startsWith("TEMPLATE_");
    }

    public boolean isLivenessError() {
        return code.startsWith("LIVENESS_");
    }

    public boolean isBulkOperationError() {
        return code.startsWith("BULK_");
    }

    public boolean isServiceError() {
        return code.startsWith("SERVICE_");
    }

    public boolean isDataConsistencyError() {
        return code.startsWith("DATA_");
    }

    public boolean isSecurityError() {
        return code.startsWith("SECURITY_");
    }
}