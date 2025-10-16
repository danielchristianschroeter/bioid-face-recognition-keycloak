package com.bioid.keycloak.client.logging;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Statistics for log events by operation type.
 * Maintains running statistics for performance monitoring and analysis.
 */
public class LogStatistics {
    private final AtomicLong totalCount = new AtomicLong(0);
    private final AtomicLong successCount = new AtomicLong(0);
    private final AtomicLong failureCount = new AtomicLong(0);
    private final AtomicLong totalDurationMs = new AtomicLong(0);
    private final AtomicLong durationSamples = new AtomicLong(0);
    
    private volatile Instant firstSeen;
    private volatile Instant lastSeen;
    
    public void incrementTotal() {
        totalCount.incrementAndGet();
        updateTimestamps();
    }
    
    public void incrementSuccess() {
        successCount.incrementAndGet();
    }
    
    public void incrementFailure() {
        failureCount.incrementAndGet();
    }
    
    public void addDuration(Duration duration) {
        totalDurationMs.addAndGet(duration.toMillis());
        durationSamples.incrementAndGet();
    }
    
    public void setLastSeen(Instant lastSeen) {
        this.lastSeen = lastSeen;
    }
    
    private void updateTimestamps() {
        Instant now = Instant.now();
        if (firstSeen == null) {
            firstSeen = now;
        }
        lastSeen = now;
    }
    
    public long getTotalCount() {
        return totalCount.get();
    }
    
    public long getSuccessCount() {
        return successCount.get();
    }
    
    public long getFailureCount() {
        return failureCount.get();
    }
    
    public double getSuccessRate() {
        long total = totalCount.get();
        return total > 0 ? (double) successCount.get() / total : 0.0;
    }
    
    public double getFailureRate() {
        long total = totalCount.get();
        return total > 0 ? (double) failureCount.get() / total : 0.0;
    }
    
    public Duration getAverageDuration() {
        long samples = durationSamples.get();
        if (samples > 0) {
            long avgMs = totalDurationMs.get() / samples;
            return Duration.ofMillis(avgMs);
        }
        return Duration.ZERO;
    }
    
    public Instant getFirstSeen() {
        return firstSeen;
    }
    
    public Instant getLastSeen() {
        return lastSeen;
    }
}