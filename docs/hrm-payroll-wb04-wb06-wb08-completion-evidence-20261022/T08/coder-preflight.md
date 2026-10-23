# T08历史六表、权限裁剪与下载阶段最小预审

日期：2026-10-23，Asia/Shanghai。内部证据，不是最终fix报告。coder期望/请求gpt-6.1-sol / medium，实际模型未核实。本轮只读计划§11、真实治理27项及Release/Projection/Export代码，不写Java、不改产品/既有差异、不运行编译/测试、DB、浏览器或提交。T06下载/原生阅读仍待实际验收。

## 现有27项中可直接复用的精确覆盖

| 已有测试 | 已断言事实 | 不应扩大成的结论 |
|---|---|---|
| fixedSnapshotKeepsOldNamesRulesDeclarationsAndStatisticsAfterCurrentChanges | V1后改需求description、账号nickname、批准Q-04替代，前后完整projection JSON相等，仍旧显示名而非新名 | 尚未逐项更改source规格/单位/交付、mapping、issue；没有把两个真实下载bytes重建后比较全部六表 |
| hiddenSourcesAndPublicationEvidenceAreMergedWithoutIdsNamesFieldsOrCounts | 撤reader根后只给需求READ/EXPORT，两个隐藏来源名与字段不出现在projection，readiness0/1/sourceCount0、来源及映射各一受限占位、版本说明一受限GRANT | 尚未扫描所有真实hidden IDs、digest/数量/长依据，未证明XML bytes也无泄漏或每条六表引用闭合 |
| downloadRechecksAuthorizationAtEveryStageAndOldJobsCannotBeClaimed | 先生成/领取成功，撤根后status/claim/projection抛异常 | 方法名不能替代断言：此例实际覆盖READY之后撤权，不是三阶段所有情况 |
| queuedGenerationAndNewJobsRejectAfterCurrentRootRevocation | 已create QUEUED后撤根，generate/create/status/claim拒绝，job仍QUEUED | 已覆盖撤权前/生成前的串行服务拒绝，无需原样重写；未覆盖DISABLED、其他有权人的自身job与真实HTTP旧链接 |
| referencedAnchorChangesStillRejectQueuedAndReadyExport | 相关anchor版本变化使QUEUED生成、READY查询/领取拒绝 | 不代表人员停用或scope收缩也逐项测过 |
| unrelatedAnchorCannotInvalidateQueuedOrReadyExport | 无关anchor变更不妨碍生成/领取，同job.bytes相等 | 不应让全部历史访问一律失效作为“安全修复” |
| replacedDecisionReferencedByOldConfirmationStillHasResolvableSnapshotRow | 替代前后决议均在冻结版本说明，记录号唯一 | 未断言所有授权引用及后续变更不会漂移 |
| inactiveMappingAndRemovedNaDeclarationRemainResolvableInFrozenSixTables | V2仍含失活MAPPING及已移除NA声明，V1仍有旧来源名，统计不虚算sourceCount | 尚非字段/单位/交付/问题/人员/批准事实全量冻结对账 |
| visibleSourceAndMappingDoNotExposeRestrictedDecisionFreeText | 业务可见来源/映射，不可见决议依据时restricted sentinel正文不出projection，显示受限依据 | 未等同文件全量泄漏扫描及summary/JSON嵌套计数对抗 |
| sourceAuthorityCannotStitchAnotherModulesControlledGrant | 不同模块root动作不能拼成来源授权 | 不代表release/job跨租户都已验证 |

`listingManyRowsResolvesCurrentAuthorityOnceAndRechecksNextRead`与`readLocalSourceGraphIsDiscardedAfterFailureAndCannotCrossProjects`是读取缓存边界/性能与本请求隔离，可复用，不重复改为同义测试。T02/T03已用真正tenant SQL过滤验证需求/问题对象，T08必须在RELEASE/JOB/投影/下载对象上另验，不能推论继承。

## 最小新增候选设计

独立建议`HrmPayrollT08FrozenExportContractTest.java`。真实Store/Root/Decision/Release/Review/Projection/Export/Workbench及生产同类tenant SQL handler；只mock外部User/Permission，Store spy只stub外部userFacts查询；不mock projection、bytes、authorizationStamp、claim或身份检查。不覆盖原GovernanceServiceTest/ExportService未提交差异。

