package de.openshopfront.platform.auth;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistration.Builder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;

/** Configures persistent Google authorization for platform services. */
@Configuration
@ConditionalOnProperty(name = "platform.google.enabled", havingValue = "true")
public class GoogleServiceOAuthConfiguration {
  /** Registers the Google OIDC client and the minimum service scopes. */
  @Bean
  ClientRegistrationRepository googleClientRegistration(
      @Value("${platform.google.client-id}") String clientId,
      @Value("${platform.google.client-secret}") String clientSecret,
      @Value("${platform.admin.base-url}") String baseUrl) {
    if (clientId.isBlank() || clientSecret.isBlank())
      throw new IllegalStateException("Set GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET.");
    Builder builder = CommonOAuth2Provider.GOOGLE.getBuilder("google");
    ClientRegistration registration =
        builder
            .clientId(clientId)
            .clientSecret(clientSecret)
            .redirectUri(baseUrl + "/login/oauth2/code/{registrationId}")
            .scope(
                "openid",
                "profile",
                "email",
                GoogleAccessTokenService.CALENDAR_EVENTS_SCOPE,
                GoogleAccessTokenService.CALENDAR_LIST_SCOPE,
                GoogleAccessTokenService.GMAIL_SEND_SCOPE)
            .clientName("Google Calendar and Gmail")
            .build();
    return new InMemoryClientRegistrationRepository(registration);
  }

  /** Keeps encrypted OAuth credentials in PostgreSQL across backend restarts. */
  @Bean
  OAuth2AuthorizedClientService googleAuthorizedClients(
      ClientRegistrationRepository registrations,
      JdbcOperations db,
      @Value("${platform.google.token-encryption-key}") String tokenEncryptionKey) {
    return new EncryptedJdbcOAuth2AuthorizedClientService(db, registrations, tokenEncryptionKey);
  }

  /** Refreshes stored Google access tokens for scheduled and request-driven work. */
  @Bean
  OAuth2AuthorizedClientManager googleAuthorizedClientManager(
      ClientRegistrationRepository registrations, OAuth2AuthorizedClientService clients) {
    OAuth2AuthorizedClientProvider provider =
        OAuth2AuthorizedClientProviderBuilder.builder().refreshToken().build();
    var manager = new AuthorizedClientServiceOAuth2AuthorizedClientManager(registrations, clients);
    manager.setAuthorizedClientProvider(provider);
    return manager;
  }

  /** Requests explicit offline consent so Google can issue a refresh token. */
  @Bean
  OAuth2AuthorizationRequestResolver googleAuthorizationRequestResolver(
      ClientRegistrationRepository registrations, GoogleServiceConnection connection) {
    DefaultOAuth2AuthorizationRequestResolver resolver =
        new DefaultOAuth2AuthorizationRequestResolver(registrations, "/oauth2/authorization");
    resolver.setAuthorizationRequestCustomizer(
        request ->
            request.additionalParameters(
                Map.of(
                    "access_type",
                    "offline",
                    "prompt",
                    "consent",
                    "include_granted_scopes",
                    "true")));
    return new GoogleConnectionAuthorizationRequestResolver(resolver, connection);
  }
}
