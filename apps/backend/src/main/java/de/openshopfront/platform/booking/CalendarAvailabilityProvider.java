package de.openshopfront.platform.booking;

import java.util.Optional;

/** Reports whether one calendar provider can protect a resource from double booking. */
public interface CalendarAvailabilityProvider {
  /** Returns no value when this provider has no mapping for the resource. */
  Optional<Readiness> check(String resourceId);

  /** Describes one provider's current availability protection state. */
  record Readiness(boolean available, String reason) {}
}
