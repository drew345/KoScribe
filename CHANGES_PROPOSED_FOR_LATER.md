# Changes proposed for later

Updated: 2026-10-29 (Asia/Seoul).

Andrew is trying the current MVP and will report anything needing an immediate
fix. This is the standing list of later improvements and unfinished acceptance
work. Recording an idea here does not start its implementation. Current installed
build and measured results remain in [PROGRESS.md](PROGRESS.md).

## Deferred changes

1. **Approve English, then translate and insert automatically.** Deferred by
   Andrew while establishing the MVP. Preserve the manual Insert and Copy & close
   baseline. Resume only with a way to verify the original destination, caret,
   surrounding text and continuity after review; otherwise offer manual Insert.
   Existing experimental code is disabled. The proposed handling of the original
   window disappearing/reappearing during review remains unimplemented and has
   no automatic-insertion pass. See the [Phase 3 proposal](docs/PHASE3.md#stopped-october25-unimplemented-paired-occlusion-proposal).

2. **Make recording and processing states more obvious.** Andrew initially did
   not realize tapping a language choice immediately started recording. Improve
   the prominent recording label, timer or visual indicator; consider optional
   start/stop vibration or sound. The choice of feedback is still open. Stop and
   Cancel visibility was already fixed and phone-tested; broader menu polish
   remains later work.

3. **Optional Korean preview before insertion.** Andrew considered seeing the
   Korean result useful, but also wants fewer steps. Explore showing it within
   the existing result control, with Insert and Copy & close, without requiring
   another confirmation screen. This is an option to evaluate, not a settled
   requirement or a replacement for the automatic-flow proposal.

4. **An option to skip English review.** Initially every message keeps editable
   English review, as agreed. Later offer an explicit preference for skipping
   review when transcription is trusted. Decide its behavior and safeguards
   separately from automatic insertion after approval.

5. **Background readiness and recovery with minimal notification clutter.**
   Investigate idle survival, Samsung battery/sleep settings, service reconnection
   after updates/reboot/unlock, and an understandable status/repair screen.
   Andrew prefers KoScribe's current lack of persistent alerts. Compare the
   existing accessibility-overlay architecture with alternatives only when
   measured reliability or interaction benefits justify them. Keep the Wispr
   screenshot below as a reference; persistent notifications are not a selected
   KoScribe feature. Wispr's public setup guide attributes its ongoing notice
   to background operation, not to the insertion method (see docs/RESEARCH.md).

6. **Better compatibility with unsupported editors, including Karrot and Instagram comments.** The
   current metadata probe found that Karrot does not expose the focused/editable
   field information guarded Insert requires. Keep Copy & close followed by
   normal Paste as the MVP route. Any future integration needs preservation and
   destination evidence; a blinking cursor alone is not sufficient reason to
   remove the safeguards.

   On 2026-10-29 Andrew tried Insert into an empty Karrot chat and then tapped
   the field and tried again. Both were refused; KoScribe's own counters show no
   commit. Same-node metadata remained focused=0, editable=0 and input type zero
   even after a refresh. An independently safe alternative needs investigation.

   On 2026-10-28 Andrew reported that KoScribe would not paste text into
   Instagram comments, while Instagram DMs work. On October 29 he clarified
   that the floating Insert first refused destination verification, then worked
   after he tapped the comment field again. The cause remains unverified. Preserve
   this successful reselect behavior while investigating reliability.

   Wispr's public Android documentation says its own insertion can fail and
   then falls back to clipboard. Compare Wispr's actual behavior in Karrot with
   an unsent draft before assuming it has a general solution for this editor.

7. **Broader layout and interruption resilience.** Test large fonts, small usable
   screen heights, rotation/split-screen, keyboard and app switching, calls,
   lock/unlock, draft expiry and service restarts. Improve compact controls and
   recovery where these tests expose problems. Existing tests cover bounded
   scenarios, not all daily-use combinations.

8. **A complete launcher-based fallback.** Keep the accepted direction available:
   home-screen app → microphone → editable English → choose 요/반말 → translate →
   Copy & close → normal Paste. The bubble already offers Copy & close; this
   complete launcher workflow has not been built.

9. **Personal installation and settings polish.** Move beyond the “Phase 1”
   labels/debug-only feature switches to a deliberately configured personal
   build, understandable setup/status controls, and a signed update process.
   Protect the existing key and signing material; keep secrets outside Git and
   the APK, with confirmed DeveloperStuff backup for irreplaceable credentials.

10. **Custom vocabulary and correction rules for dictation.** Requested by
    Andrew after English transcription missed Korean app/brand and coffee-shop
    names during MVP use. First examples, preserved exactly as he supplied them:
    **Dangun pay** and **Twosome cafe**. Later evaluate a personal vocabulary list,
    pronunciation aliases and explicit corrections, with corrected English still
    available for review before translation. Preferred English/Korean spellings
    and the actual misrecognized forms have not been specified; confirm those
    when implementing rather than inventing replacement mappings. Deferred;
    no transcription or replacement behavior has changed.

## Remaining acceptance work

- **Korean meaning and tone in both forms:** requests, apologies, questions,
  negation, names, numbers, scheduling and longer messages. One real edited 요
  translation passed; broader quality and 반말 acceptance remain open.
- **Latency and cost:** measure transcription and translation separately before
  proposing a different model or reasoning setting. The agreed translation
  model stays in place until a change is discussed.
- **Insertion preservation and recovery:** broaden current-build coverage for
  existing Korean/English/emoji, middle insertion, selected text and composition,
  alongside repeated use. Preserve the one-attempt rule for ambiguous commits,
  Microsoft SwiftKey and manual Send. Existing real-app trial scopes are consumed.

The selected light gray, translucent Spoken K and persistent small bubble after
completion are implemented and phone-tested. They are not pending design items.

## Wispr notification reference

Andrew supplied this image on 2026-10-26 and reports that these notices remain or
return after Clear. The original JPEG is preserved here:

![Wispr Flow overlay and dictation-ready notifications](docs/references/wispr-flow-notifications-2026-10-26.jpg)

- **Upper card:** the visible title is truncated to “Wispr Flow is displ…”. It
  appears to be Android's notice that an app is displaying over other apps.
  The full title is not visible. Wispr's own troubleshooting documentation
  discusses Android's overlay notification. This is a system visibility notice,
  rather than evidence of an insertion failure. [Wispr troubleshooting](https://docs.wisprflow.ai/articles/5002934560-Troubleshooting-the-Flow-Bar-%28Desktop%29-and-Flow-Bubble-%28Android%29)
- **Lower card:** Wispr Flow says “Voice dictation is ready”. Its setup guide
  explicitly associates this ongoing notification with background operation.
  The screenshot alone does not establish active microphone recording or the
  exact mechanism that restores a cleared notice. [Wispr Android setup](https://docs.wisprflow.ai/articles/8858845757-setup-wispr-flow-on-android-android-settings)

**Technical interpretation, checked 2026-10-26:** Android foreground services
have an associated status notification. KoScribe currently uses an accessibility
service and accessibility overlay; its manifest has no app-owned foreground
service or notification permission. These are different architecture choices.
The notification difference is a useful research lead for background readiness.
It does not establish why Wispr can insert automatically or show that adding a
notification would solve KoScribe's destination-continuity issue after review.
[Android foreground services](https://developer.android.com/develop/background-work/services/fgs),
[current manifest](app/src/main/AndroidManifest.xml),
[review-return evidence](docs/DEVICE_TESTS.md).

Preserve Andrew's preference for quiet operation when evaluating a future design.
Any required system notification belongs to the chosen architecture and its
Android requirements; adding an alert alone grants no additional editor access.
