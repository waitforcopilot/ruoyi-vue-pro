# S2成员与授权阶段交付报告

日期：2026-10-19。分支work，起点5099a850b31af28c16c576bf2682e1f91e573850。续接已有未提交候选，补齐提示词审计新增要求；不重复S1，不启动S3。

## 当前结论

S2阶段任务8/8通过，A1～A8阶段验收8/8通过；初版6f4cc997b3已推送；最终创建人边界补丁已验通过，提交推送待收尾。完整证据见[验收矩阵](hrm-payroll-stage-2-evidence-20261019/README.md)。这是本机TEST合成实现回归，不是正式业务批准或全部PRD验收。

## 实现内容和边界

- 项目成员授权：新增/查看/有效期/撤销/乐观版本，委派动作、业务范围、模块、稳定需求ID和有效期不能超上游；服务端指定parent/grantor，不接受请求自造根。授权详情展示上游、版本、日期、依据和撤销记录。
- READ/META/EDIT/ASSIGN/CONFIRM/EXPORT/MEMBER/SCOPE独立；同一授权的动作与范围一起匹配，不拼接成跨组织编辑权。活动账号、项目、上游、日期均每次检查；无技术角色或ownerId业务越权。
- 创建须有租户级PROJECT_CREATE及覆盖初始业务边界的根记录。创建时申报边界写入可撤销草稿授权，项目业务范围仍未定义，须独立修订；不能据创建视为业务批准。ROOT登记及正式依据生命周期留S5。
- 当前需求/历史/同步导出统一服务端反查及范围裁剪，保存前后均检查；列表返回行的permittedActions供页面控制，服务器仍独立校验。历史按当前读取权限过滤，原签署不改写；受限同步清单明确标节选。
- 分派与编辑分离，目标同时具备当前对象READ/CONFIRM及平台查询/确认资格；无维护权的分派人可以合法操作，无维护权的评审人可以本人确认。撤销后旧会话请求被拒绝，其他有权成员仍能读原签署并改派。
- 无projectId的LEGACY路径保留历史身份及数据，但现在也要求显式项目动作授权，不再仅凭通用平台权限访问；未登记成员时拒绝，不自动给旧角色扩权。旧路径不被当作新版正式基线。
- S5正式决议校验尚未交付，新项目非TEST确认先拒绝；TEST确认仍走所有成员/对象/版本/平台/分派校验，不提供全局跳过开关。S5须将当前临时的合成/正式隔离边界接到真实版本化依据，不能据本轮认为正式根和决议生命周期已实现。

## 动作契约与平台入口

| 当前存储/接口动作 | PRD规范动作 | 对应平台条件/入口 |
| --- | --- | --- |
| READ | PROJECT_READ | requirement:query；项目/列表/历史/按ID读取 |
| META | PROJECT_META_EDIT | requirement:query及项目META；仅展示名称 |
| EDIT | REQ_EDIT | requirement:update；项目模板初始化/采纳另需查询及完整EDIT范围 |
| ASSIGN | REQ_ASSIGN | requirement:query + requirement:assign，目标资格独立检查 |
| CONFIRM | REQ_CONFIRM | requirement:query + requirement:confirm，当前分派人本人 |
| EXPORT | PROJECT_EXPORT | requirement:query + requirement:export，与READ交集 |
| MEMBER | PROJECT_MEMBER_MANAGE | requirement:query及项目MEMBER/明确委派上限；根生命周期由S5受控登记 |
| SCOPE | PROJECT_SCOPE_EDIT | requirement:query及全项目READ/SCOPE，授权覆盖旧新范围并集 |
| project_id=0，PROJECT_CREATE | PROJECT_CREATE（租户级） | requirement:query + requirement:update及独立创建根/初始边界；不是项目成员 |

SCOPE不等于SCOPE_DECIDE，未知新动作不默认授予。来源、问题、决议、发布等动作由S3～S5显式扩展并接入对象范围；不得批量补给旧角色。正式批准依据与业务根读权也留S5验证。

## 实际验证

66项后端单测/数据库/方法权限测试通过，后端打包成功；完整vue-tsc（8192MB堆）退出0，四个前端受影响文件ESLint/Prettier通过，git diff --check通过。最终候选文件哈希和运行jar哈希见[candidate.json](hrm-payroll-stage-2-evidence-20261019/candidate.json)，不把旧构建日志冒充最终运行证据。

内嵌浏览器走完：成员授权→最小权限编辑（含取消/重开/保存）→TAX范围不可见和全项目初始化拒绝→受限清单下载→最小权限分派→被分派人本人确认→历史→原会话撤销后拒绝→管理成员新增替代评审、浏览器撤销编辑授权→改派后重审且原签署保留。又验证创建边界999被拒、输入保留，改101后创建项目19。见[真实时间线](hrm-payroll-stage-2-evidence-20261019/browser-flow.md)。

需求486版本1→2编辑、2→3分派、3→4本人确认（签署人258）、4→5改派259进入评审中。最终历史仍保留版本4的原签署。仅TEST，不代表真实业务签署。成员256只有平台查询/分派，257查询/维护/导出，258及259查询/确认；创建回归另用有独立TEST创建根的admin，不替代最小权限正例。

真实API/DB验证动作与范围交集、模块/对象/主体/组织/人员类型/场景/期间、跨项目/租户、缺projectId、合法分派确认和各类拒绝；同版本并发保存仅一成功，撤销与保存竞争后旧token新请求被拒。独立授权仍可读、上游撤销/到期失效；TEST账号停用即拒绝，恢复账号后原token仍失效，重新登录才恢复其合法访问。

