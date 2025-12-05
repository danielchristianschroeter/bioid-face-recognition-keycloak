package com.bioid.keycloak.failedauth.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * JPA Entity for realm-level configuration of failed authentication storage.
 */
@Entity
@Table(name = "failed_auth_realm_config")
public class FailedAuthRealmConfigEntity {
    
    @Id
    @Column(name = "realm_id", length = 36, nullable = false)
    private String realmId;
    
    // Feature flags
    @Column(name = "enabled")
    private Boolean enabled = true;
    
    @Column(name = "admin_access_enabled")
    private Boolean adminAccessEnabled = true;
    
    @Column(name = "notification_enabled")
    private Boolean notificationEnabled = true;
    
    // Retention policy
    @Column(name = "retention_days")
    private Integer retentionDays = 30;
    
    @Column(name = "max_attempts_per_user")
    private Integer maxAttemptsPerUser = 20;
    
    @Column(name = "auto_cleanup_enabled")
    private Boolean autoCleanupEnabled = true;
    
    // Quality thresholds
    @Column(name = "min_quality_score")
    private Double minQualityScore = 0.65;
    
    @Column(name = "min_enroll_quality_score")
    private Double minEnrollQualityScore = 0.70;
    
    // Rate limits
    @Column(name = "api_rate_limit_per_minute")
    private Integer apiRateLimitPerMinute = 30;
    
    @Column(name = "enroll_rate_limit_per_hour")
    private Integer enrollRateLimitPerHour = 10;
    
    // Notification settings
    @Column(name = "notification_threshold")
    private Integer notificationThreshold = 3;
    
    @Column(name = "notification_cooldown_hours")
    private Integer notificationCooldownHours = 24;
    
    @Column(name = "notification_from_email", length = 255)
    private String notificationFromEmail;
    
    @Column(name = "notification_template", columnDefinition = "TEXT")
    private String notificationTemplate;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at")
    private Instant updatedAt;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
    
    // Constructors
    public FailedAuthRealmConfigEntity() {
    }
    
    public FailedAuthRealmConfigEntity(String realmId) {
        this.realmId = realmId;
    }
    
    // Getters and Setters
    public String getRealmId() {
        return realmId;
    }
    
    public void setRealmId(String realmId) {
        this.realmId = realmId;
    }
    
    public Boolean getEnabled() {
        return enabled;
    }
    
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
    
    public Boolean getAdminAccessEnabled() {
        return adminAccessEnabled;
    }
    
    public void setAdminAccessEnabled(Boolean adminAccessEnabled) {
        this.adminAccessEnabled = adminAccessEnabled;
    }
    
    public Boolean getNotificationEnabled() {
        return notificationEnabled;
    }
    
    public void setNotificationEnabled(Boolean notificationEnabled) {
        this.notificationEnabled = notificationEnabled;
    }
    
    public Integer getRetentionDays() {
        return retentionDays;
    }
    
    public void setRetentionDays(Integer retentionDays) {
        this.retentionDays = retentionDays;
    }
    
    public Integer getMaxAttemptsPerUser() {
        return maxAttemptsPerUser;
    }
    
    public void setMaxAttemptsPerUser(Integer maxAttemptsPerUser) {
        this.maxAttemptsPerUser = maxAttemptsPerUser;
    }
    
    public Boolean getAutoCleanupEnabled() {
        return autoCleanupEnabled;
    }
    
    public void setAutoCleanupEnabled(Boolean autoCleanupEnabled) {
        this.autoCleanupEnabled = autoCleanupEnabled;
    }
    
    public Double getMinQualityScore() {
        return minQualityScore;
    }
    
    public void setMinQualityScore(Double minQualityScore) {
        this.minQualityScore = minQualityScore;
    }
    
    public Double getMinEnrollQualityScore() {
        return minEnrollQualityScore;
    }
    
    public void setMinEnrollQualityScore(Double minEnrollQualityScore) {
        this.minEnrollQualityScore = minEnrollQualityScore;
    }
    
    public Integer getApiRateLimitPerMinute() {
        return apiRateLimitPerMinute;
    }
    
    public void setApiRateLimitPerMinute(Integer apiRateLimitPerMinute) {
        this.apiRateLimitPerMinute = apiRateLimitPerMinute;
    }
    
    public Integer getEnrollRateLimitPerHour() {
        return enrollRateLimitPerHour;
    }
    
    public void setEnrollRateLimitPerHour(Integer enrollRateLimitPerHour) {
        this.enrollRateLimitPerHour = enrollRateLimitPerHour;
    }
    
    public Integer getNotificationThreshold() {
        return notificationThreshold;
    }
    
    public void setNotificationThreshold(Integer notificationThreshold) {
        this.notificationThreshold = notificationThreshold;
    }
    
    public Integer getNotificationCooldownHours() {
        return notificationCooldownHours;
    }
    
    public void setNotificationCooldownHours(Integer notificationCooldownHours) {
        this.notificationCooldownHours = notificationCooldownHours;
    }
    
    public String getNotificationFromEmail() {
        return notificationFromEmail;
    }
    
    public void setNotificationFromEmail(String notificationFromEmail) {
        this.notificationFromEmail = notificationFromEmail;
    }
    
    public String getNotificationTemplate() {
        return notificationTemplate;
    }
    
    public void setNotificationTemplate(String notificationTemplate) {
        this.notificationTemplate = notificationTemplate;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    
    public Instant getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
