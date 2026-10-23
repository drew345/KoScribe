# Phase 1 device feasibility prototype

This is a sample-only Android app for checking the bubble, microphone lifecycle,
SwiftKey coexistence, and safe text insertion on the Galaxy A35. It has no
`INTERNET` permission, API key, speech recognizer, or translation request. The
microphone is activated in a visible Activity; frames are read and discarded.
Stop shows the same editable English example every time:
`Hello, I will arrive in ten minutes.` Korean buttons map this exact example to
fixed polite/casual examples. Editing it disables Korean sample insertion;
EN mode can insert edited English text.

## Build and install

Use Android SDK platform 36, Build Tools 36.0.0, Gradle 8.13, and JDK 17 or
newer. In PowerShell from the repository root:

```powershell
$env:JAVA_HOME = 'C:\AI\Codex\Tools\openjdk21\jdk-21.0.12.1+1'
$env:ANDROID_HOME = 'C:\AI\Codex\Tools\android-sdk'
$env:GRADLE_USER_HOME = 'C:\AI\Codex\Tools\gradle-cache'
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
& "$env:ANDROID_HOME\platform-tools\adb.exe" devices -l
& "$env:ANDROID_HOME\platform-tools\adb.exe" -d shell pm list users
& "$env:ANDROID_HOME\platform-tools\adb.exe" -d install --user 0 -r app/build/outputs/apk/debug/app-debug.apk
```

Confirm exactly one physical device (and its users) before installing. Set
`JAVA_HOME`, `ANDROID_HOME`, and `GRADLE_USER_HOME` for the prepared shared
toolchain. Gradle may need network access to fetch dependencies on a fresh
build machine.

No release signing or API credentials are required; the installed prototype
works offline. The local `MainActivity` offers a test editor and links to Android Accessibility
settings. Enable **KoScribe Phase 1 input test** there and grant microphone
permission. On a sideloaded build, Android may first require enabling
restricted settings for KoScribe in App info. Keep SwiftKey selected.

The current backup/transfer exclusion covers the private root for this
no-storage probe. Before Phase 2 credential storage, review every applicable
backup domain and verify Samsung device-transfer behavior on the phone.

## Test sequence

1. Focus the local test editor or a draft field in KakaoTalk, Instagram DMs,
   or the actual SMS/RCS app. Put known text before and after a collapsed
   caret. Tap **Ko**, then **요**, **반말**, or **EN**. This launches a visible
   recording screen. The chosen mode stays visible.
2. Verify the system microphone indicator appears. Tap **Stop**; verify it
   disappears. The editable English sample appears. Try **Edit English**.
3. Confirm the unchanged Korean sample or EN sample. The recording screen
   closes; KoScribe tries one accessibility `commitText` only if the original
   editor, caret, and surrounding text still match. Inspect the draft and
   SwiftKey state. KoScribe does not send or press Enter.
4. Repeat for beginning/middle/end, existing Korean and emoji, selected text,
   active SwiftKey composition, text changed while processing, switching
   field/app/conversation, lock/rotation, password/PIN, cancelled session,
   and rapid repeated controls. Record which fields accept insertion or fail
   closed. Do not send test messages unless Andrew chooses to.
5. If insertion is refused, open **Ko** and use **Copy result manually** only
   after verifying the intended destination. Copy replaces the system
   clipboard. If the outcome says *uncertain*, inspect the editor before
   pasting to avoid duplication.

Record the phone build, SwiftKey version/selection, SMS package, app versions,
individual outcomes, and any logs sanitized of message content. No phone or
destination-app behavior is claimed verified until measured on the device.

## Guard and known limits

The service requires an editable text node, non-password text input, a known
collapsed selection, a live
accessibility input connection, one globally focused application window, a
stable window/source-node identity, and matching local surrounding text. It
observes focus, content, navigation and input-session events while a session
is live and refuses if the target has changed. Commit is reserved before
the IPC call, so uncertain outcomes are never retried automatically. The
service never invokes `ACTION_SET_TEXT`, keyboard replacement, editor actions,
Enter, or Send.

Known active composition is refused using editor selection callbacks or styled
surrounding text. When the editor does not report composition, the state stays
**unknown**, not clear. This sample-only prototype permits that case after all
other target checks because the Android accessibility commit path finishes
composition before inserting text in [AOSP's input connection
implementation](https://android.googlesource.com/platform/frameworks/base/+/eabfd2281dfa777d8b7e10619f7e014cc56d3168/core/java/android/view/inputmethod/RemoteInputConnectionImpl.java).
OEM/custom editor behavior and SwiftKey interaction still require the local
synthetic editor test before any real app draft.

Some apps omit selection or context; those fields are unsupported for automatic
insertion in this prototype. A same-app view can
reuse source identity, and target checks plus commit are not atomic across
processes. These are device-test questions, so a successful unit test is not proof of
safe behavior in a real conversation. Phase 2 remains gated on the A35
results in `IMPLEMENTATION.md`.
