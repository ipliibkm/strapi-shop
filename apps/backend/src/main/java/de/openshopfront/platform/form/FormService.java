package de.openshopfront.platform.form;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.openshopfront.platform.auth.StrapiAdminClient;
import de.openshopfront.platform.mail.EmailSettingsService;
import de.openshopfront.platform.mail.MailService;
import jakarta.servlet.http.HttpSession;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/** Manages reusable Strapi form definitions and validated customer submissions. */
@Service
public class FormService {
  private static final Logger log = LoggerFactory.getLogger(FormService.class);
  private static final String UID = "api::storefront-form.storefront-form";
  private static final String CONTENT_MANAGER_PATH = "/content-manager/collection-types/" + UID;
  private static final String SUBMISSION_UID = "api::form-submission.form-submission";
  private static final String SUBMISSION_MANAGER_PATH =
      "/content-manager/collection-types/" + SUBMISSION_UID;
  private static final Set<String> FIELD_TYPES =
      Set.of("text", "email", "tel", "textarea", "select", "checkbox");
  private static final Pattern KEY = Pattern.compile("[a-z][a-z0-9_]{0,63}");
  private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
  private static final int MAX_FIELDS = 40;

  private final StrapiAdminClient admin;
  private final RestClient publicApi;
  private final ObjectMapper json;
  private final String serviceToken;
  private final MailService mail;
  private final EmailSettingsService email;

  public FormService(
      StrapiAdminClient admin,
      ObjectMapper json,
      @Value("${platform.strapi.base-url}") String baseUrl,
      @Value("${platform.strapi.service-token:}") String serviceToken,
      MailService mail,
      EmailSettingsService email) {
    this.admin = admin;
    this.json = json;
    this.publicApi = RestClient.builder().baseUrl(baseUrl).build();
    this.serviceToken = (serviceToken == null || serviceToken.isBlank()) ? null : serviceToken;
    this.mail = mail;
    this.email = email;
  }

  /** Writes one submission using the best available credential. */
  private JsonNode writeSubmission(
      String method, String path, ObjectNode body, HttpSession session) {
    if (session != null) return admin.content(session, method, path, body);
    if (serviceToken == null) {
      throw new IllegalStateException(
          "Set STRAPI_SERVICE_TOKEN so the platform can store anonymous form submissions.");
    }
    return admin.contentWithToken(method, path, body);
  }

  /** Lists reusable forms for the administrator workspace. */
  public List<FormSummary> list(HttpSession session) {
    JsonNode response =
        admin.content(
            session, "GET", CONTENT_MANAGER_PATH + "?page=1&pageSize=100&sort=name:ASC", null);
    List<FormSummary> forms = new ArrayList<>();
    for (JsonNode item : response.path("results")) {
      String status = item.path("status").asText("draft");
      forms.add(
          new FormSummary(
              item.path("documentId").asText(),
              item.path("slug").asText(),
              item.path("name").asText(),
              item.path("enabled").asBoolean(true),
              !"draft".equals(status),
              item.path("fields").size()));
    }
    return List.copyOf(forms);
  }

  /** Loads one editable form draft. */
  public FormDefinition find(String slug, HttpSession session) {
    return findOptional(slug, session)
        .orElseThrow(() -> new IllegalArgumentException("This form does not exist."));
  }

  /** Creates or updates one validated form draft. */
  public FormDefinition save(String slug, SaveForm request, HttpSession session) {
    String cleanSlug = validateSlug(slug);
    ObjectNode body = json.createObjectNode();
    body.put("name", required(request.name(), "Enter a form name.", 200));
    body.put("slug", cleanSlug);
    body.put("enabled", request.enabled());
    body.put("sendMailEnabled", request.sendMailEnabled() == null || request.sendMailEnabled());
    body.put("submitLabel", required(request.submitLabel(), "Enter a submit button label.", 80));
    body.put(
        "successMessage",
        required(request.successMessage(), "Enter a submission confirmation message.", 500));
    ArrayNode fields = body.putArray("fields");
    normalizeFields(request.fields()).forEach(field -> fields.add(json.valueToTree(field)));

    Optional<FormDefinition> current = findOptional(cleanSlug, session);
    if (current.isPresent()) {
      admin.content(session, "PUT", CONTENT_MANAGER_PATH + "/" + current.get().documentId(), body);
    } else {
      admin.content(session, "POST", CONTENT_MANAGER_PATH, body);
    }
    return find(cleanSlug, session);
  }

