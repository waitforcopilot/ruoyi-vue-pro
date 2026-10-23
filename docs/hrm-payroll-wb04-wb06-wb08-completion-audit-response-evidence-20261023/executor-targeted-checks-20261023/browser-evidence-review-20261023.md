# Browser evidence review — 2026-10-23

Browser operator: main thread. This executor only read the saved AX and PNG originals. Requested executor model: `gpt-6-luna / max`; actual runtime model: unverified. Scope is the two corrected topic lines and their governance-page navigation.

## Findings

| Evidence | Review |
| --- | --- |
| `browser/001-col-topic-corrected.txt` and `.png` | PASS for visible COL-03 copy. AX lines 134–137 and the screenshot show “独立治理页提供TEST受控发布、固定快照及异步导出，操作成功不等于正式业务批准”. The page is `projectId=153`; the visible scope is version 5 and lists TEST-only entities/organization/date range. The focused element is COL-03. |
| `browser/002-rpt-topic-corrected.txt` and `.png` | Retained as the first-load record. It shows the RPT-05 revised wording, but AX says real review data is still loading and the project scope is not yet defined or may be hidden by current permission. It does not establish an empty project or stable data state. |
| `browser/003-rpt-stable-project153.txt` and `.png` | PASS for stable RPT-05 copy and project context. AX lines 175–178 and the screenshot show “当前筛选清单与发布时固定的六表快照分别查看”. URL retains `projectId=153`; the same TEST project and scope version 5 are visible. The selected filter returns no visible requirements, with the page explicitly saying that this does not mean the whole project has no requirements. |
| `browser/004-rpt-to-governance-project153.txt` and `.png` | PASS for RPT-to-governance navigation context. URL is `/hrm-payroll/governance?projectId=153`; the same TEST project is selected with scope version 5. The page identifies TEST material and synthetic signatures as non-formal approval. |
| `browser/005-col-to-governance-project153.txt` and `.png` | PASS for COL-to-governance navigation context. URL is `/hrm-payroll/governance?projectId=153`; the same TEST project is selected with scope version 5. The screenshot shows the same displayed TEST S5 editor account as the preceding pages. |
| `browser/console-warnings-errors.json` | Six warnings, zero errors. The warnings are Vue Router `next()` deprecation messages. This is limited to the saved console record and does not establish zero warnings across the product. |

The two requested copy changes appear in the visible pages, and both navigation paths retain project 153 and its TEST scope. The candidate source diff contains only the two topic-string edits; it does not change navigation, permissions, interfaces, or scope logic. The saved pages show the same TEST account and project context, with no visible permission expansion. These screenshots do not independently enumerate effective RBAC grants, so they are not a backend authorization audit.

`002` remains an unstable first-load capture. `003` is the stable RPT view used for the page result; its empty selected-filter result is not treated as proof that the project has no requirements. The page copy and TEST screenshots establish interface behavior for this candidate only, not formal business approval or production acceptance.

SHA-256 values for all five PNGs, their five AX text files, and the console JSON are stored in `browser-hashes.stdout.txt`. The hash command, stdout, stderr, and exit code are stored alongside it.
