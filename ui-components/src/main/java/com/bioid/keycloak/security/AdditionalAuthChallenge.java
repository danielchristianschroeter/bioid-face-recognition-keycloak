package com.bioid.keycloak.security;

import com.bioid.keycloak.audit.AdminActionType;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * Challenge for additional authentication requirements for high-risk operations.
 */
public class AdditionalAuthChallenge {
    
    private String challengeId;
    private String userId;
    private AdminActionType action;
    private Set<AuthMethod> requiredMethods;
    private Set<AuthMethod> completedMethods;
    private Instant createdAt;
    private Instant expiresAt;
    private Instant completedAt;
    private boolean completed;
    private String failureReason;

    public AdditionalAuthChallenge() {
        this.requiredMethods = new HashSet<>();
        this.completedMethods = new HashSet<>();
        this.completed = false;
    }

    public String getChallengeId() {
        return challengeId;
    }

    public void setChallengeId(String challengeId) {
        this.challengeId = challengeId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public AdminActionType getAction() {
        return action;
    }

    public void setAction(AdminActionType action) {
        this.action = action;
    }

    public Set<AuthMethod> getRequiredMethods() {
        return requiredMethods;
    }

    public void setRequiredMethods(Set<AuthMethod> requiredMethods) {
        this.requiredMethods = requiredMethods;
    }

    public Set<AuthMethod> getCompletedMethods() {
        return completedMethods;
    }

    public void setCompletedMethods(Set<AuthMethod> completedMethods) {
        this.completedMethods = completedMethods;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    /**
     * Adds a required authentication method.
     * 
     * @param method The authentication method to require
     */
    public void addRequiredMethod(AuthMethod method) {
        this.requiredMethods.add(method);
    }

    /**
     * Marks an authentication method as completed.
     * 
     * @param method The completed authentication method
     */
    public void completeMethod(AuthMethod method) {
        this.completedMethods.add(method);
    }

    /**
     * Checks if the challenge has expired.
     * 
     * @return true if the challenge has expired
     */
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    /**
     * Checks if all required authentication methods have been completed.
     * 
     * @return true if all methods are completed
     */
    public boolean areAllMethodsCompleted() {
        return completedMethods.containsAll(requiredMethods);
    }

    /**
     * Gets the remaining authentication methods that need to be completed.
     * 
     * @return Set of remaining methods
     */
    public Set<AuthMethod> getRemainingMethods() {
        Set<AuthMethod> remaining = new HashSet<>(requiredMethods);
        remaining.removeAll(completedMethods);
        return remaining;
    }

    /**
     * Gets the progress percentage of completed authentication methods.
     * 
     * @return Progress percentage (0-100)
     */
    public int getProgressPercentage() {
        if (requiredMethods.isEmpty()) {
            return 100;
        }
        
        return (completedMethods.size() * 100) / requiredMethods.size();
    }

    @Override
    public String toString() {
        return String.format("AdditionalAuthChallenge{challengeId='%s', userId='%s', action=%s, " +
                           "requiredMethods=%s, completedMethods=%s, completed=%s, expired=%s}", 
                           challengeId, userId, action, requiredMethods, completedMethods, 
                           completed, isExpired());
    }
}