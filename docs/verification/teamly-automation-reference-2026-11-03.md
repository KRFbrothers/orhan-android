# REA reference slice: automation gallery and draft builder

User-directed target: reproduce Teamly's observed non-social flows in Orhan,
starting with an inspected flow rather than treating backend PRs as feature
parity. This slice covers the automation draft entry/builder, not execution.

## Current inventory and design contract

Started from a clean Orhan checkout, inspected current history and live PR
metadata, fetched the integrated base and created
`feat/reference-automation-builder` from `origin/feat/meeting-minutes-actions`
at `fe101c2`. #113 is already merged; its workflow execution/recurring code is
retained. No backend, auth, dependency, signing, version or CI change is needed.
Existing Memory/context stores remain unchanged and are not attached to drafts.

| Owner / entry | Existing behavior | Gap / decision |
| --- | --- | --- |
| Reference home + | Opens create menu, including Automations | Observed navigation, not assumed from web docs |
| Reference Automations | My Automations / Work / Life, Set up manually and Work template cards | Implement the observed Work entry/catalog; Life contents remain unknown |
| Reference daily brief | Compiled task area, intent/schedule/team sections, Generate Prompt | Implement a working prompt draft slice; unseen options are not parity claims |
| Orhan home + | Directly opened a new chat | Preserve New chat in a create menu and add Automations |
| Orhan canonical WorkflowStore/editor | Real persisted reusable prompts, agent IDs, editor/delete, execution history/controls | Reuse that owner; do not invent a second automation store or fake running jobs |

Screen job: choose a starter, describe a task, select a saved agent and review the
compiled prompt. Primary action: await a real canonical save, then open the
existing chat draft/sign-in route without sending. Required states: empty task,
no saved workflows, unavailable/deleted agent, full/write-failed storage and
unsaved Back confirmation. Layout uses existing Compose/Material 3 theme roles,
scrolling content and 48dp controls; keyboard/navigation insets are respected.
Dark, compact and large-text device results are pending. No reference branding,
account content, logos or avatar assets are copied into Orhan.

## Fresh observed reference evidence

REA `list_adb_devices` and `inspect_adb_package` confirmed selected serial
`9cefa3cb`, RMX5051, reference package `ai.teamily.mobile`, version 2.0.20/code
234 before acquisition. REA started its resolved launcher activity and captured
frames. Normal ADB navigation opened the observed + menu and Automations page;
REA itself did not perform arbitrary taps. No Generate/save/send/publish or
personal-record mutation was performed on the reference account.

| Observation | REA evidence | Capture SHA-256 |
| --- | --- | --- |
| Reference package/version | `ev_0a1d2a1645adf6ac84777e2beb14849c3b4ef701e19e06094e99d27b584db8c5` | Package observation, not APK identity |
| Home + menu with Automations | `ev_1efe9573b7265b91868968afc102beb285ce3edbf1d703cc14ed7f9c45bef8fc` | `5f70bdc11ba451ae9c94aed2a90f93efe8366c625b3c240ed3066690e9f9b61b` |
| Work template gallery | `ev_d480dd800bc33469bbb9a6723a4be13b8e7a7b7b3a478a14fe424b74a2022d9f` | `3e3b6e73faebac9e9f8e69ab812f7059c34ea07eb8deb15f1eab193224134345` |
| Daily work brief builder | `ev_79365d02198e79e2be466312acaf0927e16cd379e24aff80e2e297b0e02654a5` | `1667dc9387abb48131ba4bdb00ad1e70b8c310aac7dc63dbd2ceb7810f901432` |

Frames are private, uncommitted files under
`captures/teamly-workflows-2026-11-03/{menu,automations,daily-brief}/screen.png`.
The menu/home capture contains account UI; it is deliberately not committed.
No personal text, account identifiers, screenshots or provider tokens are
included in this report. Capture hashes describe these frames, not UI interaction
coverage or APK code. No decompilation or instrumentation was performed.

