package de.ipli.platform;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

// Explicit opt-in local development only; never activate on public production instances.
@Configuration
@Profile("dev")
public class DevSecurityConfiguration {
 @Bean @Order(1) SecurityFilterChain devApi(HttpSecurity http) throws Exception {
  return http.securityMatcher("/api/v1/**")
   .csrf(csrf->csrf.disable())
   .authorizeHttpRequests(a->a.requestMatchers("/api/v1/availability").permitAll().anyRequest().hasRole("OPERATOR"))
   .httpBasic(Customizer.withDefaults()).build();
 }
 @Bean UserDetailsService devUsers(org.springframework.core.env.Environment environment) {
  String password=environment.getProperty("DEV_ADMIN_PASSWORD");
  if(password==null || password.length()<16) throw new IllegalStateException("DEV_ADMIN_PASSWORD must contain at least 16 characters");
  return new InMemoryUserDetailsManager(User.withUsername("local-admin").password("{noop}"+password).roles("OPERATOR").build());
 }
}
