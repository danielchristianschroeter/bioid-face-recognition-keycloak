package com.bioid.keycloak.client.error;

import static org.assertj.core.api.Assertions.*;

import com.bioid.keycloak.client.exception.*;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Unit tests for BioIdErrorMapper. Tests error mapping, classification, and retry logic. */
class BioIdErrorMapperTest {

  @Test
  @DisplayName("Should map UNAUTHENTICATED to BioIdAuthenticationException")
  void shouldMapUnauthenticatedToAuthenticationException() {
    // Given
    StatusRuntimeException grpcException =
        Status.UNAUTHENTICATED.withDescription("Invalid JWT token").asRuntimeException();

    // When
    BioIdException result = BioIdErrorMapper.mapGrpcException(grpcException, "enroll");

    // Then
    assertThat(result).isInstanceOf(BioIdAuthenticationException.class);
    assertThat(result.getErrorCode()).isEqualTo("UNAUTHENTICATED");
    assertThat(result.getMessage()).contains("enroll operation failed");
    assertThat(result.getMessage()).contains("Invalid JWT token");
    assertThat(result.isRetryable()).isFalse();
    assertThat(result.getHttpStatus()).isEqualTo(401);
  }

  @Test
  @DisplayName("Should map PERMISSION_DENIED to BioIdAuthenticationException")
  void shouldMapPermissionDeniedToAuthenticationException() {
    // Given
    StatusRuntimeException grpcException =
        Status.PERMISSION_DENIED.withDescription("Insufficient permissions").asRuntimeException();

    // When
    BioIdException result = BioIdErrorMapper.mapGrpcException(grpcException, "verify");

    // Then
    assertThat(result).isInstanceOf(BioIdAuthenticationException.class);
    assertThat(result.getErrorCode()).isEqualTo("PERMISSION_DENIED");
    assertThat(result.isRetryable()).isFalse();
    assertThat(result.getHttpStatus()).isEqualTo(401);
  }

  @Test
  @DisplayName("Should map INVALID_ARGUMENT to BioIdValidationException")
  void shouldMapInvalidArgumentToValidationException() {
    // Given
    StatusRuntimeException grpcException =
        Status.INVALID_ARGUMENT.withDescription("Invalid image format").asRuntimeException();

    // When
    BioIdException result = BioIdErrorMapper.mapGrpcException(grpcException, "enroll");

    // Then
    assertThat(result).isInstanceOf(BioIdValidationException.class);
    assertThat(result.getErrorCode()).isEqualTo("INVALID_ARGUMENT");
    assertThat(result.isRetryable()).isFalse();
    assertThat(result.getHttpStatus()).isEqualTo(400);
  }

  @Test
  @DisplayName("Should map NOT_FOUND to BioIdValidationException with template not found")
  void shouldMapNotFoundToValidationException() {
    // Given
    StatusRuntimeException grpcException =
        Status.NOT_FOUND.withDescription("Template not found").asRuntimeException();

    // When
    BioIdException result = BioIdErrorMapper.mapGrpcException(grpcException, "verify");

    // Then
    assertThat(result).isInstanceOf(BioIdValidationException.class);
    assertThat(result.getErrorCode()).isEqualTo("TEMPLATE_NOT_FOUND");
    assertThat(result.isRetryable()).isFalse();
    assertThat(result.getHttpStatus()).isEqualTo(400);
  }

  @Test
  @DisplayName("Should map UNAVAILABLE to BioIdServiceException")
  void shouldMapUnavailableToServiceException() {
    // Given
    StatusRuntimeException grpcException =
        Status.UNAVAILABLE.withDescription("Service temporarily unavailable").asRuntimeException();

    // When
    BioIdException result = BioIdErrorMapper.mapGrpcException(grpcException, "enroll");

    // Then
    assertThat(result).isInstanceOf(BioIdServiceException.class);
    assertThat(result.getErrorCode()).isEqualTo("SERVICE_UNAVAILABLE");
    assertThat(result.isRetryable()).isTrue();
    assertThat(result.getHttpStatus()).isEqualTo(503);
  }

