# FA-01及独立复核发现处理报告

日期：2026-10-20。本轮依据本机`docs/hrm-payroll-stage-1-6-fix-audit-reaudit-20261020.md`，处理发布依赖范围及证据留档问题；原审计与独立复核文件保持只读。

## 1. 处理结果

| 发现 / 缺口 | 本轮处理 | 状态 |
| --- | --- | --- |
| FA-01：无关项目ROOT、ANCHOR及人员变化干扰发布/同号重试 | 收窄依赖摘要，新增四个回归测试；修复前反例失败，修复后133项相关测试通过；真实IAB跨项目发布/重试及相关变更拒绝通过 | 工程修复及本轮受影响回归通过 |
| 上轮浏览器只读抽查未独立留档 | 本轮重新实际控制IAB，保存全量AX、截图和分步骤索引。不是补造上轮截图，也不回写原报告为已留档 | 本轮证据补齐 |
| 上轮20份XML核对未另存本轮日志 | 本轮重新运行只读核验器，对保存的20份负载文件逐字段比较并独立保存文件哈希及结果 | 本轮证据补齐；不是重跑负载 |
| MB-01：真实受控授权/条款批准缺失 | 没有以TEST材料代替真实批准；正式动作门槛未放宽 | **材料阻塞，未完成** |
| AV-01 / F-15：正式组合验收 | 本轮仅受影响修复链；未重跑全部28AC或正式材料链 | **未关闭** |

原F-07、F-15继续开放，原13/15自身工程包有证据支持的统计不变；S5仍7/8、S6仍6/8，不因FA-01修复宣称整个PRD通过。

## 2. 代码与保持的边界

修改`HrmPayrollGovernanceReleaseService.dependencies()`：

- ROOT限定为本项目登记的根，以及本项目授权记录明确引用的根；引用根按ID稳定排序，未简单遗漏可能的上游根。
- ANCHOR从租户级`projectId=0`集合按这些根实际引用的anchorId选取。
- 人员事实限定为当前发布人、项目负责人及项目需求/来源/问题/授权/决议/根/锚点中的明确人员引用。未把任意对象ID当成用户ID；根和锚点中的签署人、保管人及决议批准人仍纳入。
- 项目内需求、工作台节点、授权、决议等依赖仍参与摘要。保留当前平台/对象/完整闭包/授权根检查，以及“相关依赖校验先于幂等结果返回”的顺序，避免借同号重试绕过撤销、停用或决议失效。
- 没有变更release快照格式、历史投影、下载再鉴权或数据库结构。不需要迁移。

本修复解决依赖集合过宽导致的错误冲突；没有取消原有租户串行锁，也没有宣称消除全局人员查询/锁的性能成本。

源码：[ReleaseService](/Users/waitforpro/source/github-yudao/ruoyi-vue-pro/yudao-module-hrm/src/main/java/cn/iocoder/yudao/module/hrm/service/payroll/HrmPayrollGovernanceReleaseService.java)、[治理回归测试](/Users/waitforpro/source/github-yudao/ruoyi-vue-pro/yudao-module-hrm/src/test/java/cn/iocoder/yudao/module/hrm/service/payroll/HrmPayrollGovernanceServiceTest.java)。

## 3. 修复前后及真实浏览器结果

1. 新增反例在修复前执行失败：无关项目新增ROOT、无关ANCHOR及无关人员变化后，甲项目dependencyToken变化。原始失败日志和哈希保留；不是仅静态推演。
2. 修复后新增用例覆盖：无关项目登记/独立签署根、无关锚点/人员不影响首次发布或同号重试；相关人员/锚点变化拒绝旧请求；相关决议撤销后拒绝已成功请求的重试；发布人停用后同样拒绝。既有同号改正文、根撤销、范围变化和六表等测试一并运行。
3. 实际IAB在修复后运行包上对甲项目105执行预检查；随后实际API在新TEST空项目148登记待签根1350；甲浏览器发布成功，产生**1352 / 发布版本3**。
4. 乙148再登记待签根1354；甲IAB点击“重试相同发布请求”，仍为1352。MySQL按幂等号`TEST-FA01-IAB-20261020-0928`核到release仅1条，甲总数从原2增加到3；乙没有release。
5. 在甲105新增待签根1356后，相关依赖变化：真实API旧令牌返回业务拒绝码1050810003；同一IAB再次重试出现“发布或重试未完成”并保留输入，release仍1352，没有额外版本。该拒绝是本轮预期负例，不是未处理的启动故障。
6. IAB点击1352“查看冻结六表”，可读取SUMMARY、DEC-604、GRANT-PUBLISH及根关联；旧900/1347仍列出。实际API另下载当前候选1352的任务1358，单文件638字段、六表及显式引用校验通过。此项不冒充IAB下载、原生打开或两次重导出比较。

