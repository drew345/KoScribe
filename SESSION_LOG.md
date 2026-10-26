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

## 2026-10-23 — Phase 1 implementation; awaiting A35

- Andrew authorized Phase 1 and pointed to MindHub's PhoneCare subproject.
  Followed the MindHub migration pointer to the shared authoritative files;
  reused PhoneCare's ADB/SDK/JDK tools. No phone-care files were changed.
- Prepared standard API 36 / Build Tools 36.0.0 SDK paths by copying existing
  tools; verified and installed official Gradle 8.13 and its pinned wrapper.
- Sol 6 Extra High implemented the Kotlin feasibility app: three-mode bubble,
  visible microphone screen, editable fixed English review, Korean samples,
  guarded one-attempt insertion, and explicit Copy recovery. No API/key/network
  integration; microphone audio is discarded.
- Astra Extra High independently reviewed source. Sol fixed microphone
  permission handling, input/window binding, navigation continuity, composing
  state evidence, cancellation ownership, and recording-screen task affinity.
  Unknown composition remains explicitly unknown; the platform's accessibility
  commit behavior supports this controlled prototype policy, pending A35 tests.
- Final debug build and seven unit tests passed. Lint has 0 errors/18 warnings
  (prototype UI localization and Gradle update notice). APK signature verified;
  packaged permission list contains only RECORD_AUDIO. No release key created.
- Astra cleared the source for controlled local-editor testing, not real-app
  compatibility. Added `docs/PHASE1.md` and an unmeasured device acceptance
  matrix. Phase 2 is still gated on real-device results.
- Requested USB connection when build/review completed. ADB starts correctly
  and currently lists no device. No installation or phone inspection occurred.
- Staged prototype secret-pattern scan passed; credential/audio/APK ignore rules
  verified. GitHub visibility reconfirmed PRIVATE before publication.

## 2026-10-23 — A35 tests and compact panel

- Installed in main user 0 after Andrew connected USB and granted microphone
  and accessibility access. Verified SwiftKey selection and Google Messages as
  the default SMS app; preserved Wispr and other profiles.
- Measured microphone start, Stop/Cancel cleanup, active-composition refusal,
  and preservation of the local draft after refusal. No sample insertion is
  confirmed yet; no real-app draft test or API call has occurred.
- Code-only diagnostics identified the empty TextView caret omission. After
  automatic review rejected an earlier diagnostic-scope change, Andrew
  explicitly approved the narrow independently verified empty-field exception.
  Sol implemented it; Astra reviewed it. Empty-field recording now starts.
- Full-screen review caused conservative navigation guards to refuse return.
  Andrew authorized a compact panel; it avoided those events in one observed
  return, but the session expired before confirmation. Guards remain intact.
- Compact-panel build and 10 unit tests passed; lint has 0 errors/14 warnings.
  An app-targeted instrumentation attempt failed all three cases in setup by
  restarting the product process and disconnecting its accessibility service.
  Requested Andrew's visible accessibility toggle and a separate self-targeted
  smoke runner. Current next action is maintained in `PROGRESS.md`.
- Andrew restored the service; the self-targeted runner preserves it. The
  keyboard-hidden diagnostic then inserted the exact English sample once,
  confirmed by field assertion, code-only commit trace, and screenshot.
  Normal keyboard-visible attempts still refuse an ancestor ScrollView subtree
  event. The keyboard visibility setting experiment had no effect and was
  reverted. Astra reviewed the test changes and recommended improving the
  synthetic editor layout while retaining every insertion guard.

## 2026-10-23 — Cross-app synthetic insertion and review editing

- Added a minimal separate-process synthetic editor and self-targeted runner,
  preserving the product accessibility service and SwiftKey. Removed the white
  surround from the collapsed bubble; final icon design is deferred.
- Andrew offered Astra Ultra for difficult implementation/diagnosis. Used it
  for the modal-window correction and independent review; Sol added scoped
  test cases. The first phase-bound, one-pair rule was source-reviewed, built,
  and tested, with 23 passing unit tests and zero lint errors.
- Normal SwiftKey-visible empty insertion passed exactly once. Explicit
  review focus then enabled exact middle insertion with both sides preserved;
  an uncontended repeat also passed. Selected-range refusal, Cancel, and
  leaving-review preservation passed. Real messaging-app compatibility remains
  untested; no API call or credential retrieval occurred.
- Explicit editing inside the English review still triggered repeated modal
  window visibility changes. Fixed-height and dialog-movement-animation
  experiments were ineffective and reverted. Current work is replacing
  brittle phase/count assumptions with independently reviewed continuous modal
  ownership and fresh field-identity verification for each visibility return.
- Andrew used Messages/Kakao briefly during tests, then finished; subsequent
  uncontended edited-review failure confirms the remaining issue is not solely
  user interference. An unintended post-test screenshot was immediately
  deleted; use exact fixture assertions/code-only traces for further testing.
- Keep the same-view/unchanged-field hidden-navigation limitation explicit.
  Phase 1 acceptance and Phase 2 remain gated. Current result and next action
  are maintained in `PROGRESS.md`; no new publish is recorded yet.
- The narrowed continuous-modal policy passed automatic review after an initial
  broader focus-helper patch was rejected. Astra independently cleared the
  final code; build, 27 unit tests, lint, and APK signature checks passed.
- After Andrew unlocked the phone, all eight normal synthetic cases passed on
  the final guard, including edited-English return, both Korean modes, Unicode
  preservation, and three refusal/cancellation checks. No repeated insertion;
  microphone stopped and SwiftKey remained selected. Requested a prepared
  KakaoTalk self-chat composer for the first real-app unsent-draft test.

## 2026-10-23 — Permission audit and external-test boundary

- Andrew confirmed the prepared KakaoTalk self-chat. Automatic approval review
  nevertheless rejected adding the opt-in external draft test twice, citing
  real-app interaction restrictions. No test file or Kakao draft was changed.
  Saved the non-executable scope in `docs/EXTERNAL_DRAFT_TEST_PROPOSAL.md`;
  explicit scoped approval remains outstanding. This is not an Android grant.
- At Andrew's request, checked current official Wispr/DictoKey setup guidance,
  Android/Samsung documentation, and installed permission metadata. Microphone
  and accessibility are enabled for KoScribe; its accessibility overlay works
  without a separate overlay grant. Wispr/DictoKey have notifications, overlay
  access, and Doze exemptions; KoScribe has no exemption. Samsung sleep lists
  and idle/reboot reliability remain unmeasured. No settings were changed.
- Expanded `docs/RESEARCH.md` with required, optional, conditional, and future
  permissions; added background/recovery checks to device acceptance. The
  fixture-only keep-screen-on update passed smoke assembly/lint after the eight
  device passes; that fixture update is not installed and product APK is unchanged.
- Astra Extra High independently checked the permission audit. Verified the
  GitHub repository is private; staged-path/secret-pattern checks passed.
  Published the reviewed source, synthetic tests, and documentation as
  `76fd0b4`. Requested explicit approval of the narrowly scoped unsent self-chat
  exception; no rejected real-app action was retried or rerouted.
