package com.bioid.keycloak.admin.dto;

/**
 * Data Transfer Object for Face Recognition configuration. Used by the admin REST API to transfer
 * configuration data.
 */
public class FaceRecognitionConfigDto {

  private String endpoint;
  private String clientId;
  private double verificationThreshold;
  private int maxRetries;
  private boolean livenessEnabled;
  private boolean passiveLivenessEnabled;
  private boolean activeLivenessEnabled;
  private boolean challengeResponseLivenessEnabled;
  private double livenessConfidenceThreshold;
  private int livenessMaxOverheadMs;
  private String preferredRegion;
  private boolean dataResidencyRequired;
  private boolean failoverEnabled;
  private int channelPoolSize;
  private int keepAliveTime;
  private int verificationTimeout;
  private int enrollmentTimeout;
  private int templateTtlDays;

  // Default constructor for JSON deserialization
  public FaceRecognitionConfigDto() {}

  private FaceRecognitionConfigDto(Builder builder) {
    this.endpoint = builder.endpoint;
    this.clientId = builder.clientId;
    this.verificationThreshold = builder.verificationThreshold;
    this.maxRetries = builder.maxRetries;
    this.livenessEnabled = builder.livenessEnabled;
    this.passiveLivenessEnabled = builder.passiveLivenessEnabled;
    this.activeLivenessEnabled = builder.activeLivenessEnabled;
    this.challengeResponseLivenessEnabled = builder.challengeResponseLivenessEnabled;
    this.livenessConfidenceThreshold = builder.livenessConfidenceThreshold;
    this.livenessMaxOverheadMs = builder.livenessMaxOverheadMs;
    this.preferredRegion = builder.preferredRegion;
    this.dataResidencyRequired = builder.dataResidencyRequired;
    this.failoverEnabled = builder.failoverEnabled;
    this.channelPoolSize = builder.channelPoolSize;
    this.keepAliveTime = builder.keepAliveTime;
    this.verificationTimeout = builder.verificationTimeout;
    this.enrollmentTimeout = builder.enrollmentTimeout;
    this.templateTtlDays = builder.templateTtlDays;
  }

  public static Builder builder() {
    return new Builder();
  }

  // Getters and setters
  public String getEndpoint() {
    return endpoint;
  }

  public void setEndpoint(String endpoint) {
    this.endpoint = endpoint;
  }

  public String getClientId() {
    return clientId;
  }

  public void setClientId(String clientId) {
    this.clientId = clientId;
  }

  public double getVerificationThreshold() {
    return verificationThreshold;
  }

  public void setVerificationThreshold(double verificationThreshold) {
    this.verificationThreshold = verificationThreshold;
  }

  public int getMaxRetries() {
    return maxRetries;
  }

  public void setMaxRetries(int maxRetries) {
    this.maxRetries = maxRetries;
  }

  public boolean isLivenessEnabled() {
    return livenessEnabled;
  }

  public void setLivenessEnabled(boolean livenessEnabled) {
    this.livenessEnabled = livenessEnabled;
  }

  public boolean isPassiveLivenessEnabled() {
    return passiveLivenessEnabled;
  }

  public void setPassiveLivenessEnabled(boolean passiveLivenessEnabled) {
    this.passiveLivenessEnabled = passiveLivenessEnabled;
  }

  public boolean isActiveLivenessEnabled() {
    return activeLivenessEnabled;
  }

  public void setActiveLivenessEnabled(boolean activeLivenessEnabled) {
    this.activeLivenessEnabled = activeLivenessEnabled;
  }

  public boolean isChallengeResponseLivenessEnabled() {
    return challengeResponseLivenessEnabled;
  }

  public void setChallengeResponseLivenessEnabled(boolean challengeResponseLivenessEnabled) {
    this.challengeResponseLivenessEnabled = challengeResponseLivenessEnabled;
  }

  public double getLivenessConfidenceThreshold() {
    return livenessConfidenceThreshold;
  }

  public void setLivenessConfidenceThreshold(double livenessConfidenceThreshold) {
    this.livenessConfidenceThreshold = livenessConfidenceThreshold;
  }

  public int getLivenessMaxOverheadMs() {
    return livenessMaxOverheadMs;
  }

  public void setLivenessMaxOverheadMs(int livenessMaxOverheadMs) {
    this.livenessMaxOverheadMs = livenessMaxOverheadMs;
  }

  public String getPreferredRegion() {
    return preferredRegion;
  }

  public void setPreferredRegion(String preferredRegion) {
    this.preferredRegion = preferredRegion;
  }

  public boolean isDataResidencyRequired() {
    return dataResidencyRequired;
  }

