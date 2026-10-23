# Phase 1 device acceptance record

Status: eight normal synthetic cross-app checks passed on the final modal guard
on 2026-10-23, including edited English review return and Korean/emoji insertion.
First Kakao flow safely refused insertion; other external-app checks and actual
SwiftKey keystroke editing remain pending.

## Environment to verify

- Verified Galaxy A35 5G (SM-A356E), Android 16 / API 36, One UI property
  `80500` (8.5), build `BP4A.251205.006.A356EXXSBDZH1`.
- Exactly one authorized physical device. Current user is main user 0; other
  existing profiles were left unchanged. Installed `com.koscribe.prototype`
  only into user 0 and launched successfully.
- SwiftKey 9.13.18.4 remains the selected input method.
- Wispr Flow 2.8.0 remains installed and its accessibility service enabled.
- KakaoTalk 26.9.0; Instagram 452.1.0.53.64.
- Default SMS role is Google Messages (`com.google.android.apps.messaging`),
  version `messages.android_20261007_04_RC00.phone_samsung_dynamic`.
- No personal messages or contacts were collected. Device identifiers are not
  retained in this record.

## Acceptance matrix

Use synthetic messages in the local test editor first. Real-app tests remain
unsent drafts; Andrew controls any manual Send. Record observed results, not
inferences from unit tests.

| Check | Local editor | KakaoTalk | Instagram DM | SMS/RCS |
| --- | --- | --- | --- | --- |
| Bubble visible; SwiftKey stays selected; Wispr coexists | Pending | Pending | Pending | Pending |
| Choose 요 / 반말 / EN; selected mode visible | All three fixed-sample paths passed | Pending | Pending | Pending |
| Microphone starts; Stop/Cancel/background/lock stop it | Pending | Pending | Pending | Pending |
| Review/edit English with SwiftKey; return to original field | Edited-content return passed with IME visible; actual keypresses pending | Pending | Pending | Pending |
| Empty/beginning/middle/end insertion; existing text preserved | Empty and middle passed with SwiftKey visible; middle exact preservation repeated | Empty draft unchanged after guard refusal; insertion unverified | Pending | Pending |
| Existing Korean, English, emoji; composition preserved/refused | Exact Korean/English/emoji preserved; known active composition refused separately | Pending | Pending | Pending |
| Selected text or unknown caret refuses automatic insertion | Selected range refused; exact draft preserved | Pending | Pending | Pending |
| Changed text/field/app/conversation (including switch-back) refuses | Pending | Pending | Pending | Pending |
| Cancel, repeated controls, rotation, service loss do not insert late | Cancel and leaving review passed; other cases pending | Pending | Pending | Pending |
| No automatic Send/Enter; uncertain outcome never auto-retries | Pending | Pending | Pending | Pending |
| Explicit Copy recovery works after refusal | Pending | Pending | Pending | Pending |

Separately check password/PIN/nontext fields, unavailable microphone, denied
permission, and unavailable accessibility service. Do not enter real secrets.
Inspect Samsung per-app battery/sleep settings and test background/idle survival,
reboot recovery, and global microphone blocking. The permission metadata audit
in `RESEARCH.md` confirms current grants, not those reliability behaviors.
Build warnings and unit-test results belong in `PROGRESS.md`; failures here
must describe the observed action/result without saving personal chat text.

## Gate

Phase 2 remains blocked on the required device evidence and any necessary
Phase 1 fixes. Unsupported fields must refuse safely. A passing local editor
test does not establish compatibility with another app.

## Initial local observations (prototype cfdd6fb)

- Bubble appears with all three modes; SwiftKey remains selected. Wispr's
  accessibility grant remains enabled; full Wispr functional coexistence is
  not yet tested.
- An empty-field attempt refused before recording with a generic observation
  error. Diagnostic refinement is in progress; cause not yet established.
- A synthetic word with active SwiftKey composition was refused. Finishing the
  word with a space allowed an EN recording session to start.
