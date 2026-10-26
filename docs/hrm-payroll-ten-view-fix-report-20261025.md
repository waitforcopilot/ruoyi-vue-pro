# 十业务视图补齐与联合验收修复报告

日期：2026-10-25，Asia/Shanghai。依据[固定13项计划](hrm-payroll-ten-view-fix-plan-20261024.md)，V00～V12共13项技术验收已关闭；本报告及关联文档的交付检查独立记录并绑定最终manifest。结果限定PRD阶段A的TEST业务展示、示例交互与真实评审征集，不等于全PRD、阶段C真实薪酬、正式业务批准或生产部署通过。

[46候选覆盖表](hrm-payroll-ten-view-coverage-20261024.md)逐行列出具体实现、稳定锚点、证据和唯一任务归属。OV4＋PLAN4＋CALC5＋TAX5＋ATT5＋OT5＋HOUR5＋INS5＋RPT5＋COL3＝46；V00、V01、V12不重复计候选分母。原[功能完成报告](hrm-payroll-prd04-function-completion-audit-20261021.md)§4的历史展示判定与本轮业务示例验收分别保留。

## 候选、执行者与范围

最终源码候选为HEAD `d31c400fb2b9c2ab473de1d1260f00303a069e51`之上的未提交工作区，固定于[V10 repair1 manifest](/Users/waitforpro/.hrm-payroll-local/ten-view-20261025-v10-coder/candidate-repair1.json)，原字节SHA-256 `2bf1a15e941b21757fb5f34f5851d0573b9bd14f2719c3d9b4c888f20f9979b5`。冻结34文件包含未改动参考文件（如requirements/validation），不等于修改34文件；tracked差异也不包含全部新增未跟踪文件，不能用其统计推导修改总数。

编码者请求`gpt-6.1-sol / medium`，executor自动验证及原始证据复核请求`gpt-6-luna / max`；用户可见内嵌浏览器由主线程操作。实际模型均缺运行端身份核实，不以角色配置证明已运行指定模型。未执行Cursor/Grok独立审计。业务HR/主管/财务标签为TEST场景视角，实际账号和项目授权分别记录。

主链使用项目153、范围版本5、2026-10及同一虚构员工；项目155用于切换重置。受限账号268仅按其可见集合核验，受限导出另为项目138／范围版本1。现有评审账号267在V11因资格条件拒绝，改用已具有有效授权的279完成本人确认，没有为通过测试新增权限或重置口令。最终会话379恢复账号267、项目153、COL、scope5；未宣称其具有1937确认资格。

各早期任务复核绑定当时的增量候选；不能把其执行结果直接称为最终34文件候选全量重跑。V12对最终候选重新核对34文件、十视图46卡片、主链和重置，并实跑Node测试。V07 repair1仅能证明manifest中单个文件哈希变化，缺独立前版源码快照，不能宣称已作字节级补丁复算。各阶段原始候选、排除现场与复核结论均保留。

## 13项技术关闭结果

