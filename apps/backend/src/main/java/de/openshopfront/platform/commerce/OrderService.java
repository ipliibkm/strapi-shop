package de.openshopfront.platform.commerce;

import de.openshopfront.platform.mail.EmailSettingsService;
import de.openshopfront.platform.mail.MailService;
import io.micrometer.core.instrument.MeterRegistry;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Types;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Confirms orders, holds stock for payment, and manages fulfillment and refunds. */
@Service
public class OrderService {
  private static final Logger log = LoggerFactory.getLogger(OrderService.class);
  private static final java.time.Duration CHECKOUT_WINDOW = java.time.Duration.ofMinutes(31);
  private static final SecureRandom RANDOM = new SecureRandom();
  private static final int ORDER_ID_ATTEMPTS = 5;
  private final NamedParameterJdbcTemplate db;
  private final CommerceSettingsService commerce;
  private final MeterRegistry metrics;
  private final MailService mail;
  private final EmailSettingsService email;

  public OrderService(
      NamedParameterJdbcTemplate db,
      CommerceSettingsService commerce,
      MeterRegistry metrics,
      MailService mail,
      EmailSettingsService email) {
    this.db = db;
    this.commerce = commerce;
    this.metrics = metrics;
    this.mail = mail;
    this.email = email;
  }

  /** Creates an unpaid order and holds stock before the customer pays. */
  @Transactional
  public CheckoutResult checkout(CheckoutRequest request) {
    CommerceSettingsService.Settings config = commerce.require();
    if (!config.shopEnabled()) throw new ShopDisabledException();
    String name = clean(request.customerName(), 200, "Enter your full name.");
    String email = cleanEmail(request.customerEmail());
    List<Line> lines = validateLines(request.lines());

    Map<UUID, VariantState> stock = lockVariants(lines);
    for (Line line : lines) {
      if (!stock.containsKey(line.variantId()))
        throw new IllegalArgumentException("One of the selected products is not available.");
      if (stock.get(line.variantId()).availableQuantity < line.quantity())
        throw new OutOfStockException(stock.get(line.variantId()).productName);
    }

    long subtotal = 0L;
    for (Line line : lines) {
      subtotal += stock.get(line.variantId()).priceCents * line.quantity();
    }
    long tax = CommercePricing.taxCents(subtotal, config.taxRateBps());
    long shipping = CommercePricing.shippingCents(subtotal, config);
    long total = CommercePricing.totalCents(subtotal, tax, shipping);
    Instant expiresAt = Instant.now().plus(CHECKOUT_WINDOW);

    UUID orderId = UUID.randomUUID();
    String orderNumber = null;
    String accessToken = null;
    for (int attempt = 0; attempt < ORDER_ID_ATTEMPTS; attempt++) {
      orderNumber = newOrderNumber();
      accessToken = newAccessToken();
      Map<String, Object> params = new HashMap<>();
      params.put("id", orderId);
      params.put("number", orderNumber);
      params.put("accessTokenHash", tokenHash(accessToken));
      params.put("name", name);
      params.put("email", email);
      params.put("street", optional(request.street()));
      params.put("city", optional(request.city()));
      params.put("postal", optional(request.postalCode()));
      params.put("country", country(request.country()));
      params.put("locale", locale(request.locale()));
      params.put("subtotal", subtotal);
      params.put("tax", tax);
      params.put("shipping", shipping);
      params.put("total", total);
      // Bind the Instant with an explicit SQL type; Postgres cannot infer it from the object.
      params.put(
          "expires",
          new SqlParameterValue(Types.TIMESTAMP_WITH_TIMEZONE, expiresAt.atOffset(ZoneOffset.UTC)));
      int inserted =
          db.update(
              """
              insert into shop_order (id,order_number,public_access_token_hash,status,customer_name,customer_email,
                   street,city,postal_code,country,currency,subtotal_cents,tax_cents,shipping_cents,total_cents,
                   expires_at,locale)
               values (:id,:number,:accessTokenHash,'PENDING_PAYMENT',:name,:email,:street,:city,:postal,:country,
                   'EUR',:subtotal,:tax,:shipping,:total,:expires,:locale)
               on conflict do nothing""",
              params);
      if (inserted == 1) break;
      orderNumber = null;
      accessToken = null;
    }
    if (orderNumber == null || accessToken == null)
      throw new IllegalStateException("The order identifier could not be created. Try again.");

    for (Line line : lines) {
      VariantState variant = stock.get(line.variantId());
      db.update(
          """
          insert into shop_order_item (id,order_id,variant_id,product_name,variant_label,quantity,unit_price_cents,line_total_cents)
          values (:id,:orderId,:variantId,:productName,:label,:quantity,:price,:lineTotal)""",
          Map.of(
              "id",
              UUID.randomUUID(),
              "orderId",
              orderId,
              "variantId",
              line.variantId(),
              "productName",
              variant.productName,
              "label",
              variant.sku,
              "quantity",
              line.quantity(),
              "price",
              variant.priceCents,
              "lineTotal",
              variant.priceCents * line.quantity()));
      int released =
          db.update(
              """
              update product_variant set reserved_quantity = reserved_quantity + :qty
               where id=:id and stock_quantity - reserved_quantity >= :qty""",
              Map.of("id", line.variantId(), "qty", line.quantity()));
      if (released == 0) {
        metrics.counter("commerce.checkout.failed", "reason", "OUT_OF_STOCK").increment();
        throw new OutOfStockException(variant.productName);
      }
    }

    db.update(
        "insert into payment (id,order_id,provider,amount_cents,status) values (:id,:orderId,'stripe',:amount,'PENDING')",
        Map.of("id", UUID.randomUUID(), "orderId", orderId, "amount", total));

    metrics.counter("commerce.orders.created").increment();
    int itemCount = lines.stream().mapToInt(Line::quantity).sum();
    return new CheckoutResult(orderId, orderNumber, accessToken, total, itemCount, expiresAt);
  }

