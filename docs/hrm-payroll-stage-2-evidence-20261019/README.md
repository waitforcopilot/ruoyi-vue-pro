# S2验收证据索引

2026-10-19。本阶段范围为当前项目成员、动作/对象/业务范围、分派确认与撤销；正式决议生命周期、来源/问题/release及异步下载留S3/S5。合成实现验收与真实业务批准分别记录。

## A1～A8验收集合

每组只有下列必需子例都有相应证据才计通过；8个标题不是8个PASS。提交/push状态以阶段报告后附记录为准。

| 验收组 / 任务 | PRD依据 | 前置及实际操作/独立预期 | 证据与结果 |
| --- | --- | --- | --- |
| A1 / S2-01 | 迁移保留；6.8；9.2 | 全量旧记录/字段基准，备份和匹配密钥，当前库迁移两次及隔离恢复后两次；撤销+逻辑删除的创建人授权不得复活 | 通过：[baseline.json](baseline.json)、[migration-reentry.json](migration-reentry.json)、[restore-result.json](restore-result.json)。481需求/685历史/16项目/18授权/57事件/1814角色菜单/29用户角色原字段逐行一致 |
| A2 / S2-02 | 9.1委派上限 | 合成独立根，新增/查看、有限期间、撤销、旧版本冲突；上游撤销和到期；越权/自提上限拒绝 | 通过：[成员撤销详情](browser-member-revocation.png)、[生命周期](lifecycle-result.json)、[API](api-result.json)、66项后端检查中的相应测试 |
| A3 / S2-03 | 9.1独立动作及创建 | 8动作分别校验；创建人无业务动作；单独租户创建权/初始边界，技术管理员越界也拒绝；编辑不能变META/CONFIRM/MEMBER/SCOPE | 通过：[创建拒绝](browser-create-denied.png)、[创建成功](browser-create-success.png)、[补充API](final-api-result.json)、[checks.json](checks.json) |
| A4 / S2-04 | 9.1、AC-14/18/19相关子例 | 先反查稳定ID归属；列表/历史/导出同边界；模块/对象/主体/组织/人员/场景/期间修改前后验证；跨项目/租户与无projectId不能绕过 | 通过：[API](api-result.json)、[边界API](final-api-result.json)、[页面导出解析](browser-export.json)、[编辑页面](browser-editor.png) |
| A5 / S2-05 | 6.3、9.1 | 分派人无编辑权仍可合法分派；候选人当前项目及平台READ/CONFIRM范围匹配；本人确认成功，自派/不合格/非本人拒绝 | 通过：[真实浏览器时间线](browser-flow.md)、[签署](browser-signed-history.png)、[API](api-result.json)、单测 |
| A6 / S2-06 | 9.1撤销及历史 | 原会话撤销后历史/列表拒绝，API保存/确认/导出拒绝；独立授权仍可读；停用账号、上游撤销及到期失效；同版本并发仅1成功；历史签署不变，可另行改派 | 通过：[撤销历史拒绝](browser-revoked-history.png)、[列表拒绝](browser-revoked-list.png)、[最终历史](browser-final-history.png)、[API](api-result.json)、[生命周期](lifecycle-result.json)、[补充API](final-api-result.json) |
| A7 / S2-07 | 用户浏览器门槛 | 初版6f4cc997b3运行候选上完成成员新增/查看/撤销、取消/保存、编辑、分派、确认、历史、旧会话拒绝及其他成员改派；刷新/多次重登、部分范围和受限导出 | 通过：[逐步记录](browser-flow.md)、[完整AX树](browser-final-history.txt)、本目录截图；API/模拟不替代浏览器 |
| A8 / S2-08 | 阶段路线5/6 | 必要单测、构建、类型/风格、API/DB并发、候选关联和证据完整；浏览器通过后才提交并push，核对远端 | 通过：[checks.json](checks.json)、[candidate.json](candidate.json)、[log-review.json](log-review.json)；[提交推送及远端核对](git-delivery.json) |

## 证据类别及夹具

- 真实浏览器：browser-flow.md中的实际IAB操作与截图。项目17需求486，成员256/257/258/259；项目19为创建及有限上限扩围回归，项目21为未知上限回归。
- 真实API/DB：api-result.json最新为项目22、需求495/496；lifecycle-result.json和final-api-result.json为独立项目18、需求488/489。不能把它们和浏览器项目混为一次执行。
- 模拟/自动检查：66项H2/Mockito/方法权限测试及vue-tsc/ESLint等。测试数量不代表AC计数。
- 迁移/恢复：当前开发库与独立恢复库分开；完整备份、配置/税务密钥及原始数据只在私有目录。
- 未验依赖：S3实际来源/问题对象、S5正式根与决议生命周期、release/异步下载再鉴权及六表快照；不据本轮关闭整个F-02或全部AC-19。

## 可复核入口

[阶段报告](../hrm-payroll-stage-2-report-20261019.md)、[阶段计划](../hrm-payroll-stage-2-plan-20261019.md)。真实API主回归脚本：`scripts/hrm/runtime_workbench_stage2.py --state-file <仓库外状态文件> --output <证据文件>`。必须准备新的TEST项目、独立有效合成根、创建人草稿访问及最小平台账号；脚本不能注册或复活根。状态文件私有字段含apiProjectId及三个测试账号凭据，禁止打印或提交。

本轮私有原始目录：`~/.hrm-payroll-local/stage2-20261019/`。prepare_api.py、lifecycle_check.py、final_api_checks.py、check_preservation.py及restore_check.py保留在那里供本机复核；涉及根登记/备份的操作须先阅读并检查当前对象和幂等条件，不盲目重跑或把合成材料变为正式批准。

## 最终补丁影响回归

初版主链与最终补丁分别取证，最终补丁未逐项重复全部初版UI动作。A1/A3/A4/A6补充：[迁移上限](creator-ceiling-migration.json)、[有限上限扩围](creator-expansion-result.json)、[未知上限](unknown-creator-result.json)。真实浏览器截图：[扩围前](browser-creator-before.png)、[扩围后](browser-creator-after.png)、[未知上限隔离](browser-unknown-creator.png)、[最终原签署保留](browser-postfix-history.png)。最终运行包66项测试及项目22真实API通过；候选哈希见candidate.json。
