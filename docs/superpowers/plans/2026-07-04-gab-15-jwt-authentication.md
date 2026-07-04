# GAB-15 JWT Authentication — Implementation Plan

**Goal:** Add JWT-based login for GEMA, and have user registration also return a token.

**Architecture:** A `JwtService` (HS256 via `jjwt`) generates tokens; `UserService` gains a `login` method and its existing `createUser` now returns a token too; a new `AuthController` exposes `POST /api/auth/login`.

**Tech Stack:** Spring Boot 3.5 / Java 21, `io.jsonwebtoken:jjwt` 0.12.6.

## Global Constraints
- JWT must be used for authentication (GAB-15 AC).
- JWT expiration must be configurable (GAB-15 AC).
- Login returns 200 + token on success, 401 on invalid credentials (GAB-15 AC).
- Registration (`POST /api/users`) returns a token on success (GAB-15 AC).

## Scope decisions
- No route is locked down behind the JWT in this change — the AC only requires login/registration to *issue* a token, not that any endpoint *require* one. Enforcement is deferred to whichever future issue first needs a protected route.
- Dev-only JWT secret is committed in `application.yaml` with an env-var override (`JWT_SECRET`), mirroring the existing committed Postgres dev credentials in the same file.

## Execution order
1. `build.gradle` + `application.yaml` — add `jjwt` deps and `app.jwt.*` config.
2. `UnauthorizedException` (401, reuses existing generic `RestException` handler).
3. `JwtService` (+ `JwtServiceTest`).
4. `UserRepository.findByUsername`.
5. `LoginRequest` / `AuthResponse` DTOs.
6. `UserService`/`UserController` updated to issue a token on registration (+ tests).
7. `AuthController` with `POST /api/auth/login` (+ tests).
8. Full local `gradle test` run, then review → test → final review → commit.