  /** Attaches a Stripe checkout session after the remote call succeeds. */
  @Transactional
  public void attachSession(UUID orderId, String sessionId) {
    int updated =
        db.update(
            "update shop_order set stripe_session_id=:session where id=:id and status='PENDING_PAYMENT'",
            Map.of("id", orderId, "session", sessionId));
    if (updated == 0) throw new IllegalArgumentException("This order no longer accepts payment.");
  }

  /** Marks an order paid and keeps its stock hold. Repeated webhooks are ignored. */
  @Transactional
  public void markPaid(UUID orderId, String externalPaymentId) {
    Order order = requireOrderForUpdate(orderId);
    if ("PAID".equals(order.status()) || "FULFILLED".equals(order.status())) return;
    if (!"PENDING_PAYMENT".equals(order.status()))
      throw new IllegalArgumentException("Only an unpaid order can be marked paid.");
    int updated =
        db.update(
            "update shop_order set status='PAID' where id=:id and status='PENDING_PAYMENT'",
            Map.of("id", orderId));
    if (updated != 1) throw new IllegalStateException("The payment state changed concurrently.");
    metrics.counter("commerce.orders.paid").increment();
    if (externalPaymentId != null && !externalPaymentId.isBlank()) {
      db.update(
          "update payment set status='SUCCEEDED',external_id=:external where order_id=:orderId",
          Map.of("orderId", orderId, "external", externalPaymentId));
    } else {
      db.update(
          "update payment set status='SUCCEEDED' where order_id=:orderId",
          Map.of("orderId", orderId));
    }
    notifyOrderPaid(order);
  }

