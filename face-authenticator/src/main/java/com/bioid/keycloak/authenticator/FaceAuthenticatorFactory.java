package com.bioid.keycloak.authenticator;

import java.util.List;
import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

/**
 * Factory for creating FaceAuthenticator instances.
 *
 * <p>This factory registers the face authenticator with Keycloak's SPI framework and provides
 * configuration options for the authenticator.
 */
public class FaceAuthenticatorFactory implements AuthenticatorFactory {

  public static final String PROVIDER_ID = "face-authenticator";
  public static final String DISPLAY_NAME = "Face Recognition";
  public static final String HELP_TEXT =
      "Validates user identity using face biometric verification";

  // Configuration property keys
  private static final String CONFIG_MAX_RETRIES = "maxRetries";
  private static final String CONFIG_VERIFICATION_THRESHOLD = "verificationThreshold";
  private static final String CONFIG_TIMEOUT_SECONDS = "timeoutSeconds";
  public static final String CONFIG_LIVENESS_MODE = "livenessMode";
  private static final String CONFIG_FALLBACK_ENABLED = "fallbackEnabled";

  @Override
  public String getDisplayType() {
    return DISPLAY_NAME;
  }

  @Override
  public String getReferenceCategory() {
    return "biometric";
  }

  @Override
  public boolean isConfigurable() {
    return true;
  }

  @Override
  public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
    return new AuthenticationExecutionModel.Requirement[] {
      AuthenticationExecutionModel.Requirement.REQUIRED,
      AuthenticationExecutionModel.Requirement.ALTERNATIVE,
      AuthenticationExecutionModel.Requirement.DISABLED
    };
  }

  @Override
  public boolean isUserSetupAllowed() {
    return true;
  }

  @Override
  public String getHelpText() {
    return HELP_TEXT;
  }

  @Override
  public List<ProviderConfigProperty> getConfigProperties() {
    return ProviderConfigurationBuilder.create()
        .property()
        .name(CONFIG_MAX_RETRIES)
        .label("Maximum Retry Attempts")
        .helpText("Maximum number of verification attempts before fallback (default: 3)")
        .type(ProviderConfigProperty.STRING_TYPE)
        .defaultValue("3")
        .add()
        .property()
        .name(CONFIG_VERIFICATION_THRESHOLD)
        .label("Verification Threshold")
        .helpText(
            "Minimum score required for successful verification (0.0-1.0, lower = stricter, default: 0.015)")
        .type(ProviderConfigProperty.STRING_TYPE)
        .defaultValue("0.015")
        .add()
        .property()
        .name(CONFIG_TIMEOUT_SECONDS)
        .label("Verification Timeout")
        .helpText("Maximum time in seconds for verification request (default: 4)")
        .type(ProviderConfigProperty.STRING_TYPE)
        .defaultValue("4")
        .add()
        .property()
        .name(CONFIG_LIVENESS_MODE)
        .label("Liveness Detection Mode")
        .helpText("Liveness detection mode: NONE, PASSIVE, ACTIVE, CHALLENGE_RESPONSE")
        .type(ProviderConfigProperty.LIST_TYPE)
        .options("NONE", "PASSIVE", "ACTIVE", "CHALLENGE_RESPONSE")
        .defaultValue("PASSIVE")
        .add()
        .property()
        .name(CONFIG_FALLBACK_ENABLED)
        .label("Enable Fallback Authentication")
        .helpText("Allow fallback to other authentication methods when face verification fails")
        .type(ProviderConfigProperty.BOOLEAN_TYPE)
        .defaultValue("true")
        .add()
        .build();
  }

  @Override
  public Authenticator create(KeycloakSession session) {
    return new FaceAuthenticator(session);
  }

  @Override
  public void init(Config.Scope config) {
    // No initialization needed
  }

  @Override
  public void postInit(KeycloakSessionFactory factory) {
    // No post-initialization needed
  }

  @Override
  public void close() {
    // No resources to close
  }

  @Override
  public String getId() {
    return PROVIDER_ID;
  }

  /** Gets the maximum retry attempts from configuration. */
  public static int getMaxRetries(org.keycloak.models.AuthenticatorConfigModel config) {
    if (config == null) {
      return 3; // default
    }

    String value = config.getConfig().get(CONFIG_MAX_RETRIES);
    try {
      return value != null ? Integer.parseInt(value) : 3;
    } catch (NumberFormatException e) {
      return 3;
    }
  }

  /** Gets the verification threshold from configuration. */
  public static double getVerificationThreshold(
      org.keycloak.models.AuthenticatorConfigModel config) {
    if (config == null) {
      return 0.015; // default
    }

    String value = config.getConfig().get(CONFIG_VERIFICATION_THRESHOLD);
    try {
      return value != null ? Double.parseDouble(value) : 0.015;
    } catch (NumberFormatException e) {
      return 0.015;
    }
  }

  /** Gets the timeout seconds from configuration. */
  public static int getTimeoutSeconds(org.keycloak.models.AuthenticatorConfigModel config) {
    if (config == null) {
      return 4; // default
    }

    String value = config.getConfig().get(CONFIG_TIMEOUT_SECONDS);
    try {
      return value != null ? Integer.parseInt(value) : 4;
    } catch (NumberFormatException e) {
      return 4;
    }
  }

  /** Gets the liveness mode from configuration. */
  public static String getLivenessMode(org.keycloak.models.AuthenticatorConfigModel config) {
    if (config == null) {
      return "PASSIVE"; // default
    }

    return config.getConfig().getOrDefault(CONFIG_LIVENESS_MODE, "PASSIVE");
  }

  /** Gets the fallback enabled flag from configuration. */
  public static boolean isFallbackEnabled(org.keycloak.models.AuthenticatorConfigModel config) {
    if (config == null) {
      return true; // default
    }

    String value = config.getConfig().get(CONFIG_FALLBACK_ENABLED);
    return value == null || Boolean.parseBoolean(value);
  }
}
