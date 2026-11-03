# Orhan implementation guide

## Mandatory read before every task

These rules apply to Codex, REA-assisted work and every other AI agent in this
repository. Before planning, inspecting a reference, editing code or proposing a
next task:

1. Read this file and [the final user-approved Teamily reference v2.0](docs/reference/TEAMLY_FEATURE_REFERENCE.md). On a new session read the complete reference; before each task re-check its scope, evidence legend, phase plan and the relevant feature block/acceptance checks.
2. Read [the current phase/task status](docs/verification/TEAMLY_PHASE_STATUS.md), the relevant existing code/tests and prior evidence. Do not infer progress from a merged PR, a screen name or an old report.
3. Begin the task with the following declaration, and repeat it at handoff:

   ```text
   Approved reference: TEAMLY_FEATURE_REFERENCE.md v2.0
   Active phase: Phase N — <area from TEAMLY_PHASE_STATUS.md>
   Task phase / feature: Phase N — F<N>.<id> — <feature title>
   Task: <one bounded slice and its reference section>
   Evidence: <observed / source-only / documentation-based / unresolved>
   Acceptance checks: <the relevant approved checks>
   ```

   For reference/governance work spanning all phases, state `Task phase: Phases
   1–5 — reference governance`; still state the one active delivery phase.
4. Keep the task inside the approved feature and phase. No extra product behavior,
   guessed options, fabricated backend contracts, no-op controls or simulated
   success. Reuse the current architecture for implementation details.
5. `Inferred` and `unknown` items are not permission to implement a guess. Resolve
   them through supported REA observation or an explicit user decision. If
   evidence conflicts with the approved reference, record both and resolve the
   conflict before changing the affected behavior; do not silently rewrite the
   reference or replace it with an observation from a different platform/build.
6. Update the phase/task status and evidence at handoff. Do not advance phases or
   mark acceptance complete without the required verification.

The user's final v2.0 approval supersedes the previous Memory-first delivery
order and earlier next-task recommendations. Historical reports remain evidence
for their recorded builds only; they do not choose the current phase.

## Goal and scope

Build Orhan's own working implementation of Teamly's observed non-social features,
including their UI, behavior, persistence and service integration. Keep Orhan's
name, logo, application ID (`ai.orhan`) and identity. A matching screen alone does
not establish feature parity. Treat the requirements below as targets, not claims
that the current app already implements them.

Exclude Discover / Following / Feeds, public publishing and the social profile
(followers, following, posts and social likes/saves). Preserve private account and
profile editing, authentication, settings and Memory's private My Profile.
Private artifact export/share remains in scope. Remove existing social-only
navigation in a focused change; preserve navigation to the included features.

## Start from existing work

- Read this file, relevant code and tests, `git status`, branch history and current
  PR state before editing. Preserve unrelated local changes.
- Reuse existing screens, stores, routes and backend services. Extend the
  canonical implementation rather than creating a second version.
- Inspect the current Memory implementation before planning changes. PR #88 is
  already merged; its merge does not prove device verification or full parity.
- Use a focused feature branch and reviewable commits. Do not merge PRs, publish
  builds or deploy services unless the user's instructions authorize it.
- Do not change unrelated UI, auth, backend behavior, dependencies, signing,
  version derivation or CI configuration as a side effect of feature work.

## Approved phase order

Follow v2.0 §0.4 exactly. Inventory current source, observations and unknowns
inside the active phase before choosing its next slice; preserve work already
implemented in any phase rather than rebuilding or deleting it.

| Phase | Area | Approved feature IDs |
| --- | --- | --- |
| 1 | Chats | F1.1–F1.6 |
| 2 | Agents | F2.1–F2.6 |
| 3 | Memory | F3.1–F3.8 |
| 4 | Artifacts | F4.1–F4.7 |
| 5 | Meetings | F5.1–F5.3 |

The current phase and task are recorded only in
[TEAMLY_PHASE_STATUS.md](docs/verification/TEAMLY_PHASE_STATUS.md). Do not create
a competing active-phase record. Cross-feature integration is verification
within these five phases, not an invented sixth delivery phase.

Deliver one small working slice at a time: read the approved feature, inspect
the reference and current implementation, specify only evidenced behavior,
implement, test and compare on the updated build. A necessary cross-phase
dependency must be explicitly recorded with both feature IDs; it is not
permission to start an unrelated feature or silently skip the approved order.

