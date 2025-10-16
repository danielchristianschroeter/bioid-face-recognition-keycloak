package com.bioid.keycloak.client.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Log aggregation and analysis service for BioID administrative operations.
 * Provides centralized log collection, analysis, and reporting capabilities
 * for monitoring, debugging, and compliance purposes.
 */
public class LogAggregationService {
    private static final Logger logger = LoggerFactory.getLogger(LogAggregationService.class);
    
    private final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule());
    
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    
    // Log storage and aggregation
    private final List<AdminLogEvent> logBuffer = new CopyOnWriteArrayList<>();
    private final Map<String, LogStatistics> operationStats = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> errorCounts = new ConcurrentHashMap<>();
    
    // Configuration
    private static final int MAX_BUFFER_SIZE = 10000;
    private static final Duration AGGREGATION_INTERVAL = Duration.ofMinutes(5);
    private static final Duration LOG_RETENTION_PERIOD = Duration.ofDays(30);
    
    public LogAggregationService() {
        this.scheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "bioid-log-aggregation");
            t.setDaemon(true);
            return t;
        });
    }
    
    /**
     * Starts the log aggregation service.
     */
    public void start() {
        if (isRunning.compareAndSet(false, true)) {
            logger.info("Starting BioID log aggregation service");
            
            // Schedule log aggregation
            scheduler.scheduleAtFixedRate(
                this::aggregateLogs,
                0,
                AGGREGATION_INTERVAL.toSeconds(),
                TimeUnit.SECONDS
            );
            
            // Schedule log cleanup
            scheduler.scheduleAtFixedRate(
                this::cleanupOldLogs,
                Duration.ofHours(1).toSeconds(),
                Duration.ofHours(1).toSeconds(),
                TimeUnit.SECONDS
            );
            
            logger.info("BioID log aggregation service started successfully");
        } else {
            logger.warn("Log aggregation service is already running");
        }
    }
    
    /**
     * Stops the log aggregation service.
     */
    public void stop() {
        if (isRunning.compareAndSet(true, false)) {
            logger.info("Stopping BioID log aggregation service");
            
            // Perform final aggregation
            aggregateLogs();
            
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(30, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
            
            logger.info("BioID log aggregation service stopped");
        }
    }
    
    /**
     * Adds a log event to the aggregation buffer.
     *
     * @param logEvent the log event to add
     */
    public void addLogEvent(AdminLogEvent logEvent) {
        if (!isRunning.get()) {
            return;
        }
        
        // Add to buffer
        logBuffer.add(logEvent);
        
        // Maintain buffer size
        if (logBuffer.size() > MAX_BUFFER_SIZE) {
            // Remove oldest entries
            int removeCount = logBuffer.size() - MAX_BUFFER_SIZE;
            for (int i = 0; i < removeCount; i++) {
                logBuffer.remove(0);
            }
        }
        
        // Update real-time statistics
        updateOperationStatistics(logEvent);
        
        // Update error counts
        if (!logEvent.isSuccess()) {
            String errorKey = logEvent.getOperation() + "_" + logEvent.getOperationType();
            errorCounts.computeIfAbsent(errorKey, k -> new AtomicLong(0)).incrementAndGet();
        }
    }
    
    /**
     * Performs log aggregation and analysis.
     */
    private void aggregateLogs() {
        try {
            logger.debug("Performing log aggregation (buffer size: {})", logBuffer.size());
            
            if (logBuffer.isEmpty()) {
                return;
            }
            
            // Create aggregation report
            LogAggregationReport report = createAggregationReport();
            
            // Log aggregation summary
            logAggregationSummary(report);
            
            // Detect patterns and anomalies
            detectAnomalies(report);
            
            logger.debug("Log aggregation completed");
            
        } catch (Exception e) {
            logger.error("Failed to perform log aggregation", e);
        }
    }
    
    /**
     * Creates an aggregation report from current log buffer.
     */
    private LogAggregationReport createAggregationReport() {
        Instant now = Instant.now();
        Instant windowStart = now.minus(AGGREGATION_INTERVAL);
        
        List<AdminLogEvent> windowLogs = logBuffer.stream()
            .filter(log -> log.getTimestamp().isAfter(windowStart))
            .collect(Collectors.toList());
        
        LogAggregationReport report = new LogAggregationReport(windowStart, now);
        
        // Calculate statistics
        report.setTotalEvents(windowLogs.size());
        report.setSuccessfulEvents((int) windowLogs.stream().mapToLong(log -> log.isSuccess() ? 1 : 0).sum());
        report.setFailedEvents(report.getTotalEvents() - report.getSuccessfulEvents());
        
        // Group by operation type
        Map<String, List<AdminLogEvent>> byOperation = windowLogs.stream()
            .collect(Collectors.groupingBy(AdminLogEvent::getOperation));
        
        Map<String, OperationSummary> operationSummaries = new HashMap<>();
        byOperation.forEach((operation, logs) -> {
            OperationSummary summary = new OperationSummary();
            summary.setOperation(operation);
            summary.setTotalCount(logs.size());
            summary.setSuccessCount((int) logs.stream().mapToLong(log -> log.isSuccess() ? 1 : 0).sum());
            summary.setFailureCount(summary.getTotalCount() - summary.getSuccessCount());
            
            // Calculate average duration
            double avgDuration = logs.stream()
                .filter(log -> log.getDuration() != null)
                .mapToLong(log -> log.getDuration().toMillis())
                .average()
                .orElse(0.0);
            summary.setAverageDuration(Duration.ofMillis((long) avgDuration));
            
            // Get error messages
            List<String> errorMessages = logs.stream()
                .filter(log -> !log.isSuccess() && log.getErrorMessage() != null)
                .map(AdminLogEvent::getErrorMessage)
                .distinct()
                .collect(Collectors.toList());
            summary.setErrorMessages(errorMessages);
            
            operationSummaries.put(operation, summary);
        });
        
        report.setOperationSummaries(operationSummaries);
        
        // Group by user
        Map<String, Long> userActivity = windowLogs.stream()
            .filter(log -> log.getAdminUserId() != null)
            .collect(Collectors.groupingBy(
                AdminLogEvent::getAdminUserId,
                Collectors.counting()
            ));
        report.setUserActivity(userActivity);
        
        return report;
    }
    
    /**
     * Updates operation statistics.
     */
    private void updateOperationStatistics(AdminLogEvent logEvent) {
        String key = logEvent.getOperation();
        LogStatistics stats = operationStats.computeIfAbsent(key, k -> new LogStatistics());
        
        stats.incrementTotal();
        if (logEvent.isSuccess()) {
            stats.incrementSuccess();
        } else {
            stats.incrementFailure();
        }
        
        if (logEvent.getDuration() != null) {
            stats.addDuration(logEvent.getDuration());
        }
        
        stats.setLastSeen(logEvent.getTimestamp());
    }
    
    /**
     * Logs aggregation summary.
     */
    private void logAggregationSummary(LogAggregationReport report) {
        try {
            Map<String, Object> summary = new HashMap<>();
            summary.put("windowStart", report.getWindowStart());
            summary.put("windowEnd", report.getWindowEnd());
            summary.put("totalEvents", report.getTotalEvents());
            summary.put("successfulEvents", report.getSuccessfulEvents());
            summary.put("failedEvents", report.getFailedEvents());
            summary.put("successRate", report.getSuccessRate());
            summary.put("operationCount", report.getOperationSummaries().size());
            summary.put("activeUsers", report.getUserActivity().size());
            
            String summaryJson = objectMapper.writeValueAsString(summary);
            logger.info("Log Aggregation Summary: {}", summaryJson);
            
        } catch (Exception e) {
            logger.error("Failed to log aggregation summary", e);
        }
    }
    
    /**
     * Detects anomalies and patterns in the logs.
     */
    private void detectAnomalies(LogAggregationReport report) {
        // Detect high error rates
        if (report.getFailedEvents() > 0) {
            double errorRate = (double) report.getFailedEvents() / report.getTotalEvents();
            if (errorRate > 0.1) { // 10% error rate threshold
                logger.warn("High error rate detected: {:.2f}% ({} failures out of {} events)",
                    errorRate * 100, report.getFailedEvents(), report.getTotalEvents());
            }
        }
        
        // Detect operations with high failure rates
        report.getOperationSummaries().forEach((operation, summary) -> {
            if (summary.getFailureCount() > 0) {
                double operationErrorRate = (double) summary.getFailureCount() / summary.getTotalCount();
                if (operationErrorRate > 0.2) { // 20% error rate for specific operation
                    logger.warn("High error rate for operation '{}': {:.2f}% ({} failures out of {} attempts)",
                        operation, operationErrorRate * 100, summary.getFailureCount(), summary.getTotalCount());
                }
            }
        });
        
        // Detect unusual user activity
        report.getUserActivity().forEach((userId, activityCount) -> {
            if (activityCount > 100) { // High activity threshold
                logger.info("High activity detected for user '{}': {} operations in {} minutes",
                    userId, activityCount, AGGREGATION_INTERVAL.toMinutes());
            }
        });
    }
    
    /**
     * Cleans up old logs to prevent memory issues.
     */
    private void cleanupOldLogs() {
        try {
            Instant cutoff = Instant.now().minus(LOG_RETENTION_PERIOD);
            
            int initialSize = logBuffer.size();
            logBuffer.removeIf(log -> log.getTimestamp().isBefore(cutoff));
            int removedCount = initialSize - logBuffer.size();
            
            if (removedCount > 0) {
                logger.debug("Cleaned up {} old log entries (retention: {})", 
                    removedCount, LOG_RETENTION_PERIOD);
            }
            
            // Clean up old statistics
            operationStats.entrySet().removeIf(entry -> 
                entry.getValue().getLastSeen().isBefore(cutoff));
            
        } catch (Exception e) {
            logger.error("Failed to cleanup old logs", e);
        }
    }
    
    /**
     * Gets current log statistics.
     *
     * @return log statistics
     */
    public Map<String, Object> getLogStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("bufferSize", logBuffer.size());
        stats.put("maxBufferSize", MAX_BUFFER_SIZE);
        stats.put("operationTypes", operationStats.size());
        stats.put("aggregationInterval", AGGREGATION_INTERVAL.toString());
        stats.put("retentionPeriod", LOG_RETENTION_PERIOD.toString());
        stats.put("isRunning", isRunning.get());
        
        // Error summary
        Map<String, Long> errorSummary = new HashMap<>();
        errorCounts.forEach((key, count) -> errorSummary.put(key, count.get()));
        stats.put("errorCounts", errorSummary);
        
        return stats;
    }
    
    /**
     * Gets operation statistics.
     *
     * @return operation statistics
     */
    public Map<String, LogStatistics> getOperationStatistics() {
        return new HashMap<>(operationStats);
    }
    
    /**
     * Searches logs by criteria.
     *
     * @param criteria search criteria
     * @return matching log events
     */
    public List<AdminLogEvent> searchLogs(LogSearchCriteria criteria) {
        return logBuffer.stream()
            .filter(log -> matchesCriteria(log, criteria))
            .collect(Collectors.toList());
    }
    
    /**
     * Checks if a log event matches search criteria.
     */
    private boolean matchesCriteria(AdminLogEvent log, LogSearchCriteria criteria) {
        if (criteria.getStartTime() != null && log.getTimestamp().isBefore(criteria.getStartTime())) {
            return false;
        }
        
        if (criteria.getEndTime() != null && log.getTimestamp().isAfter(criteria.getEndTime())) {
            return false;
        }
        
        if (criteria.getOperation() != null && !criteria.getOperation().equals(log.getOperation())) {
            return false;
        }
        
        if (criteria.getUserId() != null && !criteria.getUserId().equals(log.getUserId())) {
            return false;
        }
        
        if (criteria.getAdminUserId() != null && !criteria.getAdminUserId().equals(log.getAdminUserId())) {
            return false;
        }
        
        if (criteria.getSuccessOnly() != null) {
            if (criteria.getSuccessOnly() && !log.isSuccess()) {
                return false;
            }
            if (!criteria.getSuccessOnly() && log.isSuccess()) {
                return false;
            }
        }
        
        return true;
    }
}