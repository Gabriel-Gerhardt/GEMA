# Implementation Plan: GAB-31 — New application frontend (React Native rebuild)

## Summary
Replace the existing `frontend/` project — a Vite + React (web, `react-dom`)
wireframe — with a proper React Native application (Expo managed workflow),
matching the tech stack the repo's own `README.md` already declares
("React Native + TypeScript (frontend)") but which was never actually built
that way. The new app implements the 12 screens defined in the "Broto"
design handoff supplied with this issue (`DesignGEMA.zip`), broken into
reusable components, with **no backend integration** — all data is local
mock/in-memory state, mirroring the pattern the current wireframe already
uses (`MOCK_CODES` in `QrCodeGallery.tsx`). The repo's root `DESIGN.md`
(currently a placeholder) is rewritten with the new design tokens and
screen inventory, per the issue's explicit instruction.

## Impacted projects
- `frontend/` only. `backend/` is untouched — this issue explicitly excludes
  backend integration. Root `DESIGN.md` is rewritten (design agent's output,
  spawned as a separate step per the coordinator protocol, but tracked here
  since it's part of this issue's acceptance criteria).

## Context gathered from the codebase and design handoff
- `frontend/package.json`: current stack is Vite 8 + React 19 + Tailwind v4
  (via `@tailwindcss/vite`) + `react-router-dom` + Vitest/RTL for tests. No
  React Native anywhere despite the README's stated stack.
- `frontend/src/index.css` (`@theme` block) and `frontend/src/components/*`
  (`Button.tsx`, `Card.tsx`, `Input.tsx`, `Header.tsx`, `Logo.tsx`) define an
  **older** design system: 8-petal single-tone sunflower, Inter font,
  `--color-primary-green: #4f8f3d`, English copy. This is the design the
  issue explicitly says NOT to use ("Does not use the design.md of the
  repository") — it predates the Broto handoff and is being fully replaced.
- `frontend/src/pages/*`: 12ish page components, each colocated with
  `*.test.tsx` (unit) and some `*.integration.test.tsx` files, all using
  Vitest + `@testing-library/react`, `render`/`screen`/`fireEvent`,
  `describe`/`it` blocks, `afterEach(cleanup)`. This is the testing
  convention to mirror (translated to React Native Testing Library).
- `frontend/src/lib/apiClient.ts`: a fetch wrapper for the backend, unused
  by any current page (all pages use local mock arrays). Not needed for
  this issue (no backend integration) — not carried over, to avoid building
  unused scaffolding (YAGNI).
- Design handoff (`design_handoff_gema/README.md`, `GEMA App - Broto.dc.html`,
  `GEMA Rebrand.dc.html`, all read in full): high-fidelity spec, PT-BR copy
  marked "final-intent," 12 screens (Landing, Onboarding ×3 steps, Login,
  Create Account, Home, Gallery, Plan Detail, Create Plan, Edit Plan, Plan
  Created/Success, Profile, Not Found) plus the public Emergency Guide View
  (`/q/:id`, found only in `GEMA Rebrand.dc.html`, direction 1a). Full design
  tokens (colors, Figtree typography scale, asymmetric `24px 8px 24px 8px`
  card radius, shadows) are in that README and are the source of truth for
  the new `DESIGN.md`.
- No CI workflow exists (`.github/` has no files) and no simulator/device is
  available in this sandboxed environment — verification relies on Jest +
  React Native Testing Library, plus `expo start --web` (react-native-web)
  as a visual sanity check via the pre-installed headless Chromium, since
  Expo's web target renders the same component tree.

## Resolved decisions
Judgment calls within the code persona's authority (technical scoping, not
business/product trade-offs), recorded here rather than escalated, per the
GAB-13 coordinator-log precedent for this same protocol:

1. **Replace `frontend/` in place, not a new parallel directory.** The
   README already declares React Native as the intended stack; the current
   `frontend/` is a wrong-stack implementation of that same intent, not a
   second product. Old web files are removed (not archived) — recoverable
   from git history if ever needed, and this stays on the feature branch
   until reviewed.
2. **Expo (managed workflow), not bare React Native.** No native
   build toolchain (Xcode/Android Studio) is available in this environment;
   Expo's managed workflow plus `expo start --web` is the only way to run
   and sanity-check the app here, and is itself a standard, widely-used
   pattern for RN apps — not a shortcut that hurts architecture.
