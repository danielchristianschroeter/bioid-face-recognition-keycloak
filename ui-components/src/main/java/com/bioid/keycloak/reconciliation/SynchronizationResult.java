package com.bioid.keycloak.reconciliation;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Result of a synchronization operation between Keycloak and BioID
 */
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