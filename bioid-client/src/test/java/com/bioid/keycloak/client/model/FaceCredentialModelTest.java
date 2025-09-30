package com.bioid.keycloak.client.model;

import static org.assertj.core.api.Assertions.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for FaceCredentialModel record. Tests validation, immutability, and builder
 * functionality.
 */
class FaceCredentialModelTest {

  @Test
  @DisplayName("Should create valid FaceCredentialModel with all fields")
  void shouldCreateValidModel() {
    // Given
    long classId = 1234567890123456789L;
    Instant now = Instant.now();
    Instant expiresAt = now.plusSeconds(365 * 24 * 60 * 60); // 1 year
    List<String> tags = List.of("employee", "high-security");
    Map<String, String> metadata =
        Map.of("userAgent", "Chrome/120.0", "ipAddress", "192.168.1.100");

    // When
    FaceCredentialModel model =
        new FaceCredentialModel(
            classId,
            now,
            now,
            expiresAt,
            3,
            5,
            3,
            3,
            TemplateType.STANDARD,
            tags,
            EnrollmentAction.NEW_TEMPLATE_CREATED,
            metadata);

    // Then
    assertThat(model.classId()).isEqualTo(classId);
    assertThat(model.createdAt()).isEqualTo(now);
    assertThat(model.updatedAt()).isEqualTo(now);
    assertThat(model.expiresAt()).isEqualTo(expiresAt);
    assertThat(model.imageCount()).isEqualTo(3);
    assertThat(model.encoderVersion()).isEqualTo(5);
    assertThat(model.featureVectors()).isEqualTo(3);
    assertThat(model.thumbnailsStored()).isEqualTo(3);
    assertThat(model.templateType()).isEqualTo(TemplateType.STANDARD);
    assertThat(model.tags()).containsExactly("employee", "high-security");
    assertThat(model.enrollmentAction()).isEqualTo(EnrollmentAction.NEW_TEMPLATE_CREATED);
    assertThat(model.enrollmentMetadata()).containsEntry("userAgent", "Chrome/120.0");
  }

  @Test
  @DisplayName("Should create model with builder pattern")
  void shouldCreateModelWithBuilder() {
    // Given
    long classId = 9876543210987654L;
    Instant now = Instant.now();

    // When
    FaceCredentialModel model =
        FaceCredentialModel.builder()
            .classId(classId)
            .createdAt(now)
            .imageCount(5)
            .encoderVersion(6)
            .templateType(TemplateType.FULL)
            .tags(List.of("admin", "privileged"))
            .build();

    // Then
    assertThat(model.classId()).isEqualTo(classId);
    assertThat(model.createdAt()).isEqualTo(now);
    assertThat(model.imageCount()).isEqualTo(5);
    assertThat(model.templateType()).isEqualTo(TemplateType.FULL);
    assertThat(model.tags()).containsExactly("admin", "privileged");
  }

  @Test
  @DisplayName("Should apply defaults for null values")
  void shouldApplyDefaults() {
    // Given
    long classId = 1111111111111111111L;
    Instant now = Instant.now();

    // When
    FaceCredentialModel model =
        new FaceCredentialModel(classId, now, null, null, 3, 5, 3, 3, null, null, null, null);

    // Then
    assertThat(model.updatedAt()).isEqualTo(now); // Should default to createdAt
    assertThat(model.expiresAt()).isAfter(now); // Should default to 2 years from creation
    assertThat(model.templateType()).isEqualTo(TemplateType.STANDARD); // Default template type
    assertThat(model.enrollmentAction())
        .isEqualTo(EnrollmentAction.NEW_TEMPLATE_CREATED); // Default action
    assertThat(model.tags()).isEmpty(); // Empty list
    assertThat(model.enrollmentMetadata()).isEmpty(); // Empty map
  }

