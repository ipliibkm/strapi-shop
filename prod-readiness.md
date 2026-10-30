# Production readiness tasks

This file defines the work required before a production launch. The current system is not production-ready.

## Status and usage

- Complete P0 tasks before production traffic starts.
- Complete P1 tasks before the production sign-off.
- Schedule P2 tasks with an owner and target date.
- Do not mark a task complete without the listed evidence.
- Record design decisions in `docs/DECISIONS.md`.
- Keep secrets, customer data, and production backup data out of Git.
- Use the repository writing rules in `docs/WRITING-STYLE.md`.

Each agent must update its task with:

- Owner or agent name
- Completion date
- Pull request or commit
- Test or drill evidence
- Remaining risk

## P0: Release blockers

### TLS ingress and network exposure

- [x] **PR-TLS-01: Add a production-ready Compose configuration.**
  - Use one `compose.yaml` file with separate development and production environment files.
  - Keep local HTTP behavior in `config/development.env.example`.
  - Add a Traefik 3 service with an immutable image digest.
  - Enable the Docker provider with `exposedByDefault=false`.
  - Expose only TCP ports 80 and 443 to the public network.
  - Do not publish Backend, PostgreSQL, or internal management ports.
  - **Evidence:** `docker compose --env-file config/production.env.example config --quiet` passes.
  - **Owner:** OpenCode agent (GPT-5.6 Sol)
  - **Completion date:** 2026-10-24
  - **Pull request or commit:** Working tree; the repository owner has not requested a commit.
  - **Test evidence:** `./scripts/verify-production-config.sh` passes. The script also verifies the effective published ports.
  - **Remaining risk:** Traefik has read-only Docker socket access. This access is root-equivalent on the host.

- [ ] **PR-TLS-02: Configure automatic TLS certificates.**
  - Configure a named ACME certificate resolver.
  - Store `acme.json` in a persistent, restricted volume.
  - Configure the ACME staging server for deployment tests.
  - Configure the production ACME server only after the staging test passes.
  - Select HTTP-01 or DNS-01 and document the reason.
  - Add certificate renewal failure monitoring.
  - **Evidence:** A public test host receives a trusted certificate and renews successfully in a rehearsal.
  - **Implementation update (2026-10-24):** The production environment configures HTTP-01, staging by default, and persistent ACME storage. Monitoring and a public rehearsal remain open.

- [ ] **PR-TLS-03: Configure HTTPS routing.**
  - Redirect all HTTP requests to HTTPS.
  - Route the storefront and administrator hosts explicitly.
  - Decide whether native Strapi administration requires VPN or IP allowlisting.
  - Do not expose Storybook in production.
  - Do not expose the Setup application after installation.
  - **Evidence:** An external scan finds only the approved hosts and ports.
  - **Implementation update (2026-10-24):** The production environment redirects HTTP and defines explicit Storefront and Admin routes. CMS, Storybook, and Setup stay private. The external scan remains open.

- [ ] **PR-TLS-04: Configure proxy trust.**
  - Preserve the original host, client address, and protocol.
  - Restrict trusted forwarded headers to the Traefik network.
  - Verify that Spring sees HTTPS through `X-Forwarded-Proto`.
  - Verify OAuth redirect URLs through the production host.
  - **Evidence:** Integration tests verify secure redirects, client address handling, and OAuth callback URLs.
  - **Implementation update (2026-10-24):** Nginx now preserves Traefik host, address, and protocol headers. Backend stays on the private network. Integration tests remain open.

- [ ] **PR-TLS-05: Add browser security headers.**
  - Add HSTS only after HTTPS works on all production subdomains.
  - Add a tested Content Security Policy.
  - Add `X-Content-Type-Options: nosniff`.
  - Add a suitable `Referrer-Policy`.
  - Add a restrictive `Permissions-Policy`.
  - Block framing unless a documented feature requires it.
  - Remove obsolete headers when they provide no protection.
  - **Evidence:** Security-header tests pass for Storefront, Admin, CMS, and API responses.
  - **Implementation update (2026-10-24):** Traefik adds HSTS, CSP, content-type, referrer, permissions, and frame protections. Browser tests pass for Storefront and Admin with zero console errors after the build removed the inline critical-CSS script. The CSP allows Google Fonts and the demo image service; production content must use the approved media origin. CMS and API response tests remain open.

### Cookies and browser security

