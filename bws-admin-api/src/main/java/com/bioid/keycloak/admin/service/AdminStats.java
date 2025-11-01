package com.bioid.keycloak.admin.service;

import java.time.Instant;

/**
 * Statistics about face templates.
 */
public class AdminStats {
  private int totalTemplates;
  private int activeUsers;
  private int orphanedTemplates;
  private Instant lastEnrollment;
  private Integer bwsClassCount; // Count from BWS Management API
  private String bwsError; // Error message if BWS call failed

  public int getTotalTemplates() {
    return totalTemplates;
  }

  public void setTotalTemplates(int totalTemplates) {
    this.totalTemplates = totalTemplates;
  }

  public int getActiveUsers() {
    return activeUsers;
  }

  public void setActiveUsers(int activeUsers) {
    this.activeUsers = activeUsers;
  }

  public int getOrphanedTemplates() {
    return orphanedTemplates;
  }

  public void setOrphanedTemplates(int orphanedTemplates) {
    this.orphanedTemplates = orphanedTemplates;
  }

  public Instant getLastEnrollment() {
    return lastEnrollment;
  }

  public void setLastEnrollment(Instant lastEnrollment) {
    this.lastEnrollment = lastEnrollment;
  }

  public Integer getBwsClassCount() {
    return bwsClassCount;
  }

  public void setBwsClassCount(Integer bwsClassCount) {
    this.bwsClassCount = bwsClassCount;
  }

  public String getBwsError() {
    return bwsError;
  }

  public void setBwsError(String bwsError) {
    this.bwsError = bwsError;
  }
}
