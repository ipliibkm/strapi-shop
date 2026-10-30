package de.openshopfront.platform.calendar;

import de.openshopfront.platform.auth.GoogleAccessTokenService;
import de.openshopfront.platform.booking.BookingCalendarProvider;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Delivers booking changes to mapped Google calendars with persistent retries. */
@Service
@ConditionalOnProperty(name = "platform.google.enabled", havingValue = "true")
public class GoogleBookingEventService implements BookingCalendarProvider {
  private final NamedParameterJdbcTemplate db;
  private final GoogleAccessTokenService tokens;
  private final GoogleCalendarApi api;
  private final AtomicBoolean running = new AtomicBoolean();

  public GoogleBookingEventService(
      NamedParameterJdbcTemplate db, GoogleAccessTokenService tokens, GoogleCalendarApi api) {
    this.db = db;
    this.tokens = tokens;
    this.api = api;
  }

  @Override
  public String providerId() {
    return "google";
  }

  /** Queues a newly confirmed booking when its resource has write synchronization. */
  @Override
  @Transactional
  public void enqueueCreate(UUID bookingId, String resourceId) {
    db.update(
        """
   insert into google_booking_event (booking_id,principal_name,google_calendar_id,operation,delivery_status,next_attempt_at,updated_at)
   select :booking,principal_name,google_calendar_id,'UPSERT','PENDING',now(),now() from google_calendar_mapping
    where resource_id=:resource and write_bookings=true
   on conflict (booking_id) do nothing
   """,
        Map.of("booking", bookingId, "resource", resourceId));
  }

  /** Queues an update while retaining the calendar that owns an existing provider event. */
  @Override
  @Transactional
  public void enqueueUpdate(UUID bookingId, String resourceId) {
    int updated =
        db.update(
            """
   update google_booking_event g set operation='UPSERT',delivery_status='PENDING',attempt_count=0,generation=generation+1,next_attempt_at=now(),last_error=null,updated_at=now()
    where booking_id=:booking and operation<>'DELETE' and exists (select 1 from google_calendar_mapping m where m.resource_id=:resource
     and m.write_bookings=true and m.principal_name=g.principal_name and m.google_calendar_id=g.google_calendar_id)
   """,
            Map.of("booking", bookingId, "resource", resourceId));
    if (updated == 0) enqueueCreate(bookingId, resourceId);
  }

  /** Queues deletion of an existing provider event after local cancellation. */
  @Override
  @Transactional
  public void enqueueDelete(UUID bookingId) {
    db.update(
        """
   update google_booking_event set operation='DELETE',delivery_status='PENDING',attempt_count=0,generation=generation+1,next_attempt_at=now(),last_error=null,updated_at=now()
    where booking_id=:booking
   """,
        Map.of("booking", bookingId));
  }

  @Override
  @Transactional(readOnly = true)
  public Map<UUID, DeliveryState> deliveryStates(Collection<UUID> bookingIds) {
    if (bookingIds.isEmpty()) return Map.of();
    Map<UUID, DeliveryState> result = new LinkedHashMap<>();
    db.query(
            """
            select booking_id,delivery_status,last_error from google_booking_event
             where booking_id in (:bookingIds)
            """,
            Map.of("bookingIds", bookingIds),
            (rs, row) ->
                Map.entry(
                    rs.getObject("booking_id", UUID.class),
                    new DeliveryState(rs.getString("delivery_status"), rs.getString("last_error"))))
        .forEach(entry -> result.put(entry.getKey(), entry.getValue()));
    return result;
  }

  /** Delivers due events on the configured fixed delay. */
  @Scheduled(
      fixedDelayString = "${platform.google.delivery-interval-ms:15000}",
      initialDelayString = "${platform.google.delivery-initial-delay-ms:20000}")
  public void deliverScheduled() {
    deliverDue();
  }

