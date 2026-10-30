package de.openshopfront.platform.auth;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Exposes safe administrator session metadata to the same-origin Angular application. */
@RestController
@RequestMapping("/api/v1/auth")
public class AdminSessionController {
  /** Returns the current login state without exposing OAuth tokens or identifiers. */
  @GetMapping("/session")
  public Map<String, Object> session(Authentication authentication) {
    if (authentication == null
        || !authentication.isAuthenticated()
        || !(authentication.getPrincipal() instanceof StrapiAdminPrincipal user))
      return Map.of("authenticated", false);
    return StrapiAdminAuthenticationController.session(user);
  }

  /** Returns the CSRF header metadata and writes the matching token cookie. */
  @GetMapping("/csrf")
  public Map<String, String> csrf(CsrfToken renderedToken, HttpServletRequest request) {
    Object value = request.getAttribute(SpaCsrfTokenRequestHandler.RAW_TOKEN_ATTRIBUTE);
    CsrfToken token = value instanceof CsrfToken rawToken ? rawToken : renderedToken;
    return Map.of("headerName", token.getHeaderName(), "token", token.getToken());
  }
}
