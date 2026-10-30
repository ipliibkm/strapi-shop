import { HttpClient } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import {
  OpenShopfrontGalleryComponent,
  OpenShopfrontGalleryItem,
  OpenShopfrontImageUploadComponent,
  OpenShopfrontImageUploadError,
  OpenShopfrontImageUploadLabels,
} from '@openshopfront/ui';
import { localeOf, Lang } from '../shared/i18n';
import { AdminI18n } from '../shared/i18n.service';

interface Product {
  id: string;
  slug: string;
  name: string;
  description: string;
  imageUrl?: string;
  imageUrls?: string[];
  variantCount: number;
}
interface OrderRow {
  id: string;
  orderNumber: string;
  status: string;
  customerName: string;
  customerEmail?: string | null;
  totalCents: number;
  createdAt?: string;
}
interface OrderItem {
  productName: string;
  variantLabel: string | null;
  quantity: number;
  unitPriceCents: number;
  lineTotalCents: number;
}
interface PaymentState {
  provider: string;
  externalId: string | null;
  amountCents: number;
  status: string;
}
interface ShipmentState {
  carrier?: string | null;
  trackingNumber?: string | null;
  status?: string | null;
  shippedAt?: string | null;
}
interface OrderDetail {
  order: OrderRow & {
    currency: string;
    subtotalCents: number;
    taxCents: number;
    shippingCents: number;
  };
  items: OrderItem[];
  payment?: PaymentState | null;
  shipment?: ShipmentState | null;
}
interface VariantInput {
  sku: string;
  label: string;
  priceCents: number;
  stockQuantity: number;
}
interface VariantAdmin {
  id: string;
  sku: string;
  label?: string | null;
  priceCents: number;
  stockQuantity: number;
}
interface Settings {
  shopEnabled: boolean;
  taxRateBps: number;
  shippingFlatCents: number;
  freeShippingThresholdCents?: number | null;
}

@Component({
  selector: 'ip-commerce',
  standalone: true,
  imports: [
    MatButtonModule,
    MatIconModule,
    OpenShopfrontGalleryComponent,
    OpenShopfrontImageUploadComponent,
  ],
  templateUrl: './commerce.component.html',
})
class CommerceComponent {
  private readonly http = inject(HttpClient);
  readonly i18n = inject(AdminI18n);
  /** Reads the active language so templates track it and re-render on change. */
  langVersion(): Lang {
    return this.i18n.lang();
  }
  /** Translates one interface string for the active admin language. */
  t(text: string, params?: Record<string, string | number>): string {
    return this.i18n.t(text, params);
  }
  readonly loaded = signal(false);
  readonly tab = signal<'orders' | 'products' | 'settings'>('orders');
  readonly stripeConfigured = signal(false);
  readonly orders = signal<OrderRow[]>([]);
  readonly filter = signal('ALL');
  readonly selectedOrder = signal<OrderDetail | null>(null);
  readonly products = signal<Product[]>([]);
  readonly selectedProduct = signal<Product | null>(null);
  readonly editingNew = signal(true);
  readonly productName = signal('');
  readonly productDescription = signal('');
  readonly productImageUrls = signal<string[]>([]);
  readonly imageUploading = signal(false);
  readonly imageUploadError = signal('');
  readonly previewMode = signal(false);
  readonly variants = signal<VariantInput[]>([]);
  readonly settings = signal<Settings>({
    shopEnabled: true,
    taxRateBps: 0,
    shippingFlatCents: 499,
  });
  readonly actionBusy = signal(false);
  readonly notice = signal('');
  /** Re-evaluated on every access so labels follow the active language. */
  get filters() {
    return [
      { value: 'ALL', label: this.t('All statuses') },
      { value: 'PENDING_PAYMENT', label: this.t('Pending payment') },
      { value: 'PAID', label: this.t('Paid') },
      { value: 'FULFILLED', label: this.t('Fulfilled') },
      { value: 'CANCELLED', label: this.t('Cancelled') },
    ];
  }

  constructor() {
    void this.refresh();
  }

