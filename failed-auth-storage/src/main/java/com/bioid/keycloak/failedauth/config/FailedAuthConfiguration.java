package com.bioid.keycloak.failedauth.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuration manager for Failed Authentication Image Storage feature.
 * 
 * Loads configuration from environment variables and system properties.
 */
public class FailedAuthConfiguration {
    
    private static final Logger logger = LoggerFactory.getLogger(FailedAuthConfiguration.class);
    private static final String PREFIX = "FAILED_AUTH_";
    
    private static volatile FailedAuthConfiguration instance;
    
    private FailedAuthConfiguration() {
        logger.info("Initializing Failed Authentication Storage Configuration");
    }
    
    public static FailedAuthConfiguration getInstance() {
        if (instance == null) {
            synchronized (FailedAuthConfiguration.class) {
                if (instance == null) {
                    instance = new FailedAuthConfiguration();
                }
            }
        }
        return instance;
    }
    
    // Feature flags
    public boolean isStorageEnabled() {
        return getBooleanProperty("STORAGE_ENABLED", true);
    }
    
    public boolean isAdminAccessEnabled() {
        return getBooleanProperty("ADMIN_ACCESS_ENABLED", true);
    }
    
    public boolean isNotificationEnabled() {
        return getBooleanProperty("NOTIFICATION_ENABLED", true);
    }
    
    // Retention policy
    public int getRetentionDays() {
        return getIntProperty("RETENTION_DAYS", 30);
    }
    
    public int getMaxAttemptsPerUser() {
        return getIntProperty("MAX_ATTEMPTS_PER_USER", 20);
    }
    
    public boolean isAutoCleanupEnabled() {
        return getBooleanProperty("AUTO_CLEANUP_ENABLED", true);
    }
    
    // Quality thresholds
    public double getMinQualityScore() {
        return getDoubleProperty("MIN_QUALITY_SCORE", 0.65);
    }
    
    public double getMinEnrollQualityScore() {
        return getDoubleProperty("MIN_ENROLL_QUALITY_SCORE", 0.70);
    }
    
    public boolean isRequireLivenessPass() {
        return getBooleanProperty("REQUIRE_LIVENESS_PASS", false);
    }
    
    // Image processing
    public boolean isIncludeThumbnails() {
        return getBooleanProperty("INCLUDE_THUMBNAILS", true);
    }
    
    public int getThumbnailSize() {
        return getIntProperty("THUMBNAIL_SIZE", 300);
    }
    
    public int getThumbnailQuality() {
        return getIntProperty("THUMBNAIL_QUALITY", 85);
    }
    
    public int getMaxImageSizeMB() {
        return getIntProperty("MAX_IMAGE_SIZE_MB", 5);
    }
    
    // Security
    public boolean isEncryptImages() {
        return getBooleanProperty("ENCRYPT_IMAGES", true);
    }
    
    public boolean isVerifyIntegrity() {
        return getBooleanProperty("VERIFY_INTEGRITY", true);
    }
    
    // Cleanup
    public int getCleanupIntervalHours() {
        return getIntProperty("CLEANUP_INTERVAL_HOURS", 24);
    }
    
    public int getCleanupBatchSize() {
        return getIntProperty("CLEANUP_BATCH_SIZE", 100);
    }
    
    // Enrollment
    public boolean isEnrollVerifyBeforeEnroll() {
        return getBooleanProperty("ENROLL_VERIFY_BEFORE_ENROLL", true);
    }
    
    public int getEnrollMaxImagesPerRequest() {
        return getIntProperty("ENROLL_MAX_IMAGES_PER_REQUEST", 10);
    }
    
    // Notifications
    public int getNotificationThreshold() {
        return getIntProperty("NOTIFICATION_THRESHOLD", 3);
    }
    
    public int getNotificationCooldownHours() {
        return getIntProperty("NOTIFICATION_COOLDOWN_HOURS", 24);
    }
    
    public String getNotificationMethod() {
        return getStringProperty("NOTIFICATION_METHOD", "email");
    }
    
    // Rate limiting
    public boolean isApiRateLimitEnabled() {
        return getBooleanProperty("API_RATE_LIMIT_ENABLED", true);
    }
    
    public int getApiRateLimitPerMinute() {
        return getIntProperty("API_RATE_LIMIT_PER_MINUTE", 30);
    }
    
    public int getApiEnrollRateLimitPerHour() {
        return getIntProperty("API_ENROLL_RATE_LIMIT_PER_HOUR", 10);
    }
    
    // Audit
    public boolean isAuditEnabled() {
        return getBooleanProperty("AUDIT_ENABLED", true);
    }
    
    public String getAuditLogLevel() {
        return getStringProperty("AUDIT_LOG_LEVEL", "INFO");
    }
    
    // Helper methods
    private boolean getBooleanProperty(String key, boolean defaultValue) {
        String envKey = PREFIX + key;
        String value = System.getenv(envKey);
        if (value == null) {
            value = System.getProperty(envKey.toLowerCase().replace('_', '.'));
        }
        return value != null ? Boolean.parseBoolean(value) : defaultValue;
    }
    
    private int getIntProperty(String key, int defaultValue) {
        String envKey = PREFIX + key;
        String value = System.getenv(envKey);
        if (value == null) {
            value = System.getProperty(envKey.toLowerCase().replace('_', '.'));
        }
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                logger.warn("Invalid integer value for {}: {}, using default: {}", 
                    envKey, value, defaultValue);
            }
        }
        return defaultValue;
    }
    
    private double getDoubleProperty(String key, double defaultValue) {
        String envKey = PREFIX + key;
        String value = System.getenv(envKey);
        if (value == null) {
            value = System.getProperty(envKey.toLowerCase().replace('_', '.'));
        }
        if (value != null) {
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException e) {
                logger.warn("Invalid double value for {}: {}, using default: {}", 
                    envKey, value, defaultValue);
            }
        }
        return defaultValue;
    }
    
    private String getStringProperty(String key, String defaultValue) {
        String envKey = PREFIX + key;
        String value = System.getenv(envKey);
        if (value == null) {
            value = System.getProperty(envKey.toLowerCase().replace('_', '.'));
        }
        return value != null ? value : defaultValue;
    }
    
    /**
     * Log current configuration (for debugging).
     */
    public void logConfiguration() {
        if (logger.isDebugEnabled()) {
            logger.debug("Failed Auth Storage Configuration:");
            logger.debug("  Storage Enabled: {}", isStorageEnabled());
            logger.debug("  Admin Access: {}", isAdminAccessEnabled());
            logger.debug("  Retention Days: {}", getRetentionDays());
            logger.debug("  Max Attempts Per User: {}", getMaxAttemptsPerUser());
            logger.debug("  Min Quality Score: {}", getMinQualityScore());
            logger.debug("  Encrypt Images: {}", isEncryptImages());
            logger.debug("  Notification Enabled: {}", isNotificationEnabled());
            logger.debug("  Audit Enabled: {}", isAuditEnabled());
        }
    }
}
