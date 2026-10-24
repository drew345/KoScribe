# KoScribe

Personal Android dictation app: speak English and insert natural Korean into
the focused text field, while continuing to use Microsoft SwiftKey.

**Status: Phase 1 feasibility prototype implemented; A35 device testing underway.**
The prototype tests the bubble, microphone lifecycle, English review, and safe
insertion with fixed example messages. It does not transcribe or translate yet
and needs no API key. See [build and device test instructions](docs/PHASE1.md).
The earlier prototype passed eight synthetic cross-app checks on the A35, including middle insertion,
edited review, Korean/emoji preservation, and cancellation/refusal. Real
messaging-app acceptance and actual SwiftKey typing remain pending.

The installed Oct24 REVIEW build adds editable English review and Copy & close
to the contextual, draggable bubble. All seven synthetic REVIEW gates now pass:
Copy, confirmed empty/middle/Korean insertion, selection refusal, cancellation,
and terminal uncertainty without retry. Refreshing a stale focused-node snapshot
resolved insertion confirmation while retaining the safety checks. Copy and
confirmed insertion dismiss the bubble. Earlier visibility, dragging, and saved
position gates passed on the RESELECT build. Actual SwiftKey typing and current
real-app acceptance remain pending; the ordinary default build is still OFF.

## Agreed interaction

1. Tap an editable text field to show the keyboard and the floating bubble,
   then tap the bubble. The idle bubble stays hidden outside editing. Drag it
   to a preferred position; remember that position across uses.
2. Tap one of three visible choices to start recording:
   - **요**: polite conversational Korean (해요체).
   - **반말**: casual Korean.
   - **EN**: English dictation without Korean translation.
3. Speak; the chosen label remains visible. Tap **Stop**.
4. Review the captured **English text**. Correct it with **Edit**, then choose
   **Translate** (Korean modes) or **Use English** (EN). **Cancel**
   discards the session. Review appears for every message initially; a skip
   option can be added later.
5. When the result is ready, tap the intended message field, then tap **Insert**
   on the bubble. KoScribe captures and checks that destination afresh before
   insertion. EN skips translation. If the check fails, keep the result for an
   explicit new destination attempt; never silently retarget or retry a commit.
6. After successful insertion or explicit Copy, dismiss the bubble for that
   interaction. Review/edit in the destination app and press Send yourself.

Each message requires an explicit mode choice. No hidden gesture determines
politeness. There is no 습니다 output mode. The English review is editable text,
not an image; there is no required second preview of the Korean result. Cancel,
error recovery, the final bubble icon, and the supporting settings screen still
need detailed design. Contextual visibility, dragging, and remembered position
are the current priority before further capabilities.

The separate destination-selection step was chosen after the A35 startup
diagnostics on 2026-10-23. Recording/review will no longer depend on an earlier
editor binding. This revised workflow is being implemented; real-app tests
remain non-inserting until their separate scope is agreed.

## Minimum useful fallback

Andrew specified this on 2026-10-24: open KoScribe from the home-screen icon,
tap its microphone, dictate English, review and edit the English with SwiftKey,
choose 요 or 반말 to translate, then tap Copy & close. He manually long-presses
the intended message field and pastes. A separate Close control is also useful.
The bubble/direct-insertion design must demonstrate a benefit over that simpler
workflow; the fallback is an accepted product direction, not yet implemented.

## Initial device and apps

- Samsung Galaxy A35 5G; Android 16, One UI 8.5 (verified on the connected phone).
- Microsoft SwiftKey remains the keyboard; Wispr Flow remains installed.
- First destinations: KakaoTalk, Instagram DMs, and the SMS/RCS app.
- Default SMS/RCS app is Google Messages; Samsung Messages is also installed.
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
- Speech recognition: OpenAI cloud transcription first, using the currently
  recommended `gpt-transcribe` recorded-audio workflow. Some extra processing
  time is acceptable; measure it rather than assuming a fixed latency target.
- Prove phone/editor integration before investing in full speech/translation.

## Project memory

- [AGENTS.md](AGENTS.md): durable work rules and constraints.
- [IMPLEMENTATION.md](IMPLEMENTATION.md): architecture proposals and phase gates.
- [PROGRESS.md](PROGRESS.md): current status, unresolved decisions, next action.
- [SESSION_LOG.md](SESSION_LOG.md): concise work history.
- [Research](docs/RESEARCH.md): dated official sources and limitations.
- [Device acceptance](docs/DEVICE_TESTS.md): measured results and pending checks.

## Prototype build

With JDK 17+, Android SDK 36 / Build Tools 36.0.0, and the environment configured
as described in [Phase 1](docs/PHASE1.md):

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

The debug APK is `app/build/outputs/apk/debug/app-debug.apk`. Build output,
credentials, recordings, and transcripts are excluded from Git. Real-device
acceptance is required before starting the speech/API phase.
