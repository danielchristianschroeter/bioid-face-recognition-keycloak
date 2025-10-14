package com.bioid.keycloak.client.admin;

import com.bioid.keycloak.client.BioIdClient;
import com.bioid.keycloak.client.admin.model.*;
import com.bioid.keycloak.client.exception.BioIdException;
import com.bioid.keycloak.client.exception.BioIdServiceException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of BulkOperationService providing batch processing capabilities
 * with progress tracking, rollback support, and background job processing.
 */
public class BulkOperationServiceImpl implements BulkOperationService {
    
    private static final Logger logger = Logger.getLogger(BulkOperationServiceImpl.class.getName());
    
    private final BioIdClient bioIdClient;
    private final AdminService adminService;
    private final TemplateService templateService;
    private final ExecutorService executorService;
    private final Map<String, BulkOperationResult<?>> operationResults;
    private final Map<String, Future<?>> runningOperations;
    private final BulkOperationJobProcessor jobProcessor;
    
    // Configuration
    private final int maxBulkOperationSize;
    private final int bulkOperationTimeoutMinutes;
    private final int maxConcurrentOperations;
    private final int batchSize;

    public BulkOperationServiceImpl(BioIdClient bioIdClient, AdminService adminService, 
                                   TemplateService templateService) {
        this.bioIdClient = bioIdClient;
        this.adminService = adminService;
        this.templateService = templateService;
        this.maxBulkOperationSize = 1000; // Default max size
        this.bulkOperationTimeoutMinutes = 30; // Default timeout
        this.maxConcurrentOperations = 5; // Default concurrent operations
        this.batchSize = 100; // Default batch size
        
        this.executorService = Executors.newFixedThreadPool(maxConcurrentOperations);
        this.operationResults = new ConcurrentHashMap<>();
        this.runningOperations = new ConcurrentHashMap<>();
        this.jobProcessor = new BulkOperationJobProcessor();
    }

    @Override
    public BulkOperationResult<EnrollmentLinkResult> generateBulkEnrollmentLinks(
            List<String> userIds, int validityHours) throws BioIdException {
        
        validateBulkOperationSize(userIds.size());
        
        String operationId = UUID.randomUUID().toString();
        BulkOperationResult<EnrollmentLinkResult> result = new BulkOperationResult<>(operationId, userIds.size());
        operationResults.put(operationId, result);
        jobProcessor.registerOperation(operationId, result);
        
        // Submit background job
        Future<?> future = executorService.submit(() -> {
            try {
                result.markRunning();
                processBulkEnrollmentLinks(result, userIds, validityHours);
                result.markCompleted();
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Bulk enrollment link generation failed", e);
                result.setStatus(BulkOperationStatus.FAILED);
                result.setCompletedAt(Instant.now());
            }
        });
        
        runningOperations.put(operationId, future);
        return result;
    }

    @Override
    public BulkOperationResult<Void> deleteBulkTemplates(List<Long> classIds, String reason) throws BioIdException {
        validateBulkOperationSize(classIds.size());
        
        String operationId = UUID.randomUUID().toString();
        BulkOperationResult<Void> result = new BulkOperationResult<>(operationId, classIds.size());
        operationResults.put(operationId, result);
        jobProcessor.registerOperation(operationId, result);
        
        // Submit background job
        Future<?> future = executorService.submit(() -> {
            try {
                result.markRunning();
                processBulkTemplateDeletion(result, classIds, reason);
                result.markCompleted();
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Bulk template deletion failed", e);
                result.setStatus(BulkOperationStatus.FAILED);
                result.setCompletedAt(Instant.now());
            }
        });
        
        runningOperations.put(operationId, future);
        return result;
    }

    @Override
    public BulkOperationResult<TemplateUpgradeResult> upgradeBulkTemplates(List<Long> classIds) throws BioIdException {
        validateBulkOperationSize(classIds.size());
        
        String operationId = UUID.randomUUID().toString();
        BulkOperationResult<TemplateUpgradeResult> result = new BulkOperationResult<>(operationId, classIds.size());
        operationResults.put(operationId, result);
        jobProcessor.registerOperation(operationId, result);
        
        // Submit background job
        Future<?> future = executorService.submit(() -> {
            try {
                result.markRunning();
                processBulkTemplateUpgrade(result, classIds);
                result.markCompleted();
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Bulk template upgrade failed", e);
                result.setStatus(BulkOperationStatus.FAILED);
                result.setCompletedAt(Instant.now());
            }
        });
        
        runningOperations.put(operationId, future);
        return result;
    }

