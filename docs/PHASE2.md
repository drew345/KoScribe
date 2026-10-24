# Phase2 speech and credentials

Implementation began2026-10-24 after bounded Phase1 feasibility passed in all
three named apps. See PROGRESS for exact source/build/device status; this file
describes the candidate, not an acceptance claim.

## Candidate and baseline

Build the speech candidate explicitly:

```powershell
.\gradlew.bat --console=plain -PkoscribeStartupArm=REVIEW -PkoscribeSpeech=true :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :smoke:assembleDebug :smoke:lintDebug
```

Speech requires REVIEW and nonvisual mode. Release and ordinary defaults disable
speech. Keep the saved secure sample APK as a rollback; never overwrite it with
ordinary output. Build/signature/source review and scoped synthetic checks must
precede private-key entry or a cloud transcription test.

The first speech stage supports EN output: bubble -> EN -> record -> explicit
Stop -> processing -> editable English -> Confirm English -> guarded Insert or
explicit Copy. Existing destination checks/one-attempt/no-Send rules apply.
Korean translation is Phase3; actual speech never receives canned Korean output.

The recording-controls candidate reserves a fixed Stop/Cancel row below
scrollable explanatory text inside the existing host. Stop appears after
microphone proof and disables on its first tap; Cancel stays available during
preparation, recording and processing. This addresses the reported clipped Stop
button. On A35, Andrew confirmed both controls fully visible and direct recording
Cancel working; own state confirmed microphone cleanup and no draft/commit.
Broader geometry, menu styling and recording-state prominence remain deferred.

## Credential flow

Open KoScribe -> API key settings. User privately enters a key on the phone;
never put a real key in chat, a terminal, a build flag, source, Git or test data.
Prefer a dedicated key. Save encrypts with an Android Keystore AES-GCM key and
checks the persisted result before success. Reopening shows only saved/missing/
unusable state, never the key. Delete removes the encrypted entry and alias.
An unusable existing entry requires deliberate deletion rather than silent reset.

The screen is secure, masked, excluded from recents, and opts out of saved input,
autofill and content capture. Ciphertext lives in noBackupFilesDir; manifest plus
cloud/device-transfer/legacy backup rules exclude all applicable domains. These
are source controls; actual OEM migration behavior is not claimed tested.
Hardware backing is used where supported and must not be promised universally.

When a real key is created/changed or its backup is unconfirmed, remind Andrew to
back it up in Cryptomator DeveloperStuff. Only non-secret purpose/confirmed backup
status belongs in MindHub's portable-secrets checklist. Andrew confirmed private
key entry and backup on 2026-10-24, after implementation/setup checks. No agent
inspected the credential. Budget alerts are not a hard spending cap.

## Audio and requests

Audio is captured only for an explicit recording and kept in bounded memory:
mono16kHz/16-bit PCM, maximum60s, then WAV. Limit/read failure/cancellation discards
it. Only successful explicit Stop, microphone release and reader completion can
hand audio to transcription. A partial/overlimit capture must never upload.

POST only to `https://api.openai.com/v1/audio/transcriptions`, using
`gpt-transcribe`, multipart WAV, JSON response and English language hint. Normal
TLS verification, no redirects/log interceptors or automatic replay. OkHttp 4.12
uses a 45-second call limit, 10-second connect limit and 20-second read/write
limits. The outer multipart body is one-shot, with connection retries and both
redirect options disabled. Cancel suppresses late
results and cancels the request; already transmitted bytes cannot be recalled.
No chat history, field context, screenshots or contacts are sent.

Responses are bounded and validated before review. Empty/malformed/overlong
transcripts and provider/network failures show sanitized errors, not raw response
bodies or exception details. Local audio/transcript history is not retained;
this does not assert zero provider retention. The first deliberate transcription
and later SwiftKey-edited insertion passed on A35. Broader accuracy, error recovery,
latency/cost and Phase 2 acceptance remain subject to the gates in PROGRESS.

## First device checks

Root alone operates ADB. Run exact opt-in methods separately, inspect every
result, and never run a whole smoke class/suite. No real messaging-app test scope
from Phase1 carries forward automatically.

Credential test (no real key, no network):

```powershell
adb -d shell am instrument --user 0 -w -r -e class 'com.koscribe.smoke.CredentialFlowSmokeTest#fakeKeyCanBeSavedWithoutReadbackThenDeleted' -e credentialFakeOnlyReady true com.koscribe.smoke/com.koscribe.smoke.KoScribeTestRunner
```

It requires an empty credential store, refusing usable or unusable preexisting
entries. It saves a static fake marker, verifies blank masked entry and saved
status after reopening, deletes it, then verifies empty state. Failure cleanup
can delete only the entry created after that empty-store precondition. Never
record/Stop while the fake marker is saved: it is not an API credential.

Missing-key refusal (empty store required; no network or insertion):

```powershell
adb -d shell am instrument --user 0 -w -r -e class 'com.koscribe.smoke.SpeechSetupSmokeTest#missingKeyRefusesBeforeRecording' -e speechMissingKeyReady true com.koscribe.smoke/com.koscribe.smoke.KoScribeTestRunner
```

This opens the owned synthetic editor with SwiftKey, selects EN, and requires
the missing-key notice with unchanged microphone-proof/destination/commit
counters, no review draft, and no pending microphone cleanup. It never supplies
a key, records a deliberate sample, or writes to a destination.

The live personal key is now saved. Do not run either credential setup smoke
method against it; retain those methods only for an explicitly empty test store.

After no-key safeguards and credential cleanup pass, ask Andrew to enter a real
key privately, then dictate a deliberate test sentence in the synthetic editor.
The first real API call must follow his explicit recording/Stop. Do not upload
ambient recordings, stale buffers or synthetic tests automatically.
