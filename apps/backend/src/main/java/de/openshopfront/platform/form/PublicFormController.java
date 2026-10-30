package de.openshopfront.platform.form;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Renders published form definitions and accepts validated submissions. */
@RestController
@RequestMapping("/api/v1/public/forms")
public class PublicFormController {
  private final FormService forms;

  public PublicFormController(FormService forms) {
    this.forms = forms;
  }

  @GetMapping("/{slug}")
  public FormService.FormDefinition detail(@PathVariable String slug) {
    return forms.published(slug);
  }

  /** Accepts one anonymous submission. Strapi writes use the service token. */
  @PostMapping("/{slug}/submissions")
  public FormService.SubmissionResult submit(
      @PathVariable String slug, @RequestBody SubmissionRequest request) {
    return forms.submit(slug, request.values(), request.website(), request.locale(), null);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public Map<String, String> invalid(IllegalArgumentException error) {
    return Map.of("code", "VALIDATION_ERROR", "message", error.getMessage());
  }

  /** Carries one form submission with its values and optional storefront language. */
  public record SubmissionRequest(JsonNode values, String website, String locale) {}
}
