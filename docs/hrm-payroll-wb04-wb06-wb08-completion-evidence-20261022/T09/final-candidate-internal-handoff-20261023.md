# T09 最终候选准备与联合验收骨架（内部）

日期：2026-10-23，Asia/Shanghai。仅整理内部交接，不是最终 fix 报告，不代表 T01–T09 已全部完成。coder 期望 gpt-6.1-sol / medium，实际运行模型未核实。本轮只读计划、closure、交接、执行摘要及文本/身份原件；没有重新执行测试、浏览器、DB、Excel、打包、重启、提交，也没有对本轮 PNG 逐张目视复核。

## 1. 本轮范围及候选

以 [scope-note.md](scope-note.md) 的用户范围确认为准：本轮只关闭 **T09-A 本机 TEST 工程联合验收**。T09-B 正式业务批准与正式交付另行安排，不要求本轮补真实业务材料，不将 TEST 根/签署/决议换写为正式批准。任务分母保持9；最终同时列 T09-A 与 B 状态。

当前 HEAD `fdc503dca517b4dad4ea136f73bd9fbe92e11c59`。既有提交依次已覆盖 T01（beae42d9fe）、T02（67033a059b）、T05（75139359c4）、T03 日期修复（fdc503dca5）。不能再把这些已提交文件当成本次待提交差异，也不能仅凭 HEAD 推断实际运行包体。

本次读取时 `git diff --stat` 仅有4个已跟踪修改：既有 final fix 报告、ExportService、GovernanceServiceTest、治理 SFC。前者包含既有进展记录，**此次不修改、不将它提前写成最终完成报告**。大量其他未跟踪历史审计/菜单目录不属于本次自动纳入提交的对象。

当前需随最终必要提交包含的产品/测试精确文件为以下8个（以下路径均相对仓库根；完整 SHA、字节与精确证据文件路径在 [准备快照 manifest](final-candidate-preparation-manifest-20261023.json)）：

1. `yudao-module-hrm/src/main/java/cn/iocoder/yudao/module/hrm/service/payroll/HrmPayrollGovernanceExportService.java`
2. `yudao-module-hrm/src/test/java/cn/iocoder/yudao/module/hrm/service/payroll/HrmPayrollGovernanceServiceTest.java`
3. `yudao-ui/yudao-ui-admin-vue3/src/views/hrm/payroll/governance/index.vue`
4. `yudao-module-hrm/src/test/java/cn/iocoder/yudao/module/hrm/service/payroll/HrmPayrollT04IssueConsistencyTest.java`
5. `yudao-module-hrm/src/test/java/cn/iocoder/yudao/module/hrm/service/payroll/HrmPayrollT06ExportFixtureTest.java`
6. `yudao-module-hrm/src/test/java/cn/iocoder/yudao/module/hrm/service/payroll/HrmPayrollT07GovernanceConsistencyTest.java`
7. `yudao-module-hrm/src/test/java/cn/iocoder/yudao/module/hrm/service/payroll/HrmPayrollT08FrozenExportContractTest.java`
8. `script/hrm-payroll-governance-feedback.test.cjs`

关键完整 SHA：治理 SFC `663519c7dd73e8d001e2c2a911eebbf7e81d3c16b942c1bdca6850c7b016cdde`；ExportService `d244b492cd3e90ff1c03f8e255edcda9985abfccbad4991433492db259e8e4e0`；既有 GovernanceServiceTest `9374a3e8a29edbd9d9ad131cdf9e32fc13f45437481089ddfd5302f9ca2f499e`。其他5份 SHA 与各自 handoff 一致，已逐文件重算进 manifest。

最终文档提交至少需要在验收结束后更新 `docs/hrm-payroll-wb04-wb06-wb08-completion-fix-report-20261022.md`，以及保留本轮 T04/T06/T07/T08/T09 的必要交接、执行/闭包索引与其所引用证据。准备 manifest 的 `evidenceInventory` 已逐文件列出本次可见的353份证据（4个任务目录、T09 scope-note、shared-test-preflight），用于精确选取和后续完整性核对，**不是353份全部必须提交的结论，也不是 `git add` 授权清单**。原始日志若受 ignore 规则排除，可留私有本机并在已提交索引记录摘要、哈希及位置；不为凑清单强制加入凭据/运行配置，也不把仓库外路径误称已随 Git 交付。最终报告引用的仓库证据必须实际纳入相应提交，或明确为本机私有证据。

浏览器仍在新增原件，当前 manifest 是准备时点快照，不能称封存或最终完整清单。完成后另存最终 manifest，列出实际纳入路径/哈希、HEAD及差异哈希；不覆盖历史原件。也不对整个工作区 `git add .`。

## 2. 现有证据矩阵：供 executor 闭包核对

