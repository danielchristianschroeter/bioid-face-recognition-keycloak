package com.bioid.keycloak.error;

import java.time.Instant;
import java.util.Map;
import java.util.HashMap;

/**
 * Represents an error that occurred during a bulk operation for a specific item
 */
public class BulkOperationError {
    
    private String itemId;
    private String errorCode;
    private String errorMessage;
    private boolean retryable;
    private int attemptCount;
    private Map<String, Object> context = new HashMap<>();
    private Instant occurredAt;
    private String stackTrace;

    public BulkOperationError() {
        this.occurredAt = Instant.now();
    }

    public BulkOperationError(String itemId, String errorCode, String errorMessage) {
        this();
        this.itemId = itemId;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public BulkOperationError(String itemId, AdminException exception) {
        this();
        this.itemId = itemId;
        this.errorCode = exception.getErrorCode();
        this.errorMessage = exception.getMessage();
        this.retryable = exception.isRetryable();
        this.attemptCount = exception.getAttemptCount();
        this.context = exception.getContext();
        
        if (exception.getCause() != null) {
            this.stackTrace = getStackTraceString(exception);
        }
    }

    // Getters and setters
    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public boolean isRetryable() {
        return retryable;
    }

    public void setRetryable(boolean retryable) {
        this.retryable = retryable;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(int attemptCount) {
        this.attemptCount = attemptCount;
    }

    public Map<String, Object> getContext() {
        return new HashMap<>(context);
    }

    public void setContext(Map<String, Object> context) {
        this.context = new HashMap<>(context);
    }

    public void addContext(String key, Object value) {
        this.context.put(key, value);
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }

    // Utility methods
    public AdminErrorType getErrorType() {
        return AdminErrorType.fromCode(errorCode);
    }

    public String getUserFriendlyMessage() {
        AdminErrorType errorType = getErrorType();
        if (errorType != AdminErrorType.UNKNOWN_ERROR) {
            AdminException tempException = new AdminException(errorType, errorMessage);
            return tempException.getUserFriendlyMessage();
        }
        return errorMessage;
    }

    private String getStackTraceString(Throwable throwable) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }

    @Override
    public String toString() {
        return String.format("BulkOperationError{itemId='%s', errorCode='%s', errorMessage='%s', " +
                "retryable=%s, attemptCount=%d, occurredAt=%s}", 
                itemId, errorCode, errorMessage, retryable, attemptCount, occurredAt);
    }
}