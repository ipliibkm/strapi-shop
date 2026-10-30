import { Component, inject, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import {
  OpenShopfrontContentBlocksComponent,
  OpenShopfrontProductListComponent,
  OpenShopfrontProductListItem,
} from '@openshopfront/ui';
import { StorefrontI18n } from '../shared/i18n.service';
import { StorefrontShopService } from '../shared/shop.service';
import { StorefrontFormComponent } from '../shared/storefront-form.component';
import { Page, Product, PublishedPage } from '../shared/types';

/** The shop page: published CMS intro (or static hero) plus the public product grid. */
@Component({
  selector: 'ip-shop',
  standalone: true,
  imports: [
    MatButtonModule,
    MatIconModule,
    OpenShopfrontContentBlocksComponent,
    OpenShopfrontProductListComponent,
    StorefrontFormComponent,
  ],
  templateUrl: './shop.component.html',
})
export class ShopComponent {
  readonly i18n = inject(StorefrontI18n);
  readonly shop = inject(StorefrontShopService);

  /** The published shop content, or null when not published (shows the static hero). */
  readonly content = input<PublishedPage | null>(null);
  /** Products for "products" blocks (already limited by the shell). */
  readonly products = input<Product[]>([]);

  /** A CTA block target was clicked; the shell routes to it. */
  readonly navigate = output<Page>();
  /** A product card was clicked; the shell opens the product page. */
  readonly open = output<Product>();

  langVersion(): string {
    return this.i18n.lang();
  }
  t(key: string, params?: Record<string, string | number>): string {
    return this.i18n.t(key, params);
  }

  /** Maps the catalog to the product-list renderer's shape with a formatted price. */
  listProducts(): OpenShopfrontProductListItem[] {
    return this.shop.products().map((item) => ({
      id: item.id,
      name: item.name,
      description: item.description,
      url: item.imageUrl || undefined,
      priceLabel: this.minPrice(item),
    }));
  }

  listLabels() {
    return { empty: this.t('shop.empty') };
  }

  onOpen(item: OpenShopfrontProductListItem) {
    const catalog = this.shop.products().find((p) => p.id === item.id);
    if (catalog) this.open.emit(catalog);
  }

  minPrice(product: Product): string {
    // Without a variant price on the list endpoint, show a neutral label.
    if (!product.variantCount) return this.t('price.ask');
    return this.t('price.fromShop');
  }

  /** Maps the app's products to the renderer's shape with a formatted price. */
  blockProducts() {
    return this.products().map((item) => ({
      id: item.id,
      name: item.name,
      url: item.imageUrl || undefined,
      priceLabel: this.minPrice(item),
    }));
  }

  blockLabels() {
    return { continue: this.t('block.continue'), productsNone: this.t('products.none') };
  }

  onNavigate(target?: string) {
    const allowed: Page[] = ['booking', 'shop', 'services', 'about', 'contact', 'home'];
    if (target && (allowed as string[]).includes(target)) this.navigate.emit(target as Page);
  }
}
