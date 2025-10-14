package com.bioid.keycloak.performance;

import com.bioid.keycloak.config.AdminConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Enhanced performance metrics collector for administrative operations.
 * 
 * Collects detailed metrics for administrative operations including template
 * management, bulk operations, liveness detection, and system performance.
 * Integrates with AdminConfiguration for configurable collection intervals.
 * 
 * Requirements addressed: 9.1, 9.2, 9.5, 9.6
 */
public class AdminPerformanceMetrics {
    
    private static final Logger logger = LoggerFactory.getLogger(AdminPerformanceMetrics.class);
    
    private final Map<String, AdminOperationMetrics> operationMetrics = new ConcurrentHashMap<>();
    private final Map<String, SystemResourceMetrics> resourceMetrics = new ConcurrentHashMap<>();
    private final ScheduledExecutorService metricsCollector;
    private final AtomicLong startTime = new AtomicLong(System.currentTimeMillis());
    
    private volatile AdminConfiguration configuration;
    private volatile ScheduledFuture<?> collectionTask;
    private volatile boolean metricsEnabled = true;
    
    private static volatile AdminPerformanceMetrics instance;
    
    /**
     * Metrics for administrative operations.
     */
    private static class AdminOperationMetrics {
        private final LongAdder totalCalls = new LongAdder();
        private final LongAdder totalDuration = new LongAdder();
        private final LongAdder errorCount = new LongAdder();
        private final AtomicLong minDuration = new AtomicLong(Long.MAX_VALUE);
        private final AtomicLong maxDuration = new AtomicLong(0);
        private final AtomicLong lastCallTime = new AtomicLong(0);
        
        // Administrative operation specific metrics
        private final LongAdder itemsProcessed = new LongAdder();
        private final LongAdder bytesProcessed = new LongAdder();
        private final AtomicLong peakConcurrency = new AtomicLong(0);
        private final LongAdder throttledCalls = new LongAdder();
        
        public void recordCall(long durationMs, boolean isError, long itemsCount, long bytesCount) {
            totalCalls.increment();
            totalDuration.add(durationMs);
            lastCallTime.set(System.currentTimeMillis());
            itemsProcessed.add(itemsCount);
            bytesProcessed.add(bytesCount);
            
            if (isError) {
                errorCount.increment();
            }
            
            // Update min/max duration
            minDuration.updateAndGet(current -> Math.min(current, durationMs));
            maxDuration.updateAndGet(current -> Math.max(current, durationMs));
        }
        
        public void recordThrottling() {
            throttledCalls.increment();
        }
        
        public void updatePeakConcurrency(long concurrency) {
            peakConcurrency.updateAndGet(current -> Math.max(current, concurrency));
        }
        
        public AdminOperationStats getStats() {
            long calls = totalCalls.sum();
            long duration = totalDuration.sum();
            long errors = errorCount.sum();
            long items = itemsProcessed.sum();
            long bytes = bytesProcessed.sum();
            long throttled = throttledCalls.sum();
            
            double avgDuration = calls > 0 ? (double) duration / calls : 0.0;
            double errorRate = calls > 0 ? (double) errors / calls : 0.0;
            double throughput = calls > 0 ? (double) items / calls : 0.0;
            long min = minDuration.get() == Long.MAX_VALUE ? 0 : minDuration.get();
            long max = maxDuration.get();
            long peak = peakConcurrency.get();
            
            return new AdminOperationStats(
                calls, avgDuration, min, max, errors, errorRate, 
                lastCallTime.get(), items, bytes, throughput, peak, throttled
            );
        }
    }
    
    /**
     * System resource metrics.
     */
    private static class SystemResourceMetrics {
        private final LongAdder memoryUsed = new LongAdder();
        private final LongAdder cpuTime = new LongAdder();
        private final AtomicLong peakMemory = new AtomicLong(0);
        private final AtomicLong lastUpdate = new AtomicLong(System.currentTimeMillis());
        private final LongAdder sampleCount = new LongAdder();
        
        public void recordSample(long memoryBytes, long cpuTimeMs) {
            memoryUsed.add(memoryBytes);
            cpuTime.add(cpuTimeMs);
            sampleCount.increment();
            lastUpdate.set(System.currentTimeMillis());
            
            peakMemory.updateAndGet(current -> Math.max(current, memoryBytes));
        }
        
        public SystemResourceStats getStats() {
            long samples = sampleCount.sum();
            long totalMemory = memoryUsed.sum();
            long totalCpu = cpuTime.sum();
            
            double avgMemory = samples > 0 ? (double) totalMemory / samples : 0.0;
            double avgCpu = samples > 0 ? (double) totalCpu / samples : 0.0;
            
            return new SystemResourceStats(
                avgMemory, peakMemory.get(), avgCpu, samples, lastUpdate.get()
            );
        }
    }
    
