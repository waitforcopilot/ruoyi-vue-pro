# FA-01 后续处理报告独立审计

审计日期：2026-10-20（Asia/Shanghai）。对象是 `docs/hrm-payroll-fa01-followup-fix-report-20261020.md` 及其证据目录 `docs/hrm-payroll-fa01-export-evidence-20261020/`。本文件只记录这次审计，不改该处理报告、上一轮独立审计或业务代码。

## 1. 审计结论

**后续处理报告在它自己划定的工程边界内可以维持。** `HrmPayrollGovernanceExportService.stamp()` 已把授权戳里的锚点从全部 `projectId=0` ANCHOR，收窄到本项目根以及项目授权明确引用的根所实际引用的锚点。无关锚点不再进入戳摘要。创建、生成、状态、领取仍持有租户锁，并保留当前投影、项目和项目授权；生成、状态、领取在放行前重新比较授权戳。领取仍要求任务处于 READY。

上一轮独立审计第 5 节的 8 条限制，按本报告第 1 节的关闭边界核对后可以维持。其中第 5 条的导出戳纳入全部锚点，已由当前源码和两个新 H2 用例收口。F-07、F-15、MB-01、AV-01 仍开放。S5 仍是 7/8，S6 仍是 6/8。TEST 下载成功不能当作真实授权，也不能当作正式工资、税务或付款验收。

本审计没有重跑 Maven，没有重做 638 字段的投影对照，没有重新点击内嵌浏览器，也没有再次插入锚点或重发发布请求。

## 2. 方法与边界

对照处理报告第 1–5 节、文末回执和证据索引，核对当前 `work` 历史、`origin/work`、实现提交 `c112770ed9` 与回执提交 `ac5ecf3d71` 的路径、导出服务相对父提交的差异、两个新测试、上一轮审计文件 SHA-256、候选源码哈希、私有测试与打包日志、fat jar 与 `yudao-module-hrm/target/classes`。只读查询本机 `hrm_local_20261017` 中的 1352、1359、1360、1361、1362。用处理报告所写的同一请求身份公式，复算 1352 已保存的 `requestDigest`。核对浏览器下载原件、API 下载件和四张新截图的视口。

本审计没有跑迁移、没有改授权、没有发布、没有提交或 push。健康检查只确认审计当时后端 `{"status":"UP"}`、前端 HTTP 200，用来核对环境仍在，不代替 20:14 的那次提交前检查。

## 3. 已核对成立的主张

