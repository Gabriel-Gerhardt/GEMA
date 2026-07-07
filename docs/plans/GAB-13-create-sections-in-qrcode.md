# Implementation Plan: GAB-13 — Create sections in QR code

## Summary
Add a `sections` resource, owned by a `qrcode`, so a QR code's content can be
organized into titled blocks. This redoes the feature that was previously
merged (PR #10), reverted in a batch alongside GAB-9/GAB-11 ahead of the
GAB-15 JWT-authentication refactor, redone in PR #14, and closed without
merge by the requester with the comment "Unsafe code". PR #14 was never
updated afterward, so the concrete safety gap it referred to must be
inferred (see "Resolved decisions" below) rather than assumed fixed.

Only the **create** route is in scope, per GAB-13's acceptance criteria.
GET/PUT routes for sections are tracked separately in GAB-14, which is
blocked by this issue.

## Impacted projects
- `backend` only (Spring Boot / Java 21 / Gradle). No frontend or `docs`
  changes beyond this plan and the coordinator log.

## Context gathered from the codebase
- `QrcodeController` (`backend/src/main/java/com/gema/external/rest/QrcodeController.java`)
  is the pattern to follow for a new `SectionController`: thin controller,
  `@RequestBody @Valid` request DTO, service returns a response DTO,
  `ResponseEntity.status(...)`.
- `QrcodeService` shows the pattern for looking up a parent entity and
  throwing `NotFoundException`/`BadRequestException` (from
  `com.gema.external.exception`, both extend `RestException`, handled
  generically by `GlobalExceptionHandler`).
- `QrcodeEntity`/`QrcodeRepository` show the Lombok `@Data` +
  `@AllArgsConstructor` + `@NoArgsConstructor` JPA entity pattern, and
  Liquibase migrations live at
  `backend/src/main/resources/db/changelog/migrations/NNN-*.sql`, wired into
  `db.changelog.yml`. Only `001-initial-schema.sql` exists today (creates
  `users` and `qrcodes`), so the new file is `002-create-sections-table.sql`.
- `BeanConfig` currently has `authorizeHttpRequests(auth -> auth.anyRequest().permitAll())`
  — i.e. **no route in the application enforces authentication today**, even
  though GAB-15 added JWT issuance (`JwtService`, login route). This was
  independently confirmed while redoing the sibling issues from the same
  revert batch (GAB-9, GAB-11 — see PRs #19 and #18): neither of those redo
  PRs added endpoint-level auth either, since there is no established
  authorization pattern yet to be consistent with. Enforcing auth on this one
  route only would be an isolated, inconsistent change, not a targeted fix —
  it is treated as a separate, cross-cutting concern out of scope for GAB-13
  (see "Resolved decisions" #1 below).
- Existing tests (`QrcodeControllerTest`, `QrcodeServiceTest`) use
  `@WebMvcTest` + `@MockBean` for the controller layer and plain
  Mockito/AssertJ for the service layer — the new `SectionControllerTest`/
  `SectionServiceTest` follow the same structure.
- Build: Gradle (`backend/build.gradle`), Java 21 toolchain. System Gradle is
  used to run tests (the Gradle wrapper's distribution download is blocked by
  this sandbox's egress policy, same as noted in the GAB-9 coordinator log).

## Resolved decisions
Both were judgment calls the code persona is authorized to make per its own
role definition ("Not lazy about: input validation at trust boundaries");
neither is a business/product decision, so per `planning.md`'s directive to
route only genuine open questions back to the user, they are recorded here
as decisions rather than raised as blocking questions:

1. **Authentication/authorization is out of scope for this change.** The
   original PR #10/#14 diff contained no auth check on the create route, but
   nothing in the current codebase enforces auth anywhere yet (see above), so
   singling out this one route would be inconsistent rather than a genuine
   fix. If the real reason for "Unsafe code" was missing auth, the correct
   fix is a cross-cutting `BeanConfig`/`JwtAuthFilter` change across every
   route, which belongs to a security-hardening ticket, not GAB-13.
2. **Concrete interpretation of "Unsafe code":** the previous
   `SectionSaveRequest` validated `title`/`content` with `@NotBlank` only.
   `title` is persisted to a `VARCHAR(255)` column with no request-side
   `@Size` bound — a title over 255 chars would hit the database and surface
   as an unhandled `DataIntegrityViolationException` (a 500 with no
   `RestException` mapping, i.e. unvalidated input reaching a trust
   boundary — this is exactly the category `review.md` calls out:
   "unvalidated input"). This plan closes that gap: `title` gets
   `@Size(max = 255)` to match the column, and `content` gets a bounded
   `@Size(max = 20000)` so the `TEXT` column can't be used to store
   unbounded payloads from a route with no other size control. This is the
   most concrete, addressable safety issue actually present in the diff, and
   is called out explicitly in this plan and in the PR description so the
   requester can confirm or correct the interpretation.

## Files to create/edit

- `backend/src/main/resources/db/changelog/migrations/002-create-sections-table.sql` (new)
  Liquibase changeset creating `sections`: `id`, `qrcode_id` (FK to
  `qrcodes.id`, `ON DELETE CASCADE`, `NOT NULL`), `title`, `content` (`TEXT`),
  `created_at`, `updated_at`. Index on `qrcode_id` (mirrors the existing
  `idx_qrcodes_user_id` pattern).
- `backend/src/main/resources/db/changelog/db.changelog.yml` (edit)
  Add an `include` entry for the new changeset file.
- `backend/src/main/java/com/gema/external/entity/SectionEntity.java` (new)
  JPA entity for `sections`, same Lombok annotations as `QrcodeEntity`,
  `@ManyToOne` to `QrcodeEntity` via `qrcode_id`.
- `backend/src/main/java/com/gema/external/repository/SectionRepository.java` (new)
  `JpaRepository<SectionEntity, Long>`. No extra derived-query methods beyond
  what's needed for GAB-13 (`findByQrcodeId` is GAB-14's concern, not added
  here — YAGNI).
- `backend/src/main/java/com/gema/adapters/dto/request/SectionSaveRequest.java` (new)
  Record with `title` (`@NotBlank @Size(max = 255)`) and `content`
  (`@NotBlank @Size(max = 20000)`).
- `backend/src/main/java/com/gema/adapters/dto/response/SectionCreateResponse.java` (new)
  Record: `id`, `qrcodePublicId`, `title`, `content`, `createdAt`,
  `updatedAt` — mirrors `QrcodeCreateResponse`/`QrcodeResponse` shape
  conventions.
- `backend/src/main/java/com/gema/core/service/SectionService.java` (new)
  `createSection(String qrcodePublicId, SectionSaveRequest request)`: looks
  up the `QrcodeEntity` via `QrcodeRepository.findByPublicId`, throws
  `NotFoundException` if absent (→ 404 per AC), builds and saves the
  `SectionEntity`, maps to `SectionCreateResponse`.
- `backend/src/main/java/com/gema/external/rest/SectionController.java` (new)
  `POST /api/q/{publicId}/sections` → `201 Created` with
  `SectionCreateResponse` body, matching the existing `/api/q/{publicId}`
  path convention for QR-code-scoped routes.
- `backend/src/test/java/com/gema/service/SectionServiceTest.java` (new)
  Unit tests: happy path (entity saved with correct fields, response mapped
  correctly), QR code not found → `NotFoundException`, no unrelated
  repository calls (`findAll`/`findById`) — matches the isolation-check
  pattern already used in `QrcodeServiceTest`-style tests.
- `backend/src/test/java/com/gema/rest/SectionControllerTest.java` (new)
  `@WebMvcTest(SectionController.class)` + `@MockBean SectionService`:
  201 on valid request, 400 on blank/oversized title or content, 404 when
  the service reports the QR code missing.

## Dependencies
None. No new libraries — `jakarta.validation` (`@NotBlank`, `@Size`) and
Liquibase are already project dependencies.

## Execution order
1. Migration: `002-create-sections-table.sql` + `db.changelog.yml` include.
2. `SectionEntity` + `SectionRepository`.
3. `SectionSaveRequest` + `SectionCreateResponse` DTOs.
4. `SectionService.createSection` (TDD: `SectionServiceTest` first).
5. `SectionController` (TDD: `SectionControllerTest` first).
6. Full `gradle test` run to confirm no regressions.

## Risks and challenges
- **Integration test / live Postgres:** this plan does not add a
  Testcontainers-based integration test. Per the GAB-9 coordinator log, this
  sandbox's egress policy blocks pulling `postgres:16` from Docker Hub, and
  the requester previously directed that gap be accepted rather than worked
  around for GAB-9. The same constraint applies here; unit + `@WebMvcTest`
  slice tests are the coverage this plan can actually deliver in this
  environment. The acceptance criteria's `201`/`404` behavior is verified at
  the `@WebMvcTest` layer against a mocked service, and at the service-unit
  layer against a mocked repository — not against a real database.
- **"Unsafe code" re-occurring:** since the original review comment was a
  single word with no detail and the PR was never revised afterward, there
  is a real chance the size-validation fix in "Resolved decisions" #2 is not
  what the requester meant. This is called out explicitly in the PR
  description so it can be corrected quickly if wrong, rather than silently
  re-guessed a third time.
- **Liquibase changeset id collisions:** the new changeset id
  (`002-create-sections-table`) must not collide with any changeset added by
  a different in-flight branch (GAB-9's or GAB-11's redo). Checked: neither
  PR #18 (GAB-11) nor PR #19 (GAB-9) touches the changelog, so no collision.

## Open questions
None requiring a stop. The one real ambiguity (exact meaning of "Unsafe
code") is resolved above with an explicit, reversible, low-cost
interpretation (size-bound validation) rather than left as a hard blocker,
consistent with the precedent already set for the sibling GAB-9/GAB-11
redos in this same revert batch. Flagged transparently in the PR description
so the requester can correct it if the guess is wrong.
