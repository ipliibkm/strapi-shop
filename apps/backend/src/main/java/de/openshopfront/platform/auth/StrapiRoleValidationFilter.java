package de.openshopfront.platform.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.filter.OncePerRequestFilter;

/** Reloads Strapi roles before protected requests and fails closed after account changes. */
public class StrapiRoleValidationFilter extends OncePerRequestFilter {
  private final StrapiAdminClient strapi;

  public StrapiRoleValidationFilter(StrapiAdminClient strapi) {
    this.strapi = strapi;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    return !path.startsWith("/api/v1/admin/") && !path.startsWith("/api/v1/content/");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    var current = SecurityContextHolder.getContext().getAuthentication();
    if (current == null || !(current.getPrincipal() instanceof StrapiAdminPrincipal existing)) {
      chain.doFilter(request, response);
      return;
    }
    try {
      StrapiAdminPrincipal principal = strapi.currentPrincipal(request.getSession(false));
      if (!existing.id().equals(principal.id()))
        throw new StrapiAdminClient.StrapiSessionExpiredException();
      var authorities = new ArrayList<SimpleGrantedAuthority>();
      authorities.add(new SimpleGrantedAuthority("ROLE_STRAPI_ADMIN"));
      if (principal.superAdmin()) authorities.add(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
      var authentication =
          UsernamePasswordAuthenticationToken.authenticated(
              principal, null, List.copyOf(authorities));
      var context = SecurityContextHolder.createEmptyContext();
      context.setAuthentication(authentication);
      SecurityContextHolder.setContext(context);
      request
          .getSession()
          .setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
      chain.doFilter(request, response);
    } catch (StrapiAdminClient.StrapiSessionExpiredException
        | org.springframework.web.client.HttpClientErrorException.Unauthorized error) {
      request.getSession(false).invalidate();
      SecurityContextHolder.clearContext();
      response.sendError(HttpStatus.UNAUTHORIZED.value());
    } catch (org.springframework.web.client.RestClientException error) {
      response.sendError(HttpStatus.SERVICE_UNAVAILABLE.value());
    }
  }
}
