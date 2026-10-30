import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { Product } from './types';

/**
 * Holds the public product catalog. The shop page and the "products" content blocks on
 * home/services/about all read this service so they share one load of the catalog.
 */
@Injectable({ providedIn: 'root' })
export class StorefrontShopService {
  private http = inject(HttpClient);
  /** Whether the catalog is currently being loaded. */
  readonly loading = signal(false);
  /** Whether the shop has any products to show. */
  readonly open = signal(true);
  /** The public product list, in backend order. */
  readonly products = signal<Product[]>([]);

  /** Loads the public catalog once; safe to call repeatedly. */
  async load() {
    if (this.loading()) return;
    this.loading.set(true);
    try {
      const products = await firstValueFrom(this.http.get<Product[]>('/api/v1/public/products'));
      this.products.set(products ?? []);
      if (!products?.length) this.open.set(false);
    } catch {
      this.open.set(false);
    } finally {
      this.loading.set(false);
    }
  }

  /** The first products up to a limit, for "products" content blocks. */
  first(limit: number): Product[] {
    return this.products().slice(0, Math.max(1, limit));
  }
}
