package com.bioid.keycloak.client.auth;

import static org.assertj.core.api.Assertions.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for BioIdJwtTokenProvider. Tests token generation, validation, caching, and thread
 * safety.
 */
class BioIdJwtTokenProviderTest {

  private static final String TEST_CLIENT_ID = "test-client-id";
  // Valid base64 encoded test key (64 bytes when decoded)
  private static final String TEST_SECRET_KEY =
      "dGVzdC1zZWNyZXQta2V5LWZvci1qd3Qtc2lnbmluZy1tdXN0LWJlLWxvbmctZW5vdWdoLWZvci10ZXN0aW5nLXB1cnBvc2VzLW9ubHk=";
  private static final int TEST_EXPIRE_MINUTES = 60;

  @Test
  @DisplayName("Should create token provider with valid configuration")
  void shouldCreateTokenProviderWithValidConfig() {
    // When
    BioIdJwtTokenProvider provider =
        new BioIdJwtTokenProvider(TEST_CLIENT_ID, TEST_SECRET_KEY, TEST_EXPIRE_MINUTES);

    // Then
    assertThat(provider.getClientId()).isEqualTo(TEST_CLIENT_ID);
    assertThat(provider.getExpireMinutes()).isEqualTo(TEST_EXPIRE_MINUTES);
  }

