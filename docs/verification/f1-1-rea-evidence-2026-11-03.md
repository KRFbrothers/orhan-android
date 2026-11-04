# Phase 1 — F1.1 REA evidence

Approved baseline: `TEAMLY_FEATURE_REFERENCE.md` v2.0, from approved PR #120
(`a5470992377d4003dac8cad240063630cb23042b`). The reference document is now present
in this checkout. This record contains sanitized observations and REA evidence IDs;
raw captures remain under ignored `captures/` and are not committed.

## Reference environment

- Device: realme RMX5051 / RMX5051IN, Android 16 (SDK 36), serial `9cefa3cb`.
- REA reported the device online, but transport was `unknown`; USB transport itself
  was not independently established by REA.
- Reference app: `ai.teamily.mobile` 2.0.20 (versionCode 234), Play installer.
- Orhan package was separately verified as `ai.orhan` 0.2.0 (versionCode 366).
- Evidence IDs below were produced by REA. Screenshots are single frames; ordered
  actions are recorded where needed to establish persistence or send behavior.

## Observations

| Check | Actions and expected/actual result | REA evidence |
| --- | --- | --- |
| Empty composer | Opened the Content Creator agent conversation. Empty field showed the microphone control. Focusing it opened the keyboard; the microphone remained. | `ev_2dbdf7e3af3b39b7ccf25a59692527a6ac5f330dfde9d175b56d0315ec057d07` (`empty-composer/screen.png`); `ev_f57c6b08844c148c74c1b0b9cadd00a0a55c7ddf6a8fd5b758a87973966e9aae` (`focused-empty/screen.png`) |
| Text and multiline | Typed a synthetic token: send arrow became green. ADB Enter inserted a newline and did not send. This does not establish hardware Shift+Enter behavior. | `ev_4732280cabd2c98fc4a8387af23bd137587ec811c145fc529274e8c193019331` (`typed-draft/screen.png`); `ev_e0f406a335b28421ce1b3de8dada1fc4a7e397eaefc3834ea609a0c70f318370` (`multiline/screen.png`) |
| Direct-chat mention | Typed `@` in the Content Creator 1:1 agent conversation. No member picker appeared. | `ev_dc8ddf9f3b4a2714f1c7273cce90d6925b93ec09a104eec70e9cfe790ca833e2` (`direct-mention/screen.png`) |
| Group mention | Opened the pinned Full-Stack Delivery group and typed `@`. A member picker appeared. “Everyone” was the first option; “All agents” and individual members followed. The picker was dismissed without selecting or sending. | `ev_a743347da958857d8c5a86803df269812f50938a8c794954433e49b534d42282` (`group-mention/screen.png`) |
| Draft after leaving chat | Typed a synthetic draft, returned to the conversation list, then reopened that row. The draft returned. | `ev_0e2d1ca5037cb5ae3a7d3e02bc926240a1752a2c2f9f1c4dd5d494253f8b75b3` (`left-draft/screen.png`); `ev_88133421e0ef147a1caf6643825e27b9e33c7778164d4a5040ac893a4bb51ae3` (`returned-draft/screen.png`) |
| Draft after cold restart | Force-stopped Teamily, started it cold, and reopened the same agent thread. The row no longer showed a draft and the composer was empty. This ordered sequence establishes the loss; a screenshot alone would not. User selected the v2.0 persistence requirement as Orhan’s target. | `ev_b4c8953f01c1c2324fe985bfc1d2a8d767e4d53a0fe473c05cb8bb19b967b9c6` (force-stop); `ev_3b447b68a3d77ad323a9c1ca8b8ad5580f9c5a72f002f8f2a2eae95a2c251232` (cold start); `ev_4e163cda02a8d6040fee0617cc7d2ce9f8e0aed8841d085613961998394df978` (`restarted/screen.png`); `ev_b97752709ffdf1cbef1c80d30fc5499aa9a920cd76df57e8b23dafad526d4d64` (`reopened-after-cold/screen.png`) |
| Send and result | In the Content Creator conversation (no prior user-authored message), sent one synthetic test token. It appeared immediately as a user bubble with a sent check; the agent returned a short confirmation. No failure was induced. | `ev_9950b5ee2b21702723b2b498672fb836da447f8530ccb97bf714c40cb9f46d4a` (`direct-typed-send/screen.png`); `ev_3d3a6125c5d4a3d94b0e66416b999bbd988f06322bc0a79591d908de4e3ce335` (`direct-after-send-2/screen.png`) |

## F1.1 requirement map