3. **NativeWind (Tailwind for React Native)** for styling, over the plain
   `StyleSheet` API. Mirrors the existing web app's utility-class,
   token-driven approach (`@theme` tokens → Tailwind classes) so the design
   token file doubles as both the `DESIGN.md` source and the working
   `tailwind.config.js`, avoiding a second source of truth for colors.
4. **React Navigation** (native-stack + bottom-tabs) for routing — the
   de facto standard for Expo/RN apps, directly analogous to the current
   app's `react-router-dom` usage (`RootNavigator` auth-gated switch mirrors
   today's `AppLayout`/`PublicLayout` split in `App.tsx`).
5. **Jest (`jest-expo` preset) + React Native Testing Library**, translating
   the existing Vitest/RTL convention (same `describe`/`it`/`render`/
   `fireEvent` shape, `@testing-library/react-native` instead of
   `@testing-library/react`).
6. **Mock auth and mock plan data live in local React Context, in-memory
   only** (no `AsyncStorage`/persistence, no network calls). Submitting the
   Login/Create Account forms with non-empty fields flips the mock
   "signed in" flag and navigates into the app shell — this keeps the app
   click-through/demoable end to end (Landing → Login → Home → Gallery →
   Plan Detail → Edit → back) without inventing a real auth system, which
   the acceptance criteria explicitly excludes for now.
7. **Section drag-reorder is a known, named shortcut.** The design's drag
   handle (⋮⋮) is rendered, but reordering is implemented via tap-to-move
   (up/down), not a full pan-gesture drag library. Ceiling: no live
   drag-and-drop feel; upgrade path: `react-native-draggable-flatlist` (adds
   `react-native-gesture-handler` + `react-native-reanimated`) if/when this
   is prioritized. The design handoff's own README flags reorder
   persistence (an `order` field) as "not in the stated schema, flag with
   the team" — consistent with treating this as intentionally lightweight
   for now.
8. **Destructive actions (remove section, "Excluir plano", "Excluir conta")
   confirm via the built-in `Alert.alert`**, no new dependency — satisfies
   the design README's "confirm step recommended before deletion."
9. **Inactive-plan state on the public Emergency Guide View is out of
   scope.** The design README itself says this is "not yet designed, flag
   if needed." Only the active-plan guide (the one screen actually
   designed) is implemented.
10. **Figtree font** loaded via `@expo-google-fonts/figtree` +
    `expo-font`, with the system sans as fallback while loading — standard
    Expo pattern, avoids bundling static font files by hand.

## Files to create/edit

### Remove (old wireframe)
- All of `frontend/src/` (components, pages, `App.tsx`, `main.tsx`,
  `index.css`, `lib/apiClient.ts`, `vite-env.d.ts`), `frontend/index.html`,
  `frontend/vite.config.ts`, `frontend/eslint.config.js`,
  `frontend/tsconfig*.json`, `frontend/public/*` — the whole Vite web app.
  `frontend/README.md`, `frontend/.env.example` also removed (no backend
  wiring, no separate frontend README needed — root README documents setup).

### Project scaffolding (new)
- `frontend/app.json` — Expo app config (name, slug, icon placeholder,
  splash, web bundler).
- `frontend/package.json` — new dependency set (Expo, React Native, React
  Navigation, NativeWind, Jest/RNTL — full list under Dependencies below).
- `frontend/tsconfig.json` — Expo's TS base config.
- `frontend/babel.config.js` — `babel-preset-expo` + NativeWind's Babel
  plugin.
- `frontend/metro.config.js` — Expo Metro config with NativeWind's CSS
  transform wired in.
- `frontend/tailwind.config.js` — NativeWind preset, `content` globs over
  `src/`, `theme.extend` populated from the Broto design tokens.