| 处理报告主张 | 本审计结果 |
| --- | --- |
| 起点 `work` / `bfa68aa559cc524e33e68cf6201f33a31c361291` | 该提交是实现提交的父提交 |
| 上一轮审计原文及 SHA 保持不变 | 当前 `docs/hrm-payroll-fa01-fix-report-audit-20261020.md` 的 SHA-256 为 `5a9c259b…f39dce54`，与 candidate.json 的 `readOnlyAuditSha256` 一致。回执提交没有再改这个文件 |
| 实现提交 `c112770ed9bb97d8bbd677f7a8e39864fbbe88ba` 已进入 `origin/work` | 该 SHA 是当前 `origin/work` 的祖先。提交说明为 `fix(hrm): exclude unrelated anchors from export authorization stamps`，时间 20:14:53 +0800。回执记录时的远端尖端是这个 SHA；当前远端尖端已是回执提交，见第 5 节 |
| 回执只补处理报告和 `delivery.json` | `ac5ecf3d71`（20:15:29）只改这两个文件，没有业务源码。`delivery.json` 的 `observedAt` 与该提交时间相同 |
| 源码哈希与 candidate.json 一致 | 当前导出服务为 `e45374aa…92312dfd`，治理测试为 `baa9be43…d40318c1`。实现提交之后这两份文件没有再变 |
| 最终 jar `90b6d03f…4ebcb036`，HRM class 1,109 个且逐字节相同 | 当前 `yudao-server/target/yudao-server.jar` 哈希相同，文件时间 09:49:29。本地 class 与 `BOOT-INF/lib/yudao-module-hrm-2026.09-jdk8-SNAPSHOT.jar` 均为 1,109，差异 0 |
| 修复前反例是 1 个测试、0 个断言失败、1 个服务异常 | `fa01-export-before-20261020.log` 的 SHA-256 为 `0d425bfe…efe87b6e`，与 candidate.json 相同。失败点是 `unrelatedAnchorCannotInvalidateQueuedOrReadyExport:132`，即无关锚点插入后的 `generate`；Tests run 1，Failures 0，Errors 1，BUILD FAILURE，结束于 09:48:20 |
| 修复后六组共 135，失败 0、错误 0、跳过 0 | `fa01-export-tests-20261020.log` 的 SHA-256 为 `c5890739…f0d26d20`。权限 9、评审 17、工作台 25、项目 26、治理 27、需求 31，合计 135，BUILD SUCCESS，结束于 09:49:09。当前治理测试类共 27 个 `@Test`，含两个新方法。报告中的“来源 25”对应 `HrmPayrollWorkbenchServiceTest` |
| 135 项完成后再打包 | 打包日志结束于 09:49:29，晚于测试日志，BUILD SUCCESS。jar 修改时间与该结束时间相同 |
| 前端没有源码修改 | `bfa68aa559..HEAD` 没有前端路径。运行代码差异只在导出服务和治理测试 |
| 1359 在锚点 1360 之后仍 READY，1361 在锚点 1362 之后仍 READY | 只读 SQL：1359、1361 均为项目 105 的 JOB，`releaseId=1352`，`actorId=266`，状态 READY。1360、1362 均为 `projectId=0` 的 ANCHOR，状态 `TEST_FIXTURE_UNREGISTERED`，mode 为 TEST，无 custodian、signer、actions。没有任何 ROOT 的 `anchorId` 指向这两个锚点 |
| 浏览器下载的 1361 原件与 API 领取字节一致 | `~/Downloads/薪酬评审-快照1352-任务1361.xlsx` 与私有目录 `release-1352-job1361.xlsx` 的 SHA-256 都是 `355be072…a6aa5aa0`，与 workbook.json、api-iab.json 相同。1359 的 API 文件是另一个哈希 `1a318de5…1fcd26bb` |
| 下载文件恰好六表 | 1361 原件的工作表元素为评审版本说明、功能需求清单、数据源清单、字段映射、问题与待办、范围与变更，工作表部件 6 个 |
| 同一原发布请求在摘要匹配后被 1050810003 拒绝，且没有新增 release | 按私有 `check.py` 的身份公式复算，当前 1352 的 `requestDigest` 仍与该公式相等。请求号仍是 `TEST-FA01-IAB-20261020-0928`。项目 105 的 RELEASE 仍只有 900、1347、1352；全库 RELEASE 最大 id 仍是 1352。1352 为 `draft=false`，快照内版本 3，mode 为 `TEST合成验证，非正式业务批准`。项目 148 的 RELEASE 为 0。错误码常量 `PAYROLL_REVIEW_INCOMPLETE` 的数值是 1_050_810_003。本审计没有重发该请求 |
| 发布依赖代码和上一轮阶段材料未改写 | 相对父提交，`HrmPayrollGovernanceReleaseService` 不在差异中。全局 `userFacts()` 与 `store.lock()` 仍在 |

实现提交另有 `docs/hrm-payroll-fix-progress-20261017.md` 的 4 行进度附记。处理报告第 4 节的“只改”清单没有写出这个文件。附记内容与本轮数字一致，不改变导出戳结论。

## 4. 代码路径

父提交的 `stamp()` 把 `store.all(0L,"ANCHOR")` 整表放进摘要。当前实现保留当前投影、项目记录和本项目授权，根集合先取本项目 `ROOT`，再按授权 `rootRecordId` 用 ID 排序补入尚未出现的根；锚点仍先读取 `projectId=0` 的 ANCHOR，摘要里只保留上述根实际引用的 `anchorId`。`store.all` 按 ID 升序，过滤后的锚点顺序稳定。

四个入口仍调用 `store.lock()`。`create` 计算并保存新戳；`generate`、`status`、`claim` 在使用缓存结果前要求已保存的戳等于当前重算值。`claim` 同时要求状态为 READY，并校验任务所属项目和 actor。缓存字节只在这些检查之后解码。本轮没有迁移或回写旧 `authorizationStamp`，因此旧规则生成的任务会在后续时点失败关闭。这与报告的兼容边界一致。

`unrelatedAnchorCannotInvalidateQueuedOrReadyExport` 在 QUEUED 后插入未引用锚点，再生成、读状态并领取；READY 后再插入一个未引用锚点，状态保持 READY，领取字节与前一次相同。`referencedAnchorChangesStillRejectQueuedAndReadyExport` 在相关锚点 `materialVersion` 变化后，拒绝 QUEUED 生成以及 READY 状态和领取。已有的根撤销用例仍覆盖生成、新建、状态和领取四个拒绝点。修复前日志的失败行就是第一个新用例的生成步骤。

发布依赖路径没有回到全量锚点。`dependencies()` 仍只纳入引用锚点和明确人员；`publishRead` 仍先比较依赖令牌和请求摘要，再按幂等号返回已有 release。快照中的 `controlledRoots` 仍只含本项目根。导出戳和发布令牌一样，会纳入授权明确引用的其他项目根。这是报告已经写明的保守范围。

## 5. 证据限制与残留

