# KoScribe

Personal Android dictation app: speak English and insert natural Korean into
the focused text field, while continuing to use Microsoft SwiftKey.

**Status: planning only. No Android application has been implemented.**

## Agreed interaction

1. Focus a message field and tap the floating bubble.
2. Tap one of three visible choices to start recording:
   - **요**: polite conversational Korean (해요체).
   - **반말**: casual Korean.
   - **EN**: English dictation without Korean translation.
3. Speak; the chosen label remains visible. Tap **Stop**.
4. Insert the result immediately if the original focused field is safe.
   If it cannot be verified, preserve the result for explicit copy/retry.
5. Review/edit in the destination app and press Send yourself.

Each message requires an explicit mode choice. No hidden gesture determines
politeness. There is no 습니다 output mode. Cancel, error recovery, the bubble
icon, placement, and the supporting settings screen still need detailed design.

## Initial device and apps

- Samsung Galaxy A35 5G; Android 16, One UI 8.5 (user-reported).
- Microsoft SwiftKey remains the keyboard; Wispr Flow remains installed.
- First destinations: KakaoTalk, Instagram DMs, and the SMS/RCS app.
- SMS app may be Samsung Messages; verify its package when the phone is
  connected for debugging. Phone access is planned for a later session.
- Long-term platform: Android. Personal-use distribution is the starting
  assumption; installation/update method is not yet settled.

## Core requirements

- Preserve meaning, names, numbers, dates, negation, and uncertainty.
- Match the selected Korean speech level consistently; polite output should
  sound conversational, not like a formal business letter.
- Insert at a verified caret without unintended replacement of existing text.
- Never send, submit, or trigger an editor action automatically.
- Keep message processing transient. No chat-history ingestion is needed.
- User enters their own API key on the phone; protect it locally and keep it
  outside Git and the APK. No backend is selected for the first version.
- Translation model: GPT-6 Luna with Extra High reasoning (`gpt-6-luna`,
  `xhigh`), subject to account access and quality/latency testing.
- Prove phone/editor integration before investing in full speech/translation.

## Project memory

- [AGENTS.md](AGENTS.md): durable work rules and constraints.
- [IMPLEMENTATION.md](IMPLEMENTATION.md): architecture proposals and phase gates.
- [PROGRESS.md](PROGRESS.md): current status, unresolved decisions, next action.
- [SESSION_LOG.md](SESSION_LOG.md): concise work history.
- [Research](docs/RESEARCH.md): dated official sources and limitations.

No build commands exist yet. The repository currently contains planning only.
