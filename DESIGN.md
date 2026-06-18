# GEMA Frontend Design System

This document defines the visual language and screen inventory for the GEMA
frontend (GAB-7).

## Concept: the green sunflower

GEMA's mark fuses two existing symbols rather than inventing a generic one.
The **Sunflower Lanyard** is an international symbol worn to discreetly
signal a hidden disability or a need for patience and support. Awareness
cords/ribbons traditionally use color to signal a specific cause, and green
is widely used for mental health and many invisible-condition causes. GEMA
renders the sunflower with **green petals** around a **warm gold-brown
center** instead of the conventional all-yellow flower — keeping the
recognizable sunflower silhouette (so the lanyard association still reads)
while making the color story distinctly GEMA's own, not a generic yellow
SaaS flower.

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

| Screen | Route | Layout | Purpose | Key elements |
| --- | --- | --- | --- | --- |
| Landing Page | `/` | PublicLayout | Public marketing entry point for logged-out visitors | `SunflowerMark`, hero H1/sub-copy, primary CTA Button to `/home`, secondary "Entrar" link to `/login`, "Quem somos" boxed Card, 3-card grid explaining the product flow |
| Home | `/home` | AppLayout | Landing/dashboard after login | Hero copy, primary CTAs (scan / gallery), recent activity card |
| Login | `/login` | AppLayout | Authenticate an existing user | Email/password Input fields, primary submit Button, link to Create Account |
| Create Account | `/create-account` | AppLayout | Register a new user | Name/email/password Input fields, primary submit Button, links to Login and to `/welcome` |
| Profile | `/profile` | AppLayout | View/manage the current user | User name/email card, QR codes created count, edit/delete actions |
| QR Scan | `/qr/scan` | AppLayout | Scan a QR code | Camera preview placeholder, "Start scanning" Button |
| QR Gallery | `/qr/gallery` | AppLayout | Browse previously created/scanned codes | Grid of Cards, one per code, each linking to its detail/edit screen |
| QR Code Detail | `/qr/:publicId/edit` | AppLayout | View/edit a single code's guide | Title + description inputs, active toggle, save/delete actions |
| Onboarding | `/welcome` | PublicLayout | Explain the green-sunflower concept and how GEMA works pre-login | A 3-step flow (mark + symbolism blurb; "Create a QR code"; "Share it" + CTA) with Back/Next navigation and a `Header` step-progress indicator; CTA into Create Account on the final step |
| Emergency Guide View | `/q/:publicId` | PublicLayout | What someone sees after scanning a real GEMA QR code | Loading state, found+active guide rendered full-bleed (`Card variant="plain"`, title + free-text description), and a calm bounded-card "not active" state for missing/inactive codes |
| Not Found | `*` | PublicLayout | Branded 404 | Sunflower mark, friendly copy, link back to `/` |
| Style Guide | `/style-guide` | AppLayout | Developer-facing design system reference | All color swatches, full type scale, logo, all Button/Input/Card variants |

`AppLayout` renders the shared `Header` (sunflower wordmark + Home / Gallery
/ Profile nav links) above page content — used for every authenticated/
internal route. `PublicLayout` renders just the `SunflowerMark` with no nav
or Login link, for screens that must be reachable by someone who isn't
logged in and shouldn't be invited to navigate the internal app (most
importantly, whoever scans a real QR code at `/q/:publicId`).

Every screen still under active development carries a visible
**"Wireframe — no backend wired up"** badge so nobody mistakes these
placeholders for finished, backend-integrated features. `EmergencyGuideView`
keeps that same convention for consistency but its "found and active" state
intentionally drops the badge — that state is meant to read as a finished
support guide to the person scanning the code, not as a debug artifact. It
still runs entirely on mock data with a simulated loading delay
(`useState`/`useEffect`/`setTimeout`), exactly like the other wireframe
pages, since no backend client exists yet in this frontend. The shape it
renders (`publicId`, `title`, `description`, `isActive`, `createdAt`)
matches the current backend's guide model.

## Components

All components live in `frontend/src/components/` and are typed with
explicit TypeScript prop interfaces.

