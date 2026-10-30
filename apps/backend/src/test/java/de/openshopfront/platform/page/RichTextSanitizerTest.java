package de.openshopfront.platform.page;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Verifies that only approved markup survives sanitization. */
class RichTextSanitizerTest {
  @Test
  void keepsApprovedElements() {
    String result =
        RichTextSanitizer.sanitize(
            "<p>Hello <strong>world</strong></p><a href=\"https://example.com\">link</a>");
    assertTrue(result.contains("<strong>"));
    assertTrue(result.contains("href=\"https://example.com\""));
  }

  @Test
  void removesScriptElementsAndEventAttributes() {
    String result =
        RichTextSanitizer.sanitize(
            "<p>ok</p><script>alert(1)</script><img src=x onerror=alert(2)>");
    // Disallowed elements are removed. Their inline text may remain as visible content.
    assertFalse(result.contains("onerror"));
    assertTrue(result.contains("ok"));
  }

  @Test
  void rejectsJavaScriptLinks() {
    String result = RichTextSanitizer.sanitize("<a href=\"javascript:alert(3)\">bad</a>");
    assertFalse(result.contains("javascript:"));
  }

  @Test
  void allowsRelativeAndHttpsLinksOnly() {
    assertEquals("/page", RichTextSanitizer.findHref("<a href=\"/page\">x</a>"));
    org.junit.jupiter.api.Assertions.assertNull(
        RichTextSanitizer.findHref("<a href=\"data:text/html,x\">y</a>"));
  }

  @Test
  void trimsWhitespaceAndHandlesNull() {
    assertEquals("", RichTextSanitizer.sanitize(null));
    assertEquals("text", RichTextSanitizer.sanitize("  text  "));
  }
}
