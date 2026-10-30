package de.openshopfront.platform.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/** Limits failed administrator credentials without counting successful login and logout cycles. */
@Service
public class AdminLoginAttemptService {
  private static final int MAX_FAILURES = 5;
  private static final Duration WINDOW = Duration.ofMinutes(5);
  private final ConcurrentHashMap<String, Failures> failures = new ConcurrentHashMap<>();
  private final Clock clock;

  public AdminLoginAttemptService() {
    this(Clock.systemUTC());
  }

  AdminLoginAttemptService(Clock clock) {
    this.clock = clock;
  }

  /** Rejects an identity after five failed credentials in the active window. */
  public void check(String email, String address) {
    String key = key(email, address);
    Instant now = clock.instant();
    Failures value = failures.get(key);
    if (value == null) return;
    if (!now.isBefore(value.expiresAt())) {
      failures.remove(key, value);
      return;
    }
    if (value.count() >= MAX_FAILURES) {
      long seconds = Math.max(1, Duration.between(now, value.expiresAt()).toSeconds());
      throw new StrapiAdminClient.TooManyLoginAttemptsException(Long.toString(seconds));
    }
  }

  /** Records one rejected password for the email and client address. */
  public void failed(String email, String address) {
    String key = key(email, address);
    Instant now = clock.instant();
    failures.compute(
        key,
        (ignored, current) ->
            current == null || !now.isBefore(current.expiresAt())
                ? new Failures(1, now.plus(WINDOW))
                : new Failures(current.count() + 1, current.expiresAt()));
  }

  /** Clears prior failures after Strapi accepts the credentials. */
  public void succeeded(String email, String address) {
    failures.remove(key(email, address));
  }

  private static String key(String email, String address) {
    return email.strip().toLowerCase(Locale.ROOT) + '\0' + address;
  }

  private record Failures(int count, Instant expiresAt) {}
}