  @Test
  @DisplayName("Should map DEADLINE_EXCEEDED to BioIdServiceException")
  void shouldMapDeadlineExceededToServiceException() {
    // Given
    StatusRuntimeException grpcException =
        Status.DEADLINE_EXCEEDED.withDescription("Request timeout").asRuntimeException();

    // When
    BioIdException result = BioIdErrorMapper.mapGrpcException(grpcException, "verify");

    // Then
    assertThat(result).isInstanceOf(BioIdServiceException.class);
    assertThat(result.getErrorCode()).isEqualTo("REQUEST_TIMEOUT");
    assertThat(result.isRetryable()).isTrue();
    assertThat(result.getHttpStatus()).isEqualTo(503);
  }

  @Test
  @DisplayName("Should map RESOURCE_EXHAUSTED to BioIdServiceException")
  void shouldMapResourceExhaustedToServiceException() {
    // Given
    StatusRuntimeException grpcException =
        Status.RESOURCE_EXHAUSTED.withDescription("Rate limit exceeded").asRuntimeException();

    // When
    BioIdException result = BioIdErrorMapper.mapGrpcException(grpcException, "enroll");

    // Then
    assertThat(result).isInstanceOf(BioIdServiceException.class);
    assertThat(result.getErrorCode()).isEqualTo("RATE_LIMIT_EXCEEDED");
    assertThat(result.isRetryable()).isTrue();
    assertThat(result.getHttpStatus()).isEqualTo(503);
  }

  @Test
  @DisplayName("Should extract BWS error codes from descriptions")
  void shouldExtractBwsErrorCodes() {
    // Given
    StatusRuntimeException templateNotFound =
        Status.INVALID_ARGUMENT
            .withDescription("Template not found for class ID 123")
            .asRuntimeException();

    StatusRuntimeException noSuitableFace =
        Status.INVALID_ARGUMENT
            .withDescription("No suitable face image detected")
            .asRuntimeException();

    StatusRuntimeException multipleFaces =
        Status.INVALID_ARGUMENT
            .withDescription("Multiple faces found in image")
            .asRuntimeException();

    // When
    BioIdException result1 = BioIdErrorMapper.mapGrpcException(templateNotFound, "verify");
    BioIdException result2 = BioIdErrorMapper.mapGrpcException(noSuitableFace, "enroll");
    BioIdException result3 = BioIdErrorMapper.mapGrpcException(multipleFaces, "enroll");

    // Then
    assertThat(result1.getErrorCode()).isEqualTo("TemplateNotFound");
    assertThat(result2.getErrorCode()).isEqualTo("NoSuitableFaceImage");
    assertThat(result3.getErrorCode()).isEqualTo("MultipleFacesFound");

    assertThat(result1.isRetryable()).isFalse();
    assertThat(result2.isRetryable()).isFalse();
    assertThat(result3.isRetryable()).isFalse();
  }

  @Test
  @DisplayName("Should handle INTERNAL errors with BWS error code classification")
  void shouldHandleInternalErrorsWithBwsClassification() {
    // Given - Retryable internal error
    StatusRuntimeException retryableInternal =
        Status.INTERNAL
            .withDescription("Service unavailable - temporary maintenance")
            .asRuntimeException();

    // Given - Non-retryable internal error
    StatusRuntimeException nonRetryableInternal =
        Status.INTERNAL
            .withDescription("No feature vectors could be extracted")
            .asRuntimeException();

    // When
    BioIdException retryableResult = BioIdErrorMapper.mapGrpcException(retryableInternal, "enroll");
    BioIdException nonRetryableResult =
        BioIdErrorMapper.mapGrpcException(nonRetryableInternal, "enroll");

    // Then
    assertThat(retryableResult).isInstanceOf(BioIdServiceException.class);
    assertThat(retryableResult.isRetryable()).isTrue();

    assertThat(nonRetryableResult).isInstanceOf(BioIdValidationException.class);
    assertThat(nonRetryableResult.getErrorCode()).isEqualTo("NoFeatureVectors");
    assertThat(nonRetryableResult.isRetryable()).isFalse();
  }

