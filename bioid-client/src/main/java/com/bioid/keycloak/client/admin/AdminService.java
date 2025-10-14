package com.bioid.keycloak.client.admin;

import com.bioid.keycloak.client.admin.model.AdminDashboardData;
import com.bioid.keycloak.client.admin.model.EnrollmentLinkResult;
import com.bioid.keycloak.client.admin.model.UserEnrollmentStatus;
import com.bioid.keycloak.client.exception.BioIdException;

import java.util.List;

/**
 * Central administrative service for managing biometric enrollments and system operations.
 * Provides comprehensive administrative controls for realm administrators.
 * 
 * Note: This interface is designed to be implemented by modules that have access to 
 * Keycloak models and services. The bioid-client module provides the core functionality
 * that can be used by Keycloak-aware implementations.
 */
public interface AdminService {

    /**
     * Retrieves dashboard data with enrollment statistics and system health metrics.
     *
     * @param realmId the realm identifier to get dashboard data for
     * @return comprehensive dashboard data including enrollment statistics and system health
     * @throws BioIdException if dashboard data retrieval fails
     */
    AdminDashboardData getDashboardData(String realmId) throws BioIdException;

    /**
     * Retrieves user enrollment statuses with pagination support.
     *
     * @param realmId the realm identifier to query users from
     * @param offset the starting offset for pagination
     * @param limit the maximum number of results to return
     * @return list of user enrollment statuses with template metadata
     * @throws BioIdException if user enrollment data retrieval fails
     */
    List<UserEnrollmentStatus> getUserEnrollments(String realmId, int offset, int limit) throws BioIdException;

    /**
     * Generates a secure enrollment link for a user with configurable expiration.
     *
     * @param userId the user identifier to generate enrollment link for
     * @param validityHours the number of hours the link should be valid
     * @return enrollment link result with URL, token, and expiration details
     * @throws BioIdException if enrollment link generation fails
     */
    EnrollmentLinkResult generateEnrollmentLink(String userId, int validityHours) throws BioIdException;

    /**
     * Deletes a user's biometric enrollment with comprehensive audit logging.
     *
     * @param userId the user identifier whose enrollment should be deleted
     * @param reason the reason for deletion (for audit purposes)
     * @throws BioIdException if enrollment deletion fails
     */
    void deleteUserEnrollment(String userId, String reason) throws BioIdException;
}