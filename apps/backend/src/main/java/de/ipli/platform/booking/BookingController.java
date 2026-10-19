package de.ipli.platform.booking;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class BookingController {
 private final BookingService bookings;
 public BookingController(BookingService bookings){this.bookings=bookings;}
 public record Availability(boolean available,String resourceId,Instant startsAt,Instant endsAt){}
 public record CreateRequest(String resourceId,Instant startsAt,Instant endsAt){}
 public record ApiError(String code,String message, String correlationId){}
 @GetMapping("/availability")
 public Availability availability(@RequestParam String resourceId,@RequestParam Instant startsAt,@RequestParam Instant endsAt) {
  return new Availability(!bookings.isBusy(resourceId,startsAt,endsAt),resourceId,startsAt,endsAt);
 }
 @GetMapping("/admin/bookings")
 public List<BookingService.Booking> list(@RequestParam String resourceId,@RequestParam Instant from,@RequestParam Instant to) {
  return bookings.list(resourceId,from,to);
 }
 @PostMapping("/admin/bookings")
 @ResponseStatus(HttpStatus.CREATED)
 public BookingService.Booking create(@RequestBody CreateRequest r){return bookings.create(r.resourceId,r.startsAt,r.endsAt);}
 @ExceptionHandler(BookingService.AvailabilityConflictException.class)
 public ResponseEntity<ApiError> conflict(BookingService.AvailabilityConflictException e) {
  return ResponseEntity.status(409).body(new ApiError("AVAILABILITY_CONFLICT",e.getMessage(),UUID.randomUUID().toString()));
 }
 @ExceptionHandler(IllegalArgumentException.class)
 public ResponseEntity<ApiError> badRequest(IllegalArgumentException e){
  return ResponseEntity.badRequest().body(new ApiError("VALIDATION_ERROR",e.getMessage(),UUID.randomUUID().toString()));
 }
}
