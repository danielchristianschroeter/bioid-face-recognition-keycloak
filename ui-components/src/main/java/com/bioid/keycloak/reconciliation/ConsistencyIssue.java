package com.bioid.keycloak.reconciliation;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a data consistency issue between Keycloak and BioID systems
 */
public class ConsistencyIssue {
    
    private String issueId;
    private ConsistencyIssueType type;
    private IssueSeverity severity;
    private String userId;
    private String username;
    private Long classId;
    private String credentialId;
    private String description;
    private String detailedDescription;
    private Instant detectedAt;
    private Map<String, Object> metadata = new HashMap<>();
    private boolean resolved = false;
    private Instant resolvedAt;
    private String resolvedBy;
    private String resolutionNotes;

    public ConsistencyIssue() {
        this.detectedAt = Instant.now();
    }

    // Getters and setters
    public String getIssueId() {
        return issueId;
    }

    public void setIssueId(String issueId) {
        this.issueId = issueId;
    }

    public ConsistencyIssueType getType() {
        return type;
    }

    public void setType(ConsistencyIssueType type) {
        this.type = type;
    }

    public IssueSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(IssueSeverity severity) {
        this.severity = severity;
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

    public Long getClassId() {
        return classId;
    }

    public void setClassId(Long classId) {
        this.classId = classId;
    }

    public String getCredentialId() {
        return credentialId;
    }

    public void setCredentialId(String credentialId) {
        this.credentialId = credentialId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDetailedDescription() {
        return detailedDescription;
    }

    public void setDetailedDescription(String detailedDescription) {
        this.detailedDescription = detailedDescription;
    }

    public Instant getDetectedAt() {
        return detectedAt;
    }

    public void setDetectedAt(Instant detectedAt) {
        this.detectedAt = detectedAt;
    }

    public Map<String, Object> getMetadata() {
        return new HashMap<>(metadata);
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = new HashMap<>(metadata);
    }

    public void addMetadata(String key, Object value) {
        this.metadata.put(key, value);
    }

    public boolean isResolved() {
        return resolved;
    }

    public void setResolved(boolean resolved) {
        this.resolved = resolved;
        if (resolved && resolvedAt == null) {
            this.resolvedAt = Instant.now();
        }
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public String getResolvedBy() {
        return resolvedBy;
    }

    public void setResolvedBy(String resolvedBy) {
        this.resolvedBy = resolvedBy;
    }

    public String getResolutionNotes() {
        return resolutionNotes;
    }

    public void setResolutionNotes(String resolutionNotes) {
        this.resolutionNotes = resolutionNotes;
    }

    // Utility methods
    public boolean isCritical() {
        return severity == IssueSeverity.CRITICAL;
    }

    public boolean isHighSeverity() {
        return severity == IssueSeverity.HIGH;
    }

    public boolean requiresImmediateAttention() {
        return severity == IssueSeverity.CRITICAL || severity == IssueSeverity.HIGH;
    }

    public String getSeverityDisplayName() {
        return severity != null ? severity.getDisplayName() : "Unknown";
    }

    public String getTypeDisplayName() {
        return type != null ? type.getDisplayName() : "Unknown";
    }

    @Override
    public String toString() {
        return String.format("ConsistencyIssue{issueId='%s', type=%s, severity=%s, userId='%s', classId=%d, description='%s'}", 
            issueId, type, severity, userId, classId, description);
    }
}