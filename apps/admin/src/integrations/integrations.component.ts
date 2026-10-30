import { HttpClient } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { firstValueFrom } from 'rxjs';
import { AdminAuth } from '../shared/auth.service';
import { AdminI18n } from '../shared/i18n.service';
import { GoogleCalendar, GoogleConnection } from '../shared/types';

/** The Google Calendar connection and resource-mapping view. */
@Component({
  selector: 'ip-integrations',
  standalone: true,
  imports: [
    MatButtonModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
  ],
  templateUrl: './integrations.component.html',
})
export class IntegrationsComponent {
  private http = inject(HttpClient);
  readonly i18n = inject(AdminI18n);
  readonly auth = inject(AdminAuth);
  readonly google = signal<GoogleConnection>({ connected: false });
  readonly calendars = signal<GoogleCalendar[]>([]);
  readonly googleBusy = signal(false);
  readonly googleNotice = signal('');

  constructor() {
    void this.refreshGoogle();
  }

  langVersion(): string {
    return this.i18n.lang();
  }
  t(text: string, params?: Record<string, string | number>): string {
    return this.i18n.t(text, params);
  }
  connectGoogle() {
    window.location.assign('/oauth2/authorization/google');
  }
  private async refreshGoogle() {
    try {
      const connection = await firstValueFrom(
        this.http.get<GoogleConnection>('/api/v1/admin/integrations/google'),
      );
      this.google.set(connection);
      this.googleNotice.set(connection.error || '');
      if (connection.connected)
        this.calendars.set(
          await firstValueFrom(
            this.http.get<GoogleCalendar[]>('/api/v1/admin/integrations/google/calendars'),
          ),
        );
    } catch {
      this.google.set({ connected: false });
      this.calendars.set([]);
      this.googleNotice.set(this.t('The app cannot verify the Google Calendar connection.'));
    }
  }
  async syncGoogle() {
    this.googleBusy.set(true);
    this.googleNotice.set('');
    try {
      await firstValueFrom(this.http.post('/api/v1/admin/integrations/google/sync', null));
      await this.refreshGoogle();
      this.googleNotice.set(this.t('Calendar synchronization finished.'));
    } catch {
      this.googleNotice.set(this.t('Calendar synchronization failed. Check the mapping status.'));
    } finally {
      this.googleBusy.set(false);
    }
  }
  async saveMapping(resourceId: string, calendarId: string, writeBookings: boolean) {
    if (!resourceId.trim() || !calendarId) {
      this.googleNotice.set(this.t('Enter a resource ID and select a calendar.'));
      return;
    }
    const calendar = this.calendars().find((item) => item.id === calendarId);
    if (!calendar) return;
    this.googleBusy.set(true);
    try {
      await firstValueFrom(
        this.http.put(
          `/api/v1/admin/integrations/google/mappings/${encodeURIComponent(resourceId.trim())}`,
          { calendarId, displayName: calendar.name, writeBookings },
        ),
      );
      await this.syncGoogle();
    } catch {
      this.googleNotice.set(this.t('The app could not save the calendar mapping.'));
    } finally {
      this.googleBusy.set(false);
    }
  }
  async removeMapping(resourceId: string) {
    this.googleBusy.set(true);
    try {
      await firstValueFrom(
        this.http.delete(
          `/api/v1/admin/integrations/google/mappings/${encodeURIComponent(resourceId)}`,
        ),
      );
      await this.refreshGoogle();
      this.googleNotice.set(this.t('The calendar mapping was removed.'));
    } catch {
      this.googleNotice.set(this.t('The app could not remove the calendar mapping.'));
    } finally {
      this.googleBusy.set(false);
    }
  }
  async disconnectGoogle() {
    this.googleBusy.set(true);
    try {
      await firstValueFrom(this.http.delete('/api/v1/admin/integrations/google'));
      this.google.set({ connected: false });
      this.calendars.set([]);
      this.googleNotice.set(this.t('The Google Calendar account was disconnected.'));
    } catch {
      this.googleNotice.set(this.t('The app could not disconnect the Google Calendar account.'));
    } finally {
      this.googleBusy.set(false);
    }
  }
}
