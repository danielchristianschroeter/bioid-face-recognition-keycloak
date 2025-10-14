/**
 * BioID Face Credentials Account Management Component
 * Enhanced self-service face authentication management with template status and GDPR compliance
 */

class FaceCredentialsManager {
    constructor() {
        this.apiBase = '/auth/realms/' + keycloakRealm + '/face-credentials';
        this.credentialData = null;
        this.init();
    }

    async init() {
        await this.loadCredentialData();
        this.renderComponent();
        this.attachEventListeners();
    }

    async loadCredentialData() {
        try {
            const response = await fetch(this.apiBase, {
                method: 'GET',
                headers: {
                    'Authorization': 'Bearer ' + keycloakToken,
                    'Content-Type': 'application/json'
                }
            });

            if (response.ok) {
                this.credentialData = await response.json();
            } else {
                console.error('Failed to load credential data:', response.status);
                this.showError('Failed to load face authentication data');
            }
        } catch (error) {
            console.error('Error loading credential data:', error);
            this.showError('Network error loading face authentication data');
        }
    }

    renderComponent() {
        const container = document.getElementById('face-credentials-container');
        if (!container) return;

        container.innerHTML = this.generateHTML();
    }

    generateHTML() {
        if (!this.credentialData) {
            return '<div class="loading">Loading face authentication data...</div>';
        }

        const { hasCredentials, faceAuthEnabled, templateStatus, lastAuthentication } = this.credentialData;

        return `
            <div class="face-credential-section">
                ${this.renderHeader()}
                ${this.renderStatus()}
                ${hasCredentials ? this.renderEnrolledView() : this.renderNotEnrolledView()}
                ${this.renderSettings()}
            </div>
            ${this.renderModals()}
        `;
    }

    renderHeader() {
        return `
            <div class="face-credential-header">
                <svg class="face-credential-icon" viewBox="0 0 24 24">
                    <path d="M12,2A10,10 0 0,0 2,12A10,10 0 0,0 12,22A10,10 0 0,0 22,12A10,10 0 0,0 12,2M7.07,18.28C7.5,17.38 10.12,16.5 12,16.5C13.88,16.5 16.5,17.38 16.93,18.28C15.57,19.36 13.86,20 12,20C10.14,20 8.43,19.36 7.07,18.28M18.36,16.83C16.93,15.09 13.46,14.5 12,14.5C10.54,14.5 7.07,15.09 5.64,16.83C4.62,15.5 4,13.82 4,12C4,7.59 7.59,4 12,4C16.41,4 20,7.59 20,12C20,13.82 19.38,15.5 18.36,16.83M12,6C10.06,6 8.5,7.56 8.5,9.5C8.5,11.44 10.06,13 12,13C13.94,13 15.5,11.44 15.5,9.5C15.5,7.56 13.94,6 12,6M12,11A1.5,1.5 0 0,1 10.5,9.5A1.5,1.5 0 0,1 12,8A1.5,1.5 0 0,1 13.5,9.5A1.5,1.5 0 0,1 12,11Z"/>
                </svg>
                <h3 class="face-credential-title">Face Authentication</h3>
            </div>
        `;
    }

    renderStatus() {
        const { hasCredentials, faceAuthEnabled } = this.credentialData;
        const statusClass = hasCredentials && faceAuthEnabled ? 'active' : 'inactive';
        const statusText = hasCredentials && faceAuthEnabled ? 'Active' : 'Not Active';

        return `
            <div class="face-credential-status ${statusClass}">
                <span>${statusText}</span>
            </div>
        `;
    }

