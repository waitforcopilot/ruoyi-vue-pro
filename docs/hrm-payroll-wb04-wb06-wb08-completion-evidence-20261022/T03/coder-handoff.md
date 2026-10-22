# T03最小服务契约候选交接

日期：2026-10-22。范围：计划第6节、PRD6.7问题流转；内部证据，不是最终fix报告。coder期望/请求为gpt-6.1-sol / medium，实际模型未核实。

本轮只新增`HrmPayrollT03IssueContractTest.java`；未改产品代码、T02测试或既有GovernanceServiceTest。未执行编译/测试、DB、浏览器、重启或提交。独立H2/Spring真实Workbench、Access、Project、Requirement、Governance Store/Root/Decision，安装与T02相同的生产租户SQL handler；只mock外部用户与平台权限API。TEST根/决议不是正式批准。当前没有确定需要修复的产品缺陷，候选先验证契约。

## 39个计划执行实例

| 契约 | 测试与独立断言 |
|---|---|
| 两cycle完整流转 | `twoCompleteCyclesPreserveRejectReassignmentAndFirstClosureFacts`：CREATE→START→SUBMIT→REJECT→REASSIGN→补正SUBMIT→CLOSE→REOPEN→START→SUBMIT→CLOSE；每步核对记录version+1、state、cycle、唯一事件actor/time/reason/snapshot。两次CLOSE历史分别保留cycle1处理/复核/关闭日/曾逾期；第一次处理人2、接替处理人1的提交依据不混写。第二cycle新期限、处理中/复核字段重置且前周期留历史 |
| 缺依据/原因，各6例 | `missingActionEvidenceRejectsWithoutAnyWritesAndCanBeCorrected`及`missingOperationReasonRejectsWithoutAnyWrites`：START/SUBMIT/REJECT/CLOSE/REOPEN/REASSIGN在合法前态逐缺；INCOMPLETE，全部业务records、需求/需求历史、治理事件完整序列不变；补齐后成功 |
| 错人/平台权/自关 | `wrongAssignedActorCannotExecuteAction`6例、`assignedHandlerStillNeedsPlatformActionPermission`、`handlerCannotSelfCloseAndWrongReviewerCannotClose`：有广权但非分派人、无权5、被分派却缺平台issue-handle、自关及错误复核均PROJECT_DENIED且不写；恢复权限/正确复核成功 |
| 错状态 | `invalidStateRejectsIncludingCloseEvidenceRollback`6例：START重复、OPEN时SUBMIT/REJECT/CLOSE/REOPEN、CLOSED时REASSIGN均INCOMPLETE；精确全记录不变。CLOSE当前实现先写EVIDENCE再校state，测试明确验证事务回滚，而不将临时语句等同已落库 |
| 重开期限 | `reopenDeadlineRejectsWithoutDestroyingFirstCycle`3例：null/空/2026-02-30分别INCOMPLETE并含新期限提示，保留原processing/review/closedDate及完整记录；补有效期限cycle+1。缺新证据/原因由前两组覆盖 |
| 分派目标 | `reassignmentRejectsIncompleteOrInvalidPeopleAndPreservesOldAssignments`5例：缺handler/reviewer、二人相同→INCOMPLETE；停用目标handler2/reviewer3→PROJECT_DENIED，不因停用独立根签署人/起点保管人制造伪阳性，原2/3分派保持 |
| 失效处理人续办 | `revokedHandlerCannotContinueAndAuthorizedSuccessorKeepsOriginalProcessingFacts`：原2提交后撤真实ROOT，原2后续动作/受限历史拒绝；4管理改派1/3，3驳回，1补正，3关闭；历史保留2与1各自actor/processing |
| 项目/租户/过期version | `staleVersionCrossProjectAndCrossTenantCannotChangeIssue`：旧START版本冲突；有两项目权限管理者也不能替换issueId；tenant2有自身LOCK且issue不可见，动作/历史均DENIED；回tenant1完整记录不变，正确角色续办成功 |
| 关联版本绑定 | `closingWithStaleDependencyBindingRejectsAndCanUseFreshBinding`：待复核期间需求改动，旧expectedBinding→VERSION_CONFLICT且CLOSE证据回滚，读取新binding后独立关闭成功 |
| 关闭不造确认/质量 | `closeDoesNotConfirmRequirementCreateQualityOrMakeUnverifiedSourceAvailable`：适用需求+来源+关系，无有效语义/QS/QC；关闭前后需求完整JSON及签字空、来源/关系JSON、质量/语义证据集合均不变，来源关系仍不可用；只增加一条ISSUE_CLOSE证据 |

逾期历史夹具使用上海当日减一天，并记录保留事实，不修改系统时钟；完整固定日期逾期/阻断统计属于T04。本包不运行发布/导出。

## 页面分工与角色

服务拒绝恶意对象/租户/身份/旧version由确定性测试承担，不能由隐藏按钮冒充。正常UI代表链由主线程可见浏览器执行：问题创建→处理提交→独立驳回→补正关闭→新证据/期限重开→第二周期关闭，缺项时输入保留、历史两周期及有效成员接替。266 manager需平台query+issue-manage、项目根READ/ISSUE_MANAGE；处理人另需平台issue-handle、根READ/ISSUE_HANDLE；独立复核另需平台issue-review、根READ/ISSUE_REVIEW，二人不同。279旧根1416只有READ/CONFIRM，不能据此作ISSUE复核；267旧项目根1394撤销，不作为当前业务授权。仅在project153合法当前scope与TEST决议内搭最小根，不扩大历史项目；先核实平台权限交集，再由既有独立登记/签署流程建立根。

## Luna串行执行

仓库根目录最小命令：

```bash
JAVA_HOME=/Users/waitforpro/.local/jdks/java8/Contents/Home /opt/homebrew/bin/mvn -B -ntp -pl yudao-module-hrm -am -Dtest=HrmPayrollT03IssueContractTest -Dsurefire.failIfNoSpecifiedTests=false test
```

先仅新增测试；失败保留Surefire XML/日志/退出码并返回coder，executor不改产品。通过后建议联合`HrmPayrollWorkbenchServiceTest,HrmPayrollT02ConfirmationContractTest,HrmPayrollRequirementServiceImplTest,HrmPayrollRequirementProjectServiceTest,HrmPayrollReviewServiceTest,HrmPayrollGovernanceServiceTest,HrmPayrollT03IssueContractTest`，是否联合执行由主线程安排。请求executor gpt-6-luna/max，实际身份另核实。仅通过测试不自动宣布T03闭包或完整AC-21页面验收。
