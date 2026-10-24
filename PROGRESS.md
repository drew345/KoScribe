# Current progress

Updated: 2026-10-24 (Asia/Seoul).

## Current state

The approved postcommit confirmation change is implemented, independently
reviewed, and passed seven separate synthetic phone checks on secure3AA18446.
Exact composed results confirm and dismiss; wrong composed results remain
terminally uncertain. Middle preservation, Korean insertion, selection refusal,
edited-Korean cancellation, and unchanged-commit uncertainty all passed. Forty
unit tests passed; lints have zero errors. Manual empty-field Korean insertion
now passed in Kakao self-chat, Google Messages and Instagram. The bounded Phase1
feasibility gate is met from combined device/unit/source evidence. Phase 2's
first real EN transcription -> review -> Cancel and real speech -> SwiftKey edit
-> guarded insertion/dismissal tests now passed. User reported Stop half clipped;
a narrow recording-controls layout fix is installed for manual checking. Korean translation
remains unimplemented; Phase 2 cancel/error recovery checks are pending.

Andrew's newly approved one-attempt Kakao trial PASSED and is now complete.
He reported the exact expected Korean sample appeared and remained unsent,
replying that the flow worked exactly as instructed (including dismissal).
Own baseline proof7/captures6/commits5 -> proof8/captures7/commits6, CONFIRMED,
REVIEW_COMPLETED, draftNONE/cleanupfalse, microphone ended5.450s. SwiftKey remains
selected. This verifies one empty self-chat flow, not other apps or every editor
state. No agent conversation read or phone mutation occurred after his report.
Andrew was told he can remove the unsent Kakao sample manually. No more synthetic
composition diagnosis is needed unless new evidence fails. He approved one
manual unsent sample insertion per Google Messages and Instagram. Google
Messages PASSED: user reported insertion and flow as expected; proof9/captures8/
commits7, CONFIRMED, draftNONE/cleanupfalse, mic ended5.100s, SwiftKey selected.
He described addressing himself; whether he personally sent it is unspecified.
No automated Send occurred. Instagram also PASSED: user reported expected Korean
in the proper DM field and the flow worked perfectly; own proof10/captures9/
commits8, CONFIRMED, draftNONE/cleanupfalse, mic ended3.087s, SwiftKey selected.
Both one-attempt scopes are consumed. Real speech testing uses our synthetic editor.

Astra/xhigh reassessed the remaining gate and withdrew an unnecessary proposal
for another target-change fixture. Earlier REVIEW device tests exercised window/
focus termination without retry; current guard units passed and source retains
event/input invalidation and same-target/host checks before confirmation. All
three app checks now passed, satisfying the bounded Phase1 decision. Actual same-view
conversation switch-back, broad geometry/lifecycle, and daily-use reliability
remain limitations/future tests, not silently claimed passes or new ASR blockers.

## Active Phase 2 work

Sol6/xhigh implemented credential UI/storage and EN speech integration;
Astra/xhigh independently reviewed security, cleanup and final source. Root alone
builds/tests/operates the phone. Speech is opt-in debug `koscribeSpeech=true`,
requiring REVIEW and visualfalse; release and ordinary defaults remain OFF.
The saved sample baseline is preserved. Andrew confirmed private on-phone key
entry and DeveloperStuff backup on October 24. No key value was inspected;
the first user-triggered gpt-transcribe result succeeded. Translation model access,
latency/cost measurements and broader speech quality remain unverified.

EN recording -> explicit Stop -> OpenAI gpt-transcribe -> editable English ->
Confirm -> unchanged guarded Insert/Copy. Korean modes are disabled in this
candidate until Phase 3. Audio is bounded in memory to 60 seconds; upload requires
successful microphone release and reader completion. Cancellation, capture limits
and stale results cannot hand off a transcript. The fixed HTTPS request uses
OkHttp 4.12, one-shot multipart, no redirects/replay, bounded response parsing and
call/connect/read/write timeouts. See docs/PHASE2.md for exact behavior.

The private masked key screen uses Keystore AES-GCM and ciphertext in
noBackupFilesDir, verified save/readback, explicit deletion, no key display,
autofill/saved-state opt-outs, and backup/transfer exclusions. Actual OEM transfer
behavior is not tested. Fifty-three unit tests passed, and app/smoke lint had zero
errors (37/3 warnings). Fake save/reopen/delete passed on the phone in 1.123s with
the final smoke helper, and the store was verified empty. Early fixture failures
and exact artifacts are recorded in DEVICE_TESTS.