- AppOps confirmed RECORD_AUDIO running during capture, then not running after
  Stop. Fixed English review appeared, and confirmation returned to the local
  destination. Insertion was refused as TARGET_CHANGED; original text remained.
  Copy recovery was offered (not used yet).
- A separate recording/Cancel test stopped the microphone and left the local
  draft unchanged after the delayed-insertion interval.

## Diagnostic updates and empty-field fix

- Code-only in-memory service diagnostics confirmed all three empty-editor
  conditions: empty input connection at offset/caret zero, missing node
  selection endpoints, and node empty/showing hint. AOSP explains that empty
  TextViews omit accessibility selection even with a valid input caret.
- Automatic review initially rejected the exception under the earlier
  diagnostic-only scope. Andrew explicitly approved the exact narrow rule;
  Sol implemented it and Astra reviewed it. Build, ten unit tests, and lint
  passed. Empty local editor now starts EN recording successfully.
- Redundant status-label updates were removed. Full-screen return still emits
  original-root CONTENT_CHANGED flags 0, two original-root STATE_CHANGED flags
  0, editor focus, and input start. It refuses insertion and preserves text.
- Root content flags 0 mean unspecified, not automatically harmless. Rather
  than ignore this potential navigation signal, a floating recording/review
  Activity experiment keeps the target visible and retains current guards.

## Compact-panel observations

- Installed the native floating dialog. Recording and English review remain
  visible over the destination, with Stop/Cancel controls and a visible mode.
- First confirmation returned with one target window-state event and no
  target-change invalidation, but the session had expired during a long test
  delay. It refused with EXPIRED; this is not a successful insertion.
- A later coordinate-driven attempt missed the moved review control and
  cancelled. Switched to controls identified by exact labels for reliable tests.
- The first app-targeted instrumentation run failed all three cases in setup:
  Android restarted the instrumented app and left its accessibility service
  disconnected/marked crashed. No insertion test body ran. DONT_SUPPRESS avoids
  UiAutomation suppression but cannot prevent the target process restart.
- SwiftKey remains selected; Wispr remains bound. Requested a visible user
  toggle of KoScribe accessibility and a separate self-targeted test APK.
- No sample has yet been confirmed inserted. Real-app drafts remain pending.

## First successful insertion and remaining layout issue

- Andrew toggled KoScribe accessibility off/on. The self-targeted smoke runner
  preserves the service process and Wispr; it does not change the keyboard.
- Paced automated and manual keyboard-visible attempts reached confirmation
  but refused a CONTENT_CHANGED flags 1 event. The own-fixture classifier
  identified its source as EDITOR_ANCESTOR_SCROLL. Fresh field/caret checks
  passed, but the navigation guard correctly remained decisive.
- `stateUnchanged|adjustResize` did not remove the event and was reverted.
- `emptyEditorKeyboardHiddenDiagnostic` passed: the runner dismissed the
  visible keyboard only while the own editor/window was focused, then used
  the normal bubble/record/Stop/confirm flow. Exact English sample text appeared
  once; trace showed COMMIT_REQUESTED_ONCE and no invalidation. A screenshot
  confirmed the sample in the local field. Microphone was stopped afterward;
  SwiftKey remained selected and KoScribe/Wispr accessibility remained enabled.
- This establishes the insertion API path with IME initially hidden, not the
  ordinary keyboard-visible flow or external-app compatibility. Astra reviewed
  the controlled diagnostic and recommended moving the synthetic editor out of
  its long instructions ScrollView before retesting normal insertion.

## Cross-app synthetic editor and current blocker

- Moved the original fixture above its settings ScrollView, then added a
  minimal standard EditText in the separate smoke APK. This tests a different
  package/process without personal messages or a Send control. The runner
  verifies fixture window/editor focus and an IME window before capture,
  records for two seconds, and waits for review before confirming.