  @Test
  @DisplayName("Should validate constructor parameters")
  void shouldValidateConstructorParameters() {
    // When & Then
    assertThatThrownBy(() -> new BioIdJwtTokenProvider(null, TEST_SECRET_KEY, TEST_EXPIRE_MINUTES))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Client ID cannot be null");

    assertThatThrownBy(() -> new BioIdJwtTokenProvider("", TEST_SECRET_KEY, TEST_EXPIRE_MINUTES))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Client ID cannot be null or empty");

    assertThatThrownBy(() -> new BioIdJwtTokenProvider(TEST_CLIENT_ID, null, TEST_EXPIRE_MINUTES))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Secret key cannot be null");

    assertThatThrownBy(() -> new BioIdJwtTokenProvider(TEST_CLIENT_ID, "", TEST_EXPIRE_MINUTES))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Secret key cannot be null or empty");

    assertThatThrownBy(() -> new BioIdJwtTokenProvider(TEST_CLIENT_ID, TEST_SECRET_KEY, 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Expire minutes must be positive");

    assertThatThrownBy(() -> new BioIdJwtTokenProvider(TEST_CLIENT_ID, TEST_SECRET_KEY, -1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Expire minutes must be positive");
  }

  @Test
  @DisplayName("Should generate valid JWT token")
  void shouldGenerateValidJwtToken() {
    // Given
    BioIdJwtTokenProvider provider =
        new BioIdJwtTokenProvider(TEST_CLIENT_ID, TEST_SECRET_KEY, TEST_EXPIRE_MINUTES);

    // When
    String token = provider.getToken();

    // Then
    assertThat(token).isNotNull();
    assertThat(token).isNotEmpty();
    assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts separated by dots
    assertThat(provider.isTokenExpired(token)).isFalse();
  }

  @Test
  @DisplayName("Should cache and reuse valid tokens")
  void shouldCacheAndReuseValidTokens() {
    // Given
    BioIdJwtTokenProvider provider =
        new BioIdJwtTokenProvider(TEST_CLIENT_ID, TEST_SECRET_KEY, TEST_EXPIRE_MINUTES);

    // When
    String token1 = provider.getToken();
    String token2 = provider.getToken();

    // Then
    assertThat(token1).isEqualTo(token2); // Should return cached token
  }

  @Test
  @DisplayName("Should generate new token after invalidation")
  void shouldGenerateNewTokenAfterInvalidation() throws InterruptedException {
    // Given
    BioIdJwtTokenProvider provider =
        new BioIdJwtTokenProvider(TEST_CLIENT_ID, TEST_SECRET_KEY, TEST_EXPIRE_MINUTES);

    // When
    String token1 = provider.getToken();
    provider.invalidateToken();

    // Wait a bit to ensure different timestamp
    Thread.sleep(1000);

    String token2 = provider.getToken();

    // Then
    assertThat(token1).isNotEqualTo(token2); // Should generate new token
    assertThat(provider.isTokenExpired(token1)).isFalse(); // Old token still valid
    assertThat(provider.isTokenExpired(token2)).isFalse(); // New token valid
  }

  @Test
  @DisplayName("Should handle short expiration times correctly")
  void shouldHandleShortExpirationTimes() throws InterruptedException {
    // Given - Very short expiration for testing
    BioIdJwtTokenProvider provider =
        new BioIdJwtTokenProvider(
            TEST_CLIENT_ID, TEST_SECRET_KEY, 1 // 1 minute
            );

    // When
    String token1 = provider.getToken();

    // Wait a bit to ensure renewal buffer kicks in
    Thread.sleep(100);

    String token2 = provider.getToken();

    // Then
    assertThat(token1).isNotNull();
    assertThat(token2).isNotNull();
    // Tokens might be same or different depending on timing
  }

  @Test
  @DisplayName("Should be thread-safe under concurrent access")
  void shouldBeThreadSafeUnderConcurrentAccess() throws InterruptedException {
    // Given
    BioIdJwtTokenProvider provider =
        new BioIdJwtTokenProvider(TEST_CLIENT_ID, TEST_SECRET_KEY, TEST_EXPIRE_MINUTES);

    int threadCount = 10;
    int operationsPerThread = 100;
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch latch = new CountDownLatch(threadCount);

    // When - Multiple threads accessing token concurrently
    for (int i = 0; i < threadCount; i++) {
      executor.submit(
          () -> {
            try {
              for (int j = 0; j < operationsPerThread; j++) {
                String token = provider.getToken();
                assertThat(token).isNotNull();
                assertThat(token).isNotEmpty();

                // Occasionally invalidate to test renewal
                if (j % 50 == 0) {
                  provider.invalidateToken();
                }
              }
            } finally {
              latch.countDown();
            }
          });
    }

    // Then
    assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
    executor.shutdown();

    // Final token should still be valid
    String finalToken = provider.getToken();
    assertThat(finalToken).isNotNull();
    assertThat(provider.isTokenExpired(finalToken)).isFalse();
  }

  @Test
  @DisplayName("Should detect expired tokens correctly")
  void shouldDetectExpiredTokensCorrectly() {
    // Given
    BioIdJwtTokenProvider provider =
        new BioIdJwtTokenProvider(TEST_CLIENT_ID, TEST_SECRET_KEY, TEST_EXPIRE_MINUTES);

    // When & Then
    assertThat(provider.isTokenExpired(null)).isTrue();
    assertThat(provider.isTokenExpired("")).isTrue();
    assertThat(provider.isTokenExpired("invalid-token")).isTrue();

    String validToken = provider.getToken();
    assertThat(provider.isTokenExpired(validToken)).isFalse();
  }

  @Test
  @DisplayName("Should handle whitespace in configuration")
  void shouldHandleWhitespaceInConfiguration() {
    // Given
    String clientIdWithSpaces = "  " + TEST_CLIENT_ID + "  ";
    String secretKeyWithSpaces = "  " + TEST_SECRET_KEY + "  ";

    // When
    BioIdJwtTokenProvider provider =
        new BioIdJwtTokenProvider(clientIdWithSpaces, secretKeyWithSpaces, TEST_EXPIRE_MINUTES);

    // Then
    assertThat(provider.getClientId()).isEqualTo(TEST_CLIENT_ID); // Trimmed

    String token = provider.getToken();
    assertThat(token).isNotNull();
    assertThat(provider.isTokenExpired(token)).isFalse();
  }

  @Test
  @DisplayName("Should generate different tokens for different clients")
  void shouldGenerateDifferentTokensForDifferentClients() {
    // Given
    BioIdJwtTokenProvider provider1 =
        new BioIdJwtTokenProvider("client1", TEST_SECRET_KEY, TEST_EXPIRE_MINUTES);
    BioIdJwtTokenProvider provider2 =
        new BioIdJwtTokenProvider("client2", TEST_SECRET_KEY, TEST_EXPIRE_MINUTES);

    // When
    String token1 = provider1.getToken();
    String token2 = provider2.getToken();

    // Then
    assertThat(token1).isNotEqualTo(token2); // Different clients should have different tokens
  }
}
