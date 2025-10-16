package com.bioid.keycloak.client.liveness;

import com.bioid.keycloak.client.auth.BioIdJwtTokenProvider;
import com.bioid.keycloak.client.config.BioIdClientConfig;
import com.bioid.keycloak.client.connection.BioIdConnectionManager;
import com.bioid.keycloak.client.exception.BioIdException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

/**
 * Client for performing liveness detection operations using the BioID Web Service.
 * 
 * This client supports three liveness detection modes:
 * - PASSIVE: Single image texture analysis
 * - ACTIVE: Two images with motion detection
 * - CHALLENGE_RESPONSE: Two images with specific head movement validation
 */
public class LivenessDetectionClient {

    private final BioIdClientConfig config;
    private final BioIdJwtTokenProvider tokenProvider;
    private final BioIdConnectionManager connectionManager;
    private final Random random = new Random();

    /**
     * Liveness detection modes supported by the BioID Web Service.
     */
    public enum LivenessMode {
        /** Single image texture analysis for liveness detection */
        PASSIVE,
        
        /** Two images with motion detection between them */
        ACTIVE,
        
        /** Two images with specific head movement validation */
        CHALLENGE_RESPONSE
    }

    /**
     * Challenge directions for challenge-response liveness detection.
     */
    public enum ChallengeDirection {
        UP, DOWN, LEFT, RIGHT
    }

    public LivenessDetectionClient(BioIdClientConfig config, 
                                 BioIdJwtTokenProvider tokenProvider,
                                 BioIdConnectionManager connectionManager) {
        this.config = config;
        this.tokenProvider = tokenProvider;
        this.connectionManager = connectionManager;
    }

    /**
     * Performs synchronous liveness detection.
     * 
     * @param request the liveness detection request
     * @return the liveness detection response
     * @throws BioIdException if the operation fails
     */
    public LivenessResult livenessDetection(LivenessDetectionRequest request) throws BioIdException {
        // Validate request - basic validation
        if (request.getLiveImages() == null || request.getLiveImages().isEmpty()) {
            throw new BioIdException("At least one live image is required");
        }
        if (request.getLiveImages().size() > 2) {
            throw new BioIdException("Maximum 2 images allowed for liveness detection");
        }

        // In a real implementation, this would make a gRPC call to the BioID service
        // For integration testing, we simulate the response
        return simulateLivenessDetectionResponse(request);
    }

    /**
     * Performs asynchronous liveness detection.
     * 
     * @param request the liveness detection request
     * @return a CompletableFuture containing the liveness detection response
     */
    public CompletableFuture<LivenessResult> livenessDetectionAsync(LivenessDetectionRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return livenessDetection(request);
            } catch (BioIdException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Generates random challenge directions for challenge-response liveness detection.
     * 
     * @return a list of 2 unique challenge directions
     */
    public List<ChallengeDirection> generateChallengeTags() {
        ChallengeDirection[] directions = ChallengeDirection.values();
        
        // Select 2 unique random directions
        ChallengeDirection first = directions[random.nextInt(directions.length)];
        ChallengeDirection second;
        
        do {
            second = directions[random.nextInt(directions.length)];
        } while (second == first);
        
        return Arrays.asList(first, second);
    }

    /**
     * Simulates a liveness detection response for testing purposes.
     * In a real implementation, this would be replaced with actual gRPC calls.
     */
    private LivenessResult simulateLivenessDetectionResponse(LivenessDetectionRequest request) {
        // Simulate processing based on mode
        boolean isLive = true;
        double livenessScore = 0.85 + (random.nextDouble() * 0.1); // 0.85-0.95
        
        // Simulate some failures for testing
        if (random.nextDouble() < 0.05) { // 5% failure rate
            isLive = false;
            livenessScore = 0.3 + (random.nextDouble() * 0.3); // 0.3-0.6
        }
        
        // Convert LivenessMode to LivenessMethod
        LivenessMethod method = LivenessMethod.PASSIVE; // Default
        if (request.getMode() == LivenessMode.ACTIVE) {
            method = LivenessMethod.ACTIVE_SMILE;
        } else if (request.getMode() == LivenessMode.CHALLENGE_RESPONSE) {
            method = LivenessMethod.CHALLENGE_RESPONSE;
        }
        
        return new LivenessResult(
            isLive,
            livenessScore,
            method,
            java.time.Duration.ofMillis(100), // Simulated processing time
            LivenessQuality.GOOD
        );
    }

    /**
     * Response from liveness detection operation.
     * @deprecated Use LivenessResult instead
     */
    @Deprecated
    public static class LivenessDetectionResponse {
        private final boolean live;
        private final double livenessScore;
        private final LivenessMode mode;
        private final List<String> errors;

        public LivenessDetectionResponse(boolean live, double livenessScore, 
                                       LivenessMode mode, List<String> errors) {
            this.live = live;
            this.livenessScore = livenessScore;
            this.mode = mode;
            this.errors = errors != null ? Collections.unmodifiableList(errors) : Collections.emptyList();
        }

        public boolean isLive() { return live; }
        public double getLivenessScore() { return livenessScore; }
        public LivenessMode getMode() { return mode; }
        public List<String> getErrors() { return errors; }

        @Override
        public String toString() {
            return "LivenessDetectionResponse{" +
                   "live=" + live +
                   ", livenessScore=" + livenessScore +
                   ", mode=" + mode +
                   ", errors=" + errors.size() +
                   '}';
        }
    }
}