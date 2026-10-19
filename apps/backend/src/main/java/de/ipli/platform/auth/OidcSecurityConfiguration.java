package de.ipli.platform.auth;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@ConditionalOnProperty(name="platform.auth.enabled",havingValue="true")
public class OidcSecurityConfiguration {
 @Bean @Order(1)
 SecurityFilterChain oidcApi(HttpSecurity http) throws Exception {
  JwtGrantedAuthoritiesConverter scopes=new JwtGrantedAuthoritiesConverter();
  scopes.setAuthorityPrefix("ROLE_");
  scopes.setAuthoritiesClaimName("roles");
  JwtAuthenticationConverter converter=new JwtAuthenticationConverter();
  converter.setJwtGrantedAuthoritiesConverter(scopes);
  return http.securityMatcher("/api/v1/**")
    .authorizeHttpRequests(a->a
      .requestMatchers("/api/v1/auth/config").permitAll()
      .requestMatchers("/api/v1/admin/**").hasRole("OPERATOR")
      .anyRequest().authenticated())
    .oauth2ResourceServer(o->o.jwt(j->j.jwtAuthenticationConverter(converter)))
    .sessionManagement(s->s.sessionCreationPolicy(org.springframework.security.config.http.SessionCreationPolicy.STATELESS))
    .csrf(c->c.disable()).build();
 }
}
