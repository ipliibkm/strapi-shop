import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Component, inject, input, output, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { OpenShopfrontContentBlocksComponent } from '@openshopfront/ui';
import { firstValueFrom } from 'rxjs';
import { StorefrontI18n } from '../shared/i18n.service';
import { StorefrontFormComponent } from '../shared/storefront-form.component';
import {
  BookingConfirmation,
  BookingOffering,
  BookingSettings,
  Page,
  Product,
  PublicAvailability,
  PublishedPage,
} from '../shared/types';

/** The customer booking page: published CMS intro (or static hero) plus the booking flow. */
@Component({
  selector: 'ip-booking',
  standalone: true,
  imports: [
    MatButtonModule,
    MatIconModule,
    OpenShopfrontContentBlocksComponent,
    StorefrontFormComponent,
  ],
  templateUrl: './booking.component.html',
})
export class BookingComponent {
  private http = inject(HttpClient);
  readonly i18n = inject(StorefrontI18n);

  /** The published booking content, or null when not published (shows the static hero). */
  readonly content = input<PublishedPage | null>(null);
  /** Products for "products" blocks (already limited by the shell). */
  readonly products = input<Product[]>([]);

  /** A CTA block target was clicked; the shell routes to it. */
  readonly navigate = output<Page>();
  /** A product card was clicked; the shell opens the product page. */
  readonly open = output<Product>();
  readonly selectedDate = signal(this.dateValue(new Date(Date.now() + 86400000)));
  readonly selectedTime = signal('10:00');
  readonly bookingServices = signal<BookingOffering[]>([]);
  readonly selectedServiceId = signal('');
  readonly bookingSettings = signal<BookingSettings>({
    customerBookingEnabled: false,
    defaultDurationMinutes: 60,
  });
  readonly bookingSettingsLoaded = signal(false);
  readonly available = signal(false);
  readonly checking = signal(false);
  readonly submitting = signal(false);
  readonly availabilityError = signal(false);
  readonly availabilityMessage = signal('');
  readonly confirmation = signal<BookingConfirmation | null>(null);
  readonly minimumDate = this.dateValue(new Date());
  readonly timeZone = Intl.DateTimeFormat().resolvedOptions().timeZone;
  readonly timeSlots = ['09:00', '10:00', '11:00', '13:00', '14:00', '15:00', '16:00'];

  constructor() {
    void this.loadBookingServices();
  }

  langVersion(): string {
    return this.i18n.lang();
  }
  t(key: string, params?: Record<string, string | number>): string {
    return this.i18n.t(key, params);
  }

  /** Maps the app's products to the renderer's shape with a formatted price. */
  blockProducts() {
    return this.products().map((item) => ({
      id: item.id,
      name: item.name,
      priceLabel: this.minPrice(item),
      href: `/products/${encodeURIComponent(item.slug)}`,
    }));
  }

  blockLabels() {
    return { continue: this.t('block.continue'), productsNone: this.t('products.none') };
  }

  minPrice(product: Product): string {
    if (!product.variantCount) return this.t('price.ask');
    return this.t('price.fromShop');
  }

  onNavigate(target?: string) {
    const allowed: Page[] = ['booking', 'shop', 'services', 'about', 'contact', 'home'];
    if (target && (allowed as string[]).includes(target)) this.navigate.emit(target as Page);
  }

  selectedOffering() {
    return this.bookingServices().find((item) => item.id === this.selectedServiceId());
  }
  bookingDuration() {
    return (
      this.selectedOffering()?.durationMinutes ?? this.bookingSettings().defaultDurationMinutes
    );
  }
  bookingName() {
    return this.selectedOffering()?.name ?? this.t('booking.standardAppointment');
  }

