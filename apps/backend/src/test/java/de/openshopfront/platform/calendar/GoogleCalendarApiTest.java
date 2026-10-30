package de.openshopfront.platform.calendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** Verifies Calendar API paging and event conversion. */
class GoogleCalendarApiTest {
  @Test
  void readsIncrementalEventsAndIdentifiesManagedBookings() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    GoogleCalendarApi api = new GoogleCalendarApi(builder, "https://calendar.test/v3");
    server
        .expect(
            requestTo(
                "https://calendar.test/v3/calendars/team@example.com/events?maxResults=2500&singleEvents=true&showDeleted=true&syncToken=sync-1"))
        .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer access"))
        .andRespond(
            withSuccess(
                """
    {"timeZone":"Europe/Zurich","nextSyncToken":"sync-2","items":[
      {"id":"external","status":"confirmed","start":{"dateTime":"2026-12-01T10:00:00+01:00"},"end":{"dateTime":"2026-12-01T11:00:00+01:00"}},
      {"id":"managed","status":"confirmed","extendedProperties":{"private":{"openshopfrontBookingId":"123"}},"start":{"date":"2026-12-02"},"end":{"date":"2026-12-03"}},
      {"id":"deleted","status":"cancelled"}
    ]}
    """,
                MediaType.APPLICATION_JSON));

    var page = api.events("team@example.com", "sync-1", null, "access", Instant.EPOCH);

    assertEquals("sync-2", page.nextSyncToken());
    assertEquals(3, page.events().size());
    assertEquals(Instant.parse("2026-12-01T09:00:00Z"), page.events().get(0).startsAt());
    assertFalse(page.events().get(0).managed());
    assertTrue(page.events().get(1).managed());
    assertTrue(page.events().get(2).deleted());
    server.verify();
  }

  @Test
  void treatsARepeatedDeterministicInsertAsSuccess() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    GoogleCalendarApi api = new GoogleCalendarApi(builder, "https://calendar.test/v3");
    String bookingId = "25f18664-aa33-45e4-8837-6e522c757466";
    server
        .expect(requestTo("https://calendar.test/v3/calendars/primary/events?sendUpdates=none"))
        .andExpect(
            content()
                .json(
                    "{\"id\":\"openshopfront25f18664aa3345e488376e522c757466\",\"extendedProperties\":{\"private\":{\"openshopfrontBookingId\":\""
                        + bookingId
                        + "\"}}}"))
        .andRespond(withStatus(HttpStatus.CONFLICT));

    String eventId =
        api.createEvent(
            "primary",
            new GoogleCalendarApi.BookingEvent(
                bookingId,
                "Booking",
                "Managed",
                Instant.parse("2026-12-01T10:00:00Z"),
                Instant.parse("2026-12-01T11:00:00Z")),
            "access");

    assertEquals("openshopfront25f18664aa3345e488376e522c757466", eventId);
    server.verify();
  }
}