  private async refresh() {
    try {
      const [status, settings, products] = await Promise.all([
        firstValueFrom(
          this.http.get<{ stripeConfigured: boolean }>('/api/v1/admin/commerce/status'),
        ),
        firstValueFrom(this.http.get<Settings>('/api/v1/admin/commerce/settings')),
        firstValueFrom(this.http.get<Product[]>('/api/v1/admin/commerce/products')),
      ]);
      this.stripeConfigured.set(Boolean(status.stripeConfigured));
      this.settings.set(settings);
      this.products.set(products);
      await this.loadOrders();
    } catch {
      // The backend may still be starting; the UI stays usable.
    } finally {
      this.loaded.set(true);
    }
  }

  private setTab(value: 'orders' | 'products' | 'settings') {
    this.tab.set(value);
    this.notice.set('');
  }

  money(cents: number) {
    return (cents / 100).toLocaleString(localeOf(this.i18n.lang()), {
      style: 'currency',
      currency: 'EUR',
    });
  }

  imageUploadLabels(): OpenShopfrontImageUploadLabels {
    return {
      title: this.t('Product images'),
      help: this.t('The first image is the main image. Upload up to 8 JPG, PNG, or WebP images.'),
      choose: this.t('Choose images or drop them here'),
      remove: this.t('Remove image'),
      uploading: this.t('Uploading images…'),
      limitReached: this.t('Image limit reached.'),
    };
  }

  shortStatus(status?: string | null) {
    if (!status) return '—';
    const map: Record<string, string> = {
      PENDING_PAYMENT: this.t('PENDING'),
      PAID: this.t('PAID'),
      FULFILLED: this.t('SHIPPED'),
      CANCELLED: this.t('CLOSED'),
      SUCCEEDED: this.t('SUCCEEDED'),
      FAILED: this.t('FAILED'),
      REFUNDED: this.t('REFUNDED'),
    };
    return map[status] ?? status;
  }

  statusClass(status?: string | null) {
    if (!status) return '';
    const good = ['PAID', 'FULFILLED', 'SUCCEEDED'];
    const bad = ['CANCELLED', 'FAILED', 'REFUNDED'];
    return good.includes(status) ? 'live' : bad.includes(status) ? '' : '';
  }

  // ---------- Orders ----------

  private async loadOrders() {
    try {
      const list = await firstValueFrom(
        this.http.get<OrderRow[]>('/api/v1/admin/commerce/orders', {
          params: this.filter() === 'ALL' ? {} : { statusFilter: this.filter(), limit: 50 },
        }),
      );
      this.orders.set(list);
    } catch {
      this.orders.set([]);
    }
  }

  private setFilter(event: Event) {
    this.filter.set((event.target as HTMLSelectElement).value);
    void this.loadOrders();
  }

  async openOrder(row: OrderRow) {
    try {
      const detail = await firstValueFrom(
        this.http.get<OrderDetail>(`/api/v1/admin/commerce/orders/${row.id}`),
      );
      this.selectedOrder.set(detail);
    } catch {
      this.notice.set(this.t('This order could not be loaded.'));
    }
  }

  private async withAction(promise: Promise<unknown>) {
    this.actionBusy.set(true);
    this.notice.set('');
    try {
      await promise;
      return true;
    } catch (error) {
      const message = (error as { error?: { message?: string } })?.error?.message;
      this.notice.set(this.t(message || 'This action could not be completed.'));
      return false;
    } finally {
      this.actionBusy.set(false);
    }
  }

  async markPaid(detail: OrderDetail) {
    const ok = await this.withAction(
      firstValueFrom(
        this.http.post(`/api/v1/admin/commerce/orders/${detail.order.id}/mark-paid`, null),
      ),
    );
    if (ok) {
      this.notice.set(this.t('The order was marked paid.'));
      void this.openOrder(detail.order);
      void this.loadOrders();
    }
  }

  async cancelOrder(detail: OrderDetail) {
    const ok = await this.withAction(
      firstValueFrom(this.http.delete(`/api/v1/admin/commerce/orders/${detail.order.id}`)),
    );
    if (ok) {
      this.notice.set(this.t('The order was cancelled and its stock hold released.'));
      void this.openOrder(detail.order);
      void this.loadOrders();
    }
  }

