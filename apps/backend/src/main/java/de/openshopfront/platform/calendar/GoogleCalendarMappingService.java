package de.openshopfront.platform.calendar;

import java.sql.Types;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Stores resource-to-calendar mappings and applies synchronized event changes. */
@Service
@ConditionalOnProperty(name = "platform.google.enabled", havingValue = "true")
public class GoogleCalendarMappingService {
  private final NamedParameterJdbcTemplate db;

  public GoogleCalendarMappingService(NamedParameterJdbcTemplate db) {
    this.db = db;
  }

  /** Returns all mappings owned by one connected Google account. */
  @Transactional(readOnly = true)
  public List<CalendarMapping> list(String principalName) {
    return db.query(
        """
   select resource_id,principal_name,google_calendar_id,display_name,write_bookings,sync_token,sync_status,
          last_synced_at,conflict_count,last_error,version
     from google_calendar_mapping where principal_name=:principal order by resource_id
   """,
        Map.of("principal", principalName),
        (rs, row) -> mapping(rs));
  }

  /** Returns all mappings that the scheduled worker must synchronize. */
  @Transactional(readOnly = true)
  public List<CalendarMapping> listAll() {
    return db.query(
        """
  select resource_id,principal_name,google_calendar_id,display_name,write_bookings,sync_token,sync_status,
         last_synced_at,conflict_count,last_error,version from google_calendar_mapping order by resource_id
  """,
        Map.of(),
        (rs, row) -> mapping(rs));
  }

  /** Creates or replaces one resource mapping and requests a full synchronization. */
  @Transactional
  public CalendarMapping save(
      String principalName,
      String resourceId,
      String calendarId,
      String displayName,
      boolean writeBookings) {
    validate(resourceId, calendarId, displayName);
    lock(resourceId);
    List<CalendarMapping> existing =
        db.query(
            """
   select resource_id,principal_name,google_calendar_id,display_name,write_bookings,sync_token,sync_status,
          last_synced_at,conflict_count,last_error,version from google_calendar_mapping where resource_id=:resource
   """,
            Map.of("resource", resourceId),
            (rs, row) -> mapping(rs));
    if (!existing.isEmpty()) {
      CalendarMapping current = existing.getFirst();
      if (!current.principalName().equals(principalName))
        throw new MappingConflictException(
            "This resource belongs to a different Google connection.");
      if (!current.calendarId().equals(calendarId)) {
        Integer delivered =
            db.queryForObject(
                "select count(*) from google_booking_event where principal_name=:principal and google_calendar_id=:calendar and google_event_id is not null",
                Map.of("principal", principalName, "calendar", current.calendarId()),
                Integer.class);
        if (delivered != null && delivered > 0)
          throw new MappingConflictException(
              "Remove this mapping and synchronize deletions before you select a different calendar.");
        db.update(
            "delete from google_booking_event where principal_name=:principal and google_calendar_id=:calendar",
            Map.of("principal", principalName, "calendar", current.calendarId()));
      }
      if (current.writeBookings() && !writeBookings)
        queueDeletes(principalName, current.calendarId());
    }
    db.update(
        """
   insert into google_calendar_mapping
    (resource_id,client_registration_id,principal_name,google_calendar_id,display_name,write_bookings,sync_status,updated_at)
   values (:resource,'google',:principal,:calendar,:name,:write,'PENDING',now())
   on conflict (resource_id) do update set principal_name=excluded.principal_name,
    google_calendar_id=excluded.google_calendar_id,display_name=excluded.display_name,write_bookings=excluded.write_bookings,
    sync_token=null,sync_status='PENDING',last_synced_at=null,conflict_count=0,last_error=null,version=google_calendar_mapping.version+1,updated_at=now()
   """,
        Map.of(
            "resource",
            resourceId,
            "principal",
            principalName,
            "calendar",
            calendarId,
            "name",
            displayName,
            "write",
            writeBookings));
    if (writeBookings) enqueueExisting(principalName, resourceId, calendarId);
    return list(principalName).stream()
        .filter(item -> item.resourceId().equals(resourceId))
        .findFirst()
        .orElseThrow();
  }

