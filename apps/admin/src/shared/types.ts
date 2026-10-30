/** The in-app views an administrator can open. */
export type Section =
  | 'overview'
  | 'dashboard'
  | 'bookings'
  | 'content'
  | 'resources'
  | 'pages'
  | 'forms'
  | 'email'
  | 'integrations'
  | 'commerce'
  | 'reports'
  | 'appearance'
  | 'system';

/** One backend configuration check with its status and message. */
export interface SetupCheck {
  id: string;
  status: string;
  message: string;
}

/** Headline business figures for the overview page. */
export interface OverviewStats {
  bookings: number;
  submissions: number;
  paidOrders: number;
  cancelledOrders: number;
  revenueCents: number;
  refundedCents: number;
  unitsSold: number;
}

/** A resource-to-calendar mapping reported by the Google integration. */
export interface GoogleMapping {
  resourceId: string;
  calendarId: string;
  displayName: string;
  writeBookings: boolean;
  syncStatus: string;
  lastSyncedAt?: string;
  conflictCount: number;
  lastError?: string;
}

/** The Google Calendar connection state returned by the backend. */
export interface GoogleConnection {
  connected: boolean;
  health?: string;
  email?: string;
  scopes?: string[];
  expiresAt?: string;
  refreshable?: boolean;
  mappings?: GoogleMapping[];
  error?: string;
}

/** One selectable Google calendar. */
export interface GoogleCalendar {
  id: string;
  name: string;
  primary: boolean;
  accessRole: string;
}

/** One selectable website language with its native display name. */
export interface StorefrontLanguage {
  code: string;
  name: string;
  enabled: boolean;
}

/** The published storefront presentation settings returned by the backend. */
export interface StorefrontAppearance {
  languages: StorefrontLanguage[];
  theme: string;
  customPalette: string | null;
}

/** One selectable admin interface language with its native display name. */
export interface AdminUiLanguage {
  code: string;
  name: string;
  enabled: boolean;
}
