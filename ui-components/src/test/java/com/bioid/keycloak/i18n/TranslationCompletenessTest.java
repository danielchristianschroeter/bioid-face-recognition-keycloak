package com.bioid.keycloak.i18n;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Test to validate translation completeness across all supported languages. */
public class TranslationCompletenessTest {

  private static final String[] SUPPORTED_LANGUAGES = {"en", "de", "fr", "es"};
  private static final String BASE_LANGUAGE = "en";

  private Map<String, Properties> languageProperties;

  @BeforeEach
  void setUp() throws IOException {
    languageProperties = new HashMap<>();

    // Load all language property files
    for (String lang : SUPPORTED_LANGUAGES) {
      Properties props = loadProperties("/theme/bioid/messages/messages_" + lang + ".properties");
      languageProperties.put(lang, props);
    }
  }

  @Test
  void shouldHaveAllLanguageFiles() {
    for (String lang : SUPPORTED_LANGUAGES) {
      assertTrue(languageProperties.containsKey(lang), "Missing language file for: " + lang);
      assertNotNull(languageProperties.get(lang), "Language properties is null for: " + lang);
    }
  }

  @Test
  void shouldHaveCompleteTranslations() {
    Properties baseProps = languageProperties.get(BASE_LANGUAGE);
    assertNotNull(baseProps, "Base language properties not found");

    Set<String> baseKeys = baseProps.stringPropertyNames();
    assertFalse(baseKeys.isEmpty(), "Base language has no keys");

    for (String lang : SUPPORTED_LANGUAGES) {
      if (lang.equals(BASE_LANGUAGE)) continue;

      Properties langProps = languageProperties.get(lang);
      Set<String> langKeys = langProps.stringPropertyNames();

      // Check that all base keys exist in the target language
      for (String key : baseKeys) {
        assertTrue(
            langKeys.contains(key),
            String.format("Missing translation key '%s' in language '%s'", key, lang));

        String value = langProps.getProperty(key);
        assertNotNull(value, String.format("Null value for key '%s' in language '%s'", key, lang));
        assertFalse(
            value.trim().isEmpty(),
            String.format("Empty value for key '%s' in language '%s'", key, lang));
      }

      // Check for extra keys that don't exist in base language
      for (String key : langKeys) {
        if (!key.startsWith("#") && !baseKeys.contains(key)) {
          System.out.println(String.format("Warning: Extra key '%s' in language '%s'", key, lang));
        }
      }
    }
  }

  @Test
  void shouldHaveConsistentKeyNaming() {
    Properties baseProps = languageProperties.get(BASE_LANGUAGE);
    Set<String> baseKeys = baseProps.stringPropertyNames();

    // Check key naming conventions - allow kebab-case, snake_case, and camelCase
    for (String key : baseKeys) {
      // Keys should follow common i18n patterns: kebab-case, snake_case, or camelCase
      assertTrue(
          key.matches("^[a-z][a-zA-Z0-9._-]*$"),
          "Key should follow valid naming pattern (kebab-case, snake_case, or camelCase): " + key);

      // Keys should not be too long
      assertTrue(key.length() <= 80, "Key too long (>80 chars): " + key);

      // Keys should not start or end with separators
      assertFalse(
          key.startsWith(".") || key.startsWith("-") || key.startsWith("_"),
          "Key should not start with separator: " + key);
      assertFalse(
          key.endsWith(".") || key.endsWith("-") || key.endsWith("_"),
          "Key should not end with separator: " + key);
    }
  }

