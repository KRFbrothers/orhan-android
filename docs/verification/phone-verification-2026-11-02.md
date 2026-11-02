# Connected phone verification — 2026-11-02

This report records fresh UI checks on the updated physical phone. It does not reuse older screenshots or test reports as proof.

## Build and device

- Device: RMX5051, Android 16, ADB serial `9cefa3cb`.
- Installed app: `ai.orhan`, versionCode 341, versionName 0.2.0.
- APK: `app/build/outputs/apk/debug/app-debug.apk`; signer SHA-256 `8de8c2e46e572df1a7dcedce00b48a6ba0e53c042d52079c510f2923be88e06e`.
- The package was absent before the already-authorized fresh install. `adb -s 9cefa3cb install -r` succeeded. No uninstall or app-data clear was used.
- The integrated source initially failed `:app:assembleDebug` on Compose source errors in `ChatScreen.kt` and `AgentsScreen.kt`. Those errors were fixed on `fix/integrated-chat-compile`; a subsequent `:app:assembleDebug` succeeded, and that APK was installed. The installed timestamp is `2026-11-02 22:31:14`.
- PRs #93 and #108–#110 are closed/merged. PR #93 remains the artifact feature PR; this report and compile repair are on the integrated chat branch. PR #110 metadata sync depends on Dua #506, which is also merged. Merging did not deploy or apply the database migration.

## Fresh phone UI observations

| Flow | Result | Evidence / limitation |
|---|---|---|
| Search saved message/transcript and exact-message jump | **Blocked** | Fresh install has `CHATS · 0`. Opening chat/starting a workflow reaches the normal sign-in screen. No synthetic message could be created without an authenticated service session, so search identity, exact jump, and transcript indexing were not exercised on this phone. No auth bypass was used. |
| Markdown artifact | **Verified in UI** | Created a synthetic Markdown document; detail identified it as Markdown and preview rendered a heading and bold text. Edited and saved the title/source; after app restart the updated document was still listed. Share opened Android's file-share chooser and was cancelled. Export reported success for an `.md` source file. The exported file was not found in the Files app Downloads view during cleanup, so its final destination/content is **unverified**. |
| HTML artifact | **Verified in UI** | Created synthetic HTML; detail identified `Web page (HTML)` and the preview rendered its heading/paragraph. Preview explicitly said external links/resources were disabled. HTML export/share was not separately checked. |
| Artifact deletion | **Verified in UI** | Both synthetic artifacts were removed using their in-app delete confirmation. |
| Memory pause/settings | **Verified in UI** | Pause-memory control was switched on and showed the paused explanation; it was restored to off. After app restart, Pause memory, Automatic memory, and Use topics in chats were all visibly off. |
| Memory artifact connection | **Partial** | A synthetic topic displayed its linked HTML artifact with Open artifact/Unlink actions. After that artifact was deleted, the topic detail showed `This artifact is no longer on this device.` No source-chat link could be tested because there were no chats. Synthetic topic was deleted through its in-app confirmation. |
| Agent create/edit | **Partial; local persistence observed** | Created a synthetic agent, edited its name and saved it; the edited profile appeared in the Agents list/detail. Due to an ADB text-entry encoding mistake, the synthetic instruction field contained literal `%20` text, so this does not verify useful persona instruction behavior. Agent detail explicitly showed `Per-agent tool enforcement is unavailable in the chat service contract. Existing saved tool preferences are retained but inactive.` Agent chat needs sign-in. Synthetic agent was deleted through the agent detail action. |
| Workflow save/history/launch | **Partial; server execution blocked** | Saved a synthetic workflow and selected the synthetic agent. Run created a local `queued` history entry, then the UI showed the normal sign-in screen. This is not evidence of a completed server run. Deleted the workflow using its editor's delete confirmation. |
| Light/dark appearance | **Verified in UI** | Switched Appearance to Dark and captured the selected Dark control, then restored Light and captured its selected control. |
| Restart persistence | **Partial** | After force-stopping/relaunching the app through explicit serial ADB commands, the saved Markdown edit and synthetic memory topic/link were visible before supported cleanup. Pause/settings were also visibly off after restart. Agent and workflow records were created after this restart, so their cross-restart persistence was not checked. The workflow history showed `queued`; no server outcome was verified. |

Sanitized evidence is in `C:\Users\Fareed\Downloads` and is not committed: `orhan-341-markdown-preview.png`, `orhan-341-html-preview.png`, `orhan-341-dark-appearance.png`, `orhan-341-light-appearance.png`, and `orhan-341-memory-profile-after-restart.png`. The Files screenshot was intentionally excluded because it exposed unrelated directory names.

## Remaining blockers and validation

- Normal Orhan sign-in is required for saved chat/transcript search data, agent chat requests and workflow launch. Sign in on the device, then create synthetic chat/audio content through supported UI to complete those checks.
- Agent-tool capability enforcement is unavailable in the service contract, as the phone's agent detail states. The saved selection does not currently provide active server enforcement.
- Dua #506 provides a source migration/API for workflow metadata, but migration application and backend deployment are separate pending tasks. The phone run only demonstrated local queue/history state.
- Actual server-side workflow execution, durable workers and scheduled triggers remain unimplemented service dependencies.
- `:app:assembleDebug` succeeded before install. Unit tests, instrumentation and lint were **NOT RUN—deferred by user**. No emulator troubleshooting was run. The device checks above are normal UI verification, not automated tests.
