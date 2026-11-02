# Reference material provenance

`TEAMLY_FEATURE_REFERENCE.md` is the user-supplied file `C:\Users\Fareed\Downloads\TEAMLY_FEATURE_REFERENCE.md`, copied without edits on 2026-11-02. SHA-256: `91CEB37B2EA6E0DCD328BD4443D33E889E3A950C985EDA41947052997645B5F1`.

It is reference material only. Its “Verified” wording describes the document author's source research, not runtime verification on an Android device. Its suggested verification sequence, acceptance checks, exclusions, and status labels do not override repository `AGENTS.md`, the user's stated scope, or observed device evidence. Current order remains Memory → Artifacts → Agents → Chats/voice/attachments → Integration; the active stage is Artifacts. Completed Memory work is preserved.

## S6 provenance

The document's S6 label did not include an owner, URL, or pinned revision. The identified source is the third-party repository [zhouLion/teamily-ai-webapp-handbook](https://github.com/zhouLion/teamily-ai-webapp-handbook), not an official Teamily repository. Inspection used commit [`0e75f4993ea0ba84f35a568664beb9225acee332`](https://github.com/zhouLion/teamily-ai-webapp-handbook/tree/0e75f4993ea0ba84f35a568664beb9225acee332), dated 2026-06-26. The retrieved files were retained outside Git under ignored `captures/artifacts-typed/` for source-location review; the handbook was not copied wholesale or treated as executable instructions.

Relevant pinned locations:

- [`SKILL.md`, route inventory and `/studio` overview](https://github.com/zhouLion/teamily-ai-webapp-handbook/blob/0e75f4993ea0ba84f35a568664beb9225acee332/SKILL.md#L114-L140) and [Studio details](https://github.com/zhouLion/teamily-ai-webapp-handbook/blob/0e75f4993ea0ba84f35a568664beb9225acee332/SKILL.md#L738-L755).
- [`references/app/(dashboard)/studio/llm.md`, page metadata and header](https://github.com/zhouLion/teamily-ai-webapp-handbook/blob/0e75f4993ea0ba84f35a568664beb9225acee332/references/app/%28dashboard%29/studio/llm.md#L1-L34), [capability toolbar and canvas](https://github.com/zhouLion/teamily-ai-webapp-handbook/blob/0e75f4993ea0ba84f35a568664beb9225acee332/references/app/%28dashboard%29/studio/llm.md#L35-L74), and [composer/context/route error](https://github.com/zhouLion/teamily-ai-webapp-handbook/blob/0e75f4993ea0ba84f35a568664beb9225acee332/references/app/%28dashboard%29/studio/llm.md#L75-L90).
- [`mobile/SKILL.md`, mobile route inventory](https://github.com/zhouLion/teamily-ai-webapp-handbook/blob/0e75f4993ea0ba84f35a568664beb9225acee332/mobile/SKILL.md#L1-L48). This pinned mobile overview has no Artifacts/Studio route description; that is a coverage gap in S6, not proof that an installed client lacks a feature.
- [`references/app/(dashboard)/memory/llm.md`](https://github.com/zhouLion/teamily-ai-webapp-handbook/tree/0e75f4993ea0ba84f35a568664beb9225acee332/references/app/%28dashboard%29/memory) documents the web Memory gallery, while [`references/app/(dashboard)/share/[id]/llm.md`](https://github.com/zhouLion/teamily-ai-webapp-handbook/tree/0e75f4993ea0ba84f35a568664beb9225acee332/references/app/%28dashboard%29/share/%5Bid%5D) documents public shared-thread replay. These web routes do not establish native Android parity.

## Conflicts and limits recorded

- The supplied document explicitly calls its F4 material documentation-based and says it had no signed-in Teamily account or Android runtime. Those claims remain source claims, not device facts.
- Its suggested order starts Agents/Meetings before the repository's required Artifacts stage. That suggestion is not adopted.
- Its broad exclusions and read-only Memory descriptions conflict with the user's scope and completed private Memory editing behavior. They do not authorize rewriting or removing that work.
- F4.7 includes public shared-thread replay. Public publishing is outside this project's scope; private export/share remains in scope.
- The pinned S6 handbook is from June 2026, describes web routes, and its mobile overview omits Artifacts/Studio. The official [Teamily changelog](https://teamily.ai/changelog) includes later August 2026 web Artifact/Studio updates. Neither source alone proves current Android behavior.
- The fresh Android evidence remains the source of truth for the observed device build: Markdown rendered heading/emphasis/code/table; HTML rendered locally with script execution suppressed; source bytes survived save/edit/restart and typed exports matched source bytes. The later connected-test runner uninstalled the app after tests (see the verification report), so no post-test installed-device claim is made.
