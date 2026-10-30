package de.openshopfront.platform.auth;

import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.stereotype.Service;

/** Resolves the platform-wide Google service owner independently of admin identities. */
@Service
@ConditionalOnProperty(name = "platform.google.enabled", havingValue = "true")
public class GoogleServiceConnection {
  public static final String DEFAULT_OWNER = "platform-google-services";
  private final JdbcOperations db;

  public GoogleServiceConnection(JdbcOperations db) {
    this.db = db;
  }

  /** Returns the stable owner key for current and previously stored Google credentials. */
  public String owner() {
    List<String> configured =
        db.query(
            "select principal_name from google_service_connection where id=1",
            (rs, row) -> rs.getString(1));
    if (!configured.isEmpty()) return configured.getFirst();
    List<String> mapped =
        db.query(
            "select principal_name from google_calendar_mapping order by updated_at desc limit 1",
            (rs, row) -> rs.getString(1));
    if (!mapped.isEmpty()) return mapped.getFirst();
    List<String> stored =
        db.query(
            "select principal_name from oauth2_authorized_client where client_registration_id='google' order by updated_at desc limit 1",
            (rs, row) -> rs.getString(1));
    return stored.isEmpty() ? DEFAULT_OWNER : stored.getFirst();
  }

  /** Stores safe metadata for the connected Google services account. */
  public void connected(String principalName, String email) {
    db.update(
        """
  insert into google_service_connection(id,principal_name,email,updated_at) values(1,?,?,now())
  on conflict(id) do update set principal_name=excluded.principal_name,email=excluded.email,updated_at=now()
  """,
        principalName,
        email);
  }

  public String email() {
    List<String> values =
        db.query(
            "select email from google_service_connection where id=1", (rs, row) -> rs.getString(1));
    return values.isEmpty() ? "" : values.getFirst();
  }

  public void clear() {
    db.update("delete from google_service_connection where id=1");
  }
}
