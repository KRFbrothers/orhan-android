# Third-Party Notices

Every third-party asset in this repository, with its origin, license, and the exact path it came from.

**Scope:** only assets that are (a) license-safe for redistribution and (b) actually usable by the
Orhan Android app. Nothing else was taken. See [What was deliberately left out](#what-was-deliberately-left-out)
for the assets that were rejected and why.

**Source repository:** `KRFbrothers/Dua-OG-Assets-` (commit `1a329b8`, 21 Oct 2026).
That repo is being made private; the files below are the only ones this project depends on.

---

## 1. Inter — SIL Open Font License 1.1

| | |
|---|---|
| **Files** | `assets/fonts/inter/Inter[opsz,wght].ttf` (876 KB)<br>`assets/fonts/inter/Inter-Italic[opsz,wght].ttf` (907 KB) |
| **License** | SIL Open Font License, Version 1.1 |
| **License text** | `assets/fonts/inter/OFL.txt` |
| **Copyright** | Copyright 2020 The Inter Project Authors (https://github.com/rsms/inter) |
| **Original path** | `fonts/google-fonts/ofl/inter/` |
| **Upstream** | https://github.com/rsms/inter @ `66647c0bbbe41a850d79d9c76fb13add3378940f` |
| **Provenance** | `assets/fonts/inter/METADATA.pb`, `assets/fonts/inter/upstream_info.md` |

**Why it is here:** Orhan ships **Lato** as its brand typeface. Inter is kept as a
**fallback / alternate** only — it is not wired into `Type.kt` and does not change the brand.
Inter is a variable font (`opsz`, `wght` axes), so a single file covers the whole weight range.

**OFL obligations if you redistribute:** keep `OFL.txt` alongside the font, do not sell the font
on its own, and do not use the reserved font name "Inter" for a modified version.

---

## 2. Material Symbols — Apache License 2.0

| | |
|---|---|
| **Files** | `assets/icons/material-symbols/*.xml` — 10 vector drawables |
| **License** | Apache License, Version 2.0 |
| **License text** | `assets/notices/material-symbols-LICENSE.txt` |
| **Copyright** | Copyright Google LLC |
| **Original path** | `icons/material-symbols/symbols/android/<name>/materialsymbolsrounded/<name>_24px.xml` |
| **Upstream** | https://github.com/google/material-design-icons |

### The 10 icons, and where Orhan uses them

Each file is the **Rounded** variant at 24dp, matching the `Icons.Rounded.*` calls already in
`compose/screens/` and `compose/settings/`.

| File | Orhan usage |
|---|---|
| `search_24px.xml` | Home top bar search, Discover search |
| `arrow_back_24px.xml` | Chat / Settings / Appearance back button |
| `add_24px.xml` | Home FAB (new chat) |
| `smart_toy_24px.xml` | Agent avatar + Agents tab |
| `person_24px.xml` | Profile tab, people rows |
| `more_vert_24px.xml` | Chat top bar overflow |
| `mic_24px.xml` | Chat composer voice button |
| `menu_24px.xml` | Home top bar menu |
| `description_24px.xml` | Artifact / document card |
| `attach_file_24px.xml` | Chat composer attach button |

**Why only 10 of 53:** the source set has 53 symbol names. These 10 are the ones that appear in
Orhan's Kotlin as `Icons.Rounded.*` **and** exist in the source set. The other 7 icons Orhan
uses (`DarkMode`, `LightMode`, `Explore`, `Star`, `Chat`, `ChevronRight`, `KeyboardArrowRight`)
are **not** in the source set — they come from `material-icons-extended`, which is already a
declared dependency in `gradle/libs.versions.toml`. Nothing is missing.

**Note:** these XMLs carry `android:tint="?attr/colorControlNormal"`. If you drop them into
`res/drawable/`, either keep that tint or override it with `ColorFilter` at the call site so they
follow Orhan's `colors.onSurface` / `colors.brand` tokens.

---

## What was deliberately left out

Rejected on purpose. Each one is a real risk, not an oversight.

### Telegram shaders — GPL-2.0, and not needed

`shaders/telegram/` (36 files, 228 KB) is **GNU GPL v2** (`notices/Telegram-LICENSE`).
GPL-2.0 is **copyleft**: linking GPL shader code into a closed-source app would oblige you to
release the app's source. That is a licensing decision for the whole product, not an asset swap.

It is also **not needed**. Orhan's glass effect is CSS `backdrop-filter: blur(18px) saturate(1.2)`
on the bottom nav (`design/index.html:318`) — a web-only effect. On Android the equivalent is
Compose's `Modifier.blur()` or a translucent `Surface`, neither of which needs a GLSL shader.
Adding 36 GPL files to solve a problem that does not exist would be pure liability.

**If you ever do want real glass on Android:** write it yourself against
`android.graphics.RuntimeShader` (AGSL, API 33+). The `liquid_glass_shader.agsl` in the source
repo is a useful *reference for the maths* — read it, do not copy it.

### DuaSparkle.tsx — Google-branded gradient, and Orhan already has its own

`frontend/components/DuaSparkle.tsx` (64 lines) is a React + framer-motion component. Two problems:

1. **It is React.** Orhan is Jetpack Compose. It would need a full rewrite, not a copy.
2. **Its `fourColor` variant is the Google logo palette** — `#EA4335`, `#FBBC04`, `#34A853`,
   `#4285F4`. Shipping that as an AI icon is Google brand art. The `aurora` variant
   (`#4285F4` → `#9B72CB` → `#D96570`) is the Gemini gradient — also Google's.

Orhan **already has its own sparkle**: the `#i-spark` SVG symbol at `design/index.html:824`,
used 17 times across the screens for agent badges and message ticks. It is original to this
project and already on-brand. There is nothing to gain here.

### Proprietary fonts — not redistributable

Present in the source repo, **not** taken:

| Font | Owner | Why rejected |
|---|---|---|
| `ggsans*`, `ggmono*` | Discord | Proprietary, APK-extracted |
| `ABCGintoDiscordNord*` | Dinamo / Discord | Commercial license |
| `duolingosans*` | Duolingo | Proprietary |
| `din_next_for_duolingo*` | Monotype / Duolingo | Commercial license |

**No Orhan screen references any of these.** `Type.kt` uses Lato only
(`R.font.lato_regular`, `lato_bold`, `lato_black`), and `design/index.html` declares
`font-family: 'Lato'` exclusively. So there is no gap to fill and no OFL substitute is needed.

If a future screen ever wants a geometric grotesque in that spirit, **Inter** (already in
`assets/fonts/inter/`) is the safe OFL answer — which is the other reason it is here.

### Brand-named folders — trademark art

Not taken: `airbnb`, `chrome`, `duolingo`, `google-app`, `google-fit`, `google-photos`,
`google-play`, `gmail-archive`, `oppo-search`, `telegram`, `digital-wellbeing`.

These contain third-party logos and product art. Redistributing them in a Orhan repo invites
trademark claims and has no design value — Orhan has its own block-T mark in `design/images/`.

### Everything else

`code/` (11,319 files, 195 MB — 16 vendored open-source repos), `animations/` (1,488 Lottie/Rive
files), `frontend/` (the Dua React app), `backend/` (contains a committed `.env.local` with a
live secret — do not copy that file anywhere), and the remaining 43 Material Symbols.

None of it is referenced by Orhan's design or Compose code.

---

## Summary

| Category | Files | Size | License |
|---|---|---|---|
| Inter font | 5 | 1.8 MB | OFL 1.1 |
| Material Symbols | 10 | 10 KB | Apache 2.0 |
| License texts | 2 | 12 KB | — |
| **Total** | **17** | **~1.8 MB** | all redistributable |

No GPL, no proprietary fonts, no third-party logos, no secrets.
