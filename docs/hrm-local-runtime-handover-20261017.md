# HRM 薪酬项目本地运行交接

交接日期：2026-10-17。代码仓库：<https://github.com/waitforcopilot/ruoyi-vue-pro>，本次交付分支为 `work`。本机运行不依赖 Codex Cloud；以下命令在本机终端执行，推荐 Linux、macOS 或 Windows 的 WSL2。文中的配置生成步骤经过代码核对，**原稿编写时未在本机执行；后续实机记录见第10/11节，最新阅读入口见第0节**。

## 0. 最新阅读入口（2026-10-20归档补充）

**第1～11节保留各次历史交接及执行记录，包含曾经未启动、旧提交/迁移数量和后来完成的状态，不能将其当作当前库状态执行。** 第10节的首次建库、`--fresh`和一次性密钥生成只适用于当时证明不存在的新库；本机环境此后已有数据和固定密钥，日常续接不得照旧初始化或覆盖。

- 当前交付与剩余问题以[阶段路线](hrm-payroll-stage-delivery-plan-20261019.md)、[S6报告](hrm-payroll-stage-6-report-20261019.md)及[FA-01处理报告](hrm-payroll-fa01-fix-report-20261020.md)为准。上一轮FA-01实现`f64de5524c`及回执`50db82ea68`已push；本轮仅审查归档文档，不重新开展业务验收。
- 当前需求依据：[工作台PRD v0.4](hrm-payroll-requirements-workbench-prd-20261017.md)，仍是待业务确认的评审稿；[独立修复审计](hrm-payroll-stage-1-6-fix-audit-20261020.md)、[独立复核](hrm-payroll-stage-1-6-fix-audit-reaudit-20261020.md)保留当时结论，FA-01后续处理另见上链，不修改历史审计原文。
- F-07真实受控授权/条款批准材料和F-15正式组合验收仍未关闭；不得以TEST发布或旧工资业务样本替代正式批准。
- 日常管理入口仍为`~/.hrm-payroll-local/manage.sh status|start|stop|restart|backup`。凭据、连接配置、数据库备份和税务密钥留在仓库外`~/.hrm-payroll-local/`；不要上传该私有目录。
- 本次归档范围、敏感检查及验证边界见[Git变更审查记录](hrm-git-change-review-20261020.md)。

## 1. 当前状态和验收边界

| 内容 | 结果及证据 |
| --- | --- |
| 审计 F-01～09 的已复现技术问题 | 已修复并分模块提交；详见[补齐记录](hrm-payroll-audit-remediation-20261016.md) |
| 两人普通工资，定薪、输入审批、核算、冻结、回盘、工资条、成本 | 全新 MySQL 和 Chromium 实际验证通过 |
| 月中入职、月中转正、计薪月/支付月分离 | 实际验证通过 |
| 历史申报、17+1元完税导入与零差额对账 | 实际 API、Excel、浏览器通过；均为虚构离线凭证 |
| 月中调薪、36000单独计税奖金、旧版冻结拦截、年度额度防重 | 新库 API/浏览器通过；最新整条成功门禁为 `salary-bonus-gate/gate-result.json` |
| 排班、手工打卡、非零迟到/早退/缺卡扣款、补卡闭环 | 日/期间/月度汇总、实际Excel、10条履历及浏览器通过；新增用例已接入门禁，接入后整条新库门禁尚未再跑 |
| OA加班、综合/不定时工时、调休 | 已部署隔离流程并实际提交加班；首笔批准遇到可选手机号导致短信异常。修复通过4项专项测试及打包，**尚未重启实测修复后的完整工时矩阵** |
| 机构实缴、补缴/退款、年度调基、追溯/负数收回、跨年等 | 仍需继续运行验收，不能据已有单测声明全部满足 |

关键提交：`dab757d530`（版本姓名修复、调薪/奖金验收）、`1df39bd0c1`（考勤真实流程验收）。BPM可选手机号修复和本交接文档随后分别提交，运行 `git log -6 --oneline` 可查。所有原始要求仍以[原始需求运行审计](hrm-payroll-original-requirements-runtime-audit-20261016.md)和[归档PRD](hrm-prd-audit-inputs/hrm-payroll-prd.md)为准。

此前全量启用模块测试2780项：2742通过、38跳过，0失败/错误；HRM1105项全部通过。最新BPM测试60项：54通过、6跳过，0失败/错误，其中新增4项通知测试全部通过。前端完整类型检查、development运行构建、prod部署构建已通过；最新BPM修改不涉及前端。没有把跳过测试或尚未执行的脚本记为通过。

## 2. 本机准备

