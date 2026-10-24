# PRD v0.4 固定11项TEST流程全量回归与修复报告

日期：2026-10-24，Asia/Shanghai。

本轮固定检查计划§4的 **11项最低WB流程全部执行并通过（11 PASS / 0 FAIL / 0 UNVERIFIED）**。范围是本机TEST工作台工程流程；这不是全部PRD、全部AC、正式业务批准、真实薪酬上线或生产就绪结论。逐项结论来自主线程用户可见内嵌浏览器原件及executor对本轮原件的最终复核，不由文件名、点击或历史测试数推出。

验收清单为[第五轮固定计划](hrm-payroll-prd04-browser-fifth-verification-20261023/verification-matrix-plan.md)§4；沿用同轮前段001–056，再以本次01–127补齐主要业务操作序列，另附128对155范围v1及TEST授权根1467 ACTIVE状态的只读稳定回读。两段编号空间分别列明，不混用其他历史轮。[executor最终冻结v2复核索引](hrm-payroll-prd04-full-regression-20261024/checks/executor-review-final-v2-20261024.json)matrix逐项11项`PASS`、overall总结`PASS_BOUNDED`、固定最低范围无剩余未执行步骤；98个证据引用实路径检查missing=0，记录各文件字节与SHA。冻结索引SHA-256为`84987fa3ed716a275766ecb37f4af4b33bdac1efd75cba22621dd6a92bbcc467`。此前两个私有索引含错误文件名/候选哈希笔误，保留私有历史并已被v2替代，不作为最终复核依据。

## 候选、运行身份和角色

- HEAD：`925ac220b3b09c973f9b64797bd35cf4a66a2c9b`；以下3个Vue为未提交候选，报告不把它们归入HEAD既有内容。
- `review/index.vue` SHA-256：`b953e729e93b59a909f489ad764b159a9f59f9b58427f1408996382285dbf2d7`。
- `governance/index.vue` SHA-256：`621c226e123e957b67a4b86f7755dd3ecfa00ca3c63d18e93d17bf2923ca2fe9`。
- `requirement/ProjectPanel.vue` SHA-256：`3854058b799f61669ba269540296c7a56c6cad80a15b60c420a42a85b093c7b1`。
- 三文件均位于 `yudao-ui/yudao-ui-admin-vue3/src/views/hrm/payroll/`。本轮服务入口为 `http://127.0.0.1:5173`；只读进程核对Vite PID `86218`，cwd为 `/Users/waitforpro/source/github-yudao/ruoyi-vue-pro/yudao-ui/yudao-ui-admin-vue3`，命令为本目录Vite `--mode development --host 127.0.0.1 --port 5173 --strictPort`。这是本机开发服务及工作目录绑定，不是生产构建或部署证明。
- 浏览器操作者：主线程，用户可见IAB，实际切换266编辑/处理、267独立核验及TEST材料签署、279本人确认及问题独立复核、268受限读取。角色职责按每一步当前页面权限，不从账号名称推断全权限。
- coder请求 `gpt-6.1-sol / medium`；executor请求 `gpt-6-luna / max`；两者实际运行模型均未通过运行端元数据核实。没有执行Cursor/Grok独立审计。本报告是修复与工程回归交付，不冒充独立审计结论。
- 报告编写及证据复制没有新增业务操作；页面、自动化执行和证据复核按仓库角色约定分工记录。

## 本轮三项修复

