# Durable workflow execution — source work

User-directed scope: workflow execution (Agents), superseding the earlier stage order for this slice. Automated tests, instrumentation and emulators are deferred by the user. No deployment, production migration or merge is authorized.

## Inventory before changes

| Owner / entry | Existing source | Gap |
| --- | --- | --- |
| Orhan Agents → Workflows → Run | Saved prompts v2, local runs v2, foreground chat execution and resumable ADK session IDs | No server workflow orchestration; chat completion is not multi-agent execution |
| Dua #506 (merged) | Owner-authenticated metadata GET/PUT and owner RLS | Client-writable snapshots cannot be authoritative execution state |
| Engine `/api/chat` | Owner authentication, explicit allowedTools enforcement, approvals and disconnect cancellation | No durable workflow queue or scheduler |
| Phone / Orhan #111 (merged) | Version 341 normal UI observations | Workflow reached sign-in after local queued record; no server execution verified |

Work starts from clean Orhan `f28ce4b`, on `feat/durable-workflow-client`. Backend worktree starts from Dua #506 source `3765d04f`, on `feat/workflow-execution`; unrelated Dua dependency edit is preserved in its original checkout.

After fetching PR refs, the Android branch was fast-forwarded to the already
merged #111 base `9dfb712`. That brings in `febd5d8`'s existing avatar lint fix;
it is not reimplemented by this slice. Android PR target is
`feat/meeting-minutes-actions`; backend PR target is `main`.

## Phone verification — separate result

See [prior physical-device report](phone-verification-2026-11-02.md). Its workflow result remains **partial / blocked by normal sign-in**. No new APK was built or installed for this phase. No fresh phone verification of these changes has occurred. Prior `queued` history is not execution success. Existing Memory persistence/pause implementation was inspected through navigation and retained; workflow requests do not automatically upload personal memory or topics.

## Current verification

Source review completed for the submitted source, including engine request
fields, owner checks, capability enforcement, idempotency and cancellation
races. `git diff --check` passed in both trees; staged checks are recorded in
the PR handoff. Runtime execution, migration, worker scheduling, provider calls,
restart recovery, cancellation races and light/dark UI checks remain unverified.
Tests/build/lint/instrumentation/emulator: **not run (user instruction)**.

## Completed source slice

| Capability | Status / source behavior |
| --- | --- |
| Authenticated start, status, history, cancel, retry | Source complete; new `/api/workflow-executions` owner API, separate from client-writable legacy metadata |
| Durable run ID and step states | Source complete; Postgres queue and ordered JSON step records, owner RLS, service-only mutations, queued/running/succeeded/failed/cancelled |
| Agent execution and handoff | Source complete for ordered steps; worker calls actual Agent SSE endpoint, saves complete output before passing it to the next agent |
| Duplicate prevention | Source complete; persisted phone submission snapshot/key, server fingerprint, unique active workflow, row locks and attempt-checked controls |
| Retry / cancellation | Source complete; explicit same-run retry retains succeeded outputs; fenced worker lease and abort propagation; unknown provider outcomes fail closed |
| Worker / triggers | Partial; supervised worker source, manual/authenticated event/one-shot due-time handling; external credential rotation and hosting not provisioned |
| Tools / approvals | Partial; explicit selected read-tool subset checked against real engine capabilities; unsupported writes are rejected, existing Agent approval controls are never bypassed |
| Android controls | Source complete for saved workflow editor; actual server history, step status/output, stale-history errors, refresh, retry confirmation, cancel acknowledgement and one-shot UTC input |
| Legacy saved data / Dua behavior | Existing workflow/run/chat serializers and metadata API unchanged; shared chat/auth/tool implementation unchanged; no Memory/topics automatically attached |

Prepared backend migration:
`infra/database/migrations/20261102180726_workflow_execution.sql`. The Supabase
CLI generated its timestamped file; it was placed in Dua's canonical migrations
directory. No SQL was applied or queried. Supabase documentation was checked for
[RLS](https://supabase.com/docs/guides/database/postgres/row-level-security) and
[function privileges](https://supabase.com/docs/guides/database/functions).
No Supabase SDK dependency was added.

Added backend test source covers owner spoofing, origin rejection, tool denials,
stale-attempt forwarding, kill-switch cancellation, stream completion/error/
truncation/UTF-8 handling, two-agent request/output handoff, and cancellation
before dispatch. These tests have **not run**. Transactional concurrency checks
and actual engine/provider execution remain pending; source tests do not prove
database locking behavior.

## Deployment requirements and exact blockers

- **Blocked:** new migration has not been applied, and new API/worker source
  has not been deployed. Merged Dua #506 is only legacy metadata history.
- **Blocked:** no supervised long-lived Node 24 worker is provisioned/enabled.
  Configure `DUA_WORKFLOWS_ENABLED=1` only after dependencies are ready.
- **Blocked:** worker needs its server-only Supabase service key, owner lock,
  engine URL/app-token configuration and `DUA_WORKFLOW_OWNER_TOKEN_FILE` mounted
  from a credential broker that rotates a verified owner's access JWT. The
  broker is not implemented/provisioned here. Without rotation, unattended
  schedules stop at JWT expiry. No client refresh token is retained for this.
- **Blocked / runtime unknown:** deployed engine must support capability
  discovery and enforced `allowedTools`. Existing phone UI reported this
  contract unavailable; source presence does not prove the deployed contract.
- **Pending by user:** automated checks and a fresh authorized Android build
  have not run. No new APK/phone flow was verified; normal phone sign-in also
  remains required for online execution.
- **Not started:** recurring cron, external event subscriptions/adapters,
  model-chosen dynamic routing and workflow write-approval continuation.
  Unsupported tools/triggers return actionable errors.

Additional handoff-agent choices are per-submission drafts; the backend retains
the execution snapshot, while the existing saved-prompt workflow format stays
unchanged. Deleted local workflows retain server runs; global server history is
available through the API, while Android history currently opens through a saved
workflow editor. Exactly-once external provider behavior is not claimed.

Next slice: provision the authorized staging dependencies and owner credential
rotation, then (when tests are permitted) verify real two-agent execution,
duplicate deliveries, worker restart/lease recovery, queued/running cancellation,
retry persistence, capability denial and both Android themes. Keep the draft PRs
unmerged until those checks have evidence.
