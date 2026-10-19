# Architecture Decision Register — User Input Needed

None of these is silently resolved. Suggested defaults are non-binding and recorded for review.

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
