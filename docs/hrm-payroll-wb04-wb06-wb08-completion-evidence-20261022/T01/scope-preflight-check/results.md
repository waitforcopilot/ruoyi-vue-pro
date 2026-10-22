# Scope preflight candidate verification results

Requested executor model: `gpt-6-luna / max`. Actual runtime model: unverified; no runtime evidence establishes it.

## Frozen candidate identity

The four frozen backend source/test hashes matched before testing and still match after packaging:

- `HrmPayrollRequirementServiceImpl.java`: `298f1f6e91e45e7cd78e5306c1b50a0911de3bdb022d2f10677ecbab63c628ab`
- `HrmPayrollRequirementServiceImplTest.java`: `f9ff1ab8eed7e50504f56c1aac3bf83015a8899c83361ba16053e4ed2c37d25f`
- `HrmPayrollWorkbenchService.java`: `4006b751f6c87d938962852bed7b6a08af757318ec718d870061aba921b9efe6`
- `HrmPayrollWorkbenchServiceTest.java`: `db440327168da05bb582b2c1b918cdec7b3e0c7de67da62f195beebf58eaede9`

## Targeted tests

Ran the exact JDK 8 Maven command recorded in `commands.md`. Exit code: `0` (`maven-targeted-tests.exitcode`). Surefire reports 86 tests: Workbench 31, Review 19, Requirement 36; 0 failures, errors, or skips. Maven reported `BUILD SUCCESS`. Full output is in `maven-targeted-tests.log`.

## Server package

After the passing tests, ran the exact JDK 8 package command recorded in `commands.md`. Exit code: `0` (`maven-server-package.exitcode`); Maven reported `BUILD SUCCESS`. Full output is in `maven-server-package.log`. The produced `yudao-server/target/yudao-server.jar` SHA-256 is `044a0625b5410cb8be5b086625ba60554b49f789402b5ff5423f46567cd850e5`.

This execution did not restart the backend or exercise the packaged JAR through the browser. The newly packaged artifact must not be described as the currently running server until the main thread performs its planned restart and checks it.

## Browser-loaded frontend source identity

Read-only parsing of the main-thread browser Network response artifacts confirms both captured module responses had HTTP 200. Their raw bodies hash to the `responseSha256` values recorded in `T01/browser-resource-binding.json`. Each body's inline source map contains the expected source, and its `sourcesContent` bytes exactly equal the corresponding current disk file:

- `index.vue`: request `85833.18861`; response SHA-256 `03fa4637c0fe6364ce7b6e083278158ce3b341bddf1a573b19cfa9eaeb840764`; source content and disk SHA-256 `a5817f4eff90fe921238e04b4d6ca8d993b3a51b989e090923fb3cd302479bce`.
- `validation.ts`: request `85833.18865`; response SHA-256 `856b10cc100d99888521a6de0940bf0fca18a6a7c6c551d458a36253543dd77a`; source content and disk SHA-256 `82903174d28f1743aca049b787dd6099da63aa5647e95ed5e1d32f2076b87471`.

The machine-readable comparisons are in `browser-resource-binding-verification.json`. The parent-provided metadata has `truncated=true` and `hasMore=false`; the two response-body artifacts themselves are present and their body hashes and source-map contents verified. This evidence supersedes the earlier `closure-review.md` statement that browser-loaded modules had not been bound to source hashes.

## Remaining validation boundary

No browser, UI, backend, or database operation was performed in this executor turn. The scope-mismatch confirmation UI recheck remains for the main thread after its planned backend restart. These automated test and package results do not close the browser regression by themselves.