- JDK **8**（本项目是 jdk8 分支，建议 Temurin/Zulu 8；不要直接换成JDK17）。
- Maven 3.9.x、Node.js 22、pnpm 10、Python 3.12、Git。
- Docker Engine / Docker Desktop，支持 `docker compose`。建议可用内存至少12GB，16GB更从容；完整前端类型检查约需6GB堆，避免与构建同时运行。
- 本机端口：MySQL `13306`、Redis `16380`、后端 `48080`、开发前端 `5173`；浏览器验收使用 `4173`。

```bash
git clone --branch work https://github.com/waitforcopilot/ruoyi-vue-pro.git
cd ruoyi-vue-pro
java -version
mvn -version
node --version
corepack enable
corepack prepare pnpm@10.30.0 --activate
python3 --version
docker compose version

python3 -m venv .venv
source .venv/bin/activate
python -m pip install -r scripts/hrm/requirements-runtime.txt
```

后续 Python 命令均使用上述虚拟环境。使用独立的私有配置目录，避免把本机口令、税务密钥或运行状态提交Git：

```bash
export HRM_LOCAL_DIR="$HOME/.hrm-payroll-local"
mkdir -p "$HRM_LOCAL_DIR"
chmod 700 "$HRM_LOCAL_DIR"
```

## 3. 创建本地 MySQL 和 Redis

下面两台容器专供本项目本地测试，绑定回环地址。先确认本机没有同名容器或端口占用；已有数据库不要重复执行全新安装。为了兼容仓库现有迁移/验收脚本，保留这些容器名。空密码仅限这一组本地测试容器。

在本机执行，生成私有 Compose 文件：

```bash
cat > "$HRM_LOCAL_DIR/compose.yaml" <<'YAML'
services:
  mysql:
    image: mysql:8.0
    container_name: codex-ruoyi-mysql
    environment:
      MYSQL_ALLOW_EMPTY_PASSWORD: "yes"
      TZ: Asia/Shanghai
    ports:
      - "127.0.0.1:13306:3306"
    volumes:
      - mysql-data:/var/lib/mysql
    command: ["--character-set-server=utf8mb4", "--collation-server=utf8mb4_unicode_ci"]
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "127.0.0.1"]
      interval: 3s
      timeout: 3s
      retries: 40
  redis:
    image: redis:7
    container_name: codex-ruoyi-redis
    ports:
      - "127.0.0.1:16380:6379"
    volumes:
      - redis-data:/data
    command: ["redis-server", "--appendonly", "yes"]
volumes:
  mysql-data:
  redis-data:
YAML

docker compose -p hrm-local -f "$HRM_LOCAL_DIR/compose.yaml" up -d --wait
```

从仓库根目录安装一个**尚不存在**的本地库；脚本按 `scripts/hrm/migrations.txt` 完整执行基础库和52个HRM/BPM/OA迁移，拒绝覆盖现有库：

```bash
python scripts/hrm/migrate.py --container codex-ruoyi-mysql --database hrm_local_20261017 --fresh
# 后续升级同一个库，移除 --fresh；不要再次手动导入基础SQL。
python scripts/hrm/migrate.py --container codex-ruoyi-mysql --database hrm_local_20261017
```

`sql/mysql/ruoyi-vue-pro.sql` 含删除旧表操作，只用于新库。已有业务库先备份，不用 `--fresh` 或基础SQL覆盖。

## 4. 生成私有配置和本机管理员口令

在仓库根目录执行以下一次性命令。交互输入8～16位管理员口令；口令不会写入文档或输出。生成两把随机税务密钥并保存在私有配置中，之后不要每次启动重新生成，否则已有加密税务资料无法解密。此步骤也关闭本地测试库的短信模板/渠道。

