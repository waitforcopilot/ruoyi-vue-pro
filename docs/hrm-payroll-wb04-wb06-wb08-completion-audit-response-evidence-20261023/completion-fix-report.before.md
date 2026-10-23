# WB-04、WB-06、WB-08补齐fix报告

最终更新：2026-10-23（Asia/Shanghai）。依据[固定9项任务清单](hrm-payroll-wb04-wb06-wb08-completion-task-plan-20261022.md)及用户最新范围确认。本报告是修复与工程验收汇总，不是Cursor独立审计。

## 1. 当前结论与统计口径

**本轮获准的本机TEST工程任务完成9/9：T01～T08已验收关闭，T09-A工程联合验收通过。T09-B正式业务批准及交付另行安排，本轮未验收。** 按原计划“A+B均通过”口径，整体关闭为8/9，T09整体保留“待外部材料”；它不阻塞用户本轮限定的工程交接。9为固定工作包数，未因A/B拆分扩大分母。

工程实现、当前候选验收和正式交付分别判断：三个WB在本报告列明的TEST子集具备实现与工程验收依据；正式项目范围、真实供数/映射、有效批准和生产验收没有因此获得通过。不宣称PRD全部28个AC通过，不包含AC-24负载、其他WB或生产发布。

阶段报告曾记录1/9及T02未关闭，全文原样封存在 [定稿前阶段报告原件](hrm-payroll-wb04-wb06-wb08-completion-evidence-20261022/T09/pre-final-fix-report-20261023.md)，包含本次编辑前未提交的第11/12节。该副本中的“待补/进行中”是历史状态；当前状态以本报告及最终复核为准。历史失败、过渡截图和原件文件名均保留。

## 2. 逐任务编码、结果与证据

下表“通过”均限本机TEST工程范围。浏览器由主线程操作用户可见IAB；executor按请求Luna/Max执行自动化并复核原始AX/PNG。服务层负例与页面正负例分层取证，未以接口或SQL冒充页面操作。

