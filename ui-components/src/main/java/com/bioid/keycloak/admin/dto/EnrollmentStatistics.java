package com.bioid.keycloak.admin.dto;

import java.util.List;
import java.util.Map;

/**
 * DTO for enrollment statistics displayed on the dashboard.
 */
public class EnrollmentStatistics {
    private int totalEnrollments;
    private double successRate;
    private int enrollmentsToday;
    private int enrollmentsThisWeek;
    private Map<String, Integer> changes;
    private List<TrendDataPoint> trendData;

    public EnrollmentStatistics() {}

    private EnrollmentStatistics(Builder builder) {
        this.totalEnrollments = builder.totalEnrollments;
        this.successRate = builder.successRate;
        this.enrollmentsToday = builder.enrollmentsToday;
        this.enrollmentsThisWeek = builder.enrollmentsThisWeek;
        this.changes = builder.changes;
        this.trendData = builder.trendData;
    }

    public static Builder builder() {
        return new Builder();
    }

    public int getTotalEnrollments() {
        return totalEnrollments;
    }

    public void setTotalEnrollments(int totalEnrollments) {
        this.totalEnrollments = totalEnrollments;
    }

    public double getSuccessRate() {
        return successRate;
    }

    public void setSuccessRate(double successRate) {
        this.successRate = successRate;
    }

    public int getEnrollmentsToday() {
        return enrollmentsToday;
    }

    public void setEnrollmentsToday(int enrollmentsToday) {
        this.enrollmentsToday = enrollmentsToday;
    }

    public int getEnrollmentsThisWeek() {
        return enrollmentsThisWeek;
    }

    public void setEnrollmentsThisWeek(int enrollmentsThisWeek) {
        this.enrollmentsThisWeek = enrollmentsThisWeek;
    }

    public Map<String, Integer> getChanges() {
        return changes;
    }

    public void setChanges(Map<String, Integer> changes) {
        this.changes = changes;
    }

    public List<TrendDataPoint> getTrendData() {
        return trendData;
    }

    public void setTrendData(List<TrendDataPoint> trendData) {
        this.trendData = trendData;
    }

    public static class Builder {
        private int totalEnrollments;
        private double successRate;
        private int enrollmentsToday;
        private int enrollmentsThisWeek;
        private Map<String, Integer> changes;
        private List<TrendDataPoint> trendData;

        public Builder totalEnrollments(int totalEnrollments) {
            this.totalEnrollments = totalEnrollments;
            return this;
        }

        public Builder successRate(double successRate) {
            this.successRate = successRate;
            return this;
        }

        public Builder enrollmentsToday(int enrollmentsToday) {
            this.enrollmentsToday = enrollmentsToday;
            return this;
        }

        public Builder enrollmentsThisWeek(int enrollmentsThisWeek) {
            this.enrollmentsThisWeek = enrollmentsThisWeek;
            return this;
        }

        public Builder changes(Map<String, Integer> changes) {
            this.changes = changes;
            return this;
        }

        public Builder trendData(List<TrendDataPoint> trendData) {
            this.trendData = trendData;
            return this;
        }

        public EnrollmentStatistics build() {
            return new EnrollmentStatistics(this);
        }
    }

    public static class TrendDataPoint {
        private String date;
        private int value;

        public TrendDataPoint() {}

        public TrendDataPoint(String date, int value) {
            this.date = date;
            this.value = value;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }
    }
}