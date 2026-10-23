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
