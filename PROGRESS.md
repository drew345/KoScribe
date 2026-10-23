# Current progress

Updated: 2026-10-23 (Asia/Seoul).

## Current direction

Phase 1 remains an API-free feasibility prototype. Andrew now explicitly chose:
finish recording and English review, prepare the result, tap the intended
message field, then tap Insert. This supersedes automatic return to the field
captured before recording. Recording/review and destination insertion must be
separate transactions. No destination is captured during recording; a later
explicit action starts a new fully guarded capture. See IMPLEMENTATION.md.

Sol 6 Extra High implements; Astra Extra High reviews; root alone operates the
phone. Real-app tests remain non-inserting. No API key, API integration, paid
call, transcription, or real translation has occurred. Phase 1 acceptance and
successful Kakao insertion remain unverified.

## Overnight checkpoint and next work

Stopped at Andrew's requested overnight checkpoint. Stage 1 is implemented,
independently reviewed, and passed its one exact phone gate. No Stage 2 work
has begun. Resume from this file and the latest SESSION_LOG entry tomorrow.

1. DONE: Unbound recording -> Stop -> readonly sample review -> Cancel, with >3-second
   capture. Own microphone/UI/lock/timeout cleanup remains; no editor access or
   destination session exists. Separate recording and destination counters.
2. NEXT: Editable English review, result preparation, and nonfocusable ready controls.
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
Messages. KakaoTalk self-chat was left empty with SwiftKey visible. Verify live
preconditions before each exact opt-in test; do not inspect chat history.

Installed recording-only RESELECT Stage 1 product SHA-256:
C0CC7F44848E350D9F645565A3FE5CD2DDF760BF844A19F6585DBA5422C7D69B
Saved at app/build/reselect-stage1/RESELECT.apk (ignored).
Smoke APK SHA-256:
9130F582FBFBF486EC799531F6F972A30FFC47E41333699D5FFF9D7BE007C3B3
Saved at app/build/reselect-stage1/smoke-RESELECT.apk (ignored).
Latest phase RESELECT_CANCELLED, cleanup pending false, mic ended after 4.176 s.
Ordinary app/build/outputs/apk/debug/app-debug.apk was rebuilt with OFF; it is
not the currently installed APK. The pre-experiment restore APK BC464039...
remains at app/build/pre-isolation-a15c15c.apk. All APKs are ignored artifacts.

Final normal and candidate builds passed. JVM tests: 29, zero failures
or errors. Lint: zero errors; app 18/smoke 3 warnings. Candidate signatures
verified. Source review hashes and exact test methods are in DEVICE_TESTS and
INSTRUMENTATION. The extra warnings are prototype string localization warnings.

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
reconfirmed PRIVATE during this work. main tracks origin/main; last published
checkpoint before this Stage 1 change is 6d4f1f8. The overnight Stage 1 commit
contains this handoff; use git log for its hash. Scan staged files before push.

ADB/SDK: C:\AI\Codex\Tools\android-sdk (ADB 37.0.1, API 36/build tools 36.0.0).
JDK: C:\AI\Codex\Tools\openjdk21\jdk-21.0.12.1+1.
Gradle wrapper 8.13; shared cache C:\AI\Codex\Tools\gradle-cache.
Use adb -d with user 0 and the custom non-suppressing KoScribeTestRunner.
