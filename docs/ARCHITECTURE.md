# System Architecture and Operational Boundaries

## Context and principles
Build a **configurable product platform**, not one bespoke store. Installation profiles: content-only website; content + ecommerce; content + bookings; combined. Modules off by default unless configured. Single business is initial default; tenant isolation and storefront/channel separation are explicit later decisions, not hidden assumptions.

### Logical dataflow
Browser/SEO crawlers -> CDN/WAF -> reverse proxy -> Angular storefront SSR or static/prerender routes -> backend-for-frontend/commerce/booking public APIs.
Editors -> Strapi Admin -> CMS DB/media.
Operations staff -> Angular Admin -> commerce/booking APIs.
CMS -> publish webhook/outbox bridge -> worker -> content cache invalidation/search indexing.
Commerce/Booking -> transactional DB -> outbox -> message broker/worker -> email, receipts, analytics, search, notifications.
Payments PSP -> signed webhooks -> commerce/booking transaction handler -> idempotency/reconciliation.
OTel collectors scrape/receive every component; secrets never traverse the public network.

## Component responsibility matrix
| Component | Owns | Must not own |
| --- | --- | --- |
| Strapi | content models, landing pages, navigation, localized content, product marketing details, SEO, media, publishing approvals | inventory truth, paid order ledger, PCI-sensitive card data, slot allocation |
| Commerce | SKUs/variants, prices, promotions, tax calculation orchestration, stock movements, carts, checkout, orders, payment intent state, refunds, fulfillment | rich editorial page layouts |
| Booking | resource calendars, opening rules, service durations, blocked dates, slot holds, confirmed appointments, cancellation policy | CMS page data, card details |
| Storefront | public UI, SSR, forms, client routing, UX & performance | authoritative payments or stock |
| Admin | operational commerce/booking controls, analytics read models, RBAC UI | direct DB writes |
| Workers | scheduled and asynchronous workflows, retries, delivery, index projections | trust in unverified webhook input |
| Infrastructure | composition, health, networking, TLS, deployments, backup and telemetry | domain business logic |
| Contracts | OpenAPI + event definitions, version policy and SDK generation | hand-maintained duplicate DTOs |

## Identity and authorization
Decision required: Strapi local users vs external OIDC (prefer separate OIDC provider for customers and staff); never assume Strapi admin users are customer identities. RBAC + object-level authorization + least privilege; support guest checkout as configured; MFA for staff; reset/invite, account verification, session/token refresh, CSRF/CORS, secure cookies, consent, deletion/export workflows, rate limiting and bot defense.

## Data/database rule
Use PostgreSQL databases `cms`, `commerce`, `booking` (or isolated namespaces when justified), separate credentials and backup policies. Strapi uses its schema management and native migrations. Liquibase alone controls the dedicated commerce/booking/worker-owned schemas. Check startup migration ordering, rollback/preconditions, failed migration safety, read replica support, indexes/constraints, zero-downtime expand-migrate-contract compatibility and restoring migration state alongside data. Never point Liquibase at Strapi-owned tables. Prefer transactional outbox for event publication.

## Domain integration
- Versioned REST OpenAPI as baseline. GraphQL only for justified use cases; never expose unrestricted query complexity.
- Define consistent pagination/filter/sort conventions, identifiers, optimistic locking/ETags, timestamps (UTC persisted, local timezone IANA identifiers), RFC7807 errors, correlation IDs, request validation, rate limiting, API deprecation and payload size limits.
- Decide async transport: PostgreSQL outbox + polling for simple deployments, RabbitMQ when durable queues needed, Kafka only if scale/operations justify it. At-least-once delivery + idempotent consumers, poison queues and replays.
- Publish compensation/saga flows where purchases and bookings cross services. Do not use distributed DB transactions.
- Product-to-CMS linking via stable IDs, validation/sync reconciliation; no cross-service FK.
- Feature flags for commerce/booking/payment provider, localization, search and future channels.

## UI architecture
Angular 22 target (verify exact compatible patch), standalone components/signals and typed reactive forms; Angular SSR/prerender route strategies with cache-control and hydration. Storybook for isolated documented components; test compatibility against Angular builder + package versions. Angular CDK and accessible primitives as foundations; decide whether to adopt Angular Material, PrimeNG or custom components by design review, avoid copying third-party components. Design tokens (CSS variables, semantic themes, contrast), responsive layouts, keyboard navigation, focus and screen reader testing WCAG 2.2 AA. Component playground, visual snapshot diffs, interaction tests, component API docs, mock data fixtures. Share built UI library via versioned npm/GitHub packages (not git submodule), compatibility matrix, semantic versions, changesets, dependency policy.

## Infrastructure / deployment
Docker Compose files: `compose.yaml` base + `compose.dev.yaml` + `compose.prod.yaml` + optional observability and profiles. Services: edge proxy (Traefik/Caddy decision), CMS, storefront SSR, commerce, booking, workers, PostgreSQL, optional Redis, broker, S3-compatible object store in dev; production object storage may be external. Isolated internal networks, no exposed databases, healthchecks/readiness, startup dependency checks, resource ceilings, graceful shutdown, stateless app images, read-only mounts where possible. Immutable pinned image digests, SBOM, scanning, non-root containers. Production secrets via approved secret manager/file mounts; no .env secrets in Git. Separate storage lifecycle for DB, media and backups.

**Important:** Compose single-host is not a high availability strategy. Define target RPO/RTO and disaster recovery: encrypted backups, offsite retention, WAL/PITR, recurring restore drills, media consistency, certificate/DNS runbooks, deployments and rollback of compatible versions. Uptime SLO and on-call escalation. CI needs per-repo checks, artifacts, integration smoke against pinned versions, staged promotion and protected environments. Plan cross-repo version manifest/release orchestration.

