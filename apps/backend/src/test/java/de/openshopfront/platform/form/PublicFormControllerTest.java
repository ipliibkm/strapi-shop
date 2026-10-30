package de.openshopfront.platform.form;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import de.openshopfront.platform.PublicBookingSecurityConfiguration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Verifies anonymous access and response handling for storefront forms. */
@WebMvcTest(PublicFormController.class)
@Import(PublicBookingSecurityConfiguration.class)
class PublicFormControllerTest {
  @Autowired MockMvc mvc;
  @MockitoBean FormService forms;

  @Test
  void anonymousCustomerCanReadPublishedForm() throws Exception {
    when(forms.published("contact"))
        .thenReturn(
            new FormService.FormDefinition(
                "",
                null,
                "contact",
                "Contact form",
                true,
                true,
                "Send message",
                "Thank you. Your message was submitted.",
                List.of(
                    new FormService.FormField(
                        "email",
                        "Email address",
                        "email",
                        true,
                        "you@example.com",
                        "",
                        0,
                        254,
                        List.of())),
                true));

    mvc.perform(get("/api/v1/public/forms/contact"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.slug").value("contact"))
        .andExpect(jsonPath("$.fields[0].type").value("email"));
  }

  @Test
  void anonymousCustomerCanSubmitFormWithoutCsrf() throws Exception {
    when(forms.submit(
            eq("contact"),
            any(JsonNode.class),
            eq(""),
            org.mockito.ArgumentMatchers.isNull(),
            org.mockito.ArgumentMatchers.isNull()))
        .thenReturn(
            new FormService.SubmissionResult(
                "00000000-0000-0000-0000-000000000001", "Thank you. Your message was submitted."));

    mvc.perform(
            post("/api/v1/public/forms/contact/submissions")
                .contentType("application/json")
                .content(
                    """
                    {"values":{"email":"alex@example.com"},"website":""}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value("00000000-0000-0000-0000-000000000001"));
  }

  @Test
  void invalidSubmissionReturnsValidationError() throws Exception {
    when(forms.submit(
            eq("contact"),
            any(JsonNode.class),
            eq(""),
            org.mockito.ArgumentMatchers.isNull(),
            org.mockito.ArgumentMatchers.isNull()))
        .thenThrow(new IllegalArgumentException("Enter a valid email address."));

    mvc.perform(
            post("/api/v1/public/forms/contact/submissions")
                .contentType("application/json")
                .content(
                    """
                    {"values":{"email":"invalid"},"website":""}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.message").value("Enter a valid email address."));
  }
}
