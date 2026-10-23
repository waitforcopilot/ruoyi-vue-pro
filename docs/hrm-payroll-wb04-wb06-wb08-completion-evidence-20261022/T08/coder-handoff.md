# T08 静态候选交接

日期：2026-10-23，Asia/Shanghai。内部交接，不是最终 fix 报告。T08 仍进行中，编码候选已准备，编译、测试及页面验收均未执行，不能写 PASS 或已验收关闭。

## 身份与候选

coder 期望为 Codex / gpt-6.1-sol / medium。父线程本轮通过 collaboration.spawn_agent 显式请求 fork_turns=none、model=gpt-6.1-sol、reasoning_effort=medium；该请求参数由父线程本轮补充核对。没有运行端返回的模型/effort证据，**实际模型未核实**，不以模型自述或仓库配置代替核实。

HEAD：`fdc503dca517b4dad4ea136f73bd9fbe92e11c59`。本次继续已有未跟踪的部分 Java 候选，只编辑该候选并新增本交接；没有改产品、已有治理测试、T07、最终 fix 报告或预审原件，没有提交、打包、访问业务数据库或浏览器。

| 文件 | SHA256 |
|---|---|
| 新候选 `yudao-module-hrm/src/test/java/cn/iocoder/yudao/module/hrm/service/payroll/HrmPayrollT08FrozenExportContractTest.java` | `e2cb4de8ab689d1935305702c6d79abe3422ce9da68bb580b154989ddf6cd6b4` |
| 已有未提交 ExportService | `d244b492cd3e90ff1c03f8e255edcda9985abfccbad4991433492db259e8e4e0` |
| 已有未提交 GovernanceServiceTest | `9374a3e8a29edbd9d9ad131cdf9e32fc13f45437481089ddfd5302f9ca2f499e` |
| 既有 T07 测试 | `df7a1ca2f754e6a4ad3eb2f3d7ededc391b9415a56c19f8657f9c15c477e23fc` |

后端/前端构建及实际运行服务身份本子任务未核实；HEAD及上述源码哈希不能证明当前服务已使用此候选。既有工作区其他差异全部保留。

## 16 个计划执行实例

4 个普通测试和4组参数化测试，共16个实例，全部尚未执行。下表是拟验证的契约，不能据此认定产品已满足。

| 测试方法 | 实例 | 核心断言 |
|---|---:|---|
| richHistoricalSixSheetDownloadRetainsAllBusinessFieldsAfterRealCurrentChanges | 1 | rich V1 包含3需求、3来源、4关系、多字段、NA及2问题；通过真实业务服务改变来源名/规格/单位、替换交付、mapping transform、问题状态、外部用户显示名及独立批准替代Q-04；先断言当前值确已改变，再比同权限V1全部六表及真实claim XLSX重建内容。只剔除EXPORT-METADATA的生成时间，冻结的决议、批准、根、成员及关系事实均随全字段比较 |
| narrowedScopeRejectsFullPublicationAndOldFullJobButAllowsExactCurrentSectionBytes | 1 | 撤原reader根、真实委派前半月READ/EXPORT/BASELINE_PUBLISH；旧完整job因stamp改变拒绝；预检FULL_CLOSURE_READ_REQUIRED、完整发布PROJECT_DENIED且无副作用；新job自身受限节选可导出、统计U=2/sourceCount=1，隐藏多边每需求合并1占位，隐藏typed IDs/正文/依据digest不出重建字段及XML，其他有效成员自身job成功且不能借其身份领取旧job |
| currentAccessLossAtEachDownloadStageCannotReuseOldIdentityAndOtherMemberUsesOwnJob | 6 | REVOKED/DISABLED × CREATE/GENERATE/CLAIM前撤权或停用；精确PROJECT_DENIED、完整持久状态不变，旧QUEUED/READY保持；其他当前有效成员按自身job领取成功 |
| releaseAndJobProjectTenantSubstitutionRejectsWithoutLeakingOrMutatingOwnedObjects | 1 | 同租户第二项目及tenant2替换release/job，覆盖列表、projection、create/generate/status/claim、decision history；第二租户真实LOCK防缺锁遮蔽，tenant SQL不可见旧SOURCE/RELEASE/JOB；错误码/通用消息及业务状态不变；tenant2另建有效anchor/root/决议/需求/发布/导出正例，回tenant1原状态与bytes仍可用 |
| associatedChangeCommitsBeforePublishRejectsOldTokenThenFreezesCompleteNewContracts | 2 | SOURCE/MAPPING先提交，第二事务旧token发布INCOMPLETE、无release/事件；真实重核规格/映射、本人重新确认、新token发布后六表含新契约，原V1继续旧契约 |
| publicationCommitsBeforeAssociationChangeKeepsOneFrozenSixSheetObjectGraph | 2 | SOURCE/MAPPING变更在发布后提交；同一release在首事务冻结projection与后续projection完全一致，claimed bytes重建无新契约，当前需求已REVIEWING |
| hiddenSourceCardinalityCannotChangeCurrentRestrictedSixSheetContents | 1 | V1后新建第4个真实隐藏来源并核验、发布V2；同一当前受限账号下载两版本，除公开发布ID/版本/时刻与每次导出metadata外全部六表完全一致，隐藏来源数量/正文/typed ID不通过统计或内容泄漏。此例不把来源数同其他合法公开数值硬比较 |
| generationAndCurrentRootRevocationSerializeWithoutClaimingFormerCompleteBytes | 2 | GENERATE_FIRST/REVOKE_FIRST两个真实事务；生成先提交可READY但后撤权必须拒绝claim，撤权先提交则generate PROJECT_DENIED且保持QUEUED、无副作用；其他有效成员自身新job成功 |

