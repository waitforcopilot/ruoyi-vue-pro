# 第十四模块：薪酬批次概览与真实待办

2026-10-08。基于已通过回归的试算、复核与冻结模块，补齐 T-42 的首期概览。统计口径由用户授权的行业惯例补充 Q-23/Q-24 给出，正式来源及法定成本的完整覆盖继续按路线图实施。

## 结果与统计口径

按主体编号、批次名称、状态和完整起止期间查询历史批次，展示草稿、已试算、复核中、复核通过、已冻结的真实数量。数量来自完整查询结果，独立于分页。期间采用完整日期匹配，不把自然月强加给自定义期间。

当前登录人的 HR/财务任务由 Flowable 原生待办查询，关联同租户、运行中复核轮次和批次的当前 runId/activeReviewId 后，再应用全历史/当前人员范围。旧流程、错误版本、其他租户和不可见批次不计入待办；批次的实际阶段可显示待 HR、待财务或需同步流程状态。GET 不自动批准或修复状态，办理入口跳到原批次页面，由既有权限、职责分离和来源核验办理。

金额仅取所选单个保存版本的应发、扣款、个税与实发十进制字符串。不对可能重叠的批次求和；未生成试算显示缺失，旧试算失效和来源变化明确提示并保留原金额作参考。来源是否变化复用复核模块的稳定业务指纹，不因审批状态或修订号变化误报。

同时读取当前人员、资格、规则及已登记输入的核验，展示可计薪、明确排除、阻断和具体待核对内容。输入就绪不推定考勤、加班、工时、缴费与申报已自动归集；单位缴费尚未绑定时法定成本显示未生成。试算、冻结、付款及完税状态各自保留业务含义。

## 权限、接口和页面

新增 `hrm:payroll:overview:query`，必须同时具有试算、计算规则、人员资格和 HRM 人员四项查询权限。控制器及服务均验证组合权限。批次数量、状态、待办、列表及金额采用同一整批范围门禁：全部曾涉及人员的历史组织及当前组织都须可见，即使后来移出名单或明确排除也不能泄露；租户独立。

接口前缀 `/admin-api/hrm/payroll/overview`：`GET /page` 读取状态、真实待办和分页批次；`GET /batch?id=` 读取所选版本及当前资料核验。关闭接口请求/响应体访问日志，不新增工资业务表或写入既有工资、缴费、工资条和银行资料。

前端 [`overview/index.vue`](../yudao-ui/yudao-ui-admin-vue3/src/views/hrm/payroll/overview/index.vue)，页面 `/hrm/payroll-overview`。切换查询或批次时丢弃旧响应；金额与异常入口携带 batchId 跳到 `/hrm/payroll-trial-batches` 并自动定位批次。全量 Vue3 基线与 MES 继续参与构建和类型检查。

应用 [`20261008-hrm-payroll-overview.sql`](../sql/mysql/upgrade/20261008-hrm-payroll-overview.sql)登记幂等菜单，管理员明确配置业务角色的查询权限；SQL 不自动给任何业务角色授权。前置试算/复核 SQL 与流程模型部署步骤见各模块说明。

## 验证

- [全量后端报告](./assets/hrm-payroll-overview/hrm-overview-backend-tests.json)：1,761 项报告，零失败/错误，24 项既有跳过；HRM 886 项全部执行，BPM 63 项（6 项既有跳过）。新增 26 项均执行，覆盖五项组合权限、数量与分页/筛选、参数绑定、版本金额、来源变化、GET 无写入、全历史/当前组织和租户、真实待办关联与失步状态。
- 完整 Vue3 基线 `0af03a93b6b6300f878e28add69b1c7a9f09ec34` + overlay 的生产构建与全量类型检查通过，MES 保留。
- MySQL 8 重复应用概览菜单迁移通过：唯一菜单、保留既有角色授权、不创建工资数据表。
- [实际概览 API 报告](./assets/hrm-payroll-overview/hrm-overview-api-results.json)：30 项通过；先完整复跑[前置复核模块 55 项 API/BPM 检查](./assets/hrm-payroll-overview/prior-review-api-results.json)。7 张既有工资、工资条、社保和银行资料表的校验值保持一致。
- [浏览器报告](./assets/hrm-payroll-overview/hrm-overview-browser-results.json)：11 项通过，包含真实 HR/财务账号、保存版本及缺失/失效金额、待办定位与直接刷新、旧响应拦截和移动端。9 张原始截图及 SHA-256 见[验证清单](./assets/hrm-payroll-overview/verification-manifest.json)。
- [概览](./assets/hrm-payroll-overview/payroll-overview-states.png)、[所选版本金额与资料核验](./assets/hrm-payroll-overview/payroll-overview-version-money.png)、[移动端金额](./assets/hrm-payroll-overview/payroll-overview-mobile-money.png)。合成资料与实际页面截图，未修改像素。
- [前置 PR #15 的 GitHub CI 证据](./assets/hrm-payroll-overview/prior-review-github-ci-evidence.json)绑定 `371d91f3d78828bb64f2cc9964b6379730626ab8`，三个 job 成功，下载的前后端产物校验及 53 项新增测试核验通过。本模块的远端 CI 另记其 PR，不能用前置结果代替。

复跑：`bash script/hrm/verify-overview-mysql.sh`。明确允许的隔离 `hrm_payroll_collection_*` 数据库应用前置复核合成人员 fixture 和本模块 `overview-role-fixtures.sql` 后，按环境令牌运行 `verify-overview-api.py --allow-test-fixtures --output-dir <目录>`；此脚本先复跑完整复核链路，再产生概览验证数据及浏览器 fixture。随后执行 `verify-overview-ui.py --allow-test-fixtures --fixture-file <fixture> --output-dir <目录>`。服务仅允许 loopback，连接配置、令牌和日志保留在 Git 外。

## 后续范围

本轮完成的是 T-42 的批次概览首期，仍属部分实现。方案/项目与工资输入绑定、正式来源自动归集、单位缴费成本、工时/加班结果、累计税申报、异常工单和付款/工资条发布不由本概览生成。不得用当前试算就绪或冻结状态替代这些业务状态。
