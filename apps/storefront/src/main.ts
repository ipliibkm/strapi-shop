import { bootstrapApplication } from '@angular/platform-browser';
import { Component, effect, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule, MatIconRegistry } from '@angular/material/icon';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { OpenShopfrontThemeService, parseCustomPalette } from '@openshopfront/ui';
import { HttpClient, provideHttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { LANGUAGES, Lang } from './shared/i18n';
import { StorefrontI18n } from './shared/i18n.service';
import { StorefrontCartService } from './shared/cart.service';
import { StorefrontShopService } from './shared/shop.service';
import { StorefrontSeoService } from './shared/seo.service';
import { BookingComponent } from './booking/booking.component';
import { ShopComponent } from './shop/shop.component';
import { ProductComponent } from './product/product.component';
import { CartComponent } from './cart/cart.component';
import { OrderComponent } from './order/order.component';
import { AboutComponent } from './about/about.component';
import { HomeComponent } from './home/home.component';
import { ServicesComponent } from './services/services.component';
import { MyShopComponent } from './my-shop/my-shop.component';
import { CustomerAccountService } from './shared/customer-account.service';
import {
  Page,
  Product,
  ProductDetail,
  PublicPageSummary,
  PublishedPage,
  StorefrontLanguage,
} from './shared/types';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    MatButtonModule,
    MatIconModule,
    MatButtonToggleModule,
    HomeComponent,
    ServicesComponent,
    BookingComponent,
    ShopComponent,
    ProductComponent,
    CartComponent,
    OrderComponent,
    AboutComponent,
    MyShopComponent,
  ],
  templateUrl: './main.html',
})
class StorefrontApp {
  readonly theme = inject(OpenShopfrontThemeService);
  private readonly http = inject(HttpClient);
  readonly i18n = inject(StorefrontI18n);
  readonly cart = inject(StorefrontCartService);
  readonly shop = inject(StorefrontShopService);
  readonly account = inject(CustomerAccountService);
  private readonly seo = inject(StorefrontSeoService);
  readonly page = signal<Page>(this.initialPage());
  readonly previewMode = new URLSearchParams(window.location.search).get('preview') === '1';
  /** Website languages published by the administrator, in display order. */
  readonly enabledLanguages = signal<StorefrontLanguage[]>([]);

  t(key: string, params?: Record<string, string | number>): string {
    return this.i18n.t(key, params);
  }
  langVersion(): Lang {
    return this.i18n.lang();
  }

  enabledLanguageOptions(): StorefrontLanguage[] {
    return this.enabledLanguages().filter((item) => item.enabled);
  }

  setLang(code: string) {
    const lang = (Object.keys(LANGUAGES) as Lang[]).includes(code as Lang) ? (code as Lang) : 'en';
    if (!this.enabledLanguageOptions().some((item) => item.code === lang)) return;
    this.i18n.lang.set(lang);
    try {
      localStorage.setItem('openshopfront-lang', lang);
    } catch {
      // Storage may be unavailable; the in-memory language still works.
    }
    document.documentElement.lang = lang;
    this.updateSeo();
  }

  /** Loads published languages and palette, then resolves this visitor's language. */
  private async loadSettings() {
    try {
      const settings = await firstValueFrom(
        this.http.get<{
          languages: StorefrontLanguage[];
          theme: string;
          customPalette?: string | null;
        }>('/api/v1/public/settings'),
      );
      this.enabledLanguages.set(settings.languages ?? []);
      if (!this.previewMode) {
        const palette = parseCustomPalette(settings.customPalette ?? null);
        // A published custom palette wins over the preset when one is set.
        if (palette) this.theme.applyCustom(palette);
        else this.theme.setTheme(settings.theme);
      }
    } catch {
      // Keep the default language and palette when settings are unavailable.
    }
    const enabled = this.enabledLanguageOptions().map((item) => item.code as Lang);
    const saved = ((): string | null => {
      try {
        return localStorage.getItem('openshopfront-lang');
      } catch {
        return null;
      }
    })();
    let resolved: Lang = 'en';
    if (saved && enabled.includes(saved as Lang)) resolved = saved as Lang;
    else {
      const browser = navigator.language.slice(0, 2).toLowerCase();
      if (enabled.includes(browser as Lang)) resolved = browser as Lang;
      else if (enabled.length) resolved = enabled[0];
    }
    this.setLang(resolved);
  }
  readonly pageAvailable = signal(true);
  readonly enabledPageSlugs = signal<Set<string> | null>(null);
  readonly menuOpen = signal(false);
  readonly year = new Date().getFullYear();

