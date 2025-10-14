package com.bioid.keycloak.client.liveness;

import com.bioid.keycloak.client.exception.BioIdException;
import com.bioid.keycloak.client.exception.BioIdServiceException;
import com.bioid.keycloak.client.exception.BioIdValidationException;
import com.bioid.services.Bws;
import com.bioid.services.Bwsmessages;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Specialized error handler for liveness detection operations.
 *
 * <p>This class provides comprehensive error handling for BWS-specific errors and gRPC status
 * codes, with specific handling for liveness detection failure scenarios.
 */
public class LivenessDetectionErrorHandler {

  private static final Logger logger = LoggerFactory.getLogger(LivenessDetectionErrorHandler.class);

  // BWS-specific error codes for liveness detection
  private static final Set<String> FACE_DETECTION_ERRORS = Set.of(
      "FaceNotFound",
      "MultipleFacesFound",
      "ThumbnailExtractionFailed"
  );

  private static final Set<String> LIVENESS_REJECTION_ERRORS = Set.of(
      "RejectedByPassiveLiveDetection",
      "RejectedByActiveLiveDetection", 
      "RejectedByChallengeResponse"
  );

  private static final Set<String> IMAGE_QUALITY_ERRORS = Set.of(
      "ImageTooSmall",
      "ImageTooBig",
      "ImageQualityTooLow",
      "ImageCorrupted"
  );

  /**
   * Handles gRPC exceptions and converts them to appropriate BioIdException types.
   *
   * @param e the gRPC exception
   * @param operation the operation that failed
   * @return appropriate BioIdException
   */
  public static BioIdException handleGrpcException(StatusRuntimeException e, String operation) {
    Status.Code code = e.getStatus().getCode();
    String message = e.getStatus().getDescription();
    
    logger.debug("Handling gRPC error for {}: {} - {}", operation, code, message);
    
    switch (code) {
      case CANCELLED:
        return new BioIdServiceException(
            "Liveness detection was cancelled: " + message, 
            e, 
            "LIVENESS_CANCELLED"
        );
        
      case UNKNOWN:
        return new BioIdServiceException(
            "Unknown error during liveness detection: " + message, 
            e, 
            "LIVENESS_UNKNOWN_ERROR"
        );
        
      case INVALID_ARGUMENT:
        return new BioIdValidationException(
            "Invalid argument for liveness detection: " + message, 
            e, 
            "LIVENESS_INVALID_ARGUMENT"
        );
        
      case DEADLINE_EXCEEDED:
        return new BioIdServiceException(
            "Liveness detection timed out: " + message, 
            e, 
            "LIVENESS_TIMEOUT"
        );
        
      case NOT_FOUND:
        return new BioIdServiceException(
            "Liveness detection service not found: " + message, 
            e, 
            "LIVENESS_SERVICE_NOT_FOUND"
        );
        
      case ALREADY_EXISTS:
        return new BioIdServiceException(
            "Resource already exists: " + message, 
            e, 
            "LIVENESS_ALREADY_EXISTS"
        );
        
      case PERMISSION_DENIED:
        return new BioIdServiceException(
            "Permission denied for liveness detection: " + message, 
            e, 
            "LIVENESS_PERMISSION_DENIED"
        );
        
      case RESOURCE_EXHAUSTED:
        return new BioIdServiceException(
            "Resource exhausted during liveness detection: " + message, 
            e, 
            "LIVENESS_RESOURCE_EXHAUSTED"
        );
        
      case FAILED_PRECONDITION:
        return new BioIdValidationException(
            "Failed precondition for liveness detection: " + message, 
            e, 
            "LIVENESS_FAILED_PRECONDITION"
        );
        
      case ABORTED:
        return new BioIdServiceException(
            "Liveness detection was aborted: " + message, 
            e, 
            "LIVENESS_ABORTED"
        );
        
      case OUT_OF_RANGE:
        return new BioIdValidationException(
            "Parameter out of range for liveness detection: " + message, 
            e, 
            "LIVENESS_OUT_OF_RANGE"
        );
        
      case UNIMPLEMENTED:
        return new BioIdServiceException(
            "Liveness detection method not implemented: " + message, 
            e, 
            "LIVENESS_UNIMPLEMENTED"
        );
        
      case INTERNAL:
        return new BioIdServiceException(
            "Internal error during liveness detection: " + message, 
            e, 
            "LIVENESS_INTERNAL_ERROR"
        );
        
      case UNAVAILABLE:
        return new BioIdServiceException(
            "Liveness detection service unavailable: " + message, 
            e, 
            "LIVENESS_SERVICE_UNAVAILABLE"
        );
        
      case DATA_LOSS:
        return new BioIdServiceException(
            "Data loss during liveness detection: " + message, 
            e, 
            "LIVENESS_DATA_LOSS"
        );
        
      case UNAUTHENTICATED:
        return new BioIdServiceException(
            "Authentication failed for liveness detection: " + message, 
            e, 
            "LIVENESS_UNAUTHENTICATED"
        );
        
      default:
        return new BioIdServiceException(
            "Unexpected gRPC error during liveness detection: " + message, 
            e, 
            "LIVENESS_UNEXPECTED_ERROR"
        );
    }
  }

