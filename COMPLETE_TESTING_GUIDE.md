# Complete BioID Face Authentication Testing Guide

This guide provides **complete step-by-step instructions** for testing your BioID face authentication system, including Keycloak configuration and the test application.

## 🎯 What You'll Test

1. **Face Enrollment**: First-time users enrolling their face biometrics
2. **Face Authentication**: Returning users authenticating with their face
3. **Account Management**: Users managing their face credentials
4. **Admin Configuration**: Administrators configuring the system

## 📋 Prerequisites

Before starting, ensure you have:

- ✅ **Docker and Docker Compose** installed
- ✅ **BioID BWS Account** with Client ID and Secret Key
- ✅ **Extension built**: `mvn clean package -DskipTests` completed successfully
- ✅ **Camera access** available in your browser

## 🚀 Step 1: Start Your Environment

### 1.1 Configure BioID Credentials

Create or update your `.env` file in the project root:

```bash
# Required BioID Configuration
BWS_CLIENT_ID=your_actual_client_id_here
BWS_KEY=your_actual_secret_key_here
BWS_ENDPOINT=face.bws-eu.bioid.com

# Keycloak Admin Credentials
KEYCLOAK_ADMIN=admin
KEYCLOAK_ADMIN_PASSWORD=admin123
```

### 1.2 Start Keycloak

```bash
# Start the services
docker compose up -d

# Verify Keycloak is ready (wait for this to succeed)
curl http://localhost:8080/health/ready

# Check that the BioID extension loaded
docker compose logs keycloak | grep -i bioid
```

**Expected output**: You should see logs indicating the BioID extension initialized successfully.

## 🏗️ Step 2: Configure Keycloak Realm

### 2.1 Access Keycloak Admin Console

1. **Open**: http://localhost:8080/admin
2. **Login**: `admin` / `admin123`

### 2.2 Import Demo Realm (Recommended)

1. **Click** the dropdown next to "Master" (top-left)
2. **Click** "Create Realm"
3. **Click** "Browse" and select `docker/keycloak/bioid-demo-realm.json`
4. **Click** "Create"

✅ **Success**: You should now see "bioid-demo" in the realm dropdown.

### 2.3 Verify Realm Configuration

After importing, verify these components exist:

#### Authentication Flow
1. **Go to**: Authentication → Flows
2. **Find**: "Custom Browser" flow
3. **Verify**: Contains "Face Authenticator" execution

#### Required Actions
1. **Go to**: Authentication → Required Actions
2. **Verify**: "Face Enrollment" is enabled

#### Demo User
1. **Go to**: Users
2. **Find**: User `demouser`
3. **Verify**: Has "Face Enrollment" required action

#### Demo Client
1. **Go to**: Clients
2. **Find**: `bioid-demo-client`
3. **Verify**: Valid redirect URIs include `http://localhost:3000/*`

## 🧪 Step 3: Run the Automated Test Script

### 3.1 Quick Verification

Run the automated test script to verify your setup:

**Linux/Mac:**
```bash
./test-face-auth.sh
```

**Windows:**
```bash
test-face-auth.bat
```

This script checks:
- ✅ Keycloak is running
- ✅ bioid-demo realm exists
- ✅ BioID extension is loaded
- ✅ Test files are present
- ✅ Python is available

### 3.2 Expected Output

```
🧪 BioID Face Authentication Test Script
========================================

1. Checking Keycloak availability...
✅ Keycloak is running and ready

2. Checking if bioid-demo realm exists...
✅ bioid-demo realm is accessible

3. Checking BioID extension...
✅ BioID extension is loaded and initialized

4. Checking test application files...
✅ test-app.html found
✅ serve-test-app.py found

5. Checking Python for test server...
✅ Python 3 is available

🎉 Setup verification complete!
```

## 🌐 Step 4: Start the Test Application

### 4.1 Start the Test Server

```bash
python3 serve-test-app.py
```

**Expected output:**
```
🚀 BioID Test App Server starting on port 3000
📱 Open your browser to: http://localhost:3000/test-app.html
🔐 Make sure your Keycloak is running on: http://localhost:8080
⏹️  Press Ctrl+C to stop the server

✅ Browser opened automatically
```

### 4.2 Access the Test Application

The browser should open automatically to: http://localhost:3000/test-app.html

If not, manually navigate to: http://localhost:3000/test-app.html

## 🔐 Step 5: Test Face Authentication Flow

### 5.1 Test Complete OIDC Flow

1. **Click**: "Login with Face Authentication" button
2. **Redirected to**: Keycloak login page
3. **Enter credentials**: `demouser` / `demo123`
4. **Complete**: Face enrollment (first time) or face authentication
5. **Redirected back**: To test app with user information displayed

### 5.2 What Happens During the Flow

#### First Login (Face Enrollment)
1. **Username/Password**: Standard Keycloak login
2. **Face Enrollment Prompt**: Camera access requested
3. **Enrollment Process**: Follow on-screen instructions
4. **Success**: Redirected back to test app

#### Subsequent Logins (Face Authentication)
1. **Username/Password**: Standard Keycloak login
2. **Face Authentication**: Camera access for verification
3. **Success/Failure**: Based on face recognition result
4. **Redirect**: Back to test app

### 5.3 Expected Test Results

**Successful Authentication:**
```
Authentication Successful!

User Information:
Username: demouser
Email: demo@example.com
First Name: Demo
Last Name: User
Subject: [user-id]
```

## 🔧 Step 6: Test Account Management

