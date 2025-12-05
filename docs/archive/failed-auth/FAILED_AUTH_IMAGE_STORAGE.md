# Failed Authentication Image Storage & Template Improvement

## Overview

This feature allows users to review failed authentication attempts and selectively add those images to their biometric template to improve future authentication accuracy. This creates a self-service template improvement workflow that:

- **Reduces friction**: Users can fix authentication issues themselves
- **Improves accuracy**: Templates become more robust with diverse training data
- **Increases transparency**: Users understand why authentication failed
- **Enhances security**: Only high-quality, verified images are added
- **Respects privacy**: Users control their biometric data

## Business Value

- **Reduced Support Costs**: 60-80% fewer "can't login" support tickets
- **Better User Experience**: Self-service resolution instead of admin intervention
- **Higher Adoption**: Users trust systems they can control
- **Continuous Improvement**: Templates get better over time automatically
- **Compliance**: Clear audit trail of all biometric data changes

## Architecture

### 1. **Failed Image Storage System**

#### Storage Structure
```
/opt/keycloak/failed-auth-images/
├── {realm}/
│   ├── {username}/
│   │   ├── {timestamp}-{attempt-id}/
│   │   │   ├── image1.jpg
│   │   │   ├── image2.jpg (if liveness enabled)
│   │   │   └── metadata.json
```

#### Metadata Format
```json
{
  "attemptId": "uuid-v4",
  "version": "1.0",
  "userId": "keycloak-user-id",
  "username": "john.doe",
  "classId": 12345,
  "timestamp": "2025-01-18T20:30:00Z",
  "ipAddress": "192.168.1.100",
  "userAgent": "Mozilla/5.0...",
  "sessionId": "keycloak-session-id",
  "realmId": "bioid-demo",
  
  "failureDetails": {
    "reason": "VERIFICATION_FAILED",
    "verificationScore": 0.025,
    "threshold": 0.015,
    "scoreDifference": 0.010,
    "livenessMode": "active",
    "livenessScore": 0.85,
    "livenessThreshold": 0.5,
    "livenessPassed": true,
    "challengeDirection": "LEFT",
    "retryAttempt": 2,
    "maxRetries": 3
  },
  
  "imageCount": 2,
  "imageProperties": [
    {
      "imageIndex": 0,
      "fileName": "image1.jpg",
      "fileSize": 45678,
      "width": 640,
      "height": 480,
      "format": "JPEG",
      "captureTimestamp": "2025-01-18T20:30:00.123Z",
      "faceFound": true,
      "faceCount": 1,
      "faceQuality": 0.92,
      "eyesVisible": true,
      "mouthVisible": true,
      "faceAngle": {
        "yaw": 2.5,
        "pitch": -1.2,
        "roll": 0.8
      },
      "qualityAssessments": {
        "sharpness": "PASSED",
        "brightness": "PASSED",
        "contrast": "PASSED",
        "faceSize": "PASSED",
        "eyeDistance": "PASSED",
        "backgroundUniformity": "PASSED"
      },
      "qualityScore": 0.92,
      "recommendedForEnrollment": true,
      "featureVectorsExtracted": 2
    }
  ],
  
  "enrollmentStatus": {
    "enrolled": false,
    "enrolledAt": null,
    "enrolledBy": null,
    "enrolledImages": [],
    "enrollmentResult": null
  },
  
  "reviewStatus": {
    "reviewed": false,
    "reviewedAt": null,
    "reviewedBy": null,
    "reviewNotes": null
  },
  
  "retention": {
    "expiresAt": "2025-02-17T20:30:00Z",
    "retentionDays": 30,
    "autoDeleteEnabled": true
  },
  
  "security": {
    "encrypted": true,
    "encryptionAlgorithm": "AES-256-GCM",
    "checksumSHA256": "abc123...",
    "integrityVerified": true
  },
  
  "metadata": {
    "createdAt": "2025-01-18T20:30:00Z",
    "updatedAt": "2025-01-18T20:30:00Z",
    "version": 1
  }
}
```

### 2. **Database Schema** (Keycloak User Attributes)

Store references in user attributes for quick access and statistics:

```java
// User attributes for failed auth tracking
user.setAttribute("failed.auth.attempts", List.of(
    "2025-01-18T20:30:00Z:uuid1",
    "2025-01-18T21:15:00Z:uuid2"
));
user.setAttribute("failed.auth.count.total", "5");
user.setAttribute("failed.auth.count.unenrolled", "2");
user.setAttribute("failed.auth.count.enrolled", "3");
user.setAttribute("failed.auth.last.timestamp", "2025-01-18T21:15:00Z");
user.setAttribute("failed.auth.last.attemptId", "uuid2");
user.setAttribute("failed.auth.notification.sent", "true");
user.setAttribute("failed.auth.notification.lastSent", "2025-01-18T21:20:00Z");

// Statistics for analytics
user.setAttribute("failed.auth.stats.avgQuality", "0.89");
user.setAttribute("failed.auth.stats.avgScore", "0.023");
user.setAttribute("failed.auth.stats.commonReason", "VERIFICATION_FAILED");
```

**Primary Storage: Database Tables** (chosen for cluster/failover support)

