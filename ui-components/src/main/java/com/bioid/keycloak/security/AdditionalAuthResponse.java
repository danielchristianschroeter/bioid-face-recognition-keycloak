package com.bioid.keycloak.security;

/**
 * Response containing authentication data for additional verification.
 */
public class AdditionalAuthResponse {
    
    private String password;
    private String emailCode;
    private String smsCode;
    private String totpCode;
    private String approvalCode;
    private byte[] biometricData;

    public AdditionalAuthResponse() {}

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmailCode() {
        return emailCode;
    }

    public void setEmailCode(String emailCode) {
        this.emailCode = emailCode;
    }

    public String getSmsCode() {
        return smsCode;
    }

    public void setSmsCode(String smsCode) {
        this.smsCode = smsCode;
    }

    public String getTotpCode() {
        return totpCode;
    }

    public void setTotpCode(String totpCode) {
        this.totpCode = totpCode;
    }

    public String getApprovalCode() {
        return approvalCode;
    }

    public void setApprovalCode(String approvalCode) {
        this.approvalCode = approvalCode;
    }

    public byte[] getBiometricData() {
        return biometricData;
    }

    public void setBiometricData(byte[] biometricData) {
        this.biometricData = biometricData;
    }

    /**
     * Checks if the response contains data for the specified authentication method.
     * 
     * @param method The authentication method to check
     * @return true if response contains data for the method
     */
    public boolean hasDataForMethod(AuthMethod method) {
        switch (method) {
            case PASSWORD_CONFIRMATION:
                return password != null && !password.trim().isEmpty();
            case EMAIL_VERIFICATION:
                return emailCode != null && !emailCode.trim().isEmpty();
            case SMS_VERIFICATION:
                return smsCode != null && !smsCode.trim().isEmpty();
            case TOTP_VERIFICATION:
                return totpCode != null && !totpCode.trim().isEmpty();
            case ADMIN_APPROVAL:
                return approvalCode != null && !approvalCode.trim().isEmpty();
            case BIOMETRIC_VERIFICATION:
                return biometricData != null && biometricData.length > 0;
            default:
                return false;
        }
    }

    /**
     * Clears sensitive data from the response.
     */
    public void clearSensitiveData() {
        this.password = null;
        this.emailCode = null;
        this.smsCode = null;
        this.totpCode = null;
        this.approvalCode = null;
        this.biometricData = null;
    }

    @Override
    public String toString() {
        return String.format("AdditionalAuthResponse{hasPassword=%s, hasEmailCode=%s, hasSmsCode=%s, " +
                           "hasTotpCode=%s, hasApprovalCode=%s, hasBiometricData=%s}", 
                           password != null, emailCode != null, smsCode != null, 
                           totpCode != null, approvalCode != null, biometricData != null);
    }
}