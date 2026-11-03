# Workflow integration follow-up — 2026-11-03

User-directed scope: complete available workflow integration source, evaluate
unsupported capabilities and prepare a concrete deployment/rollback plan.
Automated tests, instrumentation and emulators remain prohibited. No production
migration, deployment, worker/session provisioning or PR merge was performed.

## Starting state / dependencies

- Inspected Orhan/Dua AGENTS, clean branch states, source/history and live PR
  metadata. Dua #507 is merged at `50dc8843`; Orhan #112 is merged into
  `feat/meeting-minutes-actions` at `7215d1e`. This does not establish deployment.
- Follow-up branches start at those merged bases. Original Dua checkout's
  unrelated `apps/android/gradle/libs.versions.toml` edit is preserved.
- Existing Memory pause/context and local prompt/run/chat owners are retained.
  Explicit workflow submissions include their agent instructions/tool snapshots,
  without automatically uploading private Memory or local topics.

## Completed / partial source

| Slice | Source state | Remaining evidence |
| --- | --- | --- |
| Worker identity | Dedicated existing-owner Auth session, encrypted durable token pair, fenced refresh rotation, provision/revoke commands; access-JWT-file design rejected | Real owner session, secrets and session-policy configuration; rotation/revocation runtime checks |
| Supervision / recovery | Node 24 startup validation, systemd/environment templates, graceful cancellation, independent expired-lease reaper, serialized checkpoints and wall-clock lease fences | Supervisor/host installation, schema application, restart/kill concurrency evidence |
| Recurrence | Fixed UTC interval registration and atomic tick; missed-slot coalescing and overlap skip; stored registration snapshots | Migration/API/worker deployment and actual due-time/restart checks |
| External triggers | Owner registration, per-trigger HMAC, five-minute timestamp window, pinned owner UUID and durable event-to-run deduplication | Signing keys, provider/bridge configuration and signed-delivery runtime evidence |
| Android integration | Strict five-state models, persisted idempotent recurring requests, create/list/disable controls, actual next-due/skip metadata; manual controls survive automation API failure | Kotlin compilation and updated-phone checks |
| Write continuation | **Unsupported**, explicit selected-tool rejection preserved | New durable outcome/reconciliation API and immutable workflow approval binding/cancellation contract; current shared approvals remain unchanged |

## Request/response cross-check

| Operation | Android source | Backend source |
| --- | --- | --- |
| Start / history / output | Existing `/api/workflow-executions`, persisted original request/key, `{run}` / `{runs}` parsing | Owner-authenticated normalized snapshot; server state/output only |
| Retry / cancel | Sends `expectedAttempt` and parses the returned run | Same-run retry and stale-attempt protection; cancellation remains pending until acknowledged |
| Status | `ExecutionStatus`: queued/running/succeeded/failed/cancelled | Same five run/step wire states; unknown client states fail visibly |
| Recurring create | Persistent stable UUID/envelope before `PUT /api/workflow-automation/schedules/:id`, `expectedRevision:0` | Idempotent creation, immutable start anchor; revision required for edits |
| List / disable | `{registrations}`, `{registration}`, interval seconds, nextDue, lastSkippedAt and `expectedRevision` | Same owner-scoped shapes; disable remains available during execution pause |
| External delivery | Sender configuration stays server/operator-owned; UI lists/disables registrations | Signed `/api/workflow-automation/deliver/:id`; only stable eventId accepted |

Existing workflow v2 and local run v2 formats are unchanged. Pending ordinary
submissions keep their existing DataStore keys; recurring envelopes use a new
account-scoped key. Unknown statuses cannot enable a control by being mistaken
for success. Server automation errors are separate from normal history errors.
Recurring runs use their submitted snapshot; local edits do not silently update
a registration. Disabling future activity and cancelling a current run are
deliberately separate operations.

## Focused source review

