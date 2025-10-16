# Development Guide

This guide covers development setup, testing, contribution guidelines, and dependency management for the Keycloak BioID Face Recognition Extension.

## Development Environment Setup

### Prerequisites

- Java 21 or higher
- Maven 3.8 or higher
- Docker and Docker Compose (for integration testing)
- Git (for version control)
- Docker and Docker Compose
- Git
- BioID BWS account (for integration testing)

### Initial Setup

1. **Clone the Repository**
   ```bash
   git clone <repository-url>
   cd keycloak-bioid-extension
   ```

2. **Configure BioID Credentials**
   ```bash
   cp .env.example .env
   # Edit .env with your BioID credentials
   ```

3. **Build the Project**
   ```bash
   mvn clean compile
   ```

4. **Run Tests**
   ```bash
   # Unit tests only (no external dependencies)
   mvn test -Dtest=SimpleCompilationTest
   
   # All tests (requires BioID configuration)
   mvn test
   ```

### Docker Development Environment

The fastest way to develop and test is using Docker Compose:

```bash
# Build and start the development environment
mvn clean package -DskipTests
docker compose up -d

# View logs
docker compose logs -f keycloak

# Access Keycloak Admin Console
open http://localhost:8080/admin
```

### Development Workflow

1. **Make Code Changes**
   - Edit source files in your IDE
   - Follow the existing code style and patterns

2. **Build and Test**
   ```bash
   mvn clean compile
   mvn test -Dtest=SimpleCompilationTest
   ```

3. **Deploy to Development Environment**
   ```bash
   mvn package -DskipTests
   docker compose restart keycloak
   ```

4. **Test in Keycloak**
   - Access http://localhost:8080/admin
   - Navigate to Realm Settings > Face Recognition
   - Test configuration and functionality

## Project Structure

```
keycloak-bioid-extension/
├── bioid-client/                    # BioID gRPC client library
│   ├── src/main/java/
│   │   └── com/bioid/keycloak/client/
│   │       ├── BioIdGrpcClient.java      # Main gRPC client
│   │       ├── config/                   # Configuration classes
│   │       └── exception/                # Exception handling
│   └── src/test/java/               # Unit tests
│
├── face-authenticator/              # Authentication flow integration
│   ├── src/main/java/
│   │   └── com/bioid/keycloak/authenticator/
│   │       ├── FaceAuthenticator.java    # Main authenticator
│   │       └── FaceAuthenticatorFactory.java
│   └── src/test/java/
│
├── face-credential/                 # Credential provider
│   ├── src/main/java/
│   │   └── com/bioid/keycloak/credential/
│   │       ├── FaceCredentialProvider.java
│   │       └── FaceCredentialModel.java
│   └── src/test/java/
│
├── face-enroll-action/             # Enrollment required action
│   ├── src/main/java/
│   │   └── com/bioid/keycloak/enroll/
│   │       ├── FaceEnrollAction.java
│   │       └── FaceEnrollActionFactory.java
│   └── src/test/java/
│
├── ui-components/                  # Admin UI and REST endpoints
│   ├── src/main/java/
│   │   └── com/bioid/keycloak/
│   │       ├── admin/              # Admin REST endpoints
│   │       ├── health/             # Health check providers
│   │       ├── metrics/            # Comprehensive metrics collection
│   │       ├── logging/            # Structured logging and log aggregation
│   │       ├── tracing/            # Distributed tracing implementation
│   │       └── alerting/           # Real-time alerting system
│   ├── src/main/resources/
│   │   └── theme/                  # UI templates and assets
│   └── src/test/java/
│
├── assembly/                       # Final JAR assembly
├── docker/                         # Docker configuration
├── docs/                          # Documentation
└── pom.xml                        # Root Maven configuration
```

## Code Style and Standards

### Java Code Style

- Use Java 21 features where appropriate
- Follow standard Java naming conventions
- Use meaningful variable and method names
- Add comprehensive JavaDoc for public APIs
- Keep methods focused and single-purpose

### Example Code Style

```java
/**
 * Processes face verification requests with comprehensive error handling.
 * 
 * @param userId the user identifier for verification
 * @param imageData the face image data for verification
 * @return verification result with confidence score
 * @throws BioIdException if verification fails
 */
public VerificationResult verifyFace(String userId, byte[] imageData) throws BioIdException {
    if (userId == null || userId.isEmpty()) {
        throw new IllegalArgumentException("User ID cannot be null or empty");
    }
    
    if (imageData == null || imageData.length == 0) {
        throw new IllegalArgumentException("Image data cannot be null or empty");
    }
    
    try {
        return bioIdClient.verify(userId, imageData);
    } catch (Exception e) {
        logger.error("Face verification failed for user: {}", userId, e);
        throw new BioIdException("Verification failed", e);
    }
}
```

### Testing Standards

