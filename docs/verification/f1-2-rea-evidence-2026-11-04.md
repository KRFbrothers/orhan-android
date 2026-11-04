# F1.2 REA inspection record — 2026-11-04

> The original reference-observation record below is retained as captured. Its final “Change and verification state” subsection was written before the incremental compile was diagnosed and before updated Orhan verification; that subsection is superseded by [the build/device verification handoff](f1-2-build-verification-2026-11-04.md). The Teamly reference quote-tap highlight remains inconclusive.

Approved behavior source: `docs/reference/TEAMLY_FEATURE_REFERENCE.md` v2.0, approved in PR #120. This record distinguishes runtime observations from static Orhan source. Screenshots remain ignored under `captures/f1-2-2026-11-04/`.

## Device and reference app

- REA device list: `ev_dff603877295ad2fe5b83cfaf0115433185d82b965ff4fb3c6ee065e3a72d3e3` — one physical-looking RMX5051 / RMX5051IN, serial `9cefa3cb`, state `device`; REA reports transport `unknown`, so USB transport itself is not independently verified.
- Package: `ev_0a1d2a1645adf6ac84777e2beb14849c3b4ef701e19e06094e99d27b584db8c5` — `ai.teamily.mobile`, 2.0.20 (234), installed via Play Store.
- Foreground: `ev_919b3588803bdee0d2486cf9ae8ebe826bd63fbb719e247601a392a4f6f0d66e` — `ai.teamily.mobile/.MainActivity`.
- Fresh screenshot after resume: `ev_8f78400cddeab9672c56bf976ea21ff0f95804f7ed32ddf220d86e831b1eeb72`, capture `captures/f1-2-2026-11-04/copy-precheck/screen.png` showed the app on the chat list; synthetic Content Creator thread was reopened and captured as `ev_f0cd2e87be13a92189a47089cbf8a0196f46e5a1389ff8945cf33283d5595d5`, `captures/f1-2-2026-11-04/synthetic-open-again/screen.png`.

## Observed synthetic 1:1 flow

The Copy/dismissal follow-up is tracked in `f1-2-copy-dismiss-2026-11-04.md`. Fresh confirmed-sheet Back checks supersede the earlier Back interpretation below: Android Back returned to the chat list twice (before/after Evidence `ev_878440dc9d6d6d5a2e437a0f05347ca0db3d6eb363c0229414fc2c095f7c0e33` / `ev_adf30ce65d925b40ee0ac4d80d27e3a1754d01c096ca00dd62380091562d65c9`, and `ev_376643502c96b5b2b42c0ffc8af41dc18353869f74639cc8923a7bd8e1467863` / `ev_c59a420d48d1a88dd3715d4c5a021ee0b73b68fc8a44eb02ea6bf4ef6cb750d8`). The user explicitly chose this chat-list destination for Orhan, preserving draft/reply state. Outside tap dismissed only the sheet. Copy of a synthetic agent text response was verified by exact text comparison after paste into an empty Orhan scratch composer; a toast was not used as clipboard proof. Raw captures remain ignored under `captures/f1-2-copy-dismiss/`.

All chat interactions below used the existing synthetic Content Creator thread containing a prior synthetic F1.1 message. No personal chat was modified. The synthetic F1.2 reply message was sent during the previous authorized inspection; no unsend/delete/forward was attempted.