  public void setDataResidencyRequired(boolean dataResidencyRequired) {
    this.dataResidencyRequired = dataResidencyRequired;
  }

  public boolean isFailoverEnabled() {
    return failoverEnabled;
  }

  public void setFailoverEnabled(boolean failoverEnabled) {
    this.failoverEnabled = failoverEnabled;
  }

  public int getChannelPoolSize() {
    return channelPoolSize;
  }

  public void setChannelPoolSize(int channelPoolSize) {
    this.channelPoolSize = channelPoolSize;
  }

  public int getKeepAliveTime() {
    return keepAliveTime;
  }

  public void setKeepAliveTime(int keepAliveTime) {
    this.keepAliveTime = keepAliveTime;
  }

  public int getVerificationTimeout() {
    return verificationTimeout;
  }

  public void setVerificationTimeout(int verificationTimeout) {
    this.verificationTimeout = verificationTimeout;
  }

  public int getEnrollmentTimeout() {
    return enrollmentTimeout;
  }

  public void setEnrollmentTimeout(int enrollmentTimeout) {
    this.enrollmentTimeout = enrollmentTimeout;
  }

  public int getTemplateTtlDays() {
    return templateTtlDays;
  }

  public void setTemplateTtlDays(int templateTtlDays) {
    this.templateTtlDays = templateTtlDays;
  }

  public static class Builder {
    private String endpoint;
    private String clientId;
    private double verificationThreshold;
    private int maxRetries;
    private boolean livenessEnabled;
    private boolean passiveLivenessEnabled;
    private boolean activeLivenessEnabled;
    private boolean challengeResponseLivenessEnabled;
    private double livenessConfidenceThreshold;
    private int livenessMaxOverheadMs;
    private String preferredRegion;
    private boolean dataResidencyRequired;
    private boolean failoverEnabled;
    private int channelPoolSize;
    private int keepAliveTime;
    private int verificationTimeout;
    private int enrollmentTimeout;
    private int templateTtlDays;

    public Builder endpoint(String endpoint) {
      this.endpoint = endpoint;
      return this;
    }

    public Builder clientId(String clientId) {
      this.clientId = clientId;
      return this;
    }

    public Builder verificationThreshold(double verificationThreshold) {
      this.verificationThreshold = verificationThreshold;
      return this;
    }

    public Builder maxRetries(int maxRetries) {
      this.maxRetries = maxRetries;
      return this;
    }

    public Builder livenessEnabled(boolean livenessEnabled) {
      this.livenessEnabled = livenessEnabled;
      return this;
    }

    public Builder passiveLivenessEnabled(boolean passiveLivenessEnabled) {
      this.passiveLivenessEnabled = passiveLivenessEnabled;
      return this;
    }

    public Builder activeLivenessEnabled(boolean activeLivenessEnabled) {
      this.activeLivenessEnabled = activeLivenessEnabled;
      return this;
    }

    public Builder challengeResponseLivenessEnabled(boolean challengeResponseLivenessEnabled) {
      this.challengeResponseLivenessEnabled = challengeResponseLivenessEnabled;
      return this;
    }

    public Builder livenessConfidenceThreshold(double livenessConfidenceThreshold) {
      this.livenessConfidenceThreshold = livenessConfidenceThreshold;
      return this;
    }

    public Builder livenessMaxOverheadMs(int livenessMaxOverheadMs) {
      this.livenessMaxOverheadMs = livenessMaxOverheadMs;
      return this;
    }

    public Builder preferredRegion(String preferredRegion) {
      this.preferredRegion = preferredRegion;
      return this;
    }

    public Builder dataResidencyRequired(boolean dataResidencyRequired) {
      this.dataResidencyRequired = dataResidencyRequired;
      return this;
    }

    public Builder failoverEnabled(boolean failoverEnabled) {
      this.failoverEnabled = failoverEnabled;
      return this;
    }

    public Builder channelPoolSize(int channelPoolSize) {
      this.channelPoolSize = channelPoolSize;
      return this;
    }

    public Builder keepAliveTime(int keepAliveTime) {
      this.keepAliveTime = keepAliveTime;
      return this;
    }

    public Builder verificationTimeout(int verificationTimeout) {
      this.verificationTimeout = verificationTimeout;
      return this;
    }

    public Builder enrollmentTimeout(int enrollmentTimeout) {
      this.enrollmentTimeout = enrollmentTimeout;
      return this;
    }

    public Builder templateTtlDays(int templateTtlDays) {
      this.templateTtlDays = templateTtlDays;
      return this;
    }

    public FaceRecognitionConfigDto build() {
      return new FaceRecognitionConfigDto(this);
    }
  }
}