  // Dynamic page state
  readonly pageLoaded = signal(false);
  readonly pageData = signal<PublishedPage | null>(null);
  readonly productData = signal<ProductDetail | null>(null);

  readonly nav: { key: Page; label: string }[] = [
    { key: 'home', label: 'Home' },
    { key: 'services', label: 'Services' },
    { key: 'booking', label: 'Book' },
    { key: 'shop', label: 'Shop' },
    { key: 'about', label: 'About' },
    { key: 'contact', label: 'Contact' },
  ];

  constructor() {
    effect(() => {
      if (this.page() === 'my-shop') this.pageAvailable.set(this.account.enabled());
    });
    inject(MatIconRegistry).setDefaultFontSetClass('material-symbols-outlined');
    this.theme.load('storefront');
    window.addEventListener('message', (event) => this.receivePreview(event));
    window.addEventListener('popstate', () => this.restorePageFromHistory());
    if (this.previewMode) {
      window.parent.postMessage({ type: 'openshopfront:storefront-preview-ready' }, '*');
    }
    void this.loadSettings();
    if (!this.previewMode) void this.loadEnabledPages();
    const initial = this.initialPage();
    this.normalizeLegacyUrl(initial);
    this.updateSeo();
    if (!this.previewMode && this.contentPages.includes(initial)) void this.loadPage(initial);
    if (initial === 'shop' || initial === 'product') void this.shop.load();
  }

  /** Pages rendered from the published page builder content. */
  readonly contentPages: Page[] = ['home', 'services', 'booking', 'shop', 'about', 'contact'];

  navigate(page: Page, params?: { slug?: string }, event?: Event) {
    event?.preventDefault();
    if (!this.previewMode && this.isManagedPage(page) && !this.pageIsEnabled(page)) {
      this.pageAvailable.set(false);
      return;
    }
    if (page === 'my-shop' && !this.account.enabled()) {
      this.pageAvailable.set(false);
      return;
    }
    this.page.set(page);
    this.pageAvailable.set(page !== 'my-shop' || this.account.enabled());
    const path = this.pagePath(page, params);
    const search = this.previewMode ? window.location.search : '';
    window.history.pushState({}, '', `${path}${search}`);
    this.menuOpen.set(false);
    window.scrollTo({ top: 0, behavior: 'smooth' });
    if (this.contentPages.includes(page)) void this.loadPage(page);
    if (page === 'shop' || page === 'product') void this.shop.load();
    if (page === 'product') this.productData.set(null);
    this.updateSeo();
  }

  /** Restores the page when the customer uses the browser Back or Forward button. */
  private restorePageFromHistory() {
    const page = this.initialPage();
    this.page.set(page);
    this.pageAvailable.set(page !== 'my-shop' || this.account.enabled());
    this.menuOpen.set(false);
    if (this.contentPages.includes(page)) void this.loadPage(page);
    if (page === 'shop' || page === 'product') void this.shop.load();
    if (page === 'product') this.productData.set(null);
    this.updateSeo();
  }

  /** Returns the loaded published page when it matches the requested slug. */
  contentFor(slug: string): PublishedPage | null {
    const current = this.pageData();
    return current && current.slug === slug ? current : null;
  }

  visibleNav(): { key: Page; label: string }[] {
    if (this.previewMode || this.enabledPageSlugs() === null) return this.nav;
    return this.nav.filter((item) => this.pageIsEnabled(item.key));
  }

