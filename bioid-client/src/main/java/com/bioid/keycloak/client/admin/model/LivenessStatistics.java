package com.bioid.keycloak.client.admin.model;

import java.time.LocalDate;
import java.util.Map;

/**
 * Statistical data for liveness detection operations over a time period.
 */
public class LivenessStatistics {
    private final LocalDate fromDate;
    private final LocalDate toDate;
    private final long totalAttempts;
    private final long successfulAttempts;
    private final long liveDetections;
    private final double successRate;
    private final double liveDetectionRate;
    private final double averageLivenessScore;
    private final Map<LivenessConfiguration.LivenessMode, Long> attemptsByMode;
    private final Map<String, Long> rejectionReasons;
    private final Map<String, Double> averageScoresByMode;

    public LivenessStatistics(LocalDate fromDate, LocalDate toDate, long totalAttempts,
                            long successfulAttempts, long liveDetections, double successRate,
                            double liveDetectionRate, double averageLivenessScore,
                            Map<LivenessConfiguration.LivenessMode, Long> attemptsByMode,
                            Map<String, Long> rejectionReasons,
                            Map<String, Double> averageScoresByMode) {
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.totalAttempts = totalAttempts;
        this.successfulAttempts = successfulAttempts;
        this.liveDetections = liveDetections;
        this.successRate = successRate;
        this.liveDetectionRate = liveDetectionRate;
        this.averageLivenessScore = averageLivenessScore;
        this.attemptsByMode = attemptsByMode;
        this.rejectionReasons = rejectionReasons;
        this.averageScoresByMode = averageScoresByMode;
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public LocalDate getToDate() {
        return toDate;
    }

    public long getTotalAttempts() {
        return totalAttempts;
    }

    public long getSuccessfulAttempts() {
        return successfulAttempts;
    }

    public long getLiveDetections() {
        return liveDetections;
    }

    public double getSuccessRate() {
        return successRate;
    }

    public double getLiveDetectionRate() {
        return liveDetectionRate;
    }

    public double getAverageLivenessScore() {
        return averageLivenessScore;
    }

    public Map<LivenessConfiguration.LivenessMode, Long> getAttemptsByMode() {
        return attemptsByMode;
    }

    public Map<String, Long> getRejectionReasons() {
        return rejectionReasons;
    }

    public Map<String, Double> getAverageScoresByMode() {
        return averageScoresByMode;
    }

    public long getFailedAttempts() {
        return totalAttempts - successfulAttempts;
    }

    public double getFailureRate() {
        return totalAttempts > 0 ? (double) getFailedAttempts() / totalAttempts * 100.0 : 0.0;
    }
}