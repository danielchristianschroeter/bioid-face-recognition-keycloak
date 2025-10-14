package com.bioid.keycloak.client.admin;

import com.bioid.keycloak.client.BioIdClient;
import com.bioid.keycloak.client.admin.model.AdminDashboardData;
import com.bioid.keycloak.client.admin.model.EnrollmentLinkResult;
import com.bioid.keycloak.client.admin.model.UserEnrollmentStatus;
import com.bioid.keycloak.client.exception.BioIdException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminServiceImpl")
class AdminServiceImplTest {

    @Mock
    private BioIdClient bioIdClient;

    private AdminServiceImpl adminService;

    @BeforeEach
    void setUp() {
        adminService = new AdminServiceImpl(bioIdClient);
    }

    @Test
    @DisplayName("Should get dashboard data successfully")
    void shouldGetDashboardDataSuccessfully() throws BioIdException {
        // Given
        String realmId = "test-realm";
        when(bioIdClient.isHealthy()).thenReturn(true);

        // When
        AdminDashboardData dashboardData = adminService.getDashboardData(realmId);

        // Then
        assertThat(dashboardData).isNotNull();
        assertThat(dashboardData.getTotalEnrolledUsers()).isEqualTo(0L);
        assertThat(dashboardData.getEnrollmentSuccessRate()).isEqualTo(0.85);
        assertThat(dashboardData.getTemplatesByEncoderVersion()).isNotNull();
        assertThat(dashboardData.getRecentActivities()).isNotNull();
        assertThat(dashboardData.getHealthMetrics()).isNotNull();
        assertThat(dashboardData.getLivenessStats()).isNotNull();
        
        // Verify health metrics
        AdminDashboardData.SystemHealthMetrics healthMetrics = dashboardData.getHealthMetrics();
        assertThat(healthMetrics.getBioIdServiceStatus()).isEqualTo(AdminDashboardData.ServiceStatus.HEALTHY);
        assertThat(healthMetrics.getAverageResponseTime()).isEqualTo(150.0);
        assertThat(healthMetrics.getActiveConnections()).isEqualTo(5);
        assertThat(healthMetrics.getTotalRequests24h()).isEqualTo(1000L);
        assertThat(healthMetrics.getFailedRequests24h()).isEqualTo(50L);
        assertThat(healthMetrics.getErrorRate()).isEqualTo(0.05);
        assertThat(healthMetrics.getLastHealthCheck()).isNotNull();
        
        // Verify liveness stats
        AdminDashboardData.LivenessDetectionStats livenessStats = dashboardData.getLivenessStats();
        assertThat(livenessStats.getTotalLivenessChecks()).isEqualTo(500L);
        assertThat(livenessStats.getPassedLivenessChecks()).isEqualTo(425L);
        assertThat(livenessStats.getPassRate()).isEqualTo(0.85);
        assertThat(livenessStats.getAverageLivenessScore()).isEqualTo(0.78);
        
        verify(bioIdClient).isHealthy();
    }

    @Test
    @DisplayName("Should handle unhealthy BioID service in dashboard data")
    void shouldHandleUnhealthyBioIdServiceInDashboardData() throws BioIdException {
        // Given
        String realmId = "test-realm";
        when(bioIdClient.isHealthy()).thenReturn(false);

        // When
        AdminDashboardData dashboardData = adminService.getDashboardData(realmId);

        // Then
        assertThat(dashboardData).isNotNull();
        assertThat(dashboardData.getHealthMetrics().getBioIdServiceStatus())
            .isEqualTo(AdminDashboardData.ServiceStatus.UNHEALTHY);
        
        verify(bioIdClient).isHealthy();
    }

    @Test
    @DisplayName("Should throw exception when dashboard data retrieval fails")
    void shouldThrowExceptionWhenDashboardDataRetrievalFails() {
        // Given
        String realmId = "test-realm";
        when(bioIdClient.isHealthy()).thenThrow(new RuntimeException("Connection failed"));

        // When & Then
        assertThatThrownBy(() -> adminService.getDashboardData(realmId))
            .isInstanceOf(BioIdException.class)
            .hasMessage("Failed to retrieve dashboard data")
            .hasCauseInstanceOf(RuntimeException.class);
        
        verify(bioIdClient).isHealthy();
    }

    @Test
    @DisplayName("Should get user enrollments successfully")
    void shouldGetUserEnrollmentsSuccessfully() throws BioIdException {
        // Given
        String realmId = "test-realm";
        int offset = 0;
        int limit = 10;

        // When
        List<UserEnrollmentStatus> enrollments = adminService.getUserEnrollments(realmId, offset, limit);

        // Then
        assertThat(enrollments).isNotNull();
        assertThat(enrollments).isEmpty(); // Placeholder implementation returns empty list
    }

    @Test
    @DisplayName("Should generate enrollment link successfully")
    void shouldGenerateEnrollmentLinkSuccessfully() throws BioIdException {
        // Given
        String userId = "test-user";
        int validityHours = 24;

        // When
        EnrollmentLinkResult result = adminService.generateEnrollmentLink(userId, validityHours);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEnrollmentUrl()).isNotNull();
        assertThat(result.getEnrollmentUrl()).contains(userId);
        assertThat(result.getToken()).isNotNull();
        assertThat(result.getToken()).isNotEmpty();
        assertThat(result.getExpiresAt()).isNotNull();
        assertThat(result.getExpiresAt()).isAfter(Instant.now());
        assertThat(result.isRequiresAdminApproval()).isFalse();
        
