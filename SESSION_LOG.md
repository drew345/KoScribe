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
