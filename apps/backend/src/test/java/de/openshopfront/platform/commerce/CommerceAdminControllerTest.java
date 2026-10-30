package de.openshopfront.platform.commerce;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Verifies that live refunds wait for Stripe webhook confirmation. */
class CommerceAdminControllerTest {
  @Test
  void liveRefundUsesThePaymentIntentAndDoesNotFinalizeLocalState() {
    OrderService orders = mock(OrderService.class);
    CommerceSettingsService settings = mock(CommerceSettingsService.class);
    StripePaymentProvider stripe = mock(StripePaymentProvider.class);
    UUID orderId = UUID.randomUUID();
    OrderService.Payment payment = new OrderService.Payment("stripe", "pi_123", 2599, "SUCCEEDED");
    when(stripe.active()).thenReturn(true);
    when(orders.detail(orderId))
        .thenReturn(new OrderService.OrderDetail(null, List.of(), payment, null));
    CommerceAdminController subject = new CommerceAdminController(orders, settings, stripe);

    subject.refund(orderId);

    verify(stripe).refund("pi_123", 2599, "refund-order-" + orderId);
    verify(orders, never()).refund(orderId);
  }
}
