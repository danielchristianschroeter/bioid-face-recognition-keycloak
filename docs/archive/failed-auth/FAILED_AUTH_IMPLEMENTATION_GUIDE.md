# Failed Authentication Image Storage - Implementation Guide

## Overview

This guide provides step-by-step instructions for implementing the Failed Authentication Image Storage feature with database-backed storage for cluster/failover support.

## Architecture Decisions ✅

Based on requirements, the following decisions have been made:

1. **Storage**: PostgreSQL database (for cluster/failover support)
2. **Notifications**: Email (configurable per user and admin)
3. **Default State**: Enabled by default (users can opt-out)
4. **Admin Access**: Enabled (with full audit logging)
5. **Retention**: 30 days default (configurable 7-90 days)

## Phase 1: Database Schema & Core Services

### Step 1.1: Create Database Migration

Create Liquibase changelog: `src/main/resources/db/changelog/failed-auth-storage.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
    http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">

    <changeSet id="1-create-failed-auth-attempts" author="bioid">
        <createTable tableName="failed_auth_attempts">
            <column name="attempt_id" type="VARCHAR(36)">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="user_id" type="VARCHAR(36)">
                <constraints nullable="false"/>
            </column>
            <column name="realm_id" type="VARCHAR(36)">
                <constraints nullable="false"/>
            </column>
            <column name="username" type="VARCHAR(255)">
                <constraints nullable="false"/>
            </column>
            <column name="class_id" type="BIGINT">
                <constraints nullable="false"/>
            </column>
            
            <!-- Timestamps -->
            <column name="timestamp" type="TIMESTAMP">
                <constraints nullable="false"/>
            </column>
            <column name="created_at" type="TIMESTAMP" defaultValueComputed="CURRENT_TIMESTAMP"/>
            <column name="updated_at" type="TIMESTAMP" defaultValueComputed="CURRENT_TIMESTAMP"/>
            <column name="expires_at" type="TIMESTAMP">
                <constraints nullable="false"/>
            </column>
            
            <!-- Failure details -->
            <column name="failure_reason" type="VARCHAR(50)">
                <constraints nullable="false"/>
            </column>
            <column name="verification_score" type="DECIMAL(5,4)"/>
            <column name="verification_threshold" type="DECIMAL(5,4)"/>
            <column name="score_difference" type="DECIMAL(5,4)"/>
            
            <!-- Liveness -->
            <column name="liveness_mode" type="VARCHAR(20)"/>
            <column name="liveness_score" type="DECIMAL(5,4)"/>
            <column name="liveness_threshold" type="DECIMAL(5,4)"/>
            <column name="liveness_passed" type="BOOLEAN"/>
            <column name="challenge_direction" type="VARCHAR(10)"/>
            
            <!-- Retry info -->
            <column name="retry_attempt" type="INT" defaultValue="1"/>
            <column name="max_retries" type="INT" defaultValue="3"/>
            
            <!-- Image info -->
            <column name="image_count" type="INT">
                <constraints nullable="false"/>
            </column>
            <column name="avg_quality_score" type="DECIMAL(5,4)"/>
            
            <!-- Enrollment status -->
            <column name="enrolled" type="BOOLEAN" defaultValueBoolean="false"/>
            <column name="enrolled_at" type="TIMESTAMP"/>
            <column name="enrolled_by" type="VARCHAR(36)"/>
            <column name="enrolled_image_indices" type="TEXT"/>
            <column name="enrollment_result" type="TEXT"/>
            
            <!-- Review status -->
            <column name="reviewed" type="BOOLEAN" defaultValueBoolean="false"/>
            <column name="reviewed_at" type="TIMESTAMP"/>
            <column name="reviewed_by" type="VARCHAR(36)"/>
            <column name="review_notes" type="TEXT"/>
            
            <!-- Session info -->
            <column name="session_id" type="VARCHAR(255)"/>
            <column name="ip_address" type="VARCHAR(45)"/>
            <column name="user_agent" type="TEXT"/>
            
            <!-- Security -->
            <column name="encrypted" type="BOOLEAN" defaultValueBoolean="true"/>
            <column name="checksum_sha256" type="VARCHAR(64)"/>
            <column name="integrity_verified" type="BOOLEAN" defaultValueBoolean="true"/>
            
            <!-- Metadata -->
            <column name="metadata_json" type="TEXT"/>
        </createTable>
        
        <createIndex tableName="failed_auth_attempts" indexName="idx_user_timestamp">
            <column name="user_id"/>
            <column name="timestamp" descending="true"/>
        </createIndex>
        
        <createIndex tableName="failed_auth_attempts" indexName="idx_realm_user">
            <column name="realm_id"/>
            <column name="user_id"/>
        </createIndex>
        
        <createIndex tableName="failed_auth_attempts" indexName="idx_enrolled">
            <column name="user_id"/>
            <column name="enrolled"/>
        </createIndex>
        
        <createIndex tableName="failed_auth_attempts" indexName="idx_expires">
            <column name="expires_at"/>
        </createIndex>
    </changeSet>

    <changeSet id="2-create-failed-auth-images" author="bioid">
        <createTable tableName="failed_auth_images">
            <column name="image_id" type="BIGSERIAL">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="attempt_id" type="VARCHAR(36)">
                <constraints nullable="false"/>
            </column>
            <column name="image_index" type="INT">
                <constraints nullable="false"/>
            </column>
            
            <!-- Image data -->
            <column name="image_data" type="BYTEA">
                <constraints nullable="false"/>
            </column>
            <column name="thumbnail_data" type="BYTEA"/>
            
            <!-- Image properties -->
            <column name="file_size" type="INT">
                <constraints nullable="false"/>
            </column>
            <column name="width" type="INT"/>
            <column name="height" type="INT"/>
            <column name="format" type="VARCHAR(10)"/>
            <column name="capture_timestamp" type="TIMESTAMP"/>
            
            <!-- Face detection -->
            <column name="face_found" type="BOOLEAN"/>
            <column name="face_count" type="INT"/>
            <column name="face_quality" type="DECIMAL(5,4)"/>
            <column name="eyes_visible" type="BOOLEAN"/>
            <column name="mouth_visible" type="BOOLEAN"/>
            
            <!-- Face angles -->
            <column name="face_yaw" type="DECIMAL(5,2)"/>
            <column name="face_pitch" type="DECIMAL(5,2)"/>
            <column name="face_roll" type="DECIMAL(5,2)"/>
            
            <!-- Quality -->
            <column name="quality_assessments" type="TEXT"/>
            <column name="quality_score" type="DECIMAL(5,4)"/>
            <column name="recommended_for_enrollment" type="BOOLEAN"/>
            <column name="feature_vectors_extracted" type="INT"/>
            
            <column name="created_at" type="TIMESTAMP" defaultValueComputed="CURRENT_TIMESTAMP"/>
        </createTable>
        
        <createIndex tableName="failed_auth_images" indexName="idx_attempt_id">
            <column name="attempt_id"/>
        </createIndex>
        
        <createIndex tableName="failed_auth_images" indexName="idx_attempt_image" unique="true">
            <column name="attempt_id"/>
            <column name="image_index"/>
        </createIndex>
        
        <addForeignKeyConstraint
            baseTableName="failed_auth_images"
            baseColumnNames="attempt_id"
            constraintName="fk_attempt"
            referencedTableName="failed_auth_attempts"
            referencedColumnNames="attempt_id"
            onDelete="CASCADE"/>
    </changeSet>

    <changeSet id="3-create-failed-auth-audit-log" author="bioid">
        <createTable tableName="failed_auth_audit_log">
            <column name="log_id" type="BIGSERIAL">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="attempt_id" type="VARCHAR(36)">
                <constraints nullable="false"/>
            </column>
            <column name="user_id" type="VARCHAR(36)">
                <constraints nullable="false"/>
            </column>
            <column name="action" type="VARCHAR(50)">
                <constraints nullable="false"/>
            </column>
            <column name="performed_by" type="VARCHAR(36)">
                <constraints nullable="false"/>
            </column>
            <column name="performed_at" type="TIMESTAMP" defaultValueComputed="CURRENT_TIMESTAMP"/>
            <column name="ip_address" type="VARCHAR(45)"/>
            <column name="user_agent" type="TEXT"/>
            <column name="details" type="TEXT"/>
        </createTable>
        
        <createIndex tableName="failed_auth_audit_log" indexName="idx_audit_attempt">
            <column name="attempt_id"/>
        </createIndex>
        
        <createIndex tableName="failed_auth_audit_log" indexName="idx_audit_user">
            <column name="user_id"/>
        </createIndex>
        
        <createIndex tableName="failed_auth_audit_log" indexName="idx_audit_performed_at">
            <column name="performed_at" descending="true"/>
        </createIndex>
    </changeSet>

    <changeSet id="4-create-failed-auth-user-preferences" author="bioid">
        <createTable tableName="failed_auth_user_preferences">
            <column name="user_id" type="VARCHAR(36)">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="realm_id" type="VARCHAR(36)">
                <constraints nullable="false"/>
            </column>
            
            <!-- Preferences -->
            <column name="storage_enabled" type="BOOLEAN" defaultValueBoolean="true"/>
            <column name="notification_enabled" type="BOOLEAN" defaultValueBoolean="true"/>
            <column name="notification_threshold" type="INT" defaultValue="3"/>
            <column name="notification_cooldown_hours" type="INT" defaultValue="24"/>
            <column name="last_notification_sent" type="TIMESTAMP"/>
            
            <!-- Privacy -->
            <column name="privacy_notice_accepted" type="BOOLEAN" defaultValueBoolean="false"/>
            <column name="privacy_notice_accepted_at" type="TIMESTAMP"/>
            
            <!-- Statistics -->
            <column name="total_attempts" type="INT" defaultValue="0"/>
            <column name="enrolled_attempts" type="INT" defaultValue="0"/>
            <column name="deleted_attempts" type="INT" defaultValue="0"/>
            
            <column name="created_at" type="TIMESTAMP" defaultValueComputed="CURRENT_TIMESTAMP"/>
            <column name="updated_at" type="TIMESTAMP" defaultValueComputed="CURRENT_TIMESTAMP"/>
        </createTable>
        
        <createIndex tableName="failed_auth_user_preferences" indexName="idx_prefs_realm">
            <column name="realm_id"/>
        </createIndex>
    </changeSet>

    <changeSet id="5-create-failed-auth-realm-config" author="bioid">
        <createTable tableName="failed_auth_realm_config">
            <column name="realm_id" type="VARCHAR(36)">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            
            <!-- Feature flags -->
            <column name="enabled" type="BOOLEAN" defaultValueBoolean="true"/>
            <column name="admin_access_enabled" type="BOOLEAN" defaultValueBoolean="true"/>
            <column name="notification_enabled" type="BOOLEAN" defaultValueBoolean="true"/>
            
            <!-- Retention -->
            <column name="retention_days" type="INT" defaultValue="30"/>
            <column name="max_attempts_per_user" type="INT" defaultValue="20"/>
            <column name="auto_cleanup_enabled" type="BOOLEAN" defaultValueBoolean="true"/>
            
            <!-- Quality thresholds -->
            <column name="min_quality_score" type="DECIMAL(5,4)" defaultValue="0.65"/>
            <column name="min_enroll_quality_score" type="DECIMAL(5,4)" defaultValue="0.70"/>
            
            <!-- Rate limits -->
            <column name="api_rate_limit_per_minute" type="INT" defaultValue="30"/>
            <column name="enroll_rate_limit_per_hour" type="INT" defaultValue="10"/>
            
            <!-- Notifications -->
            <column name="notification_threshold" type="INT" defaultValue="3"/>
            <column name="notification_cooldown_hours" type="INT" defaultValue="24"/>
            <column name="notification_from_email" type="VARCHAR(255)"/>
            <column name="notification_template" type="TEXT"/>
            
            <column name="created_at" type="TIMESTAMP" defaultValueComputed="CURRENT_TIMESTAMP"/>
            <column name="updated_at" type="TIMESTAMP" defaultValueComputed="CURRENT_TIMESTAMP"/>
        </createTable>
    </changeSet>

</databaseChangeLog>
```

