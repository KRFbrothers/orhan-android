# Service integration source status — 2026-11-02

## Pull request dependency map

PRs #93–#107 are closed and merged into their respective feature-base branches. They are not all on Orhan `main`, which remains at PR #90. Dua #504 and #505 are merged to Dua `main`; deployment/configuration and runtime behavior remain unverified. PR #108 (persisted-message search) and #109 (integration of #106 durable resume onto the meeting chain) are open. No PR was merged or service deployed as part of this work.

## Agent tool policy

- Dua `apps/ai-engine/app.py` exposes authenticated `GET /api/agent/capabilities` and validates `allowedTools` on Agent `POST /api/chat`; `agent_tools.py` and `agent_adk.py` apply the allowlist during execution. Orhan now persists the selection, sends it with the API request, preflights the catalog, and uses the server response for disabled selections.
- Dua implementation is merged through [PR #504](https://github.com/KRFbrothers/Dua-ai/pull/504). Its hosted deployment/configuration is unverified. Orhan client integration is merged through [PR #102](https://github.com/KRFbrothers/orhan-android/pull/102), based on PR #101's feature branch. Runtime discovery/enforcement is not claimed.

## Workflow execution

- Existing workflow records remain format v2, unchanged. A new independent DataStore records up to 100 run entries, including saved workflow/agent IDs, status, timestamps and failure reason.
- The Agents tab now has a Run action. It starts a request through the existing Orhan chat path; an Orhan workflow can use the existing `HANDOFF: Agent | task` path to route to a saved specialist, and a workflow attached to a saved agent opens that agent's own thread. The ordinary `/api/chat` service contract and per-agent tool selection are reused. Chat messages remain the run transcript, with existing stop/cancel behavior.
- Queued/running records become interrupted after process restart; failures, cancellations, success and missing/deleted agents are recorded. The Workflow editor shows recent run history and opens the chat transcript. Legacy workflow records and local chat threads are retained.
- Dua's engine has owner-scoped durable ADK sessions (`resumeRun`) for individual agent requests. A new owner-authenticated Node API and RLS table provide durable workflow run metadata snapshots; Orhan keeps its compatible local ledger and mirrors it when a normal Supabase session is available, then merges remote history on authenticated startup. Chat content remains in the existing chat store. This does not execute workflows server-side: there is still no workflow definition/execution endpoint, background worker, or scheduler/trigger endpoint. Scheduled triggers remain blocked on a workflow execution contract and scheduler integration. The API migration is not applied or runtime-verified.
- Added workflow run serialization coverage; not run.

## Translation and transcription

- Message Translate uses the existing authenticated Orhan Cloud Run engine's configured model via plain `mode: auto`. Audio Transcribe uses Orhan's `DUA_API_URL` and the normal Supabase session: request an owner-signed upload, PUT the original local attachment, call Dua `POST /api/files/transcribe`, then request deletion of the temporary object. A manually requested transcript is saved with the original audio message in chat-thread format v7; v1–v6 threads remain readable. Persistence failure keeps the transcript in the open result dialog and reports the error.
- Dua PR #505 is merged to Dua `main`; deployment and `DUA_FILES`/Supabase/provider configuration are not verified. The route uses the existing configured Grok/Gemini/OpenAI transcription adapters. No provider response or cleanup has been runtime verified.
- Request-shape coverage was added, not run. Provider availability/live response is unverified.

### Transcript persistence follow-up

An explicit Transcribe action now persists the returned transcript onto the source audio message in local ChatThread format v7. The attachment remains in the separate file-backed store. The reader remains compatible with v1–v6 and v6's allowed-tool handoff field. After a DataStore failure the result remains in the open dialog and reports failure; the transcript is not shown as saved. Transcription is still user-requested, not automatic. Tests were added but not run.

## Meetings

- Orhan offers **Create meeting minutes** for a persisted audio chat message. On the #105 source branch, transcription uses the configured Grok/Gemini/OpenAI provider through the owner-authenticated Dua stored-file API; factual minutes are then produced by the existing Gemini engine. The result is separate from the source message; it can be saved as a typed Meeting minutes Artifact linked to the original chat/message, and explicit bullet items under `Action items` can be saved as Memory actions with that same source reference. Writes report failure and do not replace the source recording.
- The source recording remains the existing chat attachment in the device's private chat store with the 4 MiB cap. The transcription route uploads a temporary private Storage object and requests deletion after provider completion; Orhan surfaces failures to confirm removal. User-requested transcripts persist with the source chat message. Meeting minutes remain reviewable and can be saved as typed Artifacts and source-linked Memory actions.
- There is no meeting/session object, retained cloud meeting recording, meeting playback timeline, speaker/attendee provider, transcript-to-audio sync, automatic minutes trigger, or independently resumable meeting run. `apps/api/src/core/advanced-integrations.mjs` does not advertise a meeting service; `/api/voice` live sessions and TTS are separate. These gaps require a meeting service/data contract. Runtime provider configuration and temporary-object cleanup remain unverified.
- Meeting action extraction only accepts explicit bullet or numbered lines beneath `Action items`; no model prose is auto-saved or executed. The user can review minutes before saving any items.

## Relevant merged PRs and current slices

- [Orhan #102](https://github.com/KRFbrothers/orhan-android/pull/102): per-agent tool client wiring, plus chat Translate and Transcribe audio; merged to its feature base.
- [Orhan #103](https://github.com/KRFbrothers/orhan-android/pull/103): manual workflow execution and local history; merged to its feature base.
- [Orhan #104](https://github.com/KRFbrothers/orhan-android/pull/104): local meeting minutes and source-linked Memory actions; merged to its feature base.
- [Dua #504](https://github.com/KRFbrothers/Dua-ai/pull/504): server-side per-agent capability catalog and enforcement. Merged to Dua `main`; deployment not verified.
- [Dua #505](https://github.com/KRFbrothers/Dua-ai/pull/505): owner-scoped standalone stored-audio transcription using existing provider adapters. Merged to Dua `main`; deployment/configuration not verified.
- [Orhan #105](https://github.com/KRFbrothers/orhan-android/pull/105): calls the stored-file transcription API and cleans up the temporary upload; merged into the meeting feature branch.
- Orhan #106 (durable workflow-session resume) merged into `feat/workflow-run-execution`; PR #109 ports that implementation onto the #104/#105/#107 chain.
- [Orhan #108](https://github.com/KRFbrothers/orhan-android/pull/108): ports the previously merged #99 message-search behavior onto the #104/#106/#107 chain and adds persisted transcript matching plus source indicators; open, validation deferred.
- [Orhan #109](https://github.com/KRFbrothers/orhan-android/pull/109): integrates the already-merged #106 durable `resumeRun` support into the #104/#105/#107 chain; open, validation deferred.
- [Dua workflow run history API](https://github.com/KRFbrothers/Dua-ai/pull/506): owner-authenticated, RLS-backed workflow metadata list/upsert route; runtime/deployment/migration application not verified.
- [Orhan workflow run history client](https://github.com/KRFbrothers/orhan-android/pull/110): keeps local history and adds authenticated cloud mirroring/reconciliation; source-only, validation deferred.

## Validation status

Source review and `git diff --check` only. Focused workflow-run, typed-artifact, memory action-batch and meeting action-parser tests were added but not run. Build, lint, emulator and USB/device checks: **NOT RUN—deferred by user**. No service was deployed and no runtime success is claimed.
