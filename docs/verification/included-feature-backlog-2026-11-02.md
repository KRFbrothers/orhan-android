# Included-feature source backlog — 2026-11-02

This is a source inventory for planning, not a runtime or parity report. `TEAMLY_FEATURE_REFERENCE.md` is reference material; its labels and third-party handbook claims are not Android observations. No tests, build, lint, emulator, USB, or device checks were run for this update (**NOT RUN—deferred by user**).

## Stage order

The active work has been Agents, following the earlier Artifacts work. Continue with Chats/voice/attachments, then integration. Memory remains an earlier-stage concern: do not rewrite completed behavior without a concrete source defect. Meetings stay an included gap inventory until actual recording, minutes, and playback can be implemented with provider/data contracts.

## Source coverage and next slices

| Reference scope | Current Orhan source (static finding) | Status | Next actionable work / external gap |
|---|---|---|---|
| F1.1 text, mentions | Chat composer sends to `/api/chat`; model handoff uses roster/instructions. No dedicated mention picker or selected-agent insertion flow found in the source pass. | Partial | Inspect composer token behavior and add a real agent mention picker bound to saved roster; avoid claiming mention semantics parity until reference/runtime evidence exists. |
| F1.2 message actions | Long-press supports copy/share and save agent reply as artifact; user message supports action/to-do path. No complete reply/forward/translate/transcribe/react/unsend/report implementation identified. | Partial | Add only actions with local/service owners; translate/transcribe/report/reactions need explicit API/provider semantics. Social reactions remain excluded by AGENTS.md. |
| F1.3 attachments | File picker allows supported MIME types and inline request payloads; bytes and metadata persist in local chat history. Restored attachment payload was not wired into message display; this slice adds bounded image thumbnail decode. PDF/document open/export and video playback are not implemented by this preview. Camera capture is absent. | Partial | Add safe supported document open/share using app-owned content URIs, then establish upload failure/retry semantics. Camera/video viewer require implementation and device validation. |
| F1.4 voice | Android speech recognizer is used for voice typing into composer; this is not a recorded voice-message feature. | Partial | Real recording, audio payload upload, waveform/duration, playback and transcription require a service contract and audio player/recorder path. No fake voice bubble. |
| F1.5 history/search | Local persisted title-keyed threads and search are present. Attachment bytes persist locally within bounded store policy. | Partial | Verify ordering, cross-chat search results, source jumps and deletion edge cases later; reference-only result semantics remain unknown. |
| F1.6 conversation controls | Pin/unpin, remove recent and clear/remove thread paths exist in local stores/UI. Mute and remote delete semantics not established. | Partial | Inventory confirm dialogs/failure behavior and any local-only controls; mute/remote deletion require product/service semantics. |
| F2.1–F2.3 agent identity/persona | Local create/edit/rename/delete, private avatar, profile, persisted instructions, per-agent chat and handoff roster exist. Avatar metadata schema is v3 with v1/v2 reads. Delete write failure is surfaced and confirmation is retained. | Partial | Stable agent IDs/config snapshots are absent; legacy thread history is keyed by names. Deleted/renamed history is retained and now uses Orhan for future replies with a notice; old persona cannot be reconstructed. |
| F2.4 capabilities | Legacy string preferences are preserved but not sent as prompt guidance or enforced. `/api/chat` has no allowlist/catalog contract in this client. | Blocked | Shared service must define capability IDs, supported-capability discovery, enforcement and unsupported-tool errors. Do not change another repository without explicit task scope. |
| F2.5 workflows | Local reusable prompt records with name resolution, stale-agent notice and chat-prefill launch. | Partial / blocked | Multi-step Agent Flows, triggers, scheduling, execution log and durable retries need workflow execution/provider contracts. Current local prompt flow is not equivalent. |
| F2.6 handoffs | Orhan can hand off to saved agents, display specialist replies, surface failure and retry; interrupted retry state persists in thread v4 with v1-v3 read compatibility. | Partial | Validate ordering/cancellation and deleted/renamed specialist during pending retry; stable identity snapshots or explicit stale-target handling are needed. |
| F3.1–F3.8 Memory | Private memory store, topic entries, pause controls, private profile/context, actions and source connections are present from prior Memory work. | Partial; prior slices preserved | Read `artifacts-2026-11-02.md` and source owners before edits. Remaining server-driven extraction, graph and update semantics must be separated from local support; only fix concrete actionable gaps. |
| F4.1–F4.7 Artifacts | Typed Markdown/HTML/document source and rendering/export/share plus saved-message provenance were implemented in preceding Artifacts work. Coverage and unknown media/Studio behaviors are tracked separately. | Partial | Keep PR #93 artifact-only. Media payload types, DOCX conversion and collaborative Studio behaviors need actual payload/reference/service evidence. Check integration links without rewriting completed slices. |
| F5.1–F5.3 Meetings | No complete recording→minutes→playback feature found in this source pass. | Not implemented / blocked | Requires recording permissions/lifecycle, transcription/minutes provider, durable meeting ownership and playback source. Implement only when those contracts are available. |

## Source-only changes in progress

- Deleted/renamed custom-agent chat keeps the old thread title/history, but resolves future requests to Orhan instead of impersonating an unavailable saved persona. A visible notice explains this behavior.
- Persisted image attachments are decoded off the main thread and rendered with dimension bounds in the message; non-image payloads remain labeled by MIME type/name. This is not a PDF/video/audio viewer.

## Concrete blockers

- `/api/chat` selected-tool enforcement and capability catalog contract.
- Agent Flows execution/trigger/scheduling contract; current workflow persistence stores reusable prompt text only.
- Stable agent identifier/migration policy for existing name-keyed chats and name-linked workflows.
- Voice message audio recording, upload, playback and transcription service contract.
- Meeting recording/minutes/playback data and provider contract.
- Reference evidence for media artifacts and collaborative Studio features. Existing reference prose remains unverified behavior.

## Validation status

`git diff --check` is run for the source slices. All tests, build/lint, emulator and connected-device checks are **NOT RUN—deferred by user**. Do not use older reports as evidence for the new changes.
