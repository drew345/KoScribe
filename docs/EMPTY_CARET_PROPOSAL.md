# Approved empty-editor caret rule

The original Galaxy A35 empty local-editor attempt returned `OBS_NODE_CARET_MISMATCH`. A code-only probe reported `CARET_EMPTY_PROBE_CY_NY_HY`: the input connection is empty at zero, the node reports the `-1/-1` pair, and the node is empty or showing a hint. An independent AOSP review found that an empty `TextView` can expose this pair through accessibility. Andrew approved the exact narrow rule on 2026-10-23. It is implemented and boundary-tested; subsequent synthetic empty-field insertion passed on the A35. See `DEVICE_TESTS.md` for current results. KakaoTalk compatibility remains unverified.

Proposed rule inside `observeEditor`, after the existing text-type, password, surrounding-text, known-offset, and collapsed-selection checks:

```kotlin
val nodeCaretMatches = focus.textSelectionStart == caret &&
    focus.textSelectionEnd == caret
val verifiedEmptyCaret = focus.textSelectionStart == -1 &&
    focus.textSelectionEnd == -1 &&
    surrounding.offset == 0 &&
    surrounding.text.isEmpty() &&
    surrounding.selectionStart == 0 &&
    surrounding.selectionEnd == 0 &&
    (focus.isShowingHintText || focus.text.isNullOrEmpty())
if (!nodeCaretMatches && !verifiedEmptyCaret) return refuse(NODE_CARET_MISMATCH)
```

Put the comparison in a small pure helper and use it at both capture and pre-commit observation. Keep every other guard: same node/window/editor, unchanged surrounding context and selection, no known active composition, return continuity, session ownership, and single reserved `commitText` attempt. This rule does not accept a nonempty editor whose accessibility selection is unknown, mixed `-1/0` endpoints, a nonzero offset, or an uncollapsed input-connection selection. It does not infer that composition is clear.

Boundary tests for the helper and session guard:

- Accept an empty hinted editor with node `-1/-1` and input connection `offset 0`, empty text, `0/0` selection.
- Accept the same empty editor without a hint when the node text is empty.
- Accept ordinary known matching node and input-connection caret positions.
- Refuse `-1/0`, `0/-1`, nonempty surrounding text, nonzero offset, or a nonempty node without hint evidence.
- Refuse an input-connection range selection even when the accessibility node says `-1/-1`.
- Capture an empty target, then refuse pre-commit if its text, caret, node identity, window, input generation, or session ownership changes.
- Capture an empty target, cancel or supersede it, and refuse insertion even if the empty editor is still focused.

This changes the safety policy for one empty-field case. Test it first in KoScribe's synthetic editor on the A35, including preexisting text and cancellation, before considering other apps. The earlier automatic approval rejection applied to the diagnostic-only instruction; Andrew's subsequent explicit approval authorized this particular rule. No Phase 2 work or broad unknown-selection exemption is authorized.