  async fulfill(detail: OrderDetail, carrier: string, tracking: string) {
    const ok = await this.withAction(
      firstValueFrom(
        this.http.post(`/api/v1/admin/commerce/orders/${detail.order.id}/fulfill`, {
          carrier,
          trackingNumber: tracking,
        }),
      ),
    );
    if (ok) {
      this.notice.set(this.t('The order was fulfilled and stock deducted.'));
      void this.openOrder(detail.order);
      void this.loadOrders();
    }
  }

  async refundOrder(detail: OrderDetail) {
    const ok = await this.withAction(
      firstValueFrom(
        this.http.post(`/api/v1/admin/commerce/orders/${detail.order.id}/refund`, null),
      ),
    );
    if (ok) {
      this.notice.set(this.t('The order was refunded and shipped items restocked.'));
      void this.openOrder(detail.order);
      void this.loadOrders();
    }
  }

  // ---------- Products ----------

  private selectProduct(item: Product) {
    this.editingNew.set(false);
    this.selectedProduct.set(item);
    this.productName.set(item.name);
    this.productDescription.set(item.description);
    this.productImageUrls.set(
      item.imageUrls?.length ? item.imageUrls : item.imageUrl ? [item.imageUrl] : [],
    );
    this.imageUploadError.set('');
    this.previewMode.set(false);
    this.variants.set([]);
    if (item.variantCount > 0) {
      this.notice.set(this.t('Load the product to edit its options.'));
      void this.loadVariants(item.id).then((variants) => this.variants.set(variants));
    }
  }

  private async loadVariants(productId: string): Promise<VariantInput[]> {
    try {
      const detail = await firstValueFrom(
        this.http.get<{ product: Product; variants: VariantAdmin[] }>(
          `/api/v1/admin/commerce/products/${productId}`,
        ),
      );
      return detail.variants.map((variant) => ({
        sku: variant.sku,
        label: variant.label || '',
        priceCents: variant.priceCents,
        stockQuantity: variant.stockQuantity,
      }));
    } catch {
      return [];
    }
  }

  private addVariant(sku: string, label: string, price: string, stock: string) {
    const priceCents = Math.round(parseFloat(price || '0') * 100);
    if (!sku.trim() || Number.isNaN(priceCents)) return;
    this.variants.set([
      ...this.variants(),
      {
        sku: sku.trim(),
        label: (label || '').trim(),
        priceCents,
        stockQuantity: Math.max(0, parseInt(stock, 10) || 0),
      },
    ]);
  }

  private removeVariant(index: number) {
    const next = [...this.variants()];
    next.splice(index, 1);
    this.variants.set(next);
  }

  private updateVariant(index: number, field: keyof VariantInput, value: string) {
    const next = [...this.variants()];
    const variant = next[index];
    if (!variant) return;
    next[index] = {
      ...variant,
      [field]:
        field === 'priceCents'
          ? Math.round((parseFloat(value) || 0) * 100)
          : field === 'stockQuantity'
            ? Math.max(0, parseInt(value, 10) || 0)
            : value,
    };
    this.variants.set(next);
  }

  async uploadProductImages(files: File[]) {
    this.imageUploading.set(true);
    this.imageUploadError.set('');
    const uploaded: string[] = [];
    for (const file of files) {
      try {
        const body = new FormData();
        body.append('file', file);
        const result = await firstValueFrom(
          this.http.post<{ url: string }>('/api/v1/admin/commerce/product-images', body),
        );
        uploaded.push(result.url);
      } catch (error) {
        this.imageUploadError.set(this.imageUploadErrorMessage(error, file.name));
        break;
      }
    }
    if (uploaded.length) {
      this.productImageUrls.set([...this.productImageUrls(), ...uploaded].slice(0, 8));
    }
    this.imageUploading.set(false);
  }

  imageSelectionRejected(error: OpenShopfrontImageUploadError) {
    const fileName = error.fileName || this.t('The selected file');
    const message =
      error.reason === 'file-too-large'
        ? this.t('The image "{name}" is larger than 8 MB. Select a smaller image.', {
            name: fileName,
          })
        : error.reason === 'unsupported-type'
          ? this.t('The file "{name}" is not a supported image. Select a JPG, PNG, or WebP file.', {
              name: fileName,
            })
          : this.t('You can add up to 8 images. Remove an image before you add another one.');
    this.imageUploadError.set(message);
  }

