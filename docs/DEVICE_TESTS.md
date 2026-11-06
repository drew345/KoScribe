# Phase 1 device acceptance record

## Empty-relative manual insertion — November 6

Reviewed app780f17e (policy069E0CAE/reader45E56ACB/service08684FFB/testsD56A8198/
Gradle40D25924) and harness1c1b66e (EFAA2297/fixture1B3AD551). All189 units pass in
candidate and OFF builds; app lint0errors51warnings, smoke lint0errors6warnings,
both assembled. Archived signed app978EF1B5 and smoke2A87967F in
`app/build/relative-empty-insert-20261106/`; signatures match live MVP.

Andrew confirmed five-minute availability at11:15:44 KST. Verified user0,
awake/unlocked, SwiftKey and normal MVP idle, then installed candidate/fixture
in place. Only exact method
`RelativeEmptyInsertSmokeTest#freshUntouchedEmptyAfterEditedKoreanInsertsOnce`
with sole `relativeInsertFreshReady=true` ran. It failed after16.001s:

```text
STATE=UNCERTAIN REASON=RELATIVE_POSTCONTEXT_TIMEOUT ORIGIN=INITIAL_ONLY READS=7
CURRENT_SOURCE=INITIAL_ONLY CURRENT_SHAPE=COLLAPSED CURRENT_POSITION=ORIGIN
CURRENT_COMPOSITION=UNKNOWN UNRESOLVED=YES
DRAFT=UNCERTAIN CAPTURES=1 COMMITS=1
OWNER=1 SOURCE_FOCUS=0 TARGET_FOCUS=1 TARGET_EMPTY=0 HOOK=IDLE
FIXTURE_CALLS=1/1 IME=1 SWIFTKEY=1
```

The fixture counted one commit call and one applied acknowledgment, and its
target was nonempty. The required postcommit selection callback did not arrive.
The final success-path exact-text/caret and source-preservation oracle was not
reached; the prewrite preservation checks had passed. This is a write-boundary
milestone and failed confirmation gate, not a full insertion pass. No retry;
UNCERTAIN remained terminal. Own cleanup succeeded, and the other four methods
were not run. No Karrot action, API, clipboard, Send or credential operation.
The sample recording briefly used the microphone within the agreed scope.

Restored live3FE8BE0F in place and verified full installed hash, service15975,
REVIEW speech/translationtrue, all recorded automatic/probe flagsfalse, draftNONE,
cleanupfalse, SwiftKey. Removed only fixture from user0. Complete by11:17:20 KST.
Next read-only source review must distinguish Android callback behavior from
fixture validity before changing confirmation or planning another device gate.

Second diagnostic used observation-only v2 app09479EE6 and smoke80D4EA72. Reviewed
app policyE987D2B3/reader26C55D02/service18C0ACAE/testsE6AD71C3, smoke270F0653/
fixtureFB189C54. All190 unit tests pass; assembly and app/smoke lint pass with
0errors51/6warnings. No extra content reads, callback injection, longer deadline,
write-policy change or automatic retry was added.

Fresh three-minute availability began11:28:06 KST. The same exact first method/
sole flag failed15.637s, again UNCERTAIN/RELATIVE_POSTCONTEXT_TIMEOUT, reads7,
admission/currentINITIAL_ONLY, currentcompositionUNKNOWN, unresolvedYES and one
product capture/commit. Added fixed-code evidence:

| Observation | Result |
| --- | --- |
| Last existing postread | EXACT |
| Direct saved Korean / collapsed expected caret / source text unchanged | true / true / true |
| Served generation unchanged / app selection callback delta | true / ZERO |
| Native target selection callback delta/category | ONE / NONZERO |
| Input connection creation delta | ZERO |
| Wrapper begin/end call deltas | TWO_PLUS / TWO_PLUS |
| Latest wrapper call depth | ZERO |
| Last begin/end return values | YES / NO |
| Latest end returned-true delta | ZERO |
| Target focused / IME visible / SwiftKey selected | true / true / true |
| Fixture delegated commit calls / true returns | 1 / 1 |

The owned oracle now proves exact saved text, caret and source preservation in
this run. The app lacked its required independent newer selection callback, so
this remains a failed confirmation gate. LAST_POSTREAD describes the last existing
sample, not every poll. Wrapper call depth is call accounting, not TextView's
internal nesting; endBatchEdit returning false does not prove rejection. The
connection did not change, so these deltas refer to the same wrapper.

Stopped after that method; no other case or Karrot action. Own cleanup completed.
Restoration blocked on Android's app-security-check prompt, confirmed by Andrew.
He chose Send this time. Verified installed full live3FE8BE0F hash, service25543
idle/regular flags/SwiftKey and fixture removal by11:31:52 KST. The three-minute
window was exceeded only while completing that already-pending restoration.
No API/clipboard/Send/credential action. Further work is source-only pending a
reviewed next candidate and fresh availability.

## Karrot relative-read fixture — November 6

Andrew confirmed the A35 unlocked and free for five minutes. At 09:54 KST,
user0, awake/unlocked, SwiftKey selected and the normal MVP idle were verified.
Installed reviewed FIXTURE18FE04FD and smokeF90717A9 in place, then ran only
`RelativeEditorReadSmokeTest#emptyCollapsedHasUnknownAbsoluteOffset` with
`relativeEmptyReady=true`.

The test failed after2.061s with `RELATIVE_READ_NOT_OBSERVED`. Read-free product
status showed `STATE=INTERRUPTED REASON=WINDOW_STATE_CHANGED CALL=NOT_STARTED`,
`ELIGIBLE=NEVER`. The first trigger never reached its content request. Window
origin/timing was not retained, so this does not establish which UI changed.
All destination capture, commit, original-intent, automatic-attempt, recording
proof and translation request counters remained0. DraftNONE/cleanupfalse.
Stopped at that first failure; no middle/selected case or Karrot read ran.

Restored editable-Korean live3FE8BE0F in place. Verified the installed APK's full
hash, service25058 connected/idle, REVIEW, speech/translationtrue, every
automatic/old probefalse, and SwiftKey retained. Removed only the smoke package
from user0. Restoration completed by09:55:42 KST, within the availability
window. No microphone, API, clipboard, insertion, Send, screenshot or credential
action occurred. The next harness revision will settle setup events before its
single trigger and report code-only terminal state/reason/call on failure;
product terminal interruption remains unchanged.

Second session at10:17 KST used the same product18FE04FD and reviewed
harness932DBB1B/smokev2 90995B07 after Andrew announced readiness. The initial
combined `MASKED_SERVED_EDITOR_AND_SWIFTKEY_NOT_READY` check timed out after9.474s,
before the new idle wait or any diagnostic trigger. Its composite boolean did
not retain which readiness condition was false. Product status was NO_REQUEST,
CALL=NOT_STARTED; all legacy effect counters0, draftNONE/cleanupfalse. No other
case or Karrot read was run. Restored and verified installed live3FE8BE0F,
idle service28207/regular flags and SwiftKey, removed fixture by10:19:52 KST.
Further harness work must expose individual readiness conditions and inspect
owned window-focus/keyboard setup without weakening product interruption.

Third session began10:25:32 KST after Andrew provided up to ten minutes, including
the empty-Karrot check if the owned tests passed. Product18FE04FD plus reviewed
harness3572B5D2/smokev3 CB426C29 passed all three exact sole-flag methods:

| Case | Time | Asserted result |
| --- | --- | --- |
| Empty | 3.282s | EMPTY, offsetUNKNOWN, relativeCOLLAPSED, independentCOLLAPSED, SHAPE_AGREES |
| Middle | 2.272s | NONEMPTY, offsetUNKNOWN, relativeCOLLAPSED, independentCOLLAPSED, SHAPE_AGREES |
| Selected emoji | 2.466s | NONEMPTY, offsetUNKNOWN, relativeCOLLAPSED, independentSELECTED, SHAPE_CONTRADICTS |

Each method reported OBSERVED/READ_COMPLETE/CALL_RETURNED and checked unchanged
fixture content/selection and effect counters. Two supplemental standalone
service-status dumps after fixture teardown timed out; the instrumented read/
counter checks completed normally and no content request was repeated. These
results support diagnostic discrimination, not a full Karrot editor simulation.

Installed KARROT arm C46B46FF, service6327. Its own status responded normally,
showing NO_REQUEST and all effects0. Andrew explicitly prepared an empty Karrot
composer with SwiftKey. Preflight status confirmed current inputKARROT. One
`--relative-editor-read-once` returned RUNNING, then read-free status at10:29:06:

```text
STATE=OBSERVED REASON=READ_COMPLETE PAYLOAD=EMPTY OFFSET=UNKNOWN
RELATIVE=COLLAPSED STYLE=UNSTYLED INPUT_SELECTION=COLLAPSED
INPUT_SOURCE=INITIAL_ONLY INPUT_AGE=OLDER INPUT_COMPOSITION=UNKNOWN
INPUT_SIGNAL=LATER_SIGNAL AGREEMENT=SHAPE_AGREES ELIGIBLE=NEVER
CALL=RETURNED CURRENT_INPUT_STARTED=YES CURRENT_INPUT_PACKAGE=KARROT
```

All destination capture/commit/original/automatic/recording/translation counters0,
draftNONE/cleanupfalse. The read matched bounded pre/post served metadata, but
the independent selection was only the starting EditorInfo after later signals;
there was no current-generation selection update. UNSTYLED does not prove no
composition. No writer eligibility is established. Do not repeat the empty read;
the next diagnostic question is live callback viability during controlled edits.

Restored live3FE8BE0F and verified installed hash, service9329 connected/idle,
REVIEW speech/translationtrue, all experimentsfalse and SwiftKey. Removed only
fixture from user0 by10:30:24 KST. No API, mic, clipboard, insertion, Send,
screenshots or credential actions. User-operated fixed-sample selection
calibration is a separate upcoming reviewed scope.

## Cached selection calibration — November 6

Reviewed sources: cache3A3A6CC2/testsFFB584D6/probeA3295272/serviceDAB2B763/
Gradle6FD7E067 and smokeA22A6D39. All176 units pass, app lint0errors50warnings,
smoke lint0errors5warnings; signed archived arms CAAB09CD (FIXTURE_SELECTION),
7A96B1C4 (KARROT_SELECTION), smoke1ED459AC. The current committed smoke source
0C4D445B differs only by removal of one trailing space, verified with `git diff -w`.
Default/release OFF. These arms disable content reads before token allocation;
status reads only cached numeric callback classifications and a local clock.

Andrew provided ten-minute availability at10:42:35 KST. The exact owned
`RelativeSelectionSmokeTest#currentGenerationSelectionCallbacksTrackOwnedChangesWithoutContentRead`
with sole `relativeSelectionReady=true` passed3.662s on service23300. All stages
stayed generation2, starts1, finishes0, restarts0:

| Owned stage | Source | Shape/pair | Composition | Callback count | Signal order |
| --- | --- | --- | --- | --- | --- |
| Initial | INITIAL_ONLY | COLLAPSED/ZERO | UNKNOWN | 0 | NONE |
| Typed | CALLBACK_CURRENT_GEN | COLLAPSED/CARET4 | COMPOSING | 2 | BEFORE_SAMPLE |
| Selected | CALLBACK_CURRENT_GEN | SELECTED/RANGE_0_4 | NONE_REPORTED | 4 | BEFORE_SAMPLE |
| Cleared | CALLBACK_CURRENT_GEN | COLLAPSED/ZERO | NONE_REPORTED | 5 | BEFORE_SAMPLE |

The fixture used ordinary Editable changes and selection; it did not inject
callbacks or claim physical typing. Direct owned oracle and unchanged effect
counters passed. Installed KARROT_SELECTION7A96B1C4, service24978. One standalone
status call timed out during reconnect; after Andrew prepared the composer,
cached status responded normally. No content request was made.

Andrew separately confirmed each real Karrot stage. All snapshots stayed
generation4, starts2, finishes0, restarts1/LAST_START=RESTART; the restart already
preceded the empty baseline. Counts below are both per-generation and total:

| Karrot user stage | Source | Shape/position/pair | Composition | Callbacks | Signals / last |
| --- | --- | --- | --- | --- | --- |
| Empty,10:45:00 | INITIAL_ONLY | COLLAPSED/ORIGIN/ZERO | UNKNOWN | 0 | 186 / CONTENT |
| Typed ABCD,10:47:33 | CALLBACK_CURRENT_GEN | COLLAPSED/NONZERO/CARET4 | COMPOSING | 6 | 206 / SELECTION |
| Selected all four | CALLBACK_CURRENT_GEN | SELECTED/RANGE/RANGE_0_4 | NONE_REPORTED | 7 | 224 / CONTENT |
| Deleted to empty,10:48:39 | CALLBACK_CURRENT_GEN | COLLAPSED/ORIGIN/ZERO | NONE_REPORTED | 8 | 232 / CONTENT |

All Karrot snapshots had SAMPLE_AGE=OLDER and SIGNAL_ORDER=AFTER_SAMPLE;
user reply/observation delays and later delivered events remain explicit. Target
CONTENT signals are package-scoped, not proof of a particular field mutation.
CARET4/RANGE_0_4 classify supplied coordinates only, not remotely observed text
length. Independent user reports supplied the fixed-sample oracle.

Every snapshot CONTENT_READ=DISABLED/ELIGIBLE=NEVER, countersOK. Final product
effect counters0, draftNONE/cleanupfalse. This proves useful delivered selection
and composition updates in this tested Karrot session; it does not prove general
remote freshness, post-review initial selection accuracy, or write/readback.
Restored verified installed live3FE8BE0F, service28758 idle/ordinary flags and
SwiftKey, removed only fixture by10:50:47 KST. Andrew confirmed sample deletion.
No text/credential inspection, Insert, Copy, Send, microphone or API operation.

## 2026-11-04 — Final sleep checkpoint: focus-loss accepted, poll root missing

Source service39EC2A5DFD48D2C88783007F096302034CCF8C0EFFFAD2AB9C550290C8C1F4A3,
ReviewActivity85D9B60D7E899FAEB95A421516A2CC23A4B4B02C4AEF1559044482EC74D82686,
inventoryE438AF69068FC2ADC834DBA5E1BE686762B2AB0863FDF39CD072E1B7D026B8C9,
testsBD8418D8B0EC4CF1038C25ADBA070450C89815C029AE81E504DAB47CAAC82DB5
were independently cleared by Astra/xhigh and Astra/Ultra.135 units passed;
app lint0errors41warnings and assembly passed. Saved `AUTO-prefocus-v1.apk`, SHA-256
`9D5B00D6582D235AACF4CB58109723F8EC23E3AC4E9E172B38A44F121952A8B7`.
It is secure API-free REVIEW/auto, all other feature/probe flagsfalse.

Andrew requested a sleep stopping point, so root ran only the already planned
`AutoLifecycleDiagnosticSmokeTest#originalWindowReviewCancelReturnIsObservedWithoutInsertion`
with sole `autoLifecycleReady=true` using BA587844. Harness completed5.175s on
PID29021. TARGET acknowledgment/retained-host acquisition succeeded, followed by
`AUTO_ENTRY_ORIGINAL_FOCUS_LOSS_PENDING_ONCE`. Thus the new measured96 stage
passed this run. A subsequent passive POLL refused WINDOW_ROOT_MISSING.

Actual deciding frame: inventoryBASELINE, launchageLT1000, metadata0/anchor0,
reviewProved0, hostProved1, preAnchorProved1, entryActive1. PolicyENTERING_REVIEW,
draftREVIEW/leaseREVIEW, ownerpresent/same1. Local review service/unlocked/own/
local/input/wait1; remote/root/editor/sole/full/bound0. The incomplete inventory
has UNKNOWN counts; the diagnostic does not identify which window root was
unavailable. This is not evidence of original-root loss, skipped epochs or a
safe missing-root interval. Full entry completion and all automatic insertion
remain unproven. No next write-path test was attempted.

Host1/0/0, inventoryFAILED/completions0, attempts/captures/commits/manual renders0.
Original direct/accessibility identities returned stable after1removed/1added;
draftNONE/cleanupfalse and microphone560ms stopped. Restored D266E911 in place,
verified installed hash exactly, service30001 idle/regular flags, SwiftKey retained.
Removed only the owned smoke package from user0; main app/key unchanged.
Ordinary local debug/release flags were rebuilt/verified OFF/allfalse.

Restart with a diagnostic-only classification of the FIRST missing root from
the same read (fixed original/review/host/IME/baseline-other/unknown roles). No
root retry, identifier/content log or missing-metadata success. Review/build
before a fresh owned Cancel-only gate; PROGRESS owns the exact restart sequence.

## 2026-11-04 — Finite entry inventory: preparation geometry refusal

Reviewed source serviceFA6D2118/inventoryFA14163F/testsBC9DB93D passed120 units,
lint0errors41warnings and assembly. Saved `AUTO-inventory-v1.apk`, SHA-256
`E3108A9DD2CE8B2A5609683B97E37C6C4439632263FF65F86CD700E347EA049D`.
Reviewed smoke5FACB13A/61BBF8DA passed lint/build; `smoke-inventory-v1.apk`
SHA-256 `BA587844D1A4B9D8028CB92F360F6A0808B30DCEF59A6BEE65575075EEC339CD`.
It waits for full host proof and finite inventory completion before Confirm.

Exact `AutoLifecycleDiagnosticSmokeTest#originalWindowReviewCancelReturnIsObservedWithoutInsertion`
with sole `autoLifecycleReady=true` completed6.215s, PID31208. Mode-click intent
and complete baseline inventory were captured, but PREPARATION_BOUNDS_EVENT
refused eligibility before host acquisition. Host acquired/review/return0/0/0,
inventoryFAILED/completions0, attempts/captures/commits/manual renders0. Original
identity returned stable after1removed/1added; cleanCancel, mic701ms ended.
Restored D266E911; service837 idle/regular flags. This was a safe refusal.

Diagnostic-only source9BE9392C/1C93966E/1D80D24C split actual first predicates
without changing Boolean order, reads or outcomes. Independently cleared and
unit/lint/assembly passed. Saved `AUTO-inventory-preparation-diag-v1.apk`, SHA-256
`F859DC6A86CDAA3E35BDC6DDF2492514209CBAB51ED96A67FA3325C740273073`.
Same Cancel-only method ran on PID4401. Instrumentation output was truncated
and unavailable; no harness-pass or elapsed-time claim is made. Own service
recorded REVIEW_CANCELLED, draftNONE, cleanupfalse and mic581ms stopped.

Actual decision: PREPARATION_HOST_UNPROVED / HOST_REMOTE_BOUNDS, phaseSETTLING,
lease ageLT250ms; event occurrence followed lease creation and was not future.
Separate post-decision recheck: lease/draft/holder/params same; holder attached,
shown, enabled and token present; correct flags/local rectangle; one remote
window of expected type/default display and unfocused. Remote BOUNDS_NO and
REGION_CONFINED_NO, with region nonempty. This does not prove the exact old or
intermediate shape, only that final geometry had not been verified. It was not
a duplicate preparation BOUNDS notification. Host0/0/0, inventoryFAILED/0,
attempts/captures/commits/manual renders0.

Restored D266E911 successfully; service5544 idle, speech/translation enabled,
auto/probes disabled and SwiftKey retained. No real-editor inspection, API,
credential operation, clipboard, screenshot or Send in these two runs. Next is
a reviewed finite preparation protocol retaining final settled prelaunch proof
and complete survivor continuity. Automatic insertion remains unaccepted.

### Same-day finite OLD-to-TARGET preparation: third shape refused

Source5048CF94/A1705D54/4412CF1A was independently cleared by Astra/xhigh and
Astra/Ultra;128units passed, lint0errors41warnings and assembly passed. Saved
`AUTO-inventory-preparation-v2.apk`, SHA-256
`2016CA5A35CE8C47193CAAB53BE05DAFCBB5E820F19A33B8C618507196089979`.
The fixed500ms protocol freezes exact OLD before mutation, requires exact TARGET
and complete inventory before launch, permits at most one notice per endpoint,
and rejects regression, duplicate, third shape or changed survivor metadata.

Same exact Cancel-only method/flag and BA587844 harness completed5.195s on
PID10927. OLD_CAPTURED succeeded, then PREPARATION_REMOTE_THIRD_SHAPE refused
before any endpoint acknowledgment. Actual target failureREMOTE_BOUNDS, phase
SETTLING, eventAFTERlease/notfuture, leaseageLT250. Recheck local desiredrectYES,
remoteBOUNDS_NO/REGION_CONFINED_NO, unchanged identity/flags. Exact third-shape
geometry and cause remain unmeasured. Host0/0/0, inventoryFAILED/0, attempts,
captures, commits and manual renders0. Original direct/accessibility identity
returned stable after1removed/1added; draftNONE/cleanupfalse and mic525ms stopped.
Restored D266E911 successfully; service12163 idle with regular flags.

This disproves the two-endpoint assumption on this run, not the final destination
identity. Investigate eliminating the intermediate own-host shape at source;
current code issues one explicit position+size update. Move animation is only a
hypothesis. No automatic write-path test or new external-app scope is opened.

### Same-day no-move overlay: preparation passed, early entry proof refused

Public API34+ `setCanPlayMoveAnimation(false)` was applied only to the retained
host before its one existing layout update. Both reviewers cleared45FEBE57 with
unchanged modelA1705D54/tests4412CF1A.128units, lint0errors41warnings and build
passed. Saved `AUTO-inventory-no-move-v1.apk`, SHA-256
`D3CBDC2D1AA53391060A161B06EF4F26258DBDB7A55F879860D7BFE0AB854759`.

