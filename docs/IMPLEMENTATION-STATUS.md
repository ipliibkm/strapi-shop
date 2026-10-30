> **Writing standard:** Follow [WRITING-STYLE.md](WRITING-STYLE.md). Earlier text in this file has not received a complete language review.

# Implementation status — 2026-10-26

The repository contains a working development platform for storefront content, products, browser-local carts, transactional checkout, bookings, forms, administration, reports, and provider integrations. Docker builds and browser smoke tests have run successfully. The platform is **not production-ready**. See [`prod-readiness.md`](../prod-readiness.md) for release blockers.

## Current verified scope

- Spring Boot Java 21 backend with deny-by-default security, public storefront APIs, administrator APIs, Liquibase migrations, and Actuator health.
- PostgreSQL with separate application and Strapi databases.
- Strapi 5 content, page, form, and submission ownership.
- Angular 22 storefront, administrator, setup application, and shared UI library.
- Docker Compose services behind Traefik for Storefront, Admin, CMS, and Storybook.
- Product variants, current availability, browser-local cart persistence, server-authoritative checkout totals, transactional stock reservations, order actions, Stripe hosted checkout, and signed webhooks.
- Booking availability, conflict checks, Google Calendar connection, Gmail sending, reusable storefront forms, reports, published themes, and four storefront interface languages.

## Important limits

- Production identity, authorization roles, rate limits, backups, monitoring alerts, legal review, load tests, and recovery drills remain incomplete.
- Local HTTPS uses a generated certificate. Browsers display a certificate warning.
- Storefront page and product content is not localized automatically. The selected interface language changes labels only.
- The browser cart does not reserve stock. Checkout performs the authoritative stock check and creates the reservation.
- Stripe live mode requires deployment-specific keys, webhook configuration, and production verification.

## Current review status — 2026-10-26

All six findings from the 2026-10-26 review are fixed in the working tree.

- The backend test suite passes all 98 tests.
- The Admin, Storefront, Storybook, and CMS container builds pass with Node.js 24.
- The production Compose configuration check passes. All seven running Compose services report healthy.
- Form deletion checks the submission owner before it deletes the record.
- Email subject validation uses language-map keys and rejects invalid keys.
- Public orders use a 128-bit number and a separate 256-bit access token. Lookup requests have a rate limit.
- Strapi sessions are the only supported administrator authentication mode.
- Mail delivery logs contain neither recipient addresses nor provider exception bodies.
- The frontend format check passes.
- The CMS production dependency audit reports 15 moderate transitive findings.

## Historical implementation log

The sections below record incremental work. An older limitation can be superseded by a later dated section.

## Implementation continuation — 2026-10-19

### Added in this iteration (source committed, integration not validated)

- Java booking domain API: availability read, operator booking list/create, 409 conflict response and resource-scoped PostgreSQL advisory transaction locks.
- External busy projection upsert service uses matching lock to avoid booking concurrency races **once events have synchronized**.
- Explicit development-only HTTP Basic admin access (Spring profile `dev` with `DEV_ADMIN_PASSWORD`). This historical fallback remains separate from production Strapi sessions.
- Angular admin booking form, availability check, error handling/refetch after HTTP 409, and shared Angular Material components and Tailwind layout.
- GitHub Actions backend/frontend build workflows and a servlet security controller test skeleton.
- Strapi pinned to 5.58.0 (released Oct 14 2026). Dependency compatibility and actual installation still must be verified.

### Outstanding high-priority safety and completeness gaps

- Google Calendar two-way OAuth, actual ingest of events, sync freshness and provider failure policy: **do not take real appointments yet**. Imported conflicts after an already-confirmed booking need manual conflict queue handling.
- Full customer/service/resource/calendar model, group capacity, booking reschedule/cancellation, idempotency, timezone/daylight savings and opening hours.
- No live Stripe API/webhooks, Gmail OAuth/send, Strapi editor BFF or complete content models.
- Production MFA, rate limiting, CSRF policies, TLS gateway, monitoring, backup, and restore are not deployed.
- Complete Storybook setup, lockfiles, Angular Material theme CSS, SSR, user theme persistence/backend, quality gates and Docker image validation remain.
- **Historical note, superseded:** Compose initially started only Backend, CMS, and PostgreSQL. The current stack also includes Traefik, Storefront, Admin, and Storybook. The Setup application runs only through the Angular development server.
- Github repository remains PUBLIC despite requirement for PRIVATE. This needs a repo settings action.

