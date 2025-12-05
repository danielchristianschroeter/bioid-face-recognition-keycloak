package com.bioid.keycloak.reconciliation;

import java.time.Instant;

/**
 * Error during synchronization.
 */
public class SynchronizationError {
    private String issueId;
    private String errorMessage;
    private Instant occurredAt;

    public SynchronizationError(String issueId, String errorMessage) {
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
}

