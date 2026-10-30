import { HttpClient } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { localeOf, Lang } from '../shared/i18n';
import { AdminI18n } from '../shared/i18n.service';

interface Summary {
  paidOrders: number;
  cancelledOrders: number;
  revenueCents: number;
  refundedCents: number;
  unitsSold: number;
  bookings: number;
}
interface DailyRow {
  day: string;
  orders: number;
  revenueCents: number;
}
interface TopProduct {
  name: string;
  units: number;
  revenueCents: number;
}
interface InventoryAlert {
  sku: string;
  label: string;
  product: string;
  available: number;
}
interface FunnelRow {
  status: string;
  orders: number;
}

/** Operational dashboards and CSV exports for the selected date range. */
@Component({
  selector: 'ip-reports',
  standalone: true,
  imports: [MatButtonModule, MatIconModule],
  templateUrl: './reports.component.html',
  styleUrls: ['./reports.component.css'],
})
export class ReportsComponent {
  private readonly http = inject(HttpClient);
  readonly i18n = inject(AdminI18n);
  /** Reads the active language so templates track it and re-render on change. */
  langVersion(): Lang {
    return this.i18n.lang();
  }
  /** Translates one interface string for the active admin language. */
  t(text: string, params?: Record<string, string | number>): string {
    return this.i18n.t(text, params);
  }

  readonly busy = signal(false);
  readonly error = signal('');
  readonly notice = signal('');
  readonly summary = signal<Summary | null>(null);
  readonly trend = signal<DailyRow[]>([]);
  readonly top = signal<TopProduct[]>([]);
  readonly alerts = signal<InventoryAlert[]>([]);
  readonly funnel = signal<FunnelRow[]>([]);

  private fromValue: string;
  private toValue: string;
  /** Re-evaluated on every access so labels follow the active language. */
  get presets() {
    return [
      { days: 7, label: this.t('Last 7 days') },
      { days: 30, label: this.t('Last 30 days') },
      { days: 90, label: this.t('Last 90 days') },
    ];
  }

  constructor() {
    const today = this.isoDay(new Date());
    this.toValue = today;
    this.fromValue = this.isoDay(this.addDays(new Date(), -29));
  }

  usePreset(days: number) {
    this.fromValue = this.isoDay(this.addDays(new Date(), -(days - 1)));
    this.toValue = this.isoDay(new Date());
    void this.refresh();
  }

  applyRange() {
    void this.refresh();
  }

  async refresh() {
    if (this.busy()) return;
    this.busy.set(true);
    this.error.set('');
    this.notice.set('');
    try {
      const from = `${this.fromValue}T00:00:00Z`;
      const to = `${this.toValue}T23:59:59.999Z`;
      const [summary, trend, top, alerts, funnel] = await Promise.all([
        firstValueFrom(this.http.get<Summary>('/api/v1/admin/reports/summary', this.q(from, to))),
        firstValueFrom(this.http.get<DailyRow[]>('/api/v1/admin/reports/trend', this.q(from, to))),
        firstValueFrom(
          this.http.get<TopProduct[]>('/api/v1/admin/reports/top-products', this.q(from, to)),
        ),
        firstValueFrom(this.http.get<InventoryAlert[]>('/api/v1/admin/reports/inventory-alerts')),
        firstValueFrom(
          this.http.get<FunnelRow[]>('/api/v1/admin/reports/funnel', this.q(from, to)),
        ),
      ]);
      this.summary.set(summary);
      this.trend.set(trend ?? []);
      this.top.set(top ?? []);
      this.alerts.set(alerts ?? []);
      this.funnel.set(funnel ?? []);
      this.notice.set(this.t('Reports refreshed.'));
    } catch (err) {
      this.error.set(this.describe(err));
    } finally {
      this.busy.set(false);
    }
  }

  async downloadCsv(kind: 'orders' | 'trend' | 'inventory') {
    try {
      if (kind === 'orders') {
        const from = `${this.fromValue}T00:00:00Z`;
        const to = `${this.toValue}T23:59:59.999Z`;
        await this.download(
          `/api/v1/admin/reports/orders.csv?from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}`,
          `orders-${this.fromValue}-to-${this.toValue}.csv`,
        );
        return;
      }
      if (kind === 'trend') {
        const rows: string[] = ['date,orders,revenue_cents'];
        for (const row of this.trend()) rows.push(`${row.day},${row.orders},${row.revenueCents}`);
        this.saveCsv(rows.join('\n'), `trend-${this.fromValue}-to-${this.toValue}.csv`);
        return;
      }
      const rows: string[] = ['sku,label,product,available'];
      for (const row of this.alerts()) {
        rows.push(`${row.sku},"${row.label}","${row.product}",${row.available}`);
      }
      this.saveCsv(rows.join('\n'), `inventory-alerts-${this.isoDay(new Date())}.csv`);
    } catch (err) {
      this.error.set(this.describe(err));
    }
  }

  statusLabel(status: string): string {
    const map: Record<string, string> = {
      PENDING_PAYMENT: this.t('Pending payment'),
      PAID: this.t('Paid'),
      FULFILLED: this.t('Fulfilled'),
      CANCELLED: this.t('Cancelled'),
    };
    return map[status] ?? status;
  }

  funnelClass(status: string): string {
    if (status === 'CANCELLED') return 'funnel-cancelled';
    if (status === 'PENDING_PAYMENT') return 'funnel-pending';
    return '';
  }

  private money(cents: number): string {
    const value = (cents ?? 0) / 100;
    try {
      return new Intl.NumberFormat(localeOf(this.i18n.lang()), {
        style: 'currency',
        currency: 'EUR',
      }).format(value);
    } catch {
      return `${value.toFixed(2)} EUR`;
    }
  }

  private q(from: string, to: string) {
    return { params: { from, to } };
  }

  private describe(err: unknown): string {
    const anyErr = err as { error?: { message?: string; code?: string }; status?: number };
    if (anyErr?.error?.message) return this.t(anyErr.error.message);
    if (anyErr?.status === 401 || anyErr?.status === 403) {
      return this.t('Your administrator session expired. Sign in again to view reports.');
    }
    return this.t('The report request failed. Check the backend logs for details.');
  }

  private async download(url: string, filename: string) {
    const buffer = (await firstValueFrom(
      this.http.get(url, { responseType: 'arraybuffer' }),
    )) as ArrayBuffer;
    if (buffer) {
      const text = new TextDecoder('utf-8').decode(new Uint8Array(buffer));
      this.saveCsv(text, filename);
    }
  }

  private saveCsv(content: string, filename: string) {
    const blob = new Blob([content], { type: 'text/csv;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = filename;
    document.body.appendChild(anchor);
    anchor.click();
    anchor.remove();
    URL.revokeObjectURL(url);
  }

  private isoDay(date: Date): string {
    return date.toISOString().slice(0, 10);
  }

  private addDays(date: Date, days: number): Date {
    const next = new Date(date);
    next.setDate(next.getDate() + days);
    return next;
  }
}