Gallery observation: Work shows meeting preparation, daily briefing, industry
monitoring, research, reports, web/tool and task-breakdown starters. The selected
daily brief displays a task/compiled-prompt area, Monitor, Run now, Team and a
Generate Prompt button. Its explanatory text says the prompt rewrites as chips
are edited. Layout visibility is observed; actual chip mutation, generation,
server execution and persistence are **unverified**.

The subsequent intent-navigation attempt was refused by the package-focus guard.
REA capture/window inspection then hit client policy:
`MCP tool call requires approval, but approval policy is never`.
There is no retained intent-options observation from that attempt. Per the
[REA recovery guidance](<C:/Users/Fareed/.agents/skills/reverse-engineer-anything/references/connection-and-recovery.md>),
this is a client rejection before REA execution. No engine reinstall, broader
permission configuration edit or alternate-tool bypass was attempted.

## Source completed in Orhan

- Home create menu retains New chat and adds Automations.
- Work starter gallery and My Automations view use real canonical workflow
  records; Set up manually opens the existing editor. Starter descriptions and
  generated instructions are Orhan-owned text, not copied reference prompts.
- Builder has a live compiled prompt, bounded task input, intent selection,
  saved-agent ID selection and an explicit one-time scope. The reference only
  established the initial Monitor/Run now values; extra intent choices are
  Orhan implementation, not verified Teamly option parity.
- Generate Prompt awaits WorkflowStore persistence before navigating to the
  selected existing agent/Orhan chat draft. It never auto-sends or creates a
  background job. Normal authentication still gates online chat; a saved prompt
  remains available under My Automations if sign-in is needed. Existing restored
  chat-draft behavior is preserved.
- Storage-full insertion fails before evicting older workflows. Duplicate saves
  of the same generated ID remain updates. A failure keeps the builder draft,
  gives an error and allows retry. No migration or serializer format change.
- Changing or removing an agent cannot silently select another saved persona.
  Back asks before discarding typed draft content; writes disable duplicate taps.

## Verification and exact limits

Final `:app:assembleDebug` passed (1m 6s, 38 tasks). An earlier rebuild failed
because Windows could not delete the intermediate resource R.jar; a serialized
retry after lint completed passed without configuration changes.
`:app:lintDebug` failed with two ProduceStateDoesNotAssignValue errors at
ChatScreen.kt:977 and :1037, plus 53 warnings. ChatScreen.kt is unchanged from
the PR base; no baseline lint run was performed. No lint errors were reported
in the new automation files. The full local report is
`app/build/reports/lint-results-debug.html`.
`git diff --check` passed. Automated tests, instrumentation and emulator checks
were not run. No provider request, message send, backend deployment or PR merge
was performed. No new APK was installed on the phone.

| Check | State |
| --- | --- |
| Reference entry/gallery/initial daily builder layout | Observed via fresh REA frames |
| Reference chip choices, Life templates, generation destination, history and restart | Blocked/unverified after client-policy rejection |
| Orhan source wiring and debug compilation | Source complete; build passed, runtime not verified |
| Orhan updated-phone flow, persistence/failure UI, light/dark/large text | Pending; no updated build installed or inspected |
| Full schedule/team/scope parity, private multi-agent groups, ongoing goals and flow canvas | Not implemented by this first draft slice |

Existing server history, recurring registration and worker work stays intact;
this slice does not establish that those services are deployed. My Automations
here is explicitly saved workflow prompts, not a claim that each item is an
active scheduled task. Calendar/ongoing monitoring is not inferred from generated
text. The prior v341 workflow queued → sign-in observation remains separate.

Next: restore scoped REA read/capture access in the client, continue safe
navigation through the reference's intent/schedule/team/scope controls, and verify
an authorized compatible Orhan update through normal UI. Do not repeat setup or
claim an unobserved option/execution path completed. Automated test stop remains
active until the user changes it.
