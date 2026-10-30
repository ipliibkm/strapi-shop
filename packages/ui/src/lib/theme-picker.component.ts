import { Component, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';

/** One selectable preset theme with its display metadata and preview swatches. */
export interface ThemePickerOption {
  key: string;
  /** Translated display name. */
  label: string;
  /** Translated short description. */
  hint: string;
  swatches: string[];
}

/** One editable color field in the custom palette editor. */
export interface PaletteFieldDef {
  key: string;
  /** Translated field label. */
  label: string;
}

/** All user-facing strings for the picker, translated by the consuming app. */
export interface ThemePickerLabels {
  groupLabel: string;
  customTitle: string;
  customHint: string;
  saveLabel: string;
  idleHint: string;
}

/**
 * Presentational theme picker: preset cards plus an optional custom-palette card with an
 * inline color editor. It holds no state and no i18n — the consuming app passes translated
 * labels/data in and reacts to the emitted events.
 */
@Component({
  selector: 'openshopfront-theme-picker',
  standalone: true,
  imports: [MatButtonModule],
  templateUrl: './theme-picker.component.html',
})
export class OpenShopfrontThemePickerComponent {
  /** The preset themes to render, in display order. */
  readonly themes = input.required<ThemePickerOption[]>();
  /** Key of the active preset, or null when a custom palette is in use. */
  readonly selectedKey = input<string | null>(null);
  /** Whether a custom palette (rather than a preset) is currently active. */
  readonly usingCustom = input(false);
  /** Whether to render the custom-palette card at all. */
  readonly showCustom = input(true);
  /** Preview swatches for the custom card. */
  readonly customSwatches = input<string[]>([]);
  /** The editable color fields, in picker order. */
  readonly paletteFields = input<PaletteFieldDef[]>([]);
  /** The palette being edited; null means the editor is collapsed. */
  readonly draftPalette = input<Record<string, string> | null>(null);
  /** Disables the save button while a request is in flight. */
  readonly busy = input(false);
  /** Translated user-facing strings. */
  readonly labels = input.required<ThemePickerLabels>();

  /** A preset card was chosen. */
  readonly selectPreset = output<string>();
  /** The custom card header was clicked (open the editor). */
  readonly openCustom = output<void>();
  /** One color in the draft palette changed. */
  readonly updateColor = output<{ key: string; value: string }>();
  /** The save/apply button was pressed. */
  readonly saveCustom = output<void>();
}
