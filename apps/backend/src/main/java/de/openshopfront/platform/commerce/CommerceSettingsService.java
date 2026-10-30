package de.openshopfront.platform.commerce;

import java.util.HashMap;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Stores the shop switch, tax rate, and flat shipping cost. */
@Service
public class CommerceSettingsService {
  private final NamedParameterJdbcTemplate db;

  public CommerceSettingsService(NamedParameterJdbcTemplate db) {
    this.db = db;
  }

  /** Reads the current settings row. */
  @Transactional(readOnly = true)
  public Settings require() {
    return query(" where id=1").stream()
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("Commerce settings are missing."));
  }

  /** Updates the shop switch and shipping or tax configuration. */
  @Transactional
  public Settings update(
      boolean shopEnabled,
      int taxRateBps,
      long shippingFlatCents,
      Long freeShippingThresholdCents) {
    if (taxRateBps < 0 || taxRateBps > 35_000)
      throw new IllegalArgumentException("Enter a tax rate between 0 and 350 percent.");
    if (shippingFlatCents < 0 || shippingFlatCents > 10_000_000L)
      throw new IllegalArgumentException("Enter a flat shipping cost in this range.");
    if (freeShippingThresholdCents != null
        && (freeShippingThresholdCents < 0 || freeShippingThresholdCents > 10_000_000L))
      throw new IllegalArgumentException("Enter a free-shipping threshold in this range.");
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("enabled", shopEnabled);
    parameters.put("tax", taxRateBps);
    parameters.put("flat", shippingFlatCents);
    parameters.put("threshold", freeShippingThresholdCents);
    db.update(
        "update commerce_settings set shop_enabled=:enabled,tax_rate_bps=:tax,shipping_flat_cents=:flat,free_shipping_threshold_cents=:threshold,updated_at=now() where id=1",
        parameters);
    return require();
  }

  private java.util.List<Settings> query(String where) {
    return db.query(
        "select shop_enabled,tax_rate_bps,shipping_flat_cents,free_shipping_threshold_cents from commerce_settings"
            + where,
        Map.of(),
        (rs, row) ->
            new Settings(
                rs.getBoolean("shop_enabled"),
                rs.getInt("tax_rate_bps"),
                rs.getLong("shipping_flat_cents"),
                getNullableLong(rs, "free_shipping_threshold_cents")));
  }

  private static Long getNullableLong(java.sql.ResultSet rs, String column)
      throws java.sql.SQLException {
    long value = rs.getLong(column);
    return rs.wasNull() ? null : value;
  }

  /** Contains the validated commerce configuration. */
  public record Settings(
      boolean shopEnabled,
      int taxRateBps,
      long shippingFlatCents,
      Long freeShippingThresholdCents) {}
}