```sql
-- Main table for failed authentication attempts
CREATE TABLE failed_auth_attempts (
    attempt_id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    realm_id VARCHAR(36) NOT NULL,
    username VARCHAR(255) NOT NULL,
    class_id BIGINT NOT NULL,
    
    -- Timestamp information
    timestamp TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    
    -- Failure details
    failure_reason VARCHAR(50) NOT NULL,
    verification_score DECIMAL(5,4),
    verification_threshold DECIMAL(5,4),
    score_difference DECIMAL(5,4),
    
    -- Liveness information
    liveness_mode VARCHAR(20),
    liveness_score DECIMAL(5,4),
    liveness_threshold DECIMAL(5,4),
    liveness_passed BOOLEAN,
    challenge_direction VARCHAR(10),
    
    -- Retry information
    retry_attempt INT DEFAULT 1,
    max_retries INT DEFAULT 3,
    
    -- Image information
    image_count INT NOT NULL,
    avg_quality_score DECIMAL(5,4),
    
    -- Enrollment status
    enrolled BOOLEAN DEFAULT FALSE,
    enrolled_at TIMESTAMP,
    enrolled_by VARCHAR(36),
    enrolled_image_indices TEXT, -- JSON array: [0,1]
    enrollment_result TEXT, -- JSON object
    
    -- Review status
    reviewed BOOLEAN DEFAULT FALSE,
    reviewed_at TIMESTAMP,
    reviewed_by VARCHAR(36),
    review_notes TEXT,
    
    -- Session information
    session_id VARCHAR(255),
    ip_address VARCHAR(45),
    user_agent TEXT,
    
    -- Security
    encrypted BOOLEAN DEFAULT TRUE,
    checksum_sha256 VARCHAR(64),
    integrity_verified BOOLEAN DEFAULT TRUE,
    
    -- Metadata
    metadata_json TEXT, -- Full metadata as JSON
    
    -- Indexes
    INDEX idx_user_timestamp (user_id, timestamp DESC),
    INDEX idx_realm_user (realm_id, user_id),
    INDEX idx_enrolled (user_id, enrolled),
    INDEX idx_expires (expires_at),
    INDEX idx_class_id (class_id),
    INDEX idx_reviewed (user_id, reviewed),
    
    -- Foreign key constraints
    CONSTRAINT fk_realm FOREIGN KEY (realm_id) REFERENCES REALM(ID) ON DELETE CASCADE
);

-- Table for storing images (separate for better performance)
CREATE TABLE failed_auth_images (
    image_id BIGSERIAL PRIMARY KEY,
    attempt_id VARCHAR(36) NOT NULL,
    image_index INT NOT NULL,
    
    -- Image data
    image_data BYTEA NOT NULL, -- Full image (encrypted)
    thumbnail_data BYTEA, -- Thumbnail (encrypted)
    
    -- Image properties
    file_size INT NOT NULL,
    width INT,
    height INT,
    format VARCHAR(10),
    capture_timestamp TIMESTAMP,
    
    -- Face detection results
    face_found BOOLEAN,
    face_count INT,
    face_quality DECIMAL(5,4),
    eyes_visible BOOLEAN,
    mouth_visible BOOLEAN,
    
    -- Face angles
    face_yaw DECIMAL(5,2),
    face_pitch DECIMAL(5,2),
    face_roll DECIMAL(5,2),
    
    -- Quality assessments (JSON)
    quality_assessments TEXT, -- JSON object
    quality_score DECIMAL(5,4),
    recommended_for_enrollment BOOLEAN,
    feature_vectors_extracted INT,
    
    -- Metadata
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Indexes
    INDEX idx_attempt_id (attempt_id),
    UNIQUE INDEX idx_attempt_image (attempt_id, image_index),
    
    -- Foreign key
    CONSTRAINT fk_attempt FOREIGN KEY (attempt_id) REFERENCES failed_auth_attempts(attempt_id) ON DELETE CASCADE
);

-- Table for audit logging
CREATE TABLE failed_auth_audit_log (
    log_id BIGSERIAL PRIMARY KEY,
    attempt_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    action VARCHAR(50) NOT NULL, -- VIEW, ENROLL, DELETE, ADMIN_VIEW
    performed_by VARCHAR(36) NOT NULL,
    performed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45),
    user_agent TEXT,
    details TEXT, -- JSON object with action details
    
    INDEX idx_attempt_id (attempt_id),
    INDEX idx_user_id (user_id),
    INDEX idx_performed_at (performed_at DESC),
    
    CONSTRAINT fk_attempt_audit FOREIGN KEY (attempt_id) REFERENCES failed_auth_attempts(attempt_id) ON DELETE CASCADE
);

-- Table for user preferences
CREATE TABLE failed_auth_user_preferences (
    user_id VARCHAR(36) PRIMARY KEY,
    realm_id VARCHAR(36) NOT NULL,
    
    -- Feature preferences
    storage_enabled BOOLEAN DEFAULT TRUE,
    notification_enabled BOOLEAN DEFAULT TRUE,
    notification_threshold INT DEFAULT 3,
    notification_cooldown_hours INT DEFAULT 24,
    last_notification_sent TIMESTAMP,
    
    -- Privacy preferences
    privacy_notice_accepted BOOLEAN DEFAULT FALSE,
    privacy_notice_accepted_at TIMESTAMP,
    
    -- Statistics
    total_attempts INT DEFAULT 0,
    enrolled_attempts INT DEFAULT 0,
    deleted_attempts INT DEFAULT 0,
    
    -- Metadata
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_realm_id (realm_id)
);

-- Table for realm-level configuration
CREATE TABLE failed_auth_realm_config (
    realm_id VARCHAR(36) PRIMARY KEY,
    
    -- Feature flags
    enabled BOOLEAN DEFAULT TRUE,
    admin_access_enabled BOOLEAN DEFAULT TRUE,
    notification_enabled BOOLEAN DEFAULT TRUE,
    
    -- Retention policy
    retention_days INT DEFAULT 30,
    max_attempts_per_user INT DEFAULT 20,
    auto_cleanup_enabled BOOLEAN DEFAULT TRUE,
    
    -- Quality thresholds
    min_quality_score DECIMAL(5,4) DEFAULT 0.65,
    min_enroll_quality_score DECIMAL(5,4) DEFAULT 0.70,
    
    -- Rate limits
    api_rate_limit_per_minute INT DEFAULT 30,
    enroll_rate_limit_per_hour INT DEFAULT 10,
    
    -- Notification settings
    notification_threshold INT DEFAULT 3,
    notification_cooldown_hours INT DEFAULT 24,
    notification_from_email VARCHAR(255),
    notification_template TEXT,
    
    -- Metadata
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_realm_config FOREIGN KEY (realm_id) REFERENCES REALM(ID) ON DELETE CASCADE
);

-- View for easy querying
CREATE VIEW failed_auth_attempts_summary AS
SELECT 
    fa.attempt_id,
    fa.user_id,
    fa.realm_id,
    fa.username,
    fa.timestamp,
    fa.failure_reason,
    fa.verification_score,
    fa.liveness_passed,
    fa.image_count,
    fa.avg_quality_score,
    fa.enrolled,
    fa.enrolled_at,
    fa.expires_at,
    COUNT(fai.image_id) as stored_images,
    SUM(CASE WHEN fai.recommended_for_enrollment THEN 1 ELSE 0 END) as recommended_images
FROM failed_auth_attempts fa
LEFT JOIN failed_auth_images fai ON fa.attempt_id = fai.attempt_id
GROUP BY fa.attempt_id;
```

