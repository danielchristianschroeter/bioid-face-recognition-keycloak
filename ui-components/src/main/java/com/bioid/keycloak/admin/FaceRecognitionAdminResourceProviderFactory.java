package com.bioid.keycloak.admin;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;

/**
 * Factory for Face Recognition admin resource provider. Registers the admin resource provider with
 * Keycloak.
 */
public class FaceRecognitionAdminResourceProviderFactory implements RealmResourceProviderFactory {

  public static final String ID = "face-recognition-admin";

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public RealmResourceProvider create(KeycloakSession session) {
    return new FaceRecognitionAdminResourceProvider(session);
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
}
