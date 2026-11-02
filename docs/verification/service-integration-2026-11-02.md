# Service integration source status — 2026-11-02

## Pull request dependency map

PRs #93–#101 are closed and merged into their respective feature-base branches, not all into `main`. This source line continues with Orhan #102 (based on #101), workflow #103 (based on #102), and meeting #104 (based on #103). Dua #504 is a separate backend contract PR; it is not merged or deployed. No PR was merged as part of this work.

## Agent tool policy

- Dua `apps/ai-engine/app.py` exposes authenticated `GET /api/agent/capabilities` and validates `allowedTools` on Agent `POST /api/chat`; `agent_tools.py` and `agent_adk.py` apply the allowlist during execution. Orhan now persists the selection, sends it with the API request, preflights the catalog, and uses the server response for disabled selections.
- Dua implementation is in [PR #504](https://github.com/KRFbrothers/Dua-ai/pull/504). It is not merged or deployed. Orhan client integration is in [PR #102](https://github.com/KRFbrothers/orhan-android/pull/102), based on PR #101's feature branch. PR #102 requires #504 deployment before runtime discovery/enforcement can succeed.

## Workflow execution

- Existing workflow records remain format v2, unchanged. A new independent DataStore records up to 100 run entries, including saved workflow/agent IDs, status, timestamps and failure reason.
- The Agents tab now has a Run action. It starts a request through the existing Orhan chat path; an Orhan workflow can use the existing `HANDOFF: Agent | task` path to route to a saved specialist, and a workflow attached to a saved agent opens that agent's own thread. The ordinary `/api/chat` service contract and per-agent tool selection are reused. Chat messages remain the run transcript, with existing stop/cancel behavior.
- Queued/running records become interrupted after process restart; failures, cancellations, success and missing/deleted agents are recorded. The Workflow editor shows recent run history and opens the chat transcript. Legacy workflow records and local chat threads are retained.
- Dua's engine has owner-scoped durable ADK sessions (`resumeRun`) for individual agent requests but no workflow definition/run/history API or scheduler/trigger endpoint. There is no supported scheduled trigger configured in either app/service source. The implemented manual run ledger is device-local; server-side resumption, cross-device history and scheduling are not claimed. Those remain blocked on an owner-scoped workflow service and scheduler contract.
- Added workflow run serialization coverage; not run.

## Translation and transcription

- Message Translate and Transcribe audio actions use the existing Orhan Cloud Run engine's plain `mode: auto` chat request. The service source accepts audio inline and constructs Gemini content from it. This is a configured model request, not a local transcript or a changed message; the result is shown separately. New service actions use the normal Supabase session JWT, without adding a development token path.
- The separate Dua Node API has owner-scoped private `/api/files/*` storage and audio transcription inside `file-context.mjs` while handling stored-file chat. Provider order can use configured Grok, Gemini or OpenAI credentials. Main did not yet expose a standalone transcription route; the new route is proposed in Dua #505 and Orhan wiring in #105, neither runtime-verified.
- Follow-up source integration adds owner-only `POST /api/files/transcribe` to Dua PR #505. Orhan PR #105 uses its existing `DUA_API_URL` setting: obtain an owner-signed upload, PUT the original attachment, transcribe through the configured provider, then request deletion of the temporary object. Existing local attachment bytes remain the source of truth. This depends on #505 being reviewed/merged and the Dua API deployed with existing file-storage and transcription-provider configuration; no runtime call has been made.
- Request-shape coverage was added, not run. Provider availability/live response is unverified.

## Meetings

- Orhan offers **Create meeting minutes** for a persisted audio chat message. On the #105 source branch, transcription uses the configured Grok/Gemini/OpenAI provider through the owner-authenticated Dua stored-file API; factual minutes are then produced by the existing Gemini engine. The result is separate from the source message; it can be saved as a typed Meeting minutes Artifact linked to the original chat/message, and explicit bullet items under `Action items` can be saved as Memory actions with that same source reference. Writes report failure and do not replace the source recording.
- The source recording remains the existing chat attachment in the device's private chat store and keeps the existing 4 MiB inline cap. For transcription, #105 temporarily uploads it to private Storage and requests deletion after provider completion; cleanup failure is surfaced. There is no meeting/session object, retained cloud meeting recording, separately persisted transcript, playback timeline, speaker/attendee provider, or independently resumable meeting run.
- The #105 remote transcription path requires Dua #505 to be deployed with stored-file Storage and at least one existing transcription provider key. `apps/api/src/core/advanced-integrations.mjs` still reports meeting recording/uploads unavailable as a meeting feature; `/api/voice` live sessions and TTS are separate, not meeting records. Runtime provider configuration and temporary-object cleanup are unverified.
- Meeting action extraction only accepts explicit bullet or numbered lines beneath `Action items`; no model prose is auto-saved or executed. The user can review minutes before saving any items.

## Current PRs

- [Orhan #102](https://github.com/KRFbrothers/orhan-android/pull/102): per-agent tool client wiring, plus chat Translate and Transcribe audio. Depends on #101 and backend #504 before restricted-tool runtime use.
- [Orhan #103](https://github.com/KRFbrothers/orhan-android/pull/103): manual workflow execution and local history. Depends on #102.
- [Orhan #104](https://github.com/KRFbrothers/orhan-android/pull/104): local meeting minutes and source-linked Memory actions. The provider-specific transcription follow-up is #105.
- [Dua #504](https://github.com/KRFbrothers/Dua-ai/pull/504): server-side per-agent capability catalog and enforcement. No merge/deploy.
- [Dua #505](https://github.com/KRFbrothers/Dua-ai/pull/505): owner-scoped standalone stored-audio transcription using existing provider adapters. No merge/deploy.
- [Orhan #105](https://github.com/KRFbrothers/orhan-android/pull/105): calls the stored-file transcription API and cleans up the temporary upload. Depends on Dua #505 and configured provider/storage service.

## Validation status

Source review and `git diff --check` only. Focused workflow-run, typed-artifact, memory action-batch and meeting action-parser tests were added but not run. Build, lint, emulator and USB/device checks: **NOT RUN—deferred by user**. No service was deployed and no runtime success is claimed.
