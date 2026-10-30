# Production deployment

## Scope

The repository uses one `compose.yaml` file for local and production deployments. An environment file selects hosts, TLS, and exposed applications. This configuration does not make the system ready for production by itself.

The production host publishes these TCP ports:

- Port 80 for the ACME HTTP challenge and HTTPS redirects
- Port 443 for Storefront and Admin HTTPS traffic

PostgreSQL, Backend, CMS, Storybook, and Traefik management endpoints do not publish host ports. The production deployment does not include the Setup application. The Backend also disables setup endpoints.

The native Strapi administration interface is not public. This implements decision R06 in `docs/DECISIONS.md`.

## Start a local deployment

1. Copy `config/development.env.example` to `config/development.env`.
2. Replace the development-only secret values.
3. Start the services:

```bash
docker compose --env-file config/development.env up -d --build
```

Open these local addresses:

- Storefront: `http://storefront.localhost`
- Admin: `http://admin.localhost`
- CMS: `http://cms.localhost`
- Storybook: `http://storybook.localhost`

Traefik is the only service that publishes host ports. Local HTTP does not redirect to HTTPS. Local HTTPS uses Traefik's generated certificate and causes a browser warning.

## Validate the configuration

Run this command before each deployment:

```bash
./scripts/verify-production-config.sh
```

The script validates both example environment files. It verifies the effective production configuration and its public ports.

## Configure DNS and certificates

1. Set `STOREFRONT_HOST` to the public Storefront host.
2. Set `ADMIN_HOST` to the public Admin host.
3. Point both DNS records to the production host.
4. Set `ACME_EMAIL` to the monitored operations address.
5. Keep the staging ACME server for the first deployment.
6. Start the production services with `docker compose --env-file /secure/path/production.env up -d`.
7. Verify certificate issue and renewal with the staging server.
8. Set `ACME_CA_SERVER` to `https://acme-v02.api.letsencrypt.org/directory`.
9. Start Traefik again to request trusted certificates.

The deployment uses HTTP-01 because the single VPS can receive public traffic on port 80. HTTP-01 does not require DNS-provider credentials on the host.

Traefik stores ACME state in the `letsencrypt` volume. Only Traefik mounts this volume. Back up this volume with the other production state. Do not copy `acme.json` to Git.

## Supply configuration and secrets

Do not use `config/production.env.example` for a deployment. It contains validation-only values. Generate a restricted production environment file outside the repository.

Supply non-secret variables through the deployment environment. Supply secret variables through the approved secret workflow. The secret-manager decision is still open.

Required production variables include:

- `STOREFRONT_HOST`
- `ADMIN_HOST`
- `ACME_EMAIL`
- PostgreSQL and CMS database credentials
- Strapi keys and token salts
- Platform encryption keys
- Stripe and Google credentials when these integrations are active

## Proxy trust boundary

Traefik removes untrusted forwarded-header values at the public edge. It sends the original host, client address, and protocol to Nginx. Nginx preserves these values when it sends API requests to Backend.

Backend has no public port. Only services on the private `internal` network can reach it. Spring uses framework forwarded-header processing to reconstruct secure request URLs.

## Security headers

Traefik adds these headers to public Storefront and Admin responses:

- `Strict-Transport-Security`
- `Content-Security-Policy`
- `X-Content-Type-Options`
- `Referrer-Policy`
- `Permissions-Policy`
- `X-Frame-Options`

Test the Content Security Policy with all active integrations before production sign-off. HSTS includes subdomains. Do not use the production environment file if a production subdomain does not support HTTPS.

## Remaining controls

The following controls need external infrastructure or owner approval:

- Certificate-expiry and renewal-failure alerts
- External host and port scans
- Secret-manager integration and rotation drills
- Backup storage and restore drills
- Production monitoring and alert delivery
- Legal and privacy approval