  /** Removes one owned mapping and its imported availability projection. */
  @Transactional
  public void remove(String principalName, String resourceId) {
    lock(resourceId);
    db.update(
        """
   update google_booking_event g set operation='DELETE',delivery_status='PENDING',attempt_count=0,generation=generation+1,
    next_attempt_at=now(),last_error=null,updated_at=now() where google_event_id is not null and exists
    (select 1 from google_calendar_mapping m where m.resource_id=:resource and m.principal_name=:principal
     and m.principal_name=g.principal_name and m.google_calendar_id=g.google_calendar_id)
   """,
        Map.of("resource", resourceId, "principal", principalName));
    db.update(
        "delete from google_booking_event g where google_event_id is null and exists (select 1 from google_calendar_mapping m where m.resource_id=:resource and m.principal_name=:principal and m.principal_name=g.principal_name and m.google_calendar_id=g.google_calendar_id)",
        Map.of("resource", resourceId, "principal", principalName));
    db.update(
        "delete from google_calendar_mapping where resource_id=:resource and principal_name=:principal",
        Map.of("resource", resourceId, "principal", principalName));
  }

  /** Removes projections and mappings before local Google authorization is deleted. */
  @Transactional
  public void removeAll(String principalName) {
    db.update(
        "delete from google_booking_event where principal_name=:principal",
        Map.of("principal", principalName));
    db.update(
        "delete from external_busy_block where provider='google' and resource_id in (select resource_id from google_calendar_mapping where principal_name=:principal)",
        Map.of("principal", principalName));
    db.update(
        "delete from google_calendar_mapping where principal_name=:principal",
        Map.of("principal", principalName));
  }

  /** Applies a complete event change set and commits its next synchronization token. */
  @Transactional
  public SyncResult apply(
      CalendarMapping mapping,
      List<GoogleCalendarApi.CalendarEvent> events,
      String nextSyncToken,
      boolean reset) {
    lock(mapping.resourceId());
    Integer current =
        db.queryForObject(
            "select count(*) from google_calendar_mapping where resource_id=:resource and principal_name=:principal and google_calendar_id=:calendar and version=:version",
            Map.of(
                "resource",
                mapping.resourceId(),
                "principal",
                mapping.principalName(),
                "calendar",
                mapping.calendarId(),
                "version",
                mapping.version()),
            Integer.class);
    if (current == null || current == 0) throw new StaleMappingException();
    if (reset)
      db.update(
          "delete from external_busy_block where provider='google' and resource_id=:resource",
          Map.of("resource", mapping.resourceId()));
    for (GoogleCalendarApi.CalendarEvent event : events) {
      Map<String, Object> identity = Map.of("calendar", mapping.calendarId(), "event", event.id());
      if (event.deleted()
          || event.transparent()
          || event.managed()
          || event.startsAt() == null
          || event.endsAt() == null
          || !event.endsAt().isAfter(event.startsAt())) {
        db.update(
            "delete from external_busy_block where provider='google' and external_calendar_id=:calendar and external_event_id=:event",
            identity);
        continue;
      }
      var values = new java.util.HashMap<String, Object>(identity);
      values.put("id", java.util.UUID.randomUUID());
      values.put("resource", mapping.resourceId());
      values.put("start", timestamp(event.startsAt()));
      values.put("end", timestamp(event.endsAt()));
      db.update(
          """
     insert into external_busy_block (id,provider,resource_id,external_calendar_id,external_event_id,starts_at,ends_at)
     values (:id,'google',:resource,:calendar,:event,:start,:end)
     on conflict (provider,external_calendar_id,external_event_id) do update set
     resource_id=excluded.resource_id,starts_at=excluded.starts_at,ends_at=excluded.ends_at
    """,
          values);
    }
    Integer conflicts =
        db.queryForObject(
            """
   select count(*) from external_busy_block e join booking b on b.resource_id=e.resource_id and b.status='CONFIRMED'
     and b.starts_at<e.ends_at and b.ends_at>e.starts_at where e.resource_id=:resource and e.provider='google' and e.external_calendar_id=:calendar
   """,
            params(mapping),
            Integer.class);
    int count = conflicts == null ? 0 : conflicts;
    String status = count == 0 ? "SYNCED" : "CONFLICT";
    db.update(
        """
   update google_calendar_mapping set sync_token=:token,sync_status=:status,last_synced_at=now(),conflict_count=:conflicts,
    last_error=:error,updated_at=now() where resource_id=:resource and principal_name=:principal and google_calendar_id=:calendar and version=:version
   """,
        Map.of(
            "token",
            nextSyncToken,
            "status",
            status,
            "conflicts",
            count,
            "error",
            count == 0 ? "" : "Imported events overlap confirmed bookings.",
            "resource",
            mapping.resourceId(),
            "principal",
            mapping.principalName(),
            "calendar",
            mapping.calendarId(),
            "version",
            mapping.version()));
    return new SyncResult(count, status);
  }

