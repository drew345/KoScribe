# Current progress

Updated: 2026-10-24 (Asia/Seoul).

## Current direction

Andrew resumed with a new first priority: the idle bubble should appear only
while editing/keyboard visible, be draggable with a remembered position, and
disappear after Copy or successful insertion. The contextual idle bubble is now
implemented and independently reviewed in the installed RESELECT candidate.
Three exact synthetic phone gates passed: visibility/drag/Hide, saved position
after service restart, and recording/Stop/Cancel through keyboard transitions.
Copy/Insert dismissal is still pending because this arm has neither action.
Explicit Hide proves the suppression mechanism only. Next: carry dismissal
into the independently owned review/result flow before broader acceptance.

Accepted minimum fallback: home-screen app -> mic -> editable English -> choose
요/반말 -> translate -> Copy & close -> manual long-press paste. The bubble and
direct insertion must provide a demonstrated benefit over that workflow.

Phase 1 remains an API-free feasibility prototype. Andrew now explicitly chose:
finish recording and English review, prepare the result, tap the intended
message field, then tap Insert. This supersedes automatic return to the field
captured before recording. Recording/review and destination insertion must be
separate transactions. No destination is captured during recording; a later
explicit action starts a new fully guarded capture. See IMPLEMENTATION.md.

Sol 6 Extra High implemented policy/tests; Astra Ultra handled difficult geometry
and an independent Astra Ultra reviewer checked it. Root alone operates the
phone. Real-app tests remain non-inserting. No API key, API integration, paid
call, transcription, or real translation has occurred. Phase 1 acceptance and
successful Kakao insertion remain unverified.

## Prior checkpoint and deferred next work

The overnight checkpoint was completed. Stage 1 is implemented,
independently reviewed, and passed its one exact phone gate. No Stage 2 work
has begun. The Oct24 synthetic bubble gate is complete; real-app contextual
visibility, completion dismissal, and broader geometry remain unverified.

1. DONE: Unbound recording -> Stop -> readonly sample review -> Cancel, with >3-second
   capture. Own microphone/UI/lock/timeout cleanup remains; no editor access or
   destination session exists. Separate recording and destination counters.
2. NEXT: Editable English review, result preparation, and nonfocusable ready controls,
   incorporating Copy/confirmed-insertion dismissal without reopening on the
   same keyboard session. Until those actions exist, do not claim dismissal
   acceptance from the Hide test. Keep the accepted launcher fallback practical.
   Korean prototype modes still require the exact fixed English sample; EN can
   use edits. No destination capture during editing/IME transitions. Prefer a
   dedicated own review Activity after mic Stop with independent draft ownership;
   do not reuse the old target-bound SessionActivity transaction. See the IME
   lifecycle proposal and API 36 caveat in IMPLEMENTATION.md.
3. Explicit Check destination (no insert): fresh full capture/revalidation and
   terminally end the check. No commit, Copy, or silent retargeting. Ask Andrew
   to retap the intended composer when the test needs that real user action.

Only after separate device-test authorization may an actual external insertion
trial run. The prior one-sample trial is closed. Do not run the old confirmation
method or whole suites. Default/release OFF retains the older prototype until
replacement gates pass. Before Stage 3, replace Stage 1's unconditional RESELECT
event/input callback exits with phase-aware full destination guards.

## Latest measured results

- Oct24 contextual bubble gates passed on the synthetic fixture with SwiftKey:
  idle show/hide, explicit Hide through the same IME session, reappearance after
  observed keyboard hide/show, physical drag without opening the menu, saved
  position on reopen, and no bubble in the empty password-type fixture (4.889s).
- Reinstalling the exact same product without clearing data restarted its
  service. Saved-position restoration passed separately (1.883s).
- The recording regression passed (4.105s): same expanded host while the
  fixture keyboard hid/reappeared, own positive unsilenced microphone proof,
  Stop -> readonly sample -> Cancel; no destination capture, no cleanup pending.
  AppOps confirmed ended microphone use of 984ms. This short test does not
  replace the prior >3-second Kakao recording result below.
- Initial smoke failures exposed case-sensitive matching of Android button
  capitalization and an immediate panel lookup before layout. Both were fixed
  in the test and independently reviewed. The cramped expanded menu was also
  made into compact mode/footer rows before the three successful gates.
- New gates used only own synthetic controls; no real conversation was opened,
  no insertion/Copy/Send occurred, and no recordings or screenshots were saved.
  Rotation, split screen, reboot, and current-build real-app behavior remain
  unverified. Full evidence and hashes are in docs/DEVICE_TESTS.md.
- RESELECT Stage 1 passed once in 6.652 s: positive own-recorder reads and
  unsilenced configuration, 3.2 seconds of continued recording after proof,
  Stop -> readonly fixed sample -> Cancel. One recording proof, zero destination
  captures, no cleanup pending. Same empty focused Kakao field and visible
  selected SwiftKey were verified throughout and immediately/1.2s after cleanup.
  Exact-app AppOps confirmed microphone use ended after 4.176 s. This is not
  transcription quality, editable review, insertion, or general reliability proof.
