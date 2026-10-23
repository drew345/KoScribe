# Synthetic cross-app instrumentation smoke test

The dedicated `:smoke` debug APK targets its **own** package, `com.koscribe.smoke`. Android restarts an instrumentation target process; self-targeting prevents the test command from restarting KoScribe's accessibility service. The smoke APK has no launcher, runtime permission, credential, or product code. AndroidX Test currently merges a normal `REORDER_TASKS` permission and test helper activities into its APK. Its one nonexported fixture Activity contains a standard EditText and no Send control. This creates a synthetic cross-app destination in a separate process from KoScribe. Do not use the earlier app-targeted AndroidTest command: on the A35 it stopped the service before any test action.

Precondition: exactly one authorized physical A35 in user 0, with KoScribe accessibility and microphone permission already enabled; SwiftKey remains selected. The synthetic suite does not change permissions, accessibility settings, the keyboard, or any messaging app. It never sends a message or presses Enter. Product button selectors inspect only `com.koscribe.prototype` nodes; the editor selector inspects only the stable `local_test_editor` in `com.koscribe.smoke`. It finds unique enabled product buttons by exact label and uses `ACTION_SHOW_ON_SCREEN` for clipped own controls. `ACTION_SET_TEXT` and `ACTION_SET_SELECTION` prepare only the synthetic smoke editor; production insertion still uses its guarded accessibility input connection.

The custom runner forces `FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES` in both `getUiAutomation` overloads, and every test acquisition supplies it explicitly. That flag prevents UI Automation from suppressing installed accessibility services. The tests do not use UiDevice or a no-flag UiAutomation connection.

Build with the shared JDK/SDK environment in `docs/PHASE1.md`:

```powershell
.\gradlew.bat :app:assembleDebug :smoke:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

With the device and user confirmed, keep the already-installed product APK if its build is unchanged; install only the smoke APK. Run each normal case separately so its diagnostic trace is captured before another fixture starts:

```powershell
adb -d install --user 0 -r smoke/build/outputs/apk/debug/smoke-debug.apk
adb -d shell am instrument --user 0 -w -e class com.koscribe.smoke.LocalEditorSmokeTest#emptyEditorEnglishInsertsExactlyOnce com.koscribe.smoke/com.koscribe.smoke.KoScribeTestRunner
adb -d shell am instrument --user 0 -w -e class com.koscribe.smoke.LocalEditorSmokeTest#middleCaretPreservesTextOnBothSides com.koscribe.smoke/com.koscribe.smoke.KoScribeTestRunner
adb -d shell am instrument --user 0 -w -e class com.koscribe.smoke.LocalEditorSmokeTest#cancelAfterStopPreservesExistingDraft com.koscribe.smoke/com.koscribe.smoke.KoScribeTestRunner
adb -d shell am instrument --user 0 -w -e class com.koscribe.smoke.LocalEditorSmokeTest#selectedRangeRefusesRecordingAndPreservesDraft com.koscribe.smoke/com.koscribe.smoke.KoScribeTestRunner
adb -d shell am instrument --user 0 -w -e class com.koscribe.smoke.LocalEditorSmokeTest#leavingReviewCancelsWithoutInsertion com.koscribe.smoke/com.koscribe.smoke.KoScribeTestRunner
adb -d shell am instrument --user 0 -w -e class com.koscribe.smoke.LocalEditorSmokeTest#editedEnglishReviewPreservesMiddleFixtureText com.koscribe.smoke/com.koscribe.smoke.KoScribeTestRunner
adb -d shell am instrument --user 0 -w -e class com.koscribe.smoke.LocalEditorSmokeTest#politeSamplePreservesKoreanEmojiDraftAroundCaret com.koscribe.smoke/com.koscribe.smoke.KoScribeTestRunner
adb -d shell am instrument --user 0 -w -e class com.koscribe.smoke.LocalEditorSmokeTest#casualSampleInsertsIntoEmptyEditor com.koscribe.smoke/com.koscribe.smoke.KoScribeTestRunner
```

The three main cases require the smoke editor and its window focused with an IME window visible immediately before opening the bubble. They leave recording open for two seconds and wait 750 ms after review appears before confirming or cancelling. They check empty-field EN insertion, cancellation after Stop, and insertion at a middle caret in existing synthetic text. A fourth, explicitly diagnostic empty-field case first verifies that the smoke editor/window is focused and an IME window is present, then issues one global Back to hide that IME. It waits for the IME window to disappear and the smoke editor to remain focused before running the same EN flow. It never reads the keyboard tree or text. A pass in this keyboard-hidden case alone is **not** acceptance of the normal keyboard-visible flow. Each wait is bounded. Failure cleanup clicks the exact KoScribe Cancel control when available, or brings the smoke fixture forward so the recording Activity's normal `onPause` releases the microphone. Results contain test names and fixed reason codes, never field text or UI dumps. If setup reports `BUBBLE_NOT_AVAILABLE`, check the exact KoScribe accessibility component state before retrying; an enabled setting alone does not prove the service is bound. A pass covers this synthetic cross-app editor only. Real messaging-app compatibility and Phase 1 acceptance remain separate gates.

On failure the suite requests `dumpsys activity service` for the exact KoScribe component and prints only its bounded fixed diagnostic codes; it does not emit the raw dump or editor text.

The keyboard-hidden isolation method is `com.koscribe.smoke.LocalEditorSmokeTest#emptyEditorKeyboardHiddenDiagnostic`. Run it separately by substituting that value after `-e class`; the ordinary visible-keyboard method remains the primary feasibility check. An unfiltered class invocation runs all nine methods, including the one global Back diagnostic.

