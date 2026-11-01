package de.openshopfront.platform.commerce;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/** Creates Stripe checkout sessions and refunds through the REST API. */
@Service
public class StripePaymentProvider implements PaymentProvider {
  private final String secretKey;
  private final String webhookSecret;
  private final String baseUrl;
  private final String storefrontBaseUrl;
  private final ObjectMapper json;
  private final RestClient http = RestClient.create();

  public StripePaymentProvider(
      @Value("${platform.commerce.stripe-secret-key:}") String secretKey,
      @Value("${platform.commerce.stripe-webhook-secret:}") String webhookSecret,
      @Value("${platform.commerce.stripe-api-base-url:https://api.stripe.com}") String baseUrl,
      @Value("${platform.storefront-base-url:http://localhost:8081}") String storefrontBaseUrl,
      ObjectMapper json) {
    this.secretKey = secretKey == null ? "" : secretKey;
    this.webhookSecret = webhookSecret == null ? "" : webhookSecret;
    this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    this.storefrontBaseUrl =
        storefrontBaseUrl.endsWith("/")
            ? storefrontBaseUrl.substring(0, storefrontBaseUrl.length() - 1)
            : storefrontBaseUrl;
    this.json = json;
  }

  /** Reports whether a live provider is configured. */
  public boolean active() {
    return !secretKey.isBlank();
  }

  /** Returns the signing secret for webhook verification. */
  public String signatureSecret() {
    return webhookSecret;
  }

  @Override
  public Session createCheckoutSession(
      OrderService.Order order, int itemCount, Instant expiresAt, String accessToken) {
    if (!active()) throw new IllegalStateException("No payment provider is configured.");
    String successUrl =
        storefrontBaseUrl
            + "/order/"
            + encode(order.orderNumber())
            + "#token="
            + encode(accessToken);
    String cancelUrl = storefrontBaseUrl + "/cart?cancelled=1";
    List<String> form =
        List.of(
            "mode=" + encode("payment"),
            "success_url=" + encode(successUrl),
            "cancel_url=" + encode(cancelUrl),
            "client_reference_id=" + encode(order.orderNumber()),
            "expires_at=" + expiresAt.getEpochSecond(),
            "line_items[0][quantity]=1",
            "line_items[0][price_data][currency]=" + encode("eur"),
            "line_items[0][price_data][product_data][name]=" + encode(productName(itemCount)),
            "line_items[0][price_data][unit_amount]=" + order.totalCents());
    JsonNode response = post("/v1/checkout/sessions", form, "checkout-" + order.id());
    String sessionId = text(response, "id");
    String url = text(response, "url");
    if (sessionId.isBlank() || url.isBlank())
      throw new IllegalStateException("The payment provider did not return a checkout session.");
    return new Session(sessionId, url);
  }

  @Override
  public String refund(String paymentIntentId, long amountCents, String idempotencyKey) {
    if (!active()) throw new IllegalStateException("No payment provider is configured.");
    if (paymentIntentId == null || paymentIntentId.isBlank())
      throw new IllegalArgumentException("The order has no payment identifier.");
    List<String> form =
        List.of("amount=" + Math.max(1L, amountCents), "payment_intent=" + encode(paymentIntentId));
    JsonNode response = post("/v1/refunds", form, idempotencyKey);
    String refundId = text(response, "id");
    if (refundId.isBlank())
      throw new IllegalStateException("The payment provider did not return a refund identifier.");
    return refundId;
  }

  private JsonNode post(String path, List<String> form, String idempotencyKey) {
    StringBuilder body = new StringBuilder();
    for (int i = 0; i < form.size(); i++) {
      if (i > 0) body.append('&');
      body.append(form.get(i));
    }
    try {
      return http.post()
          .uri(baseUrl + path)
          .header("Authorization", "Bearer " + secretKey)
          .header("Idempotency-Key", idempotencyKey)
          .contentType(MediaType.APPLICATION_FORM_URLENCODED)
          .body(body.toString())
          .retrieve()
          .body(JsonNode.class);
    } catch (org.springframework.web.client.RestClientException error) {
      throw new IllegalStateException(
          "The payment provider did not respond. Try again later.", error);
    }
  }

  /** Builds a short Stripe product label without exceeding the 40-character limit. */
  private static String productName(int itemCount) {
    String name = itemCount <= 1 ? "Order with 1 product" : "Order with " + itemCount + " products";
    return name.length() > 40 ? name.substring(0, 40) : name;
  }

  private static String text(JsonNode node, String field) {
    if (node == null) return "";
    JsonNode value = node.get(field);
    return value == null || !value.isTextual() ? "" : value.asText();
  }

  private static String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }
}
