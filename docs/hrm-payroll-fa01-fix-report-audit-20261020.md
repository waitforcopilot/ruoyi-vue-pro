# FA-01 处理报告独立审计

审计日期：2026-10-20（Asia/Shanghai）。对象是 `docs/hrm-payroll-fa01-fix-report-20261020.md` 及其证据目录 `docs/hrm-payroll-fa01-evidence-20261020/`。本文件只记录这次审计，不改该处理报告、原阶段审计或业务代码。

## 1. 审计结论

**处理报告在它自己划定的工程边界内可以维持。** `dependencies()` 已从全租户 ROOT、全量 ANCHOR 和全量用户事实，收窄到本项目根、本项目授权引用的根、这些根实际引用的 `projectId=0` 锚点，以及明确人员引用。依赖令牌比对仍在同号幂等返回之前。修复前反例日志、修复后 131 项与最终 133 项日志、当前 jar 与 HRM class、以及本机 MySQL 中的 1352 发布行，与报告中的工程数字一致。

这个结论只覆盖发布依赖令牌。MB-01、AV-01、F-07、F-15 仍按报告所写开放。S5 仍是 7/8，S6 仍是 6/8。本审计没有重跑 Maven，也没有重新点击内嵌浏览器。

可以继续引用的范围，和不能外推的范围，见第 6 节。

## 2. 方法与边界

对照处理报告第 1–6 节和证据索引，核对当前 `work` 历史、两份源码 SHA-256、原审计文件 SHA-256、私有测试与打包日志、fat jar 与 `yudao-module-hrm/target/classes`、run3 的 20 个 xlsx 哈希、1352 下载文件哈希，以及证据 AX、截图和脚本。另对运行中的 `hrm_local_20261017` 做只读计数。发布路径直接读当前 `HrmPayrollGovernanceReleaseService` 相对 `5f8fa3d9a0` 的差异，并读导出服务的授权戳。

本审计没有跑迁移、没有改授权、没有发布、没有提交或 push。健康检查只确认审计当时后端 `{"status":"UP"}`、前端 HTTP 200，用来核对环境仍在，不代替 09:30 的那次重启记录。

## 3. 已核对成立的主张

