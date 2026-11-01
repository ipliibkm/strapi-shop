import { HttpClient } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { firstValueFrom } from 'rxjs';
import { localeOf, Lang } from '../shared/i18n';
import { AdminI18n } from '../shared/i18n.service';
import {
  TemplateEditorComponent,
  TemplateVariable,
} from '../template-editor/template-editor.component';

interface FormSummary {
  documentId: string;
  slug: string;
  name: string;
  enabled: boolean;
  published: boolean;
  fieldCount: number;
}

interface FormField {
  key: string;
  label: string;
  type: 'text' | 'email' | 'tel' | 'textarea' | 'select' | 'checkbox';
  required: boolean;
  placeholder: string;
  helpText: string;
  minLength: number;
  maxLength: number;
  options: string[];
}

interface FormDefinition {
  documentId: string;
  slug: string;
  name: string;
  enabled: boolean;
  sendMailEnabled: boolean;
  submitLabel: string;
  successMessage: string;
  fields: FormField[];
  published: boolean;
}

interface SubmissionValues {
  [key: string]: unknown;
}

interface Submission {
  id: string;
  values: SubmissionValues;
  createdAt: string;
}

interface SubmissionPage {
  items: Submission[];
  page: number;
  pageSize: number;
  total: number;
}

type FormTab = 'builder' | 'submissions';

/** Builds reusable forms and reviews the submissions they receive. */
type MailChoice = 'standard' | 'custom';

@Component({
  selector: 'ip-forms',
  standalone: true,
  imports: [MatButtonModule, MatIconModule, TemplateEditorComponent],
  templateUrl: './forms.component.html',
})
export class FormsComponent {
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
  readonly forms = signal<FormSummary[]>([]);
  readonly form = signal<FormDefinition | null>(null);
  readonly selectedSlug = signal('');
  readonly busy = signal(false);
  readonly dirty = signal(false);
  readonly notice = signal('');
  readonly tab = signal<FormTab>('builder');
  readonly submissions = signal<Submission[]>([]);
  readonly submissionTotal = signal(0);
  readonly page = signal(1);
  readonly subBusy = signal(false);
  readonly error = signal('');
  readonly detail = signal<Submission | null>(null);
  readonly confirming = signal(false);
  /** Receipt mail choice for the selected form. */
  readonly mailChoice = signal<MailChoice>('standard');
  /** The per-form template override draft; empty means the default channel template applies. */
  readonly mailTemplate = signal('');
  readonly mailSaving = signal(false);
  private pendingDelete: Submission | null = null;
  private static readonly PAGE_SIZE = 25;
  /** Re-evaluated on every access so labels follow the active language. */
  get fieldTypes(): { value: FormField['type']; label: string }[] {
    return [
      { value: 'text', label: this.t('Short text') },
      { value: 'email', label: this.t('Email address') },
      { value: 'tel', label: this.t('Telephone number') },
      { value: 'textarea', label: this.t('Long text') },
      { value: 'select', label: this.t('Dropdown') },
      { value: 'checkbox', label: this.t('Checkbox') },
    ];
  }

  constructor() {
    void this.refresh();
  }

  async refresh() {
    try {
      const forms = await firstValueFrom(this.http.get<FormSummary[]>('/api/v1/admin/forms'));
      this.forms.set(forms ?? []);
      if (!this.selectedSlug() && forms?.length) await this.selectForm(forms[0].slug);
    } catch {
      this.notice.set(this.t('The form list could not be loaded.'));
    }
  }

  async selectForm(slug: string) {
    this.busy.set(true);
    this.error.set('');
    try {
      const form = await firstValueFrom(
        this.http.get<FormDefinition>(`/api/v1/admin/forms/${encodeURIComponent(slug)}`),
      );
      this.form.set({
        ...form,
        sendMailEnabled: form.sendMailEnabled !== false,
        fields: form.fields.map((field) => ({ ...field })),
      });
      this.selectedSlug.set(slug);
      this.dirty.set(false);
      this.tab.set('builder');
      this.resetSubmissions();
      void this.loadMailTemplate(slug);
    } catch {
      this.notice.set(this.t('The form could not be opened.'));
    } finally {
      this.busy.set(false);
    }
  }

