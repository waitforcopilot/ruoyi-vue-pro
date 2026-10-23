# 项目153最小APPLICABLE规格基线UI夹具（内部建议）

2026-10-23。仅只读当前源码形成UI路径，未操作浏览器/数据库、未执行测试、未改已冻结SFC或1844。本路径尚未实际验收；1433七Q补充与1407互补生效取自主线程最新交接，是否覆盖最终新对象/范围仍以实际预检为准。1426保持冲突拒绝，不将它设为有效依据。

## 最短串行路径

1. **266创建新的COL意见需求。** 从治理页进入「返回十视图评审」，选项目153/COL，点击某一明确主题的「围绕…提意见」（newOpinion），新编号由服务器生成。来源适用性选APPLICABLE、范围INHERIT项目当前v5，状态保持PENDING。功能说明用`TEST_WBC_T04T07_SPEC_ONLY_20261023`；填写11项业务场景（下表）、规则引用版本`TEST-T04T07-RULE-V1`、验收条件「规格核验PASS且本机TEST基线可发布，未供数不得显示数据就绪」、依据「仅本机TEST工程，正式业务批准另行安排」。保存并记录新需求ID，不复制/编辑1844。若只能从传统页新增，先新建PENDING草稿，再十视图「编辑意见」补齐场景；直接十视图创建少一次编辑。
2. **266仅对新需求做范围决策/分派。** 十视图新行「范围决策」→INCLUDED（纳入本期）→本次操作依据「TEST仅规格纳入，待提供数据另行核验」→保存独立操作。到「需求、成员及实际来源」传统需求页153/COL，给新ID分派当前下拉列表可选的有效确认人，优先269（须本次实际可选/仍有READ+CONFIRM）；不能自派266，也不能因267拥有DECISION_APPROVE就分派它确认。267新root1430仅6动作，不含CONFIRM。全过程不保存旧1844。
3. **266新登记一个来源/一个消费关系。** 在同页下方「登记实际来源」，填写下列最小字段，来源scope等于项目153当前v5解析范围（主体101、组织201、TEST人员/场景、2026-10-01至10-31），owner选仍有该来源READ的266。保存后「供给交付」选PENDING/待提供，替换交付内容保持false，不填虚构交付digest/reference。随后「建立消费关系」选新需求+新source，填一个字段映射及消费检查规则。不能把需求表单的字段映射/来源系统自由文本当真实关系。
4. **267只做两个规格PASS。** 重新登录267，在项目153实际来源行「规格核验」选择PASS，填写执行依据「TEST依据已登记hours字段规格独立核对，未进行供数样本质量验收」、样本内容摘要「TEST规格字段示例hours=1，仅定义说明，数据待提供」、本次操作原因。再到消费关系页「映射核验」做RELATION_SEMANTIC PASS并填写对应依据/摘要/原因。先SOURCE_SEMANTIC后RELATION_SEMANTIC，两者须是当前bindings。此时不执行QS/QC、不改PROVIDED、不伪造供数或质量结果。1430当前scope必须覆盖新来源/需求且有SOURCE_VERIFY及必要READ/决议授权。
5. **被分派确认人本人确认。** 重新登录步骤2实际分派人，刷新新需求/来源关系，确认范围MATCH/v5、APPLICABLE且有效mapping存在、完整场景/验收/规则/依据已保存；点击新需求「确认版本」，确认最新版本。所有语义/映射/范围操作应在此之前完成，避免立即失效。本人确认与来源语义PASS独立，来源仍PENDING、数据就绪应MISSING（无PROVIDED/QS/QC）。退出重登/刷新独立回读确认及关系。
6. **266新增DATA_READINESS问题并发布TEST规格基线。** 新问题只关联新需求（必要时加新source），描述「TEST供数待提供；仅影响数据就绪」，类别DATA_READINESS，handler266、reviewer279（须当前有效且不同）、有效日期如2026-10-31、本次操作原因。wholeProject保持false，保存为OPEN，保持未关闭。回治理153执行预检，保留未决DATA_READINESS问题/待提供供给与allowed结果；条件齐备后以266有权发布者填明确TEST说明、使用当前token/请求号发布并独立回读release。若还有其他blocks，按实际提示停止解释，不能把本路径静态可行写成当前项目已全部满足。

