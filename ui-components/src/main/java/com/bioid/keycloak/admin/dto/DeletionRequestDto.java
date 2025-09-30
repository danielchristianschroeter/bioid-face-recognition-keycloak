package com.bioid.keycloak.admin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/**
 * DTO for biometric template deletion requests. Used for GDPR compliance and user privacy
 * management.
 */
public class DeletionRequestDto {

  @JsonProperty("id")
  private String id;

  @JsonProperty("userId")
  private String userId;

  @JsonProperty("username")
  private String username;

  @JsonProperty("email")
  private String email;

  @JsonProperty("classId")
  private Long classId;

  @JsonProperty("requestedAt")
  @JsonFormat(
      shape = JsonFormat.Shape.STRING,
      pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
      timezone = "UTC")
  private Instant requestedAt;

  @JsonProperty("status")
  private DeletionRequestStatus status;

  @JsonProperty("reason")
  private String reason;

  @JsonProperty("processedAt")
  @JsonFormat(
      shape = JsonFormat.Shape.STRING,
      pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
      timezone = "UTC")
  private Instant processedAt;

  @JsonProperty("processedBy")
  private String processedBy;

  @JsonProperty("adminNotes")
  private String adminNotes;

  @JsonProperty("priority")
  private DeletionRequestPriority priority;

  @JsonProperty("templateExists")
  private Boolean templateExists;

  @JsonProperty("lastVerificationAt")
  @JsonFormat(
      shape = JsonFormat.Shape.STRING,
      pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
      timezone = "UTC")
  private Instant lastVerificationAt;

  // Constructors
  public DeletionRequestDto() {}

  public DeletionRequestDto(
      String id,
      String userId,
      String username,
      Long classId,
      Instant requestedAt,
      DeletionRequestStatus status) {
    this.id = id;
    this.userId = userId;
    this.username = username;
    this.classId = classId;
    this.requestedAt = requestedAt;
    this.status = status;
    this.priority = DeletionRequestPriority.NORMAL;
  }

  // Getters and Setters
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public Long getClassId() {
    return classId;
  }

  public void setClassId(Long classId) {
    this.classId = classId;
  }

  public Instant getRequestedAt() {
    return requestedAt;
  }

  public void setRequestedAt(Instant requestedAt) {
    this.requestedAt = requestedAt;
  }

  public DeletionRequestStatus getStatus() {
    return status;
  }

  public void setStatus(DeletionRequestStatus status) {
    this.status = status;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  public Instant getProcessedAt() {
    return processedAt;
  }

  public void setProcessedAt(Instant processedAt) {
    this.processedAt = processedAt;
  }

  public String getProcessedBy() {
    return processedBy;
  }

  public void setProcessedBy(String processedBy) {
    this.processedBy = processedBy;
  }

  public String getAdminNotes() {
    return adminNotes;
  }

  public void setAdminNotes(String adminNotes) {
    this.adminNotes = adminNotes;
  }

  public DeletionRequestPriority getPriority() {
    return priority;
  }

  public void setPriority(DeletionRequestPriority priority) {
    this.priority = priority;
  }

  public Boolean getTemplateExists() {
    return templateExists;
  }

  public void setTemplateExists(Boolean templateExists) {
    this.templateExists = templateExists;
  }

  public Instant getLastVerificationAt() {
    return lastVerificationAt;
  }

  public void setLastVerificationAt(Instant lastVerificationAt) {
    this.lastVerificationAt = lastVerificationAt;
  }

  // Helper methods
  public boolean isPending() {
    return status == DeletionRequestStatus.PENDING;
  }

  public boolean isProcessed() {
    return status == DeletionRequestStatus.APPROVED || status == DeletionRequestStatus.DECLINED;
  }

  public boolean isHighPriority() {
    return priority == DeletionRequestPriority.HIGH || priority == DeletionRequestPriority.URGENT;
  }

  public long getDaysOld() {
    return java.time.Duration.between(requestedAt, Instant.now()).toDays();
  }

  @Override
  public String toString() {
    return "DeletionRequestDto{"
        + "id='"
        + id
        + '\''
        + ", userId='"
        + userId
        + '\''
        + ", username='"
        + username
        + '\''
        + ", status="
        + status
        + ", requestedAt="
        + requestedAt
        + ", priority="
        + priority
        + '}';
  }
}
