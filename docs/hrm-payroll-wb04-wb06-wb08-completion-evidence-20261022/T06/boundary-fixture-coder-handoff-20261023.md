# T06 原生边界夹具编码交接

日期：2026-10-23，Asia/Shanghai。内部交接，不是最终 fix 报告；T01–T09 全部验完前不形成最终报告。本轮 coder 按项目约定期望 gpt-6.1-sol / medium，实际运行模型未核实。未执行编译、测试、DB、浏览器、Excel 或提交。

## 固定候选

HEAD：`fdc503dca517b4dad4ea136f73bd9fbe92e11c59`。工作区有既有未提交差异，均保留；本轮只新增以下测试与本交接。

| 文件 | SHA-256 |
| --- | --- |
| `yudao-module-hrm/src/test/java/cn/iocoder/yudao/module/hrm/service/payroll/HrmPayrollT06ExportFixtureTest.java` | `7626861eff2c8f8d43d6bbe0d081d48eb2eb227be054d4dcdfd36486b35b964a` |
| 既有生产 `HrmPayrollGovernanceExportService.java`，只读未改 | `d244b492cd3e90ff1c03f8e255edcda9985abfccbad4991433492db259e8e4e0` |
| 既有 `HrmPayrollGovernanceServiceTest.java`，只读未改 | `9374a3e8a29edbd9d9ad131cdf9e32fc13f45437481089ddfd5302f9ca2f499e` |

## 夹具来源与范围

独立 JUnit 类不继承 H2 集成测试，不启 Spring/DB，不复制生产分片算法。人工构造明确标记 TEST 的六表投影，调用真实生产 `workbook()` 后使用 POI 重读；按所属记录、字段及片段序号恢复每个输入字段，核对完整字符、连续序号、片段总数、父记录唯一性和片段内 emoji 代理对，避免只比较首尾片段。

长夹具包含7000次中文/emoji/六个连续换行、2000条编号超长依据、各表连续换行、空值/空字符串/结构化值及公式形文本。空夹具的六张业务表输入全部为空；第一张表仍保留生产导出器自动生成的 `EXPORT-METADATA`。文件明确属于**测试夹具经真实导出器生成**，不是浏览器实际下载、不是DB冻结快照，也不能证明发布/权限/正式批准。

## Executor 串行执行

请求 `gpt-6-luna / max`，实际模型须按运行端证据记录；额度或模型不可用须报告阻塞，不替代。先复核上述身份，再运行（下面目录可替换为主线程指定的新目录）：

```bash
JAVA_HOME=/Users/waitforpro/.local/jdks/java8/Contents/Home /opt/homebrew/bin/mvn -B -ntp -pl yudao-module-hrm -am -Dtest=HrmPayrollT06ExportFixtureTest -Dsurefire.failIfNoSpecifiedTests=false -Dhrm.payroll.t06.fixtureDir=/Users/waitforpro/.hrm-payroll-local/t06-native-fixtures-20261023 test
```

测试默认只校验内存输出。传入 `hrm.payroll.t06.fixtureDir` 后，仅在完整重读断言通过后新建唯一 `TEST-T06-real-exporter-*` 子目录，以 `CREATE_NEW` 写两个 XLSX 和 `TEST-FIXTURE-PROVENANCE.txt`（包含 SHA-256）。控制台打印 `T06_TEST_FIXTURE_OUTPUT=` 的实际目录。历史封存件不覆盖，不将原生 Excel 编辑后的文件回写成原导出文件。

执行者记录命令、退出码、Surefire实例结果、两份文件身份和实际路径；主线程再用原生 Excel 打开对应文件，核对长正文/emoji/连续空行/超长依据的首中尾片段与空六业务表显示，保存新的原件。POI通过不能替代原生视觉通过。既有浏览器下载1429/1441及其封存证据保持独立；本夹具不补写其来源或验收状态。
