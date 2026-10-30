package de.openshopfront.platform.booking;

import java.util.List;
import org.springframework.stereotype.Service;

/** Combines calendar provider health before the platform accepts public bookings. */
@Service
public class PublicBookingReadinessService {
  private final List<CalendarAvailabilityProvider> providers;

  public PublicBookingReadinessService(List<CalendarAvailabilityProvider> providers) {
    this.providers = List.copyOf(providers);
  }

  /** Returns whether every calendar mapped to the resource is current and authorized. */
  public Readiness check(String resourceId) {
    boolean mapped = false;
    for (CalendarAvailabilityProvider provider : providers) {
      var current = provider.check(resourceId);
      if (current.isEmpty()) continue;
      mapped = true;
      if (!current.get().available()) return unavailable(current.get().reason());
    }
    return mapped
        ? new Readiness(true, null)
        : unavailable("Online booking requires a connected calendar.");
  }

  /** Rejects a public booking operation if external calendar data is not ready. */
  public void requireAvailable(String resourceId) {
    Readiness readiness = check(resourceId);
    if (!readiness.available()) throw new PublicBookingUnavailableException(readiness.reason());
  }

  private static Readiness unavailable(String reason) {
    return new Readiness(false, reason);
  }

  /** Describes the effective calendar-backed public booking state. */
  public record Readiness(boolean available, String reason) {}

  /** Indicates that external calendars cannot currently protect public availability. */
  public static class PublicBookingUnavailableException extends RuntimeException {
    public PublicBookingUnavailableException(String message) {
      super(message);
    }
  }
}
