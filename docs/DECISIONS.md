# Architecture Decision Register — Approved and Pending

The proposal table below is historical. Approved choices and remaining questions are documented in the superseding section at the end.

| ID | Question | Proposed starting default | Why it matters |
| --- | --- | --- | --- |
| D01 | Primary goal: CMS-first sites, store-first, booking-first, or all enabled? | Modular platform, website enabled; commerce/booking feature flags | Avoid overbuilding |
| D02 | Single business/site or multitenant SaaS hosting several clients? | Single-tenant, multi-site-ready contracts | Security, schema, domain design |
| D03 | Repositories public or private? Naming prefix? Existing empty `strapi-shop` usage? | Private new repos with `platform-` prefix; existing repo as architecture staging | Repository creation and governance |
| D04 | Business geography, currencies, languages, sales tax regime? | Germany/EU, EUR, de/en; confirm | Checkout, compliance |
| D05 | Product type: physical/digital/services/subscriptions/B2B? | Physical products + appointments MVP | Data model and fulfillment |
| D06 | Payment provider(s), countries, wallets, deposit/refunds? | Stripe abstraction (not commitment) | API, webhook, legal |
| D07 | Shipping carriers/rules, pickup, digital delivery? | Carrier-neutral + flat-rate start | Logistics |
| D08 | Identity: OIDC service (Keycloak/Auth0/etc.), social login, guest checkout? | OIDC for customer/staff, guest checkout optional | Identity boundary |
| D09 | Booking model: staff/resource, classes, multi-capacity, recurrence, integration? | Single-resource appointments, optional calendar sync | Race conditions & complexity |
| D10 | Search: PostgreSQL FTS, Meilisearch, Typesense, OpenSearch? | PostgreSQL FTS initially, pluggable | Search relevance, ops |
| D11 | UI base: Angular Material, PrimeNG, custom CDK? | Angular CDK + custom themed design system + Storybook | UI quality, license, maintainability |
| D12 | Hosting: one VPS/self-hosted Compose, cloud, managed PostgreSQL/storage? | Single VPS Compose + managed offsite backup | SLA, budget |
| D13 | API service runtime for commerce/booking/worker: Java Spring Boot or Node/TypeScript? | Java Spring Boot + Liquibase preferred if enterprise transaction semantics; confirm | Repo scaffolds and ORM |
| D14 | Message transport: PostgreSQL outbox polling, RabbitMQ, Kafka? | Outbox + polling MVP; RabbitMQ on demand | Delivery & operations |
| D15 | Caching/session/queues: Redis needed? | Optional, only after load needs verified | Data invalidation/complexity |
| D16 | Marketing: analytics, cookie consent, email, CRM providers? | Provider-neutral, minimal consent analytics | Privacy, events |
| D17 | CMS media storage: local dev / S3 production and image CDN? | S3-compatible object store + CDN | Durability and scale |
| D18 | Functional role boundaries: editor, operator, manager, super admin, customer? | RBAC split across systems | Access controls |
| D19 | Target SLA, latency, throughput, RPO/RTO, storage, estimated transactions? | Define before hosting design | Production readiness |
| D20 | Licensing/distribution: private business internal, open-source framework, commercial starter? | Private until clarified | Dependency licensing |
| D21 | CI/CD: GitHub Actions + registry, branch policy, secrets manager? | GitHub Actions, GHCR, staged releases | Deployment & audit |
| D22 | Admin: rely on Strapi for content AND separate Angular for operations? | Both, distinct authorization | UX & security |
| D23 | Tenant domains/branding/white-label and themes? | Tokens/theme per brand, not tenant dynamic initially | Platform productization |
| D24 | Localization/timezones/currency conversion and region residency? | EU region + IANA timezones | Correct business rules |
| D25 | Must platform be offline/edge deployable or single VPS with internet? | Online single-region | OAuth/PSP dependencies |
| D26 | SEO: editorial prerender/SSR, catalog SSR and storefront index requirements? | Hybrid SSR and selective prerender | Angular hosting |
| D27 | Data ownership/reporting: internal dashboard vs external BI? | Operational dashboard plus export | Reporting architecture |
| D28 | Purchase flows: invoice generation, VAT, gift cards, loyalty, POS? | Invoice via provider, defer loyalty/POS | Legal integrations |
| D29 | Feature packages optional commercial modules and extensibility API? | Install profiles + explicit adapter contracts | Generic reuse |
| D30 | Desired delivery phases, MVP vs full version and expected budget? | 3-stage MVP-to-production rollout | Priorities |
| D31 | Website functionality: forms, newsletter, events, maps, multilingual blog, membership? | Content/forms/blog/SEO baseline | Model scope |
| D32 | Accessibility/compliance target and age-sensitive content? | WCAG 2.2 AA / GDPR, legal counsel for sales | Acceptance gates |