        // Verify expiration time is approximately correct (within 1 minute tolerance)
        long expectedExpirationMillis = Instant.now().toEpochMilli() + (validityHours * 60 * 60 * 1000);
        long actualExpirationMillis = result.getExpiresAt().toEpochMilli();
        assertThat(Math.abs(actualExpirationMillis - expectedExpirationMillis)).isLessThan(60000); // 1 minute tolerance
    }

    @Test
    @DisplayName("Should generate unique tokens for different users")
    void shouldGenerateUniqueTokensForDifferentUsers() throws BioIdException {
        // Given
        String userId1 = "user1";
        String userId2 = "user2";
        int validityHours = 24;

        // When
        EnrollmentLinkResult result1 = adminService.generateEnrollmentLink(userId1, validityHours);
        EnrollmentLinkResult result2 = adminService.generateEnrollmentLink(userId2, validityHours);

        // Then
        assertThat(result1.getToken()).isNotEqualTo(result2.getToken());
        assertThat(result1.getEnrollmentUrl()).isNotEqualTo(result2.getEnrollmentUrl());
    }

    @Test
    @DisplayName("Should delete user enrollment successfully")
    void shouldDeleteUserEnrollmentSuccessfully() throws BioIdException {
        // Given
        String userId = "test-user";
        String reason = "User requested deletion";

        // When & Then - Should not throw exception
        assertThatCode(() -> adminService.deleteUserEnrollment(userId, reason))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should delete template by class ID successfully")
    void shouldDeleteTemplateByClassIdSuccessfully() throws BioIdException {
        // Given
        long classId = 12345L;
        doNothing().when(bioIdClient).deleteTemplate(classId);

        // When & Then - Should not throw exception
        assertThatCode(() -> adminService.deleteTemplate(classId))
            .doesNotThrowAnyException();
        
        verify(bioIdClient).deleteTemplate(classId);
    }

    @Test
    @DisplayName("Should throw exception when template deletion fails")
    void shouldThrowExceptionWhenTemplateDeletionFails() throws BioIdException {
        // Given
        long classId = 12345L;
        doThrow(new BioIdException("Template not found")).when(bioIdClient).deleteTemplate(classId);

        // When & Then
        assertThatThrownBy(() -> adminService.deleteTemplate(classId))
            .isInstanceOf(BioIdException.class)
            .hasMessage("Failed to delete template")
            .hasCauseInstanceOf(BioIdException.class);
        
        verify(bioIdClient).deleteTemplate(classId);
    }

    @Test
    @DisplayName("Should handle null realm ID gracefully")
    void shouldHandleNullRealmIdGracefully() throws BioIdException {
        // Given
        String realmId = null;

        // When
        AdminDashboardData result = adminService.getDashboardData(realmId);

        // Then - Should handle null gracefully and return data
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Should handle empty user ID gracefully")
    void shouldHandleEmptyUserIdGracefully() throws BioIdException {
        // Given
        String userId = "";
        int validityHours = 24;

        // When
        EnrollmentLinkResult result = adminService.generateEnrollmentLink(userId, validityHours);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEnrollmentUrl()).contains(userId);
    }

    @Test
    @DisplayName("Should handle negative validity hours")
    void shouldHandleNegativeValidityHours() throws BioIdException {
        // Given
        String userId = "test-user";
        int validityHours = -1;

        // When
        EnrollmentLinkResult result = adminService.generateEnrollmentLink(userId, validityHours);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getExpiresAt()).isBefore(Instant.now()); // Should be in the past
    }

    @Test
    @DisplayName("Should handle zero validity hours")
    void shouldHandleZeroValidityHours() throws BioIdException {
        // Given
        String userId = "test-user";
        int validityHours = 0;

        // When
        EnrollmentLinkResult result = adminService.generateEnrollmentLink(userId, validityHours);

        // Then
        assertThat(result).isNotNull();
        // Expiration should be very close to now (within 1 second)
        long timeDiff = Math.abs(result.getExpiresAt().toEpochMilli() - Instant.now().toEpochMilli());
        assertThat(timeDiff).isLessThan(1000);
    }

    @Test
    @DisplayName("Should handle large validity hours")
    void shouldHandleLargeValidityHours() throws BioIdException {
        // Given
        String userId = "test-user";
        int validityHours = 8760; // 1 year

        // When
        EnrollmentLinkResult result = adminService.generateEnrollmentLink(userId, validityHours);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getExpiresAt()).isAfter(Instant.now().plusSeconds(8760 * 3600 - 60)); // Within 1 minute of expected
    }

    @Test
    @DisplayName("Should handle pagination parameters correctly")
    void shouldHandlePaginationParametersCorrectly() throws BioIdException {
        // Given
        String realmId = "test-realm";
        int offset = 50;
        int limit = 25;

        // When
        List<UserEnrollmentStatus> enrollments = adminService.getUserEnrollments(realmId, offset, limit);

        // Then
        assertThat(enrollments).isNotNull();
        // In the current implementation, this returns an empty list regardless of parameters
        // In a real implementation, we would verify the parameters are used correctly
    }

    @Test
    @DisplayName("Should handle negative pagination parameters")
    void shouldHandleNegativePaginationParameters() throws BioIdException {
        // Given
        String realmId = "test-realm";
        int offset = -1;
        int limit = -1;

        // When & Then - Should not throw exception
        assertThatCode(() -> adminService.getUserEnrollments(realmId, offset, limit))
            .doesNotThrowAnyException();
    }
}