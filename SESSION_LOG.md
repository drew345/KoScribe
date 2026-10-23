# Session log

## 2026-10-23 — Project initialization and planning

- Initialized project memory in an empty KoScribe folder at Andrew's request.
- Recorded floating-bubble English-to-Korean purpose, 해요체/반말 modes, optional
  EN, SwiftKey preservation, safe insertion, and never-send constraints.
- Andrew confirmed A35 5G / Android 16 / One UI 8.5, target apps, three visible
  mode choices that start recording, and immediate insertion when safe.
- Andrew selected personal on-device key entry. Proposed Keystore-backed
  encryption, backup/log exclusions, dedicated key, and no secret in Git/APK.
  No key retrieved, copied, or created; no paid API call made.
- Research subagents checked Android integration and Wispr Flow/DictoKey
  documentation. Accessibility input connections are the leading feasibility
  candidate; phone testing remains essential.
- Verified Sol 6 and Astra Extra High are listed for Codex. GPT-5.0 Luna name
  unresolved; official API catalogs list GPT-5.6 Luna and GPT-6 Luna.
- Andrew subsequently selected GPT-6 Luna Extra High for translation.
- Andrew added English transcript review and chose to show it for every
  message initially, with a skip option later. Confirmed/edited English is
  translated, then Korean inserts immediately when safe; no second preview is
  required. Added tests for returning from transcript editing to the target.
- Created GitHub repository `drew345/KoScribe` and verified PRIVATE visibility.
- Reviewed planning files, checked credential ignore rules, and scanned staged
  files for secret patterns. Published initial planning commit `68a0475` to
  private `origin/main` and set branch tracking.
- Andrew selected OpenAI speech recognition and accepts extra processing time.
  Baseline is `gpt-transcribe`; no fixed latency target is assumed.
- Read-only environment check did not find Android/Java tools on PATH or in
  checked standard locations. Prepare tooling before requesting USB connection
  for the first prototype test. API credentials are unnecessary for Phase 1.
- Planning decisions are sufficient for Phase 1; implementation has not begun.

## 2026-10-23 — Phase 1 implementation; awaiting A35

- Andrew authorized Phase 1 and pointed to MindHub's PhoneCare subproject.
  Followed the MindHub migration pointer to the shared authoritative files;
  reused PhoneCare's ADB/SDK/JDK tools. No phone-care files were changed.
- Prepared standard API 36 / Build Tools 36.0.0 SDK paths by copying existing
  tools; verified and installed official Gradle 8.13 and its pinned wrapper.
- Sol 6 Extra High implemented the Kotlin feasibility app: three-mode bubble,
  visible microphone screen, editable fixed English review, Korean samples,
  guarded one-attempt insertion, and explicit Copy recovery. No API/key/network
  integration; microphone audio is discarded.
- Astra Extra High independently reviewed source. Sol fixed microphone
  permission handling, input/window binding, navigation continuity, composing
  state evidence, cancellation ownership, and recording-screen task affinity.
  Unknown composition remains explicitly unknown; the platform's accessibility
  commit behavior supports this controlled prototype policy, pending A35 tests.
- Final debug build and seven unit tests passed. Lint has 0 errors/18 warnings
  (prototype UI localization and Gradle update notice). APK signature verified;
  packaged permission list contains only RECORD_AUDIO. No release key created.
- Astra cleared the source for controlled local-editor testing, not real-app
  compatibility. Added `docs/PHASE1.md` and an unmeasured device acceptance
  matrix. Phase 2 is still gated on real-device results.
- Requested USB connection when build/review completed. ADB starts correctly
  and currently lists no device. No installation or phone inspection occurred.
- Staged prototype secret-pattern scan passed; credential/audio/APK ignore rules
  verified. GitHub visibility reconfirmed PRIVATE before publication.
