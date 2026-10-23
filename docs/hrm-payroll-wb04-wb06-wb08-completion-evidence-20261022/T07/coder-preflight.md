# T07决议、门槛与发布一致性最小预审

日期：2026-10-23，Asia/Shanghai。内部证据，不是最终fix报告。coder期望/请求gpt-6.1-sol / medium；实际模型未核实。本轮只读计划§10、既有真实治理27项及Decision/Release/Root代码；不写Java、不改产品/既有测试、不运行编译/测试、DB、浏览器或提交。

## 可复用的已存在断言

27项真实GovernanceServiceTest采用真实Store/Root/Decision/Release/Projection/Export，外部User/Permission mock、Store.userFacts外部系统事实query stub。已有测试的通过结果须引用executor联合记录，不能把本预审当重新执行。

- `approvalIsIndependentAndOldBodyIsImmutable`已有自批准拒绝、独立批准、旧body不变、旧版本REPLACED；`overlappingIndependentDecisionFamiliesAreRejected`已有重复覆盖族拒绝。无需复制一套相同成功链。
- `matrixReplacementPreservesSignatureButRuleReplacementRequiresRereview`已有Q-04矩阵替代保留签字，Q-05规则替代清当前签字/REVIEWING后再确认成功。
- `futureAndExpiredVersionsAreCalculatedWithoutChangingSystemTime`已有注入Clock的FUTURE→EFFECTIVE/REPLACED→EXPIRED标签，尚非发布gate/current coverage逐边界断言。
- `publishAndRetriesRequireCurrentRootAndExactDependencyAndRequest`已有相同请求回同release、同key改reason拒绝、撤销发布根后拒绝；`relatedDecisionRevocationStillRejectsExactRetryAfterSuccessfulPublication`与`disabledPublisherCannotReuseSuccessfulRequest`已覆盖撤决议/停用发布人不能重用成功请求。
- `unrelatedProjectRootAnchorAndPersonnelCannotInvalidatePublishOrExactRetry`精确断言无关依赖不改变token且retry不增RELEASE；`relatedAnchorAndPersonnelChangesStillInvalidatePreparedPublication`已有相关人员/anchor变化后拒绝且无release，不重测同义组合。
- `emptyProjectOnlyCreatesFixedEmptyDraft`已有I空完整发布拒绝、固定空草稿label；`archiveAndStaleScopeAndUnexplainedExclusionBlockPublication`已有归档/范围变更拒绝，但只检查allowed/抛异常，未为每个条件隔离精确gate code。
- T02已验证scope/confirm两次序，T04已验证问题close/reopen与发布四次序，T07不再复制这些问题事务测试；新增的是决议/范围/确认依赖与发布的跨服务冻结一致性。

## 必须补足的最小独立断言

