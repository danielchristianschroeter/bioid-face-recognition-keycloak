package com.bioid.keycloak.client;

import com.bioid.keycloak.client.config.BioIdConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper factory for creating BioIdClient instances from the shared BioID configuration.
 *
 * <p>Centralising the creation logic avoids leaking low level configuration classes to other
 * modules (e.g. Keycloak providers) and makes it easier to adjust how we bootstrap the gRPC
 * clients in one place.</p>
 */
public final class BioIdClientFactory {

  private static final Logger logger = LoggerFactory.getLogger(BioIdClientFactory.class);
  private static final Object lock = new Object();

  private static volatile BioIdClient sharedClient;

  private BioIdClientFactory() {}

  /**
   * Returns a shared production client instance, creating it on demand.
   *
   * @return shared BioIdClient or {@code null} when configuration is incomplete.
   */
  public static BioIdClient getSharedClient() {
    if (sharedClient == null) {
      synchronized (lock) {
        if (sharedClient == null) {
          sharedClient = createProductionClient();
        }
      }
    }
    return sharedClient;
  }

  /**
   * Creates a new production BioIdClient instance using the global BioIdConfiguration.
   *
   * @return BioIdClient or {@code null} if configuration is invalid/incomplete.
   */
  public static BioIdClient createProductionClient() {
    try {
      BioIdConfiguration config = BioIdConfiguration.getInstance();

      if (isBlank(config.getClientId()) || isBlank(config.getKey())) {
        logger.error("BioID credentials not configured. "
            + "Set BWS_CLIENT_ID and BWS_KEY environment variables.");
        return null;
      }

      String endpoint = config.getEndpoint();
      if (isBlank(endpoint)) {
        logger.error("BioID endpoint not configured. Set BWS_ENDPOINT environment variable.");
        return null;
      }

      logger.info("Initialising BioID gRPC client with endpoint {}", endpoint);
      return new BioIdGrpcClientProduction(config, endpoint, config.getClientId(), config.getKey());
    } catch (Exception e) {
      logger.error("Failed to initialise BioID gRPC client", e);
      return null;
    }
  }

  private static boolean isBlank(String value) {
    return value == null || value.trim().isEmpty();
  }
}

