# S6 证据索引

收尾2026-10-20；原阶段文件名20261019。任务验收6/8、阶段验收6/8、工程AC26/28；AC25/28及A5/A8真实材料阻塞。TEST不是正式业务批准。详细步骤/预期/失败/边界见[报告](../hrm-payroll-stage-6-report-20261019.md)及[AC矩阵](../hrm-payroll-stage-6-acceptance-matrix-20261019.md)。

| 分类 | 文件 | 实际含义 |
| --- | --- | --- |
| 日期校验补修 | root-date-validation.json、browser-final-candidate.json | 最终候选三个实际400负例、无新根，正常IAB日期登记1344/独立签署、合成发布1347/同号重试、真实下载1349；性能复用影响边界在candidate.json |
| 冻结/起点 | candidate.json、start.json、final-runtime.json | HEAD起点、代码/构建哈希、实际表基准和服务；提交SHA另见delivery |
| 修复复现/采样 | performance-before-fix.json、list-fix-probes.json、performance.json、performance-raw-manifest.json | 首次超时、修复后探针及唯一有效完整run3；全部原始样本私有，不删慢样本 |
| 测试 | backend-test-suites.json | 本轮实际运行最终tests-final-input-validation.log的2910条目/40跳过，HRM1233项无跳过无失败；H2仅模拟证据 |
| 两身份/并发/持久化 | browser-isolation.json、browser-conflict-*.png、browser-persistence.json、browser-context-return.png | 不同origin同时不同登录；真实冲突输入保留与人工合并、两边重登刷新和field-hours焦点 |
| 十视图/视口 | browser-ten-views.json、browser-history-390/768/1440.png、browser-network-retained-390.png | 真实保存/回读、选来源/历史及本页保存网络故障后恢复；非仅截图 |
| 决议/确认/来源/问题 | browser-independent-decision.png、browser-confirm-unprovided.png、browser-root-independent-sign.png、browser-issue-history.png | 604先于1359确认；未供数确认和QS/QC分开；1296独立TEST签署；3172驳回/关闭/重开完整人工链 |
| 固定夹具 | fixed-api-cases.json、extra-cases.json、statistics-publication.json、template-s3.json | 14个独立同起点分支、多对多、问题、独立S0～S4、模板升级与CUSTOM |
| 权限 | security-api-cases.json、expiry-personnel.json、organization-runtime-matrix.json、organization-ceiling.json | 跨租户/项目/组织/对象、最小分派、旧签字改派/复制；日期到期夹具不冒充跨日等待 |
| 并发/决议生命周期 | concurrency-scope.json、decision-lifecycle.json、future-decision.json | 实际HTTP竞争及未来/到期实际时间；旧元组不混拼 |
| 六表冻结/核对 | five-mutations-same-v1.json、workbook-mutation-check.json、performance.json | 同V1五类当前变更及每导出文件逐字段对照、无孤立引用；仅导出元数据不参与相等比较 |
| 实际打开 | native-open.json、native-xlsx-summary.png、native-xlsx-sheet-2.png至6.png | IAB下载900，本机LibreOffice实际打开并逐表导航；不是脚本替代 |
| 独立渲染 | xlsx-render-1.png至6.png | artifact工具从实际文件渲染，和上述原生打开证据分开 |
| 撤销旧下载 | browser-restricted-placeholder.png、browser-restricted-ready.png、browser-old-download-denied.png | 268合法领取1244后撤销491，同会话同旧按钮领取DENIED；隐藏关联/依据合并占位 |
| 大样本页面 | browser-500-requirements.png、browser-500-page2.png | 实际107列表500条及第二页，不以API负载替代页面 |
| 保护/恢复 | preservation.json | 本轮全库备份，开发库增量迁移两遍、隔离恢复迁移两遍，原ID/历史/签署/授权/快照和业务校验和保留 |
| 不接受的准备结果 | performance-run-1-invalid-harness.json、fixed-api-preparation-failures.json、native-attempt-old-window-excluded.png | 错路由/重叠采样/无效夹具和错旧窗口，明确不能计PASS；原始失败日志均私有保留 |
| 步骤日志 | api-steps.json | 实际方法/路径/成员/响应码，无凭据；多个脚本快照写入，非穷尽总账，以上命名用例结果优先 |

账号266编辑、267独立批准/确认、268受限读导出、269～278并发成员；均TEST、不绑定真实员工。项目105浏览器、106对照、107负载，其他独立项目ID逐例列明；没有重置原项目。配置/密码/税务密钥/原始日志和全部备份只在 `~/.hrm-payroll-local/stage6-20261019/`，不提交Git。

正式材料缺失不是工具故障。F07和F15仍开放；完整PRD和正式投产未验收。实现提交916f487e3e已push且远端完整SHA一致；见[交付核对](delivery.json)。随后只补此交付元数据，源文件与运行jar不变。
