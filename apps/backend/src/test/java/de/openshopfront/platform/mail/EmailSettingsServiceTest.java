package de.openshopfront.platform.mail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/** Verifies per-language template rendering and variable substitution. */
class EmailSettingsServiceTest {
  private NamedParameterJdbcTemplate emptyDb() {
    @SuppressWarnings("unchecked")
    NamedParameterJdbcTemplate db = mock(NamedParameterJdbcTemplate.class);
    when(db.query(any(String.class), anyMap(), any(RowMapper.class))).thenReturn(List.of());
    return db;
  }

  @Test
  void substitutesKnownTokensAndKeepsUnknownOnes() {
    Map<String, String> vars = new LinkedHashMap<>();
    vars.put("orderNumber", "SO-1");
    vars.put("customerName", "Max Mustermann");
    assertEquals(
        "Hi Max Mustermann, your order SO-1 and {unknown} are ready.",
        EmailSettingsService.substitute(
            "Hi {customerName}, your order {orderNumber} and {unknown} are ready.", vars));
  }

  @Test
  void valuesAreInsertedInOnePassWithoutRescanning() {
    Map<String, String> vars = new LinkedHashMap<>();
    vars.put("message", "please see {formName}");
    vars.put("formName", "Contact form");
    assertEquals("please see {formName}", EmailSettingsService.substitute("{message}", vars));
  }

  @Test
  void usesFallbackTextWhenNoTemplateIsConfigured() {
    var service = new EmailSettingsService(emptyDb(), new ObjectMapper());
    var rendered =
        service.render(
            EmailSettingsService.Channel.ORDER_CONFIRMATION,
            null,
            "de",
            Map.of("orderNumber", "SO-1"),
            "fallback body");
    assertEquals("Order confirmed: SO-1", rendered.subject());
    assertEquals("fallback body", rendered.body());
    assertFalse(rendered.html());
  }

  @Test
  void picksTemplateForRequestedLanguage() {
    // Direct test of the pure resolution logic via a stored row.
    var service = new EmailSettingsService(emptyDb(), new ObjectMapper());
    Map<String, String> formTemplates = Map.of("de", "<p>de</p>", "en", "<p>en</p>");
    var stored = new EmailSettingsService.Stored(Map.of(), formTemplates, Map.of(), Map.of());

    // Use the public render method with a mocked loadStored via subclass or direct pick test.
    assertEquals("<p>de</p>", EmailSettingsService.pick(formTemplates, "de"));
    assertEquals("<p>en</p>", EmailSettingsService.pick(formTemplates, "en"));
  }

  @Test
  void fallsBackToDefaultLanguageWhenRequestedMissing() {
    Map<String, String> formTemplates = Map.of("default", "<p>fallback</p>", "de", "<p>de</p>");
    assertEquals("<p>fallback</p>", EmailSettingsService.pick(formTemplates, "ru"));
    assertEquals("<p>de</p>", EmailSettingsService.pick(formTemplates, "de"));
  }

  @Test
  void subjectFallsBackToBuiltInWhenNotConfigured() {
    var service = new EmailSettingsService(emptyDb(), new ObjectMapper());
    Map<String, String> vars = Map.of("orderNumber", "SO-9");
    var rendered =
        service.render(
            EmailSettingsService.Channel.ORDER_CONFIRMATION, null, "en", vars, "fallback");
    assertEquals("Order confirmed: SO-9", rendered.subject());
  }

  @Test
  void pickReturnsNullWhenEmpty() {
    assertNull(EmailSettingsService.pick(Map.of(), "de"));
    assertNull(EmailSettingsService.pick(null, "de"));
  }

  @Test
  void pickPrefersExactLanguageOverDefault() {
    Map<String, String> map = Map.of("default", "fallback", "de", "exact");
    assertEquals("exact", EmailSettingsService.pick(map, "de"));
  }
}
