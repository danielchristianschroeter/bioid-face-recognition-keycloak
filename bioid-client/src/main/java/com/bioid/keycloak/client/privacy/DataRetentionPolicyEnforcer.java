package com.bioid.keycloak.client.privacy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Data retention policy enforcer for GDPR compliance and automatic template expiration.
 * 
 * Features:
 * - Automatic template expiration based on configured TTL
 * - GDPR-compliant data retention policies
 * - Configurable retention periods for different data types
 * - Audit logging of retention policy enforcement
 * - Compliance validation and reporting
 */
public class DataRetentionPolicyEnforcer {
    
    private static final Logger logger = LoggerFactory.getLogger(DataRetentionPolicyEnforcer.class);
    
    // Default retention policies (configurable)
    private static final Duration DEFAULT_TEMPLATE_TTL = Duration.ofDays(730); // 2 years
    private static final Duration DEFAULT_AUDIT_LOG_TTL = Duration.ofDays(2555); // 7 years
    private static final Duration DEFAULT_METADATA_TTL = Duration.ofDays(730); // 2 years
    private static final Duration DEFAULT_SESSION_TTL = Duration.ofMinutes(5); // 5 minutes
    
    private final ConcurrentMap<String, RetentionPolicy> retentionPolicies = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, DataRecord> trackedRecords = new ConcurrentHashMap<>();
    private final AtomicLong totalRecordsProcessed = new AtomicLong(0);
    private final AtomicInteger expiredRecordsRemoved = new AtomicInteger(0);
    
    public DataRetentionPolicyEnforcer() {
        initializeDefaultPolicies();
        logger.info("Initialized data retention policy enforcer with GDPR compliance");
    }
    
    /**
     * Registers a data record for retention policy tracking.
     * 
     * @param recordId unique record identifier
     * @param dataType type of data (TEMPLATE, METADATA, AUDIT_LOG, SESSION)
     * @param createdAt creation timestamp
     * @param customTtl custom TTL override (optional)
     */
    public void registerDataRecord(String recordId, DataType dataType, Instant createdAt, 
            Duration customTtl) {
        
        Duration ttl = customTtl != null ? customTtl : getRetentionPolicy(dataType).ttl;
        Instant expiresAt = createdAt.plus(ttl);
        
        DataRecord record = new DataRecord(recordId, dataType, createdAt, expiresAt, ttl);
        trackedRecords.put(recordId, record);
        totalRecordsProcessed.incrementAndGet();
        
        logger.debug("Registered data record: {} (type: {}, expires: {})", 
            recordId, dataType, expiresAt);
    }
    
    /**
     * Removes a data record from retention tracking.
     * 
     * @param recordId record identifier to remove
     */
    public void unregisterDataRecord(String recordId) {
        DataRecord removed = trackedRecords.remove(recordId);
        if (removed != null) {
            logger.debug("Unregistered data record: {} (type: {})", recordId, removed.dataType);
        }
    }
    
    /**
     * Enforces retention policies by identifying and processing expired records.
     * 
     * @return number of expired records processed
     */
    public int enforceRetentionPolicies() {
        Instant now = Instant.now();
        int expiredCount = 0;
        
        for (var entry : trackedRecords.entrySet()) {
            DataRecord record = entry.getValue();
            
            if (record.expiresAt.isBefore(now)) {
                // Record has expired
                processExpiredRecord(record);
                trackedRecords.remove(entry.getKey());
                expiredCount++;
            }
        }
        
        if (expiredCount > 0) {
            expiredRecordsRemoved.addAndGet(expiredCount);
            logger.info("Retention policy enforcement: processed {} expired records", expiredCount);
        }
        
        return expiredCount;
    }
    
    /**
     * Validates compliance with data retention policies.
     * 
     * @return true if all policies are being enforced correctly
     */
    public boolean validateCompliance() {
        Instant now = Instant.now();
        int violationCount = 0;
        
        // Check for records that should have been expired
        for (DataRecord record : trackedRecords.values()) {
            if (record.expiresAt.isBefore(now.minus(Duration.ofHours(1)))) {
                // Record is overdue for expiration (1 hour grace period)
                logger.warn("Retention policy violation: record {} overdue for expiration by {}", 
                    record.recordId, Duration.between(record.expiresAt, now));
                violationCount++;
            }
        }
        
        boolean compliant = violationCount == 0;
        logger.debug("Retention policy compliance validation: {} violations found", violationCount);
        
        return compliant;
    }
    
    /**
     * Gets retention statistics for compliance reporting.
     */
    public RetentionStats getRetentionStats() {
        Instant now = Instant.now();
        int expiringSoon = 0;
        int overdue = 0;
        
        for (DataRecord record : trackedRecords.values()) {
            Duration timeToExpiry = Duration.between(now, record.expiresAt);
            
            if (timeToExpiry.isNegative()) {
                overdue++;
            } else if (timeToExpiry.compareTo(Duration.ofDays(7)) < 0) {
                expiringSoon++;
            }
        }
        
        return new RetentionStats(
            totalRecordsProcessed.get(),
            trackedRecords.size(),
            expiredRecordsRemoved.get(),
            expiringSoon,
            overdue
        );
    }
    
    /**
     * Updates retention policy for a specific data type.
     * 
     * @param dataType data type to update
     * @param newTtl new time-to-live duration
     * @param description policy description
     */
    public void updateRetentionPolicy(DataType dataType, Duration newTtl, String description) {
        RetentionPolicy policy = new RetentionPolicy(dataType, newTtl, description, Instant.now());
        retentionPolicies.put(dataType.name(), policy);
        
        logger.info("Updated retention policy for {}: {} ({})", dataType, newTtl, description);
    }
    
