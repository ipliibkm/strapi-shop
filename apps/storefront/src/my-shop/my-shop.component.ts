import { Component, effect, inject, output, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { StorefrontI18n } from '../shared/i18n.service';
import { CustomerAccountService } from '../shared/customer-account.service';
import { OrderSummary } from '../shared/types';

@Component({
  selector: 'ip-my-shop',
  standalone: true,
  imports: [MatButtonModule],
  templateUrl: './my-shop.component.html',
  styleUrls: ['./my-shop.component.css'],
})
export class MyShopComponent {
  readonly account = inject(CustomerAccountService);
  private readonly i18n = inject(StorefrontI18n);
  readonly mode = signal<'login' | 'register'>('login');
  readonly busy = signal(false);
  readonly error = signal('');
  readonly orders = signal<OrderSummary[]>([]);
  readonly checkout = output<void>();

  constructor() {
    effect(() => {
      if (this.account.customer()) void this.loadOrders();
      else this.orders.set([]);
    });
  }

  t(key: string) {
    return this.i18n.t(key);
  }

  async loadOrders() {
    if (!this.account.customer()) return;
    try {
      this.orders.set(await this.account.orders());
    } catch {
      this.error.set(this.t('account.ordersError'));
    }
  }

  async submit(email: string, password: string) {
    this.busy.set(true);
    this.error.set('');
    try {
      const signedIn = await this.account.authenticate(this.mode(), email.trim(), password);
      if (signedIn) await this.loadOrders();
      else this.error.set(this.t('account.confirmEmail'));
    } catch {
      this.error.set(this.t('account.authError'));
    } finally {
      this.busy.set(false);
    }
  }

  logout() {
    this.account.logout();
    this.orders.set([]);
  }
}
