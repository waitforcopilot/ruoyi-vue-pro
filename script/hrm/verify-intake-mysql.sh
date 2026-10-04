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
mysql_exec -e 'CREATE DATABASE intake_verify CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci'
python3 - "$repo_root/sql/mysql/ruoyi-vue-pro.sql" <<'PY' | mysql_exec intake_verify
import re, sys
source=open(sys.argv[1]).read()
print(re.search(r'CREATE TABLE `system_menu`\s*\([\s\S]*?;',source).group())
PY
mysql_exec intake_verify < "$repo_root/sql/mysql/upgrade/20261004-hrm-payroll-requirements.sql"
mysql_exec intake_verify < "$repo_root/sql/mysql/upgrade/20261004-hrm-payroll-intake.sql"
mysql_exec intake_verify -e "INSERT INTO hrm_payroll_source_contract(source_id,source_code,source_name,contract_version,title,schema_json,field_count,tenant_id) VALUES(1,'DS-TEST','隔离来源',1,'保留草稿','{}',1,1)"
mysql_exec intake_verify < "$repo_root/sql/mysql/upgrade/20261004-hrm-payroll-intake.sql"
mysql_exec intake_verify < "$repo_root/script/hrm/verify-intake-mysql.sql"
echo 'PASS: intake migration repeatability, four permissions, large Unicode snapshots and tenant/owner unique keys.'
