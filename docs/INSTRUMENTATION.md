# Synthetic cross-app instrumentation smoke test

## Karrot relative-read diagnostic — November 6

This is a hard no-write diagnostic for the active manual-insertion goal.
Build secure REVIEW with `-PkoscribeRelativeReadProbe=FIXTURE` or `KARROT`;
every speech, translation, visual, automatic and other probe flag must be OFF.
Default/release stays OFF. The two arms share a classifier but have separate
package scope. FIXTURE additionally requires the product's signing certificate.
There are no draft, microphone, API, credential, clipboard or insertion actions
in either arm. Status always reports `ELIGIBLE=NEVER`.

After source review, archive separate APKs/configs and verify their signatures
against the installed manual MVP. Obtain phone availability before installation.
Run these owned cases individually with the exact method suffix after
`com.koscribe.smoke.RelativeEditorReadSmokeTest#` and its sole flag:

| Method | Sole flag | Required observation |
| --- | --- | --- |
| `emptyCollapsedHasUnknownAbsoluteOffset` | `relativeEmptyReady` | EMPTY, UNKNOWN offset, collapsed relative and independent selection |
| `middleCollapsedHasUnknownAbsoluteOffset` | `relativeMiddleReady` | NONEMPTY, UNKNOWN offset, collapsed relative and independent selection |
| `selectedEmojiLooksCollapsedWhenSelectedTextIsUnavailable` | `relativeSelectedReady` | NONEMPTY, UNKNOWN offset, collapsed relative selection but independent SELECTED / SHAPE_CONTRADICTS |

```powershell
adb -d shell am instrument --user 0 -w -r -e class 'com.koscribe.smoke.RelativeEditorReadSmokeTest#emptyCollapsedHasUnknownAbsoluteOffset' -e relativeEmptyReady true com.koscribe.smoke/com.koscribe.smoke.KoScribeTestRunner
```

The fixture retains its actual focus and input connection while its remote
accessibility node omits focused/editable/type/resource-ID/selection metadata.
It reproduces the default-like unknown-offset surrounding-text response with
selected text unavailable. Tests check actual remote metadata, SwiftKey, served
input, unchanged fixture text/selection and unchanged legacy effect counters.
They launch and finish only their owned fixture. Stop on the first failure.

Only after those checks pass, install the KARROT arm in place and have Andrew
prepare an empty Karrot composer with SwiftKey visible. Never inspect conversation
history or screenshots. The one deliberate command is:

```powershell
adb -d shell dumpsys activity service com.koscribe.prototype/.KoScribeService --relative-editor-read-once
adb -d shell dumpsys activity service com.koscribe.prototype/.KoScribeService --relative-editor-read-status
```

Status does not read editor text. One request permits at most one styled
`getSurroundingText(1,1,GET_TEXT_WITH_STYLES)` call, with a three-second deadline,
terminal interruption and a busy latch until the IPC returns. Android can return
the entire selection despite the small adjacent-context request. KARROT refuses
known selected/nonzero metadata before reading; unexpected nonempty content is
classified numerically then discarded without inspecting characters or spans.
No content, hash, coordinates or app-provided exception text is logged.

`PAYLOAD=EMPTY` describes the returned snippet, not the entire field.
`INPUT_SOURCE=INITIAL_ONLY` is start-time evidence; `CALLBACK_CURRENT_GEN` means
delivered in this input generation, not proven remotely fresh. Age, later signals,
composition and relative/independent selection agreement remain separate.
None of these results authorizes an insertion. Review measured evidence before
designing a manual adapter. Do not automatically retry a refused/interrupted read.

Restore the current editable-Korean live MVP in place after the session:
`app/build/manual-korean-result-20261105/manual-korean-translation-v1.apk`, SHA-256
`3FE8BE0FF5AFD51908177D81F6AF652FB6EB0C34A85CD68825731E16DA3D33EA`.
Verify installed hash, connected idle service, speech/translation true, all
experiments false, SwiftKey retained and no draft/cleanup. Remove only the owned
fixture from user 0. Never uninstall/clear the main app or alter its saved key.

## Automatic approval-to-insertion candidate — November4

Use the reviewed secure API-free REVIEW build with AUTO_INSERT=true. Speech,
translation, visual and Karrot probe flags must be false. Eligibility remains
`com.koscribe.smoke` only. Run while Andrew has confirmed the phone is free;
one exact method and its sole flag at a time. Never run the entire instrumented
suite. Inspect each outcome before continuing and restore the saved regular
speech/translation APK afterward. No screenshots or external app inspection.

Method prefix: `com.koscribe.smoke.AutomaticInsertSmokeTest#`.

| Method | Sole flag |
| --- | --- |
| `unchangedEmptyOriginalAutoInsertsOnceAndDismisses` | `autoEmptyReady` |
| `unchangedMiddleOriginalPreservesBothSides` | `autoMiddleReady` |
| `changedOriginalFallsBackAndLateRestoreNeedsManualInsert` | `autoChangedReady` |
| `changedOriginalCaretFallsBackWithoutLateAutoRetry` | `autoCaretReady` |
| `changedOriginalFieldFallsBackWithoutWritingEitherField` | `autoFieldReady` |
| `initialSelectedRangeRequiresManualFallbackWithoutWriting` | `autoSelectedReady` |
| `reviewCancelNeverAutoCommits` | `autoCancelReady` |
| `acceptedButUnobservedAutoCommitIsTerminalWithoutInsertRetry` | `autoUncertainReady` |