| 处理报告主张 | 本审计结果 |
| --- | --- |
| 起点 `work` / `5f8fa3d9a0b607073748b6b60c4cf4fced75d8bb` | 该提交是实现提交的父提交 |
| 实现提交 `f64de5524c41850b335ec762c67a9007de24eef5` 已进入 `origin/work` | 该 SHA 是当前 `origin/work` 的祖先。提交说明为 `fix(hrm): isolate release dependency tokens by project`，时间 09:31:43 +0800。当时的远端尖端记录在 `delivery.json`；当前远端尖端已不是这个 SHA，见第 5 节 |
| 回执提交只补回执和 `delivery.json` | `50db82ea68`（09:32:15）只改处理报告和 `delivery.json`，没有业务源码 |
| 源码哈希与 candidate.json 一致 | 当前 `HrmPayrollGovernanceReleaseService.java` 为 `9bc3562a…adfabf`，治理测试为 `b560177e…735fc69`。`f64de552` 之后这两份文件没有再变 |
| 最终 jar `619d2cba…688af2`，HRM class 1,109 个且逐字节相同 | 当前 `yudao-server/target/yudao-server.jar` 哈希相同。本地 class 与 `BOOT-INF/lib/yudao-module-hrm-2026.09-jdk8-SNAPSHOT.jar` 均为 1,109，差异 0 |
| 原审计与复核保持只读 | 两份文件当前 SHA-256 与 candidate.json 的 `unchangedInputs` 一致 |
| 修复前反例失败 | `fa01-before-20261020.log` 的 SHA-256 为 `01f40b8a…77fd78`，与 tests.json 相同。失败点是 `unrelatedProjectRootAnchorAndPersonnelCannotInvalidatePublishOrExactRetry:91`，期望令牌与实际令牌不同；Tests run 1，Failures 1，BUILD FAILURE |
| 首轮 131、最终 133 | `fa01-after-20261020.log` 为权限 9、评审 17、工作台 25、项目 26、治理 23、需求 31，合计 131，BUILD SUCCESS。`fa01-final-tests-20261020.log` 的 SHA-256 为 `33a86236…30d86c`，治理变为 25，合计 133，失败 0、错误 0、跳过 0。四个新测试方法都在当前测试类中 |
| 依赖校验先于同号返回 | 当前 `publishRead` 先 `need(令牌相等)`，再按 `objectKey` 返回已有 release。`precheckRead` 与 `publishRead` 仍调用 `store.lock()` |
| 快照格式、下载再鉴权和表结构未改 | 相对父提交，服务差异只在人员字段集合、`dependencies()` 和两处调用增加 `actor`。快照仍用 `store.all(项目,"ROOT")` 写 `controlledRoots` |
| 甲 105 发布 1352 / 版本 3，幂等号仅 1 条，乙 148 无 release | 只读 SQL：`TEST-FA01-IAB-20261020-0928` 的 RELEASE 为 1 行，id 1352，快照内版本 3，`draft=false`，mode 为 `TEST合成验证，非正式业务批准`。项目 105 的 RELEASE 为 900、1347、1352。项目 148 的 RELEASE 为 0。1350、1354、1356 均为 `PENDING_SIGNATURE`，`signedBy` 为空。148 的授权和需求行均为 0 |
| 20 份旧负载文件哈希留档 | `xml-reverification.json` 的 20 个 SHA-256 与 `stage6-20261019/performance-run-3/export-01.xlsx` 至 `export-20.xlsx` 全部一致。核验器脚本哈希为 `740c4ee6…408ae0`，与证据所写相同。20 个文件的工作表部件数量都是 6。本审计没有重跑单元格比较 |
| 1352 下载文件 | `release-1352.xlsx` 的 SHA-256 为 `12c06cec…899047`，与 `new-release-workbook.json` 相同，六表名称与投影常量一致。任务 1358 当前状态为 READY |

实现提交的 22 个路径是服务、治理测试、处理报告、进度附记和本轮证据。原审计文件不在这个提交里。

## 4. 代码路径

父提交的依赖集合是 `store.all(null,"ROOT")`、`store.all(null,"ANCHOR")` 和未经筛选的 `userFacts()`。`Store.all` 在项目参数为空时不附加项目条件，因此原 FA-01 描述的跨项目根、全量锚点和全局用户事实，会先改变令牌，再阻止同号命中旧 release。当前实现去掉了这三处全量纳入：

- 根取本项目 `ROOT`，再按授权 `rootRecordId` 用 ID 排序补入尚未出现的根。
- 锚点取 `projectId=0` 的 ANCHOR，并只保留上述根的 `anchorId`。正式锚点读写本来就使用 `projectId=0`，与快照里的锚点来源一致。
- 用户事实保留当前发布人、项目负责人，以及 `PERSON_FIELDS` 中的明确人员字段。对象主键不在该字段集中。

H2 用例 `unrelatedProjectRootAnchorAndPersonnelCannotInvalidatePublishOrExactRetry` 覆盖另一项目登记并签署根、插入未引用锚点、以及未引用用户事实变化后的同号返回。`relatedAnchorAndPersonnelChangesStillInvalidatePreparedPublication` 覆盖发布人昵称和已引用锚点变化时拒绝，且不产生 release。决议撤销和发布人停用两个用例保证旧的失败边界仍在。发布人停用用例走的是账号状态门禁；发布人昵称变化才直接锁定令牌中的人员事实。

令牌仍比冻结快照宽的部分，报告已经写明：授权引用的其他项目根会进入令牌，快照中的 `controlledRoots` 仍只含本项目根。这是保守冲突，不是把无关根重新放回来。

## 5. 证据限制与残留

以下各项不推翻第 1 节的边界内结论。它们限制这句话可以被引用到哪里。