- Andrew answered the subsequent explicit exception request: "Approve this one
  unsent self-chat test." Sol resumed the single opt-in test implementation
  under that new authorization; independent Astra review precedes execution.
- Sol added the opt-in Kakao test; Astra reviewed both initial and corrected
  versions. Two preflights stopped before product actions: node-only 0/0 was
  stricter than the approved product empty-caret policy, then SwiftKey was
  hidden. After Andrew restored it, the flow reached confirmation but refused
  on another target node's content-change event. The focused draft stayed empty,
  no commit was requested for that session, microphone stopped, and SwiftKey
  remained selected. No retry after confirmation. Product guards unchanged;
  further diagnosis must use code-only metadata within the privacy scope.
- Prepared a diagnostic-only update: relative uptime around capture and the
  first content event, cached panel state, and fixed source categories. No new
  cross-app inspection or guard change. Added a mutually exclusive recording/
  Stop/Cancel test with no confirmation path. Astra review, both builds, 27
  unit tests, lint (0 errors; app 15/smoke 3 warnings), and APK signature checks
  passed. New diagnostic APKs are not installed or run; further real-app
  diagnostic authorization is the next step.

## 2026-10-23 — Scoped recording/Cancel diagnostics

- Andrew authorized continued scoped KakaoTalk recording/Stop/Cancel diagnostics
  without insertion or sending. Installed the reviewed timing build and test
  APK; the accessibility service reconnected. He restored the empty self-chat
  field and visible SwiftKey after installation.
- First diagnostic completed recording/Stop/Cancel but its immediate keyboard
  return assertion failed. A later system-state check showed SwiftKey visible.
  Microphone stopped after 2.567 seconds; no confirmation or commit was invoked.
- This run's first invalidation was TARGET_WINDOW_REAPPEARED: Android announced
  the captured window added while the panel owned focus, but that window was
  absent from the current window list. The diagnostic label ROOT_ABSENT does
  not mean a present window had a null root. No content event was recorded
  before invalidation; later content events are not covered by that statement.
- Sol added a bounded four-second post-Cancel wait for SwiftKey; Astra reviewed
  it and smoke assembly/lint passed. Two complete diagnostic runs then passed
  original empty-field identity/preservation and keyboard-return assertions.
  An intervening attempt refused before recording started. All three sessions
  first invalidated on another target node's content-description event (flag 4),
  timestamped 27/35/46 ms after editor capture completed. The event's role and
  harmlessness remain unknown. No insertion, confirmation, or Send occurred.
- Sol added own-panel lifecycle milestone timing; Astra reviewed the exact
  source. Both builds, 27 unit tests, lint, and APK signature checks passed.
  Installed product SHA-256 starts BC464039. One early startup refusal followed
  by a complete recording/Stop/Cancel pass showed the event after launch request
  but before accepted bind (latest: 26 ms after launch, 25 ms before bind).
  Event delivery was delayed 105 ms. This explains why delivery-time recording
  state cannot identify occurrence-time state; it does not establish cause.
- Corrected-test total: three complete passes and two early recording-start
  refusals. Latest microphone use ended after 2.634 s; SwiftKey remains selected
  and visible. Original draft stayed empty on every completed assertion flow.
  No guards or permissions changed. Same repeated timing checks are exhausted;
  next work should assess safe startup/capture or explicit recovery design.

## 2026-10-23 — Bubble / panel startup isolation

- Andrew authorized isolating the bubble and recording-panel startup steps.
  Sol implemented compile-time debug HOLD/COLLAPSE/PANEL probes using the same
  controls and original capture; OFF/default/release retains ordinary behavior.
  Every probe blocks insertion/Copy and has bounded cleanup. Astra independently
  reviewed source, flag gating, preservation assertions, and result interpretation.
- Full normal build, 27 JVM tests, lint (zero errors; app 15/smoke 3 warnings),
  three arm builds, and signatures passed. Separate APK copies prevented arm
  confusion. No new phone permission or user preparation was needed.
- HOLD and COLLAPSE each passed with no invalidation during their 1.5-second
  observations. PANEL reproduced the same flag-4 OTHER_NODE refusal before
  binding/recording: event 105 ms after launch, delivery 5 ms later. All three
  verified the same original empty field and SwiftKey after cleanup; no new
  microphone use, insertion, sending, or clipboard operation occurred.
- Restored the exact prior product APK BC464039..., confirmed service reconnect,
  stopped microphone, and visible selected SwiftKey. Reset normal build output
  to OFF. Evidence implicates the panel-launch transition in this comparison,
  without identifying the other node or proving its change harmless. Next is
  lifecycle/recovery design evaluation; keep all insertion guards unchanged.

## 2026-10-23 — Nonfocusable recording and explicit destination selection

- Andrew authorized fixing startup with no-insertion testing. Sol implemented
  a service-owned nonfocusable recording panel; Astra reviewed microphone proof,
  cleanup ownership, terminal guards, opt-in tests, and focused-only retrieval.
- A second overlay window refused on target child-window flag512. Reusing the
  existing expanded bubble then reached positive unsilenced microphone reads.
  Direct Cancel passed; one run executed Stop but review still cancelled.
- Bounded window metadata snapshots matched, while final event timing proved a
  fresh event 2.625s after capture, before Stop, delivered 5ms later. Cause and
  harmlessness remain unknown. No whitelist/grace/recapture was introduced.
- Every cleanup check preserved the empty Kakao field and SwiftKey. Mic stopped;
  no insertion, sending, clipboard, chat traversal, API, or audio retention.
  Existing27 unit tests, builds/lint/signatures passed; precise results/hashes
  are in DEVICE_TESTS. Capture-only E5CD28... APK remains installed; normal
  local build output is OFF. GitHub remains private.
- Andrew selected the new interaction: finish review, tap intended field, then
  Insert. Recording/review will be independent of a destination; explicit Insert
  later creates a fresh guarded transaction. Root/Sol/Astra agreed staged
  RESELECT implementation and non-inserting tests. No early editor capture,
  no inherited invalidated eligibility, and no actual insertion authorization
  implied by this workflow choice. README/AGENTS/IMPLEMENTATION updated.

## 2026-10-23 — Overnight Stage 1 checkpoint

- Andrew asked to stop at the next good checkpoint and document tomorrow's
  restart. Completed only RESELECT Stage 1; no editable-review Stage 2 work.
- Sol 6 Extra High implemented independent recording ownership, callback gates,
  lifecycle cleanup, and a 15-second deadline. Astra Extra High reviewed the
  exact source and required removal of passive caret/composition processing
  in RESELECT; that correction was made and cleared before installation.
- Candidate and default OFF builds, 29 JVM tests, both lints (zero errors;
  app 18/smoke 3 warnings), and APK signatures passed. Andrew restored the
  empty Kakao self-chat and SwiftKey for one exact non-inserting phone gate.
