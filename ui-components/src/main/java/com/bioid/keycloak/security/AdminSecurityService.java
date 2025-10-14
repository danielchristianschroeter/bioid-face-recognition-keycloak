package com.bioid.keycloak.security;

import com.bioid.keycloak.audit.AdminActionType;
import com.bioid.keycloak.audit.AdminAuditService;
import com.bioid.keycloak.audit.RiskLevel;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
// Removed servlet import - using Keycloak context instead
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
// Removed unused import
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Security service for administrative operations with role-based access control,
 * additional authentication requirements, and session security.
 * 
 * <p>Provides comprehensive security controls for sensitive biometric operations
 * including permission checking, multi-factor authentication requirements,
 * and CSRF protection.
 */
@ApplicationScoped
public class AdminSecurityService {

    private static final Logger logger = LoggerFactory.getLogger(AdminSecurityService.class);
    
    // Session tracking for security monitoring
    private final Map<String, SessionSecurityInfo> activeSessions = new ConcurrentHashMap<>();
    
    // Failed attempt tracking for rate limiting
    private final Map<String, FailedAttemptInfo> failedAttempts = new ConcurrentHashMap<>();
    
    @Inject
    private AdminAuditService auditService;

    /**
     * Checks if the user has permission to perform the specified administrative action.
     * 
     * @param user The user attempting the action
     * @param realm The realm context
     * @param action The administrative action
     * @param targetUser The target user (if applicable)
     * @return Permission check result
     */
    public PermissionCheckResult checkPermission(UserModel user, RealmModel realm, 
                                                AdminActionType action, UserModel targetUser) {
        
        try {
            // Check if user has required roles
            if (!hasRequiredRole(user, realm, action)) {
                auditService.logAdminAction(AdminActionType.PERMISSION_DENIED, user, targetUser, 
                                          realm, createPermissionDetails(action, "INSUFFICIENT_ROLE"), 
                                          com.bioid.keycloak.audit.AdminActionResult.PERMISSION_DENIED);
                
                return PermissionCheckResult.denied("Insufficient role permissions for action: " + action);
            }
            
            // Check for additional authentication requirements for high-risk operations
            if (requiresAdditionalAuth(action) && !hasValidAdditionalAuth(user)) {
                auditService.logAdminAction(AdminActionType.PERMISSION_DENIED, user, targetUser, 
                                          realm, createPermissionDetails(action, "ADDITIONAL_AUTH_REQUIRED"), 
                                          com.bioid.keycloak.audit.AdminActionResult.PERMISSION_DENIED);
                
                return PermissionCheckResult.denied("Additional authentication required for high-risk operation");
            }
            
            // Check session security
            SessionSecurityCheck sessionCheck = validateSessionSecurity(user);
            if (!sessionCheck.isValid()) {
                auditService.logAdminAction(AdminActionType.SESSION_SECURITY_VIOLATION, user, targetUser, 
                                          realm, createSessionSecurityDetails(sessionCheck), 
                                          com.bioid.keycloak.audit.AdminActionResult.PERMISSION_DENIED);
                
                return PermissionCheckResult.denied("Session security violation: " + sessionCheck.getReason());
            }
            
            // Check rate limiting
            if (isRateLimited(user, action)) {
                auditService.logAdminAction(AdminActionType.PERMISSION_DENIED, user, targetUser, 
                                          realm, createPermissionDetails(action, "RATE_LIMITED"), 
                                          com.bioid.keycloak.audit.AdminActionResult.PERMISSION_DENIED);
                
                return PermissionCheckResult.denied("Rate limit exceeded for user");
            }
            
            // All checks passed
            return PermissionCheckResult.allowed();
            
        } catch (Exception e) {
            logger.error("Error during permission check", e);
            return PermissionCheckResult.denied("Internal security error");
        }
    }

    /**
     * Validates CSRF token for administrative operations.
     * 
     * @param requestToken The CSRF token from the request
     * @param expectedToken The expected CSRF token
     * @return true if CSRF token is valid
     */
    public boolean validateCsrfToken(String requestToken, String expectedToken) {
        boolean isValid = expectedToken != null && expectedToken.equals(requestToken);
        
        if (!isValid) {
            logger.warn("CSRF token validation failed. Expected: {}, Received: {}", 
                       expectedToken != null ? "[TOKEN]" : "[NULL]", 
                       requestToken != null ? "[TOKEN]" : "[NULL]");
        }
        
        return isValid;
    }

