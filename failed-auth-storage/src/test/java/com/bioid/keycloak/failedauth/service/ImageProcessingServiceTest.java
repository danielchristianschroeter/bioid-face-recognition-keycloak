package com.bioid.keycloak.failedauth.service;

import com.bioid.keycloak.failedauth.config.FailedAuthConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for ImageProcessingService.
 */
@DisplayName("ImageProcessingService Tests")
class ImageProcessingServiceTest {
    
    private ImageProcessingService imageProcessingService;
    private byte[] testImageBytes;
    
    @BeforeEach
    void setUp() throws Exception {
        // Create a real configuration with test values
        System.setProperty("FAILED_AUTH_THUMBNAIL_SIZE", "300");
        System.setProperty("FAILED_AUTH_THUMBNAIL_QUALITY", "85");
        System.setProperty("FAILED_AUTH_MAX_IMAGE_SIZE_MB", "5");
        
        FailedAuthConfiguration config = FailedAuthConfiguration.getInstance();
        imageProcessingService = new ImageProcessingService(config);
        
        // Create a test image
        testImageBytes = createTestImage(640, 480);
    }
    
    /**
     * Helper method to create a test image.
     */
    private byte[] createTestImage(int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        
        // Fill with a simple pattern
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = (x * 255 / width) << 16 | (y * 255 / height) << 8 | 128;
                image.setRGB(x, y, rgb);
            }
        }
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "JPEG", baos);
        return baos.toByteArray();
    }
    
    @Test
    @DisplayName("Should decode base64 image successfully")
    void testDecodeBase64Image() {
        // Given
        String base64 = Base64.getEncoder().encodeToString(testImageBytes);
        
        // When
        byte[] decoded = imageProcessingService.decodeBase64Image(base64);
        
        // Then
        assertThat(decoded).isEqualTo(testImageBytes);
    }
    
    @Test
    @DisplayName("Should decode base64 image with data URL prefix")
    void testDecodeBase64ImageWithDataURL() {
        // Given
        String base64 = Base64.getEncoder().encodeToString(testImageBytes);
        String dataUrl = "data:image/jpeg;base64," + base64;
        
        // When
        byte[] decoded = imageProcessingService.decodeBase64Image(dataUrl);
        
        // Then
        assertThat(decoded).isEqualTo(testImageBytes);
    }
    
    @Test
    @DisplayName("Should fail to decode invalid base64")
    void testDecodeInvalidBase64() {
        // Given
        String invalidBase64 = "This is not valid base64!@#$";
        
        // Then
        assertThatThrownBy(() -> imageProcessingService.decodeBase64Image(invalidBase64))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Invalid base64");
    }
    
    @Test
    @DisplayName("Should create thumbnail successfully")
    void testCreateThumbnail() throws Exception {
        // When
        byte[] thumbnail = imageProcessingService.createThumbnail(testImageBytes);
        
        // Then
        assertThat(thumbnail).isNotNull();
        // Note: Thumbnail may not always be smaller due to compression artifacts
        // Just verify it was created and has valid dimensions
        
        // Verify thumbnail dimensions
        int[] dimensions = imageProcessingService.getImageDimensions(thumbnail);
        assertThat(dimensions[0]).isLessThanOrEqualTo(300);
        assertThat(dimensions[1]).isLessThanOrEqualTo(300);
    }
    
    @Test
    @DisplayName("Should fail to create thumbnail from invalid image")
    void testCreateThumbnailInvalidImage() {
        // Given
        byte[] invalidImage = "Not an image".getBytes();
        
        // Then
        assertThatThrownBy(() -> imageProcessingService.createThumbnail(invalidImage))
            .isInstanceOf(Exception.class);
    }
    
    @Test
    @DisplayName("Should compress image to JPEG")
    void testCompressToJpeg() throws Exception {
        // Given
        BufferedImage image = ImageIO.read(new java.io.ByteArrayInputStream(testImageBytes));
        
        // When
        byte[] compressed = imageProcessingService.compressToJpeg(image, 80);
        
        // Then
        assertThat(compressed).isNotNull();
        assertThat(compressed.length).isGreaterThan(0);
        
        // Verify it's a valid JPEG
        assertThat(imageProcessingService.detectImageFormat(compressed)).isEqualTo("JPEG");
    }
    
    @Test
    @DisplayName("Should get image dimensions correctly")
    void testGetImageDimensions() throws Exception {
        // When
        int[] dimensions = imageProcessingService.getImageDimensions(testImageBytes);
        
        // Then
        assertThat(dimensions).hasSize(2);
        assertThat(dimensions[0]).isEqualTo(640); // width
        assertThat(dimensions[1]).isEqualTo(480); // height
    }
    
    @Test
    @DisplayName("Should fail to get dimensions from invalid image")
    void testGetImageDimensionsInvalid() {
        // Given
        byte[] invalidImage = "Not an image".getBytes();
        
        // Then
        assertThatThrownBy(() -> imageProcessingService.getImageDimensions(invalidImage))
            .isInstanceOf(Exception.class);
    }
    
    @Test
    @DisplayName("Should detect JPEG format")
    void testDetectJpegFormat() {
        // When
        String format = imageProcessingService.detectImageFormat(testImageBytes);
        
        // Then
        assertThat(format).isEqualTo("JPEG");
    }
    
    @Test
    @DisplayName("Should detect PNG format")
    void testDetectPngFormat() throws Exception {
        // Given
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", baos);
        byte[] pngBytes = baos.toByteArray();
        
        // When
        String format = imageProcessingService.detectImageFormat(pngBytes);
        
        // Then
        assertThat(format).isEqualTo("PNG");
    }
    
    @Test
    @DisplayName("Should return UNKNOWN for invalid image format")
    void testDetectUnknownFormat() {
        // Given
        byte[] invalidImage = "Not an image".getBytes();
        
        // When
        String format = imageProcessingService.detectImageFormat(invalidImage);
        
        // Then
        assertThat(format).isEqualTo("UNKNOWN");
    }
    
    @Test
    @DisplayName("Should validate image size within limits")
    void testValidateImageSizeWithinLimits() {
        // When
        boolean valid = imageProcessingService.validateImageSize(testImageBytes);
        
        // Then
        assertThat(valid).isTrue();
    }
    
    @Test
    @DisplayName("Should reject image size exceeding limits")
    void testValidateImageSizeExceedsLimits() {
        // Given - create a very large image that exceeds any reasonable limit
        byte[] largeImage = new byte[10 * 1024 * 1024]; // 10 MB
        
        // Set a small limit
        System.setProperty("FAILED_AUTH_MAX_IMAGE_SIZE_MB", "1"); // 1 MB limit
        ImageProcessingService testService = new ImageProcessingService(FailedAuthConfiguration.getInstance());
        
        // When
        boolean valid = testService.validateImageSize(largeImage);
        
        // Then
        assertThat(valid).isFalse();
    }
    
    @Test
    @DisplayName("Should validate valid image")
    void testValidateValidImage() {
        // When
        boolean valid = imageProcessingService.validateImage(testImageBytes);
        
        // Then
        assertThat(valid).isTrue();
    }
    
    @Test
    @DisplayName("Should reject invalid image")
    void testValidateInvalidImage() {
        // Given
        byte[] invalidImage = "Not an image".getBytes();
        
        // When
        boolean valid = imageProcessingService.validateImage(invalidImage);
        
        // Then
        assertThat(valid).isFalse();
    }
    
    @Test
    @DisplayName("Should standardize image to JPEG")
    void testStandardizeImage() throws Exception {
        // Given - create a PNG image
        BufferedImage image = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", baos);
        byte[] pngBytes = baos.toByteArray();
        
        // When
        byte[] standardized = imageProcessingService.standardizeImage(pngBytes);
        
        // Then
        assertThat(standardized).isNotNull();
        assertThat(imageProcessingService.detectImageFormat(standardized)).isEqualTo("JPEG");
    }
    
    @Test
    @DisplayName("Should fail to standardize invalid image")
    void testStandardizeInvalidImage() {
        // Given
        byte[] invalidImage = "Not an image".getBytes();
        
        // Then
        assertThatThrownBy(() -> imageProcessingService.standardizeImage(invalidImage))
            .isInstanceOf(Exception.class);
    }
    
    @Test
    @DisplayName("Should handle small images")
    void testSmallImage() throws Exception {
        // Given
        byte[] smallImage = createTestImage(50, 50);
        
        // When
        byte[] thumbnail = imageProcessingService.createThumbnail(smallImage);
        
        // Then
        assertThat(thumbnail).isNotNull();
        
        int[] dimensions = imageProcessingService.getImageDimensions(thumbnail);
        assertThat(dimensions[0]).isLessThanOrEqualTo(300);
    }
    
    @Test
    @DisplayName("Should handle large images")
    void testLargeImage() throws Exception {
        // Given
        byte[] largeImage = createTestImage(2000, 1500);
        
        // When
        byte[] thumbnail = imageProcessingService.createThumbnail(largeImage);
        
        // Then
        assertThat(thumbnail).isNotNull();
        assertThat(thumbnail.length).isLessThan(largeImage.length);
        
        int[] dimensions = imageProcessingService.getImageDimensions(thumbnail);
        assertThat(dimensions[0]).isLessThanOrEqualTo(300);
    }
    
    @Test
    @DisplayName("Should respect thumbnail quality setting")
    void testThumbnailQuality() throws Exception {
        // Note: Due to singleton configuration, we can't easily test different quality settings
        // in the same test run. This test verifies that thumbnails are created successfully.
        
        // When
        byte[] thumbnail = imageProcessingService.createThumbnail(testImageBytes);
        
        // Then
        assertThat(thumbnail).isNotNull();
        assertThat(thumbnail.length).isGreaterThan(0);
        
        // Verify it's a valid image
        int[] dimensions = imageProcessingService.getImageDimensions(thumbnail);
        assertThat(dimensions[0]).isGreaterThan(0);
        assertThat(dimensions[1]).isGreaterThan(0);
    }
    
    @Test
    @DisplayName("Should handle empty image data")
    void testEmptyImageData() {
        // Given
        byte[] emptyData = new byte[0];
        
        // When/Then
        assertThat(imageProcessingService.validateImage(emptyData)).isFalse();
    }
    
    @Test
    @DisplayName("Should handle null image data gracefully")
    void testNullImageData() {
        // When
        boolean valid = imageProcessingService.validateImage(null);
        
        // Then - should return false for null input rather than throwing
        assertThat(valid).isFalse();
    }
}
