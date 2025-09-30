package com.bioid.keycloak.client.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

/**
 * Metrics configuration for BioID client operations. Provides counters and timers for monitoring
 * BioID operations.
 */
public class BioIdMetrics {
  private final MeterRegistry registry;

  // Operation counters
  private final Counter enrollmentAttempts;
  private final Counter enrollmentSuccesses;
  private final Counter enrollmentFailures;

  private final Counter verificationAttempts;
  private final Counter verificationSuccesses;
  private final Counter verificationFailures;

  private final Counter templateDeletions;
  private final Counter templateStatusRequests;
  private final Counter templateTagUpdates;

  // Error counters
  private final Counter validationErrors;
  private final Counter authenticationErrors;
  private final Counter serviceErrors;
  private final Counter networkErrors;
  private final Counter templateErrors;

  // Timers
  private final Timer enrollmentTimer;
  private final Timer verificationTimer;
  private final Timer templateStatusTimer;
  private final Timer templateDeletionTimer;
  private final Timer templateTagsTimer;

  /**
   * Creates a new BioIdMetrics instance.
   *
   * @param registry the meter registry to use
   */
  public BioIdMetrics(MeterRegistry registry) {
    this.registry = registry;

    // Initialize operation counters
    this.enrollmentAttempts =
        Counter.builder("bioid.enrollment.attempts")
            .description("Number of enrollment attempts")
            .register(registry);

    this.enrollmentSuccesses =
        Counter.builder("bioid.enrollment.successes")
            .description("Number of successful enrollments")
            .register(registry);

    this.enrollmentFailures =
        Counter.builder("bioid.enrollment.failures")
            .description("Number of failed enrollments")
            .register(registry);

    this.verificationAttempts =
        Counter.builder("bioid.verification.attempts")
            .description("Number of verification attempts")
            .register(registry);

    this.verificationSuccesses =
        Counter.builder("bioid.verification.successes")
            .description("Number of successful verifications")
            .register(registry);

    this.verificationFailures =
        Counter.builder("bioid.verification.failures")
            .description("Number of failed verifications")
            .register(registry);

    this.templateDeletions =
        Counter.builder("bioid.template.deletions")
            .description("Number of template deletions")
            .register(registry);

    this.templateStatusRequests =
        Counter.builder("bioid.template.status.requests")
            .description("Number of template status requests")
            .register(registry);

    this.templateTagUpdates =
        Counter.builder("bioid.template.tag.updates")
            .description("Number of template tag updates")
            .register(registry);

    // Initialize error counters
    this.validationErrors =
        Counter.builder("bioid.errors.validation")
            .description("Number of validation errors")
            .register(registry);

    this.authenticationErrors =
        Counter.builder("bioid.errors.authentication")
            .description("Number of authentication errors")
            .register(registry);

    this.serviceErrors =
        Counter.builder("bioid.errors.service")
            .description("Number of service errors")
            .register(registry);

    this.networkErrors =
        Counter.builder("bioid.errors.network")
            .description("Number of network errors")
            .register(registry);

    this.templateErrors =
        Counter.builder("bioid.errors.template")
            .description("Number of template errors")
            .register(registry);

    // Initialize timers
    this.enrollmentTimer =
        Timer.builder("bioid.enrollment.duration")
            .description("Time taken for enrollment operations")
            .register(registry);

    this.verificationTimer =
        Timer.builder("bioid.verification.duration")
            .description("Time taken for verification operations")
            .register(registry);

    this.templateStatusTimer =
        Timer.builder("bioid.template.status.duration")
            .description("Time taken for template status operations")
            .register(registry);

    this.templateDeletionTimer =
        Timer.builder("bioid.template.deletion.duration")
            .description("Time taken for template deletion operations")
            .register(registry);

    this.templateTagsTimer =
        Timer.builder("bioid.template.tags.duration")
            .description("Time taken for template tag operations")
            .register(registry);
  }

  // Enrollment metrics
  public void incrementEnrollmentAttempts() {
    enrollmentAttempts.increment();
  }

  public void incrementEnrollmentSuccesses() {
    enrollmentSuccesses.increment();
  }

  public void incrementEnrollmentFailures() {
    enrollmentFailures.increment();
  }

  public Timer.Sample startEnrollmentTimer() {
    return Timer.start(registry);
  }

  public void stopEnrollmentTimer(Timer.Sample sample) {
    sample.stop(enrollmentTimer);
  }

  // Verification metrics
  public void incrementVerificationAttempts() {
    verificationAttempts.increment();
  }

  public void incrementVerificationSuccesses() {
    verificationSuccesses.increment();
  }

  public void incrementVerificationFailures() {
    verificationFailures.increment();
  }

  public Timer.Sample startVerificationTimer() {
    return Timer.start(registry);
  }

  public void stopVerificationTimer(Timer.Sample sample) {
    sample.stop(verificationTimer);
  }

  // Template metrics
  public void incrementTemplateDeletions() {
    templateDeletions.increment();
  }

  public void incrementTemplateStatusRequests() {
    templateStatusRequests.increment();
  }

  public void incrementTemplateTagUpdates() {
    templateTagUpdates.increment();
  }

  public Timer.Sample startTemplateStatusTimer() {
    return Timer.start(registry);
  }

  public void stopTemplateStatusTimer(Timer.Sample sample) {
    sample.stop(templateStatusTimer);
  }

  public Timer.Sample startTemplateDeletionTimer() {
    return Timer.start(registry);
  }

  public void stopTemplateDeletionTimer(Timer.Sample sample) {
    sample.stop(templateDeletionTimer);
  }

  public Timer.Sample startTemplateTagsTimer() {
    return Timer.start(registry);
  }

  public void stopTemplateTagsTimer(Timer.Sample sample) {
    sample.stop(templateTagsTimer);
  }

  // Error metrics
  public void incrementValidationErrors() {
    validationErrors.increment();
  }

  public void incrementAuthenticationErrors() {
    authenticationErrors.increment();
  }

  public void incrementServiceErrors() {
    serviceErrors.increment();
  }

  public void incrementNetworkErrors() {
    networkErrors.increment();
  }

  public void incrementTemplateErrors() {
    templateErrors.increment();
  }

  /**
   * Records an error based on its type.
   *
   * @param throwable the error to record
   */
  public void recordError(Throwable throwable) {
    if (throwable instanceof com.bioid.keycloak.client.exception.BioIdValidationException) {
      incrementValidationErrors();
    } else if (throwable
        instanceof com.bioid.keycloak.client.exception.BioIdAuthenticationException) {
      incrementAuthenticationErrors();
    } else if (throwable instanceof com.bioid.keycloak.client.exception.BioIdServiceException) {
      incrementServiceErrors();
    } else if (throwable instanceof com.bioid.keycloak.client.exception.BioIdNetworkException) {
      incrementNetworkErrors();
    } else if (throwable instanceof com.bioid.keycloak.client.exception.BioIdTemplateException) {
      incrementTemplateErrors();
    } else {
      // Unknown error type, increment service errors as a fallback
      incrementServiceErrors();
    }
  }
}
