# Codex Cloud 全量测试记录（2026-10-16）

最终应用代码基线：`f885d81214`；后端改动基线：`4e3e4c76f0`。本次针对 `yudao-server` Maven 依赖图中的全部启用模块执行测试，包含框架、基础设施、系统、BPM、OA 和 HRM。未启用的业务模块不计入本次范围。

## 运行环境

Codex Cloud 中实际运行 Java 8 / Spring Boot、MySQL 8、Redis 7、Flowable 及 Vue3 静态构建。后端使用隔离库 `hrm_acceptance_20261016_final`；关闭模拟登录、Quartz 自动调度及测试验证码，使用真实账号密码验证登录。账号、令牌和身份加密密钥保存在仓库外，没有写入 Git。

容器内部前端地址为 `http://127.0.0.1:4173`，后端为 `http://127.0.0.1:48080`。这些是 Cloud 容器内部地址，不是用户电脑或公网地址。手工远程访问需要平台提供端口转发，并使前端 API 地址或同源反向代理指向相应后端；本轮不将内部地址当作已发布站点链接。Cloud 环境服务可能随任务或实例停止，不作为长期生产托管。

## 已完成的后端与迁移结果

全部启用模块 Maven 回归成功，耗时4分30秒。本轮新生成的 Surefire XML 统计如下；发现数包含跳过项，不将跳过计为通过。

| 范围 | 发现 | 通过 | 跳过 | 失败/错误 |
| --- | ---: | ---: | ---: | ---: |
| 框架 | 126 | 126 | 0 | 0 |
| 基础设施 | 207 | 197 | 10 | 0 |
| 系统 | 479 | 471 | 8 | 0 |
| BPM | 56 | 50 | 6 | 0 |
| OA | 795 | 781 | 14 | 0 |
| HRM | 1095 | 1095 | 0 | 0 |
| 合计 | 2758 | 2720 | 38 | 0 |

跳过项为附件S3/FTP/SFTP/本地客户端10项，短信客户端/邮件投递8项，BPM候选策略6项，OA模型初始化14项，均沿用现有禁用标记。

另建隔离库 `hrm_cloud_full_20261016_0754`，50个有序业务迁移完成全新安装并再次重复执行；171张映射表、3017字段零缺失。现有运行验收库的同项结构检查也通过。46项需求证据映射及Java字段解析回归均通过。

## 本轮发现与修复

首次不筛选测试类的 Maven 回归在附件模块失败：`FileServiceImplTest` 的两项测试报告“不支持的文件类型”。实际复现表明 Tika 将 Markdown 返回为历史 MIME 别名 `text/x-web-markdown`，与上传服务支持的 `text/markdown` 不一致。

提交 `4e3e4c76f0` 将三种文件类型识别入口的该别名归一为标准 MIME 类型，保持未知类型拒绝规则。新增真实 Markdown 扩展名及内容识别回归；35项附件服务测试及2项类型工具测试通过。

初次同时执行后端回归、前端构建和浏览器检查时，构建进程被系统终止（137），浏览器后续页面崩溃。这两次不计为通过。后续重任务分开执行，前端生产构建独立重跑通过，耗时41.65秒。

完整前端类型检查首次因4GB堆不足退出，不作为通过。停止隔离后端、预留内存后，以6GB堆执行 `vue-tsc`，实际发现45条类型诊断，其中HRM23条。修复后相同完整检查退出码0、输出0条诊断。

