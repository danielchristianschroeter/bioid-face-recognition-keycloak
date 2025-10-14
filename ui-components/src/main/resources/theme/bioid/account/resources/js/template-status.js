/**
 * BioID Template Status Display Component
 * Provides user-friendly explanations for template status information
 */

class TemplateStatusDisplay {
    constructor(templateStatus, lastAuthentication) {
        this.templateStatus = templateStatus;
        this.lastAuthentication = lastAuthentication;
    }

    render() {
        if (!this.templateStatus) {
            return this.renderNoTemplateStatus();
        }

        return `
            <div class="template-status-display">
                ${this.renderStatusOverview()}
                ${this.renderDetailedInformation()}
                ${this.renderUsageStatistics()}
                ${this.renderHealthExplanation()}
                ${this.renderUpgradeNotification()}
            </div>
        `;
    }

    renderNoTemplateStatus() {
        return `
            <div class="template-status-unavailable">
                <div class="status-icon">ℹ️</div>
                <h4>Template Information Unavailable</h4>
                <p>Template status information is currently not available. This may be due to:</p>
                <ul>
                    <li>Recent enrollment (status may take a few minutes to update)</li>
                    <li>Temporary service connectivity issues</li>
                    <li>System maintenance</li>
                </ul>
                <p>Your face authentication will continue to work normally.</p>
            </div>
        `;
    }

    renderStatusOverview() {
        const { healthStatus, needsUpgrade } = this.templateStatus;
        const statusIcon = this.getStatusIcon(healthStatus);
        const statusColor = this.getStatusColor(healthStatus);

        return `
            <div class="template-status-overview">
                <div class="status-header">
                    <div class="status-icon" style="color: ${statusColor}">${statusIcon}</div>
                    <div class="status-text">
                        <h4>Template Status: ${this.formatHealthStatus(healthStatus)}</h4>
                        <p class="status-description">${this.getStatusDescription(healthStatus)}</p>
                    </div>
                </div>
                ${needsUpgrade ? this.renderUpgradeBanner() : ''}
            </div>
        `;
    }

    renderDetailedInformation() {
        const { encoderVersion, featureVectors } = this.templateStatus;
        const currentVersion = 5; // This should come from configuration

        return `
            <div class="template-details">
                <h5>Technical Details</h5>
                <div class="detail-grid">
                    <div class="detail-item">
                        <div class="detail-label">Encoder Version</div>
                        <div class="detail-value">
                            ${encoderVersion}
                            ${encoderVersion < currentVersion ? 
                                '<span class="version-badge outdated">Outdated</span>' : 
                                '<span class="version-badge current">Current</span>'
                            }
                        </div>
                        <div class="detail-explanation">
                            ${this.getEncoderVersionExplanation(encoderVersion, currentVersion)}
                        </div>
                    </div>
                    
                    <div class="detail-item">
                        <div class="detail-label">Feature Vectors</div>
                        <div class="detail-value">
                            ${featureVectors}
                            <span class="quality-indicator ${this.getQualityClass(featureVectors)}">
                                ${this.getQualityLabel(featureVectors)}
                            </span>
                        </div>
                        <div class="detail-explanation">
                            ${this.getFeatureVectorsExplanation(featureVectors)}
                        </div>
                    </div>
                </div>
            </div>
        `;
    }

    renderUsageStatistics() {
        return `
            <div class="usage-statistics">
                <h5>Usage Information</h5>
                <div class="usage-grid">
                    <div class="usage-item">
                        <div class="usage-label">Last Authentication</div>
                        <div class="usage-value">
                            ${this.lastAuthentication ? 
                                this.formatLastUsed(this.lastAuthentication) : 
                                'Never used'
                            }
                        </div>
                    </div>
                    
                    <div class="usage-item">
                        <div class="usage-label">Authentication Method</div>
                        <div class="usage-value">Face Recognition</div>
                    </div>
                </div>
                
                <div class="usage-explanation">
                    <p><strong>Privacy Note:</strong> We only store the date and time of your last authentication, not detailed usage patterns or location information.</p>
                </div>
            </div>
        `;
    }

