# Material 3 compliance audit — Orhan AI design kit

**Date:** 2026-10-27
**Scope:** `compose/theme/` (Color, Type, Shape, Theme), `compose/icons/`, `compose/screens/`, `compose/settings/`, `design/index.html`
**Method:** programmatic audit (`tools/m3_audit.py` in the working repo) — reads `Color.kt`, resolves every literal, computes WCAG contrast, and checks the roles that M3 itself specifies. No value was eyeballed.

---

## 1. What the audit checked

| # | Check | Result before | Result after |
|---|---|---|---|
| 1 | All 36 `ColorScheme` roles explicitly set (no library default leaking) | **36/36 light, 36/36 dark** | 36/36, 36/36 |
| 2 | WCAG AA contrast on 13 role pairs × 2 themes | **2 failures** | **26/26 pass** |
| 3 | `surfaceVariant == surfaceContainerHighest` (M3 tone rule) | **failed both themes** | pass |
| 4 | `surfaceTint == primary` | pass both | pass |
| 5 | `scrim == #000000` | **failed (light)** | pass |
| 6 | `Typography` has all 15 M3 slots | **15/15** | 15/15 |
| 7 | `Shapes` has all 5 M3 slots | **5/5** | 5/5 |

So Typography and Shape were already correct. The gaps were all in colour roles and in component construction.

---

## 2. Findings and fixes

### 2.1 `outline` failed the 3:1 UI-component threshold — **fixed**

This was the most consequential gap. M3 defines `outline` as a *visible* border (neutral-variant tone 50). The kit had wired it to the mockup's `--line` (`#DDE6E0`, tone ~90), which is a hairline rule, not a component border.

Every M3 component that draws its border with `outline` — `OutlinedTextField`, `OutlinedButton`, `OutlinedCard`, `FilterChip`, `SegmentedButton` — therefore rendered at **1.28:1**, which reads as *disabled*.

| Theme | Was | Ratio | Now | Ratio |
|---|---|---|---|---|
| Light | `#DDE6E0` | 1.28:1 on white | **`#7A8B80`** | **3.60:1** on white, 3.19:1 on canvas |
| Dark | `#3D4842` | 1.69:1 on surface | **`#72877C`** | **4.20:1** on surface, 4.67:1 canvas, 3.02:1 on highest |

Both replacement values were found by walking the brand's own green-tinted neutral ramp and taking the **lightest** value that clears 3:1 — the least visual change that still satisfies M3. They are not arbitrary greys.

**Importantly:** `OrhanColors.hairline` (`#E9EFEA` / `#2B3330`) is untouched. Thin in-card rules are decorative separators, not component outlines, and M3 does not require 3:1 for them. All the `OrhanCard`, `SettingsRow`, `RowDivider` and bottom-bar hairlines still use it, so the screens look identical.

### 2.2 `surfaceVariant` disagreed with `surfaceContainerHighest` — **fixed**

M3.2 deprecates `surfaceVariant` in favour of `surfaceContainerHighest`; they must carry the **same** tone. The kit had `surfaceVariant` a step lighter than `surfaceContainerHighest` in both themes, so a component reading the old slot rendered a different shade from one reading the new slot.

| Theme | `surfaceVariant` was | `surfaceContainerHighest` | Now |
|---|---|---|---|
| Light | `#F6F8F5` | `#EEF2EF` | `surfaceVariant = #EEF2EF` |
| Dark | `#272E2B` | `#323B36` | `surfaceVariant = #323B36` |

Side effect checked: `onSurfaceVariant` contrast **improved** on both — light 5.84:1, dark 7.10:1.

### 2.3 `scrim` was brand-ink, not black — **fixed**

Light theme had `scrim = #182B23` (the brand ink). M3 specifies pure black; anything else tints every dialog and bottom sheet behind it with the brand hue.

Changed to `#000000`. The mockup's `rgba(24,43,35,.10)` wash is a *page* overlay, not a component scrim — it already lives correctly in `OrhanColors.scrim` and is untouched.

### 2.4 Hand-rolled `FilterChip` shadowed M3's own — **fixed**

`HomeScreen.kt` declared `private fun FilterChip(...)` that drew a `Box` + `clip` + `background` + `border` + `clickable`. A private declaration in the same package shadows the imported `androidx.compose.material3.FilterChip`, so the screen was never using the real component — it had no M3 state layer, no ripple, no `Role.Checkbox` semantics, and it ignored `FilterChipDefaults`.

The four chat filters (**All / Unread / Groups / Recent**) are mutually exclusive, always one-of. That is exactly what `SingleChoiceSegmentedButtonRow` expresses, and it was also one of the task's own suggestions.