### 3. **REST API Endpoints**

#### Get Failed Authentication Attempts
```
GET /realms/{realm}/account/face-auth/failed-attempts
Authorization: Bearer {access_token}
Query Parameters:
  - page: int (default: 0)
  - size: int (default: 20, max: 100)
  - enrolled: boolean (filter by enrollment status)
  - minQuality: float (filter by minimum quality score)
  - sort: string (timestamp_desc, timestamp_asc, quality_desc)

Response:
{
  "attempts": [
    {
      "attemptId": "uuid",
      "timestamp": "2025-01-18T20:30:00Z",
      "failureReason": "VERIFICATION_FAILED",
      "verificationScore": 0.025,
      "threshold": 0.015,
      "scoreDifference": 0.010,
      "imageCount": 2,
      "avgQualityScore": 0.92,
      "livenessMode": "active",
      "livenessPassed": true,
      "enrolled": false,
      "canEnroll": true,
      "recommendedForEnrollment": true,
      "expiresAt": "2025-02-17T20:30:00Z",
      "daysUntilExpiry": 29
    }
  ],
  "pagination": {
    "page": 0,
    "size": 20,
    "totalElements": 5,
    "totalPages": 1,
    "hasNext": false,
    "hasPrevious": false
  },
  "statistics": {
    "totalCount": 5,
    "enrolledCount": 2,
    "unenrolledCount": 3,
    "recommendedCount": 3,
    "avgQualityScore": 0.89,
    "avgVerificationScore": 0.023,
    "mostCommonReason": "VERIFICATION_FAILED"
  }
}
```

#### Get Failed Attempt Details
```
GET /realms/{realm}/account/face-auth/failed-attempts/{attemptId}
Authorization: Bearer {access_token}

Response:
{
  "attemptId": "uuid",
  "timestamp": "2025-01-18T20:30:00Z",
  "failureReason": "VERIFICATION_FAILED",
  "verificationScore": 0.025,
  "threshold": 0.015,
  "images": [
    {
      "imageIndex": 0,
      "thumbnailUrl": "/realms/{realm}/account/face-auth/failed-attempts/{attemptId}/thumbnail/0",
      "faceQuality": 0.92,
      "qualityAssessments": {...}
    }
  ],
  "enrolled": false,
  "metadata": {...}
}
```

#### Get Image Thumbnail
```
GET /realms/{realm}/account/face-auth/failed-attempts/{attemptId}/thumbnail/{imageIndex}
Authorization: Bearer {access_token}

Response: image/jpeg (base64 or binary)
```

