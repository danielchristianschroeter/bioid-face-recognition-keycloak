package com.bioid.keycloak.security;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CSRF protection utility for administrative operations.
 * 
 * <p>Provides token generation, validation, and session management
 * for preventing Cross-Site Request Forgery attacks on administrative endpoints.
 */
@ApplicationScoped
public class CsrfProtection {

    private static final Logger logger = LoggerFactory.getLogger(CsrfProtection.class);
    private static final String CSRF_TOKEN_ATTRIBUTE = "bioid.csrf.token";
    private static final int TOKEN_LENGTH = 32;
    
    private final SecureRandom secureRandom = new SecureRandom();
    private final ConcurrentMap<String, CsrfTokenInfo> tokenCache = new ConcurrentHashMap<>();

    /**
     * Generates a new CSRF token for the session.
     * 
     * @param sessionId The session ID
     * @param userAgent The user agent string
     * @param ipAddress The client IP address
     * @return The generated CSRF token
     */
    public String generateToken(String sessionId, String userAgent, String ipAddress) {
        // Generate a cryptographically secure random token
        byte[] tokenBytes = new byte[TOKEN_LENGTH];
        secureRandom.nextBytes(tokenBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        
        // Cache token info for validation
        CsrfTokenInfo tokenInfo = new CsrfTokenInfo();
        tokenInfo.setToken(token);
        tokenInfo.setSessionId(sessionId);
        tokenInfo.setCreatedAt(System.currentTimeMillis());
        tokenInfo.setUserAgent(userAgent);
        tokenInfo.setIpAddress(ipAddress);
        
        tokenCache.put(token, tokenInfo);
        
        logger.debug("Generated CSRF token for session: {}", sessionId);
        
        return token;
    }

    /**
     * Validates a CSRF token.
     * 
     * @param token The token to validate
     * @param sessionId The session ID
     * @param currentIpAddress The current IP address
     * @return true if the token is valid
     */
    public boolean validateToken(String token, String sessionId, String currentIpAddress) {
        if (token == null || token.trim().isEmpty()) {
            logger.warn("CSRF validation failed: No token provided");
            return false;
        }
        
        if (sessionId == null) {
            logger.warn("CSRF validation failed: No session ID provided");
            return false;
        }
        
        // Validate using cached token info
        CsrfTokenInfo tokenInfo = tokenCache.get(token);
        if (tokenInfo == null) {
            logger.warn("CSRF validation failed: Token not found in cache");
            return false;
        }
        
        // Validate session ID matches
        if (!sessionId.equals(tokenInfo.getSessionId())) {
            logger.warn("CSRF validation failed: Session ID mismatch");
            return false;
        }
        
        // Validate IP address consistency (optional, can be disabled for mobile users)
        if (currentIpAddress != null && !currentIpAddress.equals(tokenInfo.getIpAddress())) {
            logger.warn("CSRF validation warning: IP address changed from {} to {} for session {}", 
                       tokenInfo.getIpAddress(), currentIpAddress, sessionId);
            // Don't fail validation for IP changes, just log warning
        }
        
        // Check token age (tokens expire after 1 hour)
        long tokenAge = System.currentTimeMillis() - tokenInfo.getCreatedAt();
        if (tokenAge > 3600000) { // 1 hour in milliseconds
            logger.warn("CSRF validation failed: Token expired for session {}", sessionId);
            tokenCache.remove(token);
            return false;
        }
        
        logger.debug("CSRF token validated successfully for session: {}", sessionId);
        return true;
    }

    /**
     * Invalidates the CSRF token.
     * 
     * @param token The token to invalidate
     */
    public void invalidateToken(String token) {
        if (token != null) {
            tokenCache.remove(token);
            logger.debug("Invalidated CSRF token: {}", token);
        }
    }

    /**
     * Performs cleanup of expired tokens.
     */
    public void cleanupExpiredTokens() {
        long currentTime = System.currentTimeMillis();
        int removedCount = 0;
        
        for (String token : tokenCache.keySet()) {
            CsrfTokenInfo tokenInfo = tokenCache.get(token);
            if (tokenInfo != null) {
                long tokenAge = currentTime - tokenInfo.getCreatedAt();
                if (tokenAge > 3600000) { // 1 hour
                    tokenCache.remove(token);
                    removedCount++;
                }
            }
        }
        
        if (removedCount > 0) {
            logger.debug("Cleaned up {} expired CSRF tokens", removedCount);
        }
    }

    /**
     * Gets statistics about CSRF token usage.
     * 
     * @return CSRF statistics
     */
    public CsrfStatistics getStatistics() {
        CsrfStatistics stats = new CsrfStatistics();
        stats.setActiveTokesCount(tokenCache.size());
        
        long currentTime = System.currentTimeMillis();
        int expiredCount = 0;
        
        for (CsrfTokenInfo tokenInfo : tokenCache.values()) {
            long tokenAge = currentTime - tokenInfo.getCreatedAt();
            if (tokenAge > 3600000) {
                expiredCount++;
            }
        }
        
        stats.setExpiredTokensCount(expiredCount);
        return stats;
    }

    // Removed getClientIpAddress method - IP address should be provided by caller

    /**
     * Information about a CSRF token.
     */
    private static class CsrfTokenInfo {
        private String token;
        private String sessionId;
        private long createdAt;
        private String userAgent;
        private String ipAddress;

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public String getSessionId() {
            return sessionId;
        }

        public void setSessionId(String sessionId) {
            this.sessionId = sessionId;
        }

        public long getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(long createdAt) {
            this.createdAt = createdAt;
        }

        public String getUserAgent() {
            return userAgent;
        }

        public void setUserAgent(String userAgent) {
            this.userAgent = userAgent;
        }

        public String getIpAddress() {
            return ipAddress;
        }

        public void setIpAddress(String ipAddress) {
            this.ipAddress = ipAddress;
        }
    }

    /**
     * CSRF protection statistics.
     */
    public static class CsrfStatistics {
        private int activeTokensCount;
        private int expiredTokensCount;

        public int getActiveTokesCount() {
            return activeTokensCount;
        }

        public void setActiveTokesCount(int activeTokensCount) {
            this.activeTokensCount = activeTokensCount;
        }

        public int getExpiredTokensCount() {
            return expiredTokensCount;
        }

        public void setExpiredTokensCount(int expiredTokensCount) {
            this.expiredTokensCount = expiredTokensCount;
        }

        @Override
        public String toString() {
            return String.format("CsrfStatistics{activeTokens=%d, expiredTokens=%d}", 
                               activeTokensCount, expiredTokensCount);
        }
    }
}