  newForm() {
    const slug = `form_${Date.now()}`;
    this.form.set({
      documentId: '',
      slug,
      name: this.t('Untitled form'),
      enabled: true,
      sendMailEnabled: true,
      submitLabel: this.t('Submit'),
      successMessage: this.t('Thank you. Your form was submitted.'),
      published: false,
      fields: [this.emptyField(1)],
    });
    this.selectedSlug.set(slug);
    this.dirty.set(true);
    this.error.set('');
    this.mailChoice.set('standard');
    this.mailTemplate.set('');
  }

  /** Loads the stored per-form template override. */
  private async loadMailTemplate(slug: string): Promise<void> {
    try {
      const res = await firstValueFrom(
        this.http.get<{ template?: string }>(
          `/api/v1/admin/email/forms/${encodeURIComponent(slug)}/template`,
        ),
      );
      const stored = (res?.template ?? '').trim();
      this.mailTemplate.set(stored);
      this.mailChoice.set(stored ? 'custom' : 'standard');
    } catch {
      // A missing override is not an error; the default channel template applies.
      this.mailTemplate.set('');
      this.mailChoice.set('standard');
    }
  }

  /** Switches the receipt mail choice and seeds a custom draft with the default template once. */
  setMailMode(mode: MailChoice): void {
    if (this.mailChoice() === mode) return;
    this.mailChoice.set(mode);
    if (mode === 'custom' && !this.mailTemplate().trim()) {
      // Start a custom draft from the configured default template when one exists.
      firstValueFrom(
        this.http.get<{ form?: { template?: string } }>('/api/v1/admin/email/settings'),
      )
        .then((settings) => {
          const base = (settings?.form?.template ?? '').trim();
          if (base && !this.mailTemplate().trim()) this.mailTemplate.set(base);
        })
        .catch(() => undefined);
    }
  }

  onMailTemplateChange(template: string): void {
    this.mailTemplate.set(template ?? '');
  }

  /** Variables for the receipt template: form name plus every field of this form. */
  mailVariables(): TemplateVariable[] {
    const current = this.form();
    if (!current) return [];
    const base: TemplateVariable[] = [{ name: 'formName', sample: current.name || 'Contact form' }];
    for (const field of current.fields) {
      if (!field.key) continue;
      base.push({ name: field.key, sample: this.mailSampleFor(field) });
    }
    return base;
  }

  private mailSampleFor(field: FormField): string {
    switch (field.type) {
      case 'email':
        return 'max@example.com';
      case 'tel':
        return '+49 123 4567890';
      case 'textarea':
        return this.t('Sample long text.');
      case 'checkbox':
        return this.t('Yes');
      case 'select':
        return field.options[0] ?? 'Option 1';
      default:
        return 'John Doe';
    }
  }

  /** Saves the receipt mail choice and template override for the selected form. */
  async saveMailConfig(): Promise<void> {
    const slug = this.selectedSlug();
    if (!slug || this.mailSaving()) return;
    this.mailSaving.set(true);
    this.error.set('');
    try {
      await firstValueFrom(
        this.http.put(`/api/v1/admin/email/forms/${encodeURIComponent(slug)}/template`, {
          template: this.mailChoice() === 'custom' ? this.mailTemplate() : '',
        }),
      );
      this.notice.set(this.t('The mail settings for this form were saved.'));
    } catch (err) {
      this.error.set(this.describe(err, 'The mail settings could not be saved for this form.'));
    } finally {
      this.mailSaving.set(false);
    }
  }

  setEnabled(event: Event) {
    this.updateForm({ enabled: (event.target as HTMLInputElement).checked });
  }

  /** Toggles the submission notification email for this form. */
  setSendMailEnabled(event: Event) {
    this.updateForm({ sendMailEnabled: (event.target as HTMLInputElement).checked });
  }

  setFormValue(field: 'name' | 'slug' | 'submitLabel' | 'successMessage', event: Event) {
    const value = (event.target as HTMLInputElement).value;
    this.updateForm({ [field]: value });
    if (field === 'slug') this.selectedSlug.set(value);
  }

  addField() {
    const current = this.form();
    if (!current) return;
    // Avoid duplicate keys: they break the field list and the submissions table.
    let index = current.fields.length + 1;
    while (current.fields.some((field) => field.key === `field_${index}`)) index++;
    this.updateForm({ fields: [...current.fields, this.emptyField(index)] });
  }

  removeField(index: number) {
    const current = this.form();
    if (!current) return;
    this.updateForm({ fields: current.fields.filter((_, fieldIndex) => fieldIndex !== index) });
  }

