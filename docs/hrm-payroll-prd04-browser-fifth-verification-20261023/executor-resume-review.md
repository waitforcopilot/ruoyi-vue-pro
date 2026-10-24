# PRD04 第五轮 executor 证据续审与最小续测矩阵

日期：2026-10-23  
仓库：`/Users/waitforpro/source/github-yudao/ruoyi-vue-pro`  
审查范围：只读检查第五轮目录内浏览器证据、矩阵计划和实现关联源码；不操作浏览器、数据库，不修改产品源码、原报告或已封存证据。

## 身份与执行边界

- 当前 HEAD：`925ac220b3b09c973f9b64797bd35cf4a66a2c9b`。候选身份与原报告、PRD、主要源文件哈希见同目录 `preflight-identity.json`；准备记录显示所列源路径当时无 tracked diff。此次没有改动这些源文件。
- 角色约定要求 executor 请求 `gpt-6-luna / max`；本次任务的请求模型按该值记录，实际运行模型没有运行端证据，仍为“未核实”。浏览器由主线程在可见 IAB 操作；本次只读查看已保存的 AX/DOM 文本和截图。
- 本次没有运行测试或生成新的业务回归结果。历史测试数和此前轮次结论不计入第五轮。本报告只评估本目录现存证据及主线程随后转交的操作摘要。

## 现有浏览器证据复核

| 证据 | 可支持的结论 | 限制或续审结论 |
|---|---|---|
| `browser/001-start-editor266-governance153.*` | 可见页面为 `projectId=153` 的治理页，项目名为 `TEST WB completion 20261022 A`，范围版本为5；当前页列出266的ACTIVE TEST授权根。 | 这是后续选择TEST153的依据；不能替代在评审页重新读取当前项目选择、版本及操作权限。 |
| `browser/002-project154-created-scope-undefined.*` 与 `003-project154-name-history.*` | 页面显示项目154范围未定义；历史元数据明确 `projectScopeVersion:null`、`scopeJson:null`。 | 项目名修改的读回不能补出项目范围或范围版本。154不适合作为需要范围版本的实施关联/协作保存夹具。 |
| `browser/004-project154-template-empty.*` 至 `007-template-compare.*` | 004显示空需求列表；005、006页面均显示46条候选，007显示当前模板无待比较项。 | 005/006证明保存的页面读回为46条；文件名和刷新后的列表不能单独证明第二次初始化按钮确实被执行，因此幂等动作仍缺独立操作证据。模板无差异也只支持当前页面的比较结果。 |
| `browser/008-opinion-OV-refreshed.*` 至 `016-opinion-RPT-refreshed.*`、`017-opinion-COL-filtered-readback.*` | 捕获了十视图相关页面/集中清单筛选状态，可用于定位意见和页面入口。 | 不将这些页面内容扩成46项业务确认，也不从视图打开推定该项业务已验收。 |
| `browser/018-flow390-saved-filtered.*`、`019-flow390-history.*` | 截图显示390宽度页面中TEST意见1902及保存提示；历史页用于回读该记录。 | 支持该条页面操作与历史回读，不外推到所有弹窗或页面。 |
| `browser/020-field768-saved.*`、`021-field768-history.*` | 截图显示768宽度页面中TEST意见1903及保存提示；历史页用于回读该记录。 | 仅支持该条页面操作与历史回读。 |
| `browser/022-operation1440-saved.*`、`023-operation1440-history.*` | 截图显示1440宽度页面中的需求1904、操作入口和保存提示；有对应历史页。 | 仅支持该条页面操作与历史回读。 |
| `browser/024-return-operation-anchor.*` | URL 带 `projectId=154&module=COL&anchor=operation-simulate`。 | 页面正文同时显示“未选择项目”、范围版本未定义。它证明返回了带锚点的路由，却没有证明项目上下文恢复成功。续测须在TEST153页面读回项目名、ID和版本。 |
| `browser/025-implementation-empty-evidence-blocked.*` | AX/截图显示需求1904的实施关联对话框，状态为“验收通过（人工记录）”，页面/API/任务引用已填，验收证据为空；保存按钮仍可点。 | 名称中的“blocked”不是提交结果；文件没有提交后的拒绝提示，不能据此判定空证据已被阻止。 |
| `browser/026-empty-evidence-submitted-dialog-remains.*` | 需求1904、当前版本1；保存后对话框仍开，显示通用“操作未完成”提示，已输入内容保留。 | 此次截图里的API引用与验收证据都为空，因而没有隔离出“仅缺验收证据”的负例。它支持拒绝后保留表单，不支持将失败归因于单一字段。 |
| `browser/027-implementation-save-null-parameter.*` | 需求1904状态为验收通过；页面、任务、验收证据引用均有TEST值，API引用为空；项目154范围版本仍未定义。AX显示通用失败提示，表单保留。 | 截图/AX未包含底层HTTP响应原文；“请求参数不正确，不能为null”是本次交接描述。154的空范围版本已足以解释请求级校验失败，API空值又是服务级独立缺项；不能用此记录证明验收证据单项校验失败。 |

