# Session log

## 2026-10-31 — Design review site revised from Andrew's feedback

- Andrew corrected the first site's plain-K current bubble and preferred a
  Wispr-like rounded-square silhouette with KoScribe's own mark. He liked the
  compact mode and recording concepts, requested no Korean preview in the
  result concept, a simpler English review, clearer Hide/Close controls, a
  single Cancel and light Move handle, and launcher outside the daily flow.
- Sol/xhigh revised the owner-private Site only; the current bubble now uses
  the exact approved foreground PNG, and the website opens on bubble stage
  1/5. Astra/xhigh independently checked product fidelity, source safety,
  contrast, and responsive widths. Follow-up fixes removed an inaccurate phone
  notch, improved small-text contrast and 320–361px framing, and retained the
  launcher audio privacy disclosure. Local desktop/mobile browser checks and
  JavaScript/source checks passed. Site source
  `2f38f8bcf2222355209790bf5885519ce41e2d66` deployed successfully at
  https://koscribe-design-review.user78395.chatgpt.site; owner sign-in remains
  required. No Android code, installed APK, key, or real-app data changed.

## 2026-10-31 — Private visual comparison site

- At Andrew's request, built and published an owner-private static website
  comparing the current KoScribe sample flow with Quiet pulse, Compact level,
  and Calm status concepts across six stages. Source is in a separate Sites
  checkout; no phone captures, private content, mic/API access, or Android code
  were added to it.
- Sol/xhigh implemented the site; Astra/xhigh independently reviewed it. Fixed
  keyboard focus after selection, low small-text contrast, and 320px phone
  framing before publication. Local desktop/phone-width visual and interaction
  checks passed. Published version `31799738d3df3bbeea811bdb930f2a0580207a3e`
  at https://koscribe-design-review.user78395.chatgpt.site. The owner-private
  gate requires ChatGPT sign-in; deployed status succeeded. No Android app
  changes were made.

## 2026-10-31 — Synthetic phone flow visual audit

- After Andrew reenabled USB debugging, captured nine accepted screenshots of
  KoScribe's owned flow with SwiftKey using a temporary screenshot-safe sample
  build. No conversation, credential, or actual recording was captured. Live
  protected screens remained protected; private/rejected captures were removed.
- The mode and review panels use more space than their controls require. The
  eleven recording bars are decorative and visually dominant. The result panel
  gives Insert and Copy & close equal weight and shows no Korean preview;
  KoScribe and Wispr bubbles overlap at the observed position. The sample-only
  recording panel clips Stop; the live fixed-row version was not recaptured.
- A single fixed Korean sample inserted in the owned test editor; no real-app
  insertion or Send. Restored reviewed live recording-indicator v2 in place and
  verified speech/translation on, auto/probes off, SwiftKey selected. Saved
  findings in docs/design/UX_AUDIT_2026-10-31.md and proposals in the standing
  changes checklist. No redesign code was changed.

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

## 2026-10-26 — Deferred custom vocabulary

- Andrew reported missed recognition of “Dangun pay” and “Twosome cafe”. Added
  custom vocabulary/correction rules as item 10 in CHANGES_PROPOSED_FOR_LATER.md,
  preserving his spellings and leaving canonical output/misheard forms open.
- Documentation only; current transcription, translation and phone build unchanged.

## 2026-10-28 — Instagram comments feedback

- Andrew reports KoScribe would not paste into Instagram comments, while DMs
  work. Added this to the deferred editor-compatibility item; the failure cause
  and ordinary clipboard Paste behavior remain unverified. Documentation only.

## 2026-10-29 — Editor compatibility investigation

- Andrew prioritized Karrot and clarified that Instagram comment Insert initially
  refused verification but succeeded after he re-tapped the field. Retained own
  process 8903 shows INPUT_NOT_STARTED refusal and then one confirmed commit;
  the trace has no app attribution. SwiftKey remains selected, speech/translation
  enabled, automatic insertion disabled. No credential changes.
- Sol/xhigh prepared opt-in metadata-only probes; Astra/xhigh reviewed them.
  Smoke-only build and lint passed (0 errors, 4 warnings); signed helper APK
  D5645EFE installed in place. Product APK and live key preserved. Initial Karrot
  probe stopped on another focused package. After Andrew reselected Karrot's empty
  field, the same-node refresh probe found identical before/after metadata:
  matching package/window, focused=0, editable=0, enabled=1, visible=1,
  password=0, resource absent, input type zero. No content/history inspected.
- Andrew tried EN Insert in an empty Karrot chat, then tapped the field and tried
  again. User reports repeated “Could not verify this editor.” Own trace records
  three NODE_EDITOR_MISMATCH refusals, capture count 2→5 and commit count unchanged
  at 1 (the pre-existing prior result). No Karrot write, Copy/Paste or Send.
  Draft later cleared (NONE), cleanup false. User reports the result box is gone.
- Astra/ultra evaluated a connection-led alternative. Sol/xhigh implemented a
  debug-only, explicitly invoked metadata probe; Astra/xhigh cleared source,
  78 units and lint (0 errors) passed. Live-speech diagnostic C72E5935 installed
  in place; prepared Karrot field yielded INPUT_STABLE, safe text EditorInfo,
  connection present, root/node stable, field ID absent, autofill ID present,
  node focused/editable false, input type zero and selection unknown before/after.
  This is not proof of Insert eligibility; no editor content was read and no
  writes occurred. Restored live APK BAA162DD; service PID18134 reconnected,
  speech and translation enabled, draft NONE, SwiftKey selected. Defaults OFF.
