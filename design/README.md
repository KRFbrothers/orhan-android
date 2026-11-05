# design/ — the HTML showcase

Open `index.html` in a browser. It is fully self-contained: the Lato fonts, the Orhan brand marks and
every agent/user avatar ship in `fonts/` and `images/`, so it renders with no network access and no build
step.

## What the page contains

| # | Screen | Key content |
|---|---|---|
| 1 | Home (Chats) | Avatar (opens Profile), Orhan mark + sync status, new-chat plus, 4 filter chips, Pinned / Chats collapsible groups, Quick actions sheet, bottom nav |
| 2 | Chat | Agent top bar, tailed bubbles, inline artifact card, typing indicator, composer |
| 3 | Agents | Agent grid with presence dots and capability tags |
| 4 | Discover | Featured agent hero + trend grid |
| 4a | Memory | What the assistant remembers, grouped by topic, with attach / recall actions. Memory nav tab carries a dot badge when new memories land |
| 4b | Artifacts | Everything the agents produced — pages, decks, files — with type, size and the agent that made it |
| 5 | Profile | Pushed route with back arrow, identity card, 3-up stat row, quick actions, account section. **No bottom nav** (matches `HomeScreen.kt:397`) |
| 6 | Settings | Grouped rows -> Appearance, Notifications, Privacy, Chat, Storage, Language, About |
| 7 | Appearance | System / Night / Day radio cards with live previews |
| 8 | Edit profile | Avatar picker, name / email / handle / bio fields |
| 9 | Notifications | Per-category switches |
| 10 | Privacy & security | App lock, two-step verification, read receipts, blocked users |
| 11 | Chat preferences | Send-with-Enter, hold-to-record, typing indicators, activity cards |
| 12 | Storage | 4-segment usage bar, legend, clean-up rows |
| 13 | Language | App language + reply language |
| 14 | About | Version, legal rows, support, sign out |

The page renders one frame and switches every screen between **Day** and **Night** with the page-level
System / Day / Night control, so each screen is visible in both themes.

## Bottom navigation

**5 destinations** — Chats, Agents, Discover, Memory, Artifacts — exactly matching the `OrhanTab` enum in
`app/.../screens/HomeScreen.kt:141`.

**Profile is not a nav destination.** It opens from the header avatar and is a pushed route, and the
bottom bar is hidden while it is open (`HomeScreen.kt:397`). The eight Settings sub-screens and Chat are
also pushed routes with a back arrow and no bottom bar.

(An earlier version of this file said 4 destinations including Profile. That was wrong — corrected
against the shipped Kotlin, not against the mockup.)

## Interactions that actually work

- Bottom nav switches between the five tabs and moves the active indicator pill
- The header avatar opens Profile; Back returns to Chats
- Pinned / Chats headers collapse and expand
- The FAB opens the Quick actions sheet (7 automation templates from `AutomationTemplates.kt`)
- The screen rail (desktop) jumps to any of the 14 screens
- Tapping a chat row opens Chat; the back arrow returns to the list
- Theme control flips Day / Night / System and persists the choice to `localStorage`
- Settings -> Appearance -> Day/Night actually repaints the whole page
- Switches toggle, radio rows select, send appends a message, empty send is a no-op

## Brand tokens

Read from the Orhan design tokens (`design/theme/Color.kt`), not approximated:

| Token | Value |
|---|---|
| Primary | `#087c45` |
| Primary gradient | `180deg #0b8249 -> #087c45` |
| Logo mint | `#42d982` |
| Brand soft | `#cbe7d2` |
| Dark canvas | `#141816` |
| Light canvas | `#eef2ef` |
| Ink | `#182b23` |
| User bubble (night) | `#505752` |
| Agent bubble (night) | `#303433` |
| Typeface | Lato 400 / 700 / 900 (self-hosted) |

**Mint `#42d982` is a fill only.** It is 1.62:1 against the light canvas, so it must never be used as
text there — badges, dots, presence rings and the logo mark only.

## Published URLs

GitHub Pages serves this folder from the `gh-pages` branch:

- **https://krfbrothers.github.io/orhan-android/** — 14-screen HTML design

`gh-pages` is published separately from `main`; updating `design/` here does not republish the site by
itself. Confirm the Pages branch in the repository settings before relying on the URL.
