# Orhan AI — Android

Orhan is an Android AI chat app (`ai.orhan`), built with Jetpack Compose + Material 3.
The app code and the HTML design live together in this repo.

## Design preview (live)
- **https://krfbrothers.github.io/orhan-android/** — 14-screen HTML design
- **https://krfbrothers.github.io/orhan-android/m3-gallery.html** — Material 3 component gallery
- Source: `design/` (`index.html`, `m3-gallery.html`, `code-pages/`), audit in `M3_AUDIT_REPORT.md`

## How it works
- `MainActivity` → `OrhanNavHost`: Home (chats), Chat, Agents, Discover, Profile, Settings
  (Appearance, Notifications, Privacy, Chat prefs, Storage, Language, About), plus Memory,
  Artifacts, Workflows and Sign-in.
- **Agent mode is always on.** Chat replies stream from the shared Dua engine
  (`DUA_ENGINE_URL`, a Cloud Run service also used by the Dua app).
  Orhan can hand a task off to one of your own agents.
- **Sign-in** uses Supabase auth. `SUPABASE_URL` / `SUPABASE_ANON_KEY` come from
  `local.properties` locally, or from GitHub Actions secrets in CI. Without them the app
  builds, but sign-in will not work. Never commit keys.
- Chats, agents, artifacts, memory, workflows, profile and settings are stored on device (DataStore/files).

## Build and run
- Android Studio → Open this folder (JDK 17+), or `./gradlew :app:assembleDebug`.
- Optional `local.properties`:
  ```
  SUPABASE_URL=...
  SUPABASE_ANON_KEY=...
  DUA_ENGINE_URL=...   # optional, has a default
  ```
- Tests: `./gradlew :app:testDebugUnitTest`.

## Debug APK
- Latest build: [Releases → `debug-apk-latest`](https://github.com/KRFbrothers/orhan-android/releases/tag/debug-apk-latest)
- Or Actions → **Build debug APK** → Run workflow → download the `Orhan-debug-APK` artifact.
- These are debug builds for testing, not Play Store releases.

## Status
Early / in active development. On `main` today:

**Works**
- Chat streams replies (always agent mode). Orhan knows its name and says it runs on Argon 4 when asked.
- Attach files, mic, and hold-to-record voice notes. Attachments survive an app restart.
- Agents: create, list, delete. Tapping an agent card opens a chat with it.
- Orhan hub on Home. Orhan can hand a task off to one of your agents.
- Notification and language settings are saved.
- Memory, artifacts, workflows, pinned chats, chat prefs, profile, appearance, sign-in (Supabase).
- Live design pages (link above) and a debug APK (see Debug APK).

**Not done / limited**
- Tool chips in Create Agent only guide the model; the engine still gets the full tool set.
- Storage: "Cached previews", "Old chat media" and "Agent logs" show "Not available yet".
- No passcode / two-step lock. Open-source licences: Settings → About → Open-source licences (bundled from `THIRD-PARTY-NOTICES.md` + `licenses/`).
- A handed-off agent's reply does not stream, and Stop can't cancel it mid-reply.
- Some unit tests for chat features; no UI tests.

## Fonts and licenses
- Lato 400/700/900 (`res/font/lato_*.ttf`, OFL, `licenses/Lato-OFL.txt`)
- Inter variable as opt-in fallback (OFL, `licenses/Inter-OFL.txt`)
- See `THIRD-PARTY-NOTICES.md`.
