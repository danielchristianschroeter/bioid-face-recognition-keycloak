package com.bioid.keycloak.admin.dto;

import java.util.List;
import java.util.Map;

/**
 * DTO for authentication metrics displayed on the dashboard.
 */
public class AuthenticationMetrics {
    private int totalAuthentications;
    private double successRate;
    private int avgResponseTime;
    private int failedAttempts;
    private Map<String, Integer> changes;
    private List<HourlyDataPoint> hourlyData;

    public AuthenticationMetrics() {}

    private AuthenticationMetrics(Builder builder) {
        this.totalAuthentications = builder.totalAuthentications;
        this.successRate = builder.successRate;
        this.avgResponseTime = builder.avgResponseTime;
        this.failedAttempts = builder.failedAttempts;
        this.changes = builder.changes;
        this.hourlyData = builder.hourlyData;
    }

    public static Builder builder() {
        return new Builder();
    }

    public int getTotalAuthentications() {
        return totalAuthentications;
    }

    public void setTotalAuthentications(int totalAuthentications) {
        this.totalAuthentications = totalAuthentications;
    }

    public double getSuccessRate() {
        return successRate;
    }

    public void setSuccessRate(double successRate) {
        this.successRate = successRate;
    }

    public int getAvgResponseTime() {
        return avgResponseTime;
    }

    public void setAvgResponseTime(int avgResponseTime) {
        this.avgResponseTime = avgResponseTime;
    }

    public int getFailedAttempts() {
        return failedAttempts;
    }

    public void setFailedAttempts(int failedAttempts) {
        this.failedAttempts = failedAttempts;
    }

    public Map<String, Integer> getChanges() {
        return changes;
    }

    public void setChanges(Map<String, Integer> changes) {
        this.changes = changes;
    }

    public List<HourlyDataPoint> getHourlyData() {
        return hourlyData;
    }

    public void setHourlyData(List<HourlyDataPoint> hourlyData) {
        this.hourlyData = hourlyData;
    }

    public static class Builder {
        private int totalAuthentications;
        private double successRate;
        private int avgResponseTime;
        private int failedAttempts;
        private Map<String, Integer> changes;
        private List<HourlyDataPoint> hourlyData;

        public Builder totalAuthentications(int totalAuthentications) {
            this.totalAuthentications = totalAuthentications;
            return this;
        }

        public Builder successRate(double successRate) {
            this.successRate = successRate;
            return this;
        }

        public Builder avgResponseTime(int avgResponseTime) {
            this.avgResponseTime = avgResponseTime;
            return this;
        }

        public Builder failedAttempts(int failedAttempts) {
            this.failedAttempts = failedAttempts;
            return this;
        }

        public Builder changes(Map<String, Integer> changes) {
            this.changes = changes;
            return this;
        }

        public Builder hourlyData(List<HourlyDataPoint> hourlyData) {
            this.hourlyData = hourlyData;
            return this;
        }

        public AuthenticationMetrics build() {
            return new AuthenticationMetrics(this);
        }
    }

    public static class HourlyDataPoint {
        private String hour;
        private double successRate;
        private int totalAttempts;

        public HourlyDataPoint() {}

        public HourlyDataPoint(String hour, double successRate, int totalAttempts) {
            this.hour = hour;
            this.successRate = successRate;
            this.totalAttempts = totalAttempts;
        }

        public String getHour() {
            return hour;
        }

        public void setHour(String hour) {
            this.hour = hour;
        }

        public double getSuccessRate() {
            return successRate;
        }

        public void setSuccessRate(double successRate) {
            this.successRate = successRate;
        }

        public int getTotalAttempts() {
            return totalAttempts;
        }

        public void setTotalAttempts(int totalAttempts) {
            this.totalAttempts = totalAttempts;
        }
    }
}