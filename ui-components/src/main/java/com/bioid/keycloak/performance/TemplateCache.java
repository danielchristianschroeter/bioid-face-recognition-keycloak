package com.bioid.keycloak.performance;

import com.bioid.keycloak.config.AdminConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * High-performance template cache with configurable TTL for frequently accessed template data.
 * 
 * This cache is specifically designed for administrative operations that need to access
 * template status information repeatedly. It supports configurable TTL based on the
 * AdminConfiguration and provides detailed metrics for monitoring.
 * 
 * Requirements addressed: 9.1, 9.2, 9.5, 9.6
 */
public class TemplateCache {
    
    private static final Logger logger = LoggerFactory.getLogger(TemplateCache.class);
    
    private static final int DEFAULT_MAX_SIZE = 5000;
    private static final long CLEANUP_INTERVAL_MINUTES = 2;
    
    private final Map<String, CacheEntry> cache;
    private final int maxSize;
    private final ScheduledExecutorService cleanupExecutor;
    private final ScheduledFuture<?> cleanupTask;
    
    // Metrics
    private final AtomicLong hits = new AtomicLong(0);
    private final AtomicLong misses = new AtomicLong(0);
    private final AtomicLong evictions = new AtomicLong(0);
    private final AtomicLong totalRequests = new AtomicLong(0);
    
    private volatile Duration ttl = Duration.ofMinutes(5); // Default TTL
    
    private static volatile TemplateCache instance;
    
    /**
     * Cache entry with TTL and access tracking.
     */
    private static class CacheEntry {
        private final Object value;
        private final Instant createdAt;
        private volatile Instant lastAccessedAt;
        private volatile long accessCount;
        private final String key;
        
        public CacheEntry(String key, Object value) {
            this.key = key;
            this.value = value;
            this.createdAt = Instant.now();
            this.lastAccessedAt = createdAt;
            this.accessCount = 1;
        }
        
        public Object getValue() {
            this.lastAccessedAt = Instant.now();
            this.accessCount++;
            return value;
        }
        
        public boolean isExpired(Duration ttl) {
            return Instant.now().isAfter(createdAt.plus(ttl));
        }
        
        public Instant getCreatedAt() { return createdAt; }
        public Instant getLastAccessedAt() { return lastAccessedAt; }
        public long getAccessCount() { return accessCount; }
        public String getKey() { return key; }
        
        public Duration getAge() {
            return Duration.between(createdAt, Instant.now());
        }
        
        public Duration getTimeSinceLastAccess() {
            return Duration.between(lastAccessedAt, Instant.now());
        }
    }
    
