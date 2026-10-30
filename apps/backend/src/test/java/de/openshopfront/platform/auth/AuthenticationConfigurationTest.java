package de.openshopfront.platform.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/** Verifies the single supported administrator authentication mode. */
class AuthenticationConfigurationTest {
  @Test
  void reportsStrapiAsTheActiveAdministratorMode() {
    assertThat(new AuthConfigController(false).config())
        .containsEntry("enabled", true)
        .containsEntry("mode", "strapi");
  }

  @Test
  void strapiSecurityDoesNotDependOnAnAuthenticationModeSwitch() {
    assertThat(StrapiAdminSecurityConfiguration.class.getAnnotation(ConditionalOnProperty.class))
        .isNull();
  }
}
