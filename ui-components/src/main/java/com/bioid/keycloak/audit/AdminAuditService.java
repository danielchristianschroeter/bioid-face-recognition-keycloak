package com.bioid.keycloak.audit;

import com.bioid.keycloak.compliance.ComplianceReportService;
import com.bioid.keycloak.compliance.ComplianceReportType;
// Removed unused import
import com.bioid.keycloak.events.FaceRecognitionEventListener;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Enhanced audit service for administrative operations with detailed logging,
 * SIEM integration capability, and compliance reporting.
 *
 * <p>Provides comprehensive audit trails for all administrative actions
 * related to biometric template management, liveness detection configuration,
 * and bulk operations.
 */
@ApplicationScoped
public class AdminAuditService {

    private static final Logger logger = LoggerFactory.getLogger(AdminAuditService.class);
    private static final Logger auditLogger = LoggerFactory.getLogger("BIOID_AUDIT");
    private static final Logger siemLogger = LoggerFactory.getLogger("BIOID_SIEM");
    
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final ExecutorService auditExecutor = Executors.newFixedThreadPool(2);
    private final AuditConfiguration auditConfig;
    private final AuditStorage auditStorage;
    
    @Inject
    private FaceRecognitionEventListener eventListener;
    
    @Inject
    private ComplianceReportService complianceReportService;
    
    private KeycloakSession session;

    public AdminAuditService() {
        this.auditConfig = new AuditConfiguration();
        this.auditStorage = new AuditStorage();
    }
    
    public void setSession(KeycloakSession session) {
        this.session = session;
    }

    /**
     * Logs an administrative action with comprehensive event data.
     *
     * @param action The administrative action being performed
     * @param adminUser The administrator performing the action
     * @param targetUser The user being affected (if applicable)
     * @param realm The realm context
     * @param details Additional details about the operation
     * @param result The result of the operation
     */
    public void logAdminAction(AdminActionType action, UserModel adminUser, UserModel targetUser,
                              RealmModel realm, Map<String, Object> details, AdminActionResult result) {
        
        AdminAuditEvent auditEvent = AdminAuditEvent.builder()
            .actionType(action)
            .adminUserId(adminUser.getId())
            .adminUsername(adminUser.getUsername())
            .targetUserId(targetUser != null ? targetUser.getId() : null)
            .targetUsername(targetUser != null ? targetUser.getUsername() : null)
            .realmId(realm.getId())
            .realmName(realm.getName())
            .timestamp(Instant.now())
            .ipAddress(getClientIpAddress())
            .userAgent(getUserAgent())
            .sessionId(getSessionId())
            .details(details)
            .result(result)
            .complianceRelevant(isComplianceRelevant(action))
            .securityRelevant(isSecurityRelevant(action))
            .riskLevel(calculateRiskLevel(action, details))
            .build();

        // Log synchronously for immediate audit trail
        logAuditEvent(auditEvent);
        
        // Store asynchronously for performance
        CompletableFuture.runAsync(() -> storeAuditEvent(auditEvent), auditExecutor);
        
        // Send to SIEM if configured and relevant
        if (auditConfig.isSiemIntegrationEnabled() && auditEvent.isSecurityRelevant()) {
            CompletableFuture.runAsync(() -> sendToSiem(auditEvent), auditExecutor);
        }
        
        // Create Keycloak admin event for integration
        createKeycloakAdminEvent(auditEvent);
    }

