# 薪酬需求征集工作台：依据PRD v0.4的代码逻辑审计与修复清单

## 1. 结论与候选身份

**当前代码具备早期需求清单功能，但尚未实现PRD v0.4定义的需求征集工作台。建议按下列15个工作包修复/补建；本轮不修改业务代码。**

| 项目 | 审计记录 |
| --- | --- |
| 日期 | 2026-10-17，Asia/Shanghai |
| 需求基准 | [PRD v0.4](hrm-payroll-requirements-workbench-prd-20261017.md)，仍待业务确认 |
| PRD SHA-256 | `7350322dd7d729275d2e424cecc04961f3c672edd86e29043a7502151523e985` |
| 仓库/分支 | `/Users/waitforpro/source/github-yudao/ruoyi-vue-pro` / `work` |
| 代码HEAD | `e1983f94ac1967949f0e30272dc931413ca3e48a` |
| 工作区边界 | 原有交接文档修改、PRD及审计/整改文件均保留；代码无本轮修改，PRD是未跟踪文件，不能把HEAD作为PRD身份 |
| 方式 | 静态追踪前端→API→控制器→服务→模型/SQL，检查测试及脚本，按PRD反例推演 |
| 执行限制 | 未调用业务写接口、未运行迁移/单测/浏览器/性能测试、未生成XLSX；不认定部署漏洞已复现或任何新AC运行通过 |

P1表示阻断相应工作台能力定稿/交付的逻辑或能力缺口，P2表示应在对应体验/验收前补齐。下表有10项P1、5项P2，无已证实的P0事故；这些是相对新PRD的交付差距，不能据此否定既有工资核算能力或原阶段验收。

## 2. 可复用基础与关键证据

本轮确认存在：租户认证/权限框架、43个候选种子、需求编辑、记录版本冲突校验、本人确认及有效用户检查、需求行历史快照、模块筛选/返回业务页、12类候选来源目录、单表Excel导出和移动弹窗布局。

这些基础均有实际代码，不能把“未实现v0.4”表述为“整个项目没有需求功能”。同样，既有薪酬批次快照、数据就绪、来源占用、工资输入工单不具备工作台所需的项目/来源/决议对象语义，不能直接计为v0.4相应条款通过。

证据路径相对仓库根目录；行号为本轮实际读取代码的一基行号：

