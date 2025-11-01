package com.bioid.keycloak.admin.security;

import org.keycloak.models.KeycloakSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * Security configuration for BWS Admin API.
 * 
 * Provides centralized security settings including:
 * - Required role name
 * - Rate limiting configuration
 * - IP whitelist/blacklist (if needed)
 * - Audit logging settings
 * 
 * @author BioID Keycloak Extension
 * @version 1.0.0
 */
public class AdminSecurityConfig {

  private static final Logger logger = LoggerFactory.getLogger(AdminSecurityConfig.class);
  
  // Default security settings
  private static final String DEFAULT_ADMIN_ROLE = "bws-admin";
  private static final int DEFAULT_RATE_LIMIT_PER_MINUTE = 60;
  private static final boolean DEFAULT_AUDIT_ENABLED = true;
  
  private final KeycloakSession session;
  private final String adminRoleName;
  private final int rateLimitPerMinute;
  private final boolean auditEnabled;
  private final Set<String> ipWhitelist;
  private final Set<String> ipBlacklist;

  public AdminSecurityConfig(KeycloakSession session) {
    this.session = session;
    
    // Load configuration from realm attributes or use defaults
    this.adminRoleName = getConfigValue("bws.admin.role", DEFAULT_ADMIN_ROLE);
    this.rateLimitPerMinute = getConfigIntValue("bws.admin.rateLimit", DEFAULT_RATE_LIMIT_PER_MINUTE);
    this.auditEnabled = getConfigBoolValue("bws.admin.auditEnabled", DEFAULT_AUDIT_ENABLED);
    this.ipWhitelist = loadIpList("bws.admin.ipWhitelist");
    this.ipBlacklist = loadIpList("bws.admin.ipBlacklist");
    
    logger.info("BWS Admin Security Config initialized: role={}, rateLimit={}/min, audit={}", 
        adminRoleName, rateLimitPerMinute, auditEnabled);
  }

  /**
   * Gets the required admin role name.
   * 
   * @return admin role name (default: "bws-admin")
   */
  public String getAdminRoleName() {
    return adminRoleName;
  }

  /**
   * Gets the rate limit per minute.
   * 
   * @return requests per minute allowed (default: 60)
   */
  public int getRateLimitPerMinute() {
    return rateLimitPerMinute;
  }

  /**
   * Checks if audit logging is enabled.
   * 
   * @return true if audit logging is enabled (default: true)
   */
  public boolean isAuditEnabled() {
    return auditEnabled;
  }

  /**
   * Checks if an IP address is allowed.
   * 
   * @param ipAddress the IP address to check
   * @return true if allowed, false if blocked
   */
  public boolean isIpAllowed(String ipAddress) {
    // If blacklist contains the IP, deny
    if (ipBlacklist.contains(ipAddress)) {
      logger.warn("IP {} is blacklisted", ipAddress);
      return false;
    }
    
    // If whitelist is empty, allow all (except blacklisted)
    if (ipWhitelist.isEmpty()) {
      return true;
    }
    
    // If whitelist is configured, only allow whitelisted IPs
    boolean allowed = ipWhitelist.contains(ipAddress);
    if (!allowed) {
      logger.warn("IP {} is not whitelisted", ipAddress);
    }
    return allowed;
  }

  /**
   * Gets a configuration value from realm attributes.
   * 
   * @param key the configuration key
   * @param defaultValue the default value if not found
   * @return the configuration value
   */
  private String getConfigValue(String key, String defaultValue) {
    try {
      String value = session.getContext().getRealm().getAttribute(key);
      return (value != null && !value.isEmpty()) ? value : defaultValue;
    } catch (Exception e) {
      logger.debug("Could not read config value for {}, using default", key, e);
      return defaultValue;
    }
  }

  /**
   * Gets an integer configuration value.
   * 
   * @param key the configuration key
   * @param defaultValue the default value if not found
   * @return the configuration value
   */
  private int getConfigIntValue(String key, int defaultValue) {
    try {
      String value = getConfigValue(key, String.valueOf(defaultValue));
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      logger.warn("Invalid integer value for {}, using default {}", key, defaultValue);
      return defaultValue;
    }
  }

  /**
   * Gets a boolean configuration value.
   * 
   * @param key the configuration key
   * @param defaultValue the default value if not found
   * @return the configuration value
   */
  private boolean getConfigBoolValue(String key, boolean defaultValue) {
    try {
      String value = getConfigValue(key, String.valueOf(defaultValue));
      return Boolean.parseBoolean(value);
    } catch (Exception e) {
      logger.warn("Invalid boolean value for {}, using default {}", key, defaultValue);
      return defaultValue;
    }
  }

  /**
   * Loads a comma-separated list of IP addresses.
   * 
   * @param key the configuration key
   * @return set of IP addresses
   */
  private Set<String> loadIpList(String key) {
    Set<String> ips = new HashSet<>();
    try {
      String value = getConfigValue(key, "");
      if (!value.isEmpty()) {
        String[] ipArray = value.split(",");
        for (String ip : ipArray) {
          String trimmed = ip.trim();
          if (!trimmed.isEmpty()) {
            ips.add(trimmed);
          }
        }
        logger.info("Loaded {} IP addresses for {}", ips.size(), key);
      }
    } catch (Exception e) {
      logger.warn("Could not load IP list for {}", key, e);
    }
    return ips;
  }
}