  /**
   * Handles BWS job errors from liveness detection responses.
   *
   * @param response the liveness detection response
   * @return appropriate BioIdException if errors exist, null otherwise
   */
  public static BioIdException handleBwsErrors(Bws.LivenessDetectionResponse response) {
    if (response.getErrorsList().isEmpty()) {
      return null;
    }
    
    List<Bwsmessages.JobError> errors = response.getErrorsList();
    Bwsmessages.JobError primaryError = errors.get(0);
    String errorCode = primaryError.getErrorCode();
    String errorMessage = primaryError.getMessage();
    
    logger.debug("Handling BWS error: {} - {}", errorCode, errorMessage);
    
    // Handle face detection errors
    if (FACE_DETECTION_ERRORS.contains(errorCode)) {
      return handleFaceDetectionError(errorCode, errorMessage, errors);
    }
    
    // Handle liveness rejection errors
    if (LIVENESS_REJECTION_ERRORS.contains(errorCode)) {
      return handleLivenessRejectionError(errorCode, errorMessage, response);
    }
    
    // Handle image quality errors
    if (IMAGE_QUALITY_ERRORS.contains(errorCode)) {
      return handleImageQualityError(errorCode, errorMessage);
    }
    
    // Handle other BWS errors
    return handleGenericBwsError(errorCode, errorMessage);
  }

  /**
   * Validates and handles liveness detection response.
   *
   * @param response the liveness detection response
   * @param threshold the liveness threshold to validate against
   * @throws BioIdException if the response indicates failure or errors
   */
  public static void validateLivenessResponse(
      Bws.LivenessDetectionResponse response, 
      double threshold) throws BioIdException {
    
    // Check for BWS errors first
    BioIdException bwsError = handleBwsErrors(response);
    if (bwsError != null) {
      throw bwsError;
    }
    
    // Check job status
    if (response.getStatus() == Bwsmessages.JobStatus.FAULTED) {
      throw new BioIdServiceException(
          "Liveness detection job failed", 
          "LIVENESS_JOB_FAULTED"
      );
    }
    
    if (response.getStatus() == Bwsmessages.JobStatus.CANCELLED) {
      throw new BioIdServiceException(
          "Liveness detection job was cancelled", 
          "LIVENESS_JOB_CANCELLED"
      );
    }
    
    // Validate liveness score against threshold
    if (!response.getLive() || response.getLivenessScore() < threshold) {
      throw new BioIdValidationException(
          String.format("Liveness detection failed (score: %.3f, threshold: %.3f)", 
              response.getLivenessScore(), threshold), 
          "LIVENESS_THRESHOLD_NOT_MET"
      );
    }
  }

