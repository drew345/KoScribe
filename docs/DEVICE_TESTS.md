# Phase 1 device acceptance record

Status: not started. No Android device behavior is verified yet.

## Environment to verify

- Galaxy A35 5G; Android 16 / One UI 8.5 are user-reported.
- Record actual build, SwiftKey version and selected keyboard, SMS/RCS package,
  and KakaoTalk / Instagram versions when connected.
- Confirm exactly one authorized physical device and intended main user before
  installing `com.koscribe.prototype`. Do not record raw device identifiers.
- Keep Wispr Flow installed and enabled as Andrew normally uses it.

## Acceptance matrix

Use synthetic messages in the local test editor first. Real-app tests remain
unsent drafts; Andrew controls any manual Send. Record observed results, not
inferences from unit tests.

| Check | Local editor | KakaoTalk | Instagram DM | SMS/RCS |
| --- | --- | --- | --- | --- |
| Bubble visible; SwiftKey stays selected; Wispr coexists | Pending | Pending | Pending | Pending |
| Choose 요 / 반말 / EN; selected mode visible | Pending | Pending | Pending | Pending |
| Microphone starts; Stop/Cancel/background/lock stop it | Pending | Pending | Pending | Pending |
| Review/edit English with SwiftKey; return to original field | Pending | Pending | Pending | Pending |
| Empty/beginning/middle/end insertion; existing text preserved | Pending | Pending | Pending | Pending |
| Existing Korean, English, emoji; composition preserved/refused | Pending | Pending | Pending | Pending |
| Selected text or unknown caret refuses automatic insertion | Pending | Pending | Pending | Pending |
| Changed text/field/app/conversation (including switch-back) refuses | Pending | Pending | Pending | Pending |
| Cancel, repeated controls, rotation, service loss do not insert late | Pending | Pending | Pending | Pending |
| No automatic Send/Enter; uncertain outcome never auto-retries | Pending | Pending | Pending | Pending |
| Explicit Copy recovery works after refusal | Pending | Pending | Pending | Pending |

Separately check password/PIN/nontext fields, unavailable microphone, denied
permission, and unavailable accessibility service. Do not enter real secrets.
Build warnings and unit-test results belong in `PROGRESS.md`; failures here
must describe the observed action/result without saving personal chat text.

## Gate

Phase 2 remains blocked on the required device evidence and any necessary
Phase 1 fixes. Unsupported fields must refuse safely. A passing local editor
test does not establish compatibility with another app.
