import { Component, inject, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { OpenShopfrontContentBlocksComponent } from '@openshopfront/ui';
import { StorefrontI18n } from '../shared/i18n.service';
import { SERVICE_CARDS } from '../shared/service-cards';
import { StorefrontFormComponent } from '../shared/storefront-form.component';
import { Page, Product, PublishedPage } from '../shared/types';

/** The home page: published CMS content or the static hero/services/statement fallback. */
@Component({
  selector: 'ip-home',
  standalone: true,
  imports: [
    MatButtonModule,
    MatIconModule,
    OpenShopfrontContentBlocksComponent,
    StorefrontFormComponent,
  ],
  templateUrl: './home.component.html',
})
export class HomeComponent {
  readonly i18n = inject(StorefrontI18n);
  /** The static service cards shown in the fallback layout. */
  readonly serviceCards = SERVICE_CARDS;

  /** Whether the page content has finished loading. */
  readonly loaded = input(false);
  /** The published home content, or null when not published (shows the fallback). */
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

  /** Maps the app's products to the renderer's shape with a formatted price. */
  blockProducts() {
    return this.products().map((item) => ({
      id: item.id,
      name: item.name,
      priceLabel: this.minPrice(item),
      href: `/products/${encodeURIComponent(item.slug)}`,
    }));
  }

  blockLabels() {
    return { continue: this.t('block.continue'), productsNone: this.t('products.none') };
  }

  minPrice(product: Product): string {
    if (!product.variantCount) return this.t('price.ask');
    return this.t('price.fromShop');
  }

  onNavigate(target?: string) {
    const allowed: Page[] = ['booking', 'shop', 'services', 'about', 'contact', 'home'];
    if (target && (allowed as string[]).includes(target)) this.navigate.emit(target as Page);
  }
}
