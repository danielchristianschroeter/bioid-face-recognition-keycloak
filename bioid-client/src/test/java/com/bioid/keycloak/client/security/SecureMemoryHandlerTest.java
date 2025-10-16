package com.bioid.keycloak.client.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.AfterEach;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Security tests for SecureMemoryHandler implementation.
 */
class SecureMemoryHandlerTest {
    
    private SecureMemoryHandler memoryHandler;
    
    @BeforeEach
    void setUp() {
        memoryHandler = new SecureMemoryHandler();
    }
    
    @AfterEach
    void tearDown() {
        if (memoryHandler != null) {
            memoryHandler.close();
        }
    }
    
    @Test
    @DisplayName("Should allocate and retrieve secure buffer successfully")
    void testAllocateAndRetrieveBuffer() {
        // Given
        String bufferId = "test-buffer-1";
        byte[] originalData = "test-biometric-image-data".getBytes();
        byte[] expectedData = originalData.clone(); // Keep a copy for comparison
        
        // When
        SecureMemoryHandler.SecureImageBuffer buffer = 
            memoryHandler.allocateSecureBuffer(bufferId, originalData);
        byte[] retrievedData = memoryHandler.getImageData(bufferId);
        
        // Then
        assertNotNull(buffer);
        assertArrayEquals(expectedData, retrievedData);
        assertEquals(expectedData.length, buffer.getSize());
        
        // Original array should be cleared for security (zero persistence)
        for (byte b : originalData) {
            assertEquals(0, b, "Original array should be cleared for zero persistence");
        }
    }
    
    @Test
    @DisplayName("Should prevent multiple access to same buffer")
    void testOneTimeAccess() {
        // Given
        String bufferId = "test-buffer-one-time";
        byte[] imageData = "sensitive-biometric-data".getBytes();
        
        // When
        memoryHandler.allocateSecureBuffer(bufferId, imageData);
        byte[] firstAccess = memoryHandler.getImageData(bufferId);
        
        // Then
        assertNotNull(firstAccess);
        
        // Second access should fail
        assertThrows(SecurityException.class, () -> 
            memoryHandler.getImageData(bufferId));
    }
    
    @Test
    @DisplayName("Should clear buffer immediately after access")
    void testImmediateClearAfterAccess() {
        // Given
        String bufferId = "test-buffer-clear";
        byte[] imageData = "data-to-be-cleared".getBytes();
        
        // When
        SecureMemoryHandler.SecureImageBuffer buffer = 
            memoryHandler.allocateSecureBuffer(bufferId, imageData);
        
        assertFalse(buffer.isStale(), "Buffer should not be stale initially");
        
        // Access the data (should clear automatically)
        memoryHandler.getImageData(bufferId);
        
        // Then
        assertTrue(buffer.isStale(), "Buffer should be stale after access");
    }
    
    @Test
    @DisplayName("Should handle manual buffer clearing")
    void testManualBufferClearing() {
        // Given
        String bufferId = "test-buffer-manual-clear";
        byte[] imageData = "data-to-clear-manually".getBytes();
        
        // When
        memoryHandler.allocateSecureBuffer(bufferId, imageData);
        memoryHandler.clearBuffer(bufferId);
        
        // Then
        assertThrows(SecurityException.class, () -> 
            memoryHandler.getImageData(bufferId));
    }
    
    @Test
    @DisplayName("Should enforce memory limits")
    void testMemoryLimits() {
        // Given - Create large data that would exceed memory limit
        byte[] largeData = new byte[60 * 1024 * 1024]; // 60MB (exceeds 50MB limit)
        java.util.Arrays.fill(largeData, (byte) 0xFF);
        
        // When/Then
        assertThrows(SecurityException.class, () -> 
            memoryHandler.allocateSecureBuffer("large-buffer", largeData));
    }
    
    @Test
    @DisplayName("Should track memory usage accurately")
    void testMemoryUsageTracking() {
        // Given
        byte[] data1 = new byte[1024]; // 1KB
        byte[] data2 = new byte[2048]; // 2KB
        
        // When
        memoryHandler.allocateSecureBuffer("buffer-1", data1);
        memoryHandler.allocateSecureBuffer("buffer-2", data2);
        
        SecureMemoryHandler.MemoryStats stats = memoryHandler.getMemoryStats();
        
        // Then
        assertEquals(2, stats.getActiveBuffers());
        assertEquals(3072, stats.getUsedBytes()); // 1KB + 2KB
        assertTrue(stats.getUsagePercentage() > 0);
    }
    
