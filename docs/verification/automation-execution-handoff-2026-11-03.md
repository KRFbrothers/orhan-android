# Automation execution handoff — 2026-11-03

## Baseline and observed reference

PR #114 was already merged at c23b5a7 before this slice; working tree was clean.
REA read/capture access now succeeds after the user resumed the client. The
earlier policy rejection is historical, not a current blocker.
Selected device 9cefa3cb was available; package ai.teamily.mobile was version
2.0.20/code 234. Device transport/kind remained unknown in REA metadata.

Fresh normal UI observations:
- Intent selector: Research, Monitor, Create, Analyze, Plan, Coordinate.
- Schedule selector: Run now, Later, Daily, Weekly, Monthly, Weekdays.
- Team section: selected personal agent, Add Agents and Add People controls.
- Scope selector: One-shot and Standing; Apply is visible.
No Generate Prompt, Apply, send, save or publish action was invoked. No
reference record was created. One Back navigation returned to the gallery;
the subsequent tap opened the deck/report starter, so schedule/team/scope
observations belong to that starter, not the daily brief. Option selection
effects, schedule persistence, delivery, Standing execution, team collaboration
and restart are still unverified. Create's social publishing text is excluded
from Orhan's implementation.

Private original captures and complete inline evidence are ignored under
captures/teamly-workflows-2026-11-03/followup-evidence.json. No account data or
screenshots are committed.

| Capture | Evidence | SHA-256 |
| --- | --- | --- |
| resumed | `ev_af4b179ea3c47d8d9b02d8e3356ee4a4ee273cbb4aacc9abaf1a0c160b07d46a` | `0fb7e2449ab296da0bb78fadde3077b844be6d6f73944cb42f0a0fbb54675489` |
| intent-options | `ev_0ad7db12d4e274361cdef6d0062ea4d9f99cfef41dfe498ad3eaea087e4162dc` | `556781d9d58a4491176e1cd02eace387e36e212b42867d9dc3c372d5700975af` |
| schedule-selector | `ev_a40e0aa306f404048a30b396791265979953023e0cecff6686ee5fbea620eba8` | `d78f89bf90d34ab755036ebd0173f97e79b99419367d5c4440f65129880a7e30` |
| team-scope | `ev_656e98936d2dc75df9f67d3bdbb6a8e7acc9bbcbc7ec27e3ad3d0b444b05970c` | `142ed154bc3fd87ce36f0d72e9f59be933b09e7c9e036a24e6c7372f1b3a1ef4` |
| scope-selector | `ev_505af2f963068f80a6b0d355ac8dc5f4783712b0affeae5a6fc036ef791af4be` | `d8006d82c4e9bd4ff570d06dc4503407c65344bd1d87b4abb157b1690bd9bd14` |

## Source-completed slice

- Save & configure execution awaits the canonical workflow upsert, then opens
  the existing WorkflowEditorScreen for that durable workflow ID. The existing
  authenticated WorkflowExecutionPanel supplies handoffs, run status/history,
  retry/cancel, one-shot time and fixed-interval registration. The button itself
  neither submits a run nor enables a schedule. Generate Prompt still saves
  then opens the existing chat draft.
- Both actions share the busy guard, stable saved draft ID, deleted-agent check,
  capacity handling and failure-preserved draft. No second workflow store,
  credential path, fake progress, backend route or migration was introduced.
- Research and Analyze now compile distinct output instructions. Research
  templates default to Research. Existing Summarize is retained as an
  Orhan-specific choice. Create/Plan prompts require confirmed tool results
  before claiming saves, publishing, invites, assignments or calendar actions.
  These prompt instructions do not replace existing server capability/approval
  enforcement.
- The builder remains one-shot. Calendar Monthly/Weekdays, Standing goals,
  shared human coordination and reference option-selection parity are open.
  A fixed daily interval is not claimed to implement local calendar recurrence.

## Integration, verification and next work

This slice depends on merged #114 and the #112/#113 execution client in
feat/meeting-minutes-actions. No new service, secret, migration or deployment
configuration is required. Existing backend worker and auth deployment
requirements remain in the prior workflow preparation handoff; their deployed
state is not established here. Rollback is reverting this focused commit;
existing persisted workflow format and records remain compatible.

Source review checked both destinations use the same awaited save and current
agent-ID validation; authentication, owner scoping, duplicate submission and
cancellation remain owned by the unchanged repository/server controls.
No runtime guarantee is inferred from source review.

`:app:assembleDebug` passed in 2m 51s (38 tasks, 12 executed).
`git diff --check` passed. Lint was not rerun for this slice; the prior run
failed with two ProduceStateDoesNotAssignValue errors in unchanged
ChatScreen.kt:977/1037 and 53 warnings. No automated tests were run.