1. **项目范围未定义时的独立操作保护及创建解释。** review页面与独立操作弹窗显示明确范围前置提示；范围决策、实施关联、会议纪要/站内提醒保存禁用且函数再次守卫，意见草稿仍可保存。ProjectPanel说明创建授权范围只作授权边界校验，项目业务范围须另经有权人员修订，创建者不自动获得修订权限。证据[01 scope-guard-implementation](hrm-payroll-prd04-full-regression-20261024/browser/01-scope-guard-implementation.txt)；[02 scope-guard-meeting](hrm-payroll-prd04-full-regression-20261024/browser/02-scope-guard-meeting.txt)；[03 scope-guard-draft-readback](hrm-payroll-prd04-full-regression-20261024/browser/03-scope-guard-draft-readback.txt)；[04 defined-scope-meeting-history](hrm-payroll-prd04-full-regression-20261024/browser/04-defined-scope-meeting-history.txt)。
2. **治理页独立bootstrap授权根加载。** anchors、roots和项目列表使用独立settled结果；无项目READ时仍按材料权限展示独立根，并阻止继续读取项目决议/发布，明确提示权限边界。登记成功显示返回ID，避免因列表加载限制重复登记。证据[49 bootstrap-signer-restricted-project](hrm-payroll-prd04-full-regression-20261024/browser/49-bootstrap-signer-restricted-project.txt)；[55 root1461-active](hrm-payroll-prd04-full-regression-20261024/browser/55-root1461-active.txt)；没有修改后端授权规则。
3. **批量分派失败对象定位。** assignBatch在请求前保存本次项目、目标reviewer及items快照；按服务端有效整数index关联“本次已选请求输入”的ID/version，逐项及整体保留原响应。无有效index不猜对象，不将客户端输入当服务器确认，不细化通用拒绝原因。真实双可见tab制造版本变化并回读成功/失败历史：[110 fixed-batch-mixed-result-identifiable](hrm-payroll-prd04-full-regression-20261024/browser/110-fixed-batch-mixed-result-identifiable.txt)；[111 batch-success-1911-history](hrm-payroll-prd04-full-regression-20261024/browser/111-batch-success-1911-history.txt)；[112 batch-stale-1912-history](hrm-payroll-prd04-full-regression-20261024/browser/112-batch-stale-1912-history.txt)。

## 固定11项逐项结果

下表的PASS只针对固定计划最低操作与所列证据切片。每个新编号链接为原始文本，旁边同stem的jpg保存可见截图；分页42/43和旧053等分别保留全部页面。完整逐文件SHA、字节数及实际编码见后文manifest。

