package com.bioid.keycloak.events;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;

/**
 * Audit event data structure for Face Recognition operations.
 *
 * <p>Provides structured logging for compliance and monitoring purposes.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FaceRecognitionAuditEvent {

  private static final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

  @JsonProperty("event_type")
  private String eventType;

  @JsonProperty("user_id")
  private String userId;

  @JsonProperty("session_id")
  private String sessionId;

  @JsonProperty("ip_address")
  private String ipAddress;

  @JsonProperty("realm_id")
  private String realmId;

  @JsonProperty("client_id")
  private String clientId;

  @JsonProperty("timestamp")
  @JsonFormat(
      shape = JsonFormat.Shape.STRING,
      pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
      timezone = "UTC")
  private Long timestamp;

  @JsonProperty("details")
  private Map<String, String> details;

  @JsonProperty("audit_category")
  private String auditCategory = "BIOMETRIC_AUTHENTICATION";

  @JsonProperty("compliance_relevant")
  private boolean complianceRelevant = true;

  // Constructors
  public FaceRecognitionAuditEvent() {}

  private FaceRecognitionAuditEvent(Builder builder) {
    this.eventType = builder.eventType;
    this.userId = builder.userId;
    this.sessionId = builder.sessionId;
    this.ipAddress = builder.ipAddress;
    this.realmId = builder.realmId;
    this.clientId = builder.clientId;
    this.timestamp = builder.timestamp;
    this.details = builder.details;
    this.auditCategory = builder.auditCategory;
    this.complianceRelevant = builder.complianceRelevant;
  }

  public static Builder builder() {
    return new Builder();
  }

  // Getters and Setters
  public String getEventType() {
    return eventType;
  }

  public void setEventType(String eventType) {
    this.eventType = eventType;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getSessionId() {
    return sessionId;
  }

  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

  public String getIpAddress() {
    return ipAddress;
  }

  public void setIpAddress(String ipAddress) {
    this.ipAddress = ipAddress;
  }

  public String getRealmId() {
    return realmId;
  }

  public void setRealmId(String realmId) {
    this.realmId = realmId;
  }

  public String getClientId() {
    return clientId;
  }

  public void setClientId(String clientId) {
    this.clientId = clientId;
  }

  public Long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Long timestamp) {
    this.timestamp = timestamp;
  }

  public Map<String, String> getDetails() {
    return details;
  }

  public void setDetails(Map<String, String> details) {
    this.details = details;
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

  /** Convert to JSON string for logging. */
  public String toJson() {
    try {
      return objectMapper.writeValueAsString(this);
    } catch (JsonProcessingException e) {
      return String.format(
          "{\"error\":\"Failed to serialize audit event\",\"event_type\":\"%s\",\"user_id\":\"%s\"}",
          eventType, userId);
    }
  }

  /** Get formatted timestamp as Instant. */
  public Instant getTimestampAsInstant() {
    return timestamp != null ? Instant.ofEpochMilli(timestamp) : null;
  }

  @Override
  public String toString() {
    return toJson();
  }

  /** Builder pattern for creating audit events. */
  public static class Builder {
    private String eventType;
    private String userId;
    private String sessionId;
    private String ipAddress;
    private String realmId;
    private String clientId;
    private Long timestamp;
    private Map<String, String> details;
    private String auditCategory = "BIOMETRIC_AUTHENTICATION";
    private boolean complianceRelevant = true;

    public Builder eventType(String eventType) {
      this.eventType = eventType;
      return this;
    }

    public Builder userId(String userId) {
      this.userId = userId;
      return this;
    }

    public Builder sessionId(String sessionId) {
      this.sessionId = sessionId;
      return this;
    }

    public Builder ipAddress(String ipAddress) {
      this.ipAddress = ipAddress;
      return this;
    }

    public Builder realmId(String realmId) {
      this.realmId = realmId;
      return this;
    }

    public Builder clientId(String clientId) {
      this.clientId = clientId;
      return this;
    }

    public Builder timestamp(Long timestamp) {
      this.timestamp = timestamp;
      return this;
    }

    public Builder details(Map<String, String> details) {
      this.details = details;
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

    public FaceRecognitionAuditEvent build() {
      return new FaceRecognitionAuditEvent(this);
    }
  }
}
