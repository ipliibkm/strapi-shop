package de.openshopfront.platform.commerce;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Verifies Stripe webhook signatures before the platform trusts an event. */
public final class StripeSignatureVerifier {
  private static final long MAX_AGE_SECONDS = 300L;

  private StripeSignatureVerifier() {}

  /** Returns true when the header, timestamp, and payload match the shared secret. */
  public static boolean verify(String header, String payload, String secret) {
    if (header == null || header.isBlank() || secret == null || secret.isBlank()) return false;
    long timestamp = -1L;
    java.util.Set<String> signatures = new java.util.HashSet<>();
    for (String part : header.split(",")) {
      String trimmed = part.trim();
      if (trimmed.startsWith("t=")) {
        try {
          timestamp = Long.parseLong(trimmed.substring(2));
        } catch (NumberFormatException ignored) {
        }
      } else if (trimmed.startsWith("v1=")) {
        signatures.add(trimmed.substring(3));
      }
    }
    if (timestamp < 0 || signatures.isEmpty()) return false;
    long now = System.currentTimeMillis() / 1000L;
    if (Math.abs(now - timestamp) > MAX_AGE_SECONDS) return false;
    String signedPayload = timestamp + "." + payload;
    for (String expected : signatures) {
      if (constantTimeEquals(computeHmac(signedPayload, secret), expected)) return true;
    }
    return false;
  }

  static String computeHmac(String payload, String secret) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
      StringBuilder hex = new StringBuilder(digest.length * 2);
      for (byte value : digest) hex.append(String.format("%02x", value));
      return hex.toString();
    } catch (java.security.GeneralSecurityException error) {
      throw new IllegalStateException(error);
    }
  }

  private static boolean constantTimeEquals(byte[] a, byte[] b) {
    if (a == null || b == null || a.length != b.length) return false;
    int result = 0;
    for (int i = 0; i < a.length; i++) result |= a[i] ^ b[i];
    return result == 0;
  }

  private static boolean constantTimeEquals(String computed, String expected) {
    if (computed == null || expected == null) return false;
    byte[] left = computed.getBytes(StandardCharsets.UTF_8);
    byte[] right = expected.getBytes(StandardCharsets.UTF_8);
    return MessageDigest.isEqual(left, right);
  }
}
