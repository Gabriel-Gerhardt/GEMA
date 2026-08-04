# Implementation plan: GAB-14 — Get and Put routes for sections

## Summary

GAB-13 already delivered the `sections` table and the `POST /api/q/{publicId}/sections`
route (create one section). GAB-14 adds the remaining two routes on the same
resource:

- `GET /api/q/{publicId}/sections` — list all sections belonging to a QR code.
- `PUT /api/q/{publicId}/sections` — replace the full set of sections belonging
  to a QR code with the ones supplied in the request (idempotent bulk replace).

Both routes return `404 Not Found` when the QR code does not exist, matching
the existing `POST` route and `QrcodeController#getQrcode` pattern.

## Impacted projects

- `backend` (Spring Boot / Java) only. No frontend work is described by the
  acceptance criteria, and no design/UI surface is affected, so the design
  agent is not invoked for this issue.

## Files to create/edit

- `backend/src/main/java/com/gema/external/repository/SectionRepository.java` —
  add a derived query method to fetch all sections for a QR code's public id,
  and a derived bulk-delete method for the same, following the existing
  `QrcodeRepository` style of derived query methods (`existsByPublicId`,
  `findByUser_Id`).
- `backend/src/main/java/com/gema/adapters/dto/response/SectionResponse.java`
  (new) — response record for a single section (`id`, `qrcodePublicId`,
  `title`, `content`, `createdAt`, `updatedAt`), reusing the same shape as the
  existing `SectionCreateResponse` so `GET` and `PUT` return the same
  representation a client already knows from `POST`'s `201` response.
- `backend/src/main/java/com/gema/adapters/dto/request/SectionListSaveRequest.java`
  (new) — request wrapper for `PUT`, carrying the list of `SectionSaveRequest`
  entries to persist (reuses the existing `SectionSaveRequest` record for each
  entry's `title`/`content` validation, so no duplicate validation rules are
  introduced).
- `backend/src/main/java/com/gema/core/service/SectionService.java` (edit) —
  add `getSections(publicId)` (look up the QR code, 404 if missing, return the
  list of sections mapped to `SectionResponse`) and `replaceSections(publicId,
  request)` (look up the QR code, 404 if missing, delete all existing sections
  for it, persist the new list, return the persisted sections mapped to
  `SectionResponse`).
- `backend/src/main/java/com/gema/external/rest/SectionController.java`
  (edit) — add `@GetMapping("/q/{publicId}/sections")` and
  `@PutMapping("/q/{publicId}/sections")` handlers delegating to the new
  service methods, following the existing controller's thin-delegation style.
- `backend/src/test/java/com/gema/service/SectionServiceTest.java` (edit) —
  add unit tests for `getSections` (happy path, QR code not found) and
  `replaceSections` (happy path replacing existing sections, empty-list
  clears all sections, QR code not found, idempotency — calling twice with
  the same payload yields the same resulting title/content set).
- `backend/src/test/java/com/gema/rest/SectionControllerTest.java` (edit) —
  add web-slice tests for both new routes: `200` happy path, `404` when the
  QR code does not exist, and validation `400`s on the `PUT` body reusing the
  same title/content constraints already tested for `POST`.
- `backend/src/test/java/com/gema/rest/SectionCreationAcceptanceTest.java`
  (edit) — extend the existing real-service-wiring acceptance test to cover
  the full journey: create QR code → create section → `GET` it back → `PUT`
  a replacement list → `GET` again to confirm the replacement took effect.

## Dependencies

None. No new libraries are required; everything follows patterns already
present in the codebase (Spring MVC, Spring Data JPA derived queries,
Bean Validation on request records).

## Execution order

1. `SectionRepository`: add `findByQrcode_PublicId` and
   `deleteByQrcode_PublicId`.
2. Add `SectionResponse` and `SectionListSaveRequest` DTOs.
3. `SectionService`: add `getSections` and `replaceSections`.
4. `SectionController`: add the `GET` and `PUT` handlers.
5. Unit tests: `SectionServiceTest`, `SectionControllerTest`.
6. Acceptance test: extend `SectionCreationAcceptanceTest` with the GET/PUT
   journey.

## Risks and challenges

- **Idempotency vs. section identity**: deleting and recreating rows on every
  `PUT` means a section's `id` is not stable across successive `PUT` calls,
  even when its `title`/`content` are unchanged. The acceptance criteria only
  require the *content* (title/content set) to be idempotent, not the row
  `id`s, but this is worth confirming (see Decisions below) since any future
  consumer that caches a section `id` would be affected.
- **Empty-list `PUT`**: "the request must replace all sections" implies an
  empty list is a valid way to clear all sections for a QR code, but the
  acceptance criteria don't say so explicitly (see Decisions below).
- **Ordering of `GET` results**: the acceptance criteria don't specify an
  order; defaulting to database/insertion order (see Decisions below).
- **Existing sections created via `POST` before a `PUT`**: a `PUT` must
  correctly replace sections regardless of whether they were created via the
  original `POST` route or a previous `PUT`, since both persist through the
  same `SectionRepository`.

## Decisions for user confirmation

1. **`PUT` request body shape.** Proposed: a JSON object wrapping the list,
   e.g. `{"sections": [{"title": "...", "content": "..."}, ...]}`, rather than
   a bare top-level JSON array. This follows the same "named object with
   `@Valid` fields" pattern used everywhere else in this codebase's request
   DTOs (`SectionSaveRequest`, `QrcodeSaveRequest`, `UserSaveRequest` are all
   objects, never bare arrays), and it lets Bean Validation cascade into each
   list entry via `@Valid @NotEmpty List<SectionSaveRequest> sections`. A bare
   array is equally valid REST and was considered instead. This is not
   dictated by the acceptance criteria either way.
2. **Section identity across `PUT` (delete-and-recreate).** Proposed: on
   `PUT`, delete all existing sections for the QR code and insert new rows
   built from the request list, so section `id`s are not preserved/stable
   across `PUT` calls. The acceptance criteria list only `title` and `content`
   as updatable fields and never mention `id` in the `PUT` request, so there
   is no id-matching signal to upsert against — an alternative (matching by
   position or by an id the client would have to send) would add complexity
   the acceptance criteria don't ask for. Flagging because this is an
   architectural choice with an observable side effect (ids churn on every
   `PUT`), not because a reasonable alternative reading is expected.
3. **Empty-list `PUT` semantics.** Proposed: an empty `sections` list is
   accepted and clears all sections for the QR code (fully consistent with
   "replace all sections linked to the informed QR Code"). The alternative is
   rejecting an empty list with `400`. Not dictated explicitly by the AC.
4. **`GET` response ordering.** Proposed: return sections in ascending `id`
   order (i.e., creation order), the simplest/default behavior of the
   underlying query. No ordering is specified by the acceptance criteria.

## Open questions

None beyond the four decisions above — each decision doubles as the open
design question for that point; there is no additional ambiguity in scope,
routes, or status codes, which the acceptance criteria already specify
precisely (`200`/`404`, `title`/`content` fields, idempotent replace).
