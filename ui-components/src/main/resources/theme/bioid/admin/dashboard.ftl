<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "header">
        ${msg("adminDashboardTitle")}
    <#elseif section = "form">
        <div id="admin-dashboard" class="pf-c-page__main-section">
            <!-- Dashboard Header -->
            <div class="pf-c-page__main-breadcrumb">
                <nav class="pf-c-breadcrumb" aria-label="breadcrumb">
                    <ol class="pf-c-breadcrumb__list">
                        <li class="pf-c-breadcrumb__item">
                            <a href="#" class="pf-c-breadcrumb__link">Admin Console</a>
                        </li>
                        <li class="pf-c-breadcrumb__item pf-m-current" aria-current="page">
                            <span class="pf-c-breadcrumb__item-divider">
                                <i class="fas fa-angle-right" aria-hidden="true"></i>
                            </span>
                            Face Authentication Dashboard
                        </li>
                    </ol>
                </nav>
            </div>

            <!-- Page Title -->
            <div class="pf-c-page__main-section pf-m-light">
                <div class="pf-c-content">
                    <h1 class="pf-c-title pf-m-2xl">Face Authentication Dashboard</h1>
                    <p class="pf-c-content">Monitor system health, enrollment statistics, and authentication metrics.</p>
                </div>
            </div>

            <!-- System Health Status -->
            <div class="pf-c-page__main-section">
                <div class="pf-c-card">
                    <div class="pf-c-card__title">
                        <h2 class="pf-c-title pf-m-lg">
                            <i class="fas fa-heartbeat pf-u-mr-sm"></i>
                            System Health
                        </h2>
                        <div class="pf-c-card__actions">
                            <button class="pf-c-button pf-m-plain" type="button" id="refresh-health-btn" aria-label="Refresh health status">
                                <i class="fas fa-sync-alt"></i>
                            </button>
                        </div>
                    </div>
                    <div class="pf-c-card__body">
                        <div class="pf-l-grid pf-m-all-6-col-on-md pf-m-all-3-col-on-lg pf-m-gutter">
                            <!-- BioID Service Status -->
                            <div class="pf-l-grid__item">
                                <div class="health-status-card">
                                    <div class="health-status-header">
                                        <div class="health-status-icon" id="bioid-service-icon">
                                            <i class="fas fa-circle"></i>
                                        </div>
                                        <div class="health-status-info">
                                            <h3 class="pf-c-title pf-m-md">BioID Service</h3>
                                            <span class="health-status-label" id="bioid-service-status">Checking...</span>
                                        </div>
                                    </div>
                                    <div class="health-status-details">
                                        <dl class="pf-c-description-list pf-m-compact">
                                            <div class="pf-c-description-list__group">
                                                <dt class="pf-c-description-list__term">Response Time</dt>
                                                <dd class="pf-c-description-list__description" id="bioid-response-time">-</dd>
                                            </div>
                                            <div class="pf-c-description-list__group">
                                                <dt class="pf-c-description-list__term">Region</dt>
                                                <dd class="pf-c-description-list__description" id="bioid-region">-</dd>
                                            </div>
                                        </dl>
                                    </div>
                                </div>
                            </div>

                            <!-- Database Status -->
                            <div class="pf-l-grid__item">
                                <div class="health-status-card">
                                    <div class="health-status-header">
                                        <div class="health-status-icon" id="database-icon">
                                            <i class="fas fa-circle"></i>
                                        </div>
                                        <div class="health-status-info">
                                            <h3 class="pf-c-title pf-m-md">Database</h3>
                                            <span class="health-status-label" id="database-status">Checking...</span>
                                        </div>
                                    </div>
                                    <div class="health-status-details">
                                        <dl class="pf-c-description-list pf-m-compact">
                                            <div class="pf-c-description-list__group">
                                                <dt class="pf-c-description-list__term">Connections</dt>
                                                <dd class="pf-c-description-list__description" id="database-connections">-</dd>
                                            </div>
                                            <div class="pf-c-description-list__group">
                                                <dt class="pf-c-description-list__term">Pool Usage</dt>
                                                <dd class="pf-c-description-list__description" id="database-pool-usage">-</dd>
                                            </div>
                                        </dl>
                                    </div>
                                </div>
                            </div>

                            <!-- Cache Status -->
                            <div class="pf-l-grid__item">
                                <div class="health-status-card">
                                    <div class="health-status-header">
                                        <div class="health-status-icon" id="cache-icon">
                                            <i class="fas fa-circle"></i>
                                        </div>
                                        <div class="health-status-info">
                                            <h3 class="pf-c-title pf-m-md">Cache</h3>
                                            <span class="health-status-label" id="cache-status">Checking...</span>
                                        </div>
                                    </div>
                                    <div class="health-status-details">
                                        <dl class="pf-c-description-list pf-m-compact">
                                            <div class="pf-c-description-list__group">
                                                <dt class="pf-c-description-list__term">Hit Rate</dt>
                                                <dd class="pf-c-description-list__description" id="cache-hit-rate">-</dd>
                                            </div>
                                            <div class="pf-c-description-list__group">
                                                <dt class="pf-c-description-list__term">Size</dt>
                                                <dd class="pf-c-description-list__description" id="cache-size">-</dd>
                                            </div>
                                        </dl>
                                    </div>
                                </div>
                            </div>

                            <!-- Overall Health -->
                            <div class="pf-l-grid__item">
                                <div class="health-status-card">
                                    <div class="health-status-header">
                                        <div class="health-status-icon" id="overall-health-icon">
                                            <i class="fas fa-circle"></i>
                                        </div>
                                        <div class="health-status-info">
                                            <h3 class="pf-c-title pf-m-md">Overall Health</h3>
                                            <span class="health-status-label" id="overall-health-status">Checking...</span>
                                        </div>
                                    </div>
                                    <div class="health-status-details">
                                        <dl class="pf-c-description-list pf-m-compact">
                                            <div class="pf-c-description-list__group">
                                                <dt class="pf-c-description-list__term">Uptime</dt>
                                                <dd class="pf-c-description-list__description" id="system-uptime">-</dd>
                                            </div>
                                            <div class="pf-c-description-list__group">
                                                <dt class="pf-c-description-list__term">Last Check</dt>
                                                <dd class="pf-c-description-list__description" id="last-health-check">-</dd>
                                            </div>
                                        </dl>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Enrollment Statistics -->
            <div class="pf-c-page__main-section">
                <div class="pf-l-grid pf-m-all-12-col pf-m-all-6-col-on-lg pf-m-gutter">
                    <!-- Enrollment Metrics -->
                    <div class="pf-l-grid__item">
                        <div class="pf-c-card">
                            <div class="pf-c-card__title">
                                <h2 class="pf-c-title pf-m-lg">
                                    <i class="fas fa-user-plus pf-u-mr-sm"></i>
                                    Enrollment Statistics
                                </h2>
                            </div>
                            <div class="pf-c-card__body">
                                <div class="pf-l-grid pf-m-all-6-col pf-m-gutter">
                                    <div class="pf-l-grid__item">
                                        <div class="metric-card">
                                            <div class="metric-value" id="total-enrollments">-</div>
                                            <div class="metric-label">Total Enrollments</div>
                                            <div class="metric-change" id="enrollments-change">-</div>
                                        </div>
                                    </div>
                                    <div class="pf-l-grid__item">
                                        <div class="metric-card">
                                            <div class="metric-value" id="enrollment-success-rate">-</div>
                                            <div class="metric-label">Success Rate</div>
                                            <div class="metric-change" id="success-rate-change">-</div>
                                        </div>
                                    </div>
                                    <div class="pf-l-grid__item">
                                        <div class="metric-card">
                                            <div class="metric-value" id="enrollments-today">-</div>
                                            <div class="metric-label">Today</div>
                                            <div class="metric-change" id="today-change">-</div>
                                        </div>
                                    </div>
                                    <div class="pf-l-grid__item">
                                        <div class="metric-card">
                                            <div class="metric-value" id="enrollments-this-week">-</div>
                                            <div class="metric-label">This Week</div>
                                            <div class="metric-change" id="week-change">-</div>
                                        </div>
                                    </div>
                                </div>
                                
                                <!-- Enrollment Chart -->
                                <div class="chart-container pf-u-mt-lg">
                                    <h4 class="pf-c-title pf-m-md">Enrollment Trend (Last 30 Days)</h4>
                                    <div class="chart-placeholder" id="enrollment-chart">
                                        <canvas id="enrollment-chart-canvas" width="400" height="200"></canvas>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- Authentication Metrics -->
                    <div class="pf-l-grid__item">
                        <div class="pf-c-card">
                            <div class="pf-c-card__title">
                                <h2 class="pf-c-title pf-m-lg">
                                    <i class="fas fa-shield-alt pf-u-mr-sm"></i>
                                    Authentication Metrics
                                </h2>
                            </div>
                            <div class="pf-c-card__body">
                                <div class="pf-l-grid pf-m-all-6-col pf-m-gutter">
                                    <div class="pf-l-grid__item">
                                        <div class="metric-card">
                                            <div class="metric-value" id="total-authentications">-</div>
                                            <div class="metric-label">Total Authentications</div>
                                            <div class="metric-change" id="auth-total-change">-</div>
                                        </div>
                                    </div>
                                    <div class="pf-l-grid__item">
                                        <div class="metric-card">
                                            <div class="metric-value" id="auth-success-rate">-</div>
                                            <div class="metric-label">Success Rate</div>
                                            <div class="metric-change" id="auth-success-change">-</div>
                                        </div>
                                    </div>
                                    <div class="pf-l-grid__item">
                                        <div class="metric-card">
                                            <div class="metric-value" id="avg-response-time">-</div>
                                            <div class="metric-label">Avg Response Time</div>
                                            <div class="metric-change" id="response-time-change">-</div>
                                        </div>
                                    </div>
                                    <div class="pf-l-grid__item">
                                        <div class="metric-card">
                                            <div class="metric-value" id="failed-attempts">-</div>
                                            <div class="metric-label">Failed Attempts</div>
                                            <div class="metric-change" id="failed-attempts-change">-</div>
                                        </div>
                                    </div>
                                </div>

                                <!-- Authentication Chart -->
                                <div class="chart-container pf-u-mt-lg">
                                    <h4 class="pf-c-title pf-m-md">Authentication Success Rate (Last 24 Hours)</h4>
                                    <div class="chart-placeholder" id="auth-chart">
                                        <canvas id="auth-chart-canvas" width="400" height="200"></canvas>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Liveness Detection Statistics -->
            <div class="pf-c-page__main-section">
                <div class="pf-c-card">
                    <div class="pf-c-card__title">
                        <h2 class="pf-c-title pf-m-lg">
                            <i class="fas fa-eye pf-u-mr-sm"></i>
                            Liveness Detection Statistics
                        </h2>
                        <div class="pf-c-card__actions">
                            <div class="pf-c-select" id="liveness-timeframe-select">
                                <button class="pf-c-select__toggle" type="button" aria-haspopup="true" aria-expanded="false">
                                    <div class="pf-c-select__toggle-wrapper">
                                        <span class="pf-c-select__toggle-text">Last 7 days</span>
                                    </div>
                                    <span class="pf-c-select__toggle-arrow">
                                        <i class="fas fa-caret-down" aria-hidden="true"></i>
                                    </span>
                                </button>
                                <ul class="pf-c-select__menu" role="listbox" hidden>
                                    <li role="presentation">
                                        <button class="pf-c-select__menu-item" type="button" role="option" data-value="24h">Last 24 hours</button>
                                    </li>
                                    <li role="presentation">
                                        <button class="pf-c-select__menu-item" type="button" role="option" data-value="7d">Last 7 days</button>
                                    </li>
                                    <li role="presentation">
                                        <button class="pf-c-select__menu-item" type="button" role="option" data-value="30d">Last 30 days</button>
                                    </li>
                                </ul>
                            </div>
                        </div>
                    </div>
                    <div class="pf-c-card__body">
                        <div class="pf-l-grid pf-m-all-12-col pf-m-all-4-col-on-lg pf-m-gutter">
                            <!-- Liveness Metrics -->
                            <div class="pf-l-grid__item">
                                <div class="pf-l-grid pf-m-all-6-col pf-m-gutter">
                                    <div class="pf-l-grid__item">
                                        <div class="metric-card">
                                            <div class="metric-value" id="liveness-total-checks">-</div>
                                            <div class="metric-label">Total Checks</div>
                                        </div>
                                    </div>
                                    <div class="pf-l-grid__item">
                                        <div class="metric-card">
                                            <div class="metric-value" id="liveness-pass-rate">-</div>
                                            <div class="metric-label">Pass Rate</div>
                                        </div>
                                    </div>
                                    <div class="pf-l-grid__item">
                                        <div class="metric-card">
                                            <div class="metric-value" id="liveness-avg-score">-</div>
                                            <div class="metric-label">Avg Score</div>
                                        </div>
                                    </div>
                                    <div class="pf-l-grid__item">
                                        <div class="metric-card">
                                            <div class="metric-value" id="liveness-avg-time">-</div>
                                            <div class="metric-label">Avg Time</div>
                                        </div>
                                    </div>
                                </div>
                            </div>

                            <!-- Liveness Mode Distribution -->
                            <div class="pf-l-grid__item">
                                <h4 class="pf-c-title pf-m-md">Detection Mode Usage</h4>
                                <div class="chart-placeholder" id="liveness-mode-chart">
                                    <canvas id="liveness-mode-chart-canvas" width="300" height="200"></canvas>
                                </div>
                            </div>

                            <!-- Rejection Reasons -->
                            <div class="pf-l-grid__item">
                                <h4 class="pf-c-title pf-m-md">Top Rejection Reasons</h4>
                                <div class="rejection-reasons-list" id="rejection-reasons">
                                    <!-- Rejection reasons will be populated by JavaScript -->
                                    <div class="pf-c-empty-state pf-m-sm">
                                        <div class="pf-c-empty-state__content">
                                            <i class="fas fa-chart-bar pf-c-empty-state__icon" aria-hidden="true"></i>
                                            <h4 class="pf-c-title pf-m-md">Loading data...</h4>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Recent Activity -->
            <div class="pf-c-page__main-section">
                <div class="pf-c-card">
                    <div class="pf-c-card__title">
                        <h2 class="pf-c-title pf-m-lg">
                            <i class="fas fa-history pf-u-mr-sm"></i>
                            Recent Activity
                        </h2>
                        <div class="pf-c-card__actions">
                            <button class="pf-c-button pf-m-link" type="button" id="view-all-activity-btn">
                                View All
                            </button>
                        </div>
                    </div>
                    <div class="pf-c-card__body">
                        <div class="activity-timeline" id="recent-activity">
                            <!-- Activity items will be populated by JavaScript -->
                            <div class="pf-c-empty-state pf-m-sm">
                                <div class="pf-c-empty-state__content">
                                    <i class="fas fa-clock pf-c-empty-state__icon" aria-hidden="true"></i>
                                    <h4 class="pf-c-title pf-m-md">Loading recent activity...</h4>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Quick Actions -->
            <div class="pf-c-page__main-section">
                <div class="pf-c-card">
                    <div class="pf-c-card__title">
                        <h2 class="pf-c-title pf-m-lg">
                            <i class="fas fa-bolt pf-u-mr-sm"></i>
                            Quick Actions
                        </h2>
                    </div>
                    <div class="pf-c-card__body">
                        <div class="pf-l-grid pf-m-all-6-col-on-md pf-m-all-3-col-on-lg pf-m-gutter">
                            <div class="pf-l-grid__item">
                                <button class="pf-c-button pf-m-secondary pf-m-block" type="button" id="manage-templates-btn">
                                    <i class="fas fa-users pf-u-mr-sm"></i>
                                    Manage Templates
                                </button>
                            </div>
                            <div class="pf-l-grid__item">
                                <button class="pf-c-button pf-m-secondary pf-m-block" type="button" id="configure-liveness-btn">
                                    <i class="fas fa-shield-alt pf-u-mr-sm"></i>
                                    Configure Liveness
                                </button>
                            </div>
                            <div class="pf-l-grid__item">
                                <button class="pf-c-button pf-m-secondary pf-m-block" type="button" id="view-reports-btn">
                                    <i class="fas fa-chart-line pf-u-mr-sm"></i>
                                    View Reports
                                </button>
                            </div>
                            <div class="pf-l-grid__item">
                                <button class="pf-c-button pf-m-secondary pf-m-block" type="button" id="system-settings-btn">
                                    <i class="fas fa-cog pf-u-mr-sm"></i>
                                    System Settings
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </#if>
</@layout.registrationLayout>