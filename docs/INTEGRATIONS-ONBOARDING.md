# Configure external services

These steps configure the implemented provider connections. A saved setting does not prove that the provider works.

## Google Calendar

1. Create a Google Cloud project for the business.
2. Enable the Google Calendar API.
3. Configure the OAuth consent screen.
4. Register the correct OAuth redirect address.
5. Keep the client secret outside Git.
6. Request only the necessary Calendar permissions.
7. Sign in as a Strapi Super Admin and connect the business Google account in **Connections**.
8. Test calendar edits, external busy blocks, token revocation, and conflict handling.

**Current state:** The platform implements Google OAuth, calendar mapping, incremental synchronization, and booking delivery. It supports one shared service connection. Production consent verification and failure drills remain open.

## Gmail

1. Enable the Gmail API.
2. Request the `gmail.send` permission only.
3. Connect the account with user consent.
4. Test one confirmation email and one reminder.
5. Check limits, retries, and failed sends.

**Current state:** Gmail sending is active when Google OAuth includes `gmail.send`. SMTP is used instead when `MAIL_HOST` is set. Durable mail queues, deduplication, bounce handling, and rate limits are not implemented.

## Stripe

1. Create or select the business Stripe account.
2. Choose the correct test or live mode.
3. Store the secret key outside Git.
4. Register a signed webhook endpoint.
5. Verify payments and refunds in test mode.
6. Start live payments only after you complete the legal and security review.

**Current state:** The platform creates Stripe Checkout sessions, verifies signed webhooks, and requests refunds. Live-mode configuration, reconciliation, and production verification remain open.

## Administrator sign-in

Use local Strapi administrator accounts for the current admin UI. Require MFA for privileged accounts when the Strapi deployment supports the approved policy.
Do not use the first operator record as an active login. Create the administrator in Strapi.
The platform supports Strapi sessions for administrator sign-in. It does not provide an OIDC administrator mode.

## Safety

A check named `configuration_present` means the setting exists. It does **not** mean the connection works.
Do not start production traffic until the tests pass.
