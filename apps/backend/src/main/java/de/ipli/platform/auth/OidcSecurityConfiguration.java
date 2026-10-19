package de.ipli.platform.auth;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@ConditionalOnProperty(name="platform.auth.enabled",havingValue="true")
public class OidcSecurityConfiguration {
 @Bean JwtDecoder oidcDecoder(@Value("${platform.auth.authority}") String issuer,@Value("${platform.auth.audience}") String audience){
  if(issuer.isBlank() || audience.isBlank()) throw new IllegalStateException("Set the OIDC issuer and API audience.");
  NimbusJwtDecoder decoder=NimbusJwtDecoder.withIssuerLocation(issuer).build();
  OAuth2TokenValidator<Jwt> audienceValidator=jwt->jwt.getAudience().contains(audience)
   ?OAuth2TokenValidatorResult.success()
   :OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token","The API audience is not valid.",null));
  decoder.setJwtValidator(new org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(issuer),audienceValidator));
  return decoder;
 }

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
