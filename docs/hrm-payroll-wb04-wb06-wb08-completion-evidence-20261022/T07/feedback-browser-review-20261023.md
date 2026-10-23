# T07 feedback browser retest review

Date: 2026-10-23 (Asia/Shanghai). Reviewer: executor. The main thread operated the visible local browser; this review uses only the handed-off AX text and PNG originals for `T07/browser/011–015`. No browser or API was operated by this reviewer. Original file hashes are recorded in `feedback-browser-review-20261023.sha256`.

Automation and SFC checks completed before this evidence review; see `feedback-automation-1/result-summary.md` and its raw command/output files. The fixed SFC and test-candidate hashes still match the coder handoff.

## Evidence review

| Capture | Observed in the saved original | Review result and limit |
|---|---|---|
| 011 | AX identifies project 153, scope v5, decision 1433 as DRAFT (Q-01/Q-03/Q-06/Q-07/Q-08/Q-09/Q-10), 1426 as PENDING_APPROVAL, and 1407 as EFFECTIVE. PNG shows the corresponding DRAFT, pending, and effective lifecycle rows. | Supports the complementary TEST decision draft and preservation of the prior active row. The screenshot horizontally clips some identity columns; AX supplies the record IDs and title. |
| 012 | PNG shows an additional pending row with the complementary Q coverage; the row identity is partly clipped. AX instead reports no independent decision read, an empty decisions table, and a disabled refresh control. | The AX/PNG pair is inconsistent, likely captured during a permission/loading transition. Do not use it as a matched identity-bound submission capture. The PNG alone suggests pending submission, but 013 is the stronger evidence of the final saved state. |
| 013 | AX reports the approver-session action was saved and lists 1433 v2 as EFFECTIVE, 1407 as EFFECTIVE, and 1426 still PENDING_APPROVAL. PNG shows the same lifecycle rows and complementary coverage; the page is in the approver workspace. | Supports 1433's effective status without replacing 1407 or approving 1426. The main thread attributes the independent approval to user 267; this frame shows the approver context and assigned approver ID but does not display a separate event-history actor row. |
| 014 | AX and PNG both show an open action dialog for decision 1426, APPROVE, version 2, with the TEST rejection rationale retained in the input. | Confirms the rationale is present in the open dialog. The frame itself is before a visible result and does not independently prove the rejection. |
| 015 | AX reports `本人动作被拒绝或版本已变；依据输入已保留，不视为批准成功。` and lists 1407 and 1433 EFFECTIVE while 1426 remains PENDING_APPROVAL. PNG shows those lifecycle rows and no success banner in the visible viewport. | Supports the rejected 1426 attempt and absence of stale success in the AX content. The PNG is scrolled below the top of the page, so the exact refusal text is supported by AX; the screenshot alone cannot exclude offscreen messages. |

Together, 013–015 support the feedback regression sequence reported by the main thread: a separate complementary TEST decision became effective, the overlapping approval attempt for 1426 was rejected, and after the dialog was dismissed the page reported only the refusal instead of retaining an earlier success message. Decision 1426 remains pending; its rejection must not be presented as an approval. The release-snapshot table is empty and publish controls are disabled in these captures, so this does not establish a baseline release or full T07 closure. All actions are TEST engineering evidence only.

The browser PNG/AX originals were not changed. This is an internal evidence review, not the final fix report.
