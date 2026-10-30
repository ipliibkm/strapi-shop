package de.openshopfront.platform.commerce;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.RequestMatcher;

/** Exposes the anonymous storefront catalog, checkout, order lookup, and Stripe webhook. */
@Configuration
public class PublicCommerceSecurityConfiguration {
  /** Keeps commerce customer requests stateless without weakening administrator CSRF protection. */
  @Bean
  @Order(-1)
  SecurityFilterChain publicCommerceSecurity(HttpSecurity http) throws Exception {
    RequestMatcher endpoints =
        request -> {
          String path = request.getServletPath();
          if (path.startsWith("/api/v1/public/products") && "GET".equals(request.getMethod()))
            return true;
          if (path.startsWith("/api/v1/public/product-images/")
              && "GET".equals(request.getMethod())) return true;
          if ("/api/v1/public/checkout".equals(path) && "POST".equals(request.getMethod()))
            return true;
          if (path.startsWith("/api/v1/public/orders/") && "GET".equals(request.getMethod()))
            return true;
          if ("/webhooks/stripe".equals(path) && "POST".equals(request.getMethod())) return true;
          if (path.startsWith("/api/v1/public/pages") && "GET".equals(request.getMethod()))
            return true;
          if ("/api/v1/public/settings".equals(path) && "GET".equals(request.getMethod()))
            return true;
          return false;
        };
    return http.securityMatcher(endpoints)
        .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .csrf(csrf -> csrf.disable())
        .build();
  }
}
