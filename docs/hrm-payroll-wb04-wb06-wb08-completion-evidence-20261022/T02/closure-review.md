# T02 验收闭包复核

日期：2026-10-22。角色：独立只读复核。请求模型：`gpt-6-luna / max`；实际运行身份未核实。

## 结论

**T02 按任务计划第5节完成工程验收，可以计入已关闭任务。** 判断基于22例T02服务契约测试与包含它的161例联合回归，以及020–025、030–041、042可见浏览器原件。自动化承担恶意参数、权限边界和事务顺序；浏览器承担真实角色的正常操作、提示、输入恢复及持久化。计划没有要求把伪造对象或租户参数改造成可由正常UI发起的操作，也不需要用不同账号重复独立会话流程。

此前[browser-successor-review.md](browser-successor-review.md)列出的项目，是相应负例没有直接页面操作原件。按计划的分层验收方式，并核对新增测试确有精确服务拒绝与无签字/原记录不变断言后，这些不构成未完成的页面必测项。不能把页面原件本身说成证明了恶意请求拒绝；该结论来自服务测试。

## 候选与执行依据

- [自动化执行记录](automation-1/results.md)记录HEAD `beae42d9fe0fbc61e2eab63ffeba0cafd90dfbb5`、测试源SHA-256 `eb9d9e40c0f902aec8942d58eebe00d1595c8c40f265d7208ac6c45705b4c3ae`；本次只读核验的HEAD及测试文件SHA与记录一致。测试类为未跟踪交接文件；记录中已有跟踪差异哈希为`0fa7751b93eeb85bb980bf0061a2d9b1703f774486ed0a193dd0b6416bc9adec`，包含此前存在的治理导出服务/测试差异，T02执行者未修改。
- 新增`HrmPayrollT02ConfirmationContractTest`独立运行22例，0失败、0错误、0跳过；联合命令运行161例，0失败、0错误、0跳过。其中包含T02 22例、Requirement 36例、Requirement Project 26例、Workbench 31例、Review 19例及Governance Service 27例。161已包含22，不应相加。
- T02测试使用真实H2服务、治理Store/Root/Decision、真实租户SQL拦截器及双连接事务；仅外部用户和权限API为mock。它不是MySQL生产部署验收。T02没有增加产品源码改动；浏览器业务对象均为项目153的TEST合成数据。

## 六项计划验收

