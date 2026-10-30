package de.openshopfront.platform.commerce;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Serves operational reports and CSV exports for the selected range. */
@RestController
@RequestMapping("/api/v1/admin/reports")
public class ReportController {
  private final ReportService reports;

  public ReportController(ReportService reports) {
    this.reports = reports;
  }

  /** Returns headline figures: orders, revenue, refunds, units sold, bookings. */
  @GetMapping("/summary")
  public Object summary(
      @RequestParam(required = false) String from, @RequestParam(required = false) String to) {
    return reports.summary(from, to);
  }

  /** Returns daily order and revenue rows for trend charts. */
  @GetMapping("/trend")
  public List<ReportService.DailyRow> trend(
      @RequestParam(required = false) String from, @RequestParam(required = false) String to) {
    return reports.dailyTrend(from, to);
  }

  /** Returns the most popular products by units sold. */
  @GetMapping("/top-products")
  public List<ReportService.TopProduct> topProducts(
      @RequestParam(required = false) String from,
      @RequestParam(required = false) String to,
      @RequestParam(defaultValue = "10") int limit) {
    return reports.topProducts(from, to, limit);
  }

  /** Returns low-stock variants for inventory alerts. */
  @GetMapping("/inventory-alerts")
  public List<ReportService.InventoryAlert> inventoryAlerts(
      @RequestParam(defaultValue = "20") int limit) {
    return reports.inventoryAlerts(limit);
  }

  /** Returns order status counts for the sales funnel. */
  @GetMapping("/funnel")
  public List<ReportService.FunnelRow> funnel(
      @RequestParam(required = false) String from, @RequestParam(required = false) String to) {
    return reports.funnel(from, to);
  }

  /** Returns order rows for review and export. */
  @GetMapping("/orders")
  public List<ReportService.OrderExport> orders(
      @RequestParam(required = false) String from,
      @RequestParam(required = false) String to,
      @RequestParam(defaultValue = "100") int limit) {
    return reports.orders(from, to, limit);
  }

  /** Streams the order ledger as CSV. PII is included; restrict access with roles. */
  @GetMapping(value = "/orders.csv", produces = "text/csv")
  public String ordersCsv(
      @RequestParam(required = false) String from,
      @RequestParam(required = false) String to,
      @RequestParam(defaultValue = "500") int limit) {
    List<ReportService.OrderExport> rows = reports.orders(from, to, Math.max(limit, 1));
    StringBuilder csv = new StringBuilder();
    csv.append(
        "order_number,status,created_at,customer_name,email,subtotal_cents,tax_cents,shipping_cents,total_cents,currency\n");
    for (ReportService.OrderExport row : rows) {
      csv.append(row.number())
          .append(',')
          .append(escapeCsv(row.status()))
          .append(',')
          .append(row.createdAt().toString())
          .append(',')
          .append(escapeCsv(row.customerName()))
          .append(',')
          .append(escapeCsv(row.email()))
          .append(',')
          .append(row.subtotalCents())
          .append(',')
          .append(row.taxCents())
          .append(',')
          .append(row.shippingCents())
          .append(',')
          .append(row.totalCents())
          .append(',')
          .append(escapeCsv(row.currency()))
          .append('\n');
    }
    return csv.toString();
  }

  private static String escapeCsv(String value) {
    if (value == null) return "";
    if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
      return '"' + value.replace("\"", "\"\"") + '"';
    }
    return value;
  }
}
