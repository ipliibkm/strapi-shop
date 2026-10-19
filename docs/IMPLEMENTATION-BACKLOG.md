# Step-by-step Implementation Backlog — Work Packages for Developers/LLMs

> **Status: NOT STARTED.** Every checkbox is unimplemented. Each package is an independently assignable workstream and needs an issue, owner, tests, documentation, review and acceptance evidence. No task is complete merely because its file exists.

## How to execute
1. Close required decisions in DECISIONS.md, record ADRs, authorize repository creation/visibility.
2. Create issue per numbered phase/work package below; add dependency links and milestone, priority, owner, acceptance criteria.
3. Use contract-first design: each API publishes OpenAPI schemas/examples before frontend integration; consume generated clients.
4. Work in feature branches with PR review; require static checks, unit, integration, security and accessibility gates.
5. Make every deployment reproducible from pinned version manifest; don't advance environments unless migration and rollback paths are tested.
6. Produce demo evidence in temporary staging; never substitute screenshots for executable acceptance tests.

## Phase 0 — Requirements and governance (blocking)
- [ ] P0-01 Gather tenant/site/store/booking priorities, personas and core user journeys.
- [ ] P0-02 Resolve D01-D32; record approved ADRs and identify intentionally deferred features.
- [ ] P0-03 Define MVP tiers: **Core** website, **Commerce**, **Booking**, **Enterprise/optional**.
- [ ] P0-04 Define supported regions, tax laws, shipping countries, currencies, language/timezone, accessibility level.
- [ ] P0-05 Define scaling assumptions, SLA/SLO, RPO/RTO, max concurrency, media volume and cost ceiling.
- [ ] P0-06 Create requirements traceability matrix mapping user journey -> model/API -> repo -> test.
- [ ] P0-07 Assess buy-vs-build for ecommerce engine vs custom transactional domain; validate Strapi license/plugins, Angular/Storybook version matrix.
- [ ] P0-08 Threat modeling and privacy impact: payment card scope, GDPR, PII retention, supplier/processor DPAs.

**Acceptance:** ADRs signed; no contradictory ownership/requirements; risk register and MVP in/out scope approved.

## Phase 1 — GitHub repositories and engineering standards
- [ ] P1-01 Create 11 planned repositories, configure visibility, CODEOWNERS, README, license, SECURITY.md, CONTRIBUTING.md, issue and PR templates.
- [ ] P1-02 Bootstrap organization-level issue taxonomy, priority labels, milestones and cross-repo project view.
- [ ] P1-03 Define conventional commits, semantic versioning, Changesets/releases, Dependabot/Renovate, compatible version manifest.
- [ ] P1-04 Establish GitHub Actions CI templates, PR status checks, commit signing policy, protected branches, repository permissions.
- [ ] P1-05 Configure private container/package distribution (GHCR/npm), image provenance/SBOM, attestations and vulnerability policies.
- [ ] P1-06 Create code scaffolding guidelines, internal API naming style, OpenAPI-first contract review process.
- [ ] P1-07 Agree secrets policy, .env.example conventions, secret manager integration, git secret scanning and rotation procedures.

**Acceptance:** every repository build pipeline exists, all templates and security policies validated, PR merge rules enforced.

## Phase 2 — Infrastructure foundations
- [ ] P2-01 Plan Compose base/development/production profiles and optional search/telemetry/broker overlays.
- [ ] P2-02 Plan edge gateway/proxy, TLS provisioning, hostname routing, CORS, CSP, request size and WAF strategy.
- [ ] P2-03 Plan PostgreSQL database/schema ownership: CMS, commerce, booking, worker; distinct roles, backup/persistence.
- [ ] P2-04 Define Liquibase lifecycle for owned domain schemas and native Strapi migration lifecycle for CMS; conflict-prevention tests.
- [ ] P2-05 Define persistent media/object storage, dev emulator, production offsite storage and CDN transforms.
- [ ] P2-06 Plan network isolation, non-root permissions, image/digest pinning, compose health/readiness, resource limits.
- [ ] P2-07 Define CI deploy/promote/rollback path, env mapping, one-click dev launch and fresh-machine validation.
- [ ] P2-08 Plan encrypted backups, PITR/WAL, restoration verification, media/DB consistency and runbooks.
- [ ] P2-09 Add telemetry pipeline architecture, metrics/traces/logs, synthetic checks, alert routing, incident process.
- [ ] P2-10 Verify production target: Compose single-host or stronger HA approach; capacity, failover and secrets strategy.

