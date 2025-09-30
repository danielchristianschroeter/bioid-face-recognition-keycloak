<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('username','password') displayInfo=true; section>
    <#if section = "header">
        ${msg("face-enroll.title")}
    <#elseif section = "form">
        <div class="enrollment-success">
            <div class="success-icon">
                <svg width="64" height="64" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <circle cx="12" cy="12" r="10" stroke="#28a745" stroke-width="2" fill="#d4edda"/>
                    <path d="m9 12 2 2 4-4" stroke="#28a745" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
            </div>
            
            <h2 class="success-title">${msg("face-enroll.success.title")}</h2>
            <p class="success-message">${msg("face-enroll.success.message")}</p>
            
            <div class="success-details">
                <div class="detail-item">
                    <strong>${msg("face-enroll.success.verification")}</strong>
                    <span class="verification-status verified">${msg("face-enroll.success.verified")}</span>
                </div>
                
                <div class="detail-item">
                    <strong>${msg("face-enroll.success.security")}</strong>
                    <span class="security-level high">${msg("face-enroll.success.high-security")}</span>
                </div>
            </div>
            
            <div class="success-actions">
                <a href="/realms/${realm.name}/account" class="btn btn-primary">
                    ${msg("face-enroll.success.go-to-account")}
                </a>
                
                <a href="/realms/${realm.name}/protocol/openid-connect/logout" class="btn btn-secondary">
                    ${msg("face-enroll.success.logout")}
                </a>
            </div>
        </div>
        
        <style>
            .enrollment-success {
                text-align: center;
                padding: 2rem;
                max-width: 500px;
                margin: 0 auto;
            }
            
            .success-icon {
                margin-bottom: 1.5rem;
            }
            
            .success-title {
                color: #28a745;
                margin-bottom: 1rem;
                font-size: 1.5rem;
            }
            
            .success-message {
                color: #666;
                margin-bottom: 2rem;
                font-size: 1.1rem;
                line-height: 1.5;
            }
            
            .success-details {
                background: #f8f9fa;
                border-radius: 8px;
                padding: 1.5rem;
                margin-bottom: 2rem;
                text-align: left;
            }
            
            .detail-item {
                display: flex;
                justify-content: space-between;
                align-items: center;
                margin-bottom: 1rem;
            }
            
            .detail-item:last-child {
                margin-bottom: 0;
            }
            
            .verification-status.verified {
                color: #28a745;
                font-weight: 600;
            }
            
            .security-level.high {
                color: #28a745;
                font-weight: 600;
            }
            
            .success-actions {
                display: flex;
                gap: 1rem;
                justify-content: center;
                flex-wrap: wrap;
            }
            
            .btn {
                padding: 0.75rem 1.5rem;
                border-radius: 4px;
                text-decoration: none;
                font-weight: 500;
                transition: all 0.2s;
                border: none;
                cursor: pointer;
            }
            
            .btn-primary {
                background-color: #0066cc;
                color: white;
            }
            
            .btn-primary:hover {
                background-color: #0052a3;
            }
            
            .btn-secondary {
                background-color: #6c757d;
                color: white;
            }
            
            .btn-secondary:hover {
                background-color: #545b62;
            }
            
            @media (max-width: 768px) {
                .success-actions {
                    flex-direction: column;
                }
                
                .btn {
                    width: 100%;
                }
            }
        </style>
    </#if>
</@layout.registrationLayout>