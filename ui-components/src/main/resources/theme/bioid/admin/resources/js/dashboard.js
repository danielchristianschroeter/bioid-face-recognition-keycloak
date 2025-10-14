/**
 * Administrative Dashboard JavaScript
 * Handles real-time metrics, health monitoring, and dashboard interactions
 */

class AdminDashboard {
    constructor() {
        this.refreshInterval = 30000; // 30 seconds
        this.charts = {};
        this.refreshTimer = null;
        this.isVisible = true;
        
        this.init();
    }

    init() {
        this.bindEvents();
        this.loadInitialData();
        this.setupPeriodicRefresh();
        this.setupVisibilityHandling();
    }

    bindEvents() {
        // Refresh health status
        document.getElementById('refresh-health-btn').addEventListener('click', () => {
            this.loadHealthStatus();
        });

        // Liveness timeframe selection
        this.setupLivenessTimeframeSelect();

        // Quick action buttons
        document.getElementById('manage-templates-btn').addEventListener('click', () => {
            this.navigateToTemplateManagement();
        });

        document.getElementById('configure-liveness-btn').addEventListener('click', () => {
            this.navigateToLivenessConfig();
        });

        document.getElementById('view-reports-btn').addEventListener('click', () => {
            this.navigateToReports();
        });

        document.getElementById('system-settings-btn').addEventListener('click', () => {
            this.navigateToSystemSettings();
        });

        document.getElementById('view-all-activity-btn').addEventListener('click', () => {
            this.navigateToActivityLog();
        });
    }

    setupLivenessTimeframeSelect() {
        const select = document.getElementById('liveness-timeframe-select');
        const toggle = select.querySelector('.pf-c-select__toggle');
        const menu = select.querySelector('.pf-c-select__menu');
        const toggleText = toggle.querySelector('.pf-c-select__toggle-text');

        toggle.addEventListener('click', () => {
            const isExpanded = toggle.getAttribute('aria-expanded') === 'true';
            toggle.setAttribute('aria-expanded', !isExpanded);
            menu.hidden = isExpanded;
        });

        menu.addEventListener('click', (e) => {
            if (e.target.classList.contains('pf-c-select__menu-item')) {
                const value = e.target.dataset.value;
                const text = e.target.textContent;
                
                toggleText.textContent = text;
                toggle.setAttribute('aria-expanded', 'false');
                menu.hidden = true;
                
                this.loadLivenessStatistics(value);
            }
        });

        // Close dropdown when clicking outside
        document.addEventListener('click', (e) => {
            if (!select.contains(e.target)) {
                toggle.setAttribute('aria-expanded', 'false');
                menu.hidden = true;
            }
        });
    }

    async loadInitialData() {
        try {
            this.showLoadingState();
            
            await Promise.all([
                this.loadHealthStatus(),
                this.loadEnrollmentStatistics(),
                this.loadAuthenticationMetrics(),
                this.loadLivenessStatistics('7d'),
                this.loadRecentActivity()
            ]);
            
            this.hideLoadingState();
        } catch (error) {
            console.error('Error loading initial dashboard data:', error);
            this.showError('Failed to load dashboard data');
            this.hideLoadingState();
        }
    }

    async loadHealthStatus() {
        try {
            const response = await fetch(`/admin/realms/${keycloakRealm}/face-recognition/health`, {
                method: 'GET',
                headers: {
                    'Authorization': 'Bearer ' + keycloakToken,
                    'Content-Type': 'application/json'
                }
            });

            if (!response.ok) {
                throw new Error('Failed to load health status');
            }

            const health = await response.json();
            this.updateHealthStatus(health);
            
        } catch (error) {
            console.error('Error loading health status:', error);
            this.updateHealthStatus({
                bioIdService: { status: 'UNKNOWN', responseTime: 0, region: 'Unknown' },
                database: { status: 'UNKNOWN', connections: 0, poolUsage: 0 },
                cache: { status: 'UNKNOWN', hitRate: 0, size: 0 },
                overall: { status: 'UNKNOWN', uptime: 0, lastCheck: new Date() }
            });
        }
    }

