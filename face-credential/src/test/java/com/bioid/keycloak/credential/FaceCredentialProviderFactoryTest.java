package com.bioid.keycloak.credential;

import static org.assertj.core.api.Assertions.*;

import com.bioid.keycloak.client.BioIdClient;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for FaceCredentialProviderFactory. Tests factory functionality and configuration
 * properties.
 */
@ExtendWith(MockitoExtension.class)
class FaceCredentialProviderFactoryTest {

  @Mock private KeycloakSession session;
  @Mock private BioIdClient mockBioIdClient;

  private FaceCredentialProviderFactory factory;

  @BeforeEach
  void setUp() {
    factory = new FaceCredentialProviderFactory();
    FaceCredentialProviderFactory.setSharedBioIdClientForTesting(mockBioIdClient);
  }

  @AfterEach
  void tearDown() {
    FaceCredentialProviderFactory.setSharedBioIdClientForTesting(null);
  }

  @Test
  @DisplayName("Should return correct provider ID")
  void shouldReturnCorrectProviderId() {
    assertThat(factory.getId()).isEqualTo("face-credential");
  }

  @Test
  @DisplayName("Should create provider instance")
  void shouldCreateProviderInstance() {
    FaceCredentialProvider provider = factory.create(session);

    assertThat(provider).isNotNull();
    assertThat(provider).isInstanceOf(FaceCredentialProvider.class);
  }

  @Test
  @DisplayName("Should provide configuration properties")
  void shouldProvideConfigurationProperties() {
    List<ProviderConfigProperty> properties = factory.getConfigProperties();

    assertThat(properties).isNotNull();
    assertThat(properties).hasSize(3);

    // Check TTL property
    ProviderConfigProperty ttlProperty =
        properties.stream()
            .filter(prop -> "face.credential.ttl.days".equals(prop.getName()))
            .findFirst()
            .orElse(null);

    assertThat(ttlProperty).isNotNull();
    assertThat(ttlProperty.getLabel()).isEqualTo("Credential TTL (days)");
    assertThat(ttlProperty.getDefaultValue()).isEqualTo("730");
    assertThat(ttlProperty.getType()).isEqualTo(ProviderConfigProperty.STRING_TYPE);

    // Check cleanup property
    ProviderConfigProperty cleanupProperty =
        properties.stream()
            .filter(prop -> "face.credential.cleanup.enabled".equals(prop.getName()))
            .findFirst()
            .orElse(null);

    assertThat(cleanupProperty).isNotNull();
    assertThat(cleanupProperty.getLabel()).isEqualTo("Enable automatic cleanup");
    assertThat(cleanupProperty.getDefaultValue()).isEqualTo("true");
    assertThat(cleanupProperty.getType()).isEqualTo(ProviderConfigProperty.BOOLEAN_TYPE);

    // Check max credentials property
    ProviderConfigProperty maxCredsProperty =
        properties.stream()
            .filter(prop -> "face.credential.max.per.user".equals(prop.getName()))
            .findFirst()
            .orElse(null);

    assertThat(maxCredsProperty).isNotNull();
    assertThat(maxCredsProperty.getLabel()).isEqualTo("Max credentials per user");
    assertThat(maxCredsProperty.getDefaultValue()).isEqualTo("3");
    assertThat(maxCredsProperty.getType()).isEqualTo(ProviderConfigProperty.STRING_TYPE);
  }

  @Test
  @DisplayName("Should handle init and postInit without errors")
  void shouldHandleInitAndPostInitWithoutErrors() {
    // These methods should not throw exceptions
    assertThatCode(
            () -> {
              factory.init(null);
              factory.postInit(null);
            })
        .doesNotThrowAnyException();
  }

  @Test
  @DisplayName("Should handle close without errors")
  void shouldHandleCloseWithoutErrors() {
    assertThatCode(() -> factory.close()).doesNotThrowAnyException();
  }
}
