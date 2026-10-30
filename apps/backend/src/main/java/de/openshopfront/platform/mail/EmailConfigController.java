package de.openshopfront.platform.mail;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Manages mail subjects and HTML templates per language for the configured channels. */
@RestController
@RequestMapping("/api/v1/admin/email")
public class EmailConfigController {
  private static final int MAX_SUBJECT = 254;
  private static final int MAX_TEMPLATE = 60_000;
  private static final String SLUG = "[a-z][a-z0-9_]{0,63}";

  /** The per-language subject lines and templates of one channel. */
  public record ChannelPayload(Map<String, String> subjects, Map<String, String> templates) {}

  /** Saves both channels in one request (per-language maps). */
  public record SettingsRequest(ChannelPayload form, ChannelPayload order) {}

  /** Holds one per-form template override (language-neutral). */
  public record TemplatePayload(String template) {}

  private final EmailSettingsService settings;
  private final ObjectMapper json;

  public EmailConfigController(EmailSettingsService settings, ObjectMapper json) {
    this.settings = settings;
    this.json = json;
  }

  @GetMapping("/settings")
  public Map<String, Object> get() throws JsonProcessingException {
    EmailSettingsService.Stored stored = settings.loadStored();
    return Map.of(
        "form",
            Map.of(
                "subjects", stored.formSubjects(),
                "templates", stored.formTemplates()),
        "order",
            Map.of(
                "subjects", stored.orderSubjects(),
                "templates", stored.orderTemplates()),
        "variables",
            Map.of(
                "order", EmailSettingsService.ORDER_VARIABLES,
                "form", EmailSettingsService.FORM_BASE_VARIABLES));
  }

  /** Replaces the per-language subject lines and templates of both channels. */
  @PutMapping("/settings")
  public void save(@RequestBody SettingsRequest request) throws JsonProcessingException {
    if (request == null || request.form() == null || request.order() == null)
      throw new IllegalArgumentException(
          "Enter the subject lines and templates for both channels.");

    Map<String, String> formSubjects = cleanSubjects(request.form().subjects(), "form");
    Map<String, String> formTemplates = cleanTemplates(request.form().templates());
    Map<String, String> orderSubjects = cleanSubjects(request.order().subjects(), "order");
    Map<String, String> orderTemplates = cleanTemplates(request.order().templates());

    settings.save(formSubjects, formTemplates, orderSubjects, orderTemplates);
  }

  @GetMapping("/forms/{slug}/template")
  public Map<String, String> getFormTemplate(@PathVariable String slug) {
    validateSlug(slug);
    return Map.of("template", settings.formOverride(slug).orElse(""));
  }

  /** Stores or clears the template override of one form. */
  @PutMapping("/forms/{slug}/template")
  public void putFormTemplate(@PathVariable String slug, @RequestBody TemplatePayload payload) {
    validateSlug(slug);
    settings.saveFormOverride(slug, template(payload == null ? null : payload.template()));
  }

  private static void validateSlug(String slug) {
    if (slug == null || !slug.matches(SLUG))
      throw new IllegalArgumentException("This form does not exist.");
  }

  private Map<String, String> cleanSubjects(Map<String, String> map, String channel)
      throws JsonProcessingException {
    Map<String, String> out = new java.util.LinkedHashMap<>();
    if (map != null) {
      for (Map.Entry<String, String> entry : map.entrySet()) {
        String key = entry.getKey() == null ? "" : entry.getKey().strip();
        String value = entry.getValue() == null ? "" : entry.getValue().strip();
        if (!key.matches("[a-z]{2,8}|default"))
          throw new IllegalArgumentException("The " + channel + " subject language is not valid.");
        if (value.length() > MAX_SUBJECT)
          throw new IllegalArgumentException("The " + channel + " subject line is too long.");
        out.put(key, value);
      }
    }
    return out;
  }

  private Map<String, String> cleanTemplates(Map<String, String> map) {
    Map<String, String> out = new java.util.LinkedHashMap<>();
    if (map != null) {
      for (Map.Entry<String, String> entry : map.entrySet()) {
        String key = entry.getKey() == null ? "" : entry.getKey().strip();
        String value = entry.getValue() == null ? "" : entry.getValue().strip();
        if (key.isEmpty()) continue;
        if (!key.matches("[a-z]{2,8}|default")) continue;
        if (value.length() > MAX_TEMPLATE)
          throw new IllegalArgumentException("The mail template is too long.");
        out.put(key, value);
      }
    }
    return out;
  }

  private static String template(String value) {
    if (value == null) return "";
    if (value.length() > MAX_TEMPLATE)
      throw new IllegalArgumentException("The mail template is too long.");
    return value;
  }

  @ExceptionHandler(IllegalArgumentException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public Map<String, String> invalid(IllegalArgumentException error) {
    return Map.of("code", "VALIDATION_ERROR", "message", error.getMessage());
  }
}
