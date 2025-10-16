package com.bioid.keycloak.client.tracing;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * Distributed tracing implementation for BioID administrative operations.
 * Provides comprehensive tracing capabilities for complex workflows including
 * bulk operations, template upgrades, and multi-step administrative processes.
 */
public class DistributedTracing {
    private static final Logger logger = LoggerFactory.getLogger(DistributedTracing.class);
    
    private static final ThreadLocal<Stack<TraceSpan>> spanStack = ThreadLocal.withInitial(Stack::new);
    private static final Map<String, TraceContext> activeTraces = new ConcurrentHashMap<>();
    
    // Trace configuration
    private static final Duration MAX_TRACE_DURATION = Duration.ofHours(1);
    private static final int MAX_SPANS_PER_TRACE = 1000;
    
    /**
     * Starts a new trace for an administrative operation.
     *
     * @param operationName the name of the operation
     * @param operationType the type of operation
     * @param userId the user ID (optional)
     * @param adminUserId the admin user ID (optional)
     * @return the trace context
     */
    public static TraceContext startTrace(String operationName, String operationType, 
                                        String userId, String adminUserId) {
        String traceId = generateTraceId();
        String spanId = generateSpanId();
        
        TraceContext context = new TraceContext(traceId, operationName, operationType, userId, adminUserId);
        TraceSpan rootSpan = new TraceSpan(spanId, null, operationName, Instant.now());
        
        context.addSpan(rootSpan);
        spanStack.get().push(rootSpan);
        activeTraces.put(traceId, context);
        
        // Set MDC for logging correlation
        MDC.put("traceId", traceId);
        MDC.put("spanId", spanId);
        MDC.put("operation", operationName);
        
        logger.debug("Started trace: {} for operation: {}", traceId, operationName);
        
        return context;
    }
    
    /**
     * Starts a new span within the current trace.
     *
     * @param spanName the name of the span
     * @param tags optional tags for the span
     * @return the span ID
     */
    public static String startSpan(String spanName, Map<String, String> tags) {
        Stack<TraceSpan> spans = spanStack.get();
        if (spans.isEmpty()) {
            logger.warn("Attempted to start span '{}' without active trace", spanName);
            return null;
        }
        
        TraceSpan parentSpan = spans.peek();
        String spanId = generateSpanId();
        TraceSpan span = new TraceSpan(spanId, parentSpan.getSpanId(), spanName, Instant.now());
        
        if (tags != null) {
            span.addTags(tags);
        }
        
        spans.push(span);
        
        // Update MDC
        MDC.put("spanId", spanId);
        MDC.put("parentSpanId", parentSpan.getSpanId());
        
        // Add span to trace context
        String traceId = MDC.get("traceId");
        if (traceId != null) {
            TraceContext context = activeTraces.get(traceId);
            if (context != null) {
                context.addSpan(span);
            }
        }
        
        logger.debug("Started span: {} (parent: {}) for operation: {}", 
            spanId, parentSpan.getSpanId(), spanName);
        
        return spanId;
    }
    
    /**
     * Finishes the current span.
     *
     * @param success whether the span completed successfully
     * @param errorMessage error message if failed
     * @param tags additional tags to add
     */
    public static void finishSpan(boolean success, String errorMessage, Map<String, String> tags) {
        Stack<TraceSpan> spans = spanStack.get();
        if (spans.isEmpty()) {
            logger.warn("Attempted to finish span without active span");
            return;
        }
        
        TraceSpan span = spans.pop();
        span.finish(success, errorMessage);
        
        if (tags != null) {
            span.addTags(tags);
        }
        
        // Update MDC to parent span
        if (!spans.isEmpty()) {
            TraceSpan parentSpan = spans.peek();
            MDC.put("spanId", parentSpan.getSpanId());
            MDC.put("parentSpanId", parentSpan.getParentSpanId());
        } else {
            MDC.remove("spanId");
            MDC.remove("parentSpanId");
        }
        
        logger.debug("Finished span: {} (duration: {}ms, success: {})", 
            span.getSpanId(), span.getDuration().toMillis(), success);
    }
    
    /**
     * Finishes the current trace.
     *
     * @param success whether the trace completed successfully
     * @param summary summary of the trace execution
     */
    public static void finishTrace(boolean success, String summary) {
        String traceId = MDC.get("traceId");
        if (traceId == null) {
            logger.warn("Attempted to finish trace without active trace");
            return;
        }
        
        TraceContext context = activeTraces.remove(traceId);
        if (context != null) {
            context.finish(success, summary);
            
            // Finish any remaining spans
            Stack<TraceSpan> spans = spanStack.get();
            while (!spans.isEmpty()) {
                TraceSpan span = spans.pop();
                span.finish(success, "Trace finished");
            }
            
            // Log trace summary
            logTraceSummary(context);
        }
        
        // Clear MDC
        MDC.remove("traceId");
        MDC.remove("spanId");
        MDC.remove("parentSpanId");
        MDC.remove("operation");
        
        // Clear thread local
        spanStack.remove();
        
        logger.debug("Finished trace: {} (success: {})", traceId, success);
    }
    
    /**
     * Adds a tag to the current span.
     *
     * @param key the tag key
     * @param value the tag value
     */
    public static void addSpanTag(String key, String value) {
        Stack<TraceSpan> spans = spanStack.get();
        if (!spans.isEmpty()) {
            spans.peek().addTag(key, value);
        }
    }
    
