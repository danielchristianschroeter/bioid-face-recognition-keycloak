# Complete Integration Guide - Failed Auth Image Storage

**Status**: Ready for Implementation  
**Estimated Time**: 4-6 hours  
**Priority**: HIGH

---

## Overview

This guide provides complete step-by-step instructions to integrate the Failed Authentication Image Storage feature so users can view and train failed images in the test app.

## Current Status

✅ **Completed**:
- Core storage service implemented
- JPA provider working
- Entities created
- Encryption service ready
- Image processing ready
- Keycloak starting successfully
- Build passing with all tests

⏳ **Remaining**:
- FaceAuthenticator integration (capture failures)
- REST API endpoints (view/enroll)
- Test app UI (display/interact)

---

## Phase 1: FaceAuthenticator Integration (1-2 hours)

### Step 1.1: Add Dependency

**File**: `face-authenticator/pom.xml`

Add after face-credential dependency:
```xml
<dependency>
    <groupId>com.bioid.keycloak</groupId>
    <artifactId>failed-auth-storage</artifactId>
    <version>${project.version}</version>
</dependency>
```

### Step 1.2: Update FaceAuthenticator

**File**: `face-authenticator/src/main/java/com/bioid/keycloak/authenticator/FaceAuthenticator.java`

Add imports:
```java
import com.bioid.keycloak.failedauth.service.FailedAuthImageStorageService;
import com.bioid.keycloak.failedauth.config.FailedAuthConfiguration;
```

Add field:
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

### Step 1.3: Capture Failed Attempts

In `handleFailure()` method, add after logging:

```java
private void handleFailure(AuthenticationFlowContext context, String errorMessage) {
    int retryCount = incrementRetryCount(context);
    int maxRetries = getMaxRetries(context);

    logger.warn("Face verification failed for user: {} (Attempt {}/{}): {}",
        context.getUser().getId(), retryCount, maxRetries, errorMessage);

    // ========== CAPTURE FAILED ATTEMPT ==========
    try {
        captureFailedAttempt(context, errorMessage, retryCount, maxRetries);
    } catch (Exception e) {
        logger.error("Failed to store failed authentication attempt", e);
        // Don't fail authentication flow
    }
    // ========== END CAPTURE ==========

    if (retryCount >= maxRetries) {
        // ... existing code ...
    }
}
```

### Step 1.4: Implement Capture Method

Add new method:
```java
private void captureFailedAttempt(
        AuthenticationFlowContext context,
        String errorMessage,
        int retryCount,
        int maxRetries) {
    
    if (!FailedAuthConfiguration.getInstance().isStorageEnabled()) {
        return;
    }
    
    // Extract images from form data
    MultivaluedMap<String, String> formData = 
        context.getHttpRequest().getDecodedFormParameters();
    String imageData = formData.getFirst("imageData");
    
    if (imageData == null || imageData.isEmpty()) {
        return;
    }
    
    // Parse images
    List<String> images = parseImages(imageData);
    if (images.isEmpty()) {
        return;
    }
    
    // Get session info
    String sessionId = context.getAuthenticationSession().getParentSession().getId();
    String ipAddress = context.getConnection().getRemoteAddr();
    String userAgent = context.getHttpRequest().getHttpHeaders()
        .getHeaderString("User-Agent");
    
    // Determine failure reason
    String failureReason = determineFailureReason(errorMessage);
    
    // Get liveness info
    com.bioid.keycloak.client.config.BioIdConfiguration bioIdConfig = 
        com.bioid.keycloak.client.config.BioIdConfiguration.getInstance();
    String livenessMode = determineLivenessMode(bioIdConfig, images.size());
    
    // Store failed attempt
    String attemptId = failedAuthStorageService.storeFailedAttempt(
        context.getSession(),
        context.getRealm(),
        context.getUser(),
        images,
        failureReason,
        null, // verificationScore - not available
        null, // verificationThreshold - not available
        livenessMode,
        null, // livenessScore - not available
        null, // livenessPassed - not available
        null, // challengeDirection
        retryCount,
        maxRetries,
        sessionId,
        ipAddress,
        userAgent
    );
    
    if (attemptId != null) {
        logger.info("Stored failed attempt: {} for user: {}", 
            attemptId, context.getUser().getUsername());
    }
}

private List<String> parseImages(String imageData) {
    List<String> images = new ArrayList<>();
    try {
        if (imageData.startsWith("{")) {
            // JSON format
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

private String determineFailureReason(String errorMessage) {
    if (errorMessage == null) return "UNKNOWN";
    
    String lower = errorMessage.toLowerCase();
    if (lower.contains("liveness")) return "LIVENESS_FAILED";
    if (lower.contains("quality")) return "LOW_QUALITY";
    if (lower.contains("face not found")) return "NO_FACE_DETECTED";
    if (lower.contains("verification failed")) return "VERIFICATION_FAILED";
    if (lower.contains("timeout")) return "TIMEOUT";
    return "VERIFICATION_FAILED";
}

private String determineLivenessMode(
        com.bioid.keycloak.client.config.BioIdConfiguration config,
        int imageCount) {
    if (imageCount >= 2) {
        if (config.isLivenessChallengeResponseEnabled()) return "CHALLENGE_RESPONSE";
        if (config.isLivenessActiveEnabled()) return "ACTIVE";
    }
    return "PASSIVE";
}
```

