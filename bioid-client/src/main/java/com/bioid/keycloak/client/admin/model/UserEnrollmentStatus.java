package com.bioid.keycloak.client.admin.model;

import java.time.Instant;
import java.util.List;

/**
 * Represents the enrollment status and metadata for a user's biometric template.
 */
public class UserEnrollmentStatus {
    private final String userId;
    private final String username;
    private final String email;
    private final boolean enrolled;
    private final Instant enrolledAt;
    private final Instant lastAuthentication;
    private final int encoderVersion;
    private final int featureVectors;
    private final List<String> tags;
    private final TemplateHealthStatus healthStatus;

    public UserEnrollmentStatus(String userId, String username, String email, boolean enrolled,
                               Instant enrolledAt, Instant lastAuthentication, int encoderVersion,
                               int featureVectors, List<String> tags, TemplateHealthStatus healthStatus) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.enrolled = enrolled;
        this.enrolledAt = enrolledAt;
        this.lastAuthentication = lastAuthentication;
        this.encoderVersion = encoderVersion;
        this.featureVectors = featureVectors;
        this.tags = tags;
        this.healthStatus = healthStatus;
    }

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public boolean isEnrolled() {
        return enrolled;
    }

    public Instant getEnrolledAt() {
        return enrolledAt;
    }

    public Instant getLastAuthentication() {
        return lastAuthentication;
    }

    public int getEncoderVersion() {
        return encoderVersion;
    }

    public int getFeatureVectors() {
        return featureVectors;
    }

    public List<String> getTags() {
        return tags;
    }

    public TemplateHealthStatus getHealthStatus() {
        return healthStatus;
    }

    public enum TemplateHealthStatus {
        HEALTHY,
        OUTDATED_ENCODER,
        MISSING_THUMBNAILS,
        EXPIRING_SOON,
        CORRUPTED,
        SYNC_MISMATCH
    }
}