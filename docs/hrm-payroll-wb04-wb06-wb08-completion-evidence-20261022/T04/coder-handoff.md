# T04静态候选交接

日期：2026-10-23（Asia/Shanghai）。证据继续保存在既有20261022目录，不重命名历史原件。coder期望/请求gpt-6.1-sol / medium，实际模型未核实；未替换模型。本文件是内部交接，不是最终fix报告。

复查已存在的完整候选后保留主体，仅补强文档说明及失败发布的无额外记录断言。候选：`yudao-module-hrm/src/test/java/cn/iocoder/yudao/module/hrm/service/payroll/HrmPayrollT04IssueConsistencyTest.java`，SHA256 `52e26c3d4266065875552264dc035504f3aeb09879fcae439237f0fcc0160fc7`。

本轮没有产品修改，也没有修改T03测试、既有治理测试或最终fix报告。未编译/测试、打包、DB、浏览器、重启或提交。T03测试SHA仍为`dccdbc3e6ce4cc124afab8e91c152e8114986959a252d383547fc447e0e04ef2`。文本静态检查没有尾随空白；这不代表Java编译或测试通过。

## 23个计划执行实例

| 分组 | 例数 | 断言 |
|---|---:|---|
| fixedShanghaiOctober17DeadlineMatrixAndHistoricalOverdue | 4 | OPEN/PROCESSING/REVIEW/CLOSED分别检查10-16与10-17期限，在固定2026-10-17上海日期的unresolved/overdue/wasOverdue；CLOSED不计当前未决/逾期而保留历史 |
| shanghaiMidnightIsIndependentOfUtcCalendarDay | 4 | 固定UTC2026-10-16的15:59:59Z/16:00:00Z跨上海10-16/10-17，验证边界；没有改本机时间 |
| baselineIssueImpactBlocksOnlyWholeProjectOrIncludedAndFreezesExclusionBasis | 5 | WHOLE_PROJECT/INCLUDED未决BASELINE_BLOCK阻断真实发布，issueIds精确匹配且失败无release；DEFERRED/CANDIDATE/REMOVED单独影响项不阻断I，真实快照保留问题状态类别与排除依据JSON |
| dataIssueAndAdvisoryPreserveFunctionalSignatureAndAllowSpecificationBaseline | 2 | 来源先有匹配语义/QS/QC及实际可用，需求本人确认。DATA_READINESS使关系不可用，ADVISORY保留可用；两者需求version/semanticVersion/tuple/人/时间保持，均计未决且允许真实需求规格基线。关闭后不新增质量证据；数据问题移除阻断可恢复已有证据的可用性，不误读为自动核验 |
| handlerCannotRemoveBaselineBlockAndReviewerMustSupplyEvidence | 2 | handler自行降ADVISORY/缩影响至排除项均PROJECT_DENIED，完整记录不变且仍阻断；reviewer缺依据INCOMPLETE且不写，补依据后版本+1、事件actor/reason及前后影响快照正确，gate重新允许 |
| staleSourceOrMappingCloseBindingRollsBackEvidenceAndHistory | 2 | SOURCE/MAPPING改变后旧CLOSE binding为VERSION_CONFLICT；issue/全部Workbench记录（含证据/历史/待办）、需求/历史、治理事件完整不变；读取新binding关闭成功。与T03已验证需求改变例互补 |
| 四种事务先后 | 4 | 见下节 |

真实服务包括Workbench/Requirement/Project/Access/Review/GovernanceStore/Root/Decision/Release，真实H2与生产同类TenantDatabaseInterceptor。仅外部用户/平台权限API使用mock；Store spy仅stub `userFacts()`外部系统用户事实查询，tenant lock、对象读写、precheck/publish、业务判断均正常执行，不能把这项边界stub表述为所有Store调用真实。治理前提是受控TEST根与独立批准决议，不是正式批准。

## 四种事务先后与冻结不变量

两个独立线程/REQUIRES_NEW/READ_COMMITTED事务，连接对象不同且autoCommit=false；屏障、firstWrote、secondAttempted、commitFirst控制先后。第一业务写未提交时第二调用200ms内不得返回，释放提交后才核对结果。所有等待有界，finally停止worker后才允许测试清理。

- CLOSE先提交：初始REVIEW BASELINE_BLOCK；旧token发布在后，拒绝INCOMPLETE，release数量不变；第一方成功关闭后的完整持久状态与失败发布后相同，排除发布额外写入；新precheck/token再发布成功，快照CLOSED、issue版本与当前一致、未决0。
- publish先提交再CLOSE：初始REVIEW ADVISORY允许发布；冻结REVIEW/原issue版本/未决1；之后当前CLOSED/未决0，旧release快照不回写。
- REOPEN先提交：初始CLOSED BASELINE_BLOCK预检允许；重开后旧token发布拒绝INCOMPLETE且无release/额外记录；新gate明确BASELINE_ISSUES_OPEN，当前OPEN/cycle2，新token也不能绕过阻断。
- publish先提交再REOPEN：初始CLOSED可发布，冻结CLOSED/cycle1/原版本/未决0；随后当前OPEN/cycle2并重新阻断，旧release正文保持。

不要求未关闭BASELINE_BLOCK也能发布；不把DATA_READINESS清零作为需求基线新门槛。H2并发不升级为生产MySQL竞态验收；固定日期纯函数断言不升级为运行页面已注入日期。

## 执行者交接

候选已冻结；请主线程停止运行MySQL的业务写入并串行交给executor后再执行。最小命令（仓库根目录）：

```bash
JAVA_HOME=/Users/waitforpro/.local/jdks/java8/Contents/Home /opt/homebrew/bin/mvn -B -ntp -pl yudao-module-hrm -am -Dtest=HrmPayrollT04IssueConsistencyTest -Dsurefire.failIfNoSpecifiedTests=false test
```

先保留编译/测试命令、退出码、Surefire XML与完整失败现场；失败交回coder，不由executor改产品。独立通过后，由主线程决定联合T02/T03/Workbench/Review/Governance回归。请求executor gpt-6-luna / max，实际模型身份单列。无论测试结果如何，主线程可见UI的代表类别/门槛/独立复核/关闭重开历史仍须按计划复核；本交接不自行关闭T04或宣称AC-22通过。
