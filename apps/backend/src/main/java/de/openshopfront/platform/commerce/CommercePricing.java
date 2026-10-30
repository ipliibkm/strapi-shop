package de.openshopfront.platform.commerce;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Calculates order totals with half-up cent rounding. The server owns these values. */
public final class CommercePricing {
  private CommercePricing() {}

  /** Applies the configured tax rate and rounds to whole cents. */
  public static long taxCents(long subtotalCents, int taxRateBps) {
    if (subtotalCents <= 0 || taxRateBps <= 0) return 0L;
    BigDecimal amount =
        BigDecimal.valueOf(subtotalCents)
            .multiply(BigDecimal.valueOf(taxRateBps))
            .divide(BigDecimal.valueOf(10_000), 0, RoundingMode.HALF_UP);
    long result = amount.longValueExact();
    return result < 0 ? 0L : result;
  }

  /** Applies free shipping above the configured threshold. */
  public static long shippingCents(long subtotalCents, CommerceSettingsService.Settings settings) {
    if (subtotalCents <= 0 || !settings.shopEnabled()) return 0L;
    Long threshold = settings.freeShippingThresholdCents();
    if (threshold != null && subtotalCents >= threshold) return 0L;
    return Math.max(0L, settings.shippingFlatCents());
  }

  /** Returns the amount the customer pays. */
  public static long totalCents(long subtotalCents, long taxCents, long shippingCents) {
    long total = subtotalCents + taxCents + shippingCents;
    return total < 0 ? 0L : total;
  }
}
