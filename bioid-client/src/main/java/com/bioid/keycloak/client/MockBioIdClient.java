package com.bioid.keycloak.client;

import com.bioid.keycloak.client.exception.BioIdException;
import java.util.Arrays;
import java.util.List;

/**
 * Mock implementation of BioIdClient for testing purposes.
 * This implementation provides basic functionality without requiring protobuf classes.
 */
public class MockBioIdClient implements BioIdClient {

    private boolean healthy = true;
    private String endpoint = "mock://localhost:9090";

    @Override
    public void enroll(byte[] imageData, long classId) throws BioIdException {
        if (imageData == null || imageData.length == 0) {
            throw new BioIdException("Invalid image data");
        }
        // Mock enrollment - always succeeds
    }

    @Override
    public boolean verify(byte[] imageData, long classId) throws BioIdException {
        if (imageData == null || imageData.length == 0) {
            throw new BioIdException("Invalid image data");
        }
        // Mock verification - always succeeds
        return true;
    }

    @Override
    public void deleteTemplate(long classId) throws BioIdException {
        // Mock deletion - always succeeds
    }

    @Override
    public String getTemplateStatus(long classId) throws BioIdException {
        // Mock status - return available
        return "AVAILABLE";
    }

    @Override
    public void setTemplateTags(long classId, String[] tags) throws BioIdException {
        // Mock tag setting - always succeeds
    }

    @Override
    public boolean livenessDetection(byte[] imageData) throws BioIdException {
        if (imageData == null || imageData.length == 0) {
            throw new BioIdException("Invalid image data");
        }
        // Mock liveness detection - always passes
        return true;
    }

    @Override
    public boolean isHealthy() {
        return healthy;
    }

    @Override
    public String getCurrentEndpoint() {
        return endpoint;
    }

    @Override
    public boolean verifyFaceWithImageData(long classId, String imageData) throws BioIdException {
        if (imageData == null || imageData.isEmpty()) {
            throw new BioIdException("Invalid image data");
        }
        // Mock verification - always succeeds
        return true;
    }

    @Override
    public EnrollmentResult enrollFaceWithImageData(long classId, String imageData) throws BioIdException {
        if (imageData == null || imageData.isEmpty()) {
            throw new BioIdException("Invalid image data");
        }
        
        // Mock enrollment result
        return new EnrollmentResult(
            classId,
            true, // available
            3, // encoder version
            1, // feature vectors
            1, // thumbnails stored
            Arrays.asList("enrolled"), // tags
            "ENROLLED", // performed action
            1 // enrolled images
        );
    }

    @Override
    public void close() {
        // Mock close - nothing to do
    }

    // Setter methods for testing
    public void setHealthy(boolean healthy) {
        this.healthy = healthy;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }
}