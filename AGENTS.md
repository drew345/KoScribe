# KoScribe operating memory

## Start here

Read this file, `PROGRESS.md`, and the latest `SESSION_LOG.md` entry. Read
`README.md` for product requirements, `IMPLEMENTATION.md` for the phased plan,
and `docs/RESEARCH.md` when a technical decision needs its evidence.
`PROGRESS.md` is the single source of current state; keep history concise.

## Work authorization and model roles

- We are planning. Repository initialization and research are authorized;
  application implementation must wait for Andrew's go-ahead.
- Once implementation is authorized, use subagents for coding with
  `gpt-6-sol` / `xhigh` (Extra High), and independent checking with
  `gpt-6-astra` / `xhigh`. Explicit model overrides require fresh-context
  subagents; provide their task, constraints, and authoritative file paths.
- Main agent is planner and overseer. Andrew requested Astra Ultra for that
  role; do not claim the active runtime setting was verified or changed
  merely because it is requested. Do not silently substitute GPT-6.1 Sol.
- Implement and test in phases. Record review and device-test results before
  advancing; never describe an untested phone behavior as working.

## Nonnegotiable product constraints

- Android floating bubble is the main interface. Keep Microsoft SwiftKey as
  the selected keyboard. Do not create a replacement keyboard/IME service.
- Per message: open bubble, choose `요`, `반말`, or `EN` to begin recording,
  then Stop. Keep the selected output mode visible during recording.
- Initially show an editable English transcript for every message. Translate
  only after confirmation; Korean modes use `Translate & insert`, EN uses
  `Insert English`. Add an option to skip review only in a later phase.
- Korean modes are conversational 해요체 and casual 반말. Do not add a
  습니다 mode. English output is secondary, but approved in the selector.
- After transcript confirmation/translation, insert immediately only when the
  intended editor is verified safe. Preserve
  existing text. Refuse uncertain selection, changed target, or ambiguous
  insertion; keep the result available for explicit recovery.
- Never send messages, activate editor actions, emulate Enter, or tap Send.
  Never let translation output issue UI commands.
- Do not use whole-field replacement (`ACTION_SET_TEXT`) as an insertion
  shortcut. No blind retries after an ambiguous commit.

## Credentials and privacy

- Andrew selected a personal-use app with a key entered by him on the phone.
  Plan Android Keystore-backed encryption of the stored credential, no
  bundled key, and no secret in Git, APK resources, build constants, logs,
  crash reports, prompts, or documentation.
- Credential architecture must be agreed before implementation. The chosen
  approach is recorded in `IMPLEMENTATION.md`; changes require discussion.
- Never print credential values. Do not reuse another project's key silently.
  Prefer a dedicated KoScribe key with restricted permissions, budget alerts,
  and a verified hard spending limit where supported; alerts are not a cap.
- When a key or release signing credential is first created/changed or its
  backup is unconfirmed, remind Andrew to back it up in Cryptomator
  `DeveloperStuff`. If shared MindHub is available, update its non-secret
  portable-secrets checklist with purpose and confirmed backup status only.
- Inspect only the local editor context needed for safe insertion. Do not
  upload chat history, existing field contents, screenshots, or contacts.
  Send dictated content only; do not retain audio/transcripts by default.
- Gitignore is a precaution, not proof: inspect staged files and scan for
  secrets before every publish. Never commit personal recordings or chats.

## Memory maintenance

Update current state and a brief dated session entry after meaningful work.
Label proposals, user decisions, documentation findings, and measured device
results distinctly. Use Asia/Seoul dates for session notes.