Pass the flag with value `true` to the existing
`com.koscribe.smoke/com.koscribe.smoke.KoScribeTestRunner`. Automatic success
requires one confirmed commit, exact fixture text, SwiftKey retained, collapsed
Ko visible, no result controls and zero increment in `Auto manual result renders`.
Changed destination cases must fall back before writing. The uncertain fixture
accepts a single commit call without applying it; the draft must remain terminal
with no Insert retry. A sample pass is not proof of live translation processing
or external-app automatic eligibility; those are separate acceptance gates.

The retained-host harness additionally waits for the service's full retained
review proof before normal automatic Confirm. Policy REVIEW alone proves the
input/focus phase, not settled keyboard geometry. The ineligible selected-range
case still follows its manual fallback path. The Cancel-only lifecycle diagnostic
waits at most two seconds for full retained review proof or terminal fallback,
reports READY/FALLBACK/TIMEOUT, then Cancels without Confirm or a write. This
observational wait does not extend the app's entry deadline. A harness pass alone
is not hypothesis acceptance: inspect readiness, refusal and no-write counters.

## Existing instrumentation architecture

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

`KakaoExternalDraftSmokeTest` is separate from the synthetic class and skips by default. Its two flags are mutually exclusive: `externalDraftReady` applies only to the completed unsent fixed-sample trial, and `externalRecordCancelReady` applies only to recording/Cancel diagnostics. Each method independently checks its own flag before any product action. Never run the unfiltered external class with either flag; select the exact method. Andrew subsequently authorized continued scoped recording/Stop/Cancel diagnostics without insertion or sending. That ongoing authorization covers repeats within the same scope; it does not authorize another confirmation/insertion trial.

The authorized no-insertion method is `com.koscribe.smoke.KakaoExternalDraftSmokeTest#kakaoEmptyComposerPoliteRecordCancelPreservesEmptyDraft`. Verify live empty self-chat and SwiftKey preconditions; the exact single-method command is:

```powershell
adb -d shell am instrument --user 0 -w -e class 'com.koscribe.smoke.KakaoExternalDraftSmokeTest#kakaoEmptyComposerPoliteRecordCancelPreservesEmptyDraft' -e externalRecordCancelReady true com.koscribe.smoke/com.koscribe.smoke.KoScribeTestRunner
```

The method verifies the sole focused KakaoTalk application window and only its focused input node, an empty/hint composer with the approved narrow empty-caret rule, and the exact selected and visible SwiftKey IME. It requires no preexisting Stop/Cancel, opens KoScribe's bubble, chooses 요, waits for a fresh visible Stop, records for about two seconds, then taps Stop and **Cancel**. It never taps confirmation or Copy. It checks the same original field remains empty immediately and after 1.2 seconds, with SwiftKey still selected and visible. No chat tree, other field, keyboard tree, screenshot, Send, Enter, editor action, app launch, setting, or permission is touched. Failure cleanup can tap only KoScribe's exact Cancel control. The fixed reason codes never include the draft contents.

After KakaoTalk regains focus, the no-insertion test allows up to four seconds for SwiftKey to become visible before checking the empty field. This accommodates asynchronous keyboard return; it does not retry recording or insertion.

KoScribe's first-content diagnostic fields describe the latest successful editor capture and persist after Cancel. If an external test fails before mode selection, do not attribute retained diagnostic fields to that failed preflight.

The lifecycle timing diagnostic also retains six one-shot own-UI milestones:
collapsed bubble `addView` return, panel launch request, accepted panel bind,
resume callback, focus callback with a known own-window ID, and a focused own
window accepted by the existing modal gate. It prints only each milestone's
BEFORE/AFTER difference from capture completion and the first valid content
event occurrence, never raw timestamps or IDs. Add return is not proof that a
frame rendered; a callback is not proof of accepted guard state. The separate
verified-window milestone reuses the existing guard observation. Missing
milestones remain NOT_OBSERVED; invalid/missing event times remain UNKNOWN.
No cross-app query or insertion policy is added by these diagnostics.

## Startup isolation experiment (authorized 2026-10-23)

Andrew authorized separating the bubble and recording-panel startup steps.
The controlled design uses compile-time debug opt-in arms and the same existing
Ko / 요 controls; extra runtime test controls would change the overlay geometry.
The normal build remains the default. The experiment arms are:

| Arm | After the original field capture | Observation scope |
| --- | --- | --- |
| HOLD | Keep the expanded bubble, do not launch a panel | Bounded 1.5-second baseline |
| COLLAPSE | Collapse the bubble normally, do not launch a panel | Bounded 1.5-second collapse observation |
| PANEL | Launch the recording panel immediately, keep the expanded bubble | Recording / Stop / Cancel only |

Each experiment must use the original capture and existing invalidation rules;
it must never recapture or restore an invalidated session. Experimental builds
block insertion and clipboard recovery in the service and omit confirmation
controls. A timeout must end the probe and release any microphone use. The
external test verifies the expected build arm before acting, requires a fresh
probe start, and checks the same original empty focused field plus SwiftKey
after cleanup. Existing privacy limits and the closed earlier insertion trial
still apply. The preserved ordinary APK is restored after measurements.