  @ParameterizedTest
  @EnumSource(
      value = Status.Code.class,
      names = {"UNAVAILABLE", "DEADLINE_EXCEEDED", "RESOURCE_EXHAUSTED", "INTERNAL", "ABORTED"})
  @DisplayName("Should identify retryable gRPC status codes")
  void shouldIdentifyRetryableGrpcStatusCodes(Status.Code code) {
    // Given
    StatusRuntimeException exception =
        code.toStatus().withDescription("Test error").asRuntimeException();

    // When
    boolean isRetryable = BioIdErrorMapper.isRetryable(exception);

    // Then
    assertThat(isRetryable).isTrue();
  }

  @ParameterizedTest
  @EnumSource(
      value = Status.Code.class,
      names = {"UNAUTHENTICATED", "PERMISSION_DENIED", "INVALID_ARGUMENT", "NOT_FOUND"})
  @DisplayName("Should identify non-retryable gRPC status codes")
  void shouldIdentifyNonRetryableGrpcStatusCodes(Status.Code code) {
    // Given
    StatusRuntimeException exception =
        code.toStatus().withDescription("Test error").asRuntimeException();

    // When
    boolean isRetryable = BioIdErrorMapper.isRetryable(exception);

    // Then
    assertThat(isRetryable).isFalse();
  }

  @Test
  @DisplayName("Should map general exceptions to BioIdException")
  void shouldMapGeneralExceptionsToBioIdException() {
    // Given
    RuntimeException generalException = new RuntimeException("Unexpected error");

    // When
    BioIdException result = BioIdErrorMapper.mapGeneralException(generalException, "enroll");

    // Then
    assertThat(result).isInstanceOf(BioIdException.class);
    assertThat(result.getErrorCode()).isEqualTo("UNEXPECTED_ERROR");
    assertThat(result.getMessage()).contains("enroll operation failed");
    assertThat(result.getMessage()).contains("Unexpected error");
    assertThat(result.isRetryable()).isFalse();
    assertThat(result.getHttpStatus()).isEqualTo(500);
    assertThat(result.getCause()).isEqualTo(generalException);
  }

  @Test
  @DisplayName("Should return existing BioIdException unchanged")
  void shouldReturnExistingBioIdExceptionUnchanged() {
    // Given
    BioIdValidationException originalException =
        new BioIdValidationException("Original error", "TEST_ERROR");

    // When
    BioIdException result = BioIdErrorMapper.mapGeneralException(originalException, "verify");

    // Then
    assertThat(result).isSameAs(originalException);
  }

