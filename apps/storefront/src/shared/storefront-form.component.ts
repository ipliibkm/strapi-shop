import { HttpClient } from '@angular/common/http';
import { Component, OnInit, inject, input, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { firstValueFrom } from 'rxjs';
import { StorefrontI18n } from './i18n.service';
import { StorefrontForm } from './types';

/**
 * A self-contained storefront form: loads its own definition by slug, renders the fields and
 * submits them. Any page can embed one for a "form" content block without shared shell state.
 */
@Component({
  selector: 'ip-storefront-form',
  standalone: true,
  imports: [MatButtonModule],
  templateUrl: './storefront-form.component.html',
})
export class StorefrontFormComponent implements OnInit {
  private http = inject(HttpClient);
  readonly i18n = inject(StorefrontI18n);

  /** The slug of the form to load and render. */
  readonly slug = input.required<string>();
  /** The loaded form definition, or null while loading / when unavailable. */
  readonly definition = signal<StorefrontForm | null>(null);
  /** Whether the definition could not be loaded at all. */
  readonly failed = signal(false);
  readonly submitting = signal(false);
  readonly status = signal<{ message: string; error: boolean } | null>(null);

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

  private async load() {
    try {
      const form = await firstValueFrom(
        this.http.get<StorefrontForm>(`/api/v1/public/forms/${encodeURIComponent(this.slug())}`),
      );
      this.definition.set(form);
    } catch {
      this.failed.set(true);
    }
  }

  async submit(event: Event) {
    event.preventDefault();
    const element = event.target as HTMLFormElement;
    if (!element.reportValidity()) return;
    const definition = this.definition();
    if (!definition) return;
    const data = new FormData(element);
    const values: Record<string, string | boolean> = {};
    for (const field of definition.fields) {
      values[field.key] =
        field.type === 'checkbox' ? data.has(field.key) : String(data.get(field.key) ?? '');
    }
    this.submitting.set(true);
    this.status.set(null);
    try {
      const result = await firstValueFrom(
        this.http.post<{ id: string; message: string }>(
          `/api/v1/public/forms/${encodeURIComponent(this.slug())}/submissions`,
          { values, website: String(data.get('website') ?? '') },
        ),
      );
      element.reset();
      this.status.set({ message: result.message, error: false });
    } catch (error) {
      const message =
        (error as { error?: { message?: string } }).error?.message ??
        'The form could not be submitted. Try again.';
      this.status.set({ message, error: true });
    } finally {
      this.submitting.set(false);
    }
  }
}