- A separate explicit diagnostic was implemented by Sol/xhigh and independently
  cleared by Astra/xhigh, with 81 unit tests and lint (0 errors) passing. Android
  can return all selected text despite small context counts; unexpected text is
  discarded locally and never logged or uploaded. Andrew approved one local read
  of a field he confirms empty. Diagnostic APK 6C902906 is temporarily installed,
  live speech/translation enabled; user needed a little time to prepare the field.
  Before restoration, ADB reported no devices. No local read was triggered.
  Last known installed build remains the diagnostic. Reconnect and either restore
  BAA162DD or, after explicit empty-field readiness, run the already-approved
  one-shot read and then restore BAA162DD.

## 2026-10-29 — Karrot one-shot diagnostic interrupted; regular app restored

- Andrew reconnected the A35 and explicitly confirmed the intended Karrot field
  was empty, with no selection, SwiftKey and cursor visible. ADB saw the A35 and
  the diagnostic service reported `NO_REQUEST` before the sole approved trigger.
  Its immediate response was `QUEUED`; the subsequent categorical status was
  `INTERRUPTED`. No usable cursor finding was produced. Because input callbacks,
  window/content events or screen off can all set this status, and the result
  does not prove whether the worker had already read locally, do not run this
  read again under the one-read approval. No insertion or Send was attempted.
- Reinstalled the previously tested regular live APK BAA162DD with `adb install
  -r`, preserving app data. The reconnected service PID11209 reported draft NONE,
  speech/translation true, auto insertion false, cleanup false; SwiftKey remained
  the selected IME. Karrot compatibility remains unresolved; do not weaken
  existing safety guards based on this interrupted diagnostic.

## 2026-10-29 — Prepared no-read Karrot interruption trace

- Astra/ultra reviewed the interrupted probe source: `QUEUED` precedes preflight;
  the terminal status does not reveal whether the local read call began. Input
  callbacks, six broad accessibility event types, screen off, and service
  interrupt all share that status. No particular cause is established by the
  previous result.
- Sol/xhigh implemented a separate explicit debug-only, metadata-only dry run.
  It reuses the Karrot preflight and continuity path, records the first fixed
  interruption source and phase, and stops before a worker or content read.
  Astra/xhigh independently cleared source; all 84 unit tests and app lint
  passed. Signed candidate APK 649F03BD, in app/build/editor-compat, has REVIEW
  with speech/translation true, auto false, old content-read probe false and
  no-read probe true; signing certificate matches the regular build. Ordinary
  debug and release outputs were rebuilt with all flags OFF.
- ADB disconnected before any candidate install. The last verified installed
  app remains regular BAA162DD with saved key/data; no new phone probe or
  insertion occurred. Andrew was asked to reconnect before the diagnostic.

## 2026-10-29 — REA and Wispr Flow public-source assessment

- Andrew redirected the next investigation to MORLUTO's REA and asked whether
  reverse engineering Wispr Flow could help KoScribe. Read the official REA
  repository, Android analysis guide, skill and responsible-use guidance; also
  checked Wispr's current terms, Play listing and Android setup guide. No REA
  setup, APK acquisition/decompilation, traffic capture or phone action occurred.
- REA supports static Android APK manifest/class/method inspection, with a JDK
  and headless JADX; static output would not prove live behavior. Wispr's terms
  restrict decompilation/reverse engineering aimed at proprietary code and
  algorithms, so APK decompilation is not the next action without a permitted
  scope. Wispr publicly says it uses the accessibility input connection and
  falls back to clipboard when insertion fails. Its ongoing notification is
  documented for background service operation, not as an insertion mechanism.
  Sources and practical comparison are in docs/RESEARCH.md.

## 2026-10-29 — Karrot no-read probe reached pre-read boundary

- Andrew reconnected A35 and opened Karrot's message composer with SwiftKey.
  Verified regular app idle (draft NONE, no pending cleanup), then installed
  signed diagnostic 649F03BD in place. Service PID17629 reported REVIEW,
  speech/translation true, automatic insertion false, old content-read probe
  false and new no-read probe true. After Andrew reopened the Karrot composer,
  status was `NO_REQUEST`; one trigger returned queued, then terminal status
  `WOULD_READ SOURCE=NONE PHASE=SECOND_YIELD_WOULD_READ`.
- The hard no-read probe reached the boundary before any worker/content API.
  It neither read editor text nor wrote anything. This shows one stable
  metadata pass, not cursor/content, insertion eligibility or the cause of the
  previous interrupted content-read attempt. Reinstalled regular BAA162DD in
  place. Service PID20869 reports draft NONE, speech/translation true,
  automatic insertion false, cleanup false; SwiftKey is still selected.
- Asked Andrew to compare Wispr Flow in an empty Karrot composer with an unsent
  test: direct insertion versus its documented clipboard fallback. Await result
  before choosing another compatibility experiment. No Wispr APK inspection.
- Andrew reported that Wispr Flow inserted the unsent test directly into the
  Karrot field on this A35. This is an observed comparison, not proof of which
  internal API Wispr used. KoScribe's refusal is therefore not evidence that
  Karrot universally rejects direct text insertion. Started an independent
  Astra/ultra review of a narrow connection-led KoScribe adapter; no app source
  or phone state was changed for that design review.

## 2026-10-29 — Reviewed Karrot read trace prepared; consent pending

- Astra/ultra concluded that relaxing the generic node mismatch check would
  still fail the later caret/postcommit guards. A narrow manual Karrot adapter
  needs separate connection-based evidence at capture, precommit and
  confirmation; `EditorInfo.autofillId` is only supplemental because virtual
  editors can share a container ID. A successful local read of the user's
  confirmed empty composer is the next evidence gate, not insertion approval.