- Gate passed in 6.652 s: own positive unsilenced reads, >3.2s recording after
  proof, Stop, readonly fixed sample, Cancel. Destination captures stayed zero;
  original empty focused field and visible SwiftKey checks passed throughout
  and after cleanup. Mic ended after 4.176 s; cleanup pending false.
- No insertion, Copy, sending, API call, retained recording, permission change,
  conversation traversal, or screenshot. Stage 1 is a bounded microphone/UI
  feasibility result, not ASR, editable review, or insertion acceptance.
- Installed RESELECT product C0CC7F44... and smoke 9130F582... are saved under
  ignored app/build/reselect-stage1. Ordinary local APK output rebuilt as OFF.
  Full hashes and review evidence are in DEVICE_TESTS; exact gate in
  INSTRUMENTATION. PROGRESS is the authoritative restart note.
- Tomorrow: independently owned editable English review after microphone Stop,
  then ready controls. Later explicit destination check must restore full
  phase-aware guards. Actual external insertion remains outside current scope.

## 2026-10-24 — Contextual draggable bubble

- Andrew prioritized a keyboard-triggered, draggable bubble with saved position
  and dismissal after Copy/insertion. He specified a launcher app with English
  review, 요/반말 translation, and Copy & close as the minimum useful fallback.
- Sol 6 Extra High implemented the idle policy and tests; Astra Ultra handled
  difficult geometry, with independent Astra Ultra review. RESELECT idle
  visibility uses metadata only, never destination capture. Active recording
  retains its host; drag saves a normalized anchor. Menu rows are now compact.
- Three exact synthetic A35 gates passed: visibility/Hide/drag/password exclusion
  (4.889s), position after confirmed service restart (1.883s), and same recording
  host through keyboard hide/show -> Stop -> readonly sample -> Cancel (4.105s).
  Test capitalization and asynchronous expansion errors were corrected and
  reviewed; the final correction changed only the smoke test.
- Final state: one positive unsilenced recording proof, zero destination captures,
  no cleanup pending. AppOps confirmed mic ended after 984ms. SwiftKey remains
  selected, Wispr remains enabled. No insertion, Copy, sending, real-chat reads,
  API calls, audio retention, screenshots, or permission changes occurred.
- Candidate/default builds, 32 JVM tests, lints (zero errors; app 20/smoke 3
  warnings), and signatures passed. Installed product 7568BD4... and smoke 598F127...
  are saved under ignored app/build/bubble-lifecycle. Ordinary output is OFF.
- Copy/Insert dismissal, editable review, current-build real-app acceptance,
  broader geometry/reliability, and the fallback remain unimplemented or untested.
  Next is independently owned editable review/result handling with completion
  dismissal, keeping the fallback practical. Phase 1 acceptance remains open.

## 2026-10-24 — Editable review and Copy checkpoint

- Sol 6 Extra High implemented REVIEW: independent editable English review,
  explicit confirmation, Copy & close, and fresh one-shot guarded Insert with
  exact postcommit confirmation. Astra Extra High independently reviewed it.
  Default OFF and recording-only RESELECT remain. No speech/API integration.
- Candidate/default builds, 37 unit tests, lints (zero errors; app 23/smoke 3
  warnings), and signatures passed. Installed product 5387AA79... and smoke
  337DE462... are saved under ignored app/build/review-stage2; ordinary output OFF.
- Fixture startup race corrected in the test only. Subsequent no-focus timeouts
  were traced to the notification shade covering a drawn fixture. Closing it
  resolved startup; no notification/chat content was read or guard changed.
- Editable EN -> Copy passed in 6.454s: original draft preserved, sensitive clip
  verified, no destination captures/commits, bubble dismissed until the next
  keyboard cycle. Mic ended after 450ms. Actual SwiftKey keypresses remain untested.
- Empty Insert observed the expected sample but failed confirmation in 12.098s:
  one capture/commit; accessibility node still reported unknown caret/empty-hint
  while connection was no longer verified empty-at-zero. Terminal UNCERTAIN;
  no retry. Test Cancel cleared draft; cleanup false, mic ended after 473ms.
- Andrew requested a checkpoint for his LAPTOP battery at 10%. Tests/builds are
  stopped. Next: narrowly diagnose postcommit caret timing with code-only
  synthetic observations, preserving terminal uncertainty. Other five REVIEW
  gates, confirmed insertion dismissal, and real-app acceptance remain pending.
  Exact evidence/source/APK hashes are in DEVICE_TESTS; restart from PROGRESS.

## 2026-10-24 — Confirmation fixed in synthetic REVIEW tests

- Resumed at Andrew's request. Sol 6 Extra High implemented a bounded diagnostic,
  focused-node refresh, and fixed ready-control footer; Astra Extra High reviewed
  each change. Root alone operated the phone. No API or real-app insertion.
- Diagnostic proved stale focused-node metadata: connection already contained
  expected text/caret; refreshing the same node supplied matching values. The
  diagnosed transaction stayed UNCERTAIN, with one commit and no retry/rearm.
- Final fix preserves all identity/context/selection/event and one-attempt checks.
  Seven exact synthetic REVIEW methods passed on product 2B00BE44... across smoke
  versions: Copy, empty/middle/Korean confirmed insertion, selection refusal,
  Korean-edit rejection/Cancel, and terminal ignored-commit uncertainty. The last
  used WINDOW_OR_FOCUS_CHANGED; settled timeout branch was not device-proven.
- A call caused one silenced startup; retry after Andrew finished passed. Other
  SYSTEM-window focus interruptions remain unattributed. A test-only Cancel
  reveal fix addressed an offscreen button and strengthened closure assertions.
- Repeated Proton Pass prompts required no password. REVIEW fixtures now omit
  their unused empty password field after autofill opt-out alone proved insufficient.
  The authorized exact negative-button helper returned NOT_FOUND, so no click;
  no global/provider settings changed. Prompt cessation remains unverified.
- Final product/smoke saved in ignored app/build/confirmation-refresh. Final smoke
  FC0A13E5...; exact hashes/source reviews/results in DEVICE_TESTS. Default output
  stays OFF. Builds/lints/signatures and 37 unit tests passed.
- Final own draft NONE, cleanup false; last mic use ended after 516ms. No test remains
  running; Andrew may disconnect. Checkpoint preserves pending manual SwiftKey
  keystrokes, current real-app acceptance, and broader lifecycle/geometry gates.
  Phase 1 remains open; next actions are in PROGRESS.

## 2026-10-24 — Manual SwiftKey gate prepared

- Andrew requested continued Phase 1 work. Added only a debug smoke launcher,
  "KoScribe test editor," to open the internal synthetic fixture with no password
  field. It ignores incoming data and performs no recording/text/product action.
  Sol 6 Extra High implemented; Astra Extra High independently cleared both files.
- Smoke build/lint/signature passed (zero lint errors, three existing warnings).
  APK 5D9D7CC7... saved under ignored app/build/manual-swiftkey and installed.
  Product remains 2B00BE44... . Launcher opened successfully and own synthetic
  foreground was confirmed by metadata. No automated recording/insertion ran.
