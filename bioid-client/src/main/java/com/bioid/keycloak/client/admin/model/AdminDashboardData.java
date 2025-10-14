package com.bioid.keycloak.client.admin.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Dashboard data for administrative overview of biometric enrollments and system health.
 */
public class AdminDashboardData {
    private final long totalEnrolledUsers;
    private final double enrollmentSuccessRate;
    private final Map<Integer, Long> templatesByEncoderVersion;
    private final List<RecentActivity> recentActivities;
    private final SystemHealthMetrics healthMetrics;
    private final LivenessDetectionStats livenessStats;

    public AdminDashboardData(long totalEnrolledUsers, double enrollmentSuccessRate,
                             Map<Integer, Long> templatesByEncoderVersion,
                             List<RecentActivity> recentActivities,
                             SystemHealthMetrics healthMetrics,
                             LivenessDetectionStats livenessStats) {
        this.totalEnrolledUsers = totalEnrolledUsers;
        this.enrollmentSuccessRate = enrollmentSuccessRate;
        this.templatesByEncoderVersion = templatesByEncoderVersion;
        this.recentActivities = recentActivities;
        this.healthMetrics = healthMetrics;
        this.livenessStats = livenessStats;
    }

    public long getTotalEnrolledUsers() {
        return totalEnrolledUsers;
    }

    public double getEnrollmentSuccessRate() {
        return enrollmentSuccessRate;
    }

    public Map<Integer, Long> getTemplatesByEncoderVersion() {
        return templatesByEncoderVersion;
    }

    public List<RecentActivity> getRecentActivities() {
        return recentActivities;
    }

    public SystemHealthMetrics getHealthMetrics() {
        return healthMetrics;
    }

    public LivenessDetectionStats getLivenessStats() {
        return livenessStats;
    }

    public static class RecentActivity {
        private final String userId;
        private final String username;
        private final String action;
        private final Instant timestamp;
        private final boolean success;

        public RecentActivity(String userId, String username, String action, Instant timestamp, boolean success) {
            this.userId = userId;
            this.username = username;
            this.action = action;
            this.timestamp = timestamp;
            this.success = success;
        }

        public String getUserId() {
            return userId;
        }

        public String getUsername() {
            return username;
        }

        public String getAction() {
            return action;
        }

        public Instant getTimestamp() {
            return timestamp;
        }

        public boolean isSuccess() {
            return success;
        }
    }

    public static class SystemHealthMetrics {
        private final ServiceStatus bioIdServiceStatus;
        private final double averageResponseTime;
        private final int activeConnections;
        private final long totalRequests24h;
        private final long failedRequests24h;
        private final double errorRate;
        private final Map<String, Integer> errorsByType;
        private final Instant lastHealthCheck;

        public SystemHealthMetrics(ServiceStatus bioIdServiceStatus, double averageResponseTime,
                                 int activeConnections, long totalRequests24h, long failedRequests24h,
                                 double errorRate, Map<String, Integer> errorsByType, Instant lastHealthCheck) {
            this.bioIdServiceStatus = bioIdServiceStatus;
            this.averageResponseTime = averageResponseTime;
            this.activeConnections = activeConnections;
            this.totalRequests24h = totalRequests24h;
            this.failedRequests24h = failedRequests24h;
            this.errorRate = errorRate;
            this.errorsByType = errorsByType;
            this.lastHealthCheck = lastHealthCheck;
        }

        public ServiceStatus getBioIdServiceStatus() {
            return bioIdServiceStatus;
        }

        public double getAverageResponseTime() {
            return averageResponseTime;
        }

        public int getActiveConnections() {
            return activeConnections;
        }

        public long getTotalRequests24h() {
            return totalRequests24h;
        }

        public long getFailedRequests24h() {
            return failedRequests24h;
        }

        public double getErrorRate() {
            return errorRate;
        }

        public Map<String, Integer> getErrorsByType() {
            return errorsByType;
        }

        public Instant getLastHealthCheck() {
            return lastHealthCheck;
        }
    }

    public static class LivenessDetectionStats {
        private final long totalLivenessChecks;
        private final long passedLivenessChecks;
        private final double passRate;
        private final Map<String, Long> checksByMode;
        private final Map<String, Long> rejectionReasons;
        private final double averageLivenessScore;

        public LivenessDetectionStats(long totalLivenessChecks, long passedLivenessChecks,
                                    double passRate, Map<String, Long> checksByMode,
                                    Map<String, Long> rejectionReasons, double averageLivenessScore) {
            this.totalLivenessChecks = totalLivenessChecks;
            this.passedLivenessChecks = passedLivenessChecks;
            this.passRate = passRate;
            this.checksByMode = checksByMode;
            this.rejectionReasons = rejectionReasons;
            this.averageLivenessScore = averageLivenessScore;
        }

        public long getTotalLivenessChecks() {
            return totalLivenessChecks;
        }

        public long getPassedLivenessChecks() {
            return passedLivenessChecks;
        }

        public double getPassRate() {
            return passRate;
        }

        public Map<String, Long> getChecksByMode() {
            return checksByMode;
        }

        public Map<String, Long> getRejectionReasons() {
            return rejectionReasons;
        }

        public double getAverageLivenessScore() {
            return averageLivenessScore;
        }
    }

    public enum ServiceStatus {
        HEALTHY,
        DEGRADED,
        UNHEALTHY,
        UNKNOWN
    }
}