- Sol/xhigh added fixed first interruption source/phase and call-state
  (`NOT_STARTED/ATTEMPTED/RETURNED/THROWN`) reporting to the existing debug-only
  one-read probe, without payload output or production insertion changes.
  Astra/xhigh found one final-check attribution error; Sol corrected it and
  Astra cleared the focused diff. Local unit tests: 90 passed; app lint passed.
  Signed candidate APK 5CFDBD64, in app/build/editor-compat, has REVIEW,
  speech/translation true, old automatic/no-read probes false, one-read probe
  true. Ordinary debug/release outputs were rebuilt OFF/all false.
- Candidate has not been installed or run. Regular BAA162DD remains the last
  verified phone installation. The earlier one-read consent was used and
  cannot authorize this new local read. Request fresh explicit consent with
  the Android selected-text caveat, then install only if idle, ask Andrew to
  confirm a newly empty Karrot field, run once, and restore BAA162DD. No
  Karrot write trial is authorized by this diagnostic.

## 2026-10-29 — Approved Karrot read returned unknown offset

- Andrew expressly approved one new local `getSurroundingText(1,1,0)` read in
  a field he confirmed completely empty, with the selected-text caveat. The
  reviewed 5CFDBD64 diagnostic was installed over idle KoScribe (PID8046),
  speech/translation true, auto false, draft NONE. Andrew noticed the old
  unsent Wispr test text was still present; no read ran. He deleted it and
  confirmed the empty field with SwiftKey/caret. First diagnostic trigger
  refused `INITIAL_SELECTION_INVALID SOURCE=PREFLIGHT_FAILURE PHASE=PREFLIGHT
  CALL=CALL_NOT_STARTED`; it did not call the content API.
- Android documents EditorInfo initial selection as a value at input start.
  It may have remained stale after text deletion, but the probe did not record
  its numeric value, so this cause is an inference. The reviewed diagnostic
  was reinstalled after the field was empty, and Andrew backed out/reopened the
  chat and reconfirmed an empty field with SwiftKey/caret. Since no local read
  had occurred, the same one-read approval remained available. The single
  subsequent call returned `OFFSET_UNKNOWN SOURCE=NONE PHASE=FINAL_CHECK
  CALL=CALL_RETURNED`. By KarrotEmptyReadPolicy, this category requires an
  empty returned text sample and negative offset; no sample text or numeric
  position was logged, retained or uploaded. No insertion, Copy/Paste or Send.
- The approved read is now consumed. Android's `SurroundingText` contract says
  offset -1 means unknown; the empty sample does not establish absolute caret
  location or destination identity. Restored regular BAA162DD in place; service
  PID11417 reports speech/translation true, auto false, draft NONE, cleanup
  false, and SwiftKey selected. Karrot manual Insert remains unsupported under
  current guards. Copy & close remains the recovery; do not authorize a Karrot
  write from this result alone.

## 2026-10-29 — Recording visibility candidate reviewed

- Andrew selected a recording indicator as the next small MVP improvement and
  requested removal of the long privacy sentence from the panel. He prefers a
  moving waveform or pulsing bars. Sol/xhigh added a prominent mode-labelled
  recording banner, decorative animated bars, and elapsed timer, gated on
  microphone proof. Stop, Cancel and all terminal paths stop the animation.
- The panel disclosure was moved out of the cramped recording surface; the
  launcher retains the audio-to-OpenAI/no-storage disclosure. Astra/xhigh found
  that the launcher copy needed to distinguish speech-only and translation
  builds. Sol fixed this, and Astra/xhigh cleared the final diff.
- All 90 unit tests, lint with zero errors, default and translation-enabled
  debug assemblies passed. The translation-enabled APK D70CC7DF is saved in
  ignored `app/build/recording-indicator`; it matches the installed signing
  certificate. It is not installed or phone-tested yet. Regular BAA162DD
  remains the last verified phone build. Next: bounded visual recording/Cancel
  check in the owned Test message editor, no insertion.

## 2026-10-29 — Recording indicator visual check and wider revision

- After Android authorized USB, installed reviewed D70CC7DF in place. KoScribe
  reconnected with speech/translation true, auto false, draft NONE, and SwiftKey
  still selected. Andrew tested recording/Cancel twice in the owned Test message
  screen. Label, timer, 3-bar motion, Stop and Cancel worked, but the three bars
  were too narrow. Own diagnostics: proof2, two USER_CANCELLED, no draft,
  translation request or commit, cleanup false.
- Sol/xhigh widened the pulse to eleven bars on a separate row; Astra/xhigh
  cleared the source. All 90 units passed, and final lint was rerun with
  `--rerun-tasks` successfully. Signed translation-enabled v2 APK SHA-256
  `159E2452C057E49A96751D5D277DE2704FB7719A12B0B2ABE40669B9659C538E`
  was installed in place. Service reconnected speech/translation true,
  auto/Karrot probes false, draft NONE. Andrew confirmed motion, label, timer,
  Stop and Cancel worked. He found the wider decorative bars too large, but
  explicitly chose to keep this version for now. Own trace: proof1,
  USER_CANCELLED, commit0, draft NONE, cleanup false. No speech API request,
  insertion or real-app trial occurred. Record finer visual/voice-reactive
  indicator design for later. The ordinary local debug output was rebuilt and
  verified OFF with speech/translation/auto/probes false; the saved v2 APK and
  its hash were unchanged. This rebuild did not change the installed phone app.

## 2026-10-31 — Native design and installation checkpoint

