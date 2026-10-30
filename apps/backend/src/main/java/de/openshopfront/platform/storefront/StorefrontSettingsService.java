package de.openshopfront.platform.storefront;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Stores the storefront language switches and the published color palette. */
@Service
public class StorefrontSettingsService {
  private static final Set<String> THEME_KEYS = Set.of("light", "dark", "ocean", "rosewater");

  private final NamedParameterJdbcTemplate db;

  public StorefrontSettingsService(NamedParameterJdbcTemplate db) {
    this.db = db;
  }

  /** Reads the language switches and the published palette for one storefront. */
  @Transactional(readOnly = true)
  public Settings require() {
    List<Language> languages = new ArrayList<>();
    for (Map<String, Object> row :
        db.queryForList(
            "select code,name,enabled from storefront_language order by sort_order", Map.of())) {
      languages.add(
          new Language(
              String.valueOf(row.get("code")),
              String.valueOf(row.get("name")),
              Boolean.TRUE.equals(row.get("enabled"))));
    }
    if (languages.isEmpty()) throw new IllegalStateException("Storefront languages are missing.");
    String theme =
        db
            .query(
                "select theme_key from storefront_appearance where id=1",
                (rs, row) -> rs.getString("theme_key"))
            .stream()
            .findFirst()
            .orElse("light");
    String customPalette =
        db
            .query(
                "select custom_palette::text as palette from storefront_appearance where id=1",
                (rs, row) -> rs.getString("palette"))
            .stream()
            .filter(java.util.Objects::nonNull)
            .findFirst()
            .orElse(null);
    return new Settings(
        List.copyOf(languages), THEME_KEYS.contains(theme) ? theme : "light", customPalette);
  }

  /** Enables or disables languages while keeping at least one language active. */
  @Transactional
  public Settings setLanguages(List<LanguageChange> changes) {
    if (changes == null || changes.isEmpty())
      throw new IllegalArgumentException("Select at least one website language.");
    boolean anyEnabled = false;
    for (LanguageChange change : changes) {
      String code = change.code() == null ? "" : change.code().strip();
      if (!code.matches("[a-z]{2,8}"))
        throw new IllegalArgumentException("This language code is not supported: " + code);
      db.update(
          "update storefront_language set enabled=:enabled where code=:code",
          Map.of("enabled", change.enabled(), "code", code));
      if (change.enabled()) anyEnabled = true;
    }
    if (!anyEnabled) throw new IllegalArgumentException("Keep at least one language enabled.");
    return require();
  }

  /** Publishes the selected color palette to every storefront visitor. */
  @Transactional
  public Settings setTheme(String themeKey) {
    String key = themeKey == null ? "" : themeKey.strip();
    if (!THEME_KEYS.contains(key))
      throw new IllegalArgumentException("This color palette is not available.");
    db.update(
        "update storefront_appearance set theme_key=:theme,custom_palette=null,updated_at=now() where id=1",
        Map.of("theme", key));
    return require();
  }

  /** Publishes a custom color palette; pass null to fall back to the preset. */
  @Transactional
  public Settings setCustomPalette(String paletteJson) {
    if (paletteJson == null || paletteJson.isBlank()) {
      db.update(
          "update storefront_appearance set custom_palette=null,updated_at=now() where id=1",
          Map.of());
      return require();
    }
    String json = paletteJson.strip();
    // Basic shape check so a malformed payload cannot break the storefront.
    if (!json.startsWith("{") || !json.endsWith("}"))
      throw new IllegalArgumentException("This color palette is not valid.");
    // Cast the bind parameter to jsonb; Postgres forbids casting the column in a SET clause.
    db.update(
        "update storefront_appearance set custom_palette=:palette::jsonb,updated_at=now() where id=1",
        Map.of("palette", json));
    return require();
  }

  /** One selectable website language with its native display name. */
  public record Language(String code, String name, boolean enabled) {}

  /** An administrator change for one language switch. */
  public record LanguageChange(String code, boolean enabled) {}

  /** The complete storefront presentation settings. */
  public record Settings(List<Language> languages, String theme, String customPalette) {}
}
