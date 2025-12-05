package com.bioid.keycloak.failedauth.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * JPA Entity for audit logging of failed authentication operations.
 */
@Entity
@Table(name = "failed_auth_audit_log", indexes = {
    @Index(name = "idx_audit_attempt", columnList = "attempt_id"),
    @Index(name = "idx_audit_user", columnList = "user_id"),
    @Index(name = "idx_audit_performed_at", columnList = "performed_at DESC")
})
public class FailedAuthAuditLogEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id", nullable = false)
    private FailedAuthAttemptEntity attempt;
    
    @Column(name = "user_id", length = 36, nullable = false)
    private String userId;
    
    @Column(name = "action", length = 50, nullable = false)
    private String action; // VIEW, ENROLL, DELETE, ADMIN_VIEW
    
    @Column(name = "performed_by", length = 36, nullable = false)
    private String performedBy;
    
    @Column(name = "performed_at", nullable = false)
    private Instant performedAt;
    
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    
    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;
    
    @Column(name = "details", columnDefinition = "TEXT")
    private String details; // JSON object with action details
    
    @PrePersist
    protected void onCreate() {
        if (performedAt == null) {
            performedAt = Instant.now();
        }
    }
    
    // Constructors
    public FailedAuthAuditLogEntity() {
    }
    
    public FailedAuthAuditLogEntity(FailedAuthAttemptEntity attempt, String userId, 
                                    String action, String performedBy) {
        this.attempt = attempt;
        this.userId = userId;
        this.action = action;
        this.performedBy = performedBy;
    }
    
    // Getters and Setters
    public Long getLogId() {
        return logId;
    }
    
    public void setLogId(Long logId) {
        this.logId = logId;
    }
    
    public FailedAuthAttemptEntity getAttempt() {
        return attempt;
    }
    
    public void setAttempt(FailedAuthAttemptEntity attempt) {
        this.attempt = attempt;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getAction() {
        return action;
    }
    
    public void setAction(String action) {
        this.action = action;
    }
    
    public String getPerformedBy() {
        return performedBy;
    }
    
    public void setPerformedBy(String performedBy) {
        this.performedBy = performedBy;
    }
    
    public Instant getPerformedAt() {
        return performedAt;
    }
    
    public void setPerformedAt(Instant performedAt) {
        this.performedAt = performedAt;
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
    
    public String getDetails() {
        return details;
    }
    
    public void setDetails(String details) {
        this.details = details;
    }
}
