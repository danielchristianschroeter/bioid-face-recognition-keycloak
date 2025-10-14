package com.bioid.keycloak.client.admin.model;

import java.util.List;

/**
 * Configuration settings for liveness detection in a realm.
 */
public class LivenessConfiguration {
    private final LivenessMode defaultLivenessMode;
    private final double livenessThreshold;
    private final boolean enableChallengeResponse;
    private final List<ChallengeDirection> allowedChallengeDirections;
    private final int maxRetryAttempts;
    private final boolean enableFallbackToPassive;

    public LivenessConfiguration(LivenessMode defaultLivenessMode, double livenessThreshold,
                               boolean enableChallengeResponse, List<ChallengeDirection> allowedChallengeDirections,
                               int maxRetryAttempts, boolean enableFallbackToPassive) {
        this.defaultLivenessMode = defaultLivenessMode;
        this.livenessThreshold = livenessThreshold;
        this.enableChallengeResponse = enableChallengeResponse;
        this.allowedChallengeDirections = allowedChallengeDirections;
        this.maxRetryAttempts = maxRetryAttempts;
        this.enableFallbackToPassive = enableFallbackToPassive;
    }

    public LivenessMode getDefaultLivenessMode() {
        return defaultLivenessMode;
    }

    public double getLivenessThreshold() {
        return livenessThreshold;
    }

    public boolean isEnableChallengeResponse() {
        return enableChallengeResponse;
    }

    public List<ChallengeDirection> getAllowedChallengeDirections() {
        return allowedChallengeDirections;
    }

    public int getMaxRetryAttempts() {
        return maxRetryAttempts;
    }

    public boolean isEnableFallbackToPassive() {
        return enableFallbackToPassive;
    }

    public enum LivenessMode {
        PASSIVE,           // Single image texture analysis
        ACTIVE,            // Two images with motion detection
        CHALLENGE_RESPONSE // Two images with head movement validation
    }

    public enum ChallengeDirection {
        UP, DOWN, LEFT, RIGHT
    }

    public static LivenessConfiguration getDefault() {
        return new LivenessConfiguration(
            LivenessMode.PASSIVE,
            0.7,
            false,
            List.of(ChallengeDirection.UP, ChallengeDirection.DOWN, 
                   ChallengeDirection.LEFT, ChallengeDirection.RIGHT),
            3,
            true
        );
    }
}