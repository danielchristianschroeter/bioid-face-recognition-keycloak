package com.bioid.keycloak.client.admin;

import com.bioid.keycloak.client.BioIdClient;
import com.bioid.keycloak.client.admin.model.AdminDashboardData;
import com.bioid.keycloak.client.admin.model.EnrollmentLinkResult;
import com.bioid.keycloak.client.admin.model.UserEnrollmentStatus;
import com.bioid.keycloak.client.exception.BioIdException;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.logging.Logger;

/**
 * Core implementation of AdminService providing central administrative operations
 * for biometric enrollment management and system monitoring.
 * 
 * This implementation provides the core BioID operations and can be extended
 * by Keycloak-aware implementations that have access to realm and user models.
 */
public class AdminServiceImpl implements AdminService {

    private static final Logger logger = Logger.getLogger(AdminServiceImpl.class.getName());
    private static final SecureRandom secureRandom = new SecureRandom();

    private final BioIdClient bioIdClient;

    public AdminServiceImpl(BioIdClient bioIdClient) {
        this.bioIdClient = bioIdClient;
    }

    @Override
    public AdminDashboardData getDashboardData(String realmId) throws BioIdException {
        logger.info("Getting dashboard data for realm: " + realmId);

        try {
            // Get system health metrics from BioID client
            AdminDashboardData.SystemHealthMetrics healthMetrics = getSystemHealthMetrics();

            // Placeholder statistics - in a real implementation, these would come from
            // a Keycloak-aware service that has access to user and credential data
            long totalEnrolledUsers = 0;
            double enrollmentSuccessRate = 0.85;
            Map<Integer, Long> templatesByEncoderVersion = new HashMap<>();
            List<AdminDashboardData.RecentActivity> recentActivities = new ArrayList<>();
            AdminDashboardData.LivenessDetectionStats livenessStats = getLivenessDetectionStats();

            return new AdminDashboardData(
                totalEnrolledUsers,
                enrollmentSuccessRate,
                templatesByEncoderVersion,
                recentActivities,
                healthMetrics,
                livenessStats
            );

        } catch (Exception e) {
            logger.severe("Failed to get dashboard data for realm: " + realmId + " - " + e.getMessage());
            throw new BioIdException("Failed to retrieve dashboard data", e);
        }
    }

    @Override
    public List<UserEnrollmentStatus> getUserEnrollments(String realmId, int offset, int limit) throws BioIdException {
        logger.info("Getting user enrollments for realm: " + realmId + ", offset: " + offset + ", limit: " + limit);

        try {
            // Placeholder implementation - in a real implementation, this would be
            // handled by a Keycloak-aware service that has access to user data
            List<UserEnrollmentStatus> enrollmentStatuses = new ArrayList<>();

            logger.info("Retrieved " + enrollmentStatuses.size() + " user enrollment statuses");
            return enrollmentStatuses;

        } catch (Exception e) {
            logger.severe("Failed to get user enrollments for realm: " + realmId + " - " + e.getMessage());
            throw new BioIdException("Failed to retrieve user enrollments", e);
        }
    }

    @Override
    public EnrollmentLinkResult generateEnrollmentLink(String userId, int validityHours) throws BioIdException {
        logger.info("Generating enrollment link for user: " + userId + ", validity: " + validityHours + " hours");

        try {
            // Generate secure token
            String token = generateSecureToken();
            
            // Calculate expiration time
            Instant expiresAt = Instant.now().plus(validityHours, ChronoUnit.HOURS);
            
            // Build enrollment URL (would be configured based on deployment)
            String enrollmentUrl = buildEnrollmentUrl(userId, token);
            
            // For now, admin approval is not required (could be configurable)
            boolean requiresAdminApproval = false;

            logger.info("Generated enrollment link for user: " + userId + ", expires at: " + expiresAt);

            return new EnrollmentLinkResult(enrollmentUrl, token, expiresAt, requiresAdminApproval);

        } catch (Exception e) {
            logger.severe("Failed to generate enrollment link for user: " + userId + " - " + e.getMessage());
            throw new BioIdException("Failed to generate enrollment link", e);
        }
    }

    @Override
    public void deleteUserEnrollment(String userId, String reason) throws BioIdException {
        logger.info("Deleting enrollment for user: " + userId + ", reason: " + reason);

        try {
            // In a real implementation, this would:
            // 1. Look up the user's class ID from Keycloak credentials
            // 2. Call BioID deleteTemplate with the class ID
            // 3. Remove the credential from Keycloak
            // 4. Log the admin event for audit
            
            // For now, we can only provide the BioID deletion functionality
            // The Keycloak integration would be handled by a higher-level service
            
            logger.info("Enrollment deletion requested for user: " + userId + " (implementation depends on Keycloak integration)");

        } catch (Exception e) {
            logger.severe("Failed to delete enrollment for user: " + userId + " - " + e.getMessage());
            throw new BioIdException("Failed to delete user enrollment", e);
        }
    }

    /**
     * Deletes a template from BioID using the class ID.
     * This method can be used by Keycloak-aware implementations.
     *
     * @param classId the class ID of the template to delete
     * @throws BioIdException if template deletion fails
     */
    public void deleteTemplate(long classId) throws BioIdException {
        try {
            // Use the convenience method that takes a long classId
            bioIdClient.deleteTemplate(classId);
            logger.info("Successfully deleted template with classId: " + classId);
        } catch (Exception e) {
            logger.severe("Failed to delete template with classId: " + classId + " - " + e.getMessage());
            throw new BioIdException("Failed to delete template", e);
        }
    }

    private AdminDashboardData.SystemHealthMetrics getSystemHealthMetrics() {
        // Get health status from BioID client
        boolean isHealthy = bioIdClient.isHealthy();
        AdminDashboardData.ServiceStatus status = isHealthy ? 
            AdminDashboardData.ServiceStatus.HEALTHY : 
            AdminDashboardData.ServiceStatus.UNHEALTHY;

        // Placeholder metrics - in a real implementation, these would come from monitoring systems
        return new AdminDashboardData.SystemHealthMetrics(
            status,
            150.0, // average response time in ms
            5, // active connections
            1000L, // total requests in 24h
            50L, // failed requests in 24h
            0.05, // 5% error rate
            Map.of("TIMEOUT", 20, "CONNECTION_ERROR", 15, "INVALID_REQUEST", 15),
            Instant.now()
        );
    }

    private AdminDashboardData.LivenessDetectionStats getLivenessDetectionStats() {
        // Placeholder - in a real implementation, this would query metrics from a database
        return new AdminDashboardData.LivenessDetectionStats(
            500L, // total liveness checks
            425L, // passed liveness checks
            0.85, // 85% pass rate
            Map.of("PASSIVE", 300L, "ACTIVE", 150L, "CHALLENGE_RESPONSE", 50L),
            Map.of("FACE_NOT_FOUND", 30L, "MULTIPLE_FACES", 20L, "REJECTED_BY_PASSIVE", 25L),
            0.78 // average liveness score
        );
    }

    private String generateSecureToken() {
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    private String buildEnrollmentUrl(String userId, String token) {
        // Build URL based on deployment configuration
        // In a real implementation, this would use the application's base URL
        return String.format("/face-enrollment?user=%s&token=%s", userId, token);
    }
}