真实API/IAB的乙项目变更是**待签登记**，没有新增实际成员权限；独立签署、无关锚点和人员变更另由H2服务测试覆盖，证据类别不混称。

证据：[索引](/Users/waitforpro/source/github-yudao/ruoyi-vue-pro/docs/hrm-payroll-fa01-evidence-20261020/README.md)。

## 4. 测试、候选和证据身份

- 起点：`work` / `5f8fa3d9a0b607073748b6b60c4cf4fced75d8bb`。
- 最终运行包SHA-256：`619d2cba0d4da4c767425ea2fd4c4a958ea7026e3d3d243a8fca412f1b688af2`；1,109个HRM class与包内逐字节相同。源码哈希见candidate.json；浏览器后未再修改业务源码。
- 最终相关测试：权限9、评审17、工作台25、项目26、治理25、需求31，共**133，失败0、错误0、跳过0，BUILD SUCCESS**。H2真实服务及数据库，外部账号/平台API模拟，不冒充真实业务资格。
- 修复前1失败、首轮修复后131通过、补充两个安全负例后最终133通过分别留档；不能把首轮当最终总数。JDK8后端打包成功，未修改前端。
- 本轮重新核验旧run3的20个XLSX，每份40,245字段、恰好六表、显式关联无孤立引用；`xml-reverification.json`独立留档。未新跑10分钟性能；本轮只改发布依赖构造，未给旧性能结果换成新jar的运行结论。
- 新1352下载的实际文件另由`new-release-workbook.json`标明单文件校验，不用于“旧版本重导出一致”的主张。

相关测试命令：

```bash
JAVA_HOME="$HOME/.local/jdks/java8/Contents/Home" /opt/homebrew/bin/mvn -pl yudao-module-hrm -am test -Dtest=HrmPayrollGovernanceServiceTest,HrmPayrollRequirementProjectServiceTest,HrmPayrollRequirementServiceImplTest,HrmPayrollWorkbenchServiceTest,HrmPayrollReviewServiceTest,HrmPayrollRequirementPermissionTest -Dsurefire.failIfNoSpecifiedTests=false
```

## 5. 数据、服务及提交边界

测试前实际执行`~/.hrm-payroll-local/manage.sh backup`备份数据库和匹配配置。新项目148是SQL复制105范围字段后建立的**独立空TEST夹具**，明确不作为项目创建权限验收；两条乙项目待签根和一条甲项目待签根均未签署，不扩展成员权限。已有授权、原基线900/1347没有被修改或删除；新增1352仅为TEST合成发布，非正式批准。

配置、凭据、税务密钥及备份留仓库外；没有重新初始化数据库、运行迁移或调用短信/邮件/银行/税务外部服务。后端通过`launchctl kickstart -k gui/501/local.hrm.backend`重启，最终后端health UP、前端HTTP200，MySQL/Redis服务沿用既有本机运行环境。

私有日志：`~/.hrm-payroll-local/fa01-before-20261020.log`、`fa01-after-20261020.log`、`fa01-final-tests-20261020.log`、`fa01-package-20261020.log`；夹具脚本、状态和新下载在`~/.hrm-payroll-local/fa01-20261020/`。服务管理仍为`~/.hrm-payroll-local/manage.sh status|stop|start|restart`。

仅白名单提交这次服务修复、测试、新证据、处理报告及进度附记；保留原交接修改和全部原有未跟踪输入，不把原审计文件混入提交。实际提交与push结果由本轮`delivery.json`记录，未执行成功前不记成功。

## 6. 剩余动作

FA-01在上述工程边界关闭。业务方仍须提供真实根、当前范围/授权矩阵及必需条款的受控批准材料，再在确定候选上补齐AC-25/28正式链及受影响组合验收。MB-01/AV-01不能由开发者补签或用本次TEST发布替代。
