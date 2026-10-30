package de.openshopfront.platform.form;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.openshopfront.platform.auth.StrapiAdminClient;
import de.openshopfront.platform.mail.EmailSettingsService;
import de.openshopfront.platform.mail.MailService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

/** Verifies submission ownership checks in the form service. */
class FormServiceTest {
  private static final String SUBMISSIONS =
      "/content-manager/collection-types/api::form-submission.form-submission";
  private final ObjectMapper json = new ObjectMapper();
  private final StrapiAdminClient admin = mock(StrapiAdminClient.class);
  private final HttpSession session = mock(HttpSession.class);
  private final FormService forms =
      new FormService(
          admin,
          json,
          "http://localhost:1337",
          "service-token",
          mock(MailService.class),
          mock(EmailSettingsService.class));

  @Test
  void deleteRejectsSubmissionOwnedByAnotherForm() {
    stubForm("contact", "form-contact");
    ObjectNode submission = json.createObjectNode().put("formDocumentId", "form-other");
    when(admin.content(session, "GET", SUBMISSIONS + "/submission-1", null)).thenReturn(submission);

    assertThat(forms.deleteSubmission("contact", "submission-1", session)).isFalse();

    verify(admin, never()).content(session, "DELETE", SUBMISSIONS + "/submission-1", null);
  }

  @Test
  void deleteReturnsFalseForUnknownSubmission() {
    stubForm("contact", "form-contact");
    when(admin.content(session, "GET", SUBMISSIONS + "/missing", null))
        .thenThrow(
            HttpClientErrorException.create(
                HttpStatus.NOT_FOUND, "Not Found", HttpHeaders.EMPTY, null, null));

    assertThat(forms.deleteSubmission("contact", "missing", session)).isFalse();

    verify(admin, never()).content(session, "DELETE", SUBMISSIONS + "/missing", null);
  }

  @Test
  void deleteRemovesSubmissionOwnedBySelectedForm() {
    stubForm("contact", "form-contact");
    ObjectNode submission = json.createObjectNode().put("formDocumentId", "form-contact");
    when(admin.content(session, "GET", SUBMISSIONS + "/submission-1", null)).thenReturn(submission);

    assertThat(forms.deleteSubmission("contact", "submission-1", session)).isTrue();

    verify(admin).content(session, "DELETE", SUBMISSIONS + "/submission-1", null);
  }

  private void stubForm(String slug, String documentId) {
    ObjectNode form =
        json.createObjectNode()
            .put("slug", slug)
            .put("documentId", documentId)
            .put("name", "Contact")
            .put("enabled", true)
            .put("status", "published");
    form.putArray("fields")
        .addObject()
        .put("key", "email")
        .put("label", "Email")
        .put("type", "email")
        .put("required", false)
        .put("minLength", 0)
        .put("maxLength", 254)
        .putArray("options");
    ObjectNode response = json.createObjectNode();
    response.putArray("results").add(form);
    when(admin.content(
            eq(session),
            eq("GET"),
            eq(
                "/content-manager/collection-types/api::storefront-form.storefront-form?page=1&pageSize=100&sort=name:ASC"),
            any()))
        .thenReturn(response);
  }
}
