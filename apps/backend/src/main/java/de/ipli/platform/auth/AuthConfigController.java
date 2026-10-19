package de.ipli.platform.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthConfigController {
 private final String authority;
 private final String clientId;
 private final boolean enabled;
 public AuthConfigController(@Value("${platform.auth.authority:}") String authority,
   @Value("${platform.auth.client-id:}") String clientId,
   @Value("${platform.auth.enabled:false}") boolean enabled) {
  this.authority=authority;this.clientId=clientId;this.enabled=enabled;
 }
 @GetMapping("/config")
 public Map<String,Object> config(){
  return Map.of("enabled",enabled,"authority",enabled?authority:"","clientId",enabled?clientId:"");
 }
}
