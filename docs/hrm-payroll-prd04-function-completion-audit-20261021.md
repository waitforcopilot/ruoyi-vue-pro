# PRD v0.4：功能完成情况与多角色浏览器核验报告

> **当前结果（2026-10-24）：固定计划§4的11项本机TEST最低WB流程已全部验完（11 PASS / 0 FAIL / 0 UNVERIFIED）。** 详见[最终修复与回归报告](hrm-payroll-prd04-full-regression-fix-report-20261024.md)。此结论不等于全部PRD、全部AC、正式批准或真实业务上线；此前截至056的进展及第1–17节均为历史。更新前全文见[封存副本](hrm-payroll-prd04-full-regression-20261024/report-before-final-update.md)。

> **历史：截至056的第五轮中期进展（2026-10-23～24，Asia/Shanghai；以下状态不代表最终结果）。** 本轮只确认下表明确列出的子项；不沿用旧轮“10项完成、1项部分完成”作为当前总数，也不宣称11个WB或完整PRD全部通过。第1～16节完整保留为历史记录，其中“当前”“最新”“本轮”均指各旧轮当时，不能覆盖本节第五轮状态。

## 历史第五轮中期进展：可见浏览器证据截至056

本节根据[本轮检查矩阵](hrm-payroll-prd04-browser-fifth-verification-20261023/verification-matrix-plan.md)与[原始AX/截图目录](hrm-payroll-prd04-browser-fifth-verification-20261023/browser/)更新。下表编号对应该目录同前缀 `.txt` AX及 `.png`命名的截图（053含0～4）。028～056截图实际为JPEG/JFIF、可解码，扩展名误标；原件不重命名或转换，不能声称PNG签名通过。证据是主线程保存的可见内嵌浏览器操作；本次coder仅更新文档，没有重新执行浏览器、自动化测试、构建或业务写入。PASS仅限列明子项，失败现场和未覆盖项分别保留，不能按功能行换算整体完成率。

### 候选、角色与证据边界

- 源候选HEAD：`925ac220b3b09c973f9b64797bd35cf4a66a2c9b`；准备时所列源码无tracked diff，身份见[preflight-identity.json](hrm-payroll-prd04-browser-fifth-verification-20261023/preflight-identity.json)。该身份不证明浏览器运行包与源码逐字节一致；本节未补做运行包身份验证。
- 更新前报告384行，SHA-256 `1c0ae7f296fb3d019c0254ff7e43875208c0de4d15a8b2a7b399d472e3f601fc`，保留[逐字节副本](hrm-payroll-prd04-browser-fifth-verification-20261023/hrm-payroll-prd04-function-completion-audit-20261021.before.md)。PRD仍为待业务确认稿，正式业务验收未完成。
- 浏览器操作者：主线程；已保存266编辑、267独立核验、279分派本人/问题复核的页面证据。279的页面确认能力不等于编辑、决议读取或发布权限；268本轮尚未完成切换验证，旧轮268结果不计本轮。
- 执行者期望/请求为 `gpt-6-luna / max`，实际运行模型未核实。[executor-resume-review.md](hrm-payroll-prd04-browser-fifth-verification-20261023/executor-resume-review.md)只复核到001～027；028～056为后续主线程原件，尚待executor续核。该文对1905“未封存”的描述是当时状态，后续证据029～048已补充，不能继续当作最新状态。
- 新建隔离TEST项目154后，页面范围显示undefined；本轮未修订业务范围或成员授权。后续操作转到既有TEST153（范围版本5），且新增1905、3613、3616等TEST记录。因此本轮实际发生153写入，偏离计划“不得在153写入”的原边界，不能宣称全部步骤都在新建隔离项目154内完成；不删除或回写原计划。
- 本节仅记录TEST合成资料；没有新批准/发布、没有实际导出文件下载、没有提交或push。历史发布1455在本轮只读；旧轮测试通过数、XLSX对账和正式批准材料不计本轮新证据。

### 逐WB已证实与未覆盖

| 功能 | 本轮证据支持的子项/现场 | 尚未覆盖或不能判通过 |
|---|---|---|
| WB-01 项目、范围、成员 | 266新建154、名称与历史回读，页面范围尚未定义；153范围版本5可读。266/267/279角色页面分别可见。001～003、029、035、045、051 | **范围修订、成员增撤/撤权、跨租户、268受限读取未执行。** undefined范围造成后续操作阻断，不能把创建成功等同范围可用。 |
| WB-02 初始化与升级 | 154空集合0，初始化后46、再次初始化及刷新后46；模板比较界面可读。004～007 | 46总量及重复后总量不变有页面回读；全46唯一编号逐页审计、升级差异采纳及升级后人工意见/确认保留未完成，不能把无待比较项当升级通过。 |
| WB-03 上下文征集 | 154十视图分别保存1892～1901，另COL流程1902、字段1903、操作1904，刷新/列表及历史回读；[13条索引](hrm-payroll-prd04-browser-fifth-verification-20261023/opinions-created-13.json)。操作入口返回URL带`anchor=operation-simulate`。008～024 | 模块/字段/操作/流程样例保存与持久化已证实；返回URL锚点不证明精准滚动，网络失败恢复和十视图×全部入口组合未执行。46候选不是46项真实薪酬实现。 |
| WB-04 完善、异议、本人确认 | 153新增1905完整TEST规格；279本人初次确认未改变状态，038明确缺映射拒绝；建立关系并完成独立语义核验后，279确认1905成功至v7，数据仍未就绪。029、035～038、042、045、047～048 | 本人确认正向及缺映射拒绝子项已证实。**DISPUTED异议、异议下一步/触发场景字段负例、语义改动后确认失效/复审、非本人确认及双窗口并发矩阵尚未完成。** |
| WB-05 来源、关系、质量 | 266尝试1905关系使用`employee_id`失败；读取真实来源3578规格`employee_key`后，关系3613（1905→3578，映射v1）建立。267独立登记关系语义证据3623 PASS。040～042、045、047 | 仅该关系及语义核验正向已证实；046 QC尝试后047无新增QC证据，**不得记QC成功**。本轮未新建来源、未跑多源/多对多/复制、QS/QC正向及交付/语义变更失配恢复。056仍显示3613不可用；来源3578为既有TEST来源。 |
| WB-06 问题闭环 | 新问题3616由266负责、279复核；043创建，044处理START并SUBMIT到待复核；279首次CLOSE出现冲突，刷新后050稳定显示已关闭。039只读既有3531历史。 | 新问题处理、提交及不同人关闭子项已证实；049保留失败现场，不能记首次CLOSE成功。**驳回REJECT、再次处理/提交、重开与新期限、改派/逾期和自己关闭负例尚未执行。** 问题关闭不代表来源可用或需求自动确认。 |
| WB-07 统计 | 154空分母显示“暂无适用项（0/0）”，意见逐条增加到59；153在054显示U3/E3/I1/C2/T1/D0，功能确认2/3、来源可用0/2。055 C集合为1845、1905；056 L为3583、3613，两条均`available:false`，与2条不可用关系一致。004～006、008～023、029、054～056 | 上述计数、C成员和L关系下钻一致的子项通过；**全部分母、移除/恢复/异议组合、固定S0～S4和受限统计泄露矩阵未完成。** 不从C2推出两条来源就绪，也不把未决问题数等同本轮3616仍未关闭。 |
| WB-08 决议、发布、六表 | 279治理页显示无决议读取/发布能力；只读历史1455“权限受限节选”。052选中评审版本说明，053-0～4逐张选中功能需求清单、数据源清单、字段映射、问题与待办、范围与变更；受限内容有裁剪提示。051～053 | 只证明该角色历史受限投影及六页签读取；**本轮新建/独立批准决议、新TEST发布/重试、实际下载文件、文件结构/对账、原生视觉、撤权领取均未执行。** 不是本轮六表导出成功，更非正式业务批准。 |
| WB-09 响应式 | 390宽流程1902、768宽字段1903、1440宽操作1904分别保存，018/020/022稳定清单、019/021/023对应历史留档。 | 三种宽度样例保存/历史可读有证据；AX本身不证明像素尺寸，尺寸按主线程本次视口设置记录。现有来源选择与全部页面/弹窗/横滑适配矩阵未完成，不能判全响应式验收通过。 |
| WB-10 实施关联 | 154的1904保存实施及会议失败并保留输入；executor截至027指出请求`projectScopeVersion`为空。153的1905在030缺多项引用尝试失败，补齐page/api/task/evidence后保存ACCEPTED至v2，032历史显示四引用及`conclusionVerified:false`；当时功能仍PENDING。025～032 | 完整TEST人工声明保存且与功能确认独立的子项已证实；**030同时缺页面、接口与验收引用，不是隔离空evidence的负例。** 025未提交画面不证明拒绝，026/027亦混有空scope/api；只缺evidence及只缺api的独立负例仍待补测。人工ACCEPTED不代表真实实现验收。 |
| WB-11 协作、分派、版本 | 1905会议v3、分派279成功v4、站内提醒v5；032打开v2/v3历史比较，035中279可见提醒而编辑/实施按钮禁用。154的1904会议028失败且保留输入。032～035、028 | 单项分派、会议/提醒及版本历史子项已证实；多项混合成功失败、显式选择任意历史对比、提醒撤销/到期及撤权后不可见未执行。028空scope失败未修源码，不能记为协作正向完成。 |

### 仍开放的阻断与续核范围

154范围版本未定义导致实施/会议请求空值问题仍未修复；本节没有将其关闭。1905虽确认且有人工ACCEPTED记录，关系3613仍不可用；QS/QC正向链仍无本轮完整证据。3616只跑到独立关闭，未执行矩阵要求的驳回和重开。WB-08停留在历史受限读取，没有新批准、发布或文件交付。WB-01范围/成员、WB-02升级、WB-04异议/复审及WB-05多源仍是明确缺项。

在028～056完成executor原件复核之前，上述结论属于coder基于已保存页面原件的进展归纳；未新增测试计数，未冒充Luna执行，也未替代Cursor独立审计。**第五轮仍进行中，完整PRD验收结论保持未完成。**

---

## 以下为历史记录（原第1～16节；不计第五轮新增通过）

> **2026-10-22 可见主线程续核（历史人工摘要，无落盘页面原件）：** [续核记录](hrm-payroll-prd04-visible-browser-reverification-20261022/README.md)记载当次在可见主标签页切换266→267→268→266，并自报运行上下文为 `gpt-6-luna / max`；该目录没有可复核的运行日志、AX、DOM、截图或HAR，因此第16节只按自我记录引用，不与第三轮封存原件同级。此前隐藏子线程操作不追认为可见执行；第三轮正向闭环不归入这次续核。

更新日期：2026-10-22（Asia/Shanghai）。仓库：`/Users/waitforpro/source/github-yudao/ruoyi-vue-pro`。**最新的逐WB完整操作表仍是第三轮，见第3.0/14节；最近一次用户可见主线程续核见第16节**。第三轮引用经第15节独立审计处理收窄；第3.1/10/12/13节保留前两轮历史。代码实现、每轮运行核验与正式业务验收分别记载。

## 1. 结论、范围与判定规则

