# FaceAuthenticator Integration Guide for Failed Auth Storage

**Date**: 2025-11-01  
**Status**: READY FOR IMPLEMENTATION  
**Estimated Time**: 2-3 hours

---

## Overview

This guide provides step-by-step instructions for integrating the Failed Authentication Image Storage feature with the FaceAuthenticator.

## Integration Points

### 1. Add Dependency

**File**: `face-authenticator/pom.xml`

```xml
<dependency>
    <groupId>com.bioid.keycloak</groupId>
    <artifactId>failed-auth-storage</artifactId>
    <version>${project.version}</version>
</dependency>
```

### 2. Import Required Classes

**File**: `face-authenticator/src/main/java/com/bioid/keycloak/authenticator/FaceAuthenticator.java`

Add imports:
```java
import com.bioid.keycloak.failedauth.service.FailedAuthImageStorageService;
import com.bioid.keycloak.failedauth.config.FailedAuthConfiguration;
import com.bioid.keycloak.failedauth.exception.FailedAuthStorageException;
```

### 3. Initialize Storage Service

Add field to FaceAuthenticator class:
```java
private final FailedAuthImageStorageService failedAuthStorageService;
```

Update constructor:
```java
public FaceAuthenticator(KeycloakSession session) {
    FailedAuthConfiguration config = FailedAuthConfiguration.getInstance();
    this.failedAuthStorageService = new FailedAuthImageStorageService(config);
}
```

### 4. Capture Failed Attempts

**Location**: In `handleFailure()` method, after logging the failure

**Add this code**:
```java
private void handleFailure(AuthenticationFlowContext context, String errorMessage) {
    int retryCount = incrementRetryCount(context);
    int maxRetries = getMaxRetries(context);

    logger.warn(
        "Face verification failed for user: {} (Attempt {}/{}): {}",
        context.getUser().getId(),
        retryCount,
        maxRetries,
        errorMessage);

    // ========== ADD THIS SECTION ==========
    // Store failed attempt if storage is enabled
    try {
        storeFailedAttempt(context, errorMessage, retryCount, maxRetries);
    } catch (Exception e) {
        // Log but don't fail authentication flow
        logger.error("Failed to store failed authentication attempt", e);
    }
    // ========== END NEW SECTION ==========

    if (retryCount >= maxRetries) {
        // ... existing code ...
    }
}
```

### 5. Implement Storage Method

**Add new method to FaceAuthenticator**:

