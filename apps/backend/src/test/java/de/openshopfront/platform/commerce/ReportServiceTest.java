package de.openshopfront.platform.commerce;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

/** Verifies report range parsing resolves to UTC day boundaries. */
class ReportServiceTest {
  private final ReportService service = new ReportService(null);

  private LocalDate start(String value) throws Exception {
    var method = ReportService.class.getDeclaredMethod("dayStart", String.class);
    method.setAccessible(true);
    return (LocalDate) method.invoke(service, value);
  }

  private LocalDate end(String value) throws Exception {
    var method = ReportService.class.getDeclaredMethod("dayEnd", String.class);
    method.setAccessible(true);
    return (LocalDate) method.invoke(service, value);
  }

  @Test
  void parsesIsoDayBounds() throws Exception {
    assertEquals(LocalDate.of(2026, 10, 20), start("2026-10-20"));
    assertEquals(LocalDate.of(2026, 10, 22), end("2026-10-22T23:59:59.999Z"));
  }

  @Test
  void parsesInstantBoundsToUtcDay() throws Exception {
    assertEquals(LocalDate.of(2026, 10, 21), start("2026-10-21T14:30:00Z"));
    assertEquals(LocalDate.of(2026, 10, 22), end("2026-10-22T05:00:00Z"));
  }

  @Test
  void emptyBoundsFallBackToSaneDefaults() throws Exception {
    LocalDate today = Instant.now().atZone(ZoneOffset.UTC).toLocalDate();
    assertEquals(today, end(""));
    assertEquals(LocalDate.of(1970, 1, 1), start(null));
  }
}
