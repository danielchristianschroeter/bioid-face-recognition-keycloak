/**
 * Template Management Dashboard JavaScript
 * Handles template listing, filtering, bulk operations, and modal interactions
 */

class TemplateManagementDashboard {
    constructor() {
        this.templates = [];
        this.filteredTemplates = [];
        this.selectedTemplates = new Set();
        this.currentPage = 1;
        this.itemsPerPage = 20;
        this.sortField = 'username';
        this.sortDirection = 'asc';
        this.filters = {
            search: '',
            searchType: 'username',
            healthStatus: 'all',
            encoderVersion: 'all'
        };
        this.bulkOperation = null;
        
        this.init();
    }

    init() {
        this.bindEvents();
        this.loadTemplates();
        this.loadStatistics();
        this.setupPeriodicRefresh();
    }

    bindEvents() {
        // Search and filters
        document.getElementById('template-search-input').addEventListener('input', (e) => {
            this.filters.search = e.target.value;
            this.applyFilters();
        });

        // Bulk actions
        document.getElementById('bulk-upgrade-btn').addEventListener('click', () => {
            this.performBulkUpgrade();
        });

        document.getElementById('bulk-delete-btn').addEventListener('click', () => {
            this.performBulkDelete();
        });

        document.getElementById('bulk-tag-btn').addEventListener('click', () => {
            this.showBulkTagModal();
        });

        // Refresh button
        document.getElementById('refresh-templates-btn').addEventListener('click', () => {
            this.loadTemplates();
            this.loadStatistics();
        });

        // Select all checkbox
        document.getElementById('select-all-templates').addEventListener('change', (e) => {
            this.toggleSelectAll(e.target.checked);
        });

        // Modal events
        document.getElementById('template-details-modal-close').addEventListener('click', () => {
            this.hideTemplateDetailsModal();
        });

        document.getElementById('modal-cancel-btn').addEventListener('click', () => {
            this.hideTemplateDetailsModal();
        });

        document.getElementById('modal-upgrade-btn').addEventListener('click', () => {
            this.upgradeTemplateFromModal();
        });

        document.getElementById('modal-delete-btn').addEventListener('click', () => {
            this.deleteTemplateFromModal();
        });

        document.getElementById('modal-download-btn').addEventListener('click', () => {
            this.downloadTemplateData();
        });

        // Bulk operation modal events
        document.getElementById('bulk-operation-cancel-btn').addEventListener('click', () => {
            this.cancelBulkOperation();
        });

        document.getElementById('bulk-operation-close-btn').addEventListener('click', () => {
            this.hideBulkOperationModal();
        });

        // Pagination events
        document.getElementById('pagination-first').addEventListener('click', () => {
            this.goToPage(1);
        });

        document.getElementById('pagination-prev').addEventListener('click', () => {
            this.goToPage(Math.max(1, this.currentPage - 1));
        });

        document.getElementById('pagination-next').addEventListener('click', () => {
            this.goToPage(this.currentPage + 1);
        });

        document.getElementById('pagination-last').addEventListener('click', () => {
            const totalPages = Math.ceil(this.filteredTemplates.length / this.itemsPerPage);
            this.goToPage(totalPages);
        });

        // Dropdown toggles
        this.setupDropdownToggles();
    }

    setupDropdownToggles() {
        // Bulk actions dropdown
        const bulkActionsToggle = document.getElementById('bulk-actions-dropdown');
        const bulkActionsMenu = bulkActionsToggle.nextElementSibling;
        
        bulkActionsToggle.addEventListener('click', () => {
            const isExpanded = bulkActionsToggle.getAttribute('aria-expanded') === 'true';
            bulkActionsToggle.setAttribute('aria-expanded', !isExpanded);
            bulkActionsMenu.hidden = isExpanded;
        });

        // Filter dropdowns
        this.setupFilterDropdown('search-type-select', (value) => {
            this.filters.searchType = value;
            this.applyFilters();
        });

        this.setupFilterDropdown('health-status-filter', (value) => {
            this.filters.healthStatus = value;
            this.applyFilters();
        });

        this.setupFilterDropdown('encoder-version-filter', (value) => {
            this.filters.encoderVersion = value;
            this.applyFilters();
        });
    }