- Write unit tests for all public methods
- Use meaningful test method names
- Follow the Given-When-Then pattern
- Mock external dependencies
- Test both success and failure scenarios

### Example Test

```java
@Test
void shouldVerifyFaceSuccessfully() {
    // Given
    String userId = "test-user";
    byte[] imageData = "test-image-data".getBytes();
    VerificationResult expectedResult = new VerificationResult(true, 0.95);
    
    when(bioIdClient.verify(userId, imageData)).thenReturn(expectedResult);
    
    // When
    VerificationResult result = faceService.verifyFace(userId, imageData);
    
    // Then
    assertTrue(result.isSuccess());
    assertEquals(0.95, result.getConfidence(), 0.01);
}
```

## Testing

### Test Categories

1. **Unit Tests** - Fast, isolated tests with no external dependencies
2. **Integration Tests** - Tests that require BioID service or database
3. **Compilation Tests** - Basic compilation and instantiation tests

### Running Tests

```bash
# All tests
mvn test

# Unit tests only
mvn test -Dtest=SimpleCompilationTest

# Specific test class
mvn test -Dtest=FaceAuthenticatorTest

# Skip tests during build
mvn package -DskipTests
```

### Test Configuration

Integration tests require BioID configuration. Create `src/test/resources/test.properties`:

```properties
bioid.clientId=test-client-id
bioid.key=test-key
bioid.endpoint=face.bws-eu.bioid.com
```

## Debugging

### Enable Debug Logging

Add to Keycloak configuration:

```properties
logger.com.bioid.keycloak.level=DEBUG
logger.com.bioid.keycloak.client.level=TRACE
```

### Debug in IDE

1. Start Keycloak with debug port:
   ```bash
   docker compose -f docker compose.debug.yml up -d
   ```

2. Connect your IDE debugger to port 5005

### Monitoring During Development

Monitor extension behavior during development:

```bash
# View real-time metrics
curl -s http://localhost:8080/admin/realms/master/face-recognition/metrics

# Check health status
curl -s http://localhost:8080/admin/realms/master/face-recognition/health | jq '.'

# View trace statistics
curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/admin/realms/master/face-recognition/traces/stats

# Monitor structured logs
tail -f /opt/keycloak/data/log/keycloak.log | grep -E "(correlationId|AdminLogEvent)"

# Monitor security events
tail -f /opt/keycloak/data/log/keycloak.log | grep -E "(SECURITY|PRIVACY|ENCRYPTION)"

# Check GDPR compliance
curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/admin/realms/master/face-recognition/privacy/compliance

# View privacy statistics
curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/admin/realms/master/face-recognition/privacy/stats
```

### Security Development Guidelines

**Secure Coding Practices:**

```java
// Good: Secure credential handling
public class SecureCredentialExample {
    private final SecureCredentialStorage storage;
    
    public void handleCredential(String credentialId, String metadata) {
        // Encrypt before storage
        String encrypted = storage.encryptCredentialMetadata(credentialId, metadata);
        
        // Clear sensitive data from memory
        metadata = null;
        
        // Store encrypted data
        persistEncryptedData(credentialId, encrypted);
    }
}

// Good: Secure memory handling
public class SecureBiometricProcessing {
    private final PrivacyProtectionService privacyService;
    
    public boolean processImage(String sessionId, byte[] imageData) {
        try (PrivacyProtectionService.SecureImageHandle handle = 
                privacyService.processImageData(sessionId, imageData, 
                    BiometricOperation.VERIFY)) {
            
            // Process biometric data
            return performVerification(handle);
            
        } // Automatic cleanup on close
    }
}

// Good: Input validation
public class SecureInputValidation {
    public void validateUserId(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        
        if (userId.length() > 255) {
            throw new IllegalArgumentException("User ID too long");
        }
        
        if (!userId.matches("^[a-zA-Z0-9_-]+$")) {
            throw new IllegalArgumentException("User ID contains invalid characters");
        }
    }
}
```

**Security Testing During Development:**

```java
// Security test example
@Test
void shouldPreventSqlInjection() {
    String maliciousInput = "'; DROP TABLE users; --";
    
    assertThrows(IllegalArgumentException.class, () -> {
        service.processUserInput(maliciousInput);
    });
}

@Test
void shouldClearSensitiveDataFromMemory() {
    byte[] sensitiveData = "biometric-data".getBytes();
    byte[] originalCopy = sensitiveData.clone();
    
    // Process data
    processSecurely(sensitiveData);
    
    // Verify original is cleared
    for (byte b : sensitiveData) {
        assertEquals(0, b, "Sensitive data should be cleared");
    }
}
```

### Common Debug Scenarios

**Extension Not Loading:**
- Check JAR is in providers directory
- Verify SPI registration in META-INF/services
- Check for class loading conflicts