```bash
python - <<'PY'
import base64, getpass, json, os, pathlib, subprocess
import bcrypt

folder = pathlib.Path(os.environ['HRM_LOCAL_DIR']).expanduser().resolve()
folder.mkdir(parents=True, exist_ok=True)
folder.chmod(0o700)
config_file = folder / 'application-workstation.yaml'
if config_file.exists():
    raise SystemExit('配置已存在，保留原密钥；无需重复初始化。')
password = getpass.getpass('设置本机 admin 口令（8～16位）: ')
if not 8 <= len(password) <= 16:
    raise SystemExit('口令长度必须为8～16位。')
url = ('jdbc:mysql://127.0.0.1:13306/hrm_local_20261017'
       '?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true'
       '&nullCatalogMeansCurrent=true&useUnicode=true&characterEncoding=UTF-8')
config = {
    'server': {'address': '127.0.0.1', 'port': 48080},
    'spring': {
        'datasource': {'dynamic': {'datasource': {
            name: {'url': url, 'username': 'root', 'password': ''}
            for name in ('master', 'slave')}}},
        'redis': {'host': '127.0.0.1', 'port': 16380, 'database': 0},
        'quartz': {'auto-startup': False},
        'boot': {'admin': {'client': {'enabled': False}}}},
    'flowable': {'async-executor-activate': False},
    'yudao': {
        'security': {'mock-enable': False}, 'captcha': {'enable': False},
        'hrm': {'tax': {
            'identity-encryption-key': base64.b64encode(os.urandom(32)).decode(),
            'identity-fingerprint-key': base64.b64encode(os.urandom(32)).decode()}}},
    'logging': {
        'level': {'cn.iocoder.yudao.module': 'WARN',
                  'cn.iocoder.yudao.framework.apilog': 'OFF',
                  'cn.iocoder.yudao.framework.web.core.handler.GlobalExceptionHandler': 'OFF'},
        'file': {'name': str(folder / 'server.log')}}}
fd = os.open(config_file, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
with os.fdopen(fd, 'w') as out:
    json.dump(config, out)  # JSON是合法YAML，Spring可以读取。
hashed = bcrypt.hashpw(password.encode(), bcrypt.gensalt(rounds=10)).decode()
subprocess.run(['docker', 'exec', '-i', 'codex-ruoyi-mysql', 'mysql', 'hrm_local_20261017'],
    input=f"UPDATE system_users SET password='{hashed}' WHERE id=1; "
          "UPDATE system_sms_channel SET status=1; UPDATE system_sms_template SET status=1;",
    text=True, check=True)
print('私有配置和管理员口令已初始化。')
PY
```

不要直接使用仓库 `application-local.yaml` 的数据库默认值和 `mock-enable: true`。上面的附加配置同时覆盖master/slave、真实登录、Redis端口和税务密钥；不需要修改提交到仓库的配置。

## 5. 本机启动前后端

后端终端（仓库根目录，JDK8已在PATH中）：

```bash
export HRM_LOCAL_DIR="$HOME/.hrm-payroll-local"
mvn -B -ntp -pl yudao-server -am package -Dmaven.test.skip=true
java -Xms256m -Xmx1536m -Duser.timezone=Asia/Shanghai \
  -jar yudao-server/target/yudao-server.jar \
  --spring.profiles.active=local,workstation \
  --spring.config.additional-location="file:$HRM_LOCAL_DIR/"
```

另开前端终端：

```bash
cd ruoyi-vue-pro/yudao-ui/yudao-ui-admin-vue3
pnpm install --frozen-lockfile
pnpm exec vite --mode development --host 127.0.0.1 --port 5173 --strictPort
```

访问 <http://127.0.0.1:5173>，租户填“芋道源码”，账号 `admin`，口令为上一步自己设置的值。后端API根路径为 <http://127.0.0.1:48080/admin-api>；以下无登录查询返回 `data:1` 表示后端已就绪：

```bash
curl --get --data-urlencode 'name=芋道源码' \
  http://127.0.0.1:48080/admin-api/system/tenant/get-id-by-name
```

初次启动可能需要一分钟以上。开发前端使用 `.env.development`，直接请求127.0.0.1:48080，验证码开关已与本地后端一致。更改后端端口时也要改前端环境配置。首次进入新库没有虚构工资数据是正常现象；可手工建档，或运行下一节的独立验收环境生成样例。

## 6. 在本机执行已有真实运行门禁

这条命令会在**本机**启动前后端、执行真实HTTP和Chromium验收；不是调用Cloud。先用Ctrl+C停止第5节两个应用进程，保持MySQL/Redis运行。门禁使用另一个新库与空Redis索引，不覆盖日常本地库。

```bash
# 仓库根目录，Python虚拟环境已激活。
python -m playwright install --with-deps chromium
export HRM_CHROMIUM_PATH="$(python -c 'from playwright.sync_api import sync_playwright; p=sync_playwright().start(); print(p.chromium.executable_path); p.stop()')"

cd yudao-ui/yudao-ui-admin-vue3
node --max_old_space_size=4096 node_modules/vite/bin/vite.js build \
  --mode development --outDir /tmp/hrm-local-runtime-ui
cd ../..

python scripts/hrm/runtime_gate.py \
  --database hrm_remediation_local_20261017a \
  --redis-database 13 \
  --ui-dir /tmp/hrm-local-runtime-ui \
  --output-dir /tmp/hrm-local-runtime-evidence
```

数据库名称必须以 `hrm_remediation_` 开头且尚不存在。重复执行改成另一个新库和空Redis索引；脚本拒绝覆盖已有库或清空其他缓存。`gate-result.json` 中 `passed:true` 才代表本次完整成功；失败后保留已成功子步骤不等于整条通过。

