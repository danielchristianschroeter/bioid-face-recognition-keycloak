package com.bioid.keycloak.admin.dto;

import java.util.List;

/**
 * DTO for liveness detection test requests.
 */
public class LivenessTestRequest {
    private List<String> images; // Base64 encoded images
    private String mode;
    private double threshold;
    private List<String> challengeDirections;

    public LivenessTestRequest() {}

    public List<String> getImages() {
        return images;
    }

    public void setImages(List<String> images) {
        this.images = images;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public double getThreshold() {
        return threshold;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    public List<String> getChallengeDirections() {
        return challengeDirections;
    }

    public void setChallengeDirections(List<String> challengeDirections) {
        this.challengeDirections = challengeDirections;
    }
}