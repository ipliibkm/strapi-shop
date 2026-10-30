package de.openshopfront.platform;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/** Provides the deny-by-default security policy when no API authentication mode is active. */
@Configuration
public class SecurityConfiguration {
  /**
   * Exposes health and client authentication metadata while denying protected application
   * operations.
   */
  @Bean
  @Order(2)
  SecurityFilterChain security(HttpSecurity http) throws Exception {
    return http.authorizeHttpRequests(
            a ->
                a.requestMatchers(
                        // /error must stay reachable so Spring can render the error response of an
                        // unhandled exception instead of denying it and losing the status and body.
                        "/actuator/health",
                        "/actuator/info",
                        "/actuator/prometheus",
                        "/api/v1/auth/config",
                        "/error")
                    .permitAll()
                    .anyRequest()
                    .denyAll())
        .build();
  }
}
