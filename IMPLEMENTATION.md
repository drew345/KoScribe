# Implementation plan

Planning baseline: 2026-10-23. Andrew authorized Phase 1 implementation on
2026-10-23. Later phases remain gated on the first device test results.

## Confirmed product choices

Use the three-mode bubble flow in `README.md`. Every message initially gets an
editable English transcript review after Stop. Translate only confirmed English;
EN skips translation. Andrew subsequently selected a separate destination step:
after review/result preparation, check the intended message field/caret, tap it
if necessary, and then tap Insert.
That explicit action begins a fresh guarded insertion transaction. Recording
and review do not bind a destination. A skip-English-review option
is deferred until later. Keep SwiftKey and never send. Test on the A35 with
Wispr Flow still installed.

## Proposed architecture

2026-10-24 priority: first fix idle bubble visibility, dragging/persistence, and
completion dismissal before adding editable review or more insertion features.
An idle visibility observer may use minimal keyboard/window/editor metadata;
it must not read field text, create a destination snapshot, or confer insertion
eligibility. Active recording/review owns its controls until terminal cleanup.
Position changes must not violate that ownership or move a captured host.
Test this on synthetic screens before real-app confirmation.

After the contextual-bubble gate passed, Andrew authorized editable English
review and completion dismissal on 2026-10-24. The next candidate is a separate
debug REVIEW arm; keep tested RESELECT and default/release OFF intact. Stop must
prove microphone cleanup before handing a transient content ID to a dedicated
nonexported review Activity. No text goes into saved hierarchy state, Intent
extras, storage, or logs. The English editor uses SwiftKey. EN accepts edits;
Korean samples still require the exact fixed source until real translation exists.

Confirm ends review and leaves a service-owned result with nonfocusable Insert,
Copy & close, and Cancel controls. It captures no destination until explicit
Insert. Copy marks its new clip sensitive; a failed write retains the result.
Fresh insertion must retain identity/context/caret/composition and continuity
checks, reserve its one attempt before IPC, and verify the exact resulting local
context and collapsed caret in the same node/root/input session. Widen the
post-insertion context request by the result's UTF-16 length so long results
cannot appear successful from a truncated fragment. Unsupported or ambiguous
confirmation is terminal for insertion of that draft; only deliberate recovery
or cancellation remains. Copy or confirmed insertion clears transient content
and suppresses idle redisplay until a verified new keyboard cycle. Seven synthetic
REVIEW device gates passed on Oct24; a later compact build also passed one manual
SwiftKey edit/Insert flow in the synthetic editor. Real-app and broader composition
acceptance remain pending. After manual use exposed destination obstruction, the
candidate replaced the large result preview with compact mode-labelled draggable
controls: Insert, Copy & close, and Cancel. Moving/reflow is prohibited while a
destination transaction exists. A later explicit Insert retains all checks.

Approved Oct24 after measured evidence: separate precommit eligibility from
postcommit observation validity. Before commit, reject active composition as
before. After one attempt, an exact result in the continuously verified original
destination/session may confirm even with composing metadata. Preserve the actual
composing flag and styled reads; never force it false or issue a composition/
keyboard command. A composed nonmatch is immediately terminally uncertain;
noncomposing observations retain their existing bounded unchanged-text behavior.
All focus/navigation/input/host/device/refresh invalidations and no-retry rules
remain. Confirmed means the expected insertion was observed, not that later
keyboard edits are impossible. Implement/review and prove deterministic composed
success and mismatch cases before considering another real-app insertion trial.

Andrew subsequently authorized and passed a separate manual synthetic test
skipping the extra destination tap when that intended cursor was already
restored. Independent review confirmed no recent-field-tap token is required:
Insert captures and checks the current focused destination afresh. The shorter
instruction is conditional on that intended field/caret, not a guarantee that
every app restores focus. No destination is inherited from recording/review.

The accepted fallback is a launcher Activity with mic -> editable English ->
요/반말 translation -> Copy & close, followed by manual paste. Design content
review/translation independently of destination integration so this remains a
practical route. Direct insertion still needs measured advantage and reliability;
do not extend failed integration diagnostics indefinitely.