Only events during each active probe count toward its comparison. In particular,
the final cleanup rerenders the bubble after the observation ends.
Absence of a first-content record after an earlier invalidation does not mean
there were no content events. Build hashes, measured outcomes, and limitations
belong in `DEVICE_TESTS.md` after execution.

Build with `-PkoscribeStartupArm=HOLD`, `COLLAPSE`, or `PANEL` and keep each
APK under a distinct ignored build-output filename. Omitting the property uses
OFF; release builds always use OFF. The test checks the installed arm from
KoScribe's fixed-code dump and requires its capture count to advance exactly
once, so a retained earlier result cannot satisfy a new run.

Use only one exact method in
`com.koscribe.smoke.KakaoStartupIsolationSmokeTest` with its corresponding
argument; never run the whole class/suite or combine legacy external flags.

| Exact method suffix after `#` | Required argument |
| --- | --- |
| `captureOnlyHoldsExpandedBubbleWithoutPanel` | `-e startupHoldReady true` |
| `collapseOnlyHoldsWithoutPanel` | `-e startupCollapseReady true` |
| `panelOnlyStartsWithoutCollapsingBubble` | `-e startupPanelReady true` |

For example, after installing the verified HOLD APK and confirming the live
empty self-chat/SwiftKey preconditions:

```powershell
adb -d shell am instrument --user 0 -w -r -e class 'com.koscribe.smoke.KakaoStartupIsolationSmokeTest#captureOnlyHoldsExpandedBubbleWithoutPanel' -e startupHoldReady true com.koscribe.smoke/com.koscribe.smoke.KoScribeTestRunner
```

The initial comparison has no extra settling delay between opening Ko and
selecting 요. HOLD therefore isolates the absence of later collapse/launch,
but cannot distinguish capture/mode-tap effects from delayed effects of opening
the expanded bubble. Add a matched settling condition only if the observations
make it useful. A refused panel startup is not a completed recording/Stop/Cancel
flow, even when the final preservation assertions succeed.

## Nonfocusable startup candidate (authorized 2026-10-23)

The debug-only OVERLAY build replaces Activity startup with recording controls
inside the already-expanded nonfocusable bubble window. It keeps that window's
attachment, token, position, flags, and measured viewport; only its own child
controls change during the active session. No window add/remove/update is part
of recording startup. The same original target is captured once;
it must remain focused and identifiable throughout. Visibility is required
before and after the panel; the panel intentionally covers part of the screen.
Missing/changed/ambiguous
target, content/selection/input transitions, lock, and ownership loss cancel
terminally and stop the microphone. The modal Activity gate is not used as an
overlay exception. OFF/default/release keeps the ordinary editable Activity.

This bounded candidate displays the selected mode, recording status, Stop,
and Cancel. Stop requires positive reads and its own non-null recorder
configuration reporting unsilenced capture, then releases the microphone and
shows readonly fixed English with Cancel. It does not demonstrate speech
recognition, editable review, translation, or insertion. These recording-only
experiment arms hard-block insertion and clipboard recovery; the later REVIEW
arm has its own separate synthetic gates below. The compact window does not cover
the whole screen; external target/focus/input changes retain terminal refusal.
An eight-second cancellation deadline ends the session. Cleanup
exceptions are reported as uncertain, block new capture, and trigger bounded
release retries; the deadline is not proof of microphone release.

Build with `-PkoscribeStartupArm=OVERLAY :app:assembleDebug` and preserve the
verified APK/hash separately under ignored build output. Use only one exact
method in `com.koscribe.smoke.KakaoOverlayRecordingSmokeTest`, with the sole
`-e overlayReady true` flag. Never combine it with older external/startup flags
or run an unfiltered class/suite. Both methods verify the installed OVERLAY arm,
fresh capture, same original empty focused Kakao field and selected/visible
SwiftKey during recording and immediately/1.2 seconds after cleanup:

```powershell
adb -d shell am instrument --user 0 -w -r -e class 'com.koscribe.smoke.KakaoOverlayRecordingSmokeTest#nonfocusableOverlayRecordsStopsAndCancelsWithoutDraftMutation' -e overlayReady true com.koscribe.smoke/com.koscribe.smoke.KoScribeTestRunner
adb -d shell am instrument --user 0 -w -r -e class 'com.koscribe.smoke.KakaoOverlayRecordingSmokeTest#nonfocusableOverlayCancelDuringRecordingReleasesOwnerAndPreservesDraft' -e overlayReady true com.koscribe.smoke/com.koscribe.smoke.KoScribeTestRunner
```

Run separately and inspect each result before proceeding. Root operates the
phone; implementation/review agents do not. Inspect only focused-field/root
metadata, IME identity, own controls, and bounded fixed-code diagnostics. Check
the exact app's RECORD_AUDIO AppOps after each run to establish capture ended.
No confirmation, Copy, insertion, Send, Enter, chat traversal, screenshot,
permission change, or automatic target recapture is authorized by these tests.
Record exact installed APK, measured result, and limitations in DEVICE_TESTS.

