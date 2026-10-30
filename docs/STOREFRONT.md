# Customer-facing website

## Start the website

Run the setup command:

```sh
sh setup.sh
```

For an existing installation, use `sh setup.sh --start-only`.

Open `http://storefront.localhost` on the same computer.

To start only the Angular development server, run:

```sh
npm install
npm run start:storefront
```

## Current pages

- Home: Published page-builder content, or a static hero with service cards when not published.
- Services: Published page-builder content, or a static service-card layout when not published.
- Shop: Published intro blocks plus the public product grid; a clear message when the catalog is empty.
- Booking: A three-step appointment flow (duration, time slot, customer details).
- Product and cart: Product detail by slug, stock-aware cart editing, checkout, and order confirmation.
- About and contact: Published page-builder content; the contact page renders its dynamic form.

Use the page menu to change pages. The site has mobile navigation.
The header shows a language switcher when more than one website language is enabled in the admin panel.
Browser Back and Forward navigation works between the shop, product, and cart pages.

## Cart and stock behavior

- The cart is stored in browser local storage for the current browser.
- A product page shows the quantity that is already in the cart.
- The quantity cannot exceed the current available stock.
- If stock decreases, the storefront reduces an old cart quantity and shows a warning.
- The cart page applies the same stock limit to quantity changes.
- The backend checks stock again when it creates the order. This check is authoritative.
- A successful checkout clears the cart and hides previous cart warnings and totals.
- The confirmation displays the order number and the hosted payment action when payment is required.
- Stripe returns the customer to an order link with a private token in the URL fragment. The storefront sends the token in the `X-Order-Access-Token` header.

Stock is not reserved when a customer only adds an item to the browser cart. The backend reserves stock when it creates a pending order.

## Before public launch

This is an **example storefront**. The brand name, service text, and page content contain sample data from the page builder.

Before you publish the site:

1. Replace all sample page content with approved business data in the admin panel.
2. Fill the product catalog and set shop settings (opening state, payment mode).
3. Add privacy pages and legal content for the business.
4. Add Angular SSR or prerendering for SEO.
5. Configure trusted production HTTPS and the remaining production security controls.
6. Test accessibility, performance, and browser compatibility.

The service is bound to the local host by Docker Compose. It is not a public production deployment.

See [implementation status](IMPLEMENTATION-STATUS.md).
