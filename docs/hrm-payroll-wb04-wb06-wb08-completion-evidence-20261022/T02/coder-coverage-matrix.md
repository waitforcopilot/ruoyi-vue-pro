# T02 编码阶段覆盖矩阵

日期：2026-10-22。范围仅为任务计划第5节的 WB-04 本人确认、重审、撤权与并发。此文档是源码审查与待执行候选交接，不是测试通过报告；本轮未执行编译、测试、数据库、浏览器、重启或提交。未修改产品代码。

角色：coder；期望/请求配置为 Codex / gpt-6.1-sol / medium；实际模型未核实。HEAD：`234dca9b7e2e328c9fb107b539b9d45d78b8621f`。保留 T01 四个后端文件及已有 GovernanceExportService、GovernanceServiceTest 差异。

## 审查结论

静态审查未发现本包内已有证据支持的产品绕过，故不改已冻结业务候选。重点补充有区分力的验证缺口：原 Requirement/Project/Workbench 测试 mock S5 Store/Root/Decision，不能证明真实租户锁下的事务先后；BaseDbUnitTest 的显式导入只有通用 MyBatis 配置，原测试设置 TenantContext 不足以证明 SQL 租户过滤。既有真实 GovernanceServiceTest 覆盖受控根/决议，但没有 T02 的双事务提交先后与跨租户请求断言。本轮只新增独立测试，不扩大到发布、六表导出或完整来源质量验收。

新增：`yudao-module-hrm/src/test/java/cn/iocoder/yudao/module/hrm/service/payroll/HrmPayrollT02ConfirmationContractTest.java`。计划22个执行实例：6个普通测试，4组参数化测试共16实例。只有外部用户/平台权限API使用mock；Requirement、Project、Workbench、Access、GovernanceStore、Root、Decision均使用真实Spring服务及H2持久化。

测试级 TenantSql 配置使用生产的 TenantDatabaseInterceptor / TenantLineInnerInterceptor 并放在MyBatis拦截器首位，断言安装身份；不导入无关的Web/Redis配置。租户2先准备自身锁，再断言租户1对象不可见及相应服务拒绝，避免由“租户2没有写锁”造成伪阳性。该配置证明本测试的DB隔离边界，不是服务器当前租户配置验收。

## 计划第5节逐条覆盖

