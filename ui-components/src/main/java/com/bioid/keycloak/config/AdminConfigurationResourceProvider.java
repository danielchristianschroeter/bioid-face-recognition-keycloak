package com.bioid.keycloak.config;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.resource.RealmResourceProvider;

/**
 * Resource provider for administrative configuration endpoints.
 * 
 * Requirements addressed: 3.2, 6.1, 9.1
 */
public class AdminConfigurationResourceProvider implements RealmResourceProvider {
    
    private final KeycloakSession session;
    
    public AdminConfigurationResourceProvider(KeycloakSession session) {
        this.session = session;
    }
    
    @Override
    public Object getResource() {
        RealmModel realm = session.getContext().getRealm();
        AuthenticationManager.AuthResult auth = new AppAuthManager.BearerTokenAuthenticator(session)
            .authenticate();
        
        return new AdminConfigurationResource(session, realm, auth);
    }
    
    @Override
    public void close() {
        // No resources to close
    }
}