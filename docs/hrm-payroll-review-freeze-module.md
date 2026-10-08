# 第十三模块：试算版本复核与冻结

2026-10-08。依赖第十二模块的不可变试算版本。本模块覆盖 T-40 的首期版本复核、审批和冻结链路，并补充 T-39/T-41 的快照回放、来源变化检查及依据留存。行业推定值见 [PRD 补充](./hrm-payroll-industry-assumptions.md) Q-20～Q-22。

## 行为与版本边界

已试算批次明确选择 HR 和财务两名启用的租户内账号，依次执行两级复核。两人不同，均不能是本次核算人或发起人；需具备试算、计算规则、人员资格和 HRM 人员四项查询权限，以及对应复核权限。校验其整个批次曾包含的人员快照及当前人员范围，避免经汇总或历史查看绕过部门/本人权限。

提交绑定当前 runId、来源指纹和经过校验的实际 BPM processDefinitionId。流程使用 Yudao BPM 的部署、待办、审批和终态事件，金额与完整业务依据保留在受权限保护的 HRM 记录中，BPM 变量仅包含控制编号和指纹。通用 BPM 直接发起、审批、驳回、转办、抄送和撤销等操作需通过业务保护，不能绕过批次权限及版本核验。未注册的其他流程保持默认行为。

批准与冻结均再次锁定并核验规则、资格和人员来源；资料变化须重新核对并试算。复核中、复核通过和冻结状态均禁止编辑或重新试算。驳回、发起人撤销及有独立权限的管理撤销使当前试算失效，旧试算和本轮依据继续保留。冻结独立授权且不能由核算人或发起人办理；带依据解冻独立授权，解除当前批准资格，须重新复核。金额与计算解释从不被审批或解冻改写。

| 批次状态 | 允许的业务推进 |
| --- | --- |
| 0 草稿 | 核验、维护、试算；此前的有效试算已失效时须重新试算 |
| 1 已试算 | 核验、维护/生成新版本、提交新一轮复核 |
| 2 复核中 | 指定本级复核人通过/驳回，发起人或管理人员凭依据撤销 |
| 3 复核通过 | 独立权限人员核验后冻结；来源变化则拦截冻结 |
| 4 已冻结 | 查看固定版本，凭独立权限与依据解冻 |

复核轮次独立记录复核中、通过、驳回、撤销、冻结和解冻；一份试算可在解冻后产生新的复核轮次。每次操作使用租户、批次、请求编号、操作人和完整请求指纹保存不可变回执，相同请求重试返回原回执，改变请求体或操作人则拒绝。终态校验实际 BPM 状态、流程 ID、轮次及当前试算，旧事件和重复事件不推进新结果。

BPM 终态事件可能早于引擎历史记录刷新，业务操作在引擎命令返回后再次核验实际终态；另有显式同步操作用于故障恢复。GET 只读取，不隐式修改状态。首期通过 BPM 待办和批次页面办理；薪酬流程关闭通用外部短信，避免可选手机号或短信服务阻断财务复核。其他流程保留原短信默认行为，回归使用合成人员，不向实际人员投递。

## 数据和接口

增量 SQL：[20261007-hrm-payroll-review-freeze.sql](../sql/mysql/upgrade/20261007-hrm-payroll-review-freeze.sql)。新建 `hrm_payroll_review_cycle` 保存轮次、绑定版本与各环节依据，`hrm_payroll_review_command` 保存幂等回执；试算批次增加 `active_review_id` 和 `frozen_run_id`。租户内轮次、流程、请求编号分别唯一，重复迁移保留既有数据。ISO 格式的薪酬快照时间使用对应字符串解析器，避免历史回放误读为 1970 年。

新增六项动作权限：`review-submit`、`hr-review`、`finance-review`、`freeze`、`unfreeze`、`admin-cancel`，前缀均为 `hrm:payroll:trial:`。迁移登记菜单权限，不自动给业务角色授权；各动作与四项查询权限、资料范围组合检查。

| 接口（前缀 `/admin-api/hrm/payroll/trial-batches/review`） | 用途 |
| --- | --- |
| `GET /get?batchId=` | 读取当前复核、真实待办及本批次历史轮次 |
| `POST /action` | 明确批次/runId/修订/轮次/请求编号/依据，执行指定动作 |
| `POST /sync?batchId=` | 核对服务器实际 BPM 终态并幂等同步业务状态 |

前端位于 `yudao-ui/yudao-ui-admin-vue3/src/views/hrm/payroll/trial/ReviewPanel.vue`，集成在试算批次页面 `/hrm/payroll-trial-batches`。动作按登录人、指定任务、权限和批次状态展示；响应丢失保留同一个请求体和编号重试，切换批次不接受旧请求覆盖当前页面。复核接口关闭请求/响应体访问日志。

## 部署和复跑