### Verification status

Files committed to GitHub; no network-enabled build/test execution performed in this response. Added Actions YAML does not prove tests passed; consult workflow run results after invocation.

## First-run business installer — 2026-10-19

- [x] Commit `sh setup.sh` / `python3 scripts/setup.py` interactive first-run wizard; company name/slug/contact, timezone, locale, currency, optional address and phone; generated random credentials (private .env), confirmation and resumed start.
- [x] Add Strapi `business-profile` single type and seed-once CMS bootstrap, mounted config file, Compose container health checks, supported Strapi interactive technical admin creation command.
- [ ] Verify full image builds, `compose up`, HTTP health polling, CMS seed and admin CLI against a clean machine. Not tested in a running Docker engine during this turn.
- [ ] Integrate browser setup with the approved Strapi administrator enrollment and role process. The current step creates only the technical Strapi administrator.
- [ ] Implement TLS/reverse proxy, offsite backups, restoration, role isolation and readiness/security tests before declaring business one-click deployment production-ready.
      See [FIRST-RUN-SETUP.md](FIRST-RUN-SETUP.md).

## Browser setup milestone

Added Angular Material + Tailwind setup wizard with company details, first operator record, modules, themes, integration guidance and a one-time setup API. The API stores a BCrypt password hash and a PostgreSQL setup state. It uses a separate setup token. See [BROWSER-SETUP.md](BROWSER-SETUP.md).

**Current remaining work:** production identity approval, secure token rotation, trusted production TLS rehearsal, backups, monitoring alerts, rate limits, legal review, load tests, and recovery drills. Later sections record the implemented Google, Gmail, Stripe, content, build, and browser-test increments.

## Setup verification increment — 2026-10-19

- [x] Add `GET /setup/api/checks` for DB, installation state, Google configuration, Stripe configuration and identity configuration. Checks are read-only. They do not validate live external connections.
- [x] Add setup wizard **Check system settings** control with visible status messages.
- [x] Add initial unit tests for invalid bootstrap setup tokens.
- [x] Add clear provider onboarding steps in `docs/INTEGRATIONS-ONBOARDING.md`.
- [ ] Complete production Strapi administrator enrollment, MFA, recovery, and lockout procedures.
- [ ] Implement OAuth authorization code flow for Google Calendar and Gmail. No real Google connection exists.
- [ ] Implement Stripe Connect or account-specific Stripe setup flow with webhook signing.
- [ ] Reconcile browser setup business details with Strapi profile.
- [x] Later increments ran backend, frontend, Docker, and browser verification. See the dated sections below for scope and remaining gaps.

## External OIDC sign-in experiment — 2026-10-19

**Superseded on 2026-10-26:** This experiment added a resource-server chain and an Angular bearer-token client. The client was later replaced by the Strapi session flow. The unsupported OIDC switch, backend resource-server configuration, and frontend OIDC package are removed. Strapi sessions are now the only supported administrator authentication mode. See [OIDC-AUTH.md](OIDC-AUTH.md).

## Customer storefront UI — 2026-10-19

- [x] Replace the original placeholder with responsive Home, Services, Shop, About, and Contact views.
- [x] Add mobile navigation, sample service cards, and Material + Tailwind styles.
- [x] Add a static Nginx container for the Storefront. Traefik now exposes it at `storefront.localhost`; the earlier direct port 8081 route is superseded.
- [x] Verify the Angular production build and Storefront Docker image end to end.
- [x] Connect CMS content, themes, media, booking, products, checkout, and customer forms. Mock profiles remain sample content and must be replaced before launch.
- [ ] Add SEO-ready SSR or prerendering and deployment TLS before public launch.
      See [STOREFRONT.md](STOREFRONT.md).

## Page builder and commerce — 2026-10-22