The OVERLAY first-child-window diagnostic privately snapshots at most 16 direct
child windows at capture and first target CHILDREN event, using IDs/types only.
It exports fixed add/remove/retain/type/order counts and exact own-host matches;
incomplete data remains unknown. It never retrieves child roots, titles, or
view trees. IDs clear at session end. Both snapshots are delivery-time data and
cannot prove event cause or harmlessness. The refusal remains unconditional.

The first CHILDREN event also records relative occurrence/delivery timing to
capture start/completion, own Stop entry, and
REVIEW_CHILDREN_POPULATED_CALLBACK. That last callback follows microphone
release and construction of the readonly review controls; it is not evidence
that review remained visible. Zero/negative/future event timestamps are UNKNOWN.
Only signed relative deltas leave the process. Timing adds no external reads
and does not filter, defer, or exempt any event.

## Independent recording, Stage 1

Andrew chose review first, then tap the intended field and Insert. The debug
RESELECT arm starts a separate recording session with no destination capture.
It returns before accessibility target handling and passive editor callbacks.
Own host stability, microphone proof, screen-off/lock, interruption, destruction,
and a 15-second session deadline still govern cleanup. RESELECT blocks commit
and Copy; REVIEW has separate synthetic gates below. Default/release remains OFF
until replacement gates pass.

Stage 1 provides recording -> Stop -> readonly fixed English review -> Cancel.
It does not implement editable review, translation, destination selection, or
insertion. Build with `-PkoscribeStartupArm=RESELECT`; preserve the verified APK
separately before resetting ordinary build output to OFF.

After Andrew prepares the empty Kakao self-chat and visible SwiftKey, run only:

```powershell
adb -d shell am instrument --user 0 -w -r -e class 'com.koscribe.smoke.KakaoReselectRecordingSmokeTest#unboundRecordingOverThreeSecondsStopsReviewsAndCancels' -e reselectReady true com.koscribe.smoke/com.koscribe.smoke.KoScribeTestRunner
```

The sole opt-in flag is mutually exclusive with all older external test flags.
The smoke checks zero product destination captures, a fresh own-microphone proof,
recording surviving 3.2 seconds after that proof, Stop/review/Cancel, ended cleanup,
and the same empty focused field/visible SwiftKey during and after the flow.
Only the smoke reads the external focused editor; it never mutates that editor.
Counters supplement source review rather than proving absence of every possible
editor read. Check exact-app RECORD_AUDIO AppOps after execution. Never use a
whole suite, Copy, confirmation, Send/Enter, chat traversal, or screenshot here.

The planned later destination check needs its own fresh transaction and full
event/input guards; the Stage 1 unconditional RESELECT callback exits must be
replaced with phase-aware handling before that check can exist.

## Editable English review synthetic checks (2026-10-24)

Use the separately built and reviewed `-PkoscribeStartupArm=REVIEW` candidate,
not the earlier RESELECT recording-only candidate. Run only exact methods in
`ReviewFlowSmokeTest`, one at a time, with their sole matching flag:

| Method | Sole flag |
| --- | --- |
| `editedEnglishCopyClosesAndReappearsAfterImeCycle` | `reviewCopyReady` |
| `editedEnglishInsertsIntoEmptyFixtureOnce` | `reviewInsertEmptyReady` |
| `longEnglishInsertsAtMiddlePreservingKoreanAndEmoji` | `reviewInsertMiddleReady` |
| `fixedKoreanSampleInsertsIntoEmptyFixture` | `reviewKoreanReady` |
| `selectedRangeRefusesAndPreservesResult` | `reviewSelectionReady` |
| `koreanEditRejectsThenCancelPreservesFixture` | `reviewKoreanEditReady` |
| `acknowledgedButUnchangedCommitIsTerminallyUncertain` | `reviewUncertainReady` |
| `exactComposedCommitConfirmsAndDismissesOnce` | `reviewComposedExactReady` |
| `wrongComposedCommitStaysTerminallyUncertain` | `reviewComposedMismatchReady` |
| `editedEnglishEmptyInsertReportsNaturalConfirmationOrTerminalProbe` | `reviewConfirmationProbeReady` |

```powershell
adb -d shell am instrument --user 0 -w -r -e class 'com.koscribe.smoke.ReviewFlowSmokeTest#editedEnglishCopyClosesAndReappearsAfterImeCycle' -e reviewCopyReady true com.koscribe.smoke/com.koscribe.smoke.KoScribeTestRunner
```

These tests launch and mutate only our synthetic fixture and KoScribe's review
editor, with SwiftKey selected/visible. They never open or inspect a conversation,
invoke Send/Enter, or change keyboard selection. Copy replaces the clipboard
with a fixed synthetic result; do not read or restore earlier clipboard content.
The Copy test checks our clip metadata before examining the known synthetic text.
The uncertainty fixture acknowledges both commit overloads without changing
text, exercising terminal uncertainty and the absence of another Insert action.
Earlier Oct24 runs ended on WINDOW_OR_FOCUS_CHANGED. The composed-confirmation
candidate later reached POSTCONTEXT_UNCONFIRMED after unchanged observations;
check the actual terminal code each time.
REVIEW launches omit the unused protected editor and opt out of autofill. The
default fixture still includes that editor for explicit protected-field tests.

