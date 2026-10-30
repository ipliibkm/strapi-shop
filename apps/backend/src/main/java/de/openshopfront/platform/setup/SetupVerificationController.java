package de.openshopfront.platform.setup;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** Reports installation checks before setup and protected runtime checks after setup. */
@RestController
public class SetupVerificationController {
  private final NamedParameterJdbcTemplate db;
  private final String googleId;
  private final String stripeKey;
  private final String strapiEncryptionKey;
  private final boolean setupEnabled;

  public SetupVerificationController(
      NamedParameterJdbcTemplate db,
      @Value("${GOOGLE_CLIENT_ID:}") String googleId,
      @Value("${STRIPE_SECRET_KEY:}") String stripeKey,
      @Value("${platform.strapi.token-encryption-key:}") String strapiEncryptionKey,
      @Value("${platform.setup.enabled:true}") boolean setupEnabled) {
    this.db = db;
    this.googleId = googleId;
    this.stripeKey = stripeKey;
    this.strapiEncryptionKey = strapiEncryptionKey;
    this.setupEnabled = setupEnabled;
  }

  public record Check(String id, String status, String message) {}

  /** Returns system checks only while first installation is incomplete. */
  @GetMapping("/setup/api/checks")
  public List<Check> setupChecks() {
    requireSetupEnabled();
    if (installed()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Setup is complete");
    return checks();
  }

  /** Returns runtime system checks to an authenticated Super Admin. */
  @GetMapping("/api/v1/admin/system/checks")
  public List<Check> checks() {
    boolean installed = installed();
    return List.of(
        new Check("database", "ready", "The database responded."),
        new Check(
            "installation",
            installed ? "ready" : "pending",
            installed ? "Business setup is saved." : "Save your business setup."),
        new Check(
            "google",
            googleId.isBlank() ? "not_configured" : "configuration_present",
            googleId.isBlank()
                ? "Google is not configured."
                : "Google client settings exist. Authorization is not verified."),
        new Check(
            "stripe",
            stripeKey.isBlank() ? "not_configured" : "configuration_present",
            stripeKey.isBlank()
                ? "Stripe is not configured."
                : "Stripe settings exist. No payment test has run."),
        new Check(
            "identity",
            strapiEncryptionKey.isBlank() ? "not_configured" : "configuration_present",
            strapiEncryptionKey.isBlank()
                ? "Administrator sign-in is not configured."
                : "Strapi administrator settings exist. Sign-in is not verified."));
  }

  @GetMapping("/setup/api/installation")
  public Map<String, Object> installation(
      @RequestHeader(value = "X-Setup-Token", required = false) String token,
      @Value("${platform.setup.token:}") String configuredToken) {
    requireSetupEnabled();
    if (configuredToken.length() < 32
        || token == null
        || !java.security.MessageDigest.isEqual(
            token.getBytes(java.nio.charset.StandardCharsets.UTF_8),
            configuredToken.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid setup token");
    }
    List<Map<String, Object>> rows =
        db.queryForList(
            "select company_json,modules_json,admin_theme,storefront_theme from installation_state where id=1",
            Map.of());
    if (rows.isEmpty())
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Setup is not complete");
    return rows.get(0);
  }

  private boolean installed() {
    return Boolean.TRUE.equals(
        db.queryForObject(
            "select exists(select 1 from installation_state where id=1)", Map.of(), Boolean.class));
  }

  private void requireSetupEnabled() {
    if (!setupEnabled) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Setup is disabled");
  }
}
