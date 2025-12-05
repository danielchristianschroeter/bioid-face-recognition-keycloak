package com.bioid.keycloak.failedauth.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for FailedAuthConfiguration.
 */
@DisplayName("FailedAuthConfiguration Tests")
class FailedAuthConfigurationTest {
    
    private FailedAuthConfiguration config;
    
    @BeforeEach
    void setUp() {
        // Clear any existing system properties
        clearSystemProperties();
        config = FailedAuthConfiguration.getInstance();
    }
    
    @AfterEach
    void tearDown() {
        clearSystemProperties();
    }
    
    private void clearSystemProperties() {
        System.clearProperty("failed.auth.storage.enabled");
        System.clearProperty("failed.auth.retention.days");
        System.clearProperty("failed.auth.min.quality.score");
    }
    
    @Test
    @DisplayName("Should return default values when no configuration is set")
    void testDefaultValues() {
        // Then
        assertThat(config.isStorageEnabled()).isTrue();
        assertThat(config.getRetentionDays()).isEqualTo(30);
        assertThat(config.getMaxAttemptsPerUser()).isEqualTo(20);
        assertThat(config.getMinQualityScore()).isEqualTo(0.65);
        assertThat(config.getMinEnrollQualityScore()).isEqualTo(0.70);
        assertThat(config.isEncryptImages()).isTrue();
        assertThat(config.isVerifyIntegrity()).isTrue();
        assertThat(config.isNotificationEnabled()).isTrue();
        assertThat(config.isAdminAccessEnabled()).isTrue();
    }
    
    @Test
    @DisplayName("Should read boolean property from system property")
    void testBooleanProperty() {
        // Given
        System.setProperty("failed.auth.storage.enabled", "false");
        FailedAuthConfiguration newConfig = FailedAuthConfiguration.getInstance();
        
        // Then
        assertThat(newConfig.isStorageEnabled()).isFalse();
    }
    
    @Test
    @DisplayName("Should read integer property from system property")
    void testIntegerProperty() {
        // Given
        System.setProperty("failed.auth.retention.days", "60");
        FailedAuthConfiguration newConfig = FailedAuthConfiguration.getInstance();
        
        // Then
        assertThat(newConfig.getRetentionDays()).isEqualTo(60);
    }
    
    @Test
    @DisplayName("Should read double property from system property")
    void testDoubleProperty() {
        // Given
        System.setProperty("failed.auth.min.quality.score", "0.80");
        FailedAuthConfiguration newConfig = FailedAuthConfiguration.getInstance();
        
        // Then
        assertThat(newConfig.getMinQualityScore()).isEqualTo(0.80);
    }
    
    @Test
    @DisplayName("Should read string property from system property")
    void testStringProperty() {
        // Given
        System.setProperty("failed.auth.notification.method", "sms");
        FailedAuthConfiguration newConfig = FailedAuthConfiguration.getInstance();
        
        // Then
        assertThat(newConfig.getNotificationMethod()).isEqualTo("sms");
    }
    
    @Test
    @DisplayName("Should use default value for invalid integer")
    void testInvalidIntegerProperty() {
        // Given
        System.setProperty("failed.auth.retention.days", "invalid");
        FailedAuthConfiguration newConfig = FailedAuthConfiguration.getInstance();
        
        // Then - should use default
        assertThat(newConfig.getRetentionDays()).isEqualTo(30);
    }
    
    @Test
    @DisplayName("Should use default value for invalid double")
    void testInvalidDoubleProperty() {
        // Given
        System.setProperty("failed.auth.min.quality.score", "not-a-number");
        FailedAuthConfiguration newConfig = FailedAuthConfiguration.getInstance();
        
        // Then - should use default
        assertThat(newConfig.getMinQualityScore()).isEqualTo(0.65);
    }
    
    @Test
    @DisplayName("Should return singleton instance")
    void testSingletonInstance() {
        // When
        FailedAuthConfiguration instance1 = FailedAuthConfiguration.getInstance();
        FailedAuthConfiguration instance2 = FailedAuthConfiguration.getInstance();
        
        // Then
        assertThat(instance1).isSameAs(instance2);
    }
    
    @Test
    @DisplayName("Should get all feature flags")
    void testFeatureFlags() {
        // Then
        assertThat(config.isStorageEnabled()).isNotNull();
        assertThat(config.isAdminAccessEnabled()).isNotNull();
        assertThat(config.isNotificationEnabled()).isNotNull();
        assertThat(config.isAutoCleanupEnabled()).isNotNull();
        assertThat(config.isEncryptImages()).isNotNull();
        assertThat(config.isVerifyIntegrity()).isNotNull();
    }
    
