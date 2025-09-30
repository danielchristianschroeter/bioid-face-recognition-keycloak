package com.bioid.keycloak.performance;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * High-performance credential cache with TTL and LRU eviction. Reduces database queries for
 * frequently accessed face credentials.
 */
public class CredentialCache {

  private static final Logger logger = LoggerFactory.getLogger(CredentialCache.class);

  private static final long DEFAULT_TTL_MINUTES = 15;
  private static final long CLEANUP_INTERVAL_MINUTES = 5;
  private static final int DEFAULT_MAX_SIZE = 1000;

  private final Map<String, CacheEntry> cache;
  private final long ttlMillis;
  private final int maxSize;
  private final ScheduledExecutorService cleanupExecutor;
  private final ScheduledFuture<?> cleanupTask;
  private final AtomicLong hits;
  private final AtomicLong misses;
  private final AtomicLong evictions;

  private static volatile CredentialCache instance;

  /** Cache entry with TTL and access tracking. */
  private static class CacheEntry {
    private final Object value;
    private final long createdTime;
    private volatile long lastAccessTime;
    private volatile long accessCount;

    public CacheEntry(Object value) {
      this.value = value;
      this.createdTime = System.currentTimeMillis();
      this.lastAccessTime = createdTime;
      this.accessCount = 1;
    }

    public Object getValue() {
      this.lastAccessTime = System.currentTimeMillis();
      this.accessCount++;
      return value;
    }

    public boolean isExpired(long ttlMillis) {
      return System.currentTimeMillis() - createdTime > ttlMillis;
    }

    public long getLastAccessTime() {
      return lastAccessTime;
    }

    public long getAccessCount() {
      return accessCount;
    }

    public long getAge() {
      return System.currentTimeMillis() - createdTime;
    }
  }

  private CredentialCache(long ttlMinutes, int maxSize) {
    this.cache = new ConcurrentHashMap<>();
    this.ttlMillis = ttlMinutes * 60 * 1000;
    this.maxSize = maxSize;
    this.hits = new AtomicLong(0);
    this.misses = new AtomicLong(0);
    this.evictions = new AtomicLong(0);

    this.cleanupExecutor =
        Executors.newSingleThreadScheduledExecutor(
            r -> {
              Thread t = new Thread(r, "credential-cache-cleanup");
              t.setDaemon(true);
              return t;
            });

    // Start cleanup task
    this.cleanupTask =
        cleanupExecutor.scheduleAtFixedRate(
            this::performCleanup,
            CLEANUP_INTERVAL_MINUTES,
            CLEANUP_INTERVAL_MINUTES,
            TimeUnit.MINUTES);

    logger.info(
        "Credential cache initialized with TTL: " + ttlMinutes + " minutes, max size: " + maxSize);
  }

  /** Get singleton instance with default settings. */
  public static CredentialCache getInstance() {
    return getInstance(DEFAULT_TTL_MINUTES, DEFAULT_MAX_SIZE);
  }

  /** Get singleton instance with custom settings. */
  public static CredentialCache getInstance(long ttlMinutes, int maxSize) {
    if (instance == null) {
      synchronized (CredentialCache.class) {
        if (instance == null) {
          instance = new CredentialCache(ttlMinutes, maxSize);
        }
      }
    }
    return instance;
  }

  /** Put a value in the cache. */
  public void put(String key, Object value) {
    if (key == null || value == null) {
      return;
    }

    // Check if we need to evict entries to make room
    if (cache.size() >= maxSize) {
      evictLeastRecentlyUsed();
    }

    cache.put(key, new CacheEntry(value));
    logger.debug("Cached credential for key: " + key);
  }

  /** Get a value from the cache. */
  @SuppressWarnings("unchecked")
  public <T> Optional<T> get(String key, Class<T> type) {
    if (key == null) {
      return Optional.empty();
    }

    CacheEntry entry = cache.get(key);
    if (entry == null) {
      misses.incrementAndGet();
      logger.debug("Cache miss for key: " + key);
      return Optional.empty();
    }

    // Check if entry is expired
    if (entry.isExpired(ttlMillis)) {
      cache.remove(key);
      misses.incrementAndGet();
      logger.debug("Cache entry expired for key: " + key);
      return Optional.empty();
    }

    hits.incrementAndGet();
    logger.debug("Cache hit for key: " + key);

    try {
      return Optional.of(type.cast(entry.getValue()));
    } catch (ClassCastException e) {
      logger.warn("Type mismatch in cache for key: " + key, e);
      cache.remove(key);
      return Optional.empty();
    }
  }

