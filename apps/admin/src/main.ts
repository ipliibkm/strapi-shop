import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule, MatIconRegistry } from '@angular/material/icon';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { bootstrapApplication } from '@angular/platform-browser';
import { OpenShopfrontThemeService } from '@openshopfront/ui';
import { LANGUAGES, Lang } from './shared/i18n';
import { AdminI18n } from './shared/i18n.service';
import { AdminAuth } from './shared/auth.service';
import { adminAuthInterceptor } from './shared/auth.interceptor';
import { BookingComponent } from './booking/booking.component';
import { BookingServicesComponent } from './booking-services/booking-services.component';
import { BusinessProfileComponent } from './business-profile/business-profile.component';
import { PagesComponent } from './pages/pages.component';
import { CommerceComponent } from './commerce/commerce.component';
import { ReportsComponent } from './reports/reports.component';
import { FormsComponent } from './forms/forms.component';
import { EmailSettingsComponent } from './email-settings/email-settings.component';
import { OverviewComponent } from './overview/overview.component';
import { DashboardComponent } from './dashboard/dashboard.component';
import { IntegrationsComponent } from './integrations/integrations.component';
import { AppearanceComponent } from './appearance/appearance.component';
import { SystemComponent } from './system/system.component';
import { AdminStatusService } from './shared/status.service';
import { AdminUiSettingsService } from './shared/admin-ui-settings.service';
import { Section } from './shared/types';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    BookingComponent,
    BookingServicesComponent,
    BusinessProfileComponent,
    PagesComponent,
    FormsComponent,
    EmailSettingsComponent,
    CommerceComponent,
    ReportsComponent,
    OverviewComponent,
    DashboardComponent,
    IntegrationsComponent,
    AppearanceComponent,
    SystemComponent,
    MatButtonModule,
    MatIconModule,
  ],
  templateUrl: './main.html',
})
class AdminApp {
  readonly theme = inject(OpenShopfrontThemeService);
  readonly i18n = inject(AdminI18n);
  /** Interface languages offered in the top bar, limited to enabled admin UI languages. */
  readonly languageOptions = computed(() => {
    const loaded = this.adminUi.languages();
    if (!loaded) return Object.values(LANGUAGES); // show all while settings load
    const byCode = new Map(Object.entries(LANGUAGES).map(([code, meta]) => [code, meta]));
    return loaded
      .filter((item) => item.enabled && byCode.has(item.code))
      .map((item) => byCode.get(item.code)!);
  });
  readonly auth = inject(AdminAuth);
  readonly status = inject(AdminStatusService);
  readonly adminUi = inject(AdminUiSettingsService);
  readonly section = signal<Section>(this.initialSection());
  /** In-app view history for the back button; browser history is not used. */
  private navStack: Section[] = [];
  readonly canGoBack = signal(false);
  readonly menuOpen = signal(false);
  readonly signingIn = signal(false);
  readonly navigation: { key: Section; label: string; icon: string }[] = [
    { key: 'overview', label: 'Overview', icon: 'insights' },
    { key: 'bookings', label: 'Bookings', icon: 'calendar_month' },
    { key: 'content', label: 'Content', icon: 'edit_note' },
    { key: 'resources', label: 'Resources', icon: 'groups' },
    { key: 'pages', label: 'Pages', icon: 'dashboard_customize' },
    { key: 'forms', label: 'Forms', icon: 'dynamic_form' },
    { key: 'email', label: 'Email', icon: 'mail' },
    { key: 'integrations', label: 'Connections', icon: 'hub' },
    { key: 'commerce', label: 'Commerce', icon: 'storefront' },
    { key: 'reports', label: 'Reports', icon: 'query_stats' },
    { key: 'dashboard', label: 'Setup', icon: 'build_circle' },
    { key: 'appearance', label: 'Appearance', icon: 'palette' },
    { key: 'system', label: 'System', icon: 'monitor_heart' },
  ];

  constructor() {
    inject(MatIconRegistry).setDefaultFontSetClass('material-symbols-outlined');
    this.theme.load('admin');
    void this.initialize();
  }

  /** Reads the active language so templates track it and re-render on change. */
  langVersion(): Lang {
    return this.i18n.lang();
  }
  /** Translates one interface string for the active admin language. */
  t(text: string, params?: Record<string, string | number>): string {
    return this.i18n.t(text, params);
  }
  sidebarStatusLabel(): string {
    if (!this.auth.user()?.superAdmin) return this.t('Strapi content access');
    return this.status.backendOnline() ? this.t('Backend online') : this.t('Backend unavailable');
  }
  setLang(code: Lang) {
    this.i18n.setLang(code);
  }
  onLangChange(event: Event) {
    const value = (event.target as HTMLSelectElement).value;
    if ((Object.keys(LANGUAGES) as Lang[]).includes(value as Lang)) this.setLang(value as Lang);
  }
  sectionLabel() {
    return this.navigation.find((item) => item.key === this.section())?.label ?? 'Overview';
  }
  visibleNavigation() {
    return this.auth.user()?.superAdmin
      ? this.navigation
      : this.navigation.filter((item) => item.key === 'content');
  }
  select(section: Section, recordHistory = true) {
    if (this.auth.user() && !this.auth.user()?.superAdmin && section !== 'content')
      section = 'content';
    const current = this.section();
    if (recordHistory && section !== current) {
      this.navStack.push(current);
      if (this.navStack.length > 50) this.navStack.shift();
      this.canGoBack.set(true);
    }
    this.section.set(section);
    window.history.replaceState(
      {},
      '',
      section === 'overview' ? window.location.pathname : `#${section}`,
    );
    this.menuOpen.set(false);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }
  /** Returns to the previously opened view without using browser history. */
  goBack() {
    const previous = this.navStack.pop();
    this.canGoBack.set(this.navStack.length > 0);
    if (previous !== undefined) this.select(previous, false);
  }
  private async initialize() {
    await this.auth.initialize();
    await this.loadAuthorizedWorkspace();
  }
  async signIn(event: Event, email: string, password: string) {
    event.preventDefault();
    this.signingIn.set(true);
    try {
      if (await this.auth.login(email, password)) await this.loadAuthorizedWorkspace();
    } finally {
      this.signingIn.set(false);
    }
  }
  private async loadAuthorizedWorkspace() {
    if (!this.auth.user()) return;
    if (!this.auth.user()?.superAdmin) {
      this.select('content', false);
      return;
    }
    await Promise.all([this.status.refreshStatus(), this.adminUi.load()]);
  }
  private initialSection(): Section {
    const value = window.location.hash.slice(1);
    return [
      'overview',
      'dashboard',
      'bookings',
      'content',
      'resources',
      'pages',
      'forms',
      'email',
      'integrations',
      'commerce',
      'reports',
      'appearance',
      'system',
    ].includes(value)
      ? (value as Section)
      : 'overview';
  }
}

bootstrapApplication(AdminApp, {
  providers: [
    provideAnimationsAsync(),
    provideHttpClient(withInterceptors([adminAuthInterceptor])),
  ],
}).catch(console.error);
