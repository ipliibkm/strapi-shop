# Customer-facing website

## Start the website

Run `sh setup.sh` first. This command starts the CMS, backend, database, and storefront.

Open `http://127.0.0.1:8081` on the same computer.

To start only the Angular development server, run:
```sh
npm install
npm run start:storefront
```

## Current pages

- Home: Main introduction and service cards.
- Services: Service information.
- Shop: A clear message when the product catalog is not ready.
- About: A placeholder for company information.
- Contact: A placeholder for company contact information.

Use the page menu to change pages. The site has mobile navigation.
Use the theme control to test light, dark, and ocean settings.

## Before public launch

This is an **example storefront**. The brand name, service text, and contact page contain sample content.
The pages do not yet read business data from Strapi. A customer cannot buy a product, send a form, or book an appointment.

Before you publish the site:
1. Connect the Strapi content types to the public website.
2. Replace all sample text with approved business data.
3. Build the product catalog and customer booking flow.
4. Add privacy pages and legal content for the business.
5. Add Angular SSR or prerendering for SEO.
6. Add an HTTPS proxy and production security controls.
7. Test accessibility, performance, and browser compatibility.

The service is bound to the local host by Docker Compose. It is not a public production deployment.

See [implementation status](IMPLEMENTATION-STATUS.md).
