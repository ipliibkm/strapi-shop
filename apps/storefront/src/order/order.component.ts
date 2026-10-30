import { HttpClient } from '@angular/common/http';
import { Component, OnInit, inject, input, output, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { firstValueFrom } from 'rxjs';
import { StorefrontI18n } from '../shared/i18n.service';
import { OrderSummary } from '../shared/types';

/** The order-status page: looks up a placed order by its number. */
@Component({
  selector: 'ip-order',
  standalone: true,
  imports: [MatButtonModule],
  templateUrl: './order.component.html',
})
export class OrderComponent implements OnInit {
  private http = inject(HttpClient);
  readonly i18n = inject(StorefrontI18n);

  /** The order number to look up, set by the shell. */
  readonly orderNumber = input.required<string>();
  /** The private customer token to use for this order lookup. */
  readonly accessToken = input.required<string>();
  readonly summary = signal<OrderSummary | null>(null);
  readonly loading = signal(false);
  /** Requests the shell to return to the shop catalog. */
  readonly backToShop = output<void>();

  ngOnInit(): void {
    // Inputs are only guaranteed to be bound from ngOnInit onwards, so the load starts here.
    void this.load();
  }

  langVersion(): string {
    return this.i18n.lang();
  }
  t(key: string, params?: Record<string, string | number>): string {
    return this.i18n.t(key, params);
  }
  money(cents: number, currency = 'EUR') {
    try {
      return (cents / 100).toLocaleString(this.i18n.locale(), { style: 'currency', currency });
    } catch {
      return `${(cents / 100).toFixed(2)} ${currency}`;
    }
  }

  shortStatus(status?: string | null) {
    if (!status) return '—';
    const map: Record<string, string> = {
      PENDING_PAYMENT: this.t('status.pending'),
      PAID: this.t('status.paid'),
      FULFILLED: this.t('status.fulfilled'),
      CANCELLED: this.t('status.cancelled'),
    };
    return map[status] ?? status;
  }

  statusClass(status?: string | null) {
    if (!status) return '';
    const good = ['PAID', 'FULFILLED'];
    return good.includes(status) ? 'ok' : '';
  }

  formatDate(value: string) {
    try {
      return new Intl.DateTimeFormat(this.i18n.locale(), {
        dateStyle: 'medium',
        timeStyle: 'short',
      }).format(new Date(value));
    } catch {
      return value;
    }
  }

  private async load() {
    this.loading.set(true);
    try {
      const data = await firstValueFrom(
        this.http.get<OrderSummary>(
          `/api/v1/public/orders/${encodeURIComponent(this.orderNumber())}`,
          { headers: { 'X-Order-Access-Token': this.accessToken() } },
        ),
      );
      this.summary.set(data);
    } catch {
      // The order may not exist; show the not-found state.
    } finally {
      this.loading.set(false);
    }
  }
}
