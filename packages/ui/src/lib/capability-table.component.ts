import { Component, input } from '@angular/core';

/** One capability row with its implementation status and owner. */
export interface CapabilityRow {
  /** Translated capability name. */
  name: string;
  live: boolean;
  /** Translated owner label. */
  owner: string;
}

/** Column/status strings for the table, translated by the consuming app. */
export interface CapabilityTableLabels {
  capability: string;
  status: string;
  owner: string;
  available: string;
  notImplemented: string;
}

/** Presentational implementation-status table used on the system page. */
@Component({
  selector: 'openshopfront-capability-table',
  standalone: true,
  templateUrl: './capability-table.component.html',
})
export class OpenShopfrontCapabilityTableComponent {
  /** The capability rows to render. */
  readonly rows = input.required<CapabilityRow[]>();
  /** Translated column and status strings. */
  readonly labels = input.required<CapabilityTableLabels>();
}