截至本次检查，目录含001–027的证据文件；027是矩阵计划编写后追加的记录。本次对025–027截图做了本地只读视觉检查。原始AX/DOM和截图均保留原位，未重命名或覆盖。

## 源码核对：空范围版本与 ACCEPTED 必填项

本仓库实施关联请求的真实字段名是 `projectScopeVersion`，不是 `expectedScopeVersion`。

1. 前端 `review/index.vue` 的 `context(row)` 将 `project.value?.projectScopeVersion` 放入请求上下文（984–991行）；`saveAction()` 把上下文与表单合并，再调用实施关联或协作保存函数（993–1008行）。`review.ts` 将实施关联 POST 到 `/hrm/payroll/review/implementation`，协作请求也经同一路由前缀发送。
2. 后端 `HrmPayrollReviewVO.Context` 要求 `projectId`、`projectScopeVersion`、`id`、`version` 均非空且满足正数/最小值，并要求 `reason` 非空（11–16行）。控制器对实施关联和协作请求使用 `@Valid @RequestBody`（`HrmPayrollReviewController.java` 20–22行）。
3. 154的页面及历史记录均显示 `projectScopeVersion:null`。前端会把该空值放入上下文；与交接中的“不能为null”一致，且两类操作共享该 Context 校验。因此154的实施关联和纪要都可能在进入业务服务前被拒绝。这是源码支持的根因判断；保存的AX/截图自身只显示前端通用提示，没有HTTP响应原文。
4. 对 `ACCEPTED`，`HrmPayrollReviewService.implementation()` 先检查当前用户有查询/更新权限、项目与需求目标可见、需求版本匹配以及 `reason` 非空；非 `UNSCHEDULED` 状态要求 `page`、`api`、`task` 都非空；`ACCEPTED` 另要求 `evidence` 非空（79–84行）。这些缺项由共享的 `PAYROLL_REVIEW_INCOMPLETE` 错误抛出。错误码的固定中文文案为“请补齐责任人、评审依据、数据来源及验收条件”，但实施关联代码没有分别验证这四个标签；它不能替代逐字段判定。
5. 主线程最新转交称：TEST153中新增需求1905完整规格草稿；空 `evidence` 的 ACCEPTED 尝试返回上述通用文案；随后填入page/task/evidence仍失败，接口引用仍空。该转交与源码的 `api` 必填条件相符。此新操作还未出现在本目录的原始AX/截图中，本文按“主线程交接摘要”记录，不将它冒充已封存证据。若153当前有效范围版本确为5，154的空值问题不应套用于153；仍需从可见页面确认当前选择和请求上下文。
6. ACCEPTED只写入人工关联和声明，服务保存的 `conclusionVerified` 固定为 `false`（Service 84行）；完成保存不证明实现或真实业务验收通过。

