package de.openshopfront.platform.auth;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

/** Secures the admin API with local Strapi accounts and Strapi role boundaries. */
@Configuration
public class StrapiAdminSecurityConfiguration {
  /** Revalidates active Strapi roles before each protected platform request. */
  @Bean
  StrapiRoleValidationFilter strapiRoleValidationFilter(StrapiAdminClient strapi) {
    return new StrapiRoleValidationFilter(strapi);
  }

  /** Creates the Angular CSRF cookie with environment-specific transport security. */
  @Bean
  CookieCsrfTokenRepository adminCsrfTokenRepository(
      @Value("${server.servlet.session.cookie.secure:false}") boolean secureCookies) {
    CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
    repository.setCookieCustomizer(
        cookie -> cookie.secure(secureCookies).sameSite("Lax").path("/"));
    return repository;
  }

  @Bean
  @Order(0)
  SecurityFilterChain strapiAdminSecurity(
      HttpSecurity http,
      StrapiAdminClient strapi,
      StrapiRoleValidationFilter roleValidation,
      ObjectProvider<OAuth2AuthorizationRequestResolver> googleResolver,
      ObjectProvider<OAuth2AuthorizedClientService> googleClients,
      ObjectProvider<GoogleServiceConnection> googleConnection,
      CookieCsrfTokenRepository csrf,
      @Value("${platform.google.enabled:false}") boolean googleEnabled,
      @Value("${platform.admin.base-url}") String baseUrl)
      throws Exception {
    http.securityMatcher("/api/v1/**", "/oauth2/**", "/login/oauth2/**")
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(
                        "/api/v1/auth/config",
                        "/api/v1/auth/session",
                        "/api/v1/auth/csrf",
                        "/api/v1/auth/login")
                    .permitAll()
                    .requestMatchers("/api/v1/content/**")
                    .hasRole("STRAPI_ADMIN")
                    .requestMatchers("/api/v1/admin/**")
                    .hasRole("SUPER_ADMIN")
                    .anyRequest()
                    .hasRole("SUPER_ADMIN"))
        .logout(
            logout ->
                logout
                    .logoutUrl("/api/v1/auth/logout")
                    .addLogoutHandler(
                        (request, response, authentication) -> {
                          HttpSession session = request.getSession(false);
                          if (session != null) strapi.logout(session);
                        })
                    .logoutSuccessHandler(
                        (request, response, authentication) ->
                            response.setStatus(HttpStatus.NO_CONTENT.value()))
                    .deleteCookies("SESSION"))
        .exceptionHandling(
            errors ->
                errors.defaultAuthenticationEntryPointFor(
                    new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                    request -> request.getRequestURI().startsWith("/api/")))
        .sessionManagement(
            session ->
                session
                    .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                    .sessionFixation(fixation -> fixation.migrateSession()))
        .csrf(
            config ->
                config
                    .csrfTokenRepository(csrf)
                    .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()))
        .addFilterBefore(roleValidation, AuthorizationFilter.class);
    if (googleEnabled) {
      OAuth2AuthorizedClientService clients = googleClients.getObject();
      GoogleServiceConnection connection = googleConnection.getObject();
      http.oauth2Login(
          oauth ->
              oauth
                  .authorizationEndpoint(
                      endpoint -> endpoint.authorizationRequestResolver(googleResolver.getObject()))
                  .successHandler(
                      (request, response, authentication) -> {
                        OAuth2AuthenticationToken google =
                            (OAuth2AuthenticationToken) authentication;
                        var client =
                            clients
                                .<org.springframework.security.oauth2.client.OAuth2AuthorizedClient>
                                    loadAuthorizedClient("google", google.getName());
                        HttpSession session = request.getSession(false);
                        Object selectedOwner =
                            session == null
                                ? null
                                : session.getAttribute(
                                    GoogleConnectionAuthorizationRequestResolver.GOOGLE_OWNER);
                        String owner =
                            selectedOwner instanceof String value
                                ? value
                                : GoogleServiceConnection.DEFAULT_OWNER;
                        var servicePrincipal =
                            org.springframework.security.authentication
                                .UsernamePasswordAuthenticationToken.authenticated(
                                owner, "", java.util.List.of());
                        clients.saveAuthorizedClient(client, servicePrincipal);
                        if (!owner.equals(google.getName()))
                          clients.removeAuthorizedClient("google", google.getName());
                        connection.connected(
                            owner, String.valueOf(google.getPrincipal().getAttribute("email")));
                        restoreLocalAuthentication(request);
                        if (session != null)
                          session.removeAttribute(
                              GoogleConnectionAuthorizationRequestResolver.GOOGLE_OWNER);
                        response.sendRedirect(baseUrl + "/#integrations");
                      })
                  .failureHandler(
                      (request, response, error) -> {
                        restoreLocalAuthentication(request);
                        response.sendRedirect(baseUrl + "/?google=failed#integrations");
                      }));
    }
    return http.build();
  }

  private static void restoreLocalAuthentication(jakarta.servlet.http.HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    if (session == null) return;
    Object value =
        session.getAttribute(GoogleConnectionAuthorizationRequestResolver.LOCAL_SECURITY_CONTEXT);
    if (value instanceof org.springframework.security.core.context.SecurityContext context) {
      SecurityContextHolder.setContext(context);
      session.setAttribute(
          HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
      session.removeAttribute(GoogleConnectionAuthorizationRequestResolver.LOCAL_SECURITY_CONTEXT);
    }
  }
}
