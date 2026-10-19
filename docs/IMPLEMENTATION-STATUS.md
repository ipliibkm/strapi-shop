> **Writing standard:** Follow [WRITING-STYLE.md](WRITING-STYLE.md). Earlier text in this file has not received a complete language review.

# Implementation status — 2026-10-19
This is an **initial unverified scaffold**, NOT production-ready and NOT a working shop, Google Calendar connection or completed editor. All frontend pages are placeholders; backend routes other than actuator are deliberately denied until authentication.

## Created
- Spring Boot Java 21 Maven project, Actuator health, deny-by-default security configuration.
- PostgreSQL Compose service, separate Strapi-owned CMS database and application database, Liquibase booking/external-busy initial tables.
- Strapi 5 package/config and container Dockerfile (must validate build and exact dependency compatibility).
- Angular 22 workspace definitions with separate storefront/admin apps and shared UI component/theming service.
- Angular Material + Tailwind 4 baseline, independently persisted local admin/storefront theme settings.

## NOT created / must not claim
- OAuth and Google Calendar two-way sync; Gmail message sending; Stripe payment transactions; working appointment slots and conflict handling; secure custom Strapi content editing; fully functioning operations admin; production security, SSL, backups and monitoring; authenticated API endpoints.
- Storybook configuration and stories still need version compatibility validation.
- All dependencies and Docker builds require network-enabled CI validation, which was not available at creation time.

## Immediate next tasks
1. Validate pinned versions, install dependencies and generate lockfiles in CI; run build and tests for all apps.
2. Complete Spring Boot domain modules and enforce authoritative Google busy-block sync before accepting reservations; return HTTP 409 with fresh availability payload/version; admin/storefront must refresh slots.
3. Implement OIDC staff/customer authentication, then content BFF and Strapi 5 Document Service adapter.
4. Implement authenticated Google OAuth connection, sync cursor/webhook, Gmail send-only and Stripe sandbox processing.
5. Implement Storybook Angular integration, full theme catalog, shared UI widgets, a11y tests and multi-app CSS isolation.
6. Harden Compose for VPS with TLS ingress, remote encrypted backups, OTel monitoring, secrets and production load/security tests.

## Implementation continuation — 2026-10-19
### Added in this iteration (source committed, integration not validated)
- Java booking domain API: availability read, operator booking list/create, 409 conflict response and resource-scoped PostgreSQL advisory transaction locks.
- External busy projection upsert service uses matching lock to avoid booking concurrency races **once events have synchronized**.
- Explicit development-only HTTP Basic admin access (Spring profile `dev` with `DEV_ADMIN_PASSWORD`); all production application paths remain denied until OIDC implemented.
- Angular admin booking form, availability check, error handling/refetch after HTTP 409, and shared Angular Material components and Tailwind layout.
- GitHub Actions backend/frontend build workflows and a servlet security controller test skeleton.
- Strapi pinned to 5.58.0 (released Oct 14 2026). Dependency compatibility and actual installation still must be verified.

### Outstanding high-priority safety and completeness gaps
- Google Calendar two-way OAuth, actual ingest of events, sync freshness and provider failure policy: **do not take real appointments yet**. Imported conflicts after an already-confirmed booking need manual conflict queue handling.
- Full customer/service/resource/calendar model, group capacity, booking reschedule/cancellation, idempotency, timezone/daylight savings and opening hours.
- No live Stripe API/webhooks, Gmail OAuth/send, Strapi editor BFF or complete content models.
- Production OIDC/MFA, rate limiting, CSRF policies, TLS gateway, monitoring, backup and restore not deployed.
- Complete Storybook setup, lockfiles, Angular Material theme CSS, SSR, user theme persistence/backend, quality gates and Docker image validation remain.
- Compose only starts backend, CMS and database; Angular is not yet packaged or routed by Compose. **Do not deploy as a production solution.**
- Github repository remains PUBLIC despite requirement for PRIVATE. This needs a repo settings action.

### Verification status
Files committed to GitHub; no network-enabled build/test execution performed in this response. Added Actions YAML does not prove tests passed; consult workflow run results after invocation.