- [ ] **PR-COOKIE-01: Define the cookie inventory.**
  - List the platform session cookie, CSRF cookie, Strapi cookies, and integration cookies.
  - Record purpose, owner, lifetime, domain, path, and classification for each cookie.
  - Classify essential cookies separately from analytics and marketing cookies.
  - **Evidence:** The approved cookie inventory is linked from the privacy documentation.

- [ ] **PR-COOKIE-02: Harden the administrator session cookie.**
  - Set `SERVER_SERVLET_SESSION_COOKIE_SECURE=true` in production.
  - Configure `HttpOnly=true` explicitly.
  - Keep `SameSite=Lax` unless a tested integration requires another value.
  - Restrict the cookie path and domain.
  - Use an approved session timeout.
  - Rotate the session identifier after authentication and privilege changes.
  - Expire the cookie during logout.
  - **Evidence:** Browser tests inspect the production `Set-Cookie` attributes.
  - **Implementation update (2026-10-24):** Production uses `Secure`, `HttpOnly`, `SameSite=Lax`, path `/`, and an eight-hour default timeout. The production browser test remains open.

- [ ] **PR-COOKIE-03: Harden the CSRF cookie flow.**
  - Keep the CSRF cookie readable by Angular only while this pattern is used.
  - Add `Secure` in production.
  - Configure `SameSite` and the path explicitly.
  - Reject unsafe requests with a missing or invalid CSRF token.
  - Verify login, logout, upload, refund, and content-write requests.
  - **Evidence:** Automated browser tests cover valid and invalid CSRF requests.
  - **Implementation update (2026-10-24):** The CSRF cookie uses `Secure`, `SameSite=Lax`, and path `/` in production. A unit test verifies these attributes. Browser flow tests remain open.

- [ ] **PR-COOKIE-04: Add cookie disclosure and consent controls.**
  - Describe all essential cookies in the privacy notice.
  - Do not show a consent banner only for strictly necessary cookies unless legal review requires it.
  - If analytics or marketing cookies are added, block them until consent exists.
  - Store and audit consent changes when consent is required.
  - **Evidence:** Legal review approves the cookie behavior and published notice.

### Backup and restore

- [ ] **PR-BACKUP-01: Approve recovery targets.**
  - Define the recovery point objective (RPO).
  - Define the recovery time objective (RTO).
  - Define backup retention periods.
  - Define who can access backups.
  - **Evidence:** The owner approves the targets in `docs/DECISIONS.md`.

- [ ] **PR-BACKUP-02: Back up PostgreSQL automatically.**
  - Back up both the `platform` and `cms` databases.
  - Include roles, grants, extensions, and schema ownership.
  - Use consistent backups and validate each archive.
  - Add WAL archiving and point-in-time recovery if the approved RPO requires them.
  - Run backup jobs outside the application containers.
  - **Evidence:** A scheduled job creates encrypted backups without manual action.

- [ ] **PR-BACKUP-03: Back up uploaded media.**
  - Back up `cmsuploads` and `productuploads`.
  - Keep database and media recovery points consistent.
  - Detect missing or orphaned files after restore.
  - **Evidence:** A restored environment serves CMS and product images correctly.

- [ ] **PR-BACKUP-04: Protect backup storage.**
  - Encrypt backups before off-site transfer.
  - Use separate credentials for backup storage.
  - Prevent the application host from deleting immutable recovery points.
  - Apply retention and lifecycle rules.
  - Monitor backup age, size, and failure status.
  - **Evidence:** The backup store contains encrypted off-site recovery points and tested retention rules.

- [ ] **PR-BACKUP-05: Create and test restore procedures.**
  - Write database, media, secret, and full-system restore runbooks.
  - Restore into an isolated environment.
  - Verify Liquibase and Strapi migration state after restore.
  - Verify orders, payments, bookings, sessions, content, and media.
  - Measure actual RPO and RTO.
  - Repeat the drill on a defined schedule.
  - **Evidence:** A signed restore report records the date, duration, checks, and problems.

### Audit log

- [ ] **PR-AUDIT-01: Define the audit event contract.**
  - Include event ID, time, actor, action, target type, target ID, result, and request ID.
  - Include source address and user agent when lawful and necessary.
  - Include safe before-and-after values for important changes.
  - Exclude passwords, tokens, secrets, payment data, and unnecessary personal data.
  - Define event versions for future schema changes.
  - **Evidence:** Security and privacy reviewers approve the event contract.

