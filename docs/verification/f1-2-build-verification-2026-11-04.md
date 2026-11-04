# F1.2 build restoration and updated-device verification — 2026-11-04

Approved behavior source: `docs/reference/TEAMLY_FEATURE_REFERENCE.md` v2.0 (PR #120). This handoff covers the existing-message Reply flow and the compile blocker. It does not sign off F1.2 as a whole. Earlier Teamly reference observations are in [the F1.2 REA record](f1-2-rea-evidence-2026-11-04.md). Raw build/device captures and full logs are ignored under `captures/f1-2-build-blocker/`.

## Build diagnosis

The required command was run first, sequentially:

```powershell
.\gradlew.bat :app:compileDebugKotlin --stacktrace --console=plain
```

It failed on the current F1.2 working tree. Full output: `captures/f1-2-build-blocker/compile-before.log`. The first compiler diagnostics were `OrhanNavHost.kt:4:41` (unresolved `languageName`) and `OrhanNavHost.kt:5:41` (unresolved `settingsDetailPreferences`), followed by many unresolved top-level symbols in app packages. Their definitions exist unchanged at `app/src/main/java/ai/orhan/feature/settings/detail/SettingsDetailPreferences.kt:55,112`; other reported declarations likewise exist in `CreateAgentScreen.kt:46`, `Theme.kt:54`, and `MeetingMinutesActions.kt:4`. The current F1.2 diff did not remove those declarations. The normal `--info` diagnostic (`compile-info.log`) shows all Kotlin sources, including these declaration files, in the compile inputs; incremental compilation was enabled, the changed-file set was the four edited F1.2 Kotlin files, deleted files were empty, and classpath changes were `NoChanges`.

Read-only comparison with successful F1.1 HEAD `63a6b1e` found no edits to those declarations or app Gradle source configuration. Existing outputs and Kotlin module metadata were present. The evidence localizes the failure to inconsistent incremental compiler state: the same source set compiled when incremental compilation was disabled for one diagnostic invocation, after which the original normal incremental command also passed. The exact internal trigger is unknown; no specific compiler defect or cache mechanism is asserted.

| Sequential command | Result | Full log |
|---|---|---|
| `:app:compileDebugKotlin --stacktrace --console=plain` | Failed with the first errors above | `compile-before.log` |
| `:app:compileDebugKotlin --stacktrace --console=plain --info` | Same failure; source/change-state evidence recorded | `compile-info.log` |
| `:app:compileDebugKotlin --stacktrace --console=plain '-Pkotlin.incremental=false'` | Passed, full compile in one diagnostic invocation | `compile-full.log` |
| `:app:compileDebugKotlin --stacktrace --console=plain` | Passed with normal project settings after state restoration | `compile-restored.log` |

No source, Gradle, toolchain, dependency, signing or CI repair was needed or made for this blocker. Incremental compilation was not disabled in project configuration, and no broad cache deletion or working-tree reset was performed.

## Checks

Commands were run one at a time from `apps/android` using the checked-in Windows wrapper:

| Command | Result | Evidence |
|---|---|---|
| `:app:testDebugUnitTest --tests ai.orhan.data.history.ChatThreadStoreTest --tests ai.orhan.data.history.ChatAttachmentPersistenceTest --tests ai.orhan.feature.chat.HandoffParseTest --console=plain` | Passed | `tests-relevant.log` |
| `:app:testDebugUnitTest --tests ai.orhan.feature.chat.MeetingMinutesActionsTest --console=plain` | Failed: 1 of 2 tests; `emptyOrUnstructuredActionSectionDoesNotCreateActions`, `MeetingMinutesActionsTest.kt:25`, expected empty but got `[unresolved]` | `tests-meeting-baseline.log` |
| `:app:assembleDebug -PversionCodeOffset=106 --console=plain` | Passed | `assemble.log` |
| `:app:lintDebug -PversionCodeOffset=106 --console=plain` | Passed | `lint.log`; HTML report at `apps/android/app/build/reports/lint-results-debug.html` |

The MeetingMinutesActions test and implementation are unchanged from baseline `b5e4360`; this failure remains separate and was not suppressed, skipped, or changed. The full unit suite was not run as a clean pass and no full-suite success is claimed.

## Updated Orhan device verification

REA's fresh device/package inspection identified realme RMX5051 / RMX5051IN, serial `9cefa3cb` (`ev_eaa3b470b48f966e07c20864b2711507580974dc8b4500c4dfce59c1964f84a4`), and installed reference `ai.teamily.mobile` 2.0.20/code 234 (`ev_0a1d2a1645adf6ac84777e2beb14849c3b4ef701e19e06094e99d27b584db8c5`). The prior synthetic Teamly action-sheet/Reply/send observations are documented in the linked reference report; its quote-tap capture did not conclusively show highlight. No personal Teamly record was changed in this verification.

Installed Orhan was `ai.orhan` v0.2.0/code 367. The built update was `ai.orhan` v0.2.0/code 369, using the existing version derivation with invocation-only `-PversionCodeOffset=106`. Installed and built APK signing SHA-256 matched: `8de8c2e46e572df1a7dcedce00b48a6ba0e53c042d52079c510f2923be88e06e`. Built APK SHA-256: `7594C5CCE55E6914AB639F1AAB50A08484DB6F49CD49D0B4D49C6A1E54490B6D`. `adb install -r` succeeded. REA then confirmed code 369 and the unchanged first-install timestamp (`ev_eb0097f1293c08407c2514acef84ba47b7eabbfc3e395d0d6605e05c1d3cd2bf`). No uninstall or app-data clear occurred.

Using only the Orhan synthetic assistant-thread source message `ORHAN_F11_DRAFT_RESTART` and synthetic reply `F12_VERIFY_REPLY_20261104`:

1. Long-pressing the source opened the existing action sheet; Reply was available.
2. Reply opened a quote composer with “Reply to you”, the selected source, and “Clear reply” (`ev_c3dba7737bdcf8a707d0da07be23a61311bb0b9493fee2520061304db9046064`, `reply-composer/screen.png`).
3. Sending the synthetic reply rendered its quote (`ev_9a132f0a9c8a0a065344078d497dc369b638e08ba90f4f99e5bfc44446b63d43`, `reply-sent/screen.png`).
4. Tapping the quote showed the exact original `ORHAN_F11_DRAFT_RESTART` in the “Source message” row with the existing highlight (`ev_627cea08a5163fe4bf2790bdb602e93f647078c2b395a8af8354a3136c4e87db`, `quote-target/screen.png`).
5. After force-stop/cold launch and reopening the thread, the quote/reply was restored; tapping the restored quote again navigated to the exact original (`ev_1d7ffbec5f6074f69d38b0e848b65c0aa4c18b057370798729576fabe4d889cc`, `restart-restored/screen.png`). This ordered action verifies persisted relation and navigation; a screenshot alone is not treated as persistence proof.
6. Repeated quote navigation in dark theme showed the original source highlight (`ev_8225987f5051881b33238511e9a653f4004019d30ac54a6e77216dba3b4419d3`, `dark-highlight/screen.png`). System theme was restored to light afterward.

## Reuse and remaining work

This verification uses the existing `ChatScreen`, `ChatViewModel`, `ChatThreadStore`, `ChatMessage`, and chat navigation. The quote relation is serialized in thread v9; v8 draft migration and earlier history remain covered by targeted store tests. No duplicate composer, screen, model, navigation or store was introduced. No source edits were necessary to restore the build during this turn.

- F1.2 Reply → quote → send → exact-source scroll/highlight: verified on updated Orhan in light/dark and after cold restart. Teamly's quote-target highlight remains inconclusive from its existing capture, so reference parity on that point is not claimed.
- Copy result, X cancellation, reactions, forward flow, multiple selection, destructive-action confirmation, failure eligibility, group Report, and other action outcomes remain unverified/partial. Continue with the smallest safe in-phase observation; do not use personal reference messages for mutation.
- F1.1 remains partial. Group mentions are blocked by the absence of an Orhan-owned group/member model. Preserve that blocker; do not infer a model or mark F1.1 complete.
- Existing unrelated `HomeScreen.kt` whitespace-only local edit and all prior draft work were preserved.
- Next slice: inspect/verify non-destructive Copy and quote cancellation/action eligibility using a synthetic context, then update this ledger with evidence. Do not begin another phase.