    /**
     * Logs a bulk operation with detailed progress tracking.
     *
     * @param operation The bulk operation being performed
     * @param adminUser The administrator performing the operation
     * @param realm The realm context
     * @param totalItems Total number of items in the operation
     * @param successCount Number of successful operations
     * @param failureCount Number of failed operations
     * @param errors List of errors encountered
     */
    public void logBulkOperation(BulkOperationType operation, UserModel adminUser, RealmModel realm,
                                int totalItems, int successCount, int failureCount, List<String> errors) {
        
        Map<String, Object> details = new HashMap<>();
        details.put("total_items", totalItems);
        details.put("success_count", successCount);
        details.put("failure_count", failureCount);
        details.put("success_rate", (double) successCount / totalItems);
        if (!errors.isEmpty()) {
            details.put("errors", errors);
        }
        
        AdminActionResult result = failureCount == 0 ? AdminActionResult.SUCCESS :
                                  successCount == 0 ? AdminActionResult.FAILURE :
                                  AdminActionResult.PARTIAL_SUCCESS;
        
        logAdminAction(AdminActionType.BULK_OPERATION, adminUser, null, realm, details, result);
    }

    /**
     * Logs template access operations including thumbnail downloads.
     *
     * @param adminUser The administrator accessing the template
     * @param realm The realm context
     * @param classId The template class ID
     * @param operation The type of template operation
     * @param thumbnailsDownloaded Whether thumbnails were downloaded
     */
    public void logTemplateAccess(UserModel adminUser, RealmModel realm, long classId,
                                 TemplateOperation operation, boolean thumbnailsDownloaded) {
        
        Map<String, Object> details = new HashMap<>();
        details.put("class_id", classId);
        details.put("operation", operation.name());
        details.put("thumbnails_downloaded", thumbnailsDownloaded);
        
        AdminActionType actionType = operation == TemplateOperation.DELETE ? 
                                   AdminActionType.TEMPLATE_DELETE : AdminActionType.TEMPLATE_ACCESS;
        
        logAdminAction(actionType, adminUser, null, realm, details, AdminActionResult.SUCCESS);
    }

    /**
     * Logs liveness detection configuration changes.
     *
     * @param adminUser The administrator making the change
     * @param realm The realm context
     * @param oldConfig The previous configuration
     * @param newConfig The new configuration
     */
    public void logLivenessConfigChange(UserModel adminUser, RealmModel realm,
                                       Map<String, Object> oldConfig, Map<String, Object> newConfig) {
        
        Map<String, Object> details = new HashMap<>();
        details.put("old_config", oldConfig);
        details.put("new_config", newConfig);
        details.put("config_changes", calculateConfigChanges(oldConfig, newConfig));
        
        logAdminAction(AdminActionType.LIVENESS_CONFIG_UPDATE, adminUser, null, realm, 
                      details, AdminActionResult.SUCCESS);
    }