    renderEnrolledView() {
        const { credentials, templateStatus, lastAuthentication } = this.credentialData;
        const credential = credentials[0]; // Assuming single credential for now

        return `
            <div class="face-credential-info">
                <p>Face authentication is set up and ready to use.</p>
                ${this.renderSecurityLevel()}
                
                <div class="credential-details">
                    <div class="detail-row">
                        <strong>Enrollment Date:</strong> 
                        <span>${this.formatDate(credential.createdDate)}</span>
                    </div>
                    <div class="detail-row">
                        <strong>Last Used:</strong> 
                        <span>${lastAuthentication ? this.formatDate(lastAuthentication) : 'Never'}</span>
                    </div>
                    <div class="detail-row">
                        <strong>Expires:</strong> 
                        <span>${this.formatDate(credential.expiryDate)}</span>
                    </div>
                    ${templateStatus ? this.renderTemplateStatus(templateStatus) : ''}
                </div>
            </div>

            <div class="face-credential-actions">
                <button type="button" class="btn btn-primary" id="re-enroll-btn">
                    Re-enroll Face
                </button>
                <button type="button" class="btn btn-secondary" id="export-data-btn">
                    Download My Data
                </button>
                <button type="button" class="btn btn-danger" id="delete-credentials-btn">
                    Delete Face Data
                </button>
            </div>
        `;
    }

    renderNotEnrolledView() {
        return `
            <div class="face-credential-info">
                <p>Face authentication is not set up. Enroll your face to enable secure biometric login.</p>
                ${this.renderSecurityLevel()}
            </div>

            <div class="face-credential-actions">
                <button type="button" class="btn btn-primary" id="enroll-btn">
                    Enroll Face
                </button>
            </div>
        `;
    }

    renderTemplateStatus(templateStatus) {
        // Use the enhanced template status display component
        const templateStatusDisplay = new TemplateStatusDisplay(templateStatus, this.credentialData.lastAuthentication);
        return templateStatusDisplay.render();
    }

    renderSettings() {
        const { faceAuthEnabled, requireFaceAuth, fallbackEnabled } = this.credentialData;

        return `
            <div class="face-credential-settings">
                <h4>Settings</h4>
                <form id="settings-form">
                    <div class="form-group">
                        <label class="checkbox-label">
                            <input type="checkbox" id="faceAuthEnabled" ${faceAuthEnabled ? 'checked' : ''}>
                            <span class="checkmark"></span>
                            Enable Face Authentication
                        </label>
                        <p class="help-text">Allow face recognition for login</p>
                    </div>
                    
                    <div class="form-group">
                        <label class="checkbox-label">
                            <input type="checkbox" id="requireFaceAuth" ${requireFaceAuth ? 'checked' : ''} ${!faceAuthEnabled ? 'disabled' : ''}>
                            <span class="checkmark"></span>
                            Require Face Authentication
                        </label>
                        <p class="help-text">Always require face recognition (disable password fallback)</p>
                    </div>
                    
                    <div class="form-group">
                        <label class="checkbox-label">
                            <input type="checkbox" id="fallbackEnabled" ${fallbackEnabled ? 'checked' : ''} ${!faceAuthEnabled ? 'disabled' : ''}>
                            <span class="checkmark"></span>
                            Allow Password Fallback
                        </label>
                        <p class="help-text">Allow password login if face recognition fails</p>
                    </div>
                </form>
            </div>
        `;
    }

    renderModals() {
        return `
            <!-- Delete Confirmation Modal -->
            <div id="delete-modal" class="modal" style="display: none;">
                <div class="modal-content">
                    <div class="modal-header">
                        <h3>Delete Face Authentication Data</h3>
                        <span class="close" id="delete-modal-close">&times;</span>
                    </div>
                    <div class="modal-body">
                        <p><strong>⚠️ This action cannot be undone.</strong></p>
                        <p>This will permanently delete your biometric template and disable face authentication. You can re-enroll at any time.</p>
                        <p>For GDPR compliance, all associated biometric data will be completely removed from our systems.</p>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" id="delete-cancel-btn">Cancel</button>
                        <button type="button" class="btn btn-danger" id="delete-confirm-btn">Delete My Data</button>
                    </div>
                </div>
            </div>

            <!-- Success Modal -->
            <div id="success-modal" class="modal" style="display: none;">
                <div class="modal-content">
                    <div class="modal-header">
                        <h3>Success</h3>
                        <span class="close" id="success-modal-close">&times;</span>
                    </div>
                    <div class="modal-body">
                        <p id="success-message"></p>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-primary" id="success-ok-btn">OK</button>
                    </div>
                </div>
            </div>

            <!-- Error Modal -->
            <div id="error-modal" class="modal" style="display: none;">
                <div class="modal-content">
                    <div class="modal-header">
                        <h3>Error</h3>
                        <span class="close" id="error-modal-close">&times;</span>
                    </div>
                    <div class="modal-body">
                        <p id="error-message"></p>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-primary" id="error-ok-btn">OK</button>
                    </div>
                </div>
            </div>
        `;
    }

