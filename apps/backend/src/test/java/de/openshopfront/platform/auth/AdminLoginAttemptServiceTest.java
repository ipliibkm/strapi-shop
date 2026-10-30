package de.openshopfront.platform.auth;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class AdminLoginAttemptServiceTest {
  @Test
  void blocksAfterFiveFailedCredentials() {
    var attempts =
        new AdminLoginAttemptService(
            Clock.fixed(Instant.parse("2026-10-21T12:00:00Z"), ZoneOffset.UTC));
    for (int count = 0; count < 5; count++) attempts.failed("admin@example.com", "127.0.0.1");

    assertThrows(
        StrapiAdminClient.TooManyLoginAttemptsException.class,
        () -> attempts.check("admin@example.com", "127.0.0.1"));
  }

  @Test
  void successfulLoginClearsFailures() {
    var attempts =
        new AdminLoginAttemptService(
            Clock.fixed(Instant.parse("2026-10-21T12:00:00Z"), ZoneOffset.UTC));
    for (int count = 0; count < 5; count++) attempts.failed("admin@example.com", "127.0.0.1");

    attempts.succeeded("admin@example.com", "127.0.0.1");

    assertDoesNotThrow(() -> attempts.check("admin@example.com", "127.0.0.1"));
  }
}
