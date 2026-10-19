# Set up a business in the browser

**Status: development preview. Do not expose this setup service to the internet.**

## 1. Prepare the system
Install Docker Compose, Python 3.10 or later, and Node.js compatible with Angular 22.
Run `sh setup.sh --skip-admin`. The script writes private secrets and starts the backend and CMS. It waits for their health endpoints.

If your `.env` file already exists from an earlier setup, generate a new secret:
```sh
python3 -c "import secrets; print('PLATFORM_SETUP_TOKEN='+secrets.token_urlsafe(48))"
```
Add the output to `.env`. Do not commit the file. Restart the backend with `docker compose up -d backend`.

## 2. Start the browser wizard
```sh
npm install
npm run start:setup
```
Open the address that the Angular CLI displays. Use it only on the same trusted machine. The development server proxies `/setup/api` to Spring Boot on port 8080.

## 3. Enter business information
Enter the company name, contact email, time zone, language, currency, address, and phone number.

## 4. Create the first operator
Enter the operator email and a password with at least 14 characters.
The backend stores a BCrypt password hash in PostgreSQL.
**This does not create an OIDC account or allow you to sign in to the Angular admin.**
Keep the technical Strapi administrator separate.

## 5. Select modules and themes
Select commerce, bookings, Google Calendar, and Gmail modules.
The website module is required.
Select the admin theme and storefront theme independently.
A selected module is a saved setting. It does **not** mean that a provider is connected.

## 6. Check and save
Find `PLATFORM_SETUP_TOKEN` in your private `.env` file.
Enter the token on the last page.
Select **Save setup**.
The backend allows one completed setup per database. It rejects a second setup with HTTP 409.

## Security limits
- The setup API is accessible only through a trusted local deployment. It is not hardened for exposure to the public internet.
- The setup token is a bearer secret. Do not share it. Rotate or disable it after the first setup.
- The setup token is compared server-side. The administrator password is hashed.
- The wizard does not configure Google OAuth, Gmail, Stripe, TLS, backups, or production identity.
- The backend setup record is not yet synchronized with the Strapi business profile.
- The setup wizard is not part of the Docker Compose service list.
- This implementation has not passed an end-to-end build or security test.

## Next checks
1. Run Java unit tests and the Angular production build.
2. Test a clean PostgreSQL database and two concurrent setup requests.
3. Test an invalid token, a duplicate setup, and invalid input.
4. Connect the operator onboarding flow to the production OIDC service.
5. Add provider connection screens and remote configuration checks.
6. Remove or disable the setup API in production after successful onboarding.

## Check the installation
On the final page, select **Check system settings**.
The app now reads `GET /setup/api/checks`. It shows the database and installation state. It also shows whether Google, Stripe, and identity settings exist.
The result `configuration_present` does not prove that an external service works.
Read [integration instructions](INTEGRATIONS-ONBOARDING.md) before you enable integrations.

**Important:** The first administrator record is not a working identity account. The production OIDC enrollment step still needs implementation. The Strapi business profile is not yet synchronized from the saved setup record.
