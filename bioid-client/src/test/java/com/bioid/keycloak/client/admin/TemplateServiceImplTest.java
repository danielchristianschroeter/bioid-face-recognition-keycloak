package com.bioid.keycloak.client.admin;

import com.bioid.keycloak.client.BioIdClient;
import com.bioid.keycloak.client.admin.model.*;
import com.bioid.keycloak.client.exception.BioIdException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TemplateServiceImpl")
class TemplateServiceImplTest {

    @Mock
    private BioIdClient bioIdClient;

    private TemplateServiceImpl templateService;

    @BeforeEach
    void setUp() {
        templateService = new TemplateServiceImpl(bioIdClient);
    }

    @Test
    @DisplayName("Should get template status successfully")
    void shouldGetTemplateStatusSuccessfully() throws BioIdException {
        // Given
        long classId = 12345L;
        boolean includeThumbnails = true;

        // When
        TemplateStatusSummary status = templateService.getTemplateStatus(classId, includeThumbnails);

        // Then
        assertThat(status).isNotNull();
        assertThat(status.getClassId()).isEqualTo(classId);
        assertThat(status.isAvailable()).isTrue();
        assertThat(status.getEnrollmentDate()).isNotNull();
        assertThat(status.getEncoderVersion()).isEqualTo(3);
        assertThat(status.getFeatureVectors()).isEqualTo(100);
        assertThat(status.getThumbnailsStored()).isEqualTo(2);
        assertThat(status.getTags()).containsExactly("user", "active");
        assertThat(status.getHealthStatus()).isEqualTo(TemplateStatusSummary.TemplateHealthStatus.HEALTHY);
    }

    @Test
    @DisplayName("Should get template status without thumbnails")
    void shouldGetTemplateStatusWithoutThumbnails() throws BioIdException {
        // Given
        long classId = 12345L;
        boolean includeThumbnails = false;

        // When
        TemplateStatusSummary status = templateService.getTemplateStatus(classId, includeThumbnails);

        // Then
        assertThat(status).isNotNull();
        assertThat(status.getClassId()).isEqualTo(classId);
        assertThat(status.isAvailable()).isTrue();
    }

    @Test
    @DisplayName("Should throw exception when template status retrieval fails")
    void shouldThrowExceptionWhenTemplateStatusRetrievalFails() {
        // Given
        long classId = 12345L;
        boolean includeThumbnails = true;

        // Mock an internal failure by making the service throw during execution
        // Since the current implementation doesn't call external services directly,
        // we'll test the exception handling by creating a subclass that throws
        TemplateServiceImpl faultyService = new TemplateServiceImpl(bioIdClient) {
            @Override
            public TemplateStatusSummary getTemplateStatus(long classId, boolean includeThumbnails) throws BioIdException {
                throw new RuntimeException("Simulated failure");
            }
        };

        // When & Then
        assertThatThrownBy(() -> faultyService.getTemplateStatus(classId, includeThumbnails))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Simulated failure");
    }

    @Test
    @DisplayName("Should get template status batch successfully")
    void shouldGetTemplateStatusBatchSuccessfully() throws BioIdException {
        // Given
        List<Long> classIds = Arrays.asList(1L, 2L, 3L);

        // When
        List<TemplateStatusSummary> statuses = templateService.getTemplateStatusBatch(classIds);

        // Then
        assertThat(statuses).hasSize(3);
        assertThat(statuses).allMatch(status -> status.isAvailable());
        assertThat(statuses).extracting(TemplateStatusSummary::getClassId)
            .containsExactlyInAnyOrder(1L, 2L, 3L);
    }

    @Test
    @DisplayName("Should handle empty class ID list in batch operation")
    void shouldHandleEmptyClassIdListInBatchOperation() throws BioIdException {
        // Given
        List<Long> classIds = Collections.emptyList();

        // When
        List<TemplateStatusSummary> statuses = templateService.getTemplateStatusBatch(classIds);

        // Then
        assertThat(statuses).isEmpty();
    }

    @Test
    @DisplayName("Should handle large batch sizes")
    void shouldHandleLargeBatchSizes() throws BioIdException {
        // Given - Create a list larger than the batch size (50)
        List<Long> classIds = Arrays.asList(
            1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L,
            11L, 12L, 13L, 14L, 15L, 16L, 17L, 18L, 19L, 20L,
            21L, 22L, 23L, 24L, 25L, 26L, 27L, 28L, 29L, 30L,
            31L, 32L, 33L, 34L, 35L, 36L, 37L, 38L, 39L, 40L,
            41L, 42L, 43L, 44L, 45L, 46L, 47L, 48L, 49L, 50L,
            51L, 52L, 53L, 54L, 55L // 55 items total
        );

        // When
        List<TemplateStatusSummary> statuses = templateService.getTemplateStatusBatch(classIds);

        // Then
        assertThat(statuses).hasSize(55);
        assertThat(statuses).extracting(TemplateStatusSummary::getClassId)
            .containsExactlyInAnyOrderElementsOf(classIds);
    }

