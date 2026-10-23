# T09 final-automation-2 内部执行摘要

日期：2026-10-23（Asia/Shanghai）。内部自动化和候选身份记录，不是 Cursor 独立审计、最终 fix 报告或正式业务验收。请求执行模型 gpt-6-luna / max；本次实际运行模型仍未核实。

## 候选与边界

- HEAD：`fdc503dca517b4dad4ea136f73bd9fbe92e11c59`，执行前后相同。
- 20 个列明的源文件、测试和前端回归脚本执行前后 SHA 全部相同；追踪文件 diff SHA 前后仍为 `5a634e02acecda6f0f3c131359f76e0b6395ebbd722a91ddad1fc081bcb215a3`。
- 没有改产品源码、测试源码、数据库或最终 fix 报告；没有重启服务、提交或执行部署。新证据均存本目录。
- 本轮 TEST 使用 Maven/H2，T06 新生成 XLSX 只作为独立 POI 回归 fixture；不覆盖历史原生文件或既有下载件。

## 自动化与构建

- Java：`/Users/waitforpro/.local/jdks/java8/Contents/Home`，OpenJDK 8u492；Maven 3.9.16。
- 联合 Maven 命令退出码 0；11 个 payroll 服务/合同测试类共 **271 tests, 0 failures, 0 errors, 0 skipped**。原始 stdout/stderr、命令、退出码及逐类 Surefire XML/TXT 和索引见 `backend-joint.*`、`surefire-reports/`。
- 三个前端行为检查均退出码 0：review confirmation 34 项、issue deadline 36 项、governance feedback Node test 15 项；总计 85 项行为断言通过。
- Prettier `--check`、ESLint、Stylelint 均退出码 0，覆盖本次相关的 review/requirement/governance 源文件。日志和命令分别保存在 `prettier/`、`eslint/`、`stylelint/`。
- `mvn -pl yudao-server -am -DskipTests package` 和 Vite `build --mode dev` 均退出码 0。构建仅说明项目可打包；其与实际运行实例的绑定另见下节。

## 运行实例与二进制身份

- 当前后端 PID 55351 的启动参数指向 `yudao-server/target/yudao-server.jar`，监听 `127.0.0.1:48080`。当前 jar 为 179,126,379 bytes，SHA256 `044a0625b5410cb8be5b086625ba60554b49f789402b5ff5423f46567cd850e5`，mtime `2026-10-22 19:36:00+0800`；进程于 `19:36:46+0800` 从该路径启动。最终 package 日志显示主类 `Nothing to compile`，构建后 jar mtime 未变。
- 当前候选 exporter 源 SHA 为 `d244b492cd3e90ff1c03f8e255edcda9985abfccbad4991433492db259e8e4e0`。`target/classes` 中的 exporter class SHA `66af4a3fb34ca91ac852110e8bb9e905557234e4564f91313b46adb2cf2c116f` 与当前 jar 内 `yudao-module-hrm` 子 jar 同名 class 字节完全相同。结合进程指向同一路径、jar 自启动以来未被本轮替换，可将运行后端与候选的 exporter 类对应；没有独立 dump JVM 全部已加载类，因此不把此结论扩大成全类加载证明。
- 当前前端可见页面使用现存 Vite development server（PID 86218，`127.0.0.1:5173`），不是静态 `dist` server。只读 GET `/src/views/hrm/payroll/governance/index.vue` 返回 200 `text/javascript`、121003 bytes、SHA256 `e0ac7667cb0d3fc481cdf57d421b0ece781a6ed03f2bf9514714c80b06d68279`；响应包含当前 SFC 的唯一策略常量/错误消息/按钮文本。响应原件、headers 和绑定结果在 `vite-runtime/` 与 `runtime-artifact-identity.json`。因此浏览器路径对应当前 Vite 源模块；Vite dist 构建产物是另一件独立制品。

## T06 输出与既有原生证据

- 新联合测试以真实生产 `workbook()` exporter 写出 TEST 长文本与六业务表空数据 XLSX，并在生成前通过 POI 完整字段重建；identity 比较见 `T06-fixture-identity-comparison.json`。
- 新输出 SHA 为长文本 `0e90e7fea8f3a2cb0071f57474c294b8f961c054d30c725b066b34b3223976cc`、空表 `f6d8005dcaed6347bbaf4da8669b694e8d7143397bd8eb78e59d5d141be1a178`。它们是此次自动化的另一次新生成，不是此前打开的原生文件。
- 之前已逐表在原生 Excel 检查的长/空 fixture 保持其旧 identity（`6e3c5ead…adecbaad` / `632eb7f…e06fe16`）；报告把两类文件明确分开，不替换原 native evidence。当前 `job1454` 本机 IAB 下载件另有 Excel 六表原生阅读及全量对账：352138 bytes、SHA256 `d32e9c9e…a975a3e5`，126 business records / 1130 fields 六表完全等于冻结 projection，并与 job1429 business projection 相等；本次 native 关闭未保存/未打印。

## T04/T07/T08/T09 closure 建议

见 `closure-suggestions.md`。T09 可见 003 缺少异议责任人/下一步时保留输入；006 历史证明 DISPUTED v10 → REVIEWING v11 且 266 保存了责任和下一步；008 独立历史证明 279 创建 CONFIRMED v12，确认元组包含 scope5、semantic8、source3578/relation3583/mapping3。不要称 004 主列表刷新截图本身显示 DISPUTED v10；其画面为当前列表 REVIEWING v11。最终 v12 之后的 1455 v4 发布下载需由主线程完成并绑定冻结/下载 identity。

## 非产品工具尝试

一个额外的手工 SFC compile harness 因 Node `require` 路径未解析 `@vue/compiler-sfc` 而退出 1，日志在 `sfc-compile-attempt-1/`。该尝试不属于交接要求的行为测试；Vite `build --mode dev` 与当前开发服务 SFC 转换响应均成功，不将 resolver 错误记作产品 SFC 编译失败。
