import { Injectable, signal } from '@angular/core';
import { Lang, localeOf, translate } from './i18n';

/**
 * Holds the storefront's active language and translates interface strings for it. The shell
 * owns this service; every page component reads `lang()` so its view re-renders on change.
 */
@Injectable({ providedIn: 'root' })
export class StorefrontI18n {
  /** The language currently shown to this visitor. */
  readonly lang = signal<Lang>('en');

  /** Translates one interface string for the active website language. */
  t(key: string, params?: Record<string, string | number>): string {
    return translate(this.lang(), key, params);
  }

  /** The BCP-47 locale of the active language, for dates and prices. */
  locale(): string {
    return localeOf(this.lang());
  }
}
