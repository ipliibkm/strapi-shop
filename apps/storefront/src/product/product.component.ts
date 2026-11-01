import { HttpClient } from '@angular/common/http';
import { Component, OnInit, inject, input, output, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { OpenShopfrontGalleryComponent, OpenShopfrontGalleryItem } from '@openshopfront/ui';
import { firstValueFrom } from 'rxjs';
import { StorefrontCartService } from '../shared/cart.service';
import { StorefrontI18n } from '../shared/i18n.service';
import { Product, ProductDetail, VariantOption } from '../shared/types';

/** The product detail page: photo gallery, variant selection and add-to-cart. */
@Component({
  selector: 'ip-product',
  standalone: true,
  imports: [MatButtonModule, MatIconModule, OpenShopfrontGalleryComponent],
  templateUrl: './product.component.html',
})
export class ProductComponent implements OnInit {
  private http = inject(HttpClient);
  readonly i18n = inject(StorefrontI18n);
  readonly cart = inject(StorefrontCartService);

  /** The product slug to load, set by the shell when navigating here. */
  readonly slug = input.required<string>();
  /** The loaded product detail, or null while loading / not found. */
  readonly product = signal<ProductDetail | null>(null);
  readonly selectedVariantId = signal('');
  readonly quantity = signal(1);
  readonly addingToCart = signal(false);
  readonly notice = signal('');
  readonly quantityWarning = signal('');
  /** Requests the shell to return to the shop catalog. */
  readonly backToShop = output<void>();
  /** Requests the shell to open the cart after adding an item. */
  readonly goToCart = output<void>();
  /** Reports loaded product data so the shell can update document metadata. */
  readonly productLoaded = output<ProductDetail>();
  /** Reports an unavailable product so the shell can prevent indexing the URL. */
  readonly productMissing = output<void>();

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
  initialOf(name?: string): string {
    return (name || '?').trim().charAt(0).toUpperCase() || '?';
  }

  /** The product photos for the gallery, or an empty list to show the placeholder. */
  galleryItems(): OpenShopfrontGalleryItem[] {
    const item = this.product();
    if (!item) return [];
    const images = item.imageUrls?.length ? item.imageUrls : item.imageUrl ? [item.imageUrl] : [];
    return images.map((url, index) => ({ id: `${index}-${url}`, url, title: item.name }));
  }
  money(cents: number, currency = 'EUR') {
    try {
      return (cents / 100).toLocaleString(this.i18n.locale(), { style: 'currency', currency });
    } catch {
      return `${(cents / 100).toFixed(2)} ${currency}`;
    }
  }

  private async load() {
    try {
      // The API nests the product under "product"; flatten it into the component's shape.
      const detail = await firstValueFrom(
        this.http.get<{ product: Product; variants: VariantOption[] }>(
          `/api/v1/public/products/${encodeURIComponent(this.slug())}`,
        ),
      );
      const flat: ProductDetail = { ...detail.product, variants: detail.variants };
      this.product.set(flat);
      this.productLoaded.emit(flat);
      if (flat.variants.length === 1) this.selectVariant(flat.variants[0]);
      else this.selectedVariantId.set('');
    } catch {
      // The product may have been hidden; show the not-found state.
      this.productMissing.emit();
    }
  }

  private selectedVariant(): VariantOption | undefined {
    return this.product()?.variants.find((option) => option.id === this.selectedVariantId());
  }

  selectVariant(option: VariantOption) {
    this.selectedVariantId.set(option.id);
    const previous = this.cart.quantityFor(option.id);
    const adjusted = this.cart.reconcileStock(option.id, option.availableQuantity);
    this.quantity.set(adjusted || 1);
    this.notice.set('');
    this.quantityWarning.set(
      previous > option.availableQuantity
        ? this.t('product.stockAdjusted', { count: option.availableQuantity })
        : '',
    );
  }

  onQuantityChange(event: Event) {
    const input = event.target as HTMLInputElement;
    const value = Number(input.value);
    if (Number.isNaN(value)) return;
    const maximum = this.selectedVariant()?.availableQuantity ?? 99;
    const adjusted = Math.max(1, Math.min(maximum, Math.round(value)));
    this.quantity.set(adjusted);
    input.value = String(adjusted);
    this.quantityWarning.set(
      value > maximum ? this.t('product.stockLimit', { count: maximum }) : '',
    );
    this.notice.set('');
  }

  quantityInCart(): number {
    return this.cart.quantityFor(this.selectedVariantId());
  }

  addToCart() {
    const product = this.product();
    const variant = this.selectedVariant();
    if (!product || !variant) return;
    this.addingToCart.set(true);
    try {
      const wasInCart = this.cart.quantityFor(variant.id) > 0;
      const adjusted = this.cart.set(
        variant.id,
        product.name,
        variant.label ?? undefined,
        variant.priceCents,
        this.quantity(),
        variant.availableQuantity,
      );
      this.quantity.set(adjusted || 1);
      this.quantityWarning.set('');
      this.notice.set(this.t(wasInCart ? 'cart.updatedNotice' : 'cart.addedNotice'));
    } finally {
      this.addingToCart.set(false);
    }
  }
}
