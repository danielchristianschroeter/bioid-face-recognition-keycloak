package com.bioid.keycloak.client.security.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Security tests for ImageValidator.
 */
class ImageValidatorTest {
    
    private ImageValidator validator;
    
    @BeforeEach
    void setUp() {
        validator = new ImageValidator();
    }
    
    @Test
    @DisplayName("Should accept valid JPEG image")
    void testValidJpegImage() {
        // Given - Valid JPEG header
        byte[] jpegData = new byte[2048];
        jpegData[0] = (byte) 0xFF;
        jpegData[1] = (byte) 0xD8;
        jpegData[2] = (byte) 0xFF;
        // Add JPEG end marker
        jpegData[jpegData.length - 2] = (byte) 0xFF;
        jpegData[jpegData.length - 1] = (byte) 0xD9;
        
        // When & Then - Should not throw exception
        assertDoesNotThrow(() -> validator.validateImageData(jpegData));
    }
    
    @Test
    @DisplayName("Should accept valid PNG image")
    void testValidPngImage() {
        // Given - Valid PNG header
        byte[] pngData = new byte[2048];
        pngData[0] = (byte) 0x89;
        pngData[1] = 0x50;
        pngData[2] = 0x4E;
        pngData[3] = 0x47;
        pngData[4] = 0x0D;
        pngData[5] = 0x0A;
        pngData[6] = 0x1A;
        pngData[7] = 0x0A;
        // Add IEND chunk
        String iend = "IEND";
        System.arraycopy(iend.getBytes(), 0, pngData, pngData.length - 10, 4);
        
        // When & Then - Should not throw exception
        assertDoesNotThrow(() -> validator.validateImageData(pngData));
    }
    
    @Test
    @DisplayName("Should reject null image data")
    void testNullImageData() {
        // When & Then
        SecurityException exception = assertThrows(SecurityException.class, 
            () -> validator.validateImageData(null));
        
        assertEquals("Image data cannot be null", exception.getMessage());
    }
    
    @Test
    @DisplayName("Should reject empty image data")
    void testEmptyImageData() {
        // When & Then
        SecurityException exception = assertThrows(SecurityException.class, 
            () -> validator.validateImageData(new byte[0]));
        
        assertEquals("Image data cannot be empty", exception.getMessage());
    }
    
    @Test
    @DisplayName("Should reject image data that is too small")
    void testImageDataTooSmall() {
        // Given - Image smaller than minimum size
        byte[] smallData = new byte[512]; // Less than 1KB minimum
        
        // When & Then
        SecurityException exception = assertThrows(SecurityException.class, 
            () -> validator.validateImageData(smallData));
        
        assertEquals("Image data too small to be valid", exception.getMessage());
    } 
   
    @Test
    @DisplayName("Should reject image data that is too large")
    void testImageDataTooLarge() {
        // Given - Image larger than maximum size (10MB)
        byte[] largeData = new byte[11 * 1024 * 1024]; // 11MB
        
        // When & Then
        SecurityException exception = assertThrows(SecurityException.class, 
            () -> validator.validateImageData(largeData));
        
        assertEquals("Image data exceeds maximum allowed size", exception.getMessage());
    }
    
    @Test
    @DisplayName("Should reject invalid image format")
    void testInvalidImageFormat() {
        // Given - Data with invalid header
        byte[] invalidData = new byte[2048];
        invalidData[0] = 0x00;
        invalidData[1] = 0x00;
        invalidData[2] = 0x00;
        
        // When & Then
        SecurityException exception = assertThrows(SecurityException.class, 
            () -> validator.validateImageData(invalidData));
        
        assertEquals("Image data does not appear to be a valid image format", exception.getMessage());
    }
    
    @Test
    @DisplayName("Should reject image with suspicious content")
    void testImageWithSuspiciousContent() {
        // Given - JPEG with embedded script
        byte[] suspiciousData = new byte[2048];
        suspiciousData[0] = (byte) 0xFF;
        suspiciousData[1] = (byte) 0xD8;
        suspiciousData[2] = (byte) 0xFF;
        
        // Embed suspicious content
        String script = "<script>alert('xss')</script>";
        System.arraycopy(script.getBytes(), 0, suspiciousData, 100, script.length());
        
        // Add JPEG end marker
        suspiciousData[suspiciousData.length - 2] = (byte) 0xFF;
        suspiciousData[suspiciousData.length - 1] = (byte) 0xD9;
        
        // When & Then
        SecurityException exception = assertThrows(SecurityException.class, 
            () -> validator.validateImageData(suspiciousData));
        
        assertEquals("Image data contains potentially malicious content", exception.getMessage());
    }
    
    @Test
    @DisplayName("Should accept valid WebP image")
    void testValidWebpImage() {
        // Given - Valid WebP header
        byte[] webpData = new byte[2048];
        // RIFF header
        webpData[0] = 0x52; // R
        webpData[1] = 0x49; // I
        webpData[2] = 0x46; // F
        webpData[3] = 0x46; // F
        // File size (little endian) - 2040 bytes
        webpData[4] = (byte) 0xF8;
        webpData[5] = 0x07;
        webpData[6] = 0x00;
        webpData[7] = 0x00;
        // WEBP identifier
        webpData[8] = 0x57;  // W
        webpData[9] = 0x45;  // E
        webpData[10] = 0x42; // B
        webpData[11] = 0x50; // P
        
        // When & Then - Should not throw exception
        assertDoesNotThrow(() -> validator.validateImageData(webpData));
    }
    
    @Test
    @DisplayName("Should provide correct supported formats")
    void testGetSupportedFormats() {
        // When
        String[] formats = validator.getSupportedFormats();
        
        // Then
        assertNotNull(formats);
        assertEquals(3, formats.length);
        assertArrayEquals(new String[]{"JPEG", "PNG", "WebP"}, formats);
    }
    
    @Test
    @DisplayName("Should provide correct size limits")
    void testGetSizeLimits() {
        // When & Then
        assertEquals(10 * 1024 * 1024, validator.getMaxImageSize()); // 10MB
        assertEquals(1024, validator.getMinImageSize()); // 1KB
    }
}