  pageIsEnabled(page: Page): boolean {
    const enabled = this.enabledPageSlugs();
    return this.previewMode || enabled === null || enabled.has(page);
  }

  private async loadEnabledPages() {
    try {
      const pages = await firstValueFrom(
        this.http.get<PublicPageSummary[]>('/api/v1/public/pages'),
      );
      const enabled = new Set((pages ?? []).map((item) => item.slug));
      this.enabledPageSlugs.set(enabled);
      if (!this.previewMode && this.isManagedPage(this.page()) && !enabled.has(this.page())) {
        this.pageAvailable.set(false);
      }
      this.updateSeo();
    } catch {
      // Keep the default navigation if the page list is temporarily unavailable.
    }
  }

  private receivePreview(event: MessageEvent) {
    if (!this.previewMode || event.source !== window.parent) return;
    const message = event.data as { type?: string; page?: PublishedPage } | null;
    if (message?.type !== 'openshopfront:storefront-preview' || !message.page) return;
    if (!this.isManagedPage(message.page.slug)) return;
    const blocks = Array.isArray(message.page.blocks) ? message.page.blocks : [];
    this.page.set(message.page.slug as Page);
    this.pageData.set({ ...message.page, blocks });
    this.pageLoaded.set(true);
    this.pageAvailable.set(true);
    if (message.page.slug === 'shop') void this.shop.load();
    this.updateSeo();
  }

  private isManagedPage(page: string): boolean {
    return (this.contentPages as string[]).includes(page);
  }

  private async loadPage(slug: Page) {
    const key = slug as string;
    this.pageLoaded.set(false);
    this.pageData.set(null);
    try {
      const data = await firstValueFrom(
        this.http.get<PublishedPage>(`/api/v1/public/pages/${key}`),
      );
      this.pageData.set(data);
    } catch {
      // The page may not be published yet; fall back to the static hero.
    } finally {
      this.pageLoaded.set(true);
      if (this.page() === slug) this.updateSeo();
    }
  }

  /** The slug of a product or order hash, without fragment query parameters. */
  productSlug(): string | null {
    const pathParts = window.location.pathname.split('/').filter(Boolean);
    if (pathParts[0] === 'products' && pathParts[1])
      return decodeURIComponent(pathParts.slice(1).join('/'));
    if (pathParts[0] === 'order' && pathParts[1])
      return decodeURIComponent(pathParts.slice(1).join('/'));
    const value = window.location.hash.slice(1);
    const legacyPath = value.split('?', 1)[0];
    if (legacyPath.startsWith('product/')) return legacyPath.slice('product/'.length) || null;
    if (legacyPath.startsWith('order/')) return legacyPath.slice('order/'.length) || null;
    return null;
  }

  /** Returns the customer access token from the order fragment. */
  orderAccessToken(): string {
    const fragment = window.location.hash.slice(1);
    if (!fragment.startsWith('order/')) return new URLSearchParams(fragment).get('token') ?? '';
    const query = fragment.includes('?') ? fragment.slice(fragment.indexOf('?') + 1) : '';
    return new URLSearchParams(query).get('token') ?? '';
  }

  /** The first catalog products for "products" blocks and the about page. */
  blockProducts() {
    return this.shop.first(6);
  }

  /** Maps a product to the content-blocks renderer's shape with a formatted price. */
  blockProductView(item: Product) {
    return {
      id: item.id,
      name: item.name,
      priceLabel: this.minPrice(item),
      href: `/products/${encodeURIComponent(item.slug)}`,
    };
  }

  /** Translated labels for the content-blocks renderer. */
  blockLabels() {
    return { continue: this.t('block.continue'), productsNone: this.t('products.none') };
  }

  private onBlockNavigate(target?: string) {
    if (!target) return;
    const allowed: Page[] = ['booking', 'shop', 'services', 'about', 'contact', 'home'];
    if ((allowed as string[]).includes(target)) this.navigate(target as Page);
  }

  async openProduct(item: { id: string; name: string }) {
    // The product component loads its own detail by slug; route to it.
    const catalog = this.shop.products().find((p) => p.id === item.id);
    if (catalog) this.navigate('product', { slug: catalog.slug });
  }

