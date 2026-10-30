package de.openshopfront.platform.booking;

import java.sql.Types;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Stores provider-neutral busy-time projections. This service is not a public API. */
@Service
public class ExternalBusyBlockService {
  private final NamedParameterJdbcTemplate db;

  public ExternalBusyBlockService(NamedParameterJdbcTemplate db) {
    this.db = db;
  }

  /** Creates or updates one external event projection under the resource lock. */
  @Transactional
  public void upsert(
      String provider,
      String resource,
      String calendarId,
      String eventId,
      Instant from,
      Instant to) {
    if (provider == null
        || provider.isBlank()
        || provider.length() > 64
        || resource == null
        || resource.isBlank()
        || calendarId == null
        || calendarId.isBlank()
        || eventId == null
        || eventId.isBlank()
        || from == null
        || to == null
        || !to.isAfter(from)) throw new IllegalArgumentException("Invalid busy event");
    db.queryForObject(
        "select pg_advisory_xact_lock(hashtextextended(:resource,0))",
        Map.of("resource", resource),
        (rs, n) -> 1);
    db.update(
        """
    insert into external_busy_block (id,provider,resource_id,external_calendar_id,external_event_id,starts_at,ends_at)
    values (:id,:provider,:resource,:calendar,:event,:from,:to)
    on conflict (provider,external_calendar_id,external_event_id)
   do update set resource_id=excluded.resource_id,starts_at=excluded.starts_at,ends_at=excluded.ends_at
    """,
        Map.of(
            "id",
            UUID.randomUUID(),
            "provider",
            provider,
            "resource",
            resource,
            "calendar",
            calendarId,
            "event",
            eventId,
            "from",
            timestamp(from),
            "to",
            timestamp(to)));
    // Future sync worker must report overlaps with existing bookings as a conflict requiring
    // operator action.
  }

  /** Converts a calendar instant to the JDBC type used by PostgreSQL timestamptz columns. */
  private static SqlParameterValue timestamp(Instant value) {
    return new SqlParameterValue(Types.TIMESTAMP_WITH_TIMEZONE, value.atOffset(ZoneOffset.UTC));
  }
}