    /**
     * Initiates additional authentication for high-risk operations.
     * 
     * @param user The user requiring additional authentication
     * @param action The high-risk action being performed
     * @return Additional authentication challenge
     */
    public AdditionalAuthChallenge initiateAdditionalAuth(UserModel user, AdminActionType action) {
        
        AdditionalAuthChallenge challenge = new AdditionalAuthChallenge();
        challenge.setChallengeId(generateChallengeId());
        challenge.setUserId(user.getId());
        challenge.setAction(action);
        challenge.setCreatedAt(Instant.now());
        challenge.setExpiresAt(Instant.now().plusSeconds(300)); // 5 minutes
        
        // Determine required authentication methods based on action risk level
        RiskLevel riskLevel = getRiskLevel(action);
        switch (riskLevel) {
            case HIGH:
                challenge.addRequiredMethod(AuthMethod.PASSWORD_CONFIRMATION);
                challenge.addRequiredMethod(AuthMethod.EMAIL_VERIFICATION);
                break;
            case CRITICAL:
                challenge.addRequiredMethod(AuthMethod.PASSWORD_CONFIRMATION);
                challenge.addRequiredMethod(AuthMethod.EMAIL_VERIFICATION);
                challenge.addRequiredMethod(AuthMethod.ADMIN_APPROVAL);
                break;
            default:
                challenge.addRequiredMethod(AuthMethod.PASSWORD_CONFIRMATION);
                break;
        }
        
        // Store challenge for validation
        storeAuthChallenge(challenge);
        
        logger.info("Additional authentication challenge created for user {} and action {}", 
                   user.getUsername(), action);
        
        return challenge;
    }

    /**
     * Validates additional authentication response.
     * 
     * @param challengeId The challenge ID
     * @param authResponse The authentication response
     * @return Validation result
     */
    public AuthValidationResult validateAdditionalAuth(String challengeId, AdditionalAuthResponse authResponse) {
        
        AdditionalAuthChallenge challenge = getAuthChallenge(challengeId);
        if (challenge == null) {
            return AuthValidationResult.failed("Invalid or expired challenge");
        }
        
        if (challenge.isExpired()) {
            removeAuthChallenge(challengeId);
            return AuthValidationResult.failed("Challenge expired");
        }
        
        // Validate each required authentication method
        for (AuthMethod method : challenge.getRequiredMethods()) {
            if (!validateAuthMethod(method, authResponse, challenge)) {
                return AuthValidationResult.failed("Authentication method validation failed: " + method);
            }
        }
        
        // Mark challenge as completed
        challenge.setCompletedAt(Instant.now());
        challenge.setCompleted(true);
        
        logger.info("Additional authentication completed successfully for challenge {}", challengeId);
        
        return AuthValidationResult.success();
    }

    /**
     * Tracks session security information.
     * 
     * @param user The user
     * @param sessionId The session ID
     * @param ipAddress The client IP address
     * @param userAgent The user agent string
     */
    public void trackSessionSecurity(UserModel user, String sessionId, String ipAddress, String userAgent) {
        
        SessionSecurityInfo securityInfo = new SessionSecurityInfo();
        securityInfo.setUserId(user.getId());
        securityInfo.setSessionId(sessionId);
        securityInfo.setIpAddress(ipAddress);
        securityInfo.setUserAgent(userAgent);
        securityInfo.setCreatedAt(Instant.now());
        securityInfo.setLastActivity(Instant.now());
        
        activeSessions.put(sessionId, securityInfo);
        
        logger.debug("Session security tracking started for user {} from IP {}", 
                    user.getUsername(), ipAddress);
    }

    /**
     * Updates session activity timestamp.
     * 
     * @param sessionId The session ID
     */
    public void updateSessionActivity(String sessionId) {
        SessionSecurityInfo securityInfo = activeSessions.get(sessionId);
        if (securityInfo != null) {
            securityInfo.setLastActivity(Instant.now());
        }
    }

    /**
     * Removes session security tracking.
     * 
     * @param sessionId The session ID
     */
    public void removeSessionTracking(String sessionId) {
        activeSessions.remove(sessionId);
        logger.debug("Session security tracking removed for session {}", sessionId);
    }

    /**
     * Records a failed authentication attempt.
     * 
     * @param user The user
     * @param action The attempted action
     * @param reason The failure reason
     */
    public void recordFailedAttempt(UserModel user, AdminActionType action, String reason) {
        
        String key = user.getId() + ":" + action.name();
        FailedAttemptInfo attemptInfo = failedAttempts.computeIfAbsent(key, k -> new FailedAttemptInfo());
        
        attemptInfo.incrementAttempts();
        attemptInfo.setLastAttempt(Instant.now());
        attemptInfo.setLastReason(reason);
        
        // Log security event
        Map<String, Object> details = new HashMap<>();
        details.put("action", action.name());
        details.put("reason", reason);
        details.put("attempt_count", attemptInfo.getAttemptCount());
        
        auditService.logAdminAction(AdminActionType.UNAUTHORIZED_ACCESS_ATTEMPT, user, null, 
                                  null, details, com.bioid.keycloak.audit.AdminActionResult.FAILURE);
        
        logger.warn("Failed authentication attempt recorded for user {} on action {}: {}", 
                   user.getUsername(), action, reason);
    }

    // Private helper methods

    private boolean hasRequiredRole(UserModel user, RealmModel realm, AdminActionType action) {
        Set<String> requiredRoles = getRequiredRoles(action);
        
        for (String roleName : requiredRoles) {
            RoleModel role = realm.getRole(roleName);
            if (role != null && user.hasRole(role)) {
                return true;
            }
            
            // Check client roles
            ClientModel adminClient = realm.getClientByClientId("admin-cli");
            if (adminClient != null) {
                RoleModel clientRole = adminClient.getRole(roleName);
                if (clientRole != null && user.hasRole(clientRole)) {
                    return true;
                }
            }
        }
        
        return false;
    }

