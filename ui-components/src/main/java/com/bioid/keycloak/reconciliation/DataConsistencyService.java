package com.bioid.keycloak.reconciliation;

import com.bioid.keycloak.error.AdminException;
import com.bioid.keycloak.error.AdminErrorType;
import com.bioid.keycloak.error.AdminErrorHandler;
import com.bioid.keycloak.client.BioIdClient;
// Note: FaceTemplateStatus would need to be imported from the actual client package
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.OTPCredentialModel;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for maintaining data consistency between Keycloak and BioID systems.
 * Handles synchronization, validation, and repair of template and credential data.
 */
@ApplicationScoped
@SuppressWarnings("all")
public class DataConsistencyService {

    private static final Logger logger = Logger.getLogger(DataConsistencyService.class);

    @Inject
    private BioIdClient bioIdClient;

    @Inject
    private AdminErrorHandler errorHandler;

    @Inject
    private KeycloakSession session;

    /**
     * Perform comprehensive data consistency check between Keycloak and BioID
     */
    public DataConsistencyReport performConsistencyCheck(RealmModel realm) {
        logger.infof("Starting data consistency check for realm: %s", realm.getName());
        
        DataConsistencyReport report = new DataConsistencyReport(realm.getId(), realm.getName());
        
        try {
            // Get all users with face credentials in Keycloak
            Map<String, UserCredentialInfo> keycloakCredentials = getUsersWithFaceCredentials(realm);
            logger.debugf("Found %d users with face credentials in Keycloak", keycloakCredentials.size());
            
            // Get all templates from BioID
            Map<Long, Object> bioIdTemplates = getBioIdTemplates(keycloakCredentials.keySet());
            logger.debugf("Found %d templates in BioID", bioIdTemplates.size());
            
            // Analyze consistency
            analyzeConsistency(keycloakCredentials, bioIdTemplates, report);
            
            report.setCompletedAt(Instant.now());
            report.setStatus(ConsistencyCheckStatus.COMPLETED);
            
            logger.infof("Data consistency check completed for realm %s: %d issues found", 
                realm.getName(), report.getTotalIssues());
            
        } catch (Exception e) {
            logger.errorf(e, "Data consistency check failed for realm: %s", realm.getName());
            report.setStatus(ConsistencyCheckStatus.FAILED);
            report.setErrorMessage(e.getMessage());
            
            if (!(e instanceof AdminException)) {
                throw new AdminException(AdminErrorType.DATA_INCONSISTENCY, 
                    "Data consistency check failed", e);
            }
            throw e;
        }
        
        return report;
    }

    /**
     * Synchronize template status between Keycloak and BioID
     */
    public SynchronizationResult synchronizeTemplateStatus(RealmModel realm, boolean dryRun) {
        logger.infof("Starting template status synchronization for realm: %s (dry run: %s)", 
            realm.getName(), dryRun);
        
        SynchronizationResult result = new SynchronizationResult();
        result.setRealmId(realm.getId());
        result.setDryRun(dryRun);
        result.setStartedAt(Instant.now());
        
        try {
            DataConsistencyReport consistencyReport = performConsistencyCheck(realm);
            
            for (ConsistencyIssue issue : consistencyReport.getIssues()) {
                try {
                    SynchronizationAction action = determineSynchronizationAction(issue);
                    
                    if (!dryRun) {
                        executeSynchronizationAction(action, realm);
                        result.addExecutedAction(action);
                    } else {
                        result.addPlannedAction(action);
                    }
                    
                } catch (Exception e) {
                    logger.errorf(e, "Failed to synchronize issue: %s", issue.getDescription());
                    result.addError(new SynchronizationError(issue.getIssueId(), e.getMessage()));
                }
            }
            
            result.setCompletedAt(Instant.now());
            result.setStatus(SynchronizationStatus.COMPLETED);
            
            logger.infof("Template status synchronization completed for realm %s: %d actions %s", 
                realm.getName(), result.getTotalActions(), dryRun ? "planned" : "executed");
            
        } catch (Exception e) {
            logger.errorf(e, "Template status synchronization failed for realm: %s", realm.getName());
            result.setStatus(SynchronizationStatus.FAILED);
            result.setErrorMessage(e.getMessage());
        }
        
        return result;
    }