- Owner identity: Auth verification, fixed worker UUID, existing allowed-owner
  email and explicit tools remain enforced. Encryption AAD binds project/owner.
  Only service role can read credential ciphertext or mutate execution states.
- Refresh loss: a 90-second refresh lease and credential revision prevent two
  workers rotating the same token. Unknown grants fail closed; commit-loss does
  not overwrite an already advanced credential. Provisioning is an operator
  maintenance operation with workers stopped, not a public delegation route.
- Execution loss: reaper does not need a usable owner JWT, and does not start
  work. Run lease checks use wall clock after locking; succeeded runs require
  complete saved outputs. Provider outcomes may still be unknown after a crash.
- Duplicate starts/slots/events: ordinary owner lock and unique-active constraint
  are reused; schedule ticks advance the persisted slot transactionally. Event
  delivery ledger returns the original run even after registration edits.
- Cancellation: local heartbeats/checkpoints are serialized, server fencing and
  attempt checks prevent old worker/control writes from changing a new attempt.
  In-flight external operations cannot be undone; exactly-once provider behavior
  is not claimed. Disable only affects future activity.
- Write approvals: existing engine single-use claims happen before side effects,
  without durable outcomes/workflow binding. No worker auto-approval, replay or
  inferred handoff result was introduced. This remains a source gap, not an
  external configuration blocker disguised as completion.

## Exact preparation and checks

- Prepared SQL: `20261102183410_workflow_identity_recovery.sql`, then
  `20261102183411_workflow_automation.sql`, after #507's
  `20261102180726_workflow_execution.sql`. CLI generated filenames; no SQL query
  or migration application occurred. #506's metadata migration is separate.
- `node --check` passed on eight source files: the five workflow core modules
  (`identity`, `runtime`, `worker`, `automation`, `execution`), service-handler,
  and the identity/automation test source files. Test bodies were not executed.
- Source review and `git diff --check` passed; staged checks are recorded in PRs.
- Automated tests, Android build/lint, instrumentation, emulator, database
  concurrency queries, provider requests and fresh phone checks: **NOT RUN**.
- Added backend identity/automation test source is preparation only. It does
  not prove database transactions, provider behavior, UI layout or persistence.

## Phone verification — separate unchanged result

The [version-341 phone report](phone-verification-2026-11-02.md) still records
local queued → normal sign-in for workflows. No new APK was built or installed
and no new physical-device flow was checked. Neither merged PRs nor source
syntax checks establish runtime success on that phone.

## Integration plan and remaining blockers

Full migration order, service configuration, exact secret names, external
signing protocol, supervisor setup and rollback are in the backend
[integration plan](https://github.com/KRFbrothers/Dua-ai/blob/feat/workflow-automation-integration/docs/integrations/workflow-integration-plan.md).
Identity/supervisor work is saved in [Dua #509](https://github.com/KRFbrothers/Dua-ai/pull/509);
automation is [Dua #510](https://github.com/KRFbrothers/Dua-ai/pull/510), stacked
on #509; [Orhan #113](https://github.com/KRFbrothers/orhan-android/pull/113)
targets the already integrated #112 branch. No merge is requested/performed by
this report. The three follow-up PRs are drafts with source-only evidence.

Required external setup: actual target migration/deployment inventory, server
service/app/encryption secrets, a dedicated confirmed owner refresh session and
suitable Auth policy, Node 24 supervisor host, deployed enforced-capabilities
engine, trigger signing keys and a provider/bridge configuration. No external
token-rotation broker source dependency remains; uncertain rotation still needs
operator reauthorization. Provider-specific subscriptions, calendar cron/DST,
dynamic routing, write approval continuation and Android global history after
local workflow deletion remain unsupported/source gaps.

Next: review this prepared PR chain and exact plan; establish separately
authorized staging configuration. When the test stop is lifted, verify real
rotation/revocation, duplicate delivery, lease/worker restart, cancellation,
recurrence and tool refusal, then a compatible Android update in both themes.
Keep all runtime/deployment checklist items pending until that evidence exists.