- [x] Strapi owns the six storefront pages and their draft/publish lifecycle. Each page has a title, slug, enabled state, and ordered content sections. Spring validates and sanitizes administrator changes before it sends them to Strapi. The admin preview uses the storefront application and displays unsaved content through `postMessage`. The storefront removes disabled pages from navigation and rejects direct access to them.
- [x] Commerce module: products with purchasable options, stock plus reservation semantics, server-authoritative pricing (tax rate, flat shipping, optional free-shipping threshold), checkout that holds stock, order lifecycle PENDING_PAYMENT → PAID → FULFILLED / CANCELLED, fulfillment with carrier and tracking number, one-time refund with restock of shipped items.
- [x] Payments: Stripe hosted Checkout Session over the raw REST API (no SDK dependency), HMAC webhook verification on `/webhooks/stripe`, idempotent event handling via `stripe_event`. Without STRIPE_SECRET_KEY the platform runs in manual mode; an admin marks orders paid.
- [x] Liquibase 012 (`storefront_page`) remains as a legacy table and is not used. Liquibase 013 adds the commerce tables. The backend test suite has 56 passing tests. The admin, storefront, and Strapi applications compile in their Docker builds.
- [ ] Stripe keys and webhook secret are not set in this environment; manual mode is the current default. Live card flow needs a configured key before customer use.

## Monitoring and reports — 2026-10-22

- [x] Prometheus metrics: added `micrometer-registry-prometheus`, exposed `/actuator/prometheus` (permit-listed in the deny-by-default security chain), tagged with the application name. Business counters recorded at each order transition: `commerce.orders.created`, `commerce.checkout.failed{reason=OUT_OF_STOCK}`, `commerce.orders.paid`, `commerce.orders.cancelled`, `commerce.orders.fulfilled`, `commerce.orders.refunded`. JVM, HTTP and system meters are available as well.
- [x] Reporting API (`/api/v1/admin/reports/**`, SUPER_ADMIN only): summary KPIs, daily trend, top products, inventory alerts, sales funnel by order status, paged order ledger, and a streaming CSV export of the order ledger. All money is EUR minor units; all ranges are half-open UTC intervals so figures reconcile to the order table (P9-08).
- [x] Admin **Reports** section: KPI cards, trend/funnel/top-product/inventory tables, date-range presets, and CSV downloads (trend, inventory alerts, order ledger). The "Monitoring and reports" capability row is now live.
- [x] The backend test suite includes UTC boundary tests and public form API tests. All 56 tests pass. MANUAL.html documents the Reports panel, the Prometheus scrape address, CSV handling, and the data-retention note; current-limits cards updated accordingly.
- [ ] Alerting rules, a hosted Grafana/Prometheus sidecar in Compose, automated backups/restore tests, and high availability remain open (P9 scope). Roles beyond SUPER_ADMIN for read-only analysts are not implemented yet (P9-02).

## Form submission inbox — 2026-10-22

- [x] Submissions are first-class **Strapi content**: each accepted value is a `form-submission` document that links to its `storefront-form`. Strapi owns the definition and the data, so submissions appear in the CMS alongside every other record. The old raw Postgres table (`storefront_form_submission`) is no longer used by Spring.
- [x] A new `form-submission` collection type stores `payload` (JSON), `submittedAt`, a many-to-one relation to its form, and a stable `formDocumentId`. Anonymous storefront writes use the long-lived `STRAPI_SERVICE_TOKEN`; administrator reads, exports, and deletions reuse the signed-in Strapi session.
- [x] The admin Forms workspace has **Builder** and **Submissions** tabs per form. The Submissions tab shows the total count, and selecting another form resets the active tab and pagination.
- [x] `GET /api/v1/admin/forms/{slug}/submissions` returns a paged body `{items, page, pageSize, total}`, newest first. `pageSize` is clamped to 100; results stay filtered to the selected form slug.
- [x] The submissions table builds columns from the form definition (field labels as headings), shows dates in the administrator's local timezone, renders checkbox values as Yes/No, and has an empty state plus a manual refresh button.
- [x] A submission detail dialog lists every configured field in order with its submitted value, preserves textarea line breaks, displays the submission ID and timestamp, and never injects values as HTML (Angular text interpolation).
- [x] `GET /api/v1/admin/forms/{slug}/submissions.csv` exports all rows using field order, includes the submission ID and timestamp, escapes spreadsheet formulas (`=`, `+`, `-`, `@`) and commas, returns UTF-8 with an `attachment; filename="form-{slug}-submissions.csv"` header.
- [x] `DELETE /api/v1/admin/forms/{slug}/submissions/{id}` removes one record after a confirmation dialog. It verifies the submission belongs to that form and returns a validation error otherwise. No bulk deletion is added.
- [x] All endpoints are administrator-only (SUPER_ADMIN) and reuse the existing CSRF cookie flow for mutations; submitted values are treated as untrusted text and are not written to logs.
- [x] Backend tests cover anonymous denial, pagination/total counts, newest-first sorting, form-specific filtering, CSV field order and formula escaping, successful deletion, wrong-slug deletion, and unknown-ID deletion. The final 2026-10-26 suite passes 98 tests. The admin build, formatting checks, and Docker builds succeed.