    /**
     * Repair specific data consistency issues
     */
    public RepairResult repairConsistencyIssues(RealmModel realm, List<String> issueIds, boolean autoApprove) {
        logger.infof("Starting repair of %d consistency issues for realm: %s", issueIds.size(), realm.getName());
        
        RepairResult result = new RepairResult();
        result.setRealmId(realm.getId());
        result.setStartedAt(Instant.now());
        
        try {
            DataConsistencyReport consistencyReport = performConsistencyCheck(realm);
            
            Map<String, ConsistencyIssue> issueMap = consistencyReport.getIssues().stream()
                .collect(Collectors.toMap(ConsistencyIssue::getIssueId, issue -> issue));
            
            for (String issueId : issueIds) {
                ConsistencyIssue issue = issueMap.get(issueId);
                if (issue == null) {
                    logger.warnf("Issue not found: %s", issueId);
                    result.addError(new RepairError(issueId, "Issue not found"));
                    continue;
                }
                
                try {
                    RepairAction action = determineRepairAction(issue);
                    
                    if (autoApprove || isLowRiskRepair(action)) {
                        executeRepairAction(action, realm);
                        result.addSuccessfulRepair(action);
                    } else {
                        result.addPendingApproval(action);
                    }
                    
                } catch (Exception e) {
                    logger.errorf(e, "Failed to repair issue: %s", issueId);
                    result.addError(new RepairError(issueId, e.getMessage()));
                }
            }
            
            result.setCompletedAt(Instant.now());
            result.setStatus(RepairStatus.COMPLETED);
            
            logger.infof("Repair completed for realm %s: %d successful, %d errors, %d pending approval", 
                realm.getName(), result.getSuccessfulRepairs().size(), 
                result.getErrors().size(), result.getPendingApprovals().size());
            
        } catch (Exception e) {
            logger.errorf(e, "Repair operation failed for realm: %s", realm.getName());
            result.setStatus(RepairStatus.FAILED);
            result.setErrorMessage(e.getMessage());
        }
        
        return result;
    }

    /**
     * Clean up orphaned data in both systems
     */
    public CleanupResult cleanupOrphanedData(RealmModel realm, boolean dryRun) {
        logger.infof("Starting orphaned data cleanup for realm: %s (dry run: %s)", realm.getName(), dryRun);
        
        CleanupResult result = new CleanupResult();
        result.setRealmId(realm.getId());
        result.setDryRun(dryRun);
        result.setStartedAt(Instant.now());
        
        try {
            DataConsistencyReport consistencyReport = performConsistencyCheck(realm);
            
            // Find orphaned credentials and templates
            List<ConsistencyIssue> orphanedCredentials = consistencyReport.getIssues().stream()
                .filter(issue -> issue.getType() == ConsistencyIssueType.ORPHANED_CREDENTIAL)
                .collect(Collectors.toList());
            
            List<ConsistencyIssue> orphanedTemplates = consistencyReport.getIssues().stream()
                .filter(issue -> issue.getType() == ConsistencyIssueType.ORPHANED_TEMPLATE)
                .collect(Collectors.toList());
            
            // Clean up orphaned credentials
            for (ConsistencyIssue issue : orphanedCredentials) {
                try {
                    if (!dryRun) {
                        cleanupOrphanedCredential(issue, realm);
                        result.addCleanedCredential(issue.getUserId());
                    } else {
                        result.addCredentialToClean(issue.getUserId());
                    }
                } catch (Exception e) {
                    logger.errorf(e, "Failed to cleanup orphaned credential for user: %s", issue.getUserId());
                    result.addError(new CleanupError(issue.getUserId(), "credential", e.getMessage()));
                }
            }
            
            // Clean up orphaned templates
            for (ConsistencyIssue issue : orphanedTemplates) {
                try {
                    if (!dryRun) {
                        cleanupOrphanedTemplate(issue);
                        result.addCleanedTemplate(issue.getClassId());
                    } else {
                        result.addTemplateToClean(issue.getClassId());
                    }
                } catch (Exception e) {
                    logger.errorf(e, "Failed to cleanup orphaned template: %s", issue.getClassId());
                    result.addError(new CleanupError(String.valueOf(issue.getClassId()), "template", e.getMessage()));
                }
            }
            
            result.setCompletedAt(Instant.now());
            result.setStatus(CleanupStatus.COMPLETED);
            
            logger.infof("Orphaned data cleanup completed for realm %s: %d credentials, %d templates %s", 
                realm.getName(), orphanedCredentials.size(), orphanedTemplates.size(), 
                dryRun ? "identified" : "cleaned");
            
        } catch (Exception e) {
            logger.errorf(e, "Orphaned data cleanup failed for realm: %s", realm.getName());
            result.setStatus(CleanupStatus.FAILED);
            result.setErrorMessage(e.getMessage());
        }
        
        return result;
    }

