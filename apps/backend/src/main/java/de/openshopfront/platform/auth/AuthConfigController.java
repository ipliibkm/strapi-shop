package de.openshopfront.platform.auth;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthConfigController {
  private final boolean googleEnabled;

  public AuthConfigController(@Value("${platform.google.enabled:false}") boolean googleEnabled) {
    this.googleEnabled = googleEnabled;
  }

  /** Returns the public authentication mode without exposing provider secrets. */
  @GetMapping("/config")
  public Map<String, Object> config() {
    return Map.of("enabled", true, "mode", "strapi", "googleCalendarEnabled", googleEnabled);
  }
}