| 任务 | 编码/补齐结果 | 当前验收结论与主要依据 | 原件索引及边界 |
|---|---|---|---|
| T01 WB-04条件与恢复 | 不适用理由与服务端规则对齐；补字段提示、具体缺映射提示；授权检查后增加范围预检。草稿保持可不完整 | 已验收关闭；86项阶段服务回归、34项前端检查；缺字段/异议、有效不适用、适用切换、范围失配与恢复、390/768/1440表单恢复、刷新/重登 | [T01闭包复核](hrm-payroll-wb04-wb06-wb08-completion-evidence-20261022/T01/closure-review.md)；仅表单子集与TEST项目153；同账号双tab早期冲突不冒充独立登录 |
| T02 WB-04本人确认/并发 | 无新增产品修改；新增22例确认权限、语义失效与事务契约测试 | 已验收关闭；阶段联合161项已含22项；两个origin独立UI登录账号266，v38冲突→人工合并v40→刷新/重登；267撤权、279有效接替本人确认，旧签字保留 | [T02闭包复核](hrm-payroll-wb04-wb06-wb08-completion-evidence-20261022/T02/closure-review.md)；同账号独立会话，不是不同账号并发；H2事务不等于生产MySQL |
| T03 WB-06流转 | 保留原始日期串并严格校验，拒绝组件自动归一化的非法日期；新增39例契约测试、36项前端日期检查 | 已验收关闭；问题3531创建/处理/独立驳回/关闭/重开，cycle3补正后闭环；269失效拒绝、266接替、279独立关闭 | [T03闭包复核](hrm-payroll-wb04-wb06-wb08-completion-evidence-20261022/T03/closure-review.md)；cycle2日期归一化是历史失败，修复后另存负例和cycle3正例 |
| T04 WB-06分类/绑定 | 无新增产品修改；新增23例逾期、分类阻断、当前范围/来源/映射绑定测试 | 已验收关闭；全项目基线阻断、分类依据和当前绑定页面检查；mapping2旧绑定在mapping3后拒绝且输入保留 | [T04闭包索引](hrm-payroll-wb04-wb06-wb08-completion-evidence-20261022/T04/closure-index-20261023.md)；021实际为CLOSED成功，不是拒绝；026拒绝，027仍REVIEW；关闭DATA_READINESS不证明供数完成 |
| T05 WB-08文案 | COL/RPT两处“S5”占位改成实际独立治理/TEST受控发布及快照入口说明 | 已验收关闭；有权/受限角色入口及最终格式候选页面复验 | [T05闭包复核](hrm-payroll-wb04-wb06-wb08-completion-evidence-20261022/T05/closure-review.md)；仅D-01两处文案/导航，不以该轮文案证明发布或下载 |
| T06 WB-08六表 | 纳入起始即存在的ExportService/GovernanceServiceTest差异：固定八列、父记录+明细分片、完整长文本/类型/引用、行高/打印布局；新增真实exporter边界夹具测试 | 已验收关闭；可见IAB job1454实际下载及独立HTTP200，Excel六表打开；126记录/1130字段精确重建；长文本/空表/受限节选原生阅读与打印预览另有原件 | [T06闭包索引](hrm-payroll-wb04-wb06-wb08-completion-evidence-20261022/T06/closure-index-20261023.md)；旧原生文件与最终自动化新fixture身份分开，未声称逐单元格肉眼检查或实际打印 |
| T07 WB-08决议/发布 | 新增31例治理一致性测试；治理SFC在14个动作/读取入口清除旧成功消息，补15项反馈回归 | 已验收关闭；独立合成批准、旧依据/请求阻断、发布1437/1452及幂等；最终1455 v4冻结v12并实际下载1457，六表精确重建 | [T07闭包索引](hrm-payroll-wb04-wb06-wb08-completion-evidence-20261022/T07/closure-index-20261023.md)；012为过渡不一致，采用013持久有效状态及后续事件；1426仍待批准，不把拒绝写为批准 |
| T08 WB-08冻结/撤权 | 无新增产品修改；新增16例冻结投影、权限收缩、各阶段撤权、跨项目/租户与并发契约 | 已验收关闭；历史1437重导业务内容不漂移；受限投影及节选；root1442撤销后旧job1445领取/新任务创建均拒绝，有效267自己的job1447成功 | [T08闭包索引](hrm-payroll-wb04-wb06-wb08-completion-evidence-20261022/T08/closure-index-20261023.md)；只覆盖列明TEST账号/项目/对象，不泛化全部生产权限矩阵 |
| T09-A三WB连接 | 最终固定候选，无新增业务实现；补联合回归、当前页面链及冻结/下载全字段对账 | 已验收关闭（本轮范围）；最终271后端+85前端检查通过；工程连接链见第5节，下载件195902 bytes/474字段一致 | [最终执行者复核](hrm-payroll-wb04-wb06-wb08-completion-evidence-20261022/T09/final-automation-2/final-review-complete.md)；工程A通过，正式B另行安排、本轮未验收 |

T01已提交`beae42d9fe0fbc61e2eab63ffeba0cafd90dfbb5`；T02为`67033a059b`；T05为`75139359c4`；T03为`fdc503dca517b4dad4ea136f73bd9fbe92e11c59`。本报告对应的T04/T06/T07/T08/T09材料及8个必要产品/测试文件尚待主线程按精确清单提交，本文不把本地文件存在写成Git交付完成。

## 3. 最终候选与运行身份

最终测试候选为HEAD `fdc503dca517b4dad4ea136f73bd9fbe92e11c59` 加列明工作区差异与新测试。[最终执行前后身份](hrm-payroll-wb04-wb06-wb08-completion-evidence-20261022/T09/final-automation-2/candidate-identity-before.json)记录20个源文件/测试/脚本SHA，最终执行前后全部一致；当时tracked diff SHA为`5a634e02acecda6f0f3c131359f76e0b6395ebbd722a91ddad1fc081bcb215a3`。该差异身份包含当时阶段报告，不能与本次文档定稿后的整树diff混用。产品/测试候选未因本文编辑而变化。