---

## Phase 2: REST API Endpoints (2-3 hours)

### Step 2.1: Create REST Resource

**File**: `failed-auth-storage/src/main/java/com/bioid/keycloak/failedauth/rest/FailedAuthResource.java`

```java
package com.bioid.keycloak.failedauth.rest;

import com.bioid.keycloak.client.BioIdClient;
import com.bioid.keycloak.failedauth.config.FailedAuthConfiguration;
import com.bioid.keycloak.failedauth.entity.FailedAuthAttemptEntity;
import com.bioid.keycloak.failedauth.service.FailedAuthImageStorageService;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Path("/")
public class FailedAuthResource {
    
    private static final Logger logger = LoggerFactory.getLogger(FailedAuthResource.class);
    
    private final KeycloakSession session;
    private final RealmModel realm;
    private final FailedAuthImageStorageService storageService;
    
    public FailedAuthResource(KeycloakSession session, RealmModel realm) {
        this.session = session;
        this.realm = realm;
        this.storageService = new FailedAuthImageStorageService(
            FailedAuthConfiguration.getInstance());
    }
    
    @GET
    @Path("/attempts")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFailedAttempts(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("pageSize") @DefaultValue("10") int pageSize) {
        
        try {
            UserModel user = getAuthenticatedUser();
            if (user == null) {
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }
            
            List<FailedAuthAttemptEntity> attempts = storageService.getFailedAttempts(
                session, user.getId(), false, null, page, pageSize);
            
            List<Map<String, Object>> result = attempts.stream()
                .map(this::toSummary)
                .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("attempts", result);
            response.put("page", page);
            response.put("pageSize", pageSize);
            
            return Response.ok(response).build();
            
        } catch (Exception e) {
            logger.error("Failed to get attempts", e);
            return Response.serverError().entity(Map.of("error", e.getMessage())).build();
        }
    }
    
    @GET
    @Path("/attempts/{attemptId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAttempt(@PathParam("attemptId") String attemptId) {
        try {
            UserModel user = getAuthenticatedUser();
            if (user == null) {
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }
            
            FailedAuthAttemptEntity attempt = storageService.getAttempt(
                session, attemptId, user.getId());
            
            return Response.ok(toDetails(attempt)).build();
            
        } catch (Exception e) {
            logger.error("Failed to get attempt", e);
            return Response.status(Response.Status.NOT_FOUND)
                .entity(Map.of("error", e.getMessage())).build();
        }
    }
    
    @GET
    @Path("/attempts/{attemptId}/image/{index}")
    @Produces("image/jpeg")
    public Response getImage(
            @PathParam("attemptId") String attemptId,
            @PathParam("index") int index,
            @QueryParam("thumbnail") @DefaultValue("false") boolean thumbnail) {
        
        try {
            UserModel user = getAuthenticatedUser();
            if (user == null) {
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }
            
            byte[] imageData = storageService.getImage(
                session, attemptId, index, user.getId(), thumbnail);
            
            return Response.ok(imageData).build();
            
        } catch (Exception e) {
            logger.error("Failed to get image", e);
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }
    
    @POST
    @Path("/attempts/{attemptId}/enroll")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response enrollAttempt(
            @PathParam("attemptId") String attemptId,
            Map<String, Object> request) {
        
        try {
            UserModel user = getAuthenticatedUser();
            if (user == null) {
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }
            
            @SuppressWarnings("unchecked")
            List<Integer> imageIndices = (List<Integer>) request.get("imageIndices");
            
            // Get BioIdClient - you'll need to inject this properly
            BioIdClient bioIdClient = getBioIdClient();
            
            FailedAuthImageStorageService.EnrollmentResult result = 
                storageService.enrollFailedAttempt(
                    session, realm, user, attemptId, imageIndices, bioIdClient);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isSuccess());
            response.put("enrolledImages", result.getEnrolledImages());
            response.put("newFeatureVectors", result.getNewFeatureVectors());
            
            return Response.ok(response).build();
            
        } catch (Exception e) {
            logger.error("Failed to enroll attempt", e);
            return Response.serverError().entity(Map.of("error", e.getMessage())).build();
        }
    }
    
    @DELETE
    @Path("/attempts/{attemptId}")
    public Response deleteAttempt(@PathParam("attemptId") String attemptId) {
        try {
            UserModel user = getAuthenticatedUser();
            if (user == null) {
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }
            
            storageService.deleteAttempt(session, attemptId, user.getId());
            
            return Response.noContent().build();
            
        } catch (Exception e) {
            logger.error("Failed to delete attempt", e);
            return Response.serverError().entity(Map.of("error", e.getMessage())).build();
        }
    }
    
    @GET
    @Path("/statistics")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStatistics() {
        try {
            UserModel user = getAuthenticatedUser();
            if (user == null) {
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }
            
            FailedAuthImageStorageService.FailedAttemptStatistics stats = 
                storageService.getStatistics(session, user.getId());
            
            Map<String, Object> response = new HashMap<>();
            response.put("totalCount", stats.getTotalCount());
            response.put("enrolledCount", stats.getEnrolledCount());
            response.put("unenrolledCount", stats.getUnenrolledCount());
            response.put("recommendedCount", stats.getRecommendedCount());
            response.put("avgQualityScore", stats.getAvgQualityScore());
            
            return Response.ok(response).build();
            
        } catch (Exception e) {
            logger.error("Failed to get statistics", e);
            return Response.serverError().entity(Map.of("error", e.getMessage())).build();
        }
    }
    
    private UserModel getAuthenticatedUser() {
        AuthenticationManager.AuthResult authResult = 
            new AppAuthManager.BearerTokenAuthenticator(session).authenticate();
        
        if (authResult == null) {
            return null;
        }
        
        return authResult.getUser();
    }
    
    private BioIdClient getBioIdClient() {
        // TODO: Get from credential provider or create new instance
        return null;
    }
    
    private Map<String, Object> toSummary(FailedAuthAttemptEntity attempt) {
        Map<String, Object> map = new HashMap<>();
        map.put("attemptId", attempt.getAttemptId());
        map.put("timestamp", attempt.getTimestamp().toString());
        map.put("failureReason", attempt.getFailureReason());
        map.put("imageCount", attempt.getImageCount());
        map.put("avgQualityScore", attempt.getAvgQualityScore());
        map.put("enrolled", attempt.getEnrolled());
        map.put("retryAttempt", attempt.getRetryAttempt());
        return map;
    }
    
    private Map<String, Object> toDetails(FailedAuthAttemptEntity attempt) {
        Map<String, Object> map = toSummary(attempt);
        map.put("verificationScore", attempt.getVerificationScore());
        map.put("verificationThreshold", attempt.getVerificationThreshold());
        map.put("livenessMode", attempt.getLivenessMode());
        map.put("livenessScore", attempt.getLivenessScore());
        map.put("ipAddress", attempt.getIpAddress());
        return map;
    }
}
```

