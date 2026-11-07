# Orhan — Android

Orhan is a private, owner-locked multimodal GenAI assistant for Android. Single-activity
Jetpack Compose app on Material 3, talking to the Dua AI engine over SSE.

| | |
|---|---|
| Package | `ai.orhan` |
| Version | `0.2.0` (from the `VERSION` file) |
| License | Apache-2.0 |
| SDK | min 26 · target 35 · compile 35 |

## Repository layout

```
orhan-android/
├── app/                          # the only Gradle module (:app)
│   ├── build.gradle.kts
│   ├── debug.keystore            # committed AOSP debug key — see Signing
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/java/ai/orhan/
│       │   ├── MainActivity.kt
│       │   ├── app/              # OrhanApp.kt, OrhanNavHost.kt, AppLockGate.kt
│       │   ├── design/           # theme/, screens/, components/, icons/
│       │   ├── feature/          # chat, agents, artifacts, auth, discover,
│       │   │                     #   memory, profile, search, settings, workflows
│       │   └── data/             # agents, applock, artifacts, auth, chat, history,
│       │                         #   memory, permissions, prefs, workflows
│       ├── main/res/             # values/, drawable/, drawable-night/,
│       │                         #   mipmap-*/ (+ mipmap-night-*), font/, xml/
│       ├── test/                 # 39 JVM unit-test files
│       └── androidTest/          # 2 instrumented test files
├── design/                       # self-contained HTML design showcase + brand assets
├── docs/                         # BACKEND.md, verification/
├── gradle/libs.versions.toml     # version catalog — single source of truth
├── licenses/                     # bundled OSS licence texts
├── VERSION                       # versionName source of truth
└── .github/workflows/            # checks.yml, build-apk.yml
```

## Toolchain

| Component | Version |
|---|---|
| Android Gradle Plugin | 8.7.3 |
| Gradle | 8.9 |
| Kotlin | 2.0.21 (K2) |
| Compose BOM | 2024.12.01 → Compose UI 1.7.6, Material3 1.3.1 |
| JDK / JVM target | 17 |
| compileSdk / targetSdk | 35 |
| minSdk | 26 |

Every version lives in `gradle/libs.versions.toml`; nothing is pinned inline in a build script.

## Build

Prerequisites: JDK 17, Android SDK Platform 35 and Build-Tools 35.0.0.

```bash
# Debug APK
./gradlew :app:assembleDebug

# JVM unit tests
./gradlew :app:testDebugUnitTest

# Android Lint
./gradlew :app:lintDebug

# Release (R8 + resource shrinking) — needs a release keystore, see Signing
./gradlew :app:assembleRelease
```

`versionCode` is derived, not hardcoded: `git rev-list --count HEAD + 100`. A shallow clone
therefore fails the configuration phase on purpose rather than guessing a number — run
`git fetch --unshallow` first. CI checks out with `fetch-depth: 0`.

## Configuration

Backend endpoints are injected as `BuildConfig` fields from `local.properties` (or the
environment) and are never committed:

| Property | Purpose |
|---|---|
| `DUA_ENGINE_URL` | Dua AI engine — streaming chat (`POST /api/chat`) |
| `DUA_API_URL` | Dua API — reserved for history / accounts |
| `SUPABASE_URL` | Supabase project URL (auth) |
| `SUPABASE_ANON_KEY` | Supabase anon key |
| `DUA_DEV_JWT`, `DUA_APP_TOKEN` | debug builds only — never baked into release |

Override them for a local or custom build without editing tracked files:

```bash
./gradlew :app:assembleDebug \
  -PDUA_ENGINE_URL="https://your-engine.example.com" \
  -PDUA_API_URL="https://your-api.example.com"
```

## Signing

- **Debug** — `app/debug.keystore` is committed with the standard AOSP debug credentials, so
  local and CI builds share one certificate and `adb install -r` works across them.
  Certificate SHA-256: `8D:E8:C2:E4:…:E0:6E`.
- **Release** — never committed. Supply `ORHAN_KEYSTORE_PATH`, `ORHAN_KEYSTORE_PASSWORD`,
  `ORHAN_KEY_ALIAS` and `ORHAN_KEY_PASSWORD` via `local.properties` or the environment. A
  release build without them fails loudly; pass `-PallowUnsignedRelease=true` for a
  compile-only check.

## Testing

- **JVM unit tests** — 39 files under `app/src/test/` (195 `@Test` methods) covering data
  stores, chat request shape, SSE parsing, memory, workflows and profile serialization.
- **Instrumented tests** — 2 files under `app/src/androidTest/` (artifact UI + sharing). The
  stock `connectedAndroidTest` task is disabled so an attached personal phone is never
  uninstalled; use the emulator-guarded tasks instead:

```bash
./gradlew :app:installDebugOnEmulator
./gradlew :app:instrumentDebugOnEmulator
```

## CI

| Workflow | Trigger | What it does |
|---|---|---|
| `.github/workflows/checks.yml` | PR, push to `main`, manual | `assembleDebug` + `assembleRelease` (R8 check) + `lintDebug` + `testDebugUnitTest`; verifies R8 ran and the unused material icons were stripped |
| `.github/workflows/build-apk.yml` | manual dispatch | Builds a signed debug APK, verifies its certificate and version against the repo, publishes it to the `debug-apk-latest` release |

## Design showcase

`design/index.html` is a self-contained 14-screen HTML mockup — the Lato fonts, brand marks
and avatars ship in `design/fonts/` and `design/images/`, so it renders with no network access
and no build step. It is published to GitHub Pages from the `gh-pages` branch:
<https://krfbrothers.github.io/orhan-android/>

Brand tokens (from `app/src/main/java/ai/orhan/design/theme/Color.kt`): primary `#087c45`, logo mint `#42d982`,
dark canvas `#141816`, light canvas `#eef2ef`.

## Documentation

- `docs/BACKEND.md` — Dua engine endpoints, chat request/response contract, SSE frames
- `docs/verification/TEAMLY_PHASE_STATUS.md` — phase status
- `design/README.md` — the HTML showcase

## License

Apache License 2.0 — see [`LICENSE`](LICENSE). Third-party notices are in
[`THIRD-PARTY-NOTICES.md`](THIRD-PARTY-NOTICES.md) and `licenses/`.
