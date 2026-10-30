# Extension architecture

## Purpose

The platform is a modular Spring Boot monolith. Add external services as adapters. Do not add one microservice for each provider.

The architecture separates capabilities from vendors:

- Calendar is a capability.
- Mail delivery is a capability.
- Google Calendar and Microsoft Graph Calendar are calendar adapters.
- Gmail, Microsoft Graph Mail, SMTP, and transactional mail APIs are mail adapters.
- OAuth is one credential mechanism. SMTP credentials and API keys are other mechanisms.

## Current runtime

```text
Angular storefront/admin
        |
        v
Spring Boot API and booking application
        |                     |
        v                     v
PostgreSQL               Strapi adapter
        |
        v
Calendar provider adapters -> Google Calendar
```

PostgreSQL is authoritative for bookings. External calendar events are busy-time projections. They cannot create confirmed customer bookings.

## Dependency rule

Dependencies point from adapters to application ports:

```text
booking domain
  -> BookingCalendarCoordinator
       -> BookingCalendarProvider port
            <- GoogleBookingEventService adapter
            <- future MicrosoftBookingEventService adapter

public booking policy
  -> PublicBookingReadinessService
       -> CalendarAvailabilityProvider port
            <- GoogleCalendarAvailabilityProvider adapter
            <- future MicrosoftCalendarAvailabilityProvider adapter
```

The booking package must not import provider, OAuth, or remote API classes. Provider adapters can import booking ports.

## Calendar extension contract

`BookingCalendarProvider` publishes booking changes and returns delivery state. `BookingCalendarCoordinator` sends each change to all configured providers and combines their delivery states.

`CalendarAvailabilityProvider` reports readiness only when it has a mapping for the requested resource. Public booking is available when at least one provider has a mapping and every mapped provider is ready.

The shared `external_busy_block` projection uses these provider-neutral identifiers:

- `provider`
- `external_calendar_id`
- `external_event_id`
- `resource_id`
- `starts_at`
- `ends_at`

Each calendar adapter must use the same PostgreSQL advisory lock as `BookingService` before it changes busy blocks. This lock prevents a synchronized event and a booking from passing the overlap check concurrently.

### Add Microsoft Outlook Calendar

Use Microsoft Graph Calendar as a new adapter:

1. Add a separate OAuth client registration such as `microsoft-calendar`.
2. Request only the Calendar scopes that the adapter needs.
3. Store tokens through `EncryptedJdbcOAuth2AuthorizedClientService`.
4. Add Microsoft connection and resource-mapping persistence through a new Liquibase change set.
5. Implement `BookingCalendarProvider` for booking event publication.
6. Implement `CalendarAvailabilityProvider` for synchronization health.
7. Import Graph delta changes into `external_busy_block` with provider `microsoft`.
8. Add provider-specific administration endpoints and a connection card.
9. Run the shared provider contract tests and booking concurrency tests.

Do not add Microsoft branches to `BookingService` or `PublicBookingReadinessService`.

## Mail extension contract

Mail is independent from Calendar. The current Google connection requests Calendar and `gmail.send` scopes in one consent flow. A future provider-neutral design can split these grants.

The current implementation sends mail directly after the business transaction commits. The target durable flow is:

```text
booking transaction
  -> notification outbox row
  -> delivery worker
  -> MailTransport port
       <- SmtpMailTransport
       <- GmailMailTransport
       <- MicrosoftGraphMailTransport
       <- transactional API adapter
```

The booking transaction writes a logical notification to PostgreSQL. It does not call a mail server. A worker claims outbox rows, calls one transport, and records the result.

A future `MailTransport` contract must include:

- a stable provider identifier;
- one idempotency key;
- sender and recipient addresses;
- subject and text or HTML content;
- a provider message identifier in the result;
- normalized temporary and permanent failure categories.

Use separate connections and grants for these capabilities:

- `google-calendar`
- `google-mail`
- `microsoft-calendar`
- `microsoft-mail`
- `smtp`

SMTP and API-key transports keep secrets in backend secret configuration. APIs return only safe connection state.

## Provider rules

Every external-service adapter must follow these rules:

- Keep credentials and refresh tokens out of browser storage and API responses.
- Encrypt persisted OAuth tokens.
- Use additive Liquibase migrations.
- Put remote calls outside booking database transactions.
- Use a transactional outbox for side effects.
- Make delivery idempotent.
- Retry temporary failures with a limit and backoff.
- Store safe error categories instead of raw sensitive responses.
- Support disconnect and credential rotation.
- Expose health without exposing account secrets.
- Keep provider labels and setup controls outside domain services.

## Compliance review

| Rule                                               | State         | Evidence                                                                                    |
| -------------------------------------------------- | ------------- | ------------------------------------------------------------------------------------------- |
| Modular monolith, not provider microservices       | Compliant     | One Spring Boot backend and provider adapters                                               |
| Booking does not depend on Google classes          | Compliant     | `BookingCalendarProvider`, `BookingCalendarCoordinator`, and `CalendarAvailabilityProvider` |
| Multiple calendar providers can publish changes    | Compliant     | Coordinator uses all `BookingCalendarProvider` beans                                        |
| Multiple mapped providers affect readiness         | Compliant     | `PublicBookingReadinessService` combines all provider results                               |
| Shared busy projection is provider-neutral         | Compliant     | Liquibase change set `011-provider-neutral-busy-block`                                      |
| Booking API status is provider-neutral             | Compliant     | `calendarProvider`, `calendarSyncStatus`, and `calendarSyncError`                           |
| OAuth token storage supports several registrations | Compliant     | Token key contains registration ID and principal name                                       |
| Calendar and mail consent are separate             | Not compliant | Current Google registration requests Calendar and Gmail send scopes together                |
| Mail delivery                                      | Partial       | SMTP and Gmail sends exist, but no durable notification outbox exists                       |
| Provider-neutral connection and mapping API        | Partial       | Current connection and mapping endpoints are Google-specific                                |
| Multi-instance worker claims                       | Not compliant | Current workers use JVM-local overlap guards                                                |

## Required work before a second production provider

Complete these items before Microsoft Calendar or a mail provider goes live:

1. Add a provider-neutral integration catalog endpoint for the admin UI.
2. Move provider-specific OAuth success handling out of `StrapiAdminSecurityConfiguration`.
3. Add a generic connection identity with provider and capability fields.
4. Add database claims with `FOR UPDATE SKIP LOCKED` or leases for delivery workers.
5. Add PostgreSQL integration tests for booking and busy-block races.
6. Add contract tests that every calendar or mail adapter must pass.

These items do not block the current single Google connection. They are extension gates, not reasons to split the application into microservices.