| 任务/计划要求 | 当前可引用证据 | 本次整理的判断及剩余收口 |
| --- | --- | --- |
| T01 / WB-04 条件、异议、恢复与三视口 | `T01/closure-review.md` 最后§7；34前端、86后端；最终范围预检拒绝/恢复；源模块响应与源码、运行 JAR 绑定 | 已有 closure 明确 technical acceptance close。沿用已通过的原件与边界，不重复制造相同 UI；最终候选只重验实际受影响连接处。 |
| T02 / 本人确认、重审、撤权、并发与租户对象边界 | `T02/closure-review.md`；22定向及包含它的161联合测试；020–025/030–041/042页面 | 已有 closure 明确关闭。161已含22，不能相加；真实H2/租户SQL不升级为生产MySQL验收。 |
| T03 / 问题全链、独立驳回/关闭、重开与改派 | `T03/closure-review.md`；39服务例、36日期前端、34确认回归、browser001–048 | 已有 closure 明确可闭合。保留旧 cycle2 日期归一化失败，不能把它计 PASS；依赖后续 raw 日期拒绝及 cycle3。 |
| T04 / 逾期、分类/影响、旧绑定与发布一致性 | `T04/automation-1/result-summary.md`：23/0/0/0；coder-handoff；browser001–027含独立分类、1845确认、3590周期与旧关闭绑定拒绝 | 服务定向已PASS；`browser-run`目前只总结早期001–008，晚近原件仍需 executor 按计划§7复核并另存最终 closure。页面当日逾期与自动化10-17固定日期必须分别列。 |
| T05 / COL/RPT 两处文案与对应入口 | `T05/closure-review.md`；demo.ts SHA abdf6ffd…；最终007/008与有权/受限角色001–006 | 已有 closure 在 D-01 有限范围关闭。没有必要重新改文案或扩大到菜单清理。 |
| T06 / 真实下载、六表原生及边界 | 1429真实下载/全字段 reconciliation；1441受限文件；`boundary-automation-1`1例PASS；native-current1429/native-boundary1429/native-restricted1441/native-current1454；TEST两夹具 | 主线程本轮交接确认已补长/空六表及受限/full边界，但早期 native-run/automation 摘要仍有“待验”时点文字。executor须按实际新原件另存 closure/身份，不覆盖旧摘要。两份合成夹具是生产导出器输出，不冒充浏览器下载/DB快照。 |
| T07 / 决议、门槛、幂等、并发 | `automation-1`31例PASS；`feedback-automation-1`15例PASS及静态；feedback-browser-review；browser016发布1437/017同ID重试/018冻结；1439下载身份 | 早期 browser-run 的未发布结论仅适用于当时；后续原件已存在，需 executor核对代表性拒绝→纠正→发布与同请求幂等、批准主体和冻结事实。012图文不一致保留披露，采用013最终状态与可得事件证明，不补造actor。 |
| T08 / 历史冻结、裁剪、撤权后下载与对象替换 | `automation-1`16例PASS；1437→1439/1440历史重导身份；1441受限结构核对；browser001–010；root1442撤销、旧job1445拒绝、独立267 job1447下载 | 完整服务矩阵已通过；页面有对应片段，但本轮仅读文本/身份，不能代executor判PNG闭包。至少将1439/1440同账号全字段重建比较与实际旧领取拒绝的业务响应核对落盘，禁止仅因不同XLSX字节或浏览器点击判断成功/失败。 |
| T09-A / 最终同候选工程链 | scope-note、本交接及准备manifest | 仍待候选/运行制品最终绑定、受影响联合复核、连接处原件及独立读回复核；本文件不判PASS。 |
| T09-B / 正式业务 | scope-note用户范围override | 另行安排，本轮未验收。不是TEST工程阻塞，也不写正式可交付。 |

## 3. 最少还需收口的链路

1. **封定同一候选与运行身份。** 主线程停止新增源码修改后，executor核对8份源/测试及T01/T03/T05相关现有文件未漂移，记录HEAD、相关差异哈希、Java/Node工具身份、后端JAR SHA及实际PID/启动/打开路径。前端如使用Vite开发态，保存当前实际页面响应体及source map sourcesContent与磁盘源码一致性，尤其治理SFC663519…；不得把GET200、文件名或完整构建成功单独当已加载证明。按计划记录最终前端构建身份；若使用构建产物则另绑定实际加载资源。缺项只补身份，不重做业务数据。
2. **一轮受影响联合自动化。** 已通过单类不必继续无限重复；最终候选尚缺同时覆盖新导出/治理反馈与旧确认/问题连接处的一轮明确记录。executor串行运行下列选定HRM类，并运行既有前端确认、问题日期与治理反馈脚本及受影响静态检查；保存命令/退出码/Surefire实际计数和前后SHA。已有同候选的等价联合记录可复用，不为形式重复跑。源/候选不变且通过后停止扩大测试。
3. **复用现成UI链，仅补断开的连接。** 以project153的既有1844/1845、issue3531/3590、决议1407/1433、release1437为对象索引，把“异议→本人确认→来源/映射变更重审→问题独立驳回/关闭/重开→TEST独立批准→发布→1439下载→当前变更后的1440历史重导→1442撤权旧1445拒绝→267自身1447成功”连成逐步表。允许多个已标识的需求承担不同角色，不要求新建一套工程或对同一对象破坏既有历史。每段记录actor/版本/结果/刷新或历史独立回读。缺的是哪一段原件就只补该段；不因部分早期文档仍写待验而重复创建/批准/发布。
4. **一次 executor 最终闭包复核。** T04/T06/T07/T08新原件与结构结果核对后，各自追加 closure 或统一T09矩阵，保留失败和时点限制；最终矩阵中注明service层、浏览器、原生Excel各自证明什么。D-07仅说明当前是否阻断三个WB，未阻断则保留遗留，不追加菜单修复任务。全部完成后再由主线程形成最终fix报告与决定必要提交。