| 任务 | 实现或复用验收 | 本轮主要证据与限制 | executor结论 |
| --- | --- | --- | --- |
| V00 | 46候选映射、代码引用及卡片锚点 | 033～042；最终359～368 | 技术关闭；[CLOSE原件](/Users/waitforpro/.hrm-payroll-local/ten-view-20261024-run1/browser-review-V00-V01.json) |
| V01 | CALC前置条件、上下文隔离、四入口征集、来源摘要、失败恢复 | 004～057；1923～1926；排除008身份歧义 | 技术关闭；[CLOSE原件](/Users/waitforpro/.hrm-payroll-local/ten-view-20261024-run1/browser-review-V00-V01.json) |
| V02 PLAN | 候选方案与V1分离、调薪例外、薪档校验 | 058～098；1927；排除075、093/094中间态 | 技术关闭；[CLOSE原件](/Users/waitforpro/.hrm-payroll-local/ten-view-20261024-run1/browser-review-V02.json) |
| V03 HOUR | 三制度、周期不足/跨月未知、排班联动 | 100～123；1928v2；排除099、118加载态 | 技术关闭；[CLOSE原件](/Users/waitforpro/.hrm-payroll-local/ten-view-20261024-run1/browser-review-V03.json) |
| V04 ATT | 日月记录、关联单据补正、异常与工资影响 | 125～144；1929；保留类型失败及修复 | 技术关闭；[CLOSE原件](/Users/waitforpro/.hrm-payroll-local/ten-view-20261024-run1/browser-review-V04.json) |
| V05 OT | 8/9/8、工资750与调休互斥、跨日去重、跨期未知 | 145～182；1930；168非横滑成功原件 | 技术关闭；[CLOSE原件](/Users/waitforpro/.hrm-payroll-local/ten-view-20261024-run1/browser-review-V05.json) |
| V06 INS | 调基/城市、入离职、补缴退款、实缴未知 | 183～211；1931v2；200/202仅加载中回跳 | 技术关闭；[CLOSE原件](/Users/waitforpro/.hrm-payroll-local/ten-view-20261024-run1/browser-review-V06.json) |
| V07 CALC | 完整演示状态机、冻结重算、人工调整、部分回盘 | 213～252；1932；231旧缺陷、232/233未提交输入排除 | 技术关闭；[CLOSE原件](/Users/waitforpro/.hrm-payroll-local/ten-view-20261024-run1/browser-review-V07.json) |
| V08 TAX | 累计税、扣除未知、奖金独立、工资条更正/投递、列预览 | 253～289；1934正确TAX-03；1933旧锚点排除 | 技术关闭；[CLOSE原件](/Users/waitforpro/.hrm-payroll-local/ten-view-20261024-run1/browser-review-V08.json) |
| V09 OV | 期间/范围、准备缺口、责任下钻、受限及失败恢复 | 290～312；1935；295瞬态以311替代 | 技术关闭；[CLOSE原件](/Users/waitforpro/.hrm-payroll-local/ten-view-20261024-run1/browser-review-V09.json) |
| V10 RPT | 分摊、分母未知、预算可比性、列预览 | 313～334；1936；类型修复现场保留 | 技术关闭；[CLOSE原件](/Users/waitforpro/.hrm-payroll-local/ten-view-20261024-run1/browser-review-V10.json) |
| V11 COL | 复用清单/确认/受限导出与治理入口，无产品代码改动 | 338～358；1937v3确认、1938v2阻断；335～337瞬态排除 | 技术关闭；[CLOSE原件](/Users/waitforpro/.hrm-payroll-local/ten-view-20261024-run1/browser-review-V11.json) |
| V12 联合 | 最终候选十视图46卡片、主例工资条、项目重置、三视口历史返回 | 359～379；最终Node71/71及导出复算 | 技术关闭；[CLOSE原件](/Users/waitforpro/.hrm-payroll-local/ten-view-20261024-run1/browser-review-V12.json) |

全部十视图均有至少一条TEST意见持久化、刷新历史及返回原模块记录；不是要求46条重复意见。有效意见主线为PLAN1927、HOUR1928v2、ATT1929、OT1930、INS1931v2、CALC1932、TAX1934、OV1935、RPT1936、COL1937。V01四类入口另为1923～1926。TAX1933原错误锚点保留，不能作为正确TAX-03意见闭环。TAX当前编号固定为03工资条、04税务导出、05奖金。

## 修复内容与保留失败现场

共用演示状态增加试算前置条件及项目/模块/期间/场景隔离；未试算直接复核的旧问题已在001复现，跨模块串状态未在旧浏览器复现，应表述为防御性修复及后续重置验证。共用来源/问题摘要复用有权Workbench读取，未知信息不当零、不推测隐藏对象数。意见保存沿用真实评审API。

各业务视图增加结构化正常/异常TEST样例与相应交互：PLAN独立候选版本；HOUR三制度和跨期；ATT原始/认可记录及关联单据；OT认可、工资/调休互斥与去重；INS资格、调基和实缴对账；CALC准备/复核/冻结/部分回盘；TAX扣除、奖金、更正和虚构投递；OV缺口汇总与下钻；RPT分摊、分母与预算可比性。COL继续复用既有清单、历史、分派、本人确认、筛选导出和治理入口，本轮未修改产品代码。

V04初始`recognized`推断number后赋null导致TS2322，repair1补显式nullable类型；V05初始新组件14条Stylelint格式诊断，repair1只调格式；V07旧输入只有aria-disabled缺陷及未提交输入尝试保留，修复后236～237实际提交100并与工资条一致，238原生键盘输入200使旧试算失效；V08初始TAX编号/锚点错配，repair1改为03工资条、04导出、05奖金并新建正确1934意见；V10初始返回行类型导致TS2339，repair1补显式返回接口。原失败日志和中间画面均不删除，也不计为最终成功证据。

## 联合对账、角色和输出

固定主例：基本10000＋绩效1000＋加班750＝应发11750；个人缴费1400、TEST税130.50、实发10219.50；单位缴费2100、成本13850。排班160＝出勤152＋带薪假8，加班8单列；成本70/30分摊9695/4155；预算14000差额-150。候选调薪、奖金、跨期、补缴及CALC人工调整有独立场景键，不并入当期基线。369～370重新准备、试算、复核及同人同期间工资条预览对账；初次未达到准备完成的尝试不计通过。371～372项目切换重置及其87条完整GET/OPTIONS网络窗口证明本次观察内无写方法。

