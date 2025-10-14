package com.bioid.keycloak.client.admin.model;

import java.time.Instant;

/**
 * Result of generating an enrollment link for a user.
 */
public class EnrollmentLinkResult {
    private final String enrollmentUrl;
    private final String token;
    private final Instant expiresAt;
    private final boolean requiresAdminApproval;

    public EnrollmentLinkResult(String enrollmentUrl, String token, Instant expiresAt, boolean requiresAdminApproval) {
        this.enrollmentUrl = enrollmentUrl;
        this.token = token;
        this.expiresAt = expiresAt;
        this.requiresAdminApproval = requiresAdminApproval;
    }

    public String getEnrollmentUrl() {
        return enrollmentUrl;
    }

    public String getToken() {
        return token;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean isRequiresAdminApproval() {
        return requiresAdminApproval;
    }
}