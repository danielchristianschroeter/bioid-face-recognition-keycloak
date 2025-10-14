package com.bioid.keycloak.reconciliation;

import com.bioid.keycloak.error.AdminException;
import com.bioid.keycloak.error.AdminErrorType;
import com.bioid.keycloak.error.AdminErrorHandler;
import com.bioid.keycloak.config.AdminConfiguration;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for automated cleanup of orphaned data and consistency maintenance
 */
@ApplicationScoped
@SuppressWarnings("all")
public class AutomatedCleanupService {

    private static final Logger logger = Logger.getLogger(AutomatedCleanupService.class);

    @Inject
    private DataConsistencyService consistencyService;

    @Inject
    private AdminErrorHandler errorHandler;

    @Inject
    private AdminConfiguration adminConfiguration;

    private KeycloakSession session;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final Map<String, CleanupSchedule> activeSchedules = new ConcurrentHashMap<>();
    private final Map<String, CleanupHistory> cleanupHistory = new ConcurrentHashMap<>();

    public void setSession(KeycloakSession session) {
        this.session = session;
    }

    /**
     * Schedule automated cleanup for a realm
     */
    public void scheduleAutomatedCleanup(RealmModel realm, CleanupConfiguration config) {
        String realmId = realm.getId();
        logger.infof("Scheduling automated cleanup for realm: %s", realm.getName());

        // Cancel existing schedule if any
        cancelScheduledCleanup(realmId);

        CleanupSchedule schedule = new CleanupSchedule();
        schedule.setRealmId(realmId);
        schedule.setRealmName(realm.getName());
        schedule.setConfiguration(config);
        schedule.setScheduledAt(Instant.now());
        schedule.setNextRunAt(calculateNextRunTime(config));

        // Schedule the cleanup task
        schedule.setScheduledFuture(scheduler.scheduleAtFixedRate(
            () -> performScheduledCleanup(realmId),
            config.getInitialDelayHours(),
            config.getIntervalHours(),
            TimeUnit.HOURS
        ));

        activeSchedules.put(realmId, schedule);
        logger.infof("Automated cleanup scheduled for realm %s: interval=%d hours, next run=%s", 
            realm.getName(), config.getIntervalHours(), schedule.getNextRunAt());
    }

    /**
     * Cancel scheduled cleanup for a realm
     */
    public void cancelScheduledCleanup(String realmId) {
        CleanupSchedule schedule = activeSchedules.remove(realmId);
        if (schedule != null && schedule.getScheduledFuture() != null) {
            schedule.getScheduledFuture().cancel(false);
            logger.infof("Cancelled automated cleanup for realm: %s", schedule.getRealmName());
        }
    }

    /**
     * Get cleanup schedule for a realm
     */
    public CleanupSchedule getCleanupSchedule(String realmId) {
        return activeSchedules.get(realmId);
    }

    /**
     * Get cleanup history for a realm
     */
    public CleanupHistory getCleanupHistory(String realmId) {
        return cleanupHistory.get(realmId);
    }

    /**
     * Perform immediate cleanup check for all realms
     */
    public void performImmediateCleanupCheck() {
        logger.info("Starting immediate cleanup check for all realms");
        
        session.realms().getRealmsStream().forEach(realm -> {
            try {
                performScheduledCleanup(realm.getId());
            } catch (Exception e) {
                logger.errorf(e, "Failed to perform cleanup check for realm: %s", realm.getName());
            }
        });
    }