  /** Publishes the current form draft. */
  public FormDefinition publish(String slug, HttpSession session) {
    FormDefinition current = find(slug, session);
    admin.content(
        session,
        "POST",
        CONTENT_MANAGER_PATH + "/" + current.documentId() + "/actions/publish",
        json.createObjectNode());
    return find(slug, session);
  }

  /** Loads one enabled, published form for storefront rendering and validation. */
  public FormDefinition published(String slug) {
    try {
      JsonNode item =
          publicApi
              .get()
              .uri("/api/storefront-forms/by-slug/{slug}", slug)
              .accept(MediaType.APPLICATION_JSON)
              .retrieve()
              .body(JsonNode.class);
      if (item == null) throw new IllegalArgumentException("This form is not available.");
      return formFrom(item, true);
    } catch (HttpClientErrorException.NotFound error) {
      throw new IllegalArgumentException("This form is not available.");
    }
  }

  /** Validates and stores one public form submission as a Strapi content document. */
  public SubmissionResult submit(
      String slug, JsonNode values, String website, String locale, HttpSession session) {
    FormDefinition form = published(slug);
    if (website != null && !website.isBlank())
      return new SubmissionResult(null, form.successMessage());
    if (values == null || !values.isObject())
      throw new IllegalArgumentException("The form values are not valid.");

    ObjectNode clean = json.createObjectNode();
    for (FormField field : form.fields()) {
      JsonNode raw = values.get(field.key());
      if ("checkbox".equals(field.type())) {
        boolean checked = raw != null && raw.asBoolean(false);
        if (field.required() && !checked)
          throw new IllegalArgumentException(field.label() + " must be selected.");
        clean.put(field.key(), checked);
        continue;
      }
      String value = raw != null && raw.isTextual() ? raw.asText().strip() : "";
      if (field.required() && value.isEmpty())
        throw new IllegalArgumentException("Enter a value for " + field.label() + ".");
      if (!value.isEmpty()) validateValue(field, value);
      clean.put(field.key(), value);
    }

    // Submissions are content: store them in Strapi, linked to the form document.
    if (form.documentId() == null || form.documentId().isBlank()) {
      throw new IllegalStateException(
          "This form is not saved. Save and publish the form before accepting submissions.");
    }

    // Create the submission via the Content API using the service token.
    ObjectNode attributes = json.createObjectNode();
    attributes.put("payload", clean);
    attributes.put(
        "submittedAt",
        Instant.now()
            .atOffset(ZoneOffset.UTC)
            .truncatedTo(java.time.temporal.ChronoUnit.SECONDS)
            .toString());
    // Stable link used for filtering (draft/publish split the numeric id into rows).
    attributes.put("formDocumentId", form.documentId());
    if (form.formId() != null) {
      // Owning relation used by the CMS to display and traverse the linked form.
      attributes.put("form", form.formId());
    }
    ObjectNode createBody = json.createObjectNode();
    createBody.set("data", attributes);

    JsonNode created = writeSubmission("POST", "/api/form-submissions", createBody, session);
    String documentId = firstDocumentId(created);
    if (documentId.isBlank()) {
      throw new IllegalStateException("The form submission could not be stored.");
    }
    if (form.sendMailEnabled()) notifySubmission(form, clean, locale);
    return new SubmissionResult(documentId, form.successMessage());
  }

