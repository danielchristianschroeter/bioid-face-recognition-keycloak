<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "header">
        ${msg("livenessConfigTitle")}
    <#elseif section = "form">
        <div id="liveness-config-panel" class="pf-c-page__main-section">
            <!-- Liveness Configuration Header -->
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
                            Liveness Detection
                        </li>
                    </ol>
                </nav>
            </div>

            <!-- Page Title -->
            <div class="pf-c-page__main-section pf-m-light">
                <div class="pf-c-content">
                    <h1 class="pf-c-title pf-m-2xl">Liveness Detection Configuration</h1>
                    <p class="pf-c-content">Configure anti-spoofing measures and liveness detection modes for enhanced security.</p>
                </div>
            </div>

            <!-- Configuration Form -->
            <div class="pf-c-page__main-section">
                <div class="pf-l-grid pf-m-all-12-col pf-m-all-6-col-on-lg pf-m-gutter">
                    <!-- Configuration Panel -->
                    <div class="pf-l-grid__item">
                        <div class="pf-c-card">
                            <div class="pf-c-card__title">
                                <h2 class="pf-c-title pf-m-lg">
                                    <i class="fas fa-shield-alt pf-u-mr-sm"></i>
                                    Liveness Detection Settings
                                </h2>
                            </div>
                            <div class="pf-c-card__body">
                                <form id="liveness-config-form" class="pf-c-form">
                                    <!-- Liveness Mode Selection -->
                                    <div class="pf-c-form__group">
                                        <div class="pf-c-form__group-label">
                                            <label class="pf-c-form__label" for="liveness-mode-select">
                                                <span class="pf-c-form__label-text">Liveness Detection Mode</span>
                                                <span class="pf-c-form__label-required" aria-hidden="true">*</span>
                                            </label>
                                        </div>
                                        <div class="pf-c-form__group-control">
                                            <div class="pf-c-select" id="liveness-mode-select">
                                                <span id="liveness-mode-select-label" hidden>Choose liveness mode</span>
                                                <button class="pf-c-select__toggle" type="button" id="liveness-mode-toggle" aria-haspopup="true" aria-expanded="false" aria-labelledby="liveness-mode-select-label liveness-mode-toggle">
                                                    <div class="pf-c-select__toggle-wrapper">
                                                        <span class="pf-c-select__toggle-text" id="liveness-mode-text">Select mode...</span>
                                                    </div>
                                                    <span class="pf-c-select__toggle-arrow">
                                                        <i class="fas fa-caret-down" aria-hidden="true"></i>
                                                    </span>
                                                </button>
                                                <ul class="pf-c-select__menu" role="listbox" aria-labelledby="liveness-mode-select-label" hidden>
                                                    <li role="presentation">
                                                        <button class="pf-c-select__menu-item" type="button" role="option" data-value="PASSIVE">
                                                            <div class="liveness-mode-option">
                                                                <div class="liveness-mode-title">Passive Detection</div>
                                                                <div class="liveness-mode-description">Single image texture analysis</div>
                                                            </div>
                                                        </button>
                                                    </li>
                                                    <li role="presentation">
                                                        <button class="pf-c-select__menu-item" type="button" role="option" data-value="ACTIVE">
                                                            <div class="liveness-mode-option">
                                                                <div class="liveness-mode-title">Active Detection</div>
                                                                <div class="liveness-mode-description">Two images with motion detection</div>
                                                            </div>
                                                        </button>
                                                    </li>
                                                    <li role="presentation">
                                                        <button class="pf-c-select__menu-item" type="button" role="option" data-value="CHALLENGE_RESPONSE">
                                                            <div class="liveness-mode-option">
                                                                <div class="liveness-mode-title">Challenge-Response</div>
                                                                <div class="liveness-mode-description">Head movement validation</div>
                                                            </div>
                                                        </button>
                                                    </li>
                                                </ul>
                                            </div>
                                            <div class="pf-c-form__helper-text">
                                                <div class="pf-c-helper-text">
                                                    <div class="pf-c-helper-text__item">
                                                        <span class="pf-c-helper-text__item-text">Choose the liveness detection method based on your security requirements.</span>
                                                    </div>
                                                </div>
                                            </div>
                                        </div>
                                    </div>

                                    <!-- Liveness Threshold -->
                                    <div class="pf-c-form__group">
                                        <div class="pf-c-form__group-label">
                                            <label class="pf-c-form__label" for="liveness-threshold">
                                                <span class="pf-c-form__label-text">Liveness Threshold</span>
                                            </label>
                                        </div>
                                        <div class="pf-c-form__group-control">
                                            <div class="liveness-threshold-container">
                                                <input type="range" class="pf-c-slider__slider" id="liveness-threshold" 
                                                       min="0" max="1" step="0.1" value="0.7" 
                                                       aria-label="Liveness threshold">
                                                <div class="pf-c-slider__rail">
                                                    <div class="pf-c-slider__rail-track"></div>
                                                </div>
                                                <div class="liveness-threshold-labels">
                                                    <span class="threshold-label-low">Low (0.0)</span>
                                                    <span class="threshold-label-current" id="threshold-current-value">0.7</span>
                                                    <span class="threshold-label-high">High (1.0)</span>
                                                </div>
                                            </div>
                                            <div class="pf-c-form__helper-text">
                                                <div class="pf-c-helper-text">
                                                    <div class="pf-c-helper-text__item">
                                                        <span class="pf-c-helper-text__item-text">Higher values require stronger liveness signals but may increase false rejections.</span>
                                                    </div>
                                                </div>
                                            </div>
                                        </div>
                                    </div>

                                    <!-- Challenge Directions (shown only for CHALLENGE_RESPONSE mode) -->
                                    <div class="pf-c-form__group" id="challenge-directions-group" style="display: none;">
                                        <div class="pf-c-form__group-label">
                                            <label class="pf-c-form__label">
                                                <span class="pf-c-form__label-text">Challenge Directions</span>
                                            </label>
                                        </div>
                                        <div class="pf-c-form__group-control">
                                            <div class="pf-c-check-group">
                                                <div class="pf-c-check">
                                                    <input class="pf-c-check__input" type="checkbox" id="challenge-up" name="challengeDirections" value="UP" checked>
                                                    <label class="pf-c-check__label" for="challenge-up">
                                                        <i class="fas fa-arrow-up pf-u-mr-sm"></i>
                                                        Look Up
                                                    </label>
                                                </div>
                                                <div class="pf-c-check">
                                                    <input class="pf-c-check__input" type="checkbox" id="challenge-down" name="challengeDirections" value="DOWN" checked>
                                                    <label class="pf-c-check__label" for="challenge-down">
                                                        <i class="fas fa-arrow-down pf-u-mr-sm"></i>
                                                        Look Down
                                                    </label>
                                                </div>
                                                <div class="pf-c-check">
                                                    <input class="pf-c-check__input" type="checkbox" id="challenge-left" name="challengeDirections" value="LEFT" checked>
                                                    <label class="pf-c-check__label" for="challenge-left">
                                                        <i class="fas fa-arrow-left pf-u-mr-sm"></i>
                                                        Look Left
                                                    </label>
                                                </div>
                                                <div class="pf-c-check">
                                                    <input class="pf-c-check__input" type="checkbox" id="challenge-right" name="challengeDirections" value="RIGHT" checked>
                                                    <label class="pf-c-check__label" for="challenge-right">
                                                        <i class="fas fa-arrow-right pf-u-mr-sm"></i>
                                                        Look Right
                                                    </label>
                                                </div>
                                            </div>
                                            <div class="pf-c-form__helper-text">
                                                <div class="pf-c-helper-text">
                                                    <div class="pf-c-helper-text__item">
                                                        <span class="pf-c-helper-text__item-text">Select which head movements users can be asked to perform.</span>
                                                    </div>
                                                </div>
                                            </div>
                                        </div>
                                    </div>

                                    <!-- Advanced Settings -->
                                    <div class="pf-c-expandable-section" id="advanced-settings">
                                        <button type="button" class="pf-c-expandable-section__toggle" aria-expanded="false">
                                            <span class="pf-c-expandable-section__toggle-icon">
                                                <i class="fas fa-angle-right" aria-hidden="true"></i>
                                            </span>
                                            <span class="pf-c-expandable-section__toggle-text">Advanced Settings</span>
                                        </button>
                                        <div class="pf-c-expandable-section__content" hidden>
                                            <div class="pf-c-form__group">
                                                <div class="pf-c-form__group-label">
                                                    <label class="pf-c-form__label" for="max-overhead">
                                                        <span class="pf-c-form__label-text">Maximum Overhead (ms)</span>
                                                    </label>
                                                </div>
                                                <div class="pf-c-form__group-control">
                                                    <input class="pf-c-form-control" type="number" id="max-overhead" 
                                                           min="50" max="5000" step="50" value="1000">
                                                    <div class="pf-c-form__helper-text">
                                                        <div class="pf-c-helper-text">
                                                            <div class="pf-c-helper-text__item">
                                                                <span class="pf-c-helper-text__item-text">Maximum additional time allowed for liveness detection.</span>
                                                            </div>
                                                        </div>
                                                    </div>
                                                </div>
                                            </div>

                                            <div class="pf-c-form__group">
                                                <div class="pf-c-form__group-control">
                                                    <div class="pf-c-check">
                                                        <input class="pf-c-check__input" type="checkbox" id="fallback-enabled" checked>
                                                        <label class="pf-c-check__label" for="fallback-enabled">
                                                            Enable fallback to standard verification if liveness detection fails
                                                        </label>
                                                    </div>
                                                </div>
                                            </div>

                                            <div class="pf-c-form__group">
                                                <div class="pf-c-form__group-control">
                                                    <div class="pf-c-check">
                                                        <input class="pf-c-check__input" type="checkbox" id="detailed-logging">
                                                        <label class="pf-c-check__label" for="detailed-logging">
                                                            Enable detailed logging for liveness detection events
                                                        </label>
                                                    </div>
                                                </div>
                                            </div>
                                        </div>
                                    </div>

                                    <!-- Form Actions -->
                                    <div class="pf-c-form__group pf-m-action">
                                        <div class="pf-c-form__actions">
                                            <button class="pf-c-button pf-m-primary" type="submit" id="save-config-btn">
                                                <i class="fas fa-save pf-u-mr-sm"></i>
                                                Save Configuration
                                            </button>
                                            <button class="pf-c-button pf-m-secondary" type="button" id="test-liveness-btn">
                                                <i class="fas fa-vial pf-u-mr-sm"></i>
                                                Test Configuration
                                            </button>
                                            <button class="pf-c-button pf-m-link" type="button" id="reset-config-btn">
                                                Reset to Defaults
                                            </button>
                                        </div>
                                    </div>
                                </form>
                            </div>
                        </div>
                    </div>

                    <!-- Test Panel -->
                    <div class="pf-l-grid__item">
                        <div class="pf-c-card">
                            <div class="pf-c-card__title">
                                <h2 class="pf-c-title pf-m-lg">
                                    <i class="fas fa-flask pf-u-mr-sm"></i>
                                    Test Liveness Detection
                                </h2>
                            </div>
                            <div class="pf-c-card__body">
                                <div class="pf-c-content">
                                    <p>Upload test images to validate your liveness detection configuration.</p>
                                </div>

                                <!-- Image Upload Area -->
                                <div class="liveness-test-upload" id="liveness-test-upload">
                                    <div class="pf-c-file-upload">
                                        <div class="pf-c-file-upload__file-select">
                                            <div class="pf-c-input-group">
                                                <div class="pf-c-file-upload__file-select-button">
                                                    <button class="pf-c-button pf-m-tertiary" type="button" id="upload-test-images-btn">
                                                        <i class="fas fa-upload pf-u-mr-sm"></i>
                                                        Upload Images
                                                    </button>
                                                    <input class="pf-c-file-upload__file-select-input" type="file" 
                                                           id="test-images-input" accept="image/*" multiple 
                                                           aria-label="Upload test images">
                                                </div>
                                                <input class="pf-c-form-control" readonly type="text" 
                                                       id="test-images-filename" placeholder="No files selected"
                                                       aria-label="Selected files">
                                            </div>
                                        </div>
                                    </div>
                                    
                                    <!-- Image Preview -->
                                    <div class="liveness-test-preview" id="test-images-preview" hidden>
                                        <h4 class="pf-c-title pf-m-md">Test Images</h4>
                                        <div class="pf-l-gallery pf-m-gutter" id="test-images-gallery">
                                            <!-- Preview images will be inserted here -->
                                        </div>
                                    </div>
                                </div>

                                <!-- Test Results -->
                                <div class="liveness-test-results" id="liveness-test-results" hidden>
                                    <h4 class="pf-c-title pf-m-md">Test Results</h4>
                                    <div class="pf-c-alert" id="test-result-alert">
                                        <div class="pf-c-alert__icon">
                                            <i class="fas fa-check-circle" aria-hidden="true"></i>
                                        </div>
                                        <div class="pf-c-alert__title">
                                            <span id="test-result-title">Liveness Detection Result</span>
                                        </div>
                                        <div class="pf-c-alert__description" id="test-result-description">
                                            <!-- Test result details will be inserted here -->
                                        </div>
                                    </div>

                                    <!-- Detailed Results -->
                                    <div class="pf-c-content" id="test-result-details">
                                        <dl class="pf-c-description-list pf-m-horizontal">
                                            <div class="pf-c-description-list__group">
                                                <dt class="pf-c-description-list__term">
                                                    <span class="pf-c-description-list__text">Liveness Score</span>
                                                </dt>
                                                <dd class="pf-c-description-list__description">
                                                    <div class="pf-c-description-list__text" id="test-liveness-score">-</div>
                                                </dd>
                                            </div>
                                            <div class="pf-c-description-list__group">
                                                <dt class="pf-c-description-list__term">
                                                    <span class="pf-c-description-list__text">Detection Mode</span>
                                                </dt>
                                                <dd class="pf-c-description-list__description">
                                                    <div class="pf-c-description-list__text" id="test-detection-mode">-</div>
                                                </dd>
                                            </div>
                                            <div class="pf-c-description-list__group">
                                                <dt class="pf-c-description-list__term">
                                                    <span class="pf-c-description-list__text">Processing Time</span>
                                                </dt>
                                                <dd class="pf-c-description-list__description">
                                                    <div class="pf-c-description-list__text" id="test-processing-time">-</div>
                                                </dd>
                                            </div>
                                        </dl>
                                    </div>
                                </div>

                                <!-- Test Actions -->
                                <div class="pf-c-form__actions pf-u-mt-md">
                                    <button class="pf-c-button pf-m-primary" type="button" id="run-liveness-test-btn" disabled>
                                        <i class="fas fa-play pf-u-mr-sm"></i>
                                        Run Test
                                    </button>
                                    <button class="pf-c-button pf-m-secondary" type="button" id="clear-test-btn">
                                        <i class="fas fa-trash pf-u-mr-sm"></i>
                                        Clear
                                    </button>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <!-- Configuration Validation Modal -->
        <div class="pf-c-backdrop" id="config-validation-modal" hidden>
            <div class="pf-l-bullseye">
                <div class="pf-c-modal-box" role="dialog" aria-modal="true" aria-labelledby="config-validation-title">
                    <button class="pf-c-button pf-m-plain" type="button" aria-label="Close dialog" id="config-validation-close">
                        <i class="fas fa-times" aria-hidden="true"></i>
                    </button>
                    <header class="pf-c-modal-box__header">
                        <h1 class="pf-c-modal-box__title" id="config-validation-title">Configuration Validation</h1>
                    </header>
                    <div class="pf-c-modal-box__body">
                        <div class="pf-c-alert pf-m-warning">
                            <div class="pf-c-alert__icon">
                                <i class="fas fa-exclamation-triangle" aria-hidden="true"></i>
                            </div>
                            <div class="pf-c-alert__title">Configuration Issues Found</div>
                            <div class="pf-c-alert__description">
                                <ul id="config-validation-errors">
                                    <!-- Validation errors will be inserted here -->
                                </ul>
                            </div>
                        </div>
                    </div>
                    <footer class="pf-c-modal-box__footer">
                        <button class="pf-c-button pf-m-primary" type="button" id="config-validation-fix">
                            Fix Issues
                        </button>
                        <button class="pf-c-button pf-m-link" type="button" id="config-validation-cancel">
                            Cancel
                        </button>
                    </footer>
                </div>
            </div>
        </div>
    </#if>
</@layout.registrationLayout>