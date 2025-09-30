package com.bioid.keycloak.action;

import org.keycloak.Config;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

/**
 * Factory for creating FaceEnrollAction required action providers.
 *
 * <p>This factory integrates with Keycloak's SPI framework to provide face biometric enrollment
 * capabilities as a required action.
 *
 * <p>Features: - Session-scoped provider creation - Integration with Keycloak's required action
 * framework - Proper resource lifecycle management
 */
public class FaceEnrollActionFactory implements RequiredActionFactory {

  public static final String PROVIDER_ID = "face-enroll";

  @Override
  public String getId() {
    return PROVIDER_ID;
  }

  @Override
  public String getDisplayText() {
    return "Face Recognition Enrollment";
  }

  @Override
  public RequiredActionProvider create(KeycloakSession session) {
    return new FaceEnrollAction();
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

  @Override
  public boolean isOneTimeAction() {
    // Face enrollment can be repeated if needed
    return false;
  }
}