脚本结束会停掉它自己启动的前后端，并输出私有状态目录路径。该目录含随机登录口令、税务密钥及日志，只留本机，不上传Git或发送截图。证据目录只包含虚构业务数据和断言结果。门禁已包含考勤新增用例；OA三类工时待续验脚本尚未纳入必过门禁。

### 接着执行尚未完成的OA/工时矩阵

先完成本机门禁，记下它打印的私有目录，例如 `/tmp/hrm-runtime-gate-xxxx`。**这是本机新生成的目录，不要照抄Cloud旧目录。** 使用它的密钥/数据库重新启动后端，前端不是该脚本的必要条件：

```bash
export HRM_GATE_PRIVATE=/tmp/hrm-runtime-gate-xxxx
java -Xms256m -Xmx1536m -Duser.timezone=Asia/Shanghai \
  -jar yudao-server/target/yudao-server.jar \
  --spring.profiles.active=local,runtime-gate \
  --spring.config.additional-location="file:$HRM_GATE_PRIVATE/"
```

另开终端（根目录及虚拟环境）：

```bash
export HRM_GATE_PRIVATE=/tmp/hrm-runtime-gate-xxxx
python scripts/hrm/runtime_oa_work_regression.py \
  --base-state "$HRM_GATE_PRIVATE/session.json" \
  --state-file "$HRM_GATE_PRIVATE/oa-work.json" \
  --database hrm_remediation_local_20261017a \
  --output-dir /tmp/hrm-local-oa-work-evidence
```

该脚本为**待验证矩阵**，不是已通过证据：真实BPM部署/加班提交已试跑，首笔审批手机号阻断已单测修复，但其余流程尚未跑通。预设验算为标准工时150元及120分钟调休、综合工时两个月实际1080分钟/基准960分钟/认可120分钟，周期金额150元；不定时普通日0元、虚构节假日300元。周期基准是特意缩短的虚构测试值，不代表正式制度。若报错，保留私有状态和错误码，修复后可按阶段继续；不要手写SQL把审批改成通过。

## 7. 后续工作的优先顺序

1. 重新启动后实际复验“无手机号仍可批准OA加班”，确认OA结果事件和工时来源可读，之后跑完三类工时/跨月/调休脚本并补浏览器证据。
2. 扩展OA请假/出差/外出、调休关联请假，考勤/工时进入冻结工资以及源变更阻断。已完成的手工补卡不能替代这些场景。
3. 机构实缴账单导入、差额对账、年度调基、补缴/退款及工资成本联动。
4. 调薪追溯、负数收回、跨年补发、入离职及缺失/重复/错期异常；保留前后版本、现金和成本证据。
5. 按原始46项逐条更新覆盖和业务验收，不把虚构验算当作负责人正式签字。继续保持每个模块完成后独立提交。

原始报告不改写历史结论；新增证据继续追加至 `docs/hrm-payroll-audit-remediation-20261016.md` 和 `docs/hrm-prd-remediation-evidence/`。

## 8. 开发检查、备份和停止

无需启动应用即可先运行：

```bash
python scripts/hrm/check_audit.py
mvn -B -ntp -pl yudao-server -am test
cd yudao-ui/yudao-ui-admin-vue3
node --max_old_space_size=6144 node_modules/vue-tsc/bin/vue-tsc.js --noEmit
node --max_old_space_size=4096 node_modules/vite/bin/vite.js build --mode prod
```

默认Java测试中部分MySQL集成测试会跳过；真实Mapper行锁验证需要显式环境变量及独立测试库，配置见 `.github/workflows/hrm-runtime.yml`。不要把默认跳过当作通过。类型检查和Vite构建按顺序执行；8GB机器上先停止后端以释放内存。

备份本机数据库与对应税务密钥，二者必须一起保留：

```bash
umask 077
docker exec codex-ruoyi-mysql mysqldump --single-transaction --no-tablespaces \
  --set-gtid-purged=OFF hrm_local_20261017 > "$HRM_LOCAL_DIR/hrm-local-backup.sql"
# application-workstation.yaml 也需备份至你自己的私有备份位置。
```

前后端终端按Ctrl+C停止。数据库停止但保留数据卷：

```bash
docker compose -p hrm-local -f "$HRM_LOCAL_DIR/compose.yaml" stop
# 下次恢复：
docker compose -p hrm-local -f "$HRM_LOCAL_DIR/compose.yaml" start
```

