package com.bioid.keycloak.rest;

import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resource.RealmResourceProvider;

/** Provider for the compliance report REST resource. */
public class ComplianceReportResourceProvider implements RealmResourceProvider {

  private final KeycloakSession session;

  public ComplianceReportResourceProvider(KeycloakSession session) {
    this.session = session;
  }

  @Override
  public Object getResource() {
    return new ComplianceReportResource(session);
  }

  @Override
  public void close() {
    // Nothing to close
  }
}
