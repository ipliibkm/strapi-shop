# Connect Google services

Google OAuth connects platform services. It does not sign an administrator into the admin UI.

Only a signed-in Strapi Super Admin can start or remove a Google connection. The platform requests Calendar event management, calendar-list discovery, and Gmail send scopes (`gmail.send`). It must not request mailbox read access.

The same connected account sends form submission mail and shop order confirmation mail. Without SMTP settings (empty `MAIL_HOST`), the backend uses this account through the Gmail API.

## Create the Google client

1. Open Google Cloud Console.
2. Enable the Google Calendar API and the Gmail API.
3. Configure the OAuth consent screen.
4. Create an OAuth client with application type **Web application**.
5. Add `http://localhost/login/oauth2/code/google` as an authorized redirect URI for local use. Google Cloud rejects `.localhost` subdomains such as `admin.localhost` with a top-level-domain error.
6. Add the production HTTPS callback before deployment.

The backend must send exactly the registered URI, so set `PLATFORM_ADMIN_BASE_URL=http://localhost` in `.env`. Traefik routes both hostnames to the admin UI.

## Configure the platform

Set these values in `.env`:

```text
PLATFORM_GOOGLE_ENABLED=true
PLATFORM_ADMIN_BASE_URL=http://localhost
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret
PLATFORM_OAUTH_TOKEN_ENCRYPTION_KEY=your-base64-encoded-32-byte-key
PLATFORM_GOOGLE_SYNC_INTERVAL_MS=60000
PLATFORM_GOOGLE_SYNC_INITIAL_LOOKBACK_DAYS=31
PLATFORM_GOOGLE_DELIVERY_INTERVAL_MS=15000
PLATFORM_ADMIN_SESSION_TIMEOUT=8h
SERVER_SERVLET_SESSION_COOKIE_SECURE=false
```

Generate the token encryption key once:

```sh
openssl rand -base64 32
```

Keep this key stable. The backend cannot decrypt stored Google tokens after you replace or lose it.

Restart the backend and admin:

```sh
docker compose up -d --build backend admin
```

1. Open `http://admin.localhost/`.
2. Sign in with a local Strapi Super Admin account.
3. Open **Connections**.
4. Select **Connect Google services**.

Set `SERVER_SERVLET_SESSION_COOKIE_SECURE=true` when the production admin uses HTTPS.

## Separate security states

The Strapi administrator session and Google service grant are independent.

- Signing out of the admin UI does not disconnect Google services.
- Disconnecting Google services does not sign out the administrator.
- A backend restart preserves both states in PostgreSQL.
- Closing the browser removes the administrator session cookie.

The backend encrypts Google access and refresh tokens with AES-256-GCM. The tokens do not enter Angular browser storage.

## Calendar synchronization

Use **Connections** to map each resource ID to one Google calendar. The worker imports busy events every 60 seconds by default. Later imports use Google incremental synchronization tokens.

The worker removes cancelled events and excludes events that this platform created. A mapping reports a conflict when an imported event overlaps a confirmed booking.

Confirmed bookings use persistent delivery records. Create, reschedule, and cancel operations are sent to Google. Failed deliveries use retry delays and stop after eight attempts. Select **Sync now** to requeue failed deliveries.

## Security requirements

- Use HTTPS and secure cookies outside localhost.
- Keep the Google client secret only in backend environment configuration.
- Back up `PLATFORM_OAUTH_TOKEN_ENCRYPTION_KEY` in an approved secret manager.
- Keep scopes limited to Calendar event management, read-only calendar discovery, and Gmail send.
- Review Google consent and verification requirements before external use.

## Re-authorize after adding the Gmail scope

Stored refresh tokens keep only the scopes granted at connection time. After this change:

1. Open **Connections** in the admin UI.
2. Remove the existing Google connection.
3. Connect the account again and approve the added `gmail.send` scope.

Mail sending through Google works only after this re-authorization. Calendar bookings keep working with the previous token until then.
