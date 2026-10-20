# FA-01实际修复证据索引

本目录是2026-10-20重新执行的证据，不是为上一轮补造历史证据。所有页面和新增材料均TEST；真实批准材料仍缺，F-07/F-15未关闭。

| 文件 | 含义与限制 |
| --- | --- |
| candidate.json | 起点、两源码/测试哈希、最终运行jar、1,109个class比对、最终测试汇总；交付SHA另见delivery.json |
| tests.json | 修复前1反例失败，最终133项通过；H2/模拟与真实浏览器分开 |
| api.json | 甲105、乙148、1350/1354无关待签根前后令牌摘要不变，甲1356相关待签根使旧请求拒绝；幂等号仅1条release |
| browser-steps.json | 实际IAB与API交错操作顺序、各步结果及证据分类 |
| browser-precheck.txt | 首次预检查的AX差异记录，仅表示按钮启用；不是完整页面快照 |
| browser-publish.txt/.png | 乙变更后甲IAB成功发布1352版本3，全量AX及截图 |
| browser-retry.txt/.png | 乙第二次变更后相同请求仍1352；数据库单行另见API记录 |
| browser-related-rejected.txt/.png | 甲相关依赖改变后，旧请求重试被拒绝并保留输入；预期负例 |
| browser-frozen.txt/.png | IAB读取新1352的SUMMARY/DECISION/GRANT及六表入口，非全量业务重新验收 |
| xml-reverification.json | 本轮重新核验保存的run3共20文件、每个40,245字段及SHA；不是新负载或原生打开 |
| new-release-workbook.json | 当前候选1352实际API下载1358，638字段/六表/关联；单文件校验，不冒称两次重导出或原生打开 |
| runtime.json | 后端UP、前端HTTP200及实际重启/备份说明 |
| submission-scan.json | 白名单文本/图片扫描、原输入只读哈希核对结果 |
| delivery.json | 验证后实现提交、实际push结果与远端核对 |

原始失败/通过测试日志、打包日志、私有状态、夹具脚本及下载原件在`/Users/waitforpro/.hrm-payroll-local/`，原始备份与凭据不入Git。
