package com.bioid.keycloak.client.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * Structured logging implementation for BioID administrative operations.
 * Provides consistent, machine-readable logging with contextual information
 * for audit trails, debugging, and monitoring.
 */
public class StructuredLogger {
    private static final Logger logger = LoggerFactory.getLogger(StructuredLogger.class);
    private static final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule());
    
    private final String component;
    private final String version;
    
    public StructuredLogger(String component, String version) {
        this.component = component;
        this.version = version;
    }
    
    /**
     * Logs an administrative operation with structured data.
     *
     * @param event the log event
     */
    public void logAdminOperation(AdminLogEvent event) {
        try {
            // Set MDC context for this log entry
            MDC.put("component", component);
            MDC.put("version", version);
            MDC.put("operation", event.getOperation());
            MDC.put("userId", event.getUserId());
            MDC.put("adminUserId", event.getAdminUserId());
            MDC.put("correlationId", event.getCorrelationId());
            
            // Create structured log entry
            Map<String, Object> logEntry = new HashMap<>();
            logEntry.put("timestamp", event.getTimestamp());
            logEntry.put("level", event.getLevel().name());
            logEntry.put("component", component);
            logEntry.put("version", version);
            logEntry.put("operation", event.getOperation());
            logEntry.put("operationType", event.getOperationType());
            logEntry.put("userId", event.getUserId());
            logEntry.put("adminUserId", event.getAdminUserId());
            logEntry.put("correlationId", event.getCorrelationId());
            logEntry.put("duration", event.getDuration() != null ? event.getDuration().toMillis() : null);
            logEntry.put("success", event.isSuccess());
            logEntry.put("errorCode", event.getErrorCode());
            logEntry.put("errorMessage", event.getErrorMessage());
            logEntry.put("details", event.getDetails());
            logEntry.put("ipAddress", event.getIpAddress());
            logEntry.put("userAgent", event.getUserAgent());
            
            String jsonLog = objectMapper.writeValueAsString(logEntry);
            
            // Log at appropriate level
            switch (event.getLevel()) {
                case ERROR:
                    logger.error("Admin Operation: {} - {}", event.getOperation(), jsonLog);
                    break;
                case WARN:
                    logger.warn("Admin Operation: {} - {}", event.getOperation(), jsonLog);
                    break;
                case INFO:
                    logger.info("Admin Operation: {} - {}", event.getOperation(), jsonLog);
                    break;
                case DEBUG:
                    logger.debug("Admin Operation: {} - {}", event.getOperation(), jsonLog);
                    break;
                case TRACE:
                    logger.trace("Admin Operation: {} - {}", event.getOperation(), jsonLog);
                    break;
            }
            
        } catch (Exception e) {
            logger.error("Failed to log structured admin operation", e);
        } finally {
            // Clear MDC context
            MDC.clear();
        }
    }
    
    /**
     * Logs a liveness detection operation.
     *
     * @param userId the user ID
     * @param mode the liveness detection mode
     * @param success whether the detection was successful
     * @param score the liveness score
     * @param duration the operation duration
     * @param correlationId the correlation ID
     */
    public void logLivenessDetection(String userId, String mode, boolean success, 
                                   double score, java.time.Duration duration, String correlationId) {
        AdminLogEvent event = AdminLogEvent.builder()
            .operation("LIVENESS_DETECTION")
            .operationType(AdminOperationType.LIVENESS_DETECTION)
            .userId(userId)
            .correlationId(correlationId)
            .duration(duration)
            .success(success)
            .level(success ? LogLevel.INFO : LogLevel.WARN)
            .details(Map.of(
                "mode", mode,
                "score", score,
                "threshold", "0.7" // This could be configurable
            ))
            .build();
        
        logAdminOperation(event);
    }
    
    /**
     * Logs a template management operation.
     *
     * @param operation the operation type
     * @param userId the user ID
     * @param adminUserId the admin user ID
     * @param success whether the operation was successful
     * @param duration the operation duration
     * @param correlationId the correlation ID
     * @param details additional operation details
     */
    public void logTemplateOperation(String operation, String userId, String adminUserId,
                                   boolean success, java.time.Duration duration, 
                                   String correlationId, Map<String, Object> details) {
        AdminLogEvent event = AdminLogEvent.builder()
            .operation(operation)
            .operationType(AdminOperationType.TEMPLATE_MANAGEMENT)
            .userId(userId)
            .adminUserId(adminUserId)
            .correlationId(correlationId)
            .duration(duration)
            .success(success)
            .level(success ? LogLevel.INFO : LogLevel.ERROR)
            .details(details)
            .build();
        
        logAdminOperation(event);
    }
    
    /**
     * Logs a bulk operation.
     *
     * @param operation the bulk operation type
     * @param adminUserId the admin user ID
     * @param itemCount the number of items processed
     * @param successCount the number of successful items
     * @param failureCount the number of failed items
     * @param duration the operation duration
     * @param correlationId the correlation ID
     */
    public void logBulkOperation(String operation, String adminUserId, int itemCount,
                               int successCount, int failureCount, java.time.Duration duration,
                               String correlationId) {
        boolean success = failureCount == 0;
        LogLevel level = success ? LogLevel.INFO : (failureCount < itemCount ? LogLevel.WARN : LogLevel.ERROR);
        
        AdminLogEvent event = AdminLogEvent.builder()
            .operation(operation)
            .operationType(AdminOperationType.BULK_OPERATION)
            .adminUserId(adminUserId)
            .correlationId(correlationId)
            .duration(duration)
            .success(success)
            .level(level)
            .details(Map.of(
                "totalItems", itemCount,
                "successfulItems", successCount,
                "failedItems", failureCount,
                "successRate", itemCount > 0 ? (double) successCount / itemCount : 0.0
            ))
            .build();
        
        logAdminOperation(event);
    }
    
    /**
     * Logs an enrollment management operation.
     *
     * @param operation the enrollment operation
     * @param userId the user ID
     * @param adminUserId the admin user ID
     * @param success whether the operation was successful
     * @param duration the operation duration
     * @param correlationId the correlation ID
     * @param errorMessage error message if failed
     */
    public void logEnrollmentOperation(String operation, String userId, String adminUserId,
                                     boolean success, java.time.Duration duration,
                                     String correlationId, String errorMessage) {
        AdminLogEvent.Builder eventBuilder = AdminLogEvent.builder()
            .operation(operation)
            .operationType(AdminOperationType.ENROLLMENT_MANAGEMENT)
            .userId(userId)
            .adminUserId(adminUserId)
            .correlationId(correlationId)
            .duration(duration)
            .success(success)
            .level(success ? LogLevel.INFO : LogLevel.ERROR);
        
        if (!success && errorMessage != null) {
            eventBuilder.errorMessage(errorMessage);
        }
        
        logAdminOperation(eventBuilder.build());
    }
    
    /**
     * Logs a security event.
     *
     * @param event the security event type
     * @param userId the user ID
     * @param adminUserId the admin user ID
     * @param ipAddress the IP address
     * @param userAgent the user agent
     * @param correlationId the correlation ID
     * @param details additional security details
     */
    public void logSecurityEvent(String event, String userId, String adminUserId,
                               String ipAddress, String userAgent, String correlationId,
                               Map<String, Object> details) {
        AdminLogEvent logEvent = AdminLogEvent.builder()
            .operation(event)
            .operationType(AdminOperationType.SECURITY)
            .userId(userId)
            .adminUserId(adminUserId)
            .correlationId(correlationId)
            .success(true) // Security events are informational
            .level(LogLevel.INFO)
            .ipAddress(ipAddress)
            .userAgent(userAgent)
            .details(details)
            .build();
        
        logAdminOperation(logEvent);
    }
    
    /**
     * Logs a compliance event.
     *
     * @param event the compliance event type
     * @param userId the user ID
     * @param adminUserId the admin user ID
     * @param correlationId the correlation ID
     * @param details compliance-specific details
     */
    public void logComplianceEvent(String event, String userId, String adminUserId,
                                 String correlationId, Map<String, Object> details) {
        AdminLogEvent logEvent = AdminLogEvent.builder()
            .operation(event)
            .operationType(AdminOperationType.COMPLIANCE)
            .userId(userId)
            .adminUserId(adminUserId)
            .correlationId(correlationId)
            .success(true)
            .level(LogLevel.INFO)
            .details(details)
            .build();
        
        logAdminOperation(logEvent);
    }
    
    /**
     * Logs a performance event.
     *
     * @param operation the operation name
     * @param duration the operation duration
     * @param correlationId the correlation ID
     * @param performanceMetrics performance-related metrics
     */
    public void logPerformanceEvent(String operation, java.time.Duration duration,
                                  String correlationId, Map<String, Object> performanceMetrics) {
        AdminLogEvent event = AdminLogEvent.builder()
            .operation(operation)
            .operationType(AdminOperationType.PERFORMANCE)
            .correlationId(correlationId)
            .duration(duration)
            .success(true)
            .level(LogLevel.DEBUG)
            .details(performanceMetrics)
            .build();
        
        logAdminOperation(event);
    }
    
    /**
     * Logs an error event with exception details.
     *
     * @param operation the operation that failed
     * @param exception the exception that occurred
     * @param correlationId the correlation ID
     * @param userId the user ID (optional)
     * @param adminUserId the admin user ID (optional)
     */
    public void logError(String operation, Exception exception, String correlationId,
                        String userId, String adminUserId) {
        AdminLogEvent event = AdminLogEvent.builder()
            .operation(operation)
            .operationType(AdminOperationType.ERROR)
            .userId(userId)
            .adminUserId(adminUserId)
            .correlationId(correlationId)
            .success(false)
            .level(LogLevel.ERROR)
            .errorMessage(exception.getMessage())
            .details(Map.of(
                "exceptionType", exception.getClass().getSimpleName(),
                "stackTrace", getStackTraceString(exception)
            ))
            .build();
        
        logAdminOperation(event);
    }
    
    /**
     * Creates a correlation ID for tracking related operations.
     *
     * @return a new correlation ID
     */
    public static String createCorrelationId() {
        return java.util.UUID.randomUUID().toString();
    }
    
    /**
     * Gets the current correlation ID from MDC.
     *
     * @return current correlation ID or null if not set
     */
    public static String getCurrentCorrelationId() {
        return MDC.get("correlationId");
    }
    
    /**
     * Sets the correlation ID in MDC for the current thread.
     *
     * @param correlationId the correlation ID to set
     */
    public static void setCorrelationId(String correlationId) {
        MDC.put("correlationId", correlationId);
    }
    
    /**
     * Clears the correlation ID from MDC.
     */
    public static void clearCorrelationId() {
        MDC.remove("correlationId");
    }
    
    /**
     * Converts exception stack trace to string.
     */
    private String getStackTraceString(Exception exception) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        exception.printStackTrace(pw);
        return sw.toString();
    }
    
    /**
     * Log levels for structured logging.
     */
    public enum LogLevel {
        TRACE, DEBUG, INFO, WARN, ERROR
    }
    
    /**
     * Administrative operation types for categorization.
     */
    public enum AdminOperationType {
        ENROLLMENT_MANAGEMENT,
        TEMPLATE_MANAGEMENT,
        LIVENESS_DETECTION,
        BULK_OPERATION,
        SECURITY,
        COMPLIANCE,
        PERFORMANCE,
        ERROR
    }
}