| 固定WB | 结果 | 实际角色 | 实际对象、版本与回读 | 本轮原件证据 |
|---|---|---|---|---|
| WB-01 评审项目与范围 | PASS | 266创建/修订；267独立材料签署；268受限读取 | TEST154创建、更名、范围v1及成员历史；TEST155隔离；成员533向268授予READ后撤销，121稳定显示失效和撤销理由；各角色实际切换。 | [55 root1461-active](hrm-payroll-prd04-full-regression-20261024/browser/55-root1461-active.txt)；[56 project154-scope1](hrm-payroll-prd04-full-regression-20261024/browser/56-project154-scope1.txt)；[62 role268-COL-read-only](hrm-payroll-prd04-full-regression-20261024/browser/62-role268-COL-read-only.txt)；[65 role268-after-revoke-denied](hrm-payroll-prd04-full-regression-20261024/browser/65-role268-after-revoke-denied.txt)；[121 member533-restricted268-revoked-stable](hrm-payroll-prd04-full-regression-20261024/browser/121-member533-restricted268-revoked-stable.txt)；[123 actual-project154-creation-and-scope-history](hrm-payroll-prd04-full-regression-20261024/browser/123-actual-project154-creation-and-scope-history.txt) |
| WB-02 候选清单初始化 | PASS | 266 | 154空0→首次46→重复46；逐页46唯一模板编号并保留人工意见。155测试模板v1→v2比较、差异决策与重复初始化回读，人工状态/历史保留。 | [44 reinitialize60-preserved](hrm-payroll-prd04-full-regression-20261024/browser/44-reinitialize60-preserved.txt)；[45 custom1906-after-init](hrm-payroll-prd04-full-regression-20261024/browser/45-custom1906-after-init.txt)；[74 template-accept-keep-readback](hrm-payroll-prd04-full-regression-20261024/browser/74-template-accept-keep-readback.txt)；[75 template-reinitialize-preserved](hrm-payroll-prd04-full-regression-20261024/browser/75-template-reinitialize-preserved.txt)；[76 template-decision-history](hrm-payroll-prd04-full-regression-20261024/browser/76-template-decision-history.txt) |
| WB-03 页面内征集 | PASS | 266 | 154十视图意见1892–1901，流程1902、字段1903、操作1904；集中清单与历史持久化，模块/字段/流程/operation-simulate返回原上下文后稳定焦点回读。 | [同轮13条意见索引](hrm-payroll-prd04-browser-fifth-verification-20261023/opinions-created-13.json)；[84 return-original-module-stable](hrm-payroll-prd04-full-regression-20261024/browser/84-return-original-module-stable.txt)；[86 return-field-stable](hrm-payroll-prd04-full-regression-20261024/browser/86-return-field-stable.txt)；[87 return-original-flow-approval-anchor](hrm-payroll-prd04-full-regression-20261024/browser/87-return-original-flow-approval-anchor.txt)；[105 return-original-operation-stable](hrm-payroll-prd04-full-regression-20261024/browser/105-return-original-operation-stable.txt) |
| WB-04 需求评审 | PASS | 266完善/异议；279分派本人确认 | 153/范围v5需求1905：补规格，分派279并本人确认；语义变化使确认失效、复审与再确认v11；只缺触发场景/只缺异议下一步各自拒绝、保留输入且无版本变化；最终后续状态见下文v15。 | [07 dispute-saved](hrm-payroll-prd04-full-regression-20261024/browser/07-dispute-saved.txt)；[12 reconfirmed-v11](hrm-payroll-prd04-full-regression-20261024/browser/12-reconfirmed-v11.txt)；[102 only-trigger-missing-field-error](hrm-payroll-prd04-full-regression-20261024/browser/102-only-trigger-missing-field-error.txt)；[103 only-dispute-next-step-missing-field-error](hrm-payroll-prd04-full-regression-20261024/browser/103-only-dispute-next-step-missing-field-error.txt)；[104 invalid-edits-no-version-change](hrm-payroll-prd04-full-regression-20261024/browser/104-invalid-edits-no-version-change.txt) |
| WB-05 来源与字段映射 | PASS | 266维护；267独立规格/质量核验 | 153来源3642、关系3647（需求1905），与既有关系分离；规格/映射及QS/QC分别核验。delivery1 QS3652/QC3656，交付改v2后失配范围与恢复已回读；127当前QS3662绑定delivery2，QC3664绑定QS3662，旧证据同时保留。 | [25 QS3652-QC3656-bound-evidence](hrm-payroll-prd04-full-regression-20261024/browser/25-QS3652-QC3656-bound-evidence.txt)；[27 relation3647-history](hrm-payroll-prd04-full-regression-20261024/browser/27-relation3647-history.txt)；[31 relation3647-after-unlink3613](hrm-payroll-prd04-full-regression-20261024/browser/31-relation3647-after-unlink3613.txt)；[48 delivery2-quality-restored](hrm-payroll-prd04-full-regression-20261024/browser/48-delivery2-quality-restored.txt)；[127 delivery2-QS3662-QC3664-bound-tuples](hrm-payroll-prd04-full-regression-20261024/browser/127-delivery2-QS3662-QC3664-bound-tuples.txt) |
| WB-06 待办与问题 | PASS | 266处理；279独立复核 | 153问题3616：新建后OPEN→PROCESSING→REVIEW，独立关闭；重开填新期限，周期2处理/提交→REJECT→PROCESSING→REVIEW→CLOSED，依据及不同处理/复核人历史可见。关闭不自动确认需求/核验来源。 | [13 issue3616-reopened](hrm-payroll-prd04-full-regression-20261024/browser/13-issue3616-reopened.txt)；[15 cycle2-rejected](hrm-payroll-prd04-full-regression-20261024/browser/15-cycle2-rejected.txt)；[17 cycle2-closed](hrm-payroll-prd04-full-regression-20261024/browser/17-cycle2-closed.txt)；[18 cycle2-history](hrm-payroll-prd04-full-regression-20261024/browser/18-cycle2-history.txt) |
| WB-07 总览与进度 | PASS | 266全项目；268授权范围观察 | 154空分母0/0；153全项目/筛选统计与集合下钻，1907移除U4/E3→恢复U4/E4；最终全局U7→筛选U1且全局确认仍1/7→清除恢复7。筛选不改变全局门槛。 | [77 removed1907-U4E3](hrm-payroll-prd04-full-regression-20261024/browser/77-removed1907-U4E3.txt)；[78 restored1907-U4E4](hrm-payroll-prd04-full-regression-20261024/browser/78-restored1907-U4E4.txt)；[124 global-statistics-visible-U7](hrm-payroll-prd04-full-regression-20261024/browser/124-global-statistics-visible-U7.jpg)；[125 filtered-statistics-visible-U1](hrm-payroll-prd04-full-regression-20261024/browser/125-filtered-statistics-visible-U1.jpg)；[126 global-statistics-visible-restored](hrm-payroll-prd04-full-regression-20261024/browser/126-global-statistics-visible-restored.jpg) |
| WB-08 版本与导出 | PASS | 266按现有TEST授权；279受限历史读取 | 153发布预检查、当前合法TEST材料下新TEST快照1458/v5及导出任务1460；六页投影、下载原文件与原生六表阅读。1905当次DEFERRED，并未因本轮文件宣称其纳入基线。 | [37 TEST-snapshot1458-v5](hrm-payroll-prd04-full-regression-20261024/browser/37-TEST-snapshot1458-v5.txt)；[39 export-downloaded](hrm-payroll-prd04-full-regression-20261024/browser/39-export-downloaded.txt)；[93 snapshot1458-范围与变更](hrm-payroll-prd04-full-regression-20261024/browser/93-snapshot1458-范围与变更.txt)；[96 calc-requirements-75pct](hrm-payroll-prd04-full-regression-20261024/browser/96-calc-requirements-75pct.txt)；[101 calc-version-sheet-75pct](hrm-payroll-prd04-full-regression-20261024/browser/101-calc-version-sheet-75pct.txt) |
| WB-09 桌面与移动评审 | PASS | 266 | 主线程可见IAB真实设置390/768/1440宽；来源picker可选既有TEST来源并保存1910/1911/1912、查看历史；1440沿用此前已选来源。picker选择不自动建立消费关系，1910–1912仍MISSING/来源缺口；未声称三次独立建立映射。像素尺寸依据主线程设置和截图，AX不独立证明像素宽度。 | [79 responsive390-history-source](hrm-payroll-prd04-full-regression-20261024/browser/79-responsive390-history-source.txt)；[80 responsive-768-history](hrm-payroll-prd04-full-regression-20261024/browser/80-responsive-768-history.txt)；[81 responsive-1440-history](hrm-payroll-prd04-full-regression-20261024/browser/81-responsive-1440-history.txt) |
| WB-10 实施关联 | PASS | 266 | 153需求1905：只缺evidence时其他page/api/task输入保留，118通用拒绝；119刷新仍v14。补完整TEST引用后120持久化v15、ACCEPTED、conclusionVerified:false；需求REVIEWING/DEFERRED独立。 | [28 only-evidence-missing-rejected](hrm-payroll-prd04-full-regression-20261024/browser/28-only-evidence-missing-rejected.txt)；[29 only-api-missing-rejected](hrm-payroll-prd04-full-regression-20261024/browser/29-only-api-missing-rejected.txt)；[118 accepted-only-empty-evidence-final-error](hrm-payroll-prd04-full-regression-20261024/browser/118-accepted-only-empty-evidence-final-error.txt)；[119 empty-evidence-no-version-change-1905-v14](hrm-payroll-prd04-full-regression-20261024/browser/119-empty-evidence-no-version-change-1905-v14.txt)；[120 complete-evidence-accepted-1905-v15-history](hrm-payroll-prd04-full-regression-20261024/browser/120-complete-evidence-accepted-1905-v15-history.txt) |
| WB-11 评审便利功能 | PASS | 266分派/纪要/提醒；279收件/本人职责 | 1905会议v3、分派v4、站内提醒v5及目标279回读，非相邻v1/v14比较。修复后两可见tab混合批次：请求1911v1成功→v2/reviewer279；请求1912v1，另一tab先存v2后该项失败，历史v2/reviewerNULL。逐项可定位请求ID/版本并保留原generic reason。 | [035 role279-reminder-and-confirm-only](hrm-payroll-prd04-browser-fifth-verification-20261023/browser/035-role279-reminder-and-confirm-only.txt)；[82 nonadjacent-version1-14-compare](hrm-payroll-prd04-full-regression-20261024/browser/82-nonadjacent-version1-14-compare.txt)；[110 fixed-batch-mixed-result-identifiable](hrm-payroll-prd04-full-regression-20261024/browser/110-fixed-batch-mixed-result-identifiable.txt)；[111 batch-success-1911-history](hrm-payroll-prd04-full-regression-20261024/browser/111-batch-success-1911-history.txt)；[112 batch-stale-1912-history](hrm-payroll-prd04-full-regression-20261024/browser/112-batch-stale-1912-history.txt) |

