# Keycloak Client Setup for Testing

## Issue

When clicking "Test Login" or "Clear Session" in `simple-test.html`, you might see:
- Redirect errors
- "Invalid redirect URI"
- "Client not found"
- Page doesn't redirect properly

## Root Cause

The Keycloak client needs to be properly configured to allow:
1. The redirect URI (`http://localhost:3000/simple-test.html`)
2. PKCE (Proof Key for Code Exchange)
3. Public client access (no client secret required)

## Solution: Configure the Client

### Step 1: Access Admin Console

1. Open: http://localhost:8080/admin
2. Login with admin credentials (default: `admin` / `admin123`)
3. Select your realm (e.g., `bioid-demo`)

### Step 2: Check if Client Exists

1. Click **"Clients"** in the left menu
2. Look for `bioid-demo-client` (or your client ID)

**If client doesn't exist**, create it:
1. Click **"Create client"**
2. **Client ID**: `bioid-demo-client`
3. Click **"Next"**
4. Enable **"Client authentication"**: OFF (public client)
5. Enable **"Authorization"**: OFF
6. Enable **"Standard flow"**: ON
7. Enable **"Direct access grants"**: ON
8. Click **"Next"**
9. Click **"Save"**

### Step 3: Configure Client Settings

Click on your client (`bioid-demo-client`) and configure:

#### General Settings Tab

- **Client ID**: `bioid-demo-client`
- **Name**: BioID Demo Client
- **Description**: Test client for BioID face authentication
- **Always display in console**: ON (optional)

#### Capability config Tab

- **Client authentication**: OFF (public client)
- **Authorization**: OFF
- **Authentication flow**:
  - ✅ Standard flow (Authorization Code Flow)
  - ✅ Direct access grants
  - ❌ Implicit flow (not needed)
  - ❌ Service accounts roles (not needed)

#### Login settings Tab

**Root URL**: `http://localhost:3000`

**Home URL**: `http://localhost:3000/simple-test.html`

**Valid redirect URIs**: Add these (one per line):
```
http://localhost:3000/*
http://localhost:3000/simple-test.html
http://localhost:3000/test-app.html
```

**Valid post logout redirect URIs**: Add these:
```
http://localhost:3000/*
http://localhost:3000/simple-test.html
```

**Web origins**: Add these:
```
http://localhost:3000
+
```
(The `+` means "allow all valid redirect URIs")

#### Advanced Tab

**Proof Key for Code Exchange Code Challenge Method**: `S256` (required for PKCE)

**Access Token Lifespan**: 5 minutes (default is fine)

**Client Session Idle**: 30 minutes (default is fine)

**Client Session Max**: 10 hours (default is fine)

### Step 4: Save

Click **"Save"** at the bottom of the page.

## Verify Configuration

### Test 1: Check Client Settings

Run this in your browser console or PowerShell:

```powershell
curl http://localhost:8080/realms/bioid-demo/.well-known/openid-configuration
```

You should see the OIDC configuration with endpoints.

### Test 2: Test Authorization Endpoint

Open this URL in your browser:
```
http://localhost:8080/realms/bioid-demo/protocol/openid-connect/auth?client_id=bioid-demo-client&redirect_uri=http://localhost:3000/simple-test.html&response_type=code&scope=openid
```

**Expected**: Redirects to Keycloak login page

**Error**: "Invalid redirect URI" or "Client not found" means configuration is wrong

### Test 3: Use the Test Page

1. Start server: `python -m http.server 3000`
2. Open: http://localhost:3000/simple-test.html
3. Click "Test Login"
4. Should redirect to Keycloak login

## Common Issues and Fixes

### Issue 1: "Invalid redirect URI"

**Cause**: Redirect URI not in "Valid redirect URIs"

**Fix**:
1. Go to Clients → bioid-demo-client → Settings
2. Add to "Valid redirect URIs": `http://localhost:3000/*`
3. Save

### Issue 2: "Client not found"

**Cause**: Client ID mismatch

**Fix**:
1. Check client ID in Keycloak (should be `bioid-demo-client`)
2. Check client ID in `simple-test.html` (line 44):
   ```javascript
   '?client_id=bioid-demo-client' +
   ```
3. Make sure they match

### Issue 3: "Invalid parameter: code_challenge_method"

**Cause**: PKCE not enabled

**Fix**:
1. Go to Clients → bioid-demo-client → Advanced
2. Set "Proof Key for Code Exchange Code Challenge Method" to `S256`
3. Save

### Issue 4: "Access denied"

**Cause**: User doesn't have permission or client not enabled

**Fix**:
1. Check client is enabled: Clients → bioid-demo-client → Settings → Enabled: ON
2. Check user exists and is enabled
3. Check authentication flow is configured

### Issue 5: Buttons don't work

**Cause**: JavaScript errors or wrong realm name