- Andrew approved installing the revised comparison design and replacing the
  Phase 1 display name. Sol/xhigh implemented the native bubble, mode/result,
  recording, review and launcher styling; the app/service label is KoScribe and
  version is 0.2. Astra/xhigh cleared the app and smoke-selector diffs after a
  Move-drag fix. Guarded manual Insert, Copy & close, SwiftKey, and the saved
  on-phone key architecture remain intact.
- Translation-enabled REVIEW build passed 90 units, lint (0 errors), assembly,
  and signer comparison. In-place A35 install preserved the same app-data inode;
  KoScribe service and SwiftKey remained active. Screenshot-safe sample QA at
  font scale 1.15 showed visible mode, recording and result controls, and Move
  tap repositioned the result. A sample Stop reached English review. No real-app
  insertion, API request, Send, or credential read occurred.
- Phone QA found white-on-white launcher status icons; fixed and visually
  rechecked. It also found excess space between review card and dock; Sol
  tightened that layout and Astra cleared source, but the final spacing was
  not phone-checked before a call interrupted the visual check. Temporary
  screenshots were removed. Secure translation APK 80DA0E88 was restored in
  place; it predates only the final review spacing patch. Andrew then requested
  a battery checkpoint. Next: build/install that final secure patch, verify
  protected review layout and phone state, remove test launcher if desired,
  and commit/push after staged-file and secret inspection. See PROGRESS.

## 2026-10-31 — Final design upgrade installed at home

- Built the reviewed REVIEW/speech/translation APK with the final compact
  English review spacing. Verified the KoScribe 0.2 package label, unchanged
  debug signing certificate, feature flags, 90 prior unit passes and lint with
  zero errors. Saved final APK D266E911 under ignored `app/build/design-install`.
- Installed in place, then briefly used a screenshot-safe fixed-sample build
  only in KoScribe's owned test editor. At A35 font scale 1.15 the review card
  sat directly above the fixed Confirm/Cancel dock and SwiftKey; Cancel returned
  to the empty editor. Restored the secure final APK; Android's installed APK
  hash matched D266E911 exactly. Removed temporary screenshots and the separate
  smoke/test packages, leaving one KoScribe installation.
- App data inode, accessibility service, microphone grant and SwiftKey remained
  intact; the saved key was not read. No real speech API request, real-app
  insertion, or Send. Rebuilt ordinary local debug output with all optional
  flags OFF. Future live review/translation quality use remains ordinary MVP
  feedback, not a claim from this sample-only layout check.

## 2026-11-04 — Automatic approval-to-insertion goal resumed

- **User decision:** reopened automatic insertion and failed-editor compatibility;
  asked us to choose one goal. Selected automatic insertion after English
  approval. Earlier deferral is superseded for this work; consumed external
  read/write trial scopes remain closed. OpenAI dot is newly reported as failing,
  without a measured cause. Karrot remains a separate compatibility follow-up.
- **Implementation/review:** Sol/xhigh added one fully attested original-window
  removal/addition, a status/Cancel return host, focused read-only translation
  processing, and adversarial pure/owned-smoke tests. Astra/xhigh cleared the
  final app and harness diffs after callback-order, pending-owner, conflicting
  focus and paused-fixture fixes. Manual recovery and one-attempt rules remain;
  automatic eligibility is still fixture-only and default/release OFF.
- **Measured local checks:** 97 unit tests passed; app/smoke lint 0 errors
  (38/4 warnings), both assembled. Saved sample9F5EC233/smoke987D40B1 and
  speech/translationBA90AA2E candidates in ignored app/build/auto-insert-20261104.
  Sample signer matches the saved live baseline. Ordinary outputs restored OFF.
- **Research finding:** official Wispr/DictoKey docs support accessibility-based
  insertion; Glaido describes desktop paste. AOSP allows unknown absolute offset,
  but its fallback can conceal missing selected-text evidence. This is a future
  adapter lead, not permission to loosen Karrot checks. Sources in RESEARCH.
- **Pending device gate:** A35 is connected; readiness question sent because tests
  take the screen. No candidate installed, content read, API call, new insertion,
  screenshot or credential change. Run exact owned sample gates, then separate
  actual translation gate; restore D266E911 after sample checks.
- **Goal status:** marked blocked after the same unanswered phone-readiness gate
  recurred across three goal turns. Clean local checkpoint and all saved APK
  hashes were reverified; implementation/review agents are complete. Resume
  when Andrew confirms the unlocked A35 is free. No device action occurred.

## 2026-11-04 — First automatic device check; system-layer refusal

- Andrew replied Ready. Root installed reviewed sample9F5EC233 and smoke987D40B1
  in place over idle KoScribe. Exact owned empty method failed at
  ORIGINAL_INTENT_NOT_ARMED_IN_REVIEW in5.538s, before confirmation or a write.
- Own evidence: original removal attestation succeeded, then SYSTEM LAYER-only
  mask16 event (nonfocused/inactive, own review sole focused window) triggered
  UNEXPECTED_WINDOW_CHANGE during ENTERING_REVIEW. Intent/proof1;
  attempts/captures/commits0, manual renders0, draftNONE/cleanupfalse after Cancel;
  microphone stopped after650ms. This is a safe refusal, not acceptance.
- Restored D266E911 in place; service reconnected as PID3258 with regular
  speech/translation enabled, auto/probes false, idle draft and SwiftKey intact.
  No screenshots, real-app content, API call, key change, Copy or Send.
- Astra review recommends a single bounded Cancel-only diagnostic comparing
  the refused system window with mode-click identity/region/focus metadata.
  AOSP layer renumbering after removal is a plausible explanation, not a measured
  Samsung cause. No arbitrary SYSTEM-window exception is approved.
