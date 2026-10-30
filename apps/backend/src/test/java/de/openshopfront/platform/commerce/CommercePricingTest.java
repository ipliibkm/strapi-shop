package de.openshopfront.platform.commerce;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Verifies server-side total calculation with half-up rounding. */
class CommercePricingTest {
  private final CommerceSettingsService.Settings settings =
      new CommerceSettingsService.Settings(true, 1900, 499L, 6000L);

  @Test
  void roundsTaxHalfUp() {
    assertEquals(1L, CommercePricing.taxCents(3L, 1900));
    assertEquals(570L, CommercePricing.taxCents(3000L, 1900));
    assertEquals(0L, CommercePricing.taxCents(1L, 299));
  }

  @Test
  void appliesFreeShippingAboveThreshold() {
    assertEquals(0L, CommercePricing.shippingCents(6000L, settings));
    assertEquals(499L, CommercePricing.shippingCents(5999L, settings));
  }

  @Test
  void sumsTotalWithoutNegativeValues() {
    assertEquals(4069L, CommercePricing.totalCents(3000L, 570L, 499L));
  }

  @Test
  void rejectsZeroSubtotalForShipping() {
    assertEquals(0L, CommercePricing.shippingCents(0L, settings));
  }
}