- [ ] **PR-AUDIT-02: Add append-only audit storage.**
  - Add a dedicated audit table or external audit sink.
  - Prevent normal application roles from updating or deleting audit records.
  - Add indexes for time, actor, action, and target.
  - Define retention and archive behavior.
  - Consider hash chaining or signed export batches when tamper evidence is required.
  - **Evidence:** Database permission tests prove that application users cannot alter existing events.

- [ ] **PR-AUDIT-03: Record security events.**
  - Record login success and failure without recording credentials.
  - Record logout, session revocation, role changes, and denied administrator actions.
  - Record setup completion, secret rotation metadata, and integration connect or disconnect actions.
  - **Evidence:** Security integration tests produce the expected events.

- [ ] **PR-AUDIT-04: Record business changes.**
  - Record order state changes, refunds, fulfillment, stock adjustments, and product changes.
  - Record booking creation, rescheduling, cancellation, and calendar connection changes.
  - Record content publication, form deletion, settings changes, and data exports.
  - Link Stripe webhook events without treating the webhook table as the audit log.
  - **Evidence:** Tests verify actor, target, result, and redaction for each event family.

- [ ] **PR-AUDIT-05: Add audit review tools.**
  - Add authorized search and export functions.
  - Restrict access to approved roles.
  - Audit access to the audit log itself.
  - Add alerts for high-risk event patterns.
  - **Evidence:** An authorized user can investigate a test incident from start to finish.

### Secrets and credentials

- [ ] **PR-SECRET-01: Remove production secrets from environment files.**
  - Select an approved secret manager or encrypted file workflow.
  - Mount secrets as files where supported.
  - Do not expose secret values through Compose inspection, logs, or process arguments.
  - Keep `.env` for local development only.
  - **Evidence:** A production configuration scan finds no plaintext secret values.

- [ ] **PR-SECRET-02: Define secret rotation.**
  - Cover database passwords, Strapi keys, JWT secrets, encryption keys, Stripe secrets, Google secrets, and the setup token.
  - Define rotation intervals and emergency rotation steps.
  - Support overlap where rotation requires old and new keys.
  - Test the effect of encryption-key loss before launch.
  - **Evidence:** A rotation drill succeeds without data loss or excessive downtime.

- [ ] **PR-SECRET-03: Add secret scanning.**
  - Scan Git history and new commits.
  - Scan container build contexts and published images.
  - Block commits that contain verified credentials.
  - Define the response procedure for a leaked secret.
  - **Evidence:** CI rejects a safe test credential fixture that matches the configured rule.

### Production authentication and authorization

- [ ] **PR-AUTH-01: Complete the production authentication controls.**
  - Use the documented Strapi session mode.
  - Document any future migration path before adding another mode.
  - Require MFA for privileged users.
  - Test account recovery and administrator lockout.
  - **Evidence:** The approved identity design and tests identify one active security chain.

- [ ] **PR-AUTH-02: Enforce least privilege.**
  - Define roles for content, bookings, commerce, reports, support, and system administration.
  - Do not give every operator `SUPER_ADMIN` access.
  - Enforce permissions in the backend, not only in the UI.
  - Add authorization tests for every privileged endpoint.
  - **Evidence:** A permission matrix maps roles to tested API operations.

- [ ] **PR-AUTH-03: Disable first-run setup after installation.**
  - Remove the Setup service from the production deployment.
  - Disable setup endpoints after successful installation.
  - Rotate or delete the setup token.
  - Verify that setup details are not returned after installation.
  - **Evidence:** External and internal requests cannot repeat or inspect setup.
  - **Implementation update (2026-10-24):** Production excludes the Setup application and disables Backend setup endpoints. Token rotation and deployment verification remain open.

- [ ] **PR-AUTH-04: Harden administrator sessions.**
  - Define idle and absolute timeouts.
  - Limit concurrent sessions if required.
  - Support server-side session revocation.
  - Revoke sessions after role or password changes.
  - Add alerts for repeated login failures.
  - **Evidence:** Session lifecycle and revocation browser tests pass.

### Public endpoint protection

- [ ] **PR-EDGE-01: Add rate limits.**
  - Apply separate limits to login, booking, checkout, forms, uploads, and webhooks.
  - Key limits by a safe combination of source, account, and resource.
  - Return clear `429` responses and `Retry-After` where applicable.
  - Do not rate-limit trusted Stripe retries in a way that loses payment events.
  - **Evidence:** Load tests verify limits and legitimate retry behavior.
  - **Implementation update (2026-10-26):** Public order lookup permits 20 attempts per order and network address each minute. The other endpoint limits and `Retry-After` behavior remain open.

