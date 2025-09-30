<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=false; section>
    <#if section = "header">
        <h1 id="kc-page-title">${msg("errorTitle")}</h1>
    <#elseif section = "form">
        <div id="kc-form">
            <div id="kc-form-wrapper">
                <div class="alert alert-error" role="alert">
                    <span class="pficon pficon-error-circle-o"></span>
                    <span class="kc-feedback-text">
                        <#if message?has_content>
                            ${kcSanitize(message.summary)?no_esc}
                        <#else>
                            ${msg("unexpectedErrorMessage")}
                        </#if>
                    </span>
                </div>
                
                <div class="error-details">
                    <#if client??>
                        <p><strong>Client:</strong> ${client.clientId}</p>
                    </#if>
                    <#if realm??>
                        <p><strong>Realm:</strong> ${realm.name}</p>
                    </#if>
                </div>
                
                <div class="error-actions">
                    <#if url.loginUrl??>
                        <a href="${url.loginUrl}" class="btn btn-primary">
                            ${msg("backToLogin")}
                        </a>
                    </#if>
                </div>
            </div>
        </div>
        
        <style>
            #kc-form {
                max-width: 500px;
                margin: 2rem auto;
                padding: 1rem;
            }
            
            #kc-form-wrapper {
                background: #fff;
                border-radius: 8px;
                box-shadow: 0 2px 8px rgba(0,0,0,0.1);
                padding: 2rem;
            }
            
            #kc-page-title {
                text-align: center;
                color: #dc3545;
                margin-bottom: 1.5rem;
            }
            
            .alert-error {
                padding: 1rem;
                background-color: #f8d7da;
                border: 1px solid #f5c6cb;
                border-radius: 6px;
                color: #721c24;
                margin-bottom: 1.5rem;
            }
            
            .alert-error .pficon {
                margin-right: 0.5rem;
            }
            
            .error-details {
                background: #f8f9fa;
                padding: 1rem;
                border-radius: 6px;
                margin-bottom: 1.5rem;
                font-size: 0.9rem;
            }
            
            .error-details p {
                margin: 0.25rem 0;
            }
            
            .error-actions {
                text-align: center;
            }
            
            .btn {
                display: inline-block;
                padding: 0.75rem 1.5rem;
                background-color: #007bff;
                color: white;
                text-decoration: none;
                border-radius: 6px;
                font-weight: 500;
                transition: background-color 0.2s;
            }
            
            .btn:hover {
                background-color: #0056b3;
                color: white;
                text-decoration: none;
            }
        </style>
    </#if>
</@layout.registrationLayout>