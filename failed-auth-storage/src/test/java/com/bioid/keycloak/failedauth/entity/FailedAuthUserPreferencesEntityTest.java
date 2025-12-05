package com.bioid.keycloak.failedauth.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for FailedAuthUserPreferencesEntity.
 */
@DisplayName("FailedAuthUserPreferencesEntity Tests")
class FailedAuthUserPreferencesEntityTest {
    
    private FailedAuthUserPreferencesEntity preferences;
    
    @BeforeEach
    void setUp() {
        preferences = new FailedAuthUserPreferencesEntity("user-123", "realm-456");
    }
    
    @Test
    @DisplayName("Should create entity with required fields")
    void testCreateEntity() {
        // Then
        assertThat(preferences.getUserId()).isEqualTo("user-123");
        assertThat(preferences.getRealmId()).isEqualTo("realm-456");
    }
    
    @Test
    @DisplayName("Should have default values")
    void testDefaultValues() {
        // Then
        assertThat(preferences.getStorageEnabled()).isTrue();
        assertThat(preferences.getNotificationEnabled()).isTrue();
        assertThat(preferences.getNotificationThreshold()).isEqualTo(3);
        assertThat(preferences.getNotificationCooldownHours()).isEqualTo(24);
        assertThat(preferences.getPrivacyNoticeAccepted()).isFalse();
        assertThat(preferences.getTotalAttempts()).isEqualTo(0);
        assertThat(preferences.getEnrolledAttempts()).isEqualTo(0);
        assertThat(preferences.getDeletedAttempts()).isEqualTo(0);
    }
    
    @Test
    @DisplayName("Should increment total attempts")
    void testIncrementTotalAttempts() {
        // When
        preferences.incrementTotalAttempts();
        preferences.incrementTotalAttempts();
        
        // Then
        assertThat(preferences.getTotalAttempts()).isEqualTo(2);
    }
    
    @Test
    @DisplayName("Should increment enrolled attempts")
    void testIncrementEnrolledAttempts() {
        // When
        preferences.incrementEnrolledAttempts();
        preferences.incrementEnrolledAttempts();
        preferences.incrementEnrolledAttempts();
        
        // Then
        assertThat(preferences.getEnrolledAttempts()).isEqualTo(3);
    }
    
    @Test
    @DisplayName("Should increment deleted attempts")
    void testIncrementDeletedAttempts() {
        // When
        preferences.incrementDeletedAttempts();
        
        // Then
        assertThat(preferences.getDeletedAttempts()).isEqualTo(1);
    }
    
    @Test
    @DisplayName("Should handle null values in increment methods")
    void testIncrementFromNull() {
        // Given
        preferences.setTotalAttempts(null);
        preferences.setEnrolledAttempts(null);
        preferences.setDeletedAttempts(null);
        
        // When
        preferences.incrementTotalAttempts();
        preferences.incrementEnrolledAttempts();
        preferences.incrementDeletedAttempts();
        
        // Then
        assertThat(preferences.getTotalAttempts()).isEqualTo(1);
        assertThat(preferences.getEnrolledAttempts()).isEqualTo(1);
        assertThat(preferences.getDeletedAttempts()).isEqualTo(1);
    }
    
    @Test
    @DisplayName("Should notify when threshold reached and no cooldown")
    void testShouldNotifyThresholdReached() {
        // Given
        preferences.setNotificationEnabled(true);
        preferences.setNotificationThreshold(3);
        preferences.setTotalAttempts(5);
        preferences.setEnrolledAttempts(1);
        preferences.setDeletedAttempts(1);
        preferences.setLastNotificationSent(null);
        
        // When
        boolean shouldNotify = preferences.shouldNotify();
        
        // Then - 5 total - 1 enrolled - 1 deleted = 3 unenrolled (meets threshold)
        assertThat(shouldNotify).isTrue();
    }
    
    @Test
    @DisplayName("Should not notify when threshold not reached")
    void testShouldNotNotifyThresholdNotReached() {
        // Given
        preferences.setNotificationEnabled(true);
        preferences.setNotificationThreshold(5);
        preferences.setTotalAttempts(3);
        preferences.setEnrolledAttempts(0);
        preferences.setDeletedAttempts(0);
        
        // When
        boolean shouldNotify = preferences.shouldNotify();
        
        // Then - only 3 unenrolled, threshold is 5
        assertThat(shouldNotify).isFalse();
    }
    
    @Test
    @DisplayName("Should not notify when notifications disabled")
    void testShouldNotNotifyWhenDisabled() {
        // Given
        preferences.setNotificationEnabled(false);
        preferences.setTotalAttempts(10);
        
        // When
        boolean shouldNotify = preferences.shouldNotify();
        
        // Then
        assertThat(shouldNotify).isFalse();
    }
    
