# T02剩余验收映射（内部证据）

日期：2026-10-22。coder期望/请求：gpt-6.1-sol / medium；实际模型未核实。本文件只做验收口径映射，不是最终fix报告；用户要求九包全部完成后统一交付最终fix报告，原fix报告作为历史过程记录保留。本轮不运行测试、不操作DB或浏览器、不提交，也不覆盖既有证据。

依据：任务计划第5节、[executor页面复核](browser-successor-review.md)、[22/161实际自动化结果](automation-1/results.md)及固定测试源码`HrmPayrollT02ConfirmationContractTest.java`。22是新增契约测试实例，161是包含这22实例的联合总数，不能相加为183。H2服务测试与当前运行页面证据互补；均不宣称生产MySQL并发或正式业务批准。

## 严格按计划分配验证层

计划明确服务端拒绝恶意参数，未要求将每个恶意请求强行改造成正常UI操作；第二条则明确要求独立会话、保留输入、人工合并、刷新与重登，必须由UI证明。隐藏按钮不是服务鉴权证据，但无需为证明后端拒绝增加前端伪造参数入口。浏览器网络注入、token注入、API登录亦不是本包关闭前置。

| 计划条款 | 适合正常UI执行的观察 | 已执行服务层精确断言 | 当前映射与剩余 |
|---|---|---|---|
| REQ_EDIT篡改reviewer/status不能代签；无权/非本人/失效成员拒绝，有权本人成功 | EDIT-only界面动作边界；合法分派与本人确认；撤权后无权提示 | `editOnlyCannotForgeReviewerStatusOrUseConfirmationAction`：伪reviewer→PROJECT_DENIED，伪CONFIRMED→CONFIRM_ACTION_REQUIRED，直接confirm→PROJECT_DENIED；每次原需求完整JSON与历史数不变，合法EDIT后2确认成功。`nonAssignedAndOutOfScopeConfirmersCannotSign`：非分派3→CONFIRMATION_INVALID、越范围2→PROJECT_DENIED，签字空、历史数不变。`lostMembership…`三实例：撤销/过期/停用2的confirm及history均PROJECT_DENIED | 001仅证明隐藏动作，不能代替左述服务断言；004/005证明撤权后页面拒绝，020—025/042证明279受控READ/CONFIRM根、改派重审和本人正例。恶意reviewer/status、非本人和过期/停用确认拒绝由已通过服务测试承担；不新增每种账号实际登录的强制条件。停用账号未必能登录业务页，不能要求其先登录再点确认 |
| 独立会话同版本冲突、输入保留、人工合并重提及刷新/退出重登回读 | 正常UI必须执行整条链 | `twoTransactionsRejectStaleSaveThenPersistManualMerge`：双连接/事务同版本；A未提交时B等待；A提交后B VERSION_CONFLICT；A字段保留，重新读取并写B字段，独立事务读到A+B与精确版本+2 | 030—041及executor复核已支持127.0.0.1/localhost两个origin各自UI登录同账号266，v38→v39冲突→人工合并v40→刷新→真实退出重登。计划只要求独立会话，不要求不同账号；无需再增加不同账号重复流程 |
| 各组语义失效与运营更新保留 | 代表流程应能看见重审/历史；不要求每个字段变体重复UI | `sevenIndependentSemanticChanges…`7实例：业务上下文、规则版本、适用性、来源契约、映射、评审人、项目范围，目标REVIEWING、新语义版本、签字清除、旧tuple历史保留；来源契约/项目范围两消费者均失效，其余其他消费者保留。`operationalChanges…`4实例：NAME/CONTACT/OWNER/SUPPLY精确保留需求版本、tuple、签字人/时间、来源语义版本及currentConfirmationValid | T01映射/范围/适用性代表后果、T02改派v37/确认v38及业务goal改动至v40 REVIEWING/C=0相互补足。不存在“所有字段都得重新跑页面”的额外前置。来源字段所有内部变体不属于本包额外全量验收；既有单位/类型测试已联合通过，完整QS/QC归后续包 |
| scope修订/旧confirm两种先后，来源/映射旧元组拒绝 | UI可展示最终重审、范围提示，不能证明数据库提交顺序 | `scopeCommitBeforeOldConfirmation…`：scope先提交，旧confirm SCOPE_STALE，scope2/SCOPE_MISMATCH且无签字；`confirmationCommitBeforeScope…`：确认先提交，scope2当前REVIEWING无签字，历史仅一个scope1/确认人2签字；`dependencyCommitBeforeOldConfirmation…`SOURCE/MAPPING两实例：旧confirm VERSION_CONFLICT，当前重审/tuple空，历史旧tuple保留 | 已通过真实H2锁/双连接事务。T01范围页面结果支持用户后果；不要求新增UI竞争来替代或重复这组确定性事务断言 |
| 确认后撤权，旧事实保留、旧成员不再confirm/history，新成员接替 | 正常UI执行授权接替、改派、本人确认和历史两签字；撤权后页面无权提示 | `lostMembership…`分别撤销真实ROOT、过期grant、停用用户；原tuple/时间保留且当前事实有效；原2 confirm/history拒绝；有权1可读历史；1改派3后3确认，2原tuple留历史 | executor已复核020—025/042，满足新根→重新分派→新本人确认→旧/新历史签字代表流程。004/005是无项目访问，不误称直接history/confirm已点过；这两项拒绝由精确服务测试承担，无需向正常页面添加绕过按钮 |
| 同租户跨项目/跨租户对象替换拒绝，自有项目正例 | 正常项目切换可见性是辅助；UI选项本就不提供恶意对象配对 | `crossProjectAndCrossTenantObjectSubstitutionCannotMutateOriginal`：无第二项目权2 confirm→PROJECT_DENIED；有两项目权1不能移动需求/复用关系；tenant2有独立LOCK且4种mapper对象不可见；confirm/history→NOT_EXISTS，save/saveRelation→PROJECT_DENIED；回tenant1需求完整JSON/历史数不变，合法修订确认成功 | 已通过生产同类TenantDatabaseInterceptor安装后的服务/SQL断言。跨对象参数替换本质属恶意请求测试，不追加必须在浏览器构造/登录其他租户的验收条件；本测试不证明生产租户配置已额外审计 |

