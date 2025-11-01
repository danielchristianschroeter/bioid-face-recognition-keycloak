package com.bioid.keycloak.admin.rest;

import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resource.RealmResourceProvider;

/**
 * Provider for the BWS Admin REST API.
 * 
 * This provider makes the BWS Admin API available at:
 * /realms/{realm}/bws-admin/*
 * 
 * @author BioID Keycloak Extension
 * @version 1.0.0
 */
public class BWSAdminResourceProvider implements RealmResourceProvider {

  private final KeycloakSession session;

  public BWSAdminResourceProvider(KeycloakSession session) {
    this.session = session;
  }

  @Override
  public Object getResource() {
    return new BWSAdminResource(session);
  }

  @Override
  public void close() {
    // No resources to close
  }
}
