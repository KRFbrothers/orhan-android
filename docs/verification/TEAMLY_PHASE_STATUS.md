# Teamily approved phase and task status

## Current delivery state

- **Approved baseline:** [TEAMLY_FEATURE_REFERENCE.md](../reference/TEAMLY_FEATURE_REFERENCE.md), v2.0 — 2026-11-03, user-approved in PR #120 (`a5470992377d4003dac8cad240063630cb23042b`).
- **Active phase:** **Phase 1 — Chats (F1.1–F1.6)**.
- **Current task phase / feature:** **Phase 1 — F1.2 — PR #121 merge-conflict resolution**, verified for the tested text context; F1.1/F1.2 remain partial overall.
- **F1.1 status:** **Partial**. Existing send and agent mention flows were reused. Restart-persistent draft storage and migration were implemented, unit-tested, and verified on an updated device build. Group mentions remain blocked by the lack of an Orhan group/member model; updated-build send was not tested in the existing personal assistant thread.
- **Task evidence:** [F1.1 REA record](f1-1-rea-evidence-2026-11-03.md), [F1.2 REA record](f1-2-rea-evidence-2026-11-04.md), and [F1.2 build/device verification](f1-2-build-verification-2026-11-04.md); raw captures and full logs remain ignored under `captures/`.
- **Source revision:** PR #121 branch `feat/android-phase1-chat-f11-f12`, originally at `cfd5483`, with latest `origin/main` integrated by the conflict-resolution commit. The unrelated `HomeScreen.kt` working-tree edit was preserved and excluded.
- **Next in-phase slice:** Phase 1 — F1.2 Translate language picker/cancellation and remaining safe action eligibility. Text Copy and chosen Back/outside semantics are verified; image/media Copy and other actions remain open. F1.1 group mentions remain blocked on an Orhan-owned group/member model.

## F1.1 acceptance status

| Acceptance item | Status | Evidence / limitation |
| --- | --- | --- |
| Empty composer blocks send and shows mic | **Verified on reference; source present in Orhan** | REA `ev_2dbdf7e3…` and `ev_f57c6b08…`; Orhan `ChatScreen.kt` composer send state. |
| Text send appears without manual refresh | **Verified on reference; source path present in Orhan** | Synthetic send and immediate reply, REA `ev_3d3a6125…`; Orhan sends through the existing `ChatViewModel`. |
| Group `@` picker has Everyone first | **Verified on reference; blocked in Orhan** | REA `ev_a743347d…`. Inspected Orhan chat flow has no group/member model or member source; no group backend was invented. |
| Direct 1:1 `@` does not open a member picker | **Verified in Teamily agent 1:1; Orhan human 1:1 context unknown** | REA `ev_dc8ddf9f…`. Orhan's assistant-thread saved-agent mention route is distinct and remains. |
| Draft returns after leaving and reopening a thread | **Verified on reference; source-only in Orhan before change** | REA `ev_0e2d1ca5…`, `ev_88133421…`. |
| Draft survives force-stop and cold relaunch | **Reference behavior conflicts with v2.0; Orhan implementation verified** | Teamily lost draft in ordered REA sequence (`ev_b4c8953f…`, `ev_3b447b68…`, `ev_b9775270…`). User chose the approved v2.0 requirement: preserve it. Updated Orhan versionCode 367 restored the synthetic draft after force-stop/cold start (`ev_21dc0649…`, `ev_b4972f5c…`). |
| Send failure behavior | **Unknown** | No failure was induced on reference; retry/failed-bubble behavior was not inferred. |
| Hardware Enter / Shift+Enter behavior | **Unknown** | Reference check used Android virtual Enter, which inserted a newline; it does not establish hardware-key behavior. |

## Implementation reuse

