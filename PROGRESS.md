# Current progress

Updated: 2026-10-23 (Asia/Seoul).

## State

Phase 0: planning. Folder initialized with project memory. No application code,
SDK project, API integration, device test, or paid API call has been created.
Private repository created and visibility verified:
https://github.com/drew345/KoScribe . Initial planning commit was pushed to
`main`; local `main` tracks `origin/main`.

## Confirmed this session

- Galaxy A35 5G, Android 16, One UI 8.5 (user-reported).
- First apps: KakaoTalk, Instagram DMs, SMS/RCS (exact SMS app unverified).
- Keep SwiftKey and installed Wispr Flow.
- Tap bubble -> choose 요 / 반말 / EN -> record -> Stop -> editable English
  review -> confirm -> translate if Korean -> safe immediate insert.
- English review appears for every message initially; skip option comes later.
- User supplies a key on the phone; no embedded key or Git credential storage.
- Translation: `gpt-6-luna` at `xhigh` (Extra High).
- Speech recognition: OpenAI cloud transcription first; documented baseline
  `gpt-transcribe`. Some additional processing time is acceptable.
- Sol 6 Extra High coding; Astra Extra High checking; main planner/overseer.

## Open decisions / checks

- Before Phase 2: verify actual API-account access to GPT-6 Luna and the
  selected OpenAI transcription model, plus billing/spending controls.
- Measure Stop-to-transcript and confirmation-to-insertion delays during
  testing. No fixed numeric target; normal dictation length is still unknown.
- Personal installation/update method; whether any wider distribution is wanted.
- Bubble icon/placement, detailed cancel/recovery UI, and settings screen.
- Verify phone build, SMS package, microphone start restrictions, safe editor
  insertion, and coexistence when Andrew connects the phone for debugging.

## Next action

No further product choice is required for the API-free Phase 1 prototype.
Andrew asked whether anything else is needed before starting; implementation
has not begun. Next implementation work: prepare the Android toolchain, build
the feasibility prototype with Sol Extra High, then review with Astra Extra
High. Request USB connection when ADB and the first installable prototype are
ready. No API key is needed for Phase 1.

## Verification so far

Official Android/competitor/OpenAI documentation reviewed. Codex local model
catalog confirms requested coding/review models and reasoning settings. No
device behavior or authenticated OpenAI model availability is yet verified.
No API key was retrieved; current process has no OPENAI_API_KEY.
Planning review added protected-field exclusions, same-app conversation-switch
checks, cancellation ownership, and at-most-once insertion. Staged-file secret
patterns and ignore rules checked; no application tests are applicable yet.
English-review flow approved; safe return from editing to the intended message
field remains an explicit device-test gate.
Read-only toolchain check: adb, Java/Javac, Gradle and sdkmanager were not found
on PATH; the checked standard Android SDK/Studio/Java locations were absent.
This is not an exhaustive scan for custom installations. Toolchain preparation
is required before device testing.
