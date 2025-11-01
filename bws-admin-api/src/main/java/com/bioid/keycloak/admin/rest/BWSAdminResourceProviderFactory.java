package com.bioid.keycloak.admin.rest;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating BWS Admin REST API providers.
 * 
 * This factory is automatically discovered by Keycloak's SPI mechanism
 * through the META-INF/services configuration.
 * 
 * @author BioID Keycloak Extension
 * @version 1.0.0
 */
public class BWSAdminResourceProviderFactory implements RealmResourceProviderFactory {

  private static final Logger logger = LoggerFactory.getLogger(BWSAdminResourceProviderFactory.class);
  
  public static final String ID = "bws-admin";

  @Override
  public RealmResourceProvider create(KeycloakSession session) {
    return new BWSAdminResourceProvider(session);
  }

  @Override
  public void init(Config.Scope config) {
    logger.info("Initializing BWS Admin API");
    // No initialization needed
  }

  @Override
  public void postInit(KeycloakSessionFactory factory) {
    logger.info("BWS Admin API initialized successfully");
    // No post-initialization needed
  }

  @Override
  public void close() {
    logger.info("Closing BWS Admin API");
    // No resources to close
  }

  @Override
  public String getId() {
    return ID;
  }
}