Tests check zero destination captures/commits during recording and review, then
fresh capture only after explicit Insert. Exact text/one-commit counters and
bubble dismissal are assertions, not conclusions from a void API return. English
edits use the owned review EditText's accessibility action; this verifies editable
content handling with the keyboard visible, not actual SwiftKey keystrokes or
prediction. Those still require a manual device check. No real translation runs.
The Korean-edit Cancel test may reveal its exact own Cancel node with one
ACTION_SHOW_ON_SCREEN. Bounded polling remains on that original node/window;
no generic scroll, Back, or coordinates. Closure requires draft NONE, unchanged
capture/commit counters, and absent review controls regardless of visibility.

Inspect each result before continuing. Root operates ADB; coding/review agents
do not. Read only exact app AppOps, fixed service codes, own nodes, and keyboard
metadata. Keep all older real-app tests outside this run; never execute a whole
class/suite. The actual phone results belong in DEVICE_TESTS and PROGRESS.

Startup prerequisite: awake/unlocked does not exclude an open notification shade.
On Oct24 the fixture attached/resumed/drew normally but the shade held focus;
UiAutomation exposed only two SYSTEM windows. A fixed-category WindowManager
focus check identified the shade; closing it resolved startup without changing
product guards or timeout. Never read notification contents to diagnose this.
Fixture publication follows setContentView; never query hasWindowFocus off the
main thread during construction. Startup timeout diagnostics contain only own
lifecycle counters, window-type counts, package classifications, and flag bits.

The new composed tests enable two extra synthetic editors only for their exact
opt-in flags. Each delegates one commit and leaves a framework composing span
on the resulting text. One uses the requested result; the other substitutes a
fixed incorrect synthetic result. Both require POSTCOMMIT_COMPOSING_OBSERVED
after the current commit, so losing the span before observation fails branch
proof. Exact requires confirmation/dismissal and stable text; mismatch requires
COMPOSED_POSTCONTEXT_UNCONFIRMED, no Insert or editing recovery, preserved result,
one product/fixture commit, and Cancel cleanup. Default/manual fixtures are unchanged.

The following probe instructions describe the previous secure980E diagnostic.
That bounded round is closed. The approved postcommit rule now admits composed
observations and removed the old ORIG composition-rejection record; use the new
deterministic methods for acceptance, not a rerun of the old probe.

The confirmation probe is a diagnostic, not an insertion acceptance test. Use its
sole flag with that exact method. It makes one synthetic empty-field commit and
handles natural CONFIRMED separately from terminal UNCERTAIN. The latter path
prints only bounded POST_DIAG or POST_COMPOSE codes before cleanup, verifies no
retry/rearm/success relabeling, then Cancels while preserving the expected fixture
text. Outcomes distinguish natural confirmation, complete diagnostics, a complete
but nonproving composition observation, and incomplete ABORT. None of the
diagnostic outcomes is an insertion acceptance pass. Run at most two fresh
attempts for the Oct24 composition investigation; no automatic retry.

The probe exists only in debug REVIEW for the synthetic package, after
NODE_CARET_MISMATCH or ACTIVE_COMPOSITION has already ended eligibility. CARET
retains original/T0/T1/DONE; COMPOSITION has ORIG plus T0/T1/T2/DONE, targeted at
immediate/150ms/500ms after probe start. A 750ms check refuses delayed-sample
admission; Android IPC can still take longer. The ORIG composition record uses
the existing observation: I=complete same-target/host identity, CX=expected
context+offset, CK=expected connection caret, NK=expected node caret, NA=node/
connection agreement. Span metadata reports presence, count capped at16, first
start/end, zero-length flag, more-than-cap and unavailable/error flags; never
span objects, text, or hashes. Later same-field observations cannot rehabilitate
the transaction or prove uninterrupted insertion continuity.

## Manual SwiftKey review gate (2026-10-24)

This Insert procedure passed on secure980E4299... after clarified instructions.
It covers one actual SwiftKey edit and guarded insertion in the synthetic field.
The earlier Oct24 visual checkpoint stopped after dragging/selecting, without
Insert, and is over.

The debug smoke APK has a launcher named **KoScribe test editor**. Its exported
DebugFixtureLauncherActivity ignores caller data/extras/actions and constructs
only a fixed manual-mode intent to the existing nonexported SmokeEditorActivity.
The manual mode has one ordinary field under "Test message" with a hint, no
prefilled text, and no protected/uncertain editors. Automated default and REVIEW
fixture modes retain their respective fields. It never types, focuses a message
field, records, copies, or inserts.
Root can open it without changing the product APK:

```powershell
adb -d shell am start --user 0 -W -n com.koscribe.smoke/.DebugFixtureLauncherActivity
```

Andrew verifies the single manual fixture field is empty, taps it, then Ko -> EN ->
Stop after about two seconds. In the review he taps the END of the fixed sample,
types ` Please wait.` using actual SwiftKey keys, taps Confirm English, returns
to that destination field, and taps Insert once. Scroll inside the own review
if its Confirm control is below the viewport. Leaving review before confirming
cancels it. Recording has a 15-second deadline; review/result has three minutes
from review creation, not renewed by edits. No automatic retries on uncertainty.

Make the location explicit: after confirmation the small Ko bubble becomes the
result box. Its top row is EN/Move and Cancel; the bottom row is Insert and Copy
& close. Insert is in that floating box, not Android's cursor Paste/Autofill menu.
If the row is missing, pause for a report rather than substituting Paste. Manual
Paste can use a sample left by a prior Copy test; it does not use KoScribe's
current result unless Copy was explicitly pressed.