## 对页面复核“仍缺”项的处理

`browser-successor-review.md`逐项准确指出没有直接页面原件，不应改写为已有页面实测；其“若验收口径要求”是条件性边界，不是计划新增条款。EDIT-only伪造、撤权后直接confirm/history、跨项目/租户替换、过期/停用确认均已有确定性服务断言及不写入不变量，正常UI代表流程已有独立复核。不能只靠001隐藏按钮声称服务鉴权通过，也不能反向把所有服务恶意负例都增加为必须页面注入才关闭。

按当前计划文字，尚无必须新增的正常UI流程缺口。剩余是executor按本映射确认22/161实际结果与001—009、020—042原件的覆盖互补，固定候选/证据身份并给出T02整体闭包判断；本coder映射不自行宣布关闭，不回填历史报告，不替代executor。若复核发现某条实际断言/原件缺失，再针对该缺失补证，不增加同义工作包。

## T03只读范围与最小候选设计（编码前交接）

目标严格限计划第6节及PRD6.7：问题创建/分派、开始、提交、独立驳回、补充重提、独立关闭、给新依据/新期限重开、第二周期再关闭；逐动作版本/actor/状态/历史。服务负例为自关/错误复核者/状态/依据/期限、成员失效后重新分派、跨项目/租户/旧版本，以及关闭不得伪造需求确认或QS/QC。不展开T04完整逾期/阻断统计与发布验收。

已读实现：`HrmPayrollWorkbenchService.issueAction`先context/对象/版本，再ISSUE_HANDLE/REVIEW/MANAGE及被分派身份校验；依据必填；START/SUBMIT/REJECT/CLOSE状态机；CLOSE绑定当前关联版本且取真实治理依据；REOPEN新期限解析、cycle+1并清当前周期字段；REASSIGN要求未关闭及有效不同成员；event保存actor与snapshot。`WorkbenchPanel.vue`有上述正常动作、重新分派、历史入口；`issueValidation.ts`已对依据、原因、期限及不同处理/复核人引导，失败弹窗保留draft。现有Workbench测试覆盖一个驳回/关闭/重开串行链、部分自关/错复核、关闭旧binding/缺依据及无效期限，但mock治理；未完整独立断言第二周期、成员失效接替和问题对象越租户。

候选先只新增`HrmPayrollT03IssueContractTest.java`（独立文件），采用T02已验证的最小真实Store/Root/Decision+租户拦截器夹具，只mock外部User/Permission；不修改T02或原GovernanceServiceTest。最小测试分组：

1. 全链真实动作至第二周期关闭，每步独立回读version/actor/state，历史保存第一次processing/review/closedDate/cycle/曾逾期；不以history.size>=N代替内容断言。
2. 各负例精确code及issue完整JSON/版本、EVIDENCE/HISTORY/TODO记录数不变；合法补齐后成功。CLOSE治理先于状态判断可造成错误提示优先级，但事务回滚下未见越权/落库缺陷，不为提示猜测先改产品。
3. 真实撤销处理人ROOT后旧动作拒绝；有权manager重新分派有效不同成员，新处理人续办，历史原actor/processing保持；错误目标或自兼处理/复核拒绝。
4. 真租户过滤、跨项目issueId、旧版本失败；关闭前后需求签字/语义版本、来源语义/交付及QS/QC证据集合不被自动创建或恢复。来源无有效质量时关闭仍不能使其可用。
5. UI仅补代表全链、独立角色、缺依据/重开期限输入保留和历史两周期；恶意跨对象/越租户仍用服务测试承担。

当前没有已验证的产品缺陷，产品最小变更为零。REASSIGN在REVIEW状态保留旧processing、REOPEN清当前周期字段均有历史快照兜底，须用上述历史内容断言确认，而非先假设抹除历史。若新测试证实具体违约，再向主线程报告失败链并最小修改WorkbenchService/对应UI；本轮尚未开始T03编码，等待主线程串行交接安排。
