package com.bioid.keycloak.admin.dto;

/** Data Transfer Object for BioID connectivity test results. */
public class ConnectivityTestResult {

  private boolean success;
  private String endpoint;
  private long responseTimeMs;
  private String message;

  // Default constructor for JSON deserialization
  public ConnectivityTestResult() {}

  private ConnectivityTestResult(Builder builder) {
    this.success = builder.success;
    this.endpoint = builder.endpoint;
    this.responseTimeMs = builder.responseTimeMs;
    this.message = builder.message;
  }

  public static Builder builder() {
    return new Builder();
  }

  // Getters and setters
  public boolean isSuccess() {
    return success;
  }

  public void setSuccess(boolean success) {
    this.success = success;
  }

  public String getEndpoint() {
    return endpoint;
  }

  public void setEndpoint(String endpoint) {
    this.endpoint = endpoint;
  }

  public long getResponseTimeMs() {
    return responseTimeMs;
  }

  public void setResponseTimeMs(long responseTimeMs) {
    this.responseTimeMs = responseTimeMs;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public static class Builder {
    private boolean success;
    private String endpoint;
    private long responseTimeMs;
    private String message;

    public Builder success(boolean success) {
      this.success = success;
      return this;
    }

    public Builder endpoint(String endpoint) {
      this.endpoint = endpoint;
      return this;
    }

    public Builder responseTimeMs(long responseTimeMs) {
      this.responseTimeMs = responseTimeMs;
      return this;
    }

    public Builder message(String message) {
      this.message = message;
      return this;
    }

    public ConnectivityTestResult build() {
      return new ConnectivityTestResult(this);
    }
  }
}
