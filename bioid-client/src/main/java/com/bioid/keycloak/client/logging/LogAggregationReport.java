package com.bioid.keycloak.client.logging;

import java.time.Instant;
import java.util.Map;

/**
 * Report generated from log aggregation analysis.
 * Contains summarized information about administrative operations over a time window.
 */
public class LogAggregationReport {
    private final Instant windowStart;
    private final Instant windowEnd;
    
    private int totalEvents;
    private int successfulEvents;
    private int failedEvents;
    private Map<String, OperationSummary> operationSummaries;
    private Map<String, Long> userActivity;
    
    public LogAggregationReport(Instant windowStart, Instant windowEnd) {
        this.windowStart = windowStart;
        this.windowEnd = windowEnd;
    }
    
    public Instant getWindowStart() {
        return windowStart;
    }
    
    public Instant getWindowEnd() {
        return windowEnd;
    }
    
    public int getTotalEvents() {
        return totalEvents;
    }
    
    public void setTotalEvents(int totalEvents) {
        this.totalEvents = totalEvents;
    }
    
    public int getSuccessfulEvents() {
        return successfulEvents;
    }
    
    public void setSuccessfulEvents(int successfulEvents) {
        this.successfulEvents = successfulEvents;
    }
    
    public int getFailedEvents() {
        return failedEvents;
    }
    
    public void setFailedEvents(int failedEvents) {
        this.failedEvents = failedEvents;
    }
    
    public Map<String, OperationSummary> getOperationSummaries() {
        return operationSummaries;
    }
    
    public void setOperationSummaries(Map<String, OperationSummary> operationSummaries) {
        this.operationSummaries = operationSummaries;
    }
    
    public Map<String, Long> getUserActivity() {
        return userActivity;
    }
    
    public void setUserActivity(Map<String, Long> userActivity) {
        this.userActivity = userActivity;
    }
    
    /**
     * Calculates the success rate as a percentage.
     *
     * @return success rate (0.0 to 1.0)
     */
    public double getSuccessRate() {
        return totalEvents > 0 ? (double) successfulEvents / totalEvents : 0.0;
    }
    
    /**
     * Calculates the failure rate as a percentage.
     *
     * @return failure rate (0.0 to 1.0)
     */
    public double getFailureRate() {
        return totalEvents > 0 ? (double) failedEvents / totalEvents : 0.0;
    }
}