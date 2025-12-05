package com.bioid.keycloak.reconciliation;

import java.time.Instant;

/**
 * Error during repair.
 */
public class RepairError {
    private String issueId;
    private String errorMessage;
    private Instant occurredAt;
    private boolean retryable;

    public RepairError(String issueId, String errorMessage) {
        this.issueId = issueId;
        this.errorMessage = errorMessage;
        this.occurredAt = Instant.now();
    }

    public String getIssueId() {
        return issueId;
    }

    public void setIssueId(String issueId) {
        this.issueId = issueId;
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
}

