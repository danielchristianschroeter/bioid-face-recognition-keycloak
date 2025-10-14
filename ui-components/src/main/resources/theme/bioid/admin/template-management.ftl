<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "header">
        ${msg("templateManagementTitle")}
    <#elseif section = "form">
        <div id="template-management-dashboard" class="pf-c-page__main-section">
            <!-- Template Management Header -->
            <div class="pf-c-page__main-breadcrumb">
                <nav class="pf-c-breadcrumb" aria-label="breadcrumb">
                    <ol class="pf-c-breadcrumb__list">
                        <li class="pf-c-breadcrumb__item">
                            <a href="#" class="pf-c-breadcrumb__link">Admin Console</a>
                        </li>
                        <li class="pf-c-breadcrumb__item">
                            <a href="#" class="pf-c-breadcrumb__link">Face Authentication</a>
                        </li>
                        <li class="pf-c-breadcrumb__item pf-m-current" aria-current="page">
                            <span class="pf-c-breadcrumb__item-divider">
                                <i class="fas fa-angle-right" aria-hidden="true"></i>
                            </span>
                            Template Management
                        </li>
                    </ol>
                </nav>
            </div>

            <!-- Page Title and Actions -->
            <div class="pf-c-page__main-section pf-m-light">
                <div class="pf-c-content">
                    <h1 class="pf-c-title pf-m-2xl">Template Management</h1>
                    <p class="pf-c-content">Manage biometric templates, monitor health status, and perform bulk operations.</p>
                </div>
            </div>

            <!-- Template Statistics Cards -->
            <div class="pf-c-page__main-section">
                <div class="pf-l-gallery pf-m-gutter" id="template-stats-cards">
                    <div class="pf-c-card">
                        <div class="pf-c-card__title">
                            <h2 class="pf-c-title pf-m-lg">
                                <i class="fas fa-users pf-u-mr-sm"></i>
                                Total Templates
                            </h2>
                        </div>
                        <div class="pf-c-card__body">
                            <div class="pf-c-content">
                                <h3 class="pf-c-title pf-m-2xl" id="total-templates-count">-</h3>
                                <p class="pf-u-color-200">Enrolled users</p>
                            </div>
                        </div>
                    </div>

                    <div class="pf-c-card">
                        <div class="pf-c-card__title">
                            <h2 class="pf-c-title pf-m-lg">
                                <i class="fas fa-check-circle pf-u-mr-sm pf-u-success-color-100"></i>
                                Healthy Templates
                            </h2>
                        </div>
                        <div class="pf-c-card__body">
                            <div class="pf-c-content">
                                <h3 class="pf-c-title pf-m-2xl" id="healthy-templates-count">-</h3>
                                <p class="pf-u-color-200">No issues detected</p>
                            </div>
                        </div>
                    </div>

                    <div class="pf-c-card">
                        <div class="pf-c-card__title">
                            <h2 class="pf-c-title pf-m-lg">
                                <i class="fas fa-exclamation-triangle pf-u-mr-sm pf-u-warning-color-100"></i>
                                Needs Upgrade
                            </h2>
                        </div>
                        <div class="pf-c-card__body">
                            <div class="pf-c-content">
                                <h3 class="pf-c-title pf-m-2xl" id="upgrade-needed-count">-</h3>
                                <p class="pf-u-color-200">Outdated encoder versions</p>
                            </div>
                        </div>
                    </div>

                    <div class="pf-c-card">
                        <div class="pf-c-card__title">
                            <h2 class="pf-c-title pf-m-lg">
                                <i class="fas fa-clock pf-u-mr-sm pf-u-info-color-100"></i>
                                Expiring Soon
                            </h2>
                        </div>
                        <div class="pf-c-card__body">
                            <div class="pf-c-content">
                                <h3 class="pf-c-title pf-m-2xl" id="expiring-templates-count">-</h3>
                                <p class="pf-u-color-200">Within 30 days</p>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Template Management Table -->
            <div class="pf-c-page__main-section">
                <div class="pf-c-card">
                    <div class="pf-c-card__title">
                        <h2 class="pf-c-title pf-m-lg">Templates</h2>
                        <div class="pf-c-card__actions">
                            <div class="pf-c-dropdown">
                                <button class="pf-c-dropdown__toggle pf-m-primary" type="button" id="bulk-actions-dropdown" aria-expanded="false" aria-haspopup="true">
                                    <span class="pf-c-dropdown__toggle-text">Bulk Actions</span>
                                    <span class="pf-c-dropdown__toggle-icon">
                                        <i class="fas fa-caret-down" aria-hidden="true"></i>
                                    </span>
                                </button>
                                <ul class="pf-c-dropdown__menu" aria-labelledby="bulk-actions-dropdown" hidden>
                                    <li>
                                        <button class="pf-c-dropdown__menu-item" type="button" id="bulk-upgrade-btn">
                                            <i class="fas fa-arrow-up pf-u-mr-sm"></i>
                                            Upgrade Selected
                                        </button>
                                    </li>
                                    <li>
                                        <button class="pf-c-dropdown__menu-item" type="button" id="bulk-delete-btn">
                                            <i class="fas fa-trash pf-u-mr-sm"></i>
                                            Delete Selected
                                        </button>
                                    </li>
                                    <li>
                                        <button class="pf-c-dropdown__menu-item" type="button" id="bulk-tag-btn">
                                            <i class="fas fa-tags pf-u-mr-sm"></i>
                                            Manage Tags
                                        </button>
                                    </li>
                                </ul>
                            </div>
                            <button class="pf-c-button pf-m-secondary" type="button" id="refresh-templates-btn">
                                <i class="fas fa-sync-alt pf-u-mr-sm"></i>
                                Refresh
                            </button>
                        </div>
                    </div>

                    <!-- Filters and Search -->
                    <div class="pf-c-card__body">
                        <div class="pf-c-toolbar" id="template-toolbar">
                            <div class="pf-c-toolbar__content">
                                <div class="pf-c-toolbar__content-section">
                                    <div class="pf-c-toolbar__group pf-m-toggle-group pf-m-show-on-xl">
                                        <div class="pf-c-toolbar__toggle">
                                            <button class="pf-c-button pf-m-plain" type="button" aria-label="Show filters" aria-expanded="false" aria-controls="template-expandable-content">
                                                <i class="fas fa-filter" aria-hidden="true"></i>
                                            </button>
                                        </div>
                                    </div>
                                    <div class="pf-c-toolbar__item pf-m-search-filter">
                                        <div class="pf-c-input-group">
                                            <div class="pf-c-select" id="search-type-select">
                                                <span id="search-type-select-label" hidden>Choose one</span>
                                                <button class="pf-c-select__toggle" type="button" id="search-type-select-toggle" aria-haspopup="true" aria-expanded="false" aria-labelledby="search-type-select-label search-type-select-toggle">
                                                    <div class="pf-c-select__toggle-wrapper">
                                                        <span class="pf-c-select__toggle-text">Username</span>
                                                    </div>
                                                    <span class="pf-c-select__toggle-arrow">
                                                        <i class="fas fa-caret-down" aria-hidden="true"></i>
                                                    </span>
                                                </button>
                                                <ul class="pf-c-select__menu" role="listbox" aria-labelledby="search-type-select-label" hidden>
                                                    <li role="presentation">
                                                        <button class="pf-c-select__menu-item" type="button" role="option">Username</button>
                                                    </li>
                                                    <li role="presentation">
                                                        <button class="pf-c-select__menu-item" type="button" role="option">Email</button>
                                                    </li>
                                                    <li role="presentation">
                                                        <button class="pf-c-select__menu-item" type="button" role="option">Class ID</button>
                                                    </li>
                                                </ul>
                                            </div>
                                            <input class="pf-c-form-control" type="search" placeholder="Search templates..." aria-label="Search templates" id="template-search-input">
                                            <button class="pf-c-button pf-m-control" type="button" aria-label="Search button for search input">
                                                <i class="fas fa-search" aria-hidden="true"></i>
                                            </button>
                                        </div>
                                    </div>
                                </div>
                                <div class="pf-c-toolbar__content-section">
                                    <div class="pf-c-toolbar__item pf-m-pagination">
                                        <div class="pf-c-pagination pf-m-compact">
                                            <div class="pf-c-pagination__total-items">
                                                <span id="template-count-display">0 items</span>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                            <div class="pf-c-toolbar__expandable-content pf-m-hidden" id="template-expandable-content">
                                <div class="pf-c-toolbar__group pf-m-filter-group">
                                    <div class="pf-c-toolbar__item">
                                        <div class="pf-c-select" id="health-status-filter">
                                            <span id="health-status-filter-label" hidden>Health Status</span>
                                            <button class="pf-c-select__toggle" type="button" aria-haspopup="true" aria-expanded="false" aria-labelledby="health-status-filter-label">
                                                <div class="pf-c-select__toggle-wrapper">
                                                    <span class="pf-c-select__toggle-text">Health Status</span>
                                                </div>
                                                <span class="pf-c-select__toggle-arrow">
                                                    <i class="fas fa-caret-down" aria-hidden="true"></i>
                                                </span>
                                            </button>
                                            <ul class="pf-c-select__menu" role="listbox" hidden>
                                                <li role="presentation">
                                                    <button class="pf-c-select__menu-item" type="button" role="option">All</button>
                                                </li>
                                                <li role="presentation">
                                                    <button class="pf-c-select__menu-item" type="button" role="option">Healthy</button>
                                                </li>
                                                <li role="presentation">
                                                    <button class="pf-c-select__menu-item" type="button" role="option">Needs Upgrade</button>
                                                </li>
                                                <li role="presentation">
                                                    <button class="pf-c-select__menu-item" type="button" role="option">Expiring</button>
                                                </li>
                                                <li role="presentation">
                                                    <button class="pf-c-select__menu-item" type="button" role="option">Issues</button>
                                                </li>
                                            </ul>
                                        </div>
                                    </div>
                                    <div class="pf-c-toolbar__item">
                                        <div class="pf-c-select" id="encoder-version-filter">
                                            <span id="encoder-version-filter-label" hidden>Encoder Version</span>
                                            <button class="pf-c-select__toggle" type="button" aria-haspopup="true" aria-expanded="false" aria-labelledby="encoder-version-filter-label">
                                                <div class="pf-c-select__toggle-wrapper">
                                                    <span class="pf-c-select__toggle-text">Encoder Version</span>
                                                </div>
                                                <span class="pf-c-select__toggle-arrow">
                                                    <i class="fas fa-caret-down" aria-hidden="true"></i>
                                                </span>
                                            </button>
                                            <ul class="pf-c-select__menu" role="listbox" hidden>
                                                <li role="presentation">
                                                    <button class="pf-c-select__menu-item" type="button" role="option">All</button>
                                                </li>
                                            </ul>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- Templates Table -->
                    <div class="pf-c-card__body pf-u-p-0">
                        <table class="pf-c-table pf-m-grid-md" role="grid" aria-label="Templates table" id="templates-table">
                            <thead>
                                <tr role="row">
                                    <th role="columnheader" scope="col" class="pf-c-table__check">
                                        <input class="pf-c-check__input" type="checkbox" id="select-all-templates" name="select-all-templates" aria-label="Select all templates">
                                    </th>
                                    <th role="columnheader" scope="col" class="pf-c-table__sort pf-m-selected" aria-sort="ascending">
                                        <button class="pf-c-table__button" type="button">
                                            <div class="pf-c-table__button-content">
                                                <span class="pf-c-table__text">User</span>
                                                <span class="pf-c-table__sort-indicator">
                                                    <i class="fas fa-long-arrow-alt-up"></i>
                                                </span>
                                            </div>
                                        </button>
                                    </th>
                                    <th role="columnheader" scope="col">Class ID</th>
                                    <th role="columnheader" scope="col">Health Status</th>
                                    <th role="columnheader" scope="col">Encoder Version</th>
                                    <th role="columnheader" scope="col">Feature Vectors</th>
                                    <th role="columnheader" scope="col">Enrolled</th>
                                    <th role="columnheader" scope="col">Last Auth</th>
                                    <th role="columnheader" scope="col">Actions</th>
                                </tr>
                            </thead>
                            <tbody role="rowgroup" id="templates-table-body">
                                <!-- Template rows will be populated by JavaScript -->
                                <tr role="row">
                                    <td colspan="9" class="pf-c-table__cell pf-u-text-align-center">
                                        <div class="pf-c-empty-state pf-m-sm">
                                            <div class="pf-c-empty-state__content">
                                                <i class="fas fa-search pf-c-empty-state__icon" aria-hidden="true"></i>
                                                <h2 class="pf-c-title pf-m-lg">Loading templates...</h2>
                                                <div class="pf-c-empty-state__body">
                                                    Please wait while we load the template data.
                                                </div>
                                            </div>
                                        </div>
                                    </td>
                                </tr>
                            </tbody>
                        </table>
                    </div>

                    <!-- Pagination -->
                    <div class="pf-c-card__footer">
                        <div class="pf-c-pagination">
                            <div class="pf-c-pagination__total-items">
                                <span id="pagination-info">0 - 0 of 0 items</span>
                            </div>
                            <div class="pf-c-options-menu">
                                <div class="pf-c-options-menu__toggle pf-m-text pf-m-plain">
                                    <span class="pf-c-options-menu__toggle-text">
                                        <b id="per-page-display">20</b> per page
                                    </span>
                                    <button class="pf-c-options-menu__toggle-button" type="button" id="pagination-options-menu" aria-haspopup="listbox" aria-expanded="false">
                                        <i class="fas fa-caret-down" aria-hidden="true"></i>
                                    </button>
                                </div>
                                <ul class="pf-c-options-menu__menu" role="listbox" aria-labelledby="pagination-options-menu" hidden>
                                    <li role="presentation">
                                        <button class="pf-c-options-menu__menu-item" type="button" role="option">10 per page</button>
                                    </li>
                                    <li role="presentation">
                                        <button class="pf-c-options-menu__menu-item" type="button" role="option">20 per page</button>
                                    </li>
                                    <li role="presentation">
                                        <button class="pf-c-options-menu__menu-item" type="button" role="option">50 per page</button>
                                    </li>
                                </ul>
                            </div>
                            <nav class="pf-c-pagination__nav" aria-label="Pagination">
                                <div class="pf-c-pagination__nav-control pf-m-first">
                                    <button class="pf-c-button pf-m-plain" type="button" aria-label="Go to first page" id="pagination-first">
                                        <i class="fas fa-angle-double-left" aria-hidden="true"></i>
                                    </button>
                                </div>
                                <div class="pf-c-pagination__nav-control pf-m-prev">
                                    <button class="pf-c-button pf-m-plain" type="button" aria-label="Go to previous page" id="pagination-prev">
                                        <i class="fas fa-angle-left" aria-hidden="true"></i>
                                    </button>
                                </div>
                                <div class="pf-c-pagination__nav-control pf-m-next">
                                    <button class="pf-c-button pf-m-plain" type="button" aria-label="Go to next page" id="pagination-next">
                                        <i class="fas fa-angle-right" aria-hidden="true"></i>
                                    </button>
                                </div>
                                <div class="pf-c-pagination__nav-control pf-m-last">
                                    <button class="pf-c-button pf-m-plain" type="button" aria-label="Go to last page" id="pagination-last">
                                        <i class="fas fa-angle-double-right" aria-hidden="true"></i>
                                    </button>
                                </div>
                            </nav>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <!-- Template Details Modal -->
        <div class="pf-c-backdrop" id="template-details-modal" hidden>
            <div class="pf-l-bullseye">
                <div class="pf-c-modal-box pf-m-lg" role="dialog" aria-modal="true" aria-labelledby="template-details-modal-title" aria-describedby="template-details-modal-description">
                    <button class="pf-c-button pf-m-plain" type="button" aria-label="Close dialog" id="template-details-modal-close">
                        <i class="fas fa-times" aria-hidden="true"></i>
                    </button>
                    <header class="pf-c-modal-box__header">
                        <h1 class="pf-c-modal-box__title" id="template-details-modal-title">Template Details</h1>
                    </header>
                    <div class="pf-c-modal-box__body" id="template-details-modal-description">
                        <div class="pf-l-grid pf-m-all-6-col-on-md pf-m-all-4-col-on-lg pf-m-gutter">
                            <div class="pf-l-grid__item">
                                <div class="pf-c-card">
                                    <div class="pf-c-card__title">
                                        <h3 class="pf-c-title pf-m-md">Basic Information</h3>
                                    </div>
                                    <div class="pf-c-card__body">
                                        <dl class="pf-c-description-list">
                                            <div class="pf-c-description-list__group">
                                                <dt class="pf-c-description-list__term">
                                                    <span class="pf-c-description-list__text">Username</span>
                                                </dt>
                                                <dd class="pf-c-description-list__description">
                                                    <div class="pf-c-description-list__text" id="modal-username">-</div>
                                                </dd>
                                            </div>
                                            <div class="pf-c-description-list__group">
                                                <dt class="pf-c-description-list__term">
                                                    <span class="pf-c-description-list__text">Email</span>
                                                </dt>
                                                <dd class="pf-c-description-list__description">
                                                    <div class="pf-c-description-list__text" id="modal-email">-</div>
                                                </dd>
                                            </div>
                                            <div class="pf-c-description-list__group">
                                                <dt class="pf-c-description-list__term">
                                                    <span class="pf-c-description-list__text">Class ID</span>
                                                </dt>
                                                <dd class="pf-c-description-list__description">
                                                    <div class="pf-c-description-list__text" id="modal-class-id">-</div>
                                                </dd>
                                            </div>
                                        </dl>
                                    </div>
                                </div>
                            </div>
                            <div class="pf-l-grid__item">
                                <div class="pf-c-card">
                                    <div class="pf-c-card__title">
                                        <h3 class="pf-c-title pf-m-md">Template Status</h3>
                                    </div>
                                    <div class="pf-c-card__body">
                                        <dl class="pf-c-description-list">
                                            <div class="pf-c-description-list__group">
                                                <dt class="pf-c-description-list__term">
                                                    <span class="pf-c-description-list__text">Health Status</span>
                                                </dt>
                                                <dd class="pf-c-description-list__description">
                                                    <div class="pf-c-description-list__text" id="modal-health-status">-</div>
                                                </dd>
                                            </div>
                                            <div class="pf-c-description-list__group">
                                                <dt class="pf-c-description-list__term">
                                                    <span class="pf-c-description-list__text">Encoder Version</span>
                                                </dt>
                                                <dd class="pf-c-description-list__description">
                                                    <div class="pf-c-description-list__text" id="modal-encoder-version">-</div>
                                                </dd>
                                            </div>
                                            <div class="pf-c-description-list__group">
                                                <dt class="pf-c-description-list__term">
                                                    <span class="pf-c-description-list__text">Feature Vectors</span>
                                                </dt>
                                                <dd class="pf-c-description-list__description">
                                                    <div class="pf-c-description-list__text" id="modal-feature-vectors">-</div>
                                                </dd>
                                            </div>
                                        </dl>
                                    </div>
                                </div>
                            </div>
                            <div class="pf-l-grid__item">
                                <div class="pf-c-card">
                                    <div class="pf-c-card__title">
                                        <h3 class="pf-c-title pf-m-md">Timestamps</h3>
                                    </div>
                                    <div class="pf-c-card__body">
                                        <dl class="pf-c-description-list">
                                            <div class="pf-c-description-list__group">
                                                <dt class="pf-c-description-list__term">
                                                    <span class="pf-c-description-list__text">Enrolled</span>
                                                </dt>
                                                <dd class="pf-c-description-list__description">
                                                    <div class="pf-c-description-list__text" id="modal-enrolled-at">-</div>
                                                </dd>
                                            </div>
                                            <div class="pf-c-description-list__group">
                                                <dt class="pf-c-description-list__term">
                                                    <span class="pf-c-description-list__text">Last Authentication</span>
                                                </dt>
                                                <dd class="pf-c-description-list__description">
                                                    <div class="pf-c-description-list__text" id="modal-last-auth">-</div>
                                                </dd>
                                            </div>
                                            <div class="pf-c-description-list__group">
                                                <dt class="pf-c-description-list__term">
                                                    <span class="pf-c-description-list__text">Expires</span>
                                                </dt>
                                                <dd class="pf-c-description-list__description">
                                                    <div class="pf-c-description-list__text" id="modal-expires-at">-</div>
                                                </dd>
                                            </div>
                                        </dl>
                                    </div>
                                </div>
                            </div>
                            <div class="pf-l-grid__item pf-m-12-col">
                                <div class="pf-c-card">
                                    <div class="pf-c-card__title">
                                        <h3 class="pf-c-title pf-m-md">Template Thumbnails</h3>
                                    </div>
                                    <div class="pf-c-card__body">
                                        <div class="pf-l-gallery pf-m-gutter" id="modal-thumbnails">
                                            <!-- Thumbnails will be populated by JavaScript -->
                                            <div class="pf-c-empty-state pf-m-sm">
                                                <div class="pf-c-empty-state__content">
                                                    <i class="fas fa-image pf-c-empty-state__icon" aria-hidden="true"></i>
                                                    <h4 class="pf-c-title pf-m-md">No thumbnails available</h4>
                                                    <div class="pf-c-empty-state__body">
                                                        Thumbnails are not stored for this template.
                                                    </div>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                    <footer class="pf-c-modal-box__footer">
                        <button class="pf-c-button pf-m-primary" type="button" id="modal-upgrade-btn">
                            <i class="fas fa-arrow-up pf-u-mr-sm"></i>
                            Upgrade Template
                        </button>
                        <button class="pf-c-button pf-m-secondary" type="button" id="modal-download-btn">
                            <i class="fas fa-download pf-u-mr-sm"></i>
                            Download Data
                        </button>
                        <button class="pf-c-button pf-m-danger" type="button" id="modal-delete-btn">
                            <i class="fas fa-trash pf-u-mr-sm"></i>
                            Delete Template
                        </button>
                        <button class="pf-c-button pf-m-link" type="button" id="modal-cancel-btn">
                            Close
                        </button>
                    </footer>
                </div>
            </div>
        </div>

        <!-- Bulk Operation Progress Modal -->
        <div class="pf-c-backdrop" id="bulk-operation-modal" hidden>
            <div class="pf-l-bullseye">
                <div class="pf-c-modal-box" role="dialog" aria-modal="true" aria-labelledby="bulk-operation-modal-title">
                    <header class="pf-c-modal-box__header">
                        <h1 class="pf-c-modal-box__title" id="bulk-operation-modal-title">Bulk Operation Progress</h1>
                    </header>
                    <div class="pf-c-modal-box__body">
                        <div class="pf-c-progress" id="bulk-operation-progress">
                            <div class="pf-c-progress__description" id="bulk-operation-description">Processing templates...</div>
                            <div class="pf-c-progress__status">
                                <span class="pf-c-progress__measure" id="bulk-operation-measure">0%</span>
                            </div>
                            <div class="pf-c-progress__bar" role="progressbar" aria-valuemin="0" aria-valuemax="100" aria-valuenow="0" aria-labelledby="bulk-operation-description">
                                <div class="pf-c-progress__indicator" style="width: 0%;" id="bulk-operation-indicator"></div>
                            </div>
                        </div>
                        <div class="pf-c-content pf-u-mt-md">
                            <p id="bulk-operation-status">Starting operation...</p>
                            <div id="bulk-operation-errors" class="pf-u-mt-sm" hidden>
                                <h4 class="pf-c-title pf-m-md">Errors:</h4>
                                <ul id="bulk-operation-error-list"></ul>
                            </div>
                        </div>
                    </div>
                    <footer class="pf-c-modal-box__footer">
                        <button class="pf-c-button pf-m-danger" type="button" id="bulk-operation-cancel-btn">
                            Cancel Operation
                        </button>
                        <button class="pf-c-button pf-m-secondary" type="button" id="bulk-operation-close-btn" hidden>
                            Close
                        </button>
                    </footer>
                </div>
            </div>
        </div>
    </#if>
</@layout.registrationLayout>