359～368按PLAN/HOUR/ATT/OT/INS/CALC/TAX/OV/RPT/COL顺序回读最终候选十页，每页至少一项样例，合计46唯一候选编号及锚点。三视口意见完整保存链可组合V02的390、V03的768及V04的1440原件；373～378另复核390/768/1440历史与返回。390有焦点记录，768/1440焦点JSON为空，只认URL保留COL-02锚点与截图附近位置，不能称三视口均有焦点证明。每页390关键操作和宽表可达见各任务复核。

V11中1937由编辑者266创建、分派既有279，279本人确认后重载为v3、scope5；1938v2缺必填字段仍PENDING、无确认人/时刻，确认被阻断。267因授权根撤销相关资格失败属于正确拒绝，不能为其扩权获得PASS。V01覆盖保存失败输入保留→恢复只成功一次；V09覆盖临时安全URL阻断读取失败→恢复；无权、真实空、未知、加载中与接口失败保持不同含义。

当前筛选`TEST TENVIEW`于项目153/scope5两页10＋4行，实际下载14行；页面、下载JSON与复算的ID/version集合一致。随后新增1938后的最终导出为15行，是不同时点快照，不能拿15替代14行对账。受限项目138/scope1只导出1502/v8，有permissionRestricted且无projectTotals，不能推测全项目总数。治理入口已复验，但V11未执行新发布；历史固定六表快照与当前筛选导出分别验收。

复算原件：[automation-V12/export-reconciliation.json](/Users/waitforpro/.hrm-payroll-local/ten-view-20261024-run1/automation-V12/export-reconciliation.json)；[V11-filter-export-reconciliation.json](/Users/waitforpro/.hrm-payroll-local/ten-view-20261024-run1/V11-filter-export-reconciliation.json)；[V11-filtered-export-with-col.json](/Users/waitforpro/.hrm-payroll-local/ten-view-20261024-run1/V11-filtered-export-with-col.json)；[V11-final-export.json](/Users/waitforpro/.hrm-payroll-local/ten-view-20261024-run1/V11-final-export.json)；[V11-restricted-export.json](/Users/waitforpro/.hrm-payroll-local/ten-view-20261024-run1/V11-restricted-export.json)。

## 自动验证及证据限制

实际工具链使用Node v22.23.3与私有pnpm 10.30.0，不把环境默认Node24/pnpm11当作本轮运行版本。V12最终候选实跑`node --test`共71项，71通过、0失败、退出0；34文件前后哈希匹配。V10 repair1的ESLint、Prettier、差异/空白检查、ts:check及build:local通过；其类型修正阶段未重跑logic/style，最终Node71项另由V12实跑。V08 repair1文案/锚点修正仅运行目标lint/format/style/diff，未重跑tests/type/build，不能回写成当时全部重跑。

原index.vue Stylelint有既有34条诊断，V01已与baseline核对相同；本轮新增组件Stylelint按相应任务检查通过，不称全仓Stylelint零诊断。构建输出及typecheck结果依各任务原始日志，文档阶段没有重跑业务测试。

原件：[automation-V12/node-tests-summary.json](/Users/waitforpro/.hrm-payroll-local/ten-view-20261024-run1/automation-V12/node-tests-summary.json)、[automation-V12/node-tests.log](/Users/waitforpro/.hrm-payroll-local/ten-view-20261024-run1/automation-V12/node-tests.log)、[automation-V12/identity-comparison.json](/Users/waitforpro/.hrm-payroll-local/ten-view-20261024-run1/automation-V12/identity-comparison.json)、[automation-V12/topic-static-audit.json](/Users/waitforpro/.hrm-payroll-local/ten-view-20261024-run1/automation-V12/topic-static-audit.json)、[automation-V10-repair1/command-results.json](/Users/waitforpro/.hrm-payroll-local/ten-view-20261024-run1/automation-V10-repair1/command-results.json)、[V12-final-ten-view-matrix.json](/Users/waitforpro/.hrm-payroll-local/ten-view-20261024-run1/V12-final-ten-view-matrix.json)。

最终十模块网络捕获均为完整窗口（hasMore=false），观察方法GET/OPTIONS；项目重置87条亦完整。较早任务及V12响应式历史窗口存在截断，后者79条，不推导全会话无业务写入。真实意见保存、分派与本人确认是明确允许的评审写入；模拟工资、税务、银行回盘与投递仍为TEST演示，没有据此宣称真实工资处理或外发成功。

