# T04只读预审与最小候选设计

日期：2026-10-22。内部过程证据，不是最终fix报告。coder期望/请求gpt-6.1-sol / medium，实际模型未核实。本轮仅读计划§7、PRD6.7、Workbench/Review/GovernanceRelease/Projection服务及相关测试，新增本设计文档；不新增Java、不改产品或既有测试，不运行编译/测试、DB、浏览器、重启或提交，避免影响executor正在验证的T03候选。

## 静态现状及缺口

| 计划要求 | 实现与已有测试 | 最小补验 |
|---|---|---|
| 上海2026-10-17逾期边界 | `WorkbenchService.dates(Issue,Clock)`把Clock转换Asia/Shanghai，以today>dueDate、state非CLOSED决定当前逾期；closed保留wasOverdue。Workbench既有`fixedShanghaiClockCoversAllOverdueStatesAndClosedHistory`逐状态检查10-16/10-17，但只测一个UTC时刻，尚未证明上海跨日边界 | 固定Clock的2个UTC时刻（2026-10-16T15:59:59Z与16:00:00Z）分别对应上海10-16末/10-17初；OPEN/PROCESSING/REVIEW×due16/17独立断言unresolved/overdue/wasOverdue；CLOSED×两期限断言false/false且历史wasOverdue保持。无需为这个纯函数测试改产品时钟或本机时间 |
| BASELINE_BLOCK全项目/影响I阻断；仅排除项不阻断且保留说明 | Workbench.blockers取INCLUDED集合，与issueImpact交集或wholeProject判定；Release.judgement把baselineBlocked转换BASELINE_ISSUES_OPEN。既有`baselineBlockOnlyBlocksIncludedIntersectionOrWholeProject`实际主要验证单个需求从未纳入到纳入的串行标记，没真正发布；真实Governance测试缺独立问题影响矩阵 | 独立重置全项目、仅I、仅DEFERRED、仅候选/REMOVED夹具；真实Review.decide创建范围依据，I有有效功能确认/TEST决议。明确断言issueIds、发布gate允许/拒绝；排除项的reason/responsible/reevaluate与问题事实留在release snapshot中。只读snapshot原文即可，不扩展六表下载/原生Excel验收 |
| DATA_READINESS只影响来源可用不清功能签；ADVISORY未决但不阻断 | Workbench.dataBlocked仅未关闭DATA_READINESS；available调用它。Release规格基线门槛检查specificationReady而不要求data readiness清零。Review统计计未决/逾期但只将BASELINE_BLOCK纳入blockIds。既有Workbench issue链已说明关闭不自动确认/核验，T03候选补不造签/质量 | 一组来源/关系先达到可用且需求已确认，再开DATA_READINESS：tuple/confirmedBy/time保持、available=false、发布仍允许；关闭时已有匹配证据可恢复available属于移除问题阻断，并非自动登记QS/QC。另一独立组ADVISORY：未决+1、签字/可用保留、发布允许。不要增加所有数据问题清零才可发布的条件 |
| handler不得降级/缩小影响；独立reviewer补依据才可改 | issueAction将RECLASSIFY映射ISSUE_REVIEW，并要求actor==reviewer且不同handler；required evidence；之后重新issueAccess/issueValid。现有Workbench handler拒绝一例后reviewer重分类成功，但缺缩小影响和逐字段历史断言 | BASELINE_BLOCK影响I时handler分别尝试改ADVISORY、把影响改排除项，均PROJECT_DENIED且完整records/issue版本/发布gate不变；独立reviewer缺evidence→INCOMPLETE不写；补依据后版本+1、actor/reason/前后影响历史正确，发布gate按真实变更重新计算。对relationIds缩小若UI/API不支持不要假造既有能力，需求ID影响集合已足以验此条 |
| 旧需求/来源/映射binding关闭拒绝且回滚 | issueBinding包含需求version/semanticVersion、来源record version、关联relation record version、scopeVersion/cycle。CLOSE先创建ISSUE_CLOSE证据再校state/binding，外层事务应回滚。Workbench既有`issueCloseRejectsChangedAssociationsAndEmptyEvidence`仅assertThrows；T03候选已补需求改变时精确VERSION_CONFLICT及全部业务记录回滚 | T04只补SOURCE/MAPPING变化两例，与T03需求例互补；旧expectedBinding必须VERSION_CONFLICT，issue/state/version、ISSUE_CLOSE EVIDENCE、HISTORY、TODO及其他记录不变；读最新binding后正确reviewer可关。不要因先发INSERT语句就认定已落库漏洞 |
| CLOSE/REOPEN与发布提交一致性 | Workbench整个Service及Release整个Service均@Transactional READ_COMMITTED，写入首先同tenant治理LOCK FOR UPDATE。Release在锁内重新验证dependencyToken/gate，并冻结requirements/workbench/statistics/relationFacts。dependencies包含完整workbench nodes，故关闭/重开也会使旧预检token过期。现有T02双真实事务方法已验证锁/连接，但未覆盖问题与发布 | 使用相同双连接有界屏障方法，分别固定issue事务/发布事务先后；精确检查快照issue状态、version、statistics和当前状态，拒绝token过期后的拼接快照，详见下节 |

