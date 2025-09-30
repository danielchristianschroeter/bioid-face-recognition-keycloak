package com.bioid.keycloak.credential;

import java.util.List;
import org.keycloak.Config;
import org.keycloak.credential.CredentialProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

/**
 * Factory for creating FaceCredentialProvider instances.
 *
 * <p>This factory is responsible for creating and configuring face credential providers for each
 * Keycloak session. It integrates with Keycloak's SPI framework to provide face biometric
 * credential management capabilities.
 *
 * <p>Features: - Session-scoped provider creation - Configuration management - Integration with
 * Keycloak's credential framework - Proper resource lifecycle management
 */
public class FaceCredentialProviderFactory
    implements CredentialProviderFactory<FaceCredentialProvider> {

  public static final String PROVIDER_ID = "face-credential";

  @Override
  public String getId() {
    return PROVIDER_ID;
  }

  @Override
  public FaceCredentialProvider create(KeycloakSession session) {
    try {
      System.out.println("DEBUG: Creating FaceCredentialProvider for session: " + session);
      FaceCredentialProvider provider = new FaceCredentialProvider(session);
      System.out.println("DEBUG: Successfully created FaceCredentialProvider: " + provider);
      return provider;
    } catch (Exception e) {
      System.err.println("ERROR: Failed to create FaceCredentialProvider: " + e.getMessage());
      e.printStackTrace();
      throw e;
    }
  }

  @Override
  public void init(Config.Scope config) {
    // No initialization required
  }

  @Override
  public void postInit(KeycloakSessionFactory factory) {
    // No post-initialization required
  }

  @Override
  public void close() {
    // No resources to close
  }

  /**
   * Gets the configuration properties for the face credential provider.
   *
   * @return list of configuration properties
   */
  public List<ProviderConfigProperty> getConfigProperties() {
    return ProviderConfigurationBuilder.create()
        .property()
        .name("face.credential.ttl.days")
        .label("Credential TTL (days)")
        .helpText("Time-to-live for face credentials in days")
        .type(ProviderConfigProperty.STRING_TYPE)
        .defaultValue("730")
        .add()
        .property()
        .name("face.credential.cleanup.enabled")
        .label("Enable automatic cleanup")
        .helpText("Enable automatic cleanup of expired credentials")
        .type(ProviderConfigProperty.BOOLEAN_TYPE)
        .defaultValue("true")
        .add()
        .property()
        .name("face.credential.max.per.user")
        .label("Max credentials per user")
        .helpText("Maximum number of face credentials per user")
        .type(ProviderConfigProperty.STRING_TYPE)
        .defaultValue("3")
        .add()
        .build();
  }
}