| 证据 | 位置与事实 |
| --- | --- |
| E01 模型 | `yudao-module-hrm/src/main/java/cn/iocoder/yudao/module/hrm/dal/dataobject/payroll/HrmPayrollRequirementDO.java:12`：单需求行含code、文本映射/来源、负责人、status/readiness/version；未含projectId、独立来源关系、发布及决议对象 |
| E02 数据库 | `sql/mysql/upgrade/20261010-hrm-requirements.sql:3`：需求表；`:13`唯一键tenant_id＋code；`:15`历史表只关联requirement_id/version；`:34`维护与导出菜单动作 |
| E03 接口授权 | `yudao-module-hrm/src/main/java/cn/iocoder/yudao/module/hrm/controller/admin/payroll/HrmPayrollRequirementController.java:21`：list/save/initialize/history/export，save仅检查update权限；`:31`导出当前列表单张“评审清单” |
| E04 保存逻辑 | `yudao-module-hrm/src/main/java/cn/iocoder/yudao/module/hrm/service/payroll/HrmPayrollRequirementServiceImpl.java:72`：版本及确认必填；`:83`检查actor等于请求reviewerId；`:90`复制请求字段，设置当前确认人/时间；`:110`保存需求行历史 |
| E05 初始化 | 同服务`:136`按code去重初始化；`:22`起43项种子，不含COL；没有projectId参数或templateItemId/version |
| E06 请求契约 | `yudao-module-hrm/src/main/java/cn/iocoder/yudao/module/hrm/controller/admin/payroll/vo/HrmPayrollRequirementSaveReqVO.java:15`：允许DEFERRED；`:16`允许客户端提交READY/EXEMPT，没有结构化适用性及核验证据 |
| E07 评审页 | `yudao-ui/yudao-ui-admin-vue3/src/views/hrm/payroll/requirement/index.vue:124`负责人从用户列表选；`:146`直接选状态；`:154`直接选就绪；`:300`统一保存；`:332`当前列表导出；`:188`历史操作人姓名从当前用户列表解析 |
| E08 上下文 | `yudao-ui/yudao-ui-admin-vue3/src/views/hrm/payroll/components/RequirementContext.vue:2`：链接＋候选来源弹层；`:42`只传module/from，不保存字段/操作锚点 |
| E09 候选目录 | `yudao-ui/yudao-ui-admin-vue3/src/views/hrm/payroll/requirements.ts:21`：静态DS目录；`:95`模块映射；`:116`PLAN直接指向formula、TAX指向tax-policy、OT与HOUR同指work |
| E10 前端API | `yudao-ui/yudao-ui-admin-vue3/src/api/hrm/payroll/requirement.ts:3`：扁平对象；`:21`起仅清单/保存/初始化/历史/导出，不带项目或发布版本 |
| E11 真实业务写入 | `yudao-ui/yudao-ui-admin-vue3/src/views/hrm/payroll/formula/index.vue:193`保存调用api.create，`:204`批准调用api.approve；对应`src/api/hrm/payroll/formula.ts:21`调用正式公式create/approve端点 |
| E12 权限复用边界 | `yudao-module-hrm/src/main/java/cn/iocoder/yudao/module/hrm/framework/datapermission/HrmPayrollDataPermissionRule.java:28`把requirement/history列入SHARED；`:80`对此类表不追加该规则的部门谓词。这不代表取消其他租户/接口权限，但不能代替项目成员授权 |
| E13 已有测试 | `yudao-module-hrm/src/test/java/cn/iocoder/yudao/module/hrm/service/payroll/HrmPayrollRequirementServiceImplTest.java:24`预期43种子；`:29`确认/历史；`:38`版本冲突；`:63`不适用仍用带来源的confirmed夹具；共9项@Test，本轮仅阅读未执行 |
| E14 业务工单/占用 | `HrmPayrollInputIssueService.java:37`按工资批次授权；`HrmPayrollSourceClaimService.java:21`说明防止跨工资批次重复使用工资日期/加班等，均位于HRM的`service/payroll/`，不等于评审来源登记及问题对象 |

缺失判断同时依据完整需求模型/接口/调用链及检索结果，而非单凭类名搜索。对HRM主Java源码、MySQL SQL及Vue3前端src共3542个Java/SQL/TS/Vue文件检索：projectScopeVersion、templateItemId、mappingVersion、deliveryVersion、PROJECT_SCOPE_EDIT、DECISION_APPROVE、BASELINE_PUBLISH、SCOPE_MISMATCH均0处。存在其他名称实现的理论可能，因此结论限定于上述工作台入口和相关链路，不外推为全仓库绝无通用组件。

## 3. 修复/补建工作包