## Reusable storefront forms — 2026-10-22

- [x] Strapi owns reusable `storefront-form` documents with draft and publish states. Each form has a name, slug, enabled state, submit label, success message, and ordered fields.
- [x] The admin Forms workspace supports short text, email, telephone, long text, dropdown, and checkbox fields. Administrators can configure required fields, length limits, placeholders, help text, and dropdown values.
- [x] The page builder has a reusable form section. It assigns an existing form by slug. The default Contact page uses the seeded `contact` form.
- [x] The storefront loads published form definitions through Spring. It renders each form and displays submission results.
- [x] Spring validates each submitted value against the published definition, then writes it to Strapi as a `form-submission` document (via the Content API and service token), linked to the form's stable `documentId`.
- [x] Anonymous GET and POST form routes have a separate stateless security matcher. Public API tests verify access without CSRF tokens and verify validation errors.
- [x] End-to-end verification confirmed the Contact form renders all seeded fields, accepts anonymous submissions, and stores each as a Strapi `form-submission` document linked to the published form. Test documents were removed after verification.
- [x] Authenticated verification completed with a dedicated Super Admin account: the Submissions tab lists stored values (newest first), opens the detail dialog, exports CSV with formula escaping, and deletes records (confirmed removed from Strapi). Deletion under an unknown form slug returns a validation error.
- [x] Data export (CSV) and single-record deletion are implemented for administrators.
- [ ] Add rate limiting, spam scoring, retention rules, and bulk operations before production use.

## Storefront languages and color palette — 2026-10-23

- [x] Liquibase 017 adds `storefront_language` (seeded with German, English, Russian, Turkish; German and English enabled) and a single-row `storefront_appearance` table holding the published palette key.
- [x] New backend package `de.openshopfront.platform.storefront`: `StorefrontSettingsService` validates language codes, keeps at least one language enabled, and restricts palettes to the four theme keys. Public endpoint `GET /api/v1/public/settings` is permit-listed in the anonymous storefront security chain; administrator endpoints live under `/api/v1/admin/storefront-settings` (SUPER_ADMIN).
- [x] Storefront i18n: `apps/storefront/src/shared/i18n.ts` holds the interface strings for German, English, Russian, and Turkish. The header shows a language switcher only when more than one language is enabled. Language resolution order: saved visitor choice, matching browser language, first enabled language. Money, dates, and booking confirmations format with the active locale.
- [x] Translation scope is the storefront interface (navigation, buttons, booking flow, shop, cart, checkout, footer). Page content authored in the page editor stays in its written language; per-language CMS editing remains open.
- [x] The published palette applies to every visitor on load. In `?preview=1` mode the footer keeps a local theme toggle so administrators can try palettes inside the page preview without publishing.
- [x] Admin Appearance section restructured: "Storefront color palette" (server-saved, publishes at once), "Website languages" (checkbox rows with client and server last-language guard), and the existing per-browser admin theme moved to a clearly labeled personal panel. Settings load on section entry and after sign-in.
- [x] Verification: backend compiles; storefront and admin typecheck clean; Prettier passes. A Playwright run against the Docker stack passed 14/14 checks: switcher lists only enabled languages, DE/RU translations render, enabling Russian in the admin makes it appear in the storefront, publishing Lagoon changes `--openshopfront-bg` to the ocean value for visitors, and the last enabled language cannot be switched off.

## Gallery block, product list component, Storybook service — 2026-10-23

