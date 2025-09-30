# Development Guide

This guide covers development setup, testing, and contribution guidelines for the Keycloak BioID Face Recognition Extension.

## Development Environment Setup

### Prerequisites

- Java 21 or higher
- Maven 3.8 or higher
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
│   │       └── metrics/            # Metrics collection
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