Same BA587844 harness and exact Cancel-only method/flag completed5.709s on
PID16713. OLD_CAPTURED, MOVE_ANIMATION_REQUESTED_OFF, BOUNDS_ACK_TARGET and
HOST_LEASE_ACQUIRED all observed. It then refused entry CURRENT_OWNED_PROOF
before any accepted original-removal or IME-readiness marker. The exact triggering
event and grouped owned-proof subpredicate are unrecorded. This is preparation
progress on this run, not completed review continuity or insertion acceptance.
Host1/0/0, inventoryFAILED/0, attempts/captures/commits/manual renders0. Original
returned stable after1removed/1added; cleanCancel and mic707ms stopped. Restored
D266E911 successfully; service18583 idle/regular flags. Next is a narrow actual
entry-decision diagnostic/source correction preserving full final proof.

### Same-day exact-frame diagnostic: original focus lost before review appears

Diagnostic-only source0E1C4097 with unchanged model/tests passed independent
review and128units/lint0errors/build. Saved `AUTO-entry-frame-diag-v1.apk`, SHA-256
`5EF08DE6B27F41C6BA9D8C0D0FAB172BEAFA13FD0EDFC6ACCA7D45FD035E3CE6`.
Same exact Cancel-only method/flag/harness completed5.556s, PID22768. It again
passed no-move preparation and then refused CURRENT_OWNED_PROOF.

The actual deciding frame (no later reread): stateBASELINE, eventORIGINAL_FOCUS,
roleORIGINAL, mask96, eventAFTERlaunch and launchageLT250, metadata1, anchor0,
reviewProved0/hostProved0, entryActive1. OriginalONE/present/unfocused, reviewZERO,
hostONE, no sole focus. PolicyENTERING_REVIEW/draftREVIEW/leaseREVIEW; local review
owner present/same but bound0; first placement failureOWNER_FOCUS. Review proof
service/unlocked/wait1; own focus/local/input/remote/root/editor/sole/full/bound0.

Host1/0/0, inventoryFAILED/0, attempts/captures/commits/manual renders0; original
returned stable after1removed/1added, draftNONE/cleanupfalse and mic634ms stopped.
Restored D266E911; service24255 idle/regular flags. The next narrow design admits
only a provisional loss of original focus with the original still present and
review absent under complete original/host/survivor checks. This grants no review
readiness or write authority; final authenticated review/removal proof remains.

## 2026-11-04 — Automatic empty-field candidate: safe refusal

Andrew confirmed the A35 was unlocked/free. Secure API-free REVIEW/auto sample
9F5EC233 and smoke987D40B1 were installed in place. SwiftKey stayed selected;
the personal credential was not accessed. Exact method
`AutomaticInsertSmokeTest#unchangedEmptyOriginalAutoInsertsOnceAndDismisses`
with sole `autoEmptyReady=true` failed at
`ORIGINAL_INTENT_NOT_ARMED_IN_REVIEW` in5.538s before confirmation.

Own process32073 trace accepted `AUTO_ORIGINAL_REMOVED_ONCE`, then refused
`UNEXPECTED_WINDOW_CHANGE` in ENTERING_REVIEW. Event mask16 was LAYER-only for
a present SYSTEM window, nonfocused/inactive on the default display. The own
review was the sole focused window; the original remained absent. This does
not identify the system window as harmless chrome. Intent/proof counters1,
automatic attempts/destination captures/commits0, manual result renders0.
Harness Cancel left draftNONE/cleanupfalse; AppOps showed the microphone had
stopped after650ms. No insertion, Copy, Send, screenshots or API call occurred.

Restored regular D266E911 in place. Reconnected service3258 was idle with
speech/translation=true, auto/probes=false and SwiftKey unchanged. This outcome
is a safe refusal, not automatic-insertion acceptance. Next is a reviewed
metadata-only system-window comparison during an owned Cancel-only run.

### Same-day diagnostic: unchanged bottom-edge SystemUI window

Diagnostic-only source D3F3FF06 and lifecycle selector patch8CF4E237 were cleared
by independent Astra/xhigh review. App97 unit tests, app/smoke lint0errors and
assembly passed. APKs saved under app/build/auto-insert-20261104:
`AUTO-system-layer-diag-v1.apk` SHA-256
`5F4EF5F390521364C245C28FD68AD3BCFFBC918872336B38E4EB683DC22676EE`, and
`smoke-system-layer-v1.apk` SHA-256
`C70B623C516A0989D4C15E2EB3A199543CD203CBECEC420A4EF92EC5FBF75D5E`.
Signer matched the restored live baseline. Andrew unlocked again after the phone
slept during preparation. Exact
`AutoLifecycleDiagnosticSmokeTest#originalWindowReviewCancelReturnIsObservedWithoutInsertion`
with sole `autoLifecycleReady=true` passed in5.575s on process11229.

Fixed-code refusal record: baseline PRESENT/complete, SYSTEM type and default
display same, layer DOWN, root identity/package/class same, bounds and touchable
Region same, parent/children same, PiP false before/after, input focus/active/
accessibility focus all false. SystemUI package before/after YES; event package
unavailable. Edge bits before/after T0/B1/L1/R1; original bounding-box relation
PARTIAL and current own-review relation NONE. Fresh review service/unlocked/
owner/local/input/wait/remote/root/editor/sole-focus/full proof bits all1.
Geometry categories alone are not an identity guarantee.

Exactly one original REMOVED and one ADDED were observed. The original owned
Activity/editor/token and restored accessibility window/root/node were unchanged,
empty/caret0, with lifecycle deltas0 at natural and300ms settled return. Intent/
proof1, attempts/captures/commits0, fallback1, draftNONE/cleanupfalse, microphone
639ms ended. This is a diagnostic pass; automatic insertion still fell back.
Restored regular D266E911; service12545 idle, speech/translation enabled, auto
false and SwiftKey retained. Next is a separately reviewed narrow entry-only
SystemUI layer rule and fresh automatic acceptance, not a broad system exemption.

### Same-day sample v2: second layer refusal

Source reviewed by Astra/xhigh;101 unit tests, lint0errors and assembly passed.
Saved `AUTO-sample-v2.apk` SHA-256
`8E05CDD5BF2DE06192421819170F31BC90DD1340D28A85F47C91F1C7CF85834F`.
With the phone still unlocked, installed in place and ran the same sole empty
automatic method against harnessC70B623C. It failed before confirmation in4.782s.
Process19799 accepted `AUTO_ORIGINAL_REMOVED_ONCE` and
`AUTO_SYSTEM_BOTTOM_LAYER_DOWN_ACCEPTED_ONCE`, then refused another SYSTEM
LAYER-only DOWN event during ENTERING_REVIEW. This second window was baseline
present with complete unchanged root/type/display/package/class/bounds/Region/
parent/children/PiP and false focus/active flags. SystemUI NO, owner unmeasured;
edge bits T0/B0/L0/R1 and partial own-review overlap. Full bound review proof1.
Intent/proof1, attempts/captures/commits0, fallback1, manual renders0. Cancel left
draftNONE/cleanupfalse; microphone722ms stopped. No API call, Copy, Send or image.
Restored D266E911; service25434 regular flags, idle and SwiftKey selected.

Next experiment retains the existing KoScribe overlay through review/return,
disabling the narrow SYSTEM allowance. It must pass independent review and a
Cancel-only identity/geometry test before another automatic write-path test.
Current traces do not justify ignoring SYSTEM events or adding owner exceptions.

### Same-day retained host: preparation passed, review proof refused

Sol implemented retained-host source; Astra/xhigh and Astra/Ultra independently
cleared service99075947/review50B61440.97 unit tests passed, lint0errors41warnings
and assembly passed. Saved `AUTO-retained-host-v1.apk`, SHA-256
`CFCB26BB5934D7FD2ABA49754E0FD8DA02D917BE27998CBEDF6E75EE3166F809`.
No SYSTEM-window exception remains. Same harnessC70 Cancel-only method completed
in5.47s on process15451, without Confirm, insertion, API, Copy, Send or images.

Host acquired/review/return counters1/0/0: prelaunch same-window shrink and tight
inventory proof passed, but ORIGINAL_REMOVAL_UNVERIFIED terminated automatic
eligibility. The trace does not establish which of the old review attestation or
new host/geometry predicates failed. The retained-host hypothesis is not accepted.
Original Activity/editor/token and accessibility window/root/node returned stable
after1removed/1added, natural and300ms settled checks. Intent/proof1,
attempts/captures/commits0, manual renders0, draftNONE/cleanupfalse; mic541ms ended.
Restored D266E911; service17802 idle with regular flags and SwiftKey retained.
Next: one diagnostic-only terminal predicate record, not a new guard exception.

### Same-day retained-host diagnostic: transient keyboard geometry

Diagnostic serviceF42CEEDE/review042BDE86 independently cleared;97 units,
lint0errors and assembly passed. Saved `AUTO-retained-host-diag-v1.apk` SHA-256
`407F495931202E984213865FE26D992DE3F669C9EE335370057A728E5DAF5FDF`.
Cancel-only harnessC70 completed5.179s on process20490. Actual refusal STEP was
OWN_HOST_PROOF, entry-wait age LT250ms, exact removal/original absent both1.
Post-decision snapshot: all old review proof bits1, FULL1; lease local and remote
identity/bounds/touch Region proof1; slot/card/editor/actions ready and visible,
slot contains chip and no overlap with controls. IME_VISIBLE1 but height0;
current accessibility IME window count0. Thus geometry readiness was incomplete,
not a measured target mismatch or proof of keyboard absence.
Host acquired/review/return1/0/0; attempts/captures/commits0, manual renders0,
stable original1removed/1added, draftNONE/cleanupfalse, mic559ms ended.
Restored D266E911. Next focused implementation separates that bounded keyboard
readiness wait from the already-proved removal identity; no auto return/write
before complete geometry, no deadline extension or rearming an invalidated intent.

### Same-day bounded IME gate: early observer refusal

Ultra implemented a fixed launch+1s IME readiness gate, independently reviewed
at service69A4EE40/reviewA3DBFC08/gateEB5DBD4E/testsC5C946B1.101 units passed,
lint0errors/build passed. Saved `AUTO-retained-ime-v1.apk` SHA-256
`4190ACADD083CBC8CBF6E5446A88E7401B04D374F6B19269BBAB64CEFE88E617`.
Sol's readiness-aware smoke133D637D/3CDBAE7F was independently cleared and built;
saved `smoke-retained-readiness-v1.apk` SHA-256
`C84BD6FE79DF29E04FF8B75A4398C0603891BB958F1B88FD27894C1DBD8DC708`.
Cancel-only method completed6.681s on process29435, reporting AUTO_FALLBACK.
Host acquired/review/return1/0/0; reason AUTO_HOST_REVIEW_GEOMETRY_LOST, before
any IME_PENDING or accepted REMOVED marker.0attempt/capture/commit/manual renders,
stable original1removed/1added, draftNONE/cleanupfalse, mic836ms ended.

Independent source review identified an INITIAL layout-observer authority
regression from exposing typed incomplete geometry. Corrected source70F9508B
defers incomplete INITIAL observations; only fully bound READY can be admitted
there, and exact REMOVED/pending guards remain unchanged.101 units/lint0errors/
build passed; saved `AUTO-retained-ime-v2.apk` SHA-256
`00C115A895FBCFAB69B704A02464089C4D389603EF34572D7EBF39DDB1F75168`.
Its phone gate is pending. Baseline restoration encountered Google Play Protect's
optional app-submission prompt; Andrew requested inspection/explanation. No
choice was selected by the agent; temporary system-prompt images were deleted.

### Same-day retained IME v2: entry proof passed, SYSTEM layer refusal remains

After Andrew selected Play Protect's Send this time, baseline restoration
completed. Installed reviewed00C115A8 with harnessC84BD6FE. Exact Cancel-only
method completed6.702s on process6748: IME_PENDING and ORIGINAL_REMOVED_ONCE
were accepted. A subsequent unchanged bottom-edge SystemUI LAYER_DOWN event
was refused with full bound review proof. No narrow SYSTEM allowance is active.
Host acquired/review/return1/0/0;0attempt/capture/commit/manual render, stable
original1removed/1added, draftNONE/cleanupfalse, mic803ms stopped.
Retaining the own host therefore does not eliminate all measured layer churn;
the known keyboard also disappears during entry. Complete inventory/order proof
is the next design, including only finite declared transitions and exact survivor
invariants. Current per-window evidence alone does not establish that proof.

Baseline restoration again paused at Play Protect; Andrew selected Send this
time again. Restore succeeded; service9686 idle with regular flags and SwiftKey.
Ordinary local debug/release BuildConfig outputs restored OFF/allfalse. No
automatic insertion, API request, real-app content, Copy or Send in these gates.

## Historical phase evidence

Status: eight normal synthetic cross-app checks passed on the final modal guard
on 2026-10-23, including edited English review return and Korean/emoji insertion.
First Kakao flow safely refused insertion; other external-app checks and actual
SwiftKey keystroke editing remain pending.
On 2026-10-24 the RESELECT candidate also passed three exact synthetic bubble
gates: visibility/drag/Hide, service-restart position restoration, and recording
through keyboard transitions. The final REVIEW candidate passed all seven exact
synthetic acceptance methods, including Copy and confirmed empty/middle/Korean
insertion with completion dismissal. A measured stale focused-node snapshot was
corrected by refresh plus unchanged confirmation guards. Current-build real-app
acceptance and manual SwiftKey keystrokes remain unresolved.
See the final sections for scope and limits; earlier passes apply to their builds.

## Environment to verify

- Verified Galaxy A35 5G (SM-A356E), Android 16 / API 36, One UI property
  `80500` (8.5), build `BP4A.251205.006.A356EXXSBDZH1`.
- Exactly one authorized physical device. Current user is main user 0; other
  existing profiles were left unchanged. Installed `com.koscribe.prototype`
  only into user 0 and launched successfully.
- SwiftKey 9.13.18.4 remains the selected input method.
- Wispr Flow 2.8.0 remains installed and its accessibility service enabled.
- KakaoTalk 26.9.0; Instagram 452.1.0.53.64.
- Default SMS role is Google Messages (`com.google.android.apps.messaging`),
  version `messages.android_20261007_04_RC00.phone_samsung_dynamic`.
- No personal messages or contacts were collected. Device identifiers are not
  retained in this record.

## Acceptance matrix

Use synthetic messages in the local test editor first. Real-app tests remain
unsent drafts; Andrew controls any manual Send. Record observed results, not
inferences from unit tests.

| Check | Local editor | KakaoTalk | Instagram DM | SMS/RCS |
| --- | --- | --- | --- | --- |
| Bubble visible; SwiftKey stays selected; Wispr coexists | Pending | Pending | Pending | Pending |
| Choose 요 / 반말 / EN; selected mode visible | All three fixed-sample paths passed | Pending | Pending | Pending |
| Microphone starts; Stop/Cancel/background/lock stop it | Pending | Pending | Pending | Pending |
| Review/edit English with SwiftKey; return to original field | Edited-content return passed with IME visible; actual keypresses pending | Pending | Pending | Pending |
| Empty/beginning/middle/end insertion; existing text preserved | Empty and middle passed with SwiftKey visible; middle exact preservation repeated | Empty draft unchanged after guard refusal; insertion unverified | Pending | Pending |
| Existing Korean, English, emoji; composition preserved/refused | Exact Korean/English/emoji preserved; known active composition refused separately | Pending | Pending | Pending |
| Selected text or unknown caret refuses automatic insertion | Selected range refused; exact draft preserved | Pending | Pending | Pending |
| Changed text/field/app/conversation (including switch-back) refuses | Pending | Pending | Pending | Pending |
| Cancel, repeated controls, rotation, service loss do not insert late | Cancel and leaving review passed; other cases pending | Pending | Pending | Pending |
| No automatic Send/Enter; uncertain outcome never auto-retries | No Send/Enter in reviewed tests; ignored-commit uncertainty passed on window/focus-change trigger | Pending | Pending | Pending |
| Explicit Copy recovery works after refusal | Pending | Pending | Pending | Pending |

Separately check password/PIN/nontext fields, unavailable microphone, denied
permission, and unavailable accessibility service. Do not enter real secrets.
Inspect Samsung per-app battery/sleep settings and test background/idle survival,
reboot recovery, and global microphone blocking. The permission metadata audit
in `RESEARCH.md` confirms current grants, not those reliability behaviors.
Build warnings and unit-test results belong in `PROGRESS.md`; failures here
must describe the observed action/result without saving personal chat text.

## Gate

Phase 2 remains blocked on the required device evidence and any necessary
Phase 1 fixes. Unsupported fields must refuse safely. A passing local editor
test does not establish compatibility with another app.

## Initial local observations (prototype cfdd6fb)

- Bubble appears with all three modes; SwiftKey remains selected. Wispr's
  accessibility grant remains enabled; full Wispr functional coexistence is
  not yet tested.
- An empty-field attempt refused before recording with a generic observation
  error. Diagnostic refinement is in progress; cause not yet established.
- A synthetic word with active SwiftKey composition was refused. Finishing the
  word with a space allowed an EN recording session to start.
- AppOps confirmed RECORD_AUDIO running during capture, then not running after
  Stop. Fixed English review appeared, and confirmation returned to the local
  destination. Insertion was refused as TARGET_CHANGED; original text remained.
  Copy recovery was offered (not used yet).
- A separate recording/Cancel test stopped the microphone and left the local
  draft unchanged after the delayed-insertion interval.

## Diagnostic updates and empty-field fix

- Code-only in-memory service diagnostics confirmed all three empty-editor
  conditions: empty input connection at offset/caret zero, missing node
  selection endpoints, and node empty/showing hint. AOSP explains that empty
  TextViews omit accessibility selection even with a valid input caret.
- Automatic review initially rejected the exception under the earlier
  diagnostic-only scope. Andrew explicitly approved the exact narrow rule;
  Sol implemented it and Astra reviewed it. Build, ten unit tests, and lint
  passed. Empty local editor now starts EN recording successfully.
- Redundant status-label updates were removed. Full-screen return still emits
  original-root CONTENT_CHANGED flags 0, two original-root STATE_CHANGED flags
  0, editor focus, and input start. It refuses insertion and preserves text.
- Root content flags 0 mean unspecified, not automatically harmless. Rather
  than ignore this potential navigation signal, a floating recording/review
  Activity experiment keeps the target visible and retains current guards.

## Compact-panel observations

- Installed the native floating dialog. Recording and English review remain
  visible over the destination, with Stop/Cancel controls and a visible mode.
- First confirmation returned with one target window-state event and no
  target-change invalidation, but the session had expired during a long test
  delay. It refused with EXPIRED; this is not a successful insertion.
- A later coordinate-driven attempt missed the moved review control and
  cancelled. Switched to controls identified by exact labels for reliable tests.
- The first app-targeted instrumentation run failed all three cases in setup:
  Android restarted the instrumented app and left its accessibility service
  disconnected/marked crashed. No insertion test body ran. DONT_SUPPRESS avoids
  UiAutomation suppression but cannot prevent the target process restart.
- SwiftKey remains selected; Wispr remains bound. Requested a visible user
  toggle of KoScribe accessibility and a separate self-targeted test APK.
- No sample has yet been confirmed inserted. Real-app drafts remain pending.

## First successful insertion and remaining layout issue

- Andrew toggled KoScribe accessibility off/on. The self-targeted smoke runner
  preserves the service process and Wispr; it does not change the keyboard.
- Paced automated and manual keyboard-visible attempts reached confirmation
  but refused a CONTENT_CHANGED flags 1 event. The own-fixture classifier
  identified its source as EDITOR_ANCESTOR_SCROLL. Fresh field/caret checks
  passed, but the navigation guard correctly remained decisive.
- `stateUnchanged|adjustResize` did not remove the event and was reverted.
- `emptyEditorKeyboardHiddenDiagnostic` passed: the runner dismissed the
  visible keyboard only while the own editor/window was focused, then used
  the normal bubble/record/Stop/confirm flow. Exact English sample text appeared
  once; trace showed COMMIT_REQUESTED_ONCE and no invalidation. A screenshot
  confirmed the sample in the local field. Microphone was stopped afterward;
  SwiftKey remained selected and KoScribe/Wispr accessibility remained enabled.
- This establishes the insertion API path with IME initially hidden, not the
  ordinary keyboard-visible flow or external-app compatibility. Astra reviewed
  the controlled diagnostic and recommended moving the synthetic editor out of
  its long instructions ScrollView before retesting normal insertion.

## Cross-app synthetic editor and current blocker

- Moved the original fixture above its settings ScrollView, then added a
  minimal standard EditText in the separate smoke APK. This tests a different
  package/process without personal messages or a Send control. The runner
  verifies fixture window/editor focus and an IME window before capture,
  records for two seconds, and waits for review before confirming.
- The cross-app normal test still refuses. Code-only evidence shows target
  REMOVED while the recording panel is visible and focused; after review
  starts, target ADDED has the same original root while KoScribe remains
  focused. Current guard refuses TARGET_WINDOW_REAPPEARED.
- Astra's AOSP review confirms these events describe the reported interactive
  window list, which can change through modal occlusion. Content events may be
  filtered while a window is unreported. A proposed narrow paired rule is being
  independently reviewed; no generic content/navigation exception is accepted.
- Andrew requested continued SwiftKey-open middle insertion tests and offered
  Astra Ultra. The next correction and independent review use Astra Ultra.