- [ ] **PR-EDGE-02: Add bot and abuse controls.**
  - Add spam controls to public forms and booking endpoints.
  - Add request limits and validation before expensive processing.
  - Consider a challenge only for suspicious traffic.
  - Define temporary blocking and appeal procedures.
  - **Evidence:** Automated abuse scenarios do not exhaust stock, booking capacity, memory, or worker queues.

- [ ] **PR-EDGE-03: Harden uploads.**
  - Verify file signatures instead of trusting extensions.
  - Restrict types, dimensions, and decompressed size.
  - Generate safe server-side file names.
  - Prevent executable uploads and path traversal.
  - Add malware scanning when the risk assessment requires it.
  - Serve untrusted files from a separate origin when necessary.
  - **Evidence:** Security tests cover malformed, oversized, polyglot, and malicious files.

### Monitoring and alerting

- [ ] **PR-OBS-01: Collect production metrics.**
  - Collect application, JVM, PostgreSQL, proxy, host, and container metrics.
  - Protect the Prometheus endpoint from public access.
  - Add dashboards for latency, errors, saturation, payments, bookings, and stock failures.
  - **Evidence:** Dashboards show data from a production-like deployment.

- [ ] **PR-OBS-02: Centralize logs.**
  - Use structured logs with request and correlation IDs.
  - Redact tokens, cookies, credentials, and personal data.
  - Define retention and access controls.
  - Preserve useful logs when an application container is replaced.
  - **Evidence:** An operator can trace one request across Traefik, Backend, and CMS.
  - **Implementation update (2026-10-26):** Mail delivery logs contain neither recipient addresses nor provider exception bodies. Central collection, correlation IDs, retention, and broader redaction verification remain open.

- [ ] **PR-OBS-03: Configure alerts.**
  - Alert on external availability, TLS expiry, error rate, latency, disk space, database health, backup failure, and queue delay.
  - Alert on payment webhook failures and stale calendar synchronization.
  - Send host-down alerts through a service outside the VPS.
  - Define severity, owner, and response time for each alert.
  - **Evidence:** A controlled failure sends and resolves each critical alert.

### Privacy, retention, and legal review

- [ ] **PR-PRIV-01: Create a data inventory.**
  - Map personal data, purpose, legal basis, processor, location, and retention.
  - Include orders, bookings, forms, logs, audit records, backups, Stripe, and Google.
  - Identify special-category data and prohibit it where it is not required.
  - **Evidence:** The privacy owner approves the inventory.

- [ ] **PR-PRIV-02: Implement retention and deletion.**
  - Define retention for bookings, abandoned orders, forms, sessions, logs, exports, and audit records.
  - Preserve financial records when law requires it.
  - Add scheduled deletion or anonymization jobs.
  - Propagate deletion to backups according to the approved policy.
  - **Evidence:** Time-controlled tests prove deletion and legal holds.

- [ ] **PR-PRIV-03: Implement data-subject procedures.**
  - Support access, correction, export, restriction, and deletion requests.
  - Authenticate the requester before disclosure.
  - Record request handling in the audit log.
  - **Evidence:** A full request drill completes within the approved response time.

- [ ] **PR-PRIV-04: Complete ecommerce and privacy review.**
  - Review checkout, pricing, tax, cancellation, refund, delivery, and consumer-rights text.
  - Publish privacy, legal notice, terms, and cookie information.
  - Complete processor agreements for hosting, Stripe, Google, email, monitoring, and backups.
  - Confirm regional data residency requirements.
  - **Evidence:** Qualified reviewers approve the launch documents and controls.

## P1: Required operational controls

### Container and host hardening

- [ ] **PR-HARD-01:** Run Backend, CMS, Nginx, and Traefik as non-root where supported.
- [ ] **PR-HARD-02:** Use read-only root filesystems and explicit writable mounts where possible.
- [ ] **PR-HARD-03:** Drop unused Linux capabilities and enable `no-new-privileges`.
- [ ] **PR-HARD-04:** Add CPU, memory, process, and log-size limits.
- [ ] **PR-HARD-05:** Pin every production image by digest and define an update process.
- [ ] **PR-HARD-06:** Harden SSH, the firewall, automatic security updates, time synchronization, and administrator access on the VPS.
- [ ] **PR-HARD-07:** Avoid direct Docker socket access where possible. Otherwise, document and mitigate its root-equivalent risk.
- [ ] **Evidence:** Container and host benchmarks produce no unapproved critical or high findings.