  /** Records a synchronization error without removing the last valid projection. */
  public void failed(CalendarMapping mapping, String message) {
    db.update(
        "update google_calendar_mapping set sync_status='ERROR',last_error=:error,updated_at=now() where resource_id=:resource and principal_name=:principal",
        Map.of(
            "error",
            limit(message),
            "resource",
            mapping.resourceId(),
            "principal",
            mapping.principalName()));
  }

  /** Clears an invalid incremental token before a replacement full sync. */
  public CalendarMapping reset(CalendarMapping mapping) {
    db.update(
        "update google_calendar_mapping set sync_token=null,sync_status='PENDING',updated_at=now() where resource_id=:resource and principal_name=:principal",
        Map.of("resource", mapping.resourceId(), "principal", mapping.principalName()));
    return new CalendarMapping(
        mapping.resourceId(),
        mapping.principalName(),
        mapping.calendarId(),
        mapping.displayName(),
        mapping.writeBookings(),
        null,
        "PENDING",
        mapping.lastSyncedAt(),
        mapping.conflictCount(),
        null,
        mapping.version());
  }

  private static CalendarMapping mapping(java.sql.ResultSet rs) throws java.sql.SQLException {
    OffsetDateTime synced = rs.getObject("last_synced_at", OffsetDateTime.class);
    return new CalendarMapping(
        rs.getString("resource_id"),
        rs.getString("principal_name"),
        rs.getString("google_calendar_id"),
        rs.getString("display_name"),
        rs.getBoolean("write_bookings"),
        rs.getString("sync_token"),
        rs.getString("sync_status"),
        synced == null ? null : synced.toInstant(),
        rs.getInt("conflict_count"),
        rs.getString("last_error"),
        rs.getLong("version"));
  }

  private static Map<String, Object> params(CalendarMapping mapping) {
    return Map.of("resource", mapping.resourceId(), "calendar", mapping.calendarId());
  }

  private static SqlParameterValue timestamp(Instant value) {
    return new SqlParameterValue(Types.TIMESTAMP_WITH_TIMEZONE, value.atOffset(ZoneOffset.UTC));
  }

  private static void validate(String resource, String calendar, String name) {
    if (resource == null
        || resource.isBlank()
        || resource.length() > 128
        || calendar == null
        || calendar.isBlank()
        || calendar.length() > 512
        || name == null
        || name.isBlank()
        || name.length() > 512)
      throw new IllegalArgumentException("The calendar mapping is not valid.");
  }

  private static String limit(String value) {
    String result = value == null || value.isBlank() ? "Calendar synchronization failed." : value;
    return result.substring(0, Math.min(result.length(), 2000));
  }

  private void lock(String resourceId) {
    db.queryForObject(
        "select pg_advisory_xact_lock(hashtextextended(:resource,0))",
        Map.of("resource", resourceId),
        (rs, row) -> 1);
  }

  private void queueDeletes(String principalName, String calendarId) {
    db.update(
        """
  update google_booking_event set operation='DELETE',delivery_status='PENDING',attempt_count=0,generation=generation+1,
   next_attempt_at=now(),last_error=null,updated_at=now() where principal_name=:principal and google_calendar_id=:calendar and google_event_id is not null
  """,
        Map.of("principal", principalName, "calendar", calendarId));
  }

  private void enqueueExisting(String principalName, String resourceId, String calendarId) {
    db.update(
        """
  insert into google_booking_event (booking_id,principal_name,google_calendar_id,operation,delivery_status,next_attempt_at,updated_at)
  select id,:principal,:calendar,'UPSERT','PENDING',now(),now() from booking where resource_id=:resource and status='CONFIRMED'
  on conflict (booking_id) do update set principal_name=excluded.principal_name,google_calendar_id=excluded.google_calendar_id,
   operation='UPSERT',delivery_status='PENDING',attempt_count=0,generation=google_booking_event.generation+1,
   next_attempt_at=now(),last_error=null,updated_at=now()
  where google_booking_event.google_calendar_id=excluded.google_calendar_id
  """,
        Map.of("principal", principalName, "calendar", calendarId, "resource", resourceId));
  }

  public record CalendarMapping(
      String resourceId,
      @com.fasterxml.jackson.annotation.JsonIgnore String principalName,
      String calendarId,
      String displayName,
      boolean writeBookings,
      String syncToken,
      String syncStatus,
      Instant lastSyncedAt,
      int conflictCount,
      String lastError,
      long version) {}

  public record SyncResult(int conflictCount, String status) {}

  public static class MappingConflictException extends RuntimeException {
    public MappingConflictException(String message) {
      super(message);
    }
  }

  public static class StaleMappingException extends RuntimeException {}
}
