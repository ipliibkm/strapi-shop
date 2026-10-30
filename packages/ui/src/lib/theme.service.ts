import { DOCUMENT } from '@angular/common';
import { Injectable, inject, signal } from '@angular/core';

export type OpenShopfrontTheme = 'light' | 'dark' | 'ocean' | 'rosewater';

/** A custom palette is a full set of semantic color tokens chosen by the admin. */
export interface CustomPalette {
  bg: string;
  surface: string;
  accent: string;
  fg: string;
  sidebar: string;
}

/** One complete theme: display metadata plus the semantic tokens it applies. */
interface ThemeDefinition {
  name: string;
  description: string;
  tokens: Record<string, string>;
}

/** The CSS custom properties a palette controls, in picker order. */
export const PALETTE_FIELDS = [
  { key: 'bg', token: '--openshopfront-bg', label: 'Background' },
  { key: 'surface', token: '--surface', label: 'Surface' },
  { key: 'accent', token: '--openshopfront-accent', label: 'Accent' },
  { key: 'fg', token: '--openshopfront-fg', label: 'Text' },
  { key: 'sidebar', token: '--sidebar', label: 'Sidebar' },
] as const;

/** Builds the full CSS-variable map for a custom palette, deriving soft/border/hero tones. */
export function tokensForPalette(p: CustomPalette): Record<string, string> {
  return {
    '--openshopfront-bg': p.bg,
    '--surface': p.surface,
    '--openshopfront-accent': p.accent,
    '--openshopfront-fg': p.fg,
    '--sidebar': p.sidebar,
    // Derived tones keep the palette coherent without extra pickers.
    '--surface-soft': mix(p.surface, p.bg, 0.5),
    '--border': mix(p.surface, p.fg, 0.12),
    '--muted': mix(p.fg, p.surface, 0.45),
    '--on-surface-muted': mix(p.fg, p.surface, 0.3),
    /** High-contrast text for brand/active/hover states; dark on bright sidebars, light on dark ones. */
    '--sidebar-on': sidebarText(p.sidebar, true),
    '--sidebar-text': sidebarText(p.sidebar),
    '--hero-a': mix(p.bg, '#ffffff', 0.25),
    '--hero-mid': mix(p.bg, '#ffffff', 0.6),
    '--hero-b': mix(p.bg, p.fg, 0.08),
  };
}

/** Picks a readable sidebar text tone: light on dark sidebars, dark on bright ones. */
function sidebarText(background: string, strong = false): string {
  const c = hex(background);
  if (!c) return mix('#ffffff', background, 0.7);
  const [r, g, b] = c.map((v) => v / 255);
  const luminance = 0.2126 * r + 0.7152 * g + 0.0722 * b;
  // Strong text (brand/active/hover) stays close to full contrast so it never washes out.
  return luminance > 0.4
    ? mix(background, '#1c1917', strong ? 0.85 : 0.7)
    : mix('#ffffff', background, strong ? 0.25 : 0.4);
}

/** Linear blend of two hex colors; t=0 returns a, t=1 returns b. */
function mix(a: string, b: string, t: number): string {
  const pa = hex(a);
  const pb = hex(b);
  if (!pa || !pb) return a;
  const c = pa.map((v, i) => Math.round(v + (pb[i] - v) * t));
  return `#${c.map((v) => v.toString(16).padStart(2, '0')).join('')}`;
}