1. **当前远端尖端已经继续前进。** 审计时 `git ls-remote origin refs/heads/work` 为 `bfa68aa559cc524e33e68cf6201f33a31c361291`。处理报告回执里的 `git ls-remote` 核对，对应 `delivery.json` 在 09:32:15 记录的 `f64de552`。其后还有回执提交，以及 09:36:38 的文档归档提交 `bfa68aa559`。后者把原先未跟踪的审计和交接材料纳入 `origin/work`，没有修改 FA-01 业务源码。不能把回执句读成当前远端仍停在实现提交。

2. **1050810003 不是依赖专用错误码。** `HrmPayrollGovernanceStore.need` 在条件失败时抛 `PAYROLL_REVIEW_INCOMPLETE`，数值就是 1_050_810_003，文案仍是评审补齐提示。范围、门禁、请求摘要和依赖令牌共用它。留存的 `check.py` 在相关负例里使用了新的随机请求号和旧令牌，断言的是发布被拒绝。它证明旧令牌不能再发布，不证明同号重试被拒绝。同号重试的页面结果在 AX 中。

3. **失败文案和冻结六表正文由全量 AX 支持，对应截图视口没有展示这两段内容。** `browser-related-rejected.txt` 含“发布或重试未完成，说明及请求号已保留”，请求号仍是 `TEST-FA01-IAB-20261020-0928`，发布表仍只有 1352 / 版本 3。`browser-related-rejected.png` 与 `browser-retry.png` 只有右上角约 633 个像素不同，视口上看不到这句失败文案。`browser-frozen.txt` 在全量树中含 SUMMARY、DEC-604、GRANT-PUBLISH，焦点在 1352 的“查看冻结六表”；`browser-frozen.png` 的视口仍停在发布列表。本审计没有重做这次点击。

4. **1352 工作簿的 `reexportOrderedCellsIdentical` 是同一文件自比。** `export_check.py` 调用 `verify(projection, book, book)`。报告和 JSON 已写明这是单文件校验。该字段不能引用为旧版本重导出一致。

5. **导出任务授权戳仍纳入全部 `projectId=0` 锚点。** `HrmPayrollGovernanceExportService.stamp` 仍对 `store.all(0L,"ANCHOR")` 做摘要，并在生成、状态和领取时要求与创建时相同。无关新锚点仍可能让已排队的导出任务失败。ROOT 在这条路径上已经按任务项目过滤，人员事实也不在这个戳里。FA-01 原文针对的是发布预检查和同号重试；处理报告没有宣称导出戳已收窄。关闭句不能外推到导出任务。

6. **全局用户查询和租户串行锁仍在。** `userFacts()` 与 `store.lock()` 没有按本项目缩小。报告已写明这一点。

7. **打包日志早于最终 133 项日志。** 包日志时间为 09:24:58，133 项日志为 09:25:33。多出来的是两个测试方法。当前主类与 jar 逐字节相同，没有看到运行包落后于当前业务源码。

8. **本审计没有重跑 133 项测试，也没有重做 40,245 字段的单元格比较。** 这两项依据是日志复算、文件哈希和六表数量。

## 6. 对原结论的使用

可以继续引用：

- FA-01 的发布依赖令牌已按项目、引用根、引用锚点和明确人员收窄；同号返回仍在令牌和当前门禁之后。
- 修复前反例失败，最终相关测试日志为 133 项通过；本机 1352 是 TEST 合成发布版本 3，148 没有 release，三条新根未签署。
- F-07、F-15、MB-01、AV-01 未关闭。S5 为 7/8，S6 为 6/8。正式批准材料不能用本次 TEST 发布代替。

不可以引用：

- 本审计已经重跑 133 项测试、重做 20 份负载文件的单元格比较，或重新完成浏览器验收。
- 1050810003 专指依赖冲突，或留存 API 负例本身就是同号重试。
- 失败截图视口已经显示“发布或重试未完成”，或冻结截图视口已经显示六表正文。
- 无关锚点不再影响导出任务的创建后校验。
- 当前 `origin/work` 的尖端仍是 `f64de552`。
- 全部 PRD、正式工资、税务或付款验收已经通过。