- [x] New `OpenShopfrontGalleryComponent` in `packages/ui`: responsive image grid with hover captions and a built-in lightbox (backdrop close, previous/next, Escape/arrow keys). Presentational and i18n-free; control labels come via an optional `labels` input. Storybook stories: Default (images) and Placeholders.
- [x] New `OpenShopfrontProductListComponent` in `packages/ui`: product card grid with media (image or initial placeholder), optional badge, name, description, price row and an optional add-to-cart button (rendered only when a label is supplied). Outputs: `open` (card click) and `addToCart`. Storybook stories: Default, WithAddToCart, Empty.
- [x] New `gallery` content block type end to end: backend allowlist + validation in `PageService` (1–12 items, safe URL check per image), admin page editor with per-image address/caption rows and add/remove controls (de/en/ru/tr labels), storefront rendering via the gallery component inside `OpenShopfrontContentBlocksComponent`.
- [x] The shop page now renders its catalog through `OpenShopfrontProductListComponent` instead of an inline card grid.
- [x] The booking page now renders its published CMS blocks (previously only a boolean flag was passed and the content was dropped); the static hero remains as fallback when the page is not published.
- [x] The about/contact page no longer duplicates the block renderer; it uses `OpenShopfrontContentBlocksComponent` with the shared form template projection.
- [x] Storybook is now a Docker Compose service (`storybook`, port 127.0.0.1:8083) built from `apps/admin/Dockerfile.storybook`; it serves the static build of all UI library components.
- [x] Bug fixes found while verifying: (1) `ProductComponent` and `OrderComponent` read a required input in the constructor, which throws NG0950 before bindings are set — loads now start in `ngOnInit`; (2) the product detail API nests the product under `product`, but the component expected a flat shape — the load now flattens it; (3) the inner item loop's `$index` shadowed the outer block loop's `$index` in the gallery editor, so edits wrote to the wrong block — handlers now take the block object and resolve its index.
- [x] Verification: storefront and admin build with zero errors and zero warnings; Storybook builds via `ng run admin:build-storybook`; all four containers (backend, storefront, admin, storybook) healthy; Playwright verified the gallery lightbox in Storybook and on the published booking page, the product list grid plus add-to-cart flow on a real product, and the gallery block editor save/publish round trip.

## Frontend structure: feature folders and separate templates — 2026-10-23

- [x] Both Angular apps reorganized into one folder per page or section component under `apps/*/src/`. Cross-cutting files (i18n, cart/shop services, types, auth) live in `shared/` only when two or more features use them. Feature-local services stay with their feature (`booking-api.service.ts` in `booking/`).
- [x] Storefront `main.ts` reduced from 790 to about 460 lines and is now a pure shell/router (header, navigation, routing switch, footer). Home, services, shop hero, booking, product, cart, order, about, and contact are separate page components that receive CMS content as inputs and emit navigation/product events as outputs.
- [x] Admin `main.ts` reduced to about 330 lines; the seven inline `page-intro` header blocks moved into their section components (bookings, business profile, booking services, pages, forms, commerce, reports).
- [x] New self-contained `StorefrontFormComponent` (`apps/storefront/src/shared/`) loads its own form definition by slug and submits it. This removed all form state from the storefront shell and fixed a bug where the contact page rendered an empty template for its `form (contact)` block. Root cause of that bug: a required input was read in the component constructor, which throws NG0950 before bindings are set; the load now starts in `ngOnInit`.
- [x] All 29 component templates extracted from `.ts` files into sibling `.html` files (`templateUrl`), and all five inline CSS blocks into sibling `.css` files (`styleUrls`). Storybook story wrapper strings stay inline by exception. The rules are recorded in `AGENTS.md` (Code structure section).
- [x] Shared UI library (`packages/ui`) unchanged in behavior: ThemePicker, LanguageList, MetricCard, CapabilityTable, and ContentBlocks remain presentational and i18n-free; Storybook builds via the Angular architect targets.
- [x] Verification: both apps build with zero errors and zero warnings; Prettier applied to all touched TS/HTML/CSS files; Docker images rebuilt for admin and storefront only; Playwright smoke test passed on storefront home, services, shop, contact (form renders and submits end-to-end), booking, cart, about, and on the admin login gate plus all seven sections with their moved intros.

## Mock test data: one-click store profiles — 2026-10-23

