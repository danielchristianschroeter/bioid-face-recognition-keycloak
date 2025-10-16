# Quick Start - Testing Face Authentication

## 🚀 Fastest Way to Test

### 1. Start HTTP Server (Choose One)

**Python** (easiest):
```cmd
python -m http.server 3000
```

**Node.js**:
```cmd
npx http-server -p 3000
```

**Or just open the file**:
```cmd
start simple-test.html
```

### 2. Open in Browser

Navigate to: **http://localhost:3000/simple-test.html**

### 3. Test the Flow

1. Click **"Logout First"** (clears session)
2. Click **"Test Login (Force Fresh)"**
3. Login with username/password
4. Face authentication triggers
5. Follow the on-screen instructions
6. Success! ✅

## 📋 What You'll See

### Success
```
✅ Success! Got authorization code: abc123def456...
```

### Failure
```
❌ Error: access_denied
Description: Face authentication failed
```

## 🔗 Quick Links in the Test Page

The test page provides these shortcuts:

1. **Account Management** - Manage your account
2. **Face Enrollment** - Enroll or update face credentials
3. **Admin Console** - Admin settings

## 🐛 Common Issues

### "Invalid redirect URI"
**Fix**: Add `http://localhost:3000/*` to client redirect URIs in Admin Console

### "Realm not found"
**Fix**: Change `bioid-demo` to your realm name in `simple-test.html`

### Camera not working
**Fix**: Use `localhost` (not 127.0.0.1) and allow camera permissions

## 📊 Check if It's Working

### View Logs
```cmd
docker compose logs -f keycloak | findstr /i "face\|bioid"
```

### Expected Log Messages

**Enrollment**:
```
INFO  BWS enrollment successful for classId: 1762195670299
INFO  Face credential created successfully
```

**Authentication**:
```
INFO  BWS verification successful for classId: 1762195670299
INFO  Face authentication successful
```

**Deletion**:
```
INFO  disableCredentialType called
INFO  DELETE CREDENTIAL CALLED
INFO  BWS template deleted successfully
```

## 🎯 Testing Checklist

Quick checklist to verify everything works:

- [ ] Can open test page
- [ ] Can login with username/password
- [ ] Face authentication triggers
- [ ] Camera activates
- [ ] Can capture images
- [ ] Authentication succeeds
- [ ] Can delete credential
- [ ] BWS template deleted
- [ ] Can re-enroll

## 📚 More Information

- Full testing guide: `TESTING_GUIDE.md`
- Deployment guide: `DEPLOYMENT_READY.md`
- Troubleshooting: `WINDOWS_BUILD_ISSUES.md`

## 🎉 That's It!

You're now testing face authentication. If everything works, you're ready to deploy!
