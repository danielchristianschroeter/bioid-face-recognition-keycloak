# Failed Authentication Storage Module

## Overview

This module provides storage and management of failed authentication attempts, allowing users to review and selectively enroll failed images to improve their biometric templates.

## Features

- **Automatic Storage**: Failed authentication attempts are automatically captured and stored
- **Database-Backed**: Uses PostgreSQL for cluster/failover support
- **Encrypted Storage**: Images encrypted at rest using AES-256-GCM
- **User Self-Service**: Users can review and enroll their own failed attempts
- **Admin Access**: Admins can view all attempts with full audit logging
- **Configurable Retention**: Auto-cleanup after configurable period (default: 30 days)
- **Quality Indicators**: Shows why authentication failed and image quality scores
- **Audit Trail**: Complete logging of all operations

## Architecture

### Entities (JPA)
- `FailedAuthAttemptEntity` - Main attempt metadata
- `FailedAuthImageEntity` - Image data and properties
- `FailedAuthAuditLogEntity` - Audit trail
- `FailedAuthUserPreferencesEntity` - User preferences
- `FailedAuthRealmConfigEntity` - Realm-level configuration

### Services
- `FailedAuthImageStorageService` - Core business logic
- `EncryptionService` - Image encryption/decryption
- `ImageProcessingService` - Thumbnail generation, compression
- `FailedAuthNotificationService` - Email notifications

### Configuration
All configuration via environment variables (see `.env` file):
- `FAILED_AUTH_STORAGE_ENABLED` - Enable/disable feature
- `FAILED_AUTH_RETENTION_DAYS` - Retention period (default: 30)
- `FAILED_AUTH_ENCRYPT_IMAGES` - Encrypt images (default: true)
- ... and 30+ more options

## Database Schema

The module uses 5 tables:
1. `failed_auth_attempts` - Attempt metadata
2. `failed_auth_images` - Image data (BLOB)
3. `failed_auth_audit_log` - Audit trail
4. `failed_auth_user_preferences` - User settings
5. `failed_auth_realm_config` - Realm configuration

See `docs/FAILED_AUTH_IMPLEMENTATION_GUIDE.md` for complete schema.

## Usage

### Automatic Capture (in FaceAuthenticator)
```java
// On authentication failure
if (config.isStorageEnabled()) {
    String attemptId = failedAuthStorageService.storeFailedAttempt(
        context,
        images,
        verificationResult,
        livenessResult
    );
}
```

### User Retrieval
```java
// Get user's failed attempts
PagedResult<FailedAttemptSummary> attempts = 
    failedAuthStorageService.getFailedAttempts(user, filter, pagination);
```

### Enrollment
```java
// Enroll selected images
EnrollmentResult result = failedAuthStorageService.enrollFailedAttempt(
    session, realm, user, attemptId, request
);
```

## Security

- **Access Control**: Users can only access their own attempts
- **Encryption**: AES-256-GCM for images at rest
- **Audit Logging**: All operations logged
- **Rate Limiting**: Prevents abuse
- **Integrity Verification**: SHA-256 checksums

## Configuration Example

```bash
# Enable feature
FAILED_AUTH_STORAGE_ENABLED=true

# Retention
FAILED_AUTH_RETENTION_DAYS=30
FAILED_AUTH_MAX_ATTEMPTS_PER_USER=20

# Quality thresholds
FAILED_AUTH_MIN_QUALITY_SCORE=0.65
FAILED_AUTH_MIN_ENROLL_QUALITY_SCORE=0.70

# Security
FAILED_AUTH_ENCRYPT_IMAGES=true
FAILED_AUTH_VERIFY_INTEGRITY=true

# Notifications
FAILED_AUTH_NOTIFICATION_ENABLED=true
FAILED_AUTH_NOTIFICATION_THRESHOLD=3
```

## API Endpoints

### User Endpoints
- `GET /realms/{realm}/account/face-auth/failed-attempts` - List attempts
- `GET /realms/{realm}/account/face-auth/failed-attempts/{id}` - Get details
- `GET /realms/{realm}/account/face-auth/failed-attempts/{id}/thumbnail/{index}` - Get thumbnail
- `POST /realms/{realm}/account/face-auth/failed-attempts/{id}/enroll` - Enroll images
- `DELETE /realms/{realm}/account/face-auth/failed-attempts/{id}` - Delete attempt

### Admin Endpoints
- `GET /realms/{realm}/bws-admin/failed-attempts` - List all attempts
- `GET /realms/{realm}/bws-admin/failed-attempts/user/{userId}` - User's attempts
- `GET /realms/{realm}/bws-admin/failed-attempts/statistics` - Statistics

## Testing

```bash
# Run unit tests
mvn test -pl failed-auth-storage

# Run integration tests
mvn verify -pl failed-auth-storage
```

## Dependencies

- Keycloak 26.4+
- PostgreSQL 17+
- Java 21+
- Jakarta Persistence API
- Jackson (JSON)
- Bouncy Castle (encryption)
- imgscalr (image processing)

## Implementation Status

✅ **Completed**:
- Module structure
- Configuration system
- Exception hierarchy
- JPA entities (3/5)
- Documentation

🚧 **In Progress**:
- Remaining JPA entities (2/5)
- Core services
- REST API endpoints

📋 **TODO**:
- UI components
- Email notifications
- Cleanup jobs
- Integration tests

## License

Same as parent project.

## Support

See main project documentation for support information.
