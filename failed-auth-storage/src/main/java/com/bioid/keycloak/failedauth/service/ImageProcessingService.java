package com.bioid.keycloak.failedauth.service;

import com.bioid.keycloak.failedauth.config.FailedAuthConfiguration;
import org.imgscalr.Scalr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Iterator;

/**
 * Service for processing images (thumbnails, compression, format conversion).
 */
public class ImageProcessingService {
    
    private static final Logger logger = LoggerFactory.getLogger(ImageProcessingService.class);
    
    private final FailedAuthConfiguration config;
    
    public ImageProcessingService(FailedAuthConfiguration config) {
        this.config = config;
    }
    
    /**
     * Decode base64 image data to bytes.
     * Handles both raw base64 and data URLs (data:image/jpeg;base64,...)
     * 
     * @param base64Data Base64-encoded image data
     * @return Decoded image bytes
     */
    public byte[] decodeBase64Image(String base64Data) {
        try {
            // Remove data URL prefix if present
            String cleanBase64 = base64Data;
            if (base64Data.contains(",")) {
                cleanBase64 = base64Data.substring(base64Data.indexOf(",") + 1);
            }
            
            return Base64.getDecoder().decode(cleanBase64);
        } catch (Exception e) {
            logger.error("Failed to decode base64 image", e);
            throw new RuntimeException("Invalid base64 image data", e);
        }
    }
    
    /**
     * Create thumbnail from image bytes.
     * 
     * @param imageBytes Original image bytes
     * @return Thumbnail image bytes (JPEG)
     * @throws Exception if thumbnail creation fails
     */
    public byte[] createThumbnail(byte[] imageBytes) throws Exception {
        try {
            int thumbnailSize = config.getThumbnailSize();
            int quality = config.getThumbnailQuality();
            
            // Read image
            BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (originalImage == null) {
                throw new Exception("Failed to read image");
            }
            
            // Create thumbnail using imgscalr (high-quality scaling)
            BufferedImage thumbnail = Scalr.resize(
                originalImage,
                Scalr.Method.QUALITY,
                Scalr.Mode.FIT_TO_WIDTH,
                thumbnailSize,
                thumbnailSize,
                Scalr.OP_ANTIALIAS
            );
            
            // Compress to JPEG
            return compressToJpeg(thumbnail, quality);
            
        } catch (Exception e) {
            logger.error("Failed to create thumbnail", e);
            throw new Exception("Thumbnail creation failed", e);
        }
    }
    
    /**
     * Compress image to JPEG format with specified quality.
     * 
     * @param image The image to compress
     * @param quality Quality (0-100)
     * @return Compressed JPEG bytes
     * @throws Exception if compression fails
     */
    public byte[] compressToJpeg(BufferedImage image, int quality) throws Exception {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            
            // Get JPEG writer
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
            if (!writers.hasNext()) {
                throw new Exception("No JPEG writer available");
            }
            ImageWriter writer = writers.next();
            
            // Set compression quality
            ImageWriteParam writeParam = writer.getDefaultWriteParam();
            writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            writeParam.setCompressionQuality(quality / 100.0f);
            
            // Write image
            try (ImageOutputStream ios = ImageIO.createImageOutputStream(outputStream)) {
                writer.setOutput(ios);
                writer.write(null, new IIOImage(image, null, null), writeParam);
            } finally {
                writer.dispose();
            }
            
            return outputStream.toByteArray();
            
        } catch (Exception e) {
            logger.error("Failed to compress image to JPEG", e);
            throw new Exception("JPEG compression failed", e);
        }
    }
    
    /**
     * Get image dimensions without fully loading the image.
     * 
     * @param imageBytes Image bytes
     * @return Array of [width, height]
     * @throws Exception if reading dimensions fails
     */
    public int[] getImageDimensions(byte[] imageBytes) throws Exception {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (image == null) {
                throw new Exception("Failed to read image");
            }
            return new int[]{image.getWidth(), image.getHeight()};
        } catch (Exception e) {
            logger.error("Failed to get image dimensions", e);
            throw new Exception("Failed to read image dimensions", e);
        }
    }
    
    /**
     * Detect image format from bytes.
     * 
     * @param imageBytes Image bytes
     * @return Format name (e.g., "JPEG", "PNG") or "UNKNOWN"
     */
    public String detectImageFormat(byte[] imageBytes) {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(imageBytes);
            BufferedImage image = ImageIO.read(inputStream);
            if (image == null) {
                return "UNKNOWN";
            }
            
            // Try to detect format from magic bytes
            if (imageBytes.length >= 3) {
                // JPEG: FF D8 FF
                if (imageBytes[0] == (byte) 0xFF && 
                    imageBytes[1] == (byte) 0xD8 && 
                    imageBytes[2] == (byte) 0xFF) {
                    return "JPEG";
                }
                // PNG: 89 50 4E 47
                if (imageBytes[0] == (byte) 0x89 && 
                    imageBytes[1] == (byte) 0x50 && 
                    imageBytes[2] == (byte) 0x4E) {
                    return "PNG";
                }
            }
            
            return "UNKNOWN";
        } catch (Exception e) {
            logger.warn("Failed to detect image format", e);
            return "UNKNOWN";
        }
    }
    
    /**
     * Validate image size is within limits.
     * 
     * @param imageBytes Image bytes
     * @return true if size is acceptable
     */
    public boolean validateImageSize(byte[] imageBytes) {
        int maxSizeBytes = config.getMaxImageSizeMB() * 1024 * 1024;
        return imageBytes.length <= maxSizeBytes;
    }
    
    /**
     * Validate image can be read and processed.
     * 
     * @param imageBytes Image bytes
     * @return true if image is valid
     */
    public boolean validateImage(byte[] imageBytes) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
            return image != null && image.getWidth() > 0 && image.getHeight() > 0;
        } catch (Exception e) {
            logger.warn("Image validation failed", e);
            return false;
        }
    }
    
    /**
     * Convert image to standard format (JPEG) if needed.
     * 
     * @param imageBytes Original image bytes
     * @return Standardized image bytes (JPEG)
     * @throws Exception if conversion fails
     */
    public byte[] standardizeImage(byte[] imageBytes) throws Exception {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (image == null) {
                throw new Exception("Failed to read image");
            }
            
            // Convert to JPEG with good quality
            return compressToJpeg(image, 90);
            
        } catch (Exception e) {
            logger.error("Failed to standardize image", e);
            throw new Exception("Image standardization failed", e);
        }
    }
}
