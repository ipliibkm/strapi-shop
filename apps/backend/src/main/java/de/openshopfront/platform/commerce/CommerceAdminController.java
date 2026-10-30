package de.openshopfront.platform.commerce;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** Lets a Super Admin manage orders, settings, fulfillment, and refunds. */
@RestController
@RequestMapping("/api/v1/admin/commerce")
public class CommerceAdminController {
  private final OrderService orders;
  private final CommerceSettingsService settings;
  private final StripePaymentProvider stripe;

  public CommerceAdminController(
      OrderService orders, CommerceSettingsService settings, StripePaymentProvider stripe) {
    this.orders = orders;
    this.settings = settings;
    this.stripe = stripe;
  }

  /** Reports whether a live payment provider is configured. */
  @GetMapping("/status")
  public Map<String, Object> status() {
    return Map.of("stripeConfigured", stripe.active());
  }

  /** Lists recent orders newest first. */
  @GetMapping("/orders")
  public List<OrderService.Order> list(
      @RequestParam(required = false) String statusFilter,
      @RequestParam(defaultValue = "50") int limit) {
    return orders.list(statusFilter, limit);
  }

  /** Loads one order with items, payment, and shipment. */
  @GetMapping("/orders/{id}")
  public OrderService.OrderDetail detail(@PathVariable java.util.UUID id) {
    return orders.detail(id);
  }

  /** Marks an unpaid order paid when no live provider is configured. */
  @PostMapping("/orders/{id}/mark-paid")
  public void markPaid(@PathVariable java.util.UUID id) {
    orders.markPaid(id, null);
  }

  /** Cancels an unpaid order and releases its stock hold. */
  @DeleteMapping("/orders/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void cancel(@PathVariable java.util.UUID id) {
    orders.cancel(id);
  }

  /** Ships a paid order with the selected carrier and tracking number. */
  @PostMapping("/orders/{id}/fulfill")
  public void fulfill(@PathVariable java.util.UUID id, @RequestBody FulfillRequest request) {
    orders.fulfill(id, request.carrier(), request.trackingNumber());
  }

  /** Refunds a paid or shipped order once. */
  @PostMapping("/orders/{id}/refund")
  public void refund(@PathVariable java.util.UUID id) {
    if (stripe.active()) {
      OrderService.OrderDetail detail = orders.detail(id);
      OrderService.Payment payment = detail.payment();
      if (payment == null || payment.externalId() == null || payment.externalId().isBlank())
        throw new IllegalArgumentException("The order has no Stripe payment identifier.");
      stripe.refund(payment.externalId(), payment.amountCents(), "refund-order-" + id);
      return;
    }
    orders.refund(id);
  }

  /** Reads the shop switch, tax rate, and shipping cost. */
  @GetMapping("/settings")
  public CommerceSettingsService.Settings settings() {
    return settings.require();
  }

  /** Updates the shop switch, tax rate, and shipping cost. */
  @PutMapping("/settings")
  public CommerceSettingsService.Settings update(@RequestBody SettingsRequest request) {
    Long threshold =
        request.freeShippingThresholdCents() == null ? null : request.freeShippingThresholdCents();
    return settings.update(
        request.shopEnabled(), request.taxRateBps(), request.shippingFlatCents(), threshold);
  }

  /** Carries shipment details for a paid order. */
  public record FulfillRequest(String carrier, String trackingNumber) {}

  /** Carries commerce configuration input from the administrator. */
  public record SettingsRequest(
      boolean shopEnabled,
      int taxRateBps,
      long shippingFlatCents,
      Long freeShippingThresholdCents) {}
}