function hex(value: string): [number, number, number] | null {
  const m = value.trim().match(/^#?([0-9a-f]{6})$/i);
  if (!m) return null;
  const n = parseInt(m[1], 16);
  return [(n >> 16) & 255, (n >> 8) & 255, n & 255];
}

const presets: Record<OpenShopfrontTheme, ThemeDefinition> = {
  light: {
    name: 'Maison',
    description: 'Warm cream surfaces with copper accents.',
    tokens: {
      '--openshopfront-bg': '#f5f0e8',
      '--openshopfront-fg': '#2a241e',
      '--openshopfront-accent': '#a65e37',
      '--surface': '#ffffff',
      '--surface-soft': '#faf6ef',
      '--border': '#e2d8c9',
      '--muted': '#7c7166',
      /** Secondary text on light surfaces — always ≥4.5:1 against --surface */
      '--on-surface-muted': '#5f554b',
      '--sidebar': '#211b15',
      '--sidebar-text': '#cbbfb0',
      '--sidebar-on': '#f7f3ee',
      '--hero-a': '#f8f4ec',
      '--hero-mid': '#fdfaf4',
      '--hero-b': '#efe8db',
    },
  },
  dark: {
    name: 'Noir Atelier',
    description: 'Deep charcoal surfaces with gold-copper light.',
    tokens: {
      '--openshopfront-bg': '#16130f',
      '--openshopfront-fg': '#ede7de',
      '--openshopfront-accent': '#d9a066',
      '--surface': '#1e1a15',
      '--surface-soft': '#26211b',
      '--border': '#383129',
      '--muted': '#a39a8d',
      /** Secondary text on dark surfaces — always ≥4.5:1 against --surface */
      '--on-surface-muted': '#c9c0b5',
      '--sidebar': '#100e0b',
      '--sidebar-text': '#c9bcad',
      '--sidebar-on': '#f5efe8',
      '--hero-a': '#1b1712',
      '--hero-mid': '#241f18',
      '--hero-b': '#1a150f',
    },
  },
  ocean: {
    name: 'Lagoon',
    description: 'Cool seafoam surfaces with deep teal accents.',
    tokens: {
      '--openshopfront-bg': '#eef6f6',
      '--openshopfront-fg': '#0c3742',
      '--openshopfront-accent': '#0e7490',
      '--surface': '#ffffff',
      '--surface-soft': '#f2faf9',
      '--border': '#d5e4e4',
      '--muted': '#5a7178',
      /** Secondary text on light surfaces — always ≥4.5:1 against --surface */
      '--on-surface-muted': '#3f5a60',
      '--sidebar': '#0b2830',
      '--sidebar-text': '#a9c2c9',
      '--sidebar-on': '#eef7f8',
      '--hero-a': '#edf6f5',
      '--hero-mid': '#f7fcfb',
      '--hero-b': '#dfedeb',
    },
  },
  rosewater: {
    name: 'Blush',
    description: 'Soft pink surfaces with raspberry accents.',
    tokens: {
      '--openshopfront-bg': '#fbf3f6',
      '--openshopfront-fg': '#4a1f30',
      '--openshopfront-accent': '#be4b79',
      '--surface': '#ffffff',
      '--surface-soft': '#fbf6f8',
      '--border': '#efdbe3',
      '--muted': '#8a6b77',
      /** Secondary text on light surfaces — always ≥4.5:1 against --surface */
      '--on-surface-muted': '#6d4a5a',
      '--sidebar': '#2e1420',
      '--sidebar-text': '#d5b7c4',
      '--sidebar-on': '#fbeff4',
      '--hero-a': '#faf2f5',
      '--hero-mid': '#fdf9fb',
      '--hero-b': '#f3e4ea',
    },
  },
};

/** Selection order shown in theme pickers. */
const themeOrder: OpenShopfrontTheme[] = ['light', 'dark', 'ocean', 'rosewater'];

/** A picker-ready description of one available theme. */
export interface ThemeOption {
  key: OpenShopfrontTheme;
  name: string;
  description: string;
  swatches: [string, string, string, string];
}

@Injectable({ providedIn: 'root' })
export class OpenShopfrontThemeService {
  private doc = inject(DOCUMENT);
  readonly current = signal<OpenShopfrontTheme>('light');
  /** The active custom palette, or null when a preset is in use. */
  readonly customPalette = signal<CustomPalette | null>(null);
  private scope = 'admin';
  private keyFor(theme: OpenShopfrontTheme) {
    return theme && presets[theme] ? theme : 'light';
  }

  load(scope: 'admin' | 'storefront') {
    this.scope = scope;
    const savedCustom = readJson(
      this.doc.defaultView?.localStorage.getItem('openshopfront-palette-' + scope) ?? null,
    );
    if (savedCustom && isPalette(savedCustom)) {
      this.applyCustom(savedCustom);
      return;
    }
    const saved = this.doc.defaultView?.localStorage.getItem('openshopfront-theme-' + scope);
    this.setTheme(
      saved && presets[saved as OpenShopfrontTheme] ? (saved as OpenShopfrontTheme) : 'light',
    );
  }

  setTheme(theme: OpenShopfrontTheme | string) {
    const resolved = this.keyFor(theme as OpenShopfrontTheme);
    this.current.set(resolved);
    this.customPalette.set(null);
    this.applyTokens(presets[resolved].tokens);
    try {
      this.doc.defaultView?.localStorage.setItem('openshopfront-theme-' + this.scope, resolved);
      this.doc.defaultView?.localStorage.removeItem('openshopfront-palette-' + this.scope);
    } catch {
      // Storage may be unavailable; the in-memory theme still works.
    }
  }

  /** Applies a custom palette and remembers it for this scope. */
  applyCustom(palette: CustomPalette) {
    this.customPalette.set(palette);
    this.applyTokens(tokensForPalette(palette));
    try {
      this.doc.defaultView?.localStorage.setItem(
        'openshopfront-palette-' + this.scope,
        JSON.stringify(palette),
      );
      this.doc.defaultView?.localStorage.removeItem('openshopfront-theme-' + this.scope);
    } catch {
      // Storage may be unavailable; the in-memory palette still works.
    }
  }

  private applyTokens(tokens: Record<string, string>) {
    const root = this.doc.documentElement;
    for (const [token, value] of Object.entries(tokens)) {
      root.style.setProperty(token, value);
    }
  }

  /** Lists every selectable theme with the swatches shown in pickers. */
  options(): ThemeOption[] {
    return themeOrder.map((key) => ({
      key,
      name: presets[key].name,
      description: presets[key].description,
      swatches: [
        presets[key].tokens['--openshopfront-bg'],
        presets[key].tokens['--surface'],
        presets[key].tokens['--openshopfront-accent'],
        presets[key].tokens['--sidebar'],
      ],
    }));
  }
}

function readJson(raw: string | null): unknown {
  if (!raw) return null;
  try {
    return JSON.parse(raw);
  } catch {
    return null;
  }
}

function isPalette(value: unknown): value is CustomPalette {
  if (typeof value !== 'object' || value === null) return false;
  const v = value as Record<string, unknown>;
  return PALETTE_FIELDS.every((f) => typeof v[f.key] === 'string');
}

/** Parses and validates a stored custom palette JSON string; null when absent or malformed. */
export function parseCustomPalette(raw: string | null): CustomPalette | null {
  if (!raw) return null;
  try {
    const obj = JSON.parse(raw) as unknown;
    return isPalette(obj) ? (obj as CustomPalette) : null;
  } catch {
    return null;
  }
}