Record unresolved acceptance checks and the exact missing evidence. Continue
independent authorized work within the current phase when one slice is blocked.
A blocker does not complete a feature or phase. Advance in order only after the
phase's applicable checks and open unknowns are resolved and recorded, or after
an explicit user-directed phase change. Do not assume source present means
runtime verified, or erase existing later-phase work because it is not active.

Reference §6.2 is investigation guidance, not a replacement delivery sequence.
Cross-phase read-only investigation must identify the dependency and task phase.

## Reference inspection with REA

- Use the connected REA server's actual tool inventory and schemas. Installed
  instructions or repository documentation do not establish tool availability.
- Verify the selected USB device and reference package before acquisition or
  capture. USB attachment to the user's laptop does not grant a remote workspace
  access to that phone.
- Reuse existing captures and evidence. For each flow record the entry point,
  actions, state/data changes, result, error states and remaining unknowns.
- Before each REA flow, identify the approved phase/feature and the question it
  resolves. Distinguish observed behavior, static findings, inference and
  unverified behavior. Screenshots establish visible layout; decompilation alone
  does not establish runtime behavior. Do not invent behavior in unseen screens.
- Use available tools or user-assisted navigation for interactions. Do not
  assume that REA offers arbitrary taps, typing or access to a private backend.
- Inspect the reference without modifying its personal records or sending
  messages to others. Keep personal content, tokens and reference account data
  out of committed screenshots, reports and fixtures. Use synthetic test data.
- Implement Orhan-owned service behavior. Teamly's private backend and provider
  credentials are not supplied by REA. Identify missing configuration honestly.

REA's [broader-investigation guidance](https://github.com/morluto/rea/blob/a6caa79bf4f393c02601fbf2f6df246eae0b1558/.agents/skills/reverse-engineer-anything/SKILL.md#L460)
uses a staged investigation, evidence-backed questions and explicit unknowns.
The implementation order above is this project's chosen delivery sequence.

## Data and behavior requirements

- Use real persisted data for lists, counts, filters and badges. Do not substitute
  sample memories, hardcoded activity counts, no-op actions or simulated success.
- Preserve saved data through backward-compatible migrations. Test older data
  formats, restart, edits and deletion. Await writes before reporting success or
  closing an editor; preserve the draft and show an error when a write fails.
- Automatic memory needs explicit enable/pause controls, source-chat references,
  edit/delete controls and verified persistence. Preserve existing consent
  choices. Do not silently begin extracting all chats or sending local topics.
- Withhold personal context until its pause setting is loaded; a preference read
  error must not unpause memory. Keep one canonical pause store.
- Distinguish private profile fields, personal context, topics, tasks, artifacts
  and meeting data. Connect their owners deliberately instead of duplicating
  records or treating one type as another.
- Finish actual recording, minutes and playback before claiming meeting support.
  Clearly record unsupported provider/file capabilities as gaps.
- Use the existing Compose/Material 3 architecture and theme tokens. Check light
  and dark themes, compact screens, large text, readable contrast, accessible
  control labels and usable touch targets. Preserve keyboard/Back behavior.
- Never commit credentials or personal data. Preserve authentication and
  owner/user isolation when adding service calls or storage.

## Verification and handoff

For each code slice, run relevant behavior and persistence tests, the debug build
and lint from the repository root using the existing Gradle wrapper:

```sh
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
./gradlew :app:lintDebug
```

Use `.\gradlew.bat` in Windows PowerShell. Target tests while iterating; run the full
unit suite before the stage handoff. Separate pre-existing failures from new ones
using evidence. Do not change toolchain/signing configuration just to bypass a
failure. Documentation-only changes need content/link review and
`git diff --check`, not an Android build.

Before updating a phone, verify application ID, signer compatibility and a valid
non-downgrade versionCode; use a higher versionCode for an update release. Preserve
the existing version derivation and signing configuration. Use a data-preserving
update when compatible. Never uninstall or clear app data to bypass a signer or
version mismatch without explicit user authorization.

Verify the actual updated build on the device. Capture sanitized light/dark
results for the implemented flows, including persistence and failure paths. An
old installed build, preview render or passing unit test is not proof of the
updated phone behavior. If no device is available, record device checks as pending.

Every PR/handoff must state the approved reference version, active phase, task
phase/feature IDs, completed slice, changed behavior, exact checks and results,
evidence location, remaining gaps/blockers and the next in-phase slice. Update
TEAMLY_PHASE_STATUS.md with the same status and evidence.
Mark checklist items verified, partial, blocked or not started; do not describe
the complete Teamly feature set as done while required behaviors remain open.