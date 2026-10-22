# T02 related regression — run 2

Requested model: `gpt-6-luna / max`; actual runtime model: unverified. HEAD and full tracked diff snapshot are recorded in `head.before-joint.txt` and `tracked-working-tree.before-joint.diff` with its SHA in `tracked-diff-sha.before-joint.txt`. Test and existing related source hashes are recorded in `source-shas.before-joint.txt`.

Run only after the new T02 contract class passed. Exact command:
`JAVA_HOME=/Users/waitforpro/.local/jdks/java8/Contents/Home /opt/homebrew/bin/mvn -B -ntp -pl yudao-module-hrm -am -Dtest=HrmPayrollT02ConfirmationContractTest,HrmPayrollRequirementServiceImplTest,HrmPayrollRequirementProjectServiceTest,HrmPayrollWorkbenchServiceTest,HrmPayrollReviewServiceTest,HrmPayrollGovernanceServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`

No package, server restart, browser, UI, or business database action is included. Existing T02 tracked modifications are preserved.