### Step 2.2: Create Resource Provider Factory

**File**: `failed-auth-storage/src/main/java/com/bioid/keycloak/failedauth/rest/FailedAuthResourceProviderFactory.java`

```java
package com.bioid.keycloak.failedauth.rest;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;

public class FailedAuthResourceProviderFactory implements RealmResourceProviderFactory {
    
    public static final String ID = "failed-auth";
    
    @Override
    public String getId() {
        return ID;
    }
    
    @Override
    public RealmResourceProvider create(KeycloakSession session) {
        return new FailedAuthResourceProvider(session);
    }
    
    @Override
    public void init(Config.Scope config) {
    }
    
    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }
    
    @Override
    public void close() {
    }
}
```

### Step 2.3: Create Resource Provider

**File**: `failed-auth-storage/src/main/java/com/bioid/keycloak/failedauth/rest/FailedAuthResourceProvider.java`

```java
package com.bioid.keycloak.failedauth.rest;

import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resource.RealmResourceProvider;

public class FailedAuthResourceProvider implements RealmResourceProvider {
    
    private final KeycloakSession session;
    
    public FailedAuthResourceProvider(KeycloakSession session) {
        this.session = session;
    }
    
    @Override
    public Object getResource() {
        return new FailedAuthResource(session, session.getContext().getRealm());
    }
    
    @Override
    public void close() {
    }
}
```

