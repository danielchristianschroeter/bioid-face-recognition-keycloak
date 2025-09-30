package com.bioid.keycloak.test;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import java.util.Base64;
import java.util.Random;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.mockito.Mockito;

/**
 * Utility class for testing BioID Keycloak extension components.
 *
 * <p>This class provides helper methods for creating mock objects, test data, and common testing
 * scenarios.
 *
 * @since 1.0.0
 */
public final class TestUtils {

  private static final Random RANDOM = new Random();

  // Sample base64 image data (1x1 pixel PNG)
  private static final String SAMPLE_IMAGE_DATA =
      "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==";

  private TestUtils() {
    // Utility class
  }

  /**
   * Creates a mock RequiredActionContext for testing.
   *
   * @return mock RequiredActionContext
   */
  public static RequiredActionContext createMockContext() {
    RequiredActionContext context =
        Mockito.mock(RequiredActionContext.class, Mockito.RETURNS_DEEP_STUBS);
    KeycloakSession session = Mockito.mock(KeycloakSession.class, Mockito.RETURNS_DEEP_STUBS);
    RealmModel realm = Mockito.mock(RealmModel.class, Mockito.RETURNS_DEEP_STUBS);
    UserModel user = Mockito.mock(UserModel.class, Mockito.RETURNS_DEEP_STUBS);
    AuthenticationSessionModel authSession =
        Mockito.mock(AuthenticationSessionModel.class, Mockito.RETURNS_DEEP_STUBS);

    Mockito.lenient().when(context.getSession()).thenReturn(session);
    Mockito.lenient().when(context.getRealm()).thenReturn(realm);
    Mockito.lenient().when(context.getUser()).thenReturn(user);
    Mockito.lenient().when(context.getAuthenticationSession()).thenReturn(authSession);

    // Default user ID
    Mockito.lenient().when(user.getId()).thenReturn("test-user-" + RANDOM.nextInt(10000));

    // Mock form provider
    var formProvider =
        Mockito.mock(org.keycloak.forms.login.LoginFormsProvider.class, Mockito.RETURNS_SELF);
    Mockito.lenient().when(context.form()).thenReturn(formProvider);

    // Mock credential manager
    var credentialManager = Mockito.mock(org.keycloak.models.SubjectCredentialManager.class);
    Mockito.lenient().when(user.credentialManager()).thenReturn(credentialManager);
    Mockito.lenient()
        .when(credentialManager.getStoredCredentialsStream())
        .thenReturn(java.util.stream.Stream.empty());

    return context;
  }

  /**
   * Creates a mock RequiredActionContext with form parameters.
   *
   * @param formParams the form parameters to include
   * @return mock RequiredActionContext with form data
   */
  public static RequiredActionContext createMockContextWithForm(
      MultivaluedMap<String, String> formParams) {
    RequiredActionContext context = createMockContext();

    // Mock HTTP request with form parameters
    var httpRequest = Mockito.mock(org.keycloak.http.HttpRequest.class);
    Mockito.when(context.getHttpRequest()).thenReturn(httpRequest);
    Mockito.when(httpRequest.getDecodedFormParameters()).thenReturn(formParams);

    return context;
  }

  /**
   * Creates form parameters for a capture action.
   *
   * @param imageData the image data to include
   * @return form parameters for capture action
   */
  public static MultivaluedMap<String, String> createCaptureFormParams(String imageData) {
    MultivaluedMap<String, String> formParams = new MultivaluedHashMap<>();
    formParams.add("action", "capture");
    formParams.add("imageData", imageData != null ? imageData : SAMPLE_IMAGE_DATA);
    return formParams;
  }

  /**
   * Creates form parameters for a submit action.
   *
   * @return form parameters for submit action
   */
  public static MultivaluedMap<String, String> createSubmitFormParams() {
    MultivaluedMap<String, String> formParams = new MultivaluedHashMap<>();
    formParams.add("action", "submit");
    return formParams;
  }

  /**
   * Generates a valid sample image data URL.
   *
   * @return sample image data URL
   */
  public static String generateSampleImageData() {
    return SAMPLE_IMAGE_DATA;
  }

  /**
   * Generates invalid image data for testing validation.
   *
   * @return invalid image data
   */
  public static String generateInvalidImageData() {
    return "invalid-image-data";
  }

  /**
   * Generates oversized image data for testing size limits.
   *
   * @param sizeMB the size in megabytes
   * @return oversized image data
   */
  public static String generateOversizedImageData(int sizeMB) {
    int sizeBytes = sizeMB * 1024 * 1024;
    byte[] data = new byte[sizeBytes];
    RANDOM.nextBytes(data);
    return "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(data);
  }

  /**
   * Creates a test user ID.
   *
   * @return random test user ID
   */
  public static String createTestUserId() {
    return "test-user-" + RANDOM.nextInt(100000);
  }

  /**
   * Creates a test realm name.
   *
   * @return random test realm name
   */
  public static String createTestRealmName() {
    return "test-realm-" + RANDOM.nextInt(1000);
  }

  /**
   * Creates test enrollment state JSON.
   *
   * @param capturedImages number of captured images
   * @param attempts number of attempts
   * @return enrollment state JSON
   */
  public static String createEnrollmentStateJson(int capturedImages, int attempts) {
    return String.format(
        "{\"capturedImages\":%d,\"attempts\":%d,\"timestamp\":%d}",
        capturedImages, attempts, System.currentTimeMillis());
  }

  /**
   * Creates oversized enrollment state JSON for testing.
   *
   * @param sizeBytes the size in bytes
   * @return oversized enrollment state JSON
   */
  public static String createOversizedEnrollmentStateJson(int sizeBytes) {
    StringBuilder sb = new StringBuilder();
    sb.append("{\"data\":\"");

    // Fill with random data to reach desired size
    int dataSize = sizeBytes - 50; // Account for JSON structure
    for (int i = 0; i < dataSize; i++) {
      sb.append("x");
    }

    sb.append("\"}");
    return sb.toString();
  }

  /**
   * Asserts that a string contains expected content (case-insensitive).
   *
   * @param actual the actual string
   * @param expected the expected content
   * @return true if content is found
   */
  public static boolean containsIgnoreCase(String actual, String expected) {
    if (actual == null || expected == null) {
      return false;
    }
    return actual.toLowerCase().contains(expected.toLowerCase());
  }

  /**
   * Creates a mock authentication session with specified attributes.
   *
   * @param attributes the session attributes
   * @return mock authentication session
   */
  public static AuthenticationSessionModel createMockAuthSession(
      MultivaluedMap<String, String> attributes) {
    AuthenticationSessionModel authSession = Mockito.mock(AuthenticationSessionModel.class);

    // Mock attribute storage
    attributes.forEach(
        (key, values) -> {
          if (!values.isEmpty()) {
            Mockito.when(authSession.getAuthNote(key)).thenReturn(values.get(0));
          }
        });

    return authSession;
  }

  /**
   * Waits for a specified duration (useful for timing-sensitive tests).
   *
   * @param milliseconds the duration to wait
   */
  public static void waitFor(long milliseconds) {
    try {
      Thread.sleep(milliseconds);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Test interrupted", e);
    }
  }

  /**
   * Generates a random string of specified length.
   *
   * @param length the desired length
   * @return random string
   */
  public static String generateRandomString(int length) {
    StringBuilder sb = new StringBuilder();
    String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    for (int i = 0; i < length; i++) {
      sb.append(chars.charAt(RANDOM.nextInt(chars.length())));
    }

    return sb.toString();
  }
}