- Diagnostic-only service D3F3FF06 and corrected Cancel-only harness8CF4E237
  were independently cleared. App97 units/lint0errors and smoke lint0errors/build
  passed. Saved diagnostic5F4EF5F3/harnessC70B623C. Phone had locked during build;
  Andrew explicitly unlocked again before installation and the sole run.
- Cancel-only diagnostic passed5.575s. The first refused system window was
  baseline-present SystemUI at the bottom edge, with same root/package/class,
  bounds/Region/display/topology and false focus/active/PiP flags; layer DOWN was
  its only measured change. Own review had complete proof and no bounding-box
  overlap. Original identity returned unchanged after exactly1removed/1added.
  Intent/proof1, attempts/captures/commits0, draftNONE/cleanupfalse, mic639ms ended.
- Restored D266E911, servicePID12545 regular speech/translation on, auto off,
  idle and SwiftKey intact. Astra architecture/review supports one narrowly
  verified bottom-edge LAYER-only entry exception, not general system events.
  Sol is implementing it with adversarial policy tests before the next device gate.

- Reviewed sample-v2 (8E05CDD5) passed101 units/lint0errors/build. Exact empty
  automatic test failed4.782s before Confirm: bottom-SystemUI allowance accepted,
  then another unchanged right-edge SYSTEM window moved DOWN and was refused.
  Its owner is unmeasured. Attempts/captures/commits0, manual renders0, clean
  Cancel and mic722ms stopped. Restored D266E911, service25434 idle/regular flags
  and SwiftKey retained. No automatic insertion has passed.
- Architecture/review selected a cause-focused next experiment: retain the own
  bubble host through review and return, with a small visible Cancel control in
  proven owned UI space. Current source removes that window at review entry;
  AOSP ordinal layers make renumbering plausible. Disable the narrow SystemUI
  allowance, preserve all guards, review first, then run Cancel-only. This is
  an implementation direction, not measured success or external-app acceptance.
- Andrew explicitly authorized Astra Ultra for coding/checking when useful.
  Existing Ultra architecture agent is also checking the retained-host change;
  Sol/xhigh remains the implementation owner and Astra/xhigh reviews independently.
- Retained-host source99075947/review50B61440 cleared both Astra reviews;97 units,
  lint0errors41warnings and build passed. Saved CFCB26BB. Owned Cancel-only5.47s
  completed cleanly but automatic eligibility refused ORIGINAL_REMOVAL_UNVERIFIED:
  host acquired/review/return1/0/0. Identity survived1removed/1added;0writes,
  clean Cancel, mic541ms stopped. Restored D266E911, service17802 idle/regular
  flags/SwiftKey. Implement diagnostic-only predicate breakdown before any further
  behavior change; missing IME insets is a hypothesis, not a measured cause.
- Reviewed diagnosticF42CEEDE/042BDE86 passed97units/lint0errors/build; saved
 407F4959. Cancel-only5.179s measured actual OWN_HOST_PROOF refusal: full old
 review identity and tight same host/slot/control geometry passed; own IME
 visible=true/height0 and accessibilityIMEcount0.0writes, stable original,
 cleanCancel/mic559ms stopped; restored D266E911. Under Andrew's Ultra permission,
 architecture agent takes the focused implementation: bounded keyboard readiness
 within existing entry deadline, preserved core identity, full proof before
 automatic return, contradiction/early Confirm/timeout terminal. Astra/xhigh checks.
- Ultra gate69A4EE40 passed101units/lint0errors/build; sample4190ACAD with reviewed
  readiness harnessC84BD6FE completed Cancel-only6.681s but refused early at
  AUTO_HOST_REVIEW_GEOMETRY_LOST (host1/0/0, noIME_PENDING/REMOVED marker).
 0writes, stableoriginal1removed/1added, cleanCancel/mic836ms. Source review
 found INITIAL observer regression; corrected70F9508B reviewed/built, saved
 00C115A8 for next Cancel-only gate. No automatic phone insertion pass yet.
- Baseline reinstall waits at Play Protect security-check prompt. Andrew asked
  to look/explain choices; agent recommended Send this time and selected nothing.
  Requested temporary screenshot showed the system prompt and was removed from
  phone/PC. Wait for restoration completion before another install/test.
- Andrew selected Send this time himself; restoration completed and regular
  service5306 was verified idle. Installed reviewed00C115A8 with C84BD6FE.
  Cancel-only6.702s now accepted IME_PENDING and ORIGINAL_REMOVED_ONCE, then
  refused unchanged bottom-SystemUI LAYER_DOWN with full bound review proof.
  Host1/0/0,0writes/manual renders, stableoriginal1removed/1added, mic803ms ended.
  This supports complete declared keyboard/window transition verification next;
  retaining the own host alone did not remove all layer churn. Ultra designs and
  implements; Astra/xhigh independently checks. No package/edge exemptions.
- Restoration again encountered Play Protect; Andrew again selected Send this
  time. D266E911 restoration completed; service9686 idle with regular flags,
  SwiftKey unchanged. Ordinary debug/release outputs restored OFF/allfalse.

## 2026-11-04 — Finite inventory and preparation timing diagnostic

- Ultra implemented the bounded inventory/order protocol; Astra/xhigh and a
  second Astra/Ultra independently cleared sourceFA6D2118/FA14163F/BC9DB93D.
  120 units, app lint0errors41warnings and assembly passed. Sol's inventory-aware
  smoke5FACB13A/61BBF8DA was cleared; smoke lint/build passed. Saved E3108A9D
  sample and BA587844 harness. No-original-removal route remains manual fallback;
  return layer changes are a separate strict gate.
