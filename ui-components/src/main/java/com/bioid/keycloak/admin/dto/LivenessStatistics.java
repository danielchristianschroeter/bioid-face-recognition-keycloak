package com.bioid.keycloak.admin.dto;

import java.util.List;
import java.util.Map;

/**
 * DTO for liveness detection statistics displayed on the dashboard.
 */
public class LivenessStatistics {
    private int totalChecks;
    private double passRate;
    private double avgScore;
    private int avgTime;
    private Map<String, Integer> modeDistribution;
    private List<RejectionReason> rejectionReasons;

    public LivenessStatistics() {}

    private LivenessStatistics(Builder builder) {
        this.totalChecks = builder.totalChecks;
        this.passRate = builder.passRate;
        this.avgScore = builder.avgScore;
        this.avgTime = builder.avgTime;
        this.modeDistribution = builder.modeDistribution;
        this.rejectionReasons = builder.rejectionReasons;
    }

    public static Builder builder() {
        return new Builder();
    }

    public int getTotalChecks() {
        return totalChecks;
    }

    public void setTotalChecks(int totalChecks) {
        this.totalChecks = totalChecks;
    }

    public double getPassRate() {
        return passRate;
    }

    public void setPassRate(double passRate) {
        this.passRate = passRate;
    }

    public double getAvgScore() {
        return avgScore;
    }

    public void setAvgScore(double avgScore) {
        this.avgScore = avgScore;
    }

    public int getAvgTime() {
        return avgTime;
    }

    public void setAvgTime(int avgTime) {
        this.avgTime = avgTime;
    }

    public Map<String, Integer> getModeDistribution() {
        return modeDistribution;
    }

    public void setModeDistribution(Map<String, Integer> modeDistribution) {
        this.modeDistribution = modeDistribution;
    }

    public List<RejectionReason> getRejectionReasons() {
        return rejectionReasons;
    }

    public void setRejectionReasons(List<RejectionReason> rejectionReasons) {
        this.rejectionReasons = rejectionReasons;
    }

    public static class Builder {
        private int totalChecks;
        private double passRate;
        private double avgScore;
        private int avgTime;
        private Map<String, Integer> modeDistribution;
        private List<RejectionReason> rejectionReasons;

        public Builder totalChecks(int totalChecks) {
            this.totalChecks = totalChecks;
            return this;
        }

        public Builder passRate(double passRate) {
            this.passRate = passRate;
            return this;
        }

        public Builder avgScore(double avgScore) {
            this.avgScore = avgScore;
            return this;
        }

        public Builder avgTime(int avgTime) {
            this.avgTime = avgTime;
            return this;
        }

        public Builder modeDistribution(Map<String, Integer> modeDistribution) {
            this.modeDistribution = modeDistribution;
            return this;
        }

        public Builder rejectionReasons(List<RejectionReason> rejectionReasons) {
            this.rejectionReasons = rejectionReasons;
            return this;
        }

        public LivenessStatistics build() {
            return new LivenessStatistics(this);
        }
    }

    public static class RejectionReason {
        private String reason;
        private int count;

        public RejectionReason() {}

        public RejectionReason(String reason, int count) {
            this.reason = reason;
            this.count = count;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }
    }
}