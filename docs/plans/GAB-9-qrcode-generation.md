# GAB-9 — Qrcode generation: implementation plan

## Summary

GAB-9 ("Qrcode generation") already has its core create/resolve flow in `main` from
GAB-12 (`POST /api/qrcodes`, `GET /api/q/{publicId}`, `QrcodeService`, `QrcodeEntity`,
`QrcodeRepository`). What is missing is everything the GAB-9 acceptance criteria adds on
top of that base:

- The JSON response contract must expose `title`, `content`, `isActive` (currently the
  `QrcodeResponse` record exposes `description` instead of `content`; `isActive` is
  already correctly named and is the application's established naming convention — see
  "Resolved decisions" below).
- Stored QR content must be validated: not empty, valid UTF-8 printable characters only,
  control characters rejected except `\n` and `\t`.
- A scannable QR barcode PNG image, generated from the `/q/{public_id}` URL.
- Unit tests for the sanitization rules, for the entity → response DTO mapping, and for
  deterministic output on repeated calls with the same input.
- The AC also calls for an integration test of the full create → resolve flow against a
  real database. That is **descoped for this pass** — see "Resolved decisions" below.

This PR was implemented once before (GEMA PR #12, later reopened as #16) but both were
reverted/closed before merging cleanly on top of the current `main`, which now includes
the GAB-15 JWT/security refactor. This plan re-implements the same behavior from scratch
against current `main`, reusing the exception/response patterns already established
(`BadRequestException`, `NotFoundException`, `RestException`, `ApiResponse`,
`GlobalExceptionHandler`).

## Resolved decisions

Two open questions were raised during planning and answered by the requester before
coding began:

1. **QR barcode/PNG image generation is in scope.** The `/qrcodes/{publicId}/image`
   endpoint producing a scannable PNG (as the earlier reverted PR did, via zxing) is
   part of this change, not a follow-up.
2. **Field naming:** `isActive` is the application's standard naming convention and
   stays as `isActive` (not renamed to `active`, despite the AC prose using lowercase
   "active"). `description` is renamed to `content` for consistency — on **both** the
   response (`QrcodeResponse`) and the request (`QrcodeSaveRequest`), since the
   requester confirmed "content is right" for both sides rather than leaving a
   write/read naming asymmetry.

A third decision was made mid-implementation, once a concrete environment limitation was
hit (not a pre-implementation open question, so handled directly rather than re-raised as
one):

3. **The Testcontainers-based integration test is descoped.** Pulling `postgres:16`
   from Docker Hub is blocked by this environment's egress policy (confirmed via the
   agent proxy: a direct 403 from Docker Hub's CDN, and per the proxy's own guidance,
   policy denials are to be reported, not routed around). A Maven-Central-only
   alternative (`io.zonky.test:embedded-postgres`, no Docker required) was considered,
   but the requester asked to skip the integration test and its new test dependency
   entirely rather than substitute it. **Net effect: no integration test is added in
   this change; the AC's integration-test criteria are not met.** The unit and
   controller-slice (`@WebMvcTest`, mocked service) tests below remain in scope and do
   verify the sanitizer, the mapping, the image encode/decode round-trip, and the HTTP
   contract (status codes, JSON shape, PNG content type) — just against mocks/pure
   functions rather than a real database end-to-end.

## Impacted projects

- `backend` only (Spring Boot / Java). No frontend or design changes are required —
  the image endpoint is a new backend route only; nothing in the frontend consumes it
  yet. The design agent is not invoked for this issue.

## Files to create/edit

- **Edit** `backend/src/main/java/com/gema/adapters/dto/response/QrcodeResponse.java` —
  rename the `description` record component to `content`. Keep `isActive` as-is.
- **Edit** `backend/src/main/java/com/gema/adapters/dto/request/QrcodeSaveRequest.java`
  — rename the `description` field to `content` (still `@NotBlank`).
- **Create** `backend/src/main/java/com/gema/adapters/dto/response/QrcodeCreateResponse.java`
  — no change needed (already just wraps `publicId`); listed here only to confirm no
  edit is required.
- **Create** `backend/src/main/java/com/gema/core/service/QrcodeContentSanitizer.java` —
  a small stateless validator invoked before persisting QR content. Rejects null/blank
  content and any character that is an ISO control character other than `\n`/`\t`
  (covering the "reject invalid characters" and "must not be empty" criteria; Java
  strings are already UTF-16/UTF-8-safe, so no separate encoding check is needed beyond
  the control-character sweep).
- **Edit** `backend/src/main/java/com/gema/core/service/QrcodeService.java` — rename the
  `request.description()` call site to `request.content()`; call the sanitizer at the
  start of `createQrcode` before touching the repository, so invalid content never
  reaches the database (needed for the unit tests that assert `userRepository`/
  `qrcodeRepository` are never touched on validation failure). Extract the entity →
  `QrcodeResponse` mapping into its own method (e.g. `toResponse`) so it can be
  unit-tested directly for the "mapping from persistence model to response DTO"
  criterion.
- **Create** `backend/src/main/java/com/gema/core/service/QrcodeImageService.java` — a
  `@Service` that takes an arbitrary string (the full `/q/{publicId}` URL) and returns
  PNG bytes of the QR barcode encoding it, using `com.google.zxing` (`QRCodeWriter` +
  `MatrixToImageWriter`), same approach as the earlier reverted PR.