- Exact Cancel-only run6.215s (PID31208) refused PREPARATION_BOUNDS_EVENT,
  host0/0/0 inventoryFAILED/completions0, attempts/captures/commits0. Original
  returned stable after1removed/1added; cleanCancel/mic701ms ended. Restored
  D266E911, service837 idle/regular flags.
- Diagnostic-only exact predicate split9BE9392C/1C93966E/1D80D24C independently
  cleared and unit/lint/build passed; saved F859DC6A. Exact same Cancel-only run
  (PID4401) measured actual PREPARATION_HOST_UNPROVED / HOST_REMOTE_BOUNDS,
  phaseSETTLING, lease ageLT250ms and event after lease creation. Secondary
  recheck retained local holder/identity/flags, but remote bounds mismatched and
  touch Region was not confined. Not a duplicate. Host0/0/0 inventoryFAILED,
  attempts/captures/commits0, draftNONE/cleanupfalse and mic581ms stopped. The
  instrumentation output was unavailable after truncation; no harness-pass or
  elapsed-time claim is made. Own service confirms REVIEW_CANCELLED cleanup.
- Restored D266E911 in place successfully; service5544 idle, normal flags and
  SwiftKey unchanged. No API, credentials, external editor, Copy or Send used.
  Next: measured bounded preparation protocol, independently reviewed before
  another owned Cancel-only gate. No automatic phone insertion has passed.
- Ultra implemented finite OLD-to-TARGET preparation with unchanged500ms limit,
  full inventory on every event/poll, no third shape/regression and one notice per
  endpoint. Both independent reviewers cleared5048CF94/A1705D54/4412CF1A;
  128units0failures, lint0errors41warnings, assembly passed. Saved2016CA5A.
  Exact Cancel-only completed5.195s, PID10927: OLD_CAPTURED then
  PREPARATION_REMOTE_THIRD_SHAPE before any endpoint acknowledgment or acquired
  host. Actual targetREMOTE_BOUNDS/SETTLING/AFTERlease/ageLT250; local target
  rectangleYES, remote bounds/Region-confinementNO.0writes, stableoriginal,
  cleanCancel/mic525ms stopped. Restored D266E911, service12163 idle/regularflags.
  Next investigates removing intermediate resize/move geometry at its source;
  animation is unproven and arbitrary third-shape allowances are not justified.
- Public API34+ no-move control applied only to retained overlay; exact source
  45FEBE57 cleared by both reviewers,128units/lint0errors/build passed. Saved
  D3CBDC2D. Cancel-only5.709s PID16713 accepted TARGET bounds and acquired host,
  then refused entry CURRENT_OWNED_PROOF before accepted originalREMOVED/IME
  markers. Host1/0/0;0writes, stableoriginal1removed1added, cleanCancel/mic707ms.
  Restored D266E911, service18583 idle/regularflags. Next needs actual first
  pre-anchor event/proof predicate; no whole-class event exception is justified.
- Exact-frame diagnostic0E1C4097 was reviewed with identical decision reads and
  predicates;128units/lint0errors/build passed, saved5EF08DE6. Cancel5.556s,
  PID22768: actual first event ORIGINAL_FOCUS/mask96 AFTERlaunch LT250, original
  present/unfocused, reviewabsent, no sole focus; reviewBound0, hoststepOWNER_FOCUS,
  entryActive1. Preparation again acquired host;0writes, stableoriginal,
  cleanCancel/mic634ms. Restored D266E911, service24255 regular/idle. Implement
  narrow provisional original-focus transition with full original/host/survivor
  checks, no readiness authority; existing final review/destination proof remains.

## 2026-11-04 — Sleep handoff: focus-loss step passes; remote root unavailable

- **User decision:** Andrew asked around20:25 KST for a clean stopping point within
  20minutes and clear restart notes for roughly9hours later. Finish the current
  build and one Cancel-only run, restore normal phone/local configurations, then
  pause. No further experiment or automatic scheduled phone action.
- **Reviewed source:** Ultra implemented exact96 focus-loss bookkeeping with
  local identity pinning, full original/host/survivor proof and event/poll history.
  Astra/xhigh and Astra/Ultra independently cleared39EC2A5D/85D9B60D/E438AF69/
  BD8418D8.135 units passed; lint0errors41warnings and assembly passed.
  Saved AUTO-prefocus-v1.apk9D5B00D6; harnessBA587844 unchanged.
- **Measured:** exact Cancel-only5.175s, PID29021, accepted retained-target host
  and ORIGINAL_FOCUS_LOSS_PENDING_ONCE, then POLL refused WINDOW_ROOT_MISSING.
  Metadata0 means the failing window identity/counts are unknown; do not infer
  original/review/IME. Host/preAnchor proofs1, local review ownership/input1,
  remote/bound0. Host1/0/0, inventoryFAILED/0; attempts/captures/commits0.
  Original returned stable1removed/1added, cleanCancel and mic560ms stopped.
- **Left safe:** normal secure D266E911 restored and exact installed hash checked;
  service30001 idle with regular flags, SwiftKey unchanged. Removed only owned
  smoke package from user0. Main app/key preserved. Ordinary debug/release flags
  rebuilt/verified OFF/allfalse. No external content, API call, clipboard or Send.
- **Restart:** PROGRESS now contains exact source/APKs/state and next step: classify
  the FIRST unavailable root by fixed role from the deciding read, diagnostic-only,
  before any rule change. Then reviewed owned Cancel-only after fresh readiness.
  Complete entry/return/automatic insertion are still unproven. No more edits or
  tests tonight; local reviewed checkpoint only, no push.

