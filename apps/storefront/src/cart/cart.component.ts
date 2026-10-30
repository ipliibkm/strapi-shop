import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Component, inject, output, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { firstValueFrom } from 'rxjs';
import { StorefrontCartService } from '../shared/cart.service';
import { StorefrontI18n } from '../shared/i18n.service';
import { CheckoutResult } from '../shared/types';

/** The cart page: line editing, checkout form and order confirmation. */
@Component({
  selector: 'ip-cart',
  standalone: true,
  imports: [MatButtonModule, MatIconModule],
  templateUrl: './cart.component.html',
  styleUrls: ['./cart.component.css'],
})
export class CartComponent {
  private http = inject(HttpClient);
  readonly i18n = inject(StorefrontI18n);
  readonly cart = inject(StorefrontCartService);

  readonly placingOrder = signal(false);
  readonly checkoutError = signal('');
  readonly quantityNotice = signal('');
  readonly checkoutPlaced = signal(false);
  readonly placedOrder = signal<{
    orderNumber: string;
    accessToken: string;
    checkoutUrl?: string | null;
  } | null>(null);
  /** Requests the shell to open the shop catalog. */
  readonly browseShop = output<void>();

  langVersion(): string {
    return this.i18n.lang();
  }
  t(key: string, params?: Record<string, string | number>): string {
    return this.i18n.t(key, params);
  }
  initialOf(name?: string): string {
    return (name || '?').trim().charAt(0).toUpperCase() || '?';
  }
  money(cents: number, currency = 'EUR') {
    try {
      return (cents / 100).toLocaleString(this.i18n.locale(), { style: 'currency', currency });
    } catch {
      return `${(cents / 100).toFixed(2)} ${currency}`;
    }
  }

  private updateLine(line: { variantId: string; availableQuantity?: number }, event: Event) {
    const input = event.target as HTMLInputElement;
    const value = Number(input.value);
    if (Number.isNaN(value)) return;
    const adjusted = this.cart.updateQuantity(line.variantId, value);
    input.value = String(adjusted);
    this.quantityNotice.set(
      value > adjusted && line.availableQuantity
        ? this.t('product.stockLimit', { count: line.availableQuantity })
        : '',
    );
  }

  private removeLine(variantId: string) {
    this.cart.remove(variantId);
  }

  async placeOrder(
    name: string,
    email: string,
    street: string,
    city: string,
    postalCode: string,
    country: string,
  ) {
    if (!name.trim() || !email.trim()) {
      this.checkoutError.set(this.t('error.nameEmail'));
      return;
    }
    const lines = this.cart.lines();
    if (lines.length === 0) {
      this.checkoutError.set(this.t('error.cartEmpty'));
      return;
    }
    this.placingOrder.set(true);
    this.checkoutError.set('');
    try {
      const result = await firstValueFrom(
        this.http.post<CheckoutResult>('/api/v1/public/checkout', {
          customerName: name.trim(),
          customerEmail: email.trim(),
          street: street.trim() || null,
          city: city.trim() || null,
          postalCode: postalCode.trim() || null,
          country: country.trim().toUpperCase() || null,
          lines: lines.map((line) => ({ variantId: line.variantId, quantity: line.quantity })),
        }),
      );
      this.cart.clear();
      this.quantityNotice.set('');
      this.checkoutPlaced.set(true);
      this.placedOrder.set({
        orderNumber: result.orderNumber,
        accessToken: result.accessToken,
        checkoutUrl: result.checkoutUrl,
      });
    } catch (error) {
      const response = error as HttpErrorResponse;
      const message = response?.error?.message;
      this.checkoutError.set(message || this.t('error.orderFailed'));
    } finally {
      this.placingOrder.set(false);
    }
  }
}
