package de.openshopfront.platform.mail;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Verifies validation of language-keyed mail subjects. */
class EmailConfigControllerTest {
  private final EmailSettingsService settings = mock(EmailSettingsService.class);
  private final EmailConfigController controller =
      new EmailConfigController(settings, new ObjectMapper());

  @Test
  void saveKeepsDefaultAndLanguageSubjects() throws Exception {
    var request =
        new EmailConfigController.SettingsRequest(
            new EmailConfigController.ChannelPayload(
                Map.of("default", "Form received", "de", "Formular erhalten"), Map.of()),
            new EmailConfigController.ChannelPayload(
                Map.of("default", "Order confirmed", "de", "Bestellung bestätigt"), Map.of()));

    controller.save(request);

    verify(settings)
        .save(
            eq(Map.of("default", "Form received", "de", "Formular erhalten")),
            eq(Map.of()),
            eq(Map.of("default", "Order confirmed", "de", "Bestellung bestätigt")),
            eq(Map.of()));
  }

  @Test
  void saveRejectsInvalidSubjectLanguageKey() {
    var request =
        new EmailConfigController.SettingsRequest(
            new EmailConfigController.ChannelPayload(Map.of("en-US", "Form received"), Map.of()),
            new EmailConfigController.ChannelPayload(
                Map.of("default", "Order confirmed"), Map.of()));

    assertThatThrownBy(() -> controller.save(request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("The form subject language is not valid.");
  }
}
