package com.bioid.keycloak.admin.dto;

/**
 * DTO for template statistics displayed on the dashboard.
 */
public class TemplateStatistics {
    private int totalTemplates;
    private int healthyTemplates;
    private int needsUpgrade;
    private int expiringSoon;

    public TemplateStatistics() {}

    private TemplateStatistics(Builder builder) {
        this.totalTemplates = builder.totalTemplates;
        this.healthyTemplates = builder.healthyTemplates;
        this.needsUpgrade = builder.needsUpgrade;
        this.expiringSoon = builder.expiringSoon;
    }

    public static Builder builder() {
        return new Builder();
    }

    public int getTotalTemplates() {
        return totalTemplates;
    }

    public void setTotalTemplates(int totalTemplates) {
        this.totalTemplates = totalTemplates;
    }

    public int getHealthyTemplates() {
        return healthyTemplates;
    }

    public void setHealthyTemplates(int healthyTemplates) {
        this.healthyTemplates = healthyTemplates;
    }

    public int getNeedsUpgrade() {
        return needsUpgrade;
    }

    public void setNeedsUpgrade(int needsUpgrade) {
        this.needsUpgrade = needsUpgrade;
    }

    public int getExpiringSoon() {
        return expiringSoon;
    }

    public void setExpiringSoon(int expiringSoon) {
        this.expiringSoon = expiringSoon;
    }

    public static class Builder {
        private int totalTemplates;
        private int healthyTemplates;
        private int needsUpgrade;
        private int expiringSoon;

        public Builder totalTemplates(int totalTemplates) {
            this.totalTemplates = totalTemplates;
            return this;
        }

        public Builder healthyTemplates(int healthyTemplates) {
            this.healthyTemplates = healthyTemplates;
            return this;
        }

        public Builder needsUpgrade(int needsUpgrade) {
            this.needsUpgrade = needsUpgrade;
            return this;
        }

        public Builder expiringSoon(int expiringSoon) {
            this.expiringSoon = expiringSoon;
            return this;
        }

        public TemplateStatistics build() {
            return new TemplateStatistics(this);
        }
    }
}