    private AdminPerformanceMetrics(AdminConfiguration config) {
        this.configuration = config;
        this.metricsEnabled = config.isEnablePerformanceMetrics();
        
        this.metricsCollector = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "admin-performance-metrics");
            t.setDaemon(true);
            return t;
        });
        
        startMetricsCollection();
        logger.info("Admin performance metrics initialized (enabled: {})", metricsEnabled);
    }
    
    /**
     * Gets the singleton instance.
     */
    public static AdminPerformanceMetrics getInstance(AdminConfiguration config) {
        if (instance == null) {
            synchronized (AdminPerformanceMetrics.class) {
                if (instance == null) {
                    instance = new AdminPerformanceMetrics(config);
                }
            }
        }
        return instance;
    }
    
    /**
     * Updates configuration and restarts metrics collection if needed.
     */
    public void updateConfiguration(AdminConfiguration config) {
        boolean wasEnabled = this.metricsEnabled;
        this.configuration = config;
        this.metricsEnabled = config.isEnablePerformanceMetrics();
        
        if (wasEnabled != metricsEnabled || 
            !config.getMetricsCollectionInterval().equals(configuration.getMetricsCollectionInterval())) {
            
            logger.info("Updating metrics configuration (enabled: {}, interval: {})", 
                       metricsEnabled, config.getMetricsCollectionInterval());
            
            stopMetricsCollection();
            if (metricsEnabled) {
                startMetricsCollection();
            }
        }
    }
    
    /**
     * Records an administrative operation.
     */
    public void recordAdminOperation(String operationType, long durationMs, boolean isError, 
                                   long itemsProcessed, long bytesProcessed) {
        if (!metricsEnabled) {
            return;
        }
        
        AdminOperationMetrics metrics = operationMetrics.computeIfAbsent(
            operationType, k -> new AdminOperationMetrics());
        metrics.recordCall(durationMs, isError, itemsProcessed, bytesProcessed);
        
        logger.debug("Recorded admin operation: {} (duration: {}ms, items: {}, error: {})", 
                    operationType, durationMs, itemsProcessed, isError);
    }
    
    /**
     * Records a throttling event.
     */
    public void recordThrottling(String operationType) {
        if (!metricsEnabled) {
            return;
        }
        
        AdminOperationMetrics metrics = operationMetrics.computeIfAbsent(
            operationType, k -> new AdminOperationMetrics());
        metrics.recordThrottling();
        
        logger.debug("Recorded throttling for operation: {}", operationType);
    }
    
    /**
     * Updates peak concurrency for an operation.
     */
    public void updatePeakConcurrency(String operationType, long concurrency) {
        if (!metricsEnabled) {
            return;
        }
        
        AdminOperationMetrics metrics = operationMetrics.computeIfAbsent(
            operationType, k -> new AdminOperationMetrics());
        metrics.updatePeakConcurrency(concurrency);
    }
    
    /**
     * Records system resource usage.
     */
    public void recordSystemResources(String resourceType, long memoryBytes, long cpuTimeMs) {
        if (!metricsEnabled) {
            return;
        }
        
        SystemResourceMetrics metrics = resourceMetrics.computeIfAbsent(
            resourceType, k -> new SystemResourceMetrics());
        metrics.recordSample(memoryBytes, cpuTimeMs);
    }
    
    /**
     * Gets metrics for a specific operation.
     */
    public AdminOperationStats getOperationStats(String operationType) {
        AdminOperationMetrics metrics = operationMetrics.get(operationType);
        return metrics != null ? metrics.getStats() : 
            new AdminOperationStats(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }
    
    /**
     * Gets all operation statistics.
     */
    public Map<String, AdminOperationStats> getAllOperationStats() {
        Map<String, AdminOperationStats> stats = new ConcurrentHashMap<>();
        for (Map.Entry<String, AdminOperationMetrics> entry : operationMetrics.entrySet()) {
            stats.put(entry.getKey(), entry.getValue().getStats());
        }
        return stats;
    }
    
    /**
     * Gets system resource statistics.
     */
    public Map<String, SystemResourceStats> getSystemResourceStats() {
        Map<String, SystemResourceStats> stats = new ConcurrentHashMap<>();
        for (Map.Entry<String, SystemResourceMetrics> entry : resourceMetrics.entrySet()) {
            stats.put(entry.getKey(), entry.getValue().getStats());
        }
        return stats;
    }
    
    /**
     * Gets overall performance summary.
     */
    public AdminPerformanceSummary getPerformanceSummary() {
        long totalOperations = 0;
        long totalErrors = 0;
        double totalDuration = 0;
        long totalItemsProcessed = 0;
        long totalBytesProcessed = 0;
        long totalThrottled = 0;
        long maxPeakConcurrency = 0;
        
        for (AdminOperationMetrics metrics : operationMetrics.values()) {
            AdminOperationStats stats = metrics.getStats();
            totalOperations += stats.getTotalCalls();
            totalErrors += stats.getErrorCount();
            totalDuration += stats.getAverageDuration() * stats.getTotalCalls();
            totalItemsProcessed += stats.getItemsProcessed();
            totalBytesProcessed += stats.getBytesProcessed();
            totalThrottled += stats.getThrottledCalls();
            maxPeakConcurrency = Math.max(maxPeakConcurrency, stats.getPeakConcurrency());
        }
        
        double avgDuration = totalOperations > 0 ? totalDuration / totalOperations : 0.0;
        double errorRate = totalOperations > 0 ? (double) totalErrors / totalOperations : 0.0;
        double throttleRate = totalOperations > 0 ? (double) totalThrottled / totalOperations : 0.0;
        
        long uptime = System.currentTimeMillis() - startTime.get();
        double throughput = uptime > 0 ? (double) totalOperations / (uptime / 1000.0) : 0.0;
        
        return new AdminPerformanceSummary(
            totalOperations, avgDuration, errorRate, throughput, uptime,
            totalItemsProcessed, totalBytesProcessed, throttleRate, maxPeakConcurrency
        );
    }
    
    /**
     * Resets all metrics.
     */
    public void reset() {
        operationMetrics.clear();
        resourceMetrics.clear();
        startTime.set(System.currentTimeMillis());
        logger.info("Admin performance metrics reset");
    }
    
    /**
     * Starts metrics collection.
     */
    private void startMetricsCollection() {
        if (!metricsEnabled || collectionTask != null) {
            return;
        }
        
        Duration interval = configuration.getMetricsCollectionInterval();
        collectionTask = metricsCollector.scheduleAtFixedRate(
            this::collectSystemMetrics,
            interval.toSeconds(),
            interval.toSeconds(),
            TimeUnit.SECONDS
        );
        
        logger.debug("Started metrics collection with interval: {}", interval);
    }
    
    /**
     * Stops metrics collection.
     */
    private void stopMetricsCollection() {
        if (collectionTask != null) {
            collectionTask.cancel(true);
            collectionTask = null;
            logger.debug("Stopped metrics collection");
        }
    }
    
    /**
     * Collects system metrics.
     */
    private void collectSystemMetrics() {
        try {
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            
            // Record JVM memory usage
            recordSystemResources("jvm", usedMemory, 0);
            
            // Collect cache statistics
            collectCacheMetrics();
            
            // Collect connection pool metrics
            collectConnectionPoolMetrics();
            
        } catch (Exception e) {
            logger.warn("Error collecting system metrics", e);
        }
    }
    
    /**
     * Collects cache metrics.
     */
    private void collectCacheMetrics() {
        try {
            // Template cache metrics
            TemplateCache templateCache = TemplateCache.getInstance();
            TemplateCache.TemplateCacheStats cacheStats = templateCache.getStats();
            recordSystemResources("template-cache", cacheStats.getEstimatedMemoryBytes(), 0);
            
            // Credential cache metrics
            CredentialCache credentialCache = CredentialCache.getInstance();
            CredentialCache.CacheStats credStats = credentialCache.getStats();
            recordSystemResources("credential-cache", credStats.getSize() * 1024, 0); // Estimate
            
        } catch (Exception e) {
            logger.debug("Error collecting cache metrics", e);
        }
    }
    
    /**
     * Collects connection pool metrics.
     */
    private void collectConnectionPoolMetrics() {
        try {
            GrpcChannelPool channelPool = GrpcChannelPool.getInstance();
            Map<String, GrpcChannelPool.ChannelPoolStats> poolStats = channelPool.getAllStats();
            
            for (Map.Entry<String, GrpcChannelPool.ChannelPoolStats> entry : poolStats.entrySet()) {
                GrpcChannelPool.ChannelPoolStats stats = entry.getValue();
                recordSystemResources("grpc-pool-" + entry.getKey(), 
                                     stats.getTotalChannels() * 1024, 0); // Estimate
            }
            
        } catch (Exception e) {
            logger.debug("Error collecting connection pool metrics", e);
        }
    }
    
    /**
     * Shuts down metrics collection.
     */
    public void shutdown() {
        logger.info("Shutting down admin performance metrics");
        
        stopMetricsCollection();
        
        metricsCollector.shutdown();
        try {
            if (!metricsCollector.awaitTermination(5, TimeUnit.SECONDS)) {
                metricsCollector.shutdownNow();
            }
        } catch (InterruptedException e) {
            metricsCollector.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        instance = null;
        logger.info("Admin performance metrics shutdown complete");
    }
    
    /**
     * Statistics for administrative operations.
     */
    public static class AdminOperationStats {
        private final long totalCalls;
        private final double averageDuration;
        private final long minDuration;
        private final long maxDuration;
        private final long errorCount;
        private final double errorRate;
        private final long lastCallTime;
        private final long itemsProcessed;
        private final long bytesProcessed;
        private final double throughput;
        private final long peakConcurrency;
        private final long throttledCalls;
        
        public AdminOperationStats(long totalCalls, double averageDuration, long minDuration,
                                 long maxDuration, long errorCount, double errorRate, long lastCallTime,
                                 long itemsProcessed, long bytesProcessed, double throughput,
                                 long peakConcurrency, long throttledCalls) {
            this.totalCalls = totalCalls;
            this.averageDuration = averageDuration;
            this.minDuration = minDuration;
            this.maxDuration = maxDuration;
            this.errorCount = errorCount;
            this.errorRate = errorRate;
            this.lastCallTime = lastCallTime;
            this.itemsProcessed = itemsProcessed;
            this.bytesProcessed = bytesProcessed;
            this.throughput = throughput;
            this.peakConcurrency = peakConcurrency;
            this.throttledCalls = throttledCalls;
        }
        
        // Getters
        public long getTotalCalls() { return totalCalls; }
        public double getAverageDuration() { return averageDuration; }
        public long getMinDuration() { return minDuration; }
        public long getMaxDuration() { return maxDuration; }
        public long getErrorCount() { return errorCount; }
        public double getErrorRate() { return errorRate; }
        public long getLastCallTime() { return lastCallTime; }
        public long getItemsProcessed() { return itemsProcessed; }
        public long getBytesProcessed() { return bytesProcessed; }
        public double getThroughput() { return throughput; }
        public long getPeakConcurrency() { return peakConcurrency; }
        public long getThrottledCalls() { return throttledCalls; }
    }
    
    /**
     * System resource statistics.
     */
    public static class SystemResourceStats {
        private final double averageMemoryBytes;
        private final long peakMemoryBytes;
        private final double averageCpuTimeMs;
        private final long sampleCount;
        private final long lastUpdate;
        
        public SystemResourceStats(double averageMemoryBytes, long peakMemoryBytes,
                                 double averageCpuTimeMs, long sampleCount, long lastUpdate) {
            this.averageMemoryBytes = averageMemoryBytes;
            this.peakMemoryBytes = peakMemoryBytes;
            this.averageCpuTimeMs = averageCpuTimeMs;
            this.sampleCount = sampleCount;
            this.lastUpdate = lastUpdate;
        }
        
        public double getAverageMemoryBytes() { return averageMemoryBytes; }
        public long getPeakMemoryBytes() { return peakMemoryBytes; }
        public double getAverageCpuTimeMs() { return averageCpuTimeMs; }
        public long getSampleCount() { return sampleCount; }
        public long getLastUpdate() { return lastUpdate; }
    }
    
    /**
     * Overall performance summary.
     */
    public static class AdminPerformanceSummary {
        private final long totalOperations;
        private final double averageDuration;
        private final double errorRate;
        private final double throughput;
        private final long uptime;
        private final long totalItemsProcessed;
        private final long totalBytesProcessed;
        private final double throttleRate;
        private final long maxPeakConcurrency;
        
        public AdminPerformanceSummary(long totalOperations, double averageDuration, double errorRate,
                                     double throughput, long uptime, long totalItemsProcessed,
                                     long totalBytesProcessed, double throttleRate, long maxPeakConcurrency) {
            this.totalOperations = totalOperations;
            this.averageDuration = averageDuration;
            this.errorRate = errorRate;
            this.throughput = throughput;
            this.uptime = uptime;
            this.totalItemsProcessed = totalItemsProcessed;
            this.totalBytesProcessed = totalBytesProcessed;
            this.throttleRate = throttleRate;
            this.maxPeakConcurrency = maxPeakConcurrency;
        }
        
        public long getTotalOperations() { return totalOperations; }
        public double getAverageDuration() { return averageDuration; }
        public double getErrorRate() { return errorRate; }
        public double getThroughput() { return throughput; }
        public long getUptime() { return uptime; }
        public long getTotalItemsProcessed() { return totalItemsProcessed; }
        public long getTotalBytesProcessed() { return totalBytesProcessed; }
        public double getThrottleRate() { return throttleRate; }
        public long getMaxPeakConcurrency() { return maxPeakConcurrency; }
    }
}