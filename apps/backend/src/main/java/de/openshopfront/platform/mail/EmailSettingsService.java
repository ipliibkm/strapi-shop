package de.openshopfront.platform.mail;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

/** Stores configurable mail subjects and HTML templates per language, then renders one send. */
@Service
public class EmailSettingsService {
  /** One platform mail channel that can be configured in the admin UI. */
  public enum Channel {
    FORM_SUBMISSION,
    ORDER_CONFIRMATION
  }

  private static final Pattern TOKEN = Pattern.compile("\\{([a-zA-Z][a-zA-Z0-9_]*)\\}");
  private static final String DEFAULT_FORM_SUBJECT = "Your form submission was received";
  private static final String DEFAULT_ORDER_SUBJECT = "Order confirmed: {orderNumber}";

  /** The map key that holds the fallback language. */
  public static final String FALLBACK_KEY = "default";

  /** Variables available in order confirmation templates. */
  public static final List<String> ORDER_VARIABLES =
      List.of(
          "orderNumber",
          "customerName",
          "email",
          "street",
          "city",
          "postalCode",
          "items",
          "subtotal",
          "tax",
          "shipping",
          "total",
          "date");

  /** Base variables available in form submission templates; each field key is added per form. */
  public static final List<String> FORM_BASE_VARIABLES = List.of("formName");

  private final NamedParameterJdbcTemplate db;
  private final ObjectMapper json;

  public EmailSettingsService(NamedParameterJdbcTemplate db, ObjectMapper json) {
    this.db = db;
    this.json = json;
  }

  // ---------- persistence ----------

  /** Reads the single settings row as per-language maps. */
  public Stored loadStored() {
    List<Stored> rows =
        db.query(
            """
            select form_subjects::text,form_templates::text,order_subjects::text,order_templates::text
              from email_settings where id=1""",
            Map.of(),
            (rs, row) ->
                new Stored(
                    readMap(rs.getString("form_subjects")),
                    readMap(rs.getString("form_templates")),
                    readMap(rs.getString("order_subjects")),
                    readMap(rs.getString("order_templates"))));
    return rows.isEmpty() ? Stored.defaults() : rows.get(0);
  }

  /** Creates or replaces the single settings row with per-language maps. */
  public void save(
      Map<String, String> formSubjects,
      Map<String, String> formTemplates,
      Map<String, String> orderSubjects,
      Map<String, String> orderTemplates) {
    db.update(
        """
        insert into email_settings (id,form_subjects,form_templates,order_subjects,order_templates,updated_at)
         values (1,:fs::jsonb,:ft::jsonb,:os::jsonb,:ot::jsonb,now())
         on conflict (id) do update set form_subjects=excluded.form_subjects,
           form_templates=excluded.form_templates, order_subjects=excluded.order_subjects,
           order_templates=excluded.order_templates, updated_at=now()""",
        Map.of(
            "fs", writeMap(formSubjects),
            "ft", writeMap(formTemplates),
            "os", writeMap(orderSubjects),
            "ot", writeMap(orderTemplates)));
  }

  /** The template override stored for one form slug (language-neutral). */
  public Optional<String> formOverride(String formSlug) {
    List<String> rows =
        db.query(
            "select template from form_email_template where form_slug=:slug",
            Map.of("slug", formSlug),
            (rs, row) -> rs.getString("template"));
    if (rows.isEmpty() || rows.get(0) == null || rows.get(0).isBlank()) return Optional.empty();
    return Optional.of(rows.get(0));
  }

  /** Stores or clears the template override for one form. */
  public void saveFormOverride(String formSlug, String template) {
    if (template == null || template.isBlank()) {
      db.update("delete from form_email_template where form_slug=:slug", Map.of("slug", formSlug));
      return;
    }
    db.update(
        """
        insert into form_email_template (form_slug,template,updated_at) values (:slug,:template,now())
         on conflict (form_slug) do update set template=excluded.template, updated_at=now()""",
        Map.of("slug", formSlug, "template", template));
  }

  // ---------- rendering ----------

