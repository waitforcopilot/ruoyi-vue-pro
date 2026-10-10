# HRM 运行环境与验证

## 数据库安装

全新测试库先执行 `sql/mysql/ruoyi-vue-pro.sql`。该上游脚本包含删除旧表操作，不能用于已有业务数据库。

已有 yudao 数据库按顺序执行 `sql/mysql/hrm.sql`、`sql/mysql/bpm.sql`、`sql/mysql/oa.sql`，再执行已交付模块的增量脚本：

- `sql/mysql/upgrade/20261010-hrm-requirements.sql`
- `sql/mysql/upgrade/20261010-hrm-batch.sql`
- `sql/mysql/upgrade/20261010-hrm-payment.sql`

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
