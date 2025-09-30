package com.bioid.keycloak.events;

import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

/**
 * Factory for creating Face Recognition Event Listener instances.
 *
 * <p>Registers the event listener with Keycloak's SPI system for automatic instantiation and
 * lifecycle management.
 */
public class FaceRecognitionEventListenerProviderFactory implements EventListenerProviderFactory {

  public static final String PROVIDER_ID = "face-recognition-events";

  @Override
  public EventListenerProvider create(KeycloakSession session) {
    return new FaceRecognitionEventListener();
  }

  @Override
  public void init(Config.Scope config) {
    // Initialize any configuration if needed
  }

  @Override
  public void postInit(KeycloakSessionFactory factory) {
    // Post-initialization if needed
  }

  @Override
  public void close() {
    // Cleanup resources if needed
  }

  @Override
  public String getId() {
    return PROVIDER_ID;
  }
}