| 新增组 | 具体隔离条件与验收不变量 |
|---|---|
| 决议草稿/提交不影响旧有效决议 | 旧Q-04/Q-05为当前依据，创建替代DRAFT、再SUBMIT时，current选中仍旧record/version/body，已有签字不变，发布允许。发布冻结草稿存在可以记录但used=false，不误当有效批准。批准之后才发生旧REPLACED及新选中 |
| root与scope批准边界 | 对PENDING_SIGNATURE/REVOKED/EXPIRED root及批准人越scope分别调用真实action APPROVE，精确PROJECT_DENIED且decision/root/业务事件不写；合法独立root正例复用已有，不靠按钮不可点。过期root时间须在其合法日期范围内构造，不修改系统时间；Decision clock和Root within的时间来源不同，不能只注入Decision clock就声称root也过期 |
| 当前依据validity与发布gate | 在固定clock下逐一断言未来新决议尚不覆盖、旧仍current；生效瞬间新成为唯一current；到期端点仍有效、端点后一瞬不有效。撤销有效决议后CURRENT_DECISION_COVERAGE_REQUIRED且发布拒绝；未来替代撤销后旧replacement标记取消、旧继续有效。不要仅断言lifecycle字符串。测试结束restore injected clock，避免共享context污染 |
| 必需决议/范围/确认/排除/完整性门槛逐项隔离 | 固定全部满足夹具，各次仅改变一项：无实际scope、必需Q缺失、决议只覆盖子scope、当前tuple缺/失效、完整可见性不足、排除reason/responsible/reevaluate各缺、I空；核对精确blocks条目及相关objectId、publish业务错误code、RELEASE/EVENT/快照不增。scope变更可能同时引出多个合理blocks，断言必含目标理由且无成功快照，不为了“唯一原因”放宽合法联动 |
| Q-03/06/08/09分类 | REQUIRED之外的4类逐类独立：未解决且无分类/互相冲突→UNRESOLVED_QUESTION_CLASSIFICATION；BASELINE_BLOCK→UNRESOLVED_BASELINE_QUESTION；DATA_READINESS/ADVISORY只记录分类且不强制清零才能发布；独立有效该Q决议→RESOLVED并使用其批准事实。采用真实draft/submit/approve替代族，不直接mock judgement或current。无需把4×所有内部变体做无差别笛卡尔积；每Q覆盖缺/冲突/阻断/允许，允许类别共享代表边界即可 |
| 幂等补强与确切错误 | 复用已有串行同key断言；新增两个真实事务相同key且相同依赖发布：返回同releaseId、版本只+1、BASELINE_PUBLISH事件只一条。旧token依赖变化→INCOMPLETE且无副作用；新token/新requestKey在条件恢复后可发布，不能把失败后换号而未修条件称恢复 |
| 真正的发布/依赖并发 | 下节按双连接控制提交顺序，不能用串行过期请求替代。业务依赖更改先提交时旧token拒绝无快照；发布先提交时完整冻结旧依赖，后续不回写。Q-05批准导致真实签字失效，新发布需本人重确认，不允许新决议自动重签 |

## 最小并发矩阵

未来候选建议独立`HrmPayrollT07GovernanceConsistencyTest.java`，复用T02/T04真实tenant LOCK、双连接REQUIRES_NEW/READ_COMMITTED及有界屏障。只mock外部User/Permission与用户事实query，不mockStore.lock/current/judgement/precheck/publish；新测试不改既有27项。

- 决议Q-04批准替代→旧token发布：前者先提交则旧token拒绝；新预检后发布用新body/approvedBy/approvalAuthority；相反顺序发布成功冻结旧有效矩阵及原事实，当前新矩阵不改旧snapshot。草稿新版本也属于dependencies变动，可能使预检token过期，即使业务gate仍允许；不得把此保守冲突误记为草稿使旧决议失效。
- Q-05语义替代→发布：替代先提交则旧token拒绝且当前需求重审无签字，新gate缺当前确认；本人确认/新token后才可V2。发布先提交冻结原决议/确认tuple，新批准随后仅改当前，旧snapshot不含混合正文/批准状态。
- scope修订→发布两顺序：先修订拒旧token且新gate要求当前scope/决议/确认；发布先提交冻结旧scope与相同scope的tuple/决议。恢复条件只按本包已有scope测试/真实补齐前提，不重新扩展T01所有范围UI。
- requirement确认依赖变更（真实业务语义save或合法assign）→发布两顺序：先改清签使旧token拒绝，后改不重写已冻结旧确认；与T02“confirm vs scope”的对象不同。若覆盖会与Q-05先改清签完全重复，可选保留scope与Q-05矩阵加一个普通save代表，而不新增同义fixture。

每成功快照逐项比较gate.used decisions、完整决议正文/digest/approval事实、需求scopeVersion/tuple/语义、授权矩阵与统计；每失败比较RELEASE数量与治理EVENT/业务records不变。不能只用publish抛异常或快照文件存在推断一致。

## UI、交接与正式材料边界

主线程正常可见UI至少新TEST材料登记→独立签署→决议草稿/提交→不同起草人的有权批准，展示一个门槛可见拒绝和补齐后的发布正例/同请求重试回读。不以历史TEST1370有效直接证明本轮批准流程，恶意越scope/自批准/竞态服务测试与代表UI互补，不要求页面注入全部非法payload。正式授权/批准材料仍独立未满足；T07工程TEST通过不能关闭该材料依赖。

本轮尚未创建Java候选或执行新测试；以上是最小设计清单，编码时先固定尚缺项，已有断言按精确测试名复用。T06原生Excel/Calc仍待实际下载验证，不能因这些治理测试结构通过而判T06完成。