- The cross-app normal test still refuses. Code-only evidence shows target
  REMOVED while the recording panel is visible and focused; after review
  starts, target ADDED has the same original root while KoScribe remains
  focused. Current guard refuses TARGET_WINDOW_REAPPEARED.
- Astra's AOSP review confirms these events describe the reported interactive
  window list, which can change through modal occlusion. Content events may be
  filtered while a window is unreported. A proposed narrow paired rule is being
  independently reviewed; no generic content/navigation exception is accepted.
- Andrew requested continued SwiftKey-open middle insertion tests and offered
  Astra Ultra. The next correction and independent review use Astra Ultra.
- Removed the collapsed bubble's opaque padded square. Built/installed and
  visually verified a compact rounded Ko button with a transparent surround;
  expanded choices and window type/flags remain unchanged. Final icon deferred.

## Paired-window correction and next focus experiment

- Astra Ultra implemented and independently reviewed a session-bound, one-use
  recording-removal/review-reappearance allowance. It requires the exact panel
  window, uninterrupted lifecycle/focus, and refreshed original root/editor
  identities. Duplicate pairs refuse, including during final return. Existing
  content, selection, context, expiry, cancellation, and one-commit guards remain.
- Product/smoke builds, 23 unit tests, and lint passed. Installed in user 0;
  accessibility reconnected without a manual toggle.
- `emptyEditorEnglishInsertsExactlyOnce` passed with SwiftKey visible before
  capture. Exact sample appeared once; trace ended COMMIT_REQUESTED_ONCE with
  no invalidation. This run used ordinary final-return reappearance and did not
  exercise the newly allowed early pair.
- Middle-caret test exercised the early pair successfully, then safely refused
  a second removal during review: DUPLICATE_TARGET_WINDOW_PAIR. It has not yet
  established successful nonempty insertion.
- Selected-range negative passed: SELECTION_NOT_COLLAPSED, no recording, exact
  synthetic draft unchanged. A cancellation test instead refused at capture
  with ACTIVE_COMPOSITION; this did not test cancellation. Its fixture now ends
  in whitespace to represent a completed word before recording.
- Next experiment prevents automatic focus in the review EditText; explicit
  Edit English will activate it. No touch-through flag or broader window-event
  exception is planned. Callback ordering and edited-review return still need
  phone evidence.

## Explicit focus and edited-review results

- Keeping focus on the nonediting review container allowed normal middle
  insertion. The test checked exact prefix + sample + suffix, then checked
  again for a duplicate. Trace showed one COMMIT_REQUESTED_ONCE and no
  invalidation. After Andrew reported temporary Messages/Kakao use and then
  finished, the uncontended middle test passed again with the same trace.
- Cancel after Stop and leaving review both passed exact synthetic-draft
  preservation with no delayed insertion. The microphone was stopped and
  SwiftKey remained selected. Some sessions still showed duplicate occlusion
  before cancellation, so these passes do not prove a clean insertion path.
- The explicit edited-English test verified the review field and visible IME,
  edited only that synthetic field, and confirmed. It refused a second target
  removal after the first accepted reappearance, with KoScribe still focused.
  This was reproduced after Andrew finished using the phone.
- Giving the panel a fixed bounded viewport height did not change that edited
  sequence. The geometry experiment is being reverted; the explicit-focus
  improvement remains. A narrowly owned edit-transition rule is under review.
- A session-dialog-only no-move-animation experiment also failed: target
  reappearance occurred during recording before Stop, which the phase-bound
  gate refused. The API-34-qualified flag was removed after this result.
  Neither fixed height nor disabling movement animation is retained. The next
  review assesses continuous exact-panel ownership with fresh identity checks
  on each visibility return, rather than arbitrary phase/count assumptions.
- An attempted post-test screenshot found a real app after the fixture exited;
  the local image was immediately deleted and is not retained or published.
  Future verification uses fixture assertions/code-only traces unless an owned
  foreground screen has independently been verified immediately before capture.

## Final modal policy and successful synthetic checks

