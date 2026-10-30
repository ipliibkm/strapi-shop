package de.openshopfront.platform.booking;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.openshopfront.platform.PublicBookingSecurityConfiguration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Verifies the anonymous customer booking contract and its public response boundary. */
@WebMvcTest(PublicBookingController.class)
@Import(PublicBookingSecurityConfiguration.class)
class PublicBookingControllerTest {
  @Autowired MockMvc mvc;
  @MockitoBean BookingService bookings;
  @MockitoBean BookingOfferingService offerings;
  @MockitoBean PublicBookingReadinessService readiness;

  @Test
  void anonymousCustomerCanListActiveServices() throws Exception {
    UUID serviceId = UUID.randomUUID();
    when(offerings.listActive())
        .thenReturn(
            List.of(new BookingOfferingService.Offering(serviceId, "Lashlifting", 45, true)));
    mvc.perform(get("/api/v1/public/services"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(serviceId.toString()))
        .andExpect(jsonPath("$[0].name").value("Lashlifting"))
        .andExpect(jsonPath("$[0].durationMinutes").value(45))
        .andExpect(jsonPath("$[0].active").doesNotExist());
  }

  @Test
  void anonymousCustomerCanReadBookingSettings() throws Exception {
    when(offerings.settings()).thenReturn(new BookingOfferingService.CustomerSettings(true, 75));
    when(readiness.check("default"))
        .thenReturn(new PublicBookingReadinessService.Readiness(true, null));
    mvc.perform(get("/api/v1/public/booking-settings"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.customerBookingEnabled").value(true))
        .andExpect(jsonPath("$.defaultDurationMinutes").value(75));
  }

  @Test
  void bookingSettingsAreInactiveWhenGoogleLoginIsRequired() throws Exception {
    when(offerings.settings()).thenReturn(new BookingOfferingService.CustomerSettings(true, 60));
    when(readiness.check("default"))
        .thenReturn(
            new PublicBookingReadinessService.Readiness(
                false, "Online booking requires the administrator to reconnect Google Calendar."));
    mvc.perform(get("/api/v1/public/booking-settings"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.customerBookingEnabled").value(false))
        .andExpect(
            jsonPath("$.unavailableReason")
                .value("Online booking requires the administrator to reconnect Google Calendar."));
  }

  @Test
  void anonymousCustomerCanCheckAvailability() throws Exception {
    Instant start = Instant.parse("2026-12-04T10:00:00Z"),
        end = Instant.parse("2026-12-04T11:00:00Z");
    UUID serviceId = UUID.randomUUID();
    when(offerings.publicOffering(serviceId))
        .thenReturn(new BookingOfferingService.Offering(serviceId, "Lashlifting", 60, true));
    when(bookings.isBusy("default", start, end)).thenReturn(false);
    mvc.perform(
            get("/api/v1/public/availability")
                .param("serviceId", serviceId.toString())
                .param("startsAt", start.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.available").value(true));
  }

  @Test
  void anonymousCustomerCanCreateBookingWithoutCsrf() throws Exception {
    Instant start = Instant.parse("2026-12-04T10:00:00Z"),
        end = Instant.parse("2026-12-04T11:00:00Z");
    UUID id = UUID.randomUUID(), serviceId = UUID.randomUUID();
    var offering = new BookingOfferingService.Offering(serviceId, "Lashlifting", 60, true);
    when(offerings.publicOffering(serviceId)).thenReturn(offering);
    when(bookings.create(
            eq("default"), eq(start), eq(offering), eq("Alex Example"), eq("alex@example.com")))
        .thenReturn(
            new BookingService.Booking(
                id,
                "default",
                start,
                end,
                "CONFIRMED",
                serviceId,
                "Lashlifting",
                60,
                "Alex Example",
                "alex@example.com",
                "google",
                "PENDING",
                null));
    mvc.perform(
            post("/api/v1/public/bookings")
                .contentType("application/json")
                .content(
                    """
   {"serviceId":"%s","startsAt":"2026-12-04T10:00:00Z","customerName":"Alex Example","customerEmail":"alex@example.com"}
   """
                        .formatted(serviceId)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(id.toString()))
        .andExpect(jsonPath("$.serviceName").value("Lashlifting"))
        .andExpect(jsonPath("$.durationMinutes").value(60))
        .andExpect(jsonPath("$.endsAt").value("2026-12-04T11:00:00Z"))
        .andExpect(jsonPath("$.customerEmail").doesNotExist())
        .andExpect(jsonPath("$.calendarSyncStatus").doesNotExist());
  }

  @Test
  void anonymousCustomerCanBookDefaultAppointmentWithoutConfiguredServices() throws Exception {
    Instant start = Instant.parse("2026-12-04T10:00:00Z"),
        end = Instant.parse("2026-12-04T11:00:00Z");
    UUID id = UUID.randomUUID();
    var fallback = new BookingOfferingService.Offering(null, "Appointment", 60, true);
    when(offerings.publicOffering(null)).thenReturn(fallback);
    when(bookings.isBusy("default", start, end)).thenReturn(false);
    mvc.perform(get("/api/v1/public/availability").param("startsAt", start.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.available").value(true))
        .andExpect(jsonPath("$.endsAt").value(end.toString()));
    when(bookings.create(
            eq("default"), eq(start), eq(fallback), eq("Alex Example"), eq("alex@example.com")))
        .thenReturn(
            new BookingService.Booking(
                id,
                "default",
                start,
                end,
                "CONFIRMED",
                null,
                "Appointment",
                60,
                "Alex Example",
                "alex@example.com",
                "google",
                "PENDING",
                null));
    mvc.perform(
            post("/api/v1/public/bookings")
                .contentType("application/json")
                .content(
                    """
   {"startsAt":"2026-12-04T10:00:00Z","customerName":"Alex Example","customerEmail":"alex@example.com"}
   """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.serviceName").value("Appointment"))
        .andExpect(jsonPath("$.durationMinutes").value(60));
  }

  @Test
  void disabledCustomerBookingIsRejected() throws Exception {
    Instant start = Instant.parse("2026-12-04T10:00:00Z");
    when(offerings.publicOffering(null))
        .thenThrow(new BookingOfferingService.CustomerBookingDisabledException());
    mvc.perform(get("/api/v1/public/availability").param("startsAt", start.toString()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("BOOKING_DISABLED"));
  }

  @Test
  void unsynchronizedGoogleCalendarRejectsBooking() throws Exception {
    Instant start = Instant.parse("2026-12-04T10:00:00Z");
    var fallback = new BookingOfferingService.Offering(null, "Appointment", 60, true);
    when(offerings.publicOffering(null)).thenReturn(fallback);
    doThrow(
            new PublicBookingReadinessService.PublicBookingUnavailableException(
                "Online booking requires the administrator to reconnect Google Calendar."))
        .when(readiness)
        .requireAvailable("default");
    mvc.perform(
            post("/api/v1/public/bookings")
                .contentType("application/json")
                .content(
                    """
   {"startsAt":"2026-12-04T10:00:00Z","customerName":"Alex Example","customerEmail":"alex@example.com"}
   """))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.code").value("BOOKING_UNAVAILABLE"));
  }

  @Test
  void invalidCustomerEmailIsRejected() throws Exception {
    mvc.perform(
            post("/api/v1/public/bookings")
                .contentType("application/json")
                .content(
                    """
   {"serviceId":"00000000-0000-0000-0000-000000000001","startsAt":"2026-12-04T10:00:00Z","customerName":"Alex Example","customerEmail":"not-an-email"}
   """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }
}