  /**
   * Validates liveness detection request parameters.
   *
   * @param images list of images
   * @param mode liveness detection mode
   * @throws BioIdValidationException if validation fails
   */
  public static void validateLivenessRequest(
      List<byte[]> images, 
      LivenessDetectionClient.LivenessMode mode) throws BioIdValidationException {
    
    if (images == null || images.isEmpty()) {
      throw new BioIdValidationException(
          "No images provided for liveness detection", 
          "LIVENESS_NO_IMAGES"
      );
    }
    
    if (images.size() > 2) {
      throw new BioIdValidationException(
          "Maximum 2 images allowed for liveness detection, got: " + images.size(), 
          "LIVENESS_TOO_MANY_IMAGES"
      );
    }
    
    // Validate image count for specific modes
    switch (mode) {
      case PASSIVE:
        if (images.size() != 1) {
          throw new BioIdValidationException(
              "Passive liveness detection requires exactly 1 image, got: " + images.size(), 
              "LIVENESS_PASSIVE_IMAGE_COUNT"
          );
        }
        break;
        
      case ACTIVE:
      case CHALLENGE_RESPONSE:
        if (images.size() != 2) {
          throw new BioIdValidationException(
              mode + " liveness detection requires exactly 2 images, got: " + images.size(), 
              "LIVENESS_ACTIVE_IMAGE_COUNT"
          );
        }
        break;
    }
    
    // Validate individual images using enhanced validation
    for (int i = 0; i < images.size(); i++) {
      validateImageFormat(images.get(i), i);
    }
  }

  /**
   * Validates complete liveness detection request including challenge directions.
   *
   * @param images list of images
   * @param mode liveness detection mode
   * @param challengeDirections challenge directions (can be null for non-challenge modes)
   * @throws BioIdValidationException if validation fails
   */
  public static void validateLivenessRequest(
      List<byte[]> images, 
      LivenessDetectionClient.LivenessMode mode,
      List<LivenessDetectionClient.ChallengeDirection> challengeDirections) 
      throws BioIdValidationException {
    
    // Validate basic request parameters
    validateLivenessRequest(images, mode);
    
    // Additional validation for challenge-response mode
    if (mode == LivenessDetectionClient.LivenessMode.CHALLENGE_RESPONSE) {
      validateChallengeDirections(challengeDirections);
    }
  }

  /**
   * Validates liveness detection threshold.
   *
   * @param threshold the threshold value
   * @throws BioIdValidationException if threshold is invalid
   */
  public static void validateLivenessThreshold(double threshold) throws BioIdValidationException {
    if (Double.isNaN(threshold) || Double.isInfinite(threshold) || threshold < 0.0 || threshold > 1.0) {
      throw new BioIdValidationException(
          "Liveness threshold must be between 0.0 and 1.0, got: " + threshold, 
          "LIVENESS_INVALID_THRESHOLD"
      );
    }
  }

  /**
   * Validates challenge directions for challenge-response liveness detection.
   *
   * @param challengeDirections list of challenge directions
   * @throws BioIdValidationException if challenge directions are invalid
   */
  public static void validateChallengeDirections(
      List<LivenessDetectionClient.ChallengeDirection> challengeDirections) 
      throws BioIdValidationException {
    
    if (challengeDirections == null || challengeDirections.isEmpty()) {
      throw new BioIdValidationException(
          "Challenge directions cannot be null or empty for challenge-response mode", 
          "LIVENESS_EMPTY_CHALLENGE_DIRECTIONS"
      );
    }
    
    if (challengeDirections.size() > 4) {
      throw new BioIdValidationException(
          "Maximum 4 challenge directions allowed, got: " + challengeDirections.size(), 
          "LIVENESS_TOO_MANY_CHALLENGE_DIRECTIONS"
      );
    }
    
    // Check for duplicates
    Set<LivenessDetectionClient.ChallengeDirection> uniqueDirections = new HashSet<>(challengeDirections);
    if (uniqueDirections.size() != challengeDirections.size()) {
      throw new BioIdValidationException(
          "Duplicate challenge directions are not allowed", 
          "LIVENESS_DUPLICATE_CHALLENGE_DIRECTIONS"
      );
    }
  }

