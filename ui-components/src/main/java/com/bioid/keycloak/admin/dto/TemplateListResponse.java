package com.bioid.keycloak.admin.dto;

import java.util.List;

/**
 * Response DTO for template listing with pagination.
 */
public class TemplateListResponse {
    private List<TemplateDto> templates;
    private int totalCount;
    private int offset;
    private int limit;

    public TemplateListResponse() {}

    private TemplateListResponse(Builder builder) {
        this.templates = builder.templates;
        this.totalCount = builder.totalCount;
        this.offset = builder.offset;
        this.limit = builder.limit;
    }

    public static Builder builder() {
        return new Builder();
    }

    public List<TemplateDto> getTemplates() {
        return templates;
    }

    public void setTemplates(List<TemplateDto> templates) {
        this.templates = templates;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public static class Builder {
        private List<TemplateDto> templates;
        private int totalCount;
        private int offset;
        private int limit;

        public Builder templates(List<TemplateDto> templates) {
            this.templates = templates;
            return this;
        }

        public Builder totalCount(int totalCount) {
            this.totalCount = totalCount;
            return this;
        }

        public Builder offset(int offset) {
            this.offset = offset;
            return this;
        }

        public Builder limit(int limit) {
            this.limit = limit;
            return this;
        }

        public TemplateListResponse build() {
            return new TemplateListResponse(this);
        }
    }
}