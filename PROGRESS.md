# Current progress

Updated: 2026-10-24 (Asia/Seoul).

## Current state

The insertion-confirmation issue is fixed in the API-free REVIEW candidate on
our synthetic A35 editor. All seven REVIEW acceptance methods passed on the
same final product APK across separately reviewed smoke builds. No real-app
insertion acceptance, speech recognition, translation, or API integration is
claimed. Phase 1 remains open.

Andrew asked to continue. A debug-only "KoScribe test editor" launcher was added
to the smoke app, independently reviewed, built/linted, installed, and opened.
It launches only the internal synthetic fixture with the protected field omitted;
it forwards no caller data and automates no product action. Manual SwiftKey
review/edit -> confirmed insertion is now awaiting Andrew's result. Do not count
this as a pass until his report and the own-service state agree. Product unchanged.

Measured cause: after a commit, the input connection already showed the exact
expected text/caret while the same focused accessibility node retained an old
hint/unknown caret. Refreshing that node made the observations agree. Cache
versus read ordering was not isolated. The fix refreshes the same focused node
after reading the connection and rechecks identity, input generation, focus,
window, and device state before the unchanged exact confirmation. No retry,
guard exemption, or rehabilitation of an uncertain transaction was added.

The ready bubble now keeps Insert, Copy & close, and Cancel outside its scrolling
preview body. This resolved clipped controls for the long synthetic result.
Sol 6 Extra High implemented the changes; Astra Extra High independently reviewed
product and tests. Default/release OFF and recording-only RESELECT remain intact.

## Latest measured results

All rows below use final product SHA-256 2B00BE44...; exact hashes, methods,
smoke versions, earlier failures, and diagnostic timings are in DEVICE_TESTS.

| Synthetic REVIEW gate | Result |
| --- | --- |
| Edited English -> Copy, original draft preserved, bubble hidden until new IME cycle | PASS 6.708s |
| Edited English -> empty-field insertion, one commit, confirmed dismissal | PASS 4.372s |
| Long English -> middle insertion preserving Korean/emoji prefix and suffix | PASS 4.207s |
| Fixed Korean -> empty-field insertion with confirmation | PASS 4.826s |
| Selected range refused, text preserved, result retained | PASS 4.724s |
| Edited Korean fixed-source rejection -> Cancel, draft cleared | PASS 4.012s |
| Acknowledged but unchanged commit -> terminal uncertainty, no retry | PASS 4.538s |

The unchanged-commit test terminated on WINDOW_OR_FOCUS_CHANGED. It proves
no-retry/uncertainty behavior on that trigger, not the settled-context timeout
branch. Unit tests separately cover unchanged-context rejection. Automated
review edits use our own EditText accessibility action with SwiftKey visible;
actual SwiftKey keystrokes/composition remain untested.

Candidate/default OFF builds, 37 JVM tests (zero failures/errors/skips), lints
(zero errors; app 23/smoke 3 warnings), and APK signatures passed. Final test-only
Cancel scrolling fix was rebuilt/linted, independently reviewed, and passed.
Its assertions require a cleared draft, unchanged capture/commit counts, and
absence of own review controls even when they are offscreen.

A phone call interrupted one recording before insertion; the unchanged retry
passed after Andrew was finished. Other startup runs lost fixture focus to
SYSTEM windows; their precise cause was not established. Earlier notification
shade interference is documented separately; do not attribute every SYSTEM
interruption to it or Proton Pass.

Andrew reported repeated Proton Pass save prompts. No password was required or
entered. The unused empty password-type fixture is a likely heuristic trigger,
not a confirmed diagnosis. Autofill opt-out alone did not prevent recurrence;
REVIEW tests now omit that field. Protected-field tests retain it by default.
Andrew authorized the prompt's negative action; one exact Android save-button
helper returned NOT_FOUND, so no click occurred. No Proton/global autofill
settings changed. Prompt cessation has not been established.

## Phone and installed artifacts

Galaxy A35 SM-A356E, Android 16/API 36, One UI 8.5; user 0 only. SwiftKey remains
selected; Wispr remains enabled. Default SMS app is Google Messages. Phone is
connected/unlocked for the pending manual gate. The synthetic fixture was
confirmed foreground after launch; do not move it while Andrew tests.
Verify live readiness next session. Fixture cleanup may return
to a real app; never assume the foreground is still synthetic.