不要用 `down -v` 删除数据卷，除非明确要丢弃全部本地数据。Cloud的数据库、随机账号、税务密钥、node_modules、JAR和/tmp目录不会随Git自动同步；本交接推荐在本机新建隔离库。已有Cloud运行证据已入Git，可直接阅读，无需继续启动Cloud。

## 9. 常见问题

| 现象 | 检查方法 |
| --- | --- |
| Java编译或启动版本不对 | `mvn -version` 和 `java -version` 都应指向JDK8；IDE的Project SDK及Maven Runner也要一致 |
| JDBC连到3306或旧数据库 | 确认 `local,workstation` profile和附加配置目录结尾的 `/`；master/slave都要覆盖 |
| 税务身份无法解密 | 使用原数据库对应的两把密钥；不要对已有库重新生成密钥 |
| 登录失败/验证码不匹配 | 租户“芋道源码”、自己设置的admin口令；development前端和本地后端验证码都关闭，mock登录关闭 |
| 前端请求错误端口 | `.env.development` 的 `VITE_BASE_URL`，以及启动时的mode；浏览器看真实请求URL |
| 构建内存不足 | 停止后端，分别执行类型检查和Vite；给Docker及Node分配足够内存 |
| 门禁拒绝数据库/Redis | 使用尚不存在的数据库和空Redis索引；不要清空日常库/缓存来迁就门禁 |
| 页面为空但接口正常 | 新库没有业务数据；先生成样例或手工建档，再检查角色菜单/主体授权 |
| OA待验脚本失败 | 该矩阵尚未完成运行验收；保存错误码与阶段，不以静态源码推断已满足 |

## 10. 本机续接记录（2026-10-17 19:43，Asia/Shanghai）

用户因 Codex 额度不足主动暂停，准备切换账号后继续。以下为本次本机实际执行状态，不能把第1节历史运行证据算作本机验收。已取消等待建库的后台命令及未完成的 Compose 拉取命令；没有留下自动建库/初始化任务。

### 10.1 当前仓库与机器

- 聚合目录：`/Users/waitforpro/source/github-yudao`；真正 Git 根目录：`/Users/waitforpro/source/github-yudao/ruoyi-vue-pro`。
- 原来检出 `docs/hrm-payroll-prd`，工作区干净。已 fetch `origin work` 并创建/切换本地跟踪分支 `work`，当前交付提交 `e1983f94ac`。不需要重新克隆或切回 PRD 分支。
- 已阅读本交接及修复记录；本仓库和适用上级目录未发现 `AGENTS.md`。恢复时如规则文件新增，再读取。
- macOS 26.5（25F71），arm64，CPU 15核，物理内存24GiB；检查时磁盘可用约528GiB、系统可用内存比例约36%（会随运行变化）。13306、16380、48080、5173最初均空闲；现在5173由本项目Vite占用。
- 本次没有启动或连接 Codex Cloud 服务，没有迁移 Cloud 数据/账号/密钥。

### 10.2 工具和已完成验证

| 工具/步骤 | 本机实际状态 |
| --- | --- |
| Git | 2.50.1（Apple Git-155） |
| JDK8 | 已有 Amazon Corretto 1.8.0_492，`/Users/waitforpro/.local/jdks/java8/Contents/Home/bin/java`；Maven实际也使用JDK8 |
| Maven | 使用已有 `/opt/homebrew/bin/mvn` 3.9.16；终端默认另有3.8.9，后续明确使用3.9路径 |
| Node22 | 新安装 `/opt/homebrew/opt/node@22/bin/node` v22.23.3；保留原Node24，不修改全局默认 |
| pnpm10 | 独立安装10.30.0，`/Users/waitforpro/.hrm-payroll-local/tools/node_modules/.bin/pnpm`；调用时PATH前置Node22 |
| Python | 已有3.12.13；独立虚拟环境 `~/.hrm-payroll-local/venv` |
| Python依赖 | requirements-runtime.txt全部安装，bcrypt5.0.0、openpyxl3.1.5、playwright1.62.0；`pip check`通过 |
| 浏览器 | 复用本机已缓存Playwright Chromium修订1234；实际headless启动Chromium151.0.7922.34成功 |
| Docker | 新安装CLI29.9.0；Colima中实际Engine29.5.2 |
| Compose | 新安装5.6.0，`docker compose version`成功；`~/.docker/config.json`增补Homebrew插件目录，保留其他配置 |
| Colima | 新安装0.10.3，Lima2.2.1；独立profile `hrm-local`，VZ/arm64、4CPU、6GiB内存、40GiB数据盘，虚拟机运行中 |
| Java编译 | `/opt/homebrew/bin/mvn -B -ntp -pl yudao-server -am package -Dmaven.test.skip=true` 本机BUILD SUCCESS；未执行完整测试 |
| 前端依赖 | pnpm10 `install --frozen-lockfile`成功；未改lockfile |
| 前端启动 | launchd已运行Vite8.1.4，`http://127.0.0.1:5173`实际HTTP200 |

