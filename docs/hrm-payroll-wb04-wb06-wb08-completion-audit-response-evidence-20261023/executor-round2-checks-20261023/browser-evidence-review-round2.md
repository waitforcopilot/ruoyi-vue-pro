# Final round-2 browser evidence review — 2026-10-23

The browser operator was the main thread; this executor only reviewed saved AX/PNG originals. Candidate `demo.ts` identity remains 17,920 bytes, SHA-256 `2d1feed5eda56fcb8e33fc56c694db26285a4a5a22221d0869cc7e1259540c43`. Requested executor model: `gpt-6-luna / max`; actual runtime model: unverified.

## Result

**Round 2 PASS for the requested visible copy and project context.** The final screenshots and AX expose both corrected topic lines in their focused cards, and the stable pages show project 153 with scope version 5. This result covers the text and page-context regression only.

| Evidence | Review |
| --- | --- |
| `browser/006-final-col-format-candidate.txt` and `.png` | First COL load. URL includes project 153, but AX says scope is undefined or unavailable under current permission and data is still loading. Retained as the initial-load artifact; not used as stable project evidence. |
| `browser/007-final-col-stable-project153.txt` and `.png` | PASS. AX lines 51 and 138–142 show the TEST project 153, scope version 5, and full COL-03 text; line 316 identifies COL-03 as the focused element. PNG shows the same text and visible TEST requirement rows. |
| `browser/008-final-rpt-stable-project153.txt` and `.png` | PASS for RPT-05 text and project context. AX lines 51 and 177–180 show project 153, scope version 5, and the full corrected RPT-05 text; line 237 identifies RPT-05 as focused. PNG shows the same text and the selected-filter empty-state explanation. |

One capture boundary remains: 008 AX also includes the live status “正在读取真实评审数据，未返回内容不推定为空或通过” (line 182), while its PNG shows the selected-filter empty-state message. Treat the two artifacts as evidence of visible copy, focus, and project scope; do not use them to assert that the RPT review-data request had settled or that the project has no requirements. This does not change the targeted-copy PASS.

Across 007/008, the AX reports the same displayed `TEST S5 editor NEW DISPLAY` account and TEST scope (`entities:[101]`, `organizations:[201]`, `workerTypes:["TEST"]`, `scenarios:["TEST"]`, 2026-10-01 through 2026-10-31). The candidate delta only changes two strings; this round did not audit backend RBAC grants. The main thread reports no business writes; this executor did not operate the browser or database.

The six source-file hashes for 006–008 are in `browser-final-hashes.stdout.txt`, with the exact command, stderr, and exit code alongside it. Earlier governance navigation evidence remains reviewed in `browser-evidence-review-20261023.md`.
