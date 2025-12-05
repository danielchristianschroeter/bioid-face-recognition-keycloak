package com.bioid.keycloak.failedauth.rest;

import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resource.RealmResourceProvider;

public class FailedAuthResourceProvider implements RealmResourceProvider {
    
    private final KeycloakSession session;
    
    public FailedAuthResourceProvider(KeycloakSession session) {
        this.session = session;
    }
    
    @Override
    public Object getResource() {
        return new FailedAuthResource(session);
    }
    
    @Override
    public void close() {
    }
}
