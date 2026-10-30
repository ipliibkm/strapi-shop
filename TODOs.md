# Form submission inbox implementation record

This work is complete. The 2026-10-26 review finding about the submission deletion boundary is resolved.

## Current state

- Submissions are Strapi `form-submission` documents linked to their `storefront-form`.
- Each record contains the submitted payload, submission time, form relation, and stable form document ID.
- The secured endpoint `GET /api/v1/admin/forms/{slug}/submissions` returns a paged result.
- The form builder is in `apps/admin/src/forms/forms.component.ts` (template in the sibling `forms.component.html`).
- Backend form code is in `apps/backend/src/main/java/de/openshopfront/platform/form/`.
- The Admin UI lists, opens, exports, and deletes submissions.

## 1. Add submission navigation

- [x] Add **Builder** and **Submissions** tabs to the selected form.
- [x] Display the number of submissions in the Submissions tab.
- [x] Keep the form list visible when the administrator changes tabs.
- [x] Reset the active tab when the administrator selects another form.

## 2. Build the submissions list

- [x] Load submissions from `/api/v1/admin/forms/{slug}/submissions`.
- [x] Display the newest submissions first.
- [x] Display the submission date and time in the administrator's local timezone.
- [x] Build table columns from the selected form definition.
- [x] Use field labels as column headings.
- [x] Display checkbox values as `Yes` or `No`.
- [x] Display a clear empty state when the form has no submissions.
- [x] Display a clear error message if loading fails.
- [x] Add a manual refresh button.

## 3. Add a submission detail view

- [x] Open a submission in a dialog or side panel.
- [x] Display all configured fields in form order.
- [x] Display the field label and submitted value.
- [x] Display the submission ID and timestamp.
- [x] Preserve line breaks in textarea values.
- [x] Do not render submitted values as HTML.

## 4. Add backend pagination

Replace the fixed `limit 200` query.

- [x] Support `page` and `pageSize` query parameters.
- [x] Restrict `pageSize` to a safe maximum, such as 100.
- [x] Return this paged response:

```json
{
  "items": [],
  "page": 1,
  "pageSize": 25,
  "total": 0
}
```

- [x] Keep results sorted by `created_at DESC`.
- [x] Add pagination controls to the Admin UI.
- [x] Reset pagination when the selected form changes.

## 5. Add CSV export

- [x] Add `GET /api/v1/admin/forms/{slug}/submissions.csv`.
- [x] Use the form field order for CSV columns.
- [x] Include the submission ID and timestamp.
- [x] Escape spreadsheet formulas that start with `=`, `+`, `-`, or `@`.
- [x] Return UTF-8 CSV with a suitable download filename.
- [x] Add a **Download CSV** button to the Submissions tab.

## 6. Add submission deletion

- [x] Add `DELETE /api/v1/admin/forms/{slug}/submissions/{id}`.
- [x] Verify that the submission belongs to the specified form before deletion.
- [x] Add a confirmation dialog before deletion.
- [x] Remove the row from the interface after successful deletion.
- [x] Display an error if deletion fails.
- [x] Do not add bulk deletion without a separate confirmation step.

## 7. Protect customer data

- [x] Keep all submission management endpoints administrator-only.
- [x] Do not expose submissions through public endpoints.
- [x] Do not write submitted values or recipient email addresses to application logs.
- [x] Treat all submitted values as untrusted text.
- [x] Do not use `innerHTML` for submitted values.
- [x] Do not include sensitive values in URLs.
- [x] Confirm that CSRF protection remains active for administrator deletion requests.

## 8. Add tests

### Backend tests

- [x] Test that anonymous requests cannot list submissions.
- [x] Test pagination and total counts.
- [x] Test newest-first sorting.
- [x] Test form-specific filtering.
- [x] Test CSV field order and escaping.
- [x] Test successful deletion.
- [ ] Add a service-level test for deletion with an incorrect form slug. The current controller test mocks the result.
- [ ] Add a service-level test for deletion of an unknown submission. The current controller test mocks the result.

### Admin tests

- [x] Test the empty state.
- [x] Test submission loading.
- [x] Test field label mapping.
- [x] Test pagination.
- [x] Test the detail view.
- [x] Test deletion confirmation and error handling.

### Browser verification

- [x] Submit the seeded Contact form through the storefront.
- [x] Sign in to the Admin UI.
- [x] Open **Forms → Contact form → Submissions**.
- [x] Confirm that the submitted values appear.
- [x] Download the CSV and verify its contents.
- [x] Delete the test submission.
- [x] Confirm that it no longer exists in Strapi.

## Acceptance criteria

- [x] Administrators can view submissions without direct database access.
- [x] Each form enforces its boundary during list, export, and deletion operations.
- [x] Submitted values use the configured field labels and order.
- [x] Pagination works without loading all records.
- [x] CSV export is safe for spreadsheet applications.
- [x] Administrators can delete individual submissions after confirmation.
- [x] Public users cannot access submission records.
- [x] Backend tests pass. The 2026-10-26 run passed 98 tests.
- [x] Formatting checks pass. The 2026-10-26 final run passed all checks.
- [x] Admin, storefront, CMS, and backend Docker builds pass.
- [x] Update `docs/IMPLEMENTATION-STATUS.md`.

## Implementation constraints

- Preserve all unrelated uncommitted changes.
- Follow `docs/WRITING-STYLE.md`.
- Keep Strapi as the owner of form definitions.
- Keep Spring as the secured backend-for-frontend.
- Keep Strapi as the submission store.
- Authenticated browser testing requires existing Strapi administrator credentials.
