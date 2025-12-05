package com.bioid.keycloak.reconciliation;

import java.time.Instant;

/**
 * Error during cleanup.
 */
public class CleanupError {
    private String itemId;
    private String itemType; // "credential" or "template"
    private String errorMessage;
    private Instant occurredAt;
    private boolean retryable;

    public CleanupError(String itemId, String itemType, String errorMessage) {
        this.itemId = itemId;
        this.itemType = itemType;
        this.errorMessage = errorMessage;
        this.occurredAt = Instant.now();
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }

    public boolean isRetryable() {
        return retryable;
    }

    public void setRetryable(boolean retryable) {
        this.retryable = retryable;
    }

    @Override
    public String toString() {
        return String.format("CleanupError{itemId='%s', itemType='%s', errorMessage='%s'}",
            itemId, itemType, errorMessage);
    }
}