  minPrice(product: { variantCount?: number }): string {
    // Without a variant price on the list endpoint, show a neutral label.
    if (!product.variantCount) return this.t('price.ask');
    return this.t('price.fromShop');
  }

  onProductLoaded(product: ProductDetail): void {
    this.productData.set(product);
    this.pageAvailable.set(true);
    this.updateSeo();
  }

  onProductMissing(): void {
    this.productData.set(null);
    this.pageAvailable.set(false);
    this.updateSeo();
  }

  /** Returns the public, crawlable URL for one storefront page. */
  pagePath(page: Page, params?: { slug?: string }): string {
    if (page === 'home') return '/';
    if (page === 'product')
      return params?.slug ? `/products/${encodeURIComponent(params.slug)}` : '/shop';
    if (page === 'order')
      return params?.slug ? `/order/${encodeURIComponent(params.slug)}` : '/order';
    return `/${page}`;
  }

  private initialPage(): Page {
    const path = window.location.pathname.split('/').filter(Boolean);
    if (path[0] === 'products') return 'product';
    if (path[0] === 'order') return 'order';
    if (['services', 'booking', 'shop', 'cart', 'about', 'contact', 'my-shop'].includes(path[0]))
      return path[0] as Page;

    const legacy = window.location.hash.slice(1).split('?', 1)[0];
    if (legacy.startsWith('product/')) return 'product';
    if (legacy.startsWith('order/')) return 'order';
    return ['services', 'booking', 'shop', 'cart', 'about', 'contact', 'my-shop'].includes(legacy)
      ? (legacy as Page)
      : 'home';
  }

  private normalizeLegacyUrl(page: Page): void {
    if (!window.location.hash || this.previewMode) return;
    const slug = this.productSlug();
    const path = this.pagePath(page, slug ? { slug } : undefined);
    const token = page === 'order' ? this.orderAccessToken() : '';
    window.history.replaceState(
      {},
      '',
      `${path}${token ? `#token=${encodeURIComponent(token)}` : ''}`,
    );
  }

  private updateSeo(): void {
    const page = this.page();
    const slug = this.productSlug();
    const path = this.pagePath(page, slug ? { slug } : undefined);
    if (page === 'product') {
      const product = this.productData();
      if (product && product.slug === slug) {
        this.seo.setProduct(product, path);
        return;
      }
      this.seo.setPage({
        title: this.t('nav.shop'),
        description: this.t('shop.text'),
        path,
        index: !this.previewMode && this.pageAvailable(),
      });
      return;
    }

    const content = this.contentFor(page);
    const title = content?.title || this.t(`nav.${page}`);
    this.seo.setPage({
      title,
      description: this.pageDescription(page, content),
      path,
      index:
        !this.previewMode &&
        this.pageAvailable() &&
        page !== 'cart' &&
        page !== 'order' &&
        page !== 'my-shop',
    });
  }

  private pageDescription(page: Page, content: PublishedPage | null): string {
    if (content) {
      for (const block of content.blocks) {
        const value = block.html || block.caption || block.title;
        if (value) {
          const element = document.createElement('div');
          element.innerHTML = value;
          const text = (element.textContent ?? '').replace(/\s+/g, ' ').trim();
          if (text) return text;
        }
      }
    }
    const fallback: Partial<Record<Page, string>> = {
      home: this.t('home.hero.text'),
      services: this.t('servicesPage.text'),
      booking: this.t('booking.step1.servicesText'),
      shop: this.t('shop.text'),
      about: this.t('footer.tagline'),
      contact: this.t('footer.tagline'),
      cart: this.t('cart.titleA') + this.t('cart.accent'),
      order: this.t('order.placed.text'),
      'my-shop': this.t('account.help'),
    };
    return fallback[page] || titleForDescription(page);
  }
}

function titleForDescription(page: Page): string {
  return `STUDIO ${page} page.`;
}

bootstrapApplication(StorefrontApp, { providers: [provideHttpClient()] }).catch(console.error);
