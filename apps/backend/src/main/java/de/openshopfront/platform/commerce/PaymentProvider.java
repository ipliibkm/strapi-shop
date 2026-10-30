package de.openshopfront.platform.commerce;

import java.time.Instant;

/** Publishes an order for payment and reports provider identifiers without tokens. */
public interface PaymentProvider {
  /** Reports whether a live provider is configured. */
  boolean active();

  /** Creates a hosted checkout session and returns its identifier and start URL. */
  Session createCheckoutSession(
      OrderService.Order order, int itemCount, Instant expiresAt, String accessToken);

  /** Returns the provider refund identifier for an already paid order. */
  String refund(String paymentIntentId, long amountCents, String idempotencyKey);

  /** Contains a created checkout session. */
  record Session(String sessionId, String url) {}
}