- Removed the collapsed bubble's opaque padded square. Built/installed and
  visually verified a compact rounded Ko button with a transparent surround;
  expanded choices and window type/flags remain unchanged. Final icon deferred.

## Paired-window correction and next focus experiment

- Astra Ultra implemented and independently reviewed a session-bound, one-use
  recording-removal/review-reappearance allowance. It requires the exact panel
  window, uninterrupted lifecycle/focus, and refreshed original root/editor
  identities. Duplicate pairs refuse, including during final return. Existing
  content, selection, context, expiry, cancellation, and one-commit guards remain.
- Product/smoke builds, 23 unit tests, and lint passed. Installed in user 0;
  accessibility reconnected without a manual toggle.
- `emptyEditorEnglishInsertsExactlyOnce` passed with SwiftKey visible before
  capture. Exact sample appeared once; trace ended COMMIT_REQUESTED_ONCE with
  no invalidation. This run used ordinary final-return reappearance and did not
  exercise the newly allowed early pair.
- Middle-caret test exercised the early pair successfully, then safely refused
  a second removal during review: DUPLICATE_TARGET_WINDOW_PAIR. It has not yet
  established successful nonempty insertion.
- Selected-range negative passed: SELECTION_NOT_COLLAPSED, no recording, exact
  synthetic draft unchanged. A cancellation test instead refused at capture
  with ACTIVE_COMPOSITION; this did not test cancellation. Its fixture now ends
  in whitespace to represent a completed word before recording.
- Next experiment prevents automatic focus in the review EditText; explicit
  Edit English will activate it. No touch-through flag or broader window-event
  exception is planned. Callback ordering and edited-review return still need
  phone evidence.

## Explicit focus and edited-review results

- Keeping focus on the nonediting review container allowed normal middle
  insertion. The test checked exact prefix + sample + suffix, then checked
  again for a duplicate. Trace showed one COMMIT_REQUESTED_ONCE and no
  invalidation. After Andrew reported temporary Messages/Kakao use and then
  finished, the uncontended middle test passed again with the same trace.
- Cancel after Stop and leaving review both passed exact synthetic-draft
  preservation with no delayed insertion. The microphone was stopped and
  SwiftKey remained selected. Some sessions still showed duplicate occlusion
  before cancellation, so these passes do not prove a clean insertion path.
- The explicit edited-English test verified the review field and visible IME,
  edited only that synthetic field, and confirmed. It refused a second target
  removal after the first accepted reappearance, with KoScribe still focused.
  This was reproduced after Andrew finished using the phone.
- Giving the panel a fixed bounded viewport height did not change that edited
  sequence. The geometry experiment is being reverted; the explicit-focus
  improvement remains. A narrowly owned edit-transition rule is under review.
- A session-dialog-only no-move-animation experiment also failed: target
  reappearance occurred during recording before Stop, which the phase-bound
  gate refused. The API-34-qualified flag was removed after this result.
  Neither fixed height nor disabling movement animation is retained. The next
  review assesses continuous exact-panel ownership with fresh identity checks
  on each visibility return, rather than arbitrary phase/count assumptions.
- An attempted post-test screenshot found a real app after the fixture exited;
  the local image was immediately deleted and is not retained or published.
  Future verification uses fixture assertions/code-only traces unless an owned
  foreground screen has independently been verified immediately before capture.

## Final modal policy and successful synthetic checks

- The phase/count rule was replaced with strict alternating visible/hidden
  states under continuous exact-panel ownership. Each addition requires freshly
  verified original root/editor identities. Confirmation stops modal handling;
  only a verified return to the captured package/window can complete an absent
  target's return. Removal after confirmation refuses. Precommit requires the
  target present and all original field/caret/context/input checks.
- Automatic review rejected an initial service patch that broadened the focus
  helper. A safer version retained the exact own-package panel helper and added
  a separate captured-target return helper; it passed automatic review. Astra
  Ultra independently cleared source and 17 gate tests; 27 total unit tests and
  lint passed. The final product APK was installed and signature verified.
- After Andrew unlocked the phone, all eight normal synthetic cases passed:
  English empty and middle insertion; explicitly edited English review return;
  polite Korean between Korean/emoji prefix and suffix; casual Korean into an
  empty field; selected-range refusal; Cancel; and leaving review. Successful
  insertion traces show one commit and no invalidation. The edited case verified
  two modal reappearances and the final return before its single commit.
- Exact draft assertions, not screenshots, establish these results. The edited
  case uses a scoped synthetic text action while SwiftKey is visible; actual
  keyboard typing/composition is not yet covered. Microphone was stopped and
  SwiftKey remained selected afterward. The fixture-only keep-screen-on flag
  was reviewed after these passes and is not part of their measured build.
- A modal may hide accessibility content events. Same-view conversation changes
  that restore identical observed field state remain unobservable. These tests
  do not resolve that limitation or establish KakaoTalk/Instagram/SMS compatibility.

## First opt-in Kakao preflight

After Andrew explicitly approved one unsent self-chat sample, Sol added the
separate default-skipped test; Astra reviewed it and smoke assembly/lint passed.
The first opted-in invocation stopped at `KAKAO_SELECTION_NOT_KNOWN_EMPTY`
before any KoScribe button, recording, confirmation, or draft mutation. Empty,
editable, nonpassword text-field checks had passed; numeric caret endpoints
were not logged, so this alone does not establish their exact values.
Review found the test's node-only 0/0 requirement stricter than the already
approved production empty-caret/input-connection rule. Sol aligned only the
test's empty-node check with that rule; Astra rereviewed the final source and
assembly/lint passed. Exact result checks still require known collapsed
selection. The second preflight then stopped at SWIFTKEY_NOT_VISIBLE before
any product action. Scoped input-method metadata confirmed the keyboard hidden;
Andrew restored it.

The subsequent flow reached recording, Stop, review, and confirmation, then
timed out at KAKAO_RESULT_NOT_EXACT. The test repeatedly verified the original
field identity and unchanged empty/hinted state. Code-only service trace showed
OBS_VERIFIED_EMPTY_CARET and MODE_SELECTED_POLITE, followed immediately by
CONTENT_FLAGS_4 and first invalidation TARGET_CONTENT_CHANGED_OTHER_NODE during
recording startup. Later lifecycle/modal/confirmation invalidations followed;
no commit was requested for this session (the earlier commit in the trace was
from the previous synthetic run). Microphone stopped after 2.516 seconds and
SwiftKey stayed selected. No Send, screenshot, chat-history traversal, or field
text logging occurred. This is a conservative product refusal, not a missing
permission or a successful Kakao insertion. Do not rerun this confirmed session.

Observation methods evolved from screencap/state queries to scoped fixture
assertions and code-only traces; the current screenshot precautions above apply.
Stock AOSP
`uiautomator dump` connects with flags 0 and temporarily suppresses/unbinds
other accessibility services. Initial permission inspection used it before any
live recording. Do not run stock
UI dumps during these tests. Sources: [dump command](https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android16-release/cmds/uiautomator/cmds/uiautomator/src/com/android/commands/uiautomator/DumpCommand.java),
[Android accessibility service handling](https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android16-release/services/accessibility/java/com/android/server/accessibility/AccessibilityManagerService.java).

## Authorized Kakao recording / Stop / Cancel diagnostics

Andrew authorized continued non-inserting diagnostics and prepared the empty
self-chat with SwiftKey visible. The reviewed timing product APK
(`67FDAE782F8A71010BCD0457BCF545DAF1D9D6547A925826A09D9E9FD138E2C6`)
and separate opt-in test were installed; accessibility reconnected.

- The first run completed recording/Stop/Cancel but checked keyboard return too
  early. A subsequent metadata check found SwiftKey visible; microphone use had
  ended after 2.567 s. First invalidation was TARGET_WINDOW_REAPPEARED: the
  target ADDED event arrived while its window was absent from the current list.
  ROOT_ABSENT denotes a missing window here, not a present window with null root.
  No first-content timing was recorded before this invalidation; the recorder
  intentionally ignores content after a prior invalidation.
- Sol added a bounded four-second keyboard-return wait to the no-insertion
  test. Astra reviewed it; smoke build/lint passed. The corrected method passed
  twice, verifying the same original empty field immediately and after 1.2 s,
  with SwiftKey still selected and visible. Microphone stopped after 2.314 s and
  2.530 s respectively. The method never confirms or requests insertion.
- Between those passes, one attempt refused before a fresh Stop appeared;
  microphone did not start. That run did not complete post-Cancel assertions
  and must not be counted as a passing recording/Cancel flow.
- All three corrected-test sessions first invalidated on
  TARGET_CONTENT_CHANGED_OTHER_NODE, content flag 4 (description change).
  Capture duration / event after capture completion / delivery delay were
  20/35/136 ms, 14/27/27 ms, and 21/46/90 ms. The first and third had cached
  RECORDING state at delivery; the middle still had PREPARING/VISIBLE0. These
  are delivery-time states, not proof of lifecycle state at event occurrence.
  All three reported event times are after capture; none supports a stale
  pre-capture explanation. Source role and harmlessness remain unknown.

These results establish cancellation and empty-draft preservation in two real
flows, not successful Kakao insertion. Product guards are unchanged. No editor
mutation, confirmation, Send/Enter, clipboard use, screenshot, conversation
history traversal, API call, or recording retention occurred in this scope.

### Own lifecycle milestone follow-up

Sol added code-only timing at existing own-UI callbacks and reused the existing
focus verification. Astra cleared source SHA-256
`4FCACE823575D42537467F307FC688D06E27FCFD0731F54E80083576B9165A9D`.
Both APK builds, 27 JVM tests (zero failures/errors), lint (zero errors;
app 15/smoke 3 warnings), and product signature verification passed. Installed
product SHA-256 is
`BC46403985B60C26EB04A26A41E6BC55199FDF5F7EB2001FC3A29510C24AD0F9`.
SwiftKey remained visible after this update; no phone action was needed.

Two more authorized no-insertion measurements produced:

| Observation | Early startup refusal | Complete Cancel pass |
| --- | --- | --- |
| Capture duration | 29 ms | 12 ms |
| Content event after capture completion | 38 ms | 38 ms |
| Delivery delay | 32 ms | 105 ms |
| Bubble add return / launch request before event | 28 ms | 26 ms |
| Accepted panel bind relative to event | Not observed | 25 ms after |
| Resume callback relative to event | Not observed | 87 ms after |
| Own known-window focus callback relative to event | Not observed | 219 ms after |

Both first invalidations were OTHER_NODE content flag 4. The early attempt
never started the microphone; PANEL_OWNER_CHANGED followed invalidation and
does not establish an actual owner switch. The completed method passed in
5.178 s with the same original field empty immediately and after 1.2 s and
SwiftKey selected/visible. Its microphone use ended after 2.634 s.

The occurrence timestamp precedes accepted panel binding in the successful
flow; the event was delivered after binding and resume. This accounts for the
previous cached RECORDING state at delivery. Launch request preceding the event
does not establish causation. The missing verified-window milestone does not
prove focus never arrived: the already-invalid gate cannot accept it. No
description text, source role, conversation content, or keyboard tree was read.

Combined corrected-test total: three complete recording/Stop/Cancel passes,
two refusals before a fresh recording control, plus the earlier uncorrected
keyboard-return assertion failure. These remain cancellation diagnostics;
successful real-app insertion is still unverified.

## Bubble / recording-panel startup isolation

Andrew explicitly authorized this controlled experiment. Sol implemented the
debug-only compile-time arms described in `INSTRUMENTATION.md`; Astra reviewed
the final source and interpretation. Normal OFF behavior and all existing
guards remain. All experimental builds refuse insertion and Copy in the service,
omit confirmation/edit/Copy review controls, and end their original session on
bounded deadlines. No session is recaptured or rearmed after invalidation.

The normal build, 27 JVM tests (zero failures/errors), and lint (zero errors;
app 15/smoke 3 warnings) passed. All three arm APKs built and passed signature
verification. Each used the same root/focused-input privacy boundary and exact
preflight checks. Each test verified its installed arm and a fresh capture count.

| Arm | APK SHA-256 |
| --- | --- |
| HOLD | `2FE738BE19951CC8D051785A86E825CA82B6CB879775381F29339A2DC4B64959` |
| COLLAPSE | `1046883B4B43400B2F5717EA14A789D52772DBB244420236A5ABC871A6BCF94C` |
| PANEL | `2E36F8AD0F91209751F015928F55EF5A13D1A3B1B12E3D4519B86E1ED98F69C6` |

One controlled run per arm produced:

| Arm | Observed sequence and result |
| --- | --- |
| HOLD | Passed in 4.039 s. No collapse or panel launch; hold ended 1,505 ms after capture completion. No invalidation or first-content event recorded during the active hold. |
| COLLAPSE | Passed in 3.911 s. Collapsed bubble add returned at capture +16 ms; hold ended at +1,519 ms. No panel launch, invalidation, or first-content event recorded during the active hold. |
| PANEL | Preserved the original empty field/SwiftKey, then failed its full-flow assertion with PANEL_STOP_NOT_AVAILABLE (6.002 s). Panel launch was verified without collapse, but the first content event invalidated the session before accepted binding or recording. |

PANEL's capture took 30 ms. The launch request was capture-completion +2 ms;
the OTHER_NODE flag-4 event occurred at +107 ms (105 ms after launch), then was
delivered 5 ms later. Cached delivery state was PREPARING/VISIBLE0. No bind,
resume, or focus milestone was accepted. Later PANEL_OWNER_CHANGED is consistent
with the invalid gate rejecting bind, not proof of an actual owner change.

All three runs checked the same original empty field immediately and after
1.2 seconds, with SwiftKey selected and visible. The PANEL verdict is emitted
after those preservation assertions and fresh-capture/launch-only checks, so it
is a measured startup refusal with draft preservation, not a recording pass.
AppOps retained the earlier 2.634-second microphone use throughout: none of
these three runs started the microphone. No insertion, confirmation, Send/Enter,
clipboard operation, screenshot, chat-history/keyboard traversal, API call, or
audio retention occurred. No additional phone action or permission was needed.

These bounded comparisons associate the refusal with panel launch. Bubble
collapse and microphone use were not necessary to reproduce it in this case.
They do not establish the exact cause, source role, semantic conversation
continuity, or harmlessness. A quiet hold does not exclude events after its
observation interval. Keep all guards unchanged; identical repeats add little.

After measurement, the exact pre-experiment product APK
`BC46403985B60C26EB04A26A41E6BC55199FDF5F7EB2001FC3A29510C24AD0F9`
was restored successfully and its service reconnected. SwiftKey remained
selected/visible and microphone stopped. The new smoke runner remains installed.
The ordinary local APK output was rebuilt with OFF so the normal install path
does not point to PANEL. Separate probe APKs and the preserved original are
ignored build artifacts; they are not committed.

## Nonfocusable recording startup correction

Andrew authorized fixing startup and testing without insertion or sending.
Sol implemented a debug OVERLAY candidate; Astra independently reviewed source,
including target continuity, recorder-specific audio evidence, terminal
cancellation, retained cleanup ownership, and focused-only accessibility reads.
The first candidate added a nonfocusable full-screen accessibility window
containing a compact recording card. All insertion/Copy paths were disabled.

Both builds, 27 existing JVM tests, lint (zero errors; app 18/smoke 3 warnings),
and APK signatures passed. Reviewed source hashes:

- Service: `BFE86F9A0BD7C6EDB2A6816005E2FD5FFE7FD7C9B878B91564B2F2ABEFF94234`.
- Panel: `C61A5C4E110F5D674A17A2FC40B73F7C617E2894920E3646EBAE5D7D350BD814`.
- Scoped smoke: `A62893069D9D8465CD4D1247E72D88457EF0FEFE719D31779CB2C3B30E163F10`.
- Product APK: `D6DC9053D6D8C45056F0B24F035897F292693558DD05AC215E3CF17189C71B67`.
- Smoke APK: `B5DF3D7F590F90566F98F731C46FFECCDACFB45B27E98EB0E62ACBD9CB6580BE`.

Installation succeeded and the service reconnected with SwiftKey still visible.
One exact Stop/Cancel attempt failed `OVERLAY_MIC_PROOF_OR_STOP_NOT_AVAILABLE`
in 5.917 s. Fresh capture count was 1, observation READY; first invalidation was
`OVERLAY_TARGET_WINDOW_CHANGED`, flags 512 (`WINDOWS_CHANGE_CHILDREN`). No first
content event was recorded before cancellation. This is not evidence that the
earlier content-event issue was solved. The original guard also rejects child
window structure changes; no exception was added.

Failure cleanup verified the original empty focused field immediately and after
1.2 seconds, with SwiftKey selected/visible. Exact-app RECORD_AUDIO AppOps showed
capture ended after 136 ms; no cleanup remained pending. Stop/review were not
reached. This does not prove unsilenced input or successful recording startup.
The second direct-Cancel method was not run against the same failing build.
No insertion, confirmation, Copy, Send/Enter, chat traversal, screenshots, API
calls, audio retention, permission changes, or user preparation occurred.

The next architectural correction reuses the existing expanded bubble window
for recording controls, preserving its token and attachment after capture.
This avoids deliberately adding/removing windows during the guarded session;
all invalidation rules remain. Its outcome must be measured separately.

### Reusing the expanded bubble window

Sol replaced the second window with controls inside the already-expanded bubble,
using a fixed-height internal viewport. Astra reviewed service
`569112F6D60B84F7EFCACD5BA9065DEBFC9AF6C6A2FD2C606B9498E12B0975FE`, panel
`FB88617228AE4E0A1195C90D46B74522E28372217A8BC6ED2EA38ED202905009`, and the
unchanged scoped smoke. Build, 27 JVM tests, lint, and signature passed. Installed
product APK: `F84E0E0DB57E985AB510416C474255321CD81A653C78E28A146035022EAA0A0D`.

The exact Stop/Cancel attempt reached a visible Stop control and verified the
same empty focused Kakao editor plus selected/visible SwiftKey during recording.
Own diagnostics confirmed the original host window, positive microphone reads,
and non-null recorder-specific unsilenced configuration. This is limited capture
evidence, not audibility/transcription quality.

A later target-window CHILDREN change (512) invalidated the session while the
own host was still verified unchanged. The subsequent Stop click returned false;
the test failed `PRODUCT_BUTTON_CLICK_FAILED` in 6.399 s. Stop/review did not
complete. The original field stayed empty immediately and after 1.2 seconds,
SwiftKey stayed visible, AppOps reported capture ended after 2.558 s, and cleanup
was not pending. No first-content event was recorded before invalidation; this
does not establish absence of later content events.

The separate direct-Cancel test then passed on the same F84E APK in 3.555 s.
Fresh capture count advanced to 2, first invalidation was none, own microphone
proof succeeded, and the original empty field/SwiftKey checks passed during
recording and after cancellation. AppOps reported capture ended after 1.537 s;
cleanup was not pending. This validates direct cancellation, not the longer
Stop/review path. Old trace entries from capture 1 are not capture-2 failures.

Startup now reaches verified capture, but the complete flow remains blocked by
an unidentified window relationship change. A bounded window-metadata-only
comparison is the next diagnostic; it must retain unconditional refusal.

### Child-window metadata comparison

Astra reviewed service `7052D1C1817C9625C58FB640B00DE65A741021E15CA25C548541868C5B6332DD`.
Only up to 16 direct child-window IDs/types were sampled privately at capture
and the first CHILDREN event. No child roots, titles, view descendants, or
content were queried. Raw IDs clear when the session ends; only fixed counts
remain. Exceptions/incomplete snapshots cannot bypass refusal. Assembly/lint
and signature verification passed. Product APK:
`11FF0E59D5CCD4E085B25FCED42188FC2F4980FCF87DDD38B591542E32A4E11B`.

One scoped Stop/Cancel run reached positive/unsilenced capture and executed
Stop: `OVERLAY_MIC_PROVED_STOPPED` confirmed cleanup without exception before
review controls were populated. It then refused target CHILDREN (512) while
the same own host remained verified. The test failed
`READ_ONLY_REVIEW_NOT_AVAILABLE` in 12.438 s. Complete sampled child lists had
ADD 0, REMOVE 0, RETAIN 2, TYPE_CHANGED 0, ORDER SAME. This does not establish
that no transient change occurred, or that the system event was harmless.

The original empty-field/SwiftKey preservation checks passed after cleanup.
AppOps reported capture ended after 2.529 s; cleanup pending was false. This
establishes executed Stop, not a completed review flow. No insertion or sending
occurred. A final timing refinement will compare event occurrence/delivery with
capture and own Stop/review milestones, retaining all refusals and adding no
target reads.

### Final window-event timing and selected workflow

Astra reviewed timing-only service
`A430541EEC2CF3BE893870AD981034A3CF91A91F3F85FCC87F2417AE1341D762`
and panel `66424814DFABA94DC96D2C991A9687DBFDEB9E56208713CE9EE9E831063E5184`.
Build/lint/signature passed; installed APK:
`E5CD28F9B5BB36DBA44069644D89F191B26C42C0C829BB0016E558EE737DF73E`.

One exact Stop diagnostic failed `PRODUCT_BUTTON_DISAPPEARED` in 4.572 s.
Verified capture preceded a target CHILDREN event at capture-start +2,646 ms,
capture-complete +2,625 ms, with 5 ms delivery delay. Neither Stop-entry nor
review-populated callback was observed. This is not a pre-capture queued event;
an executed Stop/review transition is not necessary to produce the refusal.
The host remained unchanged, and both complete child snapshots again matched
(retain 2, add/remove/type changes 0, same order). Cause and harmlessness remain
unestablished. Identical repeats are exhausted.