1. **一个丰富V1、一次完整变更、全六表重建对账。** 用真实service构建两个需求（适用+NA）、来源、多字段mapping、issue及TEST决议/授权，真实publish V1→create/generate/claim得before bytes；后用真实saveSource改name/specification/unit、supply替换交付、saveRelation改transform、issue动作、外部nickname变更、独立批准新决议。以同有效账号重新导V1，比较两个projection及XLSX重建的全部父记录/字段/types/refs与冻结清单；仅排除EXPORT-METADATA导出时间等明确允许项。不要求ZIP字节相同；另断言before/after当前对象确实已改变，避免“没有改成功所以V1相等”伪阳性。原生排版归T06，此测试只证明冻结内容/结构。
2. **精确scope收缩的projection+实际bytes裁剪。** 对既有两个组织/模块/对象范围的rich fixture，在V1后合法缩小reader当前授权；需求集合等于当前READ∩EXPORT∩filter，允许边保留、隐藏多边合并占位，统计基于可见集合重算且label是权限受限节选，绝非完整基线。建立hidden sentinel清单（IDs、name、正文、digest与数量字符串需避免与合法公开数值偶然同值），扫描所有六表重建字段与sharedStrings/inline字符串；同时检查合法ID/refs未误抹。placeholder与“受限发布依据”是合法受限终点，不为闭合引用泄露真实隐藏ID。需要检查大小/基数时用两种隐藏对象数量但同可见集合的projection比较，排除合法metadata，避免只凭无hidden名字就称无数量泄漏。
3. **部分scope不可完整发布、允许按自身权导出。** 当前reader有子scope READ/EXPORT与技术baseline-publish或项目局部BASELINE_PUBLISH也不能拼成全项目可见性；precheck明确FULL_CLOSURE_READ_REQUIRED，完整publish拒绝且不新增release；同reader对既有V1创建自身受限job可生成/领取，label与可见清单正确。另用全项目有权账号正例；不为“防泄漏”禁止所有历史导出。不要将完整可见性缺失误解为所有用户连release列表都必须禁止，按接口设计审查可返回的元数据边界。
4. **停用三阶段的精确缺口，撤权用已有断言补不变量。** DISABLED在create之前、create后generate前、READY后claim前分别独立夹具，明确PROJECT_DENIED/相关业务code、无新JOB/EVENT、原QUEUED/READY不变、文件不给出；当前有效其他成员通过自己的create/generate/claim成功，不能借其合法权claim原job（job绑定actor）。ROOT撤销路径已有前述两测试可复用；只补原未断言的exactcode/无副作用和其他成员正例，不复制六套同义阶段。scope收缩使旧authorizationStamp失配时，旧job应拒绝；必须新建本权限job才可得受限导出，不允许旧完整bytes复用。
5. **同租户跨project与跨tenant RELEASE/JOB矩阵。** 在tenant2也建LOCK，避免缺锁错误遮蔽；分别替换project/releaseId/jobId，对release详情projection、列表可见集合、decision history、create/generate/status/claim、文件bytes执行负例，断言无泄漏正文/文件/他项目计数且精确code。先区分接口的合法元数据列表与对象详情，不凭“列表非空”就认定泄漏。错误消息只允许既有受控通用文案，不回显隐藏对象；原job及release字节/状态不变；自有项目/tenant正例。真实HTTP状态/下载link由主线程可见UI/网络原件复核，不以服务异常当403截图。
6. **关联改动与冻结一致性。** 与T07普通scope/decision并发分工：只对source/mapping或issue关联更新，与真实publish冻结控制两种先后。更新先提交时旧precheck token拒绝、无release，补齐当前语义/确认/新token后只冻结新完整依赖；publish先提交则六表projection/rebuilt bytes引用同一冻结旧规格/映射/问题版本，后续当前变化不回写。无需为每种字段都跑双事务，选一个source契约与一个mapping/issue关联代表即可，T04已有issue close/reopen四次序不重复。任务生成期间当前授权变更的序列化可共享一代表事务：生成先提交后撤权仍不得claim，撤权先提交生成拒绝，无拼接投影/旧stamp绕过。

## 断言与验收层次

新测试均核对完整records/JOB/RELEASE/EVENT状态或内容不变量，使用精确ServiceException code；已有27项较宽泛assertThrows不需要全量重写，只在新增边界补足。真实事务用已验证双连接模式，不把Thread.sleep竞争概率当固定顺序；Clock注入仅Decision有效性，不改系统时间。

正常可见UI最小代表：V1发布→当前业务变更→同权限历史下载/六表显示旧事实；受限账号看到受限label/占位且自身下载成功；撤权或停用后旧任务/链接真实请求拒绝；另一当前授权成员自身任务成功。恶意project/tenant ID替换主要由服务/SQL层确定性矩阵承担，不要求向正常UI加绕过控件，也不能用按钮隐藏代替后端拒绝。页面错误泛化与网络明确拒绝分别留证，不把前者升级成所有接口授权PASS。

T06负责实际落盘SHA与原生六表逐张可读性；T08负责相同文件的冻结/裁剪业务内容与权限边界。两者共享真实下载清单但结论分开；原生尚未完成时T08结构不替代T06。正式业务批准材料与测试根仍分开。当前仅设计，尚未新建Java、执行任何新增断言或关闭T07/T08。
