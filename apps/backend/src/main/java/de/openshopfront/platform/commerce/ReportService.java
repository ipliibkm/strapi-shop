package de.openshopfront.platform.commerce;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reads order, payment, inventory and booking data for operational reporting. */
@Service
public class ReportService {
  private final NamedParameterJdbcTemplate db;

  public ReportService(NamedParameterJdbcTemplate db) {
    this.db = db;
  }

  /** Returns headline figures for the selected range. All money values are minor units. */
  @Transactional(readOnly = true)
  public Summary summary(String from, String to) {
    Map<String, Object> params = range(from, to);
    Long paidOrders =
        scalar(
            "select count(*) from shop_order where status in ('PAID','FULFILLED') and created_at >= :from and created_at < :to",
            params);
    Long cancelledOrders =
        scalar(
            "select count(*) from shop_order where status='CANCELLED' and created_at >= :from and created_at < :to",
            params);
    Long revenueCents =
        scalar(
            """
            select coalesce(sum(o.total_cents),0) from shop_order o
            join payment p on p.order_id=o.id
            where p.status in ('SUCCEEDED','PARTIALLY_REFUNDED')
              and o.created_at >= :from and o.created_at < :to""",
            params);
    Long refundedCents =
        scalar(
            """
            select coalesce(sum(o.total_cents),0) from shop_order o
            join payment p on p.order_id=o.id
            where p.status='REFUNDED' and o.created_at >= :from and o.created_at < :to""",
            params);
    Long unitsSold =
        scalar(
            """
            select coalesce(sum(i.quantity),0) from shop_order_item i
            join shop_order o on o.id=i.order_id
            where o.status in ('PAID','FULFILLED') and o.created_at >= :from and o.created_at < :to""",
            params);
    Long bookingCount =
        scalar(
            "select count(*) from booking b where b.starts_at >= :from and b.starts_at < :to and coalesce(b.status,'CONFIRMED') <> 'CANCELLED'",
            params);

    return new Summary(
        paidOrders == null ? 0L : paidOrders,
        cancelledOrders == null ? 0L : cancelledOrders,
        revenueCents == null ? 0L : revenueCents,
        refundedCents == null ? 0L : refundedCents,
        unitsSold == null ? 0L : unitsSold,
        bookingCount == null ? 0L : bookingCount);
  }

  /** Returns daily order and revenue rows for trend charts. */
  @Transactional(readOnly = true)
  public List<DailyRow> dailyTrend(String from, String to) {
    java.time.LocalDate start = dayStart(from);
    java.time.LocalDate endInclusive = dayEnd(to);
    Map<String, Object> params = new java.util.LinkedHashMap<>();
    params.put("from", java.sql.Date.valueOf(start));
    params.put("to", java.sql.Date.valueOf(endInclusive));
    return db.query(
        """
        select d.day as day,
          coalesce((select count(*) from shop_order o where date(o.created_at at time zone 'UTC')=d.day),0)::bigint as orders,
          coalesce((select sum(o.total_cents) from shop_order o join payment p on p.order_id=o.id
                     where p.status in ('SUCCEEDED','PARTIALLY_REFUNDED') and date(o.created_at at time zone 'UTC')=d.day),0)::bigint as revenue
        from generate_series(:from, :to, '1 day'::interval) d(day)
        order by d.day""",
        params,
        (rs, row) ->
            new DailyRow(
                rs.getDate("day").toLocalDate(), rs.getLong("orders"), rs.getLong("revenue")));
  }

  /** Returns the most popular purchasable options in the selected range. */
  @Transactional(readOnly = true)
  public List<TopProduct> topProducts(String from, String to, int limit) {
    Map<String, Object> params = range(from, to);
    int size = Math.max(1, Math.min(limit, 25));
    return db.query(
        """
        select i.product_name as name, coalesce(sum(i.quantity),0)::bigint as units,
          coalesce(sum(i.line_total_cents),0)::bigint as revenue
        from shop_order_item i join shop_order o on o.id=i.order_id
        where o.status in ('PAID','FULFILLED') and o.created_at >= :from and o.created_at < :to
        group by i.product_name order by units desc, name asc limit :size""",
        paramsWithLimit(params, size),
        (rs, row) ->
            new TopProduct(rs.getString("name"), rs.getLong("units"), rs.getLong("revenue")));
  }

  /** Returns variants with the lowest available stock first for inventory alerts. */
  @Transactional(readOnly = true)
  public List<InventoryAlert> inventoryAlerts(int limit) {
    int size = Math.max(1, Math.min(limit, 50));
    return db.query(
        """
        select v.sku as sku, coalesce(v.label,'') as label, p.name as product,
          (v.stock_quantity - v.reserved_quantity)::bigint as available
        from product_variant v join product p on p.id=v.product_id
        where v.active=true and p.active=true
        order by available asc, v.sku asc limit :size""",
        Map.of("size", size),
        (rs, row) ->
            new InventoryAlert(
                rs.getString("sku"),
                rs.getString("label"),
                rs.getString("product"),
                rs.getLong("available")));
  }

