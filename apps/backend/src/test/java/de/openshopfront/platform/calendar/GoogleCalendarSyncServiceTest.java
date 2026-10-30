package de.openshopfront.platform.calendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.openshopfront.platform.auth.GoogleAccessTokenService;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Verifies incremental synchronization and expired-token recovery. */
class GoogleCalendarSyncServiceTest {
  @Test
  void resetsAnExpiredSyncTokenAndRunsAFullSync() {
    GoogleCalendarMappingService mappings = mock(GoogleCalendarMappingService.class);
    GoogleAccessTokenService tokens = mock(GoogleAccessTokenService.class);
    GoogleCalendarApi api = mock(GoogleCalendarApi.class);
    var mapping =
        new GoogleCalendarMappingService.CalendarMapping(
            "room-1", "subject", "calendar", "Team", true, "old", "SYNCED", null, 0, null, 2);
    var reset =
        new GoogleCalendarMappingService.CalendarMapping(
            "room-1", "subject", "calendar", "Team", true, null, "PENDING", null, 0, null, 2);
    when(mappings.list("subject")).thenReturn(List.of(mapping));
    when(mappings.reset(mapping)).thenReturn(reset);
    when(tokens.authorize("subject"))
        .thenReturn(
            new GoogleAccessTokenService.AuthorizedToken(
                "access", Instant.now().plusSeconds(3600), true, Set.of()));
    when(api.events(eq("calendar"), eq("old"), eq(null), eq("access"), any()))
        .thenThrow(new GoogleCalendarApi.ExpiredSyncTokenException());
    when(api.events(eq("calendar"), eq(null), eq(null), eq("access"), any()))
        .thenReturn(new GoogleCalendarApi.EventPage(List.of(), null, "new"));
    when(mappings.apply(reset, List.of(), "new", true))
        .thenReturn(new GoogleCalendarMappingService.SyncResult(0, "SYNCED"));

    var result = new GoogleCalendarSyncService(mappings, tokens, api, 31).sync("subject");

    assertEquals("SYNCED", result.getFirst().status());
    verify(mappings).reset(mapping);
    verify(mappings).apply(reset, List.of(), "new", true);
  }
}
