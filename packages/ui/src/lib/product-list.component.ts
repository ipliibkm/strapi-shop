import { Component, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

/** One product of a product list. */
export interface OpenShopfrontProductListItem {
  id: string;
  name: string;
  description?: string;
  /** The formatted price to display (the app formats money, not this component). */
  priceLabel?: string;
  /** Optional short badge, for example "New" or "-20%". */
  badge?: string;
  /** The product image address. Empty shows an initial placeholder. */
  url?: string;
  /** The crawlable product detail URL. */
  href?: string;
}

/** Translated strings for the list, supplied by the consuming app. */
export interface OpenShopfrontProductListLabels {
  addToCart?: string;
  empty?: string;
}

/**
 * A presentational product grid: cards with media (image or initial placeholder), optional
 * badge, name, description and price row. It holds no i18n — labels come in via the `labels`
 * input. Card clicks emit `open`; the add-to-cart button (only rendered when `labels.addToCart`
 * is set) emits `addToCart`.
 */
@Component({
  selector: 'openshopfront-product-list',
  standalone: true,
  imports: [MatButtonModule, MatIconModule],
  templateUrl: './product-list.component.html',
  styleUrls: ['./product-list.component.css'],
})
export class OpenShopfrontProductListComponent {
  /** The products to show, in display order. */
  readonly products = input.required<OpenShopfrontProductListItem[]>();
  /** Translated strings for the list. */
  readonly labels = input<OpenShopfrontProductListLabels>({});

  /** A product card was clicked. */
  readonly open = output<OpenShopfrontProductListItem>();
  /** The add-to-cart button of a card was clicked. */
  readonly addToCart = output<OpenShopfrontProductListItem>();

  initialOf(name?: string): string {
    return (name || '?').trim().charAt(0).toUpperCase() || '?';
  }
}
