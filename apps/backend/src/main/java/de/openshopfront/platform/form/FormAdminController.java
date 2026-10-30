package de.openshopfront.platform.form;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpSession;
import java.time.Instant;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Provides administrator operations for reusable storefront forms. */
@RestController
@RequestMapping("/api/v1/admin/forms")
public class FormAdminController {
  private final FormService forms;

  public FormAdminController(FormService forms) {
    this.forms = forms;
  }

  @GetMapping
  public List<FormService.FormSummary> list(HttpSession session) {
    return forms.list(session);
  }

  @GetMapping("/{slug}")
  public FormService.FormDefinition detail(@PathVariable String slug, HttpSession session) {
    return forms.find(slug, session);
  }

  @PutMapping("/{slug}")
  public FormService.FormDefinition save(
      @PathVariable String slug, @RequestBody FormService.SaveForm request, HttpSession session) {
    return forms.save(slug, request, session);
  }

  @PostMapping("/{slug}/publish")
  public FormService.FormDefinition publish(@PathVariable String slug, HttpSession session) {
    return forms.publish(slug, session);
  }

  /** Returns one page of stored submissions for the selected form. */
  @GetMapping("/{slug}/submissions")
  public FormService.SubmissionPage submissions(
      @PathVariable String slug,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "25") int pageSize,
      HttpSession session) {
    return forms.submissions(slug, page, pageSize, session);
  }

  /** Exports all stored submissions of one form as a UTF-8 CSV file. */
  @GetMapping(value = "/{slug}/submissions.csv", produces = "text/csv;charset=UTF-8")
  public ResponseEntity<String> submissionsCsv(
      @PathVariable String slug,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant to,
      HttpSession session) {
    FormService.FormDefinition definition = forms.find(slug, session);
    List<FormService.FormSubmission> all = forms.allSubmissions(slug, session);
    StringBuilder csv = new StringBuilder("submission_id,submitted_at");
    for (FormService.FormField field : definition.fields()) {
      csv.append(',').append(escapeHeader(field.label()));
    }
    csv.append('\n');
    for (FormService.FormSubmission item : all) {
      if (from != null && item.createdAt().isBefore(from)) continue;
      if (to != null && item.createdAt().isAfter(to)) continue;
      csv.append(escapeCsv(item.id())).append(',').append(escapeCsv(item.createdAt().toString()));
      JsonNode values = item.values();
      for (FormService.FormField field : definition.fields()) {
        csv.append(',').append(escapeCsv(renderValue(values, field)));
      }
      csv.append('\n');
    }
    String filename = "form-" + slug + "-submissions.csv";
    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename(filename).build().toString())
        .body(csv.toString());
  }

  /** Deletes one stored submission after the administrator confirms it. */
  @DeleteMapping("/{slug}/submissions/{id}")
  public ResponseEntity<Void> deleteSubmission(
      @PathVariable String slug, @PathVariable String id, HttpSession session) {
    if (!forms.deleteSubmission(slug, id, session)) {
      throw new IllegalArgumentException("This form does not contain that submission.");
    }
    return ResponseEntity.noContent().build();
  }

  private static String renderValue(JsonNode values, FormService.FormField field) {
    JsonNode value = values == null ? null : values.get(field.key());
    if (value == null || value.isNull()) return "";
    if (value.isBoolean()) return value.asBoolean() ? "Yes" : "No";
    if (value.isArray()) {
      StringBuilder joined = new StringBuilder();
      for (JsonNode item : value) {
        if (joined.length() > 0) joined.append("; ");
        joined.append(item.isNumber() ? item.toString() : item.asText(""));
      }
      return joined.toString();
    }
    return value.asText("");
  }

  private static String escapeHeader(String label) {
    String clean = label == null ? "" : label.strip().replace("\n", " ");
    if (clean.isEmpty()) return "field";
    if (clean.contains(",") || clean.contains("\"")) {
      return '"' + clean.replace("\"", "\"\"") + '"';
    }
    char first = clean.charAt(0);
    if (first == '=' || first == '+' || first == '-' || first == '@') return "'" + clean;
    return clean;
  }

  private static String escapeCsv(String value) {
    String clean = value == null ? "" : value.strip();
    if (!clean.isEmpty()) {
      char first = clean.charAt(0);
      if (first == '=' || first == '+' || first == '-' || first == '@') clean = "'" + clean;
    }
    if (clean.contains(",") || clean.contains("\"") || clean.contains("\n")) {
      return '"' + clean.replace("\"", "\"\"") + '"';
    }
    return clean;
  }

  @ExceptionHandler(IllegalArgumentException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public java.util.Map<String, String> invalid(IllegalArgumentException error) {
    return java.util.Map.of("code", "VALIDATION_ERROR", "message", error.getMessage());
  }
}
