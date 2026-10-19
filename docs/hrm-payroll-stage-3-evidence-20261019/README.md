# S3 证据索引

功能实现8/8，A1～A7通过，A8浏览器通过、提交push待核对；阶段暂记7/8。详见[阶段报告及子例矩阵](../hrm-payroll-stage-3-report-20261019.md)、[计划](../hrm-payroll-stage-3-plan-20261019.md)。仅本机TEST工程验收，不作为正式业务批准。

| 证据 | 内容与类别 |
| --- | --- |
| [start.json](start.json) | 起点HEAD、实际全量保护计数、PRD只读哈希及私有备份路径 |
| [candidate.json](candidate.json)、[checks.json](checks.json) | 最终28源码哈希、实际jar及1083类逐字节匹配；91项后端测试/类型/ESLint/迁移审计。模拟DB测试与实际浏览器分开 |
| [api-candidate.json](api-candidate.json) | 最终后端包，真实登录API/数据库持久化，60～75共16独立项目子例。每子例自行建立相同基准，非串行复用 |
| [security-candidate.json](security-candidate.json) | 最终包76/77，最小权限/显式源/撤销/跨租户/复制两端，以及5组真实并发，最终状态断言通过 |
| [browser-flow.md](browser-flow.md) | 实际Codex IAB操作时间线，主链及最终影响补验17步骤；页面账号、范围/对象和预期实际、截图索引 |
| [browser-final.png](browser-final.png) | 最终源码刷新后：R1回评审中v12、关系4历史失活；R2保留当前确认v7。是预期终态，不是错误待修 |
| [browser-na-empty-rejected.png](browser-na-empty-rejected.png)、[browser-na-confirmed.png](browser-na-confirmed.png) | 空理由确认拒绝；合法不适用无来源负责人和映射本人确认 |
| [browser-applicable-restored.png](browser-applicable-restored.png) | 切回适用清当前确认，旧映射不复活 |
| [browser-final-confirmation-history.txt](browser-final-confirmation-history.txt) | 实际页面完整历史引用，旧与新确认分开、当时来源/映射版本可辨 |
| [browser-export.json](browser-export.json)、[export-api.json](export-api.json) | 实际点击下载并解析的JSON/既有Excel，受限只含497/关系4；API全/受限对比。不等于S5六表发布快照 |
| [browser-revoked-source-save.png](browser-revoked-source-save.png)、[browser-revoked-refresh.png](browser-revoked-refresh.png) | 同会话撤销后保存拒绝且输入保留，刷新裁剪并导出拒绝，独立只读497保留 |
| [virtual-sample-check.json](virtual-sample-check.json) | 独立Python复算虚构D1，非真实工资/外部来源，不声称系统内自动执行第三方检查 |
| [preservation.json](preservation.json) | 7表全部原ID/原列逐项一致，增量迁移两次重入 |
| [restore.json](restore.json) | 私有备份恢复到隔离库，S3迁移两次，匹配密钥保留，无伪造来源/核验 |

其余browser-*.png/txt是主链具体节点截图。api-result/security-races为初轮；api-final/security-final为13:22之前上一构建阶段（实际以候选冻结及日志时间为准）；最终以api-candidate/security-candidate为准，旧文件仅保留过程依据，不冒充最终候选。

正式决议生命周期、发布快照、六表历史重导出/异步下载与S4完整统计待后续阶段；未验项见报告。原始数据库、备份、私有键、token、口令、未裁剪授权均未入证据目录。预期拒绝与重启加载错误未算正例。