- The phase/count rule was replaced with strict alternating visible/hidden
  states under continuous exact-panel ownership. Each addition requires freshly
  verified original root/editor identities. Confirmation stops modal handling;
  only a verified return to the captured package/window can complete an absent
  target's return. Removal after confirmation refuses. Precommit requires the
  target present and all original field/caret/context/input checks.
- Automatic review rejected an initial service patch that broadened the focus
  helper. A safer version retained the exact own-package panel helper and added
  a separate captured-target return helper; it passed automatic review. Astra
  Ultra independently cleared source and 17 gate tests; 27 total unit tests and
  lint passed. The final product APK was installed and signature verified.
- After Andrew unlocked the phone, all eight normal synthetic cases passed:
  English empty and middle insertion; explicitly edited English review return;
  polite Korean between Korean/emoji prefix and suffix; casual Korean into an
  empty field; selected-range refusal; Cancel; and leaving review. Successful
  insertion traces show one commit and no invalidation. The edited case verified
  two modal reappearances and the final return before its single commit.
- Exact draft assertions, not screenshots, establish these results. The edited
  case uses a scoped synthetic text action while SwiftKey is visible; actual
  keyboard typing/composition is not yet covered. Microphone was stopped and
  SwiftKey remained selected afterward. The fixture-only keep-screen-on flag
  was reviewed after these passes and is not part of their measured build.
- A modal may hide accessibility content events. Same-view conversation changes
  that restore identical observed field state remain unobservable. These tests
  do not resolve that limitation or establish KakaoTalk/Instagram/SMS compatibility.

## First opt-in Kakao preflight

After Andrew explicitly approved one unsent self-chat sample, Sol added the
separate default-skipped test; Astra reviewed it and smoke assembly/lint passed.
The first opted-in invocation stopped at `KAKAO_SELECTION_NOT_KNOWN_EMPTY`
before any KoScribe button, recording, confirmation, or draft mutation. Empty,
editable, nonpassword text-field checks had passed; numeric caret endpoints
were not logged, so this alone does not establish their exact values.
Review found the test's node-only 0/0 requirement stricter than the already
approved production empty-caret/input-connection rule. Sol aligned only the
test's empty-node check with that rule; Astra rereviewed the final source and
assembly/lint passed. Exact result checks still require known collapsed
selection. The second preflight then stopped at SWIFTKEY_NOT_VISIBLE before
any product action. Scoped input-method metadata confirmed the keyboard hidden;
Andrew restored it.

The subsequent flow reached recording, Stop, review, and confirmation, then
timed out at KAKAO_RESULT_NOT_EXACT. The test repeatedly verified the original
field identity and unchanged empty/hinted state. Code-only service trace showed
OBS_VERIFIED_EMPTY_CARET and MODE_SELECTED_POLITE, followed immediately by
CONTENT_FLAGS_4 and first invalidation TARGET_CONTENT_CHANGED_OTHER_NODE during
recording startup. Later lifecycle/modal/confirmation invalidations followed;
no commit was requested for this session (the earlier commit in the trace was
from the previous synthetic run). Microphone stopped after 2.516 seconds and
SwiftKey stayed selected. No Send, screenshot, chat-history traversal, or field
text logging occurred. This is a conservative product refusal, not a missing
permission or a successful Kakao insertion. Do not rerun this confirmed session.

Observation methods evolved from screencap/state queries to scoped fixture
assertions and code-only traces; the current screenshot precautions above apply.
Stock AOSP
`uiautomator dump` connects with flags 0 and temporarily suppresses/unbinds
other accessibility services. Initial permission inspection used it before any
live recording. Do not run stock
UI dumps during these tests. Sources: [dump command](https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android16-release/cmds/uiautomator/cmds/uiautomator/src/com/android/commands/uiautomator/DumpCommand.java),
[Android accessibility service handling](https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android16-release/services/accessibility/java/com/android/server/accessibility/AccessibilityManagerService.java).
