# Executor targeted checks — 2026-10-23

Role requested by the handoff: executor, `gpt-6-luna / max`. Actual runtime model: unverified. Repository HEAD: `d5ff56dbb5b94fdcf069c90f064f332d31f6a6fb`. The candidate source recorded by the coder is `yudao-ui/yudao-ui-admin-vue3/src/views/hrm/payroll/review/demo.ts`, 17,896 bytes, SHA-256 `c55842dda33aca22636b2715626932466fa751e9744979508a0050f87cd365c8`.

No product, test, report, or sealed historical file was edited. No build, Maven run, full UI build, database operation, browser operation, stage, commit, or push was performed. The new files in this directory preserve executor commands and results.

## Results

| Check | Result | Evidence |
| --- | --- | --- |
| Prettier via `pnpm exec` | Exit 1 before the formatter started. pnpm attempted dependency-directory maintenance and aborted because the process had no TTY. | `prettier.*` |
| ESLint via `pnpm exec` | Exit 1 before ESLint started, for the same pnpm no-TTY dependency-directory guard. | `eslint.*` |
| Project-local Prettier | Exit 1: `demo.ts` needs formatting. The read-only formatter diff shows one change: Prettier expands the RPT scenario array item over multiple lines. No `--write` was used. | `prettier-direct.*`, `prettier-readonly-diff.*` |
| Project-local ESLint | Exit 0, stdout/stderr empty. | `eslint-direct.*` |
| TypeScript parser/module transpile | Exit 0; syntax parse and ES module transpile pass. Type checking was not run. | `typescript-module-parse.*` |
| Candidate and reference integrity | Exit 0; coder identity 3/3, historical source identities 18/18, HEAD matches, 34 local links checked across the active completion report and correction index, 0 broken. | `identity-reference.*` |
| Candidate text credential-pattern scan | Exit 0; 11 text files scanned, 0 files with secret-like candidates, 0 candidate matches. Values are never printed. Pattern scan only. | `credential-count-scan.*` |

The local Prettier failure is a handoff to coder for formatting only. It is not a product behavior failure. This executor did not alter the frozen candidate. The later candidate must be checked under a new identity record; this round's identity remains intact.

Every command has its exact invocation, stdout, stderr, and exit code in the same-named `.command.txt`, `.stdout.txt`, `.stderr.txt`, and `.exitcode` files. Browser-original hashes are recorded separately in `browser-hashes.stdout.txt`.