  /** The rendered content ready to send: final subject, body text or HTML. */
  public record Rendered(String subject, String body, boolean html) {}

  /** One stored settings row with per-language maps (map keys are language codes). */
  public record Stored(
      Map<String, String> formSubjects,
      Map<String, String> formTemplates,
      Map<String, String> orderSubjects,
      Map<String, String> orderTemplates) {

    public static Stored defaults() {
      return new Stored(Map.of(), Map.of(), Map.of(), Map.of());
    }
  }

  /**
   * Renders one mail for a channel and language. Resolution order: per-form override (all
   * languages), template for the requested language, template for "default", built-in text
   * fallback.
   */
  public Rendered render(
      Channel channel,
      String formSlug,
      String locale,
      Map<String, String> variables,
      String fallbackBody) {
    Stored settings = loadStored();
    Optional<String> override =
        channel == Channel.FORM_SUBMISSION && formSlug != null && !formSlug.isBlank()
            ? formOverride(formSlug)
            : Optional.empty();

    Map<String, String> subjects =
        channel == Channel.FORM_SUBMISSION ? settings.formSubjects() : settings.orderSubjects();
    String subjectValue = pick(subjects, locale);
    if (subjectValue == null) subjectValue = builtInSubject(channel);
    String subject = substitute(subjectValue, variables);

    Map<String, String> templates =
        channel == Channel.FORM_SUBMISSION ? settings.formTemplates() : settings.orderTemplates();
    String template = override.orElseGet(() -> pick(templates, locale));
    if (template == null || template.strip().isBlank()) {
      return new Rendered(subject, fallbackBody, false);
    }
    return new Rendered(subject, substitute(template.strip(), variables), true);
  }

  /** Picks the value for a language code with "default" as fallback. */
  public static String pick(Map<String, String> map, String locale) {
    if (map == null || map.isEmpty()) return null;
    String key = locale == null ? "" : locale.strip();
    String value = map.get(key);
    if (value != null && !value.isBlank()) return value;
    String fallback = map.get(FALLBACK_KEY);
    return fallback == null || fallback.isBlank() ? null : fallback;
  }

  private static String builtInSubject(Channel channel) {
    return channel == Channel.FORM_SUBMISSION ? DEFAULT_FORM_SUBJECT : DEFAULT_ORDER_SUBJECT;
  }

  // ---------- helpers ----------

  /**
   * Replaces every {variable} token with its value in a single pass. Unknown tokens stay as text.
   */
  public static String substitute(String text, Map<String, String> variables) {
    if (text == null || variables.isEmpty()) return text == null ? "" : text;
    Matcher matcher = TOKEN.matcher(text);
    StringBuilder out = new StringBuilder();
    while (matcher.find()) {
      String value = variables.get(matcher.group(1));
      matcher.appendReplacement(
          out, value == null ? matcher.group() : Matcher.quoteReplacement(value));
    }
    matcher.appendTail(out);
    return out.toString();
  }

  private Map<String, String> readMap(String jsonText) {
    if (jsonText == null || jsonText.isBlank()) return Map.of();
    try {
      Map<String, String> map =
          json.readValue(jsonText, new TypeReference<Map<String, String>>() {});
      // Drop blank values so pick() treats them as "not configured".
      map.entrySet().removeIf(entry -> entry.getValue() == null || entry.getValue().isBlank());
      return map;
    } catch (Exception error) {
      return Map.of();
    }
  }

  private String writeMap(Map<String, String> map) {
    try {
      Map<String, String> clean = new java.util.LinkedHashMap<>();
      if (map != null) {
        for (Map.Entry<String, String> entry : map.entrySet()) {
          String key = entry.getKey() == null ? "" : entry.getKey().strip();
          String value = entry.getValue() == null ? "" : entry.getValue().strip();
          if (!key.isEmpty()) clean.put(key, value);
        }
      }
      return json.writeValueAsString(clean);
    } catch (Exception error) {
      throw new IllegalStateException("The mail settings are not valid JSON.");
    }
  }
}