    updateHealthStatus(health) {
        // BioID Service
        this.updateHealthComponent('bioid-service', health.bioIdService || {});
        document.getElementById('bioid-response-time').textContent = 
            health.bioIdService?.responseTime ? `${health.bioIdService.responseTime}ms` : 'N/A';
        document.getElementById('bioid-region').textContent = 
            health.bioIdService?.region || 'Unknown';

        // Database
        this.updateHealthComponent('database', health.database || {});
        document.getElementById('database-connections').textContent = 
            health.database?.connections || '0';
        document.getElementById('database-pool-usage').textContent = 
            health.database?.poolUsage ? `${health.database.poolUsage}%` : '0%';

        // Cache
        this.updateHealthComponent('cache', health.cache || {});
        document.getElementById('cache-hit-rate').textContent = 
            health.cache?.hitRate ? `${health.cache.hitRate}%` : '0%';
        document.getElementById('cache-size').textContent = 
            health.cache?.size ? this.formatBytes(health.cache.size) : '0 B';

        // Overall Health
        this.updateHealthComponent('overall-health', health.overall || {});
        document.getElementById('system-uptime').textContent = 
            health.overall?.uptime ? this.formatUptime(health.overall.uptime) : 'Unknown';
        document.getElementById('last-health-check').textContent = 
            health.overall?.lastCheck ? this.formatTimestamp(health.overall.lastCheck) : 'Never';
    }

    updateHealthComponent(componentId, healthData) {
        const status = healthData.status || 'UNKNOWN';
        const icon = document.getElementById(`${componentId}-icon`);
        const statusLabel = document.getElementById(`${componentId}-status`);

        // Remove existing status classes
        icon.classList.remove('healthy', 'degraded', 'unhealthy', 'unknown');
        statusLabel.classList.remove('healthy', 'degraded', 'unhealthy', 'unknown');

        // Add new status class
        const statusClass = status.toLowerCase();
        icon.classList.add(statusClass);
        statusLabel.classList.add(statusClass);

        // Update status text
        statusLabel.textContent = this.formatHealthStatus(status);
    }

    formatHealthStatus(status) {
        switch (status) {
            case 'HEALTHY': return 'Healthy';
            case 'DEGRADED': return 'Degraded';
            case 'UNHEALTHY': return 'Unhealthy';
            default: return 'Unknown';
        }
    }

    async loadEnrollmentStatistics() {
        try {
            const response = await fetch(`/admin/realms/${keycloakRealm}/face-recognition/enrollment-statistics`, {
                method: 'GET',
                headers: {
                    'Authorization': 'Bearer ' + keycloakToken,
                    'Content-Type': 'application/json'
                }
            });

            if (!response.ok) {
                throw new Error('Failed to load enrollment statistics');
            }

            const stats = await response.json();
            this.updateEnrollmentStatistics(stats);
            this.createEnrollmentChart(stats.trendData || []);
            
        } catch (error) {
            console.error('Error loading enrollment statistics:', error);
            this.updateEnrollmentStatistics({
                totalEnrollments: 0,
                successRate: 0,
                enrollmentsToday: 0,
                enrollmentsThisWeek: 0,
                changes: {}
            });
        }
    }

    updateEnrollmentStatistics(stats) {
        this.updateMetric('total-enrollments', stats.totalEnrollments, stats.changes?.total);
        this.updateMetric('enrollment-success-rate', `${stats.successRate}%`, stats.changes?.successRate);
        this.updateMetric('enrollments-today', stats.enrollmentsToday, stats.changes?.today);
        this.updateMetric('enrollments-this-week', stats.enrollmentsThisWeek, stats.changes?.week);
    }

    async loadAuthenticationMetrics() {
        try {
            const response = await fetch(`/admin/realms/${keycloakRealm}/face-recognition/auth-metrics`, {
                method: 'GET',
                headers: {
                    'Authorization': 'Bearer ' + keycloakToken,
                    'Content-Type': 'application/json'
                }
            });

            if (!response.ok) {
                throw new Error('Failed to load authentication metrics');
            }

            const metrics = await response.json();
            this.updateAuthenticationMetrics(metrics);
            this.createAuthenticationChart(metrics.hourlyData || []);
            
        } catch (error) {
            console.error('Error loading authentication metrics:', error);
            this.updateAuthenticationMetrics({
                totalAuthentications: 0,
                successRate: 0,
                avgResponseTime: 0,
                failedAttempts: 0,
                changes: {}
            });
        }
    }

    updateAuthenticationMetrics(metrics) {
        this.updateMetric('total-authentications', metrics.totalAuthentications, metrics.changes?.total);
        this.updateMetric('auth-success-rate', `${metrics.successRate}%`, metrics.changes?.successRate);
        this.updateMetric('avg-response-time', `${metrics.avgResponseTime}ms`, metrics.changes?.responseTime);
        this.updateMetric('failed-attempts', metrics.failedAttempts, metrics.changes?.failed);
    }