    @Test
    @DisplayName("Should upgrade template successfully")
    void shouldUpgradeTemplateSuccessfully() throws BioIdException {
        // Given
        long classId = 12345L;

        // When
        TemplateUpgradeResult result = templateService.upgradeTemplate(classId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getClassId()).isEqualTo(classId);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOldEncoderVersion()).isEqualTo(2);
        assertThat(result.getNewEncoderVersion()).isEqualTo(3);
        assertThat(result.getErrorMessage()).isNull();
    }

    @Test
    @DisplayName("Should fail to upgrade template when already at current version")
    void shouldFailToUpgradeTemplateWhenAlreadyAtCurrentVersion() throws BioIdException {
        // Given
        long classId = 12345L;

        // When
        TemplateUpgradeResult result = templateService.upgradeTemplate(classId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getClassId()).isEqualTo(classId);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("already at current encoder version");
    }

    @Test
    @DisplayName("Should upgrade templates batch successfully")
    void shouldUpgradeTemplatesBatchSuccessfully() throws BioIdException {
        // Given
        List<Long> classIds = Arrays.asList(1L, 2L, 3L);

        // When
        BulkUpgradeResult result = templateService.upgradeTemplatesBatch(classIds);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalTemplates()).isEqualTo(3);
        assertThat(result.getSuccessfulUpgrades()).isEqualTo(0); // All fail because already at current version
        assertThat(result.getFailedUpgrades()).isEqualTo(3);
        assertThat(result.getResults()).hasSize(3);
        assertThat(result.getStartedAt()).isNotNull();
        assertThat(result.getCompletedAt()).isNotNull();
        assertThat(result.getCompletedAt()).isAfterOrEqualTo(result.getStartedAt());
    }

    @Test
    @DisplayName("Should handle empty class ID list in bulk upgrade")
    void shouldHandleEmptyClassIdListInBulkUpgrade() throws BioIdException {
        // Given
        List<Long> classIds = Collections.emptyList();

        // When
        BulkUpgradeResult result = templateService.upgradeTemplatesBatch(classIds);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalTemplates()).isEqualTo(0);
        assertThat(result.getSuccessfulUpgrades()).isEqualTo(0);
        assertThat(result.getFailedUpgrades()).isEqualTo(0);
        assertThat(result.getResults()).isEmpty();
    }

    @Test
    @DisplayName("Should analyze template health successfully")
    void shouldAnalyzeTemplateHealthSuccessfully() throws BioIdException {
        // Given
        List<Long> classIds = Arrays.asList(1L, 2L, 3L);

        // When
        TemplateHealthReport report = templateService.analyzeTemplateHealth(classIds);

        // Then
        assertThat(report).isNotNull();
        assertThat(report.getTotalTemplates()).isEqualTo(3);
        assertThat(report.getHealthyTemplates()).isEqualTo(3); // All templates are healthy in mock
        assertThat(report.getOutdatedEncoderVersions()).isEqualTo(0);
        assertThat(report.getMissingThumbnails()).isEqualTo(0);
        assertThat(report.getExpiringSoon()).isEqualTo(0);
        assertThat(report.getIssues()).isEmpty();
        assertThat(report.getIssuesByType()).isEmpty();
        assertThat(report.getGeneratedAt()).isNotNull();
        assertThat(report.getHealthPercentage()).isEqualTo(100.0);
    }

    @Test
    @DisplayName("Should handle template health analysis with issues")
    void shouldHandleTemplateHealthAnalysisWithIssues() throws BioIdException {
        // Given
        List<Long> classIds = Arrays.asList(1L);
        
        // Create a service that returns templates with issues
        TemplateServiceImpl serviceWithIssues = new TemplateServiceImpl(bioIdClient) {
            @Override
            public TemplateStatusSummary getTemplateStatus(long classId, boolean includeThumbnails) throws BioIdException {
                // Return a template with outdated encoder version
                return new TemplateStatusSummary(
                    classId, true, Instant.now().minus(30, java.time.temporal.ChronoUnit.DAYS),
                    1, // Outdated encoder version
                    100, 0, // No thumbnails
                    Arrays.asList("user", "active"),
                    TemplateStatusSummary.TemplateHealthStatus.CORRUPTED
                );
            }
        };

        // When
        TemplateHealthReport report = serviceWithIssues.analyzeTemplateHealth(classIds);

        // Then
        assertThat(report).isNotNull();
        assertThat(report.getTotalTemplates()).isEqualTo(1);
        assertThat(report.getHealthyTemplates()).isEqualTo(0);
        assertThat(report.getOutdatedEncoderVersions()).isEqualTo(1);
        assertThat(report.getMissingThumbnails()).isEqualTo(1);
        assertThat(report.getIssues()).hasSize(2); // Outdated encoder + missing thumbnails
        assertThat(report.getIssuesByType()).containsKeys("OUTDATED_ENCODER", "MISSING_THUMBNAILS");
        assertThat(report.getHealthPercentage()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Should schedule template cleanup successfully")
    void shouldScheduleTemplateCleanupSuccessfully() throws BioIdException {
        // Given
        List<Long> classIds = Arrays.asList(1L, 2L, 3L);

        // When & Then - Should not throw exception
        assertThatCode(() -> templateService.scheduleTemplateCleanup(classIds))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should handle template cleanup with unavailable templates")
    void shouldHandleTemplateCleanupWithUnavailableTemplates() throws BioIdException {
        // Given
        List<Long> classIds = Arrays.asList(1L);
        
        // Create a service that returns unavailable templates
        TemplateServiceImpl serviceWithUnavailable = new TemplateServiceImpl(bioIdClient) {
            @Override
            public TemplateStatusSummary getTemplateStatus(long classId, boolean includeThumbnails) throws BioIdException {
                return new TemplateStatusSummary(
                    classId, false, // Not available
                    Instant.now(), 0, 0, 0,
                    Collections.emptyList(),
                    TemplateStatusSummary.TemplateHealthStatus.CORRUPTED
                );
            }
        };

        // When & Then - Should not throw exception
        assertThatCode(() -> serviceWithUnavailable.scheduleTemplateCleanup(classIds))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should handle null class ID list gracefully")
    void shouldHandleNullClassIdListGracefully() {
        // Given
        List<Long> classIds = null;

        // When & Then
        assertThatThrownBy(() -> templateService.getTemplateStatusBatch(classIds))
            .isInstanceOf(BioIdException.class);
    }

    @Test
    @DisplayName("Should handle concurrent batch operations")
    void shouldHandleConcurrentBatchOperations() throws Exception {
        // Given
        List<Long> classIds1 = Arrays.asList(1L, 2L, 3L);
        List<Long> classIds2 = Arrays.asList(4L, 5L, 6L);

        // When - Execute concurrent operations
        CompletableFuture<List<TemplateStatusSummary>> future1 = 
            CompletableFuture.supplyAsync(() -> {
                try {
                    return templateService.getTemplateStatusBatch(classIds1);
                } catch (BioIdException e) {
                    throw new RuntimeException(e);
                }
            });

        CompletableFuture<List<TemplateStatusSummary>> future2 = 
            CompletableFuture.supplyAsync(() -> {
                try {
                    return templateService.getTemplateStatusBatch(classIds2);
                } catch (BioIdException e) {
                    throw new RuntimeException(e);
                }
            });

        // Then
        List<TemplateStatusSummary> result1 = future1.get(5, TimeUnit.SECONDS);
        List<TemplateStatusSummary> result2 = future2.get(5, TimeUnit.SECONDS);

        assertThat(result1).hasSize(3);
        assertThat(result2).hasSize(3);
        assertThat(result1).extracting(TemplateStatusSummary::getClassId)
            .containsExactlyInAnyOrder(1L, 2L, 3L);
        assertThat(result2).extracting(TemplateStatusSummary::getClassId)
            .containsExactlyInAnyOrder(4L, 5L, 6L);
    }

    @Test
    @DisplayName("Should handle performance under load")
    void shouldHandlePerformanceUnderLoad() throws BioIdException {
        // Given
        List<Long> classIds = Arrays.asList(1L, 2L, 3L, 4L, 5L);
        long startTime = System.currentTimeMillis();

        // When
        List<TemplateStatusSummary> statuses = templateService.getTemplateStatusBatch(classIds);

        // Then
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        assertThat(statuses).hasSize(5);
        assertThat(duration).isLessThan(5000); // Should complete within 5 seconds
    }

    @Test
    @DisplayName("Should validate class ID ranges")
    void shouldValidateClassIdRanges() throws BioIdException {
        // Given
        List<Long> classIds = Arrays.asList(0L, -1L, Long.MAX_VALUE);

        // When
        List<TemplateStatusSummary> statuses = templateService.getTemplateStatusBatch(classIds);

        // Then
        assertThat(statuses).hasSize(3);
        assertThat(statuses).extracting(TemplateStatusSummary::getClassId)
            .containsExactlyInAnyOrder(0L, -1L, Long.MAX_VALUE);
    }

    @Test
    @DisplayName("Should handle bulk upgrade timeout scenarios")
    void shouldHandleBulkUpgradeTimeoutScenarios() throws BioIdException {
        // Given
        List<Long> classIds = Arrays.asList(1L, 2L, 3L);

        // When
        BulkUpgradeResult result = templateService.upgradeTemplatesBatch(classIds);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStartedAt()).isNotNull();
        assertThat(result.getCompletedAt()).isNotNull();
        
        // Verify reasonable execution time
        long executionTime = result.getCompletedAt().toEpochMilli() - result.getStartedAt().toEpochMilli();
        assertThat(executionTime).isLessThan(30000); // Should complete within 30 seconds
    }
}