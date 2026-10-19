package de.ipli.platform.setup;

import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.UUID;
import java.time.Instant;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;

/**
 * Local, one-time bootstrap endpoint.
 * Keep this service behind a loopback-only reverse proxy.
 */
@RestController
@RequestMapping("/setup/api")
public class SetupController {
 private final NamedParameterJdbcTemplate jdbc;
 private final PasswordEncoder encoder;
 private final String bootstrapToken;
 private static final Set<String> MODULES=Set.of("website","commerce","booking","calendar","gmail");
 private static final Set<String> THEMES=Set.of("light","dark","ocean");
 public SetupController(NamedParameterJdbcTemplate jdbc, PasswordEncoder encoder,
       @Value("${platform.setup.token:}") String token) {
  this.jdbc=jdbc;this.encoder=encoder;this.bootstrapToken=token;
 }
 public record Company(String name,String contactEmail,String timezone,String locale,String currency,String address,String phone){}
 public record Admin(String email,String password){}
 public record Request(Company company,Admin admin,List<String> modules,String adminTheme,String storefrontTheme){}
 public record State(boolean complete,boolean tokenConfigured,String message){}
 private boolean completed(){
  return Boolean.TRUE.equals(jdbc.queryForObject("select exists(select 1 from installation_state where id=1)",
      Map.of(),Boolean.class));
 }
 private void authorize(String supplied) {
  if(bootstrapToken.length()<32 || supplied==null ||
     !java.security.MessageDigest.isEqual(bootstrapToken.getBytes(java.nio.charset.StandardCharsets.UTF_8),
        supplied.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
    throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED,"Invalid setup token");
  }
 }
 @GetMapping("/status")
 public State status(){
  boolean complete=completed();
  return new State(complete,bootstrapToken.length()>=32,complete?"Setup is complete.":"Use the local setup token to continue.");
 }
 @PostMapping("/complete")
 @Transactional
 public ResponseEntity<?> complete(@RequestHeader(value="X-Setup-Token",required=false) String token,@RequestBody Request request){
  authorize(token);
  // Serialize setup requests across concurrent clients.
  jdbc.getJdbcTemplate().execute("select pg_advisory_xact_lock(87005431)");
  if(completed())return ResponseEntity.status(409).body(Map.of("code","SETUP_COMPLETE","message","Setup is complete."));
  if(request==null||request.company()==null||request.admin()==null) return invalid("Enter the company and administrator details.");
  var c=request.company();var a=request.admin();
  if(blank(c.name())||c.name().length()>160||blank(c.contactEmail())||!c.contactEmail().contains("@")
    ||blank(c.timezone())||blank(c.locale())||blank(c.currency())||blank(a.email())
    ||!a.email().contains("@")||a.password()==null||a.password().length()<14)
    return invalid("Check the company fields and use an administrator password with at least 14 characters.");
  try {java.time.ZoneId.of(c.timezone());}catch(Exception e){return invalid("Select a valid IANA time zone.");}
  if(!c.currency().matches("[A-Z]{3}")||!c.locale().matches("[a-z]{2}(-[A-Z]{2})?"))return invalid("Check the language and currency codes.");
  if(request.modules()==null||request.modules().isEmpty()||!MODULES.containsAll(request.modules())||!request.modules().contains("website")
   ||!THEMES.contains(request.adminTheme())||!THEMES.contains(request.storefrontTheme()))return invalid("Select valid modules and themes.");
  UUID adminId=UUID.randomUUID();
  jdbc.update("insert into initial_operator(id,email,password_hash,created_at) values (:id,:email,:hash,:now)",
    Map.of("id",adminId,"email",a.email().strip().toLowerCase(),"hash",encoder.encode(a.password()),"now",Instant.now()));
  jdbc.update("insert into installation_state(id,company_json,modules_json,admin_theme,storefront_theme,created_at) values (1,cast(:company as jsonb),cast(:modules as jsonb),:adminTheme,:storefrontTheme,:now)",
   Map.of("company",toJson(c),"modules",toJson(request.modules()),"adminTheme",request.adminTheme(),"storefrontTheme",request.storefrontTheme(),"now",Instant.now()));
  return ResponseEntity.status(201).body(Map.of("code","SETUP_SAVED","message","Setup is saved. Configure sign-in before you use the admin app."));
 }
 private String toJson(Object value) {try{return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(value);}catch(Exception e){throw new IllegalStateException(e);}}
 private static boolean blank(String s){return s==null||s.isBlank();}
 private static ResponseEntity<?> invalid(String message){return ResponseEntity.badRequest().body(Map.of("code","INVALID_SETUP","message",message));}
}
