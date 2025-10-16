package com.bioid.keycloak.rest;

import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resource.RealmResourceProvider;

/**
 * Provider for the Face Credential REST API.
 */
public class FaceCredentialResourceProvider implements RealmResourceProvider {

  private final KeycloakSession session;

  public FaceCredentialResourceProvider(KeycloakSession session) {
    this.session = session;
  }

  @Override
  public Object getResource() {
    return new FaceCredentialResource(session);
  }

  @Override
  public void close() {
    // No resources to close
  }
}
