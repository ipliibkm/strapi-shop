import { DOCUMENT } from '@angular/common';
import { Injectable, inject } from '@angular/core';
import { Meta, Title } from '@angular/platform-browser';
import { ProductDetail } from './types';

interface PageMetadata {
  title: string;
  description: string;
  path: string;
  index?: boolean;
  image?: string;
  type?: 'website' | 'product';
  structuredData?: Record<string, unknown>;
}

/** Keeps search and social metadata synchronized with the client-side storefront route. */
@Injectable({ providedIn: 'root' })
export class StorefrontSeoService {
  private readonly document = inject(DOCUMENT);
  private readonly meta = inject(Meta);
  private readonly title = inject(Title);
  private readonly siteName = 'STUDIO';

  setPage(metadata: PageMetadata): void {
    const pageTitle =
      metadata.path === '/' ? this.siteName : `${metadata.title.trim()} | ${this.siteName}`;
    const description = this.clean(metadata.description).slice(0, 160);
    const canonicalUrl = this.absolute(metadata.path);
    const index = metadata.index !== false;

    this.title.setTitle(pageTitle);
    this.updateName('description', description);
    this.updateName(
      'robots',
      index ? 'index, follow, max-image-preview:large' : 'noindex, nofollow',
    );
    this.updateProperty('og:title', pageTitle);
    this.updateProperty('og:description', description);
    this.updateProperty('og:type', metadata.type ?? 'website');
    this.updateProperty('og:url', canonicalUrl);
    this.updateProperty('og:site_name', this.siteName);
    this.updateName('twitter:card', metadata.image ? 'summary_large_image' : 'summary');
    this.updateName('twitter:title', pageTitle);
    this.updateName('twitter:description', description);

    if (metadata.image) {
      const image = this.absolute(metadata.image);
      this.updateProperty('og:image', image);
      this.updateName('twitter:image', image);
    } else {
      this.meta.removeTag("property='og:image'");
      this.meta.removeTag("name='twitter:image'");
    }

    this.setCanonical(canonicalUrl);
    this.setStructuredData(
      metadata.structuredData ?? {
        '@context': 'https://schema.org',
        '@type': metadata.path === '/' ? 'WebSite' : 'WebPage',
        name: metadata.title,
        description,
        url: canonicalUrl,
      },
    );
  }

  setProduct(product: ProductDetail, path: string): void {
    const image = product.imageUrls?.[0] ?? product.imageUrl;
    const url = this.absolute(path);
    this.setPage({
      title: product.name,
      description: product.description || product.name,
      path,
      image,
      type: 'product',
      structuredData: {
        '@context': 'https://schema.org',
        '@type': 'Product',
        name: product.name,
        description: this.clean(product.description),
        image: (product.imageUrls?.length ? product.imageUrls : image ? [image] : []).map((item) =>
          this.absolute(item),
        ),
        url,
        offers: product.variants.map((variant) => ({
          '@type': 'Offer',
          price: (variant.priceCents / 100).toFixed(2),
          priceCurrency: variant.currency,
          availability:
            variant.availableQuantity > 0
              ? 'https://schema.org/InStock'
              : 'https://schema.org/OutOfStock',
          url,
        })),
      },
    });
  }

  private updateName(name: string, content: string): void {
    this.meta.updateTag({ name, content }, `name='${name}'`);
  }

  private updateProperty(property: string, content: string): void {
    this.meta.updateTag({ property, content }, `property='${property}'`);
  }

  private setCanonical(url: string): void {
    let link = this.document.head.querySelector<HTMLLinkElement>('link[rel="canonical"]');
    if (!link) {
      link = this.document.createElement('link');
      link.rel = 'canonical';
      this.document.head.appendChild(link);
    }
    link.href = url;
  }

  private setStructuredData(value: Record<string, unknown>): void {
    let script = this.document.head.querySelector<HTMLScriptElement>('#storefront-structured-data');
    if (!script) {
      script = this.document.createElement('script');
      script.id = 'storefront-structured-data';
      script.type = 'application/ld+json';
      this.document.head.appendChild(script);
    }
    script.textContent = JSON.stringify(value).replace(/</g, '\\u003c');
  }

  private absolute(value: string): string {
    return new URL(value, window.location.origin).href;
  }

  private clean(value: string): string {
    const element = this.document.createElement('div');
    element.innerHTML = value ?? '';
    return (element.textContent ?? '').replace(/\s+/g, ' ').trim();
  }
}