#### Enroll Failed Attempt Images
```
POST /realms/{realm}/account/face-auth/failed-attempts/{attemptId}/enroll
Authorization: Bearer {access_token}
Content-Type: application/json

Request:
{
  "imageIndices": [0, 1],     // Which images to include (required)
  "updateTemplate": true,      // Update existing or create new (default: true)
  "verifyQuality": true,       // Re-verify quality before enrollment (default: true)
  "minQualityScore": 0.7,      // Minimum quality threshold (default: from config)
  "notes": "Adding images from different lighting conditions"  // Optional user notes
}

Response:
{
  "success": true,
  "attemptId": "uuid",
  "performedAction": "TEMPLATE_UPDATED",
  "enrolledImages": 2,
  "skippedImages": 0,
  "newFeatureVectors": 4,
  "totalFeatureVectors": 12,
  "previousEncoderVersion": 8,
  "currentEncoderVersion": 8,
  "templateStatus": {
    "classId": 12345,
    "available": true,
    "encoderVersion": 8,
    "featureVectors": 12,
    "thumbnailsStored": true,
    "lastUpdated": "2025-01-18T20:35:00Z"
  },
  "qualityImprovement": {
    "before": {
      "avgQuality": 0.85,
      "featureVectors": 8
    },
    "after": {
      "avgQuality": 0.88,
      "featureVectors": 12
    },
    "improvement": "+3.5%"
  },
  "warnings": [],
  "auditLog": {
    "action": "TEMPLATE_UPDATED_FROM_FAILED_AUTH",
    "timestamp": "2025-01-18T20:35:00Z",
    "userId": "user-id",
    "attemptId": "uuid"
  }
}

Error Response (400 Bad Request):
{
  "success": false,
  "error": "QUALITY_TOO_LOW",
  "message": "Selected images do not meet minimum quality requirements",
  "details": {
    "imageIndex": 0,
    "qualityScore": 0.65,
    "minRequired": 0.70,
    "failedChecks": ["sharpness", "brightness"]
  }
}
```

#### Delete Failed Attempt
```
DELETE /realms/{realm}/account/face-auth/failed-attempts/{attemptId}
Authorization: Bearer {access_token}

Response:
{
  "success": true,
  "message": "Failed attempt deleted"
}
```

#### Bulk Operations
```
POST /realms/{realm}/account/face-auth/failed-attempts/bulk-enroll
Authorization: Bearer {access_token}
Content-Type: application/json

Request:
{
  "attemptIds": ["uuid1", "uuid2"],
  "imageSelections": {
    "uuid1": [0, 1],
    "uuid2": [0]
  }
}

DELETE /realms/{realm}/account/face-auth/failed-attempts/bulk-delete
Authorization: Bearer {access_token}
Content-Type: application/json

Request:
{
  "attemptIds": ["uuid1", "uuid2"],
  "deleteAll": false
}
```

### 4. **Configuration**

Add to `bioid.properties` / environment variables:

```properties
# ============================================================================
# Failed Authentication Image Storage Configuration
# ============================================================================

# Enable/disable failed auth image storage
failed.auth.storage.enabled=true

# Storage location (must be writable by Keycloak process)
failed.auth.storage.path=/opt/keycloak/failed-auth-images

# Retention policy
failed.auth.storage.retention.days=30
failed.auth.storage.max.attempts.per.user=20
failed.auth.storage.auto.delete.enabled=true

# Quality thresholds
failed.auth.storage.min.quality.score=0.65
failed.auth.storage.min.verification.score.diff=0.005
failed.auth.storage.require.liveness.pass=false

# Image processing
failed.auth.storage.include.thumbnails=true
failed.auth.storage.thumbnail.size=300
failed.auth.storage.thumbnail.quality=85
failed.auth.storage.compress.images=true
failed.auth.storage.max.image.size.mb=5

# Security
failed.auth.storage.encrypt=true
failed.auth.storage.encryption.algorithm=AES-256-GCM
failed.auth.storage.verify.integrity=true
failed.auth.storage.secure.delete=true

# Auto-cleanup
failed.auth.cleanup.enabled=true
failed.auth.cleanup.interval.hours=24
failed.auth.cleanup.batch.size=100

# Enrollment settings
failed.auth.enroll.min.quality.score=0.70
failed.auth.enroll.verify.before.enroll=true
failed.auth.enroll.max.images.per.request=10
failed.auth.enroll.require.user.confirmation=true

# Notifications
failed.auth.notification.enabled=true
failed.auth.notification.threshold=3
failed.auth.notification.cooldown.hours=24
failed.auth.notification.method=email

# Analytics and monitoring
failed.auth.analytics.enabled=true
failed.auth.analytics.track.patterns=true
failed.auth.analytics.alert.on.anomalies=true

# Rate limiting
failed.auth.api.rate.limit.enabled=true
failed.auth.api.rate.limit.requests.per.minute=30
failed.auth.api.rate.limit.enroll.per.hour=10

# Audit logging
failed.auth.audit.enabled=true
failed.auth.audit.log.level=INFO
failed.auth.audit.include.images.metadata=true
```

**Environment Variable Mapping:**
```bash
FAILED_AUTH_STORAGE_ENABLED=true
FAILED_AUTH_STORAGE_PATH=/opt/keycloak/failed-auth-images
FAILED_AUTH_STORAGE_RETENTION_DAYS=30
FAILED_AUTH_STORAGE_ENCRYPT=true
# ... etc
```

### 5. **Implementation Components**

