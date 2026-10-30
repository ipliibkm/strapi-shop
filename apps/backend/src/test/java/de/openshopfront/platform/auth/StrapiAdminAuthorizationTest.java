package de.openshopfront.platform.auth;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.openshopfront.platform.booking.BookingController;
import de.openshopfront.platform.booking.BookingService;
import de.openshopfront.platform.content.StrapiContentController;
import de.openshopfront.platform.setup.SetupVerificationController;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    value = {
      BookingController.class,
      StrapiContentController.class,
      SetupVerificationController.class,
      AdminSessionController.class,
      StrapiAdminAuthenticationController.class
    },
    properties = "server.servlet.session.cookie.secure=true")
@Import(StrapiAdminSecurityConfiguration.class)
class StrapiAdminAuthorizationTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired CookieCsrfTokenRepository csrfRepository;
  @MockitoBean BookingService bookings;
  @MockitoBean StrapiAdminClient strapi;
  @MockitoBean AdminLoginAttemptService loginAttempts;
  @MockitoBean NamedParameterJdbcTemplate db;

  @Test
  void csrfEndpointReturnsTheTokenUsedByApiClients() throws Exception {
    mvc.perform(get("/api/v1/auth/csrf").with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.headerName").value("X-CSRF-TOKEN"))
        .andExpect(jsonPath("$.token").isNotEmpty());
  }

  @Test
  void csrfCookieUsesProductionSecurityAttributes() throws Exception {
    var request = new MockHttpServletRequest();
    var response = new MockHttpServletResponse();

    csrfRepository.saveToken(csrfRepository.generateToken(request), request, response);

    var cookie = response.getCookie("XSRF-TOKEN");
    org.junit.jupiter.api.Assertions.assertNotNull(cookie);
    org.junit.jupiter.api.Assertions.assertEquals("/", cookie.getPath());
    org.junit.jupiter.api.Assertions.assertTrue(cookie.getSecure());
    org.junit.jupiter.api.Assertions.assertFalse(cookie.isHttpOnly());
    org.junit.jupiter.api.Assertions.assertEquals("Lax", cookie.getAttribute("SameSite"));
  }

  @Test
  void regularStrapiAdminCanReadPermittedContent() throws Exception {
    when(strapi.content(any(), eq("GET"), anyString(), isNull()))
        .thenReturn(json.createObjectNode());
    mvc.perform(get("/api/v1/content/business-profile").with(user("editor").roles("STRAPI_ADMIN")))
        .andExpect(status().isOk());
  }

  @Test
  void loginUsesAuthoritativeRoleStateImmediately() throws Exception {
    var incomplete = new StrapiAdminPrincipal("1", "Administrator", "admin@example.com", false);
    var authoritative = new StrapiAdminPrincipal("1", "Administrator", "admin@example.com", true);
    var credentials =
        new StrapiAdminClient.Credentials("1", "device", new byte[] {1}, new byte[] {2});
    when(strapi.login("admin@example.com", "password"))
        .thenReturn(new StrapiAdminClient.LoginResult(incomplete, credentials));
    when(strapi.currentPrincipal(any())).thenReturn(authoritative);

    mvc.perform(
            post("/api/v1/auth/login")
                .with(csrf())
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("{\"email\":\"admin@example.com\",\"password\":\"password\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.superAdmin").value(true));
  }

  @Test
  void loginReportsStrapiRateLimit() throws Exception {
    when(strapi.login("admin@example.com", "password"))
        .thenThrow(new StrapiAdminClient.TooManyLoginAttemptsException("60"));

    mvc.perform(
            post("/api/v1/auth/login")
                .with(csrf())
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("{\"email\":\"admin@example.com\",\"password\":\"password\"}"))
        .andExpect(status().isTooManyRequests())
        .andExpect(header().string("Retry-After", "60"))
        .andExpect(
            jsonPath("$.message").value("Too many sign-in attempts. Wait before you try again."));
  }

  @Test
  void regularStrapiAdminCannotReadPlatformAdministration() throws Exception {
    mvc.perform(get("/api/v1/admin/bookings/calendar").with(user("editor").roles("STRAPI_ADMIN")))
        .andExpect(status().isForbidden());
  }

  @Test
  void superAdminCanReadPlatformAdministration() throws Exception {
    when(bookings.listCalendar(any(Instant.class), any(Instant.class))).thenReturn(List.of());
    mvc.perform(
            get("/api/v1/admin/bookings/calendar")
                .param("from", "2026-10-20T00:00:00Z")
                .param("to", "2026-10-27T00:00:00Z")
                .with(user("super").roles("STRAPI_ADMIN", "SUPER_ADMIN")))
        .andExpect(status().isOk());
  }

  @Test
  void regularStrapiAdminCannotReadRuntimeSystemChecks() throws Exception {
    mvc.perform(get("/api/v1/admin/system/checks").with(user("editor").roles("STRAPI_ADMIN")))
        .andExpect(status().isForbidden());
  }

  @Test
  void superAdminCanReadRuntimeSystemChecks() throws Exception {
    when(db.queryForObject(anyString(), anyMap(), eq(Boolean.class))).thenReturn(true);
    mvc.perform(
            get("/api/v1/admin/system/checks")
                .with(user("super").roles("STRAPI_ADMIN", "SUPER_ADMIN")))
        .andExpect(status().isOk());
  }

  @Test
  void strapiRoleChangeRemovesPlatformAdministrationImmediately() throws Exception {
    var existing = new StrapiAdminPrincipal("1", "Administrator", "admin@example.com", true);
    var demoted = new StrapiAdminPrincipal("1", "Administrator", "admin@example.com", false);
    var auth =
        UsernamePasswordAuthenticationToken.authenticated(
            existing,
            null,
            List.of(
                new SimpleGrantedAuthority("ROLE_STRAPI_ADMIN"),
                new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")));
    when(strapi.currentPrincipal(nullable(jakarta.servlet.http.HttpSession.class)))
        .thenReturn(demoted);
    mvc.perform(get("/api/v1/admin/bookings/calendar").with(authentication(auth)))
        .andExpect(status().isForbidden());
  }
}