Homebrew安装过程中升级了其依赖openssl@3、readline、sqlite，并安装其他所需依赖；保留本机原有MySQL8.4、Redis6.2、Java和Node版本，没有启动或修改已有数据库。终端默认工具版本不等于本项目实际使用版本。

### 10.3 暂停时的真实状态与私有文件

私有目录 `/Users/waitforpro/.hrm-payroll-local` 权限700，准备文件权限600；不得提交Git、打印凭据或上传全部目录。此目录属于本机用户，切换Codex账号后应继续复用。

- Colima运行中，当前Docker context已切为 `colima-hrm-local`，后续命令显式指定context以免误操作其他Docker环境。
- Redis7镜像已经完整下载（`redis:7`）；MySQL8镜像仍缺部分层，暂停时未形成完整镜像。
- **尚无本项目MySQL/Redis容器，尚未创建目标库、未执行迁移、未生成管理员口令和税务密钥、未启动Java后端。** 暂停检查时没有 `migrations.log`、`initialize.log`、`application-workstation.yaml`、`credentials.json` 或 `browser-verification.json`，48080无监听。恢复时必须重新核实，不能依据此快照覆盖后来产生的数据。
- 前端由 `local.hrm.frontend` 用户launchd任务运行，KeepAlive=true；关闭聊天不依赖PTY存活。前后端plist仅在私有目录，没有安装成登录自动加载任务。
- `compose.yaml`：本项目MySQL8/Redis7独立容器与具名卷定义，端口仅绑定回环。数据库沿用文档本地隔离容器空root密码，严禁公网暴露。
- `initialize.py`：根据第4节命令生成的私有一次性初始化脚本。它自动生成16位随机admin口令并写入 `credentials.json`，生成两把固定税务密钥到 `application-workstation.yaml`，禁用mock/验证码、Quartz、Flowable异步执行及Admin注册；另外禁用短信渠道/模板、邮件模板，把邮件SMTP改为127.0.0.1:1并清除示例SMTP口令，暂停infra_job。**该脚本尚未执行。** 已有配置时会退出，不允许对既有加密库重新生成密钥。
- `backend.plist`、`frontend.plist`：launchd定义，进程日志和后端业务日志均指向私有目录，Umask077。
- `manage.sh`：start/stop/restart/status/backup；stop保留数据卷；backup同时保存库、配置密钥和凭据。**首次建库和配置完成前不要直接start/restart，否则后端会在缺配置时启动。**
- `verify-browser.py`：待执行的本机真实表单登录与9个主要薪酬页面检查脚本，记录实际API地址、HTTP/业务码、console/pageerror和截图；不生成业务样例、不运行完整业务矩阵。结果文件尚不存在，不能宣称通过；执行后还应人工核查页面内容及接口错误，不能只看脚本passed。
- `README.md`：私有环境说明；安装/构建日志包括 `maven-build.log`、`frontend-install.log`、`python-final-install.log`、`compose-start.log`。原 `python-install.log` / `uv-install.log` 是先前低速下载后取消的尝试；最终安装成功以 `python-final-install.log` 及pip check为准。Playwright最终分段下载并核对PyPI官方SHA256成功。

### 10.4 切换账号后的续接步骤

先读本节和本机适用规则。仅继续本地环境准备、启动和有限验证，不自动运行完整runtime_gate/OA工时矩阵。

```bash
cd /Users/waitforpro/source/github-yudao/ruoyi-vue-pro
export HRM_LOCAL_DIR=/Users/waitforpro/.hrm-payroll-local
export DOCKER_CONTEXT=colima-hrm-local
export JAVA_HOME=/Users/waitforpro/.local/jdks/java8/Contents/Home
export PATH="$JAVA_HOME/bin:/opt/homebrew/opt/node@22/bin:/opt/homebrew/bin:$PATH"

git status --short
colima -p hrm-local status
# 若虚拟机未运行：colima start hrm-local
docker compose -p hrm-local -f "$HRM_LOCAL_DIR/compose.yaml" ps -a
docker compose -p hrm-local -f "$HRM_LOCAL_DIR/compose.yaml" up -d --wait
# 此命令继续拉取未完成镜像层，并创建独立容器；不删除任何已有数据卷。

docker exec codex-ruoyi-mysql mysql -NBe \
  "SELECT schema_name FROM information_schema.schemata WHERE schema_name='hrm_local_20261017'"
```

