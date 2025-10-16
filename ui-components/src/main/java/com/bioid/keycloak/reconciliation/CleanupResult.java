package com.bioid.keycloak.reconciliation;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Result of an orphaned data cleanup operation
 */
@SuppressWarnings("auxiliaryclass")
public class CleanupResult {
    
    private String realmId;
    private boolean dryRun;
    private Instant startedAt;
    private Instant completedAt;
    private CleanupStatus status = CleanupStatus.RUNNING;
    private String errorMessage;
    private List<String> cleanedCredentials = new ArrayList<>();
    private List<Long> cleanedTemplates = new ArrayList<>();
    private List<String> credentialsToClean = new ArrayList<>();
    private List<Long> templatesToClean = new ArrayList<>();
    private List<CleanupError> errors = new ArrayList<>();

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

    public CleanupStatus getStatus() {
        return status;
    }

    public void setStatus(CleanupStatus status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public List<String> getCleanedCredentials() {
        return new ArrayList<>(cleanedCredentials);
    }

    public void setCleanedCredentials(List<String> cleanedCredentials) {
        this.cleanedCredentials = new ArrayList<>(cleanedCredentials);
    }

    public void addCleanedCredential(String userId) {
        this.cleanedCredentials.add(userId);
    }

    public List<Long> getCleanedTemplates() {
        return new ArrayList<>(cleanedTemplates);
    }

    public void setCleanedTemplates(List<Long> cleanedTemplates) {
        this.cleanedTemplates = new ArrayList<>(cleanedTemplates);
    }

    public void addCleanedTemplate(Long classId) {
        this.cleanedTemplates.add(classId);
    }

    public List<String> getCredentialsToClean() {
        return new ArrayList<>(credentialsToClean);
    }

    public void setCredentialsToClean(List<String> credentialsToClean) {
        this.credentialsToClean = new ArrayList<>(credentialsToClean);
    }

    public void addCredentialToClean(String userId) {
        this.credentialsToClean.add(userId);
    }

    public List<Long> getTemplatesToClean() {
        return new ArrayList<>(templatesToClean);
    }

    public void setTemplatesToClean(List<Long> templatesToClean) {
        this.templatesToClean = new ArrayList<>(templatesToClean);
    }

    public void addTemplateToClean(Long classId) {
        this.templatesToClean.add(classId);
    }

    public List<CleanupError> getErrors() {
        return new ArrayList<>(errors);
    }

    public void setErrors(List<CleanupError> errors) {
        this.errors = new ArrayList<>(errors);
    }

    public void addError(CleanupError error) {
        this.errors.add(error);
    }

    // Utility methods
    public int getTotalCredentialsProcessed() {
        return dryRun ? credentialsToClean.size() : cleanedCredentials.size();
    }

    public int getTotalTemplatesProcessed() {
        return dryRun ? templatesToClean.size() : cleanedTemplates.size();
    }

    public int getTotalItemsProcessed() {
        return getTotalCredentialsProcessed() + getTotalTemplatesProcessed();
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

    @Override
    public String toString() {
        return String.format("CleanupResult{realmId='%s', dryRun=%s, status=%s, credentials=%d, templates=%d, errors=%d}", 
            realmId, dryRun, status, getTotalCredentialsProcessed(), getTotalTemplatesProcessed(), errors.size());
    }
}

/**
 * Status of cleanup operation
 */
enum CleanupStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED,
    PARTIALLY_COMPLETED
}

/**
 * Error during cleanup
 */
class CleanupError {
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

    // Getters and setters
    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }

    public String getItemType() { return itemType; }
    public void setItemType(String itemType) { this.itemType = itemType; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }

    public boolean isRetryable() { return retryable; }
    public void setRetryable(boolean retryable) { this.retryable = retryable; }

    @Override
    public String toString() {
        return String.format("CleanupError{itemId='%s', itemType='%s', errorMessage='%s'}", 
            itemId, itemType, errorMessage);
    }
}