## 2026-11-05 — Independent-model consultation prepared

- Andrew resumed and asked for a reusable question with current GitHub files to
  seek Claude/Gemini ideas. Frame architectural choices as open to challenge,
  distinguish user-facing safety requirements from the current strict validator,
  and accurately retain the unknown missing-root role/no automatic success.
- Verified private repository/current main; scanned all four outgoing commits
  and21 changed files for credential/generated-file patterns, then published
  reviewed checkpoint80b1eb9 to `codex/approve-translate-insert`. Main unchanged.
  No message sent to another model, PR opened, code changed or phone operated.
- PROGRESS updated for this consultation. Supply pinned GitHub links and a
  source-only archive for models without access to the private repository.
  Await independent advice; the exact diagnostic restart step remains pending.

## 2026-11-05 — Outside advice assessed; next experiment changed

- **User request:** evaluate pasted OpenAI-sidechat/Gemini/Claude advice and decide
  whether it changes today's plan. No implementation or phone action in this review.
- **Verified findings:** all three cited open-source apps/files exist. ThumbFree
  has real input-session/node tests and one automatic commit, but noneditable
  preview and weaker host/postcheck fallbacks. Ramblr uses whole-field replacement;
  Verbally can retry unverified commits. Android docs contradict Gemini's promise
  that editable focusable overlays preserve the original input connection.
  KoScribe already implements Claude's accessibility InputMethod/commitText route.
  The missing-root role and alleged Samsung/Knox/Karrot/dot causes remain unknown.
- **Overseer decision:** preserve the current candidate; stop growing global
  inventory exceptions. Next design/review a hard no-write owned comparison of
  Activity versus focusable accessibility-overlay English review, using the same
  target observer, real SwiftKey edits and changed-destination negatives. Fold the
  first-root role diagnostic into the baseline. Matching text/caret is insufficient;
  keep the existing writer unchanged and unreachable in the probe. No production
  migration, automatic clipboard action or external trial has been accepted.
- **Memory:** updated PROGRESS, IMPLEMENTATION, standing backlog and RESEARCH with
  source links and concrete success/stop conditions. Verified repository now public
  after Andrew's pasted account of his change; agent made no visibility change.
  Code/device results remain November4's135 tests and incomplete entry gate.

## 2026-11-05 — Revised no-write comparison authorized and started

- Andrew explicitly requested continuing the goal with today's revised plan,
  ending the sleep pause. Ultra owns app/probe implementation, Sol/xhigh the owned
  fixture/harness, and Astra/xhigh independent design/source review. Root owns
  builds, phone operation and memory. No new writer policy or external trial.
- Keep the experiment API-free and hard no-write, with default/release OFF and
  the normal secure baseline preserved. Two positive arms need actual SwiftKey
  edits; negative cases use only controlled fixture changes. Hidden same-view
  rebinding is a limitation to measure, not an oracle the app may consume.
- **Implemented/reviewed:** isolated Activity/overlay arms share a fixed editor,
  original-target observer and Dismiss. Source review corrected Activity sizing,
  input-connection wrapper assumptions, bubble placement readiness and late-return
  reporting. One baseline capture, bounded500ms host preparation,120s review and
  2s return; all legacy effects blocked build-wide. Final serviceB8B3E145/controller
  266D6D7F cleared by Astra/xhigh; smoke31AF37FC independently cleared.
- **Local validation:**144 unit tests passed; app lint0errors44warnings and both
  assemblies passed; final smoke lint0errors4warnings/build passed. Initial compile
  and harness indentation failures were corrected before final successful builds.
  Saved Activity453F8C89, overlay1B615020 and smokeBAD61EC8 with configs. No phone
  action or automatic success; fresh A35 availability requested after builds/review.

## 2026-11-05 — Six A35 review comparisons completed

- After fresh readiness, Andrew edited both fixed English reviews with SwiftKey
  and tapped Dismiss. Both restored original identity/caret/context with fresh
  TARGET input. The original root/node was available in both overlay positive
  samples and neither Activity positive sample. Distinct identical fields were
  detected; hidden same-view logical rebinding was indistinguishable in both arms.
  This measures sampled introspection, not continuous semantic identity. Exact
  times/counts and six-case table are in DEVICE_TESTS.
- All six cases completed with clean UI/draft state,0 transaction captures,
  commits, auto attempts and recording proofs. No mic/API/key/clipboard/Send.
  First-null IME only in the two positives; the historical November4 null role
  remains unknown. Normal D266E911 restored and installed hash checked, service
  28713 idle, SwiftKey unchanged; removed only the smoke package from user0.
  Ordinary debug/release verified OFF/allfalse. Andrew can use the phone normally.
- Independent reviewers support a separate default-OFF stable-owned-editor
  candidate integrating editable overlay approval with the existing guarded
  writer. Preserve this no-write probe. Confirm-to-commit, actual translation
  and any external support are unproven. No external trial is reopened.
- Saved reviewed probe and measurements in local commit `0c2c94b` after staged
  source/artifact/secret-pattern checks. Began the separate stable-editor candidate
  under the existing automatic-fixture authorization. App and smoke ownership
  remain split, with independent prewrite design/source review. No new phone use.
- **Implemented/reviewed:** distinct secure default-OFF stable fixture automatic
  overlay, exact owner/input handoff, signed package/stable field admission, fresh
  return plus independent precommit and existing single guarded commit/readback.
  English approval prepares the disclosed fixed Korean test result; no speech/API.
  Manual recovery remains fixture-fenced. Source review cleared service637B2B99/
  adapterE1B44BD0 and harnessD383EDDA after deadline, stale-callback, fallback
  timing and manual-render accounting fixes. Final150 tests passed; app lint0/50,
  smoke lint0/5; assemblies passed. Saved app8D8F27BC/smoke353EED3B. Verified
  ordinary/release OFF/allfalse. Fresh phone availability requested for the first
  changed-field negative; this build has no device-success claim yet.