  @Test
  @DisplayName("Should classify errors correctly")
  void shouldClassifyErrorsCorrectly() {
    // Given
    BioIdAuthenticationException authException = new BioIdAuthenticationException("Auth failed");
    BioIdValidationException validationException =
        new BioIdValidationException("Validation failed", "INVALID_INPUT");
    BioIdServiceException serviceException =
        new BioIdServiceException("Service failed", "SERVICE_ERROR");
    StatusRuntimeException grpcException = Status.UNAVAILABLE.asRuntimeException();
    RuntimeException generalException = new RuntimeException("General error");

    // When
    BioIdErrorMapper.ErrorClassification authClassification =
        BioIdErrorMapper.classifyError(authException);
    BioIdErrorMapper.ErrorClassification validationClassification =
        BioIdErrorMapper.classifyError(validationException);
    BioIdErrorMapper.ErrorClassification serviceClassification =
        BioIdErrorMapper.classifyError(serviceException);
    BioIdErrorMapper.ErrorClassification grpcClassification =
        BioIdErrorMapper.classifyError(grpcException);
    BioIdErrorMapper.ErrorClassification generalClassification =
        BioIdErrorMapper.classifyError(generalException);

    // Then
    assertThat(authClassification.category())
        .isEqualTo(BioIdErrorMapper.ErrorCategory.AUTHENTICATION);
    assertThat(validationClassification.category())
        .isEqualTo(BioIdErrorMapper.ErrorCategory.VALIDATION);
    assertThat(serviceClassification.category()).isEqualTo(BioIdErrorMapper.ErrorCategory.SERVICE);
    assertThat(grpcClassification.category()).isEqualTo(BioIdErrorMapper.ErrorCategory.SERVICE);
    assertThat(generalClassification.category()).isEqualTo(BioIdErrorMapper.ErrorCategory.SYSTEM);

    assertThat(authClassification.retryable()).isFalse();
    assertThat(validationClassification.retryable()).isFalse();
    assertThat(serviceClassification.retryable()).isTrue();
    assertThat(grpcClassification.retryable()).isTrue();
    assertThat(generalClassification.retryable()).isFalse();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "ServiceUnavailable",
        "RequestTimeout",
        "ConnectionFailed",
        "RateLimitExceeded",
        "InternalError"
      })
  @DisplayName("Should identify retryable BWS error codes")
  void shouldIdentifyRetryableBwsErrorCodes(String errorCode) {
    // Given
    StatusRuntimeException exception =
        Status.INTERNAL.withDescription("BWS error: " + errorCode).asRuntimeException();

    // When
    boolean isRetryable = BioIdErrorMapper.isRetryable(exception);

    // Then
    assertThat(isRetryable).isTrue();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "TemplateNotFound", "TemplateCorrupted", "NoSuitableFaceImage",
        "MultipleFacesFound", "NoFeatureVectors", "PoorImageQuality"
      })
  @DisplayName("Should identify non-retryable BWS error codes")
  void shouldIdentifyNonRetryableBwsErrorCodes(String errorCode) {
    // Given
    StatusRuntimeException exception =
        Status.INTERNAL.withDescription("BWS error: " + errorCode).asRuntimeException();

    // When
    boolean isRetryable = BioIdErrorMapper.isRetryable(exception);

    // Then
    assertThat(isRetryable).isFalse();
  }

  @Test
  @DisplayName("Should handle null and empty error descriptions")
  void shouldHandleNullAndEmptyErrorDescriptions() {
    // Given
    StatusRuntimeException nullDescription = Status.INTERNAL.asRuntimeException();
    StatusRuntimeException emptyDescription =
        Status.INTERNAL.withDescription("").asRuntimeException();

    // When
    BioIdException nullResult = BioIdErrorMapper.mapGrpcException(nullDescription, "test");
    BioIdException emptyResult = BioIdErrorMapper.mapGrpcException(emptyDescription, "test");

    // Then
    assertThat(nullResult.getMessage()).contains("test operation failed");
    assertThat(emptyResult.getMessage()).contains("test operation failed");

    // Should default to retryable for INTERNAL without specific BWS error
    assertThat(nullResult.isRetryable()).isTrue();
    assertThat(emptyResult.isRetryable()).isTrue();
  }

  @Test
  @DisplayName("Should map HTTP status codes correctly")
  void shouldMapHttpStatusCodesCorrectly() {
    // Test various gRPC codes and their HTTP equivalents
    assertThat(
            BioIdErrorMapper.mapGrpcException(Status.UNAUTHENTICATED.asRuntimeException(), "test")
                .getHttpStatus())
        .isEqualTo(401);
    assertThat(
            BioIdErrorMapper.mapGrpcException(Status.PERMISSION_DENIED.asRuntimeException(), "test")
                .getHttpStatus())
        .isEqualTo(401);
    assertThat(
            BioIdErrorMapper.mapGrpcException(Status.INVALID_ARGUMENT.asRuntimeException(), "test")
                .getHttpStatus())
        .isEqualTo(400);
    assertThat(
            BioIdErrorMapper.mapGrpcException(Status.NOT_FOUND.asRuntimeException(), "test")
                .getHttpStatus())
        .isEqualTo(400);
    assertThat(
            BioIdErrorMapper.mapGrpcException(Status.UNAVAILABLE.asRuntimeException(), "test")
                .getHttpStatus())
        .isEqualTo(503);
    assertThat(
            BioIdErrorMapper.mapGrpcException(Status.DEADLINE_EXCEEDED.asRuntimeException(), "test")
                .getHttpStatus())
        .isEqualTo(503);
    assertThat(
            BioIdErrorMapper.mapGrpcException(
                    Status.RESOURCE_EXHAUSTED.asRuntimeException(), "test")
                .getHttpStatus())
        .isEqualTo(503);
    assertThat(
            BioIdErrorMapper.mapGrpcException(Status.INTERNAL.asRuntimeException(), "test")
                .getHttpStatus())
        .isEqualTo(500);
  }
}
