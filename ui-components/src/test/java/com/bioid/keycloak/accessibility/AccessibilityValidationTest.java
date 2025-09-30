package com.bioid.keycloak.accessibility;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Accessibility validation tests for Face Recognition Extension. Tests WCAG 2.1 AA compliance by
 * analyzing template files and CSS.
 */
public class AccessibilityValidationTest {

  private List<String> templateFiles;
  private List<String> cssFiles;
  private List<String> jsFiles;

  @BeforeEach
  void setUp() {
    templateFiles = new ArrayList<>();
    cssFiles = new ArrayList<>();
    jsFiles = new ArrayList<>();

    // Add template files to test
    templateFiles.add("ui-components/src/main/resources/theme/base/login/face-enroll.ftl");
    templateFiles.add(
        "face-authenticator/src/main/resources/theme/base/login/face-authenticate.ftl");

    // Add CSS files to test
    cssFiles.add("ui-components/src/main/resources/theme/base/resources/css/accessibility.css");
    cssFiles.add("ui-components/src/main/resources/theme/base/resources/css/face-enrollment.css");

    // Add JS files to test
    jsFiles.add("ui-components/src/main/resources/theme/base/resources/js/accessibility-utils.js");
    jsFiles.add("ui-components/src/main/resources/theme/base/resources/js/language-switcher.js");
  }

  @Test
  void testTemplateAccessibilityAttributes() throws IOException {
    for (String templateFile : templateFiles) {
      Path path = Paths.get(templateFile);
      if (Files.exists(path)) {
        String content = Files.readString(path);
        validateTemplateAccessibility(content, templateFile);
      }
    }
  }

  @Test
  void testCSSAccessibilityFeatures() throws IOException {
    for (String cssFile : cssFiles) {
      Path path = Paths.get(cssFile);
      if (Files.exists(path)) {
        String content = Files.readString(path);
        validateCSSAccessibility(content, cssFile);
      }
    }
  }

  @Test
  void testJavaScriptAccessibilityFeatures() throws IOException {
    for (String jsFile : jsFiles) {
      Path path = Paths.get(jsFile);
      if (Files.exists(path)) {
        String content = Files.readString(path);
        validateJSAccessibility(content, jsFile);
      }
    }
  }

  @Test
  void testARIALabelPresence() throws IOException {
    for (String templateFile : templateFiles) {
      Path path = Paths.get(templateFile);
      if (Files.exists(path)) {
        String content = Files.readString(path);

        // Check for interactive elements without ARIA labels
        Pattern buttonPattern = Pattern.compile("<button[^>]*>");
        Matcher buttonMatcher = buttonPattern.matcher(content);

        while (buttonMatcher.find()) {
          String buttonTag = buttonMatcher.group();
          assertTrue(
              buttonTag.contains("aria-label")
                  || buttonTag.contains("aria-labelledby")
                  || buttonTag.contains("aria-describedby"),
              "Button should have ARIA label in " + templateFile + ": " + buttonTag);
        }

        // Check for input elements without labels
        Pattern inputPattern = Pattern.compile("<input[^>]*>");
        Matcher inputMatcher = inputPattern.matcher(content);

        while (inputMatcher.find()) {
          String inputTag = inputMatcher.group();
          if (!inputTag.contains("type=\"hidden\"")) {
            assertTrue(
                inputTag.contains("aria-label")
                    || inputTag.contains("aria-labelledby")
                    || content.contains("label")
                    || // Has associated label
                    inputTag.contains("placeholder"),
                "Input should have accessible name in " + templateFile + ": " + inputTag);
          }
        }
      }
    }
  }

  @Test
  void testHeadingHierarchy() throws IOException {
    for (String templateFile : templateFiles) {
      Path path = Paths.get(templateFile);
      if (Files.exists(path)) {
        String content = Files.readString(path);

        // Check for proper heading hierarchy
        Pattern headingPattern = Pattern.compile("<h([1-6])[^>]*>");
        Matcher headingMatcher = headingPattern.matcher(content);

        int previousLevel = 0;
        boolean hasH1 = false;

        while (headingMatcher.find()) {
          int currentLevel = Integer.parseInt(headingMatcher.group(1));

          if (currentLevel == 1) {
            hasH1 = true;
          }

          if (previousLevel > 0) {
            assertTrue(
                currentLevel <= previousLevel + 1,
                "Heading levels should not skip in "
                    + templateFile
                    + ": h"
                    + previousLevel
                    + " to h"
                    + currentLevel);
          }

          previousLevel = currentLevel;
        }

        assertTrue(hasH1, "Page should have an h1 heading in " + templateFile);
      }
    }
  }

  @Test
  void testLiveRegionsPresence() throws IOException {
    for (String templateFile : templateFiles) {
      Path path = Paths.get(templateFile);
      if (Files.exists(path)) {
        String content = Files.readString(path);

        // Check for ARIA live regions
        assertTrue(
            content.contains("aria-live=\"polite\"") || content.contains("aria-live=\"assertive\""),
            "Template should have ARIA live regions for screen reader announcements: "
                + templateFile);
      }
    }
  }

  @Test
  void testKeyboardNavigationSupport() throws IOException {
    Path jsPath =
        Paths.get(
            "ui-components/src/main/resources/theme/base/resources/js/accessibility-utils.js");
    if (Files.exists(jsPath)) {
      String content = Files.readString(jsPath);

      // Check for keyboard event handling
      assertTrue(content.contains("keydown"), "Accessibility utils should handle keyboard events");
      assertTrue(content.contains("Tab"), "Should handle Tab key navigation");
      assertTrue(content.contains("Escape"), "Should handle Escape key");
      assertTrue(content.contains("Enter"), "Should handle Enter key activation");
    }
  }

