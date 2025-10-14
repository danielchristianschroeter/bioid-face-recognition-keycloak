package com.bioid.keycloak.config;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;

/**
 * Factory for administrative configuration resource provider.
 * 
 * Requirements addressed: 3.2, 6.1, 9.1
 */
public class AdminConfigurationResourceProviderFactory implements RealmResourceProviderFactory {
    
    public static final String ID = "bioid-admin-config";
    
    @Override
    public RealmResourceProvider create(KeycloakSession session) {
        return new AdminConfigurationResourceProvider(session);
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