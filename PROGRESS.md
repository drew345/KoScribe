# Current progress

Updated: 2026-11-06 (Asia/Seoul).

**Active goal:** make explicit manual Insert of the reviewed Korean work
reliably in Karrot Market on the A35. Andrew set Karrot as the priority.
Instagram comments are the next follow-up; he previously reported success
after tapping that field again. OpenAI dot is low priority. The installed
editable-Korean MVP is the starting point. Automatic-after-English remains
deferred.

**Current work:** both the relative-read and cached-selection calibrations passed
their owned gates. Karrot's empty read had only initial selection metadata;
the subsequent user-operated ABCD/select/delete sequence proved distinct live
selection and composition updates in one served generation. Implement/review a
separate manual relative insertion path behind a default-OFF, signed-owned-fixture
flag. Its acceptance must include the normal English/Korean review return. Preserve
usable fresh empty composers after Korean review; do not require artificial user
typing/deletion as the product workflow. Writer implementation is underway;
no new insertion candidate has been built or phone-tested. No Karrot insertion
has occurred. Branch: `codex/karrot-insertion`.

**November 6 local validation:** 168 unit tests passed; app assembly/lint passed
(0 errors, 50 warnings), smoke assembly/lint passed (0 errors, 5 warnings).
Astra/xhigh cleared controller5928DF24, policyAB570260, testsF96EFA09,
serviceA52884C8, ApiKeyActivityDFD6771A, Gradle02539BC6 and smoke8BDE13D6,
fixture9E20404B/IDs8FFA0496. Ignored `app/build/relative-read-20261106/`
contains FIXTURE18FE04FD, KARROTC46B46FF and smokeF90717A9, with saved arm
configs and signatures matching the live MVP. Each diagnostic arm blocks all
writer/mic/API/credential/clipboard effects and always reports ELIGIBLE=NEVER.
Ordinary local debug and release configs were rebuilt and verified OFF/allfalse,
including the new probe flag.

**반말 report resolved for current work:** Andrew now confirms it works and
asked to return to Karrot. On the restored live MVP, code-only diagnostics show
CASUAL selections and translation requests/success/errors/cancels2/2/0/0.
No text/key was inspected. Two source reviews found both Korean modes routed
through translation; no app change was made. The cause of the earlier report
remains unknown, with possible confusion around English review noted by Andrew.

**November 6 phone result:** after fresh five-minute availability, the first
owned empty case stopped in 2.061s: INTERRUPTED/WINDOW_STATE_CHANGED,
CALL=NOT_STARTED. All legacy effect counters remained zero; no other case or
Karrot read ran. Restored live3FE8BE0F in place, verified installed hash,
service PID25058/idle/regular flags and SwiftKey; removed only the fixture.
**Second phone run, November 6:** Andrew announced readiness at10:17 KST.
Reviewed harness932DBB1B/smokev2 90995B07 failed the initial combined
fixture/focus/SwiftKey check after9.474s, before its setup-idle wait or diagnostic
trigger. Product NO_REQUEST/CALL=NOT_STARTED; all effect counters0. Stopped and
restored live3FE8BE0F, verified hash, idle service28207/regular flags and SwiftKey,
then removed the fixture by10:19:52 KST. Investigating owned setup ordering and
separating readiness conditions in the report; do not infer which condition failed.
The product probe and insertion guards were unchanged; no Karrot read ran in that session.
Local source review mapped a separate relative proof into the existing manual
transaction; see RESEARCH. This remains dependent on measured evidence.