- 后端PID55351，路径`yudao-server/target/yudao-server.jar`，JAR SHA-256 `044a0625b5410cb8be5b086625ba60554b49f789402b5ff5423f46567cd850e5`。exporter与ProjectionService的target class分别与同JAR内对应class字节一致，且进程指向该JAR路径，最终package后JAR未改变。证据是进程路径/包体及相关class对应，没有dump JVM全部已加载类。见 [运行制品身份](hrm-payroll-wb04-wb06-wb08-completion-evidence-20261022/T09/final-automation-2/runtime-artifact-identity.json) 与 [相关class比对](hrm-payroll-wb04-wb06-wb08-completion-evidence-20261022/T09/final-automation-2/projection-class-byte-check.json)。
- 前端为现有Vite dev server PID86218/端口5173，实际治理模块GET200，121003 bytes、SHA `e0ac7667cb0d3fc481cdf57d421b0ece781a6ed03f2bf9514714c80b06d68279`；这是开发服务实际模块响应，与独立Vite构建产物分别记载。T01另保存浏览器实际加载源码/sourceMap绑定，最终相关源文件未改变。不能把build成功单独当运行身份。
- 最终精确文件SHA与证据清单见 [最终候选/证据manifest](hrm-payroll-wb04-wb06-wb08-completion-evidence-20261022/T09/final-delivery-manifest-20261023.json)。起始ExportService/治理测试差异原先未提交，现经相关T06/T08及联合测试复核后列入交接；不归称本轮新发现后重写的全部代码。

coder期望/请求`gpt-6.1-sol / medium`，executor期望/请求`gpt-6-luna / max`，实际运行模型均未核实。主线程可见浏览器分工来自用户长期授权，参见[角色约定](role-model-policy.md)。没有Cursor/Grok独立审计执行证据，不将内部复核改称独立审计。

## 4. 自动化、构建与失败保留

最终一次联合运行采用JDK8u492、Maven3.9.16。[最终自动化摘要](hrm-payroll-wb04-wb06-wb08-completion-evidence-20261022/T09/final-automation-2/final-automation-summary.md)、[Surefire逐类统计](hrm-payroll-wb04-wb06-wb08-completion-evidence-20261022/T09/final-automation-2/surefire-reports/index.json)保存命令、原始stdout/stderr、退出码与逐类XML。统计采用最终单轮，不与T01的86、T02的161或早期重复执行相加。

| 检查 | 结果 | 解释 |
|---|---|---|
| 11个payroll服务/契约类 | 271 tests，0 failure/error/skipped，exit0 | Requirement36、Project26、Workbench31、Review19、Governance27、T02 22、T03 39、T04 23、T06 1、T07 31、T08 16 |
| 前端三脚本 | 34确认+36日期+15治理反馈=85，通过，exit0 | 行为/组件绑定检查；不替代页面、真实服务权限与数据库 |
| Prettier/ESLint/Stylelint | exit0 | 最终指定相关文件检查 |
| Maven package / Vite build --mode dev | 均exit0 | 打包通过；实际运行制品另按第3节绑定 |
| 额外手工SFC harness首次尝试 | exit1，保留原日志 | Node直接require未解析compiler-sfc；T07改用项目插件依赖解析后compileScript通过，最终Vite build及运行模块转换也通过，不归为产品失败 |

T03早期非法日期归一化、早期格式失败、T07过渡截图不一致均保留；当前通过依据为后续固定候选复验。非通过的历史尝试没有删除或累计为通过用例。

## 5. 三WB联合时间线与最终冻结下载

这是T01～T08串行完成后，在未改变最终业务候选上补验连接处的工程链；确定性并发/恶意参数由对应服务测试承担。详细分层原件及executor最终复核见第2节，不把所有历史动作说成在同一时刻重做。

