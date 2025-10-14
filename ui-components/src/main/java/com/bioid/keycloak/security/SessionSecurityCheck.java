package com.bioid.keycloak.security;

/**
 * Result of a session security validation check.
 */
public class SessionSecurityCheck {
    
    private final boolean valid;
    private final String reason;

    public SessionSecurityCheck(boolean valid, String reason) {
        this.valid = valid;
        this.reason = reason;
    }

    public boolean isValid() {
        return valid;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public String toString() {
        return String.format("SessionSecurityCheck{valid=%s, reason='%s'}", valid, reason);
    }
}