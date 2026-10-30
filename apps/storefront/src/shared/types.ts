/** The storefront pages the shell can route to. */
export type Page =
  'home' | 'services' | 'booking' | 'shop' | 'product' | 'cart' | 'order' | 'about' | 'contact';

/** A website language published by the administrator. */
export interface StorefrontLanguage {
  code: string;
  name: string;
  enabled: boolean;
}

/** One availability window reported by the booking backend. */
export interface PublicAvailability {
  available: boolean;
  startsAt: string;
  endsAt: string;
}

/** One content block of a published page, in display order. */
export interface PageBlock {
  type: 'heading' | 'text' | 'image' | 'cta' | 'products' | 'form';
  eyebrow?: string;
  title?: string;
  html?: string;
  url?: string;
  caption?: string;
  label?: string;
  target?: Page | string;
  limit?: number;
  formSlug?: string;
}

/** A published page built from content blocks. */
export interface PublishedPage {
  slug: string;
  title: string;
  enabled: boolean;
  blocks: PageBlock[];
}

/** One entry of the public page list used to filter navigation. */
export interface PublicPageSummary {
  slug: string;
  title: string;
}

/** One field definition of a storefront form. */
export interface StorefrontFormField {
  key: string;
  label: string;
  type: 'text' | 'email' | 'tel' | 'textarea' | 'select' | 'checkbox';
  required: boolean;
  placeholder: string;
  helpText: string;
  minLength: number;
  maxLength: number;
  options: string[];
}

/** A storefront form definition with its fields. */
export interface StorefrontForm {
  slug: string;
  name: string;
  submitLabel: string;
  successMessage: string;
  fields: StorefrontFormField[];
}

/** One product in the public catalog list. */
export interface Product {
  id: string;
  slug: string;
  name: string;
  description: string;
  /** The main image address, or empty when the product has no photo. */
  imageUrl?: string;
  /** All product image addresses in display order. */
  imageUrls?: string[];
  variantCount: number;
}

/** One selectable variant of a product. */
export interface VariantOption {
  id: string;
  label?: string | null;
  priceCents: number;
  currency: string;
  availableQuantity: number;
}

/** A product with its purchasable variants. */
export interface ProductDetail extends Product {
  variants: VariantOption[];
}

/** One line in the customer's cart, persisted per browser. */
export interface CartLine {
  variantId: string;
  productName: string;
  label?: string | null;
  priceCents: number;
  quantity: number;
  availableQuantity?: number;
}

/** The result of placing an order at checkout. */
export interface CheckoutResult {
  orderNumber: string;
  accessToken: string;
  status: string;
  checkoutUrl?: string | null;
  paymentRequired?: boolean;
}

/** A public order summary for the order-status page. */
export interface OrderSummary {
  orderNumber: string;
  status: string;
  totalCents: number;
  currency: string;
  createdAt?: string;
}

/** One bookable service offering with its duration. */
export interface BookingOffering {
  id: string;
  name: string;
  durationMinutes: number;
}

/** The published booking settings for the storefront. */
export interface BookingSettings {
  customerBookingEnabled: boolean;
  defaultDurationMinutes: number;
  unavailableReason?: string;
}

/** A confirmed booking returned by the backend. */
export interface BookingConfirmation {
  id: string;
  serviceName: string;
  durationMinutes: number;
  startsAt: string;
  endsAt: string;
  status: string;
}
