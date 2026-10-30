package de.openshopfront.platform.booking;

import java.sql.Types;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingService {
  private final NamedParameterJdbcTemplate db;
  private final BookingCalendarCoordinator calendars;

  public BookingService(NamedParameterJdbcTemplate db, BookingCalendarCoordinator calendars) {
    this.db = db;
    this.calendars = calendars;
  }

  private static void validate(String resource, Instant from, Instant to) {
    if (resource == null
        || resource.isBlank()
        || resource.length() > 128
        || from == null
        || to == null
        || !to.isAfter(from)
        || Duration.between(from, to).toDays() > 31)
      throw new IllegalArgumentException("Invalid resource or time interval");
  }

  private Map<String, Object> args(String resource, Instant from, Instant to) {
    return Map.of("resource", resource, "start", timestamp(from), "end", timestamp(to));
  }

  /** Converts an API instant to the JDBC type used by PostgreSQL timestamptz columns. */
  private static SqlParameterValue timestamp(Instant value) {
    return new SqlParameterValue(Types.TIMESTAMP_WITH_TIMEZONE, value.atOffset(ZoneOffset.UTC));
  }

  // Exclusive end, inclusive start: [start,end)
  private static final String COLLISION =
      """
  select exists (
   select 1 from booking where resource_id=:resource and status='CONFIRMED'
      and starts_at<:end and ends_at>:start
   union all
   select 1 from external_busy_block where resource_id=:resource
      and starts_at<:end and ends_at>:start
  )
  """;

  @Transactional(readOnly = true)
  public boolean isBusy(String resource, Instant from, Instant to) {
    validate(resource, from, to);
    return Boolean.TRUE.equals(
        db.queryForObject(COLLISION, args(resource, from, to), Boolean.class));
  }

  // Serializes all booking attempts for one resource through one transaction-scoped pg advisory
  // lock.
  // Calendar imports must acquire this SAME lock before writing busy blocks.
  @Transactional
  public Booking create(String resource, Instant from, Instant to) {
    return create(resource, from, to, null, null, null, null, null);
  }

  /** Creates a confirmed booking with optional customer contact details. */
  @Transactional
  public Booking create(
      String resource, Instant from, Instant to, String customerName, String customerEmail) {
    return create(resource, from, to, null, null, null, customerName, customerEmail);
  }

  /** Creates a customer booking and derives its end from the selected service duration. */
  @Transactional
  public Booking create(
      String resource,
      Instant from,
      BookingOfferingService.Offering offering,
      String customerName,
      String customerEmail) {
    return create(
        resource,
        from,
        from.plusSeconds(offering.durationMinutes() * 60L),
        offering.id(),
        offering.name(),
        offering.durationMinutes(),
        customerName,
        customerEmail);
  }

  private Booking create(
      String resource,
      Instant from,
      Instant to,
      UUID serviceId,
      String serviceName,
      Integer durationMinutes,
      String customerName,
      String customerEmail) {
    validate(resource, from, to);
    if (!from.isAfter(Instant.now()))
      throw new IllegalArgumentException("Booking must be in the future");
    String name = clean(customerName, 200), email = clean(customerEmail, 254);
    if ((name == null) != (email == null))
      throw new IllegalArgumentException("Enter both the customer name and email address.");
    db.queryForObject(
        "select pg_advisory_xact_lock(hashtextextended(:resource,0))",
        Map.of("resource", resource),
        (rs, n) -> 1);
    if (isBusy(resource, from, to))
      throw new AvailabilityConflictException(
          "This slot is unavailable; refresh availability and select another slot");
    UUID id = UUID.randomUUID();
    Map<String, Object> values = new java.util.HashMap<>(args(resource, from, to));
    values.put("id", id);
    values.put("name", new SqlParameterValue(Types.VARCHAR, name));
    values.put("email", new SqlParameterValue(Types.VARCHAR, email));
    values.put("serviceId", new SqlParameterValue(Types.OTHER, serviceId));
    values.put("serviceName", new SqlParameterValue(Types.VARCHAR, serviceName));
    values.put("duration", new SqlParameterValue(Types.INTEGER, durationMinutes));
    db.update(
        "insert into booking (id, resource_id, starts_at, ends_at, status, service_id, service_name, duration_minutes, customer_name, customer_email, created_at) values (:id,:resource,:start,:end,'CONFIRMED',:serviceId,:serviceName,:duration,:name,:email,now())",
        values);
    calendars.bookingCreated(id, resource);
    return find(id);
  }

  @Transactional(readOnly = true)
  public List<Booking> list(String resource, Instant from, Instant to) {
    validate(resource, from, to);
    return withDeliveryStates(
        db.query(
            """
     select b.id,b.resource_id,b.starts_at,b.ends_at,b.status,b.service_id,b.service_name,b.duration_minutes,b.customer_name,b.customer_email
      from booking b
     where b.resource_id=:resource and b.starts_at<:end and b.ends_at>:start order by b.starts_at
    """,
            args(resource, from, to),
            (rs, n) -> booking(rs)));
  }

  /** Returns all bookings in one administrator calendar range. */
  @Transactional(readOnly = true)
  public List<Booking> listCalendar(Instant from, Instant to) {
    validate("calendar", from, to);
    return withDeliveryStates(
        db.query(
            """
    select b.id,b.resource_id,b.starts_at,b.ends_at,b.status,b.service_id,b.service_name,b.duration_minutes,b.customer_name,b.customer_email
      from booking b
     where b.starts_at<:end and b.ends_at>:start order by b.starts_at
    """,
            Map.of("start", timestamp(from), "end", timestamp(to)),
            (rs, row) -> booking(rs)));
  }

  /** Reschedules one confirmed booking and queues its Calendar event update. */
  @Transactional
  public Booking update(UUID id, Instant from, Instant to) {
    Booking current = find(id);
    validate(current.resourceId(), from, to);
    if (!from.isAfter(Instant.now()))
      throw new IllegalArgumentException("Booking must be in the future");
    if (!"CONFIRMED".equals(current.status()))
      throw new IllegalArgumentException("Only a confirmed booking can be rescheduled.");
    db.queryForObject(
        "select pg_advisory_xact_lock(hashtextextended(:resource,0))",
        Map.of("resource", current.resourceId()),
        (rs, n) -> 1);
    Map<String, Object> values = new java.util.HashMap<>(args(current.resourceId(), from, to));
    values.put("id", id);
    Boolean collision =
        db.queryForObject(
            """
    select exists (select 1 from booking where resource_id=:resource and status='CONFIRMED' and id<>:id and starts_at<:end and ends_at>:start
     union all select 1 from external_busy_block where resource_id=:resource and starts_at<:end and ends_at>:start)
    """,
            values,
            Boolean.class);
    if (Boolean.TRUE.equals(collision))
      throw new AvailabilityConflictException(
          "This slot is unavailable; refresh availability and select another slot");
    db.update("update booking set starts_at=:start,ends_at=:end where id=:id", values);
    calendars.bookingUpdated(id, current.resourceId());
    return find(id);
  }

  /** Cancels one booking and queues deletion of its Calendar event. */
  @Transactional
  public Booking cancel(UUID id) {
    Booking current = find(id);
    db.queryForObject(
        "select pg_advisory_xact_lock(hashtextextended(:resource,0))",
        Map.of("resource", current.resourceId()),
        (rs, n) -> 1);
    db.update("update booking set status='CANCELLED' where id=:id", Map.of("id", id));
    calendars.bookingCancelled(id);
    return find(id);
  }

  private Booking find(UUID id) {
    List<Booking> result =
        withDeliveryStates(
            db.query(
                """
     select b.id,b.resource_id,b.starts_at,b.ends_at,b.status,b.service_id,b.service_name,b.duration_minutes,b.customer_name,b.customer_email
      from booking b where b.id=:id
    """,
                Map.of("id", id),
                (rs, row) -> booking(rs)));
    if (result.isEmpty()) throw new BookingNotFoundException();
    return result.getFirst();
  }

  private static String clean(String value, int maximum) {
    if (value == null) return null;
    String cleaned = value.strip();
    if (cleaned.isEmpty() || cleaned.length() > maximum)
      throw new IllegalArgumentException("Customer details are not valid.");
    return cleaned;
  }

  private static Booking booking(java.sql.ResultSet rs) throws java.sql.SQLException {
    return new Booking(
        rs.getObject("id", UUID.class),
        rs.getString("resource_id"),
        rs.getTimestamp("starts_at").toInstant(),
        rs.getTimestamp("ends_at").toInstant(),
        rs.getString("status"),
        rs.getObject("service_id", UUID.class),
        rs.getString("service_name"),
        (Integer) rs.getObject("duration_minutes"),
        rs.getString("customer_name"),
        rs.getString("customer_email"),
        null,
        "NOT_CONFIGURED",
        null);
  }

  private List<Booking> withDeliveryStates(List<Booking> bookings) {
    Map<UUID, BookingCalendarCoordinator.DeliveryState> states =
        calendars.deliveryStates(bookings.stream().map(Booking::id).toList());
    return bookings.stream()
        .map(
            booking -> {
              var state = states.get(booking.id());
              return new Booking(
                  booking.id(),
                  booking.resourceId(),
                  booking.startsAt(),
                  booking.endsAt(),
                  booking.status(),
                  booking.serviceId(),
                  booking.serviceName(),
                  booking.durationMinutes(),
                  booking.customerName(),
                  booking.customerEmail(),
                  state == null ? null : state.provider(),
                  state == null ? "NOT_CONFIGURED" : state.status(),
                  state == null ? null : state.error());
            })
        .toList();
  }

  public record Booking(
      UUID id,
      String resourceId,
      Instant startsAt,
      Instant endsAt,
      String status,
      UUID serviceId,
      String serviceName,
      Integer durationMinutes,
      String customerName,
      String customerEmail,
      String calendarProvider,
      String calendarSyncStatus,
      String calendarSyncError) {}

  public static class AvailabilityConflictException extends RuntimeException {
    public AvailabilityConflictException(String message) {
      super(message);
    }
  }

  public static class BookingNotFoundException extends RuntimeException {}
}
