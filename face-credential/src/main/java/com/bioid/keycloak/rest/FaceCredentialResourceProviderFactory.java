package com.bioid.keycloak.rest;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;

/**
 * Factory for creating Face Credential REST API providers.
 */
public class FaceCredentialResourceProviderFactory implements RealmResourceProviderFactory {

  public static final String ID = "face-api";

  @Override
  public RealmResourceProvider create(KeycloakSession session) {
    return new FaceCredentialResourceProvider(session);
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
    return ID;
  }
}
