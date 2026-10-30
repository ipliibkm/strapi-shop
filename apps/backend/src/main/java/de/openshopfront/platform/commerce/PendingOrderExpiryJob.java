package de.openshopfront.platform.commerce;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Releases stock from unpaid orders after their checkout window ends. */
@Component
public class PendingOrderExpiryJob {
  private final OrderService orders;

  public PendingOrderExpiryJob(OrderService orders) {
    this.orders = orders;
  }

  /** Expires due orders in bounded batches. */
  @Scheduled(fixedDelayString = "${platform.commerce.expiry-interval-ms:60000}")
  public void expire() {
    orders.expirePendingOrders();
  }
}