## 必要表单值

| 表单 | 最小完整填写 |
|---|---|
| 业务场景11项 | 业务角色=TEST HR；触发场景=TEST月末盘点；业务目标=明确工时规格；期望行为=冻结已确认规格；正常样例=hours1；异常样例=缺hours拒绝；计算或业务规则=原值取小时；单位=hour；精度与舍入=2位四舍五入；有效期或适用依据=TEST2026年10月；规则例外与处理=异常人工复核 |
| 来源 | 名称=TEST_T04T07_PENDING_HOURS；模块COL；负责人266；技术联系人=TEST IT；获取方式=TEST文件；接口或文件规格版本=TEST-HOURS-V1；唯一键=employeeId,period；员工匹配方式=TEST employeeId；频率及周期粒度=月；截止时间=月末；预计数据量=TEST1行；来源检查规则版本/摘要=TEST-QS-V1；字段hours/业务含义TEST工时/type decimal/unit hour/required默认true；本次操作原因明确TEST |
| 消费关系 | 新需求ID+新sourceID；消费检查规则版本=TEST-QC-V1；来源字段hours；消费页面字段workingHours；业务含义TEST工时；type decimal；unit hour；required默认true；转换或汇总规则=原值；空值处理=拒绝；重复处理=按employeeId,period去重；跨期处理=同月；异常处理=人工复核；本次操作原因明确TEST |
| 核验 | 两个layer分别SOURCE_SEMANTIC/RELATION_SEMANTIC；result PASS；reference/sampleDigest/reason非空且明确仅规格，不把无真实交付说成QS通过 |

## 角色及源码约束

- 266需技术权限query/update/assign/scope-decide/source-edit/issue-manage/baseline-publish，以及当前项目根/成员动作READ、EDIT、ASSIGN、SCOPE_DECIDE、SOURCE_EDIT、ISSUE_MANAGE、BASELINE_PUBLISH；完整发布另需全闭包READ/PERSON_READ/DECISION_READ/GRANT_READ与相应平台读权。当前全部授权不由本静态建议重新核实。
- 来源owner须active且有该source READ；确认人须active、当前scope READ+CONFIRM、平台query+confirm并等于已持久化reviewer；独立来源核验者267须scope覆盖的SOURCE_VERIFY及平台source-verify/必要读取。来源范围必须覆盖需求范围，mapping字段必须来自来源定义。269仅是优先候选，若本次下拉不可选应另核有效成员，不硬塞ID。
- `WorkbenchPanel.vue:552`的editSource、editRelation、supply、verify对应上述UI；`requirement/index.vue:165`分派说明明确不可自派；`review/index.vue:881`的newOpinion直接创建完整场景意见，`review/index.vue:250`范围决策。
- `HrmPayrollWorkbenchService.java:175` specificationReady只需来源/关系当前语义PASS；`:176` available另要求PROVIDED、QS/QC及无DATA_READINESS；`:265` DATA_READINESS阻断available，`:268`只有关联未关闭BASELINE_BLOCK进入baselineBlocked。`HrmPayrollGovernanceReleaseService.java:88`等逐需求门槛查scope/当前确认/规格，不把PENDING供数直接当基线阻断。
- `HrmPayrollWorkbenchService.java:205` confirmationTuple要求有效来源/关系/范围/owner但不要求PROVIDED或QS/QC；`HrmPayrollRequirementServiceImpl.java:99`本人确认还检查当前范围、治理、版本及actor=reviewer；`HrmPayrollReviewService.java:37`要求完整11项业务字段。

当前源码中未找到`SOURCE_NOT_PROVIDED`字面枚举/原因code。本轮应记录真实UI的PENDING/待提供、实际MISSING及未决DATA_READINESS，不发明后端原因码。待提供自身已使available=false；额外DATA_READINESS存在且规格仍发布，可证明该类别未阻断规格发布，但不能单凭此复合条件证明「仅该问题造成数据不可用」。需要单变量证明时另用当前真实已就绪夹具做前后对照，不为此伪造数据质量PASS。