| 编号/优先级 | 当前差距及类别 | 修复项目 | 对应PRD与验收 | 证据 |
| --- | --- | --- | --- | --- |
| F-01 / P1 | 新能力：租户内全局编号，无法表达双项目及范围修订 | 增加评审项目、稳定ID、项目内编号、projectScopeVersion、继承/显式子集和SCOPE_MISMATCH；增量迁移旧数据，范围修订失效传播；metadata独立 | 6.5/6.8；AC-02/23/26 | E01/02/05/10 |
| F-02 / P1 | 补强：编辑/分派/确认未分离，未实现项目成员授权 | 分动作接口或统一动作鉴权；服务器反查目标项目，检查成员、组织/对象范围、被分派评审人；覆盖历史/计数/导出和成员撤销 | 9.1；AC-14/18/19/26 | E03/04/07/12 |
| F-03 / P1 | 逻辑调整：修改语义可携CONFIRMED保存并重新写签字，未执行强制重审/版本依赖检查 | 区分内容修订与确认动作；关键字段、范围、来源/映射/规则变化时回到评审中；确认绑定完整引用元组和当时授权，拒绝旧版本/并发失配 | 6.3/6.8/7.5；AC-09/17/25/26 | E04/06 |
| F-04 / P1 | 新能力：实际来源是需求文本，候选来源只是静态字典 | 建实际来源登记、需求—来源多对多、映射及消费字段、语义/运营/交付版本、影响集合和站内待办；跨项目复制独立ID | 6.6/7.2/7.5；AC-04/17/23 | E01/08/09/10 |
| F-05 / P1 | 逻辑调整＋新能力：readiness由请求直接赋值，无QS/QC证据 | 由供给、范围/语义/映射核验、来源层QS、消费层QC及未决问题推导可用；禁止普通保存直接置READY；变更后失配证据留历史、分层重验 | 6.3/7.6；AC-05/17/27 | E04/06/07 |
| F-06 / P1 | 逻辑调整＋新能力：DEFERRED仍是评审状态，没有独立范围取舍或征集统计 | 新增候选/纳入/暂缓/移除决策及理由/替代ID；实现U/E/I/C/T/D/L、缺口占位、S0～S4与空分母；区分全项目和可见统计 | 6.4；AC-07/10 | E01/06/07 |
| F-07 / P1 | 新能力：没有工作台强制业务决议/正式基线发布链路 | 建决议版本、生效/撤销/替代、批准授权根、独立动作、发布门槛和幂等；范围/规则/决议并发不能拼旧签字发布 | 8.2/8.3/9.1；AC-25/26/28 | E01/03/10及检索 |
| F-08 / P1 | 补强＋新能力：历史只存需求行，导出当前列表单表 | 实现releaseId及六表关联对象快照，冻结来源/映射/QS/QC/问题/决议/授权和显示名；按当前权限重导出、受限占位与下载再鉴权 | 8.1/8.3；AC-16/19/20/28 | E03/04/07/10 |
| F-09 / P1 | 新展示能力：原型评审十视图未形成独立示例工作台；当前复用路由是正式业务页 | 建无需真实员工/工资准备的评审入口和一致虚构样例；明确展示模式、独立数据适配和演示写隔离；保留正式业务入口，不把原公式保存/批准按钮当演示 | 3/4/5；AC-01/11/12/15 | E08/09/11 |
| F-10 / P1 | 新能力：工作台问题/期限/独立复核/发布阻断没有对象链路 | 增加issueId、关联/影响集合、处理→复核驳回/关闭→重开，独立人员校验、阻断分级和逾期统计；不直接挪用发薪工单 | 6.7；AC-21/22 | E01/03/14及检索 |
| F-11 / P2 | 逻辑调整：合法不适用仍被来源负责人和来源系统必填阻断 | 增加结构化适用性及条件必填；不适用有理由及有权本人确认时豁免来源/负责人/映射；切换时关系失活、签字失效、统计和导出同步 | 7.4；AC-06/09/10 | E04的78～86行、E06、E13 |
| F-12 / P2 | 补强：初始化43项缺COL，缺模板版本及项目内幂等 | 补齐43＋3、templateItemId/version、CUSTOM序列、模板升级比较与显式采纳；处理并发及旧迁移，保留人工/移除记录 | 6.5；AC-02/23 | E02/05/13 |
| F-13 / P2 | 补强：模块内只有跳转链接/弹层，意见不带持久化字段和操作上下文 | 增加页面内征集区、字段/操作/流程入口、上下文对象及锚点，保存刷新后可定位；完善十视图前后导航 | 4.2/4.3；AC-01/03/13 | E01/07/08/09 |
| F-14 / P2 | 逻辑调整：模块候选来源与v0.4第5节不一致 | CALC补DS-09；TAX补DS-05/06/12；OT补DS-05；复核其他差异并记录业务依据；用明确映射一致性检查避免目录漂移 | 5/7.1；AC-01/04 | E09的95～105行 |
| F-15 / P2 | 验收缺口：当前测试针对旧单表契约，不能证明新28项AC | 建v0.4权限正负例、双项目/迁移/并发、质量分阶段恢复、决议/导出快照、两会话持久化、移动端与性能样本；升级旧断言而非仅增加测试数 | 9.2/11；AC-01～28，重点24 | E13、现runtime_requirement_regression.py及runtime_browser_workflows.py |

F-01是项目及范围底座，F-12是其模板/初始化子能力；F-04是来源与映射对象，F-05是据其证据推导可用；F-07是决议/发布规则，F-08是其持久化快照/导出。实施拆包时沿这些边界分工，避免重复建表和重复计算工作量。

