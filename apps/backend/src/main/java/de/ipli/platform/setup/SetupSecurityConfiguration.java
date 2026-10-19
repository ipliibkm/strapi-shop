package de.ipli.platform.setup;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SetupSecurityConfiguration {
 @Bean PasswordEncoder setupPasswordEncoder(){return new BCryptPasswordEncoder(12);}
 @Bean @Order(0) SecurityFilterChain setupSecurity(HttpSecurity http) throws Exception {
  // The reverse proxy MUST bind the setup API to loopback.
  // The token is required for writes. No cookie-based authentication is used here.
  return http.securityMatcher("/setup/api/**").csrf(c->c.disable())
    .authorizeHttpRequests(a->a.requestMatchers("/setup/api/status").permitAll()
      .requestMatchers("/setup/api/complete").permitAll().anyRequest().denyAll()).build();
 }
}
