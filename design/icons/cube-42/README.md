# Cube 42 — voxel icon pack, Orhan palette

42 isometric voxel icons, regenerated from a single parametric script so the whole
set shares one geometry and one palette. This is the pack that ships as
`app/src/main/res/drawable/ic_cube_*.xml`, and `Cube 05` from it is the app's
launcher icon.

## What's here

| Path | What it is |
|---|---|
| `svg/` | 42 standalone SVGs, `24×24`, `fill` only |
| `drawable/ic_cube_01.xml` … in `app/src/main/res/drawable/` | Same 42 as Android VectorDrawables, ready for `painterResource` |
| `compose/CubeIcons.kt` | 42 `ImageVector`s (`CubeIcons.Cube01` … `Cube42`) + `CubeIcons.all` |
| `svgs.json` | All 42 as inline SVG strings, for web / preview harnesses |
| `gen.py` | The generator. **This is the source of truth** — never hand-edit the output |
| `preview.png` | Contact sheet of all 42 |

## Palette

Every colour is a literal from `app/src/main/java/ai/orhan/design/theme/Color.kt`.
Nothing was invented.

| Role | Hex | Token | Faces |
|---|---|---|---|
| Body, lightest | `#4F6157` | `Ink600` | top |
| Body, mid | `#235039` | `DarkPrimarySoft` | left |
| Body, darkest | `#182B23` | `Ink900` | right |
| Accent, lightest | `#42D982` | `Mint` | top |
| Accent, mid | `#0B8249` | `Green750` | left |
| Accent, darkest | `#066A3B` | `Green800` | right |

Face order is preserved from the original pack — `t` is key-lit from above, `l`
mid, `r` in shadow — so the voxel read survives the recolour.

> **Baseline (replaced):** the pack originally shipped charcoal `#4D4D4D` /
> `#3A3A3A` / `#2C2C2C` with orange `#FF8A2B` / `#FF6A00` / `#D95400`. Both ramps
> are gone: `grep -ri` for any of those six values across `app/src/main/res`
> returns **0**. The orange clashed with the brand's green (`#087C45` primary,
> `#42D982` mint) and appeared nowhere else in the app.

## Regenerating

```bash
cd design/icons/cube-42
python3 gen.py          # writes svg/, drawable/, compose/CubeIcons.kt, svgs.json
cp drawable/ic_cube_*.xml ../../../app/src/main/res/drawable/
```

Change colours in the `COL` dict at the top of `gen.py`. Change geometry in the
`d(…)` calls above it — each entry is `d(number, gridSize, predicate, carveFaces)`.

## Using them in Compose

These icons are **fill-only and multi-colour**, so they must not be tinted:

```kotlin
Icon(
    imageVector = CubeIcons.Cube05,
    contentDescription = null,
    tint = Color.Unspecified,   // required — the default tint flattens all 6 colours
)
```

Or from resources, no Compose-vector round trip:

```kotlin
Image(
    painter = painterResource(R.drawable.ic_cube_05),
    contentDescription = null,
)
```

## Scope note

These are **decorative geometric marks**, not a functional icon set. All 42 are
abstract voxel shapes (shells, stairs, lattices, pillars) — there is no chat
bubble, search, mic, send or settings glyph among them, so they cannot replace
the app's line-icon system (`stroke: currentColor; fill: none; stroke-width: 1.9`).
Use them for branding, empty states and illustrations; keep the existing line
icons for interface affordances.