### Deployment and rollback

- [ ] **PR-DEPLOY-01:** Define separate development, staging, and production configuration.
- [ ] **PR-DEPLOY-02:** Build immutable images once and promote the same digests.
- [ ] **PR-DEPLOY-03:** Generate an SBOM and provenance for each released image.
- [ ] **PR-DEPLOY-04:** Add protected production environments and explicit approval.
- [ ] **PR-DEPLOY-05:** Add pre-deployment backup, migration checks, health checks, and smoke tests.
- [ ] **PR-DEPLOY-06:** Write rollback rules for application and database changes.
- [ ] **PR-DEPLOY-07:** Use expand-migrate-contract changes when a rollback must remain possible.
- [ ] **Evidence:** A staging deployment and rollback drill use the documented process.

### Database operations

- [ ] **PR-DB-01:** Use separate least-privilege roles for application, CMS, migration, and backup operations.
- [ ] **PR-DB-02:** Set connection, statement, lock, and idle transaction timeouts.
- [ ] **PR-DB-03:** Monitor connections, slow queries, locks, table growth, and index health.
- [ ] **PR-DB-04:** Test migrations against a production-size restored database.
- [ ] **PR-DB-05:** Document maintenance, vacuum, analyze, upgrade, and corruption response procedures.
- [ ] **Evidence:** Database role tests and a migration rehearsal pass.

### Media and object storage

- [ ] **PR-MEDIA-01:** Decide whether production media stays on the VPS or moves to S3-compatible storage.
- [ ] **PR-MEDIA-02:** Configure durable off-host media storage when required by the availability target.
- [ ] **PR-MEDIA-03:** Define cache, CDN, lifecycle, versioning, and deletion behavior.
- [ ] **PR-MEDIA-04:** Verify database-to-media consistency and orphan cleanup.
- [ ] **Evidence:** Upload, restore, deletion, and origin-failure tests pass.

### Dependency and supply-chain security

- [ ] **PR-SUPPLY-01:** Run dependency audits and container scans in CI.
- [ ] **PR-SUPPLY-02:** Define severity gates and time-limited exception records.
- [ ] **PR-SUPPLY-03:** Track the 15 current moderate CMS findings until compatible fixes exist.
- [ ] **PR-SUPPLY-04:** Test and remove compatibility patches when upstream packages become compatible.
- [ ] **PR-SUPPLY-05:** Configure automated update proposals with full build and browser gates.
- [ ] **Evidence:** CI blocks an unapproved severe finding and reports approved exceptions.

### Application security verification

- [ ] **PR-SEC-01:** Complete a threat model for authentication, payments, bookings, forms, uploads, CMS, and administration.
- [ ] **PR-SEC-02:** Review the application against OWASP ASVS and API Security Top 10 requirements.
- [ ] **PR-SEC-03:** Add SAST, dependency, secret, container, and dynamic security scans.
- [ ] **PR-SEC-04:** Commission an independent penetration test before launch.
- [ ] **PR-SEC-05:** Fix or formally accept each finding with an owner and expiry date.
- [ ] **Evidence:** The security report has no unapproved launch blockers.

### Capacity and performance

- [ ] **PR-PERF-01:** Define expected users, products, orders, bookings, uploads, and peak request rates.
- [ ] **PR-PERF-02:** Define latency, availability, and Core Web Vitals targets.
- [ ] **PR-PERF-03:** Test checkout contention, duplicate webhooks, booking conflicts, exports, uploads, and CMS publishing.
- [ ] **PR-PERF-04:** Test PostgreSQL connection limits, disk growth, and container memory limits.
- [ ] **PR-PERF-05:** Define scaling and capacity thresholds for the single VPS.
- [ ] **Evidence:** A signed load-test report meets the approved targets.

### Failure and recovery drills

- [ ] **PR-DRILL-01:** Test PostgreSQL restart and complete database loss.
- [ ] **PR-DRILL-02:** Test disk-full and low-memory conditions.
- [ ] **PR-DRILL-03:** Test Stripe, Google, email, DNS, and certificate-provider outages.
- [ ] **PR-DRILL-04:** Test duplicate and delayed events after application restart.
- [ ] **PR-DRILL-05:** Test a failed deployment and rollback.
- [ ] **PR-DRILL-06:** Document that single-host Compose has no host-level high availability.
- [ ] **Evidence:** Drill reports record detection, response, recovery time, and follow-up work.

