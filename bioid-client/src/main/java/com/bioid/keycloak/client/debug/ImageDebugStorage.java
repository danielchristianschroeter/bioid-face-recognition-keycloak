package com.bioid.keycloak.client.debug;

import com.bioid.keycloak.client.config.BioIdConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Debug utility for storing images sent to BWS locally for debugging purposes.
 * 
 * <p>WARNING: This stores biometric data locally. Use only in development environments!
 * 
 * <p>Directory structure:
 * {storage-path}/{username}/{operation}/{timestamp}/
 *   - image1.jpg
 *   - image2.jpg (if multiple images)
 *   - metadata.json (if enabled)
 */
public class ImageDebugStorage {

    private static final Logger logger = LoggerFactory.getLogger(ImageDebugStorage.class);
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS").withZone(ZoneId.systemDefault());
    private static final ObjectMapper objectMapper = new ObjectMapper()
        .enable(SerializationFeature.INDENT_OUTPUT);

    private final BioIdConfiguration config;

    public ImageDebugStorage(BioIdConfiguration config) {
        this.config = config;
    }

    /**
     * Saves a single image for debugging.
     *
     * @param username the username
     * @param operation the operation type (enrollment, verification, liveness)
     * @param imageData base64-encoded image data
     * @param metadata additional metadata to store
     */
    public void saveImage(String username, String operation, String imageData, Map<String, Object> metadata) {
        if (!config.isDebugImageStorageEnabled()) {
            return;
        }

        try {
            Path sessionDir = createSessionDirectory(username, operation);
            saveImageFile(sessionDir, "image.jpg", imageData);
            
            if (config.isDebugImageStorageIncludeMetadata()) {
                saveMetadata(sessionDir, metadata);
            }
            
            logger.debug("Saved debug image for user: {}, operation: {}", username, operation);
        } catch (Exception e) {
            logger.warn("Failed to save debug image for user: {}, operation: {}: {}", 
                username, operation, e.getMessage());
        }
    }

    /**
     * Saves multiple images for debugging.
     *
     * @param username the username
     * @param operation the operation type (enrollment, verification, liveness)
     * @param images array of base64-encoded image data
     * @param metadata additional metadata to store
     */
    public void saveImages(String username, String operation, String[] images, Map<String, Object> metadata) {
        if (!config.isDebugImageStorageEnabled()) {
            return;
        }

        try {
            Path sessionDir = createSessionDirectory(username, operation);
            
            for (int i = 0; i < images.length; i++) {
                saveImageFile(sessionDir, String.format("image%d.jpg", i + 1), images[i]);
            }
            
            if (config.isDebugImageStorageIncludeMetadata()) {
                saveMetadata(sessionDir, metadata);
            }
            
            logger.debug("Saved {} debug images for user: {}, operation: {}", 
                images.length, username, operation);
        } catch (Exception e) {
            logger.warn("Failed to save debug images for user: {}, operation: {}: {}", 
                username, operation, e.getMessage());
        }
    }

    /**
     * Creates a session directory for storing images.
     * Structure: {storage-path}/{username}/{operation}/{timestamp}/
     */
    private Path createSessionDirectory(String username, String operation) throws IOException {
        String timestamp = TIMESTAMP_FORMATTER.format(Instant.now());
        String sanitizedUsername = sanitizeFilename(username);
        String sanitizedOperation = sanitizeFilename(operation);
        
        Path sessionDir = Paths.get(
            config.getDebugImageStoragePath(),
            sanitizedUsername,
            sanitizedOperation,
            timestamp
        );
        
        Files.createDirectories(sessionDir);
        return sessionDir;
    }

    /**
     * Saves an image file from base64 data.
     */
    private void saveImageFile(Path directory, String filename, String imageData) throws IOException {
        // Remove data URL prefix if present
        String base64Data = imageData;
        if (imageData.contains(",")) {
            base64Data = imageData.split(",", 2)[1];
        }
        
        byte[] imageBytes = Base64.getDecoder().decode(base64Data);
        Path imagePath = directory.resolve(filename);
        Files.write(imagePath, imageBytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    /**
     * Saves metadata as JSON.
     */
    private void saveMetadata(Path directory, Map<String, Object> metadata) throws IOException {
        // Add timestamp to metadata
        Map<String, Object> enrichedMetadata = new HashMap<>(metadata);
        enrichedMetadata.put("timestamp", Instant.now().toString());
        enrichedMetadata.put("timestampEpoch", System.currentTimeMillis());
        
        Path metadataPath = directory.resolve("metadata.json");
        objectMapper.writeValue(metadataPath.toFile(), enrichedMetadata);
    }

    /**
     * Sanitizes a filename by removing invalid characters.
     */
    private String sanitizeFilename(String filename) {
        if (filename == null) {
            return "unknown";
        }
        // Replace invalid filename characters with underscore
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    /**
     * Creates a metadata map with common fields.
     */
    public static Map<String, Object> createMetadata(String userId, String classId, String operation) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("userId", userId);
        metadata.put("classId", classId);
        metadata.put("operation", operation);
        return metadata;
    }

    /**
     * Adds result information to metadata.
     */
    public static void addResult(Map<String, Object> metadata, boolean success, String message) {
        metadata.put("success", success);
        if (message != null) {
            metadata.put("message", message);
        }
    }
}
