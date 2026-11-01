import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { OrderSummary } from './types';

interface Customer {
  id: number;
  email: string;
  username: string;
}

@Injectable({ providedIn: 'root' })
export class CustomerAccountService {
  private readonly http = inject(HttpClient);
  readonly enabled = signal(false);
  readonly customer = signal<Customer | null>(null);
  private token: string | null = null;

  constructor() {
    try {
      this.token = sessionStorage.getItem('shop-customer-token');
    } catch {
      /* Storage is optional. */
    }
    void this.load();
  }

  async load() {
    try {
      const config = await firstValueFrom(
        this.http.get<{ enabled: boolean }>('/api/v1/public/customer-area'),
      );
      this.enabled.set(config.enabled);
      if (!config.enabled) {
        this.logout();
        return;
      }
      if (this.token) await this.refresh();
    } catch {
      this.enabled.set(false);
    }
  }

  headers(): HttpHeaders {
    return new HttpHeaders(this.token ? { Authorization: `Bearer ${this.token}` } : {});
  }

  async refresh() {
    try {
      this.customer.set(
        await firstValueFrom(
          this.http.get<Customer>('/api/v1/public/customer-area/me', { headers: this.headers() }),
        ),
      );
    } catch {
      this.logout();
    }
  }

  async authenticate(
    mode: 'login' | 'register',
    email: string,
    password: string,
  ): Promise<boolean> {
    const result = await firstValueFrom(
      this.http.post<{ jwt?: string }>('/api/v1/public/customer-area/' + mode, { email, password }),
    );
    if (!result.jwt) return false; // Strapi can require email confirmation.
    this.token = result.jwt;
    try {
      sessionStorage.setItem('shop-customer-token', result.jwt);
    } catch {
      /* Use the in-memory token. */
    }
    await this.refresh();
    return this.customer() !== null;
  }

  async orders(): Promise<OrderSummary[]> {
    return firstValueFrom(
      this.http.get<OrderSummary[]>('/api/v1/public/customer-area/orders', {
        headers: this.headers(),
      }),
    );
  }

  logout() {
    this.token = null;
    this.customer.set(null);
    try {
      sessionStorage.removeItem('shop-customer-token');
    } catch {
      /* Storage is optional. */
    }
  }
}