联合测试建议命令（仅供 executor，本文未执行；不连接业务MySQL）：

```bash
JAVA_HOME=/Users/waitforpro/.local/jdks/java8/Contents/Home /opt/homebrew/bin/mvn -B -ntp -pl yudao-module-hrm -am -Dtest=HrmPayrollRequirementServiceImplTest,HrmPayrollRequirementProjectServiceTest,HrmPayrollWorkbenchServiceTest,HrmPayrollReviewServiceTest,HrmPayrollGovernanceServiceTest,HrmPayrollT02ConfirmationContractTest,HrmPayrollT03IssueContractTest,HrmPayrollT04IssueConsistencyTest,HrmPayrollT06ExportFixtureTest,HrmPayrollT07GovernanceConsistencyTest,HrmPayrollT08FrozenExportContractTest -Dsurefire.failIfNoSpecifiedTests=false test
```

测试类名按当前源码存在性先核对；不把预期总数写成实际执行数。启动明确请求executor gpt-6-luna/max，实际运行端无证据仍写未核实；quota/模型不可用报告阻塞，不替代模型后沿用原名称。构建/重启须由协调主线程在业务浏览器串行边界安排，避免将待验候选切到一半。

## 4. 最终AC矩阵模板

| AC映射 | 工程断言/业务场景 | 固定源码与运行制品 | 服务测试（命令/实例/退出码） | 可见UI原件及actor/版本 | 下载/原生文件SHA | 结果及边界 |
| --- | --- | --- | --- | --- | --- | --- |
| AC-06/07/12/13：WB-04表单子集 | T01条件、豁免、异议、失败恢复 | 待最终身份引用 | 引T01及最终联合 | 引T01最终闭包，受影响连接补充 | 不适用 | 不外推完整AC |
| AC-08/18；AC-09/14/19/26确认子集 | T02本人确认/旧元组/撤权/事务 | 待最终身份引用 | 引T02及最终联合 | 引T02已闭包原件与T09连接表 | 不适用 | 生产MySQL并发未由H2证明 |
| AC-21 | T03完整问题周期/独立复核 | 待最终身份引用 | T03 39及最终联合 | 引cycle3/旧失败保留 | 不适用 | 精确限定问题对象范围 |
| AC-22 | T04日期/类别/影响/绑定/发布一致性 | 待最终身份引用 | 23例及最终联合 | 当日UI与固定日期分别列 | 不适用 | 不将数据未就绪统一当发布阻断 |
| D-01 | T05 COL/RPT及导航 | demo.ts已提交身份 | 已有静态 | 007/008及有权/受限 | 不适用 | 仅D-01两处 |
| AC-16/D-03 | T06六表完整内容/原生阅读 | d244…及运行JAR | 结构对账、1夹具及联合 | 实际下载原件 | 1429/1441/1454及TEST夹具分别列 | 夹具来源不混同 |
| AC-25；AC-28当前/未来有效性部分 | T07有效依据/发布/重试/并发 | SFC663519…与后端身份 | 31治理+15反馈及联合 | 1433批准、拒绝纠正、1437幂等 | 1439 | TEST工程批准 |
| AC-19/20/28历史裁剪部分；AC-14本WB对象子集 | T08历史/裁剪/撤权/对象替换 | 同一最终身份 | 16例及联合 | 1442/1445/1447、受限147 | 1439/1440比较及1441/1447 | 不外推AC-14整体/PRD28项 |
| T09-A | 三WB连接流程与独立回读 | 最终身份总表 | 最终实际计数 | 连接表逐段索引 | 对应原件身份 | 待验收后填写 |
| T09-B | 正式业务批准及交付 | 本轮范围外 | 未执行 | 未执行 | 未执行 | 另行安排 |

最终不得用各任务PASS数量简单相加产生新的不重复测试总数；联合161已含T02，前端34在多轮重复也只按实际命令报告。没有新增明确失败时，不追加功能、负载、全菜单或正式材料任务。
