package com.bioid.keycloak.client.tracing;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Trace context for distributed tracing of administrative operations.
 * Maintains the complete trace information including all spans and metadata.
 */
public class TraceContext {
    private final String traceId;
    private final String operationName;
    private final String operationType;
    private final String userId;
    private final String adminUserId;
    private final Instant startTime;
    private final List<TraceSpan> spans;
    
    private Instant endTime;
    private boolean success;
    private String summary;
    
    public TraceContext(String traceId, String operationName, String operationType, 
                       String userId, String adminUserId) {
        this.traceId = traceId;
        this.operationName = operationName;
        this.operationType = operationType;
        this.userId = userId;
        this.adminUserId = adminUserId;
        this.startTime = Instant.now();
        this.spans = new CopyOnWriteArrayList<>();
        this.success = false;
    }
    
    public String getTraceId() {
        return traceId;
    }
    
    public String getOperationName() {
        return operationName;
    }
    
    public String getOperationType() {
        return operationType;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public String getAdminUserId() {
        return adminUserId;
    }
    
    public Instant getStartTime() {
        return startTime;
    }
    
    public Instant getEndTime() {
        return endTime;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public String getSummary() {
        return summary;
    }
    
    public List<TraceSpan> getSpans() {
        return new ArrayList<>(spans);
    }
    
    public Duration getDuration() {
        if (endTime != null) {
            return Duration.between(startTime, endTime);
        } else {
            return Duration.between(startTime, Instant.now());
        }
    }
    
    public void addSpan(TraceSpan span) {
        spans.add(span);
    }
    
    public void finish(boolean success, String summary) {
        this.endTime = Instant.now();
        this.success = success;
        this.summary = summary;
    }
    
    public boolean isFinished() {
        return endTime != null;
    }
    
    /**
     * Gets the root span of this trace.
     *
     * @return the root span or null if no spans exist
     */
    public TraceSpan getRootSpan() {
        return spans.stream()
            .filter(span -> span.getParentSpanId() == null)
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Gets all child spans of a given parent span.
     *
     * @param parentSpanId the parent span ID
     * @return list of child spans
     */
    public List<TraceSpan> getChildSpans(String parentSpanId) {
        return spans.stream()
            .filter(span -> parentSpanId.equals(span.getParentSpanId()))
            .toList();
    }
    
    /**
     * Gets the total number of failed spans in this trace.
     *
     * @return number of failed spans
     */
    public long getFailedSpanCount() {
        return spans.stream()
            .mapToLong(span -> span.isSuccess() ? 0 : 1)
            .sum();
    }
    
    /**
     * Gets the maximum depth of spans in this trace.
     *
     * @return maximum span depth
     */
    public int getMaxSpanDepth() {
        return spans.stream()
            .mapToInt(this::calculateSpanDepth)
            .max()
            .orElse(0);
    }
    
    /**
     * Calculates the depth of a span in the trace hierarchy.
     */
    private int calculateSpanDepth(TraceSpan span) {
        int depth = 0;
        String parentId = span.getParentSpanId();
        
        while (parentId != null) {
            depth++;
            final String currentParentId = parentId;
            parentId = spans.stream()
                .filter(s -> s.getSpanId().equals(currentParentId))
                .findFirst()
                .map(TraceSpan::getParentSpanId)
                .orElse(null);
        }
        
        return depth;
    }
    
    @Override
    public String toString() {
        return String.format("TraceContext{traceId='%s', operationName='%s', " +
            "operationType='%s', userId='%s', adminUserId='%s', startTime=%s, " +
            "endTime=%s, success=%s, spanCount=%d}",
            traceId, operationName, operationType, userId, adminUserId, 
            startTime, endTime, success, spans.size());
    }
}