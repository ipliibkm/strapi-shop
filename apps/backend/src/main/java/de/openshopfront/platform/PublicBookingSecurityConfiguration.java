package de.openshopfront.platform;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Exposes only the settings, services, availability, and creation operations required by the public
 * booking page.
 */
@Configuration
public class PublicBookingSecurityConfiguration {
  /** Keeps public booking requests stateless without weakening administrator CSRF protection. */
  @Bean
  @Order(-1)
  SecurityFilterChain publicBookingSecurity(HttpSecurity http) throws Exception {
    RequestMatcher endpoints =
        request ->
            ("GET".equals(request.getMethod())
                    && java.util.Set.of(
                            "/api/v1/public/booking-settings",
                            "/api/v1/public/services",
                            "/api/v1/public/availability")
                        .contains(request.getServletPath()))
                || ("POST".equals(request.getMethod())
                    && "/api/v1/public/bookings".equals(request.getServletPath()))
                || (("GET".equals(request.getMethod()) || "POST".equals(request.getMethod()))
                    && request.getServletPath().startsWith("/api/v1/public/forms/"));
    return http.securityMatcher(endpoints)
        .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .csrf(csrf -> csrf.disable())
        .build();
  }
}
