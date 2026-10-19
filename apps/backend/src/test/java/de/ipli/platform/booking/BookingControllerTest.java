package de.ipli.platform.booking;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.context.annotation.Import;
import de.ipli.platform.SecurityConfiguration;

@WebMvcTest(BookingController.class)
@Import(SecurityConfiguration.class)
class BookingControllerTest {
 @Autowired MockMvc mvc;
 @MockitoBean BookingService bookings;
 @Test void unauthenticatedWritesAreDenied() throws Exception {
  mvc.perform(post("/api/v1/admin/bookings").contentType("application/json").content("{}")).andExpect(status().isForbidden());
 }
 @Test void availabilityRequiresAuthenticationOutsideDevProfile() throws Exception {
  mvc.perform(get("/api/v1/availability").param("resourceId","room-1").param("startsAt","2026-12-01T10:00:00Z").param("endsAt","2026-12-01T11:00:00Z")).andExpect(status().isForbidden());
 }
}
