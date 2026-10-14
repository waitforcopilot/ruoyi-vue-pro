# HRM 运行环境与验证

## 数据库安装

全新测试库先执行 `sql/mysql/ruoyi-vue-pro.sql`。该上游脚本包含删除旧表操作，不能用于已有业务数据库。

已有 yudao 数据库按顺序执行 `sql/mysql/hrm.sql`、`sql/mysql/bpm.sql`、`sql/mysql/oa.sql`，再执行已交付模块的增量脚本：

必须先执行需求脚本创建薪酬父菜单，然后按以下顺序安装已交付功能（同日脚本也有依赖，不能直接按文件名字母排序）：

- `sql/mysql/upgrade/20261010-hrm-requirements.sql`
- `sql/mysql/upgrade/20261010-hrm-batch.sql`
- `sql/mysql/upgrade/20261010-hrm-payment.sql`
- `sql/mysql/upgrade/20261010-hrm-slip-publication.sql`
- `sql/mysql/upgrade/20261010-hrm-formula.sql`
- `sql/mysql/upgrade/20261010-hrm-salary-proposal.sql`
- `sql/mysql/upgrade/20261010-hrm-work.sql`
- `sql/mysql/upgrade/20261011-hrm-grade.sql`
- `sql/mysql/upgrade/20261011-hrm-attendance.sql`
- `sql/mysql/upgrade/20261011-hrm-entity.sql`
- `sql/mysql/upgrade/20261011-hrm-batch-scope.sql`
- `sql/mysql/upgrade/20261012-hrm-insurance-policy.sql`
- `sql/mysql/upgrade/20261012-hrm-insurance-bill.sql`
- `sql/mysql/upgrade/20261013-hrm-input.sql`
- `sql/mysql/upgrade/20261013-hrm-source-claim.sql`
- `sql/mysql/upgrade/20261013-hrm-time-off-leave.sql`
- `sql/mysql/upgrade/20261013-hrm-work-entity.sql`
- `sql/mysql/upgrade/20261013-hrm-no-overtime.sql`
- `sql/mysql/upgrade/20261013-hrm-work-version.sql`
- `sql/mysql/upgrade/20261013-hrm-work-variance.sql`
- `sql/mysql/upgrade/20261013-hrm-tax-policy.sql`
- `sql/mysql/upgrade/20261013-hrm-tax-profile.sql`
- `sql/mysql/upgrade/20261013-hrm-bank-value-date.sql`
- `sql/mysql/upgrade/20261014-hrm-tax-deduction.sql`
- `sql/mysql/upgrade/20261014-hrm-payment-plan.sql`

安装扣除资料迁移后，对每个使用薪酬的租户执行现有 `PUT /hrm/salary-option/sync`（`hrm:salary:option:update`）同步新增 `260106` 婴幼儿照护目录项；保留原启用/展示设置。目录项缺失或归属错误时主体工资核算会阻断，不静默忽略非零扣除。旧已复核的五项扣除输入需按六项模板明确补齐并重新复核；不能自行给缺项补零。

三个基础模块脚本仅创建缺失业务表。Flowable 使用项目现有的自动升级配置创建流程引擎表。后续模块的增量脚本应随相应功能交付安装，不能仅凭文件存在判断已验收。

## 启动

服务器使用项目 Maven 构建 `yudao-server` 及其依赖。配置 MySQL 主、从数据源及 Redis，运行时设置 `-Duser.timezone=Asia/Shanghai`。隔离测试环境关闭 Quartz 自动启动、关闭模拟登录，避免启动业务定时任务。

Vue3 目录安装锁定依赖后运行 `pnpm dev`，使用 development 模式；`.env.development` 默认连接本机后端。需要跨机器访问时按部署环境配置后端地址及代理，不使用测试库的账号配置作为生产配置。

## 可重复检查

数据库实体字段检查：

```bash
python3 scripts/hrm/check_schema.py --container <测试MySQL容器> --database <测试库>
```

本机接口检查（测试账号密码从环境变量 `HRM_SMOKE_PASSWORD` 读取）：

```bash
python3 scripts/hrm/smoke_api.py
```

仅在隔离测试库增加 `--initialize` 检查需求清单初始化的幂等性。脚本限制访问本机，不输出访问令牌。

## 本次实际运行结果

在隔离 MySQL 8、Redis 7 环境启动服务器及 Flowable。针对已交付代码检查 113 张业务表、1,189 个实体字段，无缺失列；BPM/OA 建表脚本重复执行通过。

真实接口验证登录与权限信息、需求查询/初始化/Excel 导出、核算批次分页、代发批次与银行模板查询。未登录访问返回 401，错误租户访问返回 403。

Chromium 浏览器完成登录并打开需求评审、核算审批、银行代发三个页面，无未捕获 JavaScript 异常。此结果覆盖已交付模块的运行基础；其余 PRD 功能仍需按计划开发及业务验收。

工时差异升级同时创建站内信模板及默认停用的 `hrmPayrollWorkVarianceReminderJob`。生产启用前检查责任人/复核人主体授权和站内信配置；此轮只验证模拟发送、每日防重及失败重试，没有向真实人员发送提醒。工时差异台账与事件表包含来源证明，沿用工时查询权限及主体范围校验。

税务身份档案配置 `yudao.hrm.tax.identity-encryption-key` 与 `yudao.hrm.tax.identity-fingerprint-key`，或对应环境变量 `YUDAO_HRM_TAX_IDENTITY_ENCRYPTION_KEY`、`YUDAO_HRM_TAX_IDENTITY_FINGERPRINT_KEY`。二者均为独立生成的32字节随机密钥的Base64表示，由部署密钥管理提供，不写入Git；未配置时拒绝保存/核实敏感身份。加密密钥用于AES-GCM，指纹密钥用于跨记录身份匹配，不能随部署随机重置。旧数据迁移及密钥更换需受控重新加密/匹配迁移，不通过覆盖配置丢弃原数据。单元测试使用明确的测试专用密钥和虚构标识；普通JSON与审计快照不返回明文标识。
