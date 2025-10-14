package com.bioid.keycloak.admin.dto;

import java.util.List;

/**
 * DTO for liveness detection configuration.
 */
public class LivenessConfigDto {
    private String defaultLivenessMode;
    private double livenessThreshold;
    private List<String> challengeDirections;
    private int maxOverheadMs;
    private boolean fallbackEnabled;
    private boolean detailedLogging;

    public LivenessConfigDto() {}

    private LivenessConfigDto(Builder builder) {
        this.defaultLivenessMode = builder.defaultLivenessMode;
        this.livenessThreshold = builder.livenessThreshold;
        this.challengeDirections = builder.challengeDirections;
        this.maxOverheadMs = builder.maxOverheadMs;
        this.fallbackEnabled = builder.fallbackEnabled;
        this.detailedLogging = builder.detailedLogging;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getDefaultLivenessMode() {
        return defaultLivenessMode;
    }

    public void setDefaultLivenessMode(String defaultLivenessMode) {
        this.defaultLivenessMode = defaultLivenessMode;
    }

    public double getLivenessThreshold() {
        return livenessThreshold;
    }

    public void setLivenessThreshold(double livenessThreshold) {
        this.livenessThreshold = livenessThreshold;
    }

    public List<String> getChallengeDirections() {
        return challengeDirections;
    }

    public void setChallengeDirections(List<String> challengeDirections) {
        this.challengeDirections = challengeDirections;
    }

    public int getMaxOverheadMs() {
        return maxOverheadMs;
    }

    public void setMaxOverheadMs(int maxOverheadMs) {
        this.maxOverheadMs = maxOverheadMs;
    }

    public boolean isFallbackEnabled() {
        return fallbackEnabled;
    }

    public void setFallbackEnabled(boolean fallbackEnabled) {
        this.fallbackEnabled = fallbackEnabled;
    }

    public boolean isDetailedLogging() {
        return detailedLogging;
    }

    public void setDetailedLogging(boolean detailedLogging) {
        this.detailedLogging = detailedLogging;
    }

    public static class Builder {
        private String defaultLivenessMode;
        private double livenessThreshold;
        private List<String> challengeDirections;
        private int maxOverheadMs;
        private boolean fallbackEnabled;
        private boolean detailedLogging;

        public Builder defaultLivenessMode(String defaultLivenessMode) {
            this.defaultLivenessMode = defaultLivenessMode;
            return this;
        }

        public Builder livenessThreshold(double livenessThreshold) {
            this.livenessThreshold = livenessThreshold;
            return this;
        }

        public Builder challengeDirections(List<String> challengeDirections) {
            this.challengeDirections = challengeDirections;
            return this;
        }

        public Builder maxOverheadMs(int maxOverheadMs) {
            this.maxOverheadMs = maxOverheadMs;
            return this;
        }

        public Builder fallbackEnabled(boolean fallbackEnabled) {
            this.fallbackEnabled = fallbackEnabled;
            return this;
        }

        public Builder detailedLogging(boolean detailedLogging) {
            this.detailedLogging = detailedLogging;
            return this;
        }

        public LivenessConfigDto build() {
            return new LivenessConfigDto(this);
        }
    }
}