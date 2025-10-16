package com.bioid.keycloak.client.alerting;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Alert representation for system monitoring and notifications.
 * Contains all information needed to process and route alerts appropriately.
 */
public class Alert {
    private final AlertType type;
    private final AlertSeverity severity;
    private final String message;
    private final Map<String, String> details;
    private final Instant timestamp;
    private final String alertId;
    
    public Alert(AlertType type, AlertSeverity severity, String message, 
                Map<String, String> details, Instant timestamp) {
        this.type = type;
        this.severity = severity;
        this.message = message;
        this.details = details != null ? new HashMap<>(details) : new HashMap<>();
        this.timestamp = timestamp;
        this.alertId = generateAlertId();
    }
    
    public AlertType getType() {
        return type;
    }
    
    public AlertSeverity getSeverity() {
        return severity;
    }
    
    public String getMessage() {
        return message;
    }
    
    public Map<String, String> getDetails() {
        return new HashMap<>(details);
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public String getAlertId() {
        return alertId;
    }
    
    /**
     * Gets a specific detail value.
     *
     * @param key the detail key
     * @return the detail value or null if not found
     */
    public String getDetail(String key) {
        return details.get(key);
    }
    
    /**
     * Checks if this alert has a specific detail.
     *
     * @param key the detail key
     * @return true if the detail exists
     */
    public boolean hasDetail(String key) {
        return details.containsKey(key);
    }
    
    /**
     * Generates a unique alert ID.
     */
    private String generateAlertId() {
        return String.format("alert-%s-%s-%d", 
            type.name().toLowerCase(), 
            severity.name().toLowerCase(),
            timestamp.toEpochMilli());
    }
    
    @Override
    public String toString() {
        return String.format("Alert{id='%s', type=%s, severity=%s, message='%s', " +
            "timestamp=%s, detailCount=%d}",
            alertId, type, severity, message, timestamp, details.size());
    }
}