**Fix**:
1. Open browser console (F12)
2. Check for JavaScript errors
3. Verify realm name in `simple-test.html` matches your realm:
   ```javascript
   // Change 'bioid-demo' to your realm name
   const authUrl = 'http://localhost:8080/realms/bioid-demo/protocol/openid-connect/auth'
   ```

## Alternative: Use Existing Client

If you already have a client configured, update `simple-test.html`:

```javascript
// Line 44 - Change client_id
const authUrl = 'http://localhost:8080/realms/YOUR-REALM/protocol/openid-connect/auth' +
    '?client_id=YOUR-CLIENT-ID' +
    '&redirect_uri=' + encodeURIComponent('http://localhost:3000/simple-test.html') +
    // ... rest of the URL
```

## Quick Setup Script

Here's a quick checklist:

```
☐ Realm exists (bioid-demo)
☐ Client exists (bioid-demo-client)
☐ Client authentication: OFF
☐ Standard flow: ON
☐ Valid redirect URIs: http://localhost:3000/*
☐ PKCE enabled: S256
☐ Web origins: http://localhost:3000
☐ Client is enabled
☐ User exists and is enabled
```

## Testing After Configuration

1. **Clear browser cache** (Ctrl+Shift+Delete)
2. **Restart HTTP server**:
   ```cmd
   python -m http.server 3000
   ```
3. **Open test page**: http://localhost:3000/simple-test.html
4. **Click "Clear Session"** (should reload without error)
5. **Click "Test Login"** (should redirect to Keycloak)
6. **Login** with username/password
7. **Should redirect back** with success message

## Expected Flow

```
1. Click "Test Login"
   ↓
2. Redirect to Keycloak login
   ↓
3. Enter username/password
   ↓
4. Face authentication (if configured)
   ↓
5. Redirect back to simple-test.html?code=...
   ↓
6. Show: ✅ Success! Got authorization code: abc123...
```

## Still Not Working?

### Check Browser Console

1. Open browser console (F12)
2. Click "Test Login"
3. Look for errors:
   - CORS errors → Check web origins
   - 404 errors → Check realm/client names
   - Redirect errors → Check redirect URIs

### Check Keycloak Logs

```cmd
docker compose logs keycloak | findstr /i "error\|invalid\|denied"
```

### Check Network Tab

1. Open browser DevTools (F12)
2. Go to Network tab
3. Click "Test Login"
4. Check the redirect chain:
   - Request to `/auth` endpoint
   - Redirect to login page
   - Redirect back with code

### Verify Realm Name

The test page uses `bioid-demo` realm. If your realm has a different name:

1. Find all occurrences of `bioid-demo` in `simple-test.html`
2. Replace with your realm name
3. Save and refresh

## BWS Admin Role Configuration

The `bws-admin` role allows administrators to manage face templates across all users using the BWS Management API.

### What Can BWS Admins Do?

- List all enrolled face templates in the system
- View template details (class ID, enrollment date, quality metrics)
- Delete orphaned templates (templates without corresponding Keycloak users)
- Validate template consistency between Keycloak and BWS
- Monitor enrollment statistics

### Step 1: Create the bws-admin Role

1. Go to **Realm Roles**
2. Click **"Create role"**
3. **Role name**: `bws-admin`
4. **Description**: `Administrator role for managing BWS face templates`
5. Click **"Save"**

### Step 2: Assign Role to Admin Users

1. Go to **Users** → Select a user
2. Click **"Role mapping"** tab
3. Click **"Assign role"**
4. Select `bws-admin` from the list
5. Click **"Assign"**

### Step 3: Configure BWS Management API Access

The BWS admin functionality requires access to the BWS Management API. You need:

1. **BWS Portal Account**: Sign up at https://bwsportal.bioid.com
2. **API Key**: Get your API key from your BWS Portal user profile
3. **Email Address**: Your BWS Portal email address

#### Generate Management Access Token

You can generate a JWT token for BWS Management API access in two ways:

**Option 1: Using BWS Portal**
1. Login to https://bwsportal.bioid.com
2. Click on your user profile (top right)
3. Click on your email address
4. Copy the **Management Access JWT**

**Option 2: Using JWT Tool**
1. Download the JWT tool from https://bwsportal.bioid.com/tools
2. Run:
   ```bash
   ./jwt --sub "your-email@example.com" --key "your-api-key"
   ```

### Step 4: Configure Environment Variables

Add these to your `.env` file:

```properties
# BWS Management API Configuration
BWS_MANAGEMENT_API_URL=https://bwsportal.bioid.com/api
BWS_MANAGEMENT_EMAIL=your-email@example.com
BWS_MANAGEMENT_API_KEY=your-api-key
BWS_CLIENT_ID=your-bws-client-id
```

### Step 5: Test Admin Access