The selected-range negative verifies refusal before recording and exact draft preservation. The interrupted-review negative brings only the existing synthetic fixture forward with `REORDER_TO_FRONT`, verifies cancellation, and checks that its draft remains unchanged after the insertion delay. Cancellation drafts end in a space to avoid testing active-word composition instead of cancellation.

The edited-English case finds one visible enabled EditText within the single focused KoScribe application window, verifies the review controls and fixed sample, and changes only that review field with `ACTION_SET_TEXT`. It requires the IME to be visible and verifies exact insertion between both original fixture sides. This tests edited-content return with SwiftKey visible; it does not establish real SwiftKey keystroke/composition behavior. That remains a separate manual/device check.

The Korean cases exercise exact 요/반말 choices with their fixed samples. The polite case preserves both Korean and emoji around a UTF-16 middle caret; the casual case starts empty. These verify insertion and mode routing, not speech recognition or translation. The fixture keeps the screen on only while its own window is visible; it never changes the global timeout, wakes the phone, or unlocks it.

## Separately gated KakaoTalk self-chat tests

`KakaoExternalDraftSmokeTest` is separate from the synthetic class and skips by default. Its two flags are mutually exclusive: `externalDraftReady` applies only to the previously approved unsent fixed-sample insertion, and `externalRecordCancelReady` applies only to the new recording/Cancel diagnostic. Each method independently checks its own flag before any product action. Never run the unfiltered external class with either flag; select the exact method. The earlier draft insertion authorization does not authorize another insertion or this new diagnostic run.

The new no-insertion method is `com.koscribe.smoke.KakaoExternalDraftSmokeTest#kakaoEmptyComposerPoliteRecordCancelPreservesEmptyDraft`. It requires Andrew's separate approval before running. After that approval, the exact single-method command is:

```powershell
adb -d shell am instrument --user 0 -w -e class 'com.koscribe.smoke.KakaoExternalDraftSmokeTest#kakaoEmptyComposerPoliteRecordCancelPreservesEmptyDraft' -e externalRecordCancelReady true com.koscribe.smoke/com.koscribe.smoke.KoScribeTestRunner
```

The method verifies the sole focused KakaoTalk application window and only its focused input node, an empty/hint composer with the approved narrow empty-caret rule, and the exact selected and visible SwiftKey IME. It requires no preexisting Stop/Cancel, opens KoScribe's bubble, chooses 요, waits for a fresh visible Stop, records for about two seconds, then taps Stop and **Cancel**. It never taps confirmation or Copy. It checks the same original field remains empty immediately and after 1.2 seconds, with SwiftKey still selected and visible. No chat tree, other field, keyboard tree, screenshot, Send, Enter, editor action, app launch, setting, or permission is touched. Failure cleanup can tap only KoScribe's exact Cancel control. The fixed reason codes never include the draft contents.

KoScribe's first-content diagnostic fields describe the latest successful editor capture and persist after Cancel. If an external test fails before mode selection, do not attribute retained diagnostic fields to that failed preflight.

Run while Andrew is not using the phone; app switches can correctly invalidate a session. Instrumentation completion can remove the fixture and return to a previous real app. Do not take post-test full-screen screenshots on the assumption that the synthetic editor is still visible. Prefer exact fixture assertions and code-only traces; capture pixels only after independently verifying the current foreground is an owned synthetic screen.

References: [AOSP instrumentation target restart](https://source.android.com/docs/core/tests/development/instrumentation), [UiAutomation flag](https://developer.android.com/reference/android/app/UiAutomation#FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES), [flagged acquisition](https://developer.android.com/reference/android/app/Instrumentation#getUiAutomation(int)).
