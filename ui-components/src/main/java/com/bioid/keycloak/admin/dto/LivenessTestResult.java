package com.bioid.keycloak.admin.dto;

/**
 * DTO for liveness detection test results.
 */
public class LivenessTestResult {
    private boolean live;
    private double livenessScore;
    private int processingTime;
    private String rejectionReason;

    public LivenessTestResult() {}

    private LivenessTestResult(Builder builder) {
        this.live = builder.live;
        this.livenessScore = builder.livenessScore;
        this.processingTime = builder.processingTime;
        this.rejectionReason = builder.rejectionReason;
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isLive() {
        return live;
    }

    public void setLive(boolean live) {
        this.live = live;
    }

    public double getLivenessScore() {
        return livenessScore;
    }

    public void setLivenessScore(double livenessScore) {
        this.livenessScore = livenessScore;
    }

    public int getProcessingTime() {
        return processingTime;
    }

    public void setProcessingTime(int processingTime) {
        this.processingTime = processingTime;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public static class Builder {
        private boolean live;
        private double livenessScore;
        private int processingTime;
        private String rejectionReason;

        public Builder live(boolean live) {
            this.live = live;
            return this;
        }

        public Builder livenessScore(double livenessScore) {
            this.livenessScore = livenessScore;
            return this;
        }

        public Builder processingTime(int processingTime) {
            this.processingTime = processingTime;
            return this;
        }

        public Builder rejectionReason(String rejectionReason) {
            this.rejectionReason = rejectionReason;
            return this;
        }

        public LivenessTestResult build() {
            return new LivenessTestResult(this);
        }
    }
}