| Component | Variants / props | Notes |
| --- | --- | --- |
| `Button` | `variant`: `primary` \| `secondary` \| `danger`; `disabled` | Extends native `button` props. Primary = green fill, Secondary = white/outlined, Danger = red fill. Disabled state dims opacity and blocks pointer events. |
| `Input` | `label` (required), `helperText`, `errorText` | Renders a `<label>` + `<input>` pair with an `id` generated via `useId` when not supplied. When `errorText` is set, the border/ring turn red and the error text replaces any helper text. |
| `Card` | `variant`: `boxed` (default) \| `plain` | `boxed` is a bordered, padded container with a subtle warm-tinted shadow and a slightly asymmetric corner radius (organic feel) instead of uniform rounded corners. `plain` drops the border/shadow/background/padding for full-bleed content — used by the active `EmergencyGuideView` state (see "Layout & composition" below). |
| `Header` | `progress?: { step, total }` | Renders `SunflowerWordmark` + nav links (Home, Gallery, Profile) by default. Uses `flex-wrap` so it degrades gracefully on narrow viewports without a hamburger menu. When `progress` is supplied, renders a "Step X of Y" indicator with step dots instead of the nav links — used by the `Onboarding` flow. |
| `Logo` (`SunflowerMark`, `SunflowerWordmark`) | `size`, `className` | `SunflowerMark` is the icon-only mark (green petals, gold-brown center). `SunflowerWordmark` pairs it with the "GEMA" text for header/lockup use. A simplified static SVG copy lives at `frontend/public/favicon.svg`. |

## Color palette

| Token (Tailwind theme var) | Hex | Usage |
| --- | --- | --- |
| `--color-primary-green` (`bg-primary-green`) | `#4F8F3D` | Brand accent tone — icons, the logo's petals, swatches. Large/non-text use only (3.7:1 on cream). |
| `--color-primary-green-dark` (`bg-primary-green-dark`) | `#3E7A2F` | Primary button fill (white text on it is 5.2:1, passes AA); also safe as standalone text on cream (4.9:1). |
| `--color-leaf-green` (`bg-leaf-green`) | `#2F5233` | Deep green for text/links/headings on cream or mint backgrounds (8.3:1 on cream) and primary-button hover fill. |
| `--color-mint-50` (`bg-mint-50`) | `#E9F3E3` | Soft tinted background for badges (e.g. the "Wireframe" badge) and placeholder blocks. |
| `--color-accent-gold` (`bg-accent-gold`) | `#C97B2E` | Secondary/sunflower-center accent — badges, focus rings, decorative fills. Fails AA as body text on cream (3.1:1); use as a fill/border/icon only. |
| `--color-accent-gold-dark` (`bg-accent-gold-dark`) | `#8F5219` | Darker gold for the rare case gold needs to be text-safe (5.8:1 on cream). |
| `--color-base-white` (`bg-base-white`) | `#FFFFFF` | Card/surface backgrounds, secondary button fill. |
| `--color-surface-cream` (`bg-surface-cream`) | `#FBF8F0` | App background — warm cream instead of cool gray. |
| `--color-border-warm-200` (`bg-border-warm-200`) | `#E8E1D3` | Borders, dividers, secondary button border. |
| `--color-text-warm-900` (`text-text-warm-900`) | `#332E22` | Primary text, headings (12.7:1 on cream). |
| `--color-text-warm-600` (`text-text-warm-600`) | `#6B6354` | Secondary/muted text, helper text (5.6:1 on cream). |
| `--color-semantic-success` (`bg-semantic-success`) | `#27803F` | Success states; re-tuned darker than a typical "success green" so it also passes AA as standalone text on cream (4.7:1), not just as a fill. |
| `--color-semantic-danger` (`bg-semantic-danger`) | `#C23030` | Danger button fill, error text/borders; re-tuned darker so it passes AA as text on cream (5.3:1) as well as a fill with white text (5.6:1). |
| `--radius-organic` | `1.5rem 0.5rem 1.5rem 0.5rem` | Optional asymmetric corner radius for the occasional organic flourish. Most components just use ordinary `rounded-md`/`rounded-lg`; this token (or inline Tailwind arbitrary values) is reserved for accents, not a whole new shape system. |

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

## Accessibility: contrast on the cream background

The previous yellow palette only worked as a fill (dark text on top of
yellow); the green/gold palette is held to the same discipline, but several
tones were specifically re-tuned so they're also safe to use **as text**,
not just as fills, since green/gold show up more often in this redesign as
links and badge text:

- `primary/green` (`#4F8F3D`) is **3.7:1** on cream — below the 4.5:1 body
  text minimum. It's used for large icon/logo work and swatches, never as
  small standalone text. `primary/green-dark` (`#3E7A2F`, 4.9:1) and
  `leaf/green` (`#2F5233`, 8.3:1) are the text-safe greens.
