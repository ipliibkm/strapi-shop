package de.openshopfront.platform.booking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Types;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/** Verifies JDBC parameter conversion for booking time ranges. */
@ExtendWith(MockitoExtension.class)
class BookingServiceTest {
  @Mock NamedParameterJdbcTemplate db;
  @Mock BookingCalendarCoordinator calendars;

  @Test
  void availabilityUsesPostgresTimestampWithTimeZoneParameters() {
    Instant from = Instant.parse("2026-12-01T10:00:00Z");
    Instant to = Instant.parse("2026-12-01T11:00:00Z");
    when(db.queryForObject(
            anyString(), org.mockito.ArgumentMatchers.<Map<String, ?>>any(), eq(Boolean.class)))
        .thenReturn(false);

    new BookingService(db, calendars).isBusy("room-1", from, to);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<String, ?>> parameters = ArgumentCaptor.forClass(Map.class);
    verify(db).queryForObject(anyString(), parameters.capture(), eq(Boolean.class));
    assertTimestamp(parameters.getValue().get("start"), from);
    assertTimestamp(parameters.getValue().get("end"), to);
  }

  private static void assertTimestamp(Object parameter, Instant expected) {
    SqlParameterValue value = assertInstanceOf(SqlParameterValue.class, parameter);
    assertEquals(Types.TIMESTAMP_WITH_TIMEZONE, value.getSqlType());
    assertEquals(expected, assertInstanceOf(OffsetDateTime.class, value.getValue()).toInstant());
  }
}
