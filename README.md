# Composable Strapi Commerce & Website Platform — Architecture and Delivery Plan

> PLANNING ONLY. No application code, infrastructure or production service is implemented here.
> Target owner: ipliibkm. Blueprint captured 2026-10-19.
> Detailed documents: [Architecture](docs/ARCHITECTURE.md), [Data/API contracts](docs/DOMAIN-AND-API.md), [Execution backlog](docs/IMPLEMENTATION-BACKLOG.md), [Decisions](docs/DECISIONS.md).

## Goals
Build a reusable, production-deployable platform for content-driven websites, ecommerce, bookings/calendar, payments and operational admin, with interchangeable modules and a shared Angular design system. Do not equate automatic Strapi content APIs with a secure commerce transaction engine.

## Baseline
- Strapi 5.x, pin an explicit verified release (v5.58.0 current as of 2026-10-19); TypeScript, PostgreSQL for CMS-owned models.
- Angular 22.x, standalone components, signals, SSR/hybrid rendering and hydration for indexed storefronts.
- PostgreSQL with **separate databases or schemas and roles** for Strapi and business transactional services.
- Liquibase for **application-owned tables only**, never for Strapi-managed tables. Strapi schema sync and Strapi migrations own the CMS database.
- Docker Compose for development and single-host deployment; production HA/multi-node strategy must be decided separately.
- Storybook + Angular CDK + design tokens + accessible component primitives. Evaluate Angular-Vite Storybook preview compatibility before pinning.
- Provider abstraction for payments, shipping, email, media, identity, search, tax and calendar connectors.

## Repositories to create under ipliibkm (each independently versioned)
1. `platform-architecture` — ADRs, contracts, roadmap, threat model, governance and cross-repo dependency graph.
2. `platform-infrastructure` — Compose overlays, edge proxy, networks, volumes, secret injection, monitoring, backup/restore, deployment runbooks.
3. `platform-cms` — Strapi models, editorial workflows, pages/SEO/marketing, CMS webhooks and CMS-only migrations.
4. `platform-commerce` — transactional API/service for cart, checkout, orders, inventory, returns, taxation orchestration, payment/shipping adapters; PostgreSQL and Liquibase.
5. `platform-booking` — calendars, resources, availability, holds, reservations, recurrence, timezone/DST, booking payments and notifications; PostgreSQL and Liquibase.
6. `platform-storefront` — Angular 22 public website and optional ecommerce/booking sections with SSR.
7. `platform-admin` — Angular operational backoffice for business workflows not covered by Strapi's editorial admin.
8. `platform-design-system` — Angular UI package, tokens, theme, accessibility, Storybook, component regression tests.
9. `platform-contracts` — versioned OpenAPI specifications, event schemas, shared generated client packages, compatibility policies.
10. `platform-workers` — outbox consumers, scheduling, notification dispatch, webhook retries, search projection/reindex, queues.
11. `platform-quality` — cross-service E2E, performance, security, accessibility, smoke and disaster recovery test plans.
Optional: `platform-integrations` for enough independently deployed connectors, `platform-analytics` if BI workload warrants separation.

## Non-negotiable architecture boundaries
- Strapi is the CMS and editorial admin, **not** the sole source of truth for orders, payment status, inventory or time-slot reservations.
- Payments are settled by the PSP; accept signed webhooks, verify signatures, process idempotently and reconcile independently of browser redirects.
- Checkout uses authoritative server-side prices, taxes and inventory; never trusts client totals.
- Reservations require concurrency-safe allocation, timezone policy, expired holds and no-double-booking guarantees.
- Multi-project repositories communicate through contracts, not cross-database writes or shared internal libraries leaking domain persistence details.
- Observability needs logs, metrics, traces, dashboards, alerts and tested restoration, not just uptime checks.
- No production deploy until decisions in docs/DECISIONS.md are resolved and quality gates pass.

## Reading order
1. Decide scope and feature flags in DECISIONS.
2. Approve service boundaries in ARCHITECTURE.
3. Specify models/endpoints in DOMAIN-AND-API.
4. Execute phased checklists in IMPLEMENTATION-BACKLOG.
