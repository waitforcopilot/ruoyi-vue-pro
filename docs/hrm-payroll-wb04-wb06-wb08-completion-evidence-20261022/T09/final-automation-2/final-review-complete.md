# T09 最终复核补记：1455 冻结与 1457 下载

日期：2026-10-23（Asia/Shanghai）。这是本机 TEST 工程验收的内部复核附件，供最终 fix 报告引用；不是 Cursor 独立审计、正式业务批准或最终 fix 报告。请求模型为 gpt-6-luna / max；实际运行模型未核实。

## 下载件、冻结对象与全字段重建

T09/012 的原始 AX 与截图显示治理页面中的任务 1457 状态 READY、快照 1455、范围为“本期完整确认需求基线”，并显示“领取 XLSX”。浏览器下载事件与落盘文件身份记录指向 release1455-job1457-browser.xlsx，大小 195,902 bytes，SHA-256 823c2b41b9f187086bc291409276b2220ea9556ac8c0cf9343ce2a0ed75bc3eb。该次下载没有独立捕获 HTTP 状态；不得引用 T06/job1454 的 HTTP 200 来替代。

在 hrm_local_20261017 中以 SET SESSION TRANSACTION READ ONLY 和 START TRANSACTION WITH CONSISTENT SNAPSHOT 只读读取 release/job 两行，随后回滚；没有执行写语句。数据库快照确认 RELEASE 1455 属于 project 153、tenant 1，发布版本 4、范围版本 5、TEST 合成性质；冻结 data_json 为 702,850 bytes，SHA-256 3d56b6fdf43e20e60cda3412a0c986fc2c92e01d2cfca038445aef5c13c80edd。JOB 1457 属于同一 project，actor 266、releaseId 1455、状态 READY。数据库任务保存的 XLSX 字节与浏览器文件均为 195,902 bytes，SHA-256 完全一致。

复核脚本读取冻结 release 的完整 snapshot，按全项目授权投影重建六表期望对象，再调用既有 verify_workbook.py 的 load() 解析器重建浏览器 XLSX。除每个任务独有的 EXPORT-METADATA 父记录及其明细外，没有排除业务记录或字段。六表逐记录、逐字段精确相等，共 85 条业务记录、474 个字段，0 缺失、0 多余、0 不匹配。现有解析器同时检查六表名称及次序、八列表头、公式、父子引用和文本分片完整性、行高边界。

| 工作表 | 记录数 | 字段数 | 结果 |
|---|---:|---:|---|
| 评审版本说明 | 33 | 164 | 精确相等 |
| 功能需求清单 | 2 | 52 | 精确相等 |
| 数据源清单 | 3 | 33 | 精确相等 |
| 字段映射 | 2 | 22 | 精确相等 |
| 问题与待办 | 43 | 175 | 精确相等 |
| 范围与变更 | 2 | 28 | 精确相等 |

冻结的 REQ-1845 为版本 12，由 279 确认；范围版本 5、需求语义版本 8，签署来源元组为 source 3578 / semantic 1 / relation 3583 / mapping 3。此快照的 mode 明确标为 TEST，不表示正式业务批准或发薪依据。

机器结果见 release1455-job1457-full-field-reconciliation.json，可复跑脚本见 reconcile-release1455.py。脚本只写上述结果 JSON，不修改数据库或工作簿。

## T09 可见回归闭环

- 003 缺少异议责任人和下一步时拒绝提交并保留输入。004 刷新后的主列表状态为 REVIEWING v11；DISPUTED v10→REVIEWING v11 的转换由 006 历史记录证明，责任和下一步由 266 保存。
- 007 在独立刷新后由 279 本人确认；008 历史记录显示 CONFIRMED v12 及对应 scope 5 / semantic 8 / source 3578 semantic 1 / relation 3583 / mapping 3 元组。
- 009/010 显示当前确认预检通过并持久化 release 1455 v4；011 读取其冻结 REQ-1845，确认同一 v12 与“当前签字有效（发布时）”。012 关联真实可见 IAB 下载任务 1457，下载件与数据库 JOB 字节一致。
- browser-console-warnings-errors.json 中有 VueRouter next() 与 ElementPlus underline 弃用 warning；未见 console error。本结论只覆盖已保存页面和当前回归，不宣称产品无 warning。

## T04 / T06 / T07 / T08 closure 建议

这四项可建议按 TEST 范围关闭，正式业务、生产数据库或 Cursor 独立审计均不在此结论内。

