package de.openshopfront.platform.commerce;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** Verifies transactional handling of Stripe retries and refund events. */
class CommerceWebhookControllerTest {
  private static final String SECRET = "whsec_controller_test";

  @Test
  void businessFailureEscapesTheTransactionalWebhook() throws Exception {
    OrderService orders = mock(OrderService.class);
    StripePaymentProvider stripe = mock(StripePaymentProvider.class);
    NamedParameterJdbcTemplate db = mock(NamedParameterJdbcTemplate.class);
    UUID orderId = UUID.randomUUID();
    String payload =
        """
        {"id":"evt_paid","type":"checkout.session.completed","data":{"object":{"id":"cs_1","payment_status":"paid","payment_intent":"pi_1"}}}
        """;
    when(stripe.signatureSecret()).thenReturn(SECRET);
    when(db.update(contains("on conflict"), anyMap())).thenReturn(1);
    when(orders.orderIdForSession("cs_1")).thenReturn(orderId);
    org.mockito.Mockito.doThrow(new IllegalStateException("database failure"))
        .when(orders)
        .markPaid(orderId, "pi_1");
    CommerceWebhookController subject =
        new CommerceWebhookController(orders, stripe, new ObjectMapper(), db);

    assertThatThrownBy(() -> subject.receive(signature(payload), payload))
        .isInstanceOf(IllegalStateException.class);
    assertThat(
            CommerceWebhookController.class
                .getMethod("receive", String.class, String.class)
                .isAnnotationPresent(Transactional.class))
        .isTrue();
  }

  @Test
  void verifiedRefundEventFinalizesTheLocalRefund() throws Exception {
    OrderService orders = mock(OrderService.class);
    StripePaymentProvider stripe = mock(StripePaymentProvider.class);
    NamedParameterJdbcTemplate db = mock(NamedParameterJdbcTemplate.class);
    UUID orderId = UUID.randomUUID();
    String payload =
        """
        {"id":"evt_refund","type":"charge.refunded","data":{"object":{"payment_intent":"pi_2"}}}
        """;
    when(stripe.signatureSecret()).thenReturn(SECRET);
    when(db.update(contains("on conflict"), anyMap())).thenReturn(1);
    when(orders.orderIdForPayment("pi_2")).thenReturn(orderId);
    CommerceWebhookController subject =
        new CommerceWebhookController(orders, stripe, new ObjectMapper(), db);

    subject.receive(signature(payload), payload);

    verify(orders).refund(orderId);
  }

  private static String signature(String payload) throws Exception {
    long timestamp = System.currentTimeMillis() / 1000L;
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
    byte[] digest = mac.doFinal((timestamp + "." + payload).getBytes(StandardCharsets.UTF_8));
    return "t=" + timestamp + ",v1=" + java.util.HexFormat.of().formatHex(digest);
  }
}
