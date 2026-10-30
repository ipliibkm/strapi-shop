import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import {
  CustomPalette,
  OpenShopfrontLanguageListComponent,
  OpenShopfrontThemePickerComponent,
  OpenShopfrontThemeService,
  PALETTE_FIELDS,
  PaletteFieldDef,
  parseCustomPalette,
  ThemePickerLabels,
  ThemePickerOption,
} from '@openshopfront/ui';
import { AdminI18n } from '../shared/i18n.service';
import { AdminUiSettingsService } from '../shared/admin-ui-settings.service';
import { StorefrontAppearance, StorefrontLanguage } from '../shared/types';

/** The appearance section being edited: the customer site or this admin panel. */
type AppearanceTab = 'storefront' | 'admin';

/** The Appearance view: storefront + admin color palettes and available languages. */
@Component({
  selector: 'ip-appearance',
  standalone: true,
  imports: [OpenShopfrontThemePickerComponent, OpenShopfrontLanguageListComponent],
  templateUrl: './appearance.component.html',
})
export class AppearanceComponent {
  private http = inject(HttpClient);
  readonly i18n = inject(AdminI18n);
  readonly theme = inject(OpenShopfrontThemeService);
  readonly adminUi = inject(AdminUiSettingsService);
  /** The active appearance section: storefront (customer site) or admin panel. */
  readonly tab = signal<AppearanceTab>('storefront');

  /** Storefront language switches and published palette, or null while loading. */
  readonly storefrontSettings = signal<StorefrontAppearance | null>(null);
  readonly storefrontBusy = signal(false);
  readonly storefrontNotice = signal('');
  /** The published custom palette parsed into fields, or null when a preset is active. */
  readonly publishedCustomPalette = computed<CustomPalette | null>(() =>
    parseCustomPalette(this.storefrontSettings()?.customPalette ?? null),
  );
  /** The palette being edited in the storefront custom card. */
  readonly draftPalette = signal<CustomPalette | null>(null);
  /** Whether the storefront currently uses a custom palette rather than a preset. */
  readonly usingCustomStorefront = computed(() => this.publishedCustomPalette() !== null);
  /** The palette being edited in the admin personal custom card. */
  readonly adminDraftPalette = signal<CustomPalette | null>(null);
  /** Whether the admin panel currently uses a custom palette rather than a preset. */
  readonly usingCustomAdmin = computed(() => this.theme.customPalette() !== null);

  /** Preset themes with translated labels, shared by both pickers. */
  readonly themePickerOptions = computed<ThemePickerOption[]>(() =>
    this.theme.options().map((o) => ({
      key: o.key,
      label: this.t(o.name),
      hint: this.t(o.description),
      swatches: o.swatches,
    })),
  );
  /** The editable color fields with translated labels. */
  readonly paletteFieldDefs = computed<PaletteFieldDef[]>(() =>
    PALETTE_FIELDS.map((f) => ({ key: f.key, label: this.t(f.label) })),
  );

  constructor() {
    void this.loadStorefrontSettings();
    void this.adminUi.load();
  }

  langVersion(): string {
    return this.i18n.lang();
  }
  t(text: string, params?: Record<string, string | number>): string {
    return this.i18n.t(text, params);
  }

  /** The published storefront palette key, or null while settings load. */
  storefrontTheme(): string | null {
    return this.storefrontSettings()?.theme ?? null;
  }
  /** All website languages in display order for the language panel. */
  storefrontLanguages(): StorefrontLanguage[] {
    return this.storefrontSettings()?.languages ?? [];
  }
  /** The storefront draft palette as a plain token map for the picker. */
  storefrontDraft(): Record<string, string> | null {
    const p = this.draftPalette();
    return p ? (p as unknown as Record<string, string>) : null;
  }
  /** The admin draft palette as a plain token map for the picker. */
  adminDraft(): Record<string, string> | null {
    const p = this.adminDraftPalette();
    return p ? (p as unknown as Record<string, string>) : null;
  }
  /** Swatches previewing the draft (or published) custom palette for its card. */
  storefrontCustomSwatches(): string[] {
    const p = this.draftPalette() ?? this.publishedCustomPalette();
    if (!p) return ['#f5f0e8', '#ffffff', '#a65e37', '#211b15'];
    return [p.bg, p.surface, p.accent, p.sidebar];
  }
  /** Swatches previewing the admin custom palette for its card. */
  adminCustomSwatches(): string[] {
    const p = this.adminDraftPalette() ?? this.theme.customPalette();
    if (!p) return ['#f5f0e8', '#ffffff', '#a65e37', '#211b15'];
    return [p.bg, p.surface, p.accent, p.sidebar];
  }
  storefrontPickerLabels(): ThemePickerLabels {
    return {
      groupLabel: this.t('Storefront color palette'),
      customTitle: this.t('Custom palette'),
      customHint: this.t('Choose each color yourself.'),
      saveLabel: this.t('Save custom palette'),
      idleHint: this.t('Select a color to start editing.'),
    };
  }
  adminPickerLabels(): ThemePickerLabels {
    return {
      groupLabel: this.t('Admin theme'),
      customTitle: this.t('Custom palette'),
      customHint: this.t('Choose each color yourself.'),
      saveLabel: this.t('Apply to admin panel'),
      idleHint: this.t('Select a color to start editing.'),
    };
  }