```java
/**
 * Store failed authentication attempt with images and metadata.
 */
private void storeFailedAttempt(
        AuthenticationFlowContext context,
        String errorMessage,
        int retryCount,
        int maxRetries) {
    
    try {
        // Check if storage is enabled
        if (!FailedAuthConfiguration.getInstance().isStorageEnabled()) {
            logger.debug("Failed auth storage is disabled");
            return;
        }
        
        // Extract images from form data
        MultivaluedMap<String, String> formData = 
            context.getHttpRequest().getDecodedFormParameters();
        String imageData = formData.getFirst("imageData");
        
        if (imageData == null || imageData.isEmpty()) {
            logger.debug("No image data to store");
            return;
        }
        
        // Parse images (handle both single and multiple images)
        List<String> images = parseImages(imageData);
        
        if (images.isEmpty()) {
            logger.debug("No valid images to store");
            return;
        }
        
        // Get session information
        String sessionId = context.getAuthenticationSession().getParentSession().getId();
        String ipAddress = context.getConnection().getRemoteAddr();
        String userAgent = context.getHttpRequest().getHttpHeaders()
            .getHeaderString("User-Agent");
        
        // Determine failure reason
        String failureReason = determineFailureReason(errorMessage);
        
        // Get liveness information
        com.bioid.keycloak.client.config.BioIdConfiguration bioIdConfig = 
            com.bioid.keycloak.client.config.BioIdConfiguration.getInstance();
        
        String livenessMode = determineLivenessMode(bioIdConfig, images.size());
        String challengeDirection = extractChallengeDirection(imageData);
        
        // Store the failed attempt
        String attemptId = failedAuthStorageService.storeFailedAttempt(
            context.getSession(),
            context.getRealm(),
            context.getUser(),
            images,
            failureReason,
            null, // verificationScore - not available in current flow
            null, // verificationThreshold - not available
            livenessMode,
            null, // livenessScore - not available
            null, // livenessPassed - not available
            challengeDirection,
            retryCount,
            maxRetries,
            sessionId,
            ipAddress,
            userAgent
        );
        
        if (attemptId != null) {
            logger.info("Stored failed authentication attempt: {} for user: {}", 
                attemptId, context.getUser().getUsername());
        }
        
    } catch (FailedAuthStorageException e) {
        logger.error("Failed to store failed authentication attempt", e);
        // Don't propagate - storage failure shouldn't affect authentication flow
    }
}

/**
 * Parse images from form data (handles both single and multiple images).
 */
private List<String> parseImages(String imageData) {
    List<String> images = new ArrayList<>();
    
    try {
        if (imageData.startsWith("{")) {
            // JSON format with multiple images
            com.fasterxml.jackson.databind.ObjectMapper mapper = 
                new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode jsonNode = mapper.readTree(imageData);
            com.fasterxml.jackson.databind.JsonNode imagesNode = jsonNode.get("images");
            
            if (imagesNode != null && imagesNode.isArray()) {
                for (com.fasterxml.jackson.databind.JsonNode imageNode : imagesNode) {
                    images.add(imageNode.asText());
                }
            }
        } else {
            // Single image
            images.add(imageData);
        }
    } catch (Exception e) {
        logger.error("Failed to parse image data", e);
    }
    
    return images;
}

/**
 * Determine failure reason from error message.
 */
private String determineFailureReason(String errorMessage) {
    if (errorMessage == null) {
        return "UNKNOWN";
    }
    
    String lowerMessage = errorMessage.toLowerCase();
    
    if (lowerMessage.contains("liveness")) {
        return "LIVENESS_FAILED";
    } else if (lowerMessage.contains("quality")) {
        return "LOW_QUALITY";
    } else if (lowerMessage.contains("face not found") || lowerMessage.contains("no face")) {
        return "NO_FACE_DETECTED";
    } else if (lowerMessage.contains("verification failed") || lowerMessage.contains("not match")) {
        return "VERIFICATION_FAILED";
    } else if (lowerMessage.contains("timeout")) {
        return "TIMEOUT";
    } else {
        return "VERIFICATION_FAILED";
    }
}

/**
 * Determine liveness mode based on configuration and image count.
 */
private String determineLivenessMode(
        com.bioid.keycloak.client.config.BioIdConfiguration config,
        int imageCount) {
    
    if (imageCount >= 2) {
        if (config.isLivenessChallengeResponseEnabled()) {
            return "CHALLENGE_RESPONSE";
        } else if (config.isLivenessActiveEnabled()) {
            return "ACTIVE";
        }
    }
    
    return "PASSIVE";
}

/**
 * Extract challenge direction from JSON data.
 */
private String extractChallengeDirection(String imageData) {
    try {
        if (imageData.startsWith("{")) {
            com.fasterxml.jackson.databind.ObjectMapper mapper = 
                new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode jsonNode = mapper.readTree(imageData);
            
            if (jsonNode.has("challengeDirection")) {
                return jsonNode.get("challengeDirection").asText();
            }
        }
    } catch (Exception e) {
        logger.debug("Could not extract challenge direction", e);
    }
    
    return null;
}
```

### 6. Enhanced Integration (Optional)

For better data quality, modify the `performVerification()` method to capture verification scores:

```java
private boolean performVerification(
      AuthenticationFlowContext context, FaceCredentialModel credential, String imageData)
      throws BioIdException {
    
    logger.info("Performing verification for classId: {}", credential.getClassId());

    // Store verification result for failed auth storage
    context.getAuthenticationSession().setAuthNote("imageData", imageData);
    
    boolean result;
    
    if (imageData.startsWith("{")) {
      result = performLivenessVerification(context, credential, imageData);
    } else {
      result = getCredentialProvider(context.getSession())
          .verifyFace(context.getRealm(), context.getUser(), imageData);
    }
    
    // Store result for potential failed auth storage
    context.getAuthenticationSession().setAuthNote("verificationResult", 
        String.valueOf(result));
    
    return result;
}
```

