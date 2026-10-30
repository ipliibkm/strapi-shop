package de.openshopfront.platform.calendar;

import de.openshopfront.platform.auth.GoogleAccessTokenService;
import de.openshopfront.platform.auth.GoogleServiceConnection;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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

/** Manages Calendar discovery, resource mappings, and manual synchronization. */
@RestController
@RequestMapping("/api/v1/admin/integrations/google")
@ConditionalOnProperty(name = "platform.google.enabled", havingValue = "true")
public class GoogleCalendarController {
  private final GoogleAccessTokenService tokens;
  private final GoogleCalendarApi api;
  private final GoogleCalendarMappingService mappings;
  private final GoogleCalendarSyncService sync;
  private final GoogleBookingEventService bookingEvents;
  private final GoogleServiceConnection connection;

  public GoogleCalendarController(
      GoogleAccessTokenService tokens,
      GoogleCalendarApi api,
      GoogleCalendarMappingService mappings,
      GoogleCalendarSyncService sync,
      GoogleBookingEventService bookingEvents,
      GoogleServiceConnection connection) {
    this.tokens = tokens;
    this.api = api;
    this.mappings = mappings;
    this.sync = sync;
    this.bookingEvents = bookingEvents;
    this.connection = connection;
  }

  /** Lists calendars available to the connected account. */
  @GetMapping("/calendars")
  public List<GoogleCalendarApi.CalendarInfo> calendars() {
    return api.calendars(tokens.authorize(connection.owner()).value());
  }

  /** Lists resource mappings owned by the connected account. */
  @GetMapping("/mappings")
  public List<GoogleCalendarMappingService.CalendarMapping> mappings() {
    return mappings.list(connection.owner());
  }

  /** Creates or replaces one resource mapping. */
  @PutMapping("/mappings/{resourceId}")
  public GoogleCalendarMappingService.CalendarMapping save(
      @PathVariable String resourceId, @RequestBody MappingRequest request) {
    String owner = connection.owner();
    if (request.writeBookings()) {
      String accessToken = tokens.authorize(owner).value();
      GoogleCalendarApi.CalendarInfo selected =
          api.calendars(accessToken).stream()
              .filter(item -> item.id().equals(request.calendarId()))
              .findFirst()
              .orElseThrow(
                  () ->
                      new IllegalArgumentException(
                          "The selected Google calendar is not available."));
      if (!java.util.Set.of("owner", "writer", "writerWithoutPrivateAccess")
          .contains(selected.accessRole()))
        throw new IllegalArgumentException(
            "The selected Google calendar does not permit event changes.");
    }
    return mappings.save(
        owner, resourceId, request.calendarId(), request.displayName(), request.writeBookings());
  }

  /** Removes one resource mapping and its imported busy blocks. */
  @DeleteMapping("/mappings/{resourceId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void remove(@PathVariable String resourceId) {
    mappings.remove(connection.owner(), resourceId);
  }

  /** Starts synchronization immediately for the connected account. */
  @PostMapping("/sync")
  public List<GoogleCalendarSyncService.SyncOutcome> synchronize() {
    String owner = connection.owner();
    bookingEvents.retryFailed(owner);
    List<GoogleCalendarSyncService.SyncOutcome> result = sync.sync(owner);
    bookingEvents.deliverDue();
    return result;
  }

  public record MappingRequest(String calendarId, String displayName, boolean writeBookings) {}

  @ExceptionHandler({
    IllegalArgumentException.class,
    GoogleCalendarMappingService.MappingConflictException.class
  })
  public ResponseEntity<java.util.Map<String, String>> invalidMapping(RuntimeException error) {
    return ResponseEntity.badRequest().body(java.util.Map.of("message", error.getMessage()));
  }
}
