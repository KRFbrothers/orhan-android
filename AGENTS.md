# AGENTS.md — Universal AI Coding Agent Rules & Clean Architecture Contract for Orhan (`KRFbrothers/orhan-android`)

> **Cross-Tool Standard:** This file follows the open [AGENTS.md](https://agents.md/) specification. Every AI coding agent working in this repository—including **Google Antigravity (`agy`)**, **Grok Build (`grok`)**, **Claude Code (`claude`)**, **OpenAI Codex (`codex`)**, **Cursor**, **GitHub Copilot**, and **Orhan's internal coding agent**—MUST read and obey these rules strictly before planning or editing any file.

## 0. ZERO-ASSUMPTION & ANTI-HALLUCINATION PROTOCOL (HARD STOP ON GUESSING)

**ABSOLUTE RULE FOR ALL AI AGENTS: NEVER ASSUME, NEVER GUESS, AND NEVER WRITE SPECULATIVE CODE ON YOUR OWN.**

The repository owner is a **product-focused non-coder**. Agents frequently break projects by making silent assumptions, inventing unrequested features, guessing file names, or fabricating UI styles. In this repository, **assumption-driven coding is strictly forbidden**:

1. **Zero Silent Assumptions (Stop & Ask When Unsure):**
   - You may ONLY write code that is directly backed by one of three verified sources of truth:
     1. **Explicit instruction** in the owner's prompt.
     2. **Verified existing source code** in `orhan-android` (read completely from line 1 to the end of the file before editing).
     3. **Verified REA / ADB inspection evidence** (actual decompiled classes, manifest/resources, or live screenshots from the connected reference device when the owner asks to match a reference app).
   - If any color, layout detail, button behavior, API field, or requirement is missing or ambiguous, **STOP IMMEDIATELY and ask the owner in simple Hinglish/English** with 2–3 clear choices. Never "fill in the blanks" with your own imagination.
2. **Ignore Legacy / Historical Specs & HTML Mockups:**
   - **DO NOT follow** `docs/legacy/README.old.md`, `docs/verification/TEAMLY_PHASE_STATUS.md`, or the static HTML prototype in `design/` (`design/index.html`) as active instructions. Those are historical artifacts from older completed PRs and are **NOT** active instructions.
   - Always treat `AGENTS.md` + the owner's current prompt as the sole active specification.
3. **Mandatory Pre-Edit Evidence Table:**
   - Before modifying any file, you MUST verify and state:
     - **Canonical File Path:** Which exact existing file in `orhan-android` will be edited.
     - **Source of Truth:** Where each change comes from (e.g., exact REA class/method/screenshot or explicit owner instruction).
     - **Preserved Behavior:** Confirmation that existing ViewModel callbacks, state flows, and `Modifier.testTag` values remain intact.
   - If you cannot point to a concrete source of truth for a change, **do not write that code**.
4. **Zero Scope Creep & Zero Unrequested "Improvements":**
   - Touch ONLY what the owner asked for. If the owner asks for a UI/UX theme or layout update, do NOT touch backend routes, authentication, database schemas, or unrelated screens.
   - Never add new libraries or dependencies to `gradle/libs.versions.toml` or `app/build.gradle.kts` unless explicitly approved by the owner.
5. **Zero Fake / Dummy UI or No-Op Buttons:**
   - Never invent fake buttons (`onClick = {}`), hardcoded dummy chat messages, fake user profiles, or pretend toggles that have no real backing state in `feature/chat/ChatViewModel.kt`. Every UI control must wire to real existing `orhan-android` state/callbacks or be omitted.
6. **End-to-End Autonomous Execution (Zero Broken Builds Left for the Owner):**
   - Once the requirements and evidence are verified, handle 100% of imports, token wiring, test updates, and compile fixes yourself. Never leave `// TODO`, `TODO()`, or broken builds for the owner to debug.

## 1. Repository Architecture Map

**Orhan (`ai.orhan`)** is a private, owner-locked multimodal GenAI assistant shipped as a **single-module native Android Jetpack Compose app** (`app/`), talking to the Dua AI engine over SSE. There is **no monorepo**: no `apps/`, no `infra/`, no `tooling/`, no web frontend and no Node API in this repository.

| Layer / Directory | Tech Stack & Runtimes | Key Entry Points & Files | Core Responsibility |
| :--- | :--- | :--- | :--- |
| **App Entry & Root Scaffold** (`app/src/main/java/ai/orhan/`) | Kotlin 2.0.21 (K2) + Jetpack Compose (BOM 2024.12.01) + Material 3; AGP 8.7.3; Gradle 8.9; compileSdk 35, targetSdk 35, minSdk 26, JVM target 17 | `MainActivity.kt`, `app/OrhanApp.kt`, `app/OrhanNavHost.kt`, `app/AppLockGate.kt` | Single-activity Compose host, navigation graph, app-lock gate |
| **Design System** (`app/src/main/java/ai/orhan/design/`) | Compose `@Immutable` design tokens | `theme/Color.kt`, `theme/Theme.kt`, `theme/Type.kt`, `theme/Shape.kt`, `theme/ThemePreferences.kt`, `theme/FallbackFonts.kt`, `components/OrhanCubeLogo.kt`, `icons/OrhanIcons.kt` | Single source of truth for Light/Dark palettes, typography, shapes, and the Orhan cube brand mark |
| **Screens** (`app/src/main/java/ai/orhan/design/screens/`) | Compose screens | `HomeScreen.kt`, `ChatScreen.kt` | The two top-level screens (home/chat list and the chat thread) |
| **Features** (`app/src/main/java/ai/orhan/feature/`) | Compose + ViewModel | `chat/` (`ChatViewModel.kt`, `AgentInstructionsBlock.kt`, `MeetingMinutesActions.kt`), `agents/`, `artifacts/`, `auth/`, `discover/`, `memory/`, `profile/`, `search/`, `settings/` (+ `settings/appearance/`, `settings/detail/`), `workflows/` | Feature UI and presentation logic, one package per domain |
| **Data Layer** (`app/src/main/java/ai/orhan/data/`) | Kotlin + DataStore + `HttpURLConnection` + `org.json` | `agents/`, `applock/`, `artifacts/`, `auth/`, `chat/`, `history/`, `memory/`, `permissions/`, `prefs/`, `workflows/` | Repositories, local stores, network clients, persistence |
| **Android Resources** (`app/src/main/res/`) | Android resource XML + PNG | `values/strings.xml`, `drawable/` (42 cube icons + launcher layers), `drawable-night/`, `drawable-nodpi/`, `mipmap-*/` + `mipmap-night-*/`, `font/`, `xml/` | Strings, brand drawables, launcher icons (light + night), fonts, file-provider paths |
| **Design Showcase** (`design/`) | Self-contained HTML/CSS | `index.html`, `README.md`, `fonts/`, `images/`, `icons/cube-42/` (42 SVGs + `compose/CubeIcons.kt` + `gen.py`), `preview-assets/before/` | Offline 14-screen HTML mockup and the cube-icon source set |
| **Documentation** (`docs/`) | Markdown | `BACKEND.md`, `verification/TEAMLY_PHASE_STATUS.md`, `legacy/README.old.md` | Backend contract, phase ledger, archived README |
| **Build & CI** (repo root, `.github/`) | Gradle Kotlin DSL + GitHub Actions | `settings.gradle.kts`, `build.gradle.kts`, `app/build.gradle.kts`, `gradle/libs.versions.toml`, `VERSION`, `.github/workflows/checks.yml`, `.github/workflows/build-apk.yml` | Build config, version catalog, version source of truth, CI |

## 2. Canonical File Ownership Map & Anti-Duplication Rules

### 2.1 Zero Duplication Rule (STRICT)

When redesigning UI/UX (such as applying reference app themes, layouts, bottom sheets, or navigation) or adding features:

- **NEVER create parallel or suffixed files** such as `ChatScreenNew.kt`, `ChatScreenV2.kt`, `OrhanThemeV2.kt`, `AgentsScreen.kt`, `CreateAgentScreen.kt`, `ArtifactsScreen.kt`, or `EditProfileScreen.kt` when a canonical owner file already exists in `ai.orhan`.
- **NEVER create duplicate Composable functions** with slightly different names in the same package to avoid editing the original function. Refactor the canonical Composable **in place** while keeping its public state/callback signature compatible with existing callers and tests.
- **Reuse existing navigation destinations before adding new ones:** if a feature matches a route already declared in `app/OrhanNavHost.kt` (`AppRoutes`) or the settings routes, wire the implementation into that existing destination rather than inventing duplicate routes.
- **Delete dead helpers immediately:** if an internal private helper composable inside a file is superseded by an in-place redesign, delete the unused private helper in the same edit so zero dead code accumulates.

### 2.2 Canonical File Lookup Table (`app/src/main/java/ai/orhan/`)

Always inspect and edit the exact canonical owner listed below instead of inventing or assuming a filename:

| Feature / Domain Area | Canonical Owner File(s) (Relative to `app/src/main/java/ai/orhan/`) | Do NOT Create / Confuse With |
| :--- | :--- | :--- |
| **App Entry & Root Scaffold** | `MainActivity.kt`, `app/OrhanApp.kt`, `app/AppLockGate.kt` | Do not create a second Activity or a secondary root theme wrapper. |
| **Navigation Graph & Routes** | `app/OrhanNavHost.kt` (`AppRoutes`, settings routes) | Do not create a parallel `AppNavigation.kt` or a duplicate NavHost. |
| **Home & Chat Screens** | `design/screens/HomeScreen.kt`, `design/screens/ChatScreen.kt` | Do not create `HomeScreenV2.kt` or `ChatScreenNew.kt`. These two own the home/chat-list and the chat thread. |
| **Chat Presentation & State** | `feature/chat/ChatViewModel.kt`, `feature/chat/AgentInstructionsBlock.kt`, `feature/chat/MeetingMinutesActions.kt` | Do not create a second chat ViewModel or a parallel message-state holder. |
| **Chat Data & Transport** | `data/chat/` (repository + SSE parsing), `data/history/` (thread store) | Do not create a second HTTP client; reuse the existing repository and `HttpURLConnection` + `org.json` approach (no new deps). |
| **Agents** | `feature/agents/`, `data/agents/` | Do not create `AgentsScreen.kt` or `CreateAgentScreen.kt` outside `feature/agents/`. |
| **Artifacts** | `feature/artifacts/` (`ArtifactPreview.kt`, editor state), `data/artifacts/` | Do not create `ArtifactsScreen.kt`; artifact viewing/editing lives in `feature/artifacts/`. |
| **Auth** | `feature/auth/` (`SignInScreen`), `data/auth/` (`AuthRepository.kt`, token crypto) | Do not create a second sign-in screen or a parallel session store. |
| **Memory** | `feature/memory/`, `data/memory/` | Do not create a duplicate memory screen or a second memory store. |
| **Profile** | `feature/profile/` | Do not create `EditProfileScreen.kt`; profile editing lives in `feature/profile/`. |
| **Search** | `feature/search/` | Do not create a parallel local-search implementation. |
| **Settings** | `feature/settings/`, `feature/settings/appearance/`, `feature/settings/detail/` | Do not create duplicate settings screens; extend the existing settings packages. |
| **Workflows / Automations** | `feature/workflows/`, `data/workflows/` | Do not create a parallel workflow store or serializer. |
| **App Lock** | `data/applock/`, `app/AppLockGate.kt` | Do not create a second lock gate or a parallel lock store. |
| **Permissions** | `data/permissions/` | Do not create a parallel ask-before-acting store. |
| **Preferences** | `data/prefs/`, `design/theme/ThemePreferences.kt` | Do not create a second preferences store; theme choice belongs to `ThemePreferences.kt`. |
| **Design Tokens** | `design/theme/Color.kt`, `design/theme/Theme.kt`, `design/theme/Type.kt`, `design/theme/Shape.kt` | Do not create `OrhanThemeV2.kt` or a parallel token file. |
| **Brand Mark & Icons** | `design/components/OrhanCubeLogo.kt`, `design/icons/OrhanIcons.kt` | Do not create a second cube-logo composable or a parallel icon set. |
| **Launcher Icon & Brand Drawables** | `res/drawable/ic_launcher_foreground.xml`, `res/drawable/ic_launcher_background.xml`, `res/drawable-night/ic_launcher_{foreground,background}.xml`, `res/mipmap-*/`, `res/mipmap-night-*/`, `res/mipmap-anydpi-v26/` | Do not add a parallel launcher icon under a new name; replace the existing `ic_launcher` / `ic_launcher_round` resources in place. |
| **Cube Icon Source Set** | `design/icons/cube-42/` (`svg/cube_01..42.svg`, `compose/CubeIcons.kt`, `gen.py`, `svgs.json`) | Do not hand-edit generated `CubeIcons.kt` or the `res/drawable/ic_cube_*.xml` files; regenerate from `gen.py`. |

## 3. Clean Architecture & Unidirectional Data Flow (UDF) Rules

Every change in `app/` must respect strict separation of concerns across three layers:

1. **UI Layer (`design/screens/**`, `feature/**` composables):**
   - Contains **stateless `@Composable` functions** that read immutable UI state and emit user intents via lambda callbacks (`onSendMessage`, `onSelectModel`, `onOpenSettings`, etc.).
   - **Never perform network calls, file I/O, SQL/DataStore writes, or JSON parsing directly inside `@Composable` functions.**
   - **Route vs. Screen Separation:** route composables collect `StateFlow`s from the ViewModel and wire Android `ActivityResultLauncher`s; screen composables remain pure UI so JVM Compose UI tests can render them deterministically without a live backend.
2. **Presentation / State Layer (`feature/*/…ViewModel.kt`):**
   - `feature/chat/ChatViewModel.kt` owns chat state. Put new chat actions into the matching `feature/chat/` file rather than bloating the ViewModel or putting business logic inside a screen file.
   - Keep `StateFlow` exposure narrow; do not leak mutable state to the UI.
3. **Domain & Data Layer (`data/**`):**
   - Pure Kotlin models live beside their feature package.
   - Network streaming and REST calls go through the existing `data/chat/` repository (sharing one client) — never instantiate a new `HttpURLConnection`/`OkHttpClient` in UI or ViewModel code.
   - Local storage and persistence stay in their authoritative owners (`data/history/`, `data/prefs/`, `data/auth/`, `data/artifacts/`, `data/memory/`, `data/workflows/`).

## 4. Hard Stop Rules (Zero-Tolerance Guardrails)

### 4.1 Protected Paths
Do not edit, delete, or move these infrastructure paths unless explicitly instructed by the owner:
- `.github/workflows/` (`checks.yml`, `build-apk.yml`)
- `app/debug.keystore` — the committed AOSP debug key; rotating it breaks in-place APK updates
- `VERSION` — the `versionName` source of truth
- `gradle/libs.versions.toml` — the single version catalog
- `app/proguard-rules.pro` — reflective-entry-point keeps
- `licenses/` and `THIRD-PARTY-NOTICES.md` — bundled OSS licence texts copied into build assets

*(Note: all UI/theme files under `app/src/main/java/ai/orhan/design/` and `feature/` ARE permitted to be updated in-place when the owner requests UI/UX changes, provided all existing callbacks and test tags remain wired.)*

### 4.2 Zero Secrets in Code or Docs
- Never hardcode API keys, tokens, service-role keys, passwords, or personal emails in source files, tests, or documentation.
- Backend endpoints and keys are injected as `BuildConfig` fields from `local.properties` (or the environment) and are never committed: `DUA_ENGINE_URL`, `DUA_API_URL`, `SUPABASE_URL`, `SUPABASE_ANON_KEY`, and (debug only) `DUA_DEV_JWT`, `DUA_APP_TOKEN`.
- Never expose server secrets in Android builds, responses, logs, or tool output.

### 4.3 Strict Brand Ownership (`KRFbrothers / Orhan`)
- Never introduce third-party competitor logos, brand names (e.g., do not rename the app to "Teamily" or "Gemini"), or proprietary unlicensed font files as Orhan branding. All UI branding, the package name (`ai.orhan`), and user-facing copy must maintain the **KRFbrothers / Orhan** identity.
- When adapting UI/UX from a reference app (such as Teamily AI), adapt the **layout hierarchy, color tokens, typography scale, spacing, bottom sheets, and interaction smoothness** while preserving Orhan's own cube mark (`OrhanCubeLogo` in `design/components/OrhanCubeLogo.kt`) and the `ai.orhan` identity.
- The only permitted Teamily references are historical/evidence documents (`docs/verification/TEAMLY_PHASE_STATUS.md`) and the superseded assets kept for the before/after preview page (`design/preview-assets/before/`). Never add a new Teamily asset, and never let one reach `app/src/main/res/` or `design/images/`.

## 5. Android Design System, Smoothness & UI Test Contracts

### 5.1 Three-Layer Design Token Architecture

1. **Layer 1 (Palette):** Raw hex values (`Color(0x...)`) belong **ONLY** inside `design/theme/Color.kt` (or approved token files inside `design/theme/`).
2. **Layer 2 (Semantic Theme Roles):** Map palette colors to semantic Light and Dark roles in `design/theme/Color.kt` and `design/theme/Theme.kt`.
3. **Layer 3 (Component & Screen Consumption):** Screens and components must read colors exclusively via `OrhanTheme.colors` / `MaterialTheme.colorScheme`, never a raw literal.
4. **Typography & Spacing:** All `.sp` font sizes and `TextStyle` definitions belong **ONLY** in `design/theme/Type.kt`. Screens must use `MaterialTheme.typography` or named styles from `Type.kt`.
5. **Contrast Guardrails:** Both Light and Dark themes must satisfy WCAG AA contrast (`>= 4.5:1` for text, `>= 3.0:1` for non-text icons). The brand mint `#42D982` is a **fill only** — it is 1.62:1 on the light canvas and must never be used as text there.

### 5.2 Updating Value-Assertion Tests When the Owner Changes the UI
- **Structural tests (never weaken):** tests that enforce architectural hygiene or WCAG contrast must never be deleted or weakened.
- **Snapshot / value-assertion tests:** when the owner explicitly asks to update theme colors, typography, or screen layouts, existing value-assertion tests may fail because they check older hex values or old layout nodes. **Update the expected values in those test files to match the newly approved tokens/layouts — NEVER revert the owner's requested UI changes just to pass an old hardcoded assertion, and NEVER delete the test file.**

### 5.3 Smoothness & Recomposition Rules
- **State isolation during streaming:** pass only primitive or narrow arguments (draft string, typing boolean) into the composer and top bar so streaming deltas do not recompose the whole screen.
- **Memoize heavy parsing:** wrap Markdown escaping, source splitting, and artifact extraction in `remember(key)`.
- **LazyColumn keys & contentType:** always provide stable `key` and `contentType` parameters.
- **GPU-accelerated motion:** use `Modifier.graphicsLayer` for alpha, scale, and translation animations.
- **Shared networking:** always reuse the existing chat repository client; never instantiate a new HTTP client in UI or ViewModel code.

### 5.4 Mandatory Accessibility & Test-Tag Preservation
- Every interactive button, icon button, chip, or row must have at least a `48.dp` touch target and a meaningful `contentDescription` or semantics label.
- **NEVER remove or rename existing `Modifier.testTag(...)` strings** used by the JVM Compose UI test suite. If a test tag exists in the source, it must survive your edit.

## 6. Backend Contract Rules (`docs/BACKEND.md`)

1. **Chat transport:** `POST /api/chat` on the Dua AI engine, `Authorization: Bearer <Supabase user JWT>`, optional `X-Dua-App-Token`. Roles are `user` / `model`; the app sends the last 20 turns and always includes `"mode": "agent"`.
2. **Streaming:** the app first requests `"stream": true` with `Accept: text/event-stream` and consumes `text` / `error` / `done` frames, ignoring the rest. On a pre-text failure it retries once with `"stream": false`; a 401 is never retried (the session is cleared).
3. **Auth:** Supabase GoTrue REST (`/auth/v1/token`, `/auth/v1/logout`) with `SUPABASE_URL` + `SUPABASE_ANON_KEY` only. The session lives in app-private DataStore and the access token auto-refreshes when under 60 s from expiry.
4. **No new dependencies** for transport: `HttpURLConnection` + `org.json` on `Dispatchers.IO`.

## 7. Mandatory Pre-Completion Verification Checklist & Commands

Before finishing any slice or reporting completion to the owner, run the verification commands for the modified layer from the **repository root**:

### 7.1 Fast Static Guardrails (Run Before the Gradle Test Suite)
Check that no raw hex colors, raw `.sp` literals, or duplicate untracked files were introduced:
```sh
rg 'Color\(0x' app/src/main          # Must match ONLY design/theme/
rg '\.sp\b' app/src/main             # Must match ONLY design/theme/Type.kt
git status --short                   # Verify no duplicate *V2.kt / *New.kt files
git diff --check                     # Verify no whitespace or conflict markers
```

### 7.2 Android Build, Unit Tests & Lint
```sh
./gradlew :app:assembleDebug --no-daemon --max-workers=2
./gradlew :app:testDebugUnitTest --no-daemon --max-workers=2
./gradlew :app:lintDebug --no-daemon --max-workers=2
```
- `versionCode` is derived, not hardcoded: `git rev-list --count HEAD + 100`. A **shallow clone fails the configuration phase on purpose** — run `git fetch --unshallow` first. CI checks out with `fetch-depth: 0`.
- A release build needs a keystore (`ORHAN_KEYSTORE_PATH`, `ORHAN_KEYSTORE_PASSWORD`, `ORHAN_KEY_ALIAS`, `ORHAN_KEY_PASSWORD`); without it the build fails loudly. Pass `-PallowUnsignedRelease=true` for a compile-only check.

### 7.3 Instrumented Tests (Emulator Only)
The stock `connectedAndroidTest` task is **disabled** so an attached personal phone is never uninstalled. Use the emulator-guarded tasks instead:
```sh
./gradlew :app:installDebugOnEmulator
./gradlew :app:instrumentDebugOnEmulator
```

### 7.4 Honest Reporting
- If a build, lint run, unit test, or device install was **not** executed, say so explicitly. Never report a pass you have no run for.
- CI status is a separate, later fact: `checks.yml` runs on `pull_request` and on push to `main`, so a feature-branch push alone triggers nothing. Report it as **pending**, never green.
