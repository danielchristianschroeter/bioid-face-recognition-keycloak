# Testing Guide

This guide covers testing strategies, test execution, and validation procedures for the Keycloak BioID Face Recognition Extension.

## Testing Strategy

### Test Pyramid

The extension follows a comprehensive testing strategy:

```
    /\
   /  \     E2E Tests (Browser automation)
  /____\    
 /      \   Integration Tests (BioID service)
/________\  
Unit Tests (Isolated components)
```

### Test Categories

1. **Unit Tests** - Fast, isolated tests with no external dependencies
2. **Integration Tests** - Tests requiring BioID service or database
3. **End-to-End Tests** - Full workflow tests with browser automation
4. **Performance Tests** - Load and stress testing
5. **Security Tests** - Vulnerability and penetration testing

## Test Environment Setup

### Local Testing Environment

```bash
# 1. Start test environment
docker compose -f docker compose.debug.yml up -d

# 2. Wait for services to be ready
docker compose -f docker compose.debug.yml logs -f keycloak | grep "started"

# 3. Run tests
mvn test
```

### Test Configuration

Create test-specific configuration:

```properties
# src/test/resources/test-bioid.properties
bioid.clientId=test-client-id
bioid.key=test-key
bioid.endpoint=face.bws-eu.bioid.com
bioid.verificationThreshold=0.6
bioid.maxRetries=2
bioid.livenessEnabled=false
```

## Unit Testing

### Running Unit Tests

```bash
# Run all unit tests
mvn test -Dtest=*Test

# Run specific test class
mvn test -Dtest=SimpleCompilationTest

# Run tests with coverage
mvn test jacoco:report

# View coverage report
open target/site/jacoco/index.html
```

### Unit Test Examples

**Testing DTO Creation:**
```java
@Test
void shouldCreateDeletionRequestDto() {
    // Given
    String userId = "user-123";
    String reason = "GDPR request";
    
    // When
    DeletionRequestDto dto = new DeletionRequestDto();
    dto.setUserId(userId);
    dto.setReason(reason);
    dto.setStatus(DeletionRequestStatus.PENDING);
    
    // Then
    assertEquals(userId, dto.getUserId());
    assertEquals(reason, dto.getReason());
    assertTrue(dto.isPending());
}
```

**Testing Enum Functionality:**
```java
@Test
void shouldConvertStatusFromValue() {
    // Given
    String statusValue = "pending";
    
    // When
    DeletionRequestStatus status = DeletionRequestStatus.fromValue(statusValue);
    
    // Then
    assertEquals(DeletionRequestStatus.PENDING, status);
    assertEquals("pending", status.getValue());
}
```

### Test Coverage Goals

- **Minimum Coverage**: 80% line coverage
- **Critical Components**: 95% coverage for security-related code
- **DTOs and Models**: 100% coverage for data classes

## Integration Testing

### Prerequisites

Integration tests require:
- Valid BioID credentials
- Network access to BioID endpoints
- Running Keycloak instance
- Test database

### Setup Integration Tests

```bash
# 1. Configure test credentials
export BWS_CLIENT_ID=your-test-client-id
export BWS_KEY=your-test-key

# 2. Start test environment
docker compose -f docker compose.debug.yml up -d

# 3. Run integration tests
mvn test -Dtest=*IntegrationTest
```

### Integration Test Examples

**Testing BioID Service Connection:**
```java
@Test
@EnabledIf("isBioIdConfigured")
void shouldConnectToBioIdService() {
    // Given
    BioIdGrpcClient client = new BioIdGrpcClient(configuration);
    
    // When
    boolean isHealthy = client.isHealthy();
    
    // Then
    assertTrue(isHealthy, "BioID service should be accessible");
}

private boolean isBioIdConfigured() {
    return System.getenv("BWS_CLIENT_ID") != null && 
           System.getenv("BWS_KEY") != null;
}
```

**Testing Deletion Request Workflow:**
```java
@Test
@Transactional
void shouldProcessDeletionRequestWorkflow() {
    // Given
    String userId = "test-user-" + UUID.randomUUID();
    DeletionRequestService service = new DeletionRequestService(session, realm);
    
    // When - Create request
    DeletionRequestDto request = service.createDeletionRequest(
        userId, "Test deletion", DeletionRequestPriority.HIGH);
    
    // Then - Verify creation
    assertNotNull(request.getId());
    assertEquals(DeletionRequestStatus.PENDING, request.getStatus());
    
    // When - Approve request
    DeletionRequestDto approved = service.approveDeletionRequest(
        request.getId(), "admin", "Approved for testing");
    
    // Then - Verify approval
    assertEquals(DeletionRequestStatus.COMPLETED, approved.getStatus());
    assertNotNull(approved.getProcessedAt());
}
```

