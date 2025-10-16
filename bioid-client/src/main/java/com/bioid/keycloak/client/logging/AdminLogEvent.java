package com.bioid.keycloak.client.logging;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * Structured log event for administrative operations.
 * Provides a consistent data structure for logging administrative activities
 * with comprehensive contextual information for audit and monitoring purposes.
 */
public class AdminLogEvent {
    private final Instant timestamp;
    private final StructuredLogger.LogLevel level;
    private final String operation;
    private final StructuredLogger.AdminOperationType operationType;
    private final String userId;
    private final String adminUserId;
    private final String correlationId;
    private final Duration duration;
    private final boolean success;
    private final String errorCode;
    private final String errorMessage;
    private final Map<String, Object> details;
    private final String ipAddress;
    private final String userAgent;
    
    private AdminLogEvent(Builder builder) {
        this.timestamp = builder.timestamp != null ? builder.timestamp : Instant.now();
        this.level = builder.level != null ? builder.level : StructuredLogger.LogLevel.INFO;
        this.operation = builder.operation;
        this.operationType = builder.operationType;
        this.userId = builder.userId;
        this.adminUserId = builder.adminUserId;
        this.correlationId = builder.correlationId;
        this.duration = builder.duration;
        this.success = builder.success;
        this.errorCode = builder.errorCode;
        this.errorMessage = builder.errorMessage;
        this.details = builder.details;
        this.ipAddress = builder.ipAddress;
        this.userAgent = builder.userAgent;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public StructuredLogger.LogLevel getLevel() {
        return level;
    }
    
    public String getOperation() {
        return operation;
    }
    
    public StructuredLogger.AdminOperationType getOperationType() {
        return operationType;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public String getAdminUserId() {
        return adminUserId;
    }
    
    public String getCorrelationId() {
        return correlationId;
    }
    
    public Duration getDuration() {
        return duration;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public Map<String, Object> getDetails() {
        return details;
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public String getUserAgent() {
        return userAgent;
    }
    
    /**
     * Creates a new builder for AdminLogEvent.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Builder class for AdminLogEvent.
     */
    public static class Builder {
        private Instant timestamp;
        private StructuredLogger.LogLevel level;
        private String operation;
        private StructuredLogger.AdminOperationType operationType;
        private String userId;
        private String adminUserId;
        private String correlationId;
        private Duration duration;
        private boolean success;
        private String errorCode;
        private String errorMessage;
        private Map<String, Object> details;
        private String ipAddress;
        private String userAgent;
        
        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }
        
        public Builder level(StructuredLogger.LogLevel level) {
            this.level = level;
            return this;
        }
        
        public Builder operation(String operation) {
            this.operation = operation;
            return this;
        }
        
        public Builder operationType(StructuredLogger.AdminOperationType operationType) {
            this.operationType = operationType;
            return this;
        }
        
        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }
        
        public Builder adminUserId(String adminUserId) {
            this.adminUserId = adminUserId;
            return this;
        }
        
        public Builder correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }
        
        public Builder duration(Duration duration) {
            this.duration = duration;
            return this;
        }
        
        public Builder success(boolean success) {
            this.success = success;
            return this;
        }
        
        public Builder errorCode(String errorCode) {
            this.errorCode = errorCode;
            return this;
        }
        
        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }
        
        public Builder details(Map<String, Object> details) {
            this.details = details;
            return this;
        }
        
        public Builder ipAddress(String ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }
        
        public Builder userAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }
        
        public AdminLogEvent build() {
            return new AdminLogEvent(this);
        }
    }
    
    @Override
    public String toString() {
        return String.format("AdminLogEvent{timestamp=%s, level=%s, operation='%s', " +
            "operationType=%s, userId='%s', adminUserId='%s', correlationId='%s', " +
            "duration=%s, success=%s, errorCode='%s', errorMessage='%s'}",
            timestamp, level, operation, operationType, userId, adminUserId, 
            correlationId, duration, success, errorCode, errorMessage);
    }
}