    attachEventListeners() {
        // Action buttons
        this.attachButtonListener('enroll-btn', () => this.enrollFace());
        this.attachButtonListener('re-enroll-btn', () => this.reEnrollFace());
        this.attachButtonListener('export-data-btn', () => this.exportData());
        this.attachButtonListener('delete-credentials-btn', () => this.showDeleteModal());

        // Settings form
        const settingsInputs = ['faceAuthEnabled', 'requireFaceAuth', 'fallbackEnabled'];
        settingsInputs.forEach(id => {
            const element = document.getElementById(id);
            if (element) {
                element.addEventListener('change', () => this.updateSettings());
            }
        });

        // Modal event listeners
        this.attachModalListeners();
    }

    attachButtonListener(id, handler) {
        const element = document.getElementById(id);
        if (element) {
            element.addEventListener('click', handler);
        }
    }

    attachModalListeners() {
        // Delete modal
        this.attachButtonListener('delete-modal-close', () => this.hideModal('delete-modal'));
        this.attachButtonListener('delete-cancel-btn', () => this.hideModal('delete-modal'));
        this.attachButtonListener('delete-confirm-btn', () => this.deleteCredentials());

        // Success modal
        this.attachButtonListener('success-modal-close', () => this.hideModal('success-modal'));
        this.attachButtonListener('success-ok-btn', () => this.hideModal('success-modal'));

        // Error modal
        this.attachButtonListener('error-modal-close', () => this.hideModal('error-modal'));
        this.attachButtonListener('error-ok-btn', () => this.hideModal('error-modal'));
    }

    async enrollFace() {
        try {
            const response = await fetch(this.apiBase + '/enroll', {
                method: 'POST',
                headers: {
                    'Authorization': 'Bearer ' + keycloakToken,
                    'Content-Type': 'application/json'
                }
            });

            if (response.ok) {
                this.showSuccess('Face enrollment initiated. You will be redirected to complete the enrollment process.');
                setTimeout(() => {
                    window.location.reload();
                }, 2000);
            } else {
                this.showError('Failed to initiate face enrollment');
            }
        } catch (error) {
            console.error('Error enrolling face:', error);
            this.showError('Network error during enrollment');
        }
    }

    async reEnrollFace() {
        try {
            const response = await fetch(this.apiBase + '/re-enroll', {
                method: 'POST',
                headers: {
                    'Authorization': 'Bearer ' + keycloakToken,
                    'Content-Type': 'application/json'
                }
            });

            if (response.ok) {
                this.showSuccess('Face re-enrollment initiated. You will be redirected to complete the process.');
                setTimeout(() => {
                    window.location.reload();
                }, 2000);
            } else {
                this.showError('Failed to initiate face re-enrollment');
            }
        } catch (error) {
            console.error('Error re-enrolling face:', error);
            this.showError('Network error during re-enrollment');
        }
    }

    async exportData() {
        try {
            const response = await fetch(this.apiBase + '/export', {
                method: 'GET',
                headers: {
                    'Authorization': 'Bearer ' + keycloakToken
                }
            });

            if (response.ok) {
                const blob = await response.blob();
                const url = window.URL.createObjectURL(blob);
                const a = document.createElement('a');
                a.href = url;
                a.download = `face-auth-data-${new Date().toISOString().split('T')[0]}.json`;
                document.body.appendChild(a);
                a.click();
                document.body.removeChild(a);
                window.URL.revokeObjectURL(url);
                
                this.showSuccess('Your face authentication data has been downloaded.');
            } else {
                this.showError('Failed to export data');
            }
        } catch (error) {
            console.error('Error exporting data:', error);
            this.showError('Network error during data export');
        }
    }