**当前代码功能口径为11项中10项已完成、1项部分完成（WB-08）、0项未开始；不是11项浏览器完整验收通过。** 原8/3/0是旧候选的判定。第三轮核对后续修复并实际验证WB-04字段引导、WB-06重开期限引导及其正向闭环，关闭这两行原有的已知交互缺口。WB-08已输出8列六表，但原生视觉及正式材料边界仍开放。逐行通过范围和未覆盖分支见第3.0节，不将实现数量换算为验收完成率。第三轮登录切换266→267→268→266有封存原件；第16节同顺序切换仅为后续人工摘要自述，不能合并为一次执行。完整PRD未正式验收。

- **已完成**：该行列明的代码功能已接通前端/服务/持久化或读取路径，有针对性测试或既有运行证据支持；不表示本轮重新跑过所有页面，也不代表正式业务签署或生产验收。
- **部分完成**：已有核心实现，但存在与PRD交互/交付要求不符的已知缺口，或正式闭环材料未完成；每行列明剩余项。
- **未开始**：没有发现对应功能实现。11个WB功能中既有代码审计未发现此类；不能因缺验收而声称从未开发。

PRD第2.3节把需求征集工作台、正式基线、真实薪酬实施分阶段管理。**十视图及46个候选的展示/征集完成，不等于46项真实薪酬业务已全部实现。** 原代码审计及第三轮逐一核对工作台契约；对真实工资业务只引用现有明确通过/断点，不对未审查的全部生产分支给“已完成”结论。

## 2. 候选与实际动作

### 2.0 第三轮完整操作候选（2026-10-21～22；其后的可见续核见第16节）

| 对象 | 实际身份及边界 |
|---|---|
| HEAD / 分支 | `4ddc3c6643408cc4434b5c845499ee6fb6f9df32` / `work` |
| 当前源码 | 相比旧7189候选已有WB-04/06等修复；本轮开始已有ExportService及GovernanceServiceTest两份跟踪改动，未在本轮修改。13个关注文件起止哈希见第三轮candidate-start/end.json，不等于全仓库逐字节审计 |
| 本机包体 | target JAR SHA256 `1f425b993c23fdc4521517c706e17e80e8734db8bc57f21207a2e62d5ae7f1ab`，起止不变；不据此声称重新逐类核验运行进程与磁盘包体 |
| 实际浏览器 | 本机5173界面操作，真实退出/登录既有编辑266、独立复核267、受限268并恢复266；未直接SQL/API写入、未扩大成员权限 |
| TEST数据 | 新项目152；新意见1827～1839；105内1780确认到v14、3441交付D3、3450可用、3418第五轮关闭v24；TEST基线1388/v8、实际下载任务1390 |
| 自动验证 | 本轮只对实际下载XLSX执行只读结构及6条记录82字段对账；没有重跑135/138项后端测试或整个AC矩阵 |
| 证据 | [第三轮索引](hrm-payroll-prd04-browser-third-verification-20261021/README.md)、更新前报告副本、起止候选及manifest。原封存包不改写，本轮未提交/push |

### 2.1 原代码审计及前两轮候选（历史）

下表保持历史身份，不代表最新HEAD或包体：

| 对象 | 本轮身份/动作 |
|---|---|
| PRD | [v0.4原文](hrm-payroll-requirements-workbench-prd-20261017.md)，SHA256 `7350322dd7d729275d2e424cecc04961f3c672edd86e29043a7502151523e985`；状态仍是待业务确认产品评审稿 |
| 分支 / HEAD | `work` / `7189b6a94ade8b081eef4361fdc87d84a679e376`；已跟踪业务源码无差异 |
| 包体 | 本机target JAR SHA256 `90b6d03f62cb03724f8729c1d581790da2f16ed2672b2d246e9aeb294ebcb036`；只记录磁盘包体，本轮未重新做进程加载类逐字节核对 |
| 直接代码检查 | Project/Access/Requirement/Workbench/Review、Root/Decision/Release/Projection/Export、Controller/DTO、Vue页面和迁移；身份见[candidate.json](hrm-payroll-prd04-function-audit-evidence-20261021/candidate.json) |
| 原代码审计自动测试（此次未重跑） | 6组135项，失败0、错误0、跳过0，BUILD SUCCESS；[用例与计数](hrm-payroll-prd04-function-audit-evidence-20261021/tests-summary.json)。使用H2及mock协作对象，部分旧阶段测试隔离治理依赖，不冒充真实HTTP、权限中间件或组合验收 |
| 首轮浏览器补验（历史） | 实际操作266/267/268三个账号；TEST项目150有一次46项计数回读，随后十视图保存1708～1717、三视口保存1718～1720，终态59项；没有留存两次初始化各自计数，不能据此判浏览器幂等回归通过。项目105的1707双角色确认/重审，来源3441/关系3450四层核验，问题3418两轮闭环；固定草稿1377及下载任务1379实际领取XLSX。详见3.1及第10节 |
| 第二轮浏览器复验（历史） | 主标签页实际退出/登录266→267→268→266；151空项目0→46→46有不同AX留档。150仅有一份59条DOM内容（02/03/04同哈希），不能当三次独立回读；新增13条后终态72。1707语义重审及267确认到v15、人工实施验收缺证据负例及补齐正例；3441交付D2/QS/QC失效恢复；3418第三轮独立关闭；TEST草稿1380同请求号重试及1382实际下载。见3.1/12及第二轮证据索引 |
| 历史证据 | 原目录155文件重新按原manifest核验，0不符；[结果](hrm-payroll-prd04-function-audit-evidence-20261021/historical-integrity.json)。仅证明文件完整，不将历史操作改称本轮执行 |
| 操作边界 | 此次通过本机页面写入TEST合成资料并保留记录。创建项目自动产生的创建人权限之外未扩大成员授权。未修改业务源码、菜单、迁移或真实薪资；未正式发布或外发，未提交或push。按用户要求更新本报告，原副本保留 |

## 3. 按业务使用顺序的功能总表（固定11项）

### 3.0 第三轮多角色浏览器完整操作表（其后的可见续核见第16节）

本轮证据目录：[第三轮索引](hrm-payroll-prd04-browser-third-verification-20261021/README.md)。以下编号均指该目录内同前缀DOM及PNG；**通过只限列出的操作**。第三轮封存README与记录中的错误引用由第15节及本表替代，不改写原包。字段引导修复属于当前实现，正式业务批准未完成。所有新写入均明确TEST，不操作真实工资。

| 功能 | 本轮实际操作与结果 | 通过边界 / 未覆盖 |
|---|---|---|
| WB-01 项目、范围、成员 | 266新建152，补创建上限后成功、改名回读；初始业务范围未定义，不能确认。只读105既有成员授权；实际切换267、268再恢复266。268评审显示“无项目访问或操作权限”，治理未加载受控内容 | 创建/改名/角色切换/上述受限读已验；创建上限不等于业务范围修订。本轮未增撤成员、修订范围或跑跨租户。05/06、29、38～41、46 |
| WB-02 初始化与升级 | 152空集合0，其后有一份46条DOM，07与08两次采样的DOM同哈希，C0；模板比较无待比较项；新增13条后59 | 空集合→46及新增草稿持久化已验；本轮重复初始化浏览器幂等未取得独立证明，不判通过。06/07/08、15、47 |
| WB-03 上下文征集 | 十视图各保存操作意见1827～1836；COL流程1837、字段1838、模块1839；13条分两页回读，1836及1837历史实际打开 | 四类入口样例保存/持久化通过；执行过“返回原位置”点击，但未据此判精准滚动或全入口矩阵通过；网络恢复未验。09～17 |
| WB-04 完善、异议、本人确认 | 1780起始合并正文v9独立回读；266改为DISPUTED v10，会议/提醒后v13；267本人确认至v14并实际打开历史。缺触发场景、缺异议下一步均显示具体字段、保留输入；取消后重新读取仍已确认v14 | 本轮已验字段引导、异议保存、独立确认及无效保存不写入；并发双窗口冲突未本轮重演，v9只为本轮新回读，不倒填旧证据。01、18、22、30/31、32b、48/49/51 |
| WB-05 来源、关系、质量 | 3441交付D2→D3（版本1/4/3）后旧QS失配；267登记新QS、新QC；最终问题关闭后关系3450显示已核验可用 | 同一对象交付替换、人工QS/QC和问题条件分开已验；35尚未可用，最终只用35b证明可用。未新建来源/映射、未重演语义变更、多对多/复制或真实文件核验。23/24、33/34、35b |
| WB-06 问题闭环 | 3418缺新期限显示字段提示并保留输入；补2026-11-05，第五轮OPEN→PROCESSING→REVIEW，由267独立关闭v24；实际打开对象历史含21～24、最后actor267 | 期限负例、处理/提交/独立关闭及真实历史通过；未重演REJECT、REASSIGN、逾期、自己关闭负例。25/26/28、36/37（PROCESSING取37事件，27列表仍待处理） |
| WB-07 统计 | 152 U/E由0到46（07/08同一份DOM只计一次），最后全项目59/筛选13、C0；105的1780异议时C0/D1、确认后C1/D0，T1保留 | 上述集合/筛选分母与确认独立通过；未重建固定S0～S4及全部受限统计矩阵。06～09、15/16、18、30/31、51 |
| WB-08 决议、发布、六表 | 使用既有TEST有效决议1370；预检查齐备后发布TEST需求基线1388/v8，同号重试后最终仍8个版本；1390 READY并实际下载。六页签实际逐张选中，每表展开1条冻结记录；XLSX六表8列，6条/82字段对账相等 | 仅TEST发布、重试、六投影/下载及样例字段一致性通过；本轮未新建/独立批准决议、正式发布、撤权下载、历史重导比对或原生六表视觉验收。42、43b、45、sheet-*-stable、53/54及xlsx-verification.json |
| WB-09 响应式 | 390×844流程1837、768×1024字段1838、1440×1000模块1839实际保存，最终列表回读；FLOW历史在恢复默认视口后打开 | 三种尺寸的样例表单保存通过；不声称三种尺寸均打开历史或全弹窗/全视图适配通过。12～17 |
| WB-10 实施关联 | 1780人工ACCEPTED且仅证据留空时，“保存独立操作”按钮禁用，其他已填字段仍在；20采样已填证据但显示通用失败句；20b补齐后稳定保存v11，功能仍DISPUTED/C0，后267独立确认 | 空证据禁用保存、后续稳定保存及状态独立已验；19b没有“输入已保留”错误句，20的通用失败不能归因为空证据。TEST引用真实性未核验。19b、20、20b、31/51 |
| WB-11 协作、分派与版本 | 单选1780经批量入口返回success；22未读出分派目标ID，不能证明该请求目标为267；267看到TEST提醒、最终本人确认及v13/v14差异另由30/32b证明 | 单项批量成功、纪要、267收提醒与本人确认、默认比较已验；不将后续角色事实倒填为22的目标参数。未跑多项混合成败或提醒到期/撤销。21/22、30、32b |



### 3.1 第二轮多角色浏览器逐项复验（历史，最新见3.0）

本轮重新操作，不沿用首轮PASS。证据位于[第二轮复验索引](hrm-payroll-prd04-browser-reverification-20261021/README.md)；表中编号对应该目录，`通过`仅限所列步骤。未覆盖不等于未开发，也不构成完整AC验收通过。首轮细节保留在第10节及本轮前报告副本。

