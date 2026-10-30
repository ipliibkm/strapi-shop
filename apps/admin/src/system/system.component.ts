import { HttpClient } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import {
  MAT_DIALOG_DATA,
  MatDialog,
  MatDialogModule,
  MatDialogRef,
} from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import {
  CapabilityRow,
  CapabilityTableLabels,
  OpenShopfrontCapabilityTableComponent,
} from '@openshopfront/ui';
import { firstValueFrom } from 'rxjs';
import { AdminI18n } from '../shared/i18n.service';
import { AdminStatusService } from '../shared/status.service';

/** The system view listing implemented capabilities and re-running backend checks. */
@Component({
  selector: 'ip-system',
  standalone: true,
  imports: [MatButtonModule, MatDialogModule, MatIconModule, OpenShopfrontCapabilityTableComponent],
  templateUrl: './system.component.html',
  styleUrls: ['./system.component.css'],
})
export class SystemComponent {
  private readonly http = inject(HttpClient);
  private readonly dialog = inject(MatDialog);
  readonly i18n = inject(AdminI18n);
  readonly status = inject(AdminStatusService);

  /** The test store currently being applied, or null while idle. */
  readonly mockApplying = signal<string | null>(null);
  /** The result message of the last apply run. */
  readonly mockNotice = signal<{ message: string; error: boolean } | null>(null);
  /** The capabilities reported on this view, with their implementation status. */
  private readonly capabilities = [
    { name: 'Availability checks', live: true, owner: 'Spring backend' },
    { name: 'Booking lifecycle', live: true, owner: 'Spring backend' },
    { name: 'Business Profile', live: true, owner: 'Strapi CMS' },
    { name: 'Page and navigation editing', live: true, owner: 'Admin page editor' },
    { name: 'Resource calendar mapping', live: true, owner: 'Booking backend' },
    { name: 'Google Calendar synchronization', live: true, owner: 'Integration service' },
    { name: 'Commerce and payments', live: true, owner: 'Commerce backend' },
    { name: 'Monitoring and reports', live: true, owner: 'Reports panel + Prometheus metrics' },
  ];

  langVersion(): string {
    return this.i18n.lang();
  }
  t(text: string, params?: Record<string, string | number>): string {
    return this.i18n.t(text, params);
  }
  /** The capability rows with translated names and owners. */
  capabilityRows(): CapabilityRow[] {
    return this.capabilities.map((item) => ({
      name: this.t(item.name),
      live: item.live,
      owner: this.t(item.owner),
    }));
  }
  tableLabels(): CapabilityTableLabels {
    return {
      capability: this.t('Capability'),
      status: this.t('Status'),
      owner: this.t('Owner'),
      available: this.t('AVAILABLE'),
      notImplemented: this.t('NOT IMPLEMENTED'),
    };
  }

  /** The test store buttons with translated labels. */
  mockStores(): { key: string; label: string }[] {
    return [
      { key: 'shoes', label: this.t('Shoe Store') },
      { key: 'flowers', label: this.t('Flower Store') },
      { key: 'tattoo', label: this.t('Tattoo Studio') },
      { key: 'bakery', label: this.t('Bakery') },
      { key: 'fashion', label: this.t('Fashion Store') },
    ];
  }

  /** Asks for confirmation before wiping the current content. */
  confirmMock(key: string) {
    if (this.mockApplying()) return;
    const store = this.mockStores().find((item) => item.key === key);
    const ref = this.dialog.open(MockConfirmDialog, {
      width: '460px',
      data: { label: store?.label ?? key },
    });
    void ref.afterClosed().subscribe((confirmed) => {
      if (confirmed) void this.applyMock(key);
    });
  }

  /** Replaces the application content with one test store profile. */
  async applyMock(key: string) {
    if (this.mockApplying()) return;
    this.mockApplying.set(key);
    this.mockNotice.set(null);
    try {
      const result = await firstValueFrom(
        this.http.post<{ products: number; services: number; pages: number }>(
          `/api/v1/admin/mock-data/${key}`,
          {},
        ),
      );
      this.mockNotice.set({
        message: this.t(
          'Test data applied: {products} products, {services} services and {pages} pages.',
          {
            products: result.products,
            services: result.services,
            pages: result.pages,
          },
        ),
        error: false,
      });
    } catch {
      this.mockNotice.set({
        message: this.t('The test data could not be applied. Try again.'),
        error: true,
      });
    } finally {
      this.mockApplying.set(null);
    }
  }
}

/** The confirmation dialog shown before the mock data wipes the current content. */
@Component({
  selector: 'ip-mock-confirm',
  standalone: true,
  imports: [MatButtonModule, MatDialogModule],
  template: `
    <h2 mat-dialog-title>{{ t('Replace all content?') }}</h2>
    <mat-dialog-content>
      <p class="muted">
        {{
          t(
            'This deletes every product, booking service and page in the application. Only the {store} test data will remain.',
            { store: store }
          )
        }}
      </p>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-stroked-button (click)="close(false)">{{ t('Cancel') }}</button>
      <button mat-raised-button color="warn" (click)="close(true)">
        {{ t('Replace all content') }}
      </button>
    </mat-dialog-actions>
  `,
})
export class MockConfirmDialog {
  private readonly ref = inject(MatDialogRef<MockConfirmDialog, boolean>);
  private readonly i18n = inject(AdminI18n);
  /** The store label shown in the warning text. */
  readonly store: string;

  constructor() {
    const data = inject(MAT_DIALOG_DATA) as { label?: string } | null;
    this.store = data?.label ?? '';
  }
  t(text: string, params?: Record<string, string | number>): string {
    return this.i18n.t(text, params);
  }
  close(confirmed: boolean) {
    this.ref.close(confirmed);
  }
}
