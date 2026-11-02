# Artifacts layout and persistence verification — 2026-11-02

Active stage: **2, Artifacts**. This is a verification-only continuation of
[PR #93](https://github.com/KRFbrothers/orhan-android/pull/93), not a claim of
complete feature parity. No application, Memory, auth, signing or version code
was changed in this slice.

## Starting state and ownership

The working tree was clean on `feat/artifacts-save`. Local HEAD and the live
PR head were both `a77a92e6c31002c4f1270e092d20ab2b5122d73d`. PR #93 was open,
unmerged and mergeable, targeting `feat/memory-actions` (`b540f17`). Its body
still described the older `3ac2ecd` build. Existing work was inspected and reused:

| Flow | Canonical owner | State before this slice / remaining dependency |
| --- | --- | --- |
| Create, save, edit, delete, search | `ArtifactStore`, `ArtifactsScreen` | `3ac2ecd` awaits writes and preserves failed drafts; no duplicate implementation needed |
| Compact / large text preview | `ArtifactListPreferences`, `ArtifactsScreen` | Implemented by `a77a92e`; device verification needed |
| Detail and text sharing | `ArtifactDetail` in `ArtifactsScreen` | Android `ACTION_SEND`, `text/plain`; receiver delivery is outside this check |
| Memory → My Artifacts | `MemoryScreen`, existing artifact navigation | Canonical detail route already wired; prior PR evidence retained |
| Chat reply → artifact | `ChatScreen`, `OrhanNavHost` | Save callback wired; successful live reply requires usable Orhan authentication |
| Web, Markdown, media, saved messages | No verified typed model yet | Reference samples/formats and supported creation/export behavior still needed |

Memory's pause/automatic consent store and artifact-id links were inspected.
The previous Memory slices were preserved. Their outstanding reference/service
gaps remain outstanding; this check does not reverify Memory parity.

## Build and device identity

- REA inventory actually exposed ADB discovery, device/package inspection and
  APK pull tools. Local ADB performed taps/typing; arbitrary interaction was
  not attributed to REA.
- Connected serial `9cefa3cb`, model RMX5051, Android 16 / API 36. REA reported
  transport/kind unknown, so this report does not infer USB transport from that
  field. Reference package `ai.teamily.mobile` was resolved on this device.
- Installed Orhan: `ai.orhan`, versionName `0.2.0`, versionCode **293**;
  firstInstallTime `2026-11-01 01:15:49`, lastUpdateTime `2026-11-02 09:29:22`.
- REA pulled the installed APK read-only. Its SHA-256 exactly matched
  `app/build/outputs/apk/debug/app-debug.apk`:
  `9f933c4224ee3049ec470b38e54bbb9b77cf79f0e1d3fe9dfb0e04d5696ac595`.
  This verifies the already-updated `a77a92e` binary, rather than merely assuming
  that an old installed version matches source. No reinstall was necessary.
- REA evidence IDs: device list
  `ev_8545df1b7d209d3e3760faf605ace6cbb440ba97ce001e2e0ddcee11340c98e4`,
  package inspection
  `ev_f79986a34fd756ef2ebcb01f20252dea29a066f9ea905862d8c9204e82943bff`,
  installed APK acquisition
  `ev_5a06485cb831cd80aed7c7d12a4107445d73d34c06014eab843e18b0ac32b1ad`.

## Checks and provenance

From the repository root, using the existing wrapper:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug --console=plain
```

The first invocation was interrupted by a tool-server restart after tests began.
The follow-up invocation completed **BUILD SUCCESSFUL**, 54 actionable tasks:
3 executed, 51 up-to-date. Unit tests and assemble were **UP-TO-DATE** in that
successful invocation; they were not freshly executed a second time.

Current unit XML, produced during this session at `2026-11-02T04:38:37` through
`04:39:17` UTC: **31 suites, 144 tests, 0 failures, 0 errors**. This includes
artifact-store save/edit/delete/search/failure tests and layout-preference tests.
Lint executed in the successful invocation: **0 errors, 50 warnings, 1 information
item**, report modified `2026-11-02T04:45:21Z`. Assemble validated the existing
versionCode 293 output. These are distinct from the historical 141-test reports
for `3ac2ecd` quoted in the old PR body.

Local raw output: `captures/artifacts-2026-11-02/gradle-checks.log` (ignored).
XML/HTML: `app/build/test-results/testDebugUnitTest/`,
`app/build/reports/tests/testDebugUnitTest/`, and
`app/build/reports/lint-results-debug.xml` (ignored build outputs).
The documentation/evidence commit requires content/link review and
`git diff --check`; no Android source changed after the checks.

## Fresh runtime observations on versionCode 293

| Flow | Actions and observed result | Status |
| --- | --- | --- |
| Creation/save/detail | Artifacts → New document; enter synthetic `ArtifactQA1102` and `Synthetic preview needle1102`; Save opens detail with the saved title/body | Verified |
| Compact/large preview | Switch layout: compact title/date row becomes a card with the saved body; switch back returns compact row | Verified |
| Layout persistence | Force-stop/relaunch after selecting large and later compact; each selection survives | Verified |
| Content search | `needle1102` matches only the new document, although its title does not contain the query | Verified |
| No-match search | `NoSuchArtifactQA1102` shows `No artifacts match`; restart clears the transient query | Verified |
| Editing | Edit existing document; insert text; Save opens detail with `Synthet editedic preview needle1102`; created date remains 10:14 am; edited body survives force-stop/relaunch | Verified |
| Sharing | Share opens Android's text share sheet with the edited saved body; dismiss without choosing a recipient | Verified to chooser; receiver delivery not tested |
| Delete cancellation | Delete → Cancel returns to the existing document | Verified |
| Delete persistence | Delete → confirm removes only the new synthetic document; after force-stop/relaunch only the pre-existing `Layoutcheck` remains | Verified |
| Themes and font | Both list layouts in light/System and explicit Dark; detail and both lists at font scale 1.5 on 1272×2800 / 560 dpi (about 363 dp width) | Verified for these samples |
| Write failure UI | Existing unit tests cover failed store writes retaining data and failed preference writes retaining the choice; device disk/write failure was not induced | Partial; device failure path pending |

The original theme was System; it was restored through Appearance after the
temporary Dark check. System font scale was restored from 1.5 to its original
1.0. The list was restored to the original compact layout. No uninstall, data
clear, manual app-history rewrite, reference-record mutation or message send
occurred. The only new saved artifact was removed through Orhan's own UI.

## Visual evidence

Screens contain synthetic document text, not personal reference records. No
share-sheet contacts, chats, tokens or auth files are included. Screenshots are
direct device captures, not previews. Additional captures and the temporary
ADB helper remain in ignored `captures/artifacts-2026-11-02/`.

- [Compact, light](artifacts-2026-11-02/compact-light.png)
- [Large preview, light](artifacts-2026-11-02/large-light.png)
- [Compact, dark](artifacts-2026-11-02/compact-dark.png)
- [Large preview, dark](artifacts-2026-11-02/large-dark.png)
- [Detail at 1.5× font scale](artifacts-2026-11-02/detail-large-text.png)

## Remaining work and next slice

Stay in Artifacts. Completed layout implementation was not repeated.

- **Partial:** safe device verification of failed save/delete/layout writes;
  failure injection must preserve real saved data. Long content/title, rotation,
  editor process-death and exhaustive accessibility checks are not established
  by the short synthetic samples here.
- **Blocked for live device reply-save:** earlier PR #93 observation found no
  Supabase JWT. This session did not attempt sign-in or repeat the backend call;
  successful reply-save remains unverified until usable authentication is supplied.
- **Not started:** typed web-page, Markdown, media and saved-message support.
  Earlier PR observations are historical reference evidence: web/Markdown
  rendering, empty Media/Messages, sort and filter sheets. They were not captured
  afresh here. Actual file formats, error paths and export behavior remain unknown.
  Next slice: inspect existing reference/static evidence for one typed artifact
  flow, then specify an Orhan-owned model and service boundary before implementing.
- **Not started:** observed creator/conversation filters and sort choices;
  existing local artifacts do not yet carry the required provenance metadata.
- **Preserved Memory gaps:** overview-edge behavior, private profile synthesis /
  Dream refresh, anticipated generation/actions, meeting recording/playback/minutes.
  Their prior blockers are not completion.
- Social-only Discover navigation remains visible and is an outstanding separate
  focused change under AGENTS.md; it was not modified during this verification.

No stage handoff to Agents, merge, release publication or service deployment
is implied by these results.