  moveField(index: number, direction: number) {
    const current = this.form();
    if (!current) return;
    const target = index + direction;
    if (target < 0 || target >= current.fields.length) return;
    const fields = [...current.fields];
    [fields[index], fields[target]] = [fields[target], fields[index]];
    this.updateForm({ fields });
  }

  setFieldValue(index: number, field: 'key' | 'label' | 'placeholder' | 'helpText', event: Event) {
    this.updateField(index, { [field]: (event.target as HTMLInputElement).value });
  }

  setFieldType(index: number, event: Event) {
    const type = (event.target as HTMLSelectElement).value as FormField['type'];
    const change: Partial<FormField> = { type };
    if (type === 'select' && !this.form()?.fields[index].options.length) {
      change.options = ['Option 1', 'Option 2'];
    }
    this.updateField(index, change);
  }

  setFieldRequired(index: number, event: Event) {
    this.updateField(index, { required: (event.target as HTMLInputElement).checked });
  }

  setFieldNumber(index: number, field: 'minLength' | 'maxLength', event: Event) {
    const value = Math.max(0, Number((event.target as HTMLInputElement).value) || 0);
    this.updateField(index, { [field]: value });
  }

  fieldOptions(field: FormField): string[] {
    return field.options;
  }

  setFieldOptions(index: number, event: Event) {
    const options = (event.target as HTMLTextAreaElement).value
      .split('\n')
      .map((option) => option.trim())
      .filter(Boolean);
    this.updateField(index, { options });
  }

  async saveDraft(): Promise<boolean> {
    const current = this.form();
    if (!current) return false;
    this.busy.set(true);
    this.error.set('');
    try {
      const saved = await firstValueFrom(
        this.http.put<FormDefinition>(`/api/v1/admin/forms/${encodeURIComponent(current.slug)}`, {
          name: current.name,
          enabled: current.enabled,
          sendMailEnabled: current.sendMailEnabled,
          submitLabel: current.submitLabel,
          successMessage: current.successMessage,
          fields: current.fields,
        }),
      );
      this.form.set(saved);
      this.selectedSlug.set(saved.slug);
      this.dirty.set(false);
      this.notice.set(this.t('Draft saved.'));
      await this.refresh();
      return true;
    } catch (err) {
      this.error.set(this.describe(err, 'The form draft could not be saved.'));
      return false;
    } finally {
      this.busy.set(false);
    }
  }

  async publish() {
    if (!(await this.saveDraft())) return;
    const current = this.form();
    if (!current) return;
    this.busy.set(true);
    try {
      const published = await firstValueFrom(
        this.http.post<FormDefinition>(
          `/api/v1/admin/forms/${encodeURIComponent(current.slug)}/publish`,
          null,
        ),
      );
      this.form.set(published);
      this.error.set('');
      this.notice.set(
        published.enabled
          ? this.t('The form is available on the storefront.')
          : this.t('The form is published but disabled.'),
      );
      await this.refresh();
    } catch (err) {
      this.error.set(this.describe(err, 'The form could not be published.'));
    } finally {
      this.busy.set(false);
    }
  }

  switchTab(next: FormTab) {
    if (this.tab() === next) return;
    this.tab.set(next);
    this.error.set('');
    if (next === 'submissions') void this.refreshSubmissions();
  }

  pageCount(): number {
    const total = Math.max(1, this.submissionTotal());
    return Math.ceil(total / FormsComponent.PAGE_SIZE);
  }

  hasMorePages(): boolean {
    return this.pageCount() > 1;
  }

  goToPage(page: number) {
    if (page < 1 || page > this.pageCount()) return;
    this.page.set(page);
    void this.refreshSubmissions();
  }

  async refreshSubmissions() {
    const slug = this.selectedSlug();
    if (!slug) return;
    this.subBusy.set(true);
    this.error.set('');
    try {
      const pageRes = await firstValueFrom(
        this.http.get<SubmissionPage>(
          `/api/v1/admin/forms/${encodeURIComponent(slug)}/submissions`,
          {
            params: { page: String(this.page()), pageSize: String(FormsComponent.PAGE_SIZE) },
          },
        ),
      );
      this.submissions.set(pageRes.items ?? []);
      this.submissionTotal.set(pageRes.total ?? 0);
    } catch (err) {
      this.error.set(this.describe(err, 'The submission list could not be loaded.'));
    } finally {
      this.subBusy.set(false);
    }
  }

