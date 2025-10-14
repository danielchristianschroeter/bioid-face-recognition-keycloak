/**
 * Common Admin Console JavaScript Utilities
 * Shared functionality across all admin components
 */

// Global admin utilities
window.AdminUtils = {
    
    /**
     * Show a toast notification
     */
    showToast: function(message, type = 'info', duration = 5000) {
        const toast = document.createElement('div');
        toast.className = `admin-toast admin-toast-${type}`;
        toast.innerHTML = `
            <div class="admin-toast-content">
                <i class="fas ${this.getToastIcon(type)} admin-toast-icon"></i>
                <span class="admin-toast-message">${this.escapeHtml(message)}</span>
                <button class="admin-toast-close" onclick="this.parentElement.parentElement.remove()">
                    <i class="fas fa-times"></i>
                </button>
            </div>
        `;
        
        // Add toast container if it doesn't exist
        let container = document.getElementById('admin-toast-container');
        if (!container) {
            container = document.createElement('div');
            container.id = 'admin-toast-container';
            container.className = 'admin-toast-container';
            document.body.appendChild(container);
        }
        
        container.appendChild(toast);
        
        // Auto-remove after duration
        setTimeout(() => {
            if (toast.parentElement) {
                toast.remove();
            }
        }, duration);
        
        return toast;
    },
    
    /**
     * Get icon for toast type
     */
    getToastIcon: function(type) {
        switch (type) {
            case 'success': return 'fa-check-circle';
            case 'warning': return 'fa-exclamation-triangle';
            case 'error': return 'fa-times-circle';
            case 'info': 
            default: return 'fa-info-circle';
        }
    },
    
    /**
     * Show a confirmation dialog
     */
    showConfirmDialog: function(title, message, confirmText = 'Confirm', cancelText = 'Cancel') {
        return new Promise((resolve) => {
            const modal = document.createElement('div');
            modal.className = 'admin-modal-backdrop';
            modal.innerHTML = `
                <div class="admin-modal">
                    <div class="admin-modal-header">
                        <h3 class="admin-modal-title">${this.escapeHtml(title)}</h3>
                        <button class="admin-modal-close" onclick="this.closest('.admin-modal-backdrop').remove(); resolve(false);">
                            <i class="fas fa-times"></i>
                        </button>
                    </div>
                    <div class="admin-modal-body">
                        <p>${this.escapeHtml(message)}</p>
                    </div>
                    <div class="admin-modal-footer">
                        <button class="admin-btn admin-btn-primary" onclick="this.closest('.admin-modal-backdrop').remove(); resolve(true);">
                            ${this.escapeHtml(confirmText)}
                        </button>
                        <button class="admin-btn admin-btn-secondary" onclick="this.closest('.admin-modal-backdrop').remove(); resolve(false);">
                            ${this.escapeHtml(cancelText)}
                        </button>
                    </div>
                </div>
            `;
            
            document.body.appendChild(modal);
            
            // Focus the confirm button
            modal.querySelector('.admin-btn-primary').focus();
            
            // Handle escape key
            const handleEscape = (e) => {
                if (e.key === 'Escape') {
                    modal.remove();
                    document.removeEventListener('keydown', handleEscape);
                    resolve(false);
                }
            };
            document.addEventListener('keydown', handleEscape);
        });
    },
    
    /**
     * Format file size in bytes
     */
    formatBytes: function(bytes, decimals = 2) {
        if (bytes === 0) return '0 Bytes';
        
        const k = 1024;
        const dm = decimals < 0 ? 0 : decimals;
        const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'];
        
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        
        return parseFloat((bytes / Math.pow(k, i)).toFixed(dm)) + ' ' + sizes[i];
    },
    
    /**
     * Format duration in milliseconds
     */
    formatDuration: function(ms) {
        if (ms < 1000) {
            return `${ms}ms`;
        } else if (ms < 60000) {
            return `${(ms / 1000).toFixed(1)}s`;
        } else if (ms < 3600000) {
            return `${(ms / 60000).toFixed(1)}m`;
        } else {
            return `${(ms / 3600000).toFixed(1)}h`;
        }
    },
    
    /**
     * Format timestamp relative to now
     */
    formatRelativeTime: function(timestamp) {
        const now = new Date();
        const date = new Date(timestamp);
        const diffMs = now - date;
        const diffSecs = Math.floor(diffMs / 1000);
        const diffMins = Math.floor(diffMs / 60000);
        const diffHours = Math.floor(diffMs / 3600000);
        const diffDays = Math.floor(diffMs / 86400000);
        
        if (diffSecs < 60) {
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
    },
    
    /**
     * Escape HTML to prevent XSS
     */
    escapeHtml: function(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    },
    
    /**
     * Debounce function calls
     */
    debounce: function(func, wait, immediate) {
        let timeout;
        return function executedFunction(...args) {
            const later = () => {
                timeout = null;
                if (!immediate) func.apply(this, args);
            };
            const callNow = immediate && !timeout;
            clearTimeout(timeout);
            timeout = setTimeout(later, wait);
            if (callNow) func.apply(this, args);
        };
    },
    
    /**
     * Throttle function calls
     */
    throttle: function(func, limit) {
        let inThrottle;
        return function(...args) {
            if (!inThrottle) {
                func.apply(this, args);
                inThrottle = true;
                setTimeout(() => inThrottle = false, limit);
            }
        };
    },
    
    /**
     * Make HTTP requests with proper error handling
     */
    request: async function(url, options = {}) {
        const defaultOptions = {
            method: 'GET',
            headers: {
                'Authorization': 'Bearer ' + (window.keycloakToken || ''),
                'Content-Type': 'application/json'
            }
        };
        
        const finalOptions = { ...defaultOptions, ...options };
        if (finalOptions.headers && options.headers) {
            finalOptions.headers = { ...defaultOptions.headers, ...options.headers };
        }
        
        try {
            const response = await fetch(url, finalOptions);
            
            if (!response.ok) {
                const errorText = await response.text();
                let errorMessage;
                try {
                    const errorJson = JSON.parse(errorText);
                    errorMessage = errorJson.error || errorJson.message || `HTTP ${response.status}`;
                } catch {
                    errorMessage = errorText || `HTTP ${response.status}`;
                }
                throw new Error(errorMessage);
            }
            
            const contentType = response.headers.get('content-type');
            if (contentType && contentType.includes('application/json')) {
                return await response.json();
            } else {
                return await response.text();
            }
        } catch (error) {
            console.error('Request failed:', error);
            throw error;
        }
    },
    
    /**
     * Copy text to clipboard
     */
    copyToClipboard: async function(text) {
        try {
            await navigator.clipboard.writeText(text);
            this.showToast('Copied to clipboard', 'success');
            return true;
        } catch (error) {
            console.error('Failed to copy to clipboard:', error);
            this.showToast('Failed to copy to clipboard', 'error');
            return false;
        }
    },
    
    /**
     * Download data as file
     */
    downloadFile: function(data, filename, type = 'application/json') {
        const blob = new Blob([data], { type });
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = filename;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        URL.revokeObjectURL(url);
    },
    
    /**
     * Validate email address
     */
    isValidEmail: function(email) {
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return emailRegex.test(email);
    },
    
    /**
     * Generate random ID
     */
    generateId: function(prefix = 'admin') {
        return `${prefix}-${Math.random().toString(36).substr(2, 9)}`;
    },
    
    /**
     * Setup keyboard navigation for dropdowns
     */
    setupDropdownKeyboard: function(dropdown) {
        const toggle = dropdown.querySelector('.pf-c-select__toggle, .pf-c-dropdown__toggle');
        const menu = dropdown.querySelector('.pf-c-select__menu, .pf-c-dropdown__menu');
        const items = menu.querySelectorAll('.pf-c-select__menu-item, .pf-c-dropdown__menu-item');
        
        let currentIndex = -1;
        
        toggle.addEventListener('keydown', (e) => {
            if (e.key === 'ArrowDown' || e.key === 'Enter' || e.key === ' ') {
                e.preventDefault();
                toggle.click();
                if (items.length > 0) {
                    currentIndex = 0;
                    items[0].focus();
                }
            }
        });
        
        items.forEach((item, index) => {
            item.addEventListener('keydown', (e) => {
                switch (e.key) {
                    case 'ArrowDown':
                        e.preventDefault();
                        currentIndex = (currentIndex + 1) % items.length;
                        items[currentIndex].focus();
                        break;
                    case 'ArrowUp':
                        e.preventDefault();
                        currentIndex = currentIndex <= 0 ? items.length - 1 : currentIndex - 1;
                        items[currentIndex].focus();
                        break;
                    case 'Escape':
                        e.preventDefault();
                        toggle.click();
                        toggle.focus();
                        break;
                    case 'Enter':
                    case ' ':
                        e.preventDefault();
                        item.click();
                        break;
                }
            });
        });
    }
};

// Global error handler
window.addEventListener('error', (event) => {
    console.error('Global error:', event.error);
    if (window.AdminUtils) {
        window.AdminUtils.showToast('An unexpected error occurred', 'error');
    }
});

// Global unhandled promise rejection handler
window.addEventListener('unhandledrejection', (event) => {
    console.error('Unhandled promise rejection:', event.reason);
    if (window.AdminUtils) {
        window.AdminUtils.showToast('An unexpected error occurred', 'error');
    }
});

// Initialize common functionality when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    // Setup all dropdowns for keyboard navigation
    document.querySelectorAll('.pf-c-select, .pf-c-dropdown').forEach(dropdown => {
        window.AdminUtils.setupDropdownKeyboard(dropdown);
    });
    
    // Add toast container styles if not already present
    if (!document.getElementById('admin-toast-styles')) {
        const styles = document.createElement('style');
        styles.id = 'admin-toast-styles';
        styles.textContent = `
            .admin-toast-container {
                position: fixed;
                top: 20px;
                right: 20px;
                z-index: 9999;
                max-width: 400px;
            }
            
            .admin-toast {
                margin-bottom: 10px;
                border-radius: 4px;
                box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
                animation: admin-toast-slide-in 0.3s ease-out;
            }
            
            .admin-toast-content {
                display: flex;
                align-items: center;
                padding: 12px 16px;
                background: white;
                border-radius: 4px;
                border-left: 4px solid;
            }
            
            .admin-toast-info .admin-toast-content { border-left-color: #17a2b8; }
            .admin-toast-success .admin-toast-content { border-left-color: #28a745; }
            .admin-toast-warning .admin-toast-content { border-left-color: #ffc107; }
            .admin-toast-error .admin-toast-content { border-left-color: #dc3545; }
            
            .admin-toast-icon {
                margin-right: 12px;
                font-size: 18px;
            }
            
            .admin-toast-info .admin-toast-icon { color: #17a2b8; }
            .admin-toast-success .admin-toast-icon { color: #28a745; }
            .admin-toast-warning .admin-toast-icon { color: #ffc107; }
            .admin-toast-error .admin-toast-icon { color: #dc3545; }
            
            .admin-toast-message {
                flex: 1;
                font-size: 14px;
                color: #333;
            }
            
            .admin-toast-close {
                background: none;
                border: none;
                color: #999;
                cursor: pointer;
                font-size: 16px;
                margin-left: 12px;
                padding: 0;
            }
            
            .admin-toast-close:hover {
                color: #666;
            }
            
            @keyframes admin-toast-slide-in {
                from {
                    transform: translateX(100%);
                    opacity: 0;
                }
                to {
                    transform: translateX(0);
                    opacity: 1;
                }
            }
            
            .admin-modal-backdrop {
                position: fixed;
                top: 0;
                left: 0;
                width: 100%;
                height: 100%;
                background: rgba(0, 0, 0, 0.5);
                display: flex;
                align-items: center;
                justify-content: center;
                z-index: 10000;
            }
            
            .admin-modal {
                background: white;
                border-radius: 8px;
                box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
                max-width: 500px;
                width: 90%;
                max-height: 90%;
                overflow: hidden;
            }
            
            .admin-modal-header {
                display: flex;
                align-items: center;
                justify-content: space-between;
                padding: 20px 24px;
                border-bottom: 1px solid #e9ecef;
            }
            
            .admin-modal-title {
                margin: 0;
                font-size: 18px;
                font-weight: 600;
                color: #333;
            }
            
            .admin-modal-close {
                background: none;
                border: none;
                color: #999;
                cursor: pointer;
                font-size: 18px;
                padding: 4px;
            }
            
            .admin-modal-close:hover {
                color: #666;
            }
            
            .admin-modal-body {
                padding: 20px 24px;
                color: #666;
                line-height: 1.5;
            }
            
            .admin-modal-footer {
                display: flex;
                gap: 12px;
                justify-content: flex-end;
                padding: 20px 24px;
                border-top: 1px solid #e9ecef;
                background: #f8f9fa;
            }
        `;
        document.head.appendChild(styles);
    }
});

// Export for use in other modules
if (typeof module !== 'undefined' && module.exports) {
    module.exports = window.AdminUtils;
}