    async loadLivenessStatistics(timeframe = '7d') {
        try {
            const response = await fetch(`/admin/realms/${keycloakRealm}/face-recognition/liveness-statistics?timeframe=${timeframe}`, {
                method: 'GET',
                headers: {
                    'Authorization': 'Bearer ' + keycloakToken,
                    'Content-Type': 'application/json'
                }
            });

            if (!response.ok) {
                throw new Error('Failed to load liveness statistics');
            }

            const stats = await response.json();
            this.updateLivenessStatistics(stats);
            this.createLivenessModeChart(stats.modeDistribution || {});
            this.updateRejectionReasons(stats.rejectionReasons || []);
            
        } catch (error) {
            console.error('Error loading liveness statistics:', error);
            this.updateLivenessStatistics({
                totalChecks: 0,
                passRate: 0,
                avgScore: 0,
                avgTime: 0
            });
        }
    }

    updateLivenessStatistics(stats) {
        document.getElementById('liveness-total-checks').textContent = stats.totalChecks || 0;
        document.getElementById('liveness-pass-rate').textContent = `${stats.passRate || 0}%`;
        document.getElementById('liveness-avg-score').textContent = (stats.avgScore || 0).toFixed(2);
        document.getElementById('liveness-avg-time').textContent = `${stats.avgTime || 0}ms`;
    }

    updateRejectionReasons(reasons) {
        const container = document.getElementById('rejection-reasons');
        
        if (reasons.length === 0) {
            container.innerHTML = `
                <div class="pf-c-empty-state pf-m-sm">
                    <div class="pf-c-empty-state__content">
                        <i class="fas fa-check-circle pf-c-empty-state__icon" aria-hidden="true"></i>
                        <h4 class="pf-c-title pf-m-md">No rejections</h4>
                        <div class="pf-c-empty-state__body">All liveness checks passed successfully.</div>
                    </div>
                </div>
            `;
            return;
        }

        const total = reasons.reduce((sum, reason) => sum + reason.count, 0);
        
        container.innerHTML = reasons.map(reason => {
            const percentage = total > 0 ? ((reason.count / total) * 100).toFixed(1) : 0;
            return `
                <div class="rejection-reason-item">
                    <span class="rejection-reason-label">${this.escapeHtml(reason.reason)}</span>
                    <div>
                        <span class="rejection-reason-count">${reason.count}</span>
                        <span class="rejection-reason-percentage">(${percentage}%)</span>
                    </div>
                </div>
            `;
        }).join('');
    }

    async loadRecentActivity() {
        try {
            const response = await fetch(`/admin/realms/${keycloakRealm}/face-recognition/recent-activity?limit=10`, {
                method: 'GET',
                headers: {
                    'Authorization': 'Bearer ' + keycloakToken,
                    'Content-Type': 'application/json'
                }
            });

            if (!response.ok) {
                throw new Error('Failed to load recent activity');
            }

            const activities = await response.json();
            this.updateRecentActivity(activities);
            
        } catch (error) {
            console.error('Error loading recent activity:', error);
            this.updateRecentActivity([]);
        }
    }

    updateRecentActivity(activities) {
        const container = document.getElementById('recent-activity');
        
        if (activities.length === 0) {
            container.innerHTML = `
                <div class="pf-c-empty-state pf-m-sm">
                    <div class="pf-c-empty-state__content">
                        <i class="fas fa-clock pf-c-empty-state__icon" aria-hidden="true"></i>
                        <h4 class="pf-c-title pf-m-md">No recent activity</h4>
                        <div class="pf-c-empty-state__body">Activity will appear here as it occurs.</div>
                    </div>
                </div>
            `;
            return;
        }

        container.innerHTML = activities.map(activity => `
            <div class="activity-item">
                <div class="activity-icon ${activity.type}">
                    <i class="fas ${this.getActivityIcon(activity.type)}" aria-hidden="true"></i>
                </div>
                <div class="activity-content">
                    <div class="activity-title">${this.escapeHtml(activity.title)}</div>
                    <div class="activity-description">${this.escapeHtml(activity.description)}</div>
                    <div class="activity-timestamp">${this.formatTimestamp(activity.timestamp)}</div>
                </div>
            </div>
        `).join('');
    }

    getActivityIcon(type) {
        switch (type) {
            case 'enrollment': return 'fa-user-plus';
            case 'authentication': return 'fa-shield-alt';
            case 'deletion': return 'fa-trash';
            case 'configuration': return 'fa-cog';
            case 'error': return 'fa-exclamation-triangle';
            default: return 'fa-info-circle';
        }
    }

