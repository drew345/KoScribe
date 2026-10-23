# Approved opt-in KakaoTalk draft test

Status: Andrew explicitly approved the scoped exception below on 2026-10-23
after automatic approval review rejected the original two attempts. His earlier
self-chat readiness confirmation had not resolved that automated block. Sol
implemented the test; Astra's final rereview and smoke assembly/lint passed.
Two preflights stopped before any product action (strict node-caret check, then
hidden SwiftKey). After Andrew restored SwiftKey, the flow ran but production
refused on TARGET_CONTENT_CHANGED_OTHER_NODE. The original field remained empty
through the result polling interval; the session trace contains no commit.
Do not retry this confirmed session. Diagnose using code-only metadata first.

Approved scope:

- Add a separate test class that is skipped unless an explicit external-draft
  opt-in argument is supplied. Ordinary synthetic test runs cannot invoke it.
- Run it once only after Andrew confirms KakaoTalk Chat with myself is ready.
  The self-chat identity comes from Andrew; the test does not read chat history.
- Require KakaoTalk (`com.kakao.talk`) to be the sole focused application,
  SwiftKey visible, and its focused input node to be visible, enabled, editable,
  non-password, empty, and without a selected range. Otherwise stop before any
  session starts. Inspect only root metadata and the focused input node.
  The test permits initial empty/hinted node selection only at 0/0 or exact
  -1/-1, consistent with the previously approved product rule: fresh visible
  Stop establishes that the unchanged input-connection check succeeded. Exact
  nonempty result checks require known collapsed in-bounds selection.
- Use only KoScribe's own buttons: open bubble, choose 요, record briefly,
  Stop, confirm the fixed sample. Existing production insertion guards apply.
- Expected unsent draft: `안녕하세요. 10분 후에 도착할게요.`
- Verify the original field identity and exact one-time result. Do not seed,
  select, replace, or click the real editor directly. Never press Send, Enter,
  or an editor action. Do not take screenshots or log field contents.
- On failure, cancel only through KoScribe's own Cancel control. Do not switch
  apps, use global Back, change permissions, or alter the keyboard.

This is an explicit exception to the earlier synthetic-test-only scope, limited
to the user-prepared unsent self-chat draft. It does not authorize other chats,
recipients, real messages, or sending. Astra must review the implementation and
the build must pass before the device test runs. Hidden same-view conversation
reuse remains an acknowledged limitation of the prototype.

## Authorized follow-up: recording and Cancel only

Andrew authorized continued scoped diagnostics without inserting or sending on
2026-10-23. The lifecycle timing build and corrected test are installed; three complete
recording/Stop/Cancel runs passed. See `DEVICE_TESTS.md` for the full results.
Use the separately opted-in diagnostic path in `INSTRUMENTATION.md` with
the same empty self-chat/SwiftKey preconditions. Open KoScribe, choose 요, wait
briefly for recording, Stop, then Cancel. There must be no confirmation, Copy,
or insertion path. Verify the original draft stays empty and the microphone
stops; do not inspect conversation history or keyboard contents.

The product diagnostic records only first-event timing relative to capture,
event category/source-equality code, cached own-panel state, and own lifecycle
milestone timing described in `INSTRUMENTATION.md`.
It adds no cross-app reads and changes no guard decision. Zero/unknown timestamps
remain unknown. Its purpose is to distinguish event timing from panel state;
timing alone cannot establish that an event is harmless or justify ignoring it.
Source review and build passed before Andrew approved this ongoing diagnostic
scope. Do not request the same authorization again. The completed one-message
trial remains closed; do not reuse the draft test, which has a confirmation step.
