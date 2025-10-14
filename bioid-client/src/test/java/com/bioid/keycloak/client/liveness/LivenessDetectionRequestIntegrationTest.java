package com.bioid.keycloak.client.liveness;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for LivenessDetectionRequest model and builder.
 * 
 * Tests the complete request construction workflow including:
 * - Builder pattern validation
 * - Protobuf conversion
 * - Different liveness modes
 * - Challenge-response scenarios
 */
@DisplayName("LivenessDetectionRequest Integration Tests")
class LivenessDetectionRequestIntegrationTest {

    private static final byte[] TEST_IMAGE_1 = createTestImage("image1", 1024);
    private static final byte[] TEST_IMAGE_2 = createTestImage("image2", 1024);
    private static final List<String> CHALLENGE_TAGS = Arrays.asList("UP", "DOWN");

    @Test
    @DisplayName("Should create passive liveness detection request")
    void shouldCreatePassiveLivenessDetectionRequest() {
        // When
        LivenessDetectionRequest request = LivenessDetectionRequest.passive(TEST_IMAGE_1);

        // Then
        assertThat(request).isNotNull();
        assertThat(request.getMode()).isEqualTo(LivenessDetectionClient.LivenessMode.PASSIVE);
        assertThat(request.getLiveImages()).hasSize(1);
        assertThat(request.getThreshold()).isEqualTo(0.7); // Default threshold
        assertThat(request.getChallengeDirections()).isEmpty();
        
        // Verify image data
        LivenessDetectionRequest.ImageData imageData = request.getLiveImages().get(0);
        assertThat(imageData.getImageBytes()).isEqualTo(TEST_IMAGE_1);
        assertThat(imageData.getTags()).isEmpty();
    }

    @Test
    @DisplayName("Should create active liveness detection request")
    void shouldCreateActiveLivenessDetectionRequest() {
        // When
        LivenessDetectionRequest request = LivenessDetectionRequest.active(TEST_IMAGE_1, TEST_IMAGE_2);

        // Then
        assertThat(request).isNotNull();
        assertThat(request.getMode()).isEqualTo(LivenessDetectionClient.LivenessMode.ACTIVE);
        assertThat(request.getLiveImages()).hasSize(2);
        assertThat(request.getThreshold()).isEqualTo(0.7);
        assertThat(request.getChallengeDirections()).isEmpty();
        
        // Verify both images
        assertThat(request.getLiveImages().get(0).getImageBytes()).isEqualTo(TEST_IMAGE_1);
        assertThat(request.getLiveImages().get(1).getImageBytes()).isEqualTo(TEST_IMAGE_2);
    }

    @Test
    @DisplayName("Should create challenge-response liveness detection request")
    void shouldCreateChallengeResponseLivenessDetectionRequest() {
        // Given
        List<LivenessDetectionClient.ChallengeDirection> directions = Arrays.asList(
            LivenessDetectionClient.ChallengeDirection.UP,
            LivenessDetectionClient.ChallengeDirection.DOWN
        );

        // When
        LivenessDetectionRequest request = LivenessDetectionRequest.challengeResponse(
            TEST_IMAGE_1, TEST_IMAGE_2, directions);

        // Then
        assertThat(request).isNotNull();
        assertThat(request.getMode()).isEqualTo(LivenessDetectionClient.LivenessMode.CHALLENGE_RESPONSE);
        assertThat(request.getLiveImages()).hasSize(2);
        assertThat(request.getChallengeDirections()).isEqualTo(directions);
        
        // Verify first image has no tags
        assertThat(request.getLiveImages().get(0).getTags()).isEmpty();
        
        // Verify second image has challenge tags
        assertThat(request.getLiveImages().get(1).getTags()).containsExactly("UP", "DOWN");
    }

    @Test
    @DisplayName("Should build request using builder pattern")
    void shouldBuildRequestUsingBuilderPattern() {
        // When
        LivenessDetectionRequest request = LivenessDetectionRequest.builder()
            .addImage(TEST_IMAGE_1)
            .addImage(TEST_IMAGE_2)
            .mode(LivenessDetectionClient.LivenessMode.ACTIVE)
            .threshold(0.8)
            .build();

        // Then
        assertThat(request).isNotNull();
        assertThat(request.getMode()).isEqualTo(LivenessDetectionClient.LivenessMode.ACTIVE);
        assertThat(request.getLiveImages()).hasSize(2);
        assertThat(request.getThreshold()).isEqualTo(0.8);
    }

