package de.openshopfront.platform.commerce;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/** Limits repeated public lookup attempts for one order and network address. */
@Service
public class OrderLookupRateLimiter {
  private static final int MAX_ATTEMPTS = 20;
  private static final Duration WINDOW = Duration.ofMinutes(1);
  private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
  private final Clock clock;

  public OrderLookupRateLimiter() {
    this(Clock.systemUTC());
  }

  OrderLookupRateLimiter(Clock clock) {
    this.clock = clock;
  }

  /** Rejects requests after the configured limit in the current time window. */
  public void check(String orderNumber, String remoteAddress) {
    Instant now = clock.instant();
    String key = String.valueOf(remoteAddress) + ':' + String.valueOf(orderNumber);
    Window window =
        windows.compute(
            key,
            (ignored, current) -> {
              if (current == null || !now.isBefore(current.startedAt.plus(WINDOW)))
                return new Window(now, 1);
              return new Window(current.startedAt, current.attempts + 1);
            });
    if (window.attempts > MAX_ATTEMPTS) throw new RateLimitException();
    if (windows.size() > 10_000)
      windows.entrySet().removeIf(entry -> !now.isBefore(entry.getValue().startedAt.plus(WINDOW)));
  }

  private record Window(Instant startedAt, int attempts) {}

  /** Raised when one client makes too many order lookup requests. */
  public static class RateLimitException extends RuntimeException {}
}
