# REST API Setup Guide

## What Was Implemented

A complete REST API for face credential management that provides:

1. **GET /realms/{realm}/face-api/status** - Get template status and enrolled images
2. **DELETE /realms/{realm}/face-api/template** - Delete face credentials
3. **Full CORS support** - Works from any origin (including localhost:3000)

## Files Created/Modified

### New Files
- `face-credential/src/main/java/com/bioid/keycloak/rest/FaceCredentialResource.java` - REST endpoints
- `face-credential/src/main/java/com/bioid/keycloak/rest/FaceCredentialResourceProvider.java` - Provider
- `face-credential/src/main/java/com/bioid/keycloak/rest/FaceCredentialResourceProviderFactory.java` - Factory
- `face-credential/src/main/resources/META-INF/services/org.keycloak.services.resource.RealmResourceProviderFactory` - SPI registration

### Modified Files
- `test-app.html` - Added API calls to display template status and enrolled images

## How to Build and Deploy

### Option 1: Using build-windows.ps1 (Recommended for Windows)

```powershell
.\build-windows.ps1
```

This script handles file locking issues on Windows.

### Option 2: Manual Maven Build

```bash
# Clean build
mvn clean package -DskipTests

# Copy to Keycloak
copy deployment\target\keycloak-bioid-extension-*.jar keycloak-data\providers\

# Restart Keycloak
docker compose restart keycloak
```

### Option 3: Skip Tests if Build Fails

If you encounter test compilation errors due to Windows file locking:

```bash
mvn clean package -DskipTests
```

## How to Test

### 1. Start the Test Server

```bash
cd test-server
npm install
npm start
```

This serves test-app.html at http://localhost:3000

### 2. Open test-app.html

Navigate to: http://localhost:3000/test-app.html

### 3. Login and View Status

1. Click "Login with Face Authentication"
2. Complete the authentication flow
3. After successful login, you should see:
   - User information
   - Authentication details
   - **Template Status** (enrolled/not enrolled)
   - **Enrolled Images** (thumbnails from BWS)

### 4. Expected Output

If enrolled:
```
✅ Template Status: Enrolled
Class ID: 12345
Enrolled At: 10/16/2025, 10:30:00 PM
Encoder Version: 9.1
Feature Vectors: 2
Thumbnails Stored: 2

📸 Enrolled Images:
[Image 1] [Image 2]
```

If not enrolled:
```
⚠️ No face template enrolled. Please enroll your face in Account Management.
```

## API Endpoints

### Get Template Status

```bash
curl -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  http://localhost:8080/realms/bioid-demo/face-api/status
```

Response:
```json
{
  "enrolled": true,
  "classId": 12345,
  "enrolledAt": "2025-10-16T20:30:00Z",
  "encoderVersion": "9.1",
  "featureVectors": 2,
  "thumbnailsStored": 2,
  "tags": [],
  "thumbnails": [
    {
      "enrolledAt": "2025-10-16T20:30:00Z",
      "image": "data:image/png;base64,iVBORw0KG..."
    }
  ]
}
```

### Delete Template

```bash
curl -X DELETE \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  http://localhost:8080/realms/bioid-demo/face-api/template
```

Response:
```json
{
  "deleted": true,
  "count": 1,
  "message": "Successfully deleted 1 face credential(s)"
}
```

## CORS Configuration

The REST API includes full CORS support with these headers:
- `Access-Control-Allow-Origin: *`
- `Access-Control-Allow-Methods: GET, DELETE, OPTIONS`
- `Access-Control-Allow-Headers: Authorization, Content-Type`

This allows the API to be called from any origin, including:
- localhost:3000 (test server)
- Browser extensions
- Mobile apps
- Other web applications

## Troubleshooting

### Issue: CORS Error

**Symptom**: "Access to fetch at '...' has been blocked by CORS policy"

**Solution**: The REST API now includes CORS headers. Rebuild and redeploy:
```bash
mvn clean package -DskipTests
copy deployment\target\keycloak-bioid-extension-*.jar keycloak-data\providers\
docker compose restart keycloak
```

### Issue: 404 Not Found

**Symptom**: GET http://localhost:8080/realms/bioid-demo/face-api/status 404

**Solution**: The SPI is not registered. Check:
1. File exists: `face-credential/src/main/resources/META-INF/services/org.keycloak.services.resource.RealmResourceProviderFactory`
2. Contains: `com.bioid.keycloak.rest.FaceCredentialResourceProviderFactory`
3. Rebuild and redeploy

### Issue: 401 Unauthorized

**Symptom**: {"error": "Authentication required"}

**Solution**: The access token is invalid or expired. Re-login in test-app.html.

### Issue: Build Fails with File Locking

**Symptom**: "unable to access file: java.nio.file.NoSuchFileException"

**Solution**: Use the Windows build script:
```powershell
.\build-windows.ps1
```

Or skip tests:
```bash
mvn clean package -DskipTests
```

## Next Steps

1. **Enroll a face** in Account Management
2. **Test the API** using test-app.html
3. **View enrolled images** in the browser
4. **Delete credentials** using the API or admin console

The REST API is now fully integrated and ready to use!
