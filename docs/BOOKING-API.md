# Booking API — implemented skeleton, not production availability
The current Spring backend has `GET /api/v1/availability?resourceId=room-1&startsAt=<ISO-UTC>&endsAt=<ISO-UTC>`, `GET /api/v1/admin/bookings?resourceId=...&from=...&to=...`, and `POST /api/v1/admin/bookings` with `{"resourceId":"room-1","startsAt":"2026-12-01T10:00:00Z","endsAt":"2026-12-01T11:00:00Z"}`. Responses are UTC instants.

Outside Spring profile `dev` all application endpoints remain denied. Profile `dev` **must not** be used on exposed production systems: locally enable it with `SPRING_PROFILES_ACTIVE=dev` and a strong `DEV_ADMIN_PASSWORD`, and use local HTTP Basic for admin requests. Browser admin integration awaits OIDC. Spring application data uses Liquibase.

Conflicts return `HTTP 409`, `{"code":"AVAILABILITY_CONFLICT","message":"...","correlationId":"..."}`. Clients must notify users and refresh availability. PostgreSQL advisory transaction locks serialize attempts to book the same resource, and the external busy-block writer uses the same lock. **This only checks Google busy-blocks already synchronized into PostgreSQL**; it DOES NOT guarantee up-to-the-second Google correctness. A future sync freshness gate must fail closed when provider data is stale.

Missing: Google OAuth/sync, booking idempotency, group capacity, slot generation, cancellation, deposits, resource configuration, authentication/authorization, reliability tests with real PostgreSQL and concurrency, business open-hours, full reservation APIs. No real money or confirmed customer bookings should run on this scaffold.
