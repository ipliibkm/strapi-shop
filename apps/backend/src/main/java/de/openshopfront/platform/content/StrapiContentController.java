package de.openshopfront.platform.content;

import com.fasterxml.jackson.databind.JsonNode;
import de.openshopfront.platform.auth.StrapiAdminClient;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;

/** Proxies supported content operations while Strapi enforces administrator permissions. */
@RestController
@RequestMapping("/api/v1/content/business-profile")
public class StrapiContentController {
  private static final String STRAPI_PATH =
      "/content-manager/single-types/api::business-profile.business-profile";
  private final StrapiAdminClient strapi;

  public StrapiContentController(StrapiAdminClient strapi) {
    this.strapi = strapi;
  }

  /** Returns the Business Profile when the Strapi role permits reading it. */
  @GetMapping
  public JsonNode get(HttpSession session) {
    return strapi.content(session, "GET", STRAPI_PATH, null);
  }

  /** Updates the Business Profile when the Strapi role permits changing it. */
  @PutMapping
  public JsonNode update(@RequestBody JsonNode body, HttpSession session) {
    return strapi.content(session, "PUT", STRAPI_PATH, body);
  }

  @ExceptionHandler(HttpClientErrorException.Forbidden.class)
  @ResponseStatus(HttpStatus.FORBIDDEN)
  void forbidden() {}

  @ExceptionHandler(HttpClientErrorException.BadRequest.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  void invalidContent() {}

  @ExceptionHandler(StrapiAdminClient.StrapiSessionExpiredException.class)
  @ResponseStatus(HttpStatus.UNAUTHORIZED)
  void expired() {}
}
