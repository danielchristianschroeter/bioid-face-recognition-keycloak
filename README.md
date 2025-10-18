# Keycloak BioID Face Recognition Extension

A comprehensive Keycloak extension that integrates BioID's face recognition technology for biometric authentication, enrollment, and user management. This extension provides secure, GDPR-compliant face recognition capabilities with comprehensive administrative tools.

## Features

### Core Authentication
- Face-based user authentication with configurable verification thresholds
- Multi-frame enrollment process with quality validation
- Automatic retry logic with fallback to traditional authentication
- Regional endpoint support with automatic failover
- gRPC-based communication with BioID BWS 3 service

### Liveness Detection
- **Passive liveness detection** - Automatic detection without user interaction (< 200ms overhead)
- **Active smile detection** - User interaction with smile challenge (< 500ms processing)
- **Challenge-response detection** - Head movement challenges for highest security (< 1000ms)
- **Combined detection** - Multiple methods for maximum security with adaptive experience
- Configurable confidence thresholds and risk-based enforcement

### Security & Privacy
- **Secure credential storage** with encryption and secure memory handling
- **TLS configuration** with mutual TLS support for enhanced security
- **Input validation** for images, metadata, and file paths to prevent attacks
- **Rate limiting** to prevent abuse and DoS attacks
- **Privacy protection** with GDPR-compliant data retention policies
- **Audit logging** for all biometric operations and privacy events
- **Zero persistence** of raw biometric data with immediate cleanup

### Administrative Management
- **Bulk operations** for user management and template operations
- **Template lifecycle management** with automatic cleanup and health monitoring
- **Liveness service management** with real-time configuration
- **Admin dashboard** with comprehensive metrics and status information
- **Health checks** and connectivity testing for BioID services

### User REST API
- **Template status API** - View enrollment status and metadata
- **Enrolled images API** - Retrieve thumbnail images from BWS
- **Credential management API** - Delete face credentials programmatically
- **Full CORS support** - Works from any origin for easy integration
- **Bearer token authentication** - Secure access with OIDC tokens

### Mobile Responsive UI
- **Fully responsive design** - Optimized for mobile phones, tablets, and desktop
- **Touch-optimized controls** - Large touch targets and gesture support
- **Adaptive layouts** - Portrait and landscape orientation support
- **Mobile camera handling** - Optimized video capture and face detection
- **Progressive enhancement** - Works on all modern mobile browsers
- **Accessibility features** - Screen reader support and keyboard navigation

### Monitoring and Observability
- **Comprehensive metrics** via Micrometer integration
- **Health check endpoints** for readiness and liveness probes
- **Connection monitoring** with pool metrics and performance tracking
- **Prometheus metrics export** for monitoring integration
- **Structured logging** with operation summaries and audit trails

## Architecture

The extension is built as a multi-module Maven project:

```
keycloak-bioid-extension/
├── bioid-client/           # BioID gRPC client and core services
├── face-authenticator/     # Authentication flow integration
├── face-credential/        # Credential provider implementation
├── face-enroll-action/     # Required action for enrollment
├── ui-components/          # Admin UI and REST endpoints
├── deployment/             # Final JAR assembly and packaging
└── integration-tests/      # Integration and end-to-end tests
```

## Prerequisites

- Java 21 or higher
- Maven 3.8 or higher
- Keycloak 26.0 or higher
- BioID BWS 3 account and credentials
- Docker and Docker Compose (for development)

## Quick Start with Docker Compose

📖 **For complete setup instructions, troubleshooting, and configuration details, see [SETUP.md](SETUP.md)**  
🪟 **Windows developers**: See [WINDOWS-SETUP.md](WINDOWS-SETUP.md) for Windows-specific instructions  
🔧 **Manual setup**: See [MANUAL_SETUP.md](MANUAL_SETUP.md) for manual realm configuration without automatic import  
🧹 **Clean start**: Use `./clean-setup.sh` (Linux/Mac) or `clean-setup.bat` (Windows) to reset everything  
🧪 **Test setup**: Use `./test-face-auth.sh` (Linux/Mac) or `test-face-auth.bat` (Windows) to verify your setup  
⚠️ **Build notes**: Some integration tests may have compilation issues due to gRPC code generation timing. Use `-DskipTests` for clean builds.

The easiest way to get started is using the provided Docker Compose setup:

### 1. Clone and Build

```bash
git clone <repository-url>
cd keycloak-bioid-extension
mvn clean package -DskipTests
```

