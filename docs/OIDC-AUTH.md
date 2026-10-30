# Administrator authentication

The admin UI uses local Strapi administrator accounts. This is the only supported administrator authentication mode.

The backend has no OIDC administrator switch. Google OAuth connects external services only.

## Access rules

- Every active Strapi administrator can sign in.
- Strapi applies each administrator's Content Manager permissions.
- Only users with the `strapi-super-admin` role can access platform administration.
- Platform administration includes bookings, services, connections, commerce, appearance, and system functions.
- Google OAuth connects Google services only. It does not authenticate administrators.

## Sign in

1. Open the Angular admin.
2. Enter the email address and password of a local Strapi administrator.
3. Select **Sign in**.

Spring sends the credentials to Strapi through the internal network. Spring does not store the password.

Strapi returns short-lived access data and a refresh session. Spring encrypts these values and stores them in the server-side JDBC session. The browser receives only the HttpOnly platform session cookie and the CSRF cookie.

## Content authorization

The Angular admin sends content operations to the Spring backend. Spring forwards them to Strapi with the signed-in user's encrypted Strapi session. Strapi remains the authorization source for content.

A user without permission for a content type receives `403 Forbidden`. A platform role cannot override this Strapi decision.

## Session configuration

Set these values in `.env`:

```text
PLATFORM_STRAPI_TOKEN_ENCRYPTION_KEY=your-base64-encoded-32-byte-key
PLATFORM_ADMIN_SESSION_TIMEOUT=8h
SERVER_SERVLET_SESSION_COOKIE_SECURE=false
```

The platform uses `PLATFORM_OAUTH_TOKEN_ENCRYPTION_KEY` as a fallback when the dedicated Strapi key is not set.

Use HTTPS and secure cookies in production. Configure MFA in Strapi when the deployment requires it.
