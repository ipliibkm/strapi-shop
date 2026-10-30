package de.openshopfront.platform.auth;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/** Supplies valid Google access tokens and refreshes expired credentials. */
@Service
@ConditionalOnProperty(name = "platform.google.enabled", havingValue = "true")
public class GoogleAccessTokenService {
  public static final String CALENDAR_EVENTS_SCOPE =
      "https://www.googleapis.com/auth/calendar.events";
  public static final String CALENDAR_LIST_SCOPE =
      "https://www.googleapis.com/auth/calendar.calendarlist.readonly";

  /** Sends mail through the connected Google account; no mailbox read access. */
  public static final String GMAIL_SEND_SCOPE = "https://www.googleapis.com/auth/gmail.send";

  private final OAuth2AuthorizedClientManager clients;
  private final RestClient googleApi;

  public GoogleAccessTokenService(
      OAuth2AuthorizedClientManager clients,
      @Value("${platform.google.api-base-url:https://www.googleapis.com}") String googleBaseUrl) {
    this.clients = clients;
    this.googleApi = RestClient.builder().baseUrl(googleBaseUrl).build();
  }

  /** Returns the primary email of one authorized Google account. */
  public String profileEmail(String principalName, String accessTokenValue) {
    JsonNode response =
        googleApi
            .get()
            .uri("/oauth2/v3/userinfo")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessTokenValue)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .body(JsonNode.class);
    if (response == null || !response.path("email").isTextual()) return "";
    String value = response.path("email").asText().strip();
    return value.isBlank() ? "" : value;
  }

  /** Returns a usable token for a stored Google principal. */
  public AuthorizedToken authorize(String principalName) {
    var principal = UsernamePasswordAuthenticationToken.authenticated(principalName, "", List.of());
    OAuth2AuthorizedClient client =
        clients.authorize(
            OAuth2AuthorizeRequest.withClientRegistrationId("google").principal(principal).build());
    if (client == null)
      throw new GoogleAuthorizationException(
          "Google authorization is not available. Connect the account again.");
    if (!client
        .getAccessToken()
        .getScopes()
        .containsAll(java.util.Set.of(CALENDAR_EVENTS_SCOPE, CALENDAR_LIST_SCOPE)))
      throw new GoogleAuthorizationException(
          "Google authorization does not include the required Calendar scopes. Connect the account again.");
    return new AuthorizedToken(
        client.getAccessToken().getTokenValue(),
        client.getAccessToken().getExpiresAt(),
        client.getRefreshToken() != null,
        client.getAccessToken().getScopes());
  }

  /** Safe metadata for one refreshed authorized client. */
  public record AuthorizedToken(
      String value, Instant expiresAt, boolean refreshable, java.util.Set<String> scopes) {}

  /** Indicates that stored Google authorization cannot supply an access token. */
  public static class GoogleAuthorizationException extends RuntimeException {
    public GoogleAuthorizationException(String message) {
      super(message);
    }
  }
}