Phone verification: application ID ai.orhan, APK version 353 > installed 341;
both APK signer SHA-256 digests matched
8de8c2e46e572df1a7dcedce00b48a6ba0e53c042d52079c510f2923be88e06e.
ADB install -r succeeded without uninstall/clear; REA confirmed version 353,
unchanged user ID and first-install time. In light theme, normal UI verified
home + menu, gallery, Research default/live prompt, a synthetic task save,
navigation to the populated existing execution editor, and its saved item in
My Automations after force-stop/relaunch. The synthetic saved item is retained.
Complete evidence is ignored at captures/teamly-workflows-2026-11-03/orhan-evidence.json.

The execution panel displayed `History may be out of date. Not found.` and a
disabled Start server run. Source maps history to authenticated GET
/api/workflow-executions?workflowId=<saved-id> on BuildConfig.DUA_API_URL.
This establishes a visible service failure, not its root cause: deployed route,
configured origin and backend version must be checked before runtime execution
verification. No start/retry/cancel or provider execution was invoked. Dark
theme, altered font scale, capacity/save failure UI and successful server runs
remain unverified. Automated tests, instrumentation and emulator remain stopped by user
instruction. No production migration/deployment or PR merge was performed.
Next slice: diagnose the configured backend history failure and verify
execution; implement supported schedule selection against the canonical execution
contract, keeping calendar recurrence and Standing unsupported until their
backend semantics are implemented.

## Release-readiness follow-up — 2026-11-03

This section supersedes the earlier “lint not rerun” statement above for the
new Android commit only. It does not replace the older version-353 phone report.

### Current source and build

- Confirmed the artifact v3 reader stores the serialized kind string once
  before enum lookup.
- Confirmed the API imports `ProviderFileError` from the canonical
  `gemini-files.mjs` export and keeps `transcribe` imported from
  `transcribe.mjs`. The agent engine imports
  `dua_engine.agent.agent_models` by package path.
- Replaced the two remaining `produceState` attachment loaders with
  `remember` and `LaunchedEffect`.
- Android history errors preserve HTTP status and only log a sanitized
  origin/path, status, safe code/message and request ID in debug builds.
- Fresh Kotlin compile, `:app:assembleDebug` and `:app:lintDebug` passed on
  commit `aff357dd`. The APK is `ai.orhan` versionCode 355/versionName 0.2.0,
  signed with the existing debug certificate.
- The locked API dependency `cfb@1.2.2` is present under
  `apps/api/node_modules`. API source `node --check` and `git diff --check`
  passed; no lockfile changed. No Python runtime was available for a Python
  import check.
- Automated tests, instrumentation and the full test suite were not run.

### Emulator and requested UI flows

Four emulator processes were present, but the configured ADB client returned
an empty device list. The newest emulator's console port 5554 was listening
while its ADB bridge at 5555 refused connections. APK install was therefore
not attempted; no app data was changed. The current commit has no new device
UI evidence.

| Requested flow | Current commit status |
| --- | --- |
| Chat save/reopen and saved-message artifact | Not verified on this build |
| Plain text, Markdown, HTML create/edit/reopen/share/delete | Not verified on this build |
| Memory-to-artifact and source-chat navigation | Not verified on this build |
| Agent create/edit/select and workflow save/reopen | Not verified on this build |
| Restart persistence | Not verified on this build |

Use an ADB-visible, signed-in emulator and install versionCode 355 with
`adb install -r` after checking the installed package version and signer. Do
not clear or uninstall its data. The earlier version-353 UI evidence remains
historical and cannot substitute for this update.

### PR and integration order

- Android #115 remains draft on `feat/meeting-minutes-actions`. That base
  contains #112 durable history, #113 recurring controls and #114 the builder,
  but refreshed Git ancestry is 66 commits ahead of `origin/main`. Keep #115 on
  its current base until the upstream feature chain is reviewed and integrated;
  do not retarget it directly to main.
- Backend #507 and #509 are in refreshed `origin/main`. #510's automation
  source is merged into `feat/workflow-runtime-lifecycle` but is not in main.
  Draft Dua #511 now proposes that automation delta against main, with the two
  runtime import fixes. No PR was merged.
- Migration order remains `20261102180726_workflow_execution.sql`, then
  `20261102183410_workflow_identity_recovery.sql`, then
  `20261102183411_workflow_automation.sql`. Do not apply them until the target
  project ledger is inspected and each missing migration is separately
  reviewed/authorized.
- The previously observed workflow-history 404 remains a separate backend
  route/schema deployment blocker. No same history request was repeated and no
  server execution success is claimed.
