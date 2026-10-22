# T01 关闭矩阵复核

日期：2026-10-22。期望/请求模型：`gpt-6-luna / max`；实际运行模型：未核实。本轮只读复核任务计划第 4 节、已有自动化日志及 T01 DOM/PNG/网络原件，新增本报告；没有操作浏览器、数据库、服务或产品源码。

## 当前自动化证据

- 后端定向测试退出码 0：81 项通过，0 失败/错误/跳过。分布为 `HrmPayrollReviewServiceTest` 19 项、`HrmPayrollWorkbenchServiceTest` 31 项、`HrmPayrollRequirementServiceImplTest` 31 项。日志及退出码见 [`server-feedback-check/`](server-feedback-check/)。其中包括适用性与失活关系、映射缺失拒绝、有效豁免、签字历史、版本冲突、异议/暂缓状态及统计规则等服务测试。
- 前端回归脚本退出码 0：34 项通过，见 [`recheck-1/results.md`](recheck-1/results.md)。它从 `validation.ts` 抽取校验逻辑、从 Vue SFC 抽取保存处理器，以 mock API 测错误/重试；不是浏览器页面、真实网络、完整前端构建或服务端集成验收。`Prettier`、SFC parse/compileScript 和 `git diff --check` 也通过，但不替代六条业务验收。
- 固定候选细节沿用 [角色与 T01 既有复核记录](../role-policy-check.md)：定向测试的服务源码/测试源码 SHA 分别为 `4006b751f6c87d938962852bed7b6a08af757318ec718d870061aba921b9efe6`、`db440327168da05bb582b2c1b918cdec7b3e0c7de67da62f195beebf58eaede9`；前端 34 项所用 `validation.ts`、`index.vue`、脚本 SHA 分别为 `82903174d28f1743aca049b787dd6099da63aa5647e95ed5e1d32f2076b87471`、`a5817f4eff90fe921238e04b4d6ca8d993b3a51b989e090923fb3cd302479bce`、`300b895151aac18f520646aa327b23c2b91b15e6bba7d641edcb695afc6282fd`。打包 JAR SHA 为 `5b8cb5a999ca2087a87f28526a166aa37ce04c30c2dafe4a02ae44b4ed3e642e`，但打包来自含既有差异的整工作树，不能归为仅含这两份后端候选。先前报告也记录：Vite 源模块 GET 200 不足以证明当前页面加载了该候选；本复核不独立验证运行进程、页面资源或 JAR 绑定关系。

## 六项验收

