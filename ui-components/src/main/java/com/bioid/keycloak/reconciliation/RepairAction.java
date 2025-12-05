package com.bioid.keycloak.reconciliation;

import java.time.Instant;

/**
 * Action taken during repair.
 */
public class RepairAction {
    private String issueId;
    private RepairActionType type;
    private String description;
    private Instant executedAt;
    private boolean requiresApproval;
    private String approvedBy;
    private Instant approvedAt;
    private boolean successful = true;
    private String errorMessage;

    public RepairAction(String issueId, RepairActionType type, String description) {
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

    public RepairActionType getType() {
        return type;
    }

    public void setType(RepairActionType type) {
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

    public boolean isRequiresApproval() {
        return requiresApproval;
    }

    public void setRequiresApproval(boolean requiresApproval) {
        this.requiresApproval = requiresApproval;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(Instant approvedAt) {
        this.approvedAt = approvedAt;
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