| F1.1 requirement | REA observation | Orhan file:lines | Status | Minimal change |
| --- | --- | --- | --- | --- |
| Empty composer shows mic; send inactive | Observed empty mic; send became active only with text. | `app/src/main/java/ai/orhan/design/screens/ChatScreen.kt:1270-1435` | Already present | None |
| Send text and show result | Synthetic text appeared immediately; source appends a user bubble and streams a reply. | `app/src/main/java/ai/orhan/design/screens/ChatScreen.kt:463-505`; `app/src/main/java/ai/orhan/feature/chat/ChatViewModel.kt:378-408` | Already present | None |
| Multiline and Enter behavior | Reference virtual Enter inserted newline. Orhan composer supports up to five lines and uses the configured IME action; hardware Enter parity is unverified. | `app/src/main/java/ai/orhan/design/screens/ChatScreen.kt:1300-1395` | Partial / hardware behavior unknown | None; keep current keyboard preference |
| Mention picker in group | Reference group picker has “Everyone” first. Orhan routes only saved-agent mentions in its Orhan assistant thread; no group/member model or member source exists in the inspected Android chat flow. | `app/src/main/java/ai/orhan/design/screens/ChatScreen.kt:618-633`; `app/src/main/java/ai/orhan/app/OrhanNavHost.kt:606-620` | Group support blocked by missing Orhan group capability | No group backend or duplicate composer added |
| `@` in direct 1:1 does not open member picker | Observed in a Teamily agent 1:1. Orhan has an assistant-thread agent roster, which is a separate context. | `app/src/main/java/ai/orhan/design/screens/ChatScreen.kt:618-633`; `app/src/main/java/ai/orhan/app/OrhanNavHost.kt:606-620` | Existing agent mention route present; human 1:1 parity unknown | None |
| Draft survives app restart and restores in same thread | Teamily lost it after force-stop/cold start; v2.0 and user decision require preservation. Orhan previously used Compose `rememberSaveable` only. | `app/src/main/java/ai/orhan/design/screens/ChatScreen.kt:255-284`; `app/src/main/java/ai/orhan/feature/chat/ChatViewModel.kt:166-230`; `app/src/main/java/ai/orhan/data/history/ChatThreadStore.kt:28-326` | Verified source gap; implementation added | Persist draft through the existing per-thread DataStore and serializer v8; preserve v7 transcripts |
| Send failure | No failure was induced in Teamily; exact retry/failed-bubble behavior remains unknown. | Existing send path: `app/src/main/java/ai/orhan/feature/chat/ChatViewModel.kt:378-408` | Unknown | No invented retry behavior |

## Implementation and verification limits

- Reused `ChatScreen`, `ChatViewModel`, `ChatThreadStore`, and the existing chat
  route. No second composer, thread store, or message model was added.
- Draft writes are associated with the thread title, survive thread message saves,
  and restore after DataStore load. Empty draft text is persisted after send so an
  older saved draft cannot reappear if the screen closes immediately.
- Tests cover v8 round-trip, draft-only threads, clearing while preserving messages,
  and v7 transcript migration.

## Updated Orhan build

- Device: same RMX5051 / Android 16, serial `9cefa3cb` (REA continued to report
  transport as unknown).
- Before update, REA observed `ai.orhan` 0.2.0/versionCode 366. The built package
  was `ai.orhan` 0.2.0/versionCode 367. Installed and built APKs had the same
  signing certificate SHA-256:
  `8de8c2e46e572df1a7dcedce00b48a6ba0e53c042d52079c510f2923be88e06e`.
- `adb install -r` returned `Success`. REA then observed versionCode 367; the
  original first-install timestamp remained unchanged. No uninstall or app-data
  clear was performed.

| Check | Actions and expected/actual result | REA evidence |
| --- | --- | --- |
| Updated build launch and empty composer | Started the updated `ai.orhan/.MainActivity` cold and opened the existing Orhan conversation. Empty composer displayed microphone; no message was sent. | Start `ev_eac9f80bde13a7b4162efe404a7929e35e66099a6db04185e2d9d854c2c34871`; foreground/capture `ev_67acc81516a9da24f8d5e2249472934846f4098d150039582a2f4c93b2410ca1`; empty chat `ev_10947644f1e050bf92d04a337b74fd8dfb219f0441c8264eb34caa055f5092ed` (`orhan-chat-open/screen.png`) |
| Draft and send affordance | Typed one synthetic token into the existing composer. It remained in the field and the send arrow appeared; it was not sent. | `ev_71e685d88208de11fdef51afa2a44b2ac59003ebdc5c97d38b8776f670a1b3d5` (`orhan-draft-typed/screen.png`) |
| Leave and return | Dismissed keyboard, left the thread, reopened the same thread. The synthetic draft was restored. | `ev_f700e461b8d65708e5675719b415f159d578d03cb1ad6673ea2c4dc50d5d0e8f` (`orhan-left-draft/screen.png`); `ev_f74904d49166a120f96ac9d981d8f93d3bd400ad7eb9c7c2129bdd9cea7df368` (`orhan-returned-draft/screen.png`) |
| Force-stop and cold relaunch | Force-stopped the updated app without clearing data, started it cold with REA, reopened the same conversation. The draft remained in the composer. | Start `ev_21dc0649460d82c66d3788c1e422f605138681d81af9fb8d12b1a854a469c3e3`; restored draft `ev_b4972f5c0294c3cd4ad20baacff11201df0a3d5489cbff7630386747ca5bc1c4` (`orhan-draft-after-cold/screen.png`) |

The updated build's composer and restart-draft behavior were compared. A send was
not performed from Orhan's existing personal assistant thread, so updated-build
send/result and failure behavior remain unverified there. Teamily's synthetic
agent send was observed above.
