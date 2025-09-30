package com.bioid.keycloak.events;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;

/**
 * Admin audit event data structure for Face Recognition administrative operations.
 *
 * <p>Provides structured logging for administrative actions and compliance monitoring.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FaceRecognitionAdminAuditEvent {

  private static final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

  @JsonProperty("operation_type")
  private String operationType;

  @JsonProperty("resource_type")
  private String resourceType;

  @JsonProperty("resource_path")
  private String resourcePath;

  @JsonProperty("admin_user_id")
  private String adminUserId;

  @JsonProperty("admin_username")
  private String adminUsername;

  @JsonProperty("realm_id")
  private String realmId;

  @JsonProperty("timestamp")
  @JsonFormat(
      shape = JsonFormat.Shape.STRING,
      pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
      timezone = "UTC")
  private Long timestamp;

  @JsonProperty("ip_address")
  private String ipAddress;

  @JsonProperty("user_agent")
  private String userAgent;

  @JsonProperty("representation")
  private String representation;

  @JsonProperty("audit_category")
  private String auditCategory = "BIOMETRIC_ADMINISTRATION";

  @JsonProperty("compliance_relevant")
  private boolean complianceRelevant = true;

  @JsonProperty("security_relevant")
  private boolean securityRelevant = true;

  // Constructors
  public FaceRecognitionAdminAuditEvent() {}

  private FaceRecognitionAdminAuditEvent(Builder builder) {
    this.operationType = builder.operationType;
    this.resourceType = builder.resourceType;
    this.resourcePath = builder.resourcePath;
    this.adminUserId = builder.adminUserId;
    this.adminUsername = builder.adminUsername;
    this.realmId = builder.realmId;
    this.timestamp = builder.timestamp;
    this.ipAddress = builder.ipAddress;
    this.userAgent = builder.userAgent;
    this.representation = builder.representation;
    this.auditCategory = builder.auditCategory;
    this.complianceRelevant = builder.complianceRelevant;
    this.securityRelevant = builder.securityRelevant;
  }

  public static Builder builder() {
    return new Builder();
  }

  // Getters and Setters
  public String getOperationType() {
    return operationType;
  }

  public void setOperationType(String operationType) {
    this.operationType = operationType;
  }

  public String getResourceType() {
    return resourceType;
  }

  public void setResourceType(String resourceType) {
    this.resourceType = resourceType;
  }

  public String getResourcePath() {
    return resourcePath;
  }

  public void setResourcePath(String resourcePath) {
    this.resourcePath = resourcePath;
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

  public String getRealmId() {
    return realmId;
  }

  public void setRealmId(String realmId) {
    this.realmId = realmId;
  }

  public Long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Long timestamp) {
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

  public String getRepresentation() {
    return representation;
  }

  public void setRepresentation(String representation) {
    this.representation = representation;
  }

  public String getAuditCategory() {
    return auditCategory;
  }

  public void setAuditCategory(String auditCategory) {
    this.auditCategory = auditCategory;
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

  /** Convert to JSON string for logging. */
  public String toJson() {
    try {
      return objectMapper.writeValueAsString(this);
    } catch (JsonProcessingException e) {
      return String.format(
          "{\"error\":\"Failed to serialize admin audit event\",\"operation_type\":\"%s\",\"admin_user_id\":\"%s\"}",
          operationType, adminUserId);
    }
  }

  /** Get formatted timestamp as Instant. */
  public Instant getTimestampAsInstant() {
    return timestamp != null ? Instant.ofEpochMilli(timestamp) : null;
  }

  /** Check if this is a high-risk operation. */
  public boolean isHighRiskOperation() {
    return "DELETE".equals(operationType)
        || (resourcePath != null
            && (resourcePath.contains("deletion-requests") || resourcePath.contains("config")));
  }

  @Override
  public String toString() {
    return toJson();
  }

  /** Builder pattern for creating admin audit events. */
  public static class Builder {
    private String operationType;
    private String resourceType;
    private String resourcePath;
    private String adminUserId;
    private String adminUsername;
    private String realmId;
    private Long timestamp;
    private String ipAddress;
    private String userAgent;
    private String representation;
    private String auditCategory = "BIOMETRIC_ADMINISTRATION";
    private boolean complianceRelevant = true;
    private boolean securityRelevant = true;

    public Builder operationType(String operationType) {
      this.operationType = operationType;
      return this;
    }

    public Builder resourceType(String resourceType) {
      this.resourceType = resourceType;
      return this;
    }

    public Builder resourcePath(String resourcePath) {
      this.resourcePath = resourcePath;
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

    public Builder realmId(String realmId) {
      this.realmId = realmId;
      return this;
    }

    public Builder timestamp(Long timestamp) {
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

    public Builder representation(String representation) {
      this.representation = representation;
      return this;
    }

    public Builder auditCategory(String auditCategory) {
      this.auditCategory = auditCategory;
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

    public FaceRecognitionAdminAuditEvent build() {
      return new FaceRecognitionAdminAuditEvent(this);
    }
  }
}