## P2: Reliability improvements

### Availability and scaling

- [ ] **PR-HA-01:** Decide when the service must move beyond one VPS.
- [ ] **PR-HA-02:** Define thresholds for managed PostgreSQL, replicated storage, and multiple application hosts.
- [ ] **PR-HA-03:** Remove local state that prevents horizontal scaling.
- [ ] **PR-HA-04:** Test rolling replacement and graceful shutdown.
- [ ] **PR-HA-05:** Review regional failover after business targets require it.

### Operational documentation

- [ ] **PR-RUNBOOK-01:** Write installation and first-launch runbooks.
- [ ] **PR-RUNBOOK-02:** Write deployment, migration, rollback, backup, and restore runbooks.
- [ ] **PR-RUNBOOK-03:** Write incident runbooks for authentication, payment, booking, database, storage, and certificate failures.
- [ ] **PR-RUNBOOK-04:** Define on-call ownership, escalation, communication, and incident review.
- [x] **PR-RUNBOOK-05:** Remove outdated status statements from the documentation.
  - **Evidence:** The 2026-10-26 Markdown and HTML audit reconciled authentication, order access, form ownership, mail-log privacy, review status, and verification counts.
- [ ] **PR-RUNBOOK-06:** Keep a production architecture diagram and service inventory current.

### Continuous verification

- [ ] **PR-CI-01:** Run backend tests, clean frontend installs, builds, formatting, and audits on every relevant change.
- [ ] **PR-CI-02:** Run integration tests against PostgreSQL, Strapi, Stripe test mode, and mocked external failures.
- [ ] **PR-CI-03:** Run browser smoke tests for Storefront, Admin, CMS, Setup when enabled, and Storybook when published internally.
- [ ] **PR-CI-04:** Add accessibility, visual regression, and responsive layout checks.
- [ ] **PR-CI-05:** Schedule backup restore, dependency, TLS, and external availability checks.
- [ ] **PR-CI-06:** Store test evidence with each release.

## Production launch gate

Do not approve production launch until all conditions are true:

- [ ] All P0 tasks are complete.
- [ ] All P1 tasks are complete or have an approved, time-limited exception.
- [ ] The latest backup restore drill meets the RPO and RTO.
- [ ] TLS, cookie, security-header, and external exposure tests pass.
- [ ] The security review has no unapproved critical or high findings.
- [ ] The load test meets the approved capacity and latency targets.
- [ ] Monitoring and alerts detect the tested failures.
- [ ] The privacy and legal reviews are approved.
- [ ] The deployment and rollback drills pass.
- [ ] The owner accepts the documented single-host availability limit.
- [ ] The launch owner records a dated production sign-off.

## Code-review status

The working tree fixes all six findings from the 2026-10-26 review.

Current review verification:

- 98 backend tests pass.
- Frontend and backend format checks pass.
- Backend, Admin, Storefront, Storybook, and CMS container builds pass.
- All seven Compose services report healthy.
- Browser smoke tests pass for the Storefront order route and administrator UI.
- The root production dependency audit reports zero vulnerabilities.
- The CMS production dependency audit reports 15 moderate transitive Strapi findings.

## Evidence register

Add one row for each completed task or grouped verification run.

| Date       | Task ID     | Owner                        | Evidence                                                                                                                | Result | Remaining risk                                          |
| ---------- | ----------- | ---------------------------- | ----------------------------------------------------------------------------------------------------------------------- | ------ | ------------------------------------------------------- |
| 2026-10-24 | PR-TLS-01   | OpenCode agent (GPT-5.6 Sol) | `./scripts/verify-production-config.sh`                                                                                 | Pass   | Traefik has read-only Docker socket access.             |
| 2026-10-24 | PR-TLS-05   | OpenCode agent (GPT-5.6 Sol) | Browser smoke tests for `storefront.localhost` and `admin.localhost`                                                    | Pass   | The CSP allows Google Fonts and the demo image service. |
| 2026-10-24 | PR-TLS-05   | OpenCode agent (GPT-5.6 Sol) | `docker compose ps` health status for all seven services                                                                | Pass   | Single-host deployment has no host-level availability.  |
| 2026-10-26 | Code review | OpenCode agent (GPT-5.6 Sol) | 98 backend tests, format checks, five image builds, browser smoke tests, Compose validation, and seven healthy services | Pass   | CMS audit has 15 moderate transitive findings.          |
