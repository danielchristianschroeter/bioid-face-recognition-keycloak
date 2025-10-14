package com.bioid.keycloak.security;

import java.time.Instant;

/**
 * Information about failed authentication attempts for rate limiting.
 */
public class FailedAttemptInfo {
    
    private int attemptCount;
    private Instant firstAttempt;
    private Instant lastAttempt;
    private String lastReason;

    public FailedAttemptInfo() {
        this.attemptCount = 0;
        this.firstAttempt = Instant.now();
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(int attemptCount) {
        this.attemptCount = attemptCount;
    }

    public Instant getFirstAttempt() {
        return firstAttempt;
    }

    public void setFirstAttempt(Instant firstAttempt) {
        this.firstAttempt = firstAttempt;
    }

    public Instant getLastAttempt() {
        return lastAttempt;
    }

    public void setLastAttempt(Instant lastAttempt) {
        this.lastAttempt = lastAttempt;
    }

    public String getLastReason() {
        return lastReason;
    }

    public void setLastReason(String lastReason) {
        this.lastReason = lastReason;
    }

    /**
     * Increments the attempt count and updates the last attempt timestamp.
     */
    public void incrementAttempts() {
        this.attemptCount++;
        this.lastAttempt = Instant.now();
        
        if (this.firstAttempt == null) {
            this.firstAttempt = this.lastAttempt;
        }
    }

    /**
     * Resets the failed attempt counter.
     */
    public void reset() {
        this.attemptCount = 0;
        this.firstAttempt = null;
        this.lastAttempt = null;
        this.lastReason = null;
    }

    /**
     * Checks if the attempts should be reset due to time elapsed.
     * 
     * @param resetIntervalMinutes The interval in minutes after which to reset
     * @return true if attempts should be reset
     */
    public boolean shouldReset(int resetIntervalMinutes) {
        if (lastAttempt == null) {
            return false;
        }
        
        return lastAttempt.isBefore(Instant.now().minusSeconds(resetIntervalMinutes * 60L));
    }

    /**
     * Checks if the user is currently rate limited.
     * 
     * @param maxAttempts Maximum allowed attempts
     * @param lockoutMinutes Lockout duration in minutes
     * @return true if user is rate limited
     */
    public boolean isRateLimited(int maxAttempts, int lockoutMinutes) {
        if (attemptCount < maxAttempts) {
            return false;
        }
        
        if (lastAttempt == null) {
            return false;
        }
        
        return lastAttempt.isAfter(Instant.now().minusSeconds(lockoutMinutes * 60L));
    }

    @Override
    public String toString() {
        return String.format("FailedAttemptInfo{attemptCount=%d, firstAttempt=%s, lastAttempt=%s, lastReason='%s'}", 
                           attemptCount, firstAttempt, lastAttempt, lastReason);
    }
}