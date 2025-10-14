package com.bioid.keycloak.client.liveness;

import com.bioid.services.Bws;
import com.bioid.services.Bwsmessages;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Response model for liveness detection operations.
 *
 * <p>This class provides a convenient wrapper around the protobuf LivenessDetectionResponse,
 * offering easy access to liveness detection results and detailed analysis.
 */
public class LivenessDetectionResponse {

  private final JobStatus status;
  private final List<JobError> errors;
  private final List<ImageProperties> imageProperties;
  private final boolean live;
  private final double livenessScore;
  private final LivenessDetails details;

  private LivenessDetectionResponse(
      JobStatus status,
      List<JobError> errors,
      List<ImageProperties> imageProperties,
      boolean live,
      double livenessScore,
      LivenessDetails details) {
    this.status = status;
    this.errors = List.copyOf(errors);
    this.imageProperties = List.copyOf(imageProperties);
    this.live = live;
    this.livenessScore = livenessScore;
    this.details = details;
  }

  /**
   * Creates a LivenessDetectionResponse from a protobuf response.
   *
   * @param protobufResponse the protobuf response
   * @return wrapped response
   */
  public static LivenessDetectionResponse fromProtobuf(Bws.LivenessDetectionResponse protobufResponse) {
    JobStatus status = JobStatus.fromProtobuf(protobufResponse.getStatus());
    
    List<JobError> errors = protobufResponse.getErrorsList().stream()
        .map(JobError::fromProtobuf)
        .collect(Collectors.toList());
    
    List<ImageProperties> imageProperties = protobufResponse.getImagePropertiesList().stream()
        .map(ImageProperties::fromProtobuf)
        .collect(Collectors.toList());
    
    LivenessDetails details = LivenessDetails.fromProtobuf(protobufResponse);
    
    return new LivenessDetectionResponse(
        status,
        errors,
        imageProperties,
        protobufResponse.getLive(),
        protobufResponse.getLivenessScore(),
        details
    );
  }

  /**
   * Gets the job status.
   *
   * @return job status
   */
  public JobStatus getStatus() {
    return status;
  }

  /**
   * Gets the list of errors that occurred during processing.
   *
   * @return list of job errors
   */
  public List<JobError> getErrors() {
    return errors;
  }

  /**
   * Gets the image properties for each processed image.
   *
   * @return list of image properties
   */
  public List<ImageProperties> getImageProperties() {
    return imageProperties;
  }

  /**
   * Gets the liveness decision.
   *
   * @return true if the images are from a live person, false otherwise
   */
  public boolean isLive() {
    return live;
  }

  /**
   * Gets the liveness confidence score.
   *
   * @return confidence score between 0.0 and 1.0
   */
  public double getLivenessScore() {
    return livenessScore;
  }

  /**
   * Gets detailed liveness analysis results.
   *
   * @return liveness details
   */
  public LivenessDetails getDetails() {
    return details;
  }

  /**
   * Checks if the liveness detection was successful.
   *
   * @return true if status is SUCCEEDED, false otherwise
   */
  public boolean isSuccessful() {
    return status == JobStatus.SUCCEEDED;
  }

  /**
   * Checks if there were any errors during processing.
   *
   * @return true if there are errors, false otherwise
   */
  public boolean hasErrors() {
    return !errors.isEmpty();
  }

  /**
   * Gets the first error message if any errors occurred.
   *
   * @return first error message or null if no errors
   */
  public String getFirstErrorMessage() {
    return errors.isEmpty() ? null : errors.get(0).getMessage();
  }



  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    LivenessDetectionResponse that = (LivenessDetectionResponse) o;
    return live == that.live &&
        Double.compare(that.livenessScore, livenessScore) == 0 &&
        status == that.status &&
        Objects.equals(errors, that.errors) &&
        Objects.equals(imageProperties, that.imageProperties) &&
        Objects.equals(details, that.details);
  }

  @Override
  public int hashCode() {
    return Objects.hash(status, errors, imageProperties, live, livenessScore, details);
  }

  @Override
  public String toString() {
    return "LivenessDetectionResponse{" +
        "status=" + status +
        ", live=" + live +
        ", livenessScore=" + livenessScore +
        ", errorCount=" + errors.size() +
        ", imageCount=" + imageProperties.size() +
        '}';
  }

  /**
   * Job status enumeration.
   */
  public enum JobStatus {
    SUCCEEDED,
    FAULTED,
    CANCELLED;

    public static JobStatus fromProtobuf(Bwsmessages.JobStatus protobufStatus) {
      switch (protobufStatus) {
        case SUCCEEDED:
          return SUCCEEDED;
        case FAULTED:
          return FAULTED;
        case CANCELLED:
          return CANCELLED;
        default:
          throw new IllegalArgumentException("Unknown job status: " + protobufStatus);
      }
    }
  }

  /**
   * Job error information.
   */
  public static class JobError {
    private final String errorCode;
    private final String message;

    public JobError(String errorCode, String message) {
      this.errorCode = errorCode;
      this.message = message;
    }

    public static JobError fromProtobuf(Bwsmessages.JobError protobufError) {
      return new JobError(protobufError.getErrorCode(), protobufError.getMessage());
    }

    public String getErrorCode() {
      return errorCode;
    }

    public String getMessage() {
      return message;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      JobError jobError = (JobError) o;
      return Objects.equals(errorCode, jobError.errorCode) &&
          Objects.equals(message, jobError.message);
    }

    @Override
    public int hashCode() {
      return Objects.hash(errorCode, message);
    }

    @Override
    public String toString() {
      return "JobError{" +
          "errorCode='" + errorCode + '\'' +
          ", message='" + message + '\'' +
          '}';
    }
  }

  /**
   * Image properties information.
   */
  public static class ImageProperties {
    private final int rotated;
    private final int faceCount;
    private final double qualityScore;

    public ImageProperties(int rotated, int faceCount, double qualityScore) {
      this.rotated = rotated;
      this.faceCount = faceCount;
      this.qualityScore = qualityScore;
    }

    public static ImageProperties fromProtobuf(Bwsmessages.ImageProperties protobufProperties) {
      return new ImageProperties(
          protobufProperties.getRotated(),
          protobufProperties.getFacesCount(),
          protobufProperties.getQualityScore()
      );
    }

    public int getRotated() {
      return rotated;
    }

    public int getFaceCount() {
      return faceCount;
    }

    public double getQualityScore() {
      return qualityScore;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      ImageProperties that = (ImageProperties) o;
      return rotated == that.rotated &&
          faceCount == that.faceCount &&
          Double.compare(that.qualityScore, qualityScore) == 0;
    }

    @Override
    public int hashCode() {
      return Objects.hash(rotated, faceCount, qualityScore);
    }

    @Override
    public String toString() {
      return "ImageProperties{" +
          "rotated=" + rotated +
          ", faceCount=" + faceCount +
          ", qualityScore=" + qualityScore +
          '}';
    }
  }
}