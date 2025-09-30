package com.bioid.keycloak.admin;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.resource.RealmResourceProvider;

/**
 * Resource provider for Face Recognition admin functionality. Provides the admin REST resource for
 * managing face recognition settings.
 */
public class FaceRecognitionAdminResourceProvider implements RealmResourceProvider {

  private final KeycloakSession session;

  public FaceRecognitionAdminResourceProvider(KeycloakSession session) {
    this.session = session;
  }

  @Override
  public Object getResource() {
    RealmModel realm = session.getContext().getRealm();
    return new FaceRecognitionAdminResource(session, realm);
  }

  @Override
  public void close() {
    // No resources to close
  }
}