  async loadBookingServices() {
    try {
      const [services, settings] = await Promise.all([
        firstValueFrom(this.http.get<BookingOffering[]>('/api/v1/public/services')),
        firstValueFrom(this.http.get<BookingSettings>('/api/v1/public/booking-settings')),
      ]);
      this.bookingServices.set(services);
      this.bookingSettings.set(settings);
      this.bookingSettingsLoaded.set(true);
      if (!settings.customerBookingEnabled) {
        this.available.set(false);
        this.availabilityMessage.set('');
        return;
      }
      if (services.length && !this.selectedServiceId()) this.selectedServiceId.set(services[0].id);
      await this.checkAvailability();
    } catch {
      this.availabilityError.set(true);
      this.availabilityMessage.set(this.t('booking.error.unavailable'));
    }
  }

  chooseService(item: BookingOffering) {
    this.selectedServiceId.set(item.id);
    this.confirmation.set(null);
    void this.checkAvailability();
  }
  changeDate(value: string) {
    this.selectedDate.set(value);
    this.confirmation.set(null);
    void this.checkAvailability();
  }
  chooseTime(value: string) {
    this.selectedTime.set(value);
    void this.checkAvailability();
  }

  async checkAvailability() {
    this.checking.set(true);
    this.available.set(false);
    this.availabilityError.set(false);
    this.availabilityMessage.set(this.t('booking.checking'));
    if (!this.bookingSettings().customerBookingEnabled) {
      this.checking.set(false);
      this.availabilityMessage.set('');
      return;
    }
    if (this.bookingServices().length && !this.selectedServiceId()) {
      this.checking.set(false);
      this.availabilityMessage.set(this.t('booking.selectServiceFirst'));
      return;
    }
    const params: Record<string, string> = { startsAt: this.start().toISOString() };
    if (this.selectedServiceId()) params['serviceId'] = this.selectedServiceId();
    try {
      const result = await firstValueFrom(
        this.http.get<PublicAvailability>('/api/v1/public/availability', { params }),
      );
      this.available.set(result.available);
      this.availabilityMessage.set(
        result.available ? this.t('booking.available') : this.t('booking.notAvailableTime'),
      );
    } catch {
      this.availabilityError.set(true);
      this.availabilityMessage.set(this.t('booking.availabilityFailed'));
    } finally {
      this.checking.set(false);
    }
  }

  async createBooking(customerName: string, customerEmail: string) {
    if (!customerName.trim() || !customerEmail.trim()) {
      this.availabilityError.set(true);
      this.availabilityMessage.set(this.t('error.nameEmail'));
      return;
    }
    this.submitting.set(true);
    this.availabilityError.set(false);
    const request: {
      serviceId?: string;
      startsAt: string;
      customerName: string;
      customerEmail: string;
    } = {
      startsAt: this.start().toISOString(),
      customerName: customerName.trim(),
      customerEmail: customerEmail.trim(),
    };
    if (this.selectedServiceId()) request.serviceId = this.selectedServiceId();
    try {
      const result = await firstValueFrom(
        this.http.post<BookingConfirmation>('/api/v1/public/bookings', request),
      );
      this.confirmation.set(result);
    } catch (error) {
      const response = error as HttpErrorResponse;
      this.available.set(false);
      this.availabilityError.set(true);
      this.availabilityMessage.set(
        response.status === 409 ? this.t('booking.conflict') : this.t('booking.confirmFailed'),
      );
    } finally {
      this.submitting.set(false);
    }
  }

  resetBooking() {
    this.confirmation.set(null);
    void this.checkAvailability();
  }

  confirmationDate(result: BookingConfirmation) {
    return new Intl.DateTimeFormat(this.i18n.locale(), {
      weekday: 'long',
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    }).format(new Date(result.startsAt));
  }

  private start() {
    return new Date(`${this.selectedDate()}T${this.selectedTime()}:00`);
  }
  private dateValue(value: Date) {
    const local = new Date(value.getTime() - value.getTimezoneOffset() * 60000);
    return local.toISOString().slice(0, 10);
  }
}