| 功能 | 本轮实际步骤与结果 | 判定及尚未覆盖 |
|---|---|---|
| WB-01 项目、范围与成员 | 150改名并回读；266新建151，页面显示“范围尚未定义，不能确认”；通过真实退出/登录切换267、268，再恢复266；268的105评审/治理内容未加载 | **创建、改名、切换及页面前置条件已验证；完整单元未验完。** 没有业务范围修订、成员增撤或创建人确认权限负例；通用加载提示不证明具体拒绝原因。证据05/06、34、45～48、55 |
| WB-02 模板初始化与升级 | 新项目151由56/57/58三份不同AX记录空集合0→首次46→再次46，C仍0；150仅认一次59条DOM，原13条经67/68回读保留，另新增13条后终态72；模板比较无待比较项 | **151初始化及重复执行、150人工意见保留通过。** 150的02/03/04文本同哈希，不能证明三次独立59条计数；步骤文件名不能替代执行证据。升级差异采纳未验证。证据07、56～60、67/68；02～04只按一份DOM引用 |
| WB-03 模块/字段/操作/流程意见 | 十视图分别新增1721～1730操作意见；COL流程1731、字段1732、模块1733分别保存，最终13条完整回读；历史含FLOW/FIELD/MODULE及页面锚点 | **所列四类入口、保存及持久化通过。** 本轮未执行返回原位置和网络失败恢复；不是十视图×四类入口全组合。证据view-*、08～15、59/60、66 |
| WB-04 完善、本人确认与重审 | 266改1707业务目标，v10确认失效为v11评审中；分派267；来源恢复后267实际确认v14到v15，历史操作人为267 | **语义修订失效、分派及独立确认通过。** 功能DISPUTED、并发旧版本、所有缺字段负例未跑。证据16/17、22、41～44、61 |
| WB-05 来源、关系与质量 | 266将3441交付从D1替换为D2，版本1/3/2，旧QS待检查；267登记新QS，3450仍待质量核验；267再登记新QC，最终关系已核验可用 | **同一来源/关系的交付变更失效与独立QS/QC恢复通过。** 未新建来源/映射、未重跑来源语义/关系语义核验、多对多、跨项目复制及全部语义变化矩阵。均为TEST人工引用。稳定证据33、35～38、62/63 |
| WB-06 问题闭环 | 3418缺新期限重开被阻断并保留输入；补2026-11-03后OPEN，266开始/提交，267独立关闭；最终v14/CLOSED/cycle3 | **重开负例、正例及新一轮独立关闭通过；通用错误提示缺口仍在。** 本轮没有再演练驳回、重新分派、逾期或自己复核关闭负例。证据26、29～31、39/40、64/65 |
| WB-07 统计 | 151显示0→46→46、C0、本期0/0暂无适用项；150最终筛选13/全项目72；105的1707修订后C0，确认后C1，T1、来源1/1，状态独立 | **所列实时集合、筛选/全项目分母和确认变化通过。** 固定S0～S4、所有受限集合及问题分类矩阵未重建。证据17、19、41、56～61 |
| WB-08 决议、草稿及导出 | 当前预检查仍因858/1359确认不足阻断正式基线；新建TEST固定草稿1380，同请求号重试后仍仅1380；六个冻结页签实际逐张选中；下载任务1382 READY且实际领取XLSX | **预检查阻断、草稿同号重试、六投影与实际下载通过；整体仍部分完成。** 新决议独立批准、正式发布、撤权后领取、历史内容不变比较及XLSX六表原生视觉验收未做。结构仍有345列宽表。证据50～54、sheet-*、xlsx-structure.json |
| WB-09 响应式 | 390×844流程1731、768×1024字段1732、1440×1000模块1733均保存、列表回读及历史；11的DOM已有历史对话框、版本1及操作人266，66另补实时AX/截图和FLOW锚点；结束恢复默认视口 | **三种宽度的样例保存/历史通过。** 不是所有十视图和全部弹窗交叉验收。证据09～15、59/60、66；11不是“未打开历史”，66仍为更完整补证，不扩大通过范围 |
| WB-10 实施关联 | 1707选择“验收通过（人工记录）”且清空证据，保存被阻断并保留输入；补TEST引用后可保存至v12，功能仍评审中/C0，后由267确认v15 | **缺证据阻断、补齐保存和状态独立通过；错误仍通用。** 仅合成人工状态登记，不是软件/真实业务已验收，也未核实外部引用或全状态转移。证据18/19、61 |
| WB-11 分派、纪要、提醒与比较 | 单选1707经批量入口分派267返回success；保存本轮会议、提醒，267页面读到提醒；历史选择v10/v11比较，最终历史含266/267 | **所列单项分派、协作及版本比较通过。** 多项混合成败、提醒权限到期/撤销未跑。证据20～22、41、43/44 |

### 3.2 当前代码功能判定及原定位

表中的“完成”采用第1节的实现口径；WB-04/06依据后续修复与第三轮操作更新，其他行保留原代码审计依据。本表受影响代码链接由第15节重新定位，符号及逐行文本核对见第三轮引用处理的[code-link-checks.json](hrm-payroll-prd04-round3-audit-response-evidence-20261022/code-link-checks.json)；完整浏览器分支的未覆盖项见3.0。