Original empty field and SwiftKey checks passed after cleanup. Microphone use
ended after 2.605 s; cleanup pending was false. No insertion, sending, clipboard
operation, chat/view-tree traversal, screenshot, API call, audio retention,
permission change, or additional phone preparation occurred in these tests.
Final normal OFF assembly/unit/lint also passed and ordinary local APK output
was reset to OFF; the separately saved capture-only OVERLAY APK remains installed.

Andrew then explicitly chose: after review, tap the intended field, then Insert.
The accepted revision separates an unbound recording/review session from a new
explicit destination transaction. It must not preserve invalidated eligibility,
rearm sessions, or waive any captured-target event. Next staged tests remain
non-inserting; full acceptance is still pending. See IMPLEMENTATION for the
concrete design and PROGRESS for the current installed build.

## Independent recording Stage 1 — overnight checkpoint

Andrew selected review first, then tap the intended field and Insert, and asked
to stop at the next good checkpoint. Sol 6 Extra High implemented the debug
RESELECT first stage; Astra Extra High independently reviewed it. The final
review correction stops app-owned passive caret/composition processing in this
arm. No destination exists during recording. This does not exempt an event in
an existing destination transaction; later fresh destination checks must have
their full guards restored with phase-aware callback handling.

Final reviewed SHA-256 values:

- Service: `7F4B25B528B1A2E0C8D9B8D66A68DC8D96857CD5B18597DDB40E67752CC03705`.
- Panel: `5A8C4F0BC54C33B873BCF3FF9FB82A0B41C345E20F40AA464A7030C7ABB2E98D`.
- Scoped smoke: `00ABDE290C531E36E3D0CC4536F95AFCFF0D706236A72BAA9548572B1D345538`.
- Installed product: `C0CC7F44848E350D9F645565A3FE5CD2DDF760BF844A19F6585DBA5422C7D69B`.
- Installed smoke: `9130F582FBFBF486EC799531F6F972A30FFC47E41333699D5FFF9D7BE007C3B3`.

Candidate and default OFF assemblies, 29 JVM tests (zero failures/errors), lint
(zero errors, app 18/smoke 3 warnings), and signatures passed. Two new state
tests cover stale callbacks, repeated Stop, and recording ownership. Andrew
prepared the empty Kakao self-chat with SwiftKey visible; installation succeeded
and the service reconnected without new permission grants.

One exact `unboundRecordingOverThreeSecondsStopsReviewsAndCancels` gate with
sole `reselectReady=true` passed in 6.652 seconds. After positive own-recorder
reads and non-null unsilenced configuration, recording continued another 3.2s.
Stop showed the readonly fixed English sample; Cancel ended the session.
The same empty focused Kakao editor and selected/visible SwiftKey were verified
during recording, after the delay, at review, and immediately/1.2s after cleanup.
The product observed zero destination captures; external preservation reads
belonged only to the bounded smoke test.

Final own diagnostics: RESELECT_CANCELLED, one unbound recording proof, zero
destination captures, no cleanup pending, and ordered microphone proof -> Stop
entry -> proved stopped -> user Cancel. Exact-app RECORD_AUDIO AppOps showed
ended use of 4.176 seconds. SwiftKey remained selected and visible.

This is one successful bounded recording/readonly-review/Cancel flow. It does
not establish speech recognition, audio intelligibility, editable review,
translation, destination revalidation, insertion, long-session reliability,
or lock/interruption behavior on this device. All insertion and Copy paths are
blocked in this arm. No sending, chat-history traversal, screenshot, audio
retention, API call, or permission change occurred. Stage 2 was not started.

The tested APKs remain installed and saved under ignored
`app/build/reselect-stage1`; ordinary app-debug.apk output is OFF. Work stopped
at this checkpoint with tomorrow's next steps in PROGRESS and IMPLEMENTATION.

## Contextual bubble — 2026-10-24

Andrew prioritized idle visibility, dragging/persistence, and dismissal after
Copy or insertion before further capabilities. He also specified the launcher
app -> English review -> 요/반말 -> Copy & close fallback. This change covers
the contextual bubble in RESELECT; Copy/Insert remain blocked in that arm.
Explicit Hide exercises suppression but cannot establish completion dismissal.

Sol 6 Extra High implemented the initial policy and synthetic tests. Astra Ultra
handled the difficult coordinate/layout changes, and a separate Astra Ultra
reviewer checked ownership, geometry, metadata-only visibility, and final tests.
The idle observer never creates a destination or reads field text/selection.
Unknown observations hide idle UI without clearing Hide suppression. Active
recording/review retains its host independently of idle visibility and dragging.
Position is saved on completed drag; temporary bounds clamping does not rewrite
the preferred anchor. The expanded menu uses compact mode and footer rows.

Final reviewed source SHA-256:

- Service: `C93EB2441DB6CC5E094F1FFD915697FD3233C2307F42C6964FF4582933CBC982`.
- Policy/placement: `F0158C4AE085F0BE2B67CB5910C63FA7620582145F4648DE2E2EF726F7052399`.
- Final smoke: `6FDDACB214D35EE0B623E170ACB4F5BA9CB894D54ABC65BBA2F661A903C98820`.

Tested product SHA-256:
`7568BD4C21D0B7E66A232BCED5F6E4C48FD588195A576AA43F701713CB4E283B`.
Idle/restore smoke SHA-256:
`735770EB2312C235DA32F7475117A4B03369FF6BDF6E5A5363CEC921F1AB8F20`.
Final mic smoke SHA-256:
`598F1278742DCE9BA4EA5E2311A85D3C35EB85EAA65F51EADF9D8FE175DC0828`.
The final pair remains installed and saved under ignored
`app/build/bubble-lifecycle/RESELECT.apk` and `smoke.apk`.

Candidate/default OFF builds, 32 JVM tests (zero failures/errors/skips), both
lints (zero errors; app 20/smoke 3 warnings), and candidate signatures passed.
The three new unit tests cover Hide/unknown/IME policy and placement clamping.
Ordinary app-debug.apk output was reset to OFF without installing it over the
tested candidate. Prior APK checkpoints remain saved.

All phone methods below are exact opt-in methods in `BubbleLifecycleSmokeTest`.
They inspect own fixture/product nodes and IME window metadata, never chats.

| Gate | Measured result |
| --- | --- |
| `idleBubbleTracksImeHideCycleDragAndProtectedField` | Passed in 4.889s: bubble follows keyboard show/hide, Hide persists in the same IME session, verified hide/show rearms it, physical drag moves it without opening the menu, reopening restores it within 24px, empty password-type fixture hides it. |
| `savedPositionRestoresAfterProductServiceRestart` | Passed in 1.883s after reinstalling the exact same product without clearing data. Service PID changed 9275 -> 10466. Position restored within 24px of the saved test coordinates. |
| `draggedHostStaysPinnedThroughSyntheticStopAndCancel` | Passed in 4.105s: positive unsilenced microphone proof, same expanded host across fixture keyboard hide/show, Stop -> readonly sample -> Cancel, zero destination captures and no cleanup pending. |

Earlier idle runs failed because the test matched title-case Hide against an
Android uppercase button label. Fixed own-control diagnostics established that
Hide was present, and also showed a clipped Close footer; the selector was
corrected and the menu compacted before the successful gates. The first mic run
then stopped before recording on `EXPANDED_HOST_NOT_FOUND` (2.359s): it queried
Close immediately after clicking Ko. A reviewed test-only bounded wait fixed
that race; only the mic method was rerun, on the unchanged product.

Final independent AppOps read confirmed ended microphone use of 984ms. Own
service state was RESELECT_CANCELLED, proof count 1, destination count 0, cleanup
pending false, with ordered positive-unsilenced proof -> Stop -> proved stopped
-> Cancel. One service dump timed out; one bounded retry returned this state.
SwiftKey remained selected; the keyboard was hidden after fixture cleanup.
No insertion, Copy, Send/Enter, API call, retained audio, screenshot, permission
change, or conversation traversal occurred. Wispr was left enabled.

These are bounded portrait synthetic checks, not current-build real-app
acceptance or daily-use reliability. The mic run is short; the prior >3-second
Kakao result applies to its earlier Stage1 APK. Editable English review,
Copy/insertion dismissal, actual destination insertion, rotation, split-screen,
external-display/reboot behavior, and the fallback implementation remain open.
Phase 1 acceptance and the speech/API phase have not been reached.

## Editable English review checkpoint (2026-10-24)

Andrew authorized editable English review and dismissal after Copy or confirmed
insertion. Sol 6 Extra High implemented the separate debug REVIEW arm and smoke
tests; Astra Extra High independently reviewed them. Default/release OFF and the
recording-only RESELECT arm are retained. No API, credential, transcription, or
translation was added. English edits are real; Korean results remain fixed samples.

The product owns a transient unbound draft after proven microphone Stop, opens a
nonexported secure review Activity, then shows nonfocusable result controls.
No destination is captured until explicit Insert. Copy uses a sensitive clip and
clears the result only after the clipboard write returns. Insert reserves a single
attempt before IPC and requires exact same-destination postcontext/caret evidence.
Uncertainty permanently removes Insert for that draft; no edit/retry loophole.
Independent review required full identity/continuity checks, queued-event yields,
widened postcommit context, pending-Copy exclusion, and content-lifecycle cleanup.

Frozen reviewed source SHA-256:

- Service: `A0C2A77D1D381226F0318C880419A7DB83A0478F94A5B854294F2CF2130EC59C`.
- Review Activity: `663DEE26065EEE3786B1A0B1CBAF0FAC3A1BEF64A9A8BD927AD605A5AC79203B`.
- Draft state: `A386646D28D3D56937C30A744D9C72907067F54C761CF8C6274C80E1CB13F5E0`.
- Commit confirmation: `AF4B51822BD08E30ADC37298546D7FC9E5FF101BF399B7FC0399CDA9D5065A19`.
- Review smoke: `47FA422AEE0CC096096A9F838AD0345E726E1DB38DAC48958D4FD20323E1CC36`.
- Fixture: `78D125F9F07A0BD9E193BF4CA0E5E4A8D28D74DF35D2A2C5A37915AE92597C46`.

Installed product SHA-256:
`5387AA79E6FEBFE2E58977478AA915D1483DDBEE4BD3EA8486AC4F33BC0E0F68`.
Installed final smoke SHA-256:
`337DE462EBA993231F2BB232E03869005CE1FB91DC5B5FC6C064914C5C9664E9`.
Saved under ignored `app/build/review-stage2/REVIEW.apk` and `smoke-REVIEW.apk`.
The ordinary app-debug.apk output is OFF; do not substitute it for this candidate.

REVIEW and default OFF builds passed; 37 JVM tests passed with no failures/errors/
skips. Lint: zero errors, app 23/smoke 3 warnings. Signatures verified. Test-only
fixture diagnostics were subsequently rebuilt/linted and independently reviewed.

Initial fixture startup crashed with `Window couldn't find content container view`.
Publishing its Activity before setContentView and querying hasWindowFocus off-main
could race decor initialization (source-supported diagnosis). The test-only fix
publishes after layout setup and uses accessibility window metadata for focus.
It removed the crash. Subsequent startup timeouts (8.597, 8.550, 8.590, 8.558s)
occurred before recording/Copy. Owned lifecycle/window diagnostics established
RESUMED, attached, main-loop heartbeat, default display, visible/drawn surface,
but no focus callback and only two SYSTEM accessibility windows. WindowManager
classified the actual focus owner as NOTIFICATION_SHADE. Closing that panel
resolved startup with no product change. No notification content was inspected.
These were setup failures, not evidence of a REVIEW recording/Copy failure.

Exact methods in `ReviewFlowSmokeTest`:

| Gate | Measured result |
| --- | --- |
| `editedEnglishCopyClosesAndReappearsAfterImeCycle` | Passed in 6.454s. Positive mic proof -> Stop -> editable EN review -> own editor edit -> confirmation -> explicit Copy. Original synthetic draft preserved; zero destination captures/commits; expected sensitive clip checked. Bubble dismissed throughout the same IME session and returned after verified hide/show. AppOps mic duration 450ms, ended. |
| `editedEnglishInsertsIntoEmptyFixtureOnce` | Failed in 12.098s at SUCCESS_NOT_CONFIRMED. Its preceding exact-text wait passed: expected edited sample was observed in the synthetic field. One capture and one commit occurred, then product caret verification became terminally UNCERTAIN. Confirmation/dismissal assertions did not pass. |
| Other five REVIEW methods | Not run; deferred at the battery checkpoint. |

Empty-insertion fixed codes: precommit `OBS_VERIFIED_EMPTY_CARET` and
`OBS_READY_UNKNOWN` twice; then `FRESH_COMMIT_REQUESTED_ONCE`,
`CARET_EMPTY_PROBE_CN_NY_HY`, `OBS_NODE_CARET_MISMATCH`,
`FRESH_COMMIT_UNCERTAIN_TARGET_CHANGED`, and test cleanup `REVIEW_ENDED_USER_CANCELLED`.
CN means the connection was no longer verified empty-at-zero; NY means node
selection was -1/-1; HY means node text was empty or a hint. The generic
TARGET_CHANGED label does not establish an actual destination switch. Node/input
observation lag is plausible but unproven. No commit retry occurred. Final state:
REVIEW_CANCELLED, proof count 2, captures 1, commits 1, confirmation UNCERTAIN,
draft NONE, cleanup pending false; AppOps mic ended after 473ms. The test observed
expected text before waiting for confirmation; final post-cleanup field contents
were not separately checked. Do not count this as an Insert gate pass.

Andrew requested a stopping point because his LAPTOP battery was at 10% (not the
phone). No test/build remains running. Next session: one narrow synthetic
postcommit diagnostic of relative timing, target identity/generation, node and
connection caret data, lengths and expected-context match booleans only. Bounded
read-only observations after uncertainty may diagnose whether the node catches
up; they must not recapture, rearm, retry, or declare the ended transaction a
success. Keep the tested product frozen until a reviewed fix is justified.

SwiftKey stayed selected/visible; automated edits used the owned EditText action,
so actual keyboard keystrokes/prediction remain untested. No conversations,
screenshots, retained audio, Send/Enter, credential or paid API call were involved.
Copy replaced the clipboard with the disclosed fixed sample. Confirmed insertion,
middle/Korean safety gates, current-build real-app compatibility, and full Phase 1
acceptance remain open.

## Focused-node confirmation fix and final REVIEW gates (2026-10-24)

Andrew resumed the saved checkpoint to resolve insertion confirmation. All work
remained API-free and synthetic. Sol 6 Extra High implemented the diagnostic,
fix, and smoke changes; Astra Extra High independently reviewed exact sources.
Root alone built/installed/operated ADB. Real-app insertion scope was not reopened.

### Measured diagnostic

The bounded debug REVIEW diagnostic ran only against com.koscribe.smoke after
NODE_CARET_MISMATCH had already made the transaction terminally UNCERTAIN. It
stored transient expected-context digests and emitted only timing, identity,
generation, caret/length, and context-match codes. It never retried, rearmed,
or relabeled the failed attempt. Own postterminal overlay events were permitted
for observation only; this is not uninterrupted insertion-continuity evidence.

`editedEnglishEmptyInsertReportsNaturalConfirmationOrTerminalProbe` passed in
5.473s on diagnostic product `88577CC5396C4D66693CD3943EC59CF0B9F6D775D88DA34BE5AFEEC094ACDABE`
and smoke `36E14FD3448E3D61C199CF02526D3A8DA49CCD90B8809F9C77446BAE3284A5C0`,
saved under ignored app/build/confirmation-diagnostic. Reviewed service source:
`914D03E3CB9A74E526D1435A19205C7383A939C7BD56CCB1F5FF113D4D3EE546`;
review test source `0035D789EFA06A597ED986AA5D1ECE2204C9CA7C6D043BADBCBD3DD207E890F2`.

| Observation | Measured evidence |
| --- | --- |
| Original at +15ms | Same root/node/input generation; connection caret 42/42 and exact 42 UTF-16 unit expected context. Node still -1/-1 and hint length 19. |
| T0 +43 to +60ms | Same identities; discrepancy persisted before focused-node refresh. Root refresh 4ms; focused-node refresh 4ms supplied caret 42/42 and exact 42-unit node text. |
| T1 +210 to +221ms | Connection/node already consistent before refresh; root refresh 1ms, focused refresh 3ms. |

Stale focused-node metadata is the measured proximate cause in this run. Cache
versus commit/read ordering was not isolated. The diagnostic transaction remained
UNCERTAIN: one capture/commit, Cancel -> draft NONE, cleanup false. Exact-app
AppOps reported microphone use ended after 575ms.

### Reviewed fix and ready-control layout

REVIEW now refreshes the same focused node after reading connection context,
then revalidates transaction/window/root/node, input generation/EditorInfo, focus,
editable/enabled/protected/type state, and unlocked/interactive state. It retains
exact caret agreement, safe snapshots, sameFreshTarget, and CommitConfirmation.
Refresh failure after an attempted commit is terminal UNCERTAIN; no retry,
grace period, event exception, or success
from a void commit return. Android 16 creates a new connection wrapper per getter,
so wrapper identity is deliberately not used as a continuity check (see RESEARCH).

The first refresh-only product
`6C80B1CD9DB2BFB86416D4CAFF59254B056B54F5A032B5562F6E9804EE539022`
(source `12E12A324659FBC14F7E7147357FF79B99E0ABF25850991071580884230BE98A`)
passed empty insertion in 4.873s with POSTCOMMIT_FOCUS_REFRESHED and confirmed
bubble dismissal. Ignored commit passed terminal-uncertainty checks in 4.253s.
The long-result gate then stopped before insertion at READY_CONTROLS_NOT_VISIBLE
in 11.598s; captures/commits stayed 2/2 and mic ended after 627ms. Controls were
below the fixed 260x320dp scrolling panel's visible area.

Final product keeps a scrolling preview body and fixed Insert / Copy & close
row plus Cancel footer. UNCERTAIN has Copy and Cancel only. Same handlers, IDs,
nonfocusable secure window, host ownership, and safety checks remain. General
small-height/large-font/split-screen/rotation geometry is not yet tested.

Final reviewed source SHA-256:

- Service: `ABC4F0473E7F3FA77046DECFA1EB438220214357D03BFBBAD1BF93507989D48C`.
- Review test: `6514370531DD951DB03B589E89964F18DCC9C6AE3CAD480BFEA757DB1B2D3AEB`.
- Fixture: `A02ED4A44E82E3FEF90470885851C34A63EA2085550632CC75ADECF730B118B6`.
- Optional save-prompt helper: `765B585128FB6E482193DF70231FCADD1D4DA670C1F53F3FB539DDC14F644F5E`.
- CommitConfirmation and UnboundDraftState are unchanged from the earlier review.

Final product (P), installed throughout all seven passes below:
`2B00BE44077579BBD92D3D6A8D7FCED7E7391E17B05F56B5D1652444F4AD4D9E`.
Saved as ignored `app/build/confirmation-refresh/REVIEW.apk`.

| Smoke version | SHA-256 | Changes / saved filename under confirmation-refresh |
| --- | --- | --- |
| A | `F3093D1BC5466F1A688D4B061DE47BD019D7E97F5A3B5AA1C03C2CB19844E693` | Autofill opt-out; protected fixture still present. smoke-with-protected-field.apk |
| B | `309FE53C06C4155326D0E8B53649A28AE473B8E40BA646D98A57BF0F31C5A078` | REVIEW omits unused protected field; adds exact opt-in save-prompt negative helper. smoke-before-cancel-scroll.apk |
| C (final installed) | `FC0A13E58BF297AC49DD7E102384647D330644E6A43DD43FB0EC9F0D8AAD79AD` | B plus bounded same-control Cancel reveal and stronger closure assertion. smoke.apk |

All exact methods below are in ReviewFlowSmokeTest and ran separately with their
sole flags. These are seven acceptance-method passes on one product across
reviewed smoke versions, not a whole-suite run.

| Method | Smoke | Result on P |
| --- | --- | --- |
| editedEnglishCopyClosesAndReappearsAfterImeCycle | B | PASS 6.708s; edited sample copied with sensitive metadata, original fixture text retained, zero captures/commits for the flow, bubble hidden until verified IME cycle. |
| editedEnglishInsertsIntoEmptyFixtureOnce | B | PASS 4.372s; exact sample/caret confirmation, one capture/commit, POSTCOMMIT_FOCUS_REFRESHED, confirmed dismissal. |
| longEnglishInsertsAtMiddlePreservingKoreanAndEmoji | A | PASS 4.207s; long insertion preserved Korean/emoji surrounding text and confirmed dismissal. |
| fixedKoreanSampleInsertsIntoEmptyFixture | A | PASS 4.826s; exact fixed Korean insertion and confirmed dismissal. |
| selectedRangeRefusesAndPreservesResult | A | PASS 4.724s; selected text unchanged, insertion refused, result retained for explicit recovery. |
| koreanEditRejectsThenCancelPreservesFixture | C | PASS 4.012s; edited Korean source rejected, Cancel cleared draft, fixture/counters unchanged and own review nodes gone. |
| acknowledgedButUnchangedCommitIsTerminallyUncertain | B | PASS 4.538s; one attempted commit, no text change or confirmed success, terminal uncertainty with no second Insert, then Cancel. |

