package com.bioid.keycloak.client.privacy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Privacy audit logger for GDPR-compliant logging of biometric data operations.
 * 
 * Features:
 * - Comprehensive audit trail for all biometric operations
 * - GDPR-compliant logging without sensitive data
 * - Structured logging with correlation IDs
 * - Audit trail validation and completeness checking
 * - Privacy event categorization and metrics
 */
public class PrivacyAuditLogger implements AutoCloseable {
    
    private static final Logger auditLog = LoggerFactory.getLogger("PRIVACY_AUDIT");
    private static final Logger logger = LoggerFactory.getLogger(PrivacyAuditLogger.class);
    
    private final ConcurrentMap<String, AuditSession> auditSessions = new ConcurrentHashMap<>();
    private final AtomicLong auditEventCounter = new AtomicLong(0);
    
    // Audit event types
    private static final String EVENT_BIOMETRIC_ACCESS = "BIOMETRIC_DATA_ACCESS";
    private static final String EVENT_PROCESSING_COMPLETE = "BIOMETRIC_PROCESSING_COMPLETE";
    private static final String EVENT_FORCED_CLEANUP = "BIOMETRIC_FORCED_CLEANUP";
    private static final String EVENT_DATA_EXPIRED = "BIOMETRIC_DATA_EXPIRED";
    private static final String EVENT_RETENTION_POLICY = "DATA_RETENTION_POLICY_APPLIED";
    private static final String EVENT_GDPR_REQUEST = "GDPR_DATA_REQUEST";
    
    public PrivacyAuditLogger() {
        logger.info("Initialized privacy audit logger for GDPR compliance");
    }
    