- Andrew was asked to edit the fixed review sample with actual SwiftKey keys,
  then explicitly Insert once into the top synthetic field. Awaiting his report
  and post-action own-state check; no manual typing pass is claimed yet.

## 2026-10-24 — Compact result controls and visual testing

- Manual SwiftKey edit/confirmation reached the ready result, but the large panel
  obscured the destination. Andrew tapped Paste separately, then Insert refused
  ACTIVE_COMPOSITION before a product commit. Draft expired; no manual pass.
- Sol 6 Extra High implemented compact draggable result controls and concise
  refusal feedback; Astra Extra High reviewed without weakening insertion checks.
  Secure candidate 723D5D20... built/linted; 37 unit tests passed. Its first empty
  insertion check placed exact text but ended UNCERTAIN on postcommit composing
  spans (13.149s). No retry; remaining candidate regression gates stay open.
- Andrew requested seeing the screen during experiments. Added explicit default-off
  debug REVIEW visual mode; normal/release remain protected. Visual candidate
  C3A713C0... installed only for synthetic USB checkpoint screenshots. No mirror
  software, continuous recording, or device privacy-setting change was needed.
- Ready screenshot confirmed all controls visible at font scale 1.15, but initial
  placement still overlapped the upper field. Zero captures/commits. Incoming call
  interrupted before drag proof; screenshots stopped and the temporary image was
  deleted. Old draft ended by deadline. Andrew then requested a fresh attempt.
- Pending: his restarted drag-and-select report, screenshot verification, cleanup,
  and restoration of the saved secure REVIEW APK. Do not take further screenshots
  until he replies. No real-app screenshots/insertion. Ordinary build output has
  been restored to OFF/visual false; product test limitations are in DEVICE_TESTS.

## 2026-10-24 — Visual drag report and bounded composition diagnosis

- Andrew reported moving the compact result by its handle. Two fixture fields
  and an empty-field hint caused confusion. The screenshot after his reply was
  already after expiry, which reopened the large menu; no pre-expiry geometry
  pass is inferred. Own counters remained zero captures/commits, draft cleared,
  mic ended after 5.111s. Restored secure723D5D20 APK and removed temporary image.
- Sol is simplifying the manual screen and collapsing expired/cancelled review.
  Astra Extra High reviews the changes. Astra Ultra's read-only analysis found
  composition may coexist with exact inserted text, but this conjunction was
  not measured by the failed product observation. A bounded diagnostic is next;
  no guard, confirmation policy, retry rule, or phase gate is changed.
- Reviewed secure980E4299/smoke9D3F9563 built and installed;37 unit tests/lints/
  signatures passed. Both bounded fresh probes returned NATURAL_CONFIRMED in
  5.370s/4.638s. Failure did not reproduce; no composing-state diagnosis or fix
  claimed. Copy dismissal/reappearance passed5.853s. No real-app insertion.
- Final automated baseline proof3/captures2/commits2, draftNONE, COPIED,
  cleanupfalse; mic ended542ms. New single-field manual screen opened. Awaiting
  Andrew's actual SwiftKey edit/Insert report; do not act while he performs it.
  Ordinary output/release OFF/visual false; installed REVIEW remains secure.

## 2026-10-24 — Clarifying the manual Insert control

- First one-field manual flow reached READY, but Andrew selected Android Paste
  and got the prior synthetic Copy sample. Own proof4/captures2/commits2 showed
  no new KoScribe Insert or Copy. Microphone ended4.639s; draft later expired.
- Andrew requested another try after clarifying he had not looked for Insert
  inside the changed floating box. Explained its two rows, reset only the
  synthetic fixture after expiry, and reopened the empty field. Awaiting the
  fresh manual result. Actual control visibility in the prior flow is unmeasured;
  no insertion pass/failure or UI clipping diagnosis is claimed. No source change.

## 2026-10-24 — Manual SwiftKey insertion passed

- Andrew found the floating Insert button, reported his edited sentence appeared,
  and saw the box disappear. Own state confirmed one new capture/commit (2/2 ->
  3/3), CONFIRMED, draftNONE, cleanupfalse; proof5, mic ended5.318s. The manual
  synthetic EN edit/Insert gate now passes on secure980E4299/smoke9D3F9563.
- No source/build change or additional phone mutation. Earlier intermittent
  composition failure and real-app acceptance remain open; Phase1 stays gated.
- Andrew proposed omitting the field tap when the intended cursor is already
  there. Record it as a separate untested variant with fresh capture at Insert
  and unchanged guards, not a claimed capability from this successful run.

## 2026-10-24 — Restored-focus manual test prepared

- Andrew authorized testing without the extra field tap. Verified connected,
  unlocked A35, SwiftKey selected, no active draft; baseline proof5/captures3/
  commits3, CONFIRMED, cleanupfalse. Reset only the synthetic fixture and opened
  its empty manual field. Product/smoke remain unchanged and secure.
- Asked him to edit/confirm as before, check the restored intended cursor without
  touching the field, and tap floating Insert once. Stop if cursor absent or
  insertion refuses; no retry. Await his report before further phone actions.

## 2026-10-24 — Restored-focus manual insertion passed

- Andrew reported edited text insertion and dismissal without retapping the
  destination after Confirm. Own state confirmed proof6, one new capture/commit
  (3/3 -> 4/4), CONFIRMED, draftNONE, cleanupfalse; mic ended3.415s. Same secure
  product/smoke; no source/build change. Astra/xhigh verified the evidence scope.
- Instructions now condition the field tap on needing to select/reposition the
  intended cursor. Insert still captures afresh with every guard unchanged.
  This accepts the empty synthetic case; other apps and the earlier intermittent
  composition failure remain unaccepted/unresolved. No further user action pending.

## 2026-10-24 — Current REVIEW Kakao check prepared

- Andrew asked to proceed toward KakaoTalk or speech input. Selected current-build
  Kakao self-chat recording/Stop/Cancel before Phase2. Existing non-inserting
  authorization applies; later insertion scope is still separate.
- Confirmed secure REVIEW connected/unlocked, SwiftKey selected, baseline proof6/
  captures4/commits4, CONFIRMED, draftNONE, cleanupfalse. Asked Andrew to open an
  empty self-chat field, Ko -> EN -> Stop after3s -> Cancel in review and report
  empty-field/keyboard return. Awaiting report; no screenshot/chat read, code,
  build, extra permission or API work. Astra/xhigh reviews bounded next gates.

## 2026-10-24 — Kakao cancellation passed; composition evidence captured

- User reported fixed review -> Cancel -> Kakao cursor/SwiftKey return. Own
  proof7, unchanged captures/commits4/4, REVIEW_CANCELLED, draftNONE, cleanupfalse;
  mic ended5.265s. Clarified microphone capture/discard versus unimplemented ASR.
- First remaining current-build regression (middle preservation) failed12.853s
  at confirmation after exact text passed. Existing diagnostic measured complete
  identity/context/caret agreement at29ms with span115..121, persisting through
  557-572ms. No retry; terminal uncertainty then Cancel. Final proof8/captures5/
  commits5, draftNONE, cleanupfalse; mic ended624ms. No real-app insertion.
