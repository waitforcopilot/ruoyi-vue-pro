# S4 实际验证证据索引

2026-10-19；起点 `1cd9764145f8d77b27add111ae38a12782d8d18b`。所有对象为本机明确标为 TEST 的合成夹具，未绑定真实员工。账号263/hrms4editor负责编辑与范围决策，264/hrms4reviewer负责独立确认、质量复核及收件，265/hrms4restricted用于逐对象读写/导出和撤权负例。授权根为独立登记的合成根，不是正式业务授权。无口令、token或税务密钥在本目录。

## 验收矩阵

每行是子例集合，全部子例通过才计对应A组通过。API/DB、模拟测试、浏览器分别标识，不互相替代。

| 组/条款 | 操作、独立预期及实际结果 | 证据/方式 |
| --- | --- | --- |
| A1 / PRD4、5；AC01/11 | 十视图均有用途、项目范围、示例业务表、正常/异常场景、真实意见及来源区；前后、关联页和集中清单可返回。46个候选场景有独立锚点。账号均未绑定员工 | 实际IAB：`browser-ten-views.json`、`browser-view-*.png`；最终候选再遍历 `browser-final-view-*.png`。纯适配器检查 `demo-check.json` |
| A2 / AC11/12 | 同TEST员工160应出勤＝152实际＋8带薪假；时薪62.50、加班750、应发11750、个人扣款1400、税130.50、实发10219.50、成本13850。方案V2预览10800不改V1主链10000。演示核算/复核/工资条没有正式业务写 | IAB `browser-demo-slip-final.png`、`browser-adjustment-final.png`；独立数值检查`demo-check.json`；DB `preservation.json`中61表保持；适配器无API写依赖 |
| A3a / 6.2；AC03/13 | 字段意见670真实保存→编辑→刷新/重登→清单→原field-hours。草稿可不完整；评审缺场景拒绝并保留输入。业务字段、期望、单位/精度等可编辑；语义改变使签字失效 | IAB `browser-input-preserved.png`、`browser-field-return.png`、`browser-history.txt`；真实API `api-result.json`；17项新DB测试 |
| A3b / 上下文隔离 | 最终场景意见、操作意见、流程意见由浏览器新增，刷新均保留。OT-02返回焦点field-ot-02；另一空项目没有甲项目意见；深链保留项目与模块 | IAB `browser-topic-anchor-final.png`、`browser-contexts-reloaded.txt`、`browser-project-isolation.txt`；API逐行持久化复核`api-final-readonly.json` |
| A3c / WB10 | 开发中→待验收，页面/API/开发任务/验收依据与功能确认独立；刷新重登历史保留；验收通过是人工记录，证据存在不自动验证其结论；撤销EDIT后旧会话操作拒绝并保留输入 | IAB `browser-implementation-history.png`、`browser-implementation-denied.png/.txt`；真实API `api-result.json`、`api-final-impact.json` |
| A3d / WB11 | 批量分派逐对象检查：788成功、619旧版本失败并分别报告；会议纪要关联和版本差异真实保存；定向站内提醒264/265分别可读，发送者无收件，撤销265的主项目读权后旧会话立即拒绝 | IAB `browser-batch-partial.png/.txt`、`browser-history.txt`、`browser-reminder-received.png`、`browser-reminder-restricted.png`、`browser-reminder-revoked.png`；API `browser-state-api-crosscheck.json`、`reminder-api.json`、`api-final-readonly.json` |
| A4 / 6.4、6.5；AC07 | 纳入不自动确认；暂缓须理由/当前有权责任人/重新评估条件；移除须有效同项目保留项，禁止无效/循环；恢复候选回评审中并清签。SCOPE/MEMBER不隐含SCOPE_DECIDE。旧DEFERRED在隔离恢复库保留旧事实/历史并标待补，不编造理由/签字 | IAB `browser-deferred.png`、`browser-removed.png`、`browser-history.txt`、`browser-scope-decide-member-option.png/.txt`；API `api-result.json`、`api-final-impact.json`；DB `restore.json` |
| A5 / 6.4、7.4、7.6；AC10 | U/E/I/C/T/D服务端集合；适用无源按需求计缺口，首关系替换占位，有效NA豁免、无效NA仍缺口。关系按relationId计，来源另去重。暂缓/失配不减分母。实际供给/语义/QS/QC和问题推导2/4，来源2、豁免1、未决2/逾期1；下钻逐关系一致 | API `api-result.json`、`api-final-impact.json`、`api-final-readonly.json`；IAB `browser-source-statistics-final.txt/.png`、`browser-L-drilldown.png` |
| A6 / 6.4固定样例、9.1 | 独立项目85：S0=46/46/40,C40；S1=47/47/40,C40,D1；S2=47/46/40,C40；项目86：S3确认39/46、本期39/40。空项目82为0/0且percent=null。S2随后恢复候选，85最终47/47/40并非原S0快照 | 真实API `api-result.json`保存各瞬时快照；后续状态`api-final-impact.json`及`api-final-readonly.json`，从未为断言重置历史 |
| A6b / 权限与导出 | 页面、集合、下钻、当前筛选JSON导出遵循实时READ∩EXPORT；受限账号主项目当时只有670可读，不输出全项目数；来源83可读1关系/1来源但无来源EXPORT，导出变缺口且无隐藏关系ID。隐藏历史replacementId也裁剪；筛选不改变全项目门槛 | IAB `browser-restricted.png`、`browser-export.json`、`browser-filter-sort.txt`；API `api-final-impact.json`及`api-final-readonly.json`。主项目读权随后撤销，不能用早期正例描述其当前权限 |
| A7a / 异常与并发 | 缺源/空/批量部分失败/真实服务重启窗口接口错误/无权/旧版本冲突全部有明确提示；保存失败保留输入，读取最新值供人工合并；重试后恢复真实统计 | IAB `browser-api-error.png`、`browser-version-conflict.png`、`browser-input-preserved.png`、`browser-batch-partial.png`；API/DB测试分别检查拒绝不写历史 |
| A7b / 移动/键盘 | 390/768/1440逐次编辑同意见788、保存、历史、实际来源选择、原锚点返回；横表在容器滚动，弹窗正文滚动可达底栏。修复同URL返回焦点后全宽通过。最终46场景追加后再次三宽验证documentWidth等于viewport。第2页、排序、Enter应用筛选成功 | `browser-mobile.json`、`browser-mobile-*-source.png`、`browser-mobile-390-form-final.png`、`browser-final-mobile.json/.png`、`browser-pagination.txt`、`browser-filter-sort.txt`。`browser-mobile-390-form.png`与1440初始空焦点是发现证据，由final记录替代 |
| A7c / F14兼容 | 十模块候选目录沿用原sourcesForModules，COL并集及CALC/TAX/OT/HOUR映射不另建来源目录 | `browser-ten-views.json`记录逐页DS集合；`check_review_demo.cjs`检查十页都有候选来源；原F14回归不升级为实际来源完成 |
| A8 / 最终候选交付 | 最终backend含提醒读取与引用裁剪，前端含46场景、调薪预览、移动与独立授权选项；按影响再验十页、上下文/来源/权限/移动；原记录保留、迁移重入及恢复成功。108项相关测试、前端类型/ESLint、59迁移清单检查通过 | `candidate.json`、`checks.json`；提交、push与远端SHA见报告与`delivery.json`。未生成S5正式决议、基线或六表快照 |