### 6.1 Access Account Console

From the test app, click: "Manage Account & Face Enrollment"

Or directly navigate to: http://localhost:8080/realms/bioid-demo/account

### 6.2 Test Account Features

1. **Personal Info**: View/edit user profile
2. **Account Security**: Manage authentication methods
3. **Face Credentials**: View face enrollment status
4. **Sessions**: View active sessions

### 6.3 Test Face Credential Management

1. **Go to**: Account Security → Signing In
2. **Find**: Face Recognition section
3. **Test**: Remove/re-enroll face credentials

## 🛠️ Step 7: Test Admin Configuration

### 7.1 Access Face Recognition Settings

1. **Go to**: http://localhost:8080/admin
2. **Select**: bioid-demo realm
3. **Navigate**: Realm Settings → Face Recognition tab

### 7.2 Test Configuration Options

1. **BioID Service Settings**:
   - Client ID and Key
   - Service endpoint
   - Connection timeout

2. **Verification Settings**:
   - Verification threshold
   - Maximum retries
   - Liveness detection options

3. **Template Management**:
   - Template TTL
   - Cleanup policies

### 7.3 Test Connectivity

1. **Click**: "Test Connection" button
2. **Verify**: Successful connection to BioID service
3. **Check**: Service status and latency

## 🔍 Step 8: Troubleshooting Common Issues

### Issue: "Invalid redirect URI"

**Symptoms**: Error when clicking login button

**Solution**:
1. Go to Keycloak Admin → Clients → bioid-demo-client
2. Verify "Valid redirect URIs" includes: `http://localhost:3000/*`
3. Save and try again

### Issue: Face enrollment not working

**Symptoms**: Camera doesn't activate or enrollment fails

**Solutions**:
1. **Check camera permissions** in your browser
2. **Verify BioID credentials** in `.env` file
3. **Check logs**: `docker compose logs keycloak | grep -i bioid`
4. **Test connectivity** in admin console

### Issue: "User not found" error

**Symptoms**: Cannot login with demouser/demo123

**Solutions**:
1. **Verify realm**: Make sure you're in bioid-demo realm, not master
2. **Check user exists**: Admin Console → Users → Search for "demouser"
3. **Re-import realm**: Delete and re-import bioid-demo-realm.json

### Issue: Extension not loading

**Symptoms**: No face authentication options appear

**Solutions**:
1. **Check JAR exists**: `ls -la deployment/target/keycloak-bioid-extension-*.jar`
2. **Rebuild if needed**: `mvn clean package -DskipTests`
3. **Restart Keycloak**: `docker compose restart keycloak`
4. **Check logs**: `docker compose logs keycloak | grep -i error`

### Issue: CORS errors

**Symptoms**: Browser console shows CORS errors

**Solutions**:
1. **Use test server**: Make sure you're using `python3 serve-test-app.py`
2. **Check URL**: Access via `http://localhost:3000`, not file://
3. **Clear browser cache** and try again

## 📊 Step 9: Verify Metrics and Health

### 9.1 Health Checks

```bash
# Keycloak health
curl http://localhost:8080/health

# Keycloak readiness
curl http://localhost:8080/health/ready

# Keycloak liveness
curl http://localhost:8080/health/live
```

### 9.2 Metrics

```bash
# View metrics
curl http://localhost:8080/metrics | grep bioid

# Or in browser
http://localhost:8080/metrics
```

**Expected metrics**:
- `bioid_enrollment_total`
- `bioid_verification_total`
- `bioid_verification_success_total`

## ✅ Step 10: Validation Checklist

After completing all tests, verify:

- [ ] **Extension loads**: BioID extension initializes without errors
- [ ] **Realm configured**: bioid-demo realm imported successfully
- [ ] **User authentication**: Can login with demouser/demo123
- [ ] **Face enrollment**: First-time users can enroll their face
- [ ] **Face authentication**: Enrolled users can authenticate with face
- [ ] **Account management**: Users can manage face credentials
- [ ] **Admin configuration**: Administrators can configure settings
- [ ] **Connectivity**: BioID service connection works
- [ ] **Metrics**: Face authentication metrics are collected
- [ ] **Health checks**: All health endpoints respond correctly

## 🎉 Success Criteria

Your BioID face authentication system is working correctly if:

1. **Users can complete the full OIDC flow** from test app → Keycloak → back to test app
2. **Face enrollment works** for new users
3. **Face authentication works** for enrolled users
4. **Account management** allows users to manage their face credentials
5. **Admin console** allows configuration of face recognition settings
6. **Metrics and health checks** provide system monitoring

## 📚 Additional Resources

- **Detailed Setup**: [SETUP.md](SETUP.md)
- **Manual Configuration**: [MANUAL_SETUP.md](MANUAL_SETUP.md)
- **Troubleshooting**: [TESTING_FACE_AUTH.md](TESTING_FACE_AUTH.md)
- **Build Issues**: [BUILD_ISSUES.md](BUILD_ISSUES.md)

## 🆘 Getting Help

If you encounter issues:

1. **Check logs**: `docker compose logs keycloak | grep -i bioid`
2. **Verify configuration**: Review `.env` file and realm settings
3. **Test connectivity**: Use admin console connectivity test
4. **Review documentation**: Check the specific guides for your issue

---

**🎯 Remember**: The test application demonstrates the **correct OIDC flow**. The account management redirect behavior you might have seen earlier is normal - this test app shows how to properly initiate authentication from your own application.