控制台警告非零。V11最终控制台为有界57条warning快照，前序亦有Vue Router、Element Plus及submenu indexPath提示；不报告“零警告／全会话无错误”。即时回跳scope尚未加载、焦点缺失及网络截断按各review限制保留，文件名、一次点击、相同DOM不能替代稳定回读。

## 引用完整性与交付边界

原始AX、截图、网络、日志与候选保留私有证据目录，使用绝对文件链接，不复制大截图入仓库。下表为本报告引用的各阶段executor复核原文件SHA-256；技术CLOSE与本次文档交付复核分别记录。

| 复核原件 | SHA-256 |
| --- | --- |
| [browser-review-V00-V01.json](/Users/waitforpro/.hrm-payroll-local/ten-view-20261024-run1/browser-review-V00-V01.json) | `2489ac5eb22adbe97daa2f7e99e20473d5d7e6758fea9997591f2b2754e48a73` |
| [browser-review-V02.json](/Users/waitforpro/.hrm-payroll-local/ten-view-20261024-run1/browser-review-V02.json) | `1c78c2c5dd5379ea86cdc2387c6adf8ba3992cfb34f663b5c8ee47c70b5b78fa` |
| [browser-review-V03.json](/Users/waitforpro/.hrm-payroll-local/ten-view-20261024-run1/browser-review-V03.json) | `d9432ec8e716a66a99c094bae1f4467e56ca2a1766abd377b1977249bc48176d` |
| [browser-review-V04.json](/Users/waitforpro/.hrm-payroll-local/ten-view-20261024-run1/browser-review-V04.json) | `e68bbfb04a4c8b0568918d27e07bdbb40edc1f759515a59468e0f86df7b247a2` |
| [browser-review-V05.json](/Users/waitforpro/.hrm-payroll-local/ten-view-20261024-run1/browser-review-V05.json) | `07b1591d19799f3695115e459dc66efd7ead244d02245e6afeda92813a23cfb7` |
| [browser-review-V06.json](/Users/waitforpro/.hrm-payroll-local/ten-view-20261024-run1/browser-review-V06.json) | `387141658af212a50120c3cdc08a032c5f492543c9e4203b5d6615f53f1572a2` |
| [browser-review-V07.json](/Users/waitforpro/.hrm-payroll-local/ten-view-20261024-run1/browser-review-V07.json) | `77ad75b81b617203d2828b903817f5b273c18088b918a86c67a6b8d1cf9af07f` |
| [browser-review-V08.json](/Users/waitforpro/.hrm-payroll-local/ten-view-20261024-run1/browser-review-V08.json) | `9c4dc4c2c2f5ba095e80e685156fb2056731260ebfd73272fe3fa19e2f48f428` |
| [browser-review-V09.json](/Users/waitforpro/.hrm-payroll-local/ten-view-20261024-run1/browser-review-V09.json) | `483837385c99ae38ce72630d37c805e8e71967a03fb6287025bad1c609095066` |
| [browser-review-V10.json](/Users/waitforpro/.hrm-payroll-local/ten-view-20261024-run1/browser-review-V10.json) | `8620f75a113877ed6b3bb480ce2d03ae97ca5822a234f1a59576d2c46eb6f3e4` |
| [browser-review-V11.json](/Users/waitforpro/.hrm-payroll-local/ten-view-20261024-run1/browser-review-V11.json) | `c2644ea6f764bcf182a568ff77c7c942bc89da672e3d243d1d8a6884ab93429f` |
| [browser-review-V12.json](/Users/waitforpro/.hrm-payroll-local/ten-view-20261024-run1/browser-review-V12.json) | `8ef4ee136898fc2762b1322bb0471d68205d6ea5890b40cb937c527c0303b18f` |

本次文档更新仅四文件：本报告、46候选coverage、13任务plan状态、原function-completion报告§4当前说明及链接。保留原报告其余历史主体、§18、既有修改及原固定11WB报告，不修改PRD、业务源码、数据库、浏览器、进度文件或旧证据，不提交/push。最终四文档哈希另存私有文档候选manifest，交付检查独立记录并绑定最终manifest。

剩余范围包括真实员工工资条、正式规则和业务批准、真实税务申报、付款/实缴、生产接入、全PRD/全部AC、性能负载及完整跨租户/并发矩阵；这些不属于本13项技术分母，不因TEST演示通过而关闭。Cursor独立审计尚未执行，本报告不自称独立审计结论。