Ignored-commit runs ended with FRESH_COMMIT_UNCERTAIN_WINDOW_OR_FOCUS_CHANGED,
not the settled POSTCONTEXT_UNCONFIRMED timeout branch. They establish no-retry
behavior for that observed trigger; do not overstate branch coverage. Unit tests
separately cover unchanged-context rejection.

Before the long-middle pass, a 10.682s attempt stopped at STOP_NOT_AVAILABLE after
OVERLAY_MIC_SILENCED; proof/capture/commit counts were zero, mic ended after 1ms.
Andrew reported a phone call and authorized retry; the unchanged test passed.
Korean and selection startup attempts stopped before fixture focus in 8.538s and
8.563s with four SYSTEM windows. Andrew later cleared notifications; a subsequent
bounded focus check classified the fixture. Their cause is not conclusively
identified. No guard was weakened, and no conversation content was inspected.

The first Korean-edit Cancel gate on B failed in 11.850s at
OWN_CONTROL_NOT_FOUND_review_cancel after correctly rejecting the edit. Cancel
was below the own review viewport. C issues one ACTION_SHOW_ON_SCREEN on the
exact original hidden/enabled/clickable own Cancel node, enforces the same
node/window through bounded visibility checks, and clicks once. No generic
scroll, Back, coordinates, or other app navigation. Final assertions require
draft NONE, unchanged destination/commit counts, and absence of review controls
regardless of visibility, preventing an offscreen editor from falsely passing.

### Proton Pass and cleanup

Andrew reported repeated save-password prompts. No password was required or
entered; the fixture's empty protected editor is a plausible autofill heuristic
trigger, not a confirmed Proton diagnosis. Per-view/root opt-out did not prevent
recurrence. B/C omit that unused field only for REVIEW; the default protected
fixture remains for its explicit bubble exclusion gate. No Proton/global autofill
setting was changed. Prompt cessation is not yet established.

Andrew explicitly authorized clicking the visible negative save action. The
independently reviewed exact Android negative-button helper ran once on B in
0.038s and reported NOT_FOUND; no click occurred. It did not inspect proposed
credentials or vault contents. See INSTRUMENTATION for its sole opt-in scope.

Final own service: REVIEW_CANCELLED, recording proof count 8, draft NONE,
destination captures 5, commit requests 4, cleanup pending false. Last confirmation
UNCERTAIN belongs to the prior ignored-commit test; the final cancellation gate
added no captures/commits. Exact-app AppOps confirmed microphone use ended after
516ms. SwiftKey remained selected. No more tests/builds were left running.

Candidate/default OFF builds, 37 unit tests (zero failures/errors/skips), lints
(zero errors; app 23/smoke 3 warnings), and APK signatures passed. Final C
smoke-only build/lint passed after review, then only the affected cancellation
method reran. Ordinary app-debug.apk is OFF; it was not installed over P.

No real-chat traversal, Send/Enter, screenshots, retained audio, credentials,
paid/API calls, or permission changes occurred. Copy replaced the clipboard
with the disclosed synthetic sample. Automated review edits used the own
EditText action with SwiftKey visible, not actual keyboard keypresses. Real-app
insertion, manual typing/composition, navigation/recovery and general geometry
remain open Phase 1 gates. Do not advance to speech/API work on these results alone.

## Manual SwiftKey result and obstruction (2026-10-24)

User-reported behavior on product 2B00BE44... / manual-launcher smoke 5D9D7CC7...:
Andrew reached review, edited the fixed English with actual SwiftKey keys, and
confirmed. The large ready panel obscured the intended upper fixture field and
its controls. He tapped a separate Paste action and then KoScribe Insert, which
reported that it could not verify a safe destination. The pasted contents/outcome
were not inspected. Do not claim end-to-end manual acceptance or blame user error.

Own-state evidence after the report: proof count 9 (baseline 8), destination
captures 6 (baseline 5), commit requests unchanged at 4, confirmation REFUSED,
OBS_ACTIVE_COMPOSITION -> FRESH_INSERT_REFUSED_ACTIVE_COMPOSITION ->
REVIEW_ENDED_DEADLINE. Draft NONE; cleanup pending false; AppOps mic ended after
5.218s. No product commit occurred in this manual flow. The separate Paste action
was manual; its effect and relation to the composing span are not established.

Source review: REVIEW ignores selection callbacks outside freshDestination;
insertDraft resets callback composition before its first observeEditor, and own
product editors are rejected. This trace has no preceding ready observation or
second-check refusal marker, so the first observation saw SPAN_COMPOSING in the
current input connection. It is not stale own-review callback state. Keep the
composition guard. Next fix targets ready-panel obstruction and feedback only.

## Compact result candidate and visual checkpoint (2026-10-24)

Sol 6 Extra High implemented, Astra Extra High independently reviewed: normal
READY controls are 280x112dp, a mode-labelled 48dp Move handle and Cancel row,
then a 48dp Insert / Copy & close row. Refused/UNCERTAIN uses an extra 44dp bounded
notice, total 156dp; UNCERTAIN has no Insert. The title/result preview is omitted;
initial toolbar placement can still overlap the destination. Dragging uses the
saved clamped anchor and never clicks an action.
Window/IME reflow is metadata-only outside drag/recording/destination capture.
All original insertion handlers, guard, confirmation, and no-retry rules remain.
Small-height/large-font/landscape behavior is not generally accepted yet.

Reviewed final source hashes:

- Service: F3242BA505D0891F0E79D2EE9E792002582D3169E178B21248079537E741979A.
- Gradle: A91985E1FB31F68CDDF10480A788C3DB7AF6878E1D42832235774C51065BC060.
- VisualTestMode: 51ACB9A99F38DF23A4EDEC13B113CA55B61BB07498EF8A2C3308C899B7341A92.
- ReviewActivity: 2399DAAC3F3114F09E2A22EBA29E8516EFAF236D6A76FA191BB2B398D1DB3ED0.

Saved ignored artifacts under app/build/compact-result:

- REVIEW-secure.apk: 723D5D207AAF00F5F291033D0A28FA79C92D859C0BA9B7411B218A1EB0CD1287.
- REVIEW-visual-test.apk: C3A713C02EB97BEDA47182D267226EFD08F2CB37D99C72F90B33B63F01FFFDBA.
Smoke unchanged from the manual launcher APK 5D9D7CC7... . Secure/visual/default
builds and lint passed, 37 JVM tests passed, signatures verified. Current app lint
has zero errors and 25 warnings; smoke retains 3. Generated secure config is
REVIEW/false; visual is REVIEW/true; release and ordinary output are OFF/false.

First secure-candidate regression, editedEnglishInsertsIntoEmptyFixtureOnce,
FAILED in 13.149s at SUCCESS_NOT_CONFIRMED. Its preceding exact-text assertion
passed. Trace: two VERIFIED_EMPTY/READY_UNKNOWN observations, one commit request,
POSTCOMMIT_FOCUS_REFRESHED, ACTIVE_COMPOSITION, terminal UNCERTAIN_TARGET_CHANGED,
then testCancel. Capture1/commit1; draftNONE; cleanupfalse; mic ended after 617ms.
This is distinct from Andrew's earlier precommit refusal. Postcommit deliberately
ignores callback composition, so the refusal came from SPAN_COMPOSING in current
surrounding text. Identity/focus and caret checks passed before the refusal.
Layout timing may differ, but causation is unproven. No guard change or retry.
Remaining compact-candidate insertion/Copy/uncertainty regression gates are open.

Andrew then requested seeing the actual screen while he tested. Explicit debug
visual mode was built/installed for owned synthetic screenshots only. Default and
release capture protection remain; secure APK must be restored after the session.
A ready screenshot showed all four controls fully visible at the measured phone
font_scale 1.15, but the toolbar initially overlapped the upper field, while the
lower fixture field had focus. DraftREADY, proof1, captures0, commits0, cleanupfalse.
A later screenshot caught an incoming-call overlay. Visual/phone work stopped;
the local image was removed and no caller details retained. Andrew confirmed the
call ended and requested restart. The old draft had ended by DEADLINE, still with
zero captures/commits. A fresh manual drag-and-select check is pending; no dragging
or manual insertion pass is yet claimed. No conversation screenshots or real-app
insertion; the incidental incoming-call image was removed as described above.

### Visual session completion and next diagnostic (2026-10-24)

User report: the Move handle dragged the entire compact control into the blank
area. Andrew tapped both fixture fields, ending with the normal top field; he
mistook its "Synthetic text only" hint for residual content. The screenshot
taken after his reply already showed the three-minute expiry and a large mode
menu obscuring that field. It confirmed top-field focus after expiry, not the
pre-expiry dragged geometry. No manual insertion was requested or attempted.

Own state: REVIEW_CANCELLED, proof count2, draftNONE, captures0, commits0,
confirmationNONE, cleanupfalse. Both flows ended by deadline; latest microphone
use ended after 5.111s. Secure APK 723D5D20... was restored successfully and the
temporary screenshot removed. Visual C3A713C0... is no longer installed.

Next source changes: collapse non-completed review back to contextual Ko, and
give the debug manual launcher one clearly labelled ordinary field. Their device
behavior is not yet accepted. A separate bounded synthetic diagnostic will
observe the composition failure without changing confirmation or allowing retry.

### Secure composition diagnostic round (2026-10-24)

Sol6/xhigh implemented; Astra/xhigh independently cleared:

- Service F09C70C92574AC0FAF821902FFB31B06A1996B1F4D862A968E06BB3B78032FCB.
- ReviewFlowSmokeTest 299E595CE41AD8E90872365D34439F2B24D8CA7FC9BD231E49FD78401912E2CA.
- SmokeEditorActivity BE2DCDE070A7365D71C07362B4C238E06C6ACEE04A370B62C9606E8819E02D7C.
- DebugFixtureLauncherActivity C96196D52D6A805E92FDF6D3D5909BB1186FCA57908F59E4E579DDFF6BC9ABE5.

All composing/identity/context/caret/one-attempt checks are unchanged. The new
diagnostic logs a same-observation identity/context/caret conjunction and capped
numeric span metadata before the existing refusal, then samples terminal state
at nominal0/150/500ms. It cannot rehabilitate a transaction. The750ms delayed-read
admission cap does not bound Android IPC completion time. Debug REVIEW and the
owned synthetic package are required; no diagnostic text or hash output.

Saved and installed under ignored app/build/composition-probe:

- REVIEW-secure.apk: 980E4299E1B73160E82D874139A7CE04FBA46603DC32A184E0FBF50D5D627D0D.
- smoke.apk: 9D3F9563C8A3A21998EC1DD6A067921FD65868CE9F140C26F1BC2CC9CF8F361D.

Secure REVIEW/default OFF builds passed,37 JVM tests passed with zero failures,
errors or skips; lint zero errors (app25/smoke2 warnings); signatures verified.
Generated diagnostic config REVIEW/visual false; ordinary output and release
OFF/visual false. Ordinary output was not installed over the secure candidate.

Two exact fresh runs of
editedEnglishEmptyInsertReportsNaturalConfirmationOrTerminalProbe, sole flag
reviewConfirmationProbeReady, both returned NATURAL_CONFIRMED:5.370s and4.638s.
Each passed exact fixture text, confirmed dismissal, one capture/commit, stable
state/text after500ms, and SwiftKey visible. Own traces showed focus refresh,
READY_UNKNOWN and INSERT_CONFIRMED; no composition refusal/probe fired. Mic ended
after610ms and622ms respectively. The two-run diagnostic round is closed: this
does not reproduce or explain the intermittent composition failure, and does
not establish a root-cause fix or manual/real-app acceptance.

Current Copy regression editedEnglishCopyClosesAndReappearsAfterImeCycle passed
in5.853s: fixed edited sample copied, original fixture text preserved, no capture/
commit delta, dismissal until a new IME cycle. Final own baseline proof3,
captures2/commits2, confirmationCOPIED, draftNONE, cleanupfalse; mic ended542ms.
No Send/Enter, real-app field reads, screenshots, retained audio or API calls.

The manual launcher opened the new one-field fixture successfully. Andrew was
asked to edit the fixed sample with actual SwiftKey keys, Confirm, briefly select
the single destination and Insert once. Await his report before phone actions;
no manual pass or expiry-collapse device pass is claimed yet.

### First single-field manual report (2026-10-24)

Andrew reported reaching the result box, moving it by the handle, then seeing
Paste/ellipsis/Autofill beside the destination cursor. He selected Paste and
recognized the earlier synthetic Copy sample rather than his current result.
No field or clipboard contents were read by the agent. Own state immediately
after his reply: REVIEW_READY/draftREADY, proof4, captures2/commits2 unchanged,
cleanupfalse, mic ended4.639s. COPIED was retained from the previous test; no
new Copy event or destination capture/commit occurred. This is not an insertion
failure or success: the KoScribe Insert action was not reached.

The user then clarified he may have looked for Insert in the wrong place and
requested a retry. Source review confirms READY always creates the separate
Insert button, but no current bounds/screenshot evidence proves it was visible
at that instant. Height-capped fixed rows can clip if available space is below
112dp (156dp with notice); that condition was not measured here. Automated
isVisibleToUser checks do not prove complete visual exposure or lack of occlusion.

Root confirmed draftNONE/REVIEW_ENDED_DEADLINE with unchanged counters before
resetting only com.koscribe.smoke's transient fixture. The single-field launcher
reopened successfully. New instructions explicitly identify the floating box's
two rows and distinguish Insert from Android Paste/Autofill. Await new report;
no code/build change, guard relaxation, real-app access, or screenshot occurred.

### Manual SwiftKey edit and confirmed insertion PASS (2026-10-24)

Same secure product980E4299... and smoke9D3F9563...; no source/APK changes. Andrew
reported using actual SwiftKey to edit the fixed English review, confirming it,
briefly tapping the destination, then finding and pressing Insert in the result
box. The edited sentence appeared and the box disappeared. He noted the cursor
was already in that field but did perform the instructed field tap.

Own-state verification after the reply: REVIEW_COMPLETED; proof5 vs baseline4;
captures3/commits3 vs baseline2/2; CONFIRMED; draftNONE; cleanupfalse. New trace:
two VERIFIED_EMPTY_CARET/READY_UNKNOWN observations, FRESH_COMMIT_REQUESTED_ONCE,
POSTCOMMIT_FOCUS_REFRESHED, OBS_READY_UNKNOWN, REVIEW_ENDED_INSERT_CONFIRMED.
Exact-app AppOps confirmed microphone use ended after5.318s. No extra insertion,
Copy, Send/Enter, screenshot or field-content read was performed by the agent.

This accepts the specific manual EN edit/Insert/dismissal flow in the synthetic
field. It does not resolve the earlier intermittent composing-span failure or
establish real-app, middle-text, navigation, arbitrary geometry, or all SwiftKey
prediction behavior. The two prior automatic diagnostic attempts remain closed.

User proposal: skip the extra field tap if the intended cursor is already there.
This run did not exercise that variant. Keep fresh destination capture at Insert
and every existing guard; record a separate result before accepting it.

Astra/xhigh source review: insertDraft requires no recent destination-tap event
or token. Insert itself creates the fresh capture, with repeated current-editor
observations and one reserved attempt. Thus restored focus may be sufficient;
the user still needs to verify the intended field and caret. Proposed test:
confirm English, check restored intended cursor, then tap floating Insert once
without retapping the field. Stop on refusal/uncertainty; no retry or guard change.

### Manual restored-focus insertion PASS (2026-10-24)

Andrew explicitly authorized omitting the field tap after Confirm English.
Root verified the previous draft had ended, SwiftKey remained selected and the
phone was unlocked, then reset only the synthetic fixture to its empty field.
No source/APK change: secure980E4299... / smoke9D3F9563... remained installed.

User report: the instructed edit/Confirm flow went as expected, floating Insert
entered the edited text and the box disappeared without another destination tap.
This physical interaction is user-reported; the agent did not watch a screenshot
or read the resulting field. Own-state evidence independently confirms proof6
(baseline5), captures4/commits4 (baseline3/3), REVIEW_COMPLETED, CONFIRMED,
draftNONE, cleanupfalse. New trace: two VERIFIED_EMPTY_CARET/READY_UNKNOWN
observations, one FRESH_COMMIT_REQUESTED_ONCE, POSTCOMMIT_FOCUS_REFRESHED,
OBS_READY_UNKNOWN, REVIEW_ENDED_INSERT_CONFIRMED. Microphone use ended3.415s.

Astra/xhigh independently assessed the evidence as a pass for this manual empty
synthetic restored-focus case. It does not establish focus restoration in other
apps, nonempty/middle insertion, arbitrary cursor/selection changes, or a fix for
the earlier intermittent composition failure. No cached destination, guard
relaxation, retry, Copy, Send/Enter, screenshot, API call or additional agent
phone mutation occurred after the report.

Conditional instruction: check the intended field/caret, tap the field only to
select it or reposition the cursor, then tap floating Insert. All current focus,
identity, context, caret, composition and one-attempt checks remain in force.

### Current REVIEW Kakao Stop/Cancel PASS (2026-10-24)

Same secure980E4299... / smoke9D3F9563... . Andrew opened the prepared self-chat,
selected EN, stopped, saw the fixed sample and cancelled in review. He reported
return to the Kakao composer cursor with SwiftKey visible, explicitly no Confirm,
Insert, Copy or other result action. Agent did not read field/chat contents or
take screenshots. This is user-observed app return, not an independent text read.

Own proof count6 -> 7; captures/commits stayed4/4; REVIEW_CANCELLED, draftNONE,
cleanupfalse. New events: modeEN, positive unsilenced mic read, Stop, mic proved
stopped, REVIEW_ENDED_USER_CANCELLED. AppOps microphone duration5.265s ended;
SwiftKey remains selected. Retained CONFIRMED belongs to the preceding synthetic
insertion, not this cancellation. This accepts current recording/review/Cancel
ergonomics only. Audio was read/discarded; no transcription/API request occurred.

### Middle insertion reproduces persistent postcommit composition (2026-10-24)

Root next ran exactly
ReviewFlowSmokeTest#longEnglishInsertsAtMiddlePreservingKoreanAndEmoji with sole
reviewInsertMiddleReady on the same installed builds. FAILED12.853s at
SUCCESS_NOT_CONFIRMED after the exact expected fixture-text assertion passed.
The installed code-only diagnostic fired naturally; no rerun of the closed two-
attempt empty-field diagnostic round. No retry or success reclassification.

| Observation | Measured result |
| --- | --- |
| ORIG29ms, before unchanged composing refusal | I1/CX1/CK1/NK1/NA1: complete same-target/host identity and exact expected context/caret/node agreement. One span115..121; not zero-length; no truncation/error. |
| T0 57-67ms | Same root/node/generation/editor. Offset0, selection121/121, context length125, exact expected context/caret. Initially stale node4/4/length8; refreshing gave121/121/length125 and exact text. Span115..121 remained. |
| T1 207-217ms | Same checked identities, exact context/caret/node values; span115..121 remained. |
| T2 557-572ms | Same checked identities and exact values; span115..121 remained, then POST_COMPOSE_DONE. |

Later samples are postterminal and do not prove uninterrupted original transaction
continuity. ORIG supplies the previously missing conjunction before refusal.
The producer of the composing marker is still not identified; do not assert a
specific SwiftKey call or future text stability from these observations.
Mapping against the fixed synthetic string: prefix4 + result117 + suffix4 =125
UTF-16 units, insertion interval[4,121). The composing interval[115,121) is the
new result's final word plus punctuation and does not include the old suffix.

After test Cancel: proof8, captures5/commits5 (one new capture/commit), UNCERTAIN,
draftNONE, cleanupfalse; mic ended624ms. No real-app insertion or conversation
reads. Root stopped the remaining four planned synthetic methods and held the
Kakao insertion proposal. Prospective confirmation-policy discussion is required
before any behavior change; precommit guards and terminal no-retry stay intact.

### Approved composed-result confirmation PASS (2026-10-24)

After the evidence above, Andrew explicitly approved prospective exact-result
confirmation despite postcommit composing metadata. Sol6/xhigh implemented the
product/unit change and deterministic synthetic fixtures; Astra/xhigh independently
cleared both at these frozen source hashes (SHA-256):

| Source | Hash |
| --- | --- |
| KoScribeService.kt | `7875453B744A821C0D02FCC37B9423D5B3757ED4DA860D17323998F03F91653F` |
| CommitConfirmation.kt | `CFD0EAEF81126FDE9C501338EBAD101D0093621ABFA9FF16A7A13C6E3F31C7B0` |
| SessionGuard.kt | `7115659A114119B05702662379293704591F6D0D7810B8BAFC3577FBC22F3A12` |
| CommitConfirmationTest.kt | `56F5635D5D9A5C949C39AC90470F1CCF14672465B2329E3A49C67D0CE1E0442D` |
| SessionGuardTest.kt | `2DF46FEF83543D81870D3C88D51544101F05901AFA743CF0406BD40CC61D0C98` |
| SmokeEditorActivity.kt | `0819C300249A46EA33CE9ABFE94082A53C09C09C9A3441AAAD590A04CCE37B67` |
| ReviewFlowSmokeTest.kt | `974117590F1D7C3E0CF6A0A475B6BEED74FD92FF03A6944193BF4059E8C63D3C` |
| smoke ids.xml | `71303AE3BA182E6832E01F36B031815207910065C50EE18942ADC3129A40FD58` |

The original destination/host/session, exact expanded surrounding context,
preserved prefix/suffix, refreshed focused node and collapsed expected caret
remain mandatory. Precommit composition is still refused. Styled reads and the
actual composing flag remain intact. A composed nonmatch immediately ends as
uncertain; ordinary noncomposing unchanged-text polling is unchanged. Ended
transactions cannot be upgraded or retried. Confirmation establishes an observed
insertion, not immunity to subsequent keyboard/user editing.

