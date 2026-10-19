package de.ipli.platform.booking;

import java.time.Instant;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingService {
 private final NamedParameterJdbcTemplate db;
 public BookingService(NamedParameterJdbcTemplate db) { this.db=db; }
 private static void validate(String resource, Instant from, Instant to) {
  if(resource==null || resource.isBlank() || resource.length()>128 || from==null || to==null || !to.isAfter(from) || Duration.between(from,to).toDays()>31) throw new IllegalArgumentException("Invalid resource or time interval");
 }
 private Map<String,Object> args(String resource, Instant from, Instant to) {return Map.of("resource",resource,"start",from,"end",to);}
 // Exclusive end, inclusive start: [start,end)
 private static final String COLLISION = """
  select exists (
   select 1 from booking where resource_id=:resource and status='CONFIRMED'
      and starts_at<:end and ends_at>:start
   union all
   select 1 from external_busy_block where resource_id=:resource
      and starts_at<:end and ends_at>:start
  )
  """;
 @Transactional(readOnly=true)
 public boolean isBusy(String resource, Instant from, Instant to) {
  validate(resource,from,to);
  return Boolean.TRUE.equals(db.queryForObject(COLLISION,args(resource,from,to),Boolean.class));
 }
 // Serializes all booking attempts for one resource through one transaction-scoped pg advisory lock.
 // Google sync import must acquire this SAME lock before writing busy blocks.
 @Transactional
 public Booking create(String resource, Instant from, Instant to) {
  validate(resource,from,to);
  if(!from.isAfter(Instant.now())) throw new IllegalArgumentException("Booking must be in the future");
  db.queryForObject("select pg_advisory_xact_lock(hashtextextended(:resource,0))",Map.of("resource",resource),(rs,n)->{rs.getLong(1);return 1;});
  if(isBusy(resource,from,to)) throw new AvailabilityConflictException("This slot is unavailable; refresh availability and select another slot");
  UUID id=UUID.randomUUID();
  db.update("insert into booking (id, resource_id, starts_at, ends_at, status, created_at) values (:id,:resource,:start,:end,'CONFIRMED',now())",
    Map.of("id",id,"resource",resource,"start",from,"end",to));
  return new Booking(id,resource,from,to,"CONFIRMED");
 }
 @Transactional(readOnly=true)
 public List<Booking> list(String resource,Instant from,Instant to) {
  validate(resource,from,to);
  return db.query("select id,resource_id,starts_at,ends_at,status from booking where resource_id=:resource and starts_at<:end and ends_at>:start order by starts_at",
    args(resource,from,to),(rs,n)->new Booking(rs.getObject("id",UUID.class),rs.getString("resource_id"),rs.getTimestamp("starts_at").toInstant(),rs.getTimestamp("ends_at").toInstant(),rs.getString("status")));
 }
 public record Booking(UUID id,String resourceId,Instant startsAt,Instant endsAt,String status) {}
 public static class AvailabilityConflictException extends RuntimeException { public AvailabilityConflictException(String message){super(message);} }
}
