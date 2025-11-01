package com.bioid.keycloak.admin.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AdminSecurityConfig.
 */
@ExtendWith(MockitoExtension.class)
class AdminSecurityConfigTest {

  @Mock
  private KeycloakSession session;

  @Mock
  private KeycloakContext context;

  @Mock
  private RealmModel realm;

  @BeforeEach
  void setUp() {
    when(session.getContext()).thenReturn(context);
    when(context.getRealm()).thenReturn(realm);
  }

  @Test
  void testDefaultConfiguration() {
    // Given: No realm attributes configured
    when(realm.getAttribute(anyString())).thenReturn(null);

    // When: Creating security config
    AdminSecurityConfig config = new AdminSecurityConfig(session);

    // Then: Should use default values
    assertEquals("bws-admin", config.getAdminRoleName());
    assertEquals(60, config.getRateLimitPerMinute());
    assertTrue(config.isAuditEnabled());
  }

  @Test
  void testCustomRoleName() {
    // Given: Custom role name configured
    when(realm.getAttribute("bws.admin.role")).thenReturn("face-admin");
    when(realm.getAttribute(argThat(arg -> !arg.equals("bws.admin.role")))).thenReturn(null);

    // When: Creating security config
    AdminSecurityConfig config = new AdminSecurityConfig(session);

    // Then: Should use custom role name
    assertEquals("face-admin", config.getAdminRoleName());
  }

  @Test
  void testCustomRateLimit() {
    // Given: Custom rate limit configured
    when(realm.getAttribute("bws.admin.rateLimit")).thenReturn("30");
    when(realm.getAttribute(argThat(arg -> !arg.equals("bws.admin.rateLimit")))).thenReturn(null);

    // When: Creating security config
    AdminSecurityConfig config = new AdminSecurityConfig(session);

    // Then: Should use custom rate limit
    assertEquals(30, config.getRateLimitPerMinute());
  }

  @Test
  void testInvalidRateLimitUsesDefault() {
    // Given: Invalid rate limit configured
    when(realm.getAttribute("bws.admin.rateLimit")).thenReturn("invalid");
    when(realm.getAttribute(argThat(arg -> !arg.equals("bws.admin.rateLimit")))).thenReturn(null);

    // When: Creating security config
    AdminSecurityConfig config = new AdminSecurityConfig(session);

    // Then: Should use default rate limit
    assertEquals(60, config.getRateLimitPerMinute());
  }

  @Test
  void testAuditDisabled() {
    // Given: Audit disabled
    when(realm.getAttribute("bws.admin.auditEnabled")).thenReturn("false");
    when(realm.getAttribute(argThat(arg -> !arg.equals("bws.admin.auditEnabled")))).thenReturn(null);

    // When: Creating security config
    AdminSecurityConfig config = new AdminSecurityConfig(session);

    // Then: Audit should be disabled
    assertFalse(config.isAuditEnabled());
  }

  @Test
  void testIpWhitelist() {
    // Given: IP whitelist configured
    when(realm.getAttribute("bws.admin.ipWhitelist")).thenReturn("192.168.1.100,10.0.0.1");
    when(realm.getAttribute(argThat(arg -> !arg.equals("bws.admin.ipWhitelist")))).thenReturn(null);

    // When: Creating security config
    AdminSecurityConfig config = new AdminSecurityConfig(session);

    // Then: Whitelisted IPs should be allowed
    assertTrue(config.isIpAllowed("192.168.1.100"));
    assertTrue(config.isIpAllowed("10.0.0.1"));
    assertFalse(config.isIpAllowed("192.168.1.101"));
  }

  @Test
  void testIpBlacklist() {
    // Given: IP blacklist configured
    when(realm.getAttribute("bws.admin.ipBlacklist")).thenReturn("203.0.113.0,198.51.100.0");
    when(realm.getAttribute(argThat(arg -> !arg.equals("bws.admin.ipBlacklist")))).thenReturn(null);

    // When: Creating security config
    AdminSecurityConfig config = new AdminSecurityConfig(session);

    // Then: Blacklisted IPs should be blocked
    assertFalse(config.isIpAllowed("203.0.113.0"));
    assertFalse(config.isIpAllowed("198.51.100.0"));
    assertTrue(config.isIpAllowed("192.168.1.100"));
  }

  @Test
  void testBlacklistTakesPrecedenceOverWhitelist() {
    // Given: IP in both whitelist and blacklist
    when(realm.getAttribute("bws.admin.ipWhitelist")).thenReturn("192.168.1.100");
    when(realm.getAttribute("bws.admin.ipBlacklist")).thenReturn("192.168.1.100");
    when(realm.getAttribute(argThat(arg -> 
        !arg.equals("bws.admin.ipWhitelist") && !arg.equals("bws.admin.ipBlacklist")))).thenReturn(null);

    // When: Creating security config
    AdminSecurityConfig config = new AdminSecurityConfig(session);

    // Then: Blacklist should take precedence
    assertFalse(config.isIpAllowed("192.168.1.100"));
  }

  @Test
  void testEmptyWhitelistAllowsAll() {
    // Given: No whitelist configured
    when(realm.getAttribute(anyString())).thenReturn(null);

    // When: Creating security config
    AdminSecurityConfig config = new AdminSecurityConfig(session);

    // Then: All IPs should be allowed (except blacklisted)
    assertTrue(config.isIpAllowed("192.168.1.100"));
    assertTrue(config.isIpAllowed("10.0.0.1"));
    assertTrue(config.isIpAllowed("203.0.113.0"));
  }

  @Test
  void testWhitelistWithSpaces() {
    // Given: Whitelist with spaces
    when(realm.getAttribute("bws.admin.ipWhitelist")).thenReturn(" 192.168.1.100 , 10.0.0.1 ");
    when(realm.getAttribute(argThat(arg -> !arg.equals("bws.admin.ipWhitelist")))).thenReturn(null);

    // When: Creating security config
    AdminSecurityConfig config = new AdminSecurityConfig(session);

    // Then: Should handle spaces correctly
    assertTrue(config.isIpAllowed("192.168.1.100"));
    assertTrue(config.isIpAllowed("10.0.0.1"));
  }
}