    /**
     * Logs biometric data access event.
     * 
     * @param sessionId session identifier
     * @param operation biometric operation type
     * @param dataSize size of biometric data in bytes
     */
    public void logBiometricDataAccess(String sessionId, 
            PrivacyProtectionService.BiometricOperation operation, int dataSize) {
        
        AuditSession session = new AuditSession(sessionId, operation, Instant.now());
        auditSessions.put(sessionId, session);
        
        try {
            MDC.put("sessionId", sessionId);
            MDC.put("operation", operation.name());
            MDC.put("eventType", EVENT_BIOMETRIC_ACCESS);
            MDC.put("auditId", String.valueOf(auditEventCounter.incrementAndGet()));
            
            auditLog.info("Biometric data accessed - Session: {}, Operation: {}, DataSize: {} bytes, " +
                "Timestamp: {}, ZeroPersistence: true", 
                sessionId, operation.getDescription(), dataSize, 
                DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
                
        } finally {
            MDC.clear();
        }
    }
    
    /**
     * Logs completion of biometric data processing.
     * 
     * @param sessionId session identifier
     * @param operation biometric operation type
     * @param processingDuration time taken for processing
     */
    public void logBiometricDataProcessingComplete(String sessionId, 
            PrivacyProtectionService.BiometricOperation operation, Duration processingDuration) {
        
        AuditSession session = auditSessions.remove(sessionId);
        
        try {
            MDC.put("sessionId", sessionId);
            MDC.put("operation", operation.name());
            MDC.put("eventType", EVENT_PROCESSING_COMPLETE);
            MDC.put("auditId", String.valueOf(auditEventCounter.incrementAndGet()));
            
            auditLog.info("Biometric data processing completed - Session: {}, Operation: {}, " +
                "ProcessingTime: {}ms, DataCleared: true, MemorySecured: true, Timestamp: {}", 
                sessionId, operation.getDescription(), processingDuration.toMillis(),
                DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
                
        } finally {
            MDC.clear();
        }
    }
    
    /**
     * Logs forced cleanup of biometric data.
     * 
     * @param sessionId session identifier
     * @param operation biometric operation type
     */
    public void logBiometricDataForcedCleanup(String sessionId, 
            PrivacyProtectionService.BiometricOperation operation) {
        
        auditSessions.remove(sessionId);
        
        try {
            MDC.put("sessionId", sessionId);
            MDC.put("operation", operation.name());
            MDC.put("eventType", EVENT_FORCED_CLEANUP);
            MDC.put("auditId", String.valueOf(auditEventCounter.incrementAndGet()));
            
            auditLog.warn("Biometric data forced cleanup - Session: {}, Operation: {}, " +
                "Reason: ForceCleanup, DataCleared: true, Timestamp: {}", 
                sessionId, operation.getDescription(),
                DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
                
        } finally {
            MDC.clear();
        }
    }
    
    /**
     * Logs expiration of biometric data session.
     * 
     * @param sessionId session identifier
     * @param operation biometric operation type
     */
    public void logBiometricDataExpired(String sessionId, 
            PrivacyProtectionService.BiometricOperation operation) {
        
        auditSessions.remove(sessionId);
        
        try {
            MDC.put("sessionId", sessionId);
            MDC.put("operation", operation.name());
            MDC.put("eventType", EVENT_DATA_EXPIRED);
            MDC.put("auditId", String.valueOf(auditEventCounter.incrementAndGet()));
            
            auditLog.warn("Biometric data session expired - Session: {}, Operation: {}, " +
                "Reason: MaxProcessingTimeExceeded, DataCleared: true, Timestamp: {}", 
                sessionId, operation.getDescription(),
                DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
                
        } finally {
            MDC.clear();
        }
    }
    
    /**
     * Logs data retention policy enforcement.
     * 
     * @param policyType type of retention policy
     * @param affectedRecords number of records affected
     * @param retentionPeriod retention period applied
     */
    public void logDataRetentionPolicyEnforcement(String policyType, int affectedRecords, 
            Duration retentionPeriod) {
        
        try {
            MDC.put("policyType", policyType);
            MDC.put("eventType", EVENT_RETENTION_POLICY);
            MDC.put("auditId", String.valueOf(auditEventCounter.incrementAndGet()));
            
            auditLog.info("Data retention policy enforced - PolicyType: {}, " +
                "AffectedRecords: {}, RetentionPeriod: {} days, Timestamp: {}", 
                policyType, affectedRecords, retentionPeriod.toDays(),
                DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
                
        } finally {
            MDC.clear();
        }
    }
    
    /**
     * Logs GDPR data subject request.
     * 
     * @param requestType type of GDPR request (ACCESS, RECTIFICATION, ERASURE, PORTABILITY)
     * @param subjectId data subject identifier
     * @param requestId unique request identifier
     */
    public void logGdprDataRequest(String requestType, String subjectId, String requestId) {
        
        try {
            MDC.put("requestType", requestType);
            MDC.put("subjectId", hashSubjectId(subjectId)); // Hash for privacy
            MDC.put("requestId", requestId);
            MDC.put("eventType", EVENT_GDPR_REQUEST);
            MDC.put("auditId", String.valueOf(auditEventCounter.incrementAndGet()));
            
            auditLog.info("GDPR data subject request - RequestType: {}, SubjectIdHash: {}, " +
                "RequestId: {}, Timestamp: {}", 
                requestType, hashSubjectId(subjectId), requestId,
                DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
                
        } finally {
            MDC.clear();
        }
    }
    
    /**
     * Logs credential metadata access for audit purposes.
     * 
     * @param credentialId credential identifier
     * @param operation operation performed (CREATE, READ, UPDATE, DELETE)
     * @param userId user identifier
     */
    public void logCredentialMetadataAccess(String credentialId, String operation, String userId) {
        
        try {
            MDC.put("credentialId", credentialId);
            MDC.put("operation", operation);
            MDC.put("userId", hashSubjectId(userId)); // Hash for privacy
            MDC.put("eventType", "CREDENTIAL_METADATA_ACCESS");
            MDC.put("auditId", String.valueOf(auditEventCounter.incrementAndGet()));
            
            auditLog.info("Credential metadata access - CredentialId: {}, Operation: {}, " +
                "UserIdHash: {}, RawBiometricData: false, Timestamp: {}", 
                credentialId, operation, hashSubjectId(userId),
                DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
                
        } finally {
            MDC.clear();
        }
    }
    
    /**
     * Validates completeness of audit trail.
     * 
     * @return true if audit trail is complete and valid
     */
    public boolean validateAuditTrail() {
        // Check for orphaned sessions (sessions without completion events)
        int orphanedSessions = auditSessions.size();
        
        if (orphanedSessions > 0) {
            logger.warn("Audit trail validation: {} orphaned biometric data sessions found", 
                orphanedSessions);
            return false;
        }
        
        // Check audit event counter consistency
        long totalEvents = auditEventCounter.get();
        if (totalEvents == 0) {
            logger.debug("Audit trail validation: No audit events recorded yet");
            return true;
        }
        
        logger.debug("Audit trail validation: {} total events, {} orphaned sessions", 
            totalEvents, orphanedSessions);
        
        return true;
    }
    
    /**
     * Gets audit statistics for compliance reporting.
     */
    public AuditStats getAuditStats() {
        return new AuditStats(
            auditEventCounter.get(),
            auditSessions.size(),
            Instant.now()
        );
    }
    
    /**
     * Hashes subject ID for privacy protection in logs using SHA-256.
     */
    private String hashSubjectId(String subjectId) {
        if (subjectId == null) {
            return "null";
        }
        
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(subjectId.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return "sha256-" + java.util.Base64.getEncoder().encodeToString(hash).substring(0, 16);
        } catch (java.security.NoSuchAlgorithmException e) {
            // Fallback to secure hash if SHA-256 not available (should never happen)
            logger.warn("SHA-256 not available, using fallback hash", e);
            return "hash-" + Math.abs(subjectId.hashCode());
        }
    }
    
    @Override
    public void close() {
        // Log any remaining orphaned sessions
        if (!auditSessions.isEmpty()) {
            logger.warn("Closing privacy audit logger with {} orphaned sessions", 
                auditSessions.size());
            
            for (String sessionId : auditSessions.keySet()) {
                AuditSession session = auditSessions.get(sessionId);
                logBiometricDataForcedCleanup(sessionId, session.operation);
            }
        }
        
        logger.info("Privacy audit logger closed - Total events: {}", auditEventCounter.get());
    }
    
    /**
     * Audit session tracking.
     */
    private static class AuditSession {
        final String sessionId;
        final PrivacyProtectionService.BiometricOperation operation;
        final Instant startTime;
        
        AuditSession(String sessionId, PrivacyProtectionService.BiometricOperation operation, 
                Instant startTime) {
            this.sessionId = sessionId;
            this.operation = operation;
            this.startTime = startTime;
        }
    }
    
    /**
     * Audit statistics for compliance reporting.
     */
    public static class AuditStats {
        private final long totalEvents;
        private final int activeSessions;
        private final Instant reportTime;
        
        public AuditStats(long totalEvents, int activeSessions, Instant reportTime) {
            this.totalEvents = totalEvents;
            this.activeSessions = activeSessions;
            this.reportTime = reportTime;
        }
        
        public long getTotalEvents() { return totalEvents; }
        public int getActiveSessions() { return activeSessions; }
        public Instant getReportTime() { return reportTime; }
        
        @Override
        public String toString() {
            return String.format("AuditStats{totalEvents=%d, activeSessions=%d, reportTime=%s}",
                totalEvents, activeSessions, DateTimeFormatter.ISO_INSTANT.format(reportTime));
        }
    }
}