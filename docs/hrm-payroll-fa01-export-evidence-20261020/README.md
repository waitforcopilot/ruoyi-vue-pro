# FA-01独立审计后续处理证据

均为本机TEST；原报告及旧证据不改写。本轮只修复导出授权戳，并补充审计指出的证明边界。

| 文件 | 内容及边界 |
| --- | --- |
| candidate.json | 起点、输入审计SHA、改动源文件哈希、135测试与修复前1服务异常、运行jar及1109类匹配 |
| api.json | 任务1359在新增无关惰性锚点1360后实际状态/下载成功；原发布请求摘要匹配后拒绝 |
| api-iab.json | 页面刷新后新任务1361，新增1362后状态/下载成功；非同一1359浏览器续接 |
| browser-new-ready.txt | IAB1361在1362加入前的READY记录 |
| browser-claim.txt / browser-claim-visible.png | IAB1361在1362加入后实际刷新/领取，视口显示任务ID与READY |
| workbook.json | 浏览器实际下载1361原件与API字节一致；单文件638字段/六表/关联校验，不证明两次重导出 |
| browser-failure-visible.txt/.png | 重新滚动既有失败状态，文案实际出现在视口；不是本轮新制造的UI失败，当前候选拒绝另见API |
| browser-frozen-visible.txt/.png | 实际取景冻结1352表格，SUMMARY/DECISION/GRANT与六表标题在视口 |
| browser-decision-body.txt/.png | 真实点击DEC-604展开，受控正文及TEST标记在视口，非仅AX宣称可见 |
| runtime.json | 最终健康、输入只读、源码与生产类/包关联核对 |
| delivery.json | 验证后提交、push及当时远端身份；不冒称永久当前尖端 |

1359浏览器的最初创建证据为browser-export-before-anchor.txt；页面刷新后未继续冒用失效元素，重新建立1361并重新安排第二次无关锚点变更。完整脚本和日志、实际下载在仓库外私有目录。