## End-to-End Testing

### Browser Automation Setup

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.seleniumhq.selenium</groupId>
    <artifactId>selenium-java</artifactId>
    <version>4.15.0</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>io.github.bonigarcia</groupId>
    <artifactId>webdrivermanager</artifactId>
    <version>5.6.2</version>
    <scope>test</scope>
</dependency>
```

### E2E Test Examples

**Testing Face Enrollment Flow:**
```java
@Test
void shouldCompleteEnrollmentFlow() {
    // Given
    WebDriver driver = new ChromeDriver();
    driver.get("http://localhost:8080/realms/test/account");
    
    try {
        // When - Login and navigate to enrollment
        loginAsTestUser(driver);
        navigateToFaceEnrollment(driver);
        
        // Simulate camera access and enrollment
        mockCameraAccess(driver);
        clickEnrollButton(driver);
        
        // Then - Verify enrollment success
        WebElement successMessage = driver.findElement(By.id("enrollment-success"));
        assertTrue(successMessage.isDisplayed());
        
    } finally {
        driver.quit();
    }
}
```

**Testing Authentication Flow:**
```java
@Test
void shouldAuthenticateWithFace() {
    // Given
    WebDriver driver = new ChromeDriver();
    driver.get("http://localhost:8080/realms/test/protocol/openid-connect/auth");
    
    try {
        // When - Attempt face authentication
        enterUsername(driver, "testuser");
        clickFaceAuthButton(driver);
        
        // Simulate successful face verification
        mockFaceVerification(driver, true);
        
        // Then - Verify successful authentication
        assertTrue(driver.getCurrentUrl().contains("account"));
        
    } finally {
        driver.quit();
    }
}
```

### E2E Test Configuration

```properties
# e2e-test.properties
test.keycloak.url=http://localhost:8080
test.realm=test-realm
test.client.id=test-client
test.user.username=testuser
test.user.password=testpass
test.browser=chrome
test.headless=true
```

## Performance Testing

### Load Testing Setup

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.apache.jmeter</groupId>
    <artifactId>ApacheJMeter_core</artifactId>
    <version>5.6.2</version>
    <scope>test</scope>
</dependency>
```

### Performance Test Scenarios

**Authentication Load Test:**
```java
@Test
void shouldHandleAuthenticationLoad() {
    // Given
    int concurrentUsers = 50;
    int testDurationSeconds = 60;
    
    ExecutorService executor = Executors.newFixedThreadPool(concurrentUsers);
    CountDownLatch latch = new CountDownLatch(concurrentUsers);
    AtomicInteger successCount = new AtomicInteger(0);
    AtomicInteger failureCount = new AtomicInteger(0);
    
    // When - Execute concurrent authentications
    for (int i = 0; i < concurrentUsers; i++) {
        executor.submit(() -> {
            try {
                boolean success = performFaceAuthentication("user" + Thread.currentThread().getId());
                if (success) {
                    successCount.incrementAndGet();
                } else {
                    failureCount.incrementAndGet();
                }
            } finally {
                latch.countDown();
            }
        });
    }
    
    // Then - Verify performance metrics
    latch.await(testDurationSeconds, TimeUnit.SECONDS);
    
    double successRate = (double) successCount.get() / concurrentUsers;
    assertTrue(successRate > 0.95, "Success rate should be above 95%");
    
    executor.shutdown();
}
```

### Performance Metrics

Monitor these key metrics during performance testing:

- **Response Time**: < 3 seconds for authentication
- **Throughput**: > 100 authentications/minute
- **Success Rate**: > 95% under normal load
- **Memory Usage**: < 2GB heap usage
- **CPU Usage**: < 80% under load

## Security Testing

### Security Test Categories

1. **Input Validation** - Test for injection attacks
2. **Authentication Bypass** - Verify security controls
3. **Data Protection** - Ensure sensitive data handling
4. **Access Control** - Test authorization mechanisms

### Security Test Examples

**Testing Input Validation:**
```java
@Test
void shouldRejectMaliciousInput() {
    // Given
    String maliciousInput = "<script>alert('xss')</script>";
    DeletionRequestService service = new DeletionRequestService(session, realm);
    
    // When & Then
    assertThrows(IllegalArgumentException.class, () -> {
        service.createDeletionRequest("user123", maliciousInput, DeletionRequestPriority.NORMAL);
    });
}
```

**Testing Authentication Security:**
```java
@Test
void shouldPreventAuthenticationBypass() {
    // Given
    FaceAuthenticator authenticator = new FaceAuthenticator();
    AuthenticationFlowContext context = mock(AuthenticationFlowContext.class);
    
    // When - Attempt to bypass with null image
    authenticator.authenticate(context);
    
    // Then - Should fail authentication
    verify(context).failure(AuthenticationFlowError.INVALID_CREDENTIALS);
}
```