### Step 2.4: Register Provider

**File**: `failed-auth-storage/src/main/resources/META-INF/services/org.keycloak.services.resource.RealmResourceProviderFactory`

```
com.bioid.keycloak.failedauth.rest.FailedAuthResourceProviderFactory
```

---

## Phase 3: Test App UI (1-2 hours)

### Step 3.1: Update test-app.html

Add after the enrollment section:

```html
<!-- Failed Authentication Images Section -->
<div class="section" id="failedAuthSection" style="display:none;">
    <h2>Failed Authentication Images</h2>
    
    <div id="failedAuthStats" class="stats-box">
        <p>Loading statistics...</p>
    </div>
    
    <div id="failedAuthList" class="attempts-list">
        <p>Loading failed attempts...</p>
    </div>
</div>

<style>
.stats-box {
    background: #f5f5f5;
    padding: 15px;
    border-radius: 5px;
    margin-bottom: 20px;
}

.attempts-list {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
    gap: 20px;
}

.attempt-card {
    border: 1px solid #ddd;
    border-radius: 8px;
    padding: 15px;
    background: white;
}

.attempt-images {
    display: flex;
    gap: 10px;
    margin: 10px 0;
}

.attempt-image {
    width: 100px;
    height: 100px;
    object-fit: cover;
    border-radius: 4px;
    cursor: pointer;
}

.attempt-image.selected {
    border: 3px solid #4CAF50;
}
</style>
```

### Step 3.2: Add JavaScript Functions

```javascript
// Load failed authentication attempts
async function loadFailedAttempts() {
    try {
        const response = await fetch(
            `${KC_URL}/realms/${REALM}/failed-auth/attempts?pageSize=20`,
            {
                headers: {
                    'Authorization': `Bearer ${accessToken}`
                }
            }
        );
        
        if (!response.ok) {
            throw new Error('Failed to load attempts');
        }
        
        const data = await response.json();
        displayFailedAttempts(data.attempts);
        
    } catch (error) {
        console.error('Error loading failed attempts:', error);
        document.getElementById('failedAuthList').innerHTML = 
            `<p class="error">Error: ${error.message}</p>`;
    }
}

// Load statistics
async function loadStatistics() {
    try {
        const response = await fetch(
            `${KC_URL}/realms/${REALM}/failed-auth/statistics`,
            {
                headers: {
                    'Authorization': `Bearer ${accessToken}`
                }
            }
        );
        
        if (!response.ok) {
            throw new Error('Failed to load statistics');
        }
        
        const stats = await response.json();
        displayStatistics(stats);
        
    } catch (error) {
        console.error('Error loading statistics:', error);
    }
}

// Display statistics
function displayStatistics(stats) {
    document.getElementById('failedAuthStats').innerHTML = `
        <h3>Statistics</h3>
        <p>Total Failed Attempts: ${stats.totalCount}</p>
        <p>Already Enrolled: ${stats.enrolledCount}</p>
        <p>Available for Training: ${stats.unenrolledCount}</p>
        <p>Recommended for Training: ${stats.recommendedCount}</p>
        <p>Average Quality Score: ${(stats.avgQualityScore * 100).toFixed(1)}%</p>
    `;
}

// Display failed attempts
function displayFailedAttempts(attempts) {
    if (attempts.length === 0) {
        document.getElementById('failedAuthList').innerHTML = 
            '<p>No failed attempts found.</p>';
        return;
    }
    
    const html = attempts.map(attempt => `
        <div class="attempt-card" data-attempt-id="${attempt.attemptId}">
            <h4>${new Date(attempt.timestamp).toLocaleString()}</h4>
            <p><strong>Reason:</strong> ${attempt.failureReason}</p>
            <p><strong>Quality:</strong> ${(attempt.avgQualityScore * 100).toFixed(1)}%</p>
            <p><strong>Images:</strong> ${attempt.imageCount}</p>
            
            <div class="attempt-images" id="images-${attempt.attemptId}">
                ${Array.from({length: attempt.imageCount}, (_, i) => `
                    <img class="attempt-image" 
                         src="${KC_URL}/realms/${REALM}/failed-auth/attempts/${attempt.attemptId}/image/${i}?thumbnail=true"
                         data-index="${i}"
                         onclick="toggleImageSelection('${attempt.attemptId}', ${i})"
                         alt="Image ${i+1}">
                `).join('')}
            </div>
            
            ${!attempt.enrolled ? `
                <button onclick="enrollSelectedImages('${attempt.attemptId}')" 
                        class="btn btn-primary">
                    Train Selected Images
                </button>
                <button onclick="deleteAttempt('${attempt.attemptId}')" 
                        class="btn btn-danger">
                    Delete
                </button>
            ` : '<p class="success">✓ Already trained</p>'}
        </div>
    `).join('');
    
    document.getElementById('failedAuthList').innerHTML = html;
}

// Toggle image selection
function toggleImageSelection(attemptId, index) {
    const img = document.querySelector(
        `[data-attempt-id="${attemptId}"] .attempt-image[data-index="${index}"]`
    );
    img.classList.toggle('selected');
}

// Enroll selected images
async function enrollSelectedImages(attemptId) {
    const selectedImages = Array.from(
        document.querySelectorAll(
            `[data-attempt-id="${attemptId}"] .attempt-image.selected`
        )
    ).map(img => parseInt(img.dataset.index));
    
    if (selectedImages.length === 0) {
        alert('Please select at least one image to train');
        return;
    }
    
    try {
        const response = await fetch(
            `${KC_URL}/realms/${REALM}/failed-auth/attempts/${attemptId}/enroll`,
            {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${accessToken}`,
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    imageIndices: selectedImages
                })
            }
        );
        
        if (!response.ok) {
            throw new Error('Failed to enroll images');
        }
        
        const result = await response.json();
        alert(`Success! Enrolled ${result.enrolledImages} images. ` +
              `Added ${result.newFeatureVectors} feature vectors.`);
        
        // Reload attempts
        loadFailedAttempts();
        loadStatistics();
        
    } catch (error) {
        console.error('Error enrolling images:', error);
        alert(`Error: ${error.message}`);
    }
}

