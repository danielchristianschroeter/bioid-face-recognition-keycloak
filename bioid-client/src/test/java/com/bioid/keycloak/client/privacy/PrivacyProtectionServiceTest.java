package com.bioid.keycloak.client.privacy;

import com.bioid.keycloak.client.security.SecureMemoryHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.AfterEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Privacy compliance tests for PrivacyProtectionService implementation.
 */
class PrivacyProtectionServiceTest {
    
    private SecureMemoryHandler memoryHandler;
    private PrivacyProtectionService privacyService;
    
    @BeforeEach
    void setUp() {
        memoryHandler = new SecureMemoryHandler();
        privacyService = new PrivacyProtectionService(memoryHandler);
    }
    
    @AfterEach
    void tearDown() {
        if (privacyService != null) {
            privacyService.close();
        }
        if (memoryHandler != null) {
            memoryHandler.close();
        }
    }
    
    @Test
    @DisplayName("Should process biometric data with zero persistence guarantee")
    void testZeroPersistenceBiometricProcessing() {
        // Given
        String sessionId = "test-session-001";
        byte[] originalImageData = "test-biometric-image-data".getBytes();
        byte[] imageDataCopy = originalImageData.clone();
        
        // When
        PrivacyProtectionService.SecureImageHandle handle = 
            privacyService.processImageData(sessionId, imageDataCopy, 
                PrivacyProtectionService.BiometricOperation.ENROLL);
        
        // Then
        assertNotNull(handle);
        assertEquals(sessionId, handle.getSessionId());
        assertEquals(PrivacyProtectionService.BiometricOperation.ENROLL, handle.getOperation());
        
        // Original array should be cleared (zero persistence)
        for (byte b : imageDataCopy) {
            assertEquals(0, b, "Original image data should be cleared for zero persistence");
        }
        
        // Complete processing
        handle.close();
    }
    
    @Test
    @DisplayName("Should automatically clear biometric data after processing")
    void testAutomaticDataClearing() {
        // Given
        String sessionId = "test-session-002";
        byte[] imageData = "sensitive-biometric-data".getBytes();
        
        // When
        PrivacyProtectionService.SecureImageHandle handle = 
            privacyService.processImageData(sessionId, imageData, 
                PrivacyProtectionService.BiometricOperation.VERIFY);
        
        String bufferId = handle.getBufferId();
        
        // Complete processing (should clear data)
        handle.close();
        
        // Then
        // Attempting to access cleared buffer should fail
        assertThrows(SecurityException.class, () -> 
            memoryHandler.getImageData(bufferId));
    }
    
    @Test
    @DisplayName("Should track privacy statistics accurately")
    void testPrivacyStatisticsTracking() {
        // Given
        String sessionId1 = "session-stats-001";
        String sessionId2 = "session-stats-002";
        byte[] imageData1 = "image-data-1".getBytes();
        byte[] imageData2 = "image-data-2".getBytes();
        
        // When
        PrivacyProtectionService.SecureImageHandle handle1 = 
            privacyService.processImageData(sessionId1, imageData1, 
                PrivacyProtectionService.BiometricOperation.ENROLL);
        
        PrivacyProtectionService.SecureImageHandle handle2 = 
            privacyService.processImageData(sessionId2, imageData2, 
                PrivacyProtectionService.BiometricOperation.VERIFY);
        
        PrivacyProtectionService.PrivacyStats stats = privacyService.getPrivacyStats();
        
        // Then
        assertEquals(2, stats.getTotalProcessed());
        assertEquals(2, stats.getActiveSessions());
        assertEquals(0, stats.getTotalCleared()); // Not yet cleared
        
        // Complete processing
        handle1.close();
        handle2.close();
        
        PrivacyProtectionService.PrivacyStats finalStats = privacyService.getPrivacyStats();
        assertEquals(2, finalStats.getTotalProcessed());
        assertEquals(2, finalStats.getTotalCleared());
        assertEquals(0, finalStats.getActiveSessions());
        assertEquals(100.0, finalStats.getClearanceRate(), 0.01);
    }
    
    @Test
    @DisplayName("Should force cleanup of all biometric data")
    void testForceCleanup() {
        // Given
        byte[] imageData1 = "data-1".getBytes();
        byte[] imageData2 = "data-2".getBytes();
        
        PrivacyProtectionService.SecureImageHandle handle1 = 
            privacyService.processImageData("session-1", imageData1, 
                PrivacyProtectionService.BiometricOperation.ENROLL);
        
        PrivacyProtectionService.SecureImageHandle handle2 = 
            privacyService.processImageData("session-2", imageData2, 
                PrivacyProtectionService.BiometricOperation.VERIFY);
        
        // When
        privacyService.forceCleanup();
        
        // Then
        PrivacyProtectionService.PrivacyStats stats = privacyService.getPrivacyStats();
        assertEquals(0, stats.getActiveSessions());
        assertEquals(0, stats.getMemoryStats().getActiveBuffers());
        
        // Handles should be unusable after force cleanup
        assertThrows(SecurityException.class, () -> 
            memoryHandler.getImageData(handle1.getBufferId()));
        assertThrows(SecurityException.class, () -> 
            memoryHandler.getImageData(handle2.getBufferId()));
    }
    