  private imageUploadErrorMessage(error: unknown, fileName: string): string {
    const response = error as { status?: number; error?: { message?: string } | string };
    const serverMessage =
      typeof response.error === 'object' && response.error ? response.error.message : undefined;
    if (response.status === 413)
      return this.t('The image "{name}" is larger than 8 MB. Select a smaller image.', {
        name: fileName,
      });
    if (response.status === 415)
      return this.t(
        'The file "{name}" is not a supported image. Select a JPG, PNG, or WebP file.',
        {
          name: fileName,
        },
      );
    if (response.status === 401)
      return this.t('Your session has expired. Sign in again, then upload the image.');
    if (response.status === 403)
      return this.t('The server rejected the upload. Reload the page, sign in again, and retry.');
    if (response.status === 0)
      return this.t('The upload server did not respond. Check your connection and retry.');
    if (response.status === 400 && serverMessage) return this.t(serverMessage);
    if (response.status && response.status >= 500)
      return this.t('The server could not store the image. Retry later.');
    return this.t('The image "{name}" could not be uploaded. Retry the upload.', {
      name: fileName,
    });
  }

  private removeProductImage(index: number) {
    this.productImageUrls.update((images) =>
      images.filter((_, imageIndex) => imageIndex !== index),
    );
    this.imageUploadError.set('');
  }

  previewImages(): OpenShopfrontGalleryItem[] {
    return this.productImageUrls().map((url, index) => ({
      id: `${index}-${url}`,
      url,
      title: this.productName() || this.t('Product preview'),
    }));
  }

  async createProduct(
    slug: string,
    name: string,
    description: string,
    sku: string,
    label: string,
    price: string,
    stock: string,
  ) {
    const ok = await this.withAction(
      firstValueFrom(
        this.http.post<Product>('/api/v1/admin/commerce/products', {
          slug: slug.trim(),
          name,
          description,
          imageUrls: this.productImageUrls(),
          variant: {
            sku,
            label,
            priceCents: Math.round(parseFloat(price || '0') * 100),
            stockQuantity: Math.max(0, parseInt(stock, 10) || 0),
          },
        }),
      ),
    );
    if (ok) {
      this.notice.set(this.t('The product was created.'));
      await this.refresh();
      this.editingNew.set(true);
      this.selectedProduct.set(null);
      this.productImageUrls.set([]);
      this.imageUploadError.set('');
    }
  }

  async saveProduct(product: Product) {
    const ok = await this.withAction(
      firstValueFrom(
        this.http.put<Product>(`/api/v1/admin/commerce/products/${product.slug}`, {
          name: this.productName(),
          description: this.productDescription(),
          imageUrls: this.productImageUrls(),
          active: true,
          variants: this.variants(),
        }),
      ),
    );
    if (ok) {
      this.notice.set(this.t('The product was saved.'));
      await this.refresh();
    }
  }

  async deleteProduct(productId: string) {
    const ok = await this.withAction(
      firstValueFrom(this.http.delete(`/api/v1/admin/commerce/products/${productId}`)),
    );
    if (ok) {
      this.notice.set(this.t('The product was deleted.'));
      this.selectedProduct.set(null);
      this.editingNew.set(true);
      this.productImageUrls.set([]);
      this.imageUploadError.set('');
      this.previewMode.set(false);
      await this.refresh();
    }
  }

  // ---------- Settings ----------

  private setShopEnabled(event: Event) {
    const checked = (event.target as HTMLInputElement).checked;
    this.settings.set({ ...this.settings(), shopEnabled: checked });
  }

  async saveSettings(taxValue: string, flatValue: string, thresholdValue: string) {
    const taxRateBps = Math.round(parseFloat(taxValue || '0') * 100);
    const shippingFlatCents = Math.round(parseFloat(flatValue || '0') * 100);
    const freeShippingThresholdCents = thresholdValue
      ? Math.round(parseFloat(thresholdValue) * 100)
      : null;
    const ok = await this.withAction(
      firstValueFrom(
        this.http.put<Settings>('/api/v1/admin/commerce/settings', {
          shopEnabled: this.settings().shopEnabled,
          taxRateBps,
          shippingFlatCents,
          freeShippingThresholdCents,
        }),
      ),
    );
    if (ok) {
      this.notice.set(this.t('Settings saved.'));
      await this.refresh();
    }
  }
}

export { CommerceComponent };
