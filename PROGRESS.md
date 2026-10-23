# Current progress

Updated: 2026-10-23 (Asia/Seoul).

## State

Phase 1 feasibility prototype built with Sol 6 Extra High and independently
reviewed by Astra Extra High. Final build, seven unit tests, lint, and APK
signature verification passed. Ready for controlled A35 testing; ADB currently
lists no connected device. No API integration, device test, or paid API call
has been performed. Phase 1 acceptance is not complete.
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

Andrew has been asked to connect the unlocked A35 with a USB data cable and
authorize USB debugging. Confirm one physical device
and its user profiles; install only into the intended main user. Start with
synthetic text in KoScribe's local test editor before testing real-app drafts.
Follow `docs/PHASE1.md` and record outcomes in `docs/DEVICE_TESTS.md` before Phase 2.
No API key is needed for Phase 1.

## Verification so far

Official Android/competitor/OpenAI documentation reviewed. Codex local model
catalog confirms requested coding/review models and reasoning settings. No
device behavior or authenticated OpenAI model availability is yet verified.
No API key was retrieved; current process has no OPENAI_API_KEY.
Planning review added protected-field exclusions, same-app conversation-switch
checks, cancellation ownership, and at-most-once insertion. Staged-file secret
patterns and ignore rules checked during initialization and before the Phase 1
publish. No credential patterns or unexpected sensitive/generated paths were
found in the staged prototype. GitHub visibility reconfirmed PRIVATE.
English-review flow approved; safe return from editing to the intended message
field remains an explicit device-test gate.
PhoneCare resolved the custom tool locations on ROBERTSLGGRAM17: ADB 37.0.1
and Android SDK under `C:\AI\Codex\Tools\android-sdk`, Microsoft JDK
21.0.12.1 under `C:\AI\Codex\Tools\openjdk21\jdk-21.0.12.1+1`.
Existing platform/build-tools were copied into standard API 36 / 36.0.0 SDK
directories without removing originals. Official Gradle 8.13 was downloaded,
SHA-256 verified, and installed under `C:\AI\Codex\Tools\gradle\gradle-8.13`.
Shared Gradle cache: `C:\AI\Codex\Tools\gradle-cache`.

The prototype uses a nonfocusable accessibility bubble, visible recording
Activity, editable English sample review, fixed Korean examples, and explicit
Copy recovery. Audio is discarded. No INTERNET permission or persistent
message/audio storage exists. Final device behavior remains unverified.

Final checks: `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug` passed;
7 tests, 0 failures/errors. Lint: 0 errors, 18 warnings (17 prototype UI string
localization warnings and one Gradle update notice). APK signature verified;
packaged permission list contains only RECORD_AUDIO. APK SHA-256:
`1FEC3F9973171EFF2F94449B59033D71CB6AFAC56114069F5057284E93E5B0DD`.

Astra found no remaining demonstrated blocker for controlled synthetic local
editor testing after fixes to window binding, review return, navigation,
composition evidence, and stale cancellation. Conservative return events may
refuse normal insertion. Unknown composition, Samsung/SwiftKey behavior,
same-app conversation reuse, and cross-process commit timing still require
device evidence. Root-only backup exclusions must be expanded/tested before
Phase 2 credential storage.
