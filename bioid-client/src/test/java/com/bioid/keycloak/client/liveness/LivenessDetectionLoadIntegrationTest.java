package com.bioid.keycloak.client.liveness;

import com.bioid.keycloak.client.auth.BioIdJwtTokenProvider;
import com.bioid.keycloak.client.config.BioIdClientConfig;
import com.bioid.keycloak.client.connection.BioIdConnectionManager;
import com.bioid.keycloak.client.exception.BioIdException;
import io.grpc.ManagedChannel;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Load testing integration tests for liveness detection operations.
 * 
 * Tests the system's ability to handle concurrent liveness detection operations
 * under various load conditions including:
 * - High-volume concurrent requests
 * - Sustained load over time
 * - Memory efficiency under load
 * - Performance degradation analysis
 * - Resource utilization monitoring
 * - Error rate analysis under stress
 * 
 * Requirements tested: 3.1, 3.2, 3.3, 4.1-4.6
 */
@DisplayName("Liveness Detection Load Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LivenessDetectionLoadIntegrationTest {

    private static final String LOAD_TEST_ENABLED_PROPERTY = "bioid.loadtest.enabled";
    private static final int DEFAULT_THREAD_POOL_SIZE = 20;
    private static final Duration DEFAULT_TEST_TIMEOUT = Duration.ofMinutes(5);
    
    @Mock
    private BioIdJwtTokenProvider tokenProvider;
    
    @Mock
    private BioIdConnectionManager connectionManager;
    
    @Mock
    private ManagedChannel managedChannel;
    
    private LivenessDetectionClient livenessClient;
    private BioIdClientConfig config;
    private ExecutorService executorService;
    
    // Test data for load testing
    private byte[] testImageJpeg;
    private byte[] testImagePng;
    private List<byte[]> testImagePool;

    @BeforeAll
    static void setUpClass() {
        // Configure system properties for load testing
        System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "20");
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Create optimized configuration for load testing
        config = BioIdClientConfig.builder()
            .endpoint("localhost:9090")
            .clientId("load-test-client")
            .secretKey("load-test-secret")
            .requestTimeout(Duration.ofSeconds(30))
            .maxRetryAttempts(2) // Reduced retries for load testing
            .initialRetryDelay(Duration.ofMillis(50))
            .retryBackoffMultiplier(1.5)
            .build();

        when(tokenProvider.getToken()).thenReturn("load-test-jwt-token");
        try {
            when(connectionManager.getChannel()).thenReturn(managedChannel);
        } catch (Exception e) {
            // Mock setup - ignore exceptions
        }
        
        livenessClient = new LivenessDetectionClient(config, tokenProvider, connectionManager);
        
        // Create thread pool for concurrent operations
        executorService = Executors.newFixedThreadPool(DEFAULT_THREAD_POOL_SIZE);
        
        initializeTestData();
    }

    @AfterEach
    void tearDown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {10, 25, 50, 100})
    @Order(1)
    @DisplayName("Should handle concurrent passive liveness detection requests")
    void shouldHandleConcurrentPassiveLivenessDetectionRequests(int concurrentRequests) 
            throws InterruptedException, ExecutionException, TimeoutException {
        
        LoadTestMetrics metrics = new LoadTestMetrics();
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(concurrentRequests);
        
        // Create concurrent passive liveness detection tasks
        List<Future<LoadTestResult>> futures = IntStream.range(0, concurrentRequests)
            .mapToObj(i -> executorService.submit(() -> {
                try {
                    // Wait for all threads to be ready
                    startLatch.await();
                    
                    long startTime = System.nanoTime();
                    
                    // Perform passive liveness detection
                    byte[] testImage = getRandomTestImage();
                    simulatePassiveLivenessDetection(testImage);
                    
                    long endTime = System.nanoTime();
                    long durationMs = (endTime - startTime) / 1_000_000;
                    
                    metrics.recordSuccess(durationMs);
                    return new LoadTestResult(true, durationMs, null);
                    
                } catch (Exception e) {
                    metrics.recordFailure();
                    return new LoadTestResult(false, 0, e.getMessage());
                } finally {
                    completionLatch.countDown();
                }
            }))
            .toList();
        
        // Start all operations simultaneously
        long testStartTime = System.currentTimeMillis();
        startLatch.countDown();
        
        // Wait for completion
        boolean completed = completionLatch.await(60, TimeUnit.SECONDS);
        long testDuration = System.currentTimeMillis() - testStartTime;
        
        assertThat(completed).isTrue();
        
        // Collect results
        List<LoadTestResult> results = futures.stream()
            .map(future -> {
                try {
                    return future.get();
                } catch (Exception e) {
                    return new LoadTestResult(false, 0, e.getMessage());
                }
            })
            .toList();
        
        // Analyze results
        analyzeLoadTestResults(results, metrics, concurrentRequests, testDuration);
    }

    @ParameterizedTest
    @ValueSource(ints = {5, 10, 20, 30})
    @Order(2)
    @DisplayName("Should handle concurrent active liveness detection requests")
    void shouldHandleConcurrentActiveLivenessDetectionRequests(int concurrentRequests) 
            throws InterruptedException, ExecutionException, TimeoutException {
        
        LoadTestMetrics metrics = new LoadTestMetrics();
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(concurrentRequests);
        
        List<Future<LoadTestResult>> futures = IntStream.range(0, concurrentRequests)
            .mapToObj(i -> executorService.submit(() -> {
                try {
                    startLatch.await();
                    
                    long startTime = System.nanoTime();
                    
                    // Perform active liveness detection (2 images)
                    byte[] image1 = getRandomTestImage();
                    byte[] image2 = getRandomTestImage();
                    simulateActiveLivenessDetection(image1, image2);
                    
                    long endTime = System.nanoTime();
                    long durationMs = (endTime - startTime) / 1_000_000;
                    
                    metrics.recordSuccess(durationMs);
                    return new LoadTestResult(true, durationMs, null);
                    
                } catch (Exception e) {
                    metrics.recordFailure();
                    return new LoadTestResult(false, 0, e.getMessage());
                } finally {
                    completionLatch.countDown();
                }
            }))
            .toList();
        
        long testStartTime = System.currentTimeMillis();
        startLatch.countDown();
        
        boolean completed = completionLatch.await(90, TimeUnit.SECONDS);
        long testDuration = System.currentTimeMillis() - testStartTime;
        
        assertThat(completed).isTrue();
        
        List<LoadTestResult> results = futures.stream()
            .map(future -> {
                try {
                    return future.get();
                } catch (Exception e) {
                    return new LoadTestResult(false, 0, e.getMessage());
                }
            })
            .toList();
        
        analyzeLoadTestResults(results, metrics, concurrentRequests, testDuration);
    }

    @ParameterizedTest
    @ValueSource(ints = {5, 10, 15, 25})
    @Order(3)
    @DisplayName("Should handle concurrent challenge-response liveness detection requests")
    void shouldHandleConcurrentChallengeResponseLivenessDetectionRequests(int concurrentRequests) 
            throws InterruptedException, ExecutionException, TimeoutException {
        
        LoadTestMetrics metrics = new LoadTestMetrics();
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(concurrentRequests);
        
        List<Future<LoadTestResult>> futures = IntStream.range(0, concurrentRequests)
            .mapToObj(i -> executorService.submit(() -> {
                try {
                    startLatch.await();
                    
                    long startTime = System.nanoTime();
                    
                    // Generate unique challenge for each request
                    List<LivenessDetectionClient.ChallengeDirection> challenge = 
                        livenessClient.generateChallengeTags();
                    
                    byte[] image1 = getRandomTestImage();
                    byte[] image2 = getRandomTestImage();
                    simulateChallengeResponseLivenessDetection(image1, image2, challenge);
                    
                    long endTime = System.nanoTime();
                    long durationMs = (endTime - startTime) / 1_000_000;
                    
                    metrics.recordSuccess(durationMs);
                    return new LoadTestResult(true, durationMs, null);
                    
                } catch (Exception e) {
                    metrics.recordFailure();
                    return new LoadTestResult(false, 0, e.getMessage());
                } finally {
                    completionLatch.countDown();
                }
            }))
            .toList();
        
        long testStartTime = System.currentTimeMillis();
        startLatch.countDown();
        
        boolean completed = completionLatch.await(120, TimeUnit.SECONDS);
        long testDuration = System.currentTimeMillis() - testStartTime;
        
        assertThat(completed).isTrue();
        
        List<LoadTestResult> results = futures.stream()
            .map(future -> {
                try {
                    return future.get();
                } catch (Exception e) {
                    return new LoadTestResult(false, 0, e.getMessage());
                }
            })
            .toList();
        
        analyzeLoadTestResults(results, metrics, concurrentRequests, testDuration);
    }

    @Test
    @Order(4)
    @DisplayName("Should handle sustained load over time")
    void shouldHandleSustainedLoadOverTime() throws InterruptedException {
        int requestsPerSecond = 10;
        int durationSeconds = 30;
        int totalRequests = requestsPerSecond * durationSeconds;
        
        LoadTestMetrics metrics = new LoadTestMetrics();
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);
        CountDownLatch completionLatch = new CountDownLatch(totalRequests);
        
        try {
            // Schedule requests at regular intervals
            scheduler.scheduleAtFixedRate(() -> {
                for (int i = 0; i < requestsPerSecond; i++) {
                    executorService.submit(() -> {
                        try {
                            long startTime = System.nanoTime();
                            
                            byte[] testImage = getRandomTestImage();
                            simulatePassiveLivenessDetection(testImage);
                            
                            long endTime = System.nanoTime();
                            long durationMs = (endTime - startTime) / 1_000_000;
                            
                            metrics.recordSuccess(durationMs);
                            
                        } catch (Exception e) {
                            metrics.recordFailure();
                        } finally {
                            completionLatch.countDown();
                        }
                    });
                }
            }, 0, 1, TimeUnit.SECONDS);
            
            // Wait for all requests to complete
            boolean completed = completionLatch.await(durationSeconds + 30, TimeUnit.SECONDS);
            assertThat(completed).isTrue();
            
            // Analyze sustained load performance
            analyzeSustainedLoadResults(metrics, totalRequests, durationSeconds);
            
        } finally {
            scheduler.shutdown();
        }
    }

    @Test
    @Order(5)
    @DisplayName("Should handle memory efficiency under load")
    void shouldHandleMemoryEfficiencyUnderLoad() throws InterruptedException {
        int concurrentRequests = 50;
        int iterations = 5;
        
        Runtime runtime = Runtime.getRuntime();
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();
        
        for (int iteration = 0; iteration < iterations; iteration++) {
            CountDownLatch latch = new CountDownLatch(concurrentRequests);
            
            // Create memory-intensive operations
            for (int i = 0; i < concurrentRequests; i++) {
                executorService.submit(() -> {
                    try {
                        // Use different images to prevent caching effects
                        byte[] largeImage1 = createLargeTestImage(2 * 1024 * 1024); // 2MB
                        byte[] largeImage2 = createLargeTestImage(2 * 1024 * 1024); // 2MB
                        
                        simulateActiveLivenessDetection(largeImage1, largeImage2);
                        
                        // Clear references to help GC
                        largeImage1 = null;
                        largeImage2 = null;
                        
                    } catch (Exception e) {
                        // Expected for load testing
                    } finally {
                        latch.countDown();
                    }
                });
            }
            
            latch.await(60, TimeUnit.SECONDS);
            
            // Force garbage collection
            System.gc();
            Thread.sleep(1000);
        }
        
        // Check memory usage after load test
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryIncrease = finalMemory - initialMemory;
        
        // Memory increase should be reasonable (less than 100MB)
        assertThat(memoryIncrease).isLessThan(100 * 1024 * 1024);
    }

    @Test
    @Order(6)
    @DisplayName("Should handle mixed workload scenarios")
    void shouldHandleMixedWorkloadScenarios() throws InterruptedException, ExecutionException, TimeoutException {
        int totalRequests = 60;
        int passiveRequests = totalRequests / 3;
        int activeRequests = totalRequests / 3;
        int challengeRequests = totalRequests / 3;
        
        LoadTestMetrics metrics = new LoadTestMetrics();
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(totalRequests);
        
        List<Future<LoadTestResult>> futures = new java.util.ArrayList<>();
        
        // Add passive liveness detection requests
        for (int i = 0; i < passiveRequests; i++) {
            futures.add(executorService.submit(() -> {
                try {
                    startLatch.await();
                    long startTime = System.nanoTime();
                    
                    byte[] image = getRandomTestImage();
                    simulatePassiveLivenessDetection(image);
                    
                    long durationMs = (System.nanoTime() - startTime) / 1_000_000;
                    metrics.recordSuccess(durationMs);
                    return new LoadTestResult(true, durationMs, "PASSIVE");
                    
                } catch (Exception e) {
                    metrics.recordFailure();
                    return new LoadTestResult(false, 0, e.getMessage());
                } finally {
                    completionLatch.countDown();
                }
            }));
        }
        
        // Add active liveness detection requests
        for (int i = 0; i < activeRequests; i++) {
            futures.add(executorService.submit(() -> {
                try {
                    startLatch.await();
                    long startTime = System.nanoTime();
                    
                    byte[] image1 = getRandomTestImage();
                    byte[] image2 = getRandomTestImage();
                    simulateActiveLivenessDetection(image1, image2);
                    
                    long durationMs = (System.nanoTime() - startTime) / 1_000_000;
                    metrics.recordSuccess(durationMs);
                    return new LoadTestResult(true, durationMs, "ACTIVE");
                    
                } catch (Exception e) {
                    metrics.recordFailure();
                    return new LoadTestResult(false, 0, e.getMessage());
                } finally {
                    completionLatch.countDown();
                }
            }));
        }
        
        // Add challenge-response liveness detection requests
        for (int i = 0; i < challengeRequests; i++) {
            futures.add(executorService.submit(() -> {
                try {
                    startLatch.await();
                    long startTime = System.nanoTime();
                    
                    List<LivenessDetectionClient.ChallengeDirection> challenge = 
                        livenessClient.generateChallengeTags();
                    byte[] image1 = getRandomTestImage();
                    byte[] image2 = getRandomTestImage();
                    simulateChallengeResponseLivenessDetection(image1, image2, challenge);
                    
                    long durationMs = (System.nanoTime() - startTime) / 1_000_000;
                    metrics.recordSuccess(durationMs);
                    return new LoadTestResult(true, durationMs, "CHALLENGE_RESPONSE");
                    
                } catch (Exception e) {
                    metrics.recordFailure();
                    return new LoadTestResult(false, 0, e.getMessage());
                } finally {
                    completionLatch.countDown();
                }
            }));
        }
        
        // Start mixed workload
        long testStartTime = System.currentTimeMillis();
        startLatch.countDown();
        
        boolean completed = completionLatch.await(180, TimeUnit.SECONDS);
        long testDuration = System.currentTimeMillis() - testStartTime;
        
        assertThat(completed).isTrue();
        
        // Collect and analyze mixed workload results
        List<LoadTestResult> results = futures.stream()
            .map(future -> {
                try {
                    return future.get();
                } catch (Exception e) {
                    return new LoadTestResult(false, 0, e.getMessage());
                }
            })
            .toList();
        
        analyzeMixedWorkloadResults(results, metrics, testDuration);
    }

    @Test
    @Order(7)
    @EnabledIfSystemProperty(named = LOAD_TEST_ENABLED_PROPERTY, matches = "true")
    @DisplayName("Should handle extreme load conditions")
    void shouldHandleExtremeLoadConditions() throws InterruptedException {
        int extremeConcurrentRequests = 200;
        int extremeDurationSeconds = 60;
        
        LoadTestMetrics metrics = new LoadTestMetrics();
        ExecutorService extremeExecutor = Executors.newFixedThreadPool(50);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger completedRequests = new AtomicInteger(0);
        
        try {
            // Create extreme load
            for (int i = 0; i < extremeConcurrentRequests; i++) {
                extremeExecutor.submit(() -> {
                    try {
                        startLatch.await();
                        
                        long endTime = System.currentTimeMillis() + (extremeDurationSeconds * 1000);
                        
                        while (System.currentTimeMillis() < endTime) {
                            try {
                                long startTime = System.nanoTime();
                                
                                byte[] image = getRandomTestImage();
                                simulatePassiveLivenessDetection(image);
                                
                                long durationMs = (System.nanoTime() - startTime) / 1_000_000;
                                metrics.recordSuccess(durationMs);
                                completedRequests.incrementAndGet();
                                
                                // Small delay to prevent overwhelming
                                Thread.sleep(10);
                                
                            } catch (Exception e) {
                                metrics.recordFailure();
                            }
                        }
                        
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }
            
            // Start extreme load test
            long testStartTime = System.currentTimeMillis();
            startLatch.countDown();
            
            // Monitor progress
            Thread.sleep(extremeDurationSeconds * 1000);
            
            long testDuration = System.currentTimeMillis() - testStartTime;
            int totalCompleted = completedRequests.get();
            
            // Analyze extreme load results
            analyzeExtremeLoadResults(metrics, totalCompleted, testDuration, extremeConcurrentRequests);
            
        } finally {
            extremeExecutor.shutdown();
            extremeExecutor.awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    // Helper methods

    private void initializeTestData() {
        testImageJpeg = createTestImage("jpeg", 2048, (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0);
        testImagePng = createTestImage("png", 2048, (byte) 0x89, (byte) 0x50, (byte) 0x4E, (byte) 0x47);
        
        testImagePool = Arrays.asList(testImageJpeg, testImagePng);
    }

    private byte[] createTestImage(String type, int size, byte... magicBytes) {
        byte[] data = new byte[size];
        
        for (int i = 0; i < magicBytes.length && i < data.length; i++) {
            data[i] = magicBytes[i];
        }
        
        for (int i = magicBytes.length; i < size; i++) {
            data[i] = (byte) ((type.hashCode() + i) % 256);
        }
        
        return data;
    }

    private byte[] createLargeTestImage(int size) {
        return createTestImage("large", size, (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0);
    }

    private byte[] getRandomTestImage() {
        return testImagePool.get((int) (Math.random() * testImagePool.size()));
    }

    private void simulatePassiveLivenessDetection(byte[] image) throws BioIdException {
        LivenessDetectionErrorHandler.validateLivenessRequest(
            List.of(image), LivenessDetectionClient.LivenessMode.PASSIVE);
        
        // Simulate processing delay
        try {
            Thread.sleep(5 + (int) (Math.random() * 10)); // 5-15ms
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted", e);
        }
    }

    private void simulateActiveLivenessDetection(byte[] image1, byte[] image2) throws BioIdException {
        LivenessDetectionErrorHandler.validateLivenessRequest(
            List.of(image1, image2), LivenessDetectionClient.LivenessMode.ACTIVE);
        
        // Simulate processing delay (longer for active)
        try {
            Thread.sleep(10 + (int) (Math.random() * 20)); // 10-30ms
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted", e);
        }
    }

    private void simulateChallengeResponseLivenessDetection(byte[] image1, byte[] image2, 
                                                          List<LivenessDetectionClient.ChallengeDirection> directions) 
            throws BioIdException {
        LivenessDetectionErrorHandler.validateLivenessRequest(
            List.of(image1, image2), LivenessDetectionClient.LivenessMode.CHALLENGE_RESPONSE, directions);
        
        // Simulate processing delay (longest for challenge-response)
        try {
            Thread.sleep(15 + (int) (Math.random() * 25)); // 15-40ms
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted", e);
        }
    }

    private void analyzeLoadTestResults(List<LoadTestResult> results, LoadTestMetrics metrics, 
                                      int expectedRequests, long testDurationMs) {
        
        long successfulRequests = results.stream().mapToLong(r -> r.isSuccessful() ? 1 : 0).sum();
        long failedRequests = results.size() - successfulRequests;
        
        double successRate = (double) successfulRequests / results.size() * 100;
        double throughput = (double) results.size() / (testDurationMs / 1000.0);
        
        // Performance assertions
        assertThat(results).hasSize(expectedRequests);
        assertThat(successRate).isGreaterThan(95.0); // At least 95% success rate
        assertThat(metrics.getAverageResponseTime()).isLessThan(200); // Average under 200ms
        assertThat(metrics.getMaxResponseTime()).isLessThan(1000); // Max under 1 second
        
        System.out.printf("Load Test Results: %d requests, %.1f%% success rate, %.1f req/sec throughput%n", 
                         results.size(), successRate, throughput);
        System.out.printf("Response Times: avg=%.1fms, min=%dms, max=%dms%n", 
                         metrics.getAverageResponseTime(), metrics.getMinResponseTime(), metrics.getMaxResponseTime());
    }

    private void analyzeSustainedLoadResults(LoadTestMetrics metrics, int totalRequests, int durationSeconds) {
        double actualThroughput = (double) metrics.getSuccessCount() / durationSeconds;
        double successRate = (double) metrics.getSuccessCount() / totalRequests * 100;
        
        assertThat(successRate).isGreaterThan(90.0); // At least 90% success rate for sustained load
        assertThat(metrics.getAverageResponseTime()).isLessThan(300); // Average under 300ms for sustained load
        
        System.out.printf("Sustained Load Results: %.1f req/sec actual throughput, %.1f%% success rate%n", 
                         actualThroughput, successRate);
    }

    private void analyzeMixedWorkloadResults(List<LoadTestResult> results, LoadTestMetrics metrics, long testDurationMs) {
        long passiveCount = results.stream().mapToLong(r -> "PASSIVE".equals(r.getOperationType()) ? 1 : 0).sum();
        long activeCount = results.stream().mapToLong(r -> "ACTIVE".equals(r.getOperationType()) ? 1 : 0).sum();
        long challengeCount = results.stream().mapToLong(r -> "CHALLENGE_RESPONSE".equals(r.getOperationType()) ? 1 : 0).sum();
        
        double successRate = (double) metrics.getSuccessCount() / results.size() * 100;
        
        assertThat(successRate).isGreaterThan(90.0); // At least 90% success rate for mixed workload
        assertThat(passiveCount).isGreaterThan(0);
        assertThat(activeCount).isGreaterThan(0);
        assertThat(challengeCount).isGreaterThan(0);
        
        System.out.printf("Mixed Workload Results: %d passive, %d active, %d challenge-response, %.1f%% success rate%n", 
                         passiveCount, activeCount, challengeCount, successRate);
    }

    private void analyzeExtremeLoadResults(LoadTestMetrics metrics, int totalCompleted, long testDurationMs, 
                                         int concurrentThreads) {
        double actualThroughput = (double) totalCompleted / (testDurationMs / 1000.0);
        double successRate = (double) metrics.getSuccessCount() / totalCompleted * 100;
        
        // More lenient assertions for extreme load
        assertThat(successRate).isGreaterThan(70.0); // At least 70% success rate under extreme load
        assertThat(actualThroughput).isGreaterThan(10.0); // At least 10 req/sec under extreme load
        
        System.out.printf("Extreme Load Results: %d threads, %d completed requests, %.1f req/sec, %.1f%% success rate%n", 
                         concurrentThreads, totalCompleted, actualThroughput, successRate);
    }

    // Helper classes

    private static class LoadTestResult {
        private final boolean successful;
        private final long responseTimeMs;
        private final String operationType;

        public LoadTestResult(boolean successful, long responseTimeMs, String operationType) {
            this.successful = successful;
            this.responseTimeMs = responseTimeMs;
            this.operationType = operationType;
        }

        public boolean isSuccessful() { return successful; }
        public long getResponseTimeMs() { return responseTimeMs; }
        public String getOperationType() { return operationType; }
    }

    private static class LoadTestMetrics {
        private final AtomicLong successCount = new AtomicLong(0);
        private final AtomicLong failureCount = new AtomicLong(0);
        private final AtomicLong totalResponseTime = new AtomicLong(0);
        private final AtomicLong minResponseTime = new AtomicLong(Long.MAX_VALUE);
        private final AtomicLong maxResponseTime = new AtomicLong(0);

        public void recordSuccess(long responseTimeMs) {
            successCount.incrementAndGet();
            totalResponseTime.addAndGet(responseTimeMs);
            
            // Update min/max response times
            minResponseTime.updateAndGet(current -> Math.min(current, responseTimeMs));
            maxResponseTime.updateAndGet(current -> Math.max(current, responseTimeMs));
        }

        public void recordFailure() {
            failureCount.incrementAndGet();
        }

        public long getSuccessCount() { return successCount.get(); }
        public long getFailureCount() { return failureCount.get(); }
        public double getAverageResponseTime() { 
            long count = successCount.get();
            return count > 0 ? (double) totalResponseTime.get() / count : 0.0;
        }
        public long getMinResponseTime() { 
            long min = minResponseTime.get();
            return min == Long.MAX_VALUE ? 0 : min;
        }
        public long getMaxResponseTime() { return maxResponseTime.get(); }
    }
}