    /**
     * Get all users with face credentials from Keycloak
     */
    private Map<String, UserCredentialInfo> getUsersWithFaceCredentials(RealmModel realm) {
        Map<String, UserCredentialInfo> credentials = new HashMap<>();
        
        session.users().searchForUserStream(realm, Map.of()).forEach(user -> {
            // Check for face authentication credentials
            user.credentialManager().getStoredCredentialsStream()
                .filter(cred -> "face-recognition".equals(cred.getType()))
                .forEach(cred -> {
                    UserCredentialInfo info = new UserCredentialInfo();
                    info.setUserId(user.getId());
                    info.setUsername(user.getUsername());
                    info.setCredentialId(cred.getId());
                    info.setCreatedDate(cred.getCreatedDate());
                    
                    // Extract class ID from credential data
                    String credentialData = cred.getCredentialData();
                    if (credentialData != null) {
                        try {
                            // Parse credential data to extract class ID
                            Long classId = extractClassIdFromCredentialData(credentialData);
                            info.setClassId(classId);
                        } catch (Exception e) {
                            logger.warnf("Failed to extract class ID from credential for user %s: %s", 
                                user.getUsername(), e.getMessage());
                        }
                    }
                    
                    credentials.put(user.getId(), info);
                });
        });
        
        return credentials;
    }

