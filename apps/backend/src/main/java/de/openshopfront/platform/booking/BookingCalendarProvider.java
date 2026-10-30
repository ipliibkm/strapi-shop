package de.openshopfront.platform.booking;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/** Connects booking changes to one external calendar provider. */
public interface BookingCalendarProvider {
  /** Returns the stable provider identifier used in API and persistence records. */
  String providerId();

  /** Queues publication of a new booking for all applicable mappings. */
  void enqueueCreate(UUID bookingId, String resourceId);

  /** Queues publication of a changed booking for all applicable mappings. */
  void enqueueUpdate(UUID bookingId, String resourceId);

  /** Queues removal of a cancelled booking from external calendars. */
  void enqueueDelete(UUID bookingId);

  /** Returns provider delivery states for the requested bookings. */
  Map<UUID, DeliveryState> deliveryStates(Collection<UUID> bookingIds);

  /** Describes the provider state for one booking delivery. */
  record DeliveryState(String status, String error) {}
}