  /**
   * Sends one receipt email to the submitter for a stored submission. Failures never fail the
   * request.
   */
  private void notifySubmission(FormDefinition form, ObjectNode values, String locale) {
    String recipient = submitterEmail(form, values);
    if (recipient.isBlank()) {
      log.warn(
          "No mail was sent for form {}: no email address is set on the submission.", form.slug());
      return;
    }
    StringBuilder fallback = new StringBuilder();
    fallback.append("Thank you. We received your form submission.\n");
    fallback.append('\n');
    fallback.append("Form: ").append(form.name()).append('\n');
    Map<String, String> variables = new LinkedHashMap<>();
    variables.put("formName", form.name() == null ? "" : form.name());
    for (FormField field : form.fields()) {
      String text = fieldValue(values.get(field.key()));
      if ("checkbox".equals(field.type())) text = "true".equalsIgnoreCase(text) ? "Yes" : "No";
      fallback.append(field.label()).append(": ").append(text).append('\n');
      variables.put(field.key(), text);
    }
    EmailSettingsService.Rendered rendered =
        email.render(
            EmailSettingsService.Channel.FORM_SUBMISSION,
            form.slug(),
            locale,
            variables,
            fallback.toString().strip());
    if (rendered.html()) {
      mail.sendHtml(recipient, rendered.subject(), rendered.body());
    } else {
      mail.send(recipient, rendered.subject(), rendered.body());
    }
  }

  /** Returns the first valid email address entered on the submission. */
  private static String submitterEmail(FormDefinition form, ObjectNode values) {
    for (FormField field : form.fields()) {
      if ("email".equals(field.type())) {
        String value = fieldValue(values.get(field.key()));
        if (!value.isBlank() && EMAIL.matcher(value).matches()) return value;
      }
    }
    // Accept the first email-like value in any other field.
    for (FormField field : form.fields()) {
      String value = fieldValue(values.get(field.key()));
      if (EMAIL.matcher(value).matches()) return value;
    }
    return "";
  }

  private static String fieldValue(JsonNode value) {
    return value == null || value.isNull() ? "" : value.asText();
  }

  private static final int MAX_PAGE_SIZE = 100;

  /** Lists one page of stored submissions for one form, newest first. */
  public SubmissionPage submissions(String slug, int page, int pageSize, HttpSession session) {
    int safePage = Math.max(1, page);
    int safeSize = Math.min(Math.max(1, pageSize), MAX_PAGE_SIZE);
    FormDefinition form = find(slug, session);
    if (form.documentId() == null || form.documentId().isBlank())
      return new SubmissionPage(List.of(), safePage, safeSize, 0L);

    String path =
        SUBMISSION_MANAGER_PATH
            + "/?pagination[page]="
            + safePage
            + "&pagination[pageSize]="
            + safeSize
            + "&sort=submittedAt:DESC"
            + "&filters[formDocumentId]="
            + form.documentId();
    JsonNode response = admin.content(session, "GET", path, null);

    long total = response.path("pagination").path("total").asLong(0);
    if (total == 0) return new SubmissionPage(List.of(), safePage, safeSize, 0L);

    List<FormSubmission> rows = new ArrayList<>();
    for (JsonNode item : response.path("results")) {
      String id = firstDocumentId(item);
      JsonNode values = item.get("payload");
      rows.add(
          new FormSubmission(
              id,
              values == null || values.isNull() ? json.createObjectNode() : values,
              parseInstant(textOf(item, "submittedAt"))));
    }
    return new SubmissionPage(List.copyOf(rows), safePage, safeSize, total);
  }

  /** Deletes one stored submission. Returns false when the ID is blank or unknown. */
  public boolean deleteSubmission(String slug, String id, HttpSession session) {
    if (id == null || id.isBlank()) return false;
    FormDefinition form = find(slug, session);
    JsonNode submission;
    try {
      submission = admin.content(session, "GET", SUBMISSION_MANAGER_PATH + "/" + id, null);
    } catch (HttpClientErrorException.NotFound error) {
      return false;
    }
    String owner = textOf(submission, "formDocumentId");
    if (owner.isBlank()) owner = textOf(submission.path("data"), "formDocumentId");
    if (!form.documentId().equals(owner)) return false;
    try {
      admin.content(session, "DELETE", SUBMISSION_MANAGER_PATH + "/" + id, null);
    } catch (HttpClientErrorException.NotFound error) {
      return false;
    }
    return true;
  }

