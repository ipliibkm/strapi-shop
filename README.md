# Example Platform

This repository contains an example commerce, booking, content, and administration platform.

Open [README.html](README.html) for the interactive project guide. Use these documents for implementation details:

- [Architecture](docs/ARCHITECTURE.md)
- [Domain and API](docs/DOMAIN-AND-API.md)
- [Implementation status](docs/IMPLEMENTATION-STATUS.md)
- [Customer storefront](docs/STOREFRONT.md)
- [Production deployment](docs/PRODUCTION-DEPLOYMENT.md)
- [Browser setup](docs/BROWSER-SETUP.md)
- [Writing style](docs/WRITING-STYLE.md)

## Local start

1. Copy `.env.example` to `.env`.
2. Set the required secrets in `.env`.
3. Run `docker compose up --build`.
4. Open the service addresses that `compose.yaml` defines.

Do not use development credentials in a public environment.

## Development checks

Use Node.js 24 for Angular and Storybook. Angular 22.2 requires Node.js 22.22.3 or later when you use Node.js 22.

Run these checks before you open a pull request:

```sh
npm run format:check
npm run build:admin
npm run build:storefront
npm run build:setup
npm run build-storybook
mvn -f apps/backend/pom.xml test
./scripts/verify-production-config.sh
```