XLSX重建独立读取POI workbook的全部6张表、每个父记录/字段/值类型/片段，校验重复记录、片段完整性、无公式及类型，重建全部内容与projection逐字段对账，校验跨表typed引用闭合。受限占位和受限发布依据作为合法终点保留；不为了引用闭合暴露隐藏对象。权限测试另扫描重建字段与ZIP内xl/*.xml，依据正文/digest亦在受限重建字段扫描。此前27项保留复用，不重写为同义测试。

## 执行边界与方法

真实Store/Root/Decision/Release/Review/Projection/Export/Requirement/Project/Access/Workbench，真实H2及生产同类tenant SQL handler。仅外部User/Permission API mock及Store.userFacts外部系统事实query stub；projection、bytes、stamp、身份检查和claim正常执行。TEST anchor登记为隔离合成夹具，并经真实root登记、独立SIGN及决议DRAFT/SUBMIT/APPROVE。不是正式业务批准。

并发用两个独立REQUIRES_NEW/READ_COMMITTED连接、屏障、有界latch；第一事务业务写后暂不提交，第二事务在观察窗口必须等待tenant lock，释放后检查确定结果。没有Thread.sleep概率竞争。拒绝不变量覆盖项目、成员、需求、需求历史、项目事件、Workbench records、JOB/EVENT/RELEASE/ROOT/DECISION/ANCHOR；不只统计数量。

本轮只做静态调用签名/代码路径阅读、哈希及git diff --check（退出码0），未编译或运行新增断言。没有执行证据确认产品缺陷，因此没有修改产品。静态候选存在编译/夹具/期望不符的可能，须执行者验证并保留首轮失败原件。

## 交给 executor

等主线程停止共享TEST/浏览器写入后串行执行；明确请求 executor / gpt-6-luna / max，实际模型另核实。最小命令（仓库根）：

```bash
JAVA_HOME=/Users/waitforpro/.local/jdks/java8/Contents/Home /opt/homebrew/bin/mvn -B -ntp -pl yudao-module-hrm -am -Dtest=HrmPayrollT08FrozenExportContractTest -Dsurefire.failIfNoSpecifiedTests=false test
```

执行者保存候选身份、完整命令、退出码、Surefire XML及stdout/stderr；失败交回coder，不能静默改产品/断言或覆写失败证据。通过后由主线程决定与现有治理27项、T07及相关T02/T03/T04的受影响回归范围。

页面层仍须主线程可见操作：V1发布→当前变更→同权限历史重导；受限账号看受限label/占位并导出；撤权/停用后旧任务及真实下载link明确拒绝；其他有效成员自身job成功。service异常不等同HTTP 403/链接防绕过或浏览器验收；同租户/跨租户计数矩阵仅在当前服务对象边界拟验证，不宣称全部AC-14或生产MySQL通过。

T06实际落盘SHA、原生六表逐张阅读及页面回归仍待完成；本测试的POI结构/内容不能替代原生视觉。用户最新确认T09本轮仅本机TEST工程联合验收，正式业务批准另行安排；本交接不关闭正式材料依赖、不扩大到正式业务交付，也不改写9项计划分母或最终fix报告。