// Delete attempt
async function deleteAttempt(attemptId) {
    if (!confirm('Are you sure you want to delete this attempt?')) {
        return;
    }
    
    try {
        const response = await fetch(
            `${KC_URL}/realms/${REALM}/failed-auth/attempts/${attemptId}`,
            {
                method: 'DELETE',
                headers: {
                    'Authorization': `Bearer ${accessToken}`
                }
            }
        );
        
        if (!response.ok) {
            throw new Error('Failed to delete attempt');
        }
        
        alert('Attempt deleted successfully');
        loadFailedAttempts();
        loadStatistics();
        
    } catch (error) {
        console.error('Error deleting attempt:', error);
        alert(`Error: ${error.message}`);
    }
}

// Add to initialization
function initializeFailedAuth() {
    document.getElementById('failedAuthSection').style.display = 'block';
    loadStatistics();
    loadFailedAttempts();
}

// Call after successful login
// initializeFailedAuth();
```

---

## Testing Checklist

### Test 1: Capture Failed Attempt
1. Try to authenticate with wrong face
2. Check Keycloak logs for "Stored failed attempt"
3. Verify database has new record

### Test 2: View Failed Attempts
1. Open test-app.html
2. Login successfully
3. Check "Failed Authentication Images" section
4. Verify images are displayed

### Test 3: Train Images
1. Select one or more images
2. Click "Train Selected Images"
3. Verify success message
4. Check that attempt is marked as "Already trained"

### Test 4: Delete Attempt
1. Click "Delete" on an attempt
2. Confirm deletion
3. Verify attempt is removed from list

---

## Deployment Steps

1. **Build**: `mvn clean package`
2. **Deploy**: Copy JAR to Keycloak
3. **Restart**: Restart Keycloak
4. **Test**: Use test-app.html
5. **Monitor**: Check logs for errors

---

## Troubleshooting

### Issue: No attempts captured
- Check `FAILED_AUTH_STORAGE_ENABLED=true`
- Check FaceAuthenticator logs
- Verify database connection

### Issue: Can't view attempts
- Check REST API is registered
- Verify authentication token
- Check CORS settings

### Issue: Can't enroll images
- Verify BioIdClient is available
- Check BWS credentials
- Review enrollment logs

---

## Next Steps

1. Implement FaceAuthenticator integration
2. Create REST API endpoints
3. Update test-app.html UI
4. Test end-to-end flow
5. Deploy to production

**Estimated Total Time**: 4-6 hours

**Status**: Ready to implement
