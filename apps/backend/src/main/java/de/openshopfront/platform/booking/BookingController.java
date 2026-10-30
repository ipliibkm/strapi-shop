package de.openshopfront.platform.booking;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class BookingController {
  private final BookingService bookings;

  public BookingController(BookingService bookings) {
    this.bookings = bookings;
  }

  public record Availability(
      boolean available, String resourceId, Instant startsAt, Instant endsAt) {}

  public record CreateRequest(String resourceId, Instant startsAt, Instant endsAt) {}

  public record UpdateRequest(Instant startsAt, Instant endsAt) {}

  public record ApiError(String code, String message, String correlationId) {}

  @GetMapping("/availability")
  public Availability availability(
      @RequestParam String resourceId,
      @RequestParam Instant startsAt,
      @RequestParam Instant endsAt) {
    return new Availability(
        !bookings.isBusy(resourceId, startsAt, endsAt), resourceId, startsAt, endsAt);
  }

  @GetMapping("/admin/bookings")
  public List<BookingService.Booking> list(
      @RequestParam String resourceId, @RequestParam Instant from, @RequestParam Instant to) {
    return bookings.list(resourceId, from, to);
  }

  /** Returns bookings from all resources for the administrator calendar. */
  @GetMapping("/admin/bookings/calendar")
  public List<BookingService.Booking> calendar(
      @RequestParam Instant from, @RequestParam Instant to) {
    return bookings.listCalendar(from, to);
  }

  @PostMapping("/admin/bookings")
  @ResponseStatus(HttpStatus.CREATED)
  public BookingService.Booking create(@RequestBody CreateRequest r) {
    return bookings.create(r.resourceId, r.startsAt, r.endsAt);
  }

  @PutMapping("/admin/bookings/{id}")
  public BookingService.Booking update(@PathVariable UUID id, @RequestBody UpdateRequest request) {
    return bookings.update(id, request.startsAt(), request.endsAt());
  }

  @DeleteMapping("/admin/bookings/{id}")
  public BookingService.Booking cancel(@PathVariable UUID id) {
    return bookings.cancel(id);
  }

  @ExceptionHandler(BookingService.AvailabilityConflictException.class)
  public ResponseEntity<ApiError> conflict(BookingService.AvailabilityConflictException e) {
    return ResponseEntity.status(409)
        .body(new ApiError("AVAILABILITY_CONFLICT", e.getMessage(), UUID.randomUUID().toString()));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiError> badRequest(IllegalArgumentException e) {
    return ResponseEntity.badRequest()
        .body(new ApiError("VALIDATION_ERROR", e.getMessage(), UUID.randomUUID().toString()));
  }

  @ExceptionHandler(BookingService.BookingNotFoundException.class)
  public ResponseEntity<ApiError> notFound() {
    return ResponseEntity.status(404)
        .body(
            new ApiError(
                "BOOKING_NOT_FOUND", "The booking does not exist.", UUID.randomUUID().toString()));
  }
}
