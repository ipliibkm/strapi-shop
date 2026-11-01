package de.openshopfront.platform.commerce;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Serves the public catalog and accepts customer checkout requests. */
@RestController
@RequestMapping("/api/v1/public")
public class PublicCommerceController {
  private final CatalogService catalog;
  private final OrderService orders;
  private final CommerceSettingsService settings;
  private final StripePaymentProvider stripe;
  private final OrderLookupRateLimiter orderLookupRateLimiter;
  private final CustomerAccountService accounts;

  public PublicCommerceController(
      CatalogService catalog,
      OrderService orders,
      CommerceSettingsService settings,
      StripePaymentProvider stripe,
      OrderLookupRateLimiter orderLookupRateLimiter,
      CustomerAccountService accounts) {
    this.catalog = catalog;
    this.orders = orders;
    this.settings = settings;
    this.stripe = stripe;
    this.orderLookupRateLimiter = orderLookupRateLimiter;
    this.accounts = accounts;
  }

  /** Lists active products for the storefront. */
  @GetMapping("/products")
  public List<CatalogService.Product> products() {
    if (!settings.require().shopEnabled()) return List.of();
    return catalog.list(true);
  }

  /** Loads one product with its purchasable options and availability. */
  @GetMapping("/products/{slugOrId}")
  public CatalogService.ProductDetail product(@PathVariable String slugOrId) {
    if (!settings.require().shopEnabled())
      throw new IllegalArgumentException("The shop is not open yet.");
    return catalog.find(slugOrId);
  }

  /** Creates the order and returns a checkout link or the pending order number. */
  @PostMapping("/checkout")
  @ResponseStatus(HttpStatus.CREATED)
  public CheckoutResponse checkout(
      @RequestBody OrderService.CheckoutRequest request,
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    CustomerAccountService.Customer customer =
        authorization == null ? null : accounts.requireCustomer(authorization);
    Long customerId = customer == null ? null : customer.id();
    if (customer != null && !customer.email().equalsIgnoreCase(request.customerEmail()))
      throw new IllegalArgumentException("Use your account email at checkout.");
    OrderService.CheckoutResult result = orders.checkout(request, customerId);
    if (!stripe.active())
      return new CheckoutResponse(
          result.orderNumber(), result.accessToken(), "PENDING_PAYMENT", null, false);
    try {
      OrderService.Order order = orders.findByNumber(result.orderNumber());
      PaymentProvider.Session session =
          stripe.createCheckoutSession(
              order, result.itemCount(), result.expiresAt(), result.accessToken());
      orders.attachSession(order.id(), session.sessionId());
      return new CheckoutResponse(
          result.orderNumber(), result.accessToken(), "PENDING_PAYMENT", session.url(), true);
    } catch (RuntimeException error) {
      orders.cancel(result.orderId());
      throw error;
    }
  }

  /** Returns a safe status summary for the customer confirmation page. */
  @GetMapping("/orders/{orderNumber}")
  public OrderSummary order(
      @PathVariable String orderNumber,
      @RequestHeader("X-Order-Access-Token") String accessToken,
      jakarta.servlet.http.HttpServletRequest request) {
    orderLookupRateLimiter.check(orderNumber, request.getRemoteAddr());
    OrderService.Order order = orders.findPublicOrder(orderNumber, accessToken);
    return new OrderSummary(
        order.orderNumber(),
        order.status(),
        order.totalCents(),
        order.currency(),
        order.createdAt());
  }

  /** Maps invalid checkout input or an unknown product to a readable client error. */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, String>> invalid(IllegalArgumentException error) {
    return ResponseEntity.badRequest()
        .body(Map.of("code", "VALIDATION_ERROR", "message", error.getMessage()));
  }

  /** Maps an out-of-stock checkout to a readable conflict the storefront can display. */
  @ExceptionHandler(OrderService.OutOfStockException.class)
  public ResponseEntity<Map<String, String>> outOfStock(OrderService.OutOfStockException error) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(Map.of("code", "OUT_OF_STOCK", "message", error.getMessage()));
  }

  /** Maps a shop disabled by the administrator to a readable conflict response. */
  @ExceptionHandler(OrderService.ShopDisabledException.class)
  public ResponseEntity<Map<String, String>> shopDisabled() {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(Map.of("code", "SHOP_DISABLED", "message", "The shop is not open yet."));
  }

  /** Maps excessive public lookup attempts to the standard HTTP rate-limit status. */
  @ExceptionHandler(OrderLookupRateLimiter.RateLimitException.class)
  public ResponseEntity<Map<String, String>> rateLimited() {
    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
        .body(
            Map.of(
                "code",
                "RATE_LIMITED",
                "message",
                "Too many order lookup requests. Wait one minute and try again."));
  }

  /** Carries the checkout result for the storefront. */
  public record CheckoutResponse(
      String orderNumber,
      String accessToken,
      String status,
      String checkoutUrl,
      boolean paymentRequired) {}

  /** Returns a safe summary of one customer order. */
  public record OrderSummary(
      String orderNumber,
      String status,
      long totalCents,
      String currency,
      java.time.Instant createdAt) {}
}
