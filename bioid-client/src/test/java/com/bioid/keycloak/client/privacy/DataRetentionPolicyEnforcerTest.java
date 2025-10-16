package com.bioid.keycloak.client.privacy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Privacy compliance tests for DataRetentionPolicyEnforcer implementation.
 */
class DataRetentionPolicyEnforcerTest {
    
    private DataRetentionPolicyEnforcer enforcer;
    
    @BeforeEach
    void setUp() {
        enforcer = new DataRetentionPolicyEnforcer();
    }
    
    @Test
    @DisplayName("Should register and track data records")
    void testDataRecordRegistration() {
        // Given
        String recordId = "test-record-001";
        DataRetentionPolicyEnforcer.DataType dataType = DataRetentionPolicyEnforcer.DataType.TEMPLATE;
        Instant createdAt = Instant.now();
        Duration customTtl = Duration.ofDays(365);
        
        // When
        enforcer.registerDataRecord(recordId, dataType, createdAt, customTtl);
        
        // Then
        DataRetentionPolicyEnforcer.RetentionStats stats = enforcer.getRetentionStats();
        assertEquals(1, stats.getTotalProcessed());
        assertEquals(1, stats.getActiveRecords());
        assertEquals(0, stats.getExpiredRemoved());
    }
    
    @Test
    @DisplayName("Should unregister data records")
    void testDataRecordUnregistration() {
        // Given
        String recordId = "test-record-002";
        enforcer.registerDataRecord(recordId, DataRetentionPolicyEnforcer.DataType.METADATA, 
            Instant.now(), null);
        
        // When
        enforcer.unregisterDataRecord(recordId);
        
        // Then
        DataRetentionPolicyEnforcer.RetentionStats stats = enforcer.getRetentionStats();
        assertEquals(1, stats.getTotalProcessed());
        assertEquals(0, stats.getActiveRecords()); // Should be removed
    }
    
    @Test
    @DisplayName("Should enforce retention policies for expired records")
    void testRetentionPolicyEnforcement() {
        // Given - Register records with past expiration dates
        Instant pastTime = Instant.now().minus(Duration.ofDays(800)); // Older than default 730 days
        
        enforcer.registerDataRecord("expired-record-1", 
            DataRetentionPolicyEnforcer.DataType.TEMPLATE, pastTime, null);
        enforcer.registerDataRecord("expired-record-2", 
            DataRetentionPolicyEnforcer.DataType.METADATA, pastTime, null);
        
        // Also register a current record
        enforcer.registerDataRecord("current-record", 
            DataRetentionPolicyEnforcer.DataType.TEMPLATE, Instant.now(), null);
        
        // When
        int expiredCount = enforcer.enforceRetentionPolicies();
        
        // Then
        assertEquals(2, expiredCount); // Two expired records should be processed
        
        DataRetentionPolicyEnforcer.RetentionStats stats = enforcer.getRetentionStats();
        assertEquals(1, stats.getActiveRecords()); // Only current record should remain
        assertEquals(2, stats.getExpiredRemoved());
    }
    
    @Test
    @DisplayName("Should validate retention policy compliance")
    void testRetentionPolicyCompliance() {
        // Given - Register current records
        Instant now = Instant.now();
        enforcer.registerDataRecord("compliant-record-1", 
            DataRetentionPolicyEnforcer.DataType.TEMPLATE, now, null);
        enforcer.registerDataRecord("compliant-record-2", 
            DataRetentionPolicyEnforcer.DataType.AUDIT_LOG, now, null);
        
        // When
        boolean compliant = enforcer.validateCompliance();
        
        // Then
        assertTrue(compliant, "Should be compliant with current records");
    }
    
    @Test
    @DisplayName("Should detect compliance violations")
    void testComplianceViolationDetection() {
        // Given - Register overdue record (more than 1 hour past expiration)
        Instant overdueTime = Instant.now().minus(Duration.ofDays(731)); // Past default TTL + grace period
        
        enforcer.registerDataRecord("overdue-record", 
            DataRetentionPolicyEnforcer.DataType.TEMPLATE, overdueTime, null);
        
        // When
        boolean compliant = enforcer.validateCompliance();
        
        // Then
        assertFalse(compliant, "Should detect compliance violation for overdue record");
    }
    
    @Test
    @DisplayName("Should update retention policies")
    void testRetentionPolicyUpdate() {
        // Given
        DataRetentionPolicyEnforcer.DataType dataType = DataRetentionPolicyEnforcer.DataType.TEMPLATE;
        Duration newTtl = Duration.ofDays(1095); // 3 years
        String description = "Extended retention for compliance";
        
        // When
        enforcer.updateRetentionPolicy(dataType, newTtl, description);
        
        // Then
        DataRetentionPolicyEnforcer.RetentionPolicy policy = enforcer.getRetentionPolicy(dataType);
        assertNotNull(policy);
        assertEquals(dataType, policy.getDataType());
        assertEquals(newTtl, policy.getTtl());
        assertEquals(description, policy.getDescription());
    }
    
