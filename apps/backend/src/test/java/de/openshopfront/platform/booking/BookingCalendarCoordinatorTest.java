package de.openshopfront.platform.booking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Verifies provider fan-out and aggregate booking delivery state. */
@ExtendWith(MockitoExtension.class)
class BookingCalendarCoordinatorTest {
  @Mock BookingCalendarProvider google;
  @Mock BookingCalendarProvider microsoft;

  @Test
  void publishesBookingChangesToEveryProvider() {
    UUID bookingId = UUID.randomUUID();
    var coordinator = new BookingCalendarCoordinator(List.of(google, microsoft));

    coordinator.bookingCreated(bookingId, "room-1");
    coordinator.bookingUpdated(bookingId, "room-1");
    coordinator.bookingCancelled(bookingId);

    verify(google).enqueueCreate(bookingId, "room-1");
    verify(microsoft).enqueueCreate(bookingId, "room-1");
    verify(google).enqueueUpdate(bookingId, "room-1");
    verify(microsoft).enqueueUpdate(bookingId, "room-1");
    verify(google).enqueueDelete(bookingId);
    verify(microsoft).enqueueDelete(bookingId);
  }

  @Test
  void reportsTheMostImportantProviderState() {
    UUID bookingId = UUID.randomUUID();
    when(google.providerId()).thenReturn("google");
    when(microsoft.providerId()).thenReturn("microsoft");
    when(google.deliveryStates(List.of(bookingId)))
        .thenReturn(Map.of(bookingId, new BookingCalendarProvider.DeliveryState("SYNCED", null)));
    when(microsoft.deliveryStates(List.of(bookingId)))
        .thenReturn(
            Map.of(
                bookingId,
                new BookingCalendarProvider.DeliveryState("FAILED", "Authorization failed.")));

    var state =
        new BookingCalendarCoordinator(List.of(google, microsoft))
            .deliveryStates(List.of(bookingId))
            .get(bookingId);

    assertEquals("google,microsoft", state.provider());
    assertEquals("FAILED", state.status());
    assertEquals("Authorization failed.", state.error());
  }
}
