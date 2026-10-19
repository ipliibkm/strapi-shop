package de.ipli.platform;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
@Configuration
public class SecurityConfiguration {
 @Bean SecurityFilterChain security(HttpSecurity http) throws Exception {
  // Deny application endpoints until OIDC and role-based authorization are wired.
  return http.authorizeHttpRequests(a -> a.requestMatchers("/actuator/health", "/actuator/info").permitAll().anyRequest().denyAll()).build();
 }
}