    renderHealthExplanation() {
        const { healthStatus } = this.templateStatus;
        const explanation = this.getHealthExplanation(healthStatus);
        const recommendations = this.getHealthRecommendations(healthStatus);

        return `
            <div class="health-explanation">
                <h5>What This Means</h5>
                <div class="explanation-content">
                    <p>${explanation}</p>
                    ${recommendations.length > 0 ? `
                        <div class="recommendations">
                            <strong>Recommendations:</strong>
                            <ul>
                                ${recommendations.map(rec => `<li>${rec}</li>`).join('')}
                            </ul>
                        </div>
                    ` : ''}
                </div>
            </div>
        `;
    }

    renderUpgradeNotification() {
        if (!this.templateStatus.needsUpgrade) {
            return '';
        }

        return `
            <div class="upgrade-notification-detailed">
                <div class="upgrade-header">
                    <div class="upgrade-icon">🔄</div>
                    <h5>Template Upgrade Available</h5>
                </div>
                <div class="upgrade-content">
                    <p>A newer version of the face recognition technology is available. Upgrading your template will:</p>
                    <ul>
                        <li>Improve authentication accuracy</li>
                        <li>Enhance security features</li>
                        <li>Provide better performance</li>
                        <li>Ensure compatibility with future updates</li>
                    </ul>
                    <p><strong>Note:</strong> The upgrade process is automatic and will not require you to re-enroll your face.</p>
                </div>
                <div class="upgrade-actions">
                    <button type="button" class="btn btn-primary btn-sm" onclick="requestTemplateUpgrade()">
                        Request Upgrade
                    </button>
                    <button type="button" class="btn btn-link btn-sm" onclick="showUpgradeInfo()">
                        Learn More
                    </button>
                </div>
            </div>
        `;
    }

    renderUpgradeBanner() {
        return `
            <div class="upgrade-banner">
                <span class="upgrade-icon">⚠️</span>
                <span>Template upgrade recommended for improved security and performance</span>
            </div>
        `;
    }

    // Utility methods for status interpretation
    getStatusIcon(healthStatus) {
        const icons = {
            'HEALTHY': '✅',
            'NEEDS_UPGRADE': '⚠️',
            'LOW_QUALITY': '⚠️',
            'UNKNOWN': 'ℹ️'
        };
        return icons[healthStatus] || icons['UNKNOWN'];
    }

    getStatusColor(healthStatus) {
        const colors = {
            'HEALTHY': '#28a745',
            'NEEDS_UPGRADE': '#ffc107',
            'LOW_QUALITY': '#dc3545',
            'UNKNOWN': '#6c757d'
        };
        return colors[healthStatus] || colors['UNKNOWN'];
    }

    formatHealthStatus(healthStatus) {
        const labels = {
            'HEALTHY': 'Excellent',
            'NEEDS_UPGRADE': 'Good (Upgrade Available)',
            'LOW_QUALITY': 'Needs Attention',
            'UNKNOWN': 'Unknown'
        };
        return labels[healthStatus] || healthStatus;
    }

    getStatusDescription(healthStatus) {
        const descriptions = {
            'HEALTHY': 'Your face template is in excellent condition and using the latest technology.',
            'NEEDS_UPGRADE': 'Your face template is working well but can be improved with an upgrade.',
            'LOW_QUALITY': 'Your face template may need attention to ensure reliable authentication.',
            'UNKNOWN': 'Template status information is not available at this time.'
        };
        return descriptions[healthStatus] || 'Status information unavailable.';
    }

    getEncoderVersionExplanation(currentVersion, latestVersion) {
        if (currentVersion >= latestVersion) {
            return 'You are using the latest face recognition technology version.';
        } else {
            const versionsBehind = latestVersion - currentVersion;
            return `Your template uses an older version (${versionsBehind} version${versionsBehind > 1 ? 's' : ''} behind). Consider upgrading for improved performance.`;
        }
    }

    getFeatureVectorsExplanation(featureVectors) {
        if (featureVectors >= 20) {
            return 'Excellent quality template with high accuracy and reliability.';
        } else if (featureVectors >= 15) {
            return 'Good quality template that should work reliably in most conditions.';
        } else if (featureVectors >= 10) {
            return 'Adequate quality template. May have reduced accuracy in challenging lighting conditions.';
        } else {
            return 'Low quality template. Consider re-enrolling for better authentication reliability.';
        }
    }