  @Test
  @DisplayName("Should validate class ID is positive")
  void shouldValidateClassIdIsPositive() {
    // Given
    Instant now = Instant.now();

    // When & Then
    assertThatThrownBy(
            () ->
                new FaceCredentialModel(
                    0L,
                    now,
                    now,
                    now.plusSeconds(3600),
                    3,
                    5,
                    3,
                    3,
                    TemplateType.STANDARD,
                    List.of(),
                    EnrollmentAction.NEW_TEMPLATE_CREATED,
                    Map.of()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Class ID must be positive");

    assertThatThrownBy(
            () ->
                new FaceCredentialModel(
                    -1L,
                    now,
                    now,
                    now.plusSeconds(3600),
                    3,
                    5,
                    3,
                    3,
                    TemplateType.STANDARD,
                    List.of(),
                    EnrollmentAction.NEW_TEMPLATE_CREATED,
                    Map.of()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Class ID must be positive");
  }

  @Test
  @DisplayName("Should validate created timestamp is not null")
  void shouldValidateCreatedTimestampNotNull() {
    // When & Then
    assertThatThrownBy(
            () ->
                new FaceCredentialModel(
                    1234567890123456789L,
                    null,
                    Instant.now(),
                    Instant.now().plusSeconds(3600),
                    3,
                    5,
                    3,
                    3,
                    TemplateType.STANDARD,
                    List.of(),
                    EnrollmentAction.NEW_TEMPLATE_CREATED,
                    Map.of()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Created timestamp cannot be null");
  }

  @Test
  @DisplayName("Should validate image count is not negative")
  void shouldValidateImageCountNotNegative() {
    // Given
    Instant now = Instant.now();

    // When & Then
    assertThatThrownBy(
            () ->
                new FaceCredentialModel(
                    1234567890123456789L,
                    now,
                    now,
                    now.plusSeconds(3600),
                    -1,
                    5,
                    3,
                    3,
                    TemplateType.STANDARD,
                    List.of(),
                    EnrollmentAction.NEW_TEMPLATE_CREATED,
                    Map.of()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Image count cannot be negative");
  }

  @Test
  @DisplayName("Should check expiration correctly")
  void shouldCheckExpiration() {
    // Given
    Instant now = Instant.now();
    Instant pastExpiration = now.minusSeconds(3600); // 1 hour ago
    Instant futureExpiration = now.plusSeconds(3600); // 1 hour from now

    FaceCredentialModel expiredModel =
        FaceCredentialModel.builder()
            .classId(1234567890123456789L)
            .createdAt(now.minusSeconds(7200))
            .expiresAt(pastExpiration)
            .build();

    FaceCredentialModel validModel =
        FaceCredentialModel.builder()
            .classId(1234567890123456789L)
            .createdAt(now)
            .expiresAt(futureExpiration)
            .build();

    // When & Then
    assertThat(expiredModel.isExpired()).isTrue();
    assertThat(validModel.isExpired()).isFalse();

    assertThat(validModel.expiresWithin(7200)).isTrue(); // Expires within 2 hours
    assertThat(validModel.expiresWithin(1800)).isFalse(); // Does not expire within 30 minutes
  }

  @Test
  @DisplayName("Should create immutable collections")
  void shouldCreateImmutableCollections() {
    // Given
    List<String> mutableTags = List.of("tag1", "tag2");
    Map<String, String> mutableMetadata = Map.of("key1", "value1");

    FaceCredentialModel model =
        FaceCredentialModel.builder()
            .classId(1234567890123456789L)
            .createdAt(Instant.now())
            .tags(mutableTags)
            .enrollmentMetadata(mutableMetadata)
            .build();

    // When & Then
    assertThat(model.tags()).isInstanceOf(List.class);
    assertThat(model.enrollmentMetadata()).isInstanceOf(Map.class);

    // Verify immutability by checking that modifications don't affect the model
    assertThatThrownBy(() -> model.tags().add("tag3"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  @DisplayName("Should support toBuilder pattern")
  void shouldSupportToBuilderPattern() {
    // Given
    FaceCredentialModel original =
        FaceCredentialModel.builder()
            .classId(1234567890123456789L)
            .createdAt(Instant.now())
            .imageCount(3)
            .templateType(TemplateType.STANDARD)
            .build();

    // When
    FaceCredentialModel modified =
        original.toBuilder().imageCount(5).templateType(TemplateType.FULL).build();

    // Then
    assertThat(modified.classId()).isEqualTo(original.classId());
    assertThat(modified.createdAt()).isEqualTo(original.createdAt());
    assertThat(modified.imageCount()).isEqualTo(5); // Modified
    assertThat(modified.templateType()).isEqualTo(TemplateType.FULL); // Modified

    // Original should remain unchanged
    assertThat(original.imageCount()).isEqualTo(3);
    assertThat(original.templateType()).isEqualTo(TemplateType.STANDARD);
  }
}
