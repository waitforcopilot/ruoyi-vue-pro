# HRM/BPM 启动基础

这是薪酬开发路线的第一批基础实现：`hrm` Maven profile、50 张 HRM 与 8 张 BPM 应用表的 MySQL 增量建表，以及迁移回归验证。已有 system/infra 默认构建方式继续有效；带 HRM 的构建生成独立的 `yudao-server-hrm.jar`。

## 构建与测试

使用 JDK 8 和 Maven 3.9，仓库根目录执行：

```bash
mvn -B -ntp -Phrm -pl yudao-server -am verify
```

该命令同时启用 HRM 和 BPM，编译并执行现有框架、system、infra、BPM、HRM 回归，打包 `yudao-server/target/yudao-server-hrm.jar`。不要只开启 server 的 HRM 依赖而漏掉根 reactor 中的模块。

## 数据库初始化与升级

新建开发数据库时，先导入 `sql/mysql/ruoyi-vue-pro.sql`，再导入 [HRM/BPM 增量建表](../sql/mysql/upgrade/20261004-hrm-bpm-baseline.sql)。主库 SQL 含 DROP TABLE，只用于新库初始化；已有 system/infra 数据库只应用增量文件。

```bash
mysql --default-character-set=utf8mb4 -h localhost -P 3306 -u YOUR_USER -p YOUR_DATABASE \
  < sql/mysql/upgrade/20261004-hrm-bpm-baseline.sql
```

迁移使用 MySQL 8.0+，只执行 `CREATE TABLE IF NOT EXISTS`，不修改或清空现有表，也不导入示例工资、税率、缴费政策或真实员工。已有 HRM/BPM 安装应先比较现有结构；此文件不会自动修复旧表的缺列或错误类型。

金额沿用现有对象的 DECIMAL 类型。序列化工资、班次和流程配置使用 TEXT/MEDIUMTEXT，兼容已有类型处理器并避免 H2 大 VARCHAR 导致 MySQL 行长度超限。日期使用 DATETIME，可保留 1970 年前的出生日期及 2038 年后的合同期限。业务表包含租户、软删除、自增主键和常用查询索引；全局薪资项模板的访问仍遵循原有 `@TenantIgnore`。

Flowable 的 `ACT_*` / `FLW_*` 引擎表与 BPM 应用表分开管理。当前 `application.yaml` 使用 `flowable.database-schema-update: true`，首次启动会按 Flowable 6.8.0 建表；生产环境应按数据库权限及变更流程采用预建引擎表，并配置 `false`，避免启动时隐式升级。`check-process-definitions: false` 表示不会自动部署审批模型，需通过 BPM 管理功能明确发布；启用模块不代表已有薪资审批链。

## 启动

配置所选 Spring profile 的 MySQL、Redis、租户与消息等环境参数，然后运行：

```bash
java -jar yudao-server/target/yudao-server-hrm.jar --spring.profiles.active=local
```

可在本机已有服务运行时用 `--server.port=48081` 启动独立验证实例。测试应使用独立数据库与 Redis 数据库，避免共享缓存影响已运行服务。检查 `/v3/api-docs/hrm`、`/v3/api-docs/bpm`，再以获授权用户访问薪资、考勤、社保及 BPM 查询接口。

使用获授权的验证用户 token 运行只读 API 回归，脚本不会输出 token、员工或工资明细：

```bash
read -r -s HRM_SMOKE_TOKEN
export HRM_SMOKE_TOKEN
python3 script/hrm/smoke.py --base-url http://127.0.0.1:48081 --tenant-id 1
unset HRM_SMOKE_TOKEN
```

检查 HRM/BPM OpenAPI、工资/税务/工资条/考勤/社保查询、BPM 模型与分类、system/infra 基础查询及匿名工资查询拒绝。仅在启用本地 mock 登录的测试环境可使用框架的 mock token，生产环境不得启用 mock 登录。

前端完整项目仍在独立 Vue3 仓库；主库 SQL 已含 HRM 菜单，菜单组件须与该前端的 `src/views/hrm/**` 对齐。该基础实现没有新增薪资权限角色、政策参数或员工业务数据，不应凭菜单可见认为真实核算已经可用。

## MySQL 迁移回归

需要 Docker 和 Python 3：

```bash
bash script/hrm/verify-mysql-baseline.sh
```

脚本创建随机命名、无外网和无端口映射的独立 MySQL 8.0.46 容器，并在退出时清理。验证 58 张应用表、租户列、自增主键、InnoDB、重复执行保留数据、历史日期、金额精度、中文/emoji、长序列化快照和软删除。可用 `MYSQL_TEST_IMAGE` 指定已准备的兼容 MySQL 8 镜像。脚本不会连接现有数据库。

构建回归还修复了现有 infra 文件模块的一处 MIME 兼容问题：Tika 2.9.3 将 Markdown 识别为 `text/x-web-markdown`，而现有允许列表和文件测试使用 `text/markdown`。统一检测结果和扩展名转换，保持已有上传类型校验。

本模块验收后再进入需求征集编码。实际薪酬规则、来源接入、工资审批、银行回盘和工资条发布按后续业务模块逐项实现并回归。

## 本批验收记录（2026-10-04）

- `-Phrm` reactor 回归共 1439 项：零失败、零错误，24 项为已有跳过项；其中 HRM 571 项全部执行通过，BPM 56 项中 6 项为已有跳过项。
- HRM 独立 jar 打包和 MySQL 8.0.46 隔离迁移回归通过，包括超过 65,535 字节的工资/流程配置快照。
- 独立数据库与 Redis 数据库的服务启动通过；13 项只读 smoke 检查通过，加载 HRM 312 条、BPM 80 条 API 路径。
- 独立验证数据中，租户 1 的列表只返回该租户员工，按 ID 查询其他租户员工返回空结果；匿名工资查询被拒绝。

这些结果证明启动基础与现有能力的回归，不代表未实施的薪资流程、业务规则、来源连接或外部投递已完成。
