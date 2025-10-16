package com.bioid.keycloak.reconciliation;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Result of a repair operation for consistency issues
 */
@SuppressWarnings("auxiliaryclass")
public class RepairResult {
    
    private String realmId;
    private Instant startedAt;
    private Instant completedAt;
    private RepairStatus status = RepairStatus.RUNNING;
    private String errorMessage;
    private List<RepairAction> successfulRepairs = new ArrayList<>();
    private List<RepairAction> pendingApprovals = new ArrayList<>();
    private List<RepairError> errors = new ArrayList<>();

    // Getters and setters
    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public RepairStatus getStatus() {
        return status;
    }

    public void setStatus(RepairStatus status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public List<RepairAction> getSuccessfulRepairs() {
        return new ArrayList<>(successfulRepairs);
    }

    public void setSuccessfulRepairs(List<RepairAction> successfulRepairs) {
        this.successfulRepairs = new ArrayList<>(successfulRepairs);
    }

    public void addSuccessfulRepair(RepairAction repair) {
        this.successfulRepairs.add(repair);
    }

    public List<RepairAction> getPendingApprovals() {
        return new ArrayList<>(pendingApprovals);
    }

    public void setPendingApprovals(List<RepairAction> pendingApprovals) {
        this.pendingApprovals = new ArrayList<>(pendingApprovals);
    }

    public void addPendingApproval(RepairAction repair) {
        this.pendingApprovals.add(repair);
    }

    public List<RepairError> getErrors() {
        return new ArrayList<>(errors);
    }

    public void setErrors(List<RepairError> errors) {
        this.errors = new ArrayList<>(errors);
    }

    public void addError(RepairError error) {
        this.errors.add(error);
    }

    // Utility methods
    public int getTotalRepairs() {
        return successfulRepairs.size() + pendingApprovals.size() + errors.size();
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public boolean hasPendingApprovals() {
        return !pendingApprovals.isEmpty();
    }

    public long getDurationMs() {
        if (startedAt == null) {
            return 0;
        }
        Instant endTime = completedAt != null ? completedAt : Instant.now();
        return endTime.toEpochMilli() - startedAt.toEpochMilli();
    }
}

/**
 * Status of repair operation
 */
enum RepairStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED,
    PARTIALLY_COMPLETED
}

/**
 * Action taken during repair
 */
class RepairAction {
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

    // Getters and setters
    public String getIssueId() { return issueId; }
    public void setIssueId(String issueId) { this.issueId = issueId; }

    public RepairActionType getType() { return type; }
    public void setType(RepairActionType type) { this.type = type; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Instant getExecutedAt() { return executedAt; }
    public void setExecutedAt(Instant executedAt) { this.executedAt = executedAt; }

    public boolean isRequiresApproval() { return requiresApproval; }
    public void setRequiresApproval(boolean requiresApproval) { this.requiresApproval = requiresApproval; }

    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }

    public Instant getApprovedAt() { return approvedAt; }
    public void setApprovedAt(Instant approvedAt) { this.approvedAt = approvedAt; }

    public boolean isSuccessful() { return successful; }
    public void setSuccessful(boolean successful) { this.successful = successful; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}

/**
 * Types of repair actions
 */
enum RepairActionType {
    DELETE_ORPHANED_CREDENTIAL,
    DELETE_ORPHANED_TEMPLATE,
    UPDATE_METADATA,
    RECREATE_CREDENTIAL,
    RECREATE_TEMPLATE,
    RESOLVE_CONFLICT,
    MANUAL_INTERVENTION_REQUIRED
}

/**
 * Error during repair
 */
class RepairError {
    private String issueId;
    private String errorMessage;
    private Instant occurredAt;
    private boolean retryable;

    public RepairError(String issueId, String errorMessage) {
        this.issueId = issueId;
        this.errorMessage = errorMessage;
        this.occurredAt = Instant.now();
    }

    // Getters and setters
    public String getIssueId() { return issueId; }
    public void setIssueId(String issueId) { this.issueId = issueId; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }

    public boolean isRetryable() { return retryable; }
    public void setRetryable(boolean retryable) { this.retryable = retryable; }
}