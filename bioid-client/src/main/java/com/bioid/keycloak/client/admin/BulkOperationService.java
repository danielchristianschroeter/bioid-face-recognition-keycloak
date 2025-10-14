package com.bioid.keycloak.client.admin;

import com.bioid.keycloak.client.admin.model.*;
import com.bioid.keycloak.client.exception.BioIdException;

import java.util.List;

/**
 * Service for handling bulk operations on biometric templates and user enrollments.
 * Provides batch processing capabilities with progress tracking and rollback support.
 */
public interface BulkOperationService {

    /**
     * Generates enrollment links for multiple users with CSV import support.
     *
     * @param userIds list of user identifiers to generate enrollment links for
     * @param validityHours number of hours the links should be valid
     * @return bulk operation result with enrollment link results
     * @throws BioIdException if bulk enrollment link generation fails
     */
    BulkOperationResult<EnrollmentLinkResult> generateBulkEnrollmentLinks(
            List<String> userIds, int validityHours) throws BioIdException;

    /**
     * Deletes multiple templates with progress tracking and rollback capabilities.
     *
     * @param classIds list of template class IDs to delete
     * @param reason reason for deletion (for audit purposes)
     * @return bulk operation result with deletion results
     * @throws BioIdException if bulk template deletion fails
     */
    BulkOperationResult<Void> deleteBulkTemplates(List<Long> classIds, String reason) throws BioIdException;

    /**
     * Upgrades multiple templates to newer encoder versions using stored thumbnails.
     *
     * @param classIds list of template class IDs to upgrade
     * @return bulk operation result with upgrade results
     * @throws BioIdException if bulk template upgrade fails
     */
    BulkOperationResult<TemplateUpgradeResult> upgradeBulkTemplates(List<Long> classIds) throws BioIdException;

    /**
     * Sets tags on multiple templates for bulk tag management.
     *
     * @param classIds list of template class IDs to tag
     * @param tags list of tags to set on the templates
     * @return bulk operation result with tagging results
     * @throws BioIdException if bulk template tagging fails
     */
    BulkOperationResult<Void> setBulkTemplateTags(List<Long> classIds, List<String> tags) throws BioIdException;

    /**
     * Retrieves the status of a bulk operation for progress tracking.
     *
     * @param operationId the unique identifier of the bulk operation
     * @return current status of the bulk operation
     * @throws BioIdException if operation status retrieval fails
     */
    BulkOperationStatus getBulkOperationStatus(String operationId) throws BioIdException;

    /**
     * Cancels a running bulk operation with graceful cancellation.
     *
     * @param operationId the unique identifier of the bulk operation to cancel
     * @throws BioIdException if operation cancellation fails
     */
    void cancelBulkOperation(String operationId) throws BioIdException;

    /**
     * Retrieves detailed results of a completed bulk operation.
     *
     * @param operationId the unique identifier of the bulk operation
     * @param <T> the type of operation results
     * @return detailed bulk operation result
     * @throws BioIdException if operation result retrieval fails
     */
    <T> BulkOperationResult<T> getBulkOperationResult(String operationId) throws BioIdException;

    /**
     * Retrieves the current progress of a bulk operation.
     *
     * @param operationId the unique identifier of the bulk operation
     * @return progress information for the operation
     * @throws BioIdException if operation progress retrieval fails
     */
    BulkOperationProgress getBulkOperationProgress(String operationId) throws BioIdException;

    /**
     * Retrieves summaries of all active bulk operations.
     *
     * @return list of active operation summaries
     * @throws BioIdException if active operations retrieval fails
     */
    List<BulkOperationSummary> getActiveOperations() throws BioIdException;

    /**
     * Retrieves summaries of completed bulk operations.
     *
     * @return list of completed operation summaries
     * @throws BioIdException if completed operations retrieval fails
     */
    List<BulkOperationSummary> getCompletedOperations() throws BioIdException;
}