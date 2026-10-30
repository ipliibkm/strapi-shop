package de.openshopfront.platform.commerce;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/** Receives signed Stripe events and applies each one exactly once. */
@RestController
@RequestMapping("/webhooks")
public class CommerceWebhookController {
  private final OrderService orders;
  private final StripePaymentProvider stripe;
  private final com.fasterxml.jackson.databind.ObjectMapper json;
  private final NamedParameterJdbcTemplate db;

  public CommerceWebhookController(
      OrderService orders,
      StripePaymentProvider stripe,
      com.fasterxml.jackson.databind.ObjectMapper json,
      NamedParameterJdbcTemplate db) {
    this.orders = orders;
    this.stripe = stripe;
    this.json = json;
    this.db = db;
  }

  /** Applies a verified checkout or refund event. */
  @PostMapping("/stripe")
  @Transactional
  public Map<String, String> receive(
      @RequestHeader(value = "Stripe-Signature", required = false) String signature,
      @RequestBody String payload) {
    if (!StripeSignatureVerifier.verify(signature, payload, stripe.signatureSecret())) {
      throw new InvalidSignatureException("The webhook signature is not valid.");
    }
    JsonNode event;
    try {
      event = json.readTree(payload);
    } catch (com.fasterxml.jackson.core.JsonProcessingException error) {
      throw new IllegalArgumentException("The webhook body is not valid JSON.");
    }
    String eventId = text(event, "id");
    if (eventId.isBlank())
      throw new IllegalArgumentException("The webhook has no event identifier.");

    boolean firstSeen =
        db.update(
                "insert into stripe_event (event_id,type) values (:id,:type) on conflict (event_id) do nothing",
                Map.of("id", eventId, "type", text(event, "type")))
            == 1;
    if (firstSeen) apply(text(event, "type"), event);
    return Map.of("status", "accepted");
  }

  private void apply(String type, JsonNode event) {
    JsonNode object = event.path("data").path("object");
    if ("charge.refunded".equals(type)) {
      java.util.UUID orderId = orders.orderIdForPayment(text(object, "payment_intent"));
      if (orderId != null) orders.refund(orderId);
      return;
    }
    String sessionId = text(object, "id");
    java.util.UUID orderId = orders.orderIdForSession(sessionId);
    if (orderId == null) return;
    if (("checkout.session.completed".equals(type)
            && "paid".equalsIgnoreCase(text(object, "payment_status")))
        || "checkout.session.async_payment_succeeded".equals(type)) {
      String paymentIntent = text(object, "payment_intent");
      orders.markPaid(orderId, paymentIntent.isBlank() ? null : paymentIntent);
    } else if ("checkout.session.async_payment_failed".equals(type)
        || "checkout.session.expired".equals(type)) {
      try {
        orders.cancel(orderId);
      } catch (IllegalArgumentException alreadyClosed) {
        // The order was cancelled earlier; the event is still safe to ignore.
      }
    }
  }

  private static String text(JsonNode node, String field) {
    if (node == null || !node.isObject()) return "";
    JsonNode value = node.get(field);
    return value == null || !value.isTextual() ? "" : value.asText();
  }

  /** Raised before a request reaches business logic when the signature fails. */
  @ResponseStatus(HttpStatus.UNAUTHORIZED)
  public static class InvalidSignatureException extends RuntimeException {
    private InvalidSignatureException(String message) {
      super(message);
    }
  }
}
