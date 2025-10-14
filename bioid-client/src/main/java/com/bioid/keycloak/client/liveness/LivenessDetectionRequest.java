package com.bioid.keycloak.client.liveness;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a liveness detection request with images and configuration.
 * 
 * This class provides a builder pattern for constructing liveness detection requests
 * with proper validation for different liveness modes.
 */
public class LivenessDetectionRequest {
    
    private final List<ImageData> liveImages;
    private final LivenessDetectionClient.LivenessMode mode;
    private final double threshold;
    private final List<LivenessDetectionClient.ChallengeDirection> challengeDirections;

    private LivenessDetectionRequest(Builder builder) {
        this.liveImages = Collections.unmodifiableList(new ArrayList<>(builder.liveImages));
        this.mode = builder.mode;
        this.threshold = builder.threshold;
        this.challengeDirections = builder.challengeDirections != null ? 
            Collections.unmodifiableList(new ArrayList<>(builder.challengeDirections)) : 
            Collections.emptyList();
        
        validate();
    }

    /**
     * Creates a passive liveness detection request with a single image.
     */
    public static LivenessDetectionRequest passive(byte[] image) {
        return builder()
            .addImage(image)
            .mode(LivenessDetectionClient.LivenessMode.PASSIVE)
            .build();
    }

    /**
     * Creates an active liveness detection request with two images.
     */
    public static LivenessDetectionRequest active(byte[] image1, byte[] image2) {
        return builder()
            .addImage(image1)
            .addImage(image2)
            .mode(LivenessDetectionClient.LivenessMode.ACTIVE)
            .build();
    }

    /**
     * Creates a challenge-response liveness detection request.
     */
    public static LivenessDetectionRequest challengeResponse(byte[] image1, byte[] image2, 
                                                           List<LivenessDetectionClient.ChallengeDirection> directions) {
        List<String> challengeTags = directions.stream()
            .map(Enum::name)
            .toList();
            
        return builder()
            .addImage(image1)
            .addImageWithTags(image2, challengeTags)
            .mode(LivenessDetectionClient.LivenessMode.CHALLENGE_RESPONSE)
            .challengeDirections(directions)
            .build();
    }

    /**
     * Creates a new builder for constructing liveness detection requests.
     */
    public static Builder builder() {
        return new Builder();
    }

    // Getters
    public List<ImageData> getLiveImages() { return liveImages; }
    public LivenessDetectionClient.LivenessMode getMode() { return mode; }
    public double getThreshold() { return threshold; }
    public List<LivenessDetectionClient.ChallengeDirection> getChallengeDirections() { return challengeDirections; }

    private void validate() {
        if (liveImages.isEmpty()) {
            throw new IllegalStateException("At least one image is required");
        }
        
        if (liveImages.size() > 2) {
            throw new IllegalStateException("Maximum 2 images allowed");
        }
        
        if (threshold < 0.0 || threshold > 1.0) {
            throw new IllegalArgumentException("Threshold must be between 0.0 and 1.0");
        }
        
        switch (mode) {
            case PASSIVE:
                if (liveImages.size() != 1) {
                    throw new IllegalStateException("Passive mode requires exactly 1 image");
                }
                break;
            case ACTIVE:
                if (liveImages.size() != 2) {
                    throw new IllegalStateException("ACTIVE mode requires exactly 2 images");
                }
                break;
            case CHALLENGE_RESPONSE:
                if (liveImages.size() != 2) {
                    throw new IllegalStateException("CHALLENGE_RESPONSE mode requires exactly 2 images");
                }
                if (challengeDirections.isEmpty()) {
                    throw new IllegalStateException("Challenge-response mode requires challenge directions");
                }
                break;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LivenessDetectionRequest that = (LivenessDetectionRequest) o;
        return Double.compare(that.threshold, threshold) == 0 &&
               Objects.equals(liveImages, that.liveImages) &&
               mode == that.mode &&
               Objects.equals(challengeDirections, that.challengeDirections);
    }

    @Override
    public int hashCode() {
        return Objects.hash(liveImages, mode, threshold, challengeDirections);
    }

    @Override
    public String toString() {
        return "LivenessDetectionRequest{" +
               "imageCount=" + liveImages.size() +
               ", mode=" + mode +
               ", threshold=" + threshold +
               ", challengeDirections=" + challengeDirections.size() +
               '}';
    }

    /**
     * Represents image data with optional tags for challenge-response scenarios.
     */
    public static class ImageData {
        private final byte[] imageBytes;
        private final List<String> tags;

        public ImageData(byte[] imageBytes) {
            this(imageBytes, Collections.emptyList());
        }

        public ImageData(byte[] imageBytes, List<String> tags) {
            if (imageBytes == null) {
                throw new NullPointerException("Image bytes cannot be null");
            }
            if (imageBytes.length == 0) {
                throw new IllegalArgumentException("Image bytes cannot be empty");
            }
            
            this.imageBytes = imageBytes.clone(); // Defensive copy
            this.tags = tags != null ? Collections.unmodifiableList(new ArrayList<>(tags)) : Collections.emptyList();
        }

        public byte[] getImageBytes() {
            return imageBytes.clone(); // Defensive copy
        }

        public List<String> getTags() {
            return tags;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ImageData imageData = (ImageData) o;
            return Arrays.equals(imageBytes, imageData.imageBytes) &&
                   Objects.equals(tags, imageData.tags);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(tags);
            result = 31 * result + Arrays.hashCode(imageBytes);
            return result;
        }

        @Override
        public String toString() {
            return "ImageData{" +
                   "imageSize=" + imageBytes.length +
                   ", tags=" + tags +
                   '}';
        }
    }

    /**
     * Builder for constructing LivenessDetectionRequest instances.
     */
    public static class Builder {
        private final List<ImageData> liveImages = new ArrayList<>();
        private LivenessDetectionClient.LivenessMode mode = LivenessDetectionClient.LivenessMode.PASSIVE;
        private double threshold = 0.7; // Default threshold
        private List<LivenessDetectionClient.ChallengeDirection> challengeDirections;

        public Builder addImage(byte[] imageBytes) {
            liveImages.add(new ImageData(imageBytes));
            return this;
        }

        public Builder addImageWithTags(byte[] imageBytes, List<String> tags) {
            liveImages.add(new ImageData(imageBytes, tags));
            return this;
        }

        public Builder mode(LivenessDetectionClient.LivenessMode mode) {
            this.mode = mode;
            return this;
        }

        public Builder threshold(double threshold) {
            this.threshold = threshold;
            return this;
        }

        public Builder challengeDirections(List<LivenessDetectionClient.ChallengeDirection> directions) {
            this.challengeDirections = directions;
            return this;
        }

        public LivenessDetectionRequest build() {
            return new LivenessDetectionRequest(this);
        }
    }
}