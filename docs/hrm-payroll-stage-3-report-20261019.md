# S3 来源、质量与评审闭环交付报告

日期：2026-10-19；分支work，起点bbc3912d66ab01053725f3facfac11f28acb4247。S2实现764198fbb5及文档收尾已核对，未回退。仅执行S3。源码与运行候选冻结见[哈希](hrm-payroll-stage-3-evidence-20261019/candidate.json)。

当前功能实现8/8；A1～A7通过，A8实际浏览器通过，提交push待核对，故阶段验收暂记7/8。以下结论仅对本机TEST合成夹具；不代表正式业务批准、生产验收或全部PRD通过。

## 实现和入口

入口仍为需求评审：`http://127.0.0.1:5173/hrm-payroll/requirement?module=COL&projectId=23`。下方新增实际来源、消费关系映射、独立评审问题、核验证据、站内待办与建议；候选DS目录保留为候选，不自动变实际来源。

- 来源有独立sourceId、结构规格、匹配/获取/频率/范围/字段，以及语义/运营/交付版本。关系有独立relationId、mappingVersion、消费字段及空值/重复/跨期/异常规则。跨项目复制独立身份，供给、核验和签字不继承。
- 新增类型化工作台记录表，SOURCE/RELATION/ISSUE独立对象，EVIDENCE/HISTORY追加留存；服务限定可变类型，无改写旧核验接口。项目锁与READ_COMMITTED串行化范围、语义、核验、确认及关闭检查，旧版本/绑定拒绝。
- 普通保存不产生确认或READY。来源适用性结构化；合法不适用豁免来源负责人和映射，空理由可草稿但确认拒绝；切换清当前签字并回评审中，旧关系仅失活保留。
- 实际来源、映射语义、QS、QC分别核验，供给/范围/语义/匹配证据/问题共同推导可用。QS/QC是有权人员登记的实际核验结果及依据引用，不是自动连接外部系统执行检查的引擎；本轮D1为两行虚构CSV，另用Python独立复算并绑定摘要。
- 问题独立于发薪工单，含处理/复核人、期限、周期、关联集合和三类阻断。自关、错复核人、空依据、旧关联关闭均拒绝。BASELINE_BLOCK返回真实纳入交集阻断结果，等待S5调用发布门槛。
- S2新增SOURCE_EDIT/VERIFY及ISSUE_MANAGE/HANDLE/REVIEW；来源显式授权，需求读权不隐含来源读权。共享语义编辑要求覆盖全部当前消费需求；不足时可提交建议，有权负责人在站内处理。无全局跳过鉴权或正式决议开关。

## 验收子例及实际证据

