package com.bioid.keycloak.client.admin;

import com.bioid.keycloak.client.admin.model.BulkOperationProgress;
import com.bioid.keycloak.client.admin.model.BulkOperationResult;
import com.bioid.keycloak.client.admin.model.BulkOperationSummary;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Background job processor for managing long-running bulk operations.
 * Provides progress tracking, monitoring, and cleanup capabilities.
 */
public class BulkOperationJobProcessor {
    
    private static final Logger logger = Logger.getLogger(BulkOperationJobProcessor.class.getName());
    
    private final Map<String, BulkOperationResult<?>> activeOperations;
    private final Map<String, BulkOperationSummary> completedOperations;
    private final ScheduledExecutorService scheduledExecutor;
    
    // Configuration
    private final int progressUpdateIntervalSeconds;
    private final int completedOperationRetentionHours;
    private final int maxCompletedOperations;

    public BulkOperationJobProcessor() {
        this.activeOperations = new ConcurrentHashMap<>();
        this.completedOperations = new ConcurrentHashMap<>();
        this.scheduledExecutor = Executors.newScheduledThreadPool(2);
        this.progressUpdateIntervalSeconds = 5; // Update progress every 5 seconds
        this.completedOperationRetentionHours = 24; // Keep completed operations for 24 hours
        this.maxCompletedOperations = 1000; // Maximum number of completed operations to retain
    }
    
    /**
     * Initialize the processor and start background tasks.
     * This should be called after construction to avoid 'this' escape.
     */
    public void initialize() {
        startProgressMonitoring();
        startCleanupTask();
    }

    /**
     * Registers a bulk operation for monitoring.
     *
     * @param operationId the unique identifier of the operation
     * @param result the bulk operation result to monitor
     */
    public void registerOperation(String operationId, BulkOperationResult<?> result) {
        activeOperations.put(operationId, result);
        logger.info("Registered bulk operation for monitoring: " + operationId);
    }

    /**
     * Gets the current progress of a bulk operation.
     *
     * @param operationId the unique identifier of the operation
     * @return progress information or null if operation not found
     */
    public BulkOperationProgress getOperationProgress(String operationId) {
        BulkOperationResult<?> result = activeOperations.get(operationId);
        if (result == null) {
            return null;
        }

        return new BulkOperationProgress(
            operationId,
            result.getStatus(),
            result.getTotalItems(),
            result.getProcessedItems(),
            result.getSuccessfulItems(),
            result.getFailedItems(),
            result.getStartedAt(),
            getCurrentPhase(result)
        );
    }

    /**
     * Gets a summary of all active bulk operations.
     *
     * @return list of active operation summaries
     */
    public List<BulkOperationSummary> getActiveOperations() {
        List<BulkOperationSummary> summaries = new ArrayList<>();
        
        for (Map.Entry<String, BulkOperationResult<?>> entry : activeOperations.entrySet()) {
            BulkOperationResult<?> result = entry.getValue();
            BulkOperationSummary summary = new BulkOperationSummary(
                entry.getKey(),
                getOperationType(result),
                result.getStatus(),
                result.getTotalItems(),
                result.getProcessedItems(),
                result.getSuccessfulItems(),
                result.getFailedItems(),
                result.getStartedAt(),
                result.getCompletedAt(),
                "system" // TODO: Track actual initiator
            );
            summaries.add(summary);
        }
        
        return summaries;
    }

    /**
     * Gets a summary of completed bulk operations.
     *
     * @return list of completed operation summaries
     */
    public List<BulkOperationSummary> getCompletedOperations() {
        return new ArrayList<>(completedOperations.values());
    }

    /**
     * Cancels an active bulk operation.
     *
     * @param operationId the unique identifier of the operation to cancel
     * @return true if the operation was successfully cancelled, false otherwise
     */
    public boolean cancelOperation(String operationId) {
        BulkOperationResult<?> result = activeOperations.get(operationId);
        if (result == null || result.isCompleted()) {
            return false;
        }

        result.markCancelled();
        logger.info("Bulk operation cancelled: " + operationId);
        return true;
    }

    /**
     * Removes a completed operation from active monitoring.
     *
     * @param operationId the unique identifier of the operation
     */
    public void completeOperation(String operationId) {
        BulkOperationResult<?> result = activeOperations.remove(operationId);
        if (result != null && result.isCompleted()) {
            BulkOperationSummary summary = new BulkOperationSummary(
                operationId,
                getOperationType(result),
                result.getStatus(),
                result.getTotalItems(),
                result.getProcessedItems(),
                result.getSuccessfulItems(),
                result.getFailedItems(),
                result.getStartedAt(),
                result.getCompletedAt(),
                "system" // TODO: Track actual initiator
            );
            completedOperations.put(operationId, summary);
            logger.info("Bulk operation completed and moved to history: " + operationId);
        }
    }

