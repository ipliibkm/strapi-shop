import { Injectable, signal } from '@angular/core';
import { CartLine } from './types';

/**
 * Holds the customer's cart lines, persisted per browser. The header (cart count), the product
 * page (add to cart) and the cart/checkout page all read this service so they stay in sync.
 */
@Injectable({ providedIn: 'root' })
export class StorefrontCartService {
  private readonly cartKey = 'openshopfront.storefront.cart.v1';
  /** The current cart lines, loaded from browser storage on first use. */
  readonly lines = signal<CartLine[]>(this.read());

  /** Total number of units across all cart lines. */
  count(): number {
    return this.lines().reduce((sum, line) => sum + line.quantity, 0);
  }

  private read(): CartLine[] {
    try {
      const raw = localStorage.getItem(this.cartKey);
      if (!raw) return [];
      const parsed = JSON.parse(raw) as CartLine[];
      return Array.isArray(parsed) ? parsed : [];
    } catch {
      return [];
    }
  }

  private write(lines: CartLine[]) {
    try {
      localStorage.setItem(this.cartKey, JSON.stringify(lines));
    } catch {
      // Storage may be unavailable; the in-memory cart still works.
    }
  }

  /** Sets the quantity of one variant and limits it to the available stock. */
  set(
    variantId: string,
    productName: string,
    label: string | undefined,
    priceCents: number,
    quantity: number,
    availableQuantity: number,
  ): number {
    const lines = this.read();
    const existing = lines.find((line) => line.variantId === variantId);
    const maximum = Math.max(0, Math.floor(availableQuantity));
    if (maximum === 0) {
      this.remove(variantId);
      return 0;
    }
    const total = Math.max(1, Math.min(maximum, Math.round(quantity)));
    if (existing) {
      existing.productName = productName;
      existing.label = label || undefined;
      existing.priceCents = priceCents;
      existing.quantity = total;
      existing.availableQuantity = maximum;
    } else
      lines.push({
        variantId,
        productName,
        label: label || undefined,
        priceCents,
        quantity: total,
        availableQuantity: maximum,
      });
    this.write(lines);
    this.lines.set([...lines]);
    return total;
  }

  /** Sets the quantity of one cart line. */
  updateQuantity(variantId: string, quantity: number): number {
    let adjusted = 1;
    const clamped = Math.max(1, Math.min(99, Math.round(quantity)));
    const lines = this.read().map((line) => {
      if (line.variantId !== variantId) return line;
      adjusted = Math.min(clamped, line.availableQuantity ?? 99);
      return { ...line, quantity: adjusted };
    });
    this.write(lines);
    this.lines.set([...lines]);
    return adjusted;
  }

  /** Returns the quantity of one variant in the cart. */
  quantityFor(variantId: string): number {
    return this.lines().find((line) => line.variantId === variantId)?.quantity ?? 0;
  }

  /** Applies current stock to an existing line and returns its adjusted quantity. */
  reconcileStock(variantId: string, availableQuantity: number): number {
    const existing = this.lines().find((line) => line.variantId === variantId);
    if (!existing) return 0;
    if (availableQuantity <= 0) {
      this.remove(variantId);
      return 0;
    }
    return this.set(
      existing.variantId,
      existing.productName,
      existing.label ?? undefined,
      existing.priceCents,
      existing.quantity,
      availableQuantity,
    );
  }

  /** Removes one cart line. */
  remove(variantId: string) {
    const lines = this.read().filter((line) => line.variantId !== variantId);
    this.write(lines);
    this.lines.set([...lines]);
  }

  /** Clears the cart after a successful order. */
  clear() {
    this.write([]);
    this.lines.set([]);
  }
}