以下各项不推翻第 1 节的边界内结论。它们限制这句话可以被引用到哪里。

1. **当前远端尖端是回执提交，不是实现提交。** 审计时本地 HEAD 与 `git ls-remote origin refs/heads/work` 都是 `ac5ecf3d7117837879aca719bf0e2c660cfd1743`。`delivery.json` 在 20:15:29 记录的远端是 `c112770ed9`。处理报告已经写明该观察值不是永久尖端。不能把回执句读成当前远端仍停在实现提交。

2. **1050810003 仍是通用码。** 摘要身份可以在当前库上复算，并且与 1352 的已存摘要相等。留存脚本先做这个断言，再发送原请求。api.json 与 api-iab.json 记录拒绝码为 1050810003。本审计没有重放 HTTP 发布。这个码仍由范围、门禁、请求摘要和依赖令牌共用，不能单独当作依赖冲突的证明。

3. **新截图视口补上了上一轮看不到的三段内容；领取视口不展示锚点 1362。** 本审计查看了四张 PNG。`browser-failure-visible.png` 的视口含“发布或重试未完成，说明及请求号已保留”和固定快照 1352。`browser-frozen-visible.png` 的视口含快照 1352、六表标题，以及 SUMMARY、DEC-604、GRANT-PUBLISH。`browser-decision-body.png` 的对话框视口含 DEC-604 和“TEST 合成材料”正文。`browser-claim-visible.png` 的视口含任务 1361、快照 1352、READY 和“领取XLSX”。1362 是 SQL 夹具，不在这张领取视口里；它的存在由数据库行和 api-iab.json 支持。对应 AX 是整页树，包含视口以外的内容。

4. **638 字段仍是单文件校验，本审计没有重跑。** workbook.json 写明 `sameFileOnly=true`、`twoDistinctFilesCompared=false`，并且没有输出 `reexportOrderedCellsIdentical`。本审计确认六表名称和浏览器原件与 API 文件字节相同。产生这 638 个字段的冻结投影输入不在证据目录中，因此本审计没有重做单元格对照，也没有把 1359 与 1361 两个不同哈希说成历史重导出一致。

5. **摘要已排除无关锚点；读取仍先装载全部 `projectId=0` 锚点。** 无效化问题已经收口。`stamp()` 仍调用 `store.all(0L,"ANCHOR")` 后再过滤。报告没有宣称这条读取被删除。

6. **全局用户查询和租户串行锁仍在。** `lockUserFacts` 仍是 `SELECT id,nickname,status FROM system_users WHERE deleted=0 ORDER BY id FOR UPDATE`。`store.lock()` 仍锁 `projectId=0` 的 LOCK 行。报告把这两者保留为设计边界，没有据此宣布性能结论。

7. **135 项和打包日志早于当晚的浏览器回归，包体与当前源码一致。** 日志和 jar 都在 09:49，浏览器证据在 20:09–20:12，实现提交在 20:14:53。candidate.json 的源码哈希与当前文件一致，1,109 个 class 与 jar 逐字节相同。本审计没有重跑这 135 项。通过日志能看到各类合计，不能看到每个通过用例的单独结果行。

8. **文件清单少写了进度附记。** 实现提交除导出服务、治理测试、本处理报告、证据目录和上一轮审计归档外，还追加了 `docs/hrm-payroll-fix-progress-20261017.md`。这不改变第 4 节代码路径。

## 6. 对原结论的使用

可以继续引用：

- 导出授权戳已按本项目根、授权引用根和这些根实际引用的锚点收窄；无关 `projectId=0` 锚点不再使已排队或已就绪的导出任务失败。
- 修复前反例在生成步骤失败；最终相关测试日志为 135 项通过。本机 1359、1361 在惰性 TEST 锚点 1360、1362 存在时仍为 READY。1361 的浏览器原件与 API 下载字节相同，工作表为六表。
- 原发布请求号的当前摘要仍等于 1352 已存摘要；项目 105 没有出现新的 release。1050810003 仍是通用评审补齐码。
- F-07、F-15、MB-01、AV-01 未关闭。S5 为 7/8，S6 为 6/8。正式批准材料不能用本次 TEST 下载代替。

不可以引用：

- 本审计已经重跑 135 项测试、重做 638 字段对照，或重新完成浏览器验收。
- 1050810003 专指依赖冲突，或本审计已经重放了那次发布拒绝。
- 领取截图视口已经展示锚点 1362，或 1359 与 1361 两份不同哈希的文件已被证明为历史重导出一致。
- 全部 `projectId=0` 锚点已经不再被导出戳读取，或全局用户查询和租户锁已经按项目缩小。
- 当前 `origin/work` 的尖端仍是 `c112770ed9`。
- 全部 PRD、正式工资、税务或付款验收已经通过。
