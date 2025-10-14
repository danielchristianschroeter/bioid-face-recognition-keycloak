package com.bioid.keycloak.admin.dto;

import java.util.List;

/**
 * DTO for bulk operation requests.
 */
public class BulkOperationRequest {
    private List<Long> classIds;
    private String reason;

    public BulkOperationRequest() {}

    public List<Long> getClassIds() {
        return classIds;
    }

    public void setClassIds(List<Long> classIds) {
        this.classIds = classIds;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}