- Stopped remaining four methods and held Kakao insertion. Astra Ultra is
  assessing the newly measured evidence for a concrete prospective confirmation
  policy versus Copy fallback decision. No guard/code/build change yet.
- Astra Ultra recommended exact-result postcommit confirmation while keeping
  precommit composition refusal. Andrew explicitly approved. Sol6/xhigh product/
  unit and deterministic smoke work is underway, with Astra/xhigh independent
  review. No future Kakao insertion authorized by this approval; tests are pending.

## 2026-10-24 — Approved confirmation change passed scoped gates

- Sol6/xhigh implemented the approved distinction; Astra/xhigh cleared source
  and deterministic fixtures. All precommit and one-attempt guards remain.
  Exact composed postcontext may confirm; composed mismatch ends immediately.
- Secure3AA18446/smoke168091D7 installed. All40 unit tests and seven separate
  device checks passed: composed exact/mismatch, middle preservation, Korean,
  selection refusal, Korean-edit Cancel, unchanged-commit uncertainty. The final
  negative reached POSTCONTEXT_UNCONFIRMED; no retry. See DEVICE_TESTS for timings.
- Final proof7/captures6/commits5, draftNONE, UNCERTAIN from the negative,
  cleanupfalse; mic ended576ms, SwiftKey selected. No real-app insertion or read,
  screenshot, credentials or API work. Default/release OFF and secure REVIEW kept.
- Astra cleared proposing one newly authorized unsent Korean self-chat trial.
  Ask for that specific scope next; the prior trial is closed. Phase1 remains open.

## 2026-10-24 — New unsent Kakao trial approved and prepared

- Andrew approved exactly one new manual fixed Korean insertion into empty
  self-chat. Verified connected/unlocked, secure REVIEW unchanged, SwiftKey
  selected, baseline proof7/captures6/commits5, draftNONE/cleanupfalse, mic stopped.
- Sent 요 -> Stop -> unchanged English confirmation -> check intended caret ->
  floating Insert once. Await user report; no retries, Send, Copy/Paste, or
  conversation inspection. No code/build change or phone mutation this turn.

## 2026-10-24 — Current secure REVIEW Kakao insertion passed

- Andrew reported the approved Korean sample appeared exactly, the flow worked
  as instructed, and it remains unsent. Own proof7 ->8, captures6 ->7, commits5 ->6;
  current CONFIRMED/REVIEW_COMPLETED, draftNONE/cleanupfalse. Mic ended5.450s,
  SwiftKey selected. No screenshot, conversation read, or agent phone mutation.
- This accepts one empty Kakao self-chat insertion on3AA18446 and consumes the
  one-trial scope. User can remove only that unsent sample manually. Other app
  coverage and Phase2 remain pending; no code/build changes this turn.

## 2026-10-24 — Google Messages and Instagram trials approved

- Andrew chose to finish the other app checks before speech implementation,
  approving one unsent fixed-sample insertion per app, no retries or sending.
- Read-only own baseline unchanged: proof8/captures7/commits6, CONFIRMED,
  draftNONE/cleanupfalse, mic stopped, SwiftKey selected. Sent Google Messages
  procedure first; await report before further phone actions. Instagram not
  started. No app navigation, conversation read, code/build or phone mutation.
- Astra/xhigh is checking prior focus-change evidence against the written gate
  to avoid adding redundant tests before Phase2. No new testing requirement set.

## 2026-10-24 — Google Messages passed; Instagram pending

- User reported expected insertion/flow in Google Messages, addressed to himself.
  Whether he personally sent it was unclear; no Send by agent/app. Own proof9/
  captures8/commits7 (each +1), CONFIRMED, draftNONE/cleanupfalse, mic ended5.100s,
  SwiftKey selected. Scope consumed. No conversation reads or phone mutation.
- Sent already-approved Instagram procedure with baseline9/8/7; await report
  before phone actions. Same installed secure product/smoke, no code/build change.
- Astra's read-only audit found prior REVIEW window/focus termination evidence,
  passing current guard units, and preserved original-target/event checks. No
  extra mandatory target-change fixture; finish Instagram then decide Phase1.
  Same-view switch-back and broader lifecycle cases remain untested limits.

## 2026-10-24 — Instagram passed; Phase2 begins

- User reported expected Korean in Instagram DM and flow worked perfectly.
  Own proof10/captures9/commits8 (each+1), CONFIRMED, draftNONE/cleanupfalse,
  mic ended3.087s; SwiftKey selected. No chat inspection or phone mutation.
- All three named app flows passed. Combined with synthetic/unit/source evidence,
  the bounded Phase1 gate is met. Other lifecycle/geometry/navigation limits
  remain explicit, not silently accepted. One-attempt app scopes are consumed.
- Official OpenAI docs reconfirm gpt-transcribe recorded-file transcription.
  Sol6/xhigh credential and speech integration agents started, Astra/xhigh review;
  secure on-phone key entry remains agreed. Opt-in speech debug candidate,
  unchanged sample baseline, no actual key/audio upload/paid test yet.

## 2026-10-24 — Speech candidate ready for private key setup

- Sol6/xhigh implemented EN transcription and Keystore-backed private key entry;
  Astra/xhigh reviewed security, cancellation and cleanup. Explicit debug-only
  speech flag; ordinary/release remain OFF. Korean translation is still Phase 3.
- Product v3 F2079E8A built/signed, all 53 units passed, lint zero errors (37 app,
  3 smoke warnings). Fake key save/reopen/delete passed on v2 in1.123s, with empty
  store verified. No actual key or paid request was used.
- Missing-key warning initially lost its host/token to a precheck redraw. The
  reviewed minimal fix removed that redraw and retained every guard. Three v3
  runs passed notice/no-microphone/no-destination assertions, but the full methods
  failed later on automated Close cleanup. Final v4 smoke2782686D failed bounded
  own-overlay scrolling; actual Close remains unverified. Stop cleanup retries.
- Final own proof/captures/commits0, draftNONE/cleanupfalse; mic still stopped
  since Instagram. Reviewed evidence supports private key setup, not Phase 2
  acceptance. Next ask Andrew to enter a dedicated key privately on the phone,
  back it up in Cryptomator DeveloperStuff, then run deliberate synthetic speech.
  No screenshots/key reads, unattended recording/upload, or new real-app trial.

## 2026-10-24 — Private key and backup confirmed; first speech test prepared

- Andrew reported "Key saved; backup done." Recorded only purpose and confirmed
  status in shared MindHub; no credential or vault contents inspected. Preserve
  the live key and do not rerun fake-key/deletion or missing-key setup methods.
- Phone unlocked; own baseline proof/captures/commits0, draftNONE/cleanupfalse,
  mic stopped, SwiftKey selected. Launched owned single-field Test message.
- Asked Andrew for a short deliberate EN recording -> Stop -> compare actual
  English -> review Cancel. No Confirm/Insert/Copy/Send or retry; await report.
  Actual account access/transcription result is still unverified.

