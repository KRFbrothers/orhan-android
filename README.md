# Orhan AI — Android

Minimal Jetpack Compose + Material 3 app (`ai.orhan`). App aur design ab isi ek repo me hain.

## Design (live preview)
- `design/index.html` — full HTML design preview (open in browser)
- `design/m3-gallery.html` — Material 3 component gallery
- `design/code-pages/` — HTML viewers for the Kotlin theme/screens/icons
- `M3_AUDIT_REPORT.md` — Material 3 audit
- Compose source from the old design kit already lives under `app/` (not duplicated).
- Legacy: earlier the design lived in the separate repo
  [orhan-android-design-kit](https://github.com/KRFbrothers/orhan-android-design-kit) (to be archived); old Teamily preview URL is noted in `design/README.md`.

- `MainActivity` -> `OrhanAppSample()`: Home (chat list) <-> Chat, day/night toggle
- `design/theme` (Color/Type/Shape/Theme/FallbackFonts/ThemePreferences), `design/icons`, `design/screens`
- `feature/settings` (+ `appearance`) and `app/OrhanApp.kt` compiled in, ready for a NavHost (not wired to the launcher yet)
- Versions: `gradle/libs.versions.toml` from the kit (AGP 8.7.3, Gradle 8.9, Kotlin 2.0.21, Compose BOM 2024.12.01)

## Open / build
Android Studio -> Open -> pick this folder (JDK 17+). Or `./gradlew :app:assembleDebug`.
Debug APK without a local build: GitHub -> Actions -> **Build debug APK** -> Run workflow, then download the `Orhan-debug-APK` artifact.
The same APK is also published to GitHub Releases under the `debug-apk-latest` tag (always the newest debug build).

## Fonts
- Lato 400/700/900 in `res/font/lato_*.ttf` — official TTFs from google/fonts `ofl/lato` (OFL, `licenses/Lato-OFL.txt`).
- Inter variable (opt-in fallback) in `res/font/inter_*variable.ttf` from the kit (OFL, `licenses/Inter-OFL.txt`).

## Kit deltas
- `HomeScreen.kt` `OrhanAppSample`: `isSystemInDarkTheme()` hoisted out of the `rememberSaveable` lambda (didn't compile).
- `FallbackFonts.kt`: `@OptIn(ExperimentalTextApi::class)` for `FontVariation`.