> **Note**: Some integration tests may have compilation issues due to gRPC code generation timing. The extension builds and works perfectly when using `-DskipTests`.

### 2. Configure BioID Credentials

Create a `.env` file in the project root:

```bash
# BioID Configuration
BWS_CLIENT_ID=your-bioid-client-id
BWS_KEY=your-bioid-key
BWS_ENDPOINT=.bws-eu.bioid.com

# Keycloak Configuration
KEYCLOAK_ADMIN=admin
KEYCLOAK_ADMIN_PASSWORD=admin123
```

### 3. Start the Development Environment

```bash
docker compose up -d
```

This will start:
- Keycloak with the face recognition extension pre-loaded
- PostgreSQL database for Keycloak
- Automatic extension deployment and configuration

### 4. Access the System

- Keycloak Admin Console: http://localhost:8080/admin (admin/admin123)
- Health Check: http://localhost:8080/health
- Metrics: http://localhost:8080/metrics

### 5. Set Up Demo Realm

After Keycloak is running, manually import the demo realm:

1. Go to http://localhost:8080/admin
2. Login with `admin` / `admin123`
3. Click "Create Realm" → "Browse" → Select `docker/keycloak/bioid-demo-realm.json`
4. Click "Create"

The demo realm includes:
- Pre-configured face authentication flow
- Demo user with face enrollment required action
- Test client for integration testing

## Manual Installation

### 1. Build the Extension

```bash
mvn clean package
```

The built extension JAR will be located at:
`deployment/target/keycloak-bioid-extension-1.0.0-SNAPSHOT.jar`

### 2. Deploy to Keycloak

Copy the JAR to your Keycloak providers directory:

```bash
cp deployment/target/keycloak-bioid-extension-1.0.0-SNAPSHOT.jar \
   /opt/keycloak/providers/
```

### 3. Configure BioID Settings

Create `/opt/keycloak/conf/bioid.properties`:

```properties
# BioID Service Configuration
bioid.clientId=your-client-id
bioid.key=your-key
bioid.endpoint=.bws-eu.bioid.com

# Verification Settings
bioid.verificationThreshold=0.015
bioid.maxRetries=3

# Liveness Detection
bioid.livenessEnabled=true
bioid.livenessPassiveEnabled=true
bioid.livenessActiveEnabled=false
bioid.livenessChallengeResponseEnabled=false
bioid.livenessConfidenceThreshold=0.5
bioid.livenessMaxOverheadMs=200

# Regional Settings
bioid.preferredRegion=EU
bioid.dataResidencyRequired=false
bioid.failoverEnabled=true

# Connection Settings
bioid.channelPoolSize=5
bioid.keepAliveTimeSeconds=30
bioid.verificationTimeoutSeconds=10
bioid.enrollmentTimeoutSeconds=30

# Template Management
bioid.templateTtlDays=365
```

### 4. Restart Keycloak

```bash
/opt/keycloak/bin/kc.sh start
```

## Configuration

### Realm Configuration

1. Navigate to the Keycloak Admin Console
2. Select your realm
3. Go to "Authentication" > "Required Actions"
4. Enable "Face Enrollment" action
5. Go to "Authentication" > "Flows"
6. Create or modify an authentication flow to include "Face Authentication"

### Face Recognition Settings

1. In the Admin Console, navigate to "Realm Settings"
2. Click on the "Face Recognition" tab
3. Configure verification thresholds, retry limits, and liveness detection
4. Test connectivity to the BioID service
5. Save the configuration

### User Enrollment

Users can enroll their face biometrics through:

1. **Account Console**: Users can manage their biometric credentials
2. **Required Action**: Automatic enrollment during first login
3. **Admin Interface**: Administrators can manage user enrollments

## Testing the Extension

### Test Applications

The project includes two test applications for quick testing:

#### simple-test.html
A minimal test page that demonstrates the authentication flow without requiring CORS configuration:

```bash
# Serve the test page
python -m http.server 3000

# Or use Node.js
npx http-server -p 3000

# Open in browser
http://localhost:3000/simple-test.html
```

**Features:**
- No CORS configuration needed
- Direct authorization code flow
- Quick login/logout testing
- Minimal dependencies

#### test-app.html
A comprehensive test application with full token exchange and user info display:

```bash
# Start test server
cd test-server
npm install
npm start

# Open in browser
http://localhost:3000/test-app.html
```

