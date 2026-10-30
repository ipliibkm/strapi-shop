package de.openshopfront.platform.commerce;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

/** Verifies the public order lookup request limit. */
class OrderLookupRateLimiterTest {
  @Test
  void rejectsTheTwentyFirstRequestForOneOrderAndAddress() {
    OrderLookupRateLimiter limiter =
        new OrderLookupRateLimiter(
            Clock.fixed(Instant.parse("2026-10-26T10:00:00Z"), ZoneOffset.UTC));
    for (int request = 0; request < 20; request++) limiter.check("SO-123", "192.0.2.1");

    assertThatThrownBy(() -> limiter.check("SO-123", "192.0.2.1"))
        .isInstanceOf(OrderLookupRateLimiter.RateLimitException.class);
  }
}
