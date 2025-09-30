package com.bioid.keycloak.credential;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for FaceCredentialModel. Tests data model functionality, serialization, and
 * immutability.
 */
class FaceCredentialModelTest {

  private ObjectMapper objectMapper;
  private FaceCredentialModel testModel;
  private FaceCredentialModel.EnrollmentMetadata testMetadata;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());

    testMetadata =
        new FaceCredentialModel.EnrollmentMetadata(
            "Chrome/120.0", "192.168.1.100", "device-fingerprint-hash", "bws-job-id-123");

    testModel =
        FaceCredentialModel.createFaceCredential(
            123456789L,
            3,
            5,
            3,
            3,
            Instant.parse("2026-01-15T10:30:00Z"),
            List.of("employee", "high-security"),
            FaceCredentialModel.TemplateType.STANDARD,
            "NEW_TEMPLATE_CREATED",
            testMetadata);
  }

  @Test
  @DisplayName("Should create face credential model with all properties")
  void shouldCreateFaceCredentialModelWithAllProperties() {
    assertThat(testModel.getClassId()).isEqualTo(123456789L);
    assertThat(testModel.getImageCount()).isEqualTo(3);
    assertThat(testModel.getEncoderVersion()).isEqualTo(5);
    assertThat(testModel.getFeatureVectors()).isEqualTo(3);
    assertThat(testModel.getThumbnailsStored()).isEqualTo(3);
    assertThat(testModel.getExpiresAt()).isEqualTo(Instant.parse("2026-01-15T10:30:00Z"));
    assertThat(testModel.getTags()).containsExactly("employee", "high-security");
    assertThat(testModel.getTemplateType()).isEqualTo(FaceCredentialModel.TemplateType.STANDARD);
    assertThat(testModel.getEnrollmentAction()).isEqualTo("NEW_TEMPLATE_CREATED");
    assertThat(testModel.getEnrollmentMetadata()).isEqualTo(testMetadata);
  }

  @Test
  @DisplayName("Should handle null tags gracefully")
  void shouldHandleNullTagsGracefully() {
    FaceCredentialModel model =
        FaceCredentialModel.createFaceCredential(
            123L,
            3,
            5,
            3,
            3,
            Instant.now().plus(1, ChronoUnit.DAYS),
            null, // null tags
            FaceCredentialModel.TemplateType.COMPACT,
            "NEW_TEMPLATE_CREATED",
            testMetadata);

    assertThat(model.getTags()).isNotNull();
    assertThat(model.getTags()).isEmpty();
  }

  @Test
  @DisplayName("Should return immutable tags list")
  void shouldReturnImmutableTagsList() {
    List<String> tags = testModel.getTags();

    assertThatThrownBy(() -> tags.add("new-tag")).isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  @DisplayName("Should check if credential is expired")
  void shouldCheckIfCredentialIsExpired() {
    // Non-expired credential
    FaceCredentialModel nonExpired =
        FaceCredentialModel.createFaceCredential(
            123L,
            3,
            5,
            3,
            3,
            Instant.now().plus(1, ChronoUnit.DAYS), // Future expiration
            List.of(),
            FaceCredentialModel.TemplateType.COMPACT,
            "NEW_TEMPLATE_CREATED",
            testMetadata);

    // Expired credential
    FaceCredentialModel expired =
        FaceCredentialModel.createFaceCredential(
            123L,
            3,
            5,
            3,
            3,
            Instant.now().minus(1, ChronoUnit.DAYS), // Past expiration
            List.of(),
            FaceCredentialModel.TemplateType.COMPACT,
            "NEW_TEMPLATE_CREATED",
            testMetadata);

    // Credential without expiration
    FaceCredentialModel noExpiration =
        FaceCredentialModel.createFaceCredential(
            123L,
            3,
            5,
            3,
            3,
            null, // No expiration
            List.of(),
            FaceCredentialModel.TemplateType.COMPACT,
            "NEW_TEMPLATE_CREATED",
            testMetadata);

    assertThat(nonExpired.isExpired()).isFalse();
    assertThat(expired.isExpired()).isTrue();
    assertThat(noExpiration.isExpired()).isFalse();
  }

  @Test
  @DisplayName("Should create new model with updated expiration")
  void shouldCreateNewModelWithUpdatedExpiration() {
    Instant newExpiration = Instant.now().plus(365, ChronoUnit.DAYS);

    FaceCredentialModel updated = testModel.withExpiration(newExpiration);

    assertThat(updated).isNotSameAs(testModel);
    assertThat(updated.getExpiresAt()).isCloseTo(newExpiration, within(1, ChronoUnit.SECONDS));
    assertThat(updated.getClassId()).isEqualTo(testModel.getClassId());
    // Don't compare exact creation times as they may differ by milliseconds
    assertThat(updated.getCreatedAt())
        .isCloseTo(testModel.getCreatedAt(), within(1, ChronoUnit.SECONDS));
    assertThat(updated.getTags()).isEqualTo(testModel.getTags());
  }

  @Test
  @DisplayName("Should create new model with updated tags")
  void shouldCreateNewModelWithUpdatedTags() {
    List<String> newTags = List.of("admin", "privileged");

    FaceCredentialModel updated = testModel.withTags(newTags);

    assertThat(updated).isNotSameAs(testModel);
    assertThat(updated.getTags()).containsExactlyElementsOf(newTags);
    assertThat(updated.getClassId()).isEqualTo(testModel.getClassId());
    // Don't compare exact creation times as they may differ by milliseconds
    assertThat(updated.getCreatedAt())
        .isCloseTo(testModel.getCreatedAt(), within(1, ChronoUnit.SECONDS));
    assertThat(updated.getExpiresAt()).isEqualTo(testModel.getExpiresAt());
  }

  @Test
  @DisplayName("Should provide access to credential data")
  void shouldProvideAccessToCredentialData() {
    // Test that we can access the credential data components
    assertThat(testModel.getFaceCredentialData()).isNotNull();
    assertThat(testModel.getFaceSecretData()).isNotNull();
    assertThat(testModel.getFaceCredentialData().getClassId()).isEqualTo(testModel.getClassId());
    assertThat(testModel.getFaceSecretData().getClassId()).isEqualTo(testModel.getClassId());
  }

  @Test
  @DisplayName("Should handle null metadata gracefully")
  void shouldHandleNullMetadataGracefully() {
    FaceCredentialModel modelWithNullMetadata =
        FaceCredentialModel.createFaceCredential(
            123L,
            3,
            5,
            3,
            3,
            Instant.now().plus(1, ChronoUnit.DAYS),
            List.of(),
            FaceCredentialModel.TemplateType.COMPACT,
            "NEW_TEMPLATE_CREATED",
            null);

    assertThat(modelWithNullMetadata.getEnrollmentMetadata()).isNull();
    assertThat(modelWithNullMetadata.getClassId()).isEqualTo(123L);
    assertThat(modelWithNullMetadata.getTemplateType())
        .isEqualTo(FaceCredentialModel.TemplateType.COMPACT);
  }

  @Test
  @DisplayName("Should implement equals and hashCode correctly")
  void shouldImplementEqualsAndHashCodeCorrectly() {
    FaceCredentialModel different =
        FaceCredentialModel.createFaceCredential(
            987654321L, // Different class ID
            3,
            5,
            3,
            3,
            Instant.parse("2026-01-15T10:30:00Z"),
            List.of("employee", "high-security"),
            FaceCredentialModel.TemplateType.STANDARD,
            "NEW_TEMPLATE_CREATED",
            testMetadata);

    // Test equals - compare data content, not creation times
    assertThat(testModel.getClassId()).isEqualTo(123456789L);
    assertThat(different.getClassId()).isEqualTo(987654321L);
    assertThat(testModel).isNotEqualTo(different);
    assertThat(testModel).isNotEqualTo(null);
    assertThat(testModel).isNotEqualTo("not a credential model");

    // Test hashCode - different class IDs should have different hash codes
    assertThat(testModel.hashCode()).isNotEqualTo(different.hashCode());
  }

  @Test
  @DisplayName("Should have meaningful toString representation")
  void shouldHaveMeaningfulToStringRepresentation() {
    String toString = testModel.toString();

    assertThat(toString).contains("FaceCredentialModel");
    assertThat(toString).contains("classId=123456789");
    assertThat(toString).contains("imageCount=3");
    assertThat(toString).contains("encoderVersion=5");
    assertThat(toString).contains("templateType=STANDARD");
    assertThat(toString).contains("enrollmentAction='NEW_TEMPLATE_CREATED'");
  }

  @Test
  @DisplayName("Should test template type enum values")
  void shouldTestTemplateTypeEnumValues() {
    assertThat(FaceCredentialModel.TemplateType.values()).hasSize(3);
    assertThat(FaceCredentialModel.TemplateType.COMPACT).isNotNull();
    assertThat(FaceCredentialModel.TemplateType.STANDARD).isNotNull();
    assertThat(FaceCredentialModel.TemplateType.FULL).isNotNull();
  }

  @Test
  @DisplayName("Should test enrollment metadata properties")
  void shouldTestEnrollmentMetadataProperties() {
    assertThat(testMetadata.getUserAgent()).isEqualTo("Chrome/120.0");
    assertThat(testMetadata.getIpAddress()).isEqualTo("192.168.1.100");
    assertThat(testMetadata.getDeviceFingerprint()).isEqualTo("device-fingerprint-hash");
    assertThat(testMetadata.getBwsJobId()).isEqualTo("bws-job-id-123");
  }

  @Test
  @DisplayName("Should implement enrollment metadata equals and hashCode")
  void shouldImplementEnrollmentMetadataEqualsAndHashCode() {
    FaceCredentialModel.EnrollmentMetadata identical =
        new FaceCredentialModel.EnrollmentMetadata(
            "Chrome/120.0", "192.168.1.100", "device-fingerprint-hash", "bws-job-id-123");

    FaceCredentialModel.EnrollmentMetadata different =
        new FaceCredentialModel.EnrollmentMetadata(
            "Firefox/119.0", "192.168.1.100", "device-fingerprint-hash", "bws-job-id-123");

    assertThat(testMetadata).isEqualTo(identical);
    assertThat(testMetadata).isNotEqualTo(different);
    assertThat(testMetadata.hashCode()).isEqualTo(identical.hashCode());
  }

  @Test
  @DisplayName("Should have meaningful enrollment metadata toString")
  void shouldHaveMeaningfulEnrollmentMetadataToString() {
    String toString = testMetadata.toString();

    assertThat(toString).contains("EnrollmentMetadata");
    assertThat(toString).contains("userAgent='Chrome/120.0'");
    assertThat(toString).contains("ipAddress='192.168.1.100'");
    assertThat(toString).contains("bwsJobId='bws-job-id-123'");
  }
}