| Action | Evidence / capture | Expected from v2.0 | Actual observation and limits |
|---|---|---|---|
| Long-press own synthetic text message | `ev_77b396997061e3453803f92218bbbf8262da112a764ca57844dd863c37c11a24`, `synthetic-user-action-sheet-3/screen.png` | Action sheet opens | Reactions, Reply, Translate, Copy, Forward, Multiple Selection, Add to My Artifacts, Unsend, Delete visible. Report absent in this 1:1. |
| Reply | `ev_0d975abd327631518c845c60cb6977f832d1028afc274fc40c83ff095fd783`, `synthetic-reply-composer/screen.png` | Quote bar and clear X | Quote chip displayed the selected synthetic message and an X. X itself was not selected; cancellation via Back is covered below. |
| Send reply | `ev_282f3d6eacd00313c5f57f64cdb222bcc3a1eb4d4aca8140e9a85f7c2666ce10`, `synthetic-reply-sent/screen.png` | Sent message carries quote | Synthetic reply appeared with its quote preview, followed by agent response. |
| Tap quote | `ev_1f35d9ec09f2f75fae596d25671bb0f58973dc8fe9298014438df8e859ed4bb7`, `synthetic-quote-tap/screen.png`; follow-up `ev_832f5b0ebb49773018d8d435bda11110285bb17e448dcc0d56ea4b024a0a08df` | Original scrolls into view and highlights | Quote remained visible. A single frame did not conclusively establish original-message visibility/highlight; mark behavior unknown pending a clearer runtime check. |
| Long-press agent response | `ev_ac93ca675c98d35d6ab875f8889ae67b032667468b2a94ceede99e0f685c3adf`, `synthetic-agent-action-sheet/screen.png` | Recall/Unsend is own-only | Sheet showed Delete but not Unsend. No action selected. |
| Dismiss sheet | `ev_92db3389803e6f8cbbafc0f6430641632004e5ae086e29bd2fc6d03cc9899dcc`, `action-sheet-dismissed/screen.png` | Dismiss without acting | Android Back dismissed it without a message mutation. |
| Copy / forward / react / multiple selection / X cancellation | None | As v2.0 | Not observed in this pass. A later ADB long-press command was interrupted during an approval prompt; no claim of a completed action is made. |
| Failed-send eligibility / group-only Report | None | Failed sends block forward/recall; Report group-only | No failed send or synthetic group was available for this safe inspection. |

## Orhan source comparison

| Approved action or behavior | REA evidence | Existing Orhan file:lines | Status | Smallest change / remaining work |
|---|---|---|---|---|
| Long-press opens dismissible actions | Own sheet + Back dismissal above | `app/src/main/java/ai/orhan/design/screens/ChatScreen.kt:600-603, 1631-1665` | Partial: sheet/dismiss exists; REA parity of all entries unknown | Keep existing sheet and extend only verified actions. |
| Reply quote, clear, send with relation | Reply and sent quote captures above | `ChatScreen.kt:150-163, 232-239, 279-282, 490-512, 679-705, 925-1005, 1330-1405, 1631-1665`; `ChatViewModel.kt:393-412`; `ChatThreadStore.kt:28-32, 44-51, 94-111, 128-143, 176-179`; `OrhanNavHost.kt:613-627` | Verified source gap addressed; device comparison pending | Add persisted `replyToMessageId` in v9; keep v8 draft decoding and reuse the existing message list for source focus. |
| Tap quote to navigate/highlight exact original | Reference result inconclusive | `ChatScreen.kt:545-555, 597-620`; exact-message route in `OrhanNavHost.kt:700` | Unknown runtime reference; implementation reuses existing in-thread list state | Verify updated Orhan and obtain a conclusive reference capture. |
| Copy message text | No REA selection observed | `ChatScreen.kt:721-730, 1194-1198, 1655-1657` | Source present; reference runtime unknown | No change until copied clipboard result is observed. |
| Forward / bulk selection / react | Sheet shown in reference; action outcome not tested | `ChatScreen.kt:1625-1660` | Verified source gap, behavior details partly unknown | Defer; avoid inventing destination, selection or reaction state. |
| Unsend only own / Delete confirmation / group-only Report | Own vs agent eligibility observed; no destructive operation; no group | `ChatScreen.kt:1625-1660` | Verified source gap for own-only Unsend behavior; delete/report details blocked | Do not implement without safe eligibility/confirmation scope evidence. |
| Translate / transcribe | Menu availability shown; outcome not tested | `ChatScreen.kt:711-745, 1650-1655` | Existing source paths; runtime outcome unknown | No change in this F1.2 slice. |

## Change and verification state

- Source change is limited to the existing chat model, action sheet, composer, `ChatViewModel`, navigation callback, and history serializer. Thread format v8 data remains readable; F1.1 draft field and prior migrations are preserved.
- Added unit tests cover reply-ID mapping/serialization and v8 draft migration. Full unit test compilation was attempted but `:app:compileDebugKotlin` failed with widespread unresolved references across app sources; the cause was not established. No passing test/build or runtime parity claim is made.
- The existing `MeetingMinutesActionsTest.emptyOrUnstructuredActionSectionDoesNotCreateActions` previously reproduced 1 failure / 2 tests at line 25 before these F1.2 changes. Its test and implementation are unchanged from baseline commit `b5e4360`.
- Orhan phone update / UI comparison is pending. The interrupted ADB approval attempt means no follow-up ADB interaction is claimed. No app uninstall or data clear occurred.
- F1.1 remains partial: group mention support is blocked by the absent Orhan group/member model.