    @Test
    @DisplayName("Should validate GDPR compliance")
    void testGdprComplianceValidation() {
        // Given - Process some biometric data
        byte[] imageData = "gdpr-test-data".getBytes();
        PrivacyProtectionService.SecureImageHandle handle = 
            privacyService.processImageData("gdpr-session", imageData, 
                PrivacyProtectionService.BiometricOperation.LIVENESS);
        
        // When - Validate compliance with active session
        PrivacyProtectionService.GdprComplianceReport report = 
            privacyService.validateGdprCompliance();
        
        // Then - Should be compliant with active session (within processing time limit)
        assertTrue(report.isZeroPersistenceCompliant());
        assertTrue(report.isDataRetentionCompliant());
        assertTrue(report.isMemoryCleanupEffective());
        // Note: Audit trail may show orphaned session until completion
        
        // Complete processing and validate again
        handle.close();
        
        PrivacyProtectionService.GdprComplianceReport finalReport = 
            privacyService.validateGdprCompliance();
        assertTrue(finalReport.isZeroPersistenceCompliant());
        assertTrue(finalReport.isDataRetentionCompliant());
        assertTrue(finalReport.isAuditTrailComplete());
        assertTrue(finalReport.isMemoryCleanupEffective());
        assertTrue(finalReport.isOverallCompliant());
    }
    
    @Test
    @DisplayName("Should handle multiple concurrent biometric operations")
    void testConcurrentBiometricOperations() throws InterruptedException {
        // Given
        int operationCount = 10;
        Thread[] threads = new Thread[operationCount];
        
        // When
        for (int i = 0; i < operationCount; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                String sessionId = "concurrent-session-" + threadId;
                byte[] imageData = ("concurrent-data-" + threadId).getBytes();
                
                try (PrivacyProtectionService.SecureImageHandle handle = 
                        privacyService.processImageData(sessionId, imageData, 
                            PrivacyProtectionService.BiometricOperation.VERIFY)) {
                    
                    // Simulate processing time
                    Thread.sleep(10);
                    
                } catch (Exception e) {
                    fail("Concurrent operation failed: " + e.getMessage());
                }
            });
        }
        
        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }
        
        // Wait for completion
        for (Thread thread : threads) {
            thread.join(5000);
        }
        
        // Then
        PrivacyProtectionService.PrivacyStats stats = privacyService.getPrivacyStats();
        assertEquals(operationCount, stats.getTotalProcessed());
        assertEquals(operationCount, stats.getTotalCleared());
        assertEquals(0, stats.getActiveSessions());
        assertEquals(100.0, stats.getClearanceRate(), 0.01);
    }
    
    @Test
    @DisplayName("Should handle invalid biometric data appropriately")
    void testInvalidBiometricDataHandling() {
        String sessionId = "invalid-data-session";
        
        // Test null image data
        assertThrows(IllegalArgumentException.class, () -> 
            privacyService.processImageData(sessionId, null, 
                PrivacyProtectionService.BiometricOperation.ENROLL));
        
        // Test empty image data
        assertThrows(IllegalArgumentException.class, () -> 
            privacyService.processImageData(sessionId, new byte[0], 
                PrivacyProtectionService.BiometricOperation.ENROLL));
    }
    
    @Test
    @DisplayName("Should properly clean up on service close")
    void testServiceCleanupOnClose() {
        // Given
        byte[] imageData1 = "cleanup-test-1".getBytes();
        byte[] imageData2 = "cleanup-test-2".getBytes();
        
        privacyService.processImageData("cleanup-session-1", imageData1, 
            PrivacyProtectionService.BiometricOperation.ENROLL);
        privacyService.processImageData("cleanup-session-2", imageData2, 
            PrivacyProtectionService.BiometricOperation.VERIFY);
        
        PrivacyProtectionService.PrivacyStats statsBefore = privacyService.getPrivacyStats();
        assertTrue(statsBefore.getActiveSessions() > 0);
        
        // When
        privacyService.close();
        
        // Then
        PrivacyProtectionService.PrivacyStats statsAfter = privacyService.getPrivacyStats();
        assertEquals(0, statsAfter.getActiveSessions());
        assertEquals(0, statsAfter.getMemoryStats().getActiveBuffers());
    }
    
    @Test
    @DisplayName("Should validate biometric operation types")
    void testBiometricOperationTypes() {
        // Test all operation types
        PrivacyProtectionService.BiometricOperation[] operations = {
            PrivacyProtectionService.BiometricOperation.ENROLL,
            PrivacyProtectionService.BiometricOperation.VERIFY,
            PrivacyProtectionService.BiometricOperation.LIVENESS,
            PrivacyProtectionService.BiometricOperation.DELETE
        };
        
        for (int i = 0; i < operations.length; i++) {
            PrivacyProtectionService.BiometricOperation operation = operations[i];
            String sessionId = "operation-test-" + i;
            byte[] imageData = ("test-data-" + i).getBytes();
            
            try (PrivacyProtectionService.SecureImageHandle handle = 
                    privacyService.processImageData(sessionId, imageData, operation)) {
                
                assertEquals(operation, handle.getOperation());
                assertNotNull(operation.getDescription());
            }
        }
    }
}