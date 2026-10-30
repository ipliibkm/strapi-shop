import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { AdminI18n } from './i18n.service';

interface AuthConfig {
  enabled: boolean;
  mode: 'strapi';
  googleCalendarEnabled: boolean;
}
interface SessionInfo {
  authenticated: boolean;
  name?: string;
  email?: string;
  superAdmin?: boolean;
}
interface CsrfInfo {
  headerName: string;
  token: string;
}
export interface AdminUser {
  profile: { name: string; email: string };
  superAdmin: boolean;
}

@Injectable({ providedIn: 'root' })
export class AdminAuth {
  private readonly http = inject(HttpClient);
  private readonly i18n = inject(AdminI18n);
  readonly user = signal<AdminUser | null>(null);
  readonly ready = signal(false);
  readonly configured = signal(false);
  readonly googleCalendarEnabled = signal(false);
  readonly error = signal('');
  readonly csrf = signal<CsrfInfo | null>(null);

  async initialize() {
    try {
      const config = await firstValueFrom(this.http.get<AuthConfig>('/api/v1/auth/config'));
      this.configured.set(config.enabled && config.mode === 'strapi');
      this.googleCalendarEnabled.set(config.googleCalendarEnabled);
      await this.refreshCsrf();
      this.setSession(await firstValueFrom(this.http.get<SessionInfo>('/api/v1/auth/session')));
    } catch {
      this.error.set(this.i18n.t('Administrator sign-in is not available. Try again later.'));
    } finally {
      this.ready.set(true);
    }
  }

  async login(email: string, password: string): Promise<boolean> {
    this.error.set('');
    try {
      await this.refreshCsrf();
      const session = await firstValueFrom(
        this.http.post<SessionInfo>('/api/v1/auth/login', { email, password }),
      );
      // Spring Security clears the pre-login token after authentication succeeds.
      await this.refreshCsrf();
      this.setSession(session);
      return !!this.user();
    } catch (error) {
      const response = error as HttpErrorResponse;
      this.error.set(
        this.i18n.t(
          response.error?.message ||
            'Administrator sign-in failed. Check your email address and password.',
        ),
      );
      return false;
    }
  }

  async logout() {
    await firstValueFrom(this.http.post('/api/v1/auth/logout', null));
    this.user.set(null);
  }
  private async refreshCsrf() {
    this.csrf.set(await firstValueFrom(this.http.get<CsrfInfo>('/api/v1/auth/csrf')));
  }
  private setSession(session: SessionInfo) {
    this.user.set(
      session.authenticated
        ? {
            profile: {
              name: session.name || session.email || 'Administrator',
              email: session.email || '',
            },
            superAdmin: !!session.superAdmin,
          }
        : null,
    );
  }
}