#### A. FailedAuthImageStorageService
```java
/**
 * Service for managing failed authentication image storage and enrollment.
 * 
 * This service handles:
 * - Storing failed authentication attempts with images and metadata
 * - Retrieving and filtering failed attempts
 * - Enrolling selected images to improve templates
 * - Cleanup and retention management
 * - Security and encryption
 */
public class FailedAuthImageStorageService {
    
    private static final Logger logger = LoggerFactory.getLogger(FailedAuthImageStorageService.class);
    
    private final FailedAuthConfiguration config;
    private final EncryptionService encryptionService;
    private final ImageProcessingService imageProcessingService;
    private final AuditService auditService;
    
    /**
     * Store a failed authentication attempt with images and metadata.
     * 
     * @param context Authentication flow context
     * @param images List of base64-encoded images
     * @param verificationResult Verification result from BWS
     * @param livenessResult Liveness detection result
     * @return Attempt ID (UUID)
     * @throws StorageException if storage fails
     */
    public String storeFailedAttempt(
        AuthenticationFlowContext context,
        List<String> images,
        VerificationResult verificationResult,
        LivenessResult livenessResult
    ) throws StorageException;
    
    /**
     * Get paginated list of failed attempts for a user.
     * 
     * @param user User model
     * @param filter Filter criteria (enrolled, quality, date range)
     * @param pagination Pagination parameters
     * @return Paginated list of attempt summaries
     */
    public PagedResult<FailedAttemptSummary> getFailedAttempts(
        UserModel user,
        FailedAttemptFilter filter,
        PaginationParams pagination
    );
    
    /**
     * Get detailed information about a specific failed attempt.
     * 
     * @param user User model
     * @param attemptId Attempt UUID
     * @return Detailed attempt information
     * @throws NotFoundException if attempt not found
     * @throws UnauthorizedException if user doesn't own attempt
     */
    public FailedAttemptDetails getAttemptDetails(
        UserModel user, 
        String attemptId
    ) throws NotFoundException, UnauthorizedException;
    
    /**
     * Get thumbnail image for a specific attempt.
     * 
     * @param user User model
     * @param attemptId Attempt UUID
     * @param imageIndex Image index (0-based)
     * @return Thumbnail image bytes (JPEG)
     * @throws NotFoundException if image not found
     */
    public byte[] getThumbnail(
        UserModel user, 
        String attemptId, 
        int imageIndex
    ) throws NotFoundException;
    
    /**
     * Get full-size image for a specific attempt.
     * 
     * @param user User model
     * @param attemptId Attempt UUID
     * @param imageIndex Image index (0-based)
     * @return Full image bytes
     * @throws NotFoundException if image not found
     */
    public byte[] getFullImage(
        UserModel user, 
        String attemptId, 
        int imageIndex
    ) throws NotFoundException;
    
    /**
     * Enroll selected images from a failed attempt to improve template.
     * 
     * @param session Keycloak session
     * @param realm Realm model
     * @param user User model
     * @param attemptId Attempt UUID
     * @param request Enrollment request with image indices and options
     * @return Enrollment result with template status
     * @throws EnrollmentException if enrollment fails
     */
    public EnrollmentResult enrollFailedAttempt(
        KeycloakSession session,
        RealmModel realm,
        UserModel user, 
        String attemptId, 
        EnrollmentRequest request
    ) throws EnrollmentException;
    
    /**
     * Bulk enroll multiple failed attempts.
     * 
     * @param session Keycloak session
     * @param realm Realm model
     * @param user User model
     * @param requests List of enrollment requests
     * @return Bulk enrollment result
     */
    public BulkEnrollmentResult bulkEnroll(
        KeycloakSession session,
        RealmModel realm,
        UserModel user,
        List<EnrollmentRequest> requests
    );
    
    /**
     * Delete a failed attempt and its images.
     * 
     * @param user User model
     * @param attemptId Attempt UUID
     * @throws NotFoundException if attempt not found
     */
    public void deleteFailedAttempt(
        UserModel user, 
        String attemptId
    ) throws NotFoundException;
    
    /**
     * Bulk delete multiple failed attempts.
     * 
     * @param user User model
     * @param attemptIds List of attempt UUIDs
     * @return Number of deleted attempts
     */
    public int bulkDelete(
        UserModel user, 
        List<String> attemptIds
    );
    
    /**
     * Cleanup old attempts based on retention policy.
     * 
     * @param retentionDays Number of days to retain
     * @return Number of cleaned up attempts
     */
    public int cleanupOldAttempts(int retentionDays);
    
    /**
     * Get statistics about failed attempts for a user.
     * 
     * @param user User model
     * @return Statistics summary
     */
    public FailedAttemptStatistics getStatistics(UserModel user);
    
    /**
     * Verify integrity of stored attempt data.
     * 
     * @param attemptId Attempt UUID
     * @return true if integrity check passes
     */
    public boolean verifyIntegrity(String attemptId);
    
    /**
     * Check if user should be notified about failed attempts.
     * 
     * @param user User model
     * @return true if notification should be sent
     */
    public boolean shouldNotifyUser(UserModel user);
    
    /**
     * Mark user as notified about failed attempts.
     * 
     * @param user User model
     */
    public void markUserNotified(UserModel user);
}
```

#### B. Modify FaceAuthenticator
```java
private void handleFailure(AuthenticationFlowContext context, String errorMessage) {
    // ... existing retry logic ...
    
    // Store failed attempt if enabled
    if (isFailedAuthStorageEnabled()) {
        try {
            String attemptId = failedAuthStorageService.storeFailedAttempt(
                context.getUser(),
                extractImages(context),
                verificationResult,
                livenessResult
            );
            logger.info("Stored failed auth attempt: {}", attemptId);
        } catch (Exception e) {
            logger.error("Failed to store failed auth attempt", e);
        }
    }
    
    // ... rest of failure handling ...
}
```