## Observability
OpenTelemetry SDKs/collector; Prometheus/Grafana-compatible metrics; Loki-like centralized logs; Tempo/Jaeger traces; Alertmanager; optional Sentry for client errors. Golden signals (traffic, errors, latency, saturation); business indicators (abandoned checkout, failed transactions, webhook lag, appointment utilization). Trace checkout and booking end-to-end with redaction. Define dashboards, alerts, thresholds, ownership and incident response. Never collect payment card details, credentials or unnecessary PII.

## Security / compliance
Document threat model (OWASP ASVS/API Top 10), dependency/supply-chain controls, scanning, CSP, HSTS, TLS, CSRF, XSS, SSRF, SQL injection, upload restrictions, user-generated content moderation, audit trails, malware scanning where needed, rate limits, WAF, SAST/DAST, secrets rotation, auth tests and tenant isolation tests. For EU business, data inventory, GDPR legal bases, DPA, retention/deletion, subject rights, cookie consent and regional residency; Germany/EU store requires legal review for checkout, tax/VAT, pricing and consumer rights. PSP hosted fields/checkout to minimize PCI footprint.

## Nonfunctional acceptance
Define load baseline and targets for p95 API latency, storefront Core Web Vitals, availability, backup RPO/RTO, concurrency per SKU/booking slot, region and localization, expected catalogs/orders, accessibility and performance budgets. Tests must prove oversell and double-booking protection, duplicate webhook handling, crash/retry recovery, successful restore and rollout/rollback. No claim of production readiness without operational drills.


## Approved architecture amendments — 2026-10-19
### Deployable topology vs source code layout
Approve **Java / Spring Boot**, PostgreSQL, Liquibase, Strapi latest supported v5, Angular 22 target, Angular Material and Storybook; full optional module set deployed via Docker Compose on **one VPS** for **one business per installation**. Avoid premature microservices: compose backend as a **modular Spring Boot monolith** with separate commerce, booking, identity-adapter, Google-integration, shared-kernel (strictly minimal), scheduler and outbox/notification modules; expose one coherent versioned API gateway/BFF surface. Split a module into a service only after scaling, isolation, release cadence or security evidence requires it. Avoid distributed transactions for operations within the same transactional application database; use outbox for cross-process and third-party side effects.

Retain separate CMS PostgreSQL database/schema + credentials managed only by Strapi and separate application PostgreSQL database/schema + credentials managed by Liquibase. Could be **one PostgreSQL server/container on the VPS** with multiple databases and role isolation. Commerce/booking in the same application DB may use distinct schemas and Liquibase changelog namespaces, enabling atomic workflows when necessary. Strapi schema sync is authoritative for CMS-managed tables; Liquibase must never modify those tables. Do not assume Git repo count, number of Compose services and number of databases must coincide. Git repository boundary is now a follow-up decision: 11 previously approved private repository names can remain as independent projects or become workspace packages/directories within a single private monorepo; monorepo is the operational simplicity recommendation.

### Angular public website + custom Angular admin
Both Angular applications depend on **one separately versioned Angular Material-based design-system package**, built from reusable tokens/components and documented/tested in Storybook. Public storefront theme may be selected independently from admin theme; confirm policy. **Admin must be bespoke**, not generic Strapi admin skinned. Features: staff dashboard, bookings list/calendar (day/week/month/resource views), drag or explicit reschedule with conflict checks, create/edit/cancel, group roster/capacity and waitlist, resource/staff availability, calendar connection status, sync conflicts, customer records with privacy controls, payments/refunds, orders/inventory, reporting, RBAC audit logs, themes manager. Strapi editor UI remains separate for content editing unless a later explicit decision replaces it. Theme controls: preview, choose named presets, light/dark, safe token overrides/brand assets, save/publish/rollback, per-user vs global preference decision, a11y compliance. Disallow arbitrary scripts or user supplied unsafe CSS.

### Google Calendar and Gmail integration
**Google Calendar API**: OAuth 2.0 user consent to connect owner/staff accounts, least-privilege event/freebusy scopes, encrypted at-rest refresh tokens, access revocation, reconnect UI, tenant/account/resource mappings, a robust sync worker and durable cursors. Booking DB is source of truth for booking records/holds; Google Calendar is an integration/projection or external busy-time constraint. Define direction (one-way/two-way), sync conflicts, idempotent event mapping, daylight-saving IANA timezone, deleted/edited external events, notification and recovery from sync outages. A Google-side event is not itself an atomic reservation; confirm availability and lock the booking DB transaction. Calendar webhooks/watch and polling resync need subscription renewal and validation.

**Gmail API** is separate from Google Calendar API. For booking-confirmation/reminder emails, connect same Google identity with the separately consented `gmail.send` scope if Gmail is specifically required; do not request mailbox-read scopes unless inbound email features are explicitly approved. Queue outbound sends, dedupe message delivery, handle quotas, bounces/limits, account revocation and fallback provider decision. Domain-wide delegation or server-to-server authentication must not be assumed for personal Gmail accounts. OAuth consent verification/public app restrictions may apply.

### Required proof of production readiness for single VPS
RAM/CPU/disk sizing, sustained request concurrency, DB max connections, DNS/TLS automation, immutable image builds, ingress/network isolation, hardening and secret handling; encrypted automated **off-host** PostgreSQL/WAL and object storage backups; restoration rehearsal; monitoring/alerts even if VPS fails (prefer external heartbeat); upgrade/rollback, power outage and disk-full scenarios; document no multi-host HA. License/vendor viability and security GDPR obligations.
