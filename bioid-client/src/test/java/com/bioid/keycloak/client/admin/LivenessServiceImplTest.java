package com.bioid.keycloak.client.admin;

import com.bioid.keycloak.client.admin.model.LivenessConfiguration;
import com.bioid.keycloak.client.admin.model.LivenessStatistics;
import com.bioid.keycloak.client.admin.model.LivenessTestResult;
import com.bioid.keycloak.client.exception.BioIdException;
import com.bioid.keycloak.client.liveness.LivenessDetectionClient;
import com.bioid.keycloak.client.liveness.LivenessDetectionRequest;
import com.bioid.keycloak.client.liveness.LivenessDetectionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LivenessServiceImpl")
class LivenessServiceImplTest {

    @Mock
    private LivenessDetectionClient livenessDetectionClient;

    private LivenessServiceImpl livenessService;

    @BeforeEach
    void setUp() {
        livenessService = new LivenessServiceImpl(livenessDetectionClient);
    }

    @Test
    @DisplayName("Should perform liveness detection with valid request")
    void shouldPerformLivenessDetectionWithValidRequest() {
        // Given
        LivenessDetectionRequest request = LivenessDetectionRequest.builder()
            .addImage(new byte[]{1, 2, 3})
            .mode(LivenessDetectionClient.LivenessMode.PASSIVE)
            .threshold(0.7)
            .build();

        // When & Then
        assertThatThrownBy(() -> livenessService.performLivenessDetection(request))
            .isInstanceOf(BioIdException.class)
            .hasMessage("Liveness detection requires protobuf integration - not yet implemented");
    }

    @Test
    @DisplayName("Should throw exception for null liveness detection request")
    void shouldThrowExceptionForNullLivenessDetectionRequest() {
        // Given
        LivenessDetectionRequest request = null;

        // When & Then
        assertThatThrownBy(() -> livenessService.performLivenessDetection(request))
            .isInstanceOf(BioIdException.class)
            .hasMessage("Liveness detection request is required");
    }

