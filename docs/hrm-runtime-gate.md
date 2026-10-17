# HRM 真实运行验收门禁

入口：`.github/workflows/hrm-runtime.yml`。本门禁验证标准工资样例，不替代地区政策确认、业务负责人签字、真实银行联调或全部 46 项原始需求的验收。

## 实际检查

- 独立 MySQL 数据库全新安装全部迁移，重复升级；正常接口同步完整工资目录。
- 禁用 mock 登录；真实管理员、HR 复核、财务批准、员工本人账号及主体/部门/本人范围。
- 两名普通工资员工，加上独立主体的普通工资、月中转正、月中入职三类人员；上月计薪、本月支付。所有员工的定薪三级流程、工时规则、缴费、年度税务资料、四类核算输入独立批准。
- 普通两人样例每人应发6000、个人缴费400、税18、实发5582，总成本13600；三类人员样例按上月天数独立计算，4500/6000于16日转正，6000于16日入职，后两人声明年度首次工资。期望在试算前生成，验证金额、两笔失败重试和本人页面。测试支付年月取执行日，期初累计金额与支付月份一致。
- HR/财务复核批准、冻结、禁止冻结后重算；真实银行 Excel 导出、错误金额拒绝、并发重复回盘幂等、一成功一失败、旧发放次数拒绝、失败重试到账、归档。
- 本人工资条读取、其他账号拒绝、已读、撤回不可见、重新发布。仅生成隔离账号的站内待投递记录，关闭任务调度，不发送短信/邮件或向银行、税务机关提交。
- 60/40 成本分摊、单位缴费、批准报告、预算/营收/人数指标金额核对。
- Chromium 真正登录：需求确认写入、9 个模块的需求来源上下文、390/768/1440 触控新增保存刷新、归档批次和本人工资条金额。
- 第三个主体补录已结束月份的历史工资并通过非零税务申报/完税链路：申报Excel、独立复核、虚构离线回执、17+1元凭证导入/重放、自审拒绝、差额拒绝、18元对账、实际完税Excel和浏览器页面；同时验证未授权主体与完整税务标识导出拒绝。
- 月中调薪6000→9000于16日生效；36000单独计税奖金导致旧版冻结被拒，重算后付款归档；同年重复单独计税被拒、合并计税正向对照、旧快照和历史申报保持一致。浏览器实际切换版本并核对冻结员工姓名和金额。
- 单独执行真实 MyBatis Mapper + MySQL 租户过滤/行锁/事务测试，禁止把默认跳过视为通过。

## Cloud 本地运行

准备 JDK 8、Node 22、Python 3.12 和 Docker；MySQL 8 监听本机 13306，容器名 `codex-ruoyi-mysql`、root 空密码，Redis 监听本机 16380。仅使用一次性隔离容器。脚本不会建立或删除这些容器，也不会覆盖已有数据库。48080、4173 必须空闲。Redis 的验收索引也须为空（默认 13，可用 `--redis-database` 指定）；脚本拒绝复用已有缓存且不会清空其他运行的缓存。

```bash
python -m pip install -r scripts/hrm/requirements-runtime.txt
python -m playwright install --with-deps chromium
# 默认使用 /usr/bin/chromium；如使用 Playwright 下载的浏览器，设置 HRM_CHROMIUM_PATH。
mvn -B -ntp -pl yudao-server -am package -Dmaven.test.skip=true
cd yudao-ui/yudao-ui-admin-vue3
pnpm install --frozen-lockfile
node --max_old_space_size=4096 node_modules/vite/bin/vite.js build --mode development --outDir /tmp/hrm-runtime-ui
cd ../..
python scripts/hrm/runtime_gate.py \
  --database hrm_remediation_unique_run \
  --ui-dir /tmp/hrm-runtime-ui \
  --output-dir /tmp/hrm-runtime-evidence
```

`--database` 必须以 `hrm_remediation_` 开头且尚不存在。脚本自动启动后端与前端，完成后仅停止自己启动的进程，数据库留存以供复核。凭据、税务加密密钥及服务日志放在新建的仓库外 0700 临时目录，状态文件为 0600；不得将该目录加入提交或 CI 附件。结果目录只包含虚构数据、断言结果及截图。

员工 SQL 只用于准备不触发联系人通知的虚构身份、入职/转正日期、组织和账号绑定；工资、归属、政策、输入、付款、工资条与成本均通过正常业务 API 处理。初始化管理员密码散列也仅在新建隔离库中设置。

## 判定

仅所有子进程成功且生成 `gate-result.json` 的 `passed=true` 才视为门禁通过。失败时不得用旧结果文件、H2 测试成功或页面能打开替代本次结果。GitHub 分支保护需将 `HRM MySQL and browser acceptance / runtime` 配置为必需检查；本仓库提交无法替管理员修改远端分支保护。