    /**
     * Perform scheduled cleanup for a specific realm
     */
    private void performScheduledCleanup(String realmId) {
        try {
            RealmModel realm = session.realms().getRealm(realmId);
            if (realm == null) {
                logger.warnf("Realm not found for scheduled cleanup: %s", realmId);
                cancelScheduledCleanup(realmId);
                return;
            }

            logger.infof("Starting scheduled cleanup for realm: %s", realm.getName());

            CleanupSchedule schedule = activeSchedules.get(realmId);
            if (schedule == null) {
                logger.warnf("No cleanup schedule found for realm: %s", realmId);
                return;
            }

            CleanupConfiguration config = schedule.getConfiguration();
            CleanupHistory history = cleanupHistory.computeIfAbsent(realmId, 
                k -> new CleanupHistory(realmId, realm.getName()));

            // Perform consistency check
            DataConsistencyReport consistencyReport = errorHandler.executeWithRetry(() -> 
                consistencyService.performConsistencyCheck(realm));

            // Determine if cleanup is needed
            if (shouldPerformCleanup(consistencyReport, config)) {
                // Perform cleanup (dry run first if configured)
                CleanupResult cleanupResult;
                if (config.isDryRunFirst()) {
                    logger.infof("Performing dry run cleanup for realm: %s", realm.getName());
                    cleanupResult = consistencyService.cleanupOrphanedData(realm, true);
                    
                    if (cleanupResult.getTotalItemsProcessed() > 0) {
                        logger.infof("Dry run found %d items to clean in realm %s", 
                            cleanupResult.getTotalItemsProcessed(), realm.getName());
                        
                        if (config.isAutoApproveCleanup()) {
                            logger.infof("Auto-approving cleanup for realm: %s", realm.getName());
                            cleanupResult = consistencyService.cleanupOrphanedData(realm, false);
                        } else {
                            logger.infof("Cleanup requires manual approval for realm: %s", realm.getName());
                            // Store dry run result for manual review
                            history.addDryRunResult(cleanupResult);
                            return;
                        }
                    }
                } else {
                    cleanupResult = consistencyService.cleanupOrphanedData(realm, false);
                }

                // Record cleanup result
                history.addCleanupResult(cleanupResult);
                schedule.setLastRunAt(Instant.now());
                schedule.setNextRunAt(calculateNextRunTime(config));

                logger.infof("Scheduled cleanup completed for realm %s: %d items processed, %d errors", 
                    realm.getName(), cleanupResult.getTotalItemsProcessed(), cleanupResult.getErrors().size());

                // Send notifications if configured
                if (config.isNotifyOnCompletion()) {
                    sendCleanupNotification(realm, cleanupResult);
                }

            } else {
                logger.debugf("No cleanup needed for realm: %s", realm.getName());
                schedule.setLastRunAt(Instant.now());
                schedule.setNextRunAt(calculateNextRunTime(config));
            }

        } catch (Exception e) {
            logger.errorf(e, "Scheduled cleanup failed for realm: %s", realmId);
            
            CleanupHistory history = cleanupHistory.get(realmId);
            if (history != null) {
                history.addError(new CleanupExecutionError(Instant.now(), e.getMessage()));
            }
        }
    }

    /**
     * Determine if cleanup should be performed based on consistency report
     */
    private boolean shouldPerformCleanup(DataConsistencyReport report, CleanupConfiguration config) {
        if (report.getTotalIssues() == 0) {
            return false;
        }

        // Check thresholds
        int orphanedCredentials = report.getIssuesByType(ConsistencyIssueType.ORPHANED_CREDENTIAL).size();
        int orphanedTemplates = report.getIssuesByType(ConsistencyIssueType.ORPHANED_TEMPLATE).size();

        return orphanedCredentials >= config.getMinOrphanedCredentialsThreshold() ||
               orphanedTemplates >= config.getMinOrphanedTemplatesThreshold() ||
               report.hasHighSeverityIssues();
    }

    /**
     * Calculate next run time based on configuration
     */
    private Instant calculateNextRunTime(CleanupConfiguration config) {
        return Instant.now().plus(config.getIntervalHours(), ChronoUnit.HOURS);
    }

    /**
     * Send cleanup notification (placeholder implementation)
     */
    private void sendCleanupNotification(RealmModel realm, CleanupResult result) {
        // Implementation would depend on notification system
        logger.infof("Cleanup notification sent for realm %s: %d items processed", 
            realm.getName(), result.getTotalItemsProcessed());
    }

