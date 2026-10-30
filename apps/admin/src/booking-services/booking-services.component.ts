import { HttpClient } from '@angular/common/http';
import { Lang } from '../shared/i18n';
import { Component, effect, inject, input, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { firstValueFrom, type Observable } from 'rxjs';
import { AdminI18n } from '../shared/i18n.service';

interface BookingOffering {
  id: string;
  name: string;
  durationMinutes: number;
  active: boolean;
}
interface BookingSettings {
  customerBookingEnabled: boolean;
  defaultDurationMinutes: number;
}

@Component({
  selector: 'ip-booking-services',
  standalone: true,
  imports: [MatButtonModule, MatIconModule],
  templateUrl: './booking-services.component.html',
  styleUrls: ['./booking-services.component.css'],
})
export class BookingServicesComponent {
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
  readonly enabled = input(false);
  readonly offerings = signal<BookingOffering[]>([]);
  readonly settings = signal<BookingSettings>({
    customerBookingEnabled: true,
    defaultDurationMinutes: 60,
  });
  readonly busy = signal(false);
  readonly notice = signal('');
  readonly failed = signal(false);
  constructor() {
    effect(() => {
      if (this.enabled()) void this.load();
    });
  }
  activeCount() {
    return this.offerings().filter((item) => item.active).length;
  }
  async load() {
    try {
      const [offerings, settings] = await Promise.all([
        firstValueFrom(this.http.get<BookingOffering[]>('/api/v1/admin/booking-services')),
        firstValueFrom(this.http.get<BookingSettings>('/api/v1/admin/booking-services/settings')),
      ]);
      this.offerings.set(offerings);
      this.settings.set(settings);
    } catch {
      this.failed.set(true);
      this.notice.set(this.t('The app could not load booking settings.'));
    }
  }
  async saveSettings(customerBookingEnabled: boolean, duration: string) {
    await this.write(
      () =>
        this.http.put('/api/v1/admin/booking-services/settings', {
          customerBookingEnabled,
          defaultDurationMinutes: Number(duration),
        }),
      'Booking settings saved.',
    );
  }
  async create(name: string, duration: string) {
    await this.write(
      () =>
        this.http.post('/api/v1/admin/booking-services', {
          name,
          durationMinutes: Number(duration),
          active: true,
        }),
      'Service added.',
    );
  }
  async update(item: BookingOffering, name: string, duration: string) {
    await this.write(
      () =>
        this.http.put(`/api/v1/admin/booking-services/${item.id}`, {
          name,
          durationMinutes: Number(duration),
          active: item.active,
        }),
      'Service updated.',
    );
  }
  async activate(item: BookingOffering, name: string, duration: string) {
    await this.write(
      () =>
        this.http.put(`/api/v1/admin/booking-services/${item.id}`, {
          name,
          durationMinutes: Number(duration),
          active: true,
        }),
      'Service activated.',
    );
  }
  async deactivate(item: BookingOffering) {
    await this.write(
      () => this.http.delete(`/api/v1/admin/booking-services/${item.id}`),
      'Service hidden from customers.',
    );
  }
  private async write(request: () => Observable<unknown>, success: string) {
    this.busy.set(true);
    this.notice.set('');
    this.failed.set(false);
    try {
      await firstValueFrom(request());
      await this.load();
      this.notice.set(this.t(success));
    } catch {
      this.failed.set(true);
      this.notice.set(this.t('The service could not be saved. Check its name and duration.'));
    } finally {
      this.busy.set(false);
    }
  }
}