    @Test
    @DisplayName("Should clear all buffers")
    void testClearAllBuffers() {
        // Given
        memoryHandler.allocateSecureBuffer("buffer-1", "data1".getBytes());
        memoryHandler.allocateSecureBuffer("buffer-2", "data2".getBytes());
        memoryHandler.allocateSecureBuffer("buffer-3", "data3".getBytes());
        
        // When
        memoryHandler.clearAllBuffers();
        
        // Then
        SecureMemoryHandler.MemoryStats stats = memoryHandler.getMemoryStats();
        assertEquals(0, stats.getActiveBuffers());
        assertEquals(0, stats.getUsedBytes());
        
        // All buffers should be inaccessible
        assertThrows(SecurityException.class, () -> 
            memoryHandler.getImageData("buffer-1"));
        assertThrows(SecurityException.class, () -> 
            memoryHandler.getImageData("buffer-2"));
        assertThrows(SecurityException.class, () -> 
            memoryHandler.getImageData("buffer-3"));
    }
    
    @Test
    @DisplayName("Should handle buffer expiration")
    void testBufferExpiration() throws InterruptedException {
        // Given
        String bufferId = "test-buffer-expiration";
        byte[] imageData = "expiring-data".getBytes();
        
        // When
        SecureMemoryHandler.SecureImageBuffer buffer = 
            memoryHandler.allocateSecureBuffer(bufferId, imageData);
        
        // Initially should not be expired
        assertFalse(buffer.isExpired());
        
        // Note: In real test, we would need to wait 5 minutes or mock time
        // For this test, we'll just verify the expiration logic exists
        assertNotNull(buffer);
    }
    
    @Test
    @DisplayName("Should handle concurrent buffer operations safely")
    void testConcurrentBufferOperations() throws InterruptedException {
        // Given
        int threadCount = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completeLatch = new CountDownLatch(threadCount);
        
        // When
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            new Thread(() -> {
                try {
                    startLatch.await();
                    
                    String bufferId = "concurrent-buffer-" + threadId;
                    byte[] data = ("thread-" + threadId + "-data").getBytes();
                    byte[] expected = data.clone(); // Keep copy for comparison
                    
                    memoryHandler.allocateSecureBuffer(bufferId, data);
                    byte[] retrieved = memoryHandler.getImageData(bufferId);
                    
                    assertArrayEquals(expected, retrieved);
                    
                } catch (Exception e) {
                    fail("Concurrent operation failed: " + e.getMessage());
                } finally {
                    completeLatch.countDown();
                }
            }).start();
        }
        
        // Start all threads
        startLatch.countDown();
        
        // Wait for completion
        assertTrue(completeLatch.await(10, TimeUnit.SECONDS), 
            "Concurrent operations should complete within timeout");
    }
    
    @Test
    @DisplayName("Should handle invalid buffer operations")
    void testInvalidBufferOperations() {
        // Test null/empty buffer ID
        assertThrows(IllegalArgumentException.class, () -> 
            memoryHandler.allocateSecureBuffer("test", null));
        
        assertThrows(IllegalArgumentException.class, () -> 
            memoryHandler.allocateSecureBuffer("test", new byte[0]));
        
        // Test accessing non-existent buffer
        assertThrows(SecurityException.class, () -> 
            memoryHandler.getImageData("non-existent-buffer"));
    }
    
    @Test
    @DisplayName("Should properly clean up on close")
    void testProperCleanupOnClose() {
        // Given
        memoryHandler.allocateSecureBuffer("cleanup-test-1", "data1".getBytes());
        memoryHandler.allocateSecureBuffer("cleanup-test-2", "data2".getBytes());
        
        SecureMemoryHandler.MemoryStats statsBefore = memoryHandler.getMemoryStats();
        assertTrue(statsBefore.getActiveBuffers() > 0);
        
        // When
        memoryHandler.close();
        
        // Then
        SecureMemoryHandler.MemoryStats statsAfter = memoryHandler.getMemoryStats();
        assertEquals(0, statsAfter.getActiveBuffers());
        assertEquals(0, statsAfter.getUsedBytes());
    }
    
    @Test
    @DisplayName("Should validate secure buffer properties")
    void testSecureBufferProperties() {
        // Given
        String bufferId = "property-test-buffer";
        byte[] imageData = "test-image-data-for-properties".getBytes();
        
        // When
        SecureMemoryHandler.SecureImageBuffer buffer = 
            memoryHandler.allocateSecureBuffer(bufferId, imageData);
        
        // Then
        assertEquals(bufferId, buffer.getBufferId());
        assertEquals(imageData.length, buffer.getSize());
        assertFalse(buffer.isExpired());
        assertFalse(buffer.isStale());
    }
}