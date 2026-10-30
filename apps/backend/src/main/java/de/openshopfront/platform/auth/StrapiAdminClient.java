package de.openshopfront.platform.auth;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpSession;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/** Authenticates Strapi administrators and proxies authorized content requests. */
@Service
public class StrapiAdminClient {
  public static final String CREDENTIALS_SESSION_ATTRIBUTE =
      StrapiAdminClient.class.getName() + ".CREDENTIALS";
  private static final String REFRESH_COOKIE = "strapi_admin_refresh";
  private final RestClient http;
  private final OAuthTokenCipher cipher;
  private final String serviceToken;

  public StrapiAdminClient(
      @Value("${platform.strapi.base-url}") String baseUrl,
      @Value("${platform.strapi.token-encryption-key}") String tokenEncryptionKey,
      @Value("${platform.strapi.service-token:}") String serviceToken) {
    this.http = RestClient.builder().baseUrl(baseUrl).build();
    this.cipher = new OAuthTokenCipher(tokenEncryptionKey);
    this.serviceToken = serviceToken;
  }

  /** Verifies local Strapi credentials and returns safe identity and encrypted session data. */
  public LoginResult login(String email, String password) {
    String deviceId = UUID.randomUUID().toString();
    try {
      var response =
          http.post()
              .uri("/admin/login")
              .contentType(MediaType.APPLICATION_JSON)
              .body(
                  Map.of(
                      "email",
                      email,
                      "password",
                      password,
                      "rememberMe",
                      false,
                      "deviceId",
                      deviceId))
              .retrieve()
              .toEntity(JsonNode.class);
      JsonNode data = require(response.getBody(), "data");
      JsonNode user = require(data, "user");
      StrapiAdminPrincipal principal = principal(user);
      String id = principal.id();
      String accessToken = firstText(data, "accessToken", "token");
      String refreshToken = refreshToken(response.getHeaders());
      return new LoginResult(principal, encrypt(id, deviceId, accessToken, refreshToken));
    } catch (HttpClientErrorException error) {
      if (error.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS)
        throw new TooManyLoginAttemptsException(
            error.getResponseHeaders().getFirst(HttpHeaders.RETRY_AFTER));
      if (error.getStatusCode() == HttpStatus.BAD_REQUEST
          || error.getStatusCode() == HttpStatus.UNAUTHORIZED)
        throw new InvalidCredentialsException();
      throw new StrapiUnavailableException(error);
    } catch (RestClientResponseException error) {
      throw new StrapiUnavailableException(error);
    }
  }

  /** Sends one content-manager request with the permissions of the signed-in Strapi user. */
  public JsonNode content(HttpSession session, String method, String path, JsonNode body) {
    Credentials credentials = credentials(session);
    try {
      return execute(method, path, body, credentials.accessToken(cipher));
    } catch (HttpClientErrorException.Unauthorized unauthorized) {
      Credentials refreshed = refresh(credentials);
      session.setAttribute(CREDENTIALS_SESSION_ATTRIBUTE, refreshed);
      return execute(method, path, body, refreshed.accessToken(cipher));
    }
  }

  /** Sends one content-manager request using a long-lived service token. */
  public JsonNode contentWithToken(String method, String path, JsonNode body) {
    if (serviceToken == null || serviceToken.isBlank()) {
      throw new IllegalStateException(
          "Set STRAPI_SERVICE_TOKEN so the platform can store anonymous form submissions.");
    }
    return execute(method, path, body, serviceToken);
  }

  /** Reloads the administrator and role state so Strapi changes take effect immediately. */
  public StrapiAdminPrincipal currentPrincipal(HttpSession session) {
    JsonNode response = content(session, "GET", "/admin/users/me", null);
    return principal(require(response, "data"));
  }

  /** Revokes only the Strapi device session created for this platform session. */
  public void logout(HttpSession session) {
    Object value = session.getAttribute(CREDENTIALS_SESSION_ATTRIBUTE);
    if (!(value instanceof Credentials credentials)) return;
    try {
      sendLogout(credentials);
    } catch (HttpClientErrorException.Unauthorized unauthorized) {
      try {
        sendLogout(refresh(credentials));
      } catch (RestClientResponseException ignored) {
      }
    } catch (RestClientResponseException ignored) {
    }
  }

  private JsonNode execute(String method, String path, JsonNode body, String accessToken) {
    var request =
        switch (method) {
          case "GET" -> http.get().uri(path);
          case "PUT" -> http.put().uri(path);
          case "POST" -> http.post().uri(path);
          case "DELETE" -> http.delete().uri(path);
          default -> throw new IllegalArgumentException("Unsupported Strapi request method.");
        };
    var headers =
        request
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .accept(MediaType.APPLICATION_JSON);
    if (body != null && request instanceof RestClient.RequestBodySpec bodyRequest)
      return bodyRequest
          .contentType(MediaType.APPLICATION_JSON)
          .body(body)
          .retrieve()
          .body(JsonNode.class);
    return headers.retrieve().body(JsonNode.class);
  }