  async downloadCsv() {
    const slug = this.selectedSlug();
    if (!slug) return;
    this.subBusy.set(true);
    try {
      const buffer = (await firstValueFrom(
        this.http.get(`/api/v1/admin/forms/${encodeURIComponent(slug)}/submissions.csv`, {
          responseType: 'arraybuffer',
        }),
      )) as ArrayBuffer;
      if (!buffer) return;
      const text = new TextDecoder('utf-8').decode(new Uint8Array(buffer));
      this.saveCsv(text, `form-${slug}-submissions.csv`);
    } catch (err) {
      this.error.set(this.describe(err, 'The CSV download failed.'));
    } finally {
      this.subBusy.set(false);
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

  renderValue(field: FormField, values?: SubmissionValues): string {
    if (!values) return '';
    const raw = values[field.key];
    if (raw === undefined || raw === null) return '';
    if (typeof raw === 'boolean') return raw ? this.t('Yes') : this.t('No');
    if (Array.isArray(raw)) return raw.map((item) => String(item)).join('; ');
    return String(raw);
  }

  formatDate(iso: string): string {
    const date = new Date(iso);
    if (Number.isNaN(date.getTime())) return iso;
    try {
      return new Intl.DateTimeFormat(localeOf(this.i18n.lang()), {
        dateStyle: 'medium',
        timeStyle: 'short',
      }).format(date);
    } catch {
      return date.toISOString();
    }
  }

  openDetail(item: Submission) {
    this.error.set('');
    this.detail.set({ ...item, values: { ...(item.values ?? {}) } });
  }

  closeDetail() {
    if (this.confirming()) {
      this.cancelDelete();
    } else {
      this.detail.set(null);
    }
  }

  confirmDelete(target: Submission) {
    this.pendingDelete = target;
    this.error.set('');
    this.confirming.set(true);
  }

  cancelDelete() {
    this.confirming.set(false);
    this.pendingDelete = null;
  }

  async performDelete(target: Submission) {
    const slug = this.selectedSlug();
    if (!slug || !target) return;
    this.subBusy.set(true);
    this.error.set('');
    try {
      await firstValueFrom(
        this.http.delete(
          `/api/v1/admin/forms/${encodeURIComponent(slug)}/submissions/${encodeURIComponent(target.id)}`,
        ),
      );
      const open = this.detail();
      if (open && open.id === target.id) {
        this.detail.set(null);
      }
      this.confirming.set(false);
      this.pendingDelete = null;
      const remainingPageCount = Math.max(
        1,
        Math.ceil((this.submissionTotal() - 1) / FormsComponent.PAGE_SIZE),
      );
      if (this.page() > remainingPageCount && this.page() > 1) {
        this.page.set(remainingPageCount);
      }
      await this.refreshSubmissions();
      this.notice.set(this.t('The submission was deleted.'));
    } catch (err) {
      this.error.set(this.describe(err, 'The deletion failed. Try again.'));
    } finally {
      this.subBusy.set(false);
    }
  }

  private resetSubmissions() {
    this.submissions.set([]);
    this.submissionTotal.set(0);
    this.page.set(1);
    this.detail.set(null);
    this.confirming.set(false);
    this.pendingDelete = null;
    this.error.set('');
  }

  private describe(err: unknown, fallback: string): string {
    const anyErr = err as { error?: { message?: string }; status?: number };
    if (anyErr?.error?.message) return this.t(anyErr.error.message);
    if (anyErr?.status === 401 || anyErr?.status === 403) {
      return this.t('Your administrator session expired. Sign in again to continue.');
    }
    return this.t(fallback);
  }

  private updateForm(change: Partial<FormDefinition>) {
    const current = this.form();
    if (!current) return;
    this.form.set({ ...current, ...change });
    this.dirty.set(true);
  }

  private updateField(index: number, change: Partial<FormField>) {
    const current = this.form();
    if (!current) return;
    const fields = [...current.fields];
    fields[index] = { ...fields[index], ...change };
    this.updateForm({ fields });
  }

  private emptyField(index: number): FormField {
    return {
      key: `field_${index}`,
      label: this.t('Field {index}', { index }),
      type: 'text',
      required: false,
      placeholder: '',
      helpText: '',
      minLength: 0,
      maxLength: 1000,
      options: [],
    };
  }
}