| 任务计划第 4 节 | 自动化证据 | 浏览器原件 | 本次判断与关闭前的具体剩余步骤 |
|---|---|---|---|
| 1. 不完整草稿可保存；确认时分别拒绝缺场景、范围、验收条件、有效评审人或确认依据，并提示实缺项 | 34 项覆盖草稿放行、逐项业务字段及记录字段缺失；81 项覆盖草稿/评审/确认不同门槛、无效或不存在人员、确认依据和版本拒绝。两套自动化足以支持代码路径，但前端保存用 mock API。 | `012` 有不完整草稿 v1；`126`（390×844）期望行为缺失时聚焦并保留输入；`122`（768×1024）同类期望行为提示；`130`（1440×1000）缺触发场景提示；`131` 有 v16 页面回读。`003–010` 有早期视口错误/稳定记录。 | **部分覆盖，未关闭。** 缺少当前候选页面上对“缺范围、缺验收条件、无效评审人、缺确认依据/证据”分别触发确认并读回版本、历史及签字未变化的成组记录；`012` 的列表能证明已有不完整记录，尚未把“保存动作→强制刷新独立读回”串成原件。保留 390、768、1440 下针对本表单的逐项错误/恢复，不扩大成产品全局响应式结论。 |
| 2. 有效不适用理由无需来源负责人/映射即可由有权且被分派人确认；空白或 `N/A` 理由拒绝 | 34 项覆盖空白、空格、`N/A`、`NA`、短理由等前端拒绝及有效理由无来源/映射；31 项 Workbench 测试覆盖服务端拒绝/正向签署；31 项 Requirement 测试覆盖有效豁免、资格和本人确认。 | `101–102` 分派、`112–113` v8→v9 有效豁免签字和来源空集、`125/127` v14 正向确认及历史；`103`、`129` 空理由拒绝，`129` 提示“尚未产生签字”。`136` 当前映射转不适用为 v19、评审中且 C=0。 | **正负核心路径已有强证据。** 267 的分派与后续本人签署、合法理由下空来源元组，以及无效理由不产生签字均有页面/历史和服务测试支持。v19 是后续从有映射切回不适用的未确认状态，属于第 3 条撤签/计 C 检查，不覆盖或推翻 v14 的有效豁免正例。本条单独未见新的未覆盖业务分支；总体关闭仍受下方共通的运行候选身份绑定条件约束。 |
| 3. 适用缺映射禁止确认；双向适用性切换触发重审；旧关系/签字留历史；合法豁免排除 L，未确认不计 C | 31 项 Workbench 测试含缺有效关系/字段映射时拒绝且不写、适用性切换/失活关系；31 项 Requirement 测试含确认快照、内容变化撤销当前签字且留历史；Review 测试覆盖历史确认不计入当前 C。 | `115/117` 精确缺映射提示；`124` 在旧的无关系豁免状态打开 L 并读到 `[]`；`132–135` 建来源 3515、关系 3518，v17→v18 由 267 确认，签字 tuple 含 `sourceId=3515`、`relationId=3518`、`mappingVersion=1`；`136` 改不适用为 v19、状态评审中、C=0；`137–138` 关系 3518 v2 历史失活，历史包含 `MAPPING` 与 `APPLICABILITY_DEACTIVATE` 两条快照；`139` v18 的签字 tuple 仍留在历史。 | **关键切换及历史留存现已可见，仍未关闭。** 新 `124` 的 L=`[]` 发生在关系 3518 创建之前，不能证明 v19 切回不适用后历史关系被排除；需在 v19 当前状态重新打开 L、保存集合结果。`139` 的旧签字留史清楚，但应独立读当前对象以确认 v19 无有效签字、C=0，旧 v18 tuple 只在历史。映射正向确认已见，但 `133` 初建截图状态为“待核验”；若任务要求映射核验状态也合格，应补记通过的核验依据，当前证据不应把候选映射说成真实业务数据质量通过。 |
| 4. 异议缺原因/后续责任信息阻断；补齐后保存、刷新、重登、历史一致；范围暂缓不冒充异议/确认 | 前端 34 项检查缺评审人、依据、责任人及下一步字段；Review 31 项含 `DEFERRED` 必需责任/复评时间及状态/C 不被范围决策替代的测试。 | `104–107` 显示异议缺项阻断、补齐后保存、版本7异议及历史；`118–121` 显示同账号双标签旧版本冲突被拒、输入保留、读最新后手工合并并刷新回读。 | **部分覆盖，未关闭。** 未见异议补齐后由同一任务明确执行强制刷新及重新登录，再核对列表、详情、历史三处一致的动作链；未见页面上以“暂缓”完成一次有效范围决策，并读回它仍为暂缓而非异议/本人确认的原件。后者只有服务端测试。现有 118–121 是同一测试账号两个标签页，不等价于两个独立登录会话/角色。 |
| 5. 服务端校验失败、可控断网和版本冲突不假成功；输入恢复、无错误签字；以最新版本重试成功 | 34 项以 mock transport 注入网络错误、冲突和服务器校验错误，断言无成功提示/输入保留/重试可提交；服务测试断言失效版本不写入历史或签字。 | `117` 页面显示精确缺映射文案，当前 v10/C=0；`127` 的完整网络事件 cursor 记录保存 `POST` 与同 requestId 的 `loadingFailed(net::ERR_INTERNET_DISCONNECTED)`；`128` 页面显示成功重试后 v15；`118–121` 显示旧版本冲突、读取最新、人工合并及 v12 回读；`130–131` 显示 1440 缺项提示及恢复后的 v16。 | **可控断网与版本冲突有可见路径，仍未关闭。** `131-online-retry-network-metadata.json` 标记 `truncated=true` 且 events 为空，故无与成功保存对应的 POST/响应事件；`128` PNG 实际为 1269×1190，不是文件名所称的 390×844，不能作为 390 下恢复成功的同视口证据。`117` 有页面错误文案，但没有对应 HTTP 状态/响应体原件。需补一份完整、未截断的成功重试请求/响应与失败前后独立读回，并在 390×844 下取得 v15 恢复图；确认失败不新增签字/历史版本。 |
| 6. 390/768/1440 视口覆盖本表单编辑、错误和恢复 | 自动化不做视口布局或真实页面交互，不能覆盖此项。 | 390：`126–127` 错误/断网输入保留；`128` v15 成功状态但 PNG 1269×1190。768：`122–123` 错误后恢复到 v13，PNG 均 768×1024。1440：`130–131` 错误后恢复到 v16，PNG 均 1440×1000。 | **768 与 1440 有同视口错误/恢复；390 尚缺同视口恢复原件，故未关闭。** 在 390×844 完成一次错误后重试并回读 v15（或更高版本），保存实际视口截图及一致的表单状态。视口证据只限定于本表单。 |

