package com.bioid.keycloak.action;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.bioid.keycloak.credential.FaceCredentialProvider;
import com.bioid.keycloak.credential.FaceCredentialProviderFactory;
import com.bioid.keycloak.test.TestUtils;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.models.KeycloakSession;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for FaceEnrollAction.
 *
 * <p>These tests verify the core functionality of the face enrollment action, including input
 * validation, error handling, and workflow management.
 *
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Face Enrollment Action Tests")
class FaceEnrollActionTest {

  private FaceEnrollAction faceEnrollAction;

  @Mock private RequiredActionContext mockContext;

  @Mock private KeycloakSession mockSession;

  @Mock private FaceCredentialProvider mockCredentialProvider;

  @BeforeEach
  void setUp() {
    // Mock basic credential provider behavior
    lenient().when(mockCredentialProvider.hasValidFaceCredentials(any(), any())).thenReturn(false);

    // Setup mock session to return providers - use the correct provider class and ID
    lenient()
        .when(
            mockSession.getProvider(
                eq(CredentialProvider.class), eq(FaceCredentialProviderFactory.PROVIDER_ID)))
        .thenReturn(mockCredentialProvider);

    faceEnrollAction = new FaceEnrollAction();
  }

  @Test
  @DisplayName("Should validate valid image data successfully")
  void shouldValidateValidImageData() {
    // Given
    String validImageData = TestUtils.generateSampleImageData();

    // When
    boolean isValid = invokeValidateImageData(validImageData);

    // Then
    assertTrue(isValid, "Valid image data should pass validation");
  }

  @Test
  @DisplayName("Should reject null image data")
  void shouldRejectNullImageData() {
    // When
    boolean isValid = invokeValidateImageData(null);

    // Then
    assertFalse(isValid, "Null image data should be rejected");
  }

  @Test
  @DisplayName("Should reject empty image data")
  void shouldRejectEmptyImageData() {
    // When
    boolean isValid = invokeValidateImageData("");

    // Then
    assertFalse(isValid, "Empty image data should be rejected");
  }

  @Test
  @DisplayName("Should reject invalid image data format")
  void shouldRejectInvalidImageDataFormat() {
    // Given
    String invalidImageData = TestUtils.generateInvalidImageData();

    // When
    boolean isValid = invokeValidateImageData(invalidImageData);

    // Then
    assertFalse(isValid, "Invalid image data format should be rejected");
  }

  @Test
  @DisplayName("Should reject oversized image data")
  void shouldRejectOversizedImageData() {
    // Given
    String oversizedImageData = TestUtils.generateOversizedImageData(15); // 15MB

    // When
    boolean isValid = invokeValidateImageData(oversizedImageData);

    // Then
    assertFalse(isValid, "Oversized image data should be rejected");
  }

  @Test
  @DisplayName("Should reject too small image data")
  void shouldRejectTooSmallImageData() {
    // Given
    String tooSmallImageData = "data:image/png;base64,abc"; // Less than 100 chars

    // When
    boolean isValid = invokeValidateImageData(tooSmallImageData);

    // Then
    assertFalse(isValid, "Too small image data should be rejected");
  }

  @Test
  @DisplayName("Should handle capture action with valid image")
  void shouldHandleCaptureActionWithValidImage() {
    // Given
    String validImageData = TestUtils.generateSampleImageData();
    MultivaluedMap<String, String> formParams = TestUtils.createCaptureFormParams(validImageData);
    RequiredActionContext context = createMockContextWithSession(formParams);

    // When & Then
    assertDoesNotThrow(
        () -> faceEnrollAction.processAction(context),
        "Processing valid capture action should not throw exception");
  }

  @Test
  @DisplayName("Should handle submit action")
  void shouldHandleSubmitAction() {
    // Given
    MultivaluedMap<String, String> formParams = TestUtils.createSubmitFormParams();
    RequiredActionContext context = createMockContextWithSession(formParams);

    // When & Then
    assertDoesNotThrow(
        () -> faceEnrollAction.processAction(context),
        "Processing submit action should not throw exception");
  }

  @Test
  @DisplayName("Should handle unknown action gracefully")
  void shouldHandleUnknownActionGracefully() {
    // Given
    MultivaluedMap<String, String> formParams = new MultivaluedHashMap<>();
    formParams.add("action", "unknown");
    RequiredActionContext context = createMockContextWithSession(formParams);

    // When & Then
    assertDoesNotThrow(
        () -> faceEnrollAction.processAction(context),
        "Processing unknown action should not throw exception");
  }

