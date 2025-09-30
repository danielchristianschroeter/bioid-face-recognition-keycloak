<#import "template.ftl" as layout>
<@layout.mainLayout active='face-credentials' bodyClass='face-credentials'; section>

<div class="row">
    <div class="col-md-10">
        <h2>${msg("faceCredentialsTitle")}</h2>
    </div>
</div>

<div class="face-credential-section">
    <div class="face-credential-header">
        <svg class="face-credential-icon" viewBox="0 0 24 24">
            <path d="M12,2A10,10 0 0,0 2,12A10,10 0 0,0 12,22A10,10 0 0,0 22,12A10,10 0 0,0 12,2M7.07,18.28C7.5,17.38 10.12,16.5 12,16.5C13.88,16.5 16.5,17.38 16.93,18.28C15.57,19.36 13.86,20 12,20C10.14,20 8.43,19.36 7.07,18.28M18.36,16.83C16.93,15.09 13.46,14.5 12,14.5C10.54,14.5 7.07,15.09 5.64,16.83C4.62,15.5 4,13.82 4,12C4,7.59 7.59,4 12,4C16.41,4 20,7.59 20,12C20,13.82 19.38,15.5 18.36,16.83M12,6C10.06,6 8.5,7.56 8.5,9.5C8.5,11.44 10.06,13 12,13C13.94,13 15.5,11.44 15.5,9.5C15.5,7.56 13.94,6 12,6M12,11A1.5,1.5 0 0,1 10.5,9.5A1.5,1.5 0 0,1 12,8A1.5,1.5 0 0,1 13.5,9.5A1.5,1.5 0 0,1 12,11Z"/>
        </svg>
        <h3 class="face-credential-title">${msg("faceRecognition")}</h3>
    </div>

    <#if faceCredentials?has_content>
        <div class="face-credential-status active">
            <span>${msg("faceCredentialActive")}</span>
        </div>
        
        <div class="face-credential-info">
            <p>${msg("faceCredentialInfo")}</p>
            <div class="security-level security-level-high">
                <div class="security-level-indicator"></div>
                <span>${msg("securityLevelHigh")}</span>
            </div>
            
            <#list faceCredentials as credential>
                <div class="credential-details">
                    <p><strong>${msg("enrolledDate")}:</strong> ${credential.createdDate?string("yyyy-MM-dd HH:mm")}</p>
                    <p><strong>${msg("lastUsed")}:</strong> 
                        <#if credential.lastUsed??>
                            ${credential.lastUsed?string("yyyy-MM-dd HH:mm")}
                        <#else>
                            ${msg("never")}
                        </#if>
                    </p>
                    <p><strong>${msg("expiresOn")}:</strong> ${credential.expiryDate?string("yyyy-MM-dd")}</p>
                </div>
            </#list>
        </div>

        <div class="face-credential-actions">
            <form action="${url.accountUrl}/face-credentials/re-enroll" method="post" class="form-actions">
                <input type="hidden" name="stateChecker" value="${stateChecker}">
                <button type="submit" class="btn btn-primary">${msg("reEnrollFace")}</button>
            </form>
            
            <form action="${url.accountUrl}/face-credentials/disable" method="post" class="form-actions" 
                  onsubmit="return confirm('${msg("confirmDisableFaceAuth")}')">
                <input type="hidden" name="stateChecker" value="${stateChecker}">
                <button type="submit" class="btn btn-secondary">${msg("disableFaceAuth")}</button>
            </form>
            
            <form action="${url.accountUrl}/face-credentials/delete" method="post" class="form-actions"
                  onsubmit="return confirm('${msg("confirmDeleteFaceData")}')">
                <input type="hidden" name="stateChecker" value="${stateChecker}">
                <button type="submit" class="btn btn-danger">${msg("deleteFaceData")}</button>
            </form>
        </div>
    <#else>
        <div class="face-credential-status inactive">
            <span>${msg("faceCredentialInactive")}</span>
        </div>
        
        <div class="face-credential-info">
            <p>${msg("faceCredentialNotEnrolled")}</p>
            <div class="security-level security-level-low">
                <div class="security-level-indicator"></div>
                <span>${msg("securityLevelLow")}</span>
            </div>
        </div>

        <div class="face-credential-actions">
            <form action="${url.accountUrl}/face-credentials/enroll" method="post" class="form-actions">
                <input type="hidden" name="stateChecker" value="${stateChecker}">
                <button type="submit" class="btn btn-primary">${msg("enrollFace")}</button>
            </form>
        </div>
    </#if>
</div>

<div class="face-credential-section">
    <h4>${msg("faceAuthSettings")}</h4>
    
    <form action="${url.accountUrl}/face-credentials/settings" method="post" class="form-horizontal">
        <input type="hidden" name="stateChecker" value="${stateChecker}">
        
        <div class="form-group">
            <div class="col-sm-2 col-md-2">
                <label for="faceAuthEnabled" class="control-label">${msg("enableFaceAuth")}</label>
            </div>
            <div class="col-sm-10 col-md-10">
                <input type="checkbox" id="faceAuthEnabled" name="faceAuthEnabled" 
                       <#if faceAuthEnabled>checked</#if>
                       onchange="this.form.submit()">
                <span class="help-block">${msg("enableFaceAuthHelp")}</span>
            </div>
        </div>
        
        <div class="form-group">
            <div class="col-sm-2 col-md-2">
                <label for="requireFaceAuth" class="control-label">${msg("requireFaceAuth")}</label>
            </div>
            <div class="col-sm-10 col-md-10">
                <input type="checkbox" id="requireFaceAuth" name="requireFaceAuth" 
                       <#if requireFaceAuth>checked</#if>
                       <#if !faceAuthEnabled>disabled</#if>
                       onchange="this.form.submit()">
                <span class="help-block">${msg("requireFaceAuthHelp")}</span>
            </div>
        </div>
        
        <div class="form-group">
            <div class="col-sm-2 col-md-2">
                <label for="fallbackEnabled" class="control-label">${msg("allowFallback")}</label>
            </div>
            <div class="col-sm-10 col-md-10">
                <input type="checkbox" id="fallbackEnabled" name="fallbackEnabled" 
                       <#if fallbackEnabled>checked</#if>
                       <#if !faceAuthEnabled>disabled</#if>
                       onchange="this.form.submit()">
                <span class="help-block">${msg("allowFallbackHelp")}</span>
            </div>
        </div>
    </form>
</div>

</@layout.mainLayout>