  /** Remove a value from the cache. */
  public void remove(String key) {
    if (key != null) {
      CacheEntry removed = cache.remove(key);
      if (removed != null) {
        logger.debug("Removed cached credential for key: " + key);
      }
    }
  }

  /** Clear all entries from the cache. */
  public void clear() {
    int size = cache.size();
    cache.clear();
    logger.info("Cleared credential cache, removed " + size + " entries");
  }

  /** Check if a key exists in the cache (without updating access time). */
  public boolean containsKey(String key) {
    if (key == null) {
      return false;
    }

    CacheEntry entry = cache.get(key);
    return entry != null && !entry.isExpired(ttlMillis);
  }

  /** Get cache statistics. */
  public CacheStats getStats() {
    long totalRequests = hits.get() + misses.get();
    double hitRate = totalRequests > 0 ? (double) hits.get() / totalRequests : 0.0;

    return new CacheStats(cache.size(), hits.get(), misses.get(), evictions.get(), hitRate);
  }

  /** Get detailed cache information for monitoring. */
  public Map<String, Object> getDetailedStats() {
    Map<String, Object> stats = new ConcurrentHashMap<>();
    stats.put("size", cache.size());
    stats.put("maxSize", maxSize);
    stats.put("hits", hits.get());
    stats.put("misses", misses.get());
    stats.put("evictions", evictions.get());
    stats.put("hitRate", getStats().getHitRate());
    stats.put("ttlMinutes", ttlMillis / (60 * 1000));

    // Calculate average age of entries
    long totalAge = 0;
    int count = 0;
    for (CacheEntry entry : cache.values()) {
      totalAge += entry.getAge();
      count++;
    }
    stats.put("averageAgeSeconds", count > 0 ? (totalAge / count) / 1000 : 0);

    return stats;
  }

  /** Shutdown the cache and cleanup resources. */
  public void shutdown() {
    logger.info("Shutting down credential cache");

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
    logger.info("Credential cache shutdown complete");
  }

  private void performCleanup() {
    try {
      int initialSize = cache.size();
      long currentTime = System.currentTimeMillis();

      // Remove expired entries
      cache
          .entrySet()
          .removeIf(
              entry -> {
                boolean expired = entry.getValue().isExpired(ttlMillis);
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
        logger.debug("Cache cleanup: removed " + (initialSize - finalSize) + " entries");
      }

    } catch (Exception e) {
      logger.error("Error during cache cleanup", e);
    }
  }

  private void evictLeastRecentlyUsed() {
    if (cache.isEmpty()) {
      return;
    }

    String lruKey = null;
    long oldestAccessTime = Long.MAX_VALUE;

    for (Map.Entry<String, CacheEntry> entry : cache.entrySet()) {
      long accessTime = entry.getValue().getLastAccessTime();
      if (accessTime < oldestAccessTime) {
        oldestAccessTime = accessTime;
        lruKey = entry.getKey();
      }
    }

    if (lruKey != null) {
      cache.remove(lruKey);
      evictions.incrementAndGet();
      logger.debug("Evicted LRU entry: " + lruKey);
    }
  }

  /** Cache statistics. */
  public static class CacheStats {
    private final int size;
    private final long hits;
    private final long misses;
    private final long evictions;
    private final double hitRate;

    public CacheStats(int size, long hits, long misses, long evictions, double hitRate) {
      this.size = size;
      this.hits = hits;
      this.misses = misses;
      this.evictions = evictions;
      this.hitRate = hitRate;
    }

    public int getSize() {
      return size;
    }

    public long getHits() {
      return hits;
    }

    public long getMisses() {
      return misses;
    }

    public long getEvictions() {
      return evictions;
    }

    public double getHitRate() {
      return hitRate;
    }

    public long getTotalRequests() {
      return hits + misses;
    }

    @Override
    public String toString() {
      return String.format(
          "CacheStats{size=%d, hits=%d, misses=%d, evictions=%d, hitRate=%.2f%%}",
          size, hits, misses, evictions, hitRate * 100);
    }
  }
}
