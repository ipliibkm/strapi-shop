import { HttpClient } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatStepperModule } from '@angular/material/stepper';
import { OpenShopfrontShellComponent } from '@openshopfront/ui';
import { firstValueFrom } from 'rxjs';

/** Collects and saves the first-installation business settings. */
@Component({
  selector: 'ip-setup',
  standalone: true,
  imports: [
    FormsModule,
    MatStepperModule,
    MatInputModule,
    MatFormFieldModule,
    MatButtonModule,
    MatCheckboxModule,
    MatSelectModule,
    MatCardModule,
    OpenShopfrontShellComponent,
  ],
  templateUrl: './setup.component.html',
  styleUrls: ['./setup.component.css'],
})
export class SetupComponent {
  private readonly http = inject(HttpClient);
  readonly themes = [
    { key: 'light', label: 'Maison (warm cream)' },
    { key: 'dark', label: 'Noir Atelier (charcoal gold)' },
    { key: 'ocean', label: 'Lagoon (seafoam teal)' },
    { key: 'rosewater', label: 'Blush (soft pink)' },
  ];
  readonly optionalModules = ['commerce', 'booking', 'calendar', 'gmail'];
  company = {
    name: '',
    contactEmail: '',
    timezone: 'Europe/Berlin',
    locale: 'de',
    currency: 'EUR',
    address: '',
    phone: '',
  };
  admin = { email: '', password: '' };
  modules = ['website'];
  adminTheme = 'light';
  storefrontTheme = 'light';
  token = '';
  readonly message = signal('');
  readonly saving = signal(false);
  readonly checking = signal(false);
  readonly checks = signal<{ id: string; status: string; message: string }[]>([]);

  async verify() {
    this.checking.set(true);
    try {
      this.checks.set(
        await firstValueFrom(
          this.http.get<{ id: string; status: string; message: string }[]>('/setup/api/checks'),
        ),
      );
    } catch {
      this.message.set('The system check failed. Check the server connection.');
    } finally {
      this.checking.set(false);
    }
  }

  validCompany() {
    return (
      !!this.company.name.trim() &&
      this.company.contactEmail.includes('@') &&
      !!this.company.timezone.trim() &&
      this.company.currency.length === 3
    );
  }

  validAdmin() {
    return this.admin.email.includes('@') && this.admin.password.length >= 14;
  }

  toggle(module: string, checked: boolean) {
    this.modules = checked
      ? [...this.modules, module]
      : this.modules.filter((item) => item !== module);
  }

  async save() {
    this.saving.set(true);
    this.message.set('Save the setup settings.');
    try {
      const result = await firstValueFrom(
        this.http.post<{ message: string }>(
          '/setup/api/complete',
          {
            company: this.company,
            admin: this.admin,
            modules: this.modules,
            adminTheme: this.adminTheme,
            storefrontTheme: this.storefrontTheme,
          },
          { headers: { 'X-Setup-Token': this.token } },
        ),
      );
      this.message.set(result.message);
      this.admin.password = '';
      this.token = '';
      this.checks.set([]);
    } catch (error: unknown) {
      const response = error as { error?: { message?: string } };
      this.message.set(
        response.error?.message ??
          'The server did not save the settings. Check the token and try again.',
      );
    } finally {
      this.saving.set(false);
    }
  }
}