- `accent/gold` (`#C97B2E`) is **3.1:1** on cream — also below 4.5:1. It's
  used as a fill/border/icon accent (the logo's flower center, focus rings)
  and never as standalone body text. `accent/gold-dark` (`#8F5219`, 5.8:1)
  exists for the rare case gold text is needed.
- `semantic/success` and `semantic/danger` were both darkened from typical
  "success green"/"danger red" defaults specifically so they clear 4.5:1 as
  text on cream (4.7:1 and 5.3:1 respectively), in addition to working as
  button fills with white text.
- Primary button fill uses `primary/green-dark`, not the lighter
  `primary/green`, because white text on the lighter tone is only 3.9:1 —
  below AA for normal-size button text. `primary/green-dark` gives white
  text 5.2:1.
- All body copy uses `text/warm-900` (12.7:1) or `text/warm-600` (5.6:1) on
  cream or white, both comfortably passing AA.

As before: **never use a fill-only accent color as a standalone text color.**
If a tone isn't listed above as text-safe, treat it as a fill, border, or
icon color only.

## Layout & composition

GAB-29 redefined how pages are *arranged*, leaving every color token, the
sunflower mark, and the type scale above untouched. Where the original
screen inventory used one de-facto shape ("centered card + stacked
inputs") for nearly every page regardless of purpose, screens now map onto
one of six named layout archetypes, chosen by the page's actual job
(setup task vs. crisis read vs. dashboard vs. browse vs. record-editing
vs. step-by-step explainer):

| Archetype | Structure | Screens |
| --- | --- | --- |
| **A — Calm data-entry form** | Narrow column (`max-w-sm`), vertically centered rather than pinned to the top. A single `Card` holds a contextual eyebrow label, the H1, the field stack, and the primary action; the "switch task" secondary link sits below the card, outside the task boundary. | `Login`, `CreateAccount`, `QrCodeInput` (create + success states) |
| **B — Crisis-read public guide** | Full-bleed: the found+active guide sits directly on the page background via `Card variant="plain"` — no bordered panel competing with the content. Single column, `max-w-prose` line length, eyebrow label as plain text, large heading, body text as the dominant element. The loading/error/unavailable states keep the bounded `Card` (`variant="boxed"`, the default) since they're brief status notes, not primary content. | `EmergencyGuideView` (ready state full-bleed; loading/error/unavailable stay boxed) |
| **C — Dashboard / landing** | Two-zone layout: a top action zone (greeting + primary actions as a button row) and a bottom "Recent activity" zone, promoted to a responsive grid of compact cards with an explicit empty-state message rather than a single placeholder card. | `Home` |
| **D — Browse / gallery** | Grid of cards, each a horizontal row (small leading thumbnail/status swatch beside the title/date text, not stacked above it) so a grid of many cards scans quickly. 3-column on wide viewports. | `QrCodeGallery` |
| **E — Profile / browse-and-edit** | Identity/context zone, separated by a rule from a stats/fact zone (a labeled stat, not an inline sentence), separated by a rule from the actions zone — with the destructive action (delete) visually deprioritized (smaller, set apart) relative to the primary action instead of sitting at equal weight beside it. | `UserProfile`, `QrCodeDetail` |
| **F — Narrative onboarding (multi-step)** | A 3-step flow within the single `/welcome` route (no new route): step 1 is the sunflower mark + symbolism blurb alone; step 2 is "Create a QR code"; step 3 is "Share it" plus the final CTA. A consistent Back/Next (or Back/Get started) button pair anchors the bottom of every step. The `Header`'s `progress` mode renders a "Step X of Y" indicator with step dots in place of the nav links for this flow. Step state is client-side only (resets on reload) and the indicator is informational — it doesn't support jumping to a step. | `Onboarding` |

`NotFound` follows the same "bounded status card on the plain background"
logic as archetype B's loading/error/unavailable states (a brief, calm
message, not primary content), without being assigned a full archetype of
its own. `StyleGuide` is a developer reference page, not a user-facing
screen, and stays a plain documentation layout (loosely following
archetype D's grid for its swatches).

### Component changes that support the archetypes

- **`Card`** gained a `variant?: 'boxed' | 'plain'` prop (default `'boxed'`,
  unchanged from before). `'plain'` drops the border/shadow/background/
  padding, used only by archetype B's full-bleed treatment.
- **`Header`** gained an optional `progress?: { step: number; total: number }`
  prop. When set, it renders a "Step X of Y" indicator with step dots
  instead of the Home/Gallery/Profile nav links — used only by archetype
  F's `/welcome` flow (wired through `PublicLayout` in `App.tsx`).
- `Button`, `Input`, and `Logo` are unchanged — every archetype above is
  built from their existing APIs.

## Trying it out

```
cd frontend
npm install
npm run dev
```

Then visit `/style-guide` in the running app for the full, interactive
design system reference.
