package de.ipli.platform.setup;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** No credentials or personal data appear in these read-only checks. */
@RestController
@RequestMapping("/setup/api")
public class SetupVerificationController {
 private final NamedParameterJdbcTemplate db;
 private final String googleId;
 private final String stripeKey;
 private final String oidcIssuer;
 public SetupVerificationController(NamedParameterJdbcTemplate db,
   @Value("${GOOGLE_CLIENT_ID:}") String googleId,
   @Value("${STRIPE_SECRET_KEY:}") String stripeKey,
   @Value("${PLATFORM_OIDC_ISSUER:}") String oidcIssuer) {
  this.db=db;this.googleId=googleId;this.stripeKey=stripeKey;this.oidcIssuer=oidcIssuer;
 }
 public record Check(String id,String status,String message){}
 @GetMapping("/checks")
 public List<Check> checks() {
  boolean installed=Boolean.TRUE.equals(db.queryForObject(
    "select exists(select 1 from installation_state where id=1)",Map.of(),Boolean.class));
  return List.of(
    new Check("database","ready","The database responded."),
    new Check("installation",installed?"ready":"pending",installed?"Business setup is saved.":"Save your business setup."),
    new Check("google",googleId.isBlank()?"not_configured":"configuration_present",googleId.isBlank()?"Google is not configured.":"Google client settings exist. Authorization is not verified."),
    new Check("stripe",stripeKey.isBlank()?"not_configured":"configuration_present",stripeKey.isBlank()?"Stripe is not configured.":"Stripe settings exist. No payment test has run."),
    new Check("identity",oidcIssuer.isBlank()?"not_configured":"configuration_present",oidcIssuer.isBlank()?"Administrator sign-in is not configured.":"Identity settings exist. Sign-in is not verified.")
  );
 }
 @GetMapping("/installation")
 public Map<String,Object> installation(@RequestHeader(value="X-Setup-Token",required=false) String token,
    @Value("${platform.setup.token:}") String configuredToken) {
  if(configuredToken.length()<32||token==null||
     !java.security.MessageDigest.isEqual(token.getBytes(java.nio.charset.StandardCharsets.UTF_8),
      configuredToken.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid setup token");
  }
  List<Map<String,Object>> rows=db.queryForList("select company_json,modules_json,admin_theme,storefront_theme from installation_state where id=1",Map.of());
  if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Setup is not complete");
  return rows.get(0);
 }
}
