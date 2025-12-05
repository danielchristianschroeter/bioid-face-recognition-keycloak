package com.bioid.keycloak.reconciliation;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Result of a repair operation for consistency issues
 */
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