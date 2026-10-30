import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { localeOf, Lang } from '../shared/i18n';
import { AdminI18n } from '../shared/i18n.service';
import { BookingApi, type Booking } from './booking-api.service';

@Component({
  selector: 'ip-bookings',
  standalone: true,
  imports: [MatButtonModule, MatIconModule],
  templateUrl: './booking.component.html',
  styleUrls: ['./booking.component.css'],
})
export class BookingComponent {
  private readonly api = inject(BookingApi);
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
  readonly weekStart = signal(this.startOfWeek(new Date()));
  readonly bookings = signal<Booking[]>([]);
  readonly selected = signal<Booking | null>(null);
  readonly loading = signal(false);
  readonly error = signal('');
  readonly days = computed(() =>
    Array.from({ length: 7 }, (_, index) => {
      const date = this.addDays(this.weekStart(), index);
      return {
        date,
        key: this.key(date),
        weekday: this.format(date, { weekday: 'short' }),
        day: this.format(date, { day: '2-digit' }),
        month: this.format(date, { month: 'short' }),
        today: this.key(date) === this.key(new Date()),
      };
    }),
  );
  readonly confirmedCount = computed(
    () => this.bookings().filter((item) => item.status === 'CONFIRMED').length,
  );
  readonly cancelledCount = computed(
    () => this.bookings().filter((item) => item.status !== 'CONFIRMED').length,
  );
  readonly customerCount = computed(
    () => this.bookings().filter((item) => item.customerName).length,
  );
  readonly syncedCount = computed(
    () => this.bookings().filter((item) => item.calendarSyncStatus === 'SYNCED').length,
  );
  readonly weekNumber = computed(() => {
    const date = new Date(this.weekStart());
    date.setHours(0, 0, 0, 0);
    date.setDate(date.getDate() + 3 - ((date.getDay() + 6) % 7));
    const first = new Date(date.getFullYear(), 0, 4);
    return (
      1 +
      Math.round(
        ((date.getTime() - first.getTime()) / 86400000 - 3 + ((first.getDay() + 6) % 7)) / 7,
      )
    );
  });
  readonly weekLabel = computed(
    () =>
      `${this.format(this.weekStart(), { month: 'short', day: 'numeric' })} – ${this.format(this.addDays(this.weekStart(), 6), { month: 'short', day: 'numeric', year: 'numeric' })}`,
  );

  constructor() {
    effect(() => {
      if (this.enabled()) {
        this.weekStart();
        void this.load();
      }
    });
  }
  async load() {
    if (!this.enabled()) return;
    this.loading.set(true);
    this.error.set('');
    try {
      const from = this.weekStart(),
        to = this.addDays(from, 7);
      this.bookings.set(await this.api.calendar(from.toISOString(), to.toISOString()));
      const selected = this.selected();
      if (selected)
        this.selected.set(this.bookings().find((item) => item.id === selected.id) || null);
    } catch {
      this.error.set(this.t('The app could not load the booking calendar.'));
    } finally {
      this.loading.set(false);
    }
  }
  moveWeek(amount: number) {
    this.selected.set(null);
    this.weekStart.set(this.addDays(this.weekStart(), amount * 7));
  }
  goToday() {
    this.selected.set(null);
    this.weekStart.set(this.startOfWeek(new Date()));
  }
  jumpTo(value: string) {
    if (!value) return;
    this.selected.set(null);
    this.weekStart.set(this.startOfWeek(new Date(`${value}T12:00:00`)));
  }
  dateValue() {
    const date = this.weekStart(),
      offset = date.getTimezoneOffset() * 60000;
    return new Date(date.getTime() - offset).toISOString().slice(0, 10);
  }
  eventsFor(day: Date) {
    const key = this.key(day);
    return this.bookings().filter((item) => this.key(new Date(item.startsAt)) === key);
  }
  time(value: string) {
    return new Intl.DateTimeFormat(localeOf(this.i18n.lang()), {
      hour: '2-digit',
      minute: '2-digit',
    }).format(new Date(value));
  }
  fullDate(value: string) {
    return new Intl.DateTimeFormat(localeOf(this.i18n.lang()), {
      weekday: 'long',
      year: 'numeric',
      month: 'long',
      day: 'numeric',
    }).format(new Date(value));
  }
  initials(item: Booking) {
    return (item.customerName || '?')
      .split(/\s+/)
      .slice(0, 2)
      .map((part) => part.charAt(0))
      .join('')
      .toUpperCase();
  }
  syncLabel(item: Booking) {
    return (
      (
        {
          SYNCED: this.t('Synchronized'),
          PENDING: this.t('Delivery queued'),
          RETRY: this.t('Delivery retry scheduled'),
          FAILED: this.t('Delivery failed'),
          NOT_CONFIGURED: this.t('No calendar mapping'),
        } as Record<string, string>
      )[item.calendarSyncStatus] || item.calendarSyncStatus
    );
  }
  calendarLabel(item: Booking) {
    return item.calendarProvider === 'google'
      ? this.t('Google Calendar')
      : this.t('External calendar');
  }
  async cancel(item: Booking) {
    const name = item.customerName || this.t('this customer');
    if (!confirm(this.t('Cancel the booking for {name}?', { name }))) return;
    this.loading.set(true);
    try {
      await this.api.cancel(item.id);
      await this.load();
    } catch {
      this.error.set(this.t('The app could not cancel the booking.'));
    } finally {
      this.loading.set(false);
    }
  }
  private startOfWeek(value: Date) {
    const date = new Date(value);
    date.setHours(0, 0, 0, 0);
    date.setDate(date.getDate() - ((date.getDay() + 6) % 7));
    return date;
  }
  private addDays(value: Date, amount: number) {
    const date = new Date(value);
    date.setDate(date.getDate() + amount);
    return date;
  }
  private key(value: Date) {
    return `${value.getFullYear()}-${value.getMonth()}-${value.getDate()}`;
  }
  private format(value: Date, options: Intl.DateTimeFormatOptions) {
    return new Intl.DateTimeFormat(localeOf(this.i18n.lang()), options).format(value);
  }
}
