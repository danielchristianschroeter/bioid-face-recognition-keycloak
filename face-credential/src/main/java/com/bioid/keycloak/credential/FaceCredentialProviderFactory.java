package com.bioid.keycloak.credential;

import com.bioid.keycloak.client.BioIdClient;
import com.bioid.keycloak.client.BioIdGrpcClientProduction;
import com.bioid.keycloak.client.config.BioIdConfiguration;
import java.util.List;
import org.keycloak.Config;
import org.keycloak.credential.CredentialProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

  private static final Logger logger = LoggerFactory.getLogger(FaceCredentialProviderFactory.class);
  
  public static final String PROVIDER_ID = "face-credential";
  
  // Singleton gRPC client shared across all provider instances
  private static volatile BioIdClient sharedBioIdClient = null;
  private static final Object clientLock = new Object();

  @Override
  public String getId() {
    return PROVIDER_ID;
  }

  @Override
  public FaceCredentialProvider create(KeycloakSession session) {
    try {
      System.out.println("DEBUG: Creating FaceCredentialProvider for session: " + session);
      FaceCredentialProvider provider = new FaceCredentialProvider(session, getSharedBioIdClient());
      System.out.println("DEBUG: Successfully created FaceCredentialProvider: " + provider);
      return provider;
    } catch (Exception e) {
      System.err.println("ERROR: Failed to create FaceCredentialProvider: " + e.getMessage());
      e.printStackTrace();
      throw e;
    }
  }
  
  /**
   * Gets or creates the shared BioID gRPC client instance.
   * This ensures only one gRPC client is created and reused across all provider instances.
   * 
   * @return shared BioID client instance, or null if configuration is invalid
   */
  private static BioIdClient getSharedBioIdClient() {
    if (sharedBioIdClient == null) {
      synchronized (clientLock) {
        if (sharedBioIdClient == null) {
          try {
            BioIdConfiguration configAdapter = BioIdConfiguration.getInstance();

            // Check if credentials are available
            if (configAdapter.getClientId() == null || configAdapter.getClientId().trim().isEmpty()
                || configAdapter.getKey() == null || configAdapter.getKey().trim().isEmpty()) {
              logger.error(
                  "PRODUCTION ISSUE: BioID credentials not configured. Set BWS_CLIENT_ID and BWS_KEY environment variables. "
                      + "System will use mock implementations which are NOT suitable for production use.");
              return null;
            }

            // Validate endpoint configuration
            String endpoint = configAdapter.getEndpoint();
            if (endpoint == null || endpoint.trim().isEmpty()) {
              logger.error(
                  "PRODUCTION ISSUE: BioID endpoint not configured. Set BWS_ENDPOINT environment variable.");
              return null;
            }

            // Initialize the BioID gRPC client with proper configuration
            logger.info("Initializing shared BioID gRPC client with endpoint: {}", endpoint);

            // Create the production BWS gRPC client (uses proper gRPC protocol)
            sharedBioIdClient = new BioIdGrpcClientProduction(configAdapter,
                endpoint, configAdapter.getClientId(), configAdapter.getKey());

            logger.info("Shared BioID gRPC client initialized successfully with endpoint: {}", endpoint);
          } catch (Exception e) {
            logger.error(
                "PRODUCTION ISSUE: Failed to initialize shared BioID client. Check configuration and network connectivity. "
                    + "Error: {} - System will use mock implementations which are NOT suitable for production use.",
                e.getMessage());
            return null;
          }
        }
      }
    }
    return sharedBioIdClient;
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
    // Clean up shared gRPC client when factory is closed
    synchronized (clientLock) {
      if (sharedBioIdClient != null) {
        try {
          logger.info("Closing shared BioID gRPC client");
          sharedBioIdClient.close();
          sharedBioIdClient = null;
        } catch (Exception e) {
          logger.error("Error closing shared BioID gRPC client", e);
        }
      }
    }
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
