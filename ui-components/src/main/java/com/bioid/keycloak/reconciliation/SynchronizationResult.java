package com.bioid.keycloak.reconciliation;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Result of a synchronization operation between Keycloak and BioID
 */
@SuppressWarnings("auxiliaryclass")
public class SynchronizationResult {
    
    private String realmId;
    private boolean dryRun;
    private Instant startedAt;
    private Instant completedAt;
    private SynchronizationStatus status = SynchronizationStatus.RUNNING;
    private String errorMessage;
    private List<SynchronizationAction> executedActions = new ArrayList<>();
    private List<SynchronizationAction> plannedActions = new ArrayList<>();
    private List<SynchronizationError> errors = new ArrayList<>();

    // Getters and setters
    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
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

    public SynchronizationStatus getStatus() {
        return status;
    }

    public void setStatus(SynchronizationStatus status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public List<SynchronizationAction> getExecutedActions() {
        return new ArrayList<>(executedActions);
    }

    public void setExecutedActions(List<SynchronizationAction> executedActions) {
        this.executedActions = new ArrayList<>(executedActions);
    }

    public void addExecutedAction(SynchronizationAction action) {
        this.executedActions.add(action);
    }

    public List<SynchronizationAction> getPlannedActions() {
        return new ArrayList<>(plannedActions);
    }

    public void setPlannedActions(List<SynchronizationAction> plannedActions) {
        this.plannedActions = new ArrayList<>(plannedActions);
    }

    public void addPlannedAction(SynchronizationAction action) {
        this.plannedActions.add(action);
    }

    public List<SynchronizationError> getErrors() {
        return new ArrayList<>(errors);
    }

    public void setErrors(List<SynchronizationError> errors) {
        this.errors = new ArrayList<>(errors);
    }

    public void addError(SynchronizationError error) {
        this.errors.add(error);
    }

    // Utility methods
    public int getTotalActions() {
        return dryRun ? plannedActions.size() : executedActions.size();
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
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
 * Status of synchronization operation
 */
enum SynchronizationStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED
}

/**
 * Action taken during synchronization
 */
class SynchronizationAction {
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

    // Getters and setters
    public String getIssueId() { return issueId; }
    public void setIssueId(String issueId) { this.issueId = issueId; }

    public SynchronizationActionType getType() { return type; }
    public void setType(SynchronizationActionType type) { this.type = type; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Instant getExecutedAt() { return executedAt; }
    public void setExecutedAt(Instant executedAt) { this.executedAt = executedAt; }

    public boolean isSuccessful() { return successful; }
    public void setSuccessful(boolean successful) { this.successful = successful; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}

/**
 * Types of synchronization actions
 */
enum SynchronizationActionType {
    UPDATE_METADATA,
    CREATE_CREDENTIAL,
    DELETE_CREDENTIAL,
    CREATE_TEMPLATE,
    DELETE_TEMPLATE,
    RESOLVE_CONFLICT
}

/**
 * Error during synchronization
 */
class SynchronizationError {
    private String issueId;
    private String errorMessage;
    private Instant occurredAt;

    public SynchronizationError(String issueId, String errorMessage) {
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
}