package de.openshopfront.platform.commerce;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

/** Verifies webhook signature acceptance and replay rejection. */
class StripeSignatureVerifierTest {
  private static final String SECRET = "whsec_test_123";
  private static final String PAYLOAD =
      "{\"id\":\"evt_1\",\"type\":\"checkout.session.completed\"}";

  @Test
  void acceptsValidSignature() {
    long timestamp = System.currentTimeMillis() / 1000L;
    String signature = header(timestamp);
    assertTrue(StripeSignatureVerifier.verify(signature, PAYLOAD, SECRET));
  }

  @Test
  void rejectsWrongSecretAndOldTimestamps() {
    long now = System.currentTimeMillis() / 1000L;
    assertTrue(StripeSignatureVerifier.verify(header(now), PAYLOAD, SECRET));
    assertFalse(StripeSignatureVerifier.verify(header(now), PAYLOAD, "whsec_other"));
    assertFalse(StripeSignatureVerifier.verify(header(now - 3600), PAYLOAD, SECRET));
    assertFalse(StripeSignatureVerifier.verify(null, PAYLOAD, SECRET));
  }

  private static String header(long timestamp) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      byte[] digest = mac.doFinal((timestamp + "." + PAYLOAD).getBytes(StandardCharsets.UTF_8));
      StringBuilder hex = new StringBuilder();
      for (byte value : digest) hex.append(String.format("%02x", value));
      return "t=" + timestamp + ",v1=" + hex;
    } catch (java.security.GeneralSecurityException error) {
      throw new IllegalStateException(error);
    }
  }
}
