# Scope preflight candidate verification

Requested executor model: `gpt-6-luna / max`; actual runtime model: unverified.

Pre-test SHA-256 values matched the frozen handoff:
- `HrmPayrollRequirementServiceImpl.java`: `298f1f6e91e45e7cd78e5306c1b50a0911de3bdb022d2f10677ecbab63c628ab`
- `HrmPayrollRequirementServiceImplTest.java`: `f9ff1ab8eed7e50504f56c1aac3bf83015a8899c83361ba16053e4ed2c37d25f`
- `HrmPayrollWorkbenchService.java`: `4006b751f6c87d938962852bed7b6a08af757318ec718d870061aba921b9efe6`
- `HrmPayrollWorkbenchServiceTest.java`: `db440327168da05bb582b2c1b918cdec7b3e0c7de67da62f195beebf58eaede9`

Targeted test command, from repository root:
`JAVA_HOME=/Users/waitforpro/.local/jdks/java8/Contents/Home /opt/homebrew/bin/mvn -B -ntp -pl yudao-module-hrm -am -Dtest=HrmPayrollWorkbenchServiceTest,HrmPayrollReviewServiceTest,HrmPayrollRequirementServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`

Only if targeted tests pass, run package command from repository root:
`JAVA_HOME=/Users/waitforpro/.local/jdks/java8/Contents/Home /opt/homebrew/bin/mvn -B -ntp -pl yudao-server -am -DskipTests package`
