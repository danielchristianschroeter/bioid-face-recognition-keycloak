package com.bioid.keycloak.performance;

import com.bioid.keycloak.config.AdminConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Throttling service for bulk operations to prevent system overload.
 * 
 * Implements adaptive throttling based on system load, error rates, and
 * configuration settings. Provides automatic backoff when system is under stress.
 * 
 * Requirements addressed: 9.1, 9.2, 9.5, 9.6
 */
public class BulkOperationThrottler {
    
    private static final Logger logger = LoggerFactory.getLogger(BulkOperationThrottler.class);
    
    private static final double HIGH_ERROR_RATE_THRESHOLD = 0.10; // 10%
    private static final double CRITICAL_ERROR_RATE_THRESHOLD = 0.25; // 25%
    private static final long THROTTLE_WINDOW_MINUTES = 5;
    private static final int MIN_BATCH_SIZE = 5;
    
    private final ConcurrentHashMap<String, OperationMetrics> operationMetrics = new ConcurrentHashMap<>();
    private final Semaphore concurrentOperationsLimit;
    private final AtomicInteger activeBulkOperations = new AtomicInteger(0);
    private final AtomicLong totalOperationsThrottled = new AtomicLong(0);
    
    private volatile AdminConfiguration configuration;
    private volatile ThrottleLevel currentThrottleLevel = ThrottleLevel.NORMAL;
    private volatile Instant lastThrottleLevelChange = Instant.now();
    
    private static volatile BulkOperationThrottler instance;
    
    /**
     * Throttle levels for different system load conditions.
     */
    public enum ThrottleLevel {
        NORMAL(1.0, 1.0),
        LIGHT(0.8, 0.9),
        MODERATE(0.6, 0.7),
        HEAVY(0.4, 0.5),
        CRITICAL(0.2, 0.3);
        
        private final double batchSizeMultiplier;
        private final double concurrencyMultiplier;
        
        ThrottleLevel(double batchSizeMultiplier, double concurrencyMultiplier) {
            this.batchSizeMultiplier = batchSizeMultiplier;
            this.concurrencyMultiplier = concurrencyMultiplier;
        }
        
        public double getBatchSizeMultiplier() { return batchSizeMultiplier; }
        public double getConcurrencyMultiplier() { return concurrencyMultiplier; }
    }
    
    /**
     * Metrics for tracking operation performance.
     */
    private static class OperationMetrics {
        private final AtomicLong totalOperations = new AtomicLong(0);
        private final AtomicLong errorCount = new AtomicLong(0);
        private final AtomicLong totalDuration = new AtomicLong(0);
        private volatile Instant lastOperation = Instant.now();
        private volatile Instant windowStart = Instant.now();
        
        public void recordOperation(boolean isError, long durationMs) {
            totalOperations.incrementAndGet();
            totalDuration.addAndGet(durationMs);
            lastOperation = Instant.now();
            
            if (isError) {
                errorCount.incrementAndGet();
            }
            
            // Reset window if it's been too long
            if (Duration.between(windowStart, Instant.now()).toMinutes() > THROTTLE_WINDOW_MINUTES) {
                reset();
            }
        }
        
        public double getErrorRate() {
            long total = totalOperations.get();
            return total > 0 ? (double) errorCount.get() / total : 0.0;
        }
        
        public double getAverageDuration() {
            long total = totalOperations.get();
            return total > 0 ? (double) totalDuration.get() / total : 0.0;
        }
        
        public long getTotalOperations() { return totalOperations.get(); }
        public long getErrorCount() { return errorCount.get(); }
        public Instant getLastOperation() { return lastOperation; }
        
        public void reset() {
            totalOperations.set(0);
            errorCount.set(0);
            totalDuration.set(0);
            windowStart = Instant.now();
        }
    }
    
    private BulkOperationThrottler(AdminConfiguration config) {
        this.configuration = config;
        this.concurrentOperationsLimit = new Semaphore(config.getMaxConcurrentOperations());
        logger.info("Bulk operation throttler initialized with max concurrent operations: {}", 
                   config.getMaxConcurrentOperations());
    }
    
    /**
     * Gets the singleton instance.
     */
    public static BulkOperationThrottler getInstance(AdminConfiguration config) {
        if (instance == null) {
            synchronized (BulkOperationThrottler.class) {
                if (instance == null) {
                    instance = new BulkOperationThrottler(config);
                }
            }
        }
        return instance;
    }
    
    /**
     * Updates configuration settings.
     */
    public void updateConfiguration(AdminConfiguration config) {
        this.configuration = config;
        
        // Update semaphore if max concurrent operations changed
        int newLimit = config.getMaxConcurrentOperations();
        int currentPermits = concurrentOperationsLimit.availablePermits() + activeBulkOperations.get();
        
        if (newLimit != currentPermits) {
            logger.info("Updating concurrent operations limit from {} to {}", currentPermits, newLimit);
            
            if (newLimit > currentPermits) {
                concurrentOperationsLimit.release(newLimit - currentPermits);
            } else {
                concurrentOperationsLimit.drainPermits();
                concurrentOperationsLimit.release(newLimit);
            }
        }
    }
    