**Third phone session, November 6:** after fresh ten-minute availability,
app18FE04FD with independently cleared harness3572B5D2/smokev3 CB426C29 passed
empty3.282s, middle2.272s and selected2.466s. Each observed one read, unchanged
fixture/counters and the required relative/independent-selection classifications.
Standalone status dumps twice timed out after fixture teardown; the instrumented
checks themselves passed and no content request was repeated. Installed reviewed
KARROTC46B46FF; own status responded normally. Andrew prepared an empty composer.
One read on service6327 produced the result above, STYLE=UNSTYLED,
AGREEMENT=SHAPE_AGREES and ELIGIBLE=NEVER, with all effect counters0. Restored
installed live3FE8BE0F/hash, idle service9329/regular flags/SwiftKey; fixture removed
by10:30:24 KST, within the window. No insertion, API, mic or clipboard action.
Next calibration needs its own reviewed/built candidate and fresh availability.
That candidate is now built and independently cleared: 176 unit tests pass,
app lint0errors50warnings, smoke lint0errors5warnings, both assembled. App source
cache3A3A6CC2/testsFFB584D6/probeA3295272/serviceDAB2B763/Gradle6FD7E067;
smokeA22A6D39. Saved under ignored `app/build/relative-selection-20261106/`:
FIXTURE_SELECTION CAAB09CD, KARROT_SELECTION 7A96B1C4, smoke1ED459AC, with
matching signatures/configs. Ordinary debug/release rebuilt and verified OFF.
**Cached-selection phone result:** fresh ten-minute availability began10:42:35.
Owned calibration passed3.662s: one generation2, start1/finish0/restart0,
callbacks0→2→4→5 for initial/typed/selected/cleared. Actual Karrot calibration
service24978 stayed generation4, starts2/finishes0/restarts1 throughout:
initial ZERO/UNKNOWN composition/callbacks0; typed CARET4/COMPOSING/callbacks6;
selected RANGE_0_4/NONE_REPORTED/callbacks7; cleared ZERO/NONE_REPORTED/callbacks8.
Changed stages were CALLBACK_CURRENT_GEN. All snapshots retained older age and
later delivered signals; these are not remote-freshness guarantees. Every effect
counter0, CONTENT_READ=DISABLED. Restored installed live3FE8BE0F/hash, idle
service28758/regular flags/SwiftKey, fixture removed by10:50:47 KST. No text read,
Insert, Copy, Send, microphone or API action. User confirmed sample deletion.
Checkpoint `c1009f3`; compiled smokeA22A6D39 has a whitespace-only cleanup in Git
(0C4D445B), verified by whitespace-insensitive diff before commit.
Exact protocol: [INSTRUMENTATION](docs/INSTRUMENTATION.md#karrot-relative-read-diagnostic--november-6).

**Current product direction:** Andrew proposed returning to the working manual
workflow after the automatic insertion experiments. Proceed with the manual MVP:
record → edit and approve English → translate → review the Korean on the result
panel, with the option to correct it → Insert or Copy & close. Explicit Insert
still checks the field and cursor afresh. Both actions must use the Korean the
user last saved. The October design preference to hide the Korean preview has
been superseded. Keep the automatic experiments available for later investigation.

**Installed on Andrew's A35:** the secure manual MVP with editable Korean is
installed and verified (SHA-256
3FE8BE0FF5AFD51908177D81F6AF652FB6EB0C34A85CD68825731E16DA3D33EA).
The accessibility service reconnected at PID 28758 after the latest restoration. Speech and translation are
on, automatic insertion is off, the draft is empty and SwiftKey is selected.
The fixture was removed from user 0. The previous live build D266E911 is saved.

**Device acceptance:** all four manual Korean review checks passed with fixed
samples. Discard restored the original. A long correction scrolled to the end
with SwiftKey naturally visible and Insert and Copy reachable. Unchanged Korean
and a saved correction with emoji each inserted once at the newly chosen middle
cursor. Both preserved the other field, surrounding text and exact caret.
The separate live build uses the same result UI and writer. Device tests used
synthetic edits to KoScribe's editor and left the clipboard untouched. Source
and unit checks verify the saved result supplies Copy. A fresh live translation
and physical Korean typing were outside these checks.
[Full phone evidence](docs/DEVICE_TESTS.md#manual-korean-result-review-on-november-5)

**Build and source:** both app variants passed 159 unit tests, assembly and app
lint (0 errors, 50 warnings). Independent review cleared service E4BB7D07 and
draft 754C641C. App checkpoint `d40cd21`, final harness checkpoint `7000290`.
Archived in `app/build/manual-korean-result-20261105/`: sample AE1D1C6B,
live 3FE8BE0F and smoke B7962357. Ordinary debug/release still default to OFF.
Andrew can use the installed workflow while Karrot compatibility is developed.
External automatic insertion and OpenAI dot acceptance remain deferred.

**Automatic checkpoint, November 5:** C and D passed the identical-field switch
with usable manual recovery. Neither unchanged-middle test wrote any text. The
fresh original field, caret and context matched twice. D's complete trace showed
six window-change notifications timestamped before the first fresh read and
delivered after the transaction started: four layer changes (bubble host then
three system windows/events), a system window added, and keyboard bounds changed.
The guard refused TARGET_CHANGED with zero commits. The purpose of the added
system window was unknown. No trial of a timing exception or broader eligibility
was run. Empty/Cancel/caret/context gates, actual automatic translation and
external support remain untested. [Device record](docs/DEVICE_TESTS.md#system-managed-swiftkey-and-first-guarded-return-on-november-5)

**Reproducible baseline:** C and smoke v2 are in the ignored
`app/build/stable-overlay-auto-20261105/` directory at source checkpoint
`f784177`. D is `stable-overlay-precommit-D.apk` (6757F300), using smoke v2
(FE40499D). D passed 156 unit tests, assembly and app lint (0 errors/50 warnings).
See [INSTRUMENTATION](docs/INSTRUMENTATION.md#stable-editor-automatic-sample-2026-11-05) for the historical test sequence.

**Reviewed stable sample:** `app/build/stable-overlay-auto-20261105/` contains
`stable-overlay-sample-v1.apk` SHA-256
8D8F27BC5BCABDFBAE062EFCB88F33A07C926E63CC9D7BF956F57B3C4AD9B3FE and
`smoke-stable-overlay-v1.apk` SHA-256
353EED3B25D9A722F869FFD73E03CDF61D78E132F2D838B3C54FEE4F2ABF746F.
Service637B2B99, adapterE1B44BD0, policy tests3907BBDA; harnessD383EDDA,
fixture5A3190DE/IDsBDD1FB9F. Astra/xhigh cleared exact source for the signed
stable fixture. All150 final unit tests passed, app lint0errors50warnings, smoke
lint0errors5warnings; both assembled. Saved config: secure REVIEW, only
STABLE_OVERLAY_AUTO=true; existing experiments false and review probe OFF. Signer
matches the MVP. Ordinary debug/release verified OFF/allfalse after archiving.
No microphone, speech API or actual translation in this first fixed-sample gate.

**No-write comparison result (November5):** the original window/root/node was available
in overlay review samples (2/2/2 in each case), absent in Activity samples (2/0/0
positive,4/0/0 negatives). Both positive arms returned unchanged original identity,
caret/context with a fresh target input session after actual editing. Both detected
a different identical-content field. Both matched the same view after a hidden
fixture logical rebind: public evidence cannot distinguish that change. These are
sampled observations, not continuous availability or semantic identity proof.
First-null IME appeared in both positive probes, NONE in the four negatives;
November4's missing-root role remains unknown. All legacy transaction capture,
commit, auto-attempt and recording-proof counters0; clean UI/draft cleanup; no
microphone/API/key/clipboard/Send. Full table: [DEVICE_TESTS](docs/DEVICE_TESTS.md#review-surface-comparison-2026-11-05).

**November5 local validation:** Astra Ultra implemented the app; Sol/xhigh the
six-case harness; Astra/xhigh independently cleared exact final source for this
hard no-write scope. All144 unit tests passed (nine new policy tests); app assembly
and lint passed with0errors44warnings; smoke assembly/lint0errors4warnings. Both
secure arms are REVIEW with all other experiments false. Final serviceB8B3E145,
controller266D6D7F, policy5899D291, policy tests25D42213, ReviewActivityCB6ED629,
ApiKeyActivity275147B7, GradleE4E89DCD; smoke31AF37FC, fixtureAB074DC6, IDs83AEA44A.
The existing manual writer is unchanged when the probe is OFF. A single original
capture precedes bounded500ms host preparation; review lasts at most120s and
return observation2s. A late return read is discarded. No microphone/API/key,
clipboard, draft or insertion authority exists in these probe sessions.

**Saved comparison artifacts:** ignored `app/build/review-surface-20261105/`:

- `review-activity-v1.apk`:453F8C8968D2AD643849B2323BB0EED4CD432BBDE51C77B609FB643F4357E939
- `review-overlay-v1.apk`:1B61502046A4B75ED8E30D82E08A24B831DD15896615474849151D293C089FE9
- `smoke-review-surface-v1.apk`:BAD61EC848A10E41AE56C868B8D17CA662C89F6421AC6179AE1C6FFFCEC8760E

Matching arm BuildConfig files are saved beside them; both app signatures match
the saved secure MVP. Exact methods/sole flags and evidence meanings are in
[INSTRUMENTATION](docs/INSTRUMENTATION.md#review-surface-comparison-2026-11-05).
Normal debug APK/release BuildConfig were rebuilt and verified OFF/allfalse.
Keep diagnostic completion distinct from automatic eligibility.

**Deferred automatic goal from November 4:** automatic Korean translation and insertion after
English approval, without the intermediate Insert action on the verified path.
Andrew reopened this work on November4, superseding the October26 deferral.
Karrot/OpenAI dot compatibility remains a separate follow-up; old real-app
read/write trial scopes are not reopened. Automatic eligibility remains owned
fixture only, debug opt-in and default/release OFF.

**November5 decision after outside advice:** Andrew requested assessment of
OpenAI-sidechat, Gemini and Claude suggestions. Primary-source/code review supports
reassessing the complete-window validator; no suggested weaker write predicate
is accepted. ThumbFree's session/virtual-field tests are useful, but its preview
is noneditable and its fallback/postcheck rules differ from KoScribe. A focusable
accessibility overlay still transfers keyboard/input ownership. KoScribe already
uses the accessibility InputMethod/commitText route recommended by Claude.
The next work is a reviewed no-write comparison described below, rather than
another series of global-inventory exceptions. That assessment itself changed no
app code or phone state; the subsequent authorized probe work is tracked above.
Reviewed source remains `80b1eb9`; consultation notes were published at `d4c0c84`
on `codex/approve-translate-insert`. Repository visibility is now public, verified
after Andrew's pasted account of making it public; the agent did not change it.

**Preserved November4 automatic-candidate result:** sample9D5B00D6 passed retained-host preparation
and the new exact original focus-loss observation. It then safely refused
`ENTRY_INVENTORY_WINDOW_ROOT_MISSING` on a passive POLL during entry. One remote
window root was unavailable; **which window is unknown**. Do not infer it was
the original, review, or IME. No full entry, automatic return, or automatic phone
insertion has passed. Exact Cancel-only test completed5.175s on service29021.
Host acquired/review/return1/0/0; inventoryFAILED/completions0; attempts,
destination captures, commits and manual renders0. Original identity returned
stable after1removed/1added. Cancel cleared draft/cleanup; mic560ms stopped.

**Implemented/reviewed:** one authenticated original removal/addition; retained
same view/token/window through review and return; visible Cancel in a reserved
owned slot; bounded IME readiness without deadline extension; all destination,
precommit, one-attempt and postcommit safeguards. INITIAL layout callbacks cannot
admit incomplete evidence. No SYSTEM exception remains. The finite inventory
model verifies declared original/review and IME transitions, survivor identity,
geometry and rank order. Own-host preparation has fixed OLD/TARGET endpoints and
500ms deadline; API34+ move animation is disabled on that host. The exact96
focus-loss observation pins own local identity, remembers regression history,
and grants no anchor/readiness. Full removal and DOWN/UP completion remain required.
Astra Ultra implemented; Astra/xhigh and a second Astra/Ultra independently cleared
the final source. **135 unit tests passed; app lint0errors41warnings; build passed.**
Smoke source was independently reviewed and built/linted (0errors4warnings).
Ordinary local debug APK and release BuildConfig were restored to OFF/allfalse.

**Saved final candidate (not installed):**
`app/build/auto-insert-20261104/AUTO-prefocus-v1.apk`, SHA-256
9D5B00D6582D235AACF4CB58109723F8EC23E3AC4E9E172B38A44F121952A8B7.
Reviewed source SHA prefixes: service39EC2A5D, ReviewActivity85D9B60D,
EntryWindowInventoryE438AF69, EntryWindowInventoryTestBD8418D8 (34model tests).
Matching BuildConfig is saved beside the candidate: REVIEW/auto=true; speech,
translation, visual and all Karrot probes=false. Harness `smoke-inventory-v1.apk`
in the same directory, SHA-256
BA587844D1A4B9D8028CB92F360F6A0808B30DCEF59A6BEE65575075EEC339CD.
Harness source hashes: AutomaticInsert5FACB13A / AutoLifecycle61BBF8DA.
Saved APKs are ignored local build artifacts, not Git deliverables.

**Next fixture gate:** preserve the no-write comparison unchanged. Use a separate
opt-in flag and a source-controlled stable fixture editor. Pin original root/node,
window and input session at deliberate selection; require the authenticated own
overlay/editor handoff and fresh return. Original context is UNKNOWN while review
owns input. Original disappearance, unexpected input start, target contradiction,
timeout or missing evidence ends auto eligibility; never rearm it. Reuse fresh
precommit, one-attempt consumption and exact postcommit confirmation. Show manual
recovery before a write when proof fails; uncertainty after a write stays terminal.
First gate: fixed Korean into an empty and a prefilled middle caret on approval,
one confirmed commit, no manual Insert panel, adversarial changed-field/caret and
Cancel cases, no Send. Do not consume the hidden fixture oracle or claim a generic
conversation-identity guarantee. Live translation and external acceptance follow.

**November4 decisive frame:** POLL/BASELINE, launchageLT1000, metadata0, anchor0,
reviewProved0, hostProved1, preAnchorProved1, entryActive1; owner present/same,
local review service/unlocked/own/local/input/wait1, remote/root/editor/sole/full/
bound0. Membership counts are UNKNOWN because the inventory read failed.
Use this to classify the gap before deciding any next implementation.

**Acceptance still required:** the saved candidate needs full entry inventory
COMPLETE and review proof; any replacement needs its own separately reviewed
entry/return proof rather than inheriting this implementation requirement.
Both still need owned automatic empty return/insertion, middle preservation and
adversarial cases, actual translation, and separately authorized external acceptance.
In the saved candidate, RETURNING layer changes stay strict and no-removal review
paths remain unsupported.
If evidence shows skipped epochs, late DOWN against current UP, or recurring
undeclared transitions, reassess architecture instead of adding exceptions.
The completed accessibility-overlay comparison supports adapting the review
surface for a separate fixture-only candidate; no product replacement is accepted.
All trials remain fixture-only; real-app read/write scope is not renewed.

**Phone left ready for normal use:** normal secure D266E911 build restored in
place; installed APK hash verified exactly, service24728 idle, speech/translation
enabled, auto/probes false and SwiftKey unchanged. Main app/key data preserved.
Saved baseline: `app/build/design-install/KoScribe-0.2-translation-final.apk`, SHA-256
D266E911F443B896C33E49DFCE9EDBA5705F50D1E4515956CF045DEB59DAD234.
The owned `com.koscribe.smoke` package was removed from user0 after testing.
On November4, Play Protect twice asked for a security check during restoration; Andrew selected
Send this time himself both times. The second restoration completed successfully.
The requested temporary system-prompt screenshot was deleted on phone and PC.
No external-editor content read/write, conversation image, KoScribe speech or
translation request, credential change, clipboard action or message Send occurred.
Live credential/backup remain untouched.

Detailed results and candidate history: [DEVICE_TESTS](docs/DEVICE_TESTS.md).
Technical rationale/limits: [RESEARCH](docs/RESEARCH.md).

**Native design upgrade installed, 2026-10-31:** Andrew approved installing the
comparison design and replacing the visible Phase 1 name. Sol/xhigh implemented
the rounded-square Spoken K bubble and launcher icon, compact three-mode/result
controls, Quiet pulse recording panel, English review, and launcher. The app and
accessibility-service labels are now `KoScribe`; `applicationId` and signing are
unchanged, with version 0.2 (code 2). Astra/xhigh cleared the app diff after a
Move-drag fix and separately cleared updated smoke selectors. The translation-
enabled build passed 90 unit tests, lint (0 errors), and assembly; its signer
matches the prior installed build. A35 screenshot-safe sample checks at font
scale 1.15 showed the bubble, mode buttons, Stop/Cancel, English review, and
result Insert/Copy/Cancel visible; Move tap repositioned the result. The first
review layout left a large blank gap, so Sol moved the transcript card next to
the fixed action dock and Astra cleared that source change. The follow-up A35
sample check showed the card directly above Confirm/Cancel and SwiftKey, and
Cancel returned to the owned empty editor. Temporary screenshots were removed.
The **secure speech/translation APK** with the final spacing change is saved at
`app/build/design-install/KoScribe-0.2-translation-final.apk`, SHA-256
`D266E911F443B896C33E49DFCE9EDBA5705F50D1E4515956CF045DEB59DAD234`.
The installed `/data/app` APK has that exact hash. Its reviewed flags are
`STARTUP_EXPERIMENT_ARM=REVIEW`, speech=true, translation=true, and
visual/auto/Karrot probes=false.
Android reports package version 0.2, the same app-data inode, active KoScribe
service, and SwiftKey selected. The key was not accessed. The separate
`com.koscribe.smoke` test launcher and hidden `com.koscribe.prototype.test`
instrumentation package were removed; only the main KoScribe package remains.
No real-app insertion, speech API request, or Send was used in this design check.
Ordinary `app/build/outputs/apk/debug/app-debug.apk` was rebuilt with all
feature/test flags OFF; use the saved final APK for a live reinstall. The
source/memory checkpoint preceding this final acceptance is on private GitHub
at `d43bb24`.

**Visual audit and design review, 2026-10-31:** Andrew requested screenshots of
the app flow and a visual comparison website. Nine accepted screenshots of the
owned synthetic flow were captured using a temporary screenshot-safe sample
build; the protected live surfaces were not captured. The reviewed live
recording-indicator v2 APK was
restored in place, with speech/translation on, auto/Karrot probes off, and
SwiftKey selected. One fixed sample was inserted only into the owned test
editor, with a confirmed commit, no Send, and the small bubble still available.
The evidence and deferred redesign proposals are in
[the visual audit](docs/design/UX_AUDIT_2026-10-31.md) and
[the standing checklist](CHANGES_PROPOSED_FOR_LATER.md). The
[owner-private comparison site](https://koscribe-design-review.user78395.chatgpt.site)
was revised after Andrew's feedback. It now opens on the accurate current
Spoken K bubble and compares three directions across five daily flow stages;
the launcher/app home is separate. The proposed bubble is a rounded square,
mode and review panels are simpler, and the result has a Move handle and no
unsettled Korean preview. Hide/Close behavior is explained. The Site uses the
approved native foreground asset and otherwise HTML reconstructions; it is not
the Android app. Reviewed Site source `2f38f8bcf2222355209790bf5885519ce41e2d66`
deployed owner-private on October31. The native implementation status and
remaining phone check are recorded above.

**Previous baseline build:** reviewed recording indicator v2, SHA-256
`159E2452C057E49A96751D5D277DE2704FB7719A12B0B2ABE40669B9659C538E`,
saved at `app/build/recording-indicator/TRANSLATION-recording-indicator-v2.apk`.
It adds a mode-labelled recording banner, elapsed timer and decorative 11-bar
pulse only after microphone proof. The long panel disclosure was removed; the
launcher retains the audio-to-OpenAI/no-storage disclosure. Sol/xhigh implemented
and Astra/xhigh independently cleared both versions. All 90 unit tests, a fresh
lint run (0 errors), and the translation-enabled REVIEW build passed; the APK
uses the same signing certificate as the prior installed build. Speech and
translation remain enabled; automatic insertion and Karrot probes remain off.
On the A35 owned Test message screen, Andrew confirmed the first 3-bar version
recorded/animated and Cancel worked, but its bars were too small. The 11-bar
revision also recorded/animated, kept Stop and Cancel visible, and cancelled
without insertion. He found its decorative bars too large, but chose to keep
this version while considering a better indicator during use. Own diagnostic
trace: recording proof1, USER_CANCELLED, draft NONE, commit0, cleanup false.
No speech API, translation, insertion or real-app behavior was exercised by
these visual checks. Further indicator refinement is on the later list.

Andrew asked whether MORLUTO's REA can help study Wispr Flow. Public-source
review is recorded in docs/RESEARCH.md: REA can
statically inspect Android APKs, but Wispr's terms restrict decompilation to
access its proprietary code/algorithms. No REA installation or Wispr APK access
occurred. Wispr's public Play listing identifies the accessibility input
connection, and its setup guide acknowledges clipboard fallback when insertion
fails. Continue with public/observable comparison before considering any APK
analysis. The no-read Karrot diagnostic has now run; regular BAA162DD is again
the verified installed phone build.

## Current state

On 2026-10-29 Andrew requested work on Karrot/Instagram editor compatibility,
then prioritized checking whether reselecting the Karrot field helps. He clarified
that Instagram comments first refused verification, then succeeded after he tapped
the field again; this is user-reported, not a new diagnostic attribution. Own
process trace showed INPUT_NOT_STARTED followed by one confirmed commit, without
an app identifier. Karrot same-node metadata was unchanged after refresh: focused
and editable false, input type zero, matching package/window. Andrew's manual
empty-field Insert was refused before writing, and refused again after he tapped
the field. Draft cleared; no Karrot commits. A separately opted-in service probe
found a stable input connection and safe text type, matching root/node, absent
field ID, present autofill ID and unknown node selection; node focus/editable/type
stayed invalid. That observation does not prove insertion eligibility. The tested
live app BAA162DD was restored after the metadata test. A separately reviewed
and tested empty-connection diagnostic 6C902906 was temporarily installed
with speech/translation enabled; Andrew approved one local read from a Karrot
field he confirmed empty. USB later reconnected, and the one-shot request
returned `INTERRUPTED`, without a usable cursor result. The categorical status
does not establish whether the worker made its local read before interruption;
do not retry under the one-read authorization. The regular live build BAA162DD
was restored in place afterward; its service reports speech/translation true,
automatic insertion false, no draft or pending overlay cleanup. SwiftKey remains
selected. Karrot Insert remains unsupported on the measured path; preserve the
insertion guards and offer Copy & close as recovery. Astra/ultra source analysis
identified the broad callback interruption paths; it could not identify which
one occurred. A separate hard no-read, debug-only cause/phase probe was
implemented by Sol/xhigh and independently cleared by Astra/xhigh. All 84 unit
tests and app lint passed; candidate APK 649F03BD (speech/translation true,
auto and prior content-read probe false) is saved under app/build/editor-compat.
Its signature matches the regular APK. After Andrew reconnected and prepared
the Karrot field, the candidate was installed in place while KoScribe was idle.
Its old content-read probe was disabled. The sole no-read check returned
`WOULD_READ SOURCE=NONE PHASE=SECOND_YIELD_WOULD_READ`: Karrot's input/root/node
metadata stayed stable through the pre-read boundary on that attempt. This
does not establish caret/content or safe insertion, and it does not explain the
earlier interrupted read. No editor content was read or written. BAA162DD was
then restored in place; service PID20869 reports draft NONE, speech/translation
true, automatic insertion false, cleanup false. SwiftKey remains selected.
Andrew then reported that Wispr Flow directly inserted an unsent test into
Karrot on the same phone. This is user-observed behavior, not evidence of
Wispr's specific internal method. It makes a Karrot-specific KoScribe adapter
worth investigating, while the existing safety checks remain in force. An
independent Astra/ultra architecture review concluded that a Karrot-specific
manual adapter is plausible only with fresh input-connection caret/context
evidence through capture, precommit and postcommit. `autofillId` alone may
identify a virtual-editor container, not a field. The earlier one-read approval
was consumed. Sol/xhigh added fixed interruption/phase/call-stage telemetry to
the existing opt-in one-read diagnostic; Astra/xhigh reviewed it CLEAR after one
attribution correction. All 90 unit tests and app lint pass. Signed candidate
APK 5CFDBD64 is saved under app/build/editor-compat, with speech/translation
true, automatic/no-read probes false and the one-read probe true. Andrew gave a
fresh approval. The first run, after deleting the old Wispr test, refused
`INITIAL_SELECTION_INVALID` at preflight with `CALL_NOT_STARTED`; no text read.
Android documents EditorInfo's initial selection as the value when input began,
so a stale starting value after deletion is plausible, not measured. After a
fresh chat open, the sole actual read returned `OFFSET_UNKNOWN` with
`CALL_RETURNED` and `SOURCE=NONE`: the returned sample was empty, but Android
reported no absolute text offset. No text was logged/uploaded and no write or
Send occurred. This consumes the approved local read. It does not establish
the caret position or safe insertion; do not weaken Insert on this evidence.
Regular BAA162DD was restored in place; service PID11417 reports speech and
translation true, auto false, draft NONE, cleanup false, and SwiftKey remains
selected. Ordinary debug/release output flags remain OFF/all false. A future
Karrot-specific adapter would need relative caret/context and exact
postcommit evidence despite an unknown offset, with owned-fixture tests before
any separate real-app insertion approval. Copy & close remains the recovery.

Andrew is now trying the installed MVP and will bring immediate fixes back as
needed. Later proposals and unfinished acceptance work are tracked in
[Changes proposed for later](CHANGES_PROPOSED_FOR_LATER.md), including the supplied
Wispr notification image and its documented/inferred meaning. No new runtime
work or phone tests are started by that list.

Latest MVP feedback: KoScribe would not paste into Instagram comments, although
Instagram DMs work. Cause and ordinary clipboard Paste are unverified; recorded
for later in the compatibility item. Earlier feedback: English transcription missed “Dangun pay” and “Twosome cafe”.
Custom vocabulary/correction rules are recorded as deferred item 10 in that list;
the user's exact example spellings are retained pending future implementation.

**Selected stylized K installed and phone-tested; live speech/translation enabled.**
Andrew selected the middle concept, Spoken K. Service 95ECC800 retains the
56×48 dp native Button/tap/drag behavior and uses the selected original graphite
mark on an 80% opaque light gray body. Source/foreground asset and independent
Astra/xhigh review are recorded in docs/design/floating-button and DEVICE_TESTS.
Build/all 78 units/signatures passed; app/smoke lint 0 errors (34/4 warnings).
Installed `app/build/floating-k/TRANSLATION-stylized-k-v1.apk`, SHA BAA162DD,
PID22160: secure REVIEW, speech/translation=true, automatic/visual=false.
Normal voice was never replaced by a sample build in this icon update; key/data
preserved. Default debug/release outputs restored and verified OFF/allfalse.

Focused owned-fixture icon test PASSED in 4.510s: open modes, Close, drag and restore,
empty editor/SwiftKey maintained and proof/capture/commit counters unchanged at 0.
Only the installed drawable was rendered/exported for visual QA; no screen image,
recording, API call, clipboard operation or insertion. Initial case-sensitive
test failed finding Close; corrected helper 3EDA81BC independently reviewed.
Standalone service dumps intermittently time out (also before icon update);
this cause remains undiagnosed, not fixed by the matcher correction. The passing
test read idle draftNONE/counters0 internally; final AppOps still shows only the
prior stopped 4.716s recording. SwiftKey unchanged. Phone work is finished.

**Earlier October26 MVP acceptance remains the behavior baseline.**
Andrew explicitly selected manual Insert plus Copy & close and deferred automatic
insertion. Completion now closes only the result panel and keeps collapsed Ko
available while SwiftKey is open. Explicit Hide/keyboard visibility and all Insert
safeguards remain. Final service AF6DC0A4 has only completion-collapse and clearer
Copy recovery wording changes; no click-rearm policy/event subscription remains.
Astra/xhigh source/test review CLEAR; build/all78 units/signatures/lint0 errors
passed. Both owned Insert-once and Copy cases passed with continuous SwiftKey,
collapsed Ko and mode-panel reuse, without another recording/write/keyboard cycle.

Previous real speech/translation MVP: TRANSLATION-persistent-ko-v3 SHA EA8D45E0,
PID22101 (superseded by the icon update above). Andrew's actual speech→EN review→Copy & close Karrot check PASSED:
words matched and Ko stayed available. Own proof1/COPIED, draftNONE/cleanupfalse,
capture/commit0, automatic=false, speech/translation=true, stopped mic4.716s,
idle READY/IMEvisible/wanted/shown and suppressionfalse corroborate the report.
No new Karrot Insert or Send occurred. Key/data preserved. Default debug/release
outputs restored/verified OFF/allfalse. Phone actions are finished.

Karrot bounded diagnosis is complete for this MVP. Before updating, saved manual
process27092 retained NODE_EDITOR_MISMATCH refusals, followed later by successful
EN insertion in an unattributed app. User prepared empty Karrot/SwiftKey; exact
metadata-only probe found matching root/node package+window, but focused=false,
editable=false,inputType0/resource absent. No text/history/UI action was read or
performed. This is incompatible with current guarded Insert. Keep Karrot on
Copy & close/manual Paste; do not weaken Insert or start a custom adapter now.
New refusal wording points to that recovery. Key/data untouched.

The abandoned click-rearm candidate passed owned tests but failed Andrew's manual
Karrot reuse check, motivating the selected persistent-Ko behavior. Its temporary
sample/live APKs remain ignored artifacts only. Do not reinstall them as current.
Automatic insertion stays false throughout. No automatic continuity work.
Selected artwork/provenance is now saved in `docs/design/floating-button/`;
native foreground is `app/src/main/res/drawable-nodpi/ko_spoken_k.png`.
The three previews and display order remain recorded in the October26 session
history; Spoken K is the selected design, not a pending decision.

October25 checkpoint (historical): restored manual v2, PID27092 idle and mic off.
Automatic insertion remained unproven/default-OFF. Paired occlusion is documented
as a proposal, with no code written. A Korean preview remains optional and is not
part of the two current requested fixes. Earlier evidence follows.

Andrew now explicitly authorized the one-button Translate & insert experiment,
with manual fallback when original field/cursor cannot be verified. He accepted
test mode first. Root uses AstraUltra architecture analysis, Sol6/xhigh policy
and service integration, then independent Astra/xhigh review before build/device.
New opt-in AUTO_INSERT candidate preserves v2/manual behavior, allows automatic
eligibility only in the owned smoke fixture initially, and retain no old write
transaction. Read-only intent begins at deliberate mode click; only verified
return permits a fresh guarded transaction. External apps stay manual. Source is independently
cleared by Astra/xhigh, including the pending-key Hide/Close cleanup race fix and
manual fallback. Build/all77 units/signatures passed; lint0errors with app35/smoke3
warnings. API-free AUTO-sample-v1 and smoke-auto-v1 are installed, data preserved;
ordinary debug/release restored OFF with all booleansfalse. First exact empty
test stopped at FIXTURE_NOT_FOCUSED during setup, before mode selection. Own
PID32396 still has all counters0/draftNONE/cleanupfalse and mic stopped, SwiftKey
unchanged. No automatic behavior was exercised. New harness required exact node
caret0 where old working helper checked focus/text only. Reviewed smoke-only
correction EDBDEFA4 built/signed/installed as smoke-auto-v2; it accepts -1/-1 only
with refreshed owned node and direct owned EditText empty/focused/caret0 proof.
The rerun reached capture/recording/review: originalcaptured1/proof1, then terminal
FALLBACK REVIEW_WINDOW_UNVERIFIED before any accepted own-input-start callback.
Attempts/captures/commits0, draft cancelled by harness cleanup, mic ended582ms.
Diagnostic-only v2 was independently cleared, built (all77 units; lint0 errors),
signed and installed. The same empty run again fell back before any write.
Its fixed-code trace proves the local focused review window/editor existed while
accessibility reported no focused application window and no registered review
window; the input generation was still original. This is a startup readiness
ordering failure, not evidence of a changed destination. PID9446: proof1,
originalcaptured1/fallback1, attempts/captures/commits0, draftNONE/cleanupfalse;
mic ended517ms. The bounded readiness fix was independently cleared and built;
all78 units/signature passed, lint0 errors/35 warnings. Installed v3 SHA619C9241.
Fresh empty run reaches AUTO_REVIEW_ENTRY_WAIT, then a separate terminal
UNEXPECTED_WINDOW_CHANGE before own-input/ready proof. PID13306: proof1/intent1/
fallback1; attempts/captures/commits0, draftNONE/cleanupfalse; mic ended525ms.
Diagnostic-only v4 source58C392A9 was cleared, built and installed (SHA79F9B730).
The next empty run identified ORIGINAL WINDOWS_CHANGE_REMOVED (mask2), 20ms
into entry wait, while the expected own review was present and uniquely focused.
PID16450: proof1/intent1/fallback1, no attempts/captures/commits, draftNONE and
mic575ms stopped. Next is an exact opt-in NON-INSERTING fixture lifecycle trace
through review/Cancel/return, checking whether original window/root/node identity
survives. Product guards stay unchanged. No automatic insertion has passed yet.
Checkpoint d9364ba is pushed to verified private origin/main. Dedicated lifecycle
test C6214251 is independently cleared, smoke build/signature/lint0 errors passed,
and smoke-lifecycle-v3 installed (SHA7631FD3D). Product v4 is unchanged. Andrew
unlocked the phone; the lifecycle run reached review and Cancel, then stopped at
FOREIGN_FOCUSED_APPLICATION before return evidence. During review the exact direct
Activity/editor/token stayed attached/empty/caret0 with all lifecycle deltas0,
while original accessibility window/root/node were absent. Same review owner
passed pre-Cancel checks. No writes; PID16450 proof2/intent2/fallback2, draftNONE,
mic555ms stopped. Harness currently conflates null root/package metadata with
foreign application. Test-only correction will distinguish bounded not-ready
from an explicit foreign package; this suspected cause is not yet measured.
Corrected smoke CB282F6F built/signed/installed as smoke-lifecycle-v4 (SHAD2A1FB5D).
The same Cancel-only lifecycle case now PASSED: original accessibility window/
root/node and direct Activity/editor/token returned identically, empty/caret0,
create/attach/detach deltas0, at first return and300ms settled. One exact REMOVED2
and ADDED1 pair observed; return192ms after Cancel (observer timing). Product
PID16450 proof3/intent3/fallback3, no attempts/captures/commits, draftNONE/cleanupfalse,
mic497ms stopped, SwiftKey unchanged. This supports a narrow paired occlusion
protocol, not automatic insertion acceptance. AstraUltra's proposal is documented;
Sol service/policy agents stopped without implementing it at Andrew's request.
Older v2 baseline details follow; prior priority uncertainty is superseded.

Andrew resumed on October25 from clean checkpoint619f132 and explicitly started
Phase3 요/반말 translation. Bounded Phase2 remains accepted; saved secure v5 is
preserved. Two Sol6/xhigh agents implemented the client and UI/state flow;
independent Astra/xhigh source review is CLEAR. Root owns build/device/docs/Git.
Current official documentation verifies gpt-6-luna/xhigh and Responses structured
output; first actual translation now verifies account access. Preserve the live key.
See docs/PHASE3.md for the candidate contract and acceptance plan. Candidate build,
all64 unit tests and signature passed; lint has zero errors/42 warnings. Saved
`app/build/translation/TRANSLATION-v1.apk`, SHA-256
`810D9564F5D169E69E08C5617FE5F14FCE94189AE71280C9252FFC8CEEB4BF18`.
Explicit flags REVIEW/SPEECH=true/TRANSLATION=true/VISUAL=false. Ordinary debug
and release flags were restored/verified OFF/false. Andrew connected/unlocked the
phone and approved debugging. In-place installation succeeded. The first new
service dump timed out; one later check of the same process11530 was responsive,
with all translation/proof/capture/commit counters0, draftNONE, cleanupfalse and
microphone stopped. No restart/toggle was needed. SwiftKey is unchanged.
First deliberate 요 test PASSED in the owned Test message screen: Andrew reported correct Korean after editing
two to three blue notebooks. Same process11530: proof1, translation1/1/0/0,
capture1/commit1/CONFIRMED, REVIEW_COMPLETED, draftNONE/cleanupfalse, hostgone and
suppressed. Mic stopped, SwiftKey unchanged. Stop-to-review-created1761ms;
translation job-to-callback3034ms; usage input194/cached0/output105/reasoning77.
User reported Confirm & translate below the visible screen with SwiftKey and
no Korean visible before Insert. Sol fixed pinned review actions; Astra cleared
the final source. Andrew then questioned whether a preview was useful and
preferred exploring a single Translate & insert action. The unshipped preview
patch was reverted; service/insertion logic is unchanged. Andrew initially chose
translation tests first, then said he is reconsidering the priority because the
field/cursor is already selected when the bubble appears. Do not treat that
priority as settled. Root explained that visibility does not retain/verify the
original destination through recording/review. A future one-button
path needs verified intended-destination return and manual fallback; it cannot
simply call Insert on whatever field appears. The button fix (useful for either
direction) passed build/all64 units/signature, lint0errors/36warnings. ReviewActivity
SHA9F05E2C2; saved and installed `app/build/translation/TRANSLATION-actions-v2.apk`,
SHA-256 `EEE3459902AFF0AE7F15B230D661C5E527AF4BA339ECF6507216761BD78FEEA8`.
Default/release flags restored/verified OFF/false; in-place install preserves data.
New process16745 responds idle: proof/translation/capture/commit counters0,
draftNONE/cleanupfalse, stopped mic and SwiftKey unchanged. No new recording.
Actual SwiftKey review-button visibility is pending. Distinct 반말/action visibility is ready as
the next test if translation-first proceeds. Translation cancellation/error recovery,
반말 and broader quality remain. This historical manual baseline is preserved;
the current automatic experiment is described above.

Andrew resumed work on October 25 from the clean night checkpoint0b0889a.
USB debugging was initially unauthorized; Andrew approved it and the
connection now works. Three own service dumps timed out, including after a user
accessibility toggle. Root verified exact own PID/UID and sent one SIGTERM,
without force-stop, uninstall, data clearing or key access. Own MainActivity
reopened; after a second user off/on toggle, a fresh process responded normally.
Idle baseline: NOT_CAPTURED, proof/captures/commits0, draftNONE/cleanupfalse,
mic stopped, SwiftKey selected. Timeout cause is unknown; retain this lifecycle
incident as a limitation, not a diagnosed/fixed code bug. That recovery involved
no code/build changes; the later diagnostic update is described below.

The offline trial could not start: with Wi-Fi/mobile data off, SwiftKey and Wispr
appeared but Ko did not. One manual keyboard hide/reopen also left Ko absent.
Own service remains responsive/idle with proof/captures/commits0 and microphone
inactive; no recording or API request started. Andrew was told to restore normal
connectivity. Source review finds no network dependency in idle visibility, but
v4 diagnostics could not distinguish a visibility refusal from failed layout.
Sol/xhigh added cached, content-free visibility/render reason diagnostics;
Astra/xhigh cleared the corrected source. Build/signature, all53 units and lint
(zero errors/37 warnings) passed. Installed secure diagnostic v5 with app data
preserved; ordinary/release outputs restored OFF/visualfalse/SPEECHfalse.
First v5 snapshot: FOCUSED_APP_COUNT, inputStarts1, no suppression/host/render,
zero recording/capture/commit counts. At that point system window metadata reported
a focused TYPE_SYSTEM window; this explains this snapshot, not necessarily the
earlier symptom. Andrew now reports Ko appeared after returning to Test message.
The controlled offline test now PASSED on v5: user reported the network error;
own proof0->1, RESELECT_CANCELLED and RESELECT_ENDED_SPEECH_NETWORK, draftNONE,
captures/commits0, cleanupfalse; microphone ended4.858s. SwiftKey unchanged.
The new cached snapshot is READY/IME visible with the bubble attached and shown,
renderVISIBLE. This verifies current offline visibility, not an original-cause fix.
Astra/xhigh accepted both parts of the recovery check. After restoring normal
connectivity, Andrew recorded a distinct new sentence, saw correct English review,
then cancelled; cursor returned and the field remained unchanged. Own proof1->2,
REVIEW_CANCELLED/REVIEW_ENDED_USER_CANCELLED, draftNONE/captures0/commits0,
cleanupfalse; mic ended5.048s, SwiftKey unchanged, bubble READY/shown.
These microphone figures are recording duration, not API latency.
Do not repeat service restarts or weaken eligibility checks. Keep the saved key
intact. Together with the earlier real ASR/edit/Insert, control, setup and source/
unit evidence, this meets the bounded Phase2 gate. Do not repeat passed tests
without new evidence. Earlier readiness cause and broader lifecycle remain open.

The approved postcommit confirmation change is implemented, independently
reviewed, and passed seven separate synthetic phone checks on secure3AA18446.
Exact composed results confirm and dismiss; wrong composed results remain
terminally uncertain. Middle preservation, Korean insertion, selection refusal,
edited-Korean cancellation, and unchanged-commit uncertainty all passed. Forty
unit tests passed; lints have zero errors. Manual empty-field Korean insertion
now passed in Kakao self-chat, Google Messages and Instagram. The bounded Phase1
feasibility gate is met from combined device/unit/source evidence. Phase 2's
first real EN transcription -> review -> Cancel and real speech -> SwiftKey edit
-> guarded insertion/dismissal tests now passed. The clipped Stop fix also passed
manual visibility and active-recording Cancel on controls v4: both buttons fully
visible, no review or text change, microphone stopped. Korean translation remains
unimplemented; Phase2's offline failure and fresh online recovery both passed.

Andrew's newly approved one-attempt Kakao trial PASSED and is now complete.
He reported the exact expected Korean sample appeared and remained unsent,
replying that the flow worked exactly as instructed (including dismissal).
Own baseline proof7/captures6/commits5 -> proof8/captures7/commits6, CONFIRMED,
REVIEW_COMPLETED, draftNONE/cleanupfalse, microphone ended5.450s. SwiftKey remains
selected. This verifies one empty self-chat flow, not other apps or every editor
state. No agent conversation read or phone mutation occurred after his report.
Andrew was told he can remove the unsent Kakao sample manually. No more synthetic
composition diagnosis is needed unless new evidence fails. He approved one
manual unsent sample insertion per Google Messages and Instagram. Google
Messages PASSED: user reported insertion and flow as expected; proof9/captures8/
commits7, CONFIRMED, draftNONE/cleanupfalse, mic ended5.100s, SwiftKey selected.
He described addressing himself; whether he personally sent it is unspecified.
No automated Send occurred. Instagram also PASSED: user reported expected Korean
in the proper DM field and the flow worked perfectly; own proof10/captures9/
commits8, CONFIRMED, draftNONE/cleanupfalse, mic ended3.087s, SwiftKey selected.
Both one-attempt scopes are consumed. Real speech testing uses our synthetic editor.

Astra/xhigh reassessed the remaining gate and withdrew an unnecessary proposal
for another target-change fixture. Earlier REVIEW device tests exercised window/
focus termination without retry; current guard units passed and source retains
event/input invalidation and same-target/host checks before confirmation. All
three app checks now passed, satisfying the bounded Phase1 decision. Actual same-view
conversation switch-back, broad geometry/lifecycle, and daily-use reliability
remain limitations/future tests, not silently claimed passes or new ASR blockers.

## Active Phase 2 work

Sol6/xhigh implemented credential UI/storage and EN speech integration;
Astra/xhigh independently reviewed security, cleanup and final source. Root alone
builds/tests/operates the phone. Speech is opt-in debug `koscribeSpeech=true`,
requiring REVIEW and visualfalse; release and ordinary defaults remain OFF.
The saved sample baseline is preserved. Andrew confirmed private on-phone key
entry and DeveloperStuff backup on October 24. No key value was inspected;
the first user-triggered gpt-transcribe result succeeded. The first actual Korean
translation subsequently verified model access and yielded one timing/usage
sample (current state above); broader speech/translation quality remains unverified.

EN recording -> explicit Stop -> OpenAI gpt-transcribe -> editable English ->
Confirm -> unchanged guarded Insert/Copy. Korean modes are disabled in this
candidate until Phase 3. Audio is bounded in memory to 60 seconds; upload requires
successful microphone release and reader completion. Cancellation, capture limits
and stale results cannot hand off a transcript. The fixed HTTPS request uses
OkHttp 4.12, one-shot multipart, no redirects/replay, bounded response parsing and
call/connect/read/write timeouts. See docs/PHASE2.md for exact behavior.

The private masked key screen uses Keystore AES-GCM and ciphertext in
noBackupFilesDir, verified save/readback, explicit deletion, no key display,
autofill/saved-state opt-outs, and backup/transfer exclusions. Actual OEM transfer
behavior is not tested. Fifty-three unit tests passed, and app/smoke lint had zero
errors (37/3 warnings). Fake save/reopen/delete passed on the phone in 1.123s with
the final smoke helper, and the store was verified empty. Early fixture failures
and exact artifacts are recorded in DEVICE_TESTS.

Missing-key warning test failed at MISSING_KEY_NOTICE_NOT_VISIBLE. Own microphone
proof/capture/commit counts remained zero, draftNONE/cleanupfalse; AppOps showed
no new recording. Source diagnosis: redrawing the bubble before asynchronous
key lookup replaced the clicked host/token. Sol removed only that precheck redraw;
Astra cleared the fix with all continuity/lock/cancellation checks and idle refresh
retained. Rebuild passed all 53 units and lint. Three v3 device runs observed the
warning and passed every no-recording/no-insertion assertion. All three full
methods FAILED later on automated Close cleanup (including the final bounded
own-overlay scroll, OWN_OVERLAY_SCROLL_REFUSED). Stop incidental cleanup retries;
actual menu Close behavior remains unverified. No recording is active and no
draft exists. This is setup readiness evidence, not a full smoke-method pass or
Phase 2 acceptance. Andrew now confirmed key saved and backup done. The non-secret
MindHub checklist records that confirmation. Never rerun fake-key/deletion or
missing-key tests against his live credential. Root verified unlocked/idle,
microphone stopped and SwiftKey selected, then opened the owned single-field
Test message screen. Andrew reported the first deliberate EN sentence was
transcribed correctly and the requested review/Cancel flow passed. Own proof0->1,
REVIEW_CANCELLED, draftNONE, captures/commits0, cleanupfalse; AppOps confirms the
microphone ended after29.097s. That is recording duration, not API latency.
SwiftKey remains selected; no transcript/key/conversation read or screenshot.
No agent-initiated recording/upload, insertion, Copy or Send occurred.

User feedback: tapping EN starts recording, but its status was hard to spot;
he spent time locating the recording indication. He explicitly accepted the test
and deferred more prominent recording feedback/menu improvements to later. Record
that UX requirement for later. The next real speech -> SwiftKey edit -> guarded
Insert test passed: Andrew reported the complete edited text appeared and the
box disappeared. Own REVIEW_COMPLETED, captures0->1/commits0->1, CONFIRMED,
draftNONE/cleanupfalse, latest microphone ended4.368s, SwiftKey selected.
Proof1->3 includes an intervening recording/Stop/review-Cancel trace before the
successful run; its reason was not reported and no insertion happened there.

New measured usability report: Stop is only half visible at the bottom of the
recording panel, although the user could tap it. Fix this narrow reachability
issue before further recording checks. Sol6/xhigh implemented a speech-only
pinned Stop/Cancel row outside scrolling explanatory content; Astra/xhigh cleared
the source. Same host/window dimensions and all microphone, upload, insertion
and key guards remain intact; broader status/menu polish stays deferred.
Controls v4 built/signed, all53 units passed, app lint zero errors/37 warnings.
Installed with replace-in-place, no uninstall/data clearing/key access. Default
debug/release outputs restored OFF, visualfalse/SPEECHfalse. Own service reconnected
idle with proof/captures/commits0, draftNONE/cleanupfalse. Andrew then confirmed
both buttons fully visible without scrolling and direct Cancel behaved exactly
as requested, without review or text changes. Own proof0->1, RESELECT_CANCELLED,
draftNONE/captures0/commits0/cleanupfalse, no Stop event; microphone ended8.803s.
SwiftKey stays selected. This accepts current-phone control visibility and active
Cancel; it does not accept every font/window geometry or cancellation in processing.
The October24 night checkpoint was saved. October25 recovery subsequently passed;
the current requested-stop checkpoint is recorded above.

Previous secure980E build Kakao self-chat microphone -> Stop -> English review -> Cancel
PASSED. Andrew reported the sample review closed and the composer cursor/SwiftKey
returned, with no Confirm/Insert/Copy/Paste/Send. Own proof6 -> 7, captures/commits
unchanged4/4, REVIEW_CANCELLED, draftNONE, cleanupfalse; microphone ended5.265s.
No field/chat content or screenshots were read. Clarified that the microphone
runs and audio is discarded; the displayed sentence is fixed, not transcription.

The first remaining synthetic regression, long middle insertion preserving
Korean/emoji, then FAILED in12.853s at SUCCESS_NOT_CONFIRMED. Its exact-text
assertion passed. The installed diagnostic now measured the missing conjunction:
at29ms, full target/host identity, exact expected context/caret and node agreement
all true while one composing span115..121 remained. Later samples at57-67,
207-217 and557-572ms retained exact125-unit context/caret121 and that same span.
Transaction stayed UNCERTAIN with one commit; test Cancel cleared the draft.
Final proof8/captures5/commits5, cleanupfalse, mic ended624ms. No retry.
Astra Ultra reviewed this new evidence and proposed exact-result confirmation
independent of postcommit composing metadata, with every precommit guard intact.
Andrew explicitly APPROVED that prospective change. Sol6/xhigh implemented product/
unit changes and deterministic smoke fixtures; Astra/xhigh cleared both.
Composed exact result may confirm only with full original-target continuity;
composed nonmatch remains immediately uncertain, and no ended transaction can
be upgraded or retried. The reviewed candidate passed the seven checks above;
the old failure remains a historical failure, not retrospectively confirmed.
The new middle pass did not observe composition; the deterministic exact and
natural Korean checks did. Detailed evidence and limits are in DEVICE_TESTS.

The corrected manual SwiftKey flow now PASSED on secure980E4299...: Andrew
edited the English sample, confirmed, tapped the destination and then the
floating Insert button. He saw the edited sentence and dismissal. Own state
confirmed one new capture/commit (2/2 -> 3/3), CONFIRMED, draftNONE, cleanupfalse;
recording proof count4 -> 5 and microphone use ended after5.318s. This is the
first accepted end-to-end manual edit/Insert flow, not general app/composition
acceptance. Andrew's separately authorized shorter flow also PASSED on the same
build: actual SwiftKey edit/Confirm, then floating Insert without retapping the
restored destination. User reported text insertion and dismissal; own proof5 -> 6,
captures/commits3/3 -> 4/4, CONFIRMED, draftNONE, cleanupfalse, mic ended3.415s.
Independent source/evidence review confirms Insert itself creates a fresh
capture, with no recent-tap prerequisite. Conditional instruction: check the
intended field and cursor, tap the field only if needed, then floating Insert.
This proves the empty synthetic restored-focus case, not that every app returns
focus correctly. No code/build/guard change; post-report verification was read-only.

Andrew asked to continue. A debug-only "KoScribe test editor" launcher was added
to the smoke app, independently reviewed, built/linted, installed, and opened.
It launches only the internal synthetic fixture with the protected field omitted;
it forwards no caller data and automates no product action. Andrew completed
SwiftKey editing and Confirm English, but the large ready panel obscured the
destination. He tapped a separate Paste action, then Insert; insertion was refused
for ACTIVE_COMPOSITION before any product commit. The draft expired. Manual
end-to-end acceptance failed; do not attribute this to user error or count a pass.
Sol implemented and Astra reviewed a compact draggable result toolbar with
clearer refusal feedback; all insertion/composition guards stay intact. Its first
secure-build empty Insert regression failed after one commit: exact text appeared,
but postcommit SPAN_COMPOSING caused terminal uncertainty. This differs from the
manual precommit refusal; cause/timing remains unresolved. No retry occurred.

The synthetic visual session finished and secure REVIEW was restored afterward.
Andrew reported successfully dragging the whole compact control by its Move
handle and selecting the top field. Its hint was mistaken for residual text;
two fixture fields also caused confusion. The screenshot after his reply already
showed draft expiry and the large mode menu, so it does not independently prove
the dragged READY geometry. Own state confirmed two recordings, zero captures/
commits, no draft or cleanup pending, and microphone use ended after 5.111s.
Temporary images were removed. No conversation screenshots or real-app insertion;
the earlier incidental incoming-call frame was deleted.
Sol simplified the manual fixture to one clearly labelled field and made
expired/cancelled reviews collapse to the contextual bubble. Independent source
review passed. The simplified launcher and manual Insert flow are now accepted
for this synthetic case; expiry-collapse visual acceptance remains pending.

Astra Ultra's read-only analysis separated precommit safety from postcommit
observation: Android can mark existing text composing without changing text or
caret. The failed trace rejected composing spans before exact expected-context
and complete target checks. Sol implemented and Astra independently reviewed a
fixture-only diagnostic for that conjunction and bounded later samples, without
changing eligibility, confirmation, or terminal uncertainty. Both fresh runs on
secure980E4299... returned NATURAL_CONFIRMED (5.370s/4.638s): exact text/caret,
one commit each, dismissal, stable result. The failure did not reproduce, so no
composition probe ran and no root-cause fix is claimed. The two-run round is
closed. Copy dismissal/new-keyboard-cycle reappearance also passed in5.853s.
The first simplified-screen manual flow reached READY. Andrew moved the control
but looked for Insert in Android's cursor menu, then pasted the earlier synthetic
Copy sample. Own proof count rose to4; captures/commits stayed2/2, so no KoScribe
Insert or Copy occurred in this flow. The retained COPIED status was from the
earlier automated test. Mic ended after4.639s; the draft subsequently expired.
He clarified that he had not expected the Ko bubble to become a different-looking
result box and requested another try. Root reset only the synthetic fixture and
explained its two rows: EN/Move + Cancel, then Insert + Copy & close. The fresh
attempt passed as recorded above. Whether the earlier Insert row was obscured
remains unmeasured. Any completion-policy change still needs a concrete measured
proposal first.

Earlier stale-node diagnosis: after a commit, the input connection already showed the exact
expected text/caret while the same focused accessibility node retained an old
hint/unknown caret. Refreshing that node made the observations agree. Cache
versus read ordering was not isolated. The fix refreshes the same focused node
after reading the connection and rechecks identity, input generation, focus,
window, and device state before the unchanged exact confirmation. No retry,
guard exemption, or rehabilitation of an uncertain transaction was added.

The earlier fixed scrolling footer resolved clipped controls for the long sample.
The current candidate replaces it with compact Move/Cancel and Insert/Copy rows,
omits the title/result preview, and permits dragging only outside a transaction.
Sol 6 Extra High implemented the changes; Astra Extra High independently reviewed
product and tests. Default/release OFF and recording-only RESELECT remain intact.

## Earlier seven-pass checkpoint (2B00BE44)

All rows below use final product SHA-256 2B00BE44...; exact hashes, methods,
smoke versions, earlier failures, and diagnostic timings are in DEVICE_TESTS.

| Synthetic REVIEW gate | Result |
| --- | --- |
| Edited English -> Copy, original draft preserved, bubble hidden until new IME cycle | PASS 6.708s |
| Edited English -> empty-field insertion, one commit, confirmed dismissal | PASS 4.372s |
| Long English -> middle insertion preserving Korean/emoji prefix and suffix | PASS 4.207s |
| Fixed Korean -> empty-field insertion with confirmation | PASS 4.826s |
| Selected range refused, text preserved, result retained | PASS 4.724s |
| Edited Korean fixed-source rejection -> Cancel, draft cleared | PASS 4.012s |
| Acknowledged but unchanged commit -> terminal uncertainty, no retry | PASS 4.538s |

The unchanged-commit test terminated on WINDOW_OR_FOCUS_CHANGED. It proves
no-retry/uncertainty behavior on that trigger, not the settled-context timeout
branch. Unit tests separately cover unchanged-context rejection. Automated
review edits there used our own EditText accessibility action with SwiftKey visible.
Actual SwiftKey editing and confirmed insertion now passed the separate manual
gate on980E4299... . General composition/prediction acceptance remains unproven.

Candidate/default OFF builds, 37 JVM tests (zero failures/errors/skips), lints
(zero errors; app 23/smoke 3 warnings), and APK signatures passed. Final test-only
Cancel scrolling fix was rebuilt/linted, independently reviewed, and passed.
Its assertions require a cleared draft, unchanged capture/commit counts, and
absence of own review controls even when they are offscreen.
Previous secure diagnostic/default builds passed 37 unit tests and lint, with
25 app warnings and2 smoke warnings, zero errors; signatures verified. Default
and release remained OFF/visual false. The newer approved confirmation candidate
passed40 units (zero failures/errors/skips), lint (zero errors; app25/smoke3
warnings), signatures, and all seven scoped phone gates. Default/release are
restored to OFF/visual false; the installed candidate remains secure REVIEW.

A phone call interrupted one recording before insertion; the unchanged retry
passed after Andrew was finished. Other startup runs lost fixture focus to
SYSTEM windows; their precise cause was not established. Earlier notification
shade interference is documented separately; do not attribute every SYSTEM
interruption to it or Proton Pass.

Andrew reported repeated Proton Pass save prompts. No password was required or
entered. The unused empty password-type fixture is a likely heuristic trigger,
not a confirmed diagnosis. Autofill opt-out alone did not prevent recurrence;
REVIEW tests now omit that field. Protected-field tests retain it by default.
Andrew authorized the prompt's negative action; one exact Android save-button
helper returned NOT_FOUND, so no click occurred. No Proton/global autofill
settings changed. Prompt cessation has not been established.

## Phone and installed artifacts

Galaxy A35 SM-A356E, Android 16/API 36, One UI 8.5; user 0 only. SwiftKey remains
selected; Wispr remains enabled. Default SMS app is Google Messages. Phone is
connected at the latest check. The current speech setup checks finished
with draftNONE and microphone stopped; no test is running. Do not assume a synthetic screen
remains foreground after instrumentation cleanup.
Verify live readiness next session. Fixture cleanup may return
to a real app; never assume the foreground is still synthetic.

Installed MVP product: `app/build/mvp-reuse/TRANSLATION-persistent-ko-v3.apk`,
SHA-256 `EA8D45E03B1B5818BAAE9CC6E887873659AB229470BCE499830D0D744F3D0A39`.
Secure REVIEW, real speech/translation enabled, manual Insert, AUTO_INSERT=false.
Keep live key intact. Saved pre-MVP manual rollback:
`app/build/translation/TRANSLATION-actions-v2.apk`, SHA-256
`EEE3459902AFF0AE7F15B230D661C5E527AF4BA339ECF6507216761BD78FEEA8`.
Saved automatic diagnostic candidate:
`app/build/automatic-insert/AUTO-window-diag-v4.apk`, SHA-256
`79F9B730216D9C99B8EEA4B27E37D7D5C9E32EEDC4BC7A03989FA73CE21E7A3D`.
That candidate is REVIEW/AUTO=true and speech/translation/visual=false; it is
not currently installed and must not be confused with the working speech build.
Saved first-translation baseline: `app/build/translation/TRANSLATION-v1.apk`,
SHA-256 `810D9564F5D169E69E08C5617FE5F14FCE94189AE71280C9252FFC8CEEB4BF18`.
Saved Phase2 baseline: `app/build/speech-input/SPEECH-idle-diagnostics-v5.apk`,
SHA-256 `5579B7FAE962CD93B97B14720C8431565268E04944C4E7B0BDFD9B2F15A62147`.
Previous controls/Cancel baseline remains saved as SPEECH-controls-v4.apk,
SHA-256 `524133623674E42B4BE8E5745C92BAD85A649D1C46AD46EF9181441858461F05`.
Successful ASR/edit/Insert baseline remains saved as SPEECH-secure-v3.apk,
SHA-256 `F2079E8AE1181930911E7820AD4A735A56911F1C122FE06CA76DC594E0F811F9`.
Phase 1 rollback: `app/build/composed-confirmation/REVIEW-secure.apk`,
SHA-256 `3AA18446275E46E3712D5232EC8D02DC4AEF0D3C1D88D7CE7CAECE6080DF99DD`.
Earlier seven-pass product remains saved at app/build/confirmation-refresh/REVIEW.apk
with SHA-256 `2B00BE44077579BBD92D3D6A8D7FCED7E7391E17B05F56B5D1652444F4AD4D9E`.
Installed smoke: `app/build/mvp-reuse/smoke-persistent-ko-v3.apk` (ignored).
SHA-256 `C8D062B538D15CFDFBA7DCA82FE353368735E06A1D321D604B9D6B9FCD77A83B`.
New automatic tests are exact opt-in/API-free; never run the whole suite or any
earlier credential/missing-key methods against the saved live key.
Older smoke/product artifacts remain under their recorded ignored build folders.
Ordinary `app/build/outputs/apk/debug/app-debug.apk` has been rebuilt as OFF with
VISUAL_TEST=false, SPEECH_INPUT=false and TRANSLATION_INPUT=false. Release flags
are also disabled. Do not substitute ordinary output for the saved test artifact.
Prior diagnostic, refresh-only, and earlier smoke APKs are retained in ignored
build folders. Detailed source review hashes are in DEVICE_TESTS.

Last v3 checkpoint: REVIEW_COMPLETED, proof3/captures1/commits1, draftNONE,
CONFIRMED/cleanupfalse; mic ended4.368s. Newly installed controls v4 reset own
counters. Final controls v4 state: RESELECT_CANCELLED, proof1/captures0/commits0,
draftNONE, confirmationNONE, cleanupfalse; microphone ended8.803s, no Stop event.
SwiftKey remains selected. All three real-app scopes are consumed;
fixed unsent drafts are for Andrew to remove manually. Only the static fake
credential marker was saved/deleted during setup. Andrew subsequently saved his
real key privately and confirmed backup; it was not inspected. Real transcription,
review Cancel and edited-text insertion are user-confirmed. No Send/Enter,
conversation inspection, screenshot, or keyboard change occurred. Current-phone
control visibility and active-recording Cancel passed. That was the October24
night checkpoint. Current October25 idle diagnostics/readiness are tracked above.

## Next work and gates

Current work is complete for the selected K installation, bubble reuse/live
restore and bounded Karrot diagnosis. Await Andrew's visual/daily-use feedback.
The next planned product acceptance is Korean meaning/tone in both forms and
latency/cost, under separately prepared owned-editor tests.
Karrot uses Copy & close/manual Paste. Automatic insertion and
the paired-occlusion proposal are deferred; Korean preview remains optional.
No new real-app Insert trial is authorized.

1. Preserve both manual passes. The shorter instruction is conditional on the
   intended field/caret already being restored. Current-build preservation,
   selection/refusal, uncertainty and geometry coverage remains bounded by the
   recorded tests; do not generalize this empty-field pass to those cases.
   The old composition diagnostic round is complete. The approved fix passed its
   scoped checks; do not retry uncertain commits or reopen that probe round.
2. Preserve all three app passes; their scopes are consumed. Phase 2 code is
   reviewed, built and unit/lint checked. Credential fake test passed; missing-key
   safety assertions passed, with full-method Close cleanup failures recorded.
   Key entry and backup are user-confirmed. Synthetic real speech/review/Cancel
   and speech/edit/Insert passed. The clipped Stop fix and manual active-recording
   Cancel also passed on controls v4. Do not repeat those gates without new evidence.
   No API keys in chat or shell; no unattended audio upload, fake-key/deletion
   or missing-key reruns. Offline failure and fresh online recovery passed on v5;
   the bounded Phase2 gate is accepted. Phase3 confirmed-English translation is
   now implemented/reviewed and entering owned-editor testing. Keep the key intact. Broader
   reliability remains a later phase; the original morning readiness cause is open.
3. For broader daily-use reliability, extend selected/caret/navigation and
   composition coverage, small-height/large-font/rotation/split-screen, and
   lock/service recovery. These remain limits; do not silently turn the full
   matrix into new prerequisites for beginning speech after the app round.
4. Assess direct insertion's benefit against the accepted launcher -> mic ->
   editable English -> 요/반말 -> Copy & close -> manual paste fallback. Do not
   spend indefinitely on integration without measured user benefit.
5. Phase3 needs user-reviewed meaning/tone in both Korean forms and measured
   latency/cost. Model remains gpt-6-luna/xhigh; first translation verified account
   access and yielded one timing/usage sample. Key/DeveloperStuff backup are confirmed. Preserve that
   credential. Improve recording-state prominence in the later UI phase.

Earlier RESELECT synthetic gates proved keyboard-based visibility, dragging,
Hide suppression, and saved position after service restart. Earlier Stage 1
proved >3-second unbound recording -> Stop -> readonly review -> Cancel in Kakao
with zero destination captures. Those results apply to their recorded builds;
they are not full current-build real-app acceptance. See DEVICE_TESTS for history.

## Durable choices

- Bubble is the main interface; three visible modes 요 / 반말 / EN.
- Editable English review for every message initially; skip option later.
- Record/review without a destination. Prepare result, then Andrew checks the
  intended field/caret, taps it if needed, and taps Insert; only Insert starts
  a fresh guarded capture.
- Preserve existing text; one commit maximum; ambiguous attempt stays terminal.
  Keep SwiftKey; no replacement keyboard or automatic Send/Enter.
- Close only the result panel after Copy or confirmed insertion; keep the small
  K bubble available while SwiftKey is open. Explicit Hide remains.
- Personal on-phone key entry with Keystore-backed encryption, no key in Git/APK/
  logs. Translation choice gpt-6-luna/xhigh; OpenAI transcription baseline recorded
  in RESEARCH. First translation access and one edited 요 case passed; broader
  quality/latency/cost remain open. Scoped speech checks passed.
- Floating icon is the selected Spoken K with translucent light gray body.
  Broader UI, permissions/battery/reboot UX, signing and distribution remain later.

## Repository and tools

Private repository: https://github.com/drew345/KoScribe . main tracks origin/main.
Reviewed confirmation change: 56a3d39; Kakao pass: 3fb62d4; two-app scope: be6eb94.
Google Messages pass/Instagram handoff is recorded afterward; use git log for its ID.
Inspect staged files and scan for secrets before publishing.

ADB/SDK: C:\AI\Codex\Tools\android-sdk (API 36/build tools 36.0.0).
JDK: C:\AI\Codex\Tools\openjdk21\jdk-21.0.12.1+1.
Gradle wrapper 8.13; cache C:\AI\Codex\Tools\gradle-cache.
Use adb -d, user 0, and the non-suppressing KoScribeTestRunner. Root alone
operates the phone; run exact opt-in methods separately and inspect every result.