- **Edit** `backend/src/main/java/com/gema/external/rest/QrcodeController.java` — add
  `GET /api/qrcodes/{publicId}/image` producing `image/png`. It resolves the qrcode by
  `publicId` first (reusing `QrcodeService.getQrcodeByPublicId`, so unknown ids 404 the
  same way the JSON endpoint does), builds the target URL from a configured base URL +
  `/q/{publicId}`, and returns the PNG bytes from `QrcodeImageService`.
- **Edit** `backend/src/main/resources/application.yaml` — add `app.base-url`
  (`${APP_BASE_URL:http://localhost:8080}`) so the controller can build an absolute,
  scannable URL without hardcoding a host.
- **Edit** `backend/build.gradle` — add `com.google.zxing:core:3.5.3` and
  `com.google.zxing:javase:3.5.3` (`implementation`, production code needs them to
  generate the PNG). No test-scoped database dependency is added — see "Resolved
  decisions" #3.
- **Edit** `backend/src/test/java/com/gema/service/QrcodeServiceTest.java` — update the
  existing assertions/request constructions from `description(...)`/`.description()` to
  `content(...)`/`.content()`; add cases for blank/invalid content being rejected before
  repository access, and for the extracted `toResponse` mapping (including a
  determinism assertion: same entity in, equal response out across two calls).
- **Create** `backend/src/test/java/com/gema/service/QrcodeContentSanitizerTest.java` —
  unit tests for the sanitizer: null, empty, blank, control characters (NUL, DEL, form
  feed, unit separator, `\r`), allowed `\n`/`\t`, normal UTF-8 content (accents, CJK,
  emoji/surrogate pairs), whitespace-only strings containing only `\n`/`\t` (still
  blank, still rejected).
- **Create** `backend/src/test/java/com/gema/service/QrcodeImageServiceTest.java` —
  generate a PNG for a URL, decode it back with zxing's `MultiFormatReader` and assert
  it round-trips to the original URL; assert the PNG file-signature bytes; assert
  determinism (same input twice → both decode to the same value); assert emoji/
  multiline content round-trips correctly.
- **Edit** `backend/src/test/java/com/gema/rest/QrcodeControllerTest.java` — update JSON
  body/path references from `description`/`$.description` to `content`/`$.content`
  (leave `isActive` assertions unchanged); add `@TestPropertySource(properties =
  "app.base-url=http://localhost:8080")` and a `@MockBean QrcodeImageService`; add test
  cases for the new `GET /api/qrcodes/{publicId}/image` route: happy path returns 200 +
  `image/png` content type + expected bytes, unknown `publicId` returns 404, and calling
  twice returns identical bytes (determinism at the controller layer).
- ~~`QrcodeFlowIntegrationTest`~~ — not created. See "Resolved decisions" #3.

## Dependencies

- `com.google.zxing:core:3.5.3` (production)
- `com.google.zxing:javase:3.5.3` (production — provides `MatrixToImageWriter`)

## Execution order

1. Rename `QrcodeResponse.description` → `content` and `QrcodeSaveRequest.description` →
   `content`; fix the `QrcodeService` call sites that reference the old names.
2. Update `QrcodeServiceTest` and `QrcodeControllerTest` to the renamed fields (keeps
   the suite green before adding new behavior).
3. Add `QrcodeContentSanitizer` + its unit test; wire it into
   `QrcodeService.createQrcode`; extract `toResponse`; add the new `QrcodeServiceTest`
   cases (validation-rejects-before-repo-access, mapping, determinism).
4. Add the zxing dependency; add `QrcodeImageService` + its unit test.
5. Add `app.base-url` to `application.yaml`; add the `/qrcodes/{publicId}/image`
   controller route + its `QrcodeControllerTest` cases.
6. Add the Testcontainers dependencies to `build.gradle`; add
   `QrcodeFlowIntegrationTest`.
7. Run the full `./gradlew test` suite (requires Docker for Testcontainers) and fix any
   failures before handing off to review.

## Risks and challenges

- **Testcontainers requires Docker.** Confirmed available in this environment
  (`docker info` succeeds). If a future CI runner lacks Docker, this test would need a
  different strategy — flagged for the review/test agents to double check the CI
  environment can run it.
- **Renaming `description` → `content` on both DTOs is a breaking API contract
  change** for any existing consumer. Checked: the frontend has no wiring to these
  routes yet (GAB-28 "integrate frontend with routes" only touches auth/landing pages
  so far), and no other backend code references the old names outside the two test
  files listed above. Confirmed with the requester as an intentional change, not an
  accidental break.
- **`isActive` intentionally kept**, not renamed to `active` — a deliberate deviation
  from the AC's literal prose, confirmed with the requester as the actual intended
  contract (AC wording is descriptive, not a literal field-name mandate).
- **zxing image size/format aren't specified anywhere** (no AC criterion covers pixel
  dimensions or format beyond "PNG" being implied by the endpoint's media type). Follows
  the earlier reverted PR's precedent: 300x300 PNG. Flagged for review in case a
  different size is expected, but not blocking — no evidence any consumer depends on a
  specific size yet.

## Open questions

None outstanding — both questions raised during planning were answered by the requester
(see "Resolved decisions" above) before this plan was finalized.
