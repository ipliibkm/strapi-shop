package de.openshopfront.platform.auth;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;

/** Stores OAuth authorized clients in PostgreSQL with encrypted token values. */
final class EncryptedJdbcOAuth2AuthorizedClientService implements OAuth2AuthorizedClientService {
  private static final String LOAD =
      """
  select access_token_value,access_token_issued_at,access_token_expires_at,access_token_scopes,
         refresh_token_value,refresh_token_issued_at,refresh_token_expires_at
    from oauth2_authorized_client
   where client_registration_id=? and principal_name=?
  """;
  private static final String SAVE =
      """
  insert into oauth2_authorized_client
   (client_registration_id,principal_name,access_token_value,access_token_issued_at,access_token_expires_at,
    access_token_scopes,refresh_token_value,refresh_token_issued_at,refresh_token_expires_at,updated_at)
  values (?,?,?,?,?,?,?,?,?,now())
  on conflict (client_registration_id,principal_name) do update set
   access_token_value=excluded.access_token_value,
   access_token_issued_at=excluded.access_token_issued_at,
   access_token_expires_at=excluded.access_token_expires_at,
   access_token_scopes=excluded.access_token_scopes,
   refresh_token_value=excluded.refresh_token_value,
   refresh_token_issued_at=excluded.refresh_token_issued_at,
   refresh_token_expires_at=excluded.refresh_token_expires_at,
   updated_at=now()
  """;
  private final JdbcOperations db;
  private final ClientRegistrationRepository registrations;
  private final OAuthTokenCipher cipher;

  /** Creates encrypted persistent authorized-client storage. */
  EncryptedJdbcOAuth2AuthorizedClientService(
      JdbcOperations db, ClientRegistrationRepository registrations, String encodedKey) {
    this.db = db;
    this.registrations = registrations;
    this.cipher = new OAuthTokenCipher(encodedKey);
  }

  /** Loads and decrypts one authorized client for the resource owner. */
  @Override
  public <T extends OAuth2AuthorizedClient> T loadAuthorizedClient(
      String registrationId, String principalName) {
    ClientRegistration registration = registrations.findByRegistrationId(registrationId);
    if (registration == null) return null;
    List<OAuth2AuthorizedClient> clients =
        db.query(
            LOAD,
            (rs, row) -> read(rs, registration, principalName),
            registrationId,
            principalName);
    @SuppressWarnings("unchecked")
    T result = (T) (clients.isEmpty() ? null : clients.getFirst());
    return result;
  }

  /** Encrypts and saves access and refresh tokens for the resource owner. */
  @Override
  public void saveAuthorizedClient(OAuth2AuthorizedClient client, Authentication principal) {
    String registrationId = client.getClientRegistration().getRegistrationId();
    String principalName = principal.getName();
    OAuth2RefreshToken refresh = client.getRefreshToken();
    if (refresh == null) {
      OAuth2AuthorizedClient existing = loadAuthorizedClient(registrationId, principalName);
      if (existing != null) refresh = existing.getRefreshToken();
    }
    OAuth2AccessToken access = client.getAccessToken();
    db.update(
        SAVE,
        registrationId,
        principalName,
        cipher.encrypt(access.getTokenValue(), context(registrationId, principalName, "access")),
        utc(access.getIssuedAt()),
        utc(access.getExpiresAt()),
        access.getScopes().stream().sorted().collect(Collectors.joining(" ")),
        refresh == null
            ? null
            : cipher.encrypt(
                refresh.getTokenValue(), context(registrationId, principalName, "refresh")),
        refresh == null ? null : utc(refresh.getIssuedAt()),
        refresh == null ? null : utc(refresh.getExpiresAt()));
  }

  /** Removes persisted credentials after the resource owner revokes access. */
  @Override
  public void removeAuthorizedClient(String registrationId, String principalName) {
    db.update(
        "delete from oauth2_authorized_client where client_registration_id=? and principal_name=?",
        registrationId,
        principalName);
  }

  private OAuth2AuthorizedClient read(
      ResultSet rs, ClientRegistration registration, String principalName) throws SQLException {
    String registrationId = registration.getRegistrationId();
    Set<String> scopes = scopes(rs.getString("access_token_scopes"));
    OAuth2AccessToken access =
        new OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER,
            cipher.decrypt(
                rs.getBytes("access_token_value"),
                context(registrationId, principalName, "access")),
            instant(rs, "access_token_issued_at"),
            instant(rs, "access_token_expires_at"),
            scopes);
    byte[] encryptedRefresh = rs.getBytes("refresh_token_value");
    OAuth2RefreshToken refresh =
        encryptedRefresh == null
            ? null
            : new OAuth2RefreshToken(
                cipher.decrypt(encryptedRefresh, context(registrationId, principalName, "refresh")),
                instant(rs, "refresh_token_issued_at"),
                instant(rs, "refresh_token_expires_at"));
    return new OAuth2AuthorizedClient(registration, principalName, access, refresh);
  }

  private static Set<String> scopes(String value) {
    if (value == null || value.isBlank()) return Set.of();
    return Arrays.stream(value.split(" "))
        .filter(scope -> !scope.isBlank())
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  private static OffsetDateTime utc(Instant value) {
    return value == null ? null : value.atOffset(ZoneOffset.UTC);
  }

  private static Instant instant(ResultSet rs, String column) throws SQLException {
    OffsetDateTime value = rs.getObject(column, OffsetDateTime.class);
    return value == null ? null : value.toInstant();
  }

  private static String context(String registrationId, String principalName, String tokenType) {
    return registrationId + '\0' + principalName + '\0' + tokenType;
  }
}