Record his exact behavioral report separately from code evidence. Success needs
the expected edited sentence and bubble dismissal plus own state CONFIRMED,
draft NONE, captures/commits each +1, and ended microphone use. This gate is
accepted only when those observations agree. They agreed on Oct24, as recorded
in DEVICE_TESTS. This does not establish all composition or prediction cases.
Only fixed synthetic content is used.

### Restored-focus variant (manual synthetic PASS 2026-10-24)

Start a fresh empty manual fixture after confirming the previous draft ended.
Andrew taps it before opening Ko, records/stops, edits the fixed English with
SwiftKey and confirms. After returning, he must not tap the destination field.
If the cursor is visibly restored in the intended field, he taps floating Insert
once. Otherwise stop and record absent restored focus. No Paste/Copy, retap to
force focus, or retry. Confirm his report against one capture/commit delta,
CONFIRMED, draftNONE, dismissal and ended microphone use. Keep all product checks;
the implementation already captures afresh on Insert without a recent-tap token.
Andrew's report and own state agreed on secure980E4299... / smoke9D3F9563...:
one new capture/commit, CONFIRMED, draftNONE, dismissal, microphone ended3.415s.
This accepts the tested empty fixture, not automatic focus restoration in all apps.

For subsequent authorized Kakao diagnostics on REVIEW, use the manual recording
-> Stop -> Cancel-in-review flow and require zero capture/commit delta. Do not
run KakaoReselectRecordingSmokeTest on REVIEW: it requires the RESELECT build.
Keep focused-field privacy limits and the closed real-app insertion scope.

## Review-surface comparison (2026-11-05)

