# Planning research

Checked 2026-10-23. Documentation research, not hands-on competitor or phone
testing. Recheck dynamic API/platform facts when implementation begins.

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

An accessibility overlay may avoid a separate draw-over-apps permission.
A conventional application overlay needs SYSTEM_ALERT_WINDOW special access.
A nonfocusable overlay can help retain the editor/keyboard focus. This is an
architecture candidate, not a tested decision.

- [Window types and flags](https://developer.android.com/reference/android/view/WindowManager.LayoutParams)
- [Overlay permission](https://developer.android.com/reference/android/Manifest.permission#SYSTEM_ALERT_WINDOW)

Recording requires microphone permission. A microphone foreground service
also has declared type/permission requirements and a notification. Background
service-start exceptions and microphone while-in-use eligibility are separate
checks; visible overlay alone is not proof a microphone start is permitted.
The A35 prototype must test this; a visible recording Activity is a fallback.
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
| Cloud transcription | `gpt-transcribe` | OpenAI cloud provider selected; current documented baseline, account access pending. |

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

No authenticated OpenAI model list or inference call was made. Account-specific
model access and billing remain unverified. No API key was found in the current
process; no credential from another project was read or copied.

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