## 2026-10-24 — First real speech and review Cancel passed

- Andrew confirmed correct transcription and the requested Cancel flow. Own
  proof1, captures/commits0, REVIEW_CANCELLED, draftNONE/cleanupfalse; microphone
  ended29.097s, SwiftKey selected. No key/transcript/chat reads or screenshots.
- Recording status was hard to find after EN began capture. Andrew accepted the
  test and explicitly deferred clearer recording feedback/menu polish to later.
- Same installed secure candidate; no code/build changes. Next: manual synthetic
  real speech -> SwiftKey edit -> Confirm English -> one guarded Insert, then
  verify edited text/dismissal. No retry, Copy/Paste substitute or sending.

## 2026-10-24 — Real speech editing/insertion passed; clipped Stop reported

- Andrew confirmed dictated-and-edited text insertion and dismissal. Own
  REVIEW_COMPLETED, proof3/captures1/commits1, CONFIRMED, draftNONE/cleanupfalse;
  microphone ended4.368s, SwiftKey selected. An intervening review Cancel is in
  the trace; reason unspecified, no extra commit. No content/key reads or screenshots.
- Stop was half clipped but tappable. Sol6/xhigh is fixing only speech-control
  reachability with a pinned Stop/Cancel row; Astra/xhigh reviews it. Stable host,
  all guards, live credential, sample baseline and default OFF are preserved.
  General recording-status/menu polish remains deferred. Device check pending.
- Narrow source fix cleared review and installed as controls v4 52413362.
  All53 units passed; lint zero errors/37 warnings; default/release restored OFF.
  Live key/data preserved. Service idle baseline0/0/0, draftNONE/cleanupfalse.
  Asked for button visibility plus direct recording Cancel (no Stop/upload),
  awaiting user report. No clipping-fix claim until device confirmation.

## 2026-10-24 — Night checkpoint: visible controls and active Cancel passed

- Andrew confirmed both Stop/Cancel fully visible and the requested Cancel flow
  working exactly as described. Own RESELECT_CANCELLED, proof1/captures0/commits0,
  draftNONE/cleanupfalse, no Stop event; microphone ended8.803s, SwiftKey selected.
  No new build, key/content read, screenshot, insertion or Send.
- Stopped for the night at Andrew's request. Installed controls v4 52413362 and
  smoke v4 2782686D retained; default/release OFF. No test or recording running;
  live key is preserved and its DeveloperStuff backup already user-confirmed.
- Tomorrow: verify connection/idle state, then one controlled network failure
  and fresh-recording recovery with Andrew changing connectivity. Review Phase 2
  acceptance before Korean translation. Do not repeat completed app/ASR/edit/
  visibility/Cancel gates or resume automated Close cleanup diagnostics.
- General recording-status prominence/menu polish and broad geometry remain
  deferred. No overnight automation or further phone actions were started.

## 2026-10-25 — Morning resume; service restart requested

- Andrew resumed from night checkpoint0b0889a. USB debugging required a fresh
  approval; Andrew approved and ADB connected. Phone unlocked, SwiftKey selected,
  mic stopped. Own service dump twice timed out, so session/draft state is not
  yet verified. Asked Andrew to toggle only KoScribe accessibility off/on.
- No code/build, key read/change, recording, connectivity change, insertion or
  real-app inspection. Timeout cause remains unknown; avoid guessing an ANR or
  overnight cause. Preserve live credential and all prior measured passes.
- Astra/xhigh independently reviews the controlled offline/fresh-online protocol.
  Pending: restart confirmation, own idle verification, then user-driven network
  failure and fresh speech recovery before Phase 2 acceptance review.
- User's first service toggle left the same process/instance and a third dump
  timeout. Exact own UID/PID verified; one targeted SIGTERM ended that process,
  preserving app data and avoiding force-stop. Own main Activity reopened, but
  service remained not running. Requested one fresh toggle after process exit;
  no recording until responsive idle state. No ANR/main-thread diagnosis inferred.
- The second user toggle reconnected a fresh process with responsive diagnostics:
  NOT_CAPTURED, proof/captures/commits0, draftNONE/cleanupfalse, microphone stopped,
  SwiftKey selected. Opened owned Test message. Sent the reviewed offline sample
  procedure; await terminal error report while the phone remains offline. No
  key manipulation, code/build change, new real-app scope or automated recording.
- Offline trial did not reach recording: Andrew saw SwiftKey/Wispr but no Ko,
  including after one full keyboard hide/reopen. Same fresh process responds
  idle with zero recording/capture/commit counts; microphone inactive. Told him
  to restore connectivity. No API/network failure result is claimed. Astra source
  review identifies visibility metadata versus initial layout as possibilities;
  Sol is adding cached enum/counter diagnostics without changing behavior or
  reading field content. Resolve readiness before resuming the speech gate.
- Diagnostic source cleared Astra/xhigh review after two accuracy corrections;
  build/signature,53 units and lint0 errors/37 warnings passed. Installed v5
  5579B7FA with data/key preserved; default/release flags restored disabled.
  First cached reason FOCUSED_APP_COUNT, inputStarts1, no suppression/host/render;
  recording/capture/commit counts0. Window-type-only evidence currently shows a
  focused system panel. Asked Andrew to close shade/Quick Settings, return to
  Test message and tap its field without recording. Earlier cause remains unknown.
- Andrew reported Ko appeared after returning to the editor (mobile data on).
  Own counters/microphone stayed idle; last cached check again lacked a focused
  application, so do not claim a measured sustained READY state. Reissued offline
  instructions with explicit Quick Settings closure and stop-if-bubble-missing.
  Await exact terminal error before asking him to restore connectivity.
- Offline test PASSED on v5: user reported transcription network failure after
  deliberate recording/Stop. Own proof0->1, RESELECT_CANCELLED with terminal
  SPEECH_NETWORK, draftNONE/captures0/commits0/cleanupfalse; mic ended4.858s,
  SwiftKey unchanged. Cached visibility READY/IME visible/host shown/renderVISIBLE.
  Astra accepted this evidence. Asked Andrew to restore normal connectivity and
  record a distinct fresh sentence, check review, then review Cancel. Pending
  user report and own counters; no earlier readiness root-cause fix is inferred.
- Fresh online recovery PASSED: distinct sentence correctly reviewed, user Cancel
  returned cursor without changing field. Own proof1->2, REVIEW_CANCELLED with
  REVIEW_ENDED_USER_CANCELLED, draftNONE/captures0/commits0/cleanupfalse;
  mic ended5.048s, SwiftKey unchanged, cached bubble READY/shown. These durations
  measure recording, not API latency. Astra/xhigh accepted the bounded Phase2
  gate with prior speech/edit/Insert, setup, control and unit/source evidence.
- Andrew requested a stopping point. Saved checkpoint; no work remains running.
  Phase3 has not been implemented or tested. Resume with confirmed-English
  gpt-6-luna/xhigh translation and the existing credential/insertion contracts.
  No new real-app insertion scope is granted. Preserve unresolved morning
  lifecycle cause and later processing-Cancel/geometry/quality/cost/latency limits.

