package com.bioid.keycloak.client.admin.model;

import java.time.Instant;
import java.util.List;

/**
 * Result of a liveness detection test operation.
 */
public class LivenessTestResult {
    private final boolean success;
    private final boolean live;
    private final double livenessScore;
    private final LivenessConfiguration.LivenessMode mode;
    private final String errorMessage;
    private final List<String> warnings;
    private final Instant testedAt;
    private final long processingTimeMs;

    public LivenessTestResult(boolean success, boolean live, double livenessScore,
                            LivenessConfiguration.LivenessMode mode, String errorMessage,
                            List<String> warnings, Instant testedAt, long processingTimeMs) {
        this.success = success;
        this.live = live;
        this.livenessScore = livenessScore;
        this.mode = mode;
        this.errorMessage = errorMessage;
        this.warnings = warnings;
        this.testedAt = testedAt;
        this.processingTimeMs = processingTimeMs;
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isLive() {
        return live;
    }

    public double getLivenessScore() {
        return livenessScore;
    }

    public LivenessConfiguration.LivenessMode getMode() {
        return mode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public Instant getTestedAt() {
        return testedAt;
    }

    public long getProcessingTimeMs() {
        return processingTimeMs;
    }

    public static LivenessTestResult success(boolean live, double score, LivenessConfiguration.LivenessMode mode, 
                                           List<String> warnings, long processingTimeMs) {
        return new LivenessTestResult(true, live, score, mode, null, warnings, Instant.now(), processingTimeMs);
    }

    public static LivenessTestResult failure(LivenessConfiguration.LivenessMode mode, String errorMessage, 
                                           long processingTimeMs) {
        return new LivenessTestResult(false, false, 0.0, mode, errorMessage, List.of(), Instant.now(), processingTimeMs);
    }
}