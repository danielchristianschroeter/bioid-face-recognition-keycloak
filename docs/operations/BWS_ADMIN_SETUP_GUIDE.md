# BWS Admin Role - Quick Setup Guide

This guide walks you through setting up the BWS Admin role from scratch.

## Prerequisites

- Keycloak instance running
- BioID BWS account at https://bwsportal.bioid.com
- Admin access to Keycloak Admin Console

## Step 1: Get BWS Management API Credentials

### 1.1 Login to BWS Portal

1. Go to https://bwsportal.bioid.com
2. Login with your account
3. If you don't have an account, register at https://bwsportal.bioid.com/register

### 1.2 Get Your API Key

1. Click on your **user profile icon** (top right corner)
2. Click on your **email address**
3. You'll see your **Management API Key** displayed
4. Copy the API key (you'll need this later)

## Step 2: Configure Environment Variables

### 2.1 Update .env File

Open your `.env` file and add/update these values:

```bash
# BWS Management API Configuration
BWS_MANAGEMENT_API_URL=https://bwsportal.bioid.com/api
BWS_MANAGEMENT_EMAIL=your-email@example.com
BWS_MANAGEMENT_API_KEY=your-api-key-from-portal
BWS_MANAGEMENT_CLIENT_ID=${BWS_CLIENT_ID}
BWS_MANAGEMENT_JWT_EXPIRE_MINUTES=15
BWS_ADMIN_ENABLED=true
BWS_ADMIN_RATE_LIMIT_REQUESTS_PER_MINUTE=60
BWS_ADMIN_AUDIT_ENABLED=true
BWS_ADMIN_AUDIT_LOG_LEVEL=INFO
```

**Important:** Replace `your-email@example.com` and `your-api-key-from-portal` with your actual BWS Portal credentials.

### 2.2 Update bioid.properties (Alternative)

If you're using `bioid.properties` instead of environment variables:

```properties
# BWS Management API Configuration
bws.management.api.url=https://bwsportal.bioid.com/api
bws.management.email=your-email@example.com
bws.management.apiKey=your-api-key-from-portal
bws.management.clientId=${bioid.clientId}
bws.management.jwt.expireMinutes=15
bws.admin.enabled=true
bws.admin.rateLimit.requestsPerMinute=60
bws.admin.audit.enabled=true
bws.admin.audit.logLevel=INFO
```

## Step 3: Create bws-admin Role in Keycloak

### 3.1 Access Keycloak Admin Console

1. Open http://localhost:8080/admin
2. Login with admin credentials (default: `admin` / `admin123`)
3. Select your realm (e.g., `bioid-demo`)

### 3.2 Create the Role

1. Click **"Realm Roles"** in the left menu
2. Click **"Create role"** button
3. Fill in:
   - **Role name**: `bws-admin`
   - **Description**: `Administrator role for managing BWS face templates`
4. Click **"Save"**

## Step 4: Assign Role to Admin Users

### 4.1 Create or Select Admin User

**Option A: Create New Admin User**

1. Click **"Users"** in the left menu
2. Click **"Add user"**
3. Fill in:
   - **Username**: `admin-test`
   - **Email**: `admin@example.com`
   - **First name**: `Admin`
   - **Last name**: `User`
   - **Email verified**: ON
4. Click **"Create"**
5. Go to **"Credentials"** tab
6. Click **"Set password"**
7. Enter password (e.g., `admin123`)
8. Set **"Temporary"** to OFF
9. Click **"Save"**

**Option B: Use Existing User**

1. Click **"Users"** in the left menu
2. Search for and select your user

### 4.2 Assign bws-admin Role

1. With the user selected, click **"Role mapping"** tab
2. Click **"Assign role"** button
3. Find and select **`bws-admin`** from the list
4. Click **"Assign"**

You should now see `bws-admin` in the user's assigned roles.

## Step 5: Implement Backend Endpoints

The admin panel requires backend endpoints to securely access the BWS Management API.

### 5.1 Review Implementation Guide

See `docs/BWS_ADMIN_API.md` for complete backend implementation guide with:
- 7 REST API endpoints
- Java Spring Boot examples
- Security best practices
- Testing procedures

### 5.2 Required Endpoints

Implement these endpoints in your backend:

```
GET    /realms/{realm}/bws-admin/stats
GET    /realms/{realm}/bws-admin/templates
GET    /realms/{realm}/bws-admin/templates/{classId}
DELETE /realms/{realm}/bws-admin/templates/{classId}
GET    /realms/{realm}/bws-admin/validate
GET    /realms/{realm}/bws-admin/orphaned
DELETE /realms/{realm}/bws-admin/orphaned
```

### 5.3 Security Requirements

Each endpoint must:
1. Validate JWT token from Keycloak
2. Check for `bws-admin` role
3. Use BWS Management API credentials from environment
4. Log all admin actions
5. Return appropriate error responses

## Step 6: Test the Admin Panel

### 6.1 Start the Test Server

```bash
python -m http.server 3000
```

### 6.2 Login with Admin User

1. Open http://localhost:3000/test-app.html
2. Click **"Login with Face Authentication"**
3. Login with your admin user (e.g., `admin-test`)
4. Complete authentication

### 6.3 Verify Admin Panel Appears

After successful login, you should see:

- ✅ User information section
- ✅ Face credentials section
- ✅ **Admin Panel section** (new!)

The Admin Panel includes:
- 📊 Template Statistics
- 📋 View All Enrolled Faces button
- ✅ Validate Templates button
- 🔍 Find Orphaned Templates button

### 6.4 Test Admin Functions

**Note:** Until you implement the backend endpoints, you'll see helpful messages explaining what's needed.

1. Click **"View All Enrolled Faces"**
   - Should show message about backend service requirement
   - Includes example endpoint and response format

2. Click **"Validate Templates"**
   - Shows validation requirements
   - Explains what the backend should do

3. Click **"Find Orphaned Templates"**
   - Explains orphaned template detection
   - Shows expected functionality

## Step 7: Verify Role-Based Access

### 7.1 Test with Non-Admin User

1. Logout from test-app.html
2. Login with a regular user (without `bws-admin` role)
3. Verify that:
   - ✅ User info section appears
   - ✅ Face credentials section appears
   - ❌ Admin panel does NOT appear

### 7.2 Test with Admin User

1. Logout
2. Login with admin user
3. Verify that:
   - ✅ User info section appears
   - ✅ Face credentials section appears
   - ✅ Admin panel DOES appear

## Step 8: Implement Backend (Production)

For production deployment, implement the backend service:

### 8.1 Create Backend Service

See `docs/BWS_ADMIN_API.md` for complete Java Spring Boot implementation.

Key components:
- `BWSManagementService` - Calls BWS Management API
- `KeycloakAdminService` - Queries Keycloak users
- `BWSAdminController` - REST endpoints
- `AuditService` - Logs admin actions

### 8.2 Configure Security

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/realms/*/bws-admin/**")
                    .hasRole("bws-admin")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter()))
            );
        return http.build();
    }
}
```

### 8.3 Deploy Backend

1. Build the backend service
2. Deploy to your server
3. Configure environment variables
4. Test endpoints with admin user
5. Monitor audit logs

## Troubleshooting

### Issue: Admin Panel Not Visible

**Symptoms:** Admin panel doesn't appear after login

**Possible Causes:**
1. User doesn't have `bws-admin` role
2. Role not properly assigned
3. Token doesn't include role claim

**Solutions:**
1. Verify role assignment in Keycloak Admin Console
2. Check JWT token payload (visible in test-app.html)
3. Ensure realm roles are included in token

### Issue: "Backend Service Required" Message

**Symptoms:** Clicking admin buttons shows backend requirement message

**Cause:** Backend endpoints not implemented yet

**Solution:** This is expected! Implement backend endpoints per `docs/BWS_ADMIN_API.md`

### Issue: BWS Management API Authentication Failed

**Symptoms:** Backend returns authentication errors

**Possible Causes:**
1. Invalid email or API key
2. API key expired
3. Wrong BWS Portal account

**Solutions:**
1. Verify credentials in BWS Portal user profile
2. Regenerate API key if needed
3. Check email matches BWS Portal account

### Issue: "Access Denied" Error

**Symptoms:** Backend returns 403 Forbidden

**Possible Causes:**
1. Backend not validating role correctly
2. JWT token missing role claim
3. Role name mismatch

**Solutions:**
1. Check backend role validation logic
2. Verify JWT includes `realm_access.roles` claim
3. Ensure role name is exactly `bws-admin`

## Configuration Checklist

Use this checklist to verify your setup:

```
☐ BWS Portal account created
☐ BWS Management API key obtained
☐ .env file updated with BWS Management API credentials
☐ bws-admin role created in Keycloak
☐ Admin user created or selected
☐ bws-admin role assigned to admin user
☐ Test server running (python -m http.server 3000)
☐ Admin user can login to test-app.html
☐ Admin panel appears for admin user
☐ Admin panel does NOT appear for regular users
☐ Backend endpoints implemented (for production)
☐ Backend validates bws-admin role
☐ Audit logging configured
☐ HTTPS enabled (for production)
```

## Next Steps

1. ✅ Complete this setup guide
2. 📖 Read `docs/BWS_ADMIN_API.md` for backend implementation
3. 💻 Implement backend endpoints
4. 🧪 Test all admin functions
5. 📊 Monitor audit logs
6. 🚀 Deploy to production with HTTPS

## Security Reminders

⚠️ **Important Security Notes:**

1. **Never commit API keys** to version control
2. **Use environment variables** for sensitive configuration
3. **Implement backend proxy** - don't expose API keys to client
4. **Enable audit logging** for compliance
5. **Use HTTPS** in production
6. **Implement rate limiting** to prevent abuse
7. **Monitor admin actions** for suspicious activity
8. **Rotate API keys** regularly
9. **Use strong passwords** for admin accounts
10. **Enable MFA** for admin users (if available)

## Support

If you encounter issues:

1. Check `docs/BWS_ADMIN_API.md` for implementation details
2. Review `docs/KEYCLOAK_CLIENT_SETUP.md` for Keycloak configuration
3. Check browser console for JavaScript errors
4. Review backend logs for API errors
5. Verify BWS Portal credentials
6. Test with curl to isolate issues

## References

- [BWS Management API Documentation](https://bwsportal.bioid.com/api/)
- [BWS Portal](https://bwsportal.bioid.com)
- [Backend Implementation Guide](./BWS_ADMIN_API.md)
- [Keycloak Client Setup](./KEYCLOAK_CLIENT_SETUP.md)
- [Admin Role Summary](./BWS_ADMIN_ROLE_SUMMARY.md)
