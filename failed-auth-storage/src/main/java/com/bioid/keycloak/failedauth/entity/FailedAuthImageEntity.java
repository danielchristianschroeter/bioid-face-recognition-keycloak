package com.bioid.keycloak.failedauth.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * JPA Entity for failed authentication images.
 * 
 * Stores image data (encrypted) and metadata including
 * face detection results and quality assessments.
 */
@Entity
@Table(name = "failed_auth_images", indexes = {
    @Index(name = "idx_attempt_id", columnList = "attempt_id")
}, uniqueConstraints = {
    @UniqueConstraint(name = "idx_attempt_image", columnNames = {"attempt_id", "image_index"})
})
public class FailedAuthImageEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "image_id")
    private Long imageId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id", nullable = false)
    private FailedAuthAttemptEntity attempt;
    
    @Column(name = "image_index", nullable = false)
    private Integer imageIndex;
    
    // Image data (encrypted)
    @Lob
    @Column(name = "image_data", nullable = false)
    private byte[] imageData;
    
    @Lob
    @Column(name = "thumbnail_data")
    private byte[] thumbnailData;
    
    // Image properties
    @Column(name = "file_size", nullable = false)
    private Integer fileSize;
    
    @Column(name = "width")
    private Integer width;
    
    @Column(name = "height")
    private Integer height;
    
    @Column(name = "format", length = 10)
    private String format;
    
    @Column(name = "capture_timestamp")
    private Instant captureTimestamp;
    
    // Face detection results
    @Column(name = "face_found")
    private Boolean faceFound;
    
    @Column(name = "face_count")
    private Integer faceCount;
    
    @Column(name = "face_quality")
    private Double faceQuality;
    
    @Column(name = "eyes_visible")
    private Boolean eyesVisible;
    
    @Column(name = "mouth_visible")
    private Boolean mouthVisible;
    
    // Face angles
    @Column(name = "face_yaw")
    private Double faceYaw;
    
    @Column(name = "face_pitch")
    private Double facePitch;
    
    @Column(name = "face_roll")
    private Double faceRoll;
    
    // Quality assessments (stored as JSON)
    @Column(name = "quality_assessments", columnDefinition = "TEXT")
    private String qualityAssessments;
    
    @Column(name = "quality_score")
    private Double qualityScore;
    
    @Column(name = "recommended_for_enrollment")
    private Boolean recommendedForEnrollment;
    
    @Column(name = "feature_vectors_extracted")
    private Integer featureVectorsExtracted;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    // Lifecycle callbacks
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
    
    // Constructors
    public FailedAuthImageEntity() {
    }
    
    public FailedAuthImageEntity(FailedAuthAttemptEntity attempt, Integer imageIndex, 
                                 byte[] imageData, Integer fileSize) {
        this.attempt = attempt;
        this.imageIndex = imageIndex;
        this.imageData = imageData;
        this.fileSize = fileSize;
    }
    
    // Getters and Setters
    public Long getImageId() {
        return imageId;
    }
    
    public void setImageId(Long imageId) {
        this.imageId = imageId;
    }
    
    public FailedAuthAttemptEntity getAttempt() {
        return attempt;
    }
    
    public void setAttempt(FailedAuthAttemptEntity attempt) {
        this.attempt = attempt;
    }
    
    public Integer getImageIndex() {
        return imageIndex;
    }
    
    public void setImageIndex(Integer imageIndex) {
        this.imageIndex = imageIndex;
    }
    
    public byte[] getImageData() {
        return imageData;
    }
    
    public void setImageData(byte[] imageData) {
        this.imageData = imageData;
    }
    
    public byte[] getThumbnailData() {
        return thumbnailData;
    }
    
    public void setThumbnailData(byte[] thumbnailData) {
        this.thumbnailData = thumbnailData;
    }
    
    public Integer getFileSize() {
        return fileSize;
    }
    
    public void setFileSize(Integer fileSize) {
        this.fileSize = fileSize;
    }
    
    public Integer getWidth() {
        return width;
    }
    
    public void setWidth(Integer width) {
        this.width = width;
    }
    
    public Integer getHeight() {
        return height;
    }
    
    public void setHeight(Integer height) {
        this.height = height;
    }
    
    public String getFormat() {
        return format;
    }
    
    public void setFormat(String format) {
        this.format = format;
    }
    
    public Instant getCaptureTimestamp() {
        return captureTimestamp;
    }
    
    public void setCaptureTimestamp(Instant captureTimestamp) {
        this.captureTimestamp = captureTimestamp;
    }
    
    public Boolean getFaceFound() {
        return faceFound;
    }
    
    public void setFaceFound(Boolean faceFound) {
        this.faceFound = faceFound;
    }
    
    public Integer getFaceCount() {
        return faceCount;
    }
    
    public void setFaceCount(Integer faceCount) {
        this.faceCount = faceCount;
    }
    
    public Double getFaceQuality() {
        return faceQuality;
    }
    
    public void setFaceQuality(Double faceQuality) {
        this.faceQuality = faceQuality;
    }
    
    public Boolean getEyesVisible() {
        return eyesVisible;
    }
    
    public void setEyesVisible(Boolean eyesVisible) {
        this.eyesVisible = eyesVisible;
    }
    
    public Boolean getMouthVisible() {
        return mouthVisible;
    }
    
    public void setMouthVisible(Boolean mouthVisible) {
        this.mouthVisible = mouthVisible;
    }
    
    public Double getFaceYaw() {
        return faceYaw;
    }
    
    public void setFaceYaw(Double faceYaw) {
        this.faceYaw = faceYaw;
    }
    
    public Double getFacePitch() {
        return facePitch;
    }
    
    public void setFacePitch(Double facePitch) {
        this.facePitch = facePitch;
    }
    
    public Double getFaceRoll() {
        return faceRoll;
    }
    
    public void setFaceRoll(Double faceRoll) {
        this.faceRoll = faceRoll;
    }
    
    public String getQualityAssessments() {
        return qualityAssessments;
    }
    
    public void setQualityAssessments(String qualityAssessments) {
        this.qualityAssessments = qualityAssessments;
    }
    
    public Double getQualityScore() {
        return qualityScore;
    }
    
    public void setQualityScore(Double qualityScore) {
        this.qualityScore = qualityScore;
    }
    
    public Boolean getRecommendedForEnrollment() {
        return recommendedForEnrollment;
    }
    
    public void setRecommendedForEnrollment(Boolean recommendedForEnrollment) {
        this.recommendedForEnrollment = recommendedForEnrollment;
    }
    
    public Integer getFeatureVectorsExtracted() {
        return featureVectorsExtracted;
    }
    
    public void setFeatureVectorsExtracted(Integer featureVectorsExtracted) {
        this.featureVectorsExtracted = featureVectorsExtracted;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FailedAuthImageEntity)) return false;
        FailedAuthImageEntity that = (FailedAuthImageEntity) o;
        return imageId != null && imageId.equals(that.imageId);
    }
    
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
    
    @Override
    public String toString() {
        return "FailedAuthImageEntity{" +
                "imageId=" + imageId +
                ", imageIndex=" + imageIndex +
                ", fileSize=" + fileSize +
                ", faceFound=" + faceFound +
                ", qualityScore=" + qualityScore +
                '}';
    }
}