- `frontend/global.css` — Tailwind directives entry point NativeWind needs.
- `frontend/nativewind-env.d.ts` — NativeWind's TS ambient types reference.
- `frontend/jest.config.js` — `jest-expo` preset, RNTL setup file.
- `frontend/jest.setup.js` — `@testing-library/jest-native` matchers
  (or RNTL's built-in matchers if the installed version bundles them).
- `frontend/.gitignore` — Expo-specific ignores (`.expo/`, `dist/`, native
  build dirs) layered on top of the root `.gitignore`'s existing JS section.

### Design tokens
- `frontend/src/theme/tokens.ts` — single source of truth for colors,
  radii, shadows, spacing, typography scale (weights/sizes/line-heights),
  transcribed from the design handoff README's "Design Tokens" section.
  `tailwind.config.js` imports from this file so there is exactly one place
  these values live.

### Shared components (`frontend/src/components/`), each with a colocated
`*.test.tsx`
- `SunflowerMark.tsx` — the 12-petal mark as an `react-native-svg` icon
  component (ported directly from the design HTML's inline SVG `<g id="gm">`
  def), plus a `SunflowerWordmark` variant (mark + "GEMA" text) for headers.
- `Button.tsx` — `primary`/`secondary` variants (design has no destructive
  *button*, only destructive text links — see below), asymmetric-radius-free
  (buttons use the smaller `14px`/`16px` radius per tokens, not the card
  asymmetric radius).
- `Card.tsx` — the signature `24px 8px 24px 8px` asymmetric radius panel.
- `Input.tsx` — labeled text field (email/password/name/title), matching
  the design's `.input`/`.lbl` styles.
- `TextArea.tsx` — multiline variant for section content (new — the old web
  app had no textarea component).
- `StatusDot.tsx` — small filled circle, green/gray, used in Gallery/Home/
  Plan Detail for active/inactive status.
- `SectionEditorItem.tsx` — one repeatable block in the Create/Edit Plan
  section list: drag-handle glyph, "Seção N" label, × remove (with
  `Alert.alert` confirm), title `Input`, content `TextArea`, up/down
  reorder taps.
- `SectionReadItem.tsx` — read-only section block (title as uppercase
  amber label + body) for Plan Detail and the Emergency Guide View.
- `QrPlaceholder.tsx` — the dashed diagonal-stripe placeholder box (no real
  QR generation library — out of scope, matches the design mock which is
  itself a placeholder).
- `EmptyState.tsx` / reuse of `Card` for the Not Found screen's centered
  message card (kept minimal — no separate `ErrorState`/`LoadingState`
  components ported from the old app since nothing in this issue's scope
  triggers loading/error states without a backend).

### Navigation (`frontend/src/navigation/`)
- `types.ts` — `RootStackParamList`/`AppTabParamList` param-list types for
  typed navigation throughout.
- `PublicStack.tsx` — native-stack navigator: Landing, Onboarding, Login,
  CreateAccount, EmergencyGuide, NotFound.
- `AppTabs.tsx` — bottom-tabs navigator (Início/Galeria/Perfil) per the
  design's 3-tab app shell; each tab hosts its own native-stack so Gallery
  can push Plan Detail → Edit, and Home can push Create Plan → Success.
- `RootNavigator.tsx` — top-level switch on the mock auth context: signed
  out → `PublicStack`, signed in → `AppTabs`.

### Mock data & state (`frontend/src/mocks/` and `frontend/src/state/`)
- `mocks/plans.ts` — seed plan/section data (mirrors old `MOCK_CODES`, PT-BR
  copy, extended with a `sections: Section[]` array per plan, matching the
  design's "Guia do Lucas" example content).
- `state/AuthContext.tsx` — mock signed-in boolean + `signIn`/`signOut`.
- `state/PlansContext.tsx` — in-memory plans list + create/update/delete/
  reorder-sections actions, seeded from `mocks/plans.ts`.

### Screens (`frontend/src/screens/`), each with a colocated `*.test.tsx`
- `LandingScreen.tsx`, `OnboardingScreen.tsx` (internal 3-step state +
  segmented progress bar), `LoginScreen.tsx`, `CreateAccountScreen.tsx`,
  `HomeScreen.tsx`, `GalleryScreen.tsx`, `PlanDetailScreen.tsx`,
  `CreatePlanScreen.tsx`, `EditPlanScreen.tsx`, `PlanCreatedScreen.tsx`,
  `ProfileScreen.tsx`, `NotFoundScreen.tsx`, `EmergencyGuideScreen.tsx`.

### App entry
- `frontend/App.tsx` — loads Figtree via `expo-font`/`@expo-google-fonts`,
  wraps `NavigationContainer` + `AuthProvider` + `PlansProvider` +
  `RootNavigator`, shows a plain loading view until fonts are ready.
- `frontend/index.ts` (Expo's `registerRootComponent` entry, replacing the
  old `main.tsx`).

### Documentation
- `DESIGN.md` (repo root) — rewritten by the design agent (spawned as its
  own coordinator step) with the Broto tokens, typography scale, radii,
  screen inventory, and component list, replacing the "Awaiting for new
  design" placeholder. Sourced from the design handoff README plus this
  plan's component inventory, so it documents what actually got built.
- `README.md` (repo root) — the "Set-up" section's `npm run dev` frontend
  instructions updated to Expo's start command (`npx expo start`), since
  the old Vite-specific instructions no longer apply.

## Dependencies (frontend/package.json)

**Runtime:** `expo`, `react`, `react-native`, `react-native-web` (Expo web
target), `@react-navigation/native`, `@react-navigation/native-stack`,
`@react-navigation/bottom-tabs`, `react-native-screens`,
`react-native-safe-area-context`, `react-native-svg`, `nativewind`,
`expo-font`, `@expo-google-fonts/figtree`.

**Dev:** `typescript`, `@types/react`, `tailwindcss` (peer for NativeWind),
`jest`, `jest-expo`, `@testing-library/react-native`, `eslint`,
`eslint-config-expo` (or continue with the existing `typescript-eslint`
setup if the Expo preset conflicts — decide during scaffolding, not a plan
decision).

**Removed:** `vite`, `@vitejs/plugin-react`, `@tailwindcss/vite`,
`react-dom`, `react-router-dom`, `vitest`, `jsdom`,
`@testing-library/react`, `@types/react-dom` — all Vite/DOM-specific,
replaced by their Expo/RN equivalents above.

## Execution order
1. Design agent produces the new `DESIGN.md` (spawned per the coordinator's
   placement rule, before/alongside coding).
2. Remove the old `frontend/` web app files; scaffold the Expo project
   (config files, `package.json`, Babel/Metro/Tailwind/Jest config).
3. Build `theme/tokens.ts` and wire it into `tailwind.config.js`.
4. Build shared components bottom-up: `SunflowerMark` → `Button`/`Card`/
   `Input`/`TextArea`/`StatusDot`/`QrPlaceholder` → `SectionEditorItem`/
   `SectionReadItem` (depend on `Input`/`TextArea`/`Button`).
5. Build `mocks/plans.ts`, `state/AuthContext.tsx`, `state/PlansContext.tsx`.
6. Build navigation shell (`types.ts`, `PublicStack`, `AppTabs`,
   `RootNavigator`) with placeholder screens, confirm routes resolve.
7. Build screens in the design's own numbering order (Landing → Onboarding
   → Login/CreateAccount → Home → Gallery → Plan Detail → Create Plan →
   Edit Plan → Plan Created → Profile → Not Found → Emergency Guide),
   wiring each into its real navigator entry as it's built.
8. Wire `App.tsx` (font loading + providers + `RootNavigator`).
9. Unit tests alongside each component/screen (written in the same pass,
   per the code agent's own testing rule); run the full suite.
10. `expo start --web` sanity pass via headless Chromium for a visual
    spot-check of a handful of screens (Landing, Home, Create Plan) against
    the design mock, time permitting — supplementary to, not a replacement
    for, the Jest/RNTL suite.
11. Update root `README.md` setup instructions.

## Risks and challenges
- **No device/simulator in this environment** — the primary safety net is
  the Jest/RNTL suite plus an Expo-web spot check; genuine native-only
  behavior (haptics, native gesture feel) can't be verified here and isn't
  attempted (nothing in scope requires it beyond the already-scoped-down
  section reordering).
- **NativeWind + Expo Metro/Babel wiring** is the one part of scaffolding
  most likely to need iteration (version compatibility between the
  installed Expo SDK, NativeWind, and Tailwind major versions) — flagged as
  the step most likely to eat unplanned time.
- **Font loading flash** (system font before Figtree loads) — mitigated by
  gating first render on `useFonts()` per Expo's documented pattern, but
  worth calling out since the design's typography is a core part of its
  "calm/airy" tone.
- **12 screens is a large surface for one pass** — sequenced so that a
  partial result (if this run is interrupted) still leaves the app in a
  buildable, navigable state (navigation shell wired early, screens filled
  in incrementally in design order) rather than a big-bang all-or-nothing
  change.

## Open questions
None blocking. All ambiguities encountered during planning had either
strong contextual evidence to resolve them (see "Resolved decisions" above)
or were explicitly deferred by the design handoff's own README (inactive
public-link state, section-order persistence) — in both cases, escalating
would be re-litigating something already settled, which the coordinator
protocol says not to do.
