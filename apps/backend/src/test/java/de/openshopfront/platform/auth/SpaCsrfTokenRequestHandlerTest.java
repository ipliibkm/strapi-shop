package de.openshopfront.platform.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.csrf.DefaultCsrfToken;

class SpaCsrfTokenRequestHandlerTest {
  @Test
  void exposesTheRawTokenForTheSpaEndpoint() {
    var request = new MockHttpServletRequest();
    var token = new DefaultCsrfToken("X-XSRF-TOKEN", "_csrf", "raw-token");

    new SpaCsrfTokenRequestHandler().handle(request, new MockHttpServletResponse(), () -> token);

    assertThat(request.getAttribute(SpaCsrfTokenRequestHandler.RAW_TOKEN_ATTRIBUTE))
        .isSameAs(token);
  }

  @Test
  void acceptsTheRawTokenFromAngularsHeader() {
    var request = new MockHttpServletRequest();
    var token = new DefaultCsrfToken("X-XSRF-TOKEN", "_csrf", "raw-token");
    request.addHeader(token.getHeaderName(), token.getToken());

    assertThat(new SpaCsrfTokenRequestHandler().resolveCsrfTokenValue(request, token))
        .isEqualTo("raw-token");
  }
}
