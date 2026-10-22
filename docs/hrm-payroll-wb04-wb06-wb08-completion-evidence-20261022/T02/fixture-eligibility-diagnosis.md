# T02 fixture eligibility diagnosis

Date: 2026-10-22

This is a read-only local database diagnosis. Queries ran against the existing `hrm_local_20261017` database through Docker context `colima-hrm-local`, using the local MySQL container's socket/default client configuration and read-only transactions. No credentials, passwords, or signature body were read or included. No database, account, role, permission, project, grant, browser, or service state was changed. Actual runtime model identity was not independently verified.

## Finding for user 269

User `269` is enabled (`status=0`), not deleted, tenant `1`. The user's only role is role `180`, `hrm_s5_editor_20261019`, enabled (`status=0`) and not deleted. The active `hrm:payroll:requirement:query` menu is `13479` and is bound to role `180`. The active `hrm:payroll:requirement:confirm` menu is `13562`, but role `180` has no binding to it.

The project-side authorization chain exists and is active: root `1412` is version `2`, `ACTIVE`, mode `TEST`, for project `153` and user `269`; it grants `READ` and `CONFIRM`, is signed by `267`, and references anchor `4`. Its scope is limited to entity `101`, organization `201`, worker type `TEST`, scenario `TEST`, and 2026-10-01 through 2026-10-31. The linked project grant is `524`, for project `153` / user `269`, with `READ,CONFIRM`, `full_project=true`, not deleted, and not revoked.

The code explains the empty eligible-reviewer list: [`HrmPayrollRequirementAccessService.java`](/Users/waitforpro/source/github-yudao/ruoyi-vue-pro/yudao-module-hrm/src/main/java/cn/iocoder/yudao/module/hrm/service/payroll/HrmPayrollRequirementAccessService.java:108) requires an active account, project `READ` and `CONFIRM`, plus both platform permissions `hrm:payroll:requirement:query` and `hrm:payroll:requirement:confirm`. [`HrmPayrollGovernanceRootService.java`](/Users/waitforpro/source/github-yudao/ruoyi-vue-pro/yudao-module-hrm/src/main/java/cn/iocoder/yudao/module/hrm/service/payroll/HrmPayrollGovernanceRootService.java:30) applies the same query-plus-action platform permission rule. Thus the observed blocker is specifically the missing role-to-menu binding for user 269; the account, role, and project grant are enabled.

## Candidate comparison

| User | Username | Role (enabled) | Effective query | Effective confirm |
|---:|---|---|---:|---:|
| 251 | `hrmacceptoperator` | 170 `hrmacceptoperator` | yes | no |
| 270–278 | `hrms6worker1`–`hrms6worker9` | 180 `hrm_s5_editor_20261019` | yes | no |
| 279 | `hrms6siga82c7d` | 181 `hrm_s5_approver_20261019` | yes | yes |

All listed accounts were enabled (`status=0`), not deleted, and tenant `1`; all listed roles were enabled and not deleted. Among the requested candidates (251 and 270–279), user `279` is the only account with both effective platform permissions. No candidate had confirm without query.

## Minimal continuation

Do not add `confirm` to role `180`, because that role is shared by users `270`–`278`. If the UI workflow continues, use account `279` as the successor reviewer and create a narrowly scoped `TEST` authorization root for project `153`, with the independent registration/signing steps required by the UI. No such root or assignment was created during this diagnosis. Root `1412` remains the existing 269-specific fixture as observed; this report does not cancel or replace it.