Built and signature-verified secure REVIEW/visualfalse candidate and smoke, saved
under ignored `app/build/composed-confirmation/`, then installed on unlocked A35:

- `REVIEW-secure.apk`: `3AA18446275E46E3712D5232EC8D02DC4AEF0D3C1D88D7CE7CAECE6080DF99DD`.
- `smoke.apk`: `168091D7000C2CB26FD94461A52BFAA316218B3B5B4ACDD2F5728433DAF2139D`.
- All40 JVM tests passed, zero failures/errors/skips. Lints: zero errors,
  app25/smoke3 warnings. Ordinary default build and release configuration were
  restored and verified OFF/visualfalse after saving the candidate. Installed
  APK remains secure REVIEW; no visual mode was used.

Root ran each exact opt-in method separately, inspecting its result and own
service/AppOps state before continuing. Counts restarted at installation.

| Method | Result/time | Proof/captures/commits after | Confirmation / mic ended |
| --- | --- | --- | --- |
| `exactComposedCommitConfirmsAndDismissesOnce` | PASS6.060s | 1/1/1 | CONFIRMED /576ms |
| `wrongComposedCommitStaysTerminallyUncertain` | PASS5.332s | 2/2/2 | UNCERTAIN then Cancel /592ms |
| `longEnglishInsertsAtMiddlePreservingKoreanAndEmoji` | PASS5.105s | 3/3/3 | CONFIRMED /649ms |
| `fixedKoreanSampleInsertsIntoEmptyFixture` | PASS4.795s | 4/4/4 | CONFIRMED /558ms |
| `selectedRangeRefusesAndPreservesResult` | PASS4.666s | 5/5/4 | REFUSED then Cancel /490ms |
| `koreanEditRejectsThenCancelPreservesFixture` | PASS3.469s | 6/5/4 | No new capture/commit; Cancel /475ms |
| `acknowledgedButUnchangedCommitIsTerminallyUncertain` | PASS5.069s | 7/6/5 | UNCERTAIN then Cancel /576ms |

Both deterministic composing cases required POSTCOMMIT_COMPOSING_OBSERVED
after their current commit plus one product and one fixture commit. Exact
confirmed and dismissed with stable expected text; wrong reached
FRESH_COMMIT_UNCERTAIN_COMPOSED_POSTCONTEXT_UNCONFIRMED with no Insert/edit
recovery, retained explicit Copy/Cancel, then cleaned up by Cancel. No retry.

Middle preservation passed without a composing marker on that current commit;
it is not a reproduced composed-middle success. The Korean insertion did observe
composition naturally before confirmation. Selection refused specifically at
SELECTION_NOT_COLLAPSED without a commit. The edited-Korean Cancel case retained
the previous REFUSED status; it did not itself attempt destination capture.
The unchanged-commit negative observed unchanged context five times before
FRESH_COMMIT_UNCERTAIN_POSTCONTEXT_UNCONFIRMED, proving the timeout branch that
earlier WINDOW_OR_FOCUS_CHANGED runs did not exercise.

Every method ended draftNONE/cleanupfalse. Final REVIEW_CANCELLED, proof7,
captures6/commits5, UNCERTAIN from the deliberate negative, mic ended576ms;
SwiftKey remained selected. No real-app field/history reads, screenshot,
Send/Enter, clipboard action, credentials or API use in this round.

Astra/xhigh reviewed the measured evidence and cleared proposing one new manual
unsent Korean self-chat trial. That specific authorization is still required:
the old trial is closed, and the confirmation approval grants no real-app
insertion. The old980E failure remains failed. These bounded passes establish
the tested branches, not general app/composition compatibility or Phase1 completion.

### Current secure REVIEW manual Kakao insertion PASS (2026-10-24)

Andrew separately approved one new unsent insertion of the fixed polite Korean
sample into an initially empty Kakao self-chat field. Same secure3AA18446 and
smoke168091D7; no rebuild. Before instructions: connected/unlocked, SwiftKey
selected, proof7/captures6/commits5, draftNONE/cleanupfalse, mic stopped. The
retained UNCERTAIN at baseline belonged to the preceding deliberate negative.

Procedure: Ko -> 요 -> brief recording -> Stop -> unchanged English sample ->
Confirm English -> check intended field/caret, tap if needed -> floating Insert
once. No Copy/Paste, Send, or retry. User reported that it worked exactly as
instructed, the expected "안녕하세요. 10분 후에 도착할게요." appeared correctly,
and it remained unsent. His response answered the combined exact-text/dismissal
question; root did not independently view the field or conversation.

Own verification: proof8/captures7/commits6, REVIEW_COMPLETED, CONFIRMED,
draftNONE/cleanupfalse. Current trace: POLITE, positive unsilenced mic read,
Stop/MIC_PROVED_STOPPED, two VERIFIED_EMPTY_CARET observations, one
FRESH_COMMIT_REQUESTED_ONCE, POSTCOMMIT_FOCUS_REFRESHED, OBS_READY_UNKNOWN,
REVIEW_ENDED_INSERT_CONFIRMED. AppOps microphone duration5.450s ended; SwiftKey
unchanged. The composing diagnostic marker is synthetic-only, so its absence
does not establish absence of composition in Kakao.

Accept one guarded empty-field Korean insertion and dismissal in Kakao with
user-confirmed unsent text. Extra field-tap use was not separately reported.
No agent screenshot, chat/field-content inspection, UI mutation, API, or secret
use. The one-attempt scope is now consumed, not permission for repetition.
The fixed draft is left for Andrew to remove manually. Other apps and broader
editor states remain pending; this alone does not complete Phase1.

### Google Messages manual insertion PASS; Instagram pending (2026-10-24)

Andrew authorized one manual unsent fixed Korean insertion per Google Messages
and Instagram. Google Messages ran first with the same 요/Stop/unchanged-English/
Confirm/explicit floating Insert procedure and secure3AA18446/smoke168091D7.
Baseline proof8/captures7/commits6, draftNONE/cleanupfalse, stopped microphone.
User reported everything worked as expected and insertion into the message field,
replying to the combined exact-sample/dismissal question. He described addressing
himself; actual personal Send status is unspecified. No automated Send occurred.

Own proof9/captures8/commits7, current CONFIRMED/REVIEW_COMPLETED, draftNONE,
cleanupfalse. New trace: POLITE -> positive unsilenced mic -> Stop/proved stopped
-> two VERIFIED_EMPTY_CARET -> one FRESH_COMMIT_REQUESTED_ONCE -> focused-node
refresh -> READY -> INSERT_CONFIRMED. Microphone use ended5.100s, SwiftKey selected.
Agent did not inspect field/chat text, take screenshots, or mutate phone UI.
This accepts the reported empty-field Google Messages flow, not arbitrary SMS/
RCS states or content. Its one-attempt scope is consumed.

Instagram instructions sent next using baseline9/8/7; outcome pending. No new
approval required for that already authorized one-attempt scope. Stop on refusal/
uncertainty/interruption; no retries or Copy/Paste substitute.

Astra/xhigh audited the remaining focus-change gate. Earlier REVIEW ignored-
commit tests on2B00 ended via WINDOW_OR_FOCUS_CHANGED and proved no retry (see
earlier seven-pass checkpoint). Historical modal leaving-review tests are weaker
evidence for the revised transaction. Current units cover changed target/caret/
context, invalidation and one attempt; current source retains input/event
invalidation and sameFreshTarget/host checks before the postcommit verdict.
The approved composition change did not remove those checks. Therefore a new
target-change fixture is not mandatory solely because the build changed. Finish
the planned app round and decide Phase1 from combined evidence. Actual same-view
conversation switching/back, lifecycle and broad geometry coverage remain
explicit limits and future hardening; no pass is inferred for those cases.

### Instagram manual insertion PASS and Phase1 decision (2026-10-24)

Same secure3AA18446/smoke168091D7; already-authorized one-attempt empty-field
procedure, baseline9/8/7. Andrew reported the flow worked perfectly and expected
Korean appeared in the proper Instagram DM field. This answers the combined
exact-text/dismissal question; root did not view/read the conversation. Sending
was not requested or performed by KoScribe/agent; no extra user Send claim is made.

Own proof10/captures9/commits8, current CONFIRMED/REVIEW_COMPLETED, draftNONE,
cleanupfalse. New POLITE -> positive unsilenced mic -> Stop/proved stopped ->
two VERIFIED_EMPTY_CARET -> one FRESH_COMMIT_REQUESTED_ONCE -> refreshed focus
-> READY -> INSERT_CONFIRMED. Mic ended3.087s; SwiftKey remained selected. All
three real-app scopes are consumed. Agent made no UI mutation, screenshot, or
conversation read; only own diagnostics/AppOps/keyboard metadata were checked.

Decision: bounded Phase1 feasibility gate met using the three app happy paths,
synthetic preservation/refusal/composition/uncertainty and manual editing passes,
plus earlier REVIEW focus-event termination and current guard units/source
review. No additional fixture merely due to a new build. Broader lifecycle,
geometry, same-view switch-back and daily-use compatibility remain explicit
limits. Proceed to separately reviewed Phase2 speech/credential implementation;
this is not transcription, translation, or secret-storage device acceptance.

### Phase 2 speech setup candidate (2026-10-24)

Sol6/xhigh implemented private key storage and EN speech; Astra/xhigh reviewed
the final source and subsequent narrow fixes. The candidate is explicit debug
REVIEW/SPEECH true, visualfalse. Ordinary/release outputs are restored to OFF,
SPEECH false, visualfalse. No actual API credential or paid request has been used.

Build, 53 unit tests (zero failures/errors/skips), and lint passed. App lint has
37 warnings, smoke 3, neither has errors. New units cover WAV limits/poisoning,
real EN draft handling, cancelled/stale transfer, terminal-result races, strict
bounded response parsing, multipart contents, and local 302/503 no-replay tests.
Network tests used a local mock server, never OpenAI. Review corrected backup
path syntax to `.` and hardened persisted-key verification, reader cleanup,
whole-call/upload timeouts and outer multipart one-shot behavior before device use.

Saved ignored products:

- SPEECH-secure.apk: `90E5C6731C3F28F92F133C985BA8992E7574D6991C6E9077138445C343D069FA`.
- SPEECH-secure-v2.apk: `3A69C23004D41983325C0610E5DAB6ED3090ABFFF1E6A6550F860CB80D8F3B3D`.
- SPEECH-secure-v3.apk: `F2079E8AE1181930911E7820AD4A735A56911F1C122FE06CA76DC594E0F811F9`.

All are under `app/build/speech-input/`; v3 is installed. v2 moved API key settings
above the main diagnostic editor; v3 retains the clicked bubble host during the
asynchronous key precheck, without suppressing idle refresh or weakening host/
token/generation/lock checks. Its code-only notices distinguish missing key from
stale precheck host. Final KoScribeService source SHA256:
`A6270638B0820EC18CDC5AA7DED6BCCEE6CF2C4A138D2821C4DC9420B34D77E4`.

The fake-key test refuses any preexisting entry, saves a fixed noncredential
marker, verifies no readback after reopening, deletes it and verifies empty state.
No microphone action occurs while the marker exists. Initial v1 run failed before
key entry at the inaccessible settings button (10.152s). After the v2 layout fix,
a run passed in3.843s, but harness install timing made that run's exact harness
provenance ambiguous. A deliberate repeat failed before mutation because Android
returned to the existing key Activity instead of the expected main Activity
(10.133s). The test now accepts either own landing screen and closes its final
empty settings screen; it still requires an empty credential store.

Final credential check PASS1.123s on product v2 and smoke-setup-v2.apk,
SHA256 `DCFBC1566AE075CD4342D06CB79634DBCB99D0D31EA17A6F7089B4B80BAA9EB9`.
CredentialFlowSmokeTest source SHA256:
`33A83428417AA9DEA55FA61A1B19F8AA17B242BFDEE7ED5B9B03B18DBE43BEEE`.

The first missing-key check failed after Ko -> EN at the missing notice (12.807s).
Own proof/capture/commit remained0, draftNONE/cleanupfalse, with no new AppOps
recording. Source review found the precheck redraw could replace its original
host/token before callback. v3 removed that redraw only. Its retry observed
SPEECH_KEY_MISSING and passed all no-recording/no-insertion assertions, then failed
the offscreen Close cleanup (14.025s). Thus the product assertions passed, but the
whole test did not. The test-only correction reveals the unique own Close control
with ACTION_SHOW_ON_SCREEN, rechecks visibility/enabled/clickable, then clicks;
ambiguity refuses. No global Back, coordinates, or conversation UI are used.

Corrected smoke-setup-v3.apk SHA256:
`CC88532D49E43634BB5E585AD9F21E0EE50ABD5616C81852D63950E7514EB948`.
SpeechSetupSmokeTest source SHA256:
`C4B8BE1DE46FE23DC6EEB433E5B06961E1B14E64E5351520A275FEDF8F72F0C7`.
Both the product fix and test-only cleanup correction passed independent source
review. That retry again passed the notice and all state assertions, but failed
OWN_CLOSE_NOT_FOUND (13.885s): the offscreen node was not exposed at all.

A final test-only bounded fallback selects the unique visible/enabled own
ACCESSIBILITY_OVERLAY ScrollView, retains its window ID, attempts at most three
forward scrolls and clicks only the fresh exact visible Close. Source review and
build/lint/signature checks passed. Final smoke-setup-v4.apk SHA256:
`2782686D4D4BAA5E878CDF1B22950F5EBFA0D3C6098AF4E239A892791B505B0C`.
SpeechSetupSmokeTest source SHA256:
`E9D1009367C0C8F34EA3719380EB93CC597A9D66608E1516DB5D2693862ADC90`.

Final method FAILED11.103s at OWN_OVERLAY_SCROLL_REFUSED, again after emitting
MISSING_KEY_REFUSED_BEFORE_RECORDING and passing all core assertions. Do not
describe any of these three full methods as passing. Stop cleanup retries;
actual user Close behavior remains unverified. Own final state NOT_CAPTURED,
proof/captures/commits0, draftNONE, confirmationNONE, cleanupfalse and three fixed
SPEECH_KEY_MISSING notes. AppOps still shows the old Instagram3.087s recording,
no new microphone use. No actual key/API/network/insertion or conversation read.

Independent reviewer considers the fake-key pass, repeated missing-key safety
assertions, 53 units and source review sufficient for private key-entry setup,
with cleanup failure explicit. This does not accept ASR accuracy, paid behavior,
real-audio edit/insert, or Phase 2 overall. First speech test is user-triggered in
our synthetic editor; prior real-app insertion scopes do not carry over.

### First actual English transcription and review Cancel PASS (2026-10-24)

Same secure speech v3 F2079E8A and smoke v4 2782686D; no code/build change.
After private key/backup confirmation, Andrew deliberately recorded the short
test sentence and tapped Stop. He reported the actual transcript was correct
and the requested test passed. It differed from the old fixed arrival sample.
No Confirm, insertion, Copy or Send was requested in this case.

Own baseline proof0/captures0/commits0 -> proof1/captures0/commits0;
REVIEW_CANCELLED, draftNONE/cleanupfalse, confirmationNONE. Trace includes EN,
positive unsilenced microphone, Stop, and REVIEW_ENDED_USER_CANCELLED.
AppOps confirms the recording ended after29.097s; this is microphone duration,
not transcription latency. SwiftKey remained selected. Root inspected only own
codes and device state, not transcript/audio/key or conversation content.

User found the recording status hard to notice after tapping EN and spent time
looking for it before realizing recording had started. He explicitly deferred
clearer/prominent recording feedback and menu polish to later. Record that
usability issue; do not reinterpret this one accuracy/Cancel pass as UI acceptance,
general ASR accuracy, measured latency/cost, edit/insert acceptance or Phase 2 closure.

### Real speech, SwiftKey edit and guarded Insert PASS (2026-10-24)

Same secure speech v3 F2079E8A and smoke v4 2782686D. Andrew reported that the
complete dictated-and-edited text appeared in the synthetic field and the box
disappeared. Own REVIEW_COMPLETED, draftNONE, captures0->1/commits0->1,
CONFIRMED, cleanupfalse. Trace contains two VERIFIED_EMPTY_CARET observations,
one FRESH_COMMIT_REQUESTED_ONCE, focused-node refresh, then INSERT_CONFIRMED.
Proof1->3 includes an intervening EN recording -> Stop -> USER_CANCELLED before
the successful recording. Its reason is unspecified; no extra insertion occurred.
Latest microphone use ended4.368s; SwiftKey remains selected. Root inspected
only own codes/AppOps/device state, no transcript, key, screenshots or conversations.

Usability finding from Andrew: Stop was half clipped at the bottom of the
recording panel but still tappable. The functional test passes; control visibility
does not. Narrow proposed repair pins speech Stop/Cancel outside scrolling text
inside the same captured host. General recording-status prominence/menu polish
remains deferred as requested. Review/build and a manual recording-Cancel layout
check are required before claiming the clipping corrected.

### Speech controls v4 installed; manual visibility/Cancel pending (2026-10-24)

Sol6/xhigh changed only OverlayRecordingPanel: speech explanatory content gets a
weighted ScrollView above a fixed56dp Stop/Cancel row (52dp buttons), inside the
same captured host. Stop appears after microphone proof and disables on first tap;
Cancel remains through preparation/recording/processing. API-free sample layout
and recorder, join/upload, credentials and insertion paths are unchanged.
Astra/xhigh independently cleared source SHA256:
`6CB0ACBE2691C8028DACE729FD6A531F31456156D6E9195E02E6FCF6581E03B1`.

Candidate build/signature passed, all53 units passed with zero failures/errors/
skips, app lint zero errors/37 warnings. Saved and installed via replace-in-place:
`app/build/speech-input/SPEECH-controls-v4.apk`, SHA256
`524133623674E42B4BE8E5745C92BAD85A649D1C46AD46EF9181441858461F05`.
No uninstall, data clearing, key access or new permission. Smoke v4 unchanged.
Ordinary debug/release output restored and verified OFF, visualfalse/SPEECHfalse.

Service reconnected idle: NOT_CAPTURED, proof/captures/commits0, draftNONE,
confirmationNONE, cleanupfalse. Phone unlocked; owned Test message launched.
Asked Andrew to tap field -> Ko -> EN, inspect both buttons without scrolling,
then Cancel directly without Stop. No speech/upload/review/insertion is requested.
Await report and own microphone/zero-commit verification. Source review alone
does not establish visibility on the phone or larger-font/smaller-window layouts.

### Controls v4 visibility and active-recording Cancel PASS; night stop (2026-10-24)

Andrew confirmed both Stop and Cancel were completely visible without scrolling
and the requested direct Cancel flow worked exactly as described. This answers
the combined no-review/no-text-change question. Same controls v4 52413362 and
unchanged smoke v4 2782686D; no new build or agent UI action after his report.

Own baseline proof0/captures0/commits0 -> proof1/captures0/commits0;
RESELECT_CANCELLED, draftNONE, confirmationNONE, cleanupfalse. Trace shows mode
EN, positive unsilenced microphone and USER_CANCELLED, with no Stop entry.
AppOps confirms recording ended8.803s; SwiftKey remains selected. Direct Cancel
does not hand audio to transcription. No transcript, key or conversation was read,
no screenshot taken, and no insertion or sending occurred.

Accept current A35 button visibility and active-recording Cancel/cleanup.
Processing cancellation, broader geometry and network recovery are not inferred
from this pass. Andrew requested stopping for the night; no further tests or UI
actions. Tomorrow's remaining scoped Phase 2 gate is deliberate network failure
and recovery with Andrew controlling connectivity, then phase acceptance review.

### October 25 service readiness recovery; offline test prepared

Resume from clean0b0889a with unchanged installed controls v4 52413362. Initial
USB authorization required Andrew's approval. Three own service dumps timed out,
including one after his first accessibility toggle; same old process remained.
Mic AppOps showed ended use, SwiftKey selected. Exact own PID/UID verified; one
SIGTERM under app UID ended only that process, without force-stop/uninstall/data
clear/key access. MainActivity cold-opened, but service still was not running.
After a second user off/on toggle it reconnected in a fresh process and returned
responsive diagnostics. Sleeping process status does not diagnose the timeout;
cause remains unknown. No broad logs, stack/heap dumps, or conversation reads.

Fresh baseline NOT_CAPTURED, proof/captures/commits0, draftNONE, confirmationNONE,
cleanupfalse; mic inactive, SwiftKey selected. Own test fixture opened. Astra/xhigh
reviewed offline failure/fresh-online recovery protocol (docs/PHASE2.md). User
instructed to disable Wi-Fi/mobile data, deliberately record a short test sentence,
Stop once, remain offline and report the error. No automated recording, changed
network settings, insertion, retry or sending. Outcome pending. Retain the service
readiness incident as a lifecycle limitation; recovery is not a root-cause fix.

### October 25 offline trial blocked before recording: idle bubble missing

Andrew disabled Wi-Fi and mobile data, then tapped the owned Test message field.
SwiftKey and Wispr appeared but Ko did not. One manual full keyboard hide/reopen
also failed to show Ko. The fresh own service still responds with NOT_CAPTURED,
proof/captures/commits0, draftNONE/confirmationNONE/cleanupfalse. AppOps shows no
active microphone; the phone is unlocked and keyboard metadata reports shown.
Ko is enabled and bound. A crashed-service marker also remains in Android's
metadata, but it does not establish a current crash or explain this symptom.