## 4. 需优先修正的现有逻辑反例

### F-02：有编辑权限不等于有权确认

当前save的入口只有update权限，服务直接使用请求reviewerId，在其等于actor且用户有效、其他文本齐全时允许CONFIRMED；没有服务器保存的分派记录或独立确认动作检查。静态路径允许一个仅有维护权限的有效用户把reviewerId填为自己进入确认分支。修复后必须同时满足独立REQ_CONFIRM、项目/对象范围和服务器分派。此处未使用真实账号发请求，不能据此宣布部署攻击已成功。

### F-03：内容修订与重新签字混在一次保存

当前加载已确认行后修改description/fieldMapping等，保持CONFIRMED，由原评审本人保存，可直接增version并更新confirmedAt；没有先生成评审中版本及校验依赖的阶段。PRD要求关键语义变化先重审，普通保存不能默认再次签署全部新内容。应保留旧快照，显式区分修订、重新打开和确认动作。现有staleUpdateIsRejected可以复用，但不覆盖来源/范围/决议并发。

### F-05：READY并不依赖核验结果

VO允许readiness=READY，服务通过BeanUtils直接复制并更新；当前界面也提供该选项。新契约下，数据未交付、QS/QC失配或问题未关闭均不可用。应将就绪状态改为服务端计算结果，客户端提交供给事实或核验动作，不提交最终可用结论。旧功能原本允许人工维护，此项属于相对v0.4的契约调整，不把原行为描述成旧版规则下的事故。

### F-11：不适用例外没有贯通必填校验

meaningfulMapping接受“不适用：具体理由”，但更早的CONFIRMED检查仍要求sourceSystem和sourceOwnerId，之后还检查来源负责人有效。现有explicitExemptionWithReasonIsRetained测试复用了已填写这两个字段的夹具，所以不能覆盖“合法不适用且来源字段为空”的新正例。应新增该正例及空理由、适用缺映射、切换后失效三个反例/转换场景。

## 5. 实施顺序及验收边界

| 阶段 | 工作包 | 退出条件 |
| --- | --- | --- |
| 1：项目与鉴权 | F-01、02、12 | 增量迁移保留旧ID/历史；双项目46项互不影响；独立授权、范围修订及撤销正负例通过 |
| 2：评审对象与状态 | F-03、04、05、06、10、11 | 来源/映射/问题/适用性真实持久化，状态失效、S0～S4、QS/QC恢复和并发旧版本拒绝符合PRD |
| 3：交付证据 | F-07、08 | 决议有效性、授权闭包、发布幂等、六表历史与当前权限裁剪通过 |
| 4：评审体验 | F-09、13、14 | 先以加班完成示例→字段意见→来源→评审→导出，再铺其余视图；无正式业务写入、外发或员工绑定前置 |
| 贯穿阶段 | F-15 | 每阶段补对应执行证据，最终完成28项AC和规定采样；不把既有测试/页面打开当作新验收 |

F-09的示例设计和F-13的交互样稿可以与底座并行准备；接入真实工作台保存要等待第1～2阶段对象/权限契约稳定。优先级不是自动授权改代码，本轮仅列项目供用户审阅。

业务决议仍是实施前提：实际成员、组织/期间上限、授权根材料、质量检查规则和首期范围不能由源码或审计报告代签。已有真实工资系统应保留；新增工作台使用独立对象与适配层，增量迁移先备份并核对既有加密密钥。

## 6. 本轮验证与未完成事项

- 已执行候选HEAD/PRD哈希、工作区状态及适用AGENTS查找；未发现适用AGENTS文件。
- 逐行读取需求控制器/服务/VO/DO、迁移、API和页面，抽查业务写操作、权限规则、薪酬工单/来源占用及9项需求测试。
- 静态提取种子得到43项、COL为空；对照PRD第5节复算CALC/TAX/OT来源缺项如F-14。
- 未运行测试或构建；不能判断新的项目隔离、质量、决议、发布及导出运行结果，也未评估完整薪酬/劳动税务正确性。
- 本轮仅新增本报告，不修改PRD及既有审计记录，不执行修复、不改库、不停止服务、不提交推送。

该清单是v0.4的静态差距与修复计划，不是对新工作台已经开发完成后的独立验收结论。