## 文件交付、最终状态与验证

[原字节XLSX](hrm-payroll-prd04-full-regression-20261024/artifacts/snapshot1458-job1460.xlsx)来源为本机Downloads的“薪酬评审-快照1458-任务1460.xlsx”，未编辑。SHA-256 `53586124f316fdd8a992c7a22e56e9003210f9bf68414c10dac4537ba6f258e4`；绑定TEST快照1458、发布版本5、项目范围版本5、任务1460。文件内明确“TEST合成验证，非正式业务批准”。源文件签名、ZIP及结构读取已核对。

| 工作表 | 行数（含表头） | 列数 |
|---|---:|---:|
| 评审版本说明 | 787 | 8 |
| 功能需求清单 | 178 | 8 |
| 数据源清单 | 383 | 8 |
| 字段映射 | 620 | 8 |
| 问题与待办 | 326 | 8 |
| 范围与变更 | 2493 | 8 |

原生Calc六表逐张打开（94–101）：100%时存在超屏，75%时8列及中文、多行分片样例可读。这是当前文件六表可读样例的验收，不声称滚动覆盖全部字段或全部行、每个字段逐字对账、全部打印分页或所有历史文件均已验证。旧D-03当前文件版式/读性切片已完成。

最终1905为 **v15、REVIEWING、DEFERRED、来源READY**，人工实施记录`ACCEPTED`而`conclusionVerified:false`。人工引用记录不证明实际软件交付验收，来源质量恢复也没有自动恢复需求确认。快照1458的冻结状态与随后v15当前状态分开解释；不得把1905当前READY推回发布当时或宣称其当时已纳入基线。1911最终v2/reviewer279；1912最终v2/reviewerNULL，失败批次没有覆盖另一tab已保存版本。