    showDeleteModal() {
        this.showModal('delete-modal');
    }

    async deleteCredentials() {
        try {
            const response = await fetch(this.apiBase + '/delete', {
                method: 'DELETE',
                headers: {
                    'Authorization': 'Bearer ' + keycloakToken,
                    'Content-Type': 'application/json'
                }
            });

            if (response.ok) {
                this.hideModal('delete-modal');
                this.showSuccess('Your face authentication data has been permanently deleted.');
                setTimeout(() => {
                    window.location.reload();
                }, 2000);
            } else {
                this.showError('Failed to delete face credentials');
            }
        } catch (error) {
            console.error('Error deleting credentials:', error);
            this.showError('Network error during deletion');
        }
    }

    async updateSettings() {
        const faceAuthEnabled = document.getElementById('faceAuthEnabled').checked;
        const requireFaceAuth = document.getElementById('requireFaceAuth').checked;
        const fallbackEnabled = document.getElementById('fallbackEnabled').checked;

        // Update dependent checkboxes
        document.getElementById('requireFaceAuth').disabled = !faceAuthEnabled;
        document.getElementById('fallbackEnabled').disabled = !faceAuthEnabled;

        try {
            const formData = new FormData();
            formData.append('faceAuthEnabled', faceAuthEnabled);
            formData.append('requireFaceAuth', requireFaceAuth);
            formData.append('fallbackEnabled', fallbackEnabled);

            const response = await fetch(this.apiBase + '/settings', {
                method: 'POST',
                headers: {
                    'Authorization': 'Bearer ' + keycloakToken
                },
                body: formData
            });

            if (response.ok) {
                // Update local data
                this.credentialData.faceAuthEnabled = faceAuthEnabled;
                this.credentialData.requireFaceAuth = requireFaceAuth;
                this.credentialData.fallbackEnabled = fallbackEnabled;
                
                // Update status display
                this.renderComponent();
                this.attachEventListeners();
            } else {
                this.showError('Failed to update settings');
            }
        } catch (error) {
            console.error('Error updating settings:', error);
            this.showError('Network error updating settings');
        }
    }

    // Utility methods
    formatDate(dateString) {
        if (!dateString) return 'N/A';
        return new Date(dateString).toLocaleDateString() + ' ' + new Date(dateString).toLocaleTimeString();
    }

    formatHealthStatus(status) {
        const statusMap = {
            'HEALTHY': 'Healthy',
            'NEEDS_UPGRADE': 'Needs Upgrade',
            'LOW_QUALITY': 'Low Quality'
        };
        return statusMap[status] || status;
    }

    getHealthBadgeClass(status) {
        const classMap = {
            'HEALTHY': 'health-good',
            'NEEDS_UPGRADE': 'health-warning',
            'LOW_QUALITY': 'health-poor'
        };
        return classMap[status] || 'health-unknown';
    }

    renderSecurityLevel() {
        const { hasCredentials, faceAuthEnabled } = this.credentialData;
        let level, className;
        
        if (hasCredentials && faceAuthEnabled) {
            level = 'High Security';
            className = 'security-level-high';
        } else if (hasCredentials) {
            level = 'Medium Security';
            className = 'security-level-medium';
        } else {
            level = 'Low Security';
            className = 'security-level-low';
        }

        return `
            <div class="security-level ${className}">
                <div class="security-level-indicator"></div>
                <span>${level}</span>
            </div>
        `;
    }

    showModal(modalId) {
        const modal = document.getElementById(modalId);
        if (modal) {
            modal.style.display = 'block';
        }
    }

    hideModal(modalId) {
        const modal = document.getElementById(modalId);
        if (modal) {
            modal.style.display = 'none';
        }
    }

    showSuccess(message) {
        document.getElementById('success-message').textContent = message;
        this.showModal('success-modal');
    }

    showError(message) {
        document.getElementById('error-message').textContent = message;
        this.showModal('error-modal');
    }
}

// Initialize when DOM is ready
document.addEventListener('DOMContentLoaded', function() {
    if (document.getElementById('face-credentials-container')) {
        new FaceCredentialsManager();
    }
});