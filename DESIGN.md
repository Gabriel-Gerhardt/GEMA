# GEMA Frontend Design System

This document defines the visual language and screen inventory for the GEMA
frontend (GAB-7).

## Source of truth

The canonical color and typography foundations were defined as variables in
the Figma file `OaO1IRV01iXDCEECEbbWB0`. However, building out the full
component library and screen mockups in Figma is currently blocked by a
Starter-plan rate limit on that file. Until that's lifted, **this code
implementation is the working source of truth**: the design tokens live in
`frontend/src/index.css`, the components live in `frontend/src/components/`,
and the `/style-guide` route (`frontend/src/pages/StyleGuide.tsx`) is the
living, browsable reference. Run `npm run dev` in `frontend/` and visit
`/style-guide` to see every color, type style, and component variant
rendered together.

## Screens

| Screen | Route | Purpose | Key elements | Navigation |
| --- | --- | --- | --- | --- |
| Home | `/` | Landing/dashboard after login | Hero copy, primary CTAs (scan / gallery), recent activity card | Header nav (Home, Gallery, Profile); links to Scan and Gallery |
| Login | `/login` | Authenticate an existing user | Email/password Input fields, primary submit Button, link to Create Account | Linked from Create Account; reachable directly by URL |
| Create Account | `/create-account` | Register a new user | Name/email/password Input fields, primary submit Button, link to Login | Linked from Login |
| Profile | `/profile` | View/manage the current user | User name/email card, QR codes created count, edit/delete actions | Header nav |
| QR Scan | `/qr/scan` | Scan a QR code | Camera preview placeholder, "Start scanning" Button | Linked from Home |
| QR Gallery | `/qr/gallery` | Browse previously created/scanned codes | Grid of Cards, one per code, with label + created date | Header nav ("Gallery"); linked from Home |
| Style Guide | `/style-guide` | Developer-facing design system reference | All color swatches, full type scale, all Button/Input/Card variants | Reachable by URL; not in primary nav (internal tool) |

All screens render the shared `Header` (brand mark + Home / Gallery / Profile
links) above their content.

Every screen carries a visible **"Wireframe — no backend wired up"** badge so
nobody mistakes these placeholders for finished, backend-integrated features.
Data shown (user info, QR code lists) is inline static/mock data defined in
the page component itself — no `mocks/` folder, no real API calls.

## Components

All components live in `frontend/src/components/` and are typed with
explicit TypeScript prop interfaces.

| Component | Variants / props | Notes |
| --- | --- | --- |
| `Button` | `variant`: `primary` \| `secondary` \| `danger`; `disabled` | Extends native `button` props. Primary = yellow fill, Secondary = white/outlined, Danger = red fill. Disabled state dims opacity and blocks pointer events. |
| `Input` | `label` (required), `helperText`, `errorText` | Renders a `<label>` + `<input>` pair with an `id` generated via `useId` when not supplied. When `errorText` is set, the border/ring turn red and the error text replaces any helper text. |
| `Card` | — | Simple bordered, padded, subtly-shadowed container (`HTMLAttributes<HTMLDivElement>` passthrough). |
| `Header` | — | Brand/logo slot + nav links (Home, Gallery, Profile). Uses `flex-wrap` so it degrades gracefully on narrow viewports without a hamburger menu. |

## Color palette

| Token (Tailwind theme var) | Hex | Usage |
| --- | --- | --- |
| `--color-primary-yellow` (`bg-primary-yellow`) | `#FFC81C` | Primary button fill, accent backgrounds/borders/icons, badges |
| `--color-primary-yellow-dark` (`bg-primary-yellow-dark`) | `#E0A800` | Primary button hover state |
| `--color-base-white` (`bg-base-white`) | `#FFFFFF` | Card/surface backgrounds, secondary button fill |
| `--color-surface-gray-50` (`bg-surface-gray-50`) | `#FAFAF9` | App background |
| `--color-border-gray-200` (`bg-border-gray-200`) | `#E5E5E2` | Borders, dividers, secondary button border |
| `--color-text-gray-900` (`text-text-gray-900`) | `#262624` | Primary text, headings |
| `--color-text-gray-600` (`text-text-gray-600`) | `#5C5C58` | Secondary/muted text, helper text |
| `--color-semantic-success` (`bg-semantic-success`) | `#2F9E4F` | Success states (not yet used in a screen, reserved) |
| `--color-semantic-danger` (`bg-semantic-danger`) | `#D14040` | Danger button fill, error text/borders |

## Typography (Inter)

Loaded via a Google Fonts `<link>` tag in `frontend/index.html` (no extra npm
font package), with `system-ui, sans-serif` fallback configured as
`--font-sans` in `frontend/src/index.css`.

| Style | Size | Weight |
| --- | --- | --- |
| H1 | 32px | Bold (700) |
| H2 | 24px | Bold (700) |
| H3 | 18px | SemiBold (600) |
| Body | 16px | Regular (400) |
| Caption | 13px | Regular (400) |

## Accessibility: yellow contrast (WCAG AA)

`#FFC81C` (primary/yellow) on a white or `surface/gray-50` background has a
contrast ratio against `text/gray-900` (`#262624`) of roughly **1.4:1 when
yellow is the text color** — far below the WCAG AA minimum of 4.5:1 for
normal text (or 3:1 for large text). **Yellow must never be used as a
standalone text color on a light background.**

Instead, yellow is used the way it's used throughout this implementation:

- As a **fill** (button backgrounds, badges) with dark `text-gray-900` text
  on top — `#262624` on `#FFC81C` is roughly **10.7:1**, comfortably passing
  AA (and AAA) for any text size.
- As **borders, icons, and accents**, not as the text color itself.

`primary/yellow-dark` (`#E0A800`) is reserved for hover states on yellow
fills, not as an attempt to "fix" yellow-as-text-color contrast — the fix is
architectural (don't put text in yellow), not a darker shade.

All body copy uses `text/gray-900` or `text/gray-600` on white or
`surface/gray-50` backgrounds, both of which comfortably pass AA.

## Trying it out

```
cd frontend
npm install
npm run dev
```

Then visit `/style-guide` in the running app for the full, interactive
design system reference.