| PRD编号 | 用户可以做什么 | 完成情况 | 当前代码依据 | 验证依据及剩余边界 |
|---|---|---|---|---|
| WB-01 | 建立评审项目、确定范围并分派成员 | 已完成 | [创建项目](../yudao-module-hrm/src/main/java/cn/iocoder/yudao/module/hrm/service/payroll/HrmPayrollRequirementProjectService.java#L73)、[范围修订](../yudao-module-hrm/src/main/java/cn/iocoder/yudao/module/hrm/service/payroll/HrmPayrollRequirementProjectService.java#L119)、[成员授权](../yudao-module-hrm/src/main/java/cn/iocoder/yudao/module/hrm/service/payroll/HrmPayrollRequirementProjectService.java#L198)、[对象动作授权](../yudao-module-hrm/src/main/java/cn/iocoder/yudao/module/hrm/service/payroll/HrmPayrollRequirementAccessService.java#L73) | 范围/元数据版本分开；动作与对象范围逐项检查，创建人不自动有确认/发布权。Project 26项测试；正式材料另列WB-08。 |
| WB-02 | 装入46项候选、重复初始化与模板升级 | 已完成 | [项目初始化](../yudao-module-hrm/src/main/java/cn/iocoder/yudao/module/hrm/service/payroll/HrmPayrollRequirementProjectService.java#L154)、[模板采纳](../yudao-module-hrm/src/main/java/cn/iocoder/yudao/module/hrm/service/payroll/HrmPayrollRequirementProjectService.java#L174) | SQL46项与原43＋COL3身份核对通过；项目内幂等、升级显式采纳、人工及移除记录保留。 |
| WB-03 | 在模块、字段、操作和流程旁提意见 | 已完成 | [上下文意见入口](../yudao-ui/yudao-ui-admin-vue3/src/views/hrm/payroll/review/index.vue#L872)、[保存及版本校验](../yudao-module-hrm/src/main/java/cn/iocoder/yudao/module/hrm/service/payroll/HrmPayrollRequirementServiceImpl.java#L129) | 项目/模块/page/anchor持久化；第三轮十视图操作意见1827～1836及COL流程/字段/模块1837～1839已补验，见第3.0/14节；其他入口组合与失败恢复未全重演。第3.1节只保留前两轮历史。 |
| WB-04 | 补齐需求、分派本人确认、异议与重新评审 | 已完成（实现口径） | [本人确认](../yudao-module-hrm/src/main/java/cn/iocoder/yudao/module/hrm/service/payroll/HrmPayrollRequirementServiceImpl.java#L99)、[确认元组](../yudao-module-hrm/src/main/java/cn/iocoder/yudao/module/hrm/service/payroll/HrmPayrollWorkbenchService.java#L206)、[字段提示及输入保留](../yudao-ui/yudao-ui-admin-vue3/src/views/hrm/payroll/review/index.vue#L912) | 核心状态、本人及版本约束已有；后续validation.ts补字段引导，第三轮48/49明确指出缺触发场景、异议下一步并保留输入，51确认无无效写入。D-02原WB-04缺口关闭；全字段/并发浏览器矩阵未全重跑，正式材料见D-04。 |
| WB-05 | 登记来源、多对多映射、核验和变更重审 | 已完成 | [实际来源](../yudao-module-hrm/src/main/java/cn/iocoder/yudao/module/hrm/service/payroll/HrmPayrollWorkbenchService.java#L105)、[多对多映射](../yudao-module-hrm/src/main/java/cn/iocoder/yudao/module/hrm/service/payroll/HrmPayrollWorkbenchService.java#L127)、[四类核验登记](../yudao-module-hrm/src/main/java/cn/iocoder/yudao/module/hrm/service/payroll/HrmPayrollWorkbenchService.java#L162)、[可用条件](../yudao-module-hrm/src/main/java/cn/iocoder/yudao/module/hrm/service/payroll/HrmPayrollWorkbenchService.java#L176) | 供给/语义/QS/QC分别绑定版本，变更使证据失配；复制不继承可用结论。为人工登记核验，并非已接通真实来源自动检查。 |
| WB-06 | 问题分派、处理、独立复核、关闭和重开 | 已完成（实现口径） | [问题闭环](../yudao-module-hrm/src/main/java/cn/iocoder/yudao/module/hrm/service/payroll/HrmPayrollWorkbenchService.java#L230)、[问题表单失败处理](../yudao-ui/yudao-ui-admin-vue3/src/views/hrm/payroll/requirement/WorkbenchPanel.vue#L669) | 状态机/独立复核/期限/阻断已有；后续issueValidation.ts补指引，第三轮25缺期限明确阻断并保留输入，26～28、36/37补齐后独立关闭及真实历史已验。D-02原WB-06缺口关闭；其余状态分支未全重跑。 |
| WB-07 | 查看范围、确认进度、来源就绪及问题统计 | 已完成 | [统计集合](../yudao-module-hrm/src/main/java/cn/iocoder/yudao/module/hrm/service/payroll/HrmPayrollReviewService.java#L132) | U/E/I/C/T/D/L分开；0/0不标100%，筛选不改变全项目发布门槛。Review 17项测试包含固定S0～S4及受限集合。 |
| WB-08 | 按业务决议发布范围版本并导出历史六表 | 部分完成 | [发布门槛](../yudao-module-hrm/src/main/java/cn/iocoder/yudao/module/hrm/service/payroll/HrmPayrollGovernanceReleaseService.java#L72)、[冻结发布](../yudao-module-hrm/src/main/java/cn/iocoder/yudao/module/hrm/service/payroll/HrmPayrollGovernanceReleaseService.java#L110)、[六表权限投影](../yudao-module-hrm/src/main/java/cn/iocoder/yudao/module/hrm/service/payroll/HrmPayrollGovernanceProjectionService.java#L32)、[领取再鉴权](../yudao-module-hrm/src/main/java/cn/iocoder/yudao/module/hrm/service/payroll/HrmPayrollGovernanceExportService.java#L53) | 发布/不可变快照/六表/当前鉴权已有实现及TEST链；当前8列导出候选已下载并做结构/样例对账，原生可读性未验、正式授权未完成（D-03/04/05）。 |
| WB-09 | 桌面、平板、手机查看、编辑和查历史 | 已完成 | [移动布局](../yudao-ui/yudao-ui-admin-vue3/src/views/hrm/payroll/review/index.vue#L1181)、[十视图页面](../yudao-ui/yudao-ui-admin-vue3/src/views/hrm/payroll/review/index.vue#L31) | 响应式宽表/弹窗已有实现；S6保留390/768/1440的保存及历史证据。此状态限这些交互能力；第三轮已补做三视口样例保存，FLOW历史在恢复默认视口后打开；不把三种尺寸都打开历史记为通过，不保证全部表单无可用性缺陷。 |
| WB-10 | 关联实现页面、接口、开发任务和验收证据 | 已完成 | [实施记录](../yudao-module-hrm/src/main/java/cn/iocoder/yudao/module/hrm/service/payroll/HrmPayrollReviewService.java#L79) | 实施状态独立记录；ACCEPTED要求证据，conclusionVerified=false，不把手工登记当自动审计通过。 |
| WB-11 | 批量分派、会议纪要、站内提醒和版本比较 | 已完成 | [逐项批量分派](../yudao-module-hrm/src/main/java/cn/iocoder/yudao/module/hrm/service/payroll/HrmPayrollReviewService.java#L93)、[会议和站内提醒](../yudao-module-hrm/src/main/java/cn/iocoder/yudao/module/hrm/service/payroll/HrmPayrollReviewService.java#L87)、[版本差异](../yudao-ui/yudao-ui-admin-vue3/src/views/hrm/payroll/review/index.vue#L534) | 逐项授权/版本检查、失败逐项返回；纪要及提醒有历史，只站内不外发；前端可比较两个历史版本。 |

## 4. 十个业务视图分别提供什么

下表保留展示功能原代码判定；首轮见10.2、第二轮见12，最新第三轮1827～1839见3.0/14。

以下状态限定为PRD第4～5节的**评审展示及征集功能**。对应真实薪酬能力的验收边界见第8节。十页共用同一征集、来源、统计和历史组件，不把共用组件重复计成十份完成率。

| 视图 / 候选点 | 当前展示与征集内容 | 代码完成情况 | 依据与缺口 |
|---|---|---|---|
| 薪酬总览 / OV-01～04 | 应发/实发/单位成本、趋势、真实评审统计分开 | 已完成 | [OV展示](../yudao-ui/yudao-ui-admin-vue3/src/views/hrm/payroll/review/demo.ts#L71)；[OV逐项场景](../yudao-ui/yudao-ui-admin-vue3/src/views/hrm/payroll/review/demo.ts#L248)；通用征集与错误反馈余项沿用第3节，不据此宣称所有用例验收 |
| 薪酬方案 / PLAN-01～04 | 工资结构、薪档示例、演示调薪与V1/V2对比 | 已完成 | [PLAN展示](../yudao-ui/yudao-ui-admin-vue3/src/views/hrm/payroll/review/demo.ts#L85)；[PLAN逐项场景](../yudao-ui/yudao-ui-admin-vue3/src/views/hrm/payroll/review/demo.ts#L258)；通用征集与错误反馈余项沿用第3节，不据此宣称所有用例验收 |
| 工资核算 / CALC-01～05 | 准备→试算→复核→发放/归档讨论、部分失败例外 | 已完成 | [CALC展示](../yudao-ui/yudao-ui-admin-vue3/src/views/hrm/payroll/review/demo.ts#L101)；[CALC逐项场景](../yudao-ui/yudao-ui-admin-vue3/src/views/hrm/payroll/review/demo.ts#L272)；通用征集与错误反馈余项沿用第3节，不据此宣称所有用例验收 |
| 个税与工资条 / TAX-01～05 | 虚构累计税、扣除、工资条预览、奖金及申报字段讨论 | 已完成 | [TAX展示](../yudao-ui/yudao-ui-admin-vue3/src/views/hrm/payroll/review/demo.ts#L114)；[TAX逐项场景](../yudao-ui/yudao-ui-admin-vue3/src/views/hrm/payroll/review/demo.ts#L283)；通用征集与错误反馈余项沿用第3节，不据此宣称所有用例验收 |
| 考勤 / ATT-01～05 | 日/月归集、假单与缺卡/迟到等正常和异常场景 | 已完成 | [ATT展示](../yudao-ui/yudao-ui-admin-vue3/src/views/hrm/payroll/review/demo.ts#L140)；[ATT逐项场景](../yudao-ui/yudao-ui-admin-vue3/src/views/hrm/payroll/review/demo.ts#L306)；通用征集与错误反馈余项沿用第3节，不据此宣称所有用例验收 |
| 加班 / OT-01～05 | 申请/打卡/认可时长、倍率、调休与合规问题 | 已完成 | [OT展示](../yudao-ui/yudao-ui-admin-vue3/src/views/hrm/payroll/review/demo.ts#L155)；[OT逐项场景](../yudao-ui/yudao-ui-admin-vue3/src/views/hrm/payroll/review/demo.ts#L317)；通用征集与错误反馈余项沿用第3节，不据此宣称所有用例验收 |
| 工时 / HOUR-01～05 | 三类制度、周期/排班与工资联动讨论 | 已完成 | [HOUR展示](../yudao-ui/yudao-ui-admin-vue3/src/views/hrm/payroll/review/demo.ts#L179)；[HOUR逐项场景](../yudao-ui/yudao-ui-admin-vue3/src/views/hrm/payroll/review/demo.ts#L328)；通用征集与错误反馈余项沿用第3节，不据此宣称所有用例验收 |
| 社保公积金 / INS-01～05 | 城市基数比例、个人扣款/单位成本及应缴实缴例外 | 已完成 | [INS展示](../yudao-ui/yudao-ui-admin-vue3/src/views/hrm/payroll/review/demo.ts#L195)；[INS逐项场景](../yudao-ui/yudao-ui-admin-vue3/src/views/hrm/payroll/review/demo.ts#L339)；通用征集与错误反馈余项沿用第3节，不据此宣称所有用例验收 |
| 报表分析 / RPT-01～05 | 成本分摊、分母/人均/营收比、预算差异及结构图 | 已完成 | [RPT展示](../yudao-ui/yudao-ui-admin-vue3/src/views/hrm/payroll/review/demo.ts#L210)；[RPT逐项场景](../yudao-ui/yudao-ui-admin-vue3/src/views/hrm/payroll/review/demo.ts#L346)；通用征集与错误反馈余项沿用第3节，不据此宣称所有用例验收 |
| 需求征集总览 / COL-01～03 | 评审、范围、实施、来源、问题与输出版本分开 | 部分完成 | [COL展示](../yudao-ui/yudao-ui-admin-vue3/src/views/hrm/payroll/review/demo.ts#L227)；[COL逐项场景](../yudao-ui/yudao-ui-admin-vue3/src/views/hrm/payroll/review/demo.ts#L357)；D-01页面证据为第三轮51稳定DOM，源码为demo.ts第236行；RPT第223行亦有S5占位注释，第16节没有重新记录这两句文案。导出余项另列WB-08 |

示例采用同一虚构员工，[隔离示例计算](../yudao-ui/yudao-ui-admin-vue3/src/views/hrm/payroll/review/demo.ts#L29)独立计算：应发11750、个人扣款1400、个税130.50、实发10219.50、单位缴费2100、单位成本13850。只验证示例内部关系，不将演示税率当现行政策。`demo.ts`没有正式业务API导入；页面的simulate/复核/工资条预览改本地状态，真实意见保存使用需求API。

候选身份核对：SQL46个唯一编号＝原43个＋COL-01～03；[机器可读结果](hrm-payroll-prd04-function-audit-evidence-20261021/template-coverage.json)。这只证明模板身份覆盖，**不证明正式业务确认、真实接入或生产实现**。

## 5. 原代码审计关键子功能核对（历史依据，不是新增工作包）

| 业务时间线 | 逐项检查及当前逻辑 | 代码依据 / 本轮测试覆盖 |
|---|---|---|
| 发起与准备 | 双项目同原始编号不同记录ID；项目内CUSTOM单调序列；新增模板只补缺、语义更新显式采纳 | [项目初始化](../yudao-module-hrm/src/main/java/cn/iocoder/yudao/module/hrm/service/payroll/HrmPayrollRequirementProjectService.java#L154)、[模板采纳](../yudao-module-hrm/src/main/java/cn/iocoder/yudao/module/hrm/service/payroll/HrmPayrollRequirementProjectService.java#L174)；Project初始化/模板测试 |
| 授权后协作 | 平台权限＋当前有效成员＋动作＋模块/对象/组织期间范围；不能借技术管理员代签；授权委派不超上限，撤销后重新读取检查 | [对象动作授权](../yudao-module-hrm/src/main/java/cn/iocoder/yudao/module/hrm/service/payroll/HrmPayrollRequirementAccessService.java#L73)、[成员授权](../yudao-module-hrm/src/main/java/cn/iocoder/yudao/module/hrm/service/payroll/HrmPayrollRequirementProjectService.java#L198)；Project/Permission测试；并非本轮重新跑租户HTTP负例 |
| 修订范围 | 主体/期间修订递增scopeVersion，重审需求并令SEM/QS/QC绑定失配；仅名称改metadataVersion；显式子集失配不静默缩小 | [范围修订](../yudao-module-hrm/src/main/java/cn/iocoder/yudao/module/hrm/service/payroll/HrmPayrollRequirementProjectService.java#L119)、[范围核验失效](../yudao-module-hrm/src/main/java/cn/iocoder/yudao/module/hrm/service/payroll/HrmPayrollWorkbenchService.java#L217)；Project及Workbench范围/改名测试 |
| 提出意见 | 模块、角色、场景、规则、单位、精度、正常/例外及page/anchor持久化；刷新/返回由路由保留项目及锚点 | [上下文意见入口](../yudao-ui/yudao-ui-admin-vue3/src/views/hrm/payroll/review/index.vue#L872)、[保存及版本校验](../yudao-module-hrm/src/main/java/cn/iocoder/yudao/module/hrm/service/payroll/HrmPayrollRequirementServiceImpl.java#L129)；首轮十视图1708～1717；第二轮新增1721～1730（第二轮59），其他上下文1731～1733（第二轮60），原1708～1720保留见第二轮67/68；其余入口组合未全重演 |
| 分派与确认 | 一般保存不能提交新CONFIRMED；分派目标需有CONFIRM资格且不可自派；只有当前分派本人可确认保存版本 | [本人确认](../yudao-module-hrm/src/main/java/cn/iocoder/yudao/module/hrm/service/payroll/HrmPayrollRequirementServiceImpl.java#L99)、[保存及版本校验](../yudao-module-hrm/src/main/java/cn/iocoder/yudao/module/hrm/service/payroll/HrmPayrollRequirementServiceImpl.java#L129)；Requirement 31项与Project相关测试 |
| 需求不适用来源 | 有效具体理由豁免来源和映射；切换清签、关系失活留历史；无关系的适用项计缺口 | [确认元组](../yudao-module-hrm/src/main/java/cn/iocoder/yudao/module/hrm/service/payroll/HrmPayrollWorkbenchService.java#L206)、[适用性切换](../yudao-module-hrm/src/main/java/cn/iocoder/yudao/module/hrm/service/payroll/HrmPayrollWorkbenchService.java#L214)；Workbench/Requirement测试 |
| 来源与映射 | 来源多对多；名称/联系人运营更新与契约语义变化分开；关系映射只失效本关系；显式跨项目复制产生独立来源 | [实际来源](../yudao-module-hrm/src/main/java/cn/iocoder/yudao/module/hrm/service/payroll/HrmPayrollWorkbenchService.java#L105)、[独立副本](../yudao-module-hrm/src/main/java/cn/iocoder/yudao/module/hrm/service/payroll/HrmPayrollWorkbenchService.java#L118)、[多对多映射](../yudao-module-hrm/src/main/java/cn/iocoder/yudao/module/hrm/service/payroll/HrmPayrollWorkbenchService.java#L127)；Workbench 25项含多对多、副本、负责人交接 |
| 供给与质量 | 供给PROVIDED不等于READY；来源/关系语义、QS、QC独立；QS绑定规格/交付/范围，QC引用QS和映射/消费规则 | [四类核验登记](../yudao-module-hrm/src/main/java/cn/iocoder/yudao/module/hrm/service/payroll/HrmPayrollWorkbenchService.java#L162)、[可用条件](../yudao-module-hrm/src/main/java/cn/iocoder/yudao/module/hrm/service/payroll/HrmPayrollWorkbenchService.java#L176)；同D1改单位恢复、局部映射影响测试；不声称自动校验真实外部文件 |
| 功能重审 | 确认元组绑定范围、需求语义、source语义、relation映射及规则；来源语义修改重审所有当前关联需求；供给变化不代确认 | [确认元组](../yudao-module-hrm/src/main/java/cn/iocoder/yudao/module/hrm/service/payroll/HrmPayrollWorkbenchService.java#L206)、[当前确认有效性](../yudao-module-hrm/src/main/java/cn/iocoder/yudao/module/hrm/service/payroll/HrmPayrollWorkbenchService.java#L198)；Workbench传播测试 |
| 问题流转 | OPEN→PROCESSING→REVIEW→REJECT/重新提交→CLOSED→REOPEN；处理人与复核人不同，关闭检查引用；新轮必须新期限 | [问题闭环](../yudao-module-hrm/src/main/java/cn/iocoder/yudao/module/hrm/service/payroll/HrmPayrollWorkbenchService.java#L230)；Workbench独立关闭/日期/关联问题测试；第三轮已补字段引导，见3.0/14 |
| 看进度做取舍 | 纳入/暂缓/移除独立于确认和实施；重复移除有替代项校验；统计分母不因暂缓/异议消失，受限节选不输出隐藏集合 | [范围取舍](../yudao-module-hrm/src/main/java/cn/iocoder/yudao/module/hrm/service/payroll/HrmPayrollReviewService.java#L58)、[统计集合](../yudao-module-hrm/src/main/java/cn/iocoder/yudao/module/hrm/service/payroll/HrmPayrollReviewService.java#L132)；Review固定集合及受限/筛选测试 |
| 决议与发布 | 已登记独立材料→根登记/非登记人签署→决议草稿/提交/非起草人批准；未来/到期/撤销/替代单列 | [授权根登记](../yudao-module-hrm/src/main/java/cn/iocoder/yudao/module/hrm/service/payroll/HrmPayrollGovernanceRootService.java#L48)、[决议生命周期](../yudao-module-hrm/src/main/java/cn/iocoder/yudao/module/hrm/service/payroll/HrmPayrollGovernanceDecisionService.java#L48)；Governance生命周期测试；正式材料本轮仍未验证/补齐 |
| 发布一致性 | 全量闭包可见、当前范围与确认/来源规格/有效决议/相关阻断均检查；依赖摘要和请求键约束旧请求、重复请求 | [发布门槛](../yudao-module-hrm/src/main/java/cn/iocoder/yudao/module/hrm/service/payroll/HrmPayrollGovernanceReleaseService.java#L72)、[项目依赖闭包](../yudao-module-hrm/src/main/java/cn/iocoder/yudao/module/hrm/service/payroll/HrmPayrollGovernanceReleaseService.java#L52)；27项治理测试含“无关项目根/锚点/人员不影响发布及同号重试” |
| 历史及导出 | 冻结对象图和当时姓名/依据；当前权限裁剪为受限节选；任务创建、生成、状态、领取均再校验 | [冻结发布](../yudao-module-hrm/src/main/java/cn/iocoder/yudao/module/hrm/service/payroll/HrmPayrollGovernanceReleaseService.java#L110)、[六表权限投影](../yudao-module-hrm/src/main/java/cn/iocoder/yudao/module/hrm/service/payroll/HrmPayrollGovernanceProjectionService.java#L32)、[领取再鉴权](../yudao-module-hrm/src/main/java/cn/iocoder/yudao/module/hrm/service/payroll/HrmPayrollGovernanceExportService.java#L53)；历史治理测试含无关锚点不失效旧任务，D-03当前8列候选仍待原生视觉验收 |
| 实施与协作 | 实施状态独立，验收通过需证据引用但不自动核验；批量逐项检查；会议/提醒追加历史；双版本字段比较 | [实施记录](../yudao-module-hrm/src/main/java/cn/iocoder/yudao/module/hrm/service/payroll/HrmPayrollReviewService.java#L79)、[会议和站内提醒](../yudao-module-hrm/src/main/java/cn/iocoder/yudao/module/hrm/service/payroll/HrmPayrollReviewService.java#L87)、[版本差异](../yudao-ui/yudao-ui-admin-vue3/src/views/hrm/payroll/review/index.vue#L534)；Review实施/提醒/批量部分失败测试 |

## 6. 当前事项与剩余顺序（第三轮更新）

| 顺序 / ID | 事项与状态 | 依据 | 完成条件 |
|---|---|---|---|
| 1 / D-01 | COL发布能力说明及RPT阶段注释过时；**未修复** | [COL过时文案](../yudao-ui/yudao-ui-admin-vue3/src/views/hrm/payroll/review/demo.ts#L236)及[RPT阶段注释](../yudao-ui/yudao-ui-admin-vue3/src/views/hrm/payroll/review/demo.ts#L223)；第三轮[51稳定页面](hrm-payroll-prd04-browser-third-verification-20261021/51-fresh-readback-confirmed-v14.dom.txt)实际显示“S5待实现”；browser-col.txt仅属原代码审计历史包 | 同时处理第236行“S5待实现”和第223行“发布六表留S5”，文案准确区分已有TEST发布能力与正式批准；后续在COL、RPT及治理页核对，不再显示这两处阶段占位语。此项业务修复仍开放 |
| 已修复 / D-02 | WB-04/06原缺字段引导已补齐；**已完成（限定该缺口）** | 当前validation.ts/issueValidation.ts；第三轮25、48/49明确字段提示与输入保留；51稳定回读1780仍v14，36/37问题独立关闭 | 不扩大为所有错误场景或整个AC通过；WB-10空证据19b为保存禁用；已填证据的20为通用失败句，归因未核实，另保留观察项 |
| 1 / D-03 | 六表布局已改为8列，**部分完成、原生视觉未验** | 当前ExportService候选；1388/1390实际下载，六表各1条82字段一致，结构8列、最高102pt，见第三轮xlsx-verification.json | 用同一文件在Calc/Excel逐张验证中文、长文本、关联、滚动及打印阅读；结构检查不能替代视觉通过 |
| 并行 / D-04 | 正式受控授权根、范围/矩阵及必要条款批准；**未完成，材料阻塞** | Root独立ANCHOR/TEST隔离、Decision REQUIRED、发布judgement；历史MB-01及AC-25/28仍开放 | 实际有权业务方提供并批准受控材料；不得把TEST签署或管理员授权改称正式材料 |
| 4 / D-05 | 固定最终候选的正式组合验收；**部分完成** | S6历史矩阵26项工程通过/2项材料阻塞，后续FA修复和最新时间线覆盖分层；原代码审计135项测试加此次合成多角色浏览器链，仍非正式组合验收 | 受影响流程补回归，材料具备后执行正式AC-25/28；版本/权限/历史导出与版式证据对应同一候选，不直接沿用旧PASS |
| 已缩小 / D-06 | 权限/加载提示边界；**部分核实** | 历史首轮48/第二轮47只为通用加载提示，不倒填；第三轮39评审实际显示“无项目访问或操作权限”，40治理未显示受控内容 | 本轮没有记录具体HTTP状态或全部越权矩阵；不替历史请求补造原因 |
| 观察 / D-07 | 菜单展开及路由弃用警告；**未修复，未阻断此次操作** | 首轮编辑会话采样7条warn、0条error，包含菜单`/hrm-payroll`展开异常及`next()`弃用，见浏览器日志 | 单独复现菜单入口并定位；本次用已有路由完成操作，不据此声明所有菜单导航正常 |

D-01～03对应既有W-F01～03。最新D-02已修复并实测；D-03已有8列候选但视觉仍开放，不包装成重新开发整套功能。D-04/05为材料及组合验收，不能靠放宽门槛“修掉”。**旧FA-01依赖范围问题已有项目闭包修复及历史反例测试；本轮未重跑测试，不继续把原始FA-01当未修复。** 仍有全局锁/部分全量读取的实现成本，本轮未做负载评估，不能宣布性能改善或新增性能事故。

## 7. 原代码审计及前两轮验收证据边界（历史；最新见3.0/14）

| PRD验收集合 | 本轮可以支持什么 | 不能据此声称什么 |
|---|---|---|
| AC-01/03/11/12/13/15：展示、上下文、持久化、响应式 | 此次十视图各一条操作意见、字段1707、三视口各一条保存及历史，见3.1 | 所有十视图×三视口×字段/模块/操作/流程交叉矩阵全部通过 |
| AC-02/04～10/17/18/21～23/26/27：模板/评审/来源/问题/统计/范围 | 本轮六套H2/mock测试及源码可支撑其覆盖的逻辑；具体方法名在tests-summary.json | 每条AC全部真实API、租户、并发、浏览器条件在当前候选完整重演 |
| AC-14/19/20：权限、撤销、历史投影 | 当前动作/范围/投影/再鉴权路径存在；历史多角色受限投影/撤权拒绝及冻结内容比较 | 本轮重新执行全部跨租户/对象替换和隐藏字段泄漏矩阵 |
| AC-16：六表真实打开及易读 | 首轮下载1377/1379并Calc打开版本说明；第二轮下载1380/1382且逐张打开浏览器投影，但未原生逐张视觉验收，宽表问题仍在 | 最新文件六表全部视觉通过；W-F03/D-03仍开放 |
| AC-24：性能与两会话 | 历史S6有原始采样及独立审计复算记录 | 本轮重新采样10分钟或已证明所有后续变更不影响性能 |
| AC-25/28：正式发布与决议依赖 | 原治理测试27项含FA-01反例；首轮为草稿1377/任务1379；第二轮预检查为50、草稿1380、任务1382及下载副本。两轮均未重演正式决议链 | 正式授权材料已具备、正式基线已发布或PRD28/28通过 |

旧26/28是当时工程矩阵的范围和候选，不是本轮重新验收的通过数量。本轮不输出没有依据的“正式完成率”。

## 8. 真实薪酬业务的完成边界（与评审展示分开）

本PRD把以下列为后续业务实施。现有薪酬普通工资主链的确有代码和TEST操作成果，不能一笔抹成未开始；但不足以把43个业务候选全标完成。

| 业务时间线 | 已有明确成果 | 完成判断与未完成范围 |
|---|---|---|
| 主体/人员/定薪/政策/输入 | TEST主体5、员工9、定薪7；四类输入26/27/29/30独立复核；见修订后实测P01～P12 | **部分完成**：普通主链样本有证据，全部制度/人员/跨期/真实接入未验 |
| 核算→审批→冻结→本地回盘→成本→归档 | TEST批次8：6000应发、5504.40实发、6960成本；不同角色事件及文件有依据 | **部分完成**：金额沿用已独立复核样本，本轮未重算工资；模拟回盘不是银行真实付款，失败/重试/补发/追溯等未完整覆盖 |
| 工资条发布及本人查看 | 后台发布6及金额已核对；本人账号当时路由404 | **部分完成**：不能用后台可看代替员工本人读取/已读，前置权限与本人回归仍未完成 |
| 个税申报准备→批准→导出→缴退税 | 有TaxFiling等实现；十月尝试出现通用税务错误，无申报行 | **部分完成**：具体抛错分支未由请求体证明；合法结束期间的后续申报/批准/导出及缴退税未走通 |
| 缴费实缴对账、预算分析、奖金及跨期分支 | 现有功能入口/服务不能作为完成证明 | **未完成验收**：本轮未逐分支审查/运行，不判“未开始”，也不判“已完成” |

依据：[已修订薪酬时间线报告](hrm-payroll-business-multirole-timeline-test-20261020.md)、[最近引用边界更正](hrm-payroll-business-timeline-audit-followup-20261021.md)。这不是对43项正式业务逐项验收的替代品。

## 9. 文件与证据

[原代码审计证据索引](hrm-payroll-prd04-function-audit-evidence-20261021/README.md)含候选哈希、6套135项用例、46模板身份覆盖、历史完整性及当时只读抽查。首轮操作另见[首轮浏览器证据索引](hrm-payroll-prd04-browser-verification-20261021/README.md)，最新操作见[第三轮索引](hrm-payroll-prd04-browser-third-verification-20261021/README.md)。原PRD、原审计及旧证据不修改；按用户要求更新本报告，更新前副本已保留。

测试日志：`/Users/waitforpro/.hrm-payroll-local/prd04-code-audit-20261021-tests.log`。本报告没有秘密凭据；未做commit/push。

## 10. 首轮浏览器核验记录（历史，非当前终态）

### 10.1 角色、数据与操作边界

| 角色 | 本次身份与行为 |
|---|---|
| 编辑/处理人 | `hrms5editor` /266：建项目、改名、意见、分派、来源映射登记、问题处理及重开、固定草稿与导出 |
| 独立复核人 | `hrms5approver` /267：本人确认、四类核验、问题驳回/关闭、接收站内提醒。1707的v3与v10为267，其余版本为266 |
| 受限账号 | `hrms5restricted` /268：真实登录成功，105评审与治理内容未加载、写入入口禁用；页面提示混合权限/异常，未留下本次请求的HTTP原因。未扩权获取正向结果 |

127.0.0.1与localhost用于隔离浏览器会话，均为本机5173服务。业务写入均通过页面，没有SQL/API替代操作。密码从仓库外私有目录读取，只输入本机登录表单，不记录在报告中。临时角色标签页已关闭，用户标签页恢复105治理页，临时视口已清除。

保留的TEST记录：项目150（46模板＋10视图意见＋3视口意见＝59条）；105新增需求1707、来源3441、关系3450、问题3418、固定草稿1377及导出任务1379。1707最终v10/已确认/暂缓/来源已核验可用/开发中；3418最终v10/CLOSED/cycle2，新期限2026-11-02。未清理历史TEST数据、未改真实薪资或进行资金交易。

### 10.2 十视图逐一实测结果

以下十条完成“打开对应视图→补充操作规则或例外→保存草稿→最终清单回读”，均在150中为PENDING/候选/未就绪/未排期/v1。草稿不等于已确认需求。[最终完整十项回读](hrm-payroll-prd04-browser-verification-20261021/50-ten-view-final-page1-dom.txt)包含以下全部记录。

| 视图 | 保存记录 | 操作证据（目录内文件名前缀） |
|---|---|---|
| OV 薪酬总览 | 1708 / CUSTOM-0001 | 14-view-OV-opinion |
| PLAN 薪酬方案 | 1709 / CUSTOM-0002 | view-PLAN-saved |
| CALC 工资核算 | 1710 / CUSTOM-0003 | view-CALC-saved |
| TAX 个税与工资条 | 1711 / CUSTOM-0004 | view-TAX-saved |
| ATT 考勤 | 1712 / CUSTOM-0005 | view-ATT-saved |
| OT 加班 | 1713 / CUSTOM-0006 | view-OT-saved |
| HOUR 工时 | 1714 / CUSTOM-0007 | view-HOUR-saved |
| INS 社保公积金 | 1715 / CUSTOM-0008 | view-INS-saved |
| RPT 报表分析 | 1716 / CUSTOM-0009 | view-RPT-saved |
| COL 需求征集 | 1717 / CUSTOM-0010 | 16-col-draft-after-refresh |

三视口记录1718/1719/1720为CUSTOM-0011/0012/0013，分别对应390/768/1440；[最终回读](hrm-payroll-prd04-browser-verification-20261021/51-ten-view-final-page2-dom.txt)三条齐全。此次没有逐个执行43个正式薪酬候选，模拟核算、工资条预览等演示交互也没有据此全判通过；第4节代码状态不能替代实测。

### 10.3 六表导出与交付缺口

实际领取`薪酬评审-快照1377-任务1379.xlsx`，副本[draft1377.xlsx](hrm-payroll-prd04-browser-verification-20261021/draft1377.xlsx)，SHA256 `301c94b351601905c7803b2f2bf017c9c358d04d54eb00053ea2184bdcdfcac6`。它是TEST固定草稿，不是正式发布基线。

六张表依次为评审版本说明、功能需求清单、数据源清单、字段映射、问题与待办、范围与变更；XML行数（含表头）32/17/6/4/43/17，列数115/53/80/80/7/278，最高行高364pt（问题表252pt）。结构检查不等于每格关联正确或六表视觉验收。

Calc实际打开1377/1379的版本说明表，SUMMARY行占据大部分窗口，仅前3列可见，可读性仍不通过。其余五张表未逐张视觉验证；恢复会话时遇到另一未保存工作簿的关闭确认，为保留用户文件状态，未处理无关保存对话框。D-03保持开放。[原生截图](hrm-payroll-prd04-browser-verification-20261021/native-01-summary.png)、[结构结果](hrm-payroll-prd04-browser-verification-20261021/xlsx-structure.json)。

### 10.4 证据可靠性与未执行事项

- 原报告副本SHA256 `d6b7a3e5bb0ff153ca08b400a169cd66e508898dfda2810a76a0cd40a3527417`，见before.json。没有把原只读抽查改写成当时已执行多角色操作；第3.1及10节专列此次补验。
- 40/41采样时异步筛选未完成，仍是390记录，不能证明768；有效补证为44/45及51。47原生AX读到另一前台标签页150，不用作268访问105的证据；48/49精确标签页DOM和截图证明268页面内容未加载，不能单凭其通用提示确定失败原因。原采样保留，不删除无效或不利证据。
- 最终DOM50/51回读全部13条，52/53/54确认1707最终状态、历史操作者及v7→v8字段变化。helper中的submitted标志或点击完成均不能代替回读。
- 控制台采样7条warn、0条error，含菜单展开异常及路由next弃用。不能据此断言所有会话无错误，D-07为待定位观察项。
- 未执行完整AC-01～28组合验收、正式材料批准、新决议生命周期、正式发布、模板升级采纳、范围修订、成员授权撤销、多对多/跨项目复制、全部质量失效恢复、并发冲突、固定性能采样及所有三视口交叉表单。各行未覆盖项没有标为通过。

本次完成真实操作后的报告更新，没有修复业务代码、提交或push。D-01/02/03仍需编码修复，D-04为业务材料阻塞，D-05需组合验收；D-06已缩小到权限边界/提示定位，D-07为非阻断警告。

## 11. 独立审计发现的引用更正（2026-10-21）

依据[独立审计](hrm-payroll-prd04-function-completion-independent-audit-20261021.md)第4.1～4.3节，已更正D-06和WB-01/02等对应表述：治理页按48原文引用；46项只认一次留档计数，不再判浏览器幂等通过；创建人的确认限制按“范围尚未定义，不能确认”引用，不再归因为本次页面已证实权限拒绝。创建人不自动具有确认权的代码规则及原测试依据仍保留。

原浏览器证据目录（含README、verification-summary及141项清单）保持原样，属于更正前封存包。其README中的“首次46/重复46”“治理明确无权限”等措辞已被本节及[审计处理报告](hrm-payroll-prd04-function-completion-audit-response-20261021.md)替代；旧verification-summary的reportSha256仅绑定更正前报告，不能用于校验本报告当前版本。更正前副本及新的哈希关系见[处理证据](hrm-payroll-prd04-function-completion-audit-response-evidence-20261021/verification.json)。本次为文档引用纠正，没有补造历史回读、重跑浏览器/135项测试、修改业务代码或关闭D-01～05/07。


## 12. 第二轮复验的当时结果（历史，2026-10-21）

本轮按用户“再验证一次”要求重新操作。报告修改前SHA256为`ef124a084b63cffa5e505efb75b3d967dfa17a061734e777258c6a1f328e4899`，副本及本轮证据在[第二轮目录](hrm-payroll-prd04-browser-reverification-20261021/README.md)。原141项浏览器包、独立审计和引用更正包保留原样；其哈希只绑定各自当时报告，不绑定本报告新版本。当前关系由本轮verification.json记录。

### 12.1 本轮真实操作及当前终态

- 主标签页实际登录切换：编辑266→独立复核267→受限268→编辑266；初始localhost临时页只停留受限首页即关闭，没有将其作为业务操作会话。所有业务写入通过本机内嵌浏览器界面，未直接调用SQL/API，未扩权或修改密码。
- 项目150改名为“TEST PRD04 二轮角色复验 A 20261021”。02/03/04的DOM内容同哈希，只能按一份59条记录引用；不能用其文件名证明初始化前后各次59条。随后十视图新增1721～1730，COL手机流程1731、平板字段1732、桌面模块1733，最终72条/本轮筛选13条，与59＋13一致。均是PENDING草稿，不是正式功能确认。独立前后计数只采用下述151证据。
- 项目151为本轮真实新建“TEST PRD04 二轮新建 B 非正式批准”，只定义创建上限，生效业务范围仍未定义；分别留存空集合0、首次46、再次46及C0，不自动确认。该完整浏览器前后计数补足了首轮幂等证据不足，但不倒填首轮。
- 1707从v10 CONFIRMED经266改业务目标到v11 REVIEWING/C0；缺验收引用的人工ACCEPTED保存被阻断并保留输入，补合成引用后v12为“验收通过（人工记录）”且仍REVIEWING；会议/提醒至v14，267本人确认后最终v15 CONFIRMED/DEFERRED/READY/ACCEPTED（人工TEST记录）。这不代表真实薪酬、软件交付或正式业务验收通过。
- 来源3441交付D1→D2，语义/运营/交付1/3/2。旧QS/QC失配后，由267人工登记新QS，再登记3450新QC，最终已核验可用；映射版本仍1。受控样例引用及摘要都是明确TEST合成字符串，未验证实际外部文件。
- 问题3418本轮缺新期限重开被阻断；补2026-11-03后由266开始/提交，267独立关闭，最终v14/CLOSED/cycle3，历史最后关闭actor267。首轮REJECT及两轮关闭是历史，本轮没有重跑REJECT。
- 新TEST固定草稿1380：同请求号`TEST_RECHECK_20261021_DRAFT`重试没有新增草稿；六投影逐张选中并截图，任务1382 READY且实际领取。正式基线仍因858/1359缺当前本人确认而阻断；未放宽门槛、未正式发布。

十视图本轮新增记录依次为OV1721、PLAN1722、CALC1723、TAX1724、ATT1725、OT1726、HOUR1727、INS1728、RPT1729、COL1730；最终完整回读见59。三视口的1731/1732/1733最终回读见60；手机FLOW历史有效补证为66，FIELD/MODULE历史为13/15。所有宽度结束后恢复默认。

### 12.2 导出、日志与证据边界

实际下载副本[draft1380-job1382.xlsx](hrm-payroll-prd04-browser-reverification-20261021/draft1380-job1382.xlsx)，SHA256 `2f1cc9572e1e3f3b7e97f845bbe26b92d6c1f9459511f1219f1902f9a4d77f6c`。ZIP/XML六表行数（含表头）32/17/6/4/48/17、列数115/54/110/114/7/345，最高行高364pt（问题表252pt）。浏览器投影已逐张打开，但本轮未在Calc/Excel逐张视觉验收该文件，也未对账每个字段关联；D-03继续开放，不能把文件结构检查当可读性通过。

本轮末尾控制台采样64条warn、0条error：路由next弃用39条、ElementPlus underline弃用24条、菜单/hrm-payroll展开异常1条；不是全部会话无错的证明，D-07不关闭。中途53条采样另行保留，不混为同一时点。

早期部分DOM采样与实时界面不一致：23/25仍为D1，用33/62稳定AX及截图作D2依据；27/28仍是旧CLOSED，用29及65的cycle3历史证明重开；32没有成功切换来源页签。11的DOM实际已有“真实历史与版本差异”、版本1、操作人266及1731/FLOW内容，原先写成“未实际打开”不准确；66保留为实时AX/截图补证。02/03/04三份DOM完全同内容，不作三次独立计数。29起同时保存实时AX、DOM及截图；最终确认、质量、问题分别以61、62/63、64/65交叉回读，不凭点击完成认定成功。旧样本保留并标明限制。

### 12.3 第二轮当时完成边界与剩余项（历史）

原代码完成判定8/3/0未改变，不换算为浏览器通过数量。第3.1表逐项给出本轮通过范围和未覆盖分支；没有把完整WB或AC验收全部关闭。D-01过时文案、D-02字段级指引、D-03六表版式、D-04正式材料、D-05正式组合验收及D-07警告仍开放；D-06只按268实际通用加载提示引用，不补造HTTP拒绝原因。

范围修订/成员增撤、模板差异采纳、十视图全入口与三视口交叉、网络失败恢复、功能DISPUTED、并发旧版本、来源/关系语义全部变化、多对多/复制、固定统计矩阵、新决议批准/正式发布、撤权后下载及性能均未本轮完整验证。

本轮没有修改业务代码、重新运行135项测试、连接Cloud、操作真实工资/银行/税务申报、外发消息、提交或push。用户标签页已恢复266的105治理页。

## 13. 第二轮独立审计引用更正（2026-10-21）

依据[第二轮独立审计](hrm-payroll-prd04-function-completion-independent-audit-round2-20261021.md)第4节，已处理如下引用问题：

1. 项目150的02/03/04是同一份DOM内容，仅作一次59条计数；重复初始化的独立计数只使用项目151的56/57/58三份不同AX（0/46/46），不倒填首轮或项目150。
2. 第5节明确首轮1708～1717与第二轮1721～1730，第7节明确首轮1377/1379与第二轮1380/1382，避免历史记录被读成新生成记录。
3. D-06明确证据包和页号：[首轮治理48](hrm-payroll-prd04-browser-verification-20261021/48-restricted268-governance-dom.txt)、[首轮评审49](hrm-payroll-prd04-browser-verification-20261021/49-restricted268-review-dom.txt)、[第二轮治理47](hrm-payroll-prd04-browser-reverification-20261021/47-restricted268-governance105.ax.txt)、[第二轮评审46](hrm-payroll-prd04-browser-reverification-20261021/46-restricted268-review105.ax.txt)。[第二轮48](hrm-payroll-prd04-browser-reverification-20261021/48-editor266-restored.ax.txt)是266恢复页，不能当268治理证据。仍未留下当次HTTP原因。
4. [第二轮11](hrm-payroll-prd04-browser-reverification-20261021/11-flow390-history.txt)已有历史对话框、版本1与操作人266，原先写成完全未打开不准确，已纠正；[66](hrm-payroll-prd04-browser-reverification-20261021/66-flow390-confirmed-history.ax.txt)继续作为更完整的实时AX及FLOW锚点补证，不扩大WB-09的通过范围。

原第二轮227项包（包括README、verification.json）保持原样。其README关于150三次59及11未打开的文字由本节替代；其`project150CountsBeforeNewOpinions`数组仅是当时转写，不再作为三个独立执行结果。旧verification的reportSha256绑定更正前报告`d608fbd934a94a4bb71c7912800c44621eec1e42f32dc142866248f71c001a9e`；当前报告前后哈希和原包完整性见[本次处理证据](hrm-payroll-prd04-round2-audit-response-evidence-20261021/verification.json)。[处理记录](hrm-payroll-prd04-round2-audit-response-20261021.md)逐项记载关闭边界。

本次仅更正文档引用并核验原始文件，未重放浏览器或135项测试、未修改业务代码及数据、未提交/push。8/3/0代码判定维持，D-01～05/07仍开放；文档更正不等于产品缺口已修复。

## 14. 第三轮复验记录与最新边界（2026-10-21～22）

### 14.1 时间线、角色与实际保留记录

此次从10月21日晚开始，22日完成。通过用户内嵌浏览器的同一主标签页，266先处理，真实退出登录267进行本人确认、质量登记与独立关闭，再登录268验证受限读取，最终恢复266。账号分别为hrms5editor、hrms5approver、hrms5restricted；密码只从仓库外私有配置读取并输入本机登录表单，不写入证据。除创建TEST项目自动赋予的创建人边界外，未新建/扩大授权；既有成员列表只读。

| 顺序 | 实际操作及终态 | 稳定证据（第三轮目录） |
|---|---|---|
| 1 | 新建152：最初未填创建范围被拒，补既有允许上限后成功、改名；业务范围仍未定义。空0→一份46条DOM；再次采样与首次46相同 | 05/06/07/08；07/08不能算两份独立计数，重复初始化幂等本轮未独立证明；02～04不当成功证据 |
| 2 | 十视图操作意见1827～1836（OV/PLAN/CALC/TAX/ATT/OT/HOUR/INS/RPT/COL顺序）；三尺寸FLOW/FIELD/MODULE 1837～1839，最后全项目59/筛选13、C0 | 09/15/16稳定回读，10实际OPERATION历史，17实际FLOW历史（默认视口） |
| 3 | 1780已存在的合并内容v9本轮重新读取；改为DISPUTED v10，登记人工实施状态v11、纪要v12、提醒v13；267本人确认v14。后缺字段保存被阻断、取消，最终新读仍v14 | 01/18/20b/21/22/30/31/32b/48/49/51；新回读不追认旧fix证据 |
| 4 | 3441交付换D3，旧QS失配；267登记QS、QC；当时3450仍待质量或供给核验。3418第五轮独立关闭后，最终3450已核验可用 | 23/24/33/34；35为未就绪的中间态，不依其文件名判可用；35b为最终可用 |
| 5 | 3418缺新期限提示→补2026-11-05重开v21→处理v22→提交v23→267关闭v24/CLOSED/cycle5。关闭时间页面记录2026-10-22T08:04:26.139；实际打开完整对象历史 | 25/26/28、36/37；处理中使用37的ISSUE_START22（actor266、PROCESSING、cycle5），27列表仍是待处理；37也含ISSUE_CLOSE24/actor267 |
| 6 | 268实际登录；105评审明确无权限，治理不加载受控内容；恢复266。未改授权或密码 | 38～41 |
| 7 | 依据既有TEST决议1370，预检查齐备后发布TEST基线1388/v8，时间2026-10-22T08:06:22.050；同号重试后稳定列表仍版本1～8、只有一个1388；1390 READY并领取实际XLSX | 42/43b/45/53/54；43/44中间采样不单独作持久化/幂等结论 |

本轮新项目是152，不是历史150/151；1827～1839均为待确认草稿。1780最终CONFIRMED/DEFERRED/NOT_APPLICABLE/v14、人工ACCEPTED，不把人工实施登记当软件验收。1707未在本轮重新做功能确认；其来源可用随D3质量及问题条件恢复。原业务工资、银行付款和税务申报未操作。

### 14.2 六表实际下载与可证明范围

[实际下载副本](hrm-payroll-prd04-browser-third-verification-20261021/release1388-job1390.xlsx)对应TEST基线1388、任务1390。六投影在浏览器逐张切换，并每表打开1条记录，保存JSON。只读ZIP/XML核对[结果](hrm-payroll-prd04-browser-third-verification-20261021/xlsx-verification.json)：六表均8列、最高行高102pt、无公式单元格；所见6条记录共82字段与下载一致。工作簿行数含表头依次718/915/886/964/301/3635。

**本轮没有全量逐字段对账，也没有在Calc/Excel原生逐张打开六表、没有历史文件重导对比；均未完成。** 之前修复轮的1130字段/历史重导结果属于另一封存候选/文件，不移作本轮结果。D-03从旧宽表“待布局修复”推进到“8列候选已核对结构及样例，待原生视觉验收”；WB-08仍部分完成。正式根材料与必要条款仍未获得实际业务批准，TEST基线不关闭AC-25/28。

### 14.3 缺口、未覆盖与日志

- D-02原两项已修复：需求缺触发场景/异议下一步明确定位，重开缺新期限明确定位，输入保留；正向确认、独立关闭及无效保存后不写入均有当前证据。因此WB-04/06在实现口径更新为已完成，而非宣告所有浏览器分支通过。
- D-01的COL“S5待实现”仍在当前demo.ts第236行；未改。D-07历史路由/组件警告仍未定位修复。WB-10的19b空证据使保存按钮禁用，已填字段可见；20已有TEST证据却显示通用失败句，未核实具体失败原因，通用失败提示作为可用性观察项保留。
- 未重跑：完整AC-01～28、范围修订/成员增撤、模板升级差异采纳、所有十视图×四入口×三尺寸组合、并发旧版本冲突、全语义变化、多对多/跨项目复制、问题驳回/重派/逾期及自己关闭负例、固定统计矩阵、受限下载和撤权后领取、新决议独立批准/撤销、正式发布、性能/迁移矩阵。不能据历史通过把这些写成本轮通过。
- 浏览器日志采样73条：71条warn、2条error；两条error均为创建项目缺少有效范围/授权依据的负例，保存为browser-console-final.json。不声明“零错误”或所有警告无风险；实施缺证据和受限账号的页面阻断由相应DOM证明，不冒充控制台错误记录。

### 14.4 报告与候选冻结

[第三轮索引](hrm-payroll-prd04-browser-third-verification-20261021/README.md)列出有效终态与不适用的瞬时采样；saved-opinions.json为中途局部缓存，13条完整回读以09及16为准。原报告更新前副本SHA256 `35049bd194cfffda457445df708781015415aa4a21af6dce73a24ca3a12ea3ae`。第三轮candidate-end.json与manifest.json绑定的是本次更正前的第三轮报告，现已作为封存历史。当前更正前后哈希由第15节的新证据建立对应；旧包只校验各自当时文件，不能校验本报告新内容。

本轮没有修改业务代码、重启服务、运行后端测试、连接Cloud、外发邮件/短信、commit或push。用户标签页恢复266的105治理页，保留TEST记录供复核。本报告更新完成不等于正式PRD验收完成。

## 15. 第三轮独立审计处理（2026-10-22）

依据[第三轮独立审计报告](hrm-payroll-prd04-function-completion-independent-audit-round3-20261022.md)第4.1～4.5节，对五组引用逐项更正。审计原文及第三轮176份封存证据保持原字节，未为修复引用重新生成浏览器操作或初始化请求。

| 审计发现 | 更正及采用的依据 | 关闭边界 |
|---|---|---|
| 4.1 同一46条DOM被算作两份独立计数 | 3.0 WB-02/WB-07、14.1改为空0→一份46条DOM，07与08同哈希只计一次；15为后续59条总集合 | 引用已改；第三轮重复初始化幂等没有独立证明，不判该项浏览器通过。历史151记录不倒填给152，WB-02代码状态不变 |
| 4.2 空证据负例与通用失败混同 | 3.0 WB-10、6 D-02和14.3分开19b空证据/禁用按钮、20已填证据/通用失败、20b稳定保存v11 | 引用已改；19b没有失败错误句，20具体失败原因未核实，不归因为空证据。后续267确认另用31/32b/51 |
| 4.3 当前代码链接行号漂移及响应式范围过宽 | 3.2/5受影响链接改到当前newOpinion872、字段提示912、历史534、媒体查询1181、问题提示669及ReviewService各实际方法；WB-09收窄至三尺寸保存，默认视口FLOW历史 | 链接与符号已核对；不等于重新跑源码测试或三尺寸历史矩阵 |
| 4.4 D-01混用历史browser-col.txt | 6 D-01明确链接第三轮51稳定DOM及demo.ts236；历史browser-col只按原审计包使用 | 引用已改；D-01产品文案本身未修，继续开放 |
| 4.5 处理中采样及分派目标归因 | 27仍待处理；PROCESSING改引37的ISSUE_START22；22只证明1780单项success，267提醒和确认分别取30/32b | 引用已改；不从22推断目标ID267，也不把后续事件当作当时列表显示 |

当前实现口径仍10已完成/1部分完成/0未开始，WB-08部分完成；D-02只关闭WB-04/06已知字段引导。D-01、D-03原生视觉、D-04正式材料、D-05组合验收、D-07警告继续开放。第三轮73条控制台采样及6条82字段XLSX对账仍引用原包，本次没有重跑。此次仅处理文档证据引用，不引入业务代码或数据库修改。

原第三轮包的manifest及candidate-end绑定报告SHA256 `a4af02056c6e40a8b62f2897715373e1caf6135a5a947f4755221777824b8250`；更正前原样副本保存在[report-before.md](hrm-payroll-prd04-round3-audit-response-evidence-20261022/report-before.md)。原README中的0→46→46按本节收窄；它已经区分20失败与20b成功，该部分继续有效。19b/20混同更正的是本报告原3.0/6/14.3节，不能反称原README也混同。原包不改写。本次处理索引、哈希和验证结果见[处理报告](hrm-payroll-prd04-round3-audit-response-20261022.md)及[verification.json](hrm-payroll-prd04-round3-audit-response-evidence-20261022/verification.json)。

## 16. 用户可见主线程续核（2026-10-22）

本节仅转述[可见续核人工摘要](hrm-payroll-prd04-visible-browser-reverification-20261022/README.md)：摘要记载当次在用户可见的Codex内嵌主标签页1打开本机5173，并切换266→267→268→266；自报运行上下文为 `gpt-6-luna / max`。续核目录只有README及两份修改前报告副本，未落盘AX、DOM、截图、HAR或可独立核对的运行日志。**下表全部对象、版本、统计数字、时间和提示均为摘要自我记录，无落盘DOM；模型身份在该证据包内亦未独立核实。** 这些记录不与第三轮DOM/PNG/XLSX原件同级，不增加新的正向闭环，也不追认隐藏子线程为可见操作。摘要记载当次未改源码，HEAD为`234dca9b7e2e328c9fb107b539b9d45d78b8621f`；未核对运行进程实际加载制品，因此不绑定该HEAD的运行制品哈希。

| WB | 续核摘要记载（无落盘DOM） | 摘要可引用边界 |
|---|---|---|
| WB-01 项目、范围、成员 | 摘要记载项目105名为 `TEST S6 BROWSER_MAIN`、范围版本1；成员授权表可见266/267的测试成员与268两条已失效的历史范围授权；按266→267→268→266登录。 | 项目名称与范围版本分别记载，不将“TEST范围”作为范围名称；摘要只记只读，没有修订范围、增撤成员或扩大授权。 |
| WB-02 模板初始化 | 当前105已有17项；没有重新执行初始化。 | 本轮未测试幂等或模板升级。 |
| WB-03 十视图征集 | 在可见浏览器逐一打开OV、PLAN、CALC、TAX、ATT、OT、HOUR、INS、RPT、COL；显示虚构样例及安全提示。 | 只验证可见和导航；没有重做意见保存、失败恢复或完整入口矩阵。 |
| WB-04 完善与本人确认 | 267对合成需求1501的确认负例被必填字段校验阻断，提示尚未签字；读取1780历史显示v17由266记录异议、v18由267确认。 | 负例阻断为本轮操作；v17/v18是本轮读取的既有事实，不是本轮重新完成正向签署；并发冲突与全字段分支未重跑。 |
| WB-05 来源、映射与QS/QC | 当前3441为语义/运营/交付v1/5/4，显示供给、语义、QS通过；读取QS3511绑定deliveryVersion4，QC3513绑定QS3511和relation3450/mapping1，关系显示可用。 | 仅核对TEST合成规格和历史证据，不代表真实外部供数；本轮未创建或修改来源/映射。 |
| WB-06 问题处理 | 当前3418显示已关闭；历史v25～v28显示cycle6重开、处理、提交及267独立关闭，截止日2026-11-12。 | 只读既有TEST历史；没有在本轮重演处理、驳回或重开。 |
| WB-07 统计 | 当前集合U17、E17、I3、C5、T14、D0；确认5/17、范围处置17/17、本期确认3/3、来源可用3/14、本期来源可用1/1。 | 这是摘要记载的页面统计，不能独立复核为服务器响应；不是发布或实施证明，没有重建统计矩阵。 |
| WB-08 决议、发布、六表 | 267治理页预检查明确提示“缺少当前有效的受控发布授权”，发布与固定草稿按钮禁用；仅查看既有TEST快照及决议。 | 没有发布、保存新快照或下载；没有正式决议材料、正式发布或六表视觉验收。 |
| WB-09 响应式 | 本轮只在默认可见视口导航十视图。 | 未做宽度切换、横滑或三视口表单回归。 |
| WB-10 实施关联 | 本轮未重新保存实施验收状态。 | 未验证。 |
| WB-11 分派与协作 | 看到了成员权限、意见历史和工作台提醒区；编辑者提醒区显示当前无发给自己的提醒。 | 没有发送提醒、创建纪要、混合分派或到期/撤销矩阵。 |

摘要记载的项目105范围、来源、关系、问题和历史均为TEST合成资料。对受限268，摘要同时记录工作台通用失败句“可能无权或接口异常”和另一条明确提示“无项目访问或操作权限”；治理页另记“没有独立决议读权”。三句须分别引用：通用失败句的具体原因未核实，不能并入已证明的权限拒绝或排除接口异常。这里也没有落盘页面原件，不从空白表格推断项目为空，不猜测隐藏对象数量或HTTP状态。摘要称只读既有成员表、未改授权，不代表重跑完整权限矩阵。

**总体判断不变：代码实现口径仍为10项已完成、1项部分完成（WB-08）、0项未开始；该摘要没有将11项升级为浏览器全部通过，也没有关闭AC-01～28。** 摘要记载当次没有运行后端测试、读取控制台/HAR、重启服务、操作真实薪酬/税务/银行或外发消息，没有提交或push；最后恢复到266的项目105需求征集页。这是当次页面终态的自述，不表示本次文档更正重新打开或核验了当前标签页。

## 17. 第四轮独立审计处理（2026-10-22；纯文档更正）

依据[第四轮独立审计报告](hrm-payroll-prd04-function-completion-independent-audit-round4-20261022.md)第4.1～4.4节更正全文引用。修改前报告SHA-256为 `5d43029befc41ce3d0682dcd2b06c2a84a38cc6e0226ffea75b834e460c136f9`；原字节副本见[report-before.md](hrm-payroll-prd04-round4-audit-response-evidence-20261022/report-before.md)。独立审计、第三轮封存证据及可见续核目录均保留原字节。

| 发现 | 本次处理 | 关闭边界 |
|---|---|---|
| 4.1 轮次混同 | 第1节的WB-04/06正向闭环明确归第三轮；第2.0/3.0标题区分第三轮完整操作与后续第16节；第3.2字段提示也明确第三轮。 | 引用更正，不新增可见正向闭环或浏览器通过数。 |
| 4.2 权限提示及范围名称 | 第16节分别保留“可能无权或接口异常”“无项目访问或操作权限”和治理页独立读权提示；项目名与范围版本1分别记载。 | 通用失败原因未核实；不据此证明完整权限矩阵。 |
| 4.3 原件缺失 | 文首及第16节明确为人工摘要，无落盘AX/DOM/截图/HAR或独立运行日志；表中版本、统计和时间不与第三轮原件同级，模型身份在该包内未独立核实。 | 报告引用已降级；历史页面原件缺失仍存在，本次不补造或声称恢复。若需原件应另起新轮并落盘。 |
| 4.4 过早引用及D-01范围 | COL页面依据归第三轮51；D-01同时列demo.ts第223/236行；WB-03改引第三轮3.0/14；补code-link-checks.json链接。 | D-01产品文案仍未修；WB-03完成状态不变。 |

本次不修改业务代码、数据库或配置，不重放浏览器、不重跑后端测试，不提交/push。10/1/0继续采用原实现口径；D-01、D-03原生视觉、D-04正式材料、D-05组合验收、D-07继续开放。处理结果及验证见[第四轮审计处理报告](hrm-payroll-prd04-round4-audit-response-20261022.md)。
