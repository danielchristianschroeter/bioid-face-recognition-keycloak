# Keycloak BioID Face Recognition Extension - Setup Guide

This guide provides step-by-step instructions for setting up and configuring the Keycloak BioID Face Recognition Extension using Docker Compose.

## Prerequisites

1. **Docker and Docker Compose** installed on your system
2. **BioID BWS Account** - Register at [https://bwsportal.bioid.com/register](https://bwsportal.bioid.com/register)
3. **Java 21+** and **Maven 3.8+** (for building from source)

> 🪟 **Windows Users**: See [WINDOWS-SETUP.md](WINDOWS-SETUP.md) for Windows-specific setup instructions and troubleshooting.

## Quick Start

### 1. Get BioID Credentials

1. Register for a BioID BWS account at [https://bwsportal.bioid.com/register](https://bwsportal.bioid.com/register)
2. Create a new application in the BWS portal
3. Note down your `Client ID` and `Secret Key`

### 2. Clone and Build

```bash
# Clone the repository
git clone <repository-url>
cd bioid-face-recognition-keycloak

# Build the extension
mvn clean package

# Verify the JAR was created
ls -la deployment/target/keycloak-bioid-extension-*.jar
```

### 3. Configure Environment

```bash
# Copy the environment template
cp .env.example .env

# Edit .env with your BioID credentials
nano .env
```

**Required configuration in `.env`:**
```bash
BWS_CLIENT_ID=your_actual_client_id
BWS_KEY=your_actual_secret_key
```

### 4. Start Services

```bash
# Start Keycloak with PostgreSQL
docker compose up -d

# Check service status
docker compose ps

# View logs
docker compose logs -f keycloak
```

**Verify Setup**:
```bash
# Use the verification script (Linux/Mac)
./verify-setup.sh

# Or on Windows
verify-setup.bat

# Manual verification
curl -s http://localhost:8080/health/ready
```

### 5. Access Keycloak

- **Keycloak Admin Console**: [http://localhost:8080](http://localhost:8080)
- **Default Admin Credentials**: `admin` / `admin123`
- **Health Check**: [http://localhost:8080/health](http://localhost:8080/health)

### 6. Manual Realm Setup

After Keycloak is running, you need to manually set up a realm for face recognition:

#### Option A: Import Pre-configured Realm (Recommended)

1. **Access Admin Console**: Go to [http://localhost:8080](http://localhost:8080) and login with `admin` / `admin123`

2. **Import Realm**: 
   - Click the dropdown next to "Master" in the top-left
   - Click "Create Realm"
   - Click "Browse" and select `docker/keycloak/bioid-demo-realm.json`
   - Click "Create"

3. **Verify Import**: The `bioid-demo` realm should now be available with:
   - **Demo User**: `demouser` / `demo123`
   - **Demo Client**: `bioid-demo-client` (public client)
   - **Face Authentication**: Pre-configured in custom browser flow
   - **Face Enrollment**: Set as required action

#### Option B: Manual Realm Configuration

If you prefer to set up everything manually:

1. **Create New Realm**:
   - Click "Create Realm"
   - Enter realm name (e.g., `bioid-demo`)
   - Click "Create"

2. **Configure Authentication Flow**:
   - Go to "Authentication" → "Flows"
   - Duplicate the "Browser" flow and name it "Custom Browser"
   - Add "Face Authenticator" execution after "Username Password Form"
   - Set "Face Authenticator" to "Alternative"
   - Go to "Bindings" and set "Browser Flow" to "Custom Browser"

3. **Create Test User**:
   - Go to "Users" → "Add user"
   - Set username (e.g., `demouser`) and enable user
   - Go to "Credentials" tab and set password
   - Go to "Required Actions" and add "Face Enrollment"

4. **Create Test Client**:
   - Go to "Clients" → "Create client"
   - Set Client ID (e.g., `bioid-demo-client`)
   - Set Client type to "OpenID Connect"
   - Enable "Standard flow" and "Direct access grants"
   - Set valid redirect URIs (e.g., `http://localhost:3000/*`)

### Important URLs (After Realm Setup)

- **Admin Console (Master Realm)**: `http://localhost:8080/admin/master/console/`
- **Admin Console (Demo Realm)**: `http://localhost:8080/admin/master/console/#/bioid-demo`
- **Demo Realm Account Console**: `http://localhost:8080/realms/bioid-demo/account`
- **Demo Realm Login**: `http://localhost:8080/realms/bioid-demo/protocol/openid-connect/auth`

### 7. Test the Face Authentication

After setting up the realm, test the face authentication functionality:

#### Quick Test with Account Console

1. Go to `http://localhost:8080/realms/bioid-demo/account`
2. Login with `demouser` / `demo123`
3. Complete face enrollment when prompted
4. Logout and login again to test face authentication

#### Account Management Features

Users can manage their face authentication settings through the account console:

**Face Credential Management:**
- **Enable/Disable Face Authentication**: Users can toggle face recognition on/off
- **Re-enroll Face**: Replace existing face templates with new ones
- **Delete Face Data**: Permanently remove all biometric data
- **View Credential Status**: See enrollment date, last used, and expiration

**Security Settings:**
- **Require Face Authentication**: Force face auth (disable password fallback)
- **Allow Password Fallback**: Enable backup authentication methods
- **Security Level Indicators**: Visual feedback on account security level

**Access Account Management:**
- Direct URL: `http://localhost:8080/realms/bioid-demo/account/#/face-credentials`
- From test app: Click "Manage Account & Face Enrollment" after login
- From admin console: User can access via account management links

#### Full Test with Test Application

1. **Start the test server**:
   ```bash
   python3 serve-test-app.py
   ```

2. **Open the test app**: `http://localhost:3000/test-app.html`

3. **Click "Login with Face Authentication"**

4. **Complete the flow**:
   - Login with username/password
   - Enroll your face when prompted
   - Test face authentication on subsequent logins

5. **Verify account management** by clicking the account management links in the test app

## Testing Troubleshooting

### Test App Issues

**Problem**: "Invalid redirect URI" error
- **Solution**: Make sure the client's "Valid redirect URIs" includes `http://localhost:3000/*`

**Problem**: CORS errors in browser console
- **Solution**: The test server includes CORS headers, make sure you're using `python3 serve-test-app.py`

**Problem**: "Client not found" error
- **Solution**: Verify the client ID is `bioid-demo-client` and the client exists in the `bioid-demo` realm

**Problem**: Face enrollment not working
- **Solution**: 
  - Check camera permissions in your browser
  - Verify BioID credentials are correct in `.env`
  - Check Keycloak logs: `docker compose logs keycloak | grep -i bioid`

**Problem**: Account management shows 401/403 errors
- **Solution**: This usually means the realm wasn't set up correctly. Try the manual setup approach in [MANUAL_SETUP.md](MANUAL_SETUP.md)

## Detailed Configuration

### Environment Variables

#### Required Configuration
| Variable | Default | Description |
|----------|---------|-------------|
| `BWS_CLIENT_ID` | *Required* | Your BioID BWS Client ID |
| `BWS_KEY` | *Required* | Your BioID BWS Secret Key |

#### BioID Service Configuration
| Variable | Default | Description |
|----------|---------|-------------|
| `BWS_ENDPOINT` | `https://face.bws-eu.bioid.com` | BioID service endpoint |
| `BWS_JWT_EXPIRE_MINUTES` | `60` | JWT token expiration time |
| `VERIFICATION_THRESHOLD` | `0.015` | Face verification threshold (0.0-1.0, lower = more strict) |
| `VERIFICATION_MAX_RETRIES` | `3` | Maximum verification retries |
| `VERIFICATION_TIMEOUT_SECONDS` | `4` | Verification timeout |
| `ENROLLMENT_TIMEOUT_SECONDS` | `7` | Enrollment timeout |

#### Template Management
| Variable | Default | Description |
|----------|---------|-------------|
| `TEMPLATE_TTL_DAYS` | `730` | Template expiration in days |
| `TEMPLATE_CLEANUP_INTERVAL_HOURS` | `24` | Cleanup interval |
| `TEMPLATE_TYPE` | `STANDARD` | Template type |
| `TEMPLATE_ENCRYPTION_ENABLED` | `true` | Enable template encryption |

#### Liveness Detection Configuration
| Variable | Default | Description |
|----------|---------|-------------|
| `LIVENESS_ACTIVE_ENABLED` | `true` | Enable active liveness detection (2 images) |
| `LIVENESS_CHALLENGE_RESPONSE_ENABLED` | `false` | Enable challenge-response mode |
| `LIVENESS_CONFIDENCE_THRESHOLD` | `0.5` | Liveness confidence threshold (0.0-1.0) |
| `LIVENESS_MAX_OVERHEAD_MS` | `200` | Maximum liveness processing overhead |
| `LIVENESS_ADAPTIVE_MODE` | `false` | Enable adaptive liveness mode |
| `LIVENESS_FALLBACK_TO_PASSWORD` | `false` | Allow password fallback |
| `LIVENESS_CHALLENGE_COUNT` | `1` | Number of challenge directions |
| `LIVENESS_CHALLENGE_TIMEOUT_SECONDS` | `30` | Challenge timeout |

#### Regional and Network Configuration
| Variable | Default | Description |
|----------|---------|-------------|
| `REGIONAL_PREFERRED_REGION` | `EU` | Preferred region (EU, US, SA) |
| `REGIONAL_DATA_RESIDENCY_REQUIRED` | `false` | Require data residency |
| `REGIONAL_FAILOVER_ENABLED` | `true` | Enable regional failover |
| `REGIONAL_LATENCY_THRESHOLD_MS` | `1000` | Latency threshold for region selection |
| `GRPC_CHANNEL_POOL_SIZE` | `5` | gRPC connection pool size |
| `GRPC_KEEP_ALIVE_TIME_SECONDS` | `30` | gRPC keep-alive time |
| `GRPC_RETRY_MAX_ATTEMPTS` | `3` | Maximum gRPC retry attempts |
| `GRPC_RETRY_BACKOFF_MULTIPLIER` | `2.0` | Retry backoff multiplier |

#### Keycloak Configuration
| Variable | Default | Description |
|----------|---------|-------------|
| `KEYCLOAK_ADMIN` | `admin` | Keycloak admin username |
| `KEYCLOAK_ADMIN_PASSWORD` | `admin123` | Keycloak admin password |
| `KEYCLOAK_ADMIN_PASSWORD` | `admin123` | Keycloak admin password |

### User-Level Configuration

Users can configure their face authentication preferences through the account management console:

**Face Authentication Settings:**
- **Enable/Disable**: Toggle face recognition for their account
- **Require Face Auth**: Make face authentication mandatory (disables password fallback)
- **Allow Fallback**: Enable password authentication as backup
- **Re-enrollment**: Replace existing face templates
- **Data Deletion**: Permanently remove biometric data

**User Attributes (stored in Keycloak):**
- `face.auth.enabled`: Boolean - Whether face auth is enabled for the user
- `face.auth.required`: Boolean - Whether face auth is required (no fallback)
- `face.auth.fallback.enabled`: Boolean - Whether password fallback is allowed

**Access Methods:**
- Account Console: `http://localhost:8080/realms/{realm}/account/#/face-credentials`
- REST API: `http://localhost:8080/realms/{realm}/account/face-credentials`
- Admin Console: User management → User details → Attributes

### BioID Configuration File

The extension configuration is located at `docker/keycloak/bioid.properties`. Key settings:

```properties
# Verification threshold (lower = more strict)
verification.threshold=0.015

# Maximum retry attempts
verification.maxRetries=3

# Template time-to-live
template.ttl.days=730

# Liveness detection settings
liveness.enabled=true
liveness.passive.enabled=true
liveness.active.enabled=false
liveness.confidenceThreshold=0.5

# Regional settings
regional.preferredRegion=EU
regional.failoverEnabled=true
```

## Setting Up Face Authentication

### 1. Create a Realm

1. Access Keycloak Admin Console: [http://localhost:8080](http://localhost:8080)
2. Login with admin credentials
3. Click "Create Realm"
4. Name: `bioid-demo`
5. Click "Create"

### 2. Configure Authentication Flow

1. Go to **Authentication** → **Flows**
2. Click "Create flow"
3. Name: `Face Authentication Flow`
4. Type: `Basic flow`
5. Click "Create"

6. In the new flow:
   - Click "Add execution"
   - Select "Face Authenticator"
   - Set requirement to "REQUIRED"

### 3. Configure Face Enrollment

1. Go to **Authentication** → **Required Actions**
2. Search "Face Recognition Enrollment" and enable the Toggle
3. Set as "Default Action" if desired

### 4. Create a Test User

The demo realm includes a pre-configured test user:
- **Username**: `demo`
- **Password**: `demo123`
- **Email**: `demo@example.com`

Alternatively, create a new test user:
1. Go to **Users** → "Create new user"
2. Username: `testuser`
3. Email: `test@example.com`
4. Click "Create"

5. Set password:
   - Go to **Credentials** tab
   - Click "Set password"
   - Password: `password123`
   - Temporary: `Off`

### 5. Test Face Enrollment

1. Open a new browser/incognito window
2. Go to the **bioid-demo** realm account console: `http://localhost:8080/realms/bioid-demo/account`
3. Login with the demo user: `demo` / `demo123`
4. You should be prompted for face enrollment
5. Allow camera access and follow the enrollment process

**Important**: Make sure you're accessing the `bioid-demo` realm, not the `master` realm. The demo user only exists in the `bioid-demo` realm.

### 6. Test Client Application

The demo realm includes a pre-configured client for testing:
- **Client ID**: `bioid-demo-client`
- **Client Secret**: `demo-client-secret`
- **Redirect URIs**: `http://localhost:8080/*`, `https://localhost:8443/*`

You can use this client to test the authentication flow in your applications.

## Monitoring and Metrics

### Enable Monitoring Stack

```bash
# Start with monitoring services
docker compose --profile monitoring up -d

# Access monitoring services
# Prometheus: http://localhost:9090
# Grafana: http://localhost:3000 (admin/admin)
```

### Available Metrics

The extension exposes metrics at `http://localhost:9000/metrics`:

- `bioid_enrollment_total` - Total face enrollments
- `bioid_verification_total` - Total face verifications
- `bioid_verification_success_total` - Successful verifications
- `bioid_verification_failure_total` - Failed verifications
- `bioid_liveness_detection_total` - Liveness detection attempts
- `bioid_template_cleanup_total` - Template cleanup operations

## Troubleshooting

### Common Issues

#### 1. Extension Not Loading

**Symptoms**: No face authentication options in Keycloak

**Solutions**:
```bash
# Check if JAR exists
ls -la deployment/target/keycloak-bioid-extension-*.jar

# Rebuild if missing
mvn clean package

# Check Keycloak logs
docker compose logs keycloak | grep -i bioid
```

#### 2. BioID Connection Issues

**Symptoms**: "Service unavailable" errors during enrollment/verification

**Solutions**:
```bash
# Verify credentials in .env file
cat .env | grep BWS_

# Check network connectivity
docker compose exec keycloak ping face.bws-eu.bioid.com

# Check BioID service status
curl -I https://face.bws-eu.bioid.com
```

#### 3. Camera Access Issues

**Symptoms**: Camera not working in enrollment

**Solutions**:
- Ensure HTTPS is enabled for production
- Check browser permissions
- Test with different browsers
- For development, use `localhost` (not IP addresses)

#### 4. User Not Found Error

**Symptoms**: "user_not_found" error when trying to login with demo user

**Solutions**:
- Ensure you're accessing the correct realm: `http://localhost:8080/realms/bioid-demo/account`
- The demo user (`demo`/`demo123`) exists only in the `bioid-demo` realm, not the `master` realm
- Check that the realm was imported correctly:
  ```bash
  # Check Keycloak logs for realm import
  docker compose logs keycloak | grep -i "import"
  ```

#### 5. Database Connection Issues

**Symptoms**: Keycloak fails to start

**Solutions**:
```bash
# Check PostgreSQL status
docker compose logs postgres

# Restart database
docker compose restart postgres

# Check database connectivity
docker compose exec keycloak pg_isready -h postgres -U keycloak
```

### Logs and Debugging

```bash
# View all logs
docker compose logs -f

# View specific service logs
docker compose logs -f keycloak
docker compose logs -f postgres

# Enable debug logging
# Add to docker compose.yml under keycloak environment:
KC_LOG_LEVEL: DEBUG
```

### Health Checks

```bash
# Keycloak health
curl http://localhost:8080/health

# Keycloak readiness
curl http://localhost:8080/health/ready

# Keycloak liveness
curl http://localhost:8080/health/live

# Extension metrics
curl http://localhost:9000/metrics
```

## Production Deployment

### Security Considerations

1. **HTTPS**: Always use HTTPS in production
2. **Credentials**: Use secure credential management (not .env files)
3. **Database**: Use external managed database
4. **Firewall**: Restrict access to admin interfaces
5. **Monitoring**: Set up proper monitoring and alerting

### Production Configuration

```yaml
# docker compose.prod.yml example
services:
  keycloak:
    environment:
      KC_HOSTNAME: your-domain.com
      KC_HOSTNAME_STRICT: true
      KC_HOSTNAME_STRICT_HTTPS: true
      KC_HTTP_ENABLED: false
      KC_HTTPS_CERTIFICATE_FILE: /opt/keycloak/conf/server.crt
      KC_HTTPS_CERTIFICATE_KEY_FILE: /opt/keycloak/conf/server.key
      KC_LOG_LEVEL: WARN
    volumes:
      - ./certs/server.crt:/opt/keycloak/conf/server.crt:ro
      - ./certs/server.key:/opt/keycloak/conf/server.key:ro
```

### Backup and Recovery

```bash
# Backup database
docker compose exec postgres pg_dump -U keycloak keycloak > backup.sql

# Restore database
docker compose exec -T postgres psql -U keycloak keycloak < backup.sql

# Backup configuration
tar -czf keycloak-config-backup.tar.gz docker/keycloak/
```

## Support

For issues and questions:

1. Check the [troubleshooting section](#troubleshooting)
2. Review Keycloak logs: `docker compose logs keycloak`
3. Check BioID service status
4. Verify configuration files

## Next Steps

1. **Customize UI**: Modify the face authentication templates
2. **Integration**: Integrate with your applications
3. **Monitoring**: Set up comprehensive monitoring
4. **Scaling**: Configure for high availability
5. **Security**: Implement production security measures