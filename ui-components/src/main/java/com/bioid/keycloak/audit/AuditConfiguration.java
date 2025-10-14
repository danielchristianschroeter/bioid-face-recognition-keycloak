package com.bioid.keycloak.audit;

/**
 * Configuration class for audit service settings.
 */
public class AuditConfiguration {
    
    private boolean detailedLoggingEnabled = true;
    private int retentionDays = 365;
    private boolean autoCleanupEnabled = false;
    private boolean siemIntegrationEnabled = false;
    private String siemEndpoint;
    private String siemApiKey;
    private boolean complianceReportingEnabled = true;
    private String auditLogLevel = "INFO";
    private int maxBatchSize = 1000;
    private boolean encryptSensitiveData = true;

    public AuditConfiguration() {
        // Load configuration from system properties or environment variables
        loadConfiguration();
    }

    private void loadConfiguration() {
        // Load from system properties with defaults
        this.detailedLoggingEnabled = Boolean.parseBoolean(
            System.getProperty("bioid.audit.detailed.enabled", "true"));
        
        this.retentionDays = Integer.parseInt(
            System.getProperty("bioid.audit.retention.days", "365"));
        
        this.autoCleanupEnabled = Boolean.parseBoolean(
            System.getProperty("bioid.audit.cleanup.enabled", "false"));
        
        this.siemIntegrationEnabled = Boolean.parseBoolean(
            System.getProperty("bioid.audit.siem.enabled", "false"));
        
        this.siemEndpoint = System.getProperty("bioid.audit.siem.endpoint");
        this.siemApiKey = System.getProperty("bioid.audit.siem.apikey");
        
        this.complianceReportingEnabled = Boolean.parseBoolean(
            System.getProperty("bioid.audit.compliance.enabled", "true"));
        
        this.auditLogLevel = System.getProperty("bioid.audit.log.level", "INFO");
        
        this.maxBatchSize = Integer.parseInt(
            System.getProperty("bioid.audit.batch.size", "1000"));
        
        this.encryptSensitiveData = Boolean.parseBoolean(
            System.getProperty("bioid.audit.encrypt.sensitive", "true"));
    }

    // Getters and Setters
    public boolean isDetailedLoggingEnabled() {
        return detailedLoggingEnabled;
    }

    public void setDetailedLoggingEnabled(boolean detailedLoggingEnabled) {
        this.detailedLoggingEnabled = detailedLoggingEnabled;
    }

    public int getRetentionDays() {
        return retentionDays;
    }

    public void setRetentionDays(int retentionDays) {
        this.retentionDays = retentionDays;
    }

    public boolean isAutoCleanupEnabled() {
        return autoCleanupEnabled;
    }

    public void setAutoCleanupEnabled(boolean autoCleanupEnabled) {
        this.autoCleanupEnabled = autoCleanupEnabled;
    }

    public boolean isSiemIntegrationEnabled() {
        return siemIntegrationEnabled;
    }

    public void setSiemIntegrationEnabled(boolean siemIntegrationEnabled) {
        this.siemIntegrationEnabled = siemIntegrationEnabled;
    }

    public String getSiemEndpoint() {
        return siemEndpoint;
    }

    public void setSiemEndpoint(String siemEndpoint) {
        this.siemEndpoint = siemEndpoint;
    }

    public String getSiemApiKey() {
        return siemApiKey;
    }

    public void setSiemApiKey(String siemApiKey) {
        this.siemApiKey = siemApiKey;
    }

    public boolean isComplianceReportingEnabled() {
        return complianceReportingEnabled;
    }

    public void setComplianceReportingEnabled(boolean complianceReportingEnabled) {
        this.complianceReportingEnabled = complianceReportingEnabled;
    }

    public String getAuditLogLevel() {
        return auditLogLevel;
    }

    public void setAuditLogLevel(String auditLogLevel) {
        this.auditLogLevel = auditLogLevel;
    }

    public int getMaxBatchSize() {
        return maxBatchSize;
    }

    public void setMaxBatchSize(int maxBatchSize) {
        this.maxBatchSize = maxBatchSize;
    }

    public boolean isEncryptSensitiveData() {
        return encryptSensitiveData;
    }

    public void setEncryptSensitiveData(boolean encryptSensitiveData) {
        this.encryptSensitiveData = encryptSensitiveData;
    }

    /**
     * Validates the current configuration.
     * 
     * @throws IllegalArgumentException if configuration is invalid
     */
    public void validate() {
        if (retentionDays < 1) {
            throw new IllegalArgumentException("Retention days must be at least 1");
        }
        
        if (maxBatchSize < 1) {
            throw new IllegalArgumentException("Max batch size must be at least 1");
        }
        
        if (siemIntegrationEnabled && (siemEndpoint == null || siemEndpoint.trim().isEmpty())) {
            throw new IllegalArgumentException("SIEM endpoint must be configured when SIEM integration is enabled");
        }
    }

    @Override
    public String toString() {
        return "AuditConfiguration{" +
                "detailedLoggingEnabled=" + detailedLoggingEnabled +
                ", retentionDays=" + retentionDays +
                ", autoCleanupEnabled=" + autoCleanupEnabled +
                ", siemIntegrationEnabled=" + siemIntegrationEnabled +
                ", siemEndpoint='" + (siemEndpoint != null ? "[CONFIGURED]" : "[NOT_CONFIGURED]") + '\'' +
                ", complianceReportingEnabled=" + complianceReportingEnabled +
                ", auditLogLevel='" + auditLogLevel + '\'' +
                ", maxBatchSize=" + maxBatchSize +
                ", encryptSensitiveData=" + encryptSensitiveData +
                '}';
    }
}