| 条款 | 当前实现依据与已有测试 | 新候选独立断言 | 尚未覆盖/执行状态 |
|---|---|---|---|
| REQ_EDIT不能篡改reviewerId/status代签 | RequirementServiceImpl.saveInternal区分EDIT/ASSIGN/CONFIRM，保存新CONFIRMED拒绝；原Requirement测试的savingCannotGenerateSignature…、editorCannotAssignOrClearReviewer… | editOnlyCannotForgeReviewerStatusOrUseConfirmationAction：真实受控READ/EDIT根，平台仅query/update；伪造负责人、CONFIRMED及直接confirm分别拒绝且无写入；普通编辑成功后由2本人确认 | 新测试未执行；浏览器需真实EDIT-only角色 |
| 无权、非被分派、失效人员拒绝；有权本人正例 | guardTarget→requireRow先鉴权，当前根/成员/平台权限与业务范围相交；confirm要求actor==reviewer；原assigneeNeeds…、nonexistentReviewer…与Project成员测试 | nonAssignedAndOutOfScopeConfirmersCannotSign；lostMembership…参数REVOKED/EXPIRED/DISABLED；editOnly…末尾正例 | 过期与停用在合成夹具/API mock上设置，非篡改系统时间；页面各角色仍待验 |
| 双会话同版本保存冲突、人工合并重提、持久回读 | saveInternal先真实租户/项目锁，再锁需求并核对version；原staleConfirmation…仅串行过期版本 | twoTransactionsRejectStaleSaveThenPersistManualMerge：两独立线程、事务、连接先读同版本；A写未提交时B不可完成；A提交后B收到VERSION_CONFLICT；A内容保留；按新读版本手工加入B字段再提交，另一事务稳定回读A+B及精确版本 | 服务测试不证明浏览器输入保留或两个独立登录会话。T01同账号双tab v10→v12只证明当时前端冲突恢复；T02仍需独立会话的冲突、合并、重提、刷新、退出/重新登录原件 |
| 语义/规则/来源适用性/来源契约/关系映射/评审人/业务范围失效，展示联系人保留 | reviewContractChanged、Workbench.sourceSemantic/mappingSemantic及invalidate、Project.scope；既有Workbench对单位/类型/空值映射/适用性两向/联系人负责人/供给/范围测试；GovernanceServiceTest对Q-05规则语义与Q-04授权矩阵分离测试 | sevenIndependentSemanticChanges…按7类独立重置；每次真实本人确认后修改，断言目标新语义版本、REVIEWING、签字清除和历史原元组；来源契约/项目范围影响两个消费者，其余只影响所属需求；operationalChanges…独立NAME/CONTACT/OWNER/SUPPLY保留原签字、时间、tuple和语义版本 | 7类是本包业务变更分类，不是对PRD7.5每一种来源字段变体的全覆盖。字段新增/删除/业务含义/唯一键/员工匹配/周期的全部变体尚未独立参数化；来源类型、单位已由既有串行测试覆盖；本轮不扩展完整QS/QC验收 |
| 范围修订→旧确认和确认→范围修订两种提交先后 | 所有治理写首先store.lock真实tenant LOCK FOR UPDATE；T01 confirm新增授权后、治理前范围检查 | scopeCommitBeforeOldConfirmation…：SUBSET旧期间保留，scope先提交后旧确认收到SCOPE_STALE，无签字；confirmationCommitBeforeScope…：确认先提交，随后scope清当前签字并重审，历史仅有旧范围版本1的签字，没有范围2配旧签字 | 新测试未执行；H2锁和两个物理连接，不等同MySQL生产引擎或HTTP会话 |
| 来源/映射改变后的旧元组不能确认 | saveSource/saveRelation传播需求版本和签字失效；confirm锁后核版本 | dependencyCommitBeforeOldConfirmation…分别SOURCE/MAPPING：已有签字后变更先提交，另一事务旧版本确认收到VERSION_CONFLICT，当前REVIEWING且旧tuple仅历史可见 | 仅需求确认子集；不据此关闭发布并发等完整AC-19/26 |
| 撤权后旧签字保留当时事实；失效人员不能继续确认/读受限历史；新授权成员续办 | Access.effective及Root.validRoot实时检查；撤销不回写需求旧签字；assign使当前签字失效，history按当前权限过滤 | lostMembership…分别撤销真实ROOT、过期grant、停用用户；撤权前后tuple/签字时间一致且当前事实有效；原人员confirm/history拒绝；其他授权人可读旧历史；1分派3后3确认，2原tuple在历史保持 | signer4与reviewer2分离，避免停用reviewer意外撤掉独立授权根签署人。浏览器真实退出/新角色续办仍待执行 |
| 同租户跨项目及跨租户对象替换拒绝，本项目正例 | guardTarget、selected/previous项目一致性、Workbench.object校验及生产租户SQL过滤 | crossProjectAndCrossTenantObjectSubstitution…：对第二项目无权的2不能确认；有两项目权限的1也不能移动需求或复用关系对象；切tenant2后的mapper不可见、confirm/history/saveRelation/save拒绝，返回tenant1后原需求/历史不变且正例修订确认成功 | 只覆盖本包需求/来源/关系；问题、发布/下载等越界测试属于后续包 |

## 并发测试方法与边界

`orderedTransactions`使用两个独立REQUIRES_NEW / READ_COMMITTED事务及两个线程，断言连接对象不同、autoCommit=false、起读版本相同。屏障保证两方完成旧版本读取；第一方完成业务写后暂不提交，第二方开始服务调用且在200ms观察期内无法返回；释放第一方提交后，分别核对第二方错误code或成功结果及数据库/历史不变量。所有等待有界，finally释放首事务并等待worker退出，防止SQL清理与后台线程并行。

这些测试调用真实受控根及决议服务，夹具使用TEST合成受控起点和批准，不宣称正式业务批准。仅搭建确认所需的最小治理前提，不执行发布、导出或完整来源质量链。

## 执行交接

最小新增测试命令（仓库根目录）：

```bash
JAVA_HOME=/Users/waitforpro/.local/jdks/java8/Contents/Home /opt/homebrew/bin/mvn -B -ntp -pl yudao-module-hrm -am -Dtest=HrmPayrollT02ConfirmationContractTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Luna先只执行新增测试，保留命令、退出码、Surefire XML及失败现场；若失败返回coder，不自行改产品代码。新测试通过后再按主线程确定范围执行既有Requirement/Project/Workbench测试与真实GovernanceServiceTest，不重写既有冻结证据。测试/复核者请求gpt-6-luna/max；实际模型须由运行端证据单列。源码/测试通过不能自动关闭本包仍待独立会话浏览器验收的条款，也不能扩大为完整AC-14/19/26通过。
