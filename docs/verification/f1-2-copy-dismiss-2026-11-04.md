# F1.2 Copy and action-sheet dismissal — 2026-11-04

Active phase: Phase 1 — Chats. Approved reference: [v2.0 F1.2](../reference/TEAMLY_FEATURE_REFERENCE.md#f12--message-actions-reply-copy-forward-translate-transcribe-react-unsend-delete-report). Previous [reference record](f1-2-rea-evidence-2026-11-04.md) and [build/device handoff](f1-2-build-verification-2026-11-04.md) are reused. This is a text-message Copy/dismissal slice, not completion of F1.2 or F1.1.

## Identity and boundaries

- Fresh REA device list: realme RMX5051 / RMX5051IN, selected serial ending `a3cb`, state `device`; `ev_799d808e16b226595e96115bc3f6ded4fb8a98eb1eeb7c96785083a371849be9`. REA reports transport `unknown`; USB transport is not independently attested.
- Fresh reference package: `ai.teamily.mobile` 2.0.20/code 234, `ev_0a1d2a1645adf6ac84777e2beb14849c3b4ef701e19e06094e99d27b584db8c5`. Launched its MainActivity and captured its visible UI before entering the existing synthetic Content Creator thread.
- Connected REA inventory supports device/package inspection and screen capture. No advertised arbitrary tap or clipboard-read operation was used. ADB input and UIAutomator supplied navigation and text comparisons; REA supplied captures.
- Reference messages were neither sent nor edited/deleted/forwarded. Clipboard checks used only existing synthetic text. Reports omit clipboard payloads and personal content; raw captures, including list-screen frames, stay ignored under `captures/f1-2-copy-dismiss/` and must not be committed.

## Reference observations

| Entry/action | Expected/actual | REA evidence (capture directory under the ignored root) |
|---|---|---|
| Long-press synthetic own text | Sheet opens; Copy is enabled | `ev_878440dc9d6d6d5a2e437a0f05347ca0db3d6eb363c0229414fc2c095f7c0e33` (`reference-own-sheet`) |
| Long-press synthetic agent text | Sheet opens; Copy is enabled; Unsend absent | `ev_a06d447e2fd28f072e7721b8ba1c91eda6e1d87a713602eb44b374172412b53f` (`reference-agent-sheet`) |
| Copy agent response | Sheet closes, same thread remains, empty reference composer remains empty | `ev_10af8e5e54a26a0cf20278d5515a95679621e3007d2a36846b7b1ff166d09905` (`reference-copy-after`) |
| Verify actual Copy outcome | Paste into initially empty Orhan scratch composer; exact comparison with selected synthetic agent text returned **true**, length 68. This checks actual paste, not a toast | `ev_ccc5c26757f1dfec95f0218b92f8512db45b405b5196d479bf69850d8ef3a0b6` (`reference-copy-paste-proof`) |
| Outside tap | Dismisses only sheet; same thread/messages and empty composer visible | Before `ev_7bb0162977b8bd8b299734975fbc61535917445168abfae60873e7ee571c722d`, after `ev_73491cd3a1f9d896ef239abbbc2371bede3c1343766d744bdd2f27c54a42d791` (`reference-outside-before/after`) |
| Android Back from confirmed sheet | Returned to chat list twice, superseding the old sheet-only interpretation | First before/after `ev_878440dc9d6d6d5a2e437a0f05347ca0db3d6eb363c0229414fc2c095f7c0e33` / `ev_adf30ce65d925b40ee0ac4d80d27e3a1754d01c096ca00dd62380091562d65c9`; repeat `ev_376643502c96b5b2b42c0ffc8af41dc18353869f74639cc8923a7bd8e1467863` / `ev_c59a420d48d1a88dd3715d4c5a021ee0b73b68fc8a44eb02ea6bf4ef6cb750d8` |

An intermediate repeat did not establish an open sheet in its before-frame and is excluded from Back evidence. Reference draft/reply retention was not tested with nonempty state because this pass did not alter the reference composer. Its empty composer and saved synthetic messages were unchanged after reopening. The user resolved the navigation difference explicitly: **return to chat list on Back and preserve draft/reply state in Orhan**.

## Existing implementation and minimal change

Paths in this table are under `app/src/main/java/ai/orhan/`.

| Requirement | REA evidence | Existing file:lines | Result/change |
|---|---|---|---|
| Copy text available for own and agent text | Own/agent sheets above | `design/screens/ChatScreen.kt:725-741,1207-1212,1677` | Already present. Existing ClipboardManager plain-text helper retained. Exact synthetic paste verified before and after update. Image/media Copy is outside this text-only result. |
| Outside dismissal preserves messages/draft/reply | Reference outside pair; Orhan state checks below | `design/screens/ChatScreen.kt:729,1662-1668` | Existing dismissal only clears selected action-message ID; preserved. |
| Back returns to chat list | Confirmed Back pairs above; user resolution | `design/screens/ChatScreen.kt:228,730-733,1662-1668`; `app/OrhanNavHost.kt:598` | Verified gap fixed in existing sheet: dedicated Back callback and existing HOME route; scrim/swipe dismissal remains separate. |
| Reply selection survives closing/reopening chat | Explicit user requirement | `design/screens/ChatScreen.kt:229-230,284-289,520,690,735`; `app/OrhanNavHost.kt:121-122,599-602` | Hoisted thread-keyed transient selection to existing navigation host via rememberSaveable. Selecting/clearing/sending updates that same owner. No new message model, store, sheet or navigation system. |

Draft/history persistence and migrations remain in the existing thread store. Reply selection retention here covers navigation; no new claim of disk-persisted unsent reply selection after process death is made.

## Checks and updated device

All Gradle commands were sequential, using `.\gradlew.bat` from the repository root. Normal build settings were used; only the existing invocation parameter `-PversionCodeOffset=106` supplied a compatible update version. No build configuration changed.

| Command/check | Actual result |
|---|---|
| `:app:testDebugUnitTest --tests ai.orhan.data.history.ChatThreadStoreTest --tests ai.orhan.data.history.ChatAttachmentPersistenceTest --tests ai.orhan.feature.chat.HandoffParseTest -PversionCodeOffset=106 --console=plain` | Passed on final source; full log `tests-final.log` |
| `:app:testDebugUnitTest --tests ai.orhan.feature.chat.MeetingMinutesActionsTest -PversionCodeOffset=106 --console=plain` | Separately reproduced: 2 tests, 1 failed, `emptyOrUnstructuredActionSectionDoesNotCreateActions` at line 25; unchanged baseline. Log `meeting-baseline.log`; no suppression or Phase 5 change |
| `:app:assembleDebug -PversionCodeOffset=106 --console=plain` | Passed, `assemble.log` |
| `:app:lintDebug -PversionCodeOffset=106 --console=plain` | Passed, `lint.log`; `app/build/reports/lint-results-debug.html` |

Updated APK: `ai.orhan` v0.2.0/code **371**, replacing code 369. Installed and built signer SHA-256 both `8de8c2e46e572df1a7dcedce00b48a6ba0e53c042d52079c510f2923be88e06e`. APK SHA-256 `BF990E5F7D66F255F93790A66A996A93706C6821409BCA918553B13C0FAEF429`. `adb install -r` succeeded; REA confirmed code 371 with unchanged first-install timestamp (`ev_af7794270b42df8cd5755208871354a7344e58bc949f1804ea7f3ff892219477`). No uninstall/data clear.

Ordered code-371 observations:

1. In the existing synthetic Orhan thread, start with empty composer; type a temporary synthetic draft and select Reply on an existing synthetic message. Open its sheet and press Back: chat list appears (`ev_cf96c5fe364fdd1fc94acc52713de6e85069db4e39926ed9527c713e4f7ec0ea`, `updated-back-list`).
2. Reopen thread: same draft and same reply target restored (`ev_779a92b421e4cab0a8a46aaa838dce60079112a814362b217af13c571d6d5276`, `updated-reopen-outside-retained`).
3. Establish a visible sheet again, outside-tap: same chat, draft and reply target retained. Reopen sheet and Copy: same state retained (`ev_9d4640ce48755d32ef61199cc8a15769259786d61c1b458853f54d3ae49354dd`, `updated-outside-copy`). Exact appended paste comparison returned **true**; remove only the pasted suffix to restore the scratch draft (`ev_b44b398d2d014be2e88fe47017dd996139b31a53e5833d76a493a6eecb72c8d2`, `updated-copy-proof-restored`).
4. Clear only this turn's reply selection, force-stop/cold-launch, reopen: scratch draft restored. Tap the existing saved quote: exact original is visible with Source message/highlight (`ev_6d7d264b63ca42371a9d38e4456d4e558ad7116e4b61784294fd7cbfc3e7640f`, `updated-restart-quote`). Ordered actions, not a standalone screenshot, establish restart persistence.
5. Remove only this turn's synthetic scratch draft and confirm empty composer/no pending reply. No message was sent during this slice. Checks above used dark theme. Also selected Light through Appearance, confirmed the updated sheet (`ev_67253a13d03bb0729b761120cac4ec89758491b6de5f1ff1a1fda2bcf0f77217`, `updated-light-sheet-confirmed`) and Back returned to chat list. Restored the original System preference afterward.

## Remaining work

F1.1 stays partial: group/member model blocker remains. F1.2 text Copy and chosen Back/outside semantics are verified in the tested context; image/media Copy, drag-to-dismiss runtime, reference nonempty reply/draft retention, failure eligibility and other action outcomes remain open. The known MeetingMinutesActionsTest failure remains separate; no full-suite pass is claimed. Next safe F1.2 slice: Translate language picker and cancellation; forward destination inspection remains observational only. Unrelated `HomeScreen.kt` edits are preserved.