  private Credentials refresh(Credentials credentials) {
    try {
      var response =
          http.post()
              .uri("/admin/access-token")
              .header(HttpHeaders.COOKIE, REFRESH_COOKIE + '=' + credentials.refreshToken(cipher))
              .retrieve()
              .toEntity(JsonNode.class);
      String accessToken = requireText(require(response.getBody(), "data"), "token");
      return encrypt(
          credentials.userId(),
          credentials.deviceId(),
          accessToken,
          refreshToken(response.getHeaders()));
    } catch (RestClientResponseException error) {
      throw new StrapiSessionExpiredException();
    }
  }

  private void sendLogout(Credentials credentials) {
    http.post()
        .uri("/admin/logout")
        .contentType(MediaType.APPLICATION_JSON)
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + credentials.accessToken(cipher))
        .header(HttpHeaders.COOKIE, REFRESH_COOKIE + '=' + credentials.refreshToken(cipher))
        .body(Map.of("deviceId", credentials.deviceId()))
        .retrieve()
        .toBodilessEntity();
  }

  private Credentials credentials(HttpSession session) {
    Object value = session.getAttribute(CREDENTIALS_SESSION_ATTRIBUTE);
    if (value instanceof Credentials credentials) return credentials;
    throw new StrapiSessionExpiredException();
  }

  private Credentials encrypt(
      String userId, String deviceId, String accessToken, String refreshToken) {
    return new Credentials(
        userId,
        deviceId,
        cipher.encrypt(accessToken, context(userId, "access")),
        cipher.encrypt(refreshToken, context(userId, "refresh")));
  }

  private String refreshToken(HttpHeaders headers) {
    return headers.getOrEmpty(HttpHeaders.SET_COOKIE).stream()
        .filter(value -> value.startsWith(REFRESH_COOKIE + '='))
        .map(value -> value.substring(REFRESH_COOKIE.length() + 1, value.indexOf(';')))
        .findFirst()
        .orElseThrow(() -> new StrapiUnavailableException(null));
  }

  private static JsonNode require(JsonNode node, String field) {
    if (node == null || node.path(field).isMissingNode())
      throw new StrapiUnavailableException(null);
    return node.path(field);
  }

  private static String requireText(JsonNode node, String field) {
    String value = node.path(field).asText("");
    if (value.isBlank()) throw new StrapiUnavailableException(null);
    return value;
  }

  private static String firstText(JsonNode node, String... fields) {
    return Arrays.stream(fields)
        .map(field -> node.path(field).asText(""))
        .filter(value -> !value.isBlank())
        .findFirst()
        .orElseThrow(() -> new StrapiUnavailableException(null));
  }

  private static StrapiAdminPrincipal principal(JsonNode user) {
    String id = requireText(user, "id");
    boolean superAdmin = false;
    for (JsonNode role : user.path("roles"))
      if ("strapi-super-admin".equals(role.path("code").asText())) superAdmin = true;
    String name =
        (user.path("firstname").asText("") + " " + user.path("lastname").asText("")).strip();
    if (name.isBlank())
      name = user.path("username").asText(user.path("email").asText("Administrator"));
    return new StrapiAdminPrincipal(id, name, user.path("email").asText(""), superAdmin);
  }

  private static String context(String userId, String type) {
    return "strapi-admin\0" + userId + '\0' + type;
  }

  public record LoginResult(StrapiAdminPrincipal principal, Credentials credentials) {}

  public record Credentials(
      String userId, String deviceId, byte[] encryptedAccessToken, byte[] encryptedRefreshToken)
      implements Serializable {
    String accessToken(OAuthTokenCipher cipher) {
      return cipher.decrypt(encryptedAccessToken, context(userId, "access"));
    }

    String refreshToken(OAuthTokenCipher cipher) {
      return cipher.decrypt(encryptedRefreshToken, context(userId, "refresh"));
    }
  }

  public static class InvalidCredentialsException extends RuntimeException {}

  /** Reports Strapi's administrator login rate limit without treating it as an outage. */
  public static class TooManyLoginAttemptsException extends RuntimeException {
    private final String retryAfter;

    TooManyLoginAttemptsException(String retryAfter) {
      this.retryAfter = retryAfter;
    }

    public String retryAfter() {
      return retryAfter;
    }
  }

  public static class StrapiSessionExpiredException extends RuntimeException {}

  public static class StrapiUnavailableException extends RuntimeException {
    StrapiUnavailableException(Throwable cause) {
      super(cause);
    }
  }
}
