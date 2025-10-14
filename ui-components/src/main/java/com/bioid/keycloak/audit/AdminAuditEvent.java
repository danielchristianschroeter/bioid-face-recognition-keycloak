package com.bioid.keycloak.audit;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Map;

/**
 * Comprehensive admin audit event with detailed logging information.
 * 
 * <p>This class extends the basic admin audit event with additional fields
 * for enhanced compliance monitoring, risk assessment, and SIEM integration.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminAuditEvent {

    @JsonProperty("action_type")
    private AdminActionType actionType;

    @JsonProperty("admin_user_id")
    private String adminUserId;

    @JsonProperty("admin_username")
    private String adminUsername;

    @JsonProperty("target_user_id")
    private String targetUserId;

    @JsonProperty("target_username")
    private String targetUsername;

    @JsonProperty("realm_id")
    private String realmId;

    @JsonProperty("realm_name")
    private String realmName;

    @JsonProperty("timestamp")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant timestamp;

    @JsonProperty("ip_address")
    private String ipAddress;

    @JsonProperty("user_agent")
    private String userAgent;

    @JsonProperty("session_id")
    private String sessionId;

    @JsonProperty("details")
    private Map<String, Object> details;

    @JsonProperty("result")
    private AdminActionResult result;

    @JsonProperty("compliance_relevant")
    private boolean complianceRelevant;

    @JsonProperty("security_relevant")
    private boolean securityRelevant;

    @JsonProperty("risk_level")
    private RiskLevel riskLevel;

    @JsonProperty("audit_category")
    private String auditCategory = "BIOMETRIC_ADMINISTRATION";

    // Constructors
    public AdminAuditEvent() {}

    private AdminAuditEvent(Builder builder) {
        this.actionType = builder.actionType;
        this.adminUserId = builder.adminUserId;
        this.adminUsername = builder.adminUsername;
        this.targetUserId = builder.targetUserId;
        this.targetUsername = builder.targetUsername;
        this.realmId = builder.realmId;
        this.realmName = builder.realmName;
        this.timestamp = builder.timestamp;
        this.ipAddress = builder.ipAddress;
        this.userAgent = builder.userAgent;
        this.sessionId = builder.sessionId;
        this.details = builder.details;
        this.result = builder.result;
        this.complianceRelevant = builder.complianceRelevant;
        this.securityRelevant = builder.securityRelevant;
        this.riskLevel = builder.riskLevel;
        this.auditCategory = builder.auditCategory;
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getters and Setters
    public AdminActionType getActionType() {
        return actionType;
    }

    public void setActionType(AdminActionType actionType) {
        this.actionType = actionType;
    }

    public String getAdminUserId() {
        return adminUserId;
    }

    public void setAdminUserId(String adminUserId) {
        this.adminUserId = adminUserId;
    }

    public String getAdminUsername() {
        return adminUsername;
    }

    public void setAdminUsername(String adminUsername) {
        this.adminUsername = adminUsername;
    }

    public String getTargetUserId() {
        return targetUserId;
    }

    public void setTargetUserId(String targetUserId) {
        this.targetUserId = targetUserId;
    }

    public String getTargetUsername() {
        return targetUsername;
    }

    public void setTargetUsername(String targetUsername) {
        this.targetUsername = targetUsername;
    }

    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
    }

    public String getRealmName() {
        return realmName;
    }

    public void setRealmName(String realmName) {
        this.realmName = realmName;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public void setDetails(Map<String, Object> details) {
        this.details = details;
    }

    public AdminActionResult getResult() {
        return result;
    }

    public void setResult(AdminActionResult result) {
        this.result = result;
    }

    public boolean isComplianceRelevant() {
        return complianceRelevant;
    }

    public void setComplianceRelevant(boolean complianceRelevant) {
        this.complianceRelevant = complianceRelevant;
    }

    public boolean isSecurityRelevant() {
        return securityRelevant;
    }

    public void setSecurityRelevant(boolean securityRelevant) {
        this.securityRelevant = securityRelevant;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(RiskLevel riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getAuditCategory() {
        return auditCategory;
    }

    public void setAuditCategory(String auditCategory) {
        this.auditCategory = auditCategory;
    }

    /**
     * Checks if this event represents a high-risk operation.
     */
    public boolean isHighRiskOperation() {
        return riskLevel == RiskLevel.HIGH;
    }

    /**
     * Checks if this event should trigger security alerts.
     */
    public boolean shouldTriggerSecurityAlert() {
        return securityRelevant && (riskLevel == RiskLevel.HIGH || 
                                   actionType == AdminActionType.UNAUTHORIZED_ACCESS_ATTEMPT);
    }

    /**
     * Builder pattern for creating admin audit events.
     */
    public static class Builder {
        private AdminActionType actionType;
        private String adminUserId;
        private String adminUsername;
        private String targetUserId;
        private String targetUsername;
        private String realmId;
        private String realmName;
        private Instant timestamp;
        private String ipAddress;
        private String userAgent;
        private String sessionId;
        private Map<String, Object> details;
        private AdminActionResult result;
        private boolean complianceRelevant;
        private boolean securityRelevant;
        private RiskLevel riskLevel;
        private String auditCategory = "BIOMETRIC_ADMINISTRATION";

        public Builder actionType(AdminActionType actionType) {
            this.actionType = actionType;
            return this;
        }

        public Builder adminUserId(String adminUserId) {
            this.adminUserId = adminUserId;
            return this;
        }

        public Builder adminUsername(String adminUsername) {
            this.adminUsername = adminUsername;
            return this;
        }

        public Builder targetUserId(String targetUserId) {
            this.targetUserId = targetUserId;
            return this;
        }

        public Builder targetUsername(String targetUsername) {
            this.targetUsername = targetUsername;
            return this;
        }

        public Builder realmId(String realmId) {
            this.realmId = realmId;
            return this;
        }

        public Builder realmName(String realmName) {
            this.realmName = realmName;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder ipAddress(String ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }

        public Builder userAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public Builder details(Map<String, Object> details) {
            this.details = details;
            return this;
        }

        public Builder result(AdminActionResult result) {
            this.result = result;
            return this;
        }

        public Builder complianceRelevant(boolean complianceRelevant) {
            this.complianceRelevant = complianceRelevant;
            return this;
        }

        public Builder securityRelevant(boolean securityRelevant) {
            this.securityRelevant = securityRelevant;
            return this;
        }

        public Builder riskLevel(RiskLevel riskLevel) {
            this.riskLevel = riskLevel;
            return this;
        }

        public Builder auditCategory(String auditCategory) {
            this.auditCategory = auditCategory;
            return this;
        }

        public AdminAuditEvent build() {
            return new AdminAuditEvent(this);
        }
    }
}