本轮executor已有两阶段前端检查：治理改动默认Node堆上限曾OOM，退出134；增加8GB后的`vue-tsc --noEmit --pretty false --incremental false -p tsconfig.json`退出0。最后WB11修复使用 `NODE_OPTIONS=--max-old-space-size=8192 ./node_modules/.bin/vue-tsc --noEmit`退出0，`git diff --check`退出0。这些是不同次执行，不能描述为始终成功；历史测试总数不计入本轮新增数量。交付首次暂存412个文件后，`git diff --cached --check`因原始AX文本的尾随空白报警，退出非0；此前退出0的tracked diff检查只覆盖当时3个Vue差异，不能据此声称全暂存检查通过。封存原件为保持原字节及SHA未作格式化；源码及新报告排除raw证据后的空白检查另行核对，其结果以对应交付检查记录为准。

- [历史默认heap失败日志](hrm-payroll-prd04-full-regression-20261024/checks/governance-vuetsc-20261024.log)
- [治理8GB成功日志](hrm-payroll-prd04-full-regression-20261024/checks/governance-vuetsc-20261024-heap8g.log)；[治理diff日志](hrm-payroll-prd04-full-regression-20261024/checks/governance-diff-check-20261024.log)
- [最终WB11 typecheck日志](hrm-payroll-prd04-full-regression-20261024/checks/wb11-review-repair-vue-tsc-20261024.log)；[最终WB11 diff日志](hrm-payroll-prd04-full-regression-20261024/checks/wb11-review-repair-diff-check-20261024.log)

控制台在三个独立时点分别采样17 warn/0 error、22 warn/0 error、26 warn/0 error。原件分别为[final-sample](hrm-payroll-prd04-full-regression-20261024/browser/browser-console-final-sample.json)、[after-batch-fix](hrm-payroll-prd04-full-regression-20261024/browser/browser-console-after-batch-fix.json)、[end-of-regression](hrm-payroll-prd04-full-regression-20261024/browser/browser-console-end-of-regression.json)。均为当前本机VueRouter `next()`弃用采样，不能合并成累计无错或全会话无错误。D-07保持警告观察，未完成全框架/全部菜单警告清理，不阻断本次最低流程。

## 执行边界变化、历史问题与未纳入事项

