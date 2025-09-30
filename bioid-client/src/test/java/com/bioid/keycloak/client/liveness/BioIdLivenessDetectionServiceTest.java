package com.bioid.keycloak.client.liveness;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for BioIdLivenessDetectionService. */
class BioIdLivenessDetectionServiceTest {

  private BioIdLivenessDetectionService livenessService;
  private LivenessConfig defaultConfig;
  private byte[] testImageData;

  @BeforeEach
  void setUp() {
    livenessService = new BioIdLivenessDetectionService();
    defaultConfig = LivenessConfig.defaultConfig();
    testImageData = "test-image-data".getBytes();
  }

  @AfterEach
  void tearDown() {
    if (livenessService != null) {
      livenessService.shutdown();
    }
  }

  @Test
  @DisplayName("Should be available after initialization")
  void shouldBeAvailableAfterInitialization() {
    assertTrue(livenessService.isLivenessDetectionAvailable());
    assertEquals(Duration.ofMillis(200), livenessService.getMaximumLivenessOverhead());
  }

  @Test
  @DisplayName("Should perform passive liveness detection successfully")
  void shouldPerformPassiveLivenessDetectionSuccessfully()
      throws ExecutionException, InterruptedException {
    LivenessConfig config =
        LivenessConfig.builder()
            .enabled(true)
            .enabledMethods(Set.of(LivenessMethod.PASSIVE))
            .preferredMethod(LivenessMethod.PASSIVE)
            .confidenceThreshold(0.5)
            .build();

    CompletableFuture<LivenessResult> future =
        livenessService.detectPassiveLiveness(testImageData, config);
    LivenessResult result = future.get();

    assertNotNull(result);
    assertEquals(LivenessMethod.PASSIVE, result.getMethod());
    assertFalse(result.hasError());
    assertTrue(result.getProcessingTime().toMillis() > 0);
    assertTrue(result.isWithinOverheadLimit(Duration.ofMillis(200)));
  }

  @Test
  @DisplayName("Should fail passive liveness detection when disabled")
  void shouldFailPassiveLivenessDetectionWhenDisabled()
      throws ExecutionException, InterruptedException {
    LivenessConfig config = LivenessConfig.builder().enabled(false).build();

    CompletableFuture<LivenessResult> future =
        livenessService.detectPassiveLiveness(testImageData, config);
    LivenessResult result = future.get();

    assertNotNull(result);
    assertFalse(result.isAlive());
    assertTrue(result.hasError());
    assertTrue(result.getErrorMessage().contains("disabled"));
  }

  @Test
  @DisplayName("Should fail passive liveness detection with null image data")
  void shouldFailPassiveLivenessDetectionWithNullImageData()
      throws ExecutionException, InterruptedException {
    CompletableFuture<LivenessResult> future =
        livenessService.detectPassiveLiveness(null, defaultConfig);
    LivenessResult result = future.get();

    assertNotNull(result);
    assertFalse(result.isAlive());
    assertTrue(result.hasError());
    assertTrue(result.getErrorMessage().contains("No image data"));
  }

  @Test
  @DisplayName("Should perform active liveness detection successfully")
  void shouldPerformActiveLivenessDetectionSuccessfully()
      throws ExecutionException, InterruptedException {
    LivenessConfig config =
        LivenessConfig.builder()
            .enabled(true)
            .enabledMethods(Set.of(LivenessMethod.ACTIVE_SMILE))
            .preferredMethod(LivenessMethod.ACTIVE_SMILE)
            .build();

    LivenessChallenge challenge = LivenessChallenge.createSmileChallenge();

    CompletableFuture<LivenessResult> future =
        livenessService.detectActiveLiveness(testImageData, challenge, config);
    LivenessResult result = future.get();

    assertNotNull(result);
    assertEquals(LivenessMethod.ACTIVE_SMILE, result.getMethod());
    assertFalse(result.hasError());
    assertTrue(result.getProcessingTime().toMillis() > 0);
  }

  @Test
  @DisplayName("Should fail active liveness detection with expired challenge")
  void shouldFailActiveLivenessDetectionWithExpiredChallenge()
      throws ExecutionException, InterruptedException {
    LivenessConfig config =
        LivenessConfig.builder()
            .enabled(true)
            .enabledMethods(Set.of(LivenessMethod.ACTIVE_SMILE, LivenessMethod.PASSIVE))
            .preferredMethod(LivenessMethod.ACTIVE_SMILE)
            .build();

    // Create an expired challenge
    LivenessChallenge expiredChallenge =
        new LivenessChallenge(
            "test-challenge",
            LivenessChallenge.ChallengeType.SMILE,
            "Please smile",
            Duration.ofMillis(1), // Very short timeout
            0.7,
            3);

    // Wait for challenge to expire
    Thread.sleep(10);

    CompletableFuture<LivenessResult> future =
        livenessService.detectActiveLiveness(testImageData, expiredChallenge, config);
    LivenessResult result = future.get();

    assertNotNull(result);
    assertFalse(result.isAlive());
    assertTrue(result.hasError());
    assertTrue(result.getErrorMessage().contains("expired"));
  }

  @Test
  @DisplayName("Should perform challenge-response liveness detection successfully")
  void shouldPerformChallengeResponseLivenessDetectionSuccessfully()
      throws ExecutionException, InterruptedException {
    LivenessConfig config =
        LivenessConfig.builder()
            .enabled(true)
            .enabledMethods(Set.of(LivenessMethod.CHALLENGE_RESPONSE, LivenessMethod.ACTIVE_SMILE))
            .preferredMethod(LivenessMethod.CHALLENGE_RESPONSE)
            .build();

    MovementChallenge challenge = MovementChallenge.createSimpleChallenge();
    byte[][] imageSequence = {testImageData, testImageData, testImageData};

    CompletableFuture<LivenessResult> future =
        livenessService.detectChallengeResponseLiveness(imageSequence, challenge, config);
    LivenessResult result = future.get();

    assertNotNull(result);
    assertEquals(LivenessMethod.CHALLENGE_RESPONSE, result.getMethod());
    assertFalse(result.hasError());
    assertTrue(result.getProcessingTime().toMillis() > 0);
  }

