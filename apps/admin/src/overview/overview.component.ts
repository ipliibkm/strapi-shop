import { HttpClient } from '@angular/common/http';
import { Component, inject, output, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { firstValueFrom } from 'rxjs';
import { OpenShopfrontMetricCardComponent } from '@openshopfront/ui';
import { AdminI18n } from '../shared/i18n.service';
import { OverviewStats, Section } from '../shared/types';

/** The business-at-a-glance landing view with headline figures and quick access. */
@Component({
  selector: 'ip-overview',
  standalone: true,
  imports: [MatButtonModule, MatIconModule, OpenShopfrontMetricCardComponent],
  templateUrl: './overview.component.html',
})
export class OverviewComponent {
  private http = inject(HttpClient);
  readonly i18n = inject(AdminI18n);
  /** Business figures for this view, or null while loading. */
  readonly overview = signal<OverviewStats | null>(null);
  readonly overviewBusy = signal(false);
  readonly overviewNotice = signal('');
  /** Requests the shell to open another section. */
  readonly navigate = output<Section>();

  constructor() {
    void this.refreshOverview();
  }

  langVersion(): string {
    return this.i18n.lang();
  }
  t(text: string, params?: Record<string, string | number>): string {
    return this.i18n.t(text, params);
  }
  /** Renders a headline figure or a dash while it is still loading. */
  metricValue(value?: number): string {
    return value == null ? '–' : String(value);
  }
  /** Formats cents as a EUR amount for display. */
  money(cents?: number): string {
    const value = (cents ?? 0) / 100;
    try {
      return new Intl.NumberFormat('de-DE', { style: 'currency', currency: 'EUR' }).format(value);
    } catch {
      return `${value.toFixed(2)} EUR`;
    }
  }
  /** Loads the headline business figures shown on this view. */
  async refreshOverview() {
    if (this.overviewBusy()) return;
    this.overviewBusy.set(true);
    this.overviewNotice.set('');
    try {
      const [summary, formList] = await Promise.all([
        firstValueFrom(
          this.http.get<{
            paidOrders: number;
            cancelledOrders: number;
            revenueCents: number;
            refundedCents: number;
            unitsSold: number;
            bookings: number;
          }>('/api/v1/admin/reports/summary'),
        ),
        firstValueFrom(this.http.get<{ slug: string }[]>('/api/v1/admin/forms')),
      ]);
      const totals = await Promise.all(
        formList.map((form) =>
          firstValueFrom(
            this.http.get<{ total: number }>(
              `/api/v1/admin/forms/${encodeURIComponent(form.slug)}/submissions?page=0&size=1`,
            ),
          )
            .then((page) => page.total ?? 0)
            .catch(() => 0),
        ),
      );
      this.overview.set({
        bookings: summary.bookings,
        submissions: totals.reduce((sum, value) => sum + value, 0),
        paidOrders: summary.paidOrders,
        cancelledOrders: summary.cancelledOrders,
        revenueCents: summary.revenueCents,
        refundedCents: summary.refundedCents,
        unitsSold: summary.unitsSold,
      });
    } catch {
      this.overviewNotice.set(this.t('The business figures could not be loaded.'));
    } finally {
      this.overviewBusy.set(false);
    }
  }
}
