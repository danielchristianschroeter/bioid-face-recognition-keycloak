package com.bioid.keycloak.client.security.validation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Specialized validator for biometric image data.
 * 
 * Features:
 * - Image size validation
 * - Image format validation
 * - Security checks for malicious content
 */
public class ImageValidator {
    
    private static final Logger logger = LoggerFactory.getLogger(ImageValidator.class);
    
    // Image data validation constants
    private static final int MAX_IMAGE_SIZE_BYTES = 10 * 1024 * 1024; // 10MB
    private static final int MIN_IMAGE_SIZE_BYTES = 1024; // 1KB
    private static final int MIN_HEADER_SIZE = 8;
    
    // Supported image format headers
    private static final byte[] JPEG_HEADER = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_HEADER = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] WEBP_RIFF = {0x52, 0x49, 0x46, 0x46}; // "RIFF"
    private static final byte[] WEBP_WEBP = {0x57, 0x45, 0x42, 0x50}; // "WEBP"
    
    /**
     * Validates biometric image data for security compliance.
     * 
     * @param imageData image data to validate
     * @throws SecurityException if validation fails
     */
    public void validateImageData(byte[] imageData) throws SecurityException {
        validateImageNotNull(imageData);
        validateImageSize(imageData);
        validateImageFormat(imageData);
        validateImageSecurity(imageData);
    }
    
    /**
     * Validates that image data is not null or empty.
     */
    private void validateImageNotNull(byte[] imageData) throws SecurityException {
        if (imageData == null) {
            throw new SecurityException("Image data cannot be null");
        }
        
        if (imageData.length == 0) {
            throw new SecurityException("Image data cannot be empty");
        }
    }
    
    /**
     * Validates image size constraints.
     */
    private void validateImageSize(byte[] imageData) throws SecurityException {
        if (imageData.length < MIN_IMAGE_SIZE_BYTES) {
            logger.warn("Image data too small: {} bytes (minimum: {} bytes)", 
                imageData.length, MIN_IMAGE_SIZE_BYTES);
            throw new SecurityException("Image data too small to be valid");
        }
       
        if (imageData.length > MAX_IMAGE_SIZE_BYTES) {
            logger.warn("Image data too large: {} bytes (maximum: {} bytes)", 
                imageData.length, MAX_IMAGE_SIZE_BYTES);
            throw new SecurityException("Image data exceeds maximum allowed size");
        }
    }
    
    /**
     * Validates image format by checking headers.
     */
    private void validateImageFormat(byte[] imageData) throws SecurityException {
        if (!isValidImageFormat(imageData)) {
            logger.warn("Invalid image format detected - no recognized header found");
            throw new SecurityException("Image data does not appear to be a valid image format");
        }
    }
    
    /**
     * Performs additional security checks on image data.
     */
    private void validateImageSecurity(byte[] imageData) throws SecurityException {
        // Check for embedded scripts or suspicious patterns
        if (containsSuspiciousContent(imageData)) {
            logger.warn("Suspicious content detected in image data");
            throw new SecurityException("Image data contains potentially malicious content");
        }
        
        // Validate image structure integrity
        if (!hasValidImageStructure(imageData)) {
            logger.warn("Image structure validation failed");
            throw new SecurityException("Image data structure appears to be corrupted or malicious");
        }
    }
    
    /**
     * Checks if image data has a valid format header.
     */
    private boolean isValidImageFormat(byte[] imageData) {
        if (imageData.length < MIN_HEADER_SIZE) {
            return false;
        }
        
        return isJpegFormat(imageData) || isPngFormat(imageData) || isWebpFormat(imageData);
    }
    
    /**
     * Checks if image is JPEG format.
     */
    private boolean isJpegFormat(byte[] imageData) {
        if (imageData.length < JPEG_HEADER.length) {
            return false;
        }
        
        for (int i = 0; i < JPEG_HEADER.length; i++) {
            if (imageData[i] != JPEG_HEADER[i]) {
                return false;
            }
        }
        
        return true;
    } 
   
    /**
     * Checks if image is PNG format.
     */
    private boolean isPngFormat(byte[] imageData) {
        if (imageData.length < PNG_HEADER.length) {
            return false;
        }
        
        for (int i = 0; i < PNG_HEADER.length; i++) {
            if (imageData[i] != PNG_HEADER[i]) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Checks if image is WebP format.
     */
    private boolean isWebpFormat(byte[] imageData) {
        if (imageData.length < 12) {
            return false;
        }
        
        // Check RIFF header
        for (int i = 0; i < WEBP_RIFF.length; i++) {
            if (imageData[i] != WEBP_RIFF[i]) {
                return false;
            }
        }
        
        // Check WEBP identifier at offset 8
        for (int i = 0; i < WEBP_WEBP.length; i++) {
            if (imageData[8 + i] != WEBP_WEBP[i]) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Checks for suspicious content that might indicate malicious payload.
     */
    private boolean containsSuspiciousContent(byte[] imageData) {
        // Convert to string for pattern matching (only check first 1KB for performance)
        int checkLength = Math.min(imageData.length, 1024);
        String content = new String(imageData, 0, checkLength).toLowerCase();
        
        // Check for common script patterns
        String[] suspiciousPatterns = {
            "<script", "javascript:", "vbscript:", "onload=", "onerror=",
            "eval(", "document.", "window.", "alert(", "prompt(",
            "<?php", "<%", "<jsp:", "${{", "#{"
        };
        
        for (String pattern : suspiciousPatterns) {
            if (content.contains(pattern)) {
                return true;
            }
        }
        
        return false;
    }    
    
/**
     * Validates basic image structure integrity.
     */
    private boolean hasValidImageStructure(byte[] imageData) {
        if (isJpegFormat(imageData)) {
            return validateJpegStructure(imageData);
        } else if (isPngFormat(imageData)) {
            return validatePngStructure(imageData);
        } else if (isWebpFormat(imageData)) {
            return validateWebpStructure(imageData);
        }
        
        return false;
    }
    
    /**
     * Basic JPEG structure validation.
     */
    private boolean validateJpegStructure(byte[] imageData) {
        // Check for JPEG end marker (FF D9)
        if (imageData.length < 4) {
            return false;
        }
        
        // Look for end of image marker in last few bytes
        for (int i = imageData.length - 10; i < imageData.length - 1; i++) {
            if (i >= 0 && imageData[i] == (byte) 0xFF && imageData[i + 1] == (byte) 0xD9) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Basic PNG structure validation.
     */
    private boolean validatePngStructure(byte[] imageData) {
        // Check for IEND chunk at the end
        if (imageData.length < 12) {
            return false;
        }
        
        // Look for IEND chunk in last 20 bytes
        String endContent = new String(imageData, Math.max(0, imageData.length - 20), 
            Math.min(20, imageData.length));
        
        return endContent.contains("IEND");
    }
    
    /**
     * Basic WebP structure validation.
     */
    private boolean validateWebpStructure(byte[] imageData) {
        // Basic validation - check that file size matches RIFF chunk size
        if (imageData.length < 12) {
            return false;
        }
        
        // Extract file size from RIFF header (bytes 4-7, little endian)
        int declaredSize = (imageData[4] & 0xFF) |
                          ((imageData[5] & 0xFF) << 8) |
                          ((imageData[6] & 0xFF) << 16) |
                          ((imageData[7] & 0xFF) << 24);
        
        // RIFF chunk size should be file size - 8
        int expectedSize = imageData.length - 8;
        
        // Allow some tolerance for padding
        return Math.abs(declaredSize - expectedSize) <= 4;
    }    
  
  /**
     * Gets supported image formats.
     */
    public String[] getSupportedFormats() {
        return new String[]{"JPEG", "PNG", "WebP"};
    }
    
    /**
     * Gets maximum allowed image size.
     */
    public int getMaxImageSize() {
        return MAX_IMAGE_SIZE_BYTES;
    }
    
    /**
     * Gets minimum allowed image size.
     */
    public int getMinImageSize() {
        return MIN_IMAGE_SIZE_BYTES;
    }
}