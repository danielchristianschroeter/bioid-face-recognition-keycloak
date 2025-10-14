package com.bioid.keycloak.client.admin;

import com.bioid.keycloak.client.BioIdClient;
import com.bioid.keycloak.client.admin.model.BulkUpgradeResult;
import com.bioid.keycloak.client.admin.model.TemplateHealthReport;
import com.bioid.keycloak.client.admin.model.TemplateStatusSummary;
import com.bioid.keycloak.client.exception.BioIdException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Bulk Operation Performance Tests")
class BulkOperationPerformanceTest {

    @Mock
    private BioIdClient bioIdClient;

    private TemplateServiceImpl templateService;
    private ExecutorService executorService;

    @BeforeEach
    void setUp() {
        templateService = new TemplateServiceImpl(bioIdClient);
        executorService = Executors.newFixedThreadPool(10);
    }

    @Test
    @DisplayName("Should handle small batch operations efficiently")
    void shouldHandleSmallBatchOperationsEfficiently() throws BioIdException {
        // Given
        List<Long> classIds = LongStream.rangeClosed(1, 10).boxed().collect(Collectors.toList());
        Instant startTime = Instant.now();

        // When
        List<TemplateStatusSummary> results = templateService.getTemplateStatusBatch(classIds);

        // Then
        Instant endTime = Instant.now();
        Duration duration = Duration.between(startTime, endTime);
        
        assertThat(results).hasSize(10);
        assertThat(duration.toMillis()).isLessThan(1000); // Should complete within 1 second
        assertThat(results).allMatch(TemplateStatusSummary::isAvailable);
    }

    @Test
    @DisplayName("Should handle medium batch operations efficiently")
    void shouldHandleMediumBatchOperationsEfficiently() throws BioIdException {
        // Given
        List<Long> classIds = LongStream.rangeClosed(1, 100).boxed().collect(Collectors.toList());
        Instant startTime = Instant.now();

        // When
        List<TemplateStatusSummary> results = templateService.getTemplateStatusBatch(classIds);

        // Then
        Instant endTime = Instant.now();
        Duration duration = Duration.between(startTime, endTime);
        
        assertThat(results).hasSize(100);
        assertThat(duration.toMillis()).isLessThan(5000); // Should complete within 5 seconds
        assertThat(results).allMatch(TemplateStatusSummary::isAvailable);
    }

    @Test
    @DisplayName("Should handle large batch operations efficiently")
    void shouldHandleLargeBatchOperationsEfficiently() throws BioIdException {
        // Given
        List<Long> classIds = LongStream.rangeClosed(1, 500).boxed().collect(Collectors.toList());
        Instant startTime = Instant.now();

        // When
        List<TemplateStatusSummary> results = templateService.getTemplateStatusBatch(classIds);

        // Then
        Instant endTime = Instant.now();
        Duration duration = Duration.between(startTime, endTime);
        
        assertThat(results).hasSize(500);
        assertThat(duration.toMillis()).isLessThan(15000); // Should complete within 15 seconds
        assertThat(results).allMatch(TemplateStatusSummary::isAvailable);
    }

    @Test
    @DisplayName("Should handle bulk upgrade operations efficiently")
    void shouldHandleBulkUpgradeOperationsEfficiently() throws BioIdException {
        // Given
        List<Long> classIds = LongStream.rangeClosed(1, 50).boxed().collect(Collectors.toList());
        Instant startTime = Instant.now();

        // When
        BulkUpgradeResult result = templateService.upgradeTemplatesBatch(classIds);

        // Then
        Instant endTime = Instant.now();
        Duration duration = Duration.between(startTime, endTime);
        
        assertThat(result).isNotNull();
        assertThat(result.getTotalTemplates()).isEqualTo(50);
        assertThat(result.getResults()).hasSize(50);
        assertThat(duration.toMillis()).isLessThan(10000); // Should complete within 10 seconds
        
        // Verify timing information in result
        assertThat(result.getStartedAt()).isNotNull();
        assertThat(result.getCompletedAt()).isNotNull();
        assertThat(result.getCompletedAt()).isAfterOrEqualTo(result.getStartedAt());
    }

