/**
 * Liveness Detection Configuration JavaScript
 * Handles configuration form, validation, and testing functionality
 */

class LivenessConfigurationPanel {
    constructor() {
        this.config = {
            defaultLivenessMode: 'PASSIVE',
            livenessThreshold: 0.7,
            challengeDirections: ['UP', 'DOWN', 'LEFT', 'RIGHT'],
            maxOverheadMs: 1000,
            fallbackEnabled: true,
            detailedLogging: false
        };
        this.testImages = [];
        this.isTestRunning = false;
        
        this.init();
    }

    init() {
        this.bindEvents();
        this.loadConfiguration();
        this.setupFormValidation();
    }

    bindEvents() {
        // Liveness mode selection
        this.setupLivenessModeSelect();

        // Threshold slider
        const thresholdSlider = document.getElementById('liveness-threshold');
        thresholdSlider.addEventListener('input', (e) => {
            this.updateThresholdDisplay(e.target.value);
            this.config.livenessThreshold = parseFloat(e.target.value);
        });

        // Challenge directions
        document.querySelectorAll('input[name="challengeDirections"]').forEach(checkbox => {
            checkbox.addEventListener('change', () => {
                this.updateChallengeDirections();
            });
        });

        // Advanced settings toggle
        const advancedToggle = document.querySelector('#advanced-settings .pf-c-expandable-section__toggle');
        advancedToggle.addEventListener('click', () => {
            this.toggleAdvancedSettings();
        });

        // Advanced settings inputs
        document.getElementById('max-overhead').addEventListener('change', (e) => {
            this.config.maxOverheadMs = parseInt(e.target.value);
        });

        document.getElementById('fallback-enabled').addEventListener('change', (e) => {
            this.config.fallbackEnabled = e.target.checked;
        });

        document.getElementById('detailed-logging').addEventListener('change', (e) => {
            this.config.detailedLogging = e.target.checked;
        });

        // Form actions
        document.getElementById('liveness-config-form').addEventListener('submit', (e) => {
            e.preventDefault();
            this.saveConfiguration();
        });

        document.getElementById('test-liveness-btn').addEventListener('click', () => {
            this.showTestPanel();
        });

        document.getElementById('reset-config-btn').addEventListener('click', () => {
            this.resetToDefaults();
        });

        // Test panel events
        document.getElementById('upload-test-images-btn').addEventListener('click', () => {
            document.getElementById('test-images-input').click();
        });

        document.getElementById('test-images-input').addEventListener('change', (e) => {
            this.handleTestImageUpload(e.target.files);
        });

        document.getElementById('run-liveness-test-btn').addEventListener('click', () => {
            this.runLivenessTest();
        });

        document.getElementById('clear-test-btn').addEventListener('click', () => {
            this.clearTestImages();
        });

        // Modal events
        document.getElementById('config-validation-close').addEventListener('click', () => {
            this.hideValidationModal();
        });

        document.getElementById('config-validation-cancel').addEventListener('click', () => {
            this.hideValidationModal();
        });

        document.getElementById('config-validation-fix').addEventListener('click', () => {
            this.fixValidationIssues();
        });
    }