  /** Confirms the purchase by email once the paid state is committed. */
  private void notifyOrderPaid(Order order) {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              sendOrderNotifications(order);
            }
          });
    } else {
      sendOrderNotifications(order);
    }
  }

  /** Sends the customer confirmation. Failures never fail payment. */
  private void sendOrderNotifications(Order order) {
    try {
      List<OrderItem> items = loadNotificationItems(order.id());
      String fallback = orderBody(order, items).strip();
      EmailSettingsService.Rendered rendered =
          email.render(
              EmailSettingsService.Channel.ORDER_CONFIRMATION,
              null,
              order.locale(),
              orderVariables(order, items),
              fallback);
      if (rendered.html()) {
        mail.sendHtml(order.customerEmail(), rendered.subject(), rendered.body());
      } else {
        mail.send(order.customerEmail(), rendered.subject(), rendered.body());
      }
    } catch (RuntimeException error) {
      log.warn("The order confirmation for {} could not be sent.", order.orderNumber(), error);
    }
  }

  /** Builds the template variables of one order in the documented send order. */
  private static Map<String, String> orderVariables(Order order, List<OrderItem> items) {
    String currency = order.currency() == null ? "EUR" : order.currency();
    StringBuilder list = new StringBuilder();
    int index = 1;
    for (OrderItem item : items) {
      list.append(index++).append(". ").append(item.productName());
      if (item.variantLabel() != null && !item.variantLabel().isBlank())
        list.append(" (").append(item.variantLabel()).append(')');
      list.append(" x")
          .append(item.quantity())
          .append(": ")
          .append(money(currency, item.lineTotalCents()))
          .append('\n');
    }
    Map<String, String> variables = new LinkedHashMap<>();
    variables.put("orderNumber", order.orderNumber() == null ? "" : order.orderNumber());
    variables.put("customerName", orEmpty(order.customerName()));
    variables.put("email", orEmpty(order.customerEmail()));
    variables.put("street", orEmpty(order.street()));
    variables.put("city", orEmpty(order.city()));
    variables.put("postalCode", orEmpty(order.postalCode()));
    variables.put("items", list.toString().strip());
    variables.put("subtotal", money(currency, order.subtotalCents()));
    variables.put("tax", money(currency, order.taxCents()));
    variables.put("shipping", money(currency, order.shippingCents()));
    variables.put("total", money(currency, order.totalCents()));
    variables.put(
        "date",
        order.createdAt() == null
            ? ""
            : order.createdAt().atZone(java.time.ZoneOffset.UTC).toString());
    return variables;
  }

  private static String orEmpty(String value) {
    return value == null ? "" : value.strip();
  }

  private List<OrderItem> loadNotificationItems(UUID orderId) {
    return db.query(
        """
        select product_name,variant_label,quantity,line_total_cents from shop_order_item
         where order_id=:id order by id""",
        Map.of("id", orderId),
        (rs, row) ->
            new OrderItem(
                rs.getString("product_name"),
                rs.getString("variant_label"),
                rs.getInt("quantity"),
                0L,
                rs.getLong("line_total_cents"),
                0,
                0));
  }

  /** Builds the shared order confirmation text for customer and business. */
  private static String orderBody(Order order, List<OrderItem> items) {
    StringBuilder body = new StringBuilder();
    body.append("Thank you for your order.\n\n");
    body.append("Order: ").append(order.orderNumber()).append('\n');
    int index = 1;
    for (OrderItem item : items) {
      body.append(index++).append(". ").append(item.productName());
      if (item.variantLabel() != null && !item.variantLabel().isBlank())
        body.append(" (").append(item.variantLabel()).append(')');
      body.append(" x")
          .append(item.quantity())
          .append(": ")
          .append(money(order.currency(), item.lineTotalCents()));
    }
    String currency = order.currency() == null ? "EUR" : order.currency();
    body.append('\n').append('\n');
    appendAmount(body, "Subtotal", order.subtotalCents(), currency);
    if (order.taxCents() > 0) appendAmount(body, "Tax", order.taxCents(), currency);
    appendAmount(body, "Shipping", order.shippingCents(), currency);
    body.append("Total: ").append(money(currency, order.totalCents())).append("\n\n");
    if (order.customerName() != null && !order.customerName().isBlank()) {
      body.append("Customer: ").append(order.customerName()).append('\n');
    }
    if (order.street() != null) body.append(order.street()).append('\n');
    String location =
        (order.postalCode() == null ? "" : order.postalCode() + " ")
            + (order.city() == null ? "" : order.city());
    if (!location.isBlank()) body.append(location.strip()).append('\n');
    return body.toString();
  }

  private static void appendAmount(StringBuilder body, String label, long cents, String currency) {
    body.append(label).append(": ").append(money(currency, cents)).append('\n');
  }

  /** Formats one amount in minor units for the mail text. */
  private static String money(String currency, long cents) {
    return String.format(Locale.ROOT, "%,.2f %s", cents / 100.0, currency);
  }

  /** Cancels an unpaid order and releases its stock hold. */
  @Transactional
  public void cancel(UUID orderId) {
    Order order = requireOrderForUpdate(orderId);
    if (!"PENDING_PAYMENT".equals(order.status()))
      throw new IllegalArgumentException("Only an unpaid order can be cancelled.");
    cancelLocked(orderId);
    metrics.counter("commerce.orders.cancelled").increment();
  }

  /** Cancels due unpaid orders and returns the number of released reservations. */
  @Transactional
  public int expirePendingOrders() {
    List<UUID> due =
        db.query(
            """
            select id from shop_order
             where status='PENDING_PAYMENT' and expires_at is not null and expires_at<=now()
             order by expires_at for update skip locked limit 100
            """,
            Map.of(),
            (rs, row) -> rs.getObject(1, UUID.class));
    for (UUID orderId : due) cancelLocked(orderId);
    if (!due.isEmpty())
      metrics.counter("commerce.orders.cancelled", "reason", "EXPIRED").increment(due.size());
    return due.size();
  }

  /** Ships a paid order, deducts stock, and stores the tracking details. */
  @Transactional
  public void fulfill(UUID orderId, String carrier, String trackingNumber) {
    Order order = requireOrderForUpdate(orderId);
    if (!"PAID".equals(order.status()))
      throw new IllegalArgumentException("Only a paid order can be shipped.");
    List<ItemRow> items = loadItemsWithVariants(orderId);
    for (ItemRow item : items) {
      int pending = item.quantity - item.fulfilledQuantity;
      if (pending <= 0 || item.variantId == null) continue;
      int updated =
          db.update(
              """
              update product_variant set stock_quantity = stock_quantity - :qty,
                 reserved_quantity = reserved_quantity - :qty
               where id=:id and stock_quantity>=:qty and reserved_quantity>=:qty
              """,
              Map.of("id", item.variantId, "qty", pending));
      if (updated != 1)
        throw new IllegalStateException(
            "Reserved stock is missing. Do not fulfill this order until inventory is repaired.");
    }
    db.update(
        "update shop_order_item set fulfilled_quantity=quantity where order_id=:orderId and quantity>fulfilled_quantity",
        Map.of("orderId", orderId));
    db.update("update shop_order set status='FULFILLED' where id=:id", Map.of("id", orderId));
    db.update(
        """
        insert into shipment (order_id,carrier,tracking_number,status,shipped_at)
        values (:orderId,:carrier,:tracking,'SHIPPED',now())
        on conflict (order_id) do update set carrier=excluded.carrier,
             tracking_number=excluded.tracking_number,status='SHIPPED',shipped_at=now()""",
        Map.of(
            "orderId",
            orderId,
            "carrier",
            optional(carrier),
            "tracking",
            optional(trackingNumber)));
    metrics.counter("commerce.orders.fulfilled").increment();
  }

  /** Refunds a paid or shipped order once and restocks already-shipped items. */
  @Transactional
  public void refund(UUID orderId) {
    Order order = requireOrderForUpdate(orderId);
    if ("CANCELLED".equals(order.status()) && paymentIsRefunded(orderId)) return;
    if (!"PAID".equals(order.status()) && !"FULFILLED".equals(order.status()))
      throw new IllegalArgumentException("Only a paid or shipped order can be refunded.");
    if (paymentIsRefunded(orderId)) return;

    List<ItemRow> items = loadItemsWithVariants(orderId);
    for (ItemRow item : items) {
      int shipped = Math.max(0, item.fulfilledQuantity);
      if (shipped > 0 && item.variantId != null)
        db.update(
            "update product_variant set stock_quantity = stock_quantity + :qty where id=:id",
            Map.of("id", item.variantId, "qty", shipped));
    }
    db.update(
        "update shop_order_item set refunded_quantity=quantity where order_id=:orderId and quantity>refunded_quantity",
        Map.of("orderId", orderId));
    releaseHolds(orderId);
    db.update(
        "update payment set status='REFUNDED' where order_id=:orderId and status in ('SUCCEEDED','PARTIALLY_REFUNDED')",
        Map.of("orderId", orderId));
    db.update(
        "update shop_order set status='CANCELLED' where id=:id and status in ('PAID','FULFILLED')",
        Map.of("id", orderId));
    metrics.counter("commerce.orders.refunded").increment();
  }

  /** Loads the full order for the administrator screen. */
  @Transactional(readOnly = true)
  public OrderDetail detail(UUID orderId) {
    List<Order> orders =
        db.query(orderSelect() + " where o.id=:id", Map.of("id", orderId), ORDER_MAPPER);
    if (orders.isEmpty()) throw new IllegalArgumentException("This order does not exist.");
    Order order = orders.get(0);

    List<OrderItem> items =
        db.query(
            """
            select product_name,variant_label,quantity,unit_price_cents,line_total_cents,fulfilled_quantity,refunded_quantity
              from shop_order_item where order_id=:id order by id""",
            Map.of("id", orderId),
            (rs, row) ->
                new OrderItem(
                    rs.getString("product_name"),
                    rs.getString("variant_label"),
                    rs.getInt("quantity"),
                    rs.getLong("unit_price_cents"),
                    rs.getLong("line_total_cents"),
                    rs.getInt("fulfilled_quantity"),
                    rs.getInt("refunded_quantity")));

    Payment payment = null;
    List<Payment> payments = db.query(paymentSql(), Map.of("id", orderId), PAYMENT_MAPPER);
    if (!payments.isEmpty()) payment = payments.get(0);

    Shipment shipment = null;
    List<Shipment> shipments = db.query(shipmentSql(), Map.of("id", orderId), SHIPMENT_MAPPER);
    if (!shipments.isEmpty()) shipment = shipments.get(0);

    return new OrderDetail(order, List.copyOf(items), payment, shipment);
  }

  /** Lists recent orders newest first for the administrator screen. */
  @Transactional(readOnly = true)
  public List<Order> list(String statusFilter, int limit) {
    String where = "1=1";
    Map<String, Object> params = new HashMap<>();
    if (statusFilter != null && !statusFilter.isBlank()) {
      where += " and o.status=:status";
      params.put("status", statusFilter.strip().toUpperCase());
    }
    params.put("limit", Math.max(1, Math.min(limit, 200)));
    return db.query(
        orderSelect() + " where " + where + " order by o.created_at desc limit :limit",
        params,
        ORDER_MAPPER);
  }

  /** Finds one confirmed order by its customer-facing number. */
  @Transactional(readOnly = true)
  public Order findByNumber(String orderNumber) {
    if (orderNumber == null || !orderNumber.matches("SO-(?:[A-Fa-f0-9]{8}|[A-Fa-f0-9]{32})"))
      throw new IllegalArgumentException("This order does not exist.");
    List<Order> orders =
        db.query(
            orderSelect() + " where o.order_number=:number",
            Map.of("number", orderNumber.toUpperCase()),
            ORDER_MAPPER);
    if (orders.isEmpty()) throw new IllegalArgumentException("This order does not exist.");
    return orders.get(0);
  }

  /** Finds one customer order only when its private access token matches. */
  @Transactional(readOnly = true)
  public Order findPublicOrder(String orderNumber, String accessToken) {
    if (orderNumber == null
        || !orderNumber.matches("SO-[A-Fa-f0-9]{32}")
        || accessToken == null
        || !accessToken.matches("[A-Za-z0-9_-]{43}"))
      throw new IllegalArgumentException("This order does not exist.");
    List<Order> orders =
        db.query(
            orderSelect()
                + " where o.order_number=:number and o.public_access_token_hash=:accessTokenHash",
            Map.of(
                "number", orderNumber.toUpperCase(Locale.ROOT),
                "accessTokenHash", tokenHash(accessToken)),
            ORDER_MAPPER);
    if (orders.isEmpty()) throw new IllegalArgumentException("This order does not exist.");
    return orders.get(0);
  }

  /** Finds the order referenced by a Stripe checkout session. */
  @Transactional(readOnly = true)
  public UUID orderIdForSession(String sessionId) {
    if (sessionId == null || sessionId.isBlank()) return null;
    List<UUID> ids =
        db.query(
            "select id from shop_order where stripe_session_id=:session",
            Map.of("session", sessionId),
            (rs, row) -> rs.getObject(1, UUID.class));
    return ids.stream().findFirst().orElse(null);
  }

  /** Finds the order referenced by a provider payment identifier. */
  @Transactional(readOnly = true)
  public UUID orderIdForPayment(String externalPaymentId) {
    if (externalPaymentId == null || externalPaymentId.isBlank()) return null;
    List<UUID> ids =
        db.query(
            "select order_id from payment where external_id=:external",
            Map.of("external", externalPaymentId),
            (rs, row) -> rs.getObject(1, UUID.class));
    return ids.stream().findFirst().orElse(null);
  }

  private static String orderSelect() {
    return """
        select o.id,o.order_number,o.status,o.customer_name,o.customer_email,o.street,o.city,o.postal_code,o.currency,
         o.subtotal_cents,o.tax_cents,o.shipping_cents,o.total_cents,o.created_at,o.locale from shop_order o""";
  }

  private String paymentSql() {
    return "select provider,external_id,amount_cents,status from payment where order_id=:id";
  }

  private String shipmentSql() {
    return "select carrier,tracking_number,status,shipped_at from shipment where order_id=:id";
  }

  private static final RowMapper<Order> ORDER_MAPPER =
      (rs, row) ->
          new Order(
              rs.getObject("id", UUID.class),
              rs.getString("order_number"),
              rs.getString("status"),
              rs.getString("customer_name"),
              rs.getString("customer_email"),
              rs.getString("street"),
              rs.getString("city"),
              rs.getString("postal_code"),
              rs.getString("currency"),
              rs.getLong("subtotal_cents"),
              rs.getLong("tax_cents"),
              rs.getLong("shipping_cents"),
              rs.getLong("total_cents"),
              toInstant(rs.getTimestamp("created_at")),
              rs.getString("locale"));

  private static final RowMapper<Payment> PAYMENT_MAPPER =
      (rs, row) ->
          new Payment(
              rs.getString("provider"),
              rs.getString("external_id"),
              rs.getLong("amount_cents"),
              rs.getString("status"));

  private static final RowMapper<Shipment> SHIPMENT_MAPPER =
      (rs, row) ->
          new Shipment(
              rs.getString("carrier"),
              rs.getString("tracking_number"),
              rs.getString("status"),
              toInstant(rs.getTimestamp("shipped_at")));

  private List<ItemRow> loadItemsWithVariants(UUID orderId) {
    return db.query(
        """
        select i.id,i.variant_id,i.quantity,i.fulfilled_quantity from shop_order_item i
         where i.order_id=:orderId order by i.id""",
        Map.of("orderId", orderId),
        (rs, row) ->
            new ItemRow(
                rs.getObject("id", UUID.class),
                rs.getObject("variant_id", UUID.class),
                rs.getInt("quantity"),
                rs.getInt("fulfilled_quantity")));
  }

  private void releaseHolds(UUID orderId) {
    db.update(
        """
        update product_variant v set reserved_quantity = greatest(0, v.reserved_quantity - i.quantity)
          from shop_order_item i where i.order_id=:orderId and v.id=i.variant_id""",
        Map.of("orderId", orderId));
  }

  private void cancelLocked(UUID orderId) {
    releaseHolds(orderId);
    int updated =
        db.update(
            "update shop_order set status='CANCELLED' where id=:id and status='PENDING_PAYMENT'",
            Map.of("id", orderId));
    if (updated != 1) throw new IllegalStateException("The order state changed concurrently.");
    db.update(
        "update payment set status='FAILED' where order_id=:orderId and status='PENDING'",
        Map.of("orderId", orderId));
  }

  private boolean paymentIsRefunded(UUID orderId) {
    return Boolean.TRUE.equals(
        db.queryForObject(
            "select exists (select 1 from payment where order_id=:orderId and status='REFUNDED')",
            Map.of("orderId", orderId),
            Boolean.class));
  }

  private Map<UUID, VariantState> lockVariants(List<Line> lines) {
    List<Object> variantIds = new ArrayList<>();
    for (Line line : lines) variantIds.add(line.variantId());
    Map<UUID, VariantState> result = new HashMap<>();
    db.query(
        """
        select v.id,v.sku,v.price_cents,p.name as product_name,(v.stock_quantity - v.reserved_quantity) as available
          from product_variant v join product p on p.id=v.product_id
         where v.id in (:ids) and v.active=true and p.active=true for update""",
        Map.of("ids", variantIds),
        (rs, row) -> {
          VariantState state =
              new VariantState(
                  rs.getObject("id", UUID.class),
                  rs.getString("sku"),
                  rs.getLong("price_cents"),
                  rs.getString("product_name"),
                  Math.max(0, rs.getInt("available")));
          result.put(state.id(), state);
          return null;
        });
    if (result.size() != variantIds.stream().distinct().count())
      throw new IllegalArgumentException("One of the selected products is not available.");
    return result;
  }

  private List<Line> validateLines(List<LineInput> inputs) {
    if (inputs == null || inputs.isEmpty())
      throw new IllegalArgumentException("Add at least one product to the order.");
    if (inputs.size() > 25)
      throw new IllegalArgumentException("Use no more than 25 different products per order.");
    List<Line> lines = new ArrayList<>();
    Set<UUID> seen = new java.util.HashSet<>();
    for (LineInput input : inputs) {
      if (input.variantId() == null || !seen.add(input.variantId())) continue;
      int quantity = Math.max(1, Math.min(99, input.quantity()));
      lines.add(new Line(input.variantId(), quantity));
    }
    if (lines.isEmpty())
      throw new IllegalArgumentException("Add at least one product to the order.");
    return List.copyOf(lines);
  }

  private Order requireOrder(UUID orderId) {
    List<Order> orders =
        db.query(orderSelect() + " where o.id=:id", Map.of("id", orderId), ORDER_MAPPER);
    if (orders.isEmpty()) throw new IllegalArgumentException("This order does not exist.");
    return orders.get(0);
  }

  private Order requireOrderForUpdate(UUID orderId) {
    List<Order> orders =
        db.query(orderSelect() + " where o.id=:id for update", Map.of("id", orderId), ORDER_MAPPER);
    if (orders.isEmpty()) throw new IllegalArgumentException("This order does not exist.");
    return orders.get(0);
  }

  private static String clean(String value, int max, String message) {
    if (value == null || value.strip().isEmpty() || value.strip().length() > max)
      throw new IllegalArgumentException(message);
    return value.strip();
  }

  private static String cleanEmail(String value) {
    if (value == null || !value.matches("[^@\\s]+@[^@\\s]+\\.[A-Za-z]{2,}") || value.length() > 254)
      throw new IllegalArgumentException("Enter a valid email address.");
    return value.strip();
  }

  private static String optional(String value) {
    if (value == null || value.isBlank()) return null;
    String cleanValue = value.strip().replace("\u0000", "");
    return cleanValue.length() > 200 ? cleanValue.substring(0, 200) : cleanValue;
  }

  private static String country(String value) {
    if (value == null || !value.matches("[A-Za-z]{2}")) return null;
    return value.toUpperCase();
  }

  private static String newOrderNumber() {
    byte[] bytes = new byte[16];
    RANDOM.nextBytes(bytes);
    return "SO-" + HexFormat.of().withUpperCase().formatHex(bytes);
  }

  private static String newAccessToken() {
    byte[] bytes = new byte[32];
    RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  static String tokenHash(String token) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256")
                  .digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException error) {
      throw new IllegalStateException("SHA-256 is not available.", error);
    }
  }

  /** Normalizes the storefront language code for later mail rendering. */
  private static String locale(String value) {
    if (value == null) return null;
    String clean = value.strip().toLowerCase(java.util.Locale.ROOT);
    if (!clean.matches("[a-z]{2,8}")) return null;
    return clean;
  }

  private static Instant toInstant(java.sql.Timestamp timestamp) {
    return timestamp == null ? null : timestamp.toInstant();
  }

  /** Contains one requested product line. */
  public record Line(UUID variantId, int quantity) {}

  /** Carries customer checkout input from the storefront. */
  public record CheckoutRequest(
      String customerName,
      String customerEmail,
      String street,
      String city,
      String postalCode,
      String country,
      String locale,
      List<LineInput> lines) {}

  /** Carries one product line selected by a customer. */
  public record LineInput(UUID variantId, int quantity) {}

  /** Contains the confirmed order number and server-calculated total. */
  public record CheckoutResult(
      UUID orderId,
      String orderNumber,
      String accessToken,
      long totalCents,
      int itemCount,
      Instant expiresAt) {}

  /** Contains an admin view of one order without provider tokens. */
  public record OrderDetail(
      Order order, List<OrderItem> items, Payment payment, Shipment shipment) {}

  /** Contains a confirmed order and its money amounts in minor units. */
  public record Order(
      UUID id,
      String orderNumber,
      String status,
      String customerName,
      String customerEmail,
      String street,
      String city,
      String postalCode,
      String currency,
      long subtotalCents,
      long taxCents,
      long shippingCents,
      long totalCents,
      Instant createdAt,
      String locale) {}

  /** Contains one line on a confirmed order. */
  public record OrderItem(
      String productName,
      String variantLabel,
      int quantity,
      long unitPriceCents,
      long lineTotalCents,
      int fulfilledQuantity,
      int refundedQuantity) {}

  /** Contains the payment state for one order. */
  public record Payment(String provider, String externalId, long amountCents, String status) {}

  /** Contains shipment details for a fulfilled order. */
  public record Shipment(String carrier, String trackingNumber, String status, Instant shippedAt) {}

  private static final class ItemRow {
    final UUID id;
    final UUID variantId;
    final int quantity;
    final int fulfilledQuantity;

    private ItemRow(UUID id, UUID variantId, int quantity, int fulfilledQuantity) {
      this.id = id;
      this.variantId = variantId;
      this.quantity = quantity;
      this.fulfilledQuantity = fulfilledQuantity;
    }
  }

  /** Carries one locked product row for checkout calculations. */
  public record VariantState(
      UUID id, String sku, long priceCents, String productName, int availableQuantity) {}

  /** Raised when an order requests more stock than is available. */
  public static class OutOfStockException extends RuntimeException {
    private final String productName;

    OutOfStockException(String productNameValue) {
      super(
          (productNameValue == null || productNameValue.isBlank()
                  ? "This product"
                  : productNameValue)
              + " is out of stock. Select a lower quantity or another product.");
      this.productName = productNameValue == null ? "" : productNameValue;
    }

    /** Returns the affected product name shown to the customer. */
    public String productName() {
      return productName;
    }
  }

  /** Raised when online shop sales are disabled by the administrator. */
  public static class ShopDisabledException extends RuntimeException {}
}