源码定位：

- `yudao-ui/yudao-ui-admin-vue3/src/views/hrm/payroll/review/index.vue`：984–1008行。
- `yudao-ui/yudao-ui-admin-vue3/src/api/hrm/payroll/review.ts`：10–11行。
- `yudao-module-hrm/src/main/java/cn/iocoder/yudao/module/hrm/controller/admin/payroll/vo/HrmPayrollReviewVO.java`：11–27行。
- `yudao-module-hrm/src/main/java/cn/iocoder/yudao/module/hrm/controller/admin/payroll/HrmPayrollReviewController.java`：20–22行。
- `yudao-module-hrm/src/main/java/cn/iocoder/yudao/module/hrm/service/payroll/HrmPayrollReviewService.java`：28–34、79–84行。

## 后续可见页面导航

沿用矩阵计划的TEST边界，主线程在用户可见IAB继续：

1. 从“薪酬管理 → 需求评审”打开需求评审，再进入十视图工作台；也可从TEST153治理页点“返回十视图评审”。通过“评审项目”选择器选择 `153 · TEST WB completion 20261022 A`。
2. 先确认页面标题/项目横幅及URL共同指向项目153，并在页面读回范围版本5。若项目未选中、范围版本为空或权限状态不明确，停止实施关联写入，不切回154，也不在本轮修订范围。
3. 切到“需求征集（COL）”，按需求ID1905过滤；在打开实施关联前记录当前需求版本、状态和项目上下文。此ID来自主线程最新转交，尚待AX/截图回读确认。
4. 点该行“实施关联”。先按下表逐项执行TEST负例和正例；每次保存尝试之后都刷新或重新打开1905，从列表、表单/历史稳定回读结果。每项保存原始截图及AX/DOM，必要时保留可见页面能提供的请求/响应信息，并记录操作者、项目、范围版本及需求版本。

## 最小续测矩阵

| 步骤 | 表单及前置条件 | 预期与证据 |
|---|---|---|
| P0 范围上下文预检（无写入） | 在需求评审页选择TEST153，重新读取项目横幅、范围版本和需求1905当前版本。 | 项目ID=153、范围版本=5且当前需求可见；保存AX/截图。任一不符即停止，标记未执行。 |
| N1 缺少验收证据 | 状态`ACCEPTED`；填非空TEST page、api、task和依据；仅将evidence留空。 | 预期被拒绝；对话框保留输入；刷新/重开后需求版本和实施状态未变化。只有本项能隔离空evidence校验。 |
| N2 缺少接口引用 | 状态`ACCEPTED`；填TEST page、task、evidence和依据；仅将api留空。 | 预期被拒绝且表单保留；回读版本/状态未变化。对应主线程当前转交中的缺项。 |
| P1 完整人工关联 | 状态`ACCEPTED`；page、api、task、evidence及依据均为清楚标记的TEST引用；项目和需求版本使用页面当前值。 | 预期保存成功；刷新后重开并读回四项引用、状态及新增历史。功能评审/确认状态应独立；结果只能证明人工关联记录持久化，不能证明真实验收。 |

后续证据建议使用新编号028起，分别保存P0、N1、N2、P1截图和AX/DOM；不要复用或覆盖001–027。截图文件名只作索引，每项需以页面稳定回读判定。不得将单一对话框提交结果汇总成其他WB或AC的通过数。

## 结论

现有证据支持TEST154项目范围版本为空、实施关联表单保留失败输入，以及若干TEST153可见治理上下文；不支持在154上完成ACCEPTED保存，也未把025的未提交画面当作负例结果。源码显示154的空 `projectScopeVersion` 是实施关联和协作请求的共同请求级阻断点。TEST153续测应先确认页面当前范围版本，再分别隔离空验收证据、空接口引用，最后用完整TEST引用验证保存与刷新回读。主线程最新的1905结果暂属交接摘要，需新增原始浏览器证据后再计入第五轮结论。
