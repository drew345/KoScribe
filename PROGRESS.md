# Current progress

Updated: 2026-10-23 (Asia/Seoul).

## State

Phase 1 feasibility prototype built with Sol 6 Extra High and independently
reviewed by Astra Extra High. Baseline build, seven unit tests, lint, and APK
signature verification passed. A35 is connected; prototype installed in user 0
and microphone/accessibility granted. Local tests verified microphone Stop and
Cancel, safe refusal, and eight passing synthetic cross-app checks on the final
guard: empty/middle English, edited English review, polite Korean within Korean
and emoji text, casual Korean, selected-range refusal, Cancel, and leaving
review. Exact expected text and one-time insertion were checked with SwiftKey
visible. Compact recording/review dialog
and all insertion guards remain in place. No API integration or paid API call
has occurred. Phase 1 acceptance is not complete.
Private repository created and visibility verified:
https://github.com/drew345/KoScribe . Initial planning commit was pushed to
`main`; the Phase 1 baseline was later pushed as `cfdd6fb`. Reviewed device-test
fixes, eight synthetic results, and the permission audit were pushed as
`76fd0b4`; local `main` tracks `origin/main`.

## Confirmed this session

- Galaxy A35 5G, Android 16, One UI 8.5 (user-reported).
- First apps: KakaoTalk, Instagram DMs, SMS/RCS (Google Messages verified).
- Keep SwiftKey and installed Wispr Flow.
- Tap bubble -> choose 요 / 반말 / EN -> record -> Stop -> editable English
  review -> confirm -> translate if Korean -> safe immediate insert.
- English review appears for every message initially; skip option comes later.
- User supplies a key on the phone; no embedded key or Git credential storage.
- Translation: `gpt-6-luna` at `xhigh` (Extra High).
- Speech recognition: OpenAI cloud transcription first; documented baseline
  `gpt-transcribe`. Some additional processing time is acceptable.
- Sol 6 Extra High coding; Astra Extra High checking; main planner/overseer.
- Andrew offered Astra Ultra for implementation/diagnosis. Root accepted that
  upgrade for the next window-occlusion correction and independent review.
- Continue SwiftKey-open tests, including middle insertion preserving both
  sides of the caret. A better bubble icon is explicitly deferred until later.

## Open decisions / checks

- Before Phase 2: verify actual API-account access to GPT-6 Luna and the
  selected OpenAI transcription model, plus billing/spending controls.
- Measure Stop-to-transcript and confirmation-to-insertion delays during
  testing. No fixed numeric target; normal dictation length is still unknown.
- Personal installation/update method; whether any wider distribution is wanted.
- Bubble icon/placement, detailed cancel/recovery UI, and settings screen.
- Complete safe editor insertion and coexistence tests. Phone build, actual
  SMS package, and microphone start/Stop/Cancel have been checked.

## Next action

Obtain approval for additional Kakao self-chat diagnostics, starting with the
reviewed recording -> Stop -> Cancel method in `docs/INSTRUMENTATION.md`.
The code-only timing update and that separate no-confirmation method passed
Astra review, both builds, 27 unit tests, lint, and APK signature verification.
They are not installed or device-tested. Product guard behavior is unchanged.
Prepared product APK SHA-256:
`67FDAE782F8A71010BCD0457BCF545DAF1D9D6547A925826A09D9E9FD138E2C6`.
After approval, install both fresh APKs and verify service connection before the
exact diagnostic method. Do not run the old confirmation method or full suite.
Retained first-content fields belong to the latest successful capture; do not
attribute old diagnostics to a preflight failure.

