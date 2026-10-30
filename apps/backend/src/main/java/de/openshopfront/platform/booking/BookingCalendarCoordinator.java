package de.openshopfront.platform.booking;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Coordinates booking publication without coupling booking logic to a provider. */
@Service
public class BookingCalendarCoordinator {
  private final List<BookingCalendarProvider> providers;

  public BookingCalendarCoordinator(List<BookingCalendarProvider> providers) {
    this.providers = List.copyOf(providers);
  }

  /** Queues a new booking in each configured calendar provider. */
  public void bookingCreated(UUID bookingId, String resourceId) {
    providers.forEach(provider -> provider.enqueueCreate(bookingId, resourceId));
  }

  /** Queues an updated booking in each configured calendar provider. */
  public void bookingUpdated(UUID bookingId, String resourceId) {
    providers.forEach(provider -> provider.enqueueUpdate(bookingId, resourceId));
  }

  /** Queues a cancelled booking in each configured calendar provider. */
  public void bookingCancelled(UUID bookingId) {
    providers.forEach(provider -> provider.enqueueDelete(bookingId));
  }

  /** Returns one aggregate delivery state for each requested booking. */
  public Map<UUID, DeliveryState> deliveryStates(Collection<UUID> bookingIds) {
    Map<UUID, DeliveryState> result = new LinkedHashMap<>();
    for (BookingCalendarProvider provider : providers) {
      provider
          .deliveryStates(bookingIds)
          .forEach(
              (bookingId, state) ->
                  result.merge(
                      bookingId,
                      new DeliveryState(provider.providerId(), state.status(), state.error()),
                      BookingCalendarCoordinator::merge));
    }
    return result;
  }

  private static DeliveryState merge(DeliveryState first, DeliveryState second) {
    String provider = first.provider() + "," + second.provider();
    String status =
        priority(first.status()) >= priority(second.status()) ? first.status() : second.status();
    String error = first.error() != null ? first.error() : second.error();
    return new DeliveryState(provider, status, error);
  }

  private static int priority(String status) {
    return switch (status) {
      case "FAILED" -> 4;
      case "RETRY" -> 3;
      case "PENDING" -> 2;
      case "SYNCED" -> 1;
      default -> 0;
    };
  }

  /** Describes the combined external calendar delivery state for one booking. */
  public record DeliveryState(String provider, String status, String error) {}
}
