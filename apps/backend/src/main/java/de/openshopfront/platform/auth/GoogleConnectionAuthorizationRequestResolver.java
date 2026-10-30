package de.openshopfront.platform.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

/** Allows Google service authorization only for an authenticated Strapi Super Admin. */
final class GoogleConnectionAuthorizationRequestResolver
    implements OAuth2AuthorizationRequestResolver {
  static final String LOCAL_SECURITY_CONTEXT =
      GoogleConnectionAuthorizationRequestResolver.class.getName() + ".LOCAL_CONTEXT";
  static final String GOOGLE_OWNER =
      GoogleConnectionAuthorizationRequestResolver.class.getName() + ".GOOGLE_OWNER";
  private final OAuth2AuthorizationRequestResolver delegate;
  private final GoogleServiceConnection connection;

  GoogleConnectionAuthorizationRequestResolver(
      OAuth2AuthorizationRequestResolver delegate, GoogleServiceConnection connection) {
    this.delegate = delegate;
    this.connection = connection;
  }

  @Override
  public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
    return authorized(request, delegate.resolve(request));
  }

  @Override
  public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String registrationId) {
    return authorized(request, delegate.resolve(request, registrationId));
  }

  private OAuth2AuthorizationRequest authorized(
      HttpServletRequest request, OAuth2AuthorizationRequest authorizationRequest) {
    var context = SecurityContextHolder.getContext();
    var authentication = context.getAuthentication();
    if (authentication == null
        || authentication.getAuthorities().stream()
            .noneMatch(authority -> "ROLE_SUPER_ADMIN".equals(authority.getAuthority())))
      return null;
    request.getSession(true).setAttribute(LOCAL_SECURITY_CONTEXT, context);
    request.getSession().setAttribute(GOOGLE_OWNER, connection.owner());
    return authorizationRequest;
  }
}
