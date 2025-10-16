package com.bioid.keycloak.client.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Secure memory handler for biometric image data with automatic cleanup.
 * 
 * Features:
 * - Immediate disposal of biometric image data after processing
 * - Secure memory clearing with overwrite patterns
 * - Automatic garbage collection of stale references
 * - Memory usage tracking and limits
 * - Zero persistence guarantee for raw biometric data
 */
public class SecureMemoryHandler implements AutoCloseable {
    
    private static final Logger logger = LoggerFactory.getLogger(SecureMemoryHandler.class);
    
    private static final long MAX_MEMORY_USAGE_BYTES = 50 * 1024 * 1024; // 50MB limit
    private static final long CLEANUP_INTERVAL_SECONDS = 30;
    private static final byte[] OVERWRITE_PATTERN = {(byte) 0xFF, (byte) 0x00, (byte) 0xAA, (byte) 0x55};
    
    private final ConcurrentMap<String, SecureImageBuffer> activeBuffers;
    private final AtomicLong totalMemoryUsage;
    private final ScheduledExecutorService cleanupExecutor;
    
    public SecureMemoryHandler() {
        this.activeBuffers = new ConcurrentHashMap<>();
        this.totalMemoryUsage = new AtomicLong(0);
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "secure-memory-cleanup");
            t.setDaemon(true);
            return t;
        });
        
        // Start automatic cleanup
        startAutomaticCleanup();
        
        logger.info("Initialized secure memory handler with {}MB limit", 
            MAX_MEMORY_USAGE_BYTES / (1024 * 1024));
    }
    
    /**
     * Allocates secure buffer for biometric image data.
     * 
     * @param bufferId unique identifier for the buffer
     * @param imageData raw image data to secure
     * @return secure buffer handle
     * @throws SecurityException if memory limit exceeded
     */
    public SecureImageBuffer allocateSecureBuffer(String bufferId, byte[] imageData) 
            throws SecurityException {
        
        if (imageData == null || imageData.length == 0) {
            throw new IllegalArgumentException("Image data cannot be null or empty");
        }
        
        // Check memory limits
        long newMemoryUsage = totalMemoryUsage.get() + imageData.length;
        if (newMemoryUsage > MAX_MEMORY_USAGE_BYTES) {
            // Force cleanup and retry
            performCleanup();
            newMemoryUsage = totalMemoryUsage.get() + imageData.length;
            
            if (newMemoryUsage > MAX_MEMORY_USAGE_BYTES) {
                throw new SecurityException("Memory limit exceeded for biometric data storage");
            }
        }
        
        // Create secure buffer
        SecureImageBuffer buffer = new SecureImageBuffer(bufferId, imageData);
        activeBuffers.put(bufferId, buffer);
        totalMemoryUsage.addAndGet(imageData.length);
        
        logger.debug("Allocated secure buffer: {} ({} bytes)", bufferId, imageData.length);
        return buffer;
    }
    
    /**
     * Retrieves image data from secure buffer (one-time access).
     * 
     * @param bufferId buffer identifier
     * @return image data (caller must clear after use)
     * @throws SecurityException if buffer not found or already accessed
     */
    public byte[] getImageData(String bufferId) throws SecurityException {
        SecureImageBuffer buffer = activeBuffers.get(bufferId);
        if (buffer == null) {
            throw new SecurityException("Secure buffer not found: " + bufferId);
        }
        
        return buffer.getImageData();
    }
    
    /**
     * Immediately clears and removes secure buffer.
     * 
     * @param bufferId buffer to clear
     */
    public void clearBuffer(String bufferId) {
        SecureImageBuffer buffer = activeBuffers.remove(bufferId);
        if (buffer != null) {
            long bufferSize = buffer.getSize();
            buffer.clear();
            totalMemoryUsage.addAndGet(-bufferSize);
            
            logger.debug("Cleared secure buffer: {} ({} bytes)", bufferId, bufferSize);
        }
    }
    
    /**
     * Clears all active buffers immediately.
     */
    public void clearAllBuffers() {
        int clearedCount = 0;
        long clearedBytes = 0;
        
        for (SecureImageBuffer buffer : activeBuffers.values()) {
            clearedBytes += buffer.getSize();
            buffer.clear();
            clearedCount++;
        }
        
        activeBuffers.clear();
        totalMemoryUsage.set(0);
        
        logger.info("Cleared {} secure buffers ({} bytes total)", clearedCount, clearedBytes);
    }
    
    /**
     * Gets current memory usage statistics.
     */
    public MemoryStats getMemoryStats() {
        return new MemoryStats(
            activeBuffers.size(),
            totalMemoryUsage.get(),
            MAX_MEMORY_USAGE_BYTES
        );
    }
    
    /**
     * Starts automatic cleanup of stale buffers.
     */
    private void startAutomaticCleanup() {
        cleanupExecutor.scheduleWithFixedDelay(
            this::performCleanup,
            CLEANUP_INTERVAL_SECONDS,
            CLEANUP_INTERVAL_SECONDS,
            TimeUnit.SECONDS
        );
    }
    
    /**
     * Securely clears array with multiple overwrite passes.
     */
    private static void secureArrayClear(byte[] array) {
        if (array == null) {
            return;
        }
        
        // Multiple pass clearing with different patterns for security
        for (int pass = 0; pass < 3; pass++) {
            byte pattern = OVERWRITE_PATTERN[pass % OVERWRITE_PATTERN.length];
            java.util.Arrays.fill(array, pattern);
        }
        
        // Final pass with zeros
        java.util.Arrays.fill(array, (byte) 0);
    }
    
    /**
     * Performs cleanup of stale and expired buffers.
     */
    private void performCleanup() {
        int cleanedCount = 0;
        long cleanedBytes = 0;
        
        for (var entry : activeBuffers.entrySet()) {
            SecureImageBuffer buffer = entry.getValue();
            
            // Clean up expired or stale buffers
            if (buffer.isExpired() || buffer.isStale()) {
                activeBuffers.remove(entry.getKey());
                cleanedBytes += buffer.getSize();
                buffer.clear();
                cleanedCount++;
            }
        }
        
        if (cleanedCount > 0) {
            totalMemoryUsage.addAndGet(-cleanedBytes);
            logger.debug("Automatic cleanup: {} buffers ({} bytes)", cleanedCount, cleanedBytes);
        }
        
        // Force garbage collection if memory usage is high
        if (totalMemoryUsage.get() > MAX_MEMORY_USAGE_BYTES * 0.8) {
            System.gc();
            logger.debug("Triggered garbage collection due to high memory usage");
        }
    }
    
    @Override
    public void close() {
        logger.info("Shutting down secure memory handler");
        
        // Clear all buffers
        clearAllBuffers();
        
        // Shutdown cleanup executor
        cleanupExecutor.shutdown();
        try {
            if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        logger.info("Secure memory handler shutdown complete");
    }
    
    /**
     * Secure buffer for biometric image data with automatic expiration.
     */
    public static class SecureImageBuffer {
        private final String bufferId;
        private ByteBuffer secureBuffer;
        private final long createdAt;
        private final long size;
        private volatile boolean accessed = false;
        private volatile boolean cleared = false;
        
        // Buffer expires after 5 minutes
        private static final long EXPIRATION_TIME_MS = 5 * 60 * 1000;
        
        public SecureImageBuffer(String bufferId, byte[] imageData) {
            this.bufferId = bufferId;
            this.size = imageData.length;
            this.createdAt = System.currentTimeMillis();
            
            // Allocate direct buffer for better security
            this.secureBuffer = ByteBuffer.allocateDirect(imageData.length);
            this.secureBuffer.put(imageData);
            this.secureBuffer.flip();
            
            // Securely clear original array with multiple passes
            secureArrayClear(imageData);
        }
        
        /**
         * Gets image data from buffer (one-time access only).
         */
        public synchronized byte[] getImageData() throws SecurityException {
            if (cleared) {
                throw new SecurityException("Buffer already cleared: " + bufferId);
            }
            
            if (accessed) {
                throw new SecurityException("Buffer already accessed (one-time use): " + bufferId);
            }
            
            if (isExpired()) {
                clear();
                throw new SecurityException("Buffer expired: " + bufferId);
            }
            
            accessed = true;
            
            byte[] data = new byte[secureBuffer.remaining()];
            secureBuffer.get(data);
            
            // Immediately clear buffer after access
            clear();
            
            return data;
        }
        
        /**
         * Securely clears buffer with overwrite patterns.
         */
        public synchronized void clear() {
            if (cleared) {
                return;
            }
            
            if (secureBuffer != null) {
                // Overwrite with multiple patterns for security
                secureBuffer.rewind();
                
                for (int pass = 0; pass < 3; pass++) {
                    secureBuffer.rewind();
                    byte pattern = OVERWRITE_PATTERN[pass % OVERWRITE_PATTERN.length];
                    
                    while (secureBuffer.hasRemaining()) {
                        secureBuffer.put(pattern);
                    }
                }
                
                secureBuffer = null;
            }
            
            cleared = true;
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() - createdAt > EXPIRATION_TIME_MS;
        }
        
        public boolean isStale() {
            return accessed || cleared;
        }
        
        public long getSize() {
            return size;
        }
        
        public String getBufferId() {
            return bufferId;
        }
    }
    
    /**
     * Memory usage statistics.
     */
    public static class MemoryStats {
        private final int activeBuffers;
        private final long usedBytes;
        private final long maxBytes;
        
        public MemoryStats(int activeBuffers, long usedBytes, long maxBytes) {
            this.activeBuffers = activeBuffers;
            this.usedBytes = usedBytes;
            this.maxBytes = maxBytes;
        }
        
        public int getActiveBuffers() { return activeBuffers; }
        public long getUsedBytes() { return usedBytes; }
        public long getMaxBytes() { return maxBytes; }
        public double getUsagePercentage() { return (double) usedBytes / maxBytes * 100; }
        
        @Override
        public String toString() {
            return String.format("MemoryStats{buffers=%d, used=%d bytes (%.1f%%), max=%d bytes}",
                activeBuffers, usedBytes, getUsagePercentage(), maxBytes);
        }
    }
}