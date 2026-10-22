# T03 raw期限前端候选检查结果

- Frozen hashes were checked before running; all matched the supplied candidate identity. Post-check SHA values are in `candidate-hashes-after.txt` and remain unchanged.
- New raw-deadline regression: exit 0, 36/36 PASS. This is SFC/template/v-model/save integration without DOM mount, as documented in the coder handoff.
- Existing confirmation regression: exit 0, 34/34 PASS.
- Prettier check for both target files: exit 1. It flags `WorkbenchPanel.vue` formatting; no `--write` was run.
- ESLint was not run because the handoff requires stopping on the first failed check.
- No source changes were made by the executor. Requested model `gpt-6-luna / max`; actual runtime model unverified.