- Reused `ChatScreen`, `ChatViewModel`, `ChatThreadStore`, `ChatUiState`, and the existing chat route.
- Added a per-thread composer draft field to the existing serialized thread format (v8); v7 transcript decoding remains keyed to v7 and is covered by a migration test.
- Draft-only threads are retained; message saves preserve drafts; clearing a draft preserves existing messages. Draft save errors are surfaced beside the composer.
- No second composer, screen, store, message model, API client, group backend, or send route was introduced.

## F1.2 — Existing message actions (partial)

**Latest slice:** [Copy/dismissal evidence and checks](f1-2-copy-dismiss-2026-11-04.md). Fresh reference checks supersede the earlier Back interpretation below: confirmed-sheet Android Back returned to chat list twice. The user chose that destination while preserving draft/reply. The existing sheet now uses a dedicated Back callback to HOME; outside dismissal still clears only the sheet. Reply selection is retained per thread in the existing navigation host. Copy's existing helper was preserved and actual synthetic paste matched exactly on reference and Orhan. Updated signer-compatible Orhan code 371 verified Back → chat list → reopen with the same draft/reply, outside dismissal, Copy, draft cold restart and exact saved-quote navigation. Relevant tests/assemble/lint passed; MeetingMinutesActionsTest separately reproduced 1 failure of 2. Earlier reference/build observations below are historical snapshots, not the current Copy status. F1.1 group mention blocker remains open.

- **Approved baseline:** [TEAMLY_FEATURE_REFERENCE.md](../reference/TEAMLY_FEATURE_REFERENCE.md), v2.0, PR #120.
- **Runtime reference:** realme RMX5051 / RMX5051IN, Android 16, REA device observation `ev_dff603877295ad2fe5b83cfaf0115433185d82b965ff4fb3c6ee065e3a72d3e3`; installed `ai.teamily.mobile` v2.0.20 / code 234, REA package observation `ev_0a1d2a1645adf6ac84777e2beb14849c3b4ef701e19e06094e99d27b584db8c5`; foreground app `ai.teamily.mobile/.MainActivity`, REA observation `ev_919b3588803bdee0d2486cf9ae8ebe826bd63fbb719e247601a392a4f6f0d66e`.
- **Evidence:** [F1.2 REA record](f1-2-rea-evidence-2026-11-04.md); raw screenshots remain ignored under `captures/f1-2-2026-11-04/`.
- **Verified reference flow:** in the existing synthetic Content Creator 1:1 thread, long-press opened message actions (`ev_77b396997061e3453803f92218bbbf8262da112a764ca57844dd863c37c11a24`); Reply opened a composer quote with clear X (`ev_0d975abd327631518c845c60cb6977f832d1028afc274fc40c83ff095fd783`); synthetic reply sent with its quote visible (`ev_282f3d6eacd00313c5f57f64cdb222bcc3a1eb4d4aca8140e9a85f7c2666ce10`). Tapping the quote was captured, but the frame did not conclusively establish original-message highlight (`ev_1f35d9ec09f2f75fae596d25671bb0f58973dc8fe9298014438df8e859ed4bb7`). Own-message sheet offered Unsend; agent-message sheet did not (`ev_77b396997061e3453803f92218bbbf8262da112a764ca57844dd863c37c11a24`, `ev_ac93ca675c98d35d6ab875f8889ae67b032667468b2a94ceede99e0f685c3adf`). Report was absent in this 1:1 sheet. Back dismissed the sheet without action (`ev_92db3389803e6f8cbbafc0f6430641632004e5ae086e29bd2fc6d03cc9899dcc`). Copy, forward, reaction, cancellation via X, failed-send eligibility, and group-only Report remain unverified; no destructive action was taken.
- **Orhan source inventory:** reused `ChatScreen`, `ChatViewModel`, `ChatThreadStore`, and existing exact-message `focusMessageId` scrolling/highlight. Added the Reply action and composer quote/clear affordance; sent messages store the source message ID and render it as a tap target. Thread format advances from v8 to v9 while retaining earlier decoders and the F1.1 draft field.
- **Current status:** F1.2 remains partial. The existing Reply flow is verified on updated Orhan v0.2.0/code 369: quote composer, synthetic send, tap-to-exact-source scroll/highlight, cold-restart persistence/navigation, and light/dark highlight. Teamly's quote-tap capture did not conclusively prove its highlight, so reference parity for that point remains unknown. The compile failure was reproduced and localized to inconsistent incremental compiler state; a one-invocation full compile restored state, and the normal compile then passed. No source/toolchain/dependency repair was necessary. Existing HomeScreen local change remains preserved. Details/logs: [F1.2 build/device verification](f1-2-build-verification-2026-11-04.md).
- **Unchanged failure baseline:** `MeetingMinutesActionsTest.emptyOrUnstructuredActionSectionDoesNotCreateActions` previously reproduced as 1 failure out of 2 tests at `MeetingMinutesActionsTest.kt:25`; test/meeting-action implementation are unchanged since baseline `b5e4360`. It was not suppressed or modified.
- **Remaining F1.1 blocker:** group mentions still lack an Orhan group/member model; F1.1 remains partial.
- **Next in-phase slice:** inspect Translate language picker/cancellation and other remaining safe eligibility/outcomes in synthetic context. Text Copy and chosen Back/outside behavior are covered by the latest report; other Copy media types and actions remain open.

