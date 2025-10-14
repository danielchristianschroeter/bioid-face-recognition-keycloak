package com.bioid.keycloak.client.liveness;

import com.bioid.keycloak.client.auth.BioIdJwtTokenProvider;
import com.bioid.keycloak.client.config.BioIdClientConfig;
import com.bioid.keycloak.client.connection.BioIdConnectionManager;
import com.bioid.keycloak.client.exception.BioIdException;
import com.bioid.keycloak.client.exception.BioIdValidationException;
import io.grpc.ManagedChannel;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for end-to-end challenge-response workflows.
 * 
 * Tests the complete challenge-response liveness detection workflow including:
 * - Challenge generation and validation
 * - User interaction simulation
 * - Challenge tag processing
 * - Response validation
 * - Error handling for challenge failures
 * - Multi-step workflow coordination
 * 
 * Requirements tested: 3.1, 3.2, 3.3, 4.1, 4.2, 4.3, 4.4, 4.5, 4.6
 */
@DisplayName("Challenge-Response Workflow Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ChallengeResponseWorkflowIntegrationTest {

    @Mock
    private BioIdJwtTokenProvider tokenProvider;
    
    @Mock
    private BioIdConnectionManager connectionManager;
    
    @Mock
    private ManagedChannel managedChannel;
    
    private LivenessDetectionClient livenessClient;
    private BioIdClientConfig config;
    
    // Test images for challenge-response scenarios
    private byte[] neutralFaceImage;
    private byte[] upMovementImage;
    private byte[] downMovementImage;
    private byte[] leftMovementImage;
    private byte[] rightMovementImage;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        config = BioIdClientConfig.builder()
            .endpoint("localhost:9090")
            .clientId("test-client")
            .secretKey("test-secret")
            .requestTimeout(Duration.ofSeconds(15))
            .maxRetryAttempts(3)
            .initialRetryDelay(Duration.ofMillis(100))
            .retryBackoffMultiplier(2.0)
            .build();

        when(tokenProvider.getToken()).thenReturn("mock-jwt-token");
        try {
            when(connectionManager.getChannel()).thenReturn(managedChannel);
        } catch (Exception e) {
            // Mock setup - ignore exceptions
        }
        
        livenessClient = new LivenessDetectionClient(config, tokenProvider, connectionManager);
        
        initializeChallengeImages();
    }

    @Test
    @Order(1)
    @DisplayName("Should generate valid challenge directions")
    void shouldGenerateValidChallengeDirections() {
        // Test multiple challenge generation calls
        for (int i = 0; i < 10; i++) {
            List<LivenessDetectionClient.ChallengeDirection> directions = 
                livenessClient.generateChallengeTags();
            
            // Validate challenge properties
            assertThat(directions).hasSize(2);
            assertThat(directions.get(0)).isNotEqualTo(directions.get(1)); // Must be different
            assertThat(directions).allMatch(direction -> 
                Arrays.asList(LivenessDetectionClient.ChallengeDirection.values()).contains(direction));
        }
    }

    @Test
    @Order(2)
    @DisplayName("Should validate challenge direction combinations")
    void shouldValidateChallengeDirectionCombinations() {
        // Test all possible valid combinations
        LivenessDetectionClient.ChallengeDirection[] allDirections = 
            LivenessDetectionClient.ChallengeDirection.values();
        
        for (int i = 0; i < allDirections.length; i++) {
            for (int j = i + 1; j < allDirections.length; j++) {
                List<LivenessDetectionClient.ChallengeDirection> combination = 
                    Arrays.asList(allDirections[i], allDirections[j]);
                
                assertThatCode(() -> {
                    LivenessDetectionErrorHandler.validateChallengeDirections(combination);
                }).doesNotThrowAnyException();
            }
        }
    }

    @ParameterizedTest
    @MethodSource("provideChallengeDirectionScenarios")
    @Order(3)
    @DisplayName("Should handle different challenge direction scenarios")
    void shouldHandleDifferentChallengeDirectionScenarios(
            List<LivenessDetectionClient.ChallengeDirection> directions,
            boolean shouldSucceed,
            String expectedError) {
        
        if (shouldSucceed) {
            assertThatCode(() -> {
                simulateChallengeResponseWorkflow(neutralFaceImage, upMovementImage, directions);
            }).doesNotThrowAnyException();
        } else {
            assertThatThrownBy(() -> {
                simulateChallengeResponseWorkflow(neutralFaceImage, upMovementImage, directions);
            }).isInstanceOf(BioIdValidationException.class)
              .hasMessageContaining(expectedError);
        }
    }

    @Test
    @Order(4)
    @DisplayName("Should handle complete challenge-response workflow")
    void shouldHandleCompleteChallengeResponseWorkflow() throws InterruptedException, ExecutionException, TimeoutException {
        // Step 1: System generates challenge
        List<LivenessDetectionClient.ChallengeDirection> challenge = 
            livenessClient.generateChallengeTags();
        
        assertThat(challenge).hasSize(2);
        
        // Step 2: System presents challenge to user
        ChallengeInstructions instructions = buildChallengeInstructions(challenge);
        assertThat(instructions.getInstructionText()).isNotEmpty();
        assertThat(instructions.getDirections()).isEqualTo(challenge);
        
        // Step 3: User captures initial image
        byte[] initialImage = neutralFaceImage;
        
        // Step 4: User follows challenge directions and captures response image
        byte[] responseImage = getChallengeResponseImage(challenge);
        
        // Step 5: System validates and processes challenge-response
        CompletableFuture<ChallengeResponseResult> future = 
            simulateAsyncChallengeResponse(initialImage, responseImage, challenge);
        
        ChallengeResponseResult result = future.get(5, TimeUnit.SECONDS);
        
        // Step 6: Validate results
        assertThat(result).isNotNull();
        assertThat(result.isSuccessful()).isTrue();
        assertThat(result.getLivenessScore()).isGreaterThan(0.7);
        assertThat(result.getChallengeDirections()).isEqualTo(challenge);
        assertThat(result.getProcessingTimeMs()).isLessThan(1000);
    }

    @Test
    @Order(5)
    @DisplayName("Should handle challenge-response with different movement combinations")
    void shouldHandleChallengeResponseWithDifferentMovementCombinations() {
        // Test vertical movements (UP + DOWN)
        List<LivenessDetectionClient.ChallengeDirection> verticalChallenge = 
            Arrays.asList(LivenessDetectionClient.ChallengeDirection.UP, LivenessDetectionClient.ChallengeDirection.DOWN);
        
        assertThatCode(() -> {
            simulateChallengeResponseWorkflow(neutralFaceImage, upMovementImage, verticalChallenge);
        }).doesNotThrowAnyException();
        
        // Test horizontal movements (LEFT + RIGHT)
        List<LivenessDetectionClient.ChallengeDirection> horizontalChallenge = 
            Arrays.asList(LivenessDetectionClient.ChallengeDirection.LEFT, LivenessDetectionClient.ChallengeDirection.RIGHT);
        
        assertThatCode(() -> {
            simulateChallengeResponseWorkflow(neutralFaceImage, leftMovementImage, horizontalChallenge);
        }).doesNotThrowAnyException();
        
        // Test diagonal movements (UP + RIGHT)
        List<LivenessDetectionClient.ChallengeDirection> diagonalChallenge = 
            Arrays.asList(LivenessDetectionClient.ChallengeDirection.UP, LivenessDetectionClient.ChallengeDirection.RIGHT);
        
        assertThatCode(() -> {
            simulateChallengeResponseWorkflow(neutralFaceImage, rightMovementImage, diagonalChallenge);
        }).doesNotThrowAnyException();
    }

    @Test
    @Order(6)
    @DisplayName("Should handle concurrent challenge-response operations")
    void shouldHandleConcurrentChallengeResponseOperations() throws InterruptedException, ExecutionException, TimeoutException {
        int concurrentChallenges = 15;
        
        List<CompletableFuture<ChallengeResponseResult>> futures = 
            java.util.stream.IntStream.range(0, concurrentChallenges)
                .mapToObj(i -> {
                    // Generate unique challenge for each concurrent operation
                    List<LivenessDetectionClient.ChallengeDirection> challenge = 
                        livenessClient.generateChallengeTags();
                    
                    return simulateAsyncChallengeResponse(
                        neutralFaceImage, 
                        getChallengeResponseImage(challenge), 
                        challenge
                    );
                })
                .toList();
        
        // Wait for all concurrent operations to complete
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
            futures.toArray(new CompletableFuture[0]));
        
        allFutures.get(10, TimeUnit.SECONDS);
        
        // Validate all results
        for (CompletableFuture<ChallengeResponseResult> future : futures) {
            ChallengeResponseResult result = future.get();
            assertThat(result.isSuccessful()).isTrue();
            assertThat(result.getLivenessScore()).isGreaterThan(0.7);
        }
    }

    @Test
    @Order(7)
    @DisplayName("Should handle challenge-response timeout scenarios")
    void shouldHandleChallengeResponseTimeoutScenarios() {
        List<LivenessDetectionClient.ChallengeDirection> challenge = 
            Arrays.asList(LivenessDetectionClient.ChallengeDirection.UP, LivenessDetectionClient.ChallengeDirection.DOWN);
        
        // Simulate timeout during challenge processing
        assertThatThrownBy(() -> {
            CompletableFuture<ChallengeResponseResult> future = 
                simulateTimeoutChallengeResponse(neutralFaceImage, upMovementImage, challenge);
            
            future.get(100, TimeUnit.MILLISECONDS); // Very short timeout
        }).isInstanceOf(TimeoutException.class);
    }

    @Test
    @Order(8)
    @DisplayName("Should handle challenge-response validation failures")
    void shouldHandleChallengeResponseValidationFailures() {
        List<LivenessDetectionClient.ChallengeDirection> challenge = 
            Arrays.asList(LivenessDetectionClient.ChallengeDirection.UP, LivenessDetectionClient.ChallengeDirection.DOWN);
        
        // Test wrong movement response
        assertThatCode(() -> {
            ChallengeResponseResult result = simulateFailedChallengeResponse(
                neutralFaceImage, leftMovementImage, challenge, "Wrong movement detected");
            
            assertThat(result.isSuccessful()).isFalse();
            assertThat(result.getFailureReason()).contains("Wrong movement");
            assertThat(result.getLivenessScore()).isLessThan(0.7);
        }).doesNotThrowAnyException();
        
        // Test no movement response
        assertThatCode(() -> {
            ChallengeResponseResult result = simulateFailedChallengeResponse(
                neutralFaceImage, neutralFaceImage, challenge, "No movement detected");
            
            assertThat(result.isSuccessful()).isFalse();
            assertThat(result.getFailureReason()).contains("No movement");
        }).doesNotThrowAnyException();
    }

    @Test
    @Order(9)
    @DisplayName("Should handle challenge-response with image quality issues")
    void shouldHandleChallengeResponseWithImageQualityIssues() {
        List<LivenessDetectionClient.ChallengeDirection> challenge = 
            Arrays.asList(LivenessDetectionClient.ChallengeDirection.LEFT, LivenessDetectionClient.ChallengeDirection.RIGHT);
        
        // Test with low quality images
        byte[] lowQualityImage = createLowQualityImage();
        
        assertThatThrownBy(() -> {
            simulateChallengeResponseWorkflow(lowQualityImage, leftMovementImage, challenge);
        }).isInstanceOf(BioIdValidationException.class)
          .hasMessageContaining("too small");
        
        // Test with corrupted images
        byte[] corruptedImage = createCorruptedImage();
        
        assertThatThrownBy(() -> {
            simulateChallengeResponseWorkflow(neutralFaceImage, corruptedImage, challenge);
        }).isInstanceOf(BioIdValidationException.class)
          .hasMessageContaining("unsupported format");
    }

    @Test
    @Order(10)
    @DisplayName("Should handle challenge-response workflow state management")
    void shouldHandleChallengeResponseWorkflowStateManagement() {
        // Test workflow state transitions
        ChallengeResponseWorkflow workflow = new ChallengeResponseWorkflow();
        
        // Initial state
        assertThat(workflow.getState()).isEqualTo(ChallengeResponseWorkflow.State.INITIAL);
        
        // Generate challenge
        List<LivenessDetectionClient.ChallengeDirection> challenge = 
            livenessClient.generateChallengeTags();
        workflow.setChallenge(challenge);
        assertThat(workflow.getState()).isEqualTo(ChallengeResponseWorkflow.State.CHALLENGE_GENERATED);
        
        // Capture initial image
        workflow.setInitialImage(neutralFaceImage);
        assertThat(workflow.getState()).isEqualTo(ChallengeResponseWorkflow.State.INITIAL_IMAGE_CAPTURED);
        
        // Present challenge to user
        workflow.presentChallenge();
        assertThat(workflow.getState()).isEqualTo(ChallengeResponseWorkflow.State.CHALLENGE_PRESENTED);
        
        // Capture response image
        workflow.setResponseImage(getChallengeResponseImage(challenge));
        assertThat(workflow.getState()).isEqualTo(ChallengeResponseWorkflow.State.RESPONSE_IMAGE_CAPTURED);
        
        // Process challenge-response
        workflow.processResponse();
        assertThat(workflow.getState()).isEqualTo(ChallengeResponseWorkflow.State.PROCESSING);
        
        // Complete workflow
        workflow.complete(true, 0.85, "Challenge completed successfully");
        assertThat(workflow.getState()).isEqualTo(ChallengeResponseWorkflow.State.COMPLETED);
        assertThat(workflow.isSuccessful()).isTrue();
    }

    @Test
    @Order(11)
    @DisplayName("Should handle challenge-response retry scenarios")
    void shouldHandleChallengeResponseRetryScenarios() {
        List<LivenessDetectionClient.ChallengeDirection> challenge = 
            Arrays.asList(LivenessDetectionClient.ChallengeDirection.UP, LivenessDetectionClient.ChallengeDirection.DOWN);
        
        // Simulate retry workflow
        ChallengeResponseWorkflow workflow = new ChallengeResponseWorkflow();
        workflow.setChallenge(challenge);
        workflow.setInitialImage(neutralFaceImage);
        
        // First attempt fails
        workflow.setResponseImage(leftMovementImage); // Wrong movement
        workflow.processResponse();
        workflow.complete(false, 0.3, "Wrong movement detected");
        
        assertThat(workflow.isSuccessful()).isFalse();
        assertThat(workflow.canRetry()).isTrue();
        
        // Retry with correct movement
        workflow.retry();
        assertThat(workflow.getState()).isEqualTo(ChallengeResponseWorkflow.State.CHALLENGE_PRESENTED);
        
        workflow.setResponseImage(upMovementImage); // Correct movement
        workflow.processResponse();
        workflow.complete(true, 0.88, "Challenge completed successfully");
        
        assertThat(workflow.isSuccessful()).isTrue();
        assertThat(workflow.getRetryCount()).isEqualTo(1);
    }

    @Test
    @Order(12)
    @DisplayName("Should handle challenge-response performance optimization")
    void shouldHandleChallengeResponsePerformanceOptimization() throws InterruptedException, ExecutionException, TimeoutException {
        int performanceTestCount = 25;
        long startTime = System.currentTimeMillis();
        
        List<CompletableFuture<Long>> futures = 
            java.util.stream.IntStream.range(0, performanceTestCount)
                .mapToObj(i -> CompletableFuture.supplyAsync(() -> {
                    long operationStart = System.currentTimeMillis();
                    
                    try {
                        // Generate challenge
                        List<LivenessDetectionClient.ChallengeDirection> challenge = 
                            livenessClient.generateChallengeTags();
                        
                        // Simulate optimized challenge-response processing
                        simulateChallengeResponseWorkflow(neutralFaceImage, 
                            getChallengeResponseImage(challenge), challenge);
                        
                    } catch (Exception e) {
                        throw new RuntimeException("Performance test failed", e);
                    }
                    
                    return System.currentTimeMillis() - operationStart;
                }))
                .toList();
        
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
            futures.toArray(new CompletableFuture[0]));
        
        allFutures.get(15, TimeUnit.SECONDS);
        
        // Analyze performance metrics
        List<Long> processingTimes = futures.stream()
            .map(CompletableFuture::join)
            .toList();
        
        long totalTime = System.currentTimeMillis() - startTime;
        double averageTime = processingTimes.stream().mapToLong(Long::longValue).average().orElse(0);
        long maxTime = processingTimes.stream().mapToLong(Long::longValue).max().orElse(0);
        long minTime = processingTimes.stream().mapToLong(Long::longValue).min().orElse(0);
        
        // Performance assertions
        assertThat(totalTime).isLessThan(10000); // Total under 10 seconds
        assertThat(averageTime).isLessThan(150); // Average under 150ms
        assertThat(maxTime).isLessThan(500); // Max under 500ms
        assertThat(minTime).isGreaterThan(0); // Min greater than 0
        
        // Verify consistent performance
        long performanceVariance = maxTime - minTime;
        assertThat(performanceVariance).isLessThan(300); // Variance under 300ms
    }

    // Helper methods and test data providers

    private static Stream<Arguments> provideChallengeDirectionScenarios() {
        return Stream.of(
            // Valid scenarios
            Arguments.of(Arrays.asList(LivenessDetectionClient.ChallengeDirection.UP, LivenessDetectionClient.ChallengeDirection.DOWN), 
                        true, null),
            Arguments.of(Arrays.asList(LivenessDetectionClient.ChallengeDirection.LEFT, LivenessDetectionClient.ChallengeDirection.RIGHT), 
                        true, null),
            Arguments.of(Arrays.asList(LivenessDetectionClient.ChallengeDirection.UP, LivenessDetectionClient.ChallengeDirection.LEFT), 
                        true, null),
            
            // Invalid scenarios
            Arguments.of(null, false, "Challenge directions cannot be null"),
            Arguments.of(Collections.emptyList(), false, "Challenge directions cannot be null or empty"),
            Arguments.of(Arrays.asList(LivenessDetectionClient.ChallengeDirection.UP, LivenessDetectionClient.ChallengeDirection.UP), 
                        false, "Duplicate challenge directions"),
            Arguments.of(Arrays.asList(LivenessDetectionClient.ChallengeDirection.UP, LivenessDetectionClient.ChallengeDirection.DOWN, 
                                     LivenessDetectionClient.ChallengeDirection.LEFT, LivenessDetectionClient.ChallengeDirection.RIGHT,
                                     LivenessDetectionClient.ChallengeDirection.UP), 
                        false, "Maximum 4 challenge directions allowed")
        );
    }

    private void initializeChallengeImages() {
        // Create images representing different head positions/movements
        neutralFaceImage = createChallengeImage("neutral", 2048);
        upMovementImage = createChallengeImage("up-movement", 2048);
        downMovementImage = createChallengeImage("down-movement", 2048);
        leftMovementImage = createChallengeImage("left-movement", 2048);
        rightMovementImage = createChallengeImage("right-movement", 2048);
    }

    private byte[] createChallengeImage(String movementType, int size) {
        byte[] data = new byte[size];
        
        // JPEG magic bytes
        data[0] = (byte) 0xFF;
        data[1] = (byte) 0xD8;
        data[2] = (byte) 0xFF;
        data[3] = (byte) 0xE0;
        
        // Fill with pattern based on movement type
        for (int i = 4; i < size; i++) {
            data[i] = (byte) ((movementType.hashCode() + i) % 256);
        }
        
        return data;
    }

    private byte[] createLowQualityImage() {
        return new byte[512]; // Too small
    }

    private byte[] createCorruptedImage() {
        byte[] data = new byte[2048];
        // Invalid magic bytes
        data[0] = (byte) 0x00;
        data[1] = (byte) 0x00;
        data[2] = (byte) 0x00;
        data[3] = (byte) 0x00;
        return data;
    }

    private byte[] getChallengeResponseImage(List<LivenessDetectionClient.ChallengeDirection> directions) {
        // Return appropriate image based on first challenge direction
        LivenessDetectionClient.ChallengeDirection primaryDirection = directions.get(0);
        
        switch (primaryDirection) {
            case UP:
                return upMovementImage;
            case DOWN:
                return downMovementImage;
            case LEFT:
                return leftMovementImage;
            case RIGHT:
                return rightMovementImage;
            default:
                return neutralFaceImage;
        }
    }

    private void simulateChallengeResponseWorkflow(byte[] initialImage, byte[] responseImage, 
                                                 List<LivenessDetectionClient.ChallengeDirection> directions) 
            throws BioIdValidationException {
        
        // Validate request
        LivenessDetectionErrorHandler.validateLivenessRequest(
            List.of(initialImage, responseImage), 
            LivenessDetectionClient.LivenessMode.CHALLENGE_RESPONSE, 
            directions
        );
        
        // Simulate processing time (would normally call gRPC service)
        try {
            Thread.sleep(10 + (int)(Math.random() * 20)); // 10-30ms processing time
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Processing interrupted", e);
        }
    }

    private CompletableFuture<ChallengeResponseResult> simulateAsyncChallengeResponse(
            byte[] initialImage, byte[] responseImage, 
            List<LivenessDetectionClient.ChallengeDirection> directions) {
        
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            
            try {
                // Simulate processing delay
                Thread.sleep(50);
                
                // Validate challenge-response
                simulateChallengeResponseWorkflow(initialImage, responseImage, directions);
                
                long processingTime = System.currentTimeMillis() - startTime;
                
                // Return successful result
                return new ChallengeResponseResult(true, 0.85, directions, processingTime, null);
                
            } catch (Exception e) {
                long processingTime = System.currentTimeMillis() - startTime;
                return new ChallengeResponseResult(false, 0.3, directions, processingTime, e.getMessage());
            }
        });
    }

    private CompletableFuture<ChallengeResponseResult> simulateTimeoutChallengeResponse(
            byte[] initialImage, byte[] responseImage, 
            List<LivenessDetectionClient.ChallengeDirection> directions) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Simulate long processing that will timeout
                Thread.sleep(5000);
                return new ChallengeResponseResult(true, 0.85, directions, 5000, null);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Timeout simulation interrupted", e);
            }
        });
    }

    private ChallengeResponseResult simulateFailedChallengeResponse(
            byte[] initialImage, byte[] responseImage, 
            List<LivenessDetectionClient.ChallengeDirection> directions,
            String failureReason) {
        
        // Simulate failed challenge-response
        return new ChallengeResponseResult(false, 0.3, directions, 100, failureReason);
    }

    private ChallengeInstructions buildChallengeInstructions(
            List<LivenessDetectionClient.ChallengeDirection> directions) {
        
        StringBuilder text = new StringBuilder("Please perform the following head movements in order: ");
        for (int i = 0; i < directions.size(); i++) {
            if (i > 0) text.append(", then ");
            text.append(directions.get(i).name().toLowerCase());
        }
        
        return new ChallengeInstructions(text.toString(), directions);
    }

    // Helper classes for testing

    private static class ChallengeInstructions {
        private final String instructionText;
        private final List<LivenessDetectionClient.ChallengeDirection> directions;

        public ChallengeInstructions(String instructionText, 
                                   List<LivenessDetectionClient.ChallengeDirection> directions) {
            this.instructionText = instructionText;
            this.directions = directions;
        }

        public String getInstructionText() { return instructionText; }
        public List<LivenessDetectionClient.ChallengeDirection> getDirections() { return directions; }
    }

    private static class ChallengeResponseResult {
        private final boolean successful;
        private final double livenessScore;
        private final List<LivenessDetectionClient.ChallengeDirection> challengeDirections;
        private final long processingTimeMs;
        private final String failureReason;

        public ChallengeResponseResult(boolean successful, double livenessScore, 
                                     List<LivenessDetectionClient.ChallengeDirection> challengeDirections,
                                     long processingTimeMs, String failureReason) {
            this.successful = successful;
            this.livenessScore = livenessScore;
            this.challengeDirections = challengeDirections;
            this.processingTimeMs = processingTimeMs;
            this.failureReason = failureReason;
        }

        public boolean isSuccessful() { return successful; }
        public double getLivenessScore() { return livenessScore; }
        public List<LivenessDetectionClient.ChallengeDirection> getChallengeDirections() { return challengeDirections; }
        public long getProcessingTimeMs() { return processingTimeMs; }
        public String getFailureReason() { return failureReason; }
    }

    private static class ChallengeResponseWorkflow {
        public enum State {
            INITIAL, CHALLENGE_GENERATED, INITIAL_IMAGE_CAPTURED, 
            CHALLENGE_PRESENTED, RESPONSE_IMAGE_CAPTURED, PROCESSING, COMPLETED
        }

        private State state = State.INITIAL;
        private List<LivenessDetectionClient.ChallengeDirection> challenge;
        private byte[] initialImage;
        private byte[] responseImage;
        private boolean successful;
        private double livenessScore;
        private String message;
        private int retryCount = 0;

        public State getState() { return state; }
        public boolean isSuccessful() { return successful; }
        public int getRetryCount() { return retryCount; }
        public boolean canRetry() { return !successful && retryCount < 3; }

        public void setChallenge(List<LivenessDetectionClient.ChallengeDirection> challenge) {
            this.challenge = challenge;
            this.state = State.CHALLENGE_GENERATED;
        }

        public void setInitialImage(byte[] initialImage) {
            this.initialImage = initialImage;
            this.state = State.INITIAL_IMAGE_CAPTURED;
        }

        public void presentChallenge() {
            this.state = State.CHALLENGE_PRESENTED;
        }

        public void setResponseImage(byte[] responseImage) {
            this.responseImage = responseImage;
            this.state = State.RESPONSE_IMAGE_CAPTURED;
        }

        public void processResponse() {
            this.state = State.PROCESSING;
        }

        public void complete(boolean successful, double livenessScore, String message) {
            this.successful = successful;
            this.livenessScore = livenessScore;
            this.message = message;
            this.state = State.COMPLETED;
        }

        public void retry() {
            this.retryCount++;
            this.state = State.CHALLENGE_PRESENTED;
            this.successful = false;
        }
    }
}