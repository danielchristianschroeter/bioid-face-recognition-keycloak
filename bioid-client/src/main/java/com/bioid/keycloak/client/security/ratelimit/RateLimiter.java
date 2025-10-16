package com.bioid.keycloak.client.security.ratelimit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate limiter for preventing abuse.
 */
public class RateLimiter {
    
    private static final Logger logger = LoggerFactory.getLogger(RateLimiter.class);
    
    private final ConcurrentMap<String, RateLimitEntry> rateLimitMap = new ConcurrentHashMap<>();
    private static final int MAX_REQUESTS_PER_MINUTE = 60;
    private static final Duration RATE_LIMIT_WINDOW = Duration.ofMinutes(1);
    
    /**
     * Checks rate limiting for a given identifier.
     * 
     * @param identifier identifier to check (e.g., user ID, IP address)
     * @throws SecurityException if rate limit exceeded
     */
    public void checkRateLimit(String identifier) throws SecurityException {
        if (identifier == null || identifier.isEmpty()) {
            throw new SecurityException("Rate limit identifier cannot be null or empty");
        }
        
        Instant now = Instant.now();
        RateLimitEntry entry = rateLimitMap.computeIfAbsent(identifier, 
            k -> new RateLimitEntry(now));
        
        synchronized (entry) {
            // Clean up old entries
            if (Duration.between(entry.windowStart, now).compareTo(RATE_LIMIT_WINDOW) > 0) {
                entry.requestCount.set(0);
                entry.windowStart = now;
            }
            
            int currentCount = entry.requestCount.incrementAndGet();
            
            if (currentCount > MAX_REQUESTS_PER_MINUTE) {
                logger.warn("Rate limit exceeded for identifier: {} ({} requests in window)", 
                    sanitizeForLogging(identifier), currentCount);
                throw new SecurityException("Rate limit exceeded. Too many requests.");
            }
        }
    }
    
    /**
     * Cleans up expired rate limit entries.
     */
    public void cleanupRateLimitEntries() {
        Instant cutoff = Instant.now().minus(RATE_LIMIT_WINDOW);
        
        rateLimitMap.entrySet().removeIf(entry -> 
            Duration.between(entry.getValue().windowStart, cutoff).isNegative());
    }
    
    /**
     * Gets current rate limit statistics.
     */
    public RateLimitStats getRateLimitStats() {
        int totalEntries = rateLimitMap.size();
        int activeEntries = 0;
        int blockedEntries = 0;
        
        Instant now = Instant.now();
        
        for (RateLimitEntry entry : rateLimitMap.values()) {
            if (Duration.between(entry.windowStart, now).compareTo(RATE_LIMIT_WINDOW) <= 0) {
                activeEntries++;
                if (entry.requestCount.get() > MAX_REQUESTS_PER_MINUTE) {
                    blockedEntries++;
                }
            }
        }
        
        return new RateLimitStats(totalEntries, activeEntries, blockedEntries);
    }
    
    /**
     * Sanitizes input for safe logging.
     */
    private String sanitizeForLogging(String input) {
        if (input == null) {
            return "null";
        }
        
        // Truncate and remove potentially dangerous characters
        String sanitized = input.length() > 50 ? input.substring(0, 50) + "..." : input;
        return sanitized.replaceAll("[<>\"'&]", "_");
    }
    
    /**
     * Rate limiting entry for tracking requests.
     */
    private static class RateLimitEntry {
        volatile Instant windowStart;
        final AtomicInteger requestCount;
        
        RateLimitEntry(Instant windowStart) {
            this.windowStart = windowStart;
            this.requestCount = new AtomicInteger(0);
        }
    }
    
    /**
     * Rate limit statistics.
     */
    public static class RateLimitStats {
        private final int totalEntries;
        private final int activeEntries;
        private final int blockedEntries;
        
        public RateLimitStats(int totalEntries, int activeEntries, int blockedEntries) {
            this.totalEntries = totalEntries;
            this.activeEntries = activeEntries;
            this.blockedEntries = blockedEntries;
        }
        
        public int getTotalEntries() { return totalEntries; }
        public int getActiveEntries() { return activeEntries; }
        public int getBlockedEntries() { return blockedEntries; }
        
        @Override
        public String toString() {
            return String.format("RateLimitStats{total=%d, active=%d, blocked=%d}",
                totalEntries, activeEntries, blockedEntries);
        }
    }
}