- **T04**：021 原命名不准确，原始浏览器证据显示 issue 3590 已成功关闭，类别 DATA_READINESS、期限 10/31；这不证明数据已交付。026 才是 mapping2 旧绑定拒绝且保留输入，027 刷新后仍为 REVIEW。T04 联合 Surefire 23/23。
- **T06**：当前 IAB job1454 有独立捕获的 HTTP 200 与六表全字段重建；126 条记录 / 1,130 字段等于冻结 projection 和 job1429。长文本与空表原生 Excel 证据仍绑定旧文件 SHA 6e3c5ead…adecbaad / 632eb7f…e06fe16。final-automation-2 的新 POI fixture 是另一次输出：长文本 SHA 0e90e7fea8f3a2cb0071f57474c294b8f961c054d30c725b066b34b3223976cc；空表 SHA f6d8005dcaed6347bbaf4da8669b694e8d7143397bd8eb78e59d5d141be1a178。空表后缀以本句更正，不能与长文本后缀混用；新 fixture 没有替代旧原生查看文件。
- **T07**：原有 browser 023–028 覆盖旧 mapping2 依赖变化后的阻断、新 mapping3 请求发布 1452 v3 及幂等重试。最终链由 1455 v4 冻结、IAB 实际下载及上述 release/job 数据库和六表重建补齐。job1457 的 HTTP 状态没有独立捕获，因此只引用可见下载事件、实际文件和数据库任务字节身份，不写 HTTP 200。
- **T08**：浏览器 001–010 覆盖受限读取/发布、撤权后的旧任务拒绝与新建拒绝、另一有效成员使用自己的 job 下载；联合 HrmPayrollT08FrozenExportContractTest 16/16。结论仅限该组 TEST 用户、项目和对象。

## 自动化与运行制品身份

final-automation-2 在 JDK 8u492 / Maven 3.9.16 下运行 11 个 payroll 测试类，共 271 tests，0 failure/error/skipped；三个前端行为脚本合计 85 项通过；Prettier、ESLint、Stylelint、Maven package 与 Vite dev build 均退出码 0。完整命令、原始日志和 Surefire 报告位于本目录。HEAD 为 fdc503dca517b4dad4ea136f73bd9fbe92e11c59，执行前后候选源文件及 tracked diff 哈希不变。

运行身份结论不只基于 build 成功：后端 PID 55351 从 yudao-server/target/yudao-server.jar 启动；jar SHA 为 044a0625b5410cb8be5b086625ba60554b49f789402b5ff5423f46567cd850e5。当前 exporter source SHA d244b492cd3e90ff1c03f8e255edcda9985abfccbad4991433492db259e8e4e0 对应的 target class SHA 66af4a3fb34ca91ac852110e8bb9e905557234e4564f91313b46adb2cf2c116f 与 jar 内 HRM class 字节一致；ProjectionService 的 target class 88c52d7c8d46d8cbc14734519f1e4a01c54609cdf0e05891fca7f66d21da6b80 也与同 jar 内 class 字节一致，逐类比较结果见 projection-class-byte-check.json。进程指向该 jar 路径，package 后 jar 未变。没有从 JVM dump 全部已加载 class，因而该证据界定为当前运行 jar 路径及相关打包 class 对应，不扩展成全量内存加载证明。

前端 PID 86218 的 Vite dev server 实际 GET 治理页 SFC 返回 200，响应 121,003 bytes、SHA-256 e0ac7667cb0d3fc481cdf57d421b0ece781a6ed03f2bf9514714c80b06d68279；该响应包含当前页面模块内容，服务类型为 Vite dev，不是静态 dist。jar、class 和 SFC 证据详见 runtime-artifact-identity.json 与 vite-runtime/。

## 敏感值扫描与文件状态

对 T01–T09 证据目录内 615 个文本文件（共 7,247,158 bytes）按 credential/password、authorization、cookie、bearer/JWT 和 credential URL 模式扫描。唯一命中是本复核脚本通过配置键读取外部私有配置、生成临时 MySQL option file 的代码引用；不是证据中的 literal credential/token/cookie 值。字面值候选为 0；未打印或保存任何值。结果见 evidence-credential-scan-summary.json；这是模式筛查，不替代凭据管理或全格式秘密扫描。

相关 T09 证据文件及本补记目前均未被 git ignore，且处于未跟踪状态；git status 将目录显示为 ?? T09/。没有提交。供 coder 引用或纳入最终 fix 报告前，应按项目最终证据清单决定是否跟踪这些文件。

## 最终边界

建议 T04、T06、T07、T08 和 T09 按本机 TEST 工程范围关闭。下载 1457 的可见 IAB 事件和实际文件、数据库冻结身份及 474 字段重建均通过；其 HTTP 状态未独立捕获。未重启服务、未做产品或测试源码修改、未写数据库、未提交，也未修改最终 fix 报告。正式批准、真实供数/映射交付和生产验收仍须独立依据。