- Eight earlier synthetic cross-app cases passed with SwiftKey visible: empty/
  middle English, edited English, Korean/emoji middle, casual Korean, selected
  range refusal, Cancel, and leaving review. They do not establish real-app
  compatibility or actual SwiftKey keystroke/composition behavior.
- Original Kakao flow conservatively refused OTHER_NODE content-description
  flag 4. Isolation HOLD/COLLAPSE passed, while Activity PANEL launch refused.
- First separate-window OVERLAY candidate refused target child-window change
  512 before microphone proof. Recording ended after 136 ms; draft stayed empty.
- Reusing the already-expanded bubble reached positive own-recorder reads and
  non-null unsilenced configuration with the original empty Kakao field focused
  and SwiftKey visible. Direct Cancel passed (3.555 s; mic use ended at 1.537 s).
- One subsequent run executed Stop and released the mic (2.529 s), then review
  cancelled on 512. Final timing run refused before Stop: event occurred 2,625
  ms after capture completion and was delivered 5 ms later. This is not stale
  pre-capture delivery; Stop/review is not necessary for the refusal.
- Baseline/current child-window snapshots both retained the same two IDs/types/
  order. This does not exclude a transient change or prove harmlessness. No
  event exemption, grace period, guard relaxation, or recapture was added.
- Every completed cleanup assertion preserved the same empty field and SwiftKey;
  no insertion, Send/Enter, Copy, screenshot, chat-history traversal, API call,
  recording retention, or permission change occurred. Root confirmed mic ended.

Identical target-bound diagnostics are exhausted. The accepted new workflow
avoids an early destination binding; it must not reuse an invalidated binding.
Detailed hashes, source reviews, measurements, and limits: docs/DEVICE_TESTS.md.

## Device and installed build

Galaxy A35 SM-A356E, Android 16/API 36, One UI 8.5. Connected over USB; user 0
only. SwiftKey remains selected, Wispr remains enabled. Default SMS is Google
Messages. Latest synthetic fixture cleanup left the keyboard hidden; do not
assume the foreground is still synthetic. Verify live preconditions before each
exact opt-in test; do not inspect chat history.

Installed contextual-bubble RESELECT product SHA-256:
7568BD4C21D0B7E66A232BCED5F6E4C48FD588195A576AA43F701713CB4E283B
Saved at app/build/bubble-lifecycle/RESELECT.apk (ignored).
Smoke APK SHA-256:
598F1278742DCE9BA4EA5E2311A85D3C35EB85EAA65F51EADF9D8FE175DC0828
Saved at app/build/bubble-lifecycle/smoke.apk (ignored). Idle/restore passed with
earlier smoke 735770EB...; only the mic method needed the final test-only wait fix.
Latest phase RESELECT_CANCELLED, proof count 1, destination count 0, cleanup
pending false, mic ended after 984ms. A post-test service dump timed out once;
one bounded retry returned the expected terminal state.
Ordinary app/build/outputs/apk/debug/app-debug.apk was rebuilt with OFF;
do not install it over the tested candidate. The prior Stage1 and pre-experiment
restore APKs remain saved in their ignored folders.

Candidate, final smoke, and default OFF build/lint checks passed.
JVM tests: 32, zero failures/errors/skips. Lint: zero errors; app 20/smoke 3 warnings.
Signatures verified. Source review hashes and exact methods are in DEVICE_TESTS
and INSTRUMENTATION. Warnings include prototype strings, existing tool-version
advice, touch accessibility, and intentional screen-coordinate gravity.

## Durable choices and later gates

- Three visible choices: 요 / 반말 / EN. Selected mode remains visible.
- Editable English review for every message initially; skip option later.
- Keep SwiftKey; never create a keyboard or automatically send/submit.
- User enters their own key on the phone; Keystore-backed encryption, no key in
  Git/APK/logs. Dedicated key/budget/account access remain Phase 2 work.
- Translation gpt-6-luna/xhigh; OpenAI transcription selected (documented
  gpt-transcribe baseline). Authenticated account access remains unverified.
- Real apps: KakaoTalk, Instagram DMs, Google Messages. Middle insertion,
  composition/navigation safety, and daily-use reliability still need gates.
- Final bubble icon, battery/sleep settings, reboot recovery, and distribution
  remain later work. Microphone/accessibility grants are enabled. No separate
  draw-over-apps permission is needed for the accessibility bubble. Permission
  audit is in RESEARCH; no battery exemption or new FGS permission was added.

## Repository and local tools

Private GitHub repository: https://github.com/drew345/KoScribe . Visibility
reconfirmed PRIVATE on Oct24. main tracks origin/main. This Oct24 bubble
checkpoint follows 709df8d; use git log for its final hash. Inspect staged files
and scan for secrets before every push.

ADB/SDK: C:\AI\Codex\Tools\android-sdk (ADB 37.0.1, API 36/build tools 36.0.0).
JDK: C:\AI\Codex\Tools\openjdk21\jdk-21.0.12.1+1.
Gradle wrapper 8.13; shared cache C:\AI\Codex\Tools\gradle-cache.
Use adb -d with user 0 and the custom non-suppressing KoScribeTestRunner.