Installed product: `app/build/confirmation-refresh/REVIEW.apk` (ignored).
SHA-256: `2B00BE44077579BBD92D3D6A8D7FCED7E7391E17B05F56B5D1652444F4AD4D9E`.
Installed smoke: `app/build/manual-swiftkey/smoke.apk` (ignored).
SHA-256: `5D9D7CC793408B8C4C28159918E32810D8512F785425D4339128A36A57F1B648`.
This adds only the debug launcher; the previously tested smoke FC0A13E5... remains
saved at app/build/confirmation-refresh/smoke.apk.
Ordinary `app/build/outputs/apk/debug/app-debug.apk` is OFF; do not substitute it.
Prior diagnostic, refresh-only, and earlier smoke APKs are retained in ignored
build folders. Detailed source review hashes are in DEVICE_TESTS.

Baseline before the manual gate: REVIEW_CANCELLED, proof count 8, draft NONE, destination
captures 5, commit requests 4, cleanup pending false. Last confirmation UNCERTAIN
is retained from the preceding ignored-commit test; the final Cancel gate added
zero captures/commits. Exact-app AppOps confirmed mic use ended after 516ms.
No Send/Enter, real conversation access, screenshots, retained audio, credentials,
paid calls, permission changes, or keyboard changes occurred. Copy replaced the
clipboard with the previously disclosed fixed synthetic sample.

## Next work and gates

1. Manually edit a fixed English sample using actual SwiftKey keys, then exercise
   the own-screen review/result interaction. Current automated edits do not
   establish typing, prediction, or composition behavior. Coordinate with Andrew.
2. Check current-build contextual bubble and review ergonomics in real apps
   without insertion under the established scoped diagnostics. Any actual
   external insertion trial needs separate specific authorization; the earlier
   one-sample Kakao trial is closed. Do not run old confirmation tests or suites.
3. Before claiming general feasibility, cover selected/caret changes, app/field
   navigation and composition, small-height/large-font/rotation/split-screen,
   lock/service recovery, and KakaoTalk/Instagram/Google Messages acceptance.
4. Assess direct insertion's benefit against the accepted launcher -> mic ->
   editable English -> 요/반말 -> Copy & close -> manual paste fallback. Do not
   spend indefinitely on integration without measured user benefit.
5. Keep Phase 2 speech/credentials gated. No API key is needed for current work.

Earlier RESELECT synthetic gates proved keyboard-based visibility, dragging,
Hide suppression, and saved position after service restart. Earlier Stage 1
proved >3-second unbound recording -> Stop -> readonly review -> Cancel in Kakao
with zero destination captures. Those results apply to their recorded builds;
they are not full current-build real-app acceptance. See DEVICE_TESTS for history.

## Durable choices

- Bubble is the main interface; three visible modes 요 / 반말 / EN.
- Editable English review for every message initially; skip option later.
- Record/review without a destination. Prepare result, then Andrew taps the
  intended field and Insert; only that action starts a fresh guarded capture.
- Preserve existing text; one commit maximum; ambiguous attempt stays terminal.
  Keep SwiftKey; no replacement keyboard or automatic Send/Enter.
- Dismiss bubble after Copy or confirmed insertion until a new keyboard cycle.
- Personal on-phone key entry with Keystore-backed encryption, no key in Git/APK/
  logs. Translation choice gpt-6-luna/xhigh; OpenAI transcription baseline recorded
  in RESEARCH. Account access and paid quality/latency tests remain unverified.
- Final icon, permissions/battery/reboot UX, signing and distribution remain later.

## Repository and tools

Private repository: https://github.com/drew345/KoScribe . main tracks origin/main.
Confirmation checkpoint: 70e7b1e. Manual-test launcher work follows that checkpoint.
Inspect staged files and scan for secrets before publishing.

ADB/SDK: C:\AI\Codex\Tools\android-sdk (API 36/build tools 36.0.0).
JDK: C:\AI\Codex\Tools\openjdk21\jdk-21.0.12.1+1.
Gradle wrapper 8.13; cache C:\AI\Codex\Tools\gradle-cache.
Use adb -d, user 0, and the non-suppressing KoScribeTestRunner. Root alone
operates the phone; run exact opt-in methods separately and inspect every result.
