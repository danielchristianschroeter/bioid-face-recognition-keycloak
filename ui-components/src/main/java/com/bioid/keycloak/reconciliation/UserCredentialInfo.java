package com.bioid.keycloak.reconciliation;

import java.time.Instant;

/**
 * Information about a user's face authentication credential in Keycloak
 */
public class UserCredentialInfo {
    
    private String userId;
    private String username;
    private String credentialId;
    private Long classId;
    private Long createdDate;
    private String credentialData;
    private String secretData;
    private boolean active = true;

    // Getters and setters
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

    public String getCredentialId() {
        return credentialId;
    }

    public void setCredentialId(String credentialId) {
        this.credentialId = credentialId;
    }

    public Long getClassId() {
        return classId;
    }

    public void setClassId(Long classId) {
        this.classId = classId;
    }

    public Long getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Long createdDate) {
        this.createdDate = createdDate;
    }

    public String getCredentialData() {
        return credentialData;
    }

    public void setCredentialData(String credentialData) {
        this.credentialData = credentialData;
    }

    public String getSecretData() {
        return secretData;
    }

    public void setSecretData(String secretData) {
        this.secretData = secretData;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    // Utility methods
    public Instant getCreatedInstant() {
        return createdDate != null ? Instant.ofEpochMilli(createdDate) : null;
    }

    public boolean hasClassId() {
        return classId != null;
    }

    @Override
    public String toString() {
        return String.format("UserCredentialInfo{userId='%s', username='%s', credentialId='%s', classId=%d, active=%s}", 
            userId, username, credentialId, classId, active);
    }
}