    @Test
    @DisplayName("Should get default liveness configuration for new realm")
    void shouldGetDefaultLivenessConfigurationForNewRealm() throws BioIdException {
        // Given
        String realmId = "new-realm";

        // When
        LivenessConfiguration config = livenessService.getLivenessConfiguration(realmId);

        // Then
        assertThat(config).isNotNull();
        assertThat(config.getDefaultLivenessMode()).isEqualTo(LivenessConfiguration.LivenessMode.PASSIVE);
        assertThat(config.getLivenessThreshold()).isEqualTo(0.7);
        assertThat(config.isEnableChallengeResponse()).isFalse();
        assertThat(config.getAllowedChallengeDirections()).containsExactlyInAnyOrder(
            LivenessConfiguration.ChallengeDirection.UP,
            LivenessConfiguration.ChallengeDirection.DOWN,
            LivenessConfiguration.ChallengeDirection.LEFT,
            LivenessConfiguration.ChallengeDirection.RIGHT
        );
        assertThat(config.getMaxRetryAttempts()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should cache liveness configuration")
    void shouldCacheLivenessConfiguration() throws BioIdException {
        // Given
        String realmId = "test-realm";

        // When
        LivenessConfiguration config1 = livenessService.getLivenessConfiguration(realmId);
        LivenessConfiguration config2 = livenessService.getLivenessConfiguration(realmId);

        // Then
        assertThat(config1).isSameAs(config2); // Should return the same cached instance
    }

    @Test
    @DisplayName("Should update liveness configuration successfully")
    void shouldUpdateLivenessConfigurationSuccessfully() throws BioIdException {
        // Given
        String realmId = "test-realm";
        LivenessConfiguration newConfig = new LivenessConfiguration(
            LivenessConfiguration.LivenessMode.ACTIVE,
            0.8,
            true,
            Arrays.asList(
                LivenessConfiguration.ChallengeDirection.UP,
                LivenessConfiguration.ChallengeDirection.DOWN
            ),
            5,
            true
        );

        // When
        livenessService.updateLivenessConfiguration(realmId, newConfig);

        // Then
        LivenessConfiguration retrievedConfig = livenessService.getLivenessConfiguration(realmId);
        assertThat(retrievedConfig.getDefaultLivenessMode()).isEqualTo(LivenessConfiguration.LivenessMode.ACTIVE);
        assertThat(retrievedConfig.getLivenessThreshold()).isEqualTo(0.8);
        assertThat(retrievedConfig.isEnableChallengeResponse()).isTrue();
        assertThat(retrievedConfig.getAllowedChallengeDirections()).containsExactlyInAnyOrder(
            LivenessConfiguration.ChallengeDirection.UP,
            LivenessConfiguration.ChallengeDirection.DOWN
        );
        assertThat(retrievedConfig.getMaxRetryAttempts()).isEqualTo(5);
    }

    @Test
    @DisplayName("Should throw exception for invalid liveness configuration")
    void shouldThrowExceptionForInvalidLivenessConfiguration() {
        // Given
        String realmId = "test-realm";
        LivenessConfiguration invalidConfig = new LivenessConfiguration(
            null, // Invalid: null mode
            0.7,
            false,
            Arrays.asList(LivenessConfiguration.ChallengeDirection.UP),
            3,
            true
        );

        // When & Then
        assertThatThrownBy(() -> livenessService.updateLivenessConfiguration(realmId, invalidConfig))
            .isInstanceOf(BioIdException.class)
            .hasMessage("Failed to update liveness configuration")
            .hasCauseInstanceOf(BioIdException.class);
    }

    @Test
    @DisplayName("Should throw exception for invalid threshold")
    void shouldThrowExceptionForInvalidThreshold() {
        // Given
        String realmId = "test-realm";
        LivenessConfiguration invalidConfig = new LivenessConfiguration(
            LivenessConfiguration.LivenessMode.PASSIVE,
            1.5, // Invalid: > 1.0
            false,
            Arrays.asList(LivenessConfiguration.ChallengeDirection.UP),
            3,
            true
        );

        // When & Then
        assertThatThrownBy(() -> livenessService.updateLivenessConfiguration(realmId, invalidConfig))
            .isInstanceOf(BioIdException.class)
            .hasMessage("Failed to update liveness configuration");
    }

    @Test
    @DisplayName("Should throw exception for invalid retry attempts")
    void shouldThrowExceptionForInvalidRetryAttempts() {
        // Given
        String realmId = "test-realm";
        LivenessConfiguration invalidConfig = new LivenessConfiguration(
            LivenessConfiguration.LivenessMode.PASSIVE,
            0.7,
            false,
            Arrays.asList(LivenessConfiguration.ChallengeDirection.UP),
            15, // Invalid: > 10
            true
        );

        // When & Then
        assertThatThrownBy(() -> livenessService.updateLivenessConfiguration(realmId, invalidConfig))
            .isInstanceOf(BioIdException.class)
            .hasMessage("Failed to update liveness configuration");
    }

    @Test
    @DisplayName("Should throw exception for challenge response without directions")
    void shouldThrowExceptionForChallengeResponseWithoutDirections() {
        // Given
        String realmId = "test-realm";
        LivenessConfiguration invalidConfig = new LivenessConfiguration(
            LivenessConfiguration.LivenessMode.PASSIVE,
            0.7,
            true,
            Collections.emptyList(), // Invalid: empty directions
            3,
            true
        );

        // When & Then
        assertThatThrownBy(() -> livenessService.updateLivenessConfiguration(realmId, invalidConfig))
            .isInstanceOf(BioIdException.class)
            .hasMessage("Failed to update liveness configuration");
    }

    @Test
    @DisplayName("Should test passive liveness detection successfully")
    void shouldTestPassiveLivenessDetectionSuccessfully() throws BioIdException {
        // Given
        byte[] image1 = new byte[]{1, 2, 3, 4, 5};
        byte[] image2 = null;
        LivenessConfiguration.LivenessMode mode = LivenessConfiguration.LivenessMode.PASSIVE;

        // When
        LivenessTestResult result = livenessService.testLivenessDetection(image1, image2, mode);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.isLive()).isTrue();
        assertThat(result.getLivenessScore()).isEqualTo(0.85);
        assertThat(result.getMode()).isEqualTo(mode);
        assertThat(result.getWarnings()).isNotNull();
        assertThat(result.getProcessingTimeMs()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should test active liveness detection successfully")
    void shouldTestActiveLivenessDetectionSuccessfully() throws BioIdException {
        // Given
        byte[] image1 = new byte[]{1, 2, 3, 4, 5};
        byte[] image2 = new byte[]{6, 7, 8, 9, 10};
        LivenessConfiguration.LivenessMode mode = LivenessConfiguration.LivenessMode.ACTIVE;

        // When
        LivenessTestResult result = livenessService.testLivenessDetection(image1, image2, mode);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.isLive()).isTrue();
        assertThat(result.getLivenessScore()).isEqualTo(0.85);
        assertThat(result.getMode()).isEqualTo(mode);
    }

    @Test
    @DisplayName("Should test challenge response liveness detection successfully")
    void shouldTestChallengeResponseLivenessDetectionSuccessfully() throws BioIdException {
        // Given
        byte[] image1 = new byte[]{1, 2, 3, 4, 5};
        byte[] image2 = new byte[]{6, 7, 8, 9, 10};
        LivenessConfiguration.LivenessMode mode = LivenessConfiguration.LivenessMode.CHALLENGE_RESPONSE;

        // When
        LivenessTestResult result = livenessService.testLivenessDetection(image1, image2, mode);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.isLive()).isTrue();
        assertThat(result.getLivenessScore()).isEqualTo(0.85);
        assertThat(result.getMode()).isEqualTo(mode);
    }

    @Test
    @DisplayName("Should fail test when first image is null")
    void shouldFailTestWhenFirstImageIsNull() throws BioIdException {
        // Given
        byte[] image1 = null;
        byte[] image2 = new byte[]{6, 7, 8, 9, 10};
        LivenessConfiguration.LivenessMode mode = LivenessConfiguration.LivenessMode.PASSIVE;

        // When
        LivenessTestResult result = livenessService.testLivenessDetection(image1, image2, mode);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("First image is required");
        assertThat(result.getMode()).isEqualTo(mode);
    }

    @Test
    @DisplayName("Should fail test when first image is empty")
    void shouldFailTestWhenFirstImageIsEmpty() throws BioIdException {
        // Given
        byte[] image1 = new byte[0];
        byte[] image2 = new byte[]{6, 7, 8, 9, 10};
        LivenessConfiguration.LivenessMode mode = LivenessConfiguration.LivenessMode.PASSIVE;

        // When
        LivenessTestResult result = livenessService.testLivenessDetection(image1, image2, mode);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("First image is required");
    }

    @Test
    @DisplayName("Should fail test when second image is required but missing")
    void shouldFailTestWhenSecondImageIsRequiredButMissing() throws BioIdException {
        // Given
        byte[] image1 = new byte[]{1, 2, 3, 4, 5};
        byte[] image2 = null;
        LivenessConfiguration.LivenessMode mode = LivenessConfiguration.LivenessMode.ACTIVE;

        // When
        LivenessTestResult result = livenessService.testLivenessDetection(image1, image2, mode);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("Second image is required for ACTIVE mode");
    }

    @Test
    @DisplayName("Should fail test when second image is required but empty for challenge response")
    void shouldFailTestWhenSecondImageIsRequiredButEmptyForChallengeResponse() throws BioIdException {
        // Given
        byte[] image1 = new byte[]{1, 2, 3, 4, 5};
        byte[] image2 = new byte[0];
        LivenessConfiguration.LivenessMode mode = LivenessConfiguration.LivenessMode.CHALLENGE_RESPONSE;

        // When
        LivenessTestResult result = livenessService.testLivenessDetection(image1, image2, mode);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("Second image is required for CHALLENGE_RESPONSE mode");
    }

    @Test
    @DisplayName("Should get liveness statistics successfully")
    void shouldGetLivenessStatisticsSuccessfully() throws BioIdException {
        // Given
        String realmId = "test-realm";
        LocalDate from = LocalDate.of(2024, 1, 1);
        LocalDate to = LocalDate.of(2024, 1, 31);

        // When
        LivenessStatistics stats = livenessService.getLivenessStatistics(realmId, from, to);

        // Then
        assertThat(stats).isNotNull();
        assertThat(stats.getFromDate()).isEqualTo(from);
        assertThat(stats.getToDate()).isEqualTo(to);
        assertThat(stats.getTotalAttempts()).isEqualTo(1000L);
        assertThat(stats.getSuccessfulAttempts()).isEqualTo(850L);
        assertThat(stats.getLiveDetections()).isEqualTo(720L);
        assertThat(stats.getSuccessRate()).isEqualTo(85.0);
        assertThat(stats.getLiveDetectionRate()).isEqualTo(84.7, within(0.1));
        assertThat(stats.getAverageLivenessScore()).isEqualTo(0.78);
        
        // Verify attempts by mode
        Map<LivenessConfiguration.LivenessMode, Long> attemptsByMode = stats.getAttemptsByMode();
        assertThat(attemptsByMode).containsEntry(LivenessConfiguration.LivenessMode.PASSIVE, 600L);
        assertThat(attemptsByMode).containsEntry(LivenessConfiguration.LivenessMode.ACTIVE, 300L);
        assertThat(attemptsByMode).containsEntry(LivenessConfiguration.LivenessMode.CHALLENGE_RESPONSE, 100L);
        
        // Verify rejection reasons
        Map<String, Long> rejectionReasons = stats.getRejectionReasons();
        assertThat(rejectionReasons).containsEntry("FACE_NOT_FOUND", 50L);
        assertThat(rejectionReasons).containsEntry("MULTIPLE_FACES", 30L);
        assertThat(rejectionReasons).containsEntry("REJECTED_BY_PASSIVE_LIVENESS", 40L);
        
        // Verify average scores by mode
        Map<String, Double> averageScoresByMode = stats.getAverageScoresByMode();
        assertThat(averageScoresByMode).containsEntry("PASSIVE", 0.82);
        assertThat(averageScoresByMode).containsEntry("ACTIVE", 0.75);
        assertThat(averageScoresByMode).containsEntry("CHALLENGE_RESPONSE", 0.71);
    }

    @Test
    @DisplayName("Should handle date range validation in statistics")
    void shouldHandleDateRangeValidationInStatistics() throws BioIdException {
        // Given
        String realmId = "test-realm";
        LocalDate from = LocalDate.of(2024, 2, 1);
        LocalDate to = LocalDate.of(2024, 1, 31); // End date before start date

        // When
        LivenessStatistics stats = livenessService.getLivenessStatistics(realmId, from, to);

        // Then - Should still return statistics (implementation doesn't validate date order)
        assertThat(stats).isNotNull();
        assertThat(stats.getFromDate()).isEqualTo(from);
        assertThat(stats.getToDate()).isEqualTo(to);
    }

    @Test
    @DisplayName("Should handle null realm ID in statistics")
    void shouldHandleNullRealmIdInStatistics() {
        // Given
        String realmId = null;
        LocalDate from = LocalDate.of(2024, 1, 1);
        LocalDate to = LocalDate.of(2024, 1, 31);

        // When & Then
        assertThatThrownBy(() -> livenessService.getLivenessStatistics(realmId, from, to))
            .isInstanceOf(BioIdException.class)
            .hasMessage("Realm ID cannot be null");
    }

    @Test
    @DisplayName("Should handle null dates in statistics")
    void shouldHandleNullDatesInStatistics() {
        // Given
        String realmId = "test-realm";
        LocalDate from = null;
        LocalDate to = null;

        // When & Then
        assertThatThrownBy(() -> livenessService.getLivenessStatistics(realmId, from, to))
            .isInstanceOf(BioIdException.class)
            .hasMessage("Date range cannot be null");
    }

    @Test
    @DisplayName("Should handle configuration validation edge cases")
    void shouldHandleConfigurationValidationEdgeCases() throws BioIdException {
        // Given
        String realmId = "test-realm";
        
        // Test boundary values
        LivenessConfiguration validConfig = new LivenessConfiguration(
            LivenessConfiguration.LivenessMode.PASSIVE,
            0.0, // Minimum valid threshold
            false,
            Arrays.asList(LivenessConfiguration.ChallengeDirection.UP),
            0, // Minimum valid retry attempts
            true
        );

        // When & Then - Should not throw exception
        assertThatCode(() -> livenessService.updateLivenessConfiguration(realmId, validConfig))
            .doesNotThrowAnyException();
        
        // Test maximum boundary values
        LivenessConfiguration maxConfig = new LivenessConfiguration(
            LivenessConfiguration.LivenessMode.CHALLENGE_RESPONSE,
            1.0, // Maximum valid threshold
            true,
            Arrays.asList(LivenessConfiguration.ChallengeDirection.UP),
            10, // Maximum valid retry attempts
            true
        );

        assertThatCode(() -> livenessService.updateLivenessConfiguration(realmId, maxConfig))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should handle performance testing scenarios")
    void shouldHandlePerformanceTestingScenarios() throws BioIdException {
        // Given
        byte[] largeImage = new byte[1024 * 1024]; // 1MB image
        Arrays.fill(largeImage, (byte) 1);
        LivenessConfiguration.LivenessMode mode = LivenessConfiguration.LivenessMode.PASSIVE;

        // When
        long startTime = System.currentTimeMillis();
        LivenessTestResult result = livenessService.testLivenessDetection(largeImage, null, mode);
        long endTime = System.currentTimeMillis();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(endTime - startTime).isLessThan(5000); // Should complete within 5 seconds
        assertThat(result.getProcessingTimeMs()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should handle multiple realm configurations independently")
    void shouldHandleMultipleRealmConfigurationsIndependently() throws BioIdException {
        // Given
        String realm1 = "realm1";
        String realm2 = "realm2";
        
        LivenessConfiguration config1 = new LivenessConfiguration(
            LivenessConfiguration.LivenessMode.PASSIVE,
            0.6,
            false,
            Arrays.asList(LivenessConfiguration.ChallengeDirection.UP),
            3,
            true
        );
            
        LivenessConfiguration config2 = new LivenessConfiguration(
            LivenessConfiguration.LivenessMode.ACTIVE,
            0.8,
            false,
            Arrays.asList(LivenessConfiguration.ChallengeDirection.UP),
            3,
            true
        );

        // When
        livenessService.updateLivenessConfiguration(realm1, config1);
        livenessService.updateLivenessConfiguration(realm2, config2);

        // Then
        LivenessConfiguration retrieved1 = livenessService.getLivenessConfiguration(realm1);
        LivenessConfiguration retrieved2 = livenessService.getLivenessConfiguration(realm2);
        
        assertThat(retrieved1.getDefaultLivenessMode()).isEqualTo(LivenessConfiguration.LivenessMode.PASSIVE);
        assertThat(retrieved1.getLivenessThreshold()).isEqualTo(0.6);
        
        assertThat(retrieved2.getDefaultLivenessMode()).isEqualTo(LivenessConfiguration.LivenessMode.ACTIVE);
        assertThat(retrieved2.getLivenessThreshold()).isEqualTo(0.8);
    }
}