package com.bioid.keycloak.client.tracing;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Individual span within a distributed trace.
 * Represents a single operation or step within a larger administrative workflow.
 */
public class TraceSpan {
    private final String spanId;
    private final String parentSpanId;
    private final String operationName;
    private final Instant startTime;
    private final Map<String, String> tags;
    private final List<SpanEvent> events;
    
    private Instant endTime;
    private boolean success;
    private String errorMessage;
    
    public TraceSpan(String spanId, String parentSpanId, String operationName, Instant startTime) {
        this.spanId = spanId;
        this.parentSpanId = parentSpanId;
        this.operationName = operationName;
        this.startTime = startTime;
        this.tags = new HashMap<>();
        this.events = new CopyOnWriteArrayList<>();
        this.success = false;
    }
    
    public String getSpanId() {
        return spanId;
    }
    
    public String getParentSpanId() {
        return parentSpanId;
    }
    
    public String getOperationName() {
        return operationName;
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
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public Map<String, String> getTags() {
        return new HashMap<>(tags);
    }
    
    public List<SpanEvent> getEvents() {
        return new ArrayList<>(events);
    }
    
    public Duration getDuration() {
        if (endTime != null) {
            return Duration.between(startTime, endTime);
        } else {
            return Duration.between(startTime, Instant.now());
        }
    }
    
    public void addTag(String key, String value) {
        tags.put(key, value);
    }
    
    public void addTags(Map<String, String> tags) {
        this.tags.putAll(tags);
    }
    
    public void addEvent(SpanEvent event) {
        events.add(event);
    }
    
    public void finish(boolean success, String errorMessage) {
        this.endTime = Instant.now();
        this.success = success;
        this.errorMessage = errorMessage;
    }
    
    public boolean isFinished() {
        return endTime != null;
    }
    
    /**
     * Gets a specific tag value.
     *
     * @param key the tag key
     * @return the tag value or null if not found
     */
    public String getTag(String key) {
        return tags.get(key);
    }
    
    /**
     * Checks if this span has a specific tag.
     *
     * @param key the tag key
     * @return true if the tag exists
     */
    public boolean hasTag(String key) {
        return tags.containsKey(key);
    }
    
    /**
     * Gets the number of events in this span.
     *
     * @return event count
     */
    public int getEventCount() {
        return events.size();
    }
    
    /**
     * Checks if this span is a root span (has no parent).
     *
     * @return true if this is a root span
     */
    public boolean isRootSpan() {
        return parentSpanId == null;
    }
    
    @Override
    public String toString() {
        return String.format("TraceSpan{spanId='%s', parentSpanId='%s', " +
            "operationName='%s', startTime=%s, endTime=%s, success=%s, " +
            "duration=%s, tagCount=%d, eventCount=%d}",
            spanId, parentSpanId, operationName, startTime, endTime, success,
            getDuration(), tags.size(), events.size());
    }
}