  /** Loads all stored submissions for one form, newest first. Used by CSV export. */
  public List<FormSubmission> allSubmissions(String slug, HttpSession session) {
    FormDefinition form = find(slug, session);
    if (form.documentId() == null || form.documentId().isBlank()) return List.of();
    int page = 1;
    final int pageSize = MAX_PAGE_SIZE;
    List<FormSubmission> rows = new ArrayList<>();
    while (true) {
      String path =
          SUBMISSION_MANAGER_PATH
              + "/?pagination[page]="
              + page
              + "&pagination[pageSize]="
              + pageSize
              + "&sort=submittedAt:DESC"
              + "&filters[formDocumentId]="
              + form.documentId();
      JsonNode response = admin.content(session, "GET", path, null);
      JsonNode results = response.path("results");
      if (!results.isArray() || results.size() == 0) break;
      for (JsonNode item : results) {
        String id = firstDocumentId(item);
        JsonNode values = item.get("payload");
        rows.add(
            new FormSubmission(
                id,
                values == null || values.isNull() ? json.createObjectNode() : values,
                parseInstant(textOf(item, "submittedAt"))));
      }
      long total = response.path("pagination").path("total").asLong(rows.size());
      if (rows.size() >= total) break;
      page++;
    }
    return List.copyOf(rows);
  }

  private static String firstDocumentId(JsonNode... nodes) {
    for (JsonNode node : nodes) {
      if (node == null || node.isNull()) continue;
      JsonNode id = node.get("documentId");
      if (id != null && id.isTextual() && !id.asText().isBlank()) return id.asText();
      JsonNode nested = node.path("data").path("documentId");
      if (nested.isTextual() && !nested.asText().isBlank()) return nested.asText();
    }
    return "";
  }

  private static String textOf(JsonNode item, String field) {
    if (item == null) return "";
    JsonNode value = item.get(field);
    return value != null && value.isTextual() ? value.asText() : "";
  }

  private static Instant parseInstant(String value) {
    if (value == null || value.isBlank()) return Instant.now();
    try {
      return Instant.from(java.time.OffsetDateTime.parse(value));
    } catch (Exception error) {
      return Instant.now();
    }
  }

  private Optional<FormDefinition> findOptional(String slug, HttpSession session) {
    JsonNode response =
        admin.content(
            session, "GET", CONTENT_MANAGER_PATH + "?page=1&pageSize=100&sort=name:ASC", null);
    for (JsonNode item : response.path("results"))
      if (slug.equals(item.path("slug").asText())) return Optional.of(formFrom(item, false));
    return Optional.empty();
  }

  private FormDefinition formFrom(JsonNode item, boolean publicForm) {
    List<FormField> fields = new ArrayList<>();
    if (item.path("fields").isArray())
      for (JsonNode field : item.path("fields"))
        fields.add(json.convertValue(field, FormField.class));
    String status = item.path("status").asText(publicForm ? "published" : "draft");
    Long formId =
        item.hasNonNull("id") && item.get("id").isIntegralNumber() ? item.get("id").asLong() : null;
    return new FormDefinition(
        item.path("documentId").asText(),
        formId,
        item.path("slug").asText(),
        item.path("name").asText(),
        item.path("enabled").asBoolean(true),
        item.path("sendMailEnabled").asBoolean(true),
        item.path("submitLabel").asText("Submit"),
        item.path("successMessage").asText("Thank you. Your form was submitted."),
        normalizeFields(fields),
        !"draft".equals(status));
  }

