package com.bioid.keycloak.failedauth.exception;

/**
 * Exception thrown when enrollment of failed attempt images fails.
 */
public class EnrollmentException extends FailedAuthStorageException {
    
    private final String errorCode;
    
    public EnrollmentException(String message) {
        super(message);
        this.errorCode = "ENROLLMENT_FAILED";
    }
    
    public EnrollmentException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public EnrollmentException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "ENROLLMENT_FAILED";
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}