## 2026-11-05 — First stable-editor write gate

- Ran v1 app 8D8F27BC and smoke 353EED3B after Andrew's readiness reply.
  The first negative stopped before mutation in 5.250s, INPUT_UNEXPECTED_FINISH.
  Fallback count 1, transaction captures/attempts/commits/recording proofs 0.
  Harness Cancel cleared the draft. Positive cases remain pending.
- Restored and verified secure MVP D266E911, service PID 24728 idle, SwiftKey
  selected, smoke removed from user 0. Reviewing a bounded trace to identify the
  session and phase at refusal. Further phone work follows reviewed evidence.
- Built and independently reviewed v2 with a bounded callback trace retained
  across harness Cancel. APK 053DD8DA, unchanged smoke 353EED3B. All 153 tests
  passed and lint reported zero errors. The capture and write decisions match v1.
- Saved A at local commit 490ce42. To use the next A35 window efficiently,
  also built the one-line visibility-hint control B, independently reviewed
  for the specific same-review input replacement described in PROGRESS. Saved
  APK B3032F12, adapter 02F4C021, and its patch, then restored tracked source A.
  B passed 153 tests and lint. Requested availability once both builds were ready.

## 2026-11-05 — Changed-field recovery passed; unchanged return refused before write

- On the A35, diagnostic A showed the explicit keyboard request preceded full
  review focus. Reviewed C lets Android show SwiftKey using the existing window
  hint. B's trigger was absent. On C the review opened with SwiftKey and the
  actual field switch was detected. A one-shot harness check missed the recovery
  panel before layout completed. Bounded same-window control checks fixed that.
- C 636BD6CE and smoke FE40499D passed the changed-field test in 4.659s with
  zero writes, usable manual recovery and clean Cancel. The middle-caret test
  recovered the original editor at G2 after one approval. Six event notifications
  invalidated the fresh transaction. Zero commit calls. The test failed at
  10.512s. Stop the remaining cases until the first invalidating event is known.
- Returned phone within Andrew's ten-minute window. Verified normal MVP
  D266E911 installed in place, PID 5575 idle, fixture removed and SwiftKey
  selected. Full measurements and hashes are in DEVICE_TESTS.
- Reviewed diagnostic D records up to eight invalidating events and the actual
  decision at commit reservation. Its guard behavior is identical to C's. All
  156 unit tests passed, app lint 0 errors/50 warnings and assembly succeeded.
  Saved D 6757F300 with its BuildConfig and verified the original signer. A
  separate phone window is needed after returning Andrew's phone.
- In the subsequent five-minute window D passed the changed-field gate in
  5.088s. It refused the unchanged middle in 10.716s. All six notifications
  were timestamped before the fresh read: bubble layer, three system-layer
  changes, system window added, and IME bounds. RESERVE_TARGET_CHANGED, zero
  commits, clean Cancel. D266E911 restored in place, PID 13370 verified idle,
  SwiftKey selected, fixture removed. No further automatic gate ran.

## 2026-11-05 — Manual MVP with editable Korean

- Andrew proposed returning to the successful manual Insert workflow, showing
  the Korean result for checking and allowing correction before Insert or Copy.
  Switched the active work to that MVP. Preserve automatic experiments for
  later. Karrot/OpenAI dot retain the explicit Copy & close / manual Paste path.
- Implemented the preview in the same nonfocusable result panel. Only Edit
  opens a focusable Korean editor. Done and Discard return to the panel and
  leave destination choice to the next explicit Insert. The edited draft is
  the shared payload for Copy and Insert. Preserve the last attempted result
  after uncertainty. Added stale/invalid edit and exact-result regressions.
- Astra independently cleared app E4BB7D07/draft 754C641C/tests 165F4017 and
  harness 4CD5DFE5/fixture 39ABFE78. Both secure manual variants passed 159
  unit tests; app lint 0 errors/50 warnings and smoke lint 0 errors/5 warnings.
  Sample AE1D1C6B, live translation 3FE8BE0F and smoke 69A663B4 saved in the
  ignored manual-korean-result-20261105 build directory. Both app signatures
  match the normal MVP. Ordinary debug and release configurations are OFF.
  Requested a new phone window for the manual tests and installation choice.
- Andrew chose to leave the revised live MVP only after the four checks pass.
  A35 Discard passed, 7.700s, no captures/commits. The long-preview setup
  stopped before review with OVERLAY_MIC_SILENCED, 1ms and zero proof. No
  destination transaction. Requested status, then restored D266E911 within
  the five-minute window (PID 31731 idle, normal settings and SwiftKey,
  fixture removed). Long preview, unedited Insert and edited Insert are pending.
- A fresh long case on the same sample reached the saved Korean preview.
  Microphone proof passed (571ms) and no write occurred. Its immediate
  keyboard-visibility assertion failed at 6.352s after Done. Scroll and both
  Inserts remained pending. Restored D266E911, PID 6412 idle. Correct the test
  to separate saved-text visibility from keyboard visibility for the manual
  destination workflow.
- New harness E54189D6 (APK B7962357) passed independent review, assembly and
  lint. The long test reports whether SwiftKey returned naturally or the
  fixture reopened it. Both Insert cases verify preview, field, cursor and
  keyboard together after the deliberate destination selection. The app and
  fixture are unchanged, so the Discard result still applies.
