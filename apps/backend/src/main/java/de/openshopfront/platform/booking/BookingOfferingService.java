package de.openshopfront.platform.booking;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Manages customer-selectable booking services and their appointment durations. */
@Service
public class BookingOfferingService {
  private final NamedParameterJdbcTemplate db;

  public BookingOfferingService(NamedParameterJdbcTemplate db) {
    this.db = db;
  }

  /** Returns all services for administrator maintenance. */
  @Transactional(readOnly = true)
  public List<Offering> list() {
    return query("", Map.of());
  }

  /** Returns active services for the customer booking page. */
  @Transactional(readOnly = true)
  public List<Offering> listActive() {
    return query(" where active=true", Map.of());
  }

  /** Returns one active service or rejects an unavailable selection. */
  @Transactional(readOnly = true)
  public Offering active(UUID id) {
    return query(" where id=:id and active=true", Map.of("id", id)).stream()
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("The selected service is not available."));
  }

  /** Returns the customer booking switch and fallback duration. */
  @Transactional(readOnly = true)
  public CustomerSettings settings() {
    return db.queryForObject(
        "select customer_booking_enabled,default_duration_minutes from booking_customer_settings where id=1",
        Map.of(),
        (rs, row) ->
            new CustomerSettings(
                rs.getBoolean("customer_booking_enabled"), rs.getInt("default_duration_minutes")));
  }

  /** Updates the customer booking switch and fallback duration. */
  @Transactional
  public CustomerSettings updateSettings(
      boolean customerBookingEnabled, int defaultDurationMinutes) {
    validateDuration(defaultDurationMinutes);
    db.update(
        "update booking_customer_settings set customer_booking_enabled=:enabled,default_duration_minutes=:duration,updated_at=now() where id=1",
        Map.of("enabled", customerBookingEnabled, "duration", defaultDurationMinutes));
    return settings();
  }

  /** Resolves a service or the fallback appointment for one public request. */
  @Transactional(readOnly = true)
  public Offering publicOffering(UUID serviceId) {
    CustomerSettings current = settings();
    if (!current.customerBookingEnabled()) throw new CustomerBookingDisabledException();
    if (serviceId != null) return active(serviceId);
    if (!listActive().isEmpty()) throw new IllegalArgumentException("Select a service.");
    return new Offering(null, "Appointment", current.defaultDurationMinutes(), true);
  }

  /** Creates one service with the configured appointment duration. */
  @Transactional
  public Offering create(String name, int durationMinutes) {
    String cleanName = validate(name, durationMinutes);
    UUID id = UUID.randomUUID();
    db.update(
        "insert into booking_service_definition (id,name,duration_minutes,active,created_at,updated_at) values (:id,:name,:duration,true,now(),now())",
        Map.of("id", id, "name", cleanName, "duration", durationMinutes));
    return find(id);
  }

  /** Updates a service without changing historical booking snapshots. */
  @Transactional
  public Offering update(UUID id, String name, int durationMinutes, boolean active) {
    String cleanName = validate(name, durationMinutes);
    int changed =
        db.update(
            "update booking_service_definition set name=:name,duration_minutes=:duration,active=:active,updated_at=now() where id=:id",
            Map.of("id", id, "name", cleanName, "duration", durationMinutes, "active", active));
    if (changed == 0) throw new OfferingNotFoundException();
    return find(id);
  }

  /** Hides a service from new customer bookings. */
  @Transactional
  public void deactivate(UUID id) {
    if (db.update(
            "update booking_service_definition set active=false,updated_at=now() where id=:id",
            Map.of("id", id))
        == 0) throw new OfferingNotFoundException();
  }

  /** Removes a service. Historical bookings keep their stored snapshot and lose the link. */
  @Transactional
  public void delete(UUID id) {
    if (db.update("delete from booking_service_definition where id=:id", Map.of("id", id)) == 0)
      throw new OfferingNotFoundException();
  }

  private Offering find(UUID id) {
    return query(" where id=:id", Map.of("id", id)).stream()
        .findFirst()
        .orElseThrow(OfferingNotFoundException::new);
  }

  private List<Offering> query(String where, Map<String, ?> parameters) {
    return db.query(
        "select id,name,duration_minutes,active from booking_service_definition"
            + where
            + " order by active desc,name",
        parameters,
        (rs, row) ->
            new Offering(
                rs.getObject("id", UUID.class),
                rs.getString("name"),
                rs.getInt("duration_minutes"),
                rs.getBoolean("active")));
  }

  private static String validate(String name, int durationMinutes) {
    String clean = name == null ? "" : name.strip();
    validateDuration(durationMinutes);
    if (clean.isEmpty() || clean.length() > 120)
      throw new IllegalArgumentException("Enter a service name with no more than 120 characters.");
    return clean;
  }

  private static void validateDuration(int durationMinutes) {
    if (durationMinutes < 5 || durationMinutes > 480 || durationMinutes % 5 != 0)
      throw new IllegalArgumentException(
          "Enter a duration from 5 to 480 minutes in 5-minute steps.");
  }

  /** Contains the public booking switch and fallback duration. */
  public record CustomerSettings(boolean customerBookingEnabled, int defaultDurationMinutes) {}

  public record Offering(UUID id, String name, int durationMinutes, boolean active) {}

  public static class CustomerBookingDisabledException extends RuntimeException {}

  public static class OfferingNotFoundException extends RuntimeException {}
}