    @Test
    @DisplayName("Should get all retention settings")
    void testRetentionSettings() {
        // Then
        assertThat(config.getRetentionDays()).isPositive();
        assertThat(config.getMaxAttemptsPerUser()).isPositive();
        assertThat(config.getCleanupIntervalHours()).isPositive();
        assertThat(config.getCleanupBatchSize()).isPositive();
    }
    
    @Test
    @DisplayName("Should get all quality thresholds")
    void testQualityThresholds() {
        // Then
        assertThat(config.getMinQualityScore()).isBetween(0.0, 1.0);
        assertThat(config.getMinEnrollQualityScore()).isBetween(0.0, 1.0);
        assertThat(config.getMinEnrollQualityScore())
            .isGreaterThanOrEqualTo(config.getMinQualityScore());
    }
    
    @Test
    @DisplayName("Should get all image processing settings")
    void testImageProcessingSettings() {
        // Then
        assertThat(config.isIncludeThumbnails()).isNotNull();
        assertThat(config.getThumbnailSize()).isPositive();
        assertThat(config.getThumbnailQuality()).isBetween(1, 100);
        assertThat(config.getMaxImageSizeMB()).isPositive();
    }
    
    @Test
    @DisplayName("Should get all notification settings")
    void testNotificationSettings() {
        // Then
        assertThat(config.getNotificationThreshold()).isPositive();
        assertThat(config.getNotificationCooldownHours()).isPositive();
        assertThat(config.getNotificationMethod()).isNotEmpty();
    }
    
    @Test
    @DisplayName("Should get all rate limiting settings")
    void testRateLimitingSettings() {
        // Then
        assertThat(config.isApiRateLimitEnabled()).isNotNull();
        assertThat(config.getApiRateLimitPerMinute()).isPositive();
        assertThat(config.getApiEnrollRateLimitPerHour()).isPositive();
    }
    
    @Test
    @DisplayName("Should get all audit settings")
    void testAuditSettings() {
        // Then
        assertThat(config.isAuditEnabled()).isNotNull();
        assertThat(config.getAuditLogLevel()).isNotEmpty();
    }
    
    @Test
    @DisplayName("Should get all enrollment settings")
    void testEnrollmentSettings() {
        // Then
        assertThat(config.isEnrollVerifyBeforeEnroll()).isNotNull();
        assertThat(config.getEnrollMaxImagesPerRequest()).isPositive();
    }
    
    @Test
    @DisplayName("Should handle boolean true variations")
    void testBooleanTrueVariations() {
        // Test various true values
        String[] trueValues = {"true", "TRUE", "True", "1", "yes", "YES"};
        
        for (String value : trueValues) {
            System.setProperty("failed.auth.storage.enabled", value);
            FailedAuthConfiguration testConfig = FailedAuthConfiguration.getInstance();
            
            boolean result = testConfig.isStorageEnabled();
            // Only "true" (case-insensitive) should be true in Java's Boolean.parseBoolean
            if (value.equalsIgnoreCase("true")) {
                assertThat(result).isTrue();
            }
        }
    }
    
    @Test
    @DisplayName("Should handle boolean false variations")
    void testBooleanFalseVariations() {
        // Test various false values
        String[] falseValues = {"false", "FALSE", "False", "0", "no", "NO"};
        
        for (String value : falseValues) {
            System.setProperty("failed.auth.storage.enabled", value);
            FailedAuthConfiguration testConfig = FailedAuthConfiguration.getInstance();
            
            // All non-"true" values should be false
            assertThat(testConfig.isStorageEnabled()).isFalse();
        }
    }
    
    @Test
    @DisplayName("Should log configuration without errors")
    void testLogConfiguration() {
        // When/Then - should not throw exception
        assertThatCode(() -> config.logConfiguration())
            .doesNotThrowAnyException();
    }
    
    @Test
    @DisplayName("Should handle edge case values")
    void testEdgeCaseValues() {
        // Given
        System.setProperty("failed.auth.retention.days", "0");
        System.setProperty("failed.auth.min.quality.score", "0.0");
        System.setProperty("failed.auth.thumbnail.quality", "100");
        
        FailedAuthConfiguration testConfig = FailedAuthConfiguration.getInstance();
        
        // Then
        assertThat(testConfig.getRetentionDays()).isEqualTo(0);
        assertThat(testConfig.getMinQualityScore()).isEqualTo(0.0);
        assertThat(testConfig.getThumbnailQuality()).isEqualTo(100);
    }
    
    @Test
    @DisplayName("Should handle negative values gracefully")
    void testNegativeValues() {
        // Given
        System.setProperty("failed.auth.retention.days", "-10");
        
        FailedAuthConfiguration testConfig = FailedAuthConfiguration.getInstance();
        
        // Then - should accept the value (validation should be done elsewhere)
        assertThat(testConfig.getRetentionDays()).isEqualTo(-10);
    }
}
