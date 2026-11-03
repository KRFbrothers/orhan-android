# Teamily approved phase and task status

## Current delivery state

- **Approved baseline:** [TEAMLY_FEATURE_REFERENCE.md](../reference/TEAMLY_FEATURE_REFERENCE.md),
  v2.0 — 2026-11-03, user-approved.
- **Active phase:** **Phase 1 — Chats (F1.1–F1.6)**, the first phase in the newly
  approved sequence. This is a delivery/baseline choice, not a claim that earlier
  implementations are absent or later phases were never worked on.
- **Current task phase:** **Phases 1–5 — reference governance**.
- **Current task:** install the exact approved reference, make it mandatory in
  agent entry instructions, and replace the obsolete phase-order guidance.
- **Current task evidence:** approved attachment and repository documentation
  inspection; content/hash/link checks are recorded in the change handoff.
- **Feature implementation in this task:** none. No new REA observation, Android
  test/build/device result or feature acceptance is claimed.
- **Next feature task:** Phase 1 — F1.1, inventory the existing send/composer/
  mention/draft paths against its approved checks, then resolve missing evidence
  with available REA tools before choosing a concrete implementation change.

The earlier Memory tag-filter recommendation belongs to **Phase 3 — F3.3**.
It remains a recorded source/reference question, not the active next slice under
the final phase order.

## Phase ledger

| Phase | Area | Approved features | Current status / exit evidence |
| --- | --- | --- | --- |
| 1 | Chats | F1.1–F1.6 | Active: baseline inventory and acceptance verification pending. Existing chat/voice/attachment source must be inspected and reused. |
| 2 | Agents | F2.1–F2.6 | Queued for the approved sequence; existing source preserved. No new phase sign-off recorded here. |
| 3 | Memory | F3.1–F3.8 | Queued for the approved sequence; existing source/data preserved. No new phase sign-off recorded here. |
| 4 | Artifacts | F4.1–F4.7 | Queued for the approved sequence; existing source/data preserved. No new phase sign-off recorded here. |
| 5 | Meetings | F5.1–F5.3 | Queued; Android entry point and the phase's unknowns must be resolved before implementing guessed meeting behavior. |

“Queued” means queued for verification/delivery under v2.0. It does not mean
all related source is missing. Historical reports must not be flattened into
a new whole-phase completion claim.

## Required task record

Before each task, check the approved reference and announce:

```text
Approved reference: TEAMLY_FEATURE_REFERENCE.md v2.0
Active phase: Phase N — <area>
Task phase / feature: Phase N — F<N>.<id> — <feature>
Task: <one bounded slice>
Evidence: <observed / source-only / documentation-based / unresolved>
Acceptance checks: <approved checks covered by this slice>
```

For cross-phase reference/governance changes, name Phases 1–5 explicitly rather
than inventing a Phase 0 or Phase 6. Still report the active delivery phase.

At handoff update this file with the feature/task state, source revision,
evidence/report location, actual checks/results, blockers and next in-phase
slice. Use `verified`, `partial`, `blocked` or `not started` for acceptance
items, distinguishing source implementation from updated-build runtime proof.
Do not mark a task or phase complete because a blocker exists.

## Current evidence limits

- The approved document says it did not inspect Orhan source or a signed-in
  Teamily Android runtime. Its labels do not establish current runtime parity.
- REA tools/device/package availability must be checked in the executing
  session; do not assume access from a laptop USB connection or old capture.
- Missing/inferred behavior or a source/reference conflict requires supported
  observation or a user decision. Continue independent in-phase work; do not
  invent backend contracts, UI options, extra features or success results.
- Phase advancement follows the approved order and recorded acceptance, or an
  explicit user-directed phase change. Existing auth, consent, stored data,
  signing and unrelated work remain preserved.