### Vulnerability Scanning

```bash
# OWASP Dependency Check
mvn org.owasp:dependency-check-maven:check

# Static code analysis
mvn spotbugs:check

# Security scan results
open target/dependency-check-report.html
```

## Test Data Management

### Test User Creation

```java
@BeforeEach
void createTestUsers() {
    // Create test users with different scenarios
    createTestUser("enrolled-user", true);  // Has face credential
    createTestUser("new-user", false);      // No face credential
    createTestUser("expired-user", true, true); // Expired credential
}

private void createTestUser(String username, boolean hasFaceCredential) {
    UserModel user = session.users().addUser(realm, username);
    user.setEnabled(true);
    user.setEmail(username + "@test.com");
    
    if (hasFaceCredential) {
        addFaceCredential(user);
    }
}
```

### Test Data Cleanup

```java
@AfterEach
void cleanupTestData() {
    // Remove test users
    realm.getUsersStream()
        .filter(user -> user.getUsername().startsWith("test-"))
        .forEach(user -> session.users().removeUser(realm, user));
    
    // Clean up deletion requests
    deletionRequestService.getAllRequests()
        .stream()
        .filter(req -> req.getUserId().startsWith("test-"))
        .forEach(req -> deletionRequestService.deleteRequest(req.getId()));
}
```

## Continuous Integration

### GitHub Actions Workflow

```yaml
# .github/workflows/test.yml
name: Test Suite

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  unit-tests:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v4
    - name: Set up JDK 21
      uses: actions/setup-java@v4
      with:
        java-version: '21'
        distribution: 'temurin'
    
    - name: Cache Maven dependencies
      uses: actions/cache@v3
      with:
        path: ~/.m2
        key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}
    
    - name: Run unit tests
      run: mvn test -Dtest=SimpleCompilationTest
    
    - name: Generate test report
      run: mvn surefire-report:report
    
    - name: Upload test results
      uses: actions/upload-artifact@v3
      with:
        name: test-results
        path: target/surefire-reports/

  integration-tests:
    runs-on: ubuntu-latest
    if: github.event_name == 'push'
    steps:
    - uses: actions/checkout@v4
    - name: Set up JDK 21
      uses: actions/setup-java@v4
      with:
        java-version: '21'
        distribution: 'temurin'
    
    - name: Start test environment
      run: |
        docker compose -f docker compose.debug.yml up -d postgres
        sleep 10
    
    - name: Run integration tests
      env:
        BWS_CLIENT_ID: ${{ secrets.BWS_CLIENT_ID }}
        BWS_KEY: ${{ secrets.BWS_KEY }}
      run: mvn test -Dtest=*IntegrationTest
    
    - name: Cleanup
      run: docker compose -f docker compose.debug.yml down
```

### Test Reporting

```bash
# Generate comprehensive test report
mvn surefire-report:report site

# View test report
open target/site/surefire-report.html

# Generate coverage report
mvn jacoco:report

# View coverage report
open target/site/jacoco/index.html
```

## Test Maintenance

### Regular Test Review

**Monthly Tasks:**
- Review test coverage metrics
- Update test data and scenarios
- Validate test environment configuration
- Check for flaky tests

**Quarterly Tasks:**
- Performance baseline updates
- Security test scenario updates
- Test automation improvements
- Test documentation updates

### Test Quality Metrics

Track these metrics to ensure test quality:

- **Test Coverage**: Line and branch coverage percentages
- **Test Execution Time**: Time to run full test suite
- **Test Reliability**: Flaky test identification and resolution
- **Test Maintenance**: Time spent maintaining tests

### Best Practices

1. **Test Naming**: Use descriptive test method names
2. **Test Independence**: Each test should be independent
3. **Test Data**: Use factories for consistent test data
4. **Assertions**: Use meaningful assertion messages
5. **Cleanup**: Always clean up test data and resources

```java
// Good test naming
@Test
void shouldCreateDeletionRequestWhenValidUserIdProvided() { }

// Good assertion messages
assertEquals(expected, actual, "Deletion request status should be PENDING after creation");

// Good test data factory
public class DeletionRequestTestDataFactory {
    public static DeletionRequestDto createPendingRequest(String userId) {
        DeletionRequestDto request = new DeletionRequestDto();
        request.setUserId(userId);
        request.setStatus(DeletionRequestStatus.PENDING);
        request.setPriority(DeletionRequestPriority.NORMAL);
        request.setRequestedAt(Instant.now());
        return request;
    }
}
```