Native Kotlin Android app with a small control/settings Activity, an
AccessibilityService for a bubble and tightly scoped editor access, and
explicit recording sessions. Phase 1 uses Android Views for the prototype
screens and accessibility overlay. The final UI follows feasibility results.

Prototype Android 13+ accessibility input connections first. They offer an
insertion API without selecting KoScribe as a keyboard. This is not proof of
compatibility with every editor. Prototype an accessibility overlay to avoid
unnecessary draw-over-other-apps access; retain a conventional application
overlay as an alternative only if needed and disclosed.

The original Phase 1 Activity remains in the default OFF build. Device tests
now demonstrate service-owned microphone capture in the existing expanded
accessibility bubble, including unsilenced own-recorder reads and direct Cancel.
The original captured destination still invalidates during longer recordings.
RESELECT Stage 1 now separates recording from destination capture and passed
one >3-second recording -> Stop -> readonly review -> Cancel device gate.
Editable review and fresh guarded insertion are now implemented in REVIEW.
Copy and confirmed-insertion dismissal passed their synthetic device gates.
Measured stale focused-node metadata caused the earlier postcommit mismatch.
The REVIEW fix refreshes that same node after the connection read, rechecks
transaction/window/root/node/input continuity, then applies the unchanged exact
context/caret confirmation. A failed refresh or changed destination is terminal;
no retry or timing exemption was added. See DEVICE_TESTS for measured limits.
Never keep the microphone recording simply to keep a bubble alive.

Andrew selected OpenAI cloud speech recognition for the first version and
accepts some extra processing time. Start with the documented `gpt-transcribe`
recorded-audio workflow; verify account access before paid testing. The path
is audio -> English transcription -> user review/edit -> confirmed text
translation. EN skips Korean translation. Luna is a text model, so it is not
the audio recognizer. Provide visible processing/cancel states and bounded
timeouts; no numeric latency promise has been agreed.

Android on-device speech recognition remains a future alternative if needed;
its availability and quality would need device-specific testing. It is not a
blocking comparison for the first version.

Andrew selected `gpt-6-luna` with `reasoning.effort: "xhigh"` for translation.
Verify account access and benchmark it; discuss any change to that selection.
Translation is text-only, with an explicit mode and instructions to preserve
meaning and produce just the message. Treat dictated instructions as content,
not commands to control the app. Keep prior chats and field contents local.
English edits invalidate earlier derived results. Do not translate provisional
or unconfirmed English in the initial review flow.

The revised recording/review session has no destination eligibility to preserve.
It owns its microphone, visible controls, transient reviewed content, cancellation,
and timeout. Window changes in an unrelated composer cannot invalidate a
destination transaction that has not begun. Lock, lost own UI/service ownership,
microphone failure, and explicit Cancel still terminate capture safely.

After result preparation, make the bubble nonfocusable so Andrew can choose
the composer. His later explicit Insert action starts a new short transaction
with all existing editor checks; no Activity/focus transition may occur between
that capture and commit. KoScribe's own review editor is never an insertion
destination. A failed/invalidated destination attempt ends permanently; keeping
the reviewed result for a new explicit attempt does not carry eligibility forward.
Never retry an ambiguous commit. Manual Copy is a separate explicit recovery
action, not an automatic fallback.

Implement this in API-free stages. Independent recording -> Stop -> readonly
sample review -> Cancel passed beyond the observed 2.6-second window event.
The separate REVIEW arm now supplies editable review, Copy, and synthetic-only
fresh insertion testing. RESELECT continues to block insertion/Copy during its
real-app recording tests. Any later real-app destination-only check must be labeled
`Check destination (no insert)`. OFF/release defaults
remain unchanged until the new path is reviewed and tested. No real-app insertion
is authorized by selecting this workflow alone.

