import { Injectable, signal } from '@angular/core';
import { LANGUAGES, Lang, translate } from './i18n';

const STORAGE_KEY = 'openshopfront-admin-lang';

/** Holds the active admin panel language and translates interface strings. */
@Injectable({ providedIn: 'root' })
export class AdminI18n {
  readonly lang = signal<Lang>(this.initial());

  /** Translates one interface string for the active language. */
  t(text: string, params?: Record<string, string | number>): string {
    return translate(this.lang(), text, params);
  }

  setLang(code: Lang) {
    this.lang.set(code);
    try {
      localStorage.setItem(STORAGE_KEY, code);
    } catch {
      // Storage may be unavailable; the in-memory language still works.
    }
    document.documentElement.lang = code;
  }

  private initial(): Lang {
    const supported = Object.keys(LANGUAGES) as Lang[];
    try {
      const saved = localStorage.getItem(STORAGE_KEY);
      if (saved && supported.includes(saved as Lang)) return saved as Lang;
    } catch {
      // Storage may be unavailable.
    }
    const browser = navigator.language.slice(0, 2).toLowerCase();
    return supported.includes(browser as Lang) ? (browser as Lang) : 'en';
  }
}