| 时间线连接 | 当前证据及实际结果 |
|---|---|
| 提出/异议/恢复 | T01/T02需求1844的表单、独立会话冲突恢复；T09/003缺异议责任人/下一步时拒绝且保留输入；006历史证明需求1845 DISPUTED v10→REVIEWING v11，责任/下一步由266保存。004刷新主列表为REVIEWING v11，不能称其截图显示DISPUTED v10 |
| 本人确认与重审 | T01/T02适用性、范围及语义变更失效与旧签字保留；T04/T07来源映射变更触发当前依赖检查。T09/007是过渡，008独立历史才证明279本人CONFIRMED v12，scope5/semantic8/source3578 semantic1/relation3583 mapping3 |
| 问题完整生命周期 | T03/007–048：问题3531创建/分派、处理、独立驳回/补正/关闭、重开cycle3、失效处理人拒绝和接替；T04/020–027补当前映射版本拒绝与稳定回读 |
| TEST决议与发布 | T07/013及事件证明补充决议1433 EFFECTIVE、1407保留、1426待批准；014/015拒绝且无残留成功消息。T07/023–028旧mapping2请求阻断、新mapping3发布1452 v3并幂等；T09/009–011再发布1455 v4并冻结同一需求v12/签署元组 |
| 实际下载/历史重导/撤权 | T09/012真实IAB事件落盘job1457；T07/T08历史1437新任务重导业务对象一致；T08/007–010根撤销、旧任务及新任务拒绝、有效267自有任务成功 |

最终下载证据：[1457下载身份](hrm-payroll-wb04-wb06-wb08-completion-evidence-20261022/T09/job1457-download-identity.json) 与 [1457原始XLSX](hrm-payroll-wb04-wb06-wb08-completion-evidence-20261022/T09/release1455-job1457-browser.xlsx)。task1457/release1455、actor266，195902 bytes，SHA-256 `823c2b41b9f187086bc291409276b2220ea9556ac8c0cf9343ce2a0ed75bc3eb`。**本次1457的HTTP状态未独立捕获**；可见下载事件、实际文件及数据库JOB所存字节完全相同分别构成证据，不能借T06/job1454的HTTP200补写1457状态。

[完整冻结字段对账](hrm-payroll-wb04-wb06-wb08-completion-evidence-20261022/T09/final-automation-2/release1455-job1457-full-field-reconciliation.json)只读读取同一冻结release/job，并由完整snapshot按全项目授权投影重建六表，与浏览器XLSX逐记录/逐字段比较。仅排除任务独有EXPORT-METADATA父记录及其明细；85条业务记录、474字段，0缺失/多余/不匹配；数据库只读一致性快照后回滚，无写入。

| 工作表 | 业务记录 | 字段 | 结果 |
|---|---:|---:|---|
| 评审版本说明 | 33 | 164 | 精确相等 |
| 功能需求清单 | 2 | 52 | 精确相等 |
| 数据源清单 | 3 | 33 | 精确相等 |
| 字段映射 | 2 | 22 | 精确相等 |
| 问题与待办 | 43 | 175 | 精确相等 |
| 范围与变更 | 2 | 28 | 精确相等 |

T06/job1454是另一份既有TEST release1384下载件，具有自己的HTTP200、Excel六表原生查看及126记录/1130字段对账，不能与上述85/474混计。原生长/空边界文件旧SHA分别为`6e3c5ead…adecbaad`、`632eb7f…e06fe16`，实际完整身份见 [原生与新fixture身份对照](hrm-payroll-wb04-wb06-wb08-completion-evidence-20261022/T09/final-automation-2/T06-fixture-identity-comparison.json)；最终自动化新fixture长文本SHA `0e90e7fea8f3a2cb0071f57474c294b8f961c054d30c725b066b34b3223976cc`，空表SHA `f6d8005dcaed6347bbaf4da8669b694e8d7143397bd8eb78e59d5d141be1a178`。新fixture没有冒充旧原生打开文件。原生导航/打印预览未实际打印，关闭未保存，保留磁盘原件身份。