REVIEW implements a dedicated own Activity after microphone Stop, with an
independent unbound draft owner. It finishes before destination selection.
The bubble remains the recording/Insert entry. Automated editable-content/Copy
handling passed with SwiftKey visible. Two manual synthetic EN edit/Insert
variants passed with actual SwiftKey keys: tapping the destination first and
using its already restored focus. Broader composition remains unaccepted.
A failed postcommit observation stays terminally uncertain; diagnosing
node/InputConnection timing must not resurrect or retry that transaction.

If an editable overlay is chosen, clear both NOT_FOCUSABLE and ALT_FOCUSABLE_IM,
wait for actual own-window/EditText focus before requesting SwiftKey, and later
relinquish that input before destination selection. Android documents these
[keyboard focus requirements](https://developer.android.com/develop/ui/views/touch-and-input/keyboard-input/visibility).
Never use delayed global IME toggles. For target API 36,
[`hideSoftInputFromWindow`](https://developer.android.com/reference/android/view/inputmethod/InputMethodManager#hideSoftInputFromWindow(android.os.IBinder,%20int))
returning true is not evidence that the keyboard hid; inspect own window insets
if that state is required. Any editable-overlay alternative still needs its
own SwiftKey editing/return device gate; current manual passes use an Activity.

## Credential approach selected by Andrew

Personal bring-your-own-key app; Andrew enters the key on the device. No key
is bundled, committed, or supplied through build constants. This direction
was explicitly selected on 2026-10-23; the details below form the proposed
security design to carry into implementation review.

- Encrypt the credential in private app storage using an AES-GCM key generated
  and retained in Android Keystore; use hardware backing when supported.
- Exclude the credential and sensitive content from cloud backup and device
  transfer. Verify the applicable Android/Samsung backup behavior; do not rely
  only on a single manifest flag.
- Use HTTPS with normal certificate/hostname verification and an allowlisted
  API destination. No arbitrary configurable endpoint receiving the secret.
- Provide masked credential entry, replace/delete, and a sanitized connection
  test. No HTTP-body/header logging or credential-bearing crash telemetry.
- The API credential is temporarily accessible to the running app when used.
  Keystore protects storage; it cannot guarantee protection on a compromised
  device. This is a personal-use compromise relative to a private backend.
- Prefer a dedicated OpenAI project/key, limited API permissions, budget alerts,
  and a verified hard spending limit where supported. A soft budget alert is
  not a cap. Verify actual account model access before paid testing. Do not
  silently borrow a key from another project.
- No message/audio history by default. Remove temporary audio after processing
  or cancellation. Provider-side retention must be described accurately;
  disabling app history is not equivalent to zero provider retention.

## Safe insertion contract

1. Bind dictated/reviewed content to an unexpired content-session identifier and
   mode. Bind a separate insertion transaction to a fresh editor capture only
   after explicit destination selection. Cancelled/superseded/restarted
   transactions cannot insert; they cannot be rearmed or silently replaced.
2. Exclude password/PIN and unsupported nontext editors before reading context.
   Before committing, reacquire and verify the current app/editor, focus,
   unlocked state, caret, and necessary surrounding context. Field identifiers
   alone are insufficient: cancel on navigation/editor-session discontinuity,
   including switching conversations within one app and returning later.
3. Abort automatic insertion for selected text, unknown selection, changed
   editor/text, or incomplete observations. Keep the generated message.
4. Permit at most one commit per session, through the accessibility input
   connection at a verified collapsed caret. Before commit, refuse active composing
   ranges; do not treat missing styled-text spans or an absent selection
   callback as proof that composition is clear. For the Phase 1 probe, unknown
   composition alone is not a rejection: Android's accessibility commit path
   finishes composition before committing, preserving the existing composing
   text for standard editors. Keep every live caret/context/identity check.
   Test synthetic local text first, then SwiftKey/OEM/custom editor behavior
   before accepting external-app compatibility. Do not issue a separate
   finish-composition or keyboard action.
5. Observe success where supported. An uncertain outcome must not trigger a
   retry that might duplicate text. Cross-app checks and insertion are not an
   atomic transaction; document remaining compatibility limits honestly.
   The approved postcommit rule accepts composing metadata only with exact
   expected context/offset/caret and all original-destination continuity checks.
   A composed nonmatch terminates immediately; no ended uncertain transaction
   may be upgraded by later diagnostic observations.
6. Offer deliberate Copy/manual paste for unsupported fields. Copy replaces
   the system clipboard; do not promise transparent restoration. Mark the
   copied message sensitive where supported.
7. No ACTION_SET_TEXT shortcuts, coordinate-driven insertion, Send taps,
   Enter key injection, or editor-action invocation.

## Phases and acceptance gates

Oct24 decision: Phase1 bounded feasibility gate met after current sample flows
passed in all three named apps, plus synthetic preservation/refusal/uncertainty,
manual SwiftKey editing and documented focus-termination/unit/source evidence.
This does not claim full daily-use or every navigation/geometry state. Phase2
now proceeds as a separate debug speech flag, preserving REVIEW samples and
release/default disabled. Its first actual speech path is EN only; Korean modes
must not return canned output for real speech before Phase3. Record bounded audio
in memory, upload only after explicit successful Stop, cancel stale requests,
and review the real transcript before existing guarded insertion. Credential
architecture remains the previously selected personal on-phone Keystore approach.

Phase 2 implementation checkpoint: the EN speech and credential candidate is
implemented under the explicit debug flag and independently reviewed. Tests cover
bounded audio, request cancellation/stale outcomes, parsing, and no automatic
network replay. Phone setup checks use only a fake credential and the owned
synthetic editor. Real key entry and deliberate paid transcription follow these
checks; accuracy, edit preservation through real ASR, and error recovery still
gate Phase 3. See [Phase 2](docs/PHASE2.md) and PROGRESS for measured status.

| Phase | Scope | Gate before advancing |
| --- | --- | --- |
| 0: Planning | Settle UX, credentials, models, speech provider, installation and latency goals. | Andrew approves implementation plan. |
| 1: Device feasibility | Minimal bubble, explicit microphone start/stop, hardcoded sample insertion; no API needed. | On A35: SwiftKey stays selected, original text preserved, no send, focus changes fail safely; KakaoTalk/Instagram/SMS tested. |
| 2: Speech and credentials | Keystore-backed user key, OpenAI recording/transcription, cancel, editable English review and EN insertion. | Accurate transcripts; edits are preserved; explicit fresh destination safely verified after review; mic stops on every exit; errors recover; no secret/audio logs. |
| 3: Korean translation | Confirmed English -> explicit 해요체/반말 translation -> safe insertion. | User-reviewed corpus; correct meaning/tone; measured Stop-to-transcript and confirmation-to-insertion latency, plus cost. |
| 4: Daily-use reliability | Bubble placement, settings/status, coexistence with Wispr, battery/restart behavior, signed personal installation. | Real-app acceptance matrix, release APK/Git secret scan, independent Astra review, Andrew's daily-use test. |

Coding subagents: `gpt-6-sol` at `xhigh`. Independent checking after each
implementation phase: `gpt-6-astra` at `xhigh`. Main agent plans, integrates
evidence, and oversees. Do not advance a phase without its test results.

## Minimum meaningful test matrix

Existing English/Korean/emoji; empty/beginning/middle/end caret; selected text;
active SwiftKey composition; typing during processing; switching app/field;
same-app conversation switch and switch-back; rotation/lock; editor disappears;
password/PIN fields; mic in use; both bubbles present; unsupported editors;
network loss; cancel then start another recording; repeated Stop; out-of-order
responses; ambiguous commit; service restart/killed or permissions revoked.
Also: edit transcript with SwiftKey, explicitly choose the destination after
review, switch conversation during review versus after capture, cancel review,
repeat confirmation, and verify only final confirmed English is processed.
Navigation during unbound recording/review must not create insertion eligibility;
navigation after a destination capture still invalidates that transaction.
No test sends a real message automatically.

Translation corpus should include requests, apologies, scheduling, negation,
questions, names/titles, numbers, mixed Korean names in English speech, and
multi-sentence messages in both modes. Quality and latency determine the final
translation model/effort; a model name alone is not acceptance evidence.
