# Orhan implementation guide

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

## Required implementation order

First make a compact inventory of the included reference features, existing
Orhan capabilities, dependencies and unknowns. Then work through this order:

| Stage | Focus | Required work |
| --- | --- | --- |
| 0 | Feature map | Record entry points, observed flows, existing owners and gaps; identify shared prerequisites. |
| 1 | Memory | Verify and finish topics, search, tags, groups and migration; then private profile/context and pause; then automatic memory and updates; then actions and artifact connections. |
| 2 | Artifacts | Finish creation/saving, list/search, detail, editing, deletion and export/share for supported deliverables. |
| 3 | Agents | Finish creation/editing, instructions, actual tool behavior, agent chats, workflows and observed handoffs. |
| 4 | Chats, voice and attachments | Close remaining history, pin/search, composer, message-action, streaming, cancellation, attachment and voice gaps. |
| 5 | Integration | Verify end-to-end flows across chats, agents, memory and artifacts, including restart, errors and both themes. |

Within a stage, deliver small working slices: inspect, specify, implement, test,
compare on the device, and commit. Keep one implementation slice active at a
time. Earlier stages may change a later feature only for a concrete dependency,
such as the chat hook needed for automatic memory. Document that dependency and
keep its changes scoped; do not postpone essential wiring until the final stage.

Record remaining work after every slice. Advance when the slice is verified. If
required credentials, reference access or hardware block it, mark it blocked with
the exact missing evidence, then continue independent work within the current
stage. Proceed to another stage only when the user directs a scope change or all
remaining work in the current stage is explicitly blocked. A blocker never counts
as completion. Continue authorized work without asking for confirmation after
every routine step.

## Reference inspection with REA

- Use the connected REA server's actual tool inventory and schemas. Installed
  instructions or repository documentation do not establish tool availability.
- Verify the selected USB device and reference package before acquisition or
  capture. USB attachment to the user's laptop does not grant a remote workspace
  access to that phone.
- Reuse existing captures and evidence. For each flow record the entry point,
  actions, state/data changes, result, error states and remaining unknowns.
- Distinguish observed behavior, static findings, inference and unverified
  behavior. Screenshots establish visible layout; decompilation alone does not
  establish runtime behavior. Do not invent behavior in unseen screens.
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

Every PR/handoff must state the completed slice, changed behavior, exact checks
and results, evidence location, remaining gaps/blockers and the next slice.
Mark checklist items verified, partial, blocked or not started; do not describe
the complete Teamly feature set as done while required behaviors remain open.
