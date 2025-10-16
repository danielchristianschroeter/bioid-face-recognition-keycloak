package com.bioid.keycloak.reconciliation;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Report containing the results of a data consistency check between Keycloak and BioID
 */
@SuppressWarnings({"auxiliaryclass", "all"})
public class DataConsistencyReport {
    
    private String realmId;
    private String realmName;
    private Instant startedAt;
    private Instant completedAt;
    private ConsistencyCheckStatus status = ConsistencyCheckStatus.RUNNING;
    private String errorMessage;
    private List<ConsistencyIssue> issues = new ArrayList<>();
    private ConsistencyStatistics statistics = new ConsistencyStatistics();

    public DataConsistencyReport(String realmId, String realmName) {
        this.realmId = realmId;
        this.realmName = realmName;
        this.startedAt = Instant.now();
    }

    // Getters and setters
    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
    }

    public String getRealmName() {
        return realmName;
    }

    public void setRealmName(String realmName) {
        this.realmName = realmName;
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

    public ConsistencyCheckStatus getStatus() {
        return status;
    }

    public void setStatus(ConsistencyCheckStatus status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public List<ConsistencyIssue> getIssues() {
        return new ArrayList<>(issues);
    }

    public void setIssues(List<ConsistencyIssue> issues) {
        this.issues = new ArrayList<>(issues);
        updateStatistics();
    }

    public void addIssue(ConsistencyIssue issue) {
        this.issues.add(issue);
        updateStatistics();
    }

    public ConsistencyStatistics getStatistics() {
        return statistics;
    }

    // Utility methods
    public int getTotalIssues() {
        return issues.size();
    }

    public long getDurationMs() {
        if (startedAt == null) {
            return 0;
        }
        Instant endTime = completedAt != null ? completedAt : Instant.now();
        return endTime.toEpochMilli() - startedAt.toEpochMilli();
    }

    public List<ConsistencyIssue> getIssuesByType(ConsistencyIssueType type) {
        return issues.stream()
            .filter(issue -> issue.getType() == type)
            .collect(Collectors.toList());
    }

    public List<ConsistencyIssue> getIssuesBySeverity(IssueSeverity severity) {
        return issues.stream()
            .filter(issue -> issue.getSeverity() == severity)
            .collect(Collectors.toList());
    }

    public boolean hasHighSeverityIssues() {
        return issues.stream().anyMatch(issue -> issue.getSeverity() == IssueSeverity.HIGH);
    }

    public boolean hasCriticalIssues() {
        return issues.stream().anyMatch(issue -> issue.getSeverity() == IssueSeverity.CRITICAL);
    }

    private void updateStatistics() {
        statistics = new ConsistencyStatistics();
        statistics.setTotalIssues(issues.size());
        
        Map<ConsistencyIssueType, Long> issuesByType = issues.stream()
            .collect(Collectors.groupingBy(ConsistencyIssue::getType, Collectors.counting()));
        
        statistics.setOrphanedCredentials(issuesByType.getOrDefault(ConsistencyIssueType.ORPHANED_CREDENTIAL, 0L).intValue());
        statistics.setOrphanedTemplates(issuesByType.getOrDefault(ConsistencyIssueType.ORPHANED_TEMPLATE, 0L).intValue());
        statistics.setMetadataMismatches(issuesByType.getOrDefault(ConsistencyIssueType.METADATA_MISMATCH, 0L).intValue());
        statistics.setSyncConflicts(issuesByType.getOrDefault(ConsistencyIssueType.SYNC_CONFLICT, 0L).intValue());
        
        Map<IssueSeverity, Long> issuesBySeverity = issues.stream()
            .collect(Collectors.groupingBy(ConsistencyIssue::getSeverity, Collectors.counting()));
        
        statistics.setCriticalIssues(issuesBySeverity.getOrDefault(IssueSeverity.CRITICAL, 0L).intValue());
        statistics.setHighSeverityIssues(issuesBySeverity.getOrDefault(IssueSeverity.HIGH, 0L).intValue());
        statistics.setMediumSeverityIssues(issuesBySeverity.getOrDefault(IssueSeverity.MEDIUM, 0L).intValue());
        statistics.setLowSeverityIssues(issuesBySeverity.getOrDefault(IssueSeverity.LOW, 0L).intValue());
    }

    @Override
    public String toString() {
        return String.format("DataConsistencyReport{realmName='%s', status=%s, totalIssues=%d, duration=%dms}", 
            realmName, status, getTotalIssues(), getDurationMs());
    }

    /**
     * Statistics summary for the consistency report
     */
    public static class ConsistencyStatistics {
        private int totalIssues;
        private int orphanedCredentials;
        private int orphanedTemplates;
        private int metadataMismatches;
        private int syncConflicts;
        private int criticalIssues;
        private int highSeverityIssues;
        private int mediumSeverityIssues;
        private int lowSeverityIssues;

        // Getters and setters
        public int getTotalIssues() { return totalIssues; }
        public void setTotalIssues(int totalIssues) { this.totalIssues = totalIssues; }

        public int getOrphanedCredentials() { return orphanedCredentials; }
        public void setOrphanedCredentials(int orphanedCredentials) { this.orphanedCredentials = orphanedCredentials; }

        public int getOrphanedTemplates() { return orphanedTemplates; }
        public void setOrphanedTemplates(int orphanedTemplates) { this.orphanedTemplates = orphanedTemplates; }

        public int getMetadataMismatches() { return metadataMismatches; }
        public void setMetadataMismatches(int metadataMismatches) { this.metadataMismatches = metadataMismatches; }

        public int getSyncConflicts() { return syncConflicts; }
        public void setSyncConflicts(int syncConflicts) { this.syncConflicts = syncConflicts; }

        public int getCriticalIssues() { return criticalIssues; }
        public void setCriticalIssues(int criticalIssues) { this.criticalIssues = criticalIssues; }

        public int getHighSeverityIssues() { return highSeverityIssues; }
        public void setHighSeverityIssues(int highSeverityIssues) { this.highSeverityIssues = highSeverityIssues; }

        public int getMediumSeverityIssues() { return mediumSeverityIssues; }
        public void setMediumSeverityIssues(int mediumSeverityIssues) { this.mediumSeverityIssues = mediumSeverityIssues; }

        public int getLowSeverityIssues() { return lowSeverityIssues; }
        public void setLowSeverityIssues(int lowSeverityIssues) { this.lowSeverityIssues = lowSeverityIssues; }
    }
}

/**
 * Status of consistency check operation
 */
enum ConsistencyCheckStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED
}