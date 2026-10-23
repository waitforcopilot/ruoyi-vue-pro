# T06 job 1429 workbook reconciliation

Date: 2026-10-23 (Asia/Shanghai). This is an independent read-only content reconciliation of the workbook downloaded for TEST release 1384 / export job 1429 in project 105. It is not a full T06 acceptance or release report.

## Candidate identity and method

The local download and the preserved evidence copy are both 352,138 bytes and have SHA-256 `6cdab6245a67bac6672b6dd837243a6b63d4312593c7d3cb2d924c34ab3f723e`, matching `browser-download1429-identity.json`. The browser evidence names TEST project 105, existing release 1384, and job 1429. The browser's final saved state still shows the job as READY after the claim action; the local file's identity establishes that a matching workbook exists on disk.

I loaded the existing `verify_workbook.py` module and called its `load()` function only on `release1384-job1429-browser.xlsx`. I did not call the old `main()` entry point because it overwrites sealed historical output. The loaded workbook was reconstructed and compared with every business record and field in the sealed `wb08-release1384-visible-six-tables.json` projection. The generated `EXPORT-METADATA` parent and its generated detail rows were excluded from the business comparison; no other rows or fields were excluded. The machine-readable result is `reconciliation-20261023.json`.

As a second check, I used the same existing loader to reconstruct both job 1429 and the prior job 1386 workbook for release 1384, then compared every reconstructed business object. All six sheets are exactly equal after excluding only the `EXPORT-METADATA` parent, including its run-specific timestamp. See `job1429-reexport-comparison-20261023.json`. A trial of the separate `scripts/hrm/verify_governance_workbook.py` helper was not suitable for this exporter: its CLI filter removes only the metadata summary row (not generated detail rows), and its verifier expects one row per projected object rather than the export's multirow text-fragment representation. That trial stopped with an assertion and wrote no output; neither workbook or sealed artifact was modified.

## Result

Content reconciliation: **PASS**. The six sheets contain 126 business records and all 1,130 compared fields match the sealed projection. There are no missing records, extra business records, or mismatched fields. Independent re-export comparison against job 1386 also passes on every reconstructed business object.

| Sheet | Expected business records | Actual business records | Difference |
|---|---:|---:|---:|
| 评审版本说明 | 30 | 30 | 0 |
| 功能需求清单 | 17 | 17 | 0 |
| 数据源清单 | 6 | 6 | 0 |
| 字段映射 | 3 | 3 | 0 |
| 问题与待办 | 53 | 53 | 0 |
| 范围与变更 | 17 | 17 | 0 |

The loader also checked the six sheet names and order, the exact eight-column header, absence of formulas, record and field-fragment structure, and row-height bounds. The sheets contain 718, 912, 787, 850, 276, and 3,383 used rows including headers, respectively; their maximum row height is 102 points.

## Visual evidence and remaining boundaries

Saved browser and native Excel evidence was reviewed. The native evidence includes a current image and accessibility capture for each of the six sheets at 100% zoom, with the top visible portion, eight columns, headers, wrapped Chinese text, synthetic examples, NA explanations, mapping, and scope/change context visible. These captures do not establish that every row was visually inspected. The saved native workbook was not saved after inspection, and the downloaded file's SHA-256 remains unchanged.

The following T06 boundaries remain open: no full-sheet visual scroll, print-preview inspection, long-emoji sample, very long rationale sample, or restricted-excerpt sample was captured. The workbook belongs to project 105's pre-existing historical TEST release 1384, not current project 153. Content equality therefore closes only the full-field reconstruction check for this exact candidate; it does not close T06 or establish project 153 acceptance.

No product code or test source was changed for this reconciliation. No browser action or business-database write was performed. The requested executor settings were gpt-6-luna / max as verified by the parent; the actual runtime model was not independently verified.