1. Open http://localhost:3000/test-app.html
2. Login with a user that has the `bws-admin` role
3. You should see an **"Admin Panel"** section
4. Click **"View All Enrolled Faces"** to list all templates

### BWS Management API Endpoints Used

The admin interface uses these BWS Management API endpoints:

#### 1. Get Class Count
```
GET /api/client/classcount/{clientId}
```
Returns the number of enrolled face classes for a BWS 3 client.

**Parameters:**
- `clientId`: Your BWS client ID
- `tags` (optional): Filter by tags

**Response:**
```json
{
  "count": 42
}
```

#### 2. List All Classes
```
GET /api/client/classes/{clientId}
```
Returns all enrolled class IDs (Note: This endpoint may need to be requested from BioID support).

#### 3. Delete Template (via gRPC)
```
rpc DeleteTemplate (DeleteTemplateRequest) returns (DeleteTemplateResponse)
```
Deletes a face template by class ID.

**Request:**
```protobuf
message DeleteTemplateRequest {
    int64 classId = 1;
}
```

### Admin Panel Features

The admin panel in `test-app.html` provides:

1. **Template Statistics**
   - Total enrolled faces
   - Active users with face credentials
   - Orphaned templates (BWS templates without Keycloak users)

2. **Template List**
   - Class ID
   - Associated Keycloak user (if exists)
   - Enrollment date
   - Template quality metrics
   - Actions (view details, delete)

3. **Validation Tools**
   - Check for orphaned templates
   - Verify Keycloak ↔ BWS consistency
   - Bulk cleanup operations

4. **Audit Log**
   - Recent admin actions
   - Template deletions
   - User enrollments

### Security Considerations

⚠️ **Important Security Notes:**

1. **Role-Based Access**: Only users with `bws-admin` role can access admin features
2. **API Key Protection**: Never expose BWS API keys in client-side code
3. **Backend Proxy**: Implement a backend service to proxy BWS Management API calls
4. **Audit Logging**: All admin actions should be logged
5. **Rate Limiting**: Implement rate limiting for admin API calls

### Recommended Architecture

For production, implement a backend service:

```
[Admin UI] → [Keycloak Auth] → [Backend Service] → [BWS Management API]
                                      ↓
                                [Audit Log]
```

**Backend Service Responsibilities:**
- Validate `bws-admin` role from Keycloak token
- Securely store and use BWS API credentials
- Proxy requests to BWS Management API
- Log all admin actions
- Implement rate limiting
- Validate requests

### Example Backend Endpoint

```java
@RestController
@RequestMapping("/api/admin/bws")
public class BWSAdminController {
    
    @GetMapping("/templates")
    @PreAuthorize("hasRole('bws-admin')")
    public ResponseEntity<List<TemplateInfo>> listAllTemplates(
            @AuthenticationPrincipal Jwt jwt) {
        
        // Validate admin role
        if (!jwt.getClaimAsStringList("realm_access.roles").contains("bws-admin")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        // Call BWS Management API
        List<TemplateInfo> templates = bwsManagementService.listAllTemplates();
        
        // Audit log
        auditService.logAdminAction(jwt.getSubject(), "LIST_TEMPLATES");
        
        return ResponseEntity.ok(templates);
    }
    
    @DeleteMapping("/templates/{classId}")
    @PreAuthorize("hasRole('bws-admin')")
    public ResponseEntity<Void> deleteTemplate(
            @PathVariable Long classId,
            @AuthenticationPrincipal Jwt jwt) {
        
        // Validate admin role
        if (!jwt.getClaimAsStringList("realm_access.roles").contains("bws-admin")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        // Delete template via gRPC
        bwsGrpcService.deleteTemplate(classId);
        
        // Audit log
        auditService.logAdminAction(jwt.getSubject(), "DELETE_TEMPLATE", classId);
        
        return ResponseEntity.noContent().build();
    }
}
```

### Testing the Admin Role

1. **Create Test Admin User**:
   ```bash
   # In Keycloak Admin Console
   Users → Add user
   Username: admin-test
   Email: admin@example.com
   Save → Credentials → Set password
   Role mapping → Assign role → bws-admin
   ```

2. **Test Admin Access**:
   - Login to test-app.html with admin-test user
   - Verify admin panel is visible
   - Test listing templates
   - Test deleting an orphaned template

3. **Test Non-Admin User**:
   - Login with regular user
   - Verify admin panel is NOT visible
   - Verify admin API calls return 403 Forbidden

## Summary

The most common issue is **missing redirect URI configuration**. Make sure:

1. ✅ Client exists in Keycloak
2. ✅ Valid redirect URIs includes `http://localhost:3000/*`
3. ✅ PKCE is enabled (S256)
4. ✅ Client authentication is OFF (public client)
5. ✅ Standard flow is enabled

After configuring these, the test page should work perfectly!