  /**
   * Validates image format and basic properties.
   *
   * @param imageBytes the image data
   * @param imageIndex the index of the image (for error messages)
   * @throws BioIdValidationException if image format is invalid
   */
  public static void validateImageFormat(byte[] imageBytes, int imageIndex) 
      throws BioIdValidationException {
    
    if (imageBytes == null || imageBytes.length == 0) {
      throw new BioIdValidationException(
          "Image " + (imageIndex + 1) + " is null or empty", 
          "LIVENESS_EMPTY_IMAGE"
      );
    }
    
    // Check minimum image size (1KB)
    if (imageBytes.length < 1024) {
      throw new BioIdValidationException(
          "Image " + (imageIndex + 1) + " is too small: " + imageBytes.length + " bytes (minimum 1KB)", 
          "LIVENESS_IMAGE_TOO_SMALL"
      );
    }
    
    // Check maximum image size (10MB)
    if (imageBytes.length > 10 * 1024 * 1024) {
      throw new BioIdValidationException(
          "Image " + (imageIndex + 1) + " is too large: " + imageBytes.length + " bytes (maximum 10MB)", 
          "LIVENESS_IMAGE_TOO_LARGE"
      );
    }
    
    // Basic format validation by checking magic bytes
    if (!isValidImageFormat(imageBytes)) {
      throw new BioIdValidationException(
          "Image " + (imageIndex + 1) + " has unsupported format. Only JPEG and PNG are supported.", 
          "LIVENESS_UNSUPPORTED_IMAGE_FORMAT"
      );
    }
  }

  /**
   * Checks if the image has a valid format (JPEG or PNG) by examining magic bytes.
   *
   * @param imageBytes the image data
   * @return true if the image format is supported, false otherwise
   */
  private static boolean isValidImageFormat(byte[] imageBytes) {
    if (imageBytes.length < 4) {
      return false;
    }
    
    // Check for JPEG magic bytes (FF D8 FF)
    if (imageBytes[0] == (byte) 0xFF && 
        imageBytes[1] == (byte) 0xD8 && 
        imageBytes[2] == (byte) 0xFF) {
      return true;
    }
    
    // Check for PNG magic bytes (89 50 4E 47)
    if (imageBytes[0] == (byte) 0x89 && 
        imageBytes[1] == (byte) 0x50 && 
        imageBytes[2] == (byte) 0x4E && 
        imageBytes[3] == (byte) 0x47) {
      return true;
    }
    
    return false;
  }

  /**
   * Checks if a gRPC error is retryable.
   *
   * @param e the gRPC exception
   * @return true if the error is retryable, false otherwise
   */
  public static boolean isRetryableError(StatusRuntimeException e) {
    Status.Code code = e.getStatus().getCode();
    return code == Status.Code.UNAVAILABLE
        || code == Status.Code.DEADLINE_EXCEEDED
        || code == Status.Code.RESOURCE_EXHAUSTED
        || code == Status.Code.INTERNAL
        || code == Status.Code.ABORTED;
  }

  private static BioIdException handleFaceDetectionError(
      String errorCode, 
      String errorMessage, 
      List<Bwsmessages.JobError> errors) {
    
    switch (errorCode) {
      case "FaceNotFound":
        return new BioIdValidationException(
            "No face detected in the image. Please ensure the image contains a clear, visible face.", 
            "LIVENESS_FACE_NOT_FOUND"
        );
        
      case "MultipleFacesFound":
        return new BioIdValidationException(
            "Multiple faces detected in the image. Please provide an image with only one face.", 
            "LIVENESS_MULTIPLE_FACES"
        );
        
      case "ThumbnailExtractionFailed":
        return new BioIdServiceException(
            "Failed to extract face thumbnail from the image: " + errorMessage, 
            "LIVENESS_THUMBNAIL_EXTRACTION_FAILED"
        );
        
      default:
        return new BioIdServiceException(
            "Face detection error: " + errorMessage, 
            "LIVENESS_FACE_DETECTION_ERROR"
        );
    }
  }