Diagnose the first actual Kakao flow's conservative refusal using code-only
metadata. Andrew explicitly approved the opt-in unsent self-chat exception;
Sol implemented it, Astra reviewed it, and build/lint passed. Two initial
preflights made no product action (test caret check corrected to match the
approved production rule, then hidden SwiftKey restored by Andrew). The actual
flow reached confirmation but refused on TARGET_CONTENT_CHANGED_OTHER_NODE
(content flags 4 during recording startup). Original empty field remained
unchanged; no commit was requested for that session. Microphone stopped and
SwiftKey remains selected. Do not rerun this confirmed session. Scope/result:
`docs/EXTERNAL_DRAFT_TEST_PROPOSAL.md`, `docs/DEVICE_TESTS.md`.
The final independently reviewed modal guard is installed: uninterrupted exact
panel ownership, strict alternating visibility, fresh original root/editor on
every reappearance, and a separately verified return to the captured target.
All existing content/navigation/caret/context/one-commit checks remain. Fixed
height and no-move-animation experiments were reverted. The narrowed service
integration passed automatic approval after an earlier broader helper change
was rejected; that service-code block is resolved. The external-test automatic
approval block also cleared after Andrew's explicit scoped authorization.
The edited-review smoke uses a scoped synthetic text action with SwiftKey
visible; actual SwiftKey keystroke/composition editing still needs manual
verification. Same-view semantic conversation reuse while hidden remains
unobservable. Do not claim real-app compatibility from synthetic passes.
The compact pill has no white surround; final icon is deferred. Microphone is
stopped and SwiftKey remains selected. A fixture-only keep-screen-on flag was
reviewed after the eight passes; smoke assembly/lint passed and the updated
test APK was installed for that trial. The newer timing/Cancel diagnostic APKs
described above remain uninstalled; the phone still has the tested product.
SwiftKey remains selected and Wispr remains enabled. Follow `docs/PHASE1.md`
and `docs/DEVICE_TESTS.md`.
No API key is needed for Phase 1.

The requested permission audit is recorded in `docs/RESEARCH.md`. Read-only ADB
confirmed microphone/accessibility enabled; the accessibility bubble needs no
separate draw-over-apps grant. KoScribe has no battery exemption, unlike installed
Wispr/DictoKey. Samsung per-app sleep lists and long-idle/reboot reliability
remain unverified; no phone setting was changed. Notification/foreground-service
features are absent from this prototype and must not be mistaken for missing
required grants. Android grants did not cause the later Kakao guard refusal.

## Verification so far

Official Android/competitor/OpenAI documentation reviewed. Codex local model
catalog confirms requested coding/review models and reasoning settings.
Authenticated OpenAI model availability is not yet verified; measured phone
results are recorded in `docs/DEVICE_TESTS.md`.
No API key was retrieved; current process has no OPENAI_API_KEY.
Planning review added protected-field exclusions, same-app conversation-switch
checks, cancellation ownership, and at-most-once insertion. Staged-file secret
patterns and ignore rules checked during initialization and before the Phase 1
publish. No credential patterns or unexpected sensitive/generated paths were
found in the staged prototype. GitHub visibility reconfirmed PRIVATE.
English-review flow approved; safe return from editing to the intended message
field remains an explicit device-test gate.
PhoneCare resolved the custom tool locations on ROBERTSLGGRAM17: ADB 37.0.1
and Android SDK under `C:\AI\Codex\Tools\android-sdk`, Microsoft JDK
21.0.12.1 under `C:\AI\Codex\Tools\openjdk21\jdk-21.0.12.1+1`.
Existing platform/build-tools were copied into standard API 36 / 36.0.0 SDK
directories without removing originals. Official Gradle 8.13 was downloaded,
SHA-256 verified, and installed under `C:\AI\Codex\Tools\gradle\gradle-8.13`.
Shared Gradle cache: `C:\AI\Codex\Tools\gradle-cache`.

The prototype uses a nonfocusable accessibility bubble, visible recording
Activity, editable English sample review, fixed Korean examples, and explicit
Copy recovery. Audio is discarded. No INTERNET permission or persistent
message/audio storage exists. Complete device acceptance remains unverified.

Final checks: `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug` passed;
current compact-panel build has 27 tests, 0 failures/errors. Lint: 0 errors,
15 warnings in the product, 3 warnings in smoke. Current APK signature verified;
its packaged permission list contains only RECORD_AUDIO. Tested APK SHA-256:
`30DDA116F39FAB7261E5D0DEC1B98FF7F46EDC427B9D622909A47FEBFDCF4E28`.

Astra found no remaining demonstrated blocker for controlled synthetic local
editor testing after fixes to window binding, review return, navigation,
composition evidence, and stale cancellation. Conservative return events may
refuse normal insertion. Unknown composition, Samsung/SwiftKey behavior,
same-app conversation reuse, and cross-process commit timing still require
device evidence. Root-only backup exclusions must be expanded/tested before
Phase 2 credential storage.