  private List<FormField> normalizeFields(List<FormField> fields) {
    if (fields == null || fields.isEmpty())
      throw new IllegalArgumentException("Add at least one field to the form.");
    if (fields.size() > MAX_FIELDS)
      throw new IllegalArgumentException("Use no more than " + MAX_FIELDS + " fields per form.");
    List<FormField> clean = new ArrayList<>();
    Set<String> keys = new HashSet<>();
    for (FormField field : fields) {
      String key = field.key() == null ? "" : field.key().strip().toLowerCase(Locale.ROOT);
      if (!KEY.matcher(key).matches())
        throw new IllegalArgumentException(
            "Field keys must start with a letter and contain only lowercase letters, numbers, or underscores.");
      if (!keys.add(key)) throw new IllegalArgumentException("Each field key must be unique.");
      String type = field.type() == null ? "" : field.type().strip().toLowerCase(Locale.ROOT);
      if (!FIELD_TYPES.contains(type))
        throw new IllegalArgumentException("This field type is not supported: " + type);
      int hardMaximum = "textarea".equals(type) ? 10_000 : 1_000;
      int minimum = field.minLength() == null ? 0 : Math.max(0, field.minLength());
      int maximum =
          field.maxLength() == null ? hardMaximum : Math.min(hardMaximum, field.maxLength());
      if (minimum > maximum)
        throw new IllegalArgumentException(
            "A field minimum length cannot exceed its maximum length.");
      List<String> options = normalizeOptions(type, field.options());
      clean.add(
          new FormField(
              key,
              required(field.label(), "Enter a label for each field.", 120),
              type,
              field.required(),
              limit(field.placeholder(), 200),
              limit(field.helpText(), 500),
              minimum,
              maximum,
              options));
    }
    return List.copyOf(clean);
  }

  private static List<String> normalizeOptions(String type, List<String> options) {
    if (!"select".equals(type)) return List.of();
    List<String> clean =
        (options == null ? List.<String>of() : options)
            .stream()
                .map(value -> limit(value, 120))
                .filter(value -> !value.isEmpty())
                .distinct()
                .limit(100)
                .toList();
    if (clean.isEmpty())
      throw new IllegalArgumentException("Add an option to each dropdown field.");
    return clean;
  }

  private static void validateValue(FormField field, String value) {
    if (value.length() < field.minLength())
      throw new IllegalArgumentException(
          field.label() + " must contain at least " + field.minLength() + " characters.");
    if (value.length() > field.maxLength())
      throw new IllegalArgumentException(
          field.label() + " must contain no more than " + field.maxLength() + " characters.");
    if ("email".equals(field.type()) && !EMAIL.matcher(value).matches())
      throw new IllegalArgumentException("Enter a valid email address for " + field.label() + ".");
    if ("select".equals(field.type()) && !field.options().contains(value))
      throw new IllegalArgumentException("Select a valid option for " + field.label() + ".");
  }

  private static String validateSlug(String slug) {
    if (slug == null || !KEY.matcher(slug).matches())
      throw new IllegalArgumentException(
          "The form slug must start with a letter and contain only lowercase letters, numbers, or underscores.");
    return slug;
  }

  private static String required(String value, String message, int maximum) {
    String clean = limit(value, maximum);
    if (clean.isEmpty()) throw new IllegalArgumentException(message);
    return clean;
  }

  private static String limit(String value, int maximum) {
    if (value == null) return "";
    String clean = value.strip().replace("\u0000", "");
    return clean.length() > maximum ? clean.substring(0, maximum) : clean;
  }

  public record FormSummary(
      String documentId,
      String slug,
      String name,
      boolean enabled,
      boolean published,
      int fieldCount) {}

  public record FormField(
      String key,
      String label,
      String type,
      boolean required,
      String placeholder,
      String helpText,
      Integer minLength,
      Integer maxLength,
      List<String> options) {}

  public record SaveForm(
      String name,
      boolean enabled,
      Boolean sendMailEnabled,
      String submitLabel,
      String successMessage,
      List<FormField> fields) {}

  public record FormDefinition(
      String documentId,
      Long formId,
      String slug,
      String name,
      boolean enabled,
      boolean sendMailEnabled,
      String submitLabel,
      String successMessage,
      List<FormField> fields,
      boolean published) {}

  public record SubmissionResult(String id, String message) {}

  public record FormSubmission(String id, JsonNode values, Instant createdAt) {}

  /** One page of submissions with the total count for the selected form. */
  public record SubmissionPage(List<FormSubmission> items, int page, int pageSize, long total) {}
}
