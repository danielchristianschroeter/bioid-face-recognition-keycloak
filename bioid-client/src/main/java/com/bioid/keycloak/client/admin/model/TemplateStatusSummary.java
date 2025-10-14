package com.bioid.keycloak.client.admin.model;

import java.time.Instant;
import java.util.List;

/**
 * Summary of template status information for batch operations.
 */
public class TemplateStatusSummary {
    private final long classId;
    private final boolean available;
    private final Instant enrollmentDate;
    private final int encoderVersion;
    private final int featureVectors;
    private final int thumbnailsStored;
    private final List<String> tags;
    private final TemplateHealthStatus healthStatus;

    public TemplateStatusSummary(long classId, boolean available, Instant enrollmentDate,
                               int encoderVersion, int featureVectors, int thumbnailsStored,
                               List<String> tags, TemplateHealthStatus healthStatus) {
        this.classId = classId;
        this.available = available;
        this.enrollmentDate = enrollmentDate;
        this.encoderVersion = encoderVersion;
        this.featureVectors = featureVectors;
        this.thumbnailsStored = thumbnailsStored;
        this.tags = tags;
        this.healthStatus = healthStatus;
    }

    public long getClassId() {
        return classId;
    }

    public boolean isAvailable() {
        return available;
    }

    public Instant getEnrollmentDate() {
        return enrollmentDate;
    }

    public int getEncoderVersion() {
        return encoderVersion;
    }

    public int getFeatureVectors() {
        return featureVectors;
    }

    public int getThumbnailsStored() {
        return thumbnailsStored;
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