# Planning research

Checked 2026-10-23. Documentation findings and the explicitly labeled installed
permission audit below are separate evidence. Recheck dynamic API/platform facts
when implementation begins.

## Android integration

Android 13+ exposes an accessibility input connection capable of committing
text without creating/selecting a replacement keyboard. Selected text can
still be replaced, so selection/focus validation is essential. AOSP finishes
existing composition before an accessibility commit; actual SwiftKey behavior
must be tested. No universal insertion guarantee follows from these APIs.

Phase 1 review refinement: a fresh accessibility `onUpdateSelection` callback
is not guaranteed after each editor start/return, and composing spans can be
omitted by IPC serialization. Absence of either is not evidence of a clear
composition state. For the controlled probe, unknown composition may proceed
with verified live caret/context/identity, relying on the accessibility
commit implementation's finish-composition-before-commit behavior for standard
editors. Reported active composition still refuses. Astra reviewed this as a
documented policy choice, with synthetic local testing and OEM/custom editor
verification still required; it is not a universal preservation guarantee.

- [Accessibility InputMethod](https://developer.android.com/reference/android/accessibilityservice/InputMethod)
- [AccessibilityInputConnection](https://developer.android.com/reference/android/accessibilityservice/InputMethod.AccessibilityInputConnection)
- [EditorInfo: field identifiers are not sufficient target identity](https://developer.android.com/reference/android/view/inputmethod/EditorInfo)
- [InputType: identify password variants before context inspection](https://developer.android.com/reference/android/text/InputType)
- [AOSP input connection implementation](https://android.googlesource.com/platform/frameworks/base/+/eabfd2281dfa777d8b7e10619f7e014cc56d3168/core/java/android/view/inputmethod/RemoteInputConnectionImpl.java)
- [Accessibility actions: ACTION_SET_TEXT replaces field contents](https://developer.android.com/reference/android/view/accessibility/AccessibilityNodeInfo.AccessibilityAction)

The implemented nonfocusable TYPE_ACCESSIBILITY_OVERLAY uses the connected
accessibility service. It has worked on the A35 without a separate
SYSTEM_ALERT_WINDOW grant. A conventional TYPE_APPLICATION_OVERLAY requires
that special access; Wispr's requirement does not transfer to this architecture.

- [Window types and flags](https://developer.android.com/reference/android/view/WindowManager.LayoutParams)
- [Overlay permission](https://developer.android.com/reference/android/Manifest.permission#SYSTEM_ALERT_WINDOW)

Recording requires microphone permission. A microphone foreground service
also has declared type/permission requirements and a notification. Background
service-start exceptions and microphone while-in-use eligibility are separate
checks; visible overlay alone is not proof a microphone start is permitted.
The A35 prototype uses a visible recording Activity; microphone start and
Stop/Cancel cleanup have been measured. It does not use a foreground service.
Notification permission affects notification drawer visibility, not simply
whether a foreground service can start.

- [Foreground service types](https://developer.android.com/develop/background-work/services/fgs/service-types)
- [Background start and while-in-use restrictions](https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start)
- [Notification permission](https://developer.android.com/develop/ui/compose/notifications/notification-permission)
- [SpeechRecognizer, including on-device availability](https://developer.android.com/reference/android/speech/SpeechRecognizer)

Fail closed on incomplete editor data or changed focus. Keep Copy/manual paste
as an explicit fallback; it replaces the clipboard and should mark content
sensitive. A personal sideload may require allowing restricted settings for
accessibility. A future Play release has separate accessibility disclosure and
review obligations; approval is not assumed.

- [AccessibilityService and asynchronous/stale observations](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService)
- [Secure clipboard handling](https://developer.android.com/privacy-and-security/risks/secure-clipboard-handling)
- [Restricted settings](https://support.google.com/android/answer/12623953?hl=en)
- [Play accessibility policy](https://support.google.com/googleplay/android-developer/answer/10964491?hl=en)

## Competitor lessons

Wispr Flow documents using its bubble with the existing keyboard. Setup asks
for microphone, display-over-other-apps, and accessibility access, with
notifications/battery settings for reliability. Its Play disclosure describes
accessibility input connections. Documentation supports tap or hold recording;
custom fields may not work. No Android English-to-Korean speech-level selector
was found in the reviewed documentation.

- [Wispr Android](https://wisprflow.ai/android)
- [Android setup](https://docs.wisprflow.ai/articles/2809924024-Android-Download-%26-Installation-Guide)
- [Play listing](https://play.google.com/store/apps/details?id=com.wispr.flowapp)
- [Transforms platform support](https://docs.wisprflow.ai/articles/8068950331-how-to-use-transforms-beta)

DictoKey (official spelling) offers an independent bubble and an optional
keyboard. Its site documents tap for dictation, long press for translation,
Korean translation, and generic formal/casual rewriting. No documented
해요체/반말 selector was found. Andrew's experience of low-form output is user
evidence, not a proven limitation across all versions. Its older privacy
policy should not be treated as a complete current bubble permission list.

- [DictoKey product](https://dictokey.com/)
- [Accessibility explanation](https://dictokey.com/accessibility/)
- [Play listing](https://play.google.com/store/apps/details?id=com.dictokey.app)
- [Privacy policy](https://quang101182.github.io/App/dictokey/privacy.html)

Takeaways: retain the keyboard, expose permission/connection status in the app,
show speech level explicitly, and test unsupported editors. Do not copy
competitor privacy claims; document KoScribe's actual data path.

## Permission and settings audit (2026-10-23)

Wispr's current official [requirements](https://docs.wisprflow.ai/articles/6344532666-android-system-requirements)
make microphone, display-over-apps, and accessibility mandatory. Notifications
are optional and permit its own alerts, not reading other apps' notifications.
Battery exemption is recommended but skippable. Its [setup guide](https://docs.wisprflow.ai/articles/8858845757-setup-wispr-flow-on-android-android-settings)
also specifies main accessibility service on, shortcut off, and a separate
data-sharing choice. On Samsung it recommends Never sleeping apps, per-app
Battery Unrestricted, and disabling the global Put unused apps to sleep switch.
Those are vendor recommendations, not three universal Android requirements.

DictoKey's [accessibility documentation](https://dictokey.com/accessibility/)
describes an independent bubble and optional keyboard; enabling its keyboard is
not a requirement for KoScribe or for retaining SwiftKey. Its [product page](https://dictokey.com/)
explicitly makes notification shortcuts optional. Its older March privacy
policy lists microphone/network/foreground-service/vibration permissions but
is not a complete inventory of the current bubble. The installed metadata
below fills that evidence gap without claiming every declaration is mandatory.
DictoKey also documents Advanced Protection as a potential bubble limitation;
this audit did not inspect or change that protection.

Read-only ADB inspection of the three installed packages in main user 0:

| Setting | KoScribe Phase 1 | Wispr Flow 2.8.0 | DictoKey 2.73.0 |
| --- | --- | --- | --- |
| Microphone runtime permission | Granted | Granted | Granted |
| Main accessibility service | Enabled | Enabled | Not in enabled list |
| Display over other apps | Not declared; not granted; not needed by this bubble | Allowed | Allowed |
| Post notifications | Not declared; not enabled | Granted | Granted |
| Android Doze battery exemption | Not listed | Listed | Listed |
| `RUN_ANY_IN_BACKGROUND` AppOp | Default allow | Allow | Allow |
| Samsung Never sleeping/Deep sleeping lists | Not inspected | Not inspected | Not inspected |

SwiftKey remained selected. No setting, keyboard, app screen, or draft was
changed by this audit. AppOps' historical microphone timestamps are not proof
of current recording; KoScribe's operation was not running. An Android Doze
exemption does not by itself establish the separate Samsung sleep-list state.

KoScribe currently declares only RECORD_AUDIO as a uses-permission. Its service
declaration is protected by BIND_ACCESSIBILITY_SERVICE; this is not an extra
user runtime prompt. The visible Activity owns capture. INTERNET will be a
manifest declaration for the later API phase, not a permission dialog. A future
microphone foreground service would require the matching service declarations
and a notification; [Android does not require notification permission merely
to start that service](https://developer.android.com/develop/ui/compose/notifications/notification-permission).
Neither feature is implemented in Phase 1.

Remaining setup work: inspect Samsung's per-app battery and sleeping lists,
then test idle/background survival and recovery before choosing an exemption.
[Samsung documents per-app Never sleeping exceptions](https://developer.samsung.com/mobile/app-management.html);
prefer evaluating that focused control before a global sleep-policy change.
Also test permission loss/global microphone blocking and reboot recovery.
No background-reliability claim follows from the short insertion tests.

Conditional installation/setup controls are separate: [Allow restricted
settings](https://support.google.com/android/answer/12623953?hl=en) can be needed
to enable a sideloaded accessibility service. It is not blocking the current
enabled installation. Installing a future APK through a file manager may need
that installer's unknown-app-source permission; current USB installation worked.
USB debugging is a development connection, not a normal KoScribe permission.
No SMS/contact/notification-listener access or replacement keyboard is required
by the implemented product. The audit found no basis to request secure-settings
write access or to disable Samsung Auto Blocker.

## Nonfocusable recording startup (2026-10-23)

The controlled startup experiment implicated the Activity-launch transition;
it did not establish that the target content event was harmless. The next
candidate keeps the original editor focused using a nonfocusable accessibility
overlay. Android explicitly documents that even a full-screen touchable
[`TYPE_ACCESSIBILITY_OVERLAY`](https://developer.android.com/reference/android/view/WindowManager.LayoutParams#TYPE_ACCESSIBILITY_OVERLAY)
preserves accessibility introspection of covered windows.
[`FLAG_NOT_FOCUSABLE`](https://developer.android.com/reference/android/view/WindowManager.LayoutParams#FLAG_NOT_FOCUSABLE)
leaves input focus with the underlying window. A compact card on a transparent
touch-consuming scrim can therefore retain SwiftKey while outside taps cancel.
Actual Samsung event behavior still requires measurement. Editable review is
a separate focus transition and is not proved by a capture-only startup probe.

The first full-screen candidate refused on `WINDOWS_CHANGE_CHILDREN` (512).
Android defines this as a child-window relationship change, not a view-content
change; its provenance was not established. The next candidate reuses the
already-expanded bubble window and changes only its own controls after capture.
It retains all structural/content invalidations. Avoiding a deliberate window
replacement is an architectural hypothesis, not an exception for flag 512.

Root/source retrieval now uses `getRoot(0)` and `getSource(0)` where applicable:
Android 16's default methods request neighboring-node prefetch, while explicit
zero flags preserve the focused-only inspection boundary. These substitutions
do not change identity comparisons or refusal policy. See AOSP
[`AccessibilityWindowInfo`](https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android16-release/core/java/android/view/accessibility/AccessibilityWindowInfo.java)
and [`AccessibilityRecord`](https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android16-release/core/java/android/view/accessibility/AccessibilityRecord.java).

Direct microphone capture from the already-connected accessibility service has
an Android 16 source basis without adding an app-owned foreground service:
[`AccessibilityServiceConnection.bindLocked`](https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android16-release/services/accessibility/java/com/android/server/accessibility/AccessibilityServiceConnection.java)
binds with `BIND_INCLUDE_CAPABILITIES` and foreground-while-awake importance.
The [public binding contract](https://developer.android.com/reference/android/content/Context#BIND_INCLUDE_CAPABILITIES)
includes while-in-use microphone access for a bound app with the required
permission. Android's
[`OomAdjuster`](https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android16-release/services/core/java/com/android/server/am/OomAdjuster.java)
propagates these capabilities from the persistent system client;
[`AppOpsUidStateTrackerImpl`](https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android16-release/services/core/java/com/android/server/appop/AppOpsUidStateTrackerImpl.java)
uses the foreground-microphone capability when evaluating RECORD_AUDIO.
This is a source-based eligibility inference, not a claim about measured A35
audio, and does not rest on overlay visibility alone.

The candidate must [register its recording callback before capture](https://developer.android.com/media/platform/sharing-audio-input#configuration-changes)
and inspect only its own
[`AudioRecord.activeRecordingConfiguration`](https://developer.android.com/reference/android/media/AudioRecord#getActiveRecordingConfiguration()),
not global recording configurations. Require successful reads, recording state,
and a non-null configuration with
[`isClientSilenced == false`](https://developer.android.com/reference/android/media/AudioRecordingConfiguration#isClientSilenced()).
Unknown/null is not success. These checks establish limited capture eligibility,
not physical audibility, speech-recognition quality, or transcription success.
The candidate must discard captured data and zero its application-owned PCM
buffers. No new permission is needed for this
candidate; a future app-owned microphone foreground service would require its
own [type, permission, notification, and startup assessment](https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start).

## Contextual bubble lifecycle (2026-10-24)

Andrew requires an idle bubble only during editing, draggable with a saved
position, and dismissed after Copy/successful insertion. The current RESELECT
prototype has neither Copy nor insertion; an explicit Hide control can test
the dismissal policy without claiming those later actions work.

The visibility heuristic is independent of insertion eligibility. It combines
live input-started/type/package metadata, a unique focused application window,
and a nonempty input-method window on the default display. Only the focused
root's package may be cross-checked; no focused node, text, selection, source,
tree traversal, or InputConnection is needed. Unknown observations hide idle
controls but must not count as a confirmed IME hide/show cycle for rearming.
An active recording/review owner retains its host regardless of IME transitions.

Primary evidence reviewed by Astra Ultra:

- Android 16 [accessibility InputMethod source](https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android16-release/core/java/android/accessibilityservice/InputMethod.java)
  sets input-started/editor metadata before onStartInput, but calls onFinishInput
  before clearing it. Clear idle eligibility immediately on finish, then post a
  refresh. Keep the existing no-super finish rule and no selection processing.
- [AccessibilityService.getWindows](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService#getWindows())
  reports interactive default-display windows; an empty result may be unavailable.
  [Input-method windows](https://developer.android.com/reference/android/view/accessibility/AccessibilityWindowInfo#TYPE_INPUT_METHOD)
  include keyboard/suggestion UI. Their presence is a visibility heuristic,
  not proof that a field is a safe insertion destination.
- [WindowMetrics bounds](https://developer.android.com/reference/android/view/WindowMetrics#getBounds())
  include system bars, while [overlay fit-insets behavior](https://developer.android.com/reference/android/view/WindowManager.LayoutParams#setFitInsetsTypes(int))
  affects the layout frame. Do not double-apply insets. Comparing own-view screen
  position with layout coordinates is a proposed conversion that needs device
  verification after geometry changes.
- [Touch slop](https://developer.android.com/reference/android/view/ViewConfiguration#getScaledTouchSlop())
  separates tap from drag. Persist only a completed user drag; clamping for IME,
  rotation, or expanded controls must not overwrite the saved anchor.
- Accessibility [commitText](https://developer.android.com/reference/android/accessibilityservice/InputMethod.AccessibilityInputConnection#commitText(java.lang.CharSequence,%20int,%20android.view.inputmethod.TextAttribute))
  returns void. The old flow's commit-request return is not insertion success
  confirmation and must not be relabeled as such for completion dismissal.

## Editable review and completion evidence (2026-10-24)

The REVIEW design separates an editable own Activity from the later destination
transaction. Primary Android references confirm that accessibility
[commitText](https://developer.android.com/reference/android/accessibilityservice/InputMethod.AccessibilityInputConnection#commitText(java.lang.CharSequence,%20int,%20android.view.inputmethod.TextAttribute))
returns void and advances the caret using Java-character units. It can trigger
selection updates; the editor may alter supplied text.
[getSurroundingText](https://developer.android.com/reference/android/accessibilityservice/InputMethod.AccessibilityInputConnection#getSurroundingText(int,%20int,%20int))
is a read-only IPC that can return null or shorter content. Therefore the
implementation must treat exact same-destination context/caret matching as a
separate success observation, widen the request by the inserted UTF-16 length,
and keep an unconfirmed attempt terminal. This confirmation policy is our
conservative design inference, not an Android atomic-insertion guarantee.

Android's [clipboard guidance](https://developer.android.com/develop/ui/views/touch-and-input/copy-paste)
uses setPrimaryClip and recommends marking sensitive content with
ClipDescription.EXTRA_IS_SENSITIVE to suppress the system's content preview.
KoScribe copies only after an explicit action, never reads or restores the prior
clipboard, and dismisses its result only after the write returns without an
exception. Synthetic Copy and dismissal passed on Oct24; see DEVICE_TESTS.

## Models and API separation

The local Codex model catalog and the session's supported-agent metadata list
`gpt-6-sol` and `gpt-6-astra` with `xhigh`; Astra also offers `ultra` in Codex.
Other available agent choices include `gpt-6.1-sol`, `gpt-6-luna`, and the
GPT-5.6 Sol/Terra/Luna models. Requested implementation remains GPT-6 Sol,
not an automatic upgrade to GPT-6.1 Sol.

The official API catalog verifies GPT-6 Sol, Astra, GPT-5.6 Luna and GPT-6 Luna.
Both Luna candidates support `xhigh`. The exact name "GPT-5.0 Luna" did not
resolve in the reviewed sources. Codex settings and API reasoning parameters
are separate; do not assume a Codex `ultra` option is an API parameter.

| Purpose | Candidate | Status |
| --- | --- | --- |
| Coding subagents | `gpt-6-sol`, `xhigh` | User requested; session availability confirmed. |
| Independent review | `gpt-6-astra`, `xhigh` | User requested; session availability confirmed. |
| Planner | Astra / Ultra | User requested; current runtime setting not independently inspected. |
| Korean text translation | `gpt-6-luna`, `xhigh` | User selected after name verification; account access pending. |
| Cloud transcription | `gpt-transcribe` | Deliberate on-phone speech tests passed on Oct24-25; see DEVICE_TESTS. |

Standard published text rates per million tokens: GPT-5.6 Luna $0.20 input /
$1.20 output; GPT-6 Luna $0.10 input / $0.50 output. These are list rates, not a
per-message estimate; include reasoning, audio, and actual token usage in any
benchmark. Both are text models, so speech transcription is a separate stage.
Extra High should be evaluated for both quality and interactive latency.

- [Model catalog](https://developers.openai.com/api/docs/models)
- [GPT-6 Sol](https://developers.openai.com/api/docs/models/gpt-6-sol)
- [GPT-6 Astra](https://developers.openai.com/api/docs/models/gpt-6-astra)
- [GPT-5.6 Luna](https://developers.openai.com/api/docs/models/gpt-5.6-luna)
- [GPT-6 Luna](https://developers.openai.com/api/docs/models/gpt-6-luna)
- [File transcription](https://developers.openai.com/api/docs/guides/speech-to-text)

At initial planning, no authenticated model list or inference call had been made.
Andrew subsequently saved a dedicated key privately on his phone and actual
transcription passed. Translation account access and measured cost remain
unverified. No credential from another project was read or copied.

### Phase3 API verification (2026-10-25)

Reopened official [GPT-6 Luna documentation](https://developers.openai.com/api/docs/models/gpt-6-luna):
the selected gpt-6-luna/xhigh supports Responses and structured output. Standard
list rates are $0.10/M input, $0.01/M cached input and $0.50/M output. Use actual
returned usage for a labelled estimate; reasoning counts within output once.

The [text guide](https://developers.openai.com/api/docs/guides/text) and
[structured-output guide](https://developers.openai.com/api/docs/guides/structured-outputs)
support a typed `text.format` schema and parsing message/output_text alongside
reasoning items. Refusal and incomplete responses need explicit handling.
The [reasoning guide](https://developers.openai.com/api/docs/guides/reasoning)
confirms the generated-token ceiling includes reasoning and visible output.
The first candidate uses8192 tokens; quality/latency and any incomplete result
must be measured rather than silently changing the selected effort or retrying.

[Data controls](https://developers.openai.com/api/docs/guides/your-data) distinguish
application-state storage from provider retention. Set store=false, omit tools,
conversation and background processing. Standard Responses abuse-monitoring
retention may be up to30 days; no zero-retention claim follows from local RAM-only
handling or store=false. See PHASE3 for the implementation/acceptance contract.

## Focused-node confirmation investigation (2026-10-24)

Pre-fix source finding: `observeEditor` refreshed the root, obtained its focused
descendant, then synchronously read InputConnection context. It did not refresh
that focused node. Android documents that [refresh](https://developer.android.com/reference/android/view/accessibility/AccessibilityNodeInfo#refresh())
updates the represented view; refreshing a container does not promise to refresh
its descendants. AOSP's [Android 16 findFocus implementation](https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android16-release/core/java/android/view/accessibility/AccessibilityInteractionClient.java)
can return a cached focused node. Samsung behavior still requires measurement.

Inference, not a device conclusion: the node snapshot and connection context may
straddle commit application or cache updates. The first diagnostic compares the
original mismatch and bounded same-destination reads before/after node refresh,
only for the synthetic fixture in debug REVIEW. It must leave terminal uncertainty
and the one-commit decision untouched. A stale snapshot is not permission to
ignore an unknown caret, identity mismatch, or navigation event.

Measured diagnostic: at 15ms after the commit, the same node/root/input generation
matched while the connection contained the exact 42-unit expected context/caret
and the node still reported -1/-1 with hint text. At 43-60ms the same discrepancy
remained before node refresh; a successful 4ms refresh supplied the expected
42/42 caret and text. At 210-221ms both observations were already consistent.
This establishes a stale focused-node snapshot as the proximate failure on that
run; cache timing versus commit/read ordering was not isolated. The diagnostic
left the transaction terminally UNCERTAIN and never retried it.

The reviewed fix refreshes the same focused node after the connection read,
rechecks current transaction/window/root/node/input metadata, then applies the
unchanged exact caret/context confirmation. Android 16's [InputMethod source](https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android16-release/core/java/android/accessibilityservice/InputMethod.java)
returns a new connection wrapper per getter, so wrapper object equality is not a
valid continuity test. Input generation and the stored EditorInfo remain checks.

Andrew reported repeated Proton Pass save-password prompts during synthetic tests.
No credential was entered. The fixture's empty password-type editor is a likely
autofill heuristic trigger, not a confirmed Proton diagnosis. Android documents
[autofill heuristics and per-view opt-out](https://developer.android.com/identity/autofill/autofill-optimize).
The fixture now marks its own root NO_EXCLUDE_DESCENDANTS and its editors NO,
retaining the empty protected field and its input type. No Proton/global setting
was changed; prompt cessation still requires observation.

Andrew subsequently reported the prompt again, so hierarchy opt-out alone did
not establish a fix. REVIEW insertion tests now request a fixture without the
unused password editor; its default remains present for the protected-field
bubble test. Andrew also explicitly authorized clicking the visible "No, thanks"
save action. A separate opt-in helper targets only the Android SYSTEM-window
negative resource `android:id/autofill_save_no`, with a unique visible enabled
clickable node and a dismiss-only label (No thanks / No thank you / Not now).
It rejects "Never" and all other labels, rechecks before one click, and otherwise
reports NOT_FOUND. It reads no proposed credentials and changes no provider settings.
The framework resource is corroborated by [AOSP autofill CTS UiBot](https://android.googlesource.com/platform/cts/+/798fbbbb9ae7fb51b61c8ad5fec90db4fdb9bcea/tests/autofillservice/src/android/autofillservice/cts/testcore/UiBot.java).
The one Oct24 invocation returned NOT_FOUND, so it made no click. Subsequent
synthetic passes do not establish that the reported prompt can no longer recur.

## Postcommit composition policy (2026-10-24)

Documentation finding: Android 16's accessibility commit path calls batch-start,
finishComposingText, commitText, then batch-end. Its normal BaseInputConnection
commit implementation uses a noncomposing replacement. An IME can subsequently
mark existing characters composing: setComposingRegion changes neither text
contents nor cursor position. Exact inserted text and composition can therefore
coexist; a composing marker alone does not prove an incorrect insertion.

- [Android 16 accessibility commit implementation](https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android16-release/core/java/android/view/inputmethod/RemoteInputConnectionImpl.java)
- [Android 16 BaseInputConnection](https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android16-release/core/java/android/view/inputmethod/BaseInputConnection.java)
- [setComposingRegion contract](https://developer.android.com/reference/android/view/inputmethod/InputConnection#setComposingRegion(int,%20int))

Measured limitation: the compact candidate's postcommit observation rejected
SPAN_COMPOSING before exact expected-context and complete target matching. Its
separate smoke text assertion does not establish all those conditions at the
same instant. SwiftKey re-marking text is a hypothesis; producer/timing/range
have not been measured. Composition still indicates active IME editing state.

Decision: retain every precommit guard and current terminal uncertainty rule.
Gather the missing conjunction using a bounded, debug REVIEW, synthetic-only
diagnostic, without another mutation or later rehabilitation. Stop after at
most two fresh attempts. Any proposed completion-policy change must distinguish
permission to mutate from observed result, retain one consumed attempt, and
come back for discussion with measured evidence. Copy & close remains the
practical fallback if a small correction cannot demonstrate a usable flow.

Later measured evidence on the same secure diagnostic build: a single ordinary
middle-insertion regression reproduced the composing refusal. ORIG29ms supplied
complete same-target/host identity and exact expected context/caret/node agreement.
One composing span occupied only the new result's last word plus punctuation;
the original prefix and suffix were preserved. Read-only postterminal samples
through557-572ms retained the exact context/caret and same composing span. These
samples do not establish uninterrupted transaction continuity or future stability.
They support discussing confirmation based on the observed exact result; no
confirmation contract was changed, no transaction rehabilitated, and no second
commit issued. The earlier two-attempt empty diagnostic round remains closed.

User decision after Astra Ultra review: approve prospective confirmation of an
exact observed result even with postcommit composition, retaining all precommit
eligibility checks. The keyboard may subsequently edit text; this is not a
composition-free completion guarantee. Implementation must keep true composing
metadata, require full destination continuity and exact context/offset/caret,
terminate composed nonmatches immediately, and never upgrade an ended uncertain
transaction. Deterministic exact and mismatching composed fixtures are required
to demonstrate the changed branch before external insertion is proposed.

## Credential and data handling

OpenAI recommends keeping API keys server-side and out of client code. Andrew
chose personal on-device key entry; encrypted local storage is a consciously
different architecture, not equivalent protection to a backend. Android
Keystore can protect the encryption key, while the API secret must still be
used by the running app. Backup/transfer exclusions require explicit rules.

- [OpenAI authentication guidance](https://developers.openai.com/api/reference/overview)
- [Android Keystore](https://developer.android.com/privacy-and-security/keystore)
- [Android backup behavior](https://developer.android.com/identity/data/autobackup)
- [OpenAI data controls](https://developers.openai.com/api/docs/guides/your-data)
- [Spend alerts versus hard limits](https://developers.openai.com/api/docs/guides/spend-limits)

Do not claim that local non-retention or API `store=false` implies zero
provider retention. Verify the selected endpoint's current controls before
writing the app's privacy explanation.

Current API documentation distinguishes soft budget alerts from enforceable
organization/project hard limits. Hard-limit enforcement can lag slightly;
verify the configured account control rather than treating alerts as a cap.
# Phase2 transcription contract recheck — 2026-10-24

Official [file transcription guide](https://developers.openai.com/api/docs/guides/speech-to-text)
recommends `gpt-transcribe` for completed recordings. Use one multipart request
after explicit Stop with file/model and an English `languages[]` hint; use the
JSON transcript for editable review. Published upload limit25MB; our initial
60s mono16kHz/16-bit in-memory PCM cap is much smaller. No contextual editor text
or chat history belongs in the request. Account access/quality remain untested.

The [request reference](https://developers.openai.com/api/reference/resources/audio/subresources/transcriptions/methods/create)
defines POST `/v1/audio/transcriptions`, file metadata and JSON `text` output.
Use filename `recording.wav`, content type `audio/wav`, `response_format=json`,
`languages[]=en`; do not send the deprecated singular language field with this
model. Fixed API host, no redirects, no automatic retry, sanitized errors and
bounded request lifetime are application design decisions, not provider guarantees.

Android [backup documentation](https://developer.android.com/identity/data/autobackup)
defines independent file/database/sharedpref/external/root and device-protected
domains. Store encrypted credentials in noBackupFilesDir, exclude all applicable
domains from cloud and transfer rules, retain allowBackup=false and legacy rules.
The [Keystore AES-GCM example](https://developer.android.com/reference/android/security/keystore/KeyGenParameterSpec)
supports generating the AES encryption key inside AndroidKeyStore. Device storage
and backup configuration need review/tests; hardware backing is not assumed.
