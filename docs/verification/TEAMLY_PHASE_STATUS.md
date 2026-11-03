# Teamily approved phase and task status

## Current delivery state

- **Approved baseline:** [TEAMLY_FEATURE_REFERENCE.md](../reference/TEAMLY_FEATURE_REFERENCE.md), v2.0 — 2026-11-03, user-approved in PR #120 (`a5470992377d4003dac8cad240063630cb23042b`).
- **Active phase:** **Phase 1 — Chats (F1.1–F1.6)**.
- **Current task phase / feature:** **Phase 1 — F1.1 — Text sending, mentions, and composer-draft persistence**.
- **Task status:** **Partial**. Existing send and agent mention flows were reused. Restart-persistent draft storage and migration were implemented, unit-tested, and verified on an updated device build. Group mentions remain blocked by the lack of an Orhan group/member model; updated-build send was not tested in the existing personal assistant thread.
- **Task evidence:** [sanitized REA record](f1-1-rea-evidence-2026-11-03.md); captures under ignored `captures/f1-1-2026-11-03/` are raw and are not committed.
- **Source revision:** focused local commit `feat(android): persist F1.1 composer drafts` on branch `docs/chat-f11-reference-inspection`, based on `b5e4360`. The pre-existing edit in `HomeScreen.kt` was preserved and remains outside this commit.
- **Next in-phase slice:** Phase 1 — F1.2 baseline inventory may proceed while the F1.1 group mention gap stays blocked on an Orhan-owned group/member model. Do not add guessed group storage or backend behavior. Revisit F1.1 if that capability becomes available.

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

## Executed checks

- Targeted tests: `ChatThreadStoreTest`, `ChatAttachmentPersistenceTest`, and `HandoffParseTest` — **passed**.
- Full unit suite: **193 tests, 1 failure** in unchanged `MeetingMinutesActionsTest.emptyOrUnstructuredActionSectionDoesNotCreateActions` (`MeetingMinutesActionsTest.kt:25`). The F1.1 changes do not touch this test or its implementation.
- `:app:assembleDebug :app:lintDebug -PversionCodeOffset=105` — **passed** on final source. The wrapper's Kotlin daemon connection failed; Gradle's in-process fallback completed compilation. Lint report: `app/build/reports/lint-results-debug.html`.
- `git diff HEAD^ HEAD --check` and working-tree `git diff --check` — **passed** after the final status/evidence updates.
- Installed Orhan before update: `ai.orhan`, version `0.2.0`, versionCode `366`. Final APK: same package/name, versionCode `367`. Signer SHA-256 matched (`8de8c2e46e572df1a7dcedce00b48a6ba0e53c042d52079c510f2923be88e06e`). The committed derivation/configuration was unchanged; a one-build `versionCodeOffset=105` produced 262 + 105 = 367.
- `adb install -r` succeeded and REA observed versionCode 367; the original first-install timestamp was retained. No uninstall or app-data clear was performed.
- Updated build was observed on the device. The synthetic draft survived leaving/reopening the thread and force-stop/cold relaunch (`ev_f74904d4…`, `ev_b4972f5c…`). No Orhan message was sent into the existing personal assistant conversation.

## Phase ledger

| Phase | Area | Approved features | Current status / exit evidence |
| --- | --- | --- | --- |
| 1 | Chats | F1.1–F1.6 | **Active; F1.1 partial** as detailed above. Draft persistence is runtime-verified; group mention support lacks an Orhan group/member model and send/failure parity remains unverified. F1.2–F1.6 remain queued. |
| 2 | Agents | F2.1–F2.6 | Queued under v2.0; existing source preserved, no phase sign-off recorded. |
| 3 | Memory | F3.1–F3.8 | Queued under v2.0; existing source/data preserved, no phase sign-off recorded. |
| 4 | Artifacts | F4.1–F4.7 | Queued under v2.0; existing source preserved, no phase sign-off recorded. |
| 5 | Meetings | F5.1–F5.3 | Queued; Android entry point and unknowns require evidence before new behavior is added. |

“Queued” means queued for verification/delivery under v2.0; it does not claim related source is absent. Existing authentication, stored data, signing, and unrelated work remain preserved.
