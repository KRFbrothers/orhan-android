# Orhan AI — Android

Minimal Jetpack Compose + Material 3 app (`ai.orhan`) wired from
[KRFbrothers/orhan-android-design-kit](https://github.com/KRFbrothers/orhan-android-design-kit).

- `MainActivity` -> `OrhanAppSample()`: Home (chat list) <-> Chat, day/night toggle
- `design/theme` (Color/Type/Shape/Theme/FallbackFonts/ThemePreferences), `design/icons`, `design/screens`
- `feature/settings` (+ `appearance`) and `app/OrhanApp.kt` compiled in, ready for a NavHost (not wired to the launcher yet)
- Versions: `gradle/libs.versions.toml` from the kit (AGP 8.7.3, Gradle 8.9, Kotlin 2.0.21, Compose BOM 2024.12.01)

## Open / build
Android Studio -> Open -> pick this folder (JDK 17+). Or `./gradlew :app:assembleDebug`.

## Fonts
- Lato 400/700/900 in `res/font/lato_*.ttf` — official TTFs from google/fonts `ofl/lato` (OFL, `licenses/Lato-OFL.txt`).
- Inter variable (opt-in fallback) in `res/font/inter_*variable.ttf` from the kit (OFL, `licenses/Inter-OFL.txt`).

## Kit deltas
- `HomeScreen.kt` `OrhanAppSample`: `isSystemInDarkTheme()` hoisted out of the `rememberSaveable` lambda (didn't compile).
- `FallbackFonts.kt`: `@OptIn(ExperimentalTextApi::class)` for `FontVariation`.