## 尚未关闭的共通条件

1. 将可见页面动作、登录测试账号、截图/DOM、服务端响应、写入后独立回读绑定到同一时间线。当前材料能识别合成项目 153、需求 1844、编辑者 266、评审者 267 和主要版本，但并非每个失败/成功操作都有成对请求与响应证据。
2. 将固定前端文件与可见浏览器加载的模块/构建身份、固定后端候选与实际运行 JAR/PID 绑定。当前 JAR 的打包范围包含既有差异；源模块 HTTP 200 和页面内容相似都不足以单独完成该绑定。
3. 页面中的数据是 TEST 合成场景。T01 关闭只说明该候选的工程验收，不代表正式业务批准、实际工资数据来源质量通过或正式发布。

**结论：T01 继续开放。** 81 项后端定向测试及 34 项前端回归通过；第 2、3、5 项本轮得到新增的重要正负路径证据，但任务计划的每项当前候选闭环仍有上表所列缺口。本结论不改写前几轮报告或封存原件。实际运行模型身份未核实。

## 第二轮追加复核：T01 原件 140–160、活动后端及计划第 4 节当前判断

本节追加，不改写首轮结论、原表或封存原件。仅读取 140–160 的 DOM/PNG、网络事件、现存服务测试源码及运行进程元信息；没有操作浏览器、数据库或业务数据。请求/期望模型为 `gpt-6-luna / max`，实际运行模型仍未核实。

### 最新原件及视口尺寸