**Acceptance:** staging smoke deployment, secret/no-public-database audit, full DB/media restore drill, controlled roll forward/backward.

## Phase 3 — Identity, contracts and shared infrastructure
- [ ] P3-01 Select OIDC/identity provider, staff vs customer realms, MFA, account provisioning, guest checkout.
- [ ] P3-02 Define claims, RBAC/ABAC resources, tenant/site scoping, object authorization and audit-event taxonomy.
- [ ] P3-03 Publish OpenAPI 3.1 specs, standardized errors, pagination, filtering, versioning, auth and idempotency.
- [ ] P3-04 Publish versioned event schemas, topic naming, message IDs, retry/dead-letter/replay and schema compatibility.
- [ ] P3-05 Define transactional outbox/inbox delivery, consumer dedupe and retention; broker selection proof.
- [ ] P3-06 Specify user PII lifecycle, export/deletion/consent, storage encryption and audited staff access.
- [ ] P3-07 Specify generated TypeScript clients, package releases and contract compatibility tests.

**Acceptance:** API lint, schema evolution tests, forbidden access tests and example requests validated.

## Phase 4 — Strapi CMS
- [ ] P4-01 Pin verified Strapi v5 version, supported Node/Postgres and plugin compatibility.
- [ ] P4-02 Design structured types listed in DOMAIN-AND-API.md; content-vs-transaction separation and stable external IDs.
- [ ] P4-03 Specify content lifecycle: draft, preview, review/approval, publish/unpublish, audit and rollback policy.
- [ ] P4-04 Specify reusable page blocks, validation, image policies, relation depth and safe dynamic zones.
- [ ] P4-05 Design localization, slugs, redirects, canonical/hreflang, sitemap/robots, structured metadata.
- [ ] P4-06 Define media storage integration, upload security, CDN/cache, backup and expiry.
- [ ] P4-07 Define public vs staff role permissions, safe endpoints, API tokens, rate limits, deep populate restrictions.
- [ ] P4-08 Design publish webhook signing, revalidation, cache purge/search sync, retries and failure handling.
- [ ] P4-09 Define deterministic seed/sample content, imports/exports, content migrations and staging-to-production promotion.
- [ ] P4-10 Design contact forms, CAPTCHA/spam handling, privacy consent and notification workflows.

**Acceptance:** editorial journeys, localization SEO audits, unauthorized access rejection, published content shown and stale cache invalidation.

## Phase 5 — Design system, Storybook, frontend foundations
- [ ] P5-01 Compare Angular Material/CDK, PrimeNG and custom CDK components (accessibility, license, theme cost, maintenance).
- [ ] P5-02 Pin Angular 22 and compatible Storybook versions; prove build/preview/test integration before component volume grows.
- [ ] P5-03 Define design tokens, typography, spacing, radius, elevation, iconography, CSS variable semantic themes.
- [ ] P5-04 Specify light/dark, brand themes, internationalization, RTL support when needed and responsive breakpoints.
- [ ] P5-05 Inventory components: button/link, typography, forms, input/select/checkbox/radio, date/time picker, dialog, drawer, toast, table, cards, breadcrumb, tabs, pagination, search/filter, navigation, uploads, skeletons, cart controls, booking calendar, charts.
- [ ] P5-06 Specify WCAG 2.2 AA keyboard/screen reader/focus/contrast requirements and reduced-motion preference.
- [ ] P5-07 Define Storybook docs, a11y audits, interaction and visual snapshot tests, fixture and mock API patterns.
- [ ] P5-08 Define package publication, semver support, changelogs and compatibility matrix.
- [ ] P5-09 Plan Angular SSR/hydration, static prerender, localization, route guards, typed API clients, signals/state patterns.
- [ ] P5-10 Define Core Web Vitals budgets, image optimization, cache, loading/error states and responsive UI testing.

