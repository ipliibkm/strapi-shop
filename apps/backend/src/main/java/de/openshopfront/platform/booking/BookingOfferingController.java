package de.openshopfront.platform.booking;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Provides protected administrator operations for the booking service catalog. */
@RestController
@RequestMapping("/api/v1/admin/booking-services")
public class BookingOfferingController {
  private final BookingOfferingService offerings;

  public BookingOfferingController(BookingOfferingService offerings) {
    this.offerings = offerings;
  }

  /** Returns active and hidden services for administrator maintenance. */
  @GetMapping
  public List<BookingOfferingService.Offering> list() {
    return offerings.list();
  }

  /** Returns the customer booking configuration. */
  @GetMapping("/settings")
  public BookingOfferingService.CustomerSettings settings() {
    return offerings.settings();
  }

  /** Updates the customer booking switch and fallback duration. */
  @PutMapping("/settings")
  public BookingOfferingService.CustomerSettings updateSettings(
      @RequestBody SettingsRequest request) {
    return offerings.updateSettings(
        request.customerBookingEnabled(), request.defaultDurationMinutes());
  }

  /** Adds a service to the customer booking catalog. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public BookingOfferingService.Offering create(@RequestBody Request request) {
    return offerings.create(request.name(), request.durationMinutes());
  }

  /** Changes the name, duration, or visibility of a service. */
  @PutMapping("/{id}")
  public BookingOfferingService.Offering update(
      @PathVariable UUID id, @RequestBody Request request) {
    return offerings.update(id, request.name(), request.durationMinutes(), request.active());
  }

  /** Hides a service without changing historical bookings. */
  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deactivate(@PathVariable UUID id) {
    offerings.deactivate(id);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, String>> invalid(IllegalArgumentException error) {
    return ResponseEntity.badRequest().body(Map.of("message", error.getMessage()));
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<Map<String, String>> duplicate() {
    return ResponseEntity.badRequest()
        .body(Map.of("message", "A service with this name already exists."));
  }

  @ExceptionHandler(BookingOfferingService.OfferingNotFoundException.class)
  public ResponseEntity<Void> missing() {
    return ResponseEntity.notFound().build();
  }

  /** Contains administrator-editable service properties. */
  public record Request(String name, int durationMinutes, boolean active) {}

  /** Contains administrator-editable customer booking settings. */
  public record SettingsRequest(boolean customerBookingEnabled, int defaultDurationMinutes) {}
}
