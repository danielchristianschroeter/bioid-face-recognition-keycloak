package com.bioid.keycloak.failedauth.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA Entity for failed authentication attempts.
 * 
 * Stores metadata about failed authentication attempts including
 * failure reason, verification scores, and enrollment status.
 */
@Entity
@Table(name = "failed_auth_attempts", indexes = {
    @Index(name = "idx_user_timestamp", columnList = "user_id,timestamp DESC"),
    @Index(name = "idx_realm_user", columnList = "realm_id,user_id"),
    @Index(name = "idx_enrolled", columnList = "user_id,enrolled"),
    @Index(name = "idx_expires", columnList = "expires_at"),
    @Index(name = "idx_class_id", columnList = "class_id")
})
public class FailedAuthAttemptEntity {
    
    @Id
    @Column(name = "attempt_id", length = 36, nullable = false)
    private String attemptId;
    
    @Column(name = "user_id", length = 36, nullable = false)
    private String userId;
    
    @Column(name = "realm_id", length = 36, nullable = false)
    private String realmId;
    
    @Column(name = "username", length = 255, nullable = false)
    private String username;
    
    @Column(name = "class_id", nullable = false)
    private Long classId;
    
    // Timestamps
    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at")
    private Instant updatedAt;
    
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    
    // Failure details
    @Column(name = "failure_reason", length = 50, nullable = false)
    private String failureReason;
    
    @Column(name = "verification_score")
    private Double verificationScore;
    
    @Column(name = "verification_threshold")
    private Double verificationThreshold;
    
    @Column(name = "score_difference")
    private Double scoreDifference;
    
    // Liveness information
    @Column(name = "liveness_mode", length = 20)
    private String livenessMode;
    
    @Column(name = "liveness_score")
    private Double livenessScore;
    
    @Column(name = "liveness_threshold")
    private Double livenessThreshold;
    
    @Column(name = "liveness_passed")
    private Boolean livenessPassed;
    
    @Column(name = "challenge_direction", length = 10)
    private String challengeDirection;
    
    // Retry information
    @Column(name = "retry_attempt")
    private Integer retryAttempt = 1;
    
    @Column(name = "max_retries")
    private Integer maxRetries = 3;
    
    // Image information
    @Column(name = "image_count", nullable = false)
    private Integer imageCount;
    
    @Column(name = "avg_quality_score")
    private Double avgQualityScore;
    
    // Enrollment status
    @Column(name = "enrolled")
    private Boolean enrolled = false;
    
    @Column(name = "enrolled_at")
    private Instant enrolledAt;
    
    @Column(name = "enrolled_by", length = 36)
    private String enrolledBy;
    
    @Column(name = "enrolled_image_indices", columnDefinition = "TEXT")
    private String enrolledImageIndices; // JSON array: [0,1]
    
    @Column(name = "enrollment_result", columnDefinition = "TEXT")
    private String enrollmentResult; // JSON object
    
    // Review status
    @Column(name = "reviewed")
    private Boolean reviewed = false;
    
    @Column(name = "reviewed_at")
    private Instant reviewedAt;
    
    @Column(name = "reviewed_by", length = 36)
    private String reviewedBy;
    
    @Column(name = "review_notes", columnDefinition = "TEXT")
    private String reviewNotes;
    
    // Session information
    @Column(name = "session_id", length = 255)
    private String sessionId;
    
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    
    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;
    
    // Security
    @Column(name = "encrypted")
    private Boolean encrypted = true;
    
    @Column(name = "checksum_sha256", length = 64)
    private String checksumSha256;
    
    @Column(name = "integrity_verified")
    private Boolean integrityVerified = true;
    
    // Metadata
    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;
    
    // Relationships
    @OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<FailedAuthImageEntity> images = new ArrayList<>();
    
    @OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<FailedAuthAuditLogEntity> auditLogs = new ArrayList<>();
    
    // Lifecycle callbacks
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
    public FailedAuthAttemptEntity() {
    }
    
    public FailedAuthAttemptEntity(String attemptId, String userId, String realmId, 
                                   String username, Long classId) {
        this.attemptId = attemptId;
        this.userId = userId;
        this.realmId = realmId;
        this.username = username;
        this.classId = classId;
        this.timestamp = Instant.now();
    }
    
    // Getters and Setters
    public String getAttemptId() {
        return attemptId;
    }
    
    public void setAttemptId(String attemptId) {
        this.attemptId = attemptId;
    }
    
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
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public Long getClassId() {
        return classId;
    }
    
    public void setClassId(Long classId) {
        this.classId = classId;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
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
    
    public Instant getExpiresAt() {
        return expiresAt;
    }
    
    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
    
    public String getFailureReason() {
        return failureReason;
    }
    
    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }
    
    public Double getVerificationScore() {
        return verificationScore;
    }
    
    public void setVerificationScore(Double verificationScore) {
        this.verificationScore = verificationScore;
    }
    
    public Double getVerificationThreshold() {
        return verificationThreshold;
    }
    
    public void setVerificationThreshold(Double verificationThreshold) {
        this.verificationThreshold = verificationThreshold;
    }
    
    public Double getScoreDifference() {
        return scoreDifference;
    }
    
    public void setScoreDifference(Double scoreDifference) {
        this.scoreDifference = scoreDifference;
    }
    
    public String getLivenessMode() {
        return livenessMode;
    }
    
    public void setLivenessMode(String livenessMode) {
        this.livenessMode = livenessMode;
    }
    
