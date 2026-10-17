# HRM 薪酬项目本地运行交接

交接日期：2026-10-17。代码仓库：<https://github.com/waitforcopilot/ruoyi-vue-pro>，本次交付分支为 `work`。本机运行不依赖 Codex Cloud；以下命令在本机终端执行，推荐 Linux、macOS 或 Windows 的 WSL2。文中的配置生成步骤经过代码核对，**未在你的本机执行**。

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