**Authentication Flow Issues:**
- Verify flow configuration in Keycloak
- Check authenticator registration
- Review authentication session state

**BioID Connection Issues:**
- Test connectivity with admin console
- Check network configuration
- Verify credentials and endpoints

## Performance Testing

### Load Testing

Use tools like JMeter or Gatling to test authentication flows:

```bash
# Example JMeter test
jmeter -n -t tests/face-auth-load-test.jmx -l results.jtl
```

### Metrics Monitoring

Monitor key metrics during testing:
- Authentication success/failure rates
- BioID service response times
- Connection pool utilization
- Memory and CPU usage

### Performance Optimization

- Adjust connection pool sizes
- Tune timeout values
- Optimize image processing
- Use appropriate regional endpoints
- Monitor metrics collection overhead
- Configure appropriate log aggregation intervals
- Optimize trace collection for production use

## Contributing

### Pull Request Process

1. **Fork and Branch**
   ```bash
   git checkout -b feature/your-feature-name
   ```

2. **Make Changes**
   - Follow code style guidelines
   - Add tests for new functionality
   - Update documentation

3. **Test Changes**
   ```bash
   mvn clean test
   mvn package -DskipTests
   docker compose up -d
   # Test manually in Keycloak
   ```

4. **Submit Pull Request**
   - Provide clear description
   - Reference related issues
   - Include test results

### Code Review Checklist

- [ ] Code follows style guidelines
- [ ] Tests are included and passing
- [ ] Documentation is updated
- [ ] No security vulnerabilities
- [ ] Performance impact considered
- [ ] Backward compatibility maintained

## Release Process

### Version Management

Versions follow semantic versioning (MAJOR.MINOR.PATCH):
- MAJOR: Breaking changes
- MINOR: New features, backward compatible
- PATCH: Bug fixes, backward compatible

### Release Steps

1. **Update Version**
   ```bash
   mvn versions:set -DnewVersion=1.1.0
   ```

2. **Build and Test**
   ```bash
   mvn clean test
   mvn package
   ```

3. **Create Release**
   ```bash
   git tag -a v1.1.0 -m "Release version 1.1.0"
   git push origin v1.1.0
   ```

4. **Deploy Artifacts**
   - Upload JAR to release repository
   - Update Docker images
   - Update documentation

## Troubleshooting Development Issues

### Common Build Issues

**Maven Dependency Conflicts:**
```bash
mvn dependency:tree
mvn dependency:analyze
```

**Compilation Errors:**
- Check Java version compatibility
- Verify Maven version
- Clean and rebuild: `mvn clean compile`

### Runtime Issues

**Extension Not Found:**
- Verify JAR in providers directory
- Check SPI registration files
- Review Keycloak startup logs

**Configuration Issues:**
- Validate configuration file syntax
- Check environment variable substitution
- Test with minimal configuration

### Getting Help

1. Check existing documentation
2. Review GitHub issues
3. Test with minimal reproduction case
4. Provide detailed error logs
5. Include environment information
## Dep
endency Management

### Keeping Dependencies Up to Date

This project uses Maven for dependency management. Here's how to ensure all packages stay current:

### 1. Check for Dependency Updates

Use the Maven Versions plugin to check for outdated dependencies:

```bash
# Check for dependency updates
mvn versions:display-dependency-updates

# Check for plugin updates
mvn versions:display-plugin-updates

# Check for property updates
mvn versions:display-property-updates
```

### 2. Automated Dependency Analysis

Run the dependency analysis profile to get comprehensive reports:

```bash
# Run dependency analysis
mvn clean compile -Pdependency-analysis

# This will:
# - Analyze unused/undeclared dependencies
# - Display available updates
# - Check for dependency conflicts
```

### 3. Security Vulnerability Scanning

Use OWASP Dependency Check to scan for known vulnerabilities:

```bash
# Run security scan
mvn clean compile -Psecurity-scan

# Generate detailed security report
mvn org.owasp:dependency-check-maven:check
```

### 4. Update Dependencies Safely

When updating dependencies, follow this process:

#### Step 1: Update Properties
Update version properties in the main `pom.xml`:

```xml
<properties>
    <!-- Core Dependencies -->
    <keycloak.version>26.4.0</keycloak.version>
    <grpc.version>1.76.0</grpc.version>
    <protobuf.version>4.33.0-RC2</protobuf.version>
    
    <!-- Security Dependencies -->
    <jjwt.version>0.13.0</jjwt.version>
    <bouncycastle.version>1.82</bouncycastle.version>
    
    <!-- Testing Dependencies -->
    <junit.version>6.0.0</junit.version>
    <mockito.version>5.20.0</mockito.version>
</properties>
```

#### Step 2: Test Compatibility
After updating versions:

```bash
# Clean build to ensure no cached artifacts
mvn clean compile

# Run unit tests
mvn test

# Run integration tests (requires BioID credentials)
mvn verify -Pit

# Run security and quality checks
mvn clean compile -Psecurity-scan -Pcode-quality
```

#### Step 3: Update Documentation
Update version requirements in documentation:
- `README.md` - Prerequisites section
- `SETUP.md` - Requirements
- `docs/DEPLOYMENT.md` - Production requirements

### 5. Automated Dependency Updates

#### Using Dependabot (GitHub)
Create `.github/dependabot.yml`:

```yaml
version: 2
updates:
  - package-ecosystem: "maven"
    directory: "/"
    schedule:
      interval: "weekly"
    open-pull-requests-limit: 10
    reviewers:
      - "your-team"
    assignees:
      - "maintainer"
```

#### Using Renovate Bot
Create `renovate.json`:

```json
{
  "extends": ["config:base"],
  "packageRules": [
    {
      "matchPackagePatterns": ["^org.keycloak"],
      "groupName": "keycloak"
    },
    {
      "matchPackagePatterns": ["^io.grpc"],
      "groupName": "grpc"
    }
  ]
}
```

### 6. Critical Dependencies to Monitor

#### Security-Critical Dependencies
- **Keycloak**: Core platform - monitor for security updates
- **gRPC**: Network communication - check for security patches
- **BouncyCastle**: Cryptographic operations - critical for security
- **JJWT**: JWT handling - authentication security
- **Jackson**: JSON processing - potential deserialization vulnerabilities

#### Performance-Critical Dependencies
- **Micrometer**: Metrics collection - monitor for performance improvements
- **Protobuf**: Serialization - affects gRPC performance
- **SLF4J/Logback**: Logging - performance impact in high-throughput scenarios

### 7. Version Compatibility Matrix

| Component | Current Version | Minimum Supported | Notes |
|-----------|----------------|-------------------|-------|
| Java | 21 | 21 | LTS version required |
| Maven | 3.8+ | 3.8.0 | For Java 21 support |
| Keycloak | 26.4.0 | 26.0.0 | Major version compatibility |
| gRPC | 1.76.0 | 1.70.0 | BioID BWS 3 compatibility |
| Protobuf | 4.33.0-RC2 | 4.25.0 | gRPC compatibility |

### 8. Update Testing Checklist

When updating dependencies, verify:

- [ ] **Compilation**: `mvn clean compile` succeeds
- [ ] **Unit Tests**: `mvn test` passes
- [ ] **Integration Tests**: `mvn verify -Pit` passes
- [ ] **Security Scan**: `mvn -Psecurity-scan` shows no new vulnerabilities
- [ ] **Code Quality**: `mvn -Pcode-quality` passes
- [ ] **Docker Build**: `docker compose build` succeeds
- [ ] **End-to-End Tests**: Manual testing of key workflows
- [ ] **Performance**: No regression in key metrics
- [ ] **Documentation**: Updated version requirements

### 9. Rollback Strategy

If an update causes issues:

```bash
# Revert to previous versions in pom.xml
git checkout HEAD~1 -- pom.xml

# Or revert specific dependency versions
# Update the version property back to previous value

# Clean and rebuild
mvn clean compile

# Verify functionality
mvn test
```

### 10. Monitoring for Updates

#### Weekly Tasks
- Check `mvn versions:display-dependency-updates`
- Review security advisories for key dependencies
- Monitor Keycloak release notes

#### Monthly Tasks
- Run full dependency analysis
- Update non-critical dependencies
- Review and update documentation

#### Quarterly Tasks
- Major version updates (with thorough testing)
- Security audit and penetration testing
- Performance benchmarking after updates

### 11. Useful Maven Commands

```bash
# Display dependency tree
mvn dependency:tree

# Analyze dependencies
mvn dependency:analyze

# Check for conflicts
mvn dependency:resolve-sources

# Update to latest versions (use with caution)
mvn versions:use-latest-versions

# Update specific dependency
mvn versions:set-property -Dproperty=keycloak.version -DnewVersion=26.4.0

# Revert version changes
mvn versions:revert
```

### 12. IDE Integration

#### IntelliJ IDEA
- Install "Maven Helper" plugin for dependency analysis
- Enable "Maven" tool window for version management
- Use "Dependency Analyzer" for conflict resolution

#### VS Code
- Install "Extension Pack for Java"
- Use "Maven for Java" extension
- Enable dependency lens for version information

### 13. Continuous Integration

Add dependency checks to CI pipeline:

```yaml
# .github/workflows/dependency-check.yml
name: Dependency Check
on:
  schedule:
    - cron: '0 2 * * 1'  # Weekly on Monday
  workflow_dispatch:

jobs:
  dependency-check:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
      - name: Check for updates
        run: mvn versions:display-dependency-updates
      - name: Security scan
        run: mvn org.owasp:dependency-check-maven:check
```