Missing-key warning test failed at MISSING_KEY_NOTICE_NOT_VISIBLE. Own microphone
proof/capture/commit counts remained zero, draftNONE/cleanupfalse; AppOps showed
no new recording. Source diagnosis: redrawing the bubble before asynchronous
key lookup replaced the clicked host/token. Sol removed only that precheck redraw;
Astra cleared the fix with all continuity/lock/cancellation checks and idle refresh
retained. Rebuild passed all 53 units and lint. Three v3 device runs observed the
warning and passed every no-recording/no-insertion assertion. All three full
methods FAILED later on automated Close cleanup (including the final bounded
own-overlay scroll, OWN_OVERLAY_SCROLL_REFUSED). Stop incidental cleanup retries;
actual menu Close behavior remains unverified. No recording is active and no
draft exists. This is setup readiness evidence, not a full smoke-method pass or
Phase 2 acceptance. Andrew now confirmed key saved and backup done. The non-secret
MindHub checklist records that confirmation. Never rerun fake-key/deletion or
missing-key tests against his live credential. Root verified unlocked/idle,
microphone stopped and SwiftKey selected, then opened the owned single-field
Test message screen. Andrew reported the first deliberate EN sentence was
transcribed correctly and the requested review/Cancel flow passed. Own proof0->1,
REVIEW_CANCELLED, draftNONE, captures/commits0, cleanupfalse; AppOps confirms the
microphone ended after29.097s. That is recording duration, not API latency.
SwiftKey remains selected; no transcript/key/conversation read or screenshot.
No agent-initiated recording/upload, insertion, Copy or Send occurred.

User feedback: tapping EN starts recording, but its status was hard to spot;
he spent time locating the recording indication. He explicitly accepted the test
and deferred more prominent recording feedback/menu improvements to later. Record
that UX requirement for later. The next real speech -> SwiftKey edit -> guarded
Insert test passed: Andrew reported the complete edited text appeared and the
box disappeared. Own REVIEW_COMPLETED, captures0->1/commits0->1, CONFIRMED,
draftNONE/cleanupfalse, latest microphone ended4.368s, SwiftKey selected.
Proof1->3 includes an intervening recording/Stop/review-Cancel trace before the
successful run; its reason was not reported and no insertion happened there.

New measured usability report: Stop is only half visible at the bottom of the
recording panel, although the user could tap it. Fix this narrow reachability
issue before further recording checks. Sol6/xhigh implemented a speech-only
pinned Stop/Cancel row outside scrolling explanatory content; Astra/xhigh cleared
the source. Same host/window dimensions and all microphone, upload, insertion
and key guards remain intact; broader status/menu polish stays deferred.
Controls v4 built/signed, all53 units passed, app lint zero errors/37 warnings.
Installed with replace-in-place, no uninstall/data clearing/key access. Default
debug/release outputs restored OFF, visualfalse/SPEECHfalse. Own service reconnected
idle with proof/captures/commits0, draftNONE/cleanupfalse. Test message is open;
await user report that both buttons are fully visible and direct recording Cancel
closes without review or text changes. No Stop/upload/insertion is requested.
Device layout acceptance remains pending; do not claim clipping fixed yet.

Previous secure980E build Kakao self-chat microphone -> Stop -> English review -> Cancel
PASSED. Andrew reported the sample review closed and the composer cursor/SwiftKey
returned, with no Confirm/Insert/Copy/Paste/Send. Own proof6 -> 7, captures/commits
unchanged4/4, REVIEW_CANCELLED, draftNONE, cleanupfalse; microphone ended5.265s.
No field/chat content or screenshots were read. Clarified that the microphone
runs and audio is discarded; the displayed sentence is fixed, not transcription.

The first remaining synthetic regression, long middle insertion preserving
Korean/emoji, then FAILED in12.853s at SUCCESS_NOT_CONFIRMED. Its exact-text
assertion passed. The installed diagnostic now measured the missing conjunction:
at29ms, full target/host identity, exact expected context/caret and node agreement
all true while one composing span115..121 remained. Later samples at57-67,
207-217 and557-572ms retained exact125-unit context/caret121 and that same span.
Transaction stayed UNCERTAIN with one commit; test Cancel cleared the draft.
Final proof8/captures5/commits5, cleanupfalse, mic ended624ms. No retry.
Astra Ultra reviewed this new evidence and proposed exact-result confirmation
independent of postcommit composing metadata, with every precommit guard intact.
Andrew explicitly APPROVED that prospective change. Sol6/xhigh implemented product/
unit changes and deterministic smoke fixtures; Astra/xhigh cleared both.
Composed exact result may confirm only with full original-target continuity;
composed nonmatch remains immediately uncertain, and no ended transaction can
be upgraded or retried. The reviewed candidate passed the seven checks above;
the old failure remains a historical failure, not retrospectively confirmed.
The new middle pass did not observe composition; the deterministic exact and
natural Korean checks did. Detailed evidence and limits are in DEVICE_TESTS.

