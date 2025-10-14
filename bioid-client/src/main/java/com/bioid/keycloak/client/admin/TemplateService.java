package com.bioid.keycloak.client.admin;

import com.bioid.keycloak.client.admin.model.BulkUpgradeResult;
import com.bioid.keycloak.client.admin.model.TemplateHealthReport;
import com.bioid.keycloak.client.admin.model.TemplateStatusSummary;
import com.bioid.keycloak.client.admin.model.TemplateUpgradeResult;
import com.bioid.keycloak.client.exception.BioIdException;

import java.util.List;

/**
 * Service for managing biometric template status, health monitoring, and lifecycle operations.
 * Provides comprehensive template management capabilities for administrators.
 */
public interface TemplateService {

    /**
     * Retrieves detailed template status from BioID GetTemplateStatus gRPC API.
     *
     * @param classId the class ID of the template
     * @param includeThumbnails whether to include thumbnail data in the response
     * @return detailed template status summary
     * @throws BioIdException if template status retrieval fails
     */
    TemplateStatusSummary getTemplateStatus(long classId, boolean includeThumbnails) throws BioIdException;

    /**
     * Retrieves template status for multiple templates in a single batch operation.
     *
     * @param classIds list of class IDs to query
     * @return list of template status summaries
     * @throws BioIdException if batch template status retrieval fails
     */
    List<TemplateStatusSummary> getTemplateStatusBatch(List<Long> classIds) throws BioIdException;

    /**
     * Upgrades a single template to the latest encoder version using stored thumbnails.
     *
     * @param classId the class ID of the template to upgrade
     * @return result of the upgrade operation
     * @throws BioIdException if template upgrade fails
     */
    TemplateUpgradeResult upgradeTemplate(long classId) throws BioIdException;

    /**
     * Upgrades multiple templates to the latest encoder version in a batch operation.
     *
     * @param classIds list of class IDs to upgrade
     * @return comprehensive results of the bulk upgrade operation
     * @throws BioIdException if bulk template upgrade fails
     */
    BulkUpgradeResult upgradeTemplatesBatch(List<Long> classIds) throws BioIdException;

    /**
     * Analyzes template health for a list of class IDs and generates a comprehensive report.
     *
     * @param classIds the list of class IDs to analyze
     * @return detailed health report with issues and recommendations
     * @throws BioIdException if template health analysis fails
     */
    TemplateHealthReport analyzeTemplateHealth(List<Long> classIds) throws BioIdException;

    /**
     * Schedules cleanup of expired or orphaned templates for a list of class IDs.
     *
     * @param classIds the list of class IDs to schedule cleanup for
     * @throws BioIdException if template cleanup scheduling fails
     */
    void scheduleTemplateCleanup(List<Long> classIds) throws BioIdException;
}