    updateMetric(elementId, value, change) {
        const valueElement = document.getElementById(elementId);
        const changeElement = document.getElementById(`${elementId.replace(/^[^-]+-/, '')}-change`);
        
        if (valueElement) {
            const oldValue = valueElement.textContent;
            valueElement.textContent = value;
            
            if (oldValue !== value.toString()) {
                valueElement.closest('.metric-card').classList.add('metric-updated');
                setTimeout(() => {
                    valueElement.closest('.metric-card').classList.remove('metric-updated');
                }, 500);
            }
        }
        
        if (changeElement && change !== undefined) {
            changeElement.textContent = this.formatChange(change);
            changeElement.className = 'metric-change ' + this.getChangeClass(change);
        }
    }

    formatChange(change) {
        if (change === 0) return '0';
        const sign = change > 0 ? '+' : '';
        return `${sign}${change}`;
    }

    getChangeClass(change) {
        if (change > 0) return 'positive';
        if (change < 0) return 'negative';
        return 'neutral';
    }

    createEnrollmentChart(data) {
        const canvas = document.getElementById('enrollment-chart-canvas');
        const ctx = canvas.getContext('2d');
        
        // Simple chart implementation - in a real application, you'd use Chart.js or similar
        this.drawLineChart(ctx, data, {
            title: 'Enrollments',
            color: '#0066cc',
            width: canvas.width,
            height: canvas.height
        });
    }

    createAuthenticationChart(data) {
        const canvas = document.getElementById('auth-chart-canvas');
        const ctx = canvas.getContext('2d');
        
        this.drawLineChart(ctx, data, {
            title: 'Success Rate',
            color: '#28a745',
            width: canvas.width,
            height: canvas.height
        });
    }

    createLivenessModeChart(data) {
        const canvas = document.getElementById('liveness-mode-chart-canvas');
        const ctx = canvas.getContext('2d');
        
        this.drawPieChart(ctx, data, {
            width: canvas.width,
            height: canvas.height
        });
    }

    drawLineChart(ctx, data, options) {
        ctx.clearRect(0, 0, options.width, options.height);
        
        if (!data || data.length === 0) {
            ctx.fillStyle = '#666';
            ctx.font = '14px Arial';
            ctx.textAlign = 'center';
            ctx.fillText('No data available', options.width / 2, options.height / 2);
            return;
        }

        const padding = 40;
        const chartWidth = options.width - (padding * 2);
        const chartHeight = options.height - (padding * 2);
        
        const maxValue = Math.max(...data.map(d => d.value));
        const minValue = Math.min(...data.map(d => d.value));
        const valueRange = maxValue - minValue || 1;
        
        // Draw axes
        ctx.strokeStyle = '#ddd';
        ctx.lineWidth = 1;
        ctx.beginPath();
        ctx.moveTo(padding, padding);
        ctx.lineTo(padding, options.height - padding);
        ctx.lineTo(options.width - padding, options.height - padding);
        ctx.stroke();
        
        // Draw data line
        ctx.strokeStyle = options.color;
        ctx.lineWidth = 2;
        ctx.beginPath();
        
        data.forEach((point, index) => {
            const x = padding + (index / (data.length - 1)) * chartWidth;
            const y = options.height - padding - ((point.value - minValue) / valueRange) * chartHeight;
            
            if (index === 0) {
                ctx.moveTo(x, y);
            } else {
                ctx.lineTo(x, y);
            }
        });
        
        ctx.stroke();
        
        // Draw data points
        ctx.fillStyle = options.color;
        data.forEach((point, index) => {
            const x = padding + (index / (data.length - 1)) * chartWidth;
            const y = options.height - padding - ((point.value - minValue) / valueRange) * chartHeight;
            
            ctx.beginPath();
            ctx.arc(x, y, 3, 0, 2 * Math.PI);
            ctx.fill();
        });
    }