    /**
     * Adds an event to the current span.
     *
     * @param eventName the event name
     * @param attributes event attributes
     */
    public static void addSpanEvent(String eventName, Map<String, String> attributes) {
        Stack<TraceSpan> spans = spanStack.get();
        if (!spans.isEmpty()) {
            spans.peek().addEvent(new SpanEvent(eventName, Instant.now(), attributes));
        }
    }
    
    /**
     * Gets the current trace ID.
     *
     * @return current trace ID or null if no active trace
     */
    public static String getCurrentTraceId() {
        return MDC.get("traceId");
    }
    
    /**
     * Gets the current span ID.
     *
     * @return current span ID or null if no active span
     */
    public static String getCurrentSpanId() {
        return MDC.get("spanId");
    }
    
    /**
     * Gets the current trace context.
     *
     * @return current trace context or null if no active trace
     */
    public static TraceContext getCurrentTraceContext() {
        String traceId = getCurrentTraceId();
        return traceId != null ? activeTraces.get(traceId) : null;
    }
    
    /**
     * Executes a traced operation.
     *
     * @param operationName the operation name
     * @param operationType the operation type
     * @param operation the operation to execute
     * @param <T> the return type
     * @return the operation result
     * @throws Exception if the operation fails
     */
    public static <T> T traced(String operationName, String operationType, 
                              TracedOperation<T> operation) throws Exception {
        TraceContext context = startTrace(operationName, operationType, null, null);
        try {
            T result = operation.execute();
            finishTrace(true, "Operation completed successfully");
            return result;
        } catch (Exception e) {
            finishTrace(false, "Operation failed: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Executes a traced span operation.
     *
     * @param spanName the span name
     * @param operation the operation to execute
     * @param <T> the return type
     * @return the operation result
     * @throws Exception if the operation fails
     */
    public static <T> T span(String spanName, TracedOperation<T> operation) throws Exception {
        String spanId = startSpan(spanName, null);
        try {
            T result = operation.execute();
            finishSpan(true, null, null);
            return result;
        } catch (Exception e) {
            finishSpan(false, e.getMessage(), null);
            throw e;
        }
    }
    
    /**
     * Logs a comprehensive trace summary.
     */
    private static void logTraceSummary(TraceContext context) {
        try {
            Map<String, Object> traceSummary = new HashMap<>();
            traceSummary.put("traceId", context.getTraceId());
            traceSummary.put("operationName", context.getOperationName());
            traceSummary.put("operationType", context.getOperationType());
            traceSummary.put("userId", context.getUserId());
            traceSummary.put("adminUserId", context.getAdminUserId());
            traceSummary.put("startTime", context.getStartTime());
            traceSummary.put("endTime", context.getEndTime());
            traceSummary.put("duration", context.getDuration().toMillis());
            traceSummary.put("success", context.isSuccess());
            traceSummary.put("spanCount", context.getSpans().size());
            traceSummary.put("summary", context.getSummary());
            
            // Calculate span statistics
            long totalSpanDuration = context.getSpans().stream()
                .mapToLong(span -> span.getDuration().toMillis())
                .sum();
            
            traceSummary.put("totalSpanDuration", totalSpanDuration);
            traceSummary.put("averageSpanDuration", 
                context.getSpans().size() > 0 ? totalSpanDuration / context.getSpans().size() : 0);
            
            logger.info("Trace Summary: {}", traceSummary);
            
        } catch (Exception e) {
            logger.error("Failed to log trace summary for trace: {}", context.getTraceId(), e);
        }
    }
    
    /**
     * Generates a unique trace ID.
     */
    private static String generateTraceId() {
        return "trace-" + UUID.randomUUID().toString();
    }
    
    /**
     * Generates a unique span ID.
     */
    private static String generateSpanId() {
        return "span-" + UUID.randomUUID().toString();
    }
    
    /**
     * Cleans up expired traces to prevent memory leaks.
     */
    public static void cleanupExpiredTraces() {
        Instant cutoff = Instant.now().minus(MAX_TRACE_DURATION);
        
        activeTraces.entrySet().removeIf(entry -> {
            TraceContext context = entry.getValue();
            if (context.getStartTime().isBefore(cutoff)) {
                logger.warn("Removing expired trace: {} (started: {})", 
                    entry.getKey(), context.getStartTime());
                return true;
            }
            return false;
        });
    }
    
    /**
     * Gets statistics about active traces.
     *
     * @return trace statistics
     */
    public static Map<String, Object> getTraceStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("activeTraces", activeTraces.size());
        stats.put("maxTraceDuration", MAX_TRACE_DURATION.toString());
        stats.put("maxSpansPerTrace", MAX_SPANS_PER_TRACE);
        
        if (!activeTraces.isEmpty()) {
            long oldestTraceAge = activeTraces.values().stream()
                .mapToLong(context -> Duration.between(context.getStartTime(), Instant.now()).toMillis())
                .max()
                .orElse(0);
            
            stats.put("oldestTraceAgeMs", oldestTraceAge);
        }
        
        return stats;
    }
    
    /**
     * Functional interface for traced operations.
     */
    @FunctionalInterface
    public interface TracedOperation<T> {
        T execute() throws Exception;
    }
}