## 6. AC与遗留项覆盖矩阵

“通过”仅指计划指定的TEST验收范围；完整AC与子集不能混淆。本计划没有重新评定整份PRD。

| PRD/缺口 | 对应任务 | 本轮结果与界限 |
|---|---|---|
| AC-06/07/12/13 | T01 | WB-04表单条件、异议及恢复子集通过，三视口仅此表单 |
| AC-08/18；AC-09/14/19/26 | T02 | 本人确认及重审要求通过；后四项仅需求确认、权限/并发子集，不关闭完整AC |
| AC-21 | T03 | 计划约定问题全链及相关身份/状态负例通过 |
| AC-22 | T04 | 计划约定逾期、分类阻断、绑定及发布交叉断言通过；DATA_READINESS与真实供数区分 |
| D-01 | T05 | COL/RPT两处说明及导航通过 |
| AC-16 / D-03 | T06 | 约定TEST六表、长文本/空表/受限节选的结构与原生可读性通过；非所有文件/Excel版本视觉保证 |
| AC-25；AC-28当前/未来有效性 / D-04 | T07 | TEST决议、发布门槛、幂等与确定性并发子集通过；正式批准另列 |
| AC-19/20/28历史裁剪；AC-14对象矩阵 / D-05 | T08 | 三WB计划内TEST历史/权限/任务阶段鉴权通过；不能泛化完整AC或生产租户配置 |
| 三WB工程连接 | T09-A | 当前最终候选联合链通过，最终冻结/下载85记录474字段精确相等 |
| 正式业务批准/交付 | T09-B | 另行安排，本轮未验收；原计划T09整体仍待外部材料 |
| AC-24、其他WB、其他未列AC | 本轮外 | 未执行，不计入通过 |
| D-07菜单/路由警告 | 遗留 | 当前记录VueRouter next()、ElementPlus ElLink underline弃用warning，未见console error，未阻断已验证三WB操作；未声称产品无warning |

## 7. 剩余责任、交接与提交边界

正式B需要真实有权业务方另行提供/确认：实际项目与组织人员范围、强制Q/条款、受控授权根及岗位动作矩阵、批准主体/依据版本/有效期，以及正式授权下基线发布验证。业务负责人/批准方负责材料和有效批准，工程方在另行授权范围内验证正式发布。当前未核实真实材料、审批完成或责任人姓名，不制造TEST批准替代物，不包括真实发薪、银行发送或报税。

本轮开发与验收在本机合成TEST范围内完成；T06/T08有对既有历史TEST项目105/147的读取、重导和权限负例，不把它们描述为生产工资操作，也不以其替代项目153的最终联合快照。

[精确提交allowlist](hrm-payroll-wb04-wb06-wb08-completion-evidence-20261022/T09/final-submission-allowlist-20261023.json)列出本次必要8个产品/测试文件、报告/计划、T04/06/07/08/T09证据和共享准备原件，精确区分普通与被ignore但必须保留的日志。它是主线程核对提交范围的文件清单，本报告没有执行stage/commit/push；最终提交号和远端结果须以之后实际Git输出为准。排除其他历史未跟踪报告、生成target/dist、本机私有配置与凭据，不从旧preparation manifest的353条目录快照推导提交授权。

[证据敏感值扫描](hrm-payroll-wb04-wb06-wb08-completion-evidence-20261022/T09/final-automation-2/evidence-credential-scan-summary.json)记录executor对615个文本的模式扫描：literal credential/token/cookie候选0，唯一代码引用读取仓库外配置键；该结果不是OCR或全格式隐私审计。最终文档仅作引用/路径/哈希/差异检查；本次定稿未重跑测试、操作浏览器、写数据库、重启服务或修改产品/测试。
