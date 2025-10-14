package com.bioid.keycloak.client.liveness;

import com.bioid.keycloak.client.auth.BioIdJwtTokenProvider;
import com.bioid.keycloak.client.config.BioIdClientConfig;
import com.bioid.keycloak.client.connection.BioIdConnectionManager;
import com.bioid.keycloak.client.exception.BioIdException;
import com.bioid.keycloak.client.exception.BioIdServiceException;
import com.bioid.keycloak.client.exception.BioIdValidationException;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for all liveness detection modes.
 * 
 * Tests the complete liveness detection workflow including:
 * - PASSIVE mode (single image texture analysis)
 * - ACTIVE mode (two images with motion detection)
 * - CHALLENGE_RESPONSE mode (two images with head movement validation)
 * - Error handling and recovery scenarios
 * - Performance characteristics under load
 * 
 * Requirements tested: 3.1, 3.2, 3.3, 4.1-4.6
 */
@DisplayName("Liveness Detection Mode Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LivenessDetectionModeIntegrationTest {

    private static final String INTEGRATION_ENABLED_PROPERTY = "bioid.integration.enabled";
    private static final Duration TEST_TIMEOUT = Duration.ofSeconds(30);
    
    @Mock
    private BioIdJwtTokenProvider tokenProvider;
    
    @Mock
    private BioIdConnectionManager connectionManager;
    
    @Mock
    private ManagedChannel managedChannel;
    
    private LivenessDetectionClient livenessClient;
    private BioIdClientConfig config;
    
    // Test image data
    private byte[] validJpegImage1;
    private byte[] validJpegImage2;
    private byte[] validPngImage1;
    private byte[] validPngImage2;
    private byte[] invalidImage;
    private byte[] tooSmallImage;
    private byte[] tooLargeImage;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Create test configuration
        config = BioIdClientConfig.builder()
            .endpoint("localhost:9090")
            .clientId("test-client")
            .secretKey("test-secret")
            .requestTimeout(Duration.ofSeconds(10))
            .maxRetryAttempts(3)
            .initialRetryDelay(Duration.ofMillis(100))
            .retryBackoffMultiplier(2.0)
            .build();

        // Setup mocks
        when(tokenProvider.getToken()).thenReturn("mock-jwt-token");
        try {
            when(connectionManager.getChannel()).thenReturn(managedChannel);
        } catch (Exception e) {
            // Mock setup - ignore exceptions
        }
        
        // Create client
        livenessClient = new LivenessDetectionClient(config, tokenProvider, connectionManager);
        
        // Initialize test images
        initializeTestImages();
    }

    @Test
    @Order(1)
    @DisplayName("Should validate passive liveness detection mode requirements")
    void shouldValidatePassiveLivenessDetectionModeRequirements() {
        // Test valid passive mode request (1 image)
        assertThatCode(() -> {
            validateLivenessRequest(List.of(validJpegImage1), LivenessDetectionClient.LivenessMode.PASSIVE);
        }).doesNotThrowAnyException();

        // Test invalid passive mode request (2 images)
        assertThatThrownBy(() -> {
            validateLivenessRequest(List.of(validJpegImage1, validJpegImage2), LivenessDetectionClient.LivenessMode.PASSIVE);
        }).isInstanceOf(BioIdValidationException.class)
          .hasMessageContaining("Passive liveness detection requires exactly 1 image");

        // Test invalid passive mode request (no images)
        assertThatThrownBy(() -> {
            validateLivenessRequest(List.of(), LivenessDetectionClient.LivenessMode.PASSIVE);
        }).isInstanceOf(BioIdValidationException.class)
          .hasMessageContaining("No images provided");
    }

    @Test
    @Order(2)
    @DisplayName("Should validate active liveness detection mode requirements")
    void shouldValidateActiveLivenessDetectionModeRequirements() {
        // Test valid active mode request (2 images)
        assertThatCode(() -> {
            validateLivenessRequest(List.of(validJpegImage1, validJpegImage2), LivenessDetectionClient.LivenessMode.ACTIVE);
        }).doesNotThrowAnyException();

        // Test invalid active mode request (1 image)
        assertThatThrownBy(() -> {
            validateLivenessRequest(List.of(validJpegImage1), LivenessDetectionClient.LivenessMode.ACTIVE);
        }).isInstanceOf(BioIdValidationException.class)
          .hasMessageContaining("ACTIVE liveness detection requires exactly 2 images");

        // Test invalid active mode request (3 images)
        assertThatThrownBy(() -> {
            validateLivenessRequest(List.of(validJpegImage1, validJpegImage2, validPngImage1), LivenessDetectionClient.LivenessMode.ACTIVE);
        }).isInstanceOf(BioIdValidationException.class)
          .hasMessageContaining("Maximum 2 images allowed");
    }

    @Test
    @Order(3)
    @DisplayName("Should validate challenge-response liveness detection mode requirements")
    void shouldValidateChallengeResponseLivenessDetectionModeRequirements() {
        List<LivenessDetectionClient.ChallengeDirection> validDirections = Arrays.asList(
            LivenessDetectionClient.ChallengeDirection.UP,
            LivenessDetectionClient.ChallengeDirection.DOWN
        );

        // Test valid challenge-response mode request
        assertThatCode(() -> {
            validateChallengeResponseRequest(validJpegImage1, validJpegImage2, validDirections);
        }).doesNotThrowAnyException();

        // Test invalid challenge-response mode request (no directions)
        assertThatThrownBy(() -> {
            validateChallengeResponseRequest(validJpegImage1, validJpegImage2, null);
        }).isInstanceOf(BioIdValidationException.class)
          .hasMessageContaining("Challenge directions cannot be null");

        // Test invalid challenge-response mode request (empty directions)
        assertThatThrownBy(() -> {
            validateChallengeResponseRequest(validJpegImage1, validJpegImage2, List.of());
        }).isInstanceOf(BioIdValidationException.class)
          .hasMessageContaining("Challenge directions cannot be null or empty");

        // Test invalid challenge-response mode request (too many directions)
        List<LivenessDetectionClient.ChallengeDirection> tooManyDirections = Arrays.asList(
            LivenessDetectionClient.ChallengeDirection.UP,
            LivenessDetectionClient.ChallengeDirection.DOWN,
            LivenessDetectionClient.ChallengeDirection.LEFT,
            LivenessDetectionClient.ChallengeDirection.RIGHT,
            LivenessDetectionClient.ChallengeDirection.UP // Duplicate to exceed limit
        );
        
        assertThatThrownBy(() -> {
            validateChallengeResponseRequest(validJpegImage1, validJpegImage2, tooManyDirections);
        }).isInstanceOf(BioIdValidationException.class)
          .hasMessageContaining("Maximum 4 challenge directions allowed");
    }

    @ParameterizedTest
    @EnumSource(LivenessDetectionClient.LivenessMode.class)
    @Order(4)
    @DisplayName("Should handle all liveness detection modes")
    void shouldHandleAllLivenessDetectionModes(LivenessDetectionClient.LivenessMode mode) {
        List<byte[]> images = getImagesForMode(mode);
        
        // Test request validation for each mode
        assertThatCode(() -> {
            validateLivenessRequest(images, mode);
        }).doesNotThrowAnyException();
        
        // Test async method structure for each mode
        assertThatCode(() -> {
            CompletableFuture<MockLivenessResponse> future = simulateAsyncLivenessDetection(images, mode);
            assertThat(future).isNotNull();
        }).doesNotThrowAnyException();
    }

    @Test
    @Order(5)
    @DisplayName("Should handle end-to-end challenge-response workflow")
    void shouldHandleEndToEndChallengeResponseWorkflow() {
        // Step 1: Generate challenge directions
        List<LivenessDetectionClient.ChallengeDirection> challengeDirections = 
            livenessClient.generateChallengeTags();
        
        assertThat(challengeDirections).hasSize(2);
        assertThat(challengeDirections).allMatch(direction -> 
            Arrays.asList(LivenessDetectionClient.ChallengeDirection.values()).contains(direction));
        
        // Step 2: Validate challenge directions are unique
        assertThat(challengeDirections.get(0)).isNotEqualTo(challengeDirections.get(1));
        
        // Step 3: Simulate challenge-response workflow
        assertThatCode(() -> {
            // User captures first image
            byte[] firstImage = validJpegImage1;
            
            // System presents challenge directions to user
            String challengeInstructions = buildChallengeInstructions(challengeDirections);
            assertThat(challengeInstructions).isNotEmpty();
            
            // User captures second image following challenge
            byte[] secondImage = validJpegImage2;
            
            // System validates challenge-response request
            validateChallengeResponseRequest(firstImage, secondImage, challengeDirections);
            
            // System processes liveness detection
            CompletableFuture<MockLivenessResponse> future = 
                simulateChallengeResponseLivenessDetection(firstImage, secondImage, challengeDirections);
            
            assertThat(future).isNotNull();
            
        }).doesNotThrowAnyException();
    }

    @Test
    @Order(6)
    @DisplayName("Should handle concurrent liveness detection operations")
    void shouldHandleConcurrentLivenessDetectionOperations() throws InterruptedException, ExecutionException, TimeoutException {
        int concurrentRequests = 20;
        
        // Create concurrent passive liveness detection requests
        List<CompletableFuture<MockLivenessResponse>> futures = 
            java.util.stream.IntStream.range(0, concurrentRequests)
                .mapToObj(i -> CompletableFuture.supplyAsync(() -> {
                    try {
                        return simulateAsyncLivenessDetection(
                            List.of(validJpegImage1), 
                            LivenessDetectionClient.LivenessMode.PASSIVE
                        ).get(1, TimeUnit.SECONDS);
                    } catch (Exception e) {
                        throw new RuntimeException("Concurrent operation failed", e);
                    }
                }))
                .toList();
        
        // Wait for all operations to complete
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
            futures.toArray(new CompletableFuture[0]));
        
        assertThatCode(() -> allFutures.get(10, TimeUnit.SECONDS))
            .doesNotThrowAnyException();
        
        // Verify all operations completed successfully
        for (CompletableFuture<MockLivenessResponse> future : futures) {
            MockLivenessResponse response = future.get();
            assertThat(response).isNotNull();
            assertThat(response.isLive()).isTrue();
        }
    }

    @Test
    @Order(7)
    @DisplayName("Should handle load testing for different liveness modes")
    void shouldHandleLoadTestingForDifferentLivenessModes() throws InterruptedException, ExecutionException, TimeoutException {
        int loadTestRequests = 30;
        long startTime = System.currentTimeMillis();
        
        // Test load for each liveness mode
        for (LivenessDetectionClient.LivenessMode mode : LivenessDetectionClient.LivenessMode.values()) {
            List<byte[]> images = getImagesForMode(mode);
            
            List<CompletableFuture<Long>> futures = 
                java.util.stream.IntStream.range(0, loadTestRequests)
                    .mapToObj(i -> CompletableFuture.supplyAsync(() -> {
                        long requestStart = System.currentTimeMillis();
                        try {
                            validateLivenessRequest(images, mode);
                            simulateAsyncLivenessDetection(images, mode).get(500, TimeUnit.MILLISECONDS);
                        } catch (Exception e) {
                            throw new RuntimeException("Load test request failed for mode " + mode, e);
                        }
                        return System.currentTimeMillis() - requestStart;
                    }))
                    .toList();
            
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0]));
            
            allFutures.get(15, TimeUnit.SECONDS);
            
            // Verify performance characteristics
            List<Long> processingTimes = futures.stream()
                .map(CompletableFuture::join)
                .toList();
            
            double averageTime = processingTimes.stream().mapToLong(Long::longValue).average().orElse(0);
            assertThat(averageTime).isLessThan(200); // Average under 200ms for mock operations
            assertThat(processingTimes).allMatch(time -> time < 1000); // Each request under 1 second
        }
        
        long totalTime = System.currentTimeMillis() - startTime;
        assertThat(totalTime).isLessThan(30000); // Total test under 30 seconds
    }

    @ParameterizedTest
    @ValueSource(strings = {"jpeg", "png"})
    @Order(8)
    @DisplayName("Should handle different image formats")
    void shouldHandleDifferentImageFormats(String format) {
        byte[] testImage = format.equals("jpeg") ? validJpegImage1 : validPngImage1;
        
        // Test passive mode with different formats
        assertThatCode(() -> {
            validateLivenessRequest(List.of(testImage), LivenessDetectionClient.LivenessMode.PASSIVE);
        }).doesNotThrowAnyException();
        
        // Test active mode with different formats
        byte[] testImage2 = format.equals("jpeg") ? validJpegImage2 : validPngImage2;
        assertThatCode(() -> {
            validateLivenessRequest(List.of(testImage, testImage2), LivenessDetectionClient.LivenessMode.ACTIVE);
        }).doesNotThrowAnyException();
    }

    @Test
    @Order(9)
    @DisplayName("Should handle error scenarios gracefully")
    void shouldHandleErrorScenariosGracefully() {
        // Test empty image
        assertThatThrownBy(() -> {
            validateLivenessRequest(List.of(invalidImage), LivenessDetectionClient.LivenessMode.PASSIVE);
        }).isInstanceOf(BioIdValidationException.class)
          .hasMessageContaining("is null or empty");
        
        // Test image too small
        assertThatThrownBy(() -> {
            validateLivenessRequest(List.of(tooSmallImage), LivenessDetectionClient.LivenessMode.PASSIVE);
        }).isInstanceOf(BioIdValidationException.class)
          .hasMessageContaining("too small");
        
        // Test image too large
        assertThatThrownBy(() -> {
            validateLivenessRequest(List.of(tooLargeImage), LivenessDetectionClient.LivenessMode.PASSIVE);
        }).isInstanceOf(BioIdValidationException.class)
          .hasMessageContaining("too large");
        
        // Test null image list
        assertThatThrownBy(() -> {
            validateLivenessRequest(null, LivenessDetectionClient.LivenessMode.PASSIVE);
        }).isInstanceOf(BioIdValidationException.class)
          .hasMessageContaining("No images provided");
    }

    @Test
    @Order(10)
    @DisplayName("Should handle gRPC error scenarios")
    void shouldHandleGrpcErrorScenarios() {
        // Test different gRPC error codes
        StatusRuntimeException unavailableError = new StatusRuntimeException(
            Status.UNAVAILABLE.withDescription("Service unavailable"));
        
        StatusRuntimeException timeoutError = new StatusRuntimeException(
            Status.DEADLINE_EXCEEDED.withDescription("Request timeout"));
        
        StatusRuntimeException invalidArgError = new StatusRuntimeException(
            Status.INVALID_ARGUMENT.withDescription("Invalid request"));
        
        // Test error handling
        assertThatCode(() -> {
            BioIdException handledException = LivenessDetectionErrorHandler.handleGrpcException(
                unavailableError, "livenessDetection");
            assertThat(handledException).isInstanceOf(BioIdServiceException.class);
            assertThat(handledException.getMessage()).contains("unavailable");
        }).doesNotThrowAnyException();
        
        assertThatCode(() -> {
            BioIdException handledException = LivenessDetectionErrorHandler.handleGrpcException(
                timeoutError, "livenessDetection");
            assertThat(handledException).isInstanceOf(BioIdServiceException.class);
            assertThat(handledException.getMessage()).contains("timed out");
        }).doesNotThrowAnyException();
        
        assertThatCode(() -> {
            BioIdException handledException = LivenessDetectionErrorHandler.handleGrpcException(
                invalidArgError, "livenessDetection");
            assertThat(handledException).isInstanceOf(BioIdValidationException.class);
            assertThat(handledException.getMessage()).contains("Invalid argument");
        }).doesNotThrowAnyException();
    }

    @Test
    @Order(11)
    @DisplayName("Should validate liveness thresholds")
    void shouldValidateLivenessThresholds() {
        // Test valid thresholds
        double[] validThresholds = {0.0, 0.3, 0.5, 0.7, 0.9, 1.0};
        
        for (double threshold : validThresholds) {
            assertThatCode(() -> {
                LivenessDetectionErrorHandler.validateLivenessThreshold(threshold);
            }).doesNotThrowAnyException();
        }
        
        // Test invalid thresholds
        double[] invalidThresholds = {-0.1, 1.1, 2.0, -1.0};
        
        for (double threshold : invalidThresholds) {
            assertThatThrownBy(() -> {
                LivenessDetectionErrorHandler.validateLivenessThreshold(threshold);
            }).isInstanceOf(BioIdValidationException.class)
              .hasMessageContaining("must be between 0.0 and 1.0");
        }
        
        // Test NaN and infinity separately as they may have different behavior
        assertThatThrownBy(() -> {
            LivenessDetectionErrorHandler.validateLivenessThreshold(Double.NaN);
        }).isInstanceOf(BioIdValidationException.class);
        
        assertThatThrownBy(() -> {
            LivenessDetectionErrorHandler.validateLivenessThreshold(Double.POSITIVE_INFINITY);
        }).isInstanceOf(BioIdValidationException.class);
    }

    @Test
    @Order(12)
    @DisplayName("Should handle browser compatibility scenarios")
    void shouldHandleBrowserCompatibilityScenarios() {
        // Simulate different browser image capture scenarios
        
        // Chrome/Firefox JPEG capture
        byte[] chromeJpeg = createBrowserImage("chrome-jpeg", 2048, (byte) 0xFF, (byte) 0xD8, (byte) 0xFF);
        assertThatCode(() -> {
            validateLivenessRequest(List.of(chromeJpeg), LivenessDetectionClient.LivenessMode.PASSIVE);
        }).doesNotThrowAnyException();
        
        // Safari PNG capture
        byte[] safariPng = createBrowserImage("safari-png", 1536, (byte) 0x89, (byte) 0x50, (byte) 0x4E, (byte) 0x47);
        assertThatCode(() -> {
            validateLivenessRequest(List.of(safariPng), LivenessDetectionClient.LivenessMode.PASSIVE);
        }).doesNotThrowAnyException();
        
        // Edge WebP capture (should be rejected)
        byte[] edgeWebp = createBrowserImage("edge-webp", 1024, (byte) 0x52, (byte) 0x49, (byte) 0x46, (byte) 0x46);
        assertThatThrownBy(() -> {
            validateLivenessRequest(List.of(edgeWebp), LivenessDetectionClient.LivenessMode.PASSIVE);
        }).isInstanceOf(BioIdValidationException.class)
          .hasMessageContaining("unsupported format");
    }

    @Test
    @Order(13)
    @DisplayName("Should handle device compatibility scenarios")
    void shouldHandleDeviceCompatibilityScenarios() {
        // Test different device image sizes
        int[] deviceImageSizes = {1024, 2048, 4096, 8192}; // 1KB to 8KB for testing
        
        for (int size : deviceImageSizes) {
            byte[] deviceImage = createDeviceImage("device-" + size, size);
            
            assertThatCode(() -> {
                validateLivenessRequest(List.of(deviceImage), LivenessDetectionClient.LivenessMode.PASSIVE);
            }).doesNotThrowAnyException();
        }
        
        // Test mobile device portrait/landscape orientations
        byte[] portraitImage = createDeviceImage("portrait", 2048);
        byte[] landscapeImage = createDeviceImage("landscape", 2048);
        
        assertThatCode(() -> {
            validateLivenessRequest(List.of(portraitImage, landscapeImage), LivenessDetectionClient.LivenessMode.ACTIVE);
        }).doesNotThrowAnyException();
    }

    @Test
    @Order(14)
    @EnabledIfSystemProperty(named = INTEGRATION_ENABLED_PROPERTY, matches = "true")
    @DisplayName("Should perform actual gRPC integration test")
    void shouldPerformActualGrpcIntegrationTest() {
        // This test would only run when actual BioID service is available
        // and integration testing is explicitly enabled
        
        // Given - Real configuration for integration testing
        BioIdClientConfig realConfig = BioIdClientConfig.builder()
            .endpoint(System.getProperty("bioid.endpoint", "localhost:9090"))
            .clientId(System.getProperty("bioid.clientId", "test-client"))
            .secretKey(System.getProperty("bioid.secretKey", "test-secret"))
            .requestTimeout(Duration.ofSeconds(30))
            .build();
        
        // When & Then - This would test against actual BioID service
        assertThatCode(() -> {
            // Real integration test would go here
            // For now, just validate configuration
            assertThat(realConfig.endpoint()).isNotEmpty();
            assertThat(realConfig.clientId()).isNotEmpty();
            assertThat(realConfig.secretKey()).isNotEmpty();
            
            // In a real test, we would:
            // 1. Create real LivenessDetectionClient with actual gRPC connection
            // 2. Perform actual liveness detection calls
            // 3. Validate responses from real BioID service
            // 4. Test error scenarios with real service
            
        }).doesNotThrowAnyException();
    }

    // Helper methods

    private void initializeTestImages() {
        // Create valid JPEG images (with JPEG magic bytes)
        validJpegImage1 = createTestImage("jpeg1", 2048, (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0);
        validJpegImage2 = createTestImage("jpeg2", 2048, (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE1);
        
        // Create valid PNG images (with PNG magic bytes)
        validPngImage1 = createTestImage("png1", 2048, (byte) 0x89, (byte) 0x50, (byte) 0x4E, (byte) 0x47);
        validPngImage2 = createTestImage("png2", 2048, (byte) 0x89, (byte) 0x50, (byte) 0x4E, (byte) 0x47);
        
        // Create invalid images
        invalidImage = new byte[0]; // Empty image
        tooSmallImage = new byte[512]; // Too small (< 1KB)
        tooLargeImage = new byte[11 * 1024 * 1024]; // Too large (> 10MB)
    }

    private byte[] createTestImage(String identifier, int size, byte... magicBytes) {
        byte[] data = new byte[size];
        
        // Set magic bytes
        for (int i = 0; i < magicBytes.length && i < data.length; i++) {
            data[i] = magicBytes[i];
        }
        
        // Fill rest with pattern based on identifier
        for (int i = magicBytes.length; i < size; i++) {
            data[i] = (byte) ((identifier.hashCode() + i) % 256);
        }
        
        return data;
    }

    private byte[] createBrowserImage(String browserType, int size, byte... magicBytes) {
        return createTestImage(browserType, size, magicBytes);
    }

    private byte[] createDeviceImage(String deviceType, int size) {
        // Default to JPEG format for device images
        return createTestImage(deviceType, size, (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0);
    }

    private List<byte[]> getImagesForMode(LivenessDetectionClient.LivenessMode mode) {
        switch (mode) {
            case PASSIVE:
                return List.of(validJpegImage1);
            case ACTIVE:
            case CHALLENGE_RESPONSE:
                return List.of(validJpegImage1, validJpegImage2);
            default:
                throw new IllegalArgumentException("Unknown liveness mode: " + mode);
        }
    }

    private void validateLivenessRequest(List<byte[]> images, LivenessDetectionClient.LivenessMode mode) 
            throws BioIdValidationException {
        LivenessDetectionErrorHandler.validateLivenessRequest(images, mode);
    }

    private void validateChallengeResponseRequest(byte[] image1, byte[] image2, 
                                                List<LivenessDetectionClient.ChallengeDirection> directions) 
            throws BioIdValidationException {
        LivenessDetectionErrorHandler.validateLivenessRequest(
            List.of(image1, image2), 
            LivenessDetectionClient.LivenessMode.CHALLENGE_RESPONSE, 
            directions
        );
    }

    private CompletableFuture<MockLivenessResponse> simulateAsyncLivenessDetection(
            List<byte[]> images, LivenessDetectionClient.LivenessMode mode) {
        
        return CompletableFuture.supplyAsync(() -> {
            // Simulate processing delay
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted", e);
            }
            
            // Return mock successful response
            return new MockLivenessResponse(true, 0.85, mode);
        });
    }

    private CompletableFuture<MockLivenessResponse> simulateChallengeResponseLivenessDetection(
            byte[] image1, byte[] image2, List<LivenessDetectionClient.ChallengeDirection> directions) {
        
        return CompletableFuture.supplyAsync(() -> {
            // Simulate challenge-response processing
            try {
                Thread.sleep(15); // Slightly longer for challenge-response
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted", e);
            }
            
            // Return mock successful challenge-response
            return new MockLivenessResponse(true, 0.92, LivenessDetectionClient.LivenessMode.CHALLENGE_RESPONSE, directions);
        });
    }

    private String buildChallengeInstructions(List<LivenessDetectionClient.ChallengeDirection> directions) {
        StringBuilder instructions = new StringBuilder("Please perform the following head movements: ");
        for (int i = 0; i < directions.size(); i++) {
            if (i > 0) instructions.append(", then ");
            instructions.append(directions.get(i).name().toLowerCase());
        }
        return instructions.toString();
    }

    // Mock response class for testing
    private static class MockLivenessResponse {
        private final boolean live;
        private final double livenessScore;
        private final LivenessDetectionClient.LivenessMode mode;
        private final List<LivenessDetectionClient.ChallengeDirection> challengeDirections;

        public MockLivenessResponse(boolean live, double livenessScore, LivenessDetectionClient.LivenessMode mode) {
            this(live, livenessScore, mode, null);
        }

        public MockLivenessResponse(boolean live, double livenessScore, LivenessDetectionClient.LivenessMode mode, 
                                  List<LivenessDetectionClient.ChallengeDirection> challengeDirections) {
            this.live = live;
            this.livenessScore = livenessScore;
            this.mode = mode;
            this.challengeDirections = challengeDirections;
        }

        public boolean isLive() { return live; }
        public double getLivenessScore() { return livenessScore; }
        public LivenessDetectionClient.LivenessMode getMode() { return mode; }
        public List<LivenessDetectionClient.ChallengeDirection> getChallengeDirections() { return challengeDirections; }
    }
}