package de.ipli.platform.booking;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Internal projection API for future Google Calendar sync workers; do not expose publicly.
@Service
public class ExternalBusyBlockService {
 private final NamedParameterJdbcTemplate db;
 public ExternalBusyBlockService(NamedParameterJdbcTemplate db){this.db=db;}
 @Transactional
 public void upsert(String resource,String calendarId,String eventId,Instant from,Instant to){
  if(resource==null||resource.isBlank()||calendarId==null||calendarId.isBlank()||eventId==null||eventId.isBlank()||from==null||to==null||!to.isAfter(from))throw new IllegalArgumentException("Invalid busy event");
  db.queryForObject("select pg_advisory_xact_lock(hashtextextended(:resource,0))",Map.of("resource",resource),(rs,n)->{rs.getLong(1);return 1;});
  db.update("""
   insert into external_busy_block (id,resource_id,google_calendar_id,google_event_id,starts_at,ends_at)
   values (:id,:resource,:calendar,:event,:from,:to)
   on conflict (google_calendar_id,google_event_id)
   do update set resource_id=excluded.resource_id,starts_at=excluded.starts_at,ends_at=excluded.ends_at
   """,Map.of("id",UUID.randomUUID(),"resource",resource,"calendar",calendarId,"event",eventId,"from",from,"to",to));
  // Future sync worker must report overlaps with existing bookings as a conflict requiring operator action.
 }
}
