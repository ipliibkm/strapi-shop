import { Component, input, output } from '@angular/core';

/** One language row with its native display name and enabled state. */
export interface LanguageRow {
  code: string;
  /** Native display name (not translated). */
  name: string;
  enabled: boolean;
}

/**
 * Presentational list of enable/disable language rows. Holds no state or i18n — the consuming
 * app passes the rows in and reacts to toggle events (including any "keep at least one" guard).
 */
@Component({
  selector: 'openshopfront-language-list',
  standalone: true,
  templateUrl: './language-list.component.html',
})
export class OpenShopfrontLanguageListComponent {
  /** The language rows to render, in display order. */
  readonly languages = input.required<LanguageRow[]>();
  /** Accessible label for the group. */
  readonly label = input('Languages');

  /** A row's checkbox changed; carries the new enabled state. */
  readonly toggle = output<{ code: string; enabled: boolean }>();
}
