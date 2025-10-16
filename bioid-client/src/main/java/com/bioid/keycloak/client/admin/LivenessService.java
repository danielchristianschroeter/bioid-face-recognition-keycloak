package com.bioid.keycloak.client.admin;

import com.bioid.keycloak.client.admin.model.LivenessConfiguration;
import com.bioid.keycloak.client.admin.model.LivenessStatistics;
import com.bioid.keycloak.client.admin.model.LivenessTestResult;
import com.bioid.keycloak.client.exception.BioIdException;
import com.bioid.keycloak.client.liveness.LivenessDetectionRequest;
import com.bioid.keycloak.client.liveness.LivenessResult;

import java.time.LocalDate;

/**
 * Service for managing liveness detection configuration, execution, and analytics.
 * Provides comprehensive liveness detection capabilities for administrators.
 */
public interface LivenessService {

    /**
     * Performs liveness detection using the configured LivenessDetectionClient.
     *
     * @param request the liveness detection request with images and configuration
     * @return detailed liveness detection response with results and metadata
     * @throws BioIdException if liveness detection fails
     */
    LivenessResult performLivenessDetection(LivenessDetectionRequest request) throws BioIdException;

    /**
     * Retrieves the current liveness detection configuration for a realm.
     *
     * @param realmId the realm identifier to get configuration for
     * @return current liveness detection configuration
     * @throws BioIdException if configuration retrieval fails
     */
    LivenessConfiguration getLivenessConfiguration(String realmId) throws BioIdException;

    /**
     * Updates the liveness detection configuration for a realm.
     *
     * @param realmId the realm identifier to update configuration for
     * @param config the new liveness detection configuration
     * @throws BioIdException if configuration update fails
     */
    void updateLivenessConfiguration(String realmId, LivenessConfiguration config) throws BioIdException;

    /**
     * Tests liveness detection with provided images and mode for configuration validation.
     *
     * @param image1 the first image for liveness detection
     * @param image2 the second image (optional, required for ACTIVE and CHALLENGE_RESPONSE modes)
     * @param mode the liveness detection mode to test
     * @return test result with success status, liveness score, and performance metrics
     * @throws BioIdException if liveness detection test fails
     */
    LivenessTestResult testLivenessDetection(byte[] image1, byte[] image2, 
                                           LivenessConfiguration.LivenessMode mode) throws BioIdException;

    /**
     * Retrieves liveness detection statistics for a realm over a specified time period.
     *
     * @param realmId the realm identifier to get statistics for
     * @param from the start date for statistics (inclusive)
     * @param to the end date for statistics (inclusive)
     * @return comprehensive liveness detection statistics and analytics
     * @throws BioIdException if statistics retrieval fails
     */
    LivenessStatistics getLivenessStatistics(String realmId, LocalDate from, LocalDate to) throws BioIdException;
}