  /** Delivers all due booking changes and returns the number selected. */
  public int deliverDue() {
    if (!running.compareAndSet(false, true)) return 0;
    try {
      List<Delivery> due =
          db.query(
              """
    select g.booking_id,g.principal_name,g.google_calendar_id,g.google_event_id,g.operation,g.generation,
            b.resource_id,b.service_name,b.starts_at,b.ends_at,b.status
      from google_booking_event g join booking b on b.id=g.booking_id
     where g.delivery_status in ('PENDING','RETRY') and g.next_attempt_at<=now()
     order by g.next_attempt_at limit 25
    """,
              Map.of(),
              (rs, row) ->
                  new Delivery(
                      rs.getObject("booking_id", UUID.class),
                      rs.getString("principal_name"),
                      rs.getString("google_calendar_id"),
                      rs.getString("google_event_id"),
                      rs.getString("operation"),
                      rs.getLong("generation"),
                      rs.getString("resource_id"),
                      rs.getString("service_name"),
                      rs.getTimestamp("starts_at").toInstant(),
                      rs.getTimestamp("ends_at").toInstant(),
                      rs.getString("status")));
      due.forEach(this::deliverSafely);
      return due.size();
    } finally {
      running.set(false);
    }
  }

  /** Requeues failed deliveries for one connected account. */
  public void retryFailed(String principalName) {
    db.update(
        """
   update google_booking_event set delivery_status='RETRY',attempt_count=0,generation=generation+1,next_attempt_at=now(),last_error=null,updated_at=now()
    where principal_name=:principal and delivery_status='FAILED'
   """,
        Map.of("principal", principalName));
  }

  private void deliverSafely(Delivery delivery) {
    try {
      String accessToken = tokens.authorize(delivery.principalName()).value();
      if ("DELETE".equals(delivery.operation()) || !"CONFIRMED".equals(delivery.bookingStatus())) {
        if (delivery.eventId() != null)
          api.deleteEvent(delivery.calendarId(), delivery.eventId(), accessToken);
        success(delivery, delivery.eventId());
        return;
      }
      var event =
          new GoogleCalendarApi.BookingEvent(
              delivery.bookingId().toString(),
              delivery.serviceName() == null
                  ? "Booking: " + delivery.resourceId()
                  : delivery.serviceName(),
              "Managed by OpenShopfront. Booking ID: " + delivery.bookingId(),
              delivery.startsAt(),
              delivery.endsAt());
      String eventId =
          delivery.eventId() == null
              ? api.createEvent(delivery.calendarId(), event, accessToken)
              : api.updateEvent(delivery.calendarId(), delivery.eventId(), event, accessToken);
      success(delivery, eventId);
    } catch (Exception error) {
      failed(delivery, error.getMessage());
    }
  }

  private void success(Delivery delivery, String eventId) {
    if ("DELETE".equals(delivery.operation()) || !"CONFIRMED".equals(delivery.bookingStatus())) {
      db.update(
          "delete from google_booking_event where booking_id=:booking and generation=:generation",
          Map.of("booking", delivery.bookingId(), "generation", delivery.generation()));
      return;
    }
    db.update(
        """
   update google_booking_event set google_event_id=:event,operation='NONE',delivery_status='SYNCED',attempt_count=0,
    last_error=null,updated_at=now() where booking_id=:booking and generation=:generation
   """,
        parameters(delivery.bookingId(), eventId, delivery.generation()));
  }

  private void failed(Delivery delivery, String message) {
    db.update(
        """
   update google_booking_event set attempt_count=attempt_count+1,
    delivery_status=case when attempt_count+1>=8 then 'FAILED' else 'RETRY' end,
    next_attempt_at=now()+(least(power(2,attempt_count+1),3600)::text||' seconds')::interval,
    last_error=:error,updated_at=now() where booking_id=:booking and generation=:generation
   """,
        Map.of(
            "booking",
            delivery.bookingId(),
            "generation",
            delivery.generation(),
            "error",
            limit(message)));
  }

  private static Map<String, Object> parameters(UUID bookingId, String eventId, long generation) {
    var values = new java.util.HashMap<String, Object>();
    values.put("booking", bookingId);
    values.put("event", eventId);
    values.put("generation", generation);
    return values;
  }

  private static String limit(String value) {
    String result = value == null || value.isBlank() ? "Google event delivery failed." : value;
    return result.substring(0, Math.min(result.length(), 2000));
  }

  private record Delivery(
      UUID bookingId,
      String principalName,
      String calendarId,
      String eventId,
      String operation,
      long generation,
      String resourceId,
      String serviceName,
      Instant startsAt,
      Instant endsAt,
      String bookingStatus) {}
}