  @Test
  void testHighContrastSupport() throws IOException {
    Path cssPath =
        Paths.get("ui-components/src/main/resources/theme/base/resources/css/accessibility.css");
    if (Files.exists(cssPath)) {
      String content = Files.readString(cssPath);

      // Check for high contrast mode styles
      assertTrue(
          content.contains(".high-contrast"), "CSS should include high contrast mode styles");
      assertTrue(
          content.contains("prefers-contrast: high"),
          "CSS should respect system high contrast preference");
      assertTrue(
          content.contains("--bg-primary"),
          "Should use CSS custom properties for high contrast colors");
    }
  }

  @Test
  void testReducedMotionSupport() throws IOException {
    Path cssPath =
        Paths.get("ui-components/src/main/resources/theme/base/resources/css/accessibility.css");
    if (Files.exists(cssPath)) {
      String content = Files.readString(cssPath);

      // Check for reduced motion support
      assertTrue(
          content.contains("prefers-reduced-motion"),
          "CSS should respect reduced motion preference");
      assertTrue(content.contains(".reduced-motion"), "Should have reduced motion class");
      assertTrue(
          content.contains("animation-duration: 0.01ms"),
          "Should disable animations for reduced motion");
    }
  }

  @Test
  void testFocusIndicators() throws IOException {
    Path cssPath =
        Paths.get("ui-components/src/main/resources/theme/base/resources/css/accessibility.css");
    if (Files.exists(cssPath)) {
      String content = Files.readString(cssPath);

      // Check for focus indicators
      assertTrue(content.contains(":focus"), "CSS should include focus indicators");
      assertTrue(content.contains("outline"), "Focus indicators should use outline");
      assertTrue(
          content.contains("keyboard-navigation"), "Should differentiate keyboard vs mouse focus");
    }
  }

  @Test
  void testScreenReaderSupport() throws IOException {
    for (String templateFile : templateFiles) {
      Path path = Paths.get(templateFile);
      if (Files.exists(path)) {
        String content = Files.readString(path);

        // Check for screen reader only content
        assertTrue(
            content.contains("sr-only") || content.contains("screen-reader"),
            "Template should have screen reader only content: " + templateFile);

        // Check for proper roles
        assertTrue(
            content.contains("role=\"main\"")
                || content.contains("role=\"button\"")
                || content.contains("role=\"group\"")
                || content.contains("role=\"progressbar\""),
            "Template should use ARIA roles: " + templateFile);
      }
    }
  }

  @Test
  void testColorContrastCompliance() throws IOException {
    // This is a simplified test - in a real implementation,
    // you would calculate actual contrast ratios
    Path cssPath =
        Paths.get("ui-components/src/main/resources/theme/base/resources/css/accessibility.css");
    if (Files.exists(cssPath)) {
      String content = Files.readString(cssPath);

      // Check that high contrast colors are defined
      assertTrue(
          content.contains("#000000") || content.contains("black"),
          "Should define high contrast black color");
      assertTrue(
          content.contains("#ffffff") || content.contains("white"),
          "Should define high contrast white color");
      assertTrue(
          content.contains("#ffff00") || content.contains("yellow"),
          "Should define high contrast yellow for focus");
    }
  }

  @Test
  void testFormAccessibility() throws IOException {
    for (String templateFile : templateFiles) {
      Path path = Paths.get(templateFile);
      if (Files.exists(path)) {
        String content = Files.readString(path);

        // Check for form labels
        if (content.contains("<form")) {
          // If there are form controls, they should have labels
          if (content.contains("<input") && !content.contains("type=\"hidden\"")) {
            assertTrue(
                content.contains("<label") || content.contains("aria-label"),
                "Form controls should have labels in " + templateFile);
          }
        }

        // Check for error message associations
        if (content.contains("error")) {
          assertTrue(
              content.contains("aria-describedby") || content.contains("role=\"alert\""),
              "Error messages should be properly associated in " + templateFile);
        }
      }
    }
  }

  private void validateTemplateAccessibility(String content, String filename) {
    // Check for semantic HTML
    assertTrue(
        content.contains("<main") || content.contains("role=\"main\""),
        "Template should have main landmark: " + filename);

    // Check for skip links
    if (content.contains("skip-link") || content.contains("Skip to")) {
      // Good - has skip link
    } else {
      System.out.println("Warning: No skip link found in " + filename);
    }

    // Check for lang attribute
    assertTrue(
        content.contains("lang=") || content.contains("xml:lang="),
        "Template should specify language: " + filename);
  }

  private void validateCSSAccessibility(String content, String filename) {
    // Check for accessibility-specific CSS
    if (filename.contains("accessibility")) {
      assertTrue(
          content.contains("sr-only") || content.contains("screen-reader"),
          "Accessibility CSS should define screen reader classes: " + filename);

      assertTrue(
          content.contains("focus"), "Accessibility CSS should define focus styles: " + filename);
    }

    // Check for responsive design
    assertTrue(content.contains("@media"), "CSS should include responsive design: " + filename);
  }

  private void validateJSAccessibility(String content, String filename) {
    if (filename.contains("accessibility")) {
      // Check for keyboard handling
      assertTrue(
          content.contains("keydown") || content.contains("keypress"),
          "Accessibility JS should handle keyboard events: " + filename);

      // Check for ARIA updates
      assertTrue(
          content.contains("aria-") || content.contains("setAttribute"),
          "Accessibility JS should manage ARIA attributes: " + filename);

      // Check for screen reader announcements
      assertTrue(
          content.contains("aria-live") || content.contains("announceToScreenReader"),
          "Accessibility JS should support screen reader announcements: " + filename);
    }
  }
}
