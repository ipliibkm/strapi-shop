import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Lang } from '../shared/i18n';
import { Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { firstValueFrom } from 'rxjs';
import { AdminI18n } from '../shared/i18n.service';

interface BusinessProfile {
  name: string;
  slug: string;
  contactEmail: string;
  timezone: string;
  localeCode: string;
  currency: string;
  address?: string;
  phone?: string;
}

@Component({
  selector: 'ip-business-profile',
  standalone: true,
  imports: [MatButtonModule, MatIconModule],
  templateUrl: './business-profile.component.html',
  styleUrls: ['./business-profile.component.css'],
})
export class BusinessProfileComponent {
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
  readonly profile = signal<BusinessProfile | null>(null);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly forbidden = signal(false);
  readonly notice = signal('');
  readonly failed = signal(false);
  constructor() {
    void this.load();
  }
  private async load() {
    try {
      const response = await firstValueFrom(this.http.get<any>('/api/v1/content/business-profile'));
      this.profile.set((response?.data || response) as BusinessProfile);
    } catch (error) {
      this.forbidden.set((error as HttpErrorResponse).status === 403);
      this.notice.set(this.t('The app could not load this content.'));
      this.failed.set(true);
    } finally {
      this.loading.set(false);
    }
  }
  async save(
    event: Event,
    current: BusinessProfile,
    name: string,
    slug: string,
    contactEmail: string,
    timezone: string,
    localeCode: string,
    currency: string,
    address: string,
    phone: string,
  ) {
    event.preventDefault();
    this.saving.set(true);
    this.notice.set('');
    this.failed.set(false);
    try {
      const value = {
        ...current,
        name,
        slug,
        contactEmail,
        timezone,
        localeCode,
        currency,
        address,
        phone,
      };
      const response = await firstValueFrom(
        this.http.put<any>('/api/v1/content/business-profile', value),
      );
      this.profile.set((response?.data || response) as BusinessProfile);
      this.notice.set(this.t('Content saved.'));
    } catch (error) {
      this.forbidden.set((error as HttpErrorResponse).status === 403);
      this.failed.set(true);
      this.notice.set(
        this.t(
          'Strapi did not save the content. Check your role permissions and the field values.',
        ),
      );
    } finally {
      this.saving.set(false);
    }
  }
}