原计划写明不在153写入、不增加成员授权；实际执行边界已改变。依据用户此前“允许全部授权，无限制”的后续授权，本轮仍限定TEST：154/155隔离项目、153新增TEST数据，既有独立TEST材料下登记/签署TEST roots并授予测试成员；不得宣称原计划边界完全未变。成员533已撤销且由121稳定展开失效理由；本轮三条新TEST roots1461、1464、1467仍ACTIVE以便复验；155范围v1及root1467的稳定页面回读见[128原件](hrm-payroll-prd04-full-regression-20261024/browser/128-project155-TEST-root1467-active.txt)及[128截图](hrm-payroll-prd04-full-regression-20261024/browser/128-project155-TEST-root1467-active.jpg)，未宣称环境已恢复到操作前权限状态。155模板v1/v2夹具使用SQL作合成测试资料准备，私有脚本/配置未提交；模板比较、决策、初始化回读来自页面，SQL不冒充UI动作。

D-01当前COL/RPT及治理文案已区分TEST受控发布/六表与正式批准，旧“S5待实现/发布六表留S5”缺口关闭（106和107当前页面及治理原件）。D-02仅关闭本轮已测字段引导/输入保留切片。D-03当前1458/1460文件六表结构与原生阅读已验。D-05本轮固定候选TEST组合流程已验；D-06实际加载/受限读取及撤权回读已验。D-04真实业务材料、正式批准及D-05正式AC-25/28组合另行安排，不能靠TEST签署关闭。D-07警告未关闭。

固定范围不包含全PRD/全部AC、性能/负载、全跨租户攻击/并发矩阵、真实外部来源、真实员工工资/税务/银行处理、正式业务授权/发布或生产部署；没有将这些未测事项列成11WB最低流程的FAIL/UNVERIFIED，也没有为它们补填PASS。

失败/中间现场原样保留：新05/06/08/09、23/26、32/36、58及旧024等不以中间或未成功画面作终态通过依据，完整过程按最终v2索引及稳定回读判断。63只是未确认的撤权步骤，不单独证明撤销；64/65和121是后续稳定回读。42、83、85、117为loading或中间态，不作通过依据，分别由84、86及118/119补足。122文件名误写project-history，实际是成员页，不作为项目历史证据；123才是实际打开的CREATE、METADATA、INITIALIZE 46/0/0、SCOPE与MEMBER历史。旧批量66/67暴露过失败对象定位缺口，最终以修复后108–112判断。旧交付QS3652/QC3656与当前QS3662/QC3664同时保留，不能把旧绑定冒充当前交付2绑定。

## 证据封存与报告关系

[最终交接manifest](hrm-payroll-prd04-full-regression-20261024/manifest-handoff.json)记录原始source路径、SHA-256、字节数及实际格式；证据为原字节复制，不修改原件。先前[manifest](hrm-payroll-prd04-full-regression-20261024/manifest.json)、[manifest-final](hrm-payroll-prd04-full-regression-20261024/manifest-final.json)、[manifest-complete](hrm-payroll-prd04-full-regression-20261024/manifest-complete.json)、[manifest-delivery](hrm-payroll-prd04-full-regression-20261024/manifest-delivery.json)及[manifest-finalized](hrm-payroll-prd04-full-regression-20261024/manifest-finalized.json)全部保留，后续清单仅扩展库存。复制和manifest校验属于证据整理，不产生新增业务验证。文本/console/日志复制前敏感模式扫描未发现口令或非TEST个人信息；未复制私有session、凭据或模板脚本。

[旧报告本次更新前全文副本](hrm-payroll-prd04-full-regression-20261024/report-before-final-update.md)已封存。旧报告仅顶部改为当前结果链接并把截至056的第五轮进展标历史，正文第1–17节及旧原件不改写。此前拒绝/历史报告保持原样。历史快照及全文副本保留原相对URL，未按副本所在目录迁移或重写链接，不保证这些历史引用在副本位置可解析，也不声明所有历史引用均已随本轮打包。

本次拟提交范围限于上述3个Vue、本修复报告、更新后的[完成报告](hrm-payroll-prd04-function-completion-audit-20261021.md)，以及同轮证据目录`hrm-payroll-prd04-browser-fifth-verification-20261023/`和`hrm-payroll-prd04-full-regression-20261024/`；其他此前未跟踪的无关文件保留在工作区，不纳入本次提交。本报告冻结时本轮未提交、未push；后续实际提交身份及是否推送以Git记录核对，不在报告中填写包含本报告自身的提交哈希。
