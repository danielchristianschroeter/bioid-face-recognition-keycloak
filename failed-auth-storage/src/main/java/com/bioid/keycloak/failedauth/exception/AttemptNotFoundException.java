package com.bioid.keycloak.failedauth.exception;

/**
 * Exception thrown when a failed authentication attempt is not found.
 */
public class AttemptNotFoundException extends FailedAuthStorageException {
    
    public AttemptNotFoundException(String attemptId) {
        super("Failed authentication attempt not found: " + attemptId);
    }
}
