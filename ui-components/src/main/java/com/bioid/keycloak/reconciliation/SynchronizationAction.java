package com.bioid.keycloak.reconciliation;

import java.time.Instant;

/**
 * Action taken during synchronization.
 */
public class SynchronizationAction {
    private String issueId;
    private SynchronizationActionType type;
    private String description;
    private Instant executedAt;
    private boolean successful = true;
    private String errorMessage;

    public SynchronizationAction(String issueId, SynchronizationActionType type, String description) {
        this.issueId = issueId;
        this.type = type;
        this.description = description;
        this.executedAt = Instant.now();
    }

    public String getIssueId() {
        return issueId;
    }

    public void setIssueId(String issueId) {
        this.issueId = issueId;
    }

    public SynchronizationActionType getType() {
        return type;
    }

    public void setType(SynchronizationActionType type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(Instant executedAt) {
        this.executedAt = executedAt;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}