This is an API-free, hard no-write diagnostic. Build the product separately with
`-PkoscribeStartupArm=REVIEW -PkoscribeReviewSurfaceProbe=ACTIVITY` and `OVERLAY`.
Other experiment flags must be false; ordinary/release default to OFF. Save both
APKs and their generated BuildConfig, plus the matching smoke APK, after source
review. Preserve the secure installed MVP for restoration; never clear its data.
The six November5 invocations completed; measurements and limitations are in
[DEVICE_TESTS](DEVICE_TESTS.md#review-surface-comparison-2026-11-05).

Both arms use fixed English, the same original-target observer and an unchanged
collapsed bubble host. The Activity arm uses the existing ReviewActivity/theme
through an isolated branch; it is not the November4 recording/global-ledger
candidate. During review, the current input connection belongs to the review:
original text/caret remain UNKNOWN. Read the original bounded context only after
the owned target's input session, window, root and node agree again. The probe
has no insertion permit, draft, microphone, API or clipboard path.

After a fresh phone-readiness reply, install the reviewed matching arm and run
one exact method with its sole readiness flag. Do not invoke the whole suite.
All methods belong to `com.koscribe.smoke.ReviewSurfaceComparisonSmokeTest`:

| Method | Sole flag | Human action |
| --- | --- | --- |
| `activityMiddleCaret` | `activityMiddleReady` | Append ` test` with SwiftKey, then tap Dismiss probe |
| `overlayMiddleCaret` | `overlayMiddleReady` | Append ` test` with SwiftKey, then tap Dismiss probe |
| `activityIdenticalAlternate` | `activityAlternateReady` | None |
| `overlayIdenticalAlternate` | `overlayAlternateReady` | None |
| `activitySameViewRebind` | `activityRebindReady` | None |
| `overlaySameViewRebind` | `overlayRebindReady` | None |

Example for the first case:

```powershell
adb -d shell am instrument --user 0 -w -r -e class 'com.koscribe.smoke.ReviewSurfaceComparisonSmokeTest#activityMiddleCaret' -e activityMiddleReady true com.koscribe.smoke/com.koscribe.smoke.KoScribeTestRunner
```

The positive cases wait for actual user typing and user-controlled dismissal;
never simulate typing with ACTION_SET_TEXT or automatically dismiss on the first
keystroke. The four negatives mutate only the owned fixture, then dismiss. Both
fields contain identical fixed text/caret. Same-view rebinding changes only a
private fixture epoch, without an artificial input restart; the app cannot read
that oracle. Observer MATCH with oracle CHANGED exposes an indistinguishable
logical destination, not automatic eligibility or a failed test harness.

Record per-arm target-root/node availability while the review owns focus,
input-session changes, returned identity/caret/context, oracle agreement and
cleanup. First-null role describes this probe's bounded inventory sample; it
cannot identify the historical missing root. JUnit completion means the diagnostic
ran, not that its observed comparison passed. `microphone=NEVER_STARTED` is not
a recording-cleanup result. Require zero destination-capture/commit/automatic-
attempt/recording-proof counters and unchanged SwiftKey. No screenshots, external
editor actions or Send. Stop on interruption or unexpected effects and inspect
the result before continuing. Restore the saved secure MVP when the session ends.

## Manual Korean result review (2026-11-05)

Use secure manual REVIEW with its fixed Korean sample. Speech, translation,
auto insertion, visual mode and all probes must be off. The existing recording
path briefly checks microphone availability and then stops. Tests use fixed
English and Korean, make no API requests and never press Copy or Send.

Run one `com.koscribe.smoke.ManualResultEditSmokeTest` method at a time in the
owned fixture, with its single flag set to `true`. Inspect the JUnit result,
destination/commit deltas, microphone stop and cleanup before proceeding.

| Case | Method | Flag |
| --- | --- | --- |
| Discard a Korean correction, retain original, cancel with zero writes | `discardKeepsPreparedKoreanWithoutWriting` | `manualResultDiscardReady` |
| Read a long result and reach its actions above SwiftKey | `longKoreanPreviewCanBeReadAndControlsReached` | `manualResultLongReady` |
| Explicit Insert uses the prepared Korean unchanged | `uneditedPreparedKoreanIsVisibleAndInsertsOnce` | `manualResultUneditedReady` |
| Save corrected Korean, choose a different fixture field, Insert at its middle caret | `editedKoreanInsertsOnlyFinalResultIntoChosenField` | `manualResultEditedReady` |

Done returns to the nonfocusable result panel. Check the full saved text there
before choosing a destination. For the long-layout case, first allow the
keyboard to return naturally. If necessary, select the known fixture field to
open SwiftKey, then require the preview and keyboard together before scrolling.
Record how the keyboard returned. For the Insert cases, require the chosen
cursor and keyboard before the explicit Insert.

The tests edit only KoScribe's own result editor. Only the final manual Insert
may open a destination transaction. The expected target string includes the
original prefix and suffix. The source field must stay intact. Verify the Copy
payload in source/unit checks without replacing the user's clipboard.

## Stable-editor automatic sample (2026-11-05)

Deferred after the move to manual Korean result review.

This is a separate **write-capable**, API-free debug candidate. The six-case
review-surface comparison above remains hard no-write. Build the new arm with
`-PkoscribeStartupArm=REVIEW -PkoscribeStableOverlayAuto=true`; old AUTO, review
surface probe, visual, speech, translation and the other probes must remain off.
Save its signed APK, matching BuildConfig and exact smoke before any phone test.
The synthetic editor's stable logical backing is a test contract, not evidence
that a third-party composer has that property.

After final source review, successful builds, and fresh phone availability, use
only `com.koscribe.smoke.StableOverlayAutoSmokeTest`, one method and its sole
readiness flag. Inspect each result before proceeding. The tests open fixed
English directly and exercise one explicit sample approval or Cancel; they do
not record, translate through an API, type into a real app, use the clipboard,
press Enter or Send. A refusal or uncertain write is not a confirmed insertion.

| Order | Method | Sole flag | Required evidence |
| ---: | --- | --- | --- |
| 1 | `changedIdenticalFieldFallsBackBeforeWrite` | `stableAlternateReady` | Armed before change; target-specific refusal, zero commits, explicit manual recovery |
| 2 | `unchangedMiddleCaretInsertsOnce` | `stableMiddleReady` | One approval/confirmed commit; exact Korean/emoji prefix, suffix and caret; no manual result panel |
| 3 | `unchangedEmptyInsertsOnce` | `stableEmptyReady` | One approval/confirmed commit in empty stable editor |
| 4 | `reviewCancelNeverWrites` | `stableCancelReady` | Original and alternate untouched; clean Cancel |
| 5 | `changedCaretFallsBackBeforeWrite` | `stableCaretReady` | Cursor contradiction refuses before writing; restoring old position cannot rearm |
| 6 | `changedContextFallsBackBeforeWrite` | `stableContextReady` | Text contradiction refuses before writing; restoring old content cannot rearm |

The November 5 sample C passed case 1. Case 2 matched the original editor
twice but stopped at the transaction guard with zero commit requests. The
subsequent diagnostic D identified six window events, including a system window
added. Automatic testing is now deferred.
Keep each positive case's exact field contents, caret, single commit,
confirmation, zero intermediate result panels and clean completion as its
success criteria. A reason code on a failed test is evidence about that
failure. [Device record](DEVICE_TESTS.md#system-managed-swiftkey-and-first-guarded-return-on-november-5)

Compare an event's framework timestamp to the fresh editor read and transaction
creation using uptime. Android stamps accessibility events when dispatching
them. A timestamp preceding capture can identify a queued notification. It
alone cannot establish when an editor changed or make a write eligible.
[Android 16 app event dispatch](https://github.com/aosp-mirror/platform_frameworks_base/blob/android16-release/core/java/android/view/accessibility/AccessibilityManager.java#L657-L666)
and [system event dispatch](https://github.com/aosp-mirror/platform_frameworks_base/blob/android16-release/services/accessibility/java/com/android/server/accessibility/AccessibilityManagerService.java#L4387-L4393).

For example, the first (negative) gate is:

```powershell
adb -d shell am instrument --user 0 -w -r -e class 'com.koscribe.smoke.StableOverlayAutoSmokeTest#changedIdenticalFieldFallsBackBeforeWrite' -e stableAlternateReady true com.koscribe.smoke/com.koscribe.smoke.KoScribeTestRunner
```

Restore and verify the saved secure MVP after the session. Do not advance to a
positive write because a negative test happened to refuse for an unrelated
reason. Do not retry an ambiguous commit or claim this sample is a translation.

## Authorized save-prompt dismissal (2026-10-24)

Andrew explicitly authorized clicking the visible save prompt's negative action.
The separate helper must be run as its exact method with its sole flag:

```powershell
adb -d shell am instrument --user 0 -w -r -e class 'com.koscribe.smoke.AutofillSaveDismissSmokeTest#dismissVisibleAndroidAutofillSaveNoOnce' -e autofillSaveNoReady true com.koscribe.smoke/com.koscribe.smoke.KoScribeTestRunner
```

It launches no Activity and changes no focus. It queries only Android SYSTEM
roots for android:id/autofill_save_no, requires a unique visible enabled clickable
negative button with an allowed dismiss-only label, refreshes/rechecks the same
node, and clicks at most once. It rejects Never. No credential/vault reads,
generic dialog navigation, Back, coordinates, or provider/global settings.
NOT_FOUND means no click. CLICK_ACCEPTED means action acceptance, not independent
proof of dialog dismissal. Oct24's one invocation returned NOT_FOUND in 0.038s.
Do not use this as an automatic retry loop or run it with other test flags.

## Contextual bubble synthetic checks (2026-10-24)

Use a reviewed RESELECT build with `BubbleLifecycleSmokeTest`. These tests launch
only the synthetic fixture, read/traverse only KoScribe and fixture nodes, and
use window metadata for IME presence. They do not insert, copy, send, change
keyboard selection, or open a conversation. The idle test drags only the
observed Ko control. Its protected field contains no actual credential.

Run one exact method at a time, with its sole flag:

| Method suffix after `com.koscribe.smoke.BubbleLifecycleSmokeTest#` | Sole flag |
| --- | --- |
| `idleBubbleTracksImeHideCycleDragAndProtectedField` | `-e bubbleIdleReady true` |
| `savedPositionRestoresAfterProductServiceRestart` | `-e bubbleRestoreReady true` |
| `draggedHostStaysPinnedThroughSyntheticStopAndCancel` | `-e bubbleMicReady true` |

Example:

```powershell
adb -d shell am instrument --user 0 -w -r -e class 'com.koscribe.smoke.BubbleLifecycleSmokeTest#idleBubbleTracksImeHideCycleDragAndProtectedField' -e bubbleIdleReady true com.koscribe.smoke/com.koscribe.smoke.KoScribeTestRunner
```

The idle gate checks keyboard show/hide, explicit Hide suppression, a confirmed
IME hide/show rearm, physical drag without accidental expansion, position after
reopening, and hiding for a password-type fixture. It saves only its observed
coordinates in the smoke package's private test preferences. After that pass,
root may reinstall the exact same product APK without clearing app data to
restart the service, verify reconnect, then run the restore-only method. Do not
reinstall or clear the smoke package's data between those two gates.

The mic regression drags/opens the host, records, toggles the fixture IME while
recording to check host continuity, then Stop/review/Cancel. Check exact-app
RECORD_AUDIO AppOps afterward. Source review and this test do not prove general
orientation, multi-window, external display, or app compatibility. Hide tests
the suppression behavior; it does not establish Copy or insertion dismissal.

Run while Andrew is not using the phone; app switches can correctly invalidate a session. Instrumentation completion can remove the fixture and return to a previous real app. Do not take post-test full-screen screenshots on the assumption that the synthetic editor is still visible. Prefer exact fixture assertions and code-only traces; capture pixels only after independently verifying the current foreground is an owned synthetic screen.

References: [AOSP instrumentation target restart](https://source.android.com/docs/core/tests/development/instrumentation), [UiAutomation flag](https://developer.android.com/reference/android/app/UiAutomation#FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES), [flagged acquisition](https://developer.android.com/reference/android/app/Instrumentation#getUiAutomation(int)).

## Synthetic visual checkpoints (2026-10-24)

Andrew requested a way for the agent to see the same screen during the manual
experiment. Use discrete USB screenshots after he confirms a test step, not a
claim of continuous video observation. No scrcpy/mirror installation is required.
The current screenshot path uses binary-safe adb exec-out screencap transfer.
Verify the owned synthetic fixture is focused before AND after capture; discard
on mismatch. Review Activity snapshots also require the known synthetic origin.
Keep temporary images only in ignored app/build/visual-checks, remove after use,
and never commit screenshots or caller/notification details.

Ordinary REVIEW windows deliberately use FLAG_SECURE. Only an explicit local
`-PkoscribeStartupArm=REVIEW -PkoscribeVisualTest=true` debug build makes its own
review/overlay visible to screenshots; the review title and result handle say
VISUAL TEST. The property defaults false, rejects other startup arms when true,
and release hard-codes OFF/false. It does not alter insertion or microphone guards.
Synthetic-only usage is an operational boundary, not enforced by the build flag:
restore the separately saved secure REVIEW APK after the visual session and
before using any real app. Never change device-wide capture/privacy controls.

Save and identify separate secure and visual APKs before rebuilding ordinary
output as OFF/false. Check generated BuildConfig for each and the release variant.
During a visual session, ask Andrew to pause after one short sequence, inspect,
then continue. The draft's existing three-minute deadline still applies; no
ended draft is reused. Do not auto-poll screenshots while awaiting a reply:
foreground metadata alone does not exclude an incoming-call/notification overlay.
Stop visual work on an interruption and resume only when Andrew says ready.