    setupLivenessModeSelect() {
        const select = document.getElementById('liveness-mode-select');
        const toggle = select.querySelector('.pf-c-select__toggle');
        const menu = select.querySelector('.pf-c-select__menu');
        const toggleText = toggle.querySelector('.pf-c-select__toggle-text');

        toggle.addEventListener('click', () => {
            const isExpanded = toggle.getAttribute('aria-expanded') === 'true';
            toggle.setAttribute('aria-expanded', !isExpanded);
            menu.hidden = isExpanded;
        });

        menu.addEventListener('click', (e) => {
            if (e.target.closest('.pf-c-select__menu-item')) {
                const menuItem = e.target.closest('.pf-c-select__menu-item');
                const value = menuItem.dataset.value;
                const title = menuItem.querySelector('.liveness-mode-title').textContent;
                
                toggleText.textContent = title;
                toggle.setAttribute('aria-expanded', 'false');
                menu.hidden = true;
                
                this.config.defaultLivenessMode = value;
                this.updateChallengeDirectionsVisibility(value);
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

    updateChallengeDirectionsVisibility(mode) {
        const challengeGroup = document.getElementById('challenge-directions-group');
        if (mode === 'CHALLENGE_RESPONSE') {
            challengeGroup.style.display = 'block';
        } else {
            challengeGroup.style.display = 'none';
        }
    }

    updateThresholdDisplay(value) {
        document.getElementById('threshold-current-value').textContent = parseFloat(value).toFixed(1);
    }

    updateChallengeDirections() {
        const checkboxes = document.querySelectorAll('input[name="challengeDirections"]:checked');
        this.config.challengeDirections = Array.from(checkboxes).map(cb => cb.value);
    }

    toggleAdvancedSettings() {
        const toggle = document.querySelector('#advanced-settings .pf-c-expandable-section__toggle');
        const content = document.querySelector('#advanced-settings .pf-c-expandable-section__content');
        const isExpanded = toggle.getAttribute('aria-expanded') === 'true';
        
        toggle.setAttribute('aria-expanded', !isExpanded);
        content.hidden = isExpanded;
    }

    async loadConfiguration() {
        try {
            this.showLoadingState();
            
            const response = await fetch(`/admin/realms/${keycloakRealm}/face-recognition/liveness-config`, {
                method: 'GET',
                headers: {
                    'Authorization': 'Bearer ' + keycloakToken,
                    'Content-Type': 'application/json'
                }
            });

            if (!response.ok) {
                throw new Error('Failed to load configuration');
            }

            const config = await response.json();
            this.populateForm(config);
            this.hideLoadingState();
            
        } catch (error) {
            console.error('Error loading configuration:', error);
            this.showError('Failed to load configuration: ' + error.message);
            this.hideLoadingState();
        }
    }

    populateForm(config) {
        // Update internal config
        this.config = { ...this.config, ...config };

        // Update liveness mode
        const modeText = this.getLivenessModeDisplayName(config.defaultLivenessMode);
        document.getElementById('liveness-mode-text').textContent = modeText;
        this.updateChallengeDirectionsVisibility(config.defaultLivenessMode);

        // Update threshold
        document.getElementById('liveness-threshold').value = config.livenessThreshold;
        this.updateThresholdDisplay(config.livenessThreshold);

        // Update challenge directions
        document.querySelectorAll('input[name="challengeDirections"]').forEach(checkbox => {
            checkbox.checked = config.challengeDirections.includes(checkbox.value);
        });

        // Update advanced settings
        document.getElementById('max-overhead').value = config.maxOverheadMs;
        document.getElementById('fallback-enabled').checked = config.fallbackEnabled;
        document.getElementById('detailed-logging').checked = config.detailedLogging;
    }

    getLivenessModeDisplayName(mode) {
        switch (mode) {
            case 'PASSIVE': return 'Passive Detection';
            case 'ACTIVE': return 'Active Detection';
            case 'CHALLENGE_RESPONSE': return 'Challenge-Response';
            default: return 'Select mode...';
        }
    }

    setupFormValidation() {
        // Real-time validation
        document.getElementById('liveness-threshold').addEventListener('input', () => {
            this.validateThreshold();
        });

        document.getElementById('max-overhead').addEventListener('input', () => {
            this.validateMaxOverhead();
        });

        document.querySelectorAll('input[name="challengeDirections"]').forEach(checkbox => {
            checkbox.addEventListener('change', () => {
                this.validateChallengeDirections();
            });
        });
    }

    validateThreshold() {
        const threshold = parseFloat(document.getElementById('liveness-threshold').value);
        const group = document.getElementById('liveness-threshold').closest('.pf-c-form__group');
        
        if (threshold < 0 || threshold > 1) {
            this.setFieldError(group, 'Threshold must be between 0.0 and 1.0');
            return false;
        } else {
            this.clearFieldError(group);
            return true;
        }
    }

    validateMaxOverhead() {
        const overhead = parseInt(document.getElementById('max-overhead').value);
        const group = document.getElementById('max-overhead').closest('.pf-c-form__group');
        
        if (overhead < 50 || overhead > 5000) {
            this.setFieldError(group, 'Maximum overhead must be between 50 and 5000 milliseconds');
            return false;
        } else {
            this.clearFieldError(group);
            return true;
        }
    }

    validateChallengeDirections() {
        if (this.config.defaultLivenessMode === 'CHALLENGE_RESPONSE') {
            const checkedDirections = document.querySelectorAll('input[name="challengeDirections"]:checked');
            const group = document.getElementById('challenge-directions-group');
            
            if (checkedDirections.length < 2) {
                this.setFieldError(group, 'At least 2 challenge directions must be selected');
                return false;
            } else {
                this.clearFieldError(group);
                return true;
            }
        }
        return true;
    }

    setFieldError(group, message) {
        group.classList.add('pf-m-error');
        group.classList.remove('pf-m-success');
        
        let helperText = group.querySelector('.pf-c-form__helper-text');
        if (!helperText) {
            helperText = document.createElement('div');
            helperText.className = 'pf-c-form__helper-text';
            group.appendChild(helperText);
        }
        
        helperText.className = 'pf-c-form__helper-text pf-m-error';
        helperText.innerHTML = `
            <div class="pf-c-helper-text">
                <div class="pf-c-helper-text__item pf-m-error">
                    <span class="pf-c-helper-text__item-text">${message}</span>
                </div>
            </div>
        `;
    }

    clearFieldError(group) {
        group.classList.remove('pf-m-error');
        const helperText = group.querySelector('.pf-c-form__helper-text.pf-m-error');
        if (helperText) {
            helperText.classList.remove('pf-m-error');
        }
    }

    async saveConfiguration() {
        if (!this.validateForm()) {
            this.showValidationModal();
            return;
        }

        try {
            this.showLoadingState();
            
            const response = await fetch(`/admin/realms/${keycloakRealm}/face-recognition/liveness-config`, {
                method: 'PUT',
                headers: {
                    'Authorization': 'Bearer ' + keycloakToken,
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(this.config)
            });

            if (!response.ok) {
                throw new Error('Failed to save configuration');
            }

            this.showSuccess('Configuration saved successfully');
            this.hideLoadingState();
            
        } catch (error) {
            console.error('Error saving configuration:', error);
            this.showError('Failed to save configuration: ' + error.message);
            this.hideLoadingState();
        }
    }

    validateForm() {
        const validations = [
            this.validateThreshold(),
            this.validateMaxOverhead(),
            this.validateChallengeDirections()
        ];

        return validations.every(valid => valid);
    }

    showValidationModal() {
        const errors = this.getValidationErrors();
        const errorsList = document.getElementById('config-validation-errors');
        
        errorsList.innerHTML = errors.map(error => `<li>${error}</li>`).join('');
        document.getElementById('config-validation-modal').hidden = false;
        document.body.classList.add('pf-c-backdrop__open');
    }

    hideValidationModal() {
        document.getElementById('config-validation-modal').hidden = true;
        document.body.classList.remove('pf-c-backdrop__open');
    }

    getValidationErrors() {
        const errors = [];
        
        if (!this.validateThreshold()) {
            errors.push('Liveness threshold must be between 0.0 and 1.0');
        }
        
        if (!this.validateMaxOverhead()) {
            errors.push('Maximum overhead must be between 50 and 5000 milliseconds');
        }
        
        if (!this.validateChallengeDirections()) {
            errors.push('At least 2 challenge directions must be selected for Challenge-Response mode');
        }

        return errors;
    }

    fixValidationIssues() {
        // Auto-fix common validation issues
        const threshold = parseFloat(document.getElementById('liveness-threshold').value);
        if (threshold < 0 || threshold > 1) {
            document.getElementById('liveness-threshold').value = 0.7;
            this.updateThresholdDisplay(0.7);
            this.config.livenessThreshold = 0.7;
        }

        const overhead = parseInt(document.getElementById('max-overhead').value);
        if (overhead < 50 || overhead > 5000) {
            document.getElementById('max-overhead').value = 1000;
            this.config.maxOverheadMs = 1000;
        }

        if (this.config.defaultLivenessMode === 'CHALLENGE_RESPONSE') {
            const checkedDirections = document.querySelectorAll('input[name="challengeDirections"]:checked');
            if (checkedDirections.length < 2) {
                document.getElementById('challenge-up').checked = true;
                document.getElementById('challenge-down').checked = true;
                this.updateChallengeDirections();
            }
        }

        this.hideValidationModal();
        this.showSuccess('Configuration issues have been fixed');
    }

    resetToDefaults() {
        const defaultConfig = {
            defaultLivenessMode: 'PASSIVE',
            livenessThreshold: 0.7,
            challengeDirections: ['UP', 'DOWN', 'LEFT', 'RIGHT'],
            maxOverheadMs: 1000,
            fallbackEnabled: true,
            detailedLogging: false
        };

        this.populateForm(defaultConfig);
        this.showSuccess('Configuration reset to defaults');
    }

    showTestPanel() {
        // Scroll to test panel
        document.querySelector('.liveness-test-upload').scrollIntoView({ 
            behavior: 'smooth' 
        });
    }

    handleTestImageUpload(files) {
        this.testImages = [];
        const gallery = document.getElementById('test-images-gallery');
        const preview = document.getElementById('test-images-preview');
        const filename = document.getElementById('test-images-filename');
        const runButton = document.getElementById('run-liveness-test-btn');

        if (files.length === 0) {
            preview.hidden = true;
            filename.value = 'No files selected';
            runButton.disabled = true;
            return;
        }

        filename.value = `${files.length} file(s) selected`;
        gallery.innerHTML = '';

        Array.from(files).forEach((file, index) => {
            if (file.type.startsWith('image/')) {
                const reader = new FileReader();
                reader.onload = (e) => {
                    this.testImages.push({
                        file: file,
                        data: e.target.result,
                        index: index
                    });

                    const imageContainer = document.createElement('div');
                    imageContainer.className = 'test-image-preview';
                    imageContainer.innerHTML = `
                        <img src="${e.target.result}" alt="Test image ${index + 1}">
                        <button class="image-remove" onclick="this.removeTestImage(${index})" aria-label="Remove image">
                            <i class="fas fa-times"></i>
                        </button>
                    `;

                    gallery.appendChild(imageContainer);
                };
                reader.readAsDataURL(file);
            }
        });

        preview.hidden = false;
        runButton.disabled = false;
    }

    removeTestImage(index) {
        this.testImages = this.testImages.filter(img => img.index !== index);
        const gallery = document.getElementById('test-images-gallery');
        const imageContainers = gallery.querySelectorAll('.test-image-preview');
        
        imageContainers.forEach((container, i) => {
            if (i === index) {
                container.remove();
            }
        });

        if (this.testImages.length === 0) {
            document.getElementById('test-images-preview').hidden = true;
            document.getElementById('test-images-filename').value = 'No files selected';
            document.getElementById('run-liveness-test-btn').disabled = true;
        }
    }

    async runLivenessTest() {
        if (this.testImages.length === 0 || this.isTestRunning) return;

        try {
            this.isTestRunning = true;
            this.showTestRunning();

            // Prepare test data
            const testData = {
                images: this.testImages.map(img => img.data.split(',')[1]), // Remove data URL prefix
                mode: this.config.defaultLivenessMode,
                threshold: this.config.livenessThreshold,
                challengeDirections: this.config.challengeDirections
            };

            const response = await fetch(`/admin/realms/${keycloakRealm}/face-recognition/test-liveness`, {
                method: 'POST',
                headers: {
                    'Authorization': 'Bearer ' + keycloakToken,
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(testData)
            });

            if (!response.ok) {
                throw new Error('Liveness test failed');
            }

            const result = await response.json();
            this.displayTestResults(result);
            
        } catch (error) {
            console.error('Error running liveness test:', error);
            this.showTestError('Test failed: ' + error.message);
        } finally {
            this.isTestRunning = false;
            this.hideTestRunning();
        }
    }

    displayTestResults(result) {
        const resultsContainer = document.getElementById('liveness-test-results');
        const alert = document.getElementById('test-result-alert');
        const title = document.getElementById('test-result-title');
        const description = document.getElementById('test-result-description');

        // Update alert based on result
        alert.className = 'pf-c-alert';
        if (result.live) {
            alert.classList.add('pf-m-success');
            alert.querySelector('.pf-c-alert__icon i').className = 'fas fa-check-circle';
            title.textContent = 'Liveness Detected';
            description.textContent = 'The test images passed liveness detection.';
        } else {
            alert.classList.add('pf-m-danger');
            alert.querySelector('.pf-c-alert__icon i').className = 'fas fa-times-circle';
            title.textContent = 'Liveness Not Detected';
            description.textContent = result.rejectionReason || 'The test images failed liveness detection.';
        }

        // Update detailed results
        document.getElementById('test-liveness-score').textContent = 
            result.livenessScore ? result.livenessScore.toFixed(3) : 'N/A';
        document.getElementById('test-detection-mode').textContent = 
            this.getLivenessModeDisplayName(this.config.defaultLivenessMode);
        document.getElementById('test-processing-time').textContent = 
            result.processingTime ? `${result.processingTime}ms` : 'N/A';

        resultsContainer.hidden = false;
    }

    showTestError(message) {
        const resultsContainer = document.getElementById('liveness-test-results');
        const alert = document.getElementById('test-result-alert');
        const title = document.getElementById('test-result-title');
        const description = document.getElementById('test-result-description');

        alert.className = 'pf-c-alert pf-m-danger';
        alert.querySelector('.pf-c-alert__icon i').className = 'fas fa-exclamation-triangle';
        title.textContent = 'Test Error';
        description.textContent = message;

        resultsContainer.hidden = false;
    }

    clearTestImages() {
        this.testImages = [];
        document.getElementById('test-images-preview').hidden = true;
        document.getElementById('test-images-filename').value = 'No files selected';
        document.getElementById('test-images-input').value = '';
        document.getElementById('run-liveness-test-btn').disabled = true;
        document.getElementById('liveness-test-results').hidden = true;
    }

    showTestRunning() {
        const runButton = document.getElementById('run-liveness-test-btn');
        runButton.disabled = true;
        runButton.innerHTML = '<i class="fas fa-spinner fa-spin pf-u-mr-sm"></i>Running Test...';
        
        document.querySelector('.liveness-test-upload').classList.add('liveness-test-running');
    }

    hideTestRunning() {
        const runButton = document.getElementById('run-liveness-test-btn');
        runButton.disabled = false;
        runButton.innerHTML = '<i class="fas fa-play pf-u-mr-sm"></i>Run Test';
        
        document.querySelector('.liveness-test-upload').classList.remove('liveness-test-running');
    }

    showLoadingState() {
        document.getElementById('liveness-config-panel').classList.add('liveness-config-loading');
    }

    hideLoadingState() {
        document.getElementById('liveness-config-panel').classList.remove('liveness-config-loading');
    }

    showSuccess(message) {
        // You could implement a toast notification system here
        console.log('Success:', message);
    }

    showError(message) {
        // You could implement a toast notification system here
        console.error('Error:', message);
        alert(message);
    }
}

// Initialize the configuration panel when the page loads
document.addEventListener('DOMContentLoaded', () => {
    if (document.getElementById('liveness-config-panel')) {
        new LivenessConfigurationPanel();
    }
});