    setupFilterDropdown(selectId, onSelect) {
        const select = document.getElementById(selectId);
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
                const value = e.target.textContent.toLowerCase();
                toggleText.textContent = e.target.textContent;
                toggle.setAttribute('aria-expanded', 'false');
                menu.hidden = true;
                onSelect(value);
            }
        });
    }

    async loadTemplates() {
        try {
            this.showLoadingState();
            
            const response = await fetch('/admin/realms/' + keycloakRealm + '/face-recognition/templates', {
                method: 'GET',
                headers: {
                    'Authorization': 'Bearer ' + keycloakToken,
                    'Content-Type': 'application/json'
                }
            });

            if (!response.ok) {
                throw new Error('Failed to load templates');
            }

            const data = await response.json();
            this.templates = data.templates || [];
            this.applyFilters();
            this.hideLoadingState();
            
        } catch (error) {
            console.error('Error loading templates:', error);
            this.showError('Failed to load templates: ' + error.message);
            this.hideLoadingState();
        }
    }

    async loadStatistics() {
        try {
            const response = await fetch('/admin/realms/' + keycloakRealm + '/face-recognition/template-statistics', {
                method: 'GET',
                headers: {
                    'Authorization': 'Bearer ' + keycloakToken,
                    'Content-Type': 'application/json'
                }
            });

            if (!response.ok) {
                throw new Error('Failed to load statistics');
            }

            const stats = await response.json();
            this.updateStatisticsCards(stats);
            
        } catch (error) {
            console.error('Error loading statistics:', error);
        }
    }

    updateStatisticsCards(stats) {
        document.getElementById('total-templates-count').textContent = stats.totalTemplates || 0;
        document.getElementById('healthy-templates-count').textContent = stats.healthyTemplates || 0;
        document.getElementById('upgrade-needed-count').textContent = stats.needsUpgrade || 0;
        document.getElementById('expiring-templates-count').textContent = stats.expiringSoon || 0;
    }

    applyFilters() {
        this.filteredTemplates = this.templates.filter(template => {
            // Search filter
            if (this.filters.search) {
                const searchValue = this.filters.search.toLowerCase();
                const searchField = this.getSearchFieldValue(template, this.filters.searchType);
                if (!searchField.toLowerCase().includes(searchValue)) {
                    return false;
                }
            }

            // Health status filter
            if (this.filters.healthStatus !== 'all') {
                if (template.healthStatus !== this.filters.healthStatus) {
                    return false;
                }
            }

            // Encoder version filter
            if (this.filters.encoderVersion !== 'all') {
                if (template.encoderVersion.toString() !== this.filters.encoderVersion) {
                    return false;
                }
            }

            return true;
        });

        this.sortTemplates();
        this.currentPage = 1;
        this.renderTable();
        this.updatePagination();
    }

    getSearchFieldValue(template, searchType) {
        switch (searchType) {
            case 'username':
                return template.username || '';
            case 'email':
                return template.email || '';
            case 'class id':
                return template.classId.toString();
            default:
                return template.username || '';
        }
    }

    sortTemplates() {
        this.filteredTemplates.sort((a, b) => {
            let aValue = a[this.sortField];
            let bValue = b[this.sortField];

            if (typeof aValue === 'string') {
                aValue = aValue.toLowerCase();
                bValue = bValue.toLowerCase();
            }

            if (this.sortDirection === 'asc') {
                return aValue < bValue ? -1 : aValue > bValue ? 1 : 0;
            } else {
                return aValue > bValue ? -1 : aValue < bValue ? 1 : 0;
            }
        });
    }

    renderTable() {
        const tbody = document.getElementById('templates-table-body');
        const startIndex = (this.currentPage - 1) * this.itemsPerPage;
        const endIndex = startIndex + this.itemsPerPage;
        const pageTemplates = this.filteredTemplates.slice(startIndex, endIndex);

        if (pageTemplates.length === 0) {
            tbody.innerHTML = `
                <tr role="row">
                    <td colspan="9" class="pf-c-table__cell pf-u-text-align-center">
                        <div class="pf-c-empty-state pf-m-sm template-empty-state">
                            <div class="pf-c-empty-state__content">
                                <i class="fas fa-search pf-c-empty-state__icon" aria-hidden="true"></i>
                                <h2 class="pf-c-title pf-m-lg">No templates found</h2>
                                <div class="pf-c-empty-state__body">
                                    ${this.filters.search ? 'Try adjusting your search criteria.' : 'No templates are currently enrolled.'}
                                </div>
                            </div>
                        </div>
                    </td>
                </tr>
            `;
            return;
        }

        tbody.innerHTML = pageTemplates.map(template => this.renderTemplateRow(template)).join('');
        this.bindTableEvents();
    }

    renderTemplateRow(template) {
        const isSelected = this.selectedTemplates.has(template.classId);
        const healthStatusClass = this.getHealthStatusClass(template.healthStatus);
        const encoderVersionClass = template.needsUpgrade ? 'outdated' : '';

        return `
            <tr role="row" class="${isSelected ? 'selected' : ''}" data-class-id="${template.classId}">
                <td role="gridcell" class="pf-c-table__check">
                    <input class="pf-c-check__input template-checkbox" type="checkbox" 
                           ${isSelected ? 'checked' : ''} 
                           data-class-id="${template.classId}"
                           aria-label="Select template for ${template.username}">
                </td>
                <td role="gridcell" class="pf-c-table__cell">
                    <div class="template-user-cell">
                        <span class="template-user-name">${this.escapeHtml(template.username)}</span>
                        <span class="template-user-email">${this.escapeHtml(template.email || '')}</span>
                    </div>
                </td>
                <td role="gridcell" class="pf-c-table__cell">
                    <span class="template-class-id">${template.classId}</span>
                </td>
                <td role="gridcell" class="pf-c-table__cell">
                    <span class="template-status-badge ${healthStatusClass}">
                        <i class="fas ${this.getHealthStatusIcon(template.healthStatus)}" aria-hidden="true"></i>
                        ${this.formatHealthStatus(template.healthStatus)}
                    </span>
                </td>
                <td role="gridcell" class="pf-c-table__cell">
                    <span class="template-encoder-version ${encoderVersionClass}">
                        ${template.encoderVersion}
                        ${template.needsUpgrade ? '<i class="fas fa-exclamation-triangle" aria-hidden="true"></i>' : ''}
                    </span>
                </td>
                <td role="gridcell" class="pf-c-table__cell template-feature-vectors">
                    ${template.featureVectors}
                </td>
                <td role="gridcell" class="pf-c-table__cell">
                    <span class="template-timestamp">${this.formatDate(template.enrolledAt)}</span>
                </td>
                <td role="gridcell" class="pf-c-table__cell">
                    <span class="template-timestamp">${this.formatDate(template.lastAuthentication)}</span>
                </td>
                <td role="gridcell" class="pf-c-table__cell">
                    <div class="template-actions">
                        <button class="pf-c-button pf-m-small pf-m-secondary template-details-btn" 
                                type="button" data-class-id="${template.classId}">
                            <i class="fas fa-eye" aria-hidden="true"></i>
                        </button>
                        ${template.needsUpgrade ? `
                            <button class="pf-c-button pf-m-small pf-m-primary template-upgrade-btn" 
                                    type="button" data-class-id="${template.classId}">
                                <i class="fas fa-arrow-up" aria-hidden="true"></i>
                            </button>
                        ` : ''}
                        <button class="pf-c-button pf-m-small pf-m-danger template-delete-btn" 
                                type="button" data-class-id="${template.classId}">
                            <i class="fas fa-trash" aria-hidden="true"></i>
                        </button>
                    </div>
                </td>
            </tr>
        `;
    }

    bindTableEvents() {
        // Template checkboxes
        document.querySelectorAll('.template-checkbox').forEach(checkbox => {
            checkbox.addEventListener('change', (e) => {
                const classId = parseInt(e.target.dataset.classId);
                if (e.target.checked) {
                    this.selectedTemplates.add(classId);
                } else {
                    this.selectedTemplates.delete(classId);
                }
                this.updateSelectAllCheckbox();
                this.updateBulkActionsState();
            });
        });

        // Template action buttons
        document.querySelectorAll('.template-details-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const classId = parseInt(e.target.closest('button').dataset.classId);
                this.showTemplateDetails(classId);
            });
        });

        document.querySelectorAll('.template-upgrade-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const classId = parseInt(e.target.closest('button').dataset.classId);
                this.upgradeTemplate(classId);
            });
        });

        document.querySelectorAll('.template-delete-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const classId = parseInt(e.target.closest('button').dataset.classId);
                this.deleteTemplate(classId);
            });
        });
    }

    toggleSelectAll(checked) {
        const startIndex = (this.currentPage - 1) * this.itemsPerPage;
        const endIndex = startIndex + this.itemsPerPage;
        const pageTemplates = this.filteredTemplates.slice(startIndex, endIndex);

        pageTemplates.forEach(template => {
            if (checked) {
                this.selectedTemplates.add(template.classId);
            } else {
                this.selectedTemplates.delete(template.classId);
            }
        });

        this.renderTable();
        this.updateBulkActionsState();
    }

    updateSelectAllCheckbox() {
        const selectAllCheckbox = document.getElementById('select-all-templates');
        const startIndex = (this.currentPage - 1) * this.itemsPerPage;
        const endIndex = startIndex + this.itemsPerPage;
        const pageTemplates = this.filteredTemplates.slice(startIndex, endIndex);
        
        const selectedOnPage = pageTemplates.filter(template => 
            this.selectedTemplates.has(template.classId)
        ).length;

        if (selectedOnPage === 0) {
            selectAllCheckbox.checked = false;
            selectAllCheckbox.indeterminate = false;
        } else if (selectedOnPage === pageTemplates.length) {
            selectAllCheckbox.checked = true;
            selectAllCheckbox.indeterminate = false;
        } else {
            selectAllCheckbox.checked = false;
            selectAllCheckbox.indeterminate = true;
        }
    }

    updateBulkActionsState() {
        const hasSelection = this.selectedTemplates.size > 0;
        const bulkActionsToggle = document.getElementById('bulk-actions-dropdown');
        bulkActionsToggle.disabled = !hasSelection;
        
        if (!hasSelection) {
            bulkActionsToggle.setAttribute('aria-expanded', 'false');
            bulkActionsToggle.nextElementSibling.hidden = true;
        }
    }

    async showTemplateDetails(classId) {
        try {
            const template = this.templates.find(t => t.classId === classId);
            if (!template) return;

            // Load detailed template information
            const response = await fetch(`/admin/realms/${keycloakRealm}/face-recognition/templates/${classId}`, {
                method: 'GET',
                headers: {
                    'Authorization': 'Bearer ' + keycloakToken,
                    'Content-Type': 'application/json'
                }
            });

            if (!response.ok) {
                throw new Error('Failed to load template details');
            }

            const details = await response.json();
            this.populateTemplateDetailsModal(template, details);
            this.showTemplateDetailsModal();
            
        } catch (error) {
            console.error('Error loading template details:', error);
            this.showError('Failed to load template details: ' + error.message);
        }
    }

    populateTemplateDetailsModal(template, details) {
        document.getElementById('modal-username').textContent = template.username;
        document.getElementById('modal-email').textContent = template.email || '-';
        document.getElementById('modal-class-id').textContent = template.classId;
        document.getElementById('modal-health-status').innerHTML = `
            <span class="template-status-badge ${this.getHealthStatusClass(template.healthStatus)}">
                <i class="fas ${this.getHealthStatusIcon(template.healthStatus)}" aria-hidden="true"></i>
                ${this.formatHealthStatus(template.healthStatus)}
            </span>
        `;
        document.getElementById('modal-encoder-version').textContent = template.encoderVersion;
        document.getElementById('modal-feature-vectors').textContent = template.featureVectors;
        document.getElementById('modal-enrolled-at').textContent = this.formatDate(template.enrolledAt);
        document.getElementById('modal-last-auth').textContent = this.formatDate(template.lastAuthentication);
        document.getElementById('modal-expires-at').textContent = this.formatDate(template.expiresAt);

        // Populate thumbnails
        const thumbnailsContainer = document.getElementById('modal-thumbnails');
        if (details.thumbnails && details.thumbnails.length > 0) {
            thumbnailsContainer.innerHTML = details.thumbnails.map((thumbnail, index) => `
                <img src="data:image/jpeg;base64,${thumbnail}" 
                     alt="Template thumbnail ${index + 1}" 
                     class="template-thumbnail"
                     onclick="this.requestFullscreen()">
            `).join('');
        } else {
            thumbnailsContainer.innerHTML = `
                <div class="pf-c-empty-state pf-m-sm">
                    <div class="pf-c-empty-state__content">
                        <i class="fas fa-image pf-c-empty-state__icon" aria-hidden="true"></i>
                        <h4 class="pf-c-title pf-m-md">No thumbnails available</h4>
                        <div class="pf-c-empty-state__body">
                            Thumbnails are not stored for this template.
                        </div>
                    </div>
                </div>
            `;
        }

        // Store current template ID for modal actions
        this.currentModalTemplateId = template.classId;
    }

    showTemplateDetailsModal() {
        document.getElementById('template-details-modal').hidden = false;
        document.body.classList.add('pf-c-backdrop__open');
    }

    hideTemplateDetailsModal() {
        document.getElementById('template-details-modal').hidden = true;
        document.body.classList.remove('pf-c-backdrop__open');
        this.currentModalTemplateId = null;
    }

    async performBulkUpgrade() {
        if (this.selectedTemplates.size === 0) return;

        const confirmed = await this.showConfirmDialog(
            'Bulk Template Upgrade',
            `Are you sure you want to upgrade ${this.selectedTemplates.size} selected templates? This operation cannot be undone.`
        );

        if (!confirmed) return;

        this.startBulkOperation('upgrade', Array.from(this.selectedTemplates));
    }

    async performBulkDelete() {
        if (this.selectedTemplates.size === 0) return;

        const confirmed = await this.showConfirmDialog(
            'Bulk Template Deletion',
            `Are you sure you want to delete ${this.selectedTemplates.size} selected templates? This operation cannot be undone and will permanently remove the biometric data.`
        );

        if (!confirmed) return;

        this.startBulkOperation('delete', Array.from(this.selectedTemplates));
    }

    async startBulkOperation(operation, classIds) {
        try {
            const response = await fetch(`/admin/realms/${keycloakRealm}/face-recognition/bulk-${operation}`, {
                method: 'POST',
                headers: {
                    'Authorization': 'Bearer ' + keycloakToken,
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ classIds })
            });

            if (!response.ok) {
                throw new Error(`Failed to start bulk ${operation}`);
            }

            const result = await response.json();
            this.bulkOperation = result;
            this.showBulkOperationModal();
            this.monitorBulkOperation();
            
        } catch (error) {
            console.error(`Error starting bulk ${operation}:`, error);
            this.showError(`Failed to start bulk ${operation}: ` + error.message);
        }
    }

    showBulkOperationModal() {
        document.getElementById('bulk-operation-modal').hidden = false;
        document.body.classList.add('pf-c-backdrop__open');
    }

    hideBulkOperationModal() {
        document.getElementById('bulk-operation-modal').hidden = true;
        document.body.classList.remove('pf-c-backdrop__open');
        this.bulkOperation = null;
        
        // Refresh data after bulk operation
        this.loadTemplates();
        this.loadStatistics();
        this.selectedTemplates.clear();
        this.updateBulkActionsState();
    }

    async monitorBulkOperation() {
        if (!this.bulkOperation) return;

        const updateProgress = async () => {
            try {
                const response = await fetch(`/admin/realms/${keycloakRealm}/face-recognition/bulk-operations/${this.bulkOperation.operationId}`, {
                    method: 'GET',
                    headers: {
                        'Authorization': 'Bearer ' + keycloakToken,
                        'Content-Type': 'application/json'
                    }
                });

                if (!response.ok) {
                    throw new Error('Failed to get bulk operation status');
                }

                const status = await response.json();
                this.updateBulkOperationProgress(status);

                if (status.status === 'COMPLETED' || status.status === 'FAILED' || status.status === 'CANCELLED') {
                    return; // Stop monitoring
                }

                // Continue monitoring
                setTimeout(updateProgress, 1000);
                
            } catch (error) {
                console.error('Error monitoring bulk operation:', error);
            }
        };

        updateProgress();
    }

    updateBulkOperationProgress(status) {
        const progressPercent = Math.round((status.processedItems / status.totalItems) * 100);
        
        document.getElementById('bulk-operation-measure').textContent = `${progressPercent}%`;
        document.getElementById('bulk-operation-indicator').style.width = `${progressPercent}%`;
        document.getElementById('bulk-operation-status').textContent = 
            `Processed ${status.processedItems} of ${status.totalItems} items. ${status.successfulItems} successful, ${status.failedItems} failed.`;

        if (status.errors && status.errors.length > 0) {
            const errorsContainer = document.getElementById('bulk-operation-errors');
            const errorsList = document.getElementById('bulk-operation-error-list');
            
            errorsList.innerHTML = status.errors.map(error => 
                `<li>${this.escapeHtml(error.itemId)}: ${this.escapeHtml(error.errorMessage)}</li>`
            ).join('');
            
            errorsContainer.hidden = false;
        }

        if (status.status === 'COMPLETED' || status.status === 'FAILED' || status.status === 'CANCELLED') {
            document.getElementById('bulk-operation-cancel-btn').hidden = true;
            document.getElementById('bulk-operation-close-btn').hidden = false;
        }
    }

    async cancelBulkOperation() {
        if (!this.bulkOperation) return;

        try {
            const response = await fetch(`/admin/realms/${keycloakRealm}/face-recognition/bulk-operations/${this.bulkOperation.operationId}/cancel`, {
                method: 'POST',
                headers: {
                    'Authorization': 'Bearer ' + keycloakToken,
                    'Content-Type': 'application/json'
                }
            });

            if (!response.ok) {
                throw new Error('Failed to cancel bulk operation');
            }

            document.getElementById('bulk-operation-status').textContent = 'Cancelling operation...';
            
        } catch (error) {
            console.error('Error cancelling bulk operation:', error);
            this.showError('Failed to cancel bulk operation: ' + error.message);
        }
    }

    // Utility methods
    getHealthStatusClass(status) {
        switch (status) {
            case 'HEALTHY': return 'healthy';
            case 'NEEDS_UPGRADE': return 'needs-upgrade';
            case 'EXPIRING': return 'expiring';
            case 'ISSUES': return 'issues';
            default: return '';
        }
    }

    getHealthStatusIcon(status) {
        switch (status) {
            case 'HEALTHY': return 'fa-check-circle';
            case 'NEEDS_UPGRADE': return 'fa-exclamation-triangle';
            case 'EXPIRING': return 'fa-clock';
            case 'ISSUES': return 'fa-times-circle';
            default: return 'fa-question-circle';
        }
    }

    formatHealthStatus(status) {
        switch (status) {
            case 'HEALTHY': return 'Healthy';
            case 'NEEDS_UPGRADE': return 'Needs Upgrade';
            case 'EXPIRING': return 'Expiring';
            case 'ISSUES': return 'Issues';
            default: return 'Unknown';
        }
    }

    formatDate(dateString) {
        if (!dateString) return '-';
        const date = new Date(dateString);
        return date.toLocaleDateString() + ' ' + date.toLocaleTimeString();
    }

    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    showLoadingState() {
        document.getElementById('templates-table-body').innerHTML = `
            <tr role="row">
                <td colspan="9" class="pf-c-table__cell pf-u-text-align-center">
                    <div class="pf-c-empty-state pf-m-sm">
                        <div class="pf-c-empty-state__content">
                            <div class="pf-c-spinner pf-m-lg" role="progressbar" aria-valuetext="Loading...">
                                <span class="pf-c-spinner__clipper"></span>
                                <span class="pf-c-spinner__lead-ball"></span>
                                <span class="pf-c-spinner__tail-ball"></span>
                            </div>
                            <h2 class="pf-c-title pf-m-lg">Loading templates...</h2>
                        </div>
                    </div>
                </td>
            </tr>
        `;
    }

    hideLoadingState() {
        // Loading state will be replaced by renderTable()
    }

    showError(message) {
        // You could implement a toast notification system here
        alert(message);
    }

    async showConfirmDialog(title, message) {
        // Simple confirmation dialog - you could implement a more sophisticated modal
        return confirm(`${title}\n\n${message}`);
    }

    goToPage(page) {
        const totalPages = Math.ceil(this.filteredTemplates.length / this.itemsPerPage);
        if (page < 1 || page > totalPages) return;
        
        this.currentPage = page;
        this.renderTable();
        this.updatePagination();
    }

    updatePagination() {
        const totalItems = this.filteredTemplates.length;
        const totalPages = Math.ceil(totalItems / this.itemsPerPage);
        const startItem = (this.currentPage - 1) * this.itemsPerPage + 1;
        const endItem = Math.min(this.currentPage * this.itemsPerPage, totalItems);

        document.getElementById('template-count-display').textContent = `${totalItems} items`;
        document.getElementById('pagination-info').textContent = `${startItem} - ${endItem} of ${totalItems} items`;

        // Update pagination button states
        document.getElementById('pagination-first').disabled = this.currentPage === 1;
        document.getElementById('pagination-prev').disabled = this.currentPage === 1;
        document.getElementById('pagination-next').disabled = this.currentPage === totalPages;
        document.getElementById('pagination-last').disabled = this.currentPage === totalPages;
    }

    setupPeriodicRefresh() {
        // Refresh data every 30 seconds
        setInterval(() => {
            this.loadStatistics();
        }, 30000);
    }
}

// Initialize the dashboard when the page loads
document.addEventListener('DOMContentLoaded', () => {
    if (document.getElementById('template-management-dashboard')) {
        new TemplateManagementDashboard();
    }
});