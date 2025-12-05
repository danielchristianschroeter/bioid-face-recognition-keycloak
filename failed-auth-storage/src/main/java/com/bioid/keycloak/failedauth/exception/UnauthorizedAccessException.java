package com.bioid.keycloak.failedauth.exception;

/**
 * Exception thrown when a user attempts to access another user's failed attempts.
 */
public class UnauthorizedAccessException extends FailedAuthStorageException {
    
    public UnauthorizedAccessException(String message) {
        super(message);
    }
    
    public UnauthorizedAccessException(String userId, String attemptId) {
        super(String.format("User %s is not authorized to access attempt %s", userId, attemptId));
    }
}