## Decision procedure
For each ID record: chosen option, owner, date, rationale, consequences, ADR link. Block technology-dependent build issues until relevant decisions closed. In particular D02, D05, D06, D09, D11, D12, D13 are architecture-significant.

## Critical open questions for the user
1. **Service runtime:** Java 21+/Spring Boot or TypeScript/NestJS for commerce and booking?
2. **Tenancy:** one business at a time, or one installation serving many companies?
3. **Payment & geography:** Stripe/PayPal/Mollie/other, and EU only vs global?
4. **Booking:** appointment with staff, rental/resource calendar, ticket events or all?
5. **UI:** Angular Material/CDK foundation or PrimeNG style framework?
6. **Deployment:** single VPS with Compose or distributed/managed production?
7. **GitHub:** private vs public; approve proposed 11 repo names; preserve existing `strapi-shop` as planning hub?
8. **Scope:** deliver a complete staged commerce+booking solution or first a minimal website template with add-on modules?

Return decision choices before anyone generates scaffolds. Defaults can be used to continue planning, but must never be misrepresented as approved requirements.


## Approved choices — 2026-10-19 (supersedes proposed defaults above)
**Approved:** D01 full modular platform, phased delivery; D02 single business per deployment; D03 private proposed 11 GitHub repositories (creation still pending); D06 Stripe first, extensible payment adapters; D09 appointments + resource rental + group events (specific business rules still open); D11 Angular Material + Storybook; D12 single VPS with Docker Compose; D13 Java/Spring Boot for transactional core. In addition: admin frontend must be custom Angular using shared UI library, with full booking management; runtime-selectable themes administrated in the dashboard. Google account connection for Calendar and optional Gmail email sending requested. **Google Calendar API** and **Gmail API** are different products/scopes; never parse Gmail messages to run calendar availability.

**Architecture adjustment:** 11 GitHub repositories were previously **proposed**, and their names and private visibility were approved, but logical separation does not imply 11 independently deployed microservices. Recommended **modular Spring Boot monolith** containing separate commerce, booking, calendar integration and worker modules; separate Strapi process, Angular storefront SSR, Angular admin and optional worker execution, PostgreSQL with CMS/application isolation. The user is now questioning whether even the GitHub repositories need separating. Treat the 11-repo choice as **provisional pending clarification on repository boundaries**, without undoing the approved privacy choice. A single monorepo with the same 11 logical projects is the lower-overhead alternative; no need to create either set without reconfirming this tradeoff.

### Remaining architecture-critical decisions (prioritized)
- R01 (BLOCKING): Does 'separated' mean GitHub repositories, backend services, databases, or all? Recommend **single monorepo** and modular-monolith Spring backend on one VPS; retain separate DB/schema ownership for Strapi vs Liquibase.
- R02 (BLOCKING): Google integration authorization: which Google accounts connect (business owner only, multiple staff, every customer)? One-way export vs two-way sync vs availability imports; allow syncing private event details or only busy windows? OAuth consent, offline tokens, revocation, refresh rotation, verification and webhook/polling recovery required.
- R03 (BLOCKING): Gmail function: use Gmail API **to send booking confirmations/reminders** or also read/parse incoming messages? Recommend send-only gmail.send scope, and choose production transactional email provider if sending quotas/deliverability require it.
- R04 (BLOCKING): Booking requirements: individual staff appointments, pooled resources, group classes, ticketed events, max group capacity, waitlists, recurring patterns, deposits, cancellation cutoffs and conflict priority; whether Google busy times block new bookings.
- R05 (BLOCKING): Themes: admin-only or admin + public storefront? Built-in themes + custom branding, editable design tokens and preview, per-user preference versus global site setting, publishing approval and accessibility contrast. Recommend theme catalog curated by developers; admin can select/preview/assign themes, with persisted per-user preference and optional global default. Arbitrary admin-supplied CSS/code not allowed.
- R06 (BLOCKING): Should Angular custom admin replace *all* Strapi editing UI or coexist with protected Strapi editorial admin? Recommend coexist; use Angular for operations including bookings and commerce, Strapi native admin restricted to CMS editors, with optional editorial screens later.
- R07: Business operations region, tax country, currencies, locales, physical/digital products, delivery and invoicing.
- R08: Staff/customer auth provider, guest checkout, MFA, roles and scopes.
- R09: Stripe payments details: Checkout vs Elements, wallets, recurring payments, deposits, capture/refund policies.
- R10: Single VPS specs, DNS/domains/TLS, object storage, external backups, RPO/RTO, secrets and scaling thresholds.
- R11: Analytics/search/transactional email providers, regulated data/consent/legal pages and data residency.
- R12: Technical versions and package/tool compatibility must be checked and pinned immediately before coding; no unverified version pin.
