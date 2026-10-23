# Implementation plan

Planning baseline: 2026-10-23. Application coding is not yet authorized.

## Confirmed product choices

Use the three-mode bubble flow in `README.md`. Every message initially gets an
editable English transcript review after Stop. Korean modes offer Edit,
Translate & insert, and Cancel; EN offers Edit, Insert English, and Cancel.
Translate only confirmed English. After translation, insert immediately when
safe without requiring a second Korean preview. A skip-English-review option
is deferred until later. Keep SwiftKey and never send. Test on the A35 with
Wispr Flow still installed.

## Proposed architecture

Native Kotlin Android app with a small control/settings Activity, an
AccessibilityService for a bubble and tightly scoped editor access, and
explicit recording sessions. Jetpack Compose is a candidate for app screens;
the overlay toolkit should follow the feasibility results.

Prototype Android 13+ accessibility input connections first. They offer an
insertion API without selecting KoScribe as a keyboard. This is not proof of
compatibility with every editor. Prototype an accessibility overlay to avoid
unnecessary draw-over-other-apps access; retain a conventional application
overlay as an alternative only if needed and disclosed.

Android microphone foreground-service rules must be tested independently of
overlay visibility. A small visible recording Activity is a possible fallback
if a bubble-only start is unreliable; discuss any resulting interaction change.
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

The review/edit UI must preserve the intended destination separately from its
own text editor. A user editing the transcript with SwiftKey must never cause
the Korean result to be inserted back into KoScribe's review field. Restoring
the destination after a deliberate review transition requires fresh identity,
focus, selection, and context checks. If return cannot be verified, keep the
result for explicit recovery. Do not weaken navigation checks merely because
the flow includes a review screen.

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

1. Bind each recording/result to an unexpired session identifier, output mode,
   and editor session. Cancelled/superseded/restarted sessions cannot insert.
2. Exclude password/PIN and unsupported nontext editors before reading context.
   Before committing, reacquire and verify the current app/editor, focus,
   unlocked state, caret, and necessary surrounding context. Field identifiers
   alone are insufficient: cancel on navigation/editor-session discontinuity,
   including switching conversations within one app and returning later.
3. Abort automatic insertion for selected text, unknown selection, changed
   editor/text, or incomplete observations. Keep the generated message.
4. Permit at most one commit per session, through the accessibility input
   connection at a verified
   collapsed caret. Test SwiftKey composition/autocorrection explicitly.
5. Observe success where supported. An uncertain outcome must not trigger a
   retry that might duplicate text. Cross-app checks and insertion are not an
   atomic transaction; document remaining compatibility limits honestly.
6. Offer deliberate Copy/manual paste for unsupported fields. Copy replaces
   the system clipboard; do not promise transparent restoration. Mark the
   copied message sensitive where supported.
7. No ACTION_SET_TEXT shortcuts, coordinate-driven insertion, Send taps,
   Enter key injection, or editor-action invocation.

## Phases and acceptance gates

| Phase | Scope | Gate before advancing |
| --- | --- | --- |
| 0: Planning | Settle UX, credentials, models, speech provider, installation and latency goals. | Andrew approves implementation plan. |
| 1: Device feasibility | Minimal bubble, explicit microphone start/stop, hardcoded sample insertion; no API needed. | On A35: SwiftKey stays selected, original text preserved, no send, focus changes fail safely; KakaoTalk/Instagram/SMS tested. |
| 2: Speech and credentials | Keystore-backed user key, OpenAI recording/transcription, cancel, editable English review and EN insertion. | Accurate transcripts; edits are preserved; destination safely restored after review; mic stops on every exit; errors recover; no secret/audio logs. |
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
Also: edit transcript with SwiftKey, return to original destination, switch
conversation during review, cancel review, repeated confirmation, and verify
only the final confirmed English is translated/inserted.
No test sends a real message automatically.

Translation corpus should include requests, apologies, scheduling, negation,
questions, names/titles, numbers, mixed Korean names in English speech, and
multi-sentence messages in both modes. Quality and latency determine the final
translation model/effort; a model name alone is not acceptance evidence.
