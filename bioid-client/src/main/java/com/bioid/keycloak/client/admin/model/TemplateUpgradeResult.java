package com.bioid.keycloak.client.admin.model;

import java.time.Instant;

/**
 * Result of a template upgrade operation.
 */
public class TemplateUpgradeResult {
    private final long classId;
    private final boolean success;
    private final int oldEncoderVersion;
    private final int newEncoderVersion;
    private final String errorMessage;
    private final Instant upgradedAt;

    public TemplateUpgradeResult(long classId, boolean success, int oldEncoderVersion,
                               int newEncoderVersion, String errorMessage, Instant upgradedAt) {
        this.classId = classId;
        this.success = success;
        this.oldEncoderVersion = oldEncoderVersion;
        this.newEncoderVersion = newEncoderVersion;
        this.errorMessage = errorMessage;
        this.upgradedAt = upgradedAt;
    }

    public long getClassId() {
        return classId;
    }

    public boolean isSuccess() {
        return success;
    }

    public int getOldEncoderVersion() {
        return oldEncoderVersion;
    }

    public int getNewEncoderVersion() {
        return newEncoderVersion;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Instant getUpgradedAt() {
        return upgradedAt;
    }

    public static TemplateUpgradeResult success(long classId, int oldVersion, int newVersion) {
        return new TemplateUpgradeResult(classId, true, oldVersion, newVersion, null, Instant.now());
    }

    public static TemplateUpgradeResult failure(long classId, int currentVersion, String errorMessage) {
        return new TemplateUpgradeResult(classId, false, currentVersion, currentVersion, errorMessage, Instant.now());
    }
}