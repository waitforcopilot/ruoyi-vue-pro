# T03 账号与项目授权资格只读核查

日期：2026-10-22。数据库：现有本机 Docker context `colima-hrm-local` 中的 `hrm_local_20261017`。全部查询用 `START TRANSACTION READ ONLY`；没有改账号、角色、菜单、授权根、grant、项目或业务数据。输出仅包含 TEST 用户/角色 ID、状态布尔值与权限布尔值，不包含用户名、口令、token、授权材料正文或签署正文。

请求执行模型：`gpt-6-luna / max`；无运行端身份回执，实际模型未核实。

## 权限字串来源

源码 `HrmPayrollWorkbenchService.platform` 使用平台菜单权限 `hrm:payroll:requirement:<suffix>`，并把 `ISSUE_MANAGE`、`ISSUE_HANDLE`、`ISSUE_REVIEW` 映射为对应小写连字符后缀；`HrmPayrollRequirementAccessService.ACTIONS` 定义了大写项目授权动作。有效平台权限按启用且未删除的用户—角色、角色—菜单关联，以及启用且未删除的角色和菜单计算。

## 平台权限

`query`、`issue-manage`、`issue-handle`、`issue-review` 列分别对应源码权限字串 `hrm:payroll:requirement:query`、`hrm:payroll:requirement:issue-manage`、`hrm:payroll:requirement:issue-handle`、`hrm:payroll:requirement:issue-review`。所有账号与角色均启用，且未删除。

| 用户ID | 角色ID | query | issue-manage | issue-handle | issue-review |
|---:|---:|---:|---:|---:|---:|
| 266 | 180 | 是 | 是 | 是 | 否 |
| 267 | 181 | 是 | 否 | 否 | 是 |
| 269 | 180 | 是 | 是 | 是 | 否 |
| 279 | 181 | 是 | 否 | 否 | 是 |

## 项目153现有根与grant

授权根动作和项目 grant 动作分开记录。根记录均为 v2 ACTIVE；对应 grant 当前未撤销、未删除且在当前日期有效。

| 根ID | 被授权用户 | 根 ISSUE_HANDLE | 根 ISSUE_REVIEW | grant ID | grant ISSUE_HANDLE | grant ISSUE_REVIEW |
|---:|---:|---:|---:|---:|---:|---:|
| 1392 | 266 | 是 | 否 | 522 | 是 | 否 |
| 1416 | 279 | 否 | 否 | 525 | 否 | 否 |

## 对T03夹具的含义

平台角色可分别提供处理权限（用户266或269，角色180）和复核权限（用户267或279，角色181）。现有项目授权只给266处理动作；279的1416确认根不含问题处理或复核动作，267也没有当前项目 ISSUE_REVIEW grant。因此，现有角色无需扩大或修改，但完整的项目问题生命周期仍需要通过授权的可见TEST流程准备合适、互相区分的handler与reviewer项目授权，并按T03计划建立/分派问题。此报告不执行该夹具操作，也不替代后续页面证据。

## 查询证据与限制

使用本机 MySQL 容器的默认 socket/client 配置执行，只读查询针对 `system_users`、`system_user_role`、`system_role`、`system_role_menu`、`system_menu`、`hrm_payroll_governance_record` 和 `hrm_payroll_requirement_project_grant`；SQL只返回上表字段。权限布尔值代表当前数据库关联状态，不是通过前端登录后的运行时API验证。根/授权的正式业务效力未由本项查询判定。