**Acceptance:** complete documented component checklist, representative live accessible examples, reproducible consumption by storefront/admin.

## Phase 6 — Public website
- [ ] P6-01 Map navigation, homepage, dynamic pages, blog/article, FAQ, contact form and global shell.
- [ ] P6-02 Content-driven block registry, preview, 404/410, redirect and multilingual SEO URL rules.
- [ ] P6-03 Header/footer, search UX, breadcrumbs, cookies/consent, legal pages, analytics opt-in.
- [ ] P6-04 Responsive editorial media, form validation, spam prevention and lead notifications.
- [ ] P6-05 Integration contract tests against CMS and tests for SSR/hydration, sitemap/robots, social previews.
- [ ] P6-06 Provide website-only install profile with commerce/booking fully disabled and no missing dependencies.

**Acceptance:** editable, indexed, accessible site runs without commerce or booking modules.

## Phase 7 — Commerce domain and transactional APIs
- [ ] P7-01 Finalize product/channel/variant/category/attribute, money, price list, tax, promotion, media linkage.
- [ ] P7-02 Define cart lifecycle: anonymous/authenticated, merge, expiry, promotion and shipping/tax recalculation.
- [ ] P7-03 Define inventory locations, movement journal, stock reservation locks, expiration and oversell protections.
- [ ] P7-04 Define order lifecycle, immutable line snapshots, state machine, cancellation and auditing.
- [ ] P7-05 Define checkout idempotency, authoritative server-side pricing, coupon limits, tax/shipping quote, payment intent.
- [ ] P7-06 Define PSP abstraction, signed webhooks, delayed/reordered/duplicate events, replay/reconciliation and failure compensation.
- [ ] P7-07 Define refunds/partial refunds, chargebacks, returns/exchange, fulfillment and shipping status.
- [ ] P7-08 Define invoice/tax provider integration and fiscal/legal retention (confirm jurisdictions).
- [ ] P7-09 Define customer accounts, saved addresses/orders, privacy rights, guest order lookup and support audit.
- [ ] P7-10 Document APIs, Liquibase migrations, transaction boundaries, indexes, invariants and contract tests.
- [ ] P7-11 Load tests: last-unit race, duplicate webhook, payment interruption, invalid coupons, concurrent checkout.
- [ ] P7-12 Optional extension backlog: digital downloads, subscriptions, loyalty, gift cards, B2B, marketplace, POS.

**Acceptance:** reliable sandbox checkout and recovery, correct totals/stock, returns/refunds validated and payment reconciliation audited.

## Phase 8 — Scheduling and bookings
- [ ] P8-01 Define services, locations, staff/resources, capacities, opening hours, holidays, breaks, exceptions.
- [ ] P8-02 Define timezone, IANA identifiers, DST discontinuities/repeated hour policy, recurrence/ICS import-export.
- [ ] P8-03 Define availability query algorithm, atomic slot holds, TTL, appointment creation and no-double-booking constraint.
- [ ] P8-04 Define deposits/full booking payments, refund/cancellation/reschedule/no-show/waitlist policy.
- [ ] P8-05 Define staff overrides, admin approvals, conflicts and booking audit trail.
- [ ] P8-06 Define Google/Microsoft/iCal sync connectors, token refresh, sync cursor, duplicates and failure replay.
- [ ] P8-07 Define email/SMS notifications, timezone-safe reminders, unsubscribe/consent and retry behavior.
- [ ] P8-08 Define booking calendar UI, localization, accessibility and mobile flows.
- [ ] P8-09 Add tests for simultaneous bookings, expired holds, DST, booking-payment races and external sync failure.

**Acceptance:** concurrent last-slot safety, correct local times, recoverable calendar sync and cancellable/reschedulable bookings.

