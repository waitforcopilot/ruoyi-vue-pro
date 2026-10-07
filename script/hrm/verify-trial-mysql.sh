#!/usr/bin/env bash
set -euo pipefail
repo_root=$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)
container_id=''
export MYSQL_ROOT_PASSWORD
MYSQL_ROOT_PASSWORD=$(python3 -c 'import secrets; print(secrets.token_hex(24))')
cleanup() { if [ -n "$container_id" ]; then docker rm -f "$container_id" >/dev/null 2>&1 || true; fi; }
trap cleanup EXIT
container_id=$(docker run --detach --network none --env MYSQL_ROOT_PASSWORD "${MYSQL_TEST_IMAGE:-mysql:8.0.46}")
ready=false
for attempt in $(seq 1 120); do
 if docker exec "$container_id" sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot -e "SELECT 1" >/dev/null 2>&1'; then ready=true; break; fi
 sleep 1
done
if [ "$ready" != true ]; then echo 'Isolated MySQL did not become ready.' >&2; exit 1; fi
mysql_exec() { docker exec -i "$container_id" sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql --default-character-set=utf8mb4 -uroot "$@"' sh "$@"; }
mysql_exec -e 'CREATE DATABASE trial_verify CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci'
python3 - "$repo_root/sql/mysql/ruoyi-vue-pro.sql" <<'PY' | mysql_exec trial_verify
import re,sys
print(re.search(r'CREATE TABLE `system_menu`\s*\([\s\S]*?;',open(sys.argv[1]).read()).group())
print("INSERT INTO system_menu(name,type,parent_id,path) VALUES('隔离 HRM 菜单',1,0,'/hrm');")
PY
mysql_exec trial_verify < "$repo_root/sql/mysql/upgrade/20261007-hrm-payroll-trial-batches.sql"
mysql_exec trial_verify -e "INSERT INTO hrm_payroll_trial_batch(code,title,entity_code,entity_name,period_type,period_start,period_end,definition_id,revision,person_count,configuration_json,tenant_id) VALUES('RETAIN','保留试算😀','QA','合成主体','MONTHLY','2026-10-01','2026-10-31',1,1,1,JSON_OBJECT('net','6200.00'),1)"
mysql_exec trial_verify < "$repo_root/sql/mysql/upgrade/20261007-hrm-payroll-trial-batches.sql"
mysql_exec trial_verify < "$repo_root/script/hrm/verify-trial-mysql.sql"
echo 'PASS: trial migration repeatability, three permissions, retained snapshots, tenant/version/request-key uniqueness and Unicode.'
