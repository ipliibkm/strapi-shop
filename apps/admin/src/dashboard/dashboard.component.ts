import { Component, inject, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { OpenShopfrontMetricCardComponent } from '@openshopfront/ui';
import { AdminI18n } from '../shared/i18n.service';
import { AdminStatusService } from '../shared/status.service';
import { Section } from '../shared/types';

/** The setup view showing which services are configured and running. */
@Component({
  selector: 'ip-dashboard',
  standalone: true,
  imports: [MatButtonModule, MatIconModule, OpenShopfrontMetricCardComponent],
  templateUrl: './dashboard.component.html',
})
export class DashboardComponent {
  readonly i18n = inject(AdminI18n);
  readonly status = inject(AdminStatusService);
  /** Requests the shell to open another section. */
  readonly navigate = output<Section>();

  langVersion(): string {
    return this.i18n.lang();
  }
  t(text: string, params?: Record<string, string | number>): string {
    return this.i18n.t(text, params);
  }
}
