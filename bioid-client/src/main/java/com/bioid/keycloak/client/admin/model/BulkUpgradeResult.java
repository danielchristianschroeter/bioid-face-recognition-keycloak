package com.bioid.keycloak.client.admin.model;

import java.time.Instant;
import java.util.List;

/**
 * Result of a bulk template upgrade operation.
 */
public class BulkUpgradeResult {
    private final int totalTemplates;
    private final int successfulUpgrades;
    private final int failedUpgrades;
    private final List<TemplateUpgradeResult> results;
    private final Instant startedAt;
    private final Instant completedAt;

    public BulkUpgradeResult(int totalTemplates, int successfulUpgrades, int failedUpgrades,
                           List<TemplateUpgradeResult> results, Instant startedAt, Instant completedAt) {
        this.totalTemplates = totalTemplates;
        this.successfulUpgrades = successfulUpgrades;
        this.failedUpgrades = failedUpgrades;
        this.results = results;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
    }

    public int getTotalTemplates() {
        return totalTemplates;
    }

    public int getSuccessfulUpgrades() {
        return successfulUpgrades;
    }

    public int getFailedUpgrades() {
        return failedUpgrades;
    }

    public List<TemplateUpgradeResult> getResults() {
        return results;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public double getSuccessRate() {
        return totalTemplates > 0 ? (double) successfulUpgrades / totalTemplates * 100.0 : 0.0;
    }

    public long getDurationMillis() {
        return completedAt.toEpochMilli() - startedAt.toEpochMilli();
    }
}