#### C. Account Console REST Resource
```java
@Path("/face-auth")
public class FaceAuthAccountResource {
    
    @GET
    @Path("/failed-attempts")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFailedAttempts(@Context KeycloakSession session);
    
    @GET
    @Path("/failed-attempts/{attemptId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAttemptDetails(
        @PathParam("attemptId") String attemptId,
        @Context KeycloakSession session
    );
    
    @GET
    @Path("/failed-attempts/{attemptId}/thumbnail/{imageIndex}")
    @Produces("image/jpeg")
    public Response getThumbnail(
        @PathParam("attemptId") String attemptId,
        @PathParam("imageIndex") int imageIndex,
        @Context KeycloakSession session
    );
    
    @POST
    @Path("/failed-attempts/{attemptId}/enroll")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response enrollFailedAttempt(
        @PathParam("attemptId") String attemptId,
        EnrollRequest request,
        @Context KeycloakSession session
    );
}
```

### 6. **UI Components**

#### A. Account Console Page: "Failed Login Attempts"

```html
<div class="failed-attempts-page">
    <h2>Failed Face Authentication Attempts</h2>
    <p>Review and improve your face template by adding high-quality failed attempts.</p>
    
    <div class="stats-summary">
        <div class="stat">
            <span class="value">5</span>
            <span class="label">Total Attempts</span>
        </div>
        <div class="stat">
            <span class="value">3</span>
            <span class="label">Can Be Added</span>
        </div>
        <div class="stat">
            <span class="value">2</span>
            <span class="label">Already Added</span>
        </div>
    </div>
    
    <div class="attempts-grid">
        <div class="attempt-card" data-attempt-id="uuid1">
            <div class="attempt-header">
                <span class="timestamp">Jan 18, 2025 8:30 PM</span>
                <span class="badge badge-warning">Not Enrolled</span>
            </div>
            
            <div class="attempt-images">
                <img src="/thumbnail/0" alt="Attempt image 1">
                <img src="/thumbnail/1" alt="Attempt image 2">
            </div>
            
            <div class="attempt-details">
                <div class="detail">
                    <span class="label">Quality:</span>
                    <span class="value quality-good">92%</span>
                </div>
                <div class="detail">
                    <span class="label">Similarity:</span>
                    <span class="value">2.5% (threshold: 1.5%)</span>
                </div>
                <div class="detail">
                    <span class="label">Liveness:</span>
                    <span class="value liveness-passed">Passed (85%)</span>
                </div>
            </div>
            
            <div class="attempt-actions">
                <button class="btn btn-primary" onclick="enrollAttempt('uuid1')">
                    <i class="fas fa-plus"></i> Add to Template
                </button>
                <button class="btn btn-secondary" onclick="viewDetails('uuid1')">
                    <i class="fas fa-eye"></i> View Details
                </button>
                <button class="btn btn-danger" onclick="deleteAttempt('uuid1')">
                    <i class="fas fa-trash"></i> Delete
                </button>
            </div>
        </div>
    </div>
    
    <div class="bulk-actions">
        <button class="btn btn-primary" onclick="enrollSelected()">
            <i class="fas fa-check-double"></i> Add Selected to Template
        </button>
        <button class="btn btn-secondary" onclick="deleteSelected()">
            <i class="fas fa-trash-alt"></i> Delete Selected
        </button>
    </div>
</div>
```

#### B. Detailed View Modal

```html
<div class="modal" id="attempt-details-modal">
    <div class="modal-content">
        <h3>Failed Attempt Details</h3>
        
        <div class="detail-section">
            <h4>Images</h4>
            <div class="image-selector">
                <div class="image-option">
                    <input type="checkbox" id="img-0" checked>
                    <label for="img-0">
                        <img src="/thumbnail/0" alt="Image 1">
                        <div class="image-quality">
                            <span>Quality: 92%</span>
                            <span class="badge badge-success">Good</span>
                        </div>
                    </label>
                </div>
                <div class="image-option">
                    <input type="checkbox" id="img-1" checked>
                    <label for="img-1">
                        <img src="/thumbnail/1" alt="Image 2">
                        <div class="image-quality">
                            <span>Quality: 88%</span>
                            <span class="badge badge-success">Good</span>
                        </div>
                    </label>
                </div>
            </div>
        </div>
        
        <div class="detail-section">
            <h4>Quality Assessments</h4>
            <table class="quality-table">
                <tr>
                    <td>Face Found</td>
                    <td><span class="badge badge-success">✓ Passed</span></td>
                </tr>
                <tr>
                    <td>Sharpness</td>
                    <td><span class="badge badge-success">✓ Passed</span></td>
                </tr>
                <tr>
                    <td>Brightness</td>
                    <td><span class="badge badge-success">✓ Passed</span></td>
                </tr>
                <tr>
                    <td>Face Size</td>
                    <td><span class="badge badge-success">✓ Passed</span></td>
                </tr>
            </table>
        </div>
        
        <div class="detail-section">
            <h4>Verification Results</h4>
            <dl>
                <dt>Similarity Score:</dt>
                <dd>2.5% (threshold: 1.5%)</dd>
                
                <dt>Liveness Score:</dt>
                <dd>85% (threshold: 50%)</dd>
                
                <dt>Failure Reason:</dt>
                <dd>Face similarity below threshold</dd>
            </dl>
        </div>
        
        <div class="modal-actions">
            <button class="btn btn-primary" onclick="enrollSelectedImages()">
                Add Selected Images to Template
            </button>
            <button class="btn btn-secondary" onclick="closeModal()">
                Cancel
            </button>
        </div>
    </div>
</div>
```

