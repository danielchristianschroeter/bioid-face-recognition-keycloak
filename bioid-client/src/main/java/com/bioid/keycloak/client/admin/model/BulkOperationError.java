package com.bioid.keycloak.client.admin.model;

/**
 * Error information for a failed item in a bulk operation.
 */
public class BulkOperationError {
    private String itemId;
    private String errorCode;
    private String errorMessage;
    private boolean retryable;
    private int attemptCount;

    public BulkOperationError() {
        this.attemptCount = 1;
    }

    public BulkOperationError(String itemId, String errorCode, String errorMessage, boolean retryable) {
        this.itemId = itemId;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.retryable = retryable;
        this.attemptCount = 1;
    }

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

    public void incrementAttemptCount() {
        this.attemptCount++;
    }
}