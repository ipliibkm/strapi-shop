package de.openshopfront.platform.commerce;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.openshopfront.platform.mail.EmailSettingsService;
import de.openshopfront.platform.mail.EmailSettingsService.Channel;
import de.openshopfront.platform.mail.EmailSettingsService.Rendered;
import de.openshopfront.platform.mail.MailService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/** Verifies order transitions that protect reserved stock. */
class OrderServiceTest {
  private final NamedParameterJdbcTemplate db = mock(NamedParameterJdbcTemplate.class);
  private final CommerceSettingsService commerce = mock(CommerceSettingsService.class);
  private final MailService mail = mock(MailService.class);
  private final EmailSettingsService email = mock(EmailSettingsService.class);
  private final OrderService orders =
      new OrderService(db, commerce, new SimpleMeterRegistry(), mail, email);

  @Test
  @SuppressWarnings("unchecked")
  void paymentKeepsReservedStockUntilFulfillment() {
    UUID orderId = UUID.randomUUID();
    doReturn(List.of(order(orderId, "PENDING_PAYMENT")))
        .when(db)
        .query(any(String.class), anyMap(), any(RowMapper.class));
    // The generic stub above also answers the notification item query; return no items.
    doReturn(List.<OrderService.OrderItem>of())
        .when(db)
        .query(contains("shop_order_item"), anyMap(), any(RowMapper.class));
    when(db.update(startsWith("update shop_order set status='PAID'"), anyMap())).thenReturn(1);
    when(email.render(eq(Channel.ORDER_CONFIRMATION), isNull(), any(), anyMap(), anyString()))
        .thenAnswer(
            invocation -> {
              String fallback = invocation.getArgument(4, String.class);
              return new Rendered("Order confirmed: SO-12345678", fallback, false);
            });

    orders.markPaid(orderId, "pi_123");

    verify(mail)
        .send(
            eq("customer@example.com"),
            contains("SO-12345678"),
            org.mockito.ArgumentMatchers.anyString());
    verify(db, never()).update(contains("reserved_quantity"), anyMap());
    verify(db).update(contains("status='SUCCEEDED'"), anyMap());
  }

  @Test
  @SuppressWarnings("unchecked")
  void expiryCancelsOrdersAndReleasesTheirReservations() {
    UUID orderId = UUID.randomUUID();
    doReturn(List.of(orderId))
        .when(db)
        .query(startsWith("select id from shop_order"), anyMap(), any(RowMapper.class));
    when(db.update(startsWith("update shop_order set status='CANCELLED'"), anyMap())).thenReturn(1);

    int expired = orders.expirePendingOrders();

    org.assertj.core.api.Assertions.assertThat(expired).isEqualTo(1);
    verify(db).update(contains("reserved_quantity = greatest"), anyMap());
    verify(db)
        .update(
            argThat(sql -> sql.contains("status='CANCELLED'") && sql.contains("PENDING_PAYMENT")),
            anyMap());
  }

  @Test
  @SuppressWarnings("unchecked")
  void publicLookupRequiresTheHashedCustomerToken() {
    String number = "SO-0123456789ABCDEF0123456789ABCDEF";
    String token = "0123456789abcdefghijklmnopqrstuvwxyz_ABCDEF";
    doReturn(List.of(order(UUID.randomUUID(), "PAID")))
        .when(db)
        .query(
            contains("public_access_token_hash=:accessTokenHash"),
            org.mockito.ArgumentMatchers.<java.util.Map<String, ?>>argThat(
                values ->
                    number.equals(values.get("number"))
                        && OrderService.tokenHash(token).equals(values.get("accessTokenHash"))),
            any(RowMapper.class));

    assertThat(orders.findPublicOrder(number, token).status()).isEqualTo("PAID");
  }

  @Test
  void publicLookupRejectsAnInvalidTokenBeforeQuerying() {
    assertThatThrownBy(
            () -> orders.findPublicOrder("SO-0123456789ABCDEF0123456789ABCDEF", "short-token"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("This order does not exist.");
  }

  @Test
  @SuppressWarnings("unchecked")
  void checkoutRetriesAnIdentifierConflictAndReturnsStrongIdentifiers() {
    UUID variantId = UUID.randomUUID();
    when(commerce.require())
        .thenReturn(new CommerceSettingsService.Settings(true, false, 0, 0, null));
    org.mockito.Mockito.doAnswer(
            invocation -> {
              RowMapper<?> mapper = invocation.getArgument(2);
              java.sql.ResultSet row = mock(java.sql.ResultSet.class);
              when(row.getObject("id", UUID.class)).thenReturn(variantId);
              when(row.getString("sku")).thenReturn("SKU-1");
              when(row.getLong("price_cents")).thenReturn(2500L);
              when(row.getString("product_name")).thenReturn("Product");
              when(row.getInt("available")).thenReturn(5);
              mapper.mapRow(row, 0);
              return List.of();
            })
        .when(db)
        .query(contains("from product_variant"), anyMap(), any(RowMapper.class));
    AtomicInteger orderInserts = new AtomicInteger();
    when(db.update(anyString(), anyMap()))
        .thenAnswer(
            invocation -> {
              String sql = invocation.getArgument(0);
              if (sql.contains("insert into shop_order ("))
                return orderInserts.getAndIncrement() == 0 ? 0 : 1;
              return 1;
            });

    OrderService.CheckoutResult result =
        orders.checkout(
            new OrderService.CheckoutRequest(
                "Customer",
                "customer@example.com",
                null,
                null,
                null,
                null,
                "en",
                List.of(new OrderService.LineInput(variantId, 1))));

    assertThat(result.orderNumber()).matches("SO-[A-F0-9]{32}");
    assertThat(result.accessToken()).matches("[A-Za-z0-9_-]{43}");
    verify(db, times(2)).update(contains("insert into shop_order ("), anyMap());
  }

  private static OrderService.Order order(UUID id, String status) {
    return new OrderService.Order(
        id,
        "SO-12345678",
        status,
        "Customer",
        "customer@example.com",
        null,
        null,
        null,
        "EUR",
        1000,
        0,
        0,
        1000,
        Instant.now(),
        "de");
  }
}