    private TemplateCache(int maxSize) {
        this.cache = new ConcurrentHashMap<>();
        this.maxSize = maxSize;
        
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "template-cache-cleanup");
            t.setDaemon(true);
            return t;
        });
        
        // Start cleanup task
        this.cleanupTask = cleanupExecutor.scheduleAtFixedRate(
            this::performCleanup,
            CLEANUP_INTERVAL_MINUTES,
            CLEANUP_INTERVAL_MINUTES,
            TimeUnit.MINUTES
        );
        
        logger.info("Template cache initialized with max size: {}, TTL: {}", maxSize, ttl);
    }
    
    /**
     * Gets the singleton instance with default settings.
     */
    public static TemplateCache getInstance() {
        return getInstance(DEFAULT_MAX_SIZE);
    }
    
    /**
     * Gets the singleton instance with custom max size.
     */
    public static TemplateCache getInstance(int maxSize) {
        if (instance == null) {
            synchronized (TemplateCache.class) {
                if (instance == null) {
                    instance = new TemplateCache(maxSize);
                }
            }
        }
        return instance;
    }
    
    /**
     * Updates the TTL based on AdminConfiguration.
     */
    public void updateTtl(AdminConfiguration config) {
        Duration newTtl = config.getTemplateCacheTtl();
        if (!newTtl.equals(this.ttl)) {
            logger.info("Updating template cache TTL from {} to {}", this.ttl, newTtl);
            this.ttl = newTtl;
        }
    }
    
    /**
     * Puts a value in the cache.
     */
    public void put(String key, Object value) {
        if (key == null || value == null) {
            return;
        }
        
        totalRequests.incrementAndGet();
        
        // Check if we need to evict entries to make room
        if (cache.size() >= maxSize) {
            evictLeastRecentlyUsed();
        }
        
        cache.put(key, new CacheEntry(key, value));
        logger.debug("Cached template data for key: {}", key);
    }
    
    /**
     * Gets a value from the cache.
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(String key, Class<T> type) {
        if (key == null) {
            return Optional.empty();
        }
        
        totalRequests.incrementAndGet();
        
        CacheEntry entry = cache.get(key);
        if (entry == null) {
            misses.incrementAndGet();
            logger.debug("Cache miss for key: {}", key);
            return Optional.empty();
        }
        
        // Check if entry is expired
        if (entry.isExpired(ttl)) {
            cache.remove(key);
            misses.incrementAndGet();
            logger.debug("Cache entry expired for key: {}", key);
            return Optional.empty();
        }
        
        hits.incrementAndGet();
        logger.debug("Cache hit for key: {}", key);
        
        try {
            return Optional.of(type.cast(entry.getValue()));
        } catch (ClassCastException e) {
            logger.warn("Type mismatch in cache for key: {}", key, e);
            cache.remove(key);
            return Optional.empty();
        }
    }
    
    /**
     * Removes a value from the cache.
     */
    public void remove(String key) {
        if (key != null) {
            CacheEntry removed = cache.remove(key);
            if (removed != null) {
                logger.debug("Removed cached template data for key: {}", key);
            }
        }
    }
    
    /**
     * Clears all entries from the cache.
     */
    public void clear() {
        int size = cache.size();
        cache.clear();
        logger.info("Cleared template cache, removed {} entries", size);
    }
    
    /**
     * Checks if a key exists in the cache (without updating access time).
     */
    public boolean containsKey(String key) {
        if (key == null) {
            return false;
        }
        
        CacheEntry entry = cache.get(key);
        return entry != null && !entry.isExpired(ttl);
    }
    
    /**
     * Gets cache statistics.
     */
    public TemplateCacheStats getStats() {
        long totalReq = totalRequests.get();
        long hitCount = hits.get();
        long missCount = misses.get();
        double hitRate = totalReq > 0 ? (double) hitCount / totalReq : 0.0;
        
        // Calculate memory usage estimate
        long estimatedMemoryBytes = cache.size() * 1024; // Rough estimate: 1KB per entry
        
        // Calculate average age of entries
        Duration averageAge = Duration.ZERO;
        if (!cache.isEmpty()) {
            long totalAgeMillis = cache.values().stream()
                .mapToLong(entry -> entry.getAge().toMillis())
                .sum();
            averageAge = Duration.ofMillis(totalAgeMillis / cache.size());
        }
        
        return new TemplateCacheStats(
            cache.size(),
            maxSize,
            hitCount,
            missCount,
            evictions.get(),
            hitRate,
            ttl,
            estimatedMemoryBytes,
            averageAge
        );
    }
    
    /**
     * Gets detailed cache information for monitoring.
     */
    public Map<String, Object> getDetailedStats() {
        TemplateCacheStats stats = getStats();
        Map<String, Object> detailed = new ConcurrentHashMap<>();
        
        detailed.put("size", stats.getSize());
        detailed.put("maxSize", stats.getMaxSize());
        detailed.put("hits", stats.getHits());
        detailed.put("misses", stats.getMisses());
        detailed.put("evictions", stats.getEvictions());
        detailed.put("hitRate", stats.getHitRate());
        detailed.put("ttlMinutes", stats.getTtl().toMinutes());
        detailed.put("estimatedMemoryBytes", stats.getEstimatedMemoryBytes());
        detailed.put("averageAgeSeconds", stats.getAverageAge().getSeconds());
        
        // Add entry details for debugging
        if (logger.isDebugEnabled()) {
            Map<String, Object> entryDetails = new ConcurrentHashMap<>();
            for (CacheEntry entry : cache.values()) {
                Map<String, Object> entryInfo = new ConcurrentHashMap<>();
                entryInfo.put("age", entry.getAge().getSeconds());
                entryInfo.put("accessCount", entry.getAccessCount());
                entryInfo.put("timeSinceLastAccess", entry.getTimeSinceLastAccess().getSeconds());
                entryDetails.put(entry.getKey(), entryInfo);
            }
            detailed.put("entries", entryDetails);
        }
        
        return detailed;
    }
    
    /**
     * Performs cache cleanup by removing expired and least recently used entries.
     */
    private void performCleanup() {
        try {
            int initialSize = cache.size();
            
            // Remove expired entries
            cache.entrySet().removeIf(entry -> {
                boolean expired = entry.getValue().isExpired(ttl);
                if (expired) {
                    evictions.incrementAndGet();
                }
                return expired;
            });
            
            // If still over capacity, remove least recently used entries
            while (cache.size() > maxSize) {
                evictLeastRecentlyUsed();
            }
            
            int finalSize = cache.size();
            if (initialSize != finalSize) {
                logger.debug("Template cache cleanup: removed {} entries", initialSize - finalSize);
            }
            
        } catch (Exception e) {
            logger.error("Error during template cache cleanup", e);
        }
    }
    
    /**
     * Evicts the least recently used entry.
     */
    private void evictLeastRecentlyUsed() {
        if (cache.isEmpty()) {
            return;
        }
        
        String lruKey = null;
        Instant oldestAccessTime = Instant.MAX;
        
        for (Map.Entry<String, CacheEntry> entry : cache.entrySet()) {
            Instant accessTime = entry.getValue().getLastAccessedAt();
            if (accessTime.isBefore(oldestAccessTime)) {
                oldestAccessTime = accessTime;
                lruKey = entry.getKey();
            }
        }
        
        if (lruKey != null) {
            cache.remove(lruKey);
            evictions.incrementAndGet();
            logger.debug("Evicted LRU entry: {}", lruKey);
        }
    }
    
    /**
     * Shuts down the cache and cleanup resources.
     */
    public void shutdown() {
        logger.info("Shutting down template cache");
        
        // Cancel cleanup task
        if (cleanupTask != null) {
            cleanupTask.cancel(true);
        }
        
        // Clear cache
        clear();
        
        // Shutdown executor
        cleanupExecutor.shutdown();
        try {
            if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        instance = null;
        logger.info("Template cache shutdown complete");
    }
    
    /**
     * Template cache statistics.
     */
    public static class TemplateCacheStats {
        private final int size;
        private final int maxSize;
        private final long hits;
        private final long misses;
        private final long evictions;
        private final double hitRate;
        private final Duration ttl;
        private final long estimatedMemoryBytes;
        private final Duration averageAge;
        
        public TemplateCacheStats(int size, int maxSize, long hits, long misses, long evictions,
                                double hitRate, Duration ttl, long estimatedMemoryBytes, Duration averageAge) {
            this.size = size;
            this.maxSize = maxSize;
            this.hits = hits;
            this.misses = misses;
            this.evictions = evictions;
            this.hitRate = hitRate;
            this.ttl = ttl;
            this.estimatedMemoryBytes = estimatedMemoryBytes;
            this.averageAge = averageAge;
        }
        
        public int getSize() { return size; }
        public int getMaxSize() { return maxSize; }
        public long getHits() { return hits; }
        public long getMisses() { return misses; }
        public long getEvictions() { return evictions; }
        public double getHitRate() { return hitRate; }
        public Duration getTtl() { return ttl; }
        public long getEstimatedMemoryBytes() { return estimatedMemoryBytes; }
        public Duration getAverageAge() { return averageAge; }
        
        public long getTotalRequests() { return hits + misses; }
        
        @Override
        public String toString() {
            return String.format(
                "TemplateCacheStats{size=%d/%d, hits=%d, misses=%d, hitRate=%.2f%%, ttl=%s}",
                size, maxSize, hits, misses, hitRate * 100, ttl
            );
        }
    }
}