No EN session or transcription request began, so this is a readiness failure,
not the planned offline API error result. Andrew was told to restore normal
connectivity. Source review finds no network/key gate in idle visibility.
Possible causes include Ko's own input/window eligibility metadata or initial
overlay placement; current diagnostics cannot distinguish them. One bounded
diagnostic update is being prepared, with cached reason codes/counters only,
before any further speech attempt. No repeated restart, changed safety checks,
credential access, text inspection, insertion or sending.

Diagnostic candidate v5 (October25): Sol/xhigh added cached visibility/refresh
and render enums/counters only. Astra/xhigh cleared the final source after guarding
old layout callbacks from overwriting a replacement host's diagnostic outcome and
renaming the add/setup failure code accurately. Reviewed service SHA256
`36A811DFF53228FC3BECFE8F966B793DBB90314C71CD66D4489EFE8DB0DD5B75`;
IdleBubblePolicy SHA256
`15EF937CEDE0A5173F4BC13892D9054CC010805CBB8DFE9C8B7BCC9EB9809BF7`.
No visibility/insertion predicates, network, credential or microphone behavior
changed. Dump formats cached values only, with no new Android/node/window reads.

Build/signature, all53 unit tests and lint0 errors/37 warnings passed. Saved and
installed `app/build/speech-input/SPEECH-idle-diagnostics-v5.apk`, SHA256
`5579B7FAE962CD93B97B14720C8431565268E04944C4E7B0BDFD9B2F15A62147`.
In-place install preserved app data; ordinary debug/release outputs restored
OFF/visualfalse/SPEECHfalse. Smoke unchanged; no instrumentation ran.

First responsive v5 snapshot: refresh1 INPUT_STARTED, IDLE_CHECKED,
FOCUSED_APP_COUNT, connected/inputStarts1, unlocked, wanted/suppressed false,
no host/render. Recording/capture/commit counts0; no draft or cleanup pending.
Window-type/focus/bounds-only check showed focused TYPE_SYSTEM rather than an
application. Asked Andrew to close the system panel and return to Test message
with SwiftKey, then report bubble visibility without recording. This current
snapshot does not diagnose the original v4 symptom or accept a fix.

Andrew then reported Ko appeared after returning to the editor, with mobile data
on. Later own snapshot had refresh20/FOCUSED_APP_COUNT, no host and render2 hidden,
while all recording/destination/commit counts remained0 and mic inactive. Thus
user-visible recovery is reported, but sustained READY was not sampled. Reissued
the controlled offline procedure with explicit Quick Settings closure; stop if
Ko is absent, otherwise one deliberate EN recording/Stop and terminal error report.
No failed API attempt has occurred yet at this handoff.

### October25 controlled offline error PASS on diagnostic v5

Andrew left Wi-Fi and mobile data off, returned fully to Test message, observed
the restored cursor and Ko, and deliberately recorded the instructed sentence.
After one Stop he reported "Transcription failed: network." The existing cursor
made an extra field tap unnecessary; this flow did not capture any destination.

Own proof0->1; RESELECT_CANCELLED, draftNONE, captures/commits0,
confirmationNONE/cleanupfalse. Trace: MODE_SELECTED_ENGLISH,
MIC_READ_POSITIVE_UNSILENCED, STOP_ENTRY, ENDED_SPEECH_NETWORK. AppOps confirms
microphone ended4.858s; SwiftKey remains selected. Cached idle refresh44 reports
READY, IME visible, unlocked, wantedtrue/suppressedfalse, attached/shown host;
render7VISIBLE. No content/key read, screenshot, insertion or sending.

Astra/xhigh independently accepted the offline evidence. Root then asked Andrew
to restore normal connectivity and make a fresh recording with a different fixed
sentence, verify correct English review and tap review Cancel. The new sentence
helps distinguish fresh recovery from stale output. Await user correctness and
unchanged-field report plus proof increment/Stop/REVIEW_CANCELLED, no draft,
cleanupfalse, zero destination/commit counts and stopped mic. Original morning
timeout/missing-bubble cause remains unknown; this is not a lifecycle-fix claim.

### October25 fresh online recovery PASS; bounded Phase2 accepted; requested stop

Andrew restored normal connectivity, recorded a different deliberate sentence,
reported correct English review, then tapped review Cancel. Cursor returned to
the test field and no text was entered. Same v5 process: proof1->2,
REVIEW_CANCELLED, draftNONE/captures0/commits0/confirmationNONE/cleanupfalse.
New trace MODE_SELECTED_ENGLISH, MIC_READ_POSITIVE_UNSILENCED, STOP_ENTRY,
REVIEW_ENDED_USER_CANCELLED. AppOps recording ended5.048s; SwiftKey unchanged.
Cached refresh73 READY/IME visible, attached/shown host; render21VISIBLE.

Astra/xhigh accepted the bounded Phase2 gate using this failure/recovery pair,
prior real ASR/SwiftKey-edit/guarded-insertion pass, credential/setup evidence,
53 unit tests and independent source review. Microphone durations are not
transcription latency. Processing-time Cancel, broader accuracy/geometry/lifecycle,
OEM transfer, latency and cost remain unverified. Original readiness cause is open.

Andrew requested stopping. No further phone action, test, API request, code build
or automation was started. Phase3 implementation remains next, with the selected
gpt-6-luna/xhigh, same saved key, editable English confirmation before translation,
fresh guarded destination capture only on Insert and default/release disabled.
No new real-conversation insertion authorization follows from this acceptance.

### October25 Phase3 translation candidate: source/local gates PASS, phone pending

Andrew explicitly resumed Phase3. Two Sol6/xhigh agents implemented client and
UI/state changes; independent Astra/xhigh review cleared the final source.
Explicit REVIEW/SPEECH=true/TRANSLATION=true/VISUAL=false candidate built,
all64 units passed (zero failed/skipped), lint0errors/42warnings, APK signature
verified. Warnings are static UI/localization/tool-version warnings, not a
device-behavior pass. Ordinary debug and release restored/verified OFF/false.

Saved `app/build/translation/TRANSLATION-v1.apk`, SHA-256
`810D9564F5D169E69E08C5617FE5F14FCE94189AE71280C9252FFC8CEEB4BF18`.
Reviewed client SHA E30D7BF8F34E0E17672DDFE3AB5344C6E9205D3BEE80F126BC049601B7AE6025;
service DB03C6C7077B851B43B694E4E7DE86E0477ACC8C406DF54B649453F498A0F858;
review UI B7A11635CE625DF5F041A40899B44397916FB178EFE84D46A469D26631DB98D1;
draft state BCA51C682BFA6B64E441FA07665CE7F9723315EEE870766C75AC2152494408F1.

Andrew connected/unlocked and approved debugging. Preinstall v5 process14122
was responsive/idle: proof2, draftNONE, captures/commits0, cleanupfalse, microphone
stopped, SwiftKey selected, unlocked. In-place install succeeded, preserving data.
First updated service process11530 diagnostic timed out; no recording/API action
started. Owned manual Test message launcher completed. Verify service readiness
before first user recording; do not infer a cause from this startup timeout.

One later readiness check of the same process11530 responded normally: REVIEW,
NOT_CAPTURED, proof0, translation0/0/0/0, draftNONE, captures/commits0,
confirmationNONE/cleanupfalse, mic inactive. No restart/toggle occurred. Andrew
was asked for the first 요 flow, editing two to three blue notebooks before
Confirm & translate and one Insert in the owned empty field. User/device result
pending; the readiness recheck does not diagnose the initial timeout.

First planned phone gate is one real 요 translation/guarded insertion, then a
distinct 반말 case, followed by translation cancellation/error recovery and
user-reviewed quality/latency/usage. No new real-app insertion trial is authorized.

### October25 first actual 요 translation/edit/Insert PASS; UI follow-up

Andrew reported the instructed edited message produced proper Korean in Test
message. Exact generated wording was not collected. Same process11530: proof1;
translation requests/success/errors/cancels1/1/0/0; capture1/commit1; CONFIRMED,
REVIEW_COMPLETED and REVIEW_ENDED_INSERT_CONFIRMED; draftNONE/cleanupfalse.
Idle suppressiontrue and host absent corroborate dismissal. AppOps confirms mic
ended5.301s; SwiftKey unchanged. One paid deliberate translation succeeded using
the previously saved key; no credential read/change or new real-app trial.

Stop-to-review-created1761ms includes cleanup/ASR/callback through draft creation,
not review rendering. Translation job-to-accepted-callback3034ms includes key
lookup/network/parsing, not visible result rendering or user selection/insertion.
Both use awake uptime. Usage input194/cached0/output105/reasoning77. At the verified
standard list rates the translation-only estimate is USD0.0000719; reasoning is
already included in output, and this excludes transcription/unobserved failed usage.
This is one sample, not a latency/cost distribution or spending cap.

User found Confirm & translate below the viewport with SwiftKey and no Korean
visible before Insert. Sol6/xhigh implements pinned primary/Cancel actions, with
Astra/xhigh review. Andrew then questioned the preview and asked about one
Translate & insert action; the unshipped preview patch was reverted. Insertion
logic is unchanged. Andrew initially chose translation checks first, then
reopened that priority decision. A future single-action flow needs
verified intended-destination return and a safe manual fallback. No such flow
is implemented or device-tested. The distinct 반말 case can also test pinned actions.

### October25 pinned review actions v2: source/local PASS, phone layout pending

Only ReviewActivity code changed. Astra/xhigh cleared SHA-256
`9F05E2C29556BCF7CD56E18AB21399BCF7F7496276E6FAF23EFA3F23C4FE8081`.
Secure bounded review window, pinned primary/Cancel row and independently
scrolling English area; owner/cancellation/processing and service/insertion
logic unchanged. No Korean preview patch remains.

Build/all64 units/signature passed; lint0errors/36warnings. Saved and installed
`app/build/translation/TRANSLATION-actions-v2.apk`, SHA-256
`EEE3459902AFF0AE7F15B230D661C5E527AF4BA339ECF6507216761BD78FEEA8`.
Ordinary debug/release flags restored/verified OFF/false. Before in-place update,
v1 remained terminally completed with same counters, draftNONE and stopped mic.
No recording/API action was initiated for this UI update. Actual action visibility
with SwiftKey remains a manual check; do not claim a phone-layout pass yet.
New process16745 responded immediately: NOT_CAPTURED, all proof/translation/
capture/commit counters0, draftNONE/cleanupfalse, microphone inactive, SwiftKey
unchanged. Cached bubble READY/visible. No new recording or translation was run.

### October25 automatic original-intent candidate: local PASS; first setup blocked

Andrew explicitly authorized Translate & insert with manual fallback and accepted
test mode. AstraUltra designed the policy; Sol6/xhigh agents implemented policy,
integration and opt-in tests; independent Astra/xhigh cleared final source.
Product: secure REVIEW/AUTO=true; SPEECH/TRANSLATION/VISUAL=false. Only owned
smoke fixture and Korean modes are automatic; external apps and EN stay manual.
Build/all77 units/signatures passed; lint0errors/app35/smoke3warnings. Default
debug/release restored OFF/allflagsfalse. In-place updates preserve the live key.

Saved/installed product `app/build/automatic-insert/AUTO-sample-v1.apk`, SHA-256
`CEE0D1DE3EEA62A5C51A96613B2D5EC8E1F8E64DBAA4F1EDDE87C965E6332D74`;
smoke `app/build/automatic-insert/smoke-auto-v1.apk`, SHA-256
`1A2FC9A36365B85ECDE3D7232EC674D8BE520D95482C8760B56A0B4B6C33BC39`.
Source hashes: service8CCF2420, reviewD0D07EC1, policy62EAF90C,
policy-testE4F37947, build3D7B0248, smokeC5596FD6.

Ran only AutomaticInsertSmokeTest#unchangedEmptyOriginalAutoInsertsOnceAndDismisses
with autoEmptyReady=true. It failed FIXTURE_NOT_FOCUSED at seedFixture after9.2s,
before any mode click. Post-test own PID32396 stayed NOT_CAPTURED, proof0,
originalcaptured0/phaseNONE/reasonNONE, autoattempt/fallback0, capture/commit0,
draftNONE/cleanupfalse; mic inactive, SwiftKey unchanged, unlocked. This is no
automatic-insertion verdict. No paid API call or credential/test setup occurred.

The new harness demanded exact accessibility-node caret even for an empty field;
the established helper checked focused node and known text. The A35 previously
reported -1/-1 for empty node selection while the input connection proved0/0.
This is the suspected setup cause, not measured in this failed run. Narrow
harness correction adds refreshed-node/direct-owned-EditText proof and sanitized
numeric/boolean failure metadata. Product safeguards remain unchanged. Rebuild
and rerun only the same case after independent review.

Smoke-only correction EDBDEFA4 was independently cleared, built/linted/signed,
saved/installed as `app/build/automatic-insert/smoke-auto-v2.apk`, SHA-256
`60E5E0E99A8E485ED168C9EB761609EB891A97063564AD72CA54674FD3F4E10C`.
Product CEE0D1DE unchanged. The same exact empty test then failed after4.926s with
ORIGINAL_INTENT_NOT_ARMED_IN_REVIEW, beyond seeding and sample microphone proof.
Same PID32396: original captured1, FALLBACK/REVIEW_WINDOW_UNVERIFIED, proof1,
autoattempts0/captures0/commits0; REVIEW_CANCELLED by harness cleanup, draftNONE,
cleanupfalse. Mic ended582ms. Trace shows original capture, mode, mic proof/Stop,
then failed review attestation before any AUTO_OWN_INPUT_STARTED entry.

This isolates failure to own-review attestation, not a successful auto return.
The reason combines several predicates; it does not yet establish which one
failed. AstraUltra and Astra/xhigh recommend fixed boolean/callback/generation
diagnostics with unchanged immediate terminal fallback, then one fresh owned
sample. No guard broadening, API call, external conversation or Send action.

### October25 diagnostic v2: review registration ordering measured

Independent Astra/xhigh cleared diagnostic-only service7D6797EB/review4996A807.
All77 units passed, lint0 errors/35 warnings, signature verified. Saved/installed
`app/build/automatic-insert/AUTO-review-diag-v2.apk`, SHA-256
`DE3A62D02E6BD529D3416026B83C6D2E0409B0156077E8AB54E113FBDEB5620C`.
Default debug/release flags restored OFF/allfalse. Smoke-auto-v2 unchanged.

Same exact empty test failed ORIGINAL_INTENT_NOT_ARMED_IN_REVIEW after4.93s.
At the local WINDOW_FOCUS callback: generation ORIGINAL, input started but not
own, focused application count0, valid local own window ID and editor node in
that window, remote review window absent/root absent. First failed predicate ROOT.
This measures missing initial accessibility registration, not node mismatch or
changed original content. PID9446 captured1/proof1/fallback1; attempts/captures/
commits0, draftNONE/cleanupfalse after harness Cancel, mic ended517ms. SwiftKey
unchanged. The action geometry assertion passed before the armed-review assertion,
but actual manual SwiftKey editing/button usability remains pending.

Next correction may await only absent initial review metadata within a bounded
ENTERING_REVIEW interval; it must retain all contradictory event/input/owner
invalidators and never revive the completed fallback. No automatic pass yet.

### October25 review-readiness v3: next event refusal isolated

Source14EB76F9 and policy-test88F1D25B independently cleared. All78 unit tests
passed (no failures/errors/skips), lint0 errors/35 warnings, signature verified.
Saved/installed `app/build/automatic-insert/AUTO-review-ready-v3.apk`, SHA-256
`619C9241B9D4EB6EF2097A4A25E06657959DD6BE90AECA591323CC1D953B02AC`.
Ordinary debug/release restored OFF. Smoke-auto-v2 unchanged, app data preserved.

Same empty test failed ORIGINAL_INTENT_NOT_ARMED_IN_REVIEW after3.862s. Trace now
shows AUTO_REVIEW_ENTRY_WAIT followed by UNEXPECTED_WINDOW_CHANGE, before own
input start or full window readiness. This proves entry to the new wait but
does not identify which window changed. PID13306: proof1/original1/fallback1,
attempts/captures/commits0, draftNONE/cleanupfalse, mic ended525ms. No API request.
Next diagnostics will classify the event and window roles before any exception
is considered. All terminal guards and one-attempt rules remain in force.

### October25 window diagnostic v4: original window removal measured

Final source58C392A9 independently cleared; build/78 units passed, lint0 errors/
35 warnings, signature verified. Saved/installed
`app/build/automatic-insert/AUTO-window-diag-v4.apk`, SHA-256
`79F9B730216D9C99B8EEA4B27E37D7D5C9E32EEDC4BC7A03989FA73CE21E7A3D`.
Default debug/release outputs restored OFF; smoke-auto-v2 unchanged.

Same empty test failed ORIGINAL_INTENT_NOT_ARMED_IN_REVIEW after3.616s.
First event refusal: ENTERING_REVIEW, pending wait age20ms/check0; WINDOWS event
age19ms, windows-change mask2 (REMOVED in local Android36 SDK), role ORIGINAL.
POST_DECISION metadata: original absent, expected local review present, one
focused window/application matching expected review, local and entry IDs agree,
review not remotely bound yet. Root/source relation UNKNOWN because no additional
node reads were made for diagnostics. This establishes original-window removal;
it does not prove original identity on return. PID16450 proof1/intent1/fallback1,
attempts/captures/commits0, draftNONE/cleanupfalse, mic ended575ms. No API request.

Next: one opt-in non-inserting owned fixture trace across recording/review/Cancel
and return. Product guards stay unchanged. A removal exception would require an
explicit paired occlusion protocol and stable original window/root/node proof;
unavailable/changed identity must preserve manual fallback.

### October25 lifecycle observer ready; awaiting unlock

New `AutoLifecycleDiagnosticSmokeTest` source C6214251 independently reviewed.
Exact opt-in `autoLifecycleReady=true`; only owned empty fixture, Ko→요→Stop→
review→Cancel, with natural return and no refocus/reseed. Separately reports
accessibility identity and direct Activity/editor/token/lifecycle counters;
original-window event timestamps are explicitly listener callback receipt times.
No Confirm/Insert/Copy/Paste/Send, API/key access or conversation inspection.
No-write/microphone/draft cleanup assertions are mandatory; identity results
STABLE/CHANGED/UNAVAILABLE are observations, not automatic insertion acceptance.

Smoke build/signature/lint passed (0 errors/3 warnings). Saved/installed
`app/build/automatic-insert/smoke-lifecycle-v3.apk`, SHA-256
`7631FD3D7E4CB8213749F1D05F142B93A3692123F6AABF4C6687F0DE896E64C3`.
Product v4 remains unchanged. Pre-run phone check: screenOFF/showingtrue,
microphone stopped; requested unlock and did not start instrumentation.
Pending exact method: `originalWindowReviewCancelReturnIsObservedWithoutInsertion`.

Andrew then unlocked. First lifecycle run failed after3.621s with
FOREIGN_FOCUSED_APPLICATION in the post-Cancel return wait. Baseline/recording
original window/root/node identities matched; uniqueId absent on both and empty
accessibility caret unknown, while direct owned editor proved caret0. In review,
original accessibility window/root/node were absent, but direct Activity/editor/
nonnull token stayed identical, attached, empty and caret0 with create/attach/
detach deltas0. Pre-Cancel own review window/root/editor matched. No return
classification was produced. Product PID16450 proof1→2/intent1→2/fallback1→2,
no captures/attempts/commits, draftNONE/cleanupfalse, mic ended555ms.

The product readiness wait did succeed in this run: ENTRY_READY after24ms,
remote review focused, then original REMOVED still terminally rejected. Test
inspection found `uniqueFocusedApp` treats null root/package as foreign; this
is a diagnostic ambiguity, not proof the failure was missing metadata. A later
package-only snapshot showed launcher after runner cleanup, which cannot identify
the failure-time app. Correct only the test's null/foreign distinction, retaining
strict rejection of explicit foreign packages and no post-Cancel actions.

### October25 corrected Cancel lifecycle: PASS, original identity survives

Test-only correction CB282F6F independently cleared. Smoke build/lint/signature
passed; saved/installed `app/build/automatic-insert/smoke-lifecycle-v4.apk`,
SHA-256 `D2A1FB5D2D989030806A31FEB5C585F75080382A37B51295A5AD3F4360AE0BE3`.
Product unchanged. Same exact Cancel-only case PASSED in4.01s. Original remote
window/root/node/refreshed identity/resource/class/input type returned identical;
uniqueId absent both times, node empty caret unknown but direct owned editor
proved0/0. Direct Activity/editor/nonnull token stayed identical and attached,
text remained empty, lifecycle create/attach/detach deltas0. Same results at
first natural return and300ms settled, without refocus/reseed.

Original-window listener observed one REMOVED mask2 and one ADDED mask1, preceded
by ACTIVE|FOCUSED mask96. Receipt times from baseline:899/996/1462ms respectively;
removed callback arrived145ms before observer's full review proof. These are
callback receipt and polling observations, not exact occurrence/order proof in
the product service. Stop-to-observed-review392ms, Cancel-to-observed-return192ms.
No stage/event overflow and no explicit foreign app reported. This does not prove
what caused the previous helper's error.

PID16450 proof2→3/intent2→3/fallback2→3; captures/attempts/commits0, draftNONE,
cleanupfalse, mic ended497ms, SwiftKey unchanged. Product still correctly ends
auto on original REMOVED; full input-generation continuity after that fallback
is not measured by this observer. Next candidate may recognize one authenticated
own-review occlusion/return pair while retaining exact original final proofs,
content/input invalidations and one-attempt safeguards. No automatic pass yet.