| 计划验收 | 真实通过依据 | 覆盖层与边界 |
|---|---|---|
| EDIT不能篡改评审人/状态代签；非本人、越范围、失效成员拒绝；有效被分派人成功 | `editOnlyCannotForgeReviewerStatusOrUseConfirmationAction`断言伪评审人→`PAYROLL_PROJECT_DENIED`、伪CONFIRMED→`PAYROLL_REQUIREMENT_CONFIRM_ACTION_REQUIRED`、直接confirm→`PAYROLL_PROJECT_DENIED`；每个拒绝后完整需求JSON及历史数不变；正常EDIT可保存，负责人2本人随后确认。`nonAssignedAndOutOfScopeConfirmersCannotSign`分别断言`PAYROLL_REQUIREMENT_CONFIRMATION_INVALID`与`PAYROLL_PROJECT_DENIED`，确认人为空且历史数不变。撤销/过期/停用三参数化实例均断言失效成员confirm与history为`PAYROLL_PROJECT_DENIED`。页面001显示受限编辑页面没有确认动作；020–025、042覆盖有效授权根、分派和本人确认 | 恶意reviewer/status及失效成员由服务层精确拒绝；不要求通过正常UI构造伪造参数。失效成员用原签字事实/时间保留、使用原版本成功改派并由新成员确认，证明失败后没有改变当前版本与签字链；此三例未单独对每一次失败后的历史行数作即时快照比较，不把它夸写为该类断言。004/005页面显示撤权后的无项目访问提示，但不声称浏览器曾直接点击其confirm或受限历史。 |
| 两个独立会话同版本冲突、保留各自输入、人工合并重提、刷新及重新登录回读 | 服务测试`twoTransactionsRejectStaleSaveThenPersistManualMerge`用两个真实事务/连接读同版本，A提交后B得到`PAYROLL_VERSION_CONFLICT`，A字段保留、B字段未写入；B合并后读到A+B且版本精确+2。[030–041](browser-successor-review.md)记录两个不同origin分别可见UI登录账号266，v38→A保存v39→B旧版本冲突且输入保留→读取最新并手工合并→v40→刷新及退出/重登后回读 | 该UI链完全符合“两独立会话”；计划不要求两个不同用户。041显示1844仍为v40评审中、当前C=0，历史签字和当前有效签字已区分。 |
| 语义变更分别失效、展示/运营更新保留 | `sevenIndependentSemanticChangesInvalidateOnlyTheirConsumers`七实例覆盖业务上下文、规则、适用性、来源契约、映射、评审人、项目范围；断言当前REVIEWING、签字清除、新语义版本及历史旧tuple保留，并检查受影响/未受影响的其他消费者。`operationalChangesPreserveFunctionalSignature`四实例覆盖NAME/CONTACT/OWNER/SUPPLY，断言需求版本、tuple、签字人/时间、来源语义版本和currentConfirmationValid均保持。 | T01范围、映射、适用性页面原件及T02 v37重新评审、v38本人确认、v40编辑后C=0展示用户可见后果；无需将每个内部字段变体重复为UI流程。 |
| 两种scope/旧confirm提交先后及来源/映射旧元组拒绝 | `scopeCommitBeforeOldConfirmationCannotAttachOldSignature`断言scope先提交时旧confirm为`PAYROLL_PROJECT_SCOPE_STALE`、无当前签字；`confirmationCommitBeforeScopeKeepsOnlyHistoricalOldScopeSignature`断言confirm先提交后scope变更清当前状态、只留旧范围历史tuple；`dependencyCommitBeforeOldConfirmationRejectsStaleRequirement`的SOURCE/MAPPING两例旧confirm为`PAYROLL_VERSION_CONFLICT`、当前REVIEWING/tuple空且历史旧签字保留。 | 以上是H2真实事务锁/双连接的确定性次序测试。T01页面已展示范围失配提示/恢复与mapping后果；浏览器截图不能证明数据库提交先后，无需重复UI竞态。 |
| 确认后撤权、旧事实保留、旧成员失权、新成员重新分派确认 | `lostMembershipPreservesPastSignatureButRequiresAuthorizedSuccessor`分别构造撤销ROOT、已过期授权和停用账号；保留既有tuple/confirmedAt；失效用户confirm/history精确拒绝；原有读取者可读历史；有效负责人重新分派，用户3成功确认，旧tuple仍留历史。[020–025](025-old267-new279-history.txt)显示v36由267确认、重派后v38由279确认；[042](042-root1416-read-confirm-scope-details.txt)确认279的根1416只授予项目153/实体101/组织201/TEST/2026-10的READ和CONFIRM，`delegableActions=[]`，由266登记、267签署。 | 服务层验证撤权/过期/停用后的具体权限拒绝，页面验证正常接替链及受限访问结果。042中的anchor权限和范围更宽，但不把anchorFact误作授予279的root权限。 |
| 同租户跨项目/跨租户对象替换拒绝、自有项目正例 | `crossProjectAndCrossTenantObjectSubstitutionCannotMutateOriginal`断言跨项目confirm、搬移需求、替换关系为`PAYROLL_PROJECT_DENIED`；tenant2已持有LOCK后，对tenant1目标的confirm/history为`PAYROLL_REQUIREMENT_NOT_EXISTS`、save/saveRelation为`PAYROLL_PROJECT_DENIED`；回tenant1后完整需求JSON及历史数不变，合法自有项目修订和confirm成功。 | 测试安装并断言生产同类TenantDatabaseInterceptor处于首位。恶意对象替换属于服务层鉴权断言，不要求用浏览器制造越权组合；本证据不扩展为生产MySQL租户配置独立审计。 |

## 保留的限制

本闭包只表示T02要求的WB-04确认、重审、撤权和并发子集完成，不关闭完整AC-14/19/26，也不代表三个WB的交付完成。H2测试不等于生产MySQL并发验收；project153是TEST夹具，不等于真实业务授权、正式批准或真实工资处理。请求模型为gpt-6-luna/max，执行身份仍未核实。
