# Connect administrator sign-in

This feature uses OpenID Connect (OIDC). OIDC verifies the identity of a user.
The current code supports an external OIDC provider. It does not create user accounts in that provider.

## 1. Configure the identity provider
1. Create an OIDC client for the Angular admin.
2. Use the **Authorization Code flow with PKCE**.
3. Use a public client. Do not put a client secret in Angular.
4. Register `https://ADMIN_DOMAIN/auth/callback` as the redirect address.
5. Register `https://ADMIN_DOMAIN/` as the logout redirect address.
6. Issue an access token with a `roles` claim that contains `OPERATOR` for approved staff.
7. Configure the API audience. Check that the identity provider includes this audience in its access tokens.

## 2. Configure Spring Boot
Set these values in the environment:
- `PLATFORM_AUTH_ENABLED=true`
- `PLATFORM_OIDC_ISSUER=https://IDP_REALM`
- `PLATFORM_OIDC_AUDIENCE=YOUR_API_AUDIENCE`
- `PLATFORM_OIDC_CLIENT_ID=YOUR_PUBLIC_CLIENT_ID`

Do not enable OIDC before the issuer and audience values are correct.
Use HTTPS in production.

## 3. Create the first administrator
Enroll the administrator in the OIDC provider. Assign the `OPERATOR` role.
The setup wizard's initial password hash **does not** create the account in the provider.
The enrollment process needs a separate, verified provider integration.
Do not sign in with the setup password.

## 4. Test the connection
1. Open the Angular admin.
2. Select **Sign in**.
3. Sign in through the OIDC provider.
4. Confirm that the API accepts the access token.
5. Confirm that an unapproved user cannot access administrator APIs.
6. Sign out. Confirm that the app removes its local session.

## Security limits
- The current frontend stores the OIDC session in browser session storage. A BFF with secure, HttpOnly session cookies is a stronger production option and remains planned.
- Token refresh, MFA enforcement, identity enrollment, account recovery and account disablement need tests with the selected provider.
- Role claims are validated through a server-side JWT. The frontend does not grant privileges.
- Keep setup and identity provider management endpoints on restricted networks.
- This code has not passed a production security review or end-to-end tests.
