package de.openshopfront.platform.auth;

import de.openshopfront.platform.calendar.GoogleCalendarApi;
import de.openshopfront.platform.calendar.GoogleCalendarMappingService;
import java.time.Instant;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

/**
 * Reports and revokes the Google Calendar grant associated with the current administrator session.
 */
@RestController
@RequestMapping("/api/v1/admin/integrations/google")
@ConditionalOnProperty(name = "platform.google.enabled", havingValue = "true")
public class GoogleCalendarConnectionController {
  private final OAuth2AuthorizedClientService clients;
  private final GoogleAccessTokenService tokens;
  private final GoogleCalendarApi calendar;
  private final GoogleCalendarMappingService mappings;
  private final GoogleServiceConnection connection;
  private final RestClient http = RestClient.create();

  public GoogleCalendarConnectionController(
      OAuth2AuthorizedClientService clients,
      GoogleAccessTokenService tokens,
      GoogleCalendarApi calendar,
      GoogleCalendarMappingService mappings,
      GoogleServiceConnection connection) {
    this.clients = clients;
    this.tokens = tokens;
    this.calendar = calendar;
    this.mappings = mappings;
    this.connection = connection;
  }

  /** Returns the Calendar grant state without returning access or refresh tokens. */
  @GetMapping
  public ConnectionStatus status() {
    String owner = connection.owner();
    OAuth2AuthorizedClient stored = clients.loadAuthorizedClient("google", owner);
    if (stored == null)
      return new ConnectionStatus(
          false,
          "DISCONNECTED",
          connection.email(),
          java.util.Set.of(),
          "",
          false,
          List.of(),
          null);
    try {
      var authorized = tokens.authorize(owner);
      calendar.calendars(authorized.value());
      return new ConnectionStatus(
          true,
          "HEALTHY",
          connection.email(),
          authorized.scopes(),
          instant(authorized.expiresAt()),
          authorized.refreshable(),
          mappings.list(owner),
          null);
    } catch (Exception error) {
      return new ConnectionStatus(
          true,
          "ERROR",
          connection.email(),
          stored.getAccessToken().getScopes(),
          instant(stored.getAccessToken().getExpiresAt()),
          stored.getRefreshToken() != null,
          mappings.list(owner),
          "Google authorization or the Calendar API is not usable. Reconnect the account if the error continues.");
    }
  }

  /** Revokes the current Google grant and removes it from the server session. */
  @DeleteMapping
  public void disconnect() {
    String owner = connection.owner();
    OAuth2AuthorizedClient client = clients.loadAuthorizedClient("google", owner);
    if (client == null) return;
    String token =
        client.getRefreshToken() != null
            ? client.getRefreshToken().getTokenValue()
            : client.getAccessToken().getTokenValue();
    try {
      http.post()
          .uri("https://oauth2.googleapis.com/revoke")
          .contentType(MediaType.APPLICATION_FORM_URLENCODED)
          .body(
              "token=" + java.net.URLEncoder.encode(token, java.nio.charset.StandardCharsets.UTF_8))
          .retrieve()
          .toBodilessEntity();
    } catch (org.springframework.web.client.RestClientException ignored) {
    } finally {
      mappings.removeAll(owner);
      clients.removeAuthorizedClient("google", owner);
      connection.clear();
    }
  }

  private String instant(Instant value) {
    return value == null ? "" : value.toString();
  }

  public record ConnectionStatus(
      boolean connected,
      String health,
      String email,
      java.util.Set<String> scopes,
      String expiresAt,
      boolean refreshable,
      List<GoogleCalendarMappingService.CalendarMapping> mappings,
      String error) {}
}