| 组 | 必须子例和实际结果 | 证据类别/位置 |
| --- | --- | --- |
| A1 | 来源缺负责人保存拒绝、输入保留，补齐成功；联系人只增运营，单位/类型/接口只按契约影响；D1交付版本独立；旧文本不生成已核验来源 | 实际IAB source-incomplete；API contact/unit/type/interface/deliveryReplacement；隔离恢复无伪造来源 |
| A2 | 两需求一来源及一需求两来源；复制两端鉴权，新ID、版本归1、未供给/无证据/无签字，甲变更不影响副本 | api-candidate.json copy；security-candidate.json复制及跨项目负例；IAB两关系4/7 |
| A3 | 空理由草稿可保存/确认拒绝；合法不适用无来源负责人/映射确认成功；适用缺映射拒绝；双向切换清签、不复活旧关系；旧版本/非分派人拒绝 | API applicability、91项测试；IAB na-empty-rejected/na-confirmed/applicable-restored及最终刷新 |
| A4 | 联系人保留确认/核验；负责人交接待办及来源语义待验但功能确认保留；供给变动不清确认；共享单位/类型/接口影响全部消费，单映射仅R1；副本/失活关系隔离 | API contact/owner/supply/unit/type/interface/mapping/copy；IAB proposal-owner/mapping-impact/source-semantic-impact |
| A4范围 | 扩主体及改期间各自独立起点使scopeVersion=2、范围/语义及QS/QC失配，源语义/交付版本不增；仅改名称scopeVersion=1、保留两可用/确认；后关联须验新范围 | API expand/period/rename及H2数据库测试 |
| A5 | D1不替换改单位/类型/接口，语义重验后0/2，QS失败0/2，QS通过无QC仍0/2，R1新QC1/2，两QC2/2；恢复不代确认，旧证据内容不改 | API unit/type/interface独立恢复各[0,0,0,1,2]，IAB two-relations-ready；virtual-sample-check.json |
| A5映射/规则 | 只改R1映射保留QS和R2 QC，可用1/2；语义重验仍1/2、R1新QC后2/2；QS/QC规则变化分别失配，不冒充业务语义变更 | API mapping/qsRule/qcRule、测试旧绑定/当前元组失效 |
| A6 | 开始→提交→驳回→补充→关闭→新理由期限重开；自关/错人/空依据/旧关联关闭拒绝；独立复核才降级或缩影响；关闭不产生确认/核验 | 实际IAB issue-closed-history、API issues及数据库测试 |
| A6期限/阻断 | 固定上海2026-10-17：到期当日不逾期，待复核16日到期仍逾期，关闭不计未决但保留历史逾期；BASELINE_BLOCK按纳入交集，DATA_READINESS仅阻可用，ADVISORY不阻 | 25项Workbench测试中的固定时钟及真实阻断断言；API重开/独立降级 |
| A7权限 | 需求窄读不隐含源读；共享改语义覆盖不足拒绝/建议成功；复制两端、未知授权、跨项目/租户拒绝；同token撤销源/EXPORT后写入和导出拒绝，另有合法只读不受误撤销 | security-candidate.json；IAB shared-edit-denied/proposal-owner/revoked-source-save/revoked-refresh |
| A7读出 | 列表/详情/历史/证据/待办按当前授权裁剪，当前及历史确认元组不泄露授权依据；实际JSON及原Excel只含497不含498，受限标签明确；0关系不展示虚假READY | browser-export.json、export-api.json、历史裁剪数据库测试 |
| A7并发 | 来源×QS、映射×QC、交付×QC、来源×确认、范围×问题关闭五组真实竞争；顺序成功允许，但后变更使旧结果失效，旧scope核验/关闭拒绝 | security-candidate.json。返回0/0也核对最终不可用/当前失效，未把并发均必须报错当标准 |
| A7迁移 | 全部原492需求/718历史/22项目/68授权/107事件/1824角色菜单/34用户角色原列逐项相同；增量迁移重入两次、隔离备份恢复两次、税务匹配密钥保留 | preservation.json、restore.json；原始备份及凭据仅仓库外 |
| A8 | 三测试角色真实登录，来源→关联→分派本人确认待供数→语义/QS/QC→独立问题全链→单映射/共享语义重审→刷新重登/历史、受限导出及旧会话撤销；最终适用性补丁单独补验 | browser-flow.md及截图，不以API冒充页面；Git交付待下节核对 |

[完整证据索引](hrm-payroll-stage-3-evidence-20261019/README.md)；[计划矩阵](hrm-payroll-stage-3-plan-20261019.md)。主链先执行，随后补强读出/历史/导出和适用性，均按影响回归；最终后端包另建60～77项目重跑16 API组及安全并发，不用串行污染的旧夹具推断通过。

## 必要检查和修复过程

91项相关后端测试全部通过：权限9、工作台25、项目26、需求31，0失败/错误/跳过；JDK8打包成功，1083个HRM类与运行jar逐字节相同。最终前端类型检查、5个改动TS/Vue文件ESLint通过；check_audit通过46项与58条有序UTF-8迁移。详见checks.json，原始日志仓库外。

