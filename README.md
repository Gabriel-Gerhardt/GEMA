# GEMA

QrCode app that will allow personalized qr codes for autistics or others syndromes in case they get lost or enter in a crisis.
The QR will contain necessary information/guideline that will help others to support the person in case of emergency

## How It Works

- The plan owner creates an account and writes a **plan**: a short title plus a
  list of **sections** ("Sobre mim", "O que ajuda", "Em uma emergência"), each a
  few calm sentences in their own words.
- The backend gives the plan a short **public id** and can render a QR code
  image encoding the plan's public guide URL (`{frontend}/q/{publicId}`).
- The owner carries that QR code — on a card, a bracelet, a bag tag.
- A stranger scans it and lands on the **Emergency Guide View**: no login, no
  app install, just the sections in order and a tappable emergency contact.
- The owner can deactivate a plan at any time; a deactivated plan stops being
  served publicly (the guide returns 404) without being deleted.

## Tech Stack

- Java 21 + Spring Boot (backend)
- PostgreSQL
- React Native + TypeScript, Expo SDK 57 (frontend)

## Architecture

The project follows a Hexagonal Architecture (Ports and Adapters) approach. The goal is to isolate business logic from frameworks and external systems.

Core contains domain models, use cases, and ports (interfaces). It represents the business rules and has no dependency on frameworks or infrastructure. Adapters implement external concerns such as database access and external APIs. The web layer exposes the system through REST controllers.

The dependency rule is strict: the core must not depend on adapters, frameworks, or infrastructure. All dependencies point inward toward the core.

## Use Cases

- Creation of emergency QR profiles  
- Quick access to critical personal/medical information  
- Support for individuals with specific conditions or in emergency situations  
- Reliable information sharing with first responders

## API

Routes are split by audience:

| Prefix | Audience | Notes |
|---|---|---|
| `/api/q/**` | Public — reached by scanning a QR code | Read-only; serves **active** plans only |
| `/api/qrcodes/**` | Plan owner | Full create/read/update/delete, any active state |
| `/api/users/**`, `/api/auth/**` | Accounts | Register, login, read, delete |

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/api/users` | Register (returns token + user id) |
| `POST` | `/api/auth/login` | Log in |
| `GET` | `/api/users/{id}` | Account details and its plans |
| `DELETE` | `/api/users/{id}` | Delete account (cascades to plans) |
| `POST` | `/api/qrcodes` | Create a plan |
| `GET` | `/api/qrcodes/{publicId}` | Read a plan as its owner |
| `PUT` | `/api/qrcodes/{publicId}` | Rename / toggle active |
| `DELETE` | `/api/qrcodes/{publicId}` | Delete a plan |
| `GET` | `/api/qrcodes/{publicId}/image` | PNG of the plan's QR code |
| `GET`/`POST`/`PUT` | `/api/qrcodes/{publicId}/sections` | Manage sections |
| `GET` | `/api/q/{publicId}` | Public guide (404 if deactivated) |
| `GET` | `/api/q/{publicId}/sections` | Public guide sections (404 if deactivated) |

> **Not yet authenticated.** A JWT is issued at register/login, but no filter
> validates it, so every route above is currently reachable without one. The
> prefix split exists so the owner surface can be gated with a single matcher
> once that work lands. Do not deploy this as-is.

## Set-up

```sh
git clone https://github.com/Gabriel-Gerhardt/GEMA.git
cd GEMA

# Backend — Postgres first, then the app
cd backend
docker compose up -d
./gradlew clean build      # runs the test suite; no database needed for this
./gradlew bootRun

# Frontend
cd ../frontend
npm install
npx expo start --web
```

### Configuration

| Variable | Default | Purpose |
|---|---|---|
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | local Postgres | Datasource |
| `APP_BASE_URL` | `http://localhost:8080` | This API's own base URL |
| `APP_PUBLIC_BASE_URL` | `http://localhost:8081` | **Frontend** base URL — this is what a generated QR code encodes |
| `APP_CORS_ALLOWED_ORIGINS` | `http://localhost:8081,http://localhost:19006` | Comma-separated CORS origins |
| `JWT_SECRET` | dev-only placeholder | Signing key — override outside local dev |
| `JWT_EXPIRATION_MS` | `3600000` | Token lifetime |

## Access

API: http://localhost:8080 — Swagger UI at http://localhost:8080/swagger-ui.html  
Frontend: Expo Dev Tools opens a QR code and a web preview link (defaults to http://localhost:8081) — scan it with Expo Go on a device, or press `w` to open the web preview.  

## Contact

LinkedIn: https://www.linkedin.com/in/gabriel-gerhardt-0a8b852b9/  
Email: mailto:gabrielgerhardt27@gmail.com  
GitHub: https://github.com/Gabriel-Gerhardt