  private static BioIdException handleLivenessRejectionError(
      String errorCode, 
      String errorMessage, 
      Bws.LivenessDetectionResponse response) {
    
    double score = response.getLivenessScore();
    
    switch (errorCode) {
      case "RejectedByPassiveLiveDetection":
        return new BioIdServiceException(
            String.format("Passive liveness detection failed (score: %.3f). The image appears to be from a photo or video.", score), 
            "LIVENESS_PASSIVE_REJECTED"
        );
        
      case "RejectedByActiveLiveDetection":
        return new BioIdServiceException(
            String.format("Active liveness detection failed (score: %.3f). No sufficient movement detected between images.", score), 
            "LIVENESS_ACTIVE_REJECTED"
        );
        
      case "RejectedByChallengeResponse":
        return new BioIdServiceException(
            String.format("Challenge-response validation failed (score: %.3f). The required head movement was not detected.", score), 
            "LIVENESS_CHALLENGE_REJECTED"
        );
        
      default:
        return new BioIdServiceException(
            String.format("Liveness detection rejected (score: %.3f): %s", score, errorMessage), 
            "LIVENESS_REJECTED"
        );
    }
  }

  private static BioIdException handleImageQualityError(String errorCode, String errorMessage) {
    switch (errorCode) {
      case "ImageTooSmall":
        return new BioIdValidationException(
            "Image resolution is too small for liveness detection. Please provide a higher resolution image.", 
            "LIVENESS_IMAGE_TOO_SMALL"
        );
        
      case "ImageTooBig":
        return new BioIdValidationException(
            "Image file size is too large. Please provide a smaller image (maximum 10MB).", 
            "LIVENESS_IMAGE_TOO_BIG"
        );
        
      case "ImageQualityTooLow":
        return new BioIdValidationException(
            "Image quality is too low for reliable liveness detection. Please provide a clearer image.", 
            "LIVENESS_IMAGE_QUALITY_LOW"
        );
        
      case "ImageCorrupted":
        return new BioIdValidationException(
            "Image file is corrupted or in an unsupported format. Please provide a valid JPEG or PNG image.", 
            "LIVENESS_IMAGE_CORRUPTED"
        );
        
      default:
        return new BioIdValidationException(
            "Image quality error: " + errorMessage, 
            "LIVENESS_IMAGE_QUALITY_ERROR"
        );
    }
  }

  private static BioIdException handleGenericBwsError(String errorCode, String errorMessage) {
    // Map common BWS error codes to appropriate exceptions
    switch (errorCode) {
      case "InvalidPartition":
        return new BioIdValidationException(
            "Invalid partition specified for liveness detection", 
            "LIVENESS_INVALID_PARTITION"
        );
        
      case "QuotaExceeded":
        return new BioIdServiceException(
            "API quota exceeded for liveness detection", 
            "LIVENESS_QUOTA_EXCEEDED"
        );
        
      case "ServiceUnavailable":
        return new BioIdServiceException(
            "Liveness detection service is temporarily unavailable", 
            "LIVENESS_SERVICE_UNAVAILABLE"
        );
        
      case "InternalError":
        return new BioIdServiceException(
            "Internal error during liveness detection: " + errorMessage, 
            "LIVENESS_INTERNAL_ERROR"
        );
        
      default:
        return new BioIdServiceException(
            "Liveness detection error [" + errorCode + "]: " + errorMessage, 
            "LIVENESS_BWS_ERROR"
        );
    }
  }
}