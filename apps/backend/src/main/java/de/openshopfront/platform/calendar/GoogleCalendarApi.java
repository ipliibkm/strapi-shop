package de.openshopfront.platform.calendar;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

/** Calls the Google Calendar REST API with refreshed bearer tokens. */
@Component
@ConditionalOnProperty(name = "platform.google.enabled", havingValue = "true")
public class GoogleCalendarApi {
  private final RestClient http;
  private final String baseUrl;

  public GoogleCalendarApi(
      RestClient.Builder builder,
      @Value("${platform.google.calendar-api-base-url:https://www.googleapis.com/calendar/v3}")
          String baseUrl) {
    this.baseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
    this.http = builder.baseUrl(this.baseUrl).build();
  }

  /** Lists calendars that the connected account can access. */
  public List<CalendarInfo> calendars(String accessToken) {
    List<CalendarInfo> result = new ArrayList<>();
    String pageToken = null;
    do {
      String currentPage = pageToken;
      JsonNode body =
          get(
              uri(
                  "users/me/calendarList",
                  Map.of("maxResults", "250", "pageToken", value(currentPage))),
              accessToken);
      for (JsonNode item : body.path("items"))
        result.add(
            new CalendarInfo(
                item.path("id").asText(),
                item.path("summary").asText(),
                item.path("primary").asBoolean(false),
                item.path("accessRole").asText()));
      pageToken = text(body, "nextPageToken");
    } while (pageToken != null);
    return result;
  }

  /** Reads one page of full or incremental event changes. */
  public EventPage events(
      String calendarId,
      String syncToken,
      String pageToken,
      String accessToken,
      Instant initialSince) {
    var parameters = new java.util.LinkedHashMap<String, String>();
    parameters.put("maxResults", "2500");
    parameters.put("singleEvents", "true");
    parameters.put("showDeleted", "true");
    if (syncToken != null) parameters.put("syncToken", syncToken);
    else parameters.put("timeMin", initialSince.toString());
    if (pageToken != null) parameters.put("pageToken", pageToken);
    JsonNode body = get(uri("calendars/" + calendarId + "/events", parameters), accessToken);
    ZoneId calendarZone = zone(body.path("timeZone").asText("UTC"));
    List<CalendarEvent> events = new ArrayList<>();
    for (JsonNode item : body.path("items")) {
      String id = item.path("id").asText();
      boolean deleted = "cancelled".equals(item.path("status").asText());
      boolean transparent = "transparent".equals(item.path("transparency").asText());
      boolean managed =
          !item.path("extendedProperties")
              .path("private")
              .path("openshopfrontBookingId")
              .asText("")
              .isBlank();
      Instant start = deleted ? null : instant(item.path("start"), calendarZone);
      Instant end = deleted ? null : instant(item.path("end"), calendarZone);
      events.add(new CalendarEvent(id, start, end, deleted, transparent, managed));
    }
    return new EventPage(events, text(body, "nextPageToken"), text(body, "nextSyncToken"));
  }

  /** Creates one booking event and returns the provider event ID. */
  public String createEvent(String calendarId, BookingEvent event, String accessToken) {
    String deterministicId = eventId(event.bookingId());
    try {
      JsonNode response =
          http.post()
              .uri("calendars/{calendarId}/events?sendUpdates=none", calendarId)
              .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
              .body(body(event))
              .retrieve()
              .body(JsonNode.class);
      String createdId = response == null ? null : text(response, "id");
      if (createdId == null)
        throw new GoogleCalendarApiException("Google did not return an event ID.");
      return createdId;
    } catch (RestClientResponseException error) {
      if (error.getStatusCode().value() == 409) return deterministicId;
      throw error;
    }
  }

  /** Updates one booking event. Creates a replacement if Google no longer has it. */
  public String updateEvent(
      String calendarId, String eventId, BookingEvent event, String accessToken) {
    try {
      JsonNode response =
          http.patch()
              .uri("calendars/{calendarId}/events/{eventId}?sendUpdates=none", calendarId, eventId)
              .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
              .body(body(event))
              .retrieve()
              .body(JsonNode.class);
      return response == null ? eventId : response.path("id").asText(eventId);
    } catch (RestClientResponseException error) {
      if (error.getStatusCode().value() == 404) return createEvent(calendarId, event, accessToken);
      throw error;
    }
  }

  /** Deletes one booking event. A missing provider event is already deleted. */
  public void deleteEvent(String calendarId, String eventId, String accessToken) {
    try {
      http.delete()
          .uri("calendars/{calendarId}/events/{eventId}?sendUpdates=none", calendarId, eventId)
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
          .retrieve()
          .toBodilessEntity();
    } catch (RestClientResponseException error) {
      if (error.getStatusCode().value() != 404 && error.getStatusCode().value() != 410) throw error;
    }
  }

  private JsonNode get(URI uri, String accessToken) {
    try {
      return http.get()
          .uri(uri)
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
          .retrieve()
          .body(JsonNode.class);
    } catch (RestClientResponseException error) {
      if (error.getStatusCode().value() == 410) throw new ExpiredSyncTokenException();
      throw error;
    }
  }

  private static Map<String, Object> body(BookingEvent event) {
    return Map.of(
        "id",
        eventId(event.bookingId()),
        "summary",
        event.summary(),
        "description",
        event.description(),
        "start",
        Map.of("dateTime", event.startsAt().toString()),
        "end",
        Map.of("dateTime", event.endsAt().toString()),
        "extendedProperties",
        Map.of("private", Map.of("openshopfrontBookingId", event.bookingId())));
  }

  private URI uri(String path, Map<String, String> parameters) {
    UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl).path(path);
    parameters.forEach(
        (name, value) -> {
          if (value != null && !value.isBlank()) builder.queryParam(name, value);
        });
    return builder.build().encode().toUri();
  }

  private static String value(String value) {
    return value == null ? "" : value;
  }

  private static String eventId(String bookingId) {
    return "openshopfront" + bookingId.replace("-", "").toLowerCase(java.util.Locale.ROOT);
  }

  private static String text(JsonNode node, String field) {
    String value = node.path(field).asText("");
    return value.isBlank() ? null : value;
  }

  private static ZoneId zone(String value) {
    try {
      return ZoneId.of(value);
    } catch (Exception ignored) {
      return ZoneOffset.UTC;
    }
  }

  private static Instant instant(JsonNode value, ZoneId defaultZone) {
    String dateTime = value.path("dateTime").asText("");
    if (!dateTime.isBlank()) return java.time.OffsetDateTime.parse(dateTime).toInstant();
    String date = value.path("date").asText("");
    return date.isBlank()
        ? null
        : LocalDate.parse(date)
            .atStartOfDay(zone(value.path("timeZone").asText(defaultZone.getId())))
            .toInstant();
  }

  public record CalendarInfo(String id, String name, boolean primary, String accessRole) {}

  public record CalendarEvent(
      String id,
      Instant startsAt,
      Instant endsAt,
      boolean deleted,
      boolean transparent,
      boolean managed) {}

  public record EventPage(List<CalendarEvent> events, String nextPageToken, String nextSyncToken) {}

  public record BookingEvent(
      String bookingId, String summary, String description, Instant startsAt, Instant endsAt) {}

  public static class ExpiredSyncTokenException extends RuntimeException {}

  public static class GoogleCalendarApiException extends RuntimeException {
    public GoogleCalendarApiException(String message) {
      super(message);
    }
  }
}