## Executed checks

- Targeted tests: `ChatThreadStoreTest`, `ChatAttachmentPersistenceTest`, and `HandoffParseTest` — **passed**.
- Relevant history/reply tests passed. The separately run `MeetingMinutesActionsTest` had 1 failure of 2 at `MeetingMinutesActionsTest.kt:25`; it and its implementation are unchanged from baseline. The full unit suite was not rerun as a clean pass.
- Build blocker reproduction/restoration, exact checks, and results are recorded in [F1.2 build/device verification](f1-2-build-verification-2026-11-04.md). `:app:assembleDebug -PversionCodeOffset=106` and `:app:lintDebug -PversionCodeOffset=106` passed; lint report: `app/build/reports/lint-results-debug.html`.
- `git diff HEAD^ HEAD --check` and working-tree `git diff --check` — **passed** after the final status/evidence updates.
- Installed Orhan was code 367 and compatible update code 369 (`ai.orhan`, v0.2.0); signer matched and first-install timestamp was retained. `adb install -r` succeeded. No uninstall or app-data clear was performed.
- Updated build persisted and navigated the synthetic reply after cold restart in light theme; quote navigation/highlight was also checked in dark theme. This F1.2 verification sent only a synthetic message in Orhan, not a personal Teamly message.

## Phase ledger

| Phase | Area | Approved features | Current status / exit evidence |
| --- | --- | --- | --- |
| 1 | Chats | F1.1–F1.6 | **Active; F1.1 partial and F1.2 partial** as detailed above. Draft persistence is runtime-verified; group mention support lacks an Orhan group/member model. F1.2 Reply/source navigation is build- and updated-device-verified; other message actions remain open. F1.3–F1.6 remain queued. |
| 2 | Agents | F2.1–F2.6 | Queued under v2.0; existing source preserved, no phase sign-off recorded. |
| 3 | Memory | F3.1–F3.8 | Queued under v2.0; existing source/data preserved, no phase sign-off recorded. |
| 4 | Artifacts | F4.1–F4.7 | Queued under v2.0; existing source preserved, no phase sign-off recorded. |
| 5 | Meetings | F5.1–F5.3 | Queued; Android entry point and unknowns require evidence before new behavior is added. |

“Queued” means queued for verification/delivery under v2.0; it does not claim related source is absent. Existing authentication, stored data, signing, and unrelated work remain preserved.

## Required task record and evidence limits