  /** Returns order status counts for the funnel. */
  @Transactional(readOnly = true)
  public List<FunnelRow> funnel(String from, String to) {
    Map<String, Object> params = range(from, to);
    return db.query(
        "select status as status, count(*)::bigint as orders from shop_order where created_at >= :from and created_at < :to group by status order by status",
        params,
        (rs, row) -> new FunnelRow(rs.getString("status"), rs.getLong("orders")));
  }

  /** Returns one ledger-reconcilable order for CSV export. */
  @Transactional(readOnly = true)
  public List<OrderExport> orders(String from, String to, int limit) {
    Map<String, Object> params = range(from, to);
    int size = Math.max(1, Math.min(limit, 500));
    return db.query(
        """
        select o.order_number as number, o.status as status, o.created_at as createdAt,
          coalesce(o.customer_name,'') as customer, coalesce(o.customer_email,'') as email,
          o.subtotal_cents::bigint as subtotal, o.tax_cents::bigint as tax,
          o.shipping_cents::bigint as shipping, o.total_cents::bigint as total, o.currency as currency
        from shop_order o where o.created_at >= :from and o.created_at < :to
        order by o.created_at desc limit :size""",
        paramsWithLimit(params, size),
        (rs, row) ->
            new OrderExport(
                rs.getString("number"),
                rs.getString("status"),
                rs.getTimestamp("createdAt").toInstant(),
                rs.getString("customer"),
                rs.getString("email"),
                rs.getLong("subtotal"),
                rs.getLong("tax"),
                rs.getLong("shipping"),
                rs.getLong("total"),
                rs.getString("currency")));
  }

  private Map<String, Object> range(String from, String to) {
    return Map.of("from", parse(from), "to", exclusiveEnd(to));
  }

  /** Resolves a `from` boundary (Instant or ISO day) to the UTC start date. */
  private java.time.LocalDate dayStart(String value) {
    if (value == null || value.isBlank())
      return Instant.EPOCH.atZone(java.time.ZoneOffset.UTC).toLocalDate();
    try {
      return Instant.parse(value.trim()).atZone(java.time.ZoneOffset.UTC).toLocalDate();
    } catch (RuntimeException error) {
      return java.time.LocalDate.parse(value.trim());
    }
  }

  /** Resolves a `to` boundary to the inclusive UTC end date. */
  private java.time.LocalDate dayEnd(String value) {
    if (value == null || value.isBlank())
      return Instant.now().atZone(java.time.ZoneOffset.UTC).toLocalDate();
    try {
      return Instant.parse(value.trim()).atZone(java.time.ZoneOffset.UTC).toLocalDate();
    } catch (RuntimeException error) {
      return java.time.LocalDate.parse(value.trim());
    }
  }

  private java.sql.Timestamp parse(String value) {
    if (value == null || value.isBlank()) return java.sql.Timestamp.from(Instant.EPOCH);
    try {
      return java.sql.Timestamp.from(Instant.parse(value.trim()));
    } catch (RuntimeException error) {
      Instant start =
          java.time.LocalDate.parse(value.trim())
              .atStartOfDay(java.time.ZoneOffset.UTC)
              .toInstant();
      return java.sql.Timestamp.from(start);
    }
  }

  /** Converts an inclusive day boundary to an exclusive instant at UTC midnight. */
  private java.sql.Timestamp exclusiveEnd(String value) {
    if (value == null || value.isBlank())
      return java.sql.Timestamp.from(Instant.now().plusSeconds(1));
    try {
      return java.sql.Timestamp.from(Instant.parse(value.trim()));
    } catch (RuntimeException error) {
      Instant end =
          java.time.LocalDate.parse(value.trim())
              .plusDays(1)
              .atStartOfDay(java.time.ZoneOffset.UTC)
              .toInstant();
      return java.sql.Timestamp.from(end);
    }
  }

  private Long scalar(String sql, Map<String, Object> params) {
    List<Long> values = db.query(sql, params, (rs, row) -> rs.getLong(1));
    return values.isEmpty() ? null : values.get(0);
  }

  /** Combines named range parameters with a positional limit for paged exports. */
  private Map<String, Object> paramsWithLimit(Map<String, Object> base, int size) {
    java.util.LinkedHashMap<String, Object> merged = new java.util.LinkedHashMap<>(base);
    merged.put("size", size);
    return merged;
  }

  /** Carries headline report figures for one range. */
  public record Summary(
      long paidOrders,
      long cancelledOrders,
      long revenueCents,
      long refundedCents,
      long unitsSold,
      long bookings) {}

  /** One calendar day of trend data. */
  public record DailyRow(java.time.LocalDate day, long orders, long revenueCents) {}

  /** A purchasable option ranked by sold quantity. */
  public record TopProduct(String name, long units, long revenueCents) {}

  /** A low-stock variant for the inventory alert list. */
  public record InventoryAlert(String sku, String label, String product, long available) {}

  /** One order status bucket in the funnel. */
  public record FunnelRow(String status, long orders) {}

  /** One order row ready for CSV export. PII fields are masked by the caller where required. */
  public record OrderExport(
      String number,
      String status,
      Instant createdAt,
      String customerName,
      String email,
      long subtotalCents,
      long taxCents,
      long shippingCents,
      long totalCents,
      String currency) {}
}
