package de.openshopfront.platform.setup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

class SetupVerificationControllerTest {
  @Test
  void setupChecksStopAfterInstallation() {
    NamedParameterJdbcTemplate db = mock(NamedParameterJdbcTemplate.class);
    when(db.queryForObject(anyString(), anyMap(), eq(Boolean.class))).thenReturn(true);
    var subject = new SetupVerificationController(db, "", "", "configured-key", true);

    ResponseStatusException error =
        assertThrows(ResponseStatusException.class, subject::setupChecks);

    assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
  }

  @Test
  void setupChecksRemainAvailableBeforeInstallation() {
    NamedParameterJdbcTemplate db = mock(NamedParameterJdbcTemplate.class);
    when(db.queryForObject(anyString(), anyMap(), eq(Boolean.class))).thenReturn(false);
    var subject = new SetupVerificationController(db, "", "", "configured-key", true);

    assertEquals("pending", subject.setupChecks().get(1).status());
  }

  @Test
  void setupChecksAreUnavailableWhenSetupIsDisabled() {
    var subject =
        new SetupVerificationController(
            mock(NamedParameterJdbcTemplate.class), "", "", "configured-key", false);

    ResponseStatusException error =
        assertThrows(ResponseStatusException.class, subject::setupChecks);

    assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
  }
}
