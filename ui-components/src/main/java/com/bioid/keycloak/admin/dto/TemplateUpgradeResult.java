package com.bioid.keycloak.admin.dto;

/**
 * DTO for template upgrade operation result.
 */
public class TemplateUpgradeResult {
    private long classId;
    private boolean success;
    private String message;
    private String errorCode;

    public TemplateUpgradeResult() {}

    private TemplateUpgradeResult(Builder builder) {
        this.classId = builder.classId;
        this.success = builder.success;
        this.message = builder.message;
        this.errorCode = builder.errorCode;
    }

    public static Builder builder() {
        return new Builder();
    }

    public long getClassId() {
        return classId;
    }

    public void setClassId(long classId) {
        this.classId = classId;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public static class Builder {
        private long classId;
        private boolean success;
        private String message;
        private String errorCode;

        public Builder classId(long classId) {
            this.classId = classId;
            return this;
        }

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder errorCode(String errorCode) {
            this.errorCode = errorCode;
            return this;
        }

        public TemplateUpgradeResult build() {
            return new TemplateUpgradeResult(this);
        }
    }
}