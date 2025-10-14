package com.bioid.keycloak.admin.dto;

import java.time.Instant;
import java.util.List;

/**
 * DTO representing a biometric template for admin UI.
 */
public class TemplateDto {
    private long classId;
    private String username;
    private String email;
    private String healthStatus;
    private int encoderVersion;
    private int featureVectors;
    private Instant enrolledAt;
    private Instant lastAuthentication;
    private Instant expiresAt;
    private boolean needsUpgrade;
    private List<String> tags;

    public TemplateDto() {}

    private TemplateDto(Builder builder) {
        this.classId = builder.classId;
        this.username = builder.username;
        this.email = builder.email;
        this.healthStatus = builder.healthStatus;
        this.encoderVersion = builder.encoderVersion;
        this.featureVectors = builder.featureVectors;
        this.enrolledAt = builder.enrolledAt;
        this.lastAuthentication = builder.lastAuthentication;
        this.expiresAt = builder.expiresAt;
        this.needsUpgrade = builder.needsUpgrade;
        this.tags = builder.tags;
    }

    public static Builder builder() {
        return new Builder();
    }

    public long getClassId() {
        return classId;
    }

    public void setClassId(long classId) {
        this.classId = classId;
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

    public String getHealthStatus() {
        return healthStatus;
    }

    public void setHealthStatus(String healthStatus) {
        this.healthStatus = healthStatus;
    }

    public int getEncoderVersion() {
        return encoderVersion;
    }

    public void setEncoderVersion(int encoderVersion) {
        this.encoderVersion = encoderVersion;
    }

    public int getFeatureVectors() {
        return featureVectors;
    }

    public void setFeatureVectors(int featureVectors) {
        this.featureVectors = featureVectors;
    }

    public Instant getEnrolledAt() {
        return enrolledAt;
    }

    public void setEnrolledAt(Instant enrolledAt) {
        this.enrolledAt = enrolledAt;
    }

    public Instant getLastAuthentication() {
        return lastAuthentication;
    }

    public void setLastAuthentication(Instant lastAuthentication) {
        this.lastAuthentication = lastAuthentication;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isNeedsUpgrade() {
        return needsUpgrade;
    }

    public void setNeedsUpgrade(boolean needsUpgrade) {
        this.needsUpgrade = needsUpgrade;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public static class Builder {
        private long classId;
        private String username;
        private String email;
        private String healthStatus;
        private int encoderVersion;
        private int featureVectors;
        private Instant enrolledAt;
        private Instant lastAuthentication;
        private Instant expiresAt;
        private boolean needsUpgrade;
        private List<String> tags;

        public Builder classId(long classId) {
            this.classId = classId;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder healthStatus(String healthStatus) {
            this.healthStatus = healthStatus;
            return this;
        }

        public Builder encoderVersion(int encoderVersion) {
            this.encoderVersion = encoderVersion;
            return this;
        }

        public Builder featureVectors(int featureVectors) {
            this.featureVectors = featureVectors;
            return this;
        }

        public Builder enrolledAt(Instant enrolledAt) {
            this.enrolledAt = enrolledAt;
            return this;
        }

        public Builder lastAuthentication(Instant lastAuthentication) {
            this.lastAuthentication = lastAuthentication;
            return this;
        }

        public Builder expiresAt(Instant expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        public Builder needsUpgrade(boolean needsUpgrade) {
            this.needsUpgrade = needsUpgrade;
            return this;
        }

        public Builder tags(List<String> tags) {
            this.tags = tags;
            return this;
        }

        public TemplateDto build() {
            return new TemplateDto(this);
        }
    }
}