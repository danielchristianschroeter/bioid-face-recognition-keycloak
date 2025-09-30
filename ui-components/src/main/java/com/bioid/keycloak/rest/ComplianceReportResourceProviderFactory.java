package com.bioid.keycloak.rest;

import org.keycloak.Config.Scope;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;

/** Factory for creating compliance report resource providers. */
public class ComplianceReportResourceProviderFactory implements RealmResourceProviderFactory {

  public static final String ID = "bioid-compliance-reports";

  @Override
  public RealmResourceProvider create(KeycloakSession session) {
    return new ComplianceReportResourceProvider(session);
  }

  @Override
  public void init(Scope config) {
    // Nothing to initialize
  }

  @Override
  public void postInit(KeycloakSessionFactory factory) {
    // Nothing to post-initialize
  }

  @Override
  public void close() {
    // Nothing to close
  }

  @Override
  public String getId() {
    return ID;
  }
}
