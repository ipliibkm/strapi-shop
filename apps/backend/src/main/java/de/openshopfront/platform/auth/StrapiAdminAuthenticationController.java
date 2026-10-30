package de.openshopfront.platform.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Creates a platform session after Strapi verifies a local administrator account. */
@RestController
@RequestMapping("/api/v1/auth")
public class StrapiAdminAuthenticationController {
  private final StrapiAdminClient strapi;
  private final AdminLoginAttemptService loginAttempts;

  public StrapiAdminAuthenticationController(
      StrapiAdminClient strapi, AdminLoginAttemptService loginAttempts) {
    this.strapi = strapi;
    this.loginAttempts = loginAttempts;
  }

  /** Signs in with a Strapi administrator account and stores only encrypted provider tokens. */
  @PostMapping("/login")
  public Map<String, Object> login(
      @Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
    String address = servletRequest.getRemoteAddr();
    loginAttempts.check(request.email(), address);
    StrapiAdminClient.LoginResult result;
    try {
      result = strapi.login(request.email(), request.password());
    } catch (StrapiAdminClient.InvalidCredentialsException error) {
      loginAttempts.failed(request.email(), address);
      throw error;
    }
    loginAttempts.succeeded(request.email(), address);
    HttpSession session = servletRequest.getSession(true);
    servletRequest.changeSessionId();
    session.setAttribute(StrapiAdminClient.CREDENTIALS_SESSION_ATTRIBUTE, result.credentials());
    StrapiAdminPrincipal principal;
    try {
      principal = strapi.currentPrincipal(session);
    } catch (StrapiAdminClient.StrapiSessionExpiredException
        | StrapiAdminClient.StrapiUnavailableException
        | org.springframework.web.client.RestClientException error) {
      session.invalidate();
      throw new StrapiAdminClient.StrapiUnavailableException(error);
    }
    if (!result.principal().id().equals(principal.id())) {
      session.invalidate();
      throw new StrapiAdminClient.StrapiUnavailableException(null);
    }
    var authorities = new java.util.ArrayList<SimpleGrantedAuthority>();
    authorities.add(new SimpleGrantedAuthority("ROLE_STRAPI_ADMIN"));
    if (principal.superAdmin()) authorities.add(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
    var authentication =
        UsernamePasswordAuthenticationToken.authenticated(
            principal, null, List.copyOf(authorities));
    var context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(authentication);
    SecurityContextHolder.setContext(context);
    session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
    return session(principal);
  }

  static Map<String, Object> session(StrapiAdminPrincipal principal) {
    return Map.of(
        "authenticated",
        true,
        "name",
        principal.name(),
        "email",
        principal.email(),
        "superAdmin",
        principal.superAdmin());
  }

  @ExceptionHandler(StrapiAdminClient.InvalidCredentialsException.class)
  @ResponseStatus(HttpStatus.UNAUTHORIZED)
  Map<String, String> invalidCredentials() {
    return Map.of("message", "The email address or password is not correct.");
  }

  @ExceptionHandler(StrapiAdminClient.TooManyLoginAttemptsException.class)
  ResponseEntity<Map<String, String>> tooManyLoginAttempts(
      StrapiAdminClient.TooManyLoginAttemptsException error) {
    var response = ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS);
    if (error.retryAfter() != null && !error.retryAfter().isBlank())
      response.header(HttpHeaders.RETRY_AFTER, error.retryAfter());
    return response.body(
        Map.of("message", "Too many sign-in attempts. Wait before you try again."));
  }

  @ExceptionHandler(StrapiAdminClient.StrapiUnavailableException.class)
  @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
  Map<String, String> unavailable() {
    return Map.of("message", "Administrator sign-in is not available. Try again later.");
  }

  public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}
}
