package com.bioid.keycloak.failedauth.exception;

/**
 * Base exception for failed authentication storage operations.
 * 
 * This is a RuntimeException to avoid cluttering method signatures
 * with checked exception declarations throughout the codebase.
 */
public class FailedAuthStorageException extends RuntimeException {
    
    public FailedAuthStorageException(String message) {
        super(message);
    }
    
    public FailedAuthStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