确认目标库**不存在**时，才使用第3节的 `--fresh` 完整安装（基础SQL＋52迁移），日志留在私有目录。如果库已存在，先查表、迁移状态及对应密钥，必要时先备份；不导入基础SQL，不用 `--fresh`，也不盲目重复一次性初始化。

```bash
# 仅在上一步证明库不存在时：
python3.12 scripts/hrm/migrate.py --container codex-ruoyi-mysql \
  --database hrm_local_20261017 --fresh > "$HRM_LOCAL_DIR/migrations.log" 2>&1
# 检查迁移退出码和全部53条Applied（基础SQL1条＋清单52条），不能只看文件已生成。

# 仅在确认这是新库且私有配置/口令均未初始化时：
"$HRM_LOCAL_DIR/venv/bin/python" "$HRM_LOCAL_DIR/initialize.py"
# 输出仅应为初始化状态，不要cat配置或credentials.json。

"$HRM_LOCAL_DIR/manage.sh" start
```

JAR和前端依赖已经构建/安装，不需要为了续接重复全量构建。若期间源码变化，使用上述明确JDK8/Maven3.9/Node22/pnpm10路径重新构建。等待后端真正就绪，再执行：

```bash
docker exec codex-ruoyi-mysql mysql -NBe 'SELECT VERSION(), 1'
docker exec codex-ruoyi-redis redis-cli ping
curl --get --data-urlencode 'name=芋道源码' \
  http://127.0.0.1:48080/admin-api/system/tenant/get-id-by-name
# 应核实code=0且data=1。

"$HRM_LOCAL_DIR/venv/bin/python" "$HRM_LOCAL_DIR/verify-browser.py"
```

待完成的实际验收：浏览器通过租户“芋道源码”、admin和私有随机口令真实登录；主要薪酬页面内容可见、请求确实指向127.0.0.1:48080；检查浏览器console/pageerror、接口业务码和后端日志，修复启动/登录阻断。新库无工资数据正常，不运行完整业务矩阵或写样例来代替本次目标。

完成后报告软件版本、地址/登录方法、私有配置位置、运行状态、日志、停止/重启命令、验证结果、剩余问题和代码改动。当前仅交接文档追加本机记录，没有业务代码修改，没有新增提交或推送。

### 10.5 当前服务管理

```bash
# 此时仅前端正在运行；以下停止它不会删除任何文件或数据。
launchctl bootout gui/$(id -u)/local.hrm.frontend
# 单独恢复前端：
launchctl bootstrap gui/$(id -u) "$HOME/.hrm-payroll-local/frontend.plist"
# 初始建库与配置完成后，再使用统一管理：
~/.hrm-payroll-local/manage.sh status
~/.hrm-payroll-local/manage.sh stop
~/.hrm-payroll-local/manage.sh start
~/.hrm-payroll-local/manage.sh restart
~/.hrm-payroll-local/manage.sh backup
# 停止应用/容器后还需释放虚拟机内存：
colima -p hrm-local stop
```

未完成：MySQL镜像拉取、容器运行、数据库迁移、私有口令/密钥初始化、后端启动、真实登录及薪酬页面浏览器验证。前端HTTP200不等于业务验收通过。

## 11. 本机续接完成记录（2026-10-17 19:55，Asia/Shanghai）

本节是第10节暂停后的新增实机结果；第10节保留为历史快照。环境准备、启动、真实登录和主要页面检查已完成，没有执行完整业务矩阵或Cloud服务。

### 11.1 当前运行与访问

- 仓库仍为 `/Users/waitforpro/source/github-yudao/ruoyi-vue-pro`，分支 `work`、交付代码 `e1983f94ac`；业务代码未修改，本次仅追加交接文档，未提交/推送。
- 前端：<http://127.0.0.1:5173>；后端API：<http://127.0.0.1:48080/admin-api>。
- 登录：租户“芋道源码”，账号 `admin`；随机16位口令在本机私有文件 `/Users/waitforpro/.hrm-payroll-local/credentials.json`，不在文档中显示。使用文件查看器本机读取，不将内容发到聊天、截图或Git。
- MySQL容器 `codex-ruoyi-mysql`：8.0.46，健康，回环端口13306，数据库 `hrm_local_20261017`；Redis容器 `codex-ruoyi-redis`：7.4.11，运行，回环端口16380、索引0、AOF持久化。
- Java后端和Vite前端由当前用户launchd的 `local.hrm.backend` / `local.hrm.frontend` 管理，KeepAlive自动恢复，独立于聊天终端。Colima专用profile `hrm-local` 正在运行，Docker context `colima-hrm-local`。
- 四个服务的主机监听均为127.0.0.1。服务未配置成重启电脑后自动加载；重启电脑后使用下方start命令恢复。

