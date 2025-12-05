package com.bioid.keycloak.failedauth.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * JPA Entity for user preferences related to failed authentication storage.
 */
@Entity
@Table(name = "failed_auth_user_preferences", indexes = {
    @Index(name = "idx_prefs_realm", columnList = "realm_id")
})
public class FailedAuthUserPreferencesEntity {
    
    @Id
    @Column(name = "user_id", length = 36, nullable = false)
    private String userId;
    
    @Column(name = "realm_id", length = 36, nullable = false)
    private String realmId;
    
    // Feature preferences
    @Column(name = "storage_enabled")
    private Boolean storageEnabled = true;
    
    @Column(name = "notification_enabled")
    private Boolean notificationEnabled = true;
    
    @Column(name = "notification_threshold")
    private Integer notificationThreshold = 3;
    
    @Column(name = "notification_cooldown_hours")
    private Integer notificationCooldownHours = 24;
    
    @Column(name = "last_notification_sent")
    private Instant lastNotificationSent;
    
    // Privacy preferences
    @Column(name = "privacy_notice_accepted")
    private Boolean privacyNoticeAccepted = false;
    
    @Column(name = "privacy_notice_accepted_at")
    private Instant privacyNoticeAcceptedAt;
    
    // Statistics
    @Column(name = "total_attempts")
    private Integer totalAttempts = 0;
    
    @Column(name = "enrolled_attempts")
    private Integer enrolledAttempts = 0;
    
    @Column(name = "deleted_attempts")
    private Integer deletedAttempts = 0;
    
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
    public FailedAuthUserPreferencesEntity() {
    }
    
    public FailedAuthUserPreferencesEntity(String userId, String realmId) {
        this.userId = userId;
        this.realmId = realmId;
    }
    
    // Getters and Setters
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getRealmId() {
        return realmId;
    }
    
    public void setRealmId(String realmId) {
        this.realmId = realmId;
    }
    
    public Boolean getStorageEnabled() {
        return storageEnabled;
    }
    
    public void setStorageEnabled(Boolean storageEnabled) {
        this.storageEnabled = storageEnabled;
    }
    
    public Boolean getNotificationEnabled() {
        return notificationEnabled;
    }
    
    public void setNotificationEnabled(Boolean notificationEnabled) {
        this.notificationEnabled = notificationEnabled;
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
    
    public Instant getLastNotificationSent() {
        return lastNotificationSent;
    }
    
    public void setLastNotificationSent(Instant lastNotificationSent) {
        this.lastNotificationSent = lastNotificationSent;
    }
    
    public Boolean getPrivacyNoticeAccepted() {
        return privacyNoticeAccepted;
    }
    
    public void setPrivacyNoticeAccepted(Boolean privacyNoticeAccepted) {
        this.privacyNoticeAccepted = privacyNoticeAccepted;
    }
    
    public Instant getPrivacyNoticeAcceptedAt() {
        return privacyNoticeAcceptedAt;
    }
    
    public void setPrivacyNoticeAcceptedAt(Instant privacyNoticeAcceptedAt) {
        this.privacyNoticeAcceptedAt = privacyNoticeAcceptedAt;
    }
    
    public Integer getTotalAttempts() {
        return totalAttempts;
    }
    
    public void setTotalAttempts(Integer totalAttempts) {
        this.totalAttempts = totalAttempts;
    }
    
    public void incrementTotalAttempts() {
        this.totalAttempts = (this.totalAttempts == null ? 0 : this.totalAttempts) + 1;
    }
    
    public Integer getEnrolledAttempts() {
        return enrolledAttempts;
    }
    
    public void setEnrolledAttempts(Integer enrolledAttempts) {
        this.enrolledAttempts = enrolledAttempts;
    }
    
    public void incrementEnrolledAttempts() {
        this.enrolledAttempts = (this.enrolledAttempts == null ? 0 : this.enrolledAttempts) + 1;
    }
    
    public Integer getDeletedAttempts() {
        return deletedAttempts;
    }
    
    public void setDeletedAttempts(Integer deletedAttempts) {
        this.deletedAttempts = deletedAttempts;
    }
    
    public void incrementDeletedAttempts() {
        this.deletedAttempts = (this.deletedAttempts == null ? 0 : this.deletedAttempts) + 1;
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
    
    /**
     * Check if user should be notified based on threshold and cooldown.
     */
    public boolean shouldNotify() {
        if (!notificationEnabled) {
            return false;
        }
        
        // Check if we've reached the threshold
        int unenrolledCount = totalAttempts - enrolledAttempts - deletedAttempts;
        if (unenrolledCount < notificationThreshold) {
            return false;
        }
        
        // Check cooldown period
        if (lastNotificationSent != null) {
            Instant cooldownExpiry = lastNotificationSent.plusSeconds(
                notificationCooldownHours * 3600L);
            if (Instant.now().isBefore(cooldownExpiry)) {
                return false;
            }
        }
        
        return true;
    }
}
