# T02 independent contract test — run 1

Role: executor. Requested model: `gpt-6-luna / max`; actual runtime model: unverified.

Repository instructions read: `AGENTS.md` and `docs/role-model-policy.md`. They require preserving existing changes, recording HEAD and diff identity, keeping requested/actual model distinction, and using TEST-only evidence. No product source was edited by this executor.

Candidate identity recorded immediately before execution:
- HEAD: `head.before.txt`
- Full tracked working-tree patch snapshot: `tracked-working-tree.before.diff`; SHA-256 in `tracked-diff-sha.before.txt`
- Complete working-tree status: `git-status.before.txt`
- Added test source SHA-256: `test-sha.before.txt`

The tracked working-tree snapshot includes existing modifications in `HrmPayrollGovernanceExportService.java` and `HrmPayrollGovernanceServiceTest.java`. These are preserved and were not changed in this executor turn. The new test itself was untracked before this test run. No package, server restart, browser, UI, or business database action is included.

Command:
`JAVA_HOME=/Users/waitforpro/.local/jdks/java8/Contents/Home /opt/homebrew/bin/mvn -B -ntp -pl yudao-module-hrm -am -Dtest=HrmPayrollT02ConfirmationContractTest -Dsurefire.failIfNoSpecifiedTests=false test`