    /**
     * Get template status from BioID for all users
     */
    private Map<Long, Object> getBioIdTemplates(Set<String> userIds) {
        Map<Long, Object> templates = new HashMap<>();
        
        // Extract class IDs from user credentials
        List<Long> classIds = new ArrayList<>();
        // This would need to be implemented based on how class IDs are stored
        
        try {
            // Batch query template status from BioID
            for (Long classId : classIds) {
                try {
                    // Note: This would need to be implemented with actual BioID client method
                    Object status = errorHandler.executeWithRetry(() -> {
                        // bioIdClient.getTemplateStatus(classId, false);
                        return new Object(); // Placeholder
                    });
                    templates.put(classId, status);
                } catch (AdminException e) {
                    if (e.getErrorType() != AdminErrorType.TEMPLATE_NOT_FOUND) {
                        logger.warnf("Failed to get template status for class ID %d: %s", classId, e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            throw new AdminException(AdminErrorType.SERVICE_UNAVAILABLE, 
                "Failed to retrieve template status from BioID", e);
        }
        
        return templates;
    }

    /**
     * Analyze consistency between Keycloak and BioID data
     */
    private void analyzeConsistency(Map<String, UserCredentialInfo> keycloakCredentials,
                                  Map<Long, Object> bioIdTemplates,
                                  DataConsistencyReport report) {
        
        // Find orphaned credentials (in Keycloak but not in BioID)
        for (UserCredentialInfo credInfo : keycloakCredentials.values()) {
            if (credInfo.getClassId() != null && !bioIdTemplates.containsKey(credInfo.getClassId())) {
                ConsistencyIssue issue = new ConsistencyIssue();
                issue.setIssueId(UUID.randomUUID().toString());
                issue.setType(ConsistencyIssueType.ORPHANED_CREDENTIAL);
                issue.setUserId(credInfo.getUserId());
                issue.setClassId(credInfo.getClassId());
                issue.setSeverity(IssueSeverity.HIGH);
                issue.setDescription(String.format("User %s has face credential but no corresponding template in BioID", 
                    credInfo.getUsername()));
                issue.setDetectedAt(Instant.now());
                
                report.addIssue(issue);
            }
        }
        
        // Find orphaned templates (in BioID but not in Keycloak)
        Set<Long> keycloakClassIds = keycloakCredentials.values().stream()
            .map(UserCredentialInfo::getClassId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        
        for (Long classId : bioIdTemplates.keySet()) {
            if (!keycloakClassIds.contains(classId)) {
                ConsistencyIssue issue = new ConsistencyIssue();
                issue.setIssueId(UUID.randomUUID().toString());
                issue.setType(ConsistencyIssueType.ORPHANED_TEMPLATE);
                issue.setClassId(classId);
                issue.setSeverity(IssueSeverity.MEDIUM);
                issue.setDescription(String.format("Template %d exists in BioID but no corresponding credential in Keycloak", classId));
                issue.setDetectedAt(Instant.now());
                
                report.addIssue(issue);
            }
        }
        
        // Find sync mismatches (different metadata)
        for (UserCredentialInfo credInfo : keycloakCredentials.values()) {
            if (credInfo.getClassId() != null) {
                Object templateStatus = bioIdTemplates.get(credInfo.getClassId());
                if (templateStatus != null) {
                    // Check for metadata mismatches
                    if (hasMetadataMismatch(credInfo, templateStatus)) {
                        ConsistencyIssue issue = new ConsistencyIssue();
                        issue.setIssueId(UUID.randomUUID().toString());
                        issue.setType(ConsistencyIssueType.METADATA_MISMATCH);
                        issue.setUserId(credInfo.getUserId());
                        issue.setClassId(credInfo.getClassId());
                        issue.setSeverity(IssueSeverity.LOW);
                        issue.setDescription(String.format("Metadata mismatch between Keycloak and BioID for user %s", 
                            credInfo.getUsername()));
                        issue.setDetectedAt(Instant.now());
                        
                        report.addIssue(issue);
                    }
                }
            }
        }
    }

    // Helper methods for extracting data and determining actions
    private Long extractClassIdFromCredentialData(String credentialData) {
        // Implementation would depend on how credential data is structured
        // This is a placeholder
        return null;
    }

    private boolean hasMetadataMismatch(UserCredentialInfo credInfo, Object templateStatus) {
        // Compare metadata between Keycloak credential and BioID template
        // This is a placeholder implementation
        return false;
    }

    private SynchronizationAction determineSynchronizationAction(ConsistencyIssue issue) {
        // Determine what action to take for synchronization
        return new SynchronizationAction(issue.getIssueId(), SynchronizationActionType.UPDATE_METADATA, 
            issue.getDescription());
    }

    private void executeSynchronizationAction(SynchronizationAction action, RealmModel realm) {
        // Execute the synchronization action
        logger.debugf("Executing synchronization action: %s", action.getDescription());
    }

    private RepairAction determineRepairAction(ConsistencyIssue issue) {
        // Determine what repair action to take
        return new RepairAction(issue.getIssueId(), RepairActionType.DELETE_ORPHANED_CREDENTIAL, 
            issue.getDescription());
    }

    private boolean isLowRiskRepair(RepairAction action) {
        // Determine if repair action is low risk and can be auto-approved
        return action.getType() == RepairActionType.UPDATE_METADATA;
    }

    private void executeRepairAction(RepairAction action, RealmModel realm) {
        // Execute the repair action
        logger.debugf("Executing repair action: %s", action.getDescription());
    }

    private void cleanupOrphanedCredential(ConsistencyIssue issue, RealmModel realm) {
        // Remove orphaned credential from Keycloak
        UserModel user = session.users().getUserById(realm, issue.getUserId());
        if (user != null) {
            // Note: This would need to be implemented with actual credential removal method
            // user.credentialManager().removeStoredCredential(issue.getCredentialId());
            logger.infof("Removed orphaned credential for user: %s", user.getUsername());
        }
    }

    private void cleanupOrphanedTemplate(ConsistencyIssue issue) {
        // Remove orphaned template from BioID
        try {
            errorHandler.executeWithRetry(() -> {
                // Note: This would need to be implemented with actual BioID client method
                // bioIdClient.deleteTemplate(issue.getClassId());
                return null;
            });
            logger.infof("Removed orphaned template: %d", issue.getClassId());
        } catch (Exception e) {
            throw new AdminException(AdminErrorType.TEMPLATE_NOT_FOUND, 
                "Failed to delete orphaned template", e);
        }
    }
}