## Phase 9 — Operational admin & dashboard
- [ ] P9-01 Separate CMS editorial admin from operations admin responsibilities and security boundaries.
- [ ] P9-02 Specify roles for store manager, fulfillment, support agent, booker, staff and read-only analyst.
- [ ] P9-03 Order queue, refunds, stock adjustments with reason/audit, returns and customer support workflow.
- [ ] P9-04 Availability planner, resource calendar, booking approvals, blocked days, reminder templates.
- [ ] P9-05 Operational dashboards: sales funnel, conversion, revenue, cancellation, payment failures, inventory alerts, booking utilization and system status.
- [ ] P9-06 Report exports, pagination, filters, masking and data retention policy.
- [ ] P9-07 Accessible UI tests, forbidden action tests, audit trail review and PII masking.
- [ ] P9-08 Document attribution logic/time ranges/timezones; reconcile reports to order ledger before using them for financial decisions.

**Acceptance:** staff can complete approved operational workflows with least privilege and stable reporting definitions.

## Phase 10 — Workers and external integrations
- [ ] P10-01 Email provider, templates, transactional notifications and delivery tracking.
- [ ] P10-02 SMS/WhatsApp opt-in connectors if needed; templates, cost and retry handling.
- [ ] P10-03 Outbox processing, durable queue, dead-letter, retries, dedupe, replays and alerts.
- [ ] P10-04 PSP reconciliation, nightly consistency checks, stale order and booking recovery.
- [ ] P10-05 Search indexing/reindex with deleted/updated content, availability and stock projection freshness.
- [ ] P10-06 Analytics/event ingestion and aggregation with lawful consent where required.
- [ ] P10-07 Inventory/booking expiry jobs, schedule drift and multi-worker leader/lock behavior.
- [ ] P10-08 External providers resilience: rate limits, circuit breakers, failover, sandbox mocks, credential rotation.

**Acceptance:** consumers survive crashes/replays and can be manually replayed without duplicated side effects.

## Phase 11 — Quality, operations, security and launch
- [ ] P11-01 Unit/integration/contract/component/E2E tests, accessibility and browser/device matrix.
- [ ] P11-02 Load/stress/soak tests; event delivery, p95 latency, concurrent checkout/booking and queue backlogs.
- [ ] P11-03 Authn/authz, secret scanning, SAST/DAST, container scanning, dependency/SBOM and penetration testing.
- [ ] P11-04 GDPR data export/erasure/retention validation, cookie consent and EU ecommerce legal review.
- [ ] P11-05 Dashboard/alert coverage: app availability, DB, containers, payments, order/booking conversion and queue delay.
- [ ] P11-06 Test backup restore, PITR, object storage recoverability and RPO/RTO, rehearsal with sanitized staging.
- [ ] P11-07 Write operator runbooks: install, upgrade, migration, backup, restore, rollback, incident, credentials, PSP outage.
- [ ] P11-08 Create reproducible single-command developer startup, example demo data and deterministic smoke checks.
- [ ] P11-09 Validate architecture feature profiles (website only / commerce / booking / full), provider-disabled failures and startup profile isolation.
- [ ] P11-10 Final launch checklist, ownership/on-call, system capacity tests, dry run and production readiness sign-off.

**Acceptance:** documented and tested clean-room deployment, real restore and failure drills, security/legal reviews and approved launch checklist.

## Cross-repo dependencies / recommended sequence
P0 -> P1 -> P2+P3 -> P4+P5 -> P6 -> P7/P8 in parallel -> P9/P10 -> P11.
Contracts (P3) precede production API consumption. Domain schema migrations (P7/P8) precede booking/order integration testing. Platform quality testing begins in P1 and continues across each phase, even if final verification is P11. Adapters must be swappable and optional install profiles must not require unused services.

## Template for each implementation issue
**Goal**, **Scope**, **Inputs/decisions**, **Owning repo**, **API/schema changes**, **Dependent issues**, **Detailed numbered tasks**, **Acceptance tests (Given/When/Then)**, **Security/privacy review**, **Observability**, **Migration/rollback**, **Documentation**, **Out of scope**, **Evidence**, **Done criteria**.

**Definition of done**: security/privacy checks complete; unit+integration+contract+E2E (where applicable) passing; migration forward/backward compatible or documented irreversibility; metrics/logs/traces; documentation/API spec; controlled rollout and rollback; PR review; no hard-coded secrets.