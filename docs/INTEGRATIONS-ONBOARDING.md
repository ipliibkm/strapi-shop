# Configure external services

This document uses ASD-STE100-style English. These steps describe configuration. They do not establish live connections.

## Google Calendar
1. Create a Google Cloud project for the business.
2. Enable the Google Calendar API.
3. Configure the OAuth consent screen.
4. Register the correct OAuth redirect address.
5. Keep the client secret outside Git.
6. Request only the necessary Calendar permissions.
7. Connect each permitted staff account through OAuth after an OAuth callback service exists.
8. Test calendar edits, external busy blocks, token revocation, and conflict handling.

**Current state:** The setup checks whether a Google client ID is present. It does not perform OAuth.

## Gmail
1. Enable the Gmail API.
2. Request the `gmail.send` permission only.
3. Connect the account with user consent.
4. Test one confirmation email and one reminder.
5. Check limits, retries, and failed sends.

**Current state:** Gmail sending is not yet active.

## Stripe
1. Create or select the business Stripe account.
2. Choose the correct test or live mode.
3. Store the secret key outside Git.
4. Register a signed webhook endpoint.
5. Verify payments and refunds in test mode.
6. Start live payments only after you complete the legal and security review.

**Current state:** The setup checks whether Stripe settings exist. It does not make a payment or connect a Stripe account.

## Administrator sign-in
Use an OIDC identity provider with MFA for staff.
Do not use the first operator password record as an active login until you complete the account enrollment and verification flow.
Test account activation, sign-in, access rules, and account recovery.

## Safety
A check named `configuration_present` means the setting exists. It does **not** mean the connection works.
Do not start production traffic until the tests pass.