    public Double getLivenessScore() {
        return livenessScore;
    }
    
    public void setLivenessScore(Double livenessScore) {
        this.livenessScore = livenessScore;
    }
    
    public Double getLivenessThreshold() {
        return livenessThreshold;
    }
    
    public void setLivenessThreshold(Double livenessThreshold) {
        this.livenessThreshold = livenessThreshold;
    }
    
    public Boolean getLivenessPassed() {
        return livenessPassed;
    }
    
    public void setLivenessPassed(Boolean livenessPassed) {
        this.livenessPassed = livenessPassed;
    }
    
    public String getChallengeDirection() {
        return challengeDirection;
    }
    
    public void setChallengeDirection(String challengeDirection) {
        this.challengeDirection = challengeDirection;
    }
    
    public Integer getRetryAttempt() {
        return retryAttempt;
    }
    
    public void setRetryAttempt(Integer retryAttempt) {
        this.retryAttempt = retryAttempt;
    }
    
    public Integer getMaxRetries() {
        return maxRetries;
    }
    
    public void setMaxRetries(Integer maxRetries) {
        this.maxRetries = maxRetries;
    }
    
    public Integer getImageCount() {
        return imageCount;
    }
    
    public void setImageCount(Integer imageCount) {
        this.imageCount = imageCount;
    }
    
    public Double getAvgQualityScore() {
        return avgQualityScore;
    }
    
    public void setAvgQualityScore(Double avgQualityScore) {
        this.avgQualityScore = avgQualityScore;
    }
    
    public Boolean getEnrolled() {
        return enrolled;
    }
    
    public void setEnrolled(Boolean enrolled) {
        this.enrolled = enrolled;
    }
    
    public Instant getEnrolledAt() {
        return enrolledAt;
    }
    
    public void setEnrolledAt(Instant enrolledAt) {
        this.enrolledAt = enrolledAt;
    }
    
    public String getEnrolledBy() {
        return enrolledBy;
    }
    
    public void setEnrolledBy(String enrolledBy) {
        this.enrolledBy = enrolledBy;
    }
    
    public String getEnrolledImageIndices() {
        return enrolledImageIndices;
    }
    
    public void setEnrolledImageIndices(String enrolledImageIndices) {
        this.enrolledImageIndices = enrolledImageIndices;
    }
    
    public String getEnrollmentResult() {
        return enrollmentResult;
    }
    
    public void setEnrollmentResult(String enrollmentResult) {
        this.enrollmentResult = enrollmentResult;
    }
    
    public Boolean getReviewed() {
        return reviewed;
    }
    
    public void setReviewed(Boolean reviewed) {
        this.reviewed = reviewed;
    }
    
    public Instant getReviewedAt() {
        return reviewedAt;
    }
    
    public void setReviewedAt(Instant reviewedAt) {
        this.reviewedAt = reviewedAt;
    }
    
    public String getReviewedBy() {
        return reviewedBy;
    }
    
    public void setReviewedBy(String reviewedBy) {
        this.reviewedBy = reviewedBy;
    }
    
    public String getReviewNotes() {
        return reviewNotes;
    }
    
    public void setReviewNotes(String reviewNotes) {
        this.reviewNotes = reviewNotes;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    
    public String getUserAgent() {
        return userAgent;
    }
    
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
    
    public Boolean getEncrypted() {
        return encrypted;
    }
    
    public void setEncrypted(Boolean encrypted) {
        this.encrypted = encrypted;
    }
    
    public String getChecksumSha256() {
        return checksumSha256;
    }
    
    public void setChecksumSha256(String checksumSha256) {
        this.checksumSha256 = checksumSha256;
    }
    
    public Boolean getIntegrityVerified() {
        return integrityVerified;
    }
    
    public void setIntegrityVerified(Boolean integrityVerified) {
        this.integrityVerified = integrityVerified;
    }
    
    public String getMetadataJson() {
        return metadataJson;
    }
    
    public void setMetadataJson(String metadataJson) {
        this.metadataJson = metadataJson;
    }
    
    public List<FailedAuthImageEntity> getImages() {
        return images;
    }
    
    public void setImages(List<FailedAuthImageEntity> images) {
        this.images = images;
    }
    
    public void addImage(FailedAuthImageEntity image) {
        images.add(image);
        image.setAttempt(this);
    }
    
    public void removeImage(FailedAuthImageEntity image) {
        images.remove(image);
        image.setAttempt(null);
    }
    
    public List<FailedAuthAuditLogEntity> getAuditLogs() {
        return auditLogs;
    }
    
    public void setAuditLogs(List<FailedAuthAuditLogEntity> auditLogs) {
        this.auditLogs = auditLogs;
    }
    
    public void addAuditLog(FailedAuthAuditLogEntity auditLog) {
        auditLogs.add(auditLog);
        auditLog.setAttempt(this);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FailedAuthAttemptEntity)) return false;
        FailedAuthAttemptEntity that = (FailedAuthAttemptEntity) o;
        return attemptId != null && attemptId.equals(that.attemptId);
    }
    
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
    
    @Override
    public String toString() {
        return "FailedAuthAttemptEntity{" +
                "attemptId='" + attemptId + '\'' +
                ", userId='" + userId + '\'' +
                ", username='" + username + '\'' +
                ", timestamp=" + timestamp +
                ", failureReason='" + failureReason + '\'' +
                ", enrolled=" + enrolled +
                '}';
    }
}
