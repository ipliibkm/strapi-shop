# Instructions for developers and coding agents

Read [docs/WRITING-STYLE.md](docs/WRITING-STYLE.md) before you write text.

Apply ASD-STE100-style English to:
- product UI labels and messages
- administrator screens and setup steps
- CLI text and operational documentation
- API descriptions and human-readable errors
- issue descriptions and acceptance criteria
- comments that explain a procedure

Use correct programming terms and exact identifiers. Do not change code syntax to follow writing rules.

Security rules take priority. Do not hide a warning to make a sentence short.
Do not claim formal ASD-STE100 compliance without a documented compliance review.

## Code structure (mandatory)

These rules are owner preferences. Apply them on every change, not only during refactors.

### Folder layout

- One folder per page or feature component under `apps/*/src/` (for example `booking/`, `shop/`).
- Put a file in `shared/` only when two or more features or the app shell use it. A file used by one page stays in that page's folder.
- Do not place components, services, or data files loose in `src/`.

### App entry point (`main.ts`)

- `main.ts` is a pure shell and router: header, navigation, routing switch, footer.
- No page markup and no page state in `main.ts`. Every page is its own component in its own folder.
- Pages receive CMS content and data as inputs; they emit navigation and product events as outputs. The shell owns content loading and preview mode.

### Templates and styles

- Never write HTML inside a `.ts` file. Use `templateUrl: './<name>.component.html'`.
- Never write CSS inside a `.ts` file. Use `styleUrls: ['./<name>.component.css']`.
- Exception: Storybook story wrapper strings in `*.stories.ts` may stay inline.

### Deduplication and separation of concerns

- Extract shared markup, data arrays, and logic into one place before a second copy appears.
- `packages/ui` components are presentational and i18n-free: translated text goes in via inputs, events go out via outputs.
- A page component must not reach into another page's state; communicate through the shell or a shared service.

### Decisions

- Make each structural decision deliberately and be able to explain it. "It depends on shell state" is not a reason to keep code inline in `main.ts`.
- When moving code, move its state with it. Do not leave orphaned signals or helpers behind.

### Definition of done for frontend changes

- `ng build` passes with zero errors and zero warnings for every touched app.
- Prettier applied to all touched files (TS, HTML, CSS).
- Rebuild only the changed Docker services, restart them, and verify HTTP 200.
- Smoke-test the affected pages in a real browser before reporting done.