    /**
     * Acquires permission to start a bulk operation.
     * 
     * @param operationType the type of operation
     * @return throttle context for the operation
     * @throws InterruptedException if interrupted while waiting
     */
    public ThrottleContext acquireOperation(String operationType) throws InterruptedException {
        // Check if we should throttle based on current system state
        updateThrottleLevel();
        
        // Apply throttling delay if necessary
        applyThrottleDelay();
        
        // Acquire semaphore permit
        concurrentOperationsLimit.acquire();
        activeBulkOperations.incrementAndGet();
        
        logger.debug("Acquired bulk operation permit for: {} (active: {})", 
                    operationType, activeBulkOperations.get());
        
        return new ThrottleContext(operationType);
    }
    
    /**
     * Tries to acquire permission without blocking.
     * 
     * @param operationType the type of operation
     * @return throttle context if acquired, null if not available
     */
    public ThrottleContext tryAcquireOperation(String operationType) {
        // Check if we should throttle based on current system state
        updateThrottleLevel();
        
        if (currentThrottleLevel == ThrottleLevel.CRITICAL) {
            logger.warn("Rejecting bulk operation {} due to critical throttle level", operationType);
            totalOperationsThrottled.incrementAndGet();
            return null;
        }
        
        // Try to acquire semaphore permit
        if (concurrentOperationsLimit.tryAcquire()) {
            activeBulkOperations.incrementAndGet();
            logger.debug("Acquired bulk operation permit for: {} (active: {})", 
                        operationType, activeBulkOperations.get());
            return new ThrottleContext(operationType);
        } else {
            logger.debug("No permits available for bulk operation: {}", operationType);
            totalOperationsThrottled.incrementAndGet();
            return null;
        }
    }
    
    /**
     * Gets the recommended batch size for an operation type.
     * 
     * @param operationType the operation type
     * @param requestedSize the originally requested batch size
     * @return the throttled batch size
     */
    public int getThrottledBatchSize(String operationType, int requestedSize) {
        int configuredMax = configuration.getMaxBulkOperationSize();
        int effectiveMax = (int) (configuredMax * currentThrottleLevel.getBatchSizeMultiplier());
        
        int throttledSize = Math.min(requestedSize, effectiveMax);
        throttledSize = Math.max(throttledSize, MIN_BATCH_SIZE);
        
        if (throttledSize != requestedSize) {
            logger.debug("Throttled batch size for {} from {} to {} (level: {})", 
                        operationType, requestedSize, throttledSize, currentThrottleLevel);
        }
        
        return throttledSize;
    }
    
    /**
     * Records the completion of an operation for metrics.
     * 
     * @param operationType the operation type
     * @param isError whether the operation failed
     * @param durationMs the operation duration in milliseconds
     */
    public void recordOperation(String operationType, boolean isError, long durationMs) {
        OperationMetrics metrics = operationMetrics.computeIfAbsent(
            operationType, k -> new OperationMetrics());
        metrics.recordOperation(isError, durationMs);
        
        logger.debug("Recorded operation: {} (error: {}, duration: {}ms)", 
                    operationType, isError, durationMs);
    }
    
    /**
     * Gets throttling statistics.
     */
    public ThrottleStats getStats() {
        double overallErrorRate = calculateOverallErrorRate();
        double averageDuration = calculateAverageOperationDuration();
        
        return new ThrottleStats(
            currentThrottleLevel,
            activeBulkOperations.get(),
            concurrentOperationsLimit.availablePermits(),
            totalOperationsThrottled.get(),
            overallErrorRate,
            averageDuration,
            operationMetrics.size()
        );
    }
    
    /**
     * Updates the throttle level based on current system metrics.
     */
    private void updateThrottleLevel() {
        double errorRate = calculateOverallErrorRate();
        double avgDuration = calculateAverageOperationDuration();
        int activeOps = activeBulkOperations.get();
        int maxConcurrent = configuration.getMaxConcurrentOperations();
        
        ThrottleLevel newLevel = determineThrottleLevel(errorRate, avgDuration, activeOps, maxConcurrent);
        
        if (newLevel != currentThrottleLevel) {
            // Only change throttle level if enough time has passed to avoid oscillation
            if (Duration.between(lastThrottleLevelChange, Instant.now()).toSeconds() >= 30) {
                logger.info("Changing throttle level from {} to {} (errorRate: {:.2f}%, avgDuration: {:.2f}ms, activeOps: {})", 
                           currentThrottleLevel, newLevel, errorRate * 100, avgDuration, activeOps);
                currentThrottleLevel = newLevel;
                lastThrottleLevelChange = Instant.now();
            }
        }
    }
    