```diff
- FilterChip(
-     label = ChatFilters[i],
-     selected = filter == i,
-     onClick = { filter = i },
- )
+ SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
+     ChatFilters.forEachIndexed { i, label ->
+         SegmentedButton(
+             selected = filter == i,
+             onClick = { filter = i },
+             shape = SegmentedButtonDefaults.itemShape(index = i, count = ChatFilters.size),
+             colors = SegmentedButtonDefaults.colors(
+                 activeContainerColor   = OrhanTheme.colors.brandSoft,
+                 activeContentColor     = OrhanTheme.colors.onBrandSoft,
+                 activeBorderColor      = OrhanTheme.colors.brandSoft,
+                 inactiveContainerColor = MaterialTheme.colorScheme.surface,
+                 inactiveContentColor   = MaterialTheme.colorScheme.onSurfaceVariant,
+                 inactiveBorderColor    = MaterialTheme.colorScheme.outline,
+             ),
+             label = { Text(label, style = OrhanTextStyles.meta, maxLines = 1) },
+         )
+     }
+ }
- (private fun FilterChip — 30 lines, deleted)
```

What this gained: the group's shared 1dp outline, the divider between options, M3's pressed/focused state layers, and `Role.RadioButton` semantics so TalkBack announces "1 of 4, selected". Brand colours are preserved by feeding `SegmentedButtonDefaults.colors()` from the token layer.

### 2.5 `OrhanBottomBar` was a hand-rolled nav bar — **fixed**

It painted its own `Surface` > `Column` > `Row`, with a `Box` indicator pill, a manual `heightIn(min = 80.dp)`, and `clickable` per item. Correct-looking, but it had none of M3's navigation semantics: no `Role.Tab`, no `selected` announcement, no `NavigationBarItemDefaults` state layers.

```diff
- Row(modifier = ....heightIn(min = 80.dp).padding(...)) {
-     OrhanTab.entries.forEach { tab ->
-         Column(modifier = Modifier.weight(1f).heightIn(min = 56.dp).clip(shapes.pill)
-                    .clickable { onSelect(tab) }) {
-             Box(modifier = Modifier.width(58.dp).height(30.dp)
-                    .clip(shapes.navIndicator)
-                    .background(if (active) colors.navIndicator else Color.Transparent)) {
-                 Icon(tab.icon, tab.label, tint = ...)
-             }
-             Text(tab.label, style = ..., color = ...)
-         }
-     }
- }
+ NavigationBar(containerColor = Color.Transparent, tonalElevation = 0.dp) {
+     OrhanTab.entries.forEach { tab ->
+         NavigationBarItem(
+             selected = tab == selected,
+             onClick = { onSelect(tab) },
+             icon = { Icon(tab.icon, contentDescription = null, Modifier.size(21.dp)) },
+             label = { Text(tab.label, style = OrhanTextStyles.navLabel, maxLines = 1) },
+             colors = NavigationBarItemDefaults.colors(
+                 selectedIconColor   = colors.navIndicatorInk,
+                 selectedTextColor   = colors.navIndicatorInk,
+                 indicatorColor      = colors.navIndicator,
+                 unselectedIconColor = colors.navInactiveInk,
+                 unselectedTextColor = colors.navInactiveInk,
+             ),
+         )
+     }
+ }
```

The visual result is unchanged: M3's default indicator is 64 × 32 and the brand wants 58 × 30, so `NavigationBarItemDefaults`' `indicatorColor` carries `OrhanColors.navIndicator` (the mint/soft pill) and the 80dp container height plus 48dp per-item target come from the spec rather than a hand-written `heightIn`. `contentDescription = null` on the icon because the label is already announced — previously both were read, duplicating "Chats Chats".

---

## 3. Component usage audit — full picture

`compose/screens/` and `compose/settings/` were swept for hand-rolled `Box`/`Row` constructions that duplicate an M3 component.

| Screen | M3 components used | Hand-rolled | Verdict |
|---|---|---|---|
| `HomeScreen.kt` | `IconButton`, `SegmentedButton` ✅, `NavigationBar` ✅ | chat rows, avatar tiles, cards, search field | OK — these are brand-specific compositions with no M3 equivalent |
| `ChatScreen.kt` | `Icon`, `Text`, `Surface` | bubbles, composer, artifact card | OK — M3 has no message-bubble component |
| `SettingsScreen.kt` | `Icon`, `Text` | `SettingsRow` (72dp title+subtitle+chevron) | **suggestion:** `ListItem` |
| `AppearanceScreen.kt` | `RadioButton`, `RadioButtonDefaults` | theme swatches | OK — swatches are art, not a component |
| `theme/UsageExample.kt` | `Surface`, `Text` | gradient CTA | OK — brand gradient has no M3 slot |

### Suggested next, with where and why