当前静态审查没有足够证据认定产品缺陷；先补契约断言。Review统计在E集合筛选问题、发布快照仍保存全项目问题，两者用途不同，不能仅凭统计不含REMOVED项就认定排除说明丢失；应读真实冻结snapshot再判断。

## 四种事务先后，避免错误的成功预期

新测试候选建议`HrmPayrollT04IssueConsistencyTest.java`独立文件，沿用T02/T03最小真实治理与租户SQL夹具，增加真实ReviewService、GovernanceReleaseService；只有外部用户/平台权限API可mock。Release.userFacts依赖外部系统表，测试按既有GovernanceServiceTest提供受控固定用户事实（若用Spy只stub此边界，不stublock、precheck、publish、issueAction或业务判断）。不修改既有GovernanceServiceTest/ExportService差异。

1. **CLOSE先提交，旧token发布后到：**OPEN/REVIEW BASELINE_BLOCK预检取得旧依赖token；issue CLOSE在事务A成功但暂不提交，B使用旧token publish等待tenant锁；A提交后B拒绝INCOMPLETE，无RELEASE/EVENT增写。重新precheck取得当前token再publish成功，快照issue=CLOSED，统计未决/阻断与该版本一致。
2. **发布先提交，CLOSE随后提交：**使用不阻断的ADVISORY或DATA_READINESS问题，以满足发布正例前提；A publish冻结未关闭问题并暂不提交，B CLOSE等待；提交后当前问题CLOSED，但已冻结release仍为原未关闭状态与原统计。不能用未关闭BASELINE_BLOCK作为发布成功前提。
3. **REOPEN先提交，旧token发布后到：**初始CLOSED BASELINE_BLOCK预检允许；A REOPEN暂不提交，B旧token等待；A提交后B拒绝且无快照；新的precheck必须BASELINE_ISSUES_OPEN拒绝。此处区分先因token过期拒绝和新gate仍有阻断两个独立断言。
4. **发布先提交，REOPEN随后提交：**初始CLOSED问题；A publish冻结关闭事实，B REOPEN等待；A提交后当前OPEN/新cycle且发布门槛重新阻断，但旧release保持CLOSED/旧cycle及原统计。不能把后来重开写回旧快照。

双连接都先读固定起始身份/版本；第一业务调用完成但未提交时，以第二操作超时未返回及连接不同/autoCommit=false作为真实锁观察，随后释放并断言精确结果。所有等待有界且finally停止worker，清理不与后台线程竞跑。H2事务结果不升级为MySQL生产并发。

## 最小执行与UI边界

收到主线程编码交回后，先新增测试文件，不改业务；只有确定性失败证明违约才报告具体数据链并最小修复。最小执行命令预计为：

```bash
JAVA_HOME=/Users/waitforpro/.local/jdks/java8/Contents/Home /opt/homebrew/bin/mvn -B -ntp -pl yudao-module-hrm -am -Dtest=HrmPayrollT04IssueConsistencyTest -Dsurefire.failIfNoSpecifiedTests=false test
```

当前该Java候选尚未创建，命令仅设计，不可报告已执行。Luna独立测试后再决定联合T02/T03/Workbench/Review/Governance回归；不因本预审关闭AC-22。

正常可见UI代表流程建议在保留TEST项目：问题类别与影响I/排除项的预检区别、独立复核重分类依据、DATA_READINESS对功能签字保留与可用性的分离、关闭/重开历史和门槛刷新。上海2026-10-17固定日期只在自动化Clock夹具验证；若当前运行界面不能注入测试时钟，就准确记录页面当日及测试期限，不改本机时间，也不声称页面回到固定日期。并发提交顺序由确定性服务事务承担，截图不替代数据库事务证明。
