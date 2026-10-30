package de.openshopfront.platform.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;

/** Verifies encrypted authorized-client serialization without exposing token values. */
class EncryptedJdbcOAuth2AuthorizedClientServiceTest {
  @Test
  void savesEncryptedTokensAndReconstructsTheAuthorizedClient() throws Exception {
    JdbcOperations db = mock(JdbcOperations.class);
    ClientRegistrationRepository registrations = mock(ClientRegistrationRepository.class);
    ClientRegistration registration = registration();
    when(registrations.findByRegistrationId("google")).thenReturn(registration);
    Authentication principal = mock(Authentication.class);
    when(principal.getName()).thenReturn("google-subject");
    Instant issuedAt = Instant.parse("2026-10-19T20:00:00Z");
    Instant expiresAt = issuedAt.plusSeconds(3600);
    OAuth2AuthorizedClient original =
        new OAuth2AuthorizedClient(
            registration,
            "google-subject",
            new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "access-secret",
                issuedAt,
                expiresAt,
                Set.of("openid", "calendar")),
            new OAuth2RefreshToken("refresh-secret", issuedAt));
    var service = new EncryptedJdbcOAuth2AuthorizedClientService(db, registrations, key());

    service.saveAuthorizedClient(original, principal);

    ArgumentCaptor<Object[]> values = ArgumentCaptor.forClass(Object[].class);
    verify(db).update(anyString(), values.capture());
    Object[] stored = values.getValue();
    assertFalse(new String((byte[]) stored[2], StandardCharsets.UTF_8).contains("access-secret"));
    assertFalse(new String((byte[]) stored[6], StandardCharsets.UTF_8).contains("refresh-secret"));

    java.sql.ResultSet result = mock(java.sql.ResultSet.class);
    when(result.getBytes("access_token_value")).thenReturn((byte[]) stored[2]);
    when(result.getObject("access_token_issued_at", OffsetDateTime.class))
        .thenReturn((OffsetDateTime) stored[3]);
    when(result.getObject("access_token_expires_at", OffsetDateTime.class))
        .thenReturn((OffsetDateTime) stored[4]);
    when(result.getString("access_token_scopes")).thenReturn((String) stored[5]);
    when(result.getBytes("refresh_token_value")).thenReturn((byte[]) stored[6]);
    when(result.getObject("refresh_token_issued_at", OffsetDateTime.class))
        .thenReturn((OffsetDateTime) stored[7]);
    when(result.getObject("refresh_token_expires_at", OffsetDateTime.class)).thenReturn(null);
    when(db.query(
            anyString(),
            org.mockito.ArgumentMatchers.<RowMapper<OAuth2AuthorizedClient>>any(),
            any(Object[].class)))
        .thenAnswer(
            invocation ->
                List.of(
                    invocation
                        .<RowMapper<OAuth2AuthorizedClient>>getArgument(1)
                        .mapRow(result, 0)));

    OAuth2AuthorizedClient restored = service.loadAuthorizedClient("google", "google-subject");
    assertEquals("access-secret", restored.getAccessToken().getTokenValue());
    assertEquals("refresh-secret", restored.getRefreshToken().getTokenValue());
    assertEquals(Set.of("openid", "calendar"), restored.getAccessToken().getScopes());
  }

  private static ClientRegistration registration() {
    return ClientRegistration.withRegistrationId("google")
        .clientId("client")
        .clientSecret("secret")
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .redirectUri("http://localhost/callback")
        .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
        .tokenUri("https://oauth2.googleapis.com/token")
        .userInfoUri("https://openidconnect.googleapis.com/v1/userinfo")
        .userNameAttributeName("sub")
        .clientName("Google")
        .build();
  }

  private static String key() {
    byte[] value = new byte[32];
    new SecureRandom().nextBytes(value);
    return Base64.getEncoder().encodeToString(value);
  }
}
