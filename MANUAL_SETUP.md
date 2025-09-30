# Manual Keycloak BioID Setup Guide

This guide explains how to manually configure Keycloak for BioID face recognition without using the pre-configured realm JSON file.

## Why Manual Setup?

The automatic realm import can sometimes cause issues with:
- Database conflicts when restarting containers
- Account management console access problems
- Stale configuration data

Manual setup ensures a clean, working configuration every time.

## Disabling Automatic Import

If you want to completely disable automatic realm import:

1. **Remove the realm JSON file**:
   ```bash
   rm docker/keycloak/bioid-demo-realm.json
   ```

2. **Or rename it to prevent import**:
   ```bash
   mv docker/keycloak/bioid-demo-realm.json docker/keycloak/bioid-demo-realm.json.backup
   ```

3. **Restart Keycloak**:
   ```bash
   docker compose restart keycloak
   ```

## Manual Realm Configuration Steps

### 1. Create New Realm

1. Access Keycloak Admin Console: http://localhost:8080/admin
2. Login with `admin` / `admin123`
3. Click the dropdown next to "Master" → "Create Realm"
4. Enter realm name: `bioid-demo`
5. Click "Create"

### 2. Configure Authentication Flow

1. Go to "Authentication" → "Flows"
2. Click "Duplicate" on the "Browser" flow
3. Name it "Custom Browser Flow"
4. Click "Add execution" after "Username Password Form"
5. Select "Face Authenticator" from the dropdown
6. Set the requirement to "Alternative"
7. Go to "Bindings" tab
8. Set "Browser Flow" to "Custom Browser Flow"

### 3. Create Test User

1. Go to "Users" → "Add user"
2. Set username: `demouser`
3. Set email: `demo@example.com`
4. Enable "Email verified"
5. Click "Create"
6. Go to "Credentials" tab
7. Set password: `demo123`
8. Disable "Temporary"
9. Go to "Required Actions" tab
10. Add "Face Enrollment" action

### 4. Create Test Client

1. Go to "Clients" → "Create client"
2. Set Client ID: `bioid-demo-client`
3. Set Name: `BioID Demo Client`
4. Click "Next"
5. Enable "Standard flow"
6. Enable "Direct access grants"
7. Set Client authentication to "Off" (public client)
8. Click "Next"
9. Set Valid redirect URIs: `http://localhost:3000/*`, `file://*`, `http://localhost:*`
10. Set Web origins: `http://localhost:3000`, `http://localhost:8000`, `*`
11. Click "Save"

### 5. Configure Required Actions

1. Go to "Authentication" → "Required actions"
2. Find "Face Enrollment" and ensure it's enabled
3. Set it as "Default Action" if you want all new users to enroll

### 6. Test the Setup

#### Option A: Test with Account Management Console

1. Open a new browser window/incognito mode
2. Go to: `http://localhost:8080/realms/bioid-demo/account`
3. Login with `demouser` / `demo123`
4. You should be prompted for face enrollment
5. After enrollment, logout and login again to test face authentication

#### Option B: Test with the Test Application

1. **Start the test server**:
   ```bash
   python3 serve-test-app.py
   ```
   This will start a server on `http://localhost:3000` and open your browser automatically.

2. **Click "Login with Face Authentication"** on the test page

3. **Complete the authentication flow**:
   - You'll be redirected to Keycloak
   - Login with `demouser` / `demo123`
   - Complete face enrollment if prompted
   - You'll be redirected back to the test app with user information

4. **Test account management links** from the test app to verify everything works

#### Option C: Direct File Access (Alternative)

If you can't run the Python server, you can open `test-app.html` directly in your browser, but you'll need to update the client configuration:

1. **Update the client redirect URIs** to include `file://*`:
   - Go to "Clients" → "bioid-demo-client" → "Settings"
   - Add `file://*` to "Valid redirect URIs"
   - Add `*` to "Web origins" (for development only)

2. **Open the file directly**:
   - Open `test-app.html` in your browser
   - The authentication flow should work

## Troubleshooting Manual Setup

### Account Management Not Loading

If the account management console shows 401/403 errors:

1. Check that the account client has correct scopes:
   - Default client scopes: `web-origins`, `acr`, `profile`, `roles`, `basic`, `email`
   - Optional client scopes: `address`, `phone`, `offline_access`, `organization`, `microprofile-jwt`

2. Verify the account-console client settings:
   - Public client: Yes
   - Standard flow enabled: Yes
   - Full scope allowed: No
   - Valid redirect URIs: `/realms/bioid-demo/account/*`

### Face Authentication Not Working

1. Check that the Face Authenticator is properly configured in the authentication flow
2. Verify BioID credentials are correct in the `.env` file
3. Check Keycloak logs for BioID connection errors:
   ```bash
   docker compose logs keycloak | grep -i bioid
   ```

### User Cannot Enroll Face

1. Ensure "Face Enrollment" is added to the user's required actions
2. Check that the user has a valid session
3. Verify camera permissions in the browser

## Benefits of Manual Setup

- **Clean configuration**: No stale data or conflicts
- **Better understanding**: Learn how each component works
- **Customizable**: Adapt the setup to your specific needs
- **Troubleshooting**: Easier to debug issues when you know the configuration
- **Production ready**: Manual setup teaches you what's needed for production

## Converting Back to Automatic Import

If you want to export your manual configuration for future use:

1. Go to "Realm settings" → "Action" → "Partial export"
2. Enable "Export clients", "Export groups and roles"
3. Click "Export"
4. Save the downloaded JSON file as `docker/keycloak/bioid-demo-realm.json`
5. Re-enable automatic import in `docker-compose.yml` if desired