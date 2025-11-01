package com.bioid.keycloak.admin.service;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of template validation.
 */
public class ValidationResult {
  private boolean isValid;
  private List<String> orphanedTemplates = new ArrayList<>();
  private List<String> missingTemplates = new ArrayList<>();
  private String message;

  public boolean isValid() {
    return isValid;
  }

  public void setValid(boolean valid) {
    isValid = valid;
  }

  public List<String> getOrphanedTemplates() {
    return orphanedTemplates;
  }

  public void setOrphanedTemplates(List<String> orphanedTemplates) {
    this.orphanedTemplates = orphanedTemplates;
  }

  public List<String> getMissingTemplates() {
    return missingTemplates;
  }

  public void setMissingTemplates(List<String> missingTemplates) {
    this.missingTemplates = missingTemplates;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }
}
