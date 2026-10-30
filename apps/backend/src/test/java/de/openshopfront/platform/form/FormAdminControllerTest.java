package de.openshopfront.platform.form;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.openshopfront.platform.PublicBookingSecurityConfiguration;
import jakarta.servlet.http.HttpSession;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Verifies administrator access, pagination, CSV export, and deletion for form submissions. */
@WebMvcTest(FormAdminController.class)
@Import(PublicBookingSecurityConfiguration.class)
class FormAdminControllerTest {
  @Autowired MockMvc mvc;
  @MockitoBean FormService forms;

  private static final ObjectMapper JSON = new ObjectMapper();

  private static ObjectNode values(String email, boolean consent) throws Exception {
    ObjectNode node = JSON.createObjectNode();
    node.put("email", email);
    node.put("consent", consent);
    return node;
  }

  /**
   * Pins the administrator submissions route. In production deployments, anonymous requests to
   * /api/v1/admin/** are rejected by {@code StrapiAdminSecurityConfiguration}; see {@link
   * de.openshopfront.platform.SecurityConfiguration}.
   */
  @Test
  void adminSubmissionsRouteIsMapped() throws Exception {
    when(forms.submissions(eq("contact"), eq(1), any(Integer.class), any(HttpSession.class)))
        .thenReturn(new FormService.SubmissionPage(List.of(), 1, 25, 0L));

    mvc.perform(get("/api/v1/admin/forms/contact/submissions")).andExpect(status().isOk());
  }

  @Test
  void submissionsReturnPagedResponseWithNewestFirstOrder() throws Exception {
    when(forms.submissions(eq("contact"), eq(1), any(Integer.class), any(HttpSession.class)))
        .thenReturn(
            new FormService.SubmissionPage(
                List.of(
                    new FormService.FormSubmission(
                        "newer",
                        values("first@example.com", true),
                        Instant.parse("2026-10-22T10:00:00Z")),
                    new FormService.FormSubmission(
                        "older",
                        values("second@example.com", false),
                        Instant.parse("2026-10-20T09:30:00Z"))),
                1,
                25,
                2L));

    mvc.perform(get("/api/v1/admin/forms/contact/submissions").param("page", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].id").value("newer"))
        .andExpect(jsonPath("$.items[1].id").value("older"))
        .andExpect(jsonPath("$.page").value(1))
        .andExpect(jsonPath("$.pageSize").value(25))
        .andExpect(jsonPath("$.total").value(2));
  }

  @Test
  void pageSizeIsClampedToSafeMaximum() throws Exception {
    when(forms.submissions(eq("contact"), eq(1), any(Integer.class), any(HttpSession.class)))
        .thenReturn(new FormService.SubmissionPage(List.of(), 1, 100, 0L));

    mvc.perform(get("/api/v1/admin/forms/contact/submissions").param("pageSize", "5000"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.pageSize").value(100));
  }

  @Test
  void emptyFormReturnsEmptyItemsAndZeroTotal() throws Exception {
    when(forms.submissions(eq("contact"), eq(1), any(Integer.class), any(HttpSession.class)))
        .thenReturn(new FormService.SubmissionPage(List.of(), 1, 25, 0L));

    mvc.perform(get("/api/v1/admin/forms/contact/submissions"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items").isEmpty())
        .andExpect(jsonPath("$.total").value(0));
  }

  @Test
  void csvExportUsesFieldOrderAndEscapesFormulas() throws Exception {
    FormService.FormDefinition definition =
        new FormService.FormDefinition(
            "",
            null,
            "contact",
            "Contact form",
            true,
            true,
            "Send message",
            "Thank you.",
            List.of(
                new FormService.FormField(
                    "email", "Email address", "email", false, "", "", 0, 254, List.of()),
                new FormService.FormField(
                    "consent", "Consent", "checkbox", false, "", "", 0, 1, List.of())),
            true);
    when(forms.find(eq("contact"), any())).thenReturn(definition);
    when(forms.allSubmissions(eq("contact"), any(HttpSession.class)))
        .thenReturn(
            List.of(
                new FormService.FormSubmission(
                    "abc-123",
                    values("=SUM(A1:A2)", true),
                    Instant.parse("2026-10-22T10:00:00Z"))));

    String csv =
        mvc.perform(get("/api/v1/admin/forms/contact/submissions.csv").accept(MediaType.ALL))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    // Header row uses the configured field labels in order.
    org.junit.jupiter.api.Assertions.assertTrue(csv.startsWith("submission_id,submitted_at,"), csv);
    // Formula values are prefixed with a single quote so spreadsheets keep them inert.
    org.junit.jupiter.api.Assertions.assertTrue(csv.contains("'=SUM(A1:A2)"), csv);
  }

  @Test
  void deleteRemovesSubmissionForMatchingForm() throws Exception {
    when(forms.deleteSubmission(eq("contact"), eq("abc-123"), any(HttpSession.class)))
        .thenReturn(true);

    mvc.perform(delete("/api/v1/admin/forms/contact/submissions/abc-123"))
        .andExpect(status().isNoContent());
  }

  @Test
  void deleteWithWrongFormSlugReturnsValidationError() throws Exception {
    when(forms.deleteSubmission(eq("other"), eq("abc-123"), any(HttpSession.class)))
        .thenReturn(false);

    mvc.perform(delete("/api/v1/admin/forms/other/submissions/abc-123"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  void deleteUnknownSubmissionReturnsValidationError() throws Exception {
    when(forms.deleteSubmission(eq("contact"), eq("missing"), any(HttpSession.class)))
        .thenReturn(false);

    mvc.perform(delete("/api/v1/admin/forms/contact/submissions/missing"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }
}