    /**
     * Determines the appropriate throttle level based on metrics.
     */
    private ThrottleLevel determineThrottleLevel(double errorRate, double avgDuration, 
                                               int activeOps, int maxConcurrent) {
        // Critical conditions
        if (errorRate >= CRITICAL_ERROR_RATE_THRESHOLD || avgDuration > 30000) {
            return ThrottleLevel.CRITICAL;
        }
        
        // High error rate or very high load
        if (errorRate >= HIGH_ERROR_RATE_THRESHOLD || activeOps >= maxConcurrent) {
            return ThrottleLevel.HEAVY;
        }
        
        // Moderate conditions
        if (errorRate >= 0.05 || avgDuration > 10000 || activeOps >= maxConcurrent * 0.8) {
            return ThrottleLevel.MODERATE;
        }
        
        // Light throttling
        if (errorRate >= 0.02 || avgDuration > 5000 || activeOps >= maxConcurrent * 0.6) {
            return ThrottleLevel.LIGHT;
        }
        
        return ThrottleLevel.NORMAL;
    }
    
    /**
     * Applies throttle delay based on current level.
     */
    private void applyThrottleDelay() {
        if (currentThrottleLevel == ThrottleLevel.NORMAL) {
            return;
        }
        
        long delayMs = switch (currentThrottleLevel) {
            case LIGHT -> 100;
            case MODERATE -> 250;
            case HEAVY -> 500;
            case CRITICAL -> 1000;
            default -> 0;
        };
        
        if (delayMs > 0) {
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted during throttle delay", e);
            }
        }
    }
    
    /**
     * Calculates overall error rate across all operations.
     */
    private double calculateOverallErrorRate() {
        long totalOps = 0;
        long totalErrors = 0;
        
        for (OperationMetrics metrics : operationMetrics.values()) {
            totalOps += metrics.getTotalOperations();
            totalErrors += metrics.getErrorCount();
        }
        
        return totalOps > 0 ? (double) totalErrors / totalOps : 0.0;
    }
    
    /**
     * Calculates average operation duration across all operations.
     */
    private double calculateAverageOperationDuration() {
        double totalDuration = 0;
        int count = 0;
        
        for (OperationMetrics metrics : operationMetrics.values()) {
            if (metrics.getTotalOperations() > 0) {
                totalDuration += metrics.getAverageDuration();
                count++;
            }
        }
        
        return count > 0 ? totalDuration / count : 0.0;
    }
    
    /**
     * Context for tracking a bulk operation.
     */
    public class ThrottleContext implements AutoCloseable {
        private final String operationType;
        private final Instant startTime;
        private boolean isError = false;
        
        public ThrottleContext(String operationType) {
            this.operationType = operationType;
            this.startTime = Instant.now();
        }
        
        public void markError() {
            this.isError = true;
        }
        
        @Override
        public void close() {
            long duration = Duration.between(startTime, Instant.now()).toMillis();
            recordOperation(operationType, isError, duration);
            
            // Release semaphore permit
            concurrentOperationsLimit.release();
            activeBulkOperations.decrementAndGet();
            
            logger.debug("Released bulk operation permit for: {} (active: {})", 
                        operationType, activeBulkOperations.get());
        }
    }
    
    /**
     * Throttling statistics.
     */
    public static class ThrottleStats {
        private final ThrottleLevel currentLevel;
        private final int activeBulkOperations;
        private final int availablePermits;
        private final long totalThrottled;
        private final double overallErrorRate;
        private final double averageDuration;
        private final int trackedOperationTypes;
        
        public ThrottleStats(ThrottleLevel currentLevel, int activeBulkOperations, 
                           int availablePermits, long totalThrottled, double overallErrorRate,
                           double averageDuration, int trackedOperationTypes) {
            this.currentLevel = currentLevel;
            this.activeBulkOperations = activeBulkOperations;
            this.availablePermits = availablePermits;
            this.totalThrottled = totalThrottled;
            this.overallErrorRate = overallErrorRate;
            this.averageDuration = averageDuration;
            this.trackedOperationTypes = trackedOperationTypes;
        }
        
        public ThrottleLevel getCurrentLevel() { return currentLevel; }
        public int getActiveBulkOperations() { return activeBulkOperations; }
        public int getAvailablePermits() { return availablePermits; }
        public long getTotalThrottled() { return totalThrottled; }
        public double getOverallErrorRate() { return overallErrorRate; }
        public double getAverageDuration() { return averageDuration; }
        public int getTrackedOperationTypes() { return trackedOperationTypes; }
        
        @Override
        public String toString() {
            return String.format(
                "ThrottleStats{level=%s, active=%d, available=%d, throttled=%d, errorRate=%.2f%%, avgDuration=%.2fms}",
                currentLevel, activeBulkOperations, availablePermits, totalThrottled, 
                overallErrorRate * 100, averageDuration
            );
        }
    }
}