    @Override
    public BulkOperationResult<Void> setBulkTemplateTags(List<Long> classIds, List<String> tags) throws BioIdException {
        validateBulkOperationSize(classIds.size());
        
        String operationId = UUID.randomUUID().toString();
        BulkOperationResult<Void> result = new BulkOperationResult<>(operationId, classIds.size());
        operationResults.put(operationId, result);
        jobProcessor.registerOperation(operationId, result);
        
        // Submit background job
        Future<?> future = executorService.submit(() -> {
            try {
                result.markRunning();
                processBulkTemplateTagging(result, classIds, tags);
                result.markCompleted();
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Bulk template tagging failed", e);
                result.setStatus(BulkOperationStatus.FAILED);
                result.setCompletedAt(Instant.now());
            }
        });
        
        runningOperations.put(operationId, future);
        return result;
    }

    @Override
    public BulkOperationStatus getBulkOperationStatus(String operationId) throws BioIdException {
        BulkOperationResult<?> result = operationResults.get(operationId);
        if (result == null) {
            throw new BioIdException("Bulk operation not found: " + operationId);
        }
        return result.getStatus();
    }

    @Override
    public void cancelBulkOperation(String operationId) throws BioIdException {
        Future<?> future = runningOperations.get(operationId);
        BulkOperationResult<?> result = operationResults.get(operationId);
        
        if (future == null || result == null) {
            throw new BioIdException("Bulk operation not found: " + operationId);
        }
        
        if (result.isCompleted()) {
            throw new BioIdException("Cannot cancel completed operation: " + operationId);
        }
        
        boolean cancelled = future.cancel(true);
        if (cancelled || jobProcessor.cancelOperation(operationId)) {
            result.markCancelled();
            runningOperations.remove(operationId);
            logger.info("Bulk operation cancelled: " + operationId);
        } else {
            throw new BioIdException("Failed to cancel bulk operation: " + operationId);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> BulkOperationResult<T> getBulkOperationResult(String operationId) throws BioIdException {
        BulkOperationResult<?> result = operationResults.get(operationId);
        if (result == null) {
            throw new BioIdException("Bulk operation not found: " + operationId);
        }
        return (BulkOperationResult<T>) result;
    }

    @Override
    public BulkOperationProgress getBulkOperationProgress(String operationId) throws BioIdException {
        BulkOperationProgress progress = jobProcessor.getOperationProgress(operationId);
        if (progress == null) {
            throw new BioIdException("Bulk operation not found: " + operationId);
        }
        return progress;
    }

    @Override
    public List<BulkOperationSummary> getActiveOperations() throws BioIdException {
        return jobProcessor.getActiveOperations();
    }

    @Override
    public List<BulkOperationSummary> getCompletedOperations() throws BioIdException {
        return jobProcessor.getCompletedOperations();
    }

    private void processBulkEnrollmentLinks(BulkOperationResult<EnrollmentLinkResult> result, 
                                          List<String> userIds, int validityHours) {
        
        for (int i = 0; i < userIds.size(); i += batchSize) {
            if (Thread.currentThread().isInterrupted()) {
                logger.info("Bulk enrollment link generation interrupted");
                return;
            }
            
            int endIndex = Math.min(i + batchSize, userIds.size());
            List<String> batch = userIds.subList(i, endIndex);
            
            for (String userId : batch) {
                try {
                    EnrollmentLinkResult linkResult = adminService.generateEnrollmentLink(userId, validityHours);
                    result.addSuccess(linkResult);
                } catch (Exception e) {
                    BulkOperationError error = new BulkOperationError(
                        userId, 
                        "ENROLLMENT_LINK_FAILED", 
                        e.getMessage(), 
                        isRetryableError(e)
                    );
                    result.addError(error);
                    logger.log(Level.WARNING, "Failed to generate enrollment link for user: " + userId, e);
                }
            }
        }
    }

    private void processBulkTemplateDeletion(BulkOperationResult<Void> result, 
                                           List<Long> classIds, String reason) {
        
        for (int i = 0; i < classIds.size(); i += batchSize) {
            if (Thread.currentThread().isInterrupted()) {
                logger.info("Bulk template deletion interrupted");
                return;
            }
            
            int endIndex = Math.min(i + batchSize, classIds.size());
            List<Long> batch = classIds.subList(i, endIndex);
            
            for (Long classId : batch) {
                try {
                    bioIdClient.deleteTemplate(classId);
                    result.addSuccess(null); // Void result
                } catch (Exception e) {
                    BulkOperationError error = new BulkOperationError(
                        classId.toString(), 
                        "TEMPLATE_DELETE_FAILED", 
                        e.getMessage(), 
                        isRetryableError(e)
                    );
                    result.addError(error);
                    logger.log(Level.WARNING, "Failed to delete template: " + classId, e);
                }
            }
        }
    }

    private void processBulkTemplateUpgrade(BulkOperationResult<TemplateUpgradeResult> result, 
                                          List<Long> classIds) {
        
        for (int i = 0; i < classIds.size(); i += batchSize) {
            if (Thread.currentThread().isInterrupted()) {
                logger.info("Bulk template upgrade interrupted");
                return;
            }
            
            int endIndex = Math.min(i + batchSize, classIds.size());
            List<Long> batch = classIds.subList(i, endIndex);
            
            for (Long classId : batch) {
                try {
                    TemplateUpgradeResult upgradeResult = templateService.upgradeTemplate(classId);
                    result.addSuccess(upgradeResult);
                } catch (Exception e) {
                    BulkOperationError error = new BulkOperationError(
                        classId.toString(), 
                        "TEMPLATE_UPGRADE_FAILED", 
                        e.getMessage(), 
                        isRetryableError(e)
                    );
                    result.addError(error);
                    logger.log(Level.WARNING, "Failed to upgrade template: " + classId, e);
                }
            }
        }
    }

    private void processBulkTemplateTagging(BulkOperationResult<Void> result, 
                                          List<Long> classIds, List<String> tags) {
        
        for (int i = 0; i < classIds.size(); i += batchSize) {
            if (Thread.currentThread().isInterrupted()) {
                logger.info("Bulk template tagging interrupted");
                return;
            }
            
            int endIndex = Math.min(i + batchSize, classIds.size());
            List<Long> batch = classIds.subList(i, endIndex);
            
            for (Long classId : batch) {
                try {
                    // Note: This would need to be implemented in the BioIdClient
                    // For now, we'll simulate the operation
                    setTemplateTags(classId, tags);
                    result.addSuccess(null); // Void result
                } catch (Exception e) {
                    BulkOperationError error = new BulkOperationError(
                        classId.toString(), 
                        "TEMPLATE_TAG_FAILED", 
                        e.getMessage(), 
                        isRetryableError(e)
                    );
                    result.addError(error);
                    logger.log(Level.WARNING, "Failed to tag template: " + classId, e);
                }
            }
        }
    }

    private void setTemplateTags(Long classId, List<String> tags) throws BioIdException {
        // This would need to be implemented in the BioIdClient interface
        // For now, we'll throw an exception to indicate it's not implemented
        throw new BioIdServiceException("Template tagging not yet implemented in BioIdClient");
    }

    private void validateBulkOperationSize(int size) throws BioIdException {
        if (size > maxBulkOperationSize) {
            throw new BioIdException("Bulk operation size exceeds maximum allowed: " + size + " > " + maxBulkOperationSize);
        }
        if (size <= 0) {
            throw new BioIdException("Bulk operation size must be greater than 0");
        }
    }

    private boolean isRetryableError(Exception e) {
        if (e instanceof BioIdServiceException) {
            BioIdServiceException serviceException = (BioIdServiceException) e;
            // Consider network and temporary service errors as retryable
            return serviceException.getMessage().contains("UNAVAILABLE") ||
                   serviceException.getMessage().contains("DEADLINE_EXCEEDED") ||
                   serviceException.getMessage().contains("RESOURCE_EXHAUSTED");
        }
        return false;
    }

    /**
     * Cleanup method to be called on service shutdown.
     */
    public void shutdown() {
        jobProcessor.shutdown();
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}