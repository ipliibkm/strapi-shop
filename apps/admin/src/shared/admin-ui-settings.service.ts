import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { AdminI18n } from './i18n.service';
import { AdminUiLanguage } from './types';

/**
 * Holds the global admin interface language switches (shared by all admins). The top-bar
 * switcher and the Appearance page both read this service so they stay in sync.
 */
@Injectable({ providedIn: 'root' })
export class AdminUiSettingsService {
  private http = inject(HttpClient);
  private i18n = inject(AdminI18n);
  readonly languages = signal<AdminUiLanguage[] | null>(null);
  readonly busy = signal(false);
  readonly notice = signal('');

  /** All admin interface languages in display order, or an empty list while loading. */
  list(): AdminUiLanguage[] {
    return this.languages() ?? [];
  }

  /** Loads the current interface language switches from the backend. */
  async load() {
    try {
      const languages = await firstValueFrom(
        this.http.get<AdminUiLanguage[]>('/api/v1/admin/ui-settings/languages'),
      );
      this.languages.set(languages);
    } catch {
      this.notice.set(this.i18n.t('The admin interface languages could not be loaded.'));
    }
  }

  /** Toggles one admin interface language and saves the complete list for all admins. */
  async toggle(item: AdminUiLanguage, enabled: boolean) {
    if (!enabled && this.list().filter((entry) => entry.enabled).length === 1) {
      this.notice.set(this.i18n.t('Keep at least one language enabled.'));
      return;
    }
    const languages = this.list().map((entry) => ({
      code: entry.code,
      name: entry.name,
      enabled: entry.code === item.code ? enabled : entry.enabled,
    }));
    this.languages.set(languages);
    // Every request carries the complete list, so concurrent saves stay consistent.
    this.busy.set(true);
    this.notice.set('');
    try {
      const saved = await firstValueFrom(
        this.http.put<AdminUiLanguage[]>(
          '/api/v1/admin/ui-settings/languages',
          languages.map((entry) => ({ code: entry.code, enabled: entry.enabled })),
        ),
      );
      this.languages.set(saved);
      this.notice.set(this.i18n.t('The admin interface languages are saved.'));
    } catch (error) {
      const message = (error as HttpErrorResponse)?.error?.message;
      await this.load();
      this.notice.set(this.i18n.t(message || 'The language changes could not be saved.'));
    } finally {
      this.busy.set(false);
    }
  }
}
