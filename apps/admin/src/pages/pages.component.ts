import { HttpClient } from '@angular/common/http';
import { Lang } from '../shared/i18n';
import { Component, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { AdminI18n } from '../shared/i18n.service';

interface PageSummary {
  documentId: string;
  slug: string;
  title: string;
  enabled: boolean;
  published: boolean;
  unpublishedChanges: boolean;
}
interface ArticleRow {
  id: string;
  title: string;
  slug: string;
  contentHtml: string;
  tags: string[];
  published: boolean;
  createdAt: string;
  updatedAt: string;
}

/** One image of a gallery block. */
interface GalleryItem {
  url: string;
  caption: string;
}

/** One block on a storefront page. */
interface PageBlock {
  type: 'heading' | 'text' | 'image' | 'gallery' | 'cta' | 'products' | 'form';
  eyebrow?: string;
  title?: string;
  html?: string;
  url?: string;
  caption?: string;
  items?: GalleryItem[];
  label?: string;
  target?: string;
  limit?: number;
  formSlug?: string;
}
interface FormSummary {
  slug: string;
  name: string;
  enabled: boolean;
  published: boolean;
}

/** Storefront pages with block-based content and tagged articles. */
@Component({
  selector: 'ip-pages',
  standalone: true,
  imports: [MatButtonModule, MatIconModule],
  templateUrl: './pages.component.html',
})
class PagesComponent {
  private readonly http = inject(HttpClient);
  private readonly sanitizer = inject(DomSanitizer);
  readonly i18n = inject(AdminI18n);
  /** Reads the active language so templates track it and re-render on change. */
  langVersion(): Lang {
    return this.i18n.lang();
  }
  /** Translates one interface string for the active admin language. */
  t(text: string, params?: Record<string, string | number>): string {
    return this.i18n.t(text, params);
  }

  // STATE
  readonly tab = signal<'pages' | 'articles'>('pages');
  readonly pages = signal<PageSummary[]>([]);

  /** Human-friendly titles for the path tree, keyed by slug. */
  private readonly pageTitles: Record<string, string> = {
    home: 'Home Page',
    shop: 'Shop Page',
    services: 'Services Page',
    about: 'About Us',
    contact: 'Contact Us',
  };

  /** Pages that actually exist in the database — drives the navigation tree. */
  readonly pageOptions = signal<{ slug: string; title: string; enabled: boolean }[]>([]);

  // PAGE EDITOR STATE
  readonly selectedPage = signal('');
  readonly pageDetail = signal<{
    documentId: string;
    slug: string;
    title: string;
    enabled: boolean;
    blocks: PageBlock[];
  } | null>(null);
  readonly enabled = signal(true);
  readonly published = signal(false);
  readonly dirty = signal(false);
  readonly busy = signal(false);
  readonly notice = signal('');
  readonly failed = signal(false);

  readonly blocks = signal<PageBlock[]>([]);
  readonly previewUrl = signal<SafeResourceUrl | null>(null);
  readonly formOptions = signal<FormSummary[]>([]);
  private previewOrigin = '';
  /** Re-evaluated on every access so labels follow the active language. */
  get blockKinds(): { type: PageBlock['type']; label: string }[] {
    return [
      { type: 'heading', label: this.t('Heading & Intro') },
      { type: 'text', label: this.t('Paragraph / Text Block') },
      { type: 'image', label: this.t('Image with Caption') },
      { type: 'gallery', label: this.t('Image Gallery') },
      { type: 'cta', label: this.t('Call-to-Action Button') },
      { type: 'products', label: this.t('Product Grid (from Shop)') },
      { type: 'form', label: this.t('Reusable Form') },
    ];
  }

  get ctaTargets() {
    return [
      { value: 'booking', label: this.t('Book a Visit') },
      { value: 'shop', label: this.t('Shop') },
      { value: 'services', label: this.t('Services') },
      { value: 'about', label: this.t('About Us') },
      { value: 'contact', label: this.t('Contact') },
    ];
  }

  // ARTICLE EDITOR STATE
  readonly articles = signal<ArticleRow[]>([]);
  readonly selectedArticle = signal('');
  readonly articleEditor = signal<ArticleRow | null>(null);
  readonly articleTitle = signal('');
  readonly articleContent = signal('');
  readonly articleTags = signal<string[]>([]);
  readonly articleDirty = signal(false);
  readonly focusedBlock = signal<number | null>(null);

  constructor() {
    window.addEventListener('message', (event) => {
      if (event.data?.type !== 'openshopfront:storefront-preview-ready') return;
      if (this.previewOrigin && event.origin !== this.previewOrigin) return;
      const frame = document.querySelector<HTMLIFrameElement>('#storefront-page-preview');
      if (event.source !== frame?.contentWindow) return;
      this.sendPreview();
    });
    void this.refresh();
  }

  private async refresh() {
    try {
      const [list, preview, forms] = await Promise.all([
        firstValueFrom(this.http.get<PageSummary[]>('/api/v1/admin/pages')),
        firstValueFrom(
          this.http.get<{ storefrontBaseUrl: string }>('/api/v1/admin/pages/preview-config'),
        ),
        firstValueFrom(this.http.get<FormSummary[]>('/api/v1/admin/forms')),
      ]);
      this.pages.set(list ?? []);
      this.formOptions.set(forms ?? []);
      this.previewOrigin = new URL(preview.storefrontBaseUrl, window.location.href).origin;
      // Build the navigation tree only from pages that exist in the database.
      const options = (list ?? [])
        .map((p) => ({
          slug: p.slug,
          title: this.t(this.pageTitles[p.slug] ?? p.title),
          enabled: p.enabled,
        }))
        .sort(
          (a, b) =>
            ['home', 'shop', 'services', 'about', 'contact'].indexOf(a.slug) -
            ['home', 'shop', 'services', 'about', 'contact'].indexOf(b.slug),
        );
      this.pageOptions.set(options);
      if (options.length && !this.selectedPage()) {
        await this.selectPageBySlug(options[0].slug);
      }

      // Load articles too
      await this.loadArticles();
    } catch {
      this.notice.set(this.t('The page list could not be loaded.'));
      this.failed.set(true);
    }
  }

  private async loadArticles() {
    try {
      const arts = await firstValueFrom(this.http.get<ArticleRow[]>('/api/v1/admin/articles'));
      this.articles.set(arts ?? []);
    } catch (e) {
      console.error('Failed to load articles', e);
    }
  }

  // NAVIGATION
  switchToArticles() {
    this.tab.set('articles');
    if (!this.selectedArticle() && this.articles().length > 0) {
      this.selectArticleBySlug(this.articles()[0].slug);
    } else if (this.articles().length === 0) {
      this.newArticle();
    }
  }

  selectPageBySlug(slug: string) {
    this.tab.set('pages');
    void this.loadPage(slug);
  }

  selectArticleBySlug(slug: string) {
    this.tab.set('articles');
    const art = this.articles().find((a) => a.slug === slug);
    if (art) {
      this.articleEditor.set(art);
      this.articleTitle.set(art.title);
      this.articleContent.set(art.contentHtml || '');
      this.articleTags.set(art.tags ?? []);
      this.articleDirty.set(false);
      this.selectedArticle.set(slug);
    }
  }

  newArticle() {
    const slug = `article-${Date.now()}`;
    const empty: ArticleRow = {
      id: '',
      title: this.t('Untitled article'),
      slug,
      contentHtml: `<p>${this.t('Start writing your article here...')}</p>`,
      tags: [],
      published: false,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    };
    this.articleEditor.set(empty);
    this.articleTitle.set(this.t('Untitled article'));
    this.articleContent.set(empty.contentHtml);
    this.articleTags.set([]);
    this.selectedArticle.set(slug);
    this.tab.set('articles');
  }

  // PAGE LOADING
  private async loadPage(slug: string) {
    this.busy.set(true);
    this.failed.set(false);
    try {
      const detail = await firstValueFrom(
        this.http.get<{
          documentId: string;
          slug: string;
          title: string;
          enabled: boolean;
          blocks: PageBlock[];
          published: boolean;
        }>(`/api/v1/admin/pages/${slug}`),
      );
      this.selectedPage.set(slug);
      this.pageDetail.set(detail);
      this.blocks.set((detail.blocks ?? []).map((b) => ({ ...b })));
      this.enabled.set(detail.enabled);
      this.published.set(Boolean(detail.published));
      this.dirty.set(false);
      this.updatePreviewUrl(slug);
      this.sendPreviewSoon();
    } catch (error) {
      const message = (error as { error?: { message?: string } })?.error?.message;
      if (/not found|unknown/i.test(message ?? String(error))) {
        this.notice.set(
          this.t('The page "{slug}" does not exist yet. It will be created when you save.', {
            slug,
          }),
        );
      } else {
        this.notice.set(
          this.t('This page could not be opened:') +
            ' ' +
            (message ? this.t(message) : this.t('check the backend logs.')),
        );
      }
      this.failed.set(true);
    } finally {
      this.busy.set(false);
    }
  }

  // BLOCK OPERATIONS (pages)
  blockLabel(type: string) {
    const map: Record<string, string> = {
      heading: this.t('Heading & Intro'),
      text: this.t('Text Block'),
      image: this.t('Image'),
      gallery: this.t('Gallery'),
      cta: this.t('Button / CTA'),
      products: this.t('Product Grid'),
      form: this.t('Reusable Form'),
    };
    return map[type] || type.toUpperCase();
  }

  setField(index: number, field: string, event: Event) {
    const value = (event.target as HTMLInputElement | HTMLSelectElement).value ?? '';
    const next = [...this.blocks()];
    next[index] = { ...next[index], [field]: value };
    this.blocks.set(next);
    this.dirty.set(true);
    this.sendPreviewSoon();
  }

  setLimit(index: number, event: Event) {
    const raw = Number((event.target as HTMLInputElement).value);
    if (Number.isNaN(raw)) return;
    const limit = Math.max(1, Math.min(24, Math.round(raw)));
    const next = [...this.blocks()];
    next[index] = { ...next[index], limit };
    this.blocks.set(next);
    this.dirty.set(true);
    this.sendPreviewSoon();
  }

  // These methods take the block object (not its index) because the inner item loop's $index
  // shadows the outer block loop's $index in the template.

  setGalleryItem(block: PageBlock, itemIndex: number, field: 'url' | 'caption', event: Event) {
    const value = (event.target as HTMLInputElement).value ?? '';
    const index = this.blocks().indexOf(block);
    if (index < 0) return;
    const next = [...this.blocks()];
    const items = [...(next[index].items ?? [])];
    items[itemIndex] = { ...(items[itemIndex] ?? { url: '', caption: '' }), [field]: value };
    next[index] = { ...next[index], items };
    this.blocks.set(next);
    this.dirty.set(true);
    this.sendPreviewSoon();
  }

  addGalleryItem(block: PageBlock) {
    const index = this.blocks().indexOf(block);
    if (index < 0) return;
    const next = [...this.blocks()];
    const items = [...(next[index].items ?? []), { url: '', caption: '' }];
    next[index] = { ...next[index], items };
    this.blocks.set(next);
    this.dirty.set(true);
    this.sendPreviewSoon();
  }

  removeGalleryItem(block: PageBlock, itemIndex: number) {
    const index = this.blocks().indexOf(block);
    if (index < 0) return;
    const next = [...this.blocks()];
    const items = (next[index].items ?? []).filter((_, i) => i !== itemIndex);
    next[index] = { ...next[index], items };
    this.blocks.set(next);
    this.dirty.set(true);
    this.sendPreviewSoon();
  }

  addBlock(type: PageBlock['type']) {
    const base: PageBlock = { type };
    if (type === 'heading') Object.assign(base, { eyebrow: '', title: '' });
    if (type === 'text') Object.assign(base, { html: `<p>${this.t('Write your text here.')}</p>` });
    if (type === 'image') Object.assign(base, { url: '', caption: '' });
    if (type === 'gallery') Object.assign(base, { items: [{ url: '', caption: '' }] });
    if (type === 'cta') Object.assign(base, { label: this.t('Learn more'), target: 'booking' });
    if (type === 'products') Object.assign(base, { limit: 6 });
    if (type === 'form') Object.assign(base, { formSlug: this.formOptions()[0]?.slug ?? '' });
    this.blocks.set([...this.blocks(), base]);
    this.dirty.set(true);
    this.sendPreviewSoon();
  }

  moveBlock(index: number, direction: number) {
    const next = [...this.blocks()];
    const target = index + direction;
    if (target < 0 || target >= next.length) return;
    [next[index], next[target]] = [next[target], next[index]];
    this.blocks.set(next);
    this.dirty.set(true);
    this.sendPreviewSoon();
  }

  removeBlock(index: number) {
    const next = [...this.blocks()];
    next.splice(index, 1);
    this.blocks.set(next);
    this.dirty.set(true);
    this.sendPreviewSoon();
  }

  // RICH TEXT HELPERS (per-block focus tracking)
  onRichtextFocus(index: number) {
    this.focusedBlock.set(index);
  }

  formatFocused(command: string) {
    if (this.focusedBlock() !== null && document.activeElement instanceof HTMLElement) {
      // execCommand acts on the currently focused contenteditable element in DOM
      const editor = document.querySelector<HTMLElement>('.richtext-editor:focus');
      if (editor) {
        editor.focus();
        document.execCommand(command);
      }
    }
  }

  onRichtextBlur(index: number, event: FocusEvent) {
    const element = event.target as HTMLElement;
    if (index >= 0) {
      const next = [...this.blocks()];
      next[index] = { ...next[index], html: element.innerHTML };
      this.blocks.set(next);
      this.dirty.set(true);
      this.sendPreviewSoon();
    } else {
      // Article mode (-1 index)
      this.articleContent.set(element.innerHTML);
      this.articleDirty.set(true);
    }
  }

  onArticleBlur(event: FocusEvent) {
    const element = event.target as HTMLElement;
    this.articleContent.set(element.innerHTML);
    this.articleDirty.set(true);
  }

  // ARTICLE OPERATIONS
  toggleArticleTag(slug: string, event: Event) {
    const checked = (event.target as HTMLInputElement).checked;
    let tags = [...this.articleTags()];
    if (checked && !tags.includes(slug)) {
      tags.push(slug);
    } else if (!checked) {
      tags = tags.filter((t) => t !== slug);
    }
    this.articleTags.set(tags);
    this.articleDirty.set(true);
  }

  onArticleTitleChange(event: Event) {
    const value = (event.target as HTMLInputElement).value;
    this.articleTitle.set(value || this.t('Untitled article'));
    this.articleDirty.set(true);
  }

  async saveArticle() {
    if (!this.selectedArticle()) return;
    this.busy.set(true);
    try {
      const saved = await firstValueFrom(
        this.http.put<ArticleRow>(`/api/v1/admin/articles/${this.selectedArticle()}`, {
          title: this.articleTitle(),
          contentHtml: this.articleContent(),
          tags: this.articleTags(),
        }),
      );
      this.articleEditor.set(saved);
      this.articleDirty.set(false);
      await this.loadArticles(); // Refresh list
    } catch (e) {
      const msg = (e as any)?.error?.message || 'The article could not be saved.';
      alert(this.t(msg));
    } finally {
      this.busy.set(false);
    }
  }

  async unpublishArticle() {
    if (!this.selectedArticle()) return;
    // Backend doesn't have a separate unpublish endpoint yet — toggle via save with published=false?
    // For now, just reload after saving (assumes backend handles publish flag separately)
  }

  // PAGE SAVE/PUBLISH
  setEnabled(event: Event) {
    this.enabled.set((event.target as HTMLInputElement).checked);
    this.dirty.set(true);
    this.sendPreviewSoon();
  }

  async saveDraft(): Promise<boolean> {
    if (!this.selectedPage()) return false;
    this.busy.set(true);
    try {
      await firstValueFrom(
        this.http.put(`/api/v1/admin/pages/${this.selectedPage()}`, {
          title: this.pageDetail()?.title,
          enabled: this.enabled(),
          blocks: this.blocks(),
        }),
      );
      this.dirty.set(false);
      this.notice.set(this.t('Draft saved. The storefront still shows the previous version.'));
      return true;
    } catch (e) {
      const msg = (e as any)?.error?.message || 'The draft could not be saved.';
      alert(this.t(msg));
      return false;
    } finally {
      this.busy.set(false);
    }
  }

  async publishPage() {
    if (!this.selectedPage()) return;
    if (!(await this.saveDraft())) return;
    try {
      const result = await firstValueFrom(
        this.http.post<{ published: boolean }>(
          `/api/v1/admin/pages/${this.selectedPage()}/publish`,
          null,
        ),
      );
      this.published.set(Boolean(result?.published));
      this.notice.set(
        this.enabled()
          ? this.t('The page is now live on the storefront.')
          : this.t('The page is published but disabled on the storefront.'),
      );
      await this.refresh();
    } catch (e) {
      const msg = (e as any)?.error?.message || 'The page could not be published.';
      alert(this.t(msg));
    }
  }

  sendPreview() {
    const frame = document.querySelector<HTMLIFrameElement>('#storefront-page-preview');
    if (!frame?.contentWindow || !this.selectedPage()) return;
    frame.contentWindow.postMessage(
      {
        type: 'openshopfront:storefront-preview',
        page: {
          slug: this.selectedPage(),
          title: this.pageDetail()?.title ?? '',
          enabled: this.enabled(),
          blocks: this.blocks(),
        },
      },
      this.previewOrigin || '*',
    );
  }

  private sendPreviewSoon() {
    window.setTimeout(() => this.sendPreview());
  }

  private updatePreviewUrl(slug: string) {
    if (!this.previewOrigin) return;
    const url = `${this.previewOrigin}/?preview=1#${encodeURIComponent(slug)}`;
    this.previewUrl.set(this.sanitizer.bypassSecurityTrustResourceUrl(url));
  }

  // HELPERS
  ctaLabel(target: string | undefined) {
    const map: Record<string, string> = {
      booking: this.t('Book a Visit'),
      shop: this.t('Shop'),
      services: this.t('Services'),
      about: this.t('About Us'),
      contact: this.t('Contact'),
    };
    return map[target ?? ''] || target;
  }

  contentLoaded() {
    if (this.tab() === 'pages') return this.pageDetail() !== null;
    return this.articleEditor() !== null;
  }
}

export { PagesComponent };