## 2026-10-25 — Phase3 explicitly resumed

- Andrew asked to continue with 요/반말 Korean translation, keeping safety checks
  and requesting the phone only when needed. Clean baseline619f132; preserve v5.
- Official docs reverified gpt-6-luna/xhigh, Responses structured output and
  retention limitations. Account access awaits deliberate on-phone translation;
  no key read/extraction/change or paid request by the agent.
- Sol6/xhigh client and UI/state agents implement separate file scopes; Astra/xhigh
  independently reviews. New opt-in translation flag, default/release disabled,
  confirmed-English-only requests and unchanged destination/commit safety contract.
  Absolute180s draft lifetime remains; translation90s deadline is subordinate.
- Candidate source cleared by Astra/xhigh. All64 unit tests passed, lint0errors/
  42warnings, signature verified. Saved TRANSLATION-v1.apk (SHA810D9564); ordinary
  debug and release restored/verified OFF/visualfalse/speechfalse/translationfalse.
  Andrew connected/unlocked and approved debugging. Own v5 idle baseline verified;
  installing the candidate as an in-place update. No API call or key read/change.
  Phone translation access/quality/latency/cancellation/recovery remain untested.
- In-place installation succeeded. First new service dump timed out; one later
  check of the same process11530 responded idle with all new counters0, draftNONE,
  stopped mic and cleanupfalse. No restart/toggle needed; cause is unestablished.
  Owned Test message opened. Asked for first deliberate 요 translation, changing
  two to three blue notebooks in English review, then one Insert. Result pending.
- First actual 요/edit/Insert PASSED per Andrew and same-process counters:
  translation1/1/0/0, capture1/commit1 CONFIRMED, draftNONE/cleanupfalse and
  dismissed/suppressed host. Mic stopped, SwiftKey unchanged. Stop-to-review-created
  1761ms; translation job-to-callback3034ms; usage194/0/105/77. This verifies account
  access and one edited-message meaning case, not the complete quality gate.
- Andrew reported Confirm & translate below the screen with SwiftKey. Sol6/xhigh
  implements pinned accessible actions; Astra/xhigh reviews. He then questioned
  whether a Korean preview was useful and asked about one Translate & insert
  action. Unshipped preview patch reverted; service unchanged. Root explained
  explicit destination intent. Andrew initially chose translation checks first,
  then reconsidered because the intended cursor precedes bubble visibility.
  Priority remains under discussion; no automatic insertion change. Pinned-button
  fix is independently cleared because it helps either direction.
- Button-layout v2 built, all64 units passed, lint0errors/36warnings, signature
  verified. Saved/installed TRANSLATION-actions-v2.apk (SHAEEE34599) in-place after
  idle/mic check; default/release flags restored/verified OFF/false. Only
  ReviewActivity code changed: bounded secure review with pinned primary/Cancel,
  scrolling English content. Actual SwiftKey layout check remains pending.
  New process16745 responsive/idle, all counters0/draftNONE/cleanupfalse, mic
  stopped and SwiftKey unchanged. Await priority clarification; no new test begun.

## 2026-10-25 — One-button flow authorized

- Andrew selected Translate & insert with manual fallback if original field and
  cursor cannot be verified, keeping all safety checks; accepted test mode first.
- AstraUltra designs the original-intent/known-review-return gate; Sol6/xhigh
  agents own pure policy/tests and service/UI/build integration. Root retains
  build/device/docs/Git; independent Astra/xhigh review precedes device testing.
- Initial automatic scope is owned fixture only, default/release OFF, with
  API-free synthetic tests before live speech. No old insertion transaction may
  be rearmed; post-attempt uncertainty stays terminal. Installed v2 remains idle.
- Candidate independently source-cleared. Build/all77 units/signatures passed;
  lint0errors (app35/smoke3 warnings). Saved/installed API-free AUTO-sample-v1
  (SHACEE0D1DE) plus smoke-auto-v1 (SHA1A2FC9A3), preserving app data/key.
  Ordinary debug/release restored/verified OFF and all flagsfalse.
- First exact empty fixture test stopped in setup: FIXTURE_NOT_FOCUSED, before
  mode selection. Own PID32396 all proof/intent/capture/commit counters0,
  draftNONE/cleanupfalse, mic inactive and SwiftKey unchanged. New helper's exact
  node-caret requirement differs from old working focus/text helper; known empty
  node -1/-1 reporting is suspected. Narrow test-only correction requires direct
  owned EditText empty/caret0 proof; Astra review before smoke rebuild/rerun.
- Reviewed smoke correction EDBDEFA4 built/signed and installed as smoke-auto-v2
  (SHA60E5E0E9); product unchanged. Same empty test now reached sample recording
  and review, originalcaptured1/proof1, but auto eligibility terminated with
  REVIEW_WINDOW_UNVERIFIED before an accepted own-input callback. No automatic
  attempt/capture/commit; harness cancelled review, mic ended582ms, cleanupfalse.
  Both Astra reviewers recommend fixed-code predicate diagnostics first; no
  timing relaxation or rearming. Sol prepares that narrow diagnostic patch.
- Diagnostic-only v2 independently cleared, built/all77 units/lint0 errors,
  signed and installed (SHADE3A62D0); defaults restored OFF. Fresh empty test
  measured local review focus before remote accessibility registration: no
  focused application windows, own window absent, original input generation.
  Terminal fallback remained unchanged. PID9446 proof1/intent1, no attempts or
  writes, draftNONE/cleanupfalse, mic ended517ms. Narrow bounded initial-readiness
  fix is next; all conflicting metadata and lifecycle invalidators stay terminal.
- Readiness fix14EB76F9 independently cleared: same exact local binding, absent
  remote window/no focus only, fixed1s deadline, terminal contradictions and
  early Confirm, no rearm. Added early-Confirm policy regression; all78 units,
  signature and lint0 errors passed. Saved/installed v3 SHA619C9241, defaults OFF.
  Empty rerun now reaches ENTRY_WAIT then UNEXPECTED_WINDOW_CHANGE. PID13306
  proof1/intent1/fallback1, no writes, draftNONE/cleanupfalse, mic525ms stopped.
  Next diagnostics classify that event before any proposed behavioral change.
- Diagnostic-only v4 source58C392A9 independently cleared; build/78 units/lint0
  errors/signature passed. Saved/installed APK79F9B730; defaults restored OFF.
  Empty run measured original-window REMOVED,20ms into wait, expected own review
  present and uniquely focused. PID16450 no writes, draftNONE/cleanupfalse,
  mic575ms stopped. Next is a separate non-inserting fixture lifecycle trace;
  no removal exception or rearming is authorized by this evidence alone.
