import { Component, HostListener, input, output, signal } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

/** One image of a gallery. */
export interface OpenShopfrontGalleryItem {
  id: string;
  /** The image address (https or relative). Empty shows a placeholder tile. */
  url?: string;
  /** Optional short title shown in the lightbox. */
  title?: string;
  /** Optional caption shown over the tile and in the lightbox. */
  caption?: string;
}

/** Translated aria labels for the lightbox controls, supplied by the consuming app. */
export interface OpenShopfrontGalleryLabels {
  close?: string;
  previous?: string;
  next?: string;
}

/**
 * A presentational image gallery: a responsive grid of tiles with hover captions and a
 * built-in lightbox (backdrop, previous/next, close). It holds no i18n — control labels come
 * in via the `labels` input. The host app can additionally react to tile clicks via `select`.
 */
@Component({
  selector: 'openshopfront-gallery',
  standalone: true,
  imports: [MatIconModule],
  templateUrl: './gallery.component.html',
  styleUrls: ['./gallery.component.css'],
})
export class OpenShopfrontGalleryComponent {
  /** The gallery items, in display order. */
  readonly items = input.required<OpenShopfrontGalleryItem[]>();
  /** Translated aria labels for the lightbox controls. */
  readonly labels = input<OpenShopfrontGalleryLabels>({});

  /** A tile was clicked; carries the item (the lightbox also opens). */
  readonly select = output<OpenShopfrontGalleryItem>();

  /** The index of the open lightbox image, or null when closed. */
  readonly activeIndex = signal<number | null>(null);

  /** The item shown in the lightbox, or null when it is closed. */
  activeItem(): OpenShopfrontGalleryItem | null {
    const index = this.activeIndex();
    return index === null ? null : (this.items()[index] ?? null);
  }

  initialOf(title?: string): string {
    return (title || '?').trim().charAt(0).toUpperCase() || '?';
  }

  open(index: number): void {
    this.activeIndex.set(index);
    const item = this.items()[index];
    if (item) this.select.emit(item);
  }

  close(): void {
    this.activeIndex.set(null);
  }

  step(delta: number): void {
    const items = this.items();
    const current = this.activeIndex();
    if (!items.length || current === null) return;
    this.activeIndex.set((current + delta + items.length) % items.length);
  }

  @HostListener('document:keydown', ['$event'])
  onKeydown(event: KeyboardEvent): void {
    if (this.activeIndex() === null) return;
    if (event.key === 'Escape') this.close();
    else if (event.key === 'ArrowLeft') this.step(-1);
    else if (event.key === 'ArrowRight') this.step(1);
  }
}