**Features:**
- Complete OIDC flow with token exchange
- User information display
- Template status and enrolled images via REST API
- Face credential management
- Requires CORS configuration (see [KEYCLOAK_CLIENT_SETUP.md](docs/KEYCLOAK_CLIENT_SETUP.md))

**CORS Setup for test-app.html:**
1. Go to Admin Console → Clients → bioid-demo-client → Settings
2. Add to "Web origins": `http://localhost:3000` or `+`
3. Save and restart Keycloak

### Quick Testing Guide

See [QUICK_START_TESTING.md](docs/QUICK_START_TESTING.md) for a step-by-step testing guide.

## API Endpoints

### User-Facing REST API

```
GET    /realms/{realm}/face-api/status          # Get template status and enrolled images
DELETE /realms/{realm}/face-api/template        # Delete face credentials
```

**Authentication:** Requires valid access token (Bearer authentication)

**Example:**
```bash
# Get template status with enrolled images
curl -H "Authorization: Bearer $ACCESS_TOKEN" \
  http://localhost:8080/realms/bioid-demo/face-api/status

# Delete face credentials
curl -X DELETE \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  http://localhost:8080/realms/bioid-demo/face-api/template
```

### Administrative Endpoints

```
GET    /admin/realms/{realm}/face-recognition/config
PUT    /admin/realms/{realm}/face-recognition/config
POST   /admin/realms/{realm}/face-recognition/test-connectivity
GET    /admin/realms/{realm}/face-recognition/metrics
GET    /admin/realms/{realm}/face-recognition/health
```

### Bulk Operations & Template Management

```
GET    /admin/realms/{realm}/face-recognition/bulk-operations
POST   /admin/realms/{realm}/face-recognition/bulk-operations
GET    /admin/realms/{realm}/face-recognition/bulk-operations/{id}
POST   /admin/realms/{realm}/face-recognition/bulk-operations/{id}/cancel
GET    /admin/realms/{realm}/face-recognition/templates
POST   /admin/realms/{realm}/face-recognition/templates/cleanup
GET    /admin/realms/{realm}/face-recognition/templates/health
```

### Liveness Detection Management

```
GET    /admin/realms/{realm}/face-recognition/liveness/config
PUT    /admin/realms/{realm}/face-recognition/liveness/config
POST   /admin/realms/{realm}/face-recognition/liveness/test
GET    /admin/realms/{realm}/face-recognition/liveness/statistics
```

### Health and Metrics

```
GET    /health/ready
GET    /health/live
GET    /metrics
```

## Development

### Building from Source

```bash
# Clean build
mvn clean compile

# Run tests
mvn test

# Package extension
mvn package

# Skip integration tests (requires BioID credentials)
mvn package -DskipTests
```

### Dependency Management

Keep dependencies up to date for security and performance:

```bash
# Check for dependency updates (Linux/Mac)
./scripts/check-dependencies.sh

# Check for dependency updates (Windows)
scripts\check-dependencies.bat

# Update dependencies safely (Linux/Mac)
./scripts/update-dependencies.sh

# Manual dependency checks
mvn versions:display-dependency-updates
mvn org.owasp:dependency-check-maven:check
```

**Automated Updates:**
- Dependabot runs weekly to check for updates
- Security scans run on every PR and weekly
- See `DEPENDENCY_MANAGEMENT.md` for quick reference
- See `docs/DEVELOPMENT.md` for detailed dependency management guide

### Running Tests

The project includes comprehensive tests:

```bash
# Unit tests (no external dependencies)
mvn test -Dtest=SimpleCompilationTest

# Integration tests (requires BioID configuration)
mvn test -Dtest=DeletionRequestServiceTest

# All tests
mvn test
```

Note: Integration tests require proper BioID configuration and may fail in isolated environments.

### Development Environment

Use the Docker Compose setup for development:

```bash
# Start development environment
docker compose up -d

# View logs
docker compose logs -f keycloak

# Rebuild and redeploy
mvn clean package && docker compose restart keycloak

# Stop environment
docker compose down
```

### Code Structure

```
bioid-client/src/main/java/com/bioid/keycloak/client/
├── admin/                  # Admin services and bulk operations
├── auth/                   # JWT token provider for BioID authentication
├── config/                 # Configuration management and validation
├── connection/             # gRPC connection management and pooling
├── health/                 # Health check providers and status monitoring
├── liveness/               # Liveness detection services and configuration
├── metrics/                # Metrics collection and Prometheus export
├── privacy/                # GDPR compliance and data retention
├── security/               # Security validation, TLS, and credential storage
└── tracing/                # Distributed tracing and span management
```

