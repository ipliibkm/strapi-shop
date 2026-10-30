import { Component, inject, input } from '@angular/core';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatIconModule, MatIconRegistry } from '@angular/material/icon';
@Component({
  selector: 'openshopfront-shell',
  standalone: true,
  imports: [MatToolbarModule, MatIconModule],
  templateUrl: './shell.component.html',
  styleUrls: ['./shell.component.css'],
})
export class OpenShopfrontShellComponent {
  constructor() {
    inject(MatIconRegistry).setDefaultFontSetClass('material-symbols-outlined');
  }
  appName = input.required<string>();
  subtitle = input<string>('');
}
