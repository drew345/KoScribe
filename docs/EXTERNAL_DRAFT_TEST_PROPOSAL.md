# Proposed opt-in KakaoTalk draft test

Status: not implemented or run. Automatic approval review rejected adding the
test twice, citing a restriction against real messaging-app interaction. Andrew
has prepared an empty self-chat composer, but that readiness confirmation did
not resolve the automated block. The synthetic suite remains unchanged.

Requested scope extension:

- Add a separate test class that is skipped unless an explicit external-draft
  opt-in argument is supplied. Ordinary synthetic test runs cannot invoke it.
- Run it once only after Andrew confirms KakaoTalk Chat with myself is ready.
  The self-chat identity comes from Andrew; the test does not read chat history.
- Require KakaoTalk (`com.kakao.talk`) to be the sole focused application,
  SwiftKey visible, and its focused input node to be visible, enabled, editable,
  non-password, empty, and without a selected range. Otherwise stop before any
  session starts. Inspect only root metadata and the focused input node.
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
