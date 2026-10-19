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