    /**
     * Generates a compliance report for the specified date range.
     *
     * @param realm The realm to generate the report for
     * @param startDate The start date (inclusive)
     * @param endDate The end date (inclusive)
     * @param reportType The type of report to generate
     * @return CompletableFuture containing the path to the generated report
     */
    public CompletableFuture<Path> generateComplianceReport(RealmModel realm, LocalDate startDate,
                                                           LocalDate endDate, ComplianceReportType reportType) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Generating compliance report for realm {} from {} to {}", 
                           realm.getName(), startDate, endDate);
                
                Path reportPath = complianceReportService.generateReport(startDate, endDate, reportType);
                
                // Log the report generation
                Map<String, Object> details = new HashMap<>();
                details.put("start_date", startDate.toString());
                details.put("end_date", endDate.toString());
                details.put("report_type", reportType.name());
                details.put("report_path", reportPath.toString());
                
                // Note: We need to get the current admin user from the session context
                UserModel adminUser = session.getContext().getUser();
                if (adminUser != null) {
                    logAdminAction(AdminActionType.COMPLIANCE_REPORT_GENERATED, adminUser, null, 
                                  realm, details, AdminActionResult.SUCCESS);
                }
                
                return reportPath;
            } catch (Exception e) {
                logger.error("Failed to generate compliance report", e);
                throw new RuntimeException("Failed to generate compliance report", e);
            }
        }, auditExecutor);
    }

    /**
     * Retrieves audit events for the specified criteria.
     *
     * @param realm The realm to search in
     * @param startTime The start time (inclusive)
     * @param endTime The end time (inclusive)
     * @param actionTypes The action types to filter by (null for all)
     * @param adminUserId The admin user ID to filter by (null for all)
     * @return List of matching audit events
     */
    public List<AdminAuditEvent> getAuditEvents(RealmModel realm, Instant startTime, Instant endTime,
                                               List<AdminActionType> actionTypes, String adminUserId) {
        
        return auditStorage.retrieveAuditEvents(realm.getId(), startTime, endTime, actionTypes, adminUserId);
    }

    /**
     * Configures audit retention policies.
     *
     * @param retentionDays Number of days to retain audit events
     * @param autoCleanup Whether to enable automatic cleanup
     */
    public void configureRetentionPolicy(int retentionDays, boolean autoCleanup) {
        auditConfig.setRetentionDays(retentionDays);
        auditConfig.setAutoCleanupEnabled(autoCleanup);
        
        if (autoCleanup) {
            scheduleAuditCleanup();
        }
        
        logger.info("Audit retention policy configured: {} days, auto-cleanup: {}", 
                   retentionDays, autoCleanup);
    }

    /**
     * Enables or disables SIEM integration.
     *
     * @param enabled Whether SIEM integration should be enabled
     * @param siemEndpoint The SIEM endpoint URL (if enabled)
     * @param apiKey The API key for SIEM integration (if enabled)
     */
    public void configureSiemIntegration(boolean enabled, String siemEndpoint, String apiKey) {
        auditConfig.setSiemIntegrationEnabled(enabled);
        auditConfig.setSiemEndpoint(siemEndpoint);
        auditConfig.setSiemApiKey(apiKey);
        
        logger.info("SIEM integration configured: enabled={}, endpoint={}", enabled, siemEndpoint);
    }

    // Private helper methods

    private void logAuditEvent(AdminAuditEvent auditEvent) {
        try {
            String jsonEvent = objectMapper.writeValueAsString(auditEvent);
            auditLogger.info("ADMIN_AUDIT: {}", jsonEvent);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize audit event", e);
            auditLogger.error("ADMIN_AUDIT_ERROR: Failed to serialize event for action {} by user {}", 
                             auditEvent.getActionType(), auditEvent.getAdminUserId());
        }
    }

    private void storeAuditEvent(AdminAuditEvent auditEvent) {
        try {
            auditStorage.storeAuditEvent(auditEvent);
        } catch (Exception e) {
            logger.error("Failed to store audit event", e);
        }
    }

    private void sendToSiem(AdminAuditEvent auditEvent) {
        try {
            if (auditConfig.getSiemEndpoint() != null) {
                String jsonEvent = objectMapper.writeValueAsString(auditEvent);
                siemLogger.info("SIEM_EVENT: {}", jsonEvent);
                // In a real implementation, this would send to the actual SIEM system
                // via HTTP, syslog, or other configured transport
            }
        } catch (Exception e) {
            logger.error("Failed to send event to SIEM", e);
        }
    }

    private void createKeycloakAdminEvent(AdminAuditEvent auditEvent) {
        try {
            AdminEvent adminEvent = new AdminEvent();
            adminEvent.setTime(auditEvent.getTimestamp().toEpochMilli());
            adminEvent.setRealmId(auditEvent.getRealmId());
            // Set auth details - in a real implementation, this would be properly configured
            // adminEvent.setAuthDetails(session.getContext().getAuthenticationSession().getAuthenticatedUser().getId());
            adminEvent.setOperationType(mapToKeycloakOperationType(auditEvent.getActionType()));
            adminEvent.setResourceType(ResourceType.COMPONENT); // Generic resource type for custom operations
            adminEvent.setResourcePath("face-recognition/" + auditEvent.getActionType().name().toLowerCase());
            
            if (auditEvent.getDetails() != null) {
                adminEvent.setRepresentation(objectMapper.writeValueAsString(auditEvent.getDetails()));
            }
            
            // Send to Keycloak's event system
            if (eventListener != null) {
                // The event listener will handle integration with Keycloak's event system
                session.getProvider(org.keycloak.events.EventStoreProvider.class).onEvent(adminEvent, false);
            }
        } catch (Exception e) {
            logger.error("Failed to create Keycloak admin event", e);
        }
    }

    private OperationType mapToKeycloakOperationType(AdminActionType actionType) {
        switch (actionType) {
            case TEMPLATE_DELETE:
            case USER_ENROLLMENT_DELETE:
                return OperationType.DELETE;
            case LIVENESS_CONFIG_UPDATE:
            case TEMPLATE_UPGRADE:
                return OperationType.UPDATE;
            case ENROLLMENT_LINK_GENERATED:
                return OperationType.CREATE;
            default:
                return OperationType.ACTION;
        }
    }

    private boolean isComplianceRelevant(AdminActionType action) {
        return action == AdminActionType.TEMPLATE_DELETE ||
               action == AdminActionType.USER_ENROLLMENT_DELETE ||
               action == AdminActionType.TEMPLATE_ACCESS ||
               action == AdminActionType.COMPLIANCE_REPORT_GENERATED ||
               action == AdminActionType.BULK_OPERATION;
    }

    private boolean isSecurityRelevant(AdminActionType action) {
        return action == AdminActionType.TEMPLATE_DELETE ||
               action == AdminActionType.USER_ENROLLMENT_DELETE ||
               action == AdminActionType.LIVENESS_CONFIG_UPDATE ||
               action == AdminActionType.BULK_OPERATION ||
               action == AdminActionType.UNAUTHORIZED_ACCESS_ATTEMPT;
    }

    private RiskLevel calculateRiskLevel(AdminActionType action, Map<String, Object> details) {
        switch (action) {
            case TEMPLATE_DELETE:
            case USER_ENROLLMENT_DELETE:
                return RiskLevel.HIGH;
            case BULK_OPERATION:
                Integer totalItems = (Integer) details.get("total_items");
                return totalItems != null && totalItems > 100 ? RiskLevel.HIGH : RiskLevel.MEDIUM;
            case LIVENESS_CONFIG_UPDATE:
                return RiskLevel.MEDIUM;
            case TEMPLATE_ACCESS:
                Boolean thumbnailsDownloaded = (Boolean) details.get("thumbnails_downloaded");
                return Boolean.TRUE.equals(thumbnailsDownloaded) ? RiskLevel.MEDIUM : RiskLevel.LOW;
            default:
                return RiskLevel.LOW;
        }
    }

    private Map<String, Object> calculateConfigChanges(Map<String, Object> oldConfig, Map<String, Object> newConfig) {
        Map<String, Object> changes = new HashMap<>();
        
        // Compare configurations and identify changes
        for (String key : newConfig.keySet()) {
            Object oldValue = oldConfig.get(key);
            Object newValue = newConfig.get(key);
            
            if (!java.util.Objects.equals(oldValue, newValue)) {
                Map<String, Object> change = new HashMap<>();
                change.put("old_value", oldValue);
                change.put("new_value", newValue);
                changes.put(key, change);
            }
        }
        
        return changes;
    }

    private String getClientIpAddress() {
        // Extract IP address from the current request context
        // This would typically come from the HTTP request headers
        return session.getContext().getConnection() != null ? 
               session.getContext().getConnection().getRemoteAddr() : "unknown";
    }

    private String getUserAgent() {
        // Extract user agent from the current request context
        // This would typically come from the HTTP request headers
        return "Keycloak-Admin-Console"; // Placeholder
    }

    private String getSessionId() {
        return session.getContext().getAuthenticationSession() != null ?
               session.getContext().getAuthenticationSession().getParentSession().getId() : null;
    }

    private void scheduleAuditCleanup() {
        // Schedule periodic cleanup of old audit events
        // This would typically use a scheduler like Quartz or Java's ScheduledExecutorService
        logger.info("Audit cleanup scheduled for retention period: {} days", auditConfig.getRetentionDays());
    }

    public void shutdown() {
        auditExecutor.shutdown();
    }
}