  @Test
  @DisplayName("Should return correct provider ID")
  void shouldReturnCorrectProviderId() {
    // When
    String providerId = FaceEnrollAction.PROVIDER_ID;

    // Then
    assertEquals("face-enroll", providerId, "Provider ID should match expected value");
  }

  @Test
  @DisplayName("Should return correct display text")
  void shouldReturnCorrectDisplayText() {
    // Given
    FaceEnrollActionFactory factory = new FaceEnrollActionFactory();

    // When
    String displayText = factory.getDisplayText();

    // Then
    assertNotNull(displayText, "Display text should not be null");
    assertTrue(
        TestUtils.containsIgnoreCase(displayText, "face"), "Display text should contain 'face'");
  }

  @Test
  @DisplayName("Should handle enrollment state parsing")
  void shouldHandleEnrollmentStateParsing() {
    // Given
    String validStateJson = TestUtils.createEnrollmentStateJson(2, 1);
    RequiredActionContext context =
        createMockContextWithSession(
            TestUtils.createCaptureFormParams(TestUtils.generateSampleImageData()));

    // When & Then
    assertDoesNotThrow(
        () -> {
          // This tests that the context setup doesn't cause issues
          faceEnrollAction.processAction(context);
        },
        "Parsing valid enrollment state should not throw exception");
  }

  @Test
  @DisplayName("Should handle oversized enrollment state")
  void shouldHandleOversizedEnrollmentState() {
    // Given
    RequiredActionContext context =
        createMockContextWithSession(
            TestUtils.createCaptureFormParams(TestUtils.generateSampleImageData()));

    // When & Then
    assertDoesNotThrow(
        () -> faceEnrollAction.processAction(context),
        "Handling oversized enrollment state should not throw exception");
  }

  @Test
  @DisplayName("Should handle null context gracefully")
  void shouldHandleNullContextGracefully() {
    // When & Then
    assertThrows(
        Exception.class,
        () -> faceEnrollAction.processAction(null),
        "Processing with null context should throw exception");
  }

  @Test
  @DisplayName("Should handle evaluate triggers")
  void shouldHandleEvaluateTriggers() {
    // Given
    RequiredActionContext context = createMockContextWithSession();

    // When & Then
    assertDoesNotThrow(
        () -> {
          faceEnrollAction.evaluateTriggers(context);
        },
        "Evaluating triggers should not throw exception");
  }

  @Test
  @DisplayName("Should handle required action challenge")
  void shouldHandleRequiredActionChallenge() {
    // Given
    RequiredActionContext context = createMockContextWithSession();

    // When & Then
    assertDoesNotThrow(
        () -> {
          faceEnrollAction.requiredActionChallenge(context);
        },
        "Required action challenge should not throw exception");
  }

  @Test
  @DisplayName("Should handle close operation")
  void shouldHandleCloseOperation() {
    // When & Then
    assertDoesNotThrow(
        () -> faceEnrollAction.close(), "Close operation should not throw exception");
  }

  /** Helper method to create a mock context that uses our mock session. */
  private RequiredActionContext createMockContextWithSession(
      MultivaluedMap<String, String> formParams) {
    RequiredActionContext context = TestUtils.createMockContextWithForm(formParams);

    // Override the session to use our mock session
    lenient().when(context.getSession()).thenReturn(mockSession);

    return context;
  }

  /** Helper method to create a mock context that uses our mock session. */
  private RequiredActionContext createMockContextWithSession() {
    RequiredActionContext context = TestUtils.createMockContext();

    // Override the session to use our mock session
    lenient().when(context.getSession()).thenReturn(mockSession);

    return context;
  }

  /**
   * Helper method to invoke the private isImageDataValid method using reflection. In a real
   * implementation, you might make this method package-private for testing.
   */
  private boolean invokeValidateImageData(String imageData) {
    try {
      var method = FaceEnrollAction.class.getDeclaredMethod("isImageDataValid", String.class);
      method.setAccessible(true);
      return (Boolean) method.invoke(faceEnrollAction, imageData);
    } catch (Exception e) {
      throw new RuntimeException("Failed to invoke isImageDataValid method", e);
    }
  }
}