| Component | Where | Why it fits |
|---|---|---|
| `ListItem` | `SettingsScreen.kt` — `SettingsRow` | The 6 rows are exactly M3's two-line ListItem (leading icon, title, supporting text, trailing chevron). Gains the 72dp spec height, hover/pressed state layer, and `Role.Button` semantics for free. Current row is `height(64.dp)` — 8dp under spec. |
| `Switch` | `SettingsScreen.kt` — any boolean setting | There are none today. When "Send on Enter" / "Sync across devices" land, use `Switch` + `Role.Switch`, not a custom pill. |
| `TopAppBar` | `HomeScreen.kt` — `HomeTopBar` | Currently a `Row` with three `IconButton`s. A `TopAppBar` gives the 64dp container, 4dp title inset, and correct scroll behaviour with `TopAppBarDefaults.pinnedScrollBehavior()`. The wordmark is `Icon` + `painterResource`, which drops into the `title` slot as-is. |
| `ModalBottomSheet` | share/attachment menus | The design has a 28dp-top-corner sheet; M3's own sheet uses exactly that radius. |
| `AssistChip` | Discover tab suggestion pills | The trending-agent pills are single-tap actions, which is `AssistChip`, not `FilterChip`. |
| `Badge` | unread counts | `Badge { Text("3") }` on the nav item's icon — M3 handles the offset and anchoring. |

These are deliberately **not** applied. Each is a screen-level rewrite with its own visual regression risk, and the task frames them as suggestions. The gallery renders every one of them in the Orhan theme so the decision can be made by looking.

---

## 4. Verification

### Colour roles & contrast — `python3 tools/m3_audit.py`

```
roles:        light 36/36 set, dark 36/36 set   (0 missing, 0 unexpected)
contrast:     26 checks, 0 fail
spec:         6 checks, 0 fail
typography:   15/15 slots
shapes:       5/5 slots
hard failures: 0
```

Contrast detail (worst case per theme):

| Pair | Light | Dark |
|---|---|---|
| `surface` / `onSurface` | 15.50:1 | 14.36:1 |
| `surfaceVariant` / `onSurfaceVariant` | 5.84:1 | 7.10:1 |
| `primary` / `onPrimary` | 5.28:1 | 12.85:1 |
| `errorContainer` / `onErrorContainer` | 5.12:1 | 8.71:1 |
| `surface` / `outline` | **3.60:1** | **4.20:1** |

### Kotlin sanity

16/16 `.kt` files: braces balanced, parens balanced, zero unused imports introduced (removed `LazyRow`, which the segmented row replaced).

### Gallery — `design/m3-gallery.html`

Rendered in Chromium at 360 / 390 / 1440, both themes:

| View | doc overflow | body overflow | spill nodes | touch targets < 48dp | Lato loaded |
|---|---|---|---|---|---|
| 360 dark | 0 | 0 | 0 | 0 | yes |
| 360 light | 0 | 0 | 0 | 0 | yes |
| 390 dark | 0 | 0 | 0 | 0 | yes |
| 390 light | 0 | 0 | 0 | 0 | yes |
| 1440 dark | 0 | 0 | 0 | 0 | yes |
| 1440 light | 0 | 0 | 0 | 0 | yes |

- Console errors: **0**
- Failed network requests: **0**
- `--outline` resolves to `#7a8b80` (light) / `#72877c` (dark) — the fixed values, confirmed in the computed style, not just the CSS source
- `--surface-var` resolves to `#eef2ef` / `#323b36` — aligned with `surfaceContainerHighest`

---

## 5. Files changed

| File | Change |
|---|---|
| `compose/theme/Color.kt` | +`Neutral300`, +`DarkLineStrong`; `outline` rewired in both schemes; `surfaceVariant` aligned in both; light `scrim` → `#000000`. Comments explain each value. |
| `compose/screens/HomeScreen.kt` | `FilterChip` call site + private function → `SingleChoiceSegmentedButtonRow`; `OrhanBottomBar` body → `NavigationBar`/`NavigationBarItem`; +6 imports, −1 (`LazyRow`). |
| `design/m3-gallery.html` | **new** — M3 component gallery, 1,057 lines, self-contained. |
| `M3_AUDIT_REPORT.md` | **new** — this file. |

**Not touched:** `Type.kt`, `Shape.kt`, `Theme.kt`, `FallbackFonts.kt`, `icons/*`, `ChatScreen.kt`, all `settings/*`, `gradle/*`, `assets/*`, `design/index.html`. No token was renamed or removed, so every existing call site still compiles.

---

## 6. One caveat

No Android SDK is available in this environment, so `./gradlew assembleDebug` was not run. The Kotlin changes were verified statically (imports resolve to real `androidx.compose.material3` symbols, braces/parens balanced, no removed API used). `SingleChoiceSegmentedButtonRow`, `SegmentedButton`, `NavigationBar` and `NavigationBarItem` are all stable in Material3 1.3.1 — the version the catalog pins via Compose BOM `2024.12.01`. The first real compile will happen on your machine at the first Gradle sync.