### 7. **Security Considerations**

#### Access Control
- **User Isolation**: Users can ONLY access their own failed attempts
- **Realm Isolation**: Attempts are isolated by realm
- **Token Validation**: All API calls require valid access token
- **Ownership Verification**: Double-check user ID matches attempt owner
- **Admin Override**: Optional admin access with audit logging

#### Data Protection
- **Encryption at Rest**: AES-256-GCM encryption for all stored images
- **Encryption in Transit**: HTTPS/TLS for all API communications
- **Key Management**: Separate encryption keys per realm
- **Secure Deletion**: Overwrite files before deletion (DoD 5220.22-M)
- **Integrity Verification**: SHA-256 checksums for tamper detection

#### Privacy & Compliance
- **GDPR Compliance**: 
  - Right to access (view attempts)
  - Right to deletion (delete attempts)
  - Right to data portability (export attempts)
  - Consent tracking (user must opt-in)
- **Data Minimization**: Only store necessary metadata
- **Retention Limits**: Auto-delete after configured period
- **Anonymization**: Option to anonymize old attempts
- **Privacy Notice**: Clear disclosure in UI

#### Audit & Monitoring
- **Comprehensive Logging**: All operations logged with user ID, timestamp, action
- **Audit Trail**: Immutable log of all enrollment actions
- **Anomaly Detection**: Alert on suspicious patterns (mass downloads, rapid enrollments)
- **Access Logs**: Track who accessed what and when
- **SIEM Integration**: Export logs to security monitoring systems

#### Rate Limiting & Abuse Prevention
- **API Rate Limits**: 
  - 30 requests/minute for viewing attempts
  - 10 enrollments/hour per user
  - 100 requests/minute per realm
- **Enrollment Throttling**: Prevent rapid template updates
- **Storage Quotas**: Max 20 attempts per user
- **Image Size Limits**: Max 5MB per image
- **Validation**: Re-verify image quality before enrollment

#### Attack Mitigation
- **Injection Prevention**: Sanitize all file paths and user inputs
- **Path Traversal**: Validate attempt IDs are UUIDs only
- **CSRF Protection**: Use Keycloak's built-in CSRF tokens
- **XSS Prevention**: Escape all user-generated content
- **Replay Attacks**: One-time enrollment tokens
- **Brute Force**: Lock account after suspicious activity

#### Secure Development
- **Input Validation**: Strict validation of all inputs
- **Output Encoding**: Proper encoding of all outputs
- **Error Handling**: Don't leak sensitive info in errors
- **Dependency Scanning**: Regular security updates
- **Code Review**: Security review of all changes
- **Penetration Testing**: Regular security assessments

### 8. **User Workflow**

1. **Failed Authentication** → System stores images + metadata
2. **User Notification** → "You have 3 failed attempts that could improve your template"
3. **Review Attempts** → User views gallery of failed attempts
4. **Select Images** → User chooses which images to add
5. **Enroll** → System calls BWS Enroll API with selected images
6. **Template Updated** → User's template now includes new feature vectors
7. **Improved Authentication** → Future logins more likely to succeed

### 9. **Benefits**

- **Self-Service**: Users improve their own templates
- **Better Accuracy**: More diverse training data
- **User Control**: Users decide what to add
- **Transparency**: Users see why authentication failed
- **Learning Tool**: Users understand quality requirements
- **Reduced Support**: Fewer "can't login" tickets

### 10. **Implementation Priority**

#### Phase 1: Foundation (Week 1-2)
**Core Infrastructure**
- [ ] FailedAuthConfiguration class
- [ ] FailedAuthImageStorageService (basic)
- [ ] EncryptionService for image encryption
- [ ] ImageProcessingService for thumbnails
- [ ] Storage directory structure
- [ ] Metadata JSON schema
- [ ] Unit tests for core services

**Integration**
- [ ] Modify FaceAuthenticator to capture failures
- [ ] Store images and metadata on auth failure
- [ ] User attribute tracking
- [ ] Basic audit logging

**Deliverable**: Failed attempts are stored automatically

#### Phase 2: API Layer (Week 3)
**REST Endpoints**
- [ ] GET /failed-attempts (list)
- [ ] GET /failed-attempts/{id} (details)
- [ ] GET /failed-attempts/{id}/thumbnail/{index}
- [ ] POST /failed-attempts/{id}/enroll
- [ ] DELETE /failed-attempts/{id}
- [ ] Security and authorization
- [ ] Rate limiting
- [ ] API documentation

**Testing**
- [ ] Integration tests for all endpoints
- [ ] Security tests
- [ ] Performance tests

**Deliverable**: Full REST API for managing failed attempts

