package de.openshopfront.platform.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import org.junit.jupiter.api.Test;

/** Verifies authenticated encryption for persisted OAuth credentials. */
class OAuthTokenCipherTest {
  @Test
  void encryptsWithRandomNoncesAndAuthenticatesContext() {
    OAuthTokenCipher cipher = new OAuthTokenCipher(key());
    byte[] first = cipher.encrypt("refresh-token", "google\0subject\0refresh");
    byte[] second = cipher.encrypt("refresh-token", "google\0subject\0refresh");

    assertFalse(java.util.Arrays.equals(first, second));
    assertFalse(new String(first, StandardCharsets.UTF_8).contains("refresh-token"));
    assertEquals("refresh-token", cipher.decrypt(first, "google\0subject\0refresh"));
    assertThrows(
        IllegalStateException.class, () -> cipher.decrypt(first, "google\0other\0refresh"));
  }

  @Test
  void rejectsMissingOrIncorrectKeyMaterial() {
    assertThrows(IllegalStateException.class, () -> new OAuthTokenCipher(""));
    assertThrows(
        IllegalStateException.class,
        () -> new OAuthTokenCipher(Base64.getEncoder().encodeToString(new byte[16])));
  }

  private static String key() {
    byte[] value = new byte[32];
    new SecureRandom().nextBytes(value);
    return Base64.getEncoder().encodeToString(value);
  }
}