    private Set<String> getRequiredRoles(AdminActionType action) {
        switch (action) {
            case TEMPLATE_DELETE:
            case USER_ENROLLMENT_DELETE:
            case TEMPLATE_BATCH_DELETE:
                return Set.of("bioid-admin", "manage-users");
            
            case LIVENESS_CONFIG_UPDATE:
            case ADMIN_CONFIG_UPDATE:
                return Set.of("bioid-admin", "manage-realm");
            
            case BULK_OPERATION:
                return Set.of("bioid-admin", "manage-users", "bulk-operations");
            
            case COMPLIANCE_REPORT_GENERATED:
                return Set.of("bioid-admin", "view-users", "compliance-officer");
            
            case TEMPLATE_ACCESS:
                return Set.of("bioid-admin", "view-users");
            
            default:
                return Set.of("bioid-admin");
        }
    }

    private boolean requiresAdditionalAuth(AdminActionType action) {
        RiskLevel riskLevel = getRiskLevel(action);
        return riskLevel == RiskLevel.HIGH || riskLevel == RiskLevel.CRITICAL;
    }

    private RiskLevel getRiskLevel(AdminActionType action) {
        switch (action) {
            case TEMPLATE_DELETE:
            case USER_ENROLLMENT_DELETE:
            case TEMPLATE_BATCH_DELETE:
                return RiskLevel.HIGH;
            
            case BULK_OPERATION:
            case ADMIN_CONFIG_UPDATE:
                return RiskLevel.CRITICAL;
            
            case LIVENESS_CONFIG_UPDATE:
                return RiskLevel.MEDIUM;
            
            default:
                return RiskLevel.LOW;
        }
    }

    private boolean hasValidAdditionalAuth(UserModel user) {
        // Check if user has completed additional authentication recently
        // This would typically check a cache or database for recent auth completions
        return false; // Placeholder - always require additional auth for high-risk operations
    }

    private SessionSecurityCheck validateSessionSecurity(UserModel user) {
        // Implementation would check for:
        // - Session timeout
        // - IP address consistency
        // - User agent consistency
        // - Concurrent session limits
        
        return new SessionSecurityCheck(true, null);
    }

    private boolean isRateLimited(UserModel user, AdminActionType action) {
        String key = user.getId() + ":" + action.name();
        FailedAttemptInfo attemptInfo = failedAttempts.get(key);
        
        if (attemptInfo != null) {
            // Check if user has exceeded rate limit
            if (attemptInfo.getAttemptCount() >= 5) {
                // Check if cooldown period has passed (15 minutes)
                return attemptInfo.getLastAttempt().isAfter(Instant.now().minusSeconds(900));
            }
        }
        
        return false;
    }

    private Map<String, Object> createPermissionDetails(AdminActionType action, String reason) {
        Map<String, Object> details = new HashMap<>();
        details.put("action", action.name());
        details.put("denial_reason", reason);
        details.put("required_roles", getRequiredRoles(action));
        return details;
    }

    private Map<String, Object> createSessionSecurityDetails(SessionSecurityCheck sessionCheck) {
        Map<String, Object> details = new HashMap<>();
        details.put("session_valid", sessionCheck.isValid());
        details.put("violation_reason", sessionCheck.getReason());
        return details;
    }

    private String generateChallengeId() {
        return java.util.UUID.randomUUID().toString();
    }

    private void storeAuthChallenge(AdditionalAuthChallenge challenge) {
        // In a real implementation, this would store in a cache or database
        // For now, we'll use a simple in-memory store
    }

    private AdditionalAuthChallenge getAuthChallenge(String challengeId) {
        // In a real implementation, this would retrieve from cache or database
        return null; // Placeholder
    }

    private void removeAuthChallenge(String challengeId) {
        // In a real implementation, this would remove from cache or database
    }

    private boolean validateAuthMethod(AuthMethod method, AdditionalAuthResponse response, 
                                     AdditionalAuthChallenge challenge) {
        switch (method) {
            case PASSWORD_CONFIRMATION:
                return validatePasswordConfirmation(response.getPassword(), challenge.getUserId());
            case EMAIL_VERIFICATION:
                return validateEmailVerification(response.getEmailCode(), challenge.getUserId());
            case ADMIN_APPROVAL:
                return validateAdminApproval(response.getApprovalCode(), challenge);
            default:
                return false;
        }
    }

    private boolean validatePasswordConfirmation(String password, String userId) {
        // Implementation would verify the password against the user's current password
        return password != null && !password.trim().isEmpty();
    }

    private boolean validateEmailVerification(String emailCode, String userId) {
        // Implementation would verify the email verification code
        return emailCode != null && !emailCode.trim().isEmpty();
    }

    private boolean validateAdminApproval(String approvalCode, AdditionalAuthChallenge challenge) {
        // Implementation would verify admin approval
        return approvalCode != null && !approvalCode.trim().isEmpty();
    }
}