1. 应用本模块增量 SQL；前置数据库须已具备试算、规则和人员资格模块。
2. 在既有 BPM 流程模型管理中部署 [`payroll-review.bpmn20.xml`](../yudao-module-hrm/src/main/resources/bpmn/payroll-review.bpmn20.xml)。流程标识为 `hrm_payroll_trial_review`，BPMN 类型 10、自定义表单类型 20、页面路径 `/hrm/payroll-trial-batches`；关闭自动审批去重及任务撤回，允许发起人撤销。明确登记流程管理者，流程不在通用发起列表显示。服务会拒绝缺少财务环节、自动通过等不符合首期模型的部署。
3. 为实际业务角色配置六项动作权限及四项资料查询权限；提交时明确选择 HR/财务人员，不把 QA 编号复制为实际审批人员。
4. 使用 `-Phrm` 构建服务。完整 Vue3 前端按固定基线和仓库 overlay 构建，配置数据库、Redis 和 API 地址后启动。

数据库迁移复跑：`bash script/hrm/verify-review-mysql.sh`，使用无外网的临时 MySQL 8 容器检查重复迁移、保留数据、唯一约束和中文证据。

真实 BPM/API 复跑：在明确允许的隔离 `hrm_payroll_collection_*` QA 数据库应用 [`review-employee-fixtures.sql`](../script/hrm/review-employee-fixtures.sql)，配置验证账号令牌为环境变量，执行 `python3 script/hrm/verify-review-api.py --allow-test-fixtures --output-dir <目录>`。脚本仅允许 loopback 验证服务，在隔离库通过既有模型 API 部署合成流程，提供浏览器验证 fixture。随后按环境令牌执行 `verify-review-ui.py --allow-test-fixtures --fixture-file <fixture> --output-dir <目录>`。令牌、运行连接配置和本地服务日志不进入 Git。

## 验证证据

- [全量后端报告](./assets/hrm-payroll-review/hrm-review-backend-tests.json)：1,735 项报告，0 失败、0 错误；HRM 860 项全部执行、无跳过；BPM 63 项，包含既有 6 项跳过。整个 reactor 的 24 项跳过均为已有测试。本模块新增 53 项测试全部执行。
- 完整固定 Vue3 基线 `0af03a93b6b6300f878e28add69b1c7a9f09ec34`：生产构建及全量 `vue-tsc --noEmit` 通过，包含 MES。
- MySQL 8：重复迁移、六项动作权限、原试算保留、租户/轮次/流程/请求唯一和中文证据检查通过。
- [真实 API/BPM 报告](./assets/hrm-payroll-review/hrm-review-api-results.json)：55 项通过，覆盖两级审批、真实引擎终态、通用接口绕过拦截、资料变化、撤销/驳回重算、冻结/解冻、并发重试、跨租户和部门/本人范围。7 张既有工资、工资条、社保和银行卡表检查值保持一致。
- [上一模块试算 API 回归](./assets/hrm-payroll-review/prior-trial-api-results.json)：52 项在本模块服务上通过。
- [浏览器报告](./assets/hrm-payroll-review/hrm-review-browser-results.json)：13 项通过，包含响应丢失后同一请求重试、独立登录人权限、两级推进、冻结/解冻、撤销、移动布局与延迟的旧批次响应。10 张原始截图及 SHA-256 记录见[验证清单](./assets/hrm-payroll-review/verification-manifest.json)。
- [冻结页面](./assets/hrm-payroll-review/payroll-review-frozen.png)、[移动端冻结](./assets/hrm-payroll-review/payroll-review-mobile-frozen.png)、[解冻后的历史依据](./assets/hrm-payroll-review/payroll-review-unfrozen.png)。截图为实际页面及合成资料，未修改像素。
- [前置试算模块的 GitHub CI 证据](./assets/hrm-payroll-review/prior-trial-github-ci-evidence.json)对应 PR #14 的 `6e42f55b42f97ba2a97f52f2474d08c68d7b466a`，三个 job 成功且下载的前后端产物已核验；本模块提交的远端 CI 结果另记在其 PR 中，不能用前置结果代替。

技术验证使用合成工资输入，不代表企业工资已签认或发放。生产构建、类型检查和回归覆盖完整现有架构，未删除 MES 或隐藏既有检查。

后续已补存 [PR #15 的真实 GitHub CI 证据](./assets/hrm-payroll-overview/prior-review-github-ci-evidence.json)：提交 `371d91f3d78828bb64f2cc9964b6379730626ab8` 的三个 job 全部通过，前后端产物下载并校验，新增 53 项测试全部执行。证据记录本模块已完成的远端验证，不代表后续概览提交的 CI。

## 尚未包含的业务能力

首期固定 HR→财务两级；增加层级、特定审批策略和新的通知渠道需另行配置与版本化。正式来源的自动归集、特殊追溯/负数调整、累计税计算、银行付款、员工工资条发布与完整试点验收继续按路线图推进。本模块冻结的是保存的试算版本，不表示已扣税、已付款或已对员工发布。
