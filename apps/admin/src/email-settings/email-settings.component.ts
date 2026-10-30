import { HttpClient } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { firstValueFrom } from 'rxjs';
import { AdminI18n } from '../shared/i18n.service';
import { Lang, localeOf } from '../shared/i18n';
import {
  TemplateVariable,
  TemplateEditorComponent,
} from '../template-editor/template-editor.component';

interface ChannelMaps {
  subjects: Record<string, string>;
  templates: Record<string, string>;
}

interface EmailSettingsResponse {
  form: ChannelMaps;
  order: ChannelMaps;
  variables?: { order?: string[]; form?: string[] };
}

interface StorefrontLanguage {
  code: string;
  name: string;
  enabled: boolean;
}

/** Sample values shown in the preview of the order channel. */
const ORDER_SAMPLES: Record<string, string> = {
  orderNumber: 'SO-A1B2C3D4',
  customerName: 'Max Mustermann',
  email: 'customer@example.com',
  street: 'Example Street',
  city: 'Example City',
  postalCode: '00000',
  items: '1. Runner Pro (Standard) x1: 129,00 EUR',
  subtotal: '129,00 EUR',
  tax: '26,49 EUR',
  shipping: '5,90 EUR',
  total: '161,39 EUR',
};

/** Configures the mail subjects and HTML templates per language for the platform channels. */
@Component({
  selector: 'ip-email-settings',
  standalone: true,
  imports: [MatButtonModule, TemplateEditorComponent],
  templateUrl: './email-settings.component.html',
})
export class EmailSettingsComponent {
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

  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly notice = signal('');
  readonly error = signal('');
  /** The storefront language currently being edited. */
  readonly locale = signal<string>('default');
  readonly languages = signal<StorefrontLanguage[]>([]);

  // Per-language maps (unsaved state).
  readonly formSubjects = signal<Record<string, string>>({});
  readonly formTemplates = signal<Record<string, string>>({});
  readonly orderSubjects = signal<Record<string, string>>({});
  readonly orderTemplates = signal<Record<string, string>>({});

  constructor() {
    void this.load();
  }

  private async load(): Promise<void> {
    this.loading.set(true);
    try {
      const [settings, storefront] = await Promise.all([
        firstValueFrom(this.http.get<EmailSettingsResponse>('/api/v1/admin/email/settings')),
        firstValueFrom(
          this.http.get<{ languages?: StorefrontLanguage[] }>('/api/v1/public/settings'),
        ).catch(() => ({ languages: [] as StorefrontLanguage[] })),
      ]);

      this.formSubjects.set(settings?.form?.subjects ?? {});
      this.formTemplates.set(settings?.form?.templates ?? {});
      this.orderSubjects.set(settings?.order?.subjects ?? {});
      this.orderTemplates.set(settings?.order?.templates ?? {});
      this.languages.set((storefront?.languages ?? []).filter((l) => l.enabled));

      // Default to the first enabled storefront language or "default".
      const first = (storefront?.languages ?? []).find((l) => l.enabled);
      if (first) this.locale.set(first.code);
    } catch (err) {
      this.error.set(this.describe(err, 'The mail settings could not be loaded.'));
    } finally {
      this.loading.set(false);
    }
  }

  /** All selectable languages: "default" plus all enabled storefront languages. */
  readonly localeOptions = signal<{ code: string; name: string }[]>([
    { code: 'default', name: 'Default' },
  ]);

  onLocaleChange(event: Event): void {
    const value = (event.target as HTMLSelectElement).value;
    if (value) this.locale.set(value);
  }

  /** The subject for the currently selected locale. */
  formSubject(): string {
    return this.formSubjects()[this.locale()] ?? '';
  }

  orderSubject(): string {
    return this.orderSubjects()[this.locale()] ?? '';
  }

  /** The template for the currently selected locale. */
  formTemplate(): string {
    return this.formTemplates()[this.locale()] ?? '';
  }

  orderTemplate(): string {
    return this.orderTemplates()[this.locale()] ?? '';
  }

  setSubject(channel: 'form' | 'order', event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.setSubjectValue(channel, value);
  }

  private setSubjectValue(channel: 'form' | 'order', value: string): void {
    const locale = this.locale();
    if (channel === 'form') {
      this.formSubjects.set({ ...this.formSubjects(), [locale]: value });
    } else {
      this.orderSubjects.set({ ...this.orderSubjects(), [locale]: value });
    }
  }

  onFormTemplateChange(template: string): void {
    const locale = this.locale();
    this.formTemplates.set({ ...this.formTemplates(), [locale]: template ?? '' });
  }

  onOrderTemplateChange(template: string): void {
    const locale = this.locale();
    this.orderTemplates.set({ ...this.orderTemplates(), [locale]: template ?? '' });
  }

  /** Variables of the order channel: documented names plus preview sample values. */
  readonly orderVariables: TemplateVariable[] = (
    [
      'orderNumber',
      'customerName',
      'email',
      'street',
      'city',
      'postalCode',
      'items',
      'subtotal',
      'tax',
      'shipping',
      'total',
      'date',
    ] as const
  ).map((name) => ({ name, sample: name === 'date' ? this.today() : ORDER_SAMPLES[name] }));

  /** Variables of the form channel; each form adds its own field keys on the Forms page. */
  readonly formVariables: TemplateVariable[] = [{ name: 'formName', sample: 'Contact form' }];

  private today(): string {
    try {
      return new Intl.DateTimeFormat(localeOf(this.i18n.lang()), { dateStyle: 'medium' }).format(
        new Date(),
      );
    } catch {
      return new Date().toISOString();
    }
  }

  async save(): Promise<void> {
    if (this.saving()) return;
    this.saving.set(true);
    this.error.set('');
    try {
      await firstValueFrom(
        this.http.put('/api/v1/admin/email/settings', {
          form: { subjects: this.formSubjects(), templates: this.formTemplates() },
          order: { subjects: this.orderSubjects(), templates: this.orderTemplates() },
        }),
      );
      this.notice.set(this.t('The mail settings were saved.'));
    } catch (err) {
      this.error.set(this.describe(err, 'The mail settings could not be saved.'));
    } finally {
      this.saving.set(false);
    }
  }

  private describe(err: unknown, fallback: string): string {
    const anyErr = err as { error?: { message?: string }; status?: number };
    if (anyErr?.error?.message) return this.t(anyErr.error.message);
    if (anyErr?.status === 401 || anyErr?.status === 403) {
      return this.t('Your administrator session expired. Sign in again to continue.');
    }
    return this.t(fallback);
  }
}
