# Approved reference and provenance

## Final user-approved baseline

`TEAMLY_FEATURE_REFERENCE.md` is the exact user-supplied **v2.0 — 2026-11-03**
phase-wise document, approved by the owner in this task. It replaces v1.0 at
the canonical path; the v1.0 file remains available in Git history.

- Supplied filename: `TEAMLY_FEATURE_REFERENCE (1).md`.
- SHA-256: `954adb8b3abff512a6f0a6eb2753921811f4c8f8983f0ef7980c6e8eeceddb9a`.
- Approved delivery order (§0.4): **Phase 1 Chats → Phase 2 Agents → Phase 3
  Memory → Phase 4 Artifacts → Phase 5 Meetings**.
- Read [AGENTS.md](../../AGENTS.md) and
  [TEAMLY_PHASE_STATUS.md](../verification/TEAMLY_PHASE_STATUS.md) before every
  task. State active phase, task phase/feature ID, evidence and acceptance checks.

The phase order now supersedes the old Memory → Artifacts → Agents →
Chats/voice/attachments → Integration sequence and older next-task notes.
Existing source and saved data are preserved; this approval does not prove
any feature's acceptance or authorize deletion of already working behavior.

## How evidence is used

The approved file defines the feature scope and acceptance baseline. Its
“Verified” wording still describes the author's source research, not a device
test. `Documentation-based`, `inferred` and `unknown` retain the file's legend
meanings. Approval does not turn an unknown into an observed behavior.

Use supported REA inspection to resolve reference unknowns; record device,
package/version, steps, results and sanitized evidence. Do not guess private
APIs, provider credentials or unseen UI. If observed behavior conflicts with
the approved document, record the conflict and resolve the affected requirement
with the user before implementing a replacement. Do not silently edit the
approved file or expand scope. Reference §6.2 does not reorder delivery phases.

The scope excludes the social areas named in the approved file; private
account/settings and Orhan identity remain intact. F4.7's public replay material
and any ambiguous use of “comments” do not authorize excluded public/social
features. Record an unresolved scope conflict instead of choosing by assumption.

## S6 provenance

The document's S6 label did not include an owner, URL, or pinned revision. The identified source is the third-party repository [zhouLion/teamily-ai-webapp-handbook](https://github.com/zhouLion/teamily-ai-webapp-handbook), not an official Teamily repository. Inspection used commit [`0e75f4993ea0ba84f35a568664beb9225acee332`](https://github.com/zhouLion/teamily-ai-webapp-handbook/tree/0e75f4993ea0ba84f35a568664beb9225acee332), dated 2026-06-26. The retrieved files were retained outside Git under ignored `captures/artifacts-typed/` for source-location review; the handbook was not copied wholesale or treated as executable instructions.

Relevant pinned locations:

- [`SKILL.md`, route inventory and `/studio` overview](https://github.com/zhouLion/teamily-ai-webapp-handbook/blob/0e75f4993ea0ba84f35a568664beb9225acee332/SKILL.md#L114-L140) and [Studio details](https://github.com/zhouLion/teamily-ai-webapp-handbook/blob/0e75f4993ea0ba84f35a568664beb9225acee332/SKILL.md#L738-L755).
- [`references/app/(dashboard)/studio/llm.md`, page metadata and header](https://github.com/zhouLion/teamily-ai-webapp-handbook/blob/0e75f4993ea0ba84f35a568664beb9225acee332/references/app/%28dashboard%29/studio/llm.md#L1-L34), [capability toolbar and canvas](https://github.com/zhouLion/teamily-ai-webapp-handbook/blob/0e75f4993ea0ba84f35a568664beb9225acee332/references/app/%28dashboard%29/studio/llm.md#L35-L74), and [composer/context/route error](https://github.com/zhouLion/teamily-ai-webapp-handbook/blob/0e75f4993ea0ba84f35a568664beb9225acee332/references/app/%28dashboard%29/studio/llm.md#L75-L90).
- [`mobile/SKILL.md`, mobile route inventory](https://github.com/zhouLion/teamily-ai-webapp-handbook/blob/0e75f4993ea0ba84f35a568664beb9225acee332/mobile/SKILL.md#L1-L48). This pinned mobile overview has no Artifacts/Studio route description; that is a coverage gap in S6, not proof that an installed client lacks a feature.
- [`references/app/(dashboard)/memory/llm.md`](https://github.com/zhouLion/teamily-ai-webapp-handbook/tree/0e75f4993ea0ba84f35a568664beb9225acee332/references/app/%28dashboard%29/memory) documents the web Memory gallery, while [`references/app/(dashboard)/share/[id]/llm.md`](https://github.com/zhouLion/teamily-ai-webapp-handbook/tree/0e75f4993ea0ba84f35a568664beb9225acee332/references/app/%28dashboard%29/share/%5Bid%5D) documents public shared-thread replay. These web routes do not establish native Android parity.

## Historical evidence

The third-party S6 research and older reports retain their original dates and
limitations. They are not new observations and do not set the active phase.
Past Orhan Markdown/HTML captures describe only the documented build; source
merges, historical build/test results and prior setup do not establish current
device behavior. Reuse that evidence with its scope stated explicitly.

v1.0 provenance: original supplied file SHA-256
`91ceb37b2ea6e0dcd328bd4443d33e889e3a950c985eda41947052997645b5f1`;
its earlier stage-order interpretation is superseded by this approval.