    /**
     * Shutdown the scheduler
     */
    public void shutdown() {
        logger.info("Shutting down automated cleanup service");
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(30, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Configuration for automated cleanup
     */
    public static class CleanupConfiguration {
        private int intervalHours = 24; // Daily by default
        private int initialDelayHours = 1;
        private boolean dryRunFirst = true;
        private boolean autoApproveCleanup = false;
        private boolean notifyOnCompletion = true;
        private int minOrphanedCredentialsThreshold = 1;
        private int minOrphanedTemplatesThreshold = 1;
        private int maxItemsPerCleanup = 100;

        // Getters and setters
        public int getIntervalHours() { return intervalHours; }
        public void setIntervalHours(int intervalHours) { this.intervalHours = intervalHours; }

        public int getInitialDelayHours() { return initialDelayHours; }
        public void setInitialDelayHours(int initialDelayHours) { this.initialDelayHours = initialDelayHours; }

        public boolean isDryRunFirst() { return dryRunFirst; }
        public void setDryRunFirst(boolean dryRunFirst) { this.dryRunFirst = dryRunFirst; }

        public boolean isAutoApproveCleanup() { return autoApproveCleanup; }
        public void setAutoApproveCleanup(boolean autoApproveCleanup) { this.autoApproveCleanup = autoApproveCleanup; }

        public boolean isNotifyOnCompletion() { return notifyOnCompletion; }
        public void setNotifyOnCompletion(boolean notifyOnCompletion) { this.notifyOnCompletion = notifyOnCompletion; }

        public int getMinOrphanedCredentialsThreshold() { return minOrphanedCredentialsThreshold; }
        public void setMinOrphanedCredentialsThreshold(int minOrphanedCredentialsThreshold) { 
            this.minOrphanedCredentialsThreshold = minOrphanedCredentialsThreshold; 
        }

        public int getMinOrphanedTemplatesThreshold() { return minOrphanedTemplatesThreshold; }
        public void setMinOrphanedTemplatesThreshold(int minOrphanedTemplatesThreshold) { 
            this.minOrphanedTemplatesThreshold = minOrphanedTemplatesThreshold; 
        }

        public int getMaxItemsPerCleanup() { return maxItemsPerCleanup; }
        public void setMaxItemsPerCleanup(int maxItemsPerCleanup) { this.maxItemsPerCleanup = maxItemsPerCleanup; }
    }

    /**
     * Cleanup schedule information
     */
    public static class CleanupSchedule {
        private String realmId;
        private String realmName;
        private CleanupConfiguration configuration;
        private Instant scheduledAt;
        private Instant lastRunAt;
        private Instant nextRunAt;
        private java.util.concurrent.ScheduledFuture<?> scheduledFuture;

        // Getters and setters
        public String getRealmId() { return realmId; }
        public void setRealmId(String realmId) { this.realmId = realmId; }

        public String getRealmName() { return realmName; }
        public void setRealmName(String realmName) { this.realmName = realmName; }

        public CleanupConfiguration getConfiguration() { return configuration; }
        public void setConfiguration(CleanupConfiguration configuration) { this.configuration = configuration; }

        public Instant getScheduledAt() { return scheduledAt; }
        public void setScheduledAt(Instant scheduledAt) { this.scheduledAt = scheduledAt; }

        public Instant getLastRunAt() { return lastRunAt; }
        public void setLastRunAt(Instant lastRunAt) { this.lastRunAt = lastRunAt; }

        public Instant getNextRunAt() { return nextRunAt; }
        public void setNextRunAt(Instant nextRunAt) { this.nextRunAt = nextRunAt; }

        public java.util.concurrent.ScheduledFuture<?> getScheduledFuture() { return scheduledFuture; }
        public void setScheduledFuture(java.util.concurrent.ScheduledFuture<?> scheduledFuture) { 
            this.scheduledFuture = scheduledFuture; 
        }
    }

    /**
     * Cleanup execution history
     */
    public static class CleanupHistory {
        private String realmId;
        private String realmName;
        private List<CleanupResult> cleanupResults = new ArrayList<>();
        private List<CleanupResult> dryRunResults = new ArrayList<>();
        private List<CleanupExecutionError> errors = new ArrayList<>();

        public CleanupHistory(String realmId, String realmName) {
            this.realmId = realmId;
            this.realmName = realmName;
        }

        public void addCleanupResult(CleanupResult result) {
            cleanupResults.add(result);
            // Keep only last 50 results
            if (cleanupResults.size() > 50) {
                cleanupResults.remove(0);
            }
        }

        public void addDryRunResult(CleanupResult result) {
            dryRunResults.add(result);
            // Keep only last 20 dry run results
            if (dryRunResults.size() > 20) {
                dryRunResults.remove(0);
            }
        }

        public void addError(CleanupExecutionError error) {
            errors.add(error);
            // Keep only last 20 errors
            if (errors.size() > 20) {
                errors.remove(0);
            }
        }

        // Getters
        public String getRealmId() { return realmId; }
        public String getRealmName() { return realmName; }
        public List<CleanupResult> getCleanupResults() { return new ArrayList<>(cleanupResults); }
        public List<CleanupResult> getDryRunResults() { return new ArrayList<>(dryRunResults); }
        public List<CleanupExecutionError> getErrors() { return new ArrayList<>(errors); }
    }

    /**
     * Cleanup execution error
     */
    public static class CleanupExecutionError {
        private Instant occurredAt;
        private String errorMessage;

        public CleanupExecutionError(Instant occurredAt, String errorMessage) {
            this.occurredAt = occurredAt;
            this.errorMessage = errorMessage;
        }

        public Instant getOccurredAt() { return occurredAt; }
        public String getErrorMessage() { return errorMessage; }
    }
}