    drawPieChart(ctx, data, options) {
        ctx.clearRect(0, 0, options.width, options.height);
        
        const entries = Object.entries(data);
        if (entries.length === 0) {
            ctx.fillStyle = '#666';
            ctx.font = '14px Arial';
            ctx.textAlign = 'center';
            ctx.fillText('No data available', options.width / 2, options.height / 2);
            return;
        }

        const centerX = options.width / 2;
        const centerY = options.height / 2;
        const radius = Math.min(centerX, centerY) - 20;
        
        const total = entries.reduce((sum, [, value]) => sum + value, 0);
        const colors = ['#0066cc', '#28a745', '#ffc107', '#dc3545', '#6f42c1'];
        
        let currentAngle = -Math.PI / 2;
        
        entries.forEach(([label, value], index) => {
            const sliceAngle = (value / total) * 2 * Math.PI;
            
            ctx.fillStyle = colors[index % colors.length];
            ctx.beginPath();
            ctx.moveTo(centerX, centerY);
            ctx.arc(centerX, centerY, radius, currentAngle, currentAngle + sliceAngle);
            ctx.closePath();
            ctx.fill();
            
            // Draw label
            const labelAngle = currentAngle + sliceAngle / 2;
            const labelX = centerX + Math.cos(labelAngle) * (radius * 0.7);
            const labelY = centerY + Math.sin(labelAngle) * (radius * 0.7);
            
            ctx.fillStyle = '#fff';
            ctx.font = '12px Arial';
            ctx.textAlign = 'center';
            ctx.fillText(label, labelX, labelY);
            
            currentAngle += sliceAngle;
        });
    }

    setupPeriodicRefresh() {
        this.refreshTimer = setInterval(() => {
            if (this.isVisible) {
                this.loadInitialData();
            }
        }, this.refreshInterval);
    }

    setupVisibilityHandling() {
        document.addEventListener('visibilitychange', () => {
            this.isVisible = !document.hidden;
            
            if (this.isVisible) {
                // Refresh data when page becomes visible
                this.loadInitialData();
            }
        });
    }

    // Navigation methods
    navigateToTemplateManagement() {
        window.location.href = '/admin/master/console/#/realms/' + keycloakRealm + '/face-recognition/templates';
    }

    navigateToLivenessConfig() {
        window.location.href = '/admin/master/console/#/realms/' + keycloakRealm + '/face-recognition/liveness';
    }

    navigateToReports() {
        window.location.href = '/admin/master/console/#/realms/' + keycloakRealm + '/face-recognition/reports';
    }

    navigateToSystemSettings() {
        window.location.href = '/admin/master/console/#/realms/' + keycloakRealm + '/face-recognition/settings';
    }

    navigateToActivityLog() {
        window.location.href = '/admin/master/console/#/realms/' + keycloakRealm + '/face-recognition/activity';
    }

    // Utility methods
    formatBytes(bytes) {
        if (bytes === 0) return '0 B';
        const k = 1024;
        const sizes = ['B', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    }

    formatUptime(seconds) {
        const days = Math.floor(seconds / 86400);
        const hours = Math.floor((seconds % 86400) / 3600);
        const minutes = Math.floor((seconds % 3600) / 60);
        
        if (days > 0) {
            return `${days}d ${hours}h ${minutes}m`;
        } else if (hours > 0) {
            return `${hours}h ${minutes}m`;
        } else {
            return `${minutes}m`;
        }
    }

    formatTimestamp(timestamp) {
        const date = new Date(timestamp);
        const now = new Date();
        const diffMs = now - date;
        const diffMins = Math.floor(diffMs / 60000);
        const diffHours = Math.floor(diffMs / 3600000);
        const diffDays = Math.floor(diffMs / 86400000);
        
        if (diffMins < 1) {
            return 'Just now';
        } else if (diffMins < 60) {
            return `${diffMins} minute${diffMins === 1 ? '' : 's'} ago`;
        } else if (diffHours < 24) {
            return `${diffHours} hour${diffHours === 1 ? '' : 's'} ago`;
        } else if (diffDays < 7) {
            return `${diffDays} day${diffDays === 1 ? '' : 's'} ago`;
        } else {
            return date.toLocaleDateString();
        }
    }

    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    showLoadingState() {
        document.getElementById('admin-dashboard').classList.add('dashboard-loading');
    }

    hideLoadingState() {
        document.getElementById('admin-dashboard').classList.remove('dashboard-loading');
    }

    showError(message) {
        console.error('Dashboard error:', message);
        // You could implement a toast notification system here
    }

    destroy() {
        if (this.refreshTimer) {
            clearInterval(this.refreshTimer);
        }
    }
}

// Initialize the dashboard when the page loads
document.addEventListener('DOMContentLoaded', () => {
    if (document.getElementById('admin-dashboard')) {
        window.adminDashboard = new AdminDashboard();
    }
});

// Cleanup when page unloads
window.addEventListener('beforeunload', () => {
    if (window.adminDashboard) {
        window.adminDashboard.destroy();
    }
});