- 公共弹窗封装支持默认标题、透传输入校验选项并返回提示框Promise；用实际TypeScript源码转译及模拟弹窗调用验证参数与返回值。该问题此前会使HRM传入的依据校验器失效。
- HRM导入错误Excel导出使用有类型的二进制下载入口；工资状态为空时不显示发布资格，清理两个未使用的国际化变量。
- 显式导入AI/BPM/IoT/MES的消息框及PMS树组件，避免依赖未声明的全局变量；FMS规则筛选改为逐值比较，兼容数字字典类型。
- MES SN前端对照仓库现有Java控制器及VO修正生成字段`count`、分组分页/导出、UUID明细及删除，补齐类型和明细入口。模拟请求检查验证路径及参数；MES后端没有在本实例启用，未声称完成MES真实接口验收。
- 修改后的15个Vue组件均完成SFC解析与脚本编译。修复分别提交：`0705677b03`（公共弹窗）、`6faacc7b88`（HRM）、`37610a5b0b`/`f885d81214`（MES）、`83d166b0e4`（FMS）、`70726940e5`（AI）、`d49125c9c3`（BPM）、`00957b1079`（IoT）、`a59f2bed68`（PMS）。

## 最新产物运行验收

- 主服务及全部启用模块打包成功，耗时1分02秒；修复后的前端生产构建成功，耗时1分05秒；本地验收配置构建成功，耗时41.22秒。
- 会话间隔后重新启动前后端，确认监听4173和48080，HTTP健康访问成功；使用新构建重新完成真实登录和19个基础接口、12个主体接口、Excel导出、初始化幂等、主体及计划独立审批、菜单/主体/租户权限检查。
- 最新运行构建的Chromium真实登录巡检19个HRM页面，全部成功；未捕获JavaScript异常0，HRM接口错误0。另实际点击主体选项与查询按钮，确认页面显示已批准预算100000，而非仅检查空页面。
- 页面截图保存在Cloud本地 `/tmp/hrm-cloud-screenshots-final-20261016/`，交互截图为 `/tmp/hrm-final-analysis-selected.png`。测试日志保存在 `/tmp/hrm-cloud-*-20261016.log`，临时文件不会随Git提交，环境回收后可能消失。
- 本次保留前后端进程供当前Cloud环境继续检查；没有创建公网地址或发布到Codex Sites。

## 可重复命令

每个新终端先加载 Cloud 中已安装的工具链：

```bash
source /workspace/.cloud-setup/ruoyi-vue-pro/env.sh
cd /workspace/ruoyi-vue-pro
mvn -B -ntp -fae -pl yudao-server -am test
mvn -B -ntp -pl yudao-server -am package -DskipTests
python3 scripts/hrm/check_audit.py
PYTHONDONTWRITEBYTECODE=1 python3 scripts/hrm/test_schema_fields.py
python3 scripts/hrm/check_schema.py --container codex-ruoyi-mysql --database hrm_acceptance_20261016_final
```

Cloud容器内存上限8GB，完整类型检查期间需停止隔离后端并为Node预留足够内存，结束后恢复服务；不在生产实例操作。前端重任务独立执行，避免与后端测试及浏览器争用内存：

```bash
cd /workspace/ruoyi-vue-pro/yudao-ui/yudao-ui-admin-vue3
node --max_old_space_size=6144 node_modules/vue-tsc/bin/vue-tsc.js --noEmit --incremental --tsBuildInfoFile /tmp/hrm-cloud-typecheck-6144-20261016.tsbuildinfo
node --max_old_space_size=4096 node_modules/vite/bin/vite.js build --mode prod --outDir /tmp/hrm-cloud-prod-fixed-20261016
```

接口和浏览器验收脚本为 `scripts/hrm/smoke_api.py`、`scripts/hrm/acceptance_workflows.py`、`scripts/hrm/browser_acceptance.py`。密码分别从环境变量读取；需要虚构数据的工作流显式要求 `--create-isolated-fixtures`，并将状态文件放在仓库外。详细运行约束见[已有验收说明](hrm-runtime-verification.md)。

## 覆盖边界

接口检查使用真实登录，覆盖19个基础接口、12个主体接口、需求初始化幂等、Excel导出、独立审批及菜单/主体/租户权限。浏览器页面巡检用于验证页面加载及请求，不等同于逐按钮完成全部业务；核算、更正、现金回盘与并发等分支主要由后端回归覆盖。

未向真实人员发送通知，未执行真实银行付款、税务提交或外部机构联调。既有被禁用的外部服务、BPM策略和OA模型初始化测试应明确计为跳过，不计入通过数。
