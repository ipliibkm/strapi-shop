# Booking API

The storefront uses four anonymous endpoints. The administrator endpoints remain protected.

## Customer operations

Read the customer booking configuration:

```http
GET /api/v1/public/booking-settings
```

`customerBookingEnabled` is the effective public state. It is `true` only when the administrator enables booking and each mapped calendar provider is ready.

The response includes `unavailableReason` when booking is inactive. The API rejects availability and booking requests in this state.

`defaultDurationMinutes` applies when customer booking is enabled and no active services exist.

List the active services that customers can select:

```http
GET /api/v1/public/services
```

Each service contains an `id`, `name`, and `durationMinutes` value.

Check availability for one service and start time:

```http
GET /api/v1/public/availability?serviceId=2ae3cfbf-e851-4570-bfc6-90523ab983f4&startsAt=2026-12-04T09:00:00Z
```

The backend calculates `endsAt` from the current service duration. If no active services exist, omit `serviceId`. The backend then uses `defaultDurationMinutes`.

Create a confirmed booking:

```http
POST /api/v1/public/bookings
Content-Type: application/json

{
  "serviceId": "2ae3cfbf-e851-4570-bfc6-90523ab983f4",
  "startsAt": "2026-12-04T09:00:00Z",
  "customerName": "Alex Example",
  "customerEmail": "alex@example.com"
}
```

The backend stores the selected service name and duration with the booking. Later service changes do not change historical bookings. A fallback booking stores `Appointment` as its service name.

The backend assigns the resource from `PLATFORM_BOOKING_PUBLIC_RESOURCE_ID`. The default value is `default`. The public response includes the service, duration, start time, and end time. It does not include contact details or calendar delivery data.

## Administrator operations

- `GET /api/v1/admin/bookings/calendar?from=<instant>&to=<instant>` returns all bookings in a calendar range.
- `GET /api/v1/admin/bookings?resourceId=<id>&from=<instant>&to=<instant>` filters one resource.
- `POST /api/v1/admin/bookings` creates an administrator booking.
- `PUT /api/v1/admin/bookings/{id}` reschedules a booking.
- `DELETE /api/v1/admin/bookings/{id}` cancels a booking.
- `GET /api/v1/admin/booking-services` returns active and hidden services.
- `POST /api/v1/admin/booking-services` creates a service.
- `PUT /api/v1/admin/booking-services/{id}` changes a service.
- `DELETE /api/v1/admin/booking-services/{id}` hides a service from new customer bookings.
- `GET /api/v1/admin/booking-services/settings` returns the customer booking configuration.
- `PUT /api/v1/admin/booking-services/settings` changes customer booking availability and the fallback duration.

Service durations must be from 5 through 480 minutes in 5-minute steps.

## Conflicts and synchronization

A conflict returns HTTP `409` with code `AVAILABILITY_CONFLICT`. The client must ask the customer to select another time.

PostgreSQL advisory transaction locks serialize booking attempts for one resource. Availability uses external busy blocks already synchronized into PostgreSQL.

The backend permits public booking only when all these conditions are true:

- At least one writable calendar mapping exists for the public resource.
- The mapping status is `SYNCED` and has no reported conflicts.
- The last successful synchronization is not older than `PLATFORM_BOOKING_MAXIMUM_CALENDAR_SYNC_AGE_SECONDS`.
- Authorization succeeds for each mapped provider. An expired OAuth access token is refreshed when its refresh token is valid.

The default maximum synchronization age is 300 seconds. If authorization requires a new login, the public booking API returns HTTP `503` with code `BOOKING_UNAVAILABLE`.

Before public internet deployment, add gateway rate limits, bot protection, and retention rules for customer data.
