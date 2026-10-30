package de.openshopfront.platform.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.function.Supplier;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.util.StringUtils;

/** Accepts Angular's plain CSRF header while retaining BREACH protection for rendered tokens. */
final class SpaCsrfTokenRequestHandler implements CsrfTokenRequestHandler {
  static final String RAW_TOKEN_ATTRIBUTE =
      SpaCsrfTokenRequestHandler.class.getName() + ".RAW_TOKEN";
  private final CsrfTokenRequestHandler plain = new CsrfTokenRequestAttributeHandler();
  private final CsrfTokenRequestHandler xor = new XorCsrfTokenRequestAttributeHandler();

  /** Publishes an encoded request attribute and forces the raw token cookie to be written. */
  @Override
  public void handle(
      HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> csrfToken) {
    CsrfToken rawToken = csrfToken.get();
    request.setAttribute(RAW_TOKEN_ATTRIBUTE, rawToken);
    xor.handle(request, response, () -> rawToken);
  }

  /** Reads JavaScript headers as raw values and form parameters as encoded values. */
  @Override
  public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
    return (StringUtils.hasText(request.getHeader(csrfToken.getHeaderName())) ? plain : xor)
        .resolveCsrfTokenValue(request, csrfToken);
  }
}