    /**
     * Gets current retention policy for a data type.
     */
    public RetentionPolicy getRetentionPolicy(DataType dataType) {
        return retentionPolicies.get(dataType.name());
    }
    
    /**
     * Forces immediate expiration of all records of a specific type.
     * Used for GDPR "right to be forgotten" requests.
     * 
     * @param dataType type of data to expire
     * @param reason reason for forced expiration
     * @return number of records expired
     */
    public int forceExpiration(DataType dataType, String reason) {
        int expiredCount = 0;
        
        for (var entry : trackedRecords.entrySet()) {
            DataRecord record = entry.getValue();
            
            if (record.dataType == dataType) {
                processExpiredRecord(record, reason);
                trackedRecords.remove(entry.getKey());
                expiredCount++;
            }
        }
        
        expiredRecordsRemoved.addAndGet(expiredCount);
        logger.info("Forced expiration of {} {} records (reason: {})", 
            expiredCount, dataType, reason);
        
        return expiredCount;
    }
    
    /**
     * Initializes default retention policies.
     */
    private void initializeDefaultPolicies() {
        retentionPolicies.put(DataType.TEMPLATE.name(), 
            new RetentionPolicy(DataType.TEMPLATE, DEFAULT_TEMPLATE_TTL, 
                "Biometric template retention for authentication", Instant.now()));
        
        retentionPolicies.put(DataType.METADATA.name(), 
            new RetentionPolicy(DataType.METADATA, DEFAULT_METADATA_TTL, 
                "Credential metadata retention", Instant.now()));
        
        retentionPolicies.put(DataType.AUDIT_LOG.name(), 
            new RetentionPolicy(DataType.AUDIT_LOG, DEFAULT_AUDIT_LOG_TTL, 
                "Audit log retention for compliance", Instant.now()));
        
        retentionPolicies.put(DataType.SESSION.name(), 
            new RetentionPolicy(DataType.SESSION, DEFAULT_SESSION_TTL, 
                "Biometric session data retention", Instant.now()));
        
        logger.debug("Initialized {} default retention policies", retentionPolicies.size());
    }
    
    /**
     * Processes an expired data record.
     */
    private void processExpiredRecord(DataRecord record) {
        processExpiredRecord(record, "Automatic expiration");
    }
    
    /**
     * Processes an expired data record with custom reason.
     */
    private void processExpiredRecord(DataRecord record, String reason) {
        logger.info("Processing expired data record: {} (type: {}, reason: {})", 
            record.recordId, record.dataType, reason);
        
        // In a real implementation, this would:
        // 1. Delete the actual data from storage
        // 2. Call BioID DeleteTemplate service for templates
        // 3. Remove database records
        // 4. Clear any cached data
        
        // For now, we just log the action
        logger.debug("Expired record processed: {} (expired: {}, ttl: {})", 
            record.recordId, record.expiresAt, record.ttl);
    }
    
    /**
     * Data types subject to retention policies.
     */
    public enum DataType {
        TEMPLATE("Biometric template"),
        METADATA("Credential metadata"),
        AUDIT_LOG("Audit log entry"),
        SESSION("Biometric session data");
        
        private final String description;
        
        DataType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * Retention policy configuration.
     */
    public static class RetentionPolicy {
        private final DataType dataType;
        private final Duration ttl;
        private final String description;
        private final Instant createdAt;
        
        public RetentionPolicy(DataType dataType, Duration ttl, String description, Instant createdAt) {
            this.dataType = dataType;
            this.ttl = ttl;
            this.description = description;
            this.createdAt = createdAt;
        }
        
        public DataType getDataType() { return dataType; }
        public Duration getTtl() { return ttl; }
        public String getDescription() { return description; }
        public Instant getCreatedAt() { return createdAt; }
        
        @Override
        public String toString() {
            return String.format("RetentionPolicy{type=%s, ttl=%d days, description='%s'}",
                dataType, ttl.toDays(), description);
        }
    }
    
    /**
     * Data record tracking information.
     */
    private static class DataRecord {
        final String recordId;
        final DataType dataType;
        final Instant createdAt;
        final Instant expiresAt;
        final Duration ttl;
        
        DataRecord(String recordId, DataType dataType, Instant createdAt, Instant expiresAt, Duration ttl) {
            this.recordId = recordId;
            this.dataType = dataType;
            this.createdAt = createdAt;
            this.expiresAt = expiresAt;
            this.ttl = ttl;
        }
    }
    
    /**
     * Retention statistics for compliance reporting.
     */
    public static class RetentionStats {
        private final long totalProcessed;
        private final int activeRecords;
        private final int expiredRemoved;
        private final int expiringSoon;
        private final int overdue;
        
        public RetentionStats(long totalProcessed, int activeRecords, int expiredRemoved, 
                int expiringSoon, int overdue) {
            this.totalProcessed = totalProcessed;
            this.activeRecords = activeRecords;
            this.expiredRemoved = expiredRemoved;
            this.expiringSoon = expiringSoon;
            this.overdue = overdue;
        }
        
        public long getTotalProcessed() { return totalProcessed; }
        public int getActiveRecords() { return activeRecords; }
        public int getExpiredRemoved() { return expiredRemoved; }
        public int getExpiringSoon() { return expiringSoon; }
        public int getOverdue() { return overdue; }
        
        public boolean isCompliant() { return overdue == 0; }
        
        @Override
        public String toString() {
            return String.format("RetentionStats{total=%d, active=%d, expired=%d, " +
                "expiringSoon=%d, overdue=%d, compliant=%s}",
                totalProcessed, activeRecords, expiredRemoved, expiringSoon, overdue, isCompliant());
        }
    }
}