- Published reviewed default-OFF experiment and measured failures as d9364ba
  after staged secret scan (zero matches); GitHub verified private. Dedicated
  lifecycle observer C6214251 then independently cleared, built/signed/linted
  (0 errors/3 warnings) and installed as smoke-lifecycle-v3 SHA7631FD3D. Product
  unchanged. Phone was asleep before execution; requested unlock. No lifecycle
  run has begun, no new write/API action. The pending test is Cancel-only.
- Saved lifecycle harness/pending test as5503668. Andrew unlocked; one run reached
  review/Cancel but stopped at FOREIGN_FOCUSED_APPLICATION before return evidence.
  Direct original Activity/editor/token survived review attached/empty/caret0,
  all lifecycle deltas0; accessibility original absent. Own review identity stable
  before Cancel. No writes; mic555ms stopped. Found test helper conflates missing
  root metadata with foreign package; correct that distinction before interpreting
  failure. No product guard change or insertion pass.
- Corrected lifecycle test CB282F6F independently cleared, smoke build/signature/
  lint passed, installed SHAD2A1FB5D. Same Cancel-only trace PASS: exact original
  accessibility and direct owned identity survived, unchanged empty/caret0 and
  lifecycle deltas0 at natural return and300ms settled; exactly one removed2/
  added1 pair. No writes; mic497ms stopped, SwiftKey unchanged. Proceed with a
  reviewed explicit paired occlusion protocol, retaining final destination and
  one-attempt safeguards. Automatic insertion remains unproven.
- Andrew requested wrap-up for token reset. Stopped all agents before any paired
  occlusion implementation; only a proposal remains. He raised Korean preview
  plus manual Insert as a possible alternative, not a settled UI change. Restored
  secure manual speech/translation v2 (SHAEEE34599) in-place, preserving key/data.
  PID27092 responsive and idle: all new counters0, draftNONE/cleanupfalse, mic off,
  SwiftKey unchanged. No further tests or feature work running. Save/push this
  checkpoint and resume with a short priority discussion after reset.

## 2026-10-26 — MVP priority and repeat-use fix

- Andrew deferred difficult automatic insertion work. Explicitly selected manual
  Insert plus Copy & close. Requests same-editor bubble reuse and Karrot refusal
  diagnosis; no Korean preview change requested in this scope.
- Source identifies completion/Hide suppression clearing only on keyboard absence,
  with no subscribed editor-click event. Sol/xhigh prepares UI-only completion
  rearm, scoped sample tests, and independent Astra/xhigh review; AstraUltra plan
  check accepted. No automatic-destination/paired-occlusion work.
- ADB initially has no device. Requested connect/unlock to read any retained own
  Karrot refusal diagnostic before update. Cause unmeasured; do not speculate or
  remove guards based solely on a visible cursor. Preserve live credential.
- Read retained process27092 diagnostics before update: repeated NODE_EDITOR_MISMATCH
  refusals followed by later successful EN insertion (trace has no app attribution).
  Prepared Karrot metadata-only probe found same root/node package/window but
  focused0/editable0/inputType0, no resource. No content/history/action read.
  Keep Karrot on Copy & close/manual Paste rather than a new adapter.
- Fresh-click rearm candidate independently reviewed,81 units passed, both owned
  Insert/Copy cases passed without keyboard cycle. Manual Karrot Copy completed
  but Ko did not return; process32050 remained READY/IMEvisible/suppressedtrue.
  Andrew explicitly selected keeping collapsed Ko available with the keyboard,
  closing only the result panel. Sol revises that simpler completion behavior;
  no automatic insertion or guard relaxation. Pending new review/build/device gate.
- Final persistent-Ko source AF6DC0A4 independently cleared; prior click policy,
  XML subscription and extra unit tests removed. All78 units passed; app/smoke
  lint0 errors (35/3 warnings), signatures verified. Updated only older REVIEW
  completion UI expectations; their full cases were not rerun. New owned Insert
  and Copy cases both passed with continuous SwiftKey/no extra write or recording.
- Installed real speech/translation MVP EA8D45E0 in-place; automatic=false,
  key/data preserved. Andrew's real EN speech/review/Copy Karrot test passed and
  Ko stayed available. PID22101 proof1/COPIED, draftNONE/cleanupfalse/capture0/
  commit0, mic4.716s stopped, bubble READY/shown. Default/release outputs OFF.
  No real-app Insert/Send. User's intervening sample-sentence report was caused
  by the temporary API-free test build; actual voice is restored and verified.
- Andrew requested future original icon design, gray and slightly transparent,
  using Wispr Flow as a visual reference rather than copying its mark. Public
  Android examples/help located; no icon implementation or design selection yet.
- Generated three independent floating-button concept previews with built-in
  Image Gen: displayed1 Quiet Wave,2 Spoken K,3 Speak Mark. Light gray translucent
  body/opaque graphite symbol; current56x48dp component is the size target.
  Exact preview paths/mapping are in PROGRESS. Await user selection; no app source,
  installation, credential or phone-state changes in this design turn.

## 2026-10-26 — Selected Spoken K installed

- Andrew selected the middle concept and authorized installation. Preserved the
  actual original mark through Image Gen foreground extraction; selected source
  and provenance are saved in docs/design/floating-button. Native gray body is
  80% opaque; 56×48 dp Button/tap/drag/position and all processing guards unchanged.
- Sol/xhigh implementation95ECC800 and focused smoke independently reviewed by
  Astra/xhigh. All78 units pass; app/smoke lint0errors (34/4 warnings), signatures
  verified. Saved/installed live speech/translation APK BAA162DD in-place, PID22160;
  no temporary sample build or credential change. Default/release outputs OFF.
- First icon test failed MODE_CHOICES_NOT_VISIBLE; case-insensitive label helper
  correction3EDA81BC independently cleared. Corrected icon-only case PASSED4.510s:
  open/Close, drag/restore, unchanged empty fixture and work counters0. Drawable-only
  native render visually checked; screenshot protection stayed enabled. No new
  speech/API/Copy/Insert/Send. SwiftKey unchanged, prior mic4.716s remains stopped.
- Standalone own-service dumps time out intermittently, including before icon
  installation. Cause remains open; passing instrumentation obtained idle state
  internally. No broad lifecycle investigation undertaken for this cosmetic task.
  Next: user visual/daily-use feedback; Korean tone/latency acceptance remains open.

## 2026-10-26 — Standing later changes list and notification reference

- Andrew will try the MVP and report immediate fixes. Created the separate
  CHANGES_PROPOSED_FOR_LATER.md list, with deferred automatic insertion, UI/review
  options, readiness/recovery, Karrot compatibility, broader resilience, launcher
  fallback and personal distribution; acceptance work is separately identified.
- Preserved his supplied original notification JPEG under docs/references and
  linked/described it in the list. Upper truncated card appears to be Android's
  overlay notice; lower dictation-ready notice is documented by Wispr as ongoing
  background status. Official Android/Wispr sources rechecked. The image does
  not establish an automatic-insertion fix or active microphone capture.
- Linked the backlog from README/PROGRESS and added its maintenance rule to
  AGENTS. Documentation/reference-only work: no app, permissions, credential,
  build, installation or phone actions. Current MVP remains installed.
