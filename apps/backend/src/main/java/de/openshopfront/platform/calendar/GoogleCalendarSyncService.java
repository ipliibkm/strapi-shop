package de.openshopfront.platform.calendar;

import de.openshopfront.platform.auth.GoogleAccessTokenService;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/** Performs full and incremental Google Calendar synchronization. */
@Service
@ConditionalOnProperty(name = "platform.google.enabled", havingValue = "true")
public class GoogleCalendarSyncService {
  private final GoogleCalendarMappingService mappings;
  private final GoogleAccessTokenService tokens;
  private final GoogleCalendarApi api;
  private final Duration initialLookback;
  private final AtomicBoolean running = new AtomicBoolean();

  public GoogleCalendarSyncService(
      GoogleCalendarMappingService mappings,
      GoogleAccessTokenService tokens,
      GoogleCalendarApi api,
      @Value("${platform.google.sync-initial-lookback-days:31}") long initialLookbackDays) {
    this.mappings = mappings;
    this.tokens = tokens;
    this.api = api;
    this.initialLookback = Duration.ofDays(initialLookbackDays);
  }

  /** Synchronizes all mappings on the configured fixed delay. */
  @Scheduled(
      fixedDelayString = "${platform.google.sync-interval-ms:60000}",
      initialDelayString = "${platform.google.sync-initial-delay-ms:15000}")
  public void scheduledSync() {
    sync(null);
  }

  /** Synchronizes all mappings owned by a principal, or every mapping for scheduled work. */
  public List<SyncOutcome> sync(String principalName) {
    if (!running.compareAndSet(false, true)) return List.of();
    try {
      List<GoogleCalendarMappingService.CalendarMapping> selected =
          principalName == null ? mappings.listAll() : mappings.list(principalName);
      List<SyncOutcome> outcomes = new ArrayList<>();
      for (var mapping : selected) outcomes.add(syncSafely(mapping));
      return outcomes;
    } finally {
      running.set(false);
    }
  }

  private SyncOutcome syncSafely(GoogleCalendarMappingService.CalendarMapping mapping) {
    try {
      return synchronize(mapping, true);
    } catch (GoogleCalendarMappingService.StaleMappingException stale) {
      return new SyncOutcome(
          mapping.resourceId(), "STALE", 0, "The mapping changed during synchronization.");
    } catch (Exception error) {
      mappings.failed(mapping, error.getMessage());
      return new SyncOutcome(mapping.resourceId(), "ERROR", 0, error.getMessage());
    }
  }

  private SyncOutcome synchronize(
      GoogleCalendarMappingService.CalendarMapping mapping, boolean allowReset) {
    try {
      String token = tokens.authorize(mapping.principalName()).value();
      String pageToken = null;
      String nextSyncToken = null;
      List<GoogleCalendarApi.CalendarEvent> changes = new ArrayList<>();
      do {
        var page =
            api.events(
                mapping.calendarId(),
                mapping.syncToken(),
                pageToken,
                token,
                Instant.now().minus(initialLookback));
        changes.addAll(page.events());
        pageToken = page.nextPageToken();
        if (page.nextSyncToken() != null) nextSyncToken = page.nextSyncToken();
      } while (pageToken != null);
      if (nextSyncToken == null)
        throw new IllegalStateException("Google did not return a synchronization token.");
      var result = mappings.apply(mapping, changes, nextSyncToken, mapping.syncToken() == null);
      return new SyncOutcome(mapping.resourceId(), result.status(), result.conflictCount(), null);
    } catch (GoogleCalendarApi.ExpiredSyncTokenException expired) {
      if (!allowReset) throw expired;
      return synchronize(mappings.reset(mapping), false);
    }
  }

  public record SyncOutcome(String resourceId, String status, int conflictCount, String error) {}
}