    private void startProgressMonitoring() {
        scheduledExecutor.scheduleAtFixedRate(() -> {
            try {
                monitorActiveOperations();
            } catch (Exception e) {
                logger.severe("Error monitoring bulk operations: " + e.getMessage());
            }
        }, progressUpdateIntervalSeconds, progressUpdateIntervalSeconds, TimeUnit.SECONDS);
    }

    private void startCleanupTask() {
        scheduledExecutor.scheduleAtFixedRate(() -> {
            try {
                cleanupCompletedOperations();
            } catch (Exception e) {
                logger.severe("Error cleaning up completed operations: " + e.getMessage());
            }
        }, 1, 1, TimeUnit.HOURS);
    }

    private void monitorActiveOperations() {
        List<String> completedOperationIds = new ArrayList<>();
        
        for (Map.Entry<String, BulkOperationResult<?>> entry : activeOperations.entrySet()) {
            String operationId = entry.getKey();
            BulkOperationResult<?> result = entry.getValue();
            
            if (result.isCompleted()) {
                completedOperationIds.add(operationId);
            } else {
                // Log progress for long-running operations
                if (result.getDurationMillis() > 60000) { // More than 1 minute
                    logger.info(String.format("Bulk operation %s progress: %.1f%% (%d/%d items processed)",
                        operationId, result.getProgressPercentage(), result.getProcessedItems(), result.getTotalItems()));
                }
            }
        }
        
        // Move completed operations to history
        for (String operationId : completedOperationIds) {
            completeOperation(operationId);
        }
    }

    private void cleanupCompletedOperations() {
        Instant cutoffTime = Instant.now().minusSeconds(completedOperationRetentionHours * 3600L);
        List<String> operationsToRemove = new ArrayList<>();
        
        for (Map.Entry<String, BulkOperationSummary> entry : completedOperations.entrySet()) {
            BulkOperationSummary summary = entry.getValue();
            if (summary.getCompletedAt() != null && summary.getCompletedAt().isBefore(cutoffTime)) {
                operationsToRemove.add(entry.getKey());
            }
        }
        
        // Also remove oldest operations if we exceed the maximum
        if (completedOperations.size() > maxCompletedOperations) {
            List<Map.Entry<String, BulkOperationSummary>> sortedEntries = new ArrayList<>(completedOperations.entrySet());
            sortedEntries.sort((e1, e2) -> {
                Instant t1 = e1.getValue().getCompletedAt();
                Instant t2 = e2.getValue().getCompletedAt();
                if (t1 == null && t2 == null) return 0;
                if (t1 == null) return 1;
                if (t2 == null) return -1;
                return t1.compareTo(t2);
            });
            
            int excessCount = completedOperations.size() - maxCompletedOperations;
            for (int i = 0; i < excessCount; i++) {
                operationsToRemove.add(sortedEntries.get(i).getKey());
            }
        }
        
        for (String operationId : operationsToRemove) {
            completedOperations.remove(operationId);
        }
        
        if (!operationsToRemove.isEmpty()) {
            logger.info("Cleaned up " + operationsToRemove.size() + " old completed operations");
        }
    }

    private String getCurrentPhase(BulkOperationResult<?> result) {
        switch (result.getStatus()) {
            case PENDING:
                return "Queued";
            case RUNNING:
                double progress = result.getProgressPercentage();
                if (progress < 25) return "Starting";
                if (progress < 75) return "Processing";
                return "Finalizing";
            case COMPLETED:
                return "Completed";
            case FAILED:
                return "Failed";
            case CANCELLED:
                return "Cancelled";
            case PARTIALLY_COMPLETED:
                return "Partially Completed";
            default:
                return "Unknown";
        }
    }

    private String getOperationType(BulkOperationResult<?> result) {
        // This is a simplified approach - in a real implementation,
        // you might want to store the operation type in the result
        if (!result.getResults().isEmpty()) {
            Object firstResult = result.getResults().get(0);
            if (firstResult == null) {
                return "Bulk Delete"; // Void results typically indicate deletion
            }
            return firstResult.getClass().getSimpleName().replace("Result", "");
        }
        return "Unknown";
    }

    /**
     * Shutdown the job processor and cleanup resources.
     */
    public void shutdown() {
        scheduledExecutor.shutdown();
        try {
            if (!scheduledExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                scheduledExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduledExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}