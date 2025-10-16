package com.bioid.keycloak.client.logging;

import java.time.Instant;

/**
 * Search criteria for filtering log events.
 * Provides flexible filtering options for log analysis and reporting.
 */
public class LogSearchCriteria {
    private Instant startTime;
    private Instant endTime;
    private String operation;
    private String userId;
    private String adminUserId;
    private Boolean successOnly;
    private String correlationId;
    
    public Instant getStartTime() {
        return startTime;
    }
    
    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }
    
    public Instant getEndTime() {
        return endTime;
    }
    
    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }
    
    public String getOperation() {
        return operation;
    }
    
    public void setOperation(String operation) {
        this.operation = operation;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getAdminUserId() {
        return adminUserId;
    }
    
    public void setAdminUserId(String adminUserId) {
        this.adminUserId = adminUserId;
    }
    
    public Boolean getSuccessOnly() {
        return successOnly;
    }
    
    public void setSuccessOnly(Boolean successOnly) {
        this.successOnly = successOnly;
    }
    
    public String getCorrelationId() {
        return correlationId;
    }
    
    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }
    
    /**
     * Builder for LogSearchCriteria.
     */
    public static class Builder {
        private final LogSearchCriteria criteria = new LogSearchCriteria();
        
        public Builder startTime(Instant startTime) {
            criteria.setStartTime(startTime);
            return this;
        }
        
        public Builder endTime(Instant endTime) {
            criteria.setEndTime(endTime);
            return this;
        }
        
        public Builder operation(String operation) {
            criteria.setOperation(operation);
            return this;
        }
        
        public Builder userId(String userId) {
            criteria.setUserId(userId);
            return this;
        }
        
        public Builder adminUserId(String adminUserId) {
            criteria.setAdminUserId(adminUserId);
            return this;
        }
        
        public Builder successOnly(boolean successOnly) {
            criteria.setSuccessOnly(successOnly);
            return this;
        }
        
        public Builder correlationId(String correlationId) {
            criteria.setCorrelationId(correlationId);
            return this;
        }
        
        public LogSearchCriteria build() {
            return criteria;
        }
    }
    
    public static Builder builder() {
        return new Builder();
    }
}