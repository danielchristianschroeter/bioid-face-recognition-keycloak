package com.bioid.keycloak.client.tracing;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Event within a trace span.
 * Represents a specific point-in-time occurrence during span execution.
 */
public class SpanEvent {
    private final String name;
    private final Instant timestamp;
    private final Map<String, String> attributes;
    
    public SpanEvent(String name, Instant timestamp, Map<String, String> attributes) {
        this.name = name;
        this.timestamp = timestamp;
        this.attributes = attributes != null ? new HashMap<>(attributes) : new HashMap<>();
    }
    
    public SpanEvent(String name, Instant timestamp) {
        this(name, timestamp, null);
    }
    
    public SpanEvent(String name) {
        this(name, Instant.now(), null);
    }
    
    public String getName() {
        return name;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public Map<String, String> getAttributes() {
        return new HashMap<>(attributes);
    }
    
    public String getAttribute(String key) {
        return attributes.get(key);
    }
    
    public boolean hasAttribute(String key) {
        return attributes.containsKey(key);
    }
    
    @Override
    public String toString() {
        return String.format("SpanEvent{name='%s', timestamp=%s, attributes=%s}",
            name, timestamp, attributes);
    }
}