    @Test
    @DisplayName("Should not notify during cooldown period")
    void testShouldNotNotifyDuringCooldown() {
        // Given
        preferences.setNotificationEnabled(true);
        preferences.setNotificationThreshold(3);
        preferences.setNotificationCooldownHours(24);
        preferences.setTotalAttempts(5);
        preferences.setEnrolledAttempts(0);
        preferences.setDeletedAttempts(0);
        
        // Set last notification to 12 hours ago (within 24-hour cooldown)
        preferences.setLastNotificationSent(Instant.now().minus(12, ChronoUnit.HOURS));
        
        // When
        boolean shouldNotify = preferences.shouldNotify();
        
        // Then
        assertThat(shouldNotify).isFalse();
    }
    
    @Test
    @DisplayName("Should notify after cooldown period expired")
    void testShouldNotifyAfterCooldown() {
        // Given
        preferences.setNotificationEnabled(true);
        preferences.setNotificationThreshold(3);
        preferences.setNotificationCooldownHours(24);
        preferences.setTotalAttempts(5);
        preferences.setEnrolledAttempts(0);
        preferences.setDeletedAttempts(0);
        
        // Set last notification to 25 hours ago (cooldown expired)
        preferences.setLastNotificationSent(Instant.now().minus(25, ChronoUnit.HOURS));
        
        // When
        boolean shouldNotify = preferences.shouldNotify();
        
        // Then
        assertThat(shouldNotify).isTrue();
    }
    
    @Test
    @DisplayName("Should set and get all properties")
    void testSettersAndGetters() {
        // When
        preferences.setStorageEnabled(false);
        preferences.setNotificationEnabled(false);
        preferences.setNotificationThreshold(5);
        preferences.setNotificationCooldownHours(48);
        preferences.setPrivacyNoticeAccepted(true);
        
        Instant now = Instant.now();
        preferences.setPrivacyNoticeAcceptedAt(now);
        preferences.setLastNotificationSent(now);
        
        preferences.setTotalAttempts(10);
        preferences.setEnrolledAttempts(5);
        preferences.setDeletedAttempts(2);
        
        // Then
        assertThat(preferences.getStorageEnabled()).isFalse();
        assertThat(preferences.getNotificationEnabled()).isFalse();
        assertThat(preferences.getNotificationThreshold()).isEqualTo(5);
        assertThat(preferences.getNotificationCooldownHours()).isEqualTo(48);
        assertThat(preferences.getPrivacyNoticeAccepted()).isTrue();
        assertThat(preferences.getPrivacyNoticeAcceptedAt()).isEqualTo(now);
        assertThat(preferences.getLastNotificationSent()).isEqualTo(now);
        assertThat(preferences.getTotalAttempts()).isEqualTo(10);
        assertThat(preferences.getEnrolledAttempts()).isEqualTo(5);
        assertThat(preferences.getDeletedAttempts()).isEqualTo(2);
    }
    
    @Test
    @DisplayName("Should handle edge case: zero threshold")
    void testZeroThreshold() {
        // Given
        preferences.setNotificationEnabled(true);
        preferences.setNotificationThreshold(0);
        preferences.setTotalAttempts(0);
        
        // When
        boolean shouldNotify = preferences.shouldNotify();
        
        // Then - 0 unenrolled attempts, threshold is 0, should notify
        assertThat(shouldNotify).isTrue();
    }
    
    @Test
    @DisplayName("Should handle edge case: all attempts enrolled")
    void testAllAttemptsEnrolled() {
        // Given
        preferences.setNotificationEnabled(true);
        preferences.setNotificationThreshold(3);
        preferences.setTotalAttempts(5);
        preferences.setEnrolledAttempts(5);
        preferences.setDeletedAttempts(0);
        
        // When
        boolean shouldNotify = preferences.shouldNotify();
        
        // Then - 0 unenrolled attempts, should not notify
        assertThat(shouldNotify).isFalse();
    }
    
    @Test
    @DisplayName("Should handle edge case: all attempts deleted")
    void testAllAttemptsDeleted() {
        // Given
        preferences.setNotificationEnabled(true);
        preferences.setNotificationThreshold(3);
        preferences.setTotalAttempts(5);
        preferences.setEnrolledAttempts(0);
        preferences.setDeletedAttempts(5);
        
        // When
        boolean shouldNotify = preferences.shouldNotify();
        
        // Then - 0 unenrolled attempts, should not notify
        assertThat(shouldNotify).isFalse();
    }
}