    @Test
    @DisplayName("Should build request with tagged images")
    void shouldBuildRequestWithTaggedImages() {
        // When
        LivenessDetectionRequest request = LivenessDetectionRequest.builder()
            .addImage(TEST_IMAGE_1)
            .addImageWithTags(TEST_IMAGE_2, CHALLENGE_TAGS)
            .mode(LivenessDetectionClient.LivenessMode.CHALLENGE_RESPONSE)
            .challengeDirections(Arrays.asList(
                LivenessDetectionClient.ChallengeDirection.UP,
                LivenessDetectionClient.ChallengeDirection.DOWN
            ))
            .build();

        // Then
        assertThat(request).isNotNull();
        assertThat(request.getLiveImages()).hasSize(2);
        assertThat(request.getLiveImages().get(0).getTags()).isEmpty();
        assertThat(request.getLiveImages().get(1).getTags()).isEqualTo(CHALLENGE_TAGS);
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.0, 0.3, 0.5, 0.7, 0.9, 1.0})
    @DisplayName("Should accept valid threshold values")
    void shouldAcceptValidThresholdValues(double threshold) {
        // When & Then
        assertThatCode(() -> {
            LivenessDetectionRequest request = LivenessDetectionRequest.builder()
                .addImage(TEST_IMAGE_1)
                .threshold(threshold)
                .build();
            
            assertThat(request.getThreshold()).isEqualTo(threshold);
        }).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(doubles = {-0.1, 1.1, 2.0, -1.0})
    @DisplayName("Should reject invalid threshold values")
    void shouldRejectInvalidThresholdValues(double threshold) {
        // When & Then
        assertThatThrownBy(() -> {
            LivenessDetectionRequest.builder()
                .addImage(TEST_IMAGE_1)
                .threshold(threshold)
                .build();
        }).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Threshold must be between 0.0 and 1.0");
    }

    @Test
    @DisplayName("Should validate passive mode requirements")
    void shouldValidatePassiveModeRequirements() {
        // Valid passive mode (1 image)
        assertThatCode(() -> {
            LivenessDetectionRequest.builder()
                .addImage(TEST_IMAGE_1)
                .mode(LivenessDetectionClient.LivenessMode.PASSIVE)
                .build();
        }).doesNotThrowAnyException();

        // Invalid passive mode (2 images)
        assertThatThrownBy(() -> {
            LivenessDetectionRequest.builder()
                .addImage(TEST_IMAGE_1)
                .addImage(TEST_IMAGE_2)
                .mode(LivenessDetectionClient.LivenessMode.PASSIVE)
                .build();
        }).isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Passive mode requires exactly 1 image");
    }

    @Test
    @DisplayName("Should validate active mode requirements")
    void shouldValidateActiveModeRequirements() {
        // Valid active mode (2 images)
        assertThatCode(() -> {
            LivenessDetectionRequest.builder()
                .addImage(TEST_IMAGE_1)
                .addImage(TEST_IMAGE_2)
                .mode(LivenessDetectionClient.LivenessMode.ACTIVE)
                .build();
        }).doesNotThrowAnyException();

        // Invalid active mode (1 image)
        assertThatThrownBy(() -> {
            LivenessDetectionRequest.builder()
                .addImage(TEST_IMAGE_1)
                .mode(LivenessDetectionClient.LivenessMode.ACTIVE)
                .build();
        }).isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("ACTIVE mode requires exactly 2 images");
    }

    @Test
    @DisplayName("Should validate challenge-response mode requirements")
    void shouldValidateChallengeResponseModeRequirements() {
        // Valid challenge-response mode
        assertThatCode(() -> {
            LivenessDetectionRequest.builder()
                .addImage(TEST_IMAGE_1)
                .addImage(TEST_IMAGE_2)
                .mode(LivenessDetectionClient.LivenessMode.CHALLENGE_RESPONSE)
                .challengeDirections(Arrays.asList(LivenessDetectionClient.ChallengeDirection.UP))
                .build();
        }).doesNotThrowAnyException();

        // Invalid challenge-response mode (no directions)
        assertThatThrownBy(() -> {
            LivenessDetectionRequest.builder()
                .addImage(TEST_IMAGE_1)
                .addImage(TEST_IMAGE_2)
                .mode(LivenessDetectionClient.LivenessMode.CHALLENGE_RESPONSE)
                .build();
        }).isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Challenge-response mode requires challenge directions");
    }

    @Test
    @DisplayName("Should validate image requirements")
    void shouldValidateImageRequirements() {
        // No images
        assertThatThrownBy(() -> {
            LivenessDetectionRequest.builder().build();
        }).isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("At least one image is required");

        // Too many images
        assertThatThrownBy(() -> {
            LivenessDetectionRequest.builder()
                .addImage(TEST_IMAGE_1)
                .addImage(TEST_IMAGE_2)
                .addImage(createTestImage("image3", 512))
                .build();
        }).isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Maximum 2 images allowed");

        // Null image
        assertThatThrownBy(() -> {
            new LivenessDetectionRequest.ImageData(null);
        }).isInstanceOf(NullPointerException.class)
          .hasMessageContaining("Image bytes cannot be null");

        // Empty image
        assertThatThrownBy(() -> {
            new LivenessDetectionRequest.ImageData(new byte[0]);
        }).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Image bytes cannot be empty");
    }

    @Test
    @DisplayName("Should validate request structure correctly")
    void shouldValidateRequestStructureCorrectly() {
        // Given
        List<LivenessDetectionClient.ChallengeDirection> directions = Arrays.asList(
            LivenessDetectionClient.ChallengeDirection.UP,
            LivenessDetectionClient.ChallengeDirection.DOWN
        );
        
        LivenessDetectionRequest request = LivenessDetectionRequest.builder()
            .addImage(TEST_IMAGE_1)
            .addImageWithTags(TEST_IMAGE_2, CHALLENGE_TAGS)
            .mode(LivenessDetectionClient.LivenessMode.CHALLENGE_RESPONSE)
            .challengeDirections(directions)
            .threshold(0.8)
            .build();

        // Then - Validate request structure
        assertThat(request).isNotNull();
        assertThat(request.getLiveImages()).hasSize(2);
        
        // Verify first image (no tags)
        assertThat(request.getLiveImages().get(0).getImageBytes()).isEqualTo(TEST_IMAGE_1);
        assertThat(request.getLiveImages().get(0).getTags()).isEmpty();
        
        // Verify second image (with tags)
        assertThat(request.getLiveImages().get(1).getImageBytes()).isEqualTo(TEST_IMAGE_2);
        assertThat(request.getLiveImages().get(1).getTags()).isEqualTo(CHALLENGE_TAGS);
    }

    @Test
    @DisplayName("Should handle equals and hashCode correctly")
    void shouldHandleEqualsAndHashCodeCorrectly() {
        // Given
        LivenessDetectionRequest request1 = LivenessDetectionRequest.passive(TEST_IMAGE_1);
        LivenessDetectionRequest request2 = LivenessDetectionRequest.passive(TEST_IMAGE_1);
        LivenessDetectionRequest request3 = LivenessDetectionRequest.passive(TEST_IMAGE_2);

        // Then
        assertThat(request1).isEqualTo(request2);
        assertThat(request1).isNotEqualTo(request3);
        assertThat(request1.hashCode()).isEqualTo(request2.hashCode());
        assertThat(request1.hashCode()).isNotEqualTo(request3.hashCode());
    }

    @Test
    @DisplayName("Should handle toString correctly")
    void shouldHandleToStringCorrectly() {
        // Given
        LivenessDetectionRequest request = LivenessDetectionRequest.builder()
            .addImage(TEST_IMAGE_1)
            .mode(LivenessDetectionClient.LivenessMode.PASSIVE)
            .threshold(0.8)
            .build();

        // When
        String toString = request.toString();

        // Then
        assertThat(toString).contains("LivenessDetectionRequest");
        assertThat(toString).contains("imageCount=1");
        assertThat(toString).contains("mode=PASSIVE");
        assertThat(toString).contains("threshold=0.8");
    }

    @Test
    @DisplayName("Should handle ImageData equals and hashCode correctly")
    void shouldHandleImageDataEqualsAndHashCodeCorrectly() {
        // Given
        LivenessDetectionRequest.ImageData imageData1 = new LivenessDetectionRequest.ImageData(TEST_IMAGE_1);
        LivenessDetectionRequest.ImageData imageData2 = new LivenessDetectionRequest.ImageData(TEST_IMAGE_1);
        LivenessDetectionRequest.ImageData imageData3 = new LivenessDetectionRequest.ImageData(TEST_IMAGE_2);
        LivenessDetectionRequest.ImageData imageDataWithTags = new LivenessDetectionRequest.ImageData(TEST_IMAGE_1, CHALLENGE_TAGS);

        // Then
        assertThat(imageData1).isEqualTo(imageData2);
        assertThat(imageData1).isNotEqualTo(imageData3);
        assertThat(imageData1).isNotEqualTo(imageDataWithTags);
        assertThat(imageData1.hashCode()).isEqualTo(imageData2.hashCode());
    }

    @Test
    @DisplayName("Should handle ImageData toString correctly")
    void shouldHandleImageDataToStringCorrectly() {
        // Given
        LivenessDetectionRequest.ImageData imageData = new LivenessDetectionRequest.ImageData(TEST_IMAGE_1, CHALLENGE_TAGS);

        // When
        String toString = imageData.toString();

        // Then
        assertThat(toString).contains("ImageData");
        assertThat(toString).contains("imageSize=" + TEST_IMAGE_1.length);
        assertThat(toString).contains("tags=" + CHALLENGE_TAGS);
    }

    @Test
    @DisplayName("Should clone image bytes to prevent mutation")
    void shouldCloneImageBytesToPreventMutation() {
        // Given
        byte[] originalImage = TEST_IMAGE_1.clone();
        LivenessDetectionRequest.ImageData imageData = new LivenessDetectionRequest.ImageData(originalImage);

        // When
        byte[] retrievedImage = imageData.getImageBytes();
        retrievedImage[0] = (byte) (retrievedImage[0] + 1); // Mutate retrieved array

        // Then
        assertThat(imageData.getImageBytes()).isEqualTo(originalImage); // Original should be unchanged
        assertThat(imageData.getImageBytes()).isNotSameAs(retrievedImage); // Should be different instances
    }

    private static byte[] createTestImage(String identifier, int size) {
        byte[] data = new byte[size];
        for (int i = 0; i < size; i++) {
            data[i] = (byte) ((identifier.hashCode() + i) % 256);
        }
        return data;
    }
}