- `140–145` 分别把“触发场景”“验收条件”“依据或异议说明”留空保存为 v20/v21/v22，再由 267 分别尝试本人确认。DOM 和 PNG 记录各自的缺项提示及“尚未产生签字”；`146` 补齐后为 v23、C=0。`155–157` 清除评审负责人后仍能看到“本人确认”按钮，实际点击提示“评审负责人（请先分派）；尚未产生签字”。因此文件名 `156-no-assignee-no-confirm-action` 不准确：按钮没有隐藏或禁用。`158` 分派恢复为 v28；`159–160` 记录 267 本人签署 v29，当前有效确认元组为 `NOT_APPLICABLE` 且 `sources=[]`，统计 C=1、T=1、D=0。
- `147` 在关系 3518 已失活、历史保留之后打开 L 下钻，结果是 `[]`；同屏显示 C=0、有效不适用1、实际来源0。它补齐了早先在关系创建之前查看 L 的时间顺序缺口。`132–139` 仍支持来源3515/关系3518的建立、v18有效映射签字、切回不适用后的重审、关系版本2失活及旧签字/关系历史留存。
- `148–150` 是同一 390×844 视口：v23异议表单断网、重试后列表为 DISPUTED v24、强制刷新后仍读回 v24。`151–152` 重新登录后仍读到 v24，并在详情表单读回期望行为、异议责任人267、异议下一步和依据。`153` 进入 REVIEWING v25；`154` 显示 DEFERRED、T=1、D=0、C=0，暂缓没有被计为异议或确认。`160` 的历史表再读到 v24 为 DISPUTED、责任人267、下一步和期望行为；v25–v28 均无签字，v29 才由267确认。此前“缺少 v24 历史读回”的缺口已补。
- `final-offline-metadata.json`、`final-retry-metadata.json` 都是 `truncated=false`、`hasMore=false`。断网阶段为保存 `POST` requestId `85833.14504` 的 `loadingFailed(net::ERR_INTERNET_DISCONNECTED)`；重试为相同保存路径的 `POST` requestId `85833.14505`，收到 HTTP 200，随后需求列表、统计、来源及能力 GET 均收到 200。它与 `149–152` 的状态和回读互相印证。服务端映射拒绝测试还断言错误码、完整提示、版本/状态/确认元组未变和记录数未增；页面 `117` 显示具体映射缺项提示。按第4节文字，不再把每个负例额外要求为 HTTP 响应体原件。

本轮按实际 PNG 像素读取：`140–147`、`151–160` 为 1269×1190；`148–150` 为 390×844。已有 `122–123` 为 768×1024、`130–131` 为 1440×1000。390 断网、重试及强制刷新现在都由实际 390×844 原件支持；没有依据文件名推断尺寸。

### 范围服务断言与 UI 覆盖

81 项服务测试中，`HrmPayrollWorkbenchServiceTest.confirmationNamesScopeAndApplicabilityFailuresInExistingOrder()` 把 `scopeState` 设为 `SCOPE_MISMATCH`，执行确认 tuple 校验，并由 `assertConfirmationIncomplete` 断言错误码 `PAYROLL_REVIEW_INCOMPLETE` 和确切文案“请补齐以下字段：需求适用范围（须与当前项目范围匹配）”。同一测试还分别断言规则版本为空、适用性为 `UNKNOWN` 时提示各自缺项。`scopeChangeInvalidatesEvidenceButNotSourceOrDeliveryVersions()` 另断言项目范围变化后可用关系数为0、需求状态回到 `REVIEWING`、semantic/delivery 版本仍为1，且使用旧范围版本的语义核验被拒绝。这些是实际服务端范围不匹配/范围变化断言；没有单独断言 `scopeState` 为 null 或空字符串。

前端34项回归验证 `opinionMissing` 字段与保存处理器，没有构造 `scopeState`；140–160 的页面原件也没有范围不匹配后点击本人确认并看到该范围提示的动作。因此范围项应单独标为“服务端负例已覆盖；未见对应 UI 覆盖”。计划写的是缺范围时拒绝并指出缺项，没有要求81项服务测试逐一镜像成页面测试；单凭缺少 UI 镜像，不应额外要求把所有服务负例搬到浏览器。若验收口径还要求该范围分支端到端显示，具体动作是：用 TEST 需求形成范围不匹配，由有权且已分派人尝试确认，并保存页面提示和签字/版本未变化原件。若“缺范围”专指空/null范围而不是现有测试的 `SCOPE_MISMATCH`，则该边界目前既无独立服务断言也无页面原件，不能声称覆盖。

### 六项验收当前矩阵

