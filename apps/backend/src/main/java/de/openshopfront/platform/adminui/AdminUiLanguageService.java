package de.openshopfront.platform.adminui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Stores which interface languages the admin panel offers to every administrator. */
@Service
public class AdminUiLanguageService {
  private final NamedParameterJdbcTemplate db;

  public AdminUiLanguageService(NamedParameterJdbcTemplate db) {
    this.db = db;
  }

  /** Reads the admin interface language switches in display order. */
  @Transactional(readOnly = true)
  public List<Language> require() {
    List<Language> languages = new ArrayList<>();
    for (Map<String, Object> row :
        db.queryForList(
            "select code,name,enabled from admin_ui_language order by sort_order", Map.of())) {
      languages.add(
          new Language(
              String.valueOf(row.get("code")),
              String.valueOf(row.get("name")),
              Boolean.TRUE.equals(row.get("enabled"))));
    }
    if (languages.isEmpty())
      throw new IllegalStateException("Admin interface languages are missing.");
    return List.copyOf(languages);
  }

  /** Enables or disables interface languages while keeping at least one active. */
  @Transactional
  public List<Language> setLanguages(List<LanguageChange> changes) {
    if (changes == null || changes.isEmpty())
      throw new IllegalArgumentException("Select at least one interface language.");
    boolean anyEnabled = false;
    for (LanguageChange change : changes) {
      String code = change.code() == null ? "" : change.code().strip();
      if (!code.matches("[a-z]{2,8}"))
        throw new IllegalArgumentException("This language code is not supported: " + code);
      db.update(
          "update admin_ui_language set enabled=:enabled where code=:code",
          Map.of("enabled", change.enabled(), "code", code));
      if (change.enabled()) anyEnabled = true;
    }
    if (!anyEnabled) throw new IllegalArgumentException("Keep at least one language enabled.");
    return require();
  }

  /** One selectable interface language with its native display name. */
  public record Language(String code, String name, boolean enabled) {}

  /** An administrator change for one interface language switch. */
  public record LanguageChange(String code, boolean enabled) {}
}
