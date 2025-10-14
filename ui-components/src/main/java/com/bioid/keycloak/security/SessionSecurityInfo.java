package com.bioid.keycloak.security;

import java.time.Instant;

/**
 * Security information for tracking user sessions.
 */
public class SessionSecurityInfo {
    
    private String userId;
    private String sessionId;
    private String ipAddress;
    private String userAgent;
    private Instant createdAt;
    private Instant lastActivity;
    private boolean suspicious;
    private String suspiciousReason;

    public SessionSecurityInfo() {}

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getLastActivity() {
        return lastActivity;
    }

    public void setLastActivity(Instant lastActivity) {
        this.lastActivity = lastActivity;
    }

    public boolean isSuspicious() {
        return suspicious;
    }

    public void setSuspicious(boolean suspicious) {
        this.suspicious = suspicious;
    }

    public String getSuspiciousReason() {
        return suspiciousReason;
    }

    public void setSuspiciousReason(String suspiciousReason) {
        this.suspiciousReason = suspiciousReason;
    }

    /**
     * Checks if the session has been inactive for too long.
     * 
     * @param timeoutMinutes The timeout in minutes
     * @return true if session is expired
     */
    public boolean isExpired(int timeoutMinutes) {
        return lastActivity.isBefore(Instant.now().minusSeconds(timeoutMinutes * 60L));
    }

    /**
     * Checks if there's a potential IP address change.
     * 
     * @param currentIpAddress The current IP address
     * @return true if IP address has changed
     */
    public boolean hasIpAddressChanged(String currentIpAddress) {
        return !ipAddress.equals(currentIpAddress);
    }

    /**
     * Marks the session as suspicious with a reason.
     * 
     * @param reason The reason for suspicion
     */
    public void markSuspicious(String reason) {
        this.suspicious = true;
        this.suspiciousReason = reason;
    }

    @Override
    public String toString() {
        return String.format("SessionSecurityInfo{userId='%s', sessionId='%s', ipAddress='%s', " +
                           "createdAt=%s, lastActivity=%s, suspicious=%s}", 
                           userId, sessionId, ipAddress, createdAt, lastActivity, suspicious);
    }
}