The corrected manual SwiftKey flow now PASSED on secure980E4299...: Andrew
edited the English sample, confirmed, tapped the destination and then the
floating Insert button. He saw the edited sentence and dismissal. Own state
confirmed one new capture/commit (2/2 -> 3/3), CONFIRMED, draftNONE, cleanupfalse;
recording proof count4 -> 5 and microphone use ended after5.318s. This is the
first accepted end-to-end manual edit/Insert flow, not general app/composition
acceptance. Andrew's separately authorized shorter flow also PASSED on the same
build: actual SwiftKey edit/Confirm, then floating Insert without retapping the
restored destination. User reported text insertion and dismissal; own proof5 -> 6,
captures/commits3/3 -> 4/4, CONFIRMED, draftNONE, cleanupfalse, mic ended3.415s.
Independent source/evidence review confirms Insert itself creates a fresh
capture, with no recent-tap prerequisite. Conditional instruction: check the
intended field and cursor, tap the field only if needed, then floating Insert.
This proves the empty synthetic restored-focus case, not that every app returns
focus correctly. No code/build/guard change; post-report verification was read-only.

Andrew asked to continue. A debug-only "KoScribe test editor" launcher was added
to the smoke app, independently reviewed, built/linted, installed, and opened.
It launches only the internal synthetic fixture with the protected field omitted;
it forwards no caller data and automates no product action. Andrew completed
SwiftKey editing and Confirm English, but the large ready panel obscured the
destination. He tapped a separate Paste action, then Insert; insertion was refused
for ACTIVE_COMPOSITION before any product commit. The draft expired. Manual
end-to-end acceptance failed; do not attribute this to user error or count a pass.
Sol implemented and Astra reviewed a compact draggable result toolbar with
clearer refusal feedback; all insertion/composition guards stay intact. Its first
secure-build empty Insert regression failed after one commit: exact text appeared,
but postcommit SPAN_COMPOSING caused terminal uncertainty. This differs from the
manual precommit refusal; cause/timing remains unresolved. No retry occurred.

The synthetic visual session finished and secure REVIEW was restored afterward.
Andrew reported successfully dragging the whole compact control by its Move
handle and selecting the top field. Its hint was mistaken for residual text;
two fixture fields also caused confusion. The screenshot after his reply already
showed draft expiry and the large mode menu, so it does not independently prove
the dragged READY geometry. Own state confirmed two recordings, zero captures/
commits, no draft or cleanup pending, and microphone use ended after 5.111s.
Temporary images were removed. No conversation screenshots or real-app insertion;
the earlier incidental incoming-call frame was deleted.
Sol simplified the manual fixture to one clearly labelled field and made
expired/cancelled reviews collapse to the contextual bubble. Independent source
review passed. The simplified launcher and manual Insert flow are now accepted
for this synthetic case; expiry-collapse visual acceptance remains pending.

Astra Ultra's read-only analysis separated precommit safety from postcommit
observation: Android can mark existing text composing without changing text or
caret. The failed trace rejected composing spans before exact expected-context
and complete target checks. Sol implemented and Astra independently reviewed a
fixture-only diagnostic for that conjunction and bounded later samples, without
changing eligibility, confirmation, or terminal uncertainty. Both fresh runs on
secure980E4299... returned NATURAL_CONFIRMED (5.370s/4.638s): exact text/caret,
one commit each, dismissal, stable result. The failure did not reproduce, so no
composition probe ran and no root-cause fix is claimed. The two-run round is
closed. Copy dismissal/new-keyboard-cycle reappearance also passed in5.853s.
The first simplified-screen manual flow reached READY. Andrew moved the control
but looked for Insert in Android's cursor menu, then pasted the earlier synthetic
Copy sample. Own proof count rose to4; captures/commits stayed2/2, so no KoScribe
Insert or Copy occurred in this flow. The retained COPIED status was from the
earlier automated test. Mic ended after4.639s; the draft subsequently expired.
He clarified that he had not expected the Ko bubble to become a different-looking
result box and requested another try. Root reset only the synthetic fixture and
explained its two rows: EN/Move + Cancel, then Insert + Copy & close. The fresh
attempt passed as recorded above. Whether the earlier Insert row was obscured
remains unmeasured. Any completion-policy change still needs a concrete measured
proposal first.