- [x] New backend `MockDataService` + `MockDataController`: `POST /api/v1/admin/mock-data/{profile}` (SUPER_ADMIN) and `GET .../profiles`. One call replaces the whole application content with one of five test stores: business profile (Strapi single-type PUT, current document merged), product catalog (all products deleted, new ones created with a standard variant), booking services (old offerings deactivated, new ones created), and all six storefront pages saved and published.
- [x] The five store profiles: Shoe Store ("Stride & Sole"), Flower Store ("Bloom & Petal"), Tattoo Studio ("Ink & Iron"), Bakery ("Golden Crust"), Fashion Store ("Atelier Nord"). Each ships 5–6 products, 3 bookable services and six page block sets (home, services, booking, shop, about, contact).
- [x] Admin UI: "Mock test data" panel in the System section with one button per store, an applying state, and a success/error notice. Labels translated for de/ru/tr (English is the source language via fallback). A confirmation dialog ("Replace all content?") opens before any apply; it warns that every product, booking service and page will be deleted and only the chosen store's test data remains. Cancel stops without applying; "Replace all content" runs the apply.
- [x] Re-apply fix: `booking_service_definition` has a unique constraint on name, so deactivating old services left their names reserved and re-applying the same store crashed with a duplicate key. The mock apply now hard-deletes service definitions (bookings keep their stored snapshot via `ON DELETE SET NULL`) and runs in one transaction, so a failed apply rolls back instead of leaving half-applied content.
- [x] Images: each store's home page now includes a `gallery` block with four themed stock photos (stable seeded URLs from picsum.photos), rendered by the existing gallery component with lightbox.
- [x] Product images end to end: new Liquibase migration `020-product-image` adds `product.image_url`. The catalog API, admin product editor (new "Image URL" field on create and edit), storefront shop grid (`OpenShopfrontProductListComponent` now shows the photo instead of the initial-letter placeholder) and the product detail page all carry it. The detail page renders the photo through the gallery component with lightbox; mock products each get a themed main image, so `/shop` shows real photos and clicking an item opens its gallery.
- [x] Verification: backend and admin build clean; containers healthy; Playwright applied Tattoo through the UI ("Test data applied: 5 products, 3 services and 6 pages"), then applied it **again** (the case that crashed before) with the same success result; Cancel left the previous notice untouched; switching to Flower Store succeeded and the storefront home shows flower content. Repeated and cross-store switching stays clean.

## Storefront commerce flow and responsive UX — 2026-10-24

- [x] Shop, product, cart, and confirmation pages use clear actions on desktop and mobile. Long Stripe URLs are not displayed. The hosted payment URL stays behind a primary action and a short fallback action.
- [x] Browser Back and Forward navigation restores the shop, product, and cart pages. Cart state remains in browser local storage.
- [x] Product pages load the quantity already stored for the selected variant. The primary action changes from **Add to cart** to **Update cart** for an existing line.
- [x] Product and cart quantity controls use the current available stock as their maximum. Older cart data is reconciled when the product loads. A clear warning reports each reduction.
- [x] The browser stock limit is an early validation only. Checkout locks variants, checks current stock again, and reserves stock in the backend transaction.
- [x] Successful checkout clears cart notices and hides the cart grid, totals, and checkout instructions. The page shows only the order confirmation and applicable next actions.
- [x] Mobile controls have reliable touch sizes. The mobile menu exposes expanded and current-page states. The empty-cart icon uses matching width, height, font size, and line height, so it does not clip.
- [x] Verification: the Storefront Docker image builds; the container becomes healthy; HTTPS returns HTTP 200 after accepting the local generated certificate. Playwright verified catalog browsing, product selection, stock clamping from 38 to 37, cart updates, removal, empty state, Back and Forward navigation, mobile layout without horizontal overflow, and successful checkout cleanup.

## First-run script and Traefik health checks — 2026-10-24

- [x] `scripts/setup.py` starts Compose with `--wait` and the configured timeout. Docker health checks validate internal services.
- [x] The script checks the Storefront, Admin, and CMS through their current Traefik hostnames instead of obsolete direct ports.
- [x] Route checks accept every successful HTTP `2xx` status, including the CMS health endpoint's `204` response.
- [x] `--start-only` starts and checks existing images without rebuilding the complete stack.
- [x] Generated environments use `admin.localhost` and `storefront.localhost` as the local public base URLs.
- [x] Setup output prints the current Storefront, Admin, and CMS addresses. It does not advertise the private Backend port.
- [x] Verification: `sh setup.sh --start-only --timeout 120` completed successfully with all seven services healthy and all three Traefik route checks passing.
