# T07静态候选交接

日期：2026-10-23（Asia/Shanghai）。内部证据，不是最终fix报告。coder期望/请求gpt-6.1-sol / medium；实际模型未核实，没有替换模型。

新增独立`yudao-module-hrm/src/test/java/cn/iocoder/yudao/module/hrm/service/payroll/HrmPayrollT07GovernanceConsistencyTest.java`，SHA256 `df7a1ca2f754e6a4ad3eb2f3d7ededc391b9415a56c19f8657f9c15c477e23fc`。未改产品、既有治理27项、T03/T04测试或最终fix报告。T03测试仍`dccdbc3e6ce4cc124afab8e91c152e8114986959a252d383547fc447e0e04ef2`，T04仍`52e26c3d4266065875552264dc035504f3aeb09879fcae439237f0fcc0160fc7`。

本轮未编译/运行测试、DB、浏览器、打包或提交；只静态核对文件完整性/调用签名/拒绝顺序与无尾随空白。当前没有证据支持产品缺陷，先交真实契约测试验证。

## 31个计划执行实例

| 测试 | 实例 | 具体独立断言 |
|---|---:|---|
| draftAndSubmissionKeepOldCurrentPolicySignatureAndPublicationGate | 2（Q-04/Q-05） | 新DRAFT及PENDING_APPROVAL阶段旧current/tuple/gate保持，各实际发布冻结旧body/批准事实，新草稿/提交记录可存在但used=false且state正确 |
| futureReplacementUsesExactEffectiveAndExpiryBoundariesInCurrentCoverageAndGate | 1 | 注入Decision Clock：生效前1ns选旧且可发布，生效瞬间选新且批准body/digest/authority使用新；validTo端点有效，后一ns current拒绝/预检缺coverage/发布不写。不是仅检查lifecycle标签 |
| revokingFutureReplacementRestoresOldCoverageWithoutResigningRequirement | 1 | 撤未来替代清旧replacementId/From；时间越过原计划生效点后仍旧current/tuple/gate，无自动重签 |
| revokedCurrentDecisionCannotServeAsCurrentPublicationBasis | 1 | 当前Q-04撤销→current INCOMPLETE，精确CURRENT_DECISION_COVERAGE_REQUIRED、发布INCOMPLETE且完整状态不变 |
| independentApproverNeedsCurrentSignedRootAndMatchingScope | 4 | 指定3为批准人，由真实draft/submit设置；撤其原根后建立PENDING/REVOKED/EXPIRED/子范围ROOT；APPROVE均PROJECT_DENIED、PENDING_APPROVAL及完整状态/事件不写。独立保管1/签署4保持有效，避免停用共同上游产生伪阳性。过期root按运行上海日期-1合法注册日期区间，Decision时钟注入不冒充Root日期注入 |
| eachPublicationGateHasItsOwnControlledReasonAndNoReleaseSideEffects | 9 | 无scope、缺必需Q、Q只覆盖子scope、确认tuple缺、完整可见性不足、排除reason/owner/reevaluate各缺、I空。每例核对精确预检reason并确保无release/业务事件。fixture中的缺失scope/tuple/排除依据用mapper模拟既有不完整持久状态；业务路径仍真实，不mock判断。FULL_VISIBILITY/NO_SCOPE实际publish先受权限/授权范围检查拒绝PROJECT_DENIED，其余INCOMPLETE；不更改产品顺序来统一code，scope合理连带多个blocks不强求只有一个 |
| unresolvedQuestionClassificationMustBeUniqueAndPermittedWhileResolvedQuestionUsesDecision | 4（Q-03/06/08/09） | 每Q依次真实替代所有相关决议：缺分类/互相冲突→UNRESOLVED_QUESTION_CLASSIFICATION；BASELINE_BLOCK→UNRESOLVED_BASELINE_QUESTION；DATA_READINESS/ADVISORY均可发布并冻结正确分类；该Q真实批准独立决议后RESOLVED且被used。保留相同Q-05正文/clauses避免分类变更误触语义重审 |
| identicalConcurrentPublicationRequestCreatesOneReleaseAndOnePublicationEvent | 1 | 两真实事务同key/同token/同内容返回同releaseId，RELEASE恰1、BASELINE_PUBLISH事件恰1 |
| dependencyCommitBeforeOldTokenPublishRejectsWithoutMixedSnapshot | 4（MATRIX/RULE/SCOPE/REQUIREMENT） | 依赖先提交，旧token发布INCOMPLETE、无release，第一方变更后的完整状态与失败发布后相同。矩阵新token可发布新有效body；规则/正文变更先REVIEWING/清签，必须本人重确认后新token才成功；scope新范围保持需补条件，不假造新范围决议已通过 |
| publicationCommitBeforeDependencyChangeKeepsOldScopeSignatureAndDecisionFacts | 4 | 发布先提交，冻结旧body/digest/批准人4及authority、旧scope1与同scope的tuple；后依赖改变仅影响当前，旧snapshot不变。新待批版本若已存在，快照可以含它但used=false，不视为混合生效依据 |

4个普通方法+6组参数化共31个实例，尚未执行，不能写通过。重用既有27项的自批准/重叠族/串行幂等/无关依赖/技术权限边界，不原样镜像重写。T04的问题close/reopen与发布四次序不在本类重复。

## 执行身份与方法

真实Store/Root/Decision/Release/Review/Workbench/Requirement/Project/Access，真实H2及生产同类tenant SQL handler。仅外部User/Permission API mock与Store.userFacts外部系统事实query stub；tenant lock/precheck/publish/current/批准等均正常执行。

事务使用两个独立REQUIRES_NEW/READ_COMMITTED连接、autoCommit=false、屏障与有界latch；首业务写后暂不提交，次调用观察期内不可返回，释放后核精确结果。失败比较项目/需求/历史/Workbench records/全部决议/根/成员授权/治理事件/RELEASE完整持久状态，成功比较冻结决议和需求原tuple及scope；这不是生产MySQL/真实HTTP会话验收。

Decision Clock固定在上海2026-10-23并逐测试restore系统Clock，避免context污染；Root合法期间仍按真实上海日期，未修改本机时间。正式材料未具备，所有根/批准/发布均是TEST合成工程契约。

## 串行交接

候选冻结，等主线程停止运行UI业务写入后交executor。最小命令（仓库根）：

```bash
JAVA_HOME=/Users/waitforpro/.local/jdks/java8/Contents/Home /opt/homebrew/bin/mvn -B -ntp -pl yudao-module-hrm -am -Dtest=HrmPayrollT07GovernanceConsistencyTest -Dsurefire.failIfNoSpecifiedTests=false test
```

请求executor gpt-6-luna / max，实际模型另核实；保留完整命令/退出码/Surefire XML/失败现场。失败返回coder，不让executor静默改产品；通过后主线程决定联合既有治理及T02/T03/T04相关回归。新TEST材料独立签署、决议提交批准、可见拒绝/纠正后发布正例仍由主线程真实UI提供并独立复核；代码不自行关闭T07或正式批准材料依赖。T03 raw期限前端当前冻结不在本后端候选修改范围。
