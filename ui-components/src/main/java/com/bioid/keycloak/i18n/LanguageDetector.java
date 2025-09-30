package com.bioid.keycloak.i18n;

import java.util.*;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for detecting and managing user language preferences in the face recognition
 * extension.
 */
public class LanguageDetector {

  private static final Logger logger = LoggerFactory.getLogger(LanguageDetector.class);

  private static final Set<String> SUPPORTED_LANGUAGES = Set.of("en", "de", "fr", "es");
  private static final String DEFAULT_LANGUAGE = "en";

  private final KeycloakSession session;

  public LanguageDetector(KeycloakSession session) {
    this.session = session;
  }

  /**
   * Detect the best language for the current user context.
   *
   * @param realm The current realm
   * @param user The current user (may be null)
   * @param authSession The authentication session (may be null)
   * @param acceptLanguageHeader Accept-Language header value
   * @return The detected language code
   */
  public String detectLanguage(
      RealmModel realm,
      UserModel user,
      AuthenticationSessionModel authSession,
      String acceptLanguageHeader) {

    // 1. Check URL parameter (highest priority)
    String urlLang = getLanguageFromUrl();
    if (isSupported(urlLang)) {
      logger.debug("Language detected from URL: {}", urlLang);
      return urlLang;
    }

    // 2. Check authentication session
    if (authSession != null) {
      String sessionLang = authSession.getClientNote("kc_locale");
      if (isSupported(sessionLang)) {
        logger.debug("Language detected from auth session: {}", sessionLang);
        return sessionLang;
      }
    }

    // 3. Check user preferences
    if (user != null) {
      String userLang = user.getFirstAttribute("locale");
      if (isSupported(userLang)) {
        logger.debug("Language detected from user preferences: {}", userLang);
        return userLang;
      }
    }

    // 4. Check realm default
    if (realm != null) {
      String realmLang = realm.getDefaultLocale();
      if (isSupported(realmLang)) {
        logger.debug("Language detected from realm default: {}", realmLang);
        return realmLang;
      }
    }

    // 5. Check Accept-Language header
    if (acceptLanguageHeader != null) {
      String headerLang = detectFromAcceptLanguageHeader(acceptLanguageHeader);
      if (isSupported(headerLang)) {
        logger.debug("Language detected from Accept-Language header: {}", headerLang);
        return headerLang;
      }
    }

    // 6. Fall back to default
    logger.debug("Using default language: {}", DEFAULT_LANGUAGE);
    return DEFAULT_LANGUAGE;
  }

  /** Get language from URL parameters. */
  private String getLanguageFromUrl() {
    try {
      // This would typically be extracted from the request context
      // For now, we'll return null as this is handled by Keycloak's built-in mechanisms
      return null;
    } catch (Exception e) {
      logger.warn("Error extracting language from URL", e);
      return null;
    }
  }

  /** Detect language from Accept-Language header. */
  private String detectFromAcceptLanguageHeader(String acceptLanguageHeader) {
    try {
      if (acceptLanguageHeader == null || acceptLanguageHeader.trim().isEmpty()) {
        return null;
      }

      List<Locale.LanguageRange> ranges = Locale.LanguageRange.parse(acceptLanguageHeader);

      for (Locale.LanguageRange range : ranges) {
        String langCode = range.getRange().toLowerCase();

        // Handle language-country codes (e.g., "en-US" -> "en")
        if (langCode.contains("-")) {
          langCode = langCode.split("-")[0];
        }

        if (isSupported(langCode)) {
          return langCode;
        }
      }

    } catch (Exception e) {
      logger.warn("Error parsing Accept-Language header", e);
    }

    return null;
  }

  /** Check if a language code is supported. */
  private boolean isSupported(String languageCode) {
    return languageCode != null && SUPPORTED_LANGUAGES.contains(languageCode.toLowerCase());
  }

  /** Get all supported languages. */
  public static Set<String> getSupportedLanguages() {
    return Collections.unmodifiableSet(SUPPORTED_LANGUAGES);
  }

  /** Get the default language. */
  public static String getDefaultLanguage() {
    return DEFAULT_LANGUAGE;
  }

  /** Get display name for a language code. */
  public static String getLanguageDisplayName(String languageCode) {
    switch (languageCode.toLowerCase()) {
      case "en":
        return "English";
      case "de":
        return "Deutsch";
      case "fr":
        return "Français";
      case "es":
        return "Español";
      default:
        return languageCode;
    }
  }

  /** Check if a language is right-to-left. */
  public static boolean isRightToLeft(String languageCode) {
    // None of our currently supported languages are RTL
    // This method is here for future expansion
    Set<String> rtlLanguages = Set.of("ar", "he", "fa", "ur");
    return rtlLanguages.contains(languageCode.toLowerCase());
  }

  /** Get language-specific formatting preferences. */
  public static class LanguagePreferences {
    private final String languageCode;
    private final Locale locale;

    public LanguagePreferences(String languageCode) {
      this.languageCode = languageCode;
      this.locale = Locale.forLanguageTag(languageCode);
    }

    public String getLanguageCode() {
      return languageCode;
    }

    public Locale getLocale() {
      return locale;
    }

    public boolean isRightToLeft() {
      return LanguageDetector.isRightToLeft(languageCode);
    }

    public String getDisplayName() {
      return LanguageDetector.getLanguageDisplayName(languageCode);
    }

    /** Get date format pattern for this language. */
    public String getDateFormat() {
      switch (languageCode) {
        case "de":
          return "dd.MM.yyyy";
        case "fr":
          return "dd/MM/yyyy";
        case "es":
          return "dd/MM/yyyy";
        case "en":
        default:
          return "MM/dd/yyyy";
      }
    }

    /** Get time format pattern for this language. */
    public String getTimeFormat() {
      switch (languageCode) {
        case "en":
          return "h:mm a";
        case "de":
        case "fr":
        case "es":
        default:
          return "HH:mm";
      }
    }
  }

  /** Get language preferences for a language code. */
  public static LanguagePreferences getLanguagePreferences(String languageCode) {
    return new LanguagePreferences(
        isSupportedStatic(languageCode) ? languageCode : DEFAULT_LANGUAGE);
  }

  /** Check if a language code is supported (static version). */
  public static boolean isSupportedStatic(String languageCode) {
    return languageCode != null && SUPPORTED_LANGUAGES.contains(languageCode.toLowerCase());
  }
}