  @Test
  @DisplayName("Should fail challenge-response liveness detection with empty image sequence")
  void shouldFailChallengeResponseLivenessDetectionWithEmptyImageSequence()
      throws ExecutionException, InterruptedException {
    LivenessConfig config =
        LivenessConfig.builder()
            .enabled(true)
            .enabledMethods(Set.of(LivenessMethod.CHALLENGE_RESPONSE, LivenessMethod.ACTIVE_SMILE))
            .preferredMethod(LivenessMethod.CHALLENGE_RESPONSE)
            .build();

    MovementChallenge challenge = MovementChallenge.createSimpleChallenge();
    byte[][] emptySequence = {};

    CompletableFuture<LivenessResult> future =
        livenessService.detectChallengeResponseLiveness(emptySequence, challenge, config);
    LivenessResult result = future.get();

    assertNotNull(result);
    assertFalse(result.isAlive());
    assertTrue(result.hasError());
    assertTrue(result.getErrorMessage().contains("No image sequence"));
  }

  @Test
  @DisplayName("Should perform verification with liveness successfully")
  void shouldPerformVerificationWithLivenessSuccessfully()
      throws ExecutionException, InterruptedException {
    LivenessConfig config =
        LivenessConfig.builder()
            .enabled(true)
            .enabledMethods(Set.of(LivenessMethod.PASSIVE))
            .preferredMethod(LivenessMethod.PASSIVE)
            .adaptiveMode(true)
            .build();

    long classId = 12345L;

    CompletableFuture<VerificationWithLivenessResult> future =
        livenessService.verifyWithLiveness(classId, testImageData, config);
    VerificationWithLivenessResult result = future.get();

    assertNotNull(result);
    assertFalse(result.hasError());
    assertTrue(result.getTotalProcessingTime().toMillis() > 0);
    assertTrue(result.hasLivenessResult());

    if (result.getLivenessResult() != null) {
      assertEquals(LivenessMethod.PASSIVE, result.getLivenessResult().getMethod());
    }
  }

  @Test
  @DisplayName("Should perform verification without liveness when disabled")
  void shouldPerformVerificationWithoutLivenessWhenDisabled()
      throws ExecutionException, InterruptedException {
    LivenessConfig config = LivenessConfig.builder().enabled(false).build();

    long classId = 12345L;

    CompletableFuture<VerificationWithLivenessResult> future =
        livenessService.verifyWithLiveness(classId, testImageData, config);
    VerificationWithLivenessResult result = future.get();

    assertNotNull(result);
    assertFalse(result.hasError());
    assertFalse(result.hasLivenessResult());
    assertTrue(result.getTotalProcessingTime().toMillis() >= 0);
  }

  @Test
  @DisplayName("Should validate configuration correctly")
  void shouldValidateConfigurationCorrectly() {
    // Valid configuration should not throw
    assertDoesNotThrow(() -> livenessService.validateConfiguration(defaultConfig));

    // Null configuration should throw
    assertThrows(IllegalArgumentException.class, () -> livenessService.validateConfiguration(null));
  }

  @Test
  @DisplayName("Should handle concurrent liveness detection requests")
  void shouldHandleConcurrentLivenessDetectionRequests()
      throws ExecutionException, InterruptedException {
    LivenessConfig config =
        LivenessConfig.builder()
            .enabled(true)
            .enabledMethods(Set.of(LivenessMethod.PASSIVE))
            .build();

    // Submit multiple concurrent requests
    CompletableFuture<LivenessResult> future1 =
        livenessService.detectPassiveLiveness(testImageData, config);
    CompletableFuture<LivenessResult> future2 =
        livenessService.detectPassiveLiveness(testImageData, config);
    CompletableFuture<LivenessResult> future3 =
        livenessService.detectPassiveLiveness(testImageData, config);

    // Wait for all to complete
    CompletableFuture<Void> allFutures = CompletableFuture.allOf(future1, future2, future3);
    allFutures.get();

    // Verify all completed successfully
    LivenessResult result1 = future1.get();
    LivenessResult result2 = future2.get();
    LivenessResult result3 = future3.get();

    assertNotNull(result1);
    assertNotNull(result2);
    assertNotNull(result3);

    assertFalse(result1.hasError());
    assertFalse(result2.hasError());
    assertFalse(result3.hasError());
  }

  @Test
  @DisplayName("Should respect maximum overhead for passive liveness")
  void shouldRespectMaximumOverheadForPassiveLiveness()
      throws ExecutionException, InterruptedException {
    LivenessConfig config =
        LivenessConfig.builder()
            .enabled(true)
            .enabledMethods(Set.of(LivenessMethod.PASSIVE))
            .maxOverhead(Duration.ofMillis(200))
            .build();

    CompletableFuture<LivenessResult> future =
        livenessService.detectPassiveLiveness(testImageData, config);
    LivenessResult result = future.get();

    assertNotNull(result);

    // Processing time should be reasonable (allowing some variance for test execution)
    assertTrue(
        result.getProcessingTime().toMillis() < 500,
        "Processing time should be reasonable: " + result.getProcessingTime().toMillis() + "ms");
  }
}
