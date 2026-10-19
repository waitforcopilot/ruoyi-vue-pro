# S5 验证证据索引（2026-10-19）

最终实现8/8，合成工程回归8/8；阶段任务已验收7/8、阶段验收7/8。A1d真实材料阻塞，正式业务批准和正式发布未完成。详见[报告](../hrm-payroll-stage-5-report-20261019.md)。

候选起点 dc799eb9c12024b52690e92ea9ee4ebc95f8fcb3；分支 work。本目录只含合成资料及脱敏执行结果，私有凭据、完整备份和原始日志在 `~/.hrm-payroll-local/stage5-20261019/`。正式授权根/真实业务批准材料未提供；TEST 签署及发布不代表正式批准。

## 验收矩阵

实现、合成工程回归与真实材料分别判断；每组必需子例全部满足才计入对应口径。最终状态见阶段报告，不能由本表标题或测试数量推定 PASS。

| 组/子例 | PRD/AC | 前置、操作和独立预期 | 执行证据与类别 |
| --- | --- | --- | --- |
| A1a | 8.3/9.1 | 无迁移制造的根；独立登记材料→起草→另一账号签署，禁止自签 | anchor-registration.json、accounts.json、browser-06；实际API/DB/IAB；H2独立动作负例 |
| A1b | 9.1/AC18 | PROJECT_CREATE/SCOPE/MEMBER/DECISION/PUBLISH分别鉴权；旧技术授权不能作为正式根；TEST根不能创建正式项目 | final-runtime.json、127项数据库检查中的受控服务用例；实际API与H2外部系统接口模拟分列 |
| A1c | 8.3 | 账号/授予人/受领人当时状态、有效期、委派上限、完整材料和摘要冻结 | 新ROOT/授权事实模型、六表GRANT与ROOT/ANCHOR关联；实际DB/导出 |
| A1d | 正式材料边界 | 实际有权业务人员提供真实根材料和签署；不能由技术管理员/PRD/本决议自授 | **材料缺失，未验证真实正式批准**；TEST隔离和拒绝正例另记，不把材料缺失改成PASS |
| A2a | 8.3/AC28 | decisionId与不可变业务版本，正文/范围/Q/条款/条件修改新版本；指定他人批准 | IAB草稿35→提交→267批准→生效；browser-02；真实数据库与浏览器 |
| A2b | 8.3/9.1 | 起草账号不能批准、旧版本动作拒绝、根/决议读权独立 | final-runtime.json、受限浏览器截图及实际API；服务层H2自批/重叠负例 |
| A3a | 8.3/AC28 | 同起点D1/D2，草稿不影响、矩阵替代保留签字、撤销/到期阻断、V1当时依据不漂移 | remaining-cases.json 的独立实际项目；runtime-cases.json真实时间future→effective→expired；H2固定Clock补充 |
| A3b | 6.8/7.5/AC26 | 新范围旧决议/确认拒绝；新范围另批，来源覆盖显式修订，语义复核不复用QS/QC | concurrency-scope.json 的S3_NEW_SCOPE…；真实API/DB |
| A3c | 7.5/8.3 | Q05语义变更必须重审；单纯Q04矩阵变化不改旧签字；新动作查当前权 | lifecycle与remaining-cases；H2实际治理服务；明确不把单纯新决议等同需求重签 |
| A4a | 6.4/8.2/AC10 | S0 46/46/40、S1 47/47/40、S2 47/46/40及六表行数；语义重审39/40拒绝；空草稿 | statistics-publication.json、final-runtime.json；真实API、DB与固定快照 |
| A4b | 8.2/AC25 | 缺决议、旧范围、无发布权、缺完整闭包、旧签字事实、规格未明/阻断/排除理由未齐拒绝 | api-steps、final-runtime、remaining-cases、browser-01；H2归档/失配/理由负例 |
| A4c | 7.4/8.2 | 规格明确但未供数可发布需求基线，来源仍0/2，不宣称可投产 | 主项目87 V1=32，v1-projection-before.json；真实source/SEM/确认/发布 |
| A4d | 8.2/AC25 | 同号同内容同release；同号不同内容拒绝；撤销后不靠旧幂等结果越权 | API实际同请求、同步并发重试；IAB新请求250重试稳定；browser-03 |
| A5a | 8.1/8.3/AC20 | U全部、排除项、来源/交付/SEM/QS/QC/映射/问题/规则/显示名/决议和授权完整冻结 | 主项目87及范围链95；snapshot graph、六表解析与数据库证据 |
| A5b | 8.1/AC28 | 旧确认引用的已替代决议仍有DECISION行；历史失活映射仍可解析；移除NA仍有声明 | 实际服务H2专项；完整快照冻结所有决议版本，统计不纳入失活关系 |
| A6a | 8.1/AC16 | 固定六表，无第七表；不适用无假sourceId；长中文按同记录续列保存 | XLSX真实下载，Artifact Tool读取/渲染，POI长文与UTF16边界检查 |
| A6b | AC16/20 | 实际打开文件；旧V1修改当前资料后重导出，业务单元格/排序/关系/统计相同 | native-01-summary.png为实际LibreOffice打开，xlsx-preview为独立渲染；xlsx-reexport-comparison.json以及最终完整关系重导出 |
| A6c | 8.1/AC20 | 空草稿/筛选/权限节选独立标签；需求和范围表保持U口径，排除项不消失 | final-runtime.json、statistics-publication.json、browser-04及筛选任务 |
| A7a | 8.1/9.1/AC19/20 | 当前权限与冻结范围交集，两隐藏来源仅一个占位；全部受限决议/授权一个占位；自由文本同步裁剪 | runtime-cases、browser-04；H2可见来源/映射中隐藏批准正文的专项负例 |
| A7b | 8.1/AC19 | 创建/生成/交付/领取逐次鉴权，旧会话/已知ID/旧READY任务撤销后拒绝 | 真实API、browser-05；queued生成前撤销及错误领取H2测试，明确模拟外部账号接口 |
| A7c | AC20 | 筛选不得输出未选需求的孤立问题引用；独立稳定GRANT/DEC/ROOT/ANCHOR记录号 | 实际投影图校验与H2；过滤完整性标签和可见统计检查 |
| A8a | AC18/25/28 | IAB完整链：起草→他人批准→拒绝修正→本人确认→发布/重试→下载/打开→重导出→裁剪/撤销 | browser-01～最终截图、browser-steps；最终候选SHA/构建另见candidate.json |
| A8b | 8.2/8.3/AC25/26/28 | 发布×根撤销/范围变更/决议撤销/来源变更真实并发，精确重试仅一个release；不得拼不同版本 | concurrency-scope.json实际提交时间/返回码；额外构造H2一致性负例 |
| A8c | 迁移/保护 | 原记录原字段、113业务表、只读材料SHA；迁移重入及隔离恢复，原始密钥不输出 | preservation.json、legacy-migration-regression.json、migration-repair.json；真实MySQL |
| A8d | 交付 | 类型/ESLint/测试/运行class候选匹配，最终IAB通过才提交push，核对远端SHA | candidate.json、checks.json、delivery.json；不是仅页面已打开 |