At each task handoff, record the feature/task state, source revision, evidence/report location, actual checks/results, blockers and next in-phase slice. Use `verified`, `partial`, `blocked` or `not started`, distinguishing source implementation from updated-build runtime proof. Do not mark a task or phase complete because a blocker exists.

The approved document's labels do not establish runtime parity. REA tools/device/package availability must be checked in the executing session; do not assume access from an old capture. Missing or inferred behavior requires supported observation or a user decision. Do not invent backend contracts, UI options, extra features or success results. Phase advancement follows the approved order and recorded acceptance, or an explicit user-directed phase change. Existing auth, consent, stored data, signing and unrelated work remain preserved.

## PR #121 conflict-resolution handoff

- Fetched `origin/main` at `8fb774108f536c7a816e0d8477e74a6ea5fa3349` on the existing branch `feat/android-phase1-chat-f11-f12`. Merge preview and actual merge identified one content conflict: this ledger. The two ledger histories were reconciled: the current F1.1/F1.2 evidence and partial status remain, while v2.0 task-record and evidence-limit requirements from `origin/main` are retained. The four F1.1/F1.2 evidence reports remain in the PR branch; no evidence was removed.
- The unrelated local `HomeScreen.kt` edit was left unstaged and excluded. No device/reference interaction occurred for this integration task; prior device evidence is reused only at its recorded scope above.
- Checks, run sequentially: `:app:testDebugUnitTest --tests ai.orhan.data.history.ChatThreadStoreTest --tests ai.orhan.data.history.ChatAttachmentPersistenceTest --tests ai.orhan.feature.chat.HandoffParseTest` **passed**; `:app:assembleDebug` **passed**; `:app:lintDebug` **passed**; `git diff --check` **passed**.
- Historical baseline: `MeetingMinutesActionsTest.emptyOrUnstructuredActionSectionDoesNotCreateActions` had failed at line 25 before this merge. The fetched `origin/main` includes a change to `MeetingMinutesActions.kt`; the same test was run separately on the merged source and **passed**. The test was not suppressed or skipped. The full unit suite was not run, so no full-suite pass is claimed.
- PR remains the existing PR #121, unmerged. F1.1 group mentions remain blocked; F1.1 and F1.2 remain partial. Next in-phase feature slice remains F1.2 Translate language picker/cancellation and remaining safe action eligibility.

## Read-only repository cleanup inventory — 2026-11-04

- **Scope/evidence:** scanned 234 tracked files at `ca35979` for byte-identical SHA-256 contents, backup/temp filenames, references to duplicate design assets, and stale/superseded documentation. This is a removal-candidate inventory, not authorization to delete.
- **Exact duplicates:** 14 hash groups, with 441,426 bytes in extra copies if each group were reduced to one. Most are design assets: app resources mirrored under `design/images/`, and fonts/logos/app.js repeated across standalone `design/code-pages/{icons,inter-fallback,screens,theme}/` pages. App resources are used by Kotlin screens; design assets are referenced by the HTML showcase. The code-pages variants have distinct HTML/data and are not internally linked by tracked files, but may be directly served by GitHub Pages. **Do not remove these copies/pages without checking both consumers and intended published URLs.**
- **Documentation drift:** root `README.md` and `design/README.md` say 14 screens, while `design/index.html` says 16 (lines 926, 2453). `M3_AUDIT_REPORT.md` refers to `tools/m3_audit.py`, which is not present in the tracked tree. These are candidates to correct, not files to delete.
- **Historical material:** `docs/verification/included-feature-backlog-2026-11-02.md` contains an older phase-order recommendation that is superseded by v2.0. Other dated verification reports also preserve build-specific evidence. Keep these out of deletion candidates unless their evidence is deliberately archived elsewhere.
- **Checks/outcome:** no files were removed. Active phase and F1.1/F1.2 statuses are unchanged. `:app:lintDebug --quiet` could not complete in this environment (Gradle failed with error `27`), so no lint-based unused-resource conclusion is claimed.