| 计划第4节验收项 | 当前原件及自动化判断 | 具体未执行步骤 |
|---|---|---|
| 1. 不完整草稿可保存；缺场景、范围、验收条件、有效评审人、确认依据分别拒绝并提示实缺项 | 场景/验收/依据由 `140–146` 逐项拒绝、补齐并读回；缺评审人由 `155–158` 拒绝并恢复；范围不匹配由上述精确服务测试覆盖。 | 仅当要求范围负例必须有页面显示时，执行上述范围 UI 动作。服务测试覆盖的是不匹配，并非空/null范围。 |
| 2. 有效不适用无需来源负责人/映射，由被分派本人确认；无效理由拒绝 | `103/129` 及 `112–113`、`125/127`、`159–160` 覆盖无效拒绝与有效豁免签字/空来源。`confirmationNamesInvalidExemptionAndAcceptsValidExemptionWithoutRelations()` 覆盖空、N/A、短理由拒绝及无关系有效豁免。 | 无计划项级未执行步骤。 |
| 3. 适用缺映射拒绝；双向切换重审；旧关系/签字留史；有效豁免排除L且未确认不计C | `117` 是具体映射提示；`114–116`、`132–139` 覆盖切换与历史；`147` 在关系失活后读到 L=`[]`、C=0；`159–160` 读到当前豁免签字 tuple 的 `sources=[]`。服务测试断言映射拒绝和无写入。 | 无计划项级未执行步骤。历史 TEST 签字不代表来源质量通过。 |
| 4. 异议缺字段阻断；补齐保存、刷新、重登、历史一致；暂缓不冒充异议或确认 | `104–107`；`148–152`；`160` 的 v24异议历史；`153–154` 的重审与 DEFERRED T1/D0/C0，已覆盖前述动作链。 | 无计划项级未执行步骤。 |
| 5. 服务端错误、断网、版本冲突不假成功；输入可恢复、不添错误签字；重试成功独立回读 | `117` 加映射拒绝服务测试；完整断网/重试元数据及 `148–152`；`118–121` 冲突、保留输入、读最新、人工合并和 v12 回读。 | 无计划项级未执行步骤；不额外要求每类负例都有 HTTP 响应体。 |
| 6. 390/768/1440 视口覆盖表单编辑、错误与恢复 | 390：`148–150`（390×844）；768：`122–123`（768×1024）；1440：`130–131`（1440×1000）。每个尺寸均有错误/编辑及恢复证据。 | 无计划项级未执行步骤。 |

### 活动后端及候选绑定边界

只读 `launchctl list` 将 `local.hrm.backend` 映射到 PID `89875`；`ps` 显示该进程启动于 `Thu Oct 22 11:53:57 2026`，可执行文件为 JDK 8 `java`。`lsof` 显示该进程打开 `/Users/waitforpro/source/github-yudao/ruoyi-vue-pro/yudao-server/target/yudao-server.jar`；该路径当前 SHA-256 为 `5b8cb5a999ca2087a87f28526a166aa37ce04c30c2dafe4a02ae44b4ed3e642e`，与打包记录一致。未读取进程环境变量或凭据。指定服务源码/测试 SHA 仍为 `4006b751…` / `db440327…`；前端 `validation.ts` / `index.vue` SHA 仍为 `82903174…` / `a5817f4e…`。活动 PID、启动时间、打开的 JAR 路径及包体哈希现已独立核对。该 JAR 是含既有其他差异的整工作树制品，不能说成只含两份后端候选。已有 Vite 源模块 GET 200 及磁盘哈希记录，但没有浏览器资源事件把截图实际加载的前端模块绑定到该 SHA；这是当前留下的候选身份证据缺口。

**本轮结论：**第4节六项的页面场景、完整网络事件和三种视口原件已补齐。计划文字本身未要求将所有服务端负例重复成 UI；范围不匹配已有精确服务断言但没有页面镜像，故作为单独覆盖边界披露，不据此扩大验收要求。若按严格端到端解释，具体剩余是补一次范围不匹配的页面拒绝；此外仍需把浏览器实际加载的前端模块与固定哈希绑定。后端运行 JAR 已按 PID、路径和 SHA 核验。所有页面为 TEST 合成数据，不代表正式批准或实际工资数据质量通过。操作者为主线程，本执行者只复核自动化与原件；实际运行模型未核实。

### Addendum: browser source binding and scope-preflight candidate build