## 失败与修复记录

1. 实际浏览器发现导出服务READY而UI仍QUEUED。原因是修改push前的原始对象，已改为替换响应式数组中的记录，再实际下载。
2. 权限指令直接删除按钮DOM，刷新时Vue出现`__vnode`/`insertBefore`异常，已换成声明式权限条件；最终完整刷新和独立签署/下载按影响补验。
3. 旧20261016修复迁移按旧平面字段判定S3签字，重放时误改170条原合成需求。按起点备份逐字段恢复，并撤回本轮误追加的170条系统历史，原历史未删；错误副本保留私有目录。迁移已排除有结构化确认元组的记录，并分别测试原安装顺序、实际无效旧签字、结构化签字及两次重入。不得把首次失败当通过。
4. 一次下载发生在后端重启窗口，UI领取拒绝；后端就绪后同一任务重新鉴权下载成功。并发测试导致依赖已变化时旧发布重试被拒绝，另在依赖稳定时验证同号返回原release。
5. 生命周期恢复测试曾用追溯到年初的生效区间与更早版本重叠，服务端正确拒绝；测试改为真实当前生效区间，不放宽重叠门槛。范围复核曾遗漏显式修订来源覆盖期间，服务端正确拒绝；按完整前置补齐来源范围后重新核验通过。

## 前置阶段接回

- S2：移除新项目TEST确认捷径；PROJECT_CREATE、范围和委派必须有有效受控根；已有技术授权仍只作历史/普通访问，不升级业务根。
- S3：确认、来源语义/QS/QC、问题关闭冻结动作时决议/授权；新范围政策及QS/QC恢复链已接入。旧证据/签字不据新决议自动重写。
- S4：统计不是发布；固定S0～S2全量U/排除项进入六表，语义重审拒绝发布；旧项目85原签字恢复后仍不自动具备S5批准事实。
- 原S1～S4报告是当时候选证据，本轮修改处按受影响范围补验，不冒充此前版本已含S5能力。

## 最终补验与复核入口

- 最终浏览器：browser-09-final-restricted（快照431按当前权限只留一个受限发布依据）、browser-10-final-revoked（任务440原READY，撤销303/312后旧会话领取DENIED并清缓存）、browser-11-filtered-download（需求803，任务470，筛选节选并实际下载）。browser-final-console.json仅指最终登录文档，error为0，不覆盖之前已修复错误。
- decision-replacement-concurrency.json补充实际“独立批准D2替代×发布”竞争；旧依赖重试拒绝，V1不变，新请求用当前D2。与concurrency-scope.json的四种竞争及同号竞争分开列。
- complete-mutation-comparison.json逐次记录五类当前数据改变后431不漂移；xlsx-final-verification.json用独立XML检查六表231字段、29处长文续列、排序/引用和同权限重导出。原生实际打开见native-02-final-complete.png；六表独立渲染见xlsx-final-preview-1～6.png。Artifact Tool空共享字符串误读为序号，已用标准库XML读取核对原值，没有改变业务文件迁就检查工具。
- preservation-final-readonly.json是全部测试变更后的再核对：125张表原记录/字段、113业务表checksum保留；preservation.json另包含两次迁移与隔离恢复。迁移初次误写及逐字段恢复见migration-repair.json，错误副本仅在私有目录。
- checks.json、candidate.json记录最终候选/实际构建、127测试及类型/格式/运行检查；submission-safety.json为提交白名单和私有密钥值扫描。delivery.json记录实现提交/远端核对；收尾文档提交SHA由最终交付回复给出，避免文件自引用SHA。

可复现只读六表检查器：scripts/hrm/verify_governance_workbook.py；服务层实际治理回归：HrmPayrollGovernanceServiceTest。接口/并发夹具的执行脚本及会话留在私有目录stage5-20261019；原始文件不包含在公开证据中。S6完整AC、性能及正式真实人员材料不属于本轮已通过结论。