    getQualityClass(featureVectors) {
        if (featureVectors >= 20) return 'quality-excellent';
        if (featureVectors >= 15) return 'quality-good';
        if (featureVectors >= 10) return 'quality-fair';
        return 'quality-poor';
    }

    getQualityLabel(featureVectors) {
        if (featureVectors >= 20) return 'Excellent';
        if (featureVectors >= 15) return 'Good';
        if (featureVectors >= 10) return 'Fair';
        return 'Poor';
    }

    getHealthExplanation(healthStatus) {
        const explanations = {
            'HEALTHY': 'Your face template is working optimally. The biometric data is high quality and uses the latest recognition technology, ensuring reliable and secure authentication.',
            'NEEDS_UPGRADE': 'Your face template is functioning well but uses an older version of the recognition technology. An upgrade would improve accuracy and security without requiring re-enrollment.',
            'LOW_QUALITY': 'Your face template has lower quality biometric data, which may result in less reliable authentication. This could be due to poor lighting conditions during enrollment or camera quality.',
            'UNKNOWN': 'We cannot determine the current status of your face template. This is usually temporary and does not affect your ability to authenticate.'
        };
        return explanations[healthStatus] || 'Status information is not available.';
    }

    getHealthRecommendations(healthStatus) {
        const recommendations = {
            'HEALTHY': [],
            'NEEDS_UPGRADE': [
                'Contact your administrator to request a template upgrade',
                'The upgrade process is automatic and maintains your current enrollment'
            ],
            'LOW_QUALITY': [
                'Consider re-enrolling your face in good lighting conditions',
                'Ensure your camera is clean and positioned properly during enrollment',
                'Contact support if authentication frequently fails'
            ],
            'UNKNOWN': [
                'Try refreshing the page to reload status information',
                'Contact support if the status remains unknown'
            ]
        };
        return recommendations[healthStatus] || [];
    }

    formatLastUsed(lastAuthentication) {
        const date = new Date(lastAuthentication);
        const now = new Date();
        const diffMs = now - date;
        const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));
        const diffHours = Math.floor(diffMs / (1000 * 60 * 60));
        const diffMinutes = Math.floor(diffMs / (1000 * 60));

        if (diffDays > 7) {
            return date.toLocaleDateString();
        } else if (diffDays > 0) {
            return `${diffDays} day${diffDays > 1 ? 's' : ''} ago`;
        } else if (diffHours > 0) {
            return `${diffHours} hour${diffHours > 1 ? 's' : ''} ago`;
        } else if (diffMinutes > 0) {
            return `${diffMinutes} minute${diffMinutes > 1 ? 's' : ''} ago`;
        } else {
            return 'Just now';
        }
    }
}

// Global functions for upgrade actions
function requestTemplateUpgrade() {
    // This would typically call an API endpoint
    alert('Template upgrade request submitted. You will be notified when the upgrade is complete.');
}

function showUpgradeInfo() {
    const modal = document.createElement('div');
    modal.className = 'modal';
    modal.style.display = 'block';
    modal.innerHTML = `
        <div class="modal-content">
            <div class="modal-header">
                <h3>Template Upgrade Information</h3>
                <span class="close" onclick="this.closest('.modal').remove()">&times;</span>
            </div>
            <div class="modal-body">
                <h4>What is a template upgrade?</h4>
                <p>A template upgrade improves your face recognition template using newer algorithms and technology, without requiring you to re-enroll.</p>
                
                <h4>Benefits of upgrading:</h4>
                <ul>
                    <li><strong>Better Accuracy:</strong> Improved recognition in various lighting conditions</li>
                    <li><strong>Enhanced Security:</strong> Stronger protection against spoofing attempts</li>
                    <li><strong>Future Compatibility:</strong> Ensures your template works with future updates</li>
                    <li><strong>Performance:</strong> Faster authentication processing</li>
                </ul>
                
                <h4>Is it safe?</h4>
                <p>Yes, template upgrades are completely safe. Your original biometric data is not modified, and you can continue using face authentication throughout the process.</p>
                
                <h4>How long does it take?</h4>
                <p>Template upgrades are typically completed within a few minutes and happen automatically in the background.</p>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-primary" onclick="this.closest('.modal').remove()">Got it</button>
            </div>
        </div>
    `;
    document.body.appendChild(modal);
}

// Export for use in main component
window.TemplateStatusDisplay = TemplateStatusDisplay;