Earlier stale-node diagnosis: after a commit, the input connection already showed the exact
expected text/caret while the same focused accessibility node retained an old
hint/unknown caret. Refreshing that node made the observations agree. Cache
versus read ordering was not isolated. The fix refreshes the same focused node
after reading the connection and rechecks identity, input generation, focus,
window, and device state before the unchanged exact confirmation. No retry,
guard exemption, or rehabilitation of an uncertain transaction was added.

The earlier fixed scrolling footer resolved clipped controls for the long sample.
The current candidate replaces it with compact Move/Cancel and Insert/Copy rows,
omits the title/result preview, and permits dragging only outside a transaction.
Sol 6 Extra High implemented the changes; Astra Extra High independently reviewed
product and tests. Default/release OFF and recording-only RESELECT remain intact.

## Earlier seven-pass checkpoint (2B00BE44)

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
review edits there used our own EditText accessibility action with SwiftKey visible.
Actual SwiftKey editing and confirmed insertion now passed the separate manual
gate on980E4299... . General composition/prediction acceptance remains unproven.

Candidate/default OFF builds, 37 JVM tests (zero failures/errors/skips), lints
(zero errors; app 23/smoke 3 warnings), and APK signatures passed. Final test-only
Cancel scrolling fix was rebuilt/linted, independently reviewed, and passed.
Its assertions require a cleared draft, unchanged capture/commit counts, and
absence of own review controls even when they are offscreen.
Previous secure diagnostic/default builds passed 37 unit tests and lint, with
25 app warnings and2 smoke warnings, zero errors; signatures verified. Default
and release remained OFF/visual false. The newer approved confirmation candidate
passed40 units (zero failures/errors/skips), lint (zero errors; app25/smoke3
warnings), signatures, and all seven scoped phone gates. Default/release are
restored to OFF/visual false; the installed candidate remains secure REVIEW.

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
connected at the latest check. The current speech setup checks finished
with draftNONE and microphone stopped; no test is running. Do not assume a synthetic screen
remains foreground after instrumentation cleanup.
Verify live readiness next session. Fixture cleanup may return
to a real app; never assume the foreground is still synthetic.

Installed product: `app/build/speech-input/SPEECH-controls-v4.apk`,
SHA-256 `524133623674E42B4BE8E5745C92BAD85A649D1C46AD46EF9181441858461F05`.
Successful ASR/edit/Insert baseline remains saved as SPEECH-secure-v3.apk,
SHA-256 `F2079E8AE1181930911E7820AD4A735A56911F1C122FE06CA76DC594E0F811F9`.
Phase 1 rollback: `app/build/composed-confirmation/REVIEW-secure.apk`,
SHA-256 `3AA18446275E46E3712D5232EC8D02DC4AEF0D3C1D88D7CE7CAECE6080DF99DD`.
Earlier seven-pass product remains saved at app/build/confirmation-refresh/REVIEW.apk
with SHA-256 `2B00BE44077579BBD92D3D6A8D7FCED7E7391E17B05F56B5D1652444F4AD4D9E`.
Installed smoke: `app/build/speech-input/smoke-setup-v4.apk` (ignored).
SHA-256: `2782686D4D4BAA5E878CDF1B22950F5EBFA0D3C6098AF4E239A892791B505B0C`.
This adds opt-in credential/missing-key tests, preserving prior scoped methods.
Older smoke/product artifacts remain under their recorded ignored build folders.
Ordinary `app/build/outputs/apk/debug/app-debug.apk` has been rebuilt as OFF with
VISUAL_TEST=false and SPEECH_INPUT=false. Release flags are also disabled.
Do not substitute ordinary output for the saved secure speech artifact.
Prior diagnostic, refresh-only, and earlier smoke APKs are retained in ignored
build folders. Detailed source review hashes are in DEVICE_TESTS.