The prior conclusion above said the browser-loaded frontend module identity was not tied to source hashes. That specific evidence gap is superseded: read-only parsing of the captured Network response bodies verifies HTTP 200 for `index.vue` (request `85833.18861`) and `validation.ts` (request `85833.18865`). Each saved raw response body matches its recorded response SHA-256, and the inline source map's `sourcesContent` is byte-for-byte identical to the corresponding current disk source. Exact details are in `scope-preflight-check/browser-resource-binding-verification.json` and the input body artifacts remain in this directory. The parent metadata reports `truncated=true`, `hasMore=false`; these two body artifacts were nevertheless available and individually hash-verified.

The scope-preflight backend candidate matched all four frozen source/test hashes. The targeted JDK 8 Maven run passed 86 tests (Workbench 31, Review 19, Requirement 36; zero failures/errors/skips), followed by a successful `yudao-server` package. Full logs and exit-code files, commands, candidate hashes, and artifact digest are in `scope-preflight-check/`. The newly packaged JAR SHA-256 is `044a0625b5410cb8be5b086625ba60554b49f789402b5ff5423f46567cd850e5`. This executor did not restart the server or run browser/UI/database actions, so this JAR is not yet asserted to be the active backend. The scope-mismatch confirmation UI recheck remains pending for the main thread. Actual runtime model remains unverified.

## 7. 最终范围预检、恢复与 T01 闭包复核

This addendum supersedes the previous T01 “scope UI action remains” conclusion. It is limited to T01’s WB-04 form subset and the synthetic TEST project 153; it does not close T02–T09, the other two WB workstreams, or any formal business approval.

The currently running backend is the packaged scope-preflight candidate. Read-only checks show `launchctl list` maps `local.hrm.backend` to PID `55351`; `ps` reports start time `Thu Oct 22 19:36:46 2026` and the JDK 8 Java executable; `lsof` shows that PID has the target `yudao-server.jar` open. The JAR at that path hashes to `044a0625b5410cb8be5b086625ba60554b49f789402b5ff5423f46567cd850e5`. The four frozen backend source/test hashes still match the candidate recorded with the passing 86-test run. The two browser-loaded Vite response bodies are HTTP 200, match their captured response hashes, and their inline source maps contain byte-for-byte copies of the current `index.vue` and `validation.ts`; the full hashes and request IDs are in `final-review/runtime-and-source-identities.json` and `scope-preflight-check/browser-resource-binding-verification.json`. Requested model was `gpt-6-luna / max`; actual runtime model remains unverified.

The last scope attempt was made while the requirement’s bound project scope was stale. `scope-final-network-metadata.json` records the confirmation POST and a matching HTTP 200 response event; `scope-final-response.json` contains application code `1050810045` and the message “范围版本已变化或尚未定义，请刷新并核对当前范围”. Capture `164` happened too early and has no visible scope alert, so it does not prove the prompt. The later `170` DOM captures the visible alert heading and recovery guidance; `171` records the stable v34 `REVIEWING` row, `C=0`, and no new confirmation. The actual trigger was a changed scope version (project scope v4 against the stale requirement), not an undefined project. The service suite separately covers mismatch, undefined project/requirement scope, and stale bound versions: `scopeMismatchIsReportedBeforeMissingGovernanceWithoutSigning`, `undefinedProjectScopeIsReportedBeforeMissingGovernanceWithoutSigning`, and `undefinedRequirementScopeAndStaleBoundVersionAreReportedBeforeMissingGovernance` in `HrmPayrollRequirementServiceImplTest`.

The recovery chain is evidenced by `172–175`: project scope v5 is restored; decision record 1407, version 3, is `EFFECTIVE` with designated approver 267 in the TEST decision view; the requirement ends at v36 `CONFIRMED`, `confirmedScopeVersion=5`, `confirmedBy=267`, with the confirmation tuple binding scope version 5 and `sources=[]`. The history view retains v35 and the final v36 snapshot. The earlier v33/scope3 positive remains historical; it is not used as proof of v36.

