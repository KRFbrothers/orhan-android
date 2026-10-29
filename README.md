# design/ - the 14-screen HTML showcase

Open `index.html` in a browser. It is fully self-contained: the Lato fonts, the Orhan brand marks and
every agent/user avatar ship in `fonts/` and `images/`, so it renders with no network access and no build
step.

Also live at (Orhan preview, hosted on legacy URL):
**https://static.teamily.ai/sites/a92f0e27-0c1b-40b9-a4fc-6c4aba860bbf/webpages/teamily-android-app_v4/index.html**

## What the page contains

| # | Screen | Key content |
|---|---|---|
| 1 | Home (Chats) | Wordmark top bar, search, 4 filter chips, 7 chat rows, FAB, bottom nav |
| 2 | Chat | Agent top bar, tailed bubbles, inline artifact card, typing indicator, composer |
| 3 | Agents | Agent grid with live dots and capability tags |
| 4 | Discover | Community / template shelf |
| 5 | Profile | Identity card, stat row, settings entry |
| 6 | Settings | Grouped rows -> Appearance, Notifications, Privacy, Chat, Storage, Language, About |
| 7 | Appearance | System / Light / Dark radio rows with live swatches |
| 8 | Edit profile | Avatar picker, name/about fields |
| 9 | Notifications | Per-category switches |
| 10 | Privacy | App lock, two-step verification, read receipts |
| 11 | Chat preferences | Send-with-Enter, hold-to-record, preview-before-send |
| 12 | Storage | On-device counts (documents, saved chats), local-only note, clean-up placeholders |
| 13 | Language | App language + reply language |
| 14 | About | Version, legal, sign out |

Every screen is rendered twice: a day frame and a night frame.

## Interactions that actually work

- Bottom nav switches between the four tabs and moves the indicator pill
- Tapping a chat row opens Chat; the back arrow returns to the list
- Theme toggle flips day/night and persists the choice to `localStorage`
- Settings -> Appearance -> Light/Dark actually repaints the whole page
- Switches toggle, radio rows select, send appends a message, empty send is a no-op

## Verified on this page

Checked at 360 / 390 / 1440 px in both day and night:

- horizontal overflow **0**, spill **0**
- touch targets below 48x48 **0**
- contrast AA failures **0** (0 of 361 text nodes)
- failed asset requests **0**
- console errors **0**
- bottom nav and composer flush to the frame (gap 0.0)

## One implementation note worth keeping

Below a 400 px container width the Chat screen hides the video-call button so the agent name never
truncates. That rule is scoped with `@container phone (max-width: 400px)` and `container-type: inline-size`
on `.screen`, **not** a viewport media query. Inside a 390 px phone frame on a 1440 px desktop preview a
viewport query never fires, which is exactly how the title lost 45 px before the fix.