## Monitoring

### Metrics

The extension exposes comprehensive metrics via Micrometer:

- **Authentication Metrics**: Success/failure rates, verification latency
- **Connection Metrics**: Pool utilization, connection health, gRPC call metrics
- **Admin Metrics**: Bulk operation progress, template management statistics
- **Security Metrics**: Rate limiting, validation failures, security events
- **Liveness Metrics**: Detection success rates, method performance, confidence scores
- **Privacy Metrics**: Data retention compliance, audit events, deletion requests

### Health Checks

Health endpoints provide service status:

- `/health/ready` - Readiness probe
- `/health/live` - Liveness probe
- `/admin/realms/{realm}/face-recognition/health` - Detailed health

### Logging

Configure logging levels in Keycloak:

```properties
# Enable debug logging for face recognition
logger.com.bioid.keycloak.level=DEBUG
```

## Security Considerations

### Data Protection
- Raw biometric images are never persisted
- Templates are encrypted in the database
- All communications use TLS encryption
- Configurable data residency compliance

### Access Control
- Admin endpoints require realm management permissions
- User enrollment requires authentication
- Deletion requests include approval workflows
- Comprehensive audit logging

### Privacy Compliance
- GDPR-compliant deletion workflows
- User consent management
- Data retention policies
- Right to be forgotten implementation

## Troubleshooting

### Common Issues

**Extension not loading:**
- Verify JAR is in `/opt/keycloak/providers/`
- Check Keycloak logs for loading errors
- Ensure Java version compatibility

**BioID connectivity issues:**
- Verify credentials in configuration
- Test network connectivity to BioID endpoints
- Check firewall and proxy settings
- Use connectivity test in admin console

**Authentication failures:**
- Check verification threshold settings
- Verify user enrollment status
- Review authentication flow configuration
- Check BioID service status

**Performance issues:**
- Monitor connection pool metrics
- Adjust timeout settings
- Review regional endpoint configuration
- Check network latency to BioID service

### Debug Mode

Enable debug logging:

```properties
logger.com.bioid.keycloak.level=DEBUG
logger.com.bioid.keycloak.client.level=TRACE
```

### Support

For technical support:
1. Check the troubleshooting section
2. Review Keycloak and extension logs
3. Test BioID service connectivity
4. Verify configuration settings

## License

This project is licensed under the Apache License 2.0. See the LICENSE file for details.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Ensure all tests pass
6. Submit a pull request

## Documentation

For detailed information, see the documentation in the `docs/` directory:

### Getting Started
- [Setup Guide](docs/SETUP.md) - Complete setup instructions with Docker Compose
- [Windows Setup](docs/WINDOWS-SETUP.md) - Windows-specific setup and troubleshooting
- [Quick Start Testing](docs/QUICK_START_TESTING.md) - Fast testing with simple-test.html
- [Keycloak Client Setup](docs/KEYCLOAK_CLIENT_SETUP.md) - Client configuration for OIDC

### Development & Testing
- [Development Guide](docs/DEVELOPMENT.md) - Development setup and guidelines
- [Testing Guide](docs/TESTING.md) - Comprehensive testing strategies
- [Complete Testing Guide](docs/COMPLETE_TESTING_GUIDE.md) - End-to-end testing procedures

### Deployment & Operations
- [Deployment Guide](docs/DEPLOYMENT.md) - Production deployment instructions
- [REST API Setup](docs/REST_API_SETUP.md) - REST API configuration and usage
- [Monitoring Guide](docs/MONITORING.md) - Metrics, health checks, and observability
- [Troubleshooting Guide](docs/TROUBLESHOOTING.md) - Common issues and solutions

### Features & Security
- [Active Liveness Detection](docs/ACTIVE_LIVENESS_DETECTION.md) - Liveness detection features
- [Security Guide](docs/SECURITY.md) - Security best practices and compliance
- [Dependency Management](docs/DEPENDENCY_MANAGEMENT.md) - Keeping dependencies updated

### Mobile & UI
- [Mobile Responsive Guide](docs/MOBILE_RESPONSIVE.md) - Mobile optimization and responsive design
- [Mobile Testing Guide](docs/MOBILE_TESTING_GUIDE.md) - Testing on mobile devices and browsers

## Changelog

### Version 1.0.0
- Initial release with face authentication
- Admin console integration
- GDPR-compliant deletion workflows
- Micrometer metrics and health checks
- Multi-region support with failover
- Comprehensive liveness detection