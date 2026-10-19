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


## New admin and integration models and API work items (2026-10-19)
All paths below are proposals **not implemented**; exact names need OpenAPI-first approval.

### Application-owned tables (Liquibase)
- `GoogleConnection` (staff/owner association, provider subject, scopes, encrypted token reference, expiry, sync status, last error, revocation timestamps)
- `GoogleCalendarBinding` (staff/resource/calendar ID, direction and visibility/privacy policies)
- `ExternalEventMapping`, `CalendarSyncCheckpoint`, `CalendarChannelSubscription`, `CalendarConflict`, `CalendarBusyProjection`
- `OutboundNotification`, `EmailDeliveryAttempt`, `ReminderPreference`, `EmailTemplateReference`
- `ThemeDefinition`, `ThemeVersion`, `ThemeAssignment`, `UserAppearancePreference`, `BrandAssetReference`, `ThemeAuditEvent`
- Booking operations additionally require `BookingGroup`, `GroupSession`, `BookingRoster`, `ResourceAllocation`, `CapacityReservation`, `CalendarAdminAudit` when validated against the group-event requirements.
- `GoogleConnection` holds no raw credential in ordinary read responses; token values never enter logs or analytics.

### Internal authenticated operations API candidates
- `GET /api/v1/admin/bookings`, `POST /api/v1/admin/bookings`, `PATCH /api/v1/admin/bookings/{id}`, `POST /api/v1/admin/bookings/{id}/reschedule`, `POST /api/v1/admin/bookings/{id}/cancel`
- `GET /api/v1/admin/calendar`, `GET /api/v1/admin/resources/{id}/availability`, `GET /api/v1/admin/group-sessions`, `GET /api/v1/admin/group-sessions/{id}/roster`
- `GET /api/v1/admin/integrations/google`, `POST /api/v1/admin/integrations/google/connect`, `POST /api/v1/admin/integrations/google/disconnect`, `GET /api/v1/admin/integrations/google/sync-status` (OAuth callbacks dedicated, secured routes)
- `GET /api/v1/admin/themes`, `GET /api/v1/admin/themes/{id}/preview`, `PUT /api/v1/admin/appearance`, `PUT /api/v1/admin/appearance/default` (privileged action for site-wide theme)

### Security and correctness contracts
- OAuth 2.0 offline authorization and Google scopes are separately consented: Calendar events/freebusy for synchronization, optional Gmail `gmail.send` for email. Do not request full Gmail inbox/read access as a side effect.
- Define conflict matrix: booking confirmed vs Google event deleted, moved, overlapping or calendar disconnected; user chooses resolution workflow.
- Customer booking identifiers are opaque; admin role and resource ownership policy checked server-side for every staff calendar operation.
- Theme settings are tokenized, validated presets with theme schema versions; no arbitrary HTML/JS/CSS injection. Per-user preference needs precedence over global default defined.

## FINAL additions — bespoke Angular editor and two-way Google contract (2026-10-19)
**All ordinary content editing takes place in custom Angular admin.** Strapi stays API/schema management backend, not a business-facing UI.

### BFF endpoints and content models to specify in OpenAPI (proposals, not implemented)
`GET /api/v1/admin/content/types` (authorized editor schema and capabilities only), `GET /api/v1/admin/content/{type}`, `POST /api/v1/admin/content/{type}`, `GET/PATCH/DELETE /api/v1/admin/content/{type}/{documentId}`, `POST /api/v1/admin/content/{type}/{documentId}/publish`, `POST .../unpublish`, `POST .../preview`, `GET .../revisions`, `POST .../restore`; admin media upload/link, relations, validation, SEO/locale and navigation endpoints. Strapi 5 Document Service semantics, draft/publish, locales and dynamic zones need an explicit adapter specification and security/permissions model. Revision history is not to be assumed built-in without checking selected Strapi version/plugin license.

`GET /api/v1/admin/calendar/connections`, `POST /api/v1/admin/calendar/connections/connect`, OAuth callback, `DELETE /api/v1/admin/calendar/connections/{id}`, `GET /api/v1/admin/calendar/conflicts`, `POST /api/v1/admin/calendar/conflicts/{id}/resolve`, `GET /api/v1/admin/calendar/sync-health`, `POST /api/v1/admin/calendar/resync` (admin audited).
`GET /api/v1/admin/appearance/themes`, `PUT /api/v1/admin/appearance/me` (admin user), `PUT /api/v1/admin/appearance/storefront` (authorized publishing), `GET /api/v1/admin/appearance/preview` (no visitor publication). Public storefront theme resolution/API with cache invalidation and SSR awareness.
`GET /api/v1/admin/notifications/delivery`, `POST /api/v1/admin/notifications/test-send` (rate limited), `GET /api/v1/admin/integrations/gmail` (send-only scope and consent).

### State/consistency rules
Source of truth: platform booking records are authoritative; external Google event changes are processed via import/matching/conflict rules decided in B01; never blindly cancel bookings. Use event ID plus calendar + organizer identity, sync tokens and etag/revision; watch lifecycle renewal, retry/backoff/dedupe, full resync after invalidated tokens. Secure OAuth tokens; least privilege scopes and redacted busy-only projections where full event content not necessary. Gmail send-only queue supports idempotent messages, status/retry, provider quota and revoked-consent states.
Themes: `ThemeDefinition`, `ThemeVersion`, `AdminUserThemePreference`, `StorefrontPublishedTheme`, `ThemePublicationAudit`; distinct settings and authorization boundaries.
