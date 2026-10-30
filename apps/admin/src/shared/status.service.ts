import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { SetupCheck } from './types';

/** Holds the shared backend configuration status used by several views and the sidebar. */
@Injectable({ providedIn: 'root' })
export class AdminStatusService {
  private http = inject(HttpClient);
  readonly checks = signal<SetupCheck[]>([]);
  readonly backendOnline = signal(false);

  /** Number of configured services that report a ready status. */
  readyChecks(): number {
    return this.checks().filter((check) => check.status === 'ready').length;
  }

  /** Re-runs the backend configuration checks and records availability. */
  async refreshStatus() {
    try {
      this.checks.set(
        await firstValueFrom(this.http.get<SetupCheck[]>('/api/v1/admin/system/checks')),
      );
      this.backendOnline.set(true);
    } catch {
      this.backendOnline.set(false);
      this.checks.set([]);
    }
  }
}