实际使用版本沿用第10.2节：Git2.50.1、Corretto JDK8u492、Maven3.9.16、Node22.23.3、pnpm10.30.0、Python3.12.13、Docker CLI29.9.0/Engine29.5.2、Compose5.6.0、Colima0.10.3、Vite8.1.4；MySQL和Redis版本以本节实机结果为准。

### 11.2 实际执行和验证

1. 完成MySQL镜像拉取及两个独立容器启动。SQL查询证明目标库不存在后才执行 `--fresh`；迁移退出码0，日志53条Applied（基础SQL1＋清单52），初始化后219张表；启动Flowable后其自有工作流表由引擎管理。
2. 一次性生成私有口令和两把税务密钥，并设置数据库管理员密码散列；配置文件权限600，私有目录700。未使用已有业务库或原Cloud库；没有重置其他库。
3. 禁用短信渠道/模板、邮件模板、infra_job，邮件账号指向127.0.0.1:1并清除示例SMTP口令；实际查询各启用数均0。Quartz自启动、Flowable异步执行、Admin客户端注册、mock认证均关闭。
4. Java后端启动成功，租户就绪查询返回 `{"code":0,"msg":"","data":1}`；MySQL查询正常，Redis返回PONG。
5. Chromium通过真实表单和真实admin口令登录成功，实际登录响应含有效token，系统登录日志成功；验证脚本没有在证据中记录token/口令。
6. 浏览器打开薪酬总览、薪资公式、核算审批、考勤归集与异常、工时与加班、缴费政策与年度调基、年度税务政策、预算与成本分析、需求评审，共9页；检查实际文本并保存截图，人工查看总览及税务页布局。新库暂无工资数据正常，未造业务样例。
7. 最终浏览器记录50次API响应，全部请求127.0.0.1:48080；HTTP及业务码均成功，pageerror=0、console error=0、HTTP资源错误=0、请求失败=0。证据在私有目录 `browser-verification.json` 和9张页面截图，不因路由打开或脚本passed单独认定完整业务通过。
8. 实际配套备份数据库、对应配置密钥和凭据到私有 `backups/`。扫描运行日志未发现当前登录口令或两把税务密钥明文。

本机首次浏览器检查遇到脚本定位隐藏登录表单的问题，已将输入限定到可见元素，修正后真实登录。首次9页检查发现10次外部头像资源502；独立诊断确认来自基础SQL中admin的 `test.yudao.iocoder.cn` 示例头像，已仅清空新建本地库中admin的头像，改用系统默认头像；最终9页已重跑无资源错误。未修改项目业务代码。

私有配置另覆盖了仓库local中具体Mapper的DEBUG日志级别，避免父级WARN被更具体的DEBUG覆盖而输出敏感SQL；税务密钥原样保留。私有manage.sh增加配置存在检查，防止未初始化时启动后端。上述私有脚本不在Git中。

### 11.3 日常管理、日志与限制

```bash
~/.hrm-payroll-local/manage.sh status
~/.hrm-payroll-local/manage.sh stop
~/.hrm-payroll-local/manage.sh start
~/.hrm-payroll-local/manage.sh restart
~/.hrm-payroll-local/manage.sh backup
```

stop停止前后端和两个容器，保留数据卷；还需释放6GiB虚拟机内存时执行 `colima -p hrm-local stop`。start先启动专用虚拟机和容器，再加载前后端；backend首次就绪约需20秒，可重复查询租户接口确认。禁止用 `down -v` 删除数据。

私有配置：`~/.hrm-payroll-local/application-workstation.yaml`（连接配置和固定税务密钥）；口令：同目录 `credentials.json`。数据库与此配置必须一起备份，已有加密数据不得重新生成密钥。

日志均在私有目录：后端 `server.log`、`backend.stdout.log`、`backend.stderr.log`；前端 `frontend.stdout.log`、`frontend.stderr.log`；迁移 `migrations.log`；浏览器检查 `browser-run.log`、`browser-verification.json`。容器日志：

```bash
docker --context colima-hrm-local compose -p hrm-local \
  -f ~/.hrm-payroll-local/compose.yaml logs --tail 100
```

剩余非阻断提示：Netty在macOS缺少原生DNS解析器，已回退系统解析，实际本机连接正常；前端Browserslist数据过期提示。本次没有为这些提示升级依赖或改动lockfile。

本次未执行：完整Java测试、完整前端类型/生产构建复验、工资核算/审批/发放等业务矩阵、OA工时回归、真实银行/税务机构对接。第1节的历史证据与待验事项仍然适用，不据本机启动检查宣称业务全部验收。
