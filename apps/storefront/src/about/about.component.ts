import { Component, inject, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { OpenShopfrontContentBlocksComponent } from '@openshopfront/ui';
import { StorefrontI18n } from '../shared/i18n.service';
import { StorefrontFormComponent } from '../shared/storefront-form.component';
import { Page, Product, PublishedPage } from '../shared/types';

/** The about and contact pages: a CMS hero plus published content blocks. */
@Component({
  selector: 'ip-about',
  standalone: true,
  imports: [MatButtonModule, OpenShopfrontContentBlocksComponent, StorefrontFormComponent],
  templateUrl: './about.component.html',
})
export class AboutComponent {
  readonly i18n = inject(StorefrontI18n);

  /** Which of the two pages this instance renders. */
  readonly page = input.required<Page>();
  /** Whether the page content has finished loading. */
  readonly loaded = input(false);
  /** The published page content, or null when not published. */
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

  minPrice(product: { variantCount?: number }): string {
    if (!product.variantCount) return this.t('price.ask');
    return this.t('price.fromShop');
  }

  onNavigate(target?: string) {
    const allowed: Page[] = ['booking', 'shop', 'services', 'about', 'contact', 'home'];
    if (target && (allowed as string[]).includes(target)) this.navigate.emit(target as Page);
  }
}
