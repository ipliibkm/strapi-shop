package de.openshopfront.platform.calendar;

import de.openshopfront.platform.auth.GoogleAccessTokenService;
import de.openshopfront.platform.booking.CalendarAvailabilityProvider;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/** Reports whether synchronized Google data can protect one booking resource. */
@Component
@ConditionalOnProperty(name = "platform.google.enabled", havingValue = "true")
public class GoogleCalendarAvailabilityProvider implements CalendarAvailabilityProvider {
  private final NamedParameterJdbcTemplate db;
  private final GoogleAccessTokenService tokens;
  private final Duration maximumSyncAge;

  public GoogleCalendarAvailabilityProvider(
      NamedParameterJdbcTemplate db,
      GoogleAccessTokenService tokens,
      @Value("${platform.booking.maximum-calendar-sync-age-seconds:300}")
          long maximumSyncAgeSeconds) {
    this.db = db;
    this.tokens = tokens;
    this.maximumSyncAge = Duration.ofSeconds(maximumSyncAgeSeconds);
  }

  @Override
  public Optional<Readiness> check(String resourceId) {
    var mappings =
        db.query(
            """
            select principal_name,sync_status,last_synced_at from google_calendar_mapping
             where resource_id=:resource and write_bookings=true
            """,
            Map.of("resource", resourceId),
            (rs, row) ->
                new MappingState(
                    rs.getString("principal_name"),
                    rs.getString("sync_status"),
                    instant(rs.getObject("last_synced_at", OffsetDateTime.class))));
    if (mappings.isEmpty()) return Optional.empty();
    MappingState mapping = mappings.getFirst();
    if ("CONFLICT".equals(mapping.syncStatus()))
      return unavailable("Online booking is paused because calendar conflicts require attention.");
    if (!"SYNCED".equals(mapping.syncStatus()) || mapping.lastSyncedAt() == null)
      return unavailable("Online booking is paused until calendar synchronization completes.");
    if (mapping.lastSyncedAt().isBefore(Instant.now().minus(maximumSyncAge)))
      return unavailable("Online booking is paused because calendar data is not current.");
    try {
      tokens.authorize(mapping.principalName());
      return Optional.of(new Readiness(true, null));
    } catch (Exception error) {
      return unavailable("Online booking requires the administrator to reconnect Google Calendar.");
    }
  }

  private static Optional<Readiness> unavailable(String reason) {
    return Optional.of(new Readiness(false, reason));
  }

  private static Instant instant(OffsetDateTime value) {
    return value == null ? null : value.toInstant();
  }

  private record MappingState(String principalName, String syncStatus, Instant lastSyncedAt) {}
}
