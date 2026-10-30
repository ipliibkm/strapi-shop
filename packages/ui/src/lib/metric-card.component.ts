import { Component, input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

/** A single statistic card with a tinted icon, label, value and hint. */
@Component({
  selector: 'openshopfront-metric-card',
  standalone: true,
  imports: [MatIconModule],
  templateUrl: './metric-card.component.html',
})
export class OpenShopfrontMetricCardComponent {
  /** Material symbol name for the card icon. */
  readonly icon = input.required<string>();
  /** Color tone: blue, amber, green or violet (maps to a CSS modifier). */
  readonly tone = input('blue');
  /** Translated metric label. */
  readonly label = input.required<string>();
  /** The formatted value to display (already a string or number). */
  readonly value = input<string | number>('–');
  /** Translated supporting hint line. */
  readonly hint = input('');
}
