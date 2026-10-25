# KoScribe

Personal Android dictation app: speak English and insert natural Korean into
the focused text field, while continuing to use Microsoft SwiftKey.

**Status: Bounded Phase 1 and Phase 2 gates passed on A35; Phase 3 Korean translation is in device testing.**
Fixed Korean insertion passed in empty KakaoTalk, Google Messages and Instagram
fields. Synthetic checks cover preservation of surrounding text, selected-text
refusal, uncertain commits without retries, editable review, and dismissal after
Copy or confirmed insertion. Manual SwiftKey editing and insertion also passed
with the destination cursor already restored. Broader daily-use reliability
remains untested; these results apply to the builds recorded in
[device acceptance](docs/DEVICE_TESTS.md).

The separate [Phase 2 candidate](docs/PHASE2.md) adds private on-phone API key
entry and real English transcription after Stop. Source review, unit tests and
setup checks preceded private key entry. Real speech review/Cancel and
SwiftKey-edited insertion passed in the test editor. Recording-control visibility
and active Cancel also passed. Offline failure followed by a fresh successful
recording/review/Cancel passed on October25. Broader daily-use reliability remains
untested, including the cause of a morning service-readiness incident. The
[Phase 3 candidate](docs/PHASE3.md) adds confirmed-English translation to 요 or
반말 using the same saved key. Independent source review and all 64 unit tests
passed; model access, Korean quality and phone behavior still need device evidence.
The [Phase 1 sample build](docs/PHASE1.md) remains available
without an API key; ordinary and release builds default to OFF. See
[PROGRESS](PROGRESS.md) for the current installed build and next action.

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
5. When the result is ready, check the intended message field and cursor.
   Tap the field if you need to select it or reposition the cursor, then tap
   **Insert** in the floating box. KoScribe captures and checks that destination
   afresh before insertion. EN skips translation. If the check fails, keep the result for an
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
diagnostics on 2026-10-23. Recording/review does not depend on an earlier editor
binding. Explicit Insert creates a fresh guarded destination transaction.

The extra field tap was successfully omitted in one manual synthetic test when
the intended cursor was already restored after English review. Keep that check
explicit; other apps may return focus differently.

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
credentials, recordings, and transcripts are excluded from Git. This command
builds the default OFF variant; see Phase 2 for the explicit speech candidate
and Phase 3 for the opt-in translation candidate.