## First-run business installer — 2026-10-19
- [x] Commit `sh setup.sh` / `python3 scripts/setup.py` interactive first-run wizard; company name/slug/contact, timezone, locale, currency, optional address and phone; generated random credentials (private .env), confirmation and resumed start.
- [x] Add Strapi `business-profile` single type and seed-once CMS bootstrap, mounted config file, Compose container health checks, supported Strapi interactive technical admin creation command.
- [ ] Verify full image builds, `compose up`, HTTP health polling, CMS seed and admin CLI against a clean machine. Not tested in a running Docker engine during this turn.
- [ ] Implement full custom Angular first-run business/admin onboarding with OIDC and initial operator roles; current admin step creates only the technical Strapi admin account.
- [ ] Implement TLS/reverse proxy, offsite backups, restoration, role isolation and readiness/security tests before declaring business one-click deployment production-ready.
See [FIRST-RUN-SETUP.md](FIRST-RUN-SETUP.md).

## Browser setup milestone
Added Angular Material + Tailwind setup wizard with company details, first operator record, modules, themes, integration guidance and a one-time setup API. The API stores a BCrypt password hash and a PostgreSQL setup state. It uses a separate setup token. See [BROWSER-SETUP.md](BROWSER-SETUP.md).

**Still incomplete:** production OIDC sign-in, live Google/Gmail/Stripe account connections, branded asset uploads, Strapi profile reconciliation, automated environment verification, secure token rotation/disable, production TLS and access isolation. Build and end-to-end tests have not been executed successfully.

## Setup verification increment — 2026-10-19
- [x] Add `GET /setup/api/checks` for DB, installation state, Google configuration, Stripe configuration and identity configuration. Checks are read-only. They do not validate live external connections.
- [x] Add setup wizard **Check system settings** control with visible status messages.
- [x] Add initial unit tests for invalid bootstrap setup tokens.
- [x] Add clear provider onboarding steps in `docs/INTEGRATIONS-ONBOARDING.md`.
- [ ] Make production OIDC enrollment functional. Current initial operator password hash is not a usable account.
- [ ] Implement OAuth authorization code flow for Google Calendar and Gmail. No real Google connection exists.
- [ ] Implement Stripe Connect or account-specific Stripe setup flow with webhook signing.
- [ ] Reconcile browser setup business details with Strapi profile.
- [ ] Run automated builds, startup and integration tests. No successful build or test result was available in this turn.

## External OIDC sign-in foundation — 2026-10-19
- [x] Add Spring Boot OAuth2 resource server and conditional JWT-protected API chain.
- [x] Check JWT signature, issuer, expiry and configured API audience. Require OPERATOR in the token's `roles` claim for administrator APIs.
- [x] Disable the local development Basic auth chain when OIDC mode is enabled.
- [x] Add OIDC public-client Authorization Code + PKCE integration in Angular with login, callback, logout, and bearer-token interceptor.
- [x] Keep first setup operator password separate from OIDC identity.
- [ ] Select/configure the real OIDC provider, MFA and first-user enrollment; not implemented.
- [ ] Add OIDC provider-backed integration tests, token lifecycle tests and access policies for other roles.
- [ ] Verify Java and Angular builds. Dependencies were not installed or tested in this session.
- [ ] Prefer secured BFF + HttpOnly cookies before full production deployment; current SPA holds token in session storage.
- [ ] Configure same-origin reverse proxy for `/api/v1` in deployed Angular admin.
See [OIDC-AUTH.md](OIDC-AUTH.md).

## Customer storefront UI — 2026-10-19
- [x] Replace the original placeholder with responsive Home, Services, Shop, About, and Contact views.
- [x] Add mobile navigation, sample service cards, and Material + Tailwind styles.
- [x] Add a static nginx container for the storefront on localhost port 8081, included in Compose.
- [ ] Verify the Angular production build and Docker image end-to-end. No successful build proof in this session.
- [ ] Connect real CMS content, branding, media, booking, products, and customer forms. Sample pages are not transactional.
- [ ] Add SEO-ready SSR or prerendering and deployment TLS before public launch.
See [STOREFRONT.md](STOREFRONT.md).