### Step 1.2: Create Configuration Class

Create `FailedAuthConfiguration.java`:

```java
package com.bioid.keycloak.failedauth.config;

import com.bioid.keycloak.client.config.BioIdConfiguration;

public class FailedAuthConfiguration {
    
    private static final String PREFIX = "failed.auth.";
    
    // Feature flags
    public static final String STORAGE_ENABLED = PREFIX + "storage.enabled";
    public static final String ADMIN_ACCESS_ENABLED = PREFIX + "admin.access.enabled";
    public static final String NOTIFICATION_ENABLED = PREFIX + "notification.enabled";
    
    // Retention
    public static final String RETENTION_DAYS = PREFIX + "retention.days";
    public static final String MAX_ATTEMPTS_PER_USER = PREFIX + "max.attempts.per.user";
    public static final String AUTO_CLEANUP_ENABLED = PREFIX + "auto.cleanup.enabled";
    
    // Quality thresholds
    public static final String MIN_QUALITY_SCORE = PREFIX + "min.quality.score";
    public static final String MIN_ENROLL_QUALITY_SCORE = PREFIX + "min.enroll.quality.score";
    public static final String REQUIRE_LIVENESS_PASS = PREFIX + "require.liveness.pass";
    
    // Image processing
    public static final String INCLUDE_THUMBNAILS = PREFIX + "include.thumbnails";
    public static final String THUMBNAIL_SIZE = PREFIX + "thumbnail.size";
    public static final String THUMBNAIL_QUALITY = PREFIX + "thumbnail.quality";
    public static final String MAX_IMAGE_SIZE_MB = PREFIX + "max.image.size.mb";
    
    // Security
    public static final String ENCRYPT_IMAGES = PREFIX + "encrypt.images";
    public static final String VERIFY_INTEGRITY = PREFIX + "verify.integrity";
    
    // Cleanup
    public static final String CLEANUP_INTERVAL_HOURS = PREFIX + "cleanup.interval.hours";
    public static final String CLEANUP_BATCH_SIZE = PREFIX + "cleanup.batch.size";
    
    // Enrollment
    public static final String ENROLL_VERIFY_BEFORE_ENROLL = PREFIX + "enroll.verify.before.enroll";
    public static final String ENROLL_MAX_IMAGES_PER_REQUEST = PREFIX + "enroll.max.images.per.request";
    
    // Notifications
    public static final String NOTIFICATION_THRESHOLD = PREFIX + "notification.threshold";
    public static final String NOTIFICATION_COOLDOWN_HOURS = PREFIX + "notification.cooldown.hours";
    public static final String NOTIFICATION_METHOD = PREFIX + "notification.method";
    
    // Rate limiting
    public static final String API_RATE_LIMIT_ENABLED = PREFIX + "api.rate.limit.enabled";
    public static final String API_RATE_LIMIT_PER_MINUTE = PREFIX + "api.rate.limit.per.minute";
    public static final String API_ENROLL_RATE_LIMIT_PER_HOUR = PREFIX + "api.enroll.rate.limit.per.hour";
    
    // Audit
    public static final String AUDIT_ENABLED = PREFIX + "audit.enabled";
    public static final String AUDIT_LOG_LEVEL = PREFIX + "audit.log.level";
    
    private final BioIdConfiguration bioIdConfig;
    
    public FailedAuthConfiguration() {
        this.bioIdConfig = BioIdConfiguration.getInstance();
    }
    
    public boolean isStorageEnabled() {
        return getBooleanProperty(STORAGE_ENABLED, true);
    }
    
    public boolean isAdminAccessEnabled() {
        return getBooleanProperty(ADMIN_ACCESS_ENABLED, true);
    }
    
    public boolean isNotificationEnabled() {
        return getBooleanProperty(NOTIFICATION_ENABLED, true);
    }
    
    public int getRetentionDays() {
        return getIntProperty(RETENTION_DAYS, 30);
    }
    
    public int getMaxAttemptsPerUser() {
        return getIntProperty(MAX_ATTEMPTS_PER_USER, 20);
    }
    
    public boolean isAutoCleanupEnabled() {
        return getBooleanProperty(AUTO_CLEANUP_ENABLED, true);
    }
    
    public double getMinQualityScore() {
        return getDoubleProperty(MIN_QUALITY_SCORE, 0.65);
    }
    
    public double getMinEnrollQualityScore() {
        return getDoubleProperty(MIN_ENROLL_QUALITY_SCORE, 0.70);
    }
    
    public boolean isRequireLivenessPass() {
        return getBooleanProperty(REQUIRE_LIVENESS_PASS, false);
    }
    
    public boolean isIncludeThumbnails() {
        return getBooleanProperty(INCLUDE_THUMBNAILS, true);
    }
    
    public int getThumbnailSize() {
        return getIntProperty(THUMBNAIL_SIZE, 300);
    }
    
    public int getThumbnailQuality() {
        return getIntProperty(THUMBNAIL_QUALITY, 85);
    }
    
    public int getMaxImageSizeMB() {
        return getIntProperty(MAX_IMAGE_SIZE_MB, 5);
    }
    
    public boolean isEncryptImages() {
        return getBooleanProperty(ENCRYPT_IMAGES, true);
    }
    
    public boolean isVerifyIntegrity() {
        return getBooleanProperty(VERIFY_INTEGRITY, true);
    }
    
    public int getCleanupIntervalHours() {
        return getIntProperty(CLEANUP_INTERVAL_HOURS, 24);
    }
    
    public int getCleanupBatchSize() {
        return getIntProperty(CLEANUP_BATCH_SIZE, 100);
    }
    
    public boolean isEnrollVerifyBeforeEnroll() {
        return getBooleanProperty(ENROLL_VERIFY_BEFORE_ENROLL, true);
    }
    
    public int getEnrollMaxImagesPerRequest() {
        return getIntProperty(ENROLL_MAX_IMAGES_PER_REQUEST, 10);
    }
    
    public int getNotificationThreshold() {
        return getIntProperty(NOTIFICATION_THRESHOLD, 3);
    }
    
    public int getNotificationCooldownHours() {
        return getIntProperty(NOTIFICATION_COOLDOWN_HOURS, 24);
    }
    
    public String getNotificationMethod() {
        return getStringProperty(NOTIFICATION_METHOD, "email");
    }
    
    public boolean isApiRateLimitEnabled() {
        return getBooleanProperty(API_RATE_LIMIT_ENABLED, true);
    }
    
    public int getApiRateLimitPerMinute() {
        return getIntProperty(API_RATE_LIMIT_PER_MINUTE, 30);
    }
    
    public int getApiEnrollRateLimitPerHour() {
        return getIntProperty(API_ENROLL_RATE_LIMIT_PER_HOUR, 10);
    }
    
    public boolean isAuditEnabled() {
        return getBooleanProperty(AUDIT_ENABLED, true);
    }
    
    public String getAuditLogLevel() {
        return getStringProperty(AUDIT_LOG_LEVEL, "INFO");
    }
    
    // Helper methods
    private boolean getBooleanProperty(String key, boolean defaultValue) {
        return bioIdConfig.getBooleanProperty(key, defaultValue);
    }
    
    private int getIntProperty(String key, int defaultValue) {
        return bioIdConfig.getIntProperty(key, defaultValue);
    }
    
    private double getDoubleProperty(String key, double defaultValue) {
        return bioIdConfig.getDoubleProperty(key, defaultValue);
    }
    
    private String getStringProperty(String key, String defaultValue) {
        return bioIdConfig.getStringProperty(key, defaultValue);
    }
}
```

## Next Steps

1. **Review database schema** - Ensure it meets all requirements
2. **Create JPA entities** - Map tables to Java classes
3. **Implement FailedAuthImageStorageService** - Core business logic
4. **Modify FaceAuthenticator** - Capture and store failures
5. **Create REST API endpoints** - User and admin access
6. **Build UI components** - Account console pages
7. **Add email notifications** - User alerts
8. **Implement cleanup job** - Scheduled retention enforcement

Would you like me to proceed with implementing the JPA entities and core service?