At Andrew's requested stop, restored `TRANSLATION-actions-v2.apk` (SHAEEE34599)
in-place; saved automatic v4 and lifecycle v4 artifacts remain available.
New product PID27092 responds idle: proof/translation/capture/commit counters0,
draftNONE/cleanupfalse, microphone inactive and SwiftKey unchanged. No pending
recording or test. This installation restores the tested manual workflow; no new
translation or insertion test was run. Paired occlusion remains unimplemented.

### October26 MVP: persistent collapsed Ko and Karrot Copy recovery PASS

Andrew deferred automatic insertion, retained manual Insert plus Copy & close,
and selected keeping collapsed Ko available after completion with SwiftKey open.
The earlier click-rearm candidate passed owned cases but failed manual Karrot
reuse (READY/IMEvisible but suppressed); that approach was removed entirely.

Karrot read-only metadata probe CD953608: matching root/node package and window,
but node focused0/editable0/inputType0, no resource, visible/enabled1/password0.
No text, hint, description, history traversal, UI action, clipboard or API call.
Retained own process27092 contained NODE_EDITOR_MISMATCH refusals (no app labels).
This supports Copy & close for Karrot rather than weakening Insert safeguards;
no custom adapter or new real-app Insert trial was attempted.

Final service AF6DC0A4 independently Astra/xhigh reviewed. Only completion UI
refresh and NODE_EDITOR_MISMATCH recovery wording changed; policy/tests/XML
match the prior baseline. All78 unit tests pass; app lint0 errors/35 warnings.
Owned tests A81140F3, with older manual expectations A025576C, independently
reviewed and compiled; smoke lint0 errors/3 warnings. Both exact cases passed:
`confirmedInsertKeepsCollapsedKoAvailable` (6s) and
`copyAndCloseKeepsCollapsedKoAvailable` (6.267s). They observe IME from before
completion through Ko/mode panel/Close, no editor retap/keyboard cycle, no result
panel, unchanged text/caret, and no extra recording/write. Sample PID18564 ended
proof2/capture1/commit1/COPIED, draftNONE/cleanupfalse, collapsed Ko shown;
microphone ended444ms. No API call. Older full manual cases were not rerun.

Saved API-free product `app/build/mvp-reuse/REVIEW-persistent-ko-v3.apk`, SHA-256
`628BC24A13C7640F782D97B7F2E9D3B7C6FFDE599441803AE998CE43567CFEB9`;
smoke `app/build/mvp-reuse/smoke-persistent-ko-v3.apk`, SHA-256
`C8D062B538D15CFDFBA7DCA82FE353368735E06A1D321D604B9D6B9FCD77A83B`.

Real speech/translation product built/linted/signed and installed in-place:
`app/build/mvp-reuse/TRANSLATION-persistent-ko-v3.apk`, SHA-256
`EA8D45E03B1B5818BAAE9CC6E887873659AB229470BCE499830D0D744F3D0A39`.
Secure REVIEW, SPEECH/TRANSLATION=true, AUTO/VISUAL=false. Ordinary/release outputs
restored and verified OFF/allfalse. Key/data intact. Andrew's Karrot actual
EN speech→review→Confirm→Copy & close PASSED; words matched and Ko stayed visible.
PID22101 proof1/COPIED, draftNONE/cleanupfalse, capture/commit0, automatic intent
counts0, bubble READY/IMEvisible/wanted/shown/suppressionfalse, mic ended4.716s.
Stop-to-review-created1824ms includes cleanup/ASR/callback, not render time.
No new Korean translation trial, Paste, real-app Insert, Send or history inspection.

### October26 selected Spoken K: installed, focused UI PASS

Andrew selected displayed concept2, Spoken K, and requested installation. Sol/xhigh
implemented service95ECC800/drawable761E9E53/foregroundB0CAC4CF; independent
Astra/xhigh source review CLEAR. Native56×48dp Button and tap/drag behavior remain,
with opaque graphite K on80% opaque light gray. Accessible label: Open KoScribe.
Original selected reference/provenance is in docs/design/floating-button.

Explicit live REVIEW build/all78 unit tests/signatures passed; app lint0errors/
34warnings, smoke0errors/4warnings. Saved and installed in-place:
`app/build/floating-k/TRANSLATION-stylized-k-v1.apk`, SHA-256
`BAA162DDA0FEA3438B3811EC0BBC5376081F8FE09EA248261E8F799DB454FF26`.
PID22160 reports speech/translation=true, auto=false, draftNONE/counters0 after
update. VISUAL=false; FLAG_SECURE unchanged. Key/data preserved, no sample APK
installed. Default debug/release outputs restored/verified OFF/allfalse.

Exact opt-in `FloatingIconSmokeTest#floatingIconOpensClosesAndDragsWithoutRecording`
first failed MODE_CHOICES_NOT_VISIBLE. New helper used exact case while native
buttons can expose capitals; existing helpers ignored case. One-helper correction
3EDA81BC independently reviewed CLEAR. Saved signed smoke-v2 SHA-256
`2678329A589FF74E67CB32C50D6BB1966ED94C4C9B6A2756300DED33ACC1F851`.
Corrected case PASSED4.510s: unique accessible Button, open3modes/Close, horizontal
drag and restore, empty owned editor and SwiftKey maintained. DraftNONE and
proof/capture/commit counters unchanged0 at each stage. No mode selection,
microphone capture, API call, clipboard use, insertion, or real-app interaction.

Installed drawable alone rendered to an own-fixture BitmapCanvas and exported
for root visual QA: clear selected K, light gray body, transparent corners. This
is not a screen capture and contains no phone/app content. Render stays ignored
under app/build. Standalone service dumps intermittently time out before and
after update; cause unmeasured, no claim the matcher fixes it. Passing instrumented
case obtained its own state internally. Final AppOps shows only prior stopped
4.716s audio, SwiftKey unchanged. No broad reliability acceptance implied.

## Recording indicator visual checks (2026-10-29)

Reviewed D70CC7DF (three decorative bars) was installed on the A35 with the
same debug certificate; speech/translation remained true, auto false, draft
NONE and SwiftKey selected. Andrew reported the recording label, timer, motion,
fixed Stop/Cancel and Cancel behavior worked in the owned Test message screen,
but the three bars were too small. Own service diagnostics showed two proved
recordings, two USER_CANCELLED endings, draft NONE, translation requests0,
commits0 and overlay cleanup false.

Sol/xhigh expanded the pulse to eleven bars on its own line without changing
mic/session/insertion code. Astra/xhigh reviewed CLEAR; all 90 unit tests,
translation-enabled assembly and fresh lint `--rerun-tasks` passed. Signed APK
`app/build/recording-indicator/TRANSLATION-recording-indicator-v2.apk`, SHA-256
`159E2452C057E49A96751D5D277DE2704FB7719A12B0B2ABE40669B9659C538E`
was installed in place. Andrew again confirmed the label, timer, moving bars,
Stop and Cancel worked on the owned Test message screen. He found the bars too
large for decorative motion, but chose to keep this revision for now. Own trace
showed proof1, USER_CANCELLED, draft NONE, translation requests0, commit0,
cleanup false. No transcript, API call, insertion or real-app action was tested.

## Review-surface comparison (2026-11-05)

**Measured conclusion:** the focusable accessibility overlay kept the original
owned window/root/node accessible in the recorded editable-review samples. The Activity
control did not. Both restored the original middle caret and fixed context with
a fresh input session, and both detected another identical-content field. Neither
could detect the fixture's hidden logical rebinding of the same view. This supports
an overlay review direction, not automatic insertion eligibility or external-app
compatibility. Two review samples do not establish continuous availability during
the entire105-second positive case, nor a return-correctness advantage over the
Activity. No automatic phone insertion has passed.

Andrew gave fresh readiness before installation and used SwiftKey for both
positive edits, then personally tapped Dismiss probe. Four negatives changed only
the owned fixture. Exact methods/sole flags are in INSTRUMENTATION. All six JUnit
invocations completed, including the intentionally exposed indistinguishability
cases; completion alone is not a favorable destination-safety result.

| Arm/case | Seconds | Review samples/root available/node available | Return identity | Fixture oracle | Interpretation |
| --- | ---: | --- | --- | --- | --- |
| Activity middle | 26.412 | 2/0/0 | MATCH | UNCHANGED | Text/caret preserved; fresh target session |
| Activity alternate | 5.018 | 4/0/0 | MISMATCH | CHANGED | Distinct identical-content field detected |
| Activity same-view rebind | 4.777 | 4/0/0 | MATCH | CHANGED | Hidden logical change unobservable |
| Overlay middle | 105.586 | 2/2/2 | MATCH | UNCHANGED | Text/caret preserved; fresh target session |
| Overlay alternate | 4.844 | 2/2/2 | MISMATCH | CHANGED | Distinct identical-content field detected |
| Overlay same-view rebind | 4.623 | 2/2/2 | MATCH | CHANGED | Hidden logical change unobservable |

Activity target diagnostics were window ABSENT/root UNAVAILABLE/node UNAVAILABLE;
overlay diagnostics were PRESENT/SAME/AVAILABLE while its own editor held focus.
Original caret/context were not read through the review's input connection. On
return every case had caret/context MATCH, generation FRESH and input TARGET.
Same-view rebinding changed only the fixture's private logical epoch: no forced
restartInput and no oracle exposed to the product. Semantic identity remained
UNKNOWN. Sticky return mismatch was true only for the alternate-field cases.

Both positive edits produced six TextWatcher changes. The overlay's longer time
included Andrew's delay before typing, not a measured processing delay. Input
start/finish/own/target/restarting counters were2/2/1/1/0 for positives and3/3/2/1/0
for negatives, in both arms. First-null role was IME for both positives and NONE
for negatives. This is the new probe's single bounded sample, not attribution of
the November4 global-ledger failure. The positive samples show that one missing
IME root can coexist with a subsequently correct owned-editor return.

All cases ended COMPLETE/OBSERVATION_COMPLETE_NOT_ELIGIBILITY with visible probe
controls gone, cleanup CLEAN, draft NONE, overlay cleanup false and blocked-effect
counter0. Legacy destination-transaction captures, commits, automatic attempts
and recording proofs remained0. The probe did perform its scoped read-only owned
target observations. Microphone was NEVER_STARTED; AppOps still showed only the
previous stopped560ms usage. No API call, credential operation, clipboard, Send,
external-app interaction, screenshot or conversation inspection occurred.

Reviewed source serviceB8B3E145/controller266D6D7F; policy5899D291/tests25D42213;
smoke31AF37FC/fixtureAB074DC6/IDs83AEA44A. All144 unit tests passed, app lint0errors/
44warnings, smoke lint0errors/4warnings. Secure REVIEW arms, other experimentsfalse;
normal debug/release rebuilt and verified OFF/allfalse. Artifacts under ignored
`app/build/review-surface-20261105/`:

- Activity `review-activity-v1.apk`:453F8C8968D2AD643849B2323BB0EED4CD432BBDE51C77B609FB643F4357E939, servicePID14373.
- Overlay `review-overlay-v1.apk`:1B61502046A4B75ED8E30D82E08A24B831DD15896615474849151D293C089FE9, servicePID20015.
- Harness `smoke-review-surface-v1.apk`:BAD61EC848A10E41AE56C868B8D17CA662C89F6421AC6179AE1C6FFFCEC8760E.

The first post-install Activity service dump timed out; the next bounded read
succeeded before testing. No reinstall or readiness workaround was needed.
After all cases, restored exact secure MVP D266E911F443B896C33E49DFCE9EDBA5705F50D1E4515956CF045DEB59DAD234
and verified installed hash, servicePID28713, speech/translationtrue, autofalse,
draftNONE and cleanupfalse. SwiftKey remained selected. Removed only the smoke
package from user0; main app data and live credential were preserved.

## Stable-editor automatic sample on November 5

Andrew confirmed the A35 was unlocked and available. Both installed hashes
matched the reviewed artifacts in commit `2d25d1e`. Service PID 21822 started
idle with stable overlay enabled. The original automatic experiment, speech,
translation and recording were off for this run.

The first case, `changedIdenticalFieldFallsBackBeforeWrite`, failed in 5.250s
with `STABLE_AUTO_DISARMED_BEFORE_REVIEW_INPUT_UNEXPECTED_FINISH`. The test
was still waiting for an eligible review when Android ended an input session.
The fixture had yet to switch fields or approve English. This run establishes
an early refusal. Testing the field switch and positive insertion is still pending.

The trace recorded one fallback. Destination captures, automatic attempts,
commit requests, recording proofs and manual result renders were all zero.
The harness cancelled the review. The draft ended at NONE with cleanup complete.
The v1 refusal code combines several possible phases and input sessions.
Their identity and sequence require measurement.

Android 16 calls the finish handler before clearing the ending editor metadata.
This lets a diagnostic classify the ending session using the callback. A new
session can follow the finish, or the input method can become idle. An ordered
trace will distinguish those cases on this device. [Android 16 InputMethod source](https://github.com/aosp-mirror/platform_frameworks_base/blob/android16-release/core/java/android/accessibilityservice/InputMethod.java#L151-L175)

Restored the normal secure MVP and verified installed hash D266E911. Service
PID 24728 was idle with normal speech/translation settings, SwiftKey selected
and the owned fixture uninstalled for user 0. AppOps showed the previous 560ms
recording. Only the exact negative method ran.

## System-managed SwiftKey and first guarded return on November 5

Andrew made the unlocked A35 available for up to ten minutes. Only the signed
owned fixture ran. All builds used fixed English and Korean sample content.

| App and harness | Test | Measured result |
| --- | --- | --- |
| A 053DD8DA, smoke 353EED3B, PID 28163 | Switch to an identical-looking field | Failed at 3.841s before the fixture switch. The explicit keyboard-show request occurred while the original editor was still served. Android started the review editor at G1, finished it before full focus attestation and started it again at G2 after fallback. Zero captures, attempts and commits. |
| C 636BD6CE, smoke 353EED3B, PID 31375 | Same field switch | SwiftKey visible, review focus attested. The actual switch triggered RETURN_FIELD_CHANGED before approval. Zero captures, attempts and commits. JUnit failed at 3.859s while looking for the three recovery controls during layout. One-shot cleanup missed Cancel and left a sample READY. Reinstalled the same C in place for a clean session. |
| C 636BD6CE, smoke FE40499D, PID 2225 | Same field switch | **Passed in 4.659s.** Review focus and IME present. RETURN_FIELD_CHANGED before Confirm, zero captures, attempts and commits. Insert, Copy and Cancel were enabled, visible and clickable in the same owned window. Restoring the fixture focus left eligibility revoked. Actual Cancel cleared the draft. |
| Same C and harness, PID 2225 | Unchanged middle caret between emoji-bearing prefix and suffix | **Failed in 10.512s.** One approval. Trace showed the original editor return at G2. One fresh capture and automatic attempt. Six FRESH_TARGET_EVENT notifications invalidated the transaction. The precommit editor checks passed and the guard refused. Zero commit requests. Harness Cancel cleared the draft. |

The recorded G0/G1/G2 labels are generations relative to the original input
session. A never reached full review attestation before its restart, so the
precondition for the separate visibility-hint control B3032F12 was absent and
it stayed on disk. C removed the explicit keyboard-show call while retaining
the window hint, focus requests and all writer checks. The newer harness waits
up to 2.5s for usable controls together in a single owned window.

On the middle run the service emitted OBS_READY_UNKNOWN, FRESH_INSERT_REFUSED
and AUTO_GUARD_REFUSED. Source inspection narrows the refusal to an invalidated
fresh transaction. The six events shared one log label. Their types, window
roles and occurrence times remain unknown. Empty, review Cancel, caret change
and context change were held after this failure. No insertion, Send, recording,
network request or external-app test occurred during this window.

Both APKs were checked against the installed bytes. C SHA-256
636BD6CE9D1D2A7FB32202AE972594C77DAF65A49CF670AC51BFF908C5FF84A1;
smoke v2 FE40499DE3DBBE223B2E237FEA5AFEF63F7D4DC56FEEA0F673D828206588D828.
C passed 153 unit tests and app lint (0 errors, 50 warnings). Final smoke
assembled and passed lint (0 errors, 5 warnings). Astra independently cleared
the adapter AEFF4629 and harness 311EE671.

Restored and verified the normal secure D266E911 in place inside the availability
window. PID 5575, REVIEW, speech and translation on, auto off, draft NONE,
cleanup clear. SwiftKey selected, smoke removed for user 0.

In a second, separate five-minute window, diagnostic D (6757F300) and smoke
FE40499D ran on PID 11005. The identical-field test passed in 5.088s, with
usable manual controls, clean Cancel and zero fresh captures, attempts and
commits. The unchanged-middle test failed in 10.716s. The guard returned
RESERVE_TARGET_CHANGED. The six notifications below accounted for the whole
trace. Each carried a timestamp after return began and before the fresh editor
read started. All were delivered within 50ms of the transaction opening.

| Event order | Reported window role | Reported change |
| --- | --- | --- |
| 1 | KoScribe bubble host | Layer (16) |
| 2–4 | System | Layer (16) each |
| 5 | System | Added (1) |
| 6 | Current default-display IME | Bounds (8) |

The system-window reports identify the role at callback time. Several kinds of
Android surface, including navigation bars and system dialogs, share that role.
The purpose of the added window remains unknown. [Android 16 window types](https://github.com/aosp-mirror/platform_frameworks_base/blob/android16-release/services/accessibility/java/com/android/server/accessibility/AccessibilityWindowManager.java#L930-L944)

The original field and context matched at both reads, one automatic attempt
opened one fresh transaction, and commit requests stayed at zero. Cancel cleared
the draft. Testing stopped. Restored verified D266E911 and removed the fixture.
The first post-install service check preceded reconnection. A follow-up
confirmed PID 13370 idle, normal speech/translation on, auto off and cleanup
clear. SwiftKey remained selected. The automatic experiment was then shelved
when Andrew proposed the editable Korean manual MVP.

## Manual Korean result review on November 5

Andrew offered five minutes for the four owned-fixture checks and asked to
leave the revised live MVP installed if they pass. Installed secure manual
sample AE1D1C6B and smoke 69A663B4, verified both installed hashes. PID 22405,
speech/translation/auto off, SwiftKey selected. All four use fixed content.
Synthetic edits target only KoScribe's Korean editor. The clipboard is untouched.

The Discard case passed in 7.700s. The full prepared Korean was visible, Edit
accepted a replacement, Discard returned the original, and Cancel cleared the
draft. No destination capture or write occurred. The local mic proof stopped
after 632ms. The first long-preview setup failed in 11.491s at STOP_NOT_AVAILABLE.
Android reported OVERLAY_MIC_SILENCED and stopped the mic after 1ms. No recording
proof, English review, transaction or write was started by that attempt. The
draft ended NONE and cleanup clear. The long preview had yet to be exercised.

Asked whether the phone was still free after microphone silencing. Restored
the verified normal build D266E911 within the original availability window and
removed the fixture. The service reconnected at PID 31731, normal speech and
translation on, auto off, draft NONE, cleanup clear, SwiftKey selected. The
revised live build was withheld per Andrew's all-gates-pass condition. Long
preview and the two Insert cases remain pending. No actual clipboard operation
or live translation request was made.

After Andrew confirmed availability again, reinstalled the same verified sample
and harness for one fresh long-preview check. PID 4653 recorded one microphone
proof and stopped after 571ms. Done saved the entire edited Korean and the test
found it in the visible result panel. The immediate check for a visible,
selected SwiftKey failed at 6.352s before scrolling. The test had already
observed SwiftKey in the editor. This run does not establish whether the
keyboard would return by itself after a delay. Zero destination captures and
commits, draft NONE, cleanup clear. Reinstalled the verified D266E911, removed
the fixture, and confirmed PID 6412 idle with the normal speech/translation
settings and SwiftKey selected.

In the next ten-minute window Andrew authorized, the same sample AE1D1C6B
ran with smoke B7962357 (source E54189D6) on PID 10272. Installed hashes
matched. Each case ran separately and passed its full JUnit assertions.

| Case | Time | Direct evidence |
| --- | ---: | --- |
| Long Korean preview | 6.306s | SwiftKey returned naturally. The owned preview scrolled to its reported end. Insert and Copy were reachable, no capture or commit, clean Cancel. |
| Unedited Korean | 3.918s | A fresh manual Insert captured the newly chosen field and committed once. Exact full string and caret confirmed, source field intact. |
| Edited Korean | 4.754s | After Done, the corrected Korean with emoji appeared in the preview. Selecting a different field produced zero writes. The single explicit Insert placed the correction at its middle caret and preserved both sides. |

The three microphone proofs ended at 609ms, 483ms and 541ms. Cumulative
captures/commits were 0/0, 1/1 and 2/2. Both writes ended CONFIRMED. All
three ended with draft NONE and cleanup clear. Together with the preceding
Discard pass this completed the four requested gates. The fixture used fixed
content and programmatic changes to KoScribe's own editor. The Copy payload
was checked in source/unit tests; the phone's clipboard was untouched.

As requested, installed the secure live speech/translation MVP in place and
verified its bytes against 3FE8BE0FF5AFD51908177D81F6AF652FB6EB0C34A85CD68825731E16DA3D33EA.
Service PID 12646, speech/translation on, auto off, empty draft, cleanup clear,
SwiftKey selected. Removed the fixture and verified its absence for user 0.
