# Current progress

Updated: 2026-10-24 (Asia/Seoul).

## Current state

The earlier API-free REVIEW product passed all seven synthetic acceptance
methods after its stale focused-node fix. A newer compact-controls candidate is
now under evaluation and has a separate postcommit-composition failure described
below. No real-app insertion acceptance, speech recognition, translation, or API
integration is claimed. Phase 1 remains open.

The corrected manual SwiftKey flow now PASSED on secure980E4299...: Andrew
edited the English sample, confirmed, tapped the destination and then the
floating Insert button. He saw the edited sentence and dismissal. Own state
confirmed one new capture/commit (2/2 -> 3/3), CONFIRMED, draftNONE, cleanupfalse;
recording proof count4 -> 5 and microphone use ended after5.318s. This is the
first accepted end-to-end manual edit/Insert flow, not general app/composition
acceptance. Andrew suggested omitting the destination tap when the intended
field is already focused. Independent review confirms no tap token is required
by the implementation: Insert starts a fresh capture. That restored-focus
variant is not yet device-tested; no code change is needed to evaluate it.
Andrew has now authorized that test. The connected, unlocked A35 retains SwiftKey
and secure980E4299... . Root confirmed no active draft, reset only the synthetic
fixture, and reopened its one empty field. Andrew is performing the same manual
edit flow but must not retap the field after Confirm English; he should Insert
once only if its intended cursor is already restored. Await his report without
phone actions. Baseline proof5, captures3/commits3, CONFIRMED, draftNONE,
cleanupfalse. No source/build/guard change.

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
Current secure diagnostic/default builds passed 37 unit tests and lint, with
25 app warnings and2 smoke warnings, zero errors; signatures verified. Default
and release remain OFF/visual false. The earlier compact failure remains open.

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
connected and unlocked at the latest check. The prior manual test finished with
confirmed insertion; the fresh no-extra-tap manual test is now awaiting Andrew's
report. Do not infer active draft/microphone state from the earlier baseline.
Verify live readiness next session. Fixture cleanup may return
to a real app; never assume the foreground is still synthetic.

Installed product: `app/build/composition-probe/REVIEW-secure.apk`,
SHA-256 `980E4299E1B73160E82D874139A7CE04FBA46603DC32A184E0FBF50D5D627D0D`.
Earlier seven-pass product remains saved at app/build/confirmation-refresh/REVIEW.apk
with SHA-256 `2B00BE44077579BBD92D3D6A8D7FCED7E7391E17B05F56B5D1652444F4AD4D9E`.
Installed smoke: `app/build/composition-probe/smoke.apk` (ignored).
SHA-256: `9D3F9563C8A3A21998EC1DD6A067921FD65868CE9F140C26F1BC2CC9CF8F361D`.
This includes the single-field manual mode and extended diagnostic parser.
Older smoke/product artifacts remain under their recorded ignored build folders.
Ordinary `app/build/outputs/apk/debug/app-debug.apk` has been rebuilt as OFF with
VISUAL_TEST=false. Do not substitute it for the secure REVIEW artifact.
Prior diagnostic, refresh-only, and earlier smoke APKs are retained in ignored
build folders. Detailed source review hashes are in DEVICE_TESTS.

Latest checkpoint after the manual pass: REVIEW_COMPLETED, proof count5,
draftNONE, destination captures3, commits3, confirmationCONFIRMED,
cleanupfalse. Exact-app AppOps confirmed mic ended after5.318s. All insertions
used only the synthetic fixture; earlier Copy replaced the clipboard with the
disclosed fixed sample.
No Send/Enter, real conversation access, screenshots, retained audio, credentials,
paid calls, permission changes, or keyboard changes occurred in this diagnostic round.

## Next work and gates

1. Await Andrew's authorized no-extra-tap test report, then check own counters
   against proof5/captures3/commits3. Stop if restored cursor is absent or Insert
   refuses; no recapture/retry, cached destination or guard exemption. Preserve
   the prior completed tapped-field manual pass regardless of this result.
   The two automated composition diagnostic attempts are complete; intermittent
   composition failure remains unresolved. Do not retry uncertain commits.
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
Latest published source checkpoint: 8de23d6 (manual fixture/composition diagnostic).
Inspect staged files and scan for secrets before publishing.

ADB/SDK: C:\AI\Codex\Tools\android-sdk (API 36/build tools 36.0.0).
JDK: C:\AI\Codex\Tools\openjdk21\jdk-21.0.12.1+1.
Gradle wrapper 8.13; cache C:\AI\Codex\Tools\gradle-cache.
Use adb -d, user 0, and the non-suppressing KoScribeTestRunner. Root alone
operates the phone; run exact opt-in methods separately and inspect every result.
