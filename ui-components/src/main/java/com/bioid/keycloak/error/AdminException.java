package com.bioid.keycloak.error;

import java.time.Instant;
import java.util.Map;
import java.util.HashMap;

/**
 * Exception class for administrative operations in the BioID system.
 * Provides detailed error information including error type, context, and retry information.
 */
public class AdminException extends RuntimeException {
    
    private final AdminErrorType errorType;
    private final String errorCode;
    private final Map<String, Object> context;
    private final Instant timestamp;
    private final boolean retryable;
    private int attemptCount = 0;

    public AdminException(AdminErrorType errorType) {
        this(errorType, errorType.getDescription(), null, new HashMap<>());
    }

    public AdminException(AdminErrorType errorType, String message) {
        this(errorType, message, null, new HashMap<>());
    }

    public AdminException(AdminErrorType errorType, String message, Throwable cause) {
        this(errorType, message, cause, new HashMap<>());
    }

    public AdminException(AdminErrorType errorType, String message, Map<String, Object> context) {
        this(errorType, message, null, context);
    }

    public AdminException(AdminErrorType errorType, String message, Throwable cause, Map<String, Object> context) {
        super(message, cause);
        this.errorType = errorType;
        this.errorCode = errorType.getCode();
        this.context = new HashMap<>(context);
        this.timestamp = Instant.now();
        this.retryable = errorType.isRetryable();
    }

    public AdminErrorType getErrorType() {
        return errorType;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Map<String, Object> getContext() {
        return new HashMap<>(context);
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public boolean isRetryable() {
        return retryable;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public void incrementAttemptCount() {
        this.attemptCount++;
    }

    /**
     * Add context information to the exception
     */
    public AdminException withContext(String key, Object value) {
        this.context.put(key, value);
        return this;
    }

    /**
     * Add multiple context entries
     */
    public AdminException withContext(Map<String, Object> additionalContext) {
        this.context.putAll(additionalContext);
        return this;
    }

    /**
     * Create a user-friendly error message
     */
    public String getUserFriendlyMessage() {
        switch (errorType) {
            case TEMPLATE_NOT_FOUND:
                return "The requested biometric template could not be found. The user may not be enrolled.";
            case TEMPLATE_CORRUPTED:
                return "The biometric template is corrupted. Please re-enroll the user.";
            case LIVENESS_DETECTION_FAILED:
                return "Liveness detection failed. Please ensure proper lighting and face positioning.";
            case BULK_OPERATION_TIMEOUT:
                return "The bulk operation took too long to complete. Please try with a smaller batch size.";
            case SERVICE_UNAVAILABLE:
                return "The biometric service is temporarily unavailable. Please try again later.";
            case PERMISSION_DENIED:
                return "You don't have permission to perform this operation.";
            case RATE_LIMIT_EXCEEDED:
                return "Too many requests. Please wait a moment before trying again.";
            default:
                return "An error occurred: " + getMessage();
        }
    }

    @Override
    public String toString() {
        return String.format("AdminException{errorType=%s, errorCode='%s', message='%s', retryable=%s, attemptCount=%d, timestamp=%s, context=%s}",
                errorType, errorCode, getMessage(), retryable, attemptCount, timestamp, context);
    }
}