#### Phase 3: User Interface (Week 4-5)
**Account Console Pages**
- [ ] Failed attempts list page
- [ ] Pagination and filtering
- [ ] Image gallery view
- [ ] Detailed view modal
- [ ] Image selection UI
- [ ] Enroll confirmation dialog
- [ ] Success/error notifications
- [ ] Mobile-responsive design

**User Experience**
- [ ] Loading states
- [ ] Error handling
- [ ] Help text and tooltips
- [ ] Accessibility (WCAG 2.1 AA)

**Deliverable**: Complete user interface for self-service

#### Phase 4: Advanced Features (Week 6)
**Bulk Operations**
- [ ] Bulk enrollment
- [ ] Bulk deletion
- [ ] Select all/none
- [ ] Progress indicators

**Quality Indicators**
- [ ] Visual quality scores
- [ ] Recommendations
- [ ] Comparison with existing template
- [ ] Predicted improvement

**Analytics**
- [ ] User statistics dashboard
- [ ] Common failure patterns
- [ ] Quality trends
- [ ] Success rate tracking

**Deliverable**: Power user features and insights

#### Phase 5: Polish & Optimization (Week 7)
**Notifications**
- [ ] Email notifications
- [ ] In-app notifications
- [ ] Notification preferences
- [ ] Digest emails

**Performance**
- [ ] Image compression optimization
- [ ] Lazy loading
- [ ] Caching strategy
- [ ] Database indexing

**Documentation**
- [ ] User guide
- [ ] Admin guide
- [ ] API documentation
- [ ] Video tutorials

**Deliverable**: Production-ready feature

#### Phase 6: Monitoring & Maintenance (Ongoing)
**Monitoring**
- [ ] Metrics collection
- [ ] Alerting rules
- [ ] Dashboard creation
- [ ] Log aggregation

**Maintenance**
- [ ] Automated cleanup jobs
- [ ] Storage monitoring
- [ ] Performance tuning
- [ ] Security updates

**Deliverable**: Operational excellence

### 11. **Success Metrics**

#### User Adoption
- **Target**: 60% of users with failed attempts review them
- **Measure**: (Users who viewed attempts) / (Users with attempts)

#### Template Improvement
- **Target**: 40% reduction in subsequent failures after enrollment
- **Measure**: Compare failure rate before/after enrollment

#### Support Reduction
- **Target**: 70% reduction in "can't login" tickets
- **Measure**: Support ticket volume comparison

#### User Satisfaction
- **Target**: 4.5/5 stars on feature rating
- **Measure**: In-app feedback survey

#### System Performance
- **Target**: <500ms API response time (p95)
- **Measure**: API latency monitoring

### 12. **Risk Assessment**

| Risk | Impact | Probability | Mitigation |
|------|--------|-------------|------------|
| Storage fills up | High | Medium | Auto-cleanup, quotas, monitoring |
| Privacy concerns | High | Low | Encryption, clear disclosure, opt-in |
| Performance degradation | Medium | Medium | Caching, optimization, load testing |
| Security breach | High | Low | Encryption, access control, auditing |
| User confusion | Medium | Medium | Clear UI, help text, tutorials |
| BWS API failures | Medium | Low | Error handling, retry logic, fallbacks |

### 13. **Testing Strategy**

#### Unit Tests
- Service layer logic
- Encryption/decryption
- Image processing
- Metadata handling

#### Integration Tests
- API endpoints
- Database operations
- File system operations
- BWS integration

#### Security Tests
- Access control
- Injection attacks
- Path traversal
- Rate limiting

#### Performance Tests
- Load testing (1000 concurrent users)
- Storage scalability
- API response times
- Image processing speed

#### User Acceptance Tests
- End-to-end workflows
- Cross-browser testing
- Mobile device testing
- Accessibility testing

## Next Steps

### Immediate Actions
1. **Review & Approve**: Stakeholder review of this design
2. **Resource Allocation**: Assign developers (2 backend, 1 frontend)
3. **Environment Setup**: Dev/staging environments with storage
4. **Sprint Planning**: Break down Phase 1 into tasks

### Decision Points ✅ DECIDED
- [x] **Storage Strategy**: **Database** (for cluster/failover support)
  - Images stored as BLOB in PostgreSQL
  - Metadata in structured columns
  - Supports Keycloak clustering out of the box
- [x] **Notification Method**: **Email** notifications
  - Configurable per user
  - Admin can disable globally
- [x] **Opt-in vs. Opt-out**: **Enabled by default**
  - Users can disable in account settings
  - Admins can disable per realm
  - Clear privacy notice on first use
- [x] **Admin Access**: **Yes, admins can view user attempts**
  - Requires `bws-admin` role
  - Full audit logging of admin access
  - Privacy notice to users
- [x] **Retention Period**: **30 days default**
  - Configurable per realm (7-90 days)
  - Auto-cleanup via scheduled job
  - Manual cleanup option for admins

### Questions to Answer
1. What's the expected storage growth rate?
2. Do we need multi-region support?
3. Should we support image export/download?
4. Do we need integration with external analytics tools?
5. What's the disaster recovery plan for stored images?

Would you like me to:
1. **Start Phase 1 implementation** (Core Storage)?
2. **Create detailed technical specs** for specific components?
3. **Build a prototype** to validate the approach?
4. **Prepare a presentation** for stakeholders?
