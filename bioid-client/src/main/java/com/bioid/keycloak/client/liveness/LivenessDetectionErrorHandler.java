package com.bioid.keycloak.client.liveness;

import com.bioid.keycloak.client.exception.BioIdException;
import com.bioid.keycloak.client.exception.BioIdServiceException;
import com.bioid.keycloak.client.exception.BioIdValidationException;
import io.grpc.StatusRuntimeException;

import java.util.List;

/**
 * Error handler for liveness detection operations.
 * Provides validation and error handling utilities.
 */
public class LivenessDetectionErrorHandler {

    /**
     * Validates liveness detection request parameters.
     */
    public static void validateLivenessRequest(List<byte[]> images, LivenessDetectionClient.LivenessMode mode) 
            throws BioIdValidationException {
        if (images == null || images.isEmpty()) {
            throw new BioIdValidationException("No images provided");
        }
        if (images.size() > 2) {
            throw new BioIdValidationException("Maximum 2 images allowed");
        }
        
        // Validate each image
        for (int i = 0; i < images.size(); i++) {
            validateImageData(images.get(i), "Image " + (i + 1));
        }
        
        // Mode-specific validation
        if (mode == LivenessDetectionClient.LivenessMode.PASSIVE && images.size() != 1) {
            throw new BioIdValidationException("Passive liveness detection requires exactly 1 image");
        }
        if (mode == LivenessDetectionClient.LivenessMode.ACTIVE && images.size() != 2) {
            throw new BioIdValidationException("ACTIVE liveness detection requires exactly 2 images");
        }
        if (mode == LivenessDetectionClient.LivenessMode.CHALLENGE_RESPONSE && images.size() != 2) {
            throw new BioIdValidationException("CHALLENGE_RESPONSE liveness detection requires exactly 2 images");
        }
    }
    
    /**
     * Validates image data.
     */
    private static void validateImageData(byte[] imageData, String imageName) throws BioIdValidationException {
        if (imageData == null || imageData.length == 0) {
            throw new BioIdValidationException(imageName + " is null or empty");
        }
        
        // Minimum size check (1KB)
        if (imageData.length < 1024) {
            throw new BioIdValidationException(imageName + " is too small (minimum 1KB required)");
        }
        
        // Maximum size check (10MB)
        if (imageData.length > 10 * 1024 * 1024) {
            throw new BioIdValidationException(imageName + " is too large (maximum 10MB allowed)");
        }
        
        // Basic format validation - check for common image format magic bytes
        if (!isValidImageFormat(imageData)) {
            throw new BioIdValidationException(imageName + " has unsupported format (supported: JPEG, PNG)");
        }
    }
    
    /**
     * Checks if the image data has valid format magic bytes.
     * BWS supports JPEG and PNG formats only.
     */
    private static boolean isValidImageFormat(byte[] data) {
        if (data.length < 4) {
            return false;
        }
        
        // JPEG: FF D8 FF
        if (data[0] == (byte) 0xFF && data[1] == (byte) 0xD8 && data[2] == (byte) 0xFF) {
            return true;
        }
        
        // PNG: 89 50 4E 47
        if (data[0] == (byte) 0x89 && data[1] == 0x50 && data[2] == 0x4E && data[3] == 0x47) {
            return true;
        }
        
        // BWS only supports JPEG and PNG
        return false;
    }

    /**
     * Validates liveness detection request with challenge directions.
     */
    public static void validateLivenessRequest(List<byte[]> images, 
                                              LivenessDetectionClient.LivenessMode mode,
                                              List<LivenessDetectionClient.ChallengeDirection> directions) 
            throws BioIdValidationException {
        validateLivenessRequest(images, mode);
        
        if (mode == LivenessDetectionClient.LivenessMode.CHALLENGE_RESPONSE) {
            if (directions == null) {
                throw new BioIdValidationException("Challenge directions cannot be null");
            }
            if (directions.isEmpty()) {
                throw new BioIdValidationException("Challenge directions cannot be null or empty");
            }
            if (directions.size() > 4) {
                throw new BioIdValidationException("Maximum 4 challenge directions allowed");
            }
            // Check for duplicates
            if (directions.stream().distinct().count() < directions.size()) {
                throw new BioIdValidationException("Duplicate challenge directions not allowed");
            }
        }
    }

    /**
     * Validates challenge directions.
     */
    public static void validateChallengeDirections(List<LivenessDetectionClient.ChallengeDirection> directions) 
            throws BioIdValidationException {
        if (directions == null || directions.isEmpty()) {
            throw new BioIdValidationException("Challenge directions cannot be null or empty");
        }
        if (directions.size() > 4) {
            throw new BioIdValidationException("Maximum 4 challenge directions allowed");
        }
        
        // Check for duplicates
        if (directions.stream().distinct().count() < directions.size()) {
            throw new BioIdValidationException("Challenge directions must be unique");
        }
    }

    /**
     * Validates liveness threshold value.
     */
    public static void validateLivenessThreshold(double threshold) throws BioIdValidationException {
        if (Double.isNaN(threshold) || Double.isInfinite(threshold)) {
            throw new BioIdValidationException("Liveness threshold must be a valid number");
        }
        if (threshold < 0.0 || threshold > 1.0) {
            throw new BioIdValidationException("Liveness threshold must be between 0.0 and 1.0");
        }
    }

    /**
     * Handles gRPC exceptions and converts them to BioIdException.
     */
    public static BioIdException handleGrpcException(StatusRuntimeException ex, String operation) {
        String message = ex.getStatus().getDescription();
        if (message == null) {
            message = ex.getStatus().getCode().toString();
        }
        
        switch (ex.getStatus().getCode()) {
            case UNAVAILABLE:
                return new BioIdServiceException("Service unavailable during " + operation + ": " + message);
            
            case DEADLINE_EXCEEDED:
                // Use "timed out" for timeout errors to match test expectations
                return new BioIdServiceException("Request timed out during " + operation + ": " + message);
            
            case INVALID_ARGUMENT:
                return new BioIdValidationException("Invalid argument for " + operation + ": " + message);
            
            case NOT_FOUND:
                return new BioIdServiceException("Resource not found during " + operation + ": " + message);
            
            case UNAUTHENTICATED:
            case PERMISSION_DENIED:
                return new BioIdServiceException("Authentication failed during " + operation + ": " + message);
            
            default:
                return new BioIdServiceException("Error during " + operation + ": " + message);
        }
    }
}
