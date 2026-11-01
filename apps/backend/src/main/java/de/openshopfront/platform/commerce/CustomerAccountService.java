package de.openshopfront.platform.commerce;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

/** Delegates customer identity and role decisions to Strapi Users & Permissions. */
@Service
public class CustomerAccountService {
  private final RestClient strapi;
  private final CommerceSettingsService settings;

  public CustomerAccountService(
      @Value("${platform.strapi.base-url}") String baseUrl, CommerceSettingsService settings) {
    this.strapi = RestClient.builder().baseUrl(baseUrl).build();
    this.settings = settings;
  }

  public void requireEnabled() {
    if (!settings.require().customerAreaEnabled())
      throw new ResponseStatusException(
          HttpStatus.NOT_FOUND, "Customer accounts are not available.");
  }

  public JsonNode authenticate(String path, Map<String, String> credentials) {
    requireEnabled();
    try {
      return strapi
          .post()
          .uri(path)
          .contentType(MediaType.APPLICATION_JSON)
          .body(credentials)
          .retrieve()
          .body(JsonNode.class);
    } catch (RestClientResponseException error) {
      if (error.getStatusCode().value() == 429)
        throw new ResponseStatusException(
            HttpStatus.TOO_MANY_REQUESTS, "Too many attempts. Try again later.");
      if (error.getStatusCode().is4xxClientError())
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "Strapi rejected the account request. Check your details and Strapi registration settings.");
      throw new ResponseStatusException(
          HttpStatus.BAD_GATEWAY, "Strapi did not accept the account request.");
    }
  }

  /** Strapi validates the JWT, confirmation, blocked state, and current role on each request. */
  public Customer requireCustomer(String authorization) {
    requireEnabled();
    if (authorization == null || !authorization.startsWith("Bearer "))
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in to continue.");
    try {
      JsonNode user =
          strapi
              .get()
              .uri("/api/users/me?populate=role")
              .header(HttpHeaders.AUTHORIZATION, authorization)
              .retrieve()
              .body(JsonNode.class);
      if (user == null
          || !user.path("id").canConvertToLong()
          || user.path("blocked").asBoolean()
          || !user.path("confirmed").asBoolean(true))
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in to continue.");
      return new Customer(
          user.path("id").asLong(), user.path("email").asText(), user.path("username").asText());
    } catch (RestClientResponseException error) {
      if (error.getStatusCode().is4xxClientError())
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in to continue.");
      throw new ResponseStatusException(
          HttpStatus.BAD_GATEWAY, "Strapi did not respond. Try again.");
    }
  }

  public record Customer(long id, String email, String username) {}
}