| T01 plan §4 acceptance | Final evidence and executed boundary | Final assessment |
|---|---|---|
| 1. Save incomplete draft; reject each missing scenario, scope, acceptance condition, reviewer, or confirmation basis with the actual missing item | Draft v1 is present in `012`; `140–146` exercise scenario, acceptance, and evidence independently; `155–158` exercise missing reviewer and restored state. For the scope branch, `170–171` now show visible stale-scope rejection, preserved recovery guidance, v34 unchanged and C=0. The 86 service tests also cover stale, mismatched, and undefined scope states and assert rejection without a new signature/history write. | Covered for T01. The UI scenario is scope drift; undefined scope states are service-tested rather than separately induced in the browser. |
| 2. Allow a valid non-applicable reason without source/owner/mapping for the assigned authorized reviewer; reject blank and `N/A` | Invalid-reason rejection is in `103/129`; positive `112–113`, `125–127`, `159–160`, and final v36 show assigned user 267, `NOT_APPLICABLE`, and an empty `sources` tuple. Backend tests cover blank/`N/A` rejection and valid no-relation confirmation. | Covered for T01. The TEST decision is not formal approval. |
| 3. Block applicable requirements without a mapping; reopen review on applicability changes; retain old relationships/signatures in history; exclude valid exemptions from L and unconfirmed items from C | `115–117` show missing-mapping rejection; `114–116` and `132–139` record both applicability transitions, relation deactivation, and old signature history; `147` records L empty after deactivation; `159–160` and `174–175` show current exempt signatures with `sources=[]`. Current unconfirmed v34 has C=0 in `171`. The 86 service tests include no-write mapping/scope rejection and transition/history assertions. | Covered for T01. Synthetic mappings prove application behavior only, not real source quality. |
| 4. Block disputes missing reason/owner/next action; save, refresh, relog, and compare history; keep defer distinct from dispute and confirmation | `104–107` cover missing fields and saved dispute; `148–152` cover offline retention, retry, forced refresh, relog, and detail; `153–154` show review restart and `DEFERRED` without dispute or confirmation. Service tests cover decision/status invariants. | Covered for T01. |
| 5. Do not report success on server validation failure, controlled network failure, or version conflict; preserve input, avoid bad signatures, and permit retry on latest version | `117` provides precise mapping error; `scope-final-*` plus `170–171` provide the live scope-preflight response, visible alert, and unchanged C/signature state; `148–150` plus complete final offline/retry metadata cover network failure and retry; `118–121` cover conflict, latest-version read, manual merge, and persistence. Current 86 service and 34 frontend checks passed. | Covered for T01. The API uses HTTP 200 with an application error code for the scope rejection; it is a rejected business response, not an HTTP transport failure. |
| 6. Cover this form’s editing, error, and recovery at 390, 768, and 1440 viewports | `148–150` PNGs are 390×844; `122–123` are 768×1024; `130–131` are 1440×1000. Each set contains error/edit and recovery states. Scope screenshots `164`, `170–175` are 1269×1190 and are not claimed as any of those viewport cases. | Covered for the T01 form only. |

The current candidate’s automated evidence is 34 frontend regression checks and 86 backend service tests (Workbench 31, Review 19, Requirement 36), all passing; the backend package succeeded and its JAR is the one open by PID 55351. The exact logs are retained under `scope-preflight-check/` and the earlier T01 automation evidence. `git diff --check` completed with exit code 0; its stdout and exit code are in `final-review/`. A lightweight credential-marker scan of 26 round-specific DOM, PNG-byte, and scope-response/network files found no common credential labels or token shapes; it was not OCR. `final-review/png-dimensions.txt` records actual image dimensions. No code, database, or business data was changed by this executor.

**T01 technical acceptance: close.** The planned T01 paths now have passing automation, current backend/frontend identity evidence, positive and negative browser records, final scope rejection and recovery, and independent readback/history. The completion applies to the WB-04/T01 subset only. The project, requirement, approver, scope, and decision in these records are synthetic TEST fixtures; this does not establish formal approval, real payroll accuracy, deployment readiness for three WBs, or T09 completion.
