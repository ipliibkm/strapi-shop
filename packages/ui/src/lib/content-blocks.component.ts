import { Component, ContentChild, TemplateRef, input, output } from '@angular/core';
import { NgTemplateOutlet } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { OpenShopfrontGalleryComponent, OpenShopfrontGalleryItem } from './gallery.component';

/** One content block of a published page. */
export interface ContentBlock {
  type: 'heading' | 'text' | 'image' | 'gallery' | 'cta' | 'products' | 'form';
  eyebrow?: string;
  title?: string;
  html?: string;
  url?: string;
  caption?: string;
  items?: { url?: string; caption?: string }[];
  label?: string;
  target?: string;
  limit?: number;
  formSlug?: string;
}

/** One product shown in a "products" block. */
export interface ContentProduct {
  id: string;
  name: string;
  /** The formatted price to display for this product. */
  priceLabel: string;
  /** The crawlable product detail URL. */
  href?: string;
}

/** Translated strings for the blocks renderer, supplied by the consuming app. */
export interface ContentBlocksLabels {
  continue: string;
  productsNone: string;
}

/**
 * Presentational renderer for a published page's content blocks (heading, text, image, cta,
 * products, form). It holds no state and no i18n — the consuming app passes translated labels,
 * product data and reacts to navigation/product events. The "form" block is delegated to a
 * template the app projects via `<ng-template #formBlock let-slug>…</ng-template>` so each app
 * can render its own form markup for a given slug.
 */
@Component({
  selector: 'openshopfront-content-blocks',
  standalone: true,
  imports: [
    NgTemplateOutlet,
    MatButtonModule,
    MatCardModule,
    MatIconModule,
    OpenShopfrontGalleryComponent,
  ],
  templateUrl: './content-blocks.component.html',
})
export class OpenShopfrontContentBlocksComponent {
  /** The form template projected by the consuming app; rendered for "form" blocks. */
  @ContentChild('formBlock', { read: TemplateRef }) formBlock?: TemplateRef<unknown>;

  /** The content blocks to render, in display order. */
  readonly blocks = input.required<ContentBlock[]>();
  /** The products to show for "products" blocks (already limited by the app). */
  readonly products = input<ContentProduct[]>([]);
  /** Translated strings for the renderer. */
  readonly labels = input.required<ContentBlocksLabels>();

  /** A CTA block was clicked; carries its target page key. */
  readonly navigate = output<string | undefined>();
  /** A product card in a "products" blocks was clicked. */
  readonly openProduct = output<ContentProduct>();

  /** The first letter of a product name, used as its media placeholder. */
  initialOf(name?: string): string {
    return (name || '?').trim().charAt(0).toUpperCase() || '?';
  }

  /** Maps the raw gallery block items to the gallery component's shape with stable ids. */
  galleryItems(block: ContentBlock): OpenShopfrontGalleryItem[] {
    return (block.items ?? []).map((item, index) => ({
      id: String(index),
      url: item.url,
      caption: item.caption,
    }));
  }
}
