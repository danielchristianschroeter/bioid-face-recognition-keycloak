package com.bioid.keycloak.admin.dto;

import java.util.List;

/**
 * DTO for detailed template information including thumbnails.
 */
public class TemplateDetails {
    private long classId;
    private List<String> thumbnails; // Base64 encoded images

    public TemplateDetails() {}

    private TemplateDetails(Builder builder) {
        this.classId = builder.classId;
        this.thumbnails = builder.thumbnails;
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

    public List<String> getThumbnails() {
        return thumbnails;
    }

    public void setThumbnails(List<String> thumbnails) {
        this.thumbnails = thumbnails;
    }

    public static class Builder {
        private long classId;
        private List<String> thumbnails;

        public Builder classId(long classId) {
            this.classId = classId;
            return this;
        }

        public Builder thumbnails(List<String> thumbnails) {
            this.thumbnails = thumbnails;
            return this;
        }

        public TemplateDetails build() {
            return new TemplateDetails(this);
        }
    }
}