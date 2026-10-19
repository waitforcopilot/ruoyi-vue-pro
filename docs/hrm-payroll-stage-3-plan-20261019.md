# S3 来源、质量与评审闭环执行计划

2026-10-19，起点bbc3912d66ab01053725f3facfac11f28acb4247。S2最终源码与运行jar哈希一致，报告/浏览器主链及最终影响补验已核对；本机服务运行。PRD/原审计只读，既有工作区修改保留。

当前：阶段任务已验收8/8，阶段验收8/8。实现dd62d379fb已push origin/work并通过ls-remote核对。跨S4/S5未验项另列，不计本阶段通过。

| 任务/验收 | PRD/AC | 前置 | 操作及独立预期 | 状态/证据 |
| --- | --- | --- | --- | --- |
| S3-01/A1 来源身份/规格/版本 | 7.2；AC04/23 | 备份/全量基准、S2权限 | 登记实际来源，运营/语义/交付版本分开；旧文本待盘点不自动认可 | 通过；api-candidate.json联系人/单位/接口等、browser-source-incomplete.png及start.json |
| S3-02/A2 关系/复制 | 6.5/7.2；AC04/23 | A1 | 两需求一来源、一需求两来源；复制两端权限，新ID/版本且供给/核验/签字不继承；原副本互不影响 | 通过；api-candidate.json copy、security-candidate.json复制两端负例 |
| S3-03/A3 适用性/本人确认 | 6.3/7.4/7.5；AC05/06/09 | 来源/关系/合法独立授权 | 四固定子例，草稿允许缺项、确认拒绝；适用切换留失活关系；绑定完整当前元组 | 通过；api-candidate.json applicability；browser-na-empty-rejected.png、browser-na-confirmed.png、browser-applicable-restored.png |
| S3-04/A4 传播/待办 | 6.8/7.5；AC17 | 同一起点各自独立恢复 | 联系人/负责人/供给/语义/单映射/交付变化，分别核对影响ID、版本、签署和语义/质量，不误伤副本；范围三子例 | 通过；api-candidate.json contact/owner/supply/unit/type/interface/mapping/delivery/expand/period/rename；browser-mapping-impact.png/source-semantic-impact.png |
| S3-05/A5 语义/QS/QC/可用 | 7.6；AC27 | 固定D1、R1/R2，问题清零 | 不可变证据完整元组，0/2→0/2→1/2→2/2恢复；单映射保持R2/QS；规则变更不冒充业务语义 | 通过；api-candidate.json unit/type/interface/mapping/qsRule/qcRule；91项测试；browser-two-relations-ready.png |
| S3-06/A6 问题闭环 | 6.7；AC21/22 | 有效独立处理/复核者 | 开始→提交→驳回→补充→关闭→重开，拒绝自关/错人/空依据；固定上海日期、阻断、降级独立复核 | 通过；api-candidate.json issues、固定上海时钟测试；browser-issue-closed-history.png |
| S3-07/A7 权限/并发/迁移 | 9.1/6.8/7.6；AC17/23/27 | A1～A6 | 来源显式对象、关系双端、历史/计数/导出裁剪；跨项目/租户、旧会话、旧元组及范围并发拒绝；全量原字段保留/重入 | 通过；security-candidate.json五组真实并发和权限负例；preservation.json/restore.json/browser-export.json |
| S3-08/A8 浏览器/交付 | 用户门槛/9.2 | 必要检查、最终运行候选 | 实际IAB完整链、刷新重登/取消/失败保留/最小权限正负例/历史待办；白名单提交push远端核对 | 通过；browser-flow.md、candidate.json；实现dd62d379fb已push且远端SHA一致 |

证据目录：[索引](hrm-payroll-stage-3-evidence-20261019/README.md)。原始数据/凭据/备份仅仓库外stage3私有目录。全部当前492需求、718历史、22项目、68授权、107事件、1824角色菜单、34用户角色作为本轮保护基准。

正式根/决议/发布快照由S5接入后另补验；TEST材料不作正式批准。S4呈现统计不代替S3真实状态推导。仅执行S3，不启动S4～S6。

具体子例、版本元组及跨阶段条款见[阶段报告](hrm-payroll-stage-3-report-20261019.md)。最终候选28个源码文件哈希及实际jar详见证据索引。