当前库迁移反复执行；撤销并逻辑删除的TEST创建人授权不复活。在独立库hrm_s2_restore_20261019_103107恢复备份并重复迁移，全部原字段逐行一致。完整基准481需求、685历史、16项目、18授权、57事件、1814角色菜单、29用户角色均保留；包括旧账号249的原关系94/角色1。备份和匹配配置/税务密钥保留私有目录，未打印或提交。此前失败的恢复尝试使用独立空库/检查格式，最后改为二进制导入及一致的raw查询格式后通过，未对开发库做回滚。

## 最终边界补丁及证据版本

初版6f4cc997b3之后复核发现：旧S2兼容迁移生成的创建人草稿授权若没有ceiling_json，可能随未来业务范围定义/扩张而扩权。新增反例先实测失败，再修复服务端空上限判定；迁移仅一次封存S2生成的有效兼容授权上限，grant_version=2也封存未知上限，重入不得绑定未来范围或复活撤销授权。

最终运行包上66项测试通过、重新打包并启动；新建项目22重跑完整真实API回归。实际内嵌浏览器补验：项目19扩围后只见原主体101子集493，继承扩张范围的492不可见；项目21原未知上限在业务范围定义后列表为空，历史拒绝且迁移重入不扩权；返回项目17仍保留原版本4签署人258和版本5改派。详见[补验时间线](hrm-payroll-stage-2-evidence-20261019/browser-flow.md)。

初版最小权限编辑/分派/确认/下载主链按影响复用；最终补丁没有逐项重跑所有初版页面动作。最终包真实页面补验、全量API和66项测试分别列示，不混称同一次执行。兼容授权变更仅涉及本轮新增记录，原基准字段仍逐行一致。

## 真实失败及剩余问题

- 早期单测沿用“所有技术账号自动可访问LEGACY”的夹具；改为显式合成项目授权后，原业务断言及新增拒绝断言通过。没有恢复角色绕过或放宽鉴权。
- 恢复校验最初把mysqldump当UTF8文本、及raw/转义输出格式不同而失败；按字节导入并统一查询格式后，中文及所有原字段逐项一致。导出数字按Excel数字单元格读取，未将486.0误当业务数据丢失。
- TEST替代账号创建首次因随机口令超过既有4～16位限制失败；未生成账号记录，调整后仅创建独立259。停用/恢复TEST账号会撤销其旧token，复核已改用新登录，不错误期待旧token复活。
- 浏览器仍有Vue Router/Element Plus弃用警告，四次有意拒绝的请求触发了现有全局Vue错误处理；页面已显示拒绝且输入保留。未称浏览器日志零错误。旧进程重启时另有运行中jar被替换后的关闭日志异常，当前新进程健康UP；后续打包宜先停旧进程再替换jar。详见[日志核对](hrm-payroll-stage-2-evidence-20261019/log-review.json)。
- S3来源/问题对象、S5正式授权根和决议生命周期、release/六表/异步下载撤销仍未验；整个F-02及AC-19不关闭。剩13个审计包未关闭，S1、S2阶段进度分别列示，不把13换算为剩余执行次数。

## 本机运行与恢复

主回归入口：http://127.0.0.1:5173/hrm-payroll/requirement?module=COL&projectId=17 。后端：http://127.0.0.1:48080/actuator/health 。MySQL/Redis/后端/前端均继续由原本机Docker与launchd管理，不连接Cloud。

凭据和原始证据：~/.hrm-payroll-local/stage2-20261019/；配置和税务密钥：~/.hrm-payroll-local/application-workstation.yaml及credentials.json；备份：~/.hrm-payroll-local/backups/20261019-092413/。仅提供位置，不公开内容。

服务日志：~/.hrm-payroll-local/server.log、backend.stdout.log、backend.stderr.log、frontend.stdout.log、frontend.stderr.log。本轮检查日志在stage2私有目录tests-final.log/build-final.log/typecheck-final.log/eslint-final.log。短信/邮件外发及非必要调度保持关闭。

管理命令：`~/.hrm-payroll-local/manage.sh status|start|stop|restart|backup`。JDK8、Maven、Node22与Python3.12沿用原本机环境，未安装/卸载其他软件。S2回归脚本用私有venv的Python3.12执行。

提交白名单为本轮S2代码/迁移/测试/脚本及阶段文档证据；PRD、原型、原审计、S3～S6提示词、已有本机交接文档无关改动不混入。下一入口S3，需先读本报告和跨阶段待验项，不重复S2或凭旧恢复点回退。

## 提交与推送

实现提交：`6f4cc997b3189fc7fca5c057c57ace740bc2695d`，`feat(hrm): deliver S2 bounded project membership and revocation`。

实际执行`git push origin HEAD:work`成功，GitHub仓库https://github.com/waitforcopilot/ruoyi-vue-pro ，分支work；实现推送后远端refs/heads/work与本地均为上述SHA。[交付记录](hrm-payroll-stage-2-evidence-20261019/git-delivery.json)。最终边界补丁和交付记录将在实际推送后补记，当前不能据初版SHA认定最终代码已推送。

推送后健康复查：后端UP、前端HTTP200、MySQL healthy、Redis运行中，见[状态](hrm-payroll-stage-2-evidence-20261019/service-final.json)。最终源码逐文件与运行候选哈希一致，未混入私有凭据、无关交接改动或S3～S6文档。S2已完成后停止；S3尚未启动。