实际发现并修正：反转证据列表污染MyBatis会话缓存；问题START错误提交空期限导致400；历史授权引用和计数裁剪；无当前关系不能显示READY；前端遗漏适用性/理由/规则版本修订比较而误套旧确认规则；后端任意适用性切换强制评审中。没有放宽鉴权或跳过断言。重启窗口请求错误、无效DOM点击及加载中旧行均不算业务成功，服务UP后重新操作验证。

本次不声称完整HRM业务矩阵、移动端、性能或真实外部供数通过；未做这些S6/外部验收。没有实际工资、支付、短信、邮件外发或Cloud调用。

## 原审计包与后续阶段

| 包 | S3自身条款 | 整包结论/下一依赖 |
| --- | --- | --- |
| F-04 | 实际来源、多对多、字段映射、三层版本、影响/待办、两端复制及独立身份均有本机运行证据 | 自身条款关闭；不包含F-07/08正式批准/快照，也不等于整条AC04/17/23全部通过 |
| F-03 | 独立确认、范围/语义/映射/规则完整元组及当时授权绑定、变更重审 | 部分完成；S5正式决议/授权根生命周期及发布引用接入后再验 |
| F-05 | 来源可用真实推导、QS/QC完整元组、分层恢复及普通保存不置READY | 部分完成；S4统计呈现及S5冻结/发布接入待验 |
| F-10 | 独立问题全周期、期限、独立复核、阻断类型/纳入交集返回 | 部分完成；S4完整逾期统计呈现，S5发布门槛消费阻断待验 |
| F-11 | 结构化适用性、豁免/缺口/切换清签/失活关系及当前导出 | 部分完成；S4全套统计、S5发布六表导出同步待验 |
| F-01/02 | 当前范围传播、来源/问题动作对象鉴权及撤销读取/写入/同步导出 | 部分完成；正式决议、release和异步下载再鉴权留S5 |

原15包中F-04、F-12、F-14自身已关闭，其余12包未关闭（9 P1、3 P2），具体仍有F-01/02/03/05/06/07/08/09/10/11/13/15。阶段通过与整包关闭分开；不把12换成执行次数。下一阶段仅S4，尚未启动。

S5接入契约：确认引用包含需求semanticVersion、scopeVersion、ruleVersion、来源semanticVersion、relationId/mappingVersion及当时授权引用；QS包含租户/项目/scope/source/semantic/delivery/digest/qualityRule；QC再绑定匹配QS ID/版本、relation/mapping、解析范围摘要和consumptionRule。S5须按当前业务决议/授权根状态重新鉴权并冻结当时引用，不能只信TEST签字或旧CONFIRMED标签。发布前同事务消费blockers/readiness，冻结来源/关系/证据/问题/决议/授权；下载按当前权限裁剪。正式非TEST确认缺S5必需依据仍拒绝；尚无S5发布对象，不虚构B1已发布历史。

## 本机运行与数据保护

前端127.0.0.1:5173，后端127.0.0.1:48080；MySQL/Redis独立本机容器。账户editor260、reviewer261、restricted262均为新TEST角色，不打印口令。凭据、合成根登记、原始日志及会话：`~/.hrm-payroll-local/stage3-20261019/`，目录700/会话600；备份及匹配税务密钥在仓库外。262当前仅保留独立只读497，来源/导出授权已撤销，测试结束不恢复广泛授权。

后端/前端通过launchd local.hrm.backend/frontend持续运行；日志`~/.hrm-payroll-local/backend.stdout.log`、backend.stderr.log及frontend.stdout.log。管理：`~/.hrm-payroll-local/manage.sh status|stop|start|restart`。新迁移只加列/表/菜单，无既有角色自动扩权；迁移清单同时补齐已存在的S1/S2及先前独立权限脚本，共58条。无关运行交接文档改动、PRD/原审计、S4～S6文件保留原状，不纳入提交。

## Git交付核对

待完成白名单提交push后写入实际实现SHA及远端结果。浏览器已通过，不提前声称push成功。