  private async loadStorefrontSettings() {
    try {
      const settings = await firstValueFrom(
        this.http.get<StorefrontAppearance>('/api/v1/admin/storefront-settings'),
      );
      this.storefrontSettings.set(settings);
      // Seed the draft editor from the published custom palette when one exists.
      if (this.publishedCustomPalette())
        this.draftPalette.set({ ...this.publishedCustomPalette()! });
    } catch {
      this.storefrontNotice.set(this.t('The storefront settings could not be loaded.'));
    }
  }
  /** Publishes the selected preset palette to every storefront visitor. */
  async saveStorefrontTheme(key: string) {
    if (this.storefrontBusy()) return;
    this.storefrontBusy.set(true);
    this.storefrontNotice.set('');
    try {
      const settings = await firstValueFrom(
        this.http.put<StorefrontAppearance>('/api/v1/admin/storefront-settings/appearance', {
          theme: key,
        }),
      );
      this.storefrontSettings.set(settings);
      this.draftPalette.set(null);
      this.storefrontNotice.set(this.t('The color palette is published to the storefront.'));
    } catch (error) {
      const message = (error as HttpErrorResponse)?.error?.message;
      this.storefrontNotice.set(this.t(message || 'The color palette could not be saved.'));
    } finally {
      this.storefrontBusy.set(false);
    }
  }
  /** Opens the custom editor, pre-filled with the published palette or a sensible default. */
  openCustomPalette() {
    const base =
      this.publishedCustomPalette() ??
      ((): CustomPalette => ({
        bg: '#f5f0e8',
        surface: '#ffffff',
        accent: '#a65e37',
        fg: '#2a241e',
        sidebar: '#211b15',
      }))();
    this.draftPalette.set({ ...base });
  }
  /** Updates one color in the draft custom palette. */
  updateDraftColor(key: string, value: string) {
    if (!this.draftPalette()) return;
    this.draftPalette.update((current) => (current ? { ...current, [key]: value } : current));
  }
  /** Publishes the draft custom palette to every storefront visitor. */
  async saveCustomPalette() {
    const palette = this.draftPalette();
    if (!palette || this.storefrontBusy()) return;
    this.storefrontBusy.set(true);
    this.storefrontNotice.set('');
    try {
      const settings = await firstValueFrom(
        this.http.put<StorefrontAppearance>('/api/v1/admin/storefront-settings/custom-palette', {
          palette,
        }),
      );
      this.storefrontSettings.set(settings);
      this.storefrontNotice.set(this.t('The custom palette is published to the storefront.'));
    } catch (error) {
      const message = (error as HttpErrorResponse)?.error?.message;
      this.storefrontNotice.set(this.t(message || 'The custom palette could not be saved.'));
    } finally {
      this.storefrontBusy.set(false);
    }
  }
  /** Toggles one website language and saves the complete list. */
  async toggleStorefrontLanguage(item: StorefrontLanguage, enabled: boolean) {
    if (!enabled && this.storefrontLanguages().filter((entry) => entry.enabled).length === 1) {
      this.storefrontNotice.set(this.t('Keep at least one language enabled.'));
      return;
    }
    const languages = this.storefrontLanguages().map((entry) => ({
      code: entry.code,
      name: entry.name,
      enabled: entry.code === item.code ? enabled : entry.enabled,
    }));
    this.storefrontSettings.update((current) => current && { ...current, languages });
    // Every request carries the complete list, so concurrent saves stay consistent.
    this.storefrontBusy.set(true);
    this.storefrontNotice.set('');
    try {
      const settings = await firstValueFrom(
        this.http.put<StorefrontAppearance>(
          '/api/v1/admin/storefront-settings/languages',
          languages.map((entry) => ({ code: entry.code, enabled: entry.enabled })),
        ),
      );
      this.storefrontSettings.set(settings);
      this.storefrontNotice.set(this.t('The website languages are saved.'));
    } catch (error) {
      const message = (error as HttpErrorResponse)?.error?.message;
      await this.loadStorefrontSettings();
      this.storefrontNotice.set(this.t(message || 'The language changes could not be saved.'));
    } finally {
      this.storefrontBusy.set(false);
    }
  }
  onStorefrontLanguageToggle(change: { code: string; enabled: boolean }) {
    const item = this.storefrontLanguages().find((entry) => entry.code === change.code);
    if (item) void this.toggleStorefrontLanguage(item, change.enabled);
  }

  /** Opens the admin custom editor, pre-filled with the current palette or a default. */
  openAdminCustomPalette() {
    const base =
      this.theme.customPalette() ??
      ((): CustomPalette => ({
        bg: '#f5f0e8',
        surface: '#ffffff',
        accent: '#a65e37',
        fg: '#2a241e',
        sidebar: '#211b15',
      }))();
    this.adminDraftPalette.set({ ...base });
  }
  /** Updates one color in the admin draft custom palette. */
  updateAdminDraftColor(key: string, value: string) {
    if (!this.adminDraftPalette()) return;
    this.adminDraftPalette.update((c) => (c ? { ...c, [key]: value } : c));
  }
  /** Applies the admin draft palette to this browser's admin panel. */
  applyAdminCustomPalette() {
    const palette = this.adminDraftPalette();
    if (!palette) return;
    this.theme.applyCustom(palette);
  }
  onAdminLanguageToggle(change: { code: string; enabled: boolean }) {
    const item = this.adminUi.list().find((entry) => entry.code === change.code);
    if (item) void this.adminUi.toggle(item, change.enabled);
  }

  /** Switches between the storefront and admin-panel appearance sections. */
  switchTab(next: AppearanceTab) {
    if (this.tab() !== next) this.tab.set(next);
  }
}
