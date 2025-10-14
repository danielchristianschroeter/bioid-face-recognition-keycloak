package com.bioid.keycloak.client.admin.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Comprehensive health report for templates in a realm.
 */
public class TemplateHealthReport {
    private final int totalTemplates;
    private final int healthyTemplates;
    private final int outdatedEncoderVersions;
    private final int missingThumbnails;
    private final int expiringSoon;
    private final List<TemplateIssue> issues;
    private final Map<String, Integer> issuesByType;
    private final Instant generatedAt;

    public TemplateHealthReport(int totalTemplates, int healthyTemplates, int outdatedEncoderVersions,
                              int missingThumbnails, int expiringSoon, List<TemplateIssue> issues,
                              Map<String, Integer> issuesByType, Instant generatedAt) {
        this.totalTemplates = totalTemplates;
        this.healthyTemplates = healthyTemplates;
        this.outdatedEncoderVersions = outdatedEncoderVersions;
        this.missingThumbnails = missingThumbnails;
        this.expiringSoon = expiringSoon;
        this.issues = issues;
        this.issuesByType = issuesByType;
        this.generatedAt = generatedAt;
    }

    public int getTotalTemplates() {
        return totalTemplates;
    }

    public int getHealthyTemplates() {
        return healthyTemplates;
    }

    public int getOutdatedEncoderVersions() {
        return outdatedEncoderVersions;
    }

    public int getMissingThumbnails() {
        return missingThumbnails;
    }

    public int getExpiringSoon() {
        return expiringSoon;
    }

    public List<TemplateIssue> getIssues() {
        return issues;
    }

    public Map<String, Integer> getIssuesByType() {
        return issuesByType;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }

    public double getHealthPercentage() {
        return totalTemplates > 0 ? (double) healthyTemplates / totalTemplates * 100.0 : 0.0;
    }

    public static class TemplateIssue {
        private final long classId;
        private final String userId;
        private final IssueType type;
        private final IssueSeverity severity;
        private final String description;
        private final List<String> recommendedActions;
        private final Instant detectedAt;

        public TemplateIssue(long classId, String userId, IssueType type, IssueSeverity severity,
                           String description, List<String> recommendedActions, Instant detectedAt) {
            this.classId = classId;
            this.userId = userId;
            this.type = type;
            this.severity = severity;
            this.description = description;
            this.recommendedActions = recommendedActions;
            this.detectedAt = detectedAt;
        }

        public long getClassId() {
            return classId;
        }

        public String getUserId() {
            return userId;
        }

        public IssueType getType() {
            return type;
        }

        public IssueSeverity getSeverity() {
            return severity;
        }

        public String getDescription() {
            return description;
        }

        public List<String> getRecommendedActions() {
            return recommendedActions;
        }

        public Instant getDetectedAt() {
            return detectedAt;
        }
    }

    public enum IssueType {
        OUTDATED_ENCODER,
        MISSING_THUMBNAILS,
        EXPIRING_TEMPLATE,
        CORRUPTED_METADATA,
        ORPHANED_CREDENTIAL,
        SYNC_MISMATCH
    }

    public enum IssueSeverity {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }
}