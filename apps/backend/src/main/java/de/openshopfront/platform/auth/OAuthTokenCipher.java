package de.openshopfront.platform.auth;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/** Encrypts persisted OAuth credentials with versioned AES-256-GCM payloads. */
final class OAuthTokenCipher {
  private static final byte VERSION = 1;
  private static final int IV_LENGTH = 12;
  private static final int TAG_LENGTH_BITS = 128;
  private final SecretKeySpec key;
  private final SecureRandom random = new SecureRandom();

  /** Creates a cipher from a base64-encoded 256-bit key. */
  OAuthTokenCipher(String encodedKey) {
    byte[] decoded = decodeKey(encodedKey);
    if (decoded.length != 32)
      throw new IllegalStateException(
          "PLATFORM_OAUTH_TOKEN_ENCRYPTION_KEY must contain a base64-encoded 32-byte key.");
    key = new SecretKeySpec(decoded, "AES");
  }

  /** Encrypts a token and binds it to its registration, principal, and token type. */
  byte[] encrypt(String value, String context) {
    try {
      byte[] iv = new byte[IV_LENGTH];
      random.nextBytes(iv);
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
      cipher.updateAAD(context.getBytes(StandardCharsets.UTF_8));
      byte[] ciphertext = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
      return ByteBuffer.allocate(1 + iv.length + ciphertext.length)
          .put(VERSION)
          .put(iv)
          .put(ciphertext)
          .array();
    } catch (GeneralSecurityException error) {
      throw new IllegalStateException("OAuth token encryption failed.", error);
    }
  }

  /** Decrypts a token after authenticating its ciphertext and storage context. */
  String decrypt(byte[] payload, String context) {
    if (payload == null || payload.length <= 1 + IV_LENGTH || payload[0] != VERSION)
      throw new IllegalStateException("The stored OAuth token format is not valid.");
    try {
      byte[] iv = java.util.Arrays.copyOfRange(payload, 1, 1 + IV_LENGTH);
      byte[] ciphertext = java.util.Arrays.copyOfRange(payload, 1 + IV_LENGTH, payload.length);
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
      cipher.updateAAD(context.getBytes(StandardCharsets.UTF_8));
      return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
    } catch (GeneralSecurityException error) {
      throw new IllegalStateException(
          "The stored OAuth token cannot be decrypted. Check the token encryption key.", error);
    }
  }

  private static byte[] decodeKey(String encodedKey) {
    if (encodedKey == null || encodedKey.isBlank())
      throw new IllegalStateException(
          "Set PLATFORM_OAUTH_TOKEN_ENCRYPTION_KEY before you enable Google OAuth.");
    try {
      return Base64.getDecoder().decode(encodedKey.strip());
    } catch (IllegalArgumentException standardError) {
      try {
        return Base64.getUrlDecoder().decode(encodedKey.strip());
      } catch (IllegalArgumentException urlError) {
        throw new IllegalStateException(
            "PLATFORM_OAUTH_TOKEN_ENCRYPTION_KEY is not valid base64.", urlError);
      }
    }
  }
}
