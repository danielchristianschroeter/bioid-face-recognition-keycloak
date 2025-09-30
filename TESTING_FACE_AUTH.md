# Testing BioID Face Authentication

This guide explains how to properly test your BioID face authentication system using the correct OIDC flow.

## The Problem You Were Experiencing

When you accessed the Keycloak account management page directly (`http://localhost:8080/realms/bioid-demo/account`), you were being redirected back to that same page after authentication. This is **expected behavior** because:

1. The account management page itself initiated the login flow
2. Keycloak stores the original destination (`redirect_uri`) 
3. After successful authentication, it sends you back to where you started

## The Solution: Proper OIDC Flow Testing

Instead of starting from the account page, you need to test with a proper client application that initiates the OIDC flow correctly.

## Quick Start

### 1. Start Your Keycloak Server
Make sure your Keycloak server is running with the BioID extensions:
```bash
# If using Docker Compose
docker-compose up

# Or your existing startup method
```

### 2. Start the Test Application
```bash
# Run the test server
test-face-auth.bat

# Or manually:
python serve-test-app.py
```

### 3. Test the Authentication Flow
1. Open your browser to `http://localhost:3000/test-app.html`
2. Click "Login with Face Authentication"
3. You'll be redirected to Keycloak
4. Enter credentials: `demouser` / `demo123`
5. Complete the face authentication step
6. You'll be redirected back to the test app with user information

## What This Test Demonstrates

### Correct OIDC Flow
1. **Client Application** (`http://localhost:3000`) initiates login
2. **Authorization Request** to Keycloak with proper `redirect_uri`
3. **User Authentication** (username/password + face verification)
4. **Authorization Code** returned to client
5. **Token Exchange** for access/ID tokens
6. **User Information** retrieved and displayed

### Your Face Authentication in Action
- Username/password form (first factor)
- Face authentication challenge (second factor)
- Conditional flow based on user enrollment
- Proper success/failure handling

## Test Scenarios

### 1. First-Time User (Face Enrollment)
- User without face enrollment will be prompted to enroll
- Test the enrollment flow
- Verify face data is stored correctly

### 2. Returning User (Face Authentication)
- User with existing face enrollment
- Test face verification process
- Verify authentication success/failure

### 3. Error Handling
- Test with wrong password
- Test with failed face verification
- Verify error messages and flow recovery

## Configuration Details

### Client Configuration
The test uses the `bioid-demo-client` configured in your realm:
- **Client ID**: `bioid-demo-client`
- **Redirect URIs**: `http://localhost:3000/*`
- **Flow**: Authorization Code with PKCE
- **Scopes**: `openid profile email`

### Authentication Flow
Your custom authentication flow:
1. `auth-cookie` (check existing session)
2. `auth-username-password-form` (first factor)
3. `Browser - Conditional Face Auth` (conditional second factor)
   - `conditional-user-configured` (check if face enrolled)
   - `face-authenticator` (face verification)

## Troubleshooting

### Common Issues

**"Invalid redirect URI"**
- Ensure Keycloak is updated with the new client configuration
- Restart Keycloak after updating the realm JSON

**"CORS errors"**
- The test server includes CORS headers
- Ensure you're accessing via `http://localhost:3000`

**"Face authentication not triggered"**
- Check if user has face enrollment
- Verify the conditional flow configuration
- Check Keycloak logs for authentication flow details

**"gRPC connection errors"**
- Verify BioID service is running and accessible
- Check the `bioid.properties` configuration
- Review connection logs in Keycloak

### Debugging Tips

1. **Check Browser Developer Tools**
   - Network tab shows OIDC requests/responses
   - Console shows any JavaScript errors

2. **Review Keycloak Logs**
   - Authentication flow execution
   - Face verification results
   - Error details

3. **Test Individual Components**
   - Use the standalone test files (`test-bioid-connection.java`, etc.)
   - Verify gRPC connectivity separately

## Next Steps

Once you've verified the authentication flow works correctly:

1. **Integrate with Your Application**
   - Use the same OIDC flow pattern
   - Implement proper token handling
   - Add session management

2. **Production Considerations**
   - Configure proper redirect URIs for your domain
   - Set up SSL/TLS certificates
   - Review security settings
   - Monitor authentication metrics

3. **User Experience**
   - Customize the face authentication UI
   - Add proper error handling
   - Implement enrollment guidance

## Additional Resources

- [Keycloak OIDC Documentation](https://www.keycloak.org/docs/latest/securing_apps/#_oidc)
- [OAuth 2.0 Authorization Code Flow](https://tools.ietf.org/html/rfc6749#section-4.1)
- [PKCE Specification](https://tools.ietf.org/html/rfc7636)

---

**Remember**: The account management page redirect behavior you observed is correct OIDC behavior. This test application demonstrates how to properly initiate authentication from your own application and receive the user back at the intended destination.