## 夹具、时间和候选边界

- 起点、原602需求/1277历史/77项目等7表基准、只读文件SHA见`start.json`；原始数据库备份、匹配私有配置及完整日志位于`~/.hrm-payroll-local/stage4-20261019/`。
- 项目78主页面、79另一空项目、82空统计、83真实来源闭环、84有SCOPE/MEMBER无SCOPE_DECIDE；85/86为两个独立固定样例。最初项目80因列表核验太慢中止，保留34条TEST记录；81未用，均未清理/覆盖来凑样例。
- `api-result.json`为初次完整API链；`api-final-impact.json`为引用裁剪/范围独立动作候选补验；14:22最终后端加入收件查询后，以`api-final-readonly.json`、`reminder-api.json`和最终IAB截图补验。不同时间记录的计数不可混用。
- 最终遍历的批量截图调用在第十页返回时超时，前九张文件已写入，恢复同一个IAB标签后补第十张和后续操作；没有换用Chrome或接口冒充页面回归。
- 所有截图为合成对象；登录不截图且关闭记住密码；公开JSON/文本不含口令/token。页面控制、真实API/DB和108项数据库模拟测试分别列示。
- 当前导出是受权限裁剪的评审JSON清单；S5正式六表/快照/异步下载、正式授权根和决议撤销接入仍未验证。

浏览器AX文本仅规范化行尾空白以通过diff检查，原始文本另存私有raw-browser-text目录；未改动业务文字、计数或预期。