    @Test
    @DisplayName("Should force expiration for GDPR requests")
    void testForceExpirationForGdpr() {
        // Given
        DataRetentionPolicyEnforcer.DataType targetType = DataRetentionPolicyEnforcer.DataType.TEMPLATE;
        
        enforcer.registerDataRecord("gdpr-record-1", targetType, Instant.now(), null);
        enforcer.registerDataRecord("gdpr-record-2", targetType, Instant.now(), null);
        enforcer.registerDataRecord("other-record", 
            DataRetentionPolicyEnforcer.DataType.METADATA, Instant.now(), null);
        
        // When
        int expiredCount = enforcer.forceExpiration(targetType, "GDPR right to be forgotten");
        
        // Then
        assertEquals(2, expiredCount); // Only template records should be expired
        
        DataRetentionPolicyEnforcer.RetentionStats stats = enforcer.getRetentionStats();
        assertEquals(1, stats.getActiveRecords()); // Only metadata record should remain
        assertEquals(2, stats.getExpiredRemoved());
    }
    
    @Test
    @DisplayName("Should handle different data types correctly")
    void testDifferentDataTypes() {
        // Given
        DataRetentionPolicyEnforcer.DataType[] dataTypes = {
            DataRetentionPolicyEnforcer.DataType.TEMPLATE,
            DataRetentionPolicyEnforcer.DataType.METADATA,
            DataRetentionPolicyEnforcer.DataType.AUDIT_LOG,
            DataRetentionPolicyEnforcer.DataType.SESSION
        };
        
        // When
        for (int i = 0; i < dataTypes.length; i++) {
            DataRetentionPolicyEnforcer.DataType dataType = dataTypes[i];
            enforcer.registerDataRecord("record-" + i, dataType, Instant.now(), null);
            
            // Verify policy exists for each type
            DataRetentionPolicyEnforcer.RetentionPolicy policy = enforcer.getRetentionPolicy(dataType);
            assertNotNull(policy, "Policy should exist for " + dataType);
            assertEquals(dataType, policy.getDataType());
            assertNotNull(policy.getDescription());
            assertTrue(policy.getTtl().toSeconds() > 0);
        }
        
        // Then
        DataRetentionPolicyEnforcer.RetentionStats stats = enforcer.getRetentionStats();
        assertEquals(dataTypes.length, stats.getActiveRecords());
    }
    
    @Test
    @DisplayName("Should track retention statistics accurately")
    void testRetentionStatisticsTracking() {
        // Given
        Instant now = Instant.now();
        
        // Register records with different expiration times
        enforcer.registerDataRecord("current-record", 
            DataRetentionPolicyEnforcer.DataType.TEMPLATE, now, Duration.ofDays(30));
        
        enforcer.registerDataRecord("expiring-soon-record", 
            DataRetentionPolicyEnforcer.DataType.TEMPLATE, now, Duration.ofDays(3));
        
        enforcer.registerDataRecord("expired-record", 
            DataRetentionPolicyEnforcer.DataType.TEMPLATE, 
            now.minus(Duration.ofDays(10)), Duration.ofDays(5));
        
        // When
        DataRetentionPolicyEnforcer.RetentionStats stats = enforcer.getRetentionStats();
        
        // Then
        assertEquals(3, stats.getTotalProcessed());
        assertEquals(3, stats.getActiveRecords());
        assertEquals(1, stats.getExpiringSoon()); // Record expiring in 3 days
        assertEquals(1, stats.getOverdue()); // Expired record
        assertFalse(stats.isCompliant()); // Due to overdue record
    }
    
    @Test
    @DisplayName("Should handle custom TTL overrides")
    void testCustomTtlOverrides() {
        // Given
        DataRetentionPolicyEnforcer.DataType dataType = DataRetentionPolicyEnforcer.DataType.TEMPLATE;
        Duration customTtl = Duration.ofDays(90); // Shorter than default
        Instant createdAt = Instant.now().minus(Duration.ofDays(100)); // Past custom TTL
        
        // When
        enforcer.registerDataRecord("custom-ttl-record", dataType, createdAt, customTtl);
        int expiredCount = enforcer.enforceRetentionPolicies();
        
        // Then
        assertEquals(1, expiredCount); // Should be expired due to custom TTL
    }
    
    @Test
    @DisplayName("Should validate data type descriptions")
    void testDataTypeDescriptions() {
        // Test all data types have descriptions
        DataRetentionPolicyEnforcer.DataType[] dataTypes = 
            DataRetentionPolicyEnforcer.DataType.values();
        
        for (DataRetentionPolicyEnforcer.DataType dataType : dataTypes) {
            assertNotNull(dataType.getDescription());
            assertFalse(dataType.getDescription().isEmpty());
        }
    }
    
    @Test
    @DisplayName("Should handle empty retention enforcement")
    void testEmptyRetentionEnforcement() {
        // When - No records registered
        int expiredCount = enforcer.enforceRetentionPolicies();
        boolean compliant = enforcer.validateCompliance();
        
        // Then
        assertEquals(0, expiredCount);
        assertTrue(compliant);
        
        DataRetentionPolicyEnforcer.RetentionStats stats = enforcer.getRetentionStats();
        assertEquals(0, stats.getActiveRecords());
        assertEquals(0, stats.getExpiredRemoved());
        assertTrue(stats.isCompliant());
    }
}