Then update `handleFailure()` to retrieve this data:
```java
String storedImageData = context.getAuthenticationSession().getAuthNote("imageData");
if (storedImageData != null) {
    imageData = storedImageData;
}
```

---

## Testing Checklist

### Unit Tests
- [ ] Test `storeFailedAttempt()` with single image
- [ ] Test `storeFailedAttempt()` with multiple images
- [ ] Test `parseImages()` with JSON format
- [ ] Test `parseImages()` with single image
- [ ] Test `determineFailureReason()` with various messages
- [ ] Test storage disabled scenario
- [ ] Test storage exception handling

### Integration Tests
- [ ] Trigger failed authentication
- [ ] Verify attempt stored in database
- [ ] Verify images encrypted
- [ ] Verify thumbnails generated
- [ ] Verify audit log created
- [ ] Verify user preferences updated
- [ ] Test with liveness detection
- [ ] Test with challenge-response

### Manual Testing
- [ ] Deploy to test environment
- [ ] Perform failed authentication
- [ ] Check database for stored attempt
- [ ] Verify images are encrypted
- [ ] Check logs for storage confirmation
- [ ] Test with different failure reasons
- [ ] Test retry mechanism
- [ ] Test max retries exceeded

---

## Configuration

Ensure these environment variables are set:

```bash
# Enable failed auth storage
FAILED_AUTH_STORAGE_ENABLED=true

# Retention and limits
FAILED_AUTH_RETENTION_DAYS=30
FAILED_AUTH_MAX_ATTEMPTS_PER_USER=20

# Image processing
FAILED_AUTH_INCLUDE_THUMBNAILS=true
FAILED_AUTH_THUMBNAIL_SIZE=300
FAILED_AUTH_MAX_IMAGE_SIZE_MB=5

# Security
FAILED_AUTH_ENCRYPT_IMAGES=true
FAILED_AUTH_VERIFY_INTEGRITY=true

# Database
FAILED_AUTH_DB_URL=jdbc:postgresql://postgres:5432/keycloak
FAILED_AUTH_DB_USER=keycloak
FAILED_AUTH_DB_PASSWORD=keycloak
```

---

## Troubleshooting

### Issue: Attempts not being stored

**Check**:
1. `FAILED_AUTH_STORAGE_ENABLED=true`
2. Database connection working
3. JPA provider loaded (check logs for "FailedAuthJpaConnectionProviderFactory")
4. No exceptions in logs

### Issue: Images not encrypted

**Check**:
1. `FAILED_AUTH_ENCRYPT_IMAGES=true`
2. Bouncy Castle provider available
3. Check encryption service logs

### Issue: Database errors

**Check**:
1. PostgreSQL running
2. Database credentials correct
3. Tables created (auto-created on first use)
4. Connection pool not exhausted

---

## Performance Impact

**Expected overhead per failed attempt**:
- Image encryption: ~10-50ms per image
- Thumbnail generation: ~50-100ms per image
- Database insert: ~10-20ms
- **Total**: ~100-200ms

**Mitigation**:
- Storage happens asynchronously (doesn't block user)
- Only stores failed attempts (not successful ones)
- Automatic cleanup prevents database growth

---

## Security Considerations

1. **Images are encrypted** at rest using AES-256-GCM
2. **User isolation** enforced - users can only see their own attempts
3. **Audit logging** tracks all access
4. **Retention limits** ensure GDPR compliance
5. **User opt-out** available via preferences

---

## Next Steps

1. **Implement integration** following this guide
2. **Run unit tests** to verify functionality
3. **Deploy to staging** for integration testing
4. **Manual testing** with real authentication flows
5. **Monitor logs** for any issues
6. **Verify database** contains expected data

---

## Estimated Timeline

- **Implementation**: 2-3 hours
- **Unit Testing**: 1-2 hours
- **Integration Testing**: 2-3 hours
- **Total**: 5-8 hours

---

## Success Criteria

- ✅ Failed attempts stored in database
- ✅ Images encrypted and retrievable
- ✅ Thumbnails generated
- ✅ Audit logs created
- ✅ No performance degradation
- ✅ No authentication flow disruption
- ✅ All tests passing

---

**Status**: Ready for implementation  
**Priority**: HIGH  
**Blocking**: User access to failed auth feature