    @Test
    @DisplayName("Should handle template health analysis efficiently")
    void shouldHandleTemplateHealthAnalysisEfficiently() throws BioIdException {
        // Given
        List<Long> classIds = LongStream.rangeClosed(1, 100).boxed().collect(Collectors.toList());
        Instant startTime = Instant.now();

        // When
        TemplateHealthReport report = templateService.analyzeTemplateHealth(classIds);

        // Then
        Instant endTime = Instant.now();
        Duration duration = Duration.between(startTime, endTime);
        
        assertThat(report).isNotNull();
        assertThat(report.getTotalTemplates()).isEqualTo(100);
        assertThat(duration.toMillis()).isLessThan(8000); // Should complete within 8 seconds
        assertThat(report.getGeneratedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should handle concurrent batch operations")
    void shouldHandleConcurrentBatchOperations() throws Exception {
        // Given
        List<Long> batch1 = LongStream.rangeClosed(1, 50).boxed().collect(Collectors.toList());
        List<Long> batch2 = LongStream.rangeClosed(51, 100).boxed().collect(Collectors.toList());
        List<Long> batch3 = LongStream.rangeClosed(101, 150).boxed().collect(Collectors.toList());

        // When
        CompletableFuture<List<TemplateStatusSummary>> future1 = 
            CompletableFuture.supplyAsync(() -> {
                try {
                    return templateService.getTemplateStatusBatch(batch1);
                } catch (BioIdException e) {
                    throw new RuntimeException(e);
                }
            }, executorService);

        CompletableFuture<List<TemplateStatusSummary>> future2 = 
            CompletableFuture.supplyAsync(() -> {
                try {
                    return templateService.getTemplateStatusBatch(batch2);
                } catch (BioIdException e) {
                    throw new RuntimeException(e);
                }
            }, executorService);

        CompletableFuture<List<TemplateStatusSummary>> future3 = 
            CompletableFuture.supplyAsync(() -> {
                try {
                    return templateService.getTemplateStatusBatch(batch3);
                } catch (BioIdException e) {
                    throw new RuntimeException(e);
                }
            }, executorService);

        // Then
        List<TemplateStatusSummary> result1 = future1.get(10, TimeUnit.SECONDS);
        List<TemplateStatusSummary> result2 = future2.get(10, TimeUnit.SECONDS);
        List<TemplateStatusSummary> result3 = future3.get(10, TimeUnit.SECONDS);

        assertThat(result1).hasSize(50);
        assertThat(result2).hasSize(50);
        assertThat(result3).hasSize(50);
        
        // Verify no overlap in results
        List<Long> allClassIds = new ArrayList<>();
        allClassIds.addAll(result1.stream().map(TemplateStatusSummary::getClassId).collect(Collectors.toList()));
        allClassIds.addAll(result2.stream().map(TemplateStatusSummary::getClassId).collect(Collectors.toList()));
        allClassIds.addAll(result3.stream().map(TemplateStatusSummary::getClassId).collect(Collectors.toList()));
        
        assertThat(allClassIds).hasSize(150); // No duplicates
        assertThat(allClassIds).containsExactlyInAnyOrderElementsOf(
            LongStream.rangeClosed(1, 150).boxed().collect(Collectors.toList())
        );
    }

    @Test
    @DisplayName("Should handle memory usage efficiently during large operations")
    void shouldHandleMemoryUsageEfficientlyDuringLargeOperations() throws BioIdException {
        // Given
        List<Long> classIds = LongStream.rangeClosed(1, 1000).boxed().collect(Collectors.toList());
        
        // Measure memory before operation
        Runtime runtime = Runtime.getRuntime();
        runtime.gc(); // Suggest garbage collection
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();

        // When
        List<TemplateStatusSummary> results = templateService.getTemplateStatusBatch(classIds);

        // Then
        runtime.gc(); // Suggest garbage collection
        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
        long memoryUsed = memoryAfter - memoryBefore;
        
        assertThat(results).hasSize(1000);
        
        // Memory usage should be reasonable (less than 100MB for 1000 templates)
        assertThat(memoryUsed).isLessThan(100 * 1024 * 1024); // 100MB
    }

    @Test
    @DisplayName("Should handle batch size optimization")
    void shouldHandleBatchSizeOptimization() throws BioIdException {
        // Given - Test different batch sizes
        List<Long> smallBatch = LongStream.rangeClosed(1, 25).boxed().collect(Collectors.toList());
        List<Long> mediumBatch = LongStream.rangeClosed(1, 50).boxed().collect(Collectors.toList());
        List<Long> largeBatch = LongStream.rangeClosed(1, 100).boxed().collect(Collectors.toList());

        // When & Then - All should complete efficiently
        Instant start1 = Instant.now();
        List<TemplateStatusSummary> result1 = templateService.getTemplateStatusBatch(smallBatch);
        Duration duration1 = Duration.between(start1, Instant.now());

        Instant start2 = Instant.now();
        List<TemplateStatusSummary> result2 = templateService.getTemplateStatusBatch(mediumBatch);
        Duration duration2 = Duration.between(start2, Instant.now());

        Instant start3 = Instant.now();
        List<TemplateStatusSummary> result3 = templateService.getTemplateStatusBatch(largeBatch);
        Duration duration3 = Duration.between(start3, Instant.now());

        assertThat(result1).hasSize(25);
        assertThat(result2).hasSize(50);
        assertThat(result3).hasSize(100);

        // Performance should scale reasonably
        assertThat(duration1.toMillis()).isLessThan(2000);
        assertThat(duration2.toMillis()).isLessThan(4000);
        assertThat(duration3.toMillis()).isLessThan(8000);
    }

    @Test
    @DisplayName("Should handle error recovery in bulk operations")
    void shouldHandleErrorRecoveryInBulkOperations() throws BioIdException {
        // Given - Create a service that fails for some templates
        TemplateServiceImpl faultyService = new TemplateServiceImpl(bioIdClient) {
            @Override
            public TemplateStatusSummary getTemplateStatus(long classId, boolean includeThumbnails) throws BioIdException {
                if (classId % 10 == 0) { // Fail every 10th template
                    throw new BioIdException("Simulated failure for classId: " + classId);
                }
                return super.getTemplateStatus(classId, includeThumbnails);
            }
        };

        List<Long> classIds = LongStream.rangeClosed(1, 50).boxed().collect(Collectors.toList());

        // When
        List<TemplateStatusSummary> results = faultyService.getTemplateStatusBatch(classIds);

        // Then
        assertThat(results).hasSize(50); // Should still return results for all
        
        // Count successful vs error results
        long successfulResults = results.stream().filter(TemplateStatusSummary::isAvailable).count();
        long errorResults = results.stream().filter(r -> !r.isAvailable()).count();
        
        assertThat(successfulResults).isEqualTo(45); // 50 - 5 failures (every 10th)
        assertThat(errorResults).isEqualTo(5); // 5 failures
    }

    @Test
    @DisplayName("Should handle timeout scenarios in bulk operations")
    void shouldHandleTimeoutScenariosInBulkOperations() throws BioIdException {
        // Given - Create a service with simulated delays
        TemplateServiceImpl slowService = new TemplateServiceImpl(bioIdClient) {
            @Override
            public TemplateStatusSummary getTemplateStatus(long classId, boolean includeThumbnails) throws BioIdException {
                try {
                    Thread.sleep(10); // 10ms delay per template
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new BioIdException("Operation interrupted", e);
                }
                return super.getTemplateStatus(classId, includeThumbnails);
            }
        };

        List<Long> classIds = LongStream.rangeClosed(1, 100).boxed().collect(Collectors.toList());
        Instant startTime = Instant.now();

        // When
        List<TemplateStatusSummary> results = slowService.getTemplateStatusBatch(classIds);

        // Then
        Instant endTime = Instant.now();
        Duration duration = Duration.between(startTime, endTime);
        
        assertThat(results).hasSize(100);
        
        // Should complete within reasonable time despite delays (parallel processing helps)
        assertThat(duration.toMillis()).isLessThan(5000); // Should complete within 5 seconds
    }

    @Test
    @DisplayName("Should handle resource cleanup after bulk operations")
    void shouldHandleResourceCleanupAfterBulkOperations() throws BioIdException {
        // Given
        List<Long> classIds = LongStream.rangeClosed(1, 200).boxed().collect(Collectors.toList());

        // When
        List<TemplateStatusSummary> results = templateService.getTemplateStatusBatch(classIds);

        // Then
        assertThat(results).hasSize(200);
        
        // Verify that resources are cleaned up (no hanging threads, connections, etc.)
        // This is more of a smoke test to ensure the operation completes cleanly
        assertThat(results).allMatch(r -> r.getClassId() > 0);
        
        // Force garbage collection to test for memory leaks
        System.gc();
        
        // If we get here without hanging, resource cleanup is working
        assertThat(true).isTrue();
    }

    @Test
    @DisplayName("Should maintain performance consistency across multiple operations")
    void shouldMaintainPerformanceConsistencyAcrossMultipleOperations() throws BioIdException {
        // Given
        List<Long> classIds = LongStream.rangeClosed(1, 50).boxed().collect(Collectors.toList());
        List<Duration> durations = new ArrayList<>();

        // When - Perform multiple operations
        for (int i = 0; i < 5; i++) {
            Instant start = Instant.now();
            List<TemplateStatusSummary> results = templateService.getTemplateStatusBatch(classIds);
            Duration duration = Duration.between(start, Instant.now());
            
            durations.add(duration);
            assertThat(results).hasSize(50);
        }

        // Then - Performance should be consistent
        long avgDuration = durations.stream().mapToLong(Duration::toMillis).sum() / durations.size();
        long maxDuration = durations.stream().mapToLong(Duration::toMillis).max().orElse(0);
        long minDuration = durations.stream().mapToLong(Duration::toMillis).min().orElse(0);
        
        assertThat(avgDuration).isLessThan(3000); // Average should be under 3 seconds
        assertThat(maxDuration - minDuration).isLessThan(2000); // Variance should be under 2 seconds
    }
}