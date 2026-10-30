package de.openshopfront.platform.booking;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Provides the anonymous customer booking contract without exposing administrator data. */
@RestController
@RequestMapping("/api/v1/public")
public class PublicBookingController {
  private final BookingService bookings;
  private final BookingOfferingService offerings;
  private final PublicBookingReadinessService readiness;
  private final String resourceId;

  public PublicBookingController(
      BookingService bookings,
      BookingOfferingService offerings,
      PublicBookingReadinessService readiness,
      @Value("${platform.booking.public-resource-id:default}") String resourceId) {
    this.bookings = bookings;
    this.offerings = offerings;
    this.readiness = readiness;
    this.resourceId = resourceId;
  }

  /** Lists active services without exposing administrator-only fields. */
  @GetMapping("/services")
  public List<PublicOffering> services() {
    return offerings.listActive().stream()
        .map(item -> new PublicOffering(item.id(), item.name(), item.durationMinutes()))
        .toList();
  }

  /** Returns the public booking state and fallback duration. */
  @GetMapping("/booking-settings")
  public PublicSettings settings() {
    var configured = offerings.settings();
    if (!configured.customerBookingEnabled())
      return new PublicSettings(
          false, configured.defaultDurationMinutes(), "Online booking is disabled.");
    var current = readiness.check(resourceId);
    return new PublicSettings(
        current.available(), configured.defaultDurationMinutes(), current.reason());
  }

  /** Checks one customer-selected interval for the configured public resource. */
  @GetMapping("/availability")
  public Availability availability(
      @RequestParam(required = false) UUID serviceId, @RequestParam Instant startsAt) {
    BookingOfferingService.Offering offering = offerings.publicOffering(serviceId);
    readiness.requireAvailable(resourceId);
    Instant endsAt = startsAt.plusSeconds(offering.durationMinutes() * 60L);
    return new Availability(!bookings.isBusy(resourceId, startsAt, endsAt), startsAt, endsAt);
  }

  /** Creates a confirmed booking and returns only the customer-safe confirmation fields. */
  @PostMapping("/bookings")
  @ResponseStatus(HttpStatus.CREATED)
  public Confirmation create(@Valid @RequestBody CreateRequest request) {
    BookingOfferingService.Offering offering = offerings.publicOffering(request.serviceId());
    readiness.requireAvailable(resourceId);
    BookingService.Booking booking =
        bookings.create(
            resourceId,
            request.startsAt(),
            offering,
            request.customerName(),
            request.customerEmail());
    return new Confirmation(
        booking.id(),
        booking.serviceName(),
        booking.durationMinutes(),
        booking.startsAt(),
        booking.endsAt(),
        booking.status());
  }

  @ExceptionHandler(BookingService.AvailabilityConflictException.class)
  public ResponseEntity<Map<String, String>> conflict(
      BookingService.AvailabilityConflictException error) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(Map.of("code", "AVAILABILITY_CONFLICT", "message", error.getMessage()));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, String>> invalid(IllegalArgumentException error) {
    return ResponseEntity.badRequest()
        .body(Map.of("code", "VALIDATION_ERROR", "message", error.getMessage()));
  }

  /** Rejects public booking calls while customer booking is disabled. */
  @ExceptionHandler(BookingOfferingService.CustomerBookingDisabledException.class)
  public ResponseEntity<Map<String, String>> bookingDisabled() {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(Map.of("code", "BOOKING_DISABLED", "message", "Online booking is not available."));
  }

  /** Rejects public booking calls when external calendars cannot protect availability. */
  @ExceptionHandler(PublicBookingReadinessService.PublicBookingUnavailableException.class)
  public ResponseEntity<Map<String, String>> bookingUnavailable(
      PublicBookingReadinessService.PublicBookingUnavailableException error) {
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body(Map.of("code", "BOOKING_UNAVAILABLE", "message", error.getMessage()));
  }

  /** Returns a stable public response for malformed or incomplete customer data. */
  @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
  public ResponseEntity<Map<String, String>> invalidRequest(Exception error) {
    return ResponseEntity.badRequest()
        .body(
            Map.of(
                "code", "VALIDATION_ERROR", "message", "Check the booking details and try again."));
  }

  /** Describes one service that a customer can select. */
  public record PublicOffering(UUID id, String name, int durationMinutes) {}

  /** Reports the effective customer booking state without exposing provider account data. */
  public record PublicSettings(
      boolean customerBookingEnabled, int defaultDurationMinutes, String unavailableReason) {}

  /** Reports the calculated interval and its current availability. */
  public record Availability(boolean available, Instant startsAt, Instant endsAt) {}

  /** Contains customer input for a new booking. */
  public record CreateRequest(
      UUID serviceId,
      @NotNull Instant startsAt,
      @NotBlank @Size(max = 200) String customerName,
      @NotBlank @Email @Size(max = 254) String customerEmail) {}

  /** Returns customer-safe details for a confirmed booking. */
  public record Confirmation(
      UUID id,
      String serviceName,
      Integer durationMinutes,
      Instant startsAt,
      Instant endsAt,
      String status) {}
}
