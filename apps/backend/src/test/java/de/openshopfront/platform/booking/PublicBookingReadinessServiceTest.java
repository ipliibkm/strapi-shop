package de.openshopfront.platform.booking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Verifies readiness composition across optional calendar providers. */
@ExtendWith(MockitoExtension.class)
class PublicBookingReadinessServiceTest {
  @Mock CalendarAvailabilityProvider google;
  @Mock CalendarAvailabilityProvider microsoft;

  @Test
  void requiresAtLeastOneMappedCalendar() {
    var readiness = new PublicBookingReadinessService(List.of()).check("room-1");

    assertFalse(readiness.available());
    assertEquals("Online booking requires a connected calendar.", readiness.reason());
  }

  @Test
  void acceptsResourceWhenAllMappedProvidersAreReady() {
    when(google.check("room-1"))
        .thenReturn(Optional.of(new CalendarAvailabilityProvider.Readiness(true, null)));
    when(microsoft.check("room-1")).thenReturn(Optional.empty());

    var readiness = new PublicBookingReadinessService(List.of(google, microsoft)).check("room-1");

    assertTrue(readiness.available());
  }

  @Test
  void rejectsResourceWhenOneMappedProviderIsNotReady() {
    when(google.check("room-1"))
        .thenReturn(Optional.of(new CalendarAvailabilityProvider.Readiness(true, null)));
    when(microsoft.check("room-1"))
        .thenReturn(
            Optional.of(
                new CalendarAvailabilityProvider.Readiness(
                    false, "Reconnect Microsoft Calendar.")));

    var readiness = new PublicBookingReadinessService(List.of(google, microsoft)).check("room-1");

    assertFalse(readiness.available());
    assertEquals("Reconnect Microsoft Calendar.", readiness.reason());
  }
}