  @Test
  void shouldHaveReasonableTranslationLengths() {
    Properties baseProps = languageProperties.get(BASE_LANGUAGE);

    for (String lang : SUPPORTED_LANGUAGES) {
      if (lang.equals(BASE_LANGUAGE)) continue;

      Properties langProps = languageProperties.get(lang);

      for (String key : baseProps.stringPropertyNames()) {
        String baseValue = baseProps.getProperty(key);
        String langValue = langProps.getProperty(key);

        if (baseValue != null && langValue != null) {
          // Translation should not be more than 3x the base length
          // (accounting for language differences)
          int maxLength = Math.max(baseValue.length() * 3, 200);
          assertTrue(
              langValue.length() <= maxLength,
              String.format(
                  "Translation too long for key '%s' in language '%s': %d chars (base: %d)",
                  key, lang, langValue.length(), baseValue.length()));

          // Translation should not be suspiciously short compared to base
          // (unless base is very short)
          if (baseValue.length() > 10) {
            int minLength = baseValue.length() / 3;
            assertTrue(
                langValue.length() >= minLength,
                String.format(
                    "Translation suspiciously short for key '%s' in language '%s': %d chars (base: %d)",
                    key, lang, langValue.length(), baseValue.length()));
          }
        }
      }
    }
  }

  @Test
  void shouldNotContainPlaceholderText() {
    for (String lang : SUPPORTED_LANGUAGES) {
      Properties props = languageProperties.get(lang);

      for (String key : props.stringPropertyNames()) {
        String value = props.getProperty(key);

        // Check for common English placeholder patterns (as whole words)
        // Only check for English placeholders, not words that happen to contain these letters
        if (lang.equals("en")) {
          assertFalse(
              value.toLowerCase().matches(".*\\btodo\\b.*"),
              String.format("Contains TODO placeholder in key '%s' for language '%s'", key, lang));
          assertFalse(
              value.toLowerCase().matches(".*\\bfixme\\b.*"),
              String.format("Contains FIXME placeholder in key '%s' for language '%s'", key, lang));
        }
        assertFalse(
            value.contains("XXX"),
            String.format("Contains XXX placeholder in key '%s' for language '%s'", key, lang));
        assertFalse(
            value.contains("TBD"),
            String.format("Contains TBD placeholder in key '%s' for language '%s'", key, lang));

        // Check that it's not just the English text copied
        if (!lang.equals("en")) {
          Properties enProps = languageProperties.get("en");
          String enValue = enProps.getProperty(key);
          if (enValue != null && value.length() > 10) {
            assertNotEquals(
                enValue,
                value,
                String.format(
                    "Translation appears to be untranslated English for key '%s' in language '%s'",
                    key, lang));
          }
        }
      }
    }
  }

  @Test
  void shouldHaveValidCharacterEncoding() {
    for (String lang : SUPPORTED_LANGUAGES) {
      Properties props = languageProperties.get(lang);

      for (String key : props.stringPropertyNames()) {
        String value = props.getProperty(key);

        // Check for proper UTF-8 encoding (no replacement characters)
        assertFalse(
            value.contains("\uFFFD"),
            String.format(
                "Contains replacement character (encoding issue) in key '%s' for language '%s'",
                key, lang));

        // Check for language-specific characters
        if (lang.equals("de")) {
          // German should contain umlauts if it's a substantial translation
          if (value.length() > 20 && !containsGermanChars(value)) {
            System.out.println(
                String.format("Warning: German text for key '%s' may be missing umlauts", key));
          }
        } else if (lang.equals("fr")) {
          // French should contain accents if it's a substantial translation
          if (value.length() > 20 && !containsFrenchChars(value)) {
            System.out.println(
                String.format("Warning: French text for key '%s' may be missing accents", key));
          }
        } else if (lang.equals("es")) {
          // Spanish should contain accents/tildes if it's a substantial translation
          if (value.length() > 20 && !containsSpanishChars(value)) {
            System.out.println(
                String.format("Warning: Spanish text for key '%s' may be missing accents", key));
          }
        }
      }
    }
  }

  private Properties loadProperties(String resourcePath) throws IOException {
    Properties props = new Properties();
    try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
      if (is != null) {
        props.load(is);
      }
    }
    return props;
  }

  private boolean containsGermanChars(String text) {
    return text.matches(".*[äöüÄÖÜß].*");
  }

  private boolean containsFrenchChars(String text) {
    return text.matches(".*[àâäéèêëïîôöùûüÿçÀÂÄÉÈÊËÏÎÔÖÙÛÜŸÇ].*");
  }

  private boolean containsSpanishChars(String text) {
    return text.matches(".*[áéíóúüñÁÉÍÓÚÜÑ¿¡].*");
  }
}
