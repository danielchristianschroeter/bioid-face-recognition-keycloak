package com.bioid.keycloak.client.logging;

import java.time.Duration;
import java.util.List;

/**
 * Summary statistics for a specific operation type.
 * Provides aggregated information about operation performance and outcomes.
 */
public class OperationSummary {
    private String operation;
    private int totalCount;
    private int successCount;
    private int failureCount;
    private Duration averageDuration;
    private List<String> errorMessages;
    
    public String getOperation() {
        return operation;
    }
    
    public void setOperation(String operation) {
        this.operation = operation;
    }
    
    public int getTotalCount() {
        return totalCount;
    }
    
    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }
    
    public int getSuccessCount() {
        return successCount;
    }
    
    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }
    
    public int getFailureCount() {
        return failureCount;
    }
    
    public void setFailureCount(int failureCount) {
        this.failureCount = failureCount;
    }
    
    public Duration getAverageDuration() {
        return averageDuration;
    }
    
    public void setAverageDuration(Duration averageDuration) {
        this.averageDuration = averageDuration;
    }
    
    public List<String> getErrorMessages() {
        return errorMessages;
    }
    
    public void setErrorMessages(List<String> errorMessages) {
        this.errorMessages = errorMessages;
    }
    
    /**
     * Calculates the success rate for this operation.
     *
     * @return success rate (0.0 to 1.0)
     */
    public double getSuccessRate() {
        return totalCount > 0 ? (double) successCount / totalCount : 0.0;
    }
    
    /**
     * Calculates the failure rate for this operation.
     *
     * @return failure rate (0.0 to 1.0)
     */
    public double getFailureRate() {
        return totalCount > 0 ? (double) failureCount / totalCount : 0.0;
    }
}