Last v3 checkpoint: REVIEW_COMPLETED, proof3/captures1/commits1, draftNONE,
CONFIRMED/cleanupfalse; mic ended4.368s. Newly installed controls v4 reset own
counters: NOT_CAPTURED, proof/captures/commits0, draftNONE, confirmationNONE,
cleanupfalse. SwiftKey remains selected. All three real-app scopes are consumed;
fixed unsent drafts are for Andrew to remove manually. Only the static fake
credential marker was saved/deleted during setup. Andrew subsequently saved his
real key privately and confirmed backup; it was not inspected. Real transcription,
review Cancel and edited-text insertion are user-confirmed. No Send/Enter,
conversation inspection, screenshot, or keyboard change occurred. The controls
layout/active-recording Cancel check is pending on the reviewed installed candidate.

## Next work and gates

1. Preserve both manual passes. The shorter instruction is conditional on the
   intended field/caret already being restored. Current-build preservation,
   selection/refusal, uncertainty and geometry coverage remains bounded by the
   recorded tests; do not generalize this empty-field pass to those cases.
   The old composition diagnostic round is complete. The approved fix passed its
   scoped checks; do not retry uncertain commits or reopen that probe round.
2. Preserve all three app passes; their scopes are consumed. Phase 2 code is
   reviewed, built and unit/lint checked. Credential fake test passed; missing-key
   safety assertions passed, with full-method Close cleanup failures recorded.
   Key entry and backup are user-confirmed. Synthetic real speech/review/Cancel
   and speech/edit/Insert passed. First fix the clipped Stop control, then use
   a manual active-recording Cancel check to verify reachable controls and cleanup.
   No API keys in chat or shell; no unattended audio upload, fake-key/deletion
   or missing-key reruns. Astra's remaining Phase 2 checks are active Cancel and one
   controlled network failure followed by a fresh successful recording. Any
   connectivity change should be deliberate and performed by Andrew; keep the
   saved key intact. Broader reliability remains a later phase.
3. For broader daily-use reliability, extend selected/caret/navigation and
   composition coverage, small-height/large-font/rotation/split-screen, and
   lock/service recovery. These remain limits; do not silently turn the full
   matrix into new prerequisites for beginning speech after the app round.
4. Assess direct insertion's benefit against the accepted launcher -> mic ->
   editable English -> 요/반말 -> Copy & close -> manual paste fallback. Do not
   spend indefinitely on integration without measured user benefit.
5. Phase 2 broader device speech/error/cancel acceptance remains pending. First
   actual transcription succeeded. Private key entry and DeveloperStuff backup
   are user-confirmed; preserve that credential. Improve recording-state prominence
   in the later UI phase as Andrew requested.

Earlier RESELECT synthetic gates proved keyboard-based visibility, dragging,
Hide suppression, and saved position after service restart. Earlier Stage 1
proved >3-second unbound recording -> Stop -> readonly review -> Cancel in Kakao
with zero destination captures. Those results apply to their recorded builds;
they are not full current-build real-app acceptance. See DEVICE_TESTS for history.

## Durable choices

- Bubble is the main interface; three visible modes 요 / 반말 / EN.
- Editable English review for every message initially; skip option later.
- Record/review without a destination. Prepare result, then Andrew checks the
  intended field/caret, taps it if needed, and taps Insert; only Insert starts
  a fresh guarded capture.
- Preserve existing text; one commit maximum; ambiguous attempt stays terminal.
  Keep SwiftKey; no replacement keyboard or automatic Send/Enter.
- Dismiss bubble after Copy or confirmed insertion until a new keyboard cycle.
- Personal on-phone key entry with Keystore-backed encryption, no key in Git/APK/
  logs. Translation choice gpt-6-luna/xhigh; OpenAI transcription baseline recorded
  in RESEARCH. Account access and paid quality/latency tests remain unverified.
- Final icon, permissions/battery/reboot UX, signing and distribution remain later.

## Repository and tools

Private repository: https://github.com/drew345/KoScribe . main tracks origin/main.
Reviewed confirmation change: 56a3d39; Kakao pass: 3fb62d4; two-app scope: be6eb94.
Google Messages pass/Instagram handoff is recorded afterward; use git log for its ID.
Inspect staged files and scan for secrets before publishing.

ADB/SDK: C:\AI\Codex\Tools\android-sdk (API 36/build tools 36.0.0).
JDK: C:\AI\Codex\Tools\openjdk21\jdk-21.0.12.1+1.
Gradle wrapper 8.13; cache C:\AI\Codex\Tools\gradle-cache.
Use adb -d, user 0, and the non-suppressing KoScribeTestRunner. Root alone
operates the phone; run exact opt-in methods separately and inspect every result.
