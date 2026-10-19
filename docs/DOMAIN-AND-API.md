# Domain Model, API and Workflow Specification

All models below are **planning inventory**, not created Strapi collections nor endpoints. Record each item with ownership, access policy, relations, indexes, required fields, validation, lifecycle, localization, audit/event and migration plan before coding.

## Strapi-owned content types (CMS PostgreSQL)
### Core content
SiteSettings; Brand; Locale; Menu and MenuItem; Page; PageBlock (typed components/dynamic zone with versioned schema); HomePage; Header/Footer; Redirect; SeoMetadata; SocialMetadata; StructuredData; MediaAsset; FormDefinition; FormSubmission (if approved for PII handling); ContactPage; FAQ; Testimonial; TeamMember; PolicyPage; Announcement; Campaign; Banner; NewsletterContent; Taxonomy/Tag; CategoryContent; BlogArticle; Author; ProductEditorialContent (stable external SKU/product ID); ServiceEditorialContent (stable external service ID); LocationEditorialContent (external resource/location ID).
### Optional content
EventPage; LandingPage; KnowledgeBase; CaseStudy; Portfolio; Gallery; DownloadableAsset; PromotionContent; EmailTemplateContent.
CMS requirements: localization/i18n, draft/publish workflows, component constraints, preview, staging, image formats, SEO slugs + redirects, noindex/canonical, sitemap, Robots rules, revision/history policy, media transformations/CDN, data export/seed and import, editorial RBAC. Validate plugin licenses/features and capabilities for the selected Strapi release.

## Commerce-owned entities (Liquibase)
TenantOrBusiness if multi-tenant; Channel; Catalog; Product; ProductVariant/SKU; ProductAttribute; ProductOption; ProductCategoryLink; PriceList; Money/Currency; TaxClass; TaxRule/TaxProviderMapping; Promotion; Coupon; Customer; CustomerAddress; Cart; CartItem; Wishlist; InventoryLocation; StockLevel; StockReservation; StockMovement; CheckoutSession; Order; OrderLine; OrderStatusHistory; PaymentAttempt; PaymentTransaction; PSPWebhookDelivery; Refund; CreditNote; Shipment; ShippingMethod; ShippingRate; Fulfillment; ReturnRequest/RMA; ReturnLine; Exchange; InvoiceReference; GiftCard or StoreCredit (optional); OrderNote; AuditLog; IdempotencyKey; OutboxEvent.
Decide complexity of digital goods, variants, multi-warehouse, subscriptions, B2B, multi-currency, partial fulfillment and multi-vendor **before** implementing. Monetary amounts in integer minor units or validated decimal with explicit currency; never floating-point money. Tax rounding specified. Inventory invariants enforced with DB transactions/locks and idempotency.
State machines: Cart active/converted/expired; order pending/confirmed/processing/shipped/delivered/cancelled/refunded; payment initiated/authorized/captured/failed/refunded/chargeback; inventory reserved/committed/released/expired; returns requested/approved/received/refunded/rejected. Validate allowed transitions and authorization.

## Booking-owned entities (Liquibase)
ServiceOffering; Resource (staff/room/equipment); ResourceGroup; Location; AvailabilityRule; OpeningHour; TimeOff; Holiday; CalendarConnection; CalendarSyncCursor; AvailabilityException; RecurrenceRule; CapacityRule; AppointmentSlot (generated/projection, not necessarily stored); BookingHold; Booking; BookingAttendee; BookingStatusHistory; WaitlistEntry; BookingPaymentLink; BookingPolicy; Cancellation; Reschedule; ReminderSchedule; ExternalCalendarEventMapping; WebhookDelivery; AuditLog; IdempotencyKey; OutboxEvent.
Specify IANA timezone per location/resource, DST skipped/repeated local times, recurrence exceptions, buffer windows, lead time, minimum/maximum notice, staff overrides, partial availability, group capacity, overbooking rules, hold TTL and no-double-booking locking. ICS/iCal, CalDAV/Google/Microsoft providers optional, respecting scopes, sync direction, conflict resolution and reauthorization.

## API surface: example resources (must be refined into OpenAPI)
### Public CMS, mostly read-only
`GET /api/v1/content/pages/{slug}`, `GET /api/v1/content/navigation`, `GET /api/v1/content/site-settings`, `GET /api/v1/content/articles`, `GET /api/v1/content/sitemap-data`.
Strapi-native REST can be consumed through a hardened BFF; do not claim these exact paths exist automatically.

### Commerce public/customer
`GET /api/v1/products`, `GET /api/v1/products/{id}`, `GET /api/v1/categories`, `GET /api/v1/search`, `POST /api/v1/carts`, `GET /api/v1/carts/{id}`, `POST /api/v1/carts/{id}/items`, `PATCH /api/v1/carts/{id}/items/{itemId}`, `POST /api/v1/checkout-sessions`, `POST /api/v1/orders`, `GET /api/v1/orders/{id}`, `POST /api/v1/checkout-sessions/{id}/payment-intents`, `POST /api/v1/payments/webhooks/{provider}`, `POST /api/v1/returns`, `GET /api/v1/customer/me`. Authentication/authorization varies per operation; webhooks require signature verification. GET order uses ownership checks, not predictable ID alone.

### Booking public/customer
`GET /api/v1/services`, `GET /api/v1/resources`, `GET /api/v1/availability?serviceId=...&from=...&to=...&timezone=...`, `POST /api/v1/booking-holds`, `POST /api/v1/bookings`, `GET /api/v1/bookings/{id}`, `POST /api/v1/bookings/{id}/cancel`, `POST /api/v1/bookings/{id}/reschedule`, `GET /api/v1/customer/me/bookings`. Admin operations require scoped staff permissions.

### Operations/internal
`/api/v1/admin/orders/*`, `/api/v1/admin/products/*`, `/api/v1/admin/inventory/*`, `/api/v1/admin/bookings/*`, `/api/v1/admin/resources/*`, `/api/v1/admin/customers/*`, `/api/v1/admin/reports/*`; `GET /health/live`, `GET /health/ready`, `GET /metrics` internal-only.

## Canonical workflow definitions to produce as sequence diagrams
1. Author/editor creates draft, previews, publishes, webhook invalidates cached pages and rebuilds search/sitemap.
2. Guest or customer browses filtered products, builds cart, applies discount, taxes and shipping quoted by server, stock reserved atomically.
3. PSP session/intent initiated; browser redirected; signed webhook received (may arrive earlier/later/repeated); update order/payment idempotently, outbox triggers fulfillment/receipts; reconciliation corrects missing webhooks.
4. Checkout fails/expires; release stock/payment auth as applicable; retry safely.
5. Partial refund, cancellation, returns, chargeback and order-to-invoice reconciliation.
6. Availability calculated, time slot hold created atomically, optional deposit payment, booking confirmed, external calendar synced, notification dispatched.
7. Two concurrent callers request last slot: one wins; expired hold frees slot.
8. Reschedule/timezone/DST, late cancellation/no-show, external calendar conflict and sync recovery.
9. Account deletion/export where legal retention of financial records remains necessary.

## Contract governance
OpenAPI 3.1 + JSON Schema; version every public contract and async event; error schemas, permissions/scopes, HTTP caching, cursor pagination, request idempotency key and retry policy. Consumers generate typed Angular clients; consumer-driven compatibility tests; schema evolution policy and sample fixtures. Contract first, implementation later.
